package com.jetbrains.jetpad.vclang.typechecking.nameresolver;

import com.jetbrains.jetpad.vclang.module.ModuleLoader;
import com.jetbrains.jetpad.vclang.module.ModuleLoadingResult;
import com.jetbrains.jetpad.vclang.module.Namespace;
import com.jetbrains.jetpad.vclang.module.RootModule;
import com.jetbrains.jetpad.vclang.term.definition.Definition;
import com.jetbrains.jetpad.vclang.term.definition.NamespaceMember;
import com.jetbrains.jetpad.vclang.term.expr.arg.Utils;

public class LoadingNameResolver implements NameResolver {
  private final ModuleLoader myModuleLoader;
  private final NameResolver myNameResolver;

  public LoadingNameResolver(ModuleLoader moduleLoader, NameResolver nameResolver) {
    myModuleLoader = moduleLoader;
    myNameResolver = nameResolver;
  }

  @Override
  public NamespaceMember locateName(String name) {
    NamespaceMember member = myNameResolver.locateName(name);
    if (member instanceof Definition) {
      return member;
    }

    ModuleLoadingResult result;
    if (member instanceof Namespace) {
      result = myModuleLoader.load((Namespace) member, true);
    } else {
      result = myModuleLoader.load(RootModule.ROOT.getChild(new Utils.Name(name)), true);
    }

    if (result != null && result.classDefinition != null) {
      return result.classDefinition;
    } else {
      return member;
    }
  }

  @Override
  public NamespaceMember getMember(Namespace parent, String name) {
    Definition definition = parent.getDefinition(name);
    if (definition != null) {
      return definition;
    }

    Namespace child = parent.getChild(new Utils.Name(name));
    ModuleLoadingResult result = myModuleLoader.load(child, true);
    if (result != null && result.classDefinition != null) {
      return result.classDefinition;
    } else {
      return child;
    }
  }
}
