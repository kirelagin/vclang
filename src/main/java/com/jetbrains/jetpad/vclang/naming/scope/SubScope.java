package com.jetbrains.jetpad.vclang.naming.scope;

import com.jetbrains.jetpad.vclang.term.Abstract;

import java.util.HashSet;
import java.util.Set;

public class SubScope implements Scope {
  private final Scope parent;
  private final Scope child;

  public SubScope(Scope parent, Scope child) {
    this.parent = parent;
    this.child = child;
  }

  @Override
  public Set<String> getGlobalNames() {
    Set<String> names = new HashSet<>(parent.getGlobalNames());
    names.addAll(child.getGlobalNames());
    return names;
  }

  @Override
  public Abstract.Definition resolveGlobalDefinition(String name) {
    Abstract.Definition def = child.resolveGlobalDefinition(name);
    return def != null ? def : parent.resolveGlobalDefinition(name);
  }
}
