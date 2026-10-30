package io.karatelabs.ext.mcp;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.Map.Entry;

final class McpSchemaNormalizer {
   private static final String a = "x-mcp-header";

   private McpSchemaNormalizer() {
   }

   static Object a(Map<String, Object> var0) {
      if (var0.get("inputSchema") instanceof Map var1) {
         LinkedHashMap var6 = new LinkedHashMap();
         var var3 = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
         String var4 = a(var1, new ArrayList<>(), var6, var3);
         if (var4 != null) {
            return var4;
         } else {
            int var5 = a((Object)var1);
            return var5 != var6.size() ? "x-mcp-header annotation outside a plain properties chain" : var6;
         }
      } else {
         return new LinkedHashMap();
      }
   }

   private static String a(Map<?, ?> var0, List<String> var1, Map<String, List<String>> var2, Set<String> var3) {
      if (var0.get("properties") instanceof Map var4) {
         for (Entry var6 : (Iterable<Entry>) (Iterable<?>) (var4.entrySet())) {
            String var7 = String.valueOf(var6.getKey());
            if (var6.getValue() instanceof Map var8) {
               var1.add(var7);
               Object var13 = var8.get("x-mcp-header");
               if (var13 != null) {
                  String var10 = String.valueOf(var13);
                  String var11 = a(var10, var8, var3);
                  if (var11 != null) {
                     return var11;
                  }

                  var2.put(var10, new ArrayList(var1));
               }

               String var14 = a(var8, var1, var2, var3);
               if (var14 != null) {
                  return var14;
               }

               var1.remove(var1.size() - 1);
            }
         }

         return null;
      } else {
         return null;
      }
   }

   private static String a(String var0, Map<?, ?> var1, Set<String> var2) {
      if (var0.isEmpty()) {
         return "empty x-mcp-header name";
      } else {
         for (int var3 = 0; var3 < var0.length(); var3++) {
            char var4 = var0.charAt(var3);
            boolean var5 = Character.isLetterOrDigit(var4) || "!#$%&'*+-.^_`|~".indexOf(var4) >= 0;
            if (!var5) {
               return "x-mcp-header name is not a header token: " + var0;
            }
         }

         if (!var2.add(var0)) {
            return "x-mcp-header name not case-insensitively unique: " + var0;
         } else {
            Object var6 = var1.get("type");
            return !"string".equals(var6) && !"integer".equals(var6) && !"boolean".equals(var6)
               ? "x-mcp-header on a non-primitive or number property: " + var0 + " (" + var6 + ")"
               : null;
         }
      }
   }

   private static int a(Object var0) {
      int var1 = 0;
      if (var0 instanceof Map var2) {
         for (Entry var5 : (Iterable<Entry>) (Iterable<?>) (var2.entrySet())) {
            if ("x-mcp-header".equals(var5.getKey())) {
               var1++;
            }

            var1 += a(var5.getValue());
         }
      } else if (var0 instanceof List) {
         for (Object var7 : (List)var0) {
            var1 += a(var7);
         }
      }

      return var1;
   }

   static Map<String, String> a(Map<String, List<String>> var0, Map<String, Object> var1) {
      LinkedHashMap<java.lang.String, java.lang.String> var2 = new LinkedHashMap();
      if (var1 == null) {
         return var2;
      } else {
         for (Entry var4 : var0.entrySet()) {
            Object var5 = var1;

            for (String var7 : (Iterable<String>) (Iterable<?>) ((List)var4.getValue())) {
               var5 = var5 instanceof Map var8 ? var8.get(var7) : null;
            }

            if (var5 != null) {
               String var9 = var5 instanceof Boolean var11
                  ? String.valueOf(var11)
                  : (var5 instanceof Number var10 ? String.valueOf(var10.longValue()) : String.valueOf(var5));
               var2.put("Mcp-Param-" + (String)var4.getKey(), a(var9));
            }
         }

         return var2;
      }
   }

   static String a(String var0) {
      return io.karatelabs.ext.mcp.McpClient.b(var0) ? io.karatelabs.ext.mcp.McpClient.c(var0) : var0;
   }
}
