package com.jetbrains.jetpad.vclang.frontend.resolving;

import com.jetbrains.jetpad.vclang.error.ErrorReporter;
import com.jetbrains.jetpad.vclang.term.Abstract;

public interface ResolveListener extends ErrorReporter {
  void nameResolved(Abstract.DefCallExpression defCallExpression, Abstract.Definition definition);
  void moduleResolved(Abstract.ModuleCallExpression moduleCallExpression, Abstract.Definition definition);
  void nsCmdResolved(Abstract.NamespaceCommandStatement nsCmdStatement, Abstract.Definition definition);
  void implementResolved(Abstract.Implementation identifier, Abstract.ClassField definition);
  void implementResolved(Abstract.ClassFieldImpl identifier, Abstract.ClassField definition);
  void classViewResolved(Abstract.ClassView classView, Abstract.ClassField classifyingField);
  void classViewFieldResolved(Abstract.ClassViewField field, Abstract.ClassField definition);
  void classViewInstanceResolved(Abstract.ClassViewInstance instance, Abstract.Definition classifyingDefinition);

  Abstract.BinOpExpression makeBinOp(Abstract.BinOpSequenceExpression binOpExpr, Abstract.Expression left, Abstract.Definition binOp, Abstract.DefCallExpression var, Abstract.Expression right);
  Abstract.Expression makeError(Abstract.BinOpSequenceExpression binOpExpr, Abstract.SourceNode node);
  void replaceBinOp(Abstract.BinOpSequenceExpression binOpExpr, Abstract.Expression expression);
  void replaceWithConstructor(Abstract.PatternArgument patternArg);
  void replaceWithConstructor(Abstract.PatternContainer container, int index);
  void patternResolved(Abstract.ConstructorPattern pattern, Abstract.Constructor definition);
}
