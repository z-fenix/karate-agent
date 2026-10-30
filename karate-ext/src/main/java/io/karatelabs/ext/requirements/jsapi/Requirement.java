package io.karatelabs.ext.requirements.jsapi;

import io.karatelabs.common.Json;
import io.karatelabs.ext.common.MaxPaths;
import io.karatelabs.ext.common.Sha256;
import io.karatelabs.ext.common.jsapi.JsApi;
import io.karatelabs.ext.common.jsapi.JsDispatch;
import io.karatelabs.ext.requirements.ReadinessEvaluator;
import io.karatelabs.ext.requirements.CoverageAnalyzer;
import io.karatelabs.ext.requirements.RequirementsRepository;
import io.karatelabs.ext.requirements.RequirementsYamlWriter;
import io.karatelabs.ext.requirements.RequirementsReportBuilder;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Requirement extends JsDispatch {
   static final String DATA_FILE = "ext/coverage/coverage-data.json";
   private static final String RUN_META = "meta.json";
   public static final String RUN_PROVENANCE = "ext/coverage/run.json";
   private final Path dataFile;
   private final List<Path> homes;
   private final Path runsDir;
   private final Path projectRoot;
   private Function<Path, Map<String, Object>> sides;
   private static final String DELTA_SHAPE = "Requirement.delta({from, to}) compares TWO explicit run scopes, each {run:'<id>'} or {reportDir:'<path>'} — there is no default and no 'previous run', because only the caller knows which two runs are comparable";
   private static final Map<Path, Object> WRITE_LOCKS = new ConcurrentHashMap<>();
   private volatile io.karatelabs.ext.requirements.jsapi.Requirement.ReadinessCacheEntry readinessKept;
   private static final List<String> STATUS_KEYS = List.of("covered", "failing", "notrun", "notcovered");

   public Requirement(Path var1, Path var2) {
      this(var1, var2, null);
   }

   public Requirement(Path var1, Path var2, Path var3) {
      this(var1, var2 == null ? List.of() : List.of(var2), var3);
   }

   public Requirement(Path var1, List<Path> var2, Path var3) {
      this(var1, var2, var3, null);
   }

   public Requirement(Path var1, List<Path> var2, Path var3, Path var4) {
      this.dataFile = var1;
      this.homes = var2 == null ? List.of() : List.copyOf(var2);
      this.runsDir = var3;
      this.projectRoot = var4;
      this.fn("help", var0 -> JsApi.helpTopic(help(), var0));
      this.fn("list", var1x -> this.list(JsApi.arg(var1x, 0)));
      this.fn("matrix", var1x -> this.matrix(JsApi.arg(var1x, 0)));
      this.fn("gaps", var1x -> this.gaps(JsApi.arg(var1x, 0)));
      this.fn("delta", var1x -> this.delta(JsApi.arg(var1x, 0)));
      this.fn("get", var1x -> this.get(JsApi.str(var1x, 0)));
      this.fn("load", var1x -> this.load(JsApi.str(var1x, 0), JsApi.arg(var1x, 1)));
      this.fn("read", var1x -> this.read(JsApi.str(var1x, 0), JsApi.arg(var1x, 1)));
      this.fn("write", var1x -> this.write(JsApi.str(var1x, 0), JsApi.arg(var1x, 1), JsApi.arg(var1x, 2)));
      this.fn("lint", var1x -> this.lint(JsApi.arg(var1x, 0)));
      this.fn("readiness", var1x -> this.readiness(JsApi.arg(var1x, 0)));
      this.fn("posture", var1x -> this.posture(JsApi.arg(var1x, 0)));
   }

   public static Requirement atReportDir(Path var0) {
      return new Requirement(var0.resolve("ext/coverage/coverage-data.json"), null);
   }

   public Requirement withRequirementsHome(Path var1) {
      return this.withRequirementsHomes(var1 == null ? List.of() : List.of(var1));
   }

   public Requirement withRequirementsHomes(List<Path> var1) {
      return new Requirement(this.dataFile, var1, this.runsDir, this.projectRoot);
   }

   public Requirement withProjectRoot(Path var1) {
      return new Requirement(this.dataFile, this.homes, this.runsDir, var1);
   }

   public Requirement withRunsDir(Path var1) {
      return new Requirement(this.dataFile, this.homes, var1, this.projectRoot);
   }

   public Requirement withScopeBuilder(Function<Path, Map<String, Object>> var1) {
      this.sides = var1;
      return this;
   }

   private static Map<String, Object> help() {
      return JsApi.markdown(
         "# Requirement — the requirement view over the traceability graph (read-only, deterministic)\n\nRequirements are authored in git markdown (`requirements/*.md`); the derived coverage view\nreads `<reportDir>/ext/coverage/coverage-data.json` (written by a `karate run -f karate:jsonl`).\n\n**The test-side tag grammar:** link a scenario to an acceptance criterion by tagging it\n`@req=ID/criterion` (e.g. `@req=ORD-001/1`); a requirement with no criteria takes the bare\n`@req=ID`. One scenario may carry SEVERAL `@req=` tags (one per criterion it genuinely\nexercises). A bare `@req=ID` on a requirement that HAS criteria joins the edge but covers\nnothing — the `bareParent` gap below names the fix.\n\n- `Requirement.list([opts])` — the lean **index** as a plain ARRAY (id · title · source `file` ·\n  criticality · acceptance `criteria[]`, each with its exact `@req=` stamp token), read\n  from the markdown SoT so it works **before any run**; `.map`/`.filter`/`.length` work\n  on it directly. The discovery surface: what an LLM/flow can stamp a verdict\n  against (`Robot.step('… @req=ID/crit', 'pass')` / `Rule.execute(...).verify(...)`).\n  `{output:'markdown'}` renders the stamp-the-verdict checklist instead.\n- `Requirement.matrix([opts])` — every requirement × derived coverage `status` + covering test\n  labels; `criteria[]` nests each criterion with its `tests` (`{label, status, check}`, `check` = the\n  `checks/<f>.js` behind a check) + the `acceptanceStatus` AND-rollup. `{output:'csv'}` → `{csv}`.\n- `Requirement.gaps([opts])` — `notcovered` (status NOTCOVERED), `partialCriteria` (an untested\n  acceptance criterion), `refusalOnly` (criterion ids green on refusal evidence alone),\n  `notasserted` (criterion ids whose covering tests assert nothing), `derived` (`@source=derived`\n  stubs not yet grounded — readiness withholds READY while any remain),\n  `bareParent` (tagged `@req=ID` — the edge joins but does NOT cover the criteria; re-tag to\n  `@req=ID/criterion`), `notfound` (`@req=` → a missing requirement), `incidental`/`drift`/`orphans`.\n- `Requirement.delta({from, to})` — what changed between two runs; each side is a scope below,\n  and both must be the same kind (dry↔dry, executed↔executed). Buckets per criterion and per\n  requirement: `moved`, `declared`, `corroborated`, `lost`, `regressed`, plus `improved`.\n  Clearing `oracleOnly`/`refusalOnly` while the evidence stands is corroboration and counts as\n  progress; a qualifier that fell because the evidence vanished is `lost`, never a clearance.\n  Both sides carry the project's run-free rules projection (`oracle: 'project'`), so on an\n  estate the rules already cover a join reads as corroboration, with `moved` empty.\n- `Requirement.readiness([opts])` — authored `criticality` × coverage → risk per leaf →\n  the **confidence-to-ship verdict** (`state`, `ready`, `verdict`, `blockers`). `state` is three-way:\n  **READY** (all covered) · **CONDITIONAL** (covered core, no HIGH blocker, but N requirements still\n  uncovered or `derived` stubs ungrounded) · **NOT_READY** (HIGH-risk blockers); `ready` is `true`\n  only for READY.\n  `totals` — the \"N of M\" over the leaves: `{requirements, criteria}`, each `{covered, failing, notrun,\n  notcovered, total}` (`covered` includes hollow greens; `state` judges them). Each row and blocker\n  has `exercisedLevel` (exercised · partexercised · notexercised — did anything run it), apart from\n  `provenance` (`@real=`). `claimedUnverified` — every criterion a completed spec task claims and no\n  passing check backs; never re-derive it from the task list.\n- `Requirement.get(id)` — one requirement (e.g. `'ORD-001'`), each criterion with its\n  exact `@req=` stamp token (a requirement with no criteria carries the bare one).\n- `Requirement.lint([opts])` — authoring-quality findings over the markdown SoT (no run).\n- `Requirement.posture([opts])` — governance posture + eval-independence (§2f).\n- `Requirement.load([path][, opts])` — the normalized `karate-requirements/v1` doc.\n- `Requirement.read(file[, {home}])` — a requirements file's `content` and its `mtime`, the\n  `ifMatch` a guarded write passes. `file` is a `list()` row's `file`, or a name in the home;\n  `write` takes the same.\n- `Requirement.write(name, markdown, opts?)` — **author** a `<name>.md` requirements file (the\n  `karate-requirements/v1` markdown; the `requirements` skill teaches the format) →\n  `{path, file, count, ids, mtime}`. Parse-validated (≥1 requirement); `{ifMatch}` refuses a stale\n  overwrite (409 STALE_MTIME); `{overwrite:false}` refuses an existing file (409 EXISTS) — an import;\n  `{dryRun:true}` writes nothing and returns what the console reads: `requirements` (criteria,\n  heading `line`) and lint `findings`; `{home}` picks the target among several requirements homes. The requirement is in the matrix at\n  once (NOTCOVERED); a rule's `calc.req('<id>/<ac>')` lights its criterion COVERED **when a run emits\n  it** (`karate run` / a data-driven feature — `Rule.run` shows the reached criteria as `reqs`, but the\n  persisted matrix is rebuilt from a run, not from `/api/eval` calls).\n\n**Which graph a read answers off** — `matrix`/`gaps`/`readiness` take an optional scope:\n`{run:'<runId>'}` reads **that run's own** graph (a run writes a complete, self-contained\none whenever `karate-boot.js` sets `cov.requirements`) and `{reportDir:'<path>'}` reads any\nother report root; no scope reads the **project** graph, which every settled console run\nfolds itself into (a dry run never does). So \"run it, then ask\" answers off that run either\nway — `{run}` narrows to exactly one, unscoped spans every run the project has accumulated.\n\n**A verdict is never cacheable** — it is derived from the last run and changes the moment\nanything runs, including in a terminal you cannot see. Re-read; never repeat an earlier\nanswer. `runEvidence.tests` is how much run evidence is behind the reply: `0` means nothing\nhas run, so NOTCOVERED everywhere says *nothing was checked* — not *a run missed it*. Per test\nthe latest terminal execution decides (an earlier failure reads `previouslyFailed`, a running\none `pending`); `runEvidence.selection` says which executions were chosen and why.\n\nStatuses: COVERED · FAILING · NOTRUN · NOTCOVERED. A COVERED row may disclose `oracleOnly`\n(only the rulebook vouches), `refusalOnly` (only reject rows its shape refused), `modelOnly`\n(only a model checker's verdict vouches — the design, not the implementation) or\n`notasserted` (its tests passed without checking anything) — each graded like a gap by\n`readiness()`. A NOTRUN row with `weakened` passed only on a check a chat loosened, unreviewed\n(`@ai=weakened`). `{output:'markdown'|'both'}` renders prose.\nRelated: `Coverage.*` (raw graph), `Sim.*` (fabricate a world run-free), `Report.generate()`."
      );
   }

   public Object list(Object var1) {
      if (this.homes.isEmpty()) {
         return JsApi.missing("Requirement.list() needs a requirements home (the serve/suite requirements dir)");
      } else if (this.homes.stream().noneMatch(var0 -> Files.exists(var0))) {
         io.karatelabs.ext.common.jsapi.EvaluationMessageScope.a(
            "no requirements authored yet ("
               + (this.homes.size() == 1 ? "no '" + this.homes.get(0).getFileName() + "' dir" : "none of the configured homes exist: " + this.homePaths())
               + ") — author one with Requirement.write(...) / Skill.help('requirements'), or verify a value against a rule with Rule.execute(name, row).verify(ok, note) (rules attach coverage on their own)"
         );
         return listOutput(new ArrayList<>(), var1);
      } else {
         RequirementsRepository var3 = new RequirementsRepository(this.homes, this.projectRoot());

         io.karatelabs.ext.requirements.model.RequirementsBundle var2;
         try {
            var2 = this.homes.size() == 1 && !Files.isDirectory(this.homes.get(0)) ? this.loadDoc(this.homes) : var3.a();
         } catch (IOException var8) {
            return Map.of("error", "cannot read " + this.homePaths() + ": " + var8.getMessage(), "code", "NOT_FOUND", "status", 404);
         } catch (RuntimeException var9) {
            return JsApi.error(var9);
         }

         ArrayList var4 = new ArrayList();

         for (io.karatelabs.ext.requirements.model.RequirementNode var6 : var2.d()) {
            LinkedHashMap var7 = new LinkedHashMap();
            var7.put("id", var6.b());
            var7.put("title", var6.d() != null ? var6.d() : var6.b());
            putIfSet(var7, "file", var3.a(var6));
            putIfSet(var7, "criticality", var6.h());
            putCriteria(var7, var6);
            var4.add(var7);
         }

         return listOutput(var4, var1);
      }
   }

   private static void putCriteria(Map<String, Object> var0, io.karatelabs.ext.requirements.model.RequirementNode var1) {
      var var2 = io.karatelabs.ext.requirements.AcceptanceCriterion.a(var1.m());
      if (var2.isEmpty()) {
         var0.put("stamp", "@req=" + var1.b());
      } else {
         ArrayList var3 = new ArrayList();

         for (io.karatelabs.ext.requirements.AcceptanceCriterion var5 : (Iterable<io.karatelabs.ext.requirements.AcceptanceCriterion>) (Iterable<?>) (var2)) {
            LinkedHashMap var6 = new LinkedHashMap();
            var6.put("id", var5.a());
            var6.put("text", var5.b());
            var6.put("stamp", "@req=" + var1.b() + "/" + var5.a());
            var3.add(var6);
         }

         var0.put("criteria", var3);
      }
   }

   private static Object listOutput(List<Map<String, Object>> var0, Object var1) {
      if (var1 instanceof Map var2 && var2.get("output") != null) {
         LinkedHashMap var3 = new LinkedHashMap();
         var3.put("requirements", var0);
         var3.put("count", var0.size());
         return JsApi.applyOutputConvention(var3, listMarkdown(var0), var1);
      } else {
         return var0;
      }
   }

   private static String listMarkdown(List<Map<String, Object>> var0) {
      StringBuilder var1 = new StringBuilder("# Requirements index — stamp a verdict with the `@req=` token\n\n");
      if (var0.isEmpty()) {
         return var1.append("_No requirements authored yet._\n").toString();
      } else {
         for (Map var3 : var0) {
            var1.append("- **").append(var3.get("id")).append("** — ").append(var3.get("title")).append("\n");
            Object var5 = var3.get("criteria");
            if (var5 instanceof List) {
               for (Object var6 : (List)var5) {
                  Map var7 = (Map)var6;
                  var1.append("    - `").append(var7.get("stamp")).append("` — ").append(var7.get("text")).append("\n");
               }
            } else if (var3.get("stamp") != null) {
               var1.append("    - `").append(var3.get("stamp")).append("`\n");
            }
         }

         return var1.toString();
      }
   }

   Map<String, Object> matrix(Object var1) {
      boolean var2 = var1 instanceof Map var3 && "csv".equals(var3.get("output"));
      Path var21 = this.resolveDataFile(var1);
      io.karatelabs.ext.common.jsapi.CoverageGraphAccessor.CoverageGraphLoadResult var4 = io.karatelabs.ext.common.jsapi.CoverageGraphAccessor.a(var21);
      if (var2 && var4.c() == null && !Objects.equals(var21, this.dataFile)) {
         return JsApi.dataError(
            var4,
            var21,
            "that run left no requirement graph (an unknown id, or its karate-boot.js sets no cov.requirements) — export without a run for the project graph"
         );
      } else {
         var var5 = this.mergeLiveAuthored(var4.c());
         if (var5 == null) {
            return this.notFound();
         } else {
            io.karatelabs.ext.requirements.CoverageAnalyzer.RequirementCoverageAnalysis var6 = io.karatelabs.ext.requirements.CoverageAnalyzer.a(var5);
            io.karatelabs.ext.requirements.jsapi.Requirement.CoverageTestIndex var7 = new io.karatelabs.ext.requirements.jsapi.Requirement.CoverageTestIndex(var5);
            HashMap var8 = new HashMap();

            for (Map var10 : (Iterable<Map>) (Iterable<?>) (io.karatelabs.ext.common.jsapi.CoverageGraphAccessor.a(var5, "items"))) {
               var8.putIfAbsent(String.valueOf(var10.get("id")), var10);
            }

            ArrayList var22 = new ArrayList();

            for (Map var11 : (Iterable<Map>) (Iterable<?>) (io.karatelabs.ext.requirements.RequirementsReportBuilder.e(var5))) {
               String var12 = String.valueOf(var11.get("id"));
               Map var13 = io.karatelabs.ext.requirements.RequirementsReportBuilder.a(var11, var7.b, var5);
               var13.put("testFiles", var7.a(var11.get("tests")));
               io.karatelabs.ext.requirements.RequirementsReportBuilder.a(var13, var6, var12);
               var var14 = io.karatelabs.ext.requirements.RequirementsReportBuilder.a(var5, var12);
               if (!var14.isEmpty()) {
                  for (Map var16 : (Iterable<Map>) (Iterable<?>) (var14)) {
                     ArrayList var17 = new ArrayList();
                     Map var18 = (Map)var8.get(var12 + "/" + var16.get("id"));
                     if (var18 != null && var18.get("tests") instanceof List var19) {
                        var19.forEach(var2x -> var17.add(var7.a(String.valueOf(var2x))));
                     }

                     var16.put("tests", var17);
                     var16.put("testFiles", var7.a(var18 == null ? null : var18.get("tests")));
                  }

                  var13.put("criteria", var14);
                  var13.put("acceptanceStatus", acceptanceStatus(var14));
               }

               var22.add(var13);
            }

            if (var2) {
               LinkedHashMap var25 = new LinkedHashMap();
               var25.put("csv", io.karatelabs.ext.requirements.jsapi.RequirementCsvRenderer.a(var22));
               return var25;
            } else {
               LinkedHashMap var24 = new LinkedHashMap();
               var24.put("requirements", var22);
               var24.put("runEvidence", runEvidence(var5));
               return JsApi.applyOutputConvention(var24, io.karatelabs.ext.requirements.jsapi.RequirementMatrixRenderer.a(var22), var1);
            }
         }
      }
   }

   Map<String, Object> gaps(Object var1) {
      var var2 = this.readGraph(var1);
      if (var2 == null) {
         return this.notFound();
      } else {
         var var3 = io.karatelabs.ext.requirements.RequirementsReportBuilder.e(var2);
         var var4 = io.karatelabs.ext.common.jsapi.CoverageGraphAccessor.b(var2);
         HashSet var5 = new HashSet();

         for (Map var7 : (Iterable<Map>) (Iterable<?>) (io.karatelabs.ext.common.jsapi.CoverageGraphAccessor.a(var2, "hits"))) {
            var5.add(var7.get("item"));
         }

         ArrayList var22 = new ArrayList();
         ArrayList var23 = new ArrayList();
         ArrayList var8 = new ArrayList();
         ArrayList var9 = new ArrayList();
         ArrayList var10 = new ArrayList();
         ArrayList var11 = new ArrayList();

         for (Map var13 : (Iterable<Map>) (Iterable<?>) (var3)) {
            if ("NOTCOVERED".equals(var13.get("status")) && !Boolean.TRUE.equals(var13.get("container"))) {
               var22.add(io.karatelabs.ext.requirements.RequirementsReportBuilder.a(var13, var4, var2));
            }

            String var14 = String.valueOf(var13.get("id"));
            String var15 = var14.startsWith("req:") ? var14.substring(4) : var14;
            if (Boolean.TRUE.equals(var13.get("derived"))) {
               var11.add(var15);
            }

            var var16 = io.karatelabs.ext.requirements.RequirementsReportBuilder.a(var2, var14);
            ArrayList var17 = new ArrayList();

            for (Map var19 : (Iterable<Map>) (Iterable<?>) (var16)) {
               if (Boolean.TRUE.equals(var19.get("refusalOnly"))) {
                  var9.add(var15 + "/" + var19.get("id"));
               }

               if (Boolean.TRUE.equals(var19.get("notasserted"))) {
                  var10.add(var15 + "/" + var19.get("id"));
               }

               if (!"COVERED".equals(var19.get("status"))) {
                  LinkedHashMap var20 = new LinkedHashMap();
                  var20.put("reqId", var15);
                  var20.put("title", var13.get("name"));
                  var20.put("criterion", var19.get("id"));
                  var20.put("text", var19.get("text"));
                  var20.put("status", var19.get("status"));
                  if (Boolean.TRUE.equals(var19.get("claimed"))) {
                     var20.put("claimed", true);
                     io.karatelabs.ext.requirements.RequirementsReportBuilder.a(var20, var19, "claimedBy");
                  }

                  var23.add(var20);
                  var17.add(String.valueOf(var19.get("id")));
               }
            }

            if (!var16.isEmpty() && !var17.isEmpty() && var5.contains(var14)) {
               LinkedHashMap var27 = new LinkedHashMap();
               var27.put("reqId", var15);
               var27.put("title", var13.get("name"));
               var27.put("untestedCriteria", var17);
               ArrayList var28 = new ArrayList();

               for (String var21 : (Iterable<String>) (Iterable<?>) (var17)) {
                  var28.add("@req=" + var15 + "/" + var21);
               }

               var27.put("suggestedTags", var28);
               var27.put(
                  "hint",
                  "tagged @req="
                     + var15
                     + " (requirement-level) but "
                     + var17.size()
                     + " of "
                     + var16.size()
                     + " acceptance criteria are untested; a bare @req= does not cover criteria — re-tag the verifying scenario(s) to the criterion, e.g. "
                     + (String)var28.get(0)
               );
               var8.add(var27);
            }
         }

         var var24 = notFoundLinks(var2);
         io.karatelabs.ext.requirements.CoverageAnalyzer.RequirementCoverageAnalysis var25 = io.karatelabs.ext.requirements.CoverageAnalyzer.a(var2);
         LinkedHashMap var26 = new LinkedHashMap();
         var26.put("notcovered", var22);
         var26.put("partialCriteria", var23);
         var26.put("bareParent", var8);
         var26.put("refusalOnly", var9);
         var26.put("notasserted", var10);
         var26.put("derived", var11);
         var26.put("notfound", var24);
         var26.put("orphans", var25.b());
         var26.put("drift", var25.c());
         var26.put("notresolved", var25.d());
         var26.put("incidental", var25.e());
         var26.put("runEvidence", runEvidence(var2));
         return JsApi.applyOutputConvention(var26, gapsMarkdown(var22, var23, var8, var9, var10, var24, var25), var1);
      }
   }

   private String featureOf(String var1) {
      Path var2 = this.projectRoot();
      if (var2 != null && var1.endsWith(".feature")) {
         Path var3 = var2.toAbsolutePath().normalize();

         try {
            Path var4 = MaxPaths.within(var3, var3.resolve(var1));
            return Files.isRegularFile(var4) ? var3.relativize(var4).toString().replace('\\', '/') : null;
         } catch (InvalidPathException | SecurityException var5) {
            return null;
         }
      } else {
         return null;
      }
   }

   private String checkOf(Map<String, Object> var1) {
      Path var2 = this.projectRoot();
      if (var2 != null && var1.get("id") instanceof String var3) {
         String var8 = var3.startsWith("check:") ? var3.substring("check:".length()) : var3;
         if (var8.startsWith("checks/") && var8.endsWith(".js")) {
            Path var5 = var2.resolve("checks").toAbsolutePath().normalize();

            try {
               return Files.isRegularFile(MaxPaths.within(var5, var5.resolve(var8.substring("checks/".length())))) ? var8 : null;
            } catch (InvalidPathException | SecurityException var7) {
               return null;
            }
         } else {
            return null;
         }
      } else {
         return null;
      }
   }

   Map<String, Object> delta(Object var1) {
      if (var1 instanceof Map var2) {
         var var3 = this.scope(var2.get("from"), "from");
         if (var3.containsKey("error")) {
            return var3;
         } else {
            var var4 = this.scope(var2.get("to"), "to");
            if (var4.containsKey("error")) {
               return var4;
            } else {
               String var5 = this.runKind(var3);
               if (var5 == null) {
                  return this.runFree(var3, "from");
               } else {
                  String var6 = this.runKind(var4);
                  if (var6 == null) {
                     return this.runFree(var4, "to");
                  } else if (!var5.equals(var6)) {
                     return JsApi.missing(
                        "a delta compares two scopes of the SAME kind, but from is "
                           + var5
                           + " and to is "
                           + var6
                           + " — a dry run declares, an executed run verifies, so nothing read across that line is a change in coverage. Re-run the dry side for real, or compare it against another dry run."
                     );
                  } else if (this.sides == null) {
                     return JsApi.missing(
                        "no scope builder is bound in this context — a delta composes each side through the project's builder (the run's records plus the current oracle producer), which the serve profile binds"
                     );
                  } else {
                     var var7 = this.compose(var3, "from");
                     if (var7.containsKey("error")) {
                        return var7;
                     } else {
                        var var8 = this.compose(var4, "to");
                        if (var8.containsKey("error")) {
                           return var8;
                        } else {
                           LinkedHashMap var9 = new LinkedHashMap();
                           var9.put("measures", "dry".equals(var5) ? "claims" : "evidence");
                           var9.put("requirementsAsOf", "current");
                           var9.put("oracle", "project");
                           var9.put("from", side(var3, var5));
                           var9.put("to", side(var4, var6));
                           var9.putAll(
                              io.karatelabs.ext.requirements.jsapi.RequirementDeltaAnalyzer.a(
                                 this.mergeLiveAuthored(var7), this.mergeLiveAuthored(var8), this.currentRequirementIds(), "dry".equals(var5)
                              )
                           );
                           return JsApi.next(var9, Boolean.TRUE.equals(var9.get("improved")) ? "Requirement.readiness()" : "Requirement.matrix()");
                        }
                     }
                  }
               }
            }
         }
      } else {
         return JsApi.missing(
            "Requirement.delta({from, to}) compares TWO explicit run scopes, each {run:'<id>'} or {reportDir:'<path>'} — there is no default and no 'previous run', because only the caller knows which two runs are comparable"
         );
      }
   }

   private Map<String, Object> compose(Map<String, Object> var1, String var2) {
      Path var3 = this.evidenceDir(var1);
      if (var3 == null) {
         return JsApi.missing(
            "'"
               + var2
               + "' ("
               + describe(var1)
               + ") carries no evidence stream to re-read — no karate-json/karate-events.jsonl and no transcript.jsonl beside its run record. A delta side is built from the run's records, never read from a serialized graph."
         );
      } else {
         var var4 = this.sides.apply(var3);
         if (var4.get("error") != null) {
            return JsApi.missing("'" + var2 + "' (" + describe(var1) + ") could not be composed: " + var4.get("error"));
         } else {
            return var4.get("selection") instanceof Map var5 && var5.get("producerRefused") instanceof List var6 && !var6.isEmpty()
               ? JsApi.missing(
                  "the project's run-free oracle projection (Rule.cover) is what puts the estate's rules-only before-state on both sides, so without it a clearance cannot be told from a gap and the delta is refused rather than graded against the wrong baseline: "
                     + var6
               )
               : var4;
         }
      }
   }

   private static String describe(Map<String, Object> var0) {
      return var0.containsKey("run") ? "run " + var0.get("run") : "reportDir " + var0.get("reportDir");
   }

   private static boolean readable(Path var0) {
      return Files.isRegularFile(MaxPaths.eventsFile(var0)) || Files.isRegularFile(var0.resolve("transcript.jsonl"));
   }

   private Path evidenceDir(Map<String, Object> var1) {
      Path var2 = this.reportRoot(var1);
      var var3 = readMeta(var2.resolve("ext/coverage/run.json"));
      if (var3 != null && var3.get("runId") != null) {
         if (this.runsDir != null && Files.isDirectory(this.runsDir.resolve(String.valueOf(var3.get("runId"))))) {
            Path var5 = this.runsDir.resolve(String.valueOf(var3.get("runId")));
            return readable(var5) ? var5 : null;
         } else {
            return readable(var2) ? var2 : null;
         }
      } else if (readMeta(var2.resolve("meta.json")) != null) {
         return readable(var2) ? var2 : null;
      } else if (this.runsDir != null && var2.getParent() != null && "rtm".equals(String.valueOf(var2.getParent().getFileName()))) {
         Path var4 = this.runsDir.resolve(String.valueOf(var2.getFileName()));
         return readable(var4) ? var4 : null;
      } else {
         return null;
      }
   }

   private Map<String, Object> scope(Object var1, String var2) {
      if (var1 instanceof Map var3) {
         LinkedHashMap var4 = new LinkedHashMap();
         Object var5 = var3.get("run");
         Object var6 = var3.get("reportDir");
         if (var5 != null && !String.valueOf(var5).isBlank()) {
            if (this.runsDir == null) {
               return JsApi.missing("'" + var2 + "' names {run:'" + var5 + "'} but this context has no runs/ store — pass {reportDir:'<path>'} instead");
            } else {
               var4.put("run", String.valueOf(var5));
               return var4;
            }
         } else if (var6 != null && !String.valueOf(var6).isBlank()) {
            var4.put("reportDir", String.valueOf(var6));
            return var4;
         } else {
            return JsApi.missing(
               "'"
                  + var2
                  + "' is not a run scope. Requirement.delta({from, to}) compares TWO explicit run scopes, each {run:'<id>'} or {reportDir:'<path>'} — there is no default and no 'previous run', because only the caller knows which two runs are comparable"
            );
         }
      } else {
         return JsApi.missing(
            "Requirement.delta is missing '"
               + var2
               + "'. Requirement.delta({from, to}) compares TWO explicit run scopes, each {run:'<id>'} or {reportDir:'<path>'} — there is no default and no 'previous run', because only the caller knows which two runs are comparable"
         );
      }
   }

   private Set<String> currentRequirementIds() {
      var var1 = this.homes.stream().filter(var0 -> Files.isDirectory(var0)).toList();
      if (var1.isEmpty()) {
         return null;
      } else {
         io.karatelabs.ext.requirements.model.RequirementsBundle var2;
         try {
            var2 = new RequirementsRepository(var1, this.projectRoot()).a();
         } catch (RuntimeException var8) {
            return null;
         }

         if (var2 == null) {
            return null;
         } else {
            LinkedHashSet var3 = new LinkedHashSet();

            for (io.karatelabs.ext.requirements.model.RequirementNode var5 : var2.d()) {
               var3.add("req:" + var5.b());

               for (io.karatelabs.ext.requirements.AcceptanceCriterion var7 : (Iterable<io.karatelabs.ext.requirements.AcceptanceCriterion>) (Iterable<?>) (io.karatelabs.ext.requirements.AcceptanceCriterion.a(var5.m()))) {
                  var3.add("req:" + var5.b() + "/" + var7.a());
               }
            }

            return var3;
         }
      }
   }

   private static Map<String, Object> side(Map<String, Object> var0, String var1) {
      var var2 = new LinkedHashMap<>(var0);
      var2.put("kind", var1);
      return var2;
   }

   private Map<String, Object> runFree(Map<String, Object> var1, String var2) {
      return JsApi.missing(
         "'"
            + var2
            + "' ("
            + (var1.containsKey("run") ? "run " + var1.get("run") : "reportDir " + var1.get("reportDir"))
            + ") carries no run record at "
            + this.reportRoot(var1).resolve("meta.json")
            + ", so its kind is unknown. A delta side is one run: {run:'<id>'}, or a {reportDir} Report.aggregate({run:'<id>'}) wrote for one. A run-free aggregate — the project graph, a Rule.cover projection — is not a side, because it is neither a dry nor an executed run."
      );
   }

   private Path reportRoot(Map<String, Object> var1) {
      return this.resolveDataFile(var1).getParent().getParent().getParent();
   }

   private String runKind(Map<String, Object> var1) {
      Path var2 = this.reportRoot(var1);
      var var3 = readMeta(var2.resolve("ext/coverage/run.json"));
      if (var3 == null) {
         var3 = readMeta(var2.resolve("meta.json"));
      }

      if (var3 == null && this.runsDir != null && var2.getParent() != null && "rtm".equals(String.valueOf(var2.getParent().getFileName()))) {
         var3 = readMeta(this.runsDir.resolve(String.valueOf(var2.getFileName())).resolve("meta.json"));
      }

      if (var3 == null) {
         return null;
      } else {
         return Boolean.TRUE.equals(var3.get("dryRun")) ? "dry" : "executed";
      }
   }

   private static Map<String, Object> readMeta(Path var0) {
      if (!Files.isRegularFile(var0)) {
         return null;
      } else {
         try {
            return Json.of(Files.readString(var0)).value() instanceof Map var2 ? var2 : null;
         } catch (Exception var3) {
            return null;
         }
      }
   }

   Map<String, Object> get(String var1) {
      var var2 = this.readGraph();
      if (var2 == null) {
         return this.notFound();
      } else if (var1 == null) {
         return JsApi.missing("Requirement.get(id) requires a requirement id, e.g. 'ORD-001'");
      } else {
         String var3 = var1.startsWith("req:") ? var1 : "req:" + var1;
         ArrayList var4 = new ArrayList();

         for (Map var6 : (Iterable<Map>) (Iterable<?>) (io.karatelabs.ext.requirements.RequirementsReportBuilder.e(var2))) {
            if (var3.equals(var6.get("id"))) {
               Map var12 = io.karatelabs.ext.requirements.RequirementsReportBuilder.a(var6, io.karatelabs.ext.common.jsapi.CoverageGraphAccessor.b(var2), var2);
               var var8 = io.karatelabs.ext.requirements.RequirementsReportBuilder.a(var2, var3);
               String var9 = var3.substring(4);
               if (!var8.isEmpty()) {
                  for (Map var11 : (Iterable<Map>) (Iterable<?>) (var8)) {
                     var11.put("stamp", "@req=" + var9 + "/" + var11.get("id"));
                  }

                  var12.put("criteria", var8);
                  var12.put("acceptanceStatus", acceptanceStatus(var8));
               } else {
                  var12.put("stamp", "@req=" + var9);
               }

               return var12;
            }

            String var7 = String.valueOf(var6.get("id"));
            var4.add(var7.startsWith("req:") ? var7.substring(4) : var7);
         }

         return JsApi.suggest(
            JsApi.notFound("no such requirement: " + var1), io.karatelabs.ext.common.jsapi.SuggestionEngine.a(var1.startsWith("req:") ? var1.substring(4) : var1, var4)
         );
      }
   }

   Map<String, Object> load(String var1, Object var2) {
      List var3 = var1 != null ? List.of(this.resolveLoadTarget(var1)) : this.homes;
      if (var3.isEmpty()) {
         return JsApi.missing("Requirement.load(path) requires a requirements file or directory");
      } else {
         try {
            io.karatelabs.ext.requirements.model.RequirementsBundle var4 = this.loadDoc(var3);
            return JsApi.applyOutputConvention(var4.a(), io.karatelabs.ext.requirements.RequirementsYamlWriter.a(var4), var2);
         } catch (IOException var5) {
            return Map.of(
               "error",
               "cannot read "
                  + join(var3)
                  + ": "
                  + var5.getMessage()
                  + " — a bare name is a file in the requirements home ("
                  + this.homePaths()
                  + "); a path is a reference (a leading '/' anchors the project root)",
               "code",
               "NOT_FOUND",
               "status",
               404
            );
         } catch (RuntimeException var6) {
            return JsApi.error(var6);
         }
      }
   }

   Map<String, Object> read(String var1, Object var2) {
      if (var1 == null || var1.isBlank()) {
         return JsApi.missing("Requirement.read(file[, {home}]) requires a requirements file, e.g. Requirement.read('requirements/orders.md')");
      } else if (this.homes.isEmpty()) {
         return JsApi.missing("no requirements home configured for this run — nothing to read");
      } else {
         io.karatelabs.ext.requirements.jsapi.Requirement.RequirementTarget var3 = this.target(var1, var2);
         if (var3.d() != null) {
            return var3.d();
         } else {
            Path var4 = var3.b();
            if (!Files.isRegularFile(var4)) {
               return JsApi.notFound(
                  "no requirements file "
                     + var3.c()
                     + " — pass the file a Requirement.list() row names, as it is (e.g. '"
                     + this.homeRef(var3.a())
                     + "/orders.md')"
               );
            } else {
               try {
                  byte[] var5 = Files.readAllBytes(var4);
                  LinkedHashMap var6 = new LinkedHashMap();
                  var6.put("path", var4.toString());
                  var6.put("file", var3.c());
                  var6.put("home", this.homeRef(var3.a()));
                  var6.put("content", new String(var5, StandardCharsets.UTF_8));
                  var6.put("mtime", io.karatelabs.ext.common.Sha256.a(var5));
                  return var6;
               } catch (IOException var7) {
                  return Map.of("error", "cannot read " + var4 + ": " + var7.getMessage(), "code", "IO_ERROR", "status", 500);
               }
            }
         }
      }
   }

   Map<String, Object> write(String var1, Object var2, Object var3) {
      if (var1 == null || var2 == null) {
         return JsApi.missing("Requirement.write(name, markdown) requires a file name and the markdown text");
      } else if (!(var2 instanceof String var4)) {
         return JsApi.missing(
            "Requirement.write: markdown is the file's source as a string, e.g. \"#### ORD-001: <title>\\n**Acceptance:**\\n- WHEN … THE SYSTEM SHALL …\""
         );
      } else if (this.homes.isEmpty()) {
         return JsApi.missing("no requirements home configured for this run — cannot author requirements");
      } else {
         io.karatelabs.ext.requirements.jsapi.Requirement.RequirementTarget var5 = this.target(var1, var3);
         if (var5.d() != null) {
            return var5.d();
         } else {
            io.karatelabs.ext.requirements.model.RequirementsBundle var6 = io.karatelabs.ext.requirements.RequirementsMarkdownParser.a(var4);
            if (var6.d().isEmpty()) {
               return Map.of(
                  "error",
                  "no requirements parsed from the markdown — expected `#### <ID>: <title>` headings, each with a `**Acceptance:**` list (see the `requirements` skill)",
                  "code",
                  "PARSE_FAILED",
                  "status",
                  400
               );
            } else {
               ArrayList var7 = new ArrayList();

               for (io.karatelabs.ext.requirements.model.RequirementNode var9 : var6.d()) {
                  var7.add(var9.b());
               }

               if (var3 instanceof Map var17 && Boolean.TRUE.equals(var17.get("dryRun"))) {
                  return this.dryRun(var5.c(), var6, var7);
               } else {
                  Path var18 = var5.b();
                  String var19 = var3 instanceof Map var11 && var11.get("ifMatch") instanceof String var10 ? var10 : null;
                  synchronized (WRITE_LOCKS.computeIfAbsent(MaxPaths.lockKey(var18), var0 -> new Object())) {
                     LinkedHashMap var28;
                     try {
                        if (var3 instanceof Map var20 && Boolean.FALSE.equals(var20.get("overwrite")) && Files.exists(var18)) {
                           LinkedHashMap var26 = new LinkedHashMap();
                           var26.put("path", var18.toString());
                           var26.put("file", var5.c());
                           var26.put("mtime", io.karatelabs.ext.common.Sha256.a(Files.readAllBytes(var18)));
                           LinkedHashMap var27 = new LinkedHashMap();
                           var27.put("error", var5.c() + " already exists — pass {overwrite:true} to replace it");
                           var27.put("code", "EXISTS");
                           var27.put("status", 409);
                           var27.put("detail", var26);
                           return var27;
                        }

                        if (var19 != null && !var19.isBlank()) {
                           if (!Files.exists(var18)) {
                              LinkedHashMap var23 = new LinkedHashMap();
                              var23.put("path", var18.toString());
                              var23.put("expectedMtime", var19);
                              return Map.of(
                                 "error",
                                 "stale write at " + var5.c() + ": expected sha=" + var19 + ", the file no longer exists",
                                 "code",
                                 "STALE_MTIME",
                                 "status",
                                 409,
                                 "detail",
                                 var23
                              );
                           }

                           byte[] var21 = Files.readAllBytes(var18);
                           String var24 = io.karatelabs.ext.common.Sha256.a(var21);
                           if (!var24.equals(var19)) {
                              LinkedHashMap var13 = new LinkedHashMap();
                              var13.put("path", var18.toString());
                              var13.put("expectedMtime", var19);
                              var13.put("currentMtime", var24);
                              var13.put("currentBody", new String(var21, StandardCharsets.UTF_8));
                              return Map.of(
                                 "error",
                                 "stale write at " + var5.c() + ": expected sha=" + var19 + ", current=" + var24,
                                 "code",
                                 "STALE_MTIME",
                                 "status",
                                 409,
                                 "detail",
                                 var13
                              );
                           }
                        }

                        Files.createDirectories(var18.getParent());
                        byte[] var22 = var4.getBytes(StandardCharsets.UTF_8);
                        io.karatelabs.ext.common.AtomicFileWriter.a(var18, var22);
                        LinkedHashMap var25 = new LinkedHashMap();
                        var25.put("path", var18.toString());
                        var25.put("file", var5.c());
                        var25.put("count", var7.size());
                        var25.put("ids", var7);
                        var25.put("mtime", io.karatelabs.ext.common.Sha256.a(var22));
                        var28 = var25;
                     } catch (IOException var15) {
                        return Map.of("error", "cannot write " + var18 + ": " + var15.getMessage(), "code", "IO_ERROR", "status", 500);
                     }

                     return var28;
                  }
               }
            }
         }
      }
   }

   // $VF: One or more variable merging failures!
   // $VF: Could not properly define all variable types!
   // Please report this to the Vineflower issue tracker, at https://github.com/Vineflower/vineflower/issues with a copy of the class file (if you have the rights to distribute it!)
   private Map<String, Object> dryRun(String var1, io.karatelabs.ext.requirements.model.RequirementsBundle var2, List<String> var3) {
      ArrayList var4 = new ArrayList();
      int var5 = 0;

      for (io.karatelabs.ext.requirements.model.RequirementNode var7 : var2.d()) {
         LinkedHashMap var8 = new LinkedHashMap();
         var8.put("id", var7.b());
         var8.put("title", var7.d() != null ? var7.d() : var7.b());
         putIfSet(var8, "criticality", var7.h());
         var8.put("line", var7.r());
         putCriteria(var8, var7);
         var5 += var8.get("criteria") instanceof List var9 ? var9.size() : 0;
         var4.add(var8);
      }

      var var15 = new ArrayList<>(var2.d());
      Path var16 = this.projectRoot();
      if (var16 != null && this.homes.stream().anyMatch(var0 -> Files.isDirectory(var0))) {
         try {
            RequirementsRepository var17 = new RequirementsRepository(this.homes, var16);

            for (io.karatelabs.ext.requirements.model.RequirementNode var21 : var17.a().d()) {
               if (!var1.equals(var17.a(var21))) {
                  var15.add(var21);
               }
            }
         } catch (RuntimeException var14) {
         }
      }

      ArrayList var18 = new ArrayList();

      Object findingsVal = RequirementLinter.a(io.karatelabs.ext.requirements.model.RequirementsBundle.a(var15, var2.e())).get("findings");

      for (Object var24 : findingsVal instanceof List ? (List)findingsVal : List.of()) {
         if (var24 instanceof Map var12 && (var12.get("id") == null || var3.contains(String.valueOf(var12.get("id"))))) {
            var18.add(var12);
         }
      }

      LinkedHashMap var23 = new LinkedHashMap();
      var23.put("dryRun", true);
      var23.put("file", var1);
      var23.put("count", var3.size());
      var23.put("criteria", var5);
      var23.put("requirements", var4);
      var23.put("findings", var18);
      return var23;
   }

   private String homeRef(Path var1) {
      Path var2 = this.projectRoot();
      Path var3 = var1.toAbsolutePath().normalize();
      return (var2 != null && var3.startsWith(var2.toAbsolutePath().normalize()) ? var2.toAbsolutePath().normalize().relativize(var3) : var3)
         .toString()
         .replace('\\', '/');
   }

   private io.karatelabs.ext.requirements.jsapi.Requirement.RequirementTarget target(String var1, Object var2) {
      Path var3 = this.resolveWriteHome(var2);
      if (var3 == null) {
         return new io.karatelabs.ext.requirements.jsapi.Requirement.RequirementTarget(
            null,
            null,
            null,
            Map.of(
               "error",
               "unknown requirements home '" + strOpt(var2, "home") + "' — configured homes are " + this.homePaths(),
               "code",
               "NOT_FOUND",
               "status",
               404
            )
         );
      } else {
         String var4 = MaxPaths.stripLeadingSlashes(var1.replace('\\', '/'));
         var4 = var4.endsWith(".md") ? var4 : var4 + ".md";
         if (!var4.equals(".md") && !var4.endsWith("/.md")) {
            String var5 = strOpt(var2, "home");
            boolean var6 = var5 != null && !var5.isBlank();

            try {
               Path var7 = null;
               Path var8 = null;

               for (Path var10 : var6 ? List.of(var3) : this.homes) {
                  Path var11 = var10.toAbsolutePath().normalize();
                  Path var12 = this.projectRoot() != null ? this.projectRoot().toAbsolutePath().normalize() : var11.getParent();
                  Path var13 = var12 == null ? null : var12.resolve(var4).normalize();
                  if (var13 != null && var13.startsWith(var11) && !var13.equals(var11)) {
                     var7 = var13;
                     var8 = var10;
                     break;
                  }
               }

               Path var17 = null;

               try {
                  var17 = MaxPaths.within(var3, var3.resolve(var4));
               } catch (SecurityException var14) {
               }

               Path var18 = var6 ? var17 : var7;
               Path var19 = var6 ? var7 : var17;
               Path var20 = var18 == null || var19 != null && !Files.exists(var18) && Files.exists(var19) ? var19 : var18;
               if (var20 != null) {
                  Path var21 = var20 == var7 ? var8 : var3;
                  return new io.karatelabs.ext.requirements.jsapi.Requirement.RequirementTarget(var21, var20, io.karatelabs.ext.requirements.RequirementsRepository.a(this.projectRoot(), var21, var20), null);
               }
            } catch (InvalidPathException var15) {
            }
         }

         return new io.karatelabs.ext.requirements.jsapi.Requirement.RequirementTarget(
            null,
            null,
            null,
            Map.of(
               "error",
               "invalid requirements file name '"
                  + var1
                  + "' — a file inside a requirements home ("
                  + this.homePaths()
                  + "), named as Requirement.list() names it",
               "code",
               "PARSE_FAILED",
               "status",
               400
            )
         );
      }
   }

   private Path resolveLoadTarget(String var1) {
      boolean var2 = var1.indexOf(47) < 0 && var1.indexOf(92) < 0 && var1.indexOf(58) < 0;
      if (var2 && !this.homes.isEmpty()) {
         for (Path var4 : this.homes) {
            Path var5 = var4.resolve(var1);
            if (Files.exists(var5)) {
               return var5;
            }
         }

         return this.homes.get(0).resolve(var1);
      } else {
         return MaxPaths.resolvePath(this.projectRoot(), var1);
      }
   }

   private Path resolveWriteHome(Object var1) {
      String var2 = strOpt(var1, "home");
      if (var2 != null && !var2.isBlank()) {
         String var3 = var2.replace('\\', '/').replaceAll("^/+", "").replaceAll("/+$", "");

         for (Path var5 : this.homes) {
            String var6 = var5.toAbsolutePath().normalize().toString().replace('\\', '/');
            if (var6.equals(var3) || var6.endsWith("/" + var3) || var5.getFileName().toString().equals(var3)) {
               return var5;
            }
         }

         return null;
      } else {
         return this.homes.get(0);
      }
   }

   private static String strOpt(Object var0, String var1) {
      return var0 instanceof Map var3 && var3.get(var1) instanceof String var2 ? var2 : null;
   }

   private Path projectRoot() {
      if (this.projectRoot != null) {
         return this.projectRoot;
      } else {
         return this.homes.isEmpty() ? null : this.homes.get(0).getParent();
      }
   }

   private String homePaths() {
      return join(this.homes);
   }

   private static String join(List<Path> var0) {
      return var0.stream().map(String::valueOf).collect(Collectors.joining(", "));
   }

   Map<String, Object> lint(Object var1) {
      if (this.homes.isEmpty()) {
         return JsApi.missing("Requirement.lint() needs a requirements home — set req.home in karate-boot.js (in-suite) or --requirements-dir (serve)");
      } else {
         try {
            var var2 = io.karatelabs.ext.requirements.jsapi.RequirementLinter.a(this.loadDoc(this.homes));
            return JsApi.applyOutputConvention(var2, io.karatelabs.ext.requirements.jsapi.RequirementLinter.a(var2), var1);
         } catch (IOException var3) {
            return Map.of("error", "cannot read " + this.homePaths() + ": " + var3.getMessage(), "code", "NOT_FOUND", "status", 404);
         } catch (RuntimeException var4) {
            return JsApi.error(var4);
         }
      }
   }

   Map<String, Object> readiness(Object var1) {
      String var2 = this.resolveDataFile(var1) == this.dataFile ? this.readStamp() : null;
      io.karatelabs.ext.requirements.jsapi.Requirement.ReadinessCacheEntry var3 = this.readinessKept;
      Map var4;
      if (var2 != null && var3 != null && var3.a().equals(var2)) {
         var4 = detached(var3.b());
      } else {
         var var5 = this.readGraph(var1);
         if (var5 == null) {
            return this.notFound();
         }

         var4 = readinessData(var5);
         if (var2 != null) {
            this.readinessKept = new io.karatelabs.ext.requirements.jsapi.Requirement.ReadinessCacheEntry(var2, detached(var4));
         }
      }

      return JsApi.applyOutputConvention(var4, io.karatelabs.ext.requirements.ReadinessEvaluator.b(var4), var1);
   }

   private String readStamp() {
      ArrayList var1 = new ArrayList();
      if (this.dataFile != null) {
         var1.add(this.dataFile);
      }

      for (Path var3 : this.homes) {
         if (Files.isDirectory(var3)) {
            try (var var4 = Files.walk(var3)) {
               var4.filter(var0 -> Files.isRegularFile(var0) && var0.getFileName().toString().toLowerCase().endsWith(".md")).sorted().forEach(var1::add);
            } catch (UncheckedIOException | IOException var9) {
               return null;
            }
         }
      }

      return io.karatelabs.ext.common.Sha256.a(var1);
   }

   private static <T> T detached(T var0) {
      if (var0 instanceof Map var3) {
         LinkedHashMap var4 = new LinkedHashMap();
         var3.forEach((var1x, var2x) -> var4.put(String.valueOf(var1x), detached(var2x)));
         return (T)var4;
      } else if (var0 instanceof List var1) {
         ArrayList var2 = new ArrayList(var1.size());
         var1.forEach(var1x -> var2.add(detached(var1x)));
         return (T)var2;
      } else {
         return (T)var0;
      }
   }

   public static Map<String, Object> readinessData(Map<String, Object> var0) {
      var var1 = io.karatelabs.ext.requirements.RequirementsReportBuilder.b(var0);
      var var2 = io.karatelabs.ext.requirements.ReadinessEvaluator.a(var1);
      var2.put("totals", totals(var0, var1));
      var2.put("assertionStrength", io.karatelabs.ext.requirements.RequirementsReportBuilder.d(var0));
      var2.put("runEvidence", runEvidence(var0));
      if (var0.get("selection") instanceof Map var3) {
         var2.put("selection", var3);
      }

      return var2;
   }

   private static Map<String, Object> totals(Map<String, Object> var0, List<Map<String, Object>> var1) {
      var var2 = statusCounts();
      var var3 = statusCounts();

      for (Map var5 : var1) {
         countStatus(var2, var5.get("status"));

         for (Map var7 : io.karatelabs.ext.requirements.RequirementsReportBuilder.a(var0, "req:" + var5.get("reqId"))) {
            countStatus(var3, var7.get("status"));
         }
      }

      LinkedHashMap var8 = new LinkedHashMap();
      var8.put("requirements", var2);
      var8.put("criteria", var3);
      return var8;
   }

   private static Map<String, Integer> statusCounts() {
      LinkedHashMap var0 = new LinkedHashMap();
      STATUS_KEYS.forEach(var1 -> var0.put(var1, 0));
      var0.put("total", 0);
      return var0;
   }

   private static void countStatus(Map<String, Integer> var0, Object var1) {
      String var2 = String.valueOf(var1).toLowerCase(Locale.ROOT);
      if (STATUS_KEYS.contains(var2)) {
         var0.merge(var2, 1, Integer::sum);
      }

      var0.merge("total", 1, Integer::sum);
   }

   public static String readinessMarkdown(Map<String, Object> var0) {
      return io.karatelabs.ext.requirements.ReadinessEvaluator.b(var0);
   }

   public static String readinessReason(Map<?, ?> var0) {
      return io.karatelabs.ext.requirements.ReadinessEvaluator.a(var0);
   }

   public static Map<String, List<Map<String, Object>>> criteriaData(Map<String, Object> var0) {
      LinkedHashMap var1 = new LinkedHashMap();

      for (Map var3 : io.karatelabs.ext.requirements.RequirementsReportBuilder.e(var0)) {
         String var4 = String.valueOf(var3.get("id"));
         var var5 = io.karatelabs.ext.requirements.RequirementsReportBuilder.a(var0, var4);
         if (!var5.isEmpty()) {
            var1.put(var4, var5);
         }
      }

      return var1;
   }

   public Map<String, Object> traceabilityData() {
      var var1 = this.readGraph();
      if (var1 == null) {
         return null;
      } else {
         var var2 = io.karatelabs.ext.requirements.RequirementsReportBuilder.b(var1);
         LinkedHashMap var3 = new LinkedHashMap();
         var3.put("readiness", io.karatelabs.ext.requirements.ReadinessEvaluator.a(var2));
         var3.put("requirements", var2);
         return var3;
      }
   }

   public Map<String, Object> traceabilityPayload() {
      var var1 = this.readGraph();
      return var1 == null ? null : traceabilityPayload(var1, this.dataFile.getParent().getParent().getParent());
   }

   public static Map<String, Object> traceabilityPayload(Map<String, Object> var0, Path var1) {
      return io.karatelabs.ext.requirements.RequirementsReportBuilder.a(var0, var1);
   }

   public static Map<String, Object> providerLinksAt(Path var0) {
      return io.karatelabs.ext.requirements.RequirementsReportBuilder.a(var0);
   }

   public static Map<String, Object> readinessFor(Map<String, Object> var0) {
      return io.karatelabs.ext.requirements.RequirementsReportBuilder.a(var0);
   }

   public static Map<String, Object> assertionStrength(Map<String, Object> var0) {
      return io.karatelabs.ext.requirements.RequirementsReportBuilder.d(var0);
   }

   Map<String, Object> posture(Object var1) {
      var var2 = this.readGraph();
      if (var2 == null) {
         return this.notFound();
      } else {
         Object var3 = var1 instanceof Map var4 ? var4.get("policy") : null;
         var var5 = io.karatelabs.ext.requirements.jsapi.RequirementPostureAnalyzer.a(io.karatelabs.ext.requirements.RequirementsReportBuilder.b(var2), var3);
         return JsApi.applyOutputConvention(var5, io.karatelabs.ext.requirements.jsapi.RequirementPostureAnalyzer.a(var5), var1);
      }
   }

   private io.karatelabs.ext.requirements.model.RequirementsBundle loadDoc(List<Path> var1) throws IOException {
      return var1.size() == 1 && !Files.isDirectory((Path)var1.get(0))
         ? io.karatelabs.ext.requirements.RequirementsMarkdownParser.a(Files.readString((Path)var1.get(0)))
         : new RequirementsRepository(var1, this.projectRoot()).a();
   }

   private static Map<String, Object> runEvidence(Map<String, Object> var0) {
      LinkedHashMap var1 = new LinkedHashMap();
      int var2 = (int)io.karatelabs.ext.common.jsapi.CoverageGraphAccessor.a(var0, "tests").stream().filter(var0x -> !io.karatelabs.ext.requirements.RequirementsReportBuilder.c((Map<String, Object>)var0x)).count();
      var1.put("tests", var2);
      var1.put("assertionStrength", io.karatelabs.ext.requirements.RequirementsReportBuilder.d(var0));
      if (var0.get("selection") instanceof Map var3) {
         var1.put("selection", var3);
      }

      if (var2 == 0) {
         var1.put(
            "note",
            "no run is in this graph yet — every status here means 'nothing has run', NOT 'a run checked this and missed'. Run the suite (e.g. Runner.run('.')), then read again."
         );
      }

      return var1;
   }

   private static List<Map<String, Object>> notFoundLinks(Map<String, Object> var0) {
      HashSet var1 = new HashSet();

      for (Map var3 : io.karatelabs.ext.common.jsapi.CoverageGraphAccessor.a(var0, "items")) {
         if (var3.get("id") instanceof String var5 && var5.startsWith("req:")) {
            var1.add(var5);
         }
      }

      ArrayList var8 = new ArrayList();

      for (Map var10 : io.karatelabs.ext.common.jsapi.CoverageGraphAccessor.a(var0, "hits")) {
         if ("DECLARED".equals(var10.get("kind")) && var10.get("item") instanceof String var6 && var6.startsWith("req:") && !var1.contains(var6)) {
            LinkedHashMap var7 = new LinkedHashMap();
            var7.put("item", var6);
            var7.put("test", var10.get("test"));
            var8.add(var7);
         }
      }

      return var8;
   }

   private static String gapsMarkdown(
      List<Map<String, Object>> var0,
      List<Map<String, Object>> var1,
      List<Map<String, Object>> var2,
      List<String> var3,
      List<String> var4,
      List<Map<String, Object>> var5,
      io.karatelabs.ext.requirements.CoverageAnalyzer.RequirementCoverageAnalysis var6
   ) {
      StringBuilder var7 = new StringBuilder("# Requirement gaps\n\n");
      var7.append("## Not covered (no passing test)\n\n");
      if (var0.isEmpty()) {
         var7.append("_None — every requirement has a covering test._\n");
      } else {
         for (Map var9 : var0) {
            var7.append("- **").append(var9.get("reqId")).append("** ").append(var9.get("title")).append("\n");
         }
      }

      if (!var2.isEmpty()) {
         var7.append("\n## Tagged at requirement-level, but has untested criteria (re-tag to the criterion)\n\n");

         for (Map var19 : var2) {
            var7.append("- **").append(var19.get("reqId")).append("** ").append(var19.get("title")).append(" — ").append(var19.get("hint")).append("\n");
         }
      }

      if (!var1.isEmpty()) {
         var7.append("\n## Untested acceptance criteria\n\n");

         for (Map var20 : var1) {
            var7.append("- **")
               .append(var20.get("reqId"))
               .append('/')
               .append(var20.get("criterion"))
               .append("** (")
               .append(var20.get("status"))
               .append(") ")
               .append(var20.get("text"))
               .append("\n");
         }
      }

      if (!var3.isEmpty()) {
         var7.append("\n## Covered by refusal only (no positive evidence reached these)\n\n");

         for (String var21 : var3) {
            var7.append("- **").append(var21).append("**\n");
         }
      }

      if (!var4.isEmpty()) {
         var7.append("\n## Covered but notasserted (the covering tests checked nothing)\n\n");

         for (String var22 : var4) {
            var7.append("- **").append(var22).append("**\n");
         }
      }

      if (!var5.isEmpty()) {
         var7.append("\n## Not-found links (`@req=` to a missing requirement)\n\n");

         for (Map var23 : var5) {
            var7.append("- ").append(var23.get("item")).append(" ← ").append(var23.get("test")).append("\n");
         }
      }

      if (!var6.e().isEmpty()) {
         var7.append("\n## Impl-shaped coverage (credited via `@real=`, no `@req=` anchor)\n\n");

         for (Map var24 : var6.e()) {
            var7.append("- **")
               .append(var24.get("reqId"))
               .append("** ")
               .append(var24.get("title"))
               .append(" — exercised via ")
               .append(var24.get("real"))
               .append("; add a `@req=")
               .append(var24.get("reqId"))
               .append("` test to anchor intent\n");
         }
      }

      if (!var6.c().isEmpty()) {
         var7.append("\n## Drift (`@req=`-declared, but no `@real=` artifact exercised)\n\n");

         for (Map var25 : var6.c()) {
            var7.append("- **")
               .append(var25.get("reqId"))
               .append("** ")
               .append(var25.get("title"))
               .append(" — declared real artifacts ")
               .append(var25.get("real"))
               .append(" went unexercised\n");
         }
      }

      if (!var6.d().isEmpty()) {
         var7.append("\n## Unresolved `@real=` (target names no known artifact — a typo)\n\n");

         for (Map var26 : var6.d()) {
            var7.append("- **")
               .append(var26.get("reqId"))
               .append("** ")
               .append(var26.get("title"))
               .append(" — `@real=`")
               .append(var26.get("real"))
               .append(" matches no artifact in the graph;")
               .append(" fix the id (namespaced + space-free, e.g. `openapi:<opId>`) or remove the tag\n");
         }
      }

      if (!var6.b().isEmpty()) {
         var7.append("\n## Orphans (exercised, but realising no requirement)\n\n");

         for (Map var27 : var6.b()) {
            var7.append("- ").append(var27.get("id")).append(" (").append(var27.get("status")).append(")\n");
         }
      }

      return var7.toString();
   }

   private Map<String, Object> readGraph() {
      return this.readGraph(null);
   }

   private Map<String, Object> readGraph(Object var1) {
      return this.mergeLiveAuthored(io.karatelabs.ext.common.jsapi.CoverageGraphAccessor.a(this.resolveDataFile(var1)).c());
   }

   private Path resolveDataFile(Object var1) {
      if (var1 instanceof Map var2) {
         Object var3 = var2.get("run");
         if (var3 != null && this.runsDir != null) {
            return this.runsDir.resolve(String.valueOf(var3)).resolve("ext/coverage/coverage-data.json");
         } else {
            Object var4 = var2.get("reportDir");
            return var4 != null ? MaxPaths.resolvePath(this.projectRoot(), String.valueOf(var4)).resolve("ext/coverage/coverage-data.json") : this.dataFile;
         }
      } else {
         return this.dataFile;
      }
   }

   private Map<String, Object> mergeLiveAuthored(Map<String, Object> var1) {
      var var2 = this.homes.stream().filter(var0 -> Files.isDirectory(var0)).toList();
      if (var2.isEmpty()) {
         return var1;
      } else {
         RequirementsRepository var3 = new RequirementsRepository(var2, this.projectRoot());

         Map var4;
         io.karatelabs.ext.requirements.model.RequirementsBundle var5;
         try {
            var5 = var3.a();
            var4 = var3.c();
         } catch (RuntimeException var16) {
            return var1;
         }

         if (var5 != null && !var5.d().isEmpty()) {
            Map<String, Object> var6 = var1 != null ? var1 : new LinkedHashMap<>();
            var var7 = new ArrayList<>(io.karatelabs.ext.common.jsapi.CoverageGraphAccessor.a((Map<String, Object>)var6, "items"));
            HashSet var8 = new HashSet();

            for (Map var10 : (Iterable<Map>) (Iterable<?>) (var7)) {
               var8.add(String.valueOf(var10.get("id")));
            }

            LinkedHashSet var17 = new LinkedHashSet();

            for (io.karatelabs.ext.requirements.model.RequirementNode var11 : var5.d()) {
               String var12 = "req:" + var11.b();
               if (var8.add(var12)) {
                  var7.add(authoredReqItem(var11));
               }

               for (io.karatelabs.ext.requirements.AcceptanceCriterion var14 : (Iterable<io.karatelabs.ext.requirements.AcceptanceCriterion>) (Iterable<?>) (io.karatelabs.ext.requirements.AcceptanceCriterion.a(var11.m()))) {
                  String var15 = var12 + "/" + var14.a();
                  if (var8.add(var15)) {
                     var7.add(authoredCriterionItem(var11, var14, (io.karatelabs.ext.requirements.kiro.KiroSpecAdapter.KiroTaskClaim)var4.get(var11.b() + "/" + var14.a())));
                     var17.add(var12);
                  }
               }
            }

            var6.put("items", var7);

            for (int var19 = 0; var19 < var7.size(); var19++) {
               Map var20 = (Map)var7.get(var19);
               if (var17.contains(String.valueOf(var20.get("id")))) {
                  String var21 = acceptanceStatus(io.karatelabs.ext.requirements.RequirementsReportBuilder.a((Map<String, Object>)var6, String.valueOf(var20.get("id"))));
                  if (var21 != null && !var21.equals(var20.get("status"))) {
                     LinkedHashMap var22 = new LinkedHashMap(var20);
                     var22.put("status", var21);
                     var7.set(var19, var22);
                  }
               }
            }

            if (!(var6.get("hits") instanceof List)) {
               var6.put("hits", new ArrayList());
            }

            return (Map<String, Object>)var6;
         } else {
            return var1;
         }
      }
   }

   private static Map<String, Object> authoredReqItem(io.karatelabs.ext.requirements.model.RequirementNode var0) {
      LinkedHashMap var1 = new LinkedHashMap();
      var1.put("id", "req:" + var0.b());
      var1.put("source", "req");
      var1.put("kind", "req");
      var1.put("name", var0.d() != null ? var0.d() : var0.b());
      var1.put("status", "NOTCOVERED");
      putIfSet(var1, "type", var0.c());
      putIfSet(var1, "reqStatus", var0.f());
      putIfSet(var1, "priority", var0.g());
      putIfSet(var1, "criticality", var0.h());
      putIfSet(var1, "parent", var0.e());
      if (var0.m() != null && !var0.m().isEmpty()) {
         var1.put("acceptance", new ArrayList<>(var0.m()));
      }

      return var1;
   }

   private static Map<String, Object> authoredCriterionItem(
      io.karatelabs.ext.requirements.model.RequirementNode var0, io.karatelabs.ext.requirements.AcceptanceCriterion var1, io.karatelabs.ext.requirements.kiro.KiroSpecAdapter.KiroTaskClaim var2
   ) {
      LinkedHashMap var3 = new LinkedHashMap();
      var3.put("id", "req:" + var0.b() + "/" + var1.a());
      var3.put("source", "req");
      var3.put("kind", "acceptance-criterion");
      var3.put("criterion", var1.a());
      var3.put("name", var1.b());
      var3.put("status", "NOTCOVERED");
      if (var2 != null) {
         var3.put("claimed", true);
         putIfSet(var3, "claimedBy", var2.a());
         putIfSet(var3, "claimedTask", var2.b());
      }

      return var3;
   }

   private static void putIfSet(Map<String, Object> var0, String var1, String var2) {
      if (var2 != null && !var2.isBlank()) {
         var0.put(var1, var2);
      }
   }

   private static String acceptanceStatus(List<Map<String, Object>> var0) {
      if (var0.isEmpty()) {
         return null;
      } else {
         boolean var1 = false;
         boolean var2 = false;
         boolean var3 = false;

         for (Map var5 : var0) {
            String var6 = String.valueOf(var5.get("status"));
            switch (var6) {
               case "FAILING":
                  var1 = true;
                  break;
               case "NOTCOVERED":
                  var2 = true;
                  break;
               case "NOTRUN":
                  var3 = true;
            }
         }

         if (var1) {
            return "FAILING";
         } else if (var2) {
            return "NOTCOVERED";
         } else {
            return var3 ? "NOTRUN" : "COVERED";
         }
      }
   }

   private Map<String, Object> notFound() {
      return JsApi.dataError(
         io.karatelabs.ext.common.jsapi.CoverageGraphAccessor.a(this.dataFile),
         this.dataFile,
         "run your @req=-tagged suite (e.g. Runner.suite(...)) — on the console its settle builds the requirement graph — or Report.aggregate() to build it from runs/"
      );
   }

   private record ReadinessCacheEntry(String a, Map<String, Object> b) {}

   private record RequirementTarget(Path a, Path b, String c, Map<String, Object> d) {}

   private final class CoverageTestIndex {
      private final Map<String, String> b;
      private final Map<String, Map<String, Object>> c = new HashMap<>();
      private final Map<String, Optional<String>> d = new HashMap<>();
      private final Map<String, Optional<String>> e = new HashMap<>();

      CoverageTestIndex(Map<String, Object> var2) {
         this.b = io.karatelabs.ext.common.jsapi.CoverageGraphAccessor.b(var2);

         for (Map var4 : io.karatelabs.ext.common.jsapi.CoverageGraphAccessor.a(var2, "tests")) {
            this.c.putIfAbsent(String.valueOf(var4.get("id")), var4);
         }
      }

      Map<String, Object> a(String var1) {
         LinkedHashMap var2 = new LinkedHashMap();
         var2.put("label", this.b.getOrDefault(var1, var1));
         var var3 = this.c.get(var1);
         if (var3 != null) {
            io.karatelabs.ext.requirements.RequirementsReportBuilder.a(var2, var3, "status");
            this.a(var1, var3).ifPresent(var1x -> var2.put("check", var1x));
         }

         return var2;
      }

      List<String> a(Object var1) {
         LinkedHashSet var2 = new LinkedHashSet();
         if (var1 instanceof List) {
            for (Object var5 : (List)var1) {
               String var6 = String.valueOf(var5);
               var var7 = this.c.get(var6);
               if (var7 != null) {
                  Optional var8 = this.a(var6, var7);
                  if (var8.isEmpty() && var7.get("feature") instanceof String var9) {
                     var8 = this.e.computeIfAbsent(var9, var1x -> Optional.ofNullable(Requirement.this.featureOf(var1x)));
                  }

                  var8.ifPresent(var2::add);
               }
            }
         }

         return new ArrayList<>(var2);
      }

      private Optional<String> a(String var1, Map<String, Object> var2) {
         return this.d.computeIfAbsent(var1, var2x -> Optional.ofNullable(Requirement.this.checkOf(var2)));
      }
   }
}
