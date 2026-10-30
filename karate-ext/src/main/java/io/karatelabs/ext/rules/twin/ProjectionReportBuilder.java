package io.karatelabs.ext.rules.twin;

import io.karatelabs.ext.rules.WitnessBaselineStore;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Map.Entry;

final class ProjectionReportBuilder {
   static final String a = "model";
   static final String b = "An edge is two consecutive applied steps in one walk witness path or pinned sequence, labelled with its ids. It proves that adjacency alone, never a longer path through the picture.";
   static final String c = "not attempted under this plan";
   private final String d;
   private final TwinModel e;
   private final TwinWalkResult f;
   private final Set<String> g;
   private final Map<String, io.karatelabs.ext.rules.twin.TwinModel.CommandModel> h = new LinkedHashMap<>();
   private final Map<Integer, List<String>> i = new LinkedHashMap<>();
   private static final Comparator<String> j = Comparator.<String, Character>comparing(var0 -> var0.charAt(0))
      .thenComparingInt(var0 -> Integer.parseInt(var0.substring(1)));

   private ProjectionReportBuilder(String var1, TwinModel var2, TwinWalkResult var3, Collection<String> var4) {
      this.d = var1;
      this.e = var2;
      this.f = var3;
      this.g = TwinCoverageAnalyzer.a(var2);

      for (io.karatelabs.ext.rules.twin.TwinModel.CommandModel var6 : var2.c()) {
         if (var6.q() != null && var4.contains(var6.q().a())) {
            this.h.put(var6.a(), var6);
         }
      }

      for (io.karatelabs.ext.rules.twin.TwinWalkResult.WalkState var8 : var3.e()) {
         this.i.put(var8.a(), var8.e().isEmpty() ? List.of("(unnamed)") : var8.e());
      }
   }

   static Map<String, Object> a(String var0, String var1, String var2) {
      LinkedHashMap<java.lang.String, java.lang.String> var3 = new LinkedHashMap();
      var3.put("code", var1);
      var3.put("reason", var2);
      LinkedHashMap<java.lang.String, java.lang.Object> var4 = new LinkedHashMap();
      var4.put("rule", var0);
      var4.put("unavailable", var3);
      return var4;
   }

   static Map<String, Object> a(
      String var0,
      TwinModel var1,
      io.karatelabs.ext.rules.twin.TwinWalker.WalkExecution var2,
      TwinWalkPlan var3,
      List<Map<String, Object>> var4,
      List<Map<String, Object>> var5,
      RulebookRuntime var6,
      Path var7,
      String var8,
      List<String> var9,
      List<String> var10
   ) {
      ProjectionReportBuilder var11 = new ProjectionReportBuilder(var0, var1, var2.a(), var9);
      LinkedHashMap var12 = new LinkedHashMap();
      Map var13 = var11.a(var7, var12);
      io.karatelabs.ext.rules.twin.ProjectionReportBuilder.OperationFlowTracker var14 = new io.karatelabs.ext.rules.twin.ProjectionReportBuilder.OperationFlowTracker();
      TreeMap<String, Set<String>> var15 = new TreeMap<>(io.karatelabs.ext.rules.twin.CanonicalJson::a);
      ArrayList<java.lang.String> var16 = new ArrayList();

      for (Map var18 : var5) {
         String var19 = String.valueOf(var18.get("_id"));

         ReplayTrace var20;
         try {
            var20 = TwinWalker.a(var1, a(var18.get("steps")), var6);
         } catch (TwinException var24) {
            continue;
         }

         var16.add(var19);
         var11.a(var20, "sequence", var19, var14);
         Object var21 = TwinWalker.a(var1);

         for (io.karatelabs.ext.rules.twin.ReplayTrace.ReplayStep var23 : var20.a()) {
            if ("applied".equals(var23.c())) {
               var15.computeIfAbsent(var11.a(var21, var23.a(), var23.b(), var23.i()), var0x -> new TreeSet<String>(io.karatelabs.ext.rules.twin.CanonicalJson::a)).add(var19);
            }

            var21 = var23.i();
         }
      }

      io.karatelabs.ext.rules.twin.TwinCoverageAnalyzer.CoverageReport var25 = TwinCoverageAnalyzer.a(var1, var2.a(), var2.b(), var4, new LinkedHashSet<>(TwinCoverageAnalyzer.e));
      LinkedHashMap<java.lang.String, java.lang.Integer> var26 = new LinkedHashMap();

      for (io.karatelabs.ext.rules.twin.TwinCoverageAnalyzer.CoverageObligation var29 : var25.a()) {
         int var30 = var26.merge(var29.a(), 1, Integer::sum) - 1;
         var11.a(TwinWalker.a(var1, var29.d(), var6), "walk", "" + var29.a().charAt(0) + var30, var14);
      }

      var26.put("sequence", var16.size());
      LinkedHashMap<java.lang.String, java.lang.Object> var28 = new LinkedHashMap();
      var28.put("rule", var0);
      var28.put("disclosure", var11.a(var3, var8, var12));
      var28.put("worlds", var11.a());
      var28.put("operations", var11.a(var13, var15));
      var28.put("states", var11.b());
      var28.put("flow", var11.a(var14, var26));
      var28.put("modelOnly", var11.d());
      var28.put("unmodelled", var11.a(var9, var10));
      return var28;
   }

   private Map<String, Object> a(TwinWalkPlan var1, String var2, Map<String, Object> var3) {
      LinkedHashMap<java.lang.String, java.lang.Integer> var4 = new LinkedHashMap();
      var4.put("maxDepth", io.karatelabs.ext.rules.twin.TwinExecutionLimits.a.a());
      var4.put("maxNodes", io.karatelabs.ext.rules.twin.TwinExecutionLimits.a.b());
      var4.put("maxRuns", io.karatelabs.ext.rules.twin.TwinExecutionLimits.a.c());
      var4.put("maxWallTimeSeconds", io.karatelabs.ext.rules.twin.TwinExecutionLimits.a.d());
      Map<java.lang.String, java.util.LinkedHashMap> var5 = (Map)this.f.b().get("ceilings");
      var5.put("limits", var4);
      LinkedHashMap<java.lang.String, java.lang.Object> var6 = new LinkedHashMap();
      var6.put("plan", var1.a());
      var6.put("planSha", var1.f());
      var6.put("walk", this.f.c());
      var6.put("ceilings", var5);
      var6.put("grade", "model");
      var6.put("projectionDrift", this.a(var2));
      var6.put("witness", var3);
      var6.put(
         "line",
         "model grade · plan "
            + var1.f().substring(0, 12)
            + " · walk "
            + this.f.c().substring(0, 12)
            + " · ceilings "
            + var5.get("hit")
            + " · witness store "
            + var3.get("store")
      );
      var6.put("boundary", "A walk is evidence about the declaration, bounded by its ceilings; it never proves the system.");
      return var6;
   }

   private Map<String, Object> a(String var1) {
      LinkedHashMap<java.lang.String, java.lang.Object> var2 = new LinkedHashMap();

      io.karatelabs.ext.rules.twin.OpenApiProjector.OpenApiProjection var3;
      try {
         var3 = io.karatelabs.ext.rules.twin.OpenApiProjector.a(this.e, this.d);
      } catch (RuntimeException var5) {
         var2.put("drift", true);
         var2.put("reason", "the projection could not be computed: " + io.karatelabs.ext.rules.twin.ThrowableUtils.a(var5));
         return var2;
      }

      if (!var3.a()) {
         var2.put("drift", true);
         var2.put("reason", "the projection refuses, so nothing could have written this spec");
         return var2;
      } else {
         boolean var4 = !var3.d().equals(var1);
         var2.put("drift", var4);
         var2.put("digest", var3.c());
         if (var4) {
            var2.put("reason", "the spec is not what Rule.project writes");
         }

         return var2;
      }
   }

   private Map<String, String> a(Path var1, Map<String, Object> var2) {
      LinkedHashMap<java.lang.String, java.lang.String> var3 = new LinkedHashMap();

      io.karatelabs.ext.rules.WitnessBaselineStore.WitnessSnapshot var4;
      try {
         var4 = new WitnessBaselineStore(var1).a();
      } catch (io.karatelabs.ext.rules.WitnessBaselineStore.WitnessReadException var10) {
         var2.put("store", "corrupt");
         return var3;
      }

      if (var4.a() == null) {
         var2.put("store", "absent");
         return var3;
      } else if (var4.b() == null) {
         var2.put("store", "otherVersion");
         var2.put("version", var4.a());
         return var3;
      } else {
         var var5 = var4.b();
         Object var6 = b(var5.get("selection")).get("walkSha");
         var2.put("walkSha", var6);
         var2.put("against", var5.get("against"));
         if (!this.f.c().equals(var6)) {
            var2.put("store", "stale");
            return var3;
         } else {
            var2.put("store", "current");

            for (Object var8 : c(var5.get("obligations"))) {
               var var9 = b(var8);
               if ("transitions".equals(var9.get("kind"))) {
                  var3.put(String.valueOf(var9.get("key")), String.valueOf(var9.get("credit")));
               }
            }

            return var3;
         }
      }
   }

   private List<Map<String, Object>> a() {
      ArrayList<java.util.Map<java.lang.String,java.lang.Object>> var1 = new ArrayList();

      for (io.karatelabs.ext.rules.twin.TwinWalkResult.WalkState var3 : this.f.e()) {
         LinkedHashMap<java.lang.String, java.lang.Object> var4 = new LinkedHashMap();
         var4.put("id", var3.a());
         var4.put("key", var3.b());
         var4.put("states", this.i.get(var3.a()));
         var4.put("depth", var3.d());
         var1.add(var4);
      }

      return var1;
   }

   private String a(Object var1, String var2, Map<String, Object> var3, Object var4) {
      return TwinCoverageAnalyzer.a(var1, this.g) + "|" + var2 + "|" + io.karatelabs.ext.rules.twin.CanonicalJson.c(var3) + "|" + TwinCoverageAnalyzer.a(var4, this.g);
   }

   private List<Map<String, Object>> a(Map<String, String> var1, Map<String, Set<String>> var2) {
      TreeMap<java.lang.String, java.util.Map<java.lang.String,java.lang.Object>> var3 = new TreeMap<>(io.karatelabs.ext.rules.twin.CanonicalJson::a);

      for (io.karatelabs.ext.rules.twin.TwinModel.CommandModel var5 : this.h.values()) {
         var3.put(var5.q().a(), this.a(var5, var1, var2));
      }

      return new ArrayList<>(var3.values());
   }

   private Map<String, Object> a(io.karatelabs.ext.rules.twin.TwinModel.CommandModel var1, Map<String, String> var2, Map<String, Set<String>> var3) {
      TreeMap<String, io.karatelabs.ext.rules.twin.ProjectionReportBuilder.TransitionTuple> var4 = new TreeMap<>(io.karatelabs.ext.rules.twin.CanonicalJson::a);
      int var5 = 0;

      for (io.karatelabs.ext.rules.twin.TwinWalkResult.WalkTransition var7 : this.f.f()) {
         if (var7.b().equals(var1.a())) {
            if (var7.d() == null) {
               var5++;
            } else {
               Object var8 = this.f.e().get(var7.a()).c();
               Object var9 = this.f.e().get(var7.d()).c();
               String var10 = this.a(var8, var7.b(), var7.c(), var9);
               io.karatelabs.ext.rules.twin.ProjectionReportBuilder.TransitionTuple var11 = var4.computeIfAbsent(var10, var2x -> new io.karatelabs.ext.rules.twin.ProjectionReportBuilder.TransitionTuple(var2x, var7));
               var11.c.add(var7);
               var11.d.addAll(TwinCoverageAnalyzer.a(this.g, var8, var9));
            }
         }
      }

      TreeMap<java.lang.String, io.karatelabs.ext.rules.twin.ProjectionReportBuilder.RefusalOrErrorGroup> var12 = new TreeMap<>(io.karatelabs.ext.rules.twin.CanonicalJson::a);

      for (io.karatelabs.ext.rules.twin.TwinWalkResult.WalkRefusal var15 : this.f.g()) {
         if (var15.b().equals(var1.a())) {
            var12.computeIfAbsent(var15.d() + "\n" + var15.e(), var2x -> a(var1, var15)).b.add(var15.a());
         }
      }

      TreeMap<java.lang.String, io.karatelabs.ext.rules.twin.ProjectionReportBuilder.RefusalOrErrorGroup> var14 = new TreeMap<>(io.karatelabs.ext.rules.twin.CanonicalJson::a);

      for (io.karatelabs.ext.rules.twin.TwinWalkResult.WalkError var18 : this.f.h()) {
         if (var18.b().equals(var1.a())) {
            var14.computeIfAbsent(var18.d() + "\n" + var18.e(), var1x -> a(var18)).b.add(var18.a());
         }
      }

      LinkedHashMap<java.lang.String, java.lang.Object> var17 = new LinkedHashMap();
      var17.put("opId", var1.q().a());
      var17.put("command", var1.a());
      if (var1.j() != null) {
         var17.put("req", var1.j());
      }

      var17.put("grade", "model");
      ArrayList<java.util.Map<java.lang.String,java.lang.Object>> var19 = new ArrayList();

      for (io.karatelabs.ext.rules.twin.ProjectionReportBuilder.TransitionTuple var21 : (Iterable<io.karatelabs.ext.rules.twin.ProjectionReportBuilder.TransitionTuple>) (Iterable<?>) (var4.values())) {
         var19.add(var21.a((String)var2.get(var21.a), (Set<String>)var3.get(var21.a)));
      }

      var17.put("tuples", var19);
      var17.put("depthPruned", var5);
      var17.put("refusals", this.a(var12.values()));
      var17.put("errors", this.a(var14.values()));
      var17.put("groups", this.a(var4.values(), var12.values(), var14.values()));
      return var17;
   }

   private static io.karatelabs.ext.rules.twin.ProjectionReportBuilder.RefusalOrErrorGroup a(io.karatelabs.ext.rules.twin.TwinModel.CommandModel var0, io.karatelabs.ext.rules.twin.TwinWalkResult.WalkRefusal var1) {
      LinkedHashMap<java.lang.String, java.lang.Object> var2 = new LinkedHashMap();
      var2.put("stage", var1.d());
      var2.put("reason", var1.e());
      var2.put("status", b(var1.d()));
      String var3 = "guard".equals(var1.d()) ? (var0.c() ? null : var0.o().get("guard")) : ("apply".equals(var1.d()) ? var1.e() : null);
      if ("guard".equals(var1.d()) && var0.c()) {
         var2.put("scopeGuard", true);
      }

      if (var3 != null) {
         var2.put("wireReason", var3);
         List var4 = var0.q() == null ? null : var0.q().e().b().get(var3);
         if (var4 != null && !var4.isEmpty()) {
            LinkedHashMap<java.lang.String, java.lang.Object> var5 = new LinkedHashMap();
            var5.put("status", ((io.karatelabs.ext.rules.twin.TwinModel.WireRefusal)var4.get(0)).a());
            var5.put("code", ((io.karatelabs.ext.rules.twin.TwinModel.WireRefusal)var4.get(0)).b());
            var2.put("wire", var5);
         }
      }

      if (var1.f() != null) {
         var2.put("detail", var1.f());
      }

      return new io.karatelabs.ext.rules.twin.ProjectionReportBuilder.RefusalOrErrorGroup(var2);
   }

   private static io.karatelabs.ext.rules.twin.ProjectionReportBuilder.RefusalOrErrorGroup a(io.karatelabs.ext.rules.twin.TwinWalkResult.WalkError var0) {
      LinkedHashMap<String, Object> var1 = new LinkedHashMap();
      var1.put("stage", var0.d());
      var1.put("message", var0.e());
      if (var0.f() != null) {
         var1.put("code", var0.f());
      }

      var1.put("status", "errored");
      return new io.karatelabs.ext.rules.twin.ProjectionReportBuilder.RefusalOrErrorGroup(var1);
   }

   private static String b(String var0) {
      return switch (var0) {
         case "guard" -> "unavailable";
         case "apply" -> "refused";
         default -> "errored";
      };
   }

   private List<Map<String, Object>> a(Collection<io.karatelabs.ext.rules.twin.ProjectionReportBuilder.RefusalOrErrorGroup> var1) {
      ArrayList<java.util.Map<java.lang.String,java.lang.Object>> var2 = new ArrayList();

      for (io.karatelabs.ext.rules.twin.ProjectionReportBuilder.RefusalOrErrorGroup var4 : var1) {
         var var5 = new LinkedHashMap<>(var4.a);
         LinkedHashSet<String> var6 = new LinkedHashSet<>();

         for (int var8 : var4.b) {
            var6.addAll(this.i.get(var8));
         }

         var5.put("count", var4.b.size());
         var5.put("fromStates", this.a(var6));
         var2.add(var5);
      }

      return var2;
   }

   private List<Map<String, Object>> a(Collection<io.karatelabs.ext.rules.twin.ProjectionReportBuilder.TransitionTuple> var1, Collection<io.karatelabs.ext.rules.twin.ProjectionReportBuilder.RefusalOrErrorGroup> var2, Collection<io.karatelabs.ext.rules.twin.ProjectionReportBuilder.RefusalOrErrorGroup> var3) {
      ArrayList<java.util.Map<java.lang.String,java.lang.Object>> var4 = new ArrayList();

      for (String var6 : this.e()) {
         ArrayList<java.util.LinkedHashMap> var7 = new ArrayList();
         ArrayList<List<Object>> var8 = new ArrayList<>();

         for (io.karatelabs.ext.rules.twin.ProjectionReportBuilder.TransitionTuple var10 : var1) {
            ArrayList<java.lang.Object> var11 = new ArrayList();

            for (io.karatelabs.ext.rules.twin.TwinWalkResult.WalkTransition var13 : var10.c) {
               if (this.i.get(var13.a()).contains(var6)) {
                  var11.add(this.f.e().get(var13.a()).c());
               }
            }

            if (!var11.isEmpty()) {
               LinkedHashMap<java.lang.String, java.lang.String> var23 = new LinkedHashMap();
               var23.put("tuple", var10.a());
               var7.add(var23);
               var8.add(var11);
            }
         }

         for (Collection var18 : List.of(var2, var3)) {
            for (io.karatelabs.ext.rules.twin.ProjectionReportBuilder.RefusalOrErrorGroup var24 : (Iterable<io.karatelabs.ext.rules.twin.ProjectionReportBuilder.RefusalOrErrorGroup>) (Iterable<?>) (var18)) {
               ArrayList<java.lang.Object> var26 = new ArrayList();

               for (int var15 : var24.b) {
                  if (this.i.get(var15).contains(var6)) {
                     var26.add(this.f.e().get(var15).c());
                  }
               }

               if (!var26.isEmpty()) {
                  LinkedHashMap<java.lang.Object, java.lang.Object> var27 = new LinkedHashMap();
                  var27.put("stage", var24.a.get("stage"));
                  var27.put(var24.a.containsKey("reason") ? "reason" : "message", var24.a.getOrDefault("reason", var24.a.get("message")));
                  var27.put("status", var24.a.get("status"));
                  var7.add(var27);
                  var8.add(var26);
               }
            }
         }

         if (var7.size() >= 2) {
            ArrayList<Object> var17 = new ArrayList<>();
            var8.forEach(var17::addAll);
            Set<String> var19 = a(var17);

            for (int var21 = 0; var21 < var7.size(); var21++) {
               LinkedHashMap<java.lang.String, java.util.ArrayList<java.lang.String>> var25 = new LinkedHashMap();
               b((List<Object>)var8.get(var21)).forEach((var2x, var3x) -> {
                  if (var19.contains(var2x)) {
                     var25.put(var2x, new ArrayList<>(var3x));
                  }
               });
               ((Map)var7.get(var21)).put("values", var25);
            }

            LinkedHashMap<java.lang.String, java.lang.Object> var22 = new LinkedHashMap();
            var22.put("state", var6);
            var22.put("varies", new ArrayList(var19));
            var22.put("rows", var7);
            var4.add(var22);
         }
      }

      return var4;
   }

   private List<Map<String, Object>> b() {
      LinkedHashMap<Integer, Map<String, List<Map<String, Object>>>> var1 = new LinkedHashMap<>();

      for (io.karatelabs.ext.rules.twin.TwinWalkResult.WalkTransition var3 : this.f.f()) {
         LinkedHashMap<String, Object> var4 = new LinkedHashMap();
         var4.put("outcome", "applied");
         a(var1, var3.a(), var3.b(), var4, var3.d() == null);
      }

      for (io.karatelabs.ext.rules.twin.TwinWalkResult.WalkRefusal var21 : this.f.g()) {
         io.karatelabs.ext.rules.twin.TwinModel.CommandModel var24 = this.e.f().get(var21.b());
         var var5 = new LinkedHashMap<>(a(var24, var21).a);
         var5.put("outcome", var5.remove("status"));
         a(var1, var21.a(), var21.b(), var5, false);
      }

      for (io.karatelabs.ext.rules.twin.TwinWalkResult.WalkError var22 : this.f.h()) {
         var var25 = new LinkedHashMap<>(a(var22).a);
         var25.put("outcome", var25.remove("status"));
         a(var1, var22.a(), var22.b(), var25, false);
      }

      ArrayList<java.util.Map<java.lang.String,java.lang.Object>> var20 = new ArrayList();

      for (String var26 : this.e()) {
         ArrayList<io.karatelabs.ext.rules.twin.TwinWalkResult.WalkState> var27 = new ArrayList();

         for (io.karatelabs.ext.rules.twin.TwinWalkResult.WalkState var7 : this.f.e()) {
            if (this.i.get(var7.a()).contains(var26)) {
               var27.add(var7);
            }
         }

         boolean var28 = false;
         ArrayList<java.util.LinkedHashMap> var29 = new ArrayList();

         for (io.karatelabs.ext.rules.twin.TwinModel.CommandModel var9 : this.e.c()) {
            LinkedHashMap<java.lang.String, io.karatelabs.ext.rules.twin.ProjectionReportBuilder.CommandOutcomeGroup> var10 = new LinkedHashMap();

            for (io.karatelabs.ext.rules.twin.TwinWalkResult.WalkState var12 : (Iterable<io.karatelabs.ext.rules.twin.TwinWalkResult.WalkState>) (Iterable<?>) (var27)) {
               for (Map var14 : (Iterable<Map>) (Iterable<?>) (var1.getOrDefault(var12.a(), Map.of()).getOrDefault(var9.a(), List.of()))) {
                  LinkedHashMap<java.lang.String, Object> var15 = new LinkedHashMap(var14);
                  Object var16 = var15.remove("depthPruned");
                  io.karatelabs.ext.rules.twin.ProjectionReportBuilder.CommandOutcomeGroup var17 = var10.computeIfAbsent(io.karatelabs.ext.rules.twin.CanonicalJson.c(var15), var1x -> new io.karatelabs.ext.rules.twin.ProjectionReportBuilder.CommandOutcomeGroup(var15));
                  var17.b.add(var12.a());
                  if (Boolean.TRUE.equals(var16)) {
                     var17.c++;
                  }
               }
            }

            LinkedHashMap<java.lang.String, java.lang.Object> var31 = new LinkedHashMap();
            var31.put("command", var9.a());
            if (this.h.containsKey(var9.a())) {
               var31.put("operation", var9.q().a());
            }

            var31.put("inputs", a(var9));
            ArrayList<java.util.LinkedHashMap<java.lang.String,java.lang.Object>> var32 = new ArrayList();
            if (var10.isEmpty()) {
               LinkedHashMap<String, Object> var33 = new LinkedHashMap();
               var33.put("outcome", "notreached");
               var33.put("reason", "not attempted under this plan");
               var32.add(var33);
            }

            var var34 = new ArrayList<>(var10.values());
            var34.sort((var0, var1x) -> io.karatelabs.ext.rules.twin.CanonicalJson.a(io.karatelabs.ext.rules.twin.CanonicalJson.c(var0.a), io.karatelabs.ext.rules.twin.CanonicalJson.c(var1x.a)));

            for (io.karatelabs.ext.rules.twin.ProjectionReportBuilder.CommandOutcomeGroup var36 : (Iterable<io.karatelabs.ext.rules.twin.ProjectionReportBuilder.CommandOutcomeGroup>) (Iterable<?>) (var34)) {
               var var37 = new LinkedHashMap<>(var36.a);
               if (var36.c > 0) {
                  var37.put("depthPruned", var36.c);
               }

               if (var34.size() > 1) {
                  var37.put("worlds", new ArrayList<>(var36.b));
               }

               var32.add(var37);
               var28 |= a(var36.a);
            }

            var31.put("outcomes", var32);
            var29.add(var31);
         }

         LinkedHashMap<java.lang.String, java.lang.Object> var30 = new LinkedHashMap();
         var30.put("state", var26);
         var30.put("worlds", var27.size());
         var30.put("noneEnabled", !var28);
         var30.put("commands", var29);
         var20.add(var30);
      }

      return var20;
   }

   private static void a(Map<Integer, Map<String, List<Map<String, Object>>>> var0, int var1, String var2, Map<String, Object> var3, boolean var4) {
      if (var4) {
         var3.put("depthPruned", true);
      }

      var0.computeIfAbsent(var1, var0x -> new LinkedHashMap<>()).computeIfAbsent(var2, var0x -> new ArrayList<>()).add(var3);
   }

   private static boolean a(Map<String, Object> var0) {
      return !"unavailable".equals(var0.get("outcome")) && !"guard".equals(var0.get("stage"));
   }

   private static List<String> a(io.karatelabs.ext.rules.twin.TwinModel.CommandModel var0) {
      ArrayList<java.lang.String> var1 = new ArrayList();
      if (var0.g() != null) {
         for (io.karatelabs.ext.rules.twin.TwinModel.ArgumentAxis var6 : var0.g()) {
            var1.add(var6.a());
         }

         return var1;
      } else {
         TreeSet var2 = new TreeSet<String>(io.karatelabs.ext.rules.twin.CanonicalJson::a);

         for (Map var4 : var0.f()) {
            var2.addAll(var4.keySet());
         }

         var1.addAll(var2);
         return var1;
      }
   }

   private void a(ReplayTrace var1, String var2, String var3, io.karatelabs.ext.rules.twin.ProjectionReportBuilder.OperationFlowTracker var4) {
      var var5 = var1.a();

      for (int var6 = 0; var6 + 1 < var5.size(); var6++) {
         String var7 = this.a((io.karatelabs.ext.rules.twin.ReplayTrace.ReplayStep)var5.get(var6));
         String var8 = this.a((io.karatelabs.ext.rules.twin.ReplayTrace.ReplayStep)var5.get(var6 + 1));
         if (var7 != null && var8 != null) {
            var4.a
               .computeIfAbsent(var7 + "\n" + var8, var0 -> new TreeMap<>())
               .computeIfAbsent(var2, var0 -> new TreeSet<>("walk".equals(var0) ? j : io.karatelabs.ext.rules.twin.CanonicalJson::a))
               .add(var3);
         }
      }
   }

   private String a(io.karatelabs.ext.rules.twin.ReplayTrace.ReplayStep var1) {
      io.karatelabs.ext.rules.twin.TwinModel.CommandModel var2 = this.h.get(var1.a());
      return var2 != null && "applied".equals(var1.c()) ? var2.q().a() : null;
   }

   private Map<String, Object> a(io.karatelabs.ext.rules.twin.ProjectionReportBuilder.OperationFlowTracker var1, Map<String, Integer> var2) {
      ArrayList<io.karatelabs.ext.openapi.WitnessedPathGraphRenderer.WitnessedPathNode> var3 = new ArrayList();
      ArrayList<java.util.LinkedHashMap> var4 = new ArrayList();

      for (io.karatelabs.ext.rules.twin.TwinModel.CommandModel var6 : this.h.values()) {
         var3.add(new io.karatelabs.ext.openapi.WitnessedPathGraphRenderer.WitnessedPathNode(var6.q().a(), var6.a()));
         LinkedHashMap<java.lang.String, java.lang.String> var7 = new LinkedHashMap();
         var7.put("opId", var6.q().a());
         var7.put("command", var6.a());
         var4.add(var7);
      }

      ArrayList<io.karatelabs.ext.openapi.WitnessedPathGraphRenderer.WitnessedPathEdge> var15 = new ArrayList();
      ArrayList<java.util.LinkedHashMap> var16 = new ArrayList();

      for (Entry<String, Map<String, Set<String>>> var8 : var1.a.entrySet()) {
         String[] var9 = ((String)var8.getKey()).split("\n", 2);
         ArrayList var10 = new ArrayList();
         LinkedHashMap<java.lang.String, java.lang.Object> var11 = new LinkedHashMap();
         var11.put("from", var9[0]);
         var11.put("to", var9[1]);

         for (String var13 : List.of("sequence", "walk")) {
            ArrayList<String> var14 = new ArrayList<>(var8.getValue().getOrDefault(var13, Set.of()));
            var11.put(var13, var14);
            var10.addAll(var14);
         }

         var16.add(var11);
         var15.add(new io.karatelabs.ext.openapi.WitnessedPathGraphRenderer.WitnessedPathEdge(var9[0], var9[1], var10));
      }

      LinkedHashMap<java.lang.String, java.lang.Object> var18 = new LinkedHashMap();
      var18.put("nodes", var4);
      var18.put("reads", this.c());
      var18.put("edges", var16);
      var18.put("paths", var2);
      var18.put(
         "caption",
         "An edge is two consecutive applied steps in one walk witness path or pinned sequence, labelled with its ids. It proves that adjacency alone, never a longer path through the picture."
      );
      var18.put("svg", io.karatelabs.ext.openapi.WitnessedPathGraphRenderer.a(var3, var15));
      return var18;
   }

   private List<String> c() {
      io.karatelabs.ext.rules.twin.TwinModel.ResourceIdentityModel var1 = this.e.k();
      return var1.g() ? List.of("read" + var1.h(), "list" + var1.h()) : List.of("read" + var1.h());
   }

   private List<Map<String, Object>> d() {
      ArrayList<java.util.Map<java.lang.String,java.lang.Object>> var1 = new ArrayList();

      for (io.karatelabs.ext.rules.twin.TwinModel.CommandModel var3 : this.e.c()) {
         if (!this.h.containsKey(var3.a())) {
            LinkedHashMap<String, Object> var4 = new LinkedHashMap();
            var4.put("command", var3.a());
            if (var3.q() == null) {
               var4.put("reason", "unbound");
            } else {
               var4.put("operation", var3.q().a());
               var4.put("reason", "notInSpec");
            }

            var1.add(var4);
         }
      }

      return var1;
   }

   private List<String> a(List<String> var1, List<String> var2) {
      var var3 = new LinkedHashSet<>(this.c());

      for (io.karatelabs.ext.rules.twin.TwinModel.CommandModel var5 : this.h.values()) {
         var3.add(var5.q().a());
      }

      TreeSet<java.lang.String> var7 = new TreeSet<String>(io.karatelabs.ext.rules.twin.CanonicalJson::a);

      for (String var6 : var1) {
         if (!var3.contains(var6)) {
            var7.add(var6);
         }
      }

      var7.addAll(var2);
      return new ArrayList<>(var7);
   }

   private List<String> e() {
      ArrayList<java.lang.String> var1 = new ArrayList();

      for (io.karatelabs.ext.rules.twin.TwinModel.StateModel var3 : this.e.b()) {
         var1.add(var3.a());
      }

      for (List var5 : this.i.values()) {
         if (var5.contains("(unnamed)") && !var1.contains("(unnamed)")) {
            var1.add("(unnamed)");
         }
      }

      return var1;
   }

   private List<String> a(Set<String> var1) {
      ArrayList<java.lang.String> var2 = new ArrayList();

      for (String var4 : this.e()) {
         if (var1.contains(var4)) {
            var2.add(var4);
         }
      }

      return var2;
   }

   private static Set<String> a(List<Object> var0) {
      TreeSet<java.lang.String> var1 = new TreeSet<String>(io.karatelabs.ext.rules.twin.CanonicalJson::a);

      for (Entry var3 : b(var0).entrySet()) {
         if (((Set)var3.getValue()).size() > 1) {
            var1.add((String)var3.getKey());
         }
      }

      return var1;
   }

   private static Map<String, Set<String>> b(List<Object> var0) {
      TreeMap<java.lang.String, java.util.Set<java.lang.String>> var1 = new TreeMap<>(io.karatelabs.ext.rules.twin.CanonicalJson::a);
      TreeSet<String> var2 = new TreeSet<>();
      ArrayList<Map<String, String>> var3 = new ArrayList<>();

      for (Object var5 : var0) {
         LinkedHashMap<String, String> var6 = new LinkedHashMap<>();
         a("", var5, var6);
         var3.add(var6);
         var2.addAll(var6.keySet());
      }

      for (String var10 : (Iterable<String>) (Iterable<?>) (var2)) {
         TreeSet<String> var11 = new TreeSet<>();

         for (Map<String, String> var8 : var3) {
            var11.add(var8.getOrDefault(var10, "(absent)"));
         }

         var1.put(var10, var11);
      }

      return var1;
   }

   private static void a(String var0, Object var1, Map<String, String> var2) {
      if (var1 instanceof Map var3 && !var3.isEmpty()) {
         for (Entry var5 : (Iterable<Entry>) (Iterable<?>) (var3.entrySet())) {
            a(var0 + "/" + String.valueOf(var5.getKey()).replace("~", "~0").replace("/", "~1"), var5.getValue(), var2);
         }
      } else {
         var2.put(var0, io.karatelabs.ext.rules.twin.CanonicalJson.c(var1));
      }
   }

   private static List<Map<String, Object>> a(Object var0) {
      return var0 instanceof List var1 ? var1 : List.of();
   }

   private static Map<String, Object> b(Object var0) {
      return var0 instanceof Map var1 ? var1 : Map.of();
   }

   private static List<?> c(Object var0) {
      return var0 instanceof List var1 ? var1 : List.of();
   }

   private static final class OperationFlowTracker {
      final Map<String, Map<String, Set<String>>> a = new TreeMap<>(io.karatelabs.ext.rules.twin.CanonicalJson::a);
   }

   private static final class CommandOutcomeGroup {
      final Map<String, Object> a;
      final Set<Integer> b = new TreeSet<>();
      int c;

      CommandOutcomeGroup(Map<String, Object> var1) {
         this.a = var1;
      }
   }

   private static final class RefusalOrErrorGroup {
      final Map<String, Object> a;
      final Set<Integer> b = new TreeSet<>();

      RefusalOrErrorGroup(Map<String, Object> var1) {
         this.a = var1;
      }
   }

   private final class TransitionTuple {
      final String a;
      final io.karatelabs.ext.rules.twin.TwinWalkResult.WalkTransition b;
      final List<io.karatelabs.ext.rules.twin.TwinWalkResult.WalkTransition> c = new ArrayList<>();
      final Set<String> d = new TreeSet<String>(io.karatelabs.ext.rules.twin.CanonicalJson::a);

      TransitionTuple(String var2, io.karatelabs.ext.rules.twin.TwinWalkResult.WalkTransition var3) {
         this.a = var2;
         this.b = var3;
      }

      String a() {
         return io.karatelabs.ext.rules.twin.CanonicalJson.a(this.a).substring(0, 16);
      }

      List<Object> b() {
         ArrayList<java.lang.Object> var1 = new ArrayList();

         for (io.karatelabs.ext.rules.twin.TwinWalkResult.WalkTransition var3 : this.c) {
            var1.add(ProjectionReportBuilder.this.f.e().get(var3.a()).c());
         }

         return var1;
      }

      Map<String, Object> a(String var1, Set<String> var2) {
         LinkedHashSet<String> var3 = new LinkedHashSet<>();
         LinkedHashSet<String> var4 = new LinkedHashSet<>();
         ArrayList<java.util.LinkedHashMap> var5 = new ArrayList();
         ArrayList<java.lang.Object> var6 = new ArrayList();

         for (io.karatelabs.ext.rules.twin.TwinWalkResult.WalkTransition var8 : this.c) {
            var3.addAll(ProjectionReportBuilder.this.i.get(var8.a()));
            var4.addAll(ProjectionReportBuilder.this.i.get(var8.d()));
            LinkedHashMap<java.lang.String, java.lang.Integer> var9 = new LinkedHashMap();
            var9.put("from", var8.a());
            var9.put("to", var8.d());
            var5.add(var9);
            var6.add(ProjectionReportBuilder.this.f.e().get(var8.d()).c());
         }

         TreeSet var10 = new TreeSet<String>(io.karatelabs.ext.rules.twin.CanonicalJson::a);
         var10.addAll(io.karatelabs.ext.rules.twin.ProjectionReportBuilder.a(this.b()));
         var10.addAll(io.karatelabs.ext.rules.twin.ProjectionReportBuilder.a(var6));
         LinkedHashMap<java.lang.String, java.lang.Object> var11 = new LinkedHashMap();
         var11.put("id", this.a());
         var11.put("command", this.b.b());
         var11.put("argsClass", io.karatelabs.ext.rules.twin.CanonicalJson.c(this.b.c()));
         var11.put("fromStates", ProjectionReportBuilder.this.a(var3));
         var11.put("toStates", ProjectionReportBuilder.this.a(var4));
         var11.put("grade", "model");
         if (var1 != null) {
            var11.put("witness", var1);
         }

         var11.put("sequences", var2 == null ? List.of() : new ArrayList(var2));
         var11.put("members", var5);
         var11.put("differing", new ArrayList(var10));
         var11.put("maskedFields", new ArrayList<>(this.d));
         return var11;
      }
   }
}
