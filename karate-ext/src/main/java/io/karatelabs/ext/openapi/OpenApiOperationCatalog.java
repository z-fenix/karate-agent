package io.karatelabs.ext.openapi;

import io.karatelabs.common.Resource;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class OpenApiOperationCatalog {
   private final OpenApiDocument document;
   private final List<io.karatelabs.ext.openapi.OpenApiOperationCatalog.OperationInfo> operations = new ArrayList<>();
   private final List<io.karatelabs.ext.openapi.OpenApiOperationCatalog.ResourceInfo> resources = new ArrayList<>();
   private final Map<String, io.karatelabs.ext.openapi.OpenApiOperationCatalog.ResourceOperation> resourceOperations = new LinkedHashMap<>();
   private final OpenApiOperationMatcher operationMatcher;

   public static OpenApiOperationCatalog loadFromPath(String var0) {
      return loadFromResource(Resource.path(var0));
   }

   public static OpenApiOperationCatalog loadFromResource(Resource var0) {
      return fromDocument(new OpenApiDocument(var0));
   }

   public static OpenApiOperationCatalog fromDocument(OpenApiDocument var0) {
      return new OpenApiOperationCatalog(var0, var0.basePaths());
   }

   public static OpenApiOperationCatalog fromDocument(OpenApiDocument var0, List<String> var1) {
      return new OpenApiOperationCatalog(var0, var1);
   }

   private OpenApiOperationCatalog(OpenApiDocument var1, List<String> var2) {
      this.document = var1;
      ArrayList var3 = new ArrayList();

      for (OpenApiPathItem var5 : var1.paths.values()) {
         for (io.karatelabs.ext.openapi.OpenApiOperation var7 : var5.e) {
            OpenApiResponse var8 = preferredResponse(var7);
            int var9 = var8 == null ? 200 : parseStatusCode(var8.b, 200);
            Object var10 = var8 == null ? null : responseExample(var8);
            OpenApiSchemaNode var11 = requestSchema(var7);
            this.operations
               .add(
                  new io.karatelabs.ext.openapi.OpenApiOperationCatalog.OperationInfo(
                     var7.g, var7.b, var5.c, var9, var10, var11, requestExample(var7), var7.e, var7.h, var8 == null ? List.of() : requiredResponseFields(var8), var8 == null ? null : jsonResponseSchema(var8)
                  )
               );
            LinkedHashMap var12 = new LinkedHashMap();
            var12.put("method", var7.b);
            var12.put("path", var5.c);
            var12.put("opId", var7.g);
            var12.put("idx", this.operations.size() - 1);
            var3.add(var12);
         }
      }

      this.operationMatcher = new OpenApiOperationMatcher(var3, var2);
      this.indexResources();
   }

   public List<io.karatelabs.ext.openapi.OpenApiOperationCatalog.OperationInfo> operations() {
      return this.operations;
   }

   public List<io.karatelabs.ext.openapi.OpenApiOperationCatalog.ResourceInfo> resources() {
      return this.resources;
   }

   public io.karatelabs.ext.openapi.OpenApiOperationCatalog.ResourceOperation resourceOperationFor(io.karatelabs.ext.openapi.OpenApiOperationCatalog.OperationInfo var1) {
      return this.resourceOperations.get(operationKey(var1.b(), var1.c()));
   }

   public OpenApiDocument document() {
      return this.document;
   }

   public io.karatelabs.ext.openapi.OpenApiOperationCatalog.OperationInfo findOperation(String var1, String var2) {
      var var3 = this.operationMatcher.a(var1, var2);
      return var3 == null ? null : this.operations.get((Integer)var3.get("idx"));
   }

   private void indexResources() {
      HashSet var1 = new HashSet();

      for (io.karatelabs.ext.openapi.OpenApiOperationCatalog.OperationInfo var3 : this.operations) {
         var1.add(var3.c());
      }

      LinkedHashMap var8 = new LinkedHashMap();

      for (String var4 : (Iterable<String>) (Iterable<?>) (var1)) {
         String var5 = trailingPathVariable(var4);
         if (var5 != null) {
            String var6 = var4.substring(0, var4.length() - ("/{" + var5 + "}").length());
            if (!var6.isEmpty() && var1.contains(var6) && trailingPathVariable(var6) == null) {
               var8.computeIfAbsent(var6, var4x -> new io.karatelabs.ext.openapi.OpenApiOperationCatalog.ResourceInfo(lastPathSegment(var6), var6, var4, var5, this.createSchemaForCollectionPath(var6)));
            }
         }
      }

      for (io.karatelabs.ext.openapi.OpenApiOperationCatalog.OperationInfo var11 : this.operations) {
         for (io.karatelabs.ext.openapi.OpenApiOperationCatalog.ResourceInfo var13 : (Iterable<io.karatelabs.ext.openapi.OpenApiOperationCatalog.ResourceInfo>) (Iterable<?>) (var8.values())) {
            io.karatelabs.ext.openapi.OpenApiOperationCatalog.ResourceAction var7 = classifyResourceAction(var11, var13);
            if (var7 != null) {
               this.resourceOperations.put(operationKey(var11.b(), var11.c()), new io.karatelabs.ext.openapi.OpenApiOperationCatalog.ResourceOperation(var13, var7));
               break;
            }
         }
      }

      this.resources.addAll(var8.values());
   }

   private static io.karatelabs.ext.openapi.OpenApiOperationCatalog.ResourceAction classifyResourceAction(io.karatelabs.ext.openapi.OpenApiOperationCatalog.OperationInfo var0, io.karatelabs.ext.openapi.OpenApiOperationCatalog.ResourceInfo var1) {
      String var2 = var0.b() == null ? "" : var0.b().toUpperCase();
      if (var0.c().equals(var1.b())) {
         return switch (var2) {
            case "GET" -> io.karatelabs.ext.openapi.OpenApiOperationCatalog.ResourceAction.LIST;
            case "POST" -> io.karatelabs.ext.openapi.OpenApiOperationCatalog.ResourceAction.CREATE;
            default -> null;
         };
      } else if (var0.c().equals(var1.c())) {
         return switch (var2) {
            case "GET" -> io.karatelabs.ext.openapi.OpenApiOperationCatalog.ResourceAction.READ;
            case "PUT" -> io.karatelabs.ext.openapi.OpenApiOperationCatalog.ResourceAction.UPDATE;
            case "PATCH" -> io.karatelabs.ext.openapi.OpenApiOperationCatalog.ResourceAction.PATCH;
            case "DELETE" -> io.karatelabs.ext.openapi.OpenApiOperationCatalog.ResourceAction.DELETE;
            default -> null;
         };
      } else {
         return null;
      }
   }

   private static String trailingPathVariable(String var0) {
      int var1 = var0.lastIndexOf(47);
      if (var1 < 0) {
         return null;
      } else {
         String var2 = var0.substring(var1 + 1);
         if (var2.length() >= 2 && var2.charAt(0) == '{' && var2.charAt(var2.length() - 1) == '}') {
            String var3 = var2.substring(1, var2.length() - 1);
            return var3.isEmpty() ? null : var3;
         } else {
            return null;
         }
      }
   }

   private static String lastPathSegment(String var0) {
      int var1 = var0.lastIndexOf(47);
      return var1 >= 0 ? var0.substring(var1 + 1) : var0;
   }

   private OpenApiSchemaNode createSchemaForCollectionPath(String var1) {
      for (io.karatelabs.ext.openapi.OpenApiOperationCatalog.OperationInfo var3 : this.operations) {
         if (var1.equals(var3.c()) && "POST".equalsIgnoreCase(var3.b())) {
            return var3.f();
         }
      }

      return null;
   }

   private static String operationKey(String var0, String var1) {
      return (var0 == null ? "" : var0.toUpperCase()) + " " + var1;
   }

   private static OpenApiResponse preferredResponse(io.karatelabs.ext.openapi.OpenApiOperation var0) {
      OpenApiResponse var1 = null;

      for (OpenApiResponse var3 : var0.d) {
         if (var1 == null) {
            var1 = var3;
         }

         if (var3.b != null && var3.b.length() == 3 && var3.b.charAt(0) == '2') {
            return var3;
         }
      }

      return var1;
   }

   private static List<String> requiredResponseFields(OpenApiResponse var0) {
      for (OpenApiResponseContent var2 : var0.e) {
         if (var2.d != null) {
            ArrayList var3 = new ArrayList();
            collectRequiredFields(var2.d.getRootNode(), var3);
            return var3;
         }
      }

      return List.of();
   }

   private static void collectRequiredFields(OpenApiSchemaNode var0, List<String> var1) {
      if ("allOf".equals(var0.type())) {
         for (OpenApiSchemaNode var3 : var0.children) {
            collectRequiredFields(var3, var1);
         }
      } else if ("object".equals(var0.type())) {
         for (OpenApiSchemaNode var5 : var0.children) {
            if (var5.isRequired() && var5.name() != null && !var1.contains(var5.name())) {
               var1.add(var5.name());
            }
         }
      }
   }

   private static Object responseExample(OpenApiResponse var0) {
      OpenApiSchemaNode var1 = firstResponseSchema(var0);
      if (var1 == null) {
         return null;
      } else {
         var1.markTreePresent();
         return var1.generateSample();
      }
   }

   private static OpenApiSchemaNode firstResponseSchema(OpenApiResponse var0) {
      for (OpenApiResponseContent var2 : var0.e) {
         if (var2.d != null) {
            return var2.d.getRootNode();
         }
      }

      return null;
   }

   private static OpenApiSchemaNode jsonResponseSchema(OpenApiResponse var0) {
      for (OpenApiResponseContent var2 : var0.e) {
         String var3 = var2.c == null ? "" : var2.c.toLowerCase().split(";")[0].trim();
         if (var2.d != null && (var3.equals("application/json") || var3.endsWith("+json"))) {
            return var2.d.getRootNode();
         }
      }

      return null;
   }

   private static OpenApiSchemaNode requestSchema(io.karatelabs.ext.openapi.OpenApiOperation var0) {
      if (var0.c == null) {
         return null;
      } else {
         for (OpenApiMediaType var2 : var0.c.d) {
            if (var2.d != null) {
               return var2.d.getRootNode();
            }
         }

         return null;
      }
   }

   private static Object requestExample(io.karatelabs.ext.openapi.OpenApiOperation var0) {
      if (var0.c == null) {
         return null;
      } else {
         for (OpenApiMediaType var2 : var0.c.d) {
            if (var2.g != null) {
               return var2.g;
            }
         }

         return null;
      }
   }

   private static int parseStatusCode(String var0, int var1) {
      try {
         return Integer.parseInt(var0.trim());
      } catch (NumberFormatException var3) {
         return var1;
      }
   }

   public static enum ResourceAction {
      LIST,
      CREATE,
      READ,
      UPDATE,
      PATCH,
      DELETE;
   }

   public record OperationInfo(String a, String b, String c, int d, Object e, OpenApiSchemaNode f, Object g, String h, List<String> i, List<String> j, OpenApiSchemaNode k) {}

   public record ResourceOperation(io.karatelabs.ext.openapi.OpenApiOperationCatalog.ResourceInfo a, io.karatelabs.ext.openapi.OpenApiOperationCatalog.ResourceAction b) {}

   public record ResourceInfo(String a, String b, String c, String d, OpenApiSchemaNode e) {

   }
}
