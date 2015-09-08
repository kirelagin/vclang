package com.jetbrains.jetpad.vclang.term.definition.visitor;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.typechecking.nameresolver.NameResolver;

public class DefinitionResolveNameVisitor implements AbstractDefinitionVisitor<Void, Void> {
  private final NameResolver myNameResolver;

  public DefinitionResolveNameVisitor(NameResolver nameResolver) {
    myNameResolver = nameResolver;
  }

  @Override
  public Void visitFunction(Abstract.FunctionDefinition def, Void params) {
    return null;
  }

  @Override
  public Void visitData(Abstract.DataDefinition def, Void params) {
    return null;
  }

  @Override
  public Void visitConstructor(Abstract.Constructor def, Void params) {
    return null;
  }

  @Override
  public Void visitClass(Abstract.ClassDefinition def, Void params) {
    return null;
  }
}
