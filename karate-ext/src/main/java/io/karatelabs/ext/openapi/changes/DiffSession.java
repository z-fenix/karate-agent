package io.karatelabs.ext.openapi.changes;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class DiffSession {
   static final String a = "openapi.diff/1";
   final DiffDocument b;
   final DiffDocument c;
   final List<io.karatelabs.ext.openapi.changes.DiffSession.DiffFindingBuilder> d = new ArrayList<>();
   private final Map<String, io.karatelabs.ext.diff.UnevaluatedDiff> j = new LinkedHashMap<>();
   int e;
   int f;
   final Set<String> g = new HashSet<>();
   final CounterexampleGenerator h;
   final SchemaDiffGraph i = new SchemaDiffGraph();

   DiffSession(DiffDocument var1, DiffDocument var2) {
      this.b = var1;
      this.c = var2;
      this.h = new CounterexampleGenerator(var1, var2);
   }

   io.karatelabs.ext.openapi.changes.DiffSession.DiffFindingBuilder a(OpenApiChangeKind var1, io.karatelabs.ext.openapi.changes.OpenApiChangeKind.ChangeDirection var2, io.karatelabs.ext.openapi.changes.DiffSession.OperationContext var3) {
      return new io.karatelabs.ext.openapi.changes.DiffSession.DiffFindingBuilder(var1, var2, var3);
   }

   void a(String var1, String var2, String var3, String var4, Object var5) {
      this.f++;
      if (this.e <= 0) {
         String var6 = var3 + "\u0000" + var1 + "\u0000" + var2 + "\u0000" + var4;
         if (!this.j.containsKey(var6)) {
            this.j.put(var6, new io.karatelabs.ext.diff.UnevaluatedDiff(var4, var1, var3, var2, var5));
         }
      }
   }

   void a(ComparableSchema var1, String var2) {
      for (io.karatelabs.ext.openapi.changes.ComparableSchema.SchemaIssue var4 : var1.h) {
         this.a(var4.a(), var4.b(), var2, var4.c(), var4.d());
      }
   }

   List<io.karatelabs.ext.diff.UnevaluatedDiff> a() {
      return new ArrayList<>(this.j.values());
   }

   final class DiffFindingBuilder {
      final OpenApiChangeKind a;
      final io.karatelabs.ext.openapi.changes.OpenApiChangeKind.ChangeDirection b;
      final io.karatelabs.ext.openapi.changes.DiffSession.OperationContext c;
      io.karatelabs.ext.diff.DiffSeverity d;
      String e;
      String f;
      String g;
      String h;
      String i;
      String j;
      String k;
      String l;
      String m;
      String n;
      String o;
      Map<String, Object> p;
      io.karatelabs.ext.openapi.changes.CounterexampleGenerator.CounterexampleResult q;
      String r;
      Map<String, Object> s;

      DiffFindingBuilder(OpenApiChangeKind var2, io.karatelabs.ext.openapi.changes.OpenApiChangeKind.ChangeDirection var3, io.karatelabs.ext.openapi.changes.DiffSession.OperationContext var4) {
         this.a = var2;
         this.b = var3;
         this.c = var4;
         this.d = var2.level(var3);
      }

      io.karatelabs.ext.openapi.changes.DiffSession.DiffFindingBuilder a(io.karatelabs.ext.diff.DiffSeverity var1) {
         this.d = var1;
         return this;
      }

      io.karatelabs.ext.openapi.changes.DiffSession.DiffFindingBuilder a(String var1, String var2) {
         this.g = var1;
         this.h = var2;
         return this;
      }

      io.karatelabs.ext.openapi.changes.DiffSession.DiffFindingBuilder a(String var1) {
         this.i = var1;
         return this;
      }

      io.karatelabs.ext.openapi.changes.DiffSession.DiffFindingBuilder b(String var1) {
         this.e = var1;
         return this;
      }

      io.karatelabs.ext.openapi.changes.DiffSession.DiffFindingBuilder c(String var1) {
         this.f = var1;
         return this;
      }

      io.karatelabs.ext.openapi.changes.DiffSession.DiffFindingBuilder b(String var1, String var2) {
         this.j = var1;
         this.l = var2;
         this.n = var2;
         return this;
      }

      io.karatelabs.ext.openapi.changes.DiffSession.DiffFindingBuilder c(String var1, String var2) {
         this.k = var1;
         this.m = var2;
         this.o = var2;
         return this;
      }

      io.karatelabs.ext.openapi.changes.DiffSession.DiffFindingBuilder d(String var1, String var2) {
         if (this.l != null && var1 != null) {
            this.n = var1;
         }

         if (this.m != null && var2 != null) {
            this.o = var2;
         }

         return this;
      }

      io.karatelabs.ext.openapi.changes.DiffSession.DiffFindingBuilder a(Map<String, Object> var1) {
         this.p = var1 != null && !var1.isEmpty() ? var1 : null;
         return this;
      }

      io.karatelabs.ext.openapi.changes.DiffSession.DiffFindingBuilder a(io.karatelabs.ext.openapi.changes.CounterexampleGenerator.CounterexampleResult var1, String var2) {
         this.q = var1;
         this.r = var2;
         return this;
      }

      io.karatelabs.ext.openapi.changes.DiffSession.DiffFindingBuilder b(Map<String, Object> var1) {
         this.s = var1;
         return this;
      }

      void a() {
         if (this.d != null) {
            if (!this.a.annotation()) {
               DiffSession.this.f++;
            }

            if (DiffSession.this.e == 0) {
               DiffSession.this.d.add(this);
            }
         }
      }

      io.karatelabs.ext.diff.DiffFinding a(io.karatelabs.ext.diff.DiffReport var1, CoverageJoinIndex var2, io.karatelabs.ext.lint.WaiverLedger var3) {
         io.karatelabs.ext.diff.DiffFinding var4 = var1.a(this.a.name(), this.d);
         var4.a(this.e).b(this.f);
         if (this.c != null) {
            var4.a("operation", this.c.a()).a(var2.a((Map<?, ?>)this.c.b().get("graphIds")));
         }

         String var5 = this.j != null ? this.j : this.k;
         String var6 = this.l != null ? this.l : this.m;
         if (var3 != null) {
            var4.b(var3.findApplicableWaiver(this.a.name(), var5, var6));
         }

         return var4.a(this.g, this.h, this.i)
            .c(this.b.wire())
            .b(var5, var6)
            .a(this.l == null ? null : DiffSession.this.b.b(this.n), this.m == null ? null : DiffSession.this.c.b(this.o))
            .a("operation", this.c == null ? null : this.c.b())
            .a("moved", this.s)
            .a("lifecycle", this.p)
            .a("witness", this.q == null ? null : this.q.a(this.r))
            .a("oasdiff", io.karatelabs.ext.openapi.changes.OasdiffCrosswalk.a(this.a.name(), this.b.wire(), this.i, this.g, var5, var6));
      }
   }

   record OperationContext(String a, Map<String, Object> b) {

   }
}
