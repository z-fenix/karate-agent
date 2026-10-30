package io.karatelabs.ext.rules.twin;

import io.karatelabs.ext.common.jsapi.JsApi;
import io.karatelabs.ext.common.jsapi.JsDispatch;
import io.karatelabs.ext.rules.WitnessBaselineStore;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.Map.Entry;
import java.util.function.Consumer;
import java.util.regex.Pattern;

public class Twin extends JsDispatch implements AutoCloseable {
   static final String TIMEOUT_ENV = "KARATE_RULES_TWIN_TIMEOUT_MS";
   public static final String WALK_TIMEOUT_ENV = "KARATE_RULES_TWIN_WALK_TIMEOUT_MS";
   public static final String LIVE_TIMEOUT_ENV = "KARATE_RULES_TWIN_LIVE_TIMEOUT_MS";
   private static final List<String> DISPOSITIONS = List.of("PASS", "FAIL", "UNRESOLVED", "NOTCHECKED", "REFUSED", "INVALID");
   private static final Set<String> ADJUDICATIONS = Set.of("model", "system", "open");
   private static final Pattern EVIDENCE = Pattern.compile("[0-9a-f]{64}");
   public static final String BOUNDARY = "A walk is evidence about the declaration, bounded by its ceilings; it never proves the system.";
   public static final io.karatelabs.ext.common.SandboxExecutor.SandboxFailureMapper MAPPER = new io.karatelabs.ext.common.SandboxExecutor.SandboxFailureMapper() {
      @Override
      public RuntimeException a(long var1) {
         return new TwinException(408, "TIMEOUT", "twin.js replay exceeded " + var1 + "ms — check the model for a callback that never returns");
      }

      @Override
      public RuntimeException a(Throwable var1) {
         for (Throwable var2 = var1; var2 != null; var2 = var2.getCause()) {
            if (var2 instanceof io.karatelabs.ext.common.jsapi.JsWireException && var2 instanceof RuntimeException var3) {
               return var3;
            }
         }

         return (RuntimeException)(var1 instanceof RuntimeException var4 ? var4 : new IllegalStateException(var1));
      }

      @Override
      public RuntimeException a() {
         return new TwinException(500, "INTERNAL", "twin.js replay interrupted");
      }
   };
   static final io.karatelabs.ext.common.SandboxExecutor.SandboxFailureMapper LIVE_MAPPER = new io.karatelabs.ext.common.SandboxExecutor.SandboxFailureMapper() {
      @Override
      public RuntimeException a(long var1) {
         return new TwinException(
            408,
            "TIMEOUT",
            "live replay exceeded " + var1 + "ms — a system that never answered, a reset hook that never exited, or a model callback that never returns"
         );
      }

      @Override
      public RuntimeException a(Throwable var1) {
         return Twin.MAPPER.a(var1);
      }

      @Override
      public RuntimeException a() {
         return Twin.MAPPER.a();
      }
   };
   private final io.karatelabs.ext.rules.RulebookRepository manager;
   private io.karatelabs.ext.common.MockStarter mocks = (var0, var1x, var2, var3, var4) -> JsApi.missing(
      "no mock starter is bound here, so Twin.mutate cannot serve its variants"
   );
   private static final String WITNESS_USAGE = "Twin.witness(rule, {baseUrl, against, plan?, kinds?, profileCeiling?, timeoutMs?, detail?, reset?, shrink?, shrinkBudget?, sutRevision?})";
   private static final Set<String> WITNESS_KEYS = Set.of(
      "baseUrl", "against", "plan", "kinds", "profileCeiling", "timeoutMs", "detail", "reset", "shrink", "shrinkBudget", "sutRevision"
   );
   private static final String ORDERS_USAGE = "Twin.orders(rule, {baseUrl, against, strength?, plan?, reset?, timeoutMs?, profileCeiling?, detail?, shrink?, shrinkBudget?, sutRevision?})";
   private static final Set<String> ORDERS_KEYS = Set.of(
      "baseUrl", "against", "strength", "plan", "reset", "timeoutMs", "profileCeiling", "detail", "shrink", "shrinkBudget", "sutRevision"
   );
   private static final String MUTATE_USAGE = "Twin.mutate(rule, {handlers, sequences?, candidates?, operators?, budget?, reset?})";
   private static final Set<String> MUTATE_KEYS = Set.of("handlers", "sequences", "candidates", "operators", "budget", "reset");
   private static final Set<String> BUDGET_KEYS = Set.of("mutants", "timeoutMs");
   private static final Set<String> OPERATORS = Set.of("NEGATE_CONDITIONALS", "CONDITIONALS_BOUNDARY", "REMOVE_CONDITIONALS", "INLINE_CONSTANT");
   private static final String ALLOWS_USAGE = "Twin.allows(rule, {steps | sequenceId, log, profileCeiling?, timeoutMs?})";
   private static final Set<String> ALLOWS_KEYS = Set.of("steps", "sequenceId", "log", "profileCeiling", "timeoutMs");
   static final String TRACE_INVALID = "TRACE_INVALID";
   private static final String WITNESS_LANE = "witness";
   private static final String ORDERS_LANE = "orders";
   private static final String NEXT_USAGE = "Twin.next(rule, history) requires a rule name and a history: a sequence id, an array of steps, or {record: id}";

   public Twin(Path var1) {
      this(new io.karatelabs.ext.rules.RulebookRepository(var1));
   }

   private Twin(io.karatelabs.ext.rules.RulebookRepository var1) {
      this.manager = var1;
      this.fn("explore", var1x -> this.explore(JsApi.str(var1x, 0), JsApi.arg(var1x, 1)));
      this.fn("run", var1x -> this.run(JsApi.str(var1x, 0), JsApi.arg(var1x, 1), JsApi.arg(var1x, 2)));
      this.fn("next", var1x -> this.next(JsApi.str(var1x, 0), JsApi.arg(var1x, 1)));
      this.fn("check", var1x -> this.check(JsApi.str(var1x, 0), JsApi.arg(var1x, 1)));
      this.fn("live", var1x -> this.live(JsApi.str(var1x, 0), JsApi.arg(var1x, 1)));
      this.fn("witness", var1x -> this.witness(JsApi.str(var1x, 0), JsApi.arg(var1x, 1)));
      this.fn("orders", var1x -> this.orders(JsApi.str(var1x, 0), JsApi.arg(var1x, 1)));
      this.fn("mutate", var1x -> this.mutate(JsApi.str(var1x, 0), JsApi.arg(var1x, 1)));
      this.fn("allows", var1x -> this.allows(JsApi.str(var1x, 0), JsApi.arg(var1x, 1)));
      this.fn("adjudicate", var1x -> this.adjudicate(JsApi.str(var1x, 0), JsApi.str(var1x, 1), JsApi.arg(var1x, 2)));
      this.fn("help", var0 -> io.karatelabs.ext.common.help.HelpTopicProvider.a("twin").a(var0));
   }

   public static Twin atRulesHome(Path var0) {
      return new Twin(var0);
   }

   public static Twin over(io.karatelabs.ext.rules.RulebookRepository var0) {
      return new Twin(var0);
   }

   public Twin withRoot(Path var1) {
      this.manager.a(var1);
      return this;
   }

   public Twin withMocks(io.karatelabs.ext.common.MockStarter var1) {
      this.mocks = var1;
      return this;
   }

   io.karatelabs.ext.common.MockStarter mocks() {
      return this.mocks;
   }

   public Map<String, Object> explore(String var1, Object var2) {
      if (var1 == null) {
         return JsApi.missing("Twin.explore(rule, {plan?, required?, detail?, output?}) requires a rule name");
      } else {
         Object var3 = var2 instanceof Map var4 ? var4.get("required") : null;
         if (var3 == null || io.karatelabs.ext.rules.twin.CanonicalJson.a(var3) || var3 instanceof Map var6 && var6.get("rows") instanceof List) {
            try {
               return this.exploring(this.withTwin(var1), var2, optional(var3));
            } catch (RuntimeException var5) {
               return JsApi.error(var5);
            }
         } else {
            return JsApi.missing("Twin.explore(rule, {required}) takes the graded rows as {rows: [...], sha?: '...'}");
         }
      }
   }

   private Map<String, Object> exploring(io.karatelabs.ext.rules.RulebookRepository.RulebookDocument var1, Object var2, Map<String, Object> var3) {
      Map var6;
      try (
         io.karatelabs.ext.rules.TwinRuleRuntime var4 = new io.karatelabs.ext.rules.TwinRuleRuntime(this.manager, var1.d());
         io.karatelabs.ext.common.SandboxExecutor.SandboxExecutionScope var5 = walkBox();
      ) {
         var6 = var5.a(() -> walk(var1, var2, var3, var4), MAPPER);
      }

      return var6;
   }

   private static io.karatelabs.ext.common.SandboxExecutor.SandboxExecutionScope walkBox() {
      return new io.karatelabs.ext.common.SandboxExecutor.SandboxExecutionScope("twin-walk", io.karatelabs.ext.common.SandboxExecutor.a("KARATE_RULES_TWIN_WALK_TIMEOUT_MS", 630000L));
   }

   private io.karatelabs.ext.rules.RulebookRepository.RulebookDocument withTwin(String var1) {
      io.karatelabs.ext.rules.RulebookRepository.RulebookDocument var2 = this.manager.b(var1);
      if (var2.k() == null) {
         throw new io.karatelabs.ext.rules.RuleApiException(
            404, "TWIN_NOT_FOUND", "rulebook '" + var1 + "' has no twin.js — author one with Rule.twin.update(name, source)"
         );
      } else {
         return var2;
      }
   }

   private static Map<String, Object> walk(io.karatelabs.ext.rules.RulebookRepository.RulebookDocument var0, Object var1, Map<String, Object> var2, RulebookRuntime var3) {
      TwinModel var4 = TwinModelLoader.a(var0.k());
      RequiredBehaviorVerifier var5 = var2 == null ? RequiredBehaviorVerifier.empty(var4) : RequiredBehaviorVerifier.fromConfig(var2, var4);
      LinkedHashMap var6 = null;
      if (var2 != null) {
         var6 = new LinkedHashMap();
         if (var5.sha() != null) {
            var6.put("sha", var5.sha());
         }

         var6.put("rows", var5.rowCount());
      }

      Object var7 = var1 instanceof Map var8 ? var8.get("plan") : null;
      TwinWalkPlan var12 = io.karatelabs.ext.rules.twin.TwinWalkPlan.a(var7, var4, io.karatelabs.ext.rules.twin.TwinExecutionLimits.a, var5.rowIds());
      TwinWalkResult var9 = TwinWalker.a(var4, var12, io.karatelabs.ext.rules.twin.TwinExecutionLimits.a, var3);
      Map var10 = TwinReportBuilder.a(var0.d(), var9, var12, var4, var6, var3.a());
      if (var1 instanceof Map var11 && "walk".equals(var11.get("detail"))) {
         var10.put("walk", var9.a());
      }

      return JsApi.applyOutputConvention(var10, () -> report(var10), var1);
   }

   private static String report(Map<String, Object> var0) {
      var var1 = map(var0, "ceilings");
      var var2 = map(var1, "usage");
      var var3 = map(var0, "states");
      var var4 = map(var0, "transitions");
      var var5 = map(var0, "stats");
      var var6 = map(var0, "fidelity");
      var var7 = map(var6, "guards");
      var var8 = map(var6, "invariants");
      var var9 = map(var6, "steps");
      StringBuilder var10 = new StringBuilder();
      var10.append("A walk is evidence about the declaration, bounded by its ceilings; it never proves the system.").append("\n\n");
      var10.append("# ").append(var0.get("rule")).append(" — twin walk under plan ").append(map(var0, "plan").get("planSha")).append("\n\n");
      var10.append("- ceilings: ")
         .append(var1.get("hit"))
         .append(" · frontier ")
         .append(var1.get("frontier"))
         .append(" · depth ")
         .append(var2.get("maxDepthReached"))
         .append(" · nodes ")
         .append(var2.get("nodesVisited"))
         .append(" · runs ")
         .append(var2.get("runsExecuted"))
         .append('\n');
      var10.append("- states reached: ").append(list(var3.get("reached"))).append('\n');
      var10.append("- states not reached: ").append(list(var3.get("notReached"))).append('\n');
      var10.append("- transitions: ")
         .append(((List)var4.get("observed")).size())
         .append(" observed · commands not applied: ")
         .append(list(var4.get("notObserved")))
         .append('\n');
      var10.append("- stats: ")
         .append(var5.get("nodes"))
         .append(" nodes · ")
         .append(var5.get("edges"))
         .append(" edges · ")
         .append(var5.get("refusals"))
         .append(" refusals · ")
         .append(var5.get("errors"))
         .append(" errors\n");
      var10.append("- guards: ")
         .append(var7.get("exercised"))
         .append(" exercised · ")
         .append(var7.get("observedFalse"))
         .append(" observedFalse · ")
         .append(var7.get("notChecked"))
         .append(" notChecked\n");
      var10.append("- invariants: ").append(var8.get("ran")).append(" ran · ").append(var8.get("failed")).append(" failed\n");
      var10.append("- steps: ")
         .append(var9.get("oracle"))
         .append(" oracle · ")
         .append(var9.get("bare"))
         .append(" bare · clock ")
         .append(var6.get("clock"))
         .append('\n');
      var10.append("- undeclaredOpenFields: ").append(openFields(var6.get("undeclaredOpenFields"))).append('\n');
      List var11 = (List)var0.get("counterexamples");
      var10.append("- counterexamples: ").append(var11.isEmpty() ? "none" : String.valueOf(var11.size())).append('\n');

      for (Object var13 : var11) {
         var var14 = asMap(var13);
         var10.append("  - ").append(var14.get("id")).append(' ').append(var14.get("kind"));
         if (var14.get("name") != null) {
            var10.append(" '").append(var14.get("name")).append('\'');
         }

         var10.append(" at ").append(var14.get("command")).append(" after ").append(((List)var14.get("path")).size()).append(" steps\n");
      }

      return var10.toString();
   }

   private static String list(Object var0) {
      List var1 = (List)var0;
      if (var1.isEmpty()) {
         return "none";
      } else {
         StringBuilder var2 = new StringBuilder();

         for (Object var4 : var1) {
            var2.append(var2.isEmpty() ? "" : ", ").append(var4);
         }

         return var2.toString();
      }
   }

   private static String openFields(Object var0) {
      List var1 = (List)var0;
      if (var1.isEmpty()) {
         return "none";
      } else {
         StringBuilder var2 = new StringBuilder();

         for (Object var4 : var1) {
            var var5 = asMap(var4);
            var2.append(var2.isEmpty() ? "" : ", ").append(var5.get("field")).append(" (").append(var5.get("distinct")).append(" distinct)");
         }

         return var2.toString();
      }
   }

   private static Map<String, Object> map(Map<String, Object> var0, String var1) {
      return asMap(var0.get(var1));
   }

   private static Map<String, Object> asMap(Object var0) {
      return (Map<String, Object>)var0;
   }

   private static Map<String, Object> optional(Object var0) {
      return var0 != null && !io.karatelabs.ext.rules.twin.CanonicalJson.a(var0) ? asMap(var0) : null;
   }

   public Map<String, Object> check(String var1, Object var2) {
      return this.check(var1, var2, io.karatelabs.ext.common.SandboxExecutor.a("KARATE_RULES_TWIN_LIVE_TIMEOUT_MS", 300000L));
   }

   Map<String, Object> check(String var1, Object var2, long var3) {
      if (var1 == null) {
         return JsApi.missing("Twin.check(rule, {plan?, required?, sutRevision?}) requires a rule name");
      } else {
         Object var5 = var2 instanceof Map var6 ? var6.get("required") : null;
         if (var5 == null || io.karatelabs.ext.rules.twin.CanonicalJson.a(var5) || var5 instanceof Map var8 && var8.get("rows") instanceof List) {
            try {
               io.karatelabs.ext.rules.RulebookRepository.RulebookDocument var9 = this.withTwin(var1);
               return this.checking(var9, var2, optional(var5), var3, io.karatelabs.ext.rules.twin.Twin.LiveRecordSnapshot.a(this.manager.c(var9.d())));
            } catch (RuntimeException var7) {
               return JsApi.error(var7);
            }
         } else {
            return JsApi.missing("Twin.check(rule, {required}) takes the graded rows as {rows: [...], sha?: '...'}");
         }
      }
   }

   private Map<String, Object> checking(io.karatelabs.ext.rules.RulebookRepository.RulebookDocument var1, Object var2, Map<String, Object> var3, long var4, io.karatelabs.ext.rules.twin.Twin.LiveRecordSnapshot var6) {
      String var7 = revision("Twin.check", var2 instanceof Map var8 ? asMap(var8) : Map.of());

      Map var18;
      try (io.karatelabs.ext.rules.TwinRuleRuntime var17 = new io.karatelabs.ext.rules.TwinRuleRuntime(this.manager, var1.d())) {
         io.karatelabs.ext.rules.twin.Twin.TwinCheckContext var9;
         try (io.karatelabs.ext.common.SandboxExecutor.SandboxExecutionScope var10 = walkBox()) {
            var9 = var10.a(() -> this.walked(var1, var2, var3, var17), MAPPER);
         }

         var18 = projected(
            var1,
            var9,
            this.leg(var1, var9.b(), var17, var4, var6),
            this.derived(var1, var9, var17, var7, var4),
            var3,
            io.karatelabs.ext.rules.twin.TwinSourceCompiler.a(var9.b(), var9.a(), this.manager.c(var1.d()), var1.d()),
            io.karatelabs.ext.rules.twin.UiProjectionBuilder.a(var9.b(), var1.d(), this.manager.a().resolve("app"))
         );
      }

      return var18;
   }

   private io.karatelabs.ext.rules.twin.Twin.TwinCheckContext walked(io.karatelabs.ext.rules.RulebookRepository.RulebookDocument var1, Object var2, Map<String, Object> var3, RulebookRuntime var4) {
      TwinModel var5 = TwinModelLoader.a(var1.k());
      RequiredBehaviorVerifier var6 = var3 == null ? RequiredBehaviorVerifier.empty(var5) : RequiredBehaviorVerifier.fromConfig(var3, var5);
      TwinWalkPlan var7 = io.karatelabs.ext.rules.twin.TwinWalkPlan.a(var2 instanceof Map var8 ? var8.get("plan") : null, var5, io.karatelabs.ext.rules.twin.TwinExecutionLimits.a, var6.rowIds());
      io.karatelabs.ext.rules.twin.TwinWalker.WalkExecution var9 = TwinWalker.b(var5, var7, io.karatelabs.ext.rules.twin.TwinExecutionLimits.a, var4);
      return new io.karatelabs.ext.rules.twin.Twin.TwinCheckContext(var5, var6, var7, var9, this.manager.a(var1, var5), expectMismatch(var1, var5, var4));
   }

   private static List<Map<String, Object>> expectMismatch(io.karatelabs.ext.rules.RulebookRepository.RulebookDocument var0, TwinModel var1, RulebookRuntime var2) {
      ArrayList var3 = new ArrayList();

      for (Map var5 : pinned(var0)) {
         List var6 = expectMismatches(var1, var5, var2);
         if (var6 != null && !var6.isEmpty()) {
            LinkedHashMap var7 = new LinkedHashMap();
            var7.put("id", var5.get("_id"));
            if (var5.get("_from") instanceof Map var8 && var8.get("kind") != null) {
               var7.put("kind", var8.get("kind"));
            }

            var7.put("mismatches", var6);
            var3.add(var7);
         }
      }

      return var3;
   }

   public static List<Map<String, Object>> expectMismatches(TwinModel var0, Map<String, Object> var1, RulebookRuntime var2) {
      if (var1.get("_expect") == null) {
         return null;
      } else {
         var var3 = shaped(var1.get("steps"));
         var var4 = declared(var1, var3.size());
         return io.karatelabs.ext.rules.twin.ExpectationEvaluator.a(var4) ? null : io.karatelabs.ext.rules.twin.ExpectationEvaluator.a(var4, TwinWalker.a(var0, var3, var2));
      }
   }

   private static Map<String, Object> projected(
      io.karatelabs.ext.rules.RulebookRepository.RulebookDocument var0,
      io.karatelabs.ext.rules.twin.Twin.TwinCheckContext var1,
      io.karatelabs.ext.rules.twin.Twin.LiveLaneResult var2,
      io.karatelabs.ext.rules.twin.Twin.DerivedEvidenceResult var3,
      Map<String, Object> var4,
      List<Map<String, Object>> var5,
      List<Map<String, Object>> var6
   ) {
      var var7 = var1.c().verifyRowsWithCoverage(var1.a(), var1.e().b(), var1.d().e(), var2.a());
      List var8 = (List)var7.get("requiredNotreached");
      List var9 = (List)var1.f().get("sequenceDrift");
      var var10 = new ArrayList<>(var2.b());
      var10.addAll(var3.a());
      ArrayList var11 = new ArrayList((List)var7.get("incomplete"));
      if (!Boolean.TRUE.equals(var1.f().get("blessed"))) {
         var11.add(Map.of("kind", "neverBlessed"));
      } else if (Boolean.TRUE.equals(var1.f().get("twinEdited"))) {
         var11.add(Map.of("kind", "twinEdited"));
      }

      var11.addAll(var2.c());
      var11.addAll(var3.b());
      TransitionPairIndex var12 = io.karatelabs.ext.rules.twin.TransitionPairIndex.a(var1.a());
      LinkedHashSet var13 = new LinkedHashSet();
      ArrayList var14 = new ArrayList();

      for (Object var16 : (List)var1.f().getOrDefault("sequencePairs", List.of())) {
         Map var17 = (Map)var16;
         io.karatelabs.ext.rules.twin.TransitionPairIndex.TransitionPairKey var18 = io.karatelabs.ext.rules.twin.TransitionPairIndex.a(var17);
         if (var12.a(var18)) {
            var13.add(var18);
         } else {
            var14.add(io.karatelabs.ext.rules.twin.TransitionPairIndex.b(var17));
         }
      }

      var var19 = io.karatelabs.ext.rules.twin.TransitionPairIndex.b(var1.a());
      if (var19 != null) {
         var11.add(var19);
      }

      var11.addAll(var14);
      LinkedHashMap var20 = new LinkedHashMap();
      var20.put("covered", var13.size());
      var20.put("of", var12.a());
      LinkedHashMap var21 = new LinkedHashMap();
      var21.put("rule", var0.d());
      var21.put("plan", var1.d().b());
      if (var4 != null) {
         LinkedHashMap var22 = new LinkedHashMap();
         if (var1.c().sha() != null) {
            var22.put("sha", var1.c().sha());
         }

         var22.put("rows", var1.c().rowCount());
         var21.put("required", var22);
         var21.put("requiredWitness", var7.get("requiredWitness"));
      }

      var21.put("ceilings", var1.a().d());
      var21.put("evidence", var7.get("evidence"));
      var21.put("transitionPairs", var20);
      LinkedHashMap var23 = new LinkedHashMap();
      var23.put("requiredNotreached", var8);
      var23.put("sequenceDrift", var9);
      var23.put("liveMismatch", var10);
      var23.put("binding", var5);
      var23.put("ui", var6);
      var23.put("expectMismatch", var1.g());
      var23.put("transitionPairGaps", var12.a(var1.e().b(), var13));
      var23.put("unobservedVariation", var2.d());
      var23.put("incomplete", var11);
      var21.put("findings", var23);
      var21.put("ci", RequiredBehaviorVerifier.buildVerificationVerdict(var8, var9, var10, var5, var6, var1.g(), var1.a()));
      return var21;
   }

   public Map<String, Object> live(String var1, Object var2) {
      return this.live(var1, var2, io.karatelabs.ext.common.SandboxExecutor.a("KARATE_RULES_TWIN_LIVE_TIMEOUT_MS", 300000L));
   }

   Map<String, Object> live(String var1, Object var2, long var3) {
      return this.live(var1, var2, var3, true);
   }

   Map<String, Object> live(String var1, Object var2, long var3, boolean var5) {
      return this.live(var1, var2, var3, var5, List.of());
   }

   Map<String, Object> live(String var1, Object var2, long var3, boolean var5, List<Map<String, Object>> var6) {
      return this.live(var1, var2, var3, var5, var6, null);
   }

   Map<String, Object> live(String var1, Object var2, long var3, boolean var5, List<Map<String, Object>> var6, Consumer<Map<String, Object>> var7) {
      if (var1 == null) {
         return JsApi.missing(
            "Twin.live(rule, {baseUrl, against, sequences?, profileCeiling?, timeoutMs?, detail?, reset?, shrink?, shrinkBudget?}) requires a rule name"
         );
      } else {
         try {
            return this.living(this.withTwin(var1), var2 instanceof Map var8 ? asMap(var8) : Map.of(), var3, var5, var6, var7);
         } catch (RuntimeException var9) {
            return JsApi.error(var9);
         }
      }
   }

   private Map<String, Object> living(
      io.karatelabs.ext.rules.RulebookRepository.RulebookDocument var1,
      Map<String, Object> var2,
      long var3,
      boolean var5,
      List<Map<String, Object>> var6,
      Consumer<Map<String, Object>> var7
   ) {
      String var8 = required(var2, "baseUrl");
      String var9 = required(var2, "against");
      if (!"mock".equals(var9) && !"live".equals(var9)) {
         throw usage("against", "must be 'mock' or 'live' — the grade this run claims");
      } else {
         int var10 = bounded(var2, "profileCeiling", 8, 1, 8);
         int var11 = bounded(var2, "timeoutMs", 10000, 1, Integer.MAX_VALUE);
         boolean var12 = "log".equals(var2.get("detail"));
         ResetHookRunner var13 = ResetHookRunner.a(var2.get("reset"));
         boolean var14 = flag(var2, "shrink");
         int var15 = bounded(var2, "shrinkBudget", 32, 2, 256);
         String var16 = revision("Twin.live", var2);
         var var17 = selected(var1, var6, var2.get("sequences"));
         io.karatelabs.ext.rules.TwinRuleRuntime var19 = new io.karatelabs.ext.rules.TwinRuleRuntime(this.manager, var1.d());

         io.karatelabs.ext.rules.twin.Twin.TwinLiveRunResult var18;
         try (io.karatelabs.ext.common.SandboxExecutor.SandboxExecutionScope var20 = liveBox(var3)) {
            var18 = var20.a(
               () -> this.folding(var1, var17, new io.karatelabs.ext.rules.HttpTwinTransport(var8, var11), var9, var16, var10, var12, var19, var13, var14, var15, var7),
               LIVE_MAPPER
            );
         } catch (Throwable var26) {
            try {
               var19.close();
            } catch (Throwable var23) {
               var26.addSuppressed(var23);
            }

            throw var26;
         }

         var19.close();
         String var27 = var5 ? new io.karatelabs.ext.rules.LiveRecordStore(this.manager.c(var1.d())).a(var18.a()) : null;
         Map<String, Object> var28 = new LinkedHashMap<>(io.karatelabs.ext.rules.RulebookRepository.a(var1));
         var28.put("profileCeiling", var10);
         var28.put("against", var9);
         if (var13 != null) {
            var28.put("resetHook", var13.a());
         }

         LinkedHashMap var21 = new LinkedHashMap();
         var21.put("rule", var1.d());
         var21.put("against", var9);
         var21.put("sequences", var18.b());
         var21.put("summary", var18.c());
         var21.put("provenance", var28);
         if (var27 != null) {
            var21.put("mtime", var27);
         }

         return var21;
      }
   }

   private io.karatelabs.ext.rules.twin.Twin.TwinLiveRunResult folding(
      io.karatelabs.ext.rules.RulebookRepository.RulebookDocument var1,
      List<Map<String, Object>> var2,
      io.karatelabs.ext.rules.twin.TwinReplayEngine.ReplayObserver var3,
      String var4,
      String var5,
      int var6,
      boolean var7,
      RulebookRuntime var8,
      ResetHookRunner var9,
      boolean var10,
      int var11,
      Consumer<Map<String, Object>> var12
   ) {
      TwinModel var13 = TwinModelLoader.a(var1.k());
      ArrayList var14 = new ArrayList(var2.size());

      for (Map var16 : var2) {
         var14.add(declared(var13, shaped(var16.get("steps"))));
      }

      String var34 = var1.i().get("twin");
      LinkedHashMap var35 = new LinkedHashMap();
      ArrayList var17 = new ArrayList(var2.size());
      LinkedHashMap<String, Object> var18 = new LinkedHashMap<>();

      for (String var20 : DISPOSITIONS) {
         var18.put(var20, 0);
      }

      int var36 = 0;
      io.karatelabs.ext.rules.twin.FailureTraceReducer.FailureReductionContext var37 = new io.karatelabs.ext.rules.twin.FailureTraceReducer.FailureReductionContext(var13, var3, var6, var8, var9);
      io.karatelabs.ext.rules.twin.FailureTraceReducer.ShrinkBudgetCounter var21 = new io.karatelabs.ext.rules.twin.FailureTraceReducer.ShrinkBudgetCounter();
      io.karatelabs.ext.rules.LiveRecordStore var22 = var10 ? new io.karatelabs.ext.rules.LiveRecordStore(this.manager.c(var1.d())) : null;

      for (int var23 = 0; var23 < var2.size(); var23++) {
         Map var24 = (Map)var2.get(var23);
         String var25 = String.valueOf(var24.get("_id"));
         var var26 = var8.a();
         io.karatelabs.ext.rules.twin.FailureTraceReducer.FailureReductionContext.ResetAndReplayResult var27 = var37.a((List<Map<String, Object>>)var14.get(var23));
         io.karatelabs.ext.rules.twin.ResetHookRunner.ResetHookResult var28 = var27.a();
         io.karatelabs.ext.rules.twin.TwinReplayEngine.ReplayResult var29 = var27.b();
         LinkedHashMap var30 = new LinkedHashMap();
         var30.put("against", var4);
         if (var5 != null) {
            var30.put("sutRevision", var5);
         }

         var30.put("twinSha", var34);
         var30.put("inputSha", io.karatelabs.ext.rules.RulebookRepository.a(var24.get("steps")));
         var30.put("rulebooks", consulted(var26, var8.a()));
         if (var28 != null) {
            var30.put("hook", var28.b());
         }

         var30.put("profileCeiling", var6);
         var30.put("disposition", var29.a());
         var30.put("log", var29.b());
         var30.put("runSha", io.karatelabs.ext.rules.LiveRecordStore.b(var30));
         var35.put(var25, var30);
         String var31 = String.valueOf(var29.a().get("kind"));
         var18.put(var31, (Integer)var18.getOrDefault(var31, 0) + 1);

         for (Map var33 : var29.b()) {
            var36 += var33.containsKey("request") ? 1 : 0;
         }

         LinkedHashMap var40 = new LinkedHashMap();
         var40.put("sequenceId", var25);
         var40.put("disposition", var29.a());
         var40.put("entries", var29.b().size());
         if (var10 && "FAIL".equals(var31) && !adjudicatedModel(var22, var29)) {
            var var41 = FailureTraceReducer.a(var37, (List<Map<String, Object>>)var14.get(var23), var29, var11, var21);
            var40.put("shrink", var41);
            var30.put("shrink", io.karatelabs.ext.rules.LiveRecordStore.a(var41, String.valueOf(var30.get("runSha"))));
         }

         if (var7) {
            var40.put("log", var29.b());
         }

         var17.add(var40);
         if (var12 != null) {
            var12.accept(var40);
         }
      }

      var18.put("exchanges", var36);
      if (var10) {
         var18.put("shrink", var21.a());
      }

      List var38 = io.karatelabs.ext.rules.twin.TwinReplayEngine.summarizeRefusals(var35);
      if (!var38.isEmpty()) {
         var18.put("wireRefusals", var38);
      }

      Map var39 = io.karatelabs.ext.rules.twin.ObservationReconciler.a(var1.k(), var35);
      if (var39 != null) {
         var18.put("observation", var39);
      }

      return new io.karatelabs.ext.rules.twin.Twin.TwinLiveRunResult(var35, var17, var18);
   }

   private static boolean adjudicatedModel(io.karatelabs.ext.rules.LiveRecordStore var0, io.karatelabs.ext.rules.twin.TwinReplayEngine.ReplayResult var1) {
      var var2 = var0.a(String.valueOf(var1.a().get("evidence")));
      return var2 != null && "model".equals(var2.get("disposition"));
   }

   public Map<String, Object> witness(String var1, Object var2) {
      return this.witness(var1, var2, io.karatelabs.ext.common.SandboxExecutor.a("KARATE_RULES_TWIN_LIVE_TIMEOUT_MS", 300000L));
   }

   Map<String, Object> witness(String var1, Object var2, long var3) {
      if (var1 == null) {
         return JsApi.missing(
            "Twin.witness(rule, {baseUrl, against, plan?, kinds?, profileCeiling?, timeoutMs?, detail?, reset?, shrink?, shrinkBudget?, sutRevision?}) requires a rule name"
         );
      } else {
         try {
            return this.witnessing(this.withTwin(var1), var2 instanceof Map var5 ? asMap(var5) : Map.of(), var3);
         } catch (RuntimeException var6) {
            return JsApi.error(var6);
         }
      }
   }

   private Map<String, Object> witnessing(io.karatelabs.ext.rules.RulebookRepository.RulebookDocument var1, Map<String, Object> var2, long var3) {
      for (Object var6 : var2.keySet()) {
         if (!WITNESS_KEYS.contains(String.valueOf(var6))) {
            if ("sequences".equals(var6)) {
               throw usage(
                  "Twin.witness",
                  "sequences",
                  "selects pinned rows, and every witness is derived from the walk — Twin.witness(rule, {baseUrl, against, plan?, kinds?, profileCeiling?, timeoutMs?, detail?, reset?, shrink?, shrinkBudget?, sutRevision?})"
               );
            }

            throw usage(
               "Twin.witness",
               String.valueOf(var6),
               "is not an option — Twin.witness(rule, {baseUrl, against, plan?, kinds?, profileCeiling?, timeoutMs?, detail?, reset?, shrink?, shrinkBudget?, sutRevision?})"
            );
         }
      }

      String var30 = required(var2, "baseUrl");
      String var31 = required(var2, "against");
      if (!"mock".equals(var31) && !"live".equals(var31)) {
         throw usage("Twin.witness", "against", "must be 'mock' or 'live' — the grade this run claims");
      } else {
         var var7 = kinds(var2.get("kinds"));
         int var8 = bounded(var2, "profileCeiling", 8, 1, 8);
         int var9 = bounded(var2, "timeoutMs", 10000, 1, Integer.MAX_VALUE);
         boolean var10 = "log".equals(var2.get("detail"));
         ResetHookRunner var11 = ResetHookRunner.a(var2.get("reset"));
         boolean var12 = flag(var2, "shrink");
         int var13 = bounded(var2, "shrinkBudget", 32, 2, 256);
         String var14 = revision("Twin.witness", var2);
         io.karatelabs.ext.rules.twin.FailureTraceReducer.ShrinkBudgetCounter var15 = new io.karatelabs.ext.rules.twin.FailureTraceReducer.ShrinkBudgetCounter();

         LinkedHashMap var23;
         try (io.karatelabs.ext.rules.TwinRuleRuntime var16 = new io.karatelabs.ext.rules.TwinRuleRuntime(this.manager, var1.d())) {
            io.karatelabs.ext.rules.twin.Twin.WitnessCoverageResult var17;
            try (io.karatelabs.ext.common.SandboxExecutor.SandboxExecutionScope var18 = walkBox()) {
               var17 = var18.a(() -> this.owed(var1, var2.get("plan"), var7, var11, var31, var16), MAPPER);
            }

            List var32;
            try (io.karatelabs.ext.common.SandboxExecutor.SandboxExecutionScope var19 = liveBox(var3)) {
               var32 = var19.a(() -> replayed(var17, new io.karatelabs.ext.rules.HttpTwinTransport(var30, var9), var8, var10, var16, var11, var12, var13, var15), LIVE_MAPPER);
            }

            Map var33 = record(var17, var31, var14, var7, var32, var12 ? var15.a() : null);
            String var20 = new WitnessBaselineStore(this.manager.c(var1.d())).a(var33);
            Map<String, Object> var21 = new LinkedHashMap<>(io.karatelabs.ext.rules.RulebookRepository.a(var1));
            var21.put("profileCeiling", var8);
            var21.put("against", var31);
            if (var11 != null) {
               var21.put("resetHook", var11.a());
            }

            LinkedHashMap var22 = new LinkedHashMap();
            var22.put("rule", var1.d());
            var22.putAll(var33);
            var22.put("provenance", var21);
            var22.put("mtime", var20);
            var23 = var22;
         }

         return var23;
      }
   }

   private io.karatelabs.ext.rules.twin.Twin.WitnessCoverageResult owed(io.karatelabs.ext.rules.RulebookRepository.RulebookDocument var1, Object var2, Set<String> var3, ResetHookRunner var4, String var5, RulebookRuntime var6) {
      TwinModel var7 = TwinModelLoader.a(var1.k());
      creating(var7, "Twin.witness", "witness", var4, var5);
      TwinWalkPlan var8 = io.karatelabs.ext.rules.twin.TwinWalkPlan.a(var2, var7, io.karatelabs.ext.rules.twin.TwinExecutionLimits.a, Set.of());
      io.karatelabs.ext.rules.twin.TwinWalker.WalkExecution var9 = TwinWalker.b(var7, var8, io.karatelabs.ext.rules.twin.TwinExecutionLimits.a, var6);
      io.karatelabs.ext.rules.twin.TwinCoverageAnalyzer.CoverageReport var10 = TwinCoverageAnalyzer.a(var7, var9.a(), var9.b(), rows(this.manager.a(var1, var7).get("sequencePairs")), var3);
      if (var7.j() == null && var4 == null) {
         for (io.karatelabs.ext.rules.twin.TwinCoverageAnalyzer.CoverageObligation var12 : var10.a()) {
            fresh(var7, "Twin.witness", "witness", var12.b(), var12.d());
         }
      }

      for (io.karatelabs.ext.rules.twin.TwinCoverageAnalyzer.CoverageObligation var15 : var10.a()) {
         declared(var7, var15.d());
      }

      LinkedHashMap var14 = new LinkedHashMap();
      var14.put("walkSha", var9.a().c());
      var14.put("sequencesSha", var1.i().get("sequences"));
      var14.put("planSha", var8.b().get("planSha"));
      return new io.karatelabs.ext.rules.twin.Twin.WitnessCoverageResult(var7, TwinCoverageAnalyzer.a(var7), var10, var14);
   }

   private static List<Map<String, Object>> replayed(io.karatelabs.ext.rules.twin.Twin.WitnessCoverageResult var0, io.karatelabs.ext.rules.twin.TwinReplayEngine.ReplayObserver var1, int var2, boolean var3, RulebookRuntime var4, ResetHookRunner var5, boolean var6, int var7, io.karatelabs.ext.rules.twin.FailureTraceReducer.ShrinkBudgetCounter var8) {
      io.karatelabs.ext.rules.twin.FailureTraceReducer.FailureReductionContext var9 = new io.karatelabs.ext.rules.twin.FailureTraceReducer.FailureReductionContext(var0.a(), var1, var2, var4, var5);
      ArrayList var10 = new ArrayList();

      for (io.karatelabs.ext.rules.twin.TwinCoverageAnalyzer.CoverageObligation var12 : var0.c().a()) {
         io.karatelabs.ext.rules.twin.TwinReplayEngine.ReplayResult var13 = var9.a(var12.d()).b();
         LinkedHashMap var14 = new LinkedHashMap();
         var14.put("kind", var12.a());
         var14.put("key", var12.b());
         var14.putAll(TwinCoverageAnalyzer.a(var12, var13, var0.b()));
         var14.put("obligation", var12.c());
         var14.put("steps", io.karatelabs.ext.rules.twin.CanonicalJson.d(var12.d()));
         if ("refuted".equals(var14.get("credit"))) {
            io.karatelabs.ext.rules.twin.TwinReplayEngine.ReplayResult var15 = var9.a(var12.d()).b();
            var14.put("confirmed", Objects.equals(FailureTraceReducer.a(var13), FailureTraceReducer.a(var15)));
            if (var6) {
               var14.put("shrink", FailureTraceReducer.a(var9, var12.d(), var13, var7, var8, var15));
            }

            var14.put("log", var13.b());
         } else if (var3) {
            var14.put("log", var13.b());
         }

         var10.add(var14);
      }

      return var10;
   }

   private static Map<String, Object> record(io.karatelabs.ext.rules.twin.Twin.WitnessCoverageResult var0, String var1, String var2, Set<String> var3, List<Map<String, Object>> var4, Map<String, Object> var5) {
      io.karatelabs.ext.rules.twin.TwinCoverageAnalyzer.CoverageReport var6 = var0.c();
      LinkedHashMap var7 = new LinkedHashMap();
      var7.put("against", var1);
      if (var2 != null) {
         var7.put("sutRevision", var2);
      }

      var7.put("kinds", new ArrayList(var3));
      var7.put("selection", var0.d());

      for (String var9 : TwinCoverageAnalyzer.e) {
         if (var3.contains(var9)) {
            var var10 = TwinCoverageAnalyzer.a(var4, var9);
            switch (var9) {
               case "transitions":
                  var10.put("pruned", var6.b());
                  break;
               case "pairs":
                  var10.put("coveredByDeck", var6.c());
                  break;
               default:
                  var10.put("byClass", TwinCoverageAnalyzer.a(var4));
                  var10.put("scope", var6.f());
            }

            var10.put("of", var6.g().get(var9));
            var7.put(var9, var10);
         }
      }

      var7.put("capturedCollapsed", var6.d());
      var7.put("captureClasses", var6.e());
      if (var5 != null) {
         var7.put("shrink", var5);
      }

      var7.put("obligations", var4);
      return var7;
   }

   private static Set<String> kinds(Object var0) {
      if (!present(var0)) {
         return new LinkedHashSet<>(TwinCoverageAnalyzer.e);
      } else if (var0 instanceof List var1 && !var1.isEmpty()) {
         for (Object var3 : var1) {
            if (!TwinCoverageAnalyzer.e.contains(String.valueOf(var3))) {
               throw usage("Twin.witness", "kinds", "'" + var3 + "' is not a population — " + TwinCoverageAnalyzer.e);
            }
         }

         LinkedHashSet var5 = new LinkedHashSet();

         for (String var4 : TwinCoverageAnalyzer.e) {
            if (var1.contains(var4)) {
               var5.add(var4);
            }
         }

         return var5;
      } else {
         throw usage("Twin.witness", "kinds", "must be a non-empty array of " + TwinCoverageAnalyzer.e);
      }
   }

   public Map<String, Object> orders(String var1, Object var2) {
      return this.orders(var1, var2, io.karatelabs.ext.common.SandboxExecutor.a("KARATE_RULES_TWIN_LIVE_TIMEOUT_MS", 300000L));
   }

   Map<String, Object> orders(String var1, Object var2, long var3) {
      return this.orders(var1, var2, var3, null);
   }

   Map<String, Object> orders(String var1, Object var2, long var3, Long var5) {
      if (var1 == null) {
         return JsApi.missing(
            "Twin.orders(rule, {baseUrl, against, strength?, plan?, reset?, timeoutMs?, profileCeiling?, detail?, shrink?, shrinkBudget?, sutRevision?}) requires a rule name"
         );
      } else {
         try {
            return this.ordering(this.withTwin(var1), var2 instanceof Map var6 ? asMap(var6) : Map.of(), var3, var5);
         } catch (RuntimeException var7) {
            return JsApi.error(var7);
         }
      }
   }

   private Map<String, Object> ordering(io.karatelabs.ext.rules.RulebookRepository.RulebookDocument var1, Map<String, Object> var2, long var3, Long var5) {
      for (Object var7 : var2.keySet()) {
         if (!ORDERS_KEYS.contains(String.valueOf(var7))) {
            throw usage(
               "Twin.orders",
               String.valueOf(var7),
               "is not an option — Twin.orders(rule, {baseUrl, against, strength?, plan?, reset?, timeoutMs?, profileCeiling?, detail?, shrink?, shrinkBudget?, sutRevision?})"
            );
         }
      }

      String var31 = required(var2, "baseUrl");
      String var32 = required(var2, "against");
      if (!"mock".equals(var32) && !"live".equals(var32)) {
         throw usage("Twin.orders", "against", "must be 'mock' or 'live' — the grade this run claims");
      } else {
         int var8 = bounded("Twin.orders", var2.get("strength"), "strength", 2, 2, 3);
         int var9 = bounded(var2, "profileCeiling", 8, 1, 8);
         int var10 = bounded(var2, "timeoutMs", 10000, 1, Integer.MAX_VALUE);
         boolean var11 = "log".equals(var2.get("detail"));
         ResetHookRunner var12 = ResetHookRunner.a(var2.get("reset"));
         boolean var13 = flag(var2, "shrink");
         int var14 = bounded(var2, "shrinkBudget", 32, 2, 256);
         String var15 = revision("Twin.orders", var2);
         io.karatelabs.ext.rules.twin.FailureTraceReducer.ShrinkBudgetCounter var16 = new io.karatelabs.ext.rules.twin.FailureTraceReducer.ShrinkBudgetCounter();

         LinkedHashMap var24;
         try (io.karatelabs.ext.rules.TwinRuleRuntime var17 = new io.karatelabs.ext.rules.TwinRuleRuntime(this.manager, var1.d(), var5)) {
            io.karatelabs.ext.rules.twin.Twin.TwinConstructionResult var18;
            try (io.karatelabs.ext.common.SandboxExecutor.SandboxExecutionScope var19 = walkBox()) {
               var18 = var19.a(() -> this.constructed(var1, var2.get("plan"), var8, var12, var32, var17), MAPPER);
            }

            io.karatelabs.ext.rules.twin.Twin.TwinOrderExecutionResult var33;
            try (io.karatelabs.ext.common.SandboxExecutor.SandboxExecutionScope var20 = liveBox(var3)) {
               var33 = var20.a(
                  () -> dealt(var18, new io.karatelabs.ext.rules.HttpTwinTransport(var31, var10), var8, var9, var11, var17, var12, var13, var14, var16), LIVE_MAPPER
               );
            }

            var var34 = record(var18, var33, var32, var15, var8);
            String var21 = new io.karatelabs.ext.rules.TwinOrdersRepository(this.manager.c(var1.d())).a(var34);
            Map<String, Object> var22 = new LinkedHashMap<>(io.karatelabs.ext.rules.RulebookRepository.a(var1));
            var22.put("profileCeiling", var9);
            var22.put("against", var32);
            if (var12 != null) {
               var22.put("resetHook", var12.a());
            }

            LinkedHashMap var23 = new LinkedHashMap();
            var23.put("rule", var1.d());
            var23.putAll(var34);
            var23.put("provenance", var22);
            var23.put("mtime", var21);
            var24 = var23;
         }

         return var24;
      }
   }

   private io.karatelabs.ext.rules.twin.Twin.TwinConstructionResult constructed(io.karatelabs.ext.rules.RulebookRepository.RulebookDocument var1, Object var2, int var3, ResetHookRunner var4, String var5, RulebookRuntime var6) {
      TwinModel var7 = TwinModelLoader.a(var1.k());
      creating(var7, "Twin.orders", "order", var4, var5);
      TwinWalkPlan var8 = io.karatelabs.ext.rules.twin.TwinWalkPlan.a(var2, var7, io.karatelabs.ext.rules.twin.TwinExecutionLimits.a, Set.of());
      io.karatelabs.ext.rules.twin.SequenceExplorer.SequenceExplorationResult var9 = io.karatelabs.ext.rules.twin.SequenceExplorer.a(var7, var8, var3, var6);
      if (var7.j() == null && var4 == null) {
         for (io.karatelabs.ext.rules.twin.SequenceExplorer.GeneratedSequence var11 : var9.c()) {
            fresh(var7, "Twin.orders", "order", var11.a(), var11.b());
         }
      }

      for (io.karatelabs.ext.rules.twin.SequenceExplorer.GeneratedSequence var14 : var9.c()) {
         declared(var7, var14.b());
      }

      var var13 = consulted(var6);
      LinkedHashMap var15 = new LinkedHashMap();
      var15.put("twinSha", var1.i().get("twin"));
      var15.put("rulebooks", var13);
      var15.put("inputSha", io.karatelabs.ext.rules.twin.CanonicalJson.a(io.karatelabs.ext.rules.twin.CanonicalJson.c(var13)));
      var15.put("planSha", var8.b().get("planSha"));
      var15.put("alphabetSha", var9.e());
      var15.put("deckSha", var9.f());
      var15.put("constructionVersion", 1);
      return new io.karatelabs.ext.rules.twin.Twin.TwinConstructionResult(var7, var9, var15, var7.i() != null);
   }

   private static Map<String, Object> consulted(RulebookRuntime var0) {
      LinkedHashMap var1 = new LinkedHashMap();

      for (Entry var3 : var0.a().entrySet()) {
         var1.put((String)var3.getKey(), ((io.karatelabs.ext.rules.twin.RulebookRuntime.RulebookUsageStats)var3.getValue()).a());
      }

      return var1;
   }

   private static io.karatelabs.ext.rules.twin.Twin.TwinOrderExecutionResult dealt(io.karatelabs.ext.rules.twin.Twin.TwinConstructionResult var0, io.karatelabs.ext.rules.twin.TwinReplayEngine.ReplayObserver var1, int var2, int var3, boolean var4, RulebookRuntime var5, ResetHookRunner var6, boolean var7, int var8, io.karatelabs.ext.rules.twin.FailureTraceReducer.ShrinkBudgetCounter var9) {
      io.karatelabs.ext.rules.twin.SequenceExplorer.SequenceExplorationResult var10 = var0.b();
      io.karatelabs.ext.rules.twin.FailureTraceReducer.FailureReductionContext var11 = new io.karatelabs.ext.rules.twin.FailureTraceReducer.FailureReductionContext(var0.a(), var1, var3, var5, var6);
      io.karatelabs.ext.rules.twin.SequenceExplorer.CoverageLedger var12 = new io.karatelabs.ext.rules.twin.SequenceExplorer.CoverageLedger(var10.a().size(), var2);
      ArrayList var13 = new ArrayList(var10.c().size());
      Map var14 = null;
      int var15 = 0;

      for (io.karatelabs.ext.rules.twin.SequenceExplorer.GeneratedSequence var17 : var10.c()) {
         io.karatelabs.ext.rules.twin.FailureTraceReducer.FailureReductionContext.ResetAndReplayResult var18 = var11.a(var17.b());
         if (var18.a() != null) {
            var14 = var18.a().b();
         }

         io.karatelabs.ext.rules.twin.TwinReplayEngine.ReplayResult var19 = var18.b();
         boolean var20 = "FAIL".equals(var19.a().get("kind"));
         io.karatelabs.ext.rules.twin.TwinReplayEngine.ReplayResult var21 = var20 ? var11.a(var17.b()).b() : null;
         boolean var22 = var20 && Objects.equals(FailureTraceReducer.a(var19), FailureTraceReducer.a(var21));
         List var23 = "PASS".equals(var19.a().get("kind")) ? credited(var17.b(), var19.b()) : List.of();
         int var24 = io.karatelabs.ext.rules.twin.SequenceExplorer.a(var12, var10.a(), var17.b(), var23);
         var15 += var24;
         LinkedHashMap var25 = new LinkedHashMap();
         var25.put("id", var17.a());
         var25.put("steps", io.karatelabs.ext.rules.twin.CanonicalJson.d(var17.b()));
         var25.put("planned", var17.c());
         var25.put("disposition", var20 && !var22 ? unstable(var19.a()) : var19.a());
         var25.put("checked", checked(var19.b()));
         var25.put("credited", var23);
         var25.put("applied", var24);
         if (var20) {
            var25.put("confirmed", var22);
            if (var7) {
               var25.put("shrink", FailureTraceReducer.a(var11, var17.b(), var19, var8, var9, var21));
            }

            var25.put("log", var19.b());
         } else if (var4) {
            var25.put("log", var19.b());
         }

         var13.add(var25);
      }

      return new io.karatelabs.ext.rules.twin.Twin.TwinOrderExecutionResult(var13, var15, var14);
   }

   private static Map<String, Object> unstable(Map<String, Object> var0) {
      var var1 = new LinkedHashMap<>(var0);
      var1.put("kind", "UNSTABLE");
      return var1;
   }

   private static List<Integer> credited(List<Map<String, Object>> var0, List<Map<String, Object>> var1) {
      ArrayList var2 = new ArrayList();

      for (Map var4 : var1) {
         if ("command".equals(var4.get("kind"))
            && "applied".equals(var4.get("outcome"))
            && var4.get("at") instanceof Number var5
            && var5.intValue() < var0.size()
            && size(var4.get("profileBefore")) == 1
            && size(var4.get("profileAfter")) == 1
            && var4.get("candidates") instanceof List var6
            && var6.size() == 1
            && var6.get(0) instanceof Map var7
            && Boolean.TRUE.equals(var7.get("consistent"))
            && var7.get("model") instanceof Map var8
            && "applied".equals(var8.get("kind"))) {
            var2.add(var5.intValue());
         }
      }

      return var2;
   }

   private static int checked(List<Map<String, Object>> var0) {
      int var1 = 0;

      for (Map var3 : var0) {
         var1 += "command".equals(var3.get("kind")) && var3.containsKey("request") ? 1 : 0;
      }

      return var1;
   }

   private static int size(Object var0) {
      return var0 instanceof List var1 ? var1.size() : -1;
   }

   private static Map<String, Object> record(io.karatelabs.ext.rules.twin.Twin.TwinConstructionResult var0, io.karatelabs.ext.rules.twin.Twin.TwinOrderExecutionResult var1, String var2, String var3, int var4) {
      io.karatelabs.ext.rules.twin.SequenceExplorer.SequenceExplorationResult var5 = var0.b();
      LinkedHashMap var6 = new LinkedHashMap();
      var6.put("labels", var5.a());
      var6.put("of", var5.a().size());
      LinkedHashMap var7 = new LinkedHashMap();
      var7.put("hit", var5.g());
      var7.put("modelSteps", var5.h());
      var7.put("sequences", var5.c().size());
      LinkedHashMap var8 = new LinkedHashMap();
      var8.put("of", var5.b());
      var8.put("planned", var5.d());
      var8.put("applied", var1.b());
      var8.put("unconfirmed", var5.d() - var1.b());
      var8.put("residual", var5.b() - var5.d());
      LinkedHashMap var9 = new LinkedHashMap();
      var9.put("against", var2);
      if (var3 != null) {
         var9.put("sutRevision", var3);
      }

      var9.put("strength", var4);
      var9.put("alphabet", var6);
      var9.put("selection", var0.c());
      var9.put("ceilings", var7);
      var9.put("targets", var8);
      var9.put("readBack", var0.d());
      if (var1.c() != null) {
         var9.put("hook", var1.c());
      }

      var9.put("sequences", var1.a());
      return var9;
   }

   public Map<String, Object> mutate(String var1, Object var2) {
      return this.mutate(var1, var2, false);
   }

   Map<String, Object> mutate(String var1, Object var2, boolean var3) {
      if (var1 == null) {
         return JsApi.missing("Twin.mutate(rule, {handlers, sequences?, candidates?, operators?, budget?, reset?}) requires a rule name");
      } else {
         Map var4 = var2 instanceof Map var5 ? asMap(var5) : Map.of();

         try {
            return this.mutating(this.withTwin(var1), var4, var3);
         } catch (RuntimeException var6) {
            return JsApi.error(var6);
         }
      }
   }

   private Map<String, Object> mutating(io.karatelabs.ext.rules.RulebookRepository.RulebookDocument var1, Map<String, Object> var2, boolean var3) {
      for (Object var5 : var2.keySet()) {
         if ("against".equals(var5) || "baseUrl".equals(var5)) {
            throw usage(
               "Twin.mutate",
               String.valueOf(var5),
               "names a target, and the graded subject is the kit's own mock served from memory — Twin.mutate(rule, {handlers, sequences?, candidates?, operators?, budget?, reset?})"
            );
         }

         if (!MUTATE_KEYS.contains(String.valueOf(var5))) {
            throw usage(
               "Twin.mutate", String.valueOf(var5), "is not an option — Twin.mutate(rule, {handlers, sequences?, candidates?, operators?, budget?, reset?})"
            );
         }
      }

      Path var12 = this.manager.c(var1.d()).getParent();
      Path var13 = var12.toAbsolutePath().getParent();
      if (var2.get("handlers") instanceof String var6 && !var6.isBlank()) {
         String var14;
         try {
            var14 = Files.readString(io.karatelabs.ext.rules.twin.StandInServerLauncher.a(var13, var6));
         } catch (IOException var11) {
            throw usage("Twin.mutate", "handlers", "is not readable: " + var6);
         }

         Map var8 = var2.get("budget") instanceof Map var9 ? asMap(var9) : Map.of();

         for (Object var16 : var8.keySet()) {
            if (!BUDGET_KEYS.contains(String.valueOf(var16))) {
               throw usage("Twin.mutate", "budget." + var16, "is not an option — {mutants?, timeoutMs?}");
            }
         }

         ResetHookRunner.a(var2.get("reset"));
         return TwinMutationAnalyzer.analyze(
            this,
            var1,
            new io.karatelabs.ext.rules.twin.TwinMutationAnalyzer.MutationOptions(
               var1.d(),
               var6,
               var14,
               var13,
               var12,
               names("sequences", var2.get("sequences")),
               !present(var2.get("candidates")) || flag(var2, "candidates"),
               operators(var2.get("operators")),
               var8.containsKey("mutants") ? bounded(var8.get("mutants"), "budget.mutants", 0, 1, Integer.MAX_VALUE) : null,
               var8.containsKey("timeoutMs") ? bounded(var8.get("timeoutMs"), "budget.timeoutMs", 0, 1, Integer.MAX_VALUE) : null,
               var2.get("reset"),
               var3
            )
         );
      } else {
         throw usage("Twin.mutate", "handlers", "is required: the guard source, project-relative (e.g. '/mock/handlers.js')");
      }
   }

   private static Set<String> operators(Object var0) {
      var var1 = names("operators", var0);
      if (var1 == null) {
         return Set.of();
      } else {
         for (String var3 : (Iterable<String>) (Iterable<?>) (var1)) {
            if (!OPERATORS.contains(var3)) {
               throw usage("Twin.mutate", "operators", "'" + var3 + "' is not a catalog operator — " + new TreeSet<String>(OPERATORS));
            }
         }

         return new LinkedHashSet<>(var1);
      }
   }

   private static List<String> names(String var0, Object var1) {
      if (!present(var1)) {
         return null;
      } else if (var1 instanceof List var2) {
         return var2.stream().map(String::valueOf).toList();
      } else {
         throw usage("Twin.mutate", var0, "must be an array of strings");
      }
   }

   public Map<String, Object> adjudicate(String var1, String var2, Object var3) {
      if (var1 != null && var2 != null) {
         try {
            return this.adjudicating(this.withTwin(var1), var2, var3 instanceof Map var4 ? asMap(var4) : Map.of());
         } catch (RuntimeException var5) {
            return JsApi.error(var5);
         }
      } else {
         return JsApi.missing("Twin.adjudicate(rule, evidence, {disposition, note?}) requires a rule name and an entry's evidence key");
      }
   }

   private Map<String, Object> adjudicating(io.karatelabs.ext.rules.RulebookRepository.RulebookDocument var1, String var2, Map<String, Object> var3) {
      if (!(var3.get("disposition") instanceof String var5 && ADJUDICATIONS.contains(var5))) {
         throw TwinException.a(400, "ADJUDICATION_USAGE", "disposition must be one of model | system | open", Map.of("field", "disposition"));
      } else if (!EVIDENCE.matcher(var2).matches()) {
         throw unknownEvidence(var2);
      } else {
         Object var6 = var3.get("note");
         var var7 = new io.karatelabs.ext.rules.LiveRecordStore(this.manager.c(var1.d()))
            .a(var2, var5, var6 != null && !io.karatelabs.ext.rules.twin.CanonicalJson.a(var6) ? String.valueOf(var6) : null, () -> unknownEvidence(var2));
         LinkedHashMap var8 = new LinkedHashMap();
         var8.put("rule", var1.d());
         var8.put("evidence", var2);
         var8.put("disposition", var5);
         var8.put("ts", var7.get("ts"));
         return var8;
      }
   }

   public Map<String, Object> allows(String var1, Object var2) {
      if (var1 == null) {
         return JsApi.missing("Twin.allows(rule, {steps | sequenceId, log, profileCeiling?, timeoutMs?}) requires a rule name");
      } else {
         Map var3 = var2 instanceof Map var4 ? asMap(var4) : Map.of();
         Map var7 = allowsUsage(var3);
         if (var7 != null) {
            return var7;
         } else {
            try {
               return this.allowing(this.withTwin(var1), var3);
            } catch (RuntimeException var6) {
               return JsApi.error(var6);
            }
         }
      }
   }

   private static Map<String, Object> allowsUsage(Map<String, Object> var0) {
      for (Object var2 : var0.keySet()) {
         if (!ALLOWS_KEYS.contains(String.valueOf(var2))) {
            if (!"shrink".equals(var2) && !"shrinkBudget".equals(var2)) {
               if ("reset".equals(var2)) {
                  return JsApi.missing("Twin.allows reset: the hook is a process, and the log carries the t.reset exchange where the twin declares one");
               }

               if (!"against".equals(var2) && !"sequences".equals(var2) && !"detail".equals(var2)) {
                  return JsApi.missing("Twin.allows: unknown option '" + var2 + "' — Twin.allows(rule, {steps | sequenceId, log, profileCeiling?, timeoutMs?})");
               }

               return JsApi.missing(
                  "Twin.allows "
                     + var2
                     + ": it sends or selects, and a recorded history does neither — Twin.allows(rule, {steps | sequenceId, log, profileCeiling?, timeoutMs?})"
               );
            }

            return JsApi.missing("Twin.allows " + var2 + ": there is no system to re-drive an order against — minimize with Twin.live");
         }
      }

      if (present(var0.get("steps")) == present(var0.get("sequenceId"))) {
         return JsApi.missing("Twin.allows(rule, {steps | sequenceId, log, profileCeiling?, timeoutMs?}) takes exactly one of steps or sequenceId");
      } else if (var0.get("log") instanceof List var3) {
         for (int var5 = 0; var5 < var3.size(); var5++) {
            if (!(var3.get(var5) instanceof Map)) {
               return JsApi.missing("Twin.allows(rule, {steps | sequenceId, log, profileCeiling?, timeoutMs?}) log[" + var5 + "]: must be a seam entry object");
            }
         }

         return null;
      } else {
         return JsApi.missing("Twin.allows(rule, {steps | sequenceId, log, profileCeiling?, timeoutMs?}) requires log: the seam's exchange log, in seam order");
      }
   }

   private Map<String, Object> allowing(io.karatelabs.ext.rules.RulebookRepository.RulebookDocument var1, Map<String, Object> var2) {
      int var3 = bounded("Twin.allows", var2.get("profileCeiling"), "profileCeiling", 8, 1, 8);
      int var4 = bounded(
         "Twin.allows",
         var2.get("timeoutMs"),
         "timeoutMs",
         (int)io.karatelabs.ext.common.SandboxExecutor.a("KARATE_RULES_TWIN_LIVE_TIMEOUT_MS", 300000L),
         1,
         Integer.MAX_VALUE
      );
      Object var5 = var2.get("sequenceId");
      var var6 = shaped(present(var5) ? row(var1, String.valueOf(var5)).get("steps") : var2.get("steps"));
      var var7 = rows(var2.get("log"));

      Map var10;
      try (
         io.karatelabs.ext.rules.TwinRuleRuntime var8 = new io.karatelabs.ext.rules.TwinRuleRuntime(this.manager, var1.d());
         io.karatelabs.ext.common.SandboxExecutor.SandboxExecutionScope var9 = liveBox(var4);
      ) {
         var10 = var9.a(() -> graded(var1, var6, var7, var3, var8), MAPPER);
      }

      return var10;
   }

   static Map<String, Object> graded(io.karatelabs.ext.rules.RulebookRepository.RulebookDocument var0, List<Map<String, Object>> var1, List<Map<String, Object>> var2, int var3, RulebookRuntime var4) {
      TwinModel var5 = TwinModelLoader.a(var0.k());
      io.karatelabs.ext.rules.twin.Twin.RecordedExchangeObserver var6 = new io.karatelabs.ext.rules.twin.Twin.RecordedExchangeObserver(var2);
      io.karatelabs.ext.rules.twin.TwinReplayEngine.ReplayResult var7 = io.karatelabs.ext.rules.twin.TwinReplayEngine.replay(var5, var6, declared(var5, var1), var3, var4);
      var var8 = traceFault(var2, var7, var6);
      LinkedHashMap var9 = new LinkedHashMap();
      var9.put("rule", var0.d());
      var9.put("source", "trace");
      var9.put("steps", io.karatelabs.ext.rules.twin.CanonicalJson.d(var1));
      var9.put("disposition", var8 == null ? var7.a() : var8);
      var9.put("entries", var7.b().size());
      var9.put("log", var7.b());
      return var9;
   }

   private static Map<String, Object> traceFault(List<Map<String, Object>> var0, io.karatelabs.ext.rules.twin.TwinReplayEngine.ReplayResult var1, io.karatelabs.ext.rules.twin.Twin.RecordedExchangeObserver var2) {
      String var3 = String.valueOf(var1.a().get("kind"));
      boolean var4 = "FAIL".equals(var3) || "REFUSED".equals(var3) || "INVALID".equals(var3);
      if (var4 && !var2.c()) {
         return null;
      } else {
         var var5 = var1.b();

         for (int var6 = 0; var6 < Math.min(var0.size(), var5.size()); var6++) {
            String var7 = divergence((Map<String, Object>)var5.get(var6), (Map<String, Object>)var0.get(var6));
            if (var7 != null) {
               return trace(var6, var7);
            }
         }

         if (!var4) {
            if (var0.size() > var5.size()) {
               return trace(var5.size(), "the replay ended after " + var5.size() + " entries, consuming " + var2.b() + " of " + var2.a() + " exchanges");
            }

            if (var0.size() < var5.size()) {
               return trace(var0.size(), "the replay's " + phase((Map<String, Object>)var5.get(var0.size())) + " entry is missing");
            }
         }

         return var2.c() ? trace(var2.d(), var2.e()) : null;
      }
   }

   private static String divergence(Map<String, Object> var0, Map<String, Object> var1) {
      if (io.karatelabs.ext.rules.twin.CanonicalJson.c(var0.get("kind")).equals(io.karatelabs.ext.rules.twin.CanonicalJson.c(var1.get("kind")))
         && io.karatelabs.ext.rules.twin.CanonicalJson.c(var0.get("at")).equals(io.karatelabs.ext.rules.twin.CanonicalJson.c(var1.get("at")))
         && io.karatelabs.ext.rules.twin.CanonicalJson.c(var0.get("command")).equals(io.karatelabs.ext.rules.twin.CanonicalJson.c(var1.get("command")))) {
         var var2 = observed(var0);
         var var3 = observed(var1);
         if (var2 != null && var3 != null) {
            return io.karatelabs.ext.rules.twin.CanonicalJson.c(var2.get("request")).equals(io.karatelabs.ext.rules.twin.CanonicalJson.c(var3.get("request")))
               ? null
               : "expected the exchange the model derives: " + request(var2);
         } else {
            return var2 == null && var3 == null
               ? null
               : (var2 == null ? "expected an entry that sent nothing" : "expected the exchange the model derives: " + request(var2));
         }
      } else {
         return "expected the replay's " + phase(var0) + " entry";
      }
   }

   private static Map<String, Object> observed(Map<String, Object> var0) {
      return var0.get("observed") instanceof Map var1 ? asMap(var1) : null;
   }

   private static String request(Map<String, Object> var0) {
      return named(var0.get("request") instanceof Map var1 ? asMap(var1) : Map.of());
   }

   private static String named(Map<String, Object> var0) {
      return var0.get("path") == null ? io.karatelabs.ext.rules.twin.CanonicalJson.c(var0) : var0.get("method") + " " + var0.get("path");
   }

   private static String phase(Map<String, Object> var0) {
      Object var1 = var0.get("command");
      return String.valueOf(var1 == null ? var0.get("kind") : var1);
   }

   private static Map<String, Object> trace(int var0, String var1) {
      LinkedHashMap var2 = new LinkedHashMap();
      var2.put("kind", "INVALID");
      var2.put("code", "TRACE_INVALID");
      var2.put("reason", "log[" + var0 + "]: " + var1);
      var2.put("at", var0);
      return var2;
   }

   private static boolean present(Object var0) {
      return var0 != null && !io.karatelabs.ext.rules.twin.CanonicalJson.a(var0);
   }

   private io.karatelabs.ext.rules.twin.Twin.LiveLaneResult leg(io.karatelabs.ext.rules.RulebookRepository.RulebookDocument var1, TwinModel var2, RulebookRuntime var3, long var4, io.karatelabs.ext.rules.twin.Twin.LiveRecordSnapshot var6) {
      var var7 = pinned(var1);
      ArrayList var8 = new ArrayList();
      ArrayList var9 = new ArrayList();
      if (var7.isEmpty()) {
         return new io.karatelabs.ext.rules.twin.Twin.LiveLaneResult(io.karatelabs.ext.rules.twin.RequiredBehaviorVerifier.LaneCoverage.a, var8, var9);
      } else {
         io.karatelabs.ext.rules.LiveRecordStore var10 = new io.karatelabs.ext.rules.LiveRecordStore(this.manager.c(var1.d()));
         io.karatelabs.ext.rules.LiveRecordStore.LiveRecordSnapshot var11 = var6.b();
         if (var11 == null) {
            for (Map var32 : (Iterable<Map>) (Iterable<?>) (var7)) {
               var9.add(ungraded(id(var32), "CORRUPT", null));
            }

            return new io.karatelabs.ext.rules.twin.Twin.LiveLaneResult(new io.karatelabs.ext.rules.twin.RequiredBehaviorVerifier.LaneCoverage(0, 0, var7.size()), var8, var9);
         } else if (var11.a()) {
            for (Map var31 : (Iterable<Map>) (Iterable<?>) (var7)) {
               var9.add(ungraded(id(var31), "VERSION", null));
            }

            return new io.karatelabs.ext.rules.twin.Twin.LiveLaneResult(new io.karatelabs.ext.rules.twin.RequiredBehaviorVerifier.LaneCoverage(0, 0, var7.size()), var8, var9);
         } else {
            var var12 = var11.c();
            int var13 = 0;
            int var14 = 0;

            for (Map var16 : (Iterable<Map>) (Iterable<?>) (var7)) {
               String var17 = id(var16);
               Map var18 = (Map)var12.get(var17);
               if (var18 != null) {
                  if (!io.karatelabs.ext.rules.LiveRecordStore.a((Object)var18)) {
                     var9.add(ungraded(var17, "CORRUPT", null));
                  } else if (this.stale(var1, var16, var18)) {
                     var9.add(ungraded(var17, "STALE", null));
                  } else if ("RESET_HOOK_FAILED".equals(asMap(var18.get("disposition")).get("code"))) {
                     var9.add(ungraded(var17, "INVALID", "RESET_HOOK_FAILED"));
                  } else {
                     var var19 = var3.a();

                     io.karatelabs.ext.rules.twin.TwinReplayEngine.ReplayResult var20;
                     try (io.karatelabs.ext.common.SandboxExecutor.SandboxExecutionScope var21 = liveBox(var4)) {
                        var20 = var21.a(() -> io.karatelabs.ext.rules.twin.TwinReplayEngine.replay(var2, new io.karatelabs.ext.rules.twin.Twin.RecordedExchangeObserver(rows(var18.get("log"))), shaped(var16.get("steps")), ceiling(var18), var3), MAPPER);
                     } catch (RuntimeException var28) {
                        var9.add(ungraded(var17, "UNREPRODUCED", null));
                        continue;
                     }

                     if (!io.karatelabs.ext.rules.twin.CanonicalJson.c(consulted(var19, var3.a())).equals(io.karatelabs.ext.rules.twin.CanonicalJson.c(var18.get("rulebooks")))) {
                        var9.add(ungraded(var17, "UNREPRODUCED", null));
                     } else if (io.karatelabs.ext.rules.twin.CanonicalJson.c(var20.b()).equals(io.karatelabs.ext.rules.twin.CanonicalJson.c(var18.get("log")))
                        && io.karatelabs.ext.rules.twin.CanonicalJson.c(var20.a()).equals(io.karatelabs.ext.rules.twin.CanonicalJson.c(var18.get("disposition")))) {
                        String var33 = String.valueOf(var20.a().get("kind"));
                        String var22 = String.valueOf(var18.get("against"));
                        if ("PASS".equals(var33)) {
                           var13 += "mock".equals(var22) ? 1 : 0;
                           var14 += "live".equals(var22) ? 1 : 0;
                        } else if ("FAIL".equals(var33)) {
                           Object var23 = var20.a().get("evidence");
                           var var24 = var10.a(String.valueOf(var23));
                           if (var24 != null && "model".equals(var24.get("disposition"))) {
                              LinkedHashMap var34 = new LinkedHashMap();
                              var34.put("kind", "adjudicatedModel");
                              var34.put("sequenceId", var17);
                              var34.put("evidence", var23);
                              var9.add(var34);
                           } else {
                              LinkedHashMap var25 = new LinkedHashMap();
                              var25.put("sequenceId", var17);
                              var25.put("at", var20.a().get("at"));
                              var25.put("against", var22);
                              var25.put("evidence", var23);
                              var25.put("mismatch", var20.a().get("mismatch"));
                              var8.add(var25);
                           }
                        } else {
                           var9.add(ungraded(var17, var33, var20.a().get("code")));
                        }
                     } else {
                        var9.add(ungraded(var17, "CORRUPT", null));
                     }
                  }
               }
            }

            return new io.karatelabs.ext.rules.twin.Twin.LiveLaneResult(new io.karatelabs.ext.rules.twin.RequiredBehaviorVerifier.LaneCoverage(var13, var14, var7.size()), var8, var9, unobserved(var1, var12));
         }
      }
   }

   private io.karatelabs.ext.rules.twin.Twin.DerivedEvidenceResult derived(io.karatelabs.ext.rules.RulebookRepository.RulebookDocument var1, io.karatelabs.ext.rules.twin.Twin.TwinCheckContext var2, RulebookRuntime var3, String var4, long var5) {
      ArrayList var7 = new ArrayList();
      ArrayList var8 = new ArrayList();
      LinkedHashMap var9 = new LinkedHashMap();
      var9.put("walkSha", var2.a().c());
      var9.put("sequencesSha", var1.i().get("sequences"));
      var9.put("planSha", var2.d().f());
      Path var10 = this.manager.c(var1.d());

      try {
         io.karatelabs.ext.rules.WitnessBaselineStore.WitnessSnapshot var11 = new WitnessBaselineStore(var10).a();
         this.lane("witness", var11.b(), var11.a(), 3, var9, var2, var3, var4, var5, var7, var8);
      } catch (io.karatelabs.ext.rules.WitnessBaselineStore.WitnessReadException var13) {
         var8.add(ungradedRow("witness", null, "CORRUPT"));
      }

      try {
         io.karatelabs.ext.rules.TwinOrdersRepository.OrdersRecord var14 = new io.karatelabs.ext.rules.TwinOrdersRepository(var10).a();
         this.lane("orders", var14.b(), var14.a(), 1, this.ordersSelection(var1, var2, var14.b()), var2, var3, var4, var5, var7, var8);
      } catch (io.karatelabs.ext.rules.TwinOrdersRepository.OrdersRecordReadException var12) {
         var8.add(ungradedRow("orders", null, "CORRUPT"));
      }

      return new io.karatelabs.ext.rules.twin.Twin.DerivedEvidenceResult(var7, var8);
   }

   private Map<String, Object> ordersSelection(io.karatelabs.ext.rules.RulebookRepository.RulebookDocument var1, io.karatelabs.ext.rules.twin.Twin.TwinCheckContext var2, Map<String, Object> var3) {
      var var4 = this.currentHashes(var3);
      LinkedHashMap var5 = new LinkedHashMap();
      var5.put("twinSha", var1.i().get("twin"));
      var5.put("rulebooks", var4);
      var5.put("inputSha", io.karatelabs.ext.rules.twin.CanonicalJson.a(io.karatelabs.ext.rules.twin.CanonicalJson.c(var4)));
      var5.put("planSha", var2.d().f());
      var5.put("alphabetSha", io.karatelabs.ext.rules.twin.CanonicalJson.a(io.karatelabs.ext.rules.twin.CanonicalJson.c(labels(var2))));
      var5.put("constructionVersion", 1);
      return var5;
   }

   private Map<String, Object> currentHashes(Map<String, Object> var1) {
      Object var3 = (var1 == null ? null : var1.get("selection")) instanceof Map var4 ? var4.get("rulebooks") : null;
      LinkedHashMap var8 = new LinkedHashMap();
      if (var3 instanceof Map var5) {
         for (Object var7 : var5.keySet()) {
            var8.put(String.valueOf(var7), this.digest(String.valueOf(var7)));
         }
      }

      return var8;
   }

   private String digest(String var1) {
      try {
         return this.manager.d(var1).d().c();
      } catch (RuntimeException var3) {
         return null;
      }
   }

   private static List<String> labels(io.karatelabs.ext.rules.twin.Twin.TwinCheckContext var0) {
      ArrayList var1 = new ArrayList();

      for (Map var3 : io.karatelabs.ext.rules.twin.SequenceExplorer.a(var0.b(), var0.d())) {
         Object var4 = var3.get("args");
         var1.add(io.karatelabs.ext.rules.twin.SequenceExplorer.a(String.valueOf(var3.get("command")), var4 instanceof Map var5 ? asMap(var5) : Map.of()));
      }

      return var1;
   }

   private void lane(
      String var1,
      Map<String, Object> var2,
      Integer var3,
      int var4,
      Map<String, Object> var5,
      io.karatelabs.ext.rules.twin.Twin.TwinCheckContext var6,
      RulebookRuntime var7,
      String var8,
      long var9,
      List<Map<String, Object>> var11,
      List<Map<String, Object>> var12
   ) {
      if (var3 != null) {
         if (var2 != null && var3 == var4) {
            boolean var13 = !stands(var5, var2.get("selection"));

            for (Map var15 : rows(var2.get("orders".equals(var1) ? "sequences" : "obligations"))) {
               String var16 = String.valueOf(var15.get("orders".equals(var1) ? "id" : "key"));
               if (contradicts(var1, var15)) {
                  String var17 = var13
                     ? "STALE"
                     : (
                        !Boolean.TRUE.equals(var15.get("confirmed"))
                           ? "UNCONFIRMED"
                           : (
                              var8 == null || var2.get("sutRevision") == null
                                 ? "NOREVISION"
                                 : (!var8.equals(var2.get("sutRevision")) ? "REVISION" : refolds(var6.b(), var15, var7, var9))
                           )
                     );
                  if (var17 != null) {
                     var12.add(ungradedRow(var1, var16, var17));
                  } else {
                     LinkedHashMap var18 = new LinkedHashMap();
                     var18.put("lane", var1);
                     var18.put("id", var16);
                     var18.put("at", var15.get("at") == null ? failedAt(var15) : var15.get("at"));
                     var18.put("against", var2.get("against"));
                     var18.put("mismatch", mismatch(var15));
                     var11.add(var18);
                  }
               }
            }
         } else {
            var12.add(ungradedRow(var1, null, "VERSION"));
         }
      }
   }

   private static boolean contradicts(String var0, Map<String, Object> var1) {
      return "orders".equals(var0)
         ? "FAIL".equals(disposition(var1).get("kind")) || "UNSTABLE".equals(disposition(var1).get("kind"))
         : "refuted".equals(var1.get("credit"));
   }

   private static Map<String, Object> disposition(Map<String, Object> var0) {
      return var0.get("disposition") instanceof Map var1 ? asMap(var1) : Map.of();
   }

   private static Object failedAt(Map<String, Object> var0) {
      return disposition(var0).get("at");
   }

   private static Object mismatch(Map<String, Object> var0) {
      return var0.get("mismatch") == null ? disposition(var0).get("mismatch") : var0.get("mismatch");
   }

   private static boolean stands(Map<String, Object> var0, Object var1) {
      if (var1 instanceof Map var2) {
         for (Entry var4 : var0.entrySet()) {
            if (!io.karatelabs.ext.rules.twin.CanonicalJson.c(var4.getValue()).equals(io.karatelabs.ext.rules.twin.CanonicalJson.c(var2.get(var4.getKey())))) {
               return false;
            }
         }

         return true;
      } else {
         return false;
      }
   }

   private static String refolds(TwinModel var0, Map<String, Object> var1, RulebookRuntime var2, long var3) {
      var var5 = rows(var1.get("log"));
      if (var5.isEmpty()) {
         return "CORRUPT";
      } else {
         io.karatelabs.ext.rules.twin.TwinReplayEngine.ReplayResult var6;
         try (io.karatelabs.ext.common.SandboxExecutor.SandboxExecutionScope var7 = liveBox(var3)) {
            var6 = var7.a(() -> io.karatelabs.ext.rules.twin.TwinReplayEngine.replay(var0, new io.karatelabs.ext.rules.twin.Twin.RecordedExchangeObserver(var5), shaped(var1.get("steps")), 8, var2), MAPPER);
         } catch (RuntimeException var12) {
            return "UNREPRODUCED";
         }

         if (!io.karatelabs.ext.rules.twin.CanonicalJson.c(var6.b()).equals(io.karatelabs.ext.rules.twin.CanonicalJson.c(var5))) {
            return "UNREPRODUCED";
         } else {
            return "FAIL".equals(var6.a().get("kind"))
                  && io.karatelabs.ext.rules.twin.CanonicalJson.c(var6.a().get("mismatch")).equals(io.karatelabs.ext.rules.twin.CanonicalJson.c(mismatch(var1)))
               ? null
               : "CORRUPT";
         }
      }
   }

   private static Map<String, Object> ungradedRow(String var0, String var1, String var2) {
      LinkedHashMap var3 = new LinkedHashMap();
      var3.put("kind", "derivedUngraded");
      var3.put("lane", var0);
      if (var1 != null) {
         var3.put("id", var1);
      }

      var3.put("reason", var2);
      return var3;
   }

   private static String revision(String var0, Map<String, Object> var1) {
      Object var2 = var1.get("sutRevision");
      if (var2 != null && !io.karatelabs.ext.rules.twin.CanonicalJson.a(var2)) {
         if (var2 instanceof String var3 && !var3.isBlank()) {
            return var3;
         } else {
            throw usage(var0, "sutRevision", "must be a non-empty string naming the service this run was taken against — an image digest, a commit");
         }
      } else {
         return null;
      }
   }

   private static List<Map<String, Object>> unobserved(io.karatelabs.ext.rules.RulebookRepository.RulebookDocument var0, Map<String, Map<String, Object>> var1) {
      var var2 = io.karatelabs.ext.rules.twin.ObservationReconciler.a(var0.k(), var1);
      return var2 != null && var2.get("findings") instanceof List var3 ? var3 : List.of();
   }

   private boolean stale(io.karatelabs.ext.rules.RulebookRepository.RulebookDocument var1, Map<String, Object> var2, Map<String, Object> var3) {
      if (String.valueOf(var1.i().get("twin")).equals(var3.get("twinSha")) && io.karatelabs.ext.rules.RulebookRepository.a(var2.get("steps")).equals(var3.get("inputSha"))) {
         Map var4 = (Map)var3.get("rulebooks");

         for (Entry var6 : (Iterable<Entry>) (Iterable<?>) (var4.entrySet())) {
            Object var7 = var6.getValue() instanceof Map var8 ? var8.get("hash") : null;

            try {
               if (!this.manager.d(String.valueOf(var6.getKey())).d().c().equals(var7)) {
                  return true;
               }
            } catch (io.karatelabs.ext.rules.RuleApiException var10) {
               return true;
            }
         }

         return false;
      } else {
         return true;
      }
   }

   private static Map<String, Object> consulted(Map<String, io.karatelabs.ext.rules.twin.RulebookRuntime.RulebookUsageStats> var0, Map<String, io.karatelabs.ext.rules.twin.RulebookRuntime.RulebookUsageStats> var1) {
      LinkedHashMap var2 = new LinkedHashMap();

      for (Entry var4 : var1.entrySet()) {
         io.karatelabs.ext.rules.twin.RulebookRuntime.RulebookUsageStats var5 = (io.karatelabs.ext.rules.twin.RulebookRuntime.RulebookUsageStats)var0.get(var4.getKey());
         int var6 = ((io.karatelabs.ext.rules.twin.RulebookRuntime.RulebookUsageStats)var4.getValue()).b() - (var5 == null ? 0 : var5.b());
         int var7 = ((io.karatelabs.ext.rules.twin.RulebookRuntime.RulebookUsageStats)var4.getValue()).c() - (var5 == null ? 0 : var5.c());
         if (var6 != 0 || var7 != 0) {
            LinkedHashMap var8 = new LinkedHashMap();
            var8.put("hash", ((io.karatelabs.ext.rules.twin.RulebookRuntime.RulebookUsageStats)var4.getValue()).a());
            var8.put("calls", var6);
            var8.put("threw", var7);
            var2.put((String)var4.getKey(), var8);
         }
      }

      return var2;
   }

   private static List<Map<String, Object>> selected(io.karatelabs.ext.rules.RulebookRepository.RulebookDocument var0, List<Map<String, Object>> var1, Object var2) {
      var var3 = new ArrayList<>(pinned(var0));
      var3.addAll(var1);
      if (var2 != null && !io.karatelabs.ext.rules.twin.CanonicalJson.a(var2)) {
         if (var2 instanceof List var4) {
            LinkedHashSet var5 = new LinkedHashSet();
            ArrayList var6 = new ArrayList();

            for (Object var8 : var4) {
               var var9 = row(var3, var0.d(), String.valueOf(var8));
               if (var5.add(id(var9))) {
                  var6.add(var9);
               }
            }

            return var6;
         } else {
            throw usage("sequences", "must be an array of sequence ids");
         }
      } else {
         return pinned(var0);
      }
   }

   private static List<Map<String, Object>> pinned(io.karatelabs.ext.rules.RulebookRepository.RulebookDocument var0) {
      return var0.l() == null ? List.of() : rows(var0.l().get("sequences"));
   }

   private static List<Map<String, Object>> declared(TwinModel var0, List<Map<String, Object>> var1) {
      for (int var2 = 0; var2 < var1.size(); var2++) {
         Object var3 = ((Map)var1.get(var2)).get("command");
         io.karatelabs.ext.rules.twin.TwinModel.CommandModel var4 = var3 instanceof String var5 ? var0.f().get(var5) : null;
         if (var4 == null) {
            throw TwinException.a("steps[" + var2 + "].command: not a declared command", Map.of("at", var2, "command", String.valueOf(var3)));
         }

         if (var4.k() == null) {
            throw TwinException.a(
               400,
               "LIVE_UNDECLARED",
               "command '" + var4.a() + "' declares no request — a live replay has no other request source",
               Map.of("at", var2, "command", var4.a())
            );
         }
      }

      return var1;
   }

   private static void creating(TwinModel var0, String var1, String var2, ResetHookRunner var3, String var4) {
      if (var0.j() == null && var3 == null) {
         if (!"mock".equals(var4)) {
            throw TwinException.a(
               400,
               "RESET_REQUIRED",
               var1
                  + " replays one sequence per "
                  + var2
                  + " against a live service, whose own creation isolates nothing the engine can verify — declare t.reset in twin.js or pass reset: {command: [argv]}",
               Map.of("field", "reset")
            );
         } else {
            for (io.karatelabs.ext.rules.twin.TwinModel.CommandModel var6 : var0.c()) {
               if (var6.q() != null && var6.q().d()) {
                  return;
               }
            }

            throw TwinException.a(
               400,
               "RESET_REQUIRED",
               var1
                  + " replays one sequence per "
                  + var2
                  + ", and this twin binds no creating command, so no "
                  + var2
                  + " opens a fresh world — declare t.reset in twin.js or pass reset: {command: [argv]}",
               Map.of("field", "reset")
            );
         }
      }
   }

   private static void fresh(TwinModel var0, String var1, String var2, String var3, List<Map<String, Object>> var4) {
      if (!var4.isEmpty()) {
         String var5 = String.valueOf(((Map)var4.get(0)).get("command"));
         io.karatelabs.ext.rules.twin.TwinModel.CommandModel var6 = var0.f().get(var5);
         if (var6 == null || var6.q() == null || !var6.q().d()) {
            throw TwinException.a(
               400,
               "RESET_REQUIRED",
               var1
                  + " replays one sequence per "
                  + var2
                  + ", and "
                  + var2
                  + " "
                  + var3
                  + " opens with '"
                  + var5
                  + "', which starts no fresh world — declare t.reset in twin.js or pass reset: {command: [argv]}",
               Map.of("field", "reset")
            );
         }
      }
   }

   private static io.karatelabs.ext.common.SandboxExecutor.SandboxExecutionScope liveBox(long var0) {
      return new io.karatelabs.ext.common.SandboxExecutor.SandboxExecutionScope("twin-live", var0);
   }

   private static Map<String, Object> ungraded(String var0, String var1, Object var2) {
      LinkedHashMap var3 = new LinkedHashMap();
      var3.put("kind", "liveUngraded");
      var3.put("sequenceId", var0);
      var3.put("reason", var1);
      if (var2 != null) {
         var3.put("code", var2);
      }

      return var3;
   }

   private static TwinException unknownEvidence(String var0) {
      return TwinException.a(
         404,
         "ADJUDICATION_UNKNOWN",
         "no entry '" + var0 + "' in the current live record — a key nobody can find is a typo or a run since replaced",
         Map.of("evidence", var0)
      );
   }

   private static int ceiling(Map<String, Object> var0) {
      return ((Number)var0.get("profileCeiling")).intValue();
   }

   private static String id(Map<String, Object> var0) {
      return String.valueOf(var0.get("_id"));
   }

   private static String required(Map<String, Object> var0, String var1) {
      if (var0.get(var1) instanceof String var3 && !var3.isBlank()) {
         return var3;
      } else {
         throw usage(var1, "is required");
      }
   }

   private static int bounded(Map<String, Object> var0, String var1, int var2, int var3, int var4) {
      return bounded(var0.get(var1), var1, var2, var3, var4);
   }

   static int bounded(Object var0, String var1, int var2, int var3, int var4) {
      return bounded("Twin.live", var0, var1, var2, var3, var4);
   }

   static int bounded(String var0, Object var1, String var2, int var3, int var4, int var5) {
      if (var1 != null && !io.karatelabs.ext.rules.twin.CanonicalJson.a(var1)) {
         if (var1 instanceof Number var6 && var6.doubleValue() == Math.rint(var6.doubleValue()) && var6.longValue() >= var4 && var6.longValue() <= var5) {
            return (int)var6.longValue();
         } else {
            throw usage(var0, var2, "must be an integer in " + var4 + ".." + var5);
         }
      } else {
         return var3;
      }
   }

   private static boolean flag(Map<String, Object> var0, String var1) {
      Object var2 = var0.get(var1);
      if (var2 == null || io.karatelabs.ext.rules.twin.CanonicalJson.a(var2)) {
         return false;
      } else if (var2 instanceof Boolean var3) {
         return var3;
      } else {
         throw usage(var1, "must be true or false");
      }
   }

   static TwinException usage(String var0, String var1) {
      return usage("Twin.live", var0, var1);
   }

   static TwinException usage(String var0, String var1, String var2) {
      return TwinException.a(400, "LIVE_USAGE", var0 + " " + var1 + " " + var2, Map.of("field", var1));
   }

   private static List<Map<String, Object>> rows(Object var0) {
      return var0 instanceof List var1 ? var1 : List.of();
   }

   public Map<String, Object> section(String var1) {
      return this.section(var1, null, null);
   }

   public Map<String, Object> section(String var1, Object var2, Object var3) {
      io.karatelabs.ext.rules.RulebookRepository.RulebookDocument var4 = this.manager.b(var1);
      LinkedHashMap var5 = new LinkedHashMap();
      var5.put("rule", var4.d());
      var5.put("twin", var4.k() != null);
      if (var4.k() == null) {
         return var5;
      } else {
         Map var6 = null;
         LinkedHashMap var7 = null;
         if (present(var2) || present(var3)) {
            LinkedHashMap var8 = new LinkedHashMap();
            var8.put("sutRevision", var3);
            revision("Rule.report", var8);
            var6 = present(var2) ? Map.of("plan", var2) : null;
            var7 = new LinkedHashMap();
            if (present(var2)) {
               var7.put("plan", var2);
            }

            if (present(var3)) {
               var7.put("sutRevision", var3);
            }
         }

         io.karatelabs.ext.rules.twin.Twin.LiveRecordSnapshot var14 = io.karatelabs.ext.rules.twin.Twin.LiveRecordSnapshot.a(this.manager.c(var4.d()));
         Map var9 = null;

         try {
            var5.put("explore", this.exploring(var4, var6, null));
            var9 = this.checking(var4, var7, null, io.karatelabs.ext.common.SandboxExecutor.a("KARATE_RULES_TWIN_LIVE_TIMEOUT_MS", 300000L), var14);
            var5.put("check", var9);
         } catch (RuntimeException var13) {
            if (!(var13 instanceof io.karatelabs.ext.common.jsapi.JsWireException var11)) {
               throw var13;
            }

            LinkedHashMap var12 = new LinkedHashMap();
            var12.put("code", var11.b());
            var12.put("message", var11.a());
            var12.put("status", var11.c());
            var5.put("error", var12);
         }

         if (var14.b() != null) {
            var var10 = var14.a();
            var var16 = io.karatelabs.ext.rules.twin.TwinReplayEngine.summarizeRefusals(var10);
            if (!var16.isEmpty()) {
               var5.put("wireRefusals", var16);
            }

            var var18 = io.karatelabs.ext.rules.twin.ObservationReconciler.a(var4.k(), var10);
            if (var18 != null) {
               var5.put("observation", var18);
            }
         }

         Map var15 = io.karatelabs.ext.rules.twin.FreshnessAnnotator.a(pinned(var4), var14.a(), var9);
         if (var15 != null) {
            var5.put("live", var15);
         }

         var var17 = var4.l();
         var5.put("sequences", var17 == null ? List.of() : var17.get("sequences"));
         if (var17 != null) {
            var5.put("provenance", var17.get("provenance"));
         }

         return var5;
      }
   }

   public Map<String, Object> lifecycle(String var1, Object var2, String var3, List<String> var4) {
      return this.lifecycle(var1, var2, var3, var4, List.of());
   }

   public Map<String, Object> lifecycle(String var1, Object var2, String var3, List<String> var4, List<String> var5) {
      io.karatelabs.ext.rules.RulebookRepository.RulebookDocument var6;
      try {
         var6 = this.manager.b(var1);
      } catch (RuntimeException var13) {
         return unavailable(var1, var13);
      }

      if (var6.k() == null) {
         return io.karatelabs.ext.rules.twin.ProjectionReportBuilder.a(var1, "TWIN_NOT_FOUND", "rulebook '" + var1 + "' has no twin.js");
      } else {
         try {
            Map var9;
            try (
               io.karatelabs.ext.rules.TwinRuleRuntime var7 = new io.karatelabs.ext.rules.TwinRuleRuntime(this.manager, var6.d());
               io.karatelabs.ext.common.SandboxExecutor.SandboxExecutionScope var8 = walkBox();
            ) {
               var9 = var8.a(
                  () -> {
                     TwinModel var8x = TwinModelLoader.a(var6.k());
                     if (var8x.k() == null) {
                        return io.karatelabs.ext.rules.twin.ProjectionReportBuilder.a(var1, "BINDING_MISSING", "the twin declares no transport binding, so no operation joins a command");
                     } else {
                        TwinWalkPlan var9x = io.karatelabs.ext.rules.twin.TwinWalkPlan.a(var2, var8x, io.karatelabs.ext.rules.twin.TwinExecutionLimits.a, RequiredBehaviorVerifier.empty(var8x).rowIds());
                        io.karatelabs.ext.rules.twin.TwinWalker.WalkExecution var10 = TwinWalker.b(var8x, var9x, io.karatelabs.ext.rules.twin.TwinExecutionLimits.a, var7);
                        return io.karatelabs.ext.rules.twin.ProjectionReportBuilder.a(
                           var6.d(),
                           var8x,
                           var10,
                           var9x,
                           rows(this.manager.a(var6, var8x).get("sequencePairs")),
                           pinned(var6),
                           var7,
                           this.manager.c(var6.d()),
                           var3,
                           var4,
                           var5
                        );
                     }
                  },
                  MAPPER
               );
            }

            return var9;
         } catch (RuntimeException var16) {
            return unavailable(var1, var16);
         }
      }
   }

   public Map<String, Object> lifecycle(String var1) {
      return this.lifecycle(var1, null);
   }

   public Map<String, Object> lifecycle(String var1, Object var2) {
      String var3 = "";
      ArrayList var4 = new ArrayList();

      try {
         io.karatelabs.ext.rules.RulebookRepository.RulebookDocument var5 = this.manager.b(var1);
         TwinModel var6 = var5.k() == null ? null : TwinModelLoader.a(var5.k());
         if (var6 != null && var6.k() != null) {
            io.karatelabs.ext.rules.twin.OpenApiProjector.OpenApiProjection var7 = io.karatelabs.ext.rules.twin.OpenApiProjector.a(var6, var5.d());
            if (!var7.a()) {
               var var13 = var7.f().get(0);
               return io.karatelabs.ext.rules.twin.ProjectionReportBuilder.a(var5.d(), String.valueOf(var13.get("code")), String.valueOf(var13.get("reason")));
            }

            var3 = var7.d();

            for (Object var9 : ((Map)var7.b().get("paths")).values()) {
               for (Object var11 : ((Map)var9).values()) {
                  var4.add(String.valueOf(((Map)var11).get("operationId")));
               }
            }
         }
      } catch (RuntimeException var12) {
         return unavailable(var1, var12);
      }

      return this.lifecycle(var1, var2, var3, var4);
   }

   public Object plan(String var1, Object var2, Path var3) {
      Object var4 = io.karatelabs.ext.rules.twin.TwinWalkPlan.a(var2, var3);
      if (var4 == null) {
         return null;
      } else {
         io.karatelabs.ext.rules.RulebookRepository.RulebookDocument var5 = this.manager.b(var1);

         TwinModel var6;
         try {
            var6 = var5.k() == null ? null : TwinModelLoader.a(var5.k());
         } catch (RuntimeException var8) {
            return var4;
         }

         if (var6 != null) {
            io.karatelabs.ext.rules.twin.TwinWalkPlan.a(var4, var6, io.karatelabs.ext.rules.twin.TwinExecutionLimits.a, RequiredBehaviorVerifier.empty(var6).rowIds());
         }

         return var4;
      }
   }

   private static Map<String, Object> unavailable(String var0, RuntimeException var1) {
      if (var1 instanceof io.karatelabs.ext.common.jsapi.JsWireException var2) {
         return io.karatelabs.ext.rules.twin.ProjectionReportBuilder.a(var0, var2.b(), var2.a());
      } else {
         throw var1;
      }
   }

   public Map<String, Object> run(String var1, Object var2, Object var3) {
      return this.run(var1, var2, var3, io.karatelabs.ext.common.SandboxExecutor.a("KARATE_RULES_TWIN_TIMEOUT_MS", 30000L));
   }

   Map<String, Object> run(String var1, Object var2, Object var3, long var4) {
      if (var1 != null && (var2 instanceof String || var2 instanceof List)) {
         try {
            io.karatelabs.ext.rules.RulebookRepository.RulebookDocument var6 = this.withTwin(var1);

            Map var11;
            try (
               io.karatelabs.ext.rules.TwinRuleRuntime var7 = new io.karatelabs.ext.rules.TwinRuleRuntime(this.manager, var6.d());
               io.karatelabs.ext.common.SandboxExecutor.SandboxExecutionScope var8 = new io.karatelabs.ext.common.SandboxExecutor.SandboxExecutionScope("twin-run", var4);
            ) {
               long var9 = System.nanoTime() + var8.a() * 1000000L;
               var11 = var8.a(() -> this.replay(var6, var2, trace(var3), var9, var7), MAPPER);
            }

            return var11;
         } catch (RuntimeException var16) {
            return JsApi.error(var16);
         }
      } else {
         return JsApi.missing("Twin.run(rule, sequenceIdOrSteps) requires a rule name and a sequence id or an array of steps");
      }
   }

   private Map<String, Object> replay(io.karatelabs.ext.rules.RulebookRepository.RulebookDocument var1, Object var2, boolean var3, long var4, RulebookRuntime var6) {
      Map var7 = var2 instanceof String var8 ? row(var1, var8) : null;
      var var17 = shaped(var7 == null ? var2 : var7.get("steps"));
      Map var9 = var7 == null ? null : declared(var7, var17.size());
      TwinModel var10 = TwinModelLoader.a(var1.k());
      ReplayTrace var11 = TwinWalker.a(var10, var17, var6);
      LinkedHashMap var12 = new LinkedHashMap();
      var12.put("rule", var1.d());
      if (var7 != null) {
         var12.put("sequenceId", var7.get("_id"));
      }

      ArrayList var13 = new ArrayList(var11.a().size());

      for (io.karatelabs.ext.rules.twin.ReplayTrace.ReplayStep var15 : var11.a()) {
         var13.add(step(var15, var3));
      }

      var12.put("steps", var13);
      var12.put("final", var11.b());
      var12.put("expect", expect(var9, var11));
      Map var18 = var7 != null && var7.get("_from") instanceof Map var19 ? origin(var10, var19, var4, var6, this.manager.a()) : null;
      var12.put("provenance", provenance(var1, var6.a()));
      if (var18 != null) {
         var12.put("origin", var18);
      }

      return var12;
   }

   private static Map<String, Object> origin(TwinModel var0, Map<String, Object> var1, long var2, RulebookRuntime var4, Path var5) {
      var var6 = new LinkedHashMap<>(var1);
      if ("play".equals(var1.get("kind"))) {
         var6.put("quarantine", io.karatelabs.ext.rules.twin.PlayRecordStore.a(var5, var1));
         return var6;
      } else if (!"explore".equals(var1.get("kind"))) {
         return var6;
      } else {
         long var7 = (var2 - System.nanoTime()) / 1000000000L;
         if (var7 < 1L) {
            var6.put("checked", false);
            var6.put("reason", "ORIGIN_NOTCHECKED: under a second of the replay's budget remained");
            return var6;
         } else {
            io.karatelabs.ext.rules.twin.TwinExecutionLimits var9 = new io.karatelabs.ext.rules.twin.TwinExecutionLimits(
               io.karatelabs.ext.rules.twin.TwinExecutionLimits.a.a(),
               io.karatelabs.ext.rules.twin.TwinExecutionLimits.a.b(),
               io.karatelabs.ext.rules.twin.TwinExecutionLimits.a.c(),
               (int)Math.min((long)io.karatelabs.ext.rules.twin.TwinExecutionLimits.a.d(), var7)
            );

            TwinWalkPlan var10;
            try {
               var10 = io.karatelabs.ext.rules.twin.TwinWalkPlan.a(var1.get("plan"), var0, var9, Set.of());
            } catch (TwinException var17) {
               var6.put("checked", false);
               var6.put("reason", "PLAN_INVALID: " + var17.getMessage());
               return var6;
            }

            TwinWalkResult var11 = TwinWalker.a(var0, var10, var9, var4);
            if (!"exhausted".equals(var11.j().a())) {
               var6.put("checked", false);
               var6.put("reason", "ORIGIN_NOTCHECKED: the re-run tripped " + var11.j().a() + " inside the replay's budget");
               return var6;
            } else {
               String var12 = var11.c();
               var6.put("checked", true);
               var6.put("current", var12);
               var6.put("differs", !var12.equals(var1.get("walk")));
               Object var13 = var1.get("counterexample");
               if (var13 != null && !io.karatelabs.ext.rules.twin.CanonicalJson.a(var13)) {
                  boolean var14 = false;

                  for (io.karatelabs.ext.rules.twin.TwinWalkResult.Counterexample var16 : var11.m()) {
                     var14 |= var16.a().equals(var13);
                  }

                  var6.put("found", var14);
               }

               return var6;
            }
         }
      }
   }

   public Map<String, Object> next(String var1, Object var2) {
      return this.next(var1, var2, io.karatelabs.ext.common.SandboxExecutor.a("KARATE_RULES_TWIN_TIMEOUT_MS", 30000L));
   }

   Map<String, Object> next(String var1, Object var2, long var3) {
      if (var1 != null && (var2 instanceof String || var2 instanceof List || var2 instanceof Map)) {
         if (!(var2 instanceof Map var5 && (var5.size() != 1 || !(var5.get("record") instanceof String)))) {
            try {
               io.karatelabs.ext.rules.RulebookRepository.RulebookDocument var16 = this.withTwin(var1);
               List<Map<String, Object>> var6;
               if (var2 instanceof Map var7) {
                  var6 = new ArrayList();

                  for (Map var9 : io.karatelabs.ext.rules.twin.PlayRecordStore.a(this.manager.a(), (String)var7.get("record")).d()) {
                     var6.add(Map.of("command", var9.get("command"), "args", var9.get("args")));
                  }
               } else {
                  var6 = shaped(var2 instanceof String var18 ? row(var16, var18).get("steps") : var2);
               }

               Map var20;
               try (
                  io.karatelabs.ext.rules.TwinRuleRuntime var17 = new io.karatelabs.ext.rules.TwinRuleRuntime(this.manager, var16.d());
                  io.karatelabs.ext.common.SandboxExecutor.SandboxExecutionScope var19 = new io.karatelabs.ext.common.SandboxExecutor.SandboxExecutionScope("twin-run", var3);
               ) {
                  var20 = var19.a(() -> enabled(var16, var6, var17), MAPPER);
               }

               return var20;
            } catch (RuntimeException var15) {
               return JsApi.error(var15);
            }
         } else {
            return JsApi.missing("Twin.next(rule, history) requires a rule name and a history: a sequence id, an array of steps, or {record: id}");
         }
      } else {
         return JsApi.missing("Twin.next(rule, history) requires a rule name and a history: a sequence id, an array of steps, or {record: id}");
      }
   }

   private static Map<String, Object> enabled(io.karatelabs.ext.rules.RulebookRepository.RulebookDocument var0, List<Map<String, Object>> var1, RulebookRuntime var2) {
      TwinModel var3 = TwinModelLoader.a(var0.k());
      Object var4 = TwinWalker.a(var3, var1, var2).b();
      ArrayList var5 = new ArrayList(var3.c().size());

      for (io.karatelabs.ext.rules.twin.TwinModel.CommandModel var7 : var3.c()) {
         boolean var8 = CommandExecutor.b(var3, var4, var7, null);
         LinkedHashMap var9 = new LinkedHashMap();
         var9.put("command", var7.a());
         var9.put("enabled", var8);
         String var10 = var7.o().get("guard");
         if (var10 != null) {
            var9.put("reason", var10);
         }

         Map var11 = var8 ? knownArgs(var7) : null;
         if (var11 != null) {
            io.karatelabs.ext.rules.twin.CommandExecutor.CommandResult var12 = CommandExecutor.a(var3, var4, var7, var11, var2);
            if ("refused".equals(var12.b()) && "apply".equals(var12.c())) {
               LinkedHashMap var13 = new LinkedHashMap();
               var13.put("stage", "apply");
               var13.put("reason", var12.d());
               List var14 = var7.q() == null ? null : var7.q().e().b().get(var12.d());
               if (var14 != null && !var14.isEmpty()) {
                  var13.put("code", ((io.karatelabs.ext.rules.twin.TwinModel.WireRefusal)var14.get(0)).b());
               }

               var9.put("refusal", var13);
            }
         }

         var5.add(var9);
      }

      LinkedHashMap var15 = new LinkedHashMap();
      var15.put("rule", var0.d());
      var15.put("at", var1.size());
      var15.put("world", var4);
      var15.put("commands", var5);
      return var15;
   }

   private static Map<String, Object> knownArgs(io.karatelabs.ext.rules.twin.TwinModel.CommandModel var0) {
      if (var0.g() == null) {
         return var0.f().size() == 1 ? var0.f().get(0) : null;
      } else {
         LinkedHashMap var1 = new LinkedHashMap();

         for (io.karatelabs.ext.rules.twin.TwinModel.ArgumentAxis var3 : var0.g()) {
            if (var0.r() == null || !var0.r().containsKey(var3.a())) {
               return null;
            }

            var1.put(var3.a(), var0.r().get(var3.a()));
         }

         return var1;
      }
   }

   private static Map<String, Object> row(io.karatelabs.ext.rules.RulebookRepository.RulebookDocument var0, String var1) {
      return row(pinned(var0), var0.d(), var1);
   }

   private static Map<String, Object> row(List<Map<String, Object>> var0, String var1, String var2) {
      for (Map var4 : var0) {
         if (var2.equals(var4.get("_id"))) {
            return var4;
         }
      }

      throw new io.karatelabs.ext.rules.RuleApiException(404, "SEQUENCE_NOT_FOUND", "no sequence " + var2 + " in rule " + var1);
   }

   private static List<Map<String, Object>> shaped(Object var0) {
      if (var0 instanceof List var1) {
         ArrayList var2 = new ArrayList(var1.size());

         for (int var3 = 0; var3 < var1.size(); var3++) {
            if (!(var1.get(var3) instanceof Map var4)) {
               throw TwinException.a("steps[" + var3 + "]: must be an object", Map.of("at", var3));
            }

            var2.add(var4);
         }

         return var2;
      } else {
         throw TwinException.a("steps: must be an array", (Map<String, Object>)null);
      }
   }

   private static Map<String, Object> step(io.karatelabs.ext.rules.twin.ReplayTrace.ReplayStep var0, boolean var1) {
      LinkedHashMap var2 = new LinkedHashMap();
      var2.put("command", var0.a());
      var2.put("args", var0.b());
      var2.put("outcome", var0.c());
      if (var0.d() != null) {
         var2.put("stage", var0.d());
      }

      if (var0.e() != null) {
         var2.put("reason", var0.e());
      }

      if (var0.f() != null) {
         var2.put("detail", var0.f());
      }

      var2.put("postcondition", var0.g());
      ArrayList var3 = new ArrayList(var0.h().size());

      for (io.karatelabs.ext.rules.twin.CommandExecutor.InvariantEvaluation var5 : var0.h()) {
         LinkedHashMap var6 = new LinkedHashMap();
         var6.put("name", var5.a());
         var6.put("status", var5.b());
         var3.add(var6);
      }

      var2.put("invariants", var3);
      if (var1) {
         var2.put("world", var0.i());
      }

      return var2;
   }

   private static Map<String, Object> declared(Map<String, Object> var0, int var1) {
      Object var2 = var0.get("_expect");
      if (var2 == null) {
         return null;
      } else if (!(var2 instanceof Map var3)) {
         throw TwinException.a("_expect: must be an object", (Map<String, Object>)null);
      } else {
         io.karatelabs.ext.rules.twin.ExpectationEvaluator.a(var3, var1);
         return var3;
      }
   }

   private static Map<String, Object> expect(Map<String, Object> var0, ReplayTrace var1) {
      LinkedHashMap var2 = new LinkedHashMap();
      var2.put("checked", var0 != null);
      var2.put("mismatches", var0 == null ? List.of() : io.karatelabs.ext.rules.twin.ExpectationEvaluator.a(var0, var1));
      return var2;
   }

   private static Map<String, Object> provenance(io.karatelabs.ext.rules.RulebookRepository.RulebookDocument var0, Map<String, io.karatelabs.ext.rules.twin.RulebookRuntime.RulebookUsageStats> var1) {
      var var2 = io.karatelabs.ext.rules.RulebookRepository.a(var0);
      Object var3 = var0.l() == null ? null : var0.l().get("provenance");
      LinkedHashMap var4 = new LinkedHashMap();
      if (var3 != null) {
         var4.put("file", var3);
      }

      var4.put("current", var2);
      var4.put("differs", var3 != null && !var3.equals(var2));
      if (!var1.isEmpty()) {
         LinkedHashMap var5 = new LinkedHashMap();

         for (Entry var7 : var1.entrySet()) {
            LinkedHashMap var8 = new LinkedHashMap();
            var8.put("hash", ((io.karatelabs.ext.rules.twin.RulebookRuntime.RulebookUsageStats)var7.getValue()).a());
            var8.put("calls", ((io.karatelabs.ext.rules.twin.RulebookRuntime.RulebookUsageStats)var7.getValue()).b());
            var8.put("threw", ((io.karatelabs.ext.rules.twin.RulebookRuntime.RulebookUsageStats)var7.getValue()).c());
            var5.put((String)var7.getKey(), var8);
         }

         var4.put("rulebooks", var5);
      }

      return var4;
   }

   private static boolean trace(Object var0) {
      return var0 instanceof Map var1 && Boolean.TRUE.equals(var1.get("trace"));
   }

   @Override
   public void close() {
   }

   private record TwinConstructionResult(TwinModel a, io.karatelabs.ext.rules.twin.SequenceExplorer.SequenceExplorationResult b, Map<String, Object> c, boolean d) {

      private TwinConstructionResult(TwinModel a, io.karatelabs.ext.rules.twin.SequenceExplorer.SequenceExplorationResult b, Map<String, Object> c, boolean d) {
         this.a = a;
         this.b = b;
         this.c = c;
         this.d = d;
      }
   }

   private record TwinOrderExecutionResult(List<Map<String, Object>> a, int b, Map<String, Object> c) {

      private TwinOrderExecutionResult(List<Map<String, Object>> a, int b, Map<String, Object> c) {
         this.a = a;
         this.b = b;
         this.c = c;
      }
   }

   private record DerivedEvidenceResult(List<Map<String, Object>> a, List<Map<String, Object>> b) {

      private DerivedEvidenceResult(List<Map<String, Object>> a, List<Map<String, Object>> b) {
         this.a = a;
         this.b = b;
      }
   }

   private record TwinLiveRunResult(Map<String, Map<String, Object>> a, List<Map<String, Object>> b, Map<String, Object> c) {

      private TwinLiveRunResult(Map<String, Map<String, Object>> a, List<Map<String, Object>> b, Map<String, Object> c) {
         this.a = a;
         this.b = b;
         this.c = c;
      }
   }

   private record LiveLaneResult(io.karatelabs.ext.rules.twin.RequiredBehaviorVerifier.LaneCoverage a, List<Map<String, Object>> b, List<Map<String, Object>> c, List<Map<String, Object>> d) {

      LiveLaneResult(io.karatelabs.ext.rules.twin.RequiredBehaviorVerifier.LaneCoverage var1, List<Map<String, Object>> var2, List<Map<String, Object>> var3) {
         this(var1, var2, var3, List.of());
      }

      private LiveLaneResult(io.karatelabs.ext.rules.twin.RequiredBehaviorVerifier.LaneCoverage a, List<Map<String, Object>> b, List<Map<String, Object>> c, List<Map<String, Object>> d) {
         this.a = a;
         this.b = b;
         this.c = c;
         this.d = d;
      }
   }

   private static final class RecordedExchangeObserver implements io.karatelabs.ext.rules.twin.TwinReplayEngine.ReplayObserver {
      private final List<Map<String, Object>> a = new ArrayList<>();
      private final List<Integer> b = new ArrayList<>();
      private final int c;
      private int d;
      private int e = -1;
      private Map<String, Object> f;
      private Map<String, Object> g;

      RecordedExchangeObserver(List<Map<String, Object>> var1) {
         this.c = var1.size();

         for (int var2 = 0; var2 < var1.size(); var2++) {
            if (((Map)var1.get(var2)).get("observed") instanceof Map var3) {
               this.a.add(Twin.asMap(var3));
               this.b.add(var2);
            }
         }
      }

      int a() {
         return this.a.size();
      }

      int b() {
         return this.d;
      }

      boolean c() {
         return this.e >= 0;
      }

      int d() {
         return this.e;
      }

      String e() {
         return "the recorded request is not the one the model derives: "
            + Twin.named(this.f)
            + (this.g == null ? " — the record holds no exchange there" : " (the record holds " + Twin.request(this.g) + ")");
      }

      @Override
      public Map<String, Object> observe(Map<String, Object> var1) {
         int var2 = this.d < this.b.size() ? this.b.get(this.d) : this.c;
         Map var3 = this.d < this.a.size() ? this.a.get(this.d++) : null;
         if (var3 != null && io.karatelabs.ext.rules.twin.CanonicalJson.c(var1).equals(io.karatelabs.ext.rules.twin.CanonicalJson.c(var3.get("request")))) {
            return var3;
         } else {
            if (this.e < 0) {
               this.e = var2;
               this.f = var1;
               this.g = var3;
            }

            LinkedHashMap var4 = new LinkedHashMap();
            var4.put("request", var1);
            var4.put("transportFailure", true);
            return var4;
         }
      }
   }

   private record WitnessCoverageResult(TwinModel a, Set<String> b, io.karatelabs.ext.rules.twin.TwinCoverageAnalyzer.CoverageReport c, Map<String, Object> d) {

      private WitnessCoverageResult(TwinModel a, Set<String> b, io.karatelabs.ext.rules.twin.TwinCoverageAnalyzer.CoverageReport c, Map<String, Object> d) {
         this.a = a;
         this.b = b;
         this.c = c;
         this.d = d;
      }
   }

   private record LiveRecordSnapshot(io.karatelabs.ext.rules.LiveRecordStore.LiveRecordSnapshot b) {

      private LiveRecordSnapshot(io.karatelabs.ext.rules.LiveRecordStore.LiveRecordSnapshot b) {
         this.b = b;
      }

      static io.karatelabs.ext.rules.twin.Twin.LiveRecordSnapshot a(Path var0) {
         try {
            return new io.karatelabs.ext.rules.twin.Twin.LiveRecordSnapshot(new io.karatelabs.ext.rules.LiveRecordStore(var0).a());
         } catch (io.karatelabs.ext.rules.LiveRecordStore.LiveRecordException var2) {
            return new io.karatelabs.ext.rules.twin.Twin.LiveRecordSnapshot(null);
         }
      }

      Map<String, Map<String, Object>> a() {
         return this.b == null ? Map.of() : this.b.c();
      }

      public io.karatelabs.ext.rules.LiveRecordStore.LiveRecordSnapshot b() {
         return this.b;
      }
   }

   private record TwinCheckContext(TwinModel b, RequiredBehaviorVerifier c, TwinWalkPlan d, io.karatelabs.ext.rules.twin.TwinWalker.WalkExecution e, Map<String, Object> f, List<Map<String, Object>> g) {

      private TwinCheckContext(TwinModel b, RequiredBehaviorVerifier c, TwinWalkPlan d, io.karatelabs.ext.rules.twin.TwinWalker.WalkExecution e, Map<String, Object> f, List<Map<String, Object>> g) {
         this.b = b;
         this.c = c;
         this.d = d;
         this.e = e;
         this.f = f;
         this.g = g;
      }

      TwinWalkResult a() {
         return this.e.a();
      }

      public TwinModel b() {
         return this.b;
      }

      public RequiredBehaviorVerifier c() {
         return this.c;
      }

      public TwinWalkPlan d() {
         return this.d;
      }

      public io.karatelabs.ext.rules.twin.TwinWalker.WalkExecution e() {
         return this.e;
      }

      public Map<String, Object> f() {
         return this.f;
      }

      public List<Map<String, Object>> g() {
         return this.g;
      }
   }
}
