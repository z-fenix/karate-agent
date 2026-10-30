package io.karatelabs.ext.openapi;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class OpenApiDocumentView {
   private OpenApiDocumentView() {
   }

   public static Map<String, Object> a(OpenApiOperationCatalog var0) {
      OpenApiDocument var1 = var0.document();
      LinkedHashMap<java.lang.String, java.lang.Object> var2 = new LinkedHashMap();
      var2.put("title", var1.title);
      var2.put("version", var1.version());
      var2.put("specType", var1.specType);
      var2.put("baseUrl", var1.defaultServerUrl());
      if (var1.description != null && !var1.description.isBlank()) {
         var2.put("description", var1.description);
      }

      ArrayList<java.util.LinkedHashMap> var3 = new ArrayList();

      for (io.karatelabs.ext.openapi.OpenApiOperationCatalog.OperationInfo var5 : var0.operations()) {
         String var6 = var5.a() != null ? var5.a() : var5.b().toUpperCase() + " " + var5.c();
         LinkedHashMap<java.lang.String, java.lang.Object> var7 = new LinkedHashMap();
         var7.put("opId", var6);
         var7.put("method", var5.b().toUpperCase());
         var7.put("path", var5.c());
         if (var5.h() != null && !var5.h().isBlank()) {
            var7.put("summary", var5.h());
         }

         if (var5.i() != null && !var5.i().isEmpty()) {
            var7.put("tags", var5.i());
         }

         io.karatelabs.ext.openapi.OpenApiOperationCatalog.ResourceOperation var8 = var0.resourceOperationFor(var5);
         if (var8 != null) {
            var7.put("resource", var8.a().a());
            var7.put("crud", var8.b().name().toLowerCase());
         }

         if (var5.f() != null) {
            try {
               var7.put("requestSample", var5.f().sample());
            } catch (RuntimeException var11) {
            }
         }

         var7.put("responses", a(var1, var6));
         var3.add(var7);
      }

      var2.put("operations", var3);
      var2.put("operationCount", var3.size());
      ArrayList<java.util.LinkedHashMap> var12 = new ArrayList();

      for (String var14 : OpenApiDocumentQueries.b(var1)) {
         OpenApiSchemaNode var15 = OpenApiDocumentQueries.a(var1, var14);
         if (var15 != null) {
            LinkedHashMap<java.lang.String, java.lang.Object> var16 = new LinkedHashMap();
            var16.put("name", var14);

            try {
               var16.put("template", io.karatelabs.ext.openapi.SchemaMatchConverter.toMatchTemplate(var15));
               var16.put("sample", var15.sample());
            } catch (RuntimeException var10) {
            }

            var12.add(var16);
         }
      }

      var2.put("components", var12);
      return var2;
   }

   private static List<Map<String, Object>> a(OpenApiDocument var0, String var1) {
      ArrayList<java.util.Map<java.lang.String,java.lang.Object>> var2 = new ArrayList();
      OpenApiOperation var3 = OpenApiDocumentQueries.b(var0, var1);
      if (var3 == null) {
         return var2;
      } else {
         for (OpenApiResponse var5 : var3.d) {
            LinkedHashMap<java.lang.String, java.lang.Object> var6 = new LinkedHashMap();
            var6.put("status", var5.b);
            if (var5.d != null && !var5.d.isBlank()) {
               var6.put("description", var5.d);
            }

            var6.put("negative", var5.b != null && (var5.b.startsWith("4") || var5.b.startsWith("5")));
            if (!var5.e.isEmpty() && var5.e.getFirst().d != null) {
               try {
                  var6.put("sample", var5.e.getFirst().d.getRootNode().sample());
               } catch (RuntimeException var8) {
               }
            }

            var2.add(var6);
         }

         return var2;
      }
   }
}
