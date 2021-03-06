package com.jetbrains.jetpad.vclang.typechecking.implicitargs.equations;

import com.jetbrains.jetpad.vclang.core.context.binding.inference.InferenceLevelVariable;
import com.jetbrains.jetpad.vclang.core.context.binding.inference.InferenceVariable;
import com.jetbrains.jetpad.vclang.core.expr.Expression;
import com.jetbrains.jetpad.vclang.core.sort.Level;
import com.jetbrains.jetpad.vclang.core.subst.LevelSubstitution;
import com.jetbrains.jetpad.vclang.term.Abstract;

public interface Equations {
  boolean add(Expression expr1, Expression expr2, CMP cmp, Abstract.SourceNode sourceNode, InferenceVariable stuckVar);
  boolean solve(Expression type, Expression expr, CMP cmp, Abstract.SourceNode sourceNode);
  boolean add(Level level1, Level level2, CMP cmp, Abstract.SourceNode sourceNode);
  boolean add(Expression type, Expression expr, Abstract.SourceNode sourceNode, InferenceVariable stuckVar);
  boolean addVariable(InferenceLevelVariable var);
  void bindVariables(InferenceLevelVariable pVar, InferenceLevelVariable hVar);
  void remove(Equation equation);
  LevelSubstitution solve(Abstract.SourceNode sourceNode);

  enum CMP {
    LE, EQ, GE;

    public CMP not() {
      if (this == LE) return GE;
      if (this == GE) return LE;
      return this;
    }
  }
}
