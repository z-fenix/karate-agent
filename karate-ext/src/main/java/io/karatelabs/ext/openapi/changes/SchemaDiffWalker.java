package io.karatelabs.ext.openapi.changes;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

final class SchemaDiffWalker {
   static final int a = 256;
   private static final io.karatelabs.ext.openapi.changes.SchemaDiffWalker.SchemaPath e = new io.karatelabs.ext.openapi.changes.SchemaDiffWalker.SchemaPath("", "", "", "", false, 0);
   final DiffSession b;
   private final DiffDocument f;
   private final DiffDocument g;
   final io.karatelabs.ext.openapi.changes.OpenApiChangeKind.ChangeDirection c;
   private final io.karatelabs.ext.openapi.changes.DiffSession.OperationContext h;
   final Set<String> d = new HashSet<>();
   private List<io.karatelabs.ext.openapi.changes.SchemaDiffGraph.SchemaDiffNode> i;
   private boolean j;

   SchemaDiffWalker(DiffSession var1, io.karatelabs.ext.openapi.changes.OpenApiChangeKind.ChangeDirection var2, io.karatelabs.ext.openapi.changes.DiffSession.OperationContext var3) {
      this.b = var1;
      this.f = var1.b;
      this.g = var1.c;
      this.c = var2;
      this.h = var3;
   }

   void a(ComparableSchema var1, ComparableSchema var2, io.karatelabs.ext.openapi.changes.SchemaDiffWalker.SchemaPath var3) {
      if (this.i != null) {
         this.a(var1, var2);
      } else if (var3.f() > 256) {
         this.b.a("schema", var1.a() ? var2.d : var1.d, "both", "KEYWORD", "nesting deeper than 256");
      } else {
         boolean var4 = !var1.a() && !var2.a();
         String var5 = var1.d + "\u0000" + var2.d;
         if (!var4 || !this.d.contains(var5) && this.b.g.add(this.h.a() + "\u0000" + this.c + "\u0000" + var5)) {
            if (var4) {
               this.d.add(var5);
            }

            try {
               this.b(var1, var2, var3);
            } finally {
               if (var4) {
                  this.d.remove(var5);
               }
            }
         }
      }
   }

   private void b(ComparableSchema var1, ComparableSchema var2, io.karatelabs.ext.openapi.changes.SchemaDiffWalker.SchemaPath var3) {
      this.b.a(var1, "baseline");
      this.b.a(var2, "current");
      if (var1.g == null && var2.g == null) {
         ComparableSchema var4 = var1.a(this.f);
         ComparableSchema var5 = var2.a(this.g);
         if (var4 == var1 && var5 == var2) {
            this.c(var1, var2, var3);
            boolean var6 = var1.o != null || var1.p != null;
            boolean var7 = var2.o != null || var2.p != null;
            if (var6 != var7) {
               this.b(var1, var2, var3, var6);
            } else {
               boolean var8 = this.d(var1, var2, var3);
               this.e(var1, var2, var3);
               this.a(var1, var2, var3, var8);
               this.g(var1, var2, var3);
               this.h(var1, var2, var3);
               if (var1.n != null || var2.n != null) {
                  this.a(a(this.f, var1.n), a(this.g, var2.n), var3.a("/items", "[]", false));
               }

               if (var6) {
                  this.j(var1, var2, var3);
               }
            }
         } else {
            this.a(
               var4,
               var5,
               new io.karatelabs.ext.openapi.changes.SchemaDiffWalker.SchemaPath(var3.a(), var3.b(), var3.c() + (var4 != var1 ? a(var1) : ""), var3.d() + (var5 != var2 ? a(var2) : ""), var3.e(), var3.f() + 1)
            );
         }
      }
   }

   void a(io.karatelabs.ext.openapi.changes.SchemaDiffGraph.SchemaDiffNode var1) {
      var var2 = this.i;
      boolean var3 = this.j;
      this.i = new ArrayList<>();
      this.j = false;
      this.b.e++;
      int var4 = this.b.f;

      try {
         this.b(var1.c, var1.d, e);
         var1.e = this.b.f != var4;
         var1.f = this.i.toArray(new io.karatelabs.ext.openapi.changes.SchemaDiffGraph.SchemaDiffNode[0]);
         var1.g = this.j;
         if (!this.j) {
            var1.c = null;
            var1.d = null;
         }
      } finally {
         this.b.e--;
         this.i = var2;
         this.j = var3;
      }
   }

   private void a(ComparableSchema var1, ComparableSchema var2) {
      io.karatelabs.ext.openapi.changes.SchemaDiffGraph.SchemaDiffNode var3 = this.b.i.a(var1, var2, this.c);
      if (!this.i.contains(var3)) {
         this.i.add(var3);
      }
   }

   private static String a(ComparableSchema var0) {
      return var0.o != null ? "/anyOf/0" : "/oneOf/0";
   }

   private static ComparableSchema a(DiffDocument var0, List<io.karatelabs.ext.openapi.changes.ComparableSchema.ComparableSchemaNode> var1) {
      return io.karatelabs.ext.openapi.changes.ComparableSchema.a(var0, var1 == null ? List.of() : var1);
   }

   private io.karatelabs.ext.openapi.changes.DiffSession.DiffFindingBuilder a(OpenApiChangeKind var1, ComparableSchema var2, ComparableSchema var3, io.karatelabs.ext.openapi.changes.SchemaDiffWalker.SchemaPath var4) {
      return this.a(var1, this.c, var2, null, var3, null, var4);
   }

   private io.karatelabs.ext.openapi.changes.DiffSession.DiffFindingBuilder a(OpenApiChangeKind var1, String var2, ComparableSchema var3, ComparableSchema var4, io.karatelabs.ext.openapi.changes.SchemaDiffWalker.SchemaPath var5) {
      return this.a(var1, this.c, var3, var2, var4, var2, var5);
   }

   private io.karatelabs.ext.openapi.changes.DiffSession.DiffFindingBuilder a(OpenApiChangeKind var1, io.karatelabs.ext.openapi.changes.OpenApiChangeKind.ChangeDirection var2, ComparableSchema var3, String var4, ComparableSchema var5, String var6, io.karatelabs.ext.openapi.changes.SchemaDiffWalker.SchemaPath var7) {
      io.karatelabs.ext.openapi.changes.DiffSession.DiffFindingBuilder var8 = this.b.a(var1, var2, this.h).a(var7.e() ? "property" : "schema", var7.a()).b(var7.b());
      String var9 = var4 != null && var3 != null ? var3.x.get(var4) : null;
      String var10 = var6 != null && var5 != null ? var5.x.get(var6) : null;
      if (var3 != null && !var3.a()) {
         var8.b(var7.c(), a(var3, var9 != null ? var9 : var10)).d(var4 == null ? null : b(var3, var4), null);
         if (var3.t) {
            var8.a(a());
         }
      }

      if (var5 != null && !var5.a()) {
         var8.c(var7.d(), a(var5, var10 != null ? var10 : var9)).d(null, var6 == null ? null : b(var5, var6));
      }

      return var8;
   }

   private static String a(ComparableSchema var0, String var1) {
      return var1 == null ? var0.d : var1.substring(0, var1.lastIndexOf(47));
   }

   private static Map<String, Object> a() {
      LinkedHashMap<java.lang.String, java.lang.Object> var0 = new LinkedHashMap();
      var0.put("deprecated", true);
      return var0;
   }

   private static String b(ComparableSchema var0, String var1) {
      if (var0 != null && !var0.a()) {
         String var2 = var0.x.get(var1);
         return var2 != null ? var2 : var0.d + "/" + io.karatelabs.ext.openapi.changes.DiffDocument.c(var1);
      } else {
         return null;
      }
   }

   private io.karatelabs.ext.openapi.changes.DiffSession.DiffFindingBuilder a(OpenApiChangeKind var1, String var2, ComparableSchema var3, ComparableSchema var4, io.karatelabs.ext.openapi.changes.SchemaDiffWalker.SchemaPath var5, String var6) {
      return this.a(var1, var2, var3, var4, var5).a(var2).c(var6);
   }

   private void c(ComparableSchema var1, ComparableSchema var2, io.karatelabs.ext.openapi.changes.SchemaDiffWalker.SchemaPath var3) {
      if (var1.t != var2.t) {
         this.a(
               var2.t ? io.karatelabs.ext.openapi.changes.OpenApiChangeKind.DEPRECATION_ADDED : io.karatelabs.ext.openapi.changes.OpenApiChangeKind.DEPRECATION_REMOVED,
               io.karatelabs.ext.openapi.changes.OpenApiChangeKind.ChangeDirection.NONE,
               var1,
               "deprecated",
               var2,
               "deprecated",
               var3
            )
            .a();
      }
   }

   private boolean d(ComparableSchema var1, ComparableSchema var2, io.karatelabs.ext.openapi.changes.SchemaDiffWalker.SchemaPath var3) {
      var var4 = var1.i;
      var var5 = var2.i;
      boolean var6 = false;
      if (var4 != null || var5 != null) {
         OpenApiChangeKind var7 = null;
         if (var4 == null) {
            var7 = io.karatelabs.ext.openapi.changes.OpenApiChangeKind.TYPE_NARROWED;
         } else if (var5 == null) {
            var7 = io.karatelabs.ext.openapi.changes.OpenApiChangeKind.TYPE_WIDENED;
         } else {
            boolean var8 = a(var4, var5);
            boolean var9 = a(var5, var4);
            if (!var8 || !var9) {
               var7 = var8
                  ? io.karatelabs.ext.openapi.changes.OpenApiChangeKind.TYPE_WIDENED
                  : (var9 ? io.karatelabs.ext.openapi.changes.OpenApiChangeKind.TYPE_NARROWED : io.karatelabs.ext.openapi.changes.OpenApiChangeKind.TYPE_CHANGED);
            }
         }

         if (var7 != null) {
            String var11 = a(var4) + " -> " + a(var5);
            Object var12 = var1.q.get("format");
            Object var10 = var2.q.get("format");
            if (var7 == io.karatelabs.ext.openapi.changes.OpenApiChangeKind.TYPE_CHANGED && var12 != null && (var10 == null || !io.karatelabs.ext.openapi.changes.ComparableSchema.a(var12, var10))) {
               var6 = true;
               var11 = var11 + "; format " + var12 + " -> " + (var10 == null ? "absent" : var10);
            }

            this.a(var7, "type", var1, var2, var3).c(var11).a();
         }
      }

      if (var4 != null && var5 != null && var1.j != var2.j) {
         this.a(
               var2.j ? io.karatelabs.ext.openapi.changes.OpenApiChangeKind.NULLABLE_ADDED : io.karatelabs.ext.openapi.changes.OpenApiChangeKind.NULLABLE_REMOVED,
               this.c,
               var1,
               b(var1),
               var2,
               b(var2),
               var3
            )
            .a();
      }

      return var6;
   }

   private static String b(ComparableSchema var0) {
      return var0.x.containsKey("nullable") ? "nullable" : "type";
   }

   private static String a(Set<String> var0) {
      return var0 == null ? "any" : String.join("|", var0);
   }

   static boolean a(Set<String> var0, Set<String> var1) {
      for (String var3 : var0) {
         if (!var1.contains(var3) && (!var3.equals("integer") || !var1.contains("number"))) {
            return false;
         }
      }

      return true;
   }

   private void e(ComparableSchema var1, ComparableSchema var2, io.karatelabs.ext.openapi.changes.SchemaDiffWalker.SchemaPath var3) {
      Map var4 = (Map)var1.q.get("enum");
      Map var5 = (Map)var2.q.get("enum");
      Object var6 = var1.q.get("x-extensible-enum");
      Object var7 = var2.q.get("x-extensible-enum");
      if (var6 == null && var7 == null) {
         if (var4 != null && var5 != null) {
            this.a(var4, var5, var1, var2, var3, "enum", false);
         } else if (var4 == null && var5 != null) {
            this.a(io.karatelabs.ext.openapi.changes.OpenApiChangeKind.CONSTRAINT_TIGHTENED, "enum", var1, var2, var3, "enum added").a();
         } else if (var4 != null) {
            this.a(io.karatelabs.ext.openapi.changes.OpenApiChangeKind.CONSTRAINT_LOOSENED, "enum", var1, var2, var3, "enum removed").a();
         }
      } else {
         if (var4 == null && var5 == null) {
            if (var6 != null && var7 != null) {
               this.a(a(var6), a(var7), var1, var2, var3, "x-extensible-enum", true);
            } else {
               this.a(
                     var6 == null ? io.karatelabs.ext.openapi.changes.OpenApiChangeKind.EXTENSIBLE_ENUM_ADDED : io.karatelabs.ext.openapi.changes.OpenApiChangeKind.EXTENSIBLE_ENUM_REMOVED,
                     "x-extensible-enum",
                     var1,
                     var2,
                     var3,
                     "x-extensible-enum " + (var6 == null ? "added" : "removed")
                  )
                  .a();
            }
         } else {
            if (var6 != null && var4 != null) {
               this.b.a("x-extensible-enum", b(var1, "x-extensible-enum"), "baseline", "ENUM_MIXED", null);
            }

            if (var7 != null && var5 != null) {
               this.b.a("x-extensible-enum", b(var2, "x-extensible-enum"), "current", "ENUM_MIXED", null);
            }

            if ((var6 == null || var4 == null) && (var7 == null || var5 == null)) {
               this.b
                  .a(
                     "x-extensible-enum",
                     var6 != null ? b(var1, "x-extensible-enum") : b(var2, "x-extensible-enum"),
                     "both",
                     "ENUM_MIXED",
                     "enum on one side, x-extensible-enum on the other"
                  );
            }
         }
      }
   }

   private static Map<String, Object> a(Object var0) {
      LinkedHashMap<java.lang.String, java.lang.Object> var1 = new LinkedHashMap();
      if (var0 instanceof List) {
         for (Object var4 : (List)var0) {
            var1.put(io.karatelabs.ext.common.CanonicalJson.a(var4), var4);
         }
      }

      return var1;
   }

   private void a(Map<String, Object> var1, Map<String, Object> var2, ComparableSchema var3, ComparableSchema var4, io.karatelabs.ext.openapi.changes.SchemaDiffWalker.SchemaPath var5, String var6, boolean var7) {
      var var8 = new ArrayList<>(var1.keySet());
      var var9 = new ArrayList<>(var2.keySet());

      for (String var11 : (Iterable<String>) (Iterable<?>) (var8)) {
         if (!var2.containsKey(var11)) {
            this.a(
                  var7 ? io.karatelabs.ext.openapi.changes.OpenApiChangeKind.EXTENSIBLE_ENUM_REMOVED : io.karatelabs.ext.openapi.changes.OpenApiChangeKind.ENUM_VALUE_REMOVED,
                  var11,
                  var3,
                  null,
                  var5,
                  var6,
                  var8.indexOf(var11),
                  -1
               )
               .a();
         }
      }

      for (String var14 : (Iterable<String>) (Iterable<?>) (var9)) {
         if (!var1.containsKey(var14)) {
            io.karatelabs.ext.openapi.changes.DiffSession.DiffFindingBuilder var12 = this.a(io.karatelabs.ext.openapi.changes.OpenApiChangeKind.ENUM_VALUE_ADDED, var14, null, var4, var5, var6, -1, var9.indexOf(var14));
            if (var7) {
               var12.a(io.karatelabs.ext.diff.DiffSeverity.COMPATIBLE);
            }

            var12.a();
         }
      }
   }

   private io.karatelabs.ext.openapi.changes.DiffSession.DiffFindingBuilder a(OpenApiChangeKind var1, String var2, ComparableSchema var3, ComparableSchema var4, io.karatelabs.ext.openapi.changes.SchemaDiffWalker.SchemaPath var5, String var6, int var7, int var8) {
      io.karatelabs.ext.openapi.changes.DiffSession.DiffFindingBuilder var9 = this.b.a(var1, this.c, this.h).a("enumValue", var5.a() + " enum " + var2).b(var5.b() + " enum " + var2).c(var2);
      if (var3 != null) {
         var9.b(var5.c(), a(var3, var3.x.get(var6))).d(b(var3, var6) + "/" + var7, null);
      }

      if (var4 != null) {
         var9.c(var5.d(), a(var4, var4.x.get(var6))).d(null, b(var4, var6) + "/" + var8);
      }

      return var9;
   }

   private void a(ComparableSchema var1, ComparableSchema var2, io.karatelabs.ext.openapi.changes.SchemaDiffWalker.SchemaPath var3, boolean var4) {
      for (String var6 : List.of("minLength", "minItems", "minProperties")) {
         if (!a(var6, var1, var2)) {
            BigDecimal var7 = b(var1.q.get(var6));
            BigDecimal var8 = b(var2.q.get(var6));
            int var9 = var8.compareTo(var7);
            if (var9 != 0) {
               this.a(
                     var9 > 0 ? io.karatelabs.ext.openapi.changes.OpenApiChangeKind.CONSTRAINT_TIGHTENED : io.karatelabs.ext.openapi.changes.OpenApiChangeKind.CONSTRAINT_LOOSENED,
                     var6,
                     var1,
                     var2,
                     var3,
                     var6 + " " + a(var7) + " -> " + a(var8)
                  )
                  .a();
            }
         }
      }

      for (String var15 : List.of("maxLength", "maxItems", "maxProperties")) {
         if (!a(var15, var1, var2)) {
            this.a(var15, (BigDecimal)var1.q.get(var15), (BigDecimal)var2.q.get(var15), var1, var2, var3);
         }
      }

      boolean var14 = !a("minimum", var1, var2);
      if (var14 && !a(var1.r, var2.r)) {
         boolean var16 = var1.r == null || var2.r != null && io.karatelabs.ext.openapi.changes.ComparableSchema.a(var2.r, var1.r);
         this.a(
               var16 ? io.karatelabs.ext.openapi.changes.OpenApiChangeKind.CONSTRAINT_TIGHTENED : io.karatelabs.ext.openapi.changes.OpenApiChangeKind.CONSTRAINT_LOOSENED,
               "minimum",
               var1,
               var2,
               var3,
               a(var1.r, ">") + " -> " + a(var2.r, ">")
            )
            .a();
      }

      if (var14 && !a(var1.s, var2.s)) {
         boolean var17 = var1.s == null || var2.s != null && io.karatelabs.ext.openapi.changes.ComparableSchema.b(var2.s, var1.s);
         this.a(
               var17 ? io.karatelabs.ext.openapi.changes.OpenApiChangeKind.CONSTRAINT_TIGHTENED : io.karatelabs.ext.openapi.changes.OpenApiChangeKind.CONSTRAINT_LOOSENED,
               "maximum",
               var1,
               var2,
               var3,
               a(var1.s, "<") + " -> " + a(var2.s, "<")
            )
            .a();
      }

      if (var14) {
         this.f(var1, var2, var3);
      }

      boolean var18 = Boolean.TRUE.equals(var1.q.get("uniqueItems"));
      boolean var19 = Boolean.TRUE.equals(var2.q.get("uniqueItems"));
      if (var18 != var19 && !a("uniqueItems", var1, var2)) {
         this.a(
               var19 ? io.karatelabs.ext.openapi.changes.OpenApiChangeKind.CONSTRAINT_TIGHTENED : io.karatelabs.ext.openapi.changes.OpenApiChangeKind.CONSTRAINT_LOOSENED,
               "uniqueItems",
               var1,
               var2,
               var3,
               "uniqueItems " + var18 + " -> " + var19
            )
            .a();
      }

      for (String var21 : List.of("const", "format", "pattern")) {
         boolean var10 = var1.q.containsKey(var21);
         boolean var11 = var2.q.containsKey(var21);
         if ((var10 || var11) && (!var21.equals("format") || !var4) && !a(var21, var1, var2)) {
            String var12 = var21
               + " "
               + (var10 ? io.karatelabs.ext.common.CanonicalJson.a(var1.q.get(var21)) : "absent")
               + " -> "
               + (var11 ? io.karatelabs.ext.common.CanonicalJson.a(var2.q.get(var21)) : "absent");
            if (!var10) {
               this.a(io.karatelabs.ext.openapi.changes.OpenApiChangeKind.CONSTRAINT_TIGHTENED, var21, var1, var2, var3, var12).a();
            } else if (!var11) {
               this.a(io.karatelabs.ext.openapi.changes.OpenApiChangeKind.CONSTRAINT_LOOSENED, var21, var1, var2, var3, var12).a();
            } else if (!io.karatelabs.ext.openapi.changes.ComparableSchema.a(var1.q.get(var21), var2.q.get(var21))) {
               if (var21.equals("pattern")) {
                  this.a(var1, var2, var3, var12);
               } else {
                  this.a(io.karatelabs.ext.openapi.changes.OpenApiChangeKind.CONSTRAINT_CHANGED, var21, var1, var2, var3, var12).a();
               }
            }
         }
      }
   }

   private static boolean a(String var0, ComparableSchema var1, ComparableSchema var2) {
      return a(var0, var1.i) && a(var0, var2.i);
   }

   static boolean a(String var0, Collection<?> var1) {
      String var2 = switch (var0) {
         case "minLength", "maxLength", "pattern" -> "string";
         case "minimum", "maximum", "exclusiveMinimum", "exclusiveMaximum", "multipleOf" -> "number";
         case "minItems", "maxItems", "uniqueItems" -> "array";
         case "minProperties", "maxProperties" -> "object";
         default -> null;
      };
      return var2 != null && var1 != null && !var1.contains(var2) && (!var2.equals("number") || !var1.contains("integer"));
   }

   private void a(ComparableSchema var1, ComparableSchema var2, io.karatelabs.ext.openapi.changes.SchemaDiffWalker.SchemaPath var3, String var4) {
      if (this.b.e > 0) {
         this.b.f++;
      } else {
         boolean var5 = this.c == io.karatelabs.ext.openapi.changes.OpenApiChangeKind.ChangeDirection.RESPONSE;
         var var6 = new ArrayList<>(io.karatelabs.ext.openapi.changes.CounterexampleGenerator.a(String.valueOf((var5 ? var2 : var1).q.get("pattern"))));
         var6.addAll(io.karatelabs.ext.openapi.changes.CounterexampleGenerator.a(String.valueOf((var5 ? var1 : var2).q.get("pattern"))));
         var var7 = List.of(new io.karatelabs.ext.openapi.changes.CounterexampleGenerator.SchemaContext(var5 ? this.g : this.f, var5 ? var2 : var1), new io.karatelabs.ext.openapi.changes.CounterexampleGenerator.SchemaContext(var5 ? this.f : this.g, var5 ? var1 : var2));
         io.karatelabs.ext.openapi.changes.CounterexampleGenerator.CounterexampleResult var8 = this.b.h.a(this.c, var5, var1, var2, var7, var6);
         if (var8.a()) {
            this.a(
                  var5 ? io.karatelabs.ext.openapi.changes.OpenApiChangeKind.CONSTRAINT_LOOSENED : io.karatelabs.ext.openapi.changes.OpenApiChangeKind.CONSTRAINT_TIGHTENED,
                  "pattern",
                  var1,
                  var2,
                  var3,
                  var4
               )
               .a(var8, "W5")
               .a();
         } else {
            this.b.a("pattern", b(var1, "pattern"), "both", "NO_WITNESS", var8.d() == null ? var4 : var4 + "; " + var8.d());
         }
      }
   }

   private void a(String var1, BigDecimal var2, BigDecimal var3, ComparableSchema var4, ComparableSchema var5, io.karatelabs.ext.openapi.changes.SchemaDiffWalker.SchemaPath var6) {
      if ((var2 != null || var3 != null) && (var2 == null || var3 == null || var2.compareTo(var3) != 0)) {
         boolean var7 = var2 == null || var3 != null && var3.compareTo(var2) < 0;
         this.a(
               var7 ? io.karatelabs.ext.openapi.changes.OpenApiChangeKind.CONSTRAINT_TIGHTENED : io.karatelabs.ext.openapi.changes.OpenApiChangeKind.CONSTRAINT_LOOSENED,
               var1,
               var4,
               var5,
               var6,
               var1 + " " + (var2 == null ? "absent" : a(var2)) + " -> " + (var3 == null ? "absent" : a(var3))
            )
            .a();
      }
   }

   private void f(ComparableSchema var1, ComparableSchema var2, io.karatelabs.ext.openapi.changes.SchemaDiffWalker.SchemaPath var3) {
      BigDecimal var4 = (BigDecimal)var1.q.get("multipleOf");
      BigDecimal var5 = (BigDecimal)var2.q.get("multipleOf");
      if ((var4 != null || var5 != null) && (var4 == null || var5 == null || var4.compareTo(var5) != 0)) {
         OpenApiChangeKind var6;
         if (var4 == null) {
            var6 = io.karatelabs.ext.openapi.changes.OpenApiChangeKind.CONSTRAINT_TIGHTENED;
         } else if (var5 == null) {
            var6 = io.karatelabs.ext.openapi.changes.OpenApiChangeKind.CONSTRAINT_LOOSENED;
         } else if (var5.remainder(var4).signum() == 0) {
            var6 = io.karatelabs.ext.openapi.changes.OpenApiChangeKind.CONSTRAINT_TIGHTENED;
         } else if (var4.remainder(var5).signum() == 0) {
            var6 = io.karatelabs.ext.openapi.changes.OpenApiChangeKind.CONSTRAINT_LOOSENED;
         } else {
            var6 = io.karatelabs.ext.openapi.changes.OpenApiChangeKind.CONSTRAINT_CHANGED;
         }

         this.a(var6, "multipleOf", var1, var2, var3, "multipleOf " + (var4 == null ? "absent" : a(var4)) + " -> " + (var5 == null ? "absent" : a(var5))).a();
      }
   }

   private static BigDecimal b(Object var0) {
      return var0 instanceof BigDecimal var1 ? var1 : BigDecimal.ZERO;
   }

   private static String a(BigDecimal var0) {
      return var0.stripTrailingZeros().toPlainString();
   }

   private static boolean a(io.karatelabs.ext.openapi.changes.ComparableSchema.NumericBound var0, io.karatelabs.ext.openapi.changes.ComparableSchema.NumericBound var1) {
      return var0 != null && var1 != null ? var0.a().compareTo(var1.a()) == 0 && var0.b() == var1.b() : var0 == var1;
   }

   private static String a(io.karatelabs.ext.openapi.changes.ComparableSchema.NumericBound var0, String var1) {
      return var0 == null ? "absent" : var1 + (var0.b() ? "" : "=") + " " + a(var0.a());
   }

   private void g(ComparableSchema var1, ComparableSchema var2, io.karatelabs.ext.openapi.changes.SchemaDiffWalker.SchemaPath var3) {
      boolean var4 = var1.q.containsKey("default");
      boolean var5 = var2.q.containsKey("default");
      if (var4 || var5) {
         Object var6 = var4 ? var1.q.get("default") : null;
         Object var7 = var5 ? var2.q.get("default") : null;
         if (var4 != var5 || !io.karatelabs.ext.openapi.changes.ComparableSchema.a(var6, var7)) {
            this.a(
                  io.karatelabs.ext.openapi.changes.OpenApiChangeKind.DEFAULT_CHANGED,
                  "default",
                  var1,
                  var2,
                  var3,
                  (var4 ? io.karatelabs.ext.common.CanonicalJson.a(var6) : "absent") + " -> " + (var5 ? io.karatelabs.ext.common.CanonicalJson.a(var7) : "absent")
               )
               .a();
         }
      }
   }

   private void h(ComparableSchema var1, ComparableSchema var2, io.karatelabs.ext.openapi.changes.SchemaDiffWalker.SchemaPath var3) {
      var var4 = this.a(var1, this.f);
      var var5 = this.a(var2, this.g);

      for (Entry var7 : (Iterable<Entry>) (Iterable<?>) (var4.entrySet())) {
         String var8 = (String)var7.getKey();
         ComparableSchema var9 = (ComparableSchema)var7.getValue();
         io.karatelabs.ext.openapi.changes.SchemaDiffWalker.SchemaPath var10 = var3.a(var8);
         ComparableSchema var11 = (ComparableSchema)var5.get(var8);
         boolean var12 = var1.l.contains(var8);
         if (var11 == null) {
            io.karatelabs.ext.diff.DiffSeverity var13 = this.c == io.karatelabs.ext.openapi.changes.OpenApiChangeKind.ChangeDirection.REQUEST
               ? (Boolean.FALSE.equals(var2.m) ? io.karatelabs.ext.diff.DiffSeverity.BREAKING : io.karatelabs.ext.diff.DiffSeverity.WARNING)
               : (var12 ? io.karatelabs.ext.diff.DiffSeverity.BREAKING : io.karatelabs.ext.diff.DiffSeverity.WARNING);
            io.karatelabs.ext.openapi.changes.DiffSession.DiffFindingBuilder var14 = this.b
               .a(io.karatelabs.ext.openapi.changes.OpenApiChangeKind.PROPERTY_REMOVED, this.c, this.h)
               .a(var13)
               .a("property", var10.a())
               .b(var10.b())
               .b(var10.c(), var9.a() ? b(var1, "required") : c(var1, var8))
               .c(var12 ? "required property removed" : "optional property removed");
            if (var9.t) {
               var14.a(a());
            }

            var14.a();
         } else {
            boolean var21 = var2.l.contains(var8);
            if (var12 != var21) {
               io.karatelabs.ext.openapi.changes.DiffSession.DiffFindingBuilder var22 = this.a(
                  var21 ? io.karatelabs.ext.openapi.changes.OpenApiChangeKind.PROPERTY_BECAME_REQUIRED : io.karatelabs.ext.openapi.changes.OpenApiChangeKind.PROPERTY_BECAME_OPTIONAL,
                  b(var9, var1),
                  b(var11, var2),
                  var10
               );
               if (!var9.a()) {
                  var22.b(var10.c(), c(var1, var8));
               }

               if (!var11.a()) {
                  var22.c(var10.d(), c(var2, var8));
               }

               var22.a();
            }

            this.a(var9, var11, var10);
         }
      }

      for (Entry var16 : (Iterable<Entry>) (Iterable<?>) (var5.entrySet())) {
         String var17 = (String)var16.getKey();
         if (!var4.containsKey(var17)) {
            io.karatelabs.ext.openapi.changes.SchemaDiffWalker.SchemaPath var18 = var3.a(var17);
            ComparableSchema var19 = (ComparableSchema)var16.getValue();
            boolean var20 = var2.l.contains(var17);
            this.b
               .a(var20 ? io.karatelabs.ext.openapi.changes.OpenApiChangeKind.PROPERTY_ADDED_REQUIRED : io.karatelabs.ext.openapi.changes.OpenApiChangeKind.PROPERTY_ADDED_OPTIONAL, this.c, this.h)
               .a("property", var18.a())
               .b(var18.b())
               .c(var18.d(), var19.a() ? b(var2, "required") : c(var2, var17))
               .a();
         }
      }

      this.i(var1, var2, var3);
   }

   private static String c(ComparableSchema var0, String var1) {
      return var0.k.get(var1).get(0).b();
   }

   private static ComparableSchema b(ComparableSchema var0, ComparableSchema var1) {
      return var0.a() ? var1 : var0;
   }

   private Map<String, ComparableSchema> a(ComparableSchema var1, DiffDocument var2) {
      LinkedHashMap<java.lang.String, io.karatelabs.ext.openapi.changes.ComparableSchema> var3 = new LinkedHashMap();
      var var4 = new LinkedHashSet<>(var1.k.keySet());
      var4.addAll(var1.l);

      for (String var6 : (Iterable<String>) (Iterable<?>) (var4)) {
         ComparableSchema var7 = io.karatelabs.ext.openapi.changes.ComparableSchema.a(var2, var1.k.getOrDefault(var6, List.of()));
         if (!a(var7.b(var2), this.c)) {
            var3.put(var6, var7);
         }
      }

      return var3;
   }

   static boolean a(ComparableSchema var0, io.karatelabs.ext.openapi.changes.OpenApiChangeKind.ChangeDirection var1) {
      return var1 == io.karatelabs.ext.openapi.changes.OpenApiChangeKind.ChangeDirection.REQUEST && var0.u || var1 == io.karatelabs.ext.openapi.changes.OpenApiChangeKind.ChangeDirection.RESPONSE && var0.v;
   }

   private void i(ComparableSchema var1, ComparableSchema var2, io.karatelabs.ext.openapi.changes.SchemaDiffWalker.SchemaPath var3) {
      if (var1.m != null || var2.m != null) {
         Object var4 = var1.m == null ? Boolean.TRUE : var1.m;
         Object var5 = var2.m == null ? Boolean.TRUE : var2.m;
         OpenApiChangeKind var6 = null;
         if (var4 instanceof Boolean var7 && var5 instanceof Boolean var8) {
            if (var7 == var8) {
               return;
            }

            var6 = var8 ? io.karatelabs.ext.openapi.changes.OpenApiChangeKind.ADDITIONAL_PROPERTIES_OPENED : io.karatelabs.ext.openapi.changes.OpenApiChangeKind.ADDITIONAL_PROPERTIES_CLOSED;
         } else if (Boolean.FALSE.equals(var4)) {
            var6 = io.karatelabs.ext.openapi.changes.OpenApiChangeKind.ADDITIONAL_PROPERTIES_OPENED;
         } else if (Boolean.FALSE.equals(var5)) {
            var6 = io.karatelabs.ext.openapi.changes.OpenApiChangeKind.ADDITIONAL_PROPERTIES_CLOSED;
         }

         if (var6 != null) {
            this.a(var6, "additionalProperties", var1, var2, var3).c("additionalProperties " + c(var4) + " -> " + c(var5)).a();
         } else {
            this.a(a(this.f, var4), a(this.g, var5), var3.a("/additionalProperties", ".*", false));
         }
      }
   }

   private static String c(Object var0) {
      return var0 instanceof Boolean var1 ? var1.toString() : "schema";
   }

   private static ComparableSchema a(DiffDocument var0, Object var1) {
      return var1 instanceof io.karatelabs.ext.openapi.changes.ComparableSchema.ComparableSchemaNode var2 ? io.karatelabs.ext.openapi.changes.ComparableSchema.a(var0, List.of(var2)) : io.karatelabs.ext.openapi.changes.ComparableSchema.a(var0, List.of());
   }

   private List<io.karatelabs.ext.openapi.changes.SchemaDiffWalker.CompositionBranch> a(DiffDocument var1, ComparableSchema var2) {
      ArrayList<io.karatelabs.ext.openapi.changes.SchemaDiffWalker.CompositionBranch> var3 = new ArrayList();

      for (String var5 : List.of("anyOf", "oneOf")) {
         List var6 = var5.equals("anyOf") ? var2.o : var2.p;

         for (int var7 = 0; var6 != null && var7 < var6.size(); var7++) {
            io.karatelabs.ext.openapi.changes.ComparableSchema.ComparableSchemaNode var8 = (io.karatelabs.ext.openapi.changes.ComparableSchema.ComparableSchemaNode)var6.get(var7);
            ComparableSchema var9 = io.karatelabs.ext.openapi.changes.ComparableSchema.a(var1, var8.a(), var8.b());
            ComparableSchema var10 = var9.g == null ? var9.b(var1) : var9;
            var3.add(new io.karatelabs.ext.openapi.changes.SchemaDiffWalker.CompositionBranch(var8, var9, c(var10), var5, var7, var10.g == null ? var10.b() : null));
         }
      }

      return var3;
   }

   private static String c(ComparableSchema var0) {
      if (var0.f != null) {
         return "ref:" + var0.f;
      } else {
         var var1 = var0.b();
         return "type:" + (var1 == null ? "*" : String.join(",", var1));
      }
   }

   static boolean b(Set<String> var0, Set<String> var1) {
      if (var0 != null && var1 != null) {
         for (String var3 : var0) {
            if (var1.contains(var3) || var3.equals("integer") && var1.contains("number") || var3.equals("number") && var1.contains("integer")) {
               return false;
            }
         }

         return true;
      } else {
         return false;
      }
   }

   private static boolean a(List<io.karatelabs.ext.openapi.changes.SchemaDiffWalker.CompositionBranch> var0) {
      for (int var1 = 0; var1 < var0.size(); var1++) {
         for (int var2 = var1 + 1; var2 < var0.size(); var2++) {
            if (!b(((io.karatelabs.ext.openapi.changes.SchemaDiffWalker.CompositionBranch)var0.get(var1)).f(), ((io.karatelabs.ext.openapi.changes.SchemaDiffWalker.CompositionBranch)var0.get(var2)).f())) {
               return false;
            }
         }
      }

      return true;
   }

   private static String d(ComparableSchema var0) {
      return var0.o != null && var0.p != null ? "both" : (var0.o != null ? "anyOf" : (var0.p != null ? "oneOf" : "plain"));
   }

   private static boolean a(String var0, List<io.karatelabs.ext.openapi.changes.SchemaDiffWalker.CompositionBranch> var1) {
      return var0.equals("anyOf") || var0.equals("plain") || var0.equals("oneOf") && a(var1);
   }

   private void j(ComparableSchema var1, ComparableSchema var2, io.karatelabs.ext.openapi.changes.SchemaDiffWalker.SchemaPath var3) {
      this.a(var1, var2, this.a(this.f, var1), this.a(this.g, var2), var3);
   }

   private void b(ComparableSchema var1, ComparableSchema var2, io.karatelabs.ext.openapi.changes.SchemaDiffWalker.SchemaPath var3, boolean var4) {
      ComparableSchema var5 = var4 ? var2 : var1;
      io.karatelabs.ext.openapi.changes.SchemaDiffWalker.CompositionBranch var6 = new io.karatelabs.ext.openapi.changes.SchemaDiffWalker.CompositionBranch(null, var5, c(var5), "plain", -1, var5.b());
      this.a(var1, var2, var4 ? this.a(this.f, var1) : List.of(var6), var4 ? List.of(var6) : this.a(this.g, var2), var3);
   }

   private void a(ComparableSchema var1, ComparableSchema var2, List<io.karatelabs.ext.openapi.changes.SchemaDiffWalker.CompositionBranch> var3, List<io.karatelabs.ext.openapi.changes.SchemaDiffWalker.CompositionBranch> var4, io.karatelabs.ext.openapi.changes.SchemaDiffWalker.SchemaPath var5) {
      String var6 = d(var1);
      String var7 = d(var2);
      boolean var8 = var6.equals("plain") && var2.w || var7.equals("plain") && var1.w;
      if (a(var6, var3) && a(var7, var4) && !var8) {
         this.a(var1, var2, var3, var4, var6, var7, var5);
      } else if (!var6.equals("oneOf") || !var7.equals("anyOf") || !this.a(var3, var4, var1, var2, var5)) {
         this.b(var1, var2, var3, var4, var6, var7, var5);
      }
   }

   private io.karatelabs.ext.openapi.changes.SchemaDiffWalker.CompositionBranchMatch a(List<io.karatelabs.ext.openapi.changes.SchemaDiffWalker.CompositionBranch> var1, List<io.karatelabs.ext.openapi.changes.SchemaDiffWalker.CompositionBranch> var2, io.karatelabs.ext.openapi.changes.SchemaDiffWalker.SchemaPath var3) {
      var var4 = b(var2);
      HashSet var5 = new HashSet();

      for (List<io.karatelabs.ext.openapi.changes.SchemaDiffWalker.CompositionBranch> var7 : List.of(var1, var2)) {
         HashSet var8 = new HashSet();
         var7.forEach(var2x -> {
            if (!var8.add(var2x.c())) {
               var5.add(var2x.c());
            }
         });
      }

      IdentityHashMap<io.karatelabs.ext.openapi.changes.SchemaDiffWalker.CompositionBranch, io.karatelabs.ext.openapi.changes.SchemaDiffWalker.CompositionBranch> var10 = new IdentityHashMap<>();

      for (io.karatelabs.ext.openapi.changes.SchemaDiffWalker.CompositionBranch var13 : var1) {
         io.karatelabs.ext.openapi.changes.SchemaDiffWalker.CompositionBranch var9 = (io.karatelabs.ext.openapi.changes.SchemaDiffWalker.CompositionBranch)var4.get(var13.c());
         if (!var5.contains(var13.c()) && var9 != null) {
            var10.put(var13, var9);
         }
      }

      Set<io.karatelabs.ext.openapi.changes.SchemaDiffWalker.CompositionBranch> var12 = Collections.newSetFromMap(new IdentityHashMap());
      var12.addAll(var10.values());
      Map<String, List<io.karatelabs.ext.openapi.changes.SchemaDiffWalker.CompositionBranch>> var14 = a(var1, var10.keySet());
      Map<String, List<io.karatelabs.ext.openapi.changes.SchemaDiffWalker.CompositionBranch>> var15 = a(var2, var12);
      var14.forEach((var3x, var4x) -> {
         List<io.karatelabs.ext.openapi.changes.SchemaDiffWalker.CompositionBranch> var5x = var15.get(var3x);
         if (var4x.size() == 1 && var5x != null && var5x.size() == 1) {
            var10.put((io.karatelabs.ext.openapi.changes.SchemaDiffWalker.CompositionBranch)var4x.get(0), (io.karatelabs.ext.openapi.changes.SchemaDiffWalker.CompositionBranch)var5x.get(0));
            var12.add((io.karatelabs.ext.openapi.changes.SchemaDiffWalker.CompositionBranch)var5x.get(0));
         }
      });
      var14.forEach((var5x, var6) -> {
         List<io.karatelabs.ext.openapi.changes.SchemaDiffWalker.CompositionBranch> var7x = var15.get(var5x);
         if (var7x != null) {
            List var8x = var6.stream().filter(var1xx -> !var10.containsKey(var1xx)).toList();
            List var9x = var7x.stream().filter(var1xx -> !var12.contains(var1xx)).toList();

            for (int var10x = 0; var10x < Math.min(var8x.size(), var9x.size()); var10x++) {
               if (this.b((io.karatelabs.ext.openapi.changes.SchemaDiffWalker.CompositionBranch)var8x.get(var10x), (io.karatelabs.ext.openapi.changes.SchemaDiffWalker.CompositionBranch)var9x.get(var10x), var3)) {
                  var10.put((io.karatelabs.ext.openapi.changes.SchemaDiffWalker.CompositionBranch)var8x.get(var10x), (io.karatelabs.ext.openapi.changes.SchemaDiffWalker.CompositionBranch)var9x.get(var10x));
                  var12.add((io.karatelabs.ext.openapi.changes.SchemaDiffWalker.CompositionBranch)var9x.get(var10x));
               }
            }

            for (io.karatelabs.ext.openapi.changes.SchemaDiffWalker.CompositionBranch var11 : (Iterable<io.karatelabs.ext.openapi.changes.SchemaDiffWalker.CompositionBranch>) (Iterable<?>) (var8x)) {
               if (!var10.containsKey(var11)) {
                  for (io.karatelabs.ext.openapi.changes.SchemaDiffWalker.CompositionBranch var13x : (Iterable<io.karatelabs.ext.openapi.changes.SchemaDiffWalker.CompositionBranch>) (Iterable<?>) (var9x)) {
                     if (!var12.contains(var13x) && this.b(var11, var13x, var3)) {
                        var10.put(var11, var13x);
                        var12.add(var13x);
                        break;
                     }
                  }
               }
            }

            List var15x = var8x.stream().filter(var1xx -> !var10.containsKey(var1xx)).toList();
            List var16 = var9x.stream().filter(var1xx -> !var12.contains(var1xx)).toList();

            for (int var17 = 0; var17 < Math.min(var15x.size(), var16.size()); var17++) {
               var10.put((io.karatelabs.ext.openapi.changes.SchemaDiffWalker.CompositionBranch)var15x.get(var17), (io.karatelabs.ext.openapi.changes.SchemaDiffWalker.CompositionBranch)var16.get(var17));
               var12.add((io.karatelabs.ext.openapi.changes.SchemaDiffWalker.CompositionBranch)var16.get(var17));
            }
         }
      });
      return new io.karatelabs.ext.openapi.changes.SchemaDiffWalker.CompositionBranchMatch(var10, var12);
   }

   private void a(ComparableSchema var1, ComparableSchema var2, List<io.karatelabs.ext.openapi.changes.SchemaDiffWalker.CompositionBranch> var3, List<io.karatelabs.ext.openapi.changes.SchemaDiffWalker.CompositionBranch> var4, String var5, String var6, io.karatelabs.ext.openapi.changes.SchemaDiffWalker.SchemaPath var7) {
      String var8 = var5.equals("plain") ? var6 : var5;
      io.karatelabs.ext.openapi.changes.SchemaDiffWalker.CompositionBranchMatch var9 = this.a(var3, var4, var7);

      for (io.karatelabs.ext.openapi.changes.SchemaDiffWalker.CompositionBranch var11 : var3) {
         io.karatelabs.ext.openapi.changes.SchemaDiffWalker.CompositionBranch var12 = var9.a().get(var11);
         io.karatelabs.ext.openapi.changes.SchemaDiffWalker.SchemaPath var13 = a(var7, var8, var11, var12);
         if (var12 == null) {
            this.a(var13, var11, var1, var2, false, var8, "branch " + var11.c() + " removed" + (a(var11, var4) ? "" : "; it overlaps a current branch"));
         } else if (a(var11, var12, var3, var4)) {
            this.a(var11.b(), var12.b(), var13);
         } else {
            this.a(var13, var11, var12, var1, var2, var8, var7);
         }
      }

      for (io.karatelabs.ext.openapi.changes.SchemaDiffWalker.CompositionBranch var15 : var4) {
         if (!var9.b().contains(var15)) {
            io.karatelabs.ext.openapi.changes.SchemaDiffWalker.SchemaPath var16 = a(var7, var8, null, var15);
            this.a(var16, var15, var1, var2, true, var8, "branch " + var15.c() + " added" + (a(var15, var3) ? "" : "; it overlaps a baseline branch"));
         }
      }
   }

   private static boolean a(io.karatelabs.ext.openapi.changes.SchemaDiffWalker.CompositionBranch var0, io.karatelabs.ext.openapi.changes.SchemaDiffWalker.CompositionBranch var1, List<io.karatelabs.ext.openapi.changes.SchemaDiffWalker.CompositionBranch> var2, List<io.karatelabs.ext.openapi.changes.SchemaDiffWalker.CompositionBranch> var3) {
      for (io.karatelabs.ext.openapi.changes.SchemaDiffWalker.CompositionBranch var5 : var3) {
         if (var5 != var1 && !b(var0.f(), var5.f())) {
            return false;
         }
      }

      for (io.karatelabs.ext.openapi.changes.SchemaDiffWalker.CompositionBranch var7 : var2) {
         if (var7 != var0 && !b(var1.f(), var7.f())) {
            return false;
         }
      }

      return true;
   }

   private void a(io.karatelabs.ext.openapi.changes.SchemaDiffWalker.SchemaPath var1, io.karatelabs.ext.openapi.changes.SchemaDiffWalker.CompositionBranch var2, io.karatelabs.ext.openapi.changes.SchemaDiffWalker.CompositionBranch var3, ComparableSchema var4, ComparableSchema var5, String var6, io.karatelabs.ext.openapi.changes.SchemaDiffWalker.SchemaPath var7) {
      if (this.b.e == 0 && !this.a(var2, var3, var7)) {
         boolean var8 = this.c == io.karatelabs.ext.openapi.changes.OpenApiChangeKind.ChangeDirection.RESPONSE;
         String var9 = "branch " + var2.c() + " changed inside; a " + (var8 ? "baseline" : "current") + " branch overlaps it";
         var var10 = List.of(new io.karatelabs.ext.openapi.changes.CounterexampleGenerator.SchemaContext(var8 ? this.g : this.f, (var8 ? var3 : var2).b()), new io.karatelabs.ext.openapi.changes.CounterexampleGenerator.SchemaContext(var8 ? this.f : this.g, (var8 ? var2 : var3).b()));
         io.karatelabs.ext.openapi.changes.CounterexampleGenerator.CounterexampleResult var11 = this.b.h.a(this.c, var8, var4, var5, null, var10);
         if (!var11.a()) {
            this.b.a(var6, var4.d, "both", "NO_WITNESS", a(var9, var11));
         } else {
            this.b
               .a(var8 ? io.karatelabs.ext.openapi.changes.OpenApiChangeKind.COMPOSITION_BRANCH_ADDED : io.karatelabs.ext.openapi.changes.OpenApiChangeKind.COMPOSITION_BRANCH_REMOVED, this.c, this.h)
               .a("branch", var1.a())
               .b(var1.b())
               .b(var1.c(), a(var2))
               .c(var1.d(), a(var3))
               .a(io.karatelabs.ext.diff.DiffSeverity.BREAKING)
               .c(var9)
               .a(var11, "W2")
               .a();
         }
      }

      this.a(var2.b(), var3.b(), var1);
   }

   private static io.karatelabs.ext.openapi.changes.SchemaDiffWalker.SchemaPath a(io.karatelabs.ext.openapi.changes.SchemaDiffWalker.SchemaPath var0, String var1, io.karatelabs.ext.openapi.changes.SchemaDiffWalker.CompositionBranch var2, io.karatelabs.ext.openapi.changes.SchemaDiffWalker.CompositionBranch var3) {
      io.karatelabs.ext.openapi.changes.SchemaDiffWalker.CompositionBranch var4 = var2 != null ? var2 : var3;
      return new io.karatelabs.ext.openapi.changes.SchemaDiffWalker.SchemaPath(var0.a() + "/" + var1 + "[" + var4.c() + "]", var0.b() + "|" + var4.c(), var0.c() + b(var2), var0.d() + b(var3), false, var0.f() + 1);
   }

   private void a(io.karatelabs.ext.openapi.changes.SchemaDiffWalker.SchemaPath var1, io.karatelabs.ext.openapi.changes.SchemaDiffWalker.CompositionBranch var2, ComparableSchema var3, ComparableSchema var4, boolean var5, String var6, String var7) {
      if (this.b.e > 0) {
         this.b.f++;
      } else {
         io.karatelabs.ext.openapi.changes.CounterexampleGenerator.CounterexampleResult var8 = this.b.h.a(this.c, var5, var3, var4, new io.karatelabs.ext.openapi.changes.CounterexampleGenerator.SchemaContext(var5 ? this.g : this.f, var2.b()), List.of());
         if (!var8.a()) {
            this.b.a(var6, a(var2), var5 ? "current" : "baseline", "NO_WITNESS", a(var7, var8));
         } else {
            OpenApiChangeKind var9 = var5 ? io.karatelabs.ext.openapi.changes.OpenApiChangeKind.COMPOSITION_BRANCH_ADDED : io.karatelabs.ext.openapi.changes.OpenApiChangeKind.COMPOSITION_BRANCH_REMOVED;
            io.karatelabs.ext.openapi.changes.DiffSession.DiffFindingBuilder var10 = this.b
               .a(var9, this.c, this.h)
               .a("branch", var1.a())
               .b(var1.b())
               .a(var5 == (this.c == io.karatelabs.ext.openapi.changes.OpenApiChangeKind.ChangeDirection.RESPONSE) ? io.karatelabs.ext.diff.DiffSeverity.BREAKING : io.karatelabs.ext.diff.DiffSeverity.COMPATIBLE)
               .c(var7)
               .a(var8, "W1");
            if (var5) {
               var10.c(var1.d(), a(var2));
            } else {
               var10.b(var1.c(), a(var2));
            }

            var10.a();
         }
      }
   }

   private static void a(List<io.karatelabs.ext.openapi.changes.SchemaDiffWalker.CompositionBranch> var0, DiffDocument var1, List<io.karatelabs.ext.openapi.changes.CounterexampleGenerator.SchemaContext> var2) {
      for (io.karatelabs.ext.openapi.changes.SchemaDiffWalker.CompositionBranch var4 : var0) {
         var2.add(new io.karatelabs.ext.openapi.changes.CounterexampleGenerator.SchemaContext(var1, var4.b()));
      }
   }

   private static String a(String var0, io.karatelabs.ext.openapi.changes.CounterexampleGenerator.CounterexampleResult var1) {
      return var1.d() == null ? var0 : var0 + "; " + var1.d();
   }

   private boolean a(List<io.karatelabs.ext.openapi.changes.SchemaDiffWalker.CompositionBranch> var1, List<io.karatelabs.ext.openapi.changes.SchemaDiffWalker.CompositionBranch> var2, ComparableSchema var3, ComparableSchema var4, io.karatelabs.ext.openapi.changes.SchemaDiffWalker.SchemaPath var5) {
      IdentityHashMap<io.karatelabs.ext.openapi.changes.SchemaDiffWalker.CompositionBranch, io.karatelabs.ext.openapi.changes.SchemaDiffWalker.CompositionBranch> var6 = new IdentityHashMap();
      Set<io.karatelabs.ext.openapi.changes.SchemaDiffWalker.CompositionBranch> var7 = Collections.newSetFromMap(new IdentityHashMap());

      for (io.karatelabs.ext.openapi.changes.SchemaDiffWalker.CompositionBranch var9 : var1) {
         for (io.karatelabs.ext.openapi.changes.SchemaDiffWalker.CompositionBranch var11 : var2) {
            if (!var7.contains(var11) && this.a(var9, var11, var5)) {
               var6.put(var9, var11);
               var7.add(var11);
               break;
            }
         }

         if (!var6.containsKey(var9)) {
            return false;
         }
      }

      if (this.b.e > 0) {
         this.b.f++;
         return true;
      } else {
         io.karatelabs.ext.diff.DiffSeverity var13 = this.c == io.karatelabs.ext.openapi.changes.OpenApiChangeKind.ChangeDirection.RESPONSE ? io.karatelabs.ext.diff.DiffSeverity.BREAKING : io.karatelabs.ext.diff.DiffSeverity.COMPATIBLE;
         if (var7.size() == var2.size()) {
            ArrayList var14 = new ArrayList();
            a(var2, this.g, var14);
            a(var1, this.f, var14);
            String var16 = "oneOf -> anyOf over the same branches";
            io.karatelabs.ext.openapi.changes.CounterexampleGenerator.CounterexampleResult var18 = this.b.h.a(this.c, true, var3, var4, (io.karatelabs.ext.openapi.changes.CounterexampleGenerator.SchemaContext)null, var14);
            if (var18.a()) {
               this.b
                  .a(io.karatelabs.ext.openapi.changes.OpenApiChangeKind.COMPOSITION_BRANCH_ADDED, this.c, this.h)
                  .a("branch", var5.a() + "/anyOf")
                  .b(var5.b())
                  .b(var5.c(), var3.d)
                  .c(var5.d(), var4.d)
                  .d(var3.x.get("oneOf"), var4.x.get("anyOf"))
                  .a(var13)
                  .c(var16)
                  .a(var18, "W4")
                  .a();
            } else {
               this.b.a("anyOf", var4.x.get("anyOf"), "both", "NO_WITNESS", a(var16, var18));
            }
         }

         for (io.karatelabs.ext.openapi.changes.SchemaDiffWalker.CompositionBranch var17 : var2) {
            if (!var7.contains(var17)) {
               io.karatelabs.ext.openapi.changes.CounterexampleGenerator.CounterexampleResult var19 = this.b.h.a(this.c, true, var3, var4, new io.karatelabs.ext.openapi.changes.CounterexampleGenerator.SchemaContext(this.g, var17.b()), List.of());
               if (var19.a()) {
                  String var12 = var5.a() + "/anyOf[" + var17.c() + "]";
                  this.b
                     .a(io.karatelabs.ext.openapi.changes.OpenApiChangeKind.COMPOSITION_BRANCH_ADDED, this.c, this.h)
                     .a("branch", var12)
                     .b(var5.b() + "|" + var17.c())
                     .c(var5.d() + a("anyOf", var17.e()), a(var17))
                     .a(var13)
                     .c(var17.c())
                     .a(var19, "W4")
                     .a();
               } else {
                  this.b.a("anyOf", a(var17), "current", "NO_WITNESS", a("branch " + var17.c() + " added under oneOf -> anyOf", var19));
               }
            }
         }

         var6.forEach((var2x, var3x) -> this.a(var2x.b(), var3x.b(), a(var5, "anyOf", var2x, var3x)));
         return true;
      }
   }

   private void b(ComparableSchema var1, ComparableSchema var2, List<io.karatelabs.ext.openapi.changes.SchemaDiffWalker.CompositionBranch> var3, List<io.karatelabs.ext.openapi.changes.SchemaDiffWalker.CompositionBranch> var4, String var5, String var6, io.karatelabs.ext.openapi.changes.SchemaDiffWalker.SchemaPath var7) {
      String var8 = var5.equals("plain") ? var6 : (var5.equals("both") ? "anyOf" : var5);
      String var9 = var5.equals("plain") ? var2.x.getOrDefault(var8, var2.d) : var1.x.getOrDefault(var8, var1.d);
      String var10 = var5.equals(var6)
         ? "a " + var5 + " whose branches overlap changed"
         : (
            var5.equals("plain")
               ? "composition introduced"
               : (var6.equals("plain") ? "composition dropped" : var5 + " -> " + var6 + " over overlapping branches")
         );
      io.karatelabs.ext.openapi.changes.SchemaDiffWalker.CompositionBranchMatch var11 = var5.equals(var6) ? this.a(var3, var4, var7) : null;
      if (var11 == null || this.a(var11, var3, var4, var7)) {
         String var12 = var5.equals("plain") || var6.equals("plain")
            ? "W3"
            : (var11 == null ? "W4" : (var11.a().size() == var3.size() && var11.b().size() == var4.size() ? "W2" : "W1"));
         if (this.b.e > 0) {
            this.b.f++;
            return;
         }

         boolean var13 = this.c == io.karatelabs.ext.openapi.changes.OpenApiChangeKind.ChangeDirection.RESPONSE;
         ArrayList var14 = new ArrayList();
         a(var13 ? var4 : var3, var13 ? this.g : this.f, var14);
         a(var13 ? var3 : var4, var13 ? this.f : this.g, var14);
         io.karatelabs.ext.openapi.changes.CounterexampleGenerator.CounterexampleResult var15 = this.b.h.a(this.c, var13, var1, var2, (io.karatelabs.ext.openapi.changes.CounterexampleGenerator.SchemaContext)null, var14);
         if (!var15.a()) {
            this.b.a(var8, var9, "both", "NO_WITNESS", a(var10, var15));
         } else {
            this.b
               .a(var13 ? io.karatelabs.ext.openapi.changes.OpenApiChangeKind.COMPOSITION_BRANCH_ADDED : io.karatelabs.ext.openapi.changes.OpenApiChangeKind.COMPOSITION_BRANCH_REMOVED, this.c, this.h)
               .a("branch", var7.a() + "/" + var8)
               .b(var7.b())
               .b(var7.c(), var1.d)
               .c(var7.d(), var2.d)
               .d(var1.x.get(var5), var2.x.get(var6))
               .a(io.karatelabs.ext.diff.DiffSeverity.BREAKING)
               .c(var10)
               .a(var15, var12)
               .a();
         }
      }

      if (var11 == null) {
         var11 = this.a(var3, var4, var7);
      }

      for (io.karatelabs.ext.openapi.changes.SchemaDiffWalker.CompositionBranch var17 : var3) {
         io.karatelabs.ext.openapi.changes.SchemaDiffWalker.CompositionBranch var18 = var11.a().get(var17);
         if (var18 != null) {
            this.a(var17.b(), var18.b(), a(var7, var8, var17, var18));
         }
      }
   }

   private boolean a(io.karatelabs.ext.openapi.changes.SchemaDiffWalker.CompositionBranchMatch var1, List<io.karatelabs.ext.openapi.changes.SchemaDiffWalker.CompositionBranch> var2, List<io.karatelabs.ext.openapi.changes.SchemaDiffWalker.CompositionBranch> var3, io.karatelabs.ext.openapi.changes.SchemaDiffWalker.SchemaPath var4) {
      if (var1.a().size() == var2.size() && var1.b().size() == var3.size()) {
         for (Entry var6 : var1.a().entrySet()) {
            if (!this.a((io.karatelabs.ext.openapi.changes.SchemaDiffWalker.CompositionBranch)var6.getKey(), (io.karatelabs.ext.openapi.changes.SchemaDiffWalker.CompositionBranch)var6.getValue(), var4)) {
               return true;
            }
         }

         return false;
      } else {
         return true;
      }
   }

   private boolean a(io.karatelabs.ext.openapi.changes.SchemaDiffWalker.CompositionBranch var1, io.karatelabs.ext.openapi.changes.SchemaDiffWalker.CompositionBranch var2, io.karatelabs.ext.openapi.changes.SchemaDiffWalker.SchemaPath var3) {
      if (this.i != null) {
         this.a(var1.b(), var2.b());
         return true;
      } else {
         return !this.b.i.a(this, var1.b(), var2.b(), var3.f() + 1);
      }
   }

   private boolean b(io.karatelabs.ext.openapi.changes.SchemaDiffWalker.CompositionBranch var1, io.karatelabs.ext.openapi.changes.SchemaDiffWalker.CompositionBranch var2, io.karatelabs.ext.openapi.changes.SchemaDiffWalker.SchemaPath var3) {
      boolean var4 = this.b.i.a(this, var1.b(), var2.b(), var3.f() + 1);
      if (this.i != null) {
         this.j = true;
         if (var4) {
            this.b.f++;
         }
      }

      return !var4;
   }

   private static String a(io.karatelabs.ext.openapi.changes.SchemaDiffWalker.CompositionBranch var0) {
      return var0.a() != null ? var0.a().b() : var0.b().d;
   }

   private static String b(io.karatelabs.ext.openapi.changes.SchemaDiffWalker.CompositionBranch var0) {
      return var0 == null ? "" : a(var0.d(), var0.e());
   }

   private static String a(String var0, int var1) {
      return var0 != null && !var0.equals("plain") && var1 >= 0 ? "/" + var0 + "/" + var1 : "";
   }

   private static Map<String, io.karatelabs.ext.openapi.changes.SchemaDiffWalker.CompositionBranch> b(List<io.karatelabs.ext.openapi.changes.SchemaDiffWalker.CompositionBranch> var0) {
      LinkedHashMap<java.lang.String, io.karatelabs.ext.openapi.changes.SchemaDiffWalker.CompositionBranch> var1 = new LinkedHashMap();

      for (io.karatelabs.ext.openapi.changes.SchemaDiffWalker.CompositionBranch var3 : var0) {
         var1.putIfAbsent(var3.c(), var3);
      }

      return var1;
   }

   private static Map<String, List<io.karatelabs.ext.openapi.changes.SchemaDiffWalker.CompositionBranch>> a(List<io.karatelabs.ext.openapi.changes.SchemaDiffWalker.CompositionBranch> var0, Set<io.karatelabs.ext.openapi.changes.SchemaDiffWalker.CompositionBranch> var1) {
      LinkedHashMap<String, List<io.karatelabs.ext.openapi.changes.SchemaDiffWalker.CompositionBranch>> var2 = new LinkedHashMap<>();

      for (io.karatelabs.ext.openapi.changes.SchemaDiffWalker.CompositionBranch var4 : var0) {
         if (!var1.contains(var4)) {
            var2.computeIfAbsent(var4.f() == null ? "*" : String.join(",", var4.f()), var0x -> new ArrayList<>()).add(var4);
         }
      }

      return var2;
   }

   private static boolean a(io.karatelabs.ext.openapi.changes.SchemaDiffWalker.CompositionBranch var0, List<io.karatelabs.ext.openapi.changes.SchemaDiffWalker.CompositionBranch> var1) {
      for (io.karatelabs.ext.openapi.changes.SchemaDiffWalker.CompositionBranch var3 : var1) {
         if (!b(var0.f(), var3.f())) {
            return false;
         }
      }

      return true;
   }

   record SchemaPath(String a, String b, String c, String d, boolean e, int f) {


      io.karatelabs.ext.openapi.changes.SchemaDiffWalker.SchemaPath a(String var1, String var2, boolean var3) {
         return new io.karatelabs.ext.openapi.changes.SchemaDiffWalker.SchemaPath(this.a + var1, this.b + (this.f == 0 ? " " : "") + var2, this.c + var1, this.d + var1, var3, this.f + 1);
      }

      io.karatelabs.ext.openapi.changes.SchemaDiffWalker.SchemaPath a(String var1) {
         return this.a("/properties/" + io.karatelabs.ext.openapi.changes.DiffDocument.c(var1), "." + var1, true);
      }
   }

   private record CompositionBranch(io.karatelabs.ext.openapi.changes.ComparableSchema.ComparableSchemaNode a, ComparableSchema b, String c, String d, int e, Set<String> f) {

   }

   private record CompositionBranchMatch(Map<io.karatelabs.ext.openapi.changes.SchemaDiffWalker.CompositionBranch, io.karatelabs.ext.openapi.changes.SchemaDiffWalker.CompositionBranch> a, Set<io.karatelabs.ext.openapi.changes.SchemaDiffWalker.CompositionBranch> b) {

   }
}
