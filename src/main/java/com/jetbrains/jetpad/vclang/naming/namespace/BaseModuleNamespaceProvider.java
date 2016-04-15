package com.jetbrains.jetpad.vclang.naming.namespace;

import com.jetbrains.jetpad.vclang.module.ModulePath;

public abstract class BaseModuleNamespaceProvider implements ModuleNamespaceProvider {
  private final ModuleNamespace myRoot = new ModuleNamespace();

  @Override
  public ModuleNamespace root() {
    return myRoot;
  }

  protected static ModuleNamespace ensureModuleNamespace(ModuleNamespace rootNamespace, ModulePath modulePath) {
    ModulePath parentPath = modulePath.getParent();
    if (parentPath == null) {
      return rootNamespace;
    }
    ModuleNamespace parentNs = ensureModuleNamespace(rootNamespace, parentPath);
    return parentNs.ensureSubmoduleNamespace(modulePath.getName());
  }
}
