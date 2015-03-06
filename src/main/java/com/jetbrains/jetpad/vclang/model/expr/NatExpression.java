package com.jetbrains.jetpad.vclang.model.expr;

import com.jetbrains.jetpad.vclang.model.visitor.ExpressionVisitor;
import com.jetbrains.jetpad.vclang.term.expr.Abstract;
import com.jetbrains.jetpad.vclang.term.visitor.AbstractExpressionVisitor;

public class NatExpression extends Expression implements Abstract.NatExpression {
  @Override
  public <P, R> R accept(AbstractExpressionVisitor<? super P, ? extends R> visitor, P params) {
    return visitor.visitNat(this, params);
  }

  @Override
  public <R> R accept(ExpressionVisitor<? extends R> visitor) {
    return visitor.visitNat(this);
  }
}
