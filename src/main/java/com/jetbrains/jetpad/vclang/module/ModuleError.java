package com.jetbrains.jetpad.vclang.module;

public class ModuleError {
  private final Module myModule;
  private final String myMessage;

  public ModuleError(Module module, String message) {
    myModule = module;
    myMessage = message;
  }

  public Module getModule() {
    return myModule;
  }

  public String getMessage() {
    return myMessage;
  }

  @Override
  public String toString() {
    return myModule + ": " + (myMessage == null ? "Unknown error" : myMessage);
  }
}
