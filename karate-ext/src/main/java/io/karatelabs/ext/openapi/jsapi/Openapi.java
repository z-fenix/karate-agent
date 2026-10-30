package io.karatelabs.ext.openapi.jsapi;

import io.karatelabs.common.Json;
import io.karatelabs.common.Resource;
import io.karatelabs.core.ReportAssets;
import io.karatelabs.ext.diff.DiffReport;
import io.karatelabs.ext.common.MaxPaths;
import io.karatelabs.ext.common.ReportAssetsInstaller;
import io.karatelabs.ext.common.jsapi.JsApi;
import io.karatelabs.ext.common.jsapi.JsDispatch;
import io.karatelabs.ext.coverage.Coverage;
import io.karatelabs.ext.lint.ReadOnlyCoverage;
import io.karatelabs.ext.lint.GovernanceNarrativeBuilder;
import io.karatelabs.ext.lint.LintEngine;
import io.karatelabs.ext.lint.LintException;
import io.karatelabs.ext.lint.LintMarkdownRenderer;
import io.karatelabs.ext.lint.ProjectLintConfigLoader;
import io.karatelabs.ext.lint.RulePackLoader;
import io.karatelabs.ext.lint.GovernanceScorer;
import io.karatelabs.ext.lint.GovernanceGradeMarkdownRenderer;
import io.karatelabs.ext.lint.WaiverMatcher;
import io.karatelabs.ext.openapi.OpenApiDocumentQueries;
import io.karatelabs.ext.openapi.OpenApiSpecSlicer;
import io.karatelabs.ext.openapi.SchemaMatchConverter;
import io.karatelabs.ext.openapi.OpenApiOperationCatalog;
import io.karatelabs.ext.openapi.DerivedRequirementsRenderer;
import io.karatelabs.ext.openapi.OpenApiSchemaNode;
import io.karatelabs.ext.openapi.OpenApiDocument;
import io.karatelabs.ext.openapi.OpenApiDocumentView;
import io.karatelabs.ext.openapi.changes.OasdiffCrosswalk;
import io.karatelabs.ext.openapi.changes.OpenApiDiffException;
import io.karatelabs.ext.pair.PairEvidenceRepository;
import io.karatelabs.ext.pair.PairReportAssembler;
import io.karatelabs.ext.rules.twin.Twin;
import io.karatelabs.ext.scaffold.ContractScaffold;
import io.karatelabs.ext.scaffold.ScaffoldContext;
import io.karatelabs.js.Terms;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.DumperOptions.FlowStyle;

public class Openapi extends JsDispatch {
   private Path root;
   private Path classpathRoot;
   private boolean noProject;
   private Path reportDir;
   private Path rulesHome;
   private Map<String, ReportAssets> nav;
   private static final String GOVERNANCE_PAGE = "ext/governance/pages/governance.html";
   private static final String GOVERNANCE_DATA = "ext/governance/governance-data.js";
   private static final String GOVERNANCE_BADGE = "ext/governance/pages/governance-badge.svg";
   private static final Set<String> DIFF_OPTIONS = Set.of("asOf", "output", "waivers", "reportDir", "baselineReportDir", "failOn");
   private static final String DIFF_USAGE = "Openapi.diff(baseline, current[, {asOf, output, waivers, reportDir, baselineReportDir, failOn}]) — two spec paths, e.g. Openapi.diff('api/v1.yaml', 'api/v2.yaml')";
   private static final String CROSSWALK_USAGE = "Openapi.crosswalk([checkId | {disposition, level}]) — an oasdiff check id, e.g. Openapi.crosswalk('request-property-removed'); disposition is detected · notEvaluated · declined, level error · warning · info";
   private static final String DOCS_USAGE = "Openapi.docs(spec, {twin?, walk?}) — twin names the rulebook whose model joins the spec's operations; walk is a Twin.explore plan";

   public Openapi() {
      this.fn("summary", var1 -> this.withModel(JsApi.arg(var1, 0), "summary", this::summaryOf));
      this.fn("operations", var1 -> this.withModel(JsApi.arg(var1, 0), "operations", this::operationsOf));
      this.fn("resources", var1 -> this.withModel(JsApi.arg(var1, 0), "resources", this::resourcesOf));
      this.fn("subset", var1 -> this.withModel(JsApi.arg(var1, 0), "subset", var2 -> this.subsetOf(var2, JsApi.arg(var1, 1))));
      this.fn("partitions", var1 -> this.withModel(JsApi.arg(var1, 0), "partitions", var2 -> this.partitionsOf(var2, JsApi.arg(var1, 1), JsApi.arg(var1, 2))));
      this.fn(
         "matchSchema", var1 -> this.withModel(JsApi.arg(var1, 0), "matchSchema", var2 -> this.matchSchemaOf(var2, JsApi.arg(var1, 1), JsApi.arg(var1, 2)))
      );
      this.fn("docs", var1 -> this.docs(JsApi.arg(var1, 0), JsApi.arg(var1, 1)));
      this.fn("lint", var1 -> this.lint(JsApi.arg(var1, 0), JsApi.arg(var1, 1)));
      this.fn("grade", var1 -> this.grade(JsApi.arg(var1, 0), JsApi.arg(var1, 1)));
      this.fn("diff", var1 -> this.diff(JsApi.arg(var1, 0), JsApi.arg(var1, 1), JsApi.arg(var1, 2)));
      this.fn("crosswalk", var0 -> crosswalk(JsApi.arg(var0, 0)));
      this.fn("report", var1 -> this.report(JsApi.arg(var1, 0), JsApi.arg(var1, 1)));
      this.fn("deriveRequirements", var1 -> this.deriveRequirements(JsApi.arg(var1, 0)));
      this.fn("scaffold", var1 -> this.scaffold(JsApi.arg(var1, 0), JsApi.arg(var1, 1)));
      this.fn("help", var0 -> JsApi.helpTopic(help(), var0));
   }

   private static void lossy(Map<String, Object> var0, OpenApiSchemaNode var1) {
      var var2 = io.karatelabs.ext.openapi.SchemaMatchConverter.unsupportedMarkerDiagnostics(var1);
      if (!var2.isEmpty()) {
         var0.put("lossy", var2);
      }
   }

   private Object matchSchemaOf(OpenApiOperationCatalog var1, Object var2, Object var3) {
      if (var2 instanceof String var4 && !var4.isBlank()) {
         OpenApiDocument var5 = var1.document();
         String var6 = var3 == null ? null : String.valueOf(var3 instanceof Number var7 ? var7.intValue() : var3);
         LinkedHashMap<java.lang.String, java.lang.Object> var10 = new LinkedHashMap();
         var10.put("ref", var4);
         OpenApiSchemaNode var8 = OpenApiDocumentQueries.a(var5, var4);
         if (var8 != null) {
            var10.put("kind", "component");
            var10.put("schema", io.karatelabs.ext.openapi.SchemaMatchConverter.toMatchTemplate(var8));
            lossy(var10, var8);
         } else {
            if (OpenApiDocumentQueries.b(var5, var4) == null) {
               return JsApi.missing("'" + var4 + "' matches no component or operation — components: " + OpenApiDocumentQueries.b(var5) + "; operations: " + OpenApiDocumentQueries.c(var5));
            }

            OpenApiSchemaNode var9 = OpenApiDocumentQueries.a(var5, var4, var6);
            if (var9 == null) {
               return JsApi.missing(
                  "operation '"
                     + var4
                     + "' declares no response schema"
                     + (var6 != null ? " for status " + var6 : "")
                     + " — nothing to lock (try a component name, or another status)"
               );
            }

            var10.put("kind", "response");
            if (var6 != null) {
               var10.put("status", var6);
            }

            var10.put("schema", io.karatelabs.ext.openapi.SchemaMatchConverter.toMatchTemplate(var9));
            lossy(var10, var9);
         }

         String var11 = "lock the response to the contract's shape: in a feature, `match response == <schema>` (via the `spec.schema('"
            + var4
            + "')` suite global); in a .js check, `match(response, schema)`. Required fields are #typed, optional ##typed.";
         if (var10.containsKey("lossy")) {
            var11 = var11
               + " NOTE: `lossy` lists what this template does NOT check — the # vocabulary cannot carry combinators or bounds. For real JSON-Schema semantics use `match.jsonSchema(actual, schema)`.";
         }

         return JsApi.hint(var10, var11);
      } else {
         return JsApi.missing(
            "Openapi.matchSchema(spec, ref[, status]) needs a component name or an operationId, e.g. Openapi.matchSchema('api.yaml', 'User') or Openapi.matchSchema('api.yaml', 'createUser', 400)"
         );
      }
   }

   private Object lint(Object var1, Object var2) {
      Object var3 = this.run("lint", var1, var2);
      return var3 instanceof Map var4 && !var4.containsKey("error") ? JsApi.applyOutputConvention(var4, () -> io.karatelabs.ext.lint.LintMarkdownRenderer.render(var4), var2) : var3;
   }

   private Object grade(Object var1, Object var2) {
      Object var3 = this.run("grade", var1, var2);
      if (var3 instanceof Map var4 && !var4.containsKey("error")) {
         Map<java.lang.String, Object> var5 = var4;
         LinkedHashMap<java.lang.String, java.lang.Object> var6 = new LinkedHashMap();
         var6.put("spec", var4.get("spec"));

         try {
            var6.putAll(io.karatelabs.ext.lint.GovernanceScorer.score(var5, var2 instanceof Map var7 ? var7.get("weights") : null, this.contractEvidence(String.valueOf(var5.get("spec")), var2)));
         } catch (LintException var9) {
            return JsApi.missing("Openapi.grade: " + var9.getMessage());
         }

         var6.put("lint", var4);
         LinkedHashMap<java.lang.String, java.lang.Object> var10 = new LinkedHashMap();
         var10.put("headline", io.karatelabs.ext.lint.GovernanceNarrativeBuilder.buildHeadline(var6));
         var var8 = io.karatelabs.ext.lint.GovernanceNarrativeBuilder.buildActions(var6);
         if (!var8.isEmpty()) {
            var10.put("actions", var8);
         }

         var10.putAll(var6);
         return JsApi.applyOutputConvention(var10, () -> io.karatelabs.ext.lint.GovernanceGradeMarkdownRenderer.render(var10), var2);
      } else {
         return var3;
      }
   }

   public Openapi withNav(Map<String, ReportAssets> var1) {
      this.nav = var1;
      return this;
   }

   public Object callReport(Object var1, Object var2) {
      return this.report(var1, var2);
   }

   public Object callLint(Object var1, Object var2) {
      return this.run("lint", var1, var2);
   }

   private static Object gradeOpts(Object var0) {
      if (var0 instanceof Map var1 && var1.containsKey("output")) {
         LinkedHashMap<java.lang.String, Object> var2 = new LinkedHashMap(var1);
         var2.remove("output");
         return var2;
      } else {
         return var0;
      }
   }

   private Object report(Object var1, Object var2) {
      Object var3 = this.grade(var1, gradeOpts(var2));
      if (var3 instanceof Map var4 && var4.get("error") == null) {
         Map var5 = var4;
         Path var6 = this.root != null ? this.root : Path.of(".");
         Object var7 = var2 instanceof Map var8 ? var8.get("out") : null;
         String var15 = var7 == null ? null : String.valueOf(var7);
         Path var9 = var15 != null ? var6.resolve(var15) : (this.reportDir != null ? this.reportDir : var6.resolve("target/karate-reports"));

         try {
            Path var10 = var9.resolve("ext").resolve("governance");
            Files.createDirectories(var10.resolve("pages"));
            io.karatelabs.ext.common.ReportAssetsInstaller.b("META-INF/karate-ext/static/coverage.css", var10.resolve("coverage.css"));
            io.karatelabs.ext.common.ReportAssetsInstaller.b("META-INF/karate-ext/static/governance-report.css", var10.resolve("governance-report.css"));
            io.karatelabs.ext.common.ReportAssetsInstaller.b("META-INF/karate-ext/static/governance-report.js", var10.resolve("governance-report.js"));
            io.karatelabs.ext.common.ReportAssetsInstaller.b("META-INF/karate-ext/static/help-affordance.js", var10.resolve("help-affordance.js"));
            io.karatelabs.ext.common.ReportAssetsInstaller.b("META-INF/karate-ext/pages/governance.html", var9.resolve("ext/governance/pages/governance.html"));
            io.karatelabs.ext.common.ReportAssetsInstaller.a(var9);
            LinkedHashMap var11 = new LinkedHashMap(var5);
            if (this.nav != null) {
               io.karatelabs.ext.common.ReportAssetsInstaller.a(var11, this.nav);
            } else {
               io.karatelabs.ext.common.ReportAssetsInstaller.a(var11, var9);
            }

            io.karatelabs.ext.common.ReportAssetsInstaller.a(var9.resolve("ext/governance/governance-data.js"), "KARATE_GOVERNANCE", var11);
            Files.writeString(var9.resolve("ext/governance/pages/governance-badge.svg"), io.karatelabs.ext.lint.GovernanceBadgeRenderer.render(var5));
         } catch (IOException var14) {
            throw new UncheckedIOException(var14);
         }

         Path var16 = var9.resolve("ext/governance/pages/governance.html");
         LinkedHashMap<java.lang.String, java.lang.Object> var17 = new LinkedHashMap();
         var17.put("file", var16.toString());
         var17.put("url", var16.toUri().toString());
         var17.put("badge", var9.resolve("ext/governance/pages/governance-badge.svg").toString());
         var17.put("verdict", var4.get("verdict"));
         if (var4.get("headline") instanceof Map var12) {
            var17.put("headline", var12.get("word"));
         }

         return var17;
      } else {
         return var3;
      }
   }

   private Map<String, Object> contractEvidence(String var1, Object var2) {
      Path var3 = this.root != null ? this.root : Path.of(".");
      Path var4 = io.karatelabs.ext.pair.PairEvidenceRepository.findLatestBySpec(var3, var1);
      if (var4 == null) {
         Path var14 = io.karatelabs.ext.pair.PairEvidenceRepository.latestArtifact(var3);
         if (var14 == null) {
            return null;
         } else {
            LinkedHashMap<java.lang.String, java.lang.Object> var15 = new LinkedHashMap();
            var15.put("measured", false);
            Object var17 = null;

            try {
               var17 = io.karatelabs.ext.pair.PairEvidenceRepository.specInput(io.karatelabs.ext.pair.PairEvidenceRepository.readEvidence(var14));
            } catch (RuntimeException var11) {
               var15.put(
                  "note",
                  "a pair is on file ("
                     + var3.relativize(var14.toAbsolutePath())
                     + ") but it could not be read ("
                     + var11.getMessage()
                     + "). It is a committed file, so repair it from git history rather than patching it — or mint a fresh one with Contract.pair({suite, spec:'"
                     + var1
                     + "', provider})."
               );
               return var15;
            }

            var15.put(
               "note",
               var17 == null
                  ? "a pair is on file ("
                     + var3.relativize(var14.toAbsolutePath())
                     + ") but it was minted without a `spec`, so it is bound to no document and cannot be graded here. Re-mint it for this spec: Contract.pair({suite, spec:'"
                     + var1
                     + "', provider})."
                  : "a pair is on file ("
                     + var3.relativize(var14.toAbsolutePath())
                     + ") but it was minted against `"
                     + var17
                     + "`, not this document — so it is evidence about a different subject and is NOT graded here. Mint one for this spec: Contract.pair({suite, spec:'"
                     + var1
                     + "', provider})."
            );
            return var15;
         }
      } else {
         Map var5;
         try {
            var5 = io.karatelabs.ext.pair.PairEvidenceRepository.readEvidence(var4);
         } catch (RuntimeException var12) {
            return null;
         }

         Instant var6;
         try {
            var6 = var2 instanceof Map var7 && var7.get("asOf") != null ? Instant.parse(String.valueOf(var7.get("asOf"))) : Instant.now();
         } catch (RuntimeException var13) {
            throw new LintException(
               "`asOf` must be an ISO-8601 instant, e.g. '2026-09-01T00:00:00Z' — it is the EXPLICIT evaluation instant that makes a time-sensitive verdict deterministic instead of a hidden dependency on the clock"
            );
         }

         Map<java.lang.String, Object> var16 = io.karatelabs.ext.pair.PairEvidenceRepository.freshness(var5, io.karatelabs.ext.pair.PairEvidenceRepository.currentDigests(var3, var5), var6);
         Map<java.lang.String, java.lang.Number> var8 = io.karatelabs.ext.pair.PairReportAssembler.applyFreshness(var5, var16);
         LinkedHashMap<java.lang.String, java.lang.Object> var9 = new LinkedHashMap();
         var9.put("pair", var3.relativize(var4.toAbsolutePath()).toString());
         var9.put("freshness", var16.get("status"));
         if (var16.get("unchecked") != null) {
            var9.put("unchecked", var16.get("unchecked"));
         }

         if (var16.get("changed") != null) {
            var9.put("changed", var16.get("changed"));
         }

         var9.put("claimSentence", var5.get("claimSentence"));
         if (var8 != null && var8.get("rung") != null) {
            var9.put("measured", true);
            var9.put("rung", var8.get("rung"));
            var9.put("name", var8.get("name"));
            var9.put("means", var8.get("means"));
            Object var10 = var5.get("rehearsal");
            if (var10 != null) {
               var9.put("rehearsal", true);
               var9.put("rehearsalNote", String.valueOf(var10));
            } else if (var5.get("rehearsalCheck") != null) {
               var9.put("rehearsalCheck", var5.get("rehearsalCheck"));
            }

            var9.put("score", ((Number)var8.get("rung")).doubleValue() / 4.0);
            if (var8.get("waivedBars") != null) {
               var9.put("waivedBars", var8.get("waivedBars"));
               var9.put("restsOnWaiver", var8.get("restsOnWaiver"));
            }

            if (var8.get("cappedBy") != null) {
               var9.put("cappedBy", var8.get("cappedBy"));
            }

            if (var8.get("blockedBy") != null) {
               var9.put("blockedBy", var8.get("blockedBy"));
            }

            return var9;
         } else {
            var9.put("measured", false);
            var9.put(
               "note",
               "the pair on file ("
                  + var9.get("pair")
                  + ") holds no rung"
                  + (var8 == null ? " — it predates the graded ladder, so re-mint it to grade this dimension" : ": " + var8.get("note"))
            );
            return var9;
         }
      }
   }

   public Object callDiff(Object var1, Object var2, Object var3) {
      return this.diff(var1, var2, var3);
   }

   private Object diff(Object var1, Object var2, Object var3) {
      io.karatelabs.ext.common.keycheck.Main.requireProduct("openapi.diff", "openapi");
      Map<java.lang.String, Object> var4 = var3 instanceof Map var5 ? var5 : Map.of();
      String var8 = var4.get("failOn") == null ? null : String.valueOf(var4.get("failOn"));
      var var6 = this.compare(var1, var2, var4, var8);
      if (var6.get("error") != null) {
         if (var8 != null) {
            var6.put("gate", io.karatelabs.ext.diff.DiffReport.b(var8));
         }

         return var6;
      } else {
         if (var8 != null) {
            var6.put("gate", io.karatelabs.ext.diff.DiffReport.a(var8, var6));
         }

         if ("changelog".equals(var4.get("output"))) {
            return io.karatelabs.ext.openapi.changes.ApiChangelog.a(var6);
         } else {
            Map<java.lang.String, java.lang.Object> var7 = JsApi.applyOutputConvention(var6, () -> io.karatelabs.ext.openapi.changes.OpenApiDiffer.a(var6), var3);
            if (var8 != null) {
               var7.putIfAbsent("gate", var6.get("gate"));
            }

            return var7;
         }
      }
   }

   private Map<String, Object> compare(Object var1, Object var2, Map<?, ?> var3, String var4) {
      if (!(var1 instanceof String var5 && !var5.isBlank() && var2 instanceof String var6 && !var6.isBlank())) {
         return JsApi.missing(
            "Openapi.diff(baseline, current[, {asOf, output, waivers, reportDir, baselineReportDir, failOn}]) — two spec paths, e.g. Openapi.diff('api/v1.yaml', 'api/v2.yaml')"
         );
      } else if (var4 != null && !io.karatelabs.ext.diff.DiffReport.b.contains(var4)) {
         return JsApi.missing("failOn is 'breaking', 'warning' or 'compatible', not '" + var4 + "'");
      } else {
         for (Object var8 : var3.keySet()) {
            if (!DIFF_OPTIONS.contains(String.valueOf(var8))) {
               return JsApi.missing(
                  "'"
                     + var8
                     + "' is not an option — Openapi.diff(baseline, current[, {asOf, output, waivers, reportDir, baselineReportDir, failOn}]) — two spec paths, e.g. Openapi.diff('api/v1.yaml', 'api/v2.yaml')"
               );
            }
         }

         io.karatelabs.ext.openapi.changes.OpenApiDiffer.OpenApiDiffOptions var16;
         try {
            var16 = io.karatelabs.ext.openapi.changes.OpenApiDiffer.OpenApiDiffOptions.a(var3);
         } catch (IllegalArgumentException var15) {
            return JsApi.missing("Openapi.diff: " + var15.getMessage());
         }

         Object var18 = io.karatelabs.ext.lint.ProjectLintConfigLoader.load(this.root).d();
         var16 = var16.a(this.root, var16.b() != null ? var16.b() : this.defaultReportDir(), this.boundNamespace(var18, var5), this.boundNamespace(var18, var6));

         Resource var9;
         try {
            var9 = MaxPaths.resolveResource(this.root, this.classpathRoot, var5);
         } catch (RuntimeException var14) {
            return JsApi.missing("Openapi.diff could not read baseline '" + var5 + "': " + var14.getMessage() + this.didYouMean(var5));
         }

         Resource var10;
         try {
            var10 = MaxPaths.resolveResource(this.root, this.classpathRoot, var6);
         } catch (RuntimeException var13) {
            return JsApi.missing("Openapi.diff could not read current '" + var6 + "': " + var13.getMessage() + this.didYouMean(var6));
         }

         try {
            return io.karatelabs.ext.openapi.changes.OpenApiDiffer.a(var5, var9, var6, var10, var16);
         } catch (OpenApiDiffException var12) {
            return JsApi.error(var12);
         }
      }
   }

   private String boundNamespace(Object var1, String var2) {
      if (var1 != null && this.root != null) {
         try {
            Path var3 = MaxPaths.resolvePath(this.root, var2).toAbsolutePath().normalize();

            for (io.karatelabs.ext.coverage.OpenApiSpecDescriptor var5 : (Iterable<io.karatelabs.ext.coverage.OpenApiSpecDescriptor>) (Iterable<?>) (io.karatelabs.ext.coverage.OpenApiSpecDescriptor.parse(var1))) {
               try {
                  if (MaxPaths.resolvePath(this.root, var5.b()).toAbsolutePath().normalize().equals(var3)) {
                     return var5.a() == null ? "openapi" : "openapi." + var5.a();
                  }
               } catch (RuntimeException var7) {
               }
            }

            return null;
         } catch (RuntimeException var8) {
            return null;
         }
      } else {
         return null;
      }
   }

   private String defaultReportDir() {
      Path var1 = this.reportDir != null ? this.reportDir : (this.root != null ? this.root.resolve("target/karate-reports") : null);
      if (var1 != null && Files.isDirectory(var1)) {
         Path var2 = var1.toAbsolutePath().normalize();
         Path var3 = this.root == null ? null : this.root.toAbsolutePath().normalize();
         return (var3 != null && var2.startsWith(var3) ? var3.relativize(var2) : var2).toString();
      } else {
         return null;
      }
   }

   private static Object crosswalk(Object var0) {
      var var1 = io.karatelabs.ext.openapi.changes.OasdiffCrosswalk.c();
      if (var0 instanceof String var6) {
         Map var9 = (Map)var1.get(var6);
         if (var9 == null) {
            return JsApi.missing(
               "'"
                  + var6
                  + "' is not an oasdiff 1.32.1 check — Openapi.crosswalk([checkId | {disposition, level}]) — an oasdiff check id, e.g. Openapi.crosswalk('request-property-removed'); disposition is detected · notEvaluated · declined, level error · warning · info"
            );
         } else {
            LinkedHashMap<java.lang.String, java.lang.String> var11 = new LinkedHashMap();
            var11.put("id", var6);
            var11.putAll((Map)copy(var9));
            return var11;
         }
      } else if (var0 instanceof Map var5) {
         for (Object var10 : var5.keySet()) {
            if (!"disposition".equals(var10) && !"level".equals(var10)) {
               return JsApi.missing(
                  "'"
                     + var10
                     + "' is not a filter — Openapi.crosswalk([checkId | {disposition, level}]) — an oasdiff check id, e.g. Openapi.crosswalk('request-property-removed'); disposition is detected · notEvaluated · declined, level error · warning · info"
               );
            }
         }

         ArrayList<java.util.LinkedHashMap> var8 = new ArrayList();
         var1.forEach(
            (var2x, var3x) -> {
               if ((var5.get("disposition") == null || var5.get("disposition").equals(var3x.get("disposition")))
                  && (var5.get("level") == null || var5.get("level").equals(var3x.get("level")))) {
                  LinkedHashMap<java.lang.String, java.lang.String> var4x = new LinkedHashMap();
                  var4x.put("id", var2x);
                  var4x.putAll((Map)copy(var3x));
                  var8.add(var4x);
               }
            }
         );
         return var8;
      } else if (var0 != null) {
         return JsApi.missing(
            "Openapi.crosswalk([checkId | {disposition, level}]) — an oasdiff check id, e.g. Openapi.crosswalk('request-property-removed'); disposition is detected · notEvaluated · declined, level error · warning · info"
         );
      } else {
         LinkedHashMap<String, Map<String, Integer>> var2 = new LinkedHashMap<>();
         var1.values()
            .forEach(
               var1x -> var2.computeIfAbsent((String)var1x.get("disposition"), var0xx -> new LinkedHashMap<>())
                  .merge((String)var1x.get("level"), 1, Integer::sum)
            );
         LinkedHashMap<java.lang.String, java.lang.Object> var3 = new LinkedHashMap();
         var var4 = io.karatelabs.ext.openapi.changes.OasdiffCrosswalk.b();
         var3.put("reference", copy(var4.get("reference")));
         var3.put("count", var4.get("count"));
         var3.put("counts", var2);
         var3.put("dispositions", copy(var4.get("dispositions")));
         return JsApi.hint(var3, "Openapi.crosswalk('<check id>') for one check; {disposition, level} filters the list");
      }
   }

   private static Object copy(Object var0) {
      if (var0 instanceof Map var3) {
         LinkedHashMap<java.lang.String, java.lang.Object> var2 = new LinkedHashMap();
         var3.forEach((var1x, var2x) -> var2.put(String.valueOf(var1x), copy(var2x)));
         return var2;
      } else {
         return var0 instanceof List var1 ? new ArrayList(var1.stream().map(Openapi::copy).toList()) : var0;
      }
   }

   private String didYouMean(String var1) {
      if (this.root == null) {
         return "";
      } else if (!var1.contains(":")
         && !Files.isRegularFile(this.root.resolve(var1))
         && (this.classpathRoot == null || !Files.isRegularFile(this.classpathRoot.resolve(var1)))) {
         Path var2 = this.root;
         String var3 = Path.of(var1).getFileName().toString().toLowerCase(Locale.ROOT);
         ArrayList<java.lang.String> var4 = new ArrayList();
         ArrayList<java.lang.String> var5 = new ArrayList();

         try (var var6 = Files.walk(var2, 3)) {
            var6.filter(var0 -> Files.isRegularFile(var0))
               .forEach(
                  var4x -> {
                     String var5x = var2.relativize(var4x).toString().replace('\\', '/');
                     String var6x = var4x.getFileName().toString().toLowerCase(Locale.ROOT);
                     if (!var5x.startsWith("target/")
                        && !var5x.startsWith("runs/")
                        && !var5x.startsWith("node_modules/")
                        && !var5x.startsWith(".")
                        && (var6x.endsWith(".yaml") || var6x.endsWith(".yml") || var6x.endsWith(".json"))) {
                        if (var6x.equals(var3)) {
                           var4.add(var5x);
                        } else if (var6x.contains("openapi") || var6x.contains("swagger") || var6x.contains("asyncapi")) {
                           var5.add(var5x);
                        }
                     }
                  }
               );
         } catch (IOException var11) {
            return "";
         }

         var var12 = new ArrayList<>(var4);
         var12.addAll(var5);
         return var12.isEmpty() ? "" : " — did you mean: " + String.join(" · ", var12.subList(0, Math.min(5, var12.size()))) + "?";
      } else {
         return "";
      }
   }

   private Object run(String var1, Object var2, Object var3) {
      io.karatelabs.ext.common.keycheck.Main.requireProduct("openapi." + var1, "openapi");
      if (var2 instanceof String var4 && !var4.isBlank()) {
         io.karatelabs.ext.sourcedoc.SourceDocument var5;
         OpenApiDocument var6;
         try {
            Resource var7 = MaxPaths.resolveResource(this.root, this.classpathRoot, var4);
            var5 = io.karatelabs.ext.sourcedoc.SourceDocument.parse(var4, var7.getText());
            var6 = new OpenApiDocument(var7, asDocument(var5));
         } catch (RuntimeException var16) {
            return JsApi.missing("Openapi." + var1 + " could not parse spec '" + var4 + "': " + var16.getMessage() + this.didYouMean(var4));
         }

         Map<java.lang.String, Object> var17 = var3 instanceof Map var8 ? var8 : Map.of();

         io.karatelabs.ext.lint.ProjectLintConfigLoader.ProjectLintConfig var9;
         WaiverMatcher var10;
         io.karatelabs.ext.lint.RulePackLoader.RulePack var18;
         try {
            var9 = io.karatelabs.ext.lint.ProjectLintConfigLoader.load(this.root);
            var18 = io.karatelabs.ext.lint.RulePackLoader.loadRulePack(!Boolean.FALSE.equals(var17.get("rulepack")), var9.b(), var17.get("rules"));
            io.karatelabs.ext.lint.InlineWaiverScanner.InlineWaiverScan var11 = io.karatelabs.ext.lint.InlineWaiverScanner.scan(var5);
            var10 = io.karatelabs.ext.lint.WaiverMatcher.fromObject(var17.get("waivers")).addWaivers(var11.a()).addRejectedWaivers(var11.b());
         } catch (LintException var15) {
            return JsApi.missing("Openapi." + var1 + ": " + var15.getMessage());
         }

         ReadOnlyCoverage var19 = ReadOnlyCoverage.of(
            Coverage.atReportDir(this.reportDir != null ? this.reportDir : (this.root != null ? this.root : Path.of(".")))
         );
         LinkedHashMap<java.lang.String, java.lang.Object> var12 = new LinkedHashMap();
         var12.put("spec", var4);

         try {
            var12.putAll(io.karatelabs.ext.lint.LintEngine.lint(var6.rawDocument, var18.a(), var19, var10, var5));
         } catch (LintException var14) {
            return JsApi.missing("Openapi." + var1 + " could not lint '" + var4 + "': " + var14.getMessage());
         }

         var12.put("ruleset", var18.b());
         if (var9.c() != null) {
            var12.put("project", var9.unreadableConfigReport());
         }

         return var12;
      } else {
         return JsApi.missing("Openapi." + var1 + "(spec[, opts]) requires a spec path String, e.g. Openapi." + var1 + "('api/openapi.yaml')");
      }
   }

   private static Map<String, Object> asDocument(io.karatelabs.ext.sourcedoc.SourceDocument var0) {
      if (var0.value() instanceof Map var1) {
         return var1;
      } else {
         throw new LintException(
            "the document is "
               + (var0.value() == null ? "empty" : "a " + var0.value().getClass().getSimpleName())
               + ", not an object — there is no API description here to govern"
         );
      }
   }

   public Openapi withRoot(Path var1) {
      return this.withRoot(var1, var1);
   }

   public Openapi withRoot(Path var1, Path var2) {
      this.root = var1;
      this.classpathRoot = var2;
      return this;
   }

   public Openapi withNoProject(boolean var1) {
      this.noProject = var1;
      return this;
   }

   public Openapi withReportDir(Path var1) {
      this.reportDir = var1;
      return this;
   }

   private Object docs(Object var1, Object var2) {
      String var3 = null;
      Object var4 = null;
      if (var2 instanceof Map var5) {
         for (Object var7 : var5.keySet()) {
            if (!"twin".equals(var7) && !"walk".equals(var7)) {
               return JsApi.missing(
                  "'"
                     + var7
                     + "' is not an option — Openapi.docs(spec, {twin?, walk?}) — twin names the rulebook whose model joins the spec's operations; walk is a Twin.explore plan"
               );
            }
         }

         if (var5.get("twin") != null && !(var5.get("twin") instanceof String)) {
            return JsApi.missing(
               "twin must be a rulebook name — Openapi.docs(spec, {twin?, walk?}) — twin names the rulebook whose model joins the spec's operations; walk is a Twin.explore plan"
            );
         }

         var3 = (String)var5.get("twin");
         var4 = var5.get("walk");
      } else if (var2 != null && var2 != Terms.UNDEFINED) {
         return JsApi.missing(
            "Openapi.docs(spec, {twin?, walk?}) — twin names the rulebook whose model joins the spec's operations; walk is a Twin.explore plan"
         );
      }

      String var8 = var3;
      Object var9 = var4;
      return this.withModel(
         var1,
         "docs",
         var4x -> {
            var var5x = io.karatelabs.ext.openapi.OpenApiDocumentView.a(var4x);
            if (var8 != null) {
               var5x.put("lifecycle", this.lifecycle(var4x, (String)var1, var8, var9));
            }

            return JsApi.hint(
               var5x,
               "page-shaped: every operation with ALL declared responses (error paths flagged negative) + component match templates. For exploration prefer Openapi.summary/operations; for one schema, Openapi.matchSchema"
            );
         }
      );
   }

   private Map<String, Object> lifecycle(OpenApiOperationCatalog var1, String var2, String var3, Object var4) {
      ArrayList<java.lang.String> var5 = new ArrayList();
      ArrayList<java.lang.String> var6 = new ArrayList();

      for (io.karatelabs.ext.openapi.OpenApiOperationCatalog.OperationInfo var8 : var1.operations()) {
         if (var8.a() != null) {
            var5.add(var8.a());
         } else {
            var6.add(var8.b().toUpperCase() + " " + var8.c());
         }
      }

      String var14 = MaxPaths.resolveResource(this.root, this.classpathRoot, var2).getText();
      Path var15 = this.rulesHome != null ? this.rulesHome : (this.root == null ? Path.of(".") : this.root).resolve("rulebooks");

      Map var10;
      try (Twin var9 = Twin.atRulesHome(var15).withRoot(this.root)) {
         var10 = var9.lifecycle(var3, var4, var14, var5, var6);
      }

      return var10;
   }

   public Openapi withRulesHome(Path var1) {
      this.rulesHome = var1;
      return this;
   }

   private Object withModel(Object var1, String var2, Function<OpenApiOperationCatalog, Object> var3) {
      io.karatelabs.ext.common.keycheck.Main.requireProduct("openapi." + var2, "openapi");
      if (var1 instanceof String var4 && !var4.isBlank()) {
         OpenApiOperationCatalog var5;
         try {
            var5 = io.karatelabs.ext.openapi.OpenApiOperationCatalog.loadFromResource(MaxPaths.resolveResource(this.root, this.classpathRoot, var4));
         } catch (RuntimeException var7) {
            return JsApi.missing("Openapi." + var2 + " could not parse spec '" + var4 + "': " + var7.getMessage() + this.didYouMean(var4));
         }

         return var3.apply(var5);
      } else {
         return JsApi.missing("Openapi." + var2 + "(spec, …) requires a spec path String, e.g. Openapi." + var2 + "('api/openapi.yaml')");
      }
   }

   private Object summaryOf(OpenApiOperationCatalog var1) {
      OpenApiDocument var2 = var1.document();
      int var3 = 0;

      for (io.karatelabs.ext.openapi.OpenApiOperationCatalog.OperationInfo var5 : var1.operations()) {
         if (var5.e() == null) {
            var3++;
         }
      }

      EnumMap<io.karatelabs.ext.openapi.OpenApiOperationCatalog.ResourceAction, Integer> var9 = new EnumMap<>(io.karatelabs.ext.openapi.OpenApiOperationCatalog.ResourceAction.class);
      int var10 = 0;

      for (io.karatelabs.ext.openapi.OpenApiOperationCatalog.OperationInfo var7 : var1.operations()) {
         io.karatelabs.ext.openapi.OpenApiOperationCatalog.ResourceOperation var8 = var1.resourceOperationFor(var7);
         if (var8 != null) {
            var10++;
            var9.merge(var8.b(), 1, Integer::sum);
         }
      }

      LinkedHashMap<Object, java.lang.Object> var11 = new LinkedHashMap();
      var9.forEach((var1x, var2x) -> var11.put(var1x.name().toLowerCase(), var2x));
      LinkedHashMap<java.lang.String, java.lang.Object> var12 = new LinkedHashMap();
      var12.put("title", var2.title);
      var12.put("version", var2.version());
      var12.put("specType", var2.specType);
      var12.put("baseUrl", var2.defaultServerUrl());
      var12.put("operations", var1.operations().size());
      var12.put("nullResponseSamples", var3);
      var12.put("resources", var1.resources().size());
      var12.put("crudBoundOps", var10);
      var12.put("crudByAction", var11);
      var12.put("resourceKeys", var1.resources().stream().map(io.karatelabs.ext.openapi.OpenApiOperationCatalog.ResourceInfo::a).sorted().toList());
      return JsApi.hint(
         var12,
         "Openapi.resources(spec) for the CRUD detail, Openapi.operations(spec) for the full inventory, Openapi.subset(spec, {resources|tags|paths}) to carve a self-contained slice"
      );
   }

   private Object operationsOf(OpenApiOperationCatalog var1) {
      ArrayList<java.util.LinkedHashMap> var2 = new ArrayList();

      for (io.karatelabs.ext.openapi.OpenApiOperationCatalog.OperationInfo var4 : var1.operations()) {
         LinkedHashMap<java.lang.String, java.lang.Object> var5 = new LinkedHashMap();
         var5.put("operationId", var4.a());
         var5.put("method", var4.b());
         var5.put("path", var4.c());
         var5.put("summary", var4.h());
         var5.put("tags", var4.i());
         var5.put("successStatus", var4.d());
         var5.put("hasRequestSchema", var4.f() != null);
         io.karatelabs.ext.openapi.OpenApiOperationCatalog.ResourceOperation var6 = var1.resourceOperationFor(var4);
         var5.put("crud", var6 == null ? null : var6.b().name().toLowerCase());
         var2.add(var5);
      }

      LinkedHashMap<java.lang.String, java.lang.Object> var7 = new LinkedHashMap();
      var7.put("operations", var2);
      var7.put("count", var2.size());
      return var7;
   }

   private Object resourcesOf(OpenApiOperationCatalog var1) {
      ArrayList<java.util.LinkedHashMap> var2 = new ArrayList();

      for (io.karatelabs.ext.openapi.OpenApiOperationCatalog.ResourceInfo var4 : var1.resources()) {
         LinkedHashMap<java.lang.String, java.lang.String> var5 = new LinkedHashMap();

         for (io.karatelabs.ext.openapi.OpenApiOperationCatalog.OperationInfo var7 : var1.operations()) {
            io.karatelabs.ext.openapi.OpenApiOperationCatalog.ResourceOperation var8 = var1.resourceOperationFor(var7);
            if (var8 != null && var8.a() == var4) {
               var5.put(var8.b().name().toLowerCase(), var7.a());
            }
         }

         LinkedHashMap<java.lang.String, java.lang.Object> var10 = new LinkedHashMap();
         var10.put("storeKey", var4.a());
         var10.put("collectionPath", var4.b());
         var10.put("itemPath", var4.c());
         var10.put("idParam", var4.d());
         var10.put("hasSchema", var4.e() != null);
         var10.put("actions", var5);
         var2.add(var10);
      }

      LinkedHashMap<java.lang.String, java.lang.Object> var9 = new LinkedHashMap();
      var9.put("resources", var2);
      var9.put("count", var2.size());
      return var9;
   }

   private Object partitionsOf(OpenApiOperationCatalog var1, Object var2, Object var3) {
      String var4 = var2 instanceof String var5 && !var5.isBlank() ? var5 : null;
      String var11 = var3 instanceof String var6 && !var6.isBlank() ? var6 : null;
      ArrayList<java.util.LinkedHashMap> var12 = new ArrayList();

      for (io.karatelabs.ext.openapi.OpenApiOperationCatalog.OperationInfo var8 : var1.operations()) {
         if ((var4 == null || var4.equals(var8.a())) && var8.f() != null) {
            var var9 = var8.f().partitions(var11);
            if (!var9.isEmpty() || var4 != null) {
               LinkedHashMap<java.lang.String, java.lang.Object> var10 = new LinkedHashMap();
               var10.put("operationId", var8.a());
               var10.put("method", var8.b());
               var10.put("path", var8.c());
               var10.put("axes", var9.size());
               var10.put("partitions", var9);
               var12.add(var10);
            }
         }
      }

      if (var4 == null) {
         LinkedHashMap<java.lang.String, java.lang.Object> var13 = new LinkedHashMap();
         var13.put("operations", var12);
         var13.put("count", var12.size());
         return JsApi.hint(
            var13,
            var12.isEmpty()
               ? "no operation request body carries a partitionable constraint (minItems/maxItems, minLength/maxLength/pattern/format, enum, minimum/maximum) — the spec leaves inputs open"
               : "Openapi.partitions(spec, operationId) for one operation's detail; each axis is a closed universe coverage can grade observed inputs against with zero authoring (MODEL §2h)"
         );
      } else if (var12.isEmpty()) {
         return JsApi.missing(
            "Openapi.partitions: no operation '"
               + var4
               + "' with a request body — see Openapi.operations(spec) for the operationIds that carry a request schema"
         );
      } else {
         Map var14 = (Map)var12.getFirst();
         return JsApi.hint(
            var14,
            "each axis is a closed equivalence-class universe derived from the spec's validation keywords — coverage grades observed inputs against it with zero authoring"
         );
      }
   }

   private Object subsetOf(OpenApiOperationCatalog var1, Object var2) {
      if (var2 instanceof Map var3) {
         var var4 = new LinkedHashSet<>(strings(var3.get("paths")));
         var var5 = strings(var3.get("tags"));
         var var6 = strings(var3.get("operationIds"));
         var var7 = strings(var3.get("resources"));

         for (io.karatelabs.ext.openapi.OpenApiOperationCatalog.OperationInfo var9 : var1.operations()) {
            if (!var5.isEmpty() && var9.i() != null && var9.i().stream().anyMatch(var5::contains)) {
               var4.add(var9.c());
            }

            if (var6.contains(var9.a())) {
               var4.add(var9.c());
            }
         }

         for (io.karatelabs.ext.openapi.OpenApiOperationCatalog.ResourceInfo var14 : var1.resources()) {
            if (var7.contains(var14.a())) {
               var4.add(var14.b());
               var4.add(var14.c());
            }
         }

         if (var4.isEmpty()) {
            return JsApi.missing(
               "no paths matched the selector — see Openapi.resources(spec) / Openapi.operations(spec) for the available resource keys, tags, and paths"
            );
         } else {
            OpenApiSpecSlicer.a var13 = OpenApiSpecSlicer.a(var1.document(), new ArrayList<>(var4));
            String var15 = var3.get("format") instanceof String var10 ? var10 : "yaml";
            LinkedHashMap<java.lang.String, java.lang.Object> var16 = new LinkedHashMap();
            var16.put("spec", "json".equalsIgnoreCase(var15) ? Json.of(var13.a()).toString() : yaml(var13.a()));
            var16.put("format", var15);
            var16.put("paths", var13.b());
            var16.put("operationCount", var13.d());
            var16.put("components", var13.c());
            return JsApi.hint(
               var16,
               "self-contained (refs closed over) — write `spec` to a ." + var15 + " file and feed it to Http.mock / cov.openapi / Openapi.deriveRequirements"
            );
         }
      } else {
         return JsApi.missing(
            "Openapi.subset(spec, opts) needs a selector object — one of {paths:[...]}, {resources:[...]}, {tags:[...]}, {operationIds:[...]}"
         );
      }
   }

   private static List<String> strings(Object var0) {
      if (var0 instanceof List var1) {
         ArrayList<java.lang.String> var2 = new ArrayList(var1.size());

         for (Object var4 : var1) {
            if (var4 != null) {
               var2.add(String.valueOf(var4));
            }
         }

         return var2;
      } else {
         return List.of();
      }
   }

   private static String yaml(Object var0) {
      DumperOptions var1 = new DumperOptions();
      var1.setDefaultFlowStyle(FlowStyle.BLOCK);
      var1.setIndent(2);
      var1.setPrettyFlow(true);
      var1.setSplitLines(false);
      return new Yaml(var1).dump(var0);
   }

   private Object scaffold(Object var1, Object var2) {
      return this.noProject ? JsApi.noProjectSelected("Openapi.scaffold") : this.withModel(var1, "scaffold", var3 -> this.scaffoldOf(var1, var3, var2));
   }

   private Object scaffoldOf(Object var1, OpenApiOperationCatalog var2, Object var3) {
      Map<java.lang.String, Object> var4 = var3 instanceof Map var5 ? var5 : Map.of();
      String var13 = MaxPaths.resolveRef(this.root, String.valueOf(var1));
      String var6 = baseName(String.valueOf(var1));
      String var7 = featureName(var4.get("feature"), var2.document().title);
      String var8 = "checks/" + var7;
      String var10 = var4.get("out") instanceof String var11 && !var11.isBlank() ? var11 : null;
      ScaffoldContext var14 = ScaffoldContext.builder(this.root)
         .outDir(var10)
         .overwrite(var4.get("overwrite"))
         .copyArtifact(var13, var6)
         .writtenHint(
            var10 == null
               ? null
               : "wrote a runnable 3-source project (`out` is the resolved project dir, `written` lists the files) — run it with `Runner.suite('"
                  + var8
                  + "')` when `out` is the served project, else `karate run "
                  + var8
                  + "` from `out`; then read the coverage/traceability report and ground the derived stubs in requirements/derived.md (assign @criticality, rewrite intent, tag tests @req=ID/criterion) to turn them green (§2f)"
         )
         .filesHint(
            "write each entry of `files` to its named path (pass {out:'dir'} to have scaffold write them + copy the spec in), then `Runner.suite('"
               + var8
               + "')` on a served project, else `karate run "
               + var8
               + "`"
         )
         .build();
      io.karatelabs.ext.openapi.OpenApiScaffoldEmitter var12 = new io.karatelabs.ext.openapi.OpenApiScaffoldEmitter(var2, var6, var8);
      return new ContractScaffold().scaffold(io.karatelabs.ext.openapi.OpenApiContractLoader.a(var2.document()), var12, var14);
   }

   private static String baseName(String var0) {
      String var1 = var0;
      int var2 = var0.indexOf(63);
      if (var2 >= 0) {
         var1 = var0.substring(0, var2);
      }

      int var3 = var1.lastIndexOf(47);
      return var3 >= 0 ? var1.substring(var3 + 1) : var1;
   }

   private static String featureName(Object var0, String var1) {
      String var2;
      if (var0 instanceof String var3 && !var3.isBlank()) {
         var2 = var3;
      } else {
         var2 = slug(var1);
      }

      if (var2.isBlank()) {
         var2 = "starter";
      }

      return var2.endsWith(".feature") ? var2 : var2 + ".feature";
   }

   private static String slug(String var0) {
      if (var0 == null) {
         return "";
      } else {
         String var1 = var0.toLowerCase().replaceAll("[^a-z0-9]+", "-").replaceAll("(^-+|-+$)", "");
         return var1.length() > 40 ? var1.substring(0, 40).replaceAll("-+$", "") : var1;
      }
   }

   Object deriveRequirements(Object var1) {
      io.karatelabs.ext.common.keycheck.Main.requireProduct("openapi.deriveRequirements", "openapi");
      if (var1 instanceof String var2 && !var2.isBlank()) {
         OpenApiOperationCatalog var3;
         try {
            var3 = io.karatelabs.ext.openapi.OpenApiOperationCatalog.loadFromResource(MaxPaths.resolveResource(this.root, this.classpathRoot, var2));
         } catch (RuntimeException var6) {
            return JsApi.missing("Openapi.deriveRequirements could not parse spec '" + var2 + "': " + var6.getMessage() + this.didYouMean(var2));
         }

         String var4 = io.karatelabs.ext.openapi.DerivedRequirementsRenderer.a(var3).a();
         LinkedHashMap<java.lang.String, java.lang.Object> var5 = new LinkedHashMap();
         var5.put("requirements", var4);
         var5.put("count", var3.operations().size());
         return JsApi.hint(var5, "review each stub — assign @criticality, rewrite as real intent, refine acceptance, then drop @source=derived once grounded");
      } else {
         return JsApi.missing("Openapi.deriveRequirements(spec) requires a spec path String, e.g. Openapi.deriveRequirements('api/openapi.yaml')");
      }
   }

   private static Map<String, Object> help() {
      return JsApi.markdown(
         "# Openapi — start here for an OpenAPI/Swagger spec: govern it (lint + grade against a style guide), orient, slice, derive requirement stubs\n\nReverse-engineers a project artifact from an API spec **alone** (OpenAPI/Swagger today;\nPostman/AsyncAPI are future sources). Authoring only, deterministic — it never runs\nanything or calls an LLM. Every verb takes a spec path String.\n\n**Understand a large spec (read these first):**\n- `Openapi.summary(spec)` — orientation: title/version/specType/baseUrl, operation count,\n  detected resource count + CRUD-by-action breakdown, resource keys. Start here. The prefix\n  the spec declares (`servers`/`basePath`) is part of every address: `Http.mock` serves under\n  it and `cov.openapi` matches traffic under it — override either with `basePath` (`''` = none).\n- `Openapi.operations(spec)` — the full inventory as `{operations, count}` (the rows are\n  under `.operations`, not the return value itself): one row per operation —\n  `operationId`, method, path, summary, tags, successStatus, hasRequestSchema, crud.\n- `Openapi.resources(spec)` — detected REST resources: collection/item paths, id param,\n  and which operation plays each CRUD verb.\n- `Openapi.docs(spec, {twin?, walk?})` — the docs page model. `twin: '<rulebook>'` adds\n  `lifecycle`: per operation the walk's tuples, refusals with wire status and `req`; per\n  state what next (`applied` · `refused` · `unavailable` · `errored` · `notreached`); flow\n  edges; model-only commands; unmodelled operations. `walk` is a `Twin.explore` plan. No\n  twin, no binding or a model that fails to load reads `lifecycle.unavailable`.\n\n**Carve a focused slice (turn a 30k-line spec into a fixture):**\n- `Openapi.subset(spec, {resources|paths|tags|operationIds[, format]})` — returns a\n  **self-contained** spec (the transitive `$ref` closure of the selected paths, plus\n  referenced security schemes). `format` is `yaml` (default) or `json`. Write `spec` to a\n  file and feed it to `Http.mock` / `cov.openapi` / `Openapi.deriveRequirements`.\n\n**Derive test-design partitions (the spec's validation keywords ARE equivalence classes):**\n- `Openapi.partitions(spec[, operationId[, referenceDate]])` — each operation's request-body\n  schema → `{axisPath: {kind, classes, samples?, boundaries?, source:'spec'}}`. `kind` is\n  `cardinality` (array minItems/maxItems → BVA on length), `string`\n  (minLength/maxLength/pattern/format → empty/valid/length-boundary/format-invalid),\n  `date` (format date/date-time, graded vs the optional `referenceDate` — P18, never now()),\n  `enum` (the value-set), or `number` (minimum/maximum → BVA). `nullable`/optional add a\n  `null`/`absent` class. The spec leaves an unconstrained field **open** (omitted — an honest\n  gap, not a fake universe). One taxonomy, three uses: **grade** input adequacy with zero\n  authoring, **plan** the covering-array deck, **generate** a fuzz corpus from `samples`.\n\n**Lock assertions to the contract (spec-locked match templates):**\n- `Openapi.matchSchema(spec, ref[, status])` — the karate **match template** for a\n  component (`'User'`) or an operation's declared response (`operationId`; pass a\n  `status` for a declared error shape — negative paths welcome). Required fields come\n  back `#typed`, optional `##typed`, `format: uuid` → `#uuid`, enums → a value-set\n  predicate. In a **suite** the same power is the `spec` global: `spec.schema('User')`\n  (auto-registered when `cov.openapi` / the openapi ext is configured). The\n  observed-payload twin (no spec needed) is `match.schema(example)`.\n\n**Govern the spec (lint it, and grade it against the run):**\n- `Openapi.lint(spec[, opts])` — the bundled rulepack over the document →\n  `{findings, waivers, graph, summary, capabilities, ruleset}` (the `Requirement.lint()` shape:\n  `error`/`warn`/`info`, `summary.clean`, `summary.passed`). Deterministic — no LLM, no\n  network, no run needed. Two halves: generic API hygiene (contact, https, kebab paths,\n  operationId casing + uniqueness, declared path params, success/error responses,\n  example-vs-schema, unreferenced components, `$ref` siblings) **and the graph-reaching\n  rules nothing else can express** — is this operation claimed by a requirement, was it\n  ever exercised, and did anything but our own mock answer it? When there is no project\n  graph those rules are **inert, not clean**, and `graph.note` says so.\n- Every finding carries `line`/`col` and a `position` block — the full span, the character\n  `offset`/`endOffset` (so a fix is a splice), whether the position is `exact`, and which\n  half of the construct it anchors on. A pointer the file does not literally contain — the\n  ordinary case for a rule graded through a `$ref` — resolves to the nearest node it does,\n  reported as `exact:false` with the pointer that was located. A finding with no pointer\n  gets no position, deliberately: a wrong one is worse than none.\n- `ruleset` is the identity of the rules that produced the verdict —\n  `{digest, total, bundled, project, opts}`, the digest a `sha256:` over the composed rules\n  after precedence. Two results with one digest were graded by the same policy. The grade\n  carries it as `scoring.ruleset`.\n- `opts`: `{rules}` — an `{id: rule}` object of your own; a rule sharing a bundled id\n  **replaces** it, which is how you turn one off (redeclare at `info`) or retune it\n  (different `params`). `{rulepack:false}` drops the bundled pack. `{waivers}` — a waiver\n  (or a list): `{rule, scope, reason, owner}`, where `scope` is a JSON-Pointer prefix. A\n  waiver never silences: the finding is still reported, marked `waived`, and the register\n  says who signed it. `{output:'markdown'|'both'}` renders the scorecard.\n- **Waive it in the spec itself**, at the point of violation:\n  `# karate-lint-disable-next-line <rule-id> — <reason> (owner: @who)` on the line above,\n  or `# karate-lint-disable-line <rule-id> — <reason> (owner: @who)` trailing the line.\n  `<rule-id>` may be `*`. Its scope is that **exact** pointer and nothing beneath it, so a\n  comment on an operation does not waive every finding inside it. Same rules as any waiver\n  — reason and owner required, reported in the register — and a directive that cannot be\n  read is reported as a refusal with its own line. YAML only; JSON has no comments.\n  **Only a finding that HAS a pointer can be waived inline.** A whole-document finding\n  (`pointer: \"\"` — e.g. `security-scheme-declared`, which is about what the document never\n  says) has no construct to sit above: a comment on the first line binds to the first *key*,\n  not to the root, and the register reports `applied: 0`. Waive those through `opts.waivers`\n  with an empty `scope` instead. Findings with no position at all (§the position block)\n  are the same case.\n- A rule is `{given, givenType, severity, then}` — `givenType` is `jsonpath-plus` (the\n  full dialect: `~` keys, `^` parent, `@property`, filters) or `js` (an expression with\n  the document bound as `$`; `Object.keys($.paths)` **is** the keys selector). `then` is\n  a JS `(node, ctx) => …` returning nothing to pass, or a message / `{message, path}` /\n  a list of them — it reads like a karate `match`, not like a callback. `ctx` carries\n  `{path, pointer, key, parent, document, params, rule, severity, format, graph}` —\n  **`ctx.graph` is what a graph-reaching rule guards on first** (`if (!ctx.graph) return`):\n  with no graph every `Coverage` verb answers an error map, and so does an unknown id on a\n  real graph, so nothing else can tell \"nothing to ask\" from \"asked, and it is an orphan\".\n  Omit that guard and the rule fires on every node instead of none. A rule whose finding\n  is about the **key** (casing, an unsupported keyword) declares `anchor: 'key'` so its\n  position lands there; the default is `value`. Helpers: `fn.truthy`\n  `falsy` `defined` `undefined` `pattern` `casing` `length` `enumeration` `alphabetical`\n  `schema` `xor` `or` `unreferencedReusableObject` — all return `null` to pass / a message to\n  fail, the same convention as `then`, so you **return a helper's result directly, never\n  negated**; the four `truthy`/`falsy`/`defined`/`undefined` take a value alone, the rest\n  take an **options object** (never a bare string). They do not agree on a null input —\n  `pattern`/`casing`/`length`/`enumeration` pass, `xor`/`or` fail, `schema` validates it —\n  so guard an optional node yourself. The signature table is\n  `Skill.help('api-governance', 'lint-rule-authoring')`. Rules run in an enumerated sandbox —\n  the document (a copy), the read-only `Coverage` graph and `fn`; no filesystem, network,\n  process or Java interop. The result's `capabilities` block states those terms.\n- **Declare your rules once, in `karate-boot.js`** — `cov.lintRules = {id: rule, …}`, beside\n  `cov.criticalityPolicy` and `cov.real`. Every surface that lints (console, MCP, curl, CI)\n  then honours them without being passed anything. Precedence is nearest-scope-wins by id:\n  bundled pack → `cov.lintRules` → `opts.rules`. If that file cannot be evaluated the result\n  carries a `project` block saying so — we cannot know which of your rules did not run, so it\n  is disclosed and gates the grade rather than reading clean. A malformed rule in it is a\n  rulebook error, not a finding.\n- `Openapi.grade(spec[, opts])` — the same lint, **graded**: `{verdict, index, level,\n  dimensions, notMeasured, gates, weights, judged, scoring, lint}`. `verdict` is the\n  `Requirement.readiness()` vocabulary (`READY`/`CONDITIONAL`/`NOT_READY`); `level` is the\n  maturity rung — **Ungoverned → Described → Governed → Traced → Exercised → Proven** —\n  carrying what the next rung asks for AND `level.ladder`, every rung with what it asks,\n  so \"what would it take to reach the top?\" is one read rather than a climb per rung.\n  Five dimensions: `design` · `security` · `documentation`\n  · `provenness` · `contract`.\n  **A dimension with no evidence reads `not measured` — never zero**, and each unmeasured\n  one carries a `note` naming the call that moves it (or saying plainly that none does).\n  With no coverage graph `provenness` is unmeasured (not perfect, not catastrophic) — to\n  measure it, **`Report.aggregate()`** writes the graph the graph-reaching rules read, and\n  evidence gets into it from either a suite (`Runner.suite('checks')`) or a console **live\n  run** (`Harness.startLive()` → `Http.*` probes → `Harness.stop()`) — on the console each\n  settled run folds into the graph. `contract` takes a paired mock-vs-provider run, which\n  **`Contract.pair({suite, spec, provider})`** mints and commits (`Contract.read` reads it\n  back with its freshness derived at read time). **Grading READS that file** — the newest\n  pair minted against *this* spec, scored as the rung normalised (rung ÷ 4), with `Proven`\n  awarded at the top rung and nowhere else. Freshness is re-derived here too, so a pair\n  whose spec, suite or mock has moved since holds NO rung rather than a smaller one, and\n  the dimension reads `not measured` with the withdrawal named. With no pair for this spec\n  the dimension is `not measured`; the declared\n  weights are renormalised across what WAS measured and `weights.note` says so. A rung is only awarded where a dimension was\n  actually measured, so an unmeasured one stops the climb — `level.blockedBy` says which and\n  why. **`READY` needs every dimension measured**, so a clean artifact with an unmeasured one\n  reads `CONDITIONAL` and `verdictNote` says it is missing evidence, not an open finding.\n  A cap holds a dimension down (`security` is capped when no security scheme is declared at\n  all) and a bound cap is also a gate; a **gate** decides the verdict and never edits the\n  index. A rule score is failing *subjects* over subjects looked at, not findings counted up.\n  Everything is deterministic — nothing model-judged is in the number.\n  `{weights: {provenness: 0.5, …}}` overrides the weights (the rung bars and the\n  error/warn/info weighting are not overridable today); `{output:'both'}` renders it.\n  **Scope note:** `security` grades the bundled security-*hygiene* rules over the document —\n  is auth declared, does a key travel in a query string, are collections bounded. It is not\n  a posture score.\n\n**Breaking changes between two versions of a spec:**\n- `Openapi.diff(baseline, current[, {asOf, output, waivers, baselineReportDir, failOn}])` —\n  `{ok, complete, summary, breaking, warnings, compatible, notEvaluated, waivers, components?}`;\n  a finding: `kind`, `level`, `path`, `pointer`, `authored`, `id`, `oasdiff`, `join`. Gate on\n  `ok && complete`; `failOn: 'breaking'|'warning'|'compatible'` adds `gate.exitCode` (1 fail or\n  incomplete, 2 bad input) for `Runner.exit`. `waivers`: a list or file of `{rule: <kind>,\n  scope?, reason, owner, expires?}`, `scope` covering each finding whose `pointer` or `authored`\n  is at or under it (none: all), still listed. `output: 'changelog'|'markdown'`. Spec vs a twin's model:\n  `Rule.project(name, {against: 'api/<name>.yaml'})`.\n- `Openapi.crosswalk(checkId)` — that oasdiff check's disposition; none: the counts.\n\n**Author from a spec:**\n- `Openapi.deriveRequirements(spec)` — a spec → **hollow** requirement stubs as\n  `karate-requirements/v1` markdown. Returns `{requirements, count}`; write the\n  `requirements` body to `requirements/derived.md`, then **ground each stub by hand**.\n- `Openapi.scaffold(spec, {out?, feature?, overwrite?})` — the **whole runnable harness** from\n  the spec alone: `karate-boot.js` + `karate-config.js` + `start-mock.js` (a stateful\n  `Http.mock` backend) + a starter `.feature` exercising detected resources +\n  `requirements/derived.md`. Returns `{files, count}`; `{out:'dir'}` writes them all and\n  copies the spec in. An existing file refuses the whole write; `{overwrite:true}` replaces\n  it, listed under `overwritten`. Run the feature with `Runner.suite` for a 3-source\n  coverage/traceability report. The derived stubs stay NOTCOVERED until you ground them (§2f).\n\nEach derived stub is honestly incomplete (the §2f reward-hacking gate — derived-from-spec\nrequirements restate behavior, not intent):\n- `@source=derived` — the hollow marker (remove it once grounded);\n- `@real=openapi:<operationId>` — wired to the operation, so an observed hit strengthens it;\n- one happy-path EARS acceptance criterion reverse-engineered from the response;\n- `@criticality` left blank — you assign impact.\n\nRelated: `Http.mock(spec)` (stand up a stateful backend from the same spec),\n`Requirement.matrix()`/`gaps()` (the 3-source report once tests run), the\n`scaffold-from-openapi` skill (the full spec → project → report flow)."
      );
   }
}
