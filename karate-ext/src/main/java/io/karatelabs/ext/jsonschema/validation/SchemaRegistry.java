package io.karatelabs.ext.jsonschema.validation;

import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.function.Function;

public class SchemaRegistry {
   final SchemaDialect dialect;
   final Map<String, Object> schemasByUri = new LinkedHashMap<>();
   final Map<String, Object> schemasByAnchor = new LinkedHashMap<>();
   final Map<Object, String> baseUrisBySchema = new IdentityHashMap<>();
   private Function<String, Object> externalResolver;

   public SchemaRegistry(SchemaDialect var1) {
      this.dialect = var1;
   }

   public SchemaRegistry withExternalResolver(Function<String, Object> var1) {
      this.externalResolver = var1;
      return this;
   }

   public SchemaRegistry register(String var1, Object var2) {
      String var3 = var1 == null ? "" : var1;
      this.indexSchema(var2, var3);
      this.schemasByUri.put(var3, var2);
      return this;
   }

   public CompiledSchema compile(Object var1) {
      this.indexSchema(var1, "");
      this.discoverMetaSchemas();
      return new CompiledSchema(this, var1);
   }

   private void discoverMetaSchemas() {
      for (int var1 = 0; var1 < 16; var1++) {
         ArrayList<java.lang.String> var2 = new ArrayList();

         for (Entry var4 : new ArrayList<>(this.baseUrisBySchema.entrySet())) {
            if (var4.getKey() instanceof Map var5 && var5.get("$ref") instanceof String var6) {
               int var13 = var6.indexOf(35);
               String var8 = var13 < 0 ? var6 : var6.substring(0, var13);
               if (!var8.isEmpty()) {
                  String var9 = resolveUri((String)var4.getValue(), var8);
                  if (!this.schemasByUri.containsKey(var9) && !var2.contains(var9) && io.karatelabs.ext.jsonschema.validation.MetaSchemaRegistry.schemaForUri(var9) != null) {
                     var2.add(var9);
                  }
               }
            }
         }

         if (var2.isEmpty()) {
            return;
         }

         for (String var11 : (Iterable<String>) (Iterable<?>) (var2)) {
            this.loadReferencedSchema(var11, var11);
         }
      }
   }

   private void indexSchema(Object var1, String var2) {
      this.a(var1, var2, Collections.newSetFromMap(new IdentityHashMap<>()));
   }

   private void a(Object var1, String var2, Set<Object> var3) {
      if (var1 instanceof Map) {
         if (var3.add(var1)) {
            Map<java.lang.String, java.lang.Object> var4 = (Map)var1;
            boolean var5 = !this.dialect.refSiblingsApply() && var4.containsKey("$ref");
            if (!var5 && (var4.containsKey("$id") ? var4.get("$id") : var4.get("id")) instanceof String var7 && !var7.isEmpty()) {
               if (!this.dialect.isModernCore() && var7.startsWith("#")) {
                  this.schemasByAnchor.put(var2 + var7, var1);
               } else {
                  int var8 = var7.indexOf(35);
                  String var9 = var8 < 0 ? var7 : var7.substring(0, var8);
                  String var10 = var8 < 0 ? null : var7.substring(var8 + 1);
                  if (!var9.isEmpty()) {
                     var2 = resolveUri(var2, var9);
                     this.schemasByUri.put(var2, var1);
                  }

                  if (var10 != null && !var10.isEmpty()) {
                     this.schemasByAnchor.put(var2 + "#" + var10, var1);
                  }
               }
            }

            if (var4.get("$anchor") instanceof String var12) {
               this.schemasByAnchor.put(var2 + "#" + var12, var1);
            }

            if (var4.get("$dynamicAnchor") instanceof String var13) {
               this.schemasByAnchor.put(var2 + "#" + var13, var1);
            }

            if (!this.schemasByUri.containsKey(var2)) {
               this.schemasByUri.put(var2, var1);
            }

            this.baseUrisBySchema.put(var1, var2);

            for (String var20 : io.karatelabs.ext.jsonschema.validation.SchemaKeywords.SINGLE_SCHEMA_KEYWORDS) {
               this.a(var4.get(var20), var2, var3);
            }

            for (String var21 : io.karatelabs.ext.jsonschema.validation.SchemaKeywords.SCHEMA_ARRAY_KEYWORDS) {
               this.b(var4.get(var21), var2, var3);
            }

            for (String var22 : io.karatelabs.ext.jsonschema.validation.SchemaKeywords.SCHEMA_MAP_KEYWORDS) {
               if (var4.get(var22) instanceof Map var24) {
                  for (Object var11 : var24.values()) {
                     this.a(var11, var2, var3);
                  }
               }
            }

            Object var17 = var4.get("items");
            if (var17 instanceof List) {
               this.b(var17, var2, var3);
            } else {
               this.a(var17, var2, var3);
            }

            if (var4.get("dependencies") instanceof Map var23) {
               for (Object var29 : var23.values()) {
                  this.a(var29, var2, var3);
               }
            }
         }
      }
   }

   private void b(Object var1, String var2, Set<Object> var3) {
      if (var1 instanceof List) {
         for (Object var6 : (List)var1) {
            this.a(var6, var2, var3);
         }
      }
   }

   String baseUriFor(Object var1, String var2) {
      String var3 = this.baseUrisBySchema.get(var1);
      return var3 == null ? var2 : var3;
   }

   Object resolveReference(String var1, String var2) {
      int var3 = var1.indexOf(35);
      String var4 = var3 < 0 ? var1 : var1.substring(0, var3);
      String var5 = var3 < 0 ? null : var1.substring(var3 + 1);
      String var6 = var4.isEmpty() ? var2 : resolveUri(var2, var4);
      if (var5 == null || var5.isEmpty()) {
         return this.loadReferencedSchema(var6, var1);
      } else if (var5.startsWith("/")) {
         return resolveJsonPointer(this.loadReferencedSchema(var6, var1), var5, var1);
      } else {
         Object var7 = this.schemasByAnchor.get(var6 + "#" + var5);
         if (var7 == null) {
            Object var8 = this.loadReferencedSchema(var6, var1);
            var7 = this.schemasByAnchor.get(this.baseUriFor(var8, var6) + "#" + var5);
         }

         if (var7 == null) {
            throw new ValidationException("$ref anchor not found: '" + var1 + "' (resolved to '" + var6 + "#" + var5 + "')");
         } else {
            return var7;
         }
      }
   }

   private Object loadReferencedSchema(String var1, String var2) {
      Object var3 = this.schemasByUri.get(var1);
      if (var3 == null) {
         var3 = io.karatelabs.ext.jsonschema.validation.MetaSchemaRegistry.schemaForUri(var1);
         if (var3 != null) {
            for (String var5 : io.karatelabs.ext.jsonschema.validation.MetaSchemaRegistry.aliasUrisFor(var1)) {
               Object var6 = this.schemasByUri.get(var5);
               if (var6 != null) {
                  var3 = var6;
                  break;
               }
            }
         } else if (this.externalResolver != null) {
            var3 = this.externalResolver.apply(var1);
         }

         if (var3 != null) {
            this.register(var1, var3);
         }
      }

      if (var3 == null) {
         throw new ValidationException("$ref target not registered: '" + var2 + "' (resolved to '" + var1 + "')");
      } else {
         return var3;
      }
   }

   static Object resolveJsonPointer(Object var0, String var1, String var2) {
      Object var3 = var0;
      String[] var4 = var1.split("/", -1);

      for (int var5 = 1; var5 < var4.length; var5++) {
         String var6 = var4[var5];
         String var7 = decodeJsonPointerSegment(var6, var2).replace("~1", "/").replace("~0", "~");
         if (var3 instanceof Map var9) {
            if (!var9.containsKey(var7)) {
               throw new ValidationException("$ref pointer does not resolve: '" + var2 + "' (no '" + var7 + "')");
            }

            var3 = var9.get(var7);
         } else {
            if (!(var3 instanceof List var8)) {
               throw new ValidationException("$ref pointer does not resolve: '" + var2 + "'");
            }

            int var10;
            try {
               var10 = Integer.parseInt(var7);
            } catch (NumberFormatException var12) {
               throw new ValidationException("$ref pointer expects an array index: '" + var2 + "' (got '" + var7 + "')");
            }

            if (var10 < 0 || var10 >= var8.size()) {
               throw new ValidationException("$ref pointer index out of range: '" + var2 + "'");
            }

            var3 = var8.get(var10);
         }
      }

      return var3;
   }

   static String decodeJsonPointerSegment(String var0, String var1) {
      if (var0.indexOf(37) < 0) {
         return var0;
      } else {
         ByteArrayOutputStream var2 = new ByteArrayOutputStream(var0.length());

         for (int var3 = 0; var3 < var0.length(); var3++) {
            char var4 = var0.charAt(var3);
            if (var4 != '%') {
               var2.writeBytes(String.valueOf(var4).getBytes(StandardCharsets.UTF_8));
            } else {
               if (var3 + 2 >= var0.length()) {
                  throw new ValidationException("$ref has an incomplete percent escape: '" + var1 + "'");
               }

               int var5 = Character.digit(var0.charAt(var3 + 1), 16);
               int var6 = Character.digit(var0.charAt(var3 + 2), 16);
               if (var5 < 0 || var6 < 0) {
                  throw new ValidationException("$ref has an invalid percent escape: '" + var1 + "'");
               }

               var2.write((var5 << 4) + var6);
               var3 += 2;
            }
         }

         return var2.toString(StandardCharsets.UTF_8);
      }
   }

   static String resolveUri(String var0, String var1) {
      if (var1 == null || var1.isEmpty()) {
         return var0;
      } else if (var0 != null && !var0.isEmpty()) {
         try {
            URI var2 = new URI(var1);
            if (var2.isAbsolute()) {
               return var2.toString();
            } else {
               URI var3 = new URI(var0);
               return var3.isOpaque() ? var1 : var3.resolve(var2).toString();
            }
         } catch (URISyntaxException var4) {
            return var1;
         }
      } else {
         return var1;
      }
   }
}
