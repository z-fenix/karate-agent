package io.karatelabs.ext.rules.twin;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class FreshnessAnnotator {
   static final String a = "current";
   static final String b = "ungraded";
   private static final Set<String> c = Set.of("STALE", "UNREPRODUCED", "CORRUPT", "VERSION");
   private static final List<String> d = List.of("kind", "code", "at", "reason", "mismatch");
   private static final List<String> e = List.of("steps", "attempted", "from", "to", "replays", "verified", "mismatch", "budgetExhausted");

   private FreshnessAnnotator() {
   }

   static Map<String, Object> a(List<Map<String, Object>> var0, Map<String, Map<String, Object>> var1, Map<String, Object> var2) {
      var var3 = a(var2);
      ArrayList var4 = new ArrayList();

      for (Map var6 : var0) {
         String var7 = String.valueOf(var6.get("_id"));
         Map var8 = (Map)var1.get(var7);
         if (var8 != null || var3.containsKey(var7)) {
            String var9 = var2 == null ? "ungraded" : var3.getOrDefault(var7, "current");
            LinkedHashMap var10 = new LinkedHashMap();
            var10.put("sequenceId", var7);
            var10.put("freshness", var9);
            if (var8 != null && io.karatelabs.ext.rules.LiveRecordStore.a((Object)var8)) {
               a(var10, var8, "current".equals(var9));
            }

            var4.add(var10);
         }
      }

      return var4.isEmpty() ? null : Map.of("rows", var4);
   }

   private static void a(Map<String, Object> var0, Map<String, Object> var1, boolean var2) {
      var0.put("against", var1.get("against"));
      if (var1.get("sutRevision") != null) {
         var0.put("sutRevision", var1.get("sutRevision"));
      }

      Map var3 = (Map)var1.get("disposition");
      LinkedHashMap var4 = new LinkedHashMap();

      for (String var6 : d) {
         if (var3.get(var6) != null) {
            var4.put(var6, var3.get(var6));
         }
      }

      var0.put("disposition", var4);
      String var10 = io.karatelabs.ext.rules.LiveRecordStore.a(var1, var2);
      if (var10 != null) {
         LinkedHashMap var11 = new LinkedHashMap();
         var11.put("status", var10);
         if (!"DAMAGED".equals(var10)) {
            Map var7 = (Map)var1.get("shrink");

            for (String var9 : e) {
               if (var7.containsKey(var9)) {
                  var11.put(var9, var7.get(var9));
               }
            }
         }

         var0.put("shrink", var11);
      }
   }

   private static Map<String, String> a(Map<String, Object> var0) {
      LinkedHashMap var1 = new LinkedHashMap();
      Object var3 = (var0 == null ? null : var0.get("findings")) instanceof Map var4 ? var4.get("incomplete") : null;
      if (var3 instanceof List) {
         for (Object var6 : (List)var3) {
            if (var6 instanceof Map var7 && "liveUngraded".equals(var7.get("kind")) && c.contains(var7.get("reason"))) {
               var1.put(String.valueOf(var7.get("sequenceId")), String.valueOf(var7.get("reason")));
            }
         }
      }

      return var1;
   }
}
