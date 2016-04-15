package com.jetbrains.jetpad.vclang.naming;

import com.jetbrains.jetpad.vclang.naming.namespace.ModuleNamespace;
import com.jetbrains.jetpad.vclang.naming.namespace.ModuleNamespaceProvider;
import com.jetbrains.jetpad.vclang.naming.namespace.NameSpace;
import com.jetbrains.jetpad.vclang.naming.namespace.NamespaceProvider;
import com.jetbrains.jetpad.vclang.naming.scope.Scope;
import com.jetbrains.jetpad.vclang.term.Abstract;

public class NewNameResolver {
  private final NamespaceProvider myNamespaceProvider;
  private final ModuleNamespaceProvider myModuleNamespaceProvider;

  public NewNameResolver(NamespaceProvider namespaceProvider, ModuleNamespaceProvider moduleNamespaceProvider) {
    this.myNamespaceProvider = namespaceProvider;
    this.myModuleNamespaceProvider = moduleNamespaceProvider;
  }

  public ModuleNamespace resolveModuleNamespace(final Abstract.DefCallExpression moduleCall) {
    if (moduleCall.getReferent() != null) {
      if (moduleCall.getReferent() instanceof Abstract.ClassDefinition) {
        return myModuleNamespaceProvider.forModule((Abstract.ClassDefinition) moduleCall.getReferent());
      } else {
        return null;
      }
    }
    if (moduleCall.getName() == null) throw new IllegalArgumentException();

    final ModuleNamespace parentNs;
    if (moduleCall.getExpression() == null) {
      parentNs = myModuleNamespaceProvider.root();
    } else if (moduleCall.getExpression() instanceof Abstract.DefCallExpression) {
      parentNs = resolveModuleNamespace((Abstract.DefCallExpression) moduleCall.getExpression());
    } else {
      parentNs = null;
    }
    return parentNs != null ? parentNs.getSubmoduleNamespace(moduleCall.getName()) : null;
  }

  public Abstract.Definition resolveDefCall(final Scope curretScope, final Abstract.DefCallExpression defCall) {
    if (defCall.getReferent() != null) {
      if (!(defCall.getReferent() instanceof Abstract.Definition)) throw new IllegalStateException(); // FIXME: Can’t happen
      return (Abstract.Definition) defCall.getReferent();
    }
    if (defCall.getName() == null) throw new IllegalArgumentException();

    if (defCall.getExpression() == null) {
      return curretScope.resolveGlobalDefinition(defCall.getName());
    } else if (defCall.getExpression() instanceof Abstract.DefCallExpression) {
      Abstract.Definition exprTarget = resolveDefCall(curretScope, (Abstract.DefCallExpression) defCall.getExpression());
      final NameSpace ns;
      if (exprTarget != null) {
        ns = myNamespaceProvider.forDefinition(exprTarget);
      } else {
        ns = resolveModuleNamespace((Abstract.DefCallExpression) defCall.getExpression());
      }
      return ns != null ? ns.resolveGlobalDefinition(defCall.getName()) : null;
    } else {
      return null;
    }
  }
}
