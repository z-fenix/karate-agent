package io.karatelabs.ext.jsonschema.validation;

import java.util.List;
import java.util.Set;

public final class SchemaKeywords {
   public static final List<String> SINGLE_SCHEMA_KEYWORDS = List.of(
      "additionalProperties",
      "additionalItems",
      "contains",
      "propertyNames",
      "if",
      "then",
      "else",
      "not",
      "unevaluatedItems",
      "unevaluatedProperties",
      "contentSchema"
   );
   public static final List<String> SCHEMA_ARRAY_KEYWORDS = List.of("allOf", "anyOf", "oneOf", "prefixItems");
   public static final List<String> SCHEMA_MAP_KEYWORDS = List.of("properties", "patternProperties", "$defs", "definitions", "dependentSchemas");
   public static final List<String> UNIMPLEMENTED_DYNAMIC_REFERENCE_KEYWORDS = List.of("$dynamicRef", "$recursiveRef");
   private static final Set<String> SUPPORTED_KEYWORDS = Set.of(
      "$schema",
      "$id",
      "id",
      "$ref",
      "$anchor",
      "$dynamicAnchor",
      "$recursiveAnchor",
      "$vocabulary",
      "$defs",
      "definitions",
      "$comment",
      "type",
      "enum",
      "const",
      "format",
      "nullable",
      "required",
      "minProperties",
      "maxProperties",
      "dependentRequired",
      "dependencies",
      "minimum",
      "maximum",
      "exclusiveMinimum",
      "exclusiveMaximum",
      "multipleOf",
      "minLength",
      "maxLength",
      "pattern",
      "minItems",
      "maxItems",
      "uniqueItems",
      "minContains",
      "maxContains",
      "properties",
      "patternProperties",
      "additionalProperties",
      "propertyNames",
      "dependentSchemas",
      "items",
      "prefixItems",
      "additionalItems",
      "contains",
      "allOf",
      "anyOf",
      "oneOf",
      "not",
      "if",
      "then",
      "else",
      "unevaluatedProperties",
      "unevaluatedItems",
      "contentSchema",
      "title",
      "description",
      "default",
      "examples",
      "example",
      "deprecated",
      "readOnly",
      "writeOnly",
      "contentMediaType",
      "contentEncoding",
      "discriminator",
      "xml",
      "externalDocs"
   );

   private SchemaKeywords() {
   }

   public static boolean isSupportedKeyword(String var0) {
      return SUPPORTED_KEYWORDS.contains(var0) && !UNIMPLEMENTED_DYNAMIC_REFERENCE_KEYWORDS.contains(var0);
   }
}
