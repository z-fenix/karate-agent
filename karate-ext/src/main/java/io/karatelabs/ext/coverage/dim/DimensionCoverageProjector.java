package io.karatelabs.ext.coverage.dim;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

public final class DimensionCoverageProjector {
   private DimensionCoverageProjector() {
   }

   public static Map<String, Object> a(Map<String, Object> var0, String var1) {
      LinkedHashMap<String, Map<String, Map<String, Object>>> var2 = new LinkedHashMap<>();
      LinkedHashMap<String, List<String>> var3 = new LinkedHashMap<>();
      LinkedHashMap<String, Set<String>> var4 = new LinkedHashMap<>();
      LinkedHashMap<String, Set<String>> var5 = new LinkedHashMap<>();

      for (Map var7 : io.karatelabs.ext.common.jsapi.CoverageGraphAccessor.a(var0, "items")) {
         String var8 = String.valueOf(var7.get("id"));
         LinkedHashSet<String> var9 = new LinkedHashSet<>();
         if (var7.get("dimIgnore") instanceof List var10) {
            var10.forEach(var1x -> var9.add(String.valueOf(var1x)));
            var5.put(var8, var9);
         }

         if (var7.get("dimCross") instanceof List var29 && !var29.isEmpty()) {
            ArrayList<String> var37 = new ArrayList<>();
            var29.forEach(var1x -> var37.add(String.valueOf(var1x)));
            var3.put(var8, var37);
         }

         if (var7.get("dimCrossInfeasible") instanceof List var30 && !var30.isEmpty()) {
            LinkedHashSet<String> var39 = new LinkedHashSet<>();

            for (Object var13 : var30) {
               if (var13 instanceof Map var14 && var7.get("dimCross") instanceof List var15) {
                  ArrayList<String> var63 = new ArrayList<>();
                  var15.forEach(var1x -> var63.add(String.valueOf(var1x)));
                  var39.add(a(var63, var14));
               }
            }

            var4.put(var8, var39);
         }

         LinkedHashMap<String, Map<String, Object>> var31 = new LinkedHashMap<>();
         if (var7.get("responses") instanceof List var40) {
            LinkedHashMap<String, Object> var46 = new LinkedHashMap<>();
            var46.put("universe", new ArrayList(var40));
            var46.put("source", "spec");
            var31.put("response", var46);
         }

         if (var7.get("dimAxes") instanceof Map var41) {
            for (Entry var51 : (Iterable<Entry>) (Iterable<?>) (var41.entrySet())) {
               if (!var9.contains(String.valueOf(var51.getKey())) && var51.getValue() instanceof Map var56 && var56.get("universe") instanceof List) {
                  LinkedHashMap<String, Object> var60 = new LinkedHashMap(var56);
                  var31.put(String.valueOf(var51.getKey()), var60);
               }
            }
         }

         if (!var31.isEmpty()) {
            var2.put(var8, var31);
         }
      }

      LinkedHashMap<String, Map<String, Set<Object>>> var21 = new LinkedHashMap<>();
      LinkedHashMap<String, List<Map<String, Object>>> var22 = new LinkedHashMap<>();

      for (Map var25 : io.karatelabs.ext.common.jsapi.CoverageGraphAccessor.a(var0, "hits")) {
         String var32 = String.valueOf(var25.get("item"));
         if ((var1 == null || var1.equals(var32)) && var25.get("attrs") instanceof Map var42 && var42.get("dim") instanceof Map var49 && !var49.isEmpty()) {
            Set<String> var54 = var5.getOrDefault(var32, Set.of());
            Map<String, Set<Object>> var57 = var21.computeIfAbsent(var32, var0x -> new LinkedHashMap<>());
            LinkedHashMap<String, Object> var61 = new LinkedHashMap<>();

            for (Entry var17 : (Iterable<Entry>) (Iterable<?>) (var49.entrySet())) {
               String var18 = String.valueOf(var17.getKey());
               if (!var54.contains(var18)) {
                  Object var19 = var17.getValue();
                  var57.computeIfAbsent(var18, var0x -> new LinkedHashSet<>()).add(var19);
                  var61.put(var18, var19);
               }
            }

            if (!var61.isEmpty()) {
               var22.computeIfAbsent(var32, var0x -> new ArrayList<>()).add(var61);
            }
         }
      }

      LinkedHashSet<String> var24 = new LinkedHashSet<>(var21.keySet());

      for (Entry var33 : (Iterable<Entry>) (Iterable<?>) (var2.entrySet())) {
         boolean var43 = ((Map)var33.getValue()).keySet().stream().anyMatch(var0x -> !"response".equals(var0x));
         if (var43 && (var1 == null || var1.equals(var33.getKey()))) {
            var24.add((String)var33.getKey());
         }
      }

      for (String var34 : (Iterable<String>) (Iterable<?>) (var3.keySet())) {
         if (var1 == null || var1.equals(var34)) {
            var24.add(var34);
         }
      }

      ArrayList<Map<String, Object>> var28 = new ArrayList<>();

      for (String var44 : (Iterable<String>) (Iterable<?>) (var24)) {
         Map<String, Set<Object>> var50 = var21.getOrDefault(var44, Map.of());
         Map<String, Map<String, Object>> var55 = var2.getOrDefault(var44, Map.of());
         LinkedHashSet<String> var58 = new LinkedHashSet<>(var50.keySet());
         var55.forEach((var1x, var2x) -> {
            if (!"response".equals(var1x)) {
               var58.add(var1x);
            }
         });
         LinkedHashMap var62 = new LinkedHashMap();

         for (String var67 : (Iterable<String>) (Iterable<?>) (var58)) {
            ArrayList<Object> var69 = new ArrayList<>(var50.getOrDefault(var67, Set.of()));
            LinkedHashMap var71 = new LinkedHashMap();
            var71.put("seen", var69);
            Map var20 = (Map)var55.get(var67);
            if (var20 != null) {
               a(var71, var69, var20);
            } else {
               var71.put("closed", false);
               var71.put("coveredProportion", -1.0);
            }

            var62.put(var67, var71);
         }

         List<Map<String, Object>> var66 = var22.getOrDefault(var44, List.of());
         LinkedHashMap var68 = new LinkedHashMap();
         var68.put("id", var44);
         var68.put("axes", var62);
         var68.put("points", var66.size());
         var68.put("coordinates", var66);
         Map var70 = null;
         List<String> var72 = var3.get(var44);
         if (var72 != null) {
            var70 = a(var72, var55, var66, var4.getOrDefault(var44, Set.of()));
            var68.put("cross", var70);
         }

         var68.put("rollup", a(var62, var70));
         var28.add(var68);
      }

      return Map.of("items", var28);
   }

   public static void a(Map<String, Object> var0) {
      if (var0 != null && var0.get("sources") instanceof List var1 && !var1.isEmpty()) {
         var var10 = c(var0);
         var var3 = b(var0);
         if (!var10.isEmpty() || !var3.isEmpty()) {
            for (Object var5 : var1) {
               if (var5 instanceof Map var6) {
                  String var7 = String.valueOf(var6.get("namespace"));
                  Map var8 = (Map)var10.get(var7);
                  if (var8 != null) {
                     var6.put("combos", var8);
                  }

                  Map var9 = (Map)var3.get(var7);
                  if (var9 != null) {
                     var6.put("inputs", var9);
                  }
               }
            }
         }
      }
   }

   static Map<String, Map<String, Object>> b(Map<String, Object> var0) {
      LinkedHashMap<String, String> var1 = new LinkedHashMap<>();

      for (Map var3 : io.karatelabs.ext.common.jsapi.CoverageGraphAccessor.a(var0, "items")) {
         var1.put(String.valueOf(var3.get("id")), String.valueOf(var3.get("source")));
      }

      LinkedHashMap<String, Map<String, Object>> var19 = new LinkedHashMap<>();
      if (a(var0, (String)null).get("items") instanceof List var4) {
         for (Object var6 : var4) {
            if (var6 instanceof Map var7 && var7.get("axes") instanceof Map var8) {
               int var21 = 0;
               int var10 = 0;
               int var11 = 0;

               for (Entry var13 : (Iterable<Entry>) (Iterable<?>) (var8.entrySet())) {
                  if (!"response".equals(String.valueOf(var13.getKey()))
                     && var13.getValue() instanceof Map var14
                     && Boolean.TRUE.equals(var14.get("closed"))
                     && var14.get("universe") instanceof List var15) {
                     int var25 = var14.get("gaps") instanceof List var17 ? var17.size() : 0;
                     var21 += Math.max(0, var15.size() - var25);
                     var10 += var15.size();
                     var11++;
                  }
               }

               if (var10 != 0) {
                  String var22 = String.valueOf(var7.get("id"));
                  Map<String, Object> var23 = var19.computeIfAbsent(var1.getOrDefault(var22, ""), var0x -> {
                     LinkedHashMap<String, Object> var1x = new LinkedHashMap<>();
                     var1x.put("covered", 0);
                     var1x.put("total", 0);
                     var1x.put("axes", 0);
                     var1x.put("items", 0);
                     return var1x;
                  });
                  a(var23, "covered", var21);
                  a(var23, "total", var10);
                  a(var23, "axes", var11);
                  a(var23, "items", 1);
               }
            }
         }
      }

      return var19;
   }

   static Map<String, Map<String, Object>> c(Map<String, Object> var0) {
      LinkedHashMap<String, String> var1 = new LinkedHashMap<>();

      for (Map var3 : io.karatelabs.ext.common.jsapi.CoverageGraphAccessor.a(var0, "items")) {
         var1.put(String.valueOf(var3.get("id")), String.valueOf(var3.get("source")));
      }

      LinkedHashMap<String, Integer> var18 = new LinkedHashMap<>();
      Object var19 = io.karatelabs.ext.coverage.dim.CoveringArrayGenerator.a(var0, null, null).get("items");
      if (var19 instanceof List) {
         for (Object var6 : (List)var19) {
            if (var6 instanceof Map var7 && var7.get("rows") instanceof List var8) {
               var18.put(String.valueOf(var7.get("id")), var8.size());
            }
         }
      }

      LinkedHashMap<String, Map<String, Object>> var20 = new LinkedHashMap<>();
      Object var21 = a(var0, (String)null).get("items");
      if (var21 instanceof List) {
         for (Object var24 : (List)var21) {
            if (var24 instanceof Map var25
               && var25.get("cross") instanceof Map var10
               && var10.get("coveredProportion") instanceof Number var26
               && !(var26.doubleValue() < 0.0)) {
               String var27 = String.valueOf(var25.get("id"));
               int var13 = b(var10.get("required"));
               int var14 = b(var10.get("covered"));
               int var15 = var10.get("gaps") instanceof List var16 ? var16.size() : Math.max(0, var13 - var14);
               Map<String, Object> var28 = var20.computeIfAbsent(var1.getOrDefault(var27, ""), var0x -> {
                  LinkedHashMap<String, Object> var1x = new LinkedHashMap<>();
                  var1x.put("covered", 0);
                  var1x.put("required", 0);
                  var1x.put("untested", 0);
                  var1x.put("suggested", 0);
                  var1x.put("items", 0);
                  return var1x;
               });
               a(var28, "covered", var14);
               a(var28, "required", var13);
               a(var28, "untested", var15);
               a(var28, "suggested", var18.getOrDefault(var27, 0).intValue());
               a(var28, "items", 1);
            }
         }
      }

      return var20;
   }

   private static void a(Map<String, Object> var0, String var1, int var2) {
      var0.put(var1, b(var0.get(var1)) + var2);
   }

   private static int b(Object var0) {
      return var0 instanceof Number var1 ? var1.intValue() : 0;
   }

   private static void a(Map<String, Object> var0, List<Object> var1, Map<String, Object> var2) {
      List var3 = (List)var2.get("universe");
      String var4 = var2.get("source") == null ? null : String.valueOf(var2.get("source"));
      LinkedHashSet var5 = new LinkedHashSet();
      String var6 = var2.get("type") == null ? null : String.valueOf(var2.get("type"));
      boolean var7 = "cardinality".equals(var6);
      ArrayList var8 = new ArrayList();

      for (Object var10 : var1) {
         String var11 = String.valueOf(var10);
         if (("absent".equals(var11) || "null".equals(var11)) && var3.contains(var11)) {
            var5.add(var11);
         } else {
            var8.add(var10);
         }
      }

      if (("number".equals(var6) || var7) && var2.get("boundaries") instanceof List var15) {
         for (Object var25 : var8) {
            String var28 = a(var7 ? a(var25) : var25, var15, var3);
            if (var28 != null) {
               var5.add(var28);
            }
         }
      } else if ("string".equals(var6)) {
         for (Object var23 : var8) {
            String var12 = a(var23, var2, var3);
            if (var12 != null) {
               var5.add(var12);
            }
         }
      } else {
         for (Object var24 : var8) {
            var5.add(String.valueOf(var24));
         }
      }

      ArrayList var16 = new ArrayList();

      for (Object var26 : var3) {
         if (!var5.contains(String.valueOf(var26))) {
            var16.add(var26);
         }
      }

      int var22 = var3.size() - var16.size();
      double var27 = var3.isEmpty() ? 0.0 : (double)var22 / var3.size();
      var0.put("closed", true);
      if (var4 != null) {
         var0.put("source", var4);
      }

      if (var2.get("type") != null) {
         var0.put("kind", String.valueOf(var2.get("type")));
      }

      if (var2.get("examples") instanceof Map var13 && !var13.isEmpty()) {
         LinkedHashMap var29 = new LinkedHashMap();
         var13.forEach((var1x, var2x) -> var29.put(String.valueOf(var1x), var2x));
         var0.put("examples", var29);
      }

      var0.put("universe", new ArrayList(var3));
      var0.put("gaps", var16);
      var0.put("coveredProportion", var27);
   }

   static Object a(Object var0) {
      if (var0 instanceof Collection var4) {
         return var4.size();
      } else if (var0 instanceof Map var3) {
         return var3.size();
      } else if (var0 instanceof CharSequence var2) {
         return var2.length();
      } else {
         return var0 instanceof Object[] var1 ? var1.length : null;
      }
   }

   static String a(Object var0, Map<String, Object> var1, List<?> var2) {
      if (a(var0) instanceof Integer var3) {
         Integer var8 = var1.get("minLength") instanceof Number var5 ? var5.intValue() : null;
         Integer var9 = var1.get("maxLength") instanceof Number var10 ? var10.intValue() : null;
         String var11;
         if (var3 == 0) {
            var11 = "empty";
         } else if (var8 != null && var3 < var8) {
            var11 = "< minLength";
         } else if (var9 != null && var3 == var9) {
            var11 = "= maxLength";
         } else if (var9 != null && var3 > var9) {
            var11 = "> maxLength";
         } else {
            var11 = "valid";
         }

         return var2.contains(var11) ? var11 : null;
      } else {
         return null;
      }
   }

   static String a(Object var0, List<?> var1, List<?> var2) {
      if (!(var0 instanceof Number var3)) {
         return null;
      } else {
         double var4 = var3.doubleValue();
         int var6 = var1.size();
         int var7 = 2 * var6;

         for (int var8 = 0; var8 < var6; var8++) {
            double var9 = ((Number)var1.get(var8)).doubleValue();
            if (var4 < var9) {
               var7 = 2 * var8;
               break;
            }

            if (var4 == var9) {
               var7 = 2 * var8 + 1;
               break;
            }
         }

         return var7 < var2.size() ? String.valueOf(var2.get(var7)) : null;
      }
   }

   private static Map<String, Object> a(List<String> var0, Map<String, Map<String, Object>> var1, List<Map<String, Object>> var2, Set<String> var3) {
      LinkedHashMap var4 = new LinkedHashMap();
      var4.put("axes", new ArrayList(var0));
      ArrayList var5 = new ArrayList();
      ArrayList var6 = new ArrayList();
      ArrayList var7 = new ArrayList();
      ArrayList var8 = new ArrayList();

      for (String var10 : var0) {
         Map var11 = (Map)var1.get(var10);
         if (var11 != null && var11.get("universe") instanceof List var12 && !var12.isEmpty()) {
            var5.add(var10);
            var6.add(new ArrayList(var12));
            var7.add(var11);
         } else {
            var8.add(var10);
         }
      }

      if (!var8.isEmpty()) {
         var4.put("open", var8);
         var4.put("coveredProportion", -1.0);
         return var4;
      } else {
         LinkedHashMap var15 = new LinkedHashMap();
         int var16 = 0;

         for (Map var19 : (Iterable<Map>) (Iterable<?>) (a(var5, var6))) {
            String var22 = a(var5, var19);
            if (var3.contains(var22)) {
               var16++;
            } else {
               var15.put(var22, var19);
            }
         }

         LinkedHashSet var18 = new LinkedHashSet();

         for (Map var23 : var2) {
            String var14 = a(var5, var7, var6, var23);
            if (var14 != null && var15.containsKey(var14)) {
               var18.add(var14);
            }
         }

         ArrayList var21 = new ArrayList();

         for (Entry var26 : (Iterable<Entry>) (Iterable<?>) (var15.entrySet())) {
            if (!var18.contains(var26.getKey())) {
               var21.add((Map)var26.getValue());
            }
         }

         int var25 = var15.size();
         var4.put("required", var25);
         var4.put("covered", var18.size());
         var4.put("coveredProportion", var25 == 0 ? 0.0 : (double)var18.size() / var25);
         var4.put("gaps", var21);
         var4.put("pruned", var16);
         return var4;
      }
   }

   private static Map<String, Object> a(Map<String, Object> var0, Map<String, Object> var1) {
      LinkedHashMap var2 = new LinkedHashMap();
      if (var1 != null && var1.get("coveredProportion") instanceof Number var3 && var3.doubleValue() >= 0.0) {
         var2.put("kind", "cross");
         var2.put("coveredProportion", var3.doubleValue());
         var2.put("gaps", var1.get("gaps") instanceof List var14 ? var14.size() : 0);
         return var2;
      } else {
         double var13 = 0.0;
         int var5 = 0;
         int var6 = 0;

         for (Object var8 : var0.values()) {
            if (var8 instanceof Map var9 && Boolean.TRUE.equals(var9.get("closed")) && var9.get("coveredProportion") instanceof Number var10) {
               var13 += var10.doubleValue();
               var5++;
               if (var9.get("gaps") instanceof List var16) {
                  var6 += var16.size();
               }
            }
         }

         var2.put("kind", var5 == 0 ? "open" : "marginal");
         var2.put("coveredProportion", var5 == 0 ? -1.0 : var13 / var5);
         var2.put("gaps", var6);
         return var2;
      }
   }

   private static List<Map<String, Object>> a(List<String> var0, List<List<Object>> var1) {
      ArrayList var2 = new ArrayList();
      var2.add(new LinkedHashMap());

      for (int var3 = 0; var3 < var0.size(); var3++) {
         String var4 = (String)var0.get(var3);
         ArrayList var5 = new ArrayList();

         for (Map var7 : (Iterable<Map>) (Iterable<?>) (var2)) {
            for (Object var9 : (List)var1.get(var3)) {
               LinkedHashMap var10 = new LinkedHashMap(var7);
               var10.put(var4, var9);
               var5.add(var10);
            }
         }

         var2 = var5;
      }

      return var2;
   }

   private static String a(List<String> var0, Map<String, Object> var1) {
      StringBuilder var2 = new StringBuilder();

      for (String var4 : var0) {
         var2.append(var4).append('=').append(String.valueOf(var1.get(var4))).append(' ');
      }

      return var2.toString();
   }

   private static String a(List<String> var0, List<Map<String, Object>> var1, List<List<Object>> var2, Map<String, Object> var3) {
      StringBuilder var4 = new StringBuilder();

      for (int var5 = 0; var5 < var0.size(); var5++) {
         String var6 = (String)var0.get(var5);
         if (!var3.containsKey(var6)) {
            return null;
         }

         Object var7 = var3.get(var6);
         Map var8 = (Map)var1.get(var5);
         String var9;
         if ("number".equals(var8.get("type")) && var8.get("boundaries") instanceof List var10) {
            var9 = a(var7, var10, (List<?>)var2.get(var5));
            if (var9 == null) {
               return null;
            }
         } else {
            var9 = String.valueOf(var7);
         }

         var4.append(var6).append('=').append(var9).append(' ');
      }

      return var4.toString();
   }
}
