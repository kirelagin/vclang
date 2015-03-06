package com.jetbrains.jetpad.vclang.model.expr;

import com.jetbrains.jetpad.vclang.model.visitor.ExpressionVisitor;
import com.jetbrains.jetpad.vclang.term.expr.Abstract;
import com.jetbrains.jetpad.vclang.term.visitor.AbstractExpressionVisitor;
import jetbrains.jetpad.model.property.Property;
import jetbrains.jetpad.model.property.ValueProperty;

public class UniverseExpression extends Expression implements Abstract.UniverseExpression {
  public final Property<Integer> level = new ValueProperty<>();

  @Override
  public int getLevel() {
    return level.get();
  }

  @Override
  public <P, R> R accept(AbstractExpressionVisitor<? super P, ? extends R> visitor, P params) {
    return visitor.visitUniverse(this, params);
  }

  @Override
  public <R> R accept(ExpressionVisitor<? extends R> visitor) {
    return visitor.visitUniverse(this);
  }
}
