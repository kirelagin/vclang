package com.jetbrains.jetpad.vclang.core.expr;

import com.jetbrains.jetpad.vclang.core.context.binding.Binding;
import com.jetbrains.jetpad.vclang.core.context.binding.inference.InferenceVariable;
import com.jetbrains.jetpad.vclang.core.context.param.EmptyDependentLink;
import com.jetbrains.jetpad.vclang.core.definition.Definition;
import com.jetbrains.jetpad.vclang.core.expr.visitor.ExpressionVisitor;

public class ReferenceExpression extends Expression {
  protected Binding myBinding;

  public ReferenceExpression(Binding binding) {
    assert !(binding instanceof Definition);
    assert !(binding instanceof InferenceVariable);
    assert binding != EmptyDependentLink.getInstance();
    myBinding = binding;
  }

  public Binding getBinding() {
    return myBinding;
  }

  @Override
  public <P, R> R accept(ExpressionVisitor<? super P, ? extends R> visitor, P params) {
    return visitor.visitReference(this, params);
  }

  @Override
  public ReferenceExpression toReference() {
    return this;
  }

  @Override
  public Expression getStuckExpression() {
    return this;
  }
}
