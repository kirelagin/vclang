package com.jetbrains.jetpad.vclang.editor.hybrid;

import jetbrains.jetpad.hybrid.parser.SimpleToken;
import jetbrains.jetpad.hybrid.parser.Token;

public class Tokens {
  public static final Token LP = new SimpleToken("(", false, true);
  public static final Token RP = new SimpleToken(")", true, false);
  public static final Token LB = new SimpleToken("{", false, true);
  public static final Token RB = new SimpleToken("}", true, false);
  public static final Token ARROW = new SimpleToken("->");
  public static final Token DOUBLE_ARROW = new SimpleToken("=>");
  public static final Token COLON = new SimpleToken(":");
  public static final Token LAMBDA = new SimpleToken("\\", false, true);
}
