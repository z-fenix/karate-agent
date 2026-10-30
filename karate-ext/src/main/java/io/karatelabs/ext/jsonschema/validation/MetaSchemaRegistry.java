package io.karatelabs.ext.jsonschema.validation;

import io.karatelabs.common.Json;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class MetaSchemaRegistry {
   private static final String RESOURCE_ROOT = "/io/karatelabs/ext/jsonschema/meta/";
   private static final Map<String, Object> schemaCache = new ConcurrentHashMap<>();

   private MetaSchemaRegistry() {
   }

   private static String normalizeDescriptorUri(String var0) {
      String var1 = var0.trim();
      int var2 = var1.indexOf(35);
      if (var2 >= 0) {
         var1 = var1.substring(0, var2);
      }

      while (var1.endsWith("/")) {
         var1 = var1.substring(0, var1.length() - 1);
      }

      String var3;
      if (var1.startsWith("https://")) {
         var3 = var1.substring(8);
      } else {
         if (!var1.startsWith("http://")) {
            return null;
         }

         var3 = var1.substring(7);
      }

      return var3.isEmpty() ? null : var3;
   }

   public static List<String> aliasUrisFor(String var0) {
      String var1 = var0 == null ? null : normalizeDescriptorUri(var0);
      if (var1 != null && io.karatelabs.ext.jsonschema.validation.MetaSchemaRegistry.MetaSchemaManifest.INSTANCE.resourcesByNormalizedUri.containsKey(var1)) {
         ArrayList<java.lang.String> var2 = new ArrayList(8);

         for (String var4 : List.of("https://", "http://")) {
            for (String var6 : List.of("", "/")) {
               var2.add(var4 + var1 + var6);
               var2.add(var4 + var1 + var6 + "#");
            }
         }

         return var2;
      } else {
         return List.of();
      }
   }

   public static Object schemaForUri(String var0) {
      String var1 = var0 == null ? null : normalizeDescriptorUri(var0);
      if (var1 == null) {
         return null;
      } else {
         io.karatelabs.ext.jsonschema.validation.MetaSchemaRegistry.MetaSchemaResource var2 = io.karatelabs.ext.jsonschema.validation.MetaSchemaRegistry.MetaSchemaManifest.INSTANCE.resourcesByNormalizedUri.get(var1);
         return var2 == null ? null : schemaCache.computeIfAbsent(var2.file(), var0x -> Json.parseStrict(readResourceText("/io/karatelabs/ext/jsonschema/meta/" + var0x), true));
      }
   }

   public static List<io.karatelabs.ext.jsonschema.validation.MetaSchemaRegistry.MetaSchemaResource> resources() {
      return io.karatelabs.ext.jsonschema.validation.MetaSchemaRegistry.MetaSchemaManifest.INSTANCE.resources;
   }

   public static Set<String> knownUris() {
      return Collections.unmodifiableSet(io.karatelabs.ext.jsonschema.validation.MetaSchemaRegistry.MetaSchemaManifest.INSTANCE.resourcesByNormalizedUri.keySet());
   }

   public static String source() {
      return io.karatelabs.ext.jsonschema.validation.MetaSchemaRegistry.MetaSchemaManifest.INSTANCE.source;
   }

   public static String license() {
      return io.karatelabs.ext.jsonschema.validation.MetaSchemaRegistry.MetaSchemaManifest.INSTANCE.license;
   }

   public static String licenseText() {
      return readResourceText("/io/karatelabs/ext/jsonschema/meta/" + io.karatelabs.ext.jsonschema.validation.MetaSchemaRegistry.MetaSchemaManifest.INSTANCE.licenseFile);
   }

   public static String licenseFile() {
      return io.karatelabs.ext.jsonschema.validation.MetaSchemaRegistry.MetaSchemaManifest.INSTANCE.licenseFile;
   }

   public static String licenseSha256() {
      return io.karatelabs.ext.jsonschema.validation.MetaSchemaRegistry.MetaSchemaManifest.INSTANCE.licenseSha256;
   }

   public static byte[] resourceBytes(String var0) {
      try {
         byte[] var2;
         try (InputStream var1 = MetaSchemaRegistry.class.getResourceAsStream("/io/karatelabs/ext/jsonschema/meta/" + var0)) {
            if (var1 == null) {
               throw new ValidationException("pinned meta-schema resource is missing from the jar: " + var0);
            }

            var2 = var1.readAllBytes();
         }

         return var2;
      } catch (IOException var6) {
         throw new UncheckedIOException(var6);
      }
   }

   private static String readResourceText(String var0) {
      try {
         String var2;
         try (InputStream var1 = MetaSchemaRegistry.class.getResourceAsStream(var0)) {
            if (var1 == null) {
               throw new ValidationException("pinned meta-schema resource is missing from the jar: " + var0);
            }

            var2 = new String(var1.readAllBytes(), StandardCharsets.UTF_8);
         }

         return var2;
      } catch (IOException var6) {
         throw new UncheckedIOException(var6);
      }
   }

   public record MetaSchemaResource(String uri, String file, String sha256) {
   }

   private static final class MetaSchemaManifest {
      static final io.karatelabs.ext.jsonschema.validation.MetaSchemaRegistry.MetaSchemaManifest INSTANCE = new io.karatelabs.ext.jsonschema.validation.MetaSchemaRegistry.MetaSchemaManifest();
      final String source;
      final String license;
      final String licenseFile;
      final String licenseSha256;
      final List<io.karatelabs.ext.jsonschema.validation.MetaSchemaRegistry.MetaSchemaResource> resources;
      final Map<String, io.karatelabs.ext.jsonschema.validation.MetaSchemaRegistry.MetaSchemaResource> resourcesByNormalizedUri = new LinkedHashMap<>();

      private MetaSchemaManifest() {
         Map<java.lang.String, java.util.List> var1 = (Map)Json.parseStrict(io.karatelabs.ext.jsonschema.validation.MetaSchemaRegistry.readResourceText("/io/karatelabs/ext/jsonschema/meta/manifest.json"), true);
         this.source = String.valueOf(var1.get("source"));
         this.license = String.valueOf(var1.get("license"));
         this.licenseFile = String.valueOf(var1.get("licenseFile"));
         this.licenseSha256 = String.valueOf(var1.get("licenseSha256"));
         ArrayList<io.karatelabs.ext.jsonschema.validation.MetaSchemaRegistry.MetaSchemaResource> var2 = new ArrayList();

         for (Object var4 : (List)var1.get("resources")) {
            Map<java.lang.String, Object> var5 = (Map)var4;
            io.karatelabs.ext.jsonschema.validation.MetaSchemaRegistry.MetaSchemaResource var6 = new io.karatelabs.ext.jsonschema.validation.MetaSchemaRegistry.MetaSchemaResource(String.valueOf(var5.get("uri")), String.valueOf(var5.get("file")), String.valueOf(var5.get("sha256")));
            String var7 = io.karatelabs.ext.jsonschema.validation.MetaSchemaRegistry.normalizeDescriptorUri(var6.uri());
            if (var7 == null) {
               throw new ValidationException("pinned meta-schema uri is not an absolute http(s) URI: " + var6.uri());
            }

            var2.add(var6);
            this.resourcesByNormalizedUri.put(var7, var6);
         }

         this.resources = Collections.unmodifiableList(var2);
      }
   }
}
