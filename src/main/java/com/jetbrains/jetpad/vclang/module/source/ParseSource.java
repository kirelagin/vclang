package com.jetbrains.jetpad.vclang.module.source;

import com.jetbrains.jetpad.vclang.module.Namespace;
import com.jetbrains.jetpad.vclang.parser.BuildVisitor;
import com.jetbrains.jetpad.vclang.parser.ParserError;
import com.jetbrains.jetpad.vclang.parser.VcgrammarLexer;
import com.jetbrains.jetpad.vclang.parser.VcgrammarParser;
import com.jetbrains.jetpad.vclang.term.Concrete;
import com.jetbrains.jetpad.vclang.term.definition.ClassDefinition;
import com.jetbrains.jetpad.vclang.typechecking.error.CompositeErrorReporter;
import com.jetbrains.jetpad.vclang.typechecking.error.CountingErrorReporter;
import com.jetbrains.jetpad.vclang.typechecking.error.ErrorReporter;
import com.jetbrains.jetpad.vclang.typechecking.nameresolver.NameResolver;
import org.antlr.v4.runtime.*;

import java.io.IOException;
import java.io.InputStream;

public abstract class ParseSource implements Source {
  private final NameResolver myNameResolver;
  private final ErrorReporter myErrorReporter;
  private final Namespace myModule;
  private InputStream myStream;

  public ParseSource(NameResolver nameResolver, ErrorReporter errorReporter, Namespace module) {
    myNameResolver = nameResolver;
    myErrorReporter = errorReporter;
    myModule = module;
  }

  public InputStream getStream() {
    return myStream;
  }

  public void setStream(InputStream stream) {
    myStream = stream;
  }

  @Override
  public boolean load(Namespace namespace, ClassDefinition classDefinition) throws IOException {
    CountingErrorReporter countingErrorReporter = new CountingErrorReporter();
    final CompositeErrorReporter errorReporter = new CompositeErrorReporter();
    errorReporter.addErrorReporter(myErrorReporter);
    errorReporter.addErrorReporter(countingErrorReporter);

    VcgrammarLexer lexer = new VcgrammarLexer(new ANTLRInputStream(myStream));
    lexer.removeErrorListeners();
    lexer.addErrorListener(new BaseErrorListener() {
      @Override
      public void syntaxError(Recognizer<?, ?> recognizer, Object o, int line, int pos, String msg, RecognitionException e) {
        errorReporter.report(new ParserError(myModule, new Concrete.Position(line, pos), msg));
      }
    });

    VcgrammarParser parser = new VcgrammarParser(new CommonTokenStream(lexer));
    parser.removeErrorListeners();
    parser.addErrorListener(new BaseErrorListener() {
      @Override
      public void syntaxError(Recognizer<?, ?> recognizer, Object o, int line, int pos, String msg, RecognitionException e) {
        errorReporter.report(new ParserError(myModule, new Concrete.Position(line, pos), msg));
      }
    });

    int errorsCount = countingErrorReporter.getErrorsNumber();
    VcgrammarParser.DefsContext tree = parser.defs();
    if (tree == null || errorsCount != countingErrorReporter.getErrorsNumber()) return false;
    new BuildVisitor(namespace, classDefinition.getLocalNamespace(), myNameResolver, myErrorReporter).visitDefs(tree);
    return errorsCount == countingErrorReporter.getErrorsNumber();
  }
}
