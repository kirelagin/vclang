package com.jetbrains.jetpad.vclang.core.context.binding.inference;

import com.jetbrains.jetpad.vclang.core.expr.Expression;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.typechecking.error.local.ArgInferenceError;
import com.jetbrains.jetpad.vclang.typechecking.error.local.LocalTypeCheckingError;

public class ExpressionInferenceVariable extends InferenceVariable {
  public ExpressionInferenceVariable(Expression type, Abstract.SourceNode sourceNode) {
    super("H", type, sourceNode);
  }

  @Override
  public LocalTypeCheckingError getErrorInfer(Expression... candidates) {
    return new ArgInferenceError(ArgInferenceError.expression(), getSourceNode(), candidates);
  }

  @Override
  public LocalTypeCheckingError getErrorMismatch(Expression expectedType, Expression actualType, Expression candidate) {
    return new ArgInferenceError(ArgInferenceError.expression(), expectedType, actualType, getSourceNode(), candidate);
  }
}
