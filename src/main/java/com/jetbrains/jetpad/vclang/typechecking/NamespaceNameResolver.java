package com.jetbrains.jetpad.vclang.typechecking;

import com.jetbrains.jetpad.vclang.term.definition.Namespace;
import com.jetbrains.jetpad.vclang.term.definition.NamespaceMember;

public class NamespaceNameResolver extends BaseNameResolver {
  private final Namespace myNamespace;

  public NamespaceNameResolver(Namespace namespace) {
    myNamespace = namespace;
  }

  @Override
  public NamespaceMember locateName(String name) {
    return myNamespace.locateName(name);
  }
}
