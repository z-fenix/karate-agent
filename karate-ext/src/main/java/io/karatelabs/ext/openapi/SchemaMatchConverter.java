package io.karatelabs.ext.openapi;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public final class SchemaMatchConverter {
   private static final String ARRAY_MATCH_PREFIX = "#[] ";
   private static final List<String> MARKER_SCHEMA_KEYWORDS = List.of("type", "properties", "required", "items", "enum", "pattern", "format", "nullable", "allOf");
   private static final List<String> IGNORED_SCHEMA_ANNOTATIONS = List.of(
      "description",
      "title",
      "summary",
      "example",
      "examples",
      "value",
      "default",
      "deprecated",
      "readOnly",
      "writeOnly",
      "xml",
      "externalDocs",
      "$comment",
      "$id",
      "$schema",
      "$ref",
      "name",
      "in"
   );

   private SchemaMatchConverter() {
   }

   public static Object toMatchTemplateFromValue(Object var0) {
      return toMatchTemplateFromMap(io.karatelabs.ext.openapi.JsonSchemaInferrer.a(var0));
   }

   public static Object toMatchTemplate(OpenApiSchemaNode var0) {
      return buildMatchTemplateFromNode(var0, false);
   }

   public static List<Map<String, Object>> unsupportedMarkerDiagnostics(OpenApiSchemaNode var0) {
      ArrayList<java.util.Map<java.lang.String,java.lang.Object>> var1 = new ArrayList();
      collectUnsupportedMarkers(var0, "", false, var1);
      return var1;
   }

   private static void collectUnsupportedMarkers(OpenApiSchemaNode var0, String var1, boolean var2, List<Map<String, Object>> var3) {
      if (var0 != null) {
         boolean var4 = var2 || var0.schemaDefinition.nullable();
         String var5 = var0.type();

         for (String var7 : var0.schemaDefinition.rawSchema.keySet()) {
            if (!IGNORED_SCHEMA_ANNOTATIONS.contains(var7)) {
               if (!MARKER_SCHEMA_KEYWORDS.contains(var7)) {
                  var3.add(markerDiagnostic(var1, var7, unsupportedKeywordReason(var7)));
               } else {
                  if (("enum".equals(var7) || "pattern".equals(var7)) && (var2 || var4)) {
                     var3.add(markerDiagnostic(var1, var7, "a predicate marker cannot carry the ## optional prefix"));
                  }

                  if ("format".equals(var7) && !"uuid".equals(var0.schemaDefinition.format())) {
                     var3.add(markerDiagnostic(var1, "format", "only format:uuid has a marker"));
                  }

                  if ("type".equals(var7) && "integer".equals(var5)) {
                     var3.add(markerDiagnostic(var1, "type", "integer widens to #number — the marker vocabulary has one numeric type"));
                  }
               }
            }
         }

         if ("allOf".equals(var5)) {
            for (OpenApiSchemaNode var11 : var0.children) {
               if (!(buildMatchTemplateFromNode(var11, false) instanceof Map)) {
                  var3.add(markerDiagnostic(var1, "allOf", "a non-object member cannot be merged into one template"));
                  break;
               }
            }
         }

         for (OpenApiSchemaNode var12 : var0.children) {
            String var8 = var12.name() == null ? var1 + "/*" : var1 + "/" + var12.name();
            collectUnsupportedMarkers(var12, var8, !var12.isRequired(), var3);
         }
      }
   }

   private static Map<String, Object> markerDiagnostic(String var0, String var1, String var2) {
      LinkedHashMap<java.lang.String, java.lang.Object> var3 = new LinkedHashMap();
      var3.put("path", var0.isEmpty() ? "/" : var0);
      var3.put("keyword", var1);
      var3.put("note", var2);
      return var3;
   }

   private static String unsupportedKeywordReason(String var0) {
      return switch (var0) {
         case "oneOf", "anyOf", "not", "if", "then", "else" -> "the # vocabulary has no combinator — use match.jsonSchema for real semantics";
         case "additionalProperties", "patternProperties", "propertyNames", "minProperties", "maxProperties" -> "object-level constraints have no marker";
         case "const" -> "a fixed value has no marker — assert it directly";
         case "minimum", "maximum", "exclusiveMinimum", "exclusiveMaximum", "multipleOf" -> "numeric bounds have no marker";
         case "minLength", "maxLength", "minItems", "maxItems", "uniqueItems", "contains" -> "size and content bounds have no marker";
         case "dependencies", "dependentRequired", "dependentSchemas" -> "conditional requiredness has no marker";
         case "discriminator" -> "polymorphic dispatch has no marker";
         default -> "no # marker can express it";
      };
   }

   public static io.karatelabs.ext.openapi.SchemaMatchConverter.MatchBuildResult toMatchTemplateWithCompleteness(OpenApiSchemaNode var0) {
      boolean[] var1 = new boolean[]{true};
      Object var2 = buildMatchTemplate(var0, false, var1);
      return new io.karatelabs.ext.openapi.SchemaMatchConverter.MatchBuildResult(var2, var1[0]);
   }

   private static Object buildMatchTemplate(OpenApiSchemaNode var0, boolean var1, boolean[] var2) {
      Map<java.lang.String, Object> var3 = var0 == null ? Map.of() : var0.schemaDefinition.rawSchema;
      String var4 = var0 == null ? null : var0.type();
      boolean var5 = var1 || var0 != null && (var0.schemaDefinition.nullable() || var3.get("type") instanceof List var6 && var6.contains("null"));
      boolean var20 = var3.get("type") instanceof List var21 && var21.stream().filter(var0x -> !"null".equals(var0x)).count() > 1L;
      if (!var5
         && !var20
         && (var4 == null || "string".equals(var4))
         && var3.get("enum") instanceof List var22
         && !var22.isEmpty()
         && var22.stream().allMatch(var0x -> var0x instanceof String)) {
         return enumPredicate(var22);
      } else if (var4 != null && !var20 && !"oneOf".equals(var4) && !"anyOf".equals(var4)) {
         switch (var4) {
            case "object":
            case "allOf":
               List var25 = "allOf".equals(var4) ? var0.children : List.of(var0);
               LinkedHashMap<java.lang.String, java.lang.Object> var27 = new LinkedHashMap();

               for (OpenApiSchemaNode var12 : (Iterable<OpenApiSchemaNode>) (Iterable<?>) (var25)) {
                  if (!"object".equals(var12.type())) {
                     var2[0] = false;
                  } else {
                     var var13 = var12.schemaDefinition.rawSchema;
                     List var14 = var13.get("required") instanceof List var15 ? var15 : List.of();
                     Map var29 = var13.get("properties") instanceof Map var30 ? var30 : Map.of();
                     if (var25.size() > 1
                        || !Boolean.FALSE.equals(var13.get("additionalProperties"))
                        || var13.containsKey("patternProperties")
                        || var12.children.size() != var29.size()) {
                        var2[0] = false;
                     }

                     for (OpenApiSchemaNode var32 : var12.children) {
                        String var18 = var32.name();
                        if (var18 != null) {
                           boolean var19 = !var14.contains(var18) || var32.schemaDefinition.writeOnly();
                           var27.put(var18, buildMatchTemplate(var32, var19, var2));
                        }
                     }
                  }
               }

               if (!var27.isEmpty() && !var5) {
                  return var27;
               }

               return applyOptionalMarker(var1, var5, "#object");
            case "array":
               Object var9 = var0.children.isEmpty() ? null : buildMatchTemplate((OpenApiSchemaNode)var0.children.getFirst(), false, var2);
               String var26 = var9 instanceof String var11 ? "#[] " + var11 : (var9 == null ? "#array" : "#[] #object");
               return applyOptionalMarker(var1, var5, var26);
            default:
               return applyOptionalMarker(var1, var5, switch (var4) {
                  case "string" -> "#string";
                  case "integer", "number" -> "#number";
                  case "boolean" -> "#boolean";
                  case "null" -> "#null";
                  default -> "#present";
               });
         }
      } else {
         return var1 ? "#ignore" : "#present";
      }
   }

   private static Object buildMatchTemplateFromNode(OpenApiSchemaNode var0, boolean var1) {
      if (var0 == null) {
         return applyOptionalMarker(var1, false, "#notnull");
      } else {
         boolean var2 = var1 || var0.schemaDefinition.nullable();
         String var3 = var0.type();
         if (var3 == null) {
            return applyOptionalMarker(var1, var2, "#notnull");
         } else {
            if (!var1 && !var2) {
               var var4 = var0.schemaDefinition.enumValues();
               if (var4 != null && !var4.isEmpty()) {
                  return enumPredicate(var4);
               }

               String var5 = var0.schemaDefinition.pattern();
               if ("string".equals(var3) && var5 != null && !var5.isBlank()) {
                  return "#regex " + var5;
               }
            }

            switch (var3) {
               case "object":
                  if (!var1 && !var2) {
                     LinkedHashMap<java.lang.String, java.lang.Object> var14 = new LinkedHashMap();

                     for (OpenApiSchemaNode var16 : var0.children) {
                        if (var16.name() != null) {
                           var14.put(var16.name(), buildMatchTemplateFromNode(var16, !var16.isRequired()));
                        }
                     }

                     return var14;
                  }

                  return applyOptionalMarker(false, true, "#object");
               case "array":
                  String var13 = arrayItemMarker(var0);
                  return applyOptionalMarker(var1, var2, var13 == null ? "#array" : "#[] " + var13);
               case "allOf":
                  LinkedHashMap<java.lang.String, java.lang.Object> var6 = new LinkedHashMap();
                  boolean var7 = !var0.children.isEmpty();

                  for (OpenApiSchemaNode var9 : var0.children) {
                     if (buildMatchTemplateFromNode(var9, false) instanceof Map var10) {
                        var10.forEach((var1x, var2x) -> var6.put(String.valueOf(var1x), var2x));
                     } else {
                        var7 = false;
                     }
                  }

                  if (var7) {
                     return var6;
                  }

                  return applyOptionalMarker(var1, var2, "#notnull");
               case "oneOf":
               case "anyOf":
                  return applyOptionalMarker(var1, var2, "#notnull");
               default:
                  return applyOptionalMarker(var1, var2, markerForSchemaType(var3, var0));
            }
         }
      }
   }

   private static String arrayItemMarker(OpenApiSchemaNode var0) {
      if (var0.children.isEmpty()) {
         return null;
      } else {
         OpenApiSchemaNode var1 = (OpenApiSchemaNode)var0.children.getFirst();
         String var2 = var1.type();
         if (var2 == null) {
            return null;
         } else if ("object".equals(var2) || "allOf".equals(var2) || "oneOf".equals(var2) || "anyOf".equals(var2)) {
            return "#object";
         } else if ("array".equals(var2)) {
            return "#array";
         } else {
            return buildMatchTemplateFromNode(var1, false) instanceof String var4 ? var4 : "#object";
         }
      }
   }

   private static String markerForSchemaType(String var0, OpenApiSchemaNode var1) {
      return switch (var0) {
         case "string" -> "uuid".equals(var1.schemaDefinition.format()) ? "#uuid" : "#string";
         case "integer", "number" -> "#number";
         case "boolean" -> "#boolean";
         case "null" -> "#null";
         default -> "#notnull";
      };
   }

   public static Object toMatchTemplateFromMap(Map<String, Object> var0) {
      return buildMatchTemplateFromMap(var0, false);
   }

   private static Object buildMatchTemplateFromMap(Map<String, Object> var0, boolean var1) {
      if (var0 != null && !var0.isEmpty()) {
         String var3 = primaryType(var0);
         if (var3 != null || !var0.containsKey("anyOf") && !var0.containsKey("oneOf")) {
            boolean var2 = var1 || containsNullType(var0.get("type"));
            if (!var1 && !var2) {
               if (var0.get("enum") instanceof List var4 && !var4.isEmpty()) {
                  return enumPredicate(var4);
               }

               if ("string".equals(var3) && var0.get("pattern") instanceof String var6 && !var6.isBlank()) {
                  return "#regex " + var6;
               }
            }

            if ("object".equals(var3)) {
               return !var1 && !var2 ? buildObjectTemplate((Map<String, Object>)var0.getOrDefault("properties", Map.of()), (List<String>)var0.get("required")) : applyOptionalMarker(false, var2, "#object");
            } else if ("array".equals(var3)) {
               String var7 = arrayItemMarkerFromMap(var0);
               String var9 = var7 == null ? "#array" : "#[] " + var7;
               return applyOptionalMarker(var1, var2, var9);
            } else {
               return applyOptionalMarker(var1, var2, markerForSchemaTypeFromMap(var3, var0));
            }
         } else {
            return applyOptionalMarker(var1, unionAllowsNull(var0), "#notnull");
         }
      } else {
         return applyOptionalMarker(var1, false, "#notnull");
      }
   }

   private static Map<String, Object> buildObjectTemplate(Map<String, Object> var0, List<String> var1) {
      LinkedHashMap<java.lang.String, java.lang.Object> var2 = new LinkedHashMap();

      for (Entry var4 : var0.entrySet()) {
         Map var5 = var4.getValue() instanceof Map ? (Map)var4.getValue() : Map.of();
         boolean var6 = var1 == null || !var1.contains(var4.getKey());
         var2.put((String)var4.getKey(), buildMatchTemplateFromMap(var5, var6));
      }

      return var2;
   }

   private static String arrayItemMarkerFromMap(Map<String, Object> var0) {
      if (!(var0.get("items") instanceof Map)) {
         return null;
      } else {
         Map var1 = (Map)var0.get("items");
         String var2 = primaryType(var1);
         if ("object".equals(var2)) {
            return "#object";
         } else if ("array".equals(var2)) {
            return "#array";
         } else {
            return buildMatchTemplateFromMap(var1, false) instanceof String var4 ? var4 : "#object";
         }
      }
   }

   private static String markerForSchemaTypeFromMap(String var0, Map<String, Object> var1) {
      if (var0 == null) {
         return "#notnull";
      } else {
         return switch (var0) {
            case "string" -> "uuid".equals(var1.get("format")) ? "#uuid" : "#string";
            case "integer", "number" -> "#number";
            case "boolean" -> "#boolean";
            case "null" -> "#null";
            default -> "#notnull";
         };
      }
   }

   private static String enumPredicate(List<Object> var0) {
      StringBuilder var1 = new StringBuilder("#? [");

      for (int var2 = 0; var2 < var0.size(); var2++) {
         if (var2 > 0) {
            var1.append(", ");
         }

         var1.append(literalSource(var0.get(var2)));
      }

      return var1.append("].indexOf(_) != -1").toString();
   }

   private static String literalSource(Object var0) {
      if (var0 == null) {
         return "null";
      } else {
         return var0 instanceof String var1 ? "'" + var1.replace("\\", "\\\\").replace("'", "\\'") + "'" : String.valueOf(var0);
      }
   }

   private static String applyOptionalMarker(boolean var0, boolean var1, String var2) {
      return (var0 || var1) && var2.startsWith("#") && !var2.startsWith("##") ? "#" + var2 : var2;
   }

   public static Map<String, Object> toJsonSchema(Object var0) {
      return buildJsonSchema(var0).a;
   }

   private static io.karatelabs.ext.openapi.SchemaMatchConverter.SchemaBuildResult buildJsonSchema(Object var0) {
      if (var0 instanceof Map var8) {
         LinkedHashMap<java.lang.String, java.util.Map<java.lang.String,java.lang.Object>> var9 = new LinkedHashMap();
         ArrayList<java.lang.String> var3 = new ArrayList();

         for (Entry var5 : (Iterable<Entry>) (Iterable<?>) (var8.entrySet())) {
            io.karatelabs.ext.openapi.SchemaMatchConverter.SchemaBuildResult var6 = buildJsonSchema(var5.getValue());
            var9.put((String)var5.getKey(), var6.a);
            if (!var6.b) {
               var3.add((String)var5.getKey());
            }
         }

         var var10 = schemaWithType("object");
         var10.put("properties", var9);
         if (!var3.isEmpty()) {
            var10.put("required", var3);
         }

         return new io.karatelabs.ext.openapi.SchemaMatchConverter.SchemaBuildResult(var10, false);
      } else if (var0 instanceof List var7) {
         var var2 = schemaWithType("array");
         if (!var7.isEmpty()) {
            var2.put("items", buildJsonSchema(var7.get(0)).a);
         }

         return new io.karatelabs.ext.openapi.SchemaMatchConverter.SchemaBuildResult(var2, false);
      } else {
         return var0 instanceof String var1 && var1.startsWith("#") ? parseMarkerSchema(var1) : new io.karatelabs.ext.openapi.SchemaMatchConverter.SchemaBuildResult(io.karatelabs.ext.openapi.JsonSchemaInferrer.a(var0), false);
      }
   }

   private static io.karatelabs.ext.openapi.SchemaMatchConverter.SchemaBuildResult parseMarkerSchema(String var0) {
      boolean var1 = var0.startsWith("##");
      String var2 = var1 ? var0.substring(2) : var0.substring(1);
      if (var2.startsWith("[]")) {
         var var6 = schemaWithType("array");
         String var7 = var2.substring(2).trim();
         if (!var7.isEmpty()) {
            var6.put("items", parseMarkerSchema(var7).a);
         }

         return new io.karatelabs.ext.openapi.SchemaMatchConverter.SchemaBuildResult(makeNullable(var6, var1), var1);
      } else {
         String var4 = firstToken(var2);

         Object var3 = switch (var4) {
            case "string" -> schemaWithType("string");
            case "uuid" -> schemaWithTypeAndFormat("string", "uuid");
            case "number" -> schemaWithType("number");
            case "boolean" -> schemaWithType("boolean");
            case "object" -> schemaWithType("object");
            case "array" -> schemaWithType("array");
            case "null" -> schemaWithType("null");
            case "regex" -> regexSchema(var2);
            default -> new LinkedHashMap();
         };
         return new io.karatelabs.ext.openapi.SchemaMatchConverter.SchemaBuildResult(makeNullable((Map<String, Object>)var3, var1), var1);
      }
   }

   private static String primaryType(Map<String, Object> var0) {
      Object var1 = var0.get("type");
      if (var1 instanceof String var5) {
         return var5;
      } else {
         if (var1 instanceof List) {
            for (Object var4 : (List)var1) {
               if (!"null".equals(var4)) {
                  return (String)var4;
               }
            }
         }

         return null;
      }
   }

   private static boolean containsNullType(Object var0) {
      return var0 instanceof List var1 && var1.contains("null");
   }

   private static boolean unionAllowsNull(Map<String, Object> var0) {
      Object var1 = var0.containsKey("anyOf") ? var0.get("anyOf") : var0.get("oneOf");
      if (var1 instanceof List) {
         for (Object var4 : (List)var1) {
            if (var4 instanceof Map && "null".equals(primaryType((Map<String, Object>)var4))) {
               return true;
            }
         }
      }

      return false;
   }

   private static Map<String, Object> makeNullable(Map<String, Object> var0, boolean var1) {
      if (!var1) {
         return var0;
      } else {
         if (var0.get("type") instanceof String var3 && !"null".equals(var3)) {
            var0.put("type", List.of(var3, "null"));
         }

         return var0;
      }
   }

   private static String firstToken(String var0) {
      int var1 = var0.indexOf(32);
      return var1 < 0 ? var0 : var0.substring(0, var1);
   }

   private static Map<String, Object> regexSchema(String var0) {
      var var1 = schemaWithType("string");
      int var2 = var0.indexOf(32);
      if (var2 > 0) {
         var1.put("pattern", var0.substring(var2 + 1).trim());
      }

      return var1;
   }

   private static Map<String, Object> schemaWithType(String var0) {
      LinkedHashMap<java.lang.String, java.lang.Object> var1 = new LinkedHashMap();
      var1.put("type", var0);
      return var1;
   }

   private static Map<String, Object> schemaWithTypeAndFormat(String var0, String var1) {
      var var2 = schemaWithType(var0);
      var2.put("format", var1);
      return var2;
   }

   private record SchemaBuildResult(Map<String, Object> a, boolean b) {
   }

   public record MatchBuildResult(Object a, boolean b) {
   }
}
