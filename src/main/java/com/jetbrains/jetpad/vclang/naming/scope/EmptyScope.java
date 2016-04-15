package com.jetbrains.jetpad.vclang.naming.scope;

import com.jetbrains.jetpad.vclang.term.Abstract;

import java.util.Collections;
import java.util.Set;

public class EmptyScope implements Scope {
  public static final EmptyScope EMPTY_SCOPE = new EmptyScope();

  @Override
  public Set<String> getGlobalNames() {
    return Collections.emptySet();
  }

  @Override
  public Abstract.Definition resolveGlobalDefinition(String name) {
    return null;
  }
}
