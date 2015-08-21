package com.jetbrains.jetpad.vclang.module.source;

import com.jetbrains.jetpad.vclang.module.ModuleLoader;
import com.jetbrains.jetpad.vclang.module.Namespace;
import com.jetbrains.jetpad.vclang.term.definition.ClassDefinition;
import com.jetbrains.jetpad.vclang.typechecking.error.ErrorReporter;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public class FileSource extends ParseSource {
  private final File myFile;
  private final File myDirectory;

  public FileSource(ModuleLoader moduleLoader, ErrorReporter errorReporter, Namespace module, File baseDirectory) {
    super(moduleLoader, errorReporter, module);
    myFile = getFile(module, baseDirectory, ".vc");
    myDirectory = getFile(module, baseDirectory, "");
  }

  @Override
  public boolean isAvailable() {
    return myFile != null && myFile.exists() || myDirectory != null && myDirectory.exists();
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
    } else {
      // TODO
      return false;
    }
  }

  private static File getFile(Namespace namespace, File dir) {
    return namespace == null || namespace.getParent() == null ? dir : new File(getFile(namespace.getParent(), dir), namespace.getName().name);
  }

  private static File getFile(Namespace namespace, File dir, String ext) {
    return new File(getFile(namespace.getParent(), dir), namespace.getName().name + ext);
  }
}
