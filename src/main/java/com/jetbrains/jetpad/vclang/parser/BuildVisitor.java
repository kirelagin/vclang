package com.jetbrains.jetpad.vclang.parser;

import com.jetbrains.jetpad.vclang.module.Module;
import com.jetbrains.jetpad.vclang.module.ModuleLoader;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.Concrete;
import com.jetbrains.jetpad.vclang.term.Prelude;
import com.jetbrains.jetpad.vclang.term.definition.*;
import com.jetbrains.jetpad.vclang.term.definition.visitor.TypeChecking;
import com.jetbrains.jetpad.vclang.term.expr.DefCallExpression;
import com.jetbrains.jetpad.vclang.term.expr.arg.Utils;
import com.jetbrains.jetpad.vclang.term.pattern.Utils.ProcessImplicitResult;
import org.antlr.v4.runtime.Token;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.jetbrains.jetpad.vclang.parser.VcgrammarParser.*;
import static com.jetbrains.jetpad.vclang.term.expr.arg.Utils.trimToSize;
import static com.jetbrains.jetpad.vclang.term.pattern.Utils.collectPatternNames;
import static com.jetbrains.jetpad.vclang.term.pattern.Utils.processImplicit;

public class BuildVisitor extends VcgrammarBaseVisitor {
  private Definition myParent;
  private List<String> myContext = new ArrayList<>();
  private final Module myModule;
  private final ModuleLoader myModuleLoader;
  private boolean myOnlyStatics;

  public BuildVisitor(ClassDefinition parent, ModuleLoader moduleLoader, boolean onlyStatics) {
    myParent = parent;
    myModule = new Module((ClassDefinition) parent.getParent(), parent.getName().name);
    myModuleLoader = moduleLoader;
    myOnlyStatics = onlyStatics;
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
      myModuleLoader.getErrors().add(new ParserError(myModule, tokenPosition(expr.getStart()), "Expected a list of variables"));
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

  private void visitDefList(List<DefContext> defs) {
    for (DefContext def : defs) {
      Definition result = visitDef(def);
      if (myParent instanceof FunctionDefinition && result != null && result.isAbstract()) {
        myModuleLoader.getErrors().add(new ParserError(myModule, tokenPosition(def.getStart()), "Abstract functions is not allowed in where function."));
      }
    }
  }

  @Override
  public Void visitDefs(DefsContext ctx) {
    if (ctx == null) return null;
    visitDefList(ctx.def());
    return null;
  }

  public List<Concrete.FunctionDefinition> visitDefsRaw(DefsContext ctx) {
    if (ctx == null) return null;
    List<Concrete.FunctionDefinition> definitions = new ArrayList<>(ctx.def().size());
    for (DefContext def : ctx.def()) {
      if (def instanceof DefOverrideContext) {
        DefOverrideContext defOverride = (DefOverrideContext) def;
        if (defOverride.where() != null) {
          myModuleLoader.getErrors().add(new ParserError(myModule, tokenPosition(ctx.getStart()), "Where clause is not allowed in expression."));
          continue;
        }
        FunctionContext functionCtx = (FunctionContext) visit(defOverride.typeTermOpt());Concrete.FunctionDefinition definition = visitFunctionRawBegin(true, defOverride.name().size() == 1 ? null : defOverride.name(0), null, defOverride.name().size() == 1 ? defOverride.name(0) : defOverride.name(1), defOverride.tele(), functionCtx);
        if (definition != null) {
          if (functionCtx.termCtx != null) {
            definition.setTerm(visitExpr(functionCtx.termCtx));
          }
          visitFunctionRawEnd(definition);
          definitions.add(definition);
        }
      } else {
        myModuleLoader.getErrors().add(new ParserError(myModule, tokenPosition(ctx.getStart()), "Expected an overridden function"));
      }
    }
    return definitions;
  }

  public Definition visitDef(DefContext ctx) {
    if (ctx instanceof DefFunctionContext) {
      return visitDefFunction((DefFunctionContext) ctx);
    } else
    if (ctx instanceof DefDataContext) {
      return visitDefData((DefDataContext) ctx);
    } else
    if (ctx instanceof DefClassContext) {
      ClassDefinition classDef = myParent.getClass(((DefClassContext) ctx).ID().getText(), myModuleLoader.getErrors());
      if (classDef == null) return null;
      boolean oldOnlyStatics = myOnlyStatics;
      myOnlyStatics = !classDef.hasErrors();
      classDef.hasErrors(false);
      Definition oldParent = myParent;
      myParent = classDef;
      visitDefClass((DefClassContext) ctx);
      myOnlyStatics = oldOnlyStatics;
      myParent = oldParent;

      if (myParent instanceof ClassDefinition && myOnlyStatics && !TypeChecking.checkOnlyStatic(myModuleLoader, (ClassDefinition) myParent, classDef, classDef.getName())) {
          return null;
      }

      myParent.addField(classDef, myModuleLoader.getErrors());
      return classDef;
    } else
    if (ctx instanceof DefCmdContext) {
      visitDefCmd((DefCmdContext) ctx);
      return null;
    } else
    if (ctx instanceof DefOverrideContext) {
      if (myParent instanceof ClassDefinition) {
        if (myOnlyStatics) {
          List<NameContext> names = ((DefOverrideContext) ctx).name();
          Name name = getName(names.size() == 1 ? names.get(0) : names.get(1));
          if (name == null) {
            return null;
          }
          TypeChecking.checkOnlyStatic(myModuleLoader, (ClassDefinition) myParent, null, name);
          return null;
        } else {
          return visitDefOverride((DefOverrideContext) ctx);
        }
      } else {
        myModuleLoader.getErrors().add(new ParserError(myModule, tokenPosition(ctx.getStart()), "\\override is not allowed inside \\where."));
        return null;
      }
    } else {
      throw new IllegalStateException();
    }
  }

  @Override
  public Void visitDefCmd(DefCmdContext ctx) {
    if (ctx == null) return null;
    Definition module = visitModule(ctx.name(0), ctx.fieldAcc());
    if (module == null) return null;
    boolean remove = ctx.nsCmd() instanceof CloseCmdContext;
    boolean export = ctx.nsCmd() instanceof ExportCmdContext;

    if (ctx.name().size() > 1) {
      for (int i = 1; i < ctx.name().size(); ++i) {
        String name = ctx.name(i) instanceof NameBinOpContext ? ((NameBinOpContext) ctx.name(i)).BIN_OP().getText() : ((NameIdContext) ctx.name(i)).ID().getText();
        Definition definition = module.getStaticField(name);
        if (definition == null) {
          myModuleLoader.getErrors().add(new ParserError(myModule, tokenPosition(ctx.name(i).getStart()), name + " is not a static field of " + module.getFullName()));
          continue;
        }
        if (remove) {
          if (!myParent.getFields().contains(definition))
            myParent.removePrivateField(definition);
        } else {
          if (!definition.isAbstract() && myParent.canOpen(definition)) {
            myParent.addPrivateField(definition);
            if (export) {
              myParent.addField(definition, myModuleLoader.getErrors());
            }
          }
        }
      }
    } else {
      for (Definition definition : module.getStaticFields()) {
        if (remove) {
          if (!myParent.getFields().contains(definition))
            myParent.removePrivateField(definition);
        } else {
          if (!definition.isAbstract() && myParent.canOpen(definition)) {
            myParent.addPrivateField(definition);
            if (export) {
              myParent.addField(definition, myModuleLoader.getErrors());
            }
          }
        }
      }
    }
    return null;
  }

  private static class FunctionContext {
    ExprContext typeCtx;
    ArrowContext arrowCtx;
    ExprContext termCtx;
  }

  @Override
  public FunctionContext visitWithType(WithTypeContext ctx) {
    FunctionContext result = new FunctionContext();
    result.typeCtx = ctx.expr();
    result.arrowCtx = null;
    result.termCtx = null;
    return result;
  }

  @Override
  public FunctionContext visitWithTypeAndTerm(WithTypeAndTermContext ctx) {
    FunctionContext result = new FunctionContext();
    result.typeCtx = ctx.expr(0);
    result.arrowCtx = ctx.arrow();
    result.termCtx = ctx.expr(1);
    return result;
  }

  @Override
  public FunctionContext visitWithTerm(WithTermContext ctx) {
    FunctionContext result = new FunctionContext();
    result.typeCtx = null;
    result.arrowCtx = ctx.arrow();
    result.termCtx = ctx.expr();
    return result;
  }

  @Override
  public FunctionDefinition visitDefFunction(DefFunctionContext ctx) {
    if (ctx == null) return null;
    FunctionContext functionCtx = (FunctionContext) visit(ctx.typeTermOpt());
    if (functionCtx.termCtx == null && myOnlyStatics) {
      Name name = getName(ctx.name());
      if (name == null) {
        return null;
      }
      if (myParent instanceof ClassDefinition && !((ClassDefinition) myParent).isLocal())
        TypeChecking.checkOnlyStatic(myModuleLoader, (ClassDefinition) myParent, null, name);
      return null;
    }
    return visitDefFunction(false, null, ctx.precedence(), ctx.name(), ctx.tele(), functionCtx, ctx.where() == null ? null :  ctx.where().def());
  }

  @Override
  public FunctionDefinition visitDefOverride(DefOverrideContext ctx) {
    if (ctx == null) return null;
    FunctionContext functionCtx = (FunctionContext) visit(ctx.typeTermOpt());
    return visitDefFunction(true, ctx.name().size() == 1 ? null : ctx.name(0), null, ctx.name().size() == 1 ? ctx.name(0) : ctx.name(1), ctx.tele(), functionCtx, ctx.where() == null ? null : ctx.where().def());
  }

  private FunctionDefinition visitDefFunction(boolean overridden, NameContext originalName, PrecedenceContext precCtx, NameContext nameCtx, List<TeleContext> teleCtx, FunctionContext functionCtx, List<DefContext> defs) {FunctionDefinition typedDef;
    if (overridden) {
      typedDef = new OverriddenDefinition(getName(nameCtx), myParent, visitPrecedence(precCtx), null, null, visitArrow(functionCtx.arrowCtx), null, null);
    } else {
      typedDef = new FunctionDefinition(getName(nameCtx), myParent, visitPrecedence(precCtx), null, null, visitArrow(functionCtx.arrowCtx), null);
    }

    Definition oldParent = myParent;
    myParent = typedDef;

    if (defs != null)
      visitDefList(defs);

    Concrete.FunctionDefinition def = visitFunctionRawBegin(overridden, originalName, precCtx, nameCtx, teleCtx, functionCtx);
    if (def == null) {
      myParent = oldParent;
      return null;
    }

    List<Binding> localContext = new ArrayList<>();
    if (TypeChecking.typeCheckFunctionBegin(myModuleLoader, def, localContext, null, typedDef)) {
      Concrete.Expression term = functionCtx.termCtx == null ? null : visitExpr(functionCtx.termCtx);
      TypeChecking.typeCheckFunctionEnd(myModuleLoader, term, typedDef, localContext, null, myOnlyStatics);
    } else {
      typedDef = null;
    }

    visitFunctionRawEnd(def);
    myParent = oldParent;
    return typedDef;
  }

  private Concrete.FunctionDefinition visitFunctionRawBegin(boolean overridden, NameContext originalNameCtx, PrecedenceContext precCtx, NameContext nameCtx, List<TeleContext> teleCtx, FunctionContext functionCtx) {
    Name name = getName(nameCtx);
    if (name == null) {
      return null;
    }

    Name originalName = getName(originalNameCtx);
    int size = myContext.size();
    List<Concrete.Argument> arguments = visitFunctionArguments(teleCtx, overridden);
    if (arguments == null) {
      trimToSize(myContext, size);
      return null;
    }

    Concrete.Expression type = functionCtx.typeCtx == null ? null : visitExpr(functionCtx.typeCtx);
    Definition.Arrow arrow = visitArrow(functionCtx.arrowCtx);
    return new Concrete.FunctionDefinition(name.position, name, precCtx == null ? null : visitPrecedence(precCtx), arguments, type, arrow, null, overridden, originalName == null ? null : originalName);
  }

  private Abstract.Definition.Arrow visitArrow(ArrowContext arrowCtx) {
    return arrowCtx instanceof ArrowLeftContext ? Abstract.Definition.Arrow.LEFT : arrowCtx instanceof ArrowRightContext ? Abstract.Definition.Arrow.RIGHT : null;
  }

  private void visitFunctionRawEnd(Concrete.FunctionDefinition definition) {
    for (Concrete.Argument argument : definition.getArguments()) {
      if (argument instanceof Concrete.TelescopeArgument) {
        for (String ignored : ((Concrete.TelescopeArgument) argument).getNames()) {
          myContext.remove(myContext.size() - 1);
        }
      } else {
        myContext.remove(myContext.size() - 1);
      }
    }
  }

  private List<Concrete.Argument> visitFunctionArguments(List<TeleContext> teleCtx) {
    return visitFunctionArguments(teleCtx, false);
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
        myModuleLoader.getErrors().add(new ParserError(myModule, tokenPosition(tele.getStart()), "Expected a typed variable"));
        return null;
      }
    }
    return arguments;
  }

  private Abstract.Definition.Arrow getArrow(ArrowContext arrowCtx) {
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
      myModuleLoader.getErrors().add(new ParserError(myModule, tokenPosition(ctx.NUMBER().getSymbol()), "Precedence out of range: " + priority));

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
  public Concrete.NamePattern visitPatternAny(PatternAnyContext ctx) {
    return new Concrete.NamePattern(tokenPosition(ctx.getStart()), null);
  }

  @Override
  public Concrete.NamePattern visitPatternID(PatternIDContext ctx) {
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

  private void applyPatternToContext(Abstract.Pattern pattern, int varIndex) {
    if (pattern instanceof Abstract.NamePattern && ((Abstract.NamePattern) pattern).getName() == null)
      return;
    List<String> names = new ArrayList<>();
    collectPatternNames(pattern, names);
    List<String> oldContext = new ArrayList<>(myContext);
    myContext.clear();
    for (int i = 0; i < varIndex; ++i) {
      myContext.add(oldContext.get(i));
    }
    myContext.addAll(names);
    for (int i = varIndex + 1; i < oldContext.size(); ++i) {
      myContext.add(oldContext.get(i));
    }
  }

  @Override
  public Concrete.ConstructorPattern visitPatternConstructor(PatternConstructorContext ctx) {
    return new Concrete.ConstructorPattern(tokenPosition(ctx.getStart()), getName(ctx.name()), visitPatterns(ctx.patternx()));
  }

  private void visitConstructorDef(ConstructorDefContext ctx, Concrete.DataDefinition def) {
    List<String> oldContext = new ArrayList<>(myContext);
    List<Concrete.Pattern> patterns = null;

    if (ctx instanceof WithPatternsContext) {
      WithPatternsContext wpCtx = (WithPatternsContext) ctx;
      Name dataDefName = getName(wpCtx.name());
      if (!def.getName().equals(dataDefName)) {
        myModuleLoader.getErrors().add(new ParserError(myModule, dataDefName.position, "Expected a data type name: " + def.getName()));
        return;
      }

      patterns = visitPatterns(wpCtx.patternx());

      ProcessImplicitResult result = processImplicit(patterns, def.getParameters());
      if (result.patterns == null) {
        if (result.wrongImplicitPosition < patterns.size()) {
          myModuleLoader.getErrors().add(new ParserError(myModule,
              tokenPosition(wpCtx.patternx(result.wrongImplicitPosition).start), "Unexpected implicit argument"));
        } else {
          myModuleLoader.getErrors().add(new ParserError(myModule, tokenPosition(wpCtx.name().start), "Too few explicit arguments, expected: " + result.numExplicit));
        }
        return;
      }
      for (int i = 0; i < result.patterns.size(); i++) {
        applyPatternToContext(result.patterns.get(i), myContext.size() - result.patterns.size() + i);
      }
    }

    List<ConstructorContext> constructorCtxs = ctx instanceof WithPatternsContext ?
        ((WithPatternsContext) ctx).constructor() : Collections.singletonList(((NoPatternsContext) ctx).constructor());

    for (ConstructorContext conCtx : constructorCtxs) {
      Name conName = getName(conCtx.name());
      if (conName == null) {
        continue;
      }

      int size = myContext.size();
      List<Concrete.TypeArgument> arguments = visitTeles(conCtx.tele());
      trimToSize(myContext, size);

      if (arguments == null) {
        continue;
      }
      def.getConstructors().add(new Concrete.Constructor(conName.position, conName, visitPrecedence(conCtx.precedence()), new Universe.Type(), arguments, def, patterns));
    }

    myContext = oldContext;
 }

  @Override
  public DataDefinition visitDefData(DefDataContext ctx) {
    if (ctx == null) return null;
    Name name = getName(ctx.name());
    if (name == null) {
      return null;
    }
    int origSize = myContext.size();
    List<Concrete.TypeArgument> parameters = visitTeles(ctx.tele());
    if (parameters == null) {
      trimToSize(myContext, origSize);
      return null;
    }

    Concrete.Expression type = ctx.expr() == null ? null : visitExpr(ctx.expr());
    if (type != null && !(type instanceof Concrete.UniverseExpression)) {
      myModuleLoader.getErrors().add(new ParserError(myModule, tokenPosition(ctx.expr().getStart()), "Expected a universe"));
      trimToSize(myContext, origSize);
      return null;
    }

    Universe universe = type == null ? null : ((Concrete.UniverseExpression) type).getUniverse();
    Concrete.DataDefinition def = new Concrete.DataDefinition(name.position, name, visitPrecedence(ctx.precedence()), universe, parameters);
    List<Binding> localContext = new ArrayList<>();
    DataDefinition typedDef = TypeChecking.typeCheckDataBegin(myModuleLoader, myParent, def, localContext);
    if (typedDef == null)
      return null;

    for (ConstructorDefContext constructorDefCtx : ctx.constructorDef()) {
      visitConstructorDef(constructorDefCtx, def);
    }

    for (int i = 0; i < def.getConstructors().size(); i++) {
      TypeChecking.typeCheckConstructor(myModuleLoader, typedDef, def.getConstructors().get(i), localContext, i);
    }

    trimToSize(myContext, origSize);
    TypeChecking.typeCheckDataEnd(myModuleLoader, myParent, def, typedDef, null, myOnlyStatics);
    return typedDef;
  }

  @Override
  public Void visitDefClass(DefClassContext ctx) {
    if (ctx == null) return null;
    visitDefs(ctx.classFields() == null ? null : ctx.classFields().defs());
    return null;
  }

  @Override
  public Concrete.InferHoleExpression visitUnknown(UnknownContext ctx) {
    if (ctx == null) return null;
    return new Concrete.InferHoleExpression(tokenPosition(ctx.getStart()));
  }

  @Override
  public Concrete.PiExpression visitArr(ArrContext ctx) {
    if (ctx == null) return null;
    Concrete.Expression domain = visitExpr(ctx.expr(0));
    if (domain == null) return null;
    List<Concrete.TypeArgument> arguments = new ArrayList<>(1);
    arguments.add(new Concrete.TypeArgument(domain.getPosition(), true, domain));
    myContext.add(null);
    Concrete.Expression codomain = visitExpr(ctx.expr(1));
    myContext.remove(myContext.size() - 1);
    if (codomain == null) return null;
    return new Concrete.PiExpression(tokenPosition(ctx.getToken(ARROW, 0).getSymbol()), arguments, codomain);
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

  private List<Concrete.Argument> visitLamTele(TeleContext tele) {
    List<Concrete.Argument> arguments = new ArrayList<>(3);
    if (tele instanceof TeleLiteralContext) {
      LiteralContext literalContext = ((TeleLiteralContext) tele).literal();
      if (literalContext instanceof IdContext && ((IdContext) literalContext).name() instanceof NameIdContext) {
        String name = ((NameIdContext) ((IdContext) literalContext).name()).ID().getText();
        arguments.add(new Concrete.NameArgument(tokenPosition(((NameIdContext) ((IdContext) literalContext).name()).ID().getSymbol()), true, name));
        myContext.add(name);
      } else
      if (literalContext instanceof UnknownContext) {
        arguments.add(new Concrete.NameArgument(tokenPosition(literalContext.getStart()), true, null));
        myContext.add(null);
      } else {
        myModuleLoader.getErrors().add(new ParserError(myModule, tokenPosition(literalContext.getStart()), "Unexpected token. Expected an identifier."));
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
        for (Concrete.NameArgument var : vars) {
          myContext.add(var.getName());
        }
      } else {
        List<String> args = new ArrayList<>(vars.size());
        for (Concrete.NameArgument var : vars) {
          args.add(var.getName());
          myContext.add(var.getName());
        }
        arguments.add(new Concrete.TelescopeArgument(tokenPosition(tele.getStart()), explicit, args, typeExpr));
      }
    }
    return arguments;
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
    int size = myContext.size();
    List<Concrete.Argument> args = visitLamTeles(ctx.tele());
    if (args == null) {
      trimToSize(myContext, size);
      return null;
    }
    Concrete.Expression body = visitExpr(ctx.expr());
    trimToSize(myContext, size);
    if (body == null) return null;
    return new Concrete.LamExpression(tokenPosition(ctx.getStart()), args, body);
  }

  @Override
  public Concrete.Expression visitId(IdContext ctx) {
    if (ctx == null) return null;
    boolean binOp = ctx.name() instanceof NameBinOpContext;
    String name = binOp ? ((NameBinOpContext) ctx.name()).BIN_OP().getText() : ((NameIdContext) ctx.name()).ID().getText();
    return findId(name, binOp, tokenPosition(ctx.name().getStart()));
  }

  private Concrete.Expression findId(String name, boolean binOp, Concrete.Position position) {
    Definition child = Prelude.PRELUDE.getStaticField(name);
    if (child != null) {
      return new Concrete.DefCallExpression(position, null, child);
    }

    if (!binOp) {
      if (myContext.contains(name)) {
        return new Concrete.VarExpression(position, name);
      }
    }

    for (Definition definition = myParent; definition != null; definition = definition.getParent()) {
      Definition member = definition.getPrivateField(name);
        if (member != null) {
          return new Concrete.DefCallExpression(position, null, member);
        }
    }

    if (!binOp) {
      Definition definition = myModuleLoader.loadModule(new Module(myModuleLoader.rootModule(), name), true);
      if (definition != null) {
        return new Concrete.DefCallExpression(position, null, definition);
      }
    }

    myModuleLoader.getErrors().add(new ParserError(myModule, position, "Not in scope: " + name));
    return null;
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
          if (expr == null) return null;
          arguments.add(new Concrete.TypeArgument(true, expr));
          myContext.add(null);
          continue;
        }
      } else {
        typedExpr = ((ImplicitContext) tele).typedExpr();
      }
      if (typedExpr instanceof TypedContext) {
        Concrete.Expression type = visitExpr(((TypedContext) typedExpr).expr(1));
        if (type == null) return null;
        List<Concrete.NameArgument> args = getVars(((TypedContext) typedExpr).expr(0));
        if (args == null) return null;

        List<String> vars = new ArrayList<>(args.size());
        for (Concrete.NameArgument arg : args) {
          vars.add(arg.getName());
          myContext.add(arg.getName());
        }
        arguments.add(new Concrete.TelescopeArgument(tokenPosition(tele.getStart()), explicit, vars, type));
      } else {
        Concrete.Expression expr = visitExpr(((NotTypedContext) typedExpr).expr());
        if (expr == null) return null;
        arguments.add(new Concrete.TypeArgument(explicit, expr));
        myContext.add(null);
      }
    }
    return arguments;
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
    Concrete.Expression result = new Concrete.DefCallExpression(pos, null, Prelude.ZERO);
    for (int i = 0; i < number; ++i) {
      result = new Concrete.AppExpression(pos, new Concrete.DefCallExpression(pos, null, Prelude.SUC), new Concrete.ArgumentExpression(result, true, false));
    }
    return result;
  }

  @Override
  public Concrete.SigmaExpression visitSigma(SigmaContext ctx) {
    if (ctx == null) return null;
    int size = myContext.size();
    List<Concrete.TypeArgument> args = visitTeles(ctx.tele());
    trimToSize(myContext, size);
    if (args == null) return null;

    for (Concrete.TypeArgument arg : args) {
      if (!arg.getExplicit()) {
        myModuleLoader.getErrors().add(new ParserError(myModule, arg.getPosition(), "Fields in sigma types must be explicit"));
      }
    }
    return new Concrete.SigmaExpression(tokenPosition(ctx.getStart()), args);
  }

  @Override
  public Concrete.PiExpression visitPi(PiContext ctx) {
    if (ctx == null) return null;
    int size = myContext.size();
    List<Concrete.TypeArgument> args = visitTeles(ctx.tele());
    if (args == null) {
      trimToSize(myContext, size);
      return null;
    }
    Concrete.Expression codomain = visitExpr(ctx.expr());
    trimToSize(myContext, size);
    if (codomain == null) return null;
    return new Concrete.PiExpression(tokenPosition(ctx.getStart()), args, codomain);
  }

  private Concrete.Expression visitFieldsAcc(Concrete.Expression expr, List<FieldAccContext> fieldAccs) {
    for (FieldAccContext field : fieldAccs) {
      if (field instanceof ClassFieldContext) {
        Name name = getName(((ClassFieldContext) field).name());
        if (name == null) {
          return null;
        }

        if (expr instanceof Concrete.DefCallExpression && ((Concrete.DefCallExpression) expr).getDefinition() != null) {
          Definition definition = ((Concrete.DefCallExpression) expr).getDefinition();
          Definition classField = definition.getStaticField(name.name);
          if (classField == null && definition instanceof ClassDefinition) {
            classField = myModuleLoader.loadModule(new Module((ClassDefinition) definition, name.name), true);
          }
          if (classField == null && definition instanceof FunctionDefinition) {
            FunctionDefinition functionDefinition = (FunctionDefinition) definition;
            if (!functionDefinition.typeHasErrors() && functionDefinition.getArguments().isEmpty() && functionDefinition.getResultType() instanceof DefCallExpression && ((DefCallExpression) functionDefinition.getResultType()).getDefinition() instanceof ClassDefinition) {
              expr = new Concrete.DefCallNameExpression(expr.getPosition(), expr, name);
              continue;
            }
          }
          if (classField == null) {
            myModuleLoader.getErrors().add(new ParserError(myModule, tokenPosition(((ClassFieldContext) field).name().getStart()), name.name + " is not a static field of " + definition.getFullName()));
            return null;
          }
          expr = new Concrete.DefCallExpression(expr.getPosition(), null, classField);
          continue;
        }
        expr = new Concrete.DefCallNameExpression(expr.getPosition(), expr, name);
      } else {
        expr = new Concrete.ProjExpression(expr.getPosition(), expr, Integer.valueOf(((SigmaFieldContext) field).NUMBER().getText()) - 1);
      }
    }
    return expr;
  }

  @Override
  public Concrete.Expression visitAtomFieldsAcc(AtomFieldsAccContext ctx) {
    if (ctx == null) return null;
    Concrete.Expression expr = visitExpr(ctx.atom());
    if (expr == null) return null;

    expr = visitFieldsAcc(expr, ctx.fieldAcc());
    if (expr == null) return null;

    if (ctx.classFields() != null) {
      if (!(expr instanceof Concrete.DefCallExpression && ((Concrete.DefCallExpression) expr).getDefinition() instanceof ClassDefinition)) {
        myModuleLoader.getErrors().add(new ParserError(myModule, expr.getPosition(), "Expected a class name"));
        return null;
      }

      ClassDefinition newParent = (ClassDefinition) ((Concrete.DefCallExpression) expr).getDefinition();
      Definition oldParent = myParent;
      myParent = newParent;
      List<Concrete.FunctionDefinition> definitions = visitDefsRaw(ctx.classFields().defs());
      myParent = oldParent;

      expr = new Concrete.ClassExtExpression(tokenPosition(ctx.getStart()), newParent, definitions);
    }
    return expr;
  }

  private Concrete.Expression visitAtoms(Concrete.Expression expr, List<ArgumentContext> arguments) {
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

  private class StackElem {
    public Concrete.Expression argument;
    public Definition definition;

    public StackElem(Concrete.Expression argument, Definition definition) {
      this.argument = argument;
      this.definition = definition;
    }
  }

  private void pushOnStack(List<StackElem> stack, StackElem elem, Concrete.Position position) {
    if (stack.isEmpty()) {
      stack.add(elem);
      return;
    }

    StackElem topElem = stack.get(stack.size() - 1);
    Definition.Precedence prec = topElem.definition.getPrecedence();
    Definition.Precedence prec2 = elem.definition.getPrecedence();

    if (prec.priority < prec2.priority || (prec.priority == prec2.priority && prec.associativity == Definition.Associativity.RIGHT_ASSOC && prec2.associativity == Definition.Associativity.RIGHT_ASSOC)) {
      stack.add(elem);
      return;
    }

    if (!(prec.priority > prec2.priority || (prec.priority == prec2.priority && prec.associativity == Definition.Associativity.LEFT_ASSOC && prec2.associativity == Definition.Associativity.LEFT_ASSOC))) {
      String msg = "Precedence parsing error: cannot mix (" + topElem.definition.getName() + ") [" + prec + "] and (" + elem.definition.getName() + ") [" + prec2 + "] in the same infix expression";
      myModuleLoader.getErrors().add(new ParserError(myModule, position, msg));
    }
    stack.remove(stack.size() - 1);
    pushOnStack(stack, new StackElem(new Concrete.BinOpExpression(position, new Concrete.ArgumentExpression(topElem.argument, true, false), topElem.definition, new Concrete.ArgumentExpression(elem.argument, true, false)), elem.definition), position);
  }

  private Concrete.Expression rollUpStack(List<StackElem> stack, Concrete.Expression expr) {
    for (int i = stack.size() - 1; i >= 0; --i) {
      expr = new Concrete.BinOpExpression(stack.get(i).argument.getPosition(), new Concrete.ArgumentExpression(stack.get(i).argument, true, false), stack.get(i).definition, new Concrete.ArgumentExpression(expr, true, false));
    }
    return expr;
  }

  @Override
  public Concrete.Expression visitBinOp(BinOpContext ctx) {
    if (ctx == null) return null;
    List<StackElem> stack = new ArrayList<>(ctx.binOpLeft().size());
    for (BinOpLeftContext leftContext : ctx.binOpLeft()) {
      Concrete.Position position = tokenPosition(leftContext.infix().getStart());
      Definition def;
      if (leftContext.infix() instanceof InfixBinOpContext) {
        def = visitInfixBinOp((InfixBinOpContext) leftContext.infix());
      } else {
        def = visitInfixId((InfixIdContext) leftContext.infix());
      }

      Concrete.Expression expr = visitAtomFieldsAcc(leftContext.atomFieldsAcc());
      if (expr == null) continue;
      expr = visitAtoms(expr, leftContext.argument());
      if (expr == null) continue;
      if (leftContext.maybeNew() instanceof WithNewContext) {
        expr = new Concrete.NewExpression(tokenPosition(leftContext.getStart()), expr);
      }

      if (def == null) {
        stack = new ArrayList<>(ctx.binOpLeft().size() - stack.size());
        continue;
      }

      pushOnStack(stack, new StackElem(expr, def), position);
    }

    Concrete.Expression expr = visitAtomFieldsAcc(ctx.atomFieldsAcc());
    if (expr == null) return null;
    expr = visitAtoms(expr, ctx.argument());
    if (expr == null) return null;
    if (ctx.maybeNew() instanceof WithNewContext) {
      expr = new Concrete.NewExpression(tokenPosition(ctx.getStart()), expr);
    }
    return rollUpStack(stack, expr);
  }

  @Override
  public Definition visitInfixBinOp(InfixBinOpContext ctx) {
    if (ctx == null) return null;
    String name = ctx.BIN_OP().getText();
    Concrete.Position position = tokenPosition(ctx.BIN_OP().getSymbol());
    Concrete.Expression expr = findId(name, true, position);
    if (expr == null) return null;
    if (!(expr instanceof Concrete.DefCallExpression)) {
      myModuleLoader.getErrors().add(new ParserError(myModule, position, "Infix notation cannot be used with local variables"));
      return null;
    }

    return ((Concrete.DefCallExpression) expr).getDefinition();
  }

  @Override
  public Definition visitInfixId(InfixIdContext ctx) {
    if (ctx == null) return null;
    return visitModule(ctx.name(), ctx.fieldAcc());
  }

  public Definition visitModule(NameContext nameCtx, List<FieldAccContext> fieldAccCtx) {
    boolean binOp = nameCtx instanceof NameBinOpContext;
    String name = binOp ? ((NameBinOpContext) nameCtx).BIN_OP().getText() : ((NameIdContext) nameCtx).ID().getText();
    Concrete.Expression expr = findId(name, binOp, tokenPosition(nameCtx.getStart()));
    if (expr == null) return null;
    if (!(expr instanceof Concrete.DefCallExpression)) {
      myModuleLoader.getErrors().add(new ParserError(myModule, tokenPosition(nameCtx.getStart()), "Expected a global definition"));
      return null;
    }

    expr = visitFieldsAcc(expr, fieldAccCtx);
    if (expr == null) return null;
    if (!(expr instanceof Concrete.DefCallExpression) || ((Concrete.DefCallExpression) expr).getDefinition() == null) {
      myModuleLoader.getErrors().add(new ParserError(myModule, tokenPosition(nameCtx.getStart()), "Expected a global definition"));
      return null;
    }

    return ((Concrete.DefCallExpression) expr).getDefinition();
  }

  @Override
  public Concrete.Expression visitExprElim(ExprElimContext ctx) {
    if (ctx == null) return null;
    List<Concrete.Clause> clauses = new ArrayList<>(ctx.clause().size());

    Concrete.Expression elimExpr = visitExpr(ctx.expr());
    if (elimExpr == null) return null;

    int elimIndex = -1;
    if (ctx.elimCase() instanceof ElimContext) {
      if (!(elimExpr instanceof Concrete.VarExpression)) {
        myModuleLoader.getErrors().add(new ParserError(myModule, elimExpr.getPosition(), "\\elim can be applied only to a local variable"));
        return null;
      }
      String name = ((Concrete.VarExpression) elimExpr).getName();
      elimIndex = myContext.lastIndexOf(name);
      if (elimIndex == -1) {
        myModuleLoader.getErrors().add(new ParserError(myModule, elimExpr.getPosition(), "Not in scope: " + name));
        return null;
      }
    }

    List<String> oldContext = new ArrayList<>(myContext);
    for (ClauseContext clauseCtx : ctx.clause()) {
      Concrete.Pattern pattern;
      if (clauseCtx.name() != null) {
        pattern = new Concrete.ConstructorPattern(tokenPosition(clauseCtx.name().start), getName(clauseCtx.name()), visitPatterns(clauseCtx.patternx()));
      } else {
        pattern = new Concrete.NamePattern(tokenPosition(clauseCtx.start), null);
      }

      applyPatternToContext(pattern, ctx.elimCase() instanceof  ElimContext  ? elimIndex : myContext.size());
      Definition.Arrow arrow = clauseCtx.arrow() instanceof ArrowRightContext ? Definition.Arrow.RIGHT : Definition.Arrow.LEFT;

      Concrete.Expression expr = visitExpr(clauseCtx.expr());
      if (expr == null) {
        myContext = oldContext;
        return null;
      }
      clauses.add(new Concrete.Clause(tokenPosition(clauseCtx.getStart()), pattern, arrow, expr, null));

      myContext = new ArrayList<>(oldContext);
    }

    Concrete.ElimCaseExpression result = ctx.elimCase() instanceof ElimContext ?
            new Concrete.ElimExpression(tokenPosition(ctx.getStart()), elimExpr, clauses) :
            new Concrete.CaseExpression(tokenPosition(ctx.getStart()), elimExpr, clauses);

    for (Concrete.Clause clause : clauses) {
      clause.setElimExpression(result);
    }
    return result;
  }

  @Override
  public Concrete.LetClause visitLetClause(LetClauseContext ctx) {
    final int oldContextSize = myContext.size();
    final String name = ctx.ID().getText();

    final List<Concrete.Argument> arguments = visitFunctionArguments(ctx.tele());
    if (arguments == null) {
      trimToSize(myContext, oldContextSize);
      return null;
    }
    final Concrete.Expression resultType = ctx.typeAnnotation() == null ? null: visitExpr(ctx.typeAnnotation().expr());
    final Abstract.Definition.Arrow arrow = getArrow(ctx.arrow());
    final Concrete.Expression term = visitExpr(ctx.expr());

    trimToSize(myContext, oldContextSize);

    myContext.add(name);
    return new Concrete.LetClause(tokenPosition(ctx.getStart()), name, new ArrayList<>(arguments), resultType, arrow, term);
  }

  @Override
  public Concrete.LetExpression visitLet(LetContext ctx) {
    final int oldContextSize = myContext.size();
    final List<Concrete.LetClause> clauses = new ArrayList<>();
    for (LetClauseContext clauseCtx : ctx.letClause()) {
      Concrete.LetClause clause = visitLetClause(clauseCtx);
      if (clause == null)
        return null;
      clauses.add(visitLetClause(clauseCtx));
    }

    final Concrete.Expression expr = visitExpr(ctx.expr());
    trimToSize(myContext, oldContextSize);
    return new Concrete.LetExpression(tokenPosition(ctx.getStart()), clauses, expr);
  }

  private static Concrete.Position tokenPosition(Token token) {
    return new Concrete.Position(token.getLine(), token.getCharPositionInLine());
  }

  @Override
  public Concrete.ErrorExpression visitHole(HoleContext ctx) {
    if (ctx == null) return null;
    return new Concrete.ErrorExpression(tokenPosition(ctx.getStart()));
  }

  private static Name getName(NameContext ctx) {
    if (ctx == null) {
      return null;
    }

    Name result = new Name(null, null);
    result.fixity = ctx instanceof NameIdContext ? Abstract.Definition.Fixity.PREFIX : Abstract.Definition.Fixity.INFIX;
    if (result.fixity == Abstract.Definition.Fixity.PREFIX) {
      result.name = ((NameIdContext) ctx).ID().getText();
      result.position = tokenPosition(((NameIdContext) ctx).ID().getSymbol());
    } else {
      result.name = ((NameBinOpContext) ctx).BIN_OP().getText();
      result.position = tokenPosition(((NameBinOpContext) ctx).BIN_OP().getSymbol());
    }
    return result;
  }

  private static class Name extends Utils.Name {
    Concrete.Position position;

    public Name(String name, Abstract.Definition.Fixity fixity) {
      super(name, fixity);
    }

    public Name(String name) {
      super(name);
    }
  }
}
