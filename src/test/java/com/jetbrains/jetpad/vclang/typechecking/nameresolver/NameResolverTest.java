package com.jetbrains.jetpad.vclang.typechecking.nameresolver;

import com.jetbrains.jetpad.vclang.module.Namespace;
import com.jetbrains.jetpad.vclang.module.RootModule;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.Concrete;
import com.jetbrains.jetpad.vclang.term.definition.Definition;
import com.jetbrains.jetpad.vclang.term.definition.FunctionDefinition;
import com.jetbrains.jetpad.vclang.term.expr.arg.Argument;
import com.jetbrains.jetpad.vclang.term.expr.arg.Utils;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static com.jetbrains.jetpad.vclang.parser.ParserTestCase.compare;
import static com.jetbrains.jetpad.vclang.term.expr.ExpressionFactory.*;
import static com.jetbrains.jetpad.vclang.typechecking.nameresolver.NameResolverTestCase.resolveNamesClass;
import static com.jetbrains.jetpad.vclang.typechecking.nameresolver.NameResolverTestCase.resolveNamesExpr;
import static org.junit.Assert.*;

public class NameResolverTest {
  @Test
  public void nameResolverError() {
    resolveNamesExpr("A { \\function f (x : Nat) <= \\elim x | zero => zero | suc x' => zero }", 1);
  }

  @Test
  public void nameResolverLamOpenError() {
    resolveNamesExpr("\\lam x => (\\Pi (y : Nat) -> (\\lam y => y)) y", 1);
  }

  @Test
  public void nameResolverPiOpenError() {
    resolveNamesExpr("\\Pi (a b : Nat a) -> Nat a b", 1);
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

    Concrete.Expression result = resolveNamesExpr("0 + 1 * 2 + 3 * (4 * 5) * (6 + 7)", new NamespaceNameResolver(namespace, null));
    assertNotNull(result);
    assertTrue(compare(BinOp(BinOp(Zero(), plus, BinOp(Suc(Zero()), mul, Suc(Suc(Zero())))), plus, BinOp(BinOp(Suc(Suc(Suc(Zero()))), mul, BinOp(Suc(Suc(Suc(Suc(Zero())))), mul, Suc(Suc(Suc(Suc(Suc(Zero()))))))), mul, BinOp(Suc(Suc(Suc(Suc(Suc(Suc(Zero())))))), plus, Suc(Suc(Suc(Suc(Suc(Suc(Suc(Zero())))))))))), result));
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

    resolveNamesExpr("11 + 2 * 3", 1, new NamespaceNameResolver(namespace, null));
  }

  @Test
  public void whereTest() {
    resolveNamesClass("test",
        "\\static \\function f (x : Nat) => B.b (a x) \\where\n" +
            "\\static \\function a (x : Nat) => x\n" +
            "\\static \\data D | D1 | D2\n" +
            "\\static \\class B { \\static \\data C | cr \\static \\function b (x : Nat) => D1 }");
  }

  @Test
  public void whereTestDefCmd() {
    resolveNamesClass("test",
        "\\static \\function f (x : Nat) => a \\where\n" +
            "\\static \\class A { \\static \\function a => 0 }\n" +
            "\\open A");
  }

  @Test
  public void whereError() {
    resolveNamesClass("test",
        "\\static \\function f (x : Nat) => x \\where\n" +
            "\\static \\function b => x", 1);
  }

  @Test
  public void whereClosedError() {
    resolveNamesClass("test",
        "\\static \\function f => x \\where\n" +
            "\\static \\class A { \\static \\function x => 0 }\n" +
            "\\open A\n" +
            "\\close A", 1);
  }

  @Test
  public void whereOpenFunction() {
    resolveNamesClass("test",
        "\\static \\function f => x \\where\n" +
            "\\static \\function b => 0 \\where\n" +
            "\\static \\function x => 0;\n" +
            "\\open b(x)");
  }

  @Test
  public void whereNoOpenFunctionError() {
    resolveNamesClass("test",
        "\\static \\function f => x \\where\n" +
            "\\static \\function b => 0 \\where\n" +
            "\\static \\function x => 0;", 1);
  }

  @Test
  public void whereNested() {
    resolveNamesClass("test",
        "\\static \\function f => x \\where\n" +
            "\\static \\data B | b\n" +
            "\\static \\function x => a \\where\n" +
            "\\static \\function a => b");
  }

  @Test
  public void whereOuterScope() {
    resolveNamesClass("test",
        "\\static \\function f => 0 \\where\n" +
            "\\static \\function g => 0\n" +
            "\\static \\function h => g");
  }

  @Test
  public void whereInSignature() {
    resolveNamesClass("test",
        "\\static \\function f : D => d \\where\n" +
            "\\static \\data D | d");
  }

  @Test
  public void whereAccessOuter() {
    resolveNamesClass("test",
        "\\static \\function f => 0 \\where\n" +
            "\\static \\function x => 0;\n" +
            "\\static \\function g => f.x");
  }

  @Test
  public void whereNonStaticOpen() {
    resolveNamesClass("test",
        "\\static \\function f => 0 \\where\n" +
            "\\static \\function x => 0\n" +
            "\\static \\function y => x;\n" +
            "\\static \\function g => 0 \\where\n" +
            "\\open f(y)");
  }

  @Test
  public void whereAbstractError() {
    resolveNamesClass("test", "\\static \\function f => 0 \\where \\function x : Nat", 1);
  }

  @Test
  public void numberOfFieldsTest() {
    Namespace localNamespace = resolveNamesClass("test", "\\static \\class Point { \\function x : Nat \\function y : Nat } \\static \\function C => Point { \\override x => 0 }");
    assertNotNull(localNamespace);
    assertNotNull(RootModule.ROOT.getMember("test"));
    Namespace staticNamespace = RootModule.ROOT.getMember("test").namespace;

    assertEquals(2, RootModule.ROOT.getMember("test").namespace.getMembers().size());
    assertEquals(0, localNamespace.getMembers().size());
    assertEquals(0, staticNamespace.getChild(new Utils.Name("Point")).getMembers().size());
    assertEquals(2, ((Abstract.ClassDefinition) staticNamespace.getMember("Point").abstractDefinition).getStatements().size());
  }

  @Test
  public void numberOfFieldsTest2() {
    Namespace localNamespace = resolveNamesClass("test", "\\function f : Nat \\static \\function g => 0 \\class B { \\function h => 0 \\static \\function k => 0 } \\static \\class C { \\function h => 0 \\static \\function k => 0 }");
    assertNotNull(localNamespace);
    assertNotNull(RootModule.ROOT.getMember("test"));
    Namespace staticNamespace = RootModule.ROOT.getMember("test").namespace;

    assertEquals(2, staticNamespace.getMembers().size());
    assertNotNull(staticNamespace.getMember("g"));
    assertTrue(staticNamespace.getMember("C").abstractDefinition instanceof Abstract.ClassDefinition);
    assertEquals(1, staticNamespace.getMember("C").namespace.getMembers().size());
    assertEquals(2, ((Abstract.ClassDefinition) staticNamespace.getMember("C").abstractDefinition).getStatements().size());
    assertEquals(2, localNamespace.getMembers().size());
    assertNotNull(localNamespace.getMember("f"));
    assertTrue(localNamespace.getMember("B").abstractDefinition instanceof Abstract.ClassDefinition);
    assertEquals(1, localNamespace.getMember("B").namespace.getMembers().size());
    assertEquals(2, ((Abstract.ClassDefinition) localNamespace.getMember("B").abstractDefinition).getStatements().size());
  }

  @Test
  public void openTest() {
    resolveNamesClass("test", "\\static \\class A { \\static \\function x => 0 } \\open A \\static \\function y => x");
  }

  @Test
  public void closeTestError() {
    resolveNamesClass("test", "\\static \\class A { \\static \\function x => 0 } \\open A \\static \\function y => x \\close A(x) \\function z => x", 1);
  }

  @Test
  public void exportTest() {
    resolveNamesClass("test", "\\static \\class A { \\static \\class B { \\static \\function x => 0 } \\export B } \\static \\function y => A.x");
  }

  @Test
  public void staticFieldAccCallTest() {
    resolveNamesClass("test", "\\static \\class A { \\function x : Nat \\class B { \\static \\function y => x } } \\static \\function f (a : A) => a.B.y");
  }

  @Test
  public void exportPublicFieldsTest() {
    Namespace localNamespace = resolveNamesClass("test", "\\static \\class A { \\static \\function x : Nat \\static \\class B { \\static \\function y => x } \\export B } \\static \\function f => A.y");
    assertNotNull(localNamespace);
    assertNotNull(RootModule.ROOT.getMember("test"));
    Namespace staticNamespace = RootModule.ROOT.getMember("test").namespace;

    assertEquals(2, staticNamespace.getMembers().size());
    assertNotNull(staticNamespace.getMember("A"));
    assertEquals(3, staticNamespace.getMember("A").namespace.getMembers().size());
    assertEquals(3, ((Abstract.ClassDefinition) staticNamespace.getMember("A").abstractDefinition).getStatements().size());
  }

  @Test
  public void nonStaticClassExportTestError() {
    resolveNamesClass("test", "\\class A { } \\static \\class B { \\export A }", 1);
  }

  @Test
  public void exportTest2() {
    Namespace localNamespace = resolveNamesClass("test",
        "\\function (+) (x y : Nat) : Nat\n" +
        "\\class A {\n" +
          "\\function x : Nat\n" +
          "\\class B {\n" +
            "\\function y : Nat\n" +
            "\\class C {\n" +
              "\\static \\function z => x + y\n" +
              "\\static \\function w => x\n" +
            "}\n" +
            "\\export C\n" +
          "}\n" +
          "\\class D { \\export B }\n" +
          "\\function f (b : B) : b.C.z = x + b.y => path (\\lam _ => x + b.y)\n" +
        "}");
    assertNotNull(localNamespace);
    assertNotNull(RootModule.ROOT.getMember("test"));
    Namespace staticNamespace = RootModule.ROOT.getMember("test").namespace;

    assertEquals(2, staticNamespace.getMembers().size());
    assertNotNull(localNamespace.getMember("A"));
    Abstract.ClassDefinition classA = (Abstract.ClassDefinition) localNamespace.getMember("A").abstractDefinition;
    assertEquals(4, classA.getStatements().size());
    assertTrue(localNamespace.getMember("A").namespace.getMembers().isEmpty());
    Abstract.ClassDefinition classB = (Abstract.ClassDefinition) getField(classA, "B");
    assertNotNull(classB);
    assertEquals(3, classB.getStatements().size());
    Abstract.ClassDefinition classC = (Abstract.ClassDefinition) getField(classB, "C");
    assertNotNull(classC);
    assertEquals(2, classC.getStatements().size());
    Abstract.ClassDefinition classD = (Abstract.ClassDefinition) getField(classA, "D");
    assertNotNull(classD);
    assertEquals(1, classD.getStatements().size());
  }

  private Abstract.Definition getField(Abstract.ClassDefinition classDefinition, String name) {
    for (Abstract.Statement statement : classDefinition.getStatements()) {
      if (statement instanceof Abstract.DefineStatement && ((Abstract.DefineStatement) statement).getDefinition().getName().name.equals(name)) {
        return ((Abstract.DefineStatement) statement).getDefinition();
      }
    }
    return null;
  }

  @Test
  public void neverCloseField() {
    resolveNamesClass("test", "\\static \\class A { \\static \\function x => 0 } \\static \\class B { \\open A \\export A \\close A } \\static \\class C { \\static \\function y => B.x }");
  }

  @Test
  public void exportExistingTestError() {
    resolveNamesClass("test", "\\static \\class A { \\static \\class B { \\static \\function x => 0 } } \\export A \\static \\class B { \\static \\function y => 0 }", 1);
  }

  @Test
  public void exportExistingTestError2() {
    resolveNamesClass("test", "\\static \\class B { \\static \\function y => 0 } \\static \\class A { \\static \\class B { \\static \\function x => 0 } } \\export A", 1);
  }

  @Test
  public void openExportTestError() {
    resolveNamesClass("test", "\\static \\class A { \\static \\class B { \\static \\function x => 0 } \\open B } \\static \\function y => A.x", 1);
  }

  @Test
  public void export2TestError() {
    resolveNamesClass("test", "\\static \\class A { \\static \\class B { \\static \\function x => 0 } \\export B } \\static \\function y => x", 1);
  }

  @Test
  public void openAbstractTestError() {
    resolveNamesClass("test", "\\static \\class A { \\function x : Nat } \\open A \\function y => x", 1);
  }

  @Test
  public void openAbstractTestError2() {
    resolveNamesClass("test", "\\static \\class A { \\function x : Nat \\function y => x } \\open A \\function z => y", 1);
  }

  @Test
  public void staticInOnlyStaticTestError() {
    resolveNamesClass("test", "\\function B : \\Type0 \\static \\class A {} \\static \\class A { \\static \\function s => 0 \\static \\data D (A : Nat) | foo Nat | bar }", 1);
  }

  @Test
  public void classExtensionWhereTestError() {
    resolveNamesClass("test", "\\static \\function f => 0 \\where \\static \\class A {} \\static \\class A { \\function x => 0 }", 1);
  }

  @Test
  public void multipleDefsWhere() {
    resolveNamesClass("test", "\\static \\function f => 0 \\where \\static \\function d => 0 \\static \\function d => 1", 1);
  }

  @Test
  public void overrideWhere() {
    resolveNamesClass("test", "\\static \\class A { \\function x => 0 } \\static \\function C => A { \\override x => y \\where \\function y => 0 }", 1);
  }
}
