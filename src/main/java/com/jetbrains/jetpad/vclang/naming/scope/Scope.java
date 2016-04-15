package com.jetbrains.jetpad.vclang.naming.scope;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.definition.Name;

import java.util.Set;

public interface Scope {
  Set<String> getGlobalNames();
  Abstract.Definition resolveGlobalDefinition(String name);

  class MergeScopeException extends RuntimeException {
    private MergeScopeException() {
    }

    public MergeScopeException(Name name) {
      super(name + " is already defined");
    }
  }
}
