package com.jetbrains.jetpad.vclang;

import com.jetbrains.jetpad.vclang.editor.ModuleCell;
import com.jetbrains.jetpad.vclang.editor.ModuleMapper;
import com.jetbrains.jetpad.vclang.model.Module;
import com.jetbrains.jetpad.vclang.model.definition.FunctionDefinition;
import com.jetbrains.jetpad.vclang.model.expr.AppExpression;
import com.jetbrains.jetpad.vclang.model.expr.LamExpression;
import com.jetbrains.jetpad.vclang.model.expr.VarExpression;
import jetbrains.jetpad.cell.CellContainer;
import jetbrains.jetpad.mapper.Mapper;
import jetbrains.jetpad.projectional.util.RootController;
import jetbrains.jetpad.projectional.util.awt.AwtCellDemo;

public class AwtMain {

  public static void main(String[] args) {
    CellContainer container = createDemo();
    AwtCellDemo.show(container);
  }

  private static CellContainer createDemo() {
    Module m = createModel();
    Mapper<Module, ModuleCell> rootMapper = new ModuleMapper(m);
    rootMapper.attachRoot();

    CellContainer cellContainer = new CellContainer();
    cellContainer.root.children().add(rootMapper.getTarget());
    RootController.install(cellContainer);

    return cellContainer;
  }

  private static Module createModel() {
    Module result = new Module();
    FunctionDefinition def = new FunctionDefinition();
    result.definitions.add(def);
    LamExpression expr1 = new LamExpression(false);
    def.term.set(expr1);
    expr1.variable.set("x");
    AppExpression expr2 = new AppExpression(false);
    expr1.body.set(expr2);
    VarExpression expr3 = new VarExpression();
    expr2.function.set(expr3);
    expr3.name.set("x");
    VarExpression expr4 = new VarExpression();
    expr2.argument.set(expr4);
    expr4.name.set("x");
    return result;
  }
}
