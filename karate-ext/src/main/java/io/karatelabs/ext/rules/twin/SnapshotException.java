package io.karatelabs.ext.rules.twin;

public class SnapshotException extends RuntimeException {
   public final String a;
   public final String b;
   public final String c;

   public SnapshotException(String var1, String var2) {
      this(var1, var2, null);
   }

   public SnapshotException(String var1, String var2, String var3) {
      super("snapshot " + var1 + " at " + var2 + (var3 == null ? "" : " in rulebook '" + var3 + "'"));
      this.a = var1;
      this.b = var2;
      this.c = var3;
   }
}
