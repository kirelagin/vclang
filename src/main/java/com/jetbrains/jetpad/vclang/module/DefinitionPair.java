package com.jetbrains.jetpad.vclang.module;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.definition.Definition;

public class DefinitionPair {
  public Namespace namespace;
  public Definition definition;
  public Abstract.Definition abstractDefinition;

  public DefinitionPair(Namespace namespace, Definition definition, Abstract.Definition abstractDefinition) {
    this.namespace = namespace;
    this.definition = definition;
    this.abstractDefinition = abstractDefinition;
  }
}
