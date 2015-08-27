package com.jetbrains.jetpad.vclang.typechecking.nameresolver;

import com.jetbrains.jetpad.vclang.module.Namespace;
import com.jetbrains.jetpad.vclang.term.definition.NamespaceMember;

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
  public NamespaceMember locateName(String name) {
    for (NameResolver nameResolver : myNameResolvers) {
      NamespaceMember result = nameResolver.locateName(name);
      if (result != null) {
        return result;
      }
    }
    return null;
  }

  @Override
  public NamespaceMember getMember(Namespace parent, String name) {
    for (NameResolver nameResolver : myNameResolvers) {
      NamespaceMember result = nameResolver.getMember(parent, name);
      if (result != null) {
        return result;
      }
    }
    return null;
  }
}
