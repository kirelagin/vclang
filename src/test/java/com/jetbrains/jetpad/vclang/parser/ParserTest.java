package com.jetbrains.jetpad.vclang.parser;

import com.jetbrains.jetpad.vclang.module.Namespace;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.Concrete;
import com.jetbrains.jetpad.vclang.term.Prelude;
import com.jetbrains.jetpad.vclang.term.definition.Binding;
import com.jetbrains.jetpad.vclang.term.definition.ClassDefinition;
import com.jetbrains.jetpad.vclang.term.definition.Definition;
import com.jetbrains.jetpad.vclang.term.definition.FunctionDefinition;
import com.jetbrains.jetpad.vclang.term.expr.arg.Argument;
import com.jetbrains.jetpad.vclang.term.expr.arg.TypeArgument;
import com.jetbrains.jetpad.vclang.term.expr.arg.Utils;
import com.jetbrains.jetpad.vclang.term.expr.visitor.CheckTypeVisitor;
import com.jetbrains.jetpad.vclang.typechecking.error.ListErrorReporter;
import com.jetbrains.jetpad.vclang.typechecking.nameresolver.NameResolver;
import com.jetbrains.jetpad.vclang.typechecking.nameresolver.NamespaceNameResolver;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static com.jetbrains.jetpad.vclang.parser.ParserTestCase.*;
import static com.jetbrains.jetpad.vclang.term.expr.ExpressionFactory.*;
import static org.junit.Assert.*;

public class ParserTest {
  @Test
  public void parserLetToTheRight() {
    Concrete.Expression expr = parseExpr("\\lam x => \\let | x => Nat \\in x x");
    Concrete.Expression expr1 = parseExpr("\\let | x => Nat \\in \\lam x => x x");
    assertTrue(compare(Lam("x", Let(lets(let("x", lamArgs(), Nat())), Apps(Var("x"), Var("x")))), expr));
    assertTrue(compare(Let(lets(let("x", lamArgs(), Nat())), Lam("x", Apps(Var("x"), Var("x")))), expr1));
  }

  @Test
  public void parseLetMultiple() {
    Concrete.Expression expr = parseExpr("\\let | x => Nat | y => x \\in y");
    assertTrue(compare(Let(lets(let("x", Nat()), let("y", Var("x"))), Var("y")), expr));
  }

  @Test
  public void parseLetTyped() {
    Concrete.Expression expr = parseExpr("\\let | x : Nat => zero \\in x");
    assertTrue(compare(Let(lets(let("x", lamArgs(), Nat(), Abstract.Definition.Arrow.RIGHT, Zero())), Var("x")), expr));
  }

  @Test
  public void parserLam() {
    Concrete.Expression expr = parseExpr("\\lam x y z => y");
    assertTrue(compare(Lam("x", Lam("y", Lam("z", Var("y")))), expr));
  }

  @Test
  public void parserLam2() {
    Concrete.Expression expr = parseExpr("\\lam x y => (\\lam z w => y z) y");
    assertTrue(compare(Lam("x'", Lam("y'", Apps(Lam("z'", Lam("w'", Apps(Var("y"), Var("z")))), Var("y")))), expr));
  }

  @Test
  public void parserLamTele() {
    Concrete.Expression expr = parseExpr("\\lam p {x t : Nat} {y} (a : Nat -> Nat) => (\\lam (z w : Nat) => y z) y");
    assertTrue(compare(Lam(lamArgs(Name("p"), Tele(false, vars("x", "t"), DefCall(Prelude.NAT)), Name(false, "y"), Tele(vars("a"), Pi(DefCall(Prelude.NAT), DefCall(Prelude.NAT)))), Apps(Lam(lamArgs(Tele(vars("z", "w"), DefCall(Prelude.NAT))), Apps(Var("y"), Var("z"))), Var("y"))), expr));
  }

  @Test
  public void parserPi() {
    Concrete.Expression expr = parseExpr("\\Pi (x y z : Nat) (w t : Nat -> Nat) -> \\Pi (a b : \\Pi (c : Nat) -> Nat c) -> Nat b y w");
    assertTrue(compare(Pi(args(Tele(vars("x", "y", "z"), DefCall(Prelude.NAT)), Tele(vars("w", "t"), Pi(DefCall(Prelude.NAT), DefCall(Prelude.NAT)))), Pi(args(Tele(vars("a", "b"), Pi("c", DefCall(Prelude.NAT), Apps(DefCall(Prelude.NAT), Var("c"))))), Apps(DefCall(Prelude.NAT), Var("b"), Var("y"), Var("w")))), expr));
  }

  @Test
  public void parserPi2() {
    Concrete.Expression expr = parseExpr("\\Pi (x y : Nat) (z : Nat x -> Nat y) -> Nat z y x");
    assertTrue(compare(Pi(args(Tele(vars("x", "y"), DefCall(Prelude.NAT)), Tele(vars("z"), Pi(Apps(DefCall(Prelude.NAT), Var("x")), Apps(DefCall(Prelude.NAT), Var("y"))))), Apps(DefCall(Prelude.NAT), Var("z"), Var("y"), Var("x"))), expr));
  }

  @Test
  public void parserLamOpenError() {
    Concrete.Expression result = parseExpr("\\lam x => (\\Pi (y : Nat) -> (\\lam y => y)) y", 1);
    assertNull(result);
  }

  @Test
  public void parserPiOpenError() {
    Concrete.Expression result = parseExpr("\\Pi (a b : Nat a) -> Nat a b", 1);
    assertNull(result);
  }

  @Test
  public void parserDef() {
    ClassDefinition result = parseDefs(
      "\\static \\function x : Nat => zero\n" +
      "\\static \\function y : Nat => x");
    assertEquals(2, result.getNamespace().getMembers().size());
  }

  @Test
  public void parserDefType() {
    ClassDefinition result = parseDefs(
      "\\static \\function x : \\Type0 => Nat\n" +
      "\\static \\function y : x => zero");
    assertEquals(2, result.getNamespace().getMembers().size());
  }

  @Test
  public void parserImplicit() {
    FunctionDefinition def = (FunctionDefinition) parseDef("\\function f (x y : Nat) {z w : Nat} (t : Nat) {r : Nat} (A : Nat -> Nat -> Nat -> Nat -> Nat -> Nat -> \\Type0) : A x y z w t r");
    assertEquals(5, def.getArguments().size());
    assertTrue(def.getArguments().get(0).getExplicit());
    assertFalse(def.getArguments().get(1).getExplicit());
    assertTrue(def.getArguments().get(2).getExplicit());
    assertFalse(def.getArguments().get(3).getExplicit());
    assertTrue(compare(DefCall(Prelude.NAT), ((TypeArgument) def.getArguments().get(0)).getType()));
    assertTrue(compare(DefCall(Prelude.NAT), ((TypeArgument) def.getArguments().get(1)).getType()));
    assertTrue(compare(DefCall(Prelude.NAT), ((TypeArgument) def.getArguments().get(2)).getType()));
    assertTrue(compare(DefCall(Prelude.NAT), ((TypeArgument) def.getArguments().get(3)).getType()));
    assertTrue(compare(Apps(Index(0), Index(6), Index(5), Index(4), Index(3), Index(2), Index(1)), def.getResultType()));
  }

  @Test
  public void parserImplicit2() {
    FunctionDefinition def = (FunctionDefinition) parseDef("\\function f {x : Nat} (_ : Nat) {y z : Nat} (A : Nat -> Nat -> Nat -> \\Type0) (_ : A x y z) : Nat");
    assertEquals(5, def.getArguments().size());
    assertFalse(def.getArguments().get(0).getExplicit());
    assertTrue(def.getArguments().get(1).getExplicit());
    assertFalse(def.getArguments().get(2).getExplicit());
    assertTrue(def.getArguments().get(3).getExplicit());
    assertTrue(compare(DefCall(Prelude.NAT), ((TypeArgument) def.getArguments().get(0)).getType()));
    assertTrue(compare(DefCall(Prelude.NAT), ((TypeArgument) def.getArguments().get(1)).getType()));
    assertTrue(compare(DefCall(Prelude.NAT), ((TypeArgument) def.getArguments().get(2)).getType()));
    assertTrue(compare(Apps(Index(0), Index(4), Index(2), Index(1)), ((TypeArgument) def.getArguments().get(4)).getType()));
    assertTrue(compare(DefCall(Prelude.NAT), def.getResultType()));
  }

  @Test
  public void parserInfix() {
    List<Argument> arguments = new ArrayList<>(1);
    arguments.add(Tele(true, vars("x", "y"), Nat()));
    Namespace namespace = new Namespace(new Utils.Name("test"), null);
    Definition plus = new FunctionDefinition(namespace.getChild(new Utils.Name("+", Abstract.Definition.Fixity.INFIX)), null, new Definition.Precedence(Definition.Associativity.LEFT_ASSOC, (byte) 6), arguments, Nat(), Definition.Arrow.LEFT, null);
    Definition mul = new FunctionDefinition(namespace.getChild(new Utils.Name("*", Abstract.Definition.Fixity.INFIX)), null, new Definition.Precedence(Definition.Associativity.LEFT_ASSOC, (byte) 7), arguments, Nat(), Definition.Arrow.LEFT, null);
    namespace.addDefinition(plus);
    namespace.addDefinition(mul);

    ListErrorReporter errorReporter = new ListErrorReporter();
    NameResolver nameResolver = new NamespaceNameResolver(namespace, null);
    CheckTypeVisitor.Result result = parseExpr("0 + 1 * 2 + 3 * (4 * 5) * (6 + 7)", 0).accept(new CheckTypeVisitor(null, new ArrayList<Binding>(), errorReporter, CheckTypeVisitor.Side.RHS), null);
    assertEquals(0, errorReporter.getErrorList().size());
    assertTrue(result instanceof CheckTypeVisitor.OKResult);
    assertTrue(compare(BinOp(BinOp(Zero(), plus, BinOp(Suc(Zero()), mul, Suc(Suc(Zero())))), plus, BinOp(BinOp(Suc(Suc(Suc(Zero()))), mul, BinOp(Suc(Suc(Suc(Suc(Zero())))), mul, Suc(Suc(Suc(Suc(Suc(Zero()))))))), mul, BinOp(Suc(Suc(Suc(Suc(Suc(Suc(Zero())))))), plus, Suc(Suc(Suc(Suc(Suc(Suc(Suc(Zero())))))))))), result.expression));
  }

  @Test
  public void parserInfixDef() {
    ClassDefinition result = parseDefs(
      "\\static \\function (+) : Nat -> Nat -> Nat => \\lam x y => x\n" +
      "\\static \\function (*) : Nat -> Nat => \\lam x => x + zero");
    assertEquals(2, result.getNamespace().getMembers().size());
  }

  @Test
  public void parserInfixError() {
    List<Argument> arguments = new ArrayList<>(1);
    arguments.add(Tele(true, vars("x", "y"), Nat()));
    Namespace namespace = new Namespace(new Utils.Name("test"), null);
    Definition plus = new FunctionDefinition(namespace.getChild(new Utils.Name("+", Abstract.Definition.Fixity.INFIX)), null, new Definition.Precedence(Definition.Associativity.LEFT_ASSOC, (byte) 6), arguments, Nat(), Definition.Arrow.LEFT, null);
    Definition mul = new FunctionDefinition(namespace.getChild(new Utils.Name("*", Abstract.Definition.Fixity.INFIX)), null, new Definition.Precedence(Definition.Associativity.RIGHT_ASSOC, (byte) 6), arguments, Nat(), Definition.Arrow.LEFT, null);
    namespace.addDefinition(plus);
    namespace.addDefinition(mul);

    ListErrorReporter errorReporter = new ListErrorReporter();
    NameResolver nameResolver = new NamespaceNameResolver(namespace, null);
    parseExpr("11 + 2 * 3", 1).accept(new CheckTypeVisitor(null, new ArrayList<Binding>(), errorReporter, CheckTypeVisitor.Side.RHS), null);
    assertEquals(0, errorReporter.getErrorList().size());
  }

  @Test
  public void parserError() {
    parseExpr("A { \\function f (x : Nat) <= elim x | zero => zero | suc x' => zero }", -1);
  }

  @Test
  public void parserCase() {
    parseExpr("\\case 2 | zero => zero | suc x' => x'");
  }

  @Test
  public void whereTest() {
    parseDefs(
        "\\static \\function f (x : Nat) => B.b (a x) \\where\n" +
          "\\static \\function a (x : Nat) => x\n" +
          "\\static \\data D | D1 | D2\n" +
          "\\static \\class B { \\static \\data C | cr \\static \\function b (x : Nat) => D1 }");
  }

  @Test
  public void whereTestDefCmd() {
    parseDefs(
        "\\static \\function f (x : Nat) => a \\where\n" +
          "\\static \\class A { \\static \\function a => 0 }\n" +
          "\\open A");
  }

  @Test
  public void whereError() {
    parseDefs(
        "\\static \\function f (x : Nat) => x \\where\n" +
          "\\static \\function b => x", 1);
  }

  @Test
  public void whereClosedError() {
    parseDefs(
        "\\static \\function f => x \\where\n" +
          "\\static \\class A { \\static \\function x => 0 }\n" +
          "\\open A\n" +
          "\\close A", 1);
  }

  @Test
  public void whereOpenFunction() {
    parseDefs(
        "\\static \\function f => x \\where\n" +
          "\\static \\function b => 0 \\where\n" +
            "\\static \\function x => 0;\n" +
          "\\open b(x)");
  }

  @Test
  public void whereNoOpenFunctionError() {
    parseDefs(
        "\\static \\function f => x \\where\n" +
          "\\static \\function b => 0 \\where\n" +
            "\\static \\function x => 0;", 1);
  }

  @Test
  public void whereNested() {
    parseDefs(
        "\\static \\function f => x \\where\n" +
          "\\static \\data B | b\n" +
          "\\static \\function x => a \\where\n" +
            "\\static \\function a => b");
  }

  @Test
  public void whereOuterScope() {
    parseDefs(
        "\\static \\function f => 0 \\where\n" +
          "\\static \\function g => 0\n" +
          "\\static \\function h => g");
  }

  @Test
  public void whereInSignature() {
    parseDefs(
        "\\static \\function f : D => d \\where\n" +
          "\\static \\data D | d");
  }

  @Test
  public void whereAccessOuter() {
    parseDefs(
        "\\static \\function f => 0 \\where\n" +
          "\\static \\function x => 0;\n" +
        "\\static \\function g => f.x");
  }

  @Test
  public void whereNonStaticOpen() {
    parseDefs(
        "\\static \\function f => 0 \\where\n" +
          "\\static \\function x => 0\n" +
          "\\static \\function y => x;\n" +
        "\\static \\function g => 0 \\where\n" +
          "\\open f(y)");
  }

  @Test
  public void whereAbstractError() {
    parseDefs("\\static \\function f => 0 \\where \\function x : Nat", 1);
  }
}
