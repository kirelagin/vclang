package com.jetbrains.jetpad.vclang.naming.namespace;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.Abstract.DefineStatement.StaticMod;

import java.util.HashSet;
import java.util.Set;

public class ClassNamespace implements NameSpace {
  private final Abstract.ClassDefinition myClass;

  public ClassNamespace(Abstract.ClassDefinition cls) {
    this.myClass = cls;
  }

  @Override
  public Set<String> getGlobalNames() {
    StaticMod curMod = StaticMod.STATIC;
    Set<String> names = new HashSet<>();
    for (Abstract.Statement statement : myClass.getStatements()) {
      if (!(statement instanceof Abstract.DefineStatement)) continue;
      Abstract.DefineStatement defst = (Abstract.DefineStatement) statement;
      if (!StaticMod.DEFAULT.equals(defst.getStaticMod())) {
        curMod = defst.getStaticMod();
      }
      if (StaticMod.STATIC.equals(curMod)) {
        names.add(((Abstract.DefineStatement) statement).getDefinition().getName());
      }
    }
    return names;
  }

  @Override
  public Abstract.Definition resolveGlobalDefinition(String name) {
    StaticMod curMod = StaticMod.STATIC;
    for (Abstract.Statement statement : myClass.getStatements()) {
      if (!(statement instanceof Abstract.DefineStatement)) continue;
      Abstract.DefineStatement defst = (Abstract.DefineStatement) statement;
      if (!StaticMod.DEFAULT.equals(defst.getStaticMod())) {
        curMod = defst.getStaticMod();
      }
      if (StaticMod.STATIC.equals(curMod) && name.equals(defst.getDefinition().getName())) {
        return defst.getDefinition();
      }
    }
    return null;
  }
}
