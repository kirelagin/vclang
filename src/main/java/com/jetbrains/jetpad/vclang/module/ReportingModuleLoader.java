package com.jetbrains.jetpad.vclang.module;

import com.jetbrains.jetpad.vclang.typechecking.error.GeneralError;
import com.jetbrains.jetpad.vclang.typechecking.error.reporter.ErrorReporter;

public class ReportingModuleLoader extends BaseModuleLoader {
  private ErrorReporter myErrorReporter;

  public ReportingModuleLoader(ErrorReporter errorReporter, boolean recompile) {
    super(recompile);
    myErrorReporter = errorReporter;
  }

  public void setErrorReporter(ErrorReporter errorReporter) {
    myErrorReporter = errorReporter;
  }

  @Override
  public void loadingError(GeneralError error) {
    myErrorReporter.report(error);
  }

  @Override
  public void loadingSucceeded(Namespace namespace, DefinitionPair definitionPair, boolean compiled) {

  }
}
