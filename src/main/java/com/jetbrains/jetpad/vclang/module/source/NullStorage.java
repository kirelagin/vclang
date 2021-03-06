package com.jetbrains.jetpad.vclang.module.source;

import com.jetbrains.jetpad.vclang.error.ErrorReporter;
import com.jetbrains.jetpad.vclang.module.ModulePath;

import javax.annotation.Nonnull;
import java.io.InputStream;
import java.io.OutputStream;

public class NullStorage<SourceIdT extends SourceId> implements Storage<SourceIdT> {
  @Override
  public InputStream getCacheInputStream(SourceIdT sourceId) {
    return null;
  }

  @Override
  public OutputStream getCacheOutputStream(SourceIdT sourceId) {
    return null;
  }

  @Override
  public SourceIdT locateModule(@Nonnull ModulePath modulePath) {
    return null;
  }

  @Override
  public boolean isAvailable(@Nonnull SourceIdT sourceId) {
    return false;
  }

  @Override
  public LoadResult loadSource(@Nonnull SourceIdT sourceId, @Nonnull ErrorReporter errorReporter) {
    return null;
  }

  @Override
  public long getAvailableVersion(@Nonnull SourceIdT sourceId) {
    return 0;
  }
}
