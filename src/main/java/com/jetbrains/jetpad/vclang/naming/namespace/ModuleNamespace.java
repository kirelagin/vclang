package com.jetbrains.jetpad.vclang.naming.namespace;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.definition.Name;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ModuleNamespace implements NameSpace {
  private final Map<String, ModuleNamespace> mySubmoduleNamespaces = new HashMap<>();
  private Abstract.ClassDefinition myRegisteredClass = null;
  private ClassNamespace myClassNamespace = null;

  @Override
  public Set<String> getGlobalNames() {
    Set<String> names = new HashSet<>(mySubmoduleNamespaces.keySet());
    if (myClassNamespace != null) names.addAll(myClassNamespace.getGlobalNames());
    return names;
  }

  @Override
  public Abstract.Definition resolveGlobalDefinition(String name) {
    ModuleNamespace submoduleNamespace = getSubmoduleNamespace(name);
    Abstract.ClassDefinition submodule = submoduleNamespace != null ? submoduleNamespace.getMyRegisteredClass() : null;
    Abstract.Definition resolved = myClassNamespace != null ? myClassNamespace.resolveGlobalDefinition(name) : null;

    if (submodule == null) return resolved;
    else if (resolved == null) return submodule;
    else throw new MergeScopeException(new Name(name));
  }

  public ModuleNamespace getSubmoduleNamespace(String submodule) {
    return mySubmoduleNamespaces.get(submodule);
  }

  public ModuleNamespace ensureSubmoduleNamespace(String submodule) {
    ModuleNamespace ns = mySubmoduleNamespaces.get(submodule);
    if (ns == null) {
      ns = new ModuleNamespace();
      mySubmoduleNamespaces.put(submodule, ns);
    }
    return ns;
  }

  public void registerClass(Abstract.ClassDefinition module) {
    if (myRegisteredClass != null) throw new IllegalStateException();
    myClassNamespace = new ClassNamespace(module);
  }

  private Abstract.ClassDefinition getMyRegisteredClass() {
    return myRegisteredClass;
  }
}
