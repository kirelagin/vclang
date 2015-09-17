package com.jetbrains.jetpad.vclang.term.statement.visitor;

import com.jetbrains.jetpad.vclang.module.DefinitionPair;
import com.jetbrains.jetpad.vclang.module.Namespace;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.definition.visitor.DefinitionResolveNameVisitor;
import com.jetbrains.jetpad.vclang.typechecking.error.ErrorReporter;
import com.jetbrains.jetpad.vclang.typechecking.error.GeneralError;
import com.jetbrains.jetpad.vclang.typechecking.error.TypeCheckingError;
import com.jetbrains.jetpad.vclang.typechecking.nameresolver.CompositeNameResolver;
import com.jetbrains.jetpad.vclang.typechecking.nameresolver.NamespaceNameResolver;

import java.io.Closeable;
import java.util.List;

public class StatementResolveNameVisitor implements AbstractStatementVisitor<Void, Void>, Closeable {
  private final ErrorReporter myErrorReporter;
  private final Namespace myStaticNamespace;
  private final Namespace myDynamicNamespace;
  private final Namespace myPrivateNamespace;
  private final CompositeNameResolver myNameResolver;
  private final List<String> myContext;

  public StatementResolveNameVisitor(ErrorReporter errorReporter, Namespace staticNamespace, Namespace dynamicNamespace, CompositeNameResolver nameResolver, List<String> context) {
    myErrorReporter = errorReporter;
    myStaticNamespace = staticNamespace;
    myDynamicNamespace = dynamicNamespace;
    myPrivateNamespace = staticNamespace == null ? null : new Namespace(staticNamespace.getName(), null);
    myNameResolver = nameResolver;
    myContext = context;

    myNameResolver.pushNameResolver(new NamespaceNameResolver(staticNamespace, dynamicNamespace));
    if (myPrivateNamespace != null) {
      myNameResolver.pushNameResolver(new NamespaceNameResolver(myPrivateNamespace, null));
    }
  }

  @Override
  public Void visitDefine(Abstract.DefineStatement stat, Void params) {
    if (!stat.isStatic() && myDynamicNamespace == null) {
      myErrorReporter.report(new TypeCheckingError(myStaticNamespace, "Non-static definition in a static context", stat, myContext));
    } else
    if (stat.isStatic() && myStaticNamespace == null) {
      myErrorReporter.report(new TypeCheckingError(myDynamicNamespace, "Static definitions are not allowed in this context", stat, myContext));
    } else {
      stat.getDefinition().accept(new DefinitionResolveNameVisitor(myErrorReporter, myStaticNamespace, stat.isStatic() ? null : myDynamicNamespace, myNameResolver, myContext), null);
      (stat.isStatic() ? myStaticNamespace : myDynamicNamespace).addAbstractDefinition(stat.getDefinition());
    }
    return null;
  }

  @Override
  public Void visitNamespaceCommand(Abstract.NamespaceCommandStatement stat, Void params) {
    if (myStaticNamespace == null || myPrivateNamespace == null) {
      myErrorReporter.report(new TypeCheckingError(myDynamicNamespace, "Namespace commands are not allowed in this context", stat, myContext));
      return null;
    }

    boolean export = false, remove = false;
    switch (stat.getKind()) {
      case OPEN:
        break;
      case CLOSE:
        remove = true;
        break;
      case EXPORT:
        export = true;
        break;
      default:
        throw new IllegalStateException();
    }

    List<? extends Abstract.Identifier> path = stat.getPath();
    Abstract.Identifier identifier = path.get(0);
    DefinitionPair member = myNameResolver.locateName(identifier.getName().name, true);
    for (int i = 1; i < path.size(); ++i) {
      DefinitionPair member1 = myNameResolver.getMember(member.namespace, path.get(i).getName().name);
      if (member1 == null) {
        myErrorReporter.report(new TypeCheckingError(myStaticNamespace, "Name '" + path.get(i).getName() + "' " + "does not defined in " + member.namespace, stat, myContext));
        return null;
      }
      member = member1;
    }

    List<? extends Abstract.Identifier> names = stat.getNames();
    if (names != null) {
      for (Abstract.Identifier name : names) {
        DefinitionPair member1 = myNameResolver.getMember(member.namespace, name.getName().name);
        if (member1 == null) {
          myErrorReporter.report(new TypeCheckingError(myStaticNamespace, "Name '" + name.getName() + "' " + "does not defined in " + member.namespace, stat, myContext));
        } else {
          processNamespaceCommand(member1, export, remove, stat);
        }
      }
    } else {
      for (DefinitionPair member1 : member.namespace.getMembers()) {
        processNamespaceCommand(member1, export, remove, stat);
      }
    }
    return null;
  }

  private void processNamespaceCommand(DefinitionPair member, boolean export, boolean remove, Abstract.SourceNode sourceNode) {
    boolean ok;
    if (export) {
      ok = myStaticNamespace.addMember(member) == null;
    } else
    if (remove) {
      ok = myPrivateNamespace.removeMember(member) != null;
    } else {
      ok = myPrivateNamespace.addMember(member) == null;
    }

    if (!ok) {
      GeneralError error = new TypeCheckingError(myStaticNamespace, "Name '" + member.namespace.getName() + "' " + (remove ? "does not defined" : "is already in scope"), sourceNode, myContext);
      error.setLevel(GeneralError.Level.WARNING);
      myErrorReporter.report(error);
    }
  }

  @Override
  public void close() {
    if (myPrivateNamespace != null) {
      myNameResolver.popNameResolver();
    }
    myNameResolver.popNameResolver();
  }
}
