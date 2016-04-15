package com.jetbrains.jetpad.vclang.naming.scope;

import com.jetbrains.jetpad.vclang.naming.namespace.ClassNamespace;

public class ClassScope extends SubScope {
  public ClassScope(ClassNamespace myNamespace, Scope parentScope) {
    super(parentScope, myNamespace);
  }
}