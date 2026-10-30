package io.karatelabs.ext.coverage.dim;

import io.karatelabs.js.ObjectLike;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

public final class CoveringArrayGenerator {
   public static final int a = 2;

   private CoveringArrayGenerator() {
   }

   public static Map<String, Object> a(List<String> var0, List<List<Object>> var1, int var2, Set<String> var3, List<Map<String, Object>> var4) {
      int var5 = var0.size();
      int var6 = Math.max(1, Math.min(var2, var5));
      Set var7 = var3 == null ? Set.of() : var3;
      ArrayList var8 = new ArrayList();

      for (Map var10 : a(var0, var1)) {
         if (!var7.contains(a(var0, var10))) {
            var8.add(var10);
         }
      }

      var var23 = a(var5, var6);
      LinkedHashMap var24 = new LinkedHashMap();

      for (Map var12 : (Iterable<Map>) (Iterable<?>) (var8)) {
         for (List var14 : (Iterable<List>) (Iterable<?>) (var23)) {
            Map var15 = a(var0, var14, var12);
            var24.put(b(var0, var14, var15), var15);
         }
      }

      int var25 = var24.size();
      LinkedHashSet var26 = new LinkedHashSet();
      if (var4 != null) {
         for (Map var29 : var4) {
            for (List var16 : (Iterable<List>) (Iterable<?>) (var23)) {
               Map var17 = a(var0, var16, var29);
               if (var17 != null) {
                  String var18 = b(var0, var16, var17);
                  if (var24.containsKey(var18)) {
                     var26.add(var18);
                  }
               }
            }
         }
      }

      int var28 = var26.size();
      ArrayList var30 = new ArrayList();

      for (Map var34 : (Iterable<Map>) (Iterable<?>) (var8)) {
         LinkedHashSet var36 = new LinkedHashSet();

         for (List var19 : (Iterable<List>) (Iterable<?>) (var23)) {
            var36.add(b(var0, var19, a(var0, var19, var34)));
         }

         var30.add(var36);
      }

      LinkedHashSet var33 = new LinkedHashSet(var24.keySet());
      var33.removeAll(var26);
      ArrayList var35 = new ArrayList();

      while (!var33.isEmpty()) {
         int var37 = -1;
         int var40 = 0;

         for (int var41 = 0; var41 < var8.size(); var41++) {
            int var20 = 0;

            for (String var22 : (Iterable<String>) (Iterable<?>) ((Set)var30.get(var41))) {
               if (var33.contains(var22)) {
                  var20++;
               }
            }

            if (var20 > var40) {
               var40 = var20;
               var37 = var41;
            }
         }

         if (var37 < 0) {
            break;
         }

         var35.add((Map)var8.get(var37));
         var33.removeAll((Collection<?>)var30.get(var37));
      }

      LinkedHashMap var38 = new LinkedHashMap();
      var38.put("strength", var6);
      var38.put("axes", new ArrayList(var0));
      var38.put("rows", var35);
      var38.put("size", var35.size());
      var38.put("targets", var25);
      var38.put("seedCovered", var28);
      var38.put("residual", var33.size());
      var38.put("exhaustive", var6 >= var5);
      return var38;
   }

   public static int a(Object var0, String var1) {
      String var2 = io.karatelabs.ext.requirements.EvidencePolicy.b(var1);
      if (var0 instanceof Number var6) {
         return var6.intValue();
      } else {
         io.karatelabs.ext.common.JsCallback var3 = io.karatelabs.ext.common.JsCallback.a(var0);
         if (var3 != null) {
            return a(var3.a(var2, var1), a(var2));
         } else if (!(var0 instanceof ObjectLike) && !(var0 instanceof Map)) {
            return a(var2);
         } else {
            io.karatelabs.ext.common.JsCallback var4 = io.karatelabs.ext.common.JsCallback.b(var0, "resolve");
            if (var4 != null) {
               return a(var4.a(var2, var1), a(var2));
            } else {
               Object var5 = io.karatelabs.ext.common.JsCallback.a(var0, var2);
               if (var5 == null) {
                  var5 = io.karatelabs.ext.common.JsCallback.a(var0, "default");
               }

               return a(var5, a(var2));
            }
         }
      }
   }

   static int a(String var0) {
      return "high".equals(var0) ? 3 : 2;
   }

   private static int a(Object var0, int var1) {
      if (var0 instanceof Number var2) {
         return var2.intValue();
      } else {
         if (var0 != null && !String.valueOf(var0).isBlank()) {
            try {
               return Integer.parseInt(String.valueOf(var0).trim());
            } catch (NumberFormatException var3) {
            }
         }

         return var1;
      }
   }

   public static Map<String, Object> a(Map<String, Object> var0, String var1, Object var2) {
      LinkedHashMap var3 = new LinkedHashMap();

      for (Map var5 : io.karatelabs.ext.common.jsapi.CoverageGraphAccessor.a(var0, "hits")) {
         String var6 = String.valueOf(var5.get("item"));
         if ((var1 == null || var1.equals(var6)) && var5.get("attrs") instanceof Map var7 && var7.get("dim") instanceof Map var8 && !var8.isEmpty()) {
            LinkedHashMap var30 = new LinkedHashMap();
            var8.forEach((var1x, var2x) -> var30.put(String.valueOf(var1x), var2x));
            ((java.util.List)(var3.computeIfAbsent(var6, var0x -> new ArrayList<>()))).add(var30);
         }
      }

      ArrayList var24 = new ArrayList();

      for (Map var26 : io.karatelabs.ext.common.jsapi.CoverageGraphAccessor.a(var0, "items")) {
         String var27 = String.valueOf(var26.get("id"));
         if ((var1 == null || var1.equals(var27)) && var26.get("dimCross") instanceof List var28 && !var28.isEmpty()) {
            ArrayList var32 = new ArrayList();
            var28.forEach(var1x -> var32.add(String.valueOf(var1x)));
            String var10 = var26.get("dimCriticality") == null ? null : String.valueOf(var26.get("dimCriticality"));
            int var11 = a(var2, var10);
            Map var12 = a(var26);
            ArrayList var13 = new ArrayList();
            ArrayList var14 = new ArrayList();
            ArrayList var15 = new ArrayList();
            ArrayList var16 = new ArrayList();

            for (String var18 : (Iterable<String>) (Iterable<?>) (var32)) {
               Map var19 = (Map)var12.get(var18);
               if (var19 != null && var19.get("universe") instanceof List var20 && !var20.isEmpty()) {
                  var13.add(var18);
                  var14.add(new ArrayList(var20));
                  var15.add(var19);
               } else {
                  var16.add(var18);
               }
            }

            LinkedHashMap var33 = new LinkedHashMap();
            var33.put("id", var27);
            if (var10 != null) {
               var33.put("criticality", var10);
            }

            if (!var16.isEmpty()) {
               var33.put("axes", var32);
               var33.put("open", var16);
               var24.add(var33);
            } else {
               LinkedHashSet var34 = new LinkedHashSet();
               Object var37 = var26.get("dimCrossInfeasible");
               if (var37 instanceof List) {
                  for (Object var41 : (List)var37) {
                     if (var41 instanceof Map var22) {
                        LinkedHashMap var23 = new LinkedHashMap();
                        var22.forEach((var1x, var2x) -> var23.put(String.valueOf(var1x), var2x));
                        var34.add(a(var13, var23));
                     }
                  }
               }

               ArrayList var36 = new ArrayList();

               for (Map var42 : (Iterable<Map>) (Iterable<?>) (var3.getOrDefault(var27, List.of()))) {
                  Map var43 = a(var13, var14, var15, var42);
                  if (var43 != null) {
                     var36.add(var43);
                  }
               }

               Map<String, Object> summary = a(var13, var14, var11, var34, var36);
               var33.put("strength", summary.get("strength"));
               var33.put("axes", var13);
               var33.put("rows", summary.get("rows"));
               var33.put("size", summary.get("size"));
               var33.put("targets", summary.get("targets"));
               var33.put("seedCovered", summary.get("seedCovered"));
               var33.put("residual", summary.get("residual"));
               var33.put("exhaustive", summary.get("exhaustive"));
               var24.add(var33);
            }
         }
      }

      return Map.of("items", var24);
   }

   private static Map<String, Map<String, Object>> a(Map<String, Object> var0) {
      LinkedHashMap var1 = new LinkedHashMap();
      if (var0.get("responses") instanceof List var2 && !var2.isEmpty()) {
         LinkedHashMap var8 = new LinkedHashMap();
         var8.put("universe", new ArrayList(var2));
         var1.put("response", var8);
      }

      if (var0.get("dimAxes") instanceof Map var7) {
         for (Entry var4 : (Iterable<Entry>) (Iterable<?>) (var7.entrySet())) {
            if (var4.getValue() instanceof Map var5 && var5.get("universe") instanceof List) {
               LinkedHashMap var11 = new LinkedHashMap(var5);
               var1.put(String.valueOf(var4.getKey()), var11);
            }
         }
      }

      return var1;
   }

   private static Map<String, Object> a(List<String> var0, List<List<Object>> var1, List<Map<String, Object>> var2, Map<String, Object> var3) {
      LinkedHashMap var4 = new LinkedHashMap();

      for (int var5 = 0; var5 < var0.size(); var5++) {
         String var6 = (String)var0.get(var5);
         if (!var3.containsKey(var6)) {
            return null;
         }

         Object var7 = var3.get(var6);
         Map var8 = (Map)var2.get(var5);
         if ("number".equals(var8.get("type")) && var8.get("boundaries") instanceof List var9) {
            String var11 = io.karatelabs.ext.coverage.dim.DimensionCoverageProjector.a(var7, var9, (List<?>)var1.get(var5));
            if (var11 == null) {
               return null;
            }

            var4.put(var6, var11);
         } else {
            var4.put(var6, var7);
         }
      }

      return var4;
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

   private static List<List<Integer>> a(int var0, int var1) {
      ArrayList var2 = new ArrayList();
      a(0, var0, var1, new ArrayList<>(), var2);
      return var2;
   }

   private static void a(int var0, int var1, int var2, List<Integer> var3, List<List<Integer>> var4) {
      if (var3.size() == var2) {
         var4.add(new ArrayList(var3));
      } else {
         for (int var5 = var0; var5 < var1; var5++) {
            var3.add(var5);
            a(var5 + 1, var1, var2, var3, var4);
            var3.remove(var3.size() - 1);
         }
      }
   }

   private static Map<String, Object> a(List<String> var0, List<Integer> var1, Map<String, Object> var2) {
      LinkedHashMap var3 = new LinkedHashMap();

      for (int var5 : var1) {
         String var6 = (String)var0.get(var5);
         if (!var2.containsKey(var6)) {
            return null;
         }

         var3.put(var6, var2.get(var6));
      }

      return var3;
   }

   private static String b(List<String> var0, List<Integer> var1, Map<String, Object> var2) {
      StringBuilder var3 = new StringBuilder();

      for (int var5 : var1) {
         String var6 = (String)var0.get(var5);
         var3.append(var6).append('=').append(String.valueOf(var2.get(var6))).append(' ');
      }

      return var3.toString();
   }

   public static String a(List<String> var0, Map<String, Object> var1) {
      StringBuilder var2 = new StringBuilder();

      for (String var4 : var0) {
         var2.append(var4).append('=').append(String.valueOf(var1.get(var4))).append(' ');
      }

      return var2.toString();
   }
}
