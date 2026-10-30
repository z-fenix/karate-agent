package io.karatelabs.ext.coverage.collect;

import io.karatelabs.ext.coverage.Action;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public record TestExecution(String g, String h, String i, String j, Long k, Long l, Long m, io.karatelabs.ext.coverage.collect.TestExecution.ExecutionPhase n, io.karatelabs.ext.coverage.collect.TestExecution.ExecutionOutcome o, boolean p, io.karatelabs.ext.coverage.AssertionStrengthSummary q, String r, String s, String t, int u, Set<String> v, String w, List<Action> x, List<io.karatelabs.ext.coverage.graph.CoverageHit> y, List<Map<String, Object>> z, List<ExecutionFragment> A, List<Map<String, Object>> B, Map<String, Object> C, List<io.karatelabs.ext.coverage.graph.TestNode> D, Map<String, Object> E) {
   public static final String a = "legacyCaseMapped";

   public TestExecution(
      String var1,
      String var2,
      String var3,
      String var4,
      Long var5,
      Long var6,
      Long var7,
      io.karatelabs.ext.coverage.collect.TestExecution.ExecutionPhase var8,
      io.karatelabs.ext.coverage.collect.TestExecution.ExecutionOutcome var9,
      boolean var10,
      io.karatelabs.ext.coverage.AssertionStrengthSummary var11,
      String var12,
      String var13,
      String var14,
      int var15,
      Set<String> var16,
      String var17,
      List<Action> var18,
      List<io.karatelabs.ext.coverage.graph.CoverageHit> var19,
      List<Map<String, Object>> var20,
      List<ExecutionFragment> var21,
      List<Map<String, Object>> var22
   ) {
      this(
         var1,
         var2,
         var3,
         var4,
         var5,
         var6,
         var7,
         var8,
         var9,
         var10,
         var11,
         var12,
         var13,
         var14,
         var15,
         var16,
         var17,
         var18,
         var19,
         var20,
         var21,
         var22,
         Map.of(),
         List.of(),
         Map.of()
      );
   }

   public TestExecution(
      String var1,
      String var2,
      String var3,
      String var4,
      Long var5,
      Long var6,
      Long var7,
      io.karatelabs.ext.coverage.collect.TestExecution.ExecutionPhase var8,
      io.karatelabs.ext.coverage.collect.TestExecution.ExecutionOutcome var9,
      boolean var10,
      io.karatelabs.ext.coverage.AssertionStrengthSummary var11,
      String var12,
      String var13,
      String var14,
      int var15,
      Set<String> var16,
      String var17,
      List<Action> var18,
      List<io.karatelabs.ext.coverage.graph.CoverageHit> var19,
      List<Map<String, Object>> var20,
      List<ExecutionFragment> var21,
      List<Map<String, Object>> var22,
      Map<String, Object> var23,
      List<io.karatelabs.ext.coverage.graph.TestNode> var24
   ) {
      this(
         var1,
         var2,
         var3,
         var4,
         var5,
         var6,
         var7,
         var8,
         var9,
         var10,
         var11,
         var12,
         var13,
         var14,
         var15,
         var16,
         var17,
         var18,
         var19,
         var20,
         var21,
         var22,
         var23,
         var24,
         Map.of()
      );
   }


   public boolean a() {
      return this.h == null;
   }

   public boolean b() {
      return this.n == io.karatelabs.ext.coverage.collect.TestExecution.ExecutionPhase.TERMINAL;
   }

   public TestExecution c() {
      var var1 = new ArrayList<>(this.B);
      var1.add(io.karatelabs.ext.coverage.collect.ExecutionFragmentAggregator.a("unfinalizedRun", "the run has not finalized its evidence"));
      return new TestExecution(
         this.g,
         this.h,
         this.i,
         this.j,
         this.k,
         this.l,
         this.m,
         io.karatelabs.ext.coverage.collect.TestExecution.ExecutionPhase.RUNNING,
         io.karatelabs.ext.coverage.collect.TestExecution.ExecutionOutcome.UNKNOWN,
         this.p,
         null,
         this.r,
         this.s,
         this.t,
         this.u,
         this.v,
         this.w,
         this.x,
         this.y,
         this.z,
         this.A,
         var1,
         this.C,
         this.D,
         this.E
      );
   }

   public TestExecution a(Map<String, Object> var1) {
      var var2 = new LinkedHashMap<>(this.C);
      var2.putAll(var1);
      return new TestExecution(
         this.g,
         this.h,
         this.i,
         this.j,
         this.k,
         this.l,
         this.m,
         this.n,
         this.o,
         this.p,
         this.q,
         this.r,
         this.s,
         this.t,
         this.u,
         this.v,
         this.w,
         this.x,
         this.y,
         this.z,
         this.A,
         this.B,
         var2,
         this.D,
         this.E
      );
   }

   public TestExecution b(Map<String, Object> var1) {
      var var2 = new LinkedHashMap<>(this.E);
      var2.putAll(var1);
      return new TestExecution(
         this.g,
         this.h,
         this.i,
         this.j,
         this.k,
         this.l,
         this.m,
         this.n,
         this.o,
         this.p,
         this.q,
         this.r,
         this.s,
         this.t,
         this.u,
         this.v,
         this.w,
         this.x,
         this.y,
         this.z,
         this.A,
         this.B,
         this.C,
         this.D,
         var2
      );
   }

   public TestExecution a(String var1, String var2) {
      var var3 = new ArrayList<>(this.B);
      var3.add(io.karatelabs.ext.coverage.collect.ExecutionFragmentAggregator.a(var1, var2));
      return new TestExecution(
         this.g,
         this.h,
         this.i,
         this.j,
         this.k,
         this.l,
         this.m,
         this.n,
         this.o,
         this.p,
         this.q,
         this.r,
         this.s,
         this.t,
         this.u,
         this.v,
         this.w,
         this.x,
         this.y,
         this.z,
         this.A,
         var3,
         this.C,
         this.D,
         this.E
      );
   }

   public TestExecution b(String var1, String var2) {
      return new TestExecution(
            this.g,
            this.h,
            var1,
            this.j,
            this.k,
            this.l,
            this.m,
            this.n,
            this.o,
            this.p,
            this.q,
            var2,
            this.s,
            this.t,
            this.u,
            this.v,
            this.w,
            this.x,
            this.y,
            this.z,
            this.A,
            this.B,
            this.C,
            this.D,
            this.E
         )
         .a("legacyCaseMapped", "positional case " + this.i + " mapped to deck case " + var1);
   }

   public boolean a(String var1) {
      for (Map var3 : this.B) {
         if (var1.equals(var3.get("kind"))) {
            return true;
         }
      }

      return false;
   }

   public io.karatelabs.ext.coverage.graph.TestNode d() {
      io.karatelabs.ext.coverage.graph.TestNode.TestStatus var1 = switch (this.o) {
         case PASSED -> io.karatelabs.ext.coverage.graph.TestNode.TestStatus.PASSED;
         case FAILED -> io.karatelabs.ext.coverage.graph.TestNode.TestStatus.FAILED;
         default -> io.karatelabs.ext.coverage.graph.TestNode.TestStatus.SKIPPED;
      };
      if (this.n == io.karatelabs.ext.coverage.collect.TestExecution.ExecutionPhase.RUNNING) {
         var1 = io.karatelabs.ext.coverage.graph.TestNode.TestStatus.SKIPPED;
      }

      return new io.karatelabs.ext.coverage.graph.TestNode(this.r, this.s, this.t, this.u, this.v, var1, this.q);
   }

   public int e() {
      int var1 = this.g.lastIndexOf(35);
      if (var1 < 0) {
         return 0;
      } else {
         String var2 = this.g.substring(var1 + 1);
         var2 = var2.startsWith("s") ? var2.substring(1) : var2;

         try {
            return Integer.parseInt(var2);
         } catch (NumberFormatException var4) {
            return 0;
         }
      }
   }

   public Map<String, Object> f() {
      LinkedHashMap<java.lang.String, java.lang.Object> var1 = new LinkedHashMap();
      var1.put("id", this.g);
      if (this.h != null) {
         var1.put("definition", this.h);
      }

      if (this.i != null) {
         var1.put("caseId", this.i);
      }

      var1.put("runId", this.j);
      if (this.k != null) {
         var1.put("startedAt", this.k);
      }

      if (this.l != null) {
         var1.put("endedAt", this.l);
      }

      var1.put("lifecycle", this.n.name().toLowerCase());
      var1.put("outcome", this.o.wire());
      if (this.q != null) {
         var1.put("assertedProportion", this.q.b());
      }

      var1.put("test", this.r);
      var1.put("lane", this.w);
      if (!this.C.isEmpty()) {
         var1.put("provenance", this.C);
      }

      if (!this.D.isEmpty()) {
         var1.put("subjects", this.D.stream().map(io.karatelabs.ext.coverage.graph.TestNode::a).toList());
      }

      if (!this.E.isEmpty()) {
         var1.put("artifacts", this.E);
      }

      if (!this.B.isEmpty()) {
         var1.put("findings", this.B);
      }

      return var1;
   }

   public String g() {
      return this.g;
   }

   public String h() {
      return this.h;
   }

   public String i() {
      return this.i;
   }

   public String j() {
      return this.j;
   }

   public Long k() {
      return this.k;
   }

   public Long l() {
      return this.l;
   }

   public Long m() {
      return this.m;
   }

   public io.karatelabs.ext.coverage.collect.TestExecution.ExecutionPhase n() {
      return this.n;
   }

   public io.karatelabs.ext.coverage.collect.TestExecution.ExecutionOutcome o() {
      return this.o;
   }

   public boolean p() {
      return this.p;
   }

   public io.karatelabs.ext.coverage.AssertionStrengthSummary q() {
      return this.q;
   }

   public String r() {
      return this.r;
   }

   public String s() {
      return this.s;
   }

   public String t() {
      return this.t;
   }

   public int u() {
      return this.u;
   }

   public Set<String> v() {
      return this.v;
   }

   public String w() {
      return this.w;
   }

   public List<Action> x() {
      return this.x;
   }

   public List<io.karatelabs.ext.coverage.graph.CoverageHit> y() {
      return this.y;
   }

   public List<Map<String, Object>> z() {
      return this.z;
   }

   public List<ExecutionFragment> A() {
      return this.A;
   }

   public List<Map<String, Object>> B() {
      return this.B;
   }

   public Map<String, Object> C() {
      return this.C;
   }

   public List<io.karatelabs.ext.coverage.graph.TestNode> D() {
      return this.D;
   }

   public Map<String, Object> E() {
      return this.E;
   }

   public static enum ExecutionPhase {
      RUNNING,
      TERMINAL;
   }

   public static enum ExecutionOutcome {
      PASSED,
      FAILED,
      SKIPPED,
      UNKNOWN;

      public static io.karatelabs.ext.coverage.collect.TestExecution.ExecutionOutcome of(String var0) {
         if (var0 == null) {
            return UNKNOWN;
         } else {
            return switch (var0) {
               case "passed" -> PASSED;
               case "failed" -> FAILED;
               case "skipped" -> SKIPPED;
               default -> UNKNOWN;
            };
         }
      }

      public io.karatelabs.ext.coverage.collect.TestExecution.ExecutionOutcome worse(io.karatelabs.ext.coverage.collect.TestExecution.ExecutionOutcome var1) {
         return var1 != null && this.rank() < var1.rank() ? var1 : this;
      }

      private int rank() {
         return switch (this) {
            case PASSED -> 0;
            case FAILED -> 3;
            case SKIPPED -> 1;
            case UNKNOWN -> 2;
         };
      }

      public String wire() {
         return this.name().toLowerCase();
      }
   }
}
