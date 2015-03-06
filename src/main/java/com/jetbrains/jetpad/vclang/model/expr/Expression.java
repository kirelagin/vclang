package com.jetbrains.jetpad.vclang.model.expr;

import com.jetbrains.jetpad.vclang.model.Node;
import com.jetbrains.jetpad.vclang.model.visitor.ExpressionVisitor;
import com.jetbrains.jetpad.vclang.term.expr.Abstract;

public abstract class Expression extends Node implements Abstract.Expression {
  public abstract <R> R accept(ExpressionVisitor<? extends R> visitor);
  public abstract Expression copy();
}
