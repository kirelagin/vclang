package com.jetbrains.jetpad.vclang.typechecking.nameresolver;

import com.jetbrains.jetpad.vclang.module.DefinitionPair;
import com.jetbrains.jetpad.vclang.module.Namespace;

public class DeepNamespaceNameResolver extends NamespaceNameResolver {
  public DeepNamespaceNameResolver(Namespace namespace) {
    super(namespace);
  }

  @Override
  public DefinitionPair locateName(String name) {
    return getNamespace().locateName(name);
  }
}
