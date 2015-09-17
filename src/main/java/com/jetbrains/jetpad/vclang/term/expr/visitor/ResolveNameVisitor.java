package com.jetbrains.jetpad.vclang.term.expr.visitor;

import com.jetbrains.jetpad.vclang.module.DefinitionPair;
import com.jetbrains.jetpad.vclang.module.Namespace;
import com.jetbrains.jetpad.vclang.parser.BinOpParser;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.definition.Constructor;
import com.jetbrains.jetpad.vclang.term.expr.arg.Utils;
import com.jetbrains.jetpad.vclang.term.statement.visitor.StatementResolveNameVisitor;
import com.jetbrains.jetpad.vclang.typechecking.error.ErrorReporter;
import com.jetbrains.jetpad.vclang.typechecking.error.TypeCheckingError;
import com.jetbrains.jetpad.vclang.typechecking.nameresolver.CompositeNameResolver;
import com.jetbrains.jetpad.vclang.typechecking.nameresolver.NameResolver;

import java.util.ArrayList;
import java.util.List;

public class ResolveNameVisitor implements AbstractExpressionVisitor<Void, Void> {
  private final ErrorReporter myErrorReporter;
  private final NameResolver myNameResolver;
  private final List<String> myContext;
  private final boolean myStatic;

  public ResolveNameVisitor(ErrorReporter errorReporter, NameResolver nameResolver, List<String> context, boolean isStatic) {
    myErrorReporter = errorReporter;
    myNameResolver = nameResolver;
    myContext = context;
    myStatic = isStatic;
  }

  @Override
  public Void visitApp(Abstract.AppExpression expr, Void params) {
    expr.getFunction().accept(this, null);
    expr.getArgument().getExpression().accept(this, null);
    return null;
  }

  @Override
  public Void visitDefCall(Abstract.DefCallExpression expr, Void params) {
    return null;
  }

  @Override
  public Void visitIndex(Abstract.IndexExpression expr, Void params) {
    return null;
  }

  @Override
  public Void visitLam(Abstract.LamExpression expr, Void params) {
    try (Utils.ContextSaver ignored = new Utils.ContextSaver(myContext)) {
      for (Abstract.Argument argument : expr.getArguments()) {
        if (argument instanceof Abstract.TypeArgument) {
          ((Abstract.TypeArgument) argument).getType().accept(this, null);
        }
        if (argument instanceof Abstract.TelescopeArgument) {
          myContext.addAll(((Abstract.TelescopeArgument) argument).getNames());
        } else if (argument instanceof Abstract.NameArgument) {
          myContext.add(((Abstract.NameArgument) argument).getName());
        }
      }

      expr.getBody().accept(this, null);
    }
    return null;
  }

  @Override
  public Void visitPi(Abstract.PiExpression expr, Void params) {
    try (Utils.ContextSaver ignored = new Utils.ContextSaver(myContext)) {
      for (Abstract.Argument argument : expr.getArguments()) {
        if (argument instanceof Abstract.TypeArgument) {
          ((Abstract.TypeArgument) argument).getType().accept(this, null);
        }
        if (argument instanceof Abstract.TelescopeArgument) {
          myContext.addAll(((Abstract.TelescopeArgument) argument).getNames());
        } else if (argument instanceof Abstract.NameArgument) {
          myContext.add(((Abstract.NameArgument) argument).getName());
        }
      }

      expr.getCodomain().accept(this, null);
    }
    return null;
  }

  @Override
  public Void visitUniverse(Abstract.UniverseExpression expr, Void params) {
    return null;
  }

  @Override
  public Void visitVar(Abstract.VarExpression expr, Void params) {
    if (expr.getName().fixity == Abstract.Definition.Fixity.INFIX || !myContext.contains(expr.getName().name)) {
      DefinitionPair member = myNameResolver.locateName(expr.getName().name, myStatic);
      if (member != null && (member.definition != null || member.abstractDefinition != null)) {
        expr.replaceWithDefCall(member);
      } else {
        myErrorReporter.report(new TypeCheckingError(null, "Not in scope: '" + expr.getName() + "'", expr, myContext));
      }
    }
    return null;
  }

  @Override
  public Void visitInferHole(Abstract.InferHoleExpression expr, Void params) {
    return null;
  }

  @Override
  public Void visitError(Abstract.ErrorExpression expr, Void params) {
    expr.getExpr().accept(this, null);
    return null;
  }

  @Override
  public Void visitTuple(Abstract.TupleExpression expr, Void params) {
    for (Abstract.Expression expression : expr.getFields()) {
      expression.accept(this, null);
    }
    return null;
  }

  @Override
  public Void visitSigma(Abstract.SigmaExpression expr, Void params) {
    try (Utils.ContextSaver ignored = new Utils.ContextSaver(myContext)) {
      for (Abstract.Argument argument : expr.getArguments()) {
        if (argument instanceof Abstract.TypeArgument) {
          ((Abstract.TypeArgument) argument).getType().accept(this, null);
        }
        if (argument instanceof Abstract.TelescopeArgument) {
          myContext.addAll(((Abstract.TelescopeArgument) argument).getNames());
        } else if (argument instanceof Abstract.NameArgument) {
          myContext.add(((Abstract.NameArgument) argument).getName());
        }
      }
    }
    return null;
  }

  @Override
  public Void visitBinOp(Abstract.BinOpExpression expr, Void params) {
    expr.getLeft().accept(this, null);
    expr.getRight().accept(this, null);
    return null;
  }

  @Override
  public Void visitBinOpSequence(Abstract.BinOpSequenceExpression expr, Void params) {
    if (expr.getSequence().isEmpty()) {
      expr.replace(expr.getLeft());
    } else {
      BinOpParser parser = new BinOpParser(myErrorReporter, expr);
      List<BinOpParser.StackElem> stack = new ArrayList<>(expr.getSequence().size());
      Abstract.Expression expression = expr.getLeft();
      for (Abstract.BinOpSequenceElem elem : expr.getSequence()) {
        DefinitionPair member = myNameResolver.locateName(elem.binOp.getName().name, myStatic);
        if (member != null) {
          parser.pushOnStack(stack, expression, member, elem.binOp);
          expression = elem.argument;
        } else {
          myErrorReporter.report(new TypeCheckingError(null, "Not in scope: " + elem.binOp.getName().getPrefixName(), elem.binOp, myContext));
        }
      }
      expr.replace(parser.rollUpStack(stack, expression));
    }
    return null;
  }

  @Override
  public Void visitElim(Abstract.ElimExpression expr, Void params) {
    visitElimCase(expr);
    return null;
  }

  @Override
  public Void visitCase(Abstract.CaseExpression expr, Void params) {
    visitElimCase(expr);
    return null;
  }

  private void visitElimCase(Abstract.ElimCaseExpression expr) {
    expr.getExpression().accept(this, null);
    try (Utils.ContextSaver ignored = new Utils.ContextSaver(myContext)) {
      for (Abstract.Clause clause : expr.getClauses()) {
        for (int i = 0; i < clause.getPatterns().size(); i++) {
          visitPattern(clause, i);
        }

        clause.getExpression().accept(this, null);
      }
    }
  }

  public void visitPattern(Abstract.PatternContainer con, int index) {
    Abstract.Pattern pattern = con.getPatterns().get(index);
    if (pattern instanceof Abstract.NamePattern) {
      String name = ((Abstract.NamePattern) pattern).getName();
      DefinitionPair member = myNameResolver.locateName(name, myStatic);
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
  public Void visitProj(Abstract.ProjExpression expr, Void params) {
    expr.getExpression().accept(this, null);
    return null;
  }

  @Override
  public Void visitClassExt(Abstract.ClassExtExpression expr, Void params) {
    expr.getBaseClassExpression().accept(this, null);
    CompositeNameResolver nameResolver = new CompositeNameResolver();
    nameResolver.pushNameResolver(myNameResolver);
    StatementResolveNameVisitor visitor = new StatementResolveNameVisitor(myErrorReporter, null, new Namespace(new Utils.Name("<anonymous>"), null), nameResolver, myContext);
    for (Abstract.Statement statement : expr.getStatements()) {
      statement.accept(visitor, null);
    }
    return null;
  }

  @Override
  public Void visitNew(Abstract.NewExpression expr, Void params) {
    expr.getExpression().accept(this, null);
    return null;
  }

  @Override
  public Void visitLet(Abstract.LetExpression expr, Void params) {
    try (Utils.ContextSaver ignored = new Utils.ContextSaver(myContext)) {
      for (Abstract.LetClause clause : expr.getClauses()) {
        for (Abstract.Argument argument : clause.getArguments()) {
          if (argument instanceof Abstract.TypeArgument) {
            ((Abstract.TypeArgument) argument).getType().accept(this, null);
          }
          if (argument instanceof Abstract.TelescopeArgument) {
            myContext.addAll(((Abstract.TelescopeArgument) argument).getNames());
          } else
          if (argument instanceof Abstract.NameArgument) {
            myContext.add(((Abstract.NameArgument) argument).getName());
          }
        }

        if (clause.getResultType() != null) {
          clause.getResultType().accept(this, null);
        }
        clause.getTerm().accept(this, null);
        myContext.add(clause.getName().name);
      }

      expr.getExpression().accept(this, null);
    }
    return null;
  }
}
