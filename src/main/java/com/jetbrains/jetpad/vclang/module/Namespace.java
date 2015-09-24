package com.jetbrains.jetpad.vclang.module;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.definition.Definition;
import com.jetbrains.jetpad.vclang.term.definition.NamespaceMember;
import com.jetbrains.jetpad.vclang.term.expr.arg.Utils;

import java.util.*;

public class Namespace implements NamespaceMember {
  final private Utils.Name myName;
  private Namespace myParent;
  private Map<String, DefinitionPair> myMembers;

  public Namespace(Utils.Name name, Namespace parent) {
    myName = name;
    myParent = parent;
  }

  @Override
  public Namespace getNamespace() {
    return this;
  }

  @Override
  public Utils.Name getName() {
    return myName;
  }

  public String getFullName() {
    return myParent == null || myParent == RootModule.ROOT ? myName.name : myParent.getFullName() + "." + myName.name;
  }

  public Namespace getParent() {
    return myParent;
  }

  public void setParent(Namespace parent) {
    myParent = parent;
  }

  public Collection<DefinitionPair> getMembers() {
    return myMembers == null ? Collections.<DefinitionPair>emptyList() : myMembers.values();
  }

  public Namespace getChild(Utils.Name name) {
    if (myMembers != null) {
      DefinitionPair member = myMembers.get(name.name);
      if (member != null) {
        return member.namespace;
      }
    } else {
      myMembers = new HashMap<>();
    }

    Namespace child = new Namespace(name, this);
    myMembers.put(name.name, new DefinitionPair(child, null, null));
    return child;
  }

  public DefinitionPair addChild(Namespace child) {
    if (myMembers == null) {
      myMembers = new HashMap<>();
      myMembers.put(child.myName.name, new DefinitionPair(child, null, null));
      return null;
    } else {
      DefinitionPair oldMember = myMembers.get(child.myName.name);
      if (oldMember != null) {
        return oldMember;
      } else {
        myMembers.put(child.myName.name, new DefinitionPair(child, null, null));
        return null;
      }
    }
  }

  public DefinitionPair getMember(String name) {
    return myMembers == null ? null : myMembers.get(name);
  }

  public Definition getDefinition(String name) {
    DefinitionPair member = getMember(name);
    return member == null ? null : member.definition;
  }

  public DefinitionPair locateName(String name) {
    for (Namespace namespace = this; namespace != null; namespace = namespace.getParent()) {
      DefinitionPair member = namespace.getMember(name);
      if (member != null) {
        return member;
      }
    }
    return null;
  }

  public DefinitionPair addAbstractDefinition(Abstract.Definition definition) {
    if (myMembers == null) {
      myMembers = new HashMap<>();
    } else {
      DefinitionPair oldMember = myMembers.get(definition.getName().name);
      if (oldMember != null) {
        if (oldMember.abstractDefinition != null) {
          return null;
        } else {
          oldMember.abstractDefinition = definition;
          return oldMember;
        }
      }
    }

    DefinitionPair result = new DefinitionPair(getChild(definition.getName()), definition, null);
    myMembers.put(definition.getName().name, result);
    return result;
  }

  public Definition addDefinition(Definition definition) {
    if (myMembers == null) {
      myMembers = new HashMap<>();
    } else {
      DefinitionPair oldMember = myMembers.get(definition.getName().name);
      if (oldMember != null) {
        if (oldMember.definition != null) {
          return oldMember.definition;
        } else {
          oldMember.definition = definition;
          return null;
        }
      }
    }

    myMembers.put(definition.getName().name, new DefinitionPair(definition.getNamespace(), null, definition));
    return null;
  }

  public DefinitionPair addMember(DefinitionPair member) {
    if (myMembers == null) {
      myMembers = new HashMap<>();
    } else {
      DefinitionPair oldMember = myMembers.get(member.namespace.getName().name);
      if (oldMember != null) {
        if (oldMember.definition != null || oldMember.abstractDefinition != null) {
          return oldMember;
        } else {
          oldMember.abstractDefinition = member.abstractDefinition;
          oldMember.definition = member.definition;
          return null;
        }
      }
    }

    myMembers.put(member.namespace.getName().name, member);
    return null;
  }

  public DefinitionPair removeMember(DefinitionPair member) {
    return myMembers.remove(member.namespace.getName().name);
  }

  public void clear() {
    myMembers = null;
  }

  @Override
  public String toString() {
    return getFullName();
  }

  @Override
  public boolean equals(Object other) {
    if (this == other) return true;
    if (!(other instanceof Namespace)) return false;
    if (myParent != ((Namespace) other).myParent) return false;
    if (myName == null) return ((Namespace) other).myName == null;
    return myName.name.equals(((Namespace) other).myName.name);
  }

  @Override
  public int hashCode() {
    return Arrays.hashCode(new Object[]{myParent, myName == null ? null : myName.name});
  }
}
