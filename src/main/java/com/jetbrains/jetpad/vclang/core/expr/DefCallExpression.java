package com.jetbrains.jetpad.vclang.core.expr;

import com.jetbrains.jetpad.vclang.core.definition.Definition;
import com.jetbrains.jetpad.vclang.core.sort.Sort;

import java.util.Collections;
import java.util.List;

public abstract class DefCallExpression extends Expression implements CallableCallExpression {
  private final Definition myDefinition;

  public DefCallExpression(Definition definition) {
    if(!definition.status().headerIsOK()) {
      throw new IllegalStateException("Reference to a definition with a header error");
    }
    myDefinition = definition;
  }

  @Override
  public List<? extends Expression> getDefCallArguments() {
    return Collections.emptyList();
  }

  public abstract Sort getSortArgument();

  @Override
  public Definition getDefinition() {
    return myDefinition;
  }

  @Override
  public DefCallExpression toDefCall() {
    return this;
  }
}
