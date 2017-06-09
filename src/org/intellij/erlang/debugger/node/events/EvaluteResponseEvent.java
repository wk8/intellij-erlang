/*
 * Copyright 2012-2014 Sergey Ignatov
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.intellij.erlang.debugger.node.events;

import com.ericsson.otp.erlang.OtpErlangObject;
import com.ericsson.otp.erlang.OtpErlangPid;
import com.ericsson.otp.erlang.OtpErlangTuple;
import org.intellij.erlang.debugger.node.ErlangDebuggerEventListener;
import org.intellij.erlang.debugger.node.ErlangDebuggerNode;

// TODO wkpo cleanup imports
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

class EvaluteResponseEvent extends ErlangDebuggerEvent {
  public static final String NAME = "evaluate_response";
  private final OtpErlangPid myPid;
  private final OtpErlangObject myResponse;

  public EvaluteResponseEvent(OtpErlangTuple receivedMessage) throws DebuggerEventFormatException {
    OtpErlangObject[] elements = receivedMessage.elements();
    if (elements.length != 3) {
      throw new DebuggerEventFormatException();
    }

    // TODO wkpo
    try {
      Files.write(Paths.get("/tmp/wk.java.log"), ("on recoit result d'eval " + elements[1] + " // " + elements[2] + "\n").getBytes(), StandardOpenOption.APPEND);
    }catch (IOException e) {

    }

    myPid = OtpErlangTermUtil.getPidValue(elements[1]);
    myResponse = elements[2];
  }

  @Override
  public void process(ErlangDebuggerNode debuggerNode, ErlangDebuggerEventListener eventListener) {
    eventListener.handleEvaluationResponse(myPid, myResponse);
  }
}
