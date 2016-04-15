package com.jetbrains.jetpad.vclang.term.definition;

import com.jetbrains.jetpad.vclang.typechecking.implicitargs.equations.Equations;

public interface Universe {
  enum Cmp { EQUALS, LESS, GREATER, NOT_COMPARABLE }

  interface Level<L> {
    boolean equals(L other, Equations equations);
    Cmp compare(L other);
    L max(L other);
    L succ();
  }

  interface LeveledUniverseFactory<U extends Universe, L extends Level<L>> {
    U createUniverse(L level);
  }

  class CompareResult {
    public Universe MaxUniverse;
    public Cmp Result;

    public CompareResult(Universe maxUniverse, Cmp result) {
      MaxUniverse = maxUniverse;
      Result = result;
    }

    public boolean isLessOrEquals() { return Result == Cmp.LESS || Result == Cmp.EQUALS; }
  }

  boolean equals(Universe other, Equations equations);
  CompareResult compare(Universe other);
  Universe succ();
}
