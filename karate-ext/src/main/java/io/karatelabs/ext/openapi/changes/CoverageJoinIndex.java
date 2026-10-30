package io.karatelabs.ext.openapi.changes;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class CoverageJoinIndex {
   private static final String a = "openapi";
   private final Map<String, Object> b = new LinkedHashMap<>();
   private final List<io.karatelabs.ext.openapi.changes.CoverageJoinIndex.CoverageSide> c = new ArrayList<>();
   private final Map<List<String>, Map<String, Object>> d = new HashMap<>();

   CoverageJoinIndex(String var1, String var2, Path var3, String var4, String var5) {
      io.karatelabs.ext.openapi.changes.CoverageJoinIndex.CoverageSide var6 = this.a("baseline", var2, var3, var4 != null ? var4 : var5);
      io.karatelabs.ext.openapi.changes.CoverageJoinIndex.CoverageSide var7 = this.a("current", var1, var3, var5 != null ? var5 : var4);

      for (io.karatelabs.ext.openapi.changes.CoverageJoinIndex.CoverageSide var11 : new io.karatelabs.ext.openapi.changes.CoverageJoinIndex.CoverageSide[]{var7, var6}) {
         if (var11 != null) {
            this.c.add(var11);
         }
      }
   }

   Map<String, Object> a() {
      return this.b;
   }

   private io.karatelabs.ext.openapi.changes.CoverageJoinIndex.CoverageSide a(String var1, String var2, Path var3, String var4) {
      LinkedHashMap<java.lang.String, java.lang.Object> var5 = new LinkedHashMap();
      this.b.put(var1, var5);
      io.karatelabs.ext.common.jsapi.CoverageGraphAccessor.CoverageGraphLoadResult var6 = null;
      if (var2 != null) {
         Path var7 = var3 == null ? Path.of(var2) : var3.resolve(var2);
         var6 = io.karatelabs.ext.common.jsapi.CoverageGraphAccessor.a(var7.resolve("ext/coverage/coverage-data.json"));
      }

      String var18 = var6 == null ? null : (var6.a() ? io.karatelabs.ext.common.jsapi.CoverageGraphAccessor.a(var6.c(), "items", "tests") : var6.d());
      boolean var8 = var6 != null && var6.a() && var18 == null;
      var5.put("read", var8);
      var5.put("reportDir", var2);
      if (var18 != null) {
         var5.put("unreadable", var18);
      }

      if (!var8) {
         return null;
      } else {
         LinkedHashSet<java.lang.String> var9 = new LinkedHashSet();
         HashMap<String, List<String>> var10 = new HashMap<>();

         for (Map var12 : io.karatelabs.ext.common.jsapi.CoverageGraphAccessor.a(var6.c(), "items")) {
            String var13 = String.valueOf(var12.get("id"));
            int var14 = var13.indexOf(58);
            String var15 = var14 < 0 ? "" : var13.substring(0, var14);
            if (var15.equals("openapi") || var15.startsWith("openapi.")) {
               var9.add(var15);
            }

            for (Object var17 : io.karatelabs.ext.common.jsapi.CoverageGraphAccessor.a(var12)) {
               var10.computeIfAbsent(String.valueOf(var17), var0 -> new ArrayList<>()).add(var13);
            }
         }

         String var19 = "openapi";
         if (var9.stream().anyMatch(var0 -> !var0.equals("openapi"))) {
            boolean var20 = var4 != null && var9.contains(var4);
            var5.put("namespace", var20 ? var4 : "unresolved");
            if (!var20) {
               return null;
            }

            var19 = var4;
         }

         return new io.karatelabs.ext.openapi.changes.CoverageJoinIndex.CoverageSide(var1, var19, var6.c(), var10);
      }
   }

   Map<String, Object> a(Map<?, ?> var1) {
      if (this.c.isEmpty()) {
         return Map.of("state", "notjoined");
      } else {
         ArrayList<java.lang.String> var2 = new ArrayList();

         for (Object var4 : var1.values()) {
            if (!var2.contains(String.valueOf(var4))) {
               var2.add(String.valueOf(var4));
            }
         }

         return this.d.computeIfAbsent(var2, this::a);
      }
   }

   private Map<String, Object> a(List<String> var1) {
      ArrayList<java.lang.String> var2 = new ArrayList();

      for (String var4 : var1) {
         for (io.karatelabs.ext.openapi.changes.CoverageJoinIndex.CoverageSide var6 : this.c) {
            if (!var2.contains(var6.b() + ":" + var4)) {
               var2.add(var6.b() + ":" + var4);
            }
         }
      }

      LinkedHashMap<java.lang.String, java.util.LinkedHashSet> var15 = new LinkedHashMap();
      LinkedHashMap<String, Map<String, Object>> var16 = new LinkedHashMap<>();
      HashMap<java.lang.Object, java.util.Set<java.lang.String>> var17 = new HashMap();

      for (io.karatelabs.ext.openapi.changes.CoverageJoinIndex.CoverageSide var7 : this.c) {
         for (String var9 : var1) {
            String var10 = var7.b() + ":" + var9;

            for (String var12 : var7.d().getOrDefault(var10, List.of())) {
               var15.computeIfAbsent(var12, var0 -> new LinkedHashSet<>()).add(var7.a());
            }

            var var24 = io.karatelabs.ext.common.jsapi.CoverageGraphAccessor.b(var7.c(), var10);

            for (Map var13 : (Iterable<Map>) (Iterable<?>) (var24 == null ? List.of() : var24)) {
               String var14 = String.valueOf(var13.get("id"));
               var16.computeIfAbsent(var14, var2x -> {
                  LinkedHashMap<java.lang.String, java.lang.Object> var3 = new LinkedHashMap();
                  var3.put("id", var14);
                  var3.put("name", var13.get("name"));
                  var3.put("file", var13.get("feature"));
                  var3.put("line", var13.get("line"));
                  return var3;
               });
               var17.computeIfAbsent(var14, var0 -> new LinkedHashSet<>()).add(var7.a());
            }
         }
      }

      ArrayList<java.util.LinkedHashMap> var19 = new ArrayList();
      var15.forEach((var1x, var2x) -> {
         LinkedHashMap<java.lang.String, java.lang.Object> var3 = new LinkedHashMap();
         var3.put("id", var1x);
         var3.put("side", a((Set<String>)var2x));
         var19.add(var3);
      });
      ArrayList<java.lang.Object> var20 = new ArrayList();
      var16.forEach((var2x, var3) -> {
         var3.put("side", a((Set<String>)var17.get(var2x)));
         var20.add(var3);
      });
      LinkedHashMap<java.lang.String, java.lang.Object> var21 = new LinkedHashMap();
      boolean var22 = !var19.isEmpty();
      boolean var23 = !var20.isEmpty();
      var21.put("state", var22 && var23 ? "mapped" : (!var22 && !var23 ? "notmapped" : "partmapped"));
      var21.put("ids", var2);
      var21.put("requirements", var19);
      var21.put("tests", var20);
      return var21;
   }

   private static String a(Set<String> var0) {
      return var0.size() > 1 ? "both" : (String)var0.iterator().next();
   }

   private record CoverageSide(String a, String b, Map<String, Object> c, Map<String, List<String>> d) {

   }
}
