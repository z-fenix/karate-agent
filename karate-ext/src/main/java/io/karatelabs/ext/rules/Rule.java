package io.karatelabs.ext.rules;

import io.karatelabs.common.Json;
import io.karatelabs.core.Runner.Builder;
import io.karatelabs.ext.common.CoverageEmit;
import io.karatelabs.ext.common.MaxPaths;
import io.karatelabs.ext.common.jsapi.JsApi;
import io.karatelabs.ext.common.jsapi.JsDispatch;
import io.karatelabs.ext.common.keycheck.Main;
import io.karatelabs.ext.rules.twin.Twin;
import io.karatelabs.js.Engine;
import io.karatelabs.js.EngineException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Map.Entry;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Rule extends JsDispatch implements io.karatelabs.ext.coverage.dim.DimensionOracle, AutoCloseable {
   private static final Logger logger = LoggerFactory.getLogger(Rule.class);
   private static final Set<String> ENGINE_FAULT_ERRORS = Set.of(
      "TypeError", "ReferenceError", "RangeError", "SyntaxError", "URIError", "EvalError", "AggregateError"
   );
   private final Path rulesHome;
   private final RulebookRepository manager;
   private final RuleEvaluator runner;
   private Path reportDir;
   private Supplier<UnaryOperator<Builder>> runPrep = () -> var0 -> var0;
   private static volatile MutationProvider mutations = MutationProvider.a;
   private Path projectRoot;
   private List<Path> requirementsHomes = List.of();
   private io.karatelabs.ext.rules.Rule.StepListener stepSink;
   private Consumer<io.karatelabs.ext.rules.RuleEvaluationCapture.CapturedRuleEvaluation> serveTrace;
   private RulebookCoverageProvider producer;
   private Function<Path, io.karatelabs.ext.coverage.CoverageProjectBuilder.CoverageProjectBuildResult> projectBuilder;
   private Twin twin;
   public static final String TABLE_ROWS_MAX_PROPERTY = "karate.rules.tableRowsMax";
   static final List<String> SUITES = List.of("coverage", "pairwise", "boundary", "product", "cells");
   private static final Set<String> PROJECT_AGAINST = Set.of("against", "output", "failOn", "waivers", "asOf");
   private static final String PROJECT_AGAINST_USAGE = "Rule.project(rule, {against: 'api/claims.yaml', output?, failOn?, waivers?, asOf?}) — diffs the projection against a project spec and writes nothing";
   private static final String SCHEMA_EG = "\"schema = { qty: '#number' }\"";
   private static final String CALC_EG = "\"const execute = function(calc) { … }\"";
   private static final String GENERATOR_EG = "\"function generate(g) { … }\"";
   private static final String TWIN_EG = "\"t.init(w => { … })\"";

   public static void withMutations(MutationProvider var0) {
      mutations = var0;
   }

   public Rule(Path var1) {
      this.rulesHome = var1;
      this.manager = new RulebookRepository(var1);
      this.runner = new RuleEvaluator();
      this.fn("list", var1x -> this.list());
      this.fn("load", var1x -> this.load(JsApi.str(var1x, 0), JsApi.arg(var1x, 1)));
      this.fn("execute", var1x -> this.execute(JsApi.str(var1x, 0), JsApi.arg(var1x, 1)));
      this.fn("run", var1x -> this.run(JsApi.str(var1x, 0), JsApi.str(var1x, 1), JsApi.arg(var1x, 2)));
      this.fn("audit", var1x -> this.audit(JsApi.str(var1x, 0), JsApi.arg(var1x, 1), JsApi.arg(var1x, 2)));
      this.fn("diagram", var1x -> this.diagram(JsApi.str(var1x, 0), JsApi.arg(var1x, 1)));
      this.fn("explore", var1x -> this.explore(JsApi.str(var1x, 0), JsApi.arg(var1x, 1)));
      this.fn("table", var1x -> this.table(JsApi.str(var1x, 0), JsApi.arg(var1x, 1)));
      this.fn("ranges", var1x -> this.ranges(JsApi.str(var1x, 0), JsApi.arg(var1x, 1)));
      this.fn("check", var1x -> this.check(JsApi.str(var1x, 0), JsApi.arg(var1x, 1)));
      this.fn("feasible", var1x -> this.feasibleVerb(JsApi.str(var1x, 0), JsApi.arg(var1x, 1)));
      this.fn("sweep", var1x -> this.sweep(JsApi.str(var1x, 0), JsApi.arg(var1x, 1)));
      this.fn("impact", var1x -> this.impact(JsApi.str(var1x, 0), JsApi.arg(var1x, 1)));
      this.fn("cells", var1x -> this.cells(JsApi.str(var1x, 0), JsApi.arg(var1x, 1)));
      this.fn("export", var1x -> this.export(JsApi.str(var1x, 0), JsApi.arg(var1x, 1)));
      this.fn("match", var1x -> this.match(JsApi.str(var1x, 0), JsApi.arg(var1x, 1)));
      this.fn("prove", var1x -> this.prove(JsApi.str(var1x, 0)));
      this.fn("import", var1x -> this.importTable(JsApi.str(var1x, 0), JsApi.arg(var1x, 1)));
      this.fn("changeSet", var1x -> this.changeSet(JsApi.str(var1x, 0), JsApi.arg(var1x, 1)));
      this.fn("changeSetCells", var1x -> this.changeSetCells(JsApi.str(var1x, 0), JsApi.arg(var1x, 1), JsApi.arg(var1x, 2)));
      this.fn("editions", var1x -> this.editions(JsApi.str(var1x, 0), JsApi.arg(var1x, 1)));
      this.fn("tableDiff", var1x -> this.tableDiff(JsApi.str(var1x, 0), JsApi.arg(var1x, 1)));
      this.fn("mutate", var1x -> this.mutate(JsApi.str(var1x, 0), JsApi.arg(var1x, 1)));
      this.fn("report", var1x -> this.report(JsApi.str(var1x, 0), JsApi.arg(var1x, 1)));
      this.fn("bless", var1x -> this.bless(JsApi.str(var1x, 0)));
      this.fn("drift", var1x -> this.drift(JsApi.str(var1x, 0)));
      this.fn("cover", var1x -> this.cover(JsApi.str(var1x, 0), JsApi.arg(var1x, 1)));
      this.fn("project", var1x -> this.project(JsApi.str(var1x, 0), JsApi.arg(var1x, 1)));
      this.fn("create", var1x -> this.create(JsApi.str(var1x, 0), JsApi.arg(var1x, 1)));
      this.fn("delete", var1x -> this.delete(JsApi.str(var1x, 0)));
      this.val("scenario", new ScenarioVerbs(this));
      this.val("twin", new TwinVerbs(this));
      this.val("sequence", new SequenceVerbs(this));
      this.val("calc", new CalcVerbs(this));
      this.val("schema", new SchemaVerbs(this));
      this.val("generator", new GeneratorVerbs(this));
      this.val("lookups", new LookupsVerbs(this));
      this.fn("help", var0 -> JsApi.helpTopic(help(), var0));
   }

   static int tableRowsMax() {
      return Integer.getInteger("karate.rules.tableRowsMax", 250);
   }

   Map<String, Object> lookupsRows(String var1, String var2, Object var3) {
      if (var1 != null && var2 != null) {
         try {
            Map var4 = var3 instanceof Map var5 ? var5 : Map.of();
            int var10 = var4.get("offset") instanceof Number var6 ? var6.intValue() : 0;
            int var11 = var4.get("limit") instanceof Number var12 ? var12.intValue() : 0;
            return this.manager.b(var1).m().a(var2, var10, var11, tableRowsMax());
         } catch (RuntimeException var9) {
            return JsApi.error(var9);
         }
      } else {
         return JsApi.missing("Rule.lookups.rows(rule, tableKey, {offset?, limit?}) requires a rule name and a table key (see Rule.load(rule).tables)");
      }
   }

   Map<String, Object> lookupsCell(String var1, String var2) {
      if (var1 != null && var2 != null) {
         try {
            io.karatelabs.ext.rules.RulebookRepository.RulebookDocument var3 = this.manager.b(var1);
            io.karatelabs.ext.rules.RuleTableCatalog.TableCellReference var4 = var3.m().d(var2);
            if (var4 == null) {
               throw new RuleApiException(404, "LOOKUP_PATH_NOT_FOUND", "'" + var2 + "' names no file-table cell (tables: " + var3.m().b().keySet() + ")");
            } else {
               LinkedHashMap var5 = new LinkedHashMap();
               var5.put("address", var4.a());
               var5.putAll(var4.b());
               var5.put("value", var4.h());
               var5.put("sha256", var3.a().get(var4.d()));
               return var5;
            }
         } catch (RuntimeException var6) {
            return JsApi.error(var6);
         }
      } else {
         return JsApi.missing("Rule.lookups.cell(rule, address) requires a rule name and a cell address ('rate.base.CA.rate', 'fees@CA/filing')");
      }
   }

   private static Map<String, Object> help() {
      return JsApi.markdown(
         "# Rule — author + run calc/oracle Rulebooks (the headless acceptance engine)\n\nA Rulebook is a folder under the rules home holding a `calc.js` (the calculation/oracle),\na `schema.js` shape, and `scenarios.json` rows. Call `Rule.verb(args)` and end with the value.\n\n- `Rule.list()` — Rulebooks in the home dir → `[{name, scenarioCount, mtimes}]`.\n- `Rule.load(name)` — `{name, schema, scenarios, rejects, calc, generator, lookups, twin,\n  sequences, mtimes}`.\n  `scenarios` are the rows the calc runs; `rejects` are the negative rows (below), kept\n  apart so nothing runs or counts them as scenarios. `twin` is the `twin.js`\n  source and `sequences` the parsed `sequences.json` — each null when that file is absent.\n- `Rule.execute(name, row)` — **the oracle call**: run `calc.js` against an ad-hoc input row\n  → `{input, output, outcome?, audit, durationMs, truncated, reqs?}`; `outcome` rides when\n  `calc.outcome` set one and `reqs` are the criteria `calc.req` reached. A value outside a\n  closed domain `schema.js` declares (an enum) is refused before the calc runs —\n  `INVALID_INPUT` (400); a row that merely omits a declared key still runs.\n  **`input`** is that row ready to send to\n  a system under test — the business fields, with every `_`-prefixed key (the rulebook's own\n  `_id`/`_label`) removed —\n  so ONE saved scenario drives both the oracle and a schema-strict SUT (a raw row is rejected\n  by proto JSON / a strict schema: `Cannot find field: _id`). The result also carries a `.verify(ok, note?)`\n  **method** (not an enumerable key — `Object.keys` won't list it): call it to assert the\n  system against the oracle — it stamps a pass/fail verdict onto the run/report,\n  auto-attaching the rule's requirement criteria, and **a false verdict fails the run like\n  a failed `match`** (the UI↔rules bridge — an assert with free traceability).\n- `Rule.run(name, scenarioId, opts?)` — `execute` against a stored `scenarios.json` row.\n  `{diff:true}` overlays the run-to-run audit diff (per-node `status` + word-level\n  `wordDiff` on changed lines, a `diff` summary on the result). **It is a PAIR of flagged\n  runs, not a diff against whatever ran last:** the baseline advances only on a\n  `{diff:true}` run, so the first one captures it and reports `firstRun` with nothing to\n  compare — flag the run before the change AND the run after it. One baseline per\n  rulebook, any scenario: switching scenario diffs against the previously flagged one,\n  which the summary names as `prevScenario`. `{trace:true}` adds\n  `trace.sites` — the branch sites the run took (`{\"L26#0\": [\"true\"]}`), ids matching\n  the diagram's nodes.\n- `Rule.audit(name, rowOrScenarioId, opts?)` — the run's audit tree; `{output:'markdown'|'both'}`\n  renders it (trees get large).\n- `Rule.diagram(name, opts?)` — the calc.js flowchart as `{svg, mermaid, outline,\n  collapsed}` — a deterministic projection of the saved source (read-only; the code\n  stays the single source of truth). Big containers fold by default; `{expand:\n  ['L296#0']}` or `{expand:'all'}` opens them; `outline` is the jump list.\n- `Rule.explore(name, opts?)` — runtime branch exploration: run the calc against generated\n  inputs until every site×outcome is covered → `{coverage, notcovered, suite,\n  dimensions:[{path, type, levels}], universe, notderivable, stats, summary}` — the axes\n  under the same `path` identity `Rule.ranges` uses, with the raw levels the explorer\n  drove. Plus `robustness` when a declared bound was\n  crossed. `notcovered` is the unexercised `(site×outcome)` gaps. `coverage.perSite`/\n  `percent` are the EXECUTION ledger; `coverage.domainPerSite` (present when the two\n  populations differ) is the in-domain projection every reachability claim reads.\n  `{suite:'pairwise'|'boundary'|'product'}` swaps the default coverage-minimized `suite` for\n  the full all-pairs deck, the boundary-value deck (over the learned levels), or the\n  **raw-value Cartesian product** of a fully-enumerable universe (`'product'` refuses\n  honestly — `NOT_ENUMERABLE` with the open axes named, or `PRODUCT_OVERCAP` with the row\n  count when it exceeds the 50k cap; `{maxProduct}` raises it).\n  `{strength:3}` generates a 3-way deck instead of all-pairs — the reach a cell only a\n  specific triple of inputs enters needs (default `2`, clamped to the axis count).\n  `{suite:'cells', target?}` — rows `{input, expected, target, probes}` that exercise each\n  target file-table cell (`target` as `Rule.cells` takes it, default `'unexercised'`); the\n  `cells` block counts `exercised` · `unreachable` (`missing` names the value) · `notfound` (`search.truncated` → raise `{maxSearch}`).\n  An unknown suite refuses `SUITE_UNKNOWN`.\n- `Rule.table(name, opts?)` — the decision-table projection: stored scenarios + the chosen\n  `suite` (`{suite:'coverage'|'pairwise'|'boundary'|'cells'}`), each run once and grouped by\n  **outcome-key** (the full branch-outcome path identity) → `{columns{inputs,decisions,\n  outputs}, rows, siteUniverse, stats}`. Columns keep only what VARIES; each row carries\n  `provenance` (scenario/generated), `outcomeKey`, and `addsCoverage` (false = a duplicate\n  path). `{output:'markdown'}` renders the deduped table.\n\nRule-analysis toolbox (deterministic, zero-AI; an LLM drives these over\ncurl/MCP to iterate to the smallest test-data set that covers a requirement):\n- `Rule.ranges(name)` — the input-space partition **of the declared domain** →\n  `{rule, dimensions:[{path, label, type, exhaustive, size|cause, lo?, hi?, classes, gates}],\n  notderivable, outcomes}`. **An axis is identified by its `path`** (`items[].qty`) — the\n  same key `Rule.explore`'s own `dimensions` list carries; there is no `name` field. A\n  declared bound ends the partition, so a bounded axis gets no below-`lo`/above-`hi` class,\n  every `sample` is a member (integral on a `g.int` axis), and an interval holding no member\n  is omitted rather than faked. Per input dimension its equivalence\n  classes + boundary values (`<k · =k · >k`, enum members, true/false, array cardinality,\n  `field[].leaf` element dims) + the sites each gates, plus `notderivable` (the grey areas a\n  `calc.levels(path,[…])` hint closes). A free-text field the calc tests with\n  `== === != !==` also carries an **`other`** class (`complement:true`) — every value the\n  rulebook never names, so a `state != 'CA'` fallback arm is exercised instead of reading\n  unreachable; its sample is a witness, not a declared value. **`schema.js` decides this,\n  not `generator.js`** — a `'#string'` field keeps the class when sampled with\n  `g.enum('code',['BR','NT'])`. CLOSED axes (a literal list, `#boolean`, `g.int`) never get\n  one: they already list their whole domain, so an uncovered arm there is a real gap. Nor do\n  `in`/`not in`/`.includes()`, `!= null`, or a compare against a non-string. A local copy\n  is fine (`var code = input.code` then `code === 'BR'`) while that name is ASSIGNED ONCE;\n  reassign it and the compare is attributed to nothing — empty `gates`, and the compare\n  lands in `notderivable`. Each dim carries the **exhaustion marker**:\n  `exhaustive:true` + `size` (a closed axis — enum members, booleans, every integer of a\n  `g.int` range, whose `lo`/`hi` ride along) or `exhaustive:false` + `cause`\n  (`array`/`unbounded`/`notinteger`/`freetext`/`date`/`sampled`) — the axis that keeps a guarantee\n  at HOLDS instead of COMPLETE names itself. \"Which values matter, and why.\"\n- `Rule.check(name, opts?)` — completeness & health. Read `verdict` first —\n  `{status, findings, review, reasons[]}`, the rollup over everything below so you never\n  have to decide by hand which array counts. `FINDINGS` = something is wrong and the\n  engine is sure (a violated or never-checked guarantee, a `sometimes` never reached, a\n  negative scenario the schema accepted, a declared outcome nothing produces, an\n  authoring defect); `REVIEW` = arm findings only YOU can adjudicate (a `|| default`\n  fallback arm is benign, a business rule reported dead or unclaimed is a bug); `CLEAN` = neither.\n  `notderivable` and `robustness` are informational and count toward nothing. The detail,\n  split by the FIX (an arm is *reachable*\n  when some input INSIDE THE DECLARED DOMAIN can drive it — the generator demonstrates it;\n  *used* when an in-domain SAVED scenario exercises it. A saved row outside the domain earns\n  neither, and is disclosed under `robustness.savedRows`):\n  `notused` (reachable but no saved scenario exercises it → **add\n  scenario data**) vs `notreachable` (no scenario reaches it at all → **fix the rules**; `gaps` is\n  the legacy alias). Each names the input `axis` it turns on, or carries\n  `attribution:'notderivable'` when the comparison reached no input field — a `calc.levels`\n  hint or a proposed row, never a branch to rewrite; `Rule.ranges(name)` gives that axis its\n  classes and boundaries. Also `notclaimed` (decision arms no `calc.req` claims — put the call\n  inside the arm it describes, or delete the rule), `deadBranches` (a site stuck on one arm — a constant/unreachable rule),\n  `notproduced` (declared `calc.outcome`s no input produces), `notderivable` (grey areas a\n  `calc.levels` hint closes), `properties` (the PBT pair: a `calc.always` safety\n  property violated → a minimal counterexample; a `calc.sometimes` situation reads\n  `REACHED` / `NOTREACHED` (a sampled gap) / `NOTREACHABLE` (proven over a closed\n  domain walked whole — a guarded one blocks readiness) / `NOTCHECKED`). Every verdict — `checks`, `violations`, `holds`, the\n  counterexample/witness — is decided by rows INSIDE the declared domain alone, so a\n  counterexample is always an input that can occur; evaluations on rows outside it ride\n  along as `robustnessChecks`. Each `always` entry carries `status` —\n  `COMPLETE` (**proven for this input domain**: the universe is closed — every axis an\n  enum, boolean or bounded `g.int` — and check walked its ENTIRE raw-value Cartesian\n  product with zero violations, zero grey areas, the property evaluated on EVERY walked\n  row; one declared inside a branch arm runs on only the rows that reach it and stays\n  `HOLDS`, its `walkChecks` disclosing the gap) / `VIOLATED` (counterexample attached) /\n  `HOLDS` (held across the deck's in-domain rows — sampled, not proven) / `NOTCHECKED`\n  (`checks: 0` — never executed, or evaluated only on rows outside the declared domain,\n  `robustnessChecks` disclosing those) — and `req` when the property guards an acceptance\n  criterion. Every `always` is ALSO hunted at the corners of its declared box, counted\n  apart as `searched` (plus `searchViolations` where it broke there): a `VIOLATED`\n  beside `violations: 0` is a real in-domain counterexample the deck never generated. The\n  `search` block reports that hunt — `{rows, corners, maxSearch, truncated}`;\n  `truncated:true` means only `rows` of `corners` were tried, so a held property is\n  sampled that far and no further. **`{maxSearch}` raises the cap** (`0` disables the\n  hunt). The `universe` block carries the completeness premise: `enumerable`,\n  `product` (the raw-value row count), `walked`, `complete`, and `causes[]` naming every\n  downgrade (an open axis, `notderivable`, `overcap` with the count, an IN-DOMAIN throwing\n  row — a guard tripping on an input the domain excludes costs nothing — or\n  `walkmembership`, a walk row that escaped the declared domain). The\n  full-product walk is size-guarded at 50k rows — `{maxProduct}` overrides.\n  Over that cap a book is proven by CERTIFICATE instead: **`Rule.check(name,\n  {certify:true})`** walks the declared domain once by abstract interpretation, stores\n  `.baseline/certificate.json`, and every later `check`/`explore` reads it while the\n  `calc.js`/`schema.js` hashes match (`{maxBoxes}`/`{propertyBudget}` size it; a moved\n  hash reads `certified.stale:true` and is ignored). `{splitTree:'<path>.json.gz'}`\n  also writes the walk's own split tree there, so the leaf partition behind the\n  certificate can be rechecked without this engine. `universe.certified` carries\n  `{available, stale?, leaves, signatures, rows, boxes, budget, partial,\n  domain{declared, refused, accepted}, refused{rows, regions}}`, each `always` a\n  `certified {trueRows, falseRows, unknownRows, executedRows}`, and a proof over a\n  domain the oracle's own `throw` cut carries `domainRestricted: true` with the\n  subtraction in `claim`. `signatures` is the feasible branch-signature universe with a\n  witness each plus this deck's `{covered, of, notcovered}` (`Rule.table` reports the\n  same). An in-domain AUTHORED `throw` is a refused region subtracted from the claim;\n  any other runtime error still forfeits it. A book the abstraction cannot state\n  refuses typed on `universe.certified.code`, never as an exception:\n  `DIALECT_<reason>` · `RANGE_DIVISION` · `COERCION` · `LOOKUP_MISS` ·\n  `LOOKUP_MIXED` · `NULL_MEMBER` · `BOX_BUDGET` · `CERTIFICATE_MISMATCH` ·\n  `RUNTIME_ERROR`. A surviving\n  counterexample has two readings — the rules are wrong OR the property/scenario\n  expectation is wrong (a spec defect): fix the reviewed artifact, re-run, and keep the\n  counterexample as a saved scenario. `rejects` grades the **negative scenarios** — every\n  `_expect: 'schema-reject'` row put through `schema.js` alone (`calc.js` never runs) →\n  `[{_id, _label?, status, pass, why}]`. `status` is `REFUSED` (the claim holds — `why`\n  names what the shape objected to) / `ACCEPTED` (the failure — the shape is too loose) /\n  `NOTCHECKED` (the row carries no business input, so its mismatch is arithmetic, not\n  evidence). `accepts` is the other half of that gate — the saved rows the shape must ADMIT\n  → `{status, checked, incomplete, refused[]}`, `ACCEPTED` / `REFUSED` (a saved row its own\n  shape rejects: that row or the shape is wrong) / `NOTCHECKED` (no eligible row, so nothing\n  shows the shape admits anything). Read the pair: `rejects` green with `accepts`\n  NOTCHECKED is a shape that refuses everything.\n  `expectations` grades every saved row carrying `_expect: {output|outcome}` →\n  `{status, checked, failed[], notChecked?}`, `MATCHED` / `MISMATCHED` (the rules\n  contradict an author-supplied figure — one of the two is wrong) / `NOTCHECKED` (no\n  row graded; pinning is optional, not a defect). It is the only **per-row output**\n  oracle here — a figure meant to come from the requirement rather than from `calc.js`,\n  so it can be red the day it is written. (A `{req}`-linked `calc.always` is the other\n  externally-anchored verdict, at the level of a rule rather than a row.)\n  `outOfDomain` and `robustness` are the out-of-domain pair, present only when a declared\n  bound was crossed, and both findings, never verdicts. `outOfDomain` is the\n  rollup — one entry per axis, `{axis, lo?, hi?, members?, reason, rows, sources[]}` —\n  and every row or witness outside the domain is stamped `outOfDomain: [axis, …]`, so a\n  negative count priced by a boundary probe names itself instead of hiding in a string.\n  `robustness` is the detail: `{rows, failures, properties[], savedRows[], coverageOnly[],\n  unreachableBoundaries[]}` — the defensive guards the probes tripped, the saved rows the\n  declared domain forbids (fix the row or widen the generator), the arms only an impossible\n  input reaches (dead for every legal input — never \"add a scenario\"), and the thresholds\n  no legal input can cross. The domain itself is declared in `schema.js`\n  (`'#int[0,6]'`, `'#number[0,25]'`, one side open); `generator.js` may narrow it and is\n  refused if it widens it.\n  A passing verdict proves THIS row is refused, never that the gate refuses bad\n  input in general — read the `why`, since a row can be refused for a reason its author did\n  not intend. \"Is the rulebook complete and healthy — and is the fix data or rules?\"\n- `Rule.feasible(name, row)` — **can the business even sell this combination?** Runs the calc\n  over the fixed axis values (the rest seeded from `generator.js`) → `true` when the rules\n  permit it, `false` when a precondition deliberately rejects it. This is the same oracle\n  `Coverage.dimensions()` / `Coverage.coveringArray()` prune by, asked one cell at a time — so\n  it explains a missing cross-product cell as **impossible, not untested**, and it is why the\n  deck never proposes an order nobody could place. A calc *fault* (a bug, not a precondition)\n  reads `true` and logs a warning rather than silently pruning a real cell.\n- `Rule.sweep(name, {base?, dim?, output?})` — single-factor sensitivity (one input swept\n  across its classes → the output curve + outcome-flip marks); no `dim` → the tornado\n  (every input ranked by how far it moves the output). \"How sensitive, and where it tips.\"\n- `Rule.impact(name, {path, set|mul|add|pct, output?})` — perturb a data-resident lookup/\n  rate-table value and re-run the saved scenarios → per-scenario + aggregate output delta (the\n  actuary rate-change view). `path` is dot-separated **into `Rule.load(name).lookups`**, and\n  a rate table is usually an array — so an index is a step of its own:\n  `{path:'ageBands.0.factor', pct:7}` (the bracket form `ageBands[0].factor` means the same\n  thing). Every money figure it reports is rounded to 2dp. A layered file table's cell\n  is addressed `fees@CA/filing`; the what-if never writes the file.\n- `Rule.cells(name, {target?, cells?})` — which file-table cells the saved scenarios **exercise**:\n  a cell a row reads counts only when doubling it moves a declared figure (`figures = ['premium']`\n  in `schema.js`; none declared → every output field, `judgedOn: 'output'`). Summary per table\n  (`of` · `exercised` · `masked` · `notChecked` · `unread`); `target` `'all'` · `'unexercised'` ·\n  `[addresses]`; detail with `cells: {offset, limit}` or `cells: address`. `Rule.run(…, {trace:true})`\n  lists the cells one run read under `trace.cells`.\n- `Rule.export(name, {suite?|rows?|ids?, mapping?, format?})` — the deck as **one sheet** for a carrier\n  or a foreign runner: every saved row (a reject as `expected: {refusal}`) plus the named suite's\n  rows, or only `ids`, or raw input `rows` → `{book, digest, engine, figures, sheet: {digest}, rows:\n  [{id, label, provenance, input, outcome?, error?, outputs, reqs, expected}]}`, written immutably to\n  `evidence/sheets/<digest>.json`. `mapping` (a name under `evidence/mappings/`) speaks the carrier's\n  field and value names; `format: 'csv'` adds the flat projection (`vehicles[0].type`, `vehicles#`).\n- `Rule.match(name, {results, mapping?})` — grade a carrier's results file (`evidence/results/<f>.json`\n  or the object `{sheet: {digest}, mapping?, producer?, rows: [{case? | inputs, figure, error?, edition?,\n  worksheet?}]}`) against its sheet → `{counts, cases: [{case, class, reason?, ours, theirs,\n  difference?, reqs?, diagnosis?}], disclosures, ingest}`; classes `match` · `match (±x)` · `mismatch` ·\n  `not rated` · `unknown case` · `rated but expected refused`. Each call is persisted under `runs/`, the\n  latest per sheet wins; `Rule.match(name)` reads them. A rulebook, schema, mapping or engine that moved\n  since the export refuses `SHEET_STALE` naming it.\n- `Rule.prove(envelope)` — ingest a TLC run's `envelope.json` (project-relative; `etc/tlc-run.sh` writes it)\n  → `{definition, completion, obligations: [{name, outcome, tlc, criterion?, why?}], run}`; `outcome` `pass` ·\n  `counterexample` · `notchecked` · `stale` (a bound file or criterion moved) · `unbindable` (one is gone),\n  re-read at every report build. Persisted under `runs/`; the latest per spec and cfg wins.\n- `Rule.import(name, {mapping, source, ifMatch?})` — a carrier CSV or `.xlsx` sheet through\n  `evidence/mappings/<m>.import.json` into `tables/` (role `baseline`; an existing file needs `ifMatch: {<path>: tableShas[<path>]}`), `evidence/<role>/` or, for\n  target kind `ratebook`, `evidence/ratebooks/<name>.json` → `{written, rows: {from, to, counted, imported,\n  skipped: [{row, reason}]}}`. Nothing is inferred: types, units (`source` points · fraction · auto,\n  `target` number · fraction · plus-one), `places`, `pad`, `transforms` are declared.\n- `Rule.changeSet(name, {before, after, mapping, produces: {edition, effective, renewalEffective?,\n  jurisdictions, lines}, algorithm?})` — the cell diff of two files as a change set in\n  `evidence/changes/<id>.json`; `Rule.changeSetCells(name, id, {base?})` → its `cells` addresses (the\n  `target` of `Rule.cells`/`Rule.mutate`) and those `absent` from this book; `base` (the edition directory\n  the set produces, e.g. `iso27`) rebases each row's table onto it.\n- `Rule.editions(name, {records, rule?})` — the selection check: over the rate-book records in\n  `evidence/ratebooks/<records>.json` (or the object), per jurisdiction, line and transaction window,\n  which edition wins and whether it is the filed one, under `evidence/ratebooks/rule.json` (`rule` names\n  another file or passes the object; none refuses `RULE_UNDECLARED` — nothing defaults) → `{ok, checked,\n  complete, rule: {sha, ai}, windows: [{jurisdiction, line, transaction, from, to, winner, filedWinner,\n  finding}], notEvaluated, disclosures}`; findings `competing` · `ambiguous` · `not-filed` ·\n  `orphan-companion` · `none-selectable` · `ok`. An unknown status, a missing renewal date or an id\n  outside the rule's `editionId` pattern leaves that window in `notEvaluated`, never a finding. The\n  records never enter `lookup.editions`. `{output:'envelope'}` → the `karate-diff/v1` envelope.\n- `Rule.tableDiff(name, {against, changeSets, base?})` — grade an evidence table set (`against`: `evidence/under-test`\n  or `evidence/reference`, or a directory under one) against `tables/` — or only `tables/<base>/`, keyed\n  relative to it (a change-set table `<key>` or `<base>.<key>`) — with the ordered change-set ids\n  applied in memory — nothing under `tables/` moves. Per cell: `CELL_AS_DECLARED` (compatible) ·\n  `CELL_DECLARED_MISSING` · `CELL_WRONG_VALUE` · `CELL_UNEXPECTED` · `TABLE_FILE_UNEXPECTED` (breaking) →\n  `{ok, checked, complete, counts, tables, breaking, compatible, notEvaluated, disclosures}`; a row a change\n  set names that the expected side lacks, a stale `before`, a differing layout or column type, a book\n  table the evidence lacks, or no change set is `notEvaluated` by reason, never a finding. Persisted under\n  `runs/`; `Rule.report` renders the latest per set. `{output:'envelope'}` → the `karate-diff/v1` envelope.\n- `Rule.mutate(name, {suite, spec?, operation?, out?, handlers?, target?})` — **mutation testing:\n  would your tests notice if the rules broke?** Seeds defects into a COPY of the rulebook (comparison\n  flips, boundary off-by-ones, disabled arms, rate perturbations — the project file is\n  never touched), serves each mutant through the rule-backed mock from an isolated home\n  under `target/`, replays the suite, and classifies every assertion offline →\n  `{independentScore, rawKillRate, counts, mutants, worklist}`. The **worklist** names each\n  weakness actionably (\"no test would notice the volume-discount boundary breaking —\n  ORD-PRICE-001/1\"): strengthen those assertions, re-run, watch the score climb. Where no\n  literal is knowable, write a relation: call the same operation twice changing one input,\n  keep a value off the first response (`def normal = response.total`), compare\n  (`assert response.total > normal` monotonic · `==` invariant · swapped inputs symmetric).\n  It kills as independent; a value carried into the second request never counts. Statuses:\n  KILLED · SURVIVED · NOTCOVERED (no request reached the mutated site — a coverage gap,\n  excluded from the score) · SCREENED (indistinguishable on the enumerated deck) · TIMEOUT ·\n  INVALID. The score's denominator is KILLED + SURVIVED only — NOTCOVERED, SCREENED,\n  TIMEOUT and INVALID all sit outside it (they can never cap or pad either number). Both\n  numbers grade assertion strength **against the rulebook**; they transfer to the real API\n  exactly as far as the rulebook is faithful. Pass `handlers:'mock/handlers.js'` when the\n  priced operation sits inside a lifecycle (submit, then rate): the mutant is then served\n  through that overlay, exactly as the project's own mock serves it.\n- `Rule.mutate(name)` — no suite → the **headless self-run lane**: the same mutant catalog\n  grades the rulebook's OWN oracles (scenario `_expect` stored expectations + the\n  `calc.always`/`sometimes` properties), zero HTTP → `{selfRunScore, counts, oracleBook,\n  worklist}`. A surviving mutant means *nothing in the rulebook's own book would notice\n  that defect*; its worklist entry carries a ready-made counterexample row — store it as a\n  scenario with an `_expect` (`{\"output\": {…subset}}` and/or `{\"outcome\": v}`), or declare\n  the property that forbids it. `selfRunScore` grades the scenario book's vigilance, never\n  a suite's independence — the two scores are never comparable. On either lane `{target: 'read'|'all'|\n  [addresses]}` picks the file-table cells it perturbs (`'read'` = the cells scenarios read).\n- `Rule.bless(name)` / `Rule.drift(name)` — **the silent-drift audit: the tests stayed\n  green and the meaning moved. Bless BEFORE you change rules that already work** —\n  `drift` compares against the baseline on disk, so one taken after the edit has nothing\n  to compare and reports nothing. `bless` captures the rulebook's current behaviour as the\n  **verification baseline** (`.baseline/verification.json` — per-scenario outputs,\n  outcomes and branch paths, each invariant's status, every reject row's verdict) →\n  `{rule, ts, scenarios, rejects, invariants}`. It overwrites without prompting: blessing\n  is how a human ACCEPTS the current behaviour as the reference. `drift` re-runs\n  everything against the current files and reports what moved:\n  `invariantRegressions` (an invariant that held is now VIOLATED — or stopped being\n  evaluated) · `outputDrift` (a stored scenario whose oracle output changed **with no\n  edit to that scenario**, with a word-level diff) · `outcomeDrift` (its `calc.outcome`\n  or its branch path moved) · `rejectDrift` (a negative scenario's verdict moved —\n  `REFUSED`→`ACCEPTED` means somebody loosened `schema.js` and a stated claim quietly\n  stopped holding). `changed` is true iff one of those four is non-empty — gate CI on it.\n  Authored edits are reported but never set it: `added`/`removed` (scenarios,\n  reject rows or invariants that appeared or went), `edited` (rows whose input you\n  changed — their per-scenario legs are suppressed, since their output SHOULD move) and\n  `invariantImprovements` (VIOLATED→HOLDS, or the HOLDS↔COMPLETE precision change). No\n  baseline → `{firstRun:true}`; an unreadable one → `BASELINE_UNREADABLE`, never a silent\n  first run. Every rulebook file matching its baseline SHA → `{unchanged:true}` with\n  nothing re-run (the cheap CI path — `generator.js` counts, since it decides what the\n  explorer reaches and so what an invariant's grade can claim).\n- `Rule.report(name, {out?, mutation?, plan?, sutRevision?})` — render the self-contained, `file://`-openable\n  **Analysis report** HTML (Coverage decision table · Cells (file tables) · Value Ranges · Rule Check · What-If ·\n  Twin (when `twin.js` exists, with the `Openapi.docs` lifecycle pane) · Mutation (when a `Rule.mutate(name)` self-grade exists —\n  `mutation` names its report JSON, else the default `target/mutation-self/<name>/`)).\n  `plan` (an object or a project-relative JSON path) is the plan every twin walk runs under;\n  `sutRevision` is `Twin.check`'s — pass the ones `Twin.witness`/`Twin.orders` ran with.\n  The Twin section renders each pinned sequence's `Twin.live` run and shrink under that plan.\n- `Rule.cover([name][, {out?}])` — **project coverage from the rules, run-free**. Runs\n  every saved scenario through the calc oracle (omit `name` to cover ALL rulebooks) and writes\n  a unified `coverage-data.json` with TWO sources: **req** (the `calc.req` criteria the rules\n  realize) and **rules** (each scenario). Both read COVERED + `oracleOnly` — the rules realize\n  the behaviour, but nothing outside the rulebook checked it (a test or a `verify()` stamp\n  clears that). The serve-native loop closer (the interactive `Rule.run` emit is a no-op off a scenario thread).\n  Also projects every GUARDED `calc.always`/`sometimes` property (one with a `{req}` arg)\n  onto its criterion — verdicts decided off the explorer deck's IN-DOMAIN rows:\n  VIOLATED → FAILING (+ the\n  counterexample as evidence, a `readiness()` blocker), COMPLETE/HOLDS → positive\n  `oracleOnly` evidence (proven / sampled), NOTCHECKED → a visible-but-verdict-free link.\n  A reject row carrying `_req` projects too: REFUSED covers each named criterion with\n  `refusalOnly`, ACCEPTED reads FAILING, NOTCHECKED a verdict-free link — counted in\n  `rejects:{linked, unlinked}`.\n  The result's `guarantees` rollup\n  is `{guarded, complete, holds, violated, notchecked, violations:[{rule, property, req,\n  counterexample, repro}]}` — each violation's `repro` is a copy/paste `Rule.execute` call.\n  Read it back with `Requirement.matrix()` / `Coverage.summary()` → `{rules, scenarios, criteria,\n  byStatus:{req, rules}}`.\n\nAuthoring hints inside `calc.js` (all optional, no-op-cheap): **`calc.round(x[, dp])`**\nrounds money half-away-from-zero at `dp` places (default 2) — use it instead of\n`Math.round(x*100)/100`, which is IEEE-754 arithmetic and drops the penny on `1.005`\nwhile rounding `15.025` correctly, so spot-checking reads as proof;\n`calc.label(text)` names a rule\nat a decision; `calc.outcome(value)` declares the categorical result; `calc.always(name,\ncond[, {req:'<reqId>/<acId>'}])` a safety property that must hold on EVERY input the\ndeclared domain contains and\n`calc.sometimes(name, cond[, {req}])` a\nsituation the explored space must reach at least once (the PBT pair, checked across the\nswept space). The optional `{req}` names the acceptance criterion the property\nGUARDS: `Rule.cover` then projects its verdict into the requirements matrix — a violated\nguarded invariant reads FAILING on that criterion and blocks `Requirement.readiness()`\nwith the counterexample attached (a broken business rule is a release blocker, with the\ninput that breaks it); a holding one adds positive `oracleOnly` evidence. Property names\nand `{req}` ids must be string literals, names unique per rulebook — duplicates and\nunknown `{req}` ids make `Rule.cover` refuse with a typed error. `calc.levels(inputPath, [v…])` declares\nboundary values the black box can't reach; `calc.req('<reqId>/<acId>')` links a decision arm to\nan acceptance criterion — `Rule.run` surfaces the criteria a run reached as `reqs` and emits\nthem as OBSERVED hits, lighting those criteria COVERED in the requirements matrix (RTM). The\ncall claims the CLOSEST enclosing arm and no other, so `Rule.check`'s `notclaimed` lists every\narm none describes. A\nrulebook may add an optional `generator.js`\n(`function generate(g){…}`) — the constrained input domain (enum/int/bool/const/object/\narray+cardinality) the explorer sweeps. A declared bound is a MEMBERSHIP test, not a\ngeneration hint: rows outside it still run and are reported, but never decide a\nverdict — and an incoherent declaration (`lo > hi`, a fractional `g.int` bound, a non-finite\nbound, bad `g.array` cardinality, a boundary outside the bounds, one axis declared twice) is\nrefused at parse.\n\nAuthoring (writes; opts `{ifMatch: <mtimes value>}` guards against a stale edit → 409):\n- `Rule.create(name, {schema, calc, generator?, scenarios?})` — **create a NEW rulebook**\n  folder from scratch (the only verb that creates; the per-file writes below refuse to).\n  `schema`+`calc` are required JS text, `generator` optional, `scenarios` a JS array or JSON\n  (defaults `[]`); all parse-validated before any file is written → `{name, mtimes}`. Fills a\n  folder holding only `sources/`/`evidence/`; `RULE_EXISTS` (409) if a book is there.\n- `Rule.delete(name)` — **remove a whole rulebook** folder and its files →\n  `{name, status:'deleted', files}`; `RULE_NOT_FOUND` (404) if absent. The undo for a probe\n  or throwaway book. Irreversible — there is no trash.\n- `Rule.scenario.create(rule, row, opts?)` → `{id, mtime}`, or pass an ARRAY of rows for\n  one call and one write → `{ids, mtime}` (all-or-nothing: a bad row writes none) ·\n  `Rule.scenario.update(rule, id, patch, opts?)` · `Rule.scenario.delete(rule, id, opts?)`\n  → `{mtime}`.\n  A row with `_expect: 'schema-reject'` is a **negative scenario**: it claims that\n  `schema.js` must REFUSE this input. It is not a scenario — `calc.js` never runs it and\n  `Rule.run` refuses it; `Rule.check(name).rejects` is where its verdict appears. Add\n  `_req: '<reqId>/<acId>'` (or an array) to name the criteria it answers and `Rule.cover`\n  projects the verdict onto them. Use it to prove bad input is rejected instead of\n  assuming it. (The\n  OTHER `_expect` — an *object*, `{\"output\": {…}}` and/or `{\"outcome\": v}` — is the stored\n  expectation the self-run lane grades; the two are told apart by type. Any other string\n  value fails the load, naming the row. Derive that figure from the REQUIREMENT: one\n  copied from `Rule.execute`'s output agrees by construction and guards nothing.)\n- `Rule.calc.update(rule, calcJs, opts?)` — replace `calc.js` (syntax-checked first).\n- `Rule.schema.update(rule, schemaJs, opts?)` — replace `schema.js` (eval-validated).\n- `Rule.generator.update(rule, generatorJs, opts?)` — author/replace `generator.js` (the\n  optional input domain — add one to a rulebook that shipped without it). The write RUNS\n  `generate(g)`: an incoherent declaration is refused here (`GENERATOR_INVALID`, 400)\n  and nothing is written.\n- `Rule.twin.update(rule, twinJs, opts?)` — author/replace `twin.js` (the behaviour model).\n  The write LOADS it: a bad declaration is refused (`TWIN_INVALID`) and nothing is written.\n- `Rule.sequence.create(rule, rowOrRows, opts?)` → `{ids, mtime}` ·\n  `Rule.sequence.update(rule, id, patch, opts?)` · `Rule.sequence.delete(rule, id, opts?)`\n  → `{mtime}`. A row is `{steps:[{command, args?}], _label?, _id?, _expect?}` over the\n  commands `twin.js` declares; `_id` is minted when absent and `_from` is engine-written.\n  Needs a `twin.js` (`TWIN_NOT_FOUND`); a bad row is `SEQUENCE_INVALID` and writes nothing.\n- `Rule.sequence.create(rule, {promote: {plan?, counterexample}, _id?, _label?})` re-runs the\n  walk and writes that counterexample's own path — created alone, from an exhausted walk only;\n  `promote: {play: recordId}` writes a confirmed play record's steps instead (`Twin.help('play')`).\n- `Rule.project(rule, {out?})` → `{ok, spec, digest, notprojected, refusals, out}` — the\n  twin's transport binding projected; `spec` is the OpenAPI document as an object, written\n  to `rulebooks/<rule>/openapi.yaml` unless `{out:false}`. Reads declarations only, so two\n  runs are byte-identical. A command\n  with no route, an authored `request` or literal-row `args` is left out and named under\n  `notprojected`, beside the fixed fence; a model that under-declares refuses under\n  `refusals` and writes nothing. `Http.mock({twin: rule})` mounts that spec as the router\n  with the twin behind it. `Twin.help('binding')`.\n  `{against: 'api/claims.yaml', output?, failOn?, waivers?}` — a hand-written spec, by\n  convention under `api/` — writes nothing and returns the\n  diff envelope of that spec (baseline) against the projection, taxonomy `projection.diff/1`,\n  never breaking: `OPERATION_UNMODELLED` · `OPERATION_UNCLAIMED` · `*_DIFFERS` · `*_DEPARTS`\n  · `ENGINE_RESPONSE` · `CONSTRUCT_FENCED`. Done = `failOn: 'warning'` passes, findings\n  waived by name; options as `Openapi.help('diff')`.\n- `Twin.help()` — the behaviour model's own verbs and concepts.\n- `Rule.lookups.update(rule, lookups, {ifMatch})` — edit one home per call: the inline\n  `lookup` (`mtimes.calc`) or one `tables/` file (`tableShas[path]`). A file table loads as a\n  summary; read its rows with `Rule.lookups.rows(rule, key, {offset})` or `.cell(rule, address)`.\n\nA domain failure returns `{error, code, status}` (data, not a throw). Related: `Schema` (the\nshape) + this namespace's own scenarios (row-tables + the oracle-vs-live diff), the `requirements` skill."
      );
   }

   public static Rule atRulesHome(Path var0) {
      return new Rule(var0);
   }

   public Rule withRunPrep(Supplier<UnaryOperator<Builder>> var1) {
      this.runPrep = var1;
      return this;
   }

   public Rule withReportDir(Path var1) {
      this.reportDir = var1;
      return this;
   }

   public Rule withRoot(Path var1) {
      this.manager.a(var1);
      this.projectRoot = var1;
      return this;
   }

   public Rule withRequirementsHome(Path var1) {
      return this.withRequirementsHomes(var1 == null ? List.of() : List.of(var1));
   }

   public Rule withRequirementsHomes(List<Path> var1) {
      this.requirementsHomes = var1 == null ? List.of() : List.copyOf(var1);
      return this;
   }

   public Rule onStep(io.karatelabs.ext.rules.Rule.StepListener var1) {
      this.stepSink = var1;
      return this;
   }

   public Rule onServeTrace(Consumer<io.karatelabs.ext.rules.RuleEvaluationCapture.CapturedRuleEvaluation> var1) {
      this.serveTrace = var1;
      return this;
   }

   Map<String, Object> cover(String var1, Object var2) {
      if (this.requirementsHomes.isEmpty()) {
         return JsApi.missing("Rule.cover needs a requirements home (the serve profiles bind one) — none configured");
      } else {
         Path var3 = this.coverOutDir(var2);
         if (this.projectBuilder == null && var3 != null && holdsRunEvidence(var3)) {
            return Map.of(
               "error",
               "Rule.cover has no project scope here and "
                  + var3
                  + " already holds run evidence a run-less snapshot would erase — call it on serve (the profile binds the project scope) or pass {out} to another dir",
               "code",
               "NO_PROJECT_SCOPE",
               "status",
               409
            );
         } else {
            io.karatelabs.ext.coverage.CoverageProjectBuilder.CoverageProjectBuildResult var4;
            try {
               var4 = this.projectBuilder != null ? this.projectBuilder.apply(var3) : this.localSnapshot(var3);
            } catch (RuntimeException var14) {
               return JsApi.error(var14);
            }

            LinkedHashMap var5 = new LinkedHashMap();
            if (var3 != null) {
               try {
                  if (!var4.publish()) {
                     var5.put("superseded", true);
                  }

                  var5.put("dataFile", var3.resolve("ext/coverage/coverage-data.json").toString());
               } catch (IOException var13) {
                  return Map.of("error", "cannot write coverage-data: " + var13.getMessage(), "code", "IO_ERROR", "status", 500);
               }
            }

            ArrayList var6 = new ArrayList();
            if (var1 != null) {
               io.karatelabs.ext.coverage.RulebookCoverageResult var7 = var4.d().get(var1);
               if (var7 == null) {
                  var7 = this.producer().a(var1, new io.karatelabs.ext.coverage.collect.RequirementsCoverageSource(this.requirementsHomes, null, null));
               }

               var6.add(var7);
            } else {
               var6.addAll(var4.d().values());
            }

            for (io.karatelabs.ext.coverage.RulebookCoverageResult var8 : (Iterable<io.karatelabs.ext.coverage.RulebookCoverageResult>) (Iterable<?>) (var6)) {
               if (var8.hasError()) {
                  return var8.f();
               }
            }

            LinkedHashMap var16 = new LinkedHashMap<>(var1 != null ? ((io.karatelabs.ext.coverage.RulebookCoverageResult)var6.get(0)).e() : summarize(var6));
            var16.putAll(var5);
            LinkedHashMap var17 = new LinkedHashMap();
            Object var10 = var4.b().get("sources");
            if (var10 instanceof List) {
               for (Object var11 : (List)var10) {
                  if (var11 instanceof Map var12) {
                     var17.put(String.valueOf(var12.get("type")), var12.get("byStatus"));
                  }
               }
            }

            var16.put("byStatus", var17);
            return var16;
         }
      }
   }

   private static Map<String, Object> summarize(List<io.karatelabs.ext.coverage.RulebookCoverageResult> var0) {
      LinkedHashMap var1 = new LinkedHashMap();
      ArrayList var2 = new ArrayList();
      int var3 = 0;
      LinkedHashSet var4 = new LinkedHashSet();
      LinkedHashMap var5 = null;
      LinkedHashMap var6 = null;

      for (io.karatelabs.ext.coverage.RulebookCoverageResult var8 : var0) {
         var2.add(var8.c());
         var3 += ((Number)var8.e().getOrDefault("scenarios", 0)).intValue();
         var4.addAll((List)var8.e().getOrDefault("criteria", List.of()));
         if (var8.e().get("rejects") instanceof Map var9) {
            var5 = var5 == null ? new LinkedHashMap() : var5;

            for (Entry var11 : (Iterable<Entry>) (Iterable<?>) (var9.entrySet())) {
               var5.merge(String.valueOf(var11.getKey()), var11.getValue(), (var0x, var1x) -> (Integer)var0x + (Integer)var1x);
            }
         }

         if (var8.e().get("guarantees") instanceof Map var15) {
            if (var6 == null) {
               var6 = new LinkedHashMap();
               var6.put("violations", new ArrayList());
            }

            for (Entry var19 : (Iterable<Entry>) (Iterable<?>) (var15.entrySet())) {
               String var12 = String.valueOf(var19.getKey());
               if ("violations".equals(var12)) {
                  ((List)var6.get(var12)).addAll((List)var19.getValue());
               } else if (var19.getValue() instanceof Integer var13) {
                  var6.merge(var12, var13, (var0x, var1x) -> (Integer)var0x + (Integer)var1x);
               } else {
                  var6.put(var12, var19.getValue());
               }
            }
         }

         if (var8.e().get("next") != null) {
            var1.put("next", var8.e().get("next"));
         }
      }

      var1.put("rules", var2);
      var1.put("scenarios", var3);
      var1.put("criteria", new ArrayList(var4));
      if (var5 != null) {
         var1.put("rejects", var5);
      }

      if (var6 != null) {
         var1.put("guarantees", var6);
      }

      return var1;
   }

   private static boolean holdsRunEvidence(Path var0) {
      Path var1 = var0.resolve("ext/coverage/coverage-data.json");
      if (!Files.isRegularFile(var1)) {
         return false;
      } else {
         try {
            Object var3 = Json.of(Files.readString(var1)).value() instanceof Map var4 ? var4.get("executions") : null;
            if (var3 instanceof List) {
               for (Object var6 : (List)var3) {
                  if (var6 instanceof Map var7 && !String.valueOf(var7.get("runId")).startsWith("oracle:")) {
                     return true;
                  }
               }
            }
         } catch (Exception var8) {
         }

         return false;
      }
   }

   private io.karatelabs.ext.coverage.CoverageProjectBuilder.CoverageProjectBuildResult localSnapshot(Path var1) {
      var var2 = List.of(new io.karatelabs.ext.coverage.collect.RequirementsCoverageSource(this.requirementsHomes, null, null), new RulebookCoverageSource(this.rulesHome));
      return io.karatelabs.ext.coverage.CoverageProjectBuilder.build(new io.karatelabs.ext.coverage.CoverageProjectBuilder.CoverageProjectOptions().a(var2).a((io.karatelabs.ext.coverage.RulebookCoverageResult.RulebookCoverageProducer)this.producer()).reportDirectory(var1));
   }

   public RulebookCoverageProvider producer() {
      if (this.producer == null) {
         this.producer = new RulebookCoverageProvider(this.rulesHome, this.manager, this.runner);
      }

      return this.producer;
   }

   public Rule withProjectBuilder(Function<Path, io.karatelabs.ext.coverage.CoverageProjectBuilder.CoverageProjectBuildResult> var1) {
      this.projectBuilder = var1;
      return this;
   }

   private Path coverOutDir(Object var1) {
      return var1 instanceof Map var2 && var2.get("out") instanceof String var3 && !var3.isBlank() ? Path.of(var3) : this.reportDir;
   }

   Twin twin() {
      if (this.twin == null) {
         this.twin = Twin.over(this.manager);
      }

      return this.twin;
   }

   @Override
   public void close() {
      this.runner.close();
      if (this.twin != null) {
         this.twin.close();
      }
   }

   Object list() {
      try {
         ArrayList var1 = new ArrayList();

         for (io.karatelabs.ext.rules.RulebookRepository.RulebookSummary var3 : this.manager.b()) {
            LinkedHashMap var4 = new LinkedHashMap();
            var4.put("name", var3.a());
            var4.put("scenarioCount", var3.b());
            var4.put("mtimes", var3.c());
            var1.add(var4);
         }

         return var1;
      } catch (RuntimeException var5) {
         return JsApi.error(var5);
      }
   }

   Map<String, Object> load(String var1) {
      return this.load(var1, null);
   }

   Map<String, Object> load(String var1, Object var2) {
      if (var1 == null) {
         return JsApi.missing("Rule.load(name) requires a rule name (try Rule.list())");
      } else {
         try {
            io.karatelabs.ext.rules.RulebookRepository.RulebookDocument var3 = this.manager.b(var1);
            LinkedHashMap var4 = new LinkedHashMap();
            var4.put("name", var3.d());
            var4.put("schema", var3.e());
            var4.put("scenarios", dealt(var3.d(), var3.b()));
            var4.put("rejects", dealt(var3.d(), var3.c()));
            var4.put("calc", var3.g());
            var4.put("generator", var3.j());
            boolean var5 = var2 instanceof Map var6 && "full".equals(var6.get("tables"));
            var4.put("lookups", var5 ? fullLookups(var3) : summarized(var3));
            if (var5) {
               var4.put("tableHomes", tableHomes(var3));
            }

            var4.put("twin", var3.k());
            var4.put("sequences", var3.l());
            var4.put("mtimes", var3.i());
            var4.put("digest", var3.n().a());
            var4.put("tableShas", var3.a());
            ArrayList var10 = new ArrayList();

            for (io.karatelabs.ext.rules.RuleTableCatalog.TableDefinition var8 : var3.m().b().values()) {
               var10.add(var3.m().a(var8));
            }

            var4.put("tables", var10);
            return var4;
         } catch (RuntimeException var9) {
            return JsApi.error(var9);
         }
      }
   }

   private static Map<String, Object> summarized(io.karatelabs.ext.rules.RulebookRepository.RulebookDocument var0) {
      if (var0.m().a()) {
         return var0.h();
      } else {
         var var1 = new LinkedHashMap<>(var0.h());

         for (String var3 : var0.m().d().keySet()) {
            var1.remove(var3);
         }

         return var0.m().a(var1);
      }
   }

   private static int rowsInView(io.karatelabs.ext.rules.RuleTableCatalog.TableDefinition var0) {
      return var0.d() == null ? var0.i() : var0.h().size();
   }

   private static Map<String, String> tableHomes(io.karatelabs.ext.rules.RulebookRepository.RulebookDocument var0) {
      LinkedHashMap var1 = new LinkedHashMap();

      for (io.karatelabs.ext.rules.RuleTableCatalog.TableDefinition var3 : var0.m().b().values()) {
         if (var3.d() != null && rowsInView(var3) <= tableRowsMax()) {
            var3.h().forEach((var2, var3x) -> var1.put(var3.a() + "." + var2.replace('/', '.'), var3x));
         }
      }

      return var1;
   }

   private static Map<String, Object> fullLookups(io.karatelabs.ext.rules.RulebookRepository.RulebookDocument var0) {
      LinkedHashMap var1 = new LinkedHashMap();

      for (Entry var3 : var0.h().entrySet()) {
         var1.put((String)var3.getKey(), var3.getValue());
      }

      for (io.karatelabs.ext.rules.RuleTableCatalog.TableDefinition var10 : var0.m().b().values()) {
         if (rowsInView(var10) > tableRowsMax()) {
            var var4 = new LinkedHashMap<>(var0.m().a(var10));
            var4.put("truncated", true);
            String[] var5 = var10.a().split("\\.");
            LinkedHashMap var6 = var1;

            for (int var7 = 0; var7 < var5.length - 1; var7++) {
               LinkedHashMap var8 = new LinkedHashMap((Map)var6.get(var5[var7]));
               var6.put(var5[var7], var8);
               var6 = var8;
            }

            var6.put(var5[var5.length - 1], Map.of("_table", var4));
         }
      }

      return var1;
   }

   private static List<Map<String, Object>> dealt(String var0, List<Map<String, Object>> var1) {
      if (var1 == null) {
         return null;
      } else {
         ArrayList var2 = new ArrayList(var1.size());

         for (Map var4 : var1) {
            LinkedHashMap var5 = new LinkedHashMap(var4);
            if (var5.get("_id") != null) {
               var5.put("_deck", var0);
            }

            var2.add(var5);
         }

         return var2;
      }
   }

   Object execute(String var1, Object var2) {
      if (var1 == null) {
         return JsApi.missing("Rule.execute(name, row) requires a rule name");
      } else if (var2 == null) {
         return JsApi.missing("Rule.execute(name, row) requires an input row object");
      } else if (!(var2 instanceof Map)) {
         String var12 = var2 instanceof String var13
            ? " — did you mean `Rule.run('" + var1 + "', '" + var13 + "')`, which runs a stored scenarios.json row by id?"
            : ".";
         return JsApi.missing(
            "Rule.execute(name, row) takes an input row OBJECT, got "
               + (var2 instanceof String ? "the string '" + var2 + "'" : var2.getClass().getSimpleName())
               + var12
         );
      } else {
         try {
            io.karatelabs.ext.rules.RulebookRepository.RulebookDocument var3 = this.manager.b(var1);
            var var4 = Schema.closedDomainViolations(var3.e(), var2);
            if (!var4.isEmpty()) {
               return JsApi.error(new RuleApiException(400, "INVALID_INPUT", String.join("; ", var4)));
            } else {
               io.karatelabs.ext.rules.RuleEvaluator.RuleEvaluationResult var5 = this.serveTrace == null
                  ? this.runner.a(var3.g(), var3.m().e(), var2, null)
                  : RuleEvaluationCapture.a(this.runner, var3.g(), var3.m().e(), var2, this.serveTrace);
               var var6 = resultMap(var5, null, var2);
               String var7 = var2 instanceof Map var8 && var8.get("_id") != null ? String.valueOf(var8.get("_id")) : null;

               for (String var9 : var5.h()) {
                  LinkedHashMap var10 = new LinkedHashMap();
                  var10.put("rule", var1);
                  var10.put("oracle", true);
                  CoverageEmit.emit("req", var9, var10);
               }

               return new RuleResult(var1, var7, var6, this.stepSink);
            }
         } catch (RuntimeException var11) {
            return JsApi.error(var11);
         }
      }
   }

   Object feasibleVerb(String var1, Object var2) {
      if (var1 == null) {
         return JsApi.missing("Rule.feasible(name, row) requires a rule name");
      } else if (!(var2 instanceof Map)) {
         return JsApi.missing(
            "Rule.feasible(name, row) requires an input row object — the axis values to fix, e.g. Rule.feasible('order-pricing-restricted', { customerTier:'silver', region:'EU' })"
         );
      } else {
         try {
            return this.feasible(var1, (Map<String, Object>)var2);
         } catch (RuntimeException var4) {
            return JsApi.error(var4);
         }
      }
   }

   @Override
   public boolean feasible(String var1, Map<String, Object> var2) {
      try {
         io.karatelabs.ext.rules.RulebookRepository.RulebookDocument var3 = this.manager.b(var1);
         var var4 = baseWitness(var3);
         if (var2 != null) {
            var4.putAll(var2);
         }

         this.runner.a(var3.g(), var3.m().e(), var4, null);
         return true;
      } catch (RuleApiException var5) {
         if (isDeliberateRejection(var5)) {
            return false;
         } else {
            logger.warn(
               "feasibility oracle: rulebook '{}' faulted on candidate {} — NOT pruning (a calc bug, not an infeasible cell): {}", var1, var2, var5.toString()
            );
            return true;
         }
      } catch (RuntimeException var6) {
         logger.warn("feasibility oracle: rulebook '{}' failed on candidate {} — NOT pruning: {}", var1, var2, var6.toString());
         return true;
      }
   }

   private static boolean isDeliberateRejection(RuleApiException var0) {
      if (!"RUN_FAILED".equals(var0.b)) {
         return false;
      } else if (!((var0.getCause() instanceof io.karatelabs.ext.rules.CalcExecutor.CalcExecutionException var2 ? var2.getCause() : var0.getCause()) instanceof EngineException var4)) {
         return false;
      } else {
         String var5 = var4.getJsErrorName();
         return var5 == null || !ENGINE_FAULT_ERRORS.contains(var5);
      }
   }

   public List<Map<String, Object>> replay(String var1, List<Map<String, Object>> var2) {
      ArrayList var3 = new ArrayList();

      io.karatelabs.ext.rules.RulebookRepository.RulebookDocument var4;
      List var5;
      try {
         var4 = this.manager.b(var1);
         ArrayList var11 = new ArrayList();

         for (Map var14 : var2) {
            var11.add(inputOf(var14));
         }

         var5 = RuleInputSpaceExplorer.b(var4.g(), var4.m().e(), var4.e(), var4.j(), var11);
      } catch (RuntimeException var10) {
         RuntimeException var6 = var10;

         for (Map var8 : var2) {
            LinkedHashMap var9 = new LinkedHashMap();
            var9.put("property", var8.get("property"));
            var9.put("error", String.valueOf(var6.getMessage()));
            var3.add(var9);
         }

         return var3;
      }

      for (int var12 = 0; var12 < var2.size(); var12++) {
         var3.add(
            this.replayOne(
               var4,
               String.valueOf(((Map)var2.get(var12)).get("property")),
               inputOf((Map<String, Object>)var2.get(var12)),
               (List<Map<String, Object>>)var5.get(var12)
            )
         );
      }

      return var3;
   }

   private static Map<String, Object> inputOf(Map<String, Object> var0) {
      return var0.get("input") instanceof Map var1 ? new LinkedHashMap<>(var1) : new LinkedHashMap<>();
   }

   private Map<String, Object> replayOne(io.karatelabs.ext.rules.RulebookRepository.RulebookDocument var1, String var2, Map<String, Object> var3, List<Map<String, Object>> var4) {
      LinkedHashMap var5 = new LinkedHashMap();
      var5.put("property", var2);
      var5.put("input", var3);
      var5.put("schemaValid", Schema.validateAgainst(var1.e(), var3).pass);
      var5.put("inDomain", var4.isEmpty());
      if (!var4.isEmpty()) {
         var5.put("offending", var4);
      }

      try {
         io.karatelabs.ext.rules.RuleEvaluator.RuleEvaluationResult var6 = this.runner.a(var1.g(), var1.m().e(), var3, null);
         var5.put("threw", false);
         boolean var12 = false;
         boolean var8 = false;

         for (Object[] var10 : var6.i()) {
            if (var2.equals(var10[0]) && "always".equals(var10[3])) {
               var12 = true;
               if (!Boolean.TRUE.equals(var10[2])) {
                  var8 = true;
               }
            }
         }

         var5.put("evaluated", var12);
         var5.put("falsifies", var8);
      } catch (RuleApiException var11) {
         boolean var7 = isDeliberateRejection(var11);
         var5.put("threw", var7);
         var5.put("faulted", !var7);
         var5.put("why", var11.getMessage());
         var5.put("evaluated", false);
         var5.put("falsifies", false);
      }

      return var5;
   }

   private static Map<String, Object> baseWitness(io.karatelabs.ext.rules.RulebookRepository.RulebookDocument var0) {
      String var1 = var0.j();
      if (var1 != null && !var1.isBlank()) {
         Object var2 = io.karatelabs.ext.common.SandboxExecutor.a(Generator.a(var1).b());
         return var2 instanceof Map ? new LinkedHashMap<>((Map<? extends String, ? extends Object>)var2) : new LinkedHashMap<>();
      } else {
         return new LinkedHashMap<>();
      }
   }

   Map<String, Object> run(String var1, String var2, Object var3) {
      if (var1 != null && var2 != null) {
         try {
            io.karatelabs.ext.rules.RulebookRepository.RulebookDocument var4 = this.manager.b(var1);
            var var5 = RulebookRepository.a(var4, var2);
            if (RulebookRepository.a(var5)) {
               throw rejectRowRefusal(var1, var2);
            } else {
               BranchOutcomeCollector var6 = optFlag(var3, "trace") ? new BranchOutcomeCollector() : null;
               TreeSet var7 = var6 != null && !var4.m().a() ? new TreeSet() : null;
               io.karatelabs.ext.rules.RuleEvaluator.RuleEvaluationResult var8 = this.runner.a(var4.g(), var7 == null ? var4.m().e() : var4.m().a(var7::add), var5, var6);
               LinkedHashMap var9 = new LinkedHashMap();
               var9.put("durationMs", var8.c());
               if (var8.e()) {
                  var9.put("truncated", true);
               }

               CoverageEmit.emit("rules", var1 + "/" + var2, var9);

               for (String var11 : var8.h()) {
                  LinkedHashMap var12 = new LinkedHashMap();
                  var12.put("rule", var1);
                  var12.put("scenario", var2);
                  var12.put("oracle", true);
                  CoverageEmit.emit("req", var11, var12);
               }

               var var14 = resultMap(var8, var2, var5);
               if (optFlag(var3, "diff")) {
                  this.applyDiff(var14, var1, var2, var4.i(), var8);
               }

               if (var6 != null) {
                  var var15 = var6.b(RuleInputSpaceExplorer.b(var4.g()));
                  if (var7 != null) {
                     var15.put("cells", new ArrayList(var7));
                  }

                  var14.put("trace", var15);
               }

               return var14;
            }
         } catch (RuntimeException var13) {
            return JsApi.error(var13);
         }
      } else {
         return JsApi.missing("Rule.run(name, scenarioId) requires a rule name and scenario id");
      }
   }

   Map<String, Object> explore(String var1, Object var2) {
      if (var1 == null) {
         return JsApi.missing("Rule.explore(name) requires a rule name (try Rule.list())");
      } else {
         try {
            if (badMaxProduct(var2) != null) {
               return JsApi.missing("Rule.explore opts.maxProduct must be a positive number (the full-product row cap), got " + badMaxProduct(var2));
            } else if (badStrength(var2) != null) {
               return JsApi.missing(
                  "Rule.explore opts.strength must be a whole number of 2 or more (the t-way deck strength; 2 = all-pairs), got " + badStrength(var2)
               );
            } else if (badMaxSearch(var2) != null) {
               return JsApi.missing(
                  "Rule.explore opts.maxSearch must be a non-negative number (the corner-hunt and cell-search row cap; 0 disables the hunt), got "
                     + badMaxSearch(var2)
               );
            } else {
               String var3 = suiteStrategy(var2);
               io.karatelabs.ext.rules.RulebookRepository.RulebookDocument var4 = this.manager.b(var1);
               CellSensitivityPlanner var5 = this.cellSuite(var4, var3, var2);
               Long var6 = maxProductOf(var2);
               if (var6 == null && "product".equals(var3)) {
                  var6 = 50000L;
               }

               io.karatelabs.ext.rules.RuleInputSpaceExplorer.InputSpaceExplorationResult var7 = this.explore(
                  var4, var3, var6, maxSearchOr(var2), strengthOf(var2), RuleInputSpaceCertificateBuilder.a(this.manager.c(var1), var4.n().c(), var4.i().get("schema")), var5
               );
               if ("product".equals(var3)) {
                  Map var8 = (Map)var7.a().get("universe");
                  if (!Boolean.TRUE.equals(var8.get("walked"))) {
                     LinkedHashMap var9 = new LinkedHashMap();
                     boolean var10 = Boolean.TRUE.equals(var8.get("enumerable"));
                     var9.put(
                        "error",
                        var10
                           ? "the raw-value product is "
                              + var8.get("product")
                              + " rows — over the "
                              + var8.get("maxProduct")
                              + " cap; raise {maxProduct} or use suite:'pairwise'"
                           : "the input domain is not fully enumerable — see causes; use suite:'pairwise' for the sampled deck"
                     );
                     var9.put("code", var10 ? "PRODUCT_OVERCAP" : "NOT_ENUMERABLE");
                     var9.put("status", 400);
                     var9.put("universe", var8);
                     return var9;
                  }
               }

               LinkedHashMap var12 = new LinkedHashMap();
               var7.a().forEach((var2x, var3x) -> {
                  if (var5 != null && "suite".equals(var2x)) {
                     var12.put("cells", var5.a());
                  }

                  var12.put(var2x, var3x);
               });
               var12.put("summary", var7.c());
               return var12;
            }
         } catch (RuntimeException var11) {
            return JsApi.error(var11);
         }
      }
   }

   Map<String, Object> table(String var1, Object var2) {
      if (var1 == null) {
         return JsApi.missing("Rule.table(name) requires a rule name (try Rule.list())");
      } else if (badMaxSearch(var2) != null) {
         return JsApi.missing("Rule.table opts.maxSearch must be a non-negative number, got " + badMaxSearch(var2));
      } else {
         try {
            String var3 = suiteStrategy(var2);
            io.karatelabs.ext.rules.RulebookRepository.RulebookDocument var4 = this.manager.b(var1);
            List var5 = var4.b() == null ? List.of() : var4.b();
            io.karatelabs.ext.rules.RuleInputSpaceExplorer.InputSpaceExplorationResult var6 = this.explore(var4, var3, null, maxSearchOr(var2), 2, null, this.cellSuite(var4, var3, var2));
            List var7 = (List)var6.a().get("suite");
            Map var8 = RuleCoverageMatrixBuilder.a(var4.g(), var4.m().e(), var4.e(), var5, var7, this.runner, dimPaths(var6.a()), labelsByLine(var6.a()));
            var8.put("coverage", var6.a().get("coverage"));
            var8.put("notcovered", var6.a().get("notcovered"));
            Map var9 = RuleCoverageMatrixBuilder.a(var8, RuleInputSpaceCertificateBuilder.a(this.manager.c(var1), var4.n().c(), var4.i().get("schema")));
            if (var9 != null) {
               var8.put("signatures", var9);
            }

            return JsApi.applyOutputConvention(var8, RuleCoverageMatrixBuilder.a(var8), var2);
         } catch (RuntimeException var10) {
            return JsApi.error(var10);
         }
      }
   }

   @Override
   public Map<String, Object> ranges(String var1, Object var2) {
      if (var1 == null) {
         return JsApi.missing("Rule.ranges(name) requires a rule name (try Rule.list())");
      } else {
         try {
            String var3 = suiteStrategy(var2);
            return RuleAnalysisEngine.a(this.manager.b(var1), "cells".equals(var3) ? "coverage" : var3);
         } catch (RuntimeException var4) {
            return JsApi.error(var4);
         }
      }
   }

   public Map<String, Object> check(String var1, Object var2) {
      if (var1 == null) {
         return JsApi.missing("Rule.check(name) requires a rule name (try Rule.list())");
      } else if (badMaxProduct(var2) != null) {
         return JsApi.missing("Rule.check opts.maxProduct must be a positive number (the full-product row cap), got " + badMaxProduct(var2));
      } else if (badMaxSearch(var2) != null) {
         return JsApi.missing(
            "Rule.check opts.maxSearch must be a non-negative number (the corner-hunt row cap; 0 disables the hunt), got " + badMaxSearch(var2)
         );
      } else if (badSplitTree(var2) != null) {
         return JsApi.missing("Rule.check opts.splitTree " + badSplitTree(var2));
      } else {
         try {
            io.karatelabs.ext.rules.RulebookRepository.RulebookDocument var3 = this.manager.b(var1);
            return RuleAnalysisEngine.a(var3, maxProductOf(var2), maxSearchOf(var2), this.certificate(var1, var3, var2));
         } catch (RuntimeException var4) {
            return JsApi.error(var4);
         }
      }
   }

   private io.karatelabs.ext.rules.RuleInputSpaceCertificateBuilder.InputSpaceCertificateResult certificate(String var1, io.karatelabs.ext.rules.RulebookRepository.RulebookDocument var2, Object var3) {
      String var4 = var2.n().c();
      String var5 = var2.i().get("schema");
      if (var3 instanceof Map var6 && Boolean.TRUE.equals(var6.get("certify"))) {
         io.karatelabs.ext.rules.analysis.AbstractInterpreter.AnalysisBudget var17 = new io.karatelabs.ext.rules.analysis.AbstractInterpreter.AnalysisBudget(
            longOpt(var6, "maxBoxes", io.karatelabs.ext.rules.analysis.AbstractInterpreter.AnalysisBudget.a.a()), longOpt(var6, "propertyBudget", io.karatelabs.ext.rules.analysis.AbstractInterpreter.AnalysisBudget.a.b())
         );
         Object var8 = var6.get("splitTree");

         try {
            Map var9;
            if (var8 == null) {
               var9 = RuleInputSpaceCertificateBuilder.a(var2.g(), var2.m().e(), var2.e(), var4, var5, var17, io.karatelabs.ext.rules.analysis.SplitTreeListener.b);
            } else {
               try (io.karatelabs.ext.rules.analysis.SplitTreeJsonWriter var10 = new io.karatelabs.ext.rules.analysis.SplitTreeJsonWriter(Path.of((String)var8), var1, var4, var5, "5")) {
                  var9 = RuleInputSpaceCertificateBuilder.a(var2.g(), var2.m().e(), var2.e(), var4, var5, var17, var10);
                  var10.a();
               }
            }

            RuleInputSpaceCertificateBuilder.a(this.manager.c(var1), var9);
            return io.karatelabs.ext.rules.RuleInputSpaceCertificateBuilder.InputSpaceCertificateResult.a(var9);
         } catch (io.karatelabs.ext.rules.analysis.RuleAnalysisException var15) {
            return io.karatelabs.ext.rules.RuleInputSpaceCertificateBuilder.InputSpaceCertificateResult.a(var15);
         }
      } else {
         io.karatelabs.ext.rules.RuleInputSpaceCertificateBuilder.InputSpaceCertificateResult var7 = RuleInputSpaceCertificateBuilder.a(this.manager.c(var1), var4, var5);
         if (var7 != null && var7.a()) {
            try {
               io.karatelabs.ext.rules.analysis.CalcDialectAnalyzer.a(Engine.parse(var2.g()), var2.m().e().keySet());
               return var7;
            } catch (io.karatelabs.ext.rules.analysis.RuleAnalysisException var16) {
               return io.karatelabs.ext.rules.RuleInputSpaceCertificateBuilder.InputSpaceCertificateResult.a(var16);
            }
         } else {
            return var7;
         }
      }
   }

   private static long longOpt(Map<?, ?> var0, String var1, long var2) {
      return var0.get(var1) instanceof Number var4 && var4.longValue() > 0L ? var4.longValue() : var2;
   }

   private static Long maxProductOf(Object var0) {
      return var0 instanceof Map var2 && var2.get("maxProduct") instanceof Number var1 ? var1.longValue() : null;
   }

   private static Integer maxSearchOf(Object var0) {
      return var0 instanceof Map var2 && var2.get("maxSearch") instanceof Number var1 ? var1.intValue() : null;
   }

   private static Object badSplitTree(Object var0) {
      if (var0 instanceof Map var1 && var1.containsKey("splitTree")) {
         Object var2 = var1.get("splitTree");
         if (var2 instanceof String var3 && !var3.isBlank()) {
            return Boolean.TRUE.equals(var1.get("certify")) ? null : "writes the tree of the walk {certify: true} runs, so it needs that option too";
         } else {
            return "must be a non-empty file path (a '.json.gz' the split tree is written to), got " + (var2 == null ? "null" : var2);
         }
      } else {
         return null;
      }
   }

   private static Object badMaxSearch(Object var0) {
      if (var0 instanceof Map var1 && var1.containsKey("maxSearch")) {
         Object var2 = var1.get("maxSearch");
         return var2 instanceof Number var3 && var3.intValue() >= 0 ? null : (var2 == null ? "null" : var2);
      } else {
         return null;
      }
   }

   private static int strengthOf(Object var0) {
      return var0 instanceof Map var2 && var2.get("strength") instanceof Number var1 ? var1.intValue() : 2;
   }

   private static Object badStrength(Object var0) {
      if (var0 instanceof Map var1 && var1.containsKey("strength")) {
         Object var2 = var1.get("strength");
         return var2 instanceof Number var3 && var3.doubleValue() == var3.intValue() && var3.intValue() >= 2 ? null : (var2 == null ? "null" : var2);
      } else {
         return null;
      }
   }

   private static Object badMaxProduct(Object var0) {
      if (var0 instanceof Map var1 && var1.containsKey("maxProduct")) {
         Object var2 = var1.get("maxProduct");
         return var2 instanceof Number var3 && var3.longValue() >= 1L ? null : (var2 == null ? "null" : var2);
      } else {
         return null;
      }
   }

   Map<String, Object> sweep(String var1, Object var2) {
      if (var1 == null) {
         return JsApi.missing("Rule.sweep(name) requires a rule name (try Rule.list())");
      } else {
         try {
            return RuleAnalysisEngine.a(this.manager.b(var1), this.runner, optsMap(var2));
         } catch (RuntimeException var4) {
            return JsApi.error(var4);
         }
      }
   }

   Map<String, Object> impact(String var1, Object var2) {
      if (var1 == null) {
         return JsApi.missing("Rule.impact(name) requires a rule name (try Rule.list())");
      } else {
         try {
            return RuleAnalysisEngine.b(this.manager.b(var1), this.runner, optsMap(var2));
         } catch (RuntimeException var4) {
            return JsApi.error(var4);
         }
      }
   }

   Map<String, Object> cells(String var1, Object var2) {
      if (var1 == null) {
         return JsApi.missing("Rule.cells(name, {target?, cells?}) requires a rule name (try Rule.list())");
      } else {
         try {
            io.karatelabs.ext.rules.RulebookRepository.RulebookDocument var3 = this.manager.b(var1);
            Map var4 = optsMap(var2) == null ? Map.of() : optsMap(var2);
            Object var5 = var3.b();
            if (var4.get("rows") instanceof List var6) {
               var5 = new ArrayList();

               for (Object var8 : var6) {
                  if (!(var8 instanceof Map)) {
                     return JsApi.missing("Rule.cells opts.rows is a list of input rows");
                  }

                  ((java.util.List)var5).add((Map)var8);
               }
            }

            var var10 = new CellCoverageAnalyzer(var3, this.runner).a(var4.get("target"), var4.get("cells"), (List<Map<String, Object>>)var5, tableRowsMax());
            if (Integer.valueOf(0).equals(var10.get("of"))) {
               JsApi.hint(
                  var10, var3.m().a() ? "this rulebook has no tables/ — cell coverage counts file-table cells only" : "no cell to judge — nothing is checked"
               );
            }

            return var10;
         } catch (RuntimeException var9) {
            return JsApi.error(var9);
         }
      }
   }

   Map<String, Object> export(String var1, Object var2) {
      if (var1 == null) {
         return JsApi.missing("Rule.export(name, {suite?|rows?|ids?, mapping?, format?}) requires a rule name (try Rule.list())");
      } else {
         Map var3 = optsMap(var2) == null ? Map.of() : optsMap(var2);
         int var4 = (var3.get("suite") != null ? 1 : 0) + (var3.get("rows") != null ? 1 : 0) + (var3.get("ids") != null ? 1 : 0);
         if (var4 > 1) {
            return JsApi.missing("Rule.export takes one of suite, rows or ids");
         } else if (badMaxSearch(var3) != null) {
            return JsApi.missing("Rule.export opts.maxSearch must be a non-negative number, got " + badMaxSearch(var3));
         } else {
            String var5 = var3.get("format") == null ? "json" : String.valueOf(var3.get("format"));
            if (!var5.equals("json") && !var5.equals("csv")) {
               return JsApi.missing("Rule.export format is 'json' (default) or 'csv', not '" + var5 + "'");
            } else {
               try {
                  io.karatelabs.ext.rules.RulebookRepository.RulebookDocument var6 = this.manager.b(var1);
                  Path var7 = this.manager.c(var1);
                  EvidenceMapping var8 = var3.get("mapping") == null ? null : io.karatelabs.ext.rules.EvidenceMapping.a(var7, var3.get("mapping"));
                  ArrayList var9 = new ArrayList();
                  Map var10 = null;
                  Object var13 = var3.get("ids");
                  if (var13 instanceof List) {
                     for (Object var14 : (List)var13) {
                        var9.add(io.karatelabs.ext.rules.EvidenceSheetExporter.SheetCandidate.a(RulebookRepository.a(var6, String.valueOf(var14))));
                     }
                  } else {
                     if (var3.get("ids") != null) {
                        return JsApi.missing("Rule.export opts.ids is a list of scenario ids");
                     }

                     var13 = var3.get("rows");
                     if (var13 instanceof List) {
                        for (Object var31 : (List)var13) {
                           if (!(var31 instanceof Map)) {
                              return JsApi.missing("Rule.export opts.rows is a list of input rows");
                           }

                           var9.add(io.karatelabs.ext.rules.EvidenceSheetExporter.SheetCandidate.a((Map<String, Object>)var31, null));
                        }
                     } else {
                        if (var3.get("rows") != null) {
                           return JsApi.missing("Rule.export opts.rows is a list of input rows");
                        }

                        for (Map var32 : var6.f()) {
                           var9.add(io.karatelabs.ext.rules.EvidenceSheetExporter.SheetCandidate.a(var32));
                        }

                        if (var3.get("suite") != null) {
                           var13 = suiteStrategy(var3);
                           CellSensitivityPlanner var33 = this.cellSuite(var6, (String)var13, var3);
                           io.karatelabs.ext.rules.RuleInputSpaceExplorer.InputSpaceExplorationResult var15 = this.explore(var6, (String)var13, null, maxSearchOr(var3), 2, null, var33);

                           for (Map var17 : (Iterable<Map>) (Iterable<?>) ((List)var15.a().get("suite"))) {
                              if (var17.get("input") instanceof Map var18) {
                                 var9.add(io.karatelabs.ext.rules.EvidenceSheetExporter.SheetCandidate.a(var18, var17));
                              }
                           }

                           if (var33 != null) {
                              var10 = var33.a();
                           }
                        }
                     }
                  }

                  io.karatelabs.ext.rules.EvidenceSheetExporter.EvidenceSheet var23 = EvidenceSheetExporter.a(var6, this.runner, var9, var8, var10);
                  String var24 = var5.equals("csv") ? EvidenceSheetExporter.a(var23, var8, EvidenceSheetExporter.a(var6), tableRowsMax()) : null;
                  boolean var30 = !Files.exists(var7.resolve("evidence/sheets").resolve(var23.b() + ".json"));
                  Path var34 = EvidenceSheetExporter.a(var7, var23);
                  var var35 = new LinkedHashMap<>(var23.c());
                  var35.put("path", var7.relativize(var34).toString());
                  if (var24 != null) {
                     Path var36 = var34.resolveSibling(var23.b() + ".csv");

                     try {
                        EvidenceSheetExporter.a(var36, var24);
                     } catch (IOException var20) {
                        if (var30) {
                           Files.deleteIfExists(var34);
                        }

                        throw var20;
                     }

                     var35.remove("rows");
                     var35.put("csv", var24);
                     var35.put("csvPath", var7.relativize(var36).toString());
                  }

                  return var35;
               } catch (IOException var21) {
                  return JsApi.error(new UncheckedIOException(var21));
               } catch (RuntimeException var22) {
                  return JsApi.error(var22);
               }
            }
         }
      }
   }

   Map<String, Object> match(String var1, Object var2) {
      if (var1 == null) {
         return JsApi.missing("Rule.match(name, {results, mapping?}) requires a rule name (try Rule.list())");
      } else if (var2 == null) {
         try {
            this.manager.b(var1);
            LinkedHashMap var18 = new LinkedHashMap();
            var18.put("book", var1);
            var18.put("sheets", io.karatelabs.ext.coverage.collect.MatchResultArtifactParser.a(this.runsDir(), var1));
            return var18;
         } catch (RuntimeException var16) {
            return JsApi.error(var16);
         }
      } else {
         var var3 = optsMap(var2);
         if (var3 != null && var3.get("results") != null) {
            try {
               io.karatelabs.ext.rules.RulebookRepository.RulebookDocument var4 = this.manager.b(var1);
               Path var5 = this.manager.c(var1);
               var var6 = results(var5, var3.get("results"));
               if (var6.get("sheet") instanceof Map var7 && var7.get("digest") instanceof String var8) {
                  var var20 = EvidenceSheetExporter.b(var5, var8);
                  if (var20 == null) {
                     throw new RuleApiException(404, "SHEET_UNKNOWN", "no sheet " + var8 + " under evidence/sheets/ — Rule.export writes one");
                  } else {
                     Object var10 = var6.get("mapping");
                     if (var10 == null && var20.get("mapping") instanceof Map var11) {
                        var10 = var11.get("name");
                     }

                     Object var21 = var3.get("mapping");
                     if (var10 != null && var21 != null) {
                        throw new RuleApiException(
                           400,
                           "MAPPING_AMBIGUOUS",
                           "the results file or sheet names mapping '" + var10 + "' and the call passes one — a match has one mapping"
                        );
                     } else if (var10 == null && var21 == null) {
                        throw new RuleApiException(
                           400,
                           "MAPPING_REQUIRED",
                           "no mapping: name one in the results file (mapping: '<name>') or pass {mapping} — the figure, scale and keys come from it"
                        );
                     } else {
                        EvidenceMapping var22 = io.karatelabs.ext.rules.EvidenceMapping.a(var5, var10 != null ? var10 : var21);
                        var var13 = EvidenceSheetComparator.a(var4, var20, var6, var22, RatebookSelectionRule.a(var5));
                        if (var6.get("producer") instanceof Map var14) {
                           var13.put("producer", var14);
                        }

                        io.karatelabs.ext.rules.WorksheetMatchDiagnostics.a(var4, this.runner, var20, var13, var22, List.of());
                        List var23 = linkReqs(var20, var13);
                        var13.put("ingest", io.karatelabs.ext.coverage.collect.MatchResultArtifactParser.a(this.runsDir(), var13, var23));
                        return var13;
                     }
                  }
               } else {
                  throw new RuleApiException(400, "RESULTS_INVALID", "results.sheet.digest names the exported sheet the rows rate");
               }
            } catch (RuntimeException var17) {
               return JsApi.error(var17);
            }
         } else {
            return JsApi.missing(
               "Rule.match(name, {results, mapping?}) requires results — a file under the rulebook (evidence/results/<file>.json) or the results object"
            );
         }
      }
   }

   // $VF: One or more variable merging failures!
   // $VF: Could not properly define all variable types!
   // Please report this to the Vineflower issue tracker, at https://github.com/Vineflower/vineflower/issues with a copy of the class file (if you have the rights to distribute it!)
   Map<String, Object> prove(String var1) {
      if (var1 == null) {
         return JsApi.missing("Rule.prove(envelope) requires a project-relative envelope.json (etc/tlc-run.sh writes one)");
      } else {
         try {
            Path var2 = (this.projectRoot != null ? this.projectRoot : this.rulesHome.toAbsolutePath().getParent()).toAbsolutePath().normalize();
            var var3 = proofJson(var2, var1);
            if (!(var3.get("tool") instanceof Map var4 && "TLC".equals(var4.get("name")))) {
               throw new RuleApiException(400, "PROOF_UNSUPPORTED", "only a TLC envelope is ingested (tool.name 'TLC')");
            } else if (var3.get("inputs") instanceof Map var22 && var22.get("mapping") instanceof Map var6 && var6.get("path") instanceof String var7) {
               var var25 = proofJson(var2, var7);
               if (var25.get("spec") instanceof String && var25.get("cfg") instanceof String) {
                  String var9 = io.karatelabs.ext.coverage.collect.ProofArtifactParser.a(var22, var25);
                  if (var9 != null) {
                     throw new RuleApiException(
                        400,
                        "PROOF_INVALID",
                        "the envelope's " + var9 + " is not a {path, sha256} digest record matching '" + var7 + "' — rerun etc/tlc-run.sh"
                     );
                  } else if (var3.get("obligations") instanceof List var10 && !var10.isEmpty()) {
                     TreeMap var26 = new TreeMap();
                     Iterator var12 = var10.iterator();

                     while (var12.hasNext()) {
                        if (!(var12.next() instanceof Map var14) || !(var14.get("name") instanceof String var15)) {
                           throw new RuleApiException(400, "PROOF_INVALID", "an obligation without a name");
                        }

                        if (var14.get("binding") instanceof Map var37) {
                           ArrayList var39 = new ArrayList();

                           for (Object var41 : var37.get("content") instanceof List<?> values ? values : List.of()) {
                              var39.add(var41 instanceof Map var42 ? var42.get("path") : var41);
                           }

                           var26.put(var15, Arrays.asList(var37.get("criterion"), var37.get("requirements"), var39));
                        }
                     }

                     TreeMap var27 = new TreeMap();

                     for (Object var32 : var25.get("obligations") instanceof List<?> values ? values : List.of()) {
                        if (var32 instanceof Map var36) {
                           var27.put(
                              String.valueOf(var36.get("obligation")),
                              Arrays.asList(var36.get("criterion"), var36.get("requirements"), var36.get("content") instanceof List var38 ? var38 : List.of())
                           );
                        }
                     }

                     for (Entry var33 : (Iterable<Entry>) (Iterable<?>) (var26.entrySet())) {
                        if (!Objects.equals(var33.getValue(), var27.get(var33.getKey()))) {
                           throw new RuleApiException(
                              400,
                              "PROOF_INVALID",
                              "the envelope binds "
                                 + (String)var33.getKey()
                                 + " to "
                                 + var33.getValue()
                                 + " but '"
                                 + var7
                                 + "' maps it to "
                                 + var27.get(var33.getKey())
                                 + " — rerun etc/tlc-run.sh"
                           );
                        }
                     }

                     var var30 = io.karatelabs.ext.coverage.collect.ProofArtifactParser.a(this.runsDir(), var2, var3, var25);
                     LinkedHashMap var34 = new LinkedHashMap();
                     var34.put("definition", var30.get("key"));
                     var34.put("completion", var3.get("completion"));
                     var34.put("obligations", io.karatelabs.ext.coverage.collect.ProofArtifactParser.b(this.runsDir().resolve((String)var30.get("runId"))));
                     var34.put("run", var30.get("runId"));
                     return var34;
                  } else {
                     throw new RuleApiException(400, "PROOF_INVALID", "the envelope lists no obligations");
                  }
               } else {
                  throw new RuleApiException(400, "PROOF_INVALID", "'" + var7 + "' names no spec and cfg");
               }
            } else {
               throw new RuleApiException(400, "PROOF_INVALID", "the envelope names no inputs.mapping.path");
            }
         } catch (RuntimeException var21) {
            return JsApi.error(var21);
         }
      }
   }

   private static Map<String, Object> proofJson(Path var0, String var1) {
      Path var2 = io.karatelabs.ext.coverage.collect.ProofArtifactParser.a(var0, var1);
      if (var2 == null) {
         throw new RuleApiException(400, "INVALID_INPUT", "'" + var1 + "' leaves the project");
      } else if (!Files.isRegularFile(var2)) {
         throw new RuleApiException(404, "PROOF_NOT_FOUND", "no file '" + var1 + "' under the project");
      } else {
         try {
            Object var3 = Json.parseStrict(Files.readString(var2, StandardCharsets.UTF_8), true);
            if (!(var3 instanceof Map)) {
               throw new RuleApiException(400, "PROOF_INVALID", "'" + var1 + "' is not a JSON object");
            } else {
               return (Map<String, Object>)var3;
            }
         } catch (IOException var4) {
            throw new UncheckedIOException(var4);
         }
      }
   }

   Map<String, Object> importTable(String var1, Object var2) {
      var var3 = optsMap(var2);
      if (var1 != null && var3 != null && var3.get("mapping") != null && var3.get("source") instanceof String var4) {
         try {
            RuleTableCatalog var16 = this.manager.d(var1).c();
            Path var6 = this.manager.c(var1);
            ImportMapping var7 = io.karatelabs.ext.rules.ImportMapping.a(var6, var3.get("mapping"));
            if (var7.q != null) {
               var7.a(var16.b().get(var7.q));
            }

            io.karatelabs.ext.rules.RulebookImportEngine.ImportedRulebookData var8 = io.karatelabs.ext.rules.RulebookImportEngine.a(var6, var7, var4);
            LinkedHashMap var9 = new LinkedHashMap();
            LinkedHashMap var10 = new LinkedHashMap();
            var10.put("kind", var7.o);
            if (var7.q != null) {
               var10.put("role", var7.p);
               var10.put("table", var7.q);
            } else {
               var10.put("name", var7.r);
            }

            var9.put("target", var10);
            var9.put("mapping", var7.a());
            var9.put("source", var4);
            var9.put("rows", var8.b());
            if (var7.D) {
               var9.put("formulas", Map.of("policy", "cached", "cells", var8.j()));
            }

            Map var11 = var7.q != null ? io.karatelabs.ext.rules.RulebookImportEngine.a(var8, var16.b().get(var7.q)) : Map.of("evidence/ratebooks/" + var7.r + ".json", io.karatelabs.ext.rules.RulebookImportEngine.a(var8));
            Map var12 = var3.get("ifMatch") instanceof Map var13 ? var13 : null;
            var9.put("written", var7.q != null ? io.karatelabs.ext.rules.RulebookImportEngine.a(var6, var7.p, var11, var12) : io.karatelabs.ext.rules.RulebookImportEngine.a(var6, var11));
            return var9;
         } catch (RuntimeException var15) {
            return JsApi.error(var15);
         }
      } else {
         return JsApi.missing(
            "Rule.import(name, {mapping, source, ifMatch?}) requires a rule name, an import mapping (evidence/mappings/<name>.import.json) and a rulebook-relative source file"
         );
      }
   }

   Map<String, Object> changeSet(String var1, Object var2) {
      var var3 = optsMap(var2);
      if (var1 != null
         && var3 != null
         && var3.get("before") instanceof String var4
         && var3.get("after") instanceof String var5
         && var3.get("mapping") != null
         && var3.get("produces") != null) {
         try {
            this.manager.d(var1);
            Path var19 = this.manager.c(var1);
            ImportMapping var7 = io.karatelabs.ext.rules.ImportMapping.a(var19, var3.get("mapping"));
            if (var7.q == null) {
               throw io.karatelabs.ext.rules.ImportMapping.a("a change set diffs a table — target.kind is 'table'");
            } else {
               var var8 = io.karatelabs.ext.rules.ChangeSetGenerator.a(var3.get("produces"));
               io.karatelabs.ext.rules.RulebookImportEngine.ImportedRulebookData var9 = io.karatelabs.ext.rules.RulebookImportEngine.a(var19, var7, var4);
               io.karatelabs.ext.rules.RulebookImportEngine.ImportedRulebookData var10 = io.karatelabs.ext.rules.RulebookImportEngine.a(var19, var7, var5);
               var var11 = io.karatelabs.ext.rules.ChangeSetGenerator.a(var19, var7, io.karatelabs.ext.rules.ChangeSetGenerator.a(var9, var10), var8, Boolean.TRUE.equals(var3.get("algorithm")));
               LinkedHashMap var12 = new LinkedHashMap(var11);
               List<Map<String, Object>> var13 = (List)var11.get("rows");
               LinkedHashMap var14 = new LinkedHashMap();

               for (String var16 : List.of("change", "add", "drop")) {
                  var14.put(var16, var13.stream().filter(var1x -> var16.equals(var1x.get("op"))).count());
               }

               var12.put("counts", var14);
               if (var13.size() > tableRowsMax()) {
                  var12.put("rows", var13.subList(0, tableRowsMax()));
                  var12.put("truncated", true);
               }

               var12.put("accounting", Map.of("before", var9.b(), "after", var10.b()));
               return var12;
            }
         } catch (RuntimeException var17) {
            return JsApi.error(var17);
         }
      } else {
         return JsApi.missing(
            "Rule.changeSet(name, {before, after, mapping, produces: {edition, effective, renewalEffective?, jurisdictions, lines}, algorithm?}) requires both rulebook-relative files, an import mapping and produces"
         );
      }
   }

   Map<String, Object> changeSetCells(String var1, Object var2) {
      return this.changeSetCells(var1, var2, null);
   }

   Map<String, Object> changeSetCells(String var1, Object var2, Object var3) {
      if (var1 != null && var2 != null) {
         try {
            RuleTableCatalog var4 = this.manager.d(var1).c();
            var var5 = io.karatelabs.ext.rules.ChangeSetGenerator.a(this.manager.c(var1), var2);
            Map var6 = var3 == null ? Map.of() : optsMap(var3);
            if (var6 != null && (var6.get("base") == null || var6.get("base") instanceof String)) {
               if (var6.get("base") instanceof String var7) {
                  RuleTableDiffEngine.a(var4, var7);
                  var var10 = io.karatelabs.ext.rules.ChangeSetGenerator.a(io.karatelabs.ext.rules.ChangeSetGenerator.a(var5, RuleTableDiffEngine.a(var4), var7), var4);
                  var10.put("base", var7);
                  return var10;
               } else {
                  return io.karatelabs.ext.rules.ChangeSetGenerator.a(var5, var4);
               }
            } else {
               throw new RuleApiException(400, "INVALID_INPUT", "base is the edition directory under tables/ the change set produces");
            }
         } catch (RuntimeException var9) {
            return JsApi.error(var9);
         }
      } else {
         return JsApi.missing("Rule.changeSetCells(name, changeSet, {base?}) requires a rule name and a change set id (evidence/changes/<id>.json) or object");
      }
   }

   Map<String, Object> editions(String var1, Object var2) {
      if (var1 == null) {
         return JsApi.missing("Rule.editions(name, {records, rule?}) requires a rule name (try Rule.list())");
      } else {
         var var3 = optsMap(var2);
         if (var3 != null && var3.get("records") != null) {
            try {
               io.karatelabs.ext.rules.RulebookRepository.RulebookDocument var4 = this.manager.b(var1);
               Path var5 = this.manager.c(var1);
               RatebookSelectionRule var6 = RatebookSelectionRule.a(var5, var3.get("rule"));
               var var7 = io.karatelabs.ext.rules.RatebookEditionChecker.a(var5, var3.get("records"), var6);
               var var8 = io.karatelabs.ext.rules.RatebookEditionChecker.a(var4, var5, var3.get("records"), var7, var6);
               return envelope(var3) ? io.karatelabs.ext.rules.RatebookEditionChecker.a(var8, var5, var3.get("records"), var6) : var8;
            } catch (RuntimeException var9) {
               return JsApi.error(var9);
            }
         } else {
            return JsApi.missing("Rule.editions(name, {records, rule?}) requires records — a name under evidence/ratebooks/ or the records object");
         }
      }
   }

   Map<String, Object> tableDiff(String var1, Object var2) {
      var var3 = optsMap(var2);
      if (var1 != null && var3 != null && var3.get("against") instanceof String var4) {
         try {
            if (var3.get("base") != null && !(var3.get("base") instanceof String)) {
               throw new RuleApiException(400, "INVALID_INPUT", "base is one directory under tables/, e.g. 'iso26'");
            } else {
               String var17 = (String)var3.get("base");
               io.karatelabs.ext.rules.RulebookRepository.RulebookSnapshot var6 = this.manager.d(var1);
               RuleTableCatalog var7 = var17 == null ? var6.c() : RuleTableDiffEngine.a(var6.c(), var17);
               Path var8 = this.manager.c(var1);
               String var9 = var8.relativize(io.karatelabs.ext.rules.RulebookImportEngine.a(var8, var4, "against")).toString().replace('\\', '/');
               io.karatelabs.ext.rules.RuleTableCatalog.TableSetScope var10 = io.karatelabs.ext.rules.RuleTableCatalog.TableSetScope.b(var9);
               if (!Files.isDirectory(var8.resolve(var9))) {
                  throw new RuleApiException(404, "EVIDENCE_NOT_FOUND", "no evidence table set at '" + var9 + "' under the rulebook");
               } else {
                  ArrayList var11 = new ArrayList();
                  if (var3.get("changeSets") != null) {
                     Object var13 = var3.get("changeSets");
                     if (!(var13 instanceof List)) {
                        throw new RuleApiException(400, "INVALID_INPUT", "changeSets is the ordered list of change-set ids under evidence/changes/");
                     }

                     for (Object var14 : (List)var13) {
                        var11.add(io.karatelabs.ext.rules.ChangeSetGenerator.a(var8, var14));
                     }
                  }

                  var var18 = io.karatelabs.ext.rules.RulebookImportEngine.a(var8, var9);
                  RuleTableCatalog var20 = RuleTableCatalog.a(var18, var10);
                  Map var21 = RuleTableDiffEngine.a(var1, var7, var6.d(), var20, var9, RuleTableDiffEngine.b(var18), var11, var17);
                  Map var15 = RuleTableDiffEngine.a(var21, tableRowsMax());
                  var15.put("ingest", RuleTableDiffEngine.a(this.runsDir(), var15));
                  return envelope(var3) ? RuleTableDiffEngine.a(var21) : var15;
               }
            }
         } catch (RuntimeException var16) {
            return JsApi.error(var16);
         }
      } else {
         return JsApi.missing(
            "Rule.tableDiff(name, {against, changeSets, base?}) requires a rule name, an evidence table set (evidence/under-test or evidence/reference, or a directory under one) and the ordered change-set ids"
         );
      }
   }

   private static boolean envelope(Map<String, Object> var0) {
      return "envelope".equals(var0.get("output"));
   }

   private List<Map<String, Object>> tableSections(String var1, io.karatelabs.ext.rules.RulebookRepository.RulebookDocument var2) {
      ArrayList var3 = new ArrayList();

      for (Map var5 : RuleTableDiffEngine.a(this.runsDir(), var1)) {
         LinkedHashMap var6 = new LinkedHashMap(var5);
         LinkedHashMap var7 = new LinkedHashMap();
         Map var8 = (Map)var5.get("digests");
         var7.put("book", !var2.n().c().equals(((Map)var8.get("book")).get("sha256")));
         String var9 = String.valueOf(var5.get("against"));
         Path var10 = this.manager.c(var1).resolve(var9);
         var7.put("evidence", !Files.isDirectory(var10) || !RuleTableDiffEngine.b(io.karatelabs.ext.rules.RulebookImportEngine.a(this.manager.c(var1), var9)).c().equals(((Map)var8.get("evidence")).get("sha256")));
         var6.put("stale", var7);
         var3.add(var6);
      }

      return var3;
   }

   private List<Map<String, Object>> editionSections(String var1) {
      Path var2 = this.manager.c(var1).resolve("evidence/ratebooks");
      if (!Files.isDirectory(var2)) {
         return List.of();
      } else {
         String var3 = null;

         try {
            RatebookSelectionRule var4 = RatebookSelectionRule.a(this.manager.c(var1));
            var3 = var4 == null ? null : var4.f();
         } catch (RuleApiException var12) {
         }

         ArrayList var15 = new ArrayList();

         try {
            try (var var5 = Files.list(var2)) {
               for (Path var7 : (Iterable<Path>) (Iterable<?>) (var5.sorted().toList())) {
                  String var8 = var7.getFileName().toString();
                  if (var8.endsWith(".json") && !var8.equals("rule.json")) {
                     String var9 = var8.substring(0, var8.length() - ".json".length());
                     if (!var9.equals(var3)) {
                        var var10 = new LinkedHashMap<>(this.editions(var1, Map.of("records", var9)));
                        var10.put("records", var9);
                        var15.add(var10);
                     }
                  }
               }
            }

            return var15;
         } catch (IOException var14) {
            throw new UncheckedIOException(var14);
         }
      }
   }

   private Path runsDir() {
      Path var1 = this.projectRoot != null ? this.projectRoot : this.rulesHome.toAbsolutePath().getParent();
      return var1.resolve("runs");
   }

   private static List<String> linkReqs(Map<String, Object> var0, Map<String, Object> var1) {
      LinkedHashMap var2 = new LinkedHashMap();

      for (Map var4 : (Iterable<Map>) (Iterable<?>) ((List)var0.get("rows"))) {
         var2.put(String.valueOf(var4.get("id")), var4);
      }

      for (Map var9 : (Iterable<Map>) (Iterable<?>) ((List)var1.get("cases"))) {
         Map var5 = var9.get("row") != null && "unknown case".equals(var9.get("class")) ? null : (Map)var2.get(String.valueOf(var9.get("case")));
         if (var5 != null && var5.get("reqs") instanceof List var6 && !var6.isEmpty()) {
            var9.put("reqs", var6);
         }
      }

      return new ArrayList<>(var2.keySet());
   }

   private static Map<String, Object> results(Path var0, Object var1) {
      if (var1 instanceof Map var6) {
         return var6;
      } else if (var1 instanceof String var2) {
         Path var3 = var0.resolve(var2).normalize();
         if (!var3.startsWith(var0.normalize())) {
            throw new RuleApiException(400, "INVALID_INPUT", "results path '" + var2 + "' leaves the rulebook");
         } else if (!Files.isRegularFile(var3)) {
            throw new RuleApiException(404, "RESULTS_NOT_FOUND", "no results file '" + var2 + "' under the rulebook");
         } else {
            try {
               Object var4 = Json.parseStrict(Files.readString(var3, StandardCharsets.UTF_8), true);
               if (!(var4 instanceof Map)) {
                  throw new RuleApiException(400, "RESULTS_INVALID", "'" + var2 + "' is not a results object");
               } else {
                  return (Map<String, Object>)var4;
               }
            } catch (IOException var5) {
               throw new UncheckedIOException(var5);
            }
         }
      } else {
         throw new RuleApiException(400, "INVALID_INPUT", "results is a rulebook-relative file path or the results object");
      }
   }

   Map<String, Object> mutate(String var1, Object var2) {
      if (var1 == null) {
         return JsApi.missing("Rule.mutate(name, {suite?}) requires a rule name (try Rule.list())");
      } else {
         Map var3 = var2 == null ? Map.of() : optsMap(var2);
         Object var4 = var3.get("suite") != null ? var3.get("suite") : var3.get("suites");
         if (var4 == null) {
            Path var18 = this.rulesHome.toAbsolutePath().getParent();
            Path var20 = var3.get("out") instanceof String var21 && !var21.isBlank() ? var18.resolve(var21) : var18.resolve("target/mutation-self/" + var1);

            try {
               var var22 = mutations.runSelfMutation(var18, var1, var20, var3.get("target"));
               if (var22.get("error") == null) {
                  var22.put(
                     "next",
                     "each worklist entry carries a ready-made counterexample row — store it as a scenario with an _expect (or declare the property that forbids it), then re-run Rule.mutate('"
                        + var1
                        + "') to watch selfRunScore climb; pass {suite} to grade a TEST SUITE's assertions instead (a different, never-comparable score)"
                  );
               }

               return var22;
            } catch (RuleApiException var14) {
               return JsApi.error(var14);
            } catch (Exception var15) {
               return JsApi.error(new RuntimeException("Rule.mutate self-run failed: " + var15.getMessage(), var15));
            }
         } else {
            List var5 = var4 instanceof List var6 ? var6.stream().map(String::valueOf).toList() : List.of(String.valueOf(var4));
            Path var19 = this.rulesHome.toAbsolutePath().getParent();
            String var7 = var3.get("spec") instanceof String var8 && !var8.isBlank() ? var8 : "openapi.yaml";

            try {
               String var23 = var3.get("operation") instanceof String var25 && !var25.isBlank() ? var25 : null;
               if (var23 == null) {
                  var var26 = io.karatelabs.ext.openapi.OpenApiOperationCatalog.loadFromPath(var19.resolve(var7).toString())
                     .operations()
                     .stream()
                     .map(io.karatelabs.ext.openapi.OpenApiOperationCatalog.OperationInfo::a)
                     .filter(Objects::nonNull)
                     .toList();
                  if (var26.size() != 1) {
                     return JsApi.missing(
                        "Rule.mutate('"
                           + var1
                           + "', {operation}) — the spec declares "
                           + var26.size()
                           + " operations ("
                           + var26
                           + "); name the one the rulebook backs"
                     );
                  }

                  var23 = (String)var26.get(0);
               }

               Path var27 = var3.get("out") instanceof String var28 && !var28.isBlank() ? var19.resolve(var28) : var19.resolve("target/mutation/" + var1);
               LinkedHashMap var29 = new LinkedHashMap();
               if (var3.get("provenance") instanceof String var30
                  && !var30.isBlank()
                  && Json.parseStrict(Files.readString(var19.resolve(var30))) instanceof Map var13) {
                  var13.forEach((var1x, var2x) -> var29.put(String.valueOf(var1x), String.valueOf(var2x)));
               }

               String var31 = var3.get("handlers") instanceof String var33 && !var33.isBlank() ? var33 : null;
               Map var34 = mutations.runSuiteMutation(var19, var1, var7, var23, var5, var27, var29, var31, var3.get("target"), this.runPrep);
               if (var34.get("error") == null) {
                  var34.put(
                     "next",
                     "strengthen the worklist assertions (frozen literals/shape locks on the computed outputs), then re-run Rule.mutate to watch the independent score climb"
                  );
               }

               return var34;
            } catch (RuleApiException var16) {
               return JsApi.error(var16);
            } catch (Exception var17) {
               return JsApi.error(new RuntimeException("Rule.mutate failed: " + var17.getMessage(), var17));
            }
         }
      }
   }

   Map<String, Object> report(String var1, Object var2) {
      if (var1 == null) {
         return JsApi.missing("Rule.report(name) requires a rule name (try Rule.list())");
      } else {
         try {
            Object var3 = optsMap(var2);
            if (this.reportDir != null && (var3 == null || ((java.util.Map)var3).get("out") == null)) {
               var3 = var3 == null ? new LinkedHashMap() : new LinkedHashMap((Map)var3);
               ((java.util.Map)var3).put("out", this.reportDir.toString() + "/");
            }

            Object var4 = this.twin().plan(var1, var3 == null ? null : ((java.util.Map)var3).get("plan"), this.rulesHome.toAbsolutePath().getParent());
            io.karatelabs.ext.rules.RulebookRepository.RulebookDocument var5 = this.manager.b(var1);
            return RuleAnalysisReportGenerator.a(
               var5,
               this.runner,
               (Map<String, Object>)var3,
               this.twin(),
               this.mutationSelfReport(var1, (Map<String, Object>)var3),
               var4,
               io.karatelabs.ext.coverage.collect.MatchResultArtifactParser.a(this.runsDir(), var1),
               this.editionSections(var1),
               this.tableSections(var1, var5)
            );
         } catch (RuntimeException var6) {
            return JsApi.error(var6);
         }
      }
   }

   private Map<String, Object> mutationSelfReport(String var1, Map<String, Object> var2) {
      Path var3 = this.rulesHome.toAbsolutePath().getParent();
      Path var5 = (var2 == null ? null : var2.get("mutation")) instanceof String var6 && !var6.isBlank()
         ? var3.resolve(var6)
         : var3.resolve("target/mutation-self/" + var1 + "/mutation-self-report.json");

      try {
         return Files.exists(var5) && Json.parseStrict(Files.readString(var5)) instanceof Map var9 ? castMap(var9) : null;
      } catch (Exception var8) {
         return null;
      }
   }

   private static Map<String, Object> castMap(Map<?, ?> var0) {
      return (Map<String, Object>)(Map<?, ?>)var0;
   }

   Map<String, Object> bless(String var1) {
      if (var1 == null) {
         return JsApi.missing("Rule.bless(name) requires a rule name (try Rule.list())");
      } else {
         try {
            return RuleVerificationService.a(this.rulesHome, this.manager, this.runner, var1);
         } catch (RuntimeException var3) {
            return JsApi.error(var3);
         }
      }
   }

   Map<String, Object> drift(String var1) {
      if (var1 == null) {
         return JsApi.missing("Rule.drift(name) requires a rule name (try Rule.list())");
      } else {
         try {
            return RuleVerificationService.b(this.rulesHome, this.manager, this.runner, var1);
         } catch (RuntimeException var3) {
            return JsApi.error(var3);
         }
      }
   }

   private static Map<String, Object> optsMap(Object var0) {
      return var0 instanceof Map ? (Map)var0 : null;
   }

   private static List<String> dimPaths(Map<String, Object> var0) {
      if (var0.get("dimensions") instanceof List var1) {
         ArrayList var6 = new ArrayList();

         for (Object var4 : var1) {
            if (var4 instanceof Map var5 && !"array".equals(var5.get("type"))) {
               var6.add(String.valueOf(var5.get("path")));
            }
         }

         return var6;
      } else {
         return null;
      }
   }

   private static Map<Integer, String> labelsByLine(Map<String, Object> var0) {
      LinkedHashMap var1 = new LinkedHashMap();
      if (var0.get("labels") instanceof Map var2) {
         for (Entry var4 : (Iterable<Entry>) (Iterable<?>) (var2.entrySet())) {
            try {
               var1.put(Integer.parseInt(String.valueOf(var4.getKey())), String.valueOf(var4.getValue()));
            } catch (NumberFormatException var6) {
            }
         }
      }

      return var1;
   }

   private static String suiteStrategy(Object var0) {
      Object var1 = var0 instanceof Map var2 ? var2.get("suite") : null;
      if (var1 == null) {
         return "coverage";
      } else if (!SUITES.contains(var1)) {
         throw new RuleApiException(400, "SUITE_UNKNOWN", "no suite '" + var1 + "' — the suites are " + SUITES);
      } else {
         return (String)var1;
      }
   }

   private io.karatelabs.ext.rules.RuleInputSpaceExplorer.InputSpaceExplorationResult explore(io.karatelabs.ext.rules.RulebookRepository.RulebookDocument var1, String var2, Long var3, int var4, int var5, io.karatelabs.ext.rules.RuleInputSpaceCertificateBuilder.InputSpaceCertificateResult var6, CellSensitivityPlanner var7) {
      List var8 = var1.b() == null ? List.of() : var1.b();
      return RuleInputSpaceExplorer.a(var1.g(), var1.m().e(), var1.e(), var1.j(), var8, var2, var3, var4, var5, var6, var7);
   }

   private CellSensitivityPlanner cellSuite(io.karatelabs.ext.rules.RulebookRepository.RulebookDocument var1, String var2, Object var3) {
      return "cells".equals(var2) ? new CellSensitivityPlanner(var1, this.runner, ((Map)var3).get("target"), maxSearchOr(var3)) : null;
   }

   private static int maxSearchOr(Object var0) {
      Integer var1 = maxSearchOf(var0);
      return var1 != null ? var1 : 512;
   }

   private void applyDiff(Map<String, Object> var1, String var2, String var3, Map<String, String> var4, io.karatelabs.ext.rules.RuleEvaluator.RuleEvaluationResult var5) {
      Path var6 = RuleBaselineStore.a(this.rulesHome, var2);
      RuleBaselineSnapshot var7 = RuleBaselineStore.a(var6);
      io.karatelabs.ext.rules.RuleTreeDiffer.RuleTreeDiffResult var8 = RuleTreeDiffer.a(var5.a(), var7);
      var1.put("audit", RuleTreeJsonMapper.b(var8.a()));
      LinkedHashMap var9 = new LinkedHashMap();
      var9.put("firstRun", var7 == null);
      if (var7 == null) {
         var9.put(
            "note",
            "baseline captured — nothing to compare against yet. Run the scenario again with {diff:true} (before and after the change you want to see): the baseline advances only on a {diff:true} run, so an unflagged run in between is invisible here."
         );
         var1.put("diff", var9);
         RuleBaselineStore.a(var6, var5.a(), var5.b(), var5.d(), var4, var3);
      } else {
         var9.put("baselineTs", var7.c());
         if (var7.e() != null) {
            var9.put("prevScenario", var7.e());
         }

         int[] var10 = new int[4];
         countStatuses(var8.a(), var10);
         var9.put("unchanged", var10[0]);
         var9.put("added", var10[1]);
         var9.put("removed", var10[2]);
         var9.put("changed", var10[3]);
         var1.put("diff", var9);
         RuleBaselineStore.a(var6, var5.a(), var5.b(), var5.d(), var4, var3);
      }
   }

   private static void countStatuses(List<ResultTreeNode> var0, int[] var1) {
      for (ResultTreeNode var3 : var0) {
         String var4 = var3.h() == null ? "" : var3.h();
         switch (var4) {
            case "unchanged":
               var1[0]++;
               break;
            case "added":
               var1[1]++;
               break;
            case "removed":
               var1[2]++;
               break;
            case "changed":
               var1[3]++;
         }

         countStatuses(var3.k(), var1);
      }
   }

   private static boolean optFlag(Object var0, String var1) {
      return var0 instanceof Map var2 && Boolean.TRUE.equals(var2.get(var1));
   }

   private static RuleApiException rejectRowRefusal(String var0, String var1) {
      return new RuleApiException(
         400,
         "SCENARIO_IS_REJECT",
         "scenario '"
            + var1
            + "' is a reject row (_expect: 'schema-reject') — it claims that schema.js must REFUSE it, so calc.js never runs it; read its verdict in Rule.check('"
            + var0
            + "').rejects"
      );
   }

   Map<String, Object> audit(String var1, Object var2, Object var3) {
      if (var1 == null) {
         return JsApi.missing("Rule.audit(name, rowOrScenarioId) requires a rule name");
      } else {
         try {
            io.karatelabs.ext.rules.RulebookRepository.RulebookDocument var4 = this.manager.b(var1);
            String var6 = null;
            Map var5;
            if (var2 instanceof String var8) {
               var6 = var8;
               var var9 = RulebookRepository.a(var4, var8);
               if (RulebookRepository.a(var9)) {
                  throw rejectRowRefusal(var1, var8);
               }

               var5 = var9;
            } else {
               if (!(var2 instanceof Map var7)) {
                  return JsApi.missing("Rule.audit(name, rowOrScenarioId) requires a scenario id or a row object");
               }

               var5 = var7;
            }

            TreeSet var12 = var4.m().a() ? null : new TreeSet();
            io.karatelabs.ext.rules.RuleEvaluator.RuleEvaluationResult var13 = this.runner.a(var4.g(), var12 == null ? var4.m().e() : var4.m().a(var12::add), var5, null);
            var var10 = resultMap(var13, var6, var5);
            if (var12 != null) {
               var10.put("cells", new ArrayList(var12));
            }

            return JsApi.applyOutputConvention(var10, io.karatelabs.ext.rules.ResultTreeMarkdownRenderer.a(var13.a()), var3);
         } catch (RuntimeException var11) {
            return JsApi.error(var11);
         }
      }
   }

   Map<String, Object> diagram(String var1, Object var2) {
      if (var1 == null) {
         return JsApi.missing("Rule.diagram(name) requires a rule name");
      } else {
         try {
            RuleFlowGraph var3 = io.karatelabs.ext.rules.RuleFlowGraph.a(this.manager.b(var1).g());
            LinkedHashMap var4 = new LinkedHashMap();
            var var5 = expandSet(var2);
            var4.put("svg", io.karatelabs.ext.rules.RuleFlowSvgRenderer.a(var3, var5, compactOpt(var2)));
            var4.put("mermaid", var3.g());
            var var6 = var3.f();
            var4.put("outline", var6);
            var var7 = var3.e();
            List var8 = var5.contains("all") ? List.of() : var7.stream().filter(var1x -> !var5.contains(var1x)).toList();
            var4.put("collapsed", var8);
            var4.put("collapsibleAll", var7);
            if (var6.isEmpty()) {
               JsApi.hint(
                  var4,
                  "the outline is empty — it lists calc.log('# Section') markers, loops, and large decisions; add `calc.log('# …')` section markers to calc.js (they also render as section breaks in the diagram)"
               );
            }

            return var4;
         } catch (RuntimeException var9) {
            return JsApi.error(var9);
         }
      }
   }

   private static boolean compactOpt(Object var0) {
      if (!(var0 instanceof Map var1)) {
         return true;
      } else {
         Object var2 = var1.get("compact");
         return var2 == null || !Boolean.FALSE.equals(var2) && !"false".equals(var2);
      }
   }

   private static Set<String> expandSet(Object var0) {
      if (var0 instanceof Map var1) {
         Object var2 = var1.get("expand");
         if ("all".equals(var2)) {
            return Set.of("all");
         } else if (var2 instanceof List var3) {
            LinkedHashSet var4 = new LinkedHashSet();

            for (Object var6 : var3) {
               if (var6 instanceof String var7) {
                  var4.add(var7);
               }
            }

            return var4;
         } else {
            return Set.of();
         }
      } else {
         return Set.of();
      }
   }

   Map<String, Object> scenarioCreate(String var1, Object var2, Object var3) {
      if (var1 == null) {
         return JsApi.missing("Rule.scenario.create(rule, row) requires a rule name");
      } else if (var2 instanceof List) {
         ArrayList var9 = new ArrayList();

         for (Object var6 : (List)var2) {
            if (!(var6 instanceof Map)) {
               return JsApi.missing("Rule.scenario.create(rule, [rows]) requires every element to be a row object");
            }

            var9.add((Map)var6);
         }

         try {
            return this.manager.a(var1, var9, ifMatch(var3));
         } catch (RuntimeException var7) {
            return JsApi.error(var7);
         }
      } else if (!(var2 instanceof Map var4)) {
         return JsApi.missing("Rule.scenario.create(rule, row) requires a row object, or an array of them");
      } else {
         try {
            return this.manager.c(var1, var4, ifMatch(var3));
         } catch (RuntimeException var8) {
            return JsApi.error(var8);
         }
      }
   }

   Map<String, Object> scenarioUpdate(String var1, String var2, Object var3, Object var4) {
      if (var1 == null || var2 == null) {
         return JsApi.missing("Rule.scenario.update(rule, id, patch) requires a rule name and scenario id");
      } else if (!(var3 instanceof Map var5)) {
         return JsApi.missing("Rule.scenario.update(rule, id, patch) requires a patch object");
      } else {
         try {
            return this.manager.a(var1, var2, var5, ifMatch(var4));
         } catch (RuntimeException var6) {
            return JsApi.error(var6);
         }
      }
   }

   Map<String, Object> scenarioDelete(String var1, String var2, Object var3) {
      if (var1 != null && var2 != null) {
         try {
            return this.manager.d(var1, var2, ifMatch(var3));
         } catch (RuntimeException var5) {
            return JsApi.error(var5);
         }
      } else {
         return JsApi.missing("Rule.scenario.delete(rule, id) requires a rule name and scenario id");
      }
   }

   Map<String, Object> project(String var1, Object var2) {
      if (var1 == null) {
         return JsApi.missing("Rule.project(rule, {out?}) requires a rule name (try Rule.list())");
      } else {
         Map var3 = var2 instanceof Map var4 ? var4 : Map.of();
         Object var8 = var3.get("out");
         if (var8 != null && !(var8 instanceof String) && !(var8 instanceof Boolean)) {
            return JsApi.missing("Rule.project(rule, {out}) takes a path or false, got " + var8);
         } else if (var3.get("against") != null) {
            return this.projectAgainst(var1, var3);
         } else {
            try {
               io.karatelabs.ext.rules.twin.OpenApiProjector.OpenApiProjection var5 = this.projection(var1);
               LinkedHashMap var6 = new LinkedHashMap();
               var6.put("ok", var5.a());
               if (var5.a()) {
                  var6.put("spec", var5.b());
                  var6.put("digest", var5.c());
                  var6.put("out", write(this.manager.c(var1), var8, var5.d()));
               } else {
                  var6.put("out", null);
               }

               var6.put("notprojected", var5.e());
               var6.put("refusals", var5.f());
               return var6;
            } catch (RuntimeException var7) {
               return JsApi.error(var7);
            }
         }
      }
   }

   private io.karatelabs.ext.rules.twin.OpenApiProjector.OpenApiProjection projection(String var1) {
      io.karatelabs.ext.rules.RulebookRepository.RulebookDocument var2 = this.manager.b(var1);
      if (var2.k() == null) {
         throw new RuleApiException(404, "TWIN_NOT_FOUND", "rulebook '" + var1 + "' has no twin.js — author one with Rule.twin.update(name, source)");
      } else {
         return io.karatelabs.ext.rules.twin.OpenApiProjector.a(io.karatelabs.ext.rules.twin.TwinModelLoader.a(var2.k()), var1);
      }
   }

   private Map<String, Object> projectAgainst(String var1, Map<?, ?> var2) {
      Main.requireProduct("openapi.diff", "openapi");
      String var3 = var2.get("failOn") == null ? null : String.valueOf(var2.get("failOn"));
      var var4 = this.diffAgainst(var1, var2, var3);
      if (var4.get("error") != null) {
         if (var3 != null) {
            var4.put("gate", io.karatelabs.ext.diff.DiffReport.b(var3));
         }

         return var4;
      } else {
         if (var3 != null) {
            var4.put("gate", io.karatelabs.ext.diff.DiffReport.a(var3, var4));
         }

         if ("changelog".equals(var2.get("output"))) {
            return io.karatelabs.ext.openapi.changes.ApiChangelog.a(var4);
         } else {
            var var5 = JsApi.applyOutputConvention(var4, () -> io.karatelabs.ext.diff.DiffReport.b("Rule.project", var4), var2);
            if (var3 != null) {
               var5.putIfAbsent("gate", var4.get("gate"));
            }

            return var5;
         }
      }
   }

   private Map<String, Object> diffAgainst(String var1, Map<?, ?> var2, String var3) {
      for (Object var5 : var2.keySet()) {
         if (!PROJECT_AGAINST.contains(String.valueOf(var5))) {
            return JsApi.missing(
               "'"
                  + var5
                  + "' is not an option beside against — Rule.project(rule, {against: 'api/claims.yaml', output?, failOn?, waivers?, asOf?}) — diffs the projection against a project spec and writes nothing"
            );
         }
      }

      if (!(var2.get("against") instanceof String var20 && !var20.isBlank())) {
         return JsApi.missing(
            "Rule.project(rule, {against: 'api/claims.yaml', output?, failOn?, waivers?, asOf?}) — diffs the projection against a project spec and writes nothing"
         );
      } else if (var3 != null && !io.karatelabs.ext.diff.DiffReport.b.contains(var3)) {
         return JsApi.missing("failOn is 'breaking', 'warning' or 'compatible', not '" + var3 + "'");
      } else {
         Path var22 = this.projectRoot != null ? this.projectRoot : this.rulesHome.toAbsolutePath().getParent();

         try {
            LinkedHashMap var6 = new LinkedHashMap();
            var6.put("asOf", var2.get("asOf"));
            io.karatelabs.ext.openapi.changes.OpenApiDiffer.OpenApiDiffOptions var7 = io.karatelabs.ext.openapi.changes.OpenApiDiffer.OpenApiDiffOptions.a(var6);
            LocalDate var8 = var7.a() != null ? var7.a() : LocalDate.now(ZoneOffset.UTC);
            io.karatelabs.ext.lint.WaiverLedger var9 = projectWaivers(var2.get("waivers"), var22, var8);
            io.karatelabs.ext.rules.twin.OpenApiProjector.OpenApiProjection var10 = this.projection(var1);
            if (!var10.a()) {
               var var23 = JsApi.missing("the projection refuses, so there is nothing to diff — fix its refusals first (Rule.project('" + var1 + "'))");
               var23.put("refusals", var10.f());
               var23.put("notprojected", var10.e());
               return var23;
            } else {
               byte[] var11;
               try (InputStream var12 = MaxPaths.resolveResource(var22, var20).getStream()) {
                  var11 = var12.readAllBytes();
               } catch (RuntimeException | IOException var17) {
                  return JsApi.missing("Rule.project could not read against '" + var20 + "': " + var17.getMessage());
               }

               var var24 = io.karatelabs.ext.openapi.changes.OpenApiDiffer.a(var20, var11, "Rule.project('" + var1 + "')", var10.d().getBytes(StandardCharsets.UTF_8), var7);
               return io.karatelabs.ext.rules.twin.ProjectionDiffBuilder.a(var24, var10, var1, var9, var8);
            }
         } catch (IllegalArgumentException var18) {
            return JsApi.missing("Rule.project: " + var18.getMessage());
         } catch (RuntimeException var19) {
            return JsApi.error(var19);
         }
      }
   }

   private static io.karatelabs.ext.lint.WaiverLedger projectWaivers(Object var0, Path var1, LocalDate var2) {
      String var3 = null;

      try {
         if (var0 instanceof String var4) {
            var3 = var4;
            var0 = io.karatelabs.ext.sourcedoc.SourceDocument.parse(var4, Files.readString(var1.resolve(var4))).value();
            var0 = var0 == null ? List.of() : var0;
         }

         return var0 == null ? null : io.karatelabs.ext.rules.twin.ProjectionDiffBuilder.a(var0, var3, var2);
      } catch (RuntimeException | IOException var5) {
         throw new RuleApiException(400, "WAIVERS_MALFORMED", "waivers (" + (var3 == null ? "inline" : var3) + ") could not be read: " + var5.getMessage());
      }
   }

   private static String write(Path var0, Object var1, String var2) {
      if (Boolean.FALSE.equals(var1)) {
         return null;
      } else {
         Path var3 = var1 instanceof String var4 ? var0.resolve(var4) : var0.resolve("openapi.yaml");

         try {
            Files.createDirectories(var3.getParent());
            Files.writeString(var3, var2, StandardCharsets.UTF_8);
         } catch (IOException var5) {
            throw new UncheckedIOException(var5);
         }

         return var3.toString();
      }
   }

   Map<String, Object> twinUpdate(String var1, Object var2, Object var3) {
      if (var1 == null || var2 == null) {
         return JsApi.missing("Rule.twin.update(rule, twinJs) requires a rule name and the twin.js text");
      } else if (var2 instanceof String var4) {
         try {
            return Map.of("mtime", this.manager.a(var1, var4, ifMatch(var3), var2x -> this.vetTwinReqs(var1, var2x)));
         } catch (RuntimeException var6) {
            return JsApi.error(var6);
         }
      } else {
         return notSource("Rule.twin.update", "twinJs", "\"t.init(w => { … })\"");
      }
   }

   private void vetTwinReqs(String var1, io.karatelabs.ext.rules.twin.TwinModel var2) {
      if (!this.requirementsHomes.isEmpty() && !var2.c().stream().allMatch(var0 -> var0.j() == null)) {
         io.karatelabs.ext.rules.RulebookCoverageProvider.CoverageRequirementIds var3 = io.karatelabs.ext.rules.RulebookCoverageProvider.CoverageRequirementIds.a(new io.karatelabs.ext.coverage.collect.RequirementsCoverageSource(this.requirementsHomes, null, null));

         for (io.karatelabs.ext.rules.twin.TwinModel.CommandModel var5 : var2.c()) {
            String var6 = var5.j();
            if (var6 != null && !var3.a().contains(var6)) {
               boolean var7 = var3.b().contains(var6);
               LinkedHashMap var8 = new LinkedHashMap();
               var8.put("rule", var1);
               var8.put("command", var5.a());
               var8.put("req", var6);
               throw new RuleApiException(
                     400,
                     var7 ? "REQ_NOTCRITERION" : "REQ_NOTFOUND",
                     "command '" + var5.a() + "' in rulebook '" + var1 + "' links '" + RulebookCoverageProvider.a(var6, var7, var3.a())
                  )
                  .a(var8);
            }
         }
      }
   }

   Map<String, Object> sequenceCreate(String var1, Object var2, Object var3) {
      if (var1 == null) {
         return JsApi.missing("Rule.sequence.create(rule, row) requires a rule name");
      } else {
         ArrayList var4 = new ArrayList();
         if (var2 instanceof List) {
            for (Object var6 : (List)var2) {
               if (!(var6 instanceof Map)) {
                  return JsApi.missing("Rule.sequence.create(rule, [rows]) requires every element to be a row object");
               }

               var4.add((Map)var6);
            }
         } else {
            if (!(var2 instanceof Map)) {
               return JsApi.missing("Rule.sequence.create(rule, row) requires a row object, or an array of them");
            }

            var4.add((Map)var2);
         }

         try {
            return this.manager.b(var1, var4, ifMatch(var3));
         } catch (RuntimeException var7) {
            return JsApi.error(var7);
         }
      }
   }

   Map<String, Object> sequenceUpdate(String var1, String var2, Object var3, Object var4) {
      if (var1 == null || var2 == null) {
         return JsApi.missing("Rule.sequence.update(rule, id, patch) requires a rule name and sequence id");
      } else if (!(var3 instanceof Map var5)) {
         return JsApi.missing("Rule.sequence.update(rule, id, patch) requires a patch object");
      } else {
         try {
            return this.manager.b(var1, var2, var5, ifMatch(var4));
         } catch (RuntimeException var6) {
            return JsApi.error(var6);
         }
      }
   }

   Map<String, Object> sequenceDelete(String var1, String var2, Object var3) {
      if (var1 != null && var2 != null) {
         try {
            return this.manager.f(var1, var2, ifMatch(var3));
         } catch (RuntimeException var5) {
            return JsApi.error(var5);
         }
      } else {
         return JsApi.missing("Rule.sequence.delete(rule, id) requires a rule name and sequence id");
      }
   }

   Map<String, Object> calcUpdate(String var1, Object var2, Object var3) {
      if (var1 == null || var2 == null) {
         return JsApi.missing("Rule.calc.update(rule, calcJs) requires a rule name and the calc.js text");
      } else if (var2 instanceof String var4) {
         try {
            return Map.of("mtime", this.manager.a(var1, var4, ifMatch(var3)));
         } catch (RuntimeException var6) {
            return JsApi.error(var6);
         }
      } else {
         return notSource("Rule.calc.update", "calcJs", "\"const execute = function(calc) { … }\"");
      }
   }

   Map<String, Object> schemaUpdate(String var1, Object var2, Object var3) {
      if (var1 == null || var2 == null) {
         return JsApi.missing("Rule.schema.update(rule, schemaJs) requires a rule name and the schema.js text");
      } else if (var2 instanceof String var4) {
         try {
            return Map.of("mtime", this.manager.b(var1, var4, ifMatch(var3)));
         } catch (RuntimeException var6) {
            return JsApi.error(var6);
         }
      } else {
         return notSource("Rule.schema.update", "schemaJs", "\"schema = { qty: '#number' }\"");
      }
   }

   Map<String, Object> generatorUpdate(String var1, Object var2, Object var3) {
      if (var1 == null || var2 == null) {
         return JsApi.missing("Rule.generator.update(rule, generatorJs) requires a rule name and the generator.js text");
      } else if (var2 instanceof String var4) {
         try {
            return Map.of("mtime", this.manager.c(var1, var4, ifMatch(var3)));
         } catch (RuntimeException var6) {
            return JsApi.error(var6);
         }
      } else {
         return notSource("Rule.generator.update", "generatorJs", "\"function generate(g) { … }\"");
      }
   }

   Map<String, Object> create(String var1, Object var2) {
      if (var1 == null) {
         return JsApi.missing("Rule.create(name, {schema, calc, generator, scenarios}) requires a rule name");
      } else if (!(var2 instanceof Map var3)) {
         return JsApi.missing("Rule.create(name, {schema, calc, generator, scenarios}) requires a files object with at least schema and calc");
      } else {
         for (String[] var7 : new String[][]{
            {"schema", "\"schema = { qty: '#number' }\""},
            {"calc", "\"const execute = function(calc) { … }\""},
            {"generator", "\"function generate(g) { … }\""}
         }) {
            Object var8 = var3.get(var7[0]);
            if (var8 != null && !(var8 instanceof String)) {
               return notSource("Rule.create", var7[0], var7[0] + ": " + var7[1]);
            }
         }

         try {
            return this.manager
               .a(var1, (String)var3.get("schema"), (String)var3.get("calc"), (String)var3.get("generator"), scenariosText(var3.get("scenarios")));
         } catch (RuntimeException var9) {
            return JsApi.error(var9);
         }
      }
   }

   Map<String, Object> delete(String var1) {
      if (var1 == null) {
         return JsApi.missing("Rule.delete(name) requires a rule name (try Rule.list())");
      } else {
         try {
            return this.manager.e(var1);
         } catch (RuntimeException var3) {
            return JsApi.error(var3);
         }
      }
   }

   private static Map<String, Object> notSource(String var0, String var1, String var2) {
      return JsApi.missing(var0 + ": " + var1 + " is the file's source as a string, e.g. " + var2);
   }

   private static String scenariosText(Object var0) {
      if (var0 == null) {
         return null;
      } else {
         return var0 instanceof String var1 ? var1 : Json.stringifyStrict(var0);
      }
   }

   Map<String, Object> lookupsUpdate(String var1, Object var2, Object var3) {
      if (var1 == null) {
         return JsApi.missing("Rule.lookups.update(rule, lookups) requires a rule name");
      } else if (!(var2 instanceof Map var4)) {
         return JsApi.missing("Rule.lookups.update(rule, lookups) requires a lookups object (see Rule.load(...).lookups)");
      } else {
         try {
            return this.manager.b(var1, var4, ifMatch(var3));
         } catch (RuntimeException var5) {
            return JsApi.error(var5);
         }
      }
   }

   private static String ifMatch(Object var0) {
      return var0 instanceof Map var2 && var2.get("ifMatch") instanceof String var1 ? var1 : null;
   }

   private static Map<String, Object> resultMap(io.karatelabs.ext.rules.RuleEvaluator.RuleEvaluationResult var0, String var1, Object var2) {
      LinkedHashMap var3 = new LinkedHashMap();
      if (var1 != null) {
         var3.put("scenarioId", var1);
      }

      if (var2 instanceof Map var4) {
         var3.put("input", RulebookRepository.d(var4));
      }

      var3.put("output", var0.b());
      if (var0.g()) {
         var3.put("outcome", var0.f());
      }

      var3.put("audit", RuleTreeJsonMapper.b(var0.a()));
      var3.put("durationMs", var0.c());
      var3.put("truncated", var0.e());
      if (!var0.h().isEmpty()) {
         var3.put("reqs", new ArrayList<>(var0.h()));
      }

      return var3;
   }

   public interface StepListener {
      void step(String var1, String var2, String var3);
   }
}
