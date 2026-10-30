package io.karatelabs.ext.openapi;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

public final class OpenApiSpecSlicer {
   private static final Set<String> a = Set.of("get", "put", "post", "delete", "patch", "head", "options", "trace");

   private OpenApiSpecSlicer() {
   }

   public static OpenApiSpecSlicer.a a(OpenApiDocument var0, List<String> var1) {
      return a(var0.rawDocument, var1);
   }

   static OpenApiSpecSlicer.a a(Map<String, Object> var0, List<String> var1) {
      Map<?, ?> var2 = var0.get("paths") instanceof Map ? (Map<?, ?>)var0.get("paths") : Map.of();
      LinkedHashMap var3 = new LinkedHashMap();

      for (Entry var5 : var2.entrySet()) {
         if (var1.contains(var5.getKey())) {
            var3.put((String)var5.getKey(), var5.getValue());
         }
      }

      LinkedHashSet<String> var11 = new LinkedHashSet<>();
      a(var3, var11);
      a(var3, var0, var11);
      if (var0.get("security") != null) {
         a(Map.of("security", var0.get("security")), var0, var11);
      }

      boolean var12 = true;

      while (var12) {
         var12 = false;

         for (String var7 : new ArrayList<>(var11)) {
            Object var8 = a(var0, var7);
            if (var8 != null) {
               int var9 = var11.size();
               a(var8, var11);
               a(var8, var0, var11);
               if (var11.size() != var9) {
                  var12 = true;
               }
            }
         }
      }

      LinkedHashMap var13 = new LinkedHashMap();

      for (String var16 : List.of("openapi", "swagger", "info", "servers", "host", "basePath", "schemes", "consumes", "produces", "security")) {
         if (var0.containsKey(var16)) {
            var13.put(var16, var0.get(var16));
         }
      }

      var13.put("paths", var3);
      LinkedHashMap<String, Integer> var15 = new LinkedHashMap<>();

      for (String var18 : var11) {
         Object var10 = a(var0, var18);
         if (var10 != null) {
            a(var13, var18, var10);
            var15.merge(b(var18), 1, Integer::sum);
         }
      }

      return new OpenApiSpecSlicer.a(var13, new ArrayList<>(var3.keySet()), var15, a(var3));
   }

   private static void a(Object var0, Set<String> var1) {
      if (var0 instanceof Map<?, ?> var2) {
         for (Entry var5 : var2.entrySet()) {
            if ("$ref".equals(var5.getKey()) && var5.getValue() instanceof String var6 && var6.startsWith("#/")) {
               var1.add(var6.substring(1));
            } else {
               a(var5.getValue(), var1);
            }
         }
      } else if (var0 instanceof List) {
         for (Object var9 : (List)var0) {
            a(var9, var1);
         }
      }
   }

   private static void a(Object var0, Map<String, Object> var1, Set<String> var2) {
      if (var0 instanceof Map<?, ?> var3) {
         for (Entry var6 : var3.entrySet()) {
            if ("security".equals(var6.getKey())) {
               Object var8 = var6.getValue();
               if (var8 instanceof List) {
                  for (Object var9 : (List)var8) {
                     if (var9 instanceof Map var10) {
                        for (Object var12 : var10.keySet()) {
                           String var13 = "/components/securitySchemes/" + var12;
                           String var14 = "/securityDefinitions/" + var12;
                           if (a(var1, var13) != null) {
                              var2.add(var13);
                           } else if (a(var1, var14) != null) {
                              var2.add(var14);
                           }
                        }
                     }
                  }
                  continue;
               }
            }

            a(var6.getValue(), var1, var2);
         }
      } else if (var0 instanceof List) {
         for (Object var16 : (List)var0) {
            a(var16, var1, var2);
         }
      }
   }

   private static Object a(Map<String, Object> var0, String var1) {
      Object var2 = var0;

      for (String var4 : a(var1)) {
         if (var2 instanceof Map var6) {
            var2 = var6.get(var4);
         } else {
            if (!(var2 instanceof List var5)) {
               return null;
            }

            try {
               var2 = var5.get(Integer.parseInt(var4));
            } catch (IndexOutOfBoundsException | NumberFormatException var8) {
               return null;
            }
         }

         if (var2 == null) {
            return null;
         }
      }

      return var2;
   }

   private static void a(Map<String, Object> var0, String var1, Object var2) {
      List var3 = a(var1);
      Map var4 = var0;

      for (int var5 = 0; var5 < var3.size() - 1; var5++) {
         Object var6 = var4.get(var3.get(var5));
         if (!(var6 instanceof Map)) {
            var6 = new LinkedHashMap();
            var4.put((String)var3.get(var5), var6);
         }

         var4 = (Map)var6;
      }

      var4.put((String)var3.getLast(), var2);
   }

   private static List<String> a(String var0) {
      ArrayList var1 = new ArrayList();

      for (String var5 : var0.split("/")) {
         if (!var5.isEmpty()) {
            var1.add(var5.replace("~1", "/").replace("~0", "~"));
         }
      }

      return var1;
   }

   private static String b(String var0) {
      List var1 = a(var0);
      return var1.size() <= 1 ? var0 : String.join("/", var1.subList(0, var1.size() - 1));
   }

   private static int a(Map<String, Object> var0) {
      int var1 = 0;

      for (Object var3 : var0.values()) {
         if (var3 instanceof Map var4) {
            for (Object var6 : var4.keySet()) {
               if (a.contains(String.valueOf(var6).toLowerCase())) {
                  var1++;
               }
            }
         }
      }

      return var1;
   }

   public record a(Map<String, Object> a, List<String> b, Map<String, Integer> c, int d) {
   }
}
