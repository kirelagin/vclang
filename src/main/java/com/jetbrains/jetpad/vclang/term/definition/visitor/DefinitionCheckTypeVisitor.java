package com.jetbrains.jetpad.vclang.term.definition.visitor;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.definition.*;
import com.jetbrains.jetpad.vclang.typechecking.error.reporter.ErrorReporter;

public class DefinitionCheckTypeVisitor implements AbstractDefinitionVisitor<Void, Definition> {
  private final ErrorReporter myErrorReporter;

  public DefinitionCheckTypeVisitor(ErrorReporter errorReporter) {
    myErrorReporter = errorReporter;
  }

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
