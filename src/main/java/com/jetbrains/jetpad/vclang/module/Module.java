package com.jetbrains.jetpad.vclang.module;

import java.util.Arrays;

public class Module {
  private final Namespace myParent;
  private final String myName;

  public Module(Namespace parent, String name) {
    myParent = parent;
    myName = name;
  }

  public Namespace getParent() {
    return myParent;
  }

  public String getName() {
    return myName;
  }

  @Override
  public boolean equals(Object other) {
    return this == other || other instanceof Module && myParent == ((Module) other).myParent && myName.equals(((Module) other).myName);
  }

  private String toString(Namespace namespace) {
    return namespace == null || namespace.getParent() == null ? "" : toString(namespace.getParent()) + namespace.getName().name + ".";
  }

  @Override
  public String toString() {
    return toString(myParent) + myName;
  }

  @Override
  public int hashCode() {
    return Arrays.hashCode(new Object[] { myParent, myName });
  }
}
