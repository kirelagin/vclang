package com.jetbrains.jetpad.vclang.model.visitor;

import com.jetbrains.jetpad.vclang.model.expr.*;

public interface ExpressionVisitor<R> {
  R visitApp(AppExpression expr);
  R visitLam(LamExpression expr);
  R visitNat(NatExpression expr);
  R visitNelim(NelimExpression expr);
  R visitPi(PiExpression expr);
  R visitSuc(SucExpression expr);
  R visitUniverse(UniverseExpression expr);
  R visitVar(VarExpression expr);
  R visitZero(ZeroExpression expr);
}
