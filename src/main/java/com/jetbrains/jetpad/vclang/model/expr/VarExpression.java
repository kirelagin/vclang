package com.jetbrains.jetpad.vclang.model.expr;

import com.jetbrains.jetpad.vclang.model.visitor.ExpressionVisitor;
import com.jetbrains.jetpad.vclang.term.expr.Abstract;
import com.jetbrains.jetpad.vclang.term.visitor.AbstractExpressionVisitor;
import jetbrains.jetpad.model.property.Property;
import jetbrains.jetpad.model.property.ValueProperty;

public class VarExpression extends Expression implements Abstract.VarExpression {
  public final Property<String> name = new ValueProperty<>();

  @Override
  public String getName() {
    return name.get();
  }

  @Override
  public <P, R> R accept(AbstractExpressionVisitor<? super P, ? extends R> visitor, P params) {
    return visitor.visitVar(this, params);
  }

  @Override
  public <R> R accept(ExpressionVisitor<? extends R> visitor) {
    return visitor.visitVar(this);
  }

  @Override
  public VarExpression copy() {
    VarExpression result = new VarExpression();
    result.name.set(name.get());
    return result;
  }
}
