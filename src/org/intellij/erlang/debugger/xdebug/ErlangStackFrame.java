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

package org.intellij.erlang.debugger.xdebug;

import com.ericsson.otp.erlang.OtpErlangList;
import com.ericsson.otp.erlang.OtpErlangObject;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.ui.ColoredTextContainer;
import com.intellij.ui.SimpleTextAttributes;
import com.intellij.xdebugger.XSourcePosition;
import com.intellij.xdebugger.evaluation.XDebuggerEvaluator;
import com.intellij.xdebugger.frame.XCompositeNode;
import com.intellij.xdebugger.frame.XStackFrame;
import com.intellij.xdebugger.frame.XValue;
import com.intellij.xdebugger.frame.XValueChildrenList;
import org.intellij.erlang.debugger.node.ErlangTraceElement;
import org.intellij.erlang.debugger.node.ErlangVariableBinding;
import org.intellij.erlang.debugger.xdebug.xvalue.ErlangXValueFactory;
import org.intellij.erlang.psi.ErlangFile;
import org.intellij.erlang.psi.ErlangFunExpression;
import org.intellij.erlang.psi.ErlangFunction;
import org.intellij.erlang.psi.impl.ErlangPsiImplUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

public class ErlangStackFrame extends XStackFrame {
  private final ErlangDebugLocationResolver myResolver;
  private final ErlangTraceElement myTraceElement;
  private final ErlangSourcePosition mySourcePosition;

  public ErlangStackFrame(@NotNull ErlangDebugLocationResolver resolver,
                          @NotNull ErlangTraceElement traceElement) {
    this(resolver, traceElement, ErlangSourcePosition.create(resolver, traceElement));
  }

  public ErlangStackFrame(@NotNull ErlangDebugLocationResolver resolver,
                          @NotNull ErlangTraceElement traceElement,
                          @Nullable ErlangSourcePosition sourcePosition) {
    myResolver = resolver;
    myTraceElement = traceElement;
    mySourcePosition = sourcePosition;
  }

  @Nullable
  @Override
  public XDebuggerEvaluator getEvaluator() {
    try {
      Files.write(Paths.get("/tmp/wk.java.log"), ("on get eval").getBytes(), StandardOpenOption.APPEND);
    }catch (IOException e) {

    }

    return new XDebuggerEvaluator() {
      @Override
      public void evaluate(@NotNull String expression,
                           @NotNull XEvaluationCallback callback,
                           @Nullable XSourcePosition expressionPosition) {
        try {
          Files.write(Paths.get("/tmp/wk.java.log"), ("on eval " + expression).getBytes(), StandardOpenOption.APPEND);
        }catch (IOException e) {

        }
        callback.evaluated(ErlangXValueFactory.create(new OtpErlangList("coucou po")));
      }

      /*
      @Nullable
      @Override
      public TextRange getExpressionRangeAtOffset(@NotNull Project project,
                                                  @NotNull Document document,
                                                  int offset,
                                                  boolean sideEffectsAllowed) {
        try {
          Files.write(Paths.get("/tmp/wk.java.log"), ("on getExpressionRangeAtOffset " + offset + " // " + sideEffectsAllowed).getBytes(), StandardOpenOption.APPEND);
        }catch (IOException e) {

        }
        return new TextRange(0, 1);
      }
      // */
    };
  }

  @Nullable
  @Override
  public XSourcePosition getSourcePosition() {
    if (mySourcePosition == null) return null;
    return mySourcePosition.getSourcePosition();
  }

  @Override
  public void customizePresentation(@NotNull ColoredTextContainer component) {
    String functionName = mySourcePosition != null ? mySourcePosition.getFunctionName() : null;
    if (functionName != null) {
      ErlangFile module = myResolver.findPsi(mySourcePosition.getSourcePosition().getFile());
      ErlangFunction function = module != null ? module.getFunction(functionName, mySourcePosition.getFunctionArity()) : null;
      if (function != null) {
        String title = ErlangPsiImplUtil.getQualifiedFunctionName(function);
        ErlangFunExpression funExpression = ErlangPsiImplUtil.findFunExpression(function, mySourcePosition.getFunExpressionArity());
        if (funExpression != null) {
          int line = 1 + StringUtil.offsetToLineNumber(funExpression.getContainingFile().getText(), funExpression.getTextOffset());
          title += ": " + mySourcePosition.getFunExpressionName() + " at line " + line;
        }
        component.append(title, SimpleTextAttributes.REGULAR_ATTRIBUTES);
        component.setIcon(AllIcons.Debugger.StackFrame);
        return;
      }
    }
    super.customizePresentation(component);
  }

  @Override
  public void computeChildren(@NotNull XCompositeNode node) {
    XValueChildrenList myVariables = new XValueChildrenList(myTraceElement.getBindings().size());
    for (ErlangVariableBinding binding : myTraceElement.getBindings()) {
      myVariables.add(binding.getName(), getVariableValue(binding.getValue()));
    }
    node.addChildren(myVariables, true);
  }

  private static XValue getVariableValue(OtpErlangObject value) {
    return ErlangXValueFactory.create(value);
  }
}
