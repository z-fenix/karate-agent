package io.karatelabs.ext.coverage;

import java.util.List;
import java.util.Map;

public record CoverageSummary(int c, int d, int e, double f, Map<String, TagCoverageStat> g, List<Warning> h) {


   public static CoverageSummary a(int var0, int var1, Map<String, TagCoverageStat> var2, List<Warning> var3) {
      int var4 = var0 - var1;
      double var5 = var0 > 0 ? var1 * 100.0 / var0 : 0.0;
      return new CoverageSummary(var0, var1, var4, var5, var2, var3);
   }

   public boolean isFullyCovered() {
      return this.c > 0 && this.d >= this.c;
   }

   public int warningCount() {
      return this.h != null ? this.h.size() : 0;
   }

   public int c() {
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

   public Map<String, TagCoverageStat> g() {
      return this.g;
   }

   public List<Warning> h() {
      return this.h;
   }
}
