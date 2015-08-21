package com.jetbrains.jetpad.vclang.typechecking.nameresolver;

import com.jetbrains.jetpad.vclang.module.Namespace;
import com.jetbrains.jetpad.vclang.term.definition.NamespaceMember;

public class NamespaceNameResolver implements NameResolver {
  private final Namespace myNamespace;

  public NamespaceNameResolver(Namespace namespace) {
    myNamespace = namespace;
  }

  @Override
  public NamespaceMember locateName(String name) {
    return myNamespace.locateName(name);
  }

  @Override
  public NamespaceMember getMember(Namespace parent, String name) {
    return parent.getMember(name);
  }
}
