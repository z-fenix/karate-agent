package io.karatelabs.ext.rules.twin;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public record TwinWalkResult(List<io.karatelabs.ext.rules.twin.TwinWalkResult.WalkState> e, List<io.karatelabs.ext.rules.twin.TwinWalkResult.WalkTransition> f, List<io.karatelabs.ext.rules.twin.TwinWalkResult.WalkRefusal> g, List<io.karatelabs.ext.rules.twin.TwinWalkResult.WalkError> h, io.karatelabs.ext.rules.twin.TwinWalkResult.WalkPruningStats i, io.karatelabs.ext.rules.twin.TwinWalkResult.WalkCeiling j, Map<String, Object> k, Map<String, Object> l, List<io.karatelabs.ext.rules.twin.TwinWalkResult.Counterexample> m) {

   public TwinWalkResult(List<io.karatelabs.ext.rules.twin.TwinWalkResult.WalkState> e, List<io.karatelabs.ext.rules.twin.TwinWalkResult.WalkTransition> f, List<io.karatelabs.ext.rules.twin.TwinWalkResult.WalkRefusal> g, List<io.karatelabs.ext.rules.twin.TwinWalkResult.WalkError> h, io.karatelabs.ext.rules.twin.TwinWalkResult.WalkPruningStats i, io.karatelabs.ext.rules.twin.TwinWalkResult.WalkCeiling j, Map<String, Object> k, Map<String, Object> l, List<io.karatelabs.ext.rules.twin.TwinWalkResult.Counterexample> m) {
      this.e = e;
      this.f = f;
      this.g = g;
      this.h = h;
      this.i = i;
      this.j = j;
      this.k = k;
      this.l = l;
      this.m = m;
   }

   public Map<String, Object> a() {
      LinkedHashMap<java.lang.String, java.lang.Object> var1 = new LinkedHashMap();
      var1.put("nodes", a(this.e, TwinWalkResult::a));
      var1.put("edges", a(this.f, TwinWalkResult::a));
      var1.put("refusals", a(this.g, TwinWalkResult::a));
      var1.put("errors", a(this.h, TwinWalkResult::a));
      var1.put("pruning", a(this.i));
      var1.put("ceilings", a(this.j, true));
      var1.put("census", this.k);
      var1.put("disclosures", this.l);
      var1.put("counterexamples", a(this.m, TwinWalkResult::a));
      return var1;
   }

   public Map<String, Object> b() {
      var var1 = this.a();
      var1.put("ceilings", a(this.j, false));
      return var1;
   }

   public String c() {
      return io.karatelabs.ext.rules.twin.CanonicalJson.a(io.karatelabs.ext.rules.twin.CanonicalJson.c(this.b()));
   }

   public Map<String, Object> d() {
      return a(this.j, true);
   }

   private static Map<String, Object> a(io.karatelabs.ext.rules.twin.TwinWalkResult.WalkState var0) {
      LinkedHashMap<java.lang.String, java.lang.Object> var1 = new LinkedHashMap();
      var1.put("id", var0.a());
      var1.put("key", var0.b());
      var1.put("world", var0.c());
      var1.put("depth", var0.d());
      var1.put("states", var0.e());
      var1.put("invariants", a(var0.f(), var0x -> {
         LinkedHashMap<java.lang.String, java.lang.String> var1x = new LinkedHashMap();
         var1x.put("name", var0x.a());
         var1x.put("status", var0x.b());
         return var1x;
      }));
      return var1;
   }

   private static Map<String, Object> a(io.karatelabs.ext.rules.twin.TwinWalkResult.WalkTransition var0) {
      LinkedHashMap<java.lang.String, java.lang.Object> var1 = new LinkedHashMap();
      var1.put("from", var0.a());
      var1.put("command", var0.b());
      var1.put("args", var0.c());
      var1.put("to", var0.d());
      var1.put("toKey", var0.e());
      var1.put("noop", var0.f());
      var1.put("postcondition", var0.g());
      return var1;
   }

   private static Map<String, Object> a(io.karatelabs.ext.rules.twin.TwinWalkResult.WalkRefusal var0) {
      LinkedHashMap<java.lang.String, java.lang.Object> var1 = new LinkedHashMap();
      var1.put("node", var0.a());
      var1.put("command", var0.b());
      var1.put("args", var0.c());
      var1.put("stage", var0.d());
      var1.put("reason", var0.e());
      if (var0.f() != null) {
         var1.put("detail", var0.f());
      }

      return var1;
   }

   private static Map<String, Object> a(io.karatelabs.ext.rules.twin.TwinWalkResult.WalkError var0) {
      LinkedHashMap<java.lang.String, java.lang.Object> var1 = new LinkedHashMap();
      var1.put("node", var0.a());
      var1.put("command", var0.b());
      var1.put("args", var0.c());
      var1.put("stage", var0.d());
      var1.put("message", var0.e());
      if (var0.f() != null) {
         var1.put("code", var0.f());
      }

      return var1;
   }

   private static Map<String, Object> a(io.karatelabs.ext.rules.twin.TwinWalkResult.WalkPruningStats var0) {
      LinkedHashMap<java.lang.String, java.lang.Object> var1 = new LinkedHashMap();
      var1.put("depthPruned", var0.a());
      var1.put("refusedByGuard", var0.b());
      var1.put("mergedAsVisited", var0.c());
      var1.put("noop", var0.d());
      var1.put("inadmissible", var0.e());
      return var1;
   }

   private static Map<String, Object> a(io.karatelabs.ext.rules.twin.TwinWalkResult.WalkCeiling var0, boolean var1) {
      LinkedHashMap<java.lang.String, java.lang.Object> var2 = new LinkedHashMap();
      var2.put("maxDepthReached", var0.d().a());
      var2.put("nodesVisited", var0.d().b());
      var2.put("runsExecuted", var0.d().c());
      if (var1) {
         var2.put("wallSeconds", var0.d().d());
      }

      LinkedHashMap<java.lang.String, java.lang.Object> var3 = new LinkedHashMap();
      var3.put("hit", var0.a());
      var3.put("alsoTripped", var0.b());
      var3.put("frontier", var0.c());
      var3.put("usage", var2);
      return var3;
   }

   private static Map<String, Object> a(io.karatelabs.ext.rules.twin.TwinWalkResult.Counterexample var0) {
      LinkedHashMap<java.lang.String, java.lang.Object> var1 = new LinkedHashMap();
      var1.put("id", var0.a());
      var1.put("kind", var0.b());
      if (var0.c() != null) {
         var1.put("name", var0.c());
      }

      var1.put("command", var0.d());
      var1.put("args", var0.e());
      if (var0.h() != null) {
         var1.put("stage", var0.h());
         var1.put("reason", var0.i());
      }

      var1.put("node", var0.f());
      var1.put("path", var0.g());
      return var1;
   }

   private static <T, R> List<R> a(List<T> var0, Function<T, R> var1) {
      ArrayList<R> var2 = new ArrayList(var0.size());

      for (T var4 : var0) {
         var2.add(var1.apply(var4));
      }

      return var2;
   }

   public List<io.karatelabs.ext.rules.twin.TwinWalkResult.WalkState> e() {
      return this.e;
   }

   public List<io.karatelabs.ext.rules.twin.TwinWalkResult.WalkTransition> f() {
      return this.f;
   }

   public List<io.karatelabs.ext.rules.twin.TwinWalkResult.WalkRefusal> g() {
      return this.g;
   }

   public List<io.karatelabs.ext.rules.twin.TwinWalkResult.WalkError> h() {
      return this.h;
   }

   public io.karatelabs.ext.rules.twin.TwinWalkResult.WalkPruningStats i() {
      return this.i;
   }

   public io.karatelabs.ext.rules.twin.TwinWalkResult.WalkCeiling j() {
      return this.j;
   }

   public Map<String, Object> k() {
      return this.k;
   }

   public Map<String, Object> l() {
      return this.l;
   }

   public List<io.karatelabs.ext.rules.twin.TwinWalkResult.Counterexample> m() {
      return this.m;
   }

   public record WalkCeiling(String a, List<String> b, int c, io.karatelabs.ext.rules.twin.TwinWalkResult.WalkUsageStats d) {

      public WalkCeiling(String a, List<String> b, int c, io.karatelabs.ext.rules.twin.TwinWalkResult.WalkUsageStats d) {
         this.a = a;
         this.b = b;
         this.c = c;
         this.d = d;
      }
   }

   public record Counterexample(String a, String b, String c, String d, Map<String, Object> e, int f, List<Map<String, Object>> g, String h, String i) {

      public Counterexample(String a, String b, String c, String d, Map<String, Object> e, int f, List<Map<String, Object>> g, String h, String i) {
         this.a = a;
         this.b = b;
         this.c = c;
         this.d = d;
         this.e = e;
         this.f = f;
         this.g = g;
         this.h = h;
         this.i = i;
      }
   }

   public record WalkTransition(int a, String b, Map<String, Object> c, Integer d, String e, boolean f, String g) {

      public WalkTransition(int a, String b, Map<String, Object> c, Integer d, String e, boolean f, String g) {
         this.a = a;
         this.b = b;
         this.c = c;
         this.d = d;
         this.e = e;
         this.f = f;
         this.g = g;
      }
   }

   public record WalkError(int a, String b, Map<String, Object> c, String d, String e, String f) {

      public WalkError(int a, String b, Map<String, Object> c, String d, String e, String f) {
         this.a = a;
         this.b = b;
         this.c = c;
         this.d = d;
         this.e = e;
         this.f = f;
      }
   }

   public record WalkState(int a, String b, Object c, int d, List<String> e, List<io.karatelabs.ext.rules.twin.CommandExecutor.InvariantEvaluation> f) {

      public WalkState(int a, String b, Object c, int d, List<String> e, List<io.karatelabs.ext.rules.twin.CommandExecutor.InvariantEvaluation> f) {
         this.a = a;
         this.b = b;
         this.c = c;
         this.d = d;
         this.e = e;
         this.f = f;
      }
   }

   public record WalkPruningStats(int a, int b, int c, int d, int e) {

      public WalkPruningStats(int a, int b, int c, int d, int e) {
         this.a = a;
         this.b = b;
         this.c = c;
         this.d = d;
         this.e = e;
      }
   }

   public record WalkRefusal(int a, String b, Map<String, Object> c, String d, String e, Map<String, Object> f) {

      public WalkRefusal(int a, String b, Map<String, Object> c, String d, String e, Map<String, Object> f) {
         this.a = a;
         this.b = b;
         this.c = c;
         this.d = d;
         this.e = e;
         this.f = f;
      }
   }

   public record WalkUsageStats(int a, int b, int c, double d) {

      public WalkUsageStats(int a, int b, int c, double d) {
         this.a = a;
         this.b = b;
         this.c = c;
         this.d = d;
      }
   }
}
