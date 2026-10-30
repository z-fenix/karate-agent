package io.karatelabs.ext.rules.twin;

public record TwinExecutionLimits(int component1, int b, int c, int d) {
   public static final TwinExecutionLimits a = new TwinExecutionLimits(12, 1000, 5000, 600);

   public TwinExecutionLimits(int component1, int b, int c, int d) {
      this.component1 = component1;
      this.b = b;
      this.c = c;
      this.d = d;
   }

   public int a() {
      return this.component1;
   }

   public int b() {
      return this.b;
   }

   public int c() {
      return this.c;
   }

   public int d() {
      return this.d;
   }
}
