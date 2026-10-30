package io.karatelabs.ext.openapi.changes;

import java.math.BigDecimal;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

final class CounterexampleGenerator {
   static final int a = 64;
   static final int b = 512;
   private static final int c = 8;
   private static final Set<String> d = Set.of("$id", "id", "$anchor", "$dynamicAnchor", "$recursiveAnchor", "$schema", "$defs", "definitions", "$comment");
   private static final Set<String> e = Set.of(
      "additionalProperties",
      "items",
      "not",
      "if",
      "then",
      "else",
      "propertyNames",
      "contains",
      "unevaluatedProperties",
      "unevaluatedItems",
      "additionalItems",
      "contentSchema"
   );
   private static final Set<String> f = Set.of("allOf", "anyOf", "oneOf", "prefixItems");
   private static final Set<String> g = Set.of("patternProperties", "dependentSchemas");
   private static final List<Object> h = List.of(0, new BigDecimal("1.5"), "", "a", Boolean.TRUE, Map.of(), List.of());
   private final io.karatelabs.ext.openapi.changes.DiffDocument i;
   private final io.karatelabs.ext.openapi.changes.DiffDocument j;
   private final Map<String, io.karatelabs.ext.openapi.changes.CounterexampleGenerator.SchemaCompilationContext> k = new HashMap<>();
   private final Map<String, io.karatelabs.ext.openapi.changes.CounterexampleGenerator.CounterexampleResult> l = new HashMap<>();

   static String a(String var0, int var1) {
      int var2 = 0;
      int var3 = 0;

      while (var3 < var0.length()) {
         int var4 = var0.codePointAt(var3);
         var2 += var4 < 128 ? 1 : (var4 < 2048 ? 2 : (var4 < 65536 ? 3 : 4));
         if (var2 > var1) {
            break;
         }

         var3 += Character.charCount(var4);
      }

      return var0.substring(0, var3);
   }

   CounterexampleGenerator(io.karatelabs.ext.openapi.changes.DiffDocument var1, io.karatelabs.ext.openapi.changes.DiffDocument var2) {
      this.i = var1;
      this.j = var2;
   }

   io.karatelabs.ext.openapi.changes.CounterexampleGenerator.CounterexampleResult a(io.karatelabs.ext.openapi.changes.OpenApiChangeKind.ChangeDirection var1, boolean var2, ComparableSchema var3, ComparableSchema var4, io.karatelabs.ext.openapi.changes.CounterexampleGenerator.SchemaContext var5, List<io.karatelabs.ext.openapi.changes.CounterexampleGenerator.SchemaContext> var6) {
      StringBuilder var7 = new StringBuilder(var1.name())
         .append(var2)
         .append('\u0000')
         .append(a(var3))
         .append('\u0000')
         .append(a(var4))
         .append('\u0000')
         .append(var5 == null ? "" : a(var5.b()));

      for (io.karatelabs.ext.openapi.changes.CounterexampleGenerator.SchemaContext var9 : var6) {
         var7.append('\u0000').append(var9.a().a).append(a(var9.b()));
      }

      return this.l.computeIfAbsent(var7.toString(), var7x -> this.a(var1, var2, var3, var4, var5, var6, List.of()));
   }

   io.karatelabs.ext.openapi.changes.CounterexampleGenerator.CounterexampleResult a(io.karatelabs.ext.openapi.changes.OpenApiChangeKind.ChangeDirection var1, boolean var2, ComparableSchema var3, ComparableSchema var4, List<io.karatelabs.ext.openapi.changes.CounterexampleGenerator.SchemaContext> var5, List<Object> var6) {
      return this.a(var1, var2, var3, var4, null, var5, var6);
   }

   private io.karatelabs.ext.openapi.changes.CounterexampleGenerator.CounterexampleResult a(io.karatelabs.ext.openapi.changes.OpenApiChangeKind.ChangeDirection var1, boolean var2, ComparableSchema var3, ComparableSchema var4, io.karatelabs.ext.openapi.changes.CounterexampleGenerator.SchemaContext var5, List<io.karatelabs.ext.openapi.changes.CounterexampleGenerator.SchemaContext> var6, List<Object> var7) {
      io.karatelabs.ext.openapi.changes.DiffDocument var8 = var2 ? this.j : this.i;

      try {
         io.karatelabs.ext.openapi.changes.CounterexampleGenerator.SchemaCompilationContext var9 = this.a(var8, var1, true);
         io.karatelabs.ext.jsonschema.validation.CompiledSchema var10 = var9.a(var2 ? var4 : var3);
         io.karatelabs.ext.jsonschema.validation.CompiledSchema var11 = this.a(var2 ? this.i : this.j, var1, false).a(var2 ? var3 : var4);
         io.karatelabs.ext.jsonschema.validation.CompiledSchema var12 = var5 == null ? null : var9.a(var5.b());

         for (Object var14 : this.a(var1, var5 == null ? var6 : List.of(var5), var5 == null, var7)) {
            if ((var12 == null || var12.validate(var14).valid) && var10.validate(var14).valid && !var11.validate(var14).valid) {
               return new io.karatelabs.ext.openapi.changes.CounterexampleGenerator.CounterexampleResult(var14, var8.a, null);
            }
         }

         return io.karatelabs.ext.openapi.changes.CounterexampleGenerator.CounterexampleResult.a;
      } catch (StackOverflowError | RuntimeException var15) {
         return new io.karatelabs.ext.openapi.changes.CounterexampleGenerator.CounterexampleResult(null, null, "validator declined: " + var15.getMessage());
      }
   }

   private static String a(ComparableSchema var0) {
      StringBuilder var1 = new StringBuilder();

      for (io.karatelabs.ext.openapi.changes.ComparableSchema.ComparableSchemaNode var3 : var0.e) {
         var1.append(var3.b()).append(';');
      }

      return var1.toString();
   }

   private io.karatelabs.ext.openapi.changes.CounterexampleGenerator.SchemaCompilationContext a(io.karatelabs.ext.openapi.changes.DiffDocument var1, io.karatelabs.ext.openapi.changes.OpenApiChangeKind.ChangeDirection var2, boolean var3) {
      return this.k.computeIfAbsent(var1.a + var2 + var3, var3x -> new io.karatelabs.ext.openapi.changes.CounterexampleGenerator.SchemaCompilationContext(var1, var2, var3));
   }

   private List<Object> a(io.karatelabs.ext.openapi.changes.OpenApiChangeKind.ChangeDirection var1, List<io.karatelabs.ext.openapi.changes.CounterexampleGenerator.SchemaContext> var2, boolean var3, List<Object> var4) {
      LinkedHashMap<java.lang.String, java.lang.Object> var5 = new LinkedHashMap();

      for (Object var7 : var4) {
         var5.putIfAbsent(io.karatelabs.ext.common.CanonicalJson.a(var7), var7);
      }

      ArrayList<java.util.Map<java.lang.String,java.lang.Object>> var14 = new ArrayList();

      for (io.karatelabs.ext.openapi.changes.CounterexampleGenerator.SchemaContext var8 : var2) {
         io.karatelabs.ext.openapi.changes.CounterexampleGenerator.SchemaCompilationContext var9 = this.a(var8.a(), var1, false);
         Object var10 = var9.b(var8.b());
         io.karatelabs.ext.openapi.changes.CounterexampleGenerator.CounterexampleCandidateBuilder var11 = new io.karatelabs.ext.openapi.changes.CounterexampleGenerator.CounterexampleCandidateBuilder(var9);

         for (Object var13 : var11.a(var10)) {
            var5.putIfAbsent(io.karatelabs.ext.common.CanonicalJson.a(var13), var13);
         }

         var var21 = var11.a(var10, 0);
         if (var21 != null) {
            var14.add(var21);
         }
      }

      int var16 = 0;

      for (int var17 = 0; var3 && var17 < var14.size() && var16 < 64; var17++) {
         for (int var19 = var17 + 1; var19 < var14.size() && var16 < 64; var19++) {
            LinkedHashMap var20 = new LinkedHashMap((Map)var14.get(var17));
            var20.putAll((Map)var14.get(var19));
            var5.putIfAbsent(io.karatelabs.ext.common.CanonicalJson.a(var20), var20);
            var16++;
         }
      }

      if (var3 && var14.size() > 2) {
         LinkedHashMap var18 = new LinkedHashMap();
         var14.forEach(var18::putAll);
         var5.putIfAbsent(io.karatelabs.ext.common.CanonicalJson.a(var18), var18);
      }

      return new ArrayList<>(var5.values());
   }

   static List<Object> a(String var0) {
      ArrayList<java.lang.Object> var1 = new ArrayList();
      var1.add("");
      ArrayList<java.lang.String> var2 = new ArrayList();
      StringBuilder var3 = new StringBuilder();

      for (int var4 = 0; var4 < var0.length(); var4++) {
         char var5 = var0.charAt(var4);
         switch (var5) {
            case '$':
            case '(':
            case ')':
            case '*':
            case '+':
            case '.':
            case '?':
            case '^':
            case '|':
               a(var3, var2);
               break;
            case '[':
               a(var3, var2);
               int var11 = var4 + 1;
               boolean var12 = var11 < var0.length() && var0.charAt(var11) == '^';
               if (var12) {
                  var11++;
               }

               int var8;
               for (var8 = var11; var11 < var0.length() && (var0.charAt(var11) != ']' || var11 == var8); var11++) {
                  if (var0.charAt(var11) == '\\') {
                     var11++;
                  }
               }

               if (!var12 && var8 < var0.length() && var8 < var11) {
                  char var9 = var0.charAt(var8);
                  var2.add(String.valueOf(var9 == '\\' && var8 + 1 < var11 ? var0.charAt(var8 + 1) : var9));
               }

               var4 = var11;
               break;
            case '\\':
               if (++var4 < var0.length()) {
                  char var10 = var0.charAt(var4);

                  String var7 = switch (var10) {
                     case 'D' -> "a";
                     case 'S' -> "a";
                     case 'W' -> "-";
                     case 'd' -> "0";
                     case 's' -> " ";
                     case 'w' -> "a";
                     default -> null;
                  };
                  if (var7 != null) {
                     a(var3, var2);
                     var2.add(var7);
                  } else if (!Character.isLetter(var10)) {
                     var3.append(var10);
                  }
               }
               break;
            case '{':
               a(var3, var2);
               int var6 = var0.indexOf(125, var4);
               var4 = var6 < 0 ? var0.length() : var6;
               break;
            default:
               var3.append(var5);
         }
      }

      a(var3, var2);
      var1.addAll(var2);
      if (var2.size() > 1) {
         var1.add(String.join("", var2));
      }

      return var1;
   }

   private static void a(StringBuilder var0, List<String> var1) {
      if (!var0.isEmpty()) {
         var1.add(var0.toString());
         var0.setLength(0);
      }
   }

   private static final class ReferenceResolutionException extends RuntimeException {
      ReferenceResolutionException(String var1) {
         super(var1, null, false, false);
      }
   }

   private static final class CounterexampleCandidateBuilder {
      private final io.karatelabs.ext.openapi.changes.CounterexampleGenerator.SchemaCompilationContext a;
      private final Map<String, Object> b = new LinkedHashMap<>();

      CounterexampleCandidateBuilder(io.karatelabs.ext.openapi.changes.CounterexampleGenerator.SchemaCompilationContext var1) {
         this.a = var1;
      }

      List<Object> a(Object var1) {
         this.b(var1, 0);
         return new ArrayList<>(this.b.values());
      }

      private boolean b(Object var1) {
         if (this.b.size() >= 64) {
            return false;
         } else {
            this.b.putIfAbsent(io.karatelabs.ext.common.CanonicalJson.a(var1), var1);
            return true;
         }
      }

      private void b(Object var1, int var2) {
         var1 = this.a.a(var1);
         if (var2 <= 8 && this.b.size() < 64 && !Boolean.FALSE.equals(var1)) {
            if (!(var1 instanceof Map var3)) {
               io.karatelabs.ext.openapi.changes.CounterexampleGenerator.h.forEach(this::b);
               this.b(null);
            } else {
               if (var3.containsKey("example")) {
                  this.b(var3.get("example"));
               }

               if (var3.get("examples") instanceof List var4) {
                  var4.forEach(this::b);
               }

               if (var3.containsKey("default")) {
                  this.b(var3.get("default"));
               }

               if (var3.get("enum") instanceof List var17) {
                  var17.forEach(this::b);
               }

               if (var3.containsKey("const")) {
                  this.b(var3.get("const"));
               }

               for (String var21 : List.of("allOf", "anyOf", "oneOf")) {
                  Object var7 = var3.get(var21);
                  if (var7 instanceof List) {
                     for (Object var8 : (List)var7) {
                        this.b(var8, var2 + 1);
                     }
                  }
               }

               var var19 = this.a(var3);
               boolean var22 = var19.contains("integer") && !var19.contains("number");
               BigDecimal var23 = var22 ? BigDecimal.ONE : (var3.get("multipleOf") instanceof Number var25 ? new BigDecimal(var25.toString()) : BigDecimal.ONE);

               for (String var32 : List.of("minimum", "exclusiveMinimum", "maximum", "exclusiveMaximum")) {
                  if (var3.get(var32) instanceof Number var9) {
                     BigDecimal var39 = new BigDecimal(var9.toString());
                     this.b(a(var39));
                     this.b(a(var39.subtract(var23)));
                     this.b(a(var39.add(var23)));
                  }
               }

               for (String var33 : List.of("minLength", "maxLength")) {
                  if (var3.get(var33) instanceof Number var36) {
                     int var41 = var36.intValue();

                     for (int var11 = Math.max(0, var41 - 1); var11 <= var41 + 1; var11++) {
                        this.b("a".repeat(var11));
                     }
                  }
               }

               for (String var34 : List.of("minItems", "maxItems")) {
                  if (var3.get(var34) instanceof Number var37) {
                     int var43 = var37.intValue();
                     Object var47 = this.c(var3.get("items"), var2 + 1);

                     for (int var12 = Math.max(0, var43 - 1); var12 <= var43 + 1; var12++) {
                        ArrayList<java.lang.Object> var13 = new ArrayList();

                        for (int var14 = 0; var14 < var12; var14++) {
                           var13.add(var47);
                        }

                        this.b(var13);
                     }
                  }
               }

               if (var19.contains("null")) {
                  this.b(null);
               }

               if (var19.contains("boolean")) {
                  this.b(Boolean.TRUE);
               }

               if (var19.contains("integer") || var19.contains("number")) {
                  this.b(0);
               }

               if (var19.contains("number")) {
                  this.b(new BigDecimal("1.5"));
               }

               if (var19.contains("string")) {
                  this.b("");
                  this.b("a");
               }

               if (var19.contains("array")) {
                  this.b(List.of());
                  ArrayList<java.lang.Object> var29 = new ArrayList();
                  var29.add(this.c(var3.get("items"), var2 + 1));
                  this.b(var29);
               }

               if (var19.contains("object")) {
                  Map<java.lang.String, Object> var30 = this.a(var3, var2);
                  this.b(var30);
                  Map var35 = this.a(var3, var2);
                  if (var35 != null) {
                     this.b(var35);
                  }

                  if (var3.get("properties") instanceof Map var38) {
                     for (Entry var48 : (Iterable<Entry>) (Iterable<?>) (var38.entrySet())) {
                        String var50 = String.valueOf(var48.getKey());
                        if (!var30.containsKey(var50)) {
                           LinkedHashMap<java.lang.String, java.lang.Object> var52 = new LinkedHashMap(var30);
                           var52.put(var50, this.c(var48.getValue(), var2 + 1));
                           this.b(var52);
                        }
                     }

                     for (Entry var49 : (Iterable<Entry>) (Iterable<?>) (var38.entrySet())) {
                        if (var2 >= 8 || this.b.size() >= 64) {
                           break;
                        }

                        io.karatelabs.ext.openapi.changes.CounterexampleGenerator.CounterexampleCandidateBuilder var51 = new io.karatelabs.ext.openapi.changes.CounterexampleGenerator.CounterexampleCandidateBuilder(this.a);
                        var51.b(var49.getValue(), var2 + 1);

                        for (Object var54 : var51.b.values()) {
                           LinkedHashMap<java.lang.String, java.lang.Object> var15 = new LinkedHashMap(var30);
                           var15.put(String.valueOf(var49.getKey()), var54);
                           if (!this.b(var15)) {
                              break;
                           }
                        }
                     }
                  }
               }
            }
         }
      }

      Map<String, Object> a(Object var1, int var2) {
         if (var2 <= 8 && this.a.a(var1) instanceof Map var3 && this.a(var3).contains("object")) {
            Map<java.lang.String, java.lang.Object> var4 = this.a(var3, var2);
            if (var3.get("properties") instanceof Map var5) {
               for (Entry var7 : (Iterable<Entry>) (Iterable<?>) (var5.entrySet())) {
                  var4.putIfAbsent(String.valueOf(var7.getKey()), this.c(var7.getValue(), var2 + 1));
               }
            }

            for (String var14 : List.of("allOf", "anyOf", "oneOf")) {
               Object var8 = var3.get(var14);
               if (var8 instanceof List) {
                  for (Object var9 : (List)var8) {
                     var var10 = this.a(var9, var2 + 1);
                     if (var10 != null) {
                        var10.forEach(var4::putIfAbsent);
                     }
                  }
               }
            }

            return var4;
         } else {
            return null;
         }
      }

      private Map<String, Object> a(Map<String, Object> var1, int var2) {
         LinkedHashMap<java.lang.String, java.lang.Object> var3 = new LinkedHashMap();
         if (var1.get("required") instanceof List var4) {
            Map<java.lang.String, Object> var8 = var1.get("properties") instanceof Map var6 ? var6 : Map.of();

            for (Object var10 : var4) {
               var3.put(String.valueOf(var10), this.c(var8.get(String.valueOf(var10)), var2 + 1));
            }
         }

         return var3;
      }

      private Object c(Object var1, int var2) {
         if (var2 <= 8 && this.a.a(var1) instanceof Map var3) {
            if (var3.containsKey("example")) {
               return var3.get("example");
            } else if (var3.containsKey("default")) {
               return var3.get("default");
            } else if (var3.get("enum") instanceof List var4 && !var4.isEmpty()) {
               return var4.get(0);
            } else if (var3.containsKey("const")) {
               return var3.get("const");
            } else {
               for (String var13 : List.of("allOf", "anyOf", "oneOf")) {
                  if (var3.get(var13) instanceof List var6 && !var6.isEmpty()) {
                     return this.c(var6.get(0), var2 + 1);
                  }
               }

               var var12 = this.a(var3);
               if (var12.contains("object")) {
                  return this.a(var3, var2);
               } else if (var12.contains("string")) {
                  int var16 = var3.get("minLength") instanceof Number var20 ? var20.intValue() : 0;
                  int var21 = var3.get("maxLength") instanceof Number var27 ? var27.intValue() : Integer.MAX_VALUE;
                  return "a".repeat(Math.min(Math.max(var16, 1), var21));
               } else if (var12.contains("integer") || var12.contains("number")) {
                  BigDecimal var15 = BigDecimal.ZERO;
                  if (var3.get("minimum") instanceof Number var18) {
                     BigDecimal var29 = new BigDecimal(var18.toString());
                     boolean var9 = Boolean.TRUE.equals(var3.get("exclusiveMinimum"));
                     if (var15.compareTo(var29) < 0 || var9 && var15.compareTo(var29) == 0) {
                        var15 = var9 ? var29.add(BigDecimal.ONE) : var29;
                     }
                  } else if (var3.get("exclusiveMinimum") instanceof Number var24) {
                     BigDecimal var31 = new BigDecimal(var24.toString());
                     if (var15.compareTo(var31) <= 0) {
                        var15 = var31.add(BigDecimal.ONE);
                     }
                  }

                  if (var3.get("maximum") instanceof Number var19 && var15.compareTo(new BigDecimal(var19.toString())) > 0) {
                     var15 = new BigDecimal(var19.toString());
                  }

                  return a(var15);
               } else if (var12.contains("boolean")) {
                  return Boolean.TRUE;
               } else if (!var12.contains("array")) {
                  return null;
               } else {
                  ArrayList<java.lang.Object> var14 = new ArrayList();
                  int var17 = var3.get("minItems") instanceof Number var22 ? var22.intValue() : 0;

                  for (int var23 = 0; var23 < Math.min(var17, 8); var23++) {
                     var14.add(this.c(var3.get("items"), var2 + 1));
                  }

                  return var14;
               }
            }
         } else {
            return null;
         }
      }

      private Set<String> a(Map<?, ?> var1) {
         HashSet<java.lang.String> var2 = new HashSet();
         Object var3 = var1.get("type");
         if (var3 instanceof List var4) {
            var4.forEach(var1x -> var2.add(String.valueOf(var1x)));
         } else if (var3 != null) {
            var2.add(String.valueOf(var3));
         }

         if (this.a.a.e && Boolean.TRUE.equals(var1.get("nullable")) && !var2.isEmpty()) {
            var2.add("null");
         }

         if (var2.isEmpty()) {
            if (var1.containsKey("properties") || var1.containsKey("required") || var1.containsKey("additionalProperties")) {
               var2.add("object");
            }

            if (var1.containsKey("items")) {
               var2.add("array");
            }

            if (var1.containsKey("minLength") || var1.containsKey("maxLength") || var1.containsKey("pattern")) {
               var2.add("string");
            }

            if (var1.containsKey("minimum") || var1.containsKey("maximum") || var1.containsKey("multipleOf")) {
               var2.add("number");
            }
         }

         if (var2.isEmpty()) {
            var2.addAll(List.of("null", "boolean", "number", "string", "array", "object"));
         }

         return var2;
      }

      private static Object a(BigDecimal var0) {
         BigDecimal var1 = var0.stripTrailingZeros();
         return var1.scale() <= 0 && var1.abs().compareTo(BigDecimal.valueOf(2147483647L)) <= 0 ? var1.intValueExact() : var1;
      }
   }

   private static final class SchemaCompilationContext {
      final io.karatelabs.ext.openapi.changes.DiffDocument a;
      final io.karatelabs.ext.openapi.changes.OpenApiChangeKind.ChangeDirection b;
      final boolean c;
      final io.karatelabs.ext.jsonschema.validation.SchemaDialect d;
      final Set<String> e;
      final Map<String, String> f = new HashMap<>();
      final Map<String, Object> g = new LinkedHashMap<>();
      final Map<String, Set<String>> h = new HashMap<>();
      private Set<String> i = new HashSet<>();

      SchemaCompilationContext(io.karatelabs.ext.openapi.changes.DiffDocument var1, io.karatelabs.ext.openapi.changes.OpenApiChangeKind.ChangeDirection var2, boolean var3) {
         this.a = var1;
         this.b = var2;
         this.c = var3;
         this.d = var1.e ? io.karatelabs.ext.jsonschema.validation.SchemaDialect.OAS_3_0 : io.karatelabs.ext.jsonschema.validation.SchemaDialect.OAS_3_1;
         this.e = io.karatelabs.ext.jsonschema.validation.FormatValidator.supportedFormats(this.d);
      }

      io.karatelabs.ext.jsonschema.validation.CompiledSchema a(ComparableSchema var1) {
         this.i = new HashSet<>();
         Object var2 = this.b(var1);
         LinkedHashMap<java.lang.String, java.lang.Object> var3 = new LinkedHashMap();
         ArrayDeque var4 = new ArrayDeque<>(this.i);

         while (!var4.isEmpty()) {
            String var5 = (String)var4.pop();
            if (!var3.containsKey(var5)) {
               var3.put(var5, this.g.get(var5));
               var4.addAll(this.h.getOrDefault(var5, Set.of()));
            }
         }

         LinkedHashMap<java.lang.String, java.lang.Object> var7 = new LinkedHashMap();
         var7.put("allOf", List.of(var2));
         var7.put("$defs", var3);
         io.karatelabs.ext.jsonschema.validation.CompiledSchema var6 = new io.karatelabs.ext.jsonschema.validation.SchemaRegistry(this.d).compile(var7);
         return this.c ? var6.withFormatAssertion(true) : var6;
      }

      Object b(ComparableSchema var1) {
         if (var1.e.size() == 1) {
            return this.a(var1.e.get(0).a(), var1.e.get(0).b());
         } else {
            ArrayList<java.lang.Object> var2 = new ArrayList();

            for (io.karatelabs.ext.openapi.changes.ComparableSchema.ComparableSchemaNode var4 : var1.e) {
               var2.add(this.a(var4.a(), var4.b()));
            }

            LinkedHashMap<java.lang.String, java.util.ArrayList> var5 = new LinkedHashMap();
            var5.put("allOf", var2);
            return var5;
         }
      }

      Object a(Object var1, String var2) {
         if (!(var1 instanceof Map var3)) {
            return var1;
         } else if (var3.get("$ref") instanceof String) {
            io.karatelabs.ext.openapi.changes.DiffDocument.ReferenceResolution var19 = this.a.a(var3, var2);
            if (!var19.a()) {
               throw new io.karatelabs.ext.openapi.changes.CounterexampleGenerator.ReferenceResolutionException("unresolvable $ref at " + var2);
            } else {
               String var20 = this.f.get(var19.c());
               if (var20 == null) {
                  var20 = "d" + this.f.size();
                  this.f.put(var19.c(), var20);
                  LinkedHashMap<java.lang.String, java.lang.Object> var22 = new LinkedHashMap();
                  this.g.put(var20, var22);
                  var var26 = this.i;
                  this.i = new HashSet<>();
                  Object var29 = this.a(var19.b(), var19.c());
                  this.h.put(var20, this.i);
                  this.i = var26;
                  if (var29 instanceof Map var32) {
                     var32.forEach((var1x, var2x) -> var22.put(String.valueOf(var1x), var2x));
                  } else {
                     this.g.put(var20, var29);
                  }
               }

               this.i.add(var20);
               LinkedHashMap<java.lang.String, java.lang.Object> var23 = new LinkedHashMap();
               var23.put("$ref", "#/$defs/" + var20);
               if (!this.a.e) {
                  LinkedHashMap<java.lang.String, Object> var27 = new LinkedHashMap(var3);
                  var27.remove("$ref");
                  Object var30 = this.a(var27, var2);
                  if (var30 instanceof Map var33) {
                     var33.forEach((var1x, var2x) -> var23.put(String.valueOf(var1x), var2x));
                  } else if (Boolean.FALSE.equals(var30)) {
                     return var30;
                  }
               }

               return var23;
            }
         } else {
            HashSet<java.lang.String> var4 = new HashSet();
            LinkedHashMap<java.lang.String, java.lang.Object> var5 = new LinkedHashMap();

            for (Entry var7 : (Iterable<Entry>) (Iterable<?>) (var3.entrySet())) {
               String var8 = String.valueOf(var7.getKey());
               Object var9 = var7.getValue();
               String var10 = var2 + "/" + io.karatelabs.ext.openapi.changes.DiffDocument.c(var8);
               if (!io.karatelabs.ext.openapi.changes.CounterexampleGenerator.d.contains(var8)) {
                  switch (var8) {
                     case "properties":
                        LinkedHashMap<java.lang.String, java.lang.Object> var36 = new LinkedHashMap();
                        if (var9 instanceof Map var39) {
                           for (Entry var44 : (Iterable<Entry>) (Iterable<?>) (var39.entrySet())) {
                              String var46 = String.valueOf(var44.getKey());
                              String var18 = var10 + "/" + io.karatelabs.ext.openapi.changes.DiffDocument.c(var46);
                              if (io.karatelabs.ext.openapi.changes.SchemaDiffWalker.a(io.karatelabs.ext.openapi.changes.ComparableSchema.a(this.a, var44.getValue(), var18).b(this.a), this.b)) {
                                 var4.add(var46);
                              } else {
                                 var36.put(var46, this.a(var44.getValue(), var18));
                              }
                           }
                        }

                        var5.put(var8, var36);
                        break;
                     case "format":
                        if (this.c) {
                           if (!this.e.contains(String.valueOf(var9))) {
                              return Boolean.FALSE;
                           }

                           var5.put(var8, var9);
                        }
                        break;
                     case "discriminator":
                        if (this.c) {
                           return Boolean.FALSE;
                        }
                        break;
                     case "dependencies":
                        if (var9 instanceof Map var35) {
                           LinkedHashMap<java.lang.String, java.lang.Object> var38 = new LinkedHashMap();

                           for (Entry var43 : (Iterable<Entry>) (Iterable<?>) (var35.entrySet())) {
                              String var45 = String.valueOf(var43.getKey());
                              var38.put(
                                 var45,
                                 var43.getValue() instanceof List
                                    ? var43.getValue()
                                    : this.a(var43.getValue(), var10 + "/" + io.karatelabs.ext.openapi.changes.DiffDocument.c(var45))
                              );
                           }

                           var5.put(var8, var38);
                        }
                        break;
                     default:
                        if (io.karatelabs.ext.openapi.changes.CounterexampleGenerator.e.contains(var8) && !(var9 instanceof List)) {
                           var5.put(var8, this.a(var9, var10));
                        } else if (io.karatelabs.ext.openapi.changes.CounterexampleGenerator.f.contains(var8) || var8.equals("items")) {
                           ArrayList<java.lang.Object> var34 = new ArrayList();
                           if (var9 instanceof List var37) {
                              for (int var40 = 0; var40 < var37.size(); var40++) {
                                 var34.add(this.a(var37.get(var40), var10 + "/" + var40));
                              }
                           }

                           var5.put(var8, var34);
                        } else if (!io.karatelabs.ext.openapi.changes.CounterexampleGenerator.g.contains(var8)) {
                           var5.put(var8, var9);
                        } else {
                           LinkedHashMap<java.lang.String, java.lang.Object> var13 = new LinkedHashMap();
                           if (var9 instanceof Map var14) {
                              for (Entry var16 : (Iterable<Entry>) (Iterable<?>) (var14.entrySet())) {
                                 String var17 = String.valueOf(var16.getKey());
                                 var13.put(var17, this.a(var16.getValue(), var10 + "/" + io.karatelabs.ext.openapi.changes.DiffDocument.c(var17)));
                              }
                           }

                           var5.put(var8, var13);
                        }
                  }
               }
            }

            if (!var4.isEmpty() && var5.get("required") instanceof List var21) {
               ArrayList<java.lang.Object> var25 = new ArrayList();

               for (Object var31 : var21) {
                  if (!var4.contains(String.valueOf(var31))) {
                     var25.add(var31);
                  }
               }

               var5.put("required", var25);
            }

            return var5;
         }
      }

      Object a(Object var1) {
         int var2 = 0;

         while (var1 instanceof Map) {
            Map<java.lang.String, Object> var3 = (Map)var1;
            if (!(var3.get("$ref") instanceof String var4) || var2++ >= 64) {
               break;
            }

            var1 = this.g.get(var4.substring("#/$defs/".length()));
         }

         return var1;
      }
   }

   record CounterexampleResult(Object b, String c, String d) {
      static final io.karatelabs.ext.openapi.changes.CounterexampleGenerator.CounterexampleResult a = new io.karatelabs.ext.openapi.changes.CounterexampleGenerator.CounterexampleResult(null, null, null);


      boolean a() {
         return this.c != null;
      }

      Map<String, Object> a(String var1) {
         LinkedHashMap<java.lang.String, java.lang.Object> var2 = new LinkedHashMap();
         String var3 = io.karatelabs.ext.common.CanonicalJson.a(this.b);
         String var4 = io.karatelabs.ext.openapi.changes.CounterexampleGenerator.a(var3, 512);
         boolean var5 = var4.length() < var3.length();
         var2.put("instance", var5 ? var4 : this.b);
         var2.put("validUnder", this.c);
         var2.put("class", var1);
         var2.put("truncated", var5);
         var2.put("sha256", io.karatelabs.ext.common.Sha256.a(var3));
         if (var5) {
            var2.put("full", this.b);
         }

         return var2;
      }
   }

   record SchemaContext(io.karatelabs.ext.openapi.changes.DiffDocument a, ComparableSchema b) {

   }
}
