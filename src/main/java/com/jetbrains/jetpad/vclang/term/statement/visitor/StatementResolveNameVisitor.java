package com.jetbrains.jetpad.vclang.term.statement.visitor;

import com.jetbrains.jetpad.vclang.module.Namespace;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.definition.visitor.DefinitionResolveNameVisitor;
import com.jetbrains.jetpad.vclang.typechecking.nameresolver.NameResolver;

import java.util.List;

public class StatementResolveNameVisitor implements AbstractStatementVisitor<Void, Void> {
  private final Namespace myStaticNamespace;
  private final Namespace myDynamicNamespace;
  private final NameResolver myNameResolver;
  private final List<String> myContext;

  public StatementResolveNameVisitor(Namespace staticNamespace, Namespace dynamicNamespace, NameResolver nameResolver, List<String> context) {
    myStaticNamespace = staticNamespace;
    myDynamicNamespace = dynamicNamespace;
    myNameResolver = nameResolver;
    myContext = context;
  }

  @Override
  public Void visitDefine(Abstract.DefineStatement stat, Void params) {
    Namespace parentNamespace = stat.isStatic() ? myStaticNamespace : myDynamicNamespace;
    stat.getDefinition().accept(new DefinitionResolveNameVisitor(parentNamespace.getChild(stat.getDefinition().getName()), myNameResolver, myContext), null);
    return null;
  }

  @Override
  public Void visitNamespaceCommand(Abstract.NamespaceCommandStatement stat, Void params) {
    return null;
  }
}
