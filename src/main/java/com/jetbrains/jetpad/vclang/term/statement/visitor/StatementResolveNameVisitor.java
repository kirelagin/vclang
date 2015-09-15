package com.jetbrains.jetpad.vclang.term.statement.visitor;

import com.jetbrains.jetpad.vclang.module.Namespace;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.definition.visitor.DefinitionResolveNameVisitor;
import com.jetbrains.jetpad.vclang.typechecking.nameresolver.CompositeNameResolver;
import com.jetbrains.jetpad.vclang.typechecking.nameresolver.NamespaceNameResolver;

import java.io.Closeable;
import java.util.List;

public class StatementResolveNameVisitor implements AbstractStatementVisitor<Void, Void>, Closeable {
  private final Namespace myStaticNamespace;
  private final Namespace myDynamicNamespace;
  private final CompositeNameResolver myNameResolver;
  private final List<String> myContext;

  public StatementResolveNameVisitor(Namespace staticNamespace, Namespace dynamicNamespace, CompositeNameResolver nameResolver, List<String> context) {
    myStaticNamespace = staticNamespace;
    myDynamicNamespace = dynamicNamespace;
    myNameResolver = nameResolver;
    myContext = context;

    myNameResolver.pushNameResolver(new NamespaceNameResolver(staticNamespace, dynamicNamespace));
  }

  @Override
  public Void visitDefine(Abstract.DefineStatement stat, Void params) {
    if (!stat.isStatic() && myDynamicNamespace == null) {
      // TODO: report error
    } else {
      stat.getDefinition().accept(new DefinitionResolveNameVisitor(myStaticNamespace, stat.isStatic() ? null : myDynamicNamespace, myNameResolver, myContext), null);
      myStaticNamespace.addAbstractDefinition(stat.isStatic() ? myStaticNamespace : myDynamicNamespace, stat.getDefinition());
    }
    return null;
  }

  @Override
  public Void visitNamespaceCommand(Abstract.NamespaceCommandStatement stat, Void params) {
    return null;
  }

  @Override
  public void close() {
    myNameResolver.popNameResolver();
  }
}
