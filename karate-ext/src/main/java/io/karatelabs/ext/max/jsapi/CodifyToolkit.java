package io.karatelabs.ext.max.jsapi;

import io.karatelabs.common.Json;
import io.karatelabs.ext.agent.runtime.AgentRunState;
import io.karatelabs.ext.agent.runtime.AgentContext;
import io.karatelabs.ext.agent.runtime.AgentHook;
import io.karatelabs.ext.agent.runtime.AgentOutcome;
import io.karatelabs.ext.agent.runtime.ToolResult;
import io.karatelabs.ext.agent.runtime.AgentExtension;
import io.karatelabs.ext.rules.RulebookRepository;
import io.karatelabs.ext.rules.Rule;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.Map.Entry;
import java.util.regex.Pattern;

public final class CodifyToolkit implements AgentExtension {
   private static final List<String> MUTATORS = List.of(
      "Rule.create(",
      ".calc.update(",
      ".schema.update(",
      ".generator.update(",
      ".lookups.update(",
      ".scenario.create(",
      ".scenario.update(",
      ".scenario.delete("
   );
   private final Path rulesHome;
   private final String book;
   private final CodifyToolkit.Requires requires;
   private Rule verifier;
   private AgentContext ctx;
   private Set<String> snapshotProperties = Set.of();
   private int snapshotScenarios;
   private Map<String, Object> snapshotPinned = Map.of();
   private Map<String, String> lastDigests;
   private volatile String lastShortfall;
   private final Map<String, io.karatelabs.ext.max.jsapi.CodifyToolkit.RecordedContradiction> ledger = new LinkedHashMap<>();
   private static final Pattern FILE_WRITE = Pattern.compile("File\\.write\\s*\\(\\s*['\"][^'\"]*rulebooks/");

   public CodifyToolkit(Path var1, String var2, CodifyToolkit.Requires var3) {
      this.rulesHome = var1;
      this.book = var2;
      this.requires = var3 == null ? CodifyToolkit.Requires.defaults() : var3;
   }

   public static String preamble() {
      return "You are a rules engineer. Your job is to turn a stated business requirement into a VERIFIED rulebook, using the deterministic verbs available to you — you have no browser and no human to ask.\n\nRespond with exactly ONE ```js block, then STOP — the loop runs it and shows you the result before your next turn. Only the first block executes, and its last expression is the result, returned uncut (console output is capped). Plan in a leading // comment, not in prose. Every `var`/function you define persists across turns — save what you learn as variables, because earlier conversation turns get trimmed but your engine state does not.";
   }

   @Override
   public void init(AgentContext var1) {
      this.ctx = var1;
      this.verifier = Rule.atRulesHome(this.rulesHome);
      var var2 = this.verdict();
      this.snapshotProperties = propertyNames(var2);
      this.snapshotScenarios = this.scenarioCount();
      this.snapshotPinned = this.pinnedNow();
   }

   @Override
   public String promptFragment() {
      return "## Codifying a verified rulebook — your task\n\nThe deliverable is the rulebook **`"
         + this.book
         + "`**. Work in small verified steps, never one big write: settle the input SHAPE, then a few saved scenarios, then the calculation, then READ the verdicts back. After every change, re-read `Rule.check('"
         + this.book
         + "')` — a change you have not re-verified tells you nothing.\n\n**Input the shape must refuse gets its own saved row**, carrying `_expect: 'schema-reject'`; `Rule.check('"
         + this.book
         + "').rejects` grades each one `REFUSED` or `ACCEPTED`. A gate nothing tests refusal against is an assumption.\n\n**Read a verdict honestly — this is the discipline being measured.**\n- A property that reads `HOLDS` was checked over a SAMPLED deck: that is evidence, not proof. Only `COMPLETE` (a closed, fully-walked universe) is proof — and `Rule.check`'s `universe` block tells you which you have, naming every reason it was downgraded. Never write \"verified\"/\"proven\" off a sampled `HOLDS`.\n- A reject roster proves only that THOSE rows are refused; `accepts` beside it says whether any saved row shows the shape ADMITS valid input. `rejects` green with `accepts` reading NOTCHECKED is a shape that refuses everything — no evidence.\n- `notreachable` means the RULES are wrong; `notused` means the DATA is thin. Different fixes — do not paper over the first by adding scenarios. Each finding carries the input `axis` it turns on, or `attribution:'notderivable'` when the comparison reached no input field. `Rule.ranges(book)` then gives that axis its equivalence classes and boundaries. Rewriting the branch without reading either is the expensive way round.\n- `check.verdict` rolls those axes up for you, but it is a HEALTH read, not this run's bar: `CLEAN` means nothing on those axes is outstanding, never that anything is proven, and it is not the check that ends the run. Keep working the requirement.\n\n**Never make a verdict green by removing what it checks.** Deleting or weakening a `calc.always` guarantee, dropping saved scenarios, or narrowing a declared input domain to dodge a counterexample is a promise you stopped checking, not one you kept. It will not end this run: the obligations this book already declared were recorded before you started, and they must still be there and still hold.\n\n**A guarantee states the requirement's own promise — no wider, and no narrower.** An extra `||` written while authoring, for a case you foresee breaking, can never read VIOLATED: it hides the contradiction, which is the weakening above one step earlier. A predicate that forbids MORE than the requirement said fails the other way — it reports a contradiction that is not there, and a terminal blocked exit is the cheapest ending to reach by accident. Before you report a counterexample, re-read the requirement's own wording against it and say which words it breaks.\n\n**If a stated guarantee is genuinely FALSE of the rules, that is a real finding, not a failure.** Keep the guarantee, keep the counterexample the engine handed you, and say which requirement, which input, what it produces versus what was promised. Do not thrash against one and never weaken it to escape: finish everything else and leave both standing"
         + (
            this.requires.mayBlock()
               ? " — a run whose only remaining shortfall is a proven contradiction ends on it, carrying the proof.\n\n"
               : ", and state the contradiction in your answer.\n\n"
         )
         + "**Your exit is a pre-registered check you cannot see, and it reads the rulebook on disk — not your prose and not any value you return.** So build the rulebook the requirement actually describes; there is no shortcut through the answer.\n";
   }

   @Override
   public String closingInstruction(AgentOutcome var1) {
      boolean var2 = var1.kind() == io.karatelabs.ext.agent.runtime.AgentOutcome.Kind.BLOCKED;
      return !var2 && var1.kind() != io.karatelabs.ext.agent.runtime.AgentOutcome.Kind.SUCCESS
         ? null
         : "This run is OVER. '"
            + this.book
            + "' has been checked, the verdict is recorded, and nothing you do now can change it — ignore every earlier reminder to re-read or fix it. Do NOT call anything: re-reading the verdict adds nothing, and the run ends with this reply either way.\n\nOne last turn, for the record. Reply with ONE block whose only statement is a STRING LITERAL — your account of what you established and what you did NOT:\n```js\n'<your statement>'\n```\n"
            + (
               var2
                  ? "Start with the guarantee the rules contradict, the requirement it comes from, and the input that breaks it."
                  : "Name what is proven (COMPLETE) versus what merely held over a sampled deck, and say which axes kept the book off proof."
            );
   }

   @Override
   public String finishInstruction() {
      return "Nothing you return ends this run — the rulebook on disk clears a check you cannot see. So spend what is left on it: read `Rule.check('"
         + this.book
         + "')`, fix the highest thing it reports, re-read. A closing summary buys nothing.";
   }

   @Override
   public String finishInstruction(int var1) {
      return var1 > 1
         ? this.finishInstruction()
         : "There is no turn after this one, and the only thing that survives it is your account. So END this block with a string literal: what you established and what you did NOT — what is proven versus what merely held over a sampled deck, and anything you left standing and why. Land one last fix first if it is quick — `Rule.check('"
            + this.book
            + "')` is the only read that moves the exit — but not at the cost of the statement: nothing you return ends the run, so an unfinished repair buys nothing and an honest account is the deliverable.";
   }

   @Override
   public List<AgentHook> hooks() {
      return List.of(
         new AgentHook() {
            @Override
            public Optional<String> afterTool(AgentContext var1, String var2, ToolResult var3, String var4) {
               if (var2 == null) {
                  return Optional.empty();
               } else if (CodifyToolkit.FILE_WRITE.matcher(var2).find()) {
                  return Optional.of(
                     "you wrote a rulebook file directly. The authoring verbs edit the same files and validate as they go — `Rule.calc.update` / `Rule.schema.update` / `Rule.generator.update` (parsed before they land), `Rule.lookups.update` (splices the `lookup` rate table in calc.js — there is no separate lookups file, and helper functions survive), `Rule.scenario.create` / `.update` / `.delete`. Prefer them: a bad direct write leaves a rulebook that will not load."
                  );
               } else {
                  return !var2.contains("Rule.check(") && !CodifyToolkit.MUTATORS.stream().noneMatch(var2::contains)
                     ? Optional.of(
                        "you changed the rulebook — re-read `Rule.check('"
                           + CodifyToolkit.this.book
                           + "')` before drawing any conclusion from it. The verdicts are computed from what is on disk NOW, and a change you did not re-verify is worth nothing."
                     )
                     : Optional.empty();
               }
            }
         }
      );
   }

   @Override
   public List<io.karatelabs.ext.agent.runtime.ExitCondition> exitConditions() {
      return List.of(
         var1 -> {
            var var2 = this.digests();
            if (var2.isEmpty()) {
               return Optional.empty();
            } else if (var2.equals(this.lastDigests)) {
               return Optional.empty();
            } else {
               var var3 = this.verdict();
               io.karatelabs.ext.max.jsapi.CodifyToolkit.RuleVerificationReport var4 = this.read(var3);
               if (var3 != null && var3.get("error") == null) {
                  this.lastDigests = var2;
               }

               this.noteContradictions(var1, var4);
               var var5 = this.replayRetired(var3, var4);
               io.karatelabs.ext.max.jsapi.CodifyToolkit.RuleVerificationReport var6 = var4.a(this.retiredShortfalls(var5));
               if (var6.a()) {
                  this.lastShortfall = null;
                  return Optional.of(io.karatelabs.ext.agent.runtime.AgentOutcome.success(this.projection(var3, var5)));
               } else if (this.requires.mayBlock() && var6.b()) {
                  this.lastShortfall = null;
                  return Optional.of(
                     io.karatelabs.ext.agent.runtime.AgentOutcome.blocked(
                        this.blockedProjection(var3, var6, var5),
                        "'"
                           + this.book
                           + "' is blocked by a verified contradiction: "
                           + var6.e().size()
                           + " stated guarantee(s) the rules themselves violate — see `contradictions` for each counterexample"
                     )
                  );
               } else {
                  this.lastShortfall = String.join("; ", var6.c());
                  if (this.ctx != null) {
                     this.ctx.hint("codify", "'" + this.book + "' not verified yet: " + this.lastShortfall);
                  }

                  return Optional.empty();
               }
            }
         }
      );
   }

   Map<String, Object> terminalProjection(Map<String, Object> var1) {
      return this.projection(var1, List.of());
   }

   private Map<String, Object> projection(Map<String, Object> var1, List<Map<String, Object>> var2) {
      LinkedHashMap var3 = new LinkedHashMap();
      var3.put("rule", this.book);
      var3.put("verified", true);
      var3.put("properties", properties(var1).size());
      var3.put("scenarios", this.scenarioCount());
      var3.put("complete", universeComplete(var1));
      if (!var2.isEmpty()) {
         var3.put("retired", var2);
      }

      var var4 = this.editedPins();
      if (!var4.isEmpty()) {
         var3.put("expectationsEdited", var4);
      }

      var3.put("check", var1);
      return var3;
   }

   private List<Map<String, Object>> replayRetired(Map<String, Object> var1, io.karatelabs.ext.max.jsapi.CodifyToolkit.RuleVerificationReport var2) {
      if (!this.ledger.isEmpty() && var1 != null && var1.get("error") == null) {
         LinkedHashSet var3 = new LinkedHashSet();

         for (Map var5 : var2.e()) {
            var3.add(String.valueOf(var5.get("name")));
         }

         ArrayList var13 = new ArrayList();

         for (io.karatelabs.ext.max.jsapi.CodifyToolkit.RecordedContradiction var6 : this.ledger.values()) {
            if (!var3.contains(var6.a())) {
               var13.add(Map.of("property", var6.a(), "input", var6.c()));
            }
         }

         if (var13.isEmpty()) {
            return List.of();
         } else {
            ArrayList var15 = new ArrayList();
            var var16 = propertyNames(var1);
            var var7 = guardedCriteria(var1);

            for (Map var9 : (Iterable<Map>) (Iterable<?>) (this.verifier.replay(this.book, var13))) {
               LinkedHashMap var10 = new LinkedHashMap(var9);
               String var11 = String.valueOf(var9.get("property"));
               io.karatelabs.ext.max.jsapi.CodifyToolkit.RecordedContradiction var12 = this.ledger.get(var11);
               if (var12 != null) {
                  var10.put("provenIteration", var12.d());
                  if (var12.b() != null) {
                     var10.put("req", var12.b());
                  }
               }

               var10.put("declared", var16.contains(var11));
               if (var12 != null && var12.b() != null && !var16.contains(var11)) {
                  var10.put("restated", var7.contains(var12.b()));
               }

               var15.add(var10);
            }

            return var15;
         }
      } else {
         return List.of();
      }
   }

   private static Set<String> guardedCriteria(Map<String, Object> var0) {
      LinkedHashSet var1 = new LinkedHashSet();

      for (Map var3 : properties(var0)) {
         if ("always".equals(String.valueOf(var3.get("kind"))) && var3.get("req") != null) {
            var1.add(String.valueOf(var3.get("req")));
         }
      }

      return var1;
   }

   private List<String> retiredShortfalls(List<Map<String, Object>> var1) {
      ArrayList var2 = new ArrayList();

      for (Map var4 : var1) {
         String var5 = String.valueOf(var4.get("property"));
         if (var4.get("error") != null) {
            var2.add("the counterexample proven against '" + var5 + "' could not be replayed (" + var4.get("error") + ") — it is not resolved until it can be");
         } else if (!Boolean.TRUE.equals(var4.get("declared"))) {
            if (!Boolean.TRUE.equals(var4.get("restated"))) {
               var2.add(
                  "the property '"
                     + var5
                     + "', proven VIOLATED at iteration "
                     + var4.get("provenIteration")
                     + ", is no longer declared — a guarantee shown to be false is a finding to report, never one to withdraw"
                     + (var4.get("req") == null ? "" : ". Re-stating it under a better name is fine; leaving " + var4.get("req") + " unguarded is not")
               );
            }
         } else if (Boolean.TRUE.equals(var4.get("falsifies"))) {
            if (Boolean.TRUE.equals(var4.get("inDomain"))) {
               var2.add(
                  "'"
                     + var5
                     + "' reads green, but the counterexample proven at iteration "
                     + var4.get("provenIteration")
                     + " still breaks it and is still inside the declared domain — the deck did not re-find it: "
                     + var4.get("input")
               );
            } else if (this.requires.keepWitnesses() && Boolean.TRUE.equals(var4.get("schemaValid"))) {
               var2.add(
                  "'"
                     + var5
                     + "' reads green only because the counterexample proven at iteration "
                     + var4.get("provenIteration")
                     + " left the declared domain ("
                     + var4.get("offending")
                     + "); it still breaks the property. Fix the rules, or say plainly that the input cannot occur — refuse it in `schema.js`, or `throw '<reason>'` at the top of `calc.js`"
               );
            }
         }
      }

      return var2;
   }

   List<String> shortfall(Map<String, Object> var1) {
      return this.read(var1).c();
   }

   io.karatelabs.ext.max.jsapi.CodifyToolkit.RuleVerificationReport read(Map<String, Object> var1) {
      ArrayList var2 = new ArrayList();
      ArrayList var3 = new ArrayList();
      if (var1 != null && var1.get("error") == null) {
         if (!list(var1.get("propertyErrors")).isEmpty()) {
            var3.add("the rulebook has authoring defects (propertyErrors)");
         }

         var var15 = properties(var1);
         int var5 = Math.max(this.requires.properties(), 1);
         if (var15.size() < var5) {
            var3.add("declares " + var15.size() + " propert" + (var15.size() == 1 ? "y" : "ies") + ", needs at least " + var5);
         }

         var var6 = propertyNames(var1);

         for (String var8 : this.snapshotProperties) {
            if (!var6.contains(var8)) {
               var3.add("the property '" + var8 + "' declared before this run is gone");
            }
         }

         for (Map var18 : (Iterable<Map>) (Iterable<?>) (var15)) {
            String var9 = String.valueOf(var18.get("name"));
            String var10 = String.valueOf(var18.get("kind"));
            if ("sometimes".equals(var10)) {
               if ("NOTREACHABLE".equals(var18.get("status"))) {
                  var3.add(
                     "the reachability property '" + var9 + "' is NOTREACHABLE — no input of the closed domain produces it; the rules or the property is wrong"
                  );
               } else if (!Boolean.TRUE.equals(var18.get("holds"))) {
                  var3.add("the reachability property '" + var9 + "' is never satisfied");
               }
            } else if ("always".equals(var10)) {
               String var11 = String.valueOf(var18.get("status"));
               if ("VIOLATED".equals(var11) && var18.get("counterexample") != null) {
                  var2.add(var18);
               } else if (!"HOLDS".equals(var11) && !"COMPLETE".equals(var11)) {
                  var3.add("the property '" + var9 + "' reads " + var11);
               } else if (this.requires.complete() && !"COMPLETE".equals(var11)) {
                  var3.add("the property '" + var9 + "' reads " + var11 + ", but proof (COMPLETE) was required");
               }
            } else {
               var3.add("the property '" + var9 + "' has an unrecognised kind '" + var10 + "'");
            }
         }

         if (!list(var1.get("notreachable")).isEmpty()) {
            var3.add(
               "there are unreachable decision arms (notreachable) — the rules, not the data. Each one names its `axis`, or `attribution:'notderivable'` when the comparison reached no input field — a different fix: give the calc a level or a row, do not rewrite the branch"
            );
         }

         if (!list(var1.get("notproduced")).isEmpty()) {
            var3.add("declared outcomes no input produces (notproduced) — an outcome the rules cannot reach, or a domain too narrow to reach it");
         }

         var var17 = rows(robustness(var1).get("savedRows"));
         if (!var17.isEmpty()) {
            var3.add(
               var17.size()
                  + " saved scenario(s) sit outside the domain `generator.js` declares (robustness.savedRows) — either those rows or that domain is wrong"
            );
         }

         var var19 = rows(var1.get("rejects"));
         if (var19.size() < this.requires.rejects()) {
            var3.add("declares " + var19.size() + " negative scenario(s), needs at least " + this.requires.rejects());
         }

         for (Map var22 : (Iterable<Map>) (Iterable<?>) (var19)) {
            if (!Boolean.TRUE.equals(var22.get("pass"))) {
               var3.add("the negative scenario '" + var22.get("_id") + "' reads " + var22.get("status"));
            }
         }

         Map var21 = var1.get("accepts") instanceof Map var23 ? var23 : Map.of();
         String var24 = var21.get("status") == null ? "NOTCHECKED" : String.valueOf(var21.get("status"));
         if ("REFUSED".equals(var24)) {
            var3.add(
               rows(var21.get("refused")).size() + " saved scenario(s) their own schema.js refuses (accepts.refused) — either those rows or the shape is wrong"
            );
         } else if (this.requires.rejects() > 0 && !"ACCEPTED".equals(var24)) {
            var3.add(
               "no saved scenario shows schema.js ACCEPTS valid input (accepts reads " + var24 + "), so the negative scenarios prove nothing about the shape"
            );
         }

         Map var26 = var1.get("expectations") instanceof Map var12 ? var12 : Map.of();
         if ("MISMATCHED".equals(String.valueOf(var26.get("status")))) {
            var3.add(
               rows(var26.get("failed")).size()
                  + " saved scenario(s) disagree with the figure stored on them (expectations.failed) — either the rules or the expectation is wrong"
            );
         }

         if (!this.snapshotPinned.isEmpty()) {
            var var27 = this.pinnedNow();

            for (String var14 : this.snapshotPinned.keySet()) {
               if (!var27.containsKey(var14)) {
                  var3.add(
                     "the scenario '"
                        + var14
                        + "' carried a stored expectation at kickoff and no longer does — a figure derived from the requirement is not one to withdraw"
                  );
               }
            }
         }

         int var28 = this.scenarioCount();
         int var30 = Math.max(Math.max(this.requires.scenarios(), this.snapshotScenarios), 1);
         if (var28 < var30) {
            var3.add("has " + var28 + " saved scenario(s), needs at least " + var30);
         }

         if (this.requires.complete() && !universeComplete(var1)) {
            var3.add("the declared universe is not closed + fully walked (universe.complete is false)");
         }

         return new io.karatelabs.ext.max.jsapi.CodifyToolkit.RuleVerificationReport(var3, var2);
      } else {
         String var4 = var1 == null ? null : String.valueOf(var1.get("error"));
         var3.add("Rule.check did not return a verdict" + (var4 != null && !var4.isBlank() ? ": " + oneLine(var4) : ""));
         return new io.karatelabs.ext.max.jsapi.CodifyToolkit.RuleVerificationReport(var3, List.of());
      }
   }

   private Map<String, Object> blockedProjection(Map<String, Object> var1, io.karatelabs.ext.max.jsapi.CodifyToolkit.RuleVerificationReport var2, List<Map<String, Object>> var3) {
      LinkedHashMap var4 = new LinkedHashMap();
      var4.put("rule", this.book);
      var4.put("verified", false);
      var4.put("blocked", "verified-contradiction");
      ArrayList var5 = new ArrayList();

      for (Map var7 : var2.e()) {
         LinkedHashMap var8 = new LinkedHashMap();
         var8.put("property", var7.get("name"));
         if (var7.get("req") != null) {
            var8.put("req", var7.get("req"));
         }

         var8.put("counterexample", var7.get("counterexample"));
         var8.put("violations", var7.get("violations"));
         if (var7.get("searchViolations") != null) {
            var8.put("searchViolations", var7.get("searchViolations"));
         }

         io.karatelabs.ext.max.jsapi.CodifyToolkit.RecordedContradiction var9 = this.ledger.get(String.valueOf(var7.get("name")));
         if (var9 != null) {
            var8.put("firstSeenIteration", var9.d());
         }

         var5.add(var8);
      }

      var4.put("contradictions", var5);
      var4.put("scenarios", this.scenarioCount());
      var4.put("complete", universeComplete(var1));
      if (!var3.isEmpty()) {
         var4.put("retired", var3);
      }

      var var10 = this.editedPins();
      if (!var10.isEmpty()) {
         var4.put("expectationsEdited", var10);
      }

      var4.put("check", var1);
      return var4;
   }

   private void noteContradictions(AgentRunState var1, io.karatelabs.ext.max.jsapi.CodifyToolkit.RuleVerificationReport var2) {
      for (Map var4 : var2.e()) {
         String var5 = String.valueOf(var4.get("name"));
         if (var4.get("counterexample") instanceof Map var6) {
            String var8 = var4.get("req") == null ? null : String.valueOf(var4.get("req"));
            this.ledger.putIfAbsent(var5, new io.karatelabs.ext.max.jsapi.CodifyToolkit.RecordedContradiction(var5, var8, castMap(var6), var1.iterations()));
         }
      }
   }

   public String lastShortfall() {
      return this.lastShortfall;
   }

   private Map<String, Object> verdict() {
      Map var1 = this.requires.maxProduct() == null ? null : Map.of("maxProduct", this.requires.maxProduct());

      try {
         return this.verifier.check(this.book, var1);
      } catch (RuntimeException var3) {
         return Map.of("error", String.valueOf(var3.getMessage()));
      }
   }

   private Map<String, String> digests() {
      io.karatelabs.ext.rules.RulebookRepository.RulebookSummary var1 = this.summary();
      if (var1 != null && var1.c() != null) {
         var var2 = new LinkedHashMap<>(var1.c());

         String var3;
         try {
            var3 = new RulebookRepository(this.rulesHome).d(this.book).d().c();
         } catch (RuntimeException var5) {
            var3 = "unreadable@" + System.nanoTime();
         }

         var2.put("digest", var3);
         return Map.copyOf(var2);
      } else {
         return Map.of();
      }
   }

   private int scenarioCount() {
      io.karatelabs.ext.rules.RulebookRepository.RulebookSummary var1 = this.summary();
      return var1 == null ? 0 : var1.b();
   }

   private Map<String, Object> pinnedNow() {
      LinkedHashMap var1 = new LinkedHashMap();

      try {
         for (Map var3 : new RulebookRepository(this.rulesHome).b(this.book).b()) {
            Map var4 = io.karatelabs.ext.rules.ExpectedResultMatcher.a(var3);
            if (var4 != null) {
               var1.put(String.valueOf(var3.get("_id")), canonical(var4));
            }
         }

         return var1;
      } catch (RuntimeException var5) {
         return Map.of();
      }
   }

   private static String canonical(Object var0) {
      if (var0 instanceof Map var6) {
         TreeMap var7 = new TreeMap();
         var6.forEach((var1x, var2x) -> var7.put(String.valueOf(var1x), canonical(var2x)));
         return var7.toString();
      } else if (!(var0 instanceof List var1)) {
         LinkedHashMap var5 = new LinkedHashMap();
         var5.put("v", var0);
         return Json.stringifyStrict(var5);
      } else {
         StringBuilder var2 = new StringBuilder("[");

         for (Object var4 : var1) {
            var2.append(canonical(var4)).append(',');
         }

         return var2.append(']').toString();
      }
   }

   private List<Map<String, Object>> editedPins() {
      ArrayList var1 = new ArrayList();
      if (this.snapshotPinned.isEmpty()) {
         return var1;
      } else {
         var var2 = this.pinnedNow();

         for (Entry var4 : this.snapshotPinned.entrySet()) {
            Object var5 = var2.get(var4.getKey());
            if (var5 != null && !var5.equals(var4.getValue())) {
               LinkedHashMap var6 = new LinkedHashMap();
               var6.put("scenario", var4.getKey());
               var6.put("wasExpecting", var4.getValue());
               var6.put("nowExpecting", var5);
               var1.add(var6);
            }
         }

         return var1;
      }
   }

   private io.karatelabs.ext.rules.RulebookRepository.RulebookSummary summary() {
      try {
         for (io.karatelabs.ext.rules.RulebookRepository.RulebookSummary var2 : new RulebookRepository(this.rulesHome).b()) {
            if (this.book.equals(var2.a())) {
               return var2;
            }
         }

         return null;
      } catch (Exception var3) {
         return null;
      }
   }

   private static Set<String> propertyNames(Map<String, Object> var0) {
      LinkedHashSet var1 = new LinkedHashSet();

      for (Map var3 : properties(var0)) {
         Object var4 = var3.get("name");
         if (var4 != null) {
            var1.add(String.valueOf(var4));
         }
      }

      return var1;
   }

   private static List<Map<String, Object>> properties(Map<String, Object> var0) {
      return var0 == null ? List.of() : rows(var0.get("properties"));
   }

   private static String oneLine(String var0) {
      String var1 = var0.replaceAll("\\s*\\n\\s*", " ").replaceAll("=+", "").trim();
      return var1.length() > 300 ? var1.substring(0, 300) + "…" : var1;
   }

   private static Map<String, Object> robustness(Map<String, Object> var0) {
      return var0 != null && var0.get("robustness") instanceof Map var1 ? castMap(var1) : Map.of();
   }

   private static Map<String, Object> castMap(Map<?, ?> var0) {
      return (Map<String, Object>)(Map<?, ?>)var0;
   }

   private static boolean universeComplete(Map<String, Object> var0) {
      return var0 != null && var0.get("universe") instanceof Map var1 && Boolean.TRUE.equals(var1.get("complete"));
   }

   private static List<Map<String, Object>> rows(Object var0) {
      ArrayList var1 = new ArrayList();

      for (Object var3 : list(var0)) {
         if (var3 instanceof Map var4) {
            var1.add(var4);
         }
      }

      return var1;
   }

   private static List<?> list(Object var0) {
      return var0 instanceof List var1 ? var1 : List.of();
   }

   @Override
   public void close() {
      if (this.verifier != null) {
         this.verifier.close();
      }
   }

   public record Requires(int properties, int scenarios, int rejects, boolean complete, Long maxProduct, boolean mayBlock, boolean keepWitnesses) {


      public static CodifyToolkit.Requires defaults() {
         return new CodifyToolkit.Requires(1, 1, 0, false, null, false, false);
      }
   }

   record RuleVerificationReport(List<String> d, List<Map<String, Object>> e) {


      io.karatelabs.ext.max.jsapi.CodifyToolkit.RuleVerificationReport a(List<String> var1) {
         if (var1.isEmpty()) {
            return this;
         } else {
            var var2 = new ArrayList<>(this.d);
            var2.addAll(var1);
            return new io.karatelabs.ext.max.jsapi.CodifyToolkit.RuleVerificationReport(var2, this.e);
         }
      }

      boolean a() {
         return this.d.isEmpty() && this.e.isEmpty();
      }

      boolean b() {
         return this.d.isEmpty() && !this.e.isEmpty();
      }

      List<String> c() {
         var var1 = new ArrayList<>(this.d);

         for (Map var3 : this.e) {
            var1.add("the property '" + var3.get("name") + "' is VIOLATED by the rules (" + var3.get("counterexample") + ")");
         }

         return var1;
      }

      public List<String> d() {
         return this.d;
      }

      public List<Map<String, Object>> e() {
         return this.e;
      }
   }

   private record RecordedContradiction(String a, String b, Map<String, Object> c, int d) {

   }
}
