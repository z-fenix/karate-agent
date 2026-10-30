package io.karatelabs.ext.rules.twin;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public final class ExpectationEvaluator {
   private static final List<String> a = List.of("applied", "refused", "error");
   private static final List<String> b = List.of("guard", "apply", "returnValue", "snapshot", "clock");
   private static final List<String> c = List.of("at", "outcome", "stage", "reason", "world");
   private static final String d = "#equal";

   private ExpectationEvaluator() {
   }

   public static void a(Map<String, Object> var0, int var1) {
      for (Object var3 : var0.keySet()) {
         if (!"steps".equals(var3) && !"final".equals(var3)) {
            throw a("_expect." + var3 + ": unknown key");
         }
      }

      if (var0.containsKey("steps")) {
         if (!(var0.get("steps") instanceof List var11)) {
            throw a("_expect.steps: must be an array");
         }

         LinkedHashSet var13 = new LinkedHashSet();

         for (int var4 = 0; var4 < var11.size(); var4++) {
            String var5 = "_expect.steps[" + var4 + "]";
            if (!(var11.get(var4) instanceof Map var6)) {
               throw a(var5 + ": must be an object");
            }

            for (Object var8 : var6.keySet()) {
               if (!c.contains(var8)) {
                  throw a(var5 + "." + var8 + ": unknown key");
               }
            }

            if (!(var6.get("at") instanceof Number var15) || var15.doubleValue() != var15.intValue()) {
               throw a(var5 + ".at: must be an integer step index");
            }

            int var17 = var15.intValue();
            if (var17 < 0 || var17 >= var1) {
               throw a(var5 + ".at: " + var17 + " is not a step of this sequence", var17);
            }

            if (!var13.add(var17)) {
               throw a(var5 + ".at: " + var17 + " is expected twice", var17);
            }

            if (!(var6.get("outcome") instanceof String var9) || !a.contains(var9)) {
               throw a(var5 + ".outcome: must be one of " + String.join(" | ", a), var17);
            }

            if (var6.containsKey("stage") && !b.contains(var6.get("stage"))) {
               throw a(var5 + ".stage: must be one of " + String.join(" | ", b), var17);
            }

            if (var6.containsKey("reason") && !(var6.get("reason") instanceof String)) {
               throw a(var5 + ".reason: must be a string", var17);
            }

            if (var6.containsKey("world") && !(var6.get("world") instanceof Map)) {
               throw a(var5 + ".world: must be an object", var17);
            }
         }
      }

      if (var0.containsKey("final") && !(var0.get("final") instanceof Map)) {
         throw a("_expect.final: must be an object");
      }
   }

   public static boolean a(Map<String, Object> var0) {
      return !var0.containsKey("final") && !(var0.get("steps") instanceof List var1 && !var1.isEmpty());
   }

   static List<Map<String, Object>> a(Map<String, Object> var0, ReplayTrace var1) {
      ArrayList var2 = new ArrayList();
      Object var4 = var0.get("steps");
      if (var4 instanceof List) {
         for (Object var5 : (List)var4) {
            Map var6 = (Map)var5;
            int var7 = ((Number)var6.get("at")).intValue();
            io.karatelabs.ext.rules.twin.ReplayTrace.ReplayStep var8 = var1.a().get(var7);
            a(var2, var7, "outcome", var6.get("outcome"), var8.c());
            if (var6.containsKey("stage")) {
               a(var2, var7, "stage", var6.get("stage"), var8.d());
            }

            if (var6.containsKey("reason")) {
               a(var2, var7, "reason", var6.get("reason"), var8.e());
            }

            if (var6.containsKey("world")) {
               a(var2, Integer.valueOf(var7), "world", var6.get("world"), var8.i());
            }
         }
      }

      if (var0.containsKey("final")) {
         a(var2, null, "final", var0.get("final"), var1.b());
      }

      return var2;
   }

   static boolean a(Object var0, Object var1) {
      ArrayList var2 = new ArrayList();
      a(var2, null, "", var0, var1);
      return var2.isEmpty();
   }

   private static void a(List<Map<String, Object>> var0, int var1, String var2, Object var3, String var4) {
      if (var4 == null) {
         var0.add(a(var1, var2, var3));
      } else if (!var4.equals(var3)) {
         b(var0, var1, var2, var3, var4);
      }
   }

   private static void a(List<Map<String, Object>> var0, Integer var1, String var2, Object var3, Object var4) {
      if (var3 instanceof Map var5) {
         if (!(var4 instanceof Map var7)) {
            b(var0, var1, var2, var3, var4);
            return;
         }

         for (Entry var9 : (Iterable<Entry>) (Iterable<?>) (var5.entrySet())) {
            String var10 = var2 + "." + var9.getKey();
            if ("#equal".equals(var9.getKey())) {
               Object var12 = var9.getValue();
               if (var12 instanceof List) {
                  for (Object var13 : (List)var12) {
                     if (var13 instanceof List var14 && var14.size() == 2) {
                        Object var15 = a(var7, String.valueOf(var14.get(0)));
                        Object var16 = a(var7, String.valueOf(var14.get(1)));
                        if (var15 == null || var16 == null || !io.karatelabs.ext.rules.twin.CanonicalJson.c(var15).equals(io.karatelabs.ext.rules.twin.CanonicalJson.c(var16))) {
                           b(var0, var1, var10 + "[" + var14.get(0) + "=" + var14.get(1) + "]", var13, List.of(String.valueOf(var15), String.valueOf(var16)));
                        }
                     } else {
                        b(var0, var1, var10, var13, null);
                     }
                  }
               } else {
                  b(var0, var1, var10, var9.getValue(), null);
               }
            } else if (!var7.containsKey(var9.getKey())) {
               var0.add(a(var1, var10, var9.getValue()));
            } else {
               a(var0, var1, var10, var9.getValue(), var7.get(var9.getKey()));
            }
         }
      } else if (var3 instanceof List var6) {
         if (!(var4 instanceof List var17) || var17.size() != var6.size()) {
            b(var0, var1, var2, var3, var4);
            return;
         }

         for (int var18 = 0; var18 < var6.size(); var18++) {
            a(var0, var1, var2 + "[" + var18 + "]", var6.get(var18), var17.get(var18));
         }
      } else if (!io.karatelabs.ext.rules.twin.CanonicalJson.c(var3).equals(io.karatelabs.ext.rules.twin.CanonicalJson.c(var4))) {
         b(var0, var1, var2, var3, var4);
      }
   }

   private static Object a(Map<?, ?> var0, String var1) {
      Object var2 = var0;

      for (String var6 : var1.split("\\.")) {
         if (!(var2 instanceof Map var7) || !var7.containsKey(var6)) {
            return null;
         }

         var2 = var7.get(var6);
      }

      return var2;
   }

   private static void b(List<Map<String, Object>> var0, Integer var1, String var2, Object var3, Object var4) {
      var var5 = a(var1, var2, var3);
      var5.put("actual", var4);
      var0.add(var5);
   }

   private static Map<String, Object> a(Integer var0, String var1, Object var2) {
      LinkedHashMap var3 = new LinkedHashMap();
      if (var0 != null) {
         var3.put("at", var0);
      }

      var3.put("field", var1);
      var3.put("expected", var2);
      return var3;
   }

   private static TwinException a(String var0) {
      return TwinException.a(var0, (Map<String, Object>)null);
   }

   private static TwinException a(String var0, int var1) {
      LinkedHashMap var2 = new LinkedHashMap();
      var2.put("at", var1);
      return TwinException.a(var0, var2);
   }
}
