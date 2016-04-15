package com.jetbrains.jetpad.vclang.naming.scope;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.definition.Name;

import java.util.HashSet;
import java.util.Set;

public class MergeScope implements Scope {
  private final Scope scope1, scope2;

  public MergeScope(Scope scope1, Scope scope2) {
    this.scope1 = scope1;
    this.scope2 = scope2;
  }

  @Override
  public Set<String> getGlobalNames() {
    Set<String> names = new HashSet<>(scope1.getGlobalNames());
    names.addAll(scope2.getGlobalNames());
    return names;
  }

  @Override
  public Abstract.Definition resolveGlobalDefinition(String name) {
    Abstract.Definition def1 = scope1.resolveGlobalDefinition(name);
    Abstract.Definition def2 = scope2.resolveGlobalDefinition(name);

    if (def1 == null) return def2;
    else if (def2 == null) return def1;
    else throw new MergeScopeException(new Name(name));
  }

}
