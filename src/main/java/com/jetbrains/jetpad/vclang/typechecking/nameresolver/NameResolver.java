package com.jetbrains.jetpad.vclang.typechecking.nameresolver;

import com.jetbrains.jetpad.vclang.module.DefinitionPair;
import com.jetbrains.jetpad.vclang.module.Namespace;

public interface NameResolver {
  DefinitionPair locateName(String name, boolean isStatic);
  DefinitionPair getMember(Namespace parent, String name);
}
