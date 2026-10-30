package io.karatelabs.ext.contract;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class Partitions {
   private Partitions() {
   }

   public static Map<String, Object> bva(String var0, List<Double> var1, boolean var2) {
      if (var1 != null && !var1.isEmpty()) {
         var var3 = new ArrayList<>(var1);
         var3.sort(Double::compareTo);
         ArrayList<java.lang.String> var4 = new ArrayList();
         ArrayList<java.lang.Object> var5 = new ArrayList();
         ArrayList<java.lang.Object> var6 = new ArrayList();
         double var7 = (Double)var3.get(0);
         var4.add("< " + num(var7));
         var5.add(sample(var7 - step(var7), var2));

         for (int var9 = 0; var9 < var3.size(); var9++) {
            double var10 = (Double)var3.get(var9);
            var6.add(num(var10));
            var4.add("= " + num(var10));
            var5.add(num(var10));
            if (var9 < var3.size() - 1) {
               double var12 = (Double)var3.get(var9 + 1);
               var4.add(num(var10) + "–" + num(var12));
               var5.add(sample((var10 + var12) / 2.0, var2));
            }
         }

         double var14 = (Double)var3.get(var3.size() - 1);
         var4.add("> " + num(var14));
         var5.add(sample(var14 + step(var14), var2));
         return partition(var0, var4, var5, var6);
      } else {
         return null;
      }
   }

   public static Map<String, Object> string(Integer var0, Integer var1, String var2, String var3) {
      if (var0 == null && var1 == null && var2 == null && var3 == null) {
         return null;
      } else {
         ArrayList var4 = new ArrayList();
         ArrayList var5 = new ArrayList();
         add(var4, var5, "empty", "");
         int var6 = validLength(var0, var1);
         if (var1 == null || var6 != var1) {
            add(var4, var5, "valid", "x".repeat(var6));
         }

         if (var0 != null && var0 > 0) {
            add(var4, var5, "< minLength", "x".repeat(Math.max(0, var0 - 1)));
         }

         if (var1 != null) {
            add(var4, var5, "= maxLength", "x".repeat(var1));
            add(var4, var5, "> maxLength", "x".repeat(var1 + 1));
         }

         if (var2 != null) {
            add(var4, var5, "format-invalid", "not-a-" + var2);
         } else if (var3 != null) {
            add(var4, var5, "pattern-invalid", "~pattern-violation~");
         }

         Map<java.lang.String, java.lang.Object> var7 = partition("string", var4, var5, null);
         if (var0 != null) {
            var7.put("minLength", var0);
         }

         if (var1 != null) {
            var7.put("maxLength", var1);
         }

         return var7;
      }
   }

   private static int validLength(Integer var0, Integer var1) {
      int var2 = var0 != null ? Math.max(var0, 0) : 1;
      if (var2 == 0) {
         var2 = 1;
      }

      if (var1 != null && var2 > var1) {
         var2 = var1;
      }

      return var2;
   }

   private static Map<String, Object> partition(String var0, List<String> var1, List<Object> var2, List<Object> var3) {
      LinkedHashMap<java.lang.String, java.lang.Object> var4 = new LinkedHashMap();
      var4.put("kind", var0);
      var4.put("classes", var1);
      if (var2 != null) {
         var4.put("samples", var2);
      }

      if (var3 != null) {
         var4.put("boundaries", var3);
      }

      return var4;
   }

   private static void add(List<String> var0, List<Object> var1, String var2, Object var3) {
      var0.add(var2);
      var1.add(var3);
   }

   private static Object sample(double var0, boolean var2) {
      return var2 ? (int)Math.max(0L, Math.round(var0)) : num(var0);
   }

   private static Object num(double var0) {
      return var0 == Math.rint(var0) && !Double.isInfinite(var0) ? (long)var0 : var0;
   }

   private static double step(double var0) {
      return var0 == Math.rint(var0) ? 1.0 : 0.01;
   }
}
