package com.jetbrains.jetpad.vclang.parser;

import com.jetbrains.jetpad.vclang.module.Namespace;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.Concrete;
import com.jetbrains.jetpad.vclang.term.Prelude;
import com.jetbrains.jetpad.vclang.term.definition.Universe;
import com.jetbrains.jetpad.vclang.typechecking.error.ErrorReporter;
import org.antlr.v4.runtime.Token;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.jetbrains.jetpad.vclang.parser.VcgrammarParser.*;
import static com.jetbrains.jetpad.vclang.term.pattern.Utils.ProcessImplicitResult;
import static com.jetbrains.jetpad.vclang.term.pattern.Utils.processImplicit;

public class BuildVisitor extends VcgrammarBaseVisitor {
  private Namespace myNamespace;
  private final ErrorReporter myErrorReporter;

  public BuildVisitor(Namespace namespace, ErrorReporter errorReporter) {
    myNamespace = namespace;
    myErrorReporter = errorReporter;
  }

  private Concrete.NameArgument getVar(AtomFieldsAccContext ctx) {
    if (!ctx.fieldAcc().isEmpty() || !(ctx.atom() instanceof AtomLiteralContext)) {
      return null;
    }
    LiteralContext literal = ((AtomLiteralContext) ctx.atom()).literal();
    if (literal instanceof UnknownContext) {
      return new Concrete.NameArgument(tokenPosition(literal.getStart()), true, "_");
    }
    if (literal instanceof IdContext && ((IdContext) literal).name() instanceof NameIdContext) {
      return new Concrete.NameArgument(tokenPosition(literal.getStart()), true, ((NameIdContext) ((IdContext) literal).name()).ID().getText());
    }
    return null;
  }

  private List<Concrete.NameArgument> getVarsNull(ExprContext expr) {
    if (!(expr instanceof BinOpContext && ((BinOpContext) expr).binOpLeft().isEmpty())) {
      return null;
    }
    Concrete.NameArgument firstArg = getVar(((BinOpContext) expr).atomFieldsAcc());
    if (firstArg == null) {
      return null;
    }

    List<Concrete.NameArgument> result = new ArrayList<>();
    result.add(firstArg);
    for (ArgumentContext argument : ((BinOpContext) expr).argument()) {
      if (argument instanceof ArgumentExplicitContext) {
        Concrete.NameArgument arg = getVar(((ArgumentExplicitContext) argument).atomFieldsAcc());
        if (arg == null) {
          return null;
        }
        result.add(arg);
      } else {
        List<Concrete.NameArgument> arguments = getVarsNull(((ArgumentImplicitContext) argument).expr());
        if (arguments == null) {
          return null;
        }
        for (Concrete.NameArgument arg : arguments) {
          arg.setExplicit(false);
          result.add(arg);
        }
      }
    }
    return result;
  }

  private List<Concrete.NameArgument> getVars(ExprContext expr) {
    List<Concrete.NameArgument> result = getVarsNull(expr);
    if (result == null) {
      myErrorReporter.report(new ParserError(myNamespace, tokenPosition(expr.getStart()), "Expected a list of variables"));
      return null;
    } else {
      return result;
    }
  }

  public Concrete.Expression visitExpr(ExprContext expr) {
    return (Concrete.Expression) visit(expr);
  }

  public Concrete.Expression visitExpr(AtomContext expr) {
    return (Concrete.Expression) visit(expr);
  }

  public Concrete.Expression visitExpr(LiteralContext expr) {
    return (Concrete.Expression) visit(expr);
  }

  private List<Concrete.Statement> visitStatementList(List<StatementContext> statementCtxs) {
    List<Concrete.Statement> statements = new ArrayList<>(statementCtxs.size());
    for (StatementContext statementCtx : statementCtxs) {
      Concrete.Statement statement = (Concrete.Statement) visit(statementCtx);
      if (statement != null) {
        statements.add(statement);
      }
    }
    return statements;
  }

  @Override
  public List<Concrete.Statement> visitStatements(StatementsContext ctx) {
    if (ctx == null) return null;
    return visitStatementList(ctx.statement());
  }

  public Concrete.Definition visitDefinition(DefinitionContext ctx) {
    return (Concrete.Definition) visit(ctx);
  }

  @Override
  public Concrete.Statement visitStatDef(StatDefContext ctx) {
    if (ctx == null) return null;
    Concrete.Definition definition = visitDefinition(ctx.definition());
    if (definition == null) {
      return null;
    }
    return new Concrete.DefineStatement(definition.getPosition(), ctx.staticMod() instanceof StaticStaticContext, definition);
  }

  @Override
  public Concrete.Statement visitStatCmd(StatCmdContext ctx) {
    if (ctx == null) return null;
    Abstract.NamespaceCommandStatement.Kind kind = (Abstract.NamespaceCommandStatement.Kind) visit(ctx.nsCmd());
    List<Concrete.Identifier> path = new ArrayList<>();
    path.add(visitName(ctx.name(0)));
    for (FieldAccContext fieldAccContext : ctx.fieldAcc()) {
      if (fieldAccContext instanceof ClassFieldContext) {
        Concrete.Identifier identifier = visitName(((ClassFieldContext) fieldAccContext).name());
        if (identifier == null) {
          return null;
        }
        path.add(identifier);
      } else {
        myErrorReporter.report(new ParserError(myNamespace, tokenPosition(fieldAccContext.getStart()), "Expected a name"));
      }
    }

    List<Concrete.Identifier> names;
    if (ctx.name().size() > 1) {
      names = new ArrayList<>(ctx.name().size() - 1);
      for (int i = 1; i < ctx.name().size(); ++i) {
        names.add(visitName(ctx.name(i)));
      }
    } else {
      names = null;
    }
    return new Concrete.NamespaceCommandStatement(tokenPosition(ctx.getStart()), kind, path, names);
  }

  @Override
  public Abstract.NamespaceCommandStatement.Kind visitOpenCmd(OpenCmdContext ctx) {
    return Abstract.NamespaceCommandStatement.Kind.OPEN;
  }

  @Override
  public Abstract.NamespaceCommandStatement.Kind visitCloseCmd(CloseCmdContext ctx) {
    return Abstract.NamespaceCommandStatement.Kind.CLOSE;
  }

  @Override
  public Abstract.NamespaceCommandStatement.Kind visitExportCmd(ExportCmdContext ctx) {
    return Abstract.NamespaceCommandStatement.Kind.EXPORT;
  }

  private static class FunctionContext {
    ExprContext typeCtx;
    Abstract.Definition.Arrow arrow;
    ExprContext termCtx;
  }

  public FunctionContext visitTypeTermOpt(TypeTermOptContext ctx) {
    return (FunctionContext) visit(ctx);
  }

  @Override
  public FunctionContext visitWithType(WithTypeContext ctx) {
    if (ctx == null) return null;
    FunctionContext result = new FunctionContext();
    result.typeCtx = ctx.expr(0);
    result.arrow = visitArrow(ctx.arrow());
    result.termCtx = ctx.expr().size() > 1 ? ctx.expr(1) : null;
    return result;
  }

  @Override
  public FunctionContext visitWithoutType(WithoutTypeContext ctx) {
    if (ctx == null) return null;
    FunctionContext result = new FunctionContext();
    result.typeCtx = null;
    result.arrow = visitArrow(ctx.arrow());
    result.termCtx = ctx.expr();
    return result;
  }

  public Abstract.Definition.Arrow visitArrow(ArrowContext arrowCtx) {
    return arrowCtx instanceof ArrowLeftContext ? Abstract.Definition.Arrow.LEFT : arrowCtx instanceof ArrowRightContext ? Abstract.Definition.Arrow.RIGHT : null;
  }

  public Abstract.Definition.Precedence visitPrecedence(PrecedenceContext ctx) {
    return (Abstract.Definition.Precedence) visit(ctx);
  }

  @Override
  public Abstract.Definition.Precedence visitNoPrecedence(NoPrecedenceContext ctx) {
    return Abstract.Definition.DEFAULT_PRECEDENCE;
  }

  @Override
  public Abstract.Definition.Precedence visitWithPrecedence(WithPrecedenceContext ctx) {
    if (ctx == null) return null;
    int priority = Integer.valueOf(ctx.NUMBER().getText());
    if (priority < 1 || priority > 9) {
      myErrorReporter.report(new ParserError(myNamespace, tokenPosition(ctx.NUMBER().getSymbol()), "Precedence out of range: " + priority));

      if (priority < 1) {
        priority = 1;
      } else {
        priority = 9;
      }
    }

    return new Abstract.Definition.Precedence((Abstract.Definition.Associativity) visit(ctx.associativity()), (byte) priority);
  }

  @Override
  public Abstract.Definition.Associativity visitNonAssoc(NonAssocContext ctx) {
    return Abstract.Definition.Associativity.NON_ASSOC;
  }

  @Override
  public Abstract.Definition.Associativity visitLeftAssoc(LeftAssocContext ctx) {
    return Abstract.Definition.Associativity.LEFT_ASSOC;
  }

  @Override
  public Abstract.Definition.Associativity visitRightAssoc(RightAssocContext ctx) {
    return Abstract.Definition.Associativity.RIGHT_ASSOC;
  }

  @Override
  public Concrete.FunctionDefinition visitDefFunction(DefFunctionContext ctx) {
    if (ctx == null) return null;
    Concrete.Identifier identifier = visitName(ctx.name());
    Abstract.Definition.Precedence precedence = visitPrecedence(ctx.precedence());
    FunctionContext functionContext = visitTypeTermOpt(ctx.typeTermOpt());
    List<Concrete.Argument> arguments = visitFunctionArguments(ctx.tele(), false);
    if (identifier == null || precedence == null || functionContext == null || arguments == null) {
      return null;
    }

    Concrete.Expression resultType = functionContext.typeCtx == null ? null : visitExpr(functionContext.typeCtx);
    Concrete.Expression term = functionContext.termCtx == null ? null : visitExpr(functionContext.termCtx);
    List<Concrete.Statement> statements = visitStatementList(ctx.where().statement());
    return new Concrete.FunctionDefinition(tokenPosition(ctx.getStart()), identifier.getName(), precedence, arguments, resultType, functionContext.arrow, term, false, null, statements);
  }

  private List<Concrete.Argument> visitFunctionArguments(List<TeleContext> teleCtx, boolean overridden) {
    List<Concrete.Argument> arguments = new ArrayList<>();
    for (TeleContext tele : teleCtx) {
      List<Concrete.Argument> args = visitLamTele(tele);
      if (args == null) {
        return null;
      }

      if (overridden || args.get(0) instanceof Concrete.TelescopeArgument) {
        arguments.add(args.get(0));
      } else {
        myErrorReporter.report(new ParserError(myNamespace, tokenPosition(tele.getStart()), "Expected a typed variable"));
        return null;
      }
    }
    return arguments;
  }

  private List<Concrete.Argument> visitLamTele(TeleContext tele) {
    List<Concrete.Argument> arguments = new ArrayList<>(3);
    if (tele instanceof TeleLiteralContext) {
      LiteralContext literalContext = ((TeleLiteralContext) tele).literal();
      if (literalContext instanceof IdContext && ((IdContext) literalContext).name() instanceof NameIdContext) {
        String name = ((NameIdContext) ((IdContext) literalContext).name()).ID().getText();
        arguments.add(new Concrete.NameArgument(tokenPosition(((NameIdContext) ((IdContext) literalContext).name()).ID().getSymbol()), true, name));
      } else
      if (literalContext instanceof UnknownContext) {
        arguments.add(new Concrete.NameArgument(tokenPosition(literalContext.getStart()), true, null));
      } else {
        myErrorReporter.report(new ParserError(myNamespace, tokenPosition(literalContext.getStart()), "Unexpected token. Expected an identifier."));
        return null;
      }
    } else {
      boolean explicit = tele instanceof ExplicitContext;
      TypedExprContext typedExpr = explicit ? ((ExplicitContext) tele).typedExpr() : ((ImplicitContext) tele).typedExpr();
      ExprContext varsExpr;
      Concrete.Expression typeExpr;
      if (typedExpr instanceof TypedContext) {
        varsExpr = ((TypedContext) typedExpr).expr(0);
        typeExpr = visitExpr(((TypedContext) typedExpr).expr(1));
        if (typeExpr == null) return null;
      } else {
        varsExpr = ((NotTypedContext) typedExpr).expr();
        typeExpr = null;
      }
      List<Concrete.NameArgument> vars = getVars(varsExpr);
      if (vars == null) return null;

      if (typeExpr == null) {
        if (explicit) {
          arguments.addAll(vars);
        } else {
          for (Concrete.NameArgument var : vars) {
            arguments.add(new Concrete.NameArgument(var.getPosition(), false, var.getName()));
          }
        }
      } else {
        List<String> args = new ArrayList<>(vars.size());
        for (Concrete.NameArgument var : vars) {
          args.add(var.getName());
        }
        arguments.add(new Concrete.TelescopeArgument(tokenPosition(tele.getStart()), explicit, args, typeExpr));
      }
    }
    return arguments;
  }

  @Override
  public Concrete.ClassDefinition visitDefClass(DefClassContext ctx) {
    if (ctx == null || ctx.classFields() == null) return null;
    List<Concrete.Statement> statements = visitStatementList(ctx.classFields().statement());
    return new Concrete.ClassDefinition(tokenPosition(ctx.getStart()), ctx.ID().getText(), statements);
  }

  @Override
  public Concrete.DataDefinition visitDefData(DefDataContext ctx) {
    if (ctx == null) return null;
    Concrete.Identifier identifier = visitName(ctx.name());
    List<Concrete.TypeArgument> parameters = visitTeles(ctx.tele());
    Abstract.Definition.Precedence precedence = visitPrecedence(ctx.precedence());
    if (identifier == null || parameters == null || precedence == null) {
      return null;
    }

    Universe universe = null;
    if (ctx.literal() != null) {
      Concrete.Expression expression = (Concrete.Expression) visit(ctx.literal());
      if (expression instanceof Concrete.UniverseExpression) {
        universe = ((Concrete.UniverseExpression) expression).getUniverse();
      } else {
        myErrorReporter.report(new ParserError(myNamespace, expression.getPosition(), "Expected a universe"));
      }
    }

    List<Concrete.Constructor> constructors = new ArrayList<>(ctx.constructorDef().size());
    Concrete.DataDefinition dataDefinition = new Concrete.DataDefinition(tokenPosition(ctx.getStart()), identifier.getName(), precedence, parameters, universe, constructors);
    for (ConstructorDefContext constructorDefContext : ctx.constructorDef()) {
      visitConstructorDef(constructorDefContext, dataDefinition);
    }
    return dataDefinition;
  }

  private void visitConstructorDef(ConstructorDefContext ctx, Concrete.DataDefinition def) {
    List<Concrete.Pattern> patterns = null;

    if (ctx instanceof WithPatternsContext) {
      WithPatternsContext wpCtx = (WithPatternsContext) ctx;
      Concrete.Identifier dataDefIdentifier = visitName(wpCtx.name());
      if (dataDefIdentifier == null) {
        return;
      }
      if (!def.getName().name.equals(dataDefIdentifier.getName().name)) {
        myErrorReporter.report(new ParserError(myNamespace, dataDefIdentifier.getPosition(), "Expected a data type name: " + def.getName()));
        return;
      }

      patterns = visitPatterns(wpCtx.patternx());

      ProcessImplicitResult result = processImplicit(patterns, def.getParameters());
      if (result.patterns == null) {
        if (result.numExcessive != 0) {
          myErrorReporter.report(new ParserError(myNamespace,
              tokenPosition(wpCtx.patternx(wpCtx.patternx().size() - result.numExcessive).start), "Too many arguments: " + result.numExcessive + " excessive"));
        } else if (result.wrongImplicitPosition < patterns.size()) {
          myErrorReporter.report(new ParserError(myNamespace,
              tokenPosition(wpCtx.patternx(result.wrongImplicitPosition).start), "Unexpected implicit argument"));
        } else {
          myErrorReporter.report(new ParserError(myNamespace, tokenPosition(wpCtx.name().start), "Too few explicit arguments, expected: " + result.numExplicit));
        }
        return;
      }
    }

    List<ConstructorContext> constructorCtxs = ctx instanceof WithPatternsContext ?
        ((WithPatternsContext) ctx).constructor() : Collections.singletonList(((NoPatternsContext) ctx).constructor());

    for (ConstructorContext conCtx : constructorCtxs) {
      Concrete.Identifier conIdentifier = visitName(conCtx.name());
      List<Concrete.TypeArgument> arguments = visitTeles(conCtx.tele());
      if (conIdentifier == null || arguments == null) {
        continue;
      }
      def.getConstructors().add(new Concrete.Constructor(conIdentifier.getPosition(), conIdentifier.getName(), visitPrecedence(conCtx.precedence()), arguments, def, patterns));
    }
  }

  @Override
  public Concrete.NamePattern visitPatternAny(PatternAnyContext ctx) {
    return new Concrete.NamePattern(tokenPosition(ctx.getStart()), null);
  }

  @Override
  public Concrete.Pattern visitPatternID(PatternIDContext ctx) {
    return new Concrete.NamePattern(tokenPosition(ctx.getStart()), ctx.ID().getText());
  }

  private List<Concrete.Pattern> visitPatterns(List<PatternxContext> patternContexts) {
    List<Concrete.Pattern> patterns = new ArrayList<>();
    for (PatternxContext pattern : patternContexts) {
      Concrete.Pattern result = null;
      if (pattern instanceof PatternImplicitContext) {
        result = (Concrete.Pattern) visit(((PatternImplicitContext) pattern).pattern());
        result.setExplicit(false);
      } else if (pattern instanceof PatternExplicitContext){
        result = (Concrete.Pattern) visit(((PatternExplicitContext) pattern).pattern());
      }
      patterns.add(result);
    }
    return patterns;
  }

  @Override
  public Concrete.ConstructorPattern visitPatternConstructor(PatternConstructorContext ctx) {
    Concrete.Identifier identifier = visitName(ctx.name());
    if (identifier == null) {
      return null;
    }
    return new Concrete.ConstructorPattern(tokenPosition(ctx.getStart()), identifier.getName(), visitPatterns(ctx.patternx()));
  }

  @Override
  public Concrete.PiExpression visitArr(ArrContext ctx) {
    if (ctx == null) return null;
    Concrete.Expression domain = visitExpr(ctx.expr(0));
    Concrete.Expression codomain = visitExpr(ctx.expr(1));
    if (domain == null || codomain == null) {
      return null;
    }

    List<Concrete.TypeArgument> arguments = new ArrayList<>(1);
    arguments.add(new Concrete.TypeArgument(domain.getPosition(), true, domain));
    return new Concrete.PiExpression(tokenPosition(ctx.getToken(ARROW, 0).getSymbol()), arguments, codomain);
  }

  @Override
  public Concrete.Expression visitAtomLiteral(AtomLiteralContext ctx) {
    if (ctx == null) return null;
    return visitExpr(ctx.literal());
  }

  @Override
  public Concrete.Expression visitAtomNumber(AtomNumberContext ctx) {
    if (ctx == null) return null;
    int number = Integer.valueOf(ctx.NUMBER().getText());
    Concrete.Position pos = tokenPosition(ctx.NUMBER().getSymbol());
    Concrete.Expression result = new Concrete.DefCallExpression(pos, Prelude.ZERO);
    for (int i = 0; i < number; ++i) {
      result = new Concrete.AppExpression(pos, new Concrete.DefCallExpression(pos, Prelude.SUC), new Concrete.ArgumentExpression(result, true, false));
    }
    return result;
  }

  @Override
  public Concrete.UniverseExpression visitUniverse(UniverseContext ctx) {
    if (ctx == null) return null;
    return new Concrete.UniverseExpression(tokenPosition(ctx.UNIVERSE().getSymbol()), new Universe.Type(Integer.valueOf(ctx.UNIVERSE().getText().substring("\\Type".length()))));
  }

  @Override
  public Concrete.UniverseExpression visitTruncatedUniverse(TruncatedUniverseContext ctx) {
    if (ctx == null) return null;
    String text = ctx.TRUNCATED_UNIVERSE().getText();
    int indexOfMinusSign = text.indexOf('-');
    return new Concrete.UniverseExpression(tokenPosition(ctx.TRUNCATED_UNIVERSE().getSymbol()), new Universe.Type(Integer.valueOf(text.substring(1, indexOfMinusSign)), Integer.valueOf(text.substring(indexOfMinusSign + "-Type".length()))));
  }

  @Override
  public Concrete.UniverseExpression visitProp(PropContext ctx) {
    if (ctx == null) return null;
    return new Concrete.UniverseExpression(tokenPosition(ctx.PROP().getSymbol()), new Universe.Type(0, Universe.Type.PROP));
  }

  @Override
  public Concrete.UniverseExpression visitSet(SetContext ctx) {
    if (ctx == null) return null;
    return new Concrete.UniverseExpression(tokenPosition(ctx.SET().getSymbol()), new Universe.Type(Integer.valueOf(ctx.SET().getText().substring("\\Set".length())), Universe.Type.SET));
  }

  @Override
  public Concrete.InferHoleExpression visitUnknown(UnknownContext ctx) {
    if (ctx == null) return null;
    return new Concrete.InferHoleExpression(tokenPosition(ctx.getStart()));
  }

  @Override
  public Concrete.ErrorExpression visitHole(HoleContext ctx) {
    if (ctx == null) return null;
    return new Concrete.ErrorExpression(tokenPosition(ctx.getStart()));
  }

  @Override
  public Concrete.DefCallExpression visitId(IdContext ctx) {
    if (ctx == null) return null;
    Concrete.Identifier identifier = visitName(ctx.name());
    if (identifier == null) {
      return null;
    }
    return new Concrete.DefCallExpression(identifier.getPosition(), null, identifier.getName());
  }

  @Override
  public Concrete.SigmaExpression visitSigma(SigmaContext ctx) {
    if (ctx == null) return null;
    List<Concrete.TypeArgument> args = visitTeles(ctx.tele());
    if (args == null) {
      return null;
    }

    for (Concrete.TypeArgument arg : args) {
      if (!arg.getExplicit()) {
        myErrorReporter.report(new ParserError(myNamespace, arg.getPosition(), "Fields in sigma types must be explicit"));
      }
    }
    return new Concrete.SigmaExpression(tokenPosition(ctx.getStart()), args);
  }

  @Override
  public Concrete.PiExpression visitPi(PiContext ctx) {
    if (ctx == null) return null;
    List<Concrete.TypeArgument> args = visitTeles(ctx.tele());
    Concrete.Expression codomain = visitExpr(ctx.expr());
    if (args == null || codomain == null) {
      return null;
    }
    return new Concrete.PiExpression(tokenPosition(ctx.getStart()), args, codomain);
  }

  private List<Concrete.TypeArgument> visitTeles(List<TeleContext> teles) {
    List<Concrete.TypeArgument> arguments = new ArrayList<>(teles.size());
    for (TeleContext tele : teles) {
      boolean explicit = !(tele instanceof ImplicitContext);
      TypedExprContext typedExpr;
      if (explicit) {
        if (tele instanceof ExplicitContext) {
          typedExpr = ((ExplicitContext) tele).typedExpr();
        } else {
          Concrete.Expression expr = visitExpr(((TeleLiteralContext) tele).literal());
          if (expr == null) {
            return null;
          }
          arguments.add(new Concrete.TypeArgument(true, expr));
          continue;
        }
      } else {
        typedExpr = ((ImplicitContext) tele).typedExpr();
      }
      if (typedExpr instanceof TypedContext) {
        Concrete.Expression type = visitExpr(((TypedContext) typedExpr).expr(1));
        List<Concrete.NameArgument> args = getVars(((TypedContext) typedExpr).expr(0));
        if (type == null || args == null) {
          return null;
        }

        List<String> vars = new ArrayList<>(args.size());
        for (Concrete.NameArgument arg : args) {
          vars.add(arg.getName());
        }
        arguments.add(new Concrete.TelescopeArgument(tokenPosition(tele.getStart()), explicit, vars, type));
      } else {
        Concrete.Expression expr = visitExpr(((NotTypedContext) typedExpr).expr());
        if (expr == null) {
          return null;
        }
        arguments.add(new Concrete.TypeArgument(explicit, expr));
      }
    }
    return arguments;
  }

  @Override
  public Concrete.Expression visitTuple(TupleContext ctx) {
    if (ctx == null) return null;
    if (ctx.expr().size() == 1) {
      return visitExpr(ctx.expr(0));
    } else {
      List<Concrete.Expression> fields = new ArrayList<>(ctx.expr().size());
      for (ExprContext exprCtx : ctx.expr()) {
        Concrete.Expression expr = visitExpr(exprCtx);
        if (expr == null) return null;
        fields.add(expr);
      }
      return new Concrete.TupleExpression(tokenPosition(ctx.getStart()), fields);
    }
  }

  private List<Concrete.Argument> visitLamTeles(List<TeleContext> tele) {
    List<Concrete.Argument> arguments = new ArrayList<>();
    for (TeleContext arg : tele) {
      List<Concrete.Argument> arguments1 = visitLamTele(arg);
      if (arguments1 == null) return null;
      arguments.addAll(arguments1);
    }
    return arguments;
  }

  @Override
  public Concrete.Expression visitLam(LamContext ctx) {
    if (ctx == null) return null;
    List<Concrete.Argument> args = visitLamTeles(ctx.tele());
    Concrete.Expression body = visitExpr(ctx.expr());
    if (args == null || body == null) {
      return null;
    }
    return new Concrete.LamExpression(tokenPosition(ctx.getStart()), args, body);
  }

  private Concrete.Expression visitAtoms(Concrete.Expression expr, List<ArgumentContext> arguments) {
    if (expr == null) {
      return null;
    }
    for (ArgumentContext argument : arguments) {
      boolean explicit = argument instanceof ArgumentExplicitContext;
      Concrete.Expression expr1;
      if (explicit) {
        expr1 = visitAtomFieldsAcc(((ArgumentExplicitContext) argument).atomFieldsAcc());
      } else {
        expr1 = visitExpr(((ArgumentImplicitContext) argument).expr());
      }
      if (expr1 == null) return null;
      expr = new Concrete.AppExpression(expr.getPosition(), expr, new Concrete.ArgumentExpression(expr1, explicit, false));
    }
    return expr;
  }

  @Override
  public Concrete.Expression visitBinOp(BinOpContext ctx) {
    if (ctx == null) return null;
    Concrete.Expression left = null;
    Concrete.DefCallExpression binOp = null;
    List<Abstract.BinOpSequenceElem> sequence = new ArrayList<>(ctx.binOpLeft().size());

    for (BinOpLeftContext leftContext : ctx.binOpLeft()) {
      Concrete.Identifier identifier = (Concrete.Identifier) visit(leftContext.infix());
      Concrete.Expression expr = visitAtoms(visitAtomFieldsAcc(leftContext.atomFieldsAcc()), leftContext.argument());
      if (expr == null) {
        continue;
      }
      if (leftContext.maybeNew() instanceof WithNewContext) {
        expr = new Concrete.NewExpression(tokenPosition(leftContext.getStart()), expr);
      }

      if (left == null) {
        left = expr;
      } else {
        sequence.add(new Abstract.BinOpSequenceElem(binOp, expr));
      }
      binOp = new Concrete.DefCallExpression(identifier.getPosition(), null, identifier.getName());
    }

    Concrete.Expression expr = visitAtoms(visitAtomFieldsAcc(ctx.atomFieldsAcc()), ctx.argument());
    if (expr == null) {
      return null;
    }
    if (ctx.maybeNew() instanceof WithNewContext) {
      expr = new Concrete.NewExpression(tokenPosition(ctx.getStart()), expr);
    }

    if (left == null) {
      return expr;
    }

    sequence.add(new Abstract.BinOpSequenceElem(binOp, expr));
    return new Concrete.BinOpSequenceExpression(tokenPosition(ctx.getStart()), left, sequence);
  }

  @Override
  public Concrete.Expression visitAtomFieldsAcc(AtomFieldsAccContext ctx) {
    if (ctx == null) return null;
    Concrete.Expression expression = visitExpr(ctx.atom());
    if (expression == null || ctx.fieldAcc() == null) {
      return null;
    }

    for (FieldAccContext fieldAccContext : ctx.fieldAcc()) {
      if (fieldAccContext instanceof ClassFieldContext) {
        Concrete.Identifier identifier = visitName(((ClassFieldContext) fieldAccContext).name());
        if (identifier == null) {
          return null;
        }
        expression = new Concrete.DefCallExpression(tokenPosition(fieldAccContext.getStart()), expression, identifier.getName());
      } else
      if (fieldAccContext instanceof SigmaFieldContext) {
        expression = new Concrete.ProjExpression(tokenPosition(fieldAccContext.getStart()), expression, Integer.valueOf(((SigmaFieldContext) fieldAccContext).NUMBER().getText()) - 1);
      } else {
        throw new IllegalStateException();
      }
    }

    if (ctx.classFields() != null) {
      expression = new Concrete.ClassExtExpression(tokenPosition(ctx.getStart()), expression, visitStatementList(ctx.classFields().statement()));
    }
    return expression;
  }

  @Override
  public Concrete.Identifier visitInfixBinOp(InfixBinOpContext ctx) {
    if (ctx == null) return null;
    return new Concrete.Identifier(tokenPosition(ctx.getStart()), ctx.BIN_OP().getText(), Abstract.Definition.Fixity.INFIX);
  }

  @Override
  public Concrete.Identifier visitInfixId(InfixIdContext ctx) {
    if (ctx == null) return null;
    return new Concrete.Identifier(tokenPosition(ctx.getStart()), ctx.ID().getText(), Abstract.Definition.Fixity.PREFIX);
  }

  public Concrete.Identifier visitName(NameContext ctx) {
    return (Concrete.Identifier) visit(ctx);
  }

  @Override
  public Concrete.Identifier visitNameId(NameIdContext ctx) {
    if (ctx == null) return null;
    return new Concrete.Identifier(tokenPosition(ctx.getStart()), ctx.ID().getText(), Abstract.Definition.Fixity.PREFIX);
  }

  @Override
  public Concrete.Identifier visitNameBinOp(NameBinOpContext ctx) {
    if (ctx == null) return null;
    return new Concrete.Identifier(tokenPosition(ctx.getStart()), ctx.BIN_OP().getText(), Abstract.Definition.Fixity.INFIX);
  }

  @Override
  public Concrete.Expression visitExprElim(ExprElimContext ctx) {
    if (ctx == null) return null;
    List<Concrete.Clause> clauses = new ArrayList<>(ctx.clause().size());

    Concrete.Expression elimExpr = visitExpr(ctx.expr());
    if (elimExpr == null) {
      return null;
    }

    boolean wasOtherwise = false;
    for (ClauseContext clauseCtx : ctx.clause()) {
      Concrete.Pattern pattern;
      if (clauseCtx.name() != null) {
        Concrete.Identifier identifier = visitName(clauseCtx.name());
        if (identifier == null) {
          return null;
        }
        pattern = new Concrete.ConstructorPattern(tokenPosition(clauseCtx.name().start), identifier.getName(), visitPatterns(clauseCtx.patternx()));
        for (Concrete.Pattern subPattern : ((Concrete.ConstructorPattern) pattern).getPatterns()) {
          if (subPattern instanceof Concrete.ConstructorPattern) {
            myErrorReporter.report(new ParserError(myNamespace, subPattern.getPosition(), "Only simple constructor patterns are allowed under elim"));
            return null;
          }
        }
      } else {
        if (wasOtherwise) {
          myErrorReporter.report(new ParserError(myNamespace, tokenPosition(clauseCtx.start), "Multiple otherwise clauses"));
          continue;
        }
        wasOtherwise = true;
        pattern = new Concrete.NamePattern(tokenPosition(clauseCtx.start), null);
      }

      Concrete.Expression expr = visitExpr(clauseCtx.expr());
      if (expr == null) {
        return null;
      }
      clauses.add(new Concrete.Clause(tokenPosition(clauseCtx.getStart()), pattern, visitArrow(clauseCtx.arrow()), expr));
    }

    return ctx.elimCase() instanceof ElimContext ?
        new Concrete.ElimExpression(tokenPosition(ctx.getStart()), elimExpr, clauses) :
        new Concrete.CaseExpression(tokenPosition(ctx.getStart()), elimExpr, clauses);
  }

  @Override
  public Concrete.LetClause visitLetClause(LetClauseContext ctx) {
    String name = ctx.ID().getText();

    List<Concrete.Argument> arguments = visitFunctionArguments(ctx.tele(), false);
    Concrete.Expression resultType = ctx.typeAnnotation() == null ? null : visitExpr(ctx.typeAnnotation().expr());
    Abstract.Definition.Arrow arrow = visitArrow(ctx.arrow());
    Concrete.Expression term = visitExpr(ctx.expr());

    if (arguments == null || arrow == null || term == null) {
      return null;
    }

    return new Concrete.LetClause(tokenPosition(ctx.getStart()), name, new ArrayList<>(arguments), resultType, arrow, term);
  }

  @Override
  public Concrete.LetExpression visitLet(LetContext ctx) {
    List<Concrete.LetClause> clauses = new ArrayList<>();
    for (LetClauseContext clauseCtx : ctx.letClause()) {
      Concrete.LetClause clause = visitLetClause(clauseCtx);
      if (clause == null) {
        continue;
      }
      clauses.add(visitLetClause(clauseCtx));
    }

    Concrete.Expression expr = visitExpr(ctx.expr());
    if (expr == null) {
      return null;
    }
    return new Concrete.LetExpression(tokenPosition(ctx.getStart()), clauses, expr);
  }

  private static Concrete.Position tokenPosition(Token token) {
    return new Concrete.Position(token.getLine(), token.getCharPositionInLine());
  }
}
