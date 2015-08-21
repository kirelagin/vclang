package com.jetbrains.jetpad.vclang.module;

import com.jetbrains.jetpad.vclang.term.Prelude;
import com.jetbrains.jetpad.vclang.term.expr.arg.Utils;

public class RootModule {
  private final Namespace myRoot = new Namespace(new Utils.Name("\\root"), null);

  public RootModule() {
    Prelude.PRELUDE.setParent(myRoot);
    myRoot.addChild(Prelude.PRELUDE);
  }

  public Namespace getRoot() {
    return myRoot;
  }
}
