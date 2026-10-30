package io.karatelabs.ext.sim.jsapi;

import io.karatelabs.common.Json;
import io.karatelabs.ext.common.MaxPaths;
import io.karatelabs.ext.common.jsapi.JsApi;
import io.karatelabs.ext.common.jsapi.JsDispatch;
import io.karatelabs.ext.coverage.CoverageExt;
import io.karatelabs.ext.coverage.TraceDocumentBuilder;
import io.karatelabs.ext.coverage.CoverageSource;
import io.karatelabs.ext.coverage.OpenApiSpecDescriptor;
import io.karatelabs.ext.coverage.collect.RequirementsCoverageSource;
import io.karatelabs.ext.coverage.graph.TraceGraph;
import io.karatelabs.ext.sim.SimulationBuilder;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class SimApi extends JsDispatch {
   private final Path defaultReportDir;

   public SimApi() {
      this(null);
   }

   public SimApi(Path var1) {
      this.defaultReportDir = var1;
      this.fn("build", var1x -> this.build(JsApi.arg(var1x, 0)));
      this.fn("topology", var1x -> this.topology(JsApi.arg(var1x, 0)));
      this.fn("help", var0 -> JsApi.helpTopic(help(), var0));
   }

   public static SimApi atReportDir(Path var0) {
      return new SimApi(var0);
   }

   Map<String, Object> build(Object var1) {
      if (var1 instanceof Map var2) {
         String var3 = unresolvedSpec(var2);
         if (var3 != null) {
            return JsApi.missing(
               "Sim.build: spec file not found: "
                  + var3
                  + " — `spec` paths resolve against `dir` (the serve process working directory); pass an absolute path or set `dir`"
            );
         } else {
            SimulationBuilder var4 = io.karatelabs.ext.sim.SimulationBuilder.a();
            seedRequirements(var4, var2.get("requirements"));
            seedScenarios(var4, var2.get("scenarios"));
            ArrayList<io.karatelabs.ext.coverage.CoverageSource> var5 = new ArrayList<>();
            if (var2.get("requirements") instanceof List var6 && !var6.isEmpty()) {
               Path var12 = this.homeFor(var2);
               writeString(var12.resolve("store.md"), var4.e());
               var5.add(new RequirementsCoverageSource(var12, var2.get("criticalityPolicy")));
            }

            var5.addAll(CoverageExt.openApiSources(specSource(var2), rootOf(var2)));
            TraceGraph var11 = var4.a(var5.toArray(new CoverageSource[0]));
            Map<java.lang.String, java.lang.Object> var13 = io.karatelabs.ext.coverage.TraceDocumentBuilder.a(var11, var5);
            String var8 = this.effectiveOut(var2);
            if (var8 != null) {
               Path var9 = Path.of(var8).resolve("ext").resolve("coverage");
               String var10 = Json.stringifyStrict(var13);
               writeString(var9.resolve("coverage-data.json"), var10);
               writeString(var9.resolve("coverage-data.js"), "window.KARATE_COVERAGE_DATA = " + var10 + ";\n");
               var13.put("dataFile", var9.resolve("coverage-data.json").toString());
               var13.put("reportDir", var8);
            }

            return var13;
         }
      } else {
         return JsApi.missing(
            "Sim.build(spec) requires an object, e.g. { requirements:[{id,title,criticality,acceptance:[…]}], scenarios:[{name,tags:['@req=ORD-001'],status,http:'POST /orders 201'}], spec?, out? }"
         );
      }
   }

   Map<String, Object> topology(Object var1) {
      if (var1 instanceof Map var2) {
         String var3 = unresolvedSpec(var2);
         if (var3 != null) {
            return JsApi.missing(
               "Sim.topology: spec file not found: "
                  + var3
                  + " — `spec` paths resolve against `dir` (the serve process working directory); pass an absolute path or set `dir`"
            );
         } else {
            io.karatelabs.ext.sim.SimulationBuilder.SimulationSpecBuilder var4 = io.karatelabs.ext.sim.SimulationBuilder.b();
            Object var6 = var2.get("requirements");
            if (var6 instanceof List) {
               for (Object var7 : (List)var6) {
                  if (var7 instanceof Map var8) {
                     String var9 = strOpt(var8, "id");
                     if (var9 != null) {
                        var4.a(var9, strOpt(var8, "title"), strOpt(var8, "criticality"), strArray(var8.get("acceptance")));
                     }
                  }
               }
            }

            var6 = var2.get("rows");
            if (var6 instanceof List) {
               for (Object var19 : (List)var6) {
                  if (var19 instanceof Map var21) {
                     io.karatelabs.ext.sim.SimulationBuilder.SimulationRowBuilder var22 = var4.a();
                     if (strOpt(var21, "test") != null) {
                        var22.b(strOpt(var21, "test"));
                     }

                     if (strOpt(var21, "req") != null) {
                        var22.c(strOpt(var21, "req"));
                     }

                     String var10 = strOpt(var21, "status");
                     if ("failed".equals(var10)) {
                        var22.b();
                     } else if ("skipped".equals(var10)) {
                        var22.c();
                     } else if ("passed".equals(var10)) {
                        var22.a();
                     }

                     if (var21.get("http") instanceof String var11) {
                        var22.d(var11);
                     }

                     String[] var23 = expectOf(var21.get("expect"));
                     if (var23 == null) {
                        return JsApi.missing("each topology row needs expect:['<itemId>','<STATUS>'] or {item,status}");
                     }

                     var22.a(var23[0], var23[1]);
                  }
               }
            }

            var var14 = new ArrayList<>(CoverageExt.openApiSources(specSource(var2), rootOf(var2)));
            var mismatches = var4.b(this.homeFor(var2), var14.toArray(new CoverageSource[0]));
            LinkedHashMap<java.lang.String, java.lang.Object> var20 = new LinkedHashMap();
            var20.put("ok", mismatches.isEmpty());
            var20.put("checked", var4.b());
            var20.put("mismatches", mismatches);
            return var20;
         }
      } else {
         return JsApi.missing(
            "Sim.topology(spec) requires an object, e.g. { requirements:[{id,title,criticality,acceptance:[…]}], rows:[{test,req,status,http,expect:['req:ORD-001','COVERED']}], out? }"
         );
      }
   }

   private static Map<String, Object> help() {
      String var0 = "# Sim — simulate / what-if a coverage world, run-free (incl. around an OpenAPI spec)\n\nFabricate a world (requirements + test scenarios + OpenAPI/rules coverage sources), derive the\ntraceability graph through the real engine, and inspect the resulting report — **no `karate\nrun`**. Use it to what-if a change (new endpoint, failing scenario, untested criterion) and see\nhow the coverage/RTM report moves, or to stand up a demo world around a spec.\n\n`Sim.build(spec)` — fabricate + derive → `karate-trace/v1` coverage data (same shape Coverage/Graph/Requirement read).\n`Sim.topology(spec)` — the seed-is-oracle gate: rows + an authored `expect`, diffed → `{ok, checked, mismatches}`.\nCall `Sim.build({...})` and end with the value; pass `spec: 'api/openapi.yaml'` to simulate around a real OpenAPI spec.\n\n## build spec\n```\n{ requirements: [{ id, title?, criticality?, status?, parent?, body?, acceptance?: ['1.1: WHEN … SHALL …'],\n                   real?: ['openapi:placeOrder'] }],    // @real= → provenance exercised/partexercised/incidental/notexercised; parent = epic→feature→leaf edge\n  scenarios:    [{ feature?, name, line?, id?, tags?: ['@req=ORD-001'], status?: 'passed|failed|skipped',\n                   http?: 'POST /orders 201' | { method, path, status, ms? },\n                   emit?: { source, key, evidence? }, grpc?: { service, method, status, ms? } }],\n  spec?: 'api/openapi.yaml' | { store: 'store.yaml' },   // OpenAPI coverage source(s)\n  criticalityPolicy?: { high: 'airgap', default: 'shared' },  // P20 posture override (else high⇒attest)\n  root?: '.',                                            // THE root `spec` references resolve against (paths.md:\n                                                         // a leading '/' anchors it, bare is relative to it,\n                                                         // 'file:' is a host path); a spec that doesn't resolve\n                                                         // errors, never silently skips\n  out?:  'target/sim-report' }                          // write coverage-data.json (+ .js) here\n```\n\n## topology spec\n```\n{ requirements: [{ id, title?, criticality?, acceptance?: [...] }],\n  rows: [{ test?, req?, status?, http?: 'POST /orders 201',\n           expect: ['req:ORD-001','COVERED'] }],         // [itemId, STATUS] or {item,status}\n  out?: 'dir', requirementsHome?: 'dir', spec?: ... }\n```\nStatuses: COVERED · FAILING · NOTRUN · NOTCOVERED. A bare `{req, expect}` row (no test/status/http)\nasserts a seeded requirement's dry-run gap.\n";
      LinkedHashMap<java.lang.String, java.lang.Object> var1 = new LinkedHashMap();
      var1.put("markdown", var0);
      return var1;
   }

   private static void seedRequirements(SimulationBuilder var0, Object var1) {
      if (var1 instanceof List) {
         for (Object var4 : (List)var1) {
            if (var4 instanceof Map var5) {
               String var6 = strOpt(var5, "id");
               if (var6 != null) {
                  io.karatelabs.ext.sim.SimulationBuilder.RequirementBuilder var7 = var0.a(var6);
                  if (strOpt(var5, "title") != null) {
                     var7.a(strOpt(var5, "title"));
                  }

                  if (strOpt(var5, "status") != null) {
                     var7.c(strOpt(var5, "status"));
                  }

                  if (strOpt(var5, "criticality") != null) {
                     var7.d(strOpt(var5, "criticality"));
                  }

                  if (strOpt(var5, "parent") != null) {
                     var7.b(strOpt(var5, "parent"));
                  }

                  if (strOpt(var5, "body") != null) {
                     var7.e(strOpt(var5, "body"));
                  }

                  var7.a(strArray(var5.get("acceptance")));
                  var7.b(strArray(var5.get("real")));
               }
            }
         }
      }
   }

   private static void seedScenarios(SimulationBuilder var0, Object var1) {
      if (var1 instanceof List) {
         for (Object var4 : (List)var1) {
            if (var4 instanceof Map var5) {
               String var6 = strOpt(var5, "name");
               if (var6 != null) {
                  String var7 = strOpt(var5, "feature") != null ? strOpt(var5, "feature") : "sim.feature";
                  io.karatelabs.ext.sim.SimulationBuilder.SimulatedScenario var8 = var0.a(var7, var6);
                  if (var5.get("line") instanceof Number var9) {
                     var8.a(var9.intValue());
                  }

                  if (strOpt(var5, "id") != null) {
                     var8.a(strOpt(var5, "id"));
                  }

                  String[] var12 = strArray(var5.get("tags"));
                  if (var12.length > 0) {
                     var8.a(var12);
                  }

                  String var13 = strOpt(var5, "status") == null ? "passed" : strOpt(var5, "status");
                  switch (var13) {
                     case "failed":
                        var8.b();
                        break;
                     case "skipped":
                        var8.c();
                        break;
                     default:
                        var8.a();
                  }

                  applyHttp(var8, var5.get("http"));
                  applyEmit(var8, var5.get("emit"));
                  applyGrpc(var8, var5.get("grpc"));
               }
            }
         }
      }
   }

   private static void applyHttp(io.karatelabs.ext.sim.SimulationBuilder.SimulatedScenario var0, Object var1) {
      if (var1 instanceof String var2) {
         String[] var4 = var2.trim().split("\\s+");
         if (var4.length >= 3) {
            var0.a(var4[0], var4[1], parseInt(var4[2], 0));
         }
      } else if (var1 instanceof Map var3) {
         String var5 = strOpt(var3, "path");
         if (var5 != null) {
            var0.a(strOpt(var3, "method") != null ? strOpt(var3, "method") : "GET", var5, intOf(var3, "status", 200), longOf(var3, "ms", 0L));
         }
      }
   }

   private static void applyEmit(io.karatelabs.ext.sim.SimulationBuilder.SimulatedScenario var0, Object var1) {
      if (var1 instanceof Map var2) {
         String var3 = strOpt(var2, "source");
         String var4 = strOpt(var2, "key");
         if (var3 != null && var4 != null) {
            var0.a(var3, var4, var2.get("evidence") instanceof Map var6 ? var6 : Map.of());
         }
      }
   }

   private static void applyGrpc(io.karatelabs.ext.sim.SimulationBuilder.SimulatedScenario var0, Object var1) {
      if (var1 instanceof Map var2) {
         String var3 = strOpt(var2, "service");
         String var4 = strOpt(var2, "method");
         if (var3 != null && var4 != null) {
            var0.a(var3, var4, strOpt(var2, "status") != null ? strOpt(var2, "status") : "OK", longOf(var2, "ms", 0L));
         }
      }
   }

   private static String[] expectOf(Object var0) {
      if (var0 instanceof List var1 && var1.size() >= 2 && var1.get(0) != null && var1.get(1) != null) {
         return new String[]{String.valueOf(var1.get(0)), String.valueOf(var1.get(1))};
      } else {
         if (var0 instanceof Map var4) {
            String var2 = strOpt(var4, "item");
            String var3 = strOpt(var4, "status");
            if (var2 != null && var3 != null) {
               return new String[]{var2, var3};
            }
         }

         return null;
      }
   }

   private String effectiveOut(Map<?, ?> var1) {
      String var2 = strOpt(var1, "out");
      if (var2 != null) {
         return var2;
      } else {
         return this.defaultReportDir == null ? null : this.defaultReportDir.toString();
      }
   }

   private Path homeFor(Map<?, ?> var1) {
      String var2 = strOpt(var1, "requirementsHome");
      if (var2 != null) {
         return MaxPaths.resolvePath(rootOf(var1), var2);
      } else {
         String var3 = this.effectiveOut(var1);
         if (var3 != null) {
            return MaxPaths.requirementsDir(Path.of(var3));
         } else {
            try {
               return Files.createTempDirectory("sim-req");
            } catch (IOException var5) {
               throw new UncheckedIOException(var5);
            }
         }
      }
   }

   private static Object specSource(Map<?, ?> var0) {
      return var0.containsKey("spec") ? var0.get("spec") : var0.get("openapi");
   }

   private static String unresolvedSpec(Map<?, ?> var0) {
      Object var1 = specSource(var0);
      Path var2 = rootOf(var0);
      ArrayList<java.lang.String> var3 = new ArrayList();

      for (OpenApiSpecDescriptor var5 : (Iterable<OpenApiSpecDescriptor>) (Iterable<?>) (OpenApiSpecDescriptor.parse(var1))) {
         var3.add(var5.b());
      }

      for (String var8 : (Iterable<String>) (Iterable<?>) (var3)) {
         Path var6 = MaxPaths.resolvePath(var2, var8);
         if (!Files.exists(var6)) {
            return var6.toString();
         }
      }

      return null;
   }

   private static Path rootOf(Map<?, ?> var0) {
      String var1 = strOpt(var0, "root");
      return var1 == null ? null : Path.of(var1).toAbsolutePath().normalize();
   }

   private static void writeString(Path var0, String var1) {
      try {
         Files.createDirectories(var0.getParent());
         Files.writeString(var0, var1, StandardCharsets.UTF_8);
      } catch (IOException var3) {
         throw new UncheckedIOException(var3);
      }
   }

   private static String strOpt(Map<?, ?> var0, String var1) {
      Object var2 = var0.get(var1);
      return var2 == null ? null : String.valueOf(var2);
   }

   private static String[] strArray(Object var0) {
      if (var0 instanceof List var5) {
         ArrayList<java.lang.String> var2 = new ArrayList();

         for (Object var4 : var5) {
            if (var4 != null) {
               var2.add(String.valueOf(var4));
            }
         }

         return var2.toArray(new String[0]);
      } else {
         return var0 instanceof String var1 ? new String[]{var1} : new String[0];
      }
   }

   private static int intOf(Map<?, ?> var0, String var1, int var2) {
      return var0.get(var1) instanceof Number var4 ? var4.intValue() : var2;
   }

   private static long longOf(Map<?, ?> var0, String var1, long var2) {
      return var0.get(var1) instanceof Number var5 ? var5.longValue() : var2;
   }

   private static int parseInt(String var0, int var1) {
      try {
         return Integer.parseInt(var0.trim());
      } catch (NumberFormatException var3) {
         return var1;
      }
   }
}
