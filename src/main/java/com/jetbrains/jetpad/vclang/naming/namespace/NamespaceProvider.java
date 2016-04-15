package com.jetbrains.jetpad.vclang.naming.namespace;

import com.jetbrains.jetpad.vclang.term.Abstract;

public interface NamespaceProvider {
  NameSpace forDefinition(Abstract.Definition definition);
}
