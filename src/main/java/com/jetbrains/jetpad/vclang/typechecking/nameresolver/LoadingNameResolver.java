package com.jetbrains.jetpad.vclang.typechecking.nameresolver;

import com.jetbrains.jetpad.vclang.module.*;

public class LoadingNameResolver implements NameResolver {
  private final ModuleLoader myModuleLoader;
  private final NameResolver myNameResolver;

  public LoadingNameResolver(ModuleLoader moduleLoader, NameResolver nameResolver) {
    myModuleLoader = moduleLoader;
    myNameResolver = nameResolver;
  }

  @Override
  public DefinitionPair locateName(String name) {
    DefinitionPair member = myNameResolver.locateName(name);
    if (member != null) {
      if (member.definition == null && member.abstractDefinition == null) {
        myModuleLoader.load(member.namespace.getParent(), member.namespace.getName().name, true);
      }
      return member;
    }

    ModuleLoadingResult result = myModuleLoader.load(RootModule.ROOT, name, true);
    if (result == null) {
      return null;
    } else {
      return new DefinitionPair(result.namespace, result.classDefinition, null); // TODO: return Abstract.Definition
    }
  }

  @Override
  public DefinitionPair getMember(Namespace parent, String name) {
    DefinitionPair member = parent.getMember(name);
    if (member == null) {
      return null;
    }

    if (member.definition == null && member.abstractDefinition == null) {
      ModuleLoadingResult result = myModuleLoader.load(parent, name, true);
      if (result != null) {
        // TODO: return Abstract.Definition
        member.definition = result.classDefinition;
      }
    }

    return member;
  }
}
