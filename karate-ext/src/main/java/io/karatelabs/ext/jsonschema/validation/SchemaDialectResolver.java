package io.karatelabs.ext.jsonschema.validation;

import java.util.Map;

public final class SchemaDialectResolver {
   private SchemaDialectResolver() {
   }

   public static SchemaDialect resolve(Object var0, SchemaDialect var1, SchemaDialect var2) {
      SchemaDialect var3 = resolveFromSchemaKeyword(var0);
      if (var3 != null) {
         return var3;
      } else if (var1 != null) {
         return var1;
      } else {
         return var2 != null ? var2 : io.karatelabs.ext.jsonschema.validation.SchemaDialect.V2020_12;
      }
   }

   public static SchemaDialect resolveFromSchemaKeyword(Object var0) {
      if (var0 instanceof Map var1) {
         if (var1.get("$schema") instanceof String var2 && !var2.isBlank()) {
            SchemaDialect var4 = io.karatelabs.ext.jsonschema.validation.SchemaDialect.fromSchemaUri(var2);
            if (var4 == null) {
               throw new ValidationException("unknown JSON Schema dialect declared by $schema: '" + var2 + "'");
            } else {
               return var4;
            }
         } else {
            return null;
         }
      } else {
         return null;
      }
   }

   public static SchemaDialect resolveFromApiDocument(Object var0) {
      if (var0 instanceof Map var1) {
         if (var1.get("jsonSchemaDialect") instanceof String var2 && !var2.isBlank()) {
            SchemaDialect var7 = io.karatelabs.ext.jsonschema.validation.SchemaDialect.fromSchemaUri(var2);
            if (var7 == null) {
               throw new ValidationException("unknown dialect declared by jsonSchemaDialect: '" + var2 + "'");
            } else {
               return var7;
            }
         } else {
            Object var5 = var1.get("swagger");
            if (var5 != null) {
               if (versionText(var5).startsWith("2.0")) {
                  return io.karatelabs.ext.jsonschema.validation.SchemaDialect.SWAGGER_2_0;
               } else {
                  throw new ValidationException("unsupported swagger version: '" + var5 + "'");
               }
            } else {
               Object var6 = var1.get("openapi");
               if (var6 != null) {
                  String var4 = versionText(var6);
                  if (var4.startsWith("3.0")) {
                     return io.karatelabs.ext.jsonschema.validation.SchemaDialect.OAS_3_0;
                  } else if (!var4.startsWith("3.1") && !var4.startsWith("3.2")) {
                     throw new ValidationException("unsupported openapi version: '" + var6 + "'");
                  } else {
                     return io.karatelabs.ext.jsonschema.validation.SchemaDialect.OAS_3_1;
                  }
               } else {
                  return var1.containsKey("asyncapi") ? io.karatelabs.ext.jsonschema.validation.SchemaDialect.ASYNCAPI_2 : null;
               }
            }
         }
      } else {
         return null;
      }
   }

   private static String versionText(Object var0) {
      return String.valueOf(var0).trim();
   }
}
