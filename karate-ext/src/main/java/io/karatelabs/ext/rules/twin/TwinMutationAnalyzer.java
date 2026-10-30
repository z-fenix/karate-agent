package io.karatelabs.ext.rules.twin;

import io.karatelabs.js.Engine;
import io.karatelabs.parser.Node;
import io.karatelabs.parser.NodeType;
import io.karatelabs.parser.Token;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Map.Entry;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Pattern;

final class TwinMutationAnalyzer {
   static final String CONTROL_SEQUENCE_ID = "__control";
   static final String INVALID_BASELINE_CODE = "INVALID_BASELINE";
   private static final int MAX_MUTANTS = 10000;
   private static final long DEFAULT_TIMEOUT_MILLIS = 3600000L;
   private final Twin twin;
   private final io.karatelabs.ext.rules.RulebookRepository.RulebookDocument rulebook;
   private final io.karatelabs.ext.rules.twin.TwinMutationAnalyzer.MutationOptions options;
   private final List<Map<String, Object>> sequenceRows = new ArrayList<>();
   private final List<String> deckSequenceIds;
   private final List<String> candidateSequenceIds = new ArrayList<>();
   private final List<String> k;
   private final String resetMode;
   private Set<String> m;
   private final Map<String, String> n = new LinkedHashMap<>();

   private TwinMutationAnalyzer(Twin var1, io.karatelabs.ext.rules.RulebookRepository.RulebookDocument var2, io.karatelabs.ext.rules.twin.TwinMutationAnalyzer.MutationOptions var3, List<Map<String, Object>> var4) {
      this.twin = var1;
      this.rulebook = var2;
      this.options = var3;
      this.deckSequenceIds = var3.f() == null ? allRequiredSequenceIds(var2) : var3.f();
      this.sequenceRows.add(c());

      for (Map var6 : var4) {
         this.sequenceRows.add(var6);
         this.candidateSequenceIds.add(String.valueOf(var6.get("_id")));
      }

      TwinModel var8 = TwinModelLoader.a(var2.k());
      this.k = var8.i() == null ? List.of() : var8.i().d();
      boolean var9 = var3.k() != null && !io.karatelabs.ext.rules.twin.CanonicalJson.a(var3.k());
      boolean var7 = var8.j() != null;
      this.resetMode = var9 && var7 ? "hook+reset" : (var9 ? "hook" : (var7 ? "reset" : "none"));
   }

   static Map<String, Object> analyze(Twin var0, io.karatelabs.ext.rules.RulebookRepository.RulebookDocument var1, io.karatelabs.ext.rules.twin.TwinMutationAnalyzer.MutationOptions var2) {
      List var3 = var2.g() ? io.karatelabs.ext.rules.twin.TwinMutationAnalyzer.MutationMockFactory.a(var0, var1.d()) : List.of();
      return new TwinMutationAnalyzer(var0, var1, var2, var3).runAnalysis();
   }

   private Map<String, Object> runAnalysis() {
      io.karatelabs.ext.rules.twin.TwinMutationAnalyzer.FlagGuardIndex var1 = io.karatelabs.ext.rules.twin.TwinMutationAnalyzer.FlagGuardIndex.a(this.options.c());
      io.karatelabs.ext.rules.twin.TwinMutationAnalyzer.FunctionDefinitionIndex var2 = io.karatelabs.ext.rules.twin.TwinMutationAnalyzer.FunctionDefinitionIndex.a(this.options.c());
      var var3 = new io.karatelabs.ext.rules.MutationGenerator(this.options.c())
         .a()
         .stream()
         .filter(var1x -> this.options.h().isEmpty() || this.options.h().contains(var1x.e()))
         .sorted(Comparator.comparing(io.karatelabs.ext.rules.MutationGenerator.Mutant::d))
         .toList();
      ArrayList<java.lang.String> var4 = new ArrayList();
      ArrayList<io.karatelabs.ext.rules.MutationGenerator.Mutant> var5 = new ArrayList();

      for (io.karatelabs.ext.rules.MutationGenerator.Mutant var7 : (Iterable<io.karatelabs.ext.rules.MutationGenerator.Mutant>) (Iterable<?>) (var3)) {
         if (var1.a(var7.h())) {
            var4.add(var7.d());
         } else {
            var5.add(var7);
         }
      }

      ArrayList<java.lang.String> var13 = new ArrayList();
      int var14 = this.options.i() == null ? var5.size() : Math.min(this.options.i(), var5.size());

      for (io.karatelabs.ext.rules.MutationGenerator.Mutant var9 : (Iterable<io.karatelabs.ext.rules.MutationGenerator.Mutant>) (Iterable<?>) (var5.subList(var14, var5.size()))) {
         var13.add(var9.d());
      }

      var var15 = var5.subList(0, var14);
      io.karatelabs.ext.rules.twin.TwinMutationAnalyzer.BaselineProjection var16 = this.validateBaseline();
      LinkedHashMap<java.lang.String, io.karatelabs.ext.rules.twin.TwinMutationAnalyzer.MutantResultAccumulator> var10 = new LinkedHashMap();

      for (io.karatelabs.ext.rules.MutationGenerator.Mutant var12 : (Iterable<io.karatelabs.ext.rules.MutationGenerator.Mutant>) (Iterable<?>) (var15)) {
         var10.put(var12.d(), this.analyzeMutant(var12, var16));
      }

      return this.a(var15, var10, var2, var1, var4, var13, var5.size() > var14);
   }

   private io.karatelabs.ext.rules.twin.TwinMutationAnalyzer.BaselineProjection validateBaseline() {
      io.karatelabs.ext.rules.twin.TwinMutationAnalyzer.MutationRunResult var1 = this.runDeckMutation(this.options.c(), this.a(this.deckSequenceIds), 3600000L);
      io.karatelabs.ext.rules.twin.TwinMutationAnalyzer.MutationRunResult var2 = this.candidateSequenceIds.isEmpty() ? null : this.runDeckMutation(this.options.c(), this.candidateSequenceIds, 3600000L);
      ArrayList<java.lang.String> var3 = new ArrayList();

      for (String var5 : this.deckSequenceIds) {
         if (!"PASS".equals(var1.c().get(var5))) {
            var3.add(var5);
         }
      }

      if (var2 != null) {
         for (String var7 : this.candidateSequenceIds) {
            if (!"PASS".equals(var2.c().get(var7))) {
               var3.add(var7);
            }
         }
      }

      if (var1.b() != null || var2 != null && var2.b() != null) {
         throw TwinException.a(
            500,
            "INVALID_BASELINE",
            "Twin.mutate: the unmutated baseline did not replay (" + (var1.b() != null ? var1.b() : var2.b()) + ")",
            Map.of("stage", "baseline")
         );
      } else if (!var3.isEmpty()) {
         throw TwinException.a(
            400,
            "INVALID_BASELINE",
            "Twin.mutate: "
               + var3.size()
               + " selected sequence(s) do not PASS on the unmutated handlers, so nothing can be graded against them — fix the deck or the mock first",
            Map.of("sequences", var3)
         );
      } else {
         this.m = io.karatelabs.ext.rules.twin.ObservationReconciler.b(this.rulebook.k(), a(var1, var2));
         return new io.karatelabs.ext.rules.twin.TwinMutationAnalyzer.BaselineProjection(var1.b(this.deckSequenceIds), var2 == null ? null : var2.b(this.candidateSequenceIds));
      }
   }

   private static Map<String, Map<String, Object>> a(io.karatelabs.ext.rules.twin.TwinMutationAnalyzer.MutationRunResult... var0) {
      LinkedHashMap<java.lang.String, java.util.Map<java.lang.String,java.lang.Object>> var1 = new LinkedHashMap();

      for (io.karatelabs.ext.rules.twin.TwinMutationAnalyzer.MutationRunResult var5 : var0) {
         if (var5 != null && var5.f() != null) {
            for (Object var7 : (List)var5.f().get("sequences")) {
               Map<java.lang.String, Object> var8 = (Map)var7;
               var1.put(String.valueOf(var8.get("sequenceId")), var8);
            }
         }
      }

      return var1;
   }

   private io.karatelabs.ext.rules.twin.TwinMutationAnalyzer.MutantResultAccumulator analyzeMutant(io.karatelabs.ext.rules.MutationGenerator.Mutant var1, io.karatelabs.ext.rules.twin.TwinMutationAnalyzer.BaselineProjection var2) {
      MutationEvaluator var3 = new MutationEvaluator(this.deckSequenceIds, this.candidateSequenceIds);
      io.karatelabs.ext.rules.twin.TwinMutationAnalyzer.MutantResultAccumulator var4 = new io.karatelabs.ext.rules.twin.TwinMutationAnalyzer.MutantResultAccumulator(var3);
      if (!var1.o()) {
         var3.a(io.karatelabs.ext.rules.twin.MutationEvaluator.MutationLegResult.a("catalog", null, var1.p(), var1.p(), null));
         return var4.b();
      } else {
         long var5 = this.options.j() == null ? 3600000L : this.options.j().intValue();
         io.karatelabs.ext.rules.twin.TwinMutationAnalyzer.MutationRunResult var7 = this.runDeckMutation(var1.m(), this.a(this.deckSequenceIds), var5);
         this.a(var4, var7, "deck", this.deckSequenceIds, var2.deck(), var1);
         if (var7.a() && var4.k.isEmpty() && !this.candidateSequenceIds.isEmpty()) {
            this.a(var4, this.runDeckMutation(var1.m(), this.candidateSequenceIds, var5), "candidate", this.candidateSequenceIds, var2.candidate(), var1);
         }

         return var4.b();
      }
   }

   private void a(io.karatelabs.ext.rules.twin.TwinMutationAnalyzer.MutantResultAccumulator var1, io.karatelabs.ext.rules.twin.TwinMutationAnalyzer.MutationRunResult var2, String var3, List<String> var4, LiveProjection var5, io.karatelabs.ext.rules.MutationGenerator.Mutant var6) {
      MutationEvaluator var7 = var1.a;
      boolean var8 = "deck".equals(var3);
      if (var2.f() != null) {
         if (var8 && var2.c().containsKey("__control")) {
            var7.a(io.karatelabs.ext.rules.twin.MutationEvaluator.MutationLegResult.a("control", "__control", var2.c().get("__control"), var2.d().get("__control"), null));
         }

         LiveProjection var9 = var2.b(var4);

         for (String var11 : var4) {
            String var12 = var2.c().get(var11);
            if (var12 != null) {
               Boolean var13 = var5 == null ? null : io.karatelabs.ext.rules.twin.CanonicalJson.c(var9.a(var11)).equals(io.karatelabs.ext.rules.twin.CanonicalJson.c(var5.a(var11)));
               var7.a(new io.karatelabs.ext.rules.twin.MutationEvaluator.MutationLegResult(var3, var11, var12, var2.d().get(var11), null, null, var13, var2.h().get(var11), this.projectionHash(var3, var11, var5)));
               if (Boolean.FALSE.equals(var13)) {
                  var1.a(this.projectionDiffers(var9, var5, var11));
               }

               var1.a(var3, var11, var12, var2.e().get(var11));
            }
         }
      } else if (var2.j() != null) {
         var7.a(io.karatelabs.ext.rules.twin.MutationEvaluator.MutationLegResult.a("mockStart", null, "START_FAILED", var2.j(), var2.l()));
         return;
      }

      if (var2.i()) {
         var7.a(io.karatelabs.ext.rules.twin.MutationEvaluator.MutationLegResult.a(var3, null, "TIMEOUT", null, "the " + var3 + " leg exceeded " + this.options.j() + "ms"));
      } else {
         String var14 = var2.k() == null ? var2.j() : var2.k();
         if (var14 == null) {
            boolean var15 = io.karatelabs.ext.rules.MutationTraceCollector.a(var6, var2.a(var6.m()));
            var1.a(var3, var15);
            var7.a(new io.karatelabs.ext.rules.twin.MutationEvaluator.MutationLegResult(var3, null, "PASS", null, null, var15, null, null, null));
         } else {
            var7.a(var2.c().isEmpty() && var8 ? io.karatelabs.ext.rules.twin.MutationEvaluator.MutationLegResult.a("control", "__control", "INVALID", var14, var2.l()) : io.karatelabs.ext.rules.twin.MutationEvaluator.MutationLegResult.a(var3, null, "INVALID", var14, var2.l()));
         }
      }
   }

   private String projectionHash(String var1, String var2, LiveProjection var3) {
      return var3 == null
         ? null
         : this.n.computeIfAbsent(var1 + "/" + var2, var2x -> io.karatelabs.ext.rules.twin.CanonicalJson.a(io.karatelabs.ext.rules.twin.CanonicalJson.c(var3.a(var2))));
   }

   private boolean projectionDiffers(LiveProjection var1, LiveProjection var2, String var3) {
      if (this.m == null) {
         return false;
      } else {
         var var4 = var2.b(var1, var3);
         return var4 != null && !var4.isEmpty() ? var4.stream().noneMatch(var1x -> io.karatelabs.ext.rules.twin.ObservationReconciler.a(this.m, var1x)) : false;
      }
   }

   private io.karatelabs.ext.rules.twin.TwinMutationAnalyzer.MutationRunResult runDeckMutation(String var1, List<String> var2, long var3) {
      if (!this.options.l()) {
         return this.runCandidateMutation(var1, var2, var3);
      } else {
         long var5 = System.currentTimeMillis() + var3;
         io.karatelabs.ext.rules.twin.TwinMutationAnalyzer.MutationRunResult var7 = null;

         for (String var9 : var2) {
            long var10 = var5 - System.currentTimeMillis();
            io.karatelabs.ext.rules.twin.TwinMutationAnalyzer.MutationRunResult var12 = var10 <= 0L ? io.karatelabs.ext.rules.twin.TwinMutationAnalyzer.MutationRunResult.a(List.of()) : this.runCandidateMutation(var1, List.of(var9), var10);
            if (!var12.a()) {
               return var7 == null ? var12 : var7.b(var12);
            }

            var7 = var7 == null ? var12 : var7.a(var12);
         }

         return var7 == null ? io.karatelabs.ext.rules.twin.TwinMutationAnalyzer.MutationRunResult.a(Map.of("sequences", List.of()), null) : var7;
      }
   }

   private io.karatelabs.ext.rules.twin.TwinMutationAnalyzer.MutationRunResult runCandidateMutation(String var1, List<String> var2, long var3) {
      io.karatelabs.ext.rules.MutationTraceCollector[] var5 = new io.karatelabs.ext.rules.MutationTraceCollector[1];
      io.karatelabs.ext.rules.twin.StandInServerLauncher.StandInLaunchResult var6 = io.karatelabs.ext.rules.twin.StandInServerLauncher.a(this.twin.mocks(), this.options.d(), this.options.e(), var1, Map.of(), var1x -> var5[0] = io.karatelabs.ext.rules.MutationTraceCollector.a(var1x));
      if (var6 instanceof io.karatelabs.ext.rules.twin.StandInServerLauncher.StandInLaunchFailure var15) {
         return io.karatelabs.ext.rules.twin.TwinMutationAnalyzer.MutationRunResult.a(var15.a(), var15.b());
      } else {
         io.karatelabs.ext.rules.twin.TwinMutationAnalyzer.MutationRunResult var11;
         try (io.karatelabs.ext.rules.twin.StandInServerLauncher.StandInServer var7 = (io.karatelabs.ext.rules.twin.StandInServerLauncher.StandInServer)var6) {
            LinkedHashMap<java.lang.String, java.lang.Object> var8 = new LinkedHashMap();
            var8.put("baseUrl", var7.a());
            var8.put("against", "mock");
            var8.put("timeoutMs", 10000);
            var8.put("detail", "log");
            var8.put("sequences", var2);
            if (this.options.k() != null && !io.karatelabs.ext.rules.twin.CanonicalJson.a(this.options.k())) {
               var8.put("reset", this.options.k());
            }

            CopyOnWriteArrayList var9 = new CopyOnWriteArrayList();
            var var10 = this.twin.live(this.options.a(), var8, var3, false, this.sequenceRows, var9::add);
            if (var10.get("error") != null) {
               String var16 = String.valueOf(var10.get("code"));
               return "TIMEOUT".equals(var16) ? io.karatelabs.ext.rules.twin.TwinMutationAnalyzer.MutationRunResult.a(var9) : io.karatelabs.ext.rules.twin.TwinMutationAnalyzer.MutationRunResult.a(var16, String.valueOf(var10.get("error")), var9);
            }

            var11 = io.karatelabs.ext.rules.twin.TwinMutationAnalyzer.MutationRunResult.a(var10, var5[0]);
         }

         return var11;
      }
   }

   private Map<String, Object> a(
      List<io.karatelabs.ext.rules.MutationGenerator.Mutant> var1, Map<String, io.karatelabs.ext.rules.twin.TwinMutationAnalyzer.MutantResultAccumulator> var2, io.karatelabs.ext.rules.twin.TwinMutationAnalyzer.FunctionDefinitionIndex var3, io.karatelabs.ext.rules.twin.TwinMutationAnalyzer.FlagGuardIndex var4, List<String> var5, List<String> var6, boolean var7
   ) {
      LinkedHashMap<java.lang.String, java.lang.Integer> var8 = new LinkedHashMap();

      for (String var10 : List.of("KILLED", "SURVIVED", "NOTCOVERED", "TIMEOUT", "INVALID", "SCREENED")) {
         var8.put(var10, 0);
      }

      ArrayList<java.util.Map<java.lang.String,java.lang.Object>> var21 = new ArrayList();
      ArrayList<java.util.Map<java.lang.String,java.lang.Object>> var22 = new ArrayList();
      ArrayList<java.util.Map<java.lang.String,java.lang.Object>> var11 = new ArrayList();
      int var12 = 0;
      int var13 = 0;
      int var14 = 0;

      for (io.karatelabs.ext.rules.MutationGenerator.Mutant var16 : var1) {
         io.karatelabs.ext.rules.twin.TwinMutationAnalyzer.MutantResultAccumulator var17 = (io.karatelabs.ext.rules.twin.TwinMutationAnalyzer.MutantResultAccumulator)var2.get(var16.d());
         var8.merge(var17.b, 1, Integer::sum);
         var12 += var17.k.isEmpty() ? 0 : 1;
         var13 += !var17.k.isEmpty() && "ORDER".equals(var17.d) ? 1 : 0;
         var14 += !"TIMEOUT".equals(var17.b) && !var17.j.d() && !var17.j.c() ? 0 : 1;
         var var18 = var16.c();
         var18.put("status", var17.b);
         var17.a(var18, var3.a(var16.h()), var16);
         var21.add(var18);
         boolean var19 = "KILLED".equals(var17.b) || "SURVIVED".equals(var17.b);
         if (var19 && (var17.k.isEmpty() || !"ORDER".equals(var17.d))) {
            var22.add(a(var16, var17));
         }

         if (!var17.l.isEmpty()) {
            var var20 = a(var16, var17);
            var20.put("candidates", new TreeMap<>(var17.l));
            var11.add(var20);
         }
      }

      int var23 = (Integer)var8.get("KILLED") + (Integer)var8.get("SURVIVED");
      LinkedHashMap<java.lang.String, java.lang.Object> var24 = new LinkedHashMap();
      var24.put("rule", this.rulebook.d());
      var24.put("counts", var8);
      var24.put("denominator", var23);
      var24.put("independentlyKilled", var13);
      var24.put("rawKilled", var8.get("KILLED"));
      var24.put("independentScore", percentage(var13, var23));
      var24.put("rawKillRate", percentage((Integer)var8.get("KILLED"), var23));
      var24.put("baselineFailedAssertions", List.of());
      var24.put(
         "framing",
         "grades the pinned deck against a syntactic, first-order, operator-biased frame over the mock's guard code; the pricing oracle is shared, so only the order kills speak for the deck's independence (rules-transitions.md §17.10)"
      );
      var24.put("notCovered", a(var1, var2, "NOTCOVERED"));
      var24.put("screened", a(var1, var2, "SCREENED"));
      var24.put("invalid", a(var1, var2, "INVALID"));
      var24.put("timeout", a(var1, var2, "TIMEOUT"));
      var24.put("mutants", var21);
      var24.put("worklist", var22);
      var24.put("deckKillRate", percentage(var12, var23));
      var24.put("orderKillRate", percentage(var13, var23));
      var24.put("candidateKills", var11);
      var24.put("excluded", var5);
      var24.put("notRun", var6);
      var24.put("timeouts", var14);
      var24.put("provenance", this.a(var4, var1.size() + var5.size(), var1.size(), var7));
      var24.put(
         "next",
         var11.isEmpty()
            ? "every mutant the deck missed is in the worklist — strengthen the deck where a worklist row names a guard no pinned sequence distinguishes"
            : "pin the candidate sequences named in candidateKills, then re-run Twin.mutate to watch deckKillRate climb"
      );
      return var24;
   }

   private Map<String, Object> a(io.karatelabs.ext.rules.twin.TwinMutationAnalyzer.FlagGuardIndex var1, int var2, int var3, boolean var4) {
      LinkedHashMap<java.lang.String, java.lang.Object> var5 = new LinkedHashMap();
      var5.put("handlers", this.options.b());
      var5.put("handlersSha256", io.karatelabs.ext.common.Sha256.a(this.options.c()));
      var5.put("twinSha256", this.rulebook.i().get("twin"));
      var5.put("rulebookSha256", this.rulebook.n().c());
      var5.put("sequencesSha256", this.rulebook.i().get("sequences"));
      var5.put(
         "operators",
         this.options.h().isEmpty()
            ? List.of("CONDITIONALS_BOUNDARY", "INLINE_CONSTANT", "NEGATE_CONDITIONALS", "REMOVE_CONDITIONALS")
            : new TreeSet<>(this.options.h()).stream().toList()
      );
      var5.put("exclusionSpans", var1.b());
      var5.put("catalogBefore", var2);
      var5.put("catalogAfter", var3);
      LinkedHashMap<java.lang.String, java.lang.Integer> var6 = new LinkedHashMap();
      var6.put("mutants", this.options.i());
      var6.put("timeoutMs", this.options.j());
      var5.put("budget", var6);
      var5.put("budgetHit", var4);
      var5.put("deck", this.deckSequenceIds.size());
      var5.put("candidates", this.candidateSequenceIds.size());
      var5.put("isolation", this.resetMode);
      var5.put("oracleFields", this.k);
      var5.put("readSet", this.m == null ? null : this.m.size());
      return var5;
   }

   private static Map<String, Object> a(io.karatelabs.ext.rules.MutationGenerator.Mutant var0, io.karatelabs.ext.rules.twin.TwinMutationAnalyzer.MutantResultAccumulator var1) {
      LinkedHashMap<java.lang.String, java.lang.Object> var2 = new LinkedHashMap();
      var2.put("id", var0.d());
      var2.put("mutatorName", var0.e());
      var2.put("site", var0.f());
      var2.put("status", var1.b);
      var2.put("finding", "no pinned sequence independently notices " + var0.k() + " changing (" + var0.i() + " -> " + var0.j() + ")");
      return var2;
   }

   private static List<String> a(List<io.karatelabs.ext.rules.MutationGenerator.Mutant> var0, Map<String, io.karatelabs.ext.rules.twin.TwinMutationAnalyzer.MutantResultAccumulator> var1, String var2) {
      ArrayList<java.lang.String> var3 = new ArrayList();

      for (io.karatelabs.ext.rules.MutationGenerator.Mutant var5 : var0) {
         if (var2.equals(((io.karatelabs.ext.rules.twin.TwinMutationAnalyzer.MutantResultAccumulator)var1.get(var5.d())).b)) {
            var3.add(var5.d());
         }
      }

      return var3;
   }

   private static Map<String, Object> c() {
      LinkedHashMap<java.lang.String, java.lang.Object> var0 = new LinkedHashMap();
      var0.put("_id", "__control");
      var0.put("steps", List.of());
      return var0;
   }

   private List<String> a(List<String> var1) {
      ArrayList<java.lang.String> var2 = new ArrayList(var1.size() + 1);
      var2.add("__control");
      var2.addAll(var1);
      return var2;
   }

   private static List<String> allRequiredSequenceIds(io.karatelabs.ext.rules.RulebookRepository.RulebookDocument var0) {
      ArrayList<java.lang.String> var1 = new ArrayList();
      if (var0.l() != null) {
         for (Object var3 : (List)var0.l().get("sequences")) {
            var1.add(String.valueOf(((Map)var3).get("_id")));
         }
      }

      return var1;
   }

   static double percentage(int var0, int var1) {
      return var1 == 0 ? 0.0 : Math.round(var0 * 10000.0 / var1) / 10000.0;
   }

   private record BaselineProjection(LiveProjection deck, LiveProjection candidate) {

      private BaselineProjection(LiveProjection deck, LiveProjection candidate) {
         this.deck = deck;
         this.candidate = candidate;
      }
   }

   private record MutationRunResult(Map<String, String> c, Map<String, String> d, Map<String, Map<String, Object>> e, Map<String, Object> f, List<io.karatelabs.ext.rules.MutationTraceCollector> g, Map<String, String> h, boolean i, String j, String k, String l) {

      private MutationRunResult(Map<String, String> c, Map<String, String> d, Map<String, Map<String, Object>> e, Map<String, Object> f, List<io.karatelabs.ext.rules.MutationTraceCollector> g, Map<String, String> h, boolean i, String j, String k, String l) {
         this.c = c;
         this.d = d;
         this.e = e;
         this.f = f;
         this.g = g;
         this.h = h;
         this.i = i;
         this.j = j;
         this.k = k;
         this.l = l;
      }

      static io.karatelabs.ext.rules.twin.TwinMutationAnalyzer.MutationRunResult a(Map<String, Object> var0, io.karatelabs.ext.rules.MutationTraceCollector var1) {
         LinkedHashMap<java.lang.String, java.lang.String> var2 = new LinkedHashMap();
         LinkedHashMap<java.lang.String, java.lang.String> var3 = new LinkedHashMap();
         LinkedHashMap<String, Map<String, Object>> var4 = new LinkedHashMap<>();
         LinkedHashMap<java.lang.String, java.lang.String> var5 = new LinkedHashMap();
         String var6 = null;

         for (Map var8 : (Iterable<Map>) (Iterable<?>) ((List)var0.get("sequences"))) {
            String var9 = String.valueOf(var8.get("sequenceId"));
            Map<java.lang.String, Object> var10 = (Map)var8.get("disposition");
            var2.put(var9, String.valueOf(var10.get("kind")));
            var6 = var6 == null ? var9 : var6;
            var5.put(var9, var6);
            if (var10.get("code") != null) {
               var3.put(var9, String.valueOf(var10.get("code")));
            }

            if (var10.get("mismatch") instanceof Map var11) {
               LinkedHashMap<java.lang.String, java.lang.Object> var13 = new LinkedHashMap(var11);
               var13.put("at", var10.get("at"));
               var4.put(var9, var13);
            }
         }

         return new io.karatelabs.ext.rules.twin.TwinMutationAnalyzer.MutationRunResult(var2, var3, var4, var0, var1 == null ? List.of() : List.of(var1), var5, false, null, null, null);
      }

      static io.karatelabs.ext.rules.twin.TwinMutationAnalyzer.MutationRunResult a(List<Map<String, Object>> var0) {
         io.karatelabs.ext.rules.twin.TwinMutationAnalyzer.MutationRunResult var1 = a(Map.of("sequences", List.copyOf(var0)), null);
         return new io.karatelabs.ext.rules.twin.TwinMutationAnalyzer.MutationRunResult(var1.c, var1.d, var1.e, var1.f, List.of(), var1.h, true, null, null, null);
      }

      static io.karatelabs.ext.rules.twin.TwinMutationAnalyzer.MutationRunResult a(String var0, String var1) {
         return new io.karatelabs.ext.rules.twin.TwinMutationAnalyzer.MutationRunResult(Map.of(), Map.of(), Map.of(), null, List.of(), Map.of(), false, var0, null, var1);
      }

      static io.karatelabs.ext.rules.twin.TwinMutationAnalyzer.MutationRunResult a(String var0, String var1, List<Map<String, Object>> var2) {
         io.karatelabs.ext.rules.twin.TwinMutationAnalyzer.MutationRunResult var3 = a(Map.of("sequences", List.copyOf(var2)), null);
         return new io.karatelabs.ext.rules.twin.TwinMutationAnalyzer.MutationRunResult(var3.c, var3.d, var3.e, var3.f, List.of(), var3.h, false, null, var0, var1);
      }

      io.karatelabs.ext.rules.twin.TwinMutationAnalyzer.MutationRunResult a(io.karatelabs.ext.rules.twin.TwinMutationAnalyzer.MutationRunResult var1) {
         var var2 = new LinkedHashMap<>(this.c);
         var2.putAll(var1.c);
         var var3 = new LinkedHashMap<>(this.d);
         var3.putAll(var1.d);
         var var4 = new LinkedHashMap<>(this.e);
         var4.putAll(var1.e);
         ArrayList var5 = new ArrayList((List)this.f.get("sequences"));
         var5.addAll((List)var1.f.get("sequences"));
         var var6 = new ArrayList<>(this.g);
         var6.addAll(var1.g);
         var var7 = new LinkedHashMap<>(this.h);
         var7.putAll(var1.h);
         return new io.karatelabs.ext.rules.twin.TwinMutationAnalyzer.MutationRunResult(var2, var3, var4, Map.of("sequences", var5), var6, var7, false, null, null, null);
      }

      io.karatelabs.ext.rules.twin.TwinMutationAnalyzer.MutationRunResult b(io.karatelabs.ext.rules.twin.TwinMutationAnalyzer.MutationRunResult var1) {
         io.karatelabs.ext.rules.twin.TwinMutationAnalyzer.MutationRunResult var2 = var1.f == null ? this : this.a(var1);
         return new io.karatelabs.ext.rules.twin.TwinMutationAnalyzer.MutationRunResult(var2.c, var2.d, var2.e, var2.f, var2.g, var2.h, var1.i, var1.j, var1.k, var1.l);
      }

      boolean a() {
         return this.f != null && !this.i && this.j == null && this.k == null;
      }

      String b() {
         return this.k == null ? this.j : this.k;
      }

      List<io.karatelabs.ext.rules.MutationTraceCollector.MutationTraceSnapshot> a(String var1) {
         return this.g.stream().map(var1x -> var1x.a(var1)).toList();
      }

      LiveProjection b(List<String> var1) {
         var var2 = new LinkedHashSet<>(var1);
         ArrayList<java.lang.Object> var3 = new ArrayList();

         for (Object var5 : (List)this.f.get("sequences")) {
            if (var2.contains(String.valueOf(((Map)var5).get("sequenceId")))) {
               var3.add(var5);
            }
         }

         return io.karatelabs.ext.rules.twin.LiveProjection.a(Map.of("sequences", var3));
      }

      public Map<String, String> c() {
         return this.c;
      }

      public Map<String, String> d() {
         return this.d;
      }

      public Map<String, Map<String, Object>> e() {
         return this.e;
      }

      public Map<String, Object> f() {
         return this.f;
      }

      public List<io.karatelabs.ext.rules.MutationTraceCollector> g() {
         return this.g;
      }

      public Map<String, String> h() {
         return this.h;
      }

      public boolean i() {
         return this.i;
      }

      public String j() {
         return this.j;
      }

      public String k() {
         return this.k;
      }

      public String l() {
         return this.l;
      }
   }

   record FlagGuardIndex(List<int[]> a, List<Map<String, Object>> b) {
      private static final Set<String> c = Set.of("on", "hit");
      private static final Pattern d = Pattern.compile("on\\s*\\(\\s*session\\s*,");

      FlagGuardIndex(List<int[]> a, List<Map<String, Object>> b) {
         this.a = a;
         this.b = b;
      }

      static io.karatelabs.ext.rules.twin.TwinMutationAnalyzer.FlagGuardIndex a(String var0) {
         Node var1 = Engine.parse(var0);
         ArrayList<int[]> var2 = new ArrayList<>();
         ArrayList<Map<String, Object>> var3 = new ArrayList<>();

         for (Node var5 : var1.findAll(NodeType.IF_STMT)) {
            if (var5.size() > 4 && a(var5.get(2))) {
               a(var2, var3, var0, "flagGuard", var5.getFirstToken(), var5.get(4).getLastToken());
            }
         }

         for (Node var10 : var1.findAll(NodeType.VAR_DECL)) {
            Token var6 = var10.getFirstToken();
            if (var6 != null && c.contains(var6.getText())) {
               a(var2, var3, var0, "flagHelper", var6, var10.getLastToken());
               if ("on".equals(var6.getText())) {
                  int var7 = var0.lastIndexOf(10, var6.pos - 1) + 1;
                  int var8 = a(var0, var6.pos);
                  if (var8 < var7) {
                     a(var2, var3, var0, "flagComment", var8, var7);
                  }
               }
            }
         }

         var2.sort(Comparator.comparingInt(var0x -> var0x[0]));
         var3.sort(Comparator.comparingInt(var0x -> (Integer)var0x.get("fromLine")));
         return new io.karatelabs.ext.rules.twin.TwinMutationAnalyzer.FlagGuardIndex(var2, var3);
      }

      private static boolean a(Node var0) {
         for (Node var2 : var0.findAll(NodeType.FN_CALL_EXPR)) {
            String var3 = var2.getText();
            if (var3 != null && d.matcher(var3).lookingAt()) {
               return true;
            }
         }

         return false;
      }

      private static int a(String var0, int var1) {
         int var2 = var0.lastIndexOf(10, var1 - 1) + 1;

         while (var2 > 0) {
            int var3 = var0.lastIndexOf(10, var2 - 2) + 1;
            if (!var0.substring(var3, var2).trim().startsWith("//")) {
               return var2;
            }

            var2 = var3;
         }

         return var2;
      }

      private static void a(List<int[]> var0, List<Map<String, Object>> var1, String var2, String var3, Token var4, Token var5) {
         a(var0, var1, var2, var3, var4.pos, var5.pos + var5.length);
      }

      private static void a(List<int[]> var0, List<Map<String, Object>> var1, String var2, String var3, int var4, int var5) {
         var0.add(new int[]{var4, var5});
         LinkedHashMap<java.lang.String, java.lang.Object> var6 = new LinkedHashMap();
         var6.put("kind", var3);
         var6.put("fromLine", b(var2, var4));
         var6.put("toLine", b(var2, Math.max(var4, var5 - 1)));
         var1.add(var6);
      }

      private static int b(String var0, int var1) {
         int var2 = 1;

         for (int var3 = 0; var3 < var1 && var3 < var0.length(); var3++) {
            var2 += var0.charAt(var3) == '\n' ? 1 : 0;
         }

         return var2;
      }

      boolean a(int var1) {
         for (int[] var3 : this.a) {
            if (var1 >= var3[0] && var1 < var3[1]) {
               return true;
            }
         }

         return false;
      }
   }

   record FunctionDefinitionIndex(Map<String, int[]> a, Map<String, String> b) {

      FunctionDefinitionIndex(Map<String, int[]> a, Map<String, String> b) {
         this.a = a;
         this.b = b;
      }

      static io.karatelabs.ext.rules.twin.TwinMutationAnalyzer.FunctionDefinitionIndex a(String var0) {
         Node var1 = Engine.parse(var0);
         LinkedHashMap<java.lang.String, int[]> var2 = new LinkedHashMap();

         for (Node var4 : var1.findAll(NodeType.VAR_DECL)) {
            Token var5 = var4.getFirstToken();
            if (var5 != null && !var4.findAll(NodeType.FN_EXPR).isEmpty()) {
               Token var6 = var4.getLastToken();
               var2.put(var5.getText(), new int[]{var5.pos, var6.pos + var6.length});
            }
         }

         LinkedHashMap<java.lang.String, java.lang.String> var7 = new LinkedHashMap();

         for (Node var9 : var1.findAll(NodeType.OBJECT_ELEM)) {
            if (var9.size() >= 3) {
               var7.put(var9.get(2).getText(), var9.getFirst().getText());
            }
         }

         return new io.karatelabs.ext.rules.twin.TwinMutationAnalyzer.FunctionDefinitionIndex(var2, var7);
      }

      String a(int var1) {
         String var2 = null;
         int var3 = 0;

         for (Entry var5 : this.a.entrySet()) {
            int[] var6 = (int[])var5.getValue();
            if (var1 >= var6[0] && var1 < var6[1] && var6[1] - var6[0] > var3) {
               var2 = (String)var5.getKey();
               var3 = var6[1] - var6[0];
            }
         }

         return var2 == null ? null : this.b.getOrDefault(var2, var2);
      }
   }

   record MutationOptions(String a, String b, String c, Path d, Path e, List<String> f, boolean g, Set<String> h, Integer i, Integer j, Object k, boolean l) {

      MutationOptions(String a, String b, String c, Path d, Path e, List<String> f, boolean g, Set<String> h, Integer i, Integer j, Object k, boolean l) {
         this.a = a;
         this.b = b;
         this.c = c;
         this.d = d;
         this.e = e;
         this.f = f;
         this.g = g;
         this.h = h;
         this.i = i;
         this.j = j;
         this.k = k;
         this.l = l;
      }
   }

   private static final class MutationMockFactory {
      static List<Map<String, Object>> a(Twin var0, String var1) {
         var var2 = var0.check(var1, Map.of());
         if (var2.get("error") != null) {
            throw TwinException.a(
               500, "INVALID_BASELINE", "Twin.mutate: the pair-gap universe could not be derived — " + var2.get("error"), Map.of("stage", "candidates")
            );
         } else {
            List var3 = (List)((Map)var2.get("findings")).get("transitionPairGaps");
            ArrayList<java.util.Map<java.lang.String,java.lang.Object>> var4 = new ArrayList(var3.size());
            int var5 = 0;

            for (Map var7 : (Iterable<Map>) (Iterable<?>) (var3)) {
               LinkedHashMap<java.lang.String, java.lang.Object> var8 = new LinkedHashMap();
               var8.put("_id", String.format("pair-%03d", ++var5));
               var8.put("_label", var7.get("command1") + " then " + var7.get("command2"));
               var8.put("steps", ((Map)var7.get("candidate")).get("steps"));
               var4.add(var8);
            }

            return var4;
         }
      }
   }

   private final class MutantResultAccumulator {
      final MutationEvaluator a;
      String b;
      String c;
      String d;
      boolean e;
      boolean f;
      boolean g;
      boolean h;
      boolean i = true;
      io.karatelabs.ext.rules.twin.MutationEvaluator.MutationVerdict j;
      final Map<String, String> k = new TreeMap<>();
      final Map<String, String> l = new TreeMap<>();
      final Map<String, String> m = new TreeMap<>();

      MutantResultAccumulator(MutationEvaluator var2) {
         this.a = var2;
      }

      void a(boolean var1) {
         this.h = true;
         this.i &= var1;
      }

      boolean a() {
         return this.h && this.i;
      }

      void a(String var1, boolean var2) {
         if ("deck".equals(var1)) {
            this.f = var2;
         } else {
            this.g = var2;
         }
      }

      void a(String var1, String var2, String var3, Map<String, Object> var4) {
         if (!"PASS".equals(var3)) {
            if (!"FAIL".equals(var3)) {
               this.m.put(var2, var3);
            } else {
               DifferenceClassification var5 = io.karatelabs.ext.rules.twin.DifferenceClassification.a(var4, TwinMutationAnalyzer.this.k);
               this.e = this.e | var5.b();
               if (this.d == null || "ORDER".equals(var5.a())) {
                  this.d = var5.a();
               }

               String var6 = var4 == null ? var3 : var4.get("kind") + "@" + var4.get("at");
               ("deck".equals(var1) ? this.k : this.l).put(var2, var6);
            }
         }
      }

      io.karatelabs.ext.rules.twin.TwinMutationAnalyzer.MutantResultAccumulator b() {
         this.j = this.a.b();
         this.b = this.j.a();
         this.c = this.j.b();
         return this;
      }

      void a(Map<String, Object> var1, String var2, io.karatelabs.ext.rules.MutationGenerator.Mutant var3) {
         if (this.c != null) {
            var1.put("statusReason", this.c);
         }

         if (this.d != null) {
            var1.put("killClass", this.d);
            if (this.e) {
               var1.put("mixed", true);
            }
         }

         LinkedHashMap<java.lang.String, java.util.Map<java.lang.String,java.lang.String>> var4 = new LinkedHashMap();
         var4.put("deck", this.k);
         var4.put("candidates", this.l);
         var1.put("killedBy", var4);
         LinkedHashMap<java.lang.String, java.lang.Boolean> var5 = new LinkedHashMap();
         var5.put("deck", this.f);
         var5.put("candidates", this.g);
         var1.put("reached", var5);
         LinkedHashMap<java.lang.String, java.lang.Object> var6 = new LinkedHashMap();
         var6.put("line", var3.g());
         var6.put("excerpt", var3.k());
         var6.put("handler", var2);
         var1.put("site", var6);
         if (!this.m.isEmpty()) {
            var1.put("unresolved", this.m);
         }

         if (this.j.d()) {
            var1.put("candidateTimeout", true);
         }

         if (this.j.e() != null) {
            var1.put("candidateUnresolved", this.j.e());
         }

         if (this.j.c()) {
            var1.put("deckTimeout", true);
         }

         if (this.j.f()) {
            var1.put("reachUnknown", true);
         }

         if (this.j.g()) {
            var1.put("killWithoutReach", true);
         }

         if (this.j.h()) {
            var1.put("killWithEqualWire", true);
         }

         if (this.a()) {
            var1.put("projectionBlind", true);
         }

         var1.put("evidence", this.a.c());
      }
   }
}
