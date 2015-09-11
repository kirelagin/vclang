package com.jetbrains.jetpad.vclang.typechecking.nameresolver;

import com.jetbrains.jetpad.vclang.module.DefinitionPair;
import com.jetbrains.jetpad.vclang.module.Namespace;

import java.util.ArrayList;
import java.util.List;

public class CompositeNameResolver implements NameResolver {
  private final List<NameResolver> myNameResolvers;

  public CompositeNameResolver() {
    myNameResolvers = new ArrayList<>(2);
  }

  public CompositeNameResolver(List<NameResolver> nameResolvers) {
    myNameResolvers = nameResolvers;
  }

  public void pushNameResolver(NameResolver nameResolver) {
    myNameResolvers.add(nameResolver);
  }

  public void popNameResolver() {
    myNameResolvers.remove(myNameResolvers.size() - 1);
  }

  @Override
  public DefinitionPair locateName(String name) {
    for (int i = myNameResolvers.size() - 1; i >= 0; --i) {
      DefinitionPair result = myNameResolvers.get(i).locateName(name);
      if (result != null) {
        return result;
      }
    }
    return null;
  }

  @Override
  public DefinitionPair getMember(Namespace parent, String name) {
    for (int i = myNameResolvers.size() - 1; i >= 0; --i) {
      DefinitionPair result = myNameResolvers.get(i).getMember(parent, name);
      if (result != null) {
        return result;
      }
    }
    return null;
  }
}
