package com.jetbrains.jetpad.vclang.typechecking;

import com.jetbrains.jetpad.vclang.term.definition.Namespace;
import com.jetbrains.jetpad.vclang.term.definition.NamespaceMember;

public interface NameResolver {
  NamespaceMember locateName(String name);
  NamespaceMember getMember(Namespace parent, String name);
}
