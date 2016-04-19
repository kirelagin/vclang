package com.jetbrains.jetpad.vclang.term.definition;

import com.jetbrains.jetpad.vclang.term.expr.visitor.CompareVisitor;
import com.jetbrains.jetpad.vclang.typechecking.implicitargs.equations.DummyEquations;
import com.jetbrains.jetpad.vclang.typechecking.implicitargs.equations.Equations;

public abstract class BaseUniverse<U extends Universe, D, L extends Universe.Level<L, D>> implements Universe, Universe.LeveledUniverseFactory<U, D, L>  {
  private L myLevel = null;

  public BaseUniverse() {}

  public BaseUniverse(L level) { myLevel = level; }

  public L getLevel() { return myLevel; }

  /*
  @Override
  public boolean compare(Universe other, CompareVisitor visitor, Equations.CMP expectedCMP) {
    if (getClass() != other.getClass()) return false;
    L otherLevel = ((BaseUniverse<U, L>) other).getLevel();
    if (myLevel == null)
      return otherLevel == null || expectedCMP == Equations.CMP.GE;
    if (otherLevel == null)
      return expectedCMP == Equations.CMP.LE;
    return myLevel.compare(otherLevel, visitor, expectedCMP);
  } /**/

  @Override
  public CompareResult compare(Universe other) {
    if (getClass() != other.getClass()) return null;
    L otherLevel = ((BaseUniverse<U, D, L>) other).getLevel();
    if (myLevel == null)
      return otherLevel == null ? new CompareResult(this, Cmp.EQUALS) : new CompareResult(this, Cmp.GREATER);
    if (otherLevel == null)
      return new CompareResult(this, Cmp.LESS);
    Cmp res = myLevel.compare(otherLevel);
    if (res == Cmp.NOT_COMPARABLE)
      return new CompareResult(createUniverse(myLevel.max(otherLevel)), res);
    if (res == Cmp.LESS) return new CompareResult(other, res);
    return new CompareResult(this, res);
  }

  @Override
  public Universe succ() {
    return myLevel != null ? createUniverse(myLevel.succ()) : createUniverse(null);
  }

  @Override
  public boolean equals(Universe other, Equations equations) {
    if (getClass() != other.getClass()) return false;
    L otherLevel = ((BaseUniverse<U, D, L>) other).getLevel();
    if (myLevel == null || otherLevel == null) {
      return myLevel == otherLevel;
    }
    return myLevel.equals(otherLevel, equations);
  }

  protected abstract Lifts diffToLift(D diff, U uni1, U uni2);

  @Override
  public Lifts equalsWithLift(Universe other, Equations equations) {
    if (getClass() != other.getClass()) return null;
    L otherLevel = ((BaseUniverse<U, D, L>) other).getLevel();
    if (myLevel == null || otherLevel == null) {
      return myLevel == otherLevel ? new Lifts() : null;
    }
    D diff = myLevel.getDiff(otherLevel, equations);
    if (diff == null) {
      return null;
    }
    return diffToLift(diff, (U)this, (U)other);
  }

  @Override
  public boolean equals(Object other) {
    if (!(other instanceof Universe)) return false;
    return equals((Universe) other, DummyEquations.getInstance());
  }

}
