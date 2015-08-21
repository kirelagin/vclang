package com.jetbrains.jetpad.vclang.typechecking.nameresolver;

import com.jetbrains.jetpad.vclang.module.Module;
import com.jetbrains.jetpad.vclang.module.ModuleLoader;
import com.jetbrains.jetpad.vclang.module.Namespace;
import com.jetbrains.jetpad.vclang.term.definition.ClassDefinition;
import com.jetbrains.jetpad.vclang.term.definition.Definition;
import com.jetbrains.jetpad.vclang.term.definition.NamespaceMember;

public class LoadingNameResolver implements NameResolver {
  private final ModuleLoader myModuleLoader;
  private final NameResolver myNameResolver;

  public LoadingNameResolver(ModuleLoader moduleLoader, NameResolver nameResolver) {
    myModuleLoader = moduleLoader;
    myNameResolver = nameResolver;
  }

  @Override
  public NamespaceMember locateName(String name) {
    NamespaceMember result = myNameResolver.locateName(name);
    if (result instanceof Definition) {
      return result;
    }

    if (result != null) {
      ClassDefinition classDefinition = myModuleLoader.loadModule(new Module(result.getParent(), result.getName().name), true);
      return classDefinition != null ? classDefinition : result;
    } else {
      return myModuleLoader.loadModule(new Module(myModuleLoader.getRoot(), name), true);
    }
  }

  @Override
  public NamespaceMember getMember(Namespace parent, String name) {
    return null;
  }
}
