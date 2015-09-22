package com.jetbrains.jetpad.vclang.module;

import com.jetbrains.jetpad.vclang.typechecking.error.GeneralError;

public interface ModuleLoader {
  ModuleLoadingResult load(Namespace parent, String name, boolean tryLoad);
  void loadingError(GeneralError error);
  void loadingSucceeded(Namespace namespace, DefinitionPair definition, boolean compiled);
}
