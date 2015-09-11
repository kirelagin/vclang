package com.jetbrains.jetpad.vclang.typechecking.nameresolver;

import com.jetbrains.jetpad.vclang.module.DefinitionPair;
import com.jetbrains.jetpad.vclang.module.Namespace;

public class NamespaceNameResolver implements NameResolver {
  private final Namespace myNamespace;

  public NamespaceNameResolver(Namespace namespace) {
    myNamespace = namespace;
  }

  protected Namespace getNamespace() {
    return myNamespace;
  }

  @Override
  public DefinitionPair locateName(String name) {
    return myNamespace.getMember(name);
  }

  @Override
  public DefinitionPair getMember(Namespace parent, String name) {
    return parent.getMember(name);
  }
}
