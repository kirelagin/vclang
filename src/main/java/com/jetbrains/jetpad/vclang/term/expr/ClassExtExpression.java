package com.jetbrains.jetpad.vclang.term.expr;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.definition.Binding;
import com.jetbrains.jetpad.vclang.term.definition.FunctionDefinition;
import com.jetbrains.jetpad.vclang.term.definition.OverriddenDefinition;
import com.jetbrains.jetpad.vclang.term.definition.Universe;
import com.jetbrains.jetpad.vclang.term.expr.visitor.AbstractExpressionVisitor;
import com.jetbrains.jetpad.vclang.term.expr.visitor.ExpressionVisitor;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public class ClassExtExpression extends Expression implements Abstract.ClassExtExpression {
  private final Expression myBaseClassExpression;
  private final Map<FunctionDefinition, OverriddenDefinition> myDefinitions;
  private final Universe myUniverse;

  public ClassExtExpression(Expression baseClassExpression, Map<FunctionDefinition, OverriddenDefinition> definitions, Universe universe) {
    myBaseClassExpression = baseClassExpression;
    myDefinitions = definitions;
    myUniverse = universe;
  }

  @Override
  public Expression getBaseClassExpression() {
    return myBaseClassExpression;
  }

  @Override
  public Collection<OverriddenDefinition> getDefinitions() {
    return myDefinitions.values();
  }

  public Map<FunctionDefinition, OverriddenDefinition> getDefinitionsMap() {
    return myDefinitions;
  }

  public Universe getUniverse() {
    return myUniverse;
  }

  @Override
  public <T> T accept(ExpressionVisitor<? extends T> visitor) {
    return visitor.visitClassExt(this);
  }

  @Override
  public UniverseExpression getType(List<Binding> context) {
    return new UniverseExpression(myUniverse);
  }

  @Override
  public <P, R> R accept(AbstractExpressionVisitor<? super P, ? extends R> visitor, P params) {
    return visitor.visitClassExt(this, params);
  }
}
