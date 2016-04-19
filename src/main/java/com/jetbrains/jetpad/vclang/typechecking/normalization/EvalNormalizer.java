package com.jetbrains.jetpad.vclang.typechecking.normalization;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.Prelude;
import com.jetbrains.jetpad.vclang.term.Preprelude;
import com.jetbrains.jetpad.vclang.term.context.LinkList;
import com.jetbrains.jetpad.vclang.term.context.binding.Binding;
import com.jetbrains.jetpad.vclang.term.context.binding.TypedBinding;
import com.jetbrains.jetpad.vclang.term.context.param.DependentLink;
import com.jetbrains.jetpad.vclang.term.context.param.EmptyDependentLink;
import com.jetbrains.jetpad.vclang.term.context.param.TypedDependentLink;
import com.jetbrains.jetpad.vclang.term.definition.Function;
import com.jetbrains.jetpad.vclang.term.definition.FunctionDefinition;
import com.jetbrains.jetpad.vclang.term.expr.*;
import com.jetbrains.jetpad.vclang.term.expr.visitor.NormalizeVisitor;
import com.jetbrains.jetpad.vclang.term.pattern.elimtree.LeafElimTreeNode;

import java.util.*;

import static com.jetbrains.jetpad.vclang.term.expr.ExpressionFactory.*;

public class EvalNormalizer implements Normalizer {
  @Override
  public Expression normalize(LamExpression fun, List<? extends Expression> arguments, List<? extends EnumSet<AppExpression.Flag>> flags, NormalizeVisitor.Mode mode) {
    int i = 0;
    DependentLink link = fun.getParameters();
    Substitution subst = new Substitution();
    while (link.hasNext() && i < arguments.size()) {
      subst.add(link, arguments.get(i++));
      link = link.getNext();
    }

    Expression result = fun.getBody();
    if (link.hasNext()) {
      result = Lam(link, result);
    }
    result = result.subst(subst);
    if (result != fun.getBody()) {
      result = result.addArguments(arguments.subList(i, arguments.size()), flags.subList(i, flags.size()));
    } else {
      result = Apps(result, arguments.subList(i, arguments.size()), flags.subList(i, flags.size()));
    }
    return result.normalize(mode);
  }

  @Override
  public Expression normalize(Function fun, DependentLink params, List<? extends Expression> paramArgs, List<? extends Expression> arguments, List<? extends Expression> otherArguments, List<? extends EnumSet<AppExpression.Flag>> otherFlags, NormalizeVisitor.Mode mode) {
    assert fun.getNumberOfRequiredArguments() == arguments.size();

    if (fun instanceof FunctionDefinition && Prelude.isCoe((FunctionDefinition) fun)) {
      Expression result = null;

      Binding binding = new TypedBinding("i", DataCall(Preprelude.INTERVAL));
      Expression normExpr = Apps(arguments.get(1), Reference(binding)).normalize(NormalizeVisitor.Mode.NF);
      if (!normExpr.findBinding(binding)) {
        result = arguments.get(2);
      } else {
        FunCallExpression mbIsoFun = normExpr.getFunction().toFunCall();
        List<? extends Expression> mbIsoArgs = normExpr.getArguments();
        if (mbIsoFun != null && Prelude.isIso(mbIsoFun.getDefinition()) && mbIsoArgs.size() == 8) {
          boolean noFreeVar = true;
          for (int i = 0; i < mbIsoArgs.size() - 1; i++) {
            if (mbIsoArgs.get(i).findBinding(binding)) {
              noFreeVar = false;
              break;
            }
          }
          if (noFreeVar) {
            ConCallExpression normedPtCon = arguments.get(3).normalize(NormalizeVisitor.Mode.NF).toConCall();
            if (normedPtCon != null && normedPtCon.getDefinition() == Preprelude.RIGHT) {
              result = Apps(mbIsoArgs.get(3), arguments.get(2));
            }
          }
        }
      }

      if (result != null) {
        return Apps(result, otherArguments, otherFlags).normalize(mode);
      }
    } else if (fun instanceof FunctionDefinition && Preprelude.isLift((FunctionDefinition) fun)) {
      Expression liftArg = arguments.get(1).normalize(NormalizeVisitor.Mode.NF);
      if (liftArg instanceof PiExpression) {
        PiExpression pi = liftArg.toPi();
        LinkList list = new LinkList();
        for (DependentLink link = pi.getParameters(); link.hasNext(); link = link.getNext()) {
          link.setType(FunCall((FunctionDefinition)fun).addArgument(arguments.get(0), EnumSet.noneOf(AppExpression.Flag.class)).addArgument(link.getType(), AppExpression.DEFAULT));
          list.append(link);
        }
        return Pi(list.getFirst(), FunCall((FunctionDefinition)fun).addArgument(arguments.get(0), EnumSet.noneOf(AppExpression.Flag.class)).addArgument(pi.getCodomain(), AppExpression.DEFAULT));
      } else if (liftArg.getFunction() instanceof DefCallExpression) {
        Expression mbPath = liftArg;
        DefCallExpression defCall = mbPath.getFunction().toDefCall();
        if (Prelude.isPath(defCall.getDefinition())) {
          List<? extends Expression> pathArgs = mbPath.getArguments();
          Expression result = defCall;
          int liftNum = Preprelude.getLiftNum((FunctionDefinition) fun);
          result = result.addArgument(Preprelude.applyNumberOfSuc(pathArgs.get(0), Preprelude.SUC_LEVEL, liftNum), EnumSet.noneOf(AppExpression.Flag.class));
          TypedDependentLink binding = new TypedDependentLink(true, "i", DataCall(Preprelude.INTERVAL), EmptyDependentLink.getInstance());
          Expression normExpr = Apps(pathArgs.get(1), Reference(binding)).normalize(NormalizeVisitor.Mode.NF);
          LamExpression liftedLam = Lam(binding, FunCall((FunctionDefinition) fun).addArgument(arguments.get(0), EnumSet.noneOf(AppExpression.Flag.class)).addArgument(normExpr, AppExpression.DEFAULT));
          result = result.addArgument(liftedLam, EnumSet.noneOf(AppExpression.Flag.class));
          result = result.addArgument(pathArgs.get(2), AppExpression.DEFAULT);
          result = result.addArgument(pathArgs.get(3), AppExpression.DEFAULT);
          return result.normalize(mode);
        }
      }
    }

    List<Expression> matchedArguments = new ArrayList<>(arguments);
    LeafElimTreeNode leaf = fun.getElimTree().match(matchedArguments);
    if (leaf == null) {
      return null;
    }

    Substitution subst = leaf.matchedToSubst(matchedArguments);
    for (Expression argument : paramArgs) {
      subst.add(params, argument);
      params = params.getNext();
    }
    return Apps(leaf.getExpression().subst(subst), otherArguments, otherFlags).normalize(mode);
  }

  @Override
  public Expression normalize(LetExpression expression) {
    Expression term = expression.getExpression().normalize(NormalizeVisitor.Mode.NF);
    Set<Binding> bindings = new HashSet<>();
    for (LetClause clause : expression.getClauses()) {
      bindings.add(clause);
    }
    return term.findBinding(bindings) ? Let(expression.getClauses(), term) : term;
  }
}
