package io.karatelabs.ext.mock;

import io.karatelabs.ext.common.JsCallback;
import io.karatelabs.ext.openapi.OpenApiOperationCatalog;
import java.util.LinkedHashMap;
import java.util.Map;

public final class ResourceResolver {
   private final JsCallback callback;
   private final Map<String, Object> dataMap;

   private ResourceResolver(JsCallback var1, Map<String, Object> var2) {
      this.callback = var1;
      this.dataMap = var2;
   }

   public static ResourceResolver from(Object var0) {
      if (var0 == null) {
         return null;
      } else {
         JsCallback var1 = io.karatelabs.ext.common.JsCallback.a(var0);
         if (var1 != null) {
            return new ResourceResolver(var1, null);
         } else {
            JsCallback var2 = io.karatelabs.ext.common.JsCallback.b(var0, "resolve");
            if (var2 != null) {
               return new ResourceResolver(var2, null);
            } else if (var0 instanceof Map var3) {
               LinkedHashMap<java.lang.String, java.lang.Object> var4 = new LinkedHashMap();
               var3.forEach((var1x, var2x) -> {
                  if (var1x != null && var2x != null) {
                     var4.put(OpenApiMockHandler.canonicalKey(String.valueOf(var1x)), var2x);
                  }
               });
               return var4.isEmpty() ? null : new ResourceResolver(null, var4);
            } else {
               return null;
            }
         }
      }
   }

   public io.karatelabs.ext.openapi.OpenApiOperationCatalog.ResourceOperation resolve(io.karatelabs.ext.openapi.OpenApiOperationCatalog.OperationInfo var1) {
      Object var2 = this.callback != null ? this.callback.a(opData(var1)) : this.lookup(var1);
      return toBinding(var2);
   }

   private Object lookup(io.karatelabs.ext.openapi.OpenApiOperationCatalog.OperationInfo var1) {
      for (String var3 : OpenApiMockHandler.opKeys(var1)) {
         if (this.dataMap.containsKey(var3)) {
            return this.dataMap.get(var3);
         }
      }

      return null;
   }

   private static Map<String, Object> opData(io.karatelabs.ext.openapi.OpenApiOperationCatalog.OperationInfo var0) {
      LinkedHashMap<java.lang.String, java.lang.Object> var1 = new LinkedHashMap();
      var1.put("operationId", var0.a());
      var1.put("method", var0.b());
      var1.put("path", var0.c());
      return var1;
   }

   private static io.karatelabs.ext.openapi.OpenApiOperationCatalog.ResourceOperation toBinding(Object var0) {
      Object var1 = io.karatelabs.ext.common.JsCallback.a(var0, "resource");
      Object var2 = io.karatelabs.ext.common.JsCallback.a(var0, "action");
      if (var1 != null && var2 != null) {
         io.karatelabs.ext.openapi.OpenApiOperationCatalog.ResourceAction var3 = parseAction(String.valueOf(var2));
         if (var3 == null) {
            return null;
         } else {
            Object var4 = io.karatelabs.ext.common.JsCallback.a(var0, "id");
            String var5 = String.valueOf(var1);
            io.karatelabs.ext.openapi.OpenApiOperationCatalog.ResourceInfo var6 = new io.karatelabs.ext.openapi.OpenApiOperationCatalog.ResourceInfo(var5, null, null, var4 == null ? null : String.valueOf(var4), null);
            return new io.karatelabs.ext.openapi.OpenApiOperationCatalog.ResourceOperation(var6, var3);
         }
      } else {
         return null;
      }
   }

   private static io.karatelabs.ext.openapi.OpenApiOperationCatalog.ResourceAction parseAction(String var0) {
      String var1 = var0.trim().toLowerCase();

      return switch (var1) {
         case "list" -> io.karatelabs.ext.openapi.OpenApiOperationCatalog.ResourceAction.LIST;
         case "create" -> io.karatelabs.ext.openapi.OpenApiOperationCatalog.ResourceAction.CREATE;
         case "read", "get" -> io.karatelabs.ext.openapi.OpenApiOperationCatalog.ResourceAction.READ;
         case "update", "put" -> io.karatelabs.ext.openapi.OpenApiOperationCatalog.ResourceAction.UPDATE;
         case "patch" -> io.karatelabs.ext.openapi.OpenApiOperationCatalog.ResourceAction.PATCH;
         case "delete", "remove" -> io.karatelabs.ext.openapi.OpenApiOperationCatalog.ResourceAction.DELETE;
         default -> null;
      };
   }
}
