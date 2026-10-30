package io.karatelabs.ext.coverage;

public record TagCoverageStat(String c, int d, int e, double f) {

   public TagCoverageStat(String c, int d, int e, double f) {
      this.c = c;
      this.d = d;
      this.e = e;
      this.f = f;
   }

   public static TagCoverageStat create(String var0, int var1, int var2) {
      double var3 = var1 > 0 ? var2 * 100.0 / var1 : 0.0;
      return new TagCoverageStat(var0, var1, var2, var3);
   }

   public boolean isFullyCovered() {
      return this.d > 0 && this.e >= this.d;
   }

   public boolean hasNoCoveredItems() {
      return this.e == 0;
   }

   public String c() {
      return this.c;
   }

   public int d() {
      return this.d;
   }

   public int e() {
      return this.e;
   }

   public double f() {
      return this.f;
   }
}
