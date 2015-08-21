package com.jetbrains.jetpad.vclang.module;

import com.jetbrains.jetpad.vclang.module.error.CycleError;
import com.jetbrains.jetpad.vclang.module.error.ModuleNotFoundError;
import com.jetbrains.jetpad.vclang.module.output.DummyOutputSupplier;
import com.jetbrains.jetpad.vclang.module.output.Output;
import com.jetbrains.jetpad.vclang.module.output.OutputSupplier;
import com.jetbrains.jetpad.vclang.module.source.DummySourceSupplier;
import com.jetbrains.jetpad.vclang.module.source.Source;
import com.jetbrains.jetpad.vclang.module.source.SourceSupplier;
import com.jetbrains.jetpad.vclang.serialization.ModuleDeserialization;
import com.jetbrains.jetpad.vclang.term.definition.ClassDefinition;
import com.jetbrains.jetpad.vclang.typechecking.error.ErrorReporter;
import com.jetbrains.jetpad.vclang.typechecking.error.GeneralError;

import java.io.EOFException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public abstract class SynchronousModuleLoader implements ModuleLoader {
  private final List<Namespace> myLoadingModules = new ArrayList<>();
  private SourceSupplier mySourceSupplier;
  private OutputSupplier myOutputSupplier;
  private final boolean myRecompile;
  private final Set<Namespace> myLoadedModules = new HashSet<>();
  private final ErrorReporter myErrorReporter;

  public SynchronousModuleLoader(ErrorReporter errorReporter, boolean recompile) {
    mySourceSupplier = DummySourceSupplier.getInstance();
    myOutputSupplier = DummyOutputSupplier.getInstance();
    myErrorReporter = errorReporter;
    myRecompile = true; // recompile; // TODO: Fix serialization.
  }

  public void setSourceSupplier(SourceSupplier sourceSupplier) {
    mySourceSupplier = sourceSupplier;
  }

  public void setOutputSupplier(OutputSupplier outputSupplier) {
    myOutputSupplier = outputSupplier;
  }

  @Override
  public boolean load(Namespace module) {
    if (myLoadedModules.contains(module)) {
      return false;
    }

    int index = myLoadingModules.indexOf(module);
    if (index != -1) {
      loadingError(new CycleError(module, new ArrayList<>(myLoadingModules.subList(index, myLoadedModules.size()))));
      return false;
    }

    Source source = mySourceSupplier.getSource(module);
    Output output = myOutputSupplier.getOutput(module);
    boolean compile;
    if (source.isAvailable()) {
      compile = myRecompile || !output.canRead() || source.lastModified() > output.lastModified();
    } else {
      output = myOutputSupplier.locateOutput(module);
      if (!output.canRead()) {
        loadingError(new ModuleNotFoundError(module));
      }
      compile = false;
    }

    myLoadingModules.add(module);
    ClassDefinition classDefinition = null;
    if (module.getParent().getMember(module.getName().name) == null) {
      classDefinition = new ClassDefinition(module);
    }
    try {
      if (compile) {
        if (source.load(module, classDefinition)) {
          if (classDefinition != null && !classDefinition.getFields().isEmpty()) {
            classDefinition = null;
          }
          if (output.canWrite()) {
            output.write(module, classDefinition);
          }
          loadingSucceeded(module, classDefinition, true);
        }
      } else {
        int errorsNumber = output.read(module, classDefinition);
        if (classDefinition != null && !classDefinition.getFields().isEmpty()) {
          classDefinition = null;
        }
        if (errorsNumber != 0) {
          loadingError(new GeneralError(module, "module contains " + errorsNumber + (errorsNumber == 1 ? " error" : " errors")));
        } else {
          loadingSucceeded(module, classDefinition, false);
        }
      }
    } catch (EOFException e) {
      loadingError(new GeneralError(module, "Incorrect format: Unexpected EOF"));
    } catch (ModuleDeserialization.DeserializationException e) {
      loadingError(new GeneralError(module, e.toString()));
    } catch (IOException e) {
      loadingError(new GeneralError(module, GeneralError.ioError(e)));
    }
    myLoadingModules.remove(myLoadingModules.size() - 1);
    myLoadedModules.add(module);

    if (classDefinition != null) {
      module.getParent().addMember(classDefinition);
    }
    return true;
  }

  @Override
  public void loadingError(GeneralError error) {
    myErrorReporter.report(error);
  }
}
