package com.jetbrains.jetpad.vclang.term.definition.visitor;

import com.jetbrains.jetpad.vclang.term.context.param.DependentLink;
import com.jetbrains.jetpad.vclang.term.definition.*;
import com.jetbrains.jetpad.vclang.term.expr.visitor.ValidateTypeVisitor;

public class ValidateDefinitionVisitor implements DefinitionVisitor<Void, ValidateTypeVisitor.ErrorReporter>{
  private final ValidateTypeVisitor myValidateTypeVisitor;

  public ValidateDefinitionVisitor() {
    myValidateTypeVisitor = new ValidateTypeVisitor();
  }

  @Override
  public ValidateTypeVisitor.ErrorReporter visitFunction(FunctionDefinition def, Void params) {
    for (DependentLink link = def.getParameters(); link.hasNext(); link = link.getNext()) {
      link.getType().accept(myValidateTypeVisitor, null);
    }
    def.getResultType().accept(myValidateTypeVisitor, null);
    def.getElimTree().accept(myValidateTypeVisitor, def.getResultType());
    return myValidateTypeVisitor.myErrorReporter;
  }

  @Override
  public ValidateTypeVisitor.ErrorReporter visitClassDefinition(ClassDefinition def, Void params) {
    def.getType().accept(myValidateTypeVisitor, null);
    return myValidateTypeVisitor.myErrorReporter;
  }

  @Override
  public ValidateTypeVisitor.ErrorReporter visitClassField(ClassField def, Void params) {
    def.getType().accept(myValidateTypeVisitor, null);
    return myValidateTypeVisitor.myErrorReporter;
  }

  @Override
  public ValidateTypeVisitor.ErrorReporter visitConstructor(Constructor def, Void params) {
    for (DependentLink link = def.getParameters(); link.hasNext(); link = link.getNext()) {
      link.getType().accept(myValidateTypeVisitor, null);
    }
    def.getResultType().accept(myValidateTypeVisitor, null);
    def.getElimTree().accept(myValidateTypeVisitor, def.getResultType());
    return myValidateTypeVisitor.myErrorReporter;
  }

  @Override
  public ValidateTypeVisitor.ErrorReporter visitDataDefinition(DataDefinition def, Void params) {
    def.getType().accept(myValidateTypeVisitor, null);
    return myValidateTypeVisitor.myErrorReporter;
  }
}
