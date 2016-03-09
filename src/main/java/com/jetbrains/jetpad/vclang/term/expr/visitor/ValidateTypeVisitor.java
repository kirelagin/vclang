package com.jetbrains.jetpad.vclang.term.expr.visitor;

import com.jetbrains.jetpad.vclang.term.context.param.DependentLink;
import com.jetbrains.jetpad.vclang.term.expr.*;
import com.jetbrains.jetpad.vclang.term.pattern.elimtree.BranchElimTreeNode;
import com.jetbrains.jetpad.vclang.term.pattern.elimtree.EmptyElimTreeNode;
import com.jetbrains.jetpad.vclang.term.pattern.elimtree.LeafElimTreeNode;
import com.jetbrains.jetpad.vclang.term.pattern.elimtree.visitor.ElimTreeNodeVisitor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ValidateTypeVisitor extends BaseExpressionVisitor<Expression, Void>
        implements ElimTreeNodeVisitor<Expression, Void> {

  public static class ErrorReporter {
    private final ArrayList<Expression> expressions = new ArrayList<>();
    private final ArrayList<String> reasons = new ArrayList<>();

    public void addError(Expression expr, String reason) {
      expressions.add(expr);
      reasons.add(reason);
    }

    public ArrayList<Expression> getExpressions() {
      return expressions;
    }

    public ArrayList<String> getReasons() {
      return reasons;
    }

    public int errors() {
      return expressions.size();
    }
  }

  public final ErrorReporter myErrorReporter;

  public ValidateTypeVisitor() {
    this.myErrorReporter = new ErrorReporter();
  }

  public ValidateTypeVisitor(ErrorReporter errorReporter) {
    this.myErrorReporter = errorReporter;
  }

  private void visitDependentLink(DependentLink link) {
    if (!link.isExplicit()) {
      myErrorReporter.addError(link.getType(), "Explicit argument expected");
    }
    if (link.hasNext()) {
      visitDependentLink(link.getNext());
    }
  }

  @Override
  public Void visitDefCall(DefCallExpression expr, Expression expectedType) {
    expr.getType().accept(this, expectedType);
    return null;
  }

  @Override
  public Void visitApp(AppExpression expr, Expression expectedType) {
    ArrayList<ArgumentExpression> args = new ArrayList<>();
    Expression fun = expr.getFunctionArgs(args);
    Expression funType = fun.getType();
    if (!(funType instanceof PiExpression)) {
      myErrorReporter.addError(expr, "Function " + fun + " doesn't have Pi-type");
    } else {
      ArrayList<DependentLink> links = new ArrayList<>();
      funType.getPiParameters(links, true, false);
      Collections.reverse(args);
      Substitution subst = new Substitution();
      if (args.size() > links.size()) {
        myErrorReporter.addError(expr, "Too few Pi abstractions");
      }
      for (int i = 0; i < args.size(); i++) {
        DependentLink param = links.get(i);
        Expression arg = args.get(i).getExpression();
        Expression argType = arg.getType().normalize(NormalizeVisitor.Mode.NF);
        Expression expectedArgType = param.getType().subst(subst).normalize(NormalizeVisitor.Mode.NF);
        if (!argType.equals(expectedArgType)) {
          myErrorReporter.addError(arg, "Expected type: " + expectedArgType + ", actual: " + argType);
        }
        subst.add(param, arg);
      }
    }

    return null;
  }

  @Override
  public Void visitReference(ReferenceExpression expr, Expression expectedType) {
    expr.getType().accept(this, expectedType);
    expr.getBinding().getType().accept(this, expectedType);
    return null;
  }

  @Override
  public Void visitLam(LamExpression expr, Expression expectedType) {
    visitDependentLink(expr.getParameters());
    expr.getType().accept(this, expectedType);
    expr.getBody().accept(this, expectedType);
    return null;
  }

  @Override
  public Void visitPi(PiExpression expr, Expression expectedType) {
    visitDependentLink(expr.getParameters());
    expr.getType().accept(this, expectedType);
    expr.getCodomain().accept(this, expectedType);
    return null;
  }

  @Override
  public Void visitSigma(SigmaExpression expr, Expression expectedType) {
    visitDependentLink(expr.getParameters());
    expr.getType().accept(this, expectedType);
    return null;
  }

  @Override
  public Void visitUniverse(UniverseExpression expr, Expression expectedType) {
    return null;
  }

  @Override
  public Void visitError(ErrorExpression expr, Expression expectedType) {
    myErrorReporter.addError(expr, expr.getError().toString());
    return null;
  }

  @Override
  public Void visitTuple(TupleExpression expr, Expression expectedType) {
    SigmaExpression type = expr.getType();
    type = (SigmaExpression) type.normalize(NormalizeVisitor.Mode.NF);
    DependentLink link = type.getParameters();
    visitDependentLink(link);
    type.accept(this, expectedType);
    Substitution subst = new Substitution();
    for (Expression field : expr.getFields()) {
      field.accept(this, expectedType);
      Expression expectedFieldType = link.getType().subst(subst).normalize(NormalizeVisitor.Mode.NF);
      Expression actualFieldType = field.getType().normalize(NormalizeVisitor.Mode.NF);
      if (!actualFieldType.equals(expectedFieldType)) {
        myErrorReporter.addError(field, "Expected type: " + expectedFieldType + ", found: " + actualFieldType);
      }
      if (!link.hasNext()) {
        myErrorReporter.addError(expr, "Too few abstractions in Sigma");
      }
      subst.add(link, field);
      link = link.getNext();
    }
    return null;
  }

  @Override
  public Void visitProj(ProjExpression expr, Expression expectedType) {
    Expression type = expr.getType();
    type.accept(this, expectedType);
    Expression tuple = expr.getExpression().normalize(NormalizeVisitor.Mode.WHNF);
    tuple.accept(this, expectedType);
    if (!(tuple instanceof TupleExpression)) {
      myErrorReporter.addError(tuple, "Tuple expected");
    } else {
      List<Expression> fields = ((TupleExpression)tuple).getFields();
      if (fields.size() <= expr.getField()) {
        myErrorReporter.addError(tuple, "Too few fields (at least " + (expr.getField() + 1) + " expected)");
      }
    }
    return null;
  }

  @Override
  public Void visitNew(NewExpression expr, Expression expectedType) {
    expr.getType().accept(this, expectedType);
    return null;
  }

  @Override
  public Void visitLet(LetExpression letExpression, Expression expectedType) {
    for (LetClause clause : letExpression.getClauses()) {
      clause.getElimTree().accept(this, clause.getType());
    }
    return null;
  }

  @Override
  public Void visitBranch(BranchElimTreeNode branchNode, Expression expectedType) {
    return null;
  }

  @Override
  public Void visitLeaf(LeafElimTreeNode leafNode, Expression expectedType) {
    leafNode.getExpression().accept(this, expectedType);
    return null;
  }

  @Override
  public Void visitEmpty(EmptyElimTreeNode emptyNode, Expression expectedType) {
    return null;
  }

}
