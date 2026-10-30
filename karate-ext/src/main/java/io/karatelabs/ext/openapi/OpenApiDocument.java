package io.karatelabs.ext.openapi;

import io.karatelabs.common.Resource;
import io.karatelabs.ext.jsonschema.validation.SchemaDialectResolver;
import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OpenApiDocument {
   private static final Logger logger = LoggerFactory.getLogger(OpenApiDocument.class);
   public final String specType;
   public final String description;
   public final String title;
   public final Resource resource;
   public final Map<String, Object> rawDocument;
   public Map<String, OpenApiPathItem> paths = new LinkedHashMap<>();
   private final List<String> basePaths;
   private final List<String> warnings;
   private io.karatelabs.ext.jsonschema.validation.SchemaDialect schemaDialect;
   private final Map<String, SchemaDefinition> schemaCache = new HashMap<>();

   public List<String> basePaths() {
      return this.basePaths;
   }

   public List<String> warnings() {
      return this.warnings;
   }

   public Map<String, Object> info() {
      Map<String, Object> var1 = (Map)this.rawDocument.get("info");
      if (var1 == null) {
         var1 = new HashMap();
      }

      HashMap<java.lang.String, java.lang.Object> var2 = new HashMap((Map)var1);
      if (this.rawDocument.containsKey("openapi")) {
         var2.put("specType", "openapi");
      } else {
         var2.put("specType", "swagger");
      }

      var2.put("version", var1.get("version"));
      return var2;
   }

   private Map<String, Object> infoMap() {
      Map var1 = (Map)this.rawDocument.get("info");
      return var1 != null ? var1 : Collections.emptyMap();
   }

   public String version() {
      return (String)this.infoMap().get("version");
   }

   public io.karatelabs.ext.jsonschema.validation.SchemaDialect schemaDialect() {
      if (this.schemaDialect == null) {
         io.karatelabs.ext.jsonschema.validation.SchemaDialect var1 = io.karatelabs.ext.jsonschema.validation.SchemaDialectResolver.resolveFromApiDocument(this.rawDocument);
         if (var1 == null) {
            var1 = io.karatelabs.ext.jsonschema.validation.SchemaDialect.OAS_3_1;
            logger.warn("{} declares no openapi/swagger/asyncapi version — validating as {}", this.resource, var1);
         }

         this.schemaDialect = var1;
      }

      return this.schemaDialect;
   }

   Map<String, Object> loadExternalReference(String var1) {
      if (var1 != null && !var1.isEmpty()) {
         try {
            Resource var2 = var1.startsWith("./") ? this.resource.resolve(var1) : Resource.from(URI.create(var1).toURL());
            return (new OpenApiDocument(var2, null)).rawDocument;
         } catch (Exception var3) {
            logger.warn("failed to resolve external reference: {} - {}", var1, var3.getMessage());
            return null;
         }
      } else {
         return null;
      }
   }

   public String termsOfService() {
      return (String)this.infoMap().get("termsOfService");
   }

   public String contactName() {
      Map<java.lang.String, java.lang.String> var1 = (Map)this.infoMap().get("contact");
      return var1 != null ? (String)var1.get("name") : null;
   }

   public String contactEmail() {
      Map<java.lang.String, java.lang.String> var1 = (Map)this.infoMap().get("contact");
      return var1 != null ? (String)var1.get("email") : null;
   }

   public String contactUrl() {
      Map<java.lang.String, java.lang.String> var1 = (Map)this.infoMap().get("contact");
      return var1 != null ? (String)var1.get("url") : null;
   }

   public String licenseName() {
      Map<java.lang.String, java.lang.String> var1 = (Map)this.infoMap().get("license");
      return var1 != null ? (String)var1.get("name") : null;
   }

   public String licenseUrl() {
      Map<java.lang.String, java.lang.String> var1 = (Map)this.infoMap().get("license");
      return var1 != null ? (String)var1.get("url") : null;
   }

   public List<String> operationMethods() {
      ArrayList<java.lang.String> var1 = new ArrayList();

      for (OpenApiPathItem var3 : this.paths.values()) {
         for (OpenApiOperation var5 : var3.e) {
            if (!var1.contains(var5.b)) {
               var1.add(var5.b);
            }
         }
      }

      return var1;
   }

   public static Map<String, Object> parseApiDocument(Resource var0) {
      Object var1 = io.karatelabs.ext.sourcedoc.SourceDocument.parse(var0.getRelativePath(), var0.getText()).value();
      if (var1 instanceof Map var2) {
         return var2;
      } else {
         throw new IllegalArgumentException(
            "not an API description: '"
               + var0.getRelativePath()
               + "' parsed to "
               + (var1 == null ? "nothing" : "a " + var1.getClass().getSimpleName())
               + ", not an object"
         );
      }
   }

   void warmSchemaSamples() {
      for (OpenApiPathItem var2 : this.paths.values()) {
         for (OpenApiOperation var4 : var2.e) {
            if (var4.c != null) {
               for (OpenApiMediaType var6 : var4.c.d) {
                  if (var6.d != null) {
                     OpenApiSchemaNode var7 = var6.d.getRootNode();
                     var7.markTreePresent();
                     Object var8 = var7.generateSample();
                  }
               }
            }

            for (OpenApiResponse var12 : var4.d) {
               for (OpenApiResponseContent var14 : var12.e) {
                  if (var14.d != null) {
                     OpenApiSchemaNode var9 = var14.d.getRootNode();
                     var9.markTreePresent();
                     Object var10 = var9.generateSample();
                  }
               }
            }
         }
      }
   }

   public OpenApiDocument(File var1) {
      this(Resource.from(var1.toPath()), null);
   }

   public OpenApiDocument(Resource var1) {
      this(var1, null);
   }

   public OpenApiDocument(Resource var1, Map<String, Object> var2) {
      this.resource = var1;
      this.rawDocument = var2 == null ? parseApiDocument(var1) : var2;
      if (this.rawDocument == null) {
         throw new IllegalArgumentException("data is null");
      } else {
         if (this.rawDocument.containsKey("openapi")) {
            this.specType = "openapi";
         } else {
            this.specType = "swagger";
         }

         Map<java.lang.String, java.lang.String> var3 = (Map)this.rawDocument.get("info");
         if (var3 == null) {
            this.title = null;
            this.description = null;
         } else {
            this.title = (String)var3.get("title");
            this.description = (String)var3.get("description");
         }

         ArrayList var4 = new ArrayList();
         this.basePaths = io.karatelabs.ext.openapi.ServerBasePathResolver.a(this.rawDocument, var4);
         this.warnings = List.copyOf(var4);

         for (String var6 : this.warnings) {
            logger.warn("{}: {}", this, var6);
         }

         Map var10 = (Map)this.rawDocument.get("paths");
         if (var10 != null) {
            for (Entry var7 : (Iterable<Entry>) (Iterable<?>) (var10.entrySet())) {
               String var8 = (String)var7.getKey();
               if (var8 == null) {
                  logger.warn("path key is null: {} - {}", var7.getValue(), this);
               } else {
                  Map var9 = (Map)var7.getValue();
                  this.paths.put(var8, new OpenApiPathItem(this, var8, var9));
               }
            }
         }
      }
   }

   public SchemaDefinition resolveSchemaReference(OpenApiSchemaNode var1, Map<String, Object> var2) {
      if (var2 == null) {
         return null;
      } else {
         String var3 = (String)var2.get("$ref");
         if (var3 == null) {
            return new SchemaDefinition(this, var2);
         } else {
            int var4 = var3.indexOf("#/");
            if (var4 == -1) {
               return null;
            } else if (var4 == 0) {
               String var10 = var3.substring(var4 + 2);
               return resolveReferenceInDocument(var1, this, var3, var10, var2);
            } else {
               String var6 = var3.substring(0, var4);
               String var5 = var3.substring(var4 + 2);

               try {
                  Resource var7;
                  if (var6.startsWith("./")) {
                     var7 = this.resource.resolve(var6);
                  } else {
                     var7 = Resource.from(URI.create(var6).toURL());
                  }

                  OpenApiDocument var8 = new OpenApiDocument(var7, null);
                  return resolveReferenceInDocument(var1, var8, var3, var5, var2);
               } catch (Exception var9) {
                  logger.warn("failed to resolve external reference: {} - {}", var6, var9.getMessage());
                  return null;
               }
            }
         }
      }
   }

   private static SchemaDefinition resolveReferenceInDocument(OpenApiSchemaNode var0, OpenApiDocument var1, String var2, String var3, Map<String, Object> var4) {
      if (var0 != null) {
         if (var0.depth > 20) {
            for (String var14 : var0.referencePath()) {
               var1.schemaCache.put(var14, null);
            }

            return null;
         }

         if (var0.hasReferenceAncestor(var2)) {
            var var11 = var0.referencePath();
            return null;
         }
      }

      if (var1.schemaCache.containsKey(var2)) {
         return var1.schemaCache.get(var2);
      } else {
         String[] var5 = var3.split("/");
         String var6 = null;
         Map<String, Object> var7 = var1.rawDocument;

         for (int var8 = 0; var8 < var5.length; var8++) {
            String var9 = var5[var8];
            Object var10 = var7.get(var9);
            if (!(var10 instanceof Map)) {
               return null;
            }

            var7 = (Map)var10;
            if (var8 == var5.length - 1) {
               var6 = var9;
            }
         }

         if (var4.containsKey("description") || var4.containsKey("summary")) {
            HashMap<java.lang.String, java.lang.Object> var15 = new HashMap((Map)var7);
            var15.put("description", var4.get("description"));
            var15.put("summary", var4.get("summary"));
            var7 = var15;
         }

         SchemaDefinition var16 = new SchemaDefinition(var1, var2, var3, var6, (Map<String, Object>)var7);
         var1.schemaCache.put(var2, var16);
         return var16;
      }
   }

   public List<Map<String, Object>> pathSummaries() {
      ArrayList<java.util.Map<java.lang.String,java.lang.Object>> var1 = new ArrayList();

      for (OpenApiPathItem var3 : this.paths.values()) {
         var1.add(var3.toMap());
      }

      return var1;
   }

   public String defaultServerUrl() {
      if ("openapi".equals(this.specType)) {
         List var1 = (List)this.rawDocument.get("servers");
         if (var1 != null && !var1.isEmpty()) {
            Map<java.lang.String, java.lang.String> var2 = (Map)var1.get(0);
            String var3 = (String)var2.get("url");
            if (var3 == null || !var3.startsWith("http://") && !var3.startsWith("https://")) {
               return null;
            }

            return var3;
         }
      } else {
         String var6 = (String)this.rawDocument.get("host");
         if (var6 != null) {
            String var7 = (String)this.rawDocument.get("basePath");
            List var8 = (List)this.rawDocument.get("schemes");
            String var4 = var8 != null && !var8.isEmpty() ? (String)var8.get(0) : "https";
            StringBuilder var5 = new StringBuilder(var4).append("://").append(var6);
            if (var7 != null && !var7.isEmpty()) {
               if (!var7.startsWith("/")) {
                  var5.append("/");
               }

               var5.append(var7);
            }

            return var5.toString();
         }
      }

      return null;
   }

   @Override
   public String toString() {
      return this.resource == null ? this.title : this.resource.getRelativePath() + " " + this.title;
   }
}
