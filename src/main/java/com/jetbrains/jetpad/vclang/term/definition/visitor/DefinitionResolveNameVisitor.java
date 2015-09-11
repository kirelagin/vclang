package com.jetbrains.jetpad.vclang.term.definition.visitor;

import com.jetbrains.jetpad.vclang.module.DefinitionPair;
import com.jetbrains.jetpad.vclang.module.Namespace;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.definition.Constructor;
import com.jetbrains.jetpad.vclang.term.expr.arg.Utils;
import com.jetbrains.jetpad.vclang.term.expr.visitor.ResolveNameVisitor;
import com.jetbrains.jetpad.vclang.term.statement.visitor.StatementResolveNameVisitor;
import com.jetbrains.jetpad.vclang.typechecking.nameresolver.NameResolver;

import java.util.List;

public class DefinitionResolveNameVisitor implements AbstractDefinitionVisitor<Void, Object> {
  private final Namespace myNamespace;
  private final NameResolver myNameResolver;
  private List<String> myContext;

  public DefinitionResolveNameVisitor(Namespace namespace, NameResolver nameResolver, List<String> context) {
    myNamespace = namespace;
    myNameResolver = nameResolver;
    myContext = context;
  }

  @Override
  public Namespace visitFunction(Abstract.FunctionDefinition def, Void params) {
    ResolveNameVisitor visitor = new ResolveNameVisitor(myNameResolver, myContext);

    try (Utils.ContextSaver ignored = new Utils.ContextSaver(myContext)) {
      for (Abstract.Argument argument : def.getArguments()) {
        if (argument instanceof Abstract.TypeArgument) {
          ((Abstract.TypeArgument) argument).getType().accept(visitor, null);
        }
        if (argument instanceof Abstract.TelescopeArgument) {
          myContext.addAll(((Abstract.TelescopeArgument) argument).getNames());
        } else
        if (argument instanceof Abstract.NameArgument) {
          myContext.add(((Abstract.NameArgument) argument).getName());
        }
      }

      if (def.getResultType() != null) {
        def.getResultType().accept(visitor, null);
      }

      if (def.getTerm() != null) {
        def.getTerm().accept(visitor, null);
      }
    }

    Namespace localNamespace = new Namespace(myNamespace.getName(), null);
    for (Abstract.Statement statement : def.getStatements()) {
      statement.accept(new StatementResolveNameVisitor(myNamespace, localNamespace, myNameResolver, myContext), null);
    }

    return localNamespace;
  }

  @Override
  public Void visitData(Abstract.DataDefinition def, Void params) {
    ResolveNameVisitor visitor = new ResolveNameVisitor(myNameResolver, myContext);

    try (Utils.CompleteContextSaver<String> saver = new Utils.CompleteContextSaver<>(myContext)) {
      for (Abstract.TypeArgument parameter : def.getParameters()) {
        parameter.getType().accept(visitor, null);
        if (parameter instanceof Abstract.TelescopeArgument) {
          myContext.addAll(((Abstract.TelescopeArgument) parameter).getNames());
        }
      }

      for (Abstract.Constructor constructor : def.getConstructors()) {
        if (constructor.getPatterns() == null) {
          visitConstructor(constructor, null);
        } else {
          myContext = saver.getOldContext();
          visitConstructor(constructor, null);
          myContext = saver.getCurrentContext();
        }
      }
    }

    return null;
  }

  @Override
  public Void visitConstructor(Abstract.Constructor def, Void params) {
    try (Utils.ContextSaver ignored = new Utils.ContextSaver(myContext)) {
      if (def.getPatterns() != null) {
        for (int i = 0; i < def.getPatterns().size(); ++i) {
          visitPattern(def, i);
        }
      }

      ResolveNameVisitor visitor = new ResolveNameVisitor(myNameResolver, myContext);
      for (Abstract.TypeArgument argument : def.getArguments()) {
        argument.getType().accept(visitor, null);
        if (argument instanceof Abstract.TelescopeArgument) {
          myContext.addAll(((Abstract.TelescopeArgument) argument).getNames());
        }
      }
    }

    return null;
  }

  private void visitPattern(Abstract.PatternContainer con, int index) {
    Abstract.Pattern pattern = con.getPatterns().get(index);
    if (pattern instanceof Abstract.NamePattern) {
      String name = ((Abstract.NamePattern) pattern).getName();
      DefinitionPair member = myNameResolver.locateName(name);
      if (member != null && (member.definition instanceof Constructor || member.abstractDefinition instanceof Abstract.Constructor)) {
        con.replacePatternWithConstructor(index);
      } else {
        myContext.add(name);
        return;
      }
    }
    if (pattern instanceof Abstract.ConstructorPattern) {
      List<? extends Abstract.Pattern> patterns = ((Abstract.ConstructorPattern) pattern).getPatterns();
      for (int i = 0; i < patterns.size(); ++i) {
        visitPattern((Abstract.ConstructorPattern) pattern, i);
      }
    } else {
      throw new IllegalStateException();
    }
  }

  @Override
  public Namespace visitClass(Abstract.ClassDefinition def, Void params) {
    Namespace localNamespace = new Namespace(myNamespace.getName(), null);
    for (Abstract.Statement statement : def.getStatements()) {
      statement.accept(new StatementResolveNameVisitor(myNamespace, localNamespace, myNameResolver, myContext), null);
    }
    return localNamespace;
  }
}
