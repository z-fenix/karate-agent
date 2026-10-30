package io.karatelabs.ext.rules.analysis;

public final class RuleAnalysisException extends RuntimeException {
   public final String a;
   public final int b;
   public final String c;

   public RuleAnalysisException(String var1, int var2, String var3) {
      this(var1, var2, var3, null);
   }

   public RuleAnalysisException(String var1, int var2, String var3, String var4) {
      super(var1 + (var2 > 0 ? " at line " + var2 : "") + ": " + var3, null, false, false);
      this.a = var1;
      this.b = var2;
      this.c = var4;
   }
}
