package com.jetbrains.jetpad.vclang.naming.namespace;

import com.jetbrains.jetpad.vclang.module.ModulePath;
import com.jetbrains.jetpad.vclang.term.Abstract;

import java.util.HashMap;

public class HashMapModuleNamespaceProvider extends BaseModuleNamespaceProvider {
  private final HashMap<Abstract.ClassDefinition, ModuleNamespace> myMap = new HashMap<>();

  @Override
  public ModuleNamespace forModule(Abstract.ClassDefinition definition) {
    return myMap.get(definition);
  }

  public ModuleNamespace registerModule(ModuleNamespace rootNamespace, ModulePath modulePath, Abstract.ClassDefinition module) {
    if (module.getKind() != Abstract.ClassDefinition.Kind.Class) throw new IllegalArgumentException();
    if (myMap.get(module) != null) throw new IllegalStateException();
    ModuleNamespace ns = ensureModuleNamespace(rootNamespace, modulePath);
    myMap.put(module, ns);
    ns.registerClass(module);
    return ns;
  }
}
