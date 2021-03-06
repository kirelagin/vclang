package com.jetbrains.jetpad.vclang.core.internal;

import com.jetbrains.jetpad.vclang.core.context.param.DependentLink;
import com.jetbrains.jetpad.vclang.core.context.param.TypedDependentLink;
import com.jetbrains.jetpad.vclang.core.definition.ClassField;
import com.jetbrains.jetpad.vclang.core.expr.ClassCallExpression;
import com.jetbrains.jetpad.vclang.core.expr.Expression;
import com.jetbrains.jetpad.vclang.core.expr.ExpressionFactory;
import com.jetbrains.jetpad.vclang.core.expr.ReferenceExpression;
import com.jetbrains.jetpad.vclang.core.expr.visitor.ExpressionVisitor;
import com.jetbrains.jetpad.vclang.core.expr.visitor.NormalizeVisitor;
import com.jetbrains.jetpad.vclang.core.sort.Sort;

import java.util.*;
import java.util.stream.Collectors;

public class FieldSet implements ReadonlyFieldSet {
  public static class Implementation {
    public final TypedDependentLink thisParam;
    public final Expression term;

    public Implementation(TypedDependentLink thisParam, Expression term) {
      this.thisParam = thisParam;
      this.term = term;
    }

    public Expression substThisParam(Expression thisExpr) {
      return thisParam == null ? term : term.subst(thisParam, thisExpr);
    }
  }

  private final LinkedHashSet<ClassField> myFields;
  private final Map<ClassField, Implementation> myImplemented;
  private Sort mySort;

  public FieldSet(Sort sort) {
    this(new LinkedHashSet<>(), new HashMap<>(), sort);
  }

  public FieldSet(FieldSet other) {
    this(new LinkedHashSet<>(other.myFields), new HashMap<>(other.myImplemented), other.mySort);
  }

  private FieldSet(LinkedHashSet<ClassField> fields, Map<ClassField, Implementation> implemented, Sort sort) {
    myFields = fields;
    myImplemented = implemented;
    mySort = sort;
  }

  public void addField(ClassField field) {
    myFields.add(field);
  }

  public void addFieldsFrom(ReadonlyFieldSet other) {
    for (ClassField field : other.getFields()) {
      myFields.add(field);
    }
  }

  public boolean implementField(ClassField field, Implementation impl) {
    assert myFields.contains(field);
    Implementation old = myImplemented.put(field, impl);
    return old == null;
  }

  @Override
  public boolean isImplemented(ClassField field) {
    return myImplemented.containsKey(field);
  }

  @Override
  public Set<ClassField> getFields() {
    return myFields;
  }

  @Override
  public Set<Map.Entry<ClassField, Implementation>> getImplemented() {
    return myImplemented.entrySet();
  }

  @Override
  public Implementation getImplementation(ClassField field) {
    return myImplemented.get(field);
  }

  @Override
  public Sort getSort() {
    return mySort;
  }

  public void setSort(Sort sort) {
    mySort = sort;
  }

  public void updateSorts(ClassCallExpression thisClass) {
    mySort = Sort.PROP;
    for (ClassField field : myFields) {
      updateUniverseSingleField(field, thisClass);
    }
  }

  private void updateUniverseSingleField(ClassField field, ClassCallExpression thisClass) {
    if (myImplemented.containsKey(field)) return;

    Expression baseType = field.getBaseType(thisClass.getSortArgument());
    if (baseType.toError() != null) return;

    DependentLink thisParam = ExpressionFactory.parameter("\\this", thisClass);
    Expression expr = baseType.subst(field.getThisParameter(), new ReferenceExpression(thisParam)).normalize(NormalizeVisitor.Mode.WHNF);
    mySort = mySort.max(expr.getType().toSort());
  }

  public static <P> FieldSet applyVisitorToImplemented(ReadonlyFieldSet fieldSet, ReadonlyFieldSet parentFieldSet, ExpressionVisitor<P, Expression> visitor, P arg) {
    FieldSet newFieldSet = new FieldSet(fieldSet.getSort());
    newFieldSet.addFieldsFrom(fieldSet);
    for (Map.Entry<ClassField, FieldSet.Implementation> entry : fieldSet.getImplemented()) {
      if (parentFieldSet != null && parentFieldSet.isImplemented(entry.getKey())) {
        newFieldSet.implementField(entry.getKey(), entry.getValue());
      } else {
        newFieldSet.implementField(entry.getKey(), new FieldSet.Implementation(entry.getValue().thisParam, entry.getValue().term.accept(visitor, arg)));
      }
    }
    return newFieldSet;
  }

  @Override
  public String toString() {
    ArrayList<String> fields = myFields.stream().map(ClassField::getName).collect(Collectors.toCollection(ArrayList::new));
    ArrayList<String> impl = myImplemented.keySet().stream().map(ClassField::getName).collect(Collectors.toCollection(ArrayList::new));
    return "All: " + fields.toString() + " Impl: " + impl.toString();
  }
}
