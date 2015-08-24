package com.jetbrains.jetpad.vclang.module;

import com.jetbrains.jetpad.vclang.module.source.ParseSource;
import com.jetbrains.jetpad.vclang.typechecking.error.ErrorReporter;
import com.jetbrains.jetpad.vclang.typechecking.nameresolver.NameResolver;

import java.io.ByteArrayInputStream;

public class MemorySource extends ParseSource {
  public MemorySource(NameResolver nameResolver, ErrorReporter errorReporter, Namespace module, String source) {
    super(nameResolver, errorReporter, module);
    if (source != null) {
      setStream(new ByteArrayInputStream(source.getBytes()));
    }
  }

  @Override
  public boolean isAvailable() {
    return getStream() != null;
  }

  @Override
  public long lastModified() {
    return 0;
  }
}
