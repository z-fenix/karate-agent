package io.karatelabs.ext.mutation;

import io.karatelabs.common.Json;
import io.karatelabs.ext.openapi.OpenApiSchemaNode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.Map.Entry;

public final class ResponseMutationFactory {
   private ResponseMutationFactory() {
   }

   public static String normalizePath(String var0) {
      String var1 = var0 == null ? "" : var0;
      int var2 = var1.indexOf("://");
      if (var2 != -1) {
         int var3 = var1.indexOf(47, var2 + 3);
         var1 = var3 == -1 ? "/" : var1.substring(var3);
      }

      int var4 = var1.indexOf(63);
      if (var4 != -1) {
         var1 = var1.substring(0, var4);
      }

      return var1.startsWith("/") ? var1 : "/" + var1;
   }

   public static String operationIdentity(io.karatelabs.ext.openapi.OpenApiOperationCatalog var0, String var1, String var2) {
      String var3 = normalizePath(var2);
      if (var0 != null) {
         io.karatelabs.ext.openapi.OpenApiOperationCatalog.OperationInfo var4 = var0.findOperation(var1, var3);
         if (var4 != null && var4.a() != null) {
            return var4.a();
         }
      }

      return (var1 == null ? "GET" : var1.toUpperCase()) + " " + var3;
   }

   public static List<io.karatelabs.ext.mutation.ResponseMutationFactory.ObservedOperationResponse> collectOperations(List<Map<String, Object>> var0, io.karatelabs.ext.openapi.OpenApiOperationCatalog var1) {
      TreeMap<java.lang.String, io.karatelabs.ext.mutation.ResponseMutationFactory.ObservedOperationResponse> var2 = new TreeMap();
      LinkedHashMap<java.lang.String, java.lang.Boolean> var3 = new LinkedHashMap();

      for (Map var5 : var0) {
         Map<java.lang.String, Object> var6 = mapField(var5, "request");
         Map<java.lang.String, Object> var7 = mapField(var5, "response");
         if (var6 != null && var7 != null) {
            String var8 = String.valueOf(var6.get("method"));
            String var9 = String.valueOf(var6.get("url"));
            String var10 = operationIdentity(var1, var8, var9);
            int var11 = var7.get("status") instanceof Number var12 ? var12.intValue() : -1;
            boolean var15 = var11 >= 200 && var11 < 300;
            io.karatelabs.ext.mutation.ResponseMutationFactory.ObservedOperationResponse var16 = (io.karatelabs.ext.mutation.ResponseMutationFactory.ObservedOperationResponse)var2.get(var10);
            if (var16 == null || var15 && !var3.getOrDefault(var10, false)) {
               String var14 = contentType(var7);
               var2.put(var10, new io.karatelabs.ext.mutation.ResponseMutationFactory.ObservedOperationResponse(var10, var8.toUpperCase(), normalizePath(var9), var11, var7.get("body"), var14));
               var3.put(var10, var15);
            }
         }
      }

      return new ArrayList<>(var2.values());
   }

   public static List<ResponseMutationSpec> generateMutants(List<io.karatelabs.ext.mutation.ResponseMutationFactory.ObservedOperationResponse> var0, io.karatelabs.ext.openapi.OpenApiOperationCatalog var1, boolean var2) {
      ArrayList<io.karatelabs.ext.mutation.ResponseMutationSpec> var3 = new ArrayList();

      for (io.karatelabs.ext.mutation.ResponseMutationFactory.ObservedOperationResponse var5 : var0) {
         int var6 = var5.d() >= 400 ? 200 : 500;
         var3.add(
            new ResponseMutationSpec("STATUS_SWAP:" + var5.a(), "STATUS_SWAP", var5.a(), "status", var5.d(), var6, null, String.valueOf(var5.d()), String.valueOf(var6), null, -1)
         );
         if (var2 && var5.e() != null) {
            var3.add(
               new ResponseMutationSpec(
                  "CONTENT_TYPE_TAMPER:" + var5.a(),
                  "CONTENT_TYPE_TAMPER",
                  var5.a(),
                  "content-type",
                  -1,
                  -1,
                  null,
                  var5.f() == null ? "(none)" : var5.f(),
                  "text/plain",
                  null,
                  -1
               )
            );
         }

         if (var5.e() instanceof Map || var5.e() instanceof List) {
            var3.add(new ResponseMutationSpec("MALFORMED_BODY:" + var5.a(), "MALFORMED_BODY", var5.a(), "body", -1, -1, null, "well-formed JSON", "truncated bytes", null, -1));
         }

         if (var5.e() instanceof Map var7) {
            for (Entry var9 : (Iterable<Entry>) (Iterable<?>) (var7.entrySet())) {
               String var10 = String.valueOf(var9.getKey());
               Object var11 = var9.getValue();
               String var12 = previewValue(var11);
               var3.add(fieldMutation("FIELD_DROP", var5, var10, var12, "(dropped)"));
               if (var11 != null) {
                  var3.add(fieldMutation("FIELD_NULL", var5, var10, var12, "null"));
               }

               if (var11 instanceof Number var13) {
                  var3.add(fieldMutation("TYPE_FLIP", var5, var10, var12, "\"" + var12 + "\""));
                  var3.add(fieldMutation("VALUE_OFF_BY_ONE", var5, var10, var12, previewValue(incrementNumber(var13))));
               } else if (!(var11 instanceof String) && !(var11 instanceof Boolean)) {
                  if (var11 instanceof List var14 && !var14.isEmpty()) {
                     var3.add(fieldMutation("CARDINALITY_EMPTY", var5, var10, "[" + var14.size() + " element(s)]", "[]"));
                  }
               } else {
                  var3.add(fieldMutation("TYPE_FLIP", var5, var10, var12, var11 instanceof String ? "0" : "\"" + var11 + "\""));
               }
            }
         }
      }

      if (var1 != null) {
         LinkedHashMap<java.lang.String, io.karatelabs.ext.mutation.ResponseMutationFactory.ObservedOperationResponse> var15 = new LinkedHashMap();

         for (io.karatelabs.ext.mutation.ResponseMutationFactory.ObservedOperationResponse var18 : var0) {
            var15.put(var18.a(), var18);
         }

         for (io.karatelabs.ext.openapi.OpenApiOperationCatalog.OperationInfo var19 : var1.operations()) {
            String var20 = var19.a() != null ? var19.a() : var19.b().toUpperCase() + " " + var19.c();
            io.karatelabs.ext.mutation.ResponseMutationFactory.ObservedOperationResponse var22 = (io.karatelabs.ext.mutation.ResponseMutationFactory.ObservedOperationResponse)var15.get(var20);

            for (int var25 : declaredStatuses(var1, var19)) {
               if (var25 != var19.d()) {
                  var3.add(
                     new ResponseMutationSpec(
                        "DECLARED_STATUS:" + var20 + ":" + var25,
                        "DECLARED_STATUS",
                        var20,
                        "status",
                        var19.d(),
                        var25,
                        null,
                        String.valueOf(var19.d()),
                        var25 + " (declared)",
                        null,
                        -1
                     )
                  );
               }
            }

            for (Entry var26 : enumValuesByField(var1, var19).entrySet()) {
               Object var27 = var22 != null && var22.e() instanceof Map var28 ? var28.get(var26.getKey()) : null;
               Object var29 = ((List)var26.getValue()).stream().filter(var1x -> var1x != null && !var1x.equals(var27)).findFirst().orElse(null);
               if (var29 != null) {
                  var3.add(
                     new ResponseMutationSpec(
                        "ENUM_SUBSTITUTION:" + var20 + ":" + (String)var26.getKey(),
                        "ENUM_SUBSTITUTION",
                        var20,
                        "field:" + (String)var26.getKey(),
                        -1,
                        -1,
                        (String)var26.getKey(),
                        var27 == null ? "(declared enum)" : previewValue(var27),
                        previewValue(var29),
                        var29,
                        -1
                     )
                  );
               }
            }

            if (var22 == null && declaredStatuses(var1, var19).stream().allMatch(var1x -> var1x == var19.d())) {
               var3.add(new ResponseMutationSpec("STATUS_SWAP:" + var20, "STATUS_SWAP", var20, "status", var19.d(), 500, null, var19.d() + " (declared)", "500", null, -1));
            }
         }
      }

      return var3;
   }

   private static Map<String, List<Object>> enumValuesByField(io.karatelabs.ext.openapi.OpenApiOperationCatalog var0, io.karatelabs.ext.openapi.OpenApiOperationCatalog.OperationInfo var1) {
      LinkedHashMap<java.lang.String, java.util.List<java.lang.Object>> var2 = new LinkedHashMap();
      var0.document()
         .paths
         .values()
         .stream()
         .filter(var1x -> var1x.c.equals(var1.c()))
         .flatMap(var0x -> var0x.e.stream())
         .filter(var1x -> var1x.b.equalsIgnoreCase(var1.b()))
         .flatMap(var0x -> var0x.d.stream())
         .filter(var1x -> String.valueOf(var1.d()).equals(var1x.b))
         .flatMap(var0x -> var0x.e.stream())
         .filter(var0x -> var0x.d != null)
         .forEach(var1x -> {
            try {
               OpenApiSchemaNode var2x = var1x.d.getRootNode();
               var2x.markTreePresent();

               for (OpenApiSchemaNode var4 : var2x.children) {
                  var var5 = var4.schemaDefinition.enumValues();
                  if (var5 != null && var5.size() > 1 && var4.name() != null) {
                     var2.putIfAbsent(var4.name(), var5);
                  }
               }
            } catch (RuntimeException var6) {
            }
         });
      return var2;
   }

   private static ResponseMutationSpec fieldMutation(String var0, io.karatelabs.ext.mutation.ResponseMutationFactory.ObservedOperationResponse var1, String var2, String var3, String var4) {
      return new ResponseMutationSpec(var0 + ":" + var1.a() + ":" + var2, var0, var1.a(), "field:" + var2, -1, -1, var2, var3, var4, null, -1);
   }

   private static List<Integer> declaredStatuses(io.karatelabs.ext.openapi.OpenApiOperationCatalog var0, io.karatelabs.ext.openapi.OpenApiOperationCatalog.OperationInfo var1) {
      ArrayList<java.lang.Integer> var2 = new ArrayList();
      var0.document()
         .paths
         .values()
         .stream()
         .filter(var1x -> var1x.c.equals(var1.c()))
         .flatMap(var0x -> var0x.e.stream())
         .filter(var1x -> var1x.b.equalsIgnoreCase(var1.b()))
         .flatMap(var0x -> var0x.d.stream())
         .forEach(var1x -> {
            try {
               var2.add(Integer.parseInt(var1x.b));
            } catch (NumberFormatException var3) {
            }
         });
      return var2;
   }

   private static Object incrementNumber(Number var0) {
      return !(var0 instanceof Integer) && !(var0 instanceof Long) ? var0.doubleValue() + 1.0 : var0.longValue() + 1L;
   }

   static String previewValue(Object var0) {
      String var1;
      if (var0 == null) {
         var1 = "null";
      } else if (var0 instanceof String var2) {
         var1 = var2;
      } else if (!(var0 instanceof Map) && !(var0 instanceof List)) {
         var1 = String.valueOf(var0);
      } else {
         var1 = Json.stringifyStrict(var0);
      }

      return var1.length() > 40 ? var1.substring(0, 37) + "..." : var1;
   }

   private static String contentType(Map<String, Object> var0) {
      if (var0.get("headers") instanceof Map var1) {
         for (Entry var3 : (Iterable<Entry>) (Iterable<?>) (var1.entrySet())) {
            if ("content-type".equalsIgnoreCase(String.valueOf(var3.getKey()))) {
               Object var4 = var3.getValue();
               if (var4 instanceof List var5 && !var5.isEmpty()) {
                  return String.valueOf(var5.get(var5.size() - 1));
               }

               return var4 == null ? null : String.valueOf(var4);
            }
         }

         return null;
      } else {
         return null;
      }
   }

   private static Map<String, Object> mapField(Map<String, Object> var0, String var1) {
      return var0.get(var1) instanceof Map var2 ? var2 : null;
   }

   public record ObservedOperationResponse(String a, String b, String c, int d, Object e, String f) {
   }
}
