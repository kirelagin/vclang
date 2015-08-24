package com.jetbrains.jetpad.vclang.module;

import java.io.File;

public class FileOperations {
  public static final String EXTENSION = ".vc";

  public static File getFile(File dir, Namespace namespace) {
    return namespace == null || namespace.getParent() == null ? dir : new File(getFile(dir, namespace.getParent()), namespace.getName().name);
  }

  public static File getFile(File dir, Namespace namespace, String ext) {
    return new File(getFile(dir, namespace.getParent()), namespace.getName().name + ext);
  }

  public static String getVcFileName(File file) {
    String name = file.getName();
    if (name.endsWith(EXTENSION)) {
      return name.substring(0, name.length() - EXTENSION.length());
    } else {
      return null;
    }
  }
}
