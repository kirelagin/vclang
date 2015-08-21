package com.jetbrains.jetpad.vclang.term.expr;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.definition.Binding;
import com.jetbrains.jetpad.vclang.term.expr.visitor.AbstractExpressionVisitor;
import com.jetbrains.jetpad.vclang.term.expr.visitor.ExpressionVisitor;
import com.jetbrains.jetpad.vclang.typechecking.error.TypeCheckingError;

import java.util.List;

public class InferHoleExpression extends Expression implements Abstract.InferHoleExpression {
  private final TypeCheckingError myError;

  public InferHoleExpression(TypeCheckingError error) {
    myError = error;
  }

  public TypeCheckingError getError() {
    return myError;
  }

  @Override
  public <T> T accept(ExpressionVisitor<? extends T> visitor) {
    return visitor.visitInferHole(this);
  }

  @Override
  public Expression getType(List<Binding> context) {
    return null;
  }

  @Override
  public <P, R> R accept(AbstractExpressionVisitor<? super P, ? extends R> visitor, P params) {
    return visitor.visitInferHole(this, params);
  }
}
