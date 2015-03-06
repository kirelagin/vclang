package com.jetbrains.jetpad.vclang.model.expr;

import com.jetbrains.jetpad.vclang.model.visitor.ExpressionVisitor;
import com.jetbrains.jetpad.vclang.term.expr.Abstract;
import com.jetbrains.jetpad.vclang.term.visitor.AbstractExpressionVisitor;
import jetbrains.jetpad.model.property.Property;
import jetbrains.jetpad.model.property.ValueProperty;

public class AppExpression extends ParensExpression implements Abstract.AppExpression {
  public final Property<Expression> function = new ValueProperty<>();
  public final Property<Expression> argument = new ValueProperty<>();

  public AppExpression(boolean parens) {
    super(parens);
  }

  @Override
  public Expression getFunction() {
    return function.get();
  }

  @Override
  public Expression getArgument() {
    return argument.get();
  }

  @Override
  public <P, R> R accept(AbstractExpressionVisitor<? super P, ? extends R> visitor, P params) {
    return visitor.visitApp(this, params);
  }

  @Override
  public <R> R accept(ExpressionVisitor<? extends R> visitor) {
    return visitor.visitApp(this);
  }

  @Override
  public AppExpression copy() {
    AppExpression result = new AppExpression(parens);
    if (function.get() != null) {
      result.function.set(function.get().copy());
    }
    if (argument.get() != null) {
      result.argument.set(argument.get().copy());
    }
    return result;
  }
}
