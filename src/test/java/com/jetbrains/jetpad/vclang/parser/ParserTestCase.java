package com.jetbrains.jetpad.vclang.parser;

import com.jetbrains.jetpad.vclang.module.RootModule;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.Concrete;
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

  public static Concrete.Expression parseExpr(String text, int errors) {
    RootModule.initialize();
    ListErrorReporter errorReporter = new ListErrorReporter();
    Concrete.Expression result = new BuildVisitor(errorReporter).visitExpr(parse(errorReporter, text).expr());
    assertEquals(errors, errorReporter.getErrorList().size());
    return result;
  }

  public static Concrete.Expression parseExpr(String text) {
    return parseExpr(text, 0);
  }

  public static Concrete.Definition parseDef(String text) {
    return parseDef(text, 0);
  }

  public static Concrete.Definition parseDef(String text, int errors) {
    RootModule.initialize();
    ListErrorReporter errorReporter = new ListErrorReporter();
    Concrete.Definition definition = new BuildVisitor(errorReporter).visitDefinition(parse(errorReporter, text).definition());
    assertEquals(errors, errorReporter.getErrorList().size());
    return definition;
  }

  public static Concrete.ClassDefinition parseClass(String text) {
    return parseClass(text, 0);
  }

  public static Concrete.ClassDefinition parseClass(String text, int errors) {
    RootModule.initialize();
    ListErrorReporter errorReporter = new ListErrorReporter();
    List<Concrete.Statement> statements = new BuildVisitor(errorReporter).visitStatements(parse(errorReporter, text).statements());
    Concrete.ClassDefinition classDefinition = new Concrete.ClassDefinition(null, "test", statements);
    assertEquals(errors, errorReporter.getErrorList().size());
    return classDefinition;
  }

  public static boolean compare(Expression expr1, Abstract.Expression expr2) {
    List<CompareVisitor.Equation> equations = new ArrayList<>();
    CompareVisitor.Result result = Expression.compare(expr2, expr1, equations);
    return result.isOK() != CompareVisitor.CMP.NOT_EQUIV && equations.size() == 0;
  }
}
