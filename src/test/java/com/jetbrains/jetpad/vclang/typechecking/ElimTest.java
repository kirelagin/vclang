package com.jetbrains.jetpad.vclang.typechecking;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.Prelude;
import com.jetbrains.jetpad.vclang.term.definition.*;
import com.jetbrains.jetpad.vclang.term.definition.visitor.DefinitionCheckTypeVisitor;
import com.jetbrains.jetpad.vclang.term.error.TypeCheckingError;
import com.jetbrains.jetpad.vclang.term.expr.Clause;
import com.jetbrains.jetpad.vclang.term.expr.ElimExpression;
import com.jetbrains.jetpad.vclang.term.expr.Expression;
import com.jetbrains.jetpad.vclang.term.expr.arg.Argument;
import com.jetbrains.jetpad.vclang.term.expr.arg.TelescopeArgument;
import com.jetbrains.jetpad.vclang.term.expr.arg.TypeArgument;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.jetbrains.jetpad.vclang.term.expr.ExpressionFactory.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class ElimTest {
  @Test
  public void elim() {
    List<TypeArgument> parameters = new ArrayList<>(2);
    parameters.add(TypeArg(Nat()));
    parameters.add(Tele(vars("x", "y"), Nat()));
    List<Constructor> constructors = new ArrayList<>(2);
    List<TypeArgument> arguments1 = new ArrayList<>(1);
    List<TypeArgument> arguments2 = new ArrayList<>(2);
    arguments1.add(TypeArg(Nat()));
    arguments2.add(TypeArg(Pi(Nat(), Nat())));
    arguments2.add(Tele(vars("a", "b", "c"), Nat()));
    DataDefinition dataType = new DataDefinition("D", Abstract.Definition.DEFAULT_PRECEDENCE, Abstract.Definition.Fixity.PREFIX, new Universe.Type(Universe.NO_LEVEL), parameters, constructors);
    constructors.add(new Constructor(0, "con1", Abstract.Definition.DEFAULT_PRECEDENCE, Abstract.Definition.Fixity.PREFIX, new Universe.Type(Universe.NO_LEVEL), arguments1, dataType));
    constructors.add(new Constructor(1, "con2", Abstract.Definition.DEFAULT_PRECEDENCE, Abstract.Definition.Fixity.PREFIX, new Universe.Type(Universe.NO_LEVEL), arguments2, dataType));

    List<TelescopeArgument> arguments3 = new ArrayList<>(4);
    arguments3.add(Tele(vars("a1", "b1", "c1"), Nat()));
    arguments3.add(Tele(vars("d1"), Apps(DefCall(dataType), Index(2), Index(1), Index(0))));
    arguments3.add(Tele(vars("a2", "b2", "c2"), Nat()));
    arguments3.add(Tele(vars("d2"), Apps(DefCall(dataType), Index(2), Index(1), Index(0))));
    List<Argument> arguments11 = new ArrayList<>(1);
    List<Argument> arguments12 = new ArrayList<>(4);
    arguments11.add(Name("s"));
    arguments12.add(Name("x"));
    arguments12.add(Name("y"));
    arguments12.add(Name("z"));
    arguments12.add(Name("t"));
    List<Clause> clauses1 = new ArrayList<>(2);
    ElimExpression pTerm = Elim(Abstract.ElimExpression.ElimType.ELIM, Index(4), clauses1, null);
    clauses1.add(new Clause(constructors.get(0), arguments11, Abstract.Definition.Arrow.RIGHT, Nat(), pTerm));
    clauses1.add(new Clause(constructors.get(1), arguments12, Abstract.Definition.Arrow.RIGHT, Pi(Nat(), Nat()), pTerm));
    FunctionDefinition pFunction = new FunctionDefinition("P", Abstract.Definition.DEFAULT_PRECEDENCE, Abstract.Definition.Fixity.PREFIX, arguments3, Universe(), Abstract.Definition.Arrow.LEFT, pTerm);

    List<TelescopeArgument> arguments = new ArrayList<>(3);
    arguments.add(Tele(vars("q", "w"), Nat()));
    arguments.add(Tele(vars("e"), Apps(DefCall(dataType), Var("w"), Zero(), Var("q"))));
    arguments.add(Tele(vars("r"), Apps(DefCall(dataType), Var("q"), Var("w"), Suc(Zero()))));
    Expression resultType = Apps(DefCall(pFunction), Var("w"), Zero(), Var("q"), Var("e"), Var("q"), Var("w"), Suc(Zero()), Var("r"));
    List<Clause> clauses2 = new ArrayList<>();
    List<Clause> clauses3 = new ArrayList<>();
    List<Clause> clauses4 = new ArrayList<>();
    ElimExpression term2 = Elim(Abstract.ElimExpression.ElimType.ELIM, Var("r"), clauses2, null);
    ElimExpression term3 = Elim(Abstract.ElimExpression.ElimType.ELIM, Var("e"), clauses3, null);
    ElimExpression term4 = Elim(Abstract.ElimExpression.ElimType.ELIM, Var("e"), clauses4, null);
    clauses2.add(new Clause(constructors.get(1), arguments12, Abstract.Definition.Arrow.LEFT, term4, term2));
    clauses2.add(new Clause(constructors.get(0), arguments11, Abstract.Definition.Arrow.LEFT, term3, term2));
    clauses3.add(new Clause(constructors.get(1), arguments12, Abstract.Definition.Arrow.RIGHT, Var("x"), term3));
    clauses3.add(new Clause(constructors.get(0), arguments11, Abstract.Definition.Arrow.RIGHT, Var("s"), term3));
    clauses4.add(new Clause(constructors.get(0), arguments11, Abstract.Definition.Arrow.RIGHT, Apps(Var("x"), Var("z")), term4));
    clauses4.add(new Clause(constructors.get(1), arguments12, Abstract.Definition.Arrow.RIGHT, Index(7), term4));
    FunctionDefinition function = new FunctionDefinition("fun", Abstract.Definition.DEFAULT_PRECEDENCE, Abstract.Definition.Fixity.PREFIX, arguments, resultType, Abstract.Definition.Arrow.LEFT, term2);

    Map<String, Definition> globalContext = new HashMap<>(Prelude.DEFINITIONS);
    globalContext.put("D", dataType);
    globalContext.put("P", pFunction);
    globalContext.put("con1", constructors.get(0));
    globalContext.put("con2", constructors.get(1));
    List<TypeCheckingError> errors = new ArrayList<>();
    Definition result = function.accept(new DefinitionCheckTypeVisitor(globalContext, errors), new ArrayList<Binding>());
    assertEquals(0, errors.size());
    assertNotNull(result);
  }
}
