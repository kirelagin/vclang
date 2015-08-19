package com.jetbrains.jetpad.vclang.typechecking;

import com.jetbrains.jetpad.vclang.term.definition.NamespaceMember;

abstract public class BaseNameResolver implements NameResolver {
  @Override
  public NamespaceMember getMember(NamespaceMember parent, String name) {
    return namespace.getMember(fieldName);
  }

  /*
  @Override
  public DefCallExpression getField(Definition definition, String fieldName) {
    ClassDefinition type = getTypeOf(definition);
    if (type != null) {
      Definition field = getMember(type.getLocalNamespace(), fieldName);
      return field != null ? DefCall(DefCall(definition), field) : null;
    }
    return null;
  }

  @Override
  public DefCallExpression getField(String name, String fieldName) {
    NamespaceMember member = locateName(name);
    Namespace namespace;
    if (member instanceof Definition) {
      DefCallExpression result = getField((Definition) member, fieldName);
      if (result != null) {
        return result;
      } else {
        namespace = ((Definition) member).getNamespace();
      }
    } else
    if (member instanceof Namespace) {
      namespace = (Namespace) member;
    } else {
      return null;
    }

    Definition definition = getMember(namespace, fieldName);
    return definition != null ? DefCall(definition) : null;
  }

  protected static ClassDefinition getTypeOf(Definition definition) {
    if (definition.hasErrors()) {
      return null;
    }
    Expression type = definition.getType().normalize(NormalizeVisitor.Mode.WHNF);
    if (type instanceof DefCallExpression && ((DefCallExpression) type).getDefinition() instanceof ClassDefinition) {
      return (ClassDefinition) ((DefCallExpression) type).getDefinition();
    } else {
      return null;
    }
  }
  */
}
