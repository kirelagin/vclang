package com.jetbrains.jetpad.vclang.typechecking;

import com.jetbrains.jetpad.vclang.core.definition.FunctionDefinition;
import com.jetbrains.jetpad.vclang.core.sort.Sort;
import com.jetbrains.jetpad.vclang.term.Prelude;
import com.jetbrains.jetpad.vclang.typechecking.visitor.CheckTypeVisitor;
import org.junit.Ignore;
import org.junit.Test;

import static com.jetbrains.jetpad.vclang.ExpressionFactory.FunCall;
import static com.jetbrains.jetpad.vclang.ExpressionFactory.Ref;
import static com.jetbrains.jetpad.vclang.core.expr.ExpressionFactory.Nat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class TypeCheckingTest extends TypeCheckingTestCase {
  @Test
  public void typeCheckDefinition() {
    typeCheckClass(
        "\\function x : Nat => zero\n" +
        "\\function y : Nat => x");
  }

  @Test
  public void typeCheckDefType() {
    typeCheckClass(
        "\\function x : \\Set0 => Nat\n" +
        "\\function y : x => zero");
  }

  @Test
  public void typeCheckInfixDef() {
    typeCheckClass(
        "\\function (+) : Nat -> Nat -> Nat => \\lam x y => x\n" +
        "\\function (*) : Nat -> Nat => \\lam x => x + zero");
  }

  @Test
  public void typeCheckConstructor1() {
    typeCheckClass(
        "\\data D (n : Nat) {k : Nat} (m : Nat) | con\n" +
        "\\function idp {A : \\Type} {a : A} => path (\\lam _ => a)\n" +
        "\\function f : con {1} {2} {3} = (D 1 {2} 3).con => idp");
  }

  @Test
  public void typeCheckConstructor1d() {
    typeCheckClass(
        "\\data D (n : Nat) {k : Nat} (m : Nat) | con\n" +
        "\\function idp {A : \\Type} {a : A} => path (\\lam _ => a)\n" +
        "\\function f : con {1} {2} {3} = (D 1 {2} 3).con => idp");
  }

  @Test
  public void typeCheckConstructor2() {
    typeCheckClass(
        "\\data D (n : Nat) {k : Nat} (m : Nat) | con (k = m)\n" +
        "\\function idp {A : \\Type} {a : A} => path (\\lam _ => a)\n" +
        "\\function f : con {0} (path (\\lam _ => 1)) = (D 0).con idp => idp");
  }

  @Test
  public void typeCheckConstructor2d() {
    typeCheckClass(
        "\\data D (n : Nat) {k : Nat} (m : Nat) | con (k = m)\n" +
        "\\function idp {A : \\Type} {a : A} => path (\\lam _ => a)\n" +
        "\\function f : con {0} (path (\\lam _ => 1)) = (D 0).con idp => idp");
  }

  @Test
  public void testEither() {
    typeCheckClass(
        "\\data Either (A B : \\Type0) | inl A | inr B\n" +
        "\\function fun {A B : \\Type0} (e : Either A B) : \\Set0 <= \\elim e\n" +
        "  | inl _ => Nat\n" +
        "  | inr _ => Nat\n" +
        "\\function test : fun (inl {Nat} {Nat} 0) => 0");
  }

  @Test
  public void testPMap1() {
    typeCheckDef("\\function pmap {A B : \\Type1} {a a' : A} (f : A -> B) (p : a = a') : (f a = f a') => path (\\lam i => f (p @ i))");
  }

  @Test
  public void testPMap1Mix() {
    typeCheckDef("\\function pmap {A : \\Type1} {B : \\Type0} {a a' : A} (f : A -> B) (p : a = a') : (f a = f a') => path (\\lam i => f (p @ i))");
  }

  @Test
  public void testPMap1Error() {
    typeCheckDef("\\function pmap {A B : \\Type0} {a a' : A} (f : A -> B) (p : a = a') : ((=) {B} (f a) (f a'))" +
            " => path (\\lam i => f (p @ i))");
  }

  @Test
  public void testTransport1() {
    typeCheckDef("\\function transport {A : \\Type1} (B : A -> \\Type1) {a a' : A} (p : a = a') (b : B a) : B a' =>\n" +
        "coe (\\lam i => B ((@) {\\lam _ => A} {a} {a'} p i)) b right");
  }

  @Test
  public void testAt() {
    CheckTypeVisitor.Result result = typeCheckExpr("\\lam (p : suc = suc) => (p @ left) 0", null);
    assertNotNull(result.expression.getType());
  }

  @Test
  public void universeInference() {
    typeCheckClass(
        "\\function\n" +
        "transport {A : \\Type} (B : A -> \\Type) {a a' : A} (p : a = a') (b : B a)\n" +
        "  <= coe (\\lam i => B (p @ i)) b right\n" +
        "\n" +
        "\\function\n" +
        "foo (A : \\1-Type0) (B : A -> \\Type0) (a a' : A) (p : a = a') => transport B p");
  }

  @Test
  public void definitionsWithErrors() {
    typeCheckClass(
        "\\class C {\n" +
        "  \\field A : X\n" +
        "  \\field a : (\\lam (x : Nat) => Nat) A\n" +
        "}", 1, 2);
  }

  @Ignore
  @Test
  public void interruptThreadTest() {
    typeCheckClass(
      "\\function ack (m n : Nat) : Nat <= \\elim m, n | zero, n => suc n | suc m, zero => ack m 1 | suc m, suc n => ack m (ack (suc m) n)\n" +
      "\\function t : ack 4 4 = ack 4 4 => path (\\lam _ => ack 4 4)");
  }

  @Test
  public void parameters() {
    FunctionDefinition def = (FunctionDefinition) typeCheckDef("\\function f (x : Nat Nat) (p : (=) {Nat} x x) => p", 1);
    assertEquals(FunCall(Prelude.PATH_INFIX, Sort.SET0, Nat(), Ref(def.getParameters()), Ref(def.getParameters())), def.getResultType());
  }

  @Test
  public void constructorExpectedTypeMismatch() {
    typeCheckClass(
        "\\data Foo\n" +
        "\\data Bar (n : Nat) | Bar (suc n) => bar (n = n)\n" +
        "\\function foo : Foo => bar (path (\\lam _ => zero))", 1);
  }
}
