package com.jetbrains.jetpad.vclang.module;

import com.jetbrains.jetpad.vclang.module.output.DummyOutputSupplier;
import com.jetbrains.jetpad.vclang.term.definition.ClassDefinition;
import com.jetbrains.jetpad.vclang.term.expr.arg.Utils;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class ModuleLoaderTest {
  SimpleModuleLoader moduleLoader;
  MemorySourceSupplier sourceSupplier;

  @Before
  public void initialize() {
    RootModule.initialize();
    moduleLoader = new SimpleModuleLoader(true);
    sourceSupplier = new MemorySourceSupplier(moduleLoader, moduleLoader.getErrorReporter());
  }

  @Test
  public void recursiveTestError() {
    Namespace moduleA = RootModule.ROOT.getChild(new Utils.Name("A"));
    Namespace moduleB = RootModule.ROOT.getChild(new Utils.Name("B"));
    sourceSupplier.add(moduleA, "\\static \\function f => B.g");
    sourceSupplier.add(moduleB, "\\static \\function g => A.f");

    moduleLoader.setSourceSupplier(sourceSupplier);
    moduleLoader.load(moduleA, false);
    assertFalse(moduleLoader.getErrorReporter().getErrorList().isEmpty());
  }

  @Test
  public void recursiveTestError2() {
    Namespace moduleA = RootModule.ROOT.getChild(new Utils.Name("A"));
    Namespace moduleB = RootModule.ROOT.getChild(new Utils.Name("B"));
    sourceSupplier.add(moduleA, "\\static \\function f => B.g");
    sourceSupplier.add(moduleB, "\\static \\function g => A.h");

    moduleLoader.setSourceSupplier(sourceSupplier);
    moduleLoader.load(moduleA, false);
    assertFalse(moduleLoader.getErrorReporter().getErrorList().isEmpty());
  }

  @Test
  public void recursiveTestError3() {
    Namespace moduleA = RootModule.ROOT.getChild(new Utils.Name("A"));
    Namespace moduleB = RootModule.ROOT.getChild(new Utils.Name("B"));
    sourceSupplier.add(moduleA, "\\static \\function f => B.g \\static \\function h => 0");
    sourceSupplier.add(moduleB, "\\static \\function g => A.h");

    moduleLoader.setSourceSupplier(sourceSupplier);
    moduleLoader.load(moduleA, false);
    assertFalse(moduleLoader.getErrorReporter().getErrorList().isEmpty());
  }

  @Test
  public void nonStaticTestError() {
    Namespace moduleA = RootModule.ROOT.getChild(new Utils.Name("A"));
    Namespace moduleB = RootModule.ROOT.getChild(new Utils.Name("B"));
    sourceSupplier.add(moduleA, "\\function f : Nat \\function h => f");
    sourceSupplier.add(moduleB, "\\static \\function g => A.h");

    moduleLoader.setSourceSupplier(sourceSupplier);
    moduleLoader.load(moduleB, false);
    assertEquals(1, moduleLoader.getErrorReporter().getErrorList().size());
  }

  @Test
  public void staticAbstractTestError() {
    Namespace module = RootModule.ROOT.getChild(new Utils.Name("A"));
    sourceSupplier.add(module, "\\static \\function f : Nat");

    moduleLoader.setSourceSupplier(sourceSupplier);
    moduleLoader.load(module, false);
    assertEquals(1, moduleLoader.getErrorReporter().getErrorList().size());
  }

  @Test
  public void moduleTest() {
    Module moduleA = new Module(moduleLoader.getRoot(), "A");
    sourceSupplier.add(moduleA, "\\function f : Nat \\static \\class C { \\function g : Nat \\function h => g }");

    moduleLoader.init(sourceSupplier, DummyOutputSupplier.getInstance(), true);
    moduleLoader.loadModule(moduleA, false);
    assertEquals(0, moduleLoader.getErrors().size());
    assertEquals(0, moduleLoader.getTypeCheckingErrors().size());
    assertEquals(1, moduleLoader.getRoot().getChild(new Utils.Name("A")).getMembers().size());
    assertEquals(1, ((ClassDefinition) moduleLoader.getRoot().getMember("A")).getLocalNamespace().getDefinitions().size());
    assertEquals(0, moduleLoader.getRoot().getMember("A").getNamespace().getMember("C").getNamespace().getMembers().size());
    assertEquals(2, ((ClassDefinition) moduleLoader.getRoot().getMember("A").getNamespace().getMember("C")).getLocalNamespace().getDefinitions().size());
  }

  @Test
  public void nonStaticTest() {
    Module moduleA = new Module(moduleLoader.getRoot(), "A");
    Module moduleB = new Module(moduleLoader.getRoot(), "B");
    sourceSupplier.add(moduleA, "\\function f : Nat \\static \\class B { \\function g : Nat \\function h => g }");
    sourceSupplier.add(moduleB, "\\static \\function f (p : A.B) => p.h");

    moduleLoader.init(sourceSupplier, DummyOutputSupplier.getInstance(), true);
    moduleLoader.loadModule(moduleB, false);
    assertEquals(0, moduleLoader.getErrors().size());
    assertEquals(0, moduleLoader.getTypeCheckingErrors().size());
  }

  @Test
  public void nonStaticTestError2() {
    Module moduleA = new Module(moduleLoader.getRoot(), "A");
    Module moduleB = new Module(moduleLoader.getRoot(), "B");
    sourceSupplier.add(moduleA, "\\function f : Nat \\class B { \\function g : Nat \\static \\function (+) (f g : Nat) => f \\function h => f + g }");
    sourceSupplier.add(moduleB, "\\static \\function f (p : A.B) => p.h");

    moduleLoader.init(sourceSupplier, DummyOutputSupplier.getInstance(), true);
    moduleLoader.loadModule(moduleB, false);
    assertEquals(1, moduleLoader.getErrors().size());
  }

  @Test
  public void abstractNonStaticTestError() {
    Module moduleA = new Module(moduleLoader.getRoot(), "A");
    Module moduleB = new Module(moduleLoader.getRoot(), "B");
    sourceSupplier.add(moduleA, "\\function f : Nat");
    sourceSupplier.add(moduleB, "\\function g => A.f");

    moduleLoader.init(sourceSupplier, DummyOutputSupplier.getInstance(), true);
    moduleLoader.loadModule(moduleB, false);
    assertEquals(1, moduleLoader.getErrors().size());
  }
}
