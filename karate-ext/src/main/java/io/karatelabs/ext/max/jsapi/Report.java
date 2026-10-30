package io.karatelabs.ext.max.jsapi;

import io.karatelabs.common.Json;
import io.karatelabs.common.Resource;
import io.karatelabs.core.BootBinding;
import io.karatelabs.core.BootLoader;
import io.karatelabs.core.Ext;
import io.karatelabs.ext.common.MaxPaths;
import io.karatelabs.ext.common.jsapi.JsApi;
import io.karatelabs.ext.common.jsapi.JsDispatch;
import io.karatelabs.ext.coverage.CoverageExt;
import io.karatelabs.ext.coverage.RulebookCoverageResult;
import io.karatelabs.ext.coverage.CoverageSnapshotPublisher;
import io.karatelabs.ext.coverage.collect.OpenApiCoverageSource;
import io.karatelabs.ext.coverage.collect.RequirementsCoverageSource;
import io.karatelabs.ext.openapi.jsapi.Openapi;
import io.karatelabs.ext.requirements.RequirementsExt;
import io.karatelabs.ext.requirements.jsapi.Requirement;
import io.karatelabs.ext.rules.RulebookCoverageProvider;
import io.karatelabs.ext.rules.RulebookCoverageSource;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Report extends JsDispatch {
   private static final Logger logger = LoggerFactory.getLogger(Report.class);
   private static final String RES = "META-INF/karate-ext/";
   private static final String PAGE = "ext/coverage/pages/coverage.html";
   private static final String DATA = "ext/coverage/coverage-data.js";
   private static final String DATA_FILE = "ext/coverage/coverage-data.json";
   private static final String RTM_PAGE = "ext/traceability/pages/traceability.html";
   private static final String RTM_DATA = "ext/traceability/traceability-data.js";
   private static final String CONTRACT_PAGE = "ext/contract/pages/contract.html";
   private static final String ROOT = "../../../";
   private static final String AGGREGATE_SOURCES = "ext/coverage/aggregate-sources.json";
   private static final List<String> SOURCE_OPTS = List.of("requirements", "spec", "rules", "events");
   private final Path defaultReportDir;
   private final Path projectRoot;
   private Supplier<RulebookCoverageProvider> oracle;
   private static final io.karatelabs.ext.max.jsapi.Report.BootCoverageConfig NO_BOOT = new io.karatelabs.ext.max.jsapi.Report.BootCoverageConfig(null, null, null);

   public Report(Path var1) {
      this(var1, null);
   }

   private Report(Path var1, Path var2) {
      this.defaultReportDir = var1;
      this.projectRoot = var2;
      this.fn("help", var0 -> JsApi.helpTopic(help(), var0));
      this.fn("generate", var1x -> this.generate(JsApi.arg(var1x, 0)));
      this.fn("traceability", var1x -> this.traceability(JsApi.arg(var1x, 0)));
      this.fn("aggregate", var1x -> this.aggregate(JsApi.arg(var1x, 0)));
      this.fn("gate", var1x -> this.gate(JsApi.arg(var1x, 0)));
      this.fn("url", var1x -> this.url(JsApi.arg(var1x, 0)));
      this.fn("path", var1x -> Map.of("path", this.outDir(JsApi.arg(var1x, 0)).resolve(pageName(JsApi.arg(var1x, 0))).toString()));
   }

   public Report withRoot(Path var1) {
      Report var2 = new Report(this.defaultReportDir, var1);
      var2.oracle = this.oracle;
      return var2;
   }

   private static Map<String, Object> help() {
      return JsApi.markdown(
         "# Report — the standalone coverage scorecard (read-only, deterministic)\n\nRenders the self-contained, `file://`-openable coverage page (`ext/coverage/pages/coverage.html`)\nstraight from the graph (`<reportDir>/ext/coverage/coverage-data.json`, written by a\n`karate run -f karate:jsonl` with `boot.ext('coverage')`) — **no re-run** to view it. The SAME\npage a real run's report carries as its Coverage tab (one shell, one layout). Alpine.js + the\ndata as a `<script>` window global; static assets are copied once, so a re-render only rewrites\nthe data.\n\n- `Report.generate([{reportDir?, out?}])` — (re)render the coverage report; returns `{file, url, items, sources}`.\n- `Report.traceability([{reportDir?, out?, repoUrl?}])` — the **enterprise traceability report** (K-RTM):\n  the confidence-to-ship scorecard + the Requirements Traceability Matrix (requirement ×\n  criticality · risk · status · posture · tests · acceptance) + the epic→feature→leaf hierarchy\n  tree + CSV export + a built-in glossary overlay. Pass `repoUrl` to deep-link covering tests to\n  their source (`<repoUrl>/<feature>#L<line>`). Returns `{file, url, ready, verdict}`.\n- `Report.aggregate([{requirements?, runs?, run?, exclude?, formal?, spec?, rules?, events?}])` — **rebuild the\n  graph run-free**: joins the requirements universe + every EXECUTED run's evidence under `runs/`\n  (each dir's `karate-json/karate-events.jsonl` EXERCISED stream + `transcript.jsonl`/anchor\n  ATTESTED hits, auto-discovered) + optionally an OpenAPI spec, a rules home, and any\n  extra `karate-events.jsonl` files into ONE graph, and writes\n  `<reportDir>/ext/coverage/coverage-data.json`. Bare call defaults to the project's\n  `requirements/` + `runs/` dirs — the explore-then-show-the-RTM loop needs no karate run.\n  Pass `run:'<id>'` to scope the evidence to ONE run (\"what did this run verify?\")\n  or `run:['a','b']` to a picked set — the scoped graph + RTM render under\n  `<reportDir>/rtm/<id>/` (set: `<reportDir>/rtm-selection/`), never next to the evidence,\n  so the project-wide graph stays intact; the result's `rtmDir` is the home — follow with\n  `Report.traceability({reportDir: <rtmDir>})`.\n  A dry run never folds in — it is inventory: `dryRunsExcluded` says how many were\n  left out, and `Requirement.matrix({run:'<id>'})` reads one. `exclude:['<id>']` sets a run\n  aside; `formal:true` keeps only runs stamped formal. Per test the latest terminal execution\n  decides; the graph's `selection` says which executions were chosen and why.\n  The project graph keeps the `requirements`/`spec`/`rules`/`events` given here for every later\n  project build (console runs, `Rule.cover`, an aggregate naming none), filling only what\n  karate-boot.js leaves undeclared; an aggregate naming any replaces them (`{spec:[]}` drops them).\n  Then `Requirement.matrix()` / `Report.traceability()` read the fresh graph.\n- `Report.gate([{spec?, lint?, diff?, reportDir?, pathPrefix?, output?}])` — the **pull-request\n  check payload** `{schema, name, conclusion, title, summary, annotations[], sources, readiness,\n  lint?, diff?, notannotated[]}` (GitHub's `conclusion`/`level` tokens verbatim). `conclusion` is\n  the worst of its sources: NOT_READY, an unwaived lint `error` or breaking change → `failure`;\n  CONDITIONAL, an unwaived `warn` or API warning → `neutral`; READY → `success`; an empty estate,\n  a partly-evaluated policy or an incomplete diff is capped at `neutral`; a source that failed →\n  `failure`, its envelope carried. `spec` adds the `Openapi.lint` source (`lint` forwarded,\n  `lint.output` refused); `diff: {baseline, current?, waivers?, asOf?}` the `Openapi.diff`\n  source, `current` defaulting to `spec`; `pathPrefix` re-roots `path`.\n- `Report.url([{out?, page?}])` — the `file://` URL of a page (`page:'traceability'` for the RTM).\n- `Report.path([{out?, page?}])` — the filesystem path of a page.\n\nDefaults: reads/writes the bound report dir, so bare calls work after a run.\nRelated: `Coverage.*`/`Requirement.*` (query the same graph)."
      );
   }

   public static Report atReportDir(Path var0) {
      return new Report(var0);
   }

   Map<String, Object> generate(Object var1) {
      Path var2 = this.reportDir(var1).resolve("ext/coverage/coverage-data.json");
      io.karatelabs.ext.common.jsapi.CoverageGraphAccessor.CoverageGraphLoadResult var3 = io.karatelabs.ext.common.jsapi.CoverageGraphAccessor.a(var2);
      var var4 = var3.c();
      if (var4 == null) {
         return JsApi.dataError(
            var3,
            var2,
            "no graph yet — in the console, run your suite (e.g. Runner.suite(...)) then Report.aggregate() to build it from runs/; on the CLI, `karate run <feature>` with boot.ext('coverage') active (the coverage ext auto-enables the karate-events.jsonl stream it computes from)"
         );
      } else {
         Object var5 = io.karatelabs.ext.coverage.dim.DimensionCoverageProjector.a(var4, null).get("items");
         if (var5 instanceof List var6 && !var6.isEmpty()) {
            var4.put("dimensions", var5);
         }

         if (io.karatelabs.ext.coverage.dim.CoveringArrayGenerator.a(var4, null, null).get("items") instanceof List var7) {
            ArrayList var8 = new ArrayList();

            for (Object var10 : var7) {
               if (var10 instanceof Map var11 && var11.get("rows") instanceof List) {
                  var8.add(var10);
               }
            }

            if (!var8.isEmpty()) {
               var4.put("covering", var8);
            }
         }

         io.karatelabs.ext.coverage.dim.DimensionCoverageProjector.a(var4);
         var4.put("assertionStrength", Requirement.assertionStrength(var4));
         var var14 = Requirement.readinessFor(var4);
         if (var14 != null) {
            var4.put("readiness", var14);
            if (Files.isRegularFile(this.outDir(var1).resolve("ext/traceability/pages/traceability.html"))) {
               var4.put("hasTraceability", true);
               var4.put("traceabilityHref", "../../traceability/pages/traceability.html");
            }
         }

         Path var15 = this.outDir(var1);

         try {
            ensureCoverageShell(var15);
            addBackLink(var4, var15);
            addContractLink(var4, var15);
            Files.writeString(
               var15.resolve("ext/coverage/coverage-data.js"), "window.KARATE_COVERAGE_DATA = " + Json.stringifyStrict(var4) + ";\n", StandardCharsets.UTF_8
            );
         } catch (IOException var12) {
            throw new UncheckedIOException(var12);
         }

         Path var16 = var15.resolve("ext/coverage/pages/coverage.html");
         LinkedHashMap var17 = new LinkedHashMap();
         var17.put("file", var16.toString());
         var17.put("url", var16.toUri().toString());
         var17.put("items", size(var4.get("items")));
         var17.put("sources", size(var4.get("sources")));
         return JsApi.next(var17, "Report.url()", "open the file in a browser");
      }
   }

   Map<String, Object> traceability(Object var1) {
      Path var2 = this.reportDir(var1).resolve("ext/coverage/coverage-data.json");
      io.karatelabs.ext.common.jsapi.CoverageGraphAccessor.CoverageGraphLoadResult var3 = io.karatelabs.ext.common.jsapi.CoverageGraphAccessor.a(var2);
      var var4 = var3.c();
      if (var4 == null) {
         return JsApi.dataError(
            var3,
            var2,
            "no graph yet — in the console, run your suite (e.g. Runner.suite(...)) then Report.aggregate() to build it from runs/; on the CLI, `karate run <feature>` with boot.ext('coverage') active (the coverage ext auto-enables the karate-events.jsonl stream it computes from)"
         );
      } else {
         Object var5 = Requirement.atReportDir(this.reportDir(var1)).traceabilityPayload();
         Map var6 = var5 == null ? null : asMap(((java.util.Map)var5).get("readiness"));
         Path var7 = this.outDir(var1);

         try {
            ensureTraceabilityShell(var7);
            if (var5 == null) {
               var5 = new LinkedHashMap();
               ((java.util.Map)var5).put("graph", var4);
            }

            ((java.util.Map)var5).put("coverageHref", "../../coverage/pages/coverage.html");
            String var8 = strOpt(var1, "repoUrl");
            if (var8 != null) {
               ((java.util.Map)var5).put("repoUrl", var8.endsWith("/") ? var8 : var8 + "/");
            }

            if ((var1 instanceof Map var10 ? var10.get("links") : null) instanceof Map var14 && !var14.isEmpty()) {
               ((java.util.Map)var5).put("links", var14);
            }

            addBackLink((Map<String, Object>)var5, var7);
            addContractLink((Map<String, Object>)var5, var7);
            linkFeatureHtml(asMap(((java.util.Map)var5).get("graph")), var7);
            io.karatelabs.ext.coverage.dim.DimensionCoverageProjector.a(asMap(((java.util.Map)var5).get("graph")));
            Files.writeString(
               var7.resolve("ext/traceability/traceability-data.js"), "window.KARATE_TRACE = " + Json.stringifyStrict(var5) + ";\n", StandardCharsets.UTF_8
            );
            if (!Files.isRegularFile(var7.resolve("ext/coverage/pages/coverage.html"))) {
               this.generate(var1);
            }
         } catch (IOException var11) {
            throw new UncheckedIOException(var11);
         }

         Path var12 = var7.resolve("ext/traceability/pages/traceability.html");
         LinkedHashMap var13 = new LinkedHashMap();
         var13.put("file", var12.toString());
         var13.put("url", var12.toUri().toString());
         if (var6 != null) {
            var13.put("ready", var6.get("ready"));
            var13.put("verdict", var6.get("verdict"));
         }

         var13.put("items", size(var4.get("items")));
         return JsApi.next(var13, "Report.url({page:'traceability'})", "open the file in a browser");
      }
   }

   public Map<String, Object> aggregate() {
      return this.aggregate(null);
   }

   public Map<String, Object> aggregateRunInPlace(String var1) {
      return this.aggregate(Map.of("run", var1, "reportDir", "runs/" + var1));
   }

   private static void writeRunProvenance(Path var0, Path var1) throws IOException {
      Path var2 = var1.resolve("meta.json");
      if (Files.isRegularFile(var2)) {
         if (Json.of(Files.readString(var2)).value() instanceof Map var4) {
            LinkedHashMap var5 = new LinkedHashMap();
            var5.put("runId", var1.getFileName().toString());
            var5.put("dryRun", Boolean.TRUE.equals(var4.get("dryRun")));
            Files.writeString(var0.resolve("ext/coverage/run.json"), Json.stringifyStrict(var5), StandardCharsets.UTF_8);
         }
      }
   }

   private static boolean isDryRun(Path var0) {
      return dryStamp(var0.resolve("meta.json")) || dryStamp(var0.resolve("karate-json/run.json"));
   }

   private static boolean dryStamp(Path var0) {
      return stamped(var0, "dryRun");
   }

   private static boolean isDraft(Path var0) {
      return stamped(var0.resolve("meta.json"), "draft") || stamped(var0.resolve("karate-json/run.json"), "draft");
   }

   private static boolean isFormal(Path var0) {
      return stamped(var0.resolve("meta.json"), "formal");
   }

   private static boolean hasRunRecord(Path var0) {
      return Files.isRegularFile(var0.resolve("meta.json")) || Files.isRegularFile(var0.resolve("karate-json/run.json"));
   }

   private static boolean stamped(Path var0, String var1) {
      if (!Files.isRegularFile(var0)) {
         return false;
      } else {
         try {
            return Json.of(Files.readString(var0)).value() instanceof Map var2 && Boolean.TRUE.equals(var2.get(var1));
         } catch (RuntimeException | IOException var4) {
            return false;
         }
      }
   }

   private static Map<String, Object> setAside(String var0, String var1) {
      LinkedHashMap var2 = new LinkedHashMap();
      var2.put("run", var0);
      var2.put("reason", var1);
      return var2;
   }

   Map<String, Object> aggregate(Object var1) {
      if (projectScope(var1) && namesSource(var1)) {
         Path var2 = this.reportDir(var1).resolve("ext/coverage/aggregate-sources.json");
         String var3 = recordSources(var2, var1);
         boolean var4 = false;

         Map var6;
         try {
            var var5 = this.aggregate(var1, null, null);
            var4 = !var5.containsKey("error");
            var6 = var5;
         } finally {
            if (!var4) {
               try {
                  writeRecord(var2, var3);
               } catch (IOException var13) {
                  logger.warn("could not restore {} after a failed aggregate: {}", var2, var13.toString());
               }
            }
         }

         return var6;
      } else {
         return this.aggregate(var1, null, null);
      }
   }

   private static boolean projectScope(Object var0) {
      Object var1 = var0 instanceof Map var2 ? var2.get("run") : null;
      return var1 instanceof List var3 ? var3.isEmpty() : !(var1 instanceof String var4 && !var4.isBlank());
   }

   private static boolean namesSource(Object var0) {
      return var0 instanceof Map var1 && SOURCE_OPTS.stream().anyMatch(var1::containsKey);
   }

   private static String recordSources(Path var0, Object var1) {
      LinkedHashMap var2 = new LinkedHashMap();

      for (String var4 : SOURCE_OPTS) {
         Object var5 = var1 instanceof Map var6 ? var6.get(var4) : null;
         if (!(var5 instanceof List var9 ? var9.isEmpty() : var5 == null || String.valueOf(var5).isBlank())) {
            var2.put(var4, var5);
         }
      }

      try {
         String var8 = Files.isRegularFile(var0) ? Files.readString(var0) : null;
         writeRecord(var0, var2.isEmpty() ? null : Json.stringifyStrict(var2));
         return var8;
      } catch (IOException var7) {
         throw new UncheckedIOException(var7);
      }
   }

   private static void writeRecord(Path var0, String var1) throws IOException {
      if (var1 == null) {
         Files.deleteIfExists(var0);
      } else {
         Files.createDirectories(var0.getParent());
         Files.writeString(var0, var1, StandardCharsets.UTF_8);
      }
   }

   private static Map<String, Object> recordedSources(Path var0, List<Map<String, Object>> var1) {
      Path var2 = var0.resolve("ext/coverage/aggregate-sources.json");
      if (!Files.isRegularFile(var2)) {
         return null;
      } else {
         Object var3;
         try {
            var3 = Json.of(Files.readString(var2)).value();
         } catch (RuntimeException | IOException var8) {
            var1.add(droppedSource("record", "ext/coverage/aggregate-sources.json", String.valueOf(var8.getMessage())));
            return null;
         }

         LinkedHashMap var4 = new LinkedHashMap();
         if (var3 instanceof Map var5) {
            for (String var7 : SOURCE_OPTS) {
               if (var5.get(var7) != null) {
                  var4.put(var7, var5.get(var7));
               }
            }
         }

         return var4.isEmpty() ? null : var4;
      }
   }

   private List<Path> addKeptSources(Map<String, Object> var1, List<io.karatelabs.ext.coverage.CoverageSource> var2, List<Resource> var3, List<Map<String, Object>> var4) {
      HashSet var5 = new HashSet();
      var2.forEach(var1x -> var5.add(var1x.f()));
      ArrayList var6 = new ArrayList();
      var var7 = this.keptPaths("requirements", var1.get("requirements"), var4);
      if (!var7.isEmpty()) {
         var6.add(new RequirementsCoverageSource(var7, null, null, this.projectRoot));
      }

      var var8 = this.keptPaths("rules", var1.get("rules"), var4);
      if (!var8.isEmpty()) {
         var6.add(new RulebookCoverageSource((Path)var8.get(0)));
      }

      for (io.karatelabs.ext.coverage.CoverageSource var10 : (Iterable<io.karatelabs.ext.coverage.CoverageSource>) (Iterable<?>) (var6)) {
         if (var5.add(var10.f())) {
            var2.add(var10);
         }
      }

      Object var14 = var1.get("spec");
      if (var14 != null) {
         try {
            var var15 = CoverageExt.openApiSources(var14, this.anchor());
            var var11 = CoverageExt.specPaths(var14, this.anchor(), this.anchor());

            for (int var12 = 0; var12 < var15.size(); var12++) {
               if (var5.add(((io.karatelabs.ext.coverage.CoverageSource)var15.get(var12)).f())) {
                  var2.add((io.karatelabs.ext.coverage.CoverageSource)var15.get(var12));
                  var3.add((Resource)var11.get(var12));
               }
            }
         } catch (RuntimeException var13) {
            var4.add(droppedSource("spec", var14, String.valueOf(var13.getMessage())));
         }
      }

      return this.keptPaths("events", var1.get("events"), var4);
   }

   // $VF: One or more variable merging failures!
   // $VF: Could not properly define all variable types!
   // Please report this to the Vineflower issue tracker, at https://github.com/Vineflower/vineflower/issues with a copy of the class file (if you have the rights to distribute it!)
   private List<Path> keptPaths(String var1, Object var2, List<Map<String, Object>> var3) {
      ArrayList var4 = new ArrayList();

      for (Object var9 : var2 instanceof List ? (List)var2 : var2 == null ? List.of() : List.of(var2)) {
         try {
            Path var7 = this.resolveAgainstRoot(String.valueOf(var9));
            if (Files.exists(var7)) {
               var4.add(var7);
            } else {
               var3.add(droppedSource(var1, var9, "not found"));
            }
         } catch (RuntimeException var8) {
            var3.add(droppedSource(var1, var9, String.valueOf(var8.getMessage())));
         }
      }

      return var4;
   }

   private static Map<String, Object> droppedSource(String var0, Object var1, String var2) {
      LinkedHashMap var3 = new LinkedHashMap();
      var3.put("source", var0);
      var3.put("ref", var1);
      var3.put("reason", var2);
      return var3;
   }

   public Map<String, Object> settle(String var1) {
      io.karatelabs.ext.max.jsapi.Report.BootCoverageConfig var2 = this.boot();
      this.aggregate(Map.of("run", var1, "reportDir", "runs/" + var1), var2, null);
      return this.aggregate(null, var2, null);
   }

   public io.karatelabs.ext.coverage.CoverageProjectBuilder.CoverageProjectBuildResult snapshot(Path var1) {
      Object var2 = this.resolve(null, null, var1, null);
      if (var2 instanceof Map var3) {
         throw new IllegalStateException(String.valueOf(var3.get("error")));
      } else {
         return io.karatelabs.ext.coverage.CoverageProjectBuilder.build(((io.karatelabs.ext.max.jsapi.Report.ReportBuildPlan)var2).a);
      }
   }

   public Map<String, Object> composeSide(Path var1) {
      Object var2 = this.resolve(null, null, null, List.of(var1));
      if (var2 instanceof Map var4) {
         return castError(var4);
      } else {
         io.karatelabs.ext.max.jsapi.Report.ReportBuildPlan var3 = (io.karatelabs.ext.max.jsapi.Report.ReportBuildPlan)var2;
         var3.a.a((io.karatelabs.ext.coverage.RulebookCoverageResult.RulebookCoverageProducer)this.producer());
         return io.karatelabs.ext.coverage.CoverageProjectBuilder.build(var3.a).b();
      }
   }

   public Map<String, Object> compose(List<Path> var1, List<String> var2) {
      if (var1 != null && !var1.isEmpty()) {
         Object var3 = this.resolve(null, null, null, var1);
         if (var3 instanceof Map var5) {
            return castError(var5);
         } else {
            io.karatelabs.ext.max.jsapi.Report.ReportBuildPlan var4 = (io.karatelabs.ext.max.jsapi.Report.ReportBuildPlan)var3;
            if (var2 != null && !var2.isEmpty()) {
               var4.a.a(var4.h.c(var2));
            }

            return io.karatelabs.ext.coverage.CoverageProjectBuilder.build(var4.a).b();
         }
      } else {
         return JsApi.missing("a composed scope names at least one run");
      }
   }

   private static Map<String, Object> castError(Map<?, ?> var0) {
      return (Map<String, Object>)(Map<?, ?>)var0;
   }

   public Report withOracle(Supplier<RulebookCoverageProvider> var1) {
      this.oracle = var1;
      return this;
   }

   private RulebookCoverageProvider producer() {
      return this.oracle == null ? null : this.oracle.get();
   }

   // $VF: One or more variable merging failures!
   // $VF: Could not properly define all variable types!
   // Please report this to the Vineflower issue tracker, at https://github.com/Vineflower/vineflower/issues with a copy of the class file (if you have the rights to distribute it!)
   private Object resolve(Object var1, io.karatelabs.ext.max.jsapi.Report.BootCoverageConfig var2, Path var3, List<Path> var4) {
      var var5 = this.optPaths(var1, "requirements", null);
      Object var6 = var1 instanceof Map var7 ? var7.get("run") : null;
      ArrayList<Path> var37 = new ArrayList<>(var4 == null ? List.of() : var4);
      Object var8 = var6 instanceof List var10 ? new ArrayList(var10) : (var6 instanceof String var9 && !var9.isBlank() ? List.of(var9) : List.of());

      for (Path var41 : (Iterable<Path>) (Iterable<?>) (var37)) {
         if (!Files.isDirectory(var41)) {
            return JsApi.missing("no run dir at " + var41);
         }
      }

      for (Object var42 : (Iterable<Object>)(Iterable<?>)(var8)) {
         String var11 = String.valueOf(var42);
         Path var12 = !var11.contains("/") && !var11.contains(java.io.File.separator)
            ? this.resolveAgainstRoot("runs/" + var11)
            : this.resolveAgainstRoot(var11);
         if (!Files.isDirectory(var12)) {
            return JsApi.missing("no run dir at " + var12 + " — pass a run id (under runs/) or a path");
         }

         var37.add(var12);
      }

      Path var40 = var37.size() == 1
         ? this.reportDir(var1).resolve("rtm").resolve(((Path)var37.get(0)).getFileName().toString())
         : this.reportDir(var1).resolve("rtm-selection");
      Path var43 = var3 != null ? var3 : (!var37.isEmpty() && strOpt(var1, "reportDir") == null ? var40 : this.reportDir(var1));
      long var44 = io.karatelabs.ext.coverage.CoverageSnapshotPublisher.nextSnapshotSequence(var43);
      ArrayList var13 = new ArrayList();
      Map var14 = var37.isEmpty() && !namesSource(var1) ? recordedSources(var43, var13) : null;
      Path var15 = !var37.isEmpty() ? null : this.optPath(var1, "runs", "runs");
      Object var16 = var1 instanceof Map var17 ? var17.get("spec") : null;
      Path var45 = this.optPath(var1, "rules", null);
      ArrayList<io.karatelabs.ext.coverage.CoverageSource> var18 = new ArrayList<>();
      ArrayList var19 = new ArrayList();
      if (!var5.isEmpty()) {
         var18.add(new RequirementsCoverageSource(var5, null, null, this.projectRoot));
      }

      if (var16 != null) {
         var18.addAll(CoverageExt.openApiSources(var16, this.anchor()));
         var19.addAll(CoverageExt.specPaths(var16, this.anchor(), this.anchor()));
      }

      if (var45 != null) {
         var18.add(new RulebookCoverageSource(var45));
      }

      Path[] var20 = new Path[2];
      Object[] var21 = new Object[1];
      Object[] var22 = new Object[1];
      this.addBootConfiguredSources(var2 != null ? var2 : this.boot(), var18, var19, var20, var21, var22);
      List var23 = var14 == null ? List.of() : this.addKeptSources(var14, var18, var19, var13);
      if (!var13.isEmpty()) {
         logger.warn("project graph built without kept source(s) that no longer resolve: {}", var13);
      }

      if (var5.isEmpty() && this.projectRoot != null && var18.stream().noneMatch(var0 -> "req".equals(var0.b()))) {
         Path var24 = this.projectRoot.resolve("requirements");
         if (Files.isDirectory(var24)) {
            var5 = List.of(var24);
            var18.add(new RequirementsCoverageSource(List.of(var24), null, null, this.projectRoot));
         }
      }

      if (var5.isEmpty() && var15 == null && var37.isEmpty() && var16 == null && var45 == null && var18.isEmpty()) {
         return JsApi.missing(
            "nothing to aggregate — pass {requirements, runs, run, spec, rules, events} paths, or run on a project with requirements/ + runs/ dirs (or a karate-boot.js that boots cov.openapi/grpc/kafka/asyncapi)"
         );
      } else {
         ArrayList var46 = new ArrayList(var37);
         if (var46.isEmpty() && var15 != null && Files.isDirectory(var15)) {
            try (var var25 = Files.list(var15)) {
               var25.filter(var0 -> Files.isDirectory(var0)).sorted().forEach(var46::add);
            } catch (IOException var36) {
               throw new UncheckedIOException(var36);
            }
         }

         if (var37.isEmpty()) {
            var46.add(this.reportDir(var1));
         }

         LinkedHashSet var47 = new LinkedHashSet();
         Object var26 = var1 instanceof Map var27 ? var27.get("exclude") : null;

         for (Object var50 : var26 instanceof List ? (List)var26 : List.of()) {
            var47.add(String.valueOf(var50));
         }

         boolean var49 = var1 instanceof Map var51 && Boolean.TRUE.equals(var51.get("formal"));
         ArrayList var52 = new ArrayList();
         ArrayList var29 = new ArrayList();
         int var30 = 0;
         Iterator var31 = var46.iterator();

         while (var31.hasNext()) {
            Path var32 = (Path)var31.next();
            String var33 = var32.getFileName().toString();
            if (var47.contains(var33)) {
               var31.remove();
               var52.add(setAside(var33, "exclude"));
            } else if (isDraft(var32)) {
               var31.remove();
               var52.add(setAside(var33, "draft"));
            } else if (var37.isEmpty() && isDryRun(var32)) {
               var31.remove();
               var30++;
            } else if (var49 && hasRunRecord(var32) && !isFormal(var32)) {
               var31.remove();
               var29.add(var33);
            }
         }

         io.karatelabs.ext.coverage.graph.ExecutionSelection.SelectionOptions var53 = var37.isEmpty()
            ? io.karatelabs.ext.coverage.graph.ExecutionSelection.SelectionOptions.a()
            : io.karatelabs.ext.coverage.graph.ExecutionSelection.SelectionOptions.a(var37.stream().map(var0 -> var0.getFileName().toString()).toList());
         io.karatelabs.ext.max.jsapi.Report.ReportBuildPlan var54 = new io.karatelabs.ext.max.jsapi.Report.ReportBuildPlan();
         var54.h = var53.b(var52).a(var49 ? "formal" : null, var29).a(var30);
         var54.b = var37;
         var54.c = var30;
         var54.d = var19;
         var54.e = var21[0];
         var54.f = var43;
         var54.g = var13;
         List var55 = this.eventFiles(var1, var46);
         var55.addAll(0, var23);
         if (!var13.isEmpty()) {
            var54.a.a(Map.of("droppedSources", var13));
         }

         var54.a
            .a(var18)
            .requirements(var22[0])
            .b(var55)
            .a(var46, false)
            .a(var54.h)
            .executionInventory(io.karatelabs.ext.coverage.collect.ExecutionInventory.a(this.projectRoot, var20[1]))
            .a(var19, var20[0], var20[1])
            .a((io.karatelabs.ext.coverage.RulebookCoverageResult.RulebookCoverageProducer)(var37.isEmpty() ? this.producer() : null))
            .reportDirectory(var43)
            .snapshotSequence(var44);
         return var54;
      }
   }

   private Map<String, Object> aggregate(Object var1, io.karatelabs.ext.max.jsapi.Report.BootCoverageConfig var2, Path var3) {
      Object var4 = this.resolve(var1, var2, var3, null);
      if (var4 instanceof Map var16) {
         return castError(var16);
      } else {
         io.karatelabs.ext.max.jsapi.Report.ReportBuildPlan var5 = (io.karatelabs.ext.max.jsapi.Report.ReportBuildPlan)var4;
         io.karatelabs.ext.coverage.CoverageProjectBuilder.CoverageProjectBuildResult var6 = io.karatelabs.ext.coverage.CoverageProjectBuilder.build(var5.a);
         var var7 = var6.b();
         var var8 = var5.b;
         Path var9 = var5.f;
         Path var10 = var9.resolve("ext/coverage/coverage-data.json");

         boolean var11;
         try {
            RequirementsExt.writeProviderLinks(var9, var5.e);
            var11 = var6.publish();
            ensureCoverageShell(var9);
            if (hasSource(var7, "req")) {
               ensureTraceabilityShell(var9);
            }

            if (var8.size() == 1) {
               writeRunProvenance(var9, (Path)var8.get(0));
            } else if (var8.isEmpty()) {
               Files.deleteIfExists(var9.resolve("ext/coverage/run.json"));
            }
         } catch (IOException var15) {
            throw new UncheckedIOException(var15);
         }

         LinkedHashMap var12 = new LinkedHashMap();
         var12.put("file", var10.toString());
         var12.put("sources", size(var7.get("sources")));
         var12.put("items", size(var7.get("items")));
         var12.put("tests", size(var7.get("tests")));
         var12.put("hits", size(var7.get("hits")));
         var12.put("attestedRuns", var6.e());
         var12.put("attestedHits", var6.f());
         if (!var11) {
            var12.put("superseded", true);
         }

         if (var5.c > 0) {
            var12.put("dryRunsExcluded", var5.c);
         }

         if (!var5.g.isEmpty()) {
            var12.put("droppedSources", var5.g);
         }

         if (!var6.g().isEmpty()) {
            var12.put("notattachedTokens", var6.g());
         }

         if (!var8.isEmpty()) {
            var var17 = var8.stream().map(var0 -> var0.getFileName().toString()).toList();
            var12.put("run", var17.size() == 1 ? var17.get(0) : var17);
            String var18 = this.asRef(var10.getParent().getParent().getParent());
            var12.put("rtmDir", var18);
            return JsApi.next(
               var12, "Report.traceability({reportDir:'" + var18.replace("\\", "\\\\").replace("'", "\\'") + "'})", "render the scoped RTM at its home"
            );
         } else {
            if (var5.d.isEmpty()) {
               Path var13 = findOpenApiSpec(this.anchor());
               if (var13 != null) {
                  String var14 = this.anchor().relativize(var13).toString();
                  var12.put(
                     "note",
                     var14
                        + " is present but not wired as a coverage source — in karate-boot.js add `const cov = boot.ext('coverage');` then `cov.openapi = '"
                        + var14
                        + "'` (a bare `cov.openapi = …` without the boot.ext line is a ReferenceError), or pass Report.aggregate({spec:'"
                        + var14
                        + "'}) to join spec ⋈ req ⋈ tests"
                  );
               }
            }

            return JsApi.next(var12, "Report.traceability()", "render the RTM over the fresh graph, or query it via Requirement.matrix()");
         }
      }
   }

   private static Path findOpenApiSpec(Path var0) {
      if (var0 == null) {
         return null;
      } else {
         for (String var4 : new String[]{"openapi.yaml", "openapi.yml", "openapi.json"}) {
            Path var5 = var0.resolve(var4);
            if (Files.isRegularFile(var5)) {
               return var5;
            }
         }

         return null;
      }
   }

   private List<Path> optPaths(Object var1, String var2, String var3) {
      if ((var1 instanceof Map var5 ? var5.get(var2) : null) instanceof List var10) {
         ArrayList var6 = new ArrayList();

         for (Object var8 : var10) {
            if (var8 != null && !String.valueOf(var8).isBlank()) {
               var6.add(MaxPaths.resolvePath(this.projectRoot, String.valueOf(var8)));
            }
         }

         return var6;
      } else {
         Path var9 = this.optPath(var1, var2, var3);
         return var9 == null ? List.of() : List.of(var9);
      }
   }

   private Path optPath(Object var1, String var2, String var3) {
      String var4 = strOpt(var1, var2);
      if (var4 != null) {
         return MaxPaths.resolvePath(this.projectRoot, var4);
      } else if (var3 != null && this.projectRoot != null) {
         Path var5 = this.projectRoot.resolve(var3);
         return Files.isDirectory(var5) ? var5 : null;
      } else {
         return null;
      }
   }

   private Path anchor() {
      return this.projectRoot != null ? this.projectRoot : this.defaultReportDir;
   }

   private List<Path> eventFiles(Object var1, List<Path> var2) {
      Object var3 = var1 instanceof Map var4 ? var4.get("events") : null;
      ArrayList var8 = new ArrayList();
      if (var3 instanceof List var5) {
         var5.forEach(var2x -> var8.add(this.resolveAgainstRoot(String.valueOf(var2x))));
      } else if (var3 instanceof String var6 && !var6.isBlank()) {
         var8.add(this.resolveAgainstRoot(var6));
      }

      for (Path var10 : var2) {
         Path var7 = MaxPaths.eventsFile(var10);
         if (Files.isRegularFile(var7)) {
            var8.add(var7);
         }
      }

      return var8;
   }

   private Path resolveAgainstRoot(String var1) {
      return MaxPaths.resolvePath(this.projectRoot, var1);
   }

   private io.karatelabs.ext.max.jsapi.Report.BootCoverageConfig boot() {
      if (this.projectRoot == null) {
         return NO_BOOT;
      } else {
         try {
            BootBinding var1 = BootLoader.bootOnly(this.projectRoot, null);
            if (var1 == null) {
               return NO_BOOT;
            } else {
               CoverageExt var2 = null;
               Object var3 = null;

               for (Ext var5 : var1.getExts()) {
                  if (var5 instanceof CoverageExt var6) {
                     var2 = var6;
                  }

                  if (var5 instanceof RequirementsExt var9) {
                     var3 = var9.provider();
                  }
               }

               Path var8 = this.anchor();
               if (var1.getClasspathDir() != null) {
                  var8 = this.anchor().resolve(var1.getClasspathDir()).normalize();
               }

               return new io.karatelabs.ext.max.jsapi.Report.BootCoverageConfig(var2, var8, var3);
            }
         } catch (Exception var7) {
            logger.debug("aggregate: boot-config coverage sources unavailable ({}) — explicit opts only", var7.toString());
            return NO_BOOT;
         }
      }
   }

   public List<OpenApiCoverageSource> openapiSources() {
      io.karatelabs.ext.max.jsapi.Report.BootCoverageConfig var1 = this.boot();
      if (var1.a() != null && var1.a().getOpenapi() != null) {
         try {
            io.karatelabs.ext.common.keycheck.Main.requireProduct("cov.openapi", "openapi");
            return CoverageExt.openApiSources(var1.a().getOpenapi(), this.anchor(), var1.b());
         } catch (Exception var3) {
            logger.debug("cov.openapi sources unavailable ({})", var3.toString());
            return List.of();
         }
      } else {
         return List.of();
      }
   }

   private void addBootConfiguredSources(io.karatelabs.ext.max.jsapi.Report.BootCoverageConfig var1, List<io.karatelabs.ext.coverage.CoverageSource> var2, List<Resource> var3, Path[] var4, Object[] var5, Object[] var6) {
      var5[0] = var1.c();
      CoverageExt var7 = var1.a();
      if (var7 != null) {
         try {
            LinkedHashSet var8 = new LinkedHashSet();

            for (io.karatelabs.ext.coverage.CoverageSource var10 : var2) {
               var8.add(var10.f());
            }

            for (io.karatelabs.ext.coverage.CoverageSource var13 : var7.buildConfiguredSources(this.anchor(), var1.b())) {
               if (var8.add(var13.f())) {
                  var2.add(var13);
               }
            }

            if (var3.isEmpty()) {
               var3.addAll(var7.configuredSpecPaths(this.anchor(), var1.b()));
            }

            var4[0] = var7.configuredDimensionsPath(this.projectRoot, this.projectRoot);
            var4[1] = var7.configuredRulesHome(this.projectRoot, this.projectRoot);
            var6[0] = var7.getMap();
         } catch (Exception var11) {
            logger.debug("aggregate: boot-config coverage sources unavailable ({}) — explicit opts only", var11.toString());
         }
      }
   }

   Map<String, Object> gate(Object var1) {
      Object var2 = var1 instanceof Map var3 ? var3.get("lint") : null;
      if (var2 instanceof Map var16 && var16.containsKey("output")) {
         return JsApi.missing(
            "Report.gate: lint.output is not supported — the gate consumes the data-shaped lint result. Use the gate's own output: Report.gate({output:'markdown'}) or {output:'both'}."
         );
      } else {
         Object var17 = var1 instanceof Map var4 ? var4.get("diff") : null;
         if (var17 != null && !(var17 instanceof Map)) {
            return JsApi.missing("Report.gate: diff is {baseline, current?, waivers?, asOf?, baselineReportDir?}");
         } else {
            if (var17 instanceof Map var18) {
               for (String var6 : List.of("output", "failOn", "reportDir")) {
                  if (var18.containsKey(var6)) {
                     return JsApi.missing(
                        "Report.gate: diff."
                           + var6
                           + " is not supported — the gate reads the data-shaped diff envelope at its own reportDir and maps it to the check itself; for a CI exit code call Openapi.diff(baseline, current, {failOn}) directly."
                     );
                  }
               }
            }

            Path var19 = this.reportDir(var1);
            Path var20 = var19.resolve("ext/coverage/coverage-data.json");
            io.karatelabs.ext.common.jsapi.CoverageGraphAccessor.CoverageGraphLoadResult var21 = io.karatelabs.ext.common.jsapi.CoverageGraphAccessor.a(var20);
            var var7 = var21.c();
            Map var9 = null;
            Map var8;
            if (var7 == null) {
               var8 = JsApi.dataError(var21, var20, "run the suite then Report.aggregate() to build it from runs/");
            } else {
               try {
                  var8 = Requirement.readinessData(var7);
                  var9 = Requirement.criteriaData(var7);
               } catch (RuntimeException var15) {
                  var8 = JsApi.error(var15);
               }
            }

            Map var10 = null;
            String var11 = strOpt(var1, "spec");
            if (var11 != null && !var11.isBlank()) {
               try {
                  var10 = asMap(new Openapi().withRoot(this.anchor(), this.classpathRoot()).withReportDir(var19).callLint(var11, var2));
               } catch (RuntimeException var14) {
                  var10 = JsApi.error(var14);
               }

               if (var10 == null) {
                  var10 = JsApi.missing("Openapi.lint('" + var11 + "') returned no result");
               }
            }

            Map var12 = var17 instanceof Map var13 ? this.diffSource(var13, var11, var19) : null;
            Map var22 = io.karatelabs.ext.max.jsapi.ConfidenceGateBuilder.a(var8, var10, var12, var7, var9, var1);
            return JsApi.applyOutputConvention(var22, () -> String.valueOf(var22.get("summary")), var1);
         }
      }
   }

   private Map<String, Object> diffSource(Map<?, ?> var1, String var2, Path var3) {
      Object var4 = var1.get("current") != null ? var1.get("current") : var2;
      if (var4 != null && !String.valueOf(var4).isBlank()) {
         LinkedHashMap var5 = new LinkedHashMap();
         var1.forEach((var1x, var2x) -> {
            if (!"baseline".equals(var1x) && !"current".equals(var1x)) {
               var5.put(String.valueOf(var1x), var2x);
            }
         });

         try {
            var var6 = asMap(new Openapi().withRoot(this.anchor(), this.classpathRoot()).withReportDir(var3).callDiff(var1.get("baseline"), var4, var5));
            return var6 != null ? var6 : JsApi.missing("Openapi.diff returned no result");
         } catch (RuntimeException var7) {
            return JsApi.error(var7);
         }
      } else {
         return JsApi.missing("Report.gate: diff needs a current side — diff.current, or the gate's spec");
      }
   }

   Map<String, Object> url(Object var1) {
      Path var2 = this.outDir(var1).resolve(pageName(var1));
      LinkedHashMap var3 = new LinkedHashMap();
      var3.put("url", var2.toUri().toString());
      var3.put("exists", Files.exists(var2));
      return (Map<String, Object>)(!Files.exists(var2) ? JsApi.hint(var3, "not generated yet — call Report.generate() (or Report.traceability()) first") : var3);
   }

   private static String pageName(Object var0) {
      return "traceability".equals(strOpt(var0, "page")) ? "ext/traceability/pages/traceability.html" : "ext/coverage/pages/coverage.html";
   }

   private Path classpathRoot() {
      try {
         BootBinding var1 = BootLoader.bootOnly(this.anchor(), null);
         String var2 = var1 == null ? null : var1.getClasspathDir();
         if (var2 != null) {
            return this.anchor().resolve(var2).normalize();
         }
      } catch (RuntimeException var3) {
         logger.debug("could not read boot.classpath for {}: {}", this.anchor(), var3.getMessage());
      }

      return this.anchor();
   }

   private String asRef(Path var1) {
      Path var2 = var1.toAbsolutePath().normalize();
      return this.projectRoot != null && var2.startsWith(this.projectRoot.toAbsolutePath().normalize())
         ? "/" + this.projectRoot.toAbsolutePath().normalize().relativize(var2).toString().replace('\\', '/')
         : var2.toString();
   }

   private Path reportDir(Object var1) {
      String var2 = strOpt(var1, "reportDir");
      return var2 != null ? this.resolveAgainstRoot(var2) : this.defaultReportDir;
   }

   private Path outDir(Object var1) {
      String var2 = strOpt(var1, "out");
      return var2 != null ? this.resolveAgainstRoot(var2) : this.reportDir(var1);
   }

   private static void addBackLink(Map<String, Object> var0, Path var1) {
      if (var0 != null && Files.exists(var1.resolve("karate-summary.html"))) {
         var0.put("karateSummary", "../../../karate-summary.html");
      }
   }

   private static void addContractLink(Map<String, Object> var0, Path var1) {
      if (var0 != null && Files.exists(var1.resolve("ext/contract/pages/contract.html"))) {
         var0.put("contractHref", "../../../ext/contract/pages/contract.html");
      }
   }

   private static void linkFeatureHtml(Map<String, Object> var0, Path var1) {
      Path var2 = var1.resolve("feature-html");
      if (var0 != null && Files.isDirectory(var2)) {
         Object var3 = var0.get("tests");
         if (var3 instanceof List) {
            for (Object var6 : (List)var3) {
               if (var6 instanceof Map var7 && var7.get("feature") instanceof String var8 && !var8.isBlank()) {
                  String var11 = var8.replace(".feature", "").replace("/", ".").replace("\\", ".").replaceAll("[^a-zA-Z0-9_.-]", "_").toLowerCase();
                  if (Files.exists(var2.resolve(var11 + ".html"))) {
                     var7.put("featureHtml", "../../../feature-html/" + var11 + ".html");
                  }
               }
            }
         }
      }
   }

   static void ensureCoverageShell(Path var0) throws IOException {
      Path var1 = var0.resolve("ext").resolve("coverage");
      Files.createDirectories(var1.resolve("pages"));
      copyFresh("META-INF/karate-ext/static/coverage.css", var1.resolve("coverage.css"));
      copyFresh("META-INF/karate-ext/static/coverage-report.css", var1.resolve("coverage-report.css"));
      copyFresh("META-INF/karate-ext/static/coverage-report.js", var1.resolve("coverage-report.js"));
      copyFresh("META-INF/karate-ext/static/help-affordance.js", var1.resolve("help-affordance.js"));
      copyFresh("META-INF/karate-ext/pages/coverage.html", var0.resolve("ext/coverage/pages/coverage.html"));
      copySharedRes(var0);
   }

   static void ensureTraceabilityShell(Path var0) throws IOException {
      Path var1 = var0.resolve("ext").resolve("traceability");
      Files.createDirectories(var1.resolve("pages"));
      copyFresh("META-INF/karate-ext/static/coverage.css", var1.resolve("coverage.css"));
      copyFresh("META-INF/karate-ext/static/traceability-report.css", var1.resolve("traceability-report.css"));
      copyFresh("META-INF/karate-ext/static/traceability-report.js", var1.resolve("traceability-report.js"));
      copyFresh("META-INF/karate-ext/static/help-affordance.js", var1.resolve("help-affordance.js"));
      copyFresh("META-INF/karate-ext/pages/traceability.html", var0.resolve("ext/traceability/pages/traceability.html"));
      copySharedRes(var0);
   }

   private static boolean hasSource(Map<String, Object> var0, String var1) {
      return var0.get("sources") instanceof List var2 && var2.stream().anyMatch(var1x -> var1x instanceof Map var2x && var1.equals(var2x.get("type")));
   }

   private static void copyFresh(String var0, Path var1) throws IOException {
      io.karatelabs.ext.common.ReportAssetsInstaller.b(var0, var1);
   }

   private static void copySharedRes(Path var0) throws IOException {
      io.karatelabs.ext.common.ReportAssetsInstaller.a(var0);
   }

   private static String strOpt(Object var0, String var1) {
      Object var2 = var0 instanceof Map var3 ? var3.get(var1) : null;
      return var2 == null ? null : String.valueOf(var2);
   }

   private static int size(Object var0) {
      return var0 instanceof List var1 ? var1.size() : 0;
   }

   private static Map<String, Object> asMap(Object var0) {
      return var0 instanceof Map var1 ? var1 : null;
   }

   private record BootCoverageConfig(CoverageExt a, Path b, Object c) {
   }

   private static final class ReportBuildPlan {
      final io.karatelabs.ext.coverage.CoverageProjectBuilder.CoverageProjectOptions a = new io.karatelabs.ext.coverage.CoverageProjectBuilder.CoverageProjectOptions();
      List<Path> b = List.of();
      int c;
      List<Resource> d = List.of();
      Object e;
      Path f;
      List<Map<String, Object>> g = List.of();
      io.karatelabs.ext.coverage.graph.ExecutionSelection.SelectionOptions h;
   }
}
