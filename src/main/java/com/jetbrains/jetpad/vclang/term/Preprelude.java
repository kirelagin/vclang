package com.jetbrains.jetpad.vclang.term;

import com.jetbrains.jetpad.vclang.module.ModuleID;
import com.jetbrains.jetpad.vclang.module.ModulePath;
import com.jetbrains.jetpad.vclang.naming.DefinitionResolvedName;
import com.jetbrains.jetpad.vclang.naming.ModuleResolvedName;
import com.jetbrains.jetpad.vclang.naming.Namespace;
import com.jetbrains.jetpad.vclang.naming.NamespaceMember;
import com.jetbrains.jetpad.vclang.term.context.param.DependentLink;
import com.jetbrains.jetpad.vclang.term.context.param.EmptyDependentLink;
import com.jetbrains.jetpad.vclang.term.definition.*;
import com.jetbrains.jetpad.vclang.term.expr.*;
import com.jetbrains.jetpad.vclang.term.pattern.elimtree.ElimTreeNode;
import com.jetbrains.jetpad.vclang.term.pattern.elimtree.EmptyElimTreeNode;

import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

import static com.jetbrains.jetpad.vclang.term.expr.ExpressionFactory.*;

public class Preprelude extends Namespace {
  public static ModuleID moduleID = new ModuleID() {
    @Override
    public ModulePath getModulePath() {
      return new ModulePath("Preprelude");
    }
  };

  public static ClassDefinition PRE_PRELUDE_CLASS;

  public static Namespace PRE_PRELUDE = new Preprelude();

  public static DataDefinition INTERVAL;
  public static Constructor LEFT, RIGHT, ABSTRACT;

  public static DataDefinition NAT;
  public static Constructor ZERO, SUC;

  public static DataDefinition LVL;
  public static Constructor ZERO_LVL;
  public static Constructor SUC_LVL;
  public static FunctionDefinition MAX_LVL;

  public static DataDefinition CNAT;
  public static Constructor FIN, INF;

  public static FunctionDefinition MAX_NAT;
  public static FunctionDefinition MAX_CNAT;
  public static FunctionDefinition SUC_CNAT;
  public static FunctionDefinition SUC_LEVEL;
  public static FunctionDefinition MAX_LEVEL;

  public static ClassDefinition LEVEL;
  public static ClassField PLEVEL;
  public static ClassField HLEVEL;

  private static Map<Integer, FunctionDefinition> LIFTS = new HashMap<>();

  static {
    PRE_PRELUDE_CLASS = new ClassDefinition(new ModuleResolvedName(moduleID), null);

    NAT = new DataDefinition(new DefinitionResolvedName(PRE_PRELUDE, "Nat"), Abstract.Definition.DEFAULT_PRECEDENCE, null, EmptyDependentLink.getInstance());
    Namespace natNamespace = PRE_PRELUDE.getChild(NAT.getName());
    ZERO = new Constructor(new DefinitionResolvedName(natNamespace, "zero"), Abstract.Definition.DEFAULT_PRECEDENCE, null, EmptyDependentLink.getInstance(), NAT);
    SUC = new Constructor(new DefinitionResolvedName(natNamespace, "suc"), Abstract.Definition.DEFAULT_PRECEDENCE, null, param(DataCall(NAT)), NAT);

    PRE_PRELUDE.addDefinition(NAT);
    PRE_PRELUDE.addMember(NAT.addConstructor(ZERO));
    PRE_PRELUDE.addMember(NAT.addConstructor(SUC));

    LVL = new DataDefinition(new DefinitionResolvedName(PRE_PRELUDE, "Lvl"), Abstract.Definition.DEFAULT_PRECEDENCE, null, EmptyDependentLink.getInstance());
    PRE_PRELUDE.addDefinition(LVL);

    ZERO_LVL = new Constructor(new DefinitionResolvedName(PRE_PRELUDE.getChild(LVL.getName()), "zeroLvl"), Abstract.Definition.DEFAULT_PRECEDENCE, null, EmptyDependentLink.getInstance(), LVL);
    DependentLink sucLvlParameter = param("l", DataCall(LVL));
    SUC_LVL = new Constructor(new DefinitionResolvedName(PRE_PRELUDE.getChild(LVL.getName()), "sucLvl"), Abstract.Definition.DEFAULT_PRECEDENCE, null, sucLvlParameter, LVL);
    PRE_PRELUDE.addMember(LVL.addConstructor(ZERO_LVL));
    PRE_PRELUDE.addMember(LVL.addConstructor(SUC_LVL));

    DependentLink maxLvlParameter1 = param(DataCall(LVL));
    DependentLink maxLvlParameter2 = param(DataCall(LVL));
    maxLvlParameter1.setNext(maxLvlParameter2);
    DependentLink sucLvlParameterPrime = param("l'", DataCall(LVL));
    MAX_LVL = new FunctionDefinition(new DefinitionResolvedName(PRE_PRELUDE, "maxLvl"), Abstract.Definition.DEFAULT_PRECEDENCE, maxLvlParameter1, DataCall(LVL), null, null);
    ElimTreeNode maxLvlElimTree = top(maxLvlParameter1, branch(maxLvlParameter1, tail(maxLvlParameter2),
            clause(ZERO_LVL, EmptyDependentLink.getInstance(), branch(maxLvlParameter2, tail(),
                    clause(ZERO_LVL, EmptyDependentLink.getInstance(), ConCall(ZERO_LVL)),
                    clause(SUC_LVL, sucLvlParameter, Apps(ConCall(SUC_LVL), Reference(sucLvlParameter))))),
            clause(SUC_LVL, sucLvlParameter, branch(maxLvlParameter2, tail(),
                    clause(ZERO_LVL, EmptyDependentLink.getInstance(), Apps(ConCall(SUC_LVL), Reference(sucLvlParameter))),
                    clause(SUC_LVL, sucLvlParameterPrime, Apps(ConCall(SUC_LVL), Apps(FunCall(MAX_LVL), Reference(sucLvlParameter), Reference(sucLvlParameterPrime))))))));
    MAX_LVL.setElimTree(maxLvlElimTree);
    PRE_PRELUDE.addDefinition(MAX_LVL);

    CNAT = new DataDefinition(new DefinitionResolvedName(PRE_PRELUDE, "CNat"), Abstract.Definition.DEFAULT_PRECEDENCE, null, EmptyDependentLink.getInstance());
    PRE_PRELUDE.addDefinition(CNAT);

    INF = new Constructor(new DefinitionResolvedName(PRE_PRELUDE.getChild(CNAT.getName()), "inf"), Abstract.Definition.DEFAULT_PRECEDENCE, null, EmptyDependentLink.getInstance(), CNAT);
    DependentLink finParameter = param("n", DataCall(NAT));
    FIN = new Constructor(new DefinitionResolvedName(PRE_PRELUDE.getChild(CNAT.getName()), "fin"), Abstract.Definition.DEFAULT_PRECEDENCE, null, finParameter, CNAT);
    PRE_PRELUDE.addMember(CNAT.addConstructor(FIN));
    PRE_PRELUDE.addMember(CNAT.addConstructor(INF));

    DependentLink maxNatParameter1 = param(DataCall(NAT));
    DependentLink maxNatParameter2 = param(DataCall(NAT));
    maxNatParameter1.setNext(maxNatParameter2);
    DependentLink sucNatParameter = param("n", DataCall(NAT));
    DependentLink sucNatParameterPrime = param("n'", DataCall(NAT));
    MAX_NAT = new FunctionDefinition(new DefinitionResolvedName(PRE_PRELUDE, "maxNat"), Abstract.Definition.DEFAULT_PRECEDENCE, maxNatParameter1, DataCall(NAT), null, null);
    ElimTreeNode maxNatElimTree = top(maxNatParameter1, branch(maxNatParameter1, tail(maxNatParameter2),
            clause(ZERO, EmptyDependentLink.getInstance(), branch(maxNatParameter2, tail(),
                    clause(ZERO, EmptyDependentLink.getInstance(), ConCall(ZERO)),
                    clause(SUC, sucNatParameter, Apps(ConCall(SUC), Reference(sucNatParameter))))),
            clause(SUC, sucNatParameter, branch(maxNatParameter2, tail(),
                    clause(ZERO, EmptyDependentLink.getInstance(), Apps(ConCall(SUC), Reference(sucNatParameter))),
                    clause(SUC, sucNatParameterPrime, Apps(ConCall(SUC), Apps(FunCall(MAX_NAT), Reference(sucNatParameter), Reference(sucNatParameterPrime))))))));
    MAX_NAT.setElimTree(maxNatElimTree);
    PRE_PRELUDE.addDefinition(MAX_NAT);

    DependentLink maxCNatParameter1 = param(DataCall(CNAT));
    DependentLink maxCNatParameter2 = param(DataCall(CNAT));
    maxCNatParameter1.setNext(maxCNatParameter2);
    DependentLink finCNatParameter = param("n", DataCall(NAT));
    DependentLink finCNatParameterPrime = param("n'", DataCall(NAT));
    MAX_CNAT = new FunctionDefinition(new DefinitionResolvedName(PRE_PRELUDE, "maxCNat"), Abstract.Definition.DEFAULT_PRECEDENCE, maxCNatParameter1, DataCall(CNAT), null, null);
    ElimTreeNode maxCNatElimTree = top(maxCNatParameter1, branch(maxCNatParameter1, tail(maxCNatParameter2),
            clause(INF, EmptyDependentLink.getInstance(), ConCall(INF)),
            clause(FIN, finCNatParameter, branch(maxCNatParameter2, tail(),
                    clause(INF, EmptyDependentLink.getInstance(), ConCall(INF)),
                   // clause(FIN, finCNatParameterPrime, Apps(ConCall(FIN), Apps(FunCall(MAX_NAT), Reference(finCNatParameter), Reference(finCNatParameterPrime)))),
                    clause(FIN, finCNatParameterPrime, Apps(ConCall(FIN), Apps(FunCall(MAX_NAT), Reference(finCNatParameter), Reference(finCNatParameterPrime))))))));
    MAX_CNAT.setElimTree(maxCNatElimTree);
    PRE_PRELUDE.addDefinition(MAX_CNAT);

    DependentLink sucCNatParameter = param(DataCall(CNAT));
    ElimTreeNode sucCNatElimTree = top(sucCNatParameter, branch(sucCNatParameter, tail(),
            clause(INF, EmptyDependentLink.getInstance(), ConCall(INF)),
            clause(FIN, finCNatParameter, Apps(ConCall(FIN), Apps(ConCall(SUC), Reference(finCNatParameter))))));
    SUC_CNAT = new FunctionDefinition(new DefinitionResolvedName(PRE_PRELUDE, "sucCNat"), Abstract.Definition.DEFAULT_PRECEDENCE, sucCNatParameter, DataCall(CNAT), sucCNatElimTree, null);
    PRE_PRELUDE.addDefinition(SUC_CNAT);

    LEVEL = new ClassDefinition(new DefinitionResolvedName(PRE_PRELUDE, "Level"), null);
    PLEVEL = new ClassField(new DefinitionResolvedName(PRE_PRELUDE.getChild(LEVEL.getName()), "PLevel"), Abstract.Definition.DEFAULT_PRECEDENCE, DataCall(LVL), LEVEL, param("\\this", ClassCall(LEVEL)), null);
    HLEVEL = new ClassField(new DefinitionResolvedName(PRE_PRELUDE.getChild(LEVEL.getName()), "HLevel"), Abstract.Definition.DEFAULT_PRECEDENCE, DataCall(CNAT), LEVEL, param("\\this", ClassCall(LEVEL)), null);
    LEVEL.addField(PLEVEL);
    LEVEL.addField(HLEVEL);
    PRE_PRELUDE.addDefinition(LEVEL);
    PRE_PRELUDE.getChild(LEVEL.getName()).addDefinition(PLEVEL);
    PRE_PRELUDE.getChild(LEVEL.getName()).addDefinition(HLEVEL);

    DependentLink sucLevelParameter = param(ClassCall(LEVEL));
    SUC_LEVEL = new FunctionDefinition(new DefinitionResolvedName(PRE_PRELUDE, "sucLev"), Abstract.Definition.DEFAULT_PRECEDENCE, sucLevelParameter, ClassCall(LEVEL), EmptyElimTreeNode.getInstance(), null);
    PRE_PRELUDE.addDefinition(SUC_LEVEL);

    DependentLink maxLevelParameter1 = param(ClassCall(LEVEL));
    DependentLink maxLevelParameter2 = param(ClassCall(LEVEL));
    maxLevelParameter1.setNext(maxLevelParameter2);
    MAX_LEVEL = new FunctionDefinition(new DefinitionResolvedName(PRE_PRELUDE, "maxLev"), Abstract.Definition.DEFAULT_PRECEDENCE, maxLevelParameter1, ClassCall(LEVEL), EmptyElimTreeNode.getInstance(), null);
    PRE_PRELUDE.addDefinition(MAX_LEVEL);

    INTERVAL = new DataDefinition(new DefinitionResolvedName(PRE_PRELUDE, "I"), Abstract.Definition.DEFAULT_PRECEDENCE, null, EmptyDependentLink.getInstance());
    Namespace intervalNamespace = PRE_PRELUDE.getChild(INTERVAL.getName());
    LEFT = new Constructor(new DefinitionResolvedName(intervalNamespace, "left"), Abstract.Definition.DEFAULT_PRECEDENCE, null, EmptyDependentLink.getInstance(), INTERVAL);
    RIGHT = new Constructor(new DefinitionResolvedName(intervalNamespace, "right"), Abstract.Definition.DEFAULT_PRECEDENCE, null, EmptyDependentLink.getInstance(), INTERVAL);
    ABSTRACT = new Constructor(new DefinitionResolvedName(intervalNamespace, "<abstract>"), Abstract.Definition.DEFAULT_PRECEDENCE, null, EmptyDependentLink.getInstance(), INTERVAL);

    PRE_PRELUDE.addDefinition(INTERVAL);
    PRE_PRELUDE.addMember(INTERVAL.addConstructor(LEFT));
    PRE_PRELUDE.addMember(INTERVAL.addConstructor(RIGHT));
    INTERVAL.addConstructor(ABSTRACT);
  }

  public static void setUniverses() {
    NAT.setUniverse(TypeUniverse.SetOfLevel(0));
    ZERO.setUniverse(TypeUniverse.SetOfLevel(0));
    SUC.setUniverse(TypeUniverse.SetOfLevel(0));
    LVL.setUniverse(TypeUniverse.SetOfLevel(0));
    ZERO_LVL.setUniverse(TypeUniverse.SetOfLevel(0));
    SUC_LVL.setUniverse(TypeUniverse.SetOfLevel(0));
    CNAT.setUniverse(TypeUniverse.SetOfLevel(0));
    FIN.setUniverse(TypeUniverse.SetOfLevel(0));
    INF.setUniverse(TypeUniverse.SetOfLevel(0));
    LEVEL.setUniverse(TypeUniverse.SetOfLevel(0));
    PLEVEL.setUniverse(TypeUniverse.SetOfLevel(0));
    HLEVEL.setUniverse(TypeUniverse.SetOfLevel(0));
    MAX_LEVEL.setUniverse(TypeUniverse.SetOfLevel(0));
    SUC_LEVEL.setUniverse(TypeUniverse.SetOfLevel(0));
    INTERVAL.setUniverse(TypeUniverse.PROP);
    LEFT.setUniverse(TypeUniverse.PROP);
    RIGHT.setUniverse(TypeUniverse.PROP);
    ABSTRACT.setUniverse(TypeUniverse.PROP);
  }

  public Preprelude() {
    super(moduleID);
  }

  private static FunctionDefinition addLift(int lift) {
    DependentLink liftParameter1 = param(false, "l", ClassCall(LEVEL));
    DependentLink liftParameter2 = param("A", Universe(Reference(liftParameter1)));
    liftParameter1.setNext(liftParameter2);
    FunctionDefinition liftDef = new FunctionDefinition(new DefinitionResolvedName(PRE_PRELUDE, "lift" + lift), Abstract.Definition.DEFAULT_PRECEDENCE, liftParameter1, Universe(applyNumberOfSuc(Reference(liftParameter1), SUC_LEVEL, lift)), EmptyElimTreeNode.getInstance(), null);
    PRE_PRELUDE.addDefinition(liftDef);
    LIFTS.put(lift, liftDef);
    return liftDef;
  }

  @Override
  public NamespaceMember getMember(String name) {
    NamespaceMember result = super.getMember(name);
    if (result != null)
      return result;
    if (name.startsWith("lift")) {
      try {
        int lift = Integer.parseInt(name.substring("lift".length(), name.length()));
        return addLift(lift).getResolvedName().toNamespaceMember();
      } catch (Exception e) {
        return null;
      }
    }
    /*
    int indexOfMinusSign = name.indexOf('-');
    if (indexOfMinusSign == -1) {
      return null;
    }
    if (name.substring(indexOfMinusSign + 1, indexOfMinusSign + 1 + "lift".length()).equals("lift")) {
      try {
        int homLift = Integer.parseInt(name.substring(0, indexOfMinusSign));
        int predLift = Integer.parseInt(name.substring(indexOfMinusSign + "-lift".length(), name.length()));

        DependentLink liftParameter1 = param("l", ClassCall(LEVEL));
        DependentLink liftParameter2 = param("A", Universe(Reference(liftParameter1)));
        liftParameter1.setNext(liftParameter2);
        SUC_LEVEL = new FunctionDefinition(new DefinitionResolvedName(PRE_PRELUDE, "sucLev"), Abstract.Definition.DEFAULT_PRECEDENCE, liftParameter1, ClassCall(LEVEL), null, null);

        return getMember(name);
      } catch (Exception e) {
        return null;
      }
    } /**/
    return null;
  }

  @Override
  public Collection<NamespaceMember> getMembers() {
    throw new IllegalStateException();
  }

  public static boolean isIntervalOrLevel(Expression type) {
    DataCallExpression mbInterval = type.toDataCall();
    if (mbInterval != null) {
      return mbInterval.getDefinition() == INTERVAL;
    }
    ClassCallExpression mbLevel = type.toClassCall();
    return mbLevel != null && mbLevel.getDefinition() == LEVEL;
  }

  public static boolean isLift(FunctionDefinition fun) {
    return fun.getResolvedName().getName().startsWith("lift");
  }

  public static int getLiftNum(FunctionDefinition fun) {
    String name = fun.getResolvedName().getName();
    return Integer.parseInt(name.substring("lift".length(), name.length()));
  }

  public static Expression applyNumberOfSuc(Expression expr, FunctionDefinition suc, int num) {
    if (num <= 0) {
      return expr;
    }
    return FunCall(suc).addArgument(applyNumberOfSuc(expr, suc, num - 1), EnumSet.noneOf(AppExpression.Flag.class));
  }

  public static Expression applyNumberOfSuc(Expression expr, Constructor suc, int num) {
    if (num <= 0) {
      return expr;
    }
    return ConCall(suc).addArgument(applyNumberOfSuc(expr, suc, num - 1), EnumSet.noneOf(AppExpression.Flag.class));
  }

  public static class SucExtrResult {
    public int NumSuc;
    public Expression Arg;

    public SucExtrResult(int numSuc, Expression arg) {
      NumSuc = numSuc;
      Arg = arg;
    }

    public SucExtrResult incr() {
      return new SucExtrResult(NumSuc + 1, Arg);
    }
  }

  public static SucExtrResult extractSuc(Expression expr, Constructor suc) {
    Expression fun = expr.getFunction();
    if (fun.toConCall() != null && fun.toConCall().getDefinition() == suc &&
            expr.getArguments().size() == 1) {
      return extractSuc(expr.getArguments().get(0), suc).incr();
    }
    return new SucExtrResult(0, expr);
  }

  public static SucExtrResult extractSuc(Expression expr, FunctionDefinition suc) {
    Expression fun = expr.getFunction();
    if (fun.toFunCall() != null && fun.toFunCall().getDefinition() == suc &&
            expr.getArguments().size() == 1) {
      return extractSuc(expr.getArguments().get(0), suc).incr();
    }
    return new SucExtrResult(0, expr);
  }

  public static FunctionDefinition getLift(int lift) {
    if (!LIFTS.containsKey(lift)) {
      return addLift(lift);
    }
    return LIFTS.get(lift);
  }
}
