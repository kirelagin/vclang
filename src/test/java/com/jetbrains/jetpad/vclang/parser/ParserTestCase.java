package com.jetbrains.jetpad.vclang.parser;

import com.jetbrains.jetpad.vclang.module.ModuleLoader;
import com.jetbrains.jetpad.vclang.module.Namespace;
import com.jetbrains.jetpad.vclang.module.RootModule;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.Concrete;
import com.jetbrains.jetpad.vclang.term.definition.ClassDefinition;
import com.jetbrains.jetpad.vclang.term.definition.Definition;
import com.jetbrains.jetpad.vclang.term.expr.Expression;
import com.jetbrains.jetpad.vclang.term.expr.arg.Utils;
import com.jetbrains.jetpad.vclang.term.expr.visitor.CompareVisitor;
import com.jetbrains.jetpad.vclang.typechecking.error.ErrorReporter;
import com.jetbrains.jetpad.vclang.typechecking.error.ListErrorReporter;
import org.antlr.v4.runtime.*;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class ParserTestCase {
  public static VcgrammarParser parse(final ErrorReporter errorReporter, String text) {
    ANTLRInputStream input = new ANTLRInputStream(text);
    VcgrammarLexer lexer = new VcgrammarLexer(input);
    CommonTokenStream tokens = new CommonTokenStream(lexer);
    VcgrammarParser parser = new VcgrammarParser(tokens);
    parser.removeErrorListeners();
    parser.addErrorListener(new BaseErrorListener() {
      @Override
      public void syntaxError(Recognizer<?, ?> recognizer, Object o, int line, int pos, String msg, RecognitionException e) {
        errorReporter.report(new ParserError(RootModule.ROOT.getChild(new Utils.Name("test")), new Concrete.Position(line, pos), msg));
      }
    });
    return parser;
  }

  public static Concrete.Expression parseExpr(ListErrorReporter errorReporter, String text, int errors) {
    Namespace namespace = RootModule.ROOT.getChild(new Utils.Name("test"));
    Concrete.Expression result = new BuildVisitor(namespace, new Namespace(null, null), moduleLoader, errorReporter).visitExpr(parse(errorReporter, text).expr());
    assertEquals(errors, errorReporter.getErrorList().size());
    return result;
  }

  public static Concrete.Expression parseExpr(ModuleLoader moduleLoader, String text) {
    return parseExpr(moduleLoader, text, 0);
  }

  public static Definition parseDef(ModuleLoader moduleLoader, String text) {
    return parseDef(moduleLoader, text, 0);
  }

  public static Definition parseDef(ModuleLoader moduleLoader, String text, int errors) {
    Namespace namespace = moduleLoader.getRoot().getChild(new Utils.Name("test"));
    Definition result = new BuildVisitor(namespace, new Namespace(null, null), moduleLoader).visitDef(parse(moduleLoader, text).def());
    assertEquals(0, moduleLoader.getErrors().size());
    assertEquals(errors, moduleLoader.getTypeCheckingErrors().size());
    return result;
  }

  public static ClassDefinition parseDefs(ModuleLoader moduleLoader, String text) {
    return parseDefs(moduleLoader, text, 0);
  }

  public static ClassDefinition parseDefs(ModuleLoader moduleLoader, String text, int errors) {
    return parseDefs(moduleLoader, text, 0, errors);
  }

  public static ClassDefinition parseDefs(ModuleLoader moduleLoader, String text, int moduleErrors, int errors) {
    Namespace namespace = RootModule.ROOT.getChild(new Utils.Name("test"));
    ClassDefinition result = new ClassDefinition(namespace);
    new BuildVisitor(namespace, result.getLocalNamespace(), moduleLoader).visitDefs(parse(moduleLoader, text).defs());
    assertEquals(moduleErrors, moduleLoader.getErrors().size());
    assertEquals(errors, moduleLoader.getTypeCheckingErrors().size());
    return result;
  }

  public static boolean compare(Expression expr1, Abstract.Expression expr2) {
    List<CompareVisitor.Equation> equations = new ArrayList<>();
    CompareVisitor.Result result = Expression.compare(expr2, expr1, equations);
    return result.isOK() != CompareVisitor.CMP.NOT_EQUIV && equations.size() == 0;
  }
}
