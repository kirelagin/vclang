package com.jetbrains.jetpad.vclang.core.pattern.elimtree.visitor;

import com.jetbrains.jetpad.vclang.core.context.binding.Binding;
import com.jetbrains.jetpad.vclang.core.context.binding.Variable;
import com.jetbrains.jetpad.vclang.core.expr.ConCallExpression;
import com.jetbrains.jetpad.vclang.core.expr.DataCallExpression;
import com.jetbrains.jetpad.vclang.core.expr.Expression;
import com.jetbrains.jetpad.vclang.core.expr.ReferenceExpression;
import com.jetbrains.jetpad.vclang.core.expr.visitor.NormalizeVisitor;
import com.jetbrains.jetpad.vclang.core.pattern.elimtree.*;
import com.jetbrains.jetpad.vclang.core.sort.Sort;
import com.jetbrains.jetpad.vclang.core.subst.ExprSubstitution;
import com.jetbrains.jetpad.vclang.term.Prelude;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CoverageChecker implements ElimTreeNodeVisitor<ExprSubstitution, Boolean> {
  public interface CoverageCheckerMissingProcessor {
    void process(ExprSubstitution argsSubst);
  }

  private final CoverageCheckerMissingProcessor myProcessor;
  private final Expression myResultType;

  private CoverageChecker(CoverageCheckerMissingProcessor processor, Expression resultType) {
    myProcessor = processor;
    myResultType = resultType;
  }

  public static boolean check(ElimTreeNode tree, ExprSubstitution argsSubst, CoverageCheckerMissingProcessor processor, Expression resultType) {
    return tree.accept(new CoverageChecker(processor, resultType), argsSubst);
  }

  @Override
  public Boolean visitBranch(BranchElimTreeNode branchNode, ExprSubstitution argsSubst) {
    DataCallExpression type = branchNode.getReference().getType().getExpr().normalize(NormalizeVisitor.Mode.WHNF).toDataCall();

    boolean result = true;
    for (ConCallExpression conCall : type.getDefinition().getMatchedConstructors(type)) {
      if (myResultType != null) {
        Sort sort = myResultType.getType().toSort();
        if (sort != null) {
          if (sort.isLessOrEquals(Sort.PROP)) {
            if (conCall.getDefinition() == Prelude.PROP_TRUNC_PATH_CON ||
              conCall.getDefinition() == Prelude.SET_TRUNC_PATH_CON) {
              continue;
            }
          } else if (sort.getHLevel().isLessOrEquals(Sort.SET0.getHLevel())) {
            if (conCall.getDefinition() == Prelude.SET_TRUNC_PATH_CON) {
              continue;
            }
          }
        }
      }
      if (branchNode.getClause(conCall.getDefinition()) == null) {
        branchNode.addClause(conCall.getDefinition(), null);
      }
      Clause clause = branchNode.getClause(conCall.getDefinition());
      result &= clause.getChild().accept(this, clause.getSubst().compose(argsSubst));
    }

    return result;
  }

  @Override
  public Boolean visitLeaf(LeafElimTreeNode leafNode, ExprSubstitution argsSubst) {
    return true;
  }

  @Override
  public Boolean visitEmpty(EmptyElimTreeNode emptyNode, ExprSubstitution argsSubst) {
    List<Binding> tailContext = new ArrayList<>();
    for (Map.Entry<Variable, Expression> entry : argsSubst.getEntries()) {
      ReferenceExpression ref = entry.getValue().toReference();
      if (ref != null) {
        tailContext.add(ref.getBinding());
      }
    }
    return checkEmptyContext(tailContext, argsSubst);
  }

  public boolean checkEmptyContext(List<? extends Binding> tailContext, ExprSubstitution argsSubst) {
    if (tailContext.isEmpty()) {
      myProcessor.process(argsSubst);
      return false;
    }

    DataCallExpression type = tailContext.get(0).getType().getExpr().normalize(NormalizeVisitor.Mode.WHNF).toDataCall();
    if (type == null) {
      return checkEmptyContext(new ArrayList<>(tailContext.subList(1, tailContext.size())), argsSubst);
    }

    List<ConCallExpression> validConCalls = type.getDefinition().getMatchedConstructors(type);
    if (validConCalls == null) {
      return checkEmptyContext(new ArrayList<>(tailContext.subList(1, tailContext.size())), argsSubst);
    }

    BranchElimTreeNode fakeBranch = new BranchElimTreeNode(tailContext.get(0), new ArrayList<>(tailContext.subList(1, tailContext.size())));
    for (ConCallExpression conCall : validConCalls) {
      ConstructorClause clause = fakeBranch.addClause(conCall.getDefinition(), null);
      if (!checkEmptyContext(clause.getTailBindings(), clause.getSubst().compose(argsSubst)))
        return false;
    }
    return true;
  }
}
