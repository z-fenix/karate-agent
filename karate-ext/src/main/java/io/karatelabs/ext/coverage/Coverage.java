package io.karatelabs.ext.coverage;

import io.karatelabs.ext.common.jsapi.JsApi;
import io.karatelabs.ext.common.jsapi.JsDispatch;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Coverage extends JsDispatch {
   static final String DATA_FILE = "ext/coverage/coverage-data.json";
   private final Path dataFile;

   public Coverage(Path var1) {
      this.dataFile = var1;
      this.fn("help", var0 -> JsApi.helpTopic(help(), var0));
      this.fn("summary", var1x -> this.summary());
      this.fn("gaps", var1x -> this.gaps());
      this.fn("items", var1x -> this.items(JsApi.str(var1x, 0)));
      this.fn("status", var1x -> this.status(JsApi.str(var1x, 0)));
      this.fn("tests", var1x -> this.tests(JsApi.str(var1x, 0)));
      this.fn("hits", var1x -> this.hits(JsApi.str(var1x, 0)));
      this.fn("dimensions", var1x -> this.dimensions(JsApi.str(var1x, 0)));
      this.fn("coveringArray", var1x -> this.coveringArray(JsApi.str(var1x, 0), JsApi.arg(var1x, 1)));
      this.fn("sources", var1x -> this.sources());
      this.fn("path", var1x -> var1.toString());
   }

   private static Map<String, Object> help() {
      return JsApi.markdown(
         "# Coverage — query the traceability graph (read-only, deterministic)\n\nReads the `karate-trace/v1` graph from `<reportDir>/ext/coverage/coverage-data.json`\n(written by a `karate run -f karate:jsonl` with `boot.ext('coverage')`). Re-read on every call.\n\n**Return shapes (S14):** the list verbs (`summary`/`gaps`/`items`/`hits`/`tests`/`sources`)\nreturn **bare JS arrays** — `.map`/`.filter`/`.length`/`[i]` work directly, no unwrap. A\nsingle-record verb (`status`) and the graded projections (`dimensions`/`coveringArray`)\nreturn objects; an error is always a `{error, code, …}` map (branch on `Array.isArray(x)`).\n\n- `Coverage.summary()` → `[…]` — one row per source: `type` (family) + `namespace`\n  (the multi-spec instance, e.g. `openapi.store`; == `type` for a lone source),\n  totals/covered/percentage + warning count. The %/counts derive from the §2c\n  **graph status ladder** — the one truth `items[]`/`Requirement.*`/the report all share —\n  NOT the source's observed http/grpc matches. So the **declared-coverage `req` source** %\n  is real requirement coverage (a requirement is COVERED via its `@req=` declared edge), and\n  it agrees with `Requirement.readiness()`; use `Requirement.matrix()`/`readiness()` for the\n  richer requirement view, not because this % is wrong.\n- `Coverage.gaps()` → `[…]` — one row per source: `notcovered` items (NOTCOVERED) +\n  `happyOnly` items (hit only on the success path, never an error outcome — the sad-path\n  worklist) + unmatched calls.\n- `Coverage.items([source])` → `[…]` — item nodes, each with a derived `status`.\n- `Coverage.status(id)` — one namespaced item (e.g. `openapi:placeOrder`) →\n  `{id, status, callCount, tests, requirements, oracleOnly, refusalOnly, modelOnly,\n  mockOnly, notasserted}`, plus — once the\n  item was exercised — the observed status histogram (`statusCodes`/`grpcStatuses`/\n  `kafkaStatuses`) and the error-path grade `outcome: {tested, errors, declaredUntested?}`\n  (`declaredUntested` = response codes the OpenAPI spec declares that no run ever\n  produced — the sad-path gap). `requirements` is the\n  requirements whose `@real=` claims this artifact — **empty means nothing declares it**, an\n  orphan. `oracleOnly` (only the rulebook's own evidence vouches), `refusalOnly` (covered only\n  by refusal proofs — the shape refuses what it must, nothing shows the behaviour works),\n  `modelOnly` (only a model checker's verdict vouches — the design, not the implementation),\n  `mockOnly` (exercised, but a karate mock answered) and `notasserted` (covered only by tests\n  that assert nothing) are **disclosures, never a status** — always present, so branch on\n  them directly.\n- `Coverage.hits([itemId])` → `[…]` — hit edges (observed + declared), with match `confidence`.\n- `Coverage.dimensions([itemId])` — the coverage-dimension **marginal** surface (MODEL §2h).\n  No arg = every item; an `itemId` is the **namespaced** id as `gaps()`/`items()` emit it\n  (`grpc:HelloService/Hello`, `openapi:createOrder`) — a bare operation id is accepted when it\n  resolves uniquely, and an unknown id returns a did-you-mean (never a silent empty). Per item,\n  per axis (`paymentMethod`, `customerTier`, `response`, …), which values were\n  observed. A **closed** axis (the spec `response` codes, or a rulebook axis bound via a\n  `cov.dimensions` `{rulebook,extract}` entry) is **graded** by a single `coveredProportion` in\n  [0,1] (`1`=fully covered, `0`=none, `-1`=open/not gradeable) + `gaps`. Enum/boolean axes\n  grade by value; numeric axes grade by **boundary-value analysis** (each observed value\n  classified into a `< k`/`= k`/… partition). A rule-bound axis never exercised reads `0`.\n  An item with a `cov.dimensions` `cross:[axisA,axisB]` binding also carries a **`cross`** block —\n  the rule-as-oracle **cross/point** view: `coveredProportion` over the cross-product cells\n  (required combinations) + the uncovered `gaps` cells.\n- `Coverage.coveringArray([itemId], [strengthPolicy])` — the combinatorial **covering-array**\n  deck (the \"which cells to test next\" companion to the cross `gaps`): for each `cross`-bound\n  item, the minimal set of test `rows` reaching strength-`t` adequacy (`t=1`=marginal,\n  `t=2`=pairwise, `t≥|axes|`=the full cross-product). Honours the rules' feasibility\n  constraints (infeasible cells are never proposed) and the already-observed coordinates (a\n  covered tuple is not re-targeted, so the deck fills only the gap). Deterministic. The\n  **strength is selected by the item's authored `criticality`** (high ⇒ 3-way, else\n  pairwise), overridable via the argument: a number (same `t` for all items) or a policy\n  object keyed by criticality (`{high:3, default:2}`). The deck uses a deterministic greedy\n  generator. **For gate-grade, provably-optimal combinatorial generation** (deterministic\n  **IPOG** / NIST ACTS-style, with a `@criticality`-selected algorithm) — an **enterprise**\n  toolkit — **contact Karate Labs** (https://karatelabs.io).\n- `Coverage.tests([itemId])` → `[…]` — executed scenarios; pass an item id (e.g. `req:USERS-001`)\n  to get only the scenarios covering it (§4c-rolled-up for a requirement's criteria).\n- `Coverage.sources()` → `[…]` — source descriptors. `Coverage.path()` → the data-file path.\n\nRelated: `Requirement.*` (requirement view + readiness), `Graph.*` (raw traversal),\n`Report.generate()` (the file:// scorecard)."
      );
   }

   public static Coverage atReportDir(Path var0) {
      return new Coverage(var0.resolve("ext/coverage/coverage-data.json"));
   }

   Object summary() {
      var var1 = this.readGraph();
      if (var1 == null) {
         return this.notFound();
      } else {
         ArrayList<java.util.LinkedHashMap> var2 = new ArrayList();

         for (Map var4 : (Iterable<Map>) (Iterable<?>) (io.karatelabs.ext.common.jsapi.CoverageGraphAccessor.a(var1, "sources"))) {
            LinkedHashMap<java.lang.String, java.lang.Object> var5 = new LinkedHashMap();
            var5.put("type", var4.get("type"));
            var5.put("namespace", var4.getOrDefault("namespace", var4.get("type")));
            var5.put("title", var4.get("title"));
            var5.put("version", var4.get("version"));
            var5.put("total", var4.get("total"));
            var5.put("covered", var4.get("covered"));
            var5.put("percentage", var4.get("percentage"));
            var5.put("byStatus", var4.get("byStatus"));
            var5.put("warnings", sizeOf(var4.get("warnings")));
            var2.add(var5);
         }

         return var2;
      }
   }

   Object gaps() {
      var var1 = this.readGraph();
      if (var1 == null) {
         return this.notFound();
      } else {
         var var2 = io.karatelabs.ext.common.jsapi.CoverageGraphAccessor.a(var1, "items");
         var var3 = io.karatelabs.ext.common.jsapi.CoverageGraphAccessor.a(var1, "unmatched");
         ArrayList<java.util.LinkedHashMap> var4 = new ArrayList();

         for (Map var6 : (Iterable<Map>) (Iterable<?>) (io.karatelabs.ext.common.jsapi.CoverageGraphAccessor.a(var1, "sources"))) {
            Object var7 = var6.get("type");
            Object var8 = var6.getOrDefault("namespace", var7);
            LinkedHashMap<java.lang.String, java.lang.Object> var9 = new LinkedHashMap();
            var9.put("type", var7);
            var9.put("namespace", var8);
            var9.put("title", var6.get("title"));
            var9.put(
               "notcovered",
               var2.stream()
                  .filter(var1x -> var8.equals(var1x.get("source")) && "NOTCOVERED".equals(var1x.get("status")) && !Boolean.TRUE.equals(var1x.get("container")))
                  .toList()
            );
            var9.put(
               "happyOnly",
               var2.stream()
                  .filter(var1x -> var8.equals(var1x.get("source")) && var1x.get("outcome") instanceof Map var2x && Boolean.FALSE.equals(var2x.get("tested")))
                  .toList()
            );
            var9.put("unmatched", var3.stream().filter(var1x -> var8.equals(var1x.get("source"))).toList());
            var4.add(var9);
         }

         return var4;
      }
   }

   Object items(String var1) {
      var var2 = this.readGraph();
      if (var2 == null) {
         return this.notFound();
      } else {
         List<Map<String, Object>> var3 = io.karatelabs.ext.common.jsapi.CoverageGraphAccessor.a(var2, "items");
         if (var1 != null) {
            var3 = var3.stream().filter(var1x -> var1.equals(var1x.get("source"))).toList();
         }

         return var3;
      }
   }

   Map<String, Object> status(String var1) {
      var var2 = this.readGraph();
      if (var2 == null) {
         return this.notFound();
      } else if (var1 == null) {
         return JsApi.missing("Coverage.status(id) requires a namespaced item id, e.g. 'openapi:listProducts'");
      } else {
         ArrayList<java.lang.String> var3 = new ArrayList();
         Map<java.lang.String, Object> var4 = null;
         ArrayList<java.lang.String> var5 = new ArrayList();

         for (Map var7 : (Iterable<Map>) (Iterable<?>) (io.karatelabs.ext.common.jsapi.CoverageGraphAccessor.a(var2, "items"))) {
            if (var1.equals(var7.get("id"))) {
               var4 = var7;
            } else {
               var3.add(String.valueOf(var7.get("id")));
            }

            if (io.karatelabs.ext.common.jsapi.CoverageGraphAccessor.a(var7).contains(var1)) {
               var5.add(String.valueOf(var7.get("id")));
            }
         }

         if (var4 == null) {
            return JsApi.suggest(JsApi.notFound("no such item: " + var1), io.karatelabs.ext.common.jsapi.SuggestionEngine.a(var1, var3));
         } else {
            LinkedHashMap<java.lang.String, java.lang.Object> var11 = new LinkedHashMap();
            var11.put("id", var1);
            var11.put("status", var4.get("status"));
            var11.put("callCount", var4.get("callCount"));
            var11.put("tests", io.karatelabs.ext.common.jsapi.CoverageGraphAccessor.b(var2, var4.get("tests")));
            var11.put("requirements", var5);
            var11.put("oracleOnly", Boolean.TRUE.equals(var4.get("oracleOnly")));
            var11.put("refusalOnly", Boolean.TRUE.equals(var4.get("refusalOnly")));
            var11.put("modelOnly", Boolean.TRUE.equals(var4.get("modelOnly")));
            var11.put("mockOnly", Boolean.TRUE.equals(var4.get("mockOnly")));
            var11.put("notasserted", Boolean.TRUE.equals(var4.get("notasserted")));

            for (String var10 : new String[]{"statusCodes", "grpcStatuses", "kafkaStatuses", "outcome"}) {
               if (var4.get(var10) != null) {
                  var11.put(var10, var4.get(var10));
               }
            }

            return var11;
         }
      }
   }

   Object tests(String var1) {
      var var2 = this.readGraph();
      if (var2 == null) {
         return this.notFound();
      } else if (var1 != null && !var1.isBlank()) {
         var var3 = io.karatelabs.ext.common.jsapi.CoverageGraphAccessor.b(var2, var1);
         if (var3 != null) {
            return var3;
         } else {
            ArrayList<java.lang.String> var4 = new ArrayList();

            for (Map var6 : (Iterable<Map>) (Iterable<?>) (io.karatelabs.ext.common.jsapi.CoverageGraphAccessor.a(var2, "items"))) {
               var4.add(String.valueOf(var6.get("id")));
            }

            return JsApi.suggest(JsApi.notFound("no such item: " + var1), io.karatelabs.ext.common.jsapi.SuggestionEngine.a(var1, var4));
         }
      } else {
         return io.karatelabs.ext.common.jsapi.CoverageGraphAccessor.a(var2, "tests");
      }
   }

   Object hits(String var1) {
      var var2 = this.readGraph();
      if (var2 == null) {
         return this.notFound();
      } else {
         List<Map<String, Object>> var3 = io.karatelabs.ext.common.jsapi.CoverageGraphAccessor.a(var2, "hits");
         if (var1 != null) {
            var3 = var3.stream().filter(var1x -> var1.equals(var1x.get("item"))).toList();
         }

         return var3;
      }
   }

   Map<String, Object> dimensions(String var1) {
      var var2 = this.readGraph();
      if (var2 == null) {
         return this.notFound();
      } else if (var1 != null && !var1.isBlank()) {
         String var3 = resolveItemId(var2, var1);
         if (var3 != null) {
            return io.karatelabs.ext.coverage.dim.DimensionCoverageProjector.a(var2, var3);
         } else {
            ArrayList<java.lang.String> var4 = new ArrayList();

            for (Map var6 : (Iterable<Map>) (Iterable<?>) (io.karatelabs.ext.common.jsapi.CoverageGraphAccessor.a(var2, "items"))) {
               var4.add(String.valueOf(var6.get("id")));
            }

            return JsApi.suggest(JsApi.notFound("no such item: " + var1), io.karatelabs.ext.common.jsapi.SuggestionEngine.a(var1, var4));
         }
      } else {
         return io.karatelabs.ext.coverage.dim.DimensionCoverageProjector.a(var2, null);
      }
   }

   private static String resolveItemId(Map<String, Object> var0, String var1) {
      ArrayList<java.lang.String> var2 = new ArrayList();

      for (Map var4 : io.karatelabs.ext.common.jsapi.CoverageGraphAccessor.a(var0, "items")) {
         String var5 = String.valueOf(var4.get("id"));
         if (var5.equals(var1)) {
            return var5;
         }

         if (var5.endsWith(":" + var1)) {
            var2.add(var5);
         }
      }

      return var2.size() == 1 ? (String)var2.get(0) : null;
   }

   Map<String, Object> coveringArray(String var1, Object var2) {
      var var3 = this.readGraph();
      return var3 == null ? this.notFound() : io.karatelabs.ext.coverage.dim.CoveringArrayGenerator.a(var3, var1, var2);
   }

   Object sources() {
      var var1 = this.readGraph();
      return var1 == null ? this.notFound() : io.karatelabs.ext.common.jsapi.CoverageGraphAccessor.a(var1, "sources");
   }

   private Map<String, Object> readGraph() {
      return io.karatelabs.ext.common.jsapi.CoverageGraphAccessor.a(this.dataFile).c();
   }

   private Map<String, Object> notFound() {
      return JsApi.dataError(
         io.karatelabs.ext.common.jsapi.CoverageGraphAccessor.a(this.dataFile),
         this.dataFile,
         "no coverage graph yet. To see the DECLARED universe — what this project expects you to cover (the cov.kafka topics, cov.grpc methods, cov.openapi operations, …) — call Report.aggregate() now: with no runs it renders every declared item as a gap, so you learn the universe without reading the boot config. Then run your suite (e.g. Runner.suite(...)): on the console its settle folds in what it covered."
      );
   }

   private static int sizeOf(Object var0) {
      return var0 instanceof List var1 ? var1.size() : 0;
   }
}
