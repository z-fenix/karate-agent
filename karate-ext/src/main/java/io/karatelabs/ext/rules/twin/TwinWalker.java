package io.karatelabs.ext.rules.twin;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.Map.Entry;
import java.util.function.Function;

public final class TwinWalker {
   private static final String a = "exhausted";

   private TwinWalker() {
   }

   public static TwinWalkResult a(TwinModel var0, TwinExecutionLimits var1) {
      return a(var0, io.karatelabs.ext.rules.twin.TwinWalkPlan.a(null, var0, var1, Set.of()), var1);
   }

   public static TwinWalkResult a(TwinModel var0, TwinWalkPlan var1, TwinExecutionLimits var2) {
      return a(var0, var1, var2, null);
   }

   public static TwinWalkResult a(TwinModel var0, TwinWalkPlan var1, TwinExecutionLimits var2, RulebookRuntime var3) {
      return b(var0, var1, var2, var3).a();
   }

   public static io.karatelabs.ext.rules.twin.TwinWalker.WalkExecution b(TwinModel var0, TwinWalkPlan var1, TwinExecutionLimits var2, RulebookRuntime var3) {
      return new io.karatelabs.ext.rules.twin.TwinWalker.TwinWalkerState(var0, var1, var2, var3).a();
   }

   static List<Map<String, Object>> a(io.karatelabs.ext.rules.twin.TwinModel.CommandModel var0, TwinWalkPlan var1) {
      if (var0.g() == null) {
         return var0.f();
      } else {
         LinkedHashMap var2 = new LinkedHashMap();

         for (Entry var4 : var1.d().entrySet()) {
            int var5 = ((String)var4.getKey()).indexOf(46);
            if (var5 > 0 && ((String)var4.getKey()).substring(0, var5).equals(var0.a())) {
               var2.put(((String)var4.getKey()).substring(var5 + 1), (List)var4.getValue());
            }
         }

         return var2.isEmpty() ? var0.f() : TwinModel.a(var0.g(), var2);
      }
   }

   static Map<String, Object> a(String var0, Map<String, Object> var1) {
      LinkedHashMap var2 = new LinkedHashMap();
      var2.put("command", var0);
      var2.put("args", var1);
      return var2;
   }

   public static ReplayTrace a(TwinModel var0, List<Map<String, Object>> var1) {
      return a(var0, var1, (RulebookRuntime)null);
   }

   public static ReplayTrace a(TwinModel var0, List<Map<String, Object>> var1, RulebookRuntime var2) {
      var var3 = b(var0, var1);
      Object var4 = a(var0);
      ArrayList var5 = new ArrayList(var3.size());

      for (int var6 = 0; var6 < var3.size(); var6++) {
         io.karatelabs.ext.rules.twin.TwinModel.CommandModel var7 = var0.f().get(((Map)var1.get(var6)).get("command"));
         Map var8 = (Map)var3.get(var6);
         if (!a(var0, var4, var8)) {
            var5.add(new io.karatelabs.ext.rules.twin.ReplayTrace.ReplayStep(var7.a(), var8, "refused", "clock", b(var0, var4, var8), null, "notEvaluated", CommandExecutor.a(var0, var4), var4));
         } else {
            io.karatelabs.ext.rules.twin.CommandExecutor.CommandResult var9 = CommandExecutor.a(var0, var4, var7, var8, var2);
            var var10 = a(var9);
            if (var9.a()) {
               var4 = io.karatelabs.ext.rules.twin.CanonicalJson.d(var9.j());
            }

            var5.add(
               new io.karatelabs.ext.rules.twin.ReplayTrace.ReplayStep(
                  var7.a(),
                  var8,
                  var9.b(),
                  var9.c(),
                  "error".equals(var9.b()) ? var9.e() : var9.d(),
                  var10,
                  var9.a() ? var9.n() : "notEvaluated",
                  var9.a() ? var9.p() : CommandExecutor.a(var0, var4),
                  var4
               )
            );
         }
      }

      return new ReplayTrace(List.copyOf(var5), var4);
   }

   private static Map<String, Object> a(io.karatelabs.ext.rules.twin.CommandExecutor.CommandResult var0) {
      if (!"snapshot".equals(var0.c())) {
         return null;
      } else {
         LinkedHashMap var1 = new LinkedHashMap();
         var1.put("kind", var0.g());
         var1.put("path", var0.h());
         if (var0.i() != null) {
            var1.put("rule", var0.i());
         }

         return var1;
      }
   }

   static Object a(TwinModel var0) {
      LinkedHashMap var1 = new LinkedHashMap();
      var0.a().call(null, var1);

      try {
         io.karatelabs.ext.rules.twin.CanonicalJson.b(var1);
      } catch (SnapshotException var3) {
         throw TwinException.a(null, "init", var3.getMessage());
      }

      return io.karatelabs.ext.rules.twin.CanonicalJson.d(var1);
   }

   static boolean a(TwinModel var0, Object var1, Map<String, Object> var2) {
      String var3 = var0.e();
      if (var3 != null && var2.containsKey(var3)) {
         Object var4 = ((Map)var1).get(var3);
         if (var4 != null && !io.karatelabs.ext.rules.twin.CanonicalJson.a(var4)) {
            Object var5 = var2.get(var3);
            if (var5 instanceof String var6 && var4 instanceof String var9) {
               return io.karatelabs.ext.rules.twin.CanonicalJson.a(var6, var9) >= 0;
            } else {
               return var5 instanceof Number var8 && var4 instanceof Number var7 ? Double.compare(var8.doubleValue(), var7.doubleValue()) >= 0 : false;
            }
         } else {
            return true;
         }
      } else {
         return true;
      }
   }

   static String b(TwinModel var0, Object var1, Map<String, Object> var2) {
      String var3 = var0.e();
      Object var4 = ((Map)var1).get(var3);
      Object var5 = var2.get(var3);
      boolean var6 = var5 instanceof String && var4 instanceof String || var5 instanceof Number && var4 instanceof Number;
      return var3 + " " + io.karatelabs.ext.rules.twin.CanonicalJson.c(var5) + (var6 ? " does not advance the world's " : " cannot be ordered against ") + io.karatelabs.ext.rules.twin.CanonicalJson.c(var4);
   }

   private static List<Map<String, Object>> b(TwinModel var0, List<Map<String, Object>> var1) {
      if (var1 == null) {
         throw TwinException.a("steps: must be an array", (Map<String, Object>)null);
      } else {
         ArrayList var2 = new ArrayList(var1.size());

         for (int var3 = 0; var3 < var1.size(); var3++) {
            Map var4 = (Map)var1.get(var3);
            Object var5 = var4 == null ? null : var4.get("command");
            if (!(var5 instanceof String var6) || !var0.f().containsKey(var6)) {
               throw TwinException.a("steps[" + var3 + "].command: not a declared command", a(var3, var5));
            }

            Object var7 = var4.get("args");
            if (var7 != null && !io.karatelabs.ext.rules.twin.CanonicalJson.a(var7)) {
               if (!(var7 instanceof Map)) {
                  throw TwinException.a("steps[" + var3 + "].args: must be an object", a(var3, var6));
               }

               try {
                  io.karatelabs.ext.rules.twin.CanonicalJson.b(var7);
               } catch (SnapshotException var10) {
                  LinkedHashMap var9 = new LinkedHashMap();
                  var9.put("at", var3);
                  var9.put("kind", var10.a);
                  var9.put("path", var10.b);
                  throw TwinException.a("steps[" + var3 + "].args: " + var10.getMessage(), var9);
               }

               var2.add(a(io.karatelabs.ext.rules.twin.CanonicalJson.d(var7)));
            } else {
               var2.add(Map.of());
            }
         }

         return var2;
      }
   }

   private static Map<String, Object> a(int var0, Object var1) {
      LinkedHashMap var2 = new LinkedHashMap();
      var2.put("at", var0);
      var2.put("command", var1);
      return var2;
   }

   private static Map<String, Object> a(Object var0) {
      return (Map<String, Object>)var0;
   }

   private record ParentPointer(int a, String b, Map<String, Object> c) {

      private ParentPointer(int a, String b, Map<String, Object> c) {
         this.a = a;
         this.b = b;
         this.c = c;
      }
   }

   public static final class ReverseTransitionPath {
      private final List<io.karatelabs.ext.rules.twin.TwinWalker.ParentPointer> a;

      private ReverseTransitionPath(List<io.karatelabs.ext.rules.twin.TwinWalker.ParentPointer> var1) {
         this.a = var1;
      }

      public List<Map<String, Object>> a(int var1) {
         ArrayList var2 = new ArrayList();

         for (io.karatelabs.ext.rules.twin.TwinWalker.ParentPointer var3 = this.a.get(var1); var3 != null; var3 = this.a.get(var3.a())) {
            var2.add(TwinWalker.a(var3.b(), var3.c()));
         }

         ArrayList var5 = new ArrayList(var2.size());

         for (int var4 = var2.size() - 1; var4 >= 0; var4--) {
            var5.add((Map)var2.get(var4));
         }

         return var5;
      }
   }

   private static final class TwinWalkerState {
      private final TwinModel a;
      private final TwinWalkPlan b;
      private final TwinExecutionLimits c;
      private final RulebookRuntime d;
      private final long e = System.nanoTime();
      private final Map<String, List<Map<String, Object>>> f = new LinkedHashMap<>();
      private final Map<String, io.karatelabs.ext.rules.twin.TwinWalkResult.WalkState> g = new LinkedHashMap<>();
      private final List<io.karatelabs.ext.rules.twin.TwinWalkResult.WalkState> h = new ArrayList<>();
      private final List<io.karatelabs.ext.rules.twin.TwinWalkResult.WalkState> i = new ArrayList<>();
      private final List<io.karatelabs.ext.rules.twin.TwinWalker.ParentPointer> j = new ArrayList<>();
      private final List<List<String>> k = new ArrayList<>();
      private final List<io.karatelabs.ext.rules.twin.TwinWalkResult.WalkTransition> l = new ArrayList<>();
      private final List<io.karatelabs.ext.rules.twin.TwinWalkResult.WalkRefusal> m = new ArrayList<>();
      private final List<io.karatelabs.ext.rules.twin.TwinWalkResult.WalkError> n = new ArrayList<>();
      private final List<io.karatelabs.ext.rules.twin.TwinWalkResult.Counterexample> o = new ArrayList<>();
      private final Map<String, int[]> p = new LinkedHashMap<>();
      private final Map<String, int[]> q = new LinkedHashMap<>();
      private final Map<String, Integer> r = new LinkedHashMap<>();
      private final Map<String, Integer> s = new LinkedHashMap<>();
      private int t;
      private int u;
      private int v;
      private int w;
      private int x;
      private int y;
      private int z;
      private int A;

      TwinWalkerState(TwinModel var1, TwinWalkPlan var2, TwinExecutionLimits var3, RulebookRuntime var4) {
         this.a = var1;
         this.b = var2;
         this.c = var3;
         this.d = var4;

         for (io.karatelabs.ext.rules.twin.TwinModel.CommandModel var6 : var1.c()) {
            this.p.put(var6.a(), new int[3]);
            this.r.put(var6.a(), 0);
            this.s.put(var6.a(), 0);
            this.f.put(var6.a(), TwinWalker.a(var6, var2));
         }

         for (io.karatelabs.ext.rules.twin.TwinModel.StateModel var8 : var1.b()) {
            this.q.put(var8.a(), new int[2]);
         }
      }

      io.karatelabs.ext.rules.twin.TwinWalker.WalkExecution a() {
         Object var1 = TwinWalker.a(this.a);
         this.a(var1, io.karatelabs.ext.rules.twin.CanonicalJson.c(var1), 0, CommandExecutor.a(this.a, var1), null);
         String var2 = null;
         List<String> var3 = List.of();

         while (this.t < this.i.size()) {
            var var4 = this.b();
            if (!var4.isEmpty()) {
               var2 = (String)var4.get(0);
               var3 = List.copyOf(var4.subList(1, var4.size()));
               break;
            }

            this.a(this.i.get(this.t++));
         }

         return this.a(var2 == null ? "exhausted" : var2, var3);
      }

      private List<String> b() {
         ArrayList var1 = new ArrayList(3);
         if (this.d() >= this.c.d()) {
            var1.add("maxWallTimeSeconds");
         }

         if (this.u >= this.c.c()) {
            var1.add("maxRuns");
         }

         if (this.h.size() >= this.c.b()) {
            var1.add("maxNodes");
         }

         return var1;
      }

      private void a(io.karatelabs.ext.rules.twin.TwinWalkResult.WalkState var1) {
         for (io.karatelabs.ext.rules.twin.TwinModel.CommandModel var3 : this.a.c()) {
            for (Map var5 : this.f.get(var3.a())) {
               if (!TwinWalker.a(this.a, var1.c(), var5)) {
                  this.A++;
               } else {
                  this.u++;
                  this.a(var1, var3, var5);
               }
            }
         }
      }

      private void a(io.karatelabs.ext.rules.twin.TwinWalkResult.WalkState var1, io.karatelabs.ext.rules.twin.TwinModel.CommandModel var2, Map<String, Object> var3) {
         io.karatelabs.ext.rules.twin.CommandExecutor.CommandResult var4 = CommandExecutor.a(this.a, var1.c(), var2, var3, this.d);
         int[] var5 = this.p.get(var2.a());
         if ("error".equals(var4.b()) && "guard".equals(var4.c())) {
            var5[2]++;
         } else if ("refused".equals(var4.b()) && "guard".equals(var4.c())) {
            var5[1]++;
         } else {
            var5[0]++;
         }

         if ("error".equals(var4.b())) {
            this.n.add(new io.karatelabs.ext.rules.twin.TwinWalkResult.WalkError(var1.a(), var2.a(), var3, var4.c(), var4.e(), var4.f()));
            this.a("error", null, var2, var3, var1.a(), var1.a(), var4.c(), var4.e());
         } else if ("refused".equals(var4.b())) {
            this.m.add(new io.karatelabs.ext.rules.twin.TwinWalkResult.WalkRefusal(var1.a(), var2.a(), var3, var4.c(), var4.d(), TwinWalker.a(var4)));
            if ("guard".equals(var4.c())) {
               this.x++;
            }
         } else {
            this.r.merge(var2.a(), 1, Integer::sum);
            if (var4.m()) {
               this.s.merge(var2.a(), 1, Integer::sum);
            }

            io.karatelabs.ext.rules.twin.TwinWalkResult.WalkState var7 = this.g.get(var4.k());
            Integer var6;
            if (var4.l()) {
               var6 = var1.a();
               this.z++;
            } else if (var7 != null) {
               var6 = var7.a();
               this.y++;
            } else if (var1.d() + 1 > this.a(var1, var2)) {
               var6 = null;
               this.w++;
            } else {
               var6 = this.a(io.karatelabs.ext.rules.twin.CanonicalJson.d(var4.j()), var4.k(), var1.d() + 1, var4.p(), new io.karatelabs.ext.rules.twin.TwinWalker.ParentPointer(var1.a(), var2.a(), var3)).a();
            }

            this.l.add(new io.karatelabs.ext.rules.twin.TwinWalkResult.WalkTransition(var1.a(), var2.a(), var3, var6, var4.k(), var4.l(), var4.n()));
            int var8 = var6 == null ? var1.a() : var6;
            if (var7 == null && !var4.l()) {
               for (io.karatelabs.ext.rules.twin.CommandExecutor.InvariantEvaluation var10 : var4.p()) {
                  if ("failed".equals(var10.b())) {
                     this.a("invariant", var10.a(), var2, var3, var8, var1.a());
                  }
               }
            }

            if ("failed".equals(var4.n()) || "error".equals(var4.n())) {
               this.a("postcondition", var2.a(), var2, var3, var8, var1.a());
            }
         }
      }

      private int a(io.karatelabs.ext.rules.twin.TwinWalkResult.WalkState var1, io.karatelabs.ext.rules.twin.TwinModel.CommandModel var2) {
         int var3 = this.b.c().get("default");
         var var4 = this.k.get(var1.a());

         for (int var5 = 0; var5 < var4.size(); var5++) {
            Integer var6 = this.b.c().get((String)var4.get(var5) + "→" + (var5 + 1 < var4.size() ? (String)var4.get(var5 + 1) : var2.a()));
            if (var6 != null) {
               var3 = Math.max(var3, var6);
            }
         }

         return var3;
      }

      private io.karatelabs.ext.rules.twin.TwinWalkResult.WalkState a(Object var1, String var2, int var3, List<io.karatelabs.ext.rules.twin.CommandExecutor.InvariantEvaluation> var4, io.karatelabs.ext.rules.twin.TwinWalker.ParentPointer var5) {
         ArrayList var6 = new ArrayList();

         for (io.karatelabs.ext.rules.twin.TwinModel.StateModel var8 : this.a.b()) {
            String var9 = CommandExecutor.a(var8.b(), var1);
            int[] var10 = this.q.get(var8.a());
            if ("held".equals(var9)) {
               var10[0]++;
               var6.add(var8.a());
            } else if ("error".equals(var9)) {
               var10[1]++;
            }
         }

         io.karatelabs.ext.rules.twin.TwinWalkResult.WalkState var11 = new io.karatelabs.ext.rules.twin.TwinWalkResult.WalkState(this.h.size(), var2, var1, var3, var6, var4);
         this.g.put(var2, var11);
         this.h.add(var11);
         this.i.add(var11);
         this.j.add(var5);
         this.k.add(var5 == null ? List.of() : a(this.k.get(var5.a()), var5.b()));
         this.v = Math.max(this.v, var3);
         return var11;
      }

      private void a(String var1, String var2, io.karatelabs.ext.rules.twin.TwinModel.CommandModel var3, Map<String, Object> var4, int var5, int var6) {
         this.a(var1, var2, var3, var4, var5, var6, null, null);
      }

      private void a(String var1, String var2, io.karatelabs.ext.rules.twin.TwinModel.CommandModel var3, Map<String, Object> var4, int var5, int var6, String var7, String var8) {
         var var9 = this.a(var6);
         var9.add(TwinWalker.a(var3.a(), var4));
         LinkedHashMap var10 = new LinkedHashMap();
         var10.put("kind", var1);
         if (var2 != null) {
            var10.put("name", var2);
         }

         var10.put("command", var3.a());
         var10.put("args", var4);
         var10.put("path", var9);
         this.o.add(new io.karatelabs.ext.rules.twin.TwinWalkResult.Counterexample(io.karatelabs.ext.rules.twin.CanonicalJson.a(io.karatelabs.ext.rules.twin.CanonicalJson.c(var10)), var1, var2, var3.a(), var4, var5, var9, var7, var8));
      }

      private static List<String> a(List<String> var0, String var1) {
         ArrayList var2 = new ArrayList(var0.size() + 1);
         var2.addAll(var0);
         var2.add(var1);
         return var2;
      }

      private List<Map<String, Object>> a(int var1) {
         return this.c().a(var1);
      }

      private io.karatelabs.ext.rules.twin.TwinWalker.ReverseTransitionPath c() {
         return new io.karatelabs.ext.rules.twin.TwinWalker.ReverseTransitionPath(new ArrayList<>(this.j));
      }

      private double d() {
         return (System.nanoTime() - this.e) / 1.0E9;
      }

      private io.karatelabs.ext.rules.twin.TwinWalker.WalkExecution a(String var1, List<String> var2) {
         io.karatelabs.ext.rules.twin.TwinWalkResult.WalkPruningStats var3 = new io.karatelabs.ext.rules.twin.TwinWalkResult.WalkPruningStats(this.w, this.x, this.y, this.z, this.A);
         io.karatelabs.ext.rules.twin.TwinWalkResult.WalkCeiling var4 = new io.karatelabs.ext.rules.twin.TwinWalkResult.WalkCeiling(var1, var2, this.i.size() - this.t, new io.karatelabs.ext.rules.twin.TwinWalkResult.WalkUsageStats(this.v, this.h.size(), this.u, this.d()));
         var var5 = this.e();
         return new io.karatelabs.ext.rules.twin.TwinWalker.WalkExecution(
            new TwinWalkResult(
               List.copyOf(this.h),
               List.copyOf(this.l),
               List.copyOf(this.m),
               List.copyOf(this.n),
               var3,
               var4,
               this.a(var5),
               this.a(var3, var1, var5),
               List.copyOf(this.o)
            ),
            this.c()
         );
      }

      private Map<String, Object> e() {
         LinkedHashMap var1 = new LinkedHashMap();

         for (io.karatelabs.ext.rules.twin.TwinModel.InvariantModel var3 : this.a.d()) {
            int var4 = 0;
            int var5 = 0;
            ArrayList var6 = new ArrayList();

            for (io.karatelabs.ext.rules.twin.TwinWalkResult.WalkState var8 : this.h) {
               for (io.karatelabs.ext.rules.twin.CommandExecutor.InvariantEvaluation var10 : var8.f()) {
                  if (var10.a().equals(var3.a())) {
                     var4++;
                     if ("failed".equals(var10.b())) {
                        var5++;
                        var6.add(var8.a());
                     }
                  }
               }
            }

            LinkedHashMap var11 = new LinkedHashMap();
            var11.put("ran", var4);
            var11.put("failed", var5);
            var11.put("failedAt", var6);
            var1.put(var3.a(), var11);
         }

         return var1;
      }

      private Map<String, Object> a(Map<String, Object> var1) {
         LinkedHashMap var2 = new LinkedHashMap();

         for (Entry var4 : this.p.entrySet()) {
            LinkedHashMap var5 = new LinkedHashMap();
            var5.put("observedTrue", ((int[])var4.getValue())[0]);
            var5.put("observedFalse", ((int[])var4.getValue())[1]);
            var5.put("notChecked", ((int[])var4.getValue())[2]);
            var2.put((String)var4.getKey(), var5);
         }

         LinkedHashMap var8 = new LinkedHashMap();

         for (Entry var11 : this.r.entrySet()) {
            int var6 = this.s.getOrDefault(var11.getKey(), 0);
            LinkedHashMap var7 = new LinkedHashMap();
            var7.put("oracle", var6);
            var7.put("bare", (Integer)var11.getValue() - var6);
            var8.put((String)var11.getKey(), var7);
         }

         LinkedHashMap var10 = new LinkedHashMap();

         for (Entry var14 : this.q.entrySet()) {
            LinkedHashMap var15 = new LinkedHashMap();
            var15.put("observed", ((int[])var14.getValue())[0]);
            var15.put("notChecked", ((int[])var14.getValue())[1]);
            var10.put((String)var14.getKey(), var15);
         }

         LinkedHashMap var13 = new LinkedHashMap();
         var13.put("guards", var2);
         var13.put("invariants", var1);
         var13.put("commands", var8);
         var13.put("states", var10);
         return var13;
      }

      private Map<String, Object> a(io.karatelabs.ext.rules.twin.TwinWalkResult.WalkPruningStats var1, String var2, Map<String, Object> var3) {
         LinkedHashMap var4 = new LinkedHashMap();
         var4.put("guard", b(this.n, "guard"));
         var4.put("apply", b(this.n, "apply"));
         LinkedHashMap var5 = new LinkedHashMap();
         var5.put("count", this.n.size());
         var5.put("byStage", var4);
         TreeMap<String, Integer> var6 = new TreeMap<>();
         int var7 = 0;

         for (io.karatelabs.ext.rules.twin.TwinWalkResult.WalkRefusal var9 : this.m) {
            if (var9.f() != null) {
               var7++;
               var6.merge(String.valueOf(var9.f().get("kind")), 1, (var0, var1x) -> var0 + var1x);
            }
         }

         LinkedHashMap var21 = new LinkedHashMap();
         var21.put("count", var7);
         var21.put("byKind", new LinkedHashMap(var6));
         LinkedHashMap var22 = new LinkedHashMap();
         var22.put("kind", "content");
         var22.put("fields", this.g());
         ArrayList var10 = new ArrayList();
         ArrayList var11 = new ArrayList();

         for (io.karatelabs.ext.rules.twin.TwinModel.StateModel var13 : this.a.b()) {
            (((int[])this.q.get(var13.a()))[0] > 0 ? var10 : var11).add(var13.a());
         }

         LinkedHashMap var23 = new LinkedHashMap();
         var23.put("declared", a(this.a.b(), io.karatelabs.ext.rules.twin.TwinModel.StateModel::a));
         var23.put("reached", var10);
         var23.put("unreached", var11);
         LinkedHashMap var24 = new LinkedHashMap();
         var24.put("edges", this.l.size());
         var24.put("noop", var1.d());
         var24.put("merged", var1.c());
         var24.put("depthPruned", var1.a());
         ArrayList var14 = new ArrayList();

         for (Entry var16 : this.p.entrySet()) {
            if (((int[])var16.getValue())[2] > 0) {
               var14.add((String)var16.getKey());
            }
         }

         LinkedHashMap var25 = new LinkedHashMap();
         var25.put("declared", a(this.a.c(), io.karatelabs.ext.rules.twin.TwinModel.CommandModel::a));
         var25.put("notChecked", var14);
         ArrayList var26 = new ArrayList();

         for (io.karatelabs.ext.rules.twin.TwinModel.CommandModel var18 : this.a.c()) {
            if (var18.c()) {
               var26.add(var18.a());
            }
         }

         ArrayList var27 = new ArrayList();

         for (Entry var19 : var3.entrySet()) {
            if (!Integer.valueOf(0).equals(((Map)var19.getValue()).get("failed"))) {
               var27.add((String)var19.getKey());
            }
         }

         LinkedHashMap var29 = new LinkedHashMap();
         var29.put("declared", a(this.a.d(), io.karatelabs.ext.rules.twin.TwinModel.InvariantModel::a));
         var29.put("failed", var27);
         LinkedHashMap var30 = new LinkedHashMap();
         var30.put("declared", this.a.e());
         LinkedHashMap var20 = new LinkedHashMap();
         var20.put("halting", var2);
         var20.put("notExpanded", a(var1));
         var20.put("errors", var5);
         var20.put("identity", var22);
         var20.put("snapshotViolations", var21);
         var20.put("states", var23);
         var20.put("transitions", var24);
         var20.put("guards", var25);
         var20.put("scopeGuards", var26);
         var20.put("invariants", var29);
         var20.put("clock", var30);
         var20.put("plan", this.b.b());
         var20.put("undeclaredOpenFields", this.f());
         return var20;
      }

      private List<Map<String, Object>> f() {
         String var1 = this.a.e();
         LinkedHashSet var2 = new LinkedHashSet();

         for (io.karatelabs.ext.rules.twin.TwinWalkResult.WalkState var4 : this.h) {
            for (Entry var6 : (Iterable<Entry>) (Iterable<?>) (((Map)var4.c()).entrySet())) {
               if (var6.getValue() instanceof Number && !var6.getKey().equals(var1)) {
                  var2.add(String.valueOf(var6.getKey()));
               }
            }
         }

         ArrayList<String> var15 = new ArrayList(var2);
         var15.sort(io.karatelabs.ext.rules.twin.CanonicalJson::a);
         ArrayList var16 = new ArrayList();

         for (String var18 : (Iterable<String>) (Iterable<?>) (var15)) {
            HashSet var7 = new HashSet();
            LinkedHashSet var8 = new LinkedHashSet();
            boolean var9 = false;

            for (io.karatelabs.ext.rules.twin.TwinWalkResult.WalkState var11 : this.h) {
               LinkedHashMap var12 = new LinkedHashMap((Map)var11.c());
               boolean var13 = var12.containsKey(var18);
               Object var14 = var12.remove(var18);
               if (var13) {
                  var8.add(io.karatelabs.ext.rules.twin.CanonicalJson.c(var14));
               }

               var9 |= !var7.add(io.karatelabs.ext.rules.twin.CanonicalJson.c(var11.e()) + "\u0000" + io.karatelabs.ext.rules.twin.CanonicalJson.c(var12));
            }

            if (var9) {
               LinkedHashMap var19 = new LinkedHashMap();
               var19.put("field", var18);
               var19.put("distinct", var8.size());
               var16.add(var19);
            }
         }

         return var16;
      }

      private static Map<String, Object> a(io.karatelabs.ext.rules.twin.TwinWalkResult.WalkPruningStats var0) {
         LinkedHashMap var1 = new LinkedHashMap();
         var1.put("depthPruned", var0.a());
         var1.put("refusedByGuard", var0.b());
         var1.put("mergedAsVisited", var0.c());
         var1.put("noop", var0.d());
         var1.put("inadmissible", var0.e());
         return var1;
      }

      private List<String> g() {
         LinkedHashSet var1 = new LinkedHashSet();

         for (io.karatelabs.ext.rules.twin.TwinWalkResult.WalkState var3 : this.h) {
            for (Object var5 : ((Map)var3.c()).keySet()) {
               var1.add(String.valueOf(var5));
            }
         }

         ArrayList<String> var6 = new ArrayList(var1);
         var6.sort(io.karatelabs.ext.rules.twin.CanonicalJson::a);
         return var6;
      }

      private static int b(List<io.karatelabs.ext.rules.twin.TwinWalkResult.WalkError> var0, String var1) {
         int var2 = 0;

         for (io.karatelabs.ext.rules.twin.TwinWalkResult.WalkError var4 : var0) {
            if (var1.equals(var4.d())) {
               var2++;
            }
         }

         return var2;
      }

      private static <T> List<String> a(List<T> var0, Function<T, String> var1) {
         ArrayList var2 = new ArrayList(var0.size());

         for (T var4 : var0) {
            var2.add(var1.apply(var4));
         }

         return var2;
      }
   }

   public record WalkExecution(TwinWalkResult a, io.karatelabs.ext.rules.twin.TwinWalker.ReverseTransitionPath b) {

      public WalkExecution(TwinWalkResult a, io.karatelabs.ext.rules.twin.TwinWalker.ReverseTransitionPath b) {
         this.a = a;
         this.b = b;
      }
   }
}
