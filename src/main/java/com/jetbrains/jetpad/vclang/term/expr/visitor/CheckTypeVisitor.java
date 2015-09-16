package com.jetbrains.jetpad.vclang.term.expr.visitor;

import com.jetbrains.jetpad.vclang.module.Namespace;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.Concrete;
import com.jetbrains.jetpad.vclang.term.Prelude;
import com.jetbrains.jetpad.vclang.term.definition.*;
import com.jetbrains.jetpad.vclang.term.expr.*;
import com.jetbrains.jetpad.vclang.term.expr.arg.Argument;
import com.jetbrains.jetpad.vclang.term.expr.arg.TelescopeArgument;
import com.jetbrains.jetpad.vclang.term.expr.arg.TypeArgument;
import com.jetbrains.jetpad.vclang.term.pattern.ConstructorPattern;
import com.jetbrains.jetpad.vclang.term.pattern.NamePattern;
import com.jetbrains.jetpad.vclang.term.pattern.Pattern;
import com.jetbrains.jetpad.vclang.term.pattern.Utils;
import com.jetbrains.jetpad.vclang.typechecking.error.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.ListIterator;

import static com.jetbrains.jetpad.vclang.term.expr.Expression.compare;
import static com.jetbrains.jetpad.vclang.term.expr.ExpressionFactory.*;
import static com.jetbrains.jetpad.vclang.term.expr.ExpressionFactory.Error;
import static com.jetbrains.jetpad.vclang.term.expr.arg.Utils.*;
import static com.jetbrains.jetpad.vclang.term.expr.arg.Utils.Name;
import static com.jetbrains.jetpad.vclang.term.pattern.Utils.*;
import static com.jetbrains.jetpad.vclang.typechecking.error.ArgInferenceError.*;

public class CheckTypeVisitor implements AbstractExpressionVisitor<Expression, CheckTypeVisitor.Result> {
  private final Namespace myNamespace;
  private final List<Binding> myLocalContext;
  private final ErrorReporter myErrorReporter;
  private Side mySide;

  private static class Arg {
    boolean isExplicit;
    String name;
    Abstract.Expression expression;

    Arg(boolean isExplicit, String name, Abstract.Expression expression) {
      this.isExplicit = isExplicit;
      this.name = name;
      this.expression = expression;
    }
  }

  public static abstract class Result {
    public Expression expression;
    public List<CompareVisitor.Equation> equations;
  }

  public static class OKResult extends Result {
    public Expression type;

    public OKResult(Expression expression, Expression type, List<CompareVisitor.Equation> equations) {
      this.expression = expression;
      this.type = type;
      this.equations = equations;
    }
  }

  public static class LetClauseResult extends Result {
    LetClause letClause;

    public LetClauseResult(LetClause letClause, List<CompareVisitor.Equation> equations) {
      this.letClause = letClause;
      this.equations = equations;
    }
  }

  public static class InferErrorResult extends Result {
    public TypeCheckingError error;

    public InferErrorResult(InferHoleExpression hole, TypeCheckingError error, List<CompareVisitor.Equation> equations) {
      expression = hole;
      this.error = error;
      this.equations = equations;
    }
  }

  public enum Side { LHS, RHS }

  public CheckTypeVisitor(Namespace namespace, List<Binding> localContext, ErrorReporter errorReporter, Side side) {
    myNamespace = namespace;
    myLocalContext = localContext;
    myErrorReporter = errorReporter;
    mySide = side;
  }

  private Result checkResult(Expression expectedType, OKResult result, Abstract.Expression expression) {
    if (result == null) return null;
    if (expectedType == null) {
      expression.setWellTyped(result.expression);
      return result;
    }
    Expression actualNorm = result.type.normalize(NormalizeVisitor.Mode.NF, myLocalContext);
    Expression expectedNorm = expectedType.normalize(NormalizeVisitor.Mode.NF, myLocalContext);
    List<CompareVisitor.Equation> equations = new ArrayList<>();
    CompareVisitor.Result result1 = compare(expectedNorm, actualNorm, equations);
    if (result1 instanceof CompareVisitor.MaybeResult || result1.isOK() == CompareVisitor.CMP.GREATER || result1.isOK() == CompareVisitor.CMP.EQUALS) {
      if (result.equations != null) {
        result.equations.addAll(equations);
      } else {
        result.equations = equations;
      }
      expression.setWellTyped(result.expression);
      return result;
    } else {
      TypeCheckingError error = new TypeMismatchError(myNamespace, expectedNorm, actualNorm, expression, getNames(myLocalContext));
      expression.setWellTyped(Error(result.expression, error));
      myErrorReporter.report(error);
      return null;
    }
  }

  private Result checkResultImplicit(Expression expectedType, OKResult result, Abstract.Expression expression) {
    if (result == null) return null;
    if (expectedType == null) {
      expression.setWellTyped(result.expression);
      return result;
    }

    if (numberOfVariables(result.type) > numberOfVariables(expectedType)) {
      return typeCheckFunctionApps(expression, new ArrayList<Abstract.ArgumentExpression>(), expectedType, expression);
    } else {
      return checkResult(expectedType, result, expression);
    }
  }

  private Result typeCheck(Abstract.Expression expr, Expression expectedType) {
    if (expr == null) {
      return null;
    } else {
      return expr.accept(this, expectedType);
    }
  }

  private static class SolveEquationsResult {
    int index;
    TypeCheckingError error;

    public SolveEquationsResult(int index, TypeCheckingError error) {
      this.index = index;
      this.error = error;
    }
  }

  private SolveEquationsResult solveEquations(int size, Arg[] argsImp, Result[] resultArgs, List<CompareVisitor.Equation> equations, List<CompareVisitor.Equation> resultEquations, Abstract.Expression fun) {
    int found = size;
    for (CompareVisitor.Equation equation : equations) {
      for (int i = 0; i < size; ++i) {
        if (resultArgs[i] instanceof InferErrorResult && resultArgs[i].expression == equation.hole) {
          if (!(argsImp[i].expression instanceof Abstract.InferHoleExpression)) {
            if (argsImp[i].expression instanceof Expression) {
              Expression expr1 = ((Expression) argsImp[i].expression).normalize(NormalizeVisitor.Mode.NF, myLocalContext);
              List<CompareVisitor.Equation> equations1 = new ArrayList<>();
              CompareVisitor.CMP cmp = compare(expr1, equation.expression, equations1).isOK();
              if (cmp != CompareVisitor.CMP.NOT_EQUIV) {
                if (cmp == CompareVisitor.CMP.GREATER) {
                  argsImp[i] = new Arg(argsImp[i].isExplicit, null, equation.expression);
                }
              } else {
                List<Abstract.Expression> options = new ArrayList<>(2);
                options.add(argsImp[i].expression);
                options.add(equation.expression);
                for (int j = i + 1; j < size; ++j) {
                  if (resultArgs[j] instanceof InferErrorResult && resultArgs[j].expression == equation.hole) {
                    boolean was = false;
                    for (Abstract.Expression option : options) {
                      cmp = compare(option, equation.expression, equations1).isOK();
                      if (cmp != CompareVisitor.CMP.NOT_EQUIV) {
                        was = true;
                        break;
                      }
                    }
                    if (!was) {
                      options.add(equation.expression);
                    }
                  }
                }
                TypeCheckingError error = new InferredArgumentsMismatch(myNamespace, i + 1, options, fun, getNames(myLocalContext));
                myErrorReporter.report(error);
                return new SolveEquationsResult(-1, error);
              }
            } else {
              break;
            }
          }

          argsImp[i] = new Arg(argsImp[i].isExplicit, null, equation.expression);
          found = i < found ? i : found;
          break;
        }
      }
      resultEquations.add(equation);
    }
    return new SolveEquationsResult(found, null);
  }

  private boolean typeCheckArgs(Arg[] argsImp, Result[] resultArgs, List<TypeArgument> signature, List<CompareVisitor.Equation> resultEquations, int startIndex, int parametersNumber, Abstract.Expression fun) {
    for (int i = startIndex; i < resultArgs.length; ++i) {
      if (resultArgs[i] == null) {
        TypeCheckingError error = new ArgInferenceError(myNamespace, i < parametersNumber ? parameter(i + 1) : functionArg(i - parametersNumber + 1), fun, getNames(myLocalContext), fun);
        resultArgs[i] = new InferErrorResult(new InferHoleExpression(error), error, null);
      }
    }

    for (int i = startIndex; i < resultArgs.length; ++i) {
      if (resultArgs[i] instanceof OKResult || argsImp[i].expression instanceof Abstract.InferHoleExpression) continue;

      List<Expression> substExprs = new ArrayList<>(i);
      for (int j = i - 1; j >= 0; --j) {
        substExprs.add(resultArgs[j].expression);
      }
      Expression type = signature.get(i).getType().subst(substExprs, 0);

      Result result;
      /* TODO
      if (argsImp[i].expression instanceof Expression) {
        List<Expression> context = new ArrayList<>(myLocalContext.size());
        for (Binding binding : myLocalContext) {
          context.add(binding.getType());
        }
        Expression actualType = ((Expression) argsImp[i].expression).getType(context);
        result = checkResult(type, new OKResult((Expression) argsImp[i].expression, actualType, null), argsImp[i].expression);
      } else { */
        result = typeCheck(argsImp[i].expression, type);
      // }
      if (result == null) {
        for (int j = i + 1; j < resultArgs.length; ++j) {
          if (!(argsImp[j].expression instanceof Abstract.InferHoleExpression)) {
            typeCheck(argsImp[j].expression, null);
          }
        }
        return false;
      }
      if (result instanceof OKResult) {
        resultArgs[i] = result;
        argsImp[i].expression = result.expression;
      } else {
        if (resultArgs[i] != null && resultArgs[i].expression instanceof InferHoleExpression) {
          resultArgs[i] = new InferErrorResult((InferHoleExpression) resultArgs[i].expression, ((InferErrorResult) result).error, result.equations);
        } else {
          resultArgs[i] = result;
        }
      }

      if (resultArgs[i].equations == null) continue;
      SolveEquationsResult result1 = solveEquations(i, argsImp, resultArgs, resultArgs[i].equations, resultEquations, fun);
      if (result1.index < 0) return false;
      if (result1.index != i) {
        i = result1.index - 1;
      }
    }
    return true;
  }

  private Result typeCheckFunctionApps(Abstract.Expression fun, List<Abstract.ArgumentExpression> args, Expression expectedType, Abstract.Expression expression) {
    Result function;
    if (fun instanceof Abstract.DefCallExpression && ((Abstract.DefCallExpression) fun).getDefinitionPair().definition instanceof Constructor && !((Constructor) ((Abstract.DefCallExpression) fun).getDefinitionPair().definition).getDataType().getParameters().isEmpty()) {
      function = typeCheckDefCall((Abstract.DefCallExpression) fun, null);
      if (function instanceof OKResult) {
        return typeCheckApps(fun, 0, (OKResult) function, args, expectedType, expression);
      }
    } else {
      function = typeCheck(fun, null);
      if (function instanceof OKResult) {
        return typeCheckApps(fun, 0, (OKResult) function, args, expectedType, expression);
      }
    }

    if (function instanceof InferErrorResult) {
      myErrorReporter.report(((InferErrorResult) function).error);
    }
    for (Abstract.ArgumentExpression arg : args) {
      typeCheck(arg.getExpression(), null);
    }
    return null;
  }

  private Result typeCheckApps(Abstract.Expression fun, int argsSkipped, OKResult okFunction, List<Abstract.ArgumentExpression> args, Expression expectedType, Abstract.Expression expression) {
    List<TypeArgument> signatureArguments = new ArrayList<>();
    int parametersNumber = 0;
    if (okFunction.expression instanceof DefCallExpression) {
      Definition def = ((DefCallExpression) okFunction.expression).getDefinition();
      if (def instanceof Constructor) {
        if (((Constructor) def).getPatterns() == null) {
          parametersNumber = numberOfVariables(((Constructor) def).getDataType().getParameters());
        } else {
          parametersNumber = expandConstructorParameters((Constructor) def).size();
        }
      }
    }

    Expression signatureResultType = splitArguments(okFunction.type, signatureArguments);
    assert parametersNumber <= signatureArguments.size();
    Arg[] argsImp = new Arg[signatureArguments.size()];
    int i, j;
    for (i = 0; i < parametersNumber; ++i) {
      argsImp[i] = new Arg(false, null, new InferHoleExpression(new ArgInferenceError(myNamespace, parameter(i + 1), fun, getNames(myLocalContext), fun)));
    }
    for (j = 0; i < signatureArguments.size() && j < args.size(); ++i) {
      if (args.get(j).isExplicit() == signatureArguments.get(i).getExplicit()) {
        argsImp[i] = new Arg(args.get(j).isHidden(), null, args.get(j).getExpression());
        ++j;
      } else
      if (args.get(j).isExplicit()) {
        argsImp[i] = new Arg(true, null, new InferHoleExpression(new ArgInferenceError(myNamespace, functionArg(i - parametersNumber + 1), fun, getNames(myLocalContext), fun)));
      } else {
        TypeCheckingError error = new TypeCheckingError(myNamespace, "Unexpected implicit argument", args.get(j).getExpression(), getNames(myLocalContext));
        args.get(j).getExpression().setWellTyped(Error(null, error));
        myErrorReporter.report(error);
        for (Abstract.ArgumentExpression arg : args) {
          typeCheck(arg.getExpression(), null);
        }
        return null;
      }
    }

    if (j < args.size() && signatureArguments.isEmpty()) {
      TypeCheckingError error = new TypeCheckingError(myNamespace, "Function expects " + (argsSkipped + i) + " arguments, but is applied to " + (argsSkipped + i + args.size() - j), fun, getNames(myLocalContext));
      fun.setWellTyped(Error(okFunction.expression, error));
      myErrorReporter.report(error);
      for (Abstract.ArgumentExpression arg : args) {
        typeCheck(arg.getExpression(), null);
      }
      return null;
    }

    if (okFunction.expression instanceof DefCallExpression && ((DefCallExpression) okFunction.expression).getDefinition() == Prelude.PATH_CON && args.size() == 1 && j == 1) {
      Expression argExpectedType = null;
      InferHoleExpression holeExpression = null;
      if (expectedType != null) {
        List<Expression> argsExpectedType = new ArrayList<>(3);
        Expression fexpectedType = expectedType.normalize(NormalizeVisitor.Mode.WHNF, myLocalContext).getFunction(argsExpectedType);
        if (fexpectedType instanceof DefCallExpression && ((DefCallExpression) fexpectedType).getDefinition().equals(Prelude.PATH) && argsExpectedType.size() == 3) {
          if (argsExpectedType.get(2) instanceof InferHoleExpression) {
            holeExpression = (InferHoleExpression) argsExpectedType.get(2);
          } else {
            argExpectedType = Pi("i", DefCall(Prelude.INTERVAL), Apps(argsExpectedType.get(2).liftIndex(0, 1), Index(0)));
          }
        }
      }

      InferHoleExpression inferHoleExpr = null;
      if (argExpectedType == null) {
        inferHoleExpr = new InferHoleExpression(new ArgInferenceError(myNamespace, type(), args.get(0).getExpression(), getNames(myLocalContext), args.get(0).getExpression()));
        argExpectedType = Pi("i", DefCall(Prelude.INTERVAL), inferHoleExpr);
      }

      Result argResult = typeCheck(args.get(0).getExpression(), argExpectedType);
      if (!(argResult instanceof OKResult)) return argResult;
      if (argResult.equations != null) {
        for (int k = 0; k < argResult.equations.size(); ++k) {
          if (argResult.equations.get(k).hole.equals(inferHoleExpr)) {
            argResult.equations.remove(k--);
          }
        }
      }
      PiExpression piType = (PiExpression) ((OKResult) argResult).type;

      List<TypeArgument> arguments = new ArrayList<>(piType.getArguments().size());
      if (piType.getArguments().get(0) instanceof TelescopeArgument) {
        List<String> names = ((TelescopeArgument) piType.getArguments().get(0)).getNames();
        if (names.size() > 1) {
          arguments.add(Tele(piType.getArguments().get(0).getExplicit(), names.subList(1, names.size()), piType.getArguments().get(0).getType()));
        }
      }
      if (piType.getArguments().size() > 1) {
        arguments.addAll(piType.getArguments().subList(1, piType.getArguments().size()));
      }

      Expression type = arguments.size() > 0 ? Pi(arguments, piType.getCodomain()) : piType.getCodomain();
      Expression parameter1 = Lam(lamArgs(Tele(vars("i"), DefCall(Prelude.INTERVAL))), type);
      Expression parameter2 = Apps(argResult.expression, DefCall(Prelude.LEFT));
      Expression parameter3 = Apps(argResult.expression, DefCall(Prelude.RIGHT));
      Expression resultType = Apps(DefCall(Prelude.PATH), parameter1, parameter2, parameter3);
      List<CompareVisitor.Equation> resultEquations = argResult.equations;
      if (holeExpression != null) {
        if (resultEquations == null) {
          resultEquations = new ArrayList<>(1);
        }
        resultEquations.add(new CompareVisitor.Equation(holeExpression, Lam(lamArgs(Tele(vars("i"), DefCall(Prelude.INTERVAL))), type.normalize(NormalizeVisitor.Mode.NF, myLocalContext))));
      }

      List<Expression> parameters = new ArrayList<>(3);
      parameters.add(parameter1);
      parameters.add(parameter2);
      parameters.add(parameter3);
      Expression resultExpr = Apps(DefCall(null, Prelude.PATH_CON, parameters), new ArgumentExpression(argResult.expression, true, false));
      return checkResult(expectedType, new OKResult(resultExpr, resultType, resultEquations), expression);
    }

    if (expectedType != null && j == args.size()) {
      for (; i < signatureArguments.size() - numberOfVariables(expectedType); ++i) {
        if (signatureArguments.get(i).getExplicit()) {
          break;
        } else {
          argsImp[i] = new Arg(true, null, new InferHoleExpression(new ArgInferenceError(myNamespace, functionArg(i + 1), fun, getNames(myLocalContext), fun)));
        }
      }
    }

    int argsNumber = i;
    Result[] resultArgs = new Result[argsNumber];
    List<CompareVisitor.Equation> resultEquations = new ArrayList<>();
    if (!typeCheckArgs(argsImp, resultArgs, signatureArguments, resultEquations, 0, parametersNumber, fun)) {
      expression.setWellTyped(Error(null, null)); // TODO
      return null;
    }

    Expression resultType;
    if (signatureArguments.size() == argsNumber) {
      resultType = signatureResultType;
    } else {
      int size = signatureArguments.size() - argsNumber;
      List<TypeArgument> rest = new ArrayList<>(size);
      for (i = 0; i < size; ++i) {
        rest.add(signatureArguments.get(argsNumber + i));
      }
      resultType = Pi(rest, signatureResultType);
    }
    List<Expression> substExprs = new ArrayList<>(argsNumber);
    for (i = argsNumber - 1; i >= 0; --i) {
      substExprs.add(resultArgs[i].expression);
    }
    resultType = resultType.subst(substExprs, 0);

    int argIndex = 0;
    for (i = argsNumber - 1; i >= 0; --i) {
      if (!(resultArgs[i] instanceof OKResult)) {
        argIndex = i + 1;
        break;
      }
    }

    if (argIndex != 0 && expectedType != null && j == args.size() && expectedType.accept(new FindHoleVisitor()) == null) {
      Expression expectedNorm = expectedType.normalize(NormalizeVisitor.Mode.NF, myLocalContext);
      Expression actualNorm = resultType.normalize(NormalizeVisitor.Mode.NF, myLocalContext);
      List<CompareVisitor.Equation> equations = new ArrayList<>();
      CompareVisitor.Result result = compare(actualNorm, expectedNorm, equations);

      if (result instanceof CompareVisitor.JustResult && result.isOK() != CompareVisitor.CMP.LESS && result.isOK() != CompareVisitor.CMP.EQUALS) {
        Expression resultExpr = okFunction.expression;
        for (i = parametersNumber; i < argsNumber; ++i) {
          resultExpr = Apps(resultExpr, new ArgumentExpression(resultArgs[i].expression, signatureArguments.get(i).getExplicit(), argsImp[i].isExplicit));
        }

        TypeCheckingError error = new TypeMismatchError(myNamespace, expectedNorm, actualNorm, expression, getNames(myLocalContext));
        expression.setWellTyped(Error(resultExpr, error));
        myErrorReporter.report(error);
        return null;
      }

      SolveEquationsResult result1 = solveEquations(argsNumber, argsImp, resultArgs, equations, resultEquations, fun);
      if (result1.index < 0 || (result1.index != argsNumber && !typeCheckArgs(argsImp, resultArgs, signatureArguments, resultEquations, result1.index, parametersNumber, fun))) {
        Expression resultExpr = okFunction.expression;
        for (i = parametersNumber; i < argsNumber; ++i) {
          resultExpr = Apps(resultExpr, new ArgumentExpression(resultArgs[i] == null ? new InferHoleExpression(null) : resultArgs[i].expression, signatureArguments.get(i).getExplicit(), argsImp[i].isExplicit));
        }
        expression.setWellTyped(Error(resultExpr, result1.error));
        return null;
      }

      argIndex = 0;
      for (i = argsNumber - 1; i >= 0; --i) {
        if (!(resultArgs[i] instanceof OKResult)) {
          argIndex = i + 1;
          break;
        }
      }

      if (argIndex == 0) {
        if (signatureArguments.size() == argsNumber) {
          resultType = signatureResultType;
        } else {
          int size = signatureArguments.size() - argsNumber;
          List<TypeArgument> rest = new ArrayList<>(size);
          for (i = 0; i < size; ++i) {
            rest.add(signatureArguments.get(argsNumber + i));
          }
          resultType = Pi(rest, signatureResultType);
        }
        substExprs = new ArrayList<>(argsNumber);
        for (i = argsNumber - 1; i >= 0; --i) {
          substExprs.add(resultArgs[i].expression);
        }
        resultType = resultType.subst(substExprs, 0);
      }
    }

    Expression resultExpr = okFunction.expression;
    List<Expression> parameters = null;
    if (parametersNumber > 0) {
      parameters = new ArrayList<>(parametersNumber);
      ((DefCallExpression) resultExpr).setParameters(parameters);
    }
    for (i = 0; i < parametersNumber; ++i) {
      assert parameters != null;
      parameters.add(resultArgs[i].expression);
    }
    for (; i < argsNumber; ++i) {
      resultExpr = Apps(resultExpr, new ArgumentExpression(resultArgs[i].expression, signatureArguments.get(i).getExplicit(), argsImp[i].isExplicit));
    }

    if (argIndex == 0) {
      if (j < args.size()) {
        List<Abstract.ArgumentExpression> restArgs = new ArrayList<>(args.size() - j);
        for (int k = j; k < args.size(); ++k) {
          restArgs.add(args.get(k));
        }
        return typeCheckApps(fun, argsSkipped + j, new OKResult(resultExpr, resultType, resultEquations), restArgs, expectedType, expression);
      } else {
        return checkResult(expectedType, new OKResult(resultExpr, resultType, resultEquations), expression);
      }
    } else {
      TypeCheckingError error;
      if (resultArgs[argIndex - 1] instanceof InferErrorResult) {
        error = ((InferErrorResult) resultArgs[argIndex - 1]).error;
      } else {
        if (argIndex > parametersNumber) {
          error = new ArgInferenceError(myNamespace, functionArg(argIndex - parametersNumber), fun, getNames(myLocalContext), fun);
        } else {
          error = new ArgInferenceError(myNamespace, parameter(argIndex), fun, null, new StringPrettyPrintable(((Constructor) ((DefCallExpression) okFunction.expression).getDefinition()).getDataType().getName()));
        }
      }
      expression.setWellTyped(Error(resultExpr, error));
      return new InferErrorResult(new InferHoleExpression(error), error, resultEquations);
    }
  }

  public OKResult checkType(Abstract.Expression expr, Expression expectedType) {
    Result result = typeCheck(expr, expectedType);
    if (result == null) return null;
    if (result instanceof OKResult) return (OKResult) result;
    InferErrorResult errorResult = (InferErrorResult) result;
    myErrorReporter.report(errorResult.error);
    return null;
  }

  @Override
  public Result visitApp(Abstract.AppExpression expr, Expression expectedType) {
    List<Abstract.ArgumentExpression> args = new ArrayList<>();
    return typeCheckFunctionApps(Abstract.getFunction(expr, args), args, expectedType, expr);
  }

  private Result typeCheckDefCall(Abstract.DefCallExpression expr, Expression expectedType) {
    ClassDefinition parent = null;
    OKResult result = null;
    if (expr.getDefinitionPair() == null) {
      assert expr.getExpression() != null;

      Result exprResult = typeCheck(expr.getExpression(), null);
      if (!(exprResult instanceof OKResult)) return exprResult;
      OKResult okExprResult = (OKResult) exprResult;
      Expression type = okExprResult.type.normalize(NormalizeVisitor.Mode.WHNF);
      boolean notInScope = false;

      if (type instanceof ClassExtExpression || type instanceof DefCallExpression && ((DefCallExpression) type).getDefinition() instanceof ClassDefinition) {
        parent = type instanceof ClassExtExpression ? ((ClassExtExpression) type).getBaseClass() : (ClassDefinition) ((DefCallExpression) type).getDefinition();
        Definition child = parent.getField(expr.getName().name);
        if (child != null) {
          if (child.hasErrors()) {
            TypeCheckingError error = new HasErrors(myNamespace, child.getName(), expr);
            expr.setWellTyped(Error(DefCall(child), error));
            myErrorReporter.report(error);
            return null;
          } else {
            Definition resultDef = child;
            if (type instanceof ClassExtExpression && child instanceof FunctionDefinition) {
              OverriddenDefinition overridden = ((ClassExtExpression) type).getDefinitionsMap().get(child);
              if (overridden != null) {
                resultDef = overridden;
              }
            }
            Expression resultType = resultDef.getType();
            if (resultType == null) {
              resultType = child.getType();
            }
            result = new OKResult(DefCall(okExprResult.expression, resultDef), resultType, okExprResult.equations);
          }
        }
        notInScope = true;
      } else
      if (okExprResult.type instanceof UniverseExpression) {
        Expression expression = okExprResult.expression.normalize(NormalizeVisitor.Mode.WHNF);
        if (expression instanceof DefCallExpression && ((DefCallExpression) expression).getDefinition() instanceof ClassDefinition) {
          // TODO
          Definition field = null; // ((DefCallExpression) expression).getDefinition().getStaticField(expr.getName().name);
          if (field == null) {
            notInScope = true;
          } else {
            result = new OKResult(DefCall(field), field.getType(), okExprResult.equations);
          }
        } else {
          List<Expression> arguments = new ArrayList<>();
          Expression function = expression.getFunction(arguments);
          Collections.reverse(arguments);
          if (function instanceof DefCallExpression && ((DefCallExpression) function).getDefinition() instanceof DataDefinition) {
            Constructor constructor = ((DataDefinition) ((DefCallExpression) function).getDefinition()).getConstructor(expr.getName().name);
            if (constructor == null) {
              notInScope = true;
            } else {
              if (constructor.getPatterns() != null) {
                Utils.PatternMatchResult matchResult = patternMatchAll(constructor.getPatterns(), arguments, myLocalContext);
                TypeCheckingError error = null;
                if (matchResult instanceof PatternMatchMaybeResult) {
                  error = new TypeCheckingError(myNamespace, "Constructor is not appropriate, failed to match data type parameters. " +
                      "Expected " + ((PatternMatchMaybeResult) matchResult).maybePattern + ", got " + ((PatternMatchMaybeResult) matchResult).actualExpression, expr, getNames(myLocalContext));
                } else if (matchResult instanceof PatternMatchFailedResult) {
                  error = new TypeCheckingError(myNamespace, "Constructor is not appropriate, failed to match data type parameters. " +
                      "Expected " + ((PatternMatchFailedResult) matchResult).failedPattern + ", got " + ((PatternMatchFailedResult) matchResult).actualExpression, expr, getNames(myLocalContext));
                } else if (matchResult instanceof PatternMatchOKResult) {
                  arguments = ((PatternMatchOKResult) matchResult).expressions;
                }

                if (error != null) {
                  expr.setWellTyped(Error(null, error));
                  myErrorReporter.report(error);
                  return null;
                }
              }
              Collections.reverse(arguments);
              Expression resultType = constructor.getType().subst(arguments, 0);
              Collections.reverse(arguments);
              return checkResultImplicit(expectedType, new OKResult(DefCall(null, constructor, arguments), resultType, okExprResult.equations), expr);
            }
          }
        }
      }

      if (result == null) {
        TypeCheckingError error;
        if (notInScope) {
          error = new NotInScopeError(myNamespace, expr, expr.getName());
        } else {
          error = new TypeCheckingError(myNamespace, "Expected an expression of a class type or a data type", expr.getExpression(), getNames(myLocalContext));
        }
        expr.setWellTyped(Error(null, error));
        myErrorReporter.report(error);
        return null;
      }
    } else {
      if (expr.getDefinitionPair().definition instanceof FunctionDefinition && ((FunctionDefinition) expr.getDefinitionPair().definition).typeHasErrors() || !(expr.getDefinitionPair().definition instanceof FunctionDefinition) && expr.getDefinitionPair().definition.hasErrors()) {
        TypeCheckingError error = new HasErrors(myNamespace, expr.getName(), expr);
        expr.setWellTyped(Error(DefCall(expr.getDefinitionPair().definition), error));
        myErrorReporter.report(error);
        return null;
      }

      /* TODO
      if (expr.getDefinitionPair().isAbstract()) {
        myAbstractCalls.add(expr.getDefinitionPair());
      }
      */

      result = new OKResult(DefCall(expr.getDefinitionPair().definition), expr.getDefinitionPair().definition.getType(), null);
    }

    if (result.expression instanceof DefCallExpression) {
      if (((DefCallExpression) result.expression).getDefinition() instanceof Constructor) {
        Constructor constructor = ((Constructor) ((DefCallExpression) result.expression).getDefinition());
        List<TypeArgument> parameters;
        if (constructor.getPatterns() != null) {
          parameters = expandConstructorParameters(constructor);
        } else {
          parameters = constructor.getDataType().getParameters();
        }

        if (parameters != null && !parameters.isEmpty()) {
          result.type = Pi(parameters, result.type);
        }
      }
      if (((DefCallExpression) result.expression).getExpression() != null && parent != null) {
        result.type = result.type.accept(new ReplaceDefCallVisitor(parent.getLocalNamespace(), ((DefCallExpression) result.expression).getExpression()));
      }
    }
    return result;
  }

  @Override
  public Result visitDefCall(Abstract.DefCallExpression expr, Expression expectedType) {
    Result result = typeCheckDefCall(expr, expectedType);
    if (result instanceof OKResult && result.expression instanceof DefCallExpression) {
      DefCallExpression defCall = (DefCallExpression) result.expression;
      if (defCall.getDefinition() instanceof Constructor) {
        if (defCall.getParameters() != null) {
          return result;
        }
        if (!((Constructor) defCall.getDefinition()).getDataType().getParameters().isEmpty()) {
          return typeCheckApps(expr, 0, (OKResult) result, new ArrayList<Abstract.ArgumentExpression>(0), expectedType, expr);
        }
      }
    }
    return result instanceof OKResult ? checkResultImplicit(expectedType, (OKResult) result, expr) : result;
  }

  @Override
  public Result visitIndex(Abstract.IndexExpression expr, Expression expectedType) {
    OKResult result = getIndex(expr);
    result.type = result.type.liftIndex(0, ((IndexExpression) result.expression).getIndex() + 1);
    return checkResultImplicit(expectedType, result, expr);
  }

  private OKResult getIndex(Abstract.IndexExpression expr) {
    assert expr.getIndex() < myLocalContext.size();
    Expression actualType = myLocalContext.get(myLocalContext.size() - 1 - expr.getIndex()).getType();
    return new OKResult(Index(expr.getIndex()), actualType, null);
  }

  @Override
  public Result visitLam(Abstract.LamExpression expr, Expression expectedType) {
    List<CompareVisitor.Equation> resultEquations = new ArrayList<>();

    List<Arg> lambdaArgs = new ArrayList<>();
    for (Abstract.Argument arg : expr.getArguments()) {
      if (arg instanceof Abstract.NameArgument) {
        lambdaArgs.add(new Arg(arg.getExplicit(), ((Abstract.NameArgument) arg).getName(), null));
      } else {
        for (String name : ((Abstract.TelescopeArgument) arg).getNames()) {
          lambdaArgs.add(new Arg(arg.getExplicit(), name, ((Abstract.TelescopeArgument) arg).getType()));
        }
      }
    }

      List<TypeArgument> piArgs = new ArrayList<>();
      int actualNumberOfPiArgs;
      Expression resultType;
      Expression fresultType;
      if (expectedType == null) {
        for (Arg ignored : lambdaArgs) {
          piArgs.add(null);
        }
        actualNumberOfPiArgs = 0;
        resultType = null;
        fresultType = null;
      } else {
        resultType = expectedType.splitAt(lambdaArgs.size(), piArgs).normalize(NormalizeVisitor.Mode.WHNF, myLocalContext);
        fresultType = resultType.getFunction(new ArrayList<Expression>());
        actualNumberOfPiArgs = piArgs.size();
        if (fresultType instanceof InferHoleExpression) {
          for (int i = piArgs.size(); i < lambdaArgs.size(); ++i) {
            piArgs.add(null);
          }
        }
      }

    if (piArgs.size() < lambdaArgs.size()) {
      TypeCheckingError error = new TypeCheckingError(myNamespace, "Expected a function of " + piArgs.size() + " arguments, but the lambda has " + lambdaArgs.size(), expr, getNames(myLocalContext));
      expr.setWellTyped(Error(null, error));
      myErrorReporter.report(error);
      return null;
    }

    List<TypeCheckingError> errors = new ArrayList<>(lambdaArgs.size());
    for (int i = 0; i < lambdaArgs.size(); ++i) {
      if (piArgs.get(i) == null && lambdaArgs.get(i).expression == null) {
        TypeCheckingError error = new ArgInferenceError(myNamespace, lambdaArg(i + 1), expr, getNames(myLocalContext), expr);
        errors.add(error);
        if (fresultType instanceof InferHoleExpression) {
          expr.setWellTyped(Error(null, error));
          return new InferErrorResult((InferHoleExpression) fresultType, error, null);
        }
      } else
      if (piArgs.get(i) != null && lambdaArgs.get(i).expression == null) {
        InferHoleExpression hole = piArgs.get(i).getType().accept(new FindHoleVisitor());
        if (hole != null) {
          if (!errors.isEmpty()) {
            break;
          } else {
            TypeCheckingError error = new ArgInferenceError(myNamespace, lambdaArg(i + 1), expr, getNames(myLocalContext), expr);
            expr.setWellTyped(Error(null, error));
            return new InferErrorResult(hole, error, null);
          }
        }
      } else
      if (piArgs.get(i) != null && lambdaArgs.get(i).expression != null) {
        if (piArgs.get(i).getExplicit() != lambdaArgs.get(i).isExplicit) {
          errors.add(new TypeCheckingError(myNamespace, (i + 1) + suffix(i + 1) + " argument of the lambda should be " + (piArgs.get(i).getExplicit() ? "explicit" : "implicit"), expr, getNames(myLocalContext)));
        }
      }
    }
    if (!errors.isEmpty()) {
      expr.setWellTyped(Error(null, errors.get(0)));
      for (TypeCheckingError error : errors) {
        myErrorReporter.report(error);
      }
      return null;
    }

    List<TypeArgument> argumentTypes = new ArrayList<>(lambdaArgs.size());
    for (int i = 0; i < lambdaArgs.size(); ++i) {
      if (lambdaArgs.get(i).expression != null) {
        Result argResult = typeCheck(lambdaArgs.get(i).expression, Universe());
        if (!(argResult instanceof OKResult)) {
          while (i-- > 0) {
            myLocalContext.remove(myLocalContext.size() - 1);
          }
          return argResult;
        }
        OKResult okArgResult = (OKResult) argResult;
        addLiftedEquations(okArgResult, resultEquations, i);
        argumentTypes.add(Tele(lambdaArgs.get(i).isExplicit, vars(lambdaArgs.get(i).name), okArgResult.expression));

        if (piArgs.get(i) != null) {
          Expression argExpectedType = piArgs.get(i).getType().normalize(NormalizeVisitor.Mode.NF, myLocalContext);
          Expression argActualType = argumentTypes.get(i).getType().normalize(NormalizeVisitor.Mode.NF, myLocalContext);
          List<CompareVisitor.Equation> equations = new ArrayList<>();
          CompareVisitor.Result result = compare(argExpectedType, argActualType, equations);
          if (result.isOK() != CompareVisitor.CMP.LESS && result.isOK() != CompareVisitor.CMP.EQUALS) {
            errors.add(new TypeMismatchError(myNamespace, piArgs.get(i).getType(), lambdaArgs.get(i).expression, expr, getNames(myLocalContext)));
          } else {
            for (int j = 0; j < equations.size(); ++j) {
              Expression expression = equations.get(j).expression.liftIndex(0, -i);
              if (expression == null) {
                equations.remove(j--);
              } else {
                equations.set(j, new CompareVisitor.Equation(equations.get(j).hole, expression));
              }
            }
            resultEquations.addAll(equations);
          }
        }
      } else {
        argumentTypes.add(Tele(piArgs.get(i).getExplicit(), vars(lambdaArgs.get(i).name), piArgs.get(i).getType()));
      }
      myLocalContext.add(new TypedBinding(lambdaArgs.get(i).name, argumentTypes.get(i).getType()));
    }

    Result bodyResult = typeCheck(expr.getBody(), fresultType instanceof InferHoleExpression ? null : resultType);

    for (int i = 0; i < lambdaArgs.size(); ++i) {
      myLocalContext.remove(myLocalContext.size() - 1);
    }

    if (!(bodyResult instanceof OKResult)) return bodyResult;
    OKResult okBodyResult = (OKResult) bodyResult;
    addLiftedEquations(okBodyResult, resultEquations, lambdaArgs.size());

      if (resultType instanceof InferHoleExpression) {
        Expression actualType = okBodyResult.type;
        if (lambdaArgs.size() > actualNumberOfPiArgs) {
          actualType = Pi(argumentTypes.subList(actualNumberOfPiArgs, lambdaArgs.size()), actualType);
        }
        Expression expr1 = actualType.normalize(NormalizeVisitor.Mode.NF, myLocalContext).liftIndex(0, -actualNumberOfPiArgs);
        if (expr1 != null) {
          resultEquations.add(new CompareVisitor.Equation((InferHoleExpression) resultType, expr1));
        }
      }

    List<Argument> resultLambdaArgs = new ArrayList<>(argumentTypes.size());
    for (TypeArgument argumentType : argumentTypes) {
      resultLambdaArgs.add(argumentType);
    }
    OKResult result = new OKResult(Lam(resultLambdaArgs, okBodyResult.expression), Pi(argumentTypes, okBodyResult.type), resultEquations);
    expr.setWellTyped(result.expression);
    return result;
  }

  @Override
  public Result visitPi(Abstract.PiExpression expr, Expression expectedType) {
    OKResult[] domainResults = new OKResult[expr.getArguments().size()];
    int numberOfVars = 0;
    List<CompareVisitor.Equation> equations = new ArrayList<>();
    for (int i = 0; i < domainResults.length; ++i) {
      Result result = typeCheck(expr.getArguments().get(i).getType(), Universe());
      if (!(result instanceof OKResult)) return result;
      domainResults[i] = (OKResult) result;
      addLiftedEquations(domainResults[i], equations, numberOfVars);
      if (expr.getArguments().get(i) instanceof Abstract.TelescopeArgument) {
        List<String> names = ((Abstract.TelescopeArgument) expr.getArguments().get(i)).getNames();
        for (int j = 0; j < names.size(); ++j) {
          myLocalContext.add(new TypedBinding(names.get(j), domainResults[i].expression.liftIndex(0, j)));
          ++numberOfVars;
        }
      } else {
        myLocalContext.add(new TypedBinding((Name) null, domainResults[i].expression));
        ++numberOfVars;
      }
    }

    Result codomainResult = typeCheck(expr.getCodomain(), Universe());
    for (int i = 0; i < numberOfVars; ++i) {
      myLocalContext.remove(myLocalContext.size() - 1);
    }
    if (!(codomainResult instanceof OKResult)) return codomainResult;
    OKResult okCodomainResult = (OKResult) codomainResult;

    Universe universe = new Universe.Type(Universe.NO_LEVEL, Universe.Type.PROP);
    for (int i = 0; i < domainResults.length; ++i) {
      Universe argUniverse = ((UniverseExpression) domainResults[i].type).getUniverse();
      Universe maxUniverse = universe.max(argUniverse);
      if (maxUniverse == null) {
        String msg = "Universe " + argUniverse + " of " + (i + 1) + suffix(i + 1) + " argument is not compatible with universe " + universe + " of previous arguments";
        TypeCheckingError error = new TypeCheckingError(myNamespace, msg, expr, getNames(myLocalContext));
        expr.setWellTyped(Error(null, error));
        myErrorReporter.report(error);
        return null;
      }
      universe = maxUniverse;
    }
    Universe codomainUniverse = ((UniverseExpression) okCodomainResult.type).getUniverse();
    Universe maxUniverse = universe.max(codomainUniverse);
    if (maxUniverse == null) {
      String msg = "Universe " + codomainUniverse + " the codomain is not compatible with universe " + universe + " of arguments";
      TypeCheckingError error = new TypeCheckingError(myNamespace, msg, expr, getNames(myLocalContext));
      expr.setWellTyped(Error(null, error));
      myErrorReporter.report(error);
      return null;
    }
    Expression actualType = new UniverseExpression(maxUniverse);

    addLiftedEquations(okCodomainResult, equations, numberOfVars);

    List<TypeArgument> resultArguments = new ArrayList<>(domainResults.length);
    for (int i = 0; i < domainResults.length; ++i) {
      resultArguments.add(argFromArgResult(expr.getArguments().get(i), domainResults[i]));
    }
    return checkResult(expectedType, new OKResult(Pi(resultArguments, okCodomainResult.expression), actualType, equations), expr);
  }

  private TypeArgument argFromArgResult(Abstract.TypeArgument arg, OKResult argResult) {
    if (arg instanceof Abstract.TelescopeArgument) {
      return new TelescopeArgument(arg.getExplicit(), ((Abstract.TelescopeArgument) arg).getNames(), argResult.expression);
    } else {
      return new TypeArgument(arg.getExplicit(), argResult.expression);
    }
  }

  @Override
  public Result visitUniverse(Abstract.UniverseExpression expr, Expression expectedType) {
    return checkResult(expectedType, new OKResult(new UniverseExpression(expr.getUniverse()), new UniverseExpression(expr.getUniverse().succ()), null), expr);
  }

  @Override
  public Result visitVar(Abstract.VarExpression expr, Expression expectedType) {
    OKResult result = getLocalVar(expr);
    if (result == null) {
      return null;
    }
    result.type = result.type.liftIndex(0, ((IndexExpression) result.expression).getIndex() + 1);
    return checkResultImplicit(expectedType, result, expr);
  }

  private OKResult getLocalVar(Abstract.VarExpression expr) {
    ListIterator<Binding> it = myLocalContext.listIterator(myLocalContext.size());
    int index = 0;
    while (it.hasPrevious()) {
      Binding def = it.previous();
      if (expr.getName().name.equals(def.getName() == null ? null : def.getName().name)) {
        return new OKResult(Index(index), def.getType(), null);
      }
      ++index;
    }

    NotInScopeError error = new NotInScopeError(myNamespace, expr, expr.getName());
    expr.setWellTyped(Error(null, error));
    myErrorReporter.report(error);
    return null;
  }

  @Override
  public Result visitError(Abstract.ErrorExpression expr, Expression expectedType) {
    TypeCheckingError error = new GoalError(myNamespace, myLocalContext, expectedType == null ? null : expectedType.normalize(NormalizeVisitor.Mode.NF, myLocalContext), expr);
    return new InferErrorResult(new InferHoleExpression(error), error, null);
  }

  @Override
  public Result visitInferHole(Abstract.InferHoleExpression expr, Expression expectedType) {
    TypeCheckingError error = new ArgInferenceError(myNamespace, expression(), expr, getNames(myLocalContext), null);
    expr.setWellTyped(Error(null, error));
    myErrorReporter.report(error);
    return null;
  }

  @Override
  public Result visitTuple(Abstract.TupleExpression expr, Expression expectedType) {
    Expression expectedTypeNorm = null;
    if (expectedType != null) {
      expectedTypeNorm = expectedType.normalize(NormalizeVisitor.Mode.WHNF, myLocalContext);
      if (!(expectedTypeNorm instanceof SigmaExpression || expectedType instanceof InferHoleExpression)) {
        Expression fExpectedTypeNorm = expectedTypeNorm.getFunction(new ArrayList<Expression>());
        if (fExpectedTypeNorm instanceof InferHoleExpression) {
          return new InferErrorResult((InferHoleExpression) fExpectedTypeNorm, ((InferHoleExpression) fExpectedTypeNorm).getError(), null);
        }

        TypeCheckingError error = new TypeMismatchError(myNamespace, expectedTypeNorm, Sigma(args(TypeArg(Error(null, null)), TypeArg(Error(null, null)))), expr, getNames(myLocalContext));
        expr.setWellTyped(Error(null, error));
        myErrorReporter.report(error);
        return null;
      }

      if (expectedTypeNorm instanceof SigmaExpression) {
        InferHoleExpression hole = expectedTypeNorm.accept(new FindHoleVisitor());
        if (hole != null) {
          return new InferErrorResult(hole, hole.getError(), null);
        }

        List<TypeArgument> sigmaArgs = new ArrayList<>();
        splitArguments(((SigmaExpression) expectedTypeNorm).getArguments(), sigmaArgs);

        if (expr.getFields().size() != sigmaArgs.size()) {
          TypeCheckingError error = new TypeCheckingError(myNamespace, "Expected a tuple with " + sigmaArgs.size() + " fields, but given " + expr.getFields().size(), expr, getNames(myLocalContext));
          expr.setWellTyped(Error(null, error));
          myErrorReporter.report(error);
          return null;
        }

        List<Expression> fields = new ArrayList<>(expr.getFields().size());
        Expression expression = Tuple(fields, (SigmaExpression) expectedTypeNorm);
        List<CompareVisitor.Equation> equations = new ArrayList<>();
        for (int i = 0; i < sigmaArgs.size(); ++i) {
          List<Expression> substExprs = new ArrayList<>(fields.size());
          for (int j = fields.size() - 1; j >= 0; --j) {
            substExprs.add(fields.get(j));
          }

          Expression expType = sigmaArgs.get(i).getType().subst(substExprs, 0);
          Result result = typeCheck(expr.getFields().get(i), expType);
          if (!(result instanceof OKResult)) return result;
          OKResult okResult = (OKResult) result;
          fields.add(okResult.expression);
          if (okResult.equations != null) {
            equations.addAll(okResult.equations);
          }
        }
        return new OKResult(expression, expectedType, equations);
      }
    }

    List<Expression> fields = new ArrayList<>(expr.getFields().size());
    List<TypeArgument> arguments = new ArrayList<>(expr.getFields().size());
    SigmaExpression type = Sigma(arguments);
    Expression expression = Tuple(fields, type);
    List<CompareVisitor.Equation> equations = new ArrayList<>();
    for (int i = 0; i < expr.getFields().size(); ++i) {
      Result result = typeCheck(expr.getFields().get(i), null);
      if (!(result instanceof OKResult)) return result;
      OKResult okResult = (OKResult) result;
      fields.add(okResult.expression);
      arguments.add(TypeArg(okResult.type.liftIndex(0, i)));
      if (okResult.equations != null) {
        equations.addAll(okResult.equations);
      }
    }

    if (expectedTypeNorm instanceof InferHoleExpression) {
      equations.add(new CompareVisitor.Equation((InferHoleExpression) expectedTypeNorm, type));
    }
    return new OKResult(expression, type, equations);
  }

  @Override
  public Result visitSigma(Abstract.SigmaExpression expr, Expression expectedType) {
    OKResult[] domainResults = new OKResult[expr.getArguments().size()];
    int numberOfVars = 0;
    List<CompareVisitor.Equation> equations = new ArrayList<>();
    for (int i = 0; i < domainResults.length; ++i) {
      Result result = typeCheck(expr.getArguments().get(i).getType(), Universe());
      if (!(result instanceof OKResult)) return result;
      domainResults[i] = (OKResult) result;
      addLiftedEquations(domainResults[i], equations, numberOfVars);
      if (expr.getArguments().get(i) instanceof Abstract.TelescopeArgument) {
        List<String> names = ((Abstract.TelescopeArgument) expr.getArguments().get(i)).getNames();
        for (int j = 0; j < names.size(); ++j) {
          myLocalContext.add(new TypedBinding(names.get(j), domainResults[i].expression.liftIndex(0, j)));
          ++numberOfVars;
        }
      } else {
        myLocalContext.add(new TypedBinding((Name) null, domainResults[i].expression));
        ++numberOfVars;
      }
    }

    for (int i = 0; i < numberOfVars; ++i) {
      myLocalContext.remove(myLocalContext.size() - 1);
    }

    Universe universe = new Universe.Type(Universe.NO_LEVEL, Universe.Type.PROP);
    for (int i = 0; i < domainResults.length; ++i) {
      Universe argUniverse = ((UniverseExpression) domainResults[i].type).getUniverse();
      Universe maxUniverse = universe.max(argUniverse);
      if (maxUniverse == null) {
        String msg = "Universe " + argUniverse + " of " + (i + 1) + suffix(i + 1) + " argument is not compatible with universe " + universe + " of previous arguments";
        TypeCheckingError error = new TypeCheckingError(myNamespace, msg, expr, getNames(myLocalContext));
        expr.setWellTyped(Error(null, error));
        myErrorReporter.report(error);
        return null;
      }
      universe = maxUniverse;
    }
    Expression actualType = new UniverseExpression(universe);

    List<TypeArgument> resultArguments = new ArrayList<>(domainResults.length);
    for (int i = 0; i < domainResults.length; ++i) {
      if (expr.getArguments().get(i) instanceof Abstract.TelescopeArgument) {
        resultArguments.add(new TelescopeArgument(expr.getArguments().get(i).getExplicit(), ((Abstract.TelescopeArgument) expr.getArguments().get(i)).getNames(), domainResults[i].expression));
      } else {
        resultArguments.add(new TypeArgument(expr.getArguments().get(i).getExplicit(), domainResults[i].expression));
      }
    }
    return checkResult(expectedType, new OKResult(Sigma(resultArguments), actualType, equations), expr);
  }

  @Override
  public Result visitBinOp(Abstract.BinOpExpression expr, final Expression expectedType) {
    class AbstractArgumentExpression implements Abstract.ArgumentExpression {
      Abstract.Expression expression;

      public AbstractArgumentExpression(Abstract.Expression expression) {
        this.expression = expression;
      }

      @Override
      public Abstract.Expression getExpression() {
        return expression;
      }

      @Override
      public boolean isExplicit() {
        return true;
      }

      @Override
      public boolean isHidden() {
        return false;
      }

      @Override
      public void prettyPrint(StringBuilder builder, List<String> names, byte prec) {
        expression.prettyPrint(builder, names, prec);
      }
    }

    List<Abstract.ArgumentExpression> args = new ArrayList<>(2);
    args.add(new AbstractArgumentExpression(expr.getLeft()));
    args.add(new AbstractArgumentExpression(expr.getRight()));

    Concrete.Position position = expr instanceof Concrete.Expression ? ((Concrete.Expression) expr).getPosition() : null;
    return typeCheckFunctionApps(new Concrete.DefCallExpression(position, expr.getBinOp()), args, expectedType, expr);
  }

  @Override
  public Result visitBinOpSequence(Abstract.BinOpSequenceExpression expr, Expression params) {
    throw new UnsupportedOperationException();
  }

  public static class ExpandPatternResult {
    public final Expression expression;
    public final Pattern pattern;
    public final int numBindings;

    public ExpandPatternResult(Expression expression, Pattern pattern, int numBindings) {
      this.expression = expression;
      this.pattern = pattern;
      this.numBindings = numBindings;
    }
  }

  private ExpandPatternResult expandPattern(Abstract.Pattern pattern, Binding binding, Abstract.Expression expr) {
    if (pattern instanceof Abstract.NamePattern) {
      String name = ((Abstract.NamePattern) pattern).getName();
      if (name == null) {
        myLocalContext.add(binding);
      } else {
        myLocalContext.add(new TypedBinding(((Abstract.NamePattern) pattern).getName(), binding.getType()));
      }
      return new ExpandPatternResult(Index(0), new NamePattern(name, pattern.getExplicit()), 1);
    } else if (pattern instanceof Abstract.ConstructorPattern) {
      TypeCheckingError error = null;
      Abstract.ConstructorPattern constructorPattern = (Abstract.ConstructorPattern) pattern;

      List<Expression> parameters = new ArrayList<>();
      Expression type = binding.getType().normalize(NormalizeVisitor.Mode.WHNF, myLocalContext);
      Expression ftype = type.getFunction(parameters);
      Collections.reverse(parameters);
      if (!(ftype instanceof DefCallExpression && ((DefCallExpression) ftype).getDefinition() instanceof DataDefinition)) {
        error = new TypeMismatchError(myNamespace, "a data type", type, expr, getNames(myLocalContext));
        expr.setWellTyped(Error(null, error));
        return null;
      }
      DataDefinition dataType = (DataDefinition) ((DefCallExpression) ftype).getDefinition();

      Constructor constructor = null;
      for (int index = 0; index < dataType.getConstructors().size(); ++index) {
        if (dataType.getConstructors().get(index).getName().equals(constructorPattern.getConstructorName())) {
          constructor = dataType.getConstructors().get(index);
        }
      }

      if (constructor == null) {
        error = new NotInScopeError(myNamespace, pattern, constructorPattern.getConstructorName());
        myErrorReporter.report(error);
        expr.setWellTyped(Error(null, error));
        return null;
      }

      if (constructor.hasErrors()) {
        error = new HasErrors(myNamespace, constructor.getName(), pattern);
        myErrorReporter.report(error);
        expr.setWellTyped(Error(null, error));
        return null;
      }

      List<Expression> matchedParameters = null;
      if (constructor.getPatterns() != null) {
        Utils.PatternMatchResult matchResult = patternMatchAll(constructor.getPatterns(), parameters, myLocalContext);
        if (matchResult instanceof PatternMatchMaybeResult) {
          error = new TypeCheckingError(myNamespace, "Constructor is not appropriate, failed to match data type parameters. " +
              "Expected " + ((PatternMatchMaybeResult) matchResult).maybePattern + ", got " + ((PatternMatchMaybeResult) matchResult).actualExpression, pattern, getNames(myLocalContext));
        } else if (matchResult instanceof PatternMatchFailedResult) {
          error = new TypeCheckingError(myNamespace, "Constructor is not appropriate, failed to match data type parameters. " +
              "Expected " + ((PatternMatchFailedResult) matchResult).failedPattern + ", got " + ((PatternMatchFailedResult) matchResult).actualExpression, pattern, getNames(myLocalContext));
        } else if (matchResult instanceof PatternMatchOKResult) {
          matchedParameters = ((PatternMatchOKResult) matchResult).expressions;
        }

        if (error != null) {
          myErrorReporter.report(error);
          expr.setWellTyped(Error(null, error));
          return null;
        }
      } else {
        matchedParameters = new ArrayList<>(parameters);
      }
      Collections.reverse(matchedParameters);
      List<TypeArgument> constructorArguments = new ArrayList<>();
      splitArguments(constructor.getType().subst(matchedParameters, 0), constructorArguments);

      Utils.ProcessImplicitResult implicitResult = processImplicit(constructorPattern.getPatterns(), constructorArguments);
      if (implicitResult.patterns == null) {
        if (implicitResult.numExcessive != 0) {
          error = new TypeCheckingError(myNamespace, "Too many arguments: " + implicitResult.numExcessive + " excessive", constructorPattern,
              getNames(myLocalContext));
        } else if (implicitResult.wrongImplicitPosition < constructorPattern.getPatterns().size()) {
          error = new TypeCheckingError(myNamespace, "Unexpected implicit argument", constructorPattern.getPatterns().get(implicitResult.wrongImplicitPosition), getNames(myLocalContext));
        } else {
          error = new TypeCheckingError(myNamespace, "Too few explicit arguments, expected: " + implicitResult.numExplicit, constructorPattern, getNames(myLocalContext));
        }
        myErrorReporter.report(error);
        expr.setWellTyped(Error(null, error));
        return null;
      }
      List<Abstract.Pattern> patterns = implicitResult.patterns;

      List<Pattern> resultPatterns = new ArrayList<>();
      List<Expression> substituteExpressions = new ArrayList<>();
      int numBindings = 0;
      Expression substExpression = DefCall(null, constructor, parameters);
      for (int i = 0; i < constructorArguments.size(); ++i) {
        Expression argumentType = constructorArguments.get(i).getType();
        for (int j = 0; j < i; j++) {
          argumentType = expandPatternSubstitute(resultPatterns.get(j), i - j - 1, substituteExpressions.get(j), argumentType);
        }
        ExpandPatternResult result = expandPattern(patterns.get(i), new TypedBinding((String) null, argumentType), expr);
        if (result == null)
          return null;
        substituteExpressions.add(result.expression);
        substExpression = Apps(substExpression.liftIndex(0, result.numBindings), result.expression);
        resultPatterns.add(result.pattern);
        numBindings += result.numBindings;
      }

      return new ExpandPatternResult(substExpression, new ConstructorPattern(constructor, resultPatterns, pattern.getExplicit()), numBindings);
    } else {
      throw new IllegalStateException();
    }
  }

  public ExpandPatternResult expandPatternOn(Abstract.Pattern pattern, int varIndex, Abstract.Expression origExpr) {
    int varContextIndex = myLocalContext.size() - 1 - varIndex;

    Binding binding = myLocalContext.get(varContextIndex);
    List<Binding> tail = new ArrayList<>(myLocalContext.subList(varContextIndex + 1, myLocalContext.size()));
    myLocalContext.subList(varContextIndex, myLocalContext.size()).clear();

    ExpandPatternResult result = expandPattern(pattern, binding, origExpr);
    if (result == null)
      return null;
    for (int i = 0; i < tail.size(); i++) {
      // TODO: check let clause... move subst to binding?
      myLocalContext.add(new TypedBinding(tail.get(i).getName(), expandPatternSubstitute(result.pattern, i, result.expression, tail.get(i).getType())));
    }

    return result;
  }

  public OKResult lookupLocalVar(Abstract.Expression expression) {
    OKResult exprOKResult;
    if (expression instanceof Abstract.VarExpression) {
      exprOKResult = getLocalVar((Abstract.VarExpression) expression);
      if (exprOKResult == null) return null;
    } else if (expression instanceof Abstract.IndexExpression) {
      exprOKResult = getIndex((Abstract.IndexExpression) expression);
    } else {
      throw new IllegalStateException();
    }
    return exprOKResult;
  }

  @Override
  public Result visitElim(Abstract.ElimExpression expr, Expression expectedType) {
    TypeCheckingError error = null;
    if (expectedType == null) {
      error = new TypeCheckingError(myNamespace, "Cannot infer type of the expression", expr, getNames(myLocalContext));
    }
    if (mySide != Side.LHS && error == null) {
      error = new TypeCheckingError(myNamespace, "\\elim is allowed only at the root of a definition", expr, getNames(myLocalContext));
    }

    if (error != null) {
      myErrorReporter.report(error);
      expr.setWellTyped(Error(null, error));
      return null;
     }

    if (!(expr.getExpression() instanceof Abstract.IndexExpression || expr.getExpression() instanceof Abstract.VarExpression)) {
      error = new TypeCheckingError(myNamespace, "\\elim can be applied only to a local variable", expr.getExpression(), getNames(myLocalContext));
      myErrorReporter.report(error);
      expr.setWellTyped(Error(null, error));
      return null;
    }

    OKResult exprOKResult = lookupLocalVar(expr.getExpression());

    if (exprOKResult == null) {
      return null;
    }

    List<Expression> parameters = new ArrayList<>();
    Expression ftype = exprOKResult.type.normalize(NormalizeVisitor.Mode.WHNF, myLocalContext).getFunction(parameters);
    Collections.reverse(parameters);
    if (!(ftype instanceof DefCallExpression && ((DefCallExpression) ftype).getDefinition() instanceof DataDefinition)) {
      error = new TypeCheckingError(myNamespace, "Elimination is allowed only for a data type variable.", expr.getExpression(), getNames(myLocalContext));
      myErrorReporter.report(error);
      expr.getExpression().setWellTyped(Error(null, error));
      return null;
    }

    int varIndex = ((IndexExpression) exprOKResult.expression).getIndex();

    List<Constructor> validConstructors = new ArrayList<>();
    for (Constructor constructor : ((DataDefinition) ((DefCallExpression) ftype).getDefinition()).getConstructors()) {
      if (constructor.hasErrors())
        continue;
      if (constructor.getPatterns() != null) {
        PatternMatchResult matchResult = patternMatchAll(constructor.getPatterns(), parameters, myLocalContext);
        if (matchResult instanceof PatternMatchMaybeResult) {
          error = new TypeCheckingError(myNamespace, "Elimination is not possible due to undetermined constructor: " + constructor.getName(), expr.getExpression(), getNames(myLocalContext));
          myErrorReporter.report(error);
          expr.getExpression().setWellTyped(Error(null, error));
          return null;
        }
        if (matchResult instanceof PatternMatchFailedResult)
          continue;
      }
      validConstructors.add(constructor);
    }

    Result errorResult = null;
    boolean wasError = false;
    Abstract.Clause otherwiseClause = null;

    List<Clause> clauses = new ArrayList<>();
    for (Abstract.Clause clause : expr.getClauses()) {
      try (CompleteContextSaver ignore = new CompleteContextSaver<>(myLocalContext)) {
        ExpandPatternResult result = expandPatternOn(clause.getPatterns().get(0), varIndex, expr.getExpression());
        if (result == null)
          return null;
        if (result.pattern instanceof NamePattern) {
          otherwiseClause = clause;
        } else if (result.pattern instanceof ConstructorPattern) {
          ConstructorPattern constructorPattern = (ConstructorPattern) result.pattern;
          if (!validConstructors.contains(constructorPattern.getConstructor())) {
            error = new TypeCheckingError(myNamespace, "Overlapping pattern matching: " + constructorPattern.getConstructor(), clause, getNames(myLocalContext));
            expr.setWellTyped(Error(null, error));
            myErrorReporter.report(error);
            continue;
          }
          validConstructors.remove(((ConstructorPattern) result.pattern).getConstructor());
        }
        Expression clauseExpectedType = expandPatternSubstitute(result.pattern, varIndex, result.expression, expectedType);
        Side side = clause.getArrow() == Abstract.Definition.Arrow.RIGHT || !(clause.getExpression() instanceof Abstract.ElimExpression) ? Side.RHS : Side.LHS;
        Result clauseResult = new CheckTypeVisitor(myNamespace, myLocalContext, myErrorReporter, side).typeCheck(clause.getExpression(), clauseExpectedType);
        if (!(clauseResult instanceof OKResult)) {
          wasError = true;
          if (errorResult == null) {
            errorResult = clauseResult;
          } else if (clauseResult instanceof InferErrorResult) {
            myErrorReporter.report(((InferErrorResult) errorResult).error);
            errorResult = clauseResult;
          }
        } else {
          clauses.add(new Clause(result.pattern, clause.getArrow(), clauseResult.expression, null));
        }
      }
    }

    if (wasError) {
      return errorResult;
    }

    if (!validConstructors.isEmpty() && otherwiseClause == null) {
      String msg = "Incomplete pattern matching";
      if (!((DefCallExpression) ftype).getDefinition().equals(Prelude.INTERVAL)) {
        msg += ". Unhandled constructors: ";
        for (int i = 0; i < validConstructors.size(); ++i) {
          if (i > 0) msg += ", ";
          msg += validConstructors.get(i).getName().getPrefixName();
        }
      }
      error = new TypeCheckingError(myNamespace, msg, expr, getNames(myLocalContext));
      expr.setWellTyped(Error(null, error));
      myErrorReporter.report(error);
    }

    if (validConstructors.isEmpty() && otherwiseClause != null) {
      String msg = "Overlapping pattern matching";
      error = new TypeCheckingError(myNamespace, msg, otherwiseClause, getNames(myLocalContext));
      expr.setWellTyped(Error(null, error));
      myErrorReporter.report(error);
    }

    ElimExpression result = Elim((IndexExpression) exprOKResult.expression, clauses);
    for (Clause clause : clauses) {
      if (clause != null) {
        clause.setElimExpression(result);
      }
    }

    return new OKResult(result, expectedType, null);
  }

  @Override
  public Result visitCase(Abstract.CaseExpression expr, Expression expectedType) {
    if (expectedType == null) {
      TypeCheckingError error = new TypeCheckingError(myNamespace, "Cannot infer type of the expression", expr, getNames(myLocalContext));
      expr.setWellTyped(Error(null, error));
      myErrorReporter.report(error);
      return null;
    }

    List<CompareVisitor.Equation> equations = new ArrayList<>();

    Result exprResult = typeCheck(expr.getExpression(), null);
    if (!(exprResult instanceof OKResult)) return exprResult;
    OKResult exprOKResult = (OKResult) exprResult;
    equations.addAll(exprOKResult.equations);
    Abstract.ElimExpression elim = wrapCaseToElim(expr);

    myLocalContext.add(new TypedBinding((Name) null, exprOKResult.type));
    Result elimResult = elim.accept(new CheckTypeVisitor(myNamespace, myLocalContext, myErrorReporter, Side.LHS), expectedType.liftIndex(0, 1));
    if (!(elimResult instanceof OKResult)) return elimResult;
    OKResult elimOKResult = (OKResult) elimResult;
    addLiftedEquations(elimOKResult, equations, 1);
    myLocalContext.remove(myLocalContext.size() - 1);

    LetExpression letExpression = Let(lets(let("caseF", lamArgs(Tele(vars("caseA"), exprOKResult.type)), elimOKResult.type,
        Abstract.Definition.Arrow.LEFT, elimOKResult.expression)), Apps(Index(0), exprOKResult.expression));

    expr.setWellTyped(exprOKResult.type.liftIndex(0, -1));
    return new OKResult(letExpression, exprOKResult.type.liftIndex(0, -1), equations);
  }


  private Abstract.ElimExpression wrapCaseToElim(final Abstract.CaseExpression expr) {
    return new Abstract.ElimExpression() {
      @Override
      public Abstract.Expression getExpression() {
        return Index(0);
      }

      @Override
      public List<? extends Abstract.Clause> getClauses() {
        return expr.getClauses();
      }

      @Override
      public <P, R> R accept(AbstractExpressionVisitor<? super P, ? extends R> visitor, P params) {
        return visitor.visitElim(this, params);
      }

      @Override
      public void setWellTyped(Expression wellTyped) {

      }

      @Override
      public void prettyPrint(StringBuilder builder, List<String> names, byte prec) {
        accept(new PrettyPrintVisitor(builder, names, 0), prec);
      }
    };
  }

  @Override
  public Result visitProj(Abstract.ProjExpression expr, Expression expectedType) {
    Result exprResult = typeCheck(expr.getExpression(), null);
    if (!(exprResult instanceof OKResult)) return exprResult;
    OKResult okExprResult = (OKResult) exprResult;
    Expression type = okExprResult.type.normalize(NormalizeVisitor.Mode.WHNF, myLocalContext);
    if (!(type instanceof SigmaExpression)) {
      TypeCheckingError error = new TypeCheckingError(myNamespace, "Expected an expression of a sigma type", expr, getNames(myLocalContext));
      expr.setWellTyped(Error(null, error));
      myErrorReporter.report(error);
      return null;
    }

    List<TypeArgument> splitArgs = new ArrayList<>();
    splitArguments(((SigmaExpression) type).getArguments(), splitArgs);
    if (expr.getField() < 0 || expr.getField() >= splitArgs.size()) {
      TypeCheckingError error = new TypeCheckingError(myNamespace, "Index " + (expr.getField() + 1) + " out of range", expr, getNames(myLocalContext));
      expr.setWellTyped(Error(null, error));
      myErrorReporter.report(error);
      return null;
    }

    List<Expression> exprs = new ArrayList<>(expr.getField());
    for (int i = expr.getField() - 1; i >= 0; --i) {
      exprs.add(Proj(okExprResult.expression, i));
    }
    return checkResult(expectedType, new OKResult(Proj(okExprResult.expression, expr.getField()), splitArgs.get(expr.getField()).getType().subst(exprs, 0), okExprResult.equations), expr);
  }

  @Override
  public Result visitClassExt(Abstract.ClassExtExpression expr, Expression expectedType) {
    Result result = typeCheck(expr.getBaseClassExpression(), null);
    if (!(result instanceof OKResult)) {
      return result;
    }
    Expression normalizedBaseClassExpr = result.expression.normalize(NormalizeVisitor.Mode.WHNF);
    if (!(normalizedBaseClassExpr instanceof DefCallExpression && ((DefCallExpression) normalizedBaseClassExpr).getDefinition() instanceof ClassDefinition)) {
      TypeCheckingError error = new TypeCheckingError(myNamespace, "Expected a class", expr.getBaseClassExpression(), getNames(myLocalContext));
      expr.setWellTyped(Error(normalizedBaseClassExpr, error));
      myErrorReporter.report(error);
      return null;
    }

    ClassDefinition baseClass = (ClassDefinition) ((DefCallExpression) normalizedBaseClassExpr).getDefinition();
    if (baseClass.hasErrors()) {
      TypeCheckingError error = new HasErrors(myNamespace, baseClass.getName(), expr.getBaseClassExpression());
      expr.setWellTyped(Error(normalizedBaseClassExpr, error));
      myErrorReporter.report(error);
      return null;
    }

    // TODO
    // if (expr.getDefinitions().isEmpty()) {
      return checkResultImplicit(expectedType, new OKResult(normalizedBaseClassExpr, baseClass.getType(), null), expr);
    // }

    /*
    Map<String, FunctionDefinition> abstracts = new HashMap<>();
    for (Definition definition : baseClass.getStatements()) {
      if (definition instanceof FunctionDefinition && definition.isAbstract()) {
        abstracts.put(definition.getName().name, (FunctionDefinition) definition);
      }
    }

    Map<FunctionDefinition, OverriddenDefinition> definitions = new HashMap<>();
    for (Abstract.FunctionDefinition definition : expr.getDefinitions()) {
      FunctionDefinition oldDefinition = abstracts.remove(definition.getName().name);
      if (oldDefinition == null) {
        myErrorReporter.report(new TypeCheckingError(myNamespace, definition.getName() + " is not defined in " + expr.getBaseClass().getNamespace().getFullName(), definition, getNames(myLocalContext)));
      } else {
        OverriddenDefinition newDefinition = (OverriddenDefinition) TypeChecking.typeCheckFunctionBegin(myErrorReporter, myNamespace, expr.getBaseClass().getLocalNamespace(), definition, myLocalContext, oldDefinition);
        if (newDefinition == null) return null;
        TypeChecking.typeCheckFunctionEnd(myErrorReporter, myNamespace, definition.getTerm(), newDefinition, myLocalContext, oldDefinition);
        definitions.put(oldDefinition, newDefinition);
      }
    }

    Universe universe = new Universe.Type(0, Universe.Type.PROP);
    for (FunctionDefinition definition : abstracts.values()) {
      universe = universe.max(definition.getUniverse());
    }
    return checkResultImplicit(expectedType, new OKResult(ClassExt(expr.getBaseClass(), definitions, universe), new UniverseExpression(universe), null), expr);
    */
  }

  @Override
  public Result visitNew(Abstract.NewExpression expr, Expression expectedType) {
    Result exprResult = typeCheck(expr.getExpression(), null);
    if (!(exprResult instanceof OKResult)) return exprResult;
    OKResult okExprResult = (OKResult) exprResult;
    Expression normExpr = okExprResult.expression.accept(new NormalizeVisitor(NormalizeVisitor.Mode.WHNF, myLocalContext));
    if (!(normExpr instanceof DefCallExpression && ((DefCallExpression) normExpr).getDefinition() instanceof ClassDefinition || normExpr instanceof ClassExtExpression)) {
      TypeCheckingError error = new TypeCheckingError(myNamespace, "Expected a class", expr.getExpression(), getNames(myLocalContext));
      expr.setWellTyped(Error(null, error));
      myErrorReporter.report(error);
      return null;
    }
    return checkResultImplicit(expectedType, new OKResult(New(okExprResult.expression), normExpr, okExprResult.equations), expr);
  }

  private Result typeCheckLetClause(Abstract.LetClause clause) {
    List<Argument> args = new ArrayList<>();
    Expression resultType;
    Expression term;
    List<CompareVisitor.Equation> equations = new ArrayList<>();

    try (ContextSaver saver = new ContextSaver(myLocalContext)) {
      int numVarsPassed = 0;
      for (int i = 0; i < clause.getArguments().size(); i++) {
        if (clause.getArguments().get(i) instanceof Abstract.TypeArgument) {
          Abstract.TypeArgument typeArgument = (Abstract.TypeArgument) clause.getArguments().get(i);
          Result result = typeCheck(typeArgument.getType(), Universe());
          if (!(result instanceof OKResult)) return result;
          OKResult okResult = (OKResult) result;
          args.add(argFromArgResult(typeArgument, okResult));
          addLiftedEquations(okResult, equations, numVarsPassed);
          if (typeArgument instanceof Abstract.TelescopeArgument) {
            List<String> names = ((Abstract.TelescopeArgument) typeArgument).getNames();
            for (int j = 0; j < names.size(); ++j) {
              myLocalContext.add(new TypedBinding(names.get(j), okResult.expression.liftIndex(0, j)));
              ++numVarsPassed;
            }
          } else {
            myLocalContext.add(new TypedBinding((Name) null, okResult.expression));
            ++numVarsPassed;
          }
        } else {
          throw new IllegalStateException();
        }
      }

      Expression expectedType = null;
      if (clause.getResultType() != null) {
        Result result = typeCheck(clause.getResultType(), null);
        if (!(result instanceof OKResult)) return result;
        addLiftedEquations(result, equations, numVarsPassed);
        expectedType = result.expression;
      }
      Result termResult = clause.getTerm().accept(new CheckTypeVisitor(myNamespace, myLocalContext, myErrorReporter, Side.LHS), expectedType);
      if (!(termResult instanceof OKResult)) return termResult;
      addLiftedEquations(termResult, equations, numVarsPassed);

      term = ((OKResult) termResult).expression;
      resultType = ((OKResult) termResult).type;
    }

    LetClause result = new LetClause(clause.getName(), args, resultType, clause.getArrow(), term);
    myLocalContext.add(result);
    return new LetClauseResult(result, equations);
  }

  private void addLiftedEquations(Result okResult, List<CompareVisitor.Equation> equations, int numVarsPassed) {
    if (okResult.equations != null) {
      for (CompareVisitor.Equation equation : okResult.equations) {
        Expression expr1 = equation.expression.liftIndex(0, -numVarsPassed);
        if (expr1 != null) {
          equations.add(new CompareVisitor.Equation(equation.hole, expr1));
        }
      }
    }
  }

  @Override
  public Result visitLet(Abstract.LetExpression expr, Expression expectedType) {
    OKResult finalResult;
    try (ContextSaver saver = new ContextSaver(myLocalContext)) {
      List<LetClause> clauses = new ArrayList<>();
      List<CompareVisitor.Equation> equations = new ArrayList<>();
      for (int i = 0; i < expr.getClauses().size(); i++) {
        Result clauseResult = typeCheckLetClause(expr.getClauses().get(i));
        if (!(clauseResult instanceof LetClauseResult)) return clauseResult;
        addLiftedEquations(clauseResult, equations, i);
        clauses.add(((LetClauseResult) clauseResult).letClause);
      }
      Result result = typeCheck(expr.getExpression(), expectedType == null ? null : expectedType.liftIndex(0, expr.getClauses().size()));
      if (!(result instanceof OKResult)) return result;
      OKResult okResult = (OKResult) result;
      addLiftedEquations(okResult, equations, expr.getClauses().size());

      Expression normalizedResultType = okResult.type.normalize(NormalizeVisitor.Mode.NF, myLocalContext).liftIndex(0, -expr.getClauses().size());
      if (normalizedResultType == null) {
        TypeCheckingError error = new TypeCheckingError(myNamespace, "Let result type depends on a bound variable.", expr, getNames(myLocalContext));
        expr.setWellTyped(Error(null, error));
        myErrorReporter.report(error);
        return null;
      }
      finalResult = new OKResult(Let(clauses, okResult.expression), normalizedResultType, equations);
    }
    return finalResult;
  }
}
