package com.jetbrains.jetpad.vclang.naming.scope;

import com.jetbrains.jetpad.vclang.term.Abstract;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class ManualScope implements Scope {
  private final Map<String, Abstract.Definition> map;

  public ManualScope() {
    this.map = new HashMap<>();
  }

  @Override
  public Set<String> getGlobalNames() {
    return map.keySet();
  }

  @Override
  public Abstract.Definition resolveGlobalDefinition(String name) {
    return map.get(name);
  }
}
