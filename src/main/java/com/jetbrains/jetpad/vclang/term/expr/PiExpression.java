package com.jetbrains.jetpad.vclang.term.expr;

import com.jetbrains.jetpad.vclang.term.context.param.DependentLink;
import com.jetbrains.jetpad.vclang.term.definition.TypeUniverse;
import com.jetbrains.jetpad.vclang.term.definition.Universe;
import com.jetbrains.jetpad.vclang.term.expr.visitor.ExpressionVisitor;
import com.jetbrains.jetpad.vclang.term.expr.visitor.NormalizeVisitor;

public class PiExpression extends DependentTypeExpression {
  private final Expression myCodomain;

  public PiExpression(DependentLink link, Expression codomain) {
    super(link);
    assert link.hasNext();
    myCodomain = codomain;
  }

  public Expression getCodomain() {
    return myCodomain;
  }

  @Override
  public <P, R> R accept(ExpressionVisitor<? super P, ? extends R> visitor, P params) {
    return visitor.visitPi(this, params);
  }

  @Override
  public Universe getUniverse() {
    Universe universe = super.getUniverse();
    Expression type = myCodomain.getType();
    if (type == null || universe == null) {
      return null;
    }
    type = type.normalize(NormalizeVisitor.Mode.WHNF);
    Universe codomainUniverse = type.toUniverse().getUniverse();
    if (codomainUniverse == null) return null;
    if (codomainUniverse.equals(TypeUniverse.PROP)) return TypeUniverse.PROP;
    if (!universe.equals(codomainUniverse)) return null;
    return universe;
  }

  @Override
  public PiExpression toPi() {
    return this;
  }
}
