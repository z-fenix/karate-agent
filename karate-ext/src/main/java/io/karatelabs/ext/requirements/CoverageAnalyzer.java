package io.karatelabs.ext.requirements;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class CoverageAnalyzer {
   private CoverageAnalyzer() {
   }

   public static io.karatelabs.ext.requirements.CoverageAnalyzer.RequirementCoverageAnalysis a(Map<String, Object> var0) {
      var var1 = io.karatelabs.ext.common.jsapi.CoverageGraphAccessor.a(var0, "items");
      LinkedHashSet var2 = new LinkedHashSet();

      for (Map var4 : io.karatelabs.ext.common.jsapi.CoverageGraphAccessor.a(var0, "tests")) {
         String var5 = String.valueOf(var4.get("status"));
         if ("PASSED".equals(var5) || "FAILED".equals(var5)) {
            var2.add(String.valueOf(var4.get("id")));
         }
      }

      LinkedHashSet var30 = new LinkedHashSet();

      for (Map var33 : io.karatelabs.ext.common.jsapi.CoverageGraphAccessor.a(var0, "hits")) {
         if ("OBSERVED".equals(var33.get("kind")) && var2.contains(String.valueOf(var33.get("test")))) {
            var30.add(String.valueOf(var33.get("item")));
         }
      }

      LinkedHashSet var32 = new LinkedHashSet();

      for (Map var6 : io.karatelabs.ext.common.jsapi.CoverageGraphAccessor.a(var0, "hits")) {
         if ("DECLARED".equals(var6.get("kind"))) {
            var32.add(String.valueOf(var6.get("item")));
         }
      }

      LinkedHashMap var35 = new LinkedHashMap();
      LinkedHashMap var36 = new LinkedHashMap();

      for (Map var8 : (Iterable<Map>) (Iterable<?>) (var1)) {
         String var9 = String.valueOf(var8.get("id"));
         var36.put(var9, var8);
         var35.put(var9, String.valueOf(var8.get("status")));
      }

      LinkedHashMap var37 = new LinkedHashMap();
      LinkedHashSet var38 = new LinkedHashSet();
      ArrayList var39 = new ArrayList();
      ArrayList var10 = new ArrayList();
      ArrayList var11 = new ArrayList();
      boolean var12 = false;

      for (Map var14 : (Iterable<Map>) (Iterable<?>) (var1)) {
         if ("req".equals(var14.get("source")) && !"acceptance-criterion".equals(var14.get("kind"))) {
            String var15 = String.valueOf(var14.get("id"));
            var var16 = a(var14.get("real"));
            if (!var16.isEmpty()) {
               var12 = true;
               var38.addAll(var16);
               ArrayList var17 = new ArrayList();
               boolean var18 = false;

               for (String var20 : (Iterable<String>) (Iterable<?>) (var16)) {
                  if (var30.contains(var20)) {
                     var17.add(var20);
                     if ("FAILING".equals(var35.get(var20))) {
                        var18 = true;
                     }
                  }
               }

               boolean var45 = !var17.isEmpty();
               boolean var46 = var17.size() == var16.size();
               ArrayList var21 = new ArrayList();

               for (String var23 : (Iterable<String>) (Iterable<?>) (var16)) {
                  if (!var36.containsKey(var23)) {
                     var21.add(var23);
                  }
               }

               boolean var47 = !var21.isEmpty();
               boolean var48 = a(var15, var32);
               boolean var24 = var14.get("decomposes") instanceof List var25 && !var25.isEmpty();
               String var49 = (String)var35.get(var15);
               String var50 = var49;
               boolean var27 = false;
               if ("NOTCOVERED".equals(var49) && var45 && !var24) {
                  var50 = var18 ? "FAILING" : "COVERED";
                  var27 = true;
               }

               String var28;
               if (var45) {
                  if (var48) {
                     var28 = var46 ? "exercised" : "partexercised";
                  } else {
                     var28 = "incidental";
                  }
               } else if (var48) {
                  var28 = var47 ? "notresolved" : "notexercised";
               } else {
                  var28 = null;
               }

               var37.put(var15, new io.karatelabs.ext.requirements.CoverageAnalyzer.RequirementCoverage(var28, var50, var27, var16, var17));
               String var29 = var15.startsWith("req:") ? var15.substring(4) : var15;
               if ("notexercised".equals(var28)) {
                  var39.add(a(var29, var14.get("name"), var16, var49));
               } else if ("notresolved".equals(var28)) {
                  var10.add(a(var29, var14.get("name"), var21, var49));
               } else if ("incidental".equals(var28)) {
                  var11.add(a(var29, var14.get("name"), var16, var50));
               }
            }
         }
      }

      ArrayList var40 = new ArrayList();
      if (var12) {
         for (Map var42 : (Iterable<Map>) (Iterable<?>) (var1)) {
            String var43 = String.valueOf(var42.get("id"));
            if (!"req".equals(var42.get("source")) && var30.contains(var43) && !var38.contains(var43)) {
               LinkedHashMap var44 = new LinkedHashMap();
               var44.put("id", var43);
               var44.put("source", var42.get("source"));
               var44.put("name", var42.get("name"));
               var44.put("status", var42.get("status"));
               var40.add(var44);
            }
         }
      }

      return new io.karatelabs.ext.requirements.CoverageAnalyzer.RequirementCoverageAnalysis(var37, var40, var39, var10, var11, var12);
   }

   private static boolean a(String var0, Set<String> var1) {
      if (var1.contains(var0)) {
         return true;
      } else {
         String var2 = var0 + "/";

         for (String var4 : var1) {
            if (var4.startsWith(var2)) {
               return true;
            }
         }

         return false;
      }
   }

   private static Map<String, Object> a(String var0, Object var1, List<String> var2, String var3) {
      LinkedHashMap var4 = new LinkedHashMap();
      var4.put("reqId", var0);
      var4.put("title", var1);
      var4.put("real", var2);
      var4.put("status", var3);
      return var4;
   }

   private static List<String> a(Object var0) {
      if (var0 instanceof List var1) {
         ArrayList var2 = new ArrayList(var1.size());

         for (Object var4 : var1) {
            if (var4 != null) {
               var2.add(String.valueOf(var4));
            }
         }

         return var2;
      } else {
         return List.of();
      }
   }

   public record RequirementCoverageAnalysis(Map<String, io.karatelabs.ext.requirements.CoverageAnalyzer.RequirementCoverage> a, List<Map<String, Object>> b, List<Map<String, Object>> c, List<Map<String, Object>> d, List<Map<String, Object>> e, boolean f) {

   }

   public record RequirementCoverage(String a, String b, boolean c, List<String> d, List<String> e) {

   }
}
