package com.jetbrains.jetpad.vclang.editor.hybrid;

import com.jetbrains.jetpad.vclang.model.expr.AppExpression;
import com.jetbrains.jetpad.vclang.model.expr.Expression;
import com.jetbrains.jetpad.vclang.model.expr.LamExpression;
import com.jetbrains.jetpad.vclang.model.expr.VarExpression;
import jetbrains.jetpad.base.Handler;
import jetbrains.jetpad.grammar.*;
import jetbrains.jetpad.grammar.parser.Lexeme;
import jetbrains.jetpad.hybrid.parser.IdentifierToken;
import jetbrains.jetpad.hybrid.parser.Parser;
import jetbrains.jetpad.hybrid.parser.simple.SimpleParserSpecification;

import java.util.List;

public class ExpressionParser {
  static final Parser<Expression> PARSER;

  static {
    SimpleParserSpecification<Expression> spec = new SimpleParserSpecification<>();
    spec.changeGrammar(new Handler<SimpleParserSpecification.SimpleGrammarContext>() {
      @Override
      public void handle(SimpleParserSpecification.SimpleGrammarContext item) {
        Grammar g = item.grammar();

        Terminal id = item.id();
        // Terminal error = item.error();
        Terminal lp = item.terminal(Tokens.LP);
        Terminal rp = item.terminal(Tokens.RP);
        Terminal lb = item.terminal(Tokens.LB);
        Terminal rb = item.terminal(Tokens.RB);
        Terminal arrow = item.terminal(Tokens.ARROW);
        Terminal doubleArrow = item.terminal(Tokens.DOUBLE_ARROW);
        Terminal colon = item.terminal(Tokens.COLON);
        Terminal lambda = item.terminal(Tokens.LAMBDA);

        /*
        Terminal valueExpr = item.value("valueExpr", new Predicate<Object>() {
          @Override
          public boolean apply(Object input) {
            return input instanceof Expression;
          }
        });
        */

        NonTerminal expr = item.expr();
        NonTerminal expr1 = g.newNonTerminal("expr1");
        NonTerminal appExpr = g.newNonTerminal("appExpr");
        NonTerminal atom = g.newNonTerminal("atom");
        // NonTerminal dom = g.newNonTerminal("dom");
        // NonTerminal tele = g.newNonTerminal("tele");

        g.newRule(expr, expr1).setHandler(new RuleHandler() {
          @Override
          public Object handle(RuleContext ctx) {
            return ctx.get(0);
          }
        });

        // TODO: replace id with plus(id)
        g.newRule(expr, lambda, id, doubleArrow, expr).setHandler(new RuleHandler() {
          @Override
          public Object handle(RuleContext ctx) {
            LamExpression expr = new LamExpression(false);
            Lexeme lexeme = (Lexeme) ctx.get(1);
            IdentifierToken idToken = (IdentifierToken) lexeme.getValue();
            expr.variable.set(idToken.text());
            expr.body.set((Expression) ctx.get(3));
            return expr;
          }
        });

        g.newRule(appExpr, GrammarSugar.plus(atom)).setHandler(new RuleHandler() {
          @Override
          public Object handle(RuleContext ctx) {
            List<Expression> exprs = (List<Expression>) ctx.get(0);
            Expression result = exprs.get(0);
            for (int i = 1; i < exprs.size(); ++i) {
              AppExpression app = new AppExpression(false);
              app.function.set(result);
              app.argument.set(exprs.get(i));
              result = app;
            }
            return result;
          }
        });

        g.newRule(expr1, appExpr).setHandler(new RuleHandler() {
          @Override
          public Object handle(RuleContext ctx) {
            return ctx.get(0);
          }
        });

        /* g.newRule(expr1, dom, arrow, expr1).setHandler(new RuleHandler() {
          @Override
          public Object handle(RuleContext ctx) {
            return null; // TODO
          }
        });

        g.newRule(dom, expr1).setHandler(new RuleHandler() {
          @Override
          public Object handle(RuleContext ctx) {
            return ctx.get(0);
          }
        });

        g.newRule(dom, GrammarSugar.plus(tele)).setHandler(new RuleHandler() {
          @Override
          public Object handle(RuleContext ctx) {
            return ctx.get(0);
          }
        });

        g.newRule(tele, lp, GrammarSugar.plus(id), colon, expr1, rp).setHandler(new RuleHandler() {
          @Override
          public Object handle(RuleContext ctx) {
            return null; // TODO
          }
        });

        g.newRule(tele, lb, GrammarSugar.plus(id), colon, expr1, rb).setHandler(new RuleHandler() {
          @Override
          public Object handle(RuleContext ctx) {
            return null; // TODO
          }
        }); */

        g.newRule(atom, lp, expr, rp).setHandler(new RuleHandler() {
          @Override
          public Object handle(RuleContext ctx) {
            return ctx.get(1);
          }
        });

        g.newRule(atom, id).setHandler(new RuleHandler() {
          @Override
          public Object handle(RuleContext ctx) {
            VarExpression expr = new VarExpression();
            Lexeme lexeme = (Lexeme) ctx.get(0);
            IdentifierToken idToken = (IdentifierToken) lexeme.getValue();
            expr.name.set(idToken.text());
            return expr;
          }
        });
      }
    });
    PARSER = spec.buildParser();
  }
}
