package com.jetbrains.jetpad.vclang.term.expr;

import com.jetbrains.jetpad.vclang.term.Preprelude;
import com.jetbrains.jetpad.vclang.term.context.param.DependentLink;
import com.jetbrains.jetpad.vclang.term.context.param.UntypedDependentLink;
import com.jetbrains.jetpad.vclang.term.definition.TypeUniverse;
import com.jetbrains.jetpad.vclang.term.definition.Universe;
import com.jetbrains.jetpad.vclang.typechecking.implicitargs.equations.DummyEquations;

public abstract class DependentTypeExpression extends Expression {
  private final DependentLink myLink;

  public DependentTypeExpression(DependentLink link) {
    myLink = link;
  }

  public DependentLink getParameters() {
    return myLink;
  }

  public Universe getUniverse() {
    DependentLink link = myLink;
    Universe universe = null;
    boolean hasSetArgs = false;

    while (link.hasNext()) {
      if (!(link instanceof UntypedDependentLink)) {
        UniverseExpression type = link.getType().getType().toUniverse();
        if (type == null) return null;
        if (type.getUniverse().equals(TypeUniverse.PROP)) {
          link = link.getNext();
          continue;
        }
        if (type.getUniverse().equals(TypeUniverse.SET)) {
          hasSetArgs = true;
          link = link.getNext();
          continue;
        }
        if (universe == null) {
          universe = type.getUniverse();
        } else {
          if (!universe.equals(type.getUniverse())) return null;
        }
      }
      link = link.getNext();
    }

    return universe == null ? hasSetArgs ? TypeUniverse.SetOfLevel(0) : TypeUniverse.PROP : universe;
  }

  @Override
  public Expression getType() {
    Universe universe = getUniverse();
    return universe == null ? null : new UniverseExpression(universe);
  }

  @Override
  public DependentTypeExpression toDependentType() {
    return this;
  }
}
