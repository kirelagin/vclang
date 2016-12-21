package com.jetbrains.jetpad.vclang.typechecking.termination;

/*Generated by MPS */

import com.jetbrains.jetpad.vclang.term.expr.DefCallExpression;
import com.jetbrains.jetpad.vclang.term.definition.FunctionDefinition;
import com.jetbrains.jetpad.vclang.term.context.param.DependentLink;
import com.jetbrains.jetpad.vclang.term.definition.Definition;

class CallMatrix extends LabeledCallMatrix {
  private DefCallExpression myCallExpression;
  private FunctionDefinition myEnclosingDefinition;

  CallMatrix(FunctionDefinition enclosingDefinition, DefCallExpression call) {
    super(DependentLink.Helper.size(call.getDefinition().getParameters()), enclosingDefinition.getNumberOfRequiredArguments());
    myCallExpression = call;
    myEnclosingDefinition = enclosingDefinition;
  }

  public CallMatrix(CallMatrix m) {
    super(m);
    myEnclosingDefinition = m.myEnclosingDefinition;
    myCallExpression = m.myCallExpression;
  }

  public Definition getCodomain() {
    return myCallExpression.getDefinition();
  }

  public Definition getDomain() {
    return myEnclosingDefinition;
  }

  @Override
  public int getCompositeLength() {
    return 1;
  }

  @Override
  public String getMatrixLabel() {
    return "In "+myEnclosingDefinition.getName() +": " + myCallExpression.toString();
  }
}
