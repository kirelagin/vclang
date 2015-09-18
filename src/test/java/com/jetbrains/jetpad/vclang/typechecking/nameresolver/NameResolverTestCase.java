package com.jetbrains.jetpad.vclang.typechecking.nameresolver;

import com.jetbrains.jetpad.vclang.module.Namespace;
import com.jetbrains.jetpad.vclang.module.RootModule;
import com.jetbrains.jetpad.vclang.term.Concrete;
import com.jetbrains.jetpad.vclang.term.definition.visitor.DefinitionResolveNameVisitor;
import com.jetbrains.jetpad.vclang.term.expr.arg.Utils;
import com.jetbrains.jetpad.vclang.term.expr.visitor.ResolveNameVisitor;
import com.jetbrains.jetpad.vclang.typechecking.error.ListErrorReporter;

import java.util.ArrayList;

import static com.jetbrains.jetpad.vclang.parser.ParserTestCase.*;
import static org.junit.Assert.assertEquals;

public class NameResolverTestCase {
  public static int resolveNamesExpr(Concrete.Expression expression, NameResolver nameResolver) {
    ListErrorReporter errorReporter = new ListErrorReporter();
    expression.accept(new ResolveNameVisitor(errorReporter, nameResolver, new ArrayList<String>(0), true), null);
    return errorReporter.getErrorList().size();
  }

  public static int resolveNamesExpr(Concrete.Expression expression) {
    return resolveNamesExpr(expression, DummyNameResolver.getInstance());
  }

  public static Concrete.Expression resolveNamesExpr(String text, int errors, NameResolver nameResolver) {
    Concrete.Expression result = parseExpr(text, 0);
    assertEquals(errors, resolveNamesExpr(result, nameResolver));
    return result;
  }

  public static Concrete.Expression resolveNamesExpr(String text, int errors) {
    Concrete.Expression result = parseExpr(text, 0);
    assertEquals(errors, resolveNamesExpr(result));
    return result;
  }

  public static Concrete.Expression resolveNamesExpr(String text, NameResolver nameResolver) {
    return resolveNamesExpr(text, 0, nameResolver);
  }

  public static Concrete.Expression resolveNamesExpr(String text) {
    return resolveNamesExpr(text, 0);
  }

  public static int resolveNamesDef(Concrete.Definition definition) {
    ListErrorReporter errorReporter = new ListErrorReporter();
    definition.accept(new DefinitionResolveNameVisitor(errorReporter, RootModule.ROOT.getChild(new Utils.Name("test")), DummyNameResolver.getInstance()), null);
    return errorReporter.getErrorList().size();
  }

  public static Concrete.Definition resolveNamesDef(String text, int errors) {
    Concrete.Definition result = parseDef(text, 0);
    assertEquals(errors, resolveNamesDef(result));
    return result;
  }

  public static Concrete.Definition resolveNamesDef(String text) {
    return resolveNamesDef(text, 0);
  }

  public static Namespace resolveNamesClass(String name, Concrete.ClassDefinition classDefinition, int errors) {
    ListErrorReporter errorReporter = new ListErrorReporter();
    Namespace localNamespace = new DefinitionResolveNameVisitor(errorReporter, RootModule.ROOT.getChild(new Utils.Name(name)), DummyNameResolver.getInstance()).visitClass(classDefinition, null);
    assertEquals(errors, errorReporter.getErrorList().size());
    return localNamespace;
  }

  public static Namespace resolveNamesClass(String name, String text, int errors) {
    return resolveNamesClass(name, parseClass(text, 0), errors);
  }

  public static Namespace resolveNamesClass(String name, String text) {
    return resolveNamesClass(name, parseClass(text, 0), 0);
  }
}
