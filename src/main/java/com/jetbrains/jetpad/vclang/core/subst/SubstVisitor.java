package com.jetbrains.jetpad.vclang.core.subst;

import com.jetbrains.jetpad.vclang.core.context.binding.Binding;
import com.jetbrains.jetpad.vclang.core.context.param.DependentLink;
import com.jetbrains.jetpad.vclang.core.context.param.SingleDependentLink;
import com.jetbrains.jetpad.vclang.core.definition.Constructor;
import com.jetbrains.jetpad.vclang.core.elimtree.BranchElimTree;
import com.jetbrains.jetpad.vclang.core.elimtree.ElimTree;
import com.jetbrains.jetpad.vclang.core.elimtree.LeafElimTree;
import com.jetbrains.jetpad.vclang.core.expr.*;
import com.jetbrains.jetpad.vclang.core.expr.visitor.BaseExpressionVisitor;
import com.jetbrains.jetpad.vclang.core.internal.FieldSet;
import com.jetbrains.jetpad.vclang.core.pattern.elimtree.*;
import com.jetbrains.jetpad.vclang.core.pattern.elimtree.visitor.ElimTreeNodeVisitor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class SubstVisitor extends BaseExpressionVisitor<Void, Expression> implements ElimTreeNodeVisitor<Void, ElimTreeNode> {
  private final ExprSubstitution myExprSubstitution;
  private final LevelSubstitution myLevelSubstitution;

  public SubstVisitor(ExprSubstitution exprSubstitution, LevelSubstitution levelSubstitution) {
    myExprSubstitution = exprSubstitution;
    myLevelSubstitution = levelSubstitution;
  }

  @Override
  public AppExpression visitApp(AppExpression expr, Void params) {
    return new AppExpression(expr.getFunction().accept(this, null), expr.getArgument().accept(this, null));
  }

  @Override
  public Expression visitDefCall(DefCallExpression expr, Void params) {
    List<Expression> args = new ArrayList<>(expr.getDefCallArguments().size());
    for (Expression arg : expr.getDefCallArguments()) {
      args.add(arg.accept(this, null));
    }
    return expr.getDefinition().getDefCall(expr.getSortArgument().subst(myLevelSubstitution), null, args);
  }

  @Override
  public DataCallExpression visitDataCall(DataCallExpression expr, Void params) {
    return (DataCallExpression) visitDefCall(expr, null);
  }

  @Override
  public ConCallExpression visitConCall(ConCallExpression expr, Void params) {
    List<Expression> dataTypeArgs = new ArrayList<>(expr.getDataTypeArguments().size());
    for (Expression parameter : expr.getDataTypeArguments()) {
      Expression expr2 = parameter.accept(this, null);
      if (expr2 == null) {
        return null;
      }
      dataTypeArgs.add(expr2);
    }

    List<Expression> args = new ArrayList<>(expr.getDefCallArguments().size());
    for (Expression arg : expr.getDefCallArguments()) {
      args.add(arg.accept(this, null));
    }

    return new ConCallExpression(expr.getDefinition(), expr.getSortArgument().subst(myLevelSubstitution), dataTypeArgs, args);
  }

  @Override
  public ClassCallExpression visitClassCall(ClassCallExpression expr, Void params) {
    FieldSet fieldSet = FieldSet.applyVisitorToImplemented(expr.getFieldSet(), expr.getDefinition().getFieldSet(), this, null);
    return new ClassCallExpression(expr.getDefinition(), expr.getSortArgument().subst(myLevelSubstitution), fieldSet);
  }

  @Override
  public Expression visitLetClauseCall(LetClauseCallExpression expr, Void params) {
    List<Expression> args = new ArrayList<>(expr.getDefCallArguments().size());
    for (Expression arg : expr.getDefCallArguments()) {
      args.add(arg.accept(this, null));
    }

    Expression subst = myExprSubstitution.get(expr.getLetClause());
    if (subst != null) {
      if (subst.toReference() != null && subst.toReference().getBinding() instanceof LetClause) {
        return new LetClauseCallExpression((LetClause) subst.toReference().getBinding(), args);
      } else {
        for (Expression arg : args) {
          subst = new AppExpression(subst, arg);
        }
        return subst;
      }
    } else {
      return new LetClauseCallExpression(expr.getLetClause(), args);
    }
  }

  @Override
  public Expression visitFieldCall(FieldCallExpression expr, Void params) {
    Expression result = myExprSubstitution.get(expr.getDefinition());
    if (result != null) {
      return new AppExpression(result, expr.getExpression().accept(this, null));
    } else {
      return ExpressionFactory.FieldCall(expr.getDefinition(), expr.getExpression().accept(this, null));
    }
  }

  @Override
  public Expression visitReference(ReferenceExpression expr, Void params) {
    Expression result = myExprSubstitution.get(expr.getBinding());
    if (result != null) {
      return result;
    }
    return expr;
  }

  @Override
  public Expression visitInferenceReference(InferenceReferenceExpression expr, Void params) {
    if (expr.getSubstExpression() != null) {
      return new InferenceReferenceExpression(expr.getOriginalVariable(), expr.getSubstExpression().accept(this, null));
    }
    Expression result = myExprSubstitution.get(expr.getVariable());
    return result != null ? result : expr;
  }

  @Override
  public LamExpression visitLam(LamExpression expr, Void params) {
    SingleDependentLink parameters = DependentLink.Helper.subst(expr.getParameters(), myExprSubstitution, myLevelSubstitution);
    LamExpression result = new LamExpression(expr.getResultSort().subst(myLevelSubstitution), parameters, expr.getBody().accept(this, null));
    DependentLink.Helper.freeSubsts(expr.getParameters(), myExprSubstitution);
    return result;
  }

  @Override
  public PiExpression visitPi(PiExpression expr, Void params) {
    SingleDependentLink parameters = DependentLink.Helper.subst(expr.getParameters(), myExprSubstitution, myLevelSubstitution);
    PiExpression result = new PiExpression(expr.getResultSort().subst(myLevelSubstitution), parameters, expr.getCodomain().accept(this, null));
    DependentLink.Helper.freeSubsts(expr.getParameters(), myExprSubstitution);
    return result;
  }

  @Override
  public SigmaExpression visitSigma(SigmaExpression expr, Void params) {
    SigmaExpression result = new SigmaExpression(expr.getSort().subst(myLevelSubstitution), DependentLink.Helper.subst(expr.getParameters(), myExprSubstitution, myLevelSubstitution));
    DependentLink.Helper.freeSubsts(expr.getParameters(), myExprSubstitution);
    return result;
  }

  @Override
  public BranchElimTreeNode visitBranch(BranchElimTreeNode branchNode, Void params) {
    Binding newReference = visitReference(new ReferenceExpression(branchNode.getReference()), null).toReference().getBinding();
    List<Binding> newContextTail = branchNode.getContextTail().stream().map(binding -> visitReference(new ReferenceExpression(binding), null).toReference().getBinding()).collect(Collectors.toList());
    BranchElimTreeNode newNode = new BranchElimTreeNode(newReference, newContextTail);
    for (ConstructorClause clause : branchNode.getConstructorClauses()) {
      ConstructorClause newClause = newNode.addClause(clause.getConstructor(), DependentLink.Helper.toNames(clause.getParameters()));
      for (DependentLink linkOld = clause.getParameters(), linkNew = newClause.getParameters(); linkOld.hasNext(); linkOld = linkOld.getNext(), linkNew = linkNew.getNext()) {
        myExprSubstitution.add(linkOld, new ReferenceExpression(linkNew));
      }
      for (int i = 0; i < clause.getTailBindings().size(); i++) {
        myExprSubstitution.add(clause.getTailBindings().get(i), new ReferenceExpression(newClause.getTailBindings().get(i)));
      }

      newClause.setChild(clause.getChild().accept(this, null));

      myExprSubstitution.removeAll(DependentLink.Helper.toContext(clause.getParameters()));
      myExprSubstitution.removeAll(clause.getTailBindings());
    }

    if (branchNode.getOtherwiseClause() != null) {
      OtherwiseClause newClause = newNode.addOtherwiseClause();
      newClause.setChild(branchNode.getOtherwiseClause().getChild().accept(this, null));
    }

    return newNode;
  }

  @Override
  public LeafElimTreeNode visitLeaf(LeafElimTreeNode leafNode, Void params) {
    LeafElimTreeNode result = new LeafElimTreeNode(leafNode.getExpression().accept(this, null));
    if (leafNode.getMatched() != null) {
      List<Binding> matched = new ArrayList<>(leafNode.getMatched().size());
      for (Binding binding : leafNode.getMatched()) {
        Expression replacement = myExprSubstitution.get(binding);
        matched.add(replacement != null ? replacement.toReference().getBinding() : binding);
      }
      result.setMatched(matched);
    }
    return result;
  }

  @Override
  public ElimTreeNode visitEmpty(EmptyElimTreeNode emptyNode, Void params) {
    return emptyNode;
  }

  @Override
  public UniverseExpression visitUniverse(UniverseExpression expr, Void params) {
    return myLevelSubstitution.isEmpty() ? expr : new UniverseExpression(expr.getSort().subst(myLevelSubstitution));
  }

  @Override
  public Expression visitError(ErrorExpression expr, Void params) {
    return expr.getExpr() == null ? expr : new ErrorExpression(expr.getExpr().accept(this, null), expr.getError());
  }

  @Override
  public TupleExpression visitTuple(TupleExpression expr, Void params) {
    List<Expression> fields = new ArrayList<>(expr.getFields().size());
    for (Expression field : expr.getFields()) {
      fields.add(field.accept(this, null));
    }
    return new TupleExpression(fields, visitSigma(expr.getSigmaType(), null));
  }

  @Override
  public Expression visitProj(ProjExpression expr, Void params) {
    return new ProjExpression(expr.getExpression().accept(this, null), expr.getField());
  }

  @Override
  public Expression visitNew(NewExpression expr, Void params) {
    return new NewExpression(visitClassCall(expr.getExpression(), null));
  }

  @Override
  public LetExpression visitLet(LetExpression letExpression, Void params) {
    List<LetClause> clauses = new ArrayList<>(letExpression.getClauses().size());
    for (LetClause clause : letExpression.getClauses()) {
      LetClause newClause = new LetClause(clause.getName(), clause.getExpression().accept(this, null));
      clauses.add(newClause);
      myExprSubstitution.add(clause, new ReferenceExpression(newClause));
    }
    LetExpression result = new LetExpression(clauses, letExpression.getExpression().accept(this, null));
    letExpression.getClauses().forEach(myExprSubstitution::remove);
    return result;
  }

  @Override
  public Expression visitCase(CaseExpression expr, Void params) {
    List<Expression> arguments = expr.getArguments().stream().map(arg -> arg.accept(this, null)).collect(Collectors.toList());
    DependentLink parameters = DependentLink.Helper.subst(expr.getParameters(), myExprSubstitution, myLevelSubstitution);
    Expression type = expr.getResultType().accept(this, null);
    DependentLink.Helper.freeSubsts(expr.getParameters(), myExprSubstitution);
    return new CaseExpression(parameters, type, substElimTree(expr.getElimTree()), arguments);
  }

  private ElimTree substElimTree(ElimTree elimTree) {
    DependentLink vars = DependentLink.Helper.subst(elimTree.getParameters(), myExprSubstitution, myLevelSubstitution);
    if (elimTree instanceof LeafElimTree) {
      elimTree = new LeafElimTree(vars, ((LeafElimTree) elimTree).getExpression().accept(this, null));
    } else {
      Map<Constructor, ElimTree> children = new HashMap<>();
      for (Map.Entry<Constructor, ElimTree> entry : ((BranchElimTree) elimTree).getChildren()) {
        children.put(entry.getKey(), substElimTree(entry.getValue()));
      }
      elimTree = new BranchElimTree(vars, children);
    }
    DependentLink.Helper.freeSubsts(elimTree.getParameters(), myExprSubstitution);
    return elimTree;
  }

  @Override
  public Expression visitOfType(OfTypeExpression expr, Void params) {
    return new OfTypeExpression(expr.getExpression().accept(this, null), expr.getTypeOf().accept(this, null));
  }
}
