package io.karatelabs.ext.rules.analysis;

public enum TernaryBoolean {
   TRUE,
   FALSE,
   MAYBE;

   public static TernaryBoolean of(boolean var0) {
      return var0 ? TRUE : FALSE;
   }

   public TernaryBoolean not() {
      return this == MAYBE ? MAYBE : (this == TRUE ? FALSE : TRUE);
   }

   public boolean definite() {
      return this != MAYBE;
   }
}
