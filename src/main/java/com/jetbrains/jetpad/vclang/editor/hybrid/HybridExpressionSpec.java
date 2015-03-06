package com.jetbrains.jetpad.vclang.editor.hybrid;

import com.google.common.base.Function;
import com.jetbrains.jetpad.vclang.model.expr.Expression;
import com.jetbrains.jetpad.vclang.model.visitor.PrettyPrintVisitor;
import jetbrains.jetpad.completion.CompletionItem;
import jetbrains.jetpad.completion.CompletionParameters;
import jetbrains.jetpad.completion.CompletionSupplier;
import jetbrains.jetpad.hybrid.*;
import jetbrains.jetpad.hybrid.parser.Parser;
import jetbrains.jetpad.hybrid.parser.Token;
import jetbrains.jetpad.hybrid.parser.prettyprint.PrettyPrinter;
import jetbrains.jetpad.hybrid.parser.prettyprint.PrettyPrinterContext;

import java.util.ArrayList;
import java.util.List;

public class HybridExpressionSpec implements HybridPositionSpec<Expression> {
  @Override
  public Parser<Expression> getParser() {
    return ExpressionParser.PARSER;
  }

  @Override
  public PrettyPrinter<? super Expression> getPrettyPrinter() {
    return new PrettyPrinter<Expression>() {
      @Override
      public void print(Expression expr, PrettyPrinterContext<Expression> ctx) {
        expr.accept(new PrettyPrintVisitor(ctx, 0));
      }
    };
  }

  @Override
  public PairSpec getPairSpec() {
    return PairSpec.EMPTY;
  }

  @Override
  public CompletionSupplier getTokenCompletion(final Function<Token, Runnable> tokenHandler) {
    return new CompletionSupplier() {
      @Override
      public List<CompletionItem> get(CompletionParameters cp) {
        List<CompletionItem> result = new ArrayList<>();
        TokenCompletionItems items = new TokenCompletionItems(tokenHandler);
        if (!cp.isMenu()) {
          result.addAll(items.forTokens(Tokens.LP, Tokens.RP));
          result.add(items.forId());
        }
        return result;
      }
    };
  }

  @Override
  public CompletionSupplier getAdditionalCompletion(CompletionContext completionContext, Completer completer) {
    return CompletionSupplier.EMPTY;
  }
}
