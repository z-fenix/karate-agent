package io.karatelabs.ext.openapi;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class OpenApiDocumentQueries {
   private OpenApiDocumentQueries() {
   }

   public static Map<String, Object> a(OpenApiDocument var0) {
      if (var0.rawDocument.get("components") instanceof Map var2 && var2.get("schemas") instanceof Map var6) {
         return var6;
      } else {
         return var0.rawDocument.get("definitions") instanceof Map var5 ? var5 : Map.of();
      }
   }

   public static List<String> b(OpenApiDocument var0) {
      return new ArrayList<>(a(var0).keySet());
   }

   public static OpenApiSchemaNode a(OpenApiDocument var0, String var1) {
      Object var2 = a(var0).get(var1);
      return !(var2 instanceof Map) ? null : new SchemaDefinition(var0, (Map)var2).getRootNode();
   }

   public static List<String> c(OpenApiDocument var0) {
      ArrayList var1 = new ArrayList();

      for (OpenApiPathItem var3 : var0.paths.values()) {
         for (OpenApiOperation var5 : var3.e) {
            var1.add(var5.g != null ? var5.g : var5.b.toUpperCase() + " " + var3.c);
         }
      }

      return var1;
   }

   public static OpenApiOperation b(OpenApiDocument var0, String var1) {
      for (OpenApiPathItem var3 : var0.paths.values()) {
         for (OpenApiOperation var5 : var3.e) {
            if (var1.equals(var5.g) || var1.equalsIgnoreCase(var5.b + " " + var3.c)) {
               return var5;
            }
         }
      }

      return null;
   }

   public static OpenApiSchemaNode a(OpenApiDocument var0, String var1, String var2) {
      OpenApiOperation var3 = b(var0, var1);
      if (var3 == null) {
         return null;
      } else {
         OpenApiResponse var4 = a(var3.d, var2);
         if (var4 != null && !var4.e.isEmpty()) {
            SchemaDefinition var5 = ((OpenApiResponseContent)var4.e.getFirst()).d;
            return var5 == null ? null : var5.getRootNode();
         } else {
            return null;
         }
      }
   }

   private static OpenApiResponse a(List<OpenApiResponse> var0, String var1) {
      if (var1 != null) {
         for (OpenApiResponse var7 : var0) {
            if (var1.equals(var7.b)) {
               return var7;
            }
         }

         return null;
      } else {
         for (OpenApiResponse var3 : var0) {
            if (var3.b != null && var3.b.startsWith("2")) {
               return var3;
            }
         }

         for (OpenApiResponse var6 : var0) {
            if ("default".equals(var6.b)) {
               return var6;
            }
         }

         return null;
      }
   }
}
