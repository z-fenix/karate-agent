package io.karatelabs.ext.coverage.graph;

import io.karatelabs.ext.coverage.Action;
import java.util.LinkedHashMap;
import java.util.Map;

public record CoverageHit(String a, String b, HitOrigin c, Action d, MatchKind e, boolean f, Map<String, Object> g, Map<String, Object> h, String i) {

   public CoverageHit(String var1, String var2, HitOrigin var3, Action var4, MatchKind var5, boolean var6, Map<String, Object> var7, Map<String, Object> var8) {
      this(var1, var2, var3, var4, var5, var6, var7, var8, null);
   }


   public CoverageHit a(String var1) {
      return new CoverageHit(this.a, this.b, this.c, this.d, this.e, this.f, this.g, this.h, var1);
   }

   public CoverageHit b(String var1) {
      return new CoverageHit(this.a, var1, this.c, this.d, this.e, this.f, this.g, this.h, this.i);
   }

   public static CoverageHit a(String var0, String var1, Action var2) {
      return a(var0, var1, var2, MatchKind.EXACT);
   }

   public static CoverageHit a(String var0, String var1, Action var2, MatchKind var3) {
      return new CoverageHit(var0, var1, io.karatelabs.ext.coverage.graph.HitOrigin.OBSERVED, var2, var3 == null ? MatchKind.EXACT : var3, false, null, null);
   }

   public static CoverageHit a(String var0, String var1) {
      return new CoverageHit(var0, var1, io.karatelabs.ext.coverage.graph.HitOrigin.DECLARED, null, null, false, null, null);
   }

   public static CoverageHit a(String var0, String var1, Map<String, Object> var2) {
      return new CoverageHit(var0, var1, io.karatelabs.ext.coverage.graph.HitOrigin.DECLARED, null, null, true, var2, null);
   }

   public CoverageHit a(Map<String, Object> var1) {
      return new CoverageHit(this.a, this.b, this.c, this.d, this.e, this.f, this.g, var1, this.i);
   }

   public CoverageHit b(Map<String, Object> var1) {
      if (var1 != null && !var1.isEmpty()) {
         LinkedHashMap<java.lang.String, java.lang.Object> var2 = this.h == null ? new LinkedHashMap() : new LinkedHashMap<>(this.h);
         Object var3 = var2.get("dim");
         LinkedHashMap var4 = var3 instanceof Map ? new LinkedHashMap((Map)var3) : new LinkedHashMap();
         var4.putAll(var1);
         var2.put("dim", var4);
         return new CoverageHit(this.a, this.b, this.c, this.d, this.e, this.f, this.g, var2, this.i);
      } else {
         return this;
      }
   }
}
