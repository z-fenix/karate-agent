package io.karatelabs.ext.coverage;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ErrorPathCoverageAnalyzer {
   private static final String a = "OK";
   private static final int b = 400;

   private ErrorPathCoverageAnalyzer() {
   }

   public static Map<String, Object> a(Map<String, Object> var0, List<String> var1) {
      ArrayList<java.lang.String> var2 = new ArrayList();
      boolean var3 = false;
      if (var0.get("grpcStatuses") instanceof Map var4) {
         var3 = true;

         for (Object var7 : var4.keySet()) {
            String var8 = String.valueOf(var7);
            if (!"OK".equalsIgnoreCase(var8)) {
               var2.add(var8);
            }
         }
      } else if (var0.get("statusCodes") instanceof Map var5) {
         var3 = true;

         for (Object var15 : var5.keySet()) {
            if (a(var15) >= 400) {
               var2.add(String.valueOf(var15));
            }
         }
      }

      if (!var3) {
         return null;
      } else {
         LinkedHashMap<java.lang.String, java.lang.Object> var9 = new LinkedHashMap();
         var9.put("tested", !var2.isEmpty());
         var9.put("errors", var2);
         if (var1 != null) {
            ArrayList<java.lang.String> var10 = new ArrayList();

            for (String var16 : var1) {
               if (a((Object)var16) >= 400 && !var2.contains(var16)) {
                  var10.add(var16);
               }
            }

            if (!var10.isEmpty()) {
               var9.put("declaredUntested", var10);
            }
         }

         return var9;
      }
   }

   public static void a(Map<String, Object> var0) {
      if (var0.get("items") instanceof List var1) {
         LinkedHashMap<java.lang.String, int[]> var9 = new LinkedHashMap();

         for (Object var4 : var1) {
            if (var4 instanceof Map var5 && var5.get("outcome") instanceof Map var6) {
               String var15 = String.valueOf(var5.get("source"));
               int[] var8 = var9.computeIfAbsent(var15, var0x -> new int[2]);
               var8[0]++;
               if (Boolean.TRUE.equals(var6.get("tested"))) {
                  var8[1]++;
               }
            }
         }

         if (!var9.isEmpty()) {
            Object var11 = var0.get("sources");
            if (var11 instanceof List) {
               for (Object var13 : (List)var11) {
                  if (var13 instanceof Map var14) {
                     int[] var16 = (int[])var9.get(String.valueOf(var14.get("namespace")));
                     if (var16 != null && var16[0] > 0) {
                        LinkedHashMap<java.lang.String, java.lang.Integer> var17 = new LinkedHashMap();
                        var17.put("total", var16[0]);
                        var17.put("covered", var16[1]);
                        var14.put("errorPaths", var17);
                     }
                  }
               }

               return;
            }
         }
      }
   }

   private static int a(Object var0) {
      if (var0 instanceof Number var1) {
         return var1.intValue();
      } else {
         try {
            return Integer.parseInt(String.valueOf(var0).trim());
         } catch (NumberFormatException var2) {
            return -1;
         }
      }
   }
}
