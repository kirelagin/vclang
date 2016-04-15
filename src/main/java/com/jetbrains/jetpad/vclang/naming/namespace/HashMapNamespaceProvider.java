package com.jetbrains.jetpad.vclang.naming.namespace;

import com.jetbrains.jetpad.vclang.term.Abstract;

import java.util.HashMap;

public class HashMapNamespaceProvider<K, V> extends HashMap<Abstract.Definition, NameSpace> implements NamespaceProvider {
  @Override
  public NameSpace forDefinition(Abstract.Definition definition) {
    return get(definition);
  }
}
