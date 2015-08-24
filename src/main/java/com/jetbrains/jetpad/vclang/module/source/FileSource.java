package com.jetbrains.jetpad.vclang.module.source;

import com.jetbrains.jetpad.vclang.module.FileOperations;
import com.jetbrains.jetpad.vclang.module.ModuleLoader;
import com.jetbrains.jetpad.vclang.module.Namespace;
import com.jetbrains.jetpad.vclang.term.definition.ClassDefinition;
import com.jetbrains.jetpad.vclang.term.expr.arg.Utils;
import com.jetbrains.jetpad.vclang.typechecking.error.ErrorReporter;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public class FileSource extends ParseSource {
  private final File myFile;
  private final File myDirectory;

  public FileSource(ModuleLoader moduleLoader, ErrorReporter errorReporter, Namespace module, File baseDirectory) {
    super(moduleLoader, errorReporter, module);
    myFile = FileOperations.getFile(baseDirectory, module, FileOperations.EXTENSION);
    myDirectory = FileOperations.getFile(baseDirectory, module, "");
  }

  @Override
  public boolean isAvailable() {
    return myFile != null && myFile.exists() || myDirectory != null && myDirectory.exists() && myDirectory.isDirectory();
  }

  @Override
  public long lastModified() {
    if (myFile != null && myFile.exists()) {
      return myFile.lastModified();
    } else {
      return Long.MAX_VALUE;
    }
  }

  @Override
  public boolean load(Namespace namespace, ClassDefinition classDefinition) throws IOException {
    if (myFile != null && myFile.exists()) {
      setStream(new FileInputStream(myFile));
      return super.load(namespace, classDefinition);
    }

    if (myDirectory == null) {
      return false;
    }
    File[] files = myDirectory.listFiles();
    if (files == null) {
      return false;
    }
    for (File file : files) {
      if (file.isDirectory()) {
        namespace.getChild(new Utils.Name(file.getName()));
      } else
      if (file.isFile()) {
        String name = FileOperations.getVcFileName(file);
        if (name != null) {
          namespace.getChild(new Utils.Name(name));
        }
      }
    }
    return true;
  }
}
