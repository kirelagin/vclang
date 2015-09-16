package com.jetbrains.jetpad.vclang.module;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.definition.Definition;

public class DefinitionPair {
  public Namespace namespace;
  public Abstract.Definition abstractDefinition;
  public Definition definition;

  public DefinitionPair(Namespace namespace, Abstract.Definition abstractDefinition, Definition definition) {
    this.namespace = namespace;
    this.abstractDefinition = abstractDefinition;
    this.definition = definition;
  }

  public Abstract.Definition.Precedence getPrecedence() {
    return definition != null ? definition.getPrecedence() : abstractDefinition != null ? abstractDefinition.getPrecedence() : null;
  }
}
