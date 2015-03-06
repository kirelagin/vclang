package com.jetbrains.jetpad.vclang.model.visitor;

import com.jetbrains.jetpad.vclang.editor.hybrid.Tokens;
import com.jetbrains.jetpad.vclang.model.expr.*;
import com.jetbrains.jetpad.vclang.term.expr.Abstract;
import jetbrains.jetpad.hybrid.parser.ValueToken;
import jetbrains.jetpad.hybrid.parser.prettyprint.PrettyPrinterContext;

public class PrettyPrintVisitor implements ExpressionVisitor<Void> {
  private final PrettyPrinterContext<Expression> myContext;
  private int myPrec = 0;

  public PrettyPrintVisitor(PrettyPrinterContext<Expression> context, int prec) {
    myContext = context;
    myPrec = prec;
  }

  @Override
  public Void visitApp(AppExpression expr) {
    boolean parens = myPrec > Abstract.AppExpression.PREC;
    if (parens) myContext.append(Tokens.LP);
    myPrec = Abstract.AppExpression.PREC;
    myContext.append(expr.function);
    myPrec = Abstract.AppExpression.PREC + 1;
    myContext.append(expr.argument);
    if (parens) myContext.append(Tokens.RP);
    return null;
  }

  @Override
  public Void visitLam(LamExpression expr) {
    myContext.append(new ValueToken(expr, new ValueToken.ValueCloner<LamExpression>() {
      @Override
      public LamExpression clone(LamExpression val) {
        return val;
      }
    }));
    return null;
  }

  @Override
  public Void visitNat(NatExpression expr) {
    return null;
  }

  @Override
  public Void visitNelim(NelimExpression expr) {
    return null;
  }

  @Override
  public Void visitPi(PiExpression expr) {
    return null;
  }

  @Override
  public Void visitSuc(SucExpression expr) {
    return null;
  }

  @Override
  public Void visitUniverse(UniverseExpression expr) {
    return null;
  }

  @Override
  public Void visitVar(VarExpression expr) {
    myContext.appendId(expr.name);
    return null;
  }

  @Override
  public Void visitZero(ZeroExpression expr) {
    return null;
  }
}
