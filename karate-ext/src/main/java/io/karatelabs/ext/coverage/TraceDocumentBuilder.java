package io.karatelabs.ext.coverage;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class TraceDocumentBuilder {
   private static final Logger a = LoggerFactory.getLogger(TraceDocumentBuilder.class);

   private TraceDocumentBuilder() {
   }

   public static Map<String, Object> a(io.karatelabs.ext.coverage.graph.TraceGraph var0, List<CoverageSource> var1) {
      return a(var0, var1, Map.of(), Map.of());
   }

   public static Map<String, Object> a(io.karatelabs.ext.coverage.graph.TraceGraph var0, List<CoverageSource> var1, Map<String, Map<String, Object>> var2) {
      return a(var0, var1, var2, Map.of());
   }

   public static Map<String, Object> a(
      io.karatelabs.ext.coverage.graph.TraceGraph var0, List<CoverageSource> var1, Map<String, Map<String, Object>> var2, Map<String, List<String>> var3
   ) {
      return a(var0, var1, var2, var3, Map.of());
   }

   public static Map<String, Object> a(
      io.karatelabs.ext.coverage.graph.TraceGraph var0,
      List<CoverageSource> var1,
      Map<String, Map<String, Object>> var2,
      Map<String, List<String>> var3,
      Map<String, List<Map<String, Object>>> var4
   ) {
      return a(var0, var1, var2, var3, var4, Map.of());
   }

   public static Map<String, Object> a(
      io.karatelabs.ext.coverage.graph.TraceGraph var0,
      List<CoverageSource> var1,
      Map<String, Map<String, Object>> var2,
      Map<String, List<String>> var3,
      Map<String, List<Map<String, Object>>> var4,
      Map<String, String> var5
   ) {
      ArrayList<java.util.Map<java.lang.String,java.lang.Object>> var6 = new ArrayList();

      for (CoverageSource var8 : var1) {
         var6.add(a(var0, var8));
      }

      LinkedHashMap<java.lang.String, java.lang.Object> var13 = new LinkedHashMap();
      var13.put("schema", "karate-trace/v1");
      var13.put("sources", var6);
      var13.put("items", a(var0, var2, var3, var4, var5));
      var13.put("tests", a(var0));
      var13.put("hits", b(var0));
      var13.put("unmatched", b(var0, var1));
      if (!var0.a().isEmpty()) {
         ArrayList<java.util.Map<java.lang.String,java.lang.Object>> var14 = new ArrayList();

         for (io.karatelabs.ext.coverage.collect.TestExecution var10 : var0.a()) {
            var var11 = var10.f();
            var11.put("selected", var0.d(var10.g()));
            String var12 = var0.e(var10.g());
            if (var12 != null) {
               var11.put("supersededBy", var12);
            }

            if (var0.f(var10.g())) {
               var11.put("retired", true);
            }

            var14.add(var11);
         }

         var13.put("executions", var14);
      }

      var13.put("selection", var0.d());
      return var13;
   }

   private static Map<String, Object> a(io.karatelabs.ext.coverage.graph.TraceGraph var0, CoverageSource var1) {
      CoverageSourceResult var2 = var0.a(var1);
      CoverageSummary var3 = var2.j();
      String var4 = var1.f();
      var var5 = var0.i();
      LinkedHashSet<java.lang.String> var6 = new LinkedHashSet();

      for (Entry var8 : (Iterable<Entry>) (Iterable<?>) (var5.entrySet())) {
         if (var4.equals(var0.j((String)var8.getKey())) && ((CoverageItem)var8.getValue()).parentId() != null) {
            var6.add(((CoverageItem)var8.getValue()).parentId());
         }
      }

      LinkedHashMap<java.lang.String, java.lang.Integer> var18 = new LinkedHashMap();

      for (io.karatelabs.ext.coverage.graph.CoverageStatus var11 : io.karatelabs.ext.coverage.graph.CoverageStatus.values()) {
         var18.put(var11.name(), 0);
      }

      for (Entry var22 : (Iterable<Entry>) (Iterable<?>) (var5.entrySet())) {
         String var24 = (String)var22.getKey();
         if (var4.equals(var0.j(var24))
            && !"acceptance-criterion".equals(((CoverageItem)var22.getValue()).getMetadata("kind"))
            && !var6.contains(var24.substring(var4.length() + 1))) {
            var18.merge(var0.n(var24).name(), 1, Integer::sum);
         }
      }

      int var21 = var18.values().stream().mapToInt(Integer::intValue).sum();
      int var23 = (Integer)var18.get("COVERED");
      double var25 = var21 == 0 ? 0.0 : Math.round(var23 * 1000.0 / var21) / 10.0;
      a.info("coverage[{}]: {}/{} items ({}%), {} warnings, {} unmatched", new Object[]{var4, var23, var21, Math.round(var25), var3.warningCount(), var2.i().size()});
      LinkedHashMap<java.lang.String, java.lang.Object> var12 = new LinkedHashMap();
      var12.put("type", var1.b());
      var12.put("namespace", var4);
      var12.put("title", var1.d());
      var12.put("version", var1.e());
      var12.put("total", var21);
      var12.put("covered", var23);
      var12.put("percentage", var25);
      var12.put("byStatus", var18);
      LinkedHashMap<java.lang.String, java.util.LinkedHashMap> var13 = new LinkedHashMap();
      var3.g().forEach((var1x, var2x) -> {
         LinkedHashMap<java.lang.String, java.lang.Object> var3x = new LinkedHashMap();
         var3x.put("total", var2x.d());
         var3x.put("covered", var2x.e());
         var3x.put("percentage", var2x.f());
         var13.put(var1x, var3x);
      });
      var12.put("tagCoverage", var13);
      ArrayList<java.util.LinkedHashMap> var14 = new ArrayList();

      for (Warning var16 : var3.h()) {
         LinkedHashMap<java.lang.String, java.lang.Object> var17 = new LinkedHashMap();
         var17.put("type", var16.type());
         var17.put("endpoint", var16.endpoint());
         var17.put("status", var16.status());
         var17.put("message", var16.message());
         var14.add(var17);
      }

      var12.put("warnings", var14);
      return var12;
   }

   private static List<Map<String, Object>> a(
      io.karatelabs.ext.coverage.graph.TraceGraph var0,
      Map<String, Map<String, Object>> var1,
      Map<String, List<String>> var2,
      Map<String, List<Map<String, Object>>> var3,
      Map<String, String> var4
   ) {
      ArrayList<java.util.Map<java.lang.String,java.lang.Object>> var5 = new ArrayList();
      LinkedHashSet<java.lang.String> var6 = new LinkedHashSet();
      var0.i().forEach((var1x, var2x) -> {
         if ("req".equals(var2x.getMetadata("kind")) && var2x.parentId() != null) {
            var6.add(var2x.parentId());
         }
      });
      var0.i()
         .forEach(
            (var7, var8) -> {
               LinkedHashMap<java.lang.String, java.lang.Object> var9 = new LinkedHashMap();
               var9.put("id", var7);
               var9.put("source", var0.j(var7));
               var9.put("name", var8.name());
               if (var8.parentId() != null) {
                  var9.put("parent", var8.parentId());
               }

               if (var8.getMethod() != null) {
                  var9.put("method", var8.getMethod());
               }

               if (var8.getPath() != null) {
                  var9.put("path", var8.getPath());
               }

               Object var10 = var8.getMetadata("responses");
               if (var10 != null) {
                  var9.put("responses", var10);
               }

               Map var11 = (Map)var1.get(var7);
               if (var11 != null && !var11.isEmpty()) {
                  var9.put("dimAxes", var11);
               }

               List var12 = (List)var2.get(var7);
               if (var12 != null && !var12.isEmpty()) {
                  var9.put("dimCross", var12);
               }

               List var13 = (List)var3.get(var7);
               if (var13 != null && !var13.isEmpty()) {
                  var9.put("dimCrossInfeasible", var13);
               }

               String var14 = (String)var4.get(var7);
               if (var14 != null && !var14.isBlank()) {
                  var9.put("dimCriticality", var14);
               }

               if ("req".equals(var8.getMetadata("kind"))) {
                  var9.put("kind", "req");
                  String var15 = var7.substring(var0.j(var7).length() + 1);
                  if (var6.contains(var15)) {
                     var9.put("container", true);
                  }

                  for (String var17 : List.of(
                     "type",
                     "reqStatus",
                     "priority",
                     "criticality",
                     "posture",
                     "ref",
                     "real",
                     "decomposes",
                     "acceptance",
                     "body",
                     "sourceFile",
                     "anchor",
                     "line",
                     "derived"
                  )) {
                     Object var18 = var8.getMetadata(var17);
                     if (var18 != null) {
                        var9.put(var17, var18);
                     }
                  }
               } else if ("acceptance-criterion".equals(var8.getMetadata("kind"))) {
                  var9.put("kind", "acceptance-criterion");
                  var9.put("parentReq", var8.getMetadata("parentReq"));
                  var9.put("criterion", var8.getMetadata("criterion"));

                  for (String var24 : List.of("claimed", "claimedBy", "claimedTask", "proofs")) {
                     Object var26 = var8.getMetadata(var24);
                     if (var26 != null) {
                        var9.put(var24, var26);
                     }
                  }
               } else if (var8.getMetadata("rule") != null) {
                  var9.put("rulebook", var8.getMetadata("rule"));
                  if ("rule-reject".equals(var8.getMetadata("kind"))) {
                     var9.put("kind", "rule-reject");
                  }
               }

               var9.put("tags", new ArrayList<>(var8.tags()));
               if (var8.isDeprecated()) {
                  var9.put("deprecated", true);
               }

               io.karatelabs.ext.coverage.graph.CoverageStatus var23 = var0.n(var7);
               var9.put("status", var23.name());
               if (var23 == io.karatelabs.ext.coverage.graph.CoverageStatus.COVERED && var0.p(var7)) {
                  var9.put("oracleOnly", true);
               }

               if (var23 == io.karatelabs.ext.coverage.graph.CoverageStatus.COVERED && var0.r(var7)) {
                  var9.put("refusalOnly", true);
               }

               if (var23 == io.karatelabs.ext.coverage.graph.CoverageStatus.COVERED && var0.q(var7)) {
                  var9.put("modelOnly", true);
               }

               if (var23 == io.karatelabs.ext.coverage.graph.CoverageStatus.COVERED && var0.u(var7)) {
                  var9.put("mockOnly", true);
               }

               if (var23 == io.karatelabs.ext.coverage.graph.CoverageStatus.COVERED && var0.s(var7)) {
                  var9.put("notasserted", true);
               }

               if (var23 == io.karatelabs.ext.coverage.graph.CoverageStatus.NOTRUN && var0.o(var7)) {
                  var9.put("weakened", true);
               }

               Double var25 = var0.t(var7);
               if (var25 != null) {
                  var9.put("assertedProportion", var25);
               }

               var var27 = var0.l(var7);
               var var28 = var0.m(var7);
               Object var19 = var28.get("callCount");
               var9.put("callCount", var19);
               if (var19 instanceof Number var20 && var20.intValue() > 0) {
                  var9.put("avgDurationMs", var28.get("avgDurationMs"));
                  if (var28.containsKey("statusCodes")) {
                     var9.put("statusCodes", var28.get("statusCodes"));
                  }

                  if (var28.containsKey("grpcStatuses")) {
                     var9.put("grpcStatuses", var28.get("grpcStatuses"));
                  }

                  if (var28.containsKey("kafkaStatuses")) {
                     var9.put("kafkaStatuses", var28.get("kafkaStatuses"));
                  }

                  var var21 = io.karatelabs.ext.coverage.ErrorPathCoverageAnalyzer.a(var28, var8.getMetadata("responses"));
                  if (var21 != null) {
                     var9.put("outcome", var21);
                  }
               }

               var9.put("tests", var27);
               var5.add(var9);
            }
         );
      return var5;
   }

   private static List<Map<String, Object>> a(io.karatelabs.ext.coverage.graph.TraceGraph var0) {
      ArrayList<java.util.Map<java.lang.String,java.lang.Object>> var1 = new ArrayList();

      for (io.karatelabs.ext.coverage.graph.TestNode var3 : var0.f()) {
         LinkedHashMap<java.lang.String, java.lang.Object> var4 = new LinkedHashMap();
         var4.put("id", var3.a());
         var4.put("name", var3.b());
         var4.put("feature", var3.c());
         var4.put("line", var3.d());
         var4.put("status", var3.f().name());
         var4.put("tags", new ArrayList<>(var3.e()));
         io.karatelabs.ext.coverage.graph.ExecutionSelection.SelectedExecution var5 = var0.c(var3.a());
         if (var5 != null) {
            var4.put("execution", var5.b().g());
            if (var5.c()) {
               var4.put("previouslyFailed", true);
            }

            if (var5.d()) {
               var4.put("pending", true);
            }

            if (var5.e()) {
               var4.put("retired", true);
            }

            if (var5.f()) {
               var4.put("unknown", true);
            }

            if (var5.b().C().get("recording") != null) {
               var4.put("recording", var5.b().C().get("recording"));
            }
         }

         if (var3.g() != null) {
            var4.put("assertionCount", var3.g().a());
            var4.put("assertedProportion", var3.g().b());
            var4.put("shapeLocked", var3.g().c());
            var4.put("sameRoot", var3.g().d());
         }

         var1.add(var4);
      }

      return var1;
   }

   private static List<Map<String, Object>> b(io.karatelabs.ext.coverage.graph.TraceGraph var0) {
      ArrayList<java.util.Map<java.lang.String,java.lang.Object>> var1 = new ArrayList();

      for (io.karatelabs.ext.coverage.graph.CoverageHit var3 : var0.g()) {
         LinkedHashMap<java.lang.String, java.lang.Object> var4 = new LinkedHashMap();
         var4.put("item", var3.a());
         var4.put("test", var3.b());
         var4.put("kind", var3.c().name());
         if (var3.i() != null) {
            var4.put("execution", var3.i());
         }

         if (var3.e() != null) {
            var4.put("confidence", var3.e().wire());
         }

         if (var3.f()) {
            var4.put("attested", true);
            if (var3.g() != null && !var3.g().isEmpty()) {
               var4.put("evidence", var3.g());
            }
         }

         if (var3.d() instanceof HttpAction var5) {
            var4.put("method", var5.e());
            var4.put("path", var5.g());
            var4.put("status", var5.i());
            var4.put("durationMs", var5.j());
         } else if (var3.d() instanceof GrpcAction var6) {
            var4.put("service", var6.service());
            var4.put("method", var6.method());
            var4.put("status", var6.status());
            var4.put("durationMs", var6.durationMs());
         } else if (var3.d() instanceof EmitAction var7) {
            var4.put("key", var7.b());
            if (var7.c() != null) {
               var4.put("evidence", var7.c());
            }
         }

         if (var3.h() != null && !var3.h().isEmpty()) {
            var4.put("attrs", var3.h());
         }

         var1.add(var4);
      }

      return var1;
   }

   private static List<Map<String, Object>> b(io.karatelabs.ext.coverage.graph.TraceGraph var0, List<CoverageSource> var1) {
      ArrayList<java.util.Map<java.lang.String,java.lang.Object>> var2 = new ArrayList();

      for (CoverageSource var4 : var1) {
         String var5 = var4.f();

         for (Action var7 : var0.h(var5)) {
            if (var7 instanceof HttpAction var8) {
               LinkedHashMap<java.lang.String, java.lang.Object> var10 = new LinkedHashMap();
               var10.put("source", var5);
               var10.put("method", var8.e());
               var10.put("path", var8.g());
               var10.put("url", var8.f());
               var10.put("status", var8.i());
               var10.put("test", io.karatelabs.ext.coverage.graph.TestNode.a(var8.featurePath(), var8.scenarioName()));
               var2.add(var10);
            } else if (var7 instanceof EmitAction var9) {
               LinkedHashMap<String, Object> var11 = new LinkedHashMap();
               var11.put("source", var5);
               var11.put("key", var9.b());
               var11.put("test", io.karatelabs.ext.coverage.graph.TestNode.a(var9.featurePath(), var9.scenarioName()));
               var2.add(var11);
            }
         }
      }

      return var2;
   }
}
