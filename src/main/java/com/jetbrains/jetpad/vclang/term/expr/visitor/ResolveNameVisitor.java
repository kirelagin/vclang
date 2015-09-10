package com.jetbrains.jetpad.vclang.term.expr.visitor;

import com.jetbrains.jetpad.vclang.term.Abstract;

import java.util.List;

public class ResolveNameVisitor implements AbstractExpressionVisitor<Void, Void> {
  private final List<String> myContext;

  public ResolveNameVisitor(List<String> context) {
    myContext = context;
  }

  @Override
  public Void visitApp(Abstract.AppExpression expr, Void params) {
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
    return null;
  }

  @Override
  public Void visitPi(Abstract.PiExpression expr, Void params) {
    return null;
  }

  @Override
  public Void visitUniverse(Abstract.UniverseExpression expr, Void params) {
    return null;
  }

  @Override
  public Void visitVar(Abstract.VarExpression expr, Void params) {
    return null;
  }

  @Override
  public Void visitInferHole(Abstract.InferHoleExpression expr, Void params) {
    return null;
  }

  @Override
  public Void visitError(Abstract.ErrorExpression expr, Void params) {
    return null;
  }

  @Override
  public Void visitTuple(Abstract.TupleExpression expr, Void params) {
    return null;
  }

  @Override
  public Void visitSigma(Abstract.SigmaExpression expr, Void params) {
    return null;
  }

  @Override
  public Void visitBinOp(Abstract.BinOpExpression expr, Void params) {
    return null;
  }

  @Override
  public Void visitBinOpSequence(Abstract.BinOpSequenceExpression expr, Void params) {
    return null;
  }

  @Override
  public Void visitElim(Abstract.ElimExpression expr, Void params) {
    return null;
  }

  @Override
  public Void visitCase(Abstract.CaseExpression expr, Void params) {
    return null;
  }

  @Override
  public Void visitProj(Abstract.ProjExpression expr, Void params) {
    return null;
  }

  @Override
  public Void visitClassExt(Abstract.ClassExtExpression expr, Void params) {
    return null;
  }

  @Override
  public Void visitNew(Abstract.NewExpression expr, Void params) {
    return null;
  }

  @Override
  public Void visitLet(Abstract.LetExpression letExpression, Void params) {
    return null;
  }
}
