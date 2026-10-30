package io.karatelabs.ext.openapi.changes;

import java.math.BigDecimal;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.Map.Entry;

final class ComparableSchema {
   static final Set<String> a = Set.of("description", "summary", "title", "example", "examples", "externalDocs", "tags", "$comment", "$defs");
   static final Set<String> b = Set.of("x-extensible-enum", "x-sunset", "x-successor");
   static final Set<String> c = Set.of(
      "not",
      "if",
      "then",
      "else",
      "dependentSchemas",
      "dependentRequired",
      "patternProperties",
      "propertyNames",
      "unevaluatedProperties",
      "unevaluatedItems",
      "prefixItems",
      "contains",
      "minContains",
      "maxContains",
      "$dynamicRef",
      "contentMediaType",
      "contentEncoding",
      "contentSchema"
   );
   private static final Set<String> y = Set.of("additionalProperties", "unevaluatedProperties", "unevaluatedItems", "oneOf", "anyOf", "not", "if");
   private static final Set<String> z = Set.of("deprecated", "readOnly", "writeOnly", "default");
   private static final List<String> A = List.of("minLength", "minItems", "minProperties");
   private static final List<String> B = List.of("maxLength", "maxItems", "maxProperties");
   String d;
   List<io.karatelabs.ext.openapi.changes.ComparableSchema.ComparableSchemaNode> e = List.of();
   String f;
   io.karatelabs.ext.openapi.changes.ComparableSchema.SchemaIssue g;
   final List<io.karatelabs.ext.openapi.changes.ComparableSchema.SchemaIssue> h = new ArrayList<>();
   Set<String> i;
   boolean j;
   final Map<String, List<io.karatelabs.ext.openapi.changes.ComparableSchema.ComparableSchemaNode>> k = new LinkedHashMap<>();
   final Set<String> l = new LinkedHashSet<>();
   Object m;
   List<io.karatelabs.ext.openapi.changes.ComparableSchema.ComparableSchemaNode> n;
   List<io.karatelabs.ext.openapi.changes.ComparableSchema.ComparableSchemaNode> o;
   List<io.karatelabs.ext.openapi.changes.ComparableSchema.ComparableSchemaNode> p;
   final Map<String, Object> q = new LinkedHashMap<>();
   io.karatelabs.ext.openapi.changes.ComparableSchema.NumericBound r;
   io.karatelabs.ext.openapi.changes.ComparableSchema.NumericBound s;
   boolean t;
   boolean u;
   boolean v;
   boolean w;
   final Map<String, String> x = new HashMap<>();
   private boolean C;
   private boolean D;
   private String E;
   private Set<String> F;
   private boolean G;

   static ComparableSchema a(DiffDocument var0, Object var1, String var2) {
      return a(var0, var1 == null ? List.of() : List.of(new io.karatelabs.ext.openapi.changes.ComparableSchema.ComparableSchemaNode(var1, var2)));
   }

   static ComparableSchema a(DiffDocument var0, List<io.karatelabs.ext.openapi.changes.ComparableSchema.ComparableSchemaNode> var1) {
      ComparableSchema var2 = new ComparableSchema();
      var2.C = var0.e;
      var2.e = var1;
      ArrayList var3 = new ArrayList();
      ArrayDeque var4 = new ArrayDeque();
      if (var1.size() > 1) {
         var2.D = true;
      }

      for (io.karatelabs.ext.openapi.changes.ComparableSchema.ComparableSchemaNode var6 : var1) {
         var2.a(var0, var6, var3, var4, var1.size() == 1);
      }

      if (var2.g != null) {
         return var2;
      } else {
         if (var2.D) {
            for (io.karatelabs.ext.openapi.changes.ComparableSchema.ComparableSchemaNode var12 : (Iterable<io.karatelabs.ext.openapi.changes.ComparableSchema.ComparableSchemaNode>) (Iterable<?>) (var3)) {
               if (!(var12.a() instanceof Map var7)) {
                  var2.a("ALLOF_UNSAFE", var2.a(var1), "a branch is `false` at " + var12.b());
                  return var2;
               }

               for (String var9 : y) {
                  if (var7.containsKey(var9) && !a(var9, var7.get(var9))) {
                     var2.a("ALLOF_UNSAFE", var2.a(var1), "a branch carries `" + var9 + "` at " + var12.b());
                     return var2;
                  }
               }
            }
         }

         for (io.karatelabs.ext.openapi.changes.ComparableSchema.ComparableSchemaNode var13 : (Iterable<io.karatelabs.ext.openapi.changes.ComparableSchema.ComparableSchemaNode>) (Iterable<?>) (var3)) {
            if (!(var13.a() instanceof Map var14)) {
               var2.a(Boolean.FALSE.equals(var13.a()) ? "FALSE_SCHEMA" : "KEYWORD", var13.b(), Boolean.FALSE.equals(var13.a()) ? null : "not a schema");
               return var2;
            }

            var2.b(var14, var13.b());
            if (var2.g != null) {
               return var2;
            }
         }

         if (var2.G) {
            var2.j = var2.F.remove("null");
            var2.i = var2.F;
         } else {
            var2.j = true;
         }

         return var2;
      }
   }

   boolean a() {
      return this.d == null;
   }

   private String a(List<io.karatelabs.ext.openapi.changes.ComparableSchema.ComparableSchemaNode> var1) {
      return this.E != null ? this.E : (var1.isEmpty() ? this.d : ((io.karatelabs.ext.openapi.changes.ComparableSchema.ComparableSchemaNode)var1.get(0)).b());
   }

   private void a(String var1, String var2, Object var3) {
      if (this.g == null) {
         this.g = new io.karatelabs.ext.openapi.changes.ComparableSchema.SchemaIssue(
            var1.equals("FALSE_SCHEMA")
               ? "false"
               : (var1.equals("ALLOF_UNSAFE") ? "allOf" : (!var1.equals("EXTERNAL_REF") && !var1.equals("UNRESOLVED_REF") ? "schema" : "$ref")),
            var2,
            var1,
            var3
         );
         this.h.add(this.g);
      }
   }

   private void a(DiffDocument var1, io.karatelabs.ext.openapi.changes.ComparableSchema.ComparableSchemaNode var2, List<io.karatelabs.ext.openapi.changes.ComparableSchema.ComparableSchemaNode> var3, Deque<String> var4, boolean var5) {
      if (this.g == null) {
         Object var6 = var2.a();
         if (!(var6 instanceof Map var7)) {
            if (this.d == null) {
               this.d = var2.b();
            }

            if (!Boolean.TRUE.equals(var6)) {
               var3.add(var2);
            }
         } else if (var4.contains(var2.b())) {
            this.a("ALLOF_UNSAFE", var2.b(), "self-referential allOf");
         } else if (var7.get("$ref") instanceof String) {
            io.karatelabs.ext.openapi.changes.DiffDocument.ReferenceResolution var10 = var1.a(var7, var2.b());
            if (var10.e() != null) {
               this.a("EXTERNAL_REF", var10.c(), var10.e());
            } else if (var10.f()) {
               this.a("UNRESOLVED_REF", var10.c(), String.valueOf(((Map)var10.b()).get("$ref")));
            } else {
               if (var5 && this.f == null) {
                  this.f = var10.d();
               }

               Map var13 = var1.e ? Map.of() : a(var7, "$ref");
               if (!z.containsAll(var13.keySet())) {
                  this.D = true;
                  if (this.E == null) {
                     this.E = var2.b();
                  }
               }

               var4.push(var2.b());
               this.a(var1, new io.karatelabs.ext.openapi.changes.ComparableSchema.ComparableSchemaNode(var10.b(), var10.c()), var3, var4, false);
               var4.pop();
               if (!var13.isEmpty()) {
                  this.a(var1, new io.karatelabs.ext.openapi.changes.ComparableSchema.ComparableSchemaNode(var13, var2.b()), var3, var4);
               }
            }
         } else {
            if (this.d == null) {
               this.d = var2.b();
            }

            if (!(var7.get("allOf") instanceof List var8)) {
               this.a(var1, var2, var3, var4);
            } else {
               this.D = true;
               if (this.E == null) {
                  this.E = var2.b();
               }

               var4.push(var2.b());

               for (int var11 = 0; var11 < var8.size(); var11++) {
                  this.a(var1, new io.karatelabs.ext.openapi.changes.ComparableSchema.ComparableSchemaNode(var8.get(var11), var2.b() + "/allOf/" + var11), var3, var4, false);
               }

               var4.pop();
               var var12 = a(var7, "allOf");
               if (!var12.isEmpty()) {
                  this.a(var1, new io.karatelabs.ext.openapi.changes.ComparableSchema.ComparableSchemaNode(var12, var2.b()), var3, var4);
               }
            }
         }
      }
   }

   private void a(DiffDocument var1, io.karatelabs.ext.openapi.changes.ComparableSchema.ComparableSchemaNode var2, List<io.karatelabs.ext.openapi.changes.ComparableSchema.ComparableSchemaNode> var3, Deque<String> var4) {
      String var5 = this.D && var2.a() instanceof Map var6 ? a(var6) : null;
      if (var5 == null) {
         var3.add(var2);
      } else {
         Map<java.lang.String, java.util.List> var10 = (Map)var2.a();
         this.a(var1, new io.karatelabs.ext.openapi.changes.ComparableSchema.ComparableSchemaNode(((List)var10.get(var5)).get(0), var2.b() + "/" + var5 + "/0"), var3, var4, false);
         LinkedHashMap<java.lang.String, java.lang.Object> var11 = new LinkedHashMap();

         for (String var9 : z) {
            if (var10.containsKey(var9)) {
               var11.put(var9, var10.get(var9));
            }
         }

         if (!var11.isEmpty()) {
            var3.add(new io.karatelabs.ext.openapi.changes.ComparableSchema.ComparableSchemaNode(var11, var2.b()));
         }
      }
   }

   private static String a(Map<?, ?> var0) {
      String var1 = null;

      for (Entry var3 : var0.entrySet()) {
         String var4 = String.valueOf(var3.getKey());
         if (!a(var4) && !z.contains(var4) && !a(var4, var3.getValue()) && (!var4.equals("nullable") || var0.containsKey("type"))) {
            if (!var4.equals("anyOf") && !var4.equals("oneOf") || var1 != null || !(var3.getValue() instanceof List var5) || var5.size() != 1) {
               return null;
            }

            var1 = var4;
         }
      }

      return Boolean.TRUE.equals(var0.get("deprecated")) ? null : var1;
   }

   private static Map<String, Object> a(Map<?, ?> var0, String var1) {
      LinkedHashMap<java.lang.String, java.lang.Object> var2 = new LinkedHashMap();

      for (Entry var4 : var0.entrySet()) {
         String var5 = String.valueOf(var4.getKey());
         if (!var5.equals(var1) && !a(var5)) {
            var2.put(var5, var4.getValue());
         }
      }

      return var2;
   }

   static boolean a(String var0) {
      return a.contains(var0) || var0.startsWith("x-") && !b.contains(var0);
   }

   private void a(String var1, String var2) {
      this.a("ALLOF_UNSAFE", this.E != null ? this.E : var2, "two different `" + var1 + "` values");
   }

   private void b(Map<?, ?> var1, String var2) {
      TreeSet<java.lang.String> var3 = null;
      boolean var4 = false;
      var var5 = var1.entrySet().iterator();

      while (true) {
         String var7;
         Object var8;
         label272:
         while (true) {
            if (!var5.hasNext()) {
               this.c(var1, var2);
               if (var3 != null) {
                  if (var4) {
                     var3.add("null");
                  }

                  if (!this.G) {
                     this.G = true;
                     this.F = var3;
                  } else {
                     TreeSet<java.lang.String> var16 = new TreeSet();

                     for (String var18 : this.F) {
                        if (!var3.contains(var18) && (!var18.equals("integer") || !var3.contains("number"))) {
                           if (var18.equals("number") && var3.contains("integer")) {
                              var16.add("integer");
                           }
                        } else {
                           var16.add(var18);
                        }
                     }

                     if (var16.isEmpty()) {
                        this.a("type", var2);
                     }

                     this.F = var16;
                  }
               }

               return;
            }

            Entry var6 = (Entry)var5.next();
            var7 = String.valueOf(var6.getKey());
            var8 = var6.getValue();
            if (!a(var7) && !var7.equals("x-sunset") && !var7.equals("x-successor")) {
               String var9 = var2 + "/" + io.karatelabs.ext.openapi.changes.DiffDocument.c(var7);
               switch (var7) {
                  case "type":
                     var3 = new TreeSet();
                     if (var8 instanceof List) {
                        for (Object var36 : (List)var8) {
                           var3.add(String.valueOf(var36));
                        }
                     } else {
                        var3.add(String.valueOf(var8));
                     }

                     this.b(var7, var9);
                     break label272;
                  case "nullable":
                     if (this.C) {
                        var4 = Boolean.TRUE.equals(var8);
                        this.b(var7, var9);
                     } else {
                        this.h.add(new io.karatelabs.ext.openapi.changes.ComparableSchema.SchemaIssue(var7, var9, "KEYWORD", "`nullable` is not an OAS 3.1 keyword"));
                     }
                     break label272;
                  case "properties":
                     if (var8 instanceof Map var24) {
                        for (Entry var35 : (Iterable<Entry>) (Iterable<?>) (var24.entrySet())) {
                           String var37 = String.valueOf(var35.getKey());
                           this.k
                              .computeIfAbsent(var37, var0 -> new ArrayList<>())
                              .add(new io.karatelabs.ext.openapi.changes.ComparableSchema.ComparableSchemaNode(var35.getValue(), var9 + "/" + io.karatelabs.ext.openapi.changes.DiffDocument.c(var37)));
                        }
                     }

                     this.b(var7, var9);
                     break label272;
                  case "required":
                     if (var8 instanceof List) {
                        for (Object var34 : (List)var8) {
                           this.l.add(String.valueOf(var34));
                        }
                     }

                     this.b(var7, var9);
                     break label272;
                  case "additionalProperties":
                     this.m = a(var7, var8) ? Boolean.TRUE : (var8 instanceof Boolean var22 ? var22 : new io.karatelabs.ext.openapi.changes.ComparableSchema.ComparableSchemaNode(var8, var9));
                     this.b(var7, var9);
                     break label272;
                  case "items":
                     if (var8 instanceof List) {
                        this.h.add(new io.karatelabs.ext.openapi.changes.ComparableSchema.SchemaIssue(var7, var9, "KEYWORD", "tuple `items`"));
                     } else {
                        if (this.n == null) {
                           this.n = new ArrayList<>();
                        }

                        this.n.add(new io.karatelabs.ext.openapi.changes.ComparableSchema.ComparableSchemaNode(var8, var9));
                     }

                     this.b(var7, var9);
                     break label272;
                  case "anyOf":
                  case "oneOf":
                     ArrayList<io.karatelabs.ext.openapi.changes.ComparableSchema.ComparableSchemaNode> var21 = new ArrayList();
                     if (var8 instanceof List var28) {
                        for (int var33 = 0; var33 < var28.size(); var33++) {
                           var21.add(new io.karatelabs.ext.openapi.changes.ComparableSchema.ComparableSchemaNode(var28.get(var33), var9 + "/" + var33));
                        }
                     }

                     if (var7.equals("anyOf")) {
                        this.o = var21;
                     } else {
                        this.p = var21;
                     }

                     this.x.putIfAbsent(var7, var9);
                     break label272;
                  case "enum":
                     LinkedHashMap<java.lang.String, java.lang.Object> var20 = new LinkedHashMap();
                     if (var8 instanceof List) {
                        for (Object var15 : (List)var8) {
                           var20.put(io.karatelabs.ext.common.CanonicalJson.a(var15), var15);
                        }
                     }

                     Map var27 = (Map)this.q.get(var7);
                     if (var27 != null) {
                        var27.keySet().retainAll(var20.keySet());
                        if (var27.isEmpty()) {
                           this.a(var7, var9);
                        }
                     } else {
                        this.q.put(var7, var20);
                     }

                     this.b(var7, var9);
                     break label272;
                  case "x-extensible-enum":
                  case "const":
                  case "format":
                  case "pattern":
                  case "default":
                  case "multipleOf":
                     Object var19 = var7.equals("multipleOf") ? a(var8) : var8;
                     if (this.q.containsKey(var7) && !a(this.q.get(var7), var19)) {
                        this.a(var7, var9);
                     }

                     this.q.put(var7, var19);
                     this.b(var7, var9);
                     break label272;
                  case "uniqueItems":
                     if (Boolean.TRUE.equals(var8)) {
                        this.q.put(var7, Boolean.TRUE);
                        this.b(var7, var9);
                     }
                     break label272;
                  case "minLength":
                  case "minItems":
                  case "minProperties":
                  case "maxLength":
                  case "maxItems":
                  case "maxProperties":
                     BigDecimal var12 = a(var8);
                     if (var12 != null) {
                        BigDecimal var13 = (BigDecimal)this.q.get(var7);
                        boolean var14 = A.contains(var7);
                        if (var13 == null || (var14 ? var12.compareTo(var13) > 0 : var12.compareTo(var13) < 0)) {
                           this.q.put(var7, var12);
                           this.x.put(var7, var9);
                        }
                     }
                  case "minimum":
                  case "exclusiveMinimum":
                  case "maximum":
                  case "exclusiveMaximum":
                     break label272;
                  case "readOnly":
                     this.u = this.u | Boolean.TRUE.equals(var8);
                     break label272;
                  case "writeOnly":
                     this.v = this.v | Boolean.TRUE.equals(var8);
                     break label272;
                  case "deprecated":
                     if (Boolean.TRUE.equals(var8)) {
                        this.t = true;
                        this.b(var7, var9);
                     }
                     break label272;
                  case "discriminator":
                     this.h.add(new io.karatelabs.ext.openapi.changes.ComparableSchema.SchemaIssue(var7, var9, "DISCRIMINATOR", null));
                     break label272;
                  case "xml":
                     this.h.add(new io.karatelabs.ext.openapi.changes.ComparableSchema.SchemaIssue(var7, var9, "XML", null));
                     break label272;
                  default:
                     this.h.add(new io.karatelabs.ext.openapi.changes.ComparableSchema.SchemaIssue(var7, var9, "KEYWORD", c.contains(var7) ? null : "not in the openapi.diff/1 keyword inventory"));
                     break label272;
               }
            }
         }

         if (this.g != null) {
            return;
         }

         if (!var7.equals("anyOf") && !var7.equals("oneOf") && !z.contains(var7) && (!var7.equals("nullable") || var1.containsKey("type")) && !a(var7, var8)) {
            this.w = true;
         }
      }
   }

   private static boolean a(String var0, Object var1) {
      return switch (var0) {
         case "minLength", "minItems", "minProperties" -> a(var1) != null && a(var1).signum() == 0;
         case "uniqueItems" -> Boolean.FALSE.equals(var1);
         case "additionalProperties" -> {
            if (!Boolean.TRUE.equals(var1) && !(var1 instanceof Map var4 && var4.keySet().stream().allMatch(var0x -> a(String.valueOf(var0x))))) {
               yield false;
            }

            yield true;
         }
         default -> false;
      };
   }

   ComparableSchema a(DiffDocument var1) {
      List var2 = this.o != null && this.p == null ? this.o : (this.p != null && this.o == null ? this.p : null);
      if (var2 != null && var2.size() == 1 && !this.w && !this.t) {
         ComparableSchema var3 = a(var1, ((io.karatelabs.ext.openapi.changes.ComparableSchema.ComparableSchemaNode)var2.get(0)).a(), ((io.karatelabs.ext.openapi.changes.ComparableSchema.ComparableSchemaNode)var2.get(0)).b());
         var3.u = var3.u | this.u;
         var3.v = var3.v | this.v;
         if (this.q.containsKey("default")) {
            var3.q.put("default", this.q.get("default"));
            var3.x.put("default", this.x.get("default"));
         }

         return var3;
      } else {
         return this;
      }
   }

   ComparableSchema b(DiffDocument var1) {
      ComparableSchema var2 = this;
      HashSet<java.lang.String> var3 = new HashSet();

      while (var3.add(var2.d)) {
         ComparableSchema var4 = var2.a(var1);
         if (var4 == var2) {
            break;
         }

         var2 = var4;
      }

      return var2;
   }

   private void c(Map<?, ?> var1, String var2) {
      BigDecimal var3 = a(var1.get("minimum"));
      BigDecimal var4 = a(var1.get("maximum"));
      Object var5 = var1.get("exclusiveMinimum");
      Object var6 = var1.get("exclusiveMaximum");
      if (var3 != null) {
         this.a(new io.karatelabs.ext.openapi.changes.ComparableSchema.NumericBound(var3, Boolean.TRUE.equals(var5)), var2 + "/minimum");
      }

      if (var5 instanceof Number) {
         this.a(new io.karatelabs.ext.openapi.changes.ComparableSchema.NumericBound(a(var5), true), var2 + "/exclusiveMinimum");
      }

      if (var4 != null) {
         this.b(new io.karatelabs.ext.openapi.changes.ComparableSchema.NumericBound(var4, Boolean.TRUE.equals(var6)), var2 + "/maximum");
      }

      if (var6 instanceof Number) {
         this.b(new io.karatelabs.ext.openapi.changes.ComparableSchema.NumericBound(a(var6), true), var2 + "/exclusiveMaximum");
      }
   }

   private void a(io.karatelabs.ext.openapi.changes.ComparableSchema.NumericBound var1, String var2) {
      if (this.r == null || a(var1, this.r)) {
         this.r = var1;
         this.x.put("minimum", var2);
      }
   }

   private void b(io.karatelabs.ext.openapi.changes.ComparableSchema.NumericBound var1, String var2) {
      if (this.s == null || b(var1, this.s)) {
         this.s = var1;
         this.x.put("maximum", var2);
      }
   }

   static boolean a(io.karatelabs.ext.openapi.changes.ComparableSchema.NumericBound var0, io.karatelabs.ext.openapi.changes.ComparableSchema.NumericBound var1) {
      int var2 = var0.a().compareTo(var1.a());
      return var2 > 0 || var2 == 0 && var0.b() && !var1.b();
   }

   static boolean b(io.karatelabs.ext.openapi.changes.ComparableSchema.NumericBound var0, io.karatelabs.ext.openapi.changes.ComparableSchema.NumericBound var1) {
      int var2 = var0.a().compareTo(var1.a());
      return var2 < 0 || var2 == 0 && var0.b() && !var1.b();
   }

   private void b(String var1, String var2) {
      this.x.putIfAbsent(var1, var2);
   }

   static BigDecimal a(Object var0) {
      if (var0 instanceof Number var1) {
         try {
            return new BigDecimal(var1.toString());
         } catch (NumberFormatException var3) {
            return null;
         }
      } else {
         return null;
      }
   }

   static boolean a(Object var0, Object var1) {
      return var0 instanceof BigDecimal var2 && var1 instanceof BigDecimal var3
         ? var2.compareTo(var3) == 0
         : io.karatelabs.ext.common.CanonicalJson.a(var0).equals(io.karatelabs.ext.common.CanonicalJson.a(var1));
   }

   Set<String> b() {
      if (this.i == null) {
         return null;
      } else {
         var var1 = new TreeSet<>(this.i);
         if (this.j) {
            var1.add("null");
         }

         return var1;
      }
   }

   record NumericBound(BigDecimal a, boolean b) {

   }

   record SchemaIssue(String a, String b, String c, Object d) {

   }

   record ComparableSchemaNode(Object a, String b) {

   }
}
