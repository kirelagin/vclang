package com.jetbrains.jetpad.vclang.term.definition.visitor;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.expr.arg.Utils;
import com.jetbrains.jetpad.vclang.term.expr.visitor.ResolveNameVisitor;
import com.jetbrains.jetpad.vclang.term.statement.visitor.AbstractStatementVisitor;
import com.jetbrains.jetpad.vclang.typechecking.nameresolver.NameResolver;

import java.util.List;

public class DefinitionResolveNameVisitor implements AbstractDefinitionVisitor<Void, Void> {
  private final NameResolver myNameResolver;
  private final List<String> myContext;

  public DefinitionResolveNameVisitor(NameResolver nameResolver, List<String> context) {
    myNameResolver = nameResolver;
    myContext = context;
  }

  @Override
  public Void visitFunction(Abstract.FunctionDefinition def, Void params) {
    ResolveNameVisitor visitor = new ResolveNameVisitor(myContext);

    try (Utils.ContextSaver saver = new Utils.ContextSaver(myContext)) {
      for (Abstract.Argument argument : def.getArguments()) {
        if (argument instanceof Abstract.TypeArgument) {
          ((Abstract.TypeArgument) argument).getType().accept(visitor, null);
        }
        if (argument instanceof Abstract.TelescopeArgument) {
          myContext.addAll(((Abstract.TelescopeArgument) argument).getNames());
        } else if (argument instanceof Abstract.NameArgument) {
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

    for (Abstract.Statement statement : def.getStatements()) {
      statement.accept(new AbstractStatementVisitor<Void, Void>() {
        @Override
        public Void visitDefine(Abstract.DefineStatement stat, Void params) {
          stat.getDefinition().accept(DefinitionResolveNameVisitor.this, null);
          return null;
        }

        @Override
        public Void visitNamespaceCommand(Abstract.NamespaceCommandStatement stat, Void params) {
          return null;
        }
      }, null);
    }

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
