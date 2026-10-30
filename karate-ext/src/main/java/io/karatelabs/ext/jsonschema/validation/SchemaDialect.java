package io.karatelabs.ext.jsonschema.validation;

import java.util.LinkedHashMap;
import java.util.Map;

public enum SchemaDialect {
   DRAFT_07("draft-07", "http://json-schema.org/draft-07/schema#", "http://json-schema.org/draft-07/schema", "https://json-schema.org/draft-07/schema"),
   V2019_09("2019-09", "https://json-schema.org/draft/2019-09/schema", "http://json-schema.org/draft/2019-09/schema"),
   V2020_12("2020-12", "https://json-schema.org/draft/2020-12/schema", "http://json-schema.org/draft/2020-12/schema"),
   OAS_3_0("oas-3.0"),
   OAS_3_1("oas-3.1", "https://spec.openapis.org/oas/3.1/dialect/base", "https://spec.openapis.org/oas/3.1/dialect"),
   SWAGGER_2_0("swagger-2.0"),
   ASYNCAPI_2("asyncapi-2");

   private static final Map<String, SchemaDialect> BY_ID = new LinkedHashMap<>();
   private static final Map<String, SchemaDialect> BY_URI = new LinkedHashMap<>();
   private final String id;
   private final String[] uris;

   private SchemaDialect(String var3, String... var4) {
      this.id = var3;
      this.uris = var4;
   }

   public String id() {
      return this.id;
   }

   public static SchemaDialect fromSchemaUri(String var0) {
      return var0 == null ? null : BY_URI.get(normalize(var0));
   }

   public static SchemaDialect fromId(String var0) {
      return var0 == null ? null : BY_ID.get(var0.trim());
   }

   private static String normalize(String var0) {
      String var1 = var0.trim();

      while (var1.endsWith("#") || var1.endsWith("/")) {
         var1 = var1.substring(0, var1.length() - 1);
      }

      return var1;
   }

   public boolean isModernCore() {
      return this == V2019_09 || this == V2020_12 || this == OAS_3_1;
   }

   public boolean hasPrefixItems() {
      return this == V2020_12 || this == OAS_3_1;
   }

   public boolean containsAnnotatesItems() {
      return this.hasPrefixItems();
   }

   public boolean refSiblingsApply() {
      return this.isModernCore();
   }

   public boolean hasNullable() {
      return this == OAS_3_0;
   }

   public boolean requiredIsDirectionScoped() {
      return this == OAS_3_0;
   }

   public boolean hasBooleanExclusive() {
      return this == SWAGGER_2_0 || this == OAS_3_0;
   }

   @Override
   public String toString() {
      return this.id;
   }

   static {
      for (SchemaDialect var3 : values()) {
         BY_ID.put(var3.id, var3);

         for (String var7 : var3.uris) {
            BY_URI.put(normalize(var7), var3);
         }
      }
   }
}
