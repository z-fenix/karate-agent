package io.karatelabs.ext.jsonschema.validation;

import java.math.BigDecimal;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

class SchemaValidator {
   private final SchemaRegistry registry;
   private final SchemaDialect dialect;
   private final boolean assertFormats;
   private final ValidationDirection direction;
   private final Map<Object, Set<Object>> validatingInstances = new IdentityHashMap<>();
   private final ArrayDeque<Object[]> requiredScopeStack = new ArrayDeque<>();

   SchemaValidator(SchemaRegistry var1, boolean var2, ValidationDirection var3) {
      this.registry = var1;
      this.dialect = var1.dialect;
      this.assertFormats = var2;
      this.direction = var3;
   }

   ValidationResult validate(Object var1, Object var2) {
      ArrayList var3 = new ArrayList();
      this.a(var1, "", "", var2, "", var3, null);
      return new ValidationResult(var3);
   }

   private boolean a(Object var1, String var2, String var3, Object var4, String var5, List<ValidationError> var6, EvaluationTracker var7) {
      if (var1 instanceof Boolean var17) {
         if (!var17) {
            var6.add(io.karatelabs.ext.jsonschema.validation.ValidationError.of("schema", var5, var2, false, io.karatelabs.ext.jsonschema.validation.JsonValueUtils.describeValue(var4), "schema is false — no value is valid here"));
         }

         return var17;
      } else if (!(var1 instanceof Map var8)) {
         throw new ValidationException("not a schema at '" + var2 + "': " + var1);
      } else {
         var3 = this.registry.baseUriFor(var1, var3);

         for (String var10 : io.karatelabs.ext.jsonschema.validation.SchemaKeywords.UNIMPLEMENTED_DYNAMIC_REFERENCE_KEYWORDS) {
            if (var8.containsKey(var10)) {
               throw new ValidationException("keyword not implemented: '" + var10 + "' at '" + var2 + "' (deferred — see the published keyword matrix)");
            }
         }

         Set<java.lang.Object> var18 = null;
         boolean var19 = var4 instanceof Map || var4 instanceof List;
         if (var19) {
            var18 = this.validatingInstances.computeIfAbsent(var1, var0 -> Collections.newSetFromMap(new IdentityHashMap<>()));
            if (!var18.add(var4)) {
               return true;
            }
         }

         boolean var11 = this.direction != io.karatelabs.ext.jsonschema.validation.ValidationDirection.NONE && this.dialect.requiredIsDirectionScoped();
         if (var11) {
            this.requiredScopeStack.push(new Object[]{var5, var8, var3});
         }

         boolean var12;
         try {
            var12 = this.a(var8, var2, var3, var4, var5, var6, var7);
         } finally {
            if (var11) {
               this.requiredScopeStack.pop();
            }

            if (var18 != null) {
               var18.remove(var4);
            }
         }

         return var12;
      }
   }

   private boolean a(Map<String, Object> var1, String var2, String var3, Object var4, String var5, List<ValidationError> var6, EvaluationTracker var7) {
      boolean var8 = var1.containsKey("unevaluatedProperties") || var1.containsKey("unevaluatedItems");
      EvaluationTracker var9 = var7 == null && !var8 ? null : new EvaluationTracker();
      boolean var10 = true;
      if (var1.get("$ref") instanceof String var11) {
         Object var19 = this.registry.resolveReference(var11, var3);
         var10 = this.b(var19, var2 + "/$ref", this.registry.baseUriFor(var19, var3), var4, var5, var6, var9);
         if (!this.dialect.refSiblingsApply()) {
            return mergeEvaluationResult(var10, var9, var7);
         }
      }

      var10 &= this.a(var1, var2, var4, var5, var6);
      var10 &= this.b(var1, var2, var4, var5, var6);
      if (io.karatelabs.ext.jsonschema.validation.JsonValueUtils.isNumber(var4)) {
         var10 &= this.c(var1, var2, var4, var5, var6);
      }

      if (var4 instanceof String var16) {
         var10 &= this.a(var1, var2, var16, var5, var6);
      }

      if (var4 instanceof List var17) {
         var10 &= this.a(var1, var2, var3, var17, var5, var6, var9);
      }

      if (var4 instanceof Map var18) {
         var10 &= this.a(var1, var2, var3, var18, var5, var6, var9);
      }

      if (this.assertFormats) {
         var10 &= this.d(var1, var2, var4, var5, var6);
      }

      var10 &= this.b(var1, var2, var3, var4, var5, var6, var9);
      if (var8) {
         var10 &= this.c(var1, var2, var3, var4, var5, var6, var9);
      }

      return mergeEvaluationResult(var10, var9, var7);
   }

   private static boolean mergeEvaluationResult(boolean var0, EvaluationTracker var1, EvaluationTracker var2) {
      if (var2 != null) {
         var2.merge(var1);
      }

      return var0;
   }

   private boolean b(Object var1, String var2, String var3, Object var4, String var5, List<ValidationError> var6, EvaluationTracker var7) {
      EvaluationTracker var8 = var7 == null ? null : new EvaluationTracker();
      boolean var9 = this.a(var1, var2, var3, var4, var5, var6, var8);
      if (var9 && var7 != null) {
         var7.merge(var8);
      }

      return var9;
   }

   private boolean a(Map<String, Object> var1, String var2, Object var3, String var4, List<ValidationError> var5) {
      Object var6 = var1.get("type");
      if (var6 == null) {
         return true;
      } else {
         Object var7 = new ArrayList();
         if (var6 instanceof String var9) {
            ((java.util.List)var7).add(var9);
         } else {
            if (!(var6 instanceof List)) {
               throw new ValidationException("'type' must be a string or an array of strings at '" + var2 + "'");
            }

            for (Object var11 : (List)var6) {
               ((java.util.List)var7).add(String.valueOf(var11));
            }
         }

         if (this.dialect.hasNullable() && Boolean.TRUE.equals(var1.get("nullable"))) {
            ((java.util.List)var7).add("null");
         }

         for (String var13 : (Iterable<String>) (Iterable<?>) (var7)) {
            if (this.matchesJsonType(var13, var3, var2)) {
               return true;
            }
         }

         var5.add(io.karatelabs.ext.jsonschema.validation.ValidationError.of("type", var4, var2 + "/type", var6, io.karatelabs.ext.jsonschema.validation.JsonValueUtils.jsonTypeName(var3), "expected type " + (((java.util.List)var7).size() == 1 ? ((java.util.List)var7).get(0) : var7) + " but found " + io.karatelabs.ext.jsonschema.validation.JsonValueUtils.jsonTypeName(var3)));
         return false;
      }
   }

   private boolean matchesJsonType(String var1, Object var2, String var3) {
      return switch (var1) {
         case "null" -> var2 == null;
         case "boolean" -> var2 instanceof Boolean;
         case "object" -> var2 instanceof Map;
         case "array" -> var2 instanceof List;
         case "number" -> io.karatelabs.ext.jsonschema.validation.JsonValueUtils.isNumber(var2);
         case "integer" -> io.karatelabs.ext.jsonschema.validation.JsonValueUtils.isInteger(var2);
         case "string" -> var2 instanceof String;
         case "file" -> this.dialect == io.karatelabs.ext.jsonschema.validation.SchemaDialect.SWAGGER_2_0 && var2 instanceof String;
         default -> throw new ValidationException("unknown 'type' value '" + var1 + "' at '" + var3 + "'");
      };
   }

   private boolean b(Map<String, Object> var1, String var2, Object var3, String var4, List<ValidationError> var5) {
      boolean var6 = true;
      if (var1.containsKey("enum")) {
         if (!(var1.get("enum") instanceof List var7)) {
            throw new ValidationException("'enum' must be an array at '" + var2 + "'");
         }

         boolean var11 = false;

         for (Object var10 : var7) {
            if (io.karatelabs.ext.jsonschema.validation.JsonValueUtils.jsonEquals(var3, var10)) {
               var11 = true;
               break;
            }
         }

         if (!var11) {
            var5.add(io.karatelabs.ext.jsonschema.validation.ValidationError.of("enum", var4, var2 + "/enum", var7, io.karatelabs.ext.jsonschema.validation.JsonValueUtils.describeValue(var3), "value is not one of the " + var7.size() + " permitted values"));
            var6 = false;
         }
      }

      if (var1.containsKey("const") && !io.karatelabs.ext.jsonschema.validation.JsonValueUtils.jsonEquals(var3, var1.get("const"))) {
         var5.add(io.karatelabs.ext.jsonschema.validation.ValidationError.of("const", var4, var2 + "/const", var1.get("const"), io.karatelabs.ext.jsonschema.validation.JsonValueUtils.describeValue(var3), "value must be exactly " + io.karatelabs.ext.jsonschema.validation.JsonValueUtils.describeValue(var1.get("const"))));
         var6 = false;
      }

      return var6;
   }

   private boolean c(Map<String, Object> var1, String var2, Object var3, String var4, List<ValidationError> var5) {
      BigDecimal var6 = io.karatelabs.ext.jsonschema.validation.JsonValueUtils.toBigDecimal(var3);
      if (var6 == null) {
         return true;
      } else {
         boolean var7 = true;
         if (var1.get("multipleOf") instanceof Number var8) {
            BigDecimal var13 = io.karatelabs.ext.jsonschema.validation.JsonValueUtils.toBigDecimal(var8);
            if (var13 != null && var13.signum() != 0 && !isMultipleOf(var6, var13, var2, var8)) {
               var5.add(io.karatelabs.ext.jsonschema.validation.ValidationError.of("multipleOf", var4, var2 + "/multipleOf", var8, var3, var3 + " is not a multiple of " + var8));
               var7 = false;
            }
         }

         boolean var12 = this.dialect.hasBooleanExclusive();
         if (var1.get("maximum") instanceof Number var14) {
            boolean var18 = var12 && Boolean.TRUE.equals(var1.get("exclusiveMaximum"));
            int var11 = var6.compareTo(io.karatelabs.ext.jsonschema.validation.JsonValueUtils.toBigDecimal(var14));
            if (var18 ? var11 >= 0 : var11 > 0) {
               var5.add(
                  io.karatelabs.ext.jsonschema.validation.ValidationError.of("maximum", var4, var2 + "/maximum", var14, var3, var3 + " is greater than " + (var18 ? "or equal to " : "") + "the maximum " + var14)
               );
               var7 = false;
            }
         }

         if (var1.get("minimum") instanceof Number var15) {
            boolean var20 = var12 && Boolean.TRUE.equals(var1.get("exclusiveMinimum"));
            int var23 = var6.compareTo(io.karatelabs.ext.jsonschema.validation.JsonValueUtils.toBigDecimal(var15));
            if (var20 ? var23 <= 0 : var23 < 0) {
               var5.add(io.karatelabs.ext.jsonschema.validation.ValidationError.of("minimum", var4, var2 + "/minimum", var15, var3, var3 + " is less than " + (var20 ? "or equal to " : "") + "the minimum " + var15));
               var7 = false;
            }
         }

         if (!var12) {
            if (var1.get("exclusiveMaximum") instanceof Number var16 && var6.compareTo(io.karatelabs.ext.jsonschema.validation.JsonValueUtils.toBigDecimal(var16)) >= 0) {
               var5.add(io.karatelabs.ext.jsonschema.validation.ValidationError.of("exclusiveMaximum", var4, var2 + "/exclusiveMaximum", var16, var3, var3 + " must be less than " + var16));
               var7 = false;
            }

            if (var1.get("exclusiveMinimum") instanceof Number var17 && var6.compareTo(io.karatelabs.ext.jsonschema.validation.JsonValueUtils.toBigDecimal(var17)) <= 0) {
               var5.add(io.karatelabs.ext.jsonschema.validation.ValidationError.of("exclusiveMinimum", var4, var2 + "/exclusiveMinimum", var17, var3, var3 + " must be greater than " + var17));
               var7 = false;
            }
         }

         return var7;
      }
   }

   private static boolean isMultipleOf(BigDecimal var0, BigDecimal var1, String var2, Object var3) {
      try {
         return var0.remainder(var1).compareTo(BigDecimal.ZERO) == 0;
      } catch (ArithmeticException var5) {
         throw new ValidationException("'multipleOf' value is out of range at '" + var2 + "': " + var3);
      }
   }

   private boolean a(Map<String, Object> var1, String var2, String var3, String var4, List<ValidationError> var5) {
      boolean var6 = true;
      if (var1.get("maxLength") instanceof Number var7 && io.karatelabs.ext.jsonschema.validation.JsonValueUtils.codePointLength(var3) > var7.intValue()) {
         var5.add(io.karatelabs.ext.jsonschema.validation.ValidationError.of("maxLength", var4, var2 + "/maxLength", var7, io.karatelabs.ext.jsonschema.validation.JsonValueUtils.codePointLength(var3), "string is longer than " + var7 + " characters"));
         var6 = false;
      }

      if (var1.get("minLength") instanceof Number var9 && io.karatelabs.ext.jsonschema.validation.JsonValueUtils.codePointLength(var3) < var9.intValue()) {
         var5.add(io.karatelabs.ext.jsonschema.validation.ValidationError.of("minLength", var4, var2 + "/minLength", var9, io.karatelabs.ext.jsonschema.validation.JsonValueUtils.codePointLength(var3), "string is shorter than " + var9 + " characters"));
         var6 = false;
      }

      if (var1.get("pattern") instanceof String var10 && !io.karatelabs.ext.jsonschema.validation.EcmaRegex.find(var10, var3)) {
         var5.add(io.karatelabs.ext.jsonschema.validation.ValidationError.of("pattern", var4, var2 + "/pattern", var10, var3, "string does not match the pattern /" + var10 + "/"));
         var6 = false;
      }

      return var6;
   }

   private boolean d(Map<String, Object> var1, String var2, Object var3, String var4, List<ValidationError> var5) {
      if (var1.get("format") instanceof String var6 && io.karatelabs.ext.jsonschema.validation.FormatValidator.supportedFormats(this.dialect).contains(var6)) {
         String var8 = io.karatelabs.ext.jsonschema.validation.FormatValidator.formatError(var6, var3);
         if (var8 == null) {
            return true;
         } else {
            var5.add(io.karatelabs.ext.jsonschema.validation.ValidationError.of("format", var4, var2 + "/format", var6, io.karatelabs.ext.jsonschema.validation.JsonValueUtils.describeValue(var3), "value is not a valid '" + var6 + "' — " + var8));
            return false;
         }
      } else {
         return true;
      }
   }

   private boolean a(Map<String, Object> var1, String var2, String var3, List<?> var4, String var5, List<ValidationError> var6, EvaluationTracker var7) {
      boolean var8 = true;
      if (var1.get("maxItems") instanceof Number var9 && var4.size() > var9.intValue()) {
         var6.add(io.karatelabs.ext.jsonschema.validation.ValidationError.of("maxItems", var5, var2 + "/maxItems", var9, var4.size(), "array has more than " + var9 + " items"));
         var8 = false;
      }

      if (var1.get("minItems") instanceof Number var12 && var4.size() < var12.intValue()) {
         var6.add(io.karatelabs.ext.jsonschema.validation.ValidationError.of("minItems", var5, var2 + "/minItems", var12, var4.size(), "array has fewer than " + var12 + " items"));
         var8 = false;
      }

      if (Boolean.TRUE.equals(var1.get("uniqueItems"))) {
         for (int var13 = 0; var13 < var4.size() && var8; var13++) {
            for (int var15 = var13 + 1; var15 < var4.size(); var15++) {
               if (io.karatelabs.ext.jsonschema.validation.JsonValueUtils.jsonEquals(var4.get(var13), var4.get(var15))) {
                  var6.add(
                     io.karatelabs.ext.jsonschema.validation.ValidationError.of(
                        "uniqueItems",
                        var5,
                        var2 + "/uniqueItems",
                        true,
                        io.karatelabs.ext.jsonschema.validation.JsonValueUtils.describeValue(var4.get(var13)),
                        "items " + var13 + " and " + var15 + " are equal but the array must be unique"
                     )
                  );
                  var8 = false;
                  break;
               }
            }
         }
      }

      var8 &= this.b(var1, var2, var3, var4, var5, var6, var7);
      return var8 & this.c(var1, var2, var3, var4, var5, var6, var7);
   }

   private boolean b(Map<String, Object> var1, String var2, String var3, List<?> var4, String var5, List<ValidationError> var6, EvaluationTracker var7) {
      boolean var8 = true;
      Object var9 = var1.get("items");
      int var10 = 0;
      if (this.dialect.hasPrefixItems()) {
         if (var1.get("prefixItems") instanceof List var15) {
            var10 = Math.min(var15.size(), var4.size());

            for (int var20 = 0; var20 < var10; var20++) {
               var8 &= this.a(var15.get(var20), var2 + "/prefixItems/" + var20, var3, var4.get(var20), var5 + "/" + var20, var6, null);
            }

            if (var7 != null) {
               var7.markPrefixEvaluated(var10);
            }
         }

         if (var9 != null) {
            for (int var16 = var10; var16 < var4.size(); var16++) {
               var8 &= this.a(var9, var2 + "/items", var3, var4.get(var16), var5 + "/" + var16, var6, null);
            }

            if (var7 != null && var10 < var4.size()) {
               var7.markAllItemsEvaluated();
            }
         }

         return var8;
      } else {
         if (var9 instanceof List var11) {
            var10 = Math.min(var11.size(), var4.size());

            for (int var12 = 0; var12 < var10; var12++) {
               var8 &= this.a(var11.get(var12), var2 + "/items/" + var12, var3, var4.get(var12), var5 + "/" + var12, var6, null);
            }

            if (var7 != null) {
               var7.markPrefixEvaluated(var10);
            }

            Object var17 = var1.get("additionalItems");
            if (var17 != null) {
               for (int var13 = var10; var13 < var4.size(); var13++) {
                  var8 &= this.a(var17, var2 + "/additionalItems", var3, var4.get(var13), var5 + "/" + var13, var6, null);
               }

               if (var7 != null && var10 < var4.size()) {
                  var7.markAllItemsEvaluated();
               }
            }
         } else if (var9 != null) {
            for (int var18 = 0; var18 < var4.size(); var18++) {
               var8 &= this.a(var9, var2 + "/items", var3, var4.get(var18), var5 + "/" + var18, var6, null);
            }

            if (var7 != null && !var4.isEmpty()) {
               var7.markAllItemsEvaluated();
            }
         }

         return var8;
      }
   }

   private boolean c(Map<String, Object> var1, String var2, String var3, List<?> var4, String var5, List<ValidationError> var6, EvaluationTracker var7) {
      Object var8 = var1.get("contains");
      if (var8 == null) {
         return true;
      } else {
         ArrayList<java.lang.Integer> var9 = new ArrayList();

         for (int var10 = 0; var10 < var4.size(); var10++) {
            ArrayList var11 = new ArrayList();
            if (this.a(var8, var2 + "/contains", var3, var4.get(var10), var5 + "/" + var10, var11, null)) {
               var9.add(var10);
            }
         }

         int var15 = var9.size();
         int var16 = 1;
         int var12 = Integer.MAX_VALUE;
         if (this.dialect.isModernCore()) {
            if (var1.get("minContains") instanceof Number var13) {
               var16 = var13.intValue();
            }

            if (var1.get("maxContains") instanceof Number var17) {
               var12 = var17.intValue();
            }
         }

         if (var15 < var16) {
            var6.add(io.karatelabs.ext.jsonschema.validation.ValidationError.of("contains", var5, var2 + "/contains", var8, var15, "at least " + var16 + " item(s) must match 'contains' but " + var15 + " did"));
            return false;
         } else if (var15 > var12) {
            var6.add(io.karatelabs.ext.jsonschema.validation.ValidationError.of("maxContains", var5, var2 + "/maxContains", var12, var15, "at most " + var12 + " item(s) may match 'contains' but " + var15 + " did"));
            return false;
         } else {
            if (var7 != null && this.dialect.containsAnnotatesItems()) {
               for (int var20 : (Iterable<java.lang.Integer>) (Iterable<?>) (var9)) {
                  var7.markItemEvaluated(var20);
               }
            }

            return true;
         }
      }
   }

   private boolean isRequiredExcused(String var1, String var2) {
      String var3 = this.direction.excuses();
      if (var3 != null && this.dialect.requiredIsDirectionScoped()) {
         Set var4 = Collections.newSetFromMap(new IdentityHashMap());

         for (Object[] var6 : this.requiredScopeStack) {
            if (!var1.equals(var6[0])) {
               break;
            }

            if (this.a(var6[1], (String)var6[2], var2, var3, var4)) {
               return true;
            }
         }

         return false;
      } else {
         return false;
      }
   }

   private boolean a(Object var1, String var2, String var3, String var4, Set<Object> var5) {
      if (var1 instanceof Map && var5.add(var1)) {
         Map<java.lang.String, java.lang.Object> var6 = (Map)var1;
         var2 = this.registry.baseUriFor(var1, var2);
         if (var6.get("properties") instanceof Map var8
            && var8.get(var3) != null
            && var8.get(var3) instanceof Map var10
            && Boolean.TRUE.equals(var10.get(var4))) {
            return true;
         } else {
            if (var6.get("$ref") instanceof String var12) {
               Object var15 = this.registry.resolveReference(var12, var2);
               if (this.a(var15, this.registry.baseUriFor(var15, var2), var3, var4, var5)) {
                  return true;
               }
            }

            Object var16 = var6.get("allOf");
            if (var16 instanceof List) {
               for (Object var18 : (List)var16) {
                  if (this.a(var18, var2, var3, var4, var5)) {
                     return true;
                  }
               }
            }

            return false;
         }
      } else {
         return false;
      }
   }

   private boolean a(Map<String, Object> var1, String var2, String var3, Map<?, ?> var4, String var5, List<ValidationError> var6, EvaluationTracker var7) {
      boolean var8 = true;
      if (var1.get("maxProperties") instanceof Number var9 && var4.size() > var9.intValue()) {
         var6.add(io.karatelabs.ext.jsonschema.validation.ValidationError.of("maxProperties", var5, var2 + "/maxProperties", var9, var4.size(), "object has more than " + var9 + " properties"));
         var8 = false;
      }

      if (var1.get("minProperties") instanceof Number var20 && var4.size() < var20.intValue()) {
         var6.add(io.karatelabs.ext.jsonschema.validation.ValidationError.of("minProperties", var5, var2 + "/minProperties", var20, var4.size(), "object has fewer than " + var20 + " properties"));
         var8 = false;
      }

      Object var24 = var1.get("required");
      if (var24 instanceof List) {
         for (Object var11 : (List)var24) {
            if (!var4.containsKey(var11) && !this.isRequiredExcused(var5, String.valueOf(var11))) {
               var6.add(io.karatelabs.ext.jsonschema.validation.ValidationError.of("required", var5, var2 + "/required", var11, null, "required property '" + var11 + "' is missing"));
               var8 = false;
            }
         }
      }

      Map<java.lang.String, Object> var22 = (Map)var1.get("properties");
      var24 = (Map)var1.get("patternProperties");
      Object var27 = var1.get("additionalProperties");
      Object var12 = var1.get("propertyNames");

      for (Entry var14 : var4.entrySet()) {
         String var15 = String.valueOf(var14.getKey());
         String var16 = var5 + "/" + escapeJsonPointerSegment(var15);
         if (var12 != null) {
            var8 &= this.a(var12, var2 + "/propertyNames", var3, var15, var16, var6, null);
         }

         boolean var17 = false;
         if (var22 != null && var22.containsKey(var15)) {
            var17 = true;
            var8 &= this.a(var22.get(var15), var2 + "/properties/" + escapeJsonPointerSegment(var15), var3, var14.getValue(), var16, var6, null);
         }

         if (var24 != null) {
            for (Entry var19 : (Iterable<Entry>) (Iterable<?>) (((Map<?, ?>)var24).entrySet())) {
               if (io.karatelabs.ext.jsonschema.validation.EcmaRegex.find((String)var19.getKey(), var15)) {
                  var17 = true;
                  var8 &= this.a(var19.getValue(), var2 + "/patternProperties/" + escapeJsonPointerSegment((String)var19.getKey()), var3, var14.getValue(), var16, var6, null);
               }
            }
         }

         boolean var28 = var17;
         if (!var17 && var27 != null) {
            var8 &= this.a(var27, var2 + "/additionalProperties", var3, var14.getValue(), var16, var6, null);
            var28 = true;
         }

         if (var28 && var7 != null) {
            var7.markPropertyEvaluated(var15);
         }
      }

      return var8 & this.b(var1, var2, var3, var4, var5, var6, var7);
   }

   private boolean b(Map<String, Object> var1, String var2, String var3, Map<?, ?> var4, String var5, List<ValidationError> var6, EvaluationTracker var7) {
      boolean var8 = true;
      if (var1.get("dependencies") instanceof Map var9) {
         for (Entry var11 : (Iterable<Entry>) (Iterable<?>) (var9.entrySet())) {
            if (var4.containsKey(var11.getKey())) {
               if (var11.getValue() instanceof List var12) {
                  var8 &= this.a("dependencies", String.valueOf(var11.getKey()), var12, var4, var2 + "/dependencies", var5, var6);
               } else {
                  var8 &= this.b(var11.getValue(), var2 + "/dependencies/" + escapeJsonPointerSegment(String.valueOf(var11.getKey())), var3, var4, var5, var6, var7);
               }
            }
         }
      }

      if (var1.get("dependentRequired") instanceof Map var14) {
         for (Entry var21 : (Iterable<Entry>) (Iterable<?>) (var14.entrySet())) {
            if (var4.containsKey(var21.getKey()) && var21.getValue() instanceof List var23) {
               var8 &= this.a("dependentRequired", String.valueOf(var21.getKey()), var23, var4, var2 + "/dependentRequired", var5, var6);
            }
         }
      }

      if (var1.get("dependentSchemas") instanceof Map var15) {
         for (Entry var22 : (Iterable<Entry>) (Iterable<?>) (var15.entrySet())) {
            if (var4.containsKey(var22.getKey())) {
               var8 &= this.b(var22.getValue(), var2 + "/dependentSchemas/" + escapeJsonPointerSegment(String.valueOf(var22.getKey())), var3, var4, var5, var6, var7);
            }
         }
      }

      return var8;
   }

   private boolean a(String var1, String var2, List<?> var3, Map<?, ?> var4, String var5, String var6, List<ValidationError> var7) {
      boolean var8 = true;

      for (Object var10 : var3) {
         if (!var4.containsKey(var10)) {
            var7.add(io.karatelabs.ext.jsonschema.validation.ValidationError.of(var1, var6, var5 + "/" + escapeJsonPointerSegment(var2), var10, null, "property '" + var10 + "' is required because '" + var2 + "' is present"));
            var8 = false;
         }
      }

      return var8;
   }

   private boolean b(Map<String, Object> var1, String var2, String var3, Object var4, String var5, List<ValidationError> var6, EvaluationTracker var7) {
      boolean var8 = true;
      if (var1.get("allOf") instanceof List var9) {
         for (int var16 = 0; var16 < var9.size(); var16++) {
            var8 &= this.b(var9.get(var16), var2 + "/allOf/" + var16, var3, var4, var5, var6, var7);
         }
      }

      if (var1.get("anyOf") instanceof List var12) {
         var8 &= this.a("anyOf", var12, var2, var3, var4, var5, var6, 1, Integer.MAX_VALUE, var7);
      }

      if (var1.get("oneOf") instanceof List var13) {
         var8 &= this.a("oneOf", var13, var2, var3, var4, var5, var6, 1, 1, var7);
      }

      if (var1.containsKey("not")) {
         ArrayList var14 = new ArrayList();
         if (this.a(var1.get("not"), var2 + "/not", var3, var4, var5, var14, null)) {
            var6.add(io.karatelabs.ext.jsonschema.validation.ValidationError.of("not", var5, var2 + "/not", var1.get("not"), io.karatelabs.ext.jsonschema.validation.JsonValueUtils.describeValue(var4), "value matches the schema under 'not', which forbids it"));
            var8 = false;
         }
      }

      if (var1.containsKey("if")) {
         ArrayList var15 = new ArrayList();
         boolean var19 = this.b(var1.get("if"), var2 + "/if", var3, var4, var5, var15, var7);
         String var11 = var19 ? "then" : "else";
         if (var1.containsKey(var11)) {
            var8 &= this.b(var1.get(var11), var2 + "/" + var11, var3, var4, var5, var6, var7);
         }
      }

      return var8;
   }

   private boolean c(Map<String, Object> var1, String var2, String var3, Object var4, String var5, List<ValidationError> var6, EvaluationTracker var7) {
      boolean var8 = true;
      Object var9 = var1.get("unevaluatedProperties");
      if (var9 != null && var4 instanceof Map var10) {
         for (Entry var12 : (Iterable<Entry>) (Iterable<?>) (var10.entrySet())) {
            String var13 = String.valueOf(var12.getKey());
            if (!var7.isPropertyEvaluated(var13)) {
               String var14 = var5 + "/" + escapeJsonPointerSegment(var13);
               if (Boolean.FALSE.equals(var9)) {
                  var6.add(
                     io.karatelabs.ext.jsonschema.validation.ValidationError.of(
                        "unevaluatedProperties",
                        var14,
                        var2 + "/unevaluatedProperties",
                        false,
                        io.karatelabs.ext.jsonschema.validation.JsonValueUtils.describeValue(var12.getValue()),
                        "property '" + var13 + "' is not evaluated by any schema here, and 'unevaluatedProperties: false' forbids it"
                     )
                  );
                  var8 = false;
               } else {
                  var8 &= this.a(var9, var2 + "/unevaluatedProperties", var3, var12.getValue(), var14, var6, null);
               }

               var7.markPropertyEvaluated(var13);
            }
         }
      }

      Object var15 = var1.get("unevaluatedItems");
      if (var15 != null && var4 instanceof List var16) {
         for (int var17 = 0; var17 < var16.size(); var17++) {
            if (!var7.isItemEvaluated(var17)) {
               if (Boolean.FALSE.equals(var15)) {
                  var6.add(
                     io.karatelabs.ext.jsonschema.validation.ValidationError.of(
                        "unevaluatedItems",
                        var5 + "/" + var17,
                        var2 + "/unevaluatedItems",
                        false,
                        io.karatelabs.ext.jsonschema.validation.JsonValueUtils.describeValue(var16.get(var17)),
                        "item " + var17 + " is not evaluated by any schema here, and 'unevaluatedItems: false' forbids it"
                     )
                  );
                  var8 = false;
               } else {
                  var8 &= this.a(var15, var2 + "/unevaluatedItems", var3, var16.get(var17), var5 + "/" + var17, var6, null);
               }

               var7.markItemEvaluated(var17);
            }
         }
      }

      return var8;
   }

   private boolean a(String var1, List<?> var2, String var3, String var4, Object var5, String var6, List<ValidationError> var7, int var8, int var9, EvaluationTracker var10) {
      int var11 = 0;
      ArrayList<java.lang.Integer> var12 = new ArrayList();
      ArrayList var13 = null;
      int var14 = -1;

      for (int var15 = 0; var15 < var2.size(); var15++) {
         ArrayList var16 = new ArrayList();
         if (this.b(var2.get(var15), var3 + "/" + var1 + "/" + var15, var4, var5, var6, var16, var10)) {
            var11++;
            var12.add(var15);
         } else if (var13 == null || var16.size() < var13.size()) {
            var13 = var16;
            var14 = var15;
         }
      }

      if (var11 >= var8 && var11 <= var9) {
         return true;
      } else if (var11 > var9) {
         var7.add(
            new ValidationError(
               var1,
               var6,
               var3 + "/" + var1,
               "exactly one branch",
               var12,
               "value matches " + var11 + " branches of '" + var1 + "' " + var12 + " but exactly one must match",
               null,
               null
            )
         );
         return false;
      } else {
         var7.add(
            new ValidationError(
               var1,
               var6,
               var3 + "/" + var1,
               var2.size() + " branches",
               io.karatelabs.ext.jsonschema.validation.JsonValueUtils.describeValue(var5),
               "value matches no branch of '" + var1 + "'; branch " + var14 + " was the nearest",
               var14 < 0 ? null : var14,
               var13
            )
         );
         return false;
      }
   }

   private static String escapeJsonPointerSegment(String var0) {
      return var0.replace("~", "~0").replace("/", "~1");
   }
}
