package com.jetbrains.jetpad.vclang.module.caching.serialization;

import com.jetbrains.jetpad.vclang.core.definition.*;
import com.jetbrains.jetpad.vclang.module.caching.LocalizedTypecheckerState;
import com.jetbrains.jetpad.vclang.module.caching.PersistenceProvider;
import com.jetbrains.jetpad.vclang.module.source.SourceId;
import com.jetbrains.jetpad.vclang.term.Abstract;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class DefinitionStateDeserialization<SourceIdT extends SourceId> {
  private final PersistenceProvider<SourceIdT> myPersistenceProvider;
  private final SourceIdT mySourceId;

  public DefinitionStateDeserialization(SourceIdT sourceId, PersistenceProvider<SourceIdT> persistenceProvider) {
    mySourceId = sourceId;
    myPersistenceProvider = persistenceProvider;
  }

  public void readStubs(ModuleProtos.Module.DefinitionState in, LocalizedTypecheckerState<SourceIdT>.LocalTypecheckerState state) throws DeserializationError {
    for (Map.Entry<String, DefinitionProtos.Definition> entry : in.getDefinitionMap().entrySet()) {
      String id = entry.getKey();
      DefinitionProtos.Definition defProto = entry.getValue();
      final Definition def;
      final Abstract.Definition abstractDef = getAbstract(id);
      switch (defProto.getDefinitionDataCase()) {
        case CLASS:
          ClassDefinition classDef = new ClassDefinition((Abstract.ClassDefinition) abstractDef);
          for (String constructorId : defProto.getClass_().getFieldsMap().keySet()) {
            Abstract.ClassField absField = (Abstract.ClassField) getAbstract(constructorId);
            ClassField res = new ClassField(absField, classDef);
            res.setStatus(Definition.TypeCheckingStatus.NO_ERRORS);
            state.record(absField, res);
          }
          def = classDef;
          break;
        case DATA:
          DataDefinition dataDef = new DataDefinition((Abstract.DataDefinition) abstractDef);
          for (String constructorId : defProto.getData().getConstructorsMap().keySet()) {
            Abstract.Constructor absConstructor = (Abstract.Constructor) getAbstract(constructorId);
            Constructor res = new Constructor(absConstructor, dataDef);
            res.setStatus(Definition.TypeCheckingStatus.NO_ERRORS);
            state.record(absConstructor, res);
          }
          def = dataDef;
          break;
        case FUNCTION:
          def = new FunctionDefinition(getAbstract(id));
          break;
        default:
          throw new DeserializationError("Unknown Definition kind: " + defProto.getDefinitionDataCase());
      }

      switch (defProto.getStatus()) {
        case HEADER_HAS_ERRORS:
          def.setStatus(Definition.TypeCheckingStatus.HEADER_HAS_ERRORS);
          break;
        case BODY_HAS_ERRORS:
          def.setStatus(Definition.TypeCheckingStatus.BODY_HAS_ERRORS);
          break;
        case HEADER_NEEDS_TYPE_CHECKING:
          def.setStatus(Definition.TypeCheckingStatus.HEADER_NEEDS_TYPE_CHECKING);
          break;
        case BODY_NEEDS_TYPE_CHECKING:
          def.setStatus(Definition.TypeCheckingStatus.BODY_NEEDS_TYPE_CHECKING);
          break;
        case HAS_ERRORS:
          def.setStatus(Definition.TypeCheckingStatus.HAS_ERRORS);
          break;
        case NO_ERRORS:
          def.setStatus(Definition.TypeCheckingStatus.NO_ERRORS);
          break;
        default:
          throw new IllegalStateException("Unknown typechecking state");
      }

      state.record(abstractDef, def);
    }
  }

  public void fillInDefinitions(ModuleProtos.Module.DefinitionState in, LocalizedTypecheckerState<SourceIdT>.LocalTypecheckerState state, CalltargetProvider calltargetProvider) throws DeserializationError {
    CalltargetProvider.Typed typedCalltargetProvider = new CalltargetProvider.Typed(calltargetProvider);

    for (Map.Entry<String, DefinitionProtos.Definition> entry : in.getDefinitionMap().entrySet()) {
      String id = entry.getKey();
      DefinitionProtos.Definition defProto = entry.getValue();

      final Definition def = getTypechecked(state, id);

      final DefinitionDeserialization defDeserializer = new DefinitionDeserialization(typedCalltargetProvider);

      readClassifyingFields(def, typedCalltargetProvider, defProto.getClassifyingFieldList());

      if (defProto.getThisClassRef() != 0) {
        def.setThisClass(typedCalltargetProvider.getCalltarget(defProto.getThisClassRef(), ClassDefinition.class));
      }

      switch (defProto.getDefinitionDataCase()) {
        case CLASS:
          ClassDefinition classDef = (ClassDefinition) def;
          fillInClassDefinition(defDeserializer, typedCalltargetProvider, defProto.getClass_(), classDef, state);
          break;
        case DATA:
          DataDefinition dataDef = (DataDefinition) def;
          fillInDataDefinition(defDeserializer, defProto.getData(), dataDef, state);
          break;
        case FUNCTION:
          FunctionDefinition functionDef = (FunctionDefinition) def;
          fillInFunctionDefinition(defDeserializer, defProto.getFunction(), functionDef);
          break;
        default:
          throw new DeserializationError("Unknown Definition kind: " + defProto.getDefinitionDataCase());
      }
    }
  }

  private void fillInClassDefinition(DefinitionDeserialization defDeserializer, CalltargetProvider.Typed calltargetProvider, DefinitionProtos.Definition.ClassData classProto, ClassDefinition classDef, LocalizedTypecheckerState<SourceIdT>.LocalTypecheckerState state) throws DeserializationError {
    classDef.setFieldSet(defDeserializer.readFieldSet(classProto.getFieldSet()));

    Set<ClassDefinition> superClasses = new HashSet<>();
    for (int superClassRef : classProto.getSuperClassRefList()) {
      ClassDefinition superClass = calltargetProvider.getCalltarget(superClassRef, ClassDefinition.class);
      superClasses.add(superClass);
    }
    classDef.setSuperClasses(superClasses);
    if (classProto.getEnclosingThisFieldRef() != 0) {
      classDef.setEnclosingThisField(calltargetProvider.getCalltarget(classProto.getEnclosingThisFieldRef(), ClassField.class));
    }

    for (Map.Entry<String, DefinitionProtos.Definition.ClassData.Field> entry : classProto.getFieldsMap().entrySet()) {
      DefinitionProtos.Definition.ClassData.Field fieldProto = entry.getValue();
      ClassField field = getTypechecked(state, entry.getKey());
      field.setThisParameter(defDeserializer.readParameter(fieldProto.getThisParam()));
      field.setBaseType(defDeserializer.readExpr(fieldProto.getType()));
    }
  }

  private void fillInDataDefinition(DefinitionDeserialization defDeserializer, DefinitionProtos.Definition.DataData dataProto, DataDefinition dataDef, LocalizedTypecheckerState<SourceIdT>.LocalTypecheckerState state) throws DeserializationError {
    dataDef.setParameters(defDeserializer.readParameters(dataProto.getParamList()));
    dataDef.setSort(defDeserializer.readSort(dataProto.getSort()));

    for (Map.Entry<String, DefinitionProtos.Definition.DataData.Constructor> entry : dataProto.getConstructorsMap().entrySet()) {
      DefinitionProtos.Definition.DataData.Constructor constructorProto = entry.getValue();
      Constructor constructor = getTypechecked(state, entry.getKey());
      if (constructorProto.hasPatterns()) {
        constructor.setPatterns(defDeserializer.readPatterns(constructorProto.getPatterns()));
      }
      constructor.setParameters(defDeserializer.readParameters(constructorProto.getParamList()));
      if (constructorProto.hasCondition()) {
        constructor.setCondition(defDeserializer.readElimTree(constructorProto.getCondition()));
      }
      dataDef.addConstructor(constructor);
    }

    if (dataProto.getMatchesOnInterval()) {
      dataDef.setMatchesOnInterval();
    }

    int index = 0;
    for (Boolean isCovariant : dataProto.getCovariantParameterList()) {
      if (isCovariant) {
        dataDef.setCovariant(index);
      }
      index++;
    }
  }

  private void fillInFunctionDefinition(DefinitionDeserialization defDeserializer, DefinitionProtos.Definition.FunctionData functionProto, FunctionDefinition functionDef) throws DeserializationError {
    functionDef.setParameters(defDeserializer.readParameters(functionProto.getParamList()));
    functionDef.setResultType(defDeserializer.readExpr(functionProto.getType()));
    if (functionProto.hasElimTree()) {
      functionDef.setElimTree(defDeserializer.readElimTree(functionProto.getElimTree()));
    }
  }

  private void readClassifyingFields(Definition definition, CalltargetProvider.Typed calltargetProvider, List<DefinitionProtos.Definition.ClassifyingFields> classifyingFields) throws DeserializationError {
    int index = 0;
    for (DefinitionProtos.Definition.ClassifyingFields ref : classifyingFields) {
      if (ref.getFieldRefCount() > 0) {
        definition.setClassifyingFieldOfParameter(index, calltargetProvider.getCalltarget(ref.getFieldRef(0), ClassField.class));
      }
      index++;
    }
  }


  private Abstract.Definition getAbstract(String id) {
    return myPersistenceProvider.getFromId(mySourceId, id);
  }

  private <DefinitionT extends Definition> DefinitionT getTypechecked(LocalizedTypecheckerState<SourceIdT>.LocalTypecheckerState state, String id) throws DeserializationError {
    try {
      //noinspection unchecked
      return (DefinitionT) state.getTypechecked(getAbstract(id));
    } catch (ClassCastException ignored) {
      throw new DeserializationError("Stored Definition data does not match its kind");
    }
  }
}
