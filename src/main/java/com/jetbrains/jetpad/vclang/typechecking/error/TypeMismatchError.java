package com.jetbrains.jetpad.vclang.typechecking.error;

import com.jetbrains.jetpad.vclang.module.Namespace;
import com.jetbrains.jetpad.vclang.term.Abstract;

import java.util.List;

public class TypeMismatchError extends TypeCheckingError {
  private final Object myExpected;
  private final Abstract.Expression myActual;

  public TypeMismatchError(Namespace namespace, Object expected, Abstract.Expression actual, Abstract.Expression expression, List<String> names) {
    super(namespace, null, expression, names);
    myExpected = expected;
    myActual = actual;
  }

  public TypeMismatchError(Object expected, Abstract.Expression actual, Abstract.Expression expression, List<String> names) {
    super(null, expression, names);
    myExpected = expected;
    myActual = actual;
  }

  @Override
  public String toString() {
    String message = printHeader();
    message += "Type mismatch:\n" +
        "\tExpected type: " + (myExpected instanceof Abstract.Expression ? prettyPrint((Abstract.Expression) myExpected) : myExpected.toString()) + "\n" +
        "\t  Actual type: " + prettyPrint(myActual);
    if (getCause() instanceof Abstract.PrettyPrintableSourceNode) {
      message += "\n" +
          "\tIn expression: " + prettyPrint((Abstract.PrettyPrintableSourceNode) getCause());
    }
    return message;
  }
}
