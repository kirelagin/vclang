package com.jetbrains.jetpad.vclang.typechecking;

import com.jetbrains.jetpad.vclang.term.Concrete;
import com.jetbrains.jetpad.vclang.term.definition.Binding;
import com.jetbrains.jetpad.vclang.term.definition.ClassDefinition;
import com.jetbrains.jetpad.vclang.term.definition.Definition;
import com.jetbrains.jetpad.vclang.term.definition.visitor.DefinitionCheckTypeVisitor;
import com.jetbrains.jetpad.vclang.term.expr.Expression;
import com.jetbrains.jetpad.vclang.term.expr.visitor.CheckTypeVisitor;
import com.jetbrains.jetpad.vclang.typechecking.error.ErrorReporter;
import com.jetbrains.jetpad.vclang.typechecking.error.ListErrorReporter;

import java.util.ArrayList;

import static com.jetbrains.jetpad.vclang.parser.ParserTestCase.*;
import static org.junit.Assert.assertEquals;

public class TypeCheckingTestCase {
  public static CheckTypeVisitor.Result typeCheckExpr(Concrete.Expression expression, Expression expectedType, ErrorReporter errorReporter) {
    return expression.accept(new CheckTypeVisitor(new ArrayList<Binding>(0), errorReporter, CheckTypeVisitor.Side.RHS), expectedType);
  }

  public static CheckTypeVisitor.Result typeCheckExpr(Concrete.Expression expression, Expression expectedType, int errors) {
    ListErrorReporter errorReporter = new ListErrorReporter();
    CheckTypeVisitor.Result result = typeCheckExpr(expression, expectedType, errorReporter);
    assertEquals(errorReporter.getErrorList().toString(), errors, errorReporter.getErrorList().size());
    return result;
  }

  public static CheckTypeVisitor.Result typeCheckExpr(String text, Expression expectedType, ErrorReporter errorReporter) {
    return typeCheckExpr(parseExpr(text, 0), expectedType, errorReporter);
  }

  public static CheckTypeVisitor.Result typeCheckExpr(String text, Expression expectedType, int errors) {
    return typeCheckExpr(parseExpr(text, 0), expectedType, errors);
  }

  public static CheckTypeVisitor.Result typeCheckExpr(String text, Expression expectedType) {
    return typeCheckExpr(parseExpr(text, 0), expectedType, 0);
  }

  public static Definition typeCheckDef(Concrete.Definition definition, int errors) {
    ListErrorReporter errorReporter = new ListErrorReporter();
    Definition result = definition.accept(new DefinitionCheckTypeVisitor(errorReporter), null);
    assertEquals(errorReporter.getErrorList().toString(), errors, errorReporter.getErrorList().size());
    return result;
  }

  public static Definition typeCheckDef(String text, int errors) {
    return typeCheckDef(parseDef(text, 0), errors);
  }

  public static Definition typeCheckDef(String text) {
    return typeCheckDef(parseDef(text, 0), 0);
  }

  public static ClassDefinition typeCheckClass(Concrete.ClassDefinition classDefinition, int errors) {
    ListErrorReporter errorReporter = new ListErrorReporter();
    ClassDefinition result = new DefinitionCheckTypeVisitor(errorReporter).visitClass(classDefinition, null);
    assertEquals(errorReporter.getErrorList().toString(), errors, errorReporter.getErrorList().size());
    return result;
  }

  public static ClassDefinition typeCheckClass(String text, int errors) {
    return typeCheckClass(parseClass("test", text, 0), errors);
  }

  public static ClassDefinition typeCheckClass(String text) {
    return typeCheckClass(parseClass("test", text, 0), 0);
  }
}
