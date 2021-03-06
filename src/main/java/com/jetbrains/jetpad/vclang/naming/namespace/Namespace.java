package com.jetbrains.jetpad.vclang.naming.namespace;

import com.jetbrains.jetpad.vclang.error.ReportableRuntimeException;
import com.jetbrains.jetpad.vclang.term.Abstract;

import java.util.Set;

public interface Namespace {
  Set<String> getNames();
  Abstract.Definition resolveName(String name);

  abstract class InvalidNamespaceException extends ReportableRuntimeException {}
}
