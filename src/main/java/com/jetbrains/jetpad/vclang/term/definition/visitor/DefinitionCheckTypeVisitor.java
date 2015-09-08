package com.jetbrains.jetpad.vclang.term.definition.visitor;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.definition.*;

public class DefinitionCheckTypeVisitor implements AbstractDefinitionVisitor<Void, Definition> {
  @Override
  public FunctionDefinition visitFunction(Abstract.FunctionDefinition def, Void params) {
    return null;
  }

  @Override
  public DataDefinition visitData(Abstract.DataDefinition def, Void params) {
    return null;
  }

  @Override
  public Constructor visitConstructor(Abstract.Constructor def, Void params) {
    return null;
  }

  @Override
  public ClassDefinition visitClass(Abstract.ClassDefinition def, Void params) {
    return null;
  }
}
