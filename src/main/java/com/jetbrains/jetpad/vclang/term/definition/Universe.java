package com.jetbrains.jetpad.vclang.term.definition;

import com.jetbrains.jetpad.vclang.term.expr.Expression;
import com.jetbrains.jetpad.vclang.term.expr.ExpressionFactory;
import com.jetbrains.jetpad.vclang.typechecking.implicitargs.equations.Equations;

import java.util.ArrayList;
import java.util.List;

public interface Universe {
  enum Cmp { EQUALS, LESS, GREATER, NOT_COMPARABLE }

  interface Level<L, D> {
    boolean equals(L other, Equations equations);
    D getDiff(L other, Equations equations);
    Cmp compare(L other);
    L max(L other);
    L succ();
  }

  interface LeveledUniverseFactory<U extends Universe, D, L extends Level<L, D>> {
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

  class TypeUniPair {
    public Expression Type;
    public Universe Uni;

    public TypeUniPair(Expression type, Universe uni) {
      Type = type;
      Uni = uni;
    }
  }

  class Lifts {
    public LiftApplier Left;
    public LiftApplier Right;
    public Universe MaxUniverse;

    interface LiftApplier {
      Expression apply(Expression type);
    }

    public Lifts() {}

    public Lifts(LiftApplier left, LiftApplier right, Universe maxUniverse) {
      Left = left;
      Right = right;
      MaxUniverse = maxUniverse;
    }

    public static boolean equalize(List<TypeUniPair> leftUniSeq, TypeUniPair rightUni, Equations equations) {
      if (leftUniSeq.isEmpty() || rightUni.Uni.equals(TypeUniverse.PROP) || rightUni.Uni.equals(TypeUniverse.SET)) {
        leftUniSeq.add(rightUni);
        return true;
      }
      TypeUniPair firstNotSet = null;
      for (TypeUniPair leftUni : leftUniSeq) {
        if (!leftUni.Uni.equals(TypeUniverse.PROP) && !leftUni.Uni.equals(TypeUniverse.SET)) {
          firstNotSet = leftUni;
          break;
        }
      }
      if (firstNotSet == null) {
        leftUniSeq.add(rightUni);
        return true;
      }
      Lifts lifts = firstNotSet.Uni.equalsWithLift(rightUni.Uni, equations);
      if (lifts == null) {
        return false;
      }
      if (lifts.Left != null) {
        for (TypeUniPair leftUni : leftUniSeq) {
          if (leftUni.Uni.equals(TypeUniverse.PROP) || leftUni.Uni.equals(TypeUniverse.SET)) {
            continue;
          }
          leftUni.Type = lifts.Left.apply(leftUni.Type);
          leftUni.Uni = lifts.MaxUniverse;
        }
      }
      if (lifts.Right != null) {
        rightUni.Type = lifts.Right.apply(rightUni.Type);
        rightUni.Uni = lifts.MaxUniverse;
      }
      leftUniSeq.add(rightUni);
      return true;
    }

    /*public static ArrayList<Expression> equalizeUniverses(ArrayList<Universe> universes, Equations equations) {
      Universe lastUniverse = null;
      ArrayList<Expression> lifts = new ArrayList<>();
      for (Universe universe : universes) {
        if (lastUniverse == null) {
          lastUniverse = universe;
          continue;
        }
        Lifts lft = lastUniverse.equalsWithLift(universe, equations);
        if (lft == null) {
          return null;
        }
        lifts.add(lft.Left);
        lifts.add(lft.Right);
        lastUniverse = universe;
      }
      return lifts;
    }

    public static ArrayList<Expression> applyLifts() {} /**/
  }

  boolean equals(Universe other, Equations equations);
  Lifts equalsWithLift(Universe other, Equations equations);
  CompareResult compare(Universe other);
  Universe succ();
}
