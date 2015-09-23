package com.jetbrains.jetpad.vclang.term.definition.visitor;

import com.jetbrains.jetpad.vclang.module.DefinitionPair;
import com.jetbrains.jetpad.vclang.module.Namespace;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.definition.*;
import com.jetbrains.jetpad.vclang.term.expr.Expression;
import com.jetbrains.jetpad.vclang.term.expr.PiExpression;
import com.jetbrains.jetpad.vclang.term.expr.UniverseExpression;
import com.jetbrains.jetpad.vclang.term.expr.arg.Argument;
import com.jetbrains.jetpad.vclang.term.expr.arg.TelescopeArgument;
import com.jetbrains.jetpad.vclang.term.expr.arg.TypeArgument;
import com.jetbrains.jetpad.vclang.term.expr.arg.Utils;
import com.jetbrains.jetpad.vclang.term.expr.visitor.CheckTypeVisitor;
import com.jetbrains.jetpad.vclang.term.expr.visitor.FindDefCallVisitor;
import com.jetbrains.jetpad.vclang.term.expr.visitor.NormalizeVisitor;
import com.jetbrains.jetpad.vclang.term.expr.visitor.TerminationCheckVisitor;
import com.jetbrains.jetpad.vclang.term.pattern.Pattern;
import com.jetbrains.jetpad.vclang.typechecking.error.ArgInferenceError;
import com.jetbrains.jetpad.vclang.typechecking.error.TypeCheckingError;
import com.jetbrains.jetpad.vclang.typechecking.error.TypeMismatchError;
import com.jetbrains.jetpad.vclang.typechecking.error.reporter.ErrorReporter;

import java.util.ArrayList;
import java.util.List;

import static com.jetbrains.jetpad.vclang.term.expr.ExpressionFactory.*;
import static com.jetbrains.jetpad.vclang.term.pattern.Utils.processImplicit;
import static com.jetbrains.jetpad.vclang.typechecking.error.ArgInferenceError.suffix;
import static com.jetbrains.jetpad.vclang.typechecking.error.ArgInferenceError.typeOfFunctionArg;
import static com.jetbrains.jetpad.vclang.typechecking.error.TypeCheckingError.getNames;

public class DefinitionCheckTypeVisitor implements AbstractDefinitionVisitor<Namespace, Definition> {
  private final List<Binding> myContext;
  private Namespace myNamespace;
  private final ErrorReporter myErrorReporter;

  public DefinitionCheckTypeVisitor(Namespace namespace, ErrorReporter errorReporter) {
    myContext = new ArrayList<>();
    myNamespace = namespace;
    myErrorReporter = errorReporter;
  }

  public DefinitionCheckTypeVisitor(List<Binding> context, Namespace namespace, ErrorReporter errorReporter) {
    myContext = context;
    myNamespace = namespace;
    myErrorReporter = errorReporter;
  }

  public void typeCheck(DefinitionPair definitionPair) {
    if (definitionPair.definition == null) {
      definitionPair.definition = definitionPair.abstractDefinition.accept(this, null);
    }
  }

  @Override
  public FunctionDefinition visitFunction(Abstract.FunctionDefinition def, Namespace localNamespace) {
    FunctionDefinition typedDef = new FunctionDefinition(myNamespace.getChild(def.getName()), localNamespace, def.getPrecedence(), def.getArrow());
    /*
    if (overriddenFunction == null && def.isOverridden()) {
      // TODO
      // myModuleLoader.getTypeCheckingErrors().add(new TypeCheckingError("Cannot find function " + def.getName() + " in the parent class", def, getNames(myContext)));
      myErrorReporter.report(new TypeCheckingError("Overridden function " + def.getName() + " cannot be defined in a base class", def, getNames(myContext)));
      return null;
    }
    */

    List<Argument> arguments = new ArrayList<>(def.getArguments().size());
    CheckTypeVisitor visitor = new CheckTypeVisitor(myContext, myErrorReporter, CheckTypeVisitor.Side.RHS);

    /*
    List<TypeArgument> splitArgs = null;
    Expression splitResult = null;
    if (overriddenFunction != null) {
      splitArgs = new ArrayList<>();
      splitResult = splitArguments(overriddenFunction.getType(), splitArgs);
    }

    int index = 0;
    if (splitArgs != null) {
      for (Abstract.Argument argument : def.getArguments()) {
        if (index >= splitArgs.size()) {
          index = -1;
          break;
        }

        boolean ok = true;
        if (argument instanceof Abstract.TelescopeArgument) {
          for (String ignored : ((Abstract.TelescopeArgument) argument).getNames()) {
            if (splitArgs.get(index).getExplicit() != argument.getExplicit()) {
              ok = false;
              break;
            }
            ++index;
          }
        } else {
          if (splitArgs.get(index).getExplicit() != argument.getExplicit()) {
            ok = false;
          } else {
            ++index;
          }
        }

        if (!ok) {
          myErrorReporter.report(new TypeCheckingError("Type of the argument does not match the type in the overridden function", argument, null));
          return null;
        }
      }

      if (index == -1) {
        myErrorReporter.report(new TypeCheckingError("Function has more arguments than overridden function", def, null));
        return null;
      }
    }
    */

    // int numberOfArgs = index;
    int index = 0;
    try (Utils.ContextSaver ignored = new Utils.ContextSaver(myContext)) {
      for (Abstract.Argument argument : def.getArguments()) {
        if (argument instanceof Abstract.TypeArgument) {
          CheckTypeVisitor.OKResult result = visitor.checkType(((Abstract.TypeArgument) argument).getType(), Universe());
          if (result == null) return typedDef;

          // boolean ok = true;
          if (argument instanceof Abstract.TelescopeArgument) {
            List<String> names = ((Abstract.TelescopeArgument) argument).getNames();
            arguments.add(Tele(argument.getExplicit(), names, result.expression));
            for (int i = 0; i < names.size(); ++i) {
            /*
            if (splitArgs != null) {
              List<CompareVisitor.Equation> equations = new ArrayList<>(0);
              CompareVisitor.Result cmpResult = compare(splitArgs.get(index).getType(), result.expression, equations);
              if (!(cmpResult instanceof CompareVisitor.JustResult && equations.isEmpty() && (cmpResult.isOK() == CompareVisitor.CMP.EQUIV || cmpResult.isOK() == CompareVisitor.CMP.EQUALS || cmpResult.isOK() == CompareVisitor.CMP.LESS))) {
                ok = false;
                break;
              }
            }
            */

              myContext.add(new TypedBinding(names.get(i), result.expression.liftIndex(0, i)));
              ++index;
            }
          } else {
          /*
          if (splitArgs != null) {
            List<CompareVisitor.Equation> equations = new ArrayList<>(0);
            CompareVisitor.Result cmpResult = compare(splitArgs.get(index).getType(), result.expression, equations);
            if (!(cmpResult instanceof CompareVisitor.JustResult && equations.isEmpty() && (cmpResult.isOK() == CompareVisitor.CMP.EQUIV || cmpResult.isOK() == CompareVisitor.CMP.EQUALS || cmpResult.isOK() == CompareVisitor.CMP.LESS))) {
              ok = false;
            }
          }
          */

            // if (ok) {
            arguments.add(TypeArg(argument.getExplicit(), result.expression));
            myContext.add(new TypedBinding((Utils.Name) null, result.expression));
            ++index;
            // }
          }

        /*
        if (!ok) {
          myErrorReporter.report(new ArgInferenceError(typedDef.getNamespace().getParent(), typeOfFunctionArg(index + 1), argument, null, new ArgInferenceError.StringPrettyPrintable(def.getName())));
          return null;
        }
        */
        } else {
          // if (splitArgs == null) {
          myErrorReporter.report(new ArgInferenceError(typedDef.getNamespace().getParent(), typeOfFunctionArg(index + 1), argument, null, new ArgInferenceError.StringPrettyPrintable(def.getName())));
          return typedDef;
        /*
        } else {
          List<String> names = new ArrayList<>(1);
          names.add(((Abstract.NameArgument) argument).getName());
          arguments.add(Tele(argument.getExplicit(), names, splitArgs.get(index).getType()));
          myContext.add(new TypedBinding(names.get(0), splitArgs.get(index).getType()));
        }
        */
        }
      }

    /*
    Expression overriddenResultType = null;
    if (overriddenFunction != null) {
      if (numberOfArgs == splitArgs.size()) {
        overriddenResultType = splitResult;
      } else {
        List<TypeArgument> args = new ArrayList<>(splitArgs.size() - numberOfArgs);
        for (; numberOfArgs < splitArgs.size(); ++numberOfArgs) {
          args.add(splitArgs.get(numberOfArgs));
        }
        overriddenResultType = Pi(args, splitResult);
      }
    }
    */

      Expression expectedType = null;
      if (def.getResultType() != null) {
        CheckTypeVisitor.OKResult typeResult = visitor.checkType(def.getResultType(), Universe());
        if (typeResult != null) {
          expectedType = typeResult.expression;
        /*
        if (overriddenResultType != null) {
          List<CompareVisitor.Equation> equations = new ArrayList<>(0);
          CompareVisitor.Result cmpResult = compare(expectedType, overriddenResultType, equations);
          if (!(cmpResult instanceof CompareVisitor.JustResult && equations.isEmpty() && (cmpResult.isOK() == CompareVisitor.CMP.EQUIV || cmpResult.isOK() == CompareVisitor.CMP.EQUALS || cmpResult.isOK() == CompareVisitor.CMP.LESS))) {
            myErrorReporter.report(new TypeCheckingError("Result type of the function does not match the result type in the overridden function", def.getResultType(), null));
            return null;
          }
        }
        */
        }
      }

    /*
    if (expectedType == null) {
      expectedType = overriddenResultType;
    }
    */

      typedDef.setArguments(arguments);
      typedDef.setResultType(expectedType);

      if (def.getTerm() != null) {
        visitor.setSide(CheckTypeVisitor.Side.LHS);
        CheckTypeVisitor.OKResult termResult = visitor.checkType(def.getTerm(), expectedType);

        if (termResult != null) {
          typedDef.setTerm(termResult.expression);
          if (expectedType == null) {
            typedDef.setResultType(termResult.type);
          }

          if (!termResult.expression.accept(new TerminationCheckVisitor(/* overriddenFunction == null ? */ typedDef /* : overriddenFunction */))) {
            myErrorReporter.report(new TypeCheckingError("Termination check failed", def.getTerm(), getNames(myContext)));
            termResult = null;
          }
        }

        if (termResult == null) {
          typedDef.setTerm(null);
        }
      } /* TODO
      else {
      if (definition.getParent() == namespace) {
        myErrorReporter.report(new GeneralError("Static abstract definition"));
        return false;
      }
    } */

      if (typedDef.getTerm() != null || typedDef.isAbstract()) {
        typedDef.hasErrors(false);
      }

      typedDef.typeHasErrors(typedDef.getResultType() == null);
      if (typedDef.typeHasErrors()) {
        typedDef.hasErrors(true);
      }
      Expression type = typedDef.getType();
      if (type != null) {
        type = type.getType(new ArrayList<Binding>(2));
        if (type instanceof UniverseExpression) {
          typedDef.setUniverse(((UniverseExpression) type).getUniverse());
        } else {
          throw new IllegalStateException();
        }
      }

      for (Argument argument : typedDef.getArguments()) {
        if (argument instanceof TelescopeArgument) {
          for (String ignore : ((TelescopeArgument) argument).getNames()) {
            myContext.remove(myContext.size() - 1);
          }
        } else {
          myContext.remove(myContext.size() - 1);
        }
      }
    }
    /*
    if (typedDef instanceof OverriddenDefinition) {
      ((OverriddenDefinition) typedDef).setOverriddenFunction(overriddenFunction);
    }
    */

    return typedDef;
  }

  @Override
  public DataDefinition visitData(Abstract.DataDefinition def, Namespace ignore) {
    List<TypeArgument> parameters = new ArrayList<>(def.getParameters().size());
    DataDefinition dataDefinition;
    Universe universe = new Universe.Type(0, Universe.Type.PROP);
    try (Utils.ContextSaver ignored = new Utils.ContextSaver(myContext)) {
      CheckTypeVisitor visitor = new CheckTypeVisitor(myContext, myErrorReporter, CheckTypeVisitor.Side.RHS);
      for (Abstract.TypeArgument parameter : def.getParameters()) {
        CheckTypeVisitor.OKResult result = visitor.checkType(parameter.getType(), Universe());
        if (result == null) return null;
        if (parameter instanceof Abstract.TelescopeArgument) {
          parameters.add(Tele(parameter.getExplicit(), ((Abstract.TelescopeArgument) parameter).getNames(), result.expression));
          List<String> names = ((Abstract.TelescopeArgument) parameter).getNames();
          for (int i = 0; i < names.size(); ++i) {
            myContext.add(new TypedBinding(names.get(i), result.expression.liftIndex(0, i)));
          }
        } else {
          parameters.add(TypeArg(parameter.getExplicit(), result.expression));
          myContext.add(new TypedBinding((Utils.Name) null, result.expression));
        }
      }

      dataDefinition = new DataDefinition(myNamespace.getChild(def.getName()), def.getPrecedence(), def.getUniverse() != null ? def.getUniverse() : new Universe.Type(0, Universe.Type.PROP), parameters);

      myNamespace = dataDefinition.getNamespace();
      List<? extends Abstract.Constructor> constructors = def.getConstructors();
      for (int i = 0; i < constructors.size(); ++i) {
        Abstract.Constructor constructor = constructors.get(i);
        DefinitionPair member = myNamespace.getMember(constructor.getName().name);
        if (member == null) {
          member = new DefinitionPair(myNamespace.getChild(constructor.getName()), constructor, null);
          myNamespace.addMember(member);
        }
        member.definition = visitConstructor(constructor, dataDefinition, i);
        if (member.definition == null) {
          // TODO
          continue;
        }

        Universe maxUniverse = universe.max(member.definition.getUniverse());
        if (maxUniverse == null) {
          String msg = "Universe " + member.definition.getUniverse() + " of constructor " + constructor.getName() + " is not compatible with universe " + universe + " of previous constructors";
          myErrorReporter.report(new TypeCheckingError(msg, null, null));
        } else {
          universe = maxUniverse;
        }
      }
      myNamespace = myNamespace.getParent();
    }

    if (def.getUniverse() != null) {
      if (universe.lessOrEquals(def.getUniverse())) {
        dataDefinition.setUniverse(def.getUniverse());
      } else {
        myErrorReporter.report(new TypeMismatchError(new UniverseExpression(def.getUniverse()), new UniverseExpression(universe), null, new ArrayList<String>()));
        dataDefinition.setUniverse(universe);
      }
    }

    return dataDefinition;
  }

  @Override
  public Definition visitConstructor(Abstract.Constructor def, Namespace params) {
    throw new IllegalStateException();
  }

  public Constructor visitConstructor(Abstract.Constructor def, DataDefinition dataDefinition, int conIndex) {
    try (Utils.CompleteContextSaver ignored = new Utils.CompleteContextSaver<>(myContext)) {
      List<TypeArgument> arguments = new ArrayList<>(def.getArguments().size());
      Universe universe = new Universe.Type(0, Universe.Type.PROP);
      int index = 1;
      boolean ok = true;

      CheckTypeVisitor visitor = new CheckTypeVisitor(myContext, myErrorReporter, CheckTypeVisitor.Side.RHS);
      List<Pattern> patterns = null;
      if (def.getPatterns() != null) {
        patterns = new ArrayList<>();

        // Implicits are assumed to be checked during parse phase
        List<Abstract.Pattern> abstractPatterns = processImplicit(def.getPatterns(), dataDefinition.getParameters()).patterns;
        for (int i = 0; i < abstractPatterns.size(); i++) {
          CheckTypeVisitor.ExpandPatternResult result = visitor.expandPatternOn(abstractPatterns.get(i), abstractPatterns.size() - 1 - i, Index(abstractPatterns.size() - 1 - i));
          if (result == null)
            return null;
          patterns.add(result.pattern);
        }
      }

      for (Abstract.TypeArgument argument : def.getArguments()) {
        CheckTypeVisitor.OKResult result = visitor.checkType(argument.getType(), Universe());
        if (result == null) {
          return null;
        }

        Universe argUniverse = ((UniverseExpression) result.type).getUniverse();
        Universe maxUniverse = universe.max(argUniverse);
        if (maxUniverse == null) {
          String error = "Universe " + argUniverse + " of " + index + suffix(index) + " argument is not compatible with universe " + universe + " of previous arguments";
          myErrorReporter.report(new TypeCheckingError(dataDefinition.getNamespace().getParent(), error, def, new ArrayList<String>()));
          ok = false;
        } else {
          universe = maxUniverse;
        }

        if (argument instanceof Abstract.TelescopeArgument) {
          arguments.add(Tele(argument.getExplicit(), ((Abstract.TelescopeArgument) argument).getNames(), result.expression));
          List<String> names = ((Abstract.TelescopeArgument) argument).getNames();
          for (int i = 0; i < names.size(); ++i) {
            myContext.add(new TypedBinding(names.get(i), result.expression.liftIndex(0, i)));
          }
          index += ((Abstract.TelescopeArgument) argument).getNames().size();
        } else {
          arguments.add(TypeArg(argument.getExplicit(), result.expression));
          myContext.add(new TypedBinding((Utils.Name) null, result.expression));
          ++index;
        }
      }

      if (!ok) {
        return null;
      }

      // TODO: Do not create child namespace if the definition does not type check.
      Constructor constructor = new Constructor(conIndex, dataDefinition.getNamespace().getChild(def.getName()), def.getPrecedence(), universe, arguments, dataDefinition, patterns);
      for (int j = 0; j < constructor.getArguments().size(); ++j) {
        Expression type = constructor.getArguments().get(j).getType().normalize(NormalizeVisitor.Mode.WHNF);
        while (type instanceof PiExpression) {
          for (TypeArgument argument1 : ((PiExpression) type).getArguments()) {
            if (argument1.getType().accept(new FindDefCallVisitor(dataDefinition))) {
              String msg = "Non-positive recursive occurrence of data type " + dataDefinition.getName() + " in constructor " + constructor.getName();
              myErrorReporter.report(new TypeCheckingError(dataDefinition.getNamespace().getParent(), msg, def.getArguments().get(j).getType(), getNames(myContext)));
              return null;
            }
          }
          type = ((PiExpression) type).getCodomain().normalize(NormalizeVisitor.Mode.WHNF);
        }

        List<Expression> exprs = new ArrayList<>();
        type.getFunction(exprs);
        for (Expression expr : exprs) {
          if (expr.accept(new FindDefCallVisitor(dataDefinition))) {
            String msg = "Non-positive recursive occurrence of data type " + dataDefinition.getName() + " in constructor " + constructor.getName();
            myErrorReporter.report(new TypeCheckingError(dataDefinition.getNamespace().getParent(), msg, def.getArguments().get(j).getType(), getNames(myContext)));
            return null;
          }
        }
      }

      dataDefinition.addConstructor(constructor);
      dataDefinition.getNamespace().getParent().addDefinition(constructor);
      return constructor;
    }
  }

  @Override
  public ClassDefinition visitClass(Abstract.ClassDefinition def, Namespace localNamespace) {
    ClassDefinition typedDef = new ClassDefinition(myNamespace.getChild(def.getName()));
    typedDef.setLocalNamespace(localNamespace);
    for (Abstract.Statement statement : def.getStatements()) {
      if (statement instanceof Abstract.DefineStatement) {
        Namespace parentNamespace = ((Abstract.DefineStatement) statement).isStatic() ? typedDef.getNamespace() : localNamespace;
        typeCheck(parentNamespace.getMember(((Abstract.DefineStatement) statement).getDefinition().getName().name));
      }
    }
    return typedDef;
  }
}
