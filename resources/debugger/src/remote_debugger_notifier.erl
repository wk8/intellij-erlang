-module(remote_debugger_notifier).

-include("process_names.hrl").
-include("remote_debugger_messages.hrl").
-include("trace_utils.hrl").

-export([run/1, breakpoint_reached/1]).

run(Debugger) ->
  register(?RDEBUG_NOTIFIER, self()),
  attach_process(),
  loop(Debugger).

loop(Debugger) ->
  receive
    MessageToSend ->
      ?trace_message(MessageToSend),
      Debugger ! MessageToSend
  end,
  loop(Debugger).

attach_process() ->
  int:auto_attach([break], {?MODULE, breakpoint_reached, []}).

breakpoint_reached(Pid) ->
  ?RDEBUG_NOTIFIER ! #breakpoint_reached{pid = Pid, snapshot = snapshot_with_stacks()}.

snapshot_with_stacks() ->
  Wkpo = [{Pid, Init, Status, Info, get_stack(Pid, Status)} || {Pid, Init, Status, Info} <- int:snapshot()],
  wkpo("snapshot_with_stacks =>~n~p", [Wkpo]),
  Wkpo.
%% wkpo get_stack(Pid, Status) = snapshots

get_stack(Pid, break) ->
  Wkpo = do_get_stackframes(Pid),
  wkpo("get_stack(~p, break) => ~p", [Pid, Wkpo]),
  Wkpo;
get_stack(_, _) ->
  [].

do_get_stackframes(Pid) ->
  case dbg_iserver:safe_call({get_meta, Pid}) of
    {ok, MetaPid} ->
      Stack = int:meta(MetaPid, backtrace, all),
      %% wkpo {SP, TraceElement, get_bindings(MetaPid, SP)} = one snapshot
      [{SP, TraceElement, get_bindings(MetaPid, SP)} || {SP, TraceElement} <- Stack];
    Error ->
      io:format("Failed to obtain meta pid for ~p: ~p~n", [Pid, Error]),
      []
  end.

get_bindings(MetaPid, SP) ->
  int:meta(MetaPid, bindings, SP).

%% TODO wkpo
wkpo(Format, Args) ->
    {Mega, Secs, MicroSecs} = erlang:timestamp(),
    Ts = Mega * 1000000 + Secs + 0.000001 * MicroSecs,
    Format1 = "[~p (~p on ~s) - ~s] " ++ Format ++ "\n",
    Args1 = [Ts, self(), node(), ?MODULE | Args],
    String = io_lib:format(Format1, Args1),
    file:write_file("/tmp/wk.erl.log", String, [append]).
