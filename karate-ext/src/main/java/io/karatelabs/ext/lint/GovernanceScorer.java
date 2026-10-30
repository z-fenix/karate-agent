package io.karatelabs.ext.lint;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public final class GovernanceScorer {
   public static final Map<GovernanceDimension, Double> DEFAULT_WEIGHTS = createDefaultWeights();
   public static final String SCORING_VERSION = "1";

   private static Map<GovernanceDimension, Double> createDefaultWeights() {
      EnumMap var0 = new EnumMap<>(GovernanceDimension.class);
      var0.put(io.karatelabs.ext.lint.GovernanceDimension.PROVENNESS, 0.35);
      var0.put(io.karatelabs.ext.lint.GovernanceDimension.DESIGN, 0.2);
      var0.put(io.karatelabs.ext.lint.GovernanceDimension.SECURITY, 0.2);
      var0.put(io.karatelabs.ext.lint.GovernanceDimension.DOCUMENTATION, 0.1);
      var0.put(io.karatelabs.ext.lint.GovernanceDimension.CONTRACT, 0.15);
      return Collections.unmodifiableMap(var0);
   }

   private GovernanceScorer() {
   }

   public static Map<String, Object> score(Map<String, Object> var0, Object var1) {
      return score(var0, var1, null);
   }

   public static Map<String, Object> score(Map<String, Object> var0, Object var1, Map<String, Object> var2) {
      List<Map<String, Object>> var3 = (List)var0.getOrDefault("rules", List.of());
      Map<String, Object> var4 = (Map)var0.getOrDefault("summary", Map.of());
      var var5 = resolveWeights(var1);
      EnumMap var6 = new EnumMap<>(GovernanceDimension.class);

      for (GovernanceDimension var10 : GovernanceDimension.values()) {
         var6.put(var10, scoreDimension(var10, var3));
      }

      if (var2 != null) {
         var6.put(io.karatelabs.ext.lint.GovernanceDimension.CONTRACT, contractDimension(var2));
      }

      ArrayList<java.lang.String> var18 = new ArrayList();
      double var19 = 0.0;

      for (GovernanceDimension var13 : GovernanceDimension.values()) {
         if (Boolean.TRUE.equals(((Map)var6.get(var13)).get("measured"))) {
            var19 += var5.get(var13);
         } else {
            var18.add(var13.wire());
         }
      }

      LinkedHashMap<java.lang.String, java.util.Map> var21 = new LinkedHashMap();

      for (GovernanceDimension var14 : GovernanceDimension.values()) {
         Map<java.lang.String, java.lang.Double> var15 = (Map)var6.get(var14);
         var15.put("weight", var5.get(var14));
         var15.put("effectiveWeight", Boolean.TRUE.equals(var15.get("measured")) && var19 > 0.0 ? roundScore((Double)var5.get(var14) / var19) : null);
         var21.put(var14.wire(), var15);
      }

      Double var23 = weightedIndex(var6, var5, var19);
      List var25 = collectGates(var0, var3);
      io.karatelabs.ext.lint.GovernanceScorer.MaturityAssessment var27 = maturity(var6, var3, var25);
      String var28 = verdict(var25, var4, var6);
      LinkedHashMap<java.lang.String, java.lang.Object> var29 = new LinkedHashMap();
      var29.put("verdict", var28);
      if ("CONDITIONAL".equals(var28) && !var18.isEmpty()) {
         var29.put(
            "verdictNote",
            "READY needs every dimension measured, and "
               + String.join(", ", var18)
               + " "
               + (var18.size() == 1 ? "was" : "were")
               + " not — so this is CONDITIONAL on missing evidence, not on an open finding."
         );
      }

      var29.put("index", var23);
      var29.put("level", maturityLevelDetails(var27));
      var29.put("dimensions", var21);
      var29.put("notMeasured", var18);
      var29.put("gates", var25);
      var29.put("waived", waivedSummary(var4));
      if (Boolean.TRUE.equals(((Map)var6.get(io.karatelabs.ext.lint.GovernanceDimension.CONTRACT)).get("rehearsal"))) {
         LinkedHashMap<java.lang.String, java.lang.String> var16 = new LinkedHashMap();
         var16.put("dimension", io.karatelabs.ext.lint.GovernanceDimension.CONTRACT.wire());
         var16.put(
            "note",
            "The `contract` score inside this index rests on a REHEARSAL pair — the provider leg was answered by a karate mock, so that dimension measures agreement between two of our own implementations, not a real provider. Real evidence at its rung; not a provider claim. Substitute a real provider and re-mint to clear this."
         );
         var29.put("rehearsal", var16);
      }

      var29.put("weights", weightsReport(var5, var19, var18));
      LinkedHashMap<java.lang.String, java.lang.Object> var30 = new LinkedHashMap();
      var30.put("available", false);
      var30.put(
         "note",
         "no model-judged signal is in this grade. Every number above is deterministic — no LLM, no network, no run — so it can be attested and re-derived from the same inputs. When a judged residue ships it advises beside this grade, never inside it."
      );
      var29.put("judged", var30);
      LinkedHashMap<java.lang.String, java.lang.Object> var17 = new LinkedHashMap();
      var17.put("version", "1");
      var17.put("precedence", List.of("score", "cap", "mean", "gate", "ladder"));
      if (var0.get("ruleset") != null) {
         var17.put("ruleset", var0.get("ruleset"));
      }

      var29.put("scoring", var17);
      return var29;
   }

   private static Map<String, Object> scoreDimension(GovernanceDimension var0, List<Map<String, Object>> var1) {
      double var2 = 0.0;
      double var4 = 0.0;
      int var6 = 0;
      int var7 = 0;
      Double var8 = null;
      ArrayList<java.lang.String> var9 = new ArrayList();
      ArrayList<java.lang.String> var10 = new ArrayList();

      for (Map var12 : var1) {
         if (var0.wire().equals(var12.get("dimension"))) {
            var10.add(String.valueOf(var12.get("id")));
            if (!Boolean.TRUE.equals(var12.get("graded"))) {
               var7++;
            } else {
               int var13 = intValue(var12, "selected");
               int var14 = Math.min(intValue(var12, "failed"), var13);
               if (var13 == 0) {
                  var7++;
               } else {
                  double var15 = severityWeight(String.valueOf(var12.get("severity")));
                  var2 += var15 * (1.0 - (double)var14 / var13);
                  var4 += var15;
                  var6++;
                  if (var12.get("cap") instanceof Number var18 && var14 > 0) {
                     var8 = var8 == null ? var18.doubleValue() : Math.min(var8, var18.doubleValue());
                     var9.add(String.valueOf(var12.get("id")));
                  }
               }
            }
         }
      }

      LinkedHashMap<java.lang.String, java.lang.Object> var19 = new LinkedHashMap();
      var19.put("label", var0.label());
      boolean var20 = var4 > 0.0;
      var19.put("measured", var20);
      double var21 = var20 ? var2 / var4 : 0.0;
      double var22 = var8 != null ? Math.min(var21, var8) : var21;
      var19.put("score", var20 ? roundScore(var22) : null);
      if (var20 && var8 != null) {
         var19.put("cappedAt", var8);
         var19.put("cappedBy", var9);
         var19.put("uncappedScore", roundScore(var21));
      }

      var19.put("rules", var10);
      var19.put("gradedRules", var6);
      if (var7 > 0) {
         var19.put("ungradedRules", var7);
      }

      if (!var20) {
         var19.put("note", unmeasuredDimensionNote(var0, var10.isEmpty()));
      }

      if (var20 && var0 == io.karatelabs.ext.lint.GovernanceDimension.PROVENNESS) {
         var19.put(
            "carries",
            "This score measures exercised-ness, not the covering run's own outcome. The coverage graph records that an operation was exercised and by what test, not whether that test passed. Read it beside the run's own pass/fail, in the report the run wrote: a suite that was itself half red still lights this dimension. What IS measured beyond exercised-ness: the error-path (sad-path) grade — Coverage.gaps() lists happyOnly items and Coverage.status(id) carries outcome.declaredUntested, the declared response codes no run ever produced."
         );
      }

      return var19;
   }

   private static Map<String, Object> contractDimension(Map<String, Object> var0) {
      LinkedHashMap<java.lang.String, java.lang.Object> var1 = new LinkedHashMap();
      boolean var2 = Boolean.TRUE.equals(var0.get("measured"));
      var1.put("label", io.karatelabs.ext.lint.GovernanceDimension.CONTRACT.label());
      var1.put("measured", var2);
      if (!var2) {
         var1.put("score", null);
         copyFields(var1, var0, "pair", "freshness", "changed", "unchecked");
         var1.put("note", var0.get("note"));
         return var1;
      } else {
         var1.put("score", var0.get("score"));
         var1.put("rung", var0.get("rung"));
         var1.put("rungName", var0.get("name"));
         var1.put("pair", var0.get("pair"));
         var1.put("freshness", var0.get("freshness"));
         copyFields(var1, var0, "unchecked", "changed", "waivedBars", "restsOnWaiver", "rehearsal", "rehearsalNote", "rehearsalCheck");
         var1.put("claimSentence", var0.get("claimSentence"));
         if (var0.get("cappedBy") != null) {
            var1.put("cappedBy", var0.get("cappedBy"));
         }

         if (var0.get("blockedBy") != null) {
            var1.put("blockedBy", var0.get("blockedBy"));
         }

         String var3 = "This score is the paired run's rung, normalised (rung "
            + var0.get("rung")
            + " of 4). It is not a second reading of the same evidence: the ladder's bars are in the pair itself, each with the count that shows it was asked.";
         if (Boolean.TRUE.equals(var0.get("rehearsal"))) {
            var3 = var3
               + " AND IT IS A REHEARSAL: the provider leg was answered by a karate mock, so the rung measures agreement between two of our own implementations — real evidence at its rung, but not evidence about a real provider. Substitute a real provider and re-mint to clear this.";
         }

         var1.put("note", var3);
         return var1;
      }
   }

   private static boolean isRuleClean(List<Map<String, Object>> var0, String var1) {
      for (Map var3 : var0) {
         if (var1.equals(var3.get("id"))) {
            return Boolean.TRUE.equals(var3.get("graded")) && intValue(var3, "failed") == 0;
         }
      }

      return false;
   }

   private static void copyFields(Map<String, Object> var0, Map<String, Object> var1, String... var2) {
      for (String var6 : var2) {
         if (var1.get(var6) != null) {
            var0.put(var6, var1.get(var6));
         }
      }
   }

   private static String unmeasuredDimensionNote(GovernanceDimension var0, boolean var1) {
      if (var0 == io.karatelabs.ext.lint.GovernanceDimension.CONTRACT) {
         return "Not measured. No document check can reach this dimension: it takes a paired mock-vs-provider run, and no pair that is both about this spec and still fresh was found. Record one with Contract.pair({suite, spec, provider}). Until then the dimension is renormalised out of the index rather than counted against you, and it holds the top maturity level out of reach. With no provider to point at, the nearest evidence is the `operation-not-mock-only` rule: it says where only our own mock ever answered.";
      } else if (var0 == io.karatelabs.ext.lint.GovernanceDimension.PROVENNESS) {
         return "not measured — no project coverage graph was readable, so the graph-reaching rules asked nothing. That is INERT, not clean, and it is NOT a zero. Write the graph, then re-grade: call Report.aggregate() (with no runs yet it still renders the DECLARED universe — every operation as a gap — which measures this dimension honestly at its floor). EXPECT THE INDEX TO DROP when you do: an unmeasured dimension is renormalised out of the mean, and measuring it at its floor puts a low score INTO the mean. That is the number getting more honest, not worse. To lift it, get real evidence in — run your suite (Runner.suite('checks')) or probe the API inside a live run (Harness.startLive() → Http.* calls → Harness.stop()); on the console each settled run folds into the graph, so re-grade straight after.";
      } else {
         return var1
            ? "not measured — no rule graded this dimension, which is what you get from `rulepack:false` or a pack that replaced it. Declare one against it (a rule's `dimension` field, in cov.lintRules or opts.rules) and it grades."
            : "not measured — every rule on this dimension was out of scope, matched nothing, or could not run. Which, per rule, is in `lint.rules`: `graded:false` is a rule that blew up or had no graph to ask, `selected:0` is a selector that matched nothing, and a rule that threw is also a `ruleError` finding.";
      }
   }

   private static double severityWeight(String var0) {
      return switch (var0) {
         case "error" -> 3.0;
         case "warn" -> 2.0;
         default -> 1.0;
      };
   }

   private static Double weightedIndex(Map<GovernanceDimension, Map<String, Object>> var0, Map<GovernanceDimension, Double> var1, double var2) {
      if (var2 <= 0.0) {
         return null;
      } else {
         double var4 = 0.0;

         for (Entry var7 : var0.entrySet()) {
            Map<java.lang.String, java.lang.Number> var8 = (Map)var7.getValue();
            if (Boolean.TRUE.equals(var8.get("measured"))) {
               double var9 = ((Number)var8.get("score")).doubleValue();
               if (var9 <= 0.0) {
                  return 0.0;
               }

               var4 += var1.get(var7.getKey()) / var2 / var9;
            }
         }

         return roundScore(1.0 / var4);
      }
   }

   private static List<Map<String, Object>> collectGates(Map<String, Object> var0, List<Map<String, Object>> var1) {
      Map<String, Object> var2 = (Map)var0.getOrDefault("summary", Map.of());
      List<Map<String, Object>> var3 = (List)var0.getOrDefault("findings", List.of());
      ArrayList<java.util.Map<java.lang.String,java.lang.Object>> var4 = new ArrayList();
      int var5 = intValue(var2, "errors");
      if (var5 > 0) {
         var4.add(gate("unwaived-errors", var5 + " error-severity finding" + (var5 == 1 ? "" : "s") + " stand" + (var5 == 1 ? "s" : "") + " unwaived"));
      }

      long var6 = var3.stream().filter(var0x -> Boolean.TRUE.equals(var0x.get("ruleError"))).count();
      if (var6 > 0L) {
         var4.add(gate("rule-error", var6 + " rule" + (var6 == 1L ? "" : "s") + " failed to evaluate — the grade is over an incomplete run"));
      }

      if (var0.get("project") instanceof Map var8 && Boolean.FALSE.equals(var8.get("bootFileReadable"))) {
         var4.add(gate("project-policy-unreadable", String.valueOf(var8.get("note"))));
      }

      int var13 = intValue(var2, "rejectedWaivers");
      if (var13 > 0) {
         var4.add(
            gate(
               "rejected-waivers",
               var13 + " waiver" + (var13 == 1 ? " was" : "s were") + " refused — a team believing it holds an exception it does not hold is the worse failure"
            )
         );
      }

      for (Map var10 : var1) {
         if (var10.get("cap") instanceof Number var11 && Boolean.TRUE.equals(var10.get("graded")) && intValue(var10, "failed") > 0) {
            var4.add(gate("capped-dimension", "'" + var10.get("id") + "' caps the " + var10.get("dimension") + " dimension at " + var11));
         }
      }

      return var4;
   }

   private static Map<String, Object> gate(String var0, String var1) {
      LinkedHashMap<java.lang.String, java.lang.Object> var2 = new LinkedHashMap();
      var2.put("gate", var0);
      var2.put("detail", var1);
      return var2;
   }

   private static String verdict(List<Map<String, Object>> var0, Map<String, Object> var1, Map<GovernanceDimension, Map<String, Object>> var2) {
      if (!var0.isEmpty()) {
         return "NOT_READY";
      } else if (!Boolean.TRUE.equals(var1.get("clean"))) {
         return "CONDITIONAL";
      } else {
         for (Map var4 : var2.values()) {
            if (!Boolean.TRUE.equals(var4.get("measured"))) {
               return "CONDITIONAL";
            }
         }

         return "READY";
      }
   }

   private static io.karatelabs.ext.lint.GovernanceScorer.MaturityAssessment maturity(Map<GovernanceDimension, Map<String, Object>> var0, List<Map<String, Object>> var1, List<Map<String, Object>> var2) {
      if (!var2.isEmpty()) {
         return new io.karatelabs.ext.lint.GovernanceScorer.MaturityAssessment(io.karatelabs.ext.lint.GovernanceScorer.GovernanceMaturityLevel.UNGOVERNED, "a deterministic gate is open — see `gates` below");
      } else {
         String var3 = thresholdFailure(var0, io.karatelabs.ext.lint.GovernanceDimension.DOCUMENTATION, 0.7);
         if (var3 != null) {
            return new io.karatelabs.ext.lint.GovernanceScorer.MaturityAssessment(io.karatelabs.ext.lint.GovernanceScorer.GovernanceMaturityLevel.UNGOVERNED, var3);
         } else {
            var3 = thresholdFailure(var0, io.karatelabs.ext.lint.GovernanceDimension.DESIGN, 0.8);
            if (var3 == null) {
               var3 = thresholdFailure(var0, io.karatelabs.ext.lint.GovernanceDimension.SECURITY, 0.8);
            }

            if (var3 != null) {
               return new io.karatelabs.ext.lint.GovernanceScorer.MaturityAssessment(io.karatelabs.ext.lint.GovernanceScorer.GovernanceMaturityLevel.DESCRIBED, var3);
            } else if (!Boolean.TRUE.equals(((Map)var0.get(io.karatelabs.ext.lint.GovernanceDimension.PROVENNESS)).get("measured"))) {
               return new io.karatelabs.ext.lint.GovernanceScorer.MaturityAssessment(
                  io.karatelabs.ext.lint.GovernanceScorer.GovernanceMaturityLevel.GOVERNED,
                  "`provenness` was not measured — no project coverage graph was readable, so nothing is known about whether this API was claimed or exercised; the dimension's own `note` names the call that writes the graph"
               );
            } else if (!isRuleClean(var1, "operation-claimed-by-requirement")) {
               return new io.karatelabs.ext.lint.GovernanceScorer.MaturityAssessment(io.karatelabs.ext.lint.GovernanceScorer.GovernanceMaturityLevel.GOVERNED, "an operation is claimed by no requirement, or the rule that asks (`operation-claimed-by-requirement`) did not run");
            } else if (isRuleClean(var1, "operation-exercised") && isRuleClean(var1, "operation-not-mock-only")) {
               Map<java.lang.String, java.lang.Object> var4 = (Map)var0.get(io.karatelabs.ext.lint.GovernanceDimension.CONTRACT);
               if (!Boolean.TRUE.equals(var4.get("measured"))) {
                  return new io.karatelabs.ext.lint.GovernanceScorer.MaturityAssessment(
                     io.karatelabs.ext.lint.GovernanceScorer.GovernanceMaturityLevel.EXERCISED,
                     "`contract` was not measured — no paired mock-vs-provider run stands for this artifact; the dimension's own `note` names the call that records one"
                  );
               } else {
                  Object var5 = var4.get("rung");
                  return var5 instanceof Number var6 && var6.intValue() >= 4
                     ? new io.karatelabs.ext.lint.GovernanceScorer.MaturityAssessment(io.karatelabs.ext.lint.GovernanceScorer.GovernanceMaturityLevel.PROVEN, null)
                     : new io.karatelabs.ext.lint.GovernanceScorer.MaturityAssessment(
                        io.karatelabs.ext.lint.GovernanceScorer.GovernanceMaturityLevel.EXERCISED,
                        "the paired run stands at rung "
                           + var5
                           + " of 4 ("
                           + var4.get("rungName")
                           + "), so the mock is not yet PROVEN substitutable — "
                           + (var4.get("cappedBy") != null ? var4.get("cappedBy") : var4.get("blockedBy"))
                     );
               }
            } else {
               return new io.karatelabs.ext.lint.GovernanceScorer.MaturityAssessment(io.karatelabs.ext.lint.GovernanceScorer.GovernanceMaturityLevel.TRACED, "an operation was never exercised, or only a karate mock ever answered it");
            }
         }
      }
   }

   private static String thresholdFailure(Map<GovernanceDimension, Map<String, Object>> var0, GovernanceDimension var1, double var2) {
      Map<java.lang.String, java.lang.Number> var4 = (Map)var0.get(var1);
      if (!Boolean.TRUE.equals(var4.get("measured"))) {
         return "`" + var1.wire() + "` was not measured, so this rung has no evidence behind it — " + var4.get("note");
      } else {
         double var5 = ((Number)var4.get("score")).doubleValue();
         return var5 < var2
            ? "`" + var1.wire() + "` scored " + Math.round(var5 * 100.0) + "%, below the " + Math.round(var2 * 100.0) + "% this rung asks for"
            : null;
      }
   }

   private static Map<String, Object> maturityLevelDetails(io.karatelabs.ext.lint.GovernanceScorer.MaturityAssessment var0) {
      io.karatelabs.ext.lint.GovernanceScorer.GovernanceMaturityLevel var1 = var0.a();
      LinkedHashMap<java.lang.String, java.lang.Object> var2 = new LinkedHashMap();
      var2.put("rung", var1.rung());
      var2.put("name", var1.label());
      var2.put("means", var1.means());
      if (var0.b() != null) {
         var2.put("blockedBy", var0.b());
      }

      io.karatelabs.ext.lint.GovernanceScorer.GovernanceMaturityLevel var3 = var1.rung() + 1 < io.karatelabs.ext.lint.GovernanceScorer.GovernanceMaturityLevel.values().length ? io.karatelabs.ext.lint.GovernanceScorer.GovernanceMaturityLevel.values()[var1.rung() + 1] : null;
      if (var3 != null) {
         var2.put("next", Map.of("rung", var3.rung(), "name", var3.label(), "needs", var3.means()));
      }

      var2.put(
         "ladder",
         Arrays.stream(io.karatelabs.ext.lint.GovernanceScorer.GovernanceMaturityLevel.values())
            .map(var1x -> var1x.rung() + " " + var1x.label() + " — " + var1x.means() + (var1x == var1 ? "   ← you are here" : ""))
            .toList()
      );
      if (var1.rung() < io.karatelabs.ext.lint.GovernanceScorer.GovernanceMaturityLevel.PROVEN.rung()) {
         var2.put(
            "ceiling",
            "'"
               + io.karatelabs.ext.lint.GovernanceScorer.GovernanceMaturityLevel.PROVEN.label()
               + "' is not reachable from the document alone: it needs a paired mock-vs-provider run, which Contract.pair({suite, spec, provider}) records and this ladder reads. It is a RELEASE-TIME rung rather than a live badge — the pair is a dated artifact that decays when the spec, the suite or the mock changes, and a decayed pair holds NO rung rather than a smaller one. The pair's own `rungs` block carries, for every bar the rung rests on, the count that shows the bar was asked."
         );
      }

      return var2;
   }

   private static Map<GovernanceDimension, Double> resolveWeights(Object var0) {
      EnumMap var1 = new EnumMap<>(GovernanceDimension.class);
      var1.putAll(DEFAULT_WEIGHTS);
      if (var0 == null) {
         return var1;
      } else if (!(var0 instanceof Map var2)) {
         throw new LintException("`weights` must be an object keyed by dimension (" + String.join(", ", io.karatelabs.ext.lint.GovernanceDimension.names()) + "), got " + var0.getClass().getSimpleName());
      } else {
         for (Entry var4 : (Iterable<Entry>) (Iterable<?>) (var2.entrySet())) {
            GovernanceDimension var5 = io.karatelabs.ext.lint.GovernanceDimension.of(String.valueOf(var4.getKey()));
            if (!(var4.getValue() instanceof Number var6) || var6.doubleValue() < 0.0) {
               throw new LintException("weight for '" + var5.wire() + "' must be a non-negative number, got '" + var4.getValue() + "'");
            }

            var1.put(var5, var6.doubleValue());
         }

         return var1;
      }
   }

   private static Map<String, Object> waivedSummary(Map<String, Object> var0) {
      int var1 = intValue(var0, "waived");
      int var2 = intValue(var0, "rejectedWaivers");
      LinkedHashMap<java.lang.String, java.lang.Object> var3 = new LinkedHashMap();
      var3.put("count", var1);
      var3.put("rejected", var2);
      var3.put(
         "note",
         var1 == 0
            ? "no finding was waived — this index rests on no accepted exceptions"
            : var1
               + " finding"
               + (var1 == 1 ? " was" : "s were")
               + " waived, and a waived finding is NOT counted as a failing subject — so this index is the one an artifact earns WITH those exceptions accepted, not the one it would earn clean. Waiving and fixing move the number identically by design; what tells them apart is this count, `lint.summary.clean` (false while any waiver stands) and the signed register in `lint.waivers` — every waiver there carries its reason and its owner."
      );
      return var3;
   }

   private static Map<String, Object> weightsReport(Map<GovernanceDimension, Double> var0, double var1, List<String> var3) {
      LinkedHashMap<java.lang.String, java.lang.Double> var4 = new LinkedHashMap();
      LinkedHashMap<java.lang.String, java.lang.Double> var5 = new LinkedHashMap();

      for (GovernanceDimension var9 : GovernanceDimension.values()) {
         var4.put(var9.wire(), var0.get(var9));
         if (!var3.contains(var9.wire()) && var1 > 0.0) {
            var5.put(var9.wire(), roundScore((Double)var0.get(var9) / var1));
         }
      }

      LinkedHashMap<java.lang.String, java.lang.Object> var10 = new LinkedHashMap();
      var10.put("declared", var4);
      var10.put("effective", var5);
      var10.put(
         "note",
         var3.isEmpty()
            ? "every dimension was measured, so the declared weights are the effective ones"
            : "the declared weights were renormalised across the MEASURED dimensions ("
               + String.join(", ", var3)
               + " could not be measured). Renormalising is unavoidable — a mean has to be taken over something — so it is reported here rather than done quietly. An unmeasured dimension is NOT scored zero."
      );
      return var10;
   }

   static int intValue(Map<String, Object> var0, String var1) {
      return (var0 == null ? null : var0.get(var1)) instanceof Number var3 ? var3.intValue() : 0;
   }

   static double roundScore(double var0) {
      return Math.round(var0 * 1000.0) / 1000.0;
   }

   private record MaturityAssessment(io.karatelabs.ext.lint.GovernanceScorer.GovernanceMaturityLevel a, String b) {

   }

   public static enum GovernanceMaturityLevel {
      UNGOVERNED(0, "Ungoverned", "an error-severity finding stands, or a rule could not run at all"),
      DESCRIBED(1, "Described", "the document describes itself — owner, purpose, and what each operation does"),
      GOVERNED(2, "Governed", "it passes the style and security floor, and every exception is owned in git"),
      TRACED(3, "Traced", "every operation is claimed by a requirement — nothing is built without intent"),
      EXERCISED(4, "Exercised", "everything claimed was actually run, and not only against our own mock"),
      PROVEN(5, "Proven", "the mock is proven substitutable for the provider against one shared suite");

      private final int rung;
      private final String label;
      private final String means;

      private GovernanceMaturityLevel(int var3, String var4, String var5) {
         this.rung = var3;
         this.label = var4;
         this.means = var5;
      }

      public int rung() {
         return this.rung;
      }

      public String label() {
         return this.label;
      }

      public String means() {
         return this.means;
      }
   }
}
