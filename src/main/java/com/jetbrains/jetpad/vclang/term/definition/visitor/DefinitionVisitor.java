package com.jetbrains.jetpad.vclang.term.definition.visitor;

import com.jetbrains.jetpad.vclang.term.definition.*;

public interface DefinitionVisitor<P, R> {
  R visitFunction(FunctionDefinition def, P params);
  R visitClassDefinition(ClassDefinition def, P params);
  R visitClassField(ClassField def, P params);
  R visitConstructor(Constructor def, P params);
  R visitDataDefinition(DataDefinition def, P params);
}
