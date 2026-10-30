package io.karatelabs.ext.rules;

import io.karatelabs.common.Json;
import io.karatelabs.ext.common.jsapi.JsApi;
import io.karatelabs.ext.rules.twin.Twin;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.Map.Entry;

final class RuleVerificationService {
   static final String a = "verification";
   private static final String b = "COMPLETE";
   private static final String c = "HOLDS";
   private static final String d = "VIOLATED";
   private static final String e = "NOTCHECKED";
   private static final Set<String> f = Set.of("COMPLETE", "HOLDS", "VIOLATED", "NOTCHECKED");
   private static final Set<String> g = Set.of("REFUSED", "ACCEPTED", "NOTCHECKED");

   private RuleVerificationService() {
   }

   static Path a(Path var0, String var1) {
      return RuleBaselineStore.a(var0, var1, "verification");
   }

   static Map<String, Object> a(Path var0, RulebookRepository var1, RuleEvaluator var2, String var3) {
      io.karatelabs.ext.rules.RulebookRepository.RulebookDocument var4 = var1.b(var3);
      io.karatelabs.ext.rules.RuleVerificationService.VerificationSnapshot var5 = a(var1, var4, var2);
      long var6 = System.currentTimeMillis();
      LinkedHashMap<java.lang.String, java.lang.Object> var8 = new LinkedHashMap();
      var8.put("ts", var6);
      var8.put("rulebookSha", var4.n().c());
      var8.put("schemaSha", var4.i().get("schema"));
      var8.put("scenariosSha", var4.i().get("scenarios"));
      if (var4.i().get("generator") != null) {
         var8.put("generatorSha", var4.i().get("generator"));
      }

      if (var4.i().get("twin") != null) {
         var8.put("twinSha", var4.i().get("twin"));
      }

      if (var4.i().get("sequences") != null) {
         var8.put("sequencesSha", var4.i().get("sequences"));
      }

      var8.put("perScenario", var5.a);
      ArrayList<java.util.LinkedHashMap> var9 = new ArrayList(var5.b.size());

      for (Map var11 : var5.b) {
         LinkedHashMap<java.lang.String, java.lang.Object> var12 = new LinkedHashMap();
         var12.put("name", var11.get("name"));
         var12.put("status", var11.get("status"));
         var9.add(var12);
      }

      var8.put("invariants", var9);
      var8.put("rejects", var5.c);
      if (var5.d != null) {
         var8.put("sequences", var5.d);
      }

      String var13 = Json.stringifyStrict(var8);
      a(var3, var5.a, var13);
      c(a(var0, var3), var13);
      LinkedHashMap<java.lang.String, java.lang.Object> var14 = new LinkedHashMap();
      var14.put("rule", var3);
      var14.put("ts", var6);
      var14.put("scenarios", var5.a.size());
      var14.put("rejects", var5.c.size());
      var14.put("invariants", var9.size());
      if (var5.d != null) {
         var14.put("sequences", var5.d.size());
      }

      return var14;
   }

   private static void a(String var0, List<Map<String, Object>> var1, String var2) {
      try {
         Json.parseStrict(var2, true);
      } catch (RuntimeException var8) {
         ArrayList<java.lang.String> var3 = new ArrayList();

         for (Map var5 : var1) {
            try {
               Json.parseStrict(Json.stringifyStrict(var5), true);
            } catch (RuntimeException var7) {
               var3.add(String.valueOf(var5.get("scenarioId")));
            }
         }

         throw new RuleApiException(
            422,
            "BASELINE_UNSERIALIZABLE",
            "rulebook '"
               + var0
               + "' computes an output that is not representable as JSON (a NaN)"
               + (var3.isEmpty() ? "" : " on scenario(s) " + var3)
               + " — the baseline would be written unreadable and every later Rule.drift would refuse it. Fix calc.js so every output is a finite number, then bless again"
         );
      }
   }

   static Map<String, Object> b(Path var0, RulebookRepository var1, RuleEvaluator var2, String var3) {
      io.karatelabs.ext.rules.RulebookRepository.RulebookDocument var4 = var1.b(var3);
      Path var5 = a(var0, var3);
      var var6 = a(var5);
      LinkedHashMap<java.lang.String, java.lang.Object> var7 = new LinkedHashMap();
      var7.put("rule", var3);
      if (var6 == null) {
         var7.put("changed", false);
         var7.put("firstRun", true);
         JsApi.hint(
            var7,
            "no verification baseline yet — call Rule.bless('"
               + var3
               + "') to capture the current outputs, outcomes, invariant statuses and reject verdicts as the reference this verb audits against"
         );
         return var7;
      } else {
         Object var8 = var6.get("ts");
         if (a(a(var6), var4.n().c())
            && a(var6.get("schemaSha"), var4.i().get("schema"))
            && a(var6.get("scenariosSha"), var4.i().get("scenarios"))
            && a(var6.get("generatorSha"), var4.i().get("generator"))
            && a(var6.get("twinSha"), var4.i().get("twin"))
            && a(var6.get("sequencesSha"), var4.i().get("sequences"))) {
            var7.put("changed", false);
            var7.put("unchanged", true);
            var7.put("baselineTs", var8);
            return var7;
         } else {
            boolean var9 = !a(var6.get("schemaSha"), var4.i().get("schema"));
            boolean var10 = !a(var6.get("scenariosSha"), var4.i().get("scenarios"));
            io.karatelabs.ext.rules.RuleVerificationService.VerificationSnapshot var11 = a(var1, var4, var2);
            var var12 = a(f(var6.get("perScenario")), "scenarioId");
            var var13 = a(var11.a, "scenarioId");
            var var14 = a(f(var6.get("rejects")), "scenarioId");
            var var15 = a(var11.c, "scenarioId");
            var var16 = a(f(var6.get("invariants")), "name");
            var var17 = a(var11.b, "name");
            ArrayList var18 = new ArrayList();
            ArrayList var19 = new ArrayList();
            a(var12, var13, var18, var19);
            a(var14, var15, var18, var19);
            a(var16, var17, var18, var19);
            ArrayList<java.lang.String> var20 = new ArrayList();
            ArrayList<java.util.LinkedHashMap> var21 = new ArrayList();
            ArrayList<java.util.LinkedHashMap> var22 = new ArrayList();

            for (Entry var24 : (Iterable<Entry>) (Iterable<?>) (var13.entrySet())) {
               Map<java.lang.String, Object> var25 = (Map)var12.get(var24.getKey());
               if (var25 != null) {
                  Map<java.lang.String, Object> var26 = (Map)var24.getValue();
                  if (var10 && !a(var25.get("inputSha"), var26.get("inputSha"))) {
                     var20.add((String)var24.getKey());
                  } else {
                     if (!b(var25).equals(b(var26))) {
                        LinkedHashMap<java.lang.String, java.lang.Object> var27 = new LinkedHashMap();
                        var27.put("scenarioId", var24.getKey());
                        var27.put("prevOutput", e(var25));
                        var27.put("output", e(var26));
                        var27.put("wordDiff", RuleTreeJsonMapper.a(RuleTreeDiffer.a(c(var25), c(var26))));
                        var21.add(var27);
                     }

                     String var38 = f(var25);
                     String var28 = f(var26);
                     boolean var29 = !a(e(var25.get("outcomeKey")), e(var26.get("outcomeKey")));
                     if (!a((Object)var38, (Object)var28) || var29) {
                        LinkedHashMap<java.lang.String, java.lang.Object> var30 = new LinkedHashMap();
                        var30.put("scenarioId", var24.getKey());
                        if (Boolean.TRUE.equals(var25.get("outcomeSet"))) {
                           var30.put("prevOutcome", var25.get("outcome"));
                        }

                        if (Boolean.TRUE.equals(var26.get("outcomeSet"))) {
                           var30.put("outcome", var26.get("outcome"));
                        }

                        if (var29) {
                           var30.put("prevPath", var25.get("outcomeKey"));
                           var30.put("path", var26.get("outcomeKey"));
                        }

                        var22.add(var30);
                     }
                  }
               }
            }

            ArrayList<java.util.LinkedHashMap> var31 = new ArrayList();
            ArrayList<java.util.LinkedHashMap> var32 = new ArrayList();

            for (Entry var35 : (Iterable<Entry>) (Iterable<?>) (var17.entrySet())) {
               Map<java.lang.String, Object> var39 = (Map)var16.get(var35.getKey());
               if (var39 != null) {
                  String var42 = String.valueOf(var39.get("status"));
                  String var45 = String.valueOf(((Map)var35.getValue()).get("status"));
                  if (!var42.equals(var45)) {
                     LinkedHashMap<java.lang.String, java.lang.Object> var48 = new LinkedHashMap();
                     var48.put("name", var35.getKey());
                     var48.put("prevStatus", var42);
                     var48.put("status", var45);
                     if (a(var42, var45)) {
                        if (((Map)var35.getValue()).get("counterexample") != null) {
                           var48.put("counterexample", ((Map)var35.getValue()).get("counterexample"));
                        }

                        var31.add(var48);
                     } else {
                        var32.add(var48);
                     }
                  }
               }
            }

            ArrayList<java.util.LinkedHashMap> var34 = new ArrayList();

            for (Entry var40 : (Iterable<Entry>) (Iterable<?>) (var15.entrySet())) {
               Map<java.lang.String, Object> var43 = (Map)var14.get(var40.getKey());
               if (var43 != null) {
                  if (var10 && !a(var43.get("inputSha"), ((Map)var40.getValue()).get("inputSha"))) {
                     var20.add((String)var40.getKey());
                  } else if (!a(var43.get("status"), ((Map)var40.getValue()).get("status"))) {
                     LinkedHashMap<java.lang.String, java.lang.Object> var46 = new LinkedHashMap();
                     var46.put("scenarioId", var40.getKey());
                     var46.put("prevStatus", var43.get("status"));
                     var46.put("status", ((Map)var40.getValue()).get("status"));
                     var34.add(var46);
                  }
               }
            }

            boolean var37 = var6.get("sequences") instanceof List;
            List var41 = !var37 ? List.of() : a(var6, var11.d, var18, var19, var20);
            boolean var44 = !var31.isEmpty() || !var21.isEmpty() || !var22.isEmpty() || !var34.isEmpty() || !var41.isEmpty();
            boolean var47 = a(var6, var4);
            var7.put("changed", var44);
            var7.put("baselineTs", var8);
            if (var9) {
               var7.put("stale", true);
            }

            if (var47) {
               var7.put("twinEdited", true);
            }

            var7.put("invariantRegressions", var31);
            var7.put("invariantImprovements", var32);
            var7.put("outputDrift", var21);
            var7.put("outcomeDrift", var22);
            var7.put("rejectDrift", var34);
            if (var37) {
               var7.put("sequenceDrift", var41);
            }

            var7.put("added", var18);
            var7.put("removed", var19);
            var7.put("edited", var20);
            if (var44) {
               var7.put(
                  "next",
                  "the meaning moved under a still-green suite: read each entry, decide whether the rules or the expectation is wrong, fix the reviewed artifact, then Rule.bless('"
                     + var3
                     + "') to accept the new behaviour as the reference"
               );
            } else if (var47) {
               var7.put(
                  "next",
                  "twin.js moved since the baseline was blessed and every pinned leg is clean — the pinned sequences are a slice of the model, not its contract, so review the edit (Twin.check('"
                     + var3
                     + "')) and Rule.bless('"
                     + var3
                     + "')"
               );
            }

            return var7;
         }
      }
   }

   private static boolean a(Map<String, Object> var0, io.karatelabs.ext.rules.RulebookRepository.RulebookDocument var1) {
      return var0.get("twinSha") instanceof String && !a(var0.get("twinSha"), var1.i().get("twin"));
   }

   private static boolean a(String var0, String var1) {
      return !"NOTCHECKED".equals(var1) ? "VIOLATED".equals(var1) : "COMPLETE".equals(var0) || "HOLDS".equals(var0) || "VIOLATED".equals(var0);
   }

   private static io.karatelabs.ext.rules.RuleVerificationService.VerificationSnapshot a(RulebookRepository var0, io.karatelabs.ext.rules.RulebookRepository.RulebookDocument var1, RuleEvaluator var2) {
      var var3 = RuleAnalysisEngine.a(var1);
      if (var3.get("propertyErrors") instanceof List var4 && !var4.isEmpty()) {
         throw new RuleApiException(
               400,
               "PROPERTY_INVALID",
               "rulebook '"
                  + var1.d()
                  + "' has property authoring defects — the verification baseline is keyed by property name, so fix calc.js first: "
                  + var4
            )
            .a(Map.of("propertyErrors", var4));
      } else {
         var var12 = RuleInputSpaceExplorer.b(var1.g());
         var var13 = new ArrayList<>(var12.values());
         var13.sort(RuleCoverageMatrixBuilder::a);
         ArrayList<Map<String, Object>> var6 = new ArrayList<>();

         for (Map var8 : var1.b()) {
            var6.add(a(var1.g(), var1.m().e(), var8, var12, var13, var2));
         }

         ArrayList<Map<String, Object>> var14 = new ArrayList<>();

         for (Object var9 : f(var3.get("properties"))) {
            if (var9 instanceof Map var10 && "always".equals(var10.get("kind"))) {
               LinkedHashMap<java.lang.String, java.lang.Object> var11 = new LinkedHashMap();
               var11.put("name", String.valueOf(var10.get("name")));
               var11.put("status", String.valueOf(var10.get("status")));
               if (var10.get("counterexample") != null) {
                  var11.put("counterexample", var10.get("counterexample"));
               }

               var14.add(var11);
            }
         }

         ArrayList<Map<String, Object>> var16 = new ArrayList<>();

         for (Map var18 : var1.c()) {
            LinkedHashMap<java.lang.String, java.lang.Object> var19 = new LinkedHashMap();
            var19.put("scenarioId", String.valueOf(var18.get("_id")));
            var19.put("inputSha", d(var18));
            var19.put("status", Schema.rejectVerdict(var1.e(), var18).get("status"));
            var16.add(var19);
         }

         return new io.karatelabs.ext.rules.RuleVerificationService.VerificationSnapshot(
            var6,
            var14,
            var16,
            var1.k() == null
               ? null
               : a(
                  var0,
                  var1,
                  io.karatelabs.ext.rules.twin.TwinModelLoader.a(var1.k()),
                  io.karatelabs.ext.common.SandboxExecutor.a("KARATE_RULES_TWIN_WALK_TIMEOUT_MS", 630000L),
                  new ArrayList<>()
               )
         );
      }
   }

   private static List<Map<String, Object>> a(RulebookRepository var0, io.karatelabs.ext.rules.RulebookRepository.RulebookDocument var1, io.karatelabs.ext.rules.twin.TwinModel var2, long var3, List<Map<String, Object>> var5) {
      ArrayList<java.util.Map<java.lang.String,java.lang.Object>> var6 = new ArrayList();
      if (var1.l() == null) {
         return var6;
      } else {
         try (
            TwinRuleRuntime var7 = new TwinRuleRuntime(var0, var1.d());
            io.karatelabs.ext.common.SandboxExecutor.SandboxExecutionScope var8 = new io.karatelabs.ext.common.SandboxExecutor.SandboxExecutionScope("twin-walk", var3);
         ) {
            var8.a(() -> a(var1, var2, var7, var6, var5), Twin.MAPPER);
         }

         return var6;
      }
   }

   private static List<Map<String, Object>> a(
      io.karatelabs.ext.rules.RulebookRepository.RulebookDocument var0, io.karatelabs.ext.rules.twin.TwinModel var1, TwinRuleRuntime var2, List<Map<String, Object>> var3, List<Map<String, Object>> var4
   ) {
      for (Object var6 : f(var0.l().get("sequences"))) {
         var var7 = c(var6);
         String var8 = String.valueOf(var7.get("_id"));
         Object var9 = var7.get("steps");

         io.karatelabs.ext.rules.twin.ReplayTrace var10;
         try {
            var10 = io.karatelabs.ext.rules.twin.TwinWalker.a(var1, a(var8, var9), var2);
         } catch (io.karatelabs.ext.rules.twin.TwinException var14) {
            throw b(var8, var14.getMessage());
         }

         ArrayList<java.lang.String> var11 = new ArrayList(var10.a().size());

         for (io.karatelabs.ext.rules.twin.ReplayTrace.ReplayStep var13 : var10.a()) {
            var11.add(var13.c());
         }

         io.karatelabs.ext.rules.twin.TransitionPairIndex.a(var1, var8, var10, var4);
         LinkedHashMap<java.lang.String, java.lang.Object> var15 = new LinkedHashMap();
         var15.put("sequenceId", var8);
         var15.put("inputSha", a(var9));
         var15.put("dispositions", var11);
         Object var16 = var10.b();
         var15.put("finalSha", io.karatelabs.ext.rules.twin.CanonicalJson.a(io.karatelabs.ext.rules.twin.CanonicalJson.c(var16)));
         var15.put("finalKeys", b(var16));
         var3.add(var15);
      }

      return var3;
   }

   private static List<String> b(Object var0) {
      ArrayList<java.lang.String> var1 = new ArrayList();
      if (var0 instanceof Map var2) {
         a((Map<?, ?>)var2, "", var1);
      }

      var1.sort(io.karatelabs.ext.rules.twin.CanonicalJson::a);
      return var1;
   }

   private static void a(Map<?, ?> var0, String var1, List<String> var2) {
      for (Entry var4 : var0.entrySet()) {
         String var5 = var1 + "/" + String.valueOf(var4.getKey()).replace("~", "~0").replace("/", "~1");
         var2.add(var5);
         if (var4.getValue() instanceof Map var6) {
            a((Map<?, ?>)var6, var5, var2);
         }
      }
   }

   public static String a(Object var0) {
      return a(d(var0));
   }

   private static List<Map<String, Object>> a(String var0, Object var1) {
      if (var1 instanceof List var2) {
         ArrayList<java.util.Map<java.lang.String,java.lang.Object>> var3 = new ArrayList(var2.size());

         for (Object var5 : var2) {
            if (!(var5 instanceof Map)) {
               throw b(var0, "steps: every entry must be an object");
            }

            var3.add(c(var5));
         }

         return var3;
      } else {
         throw b(var0, "steps: must be an array");
      }
   }

   private static RuleApiException b(String var0, String var1) {
      return new RuleApiException(400, "SEQUENCE_INVALID", "sequence '" + var0 + "' cannot be replayed against the current twin.js (" + var1 + ")")
         .a(Map.of("id", var0, "why", String.valueOf(var1)));
   }

   private static List<Map<String, Object>> a(Map<String, Object> var0, List<Map<String, Object>> var1, List<String> var2, List<String> var3, List<String> var4) {
      ArrayList<java.util.Map<java.lang.String,java.lang.Object>> var5 = new ArrayList();
      var var6 = a(f(var0.get("sequences")), "sequenceId");
      var var7 = a(var1 == null ? List.of() : var1, "sequenceId");
      a(var6, var7, var2, var3);

      for (String var9 : (Iterable<String>) (Iterable<?>) (new TreeSet(var7.keySet()))) {
         Map<java.lang.String, Object> var10 = (Map)var6.get(var9);
         if (var10 != null) {
            Map<java.lang.String, Object> var11 = (Map)var7.get(var9);
            if (!a(var10.get("inputSha"), var11.get("inputSha"))) {
               var4.add(var9);
            } else {
               var5.addAll(a(var9, var10, var11));
            }
         }
      }

      return var5;
   }

   static Map<String, Object> a(Path var0, RulebookRepository var1, io.karatelabs.ext.rules.RulebookRepository.RulebookDocument var2, io.karatelabs.ext.rules.twin.TwinModel var3) {
      return a(var0, var1, var2, var3, io.karatelabs.ext.common.SandboxExecutor.a("KARATE_RULES_TWIN_WALK_TIMEOUT_MS", 630000L));
   }

   static Map<String, Object> a(Path var0, RulebookRepository var1, io.karatelabs.ext.rules.RulebookRepository.RulebookDocument var2, io.karatelabs.ext.rules.twin.TwinModel var3, long var4) {
      var var6 = a(a(var0, var2.d()));
      boolean var7 = var6 != null && var6.get("sequences") instanceof List;
      ArrayList var8 = new ArrayList();
      List var9 = a(var1, var2, var3, var4, var8);
      LinkedHashMap<java.lang.String, java.lang.Object> var10 = new LinkedHashMap();
      var10.put("blessed", var7);
      var10.put("twinEdited", var6 != null && a(var6, var2));
      var10.put("sequenceDrift", !var7 ? List.of() : a(var6, var9, new ArrayList<>(), new ArrayList<>(), new ArrayList<>()));
      var10.put("sequencePairs", var8);
      return var10;
   }

   private static List<Map<String, Object>> a(String var0, Map<String, Object> var1, Map<String, Object> var2) {
      ArrayList<java.util.Map<java.lang.String,java.lang.Object>> var3 = new ArrayList();
      var var4 = f(var1.get("dispositions"));
      var var5 = f(var2.get("dispositions"));

      for (int var6 = 0; var6 < Math.max(var4.size(), var5.size()); var6++) {
         Object var7 = var6 < var4.size() ? var4.get(var6) : null;
         Object var8 = var6 < var5.size() ? var5.get(var6) : null;
         if (!a(var7, var8)) {
            var3.add(a(var0, var6, var7, var8));
         }
      }

      if (!a(var1.get("finalSha"), var2.get("finalSha"))) {
         var var9 = a(var0, "final", var1.get("finalSha"), var2.get("finalSha"));
         a(var9, var1.get("finalKeys"), var2.get("finalKeys"));
         var3.add(var9);
      }

      return var3;
   }

   private static void a(Map<String, Object> var0, Object var1, Object var2) {
      if (var1 instanceof List var3 && var2 instanceof List var4) {
         ArrayList var5 = new ArrayList(var4);
         var5.removeAll(var3);
         ArrayList var6 = new ArrayList(var3);
         var6.removeAll(var4);
         if (!var5.isEmpty() || !var6.isEmpty()) {
            LinkedHashMap<java.lang.String, java.util.ArrayList> var7 = new LinkedHashMap();
            var7.put("added", var5);
            var7.put("removed", var6);
            var0.put("shape", var7);
         }
      }
   }

   private static Map<String, Object> a(String var0, Object var1, Object var2, Object var3) {
      LinkedHashMap<java.lang.String, java.lang.Object> var4 = new LinkedHashMap();
      var4.put("sequenceId", var0);
      var4.put("at", var1);
      var4.put("prev", var2);
      var4.put("now", var3);
      return var4;
   }

   private static Map<String, Object> c(Object var0) {
      return (Map<String, Object>)var0;
   }

   private static Map<String, Object> a(
      String var0, Map<String, Object> var1, Map<String, Object> var2, Map<Integer, String> var3, List<String> var4, RuleEvaluator var5
   ) {
      LinkedHashMap<java.lang.String, java.lang.Object> var6 = new LinkedHashMap();
      var6.put("scenarioId", String.valueOf(var2.get("_id")));
      var6.put("inputSha", d(var2));
      BranchOutcomeCollector var7 = new BranchOutcomeCollector();

      try {
         io.karatelabs.ext.rules.RuleEvaluator.RuleEvaluationResult var8 = var5.a(var0, var1, var2, var7);
         var6.put("output", var8.b());
         var6.put("outcome", var8.f());
         var6.put("outcomeSet", var8.g());
         var6.put("outcomeKey", RuleCoverageMatrixBuilder.a(var4, var7.a(var3)));
      } catch (RuleApiException var9) {
         if (RuleAnalysisEngine.a(var9.b)) {
            throw var9;
         }

         var6.put("error", var9.getMessage() == null ? var9.b : var9.getMessage());
         var6.put("outcomeSet", false);
      }

      return var6;
   }

   private static Map<String, Object> a(Path var0) {
      if (!Files.isRegularFile(var0)) {
         return null;
      } else {
         Object var1;
         try {
            var1 = Json.parseStrict(Files.readString(var0, StandardCharsets.UTF_8), true);
         } catch (RuntimeException | IOException var4) {
            throw b(var0, var4.toString());
         }

         if (var1 instanceof Map var2) {
            a(var0, var2);
            return var2;
         } else {
            throw b(var0, "not a verification baseline document");
         }
      }
   }

   private static Object a(Map<?, ?> var0) {
      return var0.containsKey("rulebookSha") ? var0.get("rulebookSha") : var0.get("calcSha");
   }

   private static void a(Path var0, Map<?, ?> var1) {
      if (var1.get("ts") instanceof Number
         && a(var1) instanceof String
         && var1.get("schemaSha") instanceof String
         && var1.get("scenariosSha") instanceof String
         && a(var1, "generatorSha")
         && a(var1, "twinSha")
         && a(var1, "sequencesSha")) {
         LinkedHashSet var2 = new LinkedHashSet();

         for (Map var4 : a(var0, var1.get("perScenario"), "perScenario")) {
            String var5 = a(var0, var4, "scenarioId", "perScenario");
            a(var0, var4, "inputSha", "perScenario");
            if (!(var4.get("outcomeSet") instanceof Boolean)) {
               throw b(var0, "perScenario '" + var5 + "' has no outcomeSet flag");
            }

            if (!(var4.get("error") instanceof String) && !(var4.get("outcomeKey") instanceof String)) {
               throw b(var0, "perScenario '" + var5 + "' carries neither an outcomeKey nor the error of a row that threw");
            }

            a(var0, var2, var5, "row id");
         }

         for (Map var16 : a(var0, var1.get("rejects"), "rejects")) {
            String var19 = a(var0, var16, "scenarioId", "rejects");
            a(var0, var16, "inputSha", "rejects");
            String var6 = a(var0, var16, "status", "rejects");
            if (!g.contains(var6)) {
               throw b(var0, "reject row '" + var19 + "' has the unknown verdict '" + var6 + "' (expected one of " + g + ")");
            }

            a(var0, var2, var19, "row id");
         }

         LinkedHashSet var15 = new LinkedHashSet();

         for (Map var20 : a(var0, var1.get("invariants"), "invariants")) {
            String var22 = a(var0, var20, "name", "invariants");
            String var7 = a(var0, var20, "status", "invariants");
            if (!f.contains(var7)) {
               throw b(var0, "invariant '" + var22 + "' has the unknown status '" + var7 + "' (expected one of " + f + ")");
            }

            a(var0, var15, var22, "invariant name");
         }

         if (var1.containsKey("sequences")) {
            LinkedHashSet var18 = new LinkedHashSet();

            for (Map var23 : a(var0, var1.get("sequences"), "sequences")) {
               String var24 = a(var0, var23, "sequenceId", "sequences");
               a(var0, var23, "inputSha", "sequences");
               a(var0, var23, "finalSha", "sequences");
               Object var9 = var23.get("dispositions");
               if (!(var9 instanceof List)) {
                  throw b(var0, "sequence '" + var24 + "' has no dispositions vector");
               }

               for (Object var10 : (List)var9) {
                  if (!(var10 instanceof String)) {
                     throw b(var0, "sequence '" + var24 + "' has a disposition that is not an outcome string");
                  }
               }

               if (var23.containsKey("finalKeys")) {
                  if (!(var23.get("finalKeys") instanceof List var26)) {
                     throw b(var0, "sequence '" + var24 + "' has a finalKeys that is not a list of JSON-Pointer paths");
                  }

                  String var28 = null;

                  for (Object var12 : var26) {
                     if (!(var12 instanceof String var13) || !var13.startsWith("/") || var13.matches(".*~(?![01]).*")) {
                        throw b(var0, "sequence '" + var24 + "' has a finalKeys entry that is not a JSON-Pointer path");
                     }

                     if (var28 != null && io.karatelabs.ext.rules.twin.CanonicalJson.a(var28, var13) >= 0) {
                        throw b(var0, "sequence '" + var24 + "' has finalKeys out of canonical order or duplicated");
                     }

                     var28 = var13;
                  }
               }

               a(var0, var18, var24, "sequence id");
            }
         }
      } else {
         throw b(var0, "the header (ts + the file shas) is missing or mistyped");
      }
   }

   private static boolean a(Map<?, ?> var0, String var1) {
      return !var0.containsKey(var1) || var0.get(var1) instanceof String;
   }

   private static List<Map<?, ?>> a(Path var0, Object var1, String var2) {
      if (var1 instanceof List var3) {
         ArrayList<java.util.Map<?,?>> var4 = new ArrayList(var3.size());

         for (Object var6 : var3) {
            if (!(var6 instanceof Map var7)) {
               throw b(var0, "a '" + var2 + "' entry is not an object");
            }

            var4.add(var7);
         }

         return var4;
      } else {
         throw b(var0, "'" + var2 + "' is missing or is not a list");
      }
   }

   private static String a(Path var0, Map<?, ?> var1, String var2, String var3) {
      if (var1.get(var2) instanceof String var4 && !var4.isEmpty()) {
         return var4;
      } else {
         throw b(var0, "a '" + var3 + "' entry has no " + var2);
      }
   }

   private static void a(Path var0, Set<String> var1, String var2, String var3) {
      if (!var1.add(var2)) {
         throw b(var0, "duplicate " + var3 + " '" + var2 + "' — the join is by id, so one entry would silently shadow the other");
      }
   }

   private static RuleApiException b(Path var0, String var1) {
      return new RuleApiException(
            409,
            "BASELINE_UNREADABLE",
            "the verification baseline at "
               + var0
               + " cannot be read ("
               + var1
               + ") — re-capture it with Rule.bless(); reading it as 'no baseline' would silently drop every drift signal it held"
         )
         .a(Map.of("path", var0.toString(), "why", var1));
   }

   private static void c(Path var0, String var1) {
      try {
         Files.createDirectories(var0.getParent());
         Path var2 = io.karatelabs.ext.common.TempFileCreator.a(var0.getParent(), "verification", ".tmp");

         try {
            Files.writeString(var2, var1, StandardCharsets.UTF_8);

            try {
               Files.move(var2, var0, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException var8) {
               Files.move(var2, var0, StandardCopyOption.REPLACE_EXISTING);
            }
         } finally {
            Files.deleteIfExists(var2);
         }
      } catch (IOException var10) {
         throw new UncheckedIOException(var10);
      }
   }

   private static String d(Object var0) {
      LinkedHashMap<java.lang.String, java.lang.Object> var1 = new LinkedHashMap();
      var1.put("v", var0);
      String var2 = Json.stringifyStrict(var1);

      try {
         return Json.stringifyStrict(Json.parseStrict(var2, true));
      } catch (RuntimeException var4) {
         return var2;
      }
   }

   private static String b(Map<String, Object> var0) {
      return var0.get("error") != null ? "ERROR: " + var0.get("error") : d(var0.get("output"));
   }

   private static String c(Map<String, Object> var0) {
      return var0.get("error") != null ? "ERROR: " + var0.get("error") : Json.stringifyStrict(var0.get("output"));
   }

   private static String e(Object var0) {
      if (var0 instanceof String var1 && !var1.isEmpty()) {
         StringBuilder var2 = new StringBuilder();

         for (String var6 : var1.split(";")) {
            int var7 = var6.lastIndexOf(61);
            var2.append(var7 < 0 ? var6 : var6.substring(var7 + 1)).append(';');
         }

         return var2.toString();
      } else {
         return var0 == null ? null : "";
      }
   }

   private static String d(Map<String, Object> var0) {
      return a(d(RulebookRepository.d(var0)));
   }

   private static Object e(Map<String, Object> var0) {
      return var0.get("error") != null ? "ERROR: " + var0.get("error") : var0.get("output");
   }

   private static String f(Map<String, Object> var0) {
      return Boolean.TRUE.equals(var0.get("outcomeSet")) ? d(var0.get("outcome")) : null;
   }

   private static void a(Map<String, Map<String, Object>> var0, Map<String, Map<String, Object>> var1, List<String> var2, List<String> var3) {
      for (String var5 : var1.keySet()) {
         if (!var0.containsKey(var5)) {
            var2.add(var5);
         }
      }

      for (String var7 : var0.keySet()) {
         if (!var1.containsKey(var7)) {
            var3.add(var7);
         }
      }
   }

   private static Map<String, Map<String, Object>> a(List<?> var0, String var1) {
      LinkedHashMap<java.lang.String, java.util.Map<java.lang.String,java.lang.Object>> var2 = new LinkedHashMap();

      for (Object var4 : var0) {
         if (var4 instanceof Map var5 && var5.get(var1) != null) {
            var2.put(String.valueOf(var5.get(var1)), var5);
         }
      }

      return var2;
   }

   private static List<?> f(Object var0) {
      return var0 instanceof List var1 ? var1 : List.of();
   }

   private static boolean a(Object var0, Object var1) {
      return var0 == null ? var1 == null : var0.equals(var1);
   }

   private static String a(String var0) {
      return io.karatelabs.ext.common.Sha256.a(var0);
   }

   private record VerificationSnapshot(List<Map<String, Object>> a, List<Map<String, Object>> b, List<Map<String, Object>> c, List<Map<String, Object>> d) {

      private VerificationSnapshot(List<Map<String, Object>> a, List<Map<String, Object>> b, List<Map<String, Object>> c, List<Map<String, Object>> d) {
         this.a = a;
         this.b = b;
         this.c = c;
         this.d = d;
      }
   }
}
