package com.jetbrains.jetpad.vclang.model.expr;

import com.jetbrains.jetpad.vclang.model.visitor.ExpressionVisitor;
import com.jetbrains.jetpad.vclang.term.expr.Abstract;
import com.jetbrains.jetpad.vclang.term.visitor.AbstractExpressionVisitor;
import jetbrains.jetpad.model.property.Property;
import jetbrains.jetpad.model.property.ValueProperty;

public class PiExpression extends ParensExpression implements Abstract.PiExpression {
  public final Property<Boolean> isExplicit = new ValueProperty<>();
  public final Property<String> variable = new ValueProperty<>();
  public final Property<Expression> domain = new ValueProperty<>();
  public final Property<Expression> codomain = new ValueProperty<>();

  public PiExpression(boolean parens) {
    super(parens);
  }

  @Override
  public boolean isExplicit() {
    return isExplicit.get();
  }

  @Override
  public String getVariable() {
    return variable.get();
  }

  @Override
  public Abstract.Expression getDomain() {
    return domain.get();
  }

  @Override
  public Abstract.Expression getCodomain() {
    return codomain.get();
  }

  @Override
  public <P, R> R accept(AbstractExpressionVisitor<? super P, ? extends R> visitor, P params) {
    return visitor.visitPi(this, params);
  }

  @Override
  public <R> R accept(ExpressionVisitor<? extends R> visitor) {
    return visitor.visitPi(this);
  }

  @Override
  public PiExpression copy() {
    PiExpression result = new PiExpression(parens);
    result.isExplicit.set(isExplicit.get());
    result.variable.set(variable.get());
    if (domain.get() != null) {
      result.domain.set(domain.get().copy());
    }
    if (codomain.get() != null) {
      result.codomain.set(codomain.get().copy());
    }
    return result;
  }
}
