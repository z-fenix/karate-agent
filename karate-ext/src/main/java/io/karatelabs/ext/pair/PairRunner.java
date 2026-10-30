package io.karatelabs.ext.pair;

import io.karatelabs.common.Json;
import io.karatelabs.core.Runner;
import io.karatelabs.core.SuiteResult;
import io.karatelabs.ext.common.MaxPaths;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class PairRunner {
   private static final Logger LOGGER = LoggerFactory.getLogger(PairRunner.class);
   public static final String DESTRUCTIVE_TAG = "@destructive";
   public static final String MOCK_HEADER_NAME = "Karate-Mock";
   public static final String TARGET_SYSTEM_PROPERTY = "contract.target";

   private PairRunner() {
   }

   public static Map<String, Object> run(io.karatelabs.ext.pair.PairRunner.PairRunOptions var0) {
      for (String[] var4 : new String[][]{{"provider", var0.e()}, {"mock", var0.f()}}) {
         String var5 = io.karatelabs.ext.pair.EndpointIdentityResolver.validateTarget(var4[0], var4[1]);
         if (var5 != null) {
            throw new PairException("no pair: " + var5);
         }
      }

      String var12 = io.karatelabs.ext.pair.EndpointIdentityResolver.validateDistinctTargets(var0.e(), var0.f(), var0.v());
      if (var12 != null) {
         throw new PairException("no pair: " + var12);
      } else {
         Instant var13 = Instant.now();
         String var14 = UUID.randomUUID().toString().substring(0, 8);
         Path var15 = var0.t().resolve(var14);
         var var16 = excludedScenarios(var0, var15.resolve("excluded"));
         List var6 = var16 == null ? List.of() : var16;
         PairRunEvidence var7 = runLeg(var0, "provider", var0.e(), var15.resolve("provider"));
         PairRunEvidence var8 = var7.f() != null
            ? io.karatelabs.ext.pair.PairRunEvidence.failedLeg("mock", var0.f(), var15.resolve("mock"), "not run — the provider leg produced no run, so there is no pair to complete")
            : runLeg(var0, "mock", var0.f(), var15.resolve("mock"));
         io.karatelabs.ext.pair.PairOutcomeClassifier.ConfirmationRunner var9 = var0.o() ? (var2, var3) -> confirmScenario(var0, var15, var2, var3) : io.karatelabs.ext.pair.PairOutcomeClassifier.ConfirmationRunner.a;
         io.karatelabs.ext.pair.PairOutcomeClassifier.PairClassificationResult var10 = io.karatelabs.ext.pair.PairOutcomeClassifier.classify(var7, var8, var0.j(), var0.d(), var0.i(), var9, var0.v());
         Map var11 = io.karatelabs.ext.pair.CoverageClaimBuilder.buildClaim(var7.d(), var10.c(), var6, var16 != null, var0.d(), reportOptions(var0, var7));
         return a(var0, var14, var13, var7, var8, var10, var11, var6, var16 != null);
      }
   }

   private static io.karatelabs.ext.pair.PairReportAssembler.PairAssessmentContext reportOptions(io.karatelabs.ext.pair.PairRunner.PairRunOptions var0, PairRunEvidence var1) {
      var var2 = io.karatelabs.ext.pair.EndpointIdentityResolver.a(var1, var0.v());
      return new io.karatelabs.ext.pair.PairReportAssembler.PairAssessmentContext(
         var0.i(),
         io.karatelabs.ext.pair.EndpointIdentityResolver.b(var1, var0.v()),
         hasProjectMock(var0),
         mockDescription(var0),
         (Integer)var2.get("reachedTarget"),
         (Integer)var2.get("ofScenariosMakingCalls"),
         var0.o(),
         var0.u(),
         var0.c() != null
      );
   }

   private static boolean hasProjectMock(io.karatelabs.ext.pair.PairRunner.PairRunOptions var0) {
      Object var1 = var0.g();
      if (var1 instanceof String var3) {
         return MaxPaths.isFeature(var3);
      } else {
         return !(var1 instanceof Map var2)
            ? false
            : var2.get("feature") instanceof String || var2.get("file") instanceof String || var2.get("js") instanceof String;
      }
   }

   private static String mockDescription(io.karatelabs.ext.pair.PairRunner.PairRunOptions var0) {
      Object var1 = var0.g();
      if (var1 == null) {
         return "a mock SYNTHESIZED from the spec, not the one this project ships";
      } else if (hasProjectMock(var0)) {
         return "this project's own `" + mockSourceLabel(var1) + "`";
      } else {
         return var1 instanceof String var2
            ? "a mock ALREADY STANDING at " + var2 + ", which this harness did not start"
            : "a mock SYNTHESIZED from the spec and overlaid by your declaration";
      }
   }

   private static String mockSourceLabel(Object var0) {
      if (var0 instanceof String var6) {
         return var6;
      } else {
         if (var0 instanceof Map var1) {
            for (String var3 : List.of("feature", "file", "js")) {
               if (var1.get(var3) instanceof String var4) {
                  return var4;
               }
            }
         }

         return String.valueOf(var0);
      }
   }

   private static PairRunEvidence runLeg(io.karatelabs.ext.pair.PairRunner.PairRunOptions var0, String var1, String var2, Path var3) {
      try {
         Runner.Builder var4 = Runner.path(suitePaths(var0))
            .configDir(var0.a().toString())
            .workingDir(var0.a())
            .outputDir(var3)
            .outputJsonLines(true)
            .outputHtmlReport(true)
            .backupOutputDir(false)
            .systemProperty(var0.n(), var2);

         for (Entry var6 : var0.m().entrySet()) {
            var4.systemProperty((String)var6.getKey(), (String)var6.getValue());
         }

         if (var0.l() != null && !var0.l().isBlank()) {
            var4.karateEnv(var0.l());
         }

         var0.w().apply(var4);
         var4.tags(executionTags(var0).toArray(new String[0]));
         SuiteResult var10 = var4.parallel(1);
         LOGGER.debug("pair leg {} against {}: {} scenarios, {} failed", new Object[]{var1, var2, var10.getScenarioCount(), var10.getScenarioFailedCount()});
         PairRunEvidence var11 = io.karatelabs.ext.pair.PairRunEvidenceLoader.load(var1, var2, var3);
         String var7 = "provider".equals(var1) ? var0.f() : var0.e();
         String var8 = io.karatelabs.ext.pair.EndpointIdentityResolver.a(var11, var2, var0.n(), var0.v(), var7);
         return var8 == null ? var11 : io.karatelabs.ext.pair.PairRunEvidence.failedLeg(var1, var2, var3, var8);
      } catch (RuntimeException var9) {
         return io.karatelabs.ext.pair.PairRunEvidence.failedLeg(var1, var2, var3, "the " + var1 + " leg could not run: " + var9.getMessage());
      }
   }

   private static List<String> suitePaths(io.karatelabs.ext.pair.PairRunner.PairRunOptions var0) {
      ArrayList var1 = new ArrayList();

      for (String var3 : var0.b()) {
         var1.add(!var3.startsWith("classpath:") && !Path.of(var3).isAbsolute() ? var0.a().resolve(var3).toString() : var3);
      }

      return var1;
   }

   private static List<String> executionTags(io.karatelabs.ext.pair.PairRunner.PairRunOptions var0) {
      var var1 = new ArrayList<>(var0.k());
      var1.add("~@destructive");
      return var1;
   }

   private static List<String> excludedScenarios(io.karatelabs.ext.pair.PairRunner.PairRunOptions var0, Path var1) {
      try {
         var var2 = new ArrayList<>(var0.k());
         var2.add("@destructive");
         Runner.path(suitePaths(var0))
            .configDir(var0.a().toString())
            .workingDir(var0.a())
            .outputDir(var1)
            .outputJsonLines(true)
            .outputHtmlReport(false)
            .backupOutputDir(false)
            .dryRun(true)
            .tags(var2.toArray(new String[0]))
            .parallel(1);
         PairRunEvidence var3 = io.karatelabs.ext.pair.PairRunEvidenceLoader.load("excluded", null, var1);
         ArrayList var4 = new ArrayList();

         for (io.karatelabs.ext.pair.PairRunEvidence.ScenarioRunEvidence var6 : var3.e()) {
            var4.add(var6.f());
         }

         return var4;
      } catch (RuntimeException var7) {
         LOGGER.debug("could not inventory the excluded scenarios: {}", var7.getMessage());
         return null;
      }
   }

   private static io.karatelabs.ext.pair.PairOutcomeClassifier.ConfirmationOutcome confirmScenario(io.karatelabs.ext.pair.PairRunner.PairRunOptions var0, Path var1, io.karatelabs.ext.pair.PairRunEvidence.ScenarioRunEvidence var2, String var3) {
      String var4 = "provider".equals(var3) ? var0.e() : var0.f();
      Path var5 = var1.resolve("confirm").resolve(var3 + "-" + Math.abs(var2.e().hashCode()));
      Path var6 = var0.a().resolve(var2.h()).normalize();
      if (!Files.isRegularFile(var6)) {
         return io.karatelabs.ext.pair.PairOutcomeClassifier.ConfirmationOutcome.SKIPPED;
      } else {
         try {
            Runner.Builder var7 = Runner.path(var6.toString())
               .configDir(var0.a().toString())
               .workingDir(var0.a())
               .outputDir(var5)
               .outputJsonLines(true)
               .outputHtmlReport(false)
               .backupOutputDir(false)
               .scenarioName(var2.f())
               .systemProperty(var0.n(), var4);

            for (Entry var9 : var0.m().entrySet()) {
               var7.systemProperty((String)var9.getKey(), (String)var9.getValue());
            }

            if (var0.l() != null && !var0.l().isBlank()) {
               var7.karateEnv(var0.l());
            }

            var0.w().apply(var7);
            var7.parallel(1);
         } catch (RuntimeException var10) {
            LOGGER.debug("confirmation re-run failed to start: {}", var10.getMessage());
            return io.karatelabs.ext.pair.PairOutcomeClassifier.ConfirmationOutcome.SKIPPED;
         }

         PairRunEvidence var11 = io.karatelabs.ext.pair.PairRunEvidenceLoader.load(var3, var4, var5);
         if (var11.f() == null && !var11.e().isEmpty()) {
            io.karatelabs.ext.pair.PairRunEvidence.ScenarioRunEvidence var12 = var11.e().get(0);
            if (!io.karatelabs.ext.pair.PairOutcomeClassifier.samePreconditions(var12, var2, var0.d(), var0.j())) {
               return io.karatelabs.ext.pair.PairOutcomeClassifier.ConfirmationOutcome.NOT_COMPARABLE;
            } else {
               return var12.isPassed() ? io.karatelabs.ext.pair.PairOutcomeClassifier.ConfirmationOutcome.FLAKY : io.karatelabs.ext.pair.PairOutcomeClassifier.ConfirmationOutcome.CONFIRMED;
            }
         } else {
            return io.karatelabs.ext.pair.PairOutcomeClassifier.ConfirmationOutcome.SKIPPED;
         }
      }
   }

   private static Map<String, Object> a(
      io.karatelabs.ext.pair.PairRunner.PairRunOptions var0, String var1, Instant var2, PairRunEvidence var3, PairRunEvidence var4, io.karatelabs.ext.pair.PairOutcomeClassifier.PairClassificationResult var5, Map<String, Object> var6, List<String> var7, boolean var8
   ) {
      LinkedHashMap var9 = new LinkedHashMap();
      var9.put("suite", var0.b().size() == 1 ? var0.b().get(0) : var0.b());
      if (var0.c() != null) {
         var9.put("spec", var0.c());
      }

      var9.put("provider", var0.e());
      var9.put("profile", var0.i().wire());
      if (!var0.k().isEmpty()) {
         var9.put("tags", var0.k());
      }

      if (var0.j().rawConfig() != null) {
         var9.put("ignore", var0.j().rawConfig());
      }

      if (var0.h() != null) {
         var9.put("fixture", var0.h());
      }

      if (var0.g() != null) {
         var9.put("mock", var0.g());
      }

      if (!"contract.target".equals(var0.n())) {
         var9.put("targetProp", var0.n());
      }

      if (var0.l() != null && !var0.l().isBlank()) {
         var9.put("env", var0.l());
      }

      if (!var0.m().isEmpty()) {
         var9.put("propKeys", new ArrayList<>(var0.m().keySet()));
      }

      if (!var0.o()) {
         var9.put("confirm", false);
      }

      if (var0.q() != null) {
         var9.put("providerEnv", var0.q());
      }

      if (var0.r() != null) {
         var9.put("providerBuild", var0.r());
      }

      if (var0.p() != null) {
         var9.put("maxAgeDays", var0.p());
      }

      if (!var0.v().isEmpty()) {
         var9.put("aliases", var0.v().aliasesAsMaps());
      }

      LinkedHashMap var10 = new LinkedHashMap();
      if (var0.c() != null) {
         var10.put("spec", var0.c());
      }

      var10.put("suite", var0.b());
      var10.put("mock", mockBindingValue(var0));
      var10.put("profile", var0.i().wire());
      var var11 = h(var0);
      var var12 = i(var0);
      if (!var11.isEmpty()) {
         var10.put("mockFiles", var11.stream().map(var1x -> var0.a().relativize(var1x).toString()).toList());
      }

      if (!var12.isEmpty()) {
         var10.put("rulebooks", var12.stream().map(var1x -> var0.a().relativize(var1x).toString()).toList());
      }

      if (var0.h() != null) {
         var10.put("fixture", var0.h());
      }

      LinkedHashMap var13 = new LinkedHashMap();
      if (var0.c() != null) {
         var13.put("spec", io.karatelabs.ext.pair.PairEvidenceRepository.digestFile(a(var0.a(), var0.c())));
      }

      var13.put("suite", io.karatelabs.ext.pair.PairEvidenceRepository.digestFiles(a(var0.a(), var0.b())));
      var13.put("mock", io.karatelabs.ext.pair.PairEvidenceRepository.digestWithPrefix(mockBindingValue(var0), var11));
      if (!var12.isEmpty()) {
         var13.put("rulebook", io.karatelabs.ext.pair.PairEvidenceRepository.digestFiles(var12));
      }

      var13.put("profile", io.karatelabs.ext.pair.PairEvidenceRepository.shortSha256(var0.i().wire().getBytes()));
      if (var0.h() != null) {
         var13.put("fixture", io.karatelabs.ext.pair.PairEvidenceRepository.digestFile(a(var0.a(), var0.h())));
      }

      LinkedHashMap var14 = new LinkedHashMap();
      var14.put("target", var0.e());
      var14.put("env", var0.q());
      var14.put("build", var0.r());
      LinkedHashMap var15 = new LinkedHashMap();
      var15.put("schema", "karate-pair/v1");
      var15.put("pairId", var1);
      var15.put("mintedAt", var2.toString());
      var15.put("scope", var9);
      LinkedHashMap var16 = new LinkedHashMap();
      var16.put("name", var0.i().wire());
      var16.put("means", var0.i().means());
      var15.put("dataProfile", var16);
      LinkedHashMap var17 = new LinkedHashMap();
      var17.put("provider", endpointStats(var3, var0.v()));
      var17.put("mock", endpointStats(var4, var0.v()));
      var15.put("legs", var17);
      boolean var18 = io.karatelabs.ext.pair.EndpointIdentityResolver.b(var3, var0.v());
      boolean var19 = io.karatelabs.ext.pair.EndpointIdentityResolver.c(var3).isEmpty();
      if (var19) {
         var15.put("rehearsal", null);
         var15.put(
            "rehearsalCheck",
            "Not checked. The provider leg recorded no HTTP exchange, so there was no response to read the Karate-Mock header from. Read this check as unasked, not as cleared."
         );
      } else if (!var18) {
         var15.put("rehearsal", null);
         var15.put(
            "rehearsalCheck",
            "The provider response carried no Karate-Mock header, so the responder was not one of our mocks. Whether it was the real deployment is assumed, not verified: record it in `providerEnv` and `providerBuild`."
         );
      } else {
         var15.put(
            "rehearsal",
            "A karate mock answered the provider leg, and disclosed itself on the wire (the Karate-Mock header). This run is a rehearsal. Two instances of one mock agree by construction, so the run supports no claim about a real provider, whatever its numbers are. Point `provider` at the real deployment."
         );
      }

      long var20 = var5.c().stream().filter(var0x -> var0x.n() != -1 && !var0x.a()).count();
      if (var20 > 0L) {
         var15.put(
            "preconditionProbe",
            var20
               + " of "
               + var5.c().size()
               + " scenario"
               + (var5.c().size() == 1 ? "" : "s")
               + " had NO comparable Background exchange, so their state equivalence was assumed rather than observed (`probe.state: NOT_CHECKED` on the row). Nothing was found wrong with them because nothing was looked at. Put the suite's setup read-backs in the Background — that IS the precondition probe."
         );
      }

      var15.put("outcomes", var5.a());
      var15.put("divergences", var5.b().stream().map(io.karatelabs.ext.pair.PairOutcomeClassifier.ScenarioClassification::d).toList());
      var15.put("notMeasured", var5.c().stream().filter(var0x -> "notmeasured".equals(var0x.h())).map(io.karatelabs.ext.pair.PairOutcomeClassifier.ScenarioClassification::d).toList());
      var15.put("scenarios", var5.c().stream().map(io.karatelabs.ext.pair.PairOutcomeClassifier.ScenarioClassification::d).toList());
      var15.put("ignored", var0.j().summary());
      LinkedHashMap var22 = new LinkedHashMap();
      var22.put("tag", "@destructive");
      var22.put("scenarios", var7);
      var22.put("enumerated", var8);
      var22.put(
         "note",
         !var8
            ? "the excluded set could not be enumerated — the inventory pass failed, so this pair cannot name which scenarios @destructive held out of both legs. It held out whatever carried the tag: read the denominator as incomplete rather than as an empty exclusion set."
            : (
               var7.isEmpty()
                  ? "no scenario carried @destructive, so nothing was held out of either leg"
                  : var7.size()
                     + " scenario"
                     + (var7.size() == 1 ? " was" : "s were")
                     + " held out of BOTH legs by @destructive. Their mock-verified evidence comes from the everyday mock leg, which is not in this pair and runs them freely — mock-verified, provider-unverified, which is what `mockOnly` already says. This pair claims NOTHING about them."
            )
      );
      var15.put("excluded", var22);
      var15.put("claim", var6);
      var15.put("claimSentence", buildClaimSentence(var0, var6, var2, var7, var8, var18, var0.j().summary(), var5.a().getOrDefault("agreedFailure", 0)));
      var15.put("bindings", io.karatelabs.ext.pair.PairEvidenceRepository.createBindings(var10, var13, var14, var2, var0.p()));
      LinkedHashMap var23 = new LinkedHashMap();
      var23.put("enabled", var0.o());
      long var24 = var5.c().stream().filter(var0x -> var0x.m() != null).count();
      var23.put(
         "note",
         var24 == 0L
            ? "no verdict-layer divergence was found, so nothing was confirmed either way — "
               + (var0.o() ? "confirmation was on and had nothing to re-run." : "confirmation was off, and would have had nothing to re-run.")
            : (
               var0.o()
                  ? "every verdict-layer divergence below was CONFIRMED by a one-sided re-run of the failing leg, outside the comparison; one that did not reproduce is quarantined as `notmeasured/flaky` rather than published."
                  : "confirmation was OFF, so a verdict-layer divergence below may be flakiness rather than divergence. Re-run with {confirm:true} before acting on one."
            )
      );
      var15.put("confirmation", var23);
      return var15;
   }

   private static Map<String, Object> endpointStats(PairRunEvidence var0, io.karatelabs.ext.pair.EndpointAliasSet var1) {
      LinkedHashMap var2 = new LinkedHashMap();
      var2.put("target", var0.c());
      var2.put("runDir", var0.d() == null ? null : var0.d().toString());
      var2.put("scenarios", var0.e().size());
      long var3 = var0.e().stream().filter(io.karatelabs.ext.pair.PairRunEvidence.ScenarioRunEvidence::isPassed).count();
      var2.put("passed", var3);
      var2.put("failed", var0.e().size() - var3);
      var2.put("addressedTarget", io.karatelabs.ext.pair.EndpointIdentityResolver.a(var0, var1));
      if (var0.f() != null) {
         var2.put("failure", var0.f());
      }

      return var2;
   }

   private static String buildClaimSentence(io.karatelabs.ext.pair.PairRunner.PairRunOptions var0, Map<String, Object> var1, Instant var2, List<String> var3, boolean var4, boolean var5, Map<String, Object> var6, int var7) {
      String var8 = var0.e()
         + (var0.q() == null ? "" : " (environment `" + var0.q() + "`)")
         + (var5 ? " (A KARATE MOCK, not a real provider — this is a rehearsal)" : "")
         + mockNarrativeSuffix(var0)
         + (
            var0.c() != null
               ? ""
               : ", minted WITHOUT a contract document (`spec`): operations are named by method and path, and response headers were not compared"
         );
      if (!Boolean.TRUE.equals(var1.get("measured"))) {
         Object var11 = var1.get("note");
         return "this pair ran the suite against "
            + var8
            + " and against the mock as of "
            + var2
            + ", under the `"
            + var0.i().wire()
            + "` data profile. There is NO percentage and no per-operation claim here"
            + (var11 == null ? "" : " — " + var11)
            + " Only the divergence set below stands, and it stands as a list.";
      } else {
         Map var9 = (Map)var1.get("claimCoverage");
         Map var10 = (Map)var1.get("measuredProportion");
         return "over the "
            + var9.get("exercised")
            + " of "
            + var9.get("declared")
            + " declared operations this suite exercised against "
            + var8
            + ", as of "
            + var2
            + ", under the `"
            + var0.i().wire()
            + "` data profile"
            + (
               !var4
                  ? ", excluding an UNKNOWN number of scenarios by @destructive (the inventory pass failed, so the excluded set could not be named)"
                  : (var3.isEmpty() ? "" : ", excluding " + var3.size() + " scenario" + (var3.size() == 1 ? "" : "s") + " by @destructive")
            )
            + ignoredDifferencesSuffix(var6)
            + waivedBarsSuffix(var1)
            + (var7 == 0 ? "" : ", of which " + var7 + " scenario" + (var7 == 1 ? "" : "s") + " agreed only in FAILING")
            + ": "
            + var10.get("divergenceFree")
            + " of "
            + var10.get("admissible")
            + " admissible operations were divergence-free. Each binding in this sentence is part of the claim. A statement that drops one of them is a different claim.";
      }
   }

   private static String waivedBarsSuffix(Map<String, Object> var0) {
      if (((var0.get("rungs") instanceof Map var3 ? var3.get("aggregate") : null) instanceof Map var4 ? var4.get("waivedBars") : null) instanceof List<?> var7
         && !var7.isEmpty()) {
         String var5 = var7.stream().map(var0x -> String.valueOf(((Map)var0x).get("bar"))).distinct().reduce((var0x, var1) -> var0x + ", " + var1).orElse("");
         return ", with "
            + var7.size()
            + " bar"
            + (var7.size() == 1 ? "" : "s")
            + " STOOD DOWN by waiver ("
            + var5
            + ") — the rung rests on that exception rather than on evidence for it";
      } else {
         return "";
      }
   }

   private static String ignoredDifferencesSuffix(Map<String, Object> var0) {
      int var1 = var0 == null ? 0 : ((Number)var0.getOrDefault("count", 0)).intValue();
      if (var1 == 0) {
         return "";
      } else {
         int var2 = ((Number)var0.getOrDefault("stale", 0)).intValue();
         return ", with "
            + var1
            + " difference"
            + (var1 == 1 ? "" : "s")
            + " excused by ignore rule"
            + (var2 == 0 ? "" : " (" + var2 + " of those rules matched nothing and are STALE)");
      }
   }

   private static String mockNarrativeSuffix(io.karatelabs.ext.pair.PairRunner.PairRunOptions var0) {
      Object var1 = var0.g();
      if (var1 == null) {
         return ", with the mock leg served by a mock SYNTHESIZED from the spec rather than the one this project ships";
      } else if (var1 instanceof String var2 && !MaxPaths.isFeature(var2)) {
         return ", with the mock leg served by a mock ALREADY STANDING at "
            + var2
            + ", which this harness did not start and cannot describe beyond its address";
      } else {
         String var8 = var1 instanceof String var3 ? var3 : null;
         if (var1 instanceof Map var9) {
            for (String var5 : List.of("feature", "file", "js")) {
               if (var9.get(var5) instanceof String var6) {
                  var8 = var6;
                  break;
               }
            }
         }

         return var8 == null
            ? ", with the mock leg served by a mock SYNTHESIZED from the spec (overlaid by your declaration) rather than the one this project ships"
            : ", with the mock leg served by this project's own `" + var8 + "`";
      }
   }

   private static String mockBindingValue(io.karatelabs.ext.pair.PairRunner.PairRunOptions var0) {
      Object var1 = var0.g();
      return var1 == null ? "auto:" + var0.c() : Json.stringifyStrict(!(var1 instanceof Map) && !(var1 instanceof List) ? String.valueOf(var1) : var1);
   }

   private static List<Path> h(io.karatelabs.ext.pair.PairRunner.PairRunOptions var0) {
      ArrayList var1 = new ArrayList();
      if (var0.g() instanceof Map var2) {
         for (String var4 : List.of("feature", "file", "js")) {
            Path var5 = a(var0.a(), var2.get(var4));
            if (var5 != null) {
               var1.add(var5);
            }
         }

         return var1;
      } else {
         Path var6 = a(var0.a(), var0.g());
         if (var6 != null) {
            var1.add(var6);
         }

         return var1;
      }
   }

   private static List<Path> i(io.karatelabs.ext.pair.PairRunner.PairRunOptions var0) {
      ArrayList var1 = new ArrayList();
      if (!(var0.g() instanceof Map var2)) {
         return var1;
      } else {
         for (String var4 : List.of("rules", "handlers")) {
            if (var2.get(var4) instanceof Map var5) {
               for (Object var7 : var5.values()) {
                  Path var8 = a(var0.a(), var7);
                  if (var8 != null) {
                     var1.add(var8);
                  }
               }
            }
         }

         Path var10 = a(var0.a(), var2.get("handlers"));
         if (var10 != null) {
            var1.add(var10);
         }

         return var1;
      }
   }

   private static Path a(Path var0, Object var1) {
      if (var1 instanceof String var2 && !var2.isBlank() && !var2.contains("\n") && !var2.contains("(")) {
         Path var3 = a(var0, var2);
         return var3 != null && Files.isRegularFile(var3) ? var3 : null;
      } else {
         return null;
      }
   }

   private static Path a(Path var0, String var1) {
      if (var1 != null && !var1.isBlank()) {
         Path var2 = Path.of(var1);
         return var2.isAbsolute() ? var2 : var0.resolve(var2);
      } else {
         return null;
      }
   }

   private static List<Path> a(Path var0, List<String> var1) {
      ArrayList var2 = new ArrayList();

      for (String var4 : var1) {
         Path var5 = a(var0, var4);
         if (var5 != null) {
            if (Files.isRegularFile(var5)) {
               var2.add(var5);
            } else if (Files.isDirectory(var5)) {
               try (var var6 = Files.walk(var5)) {
                  var6.filter(var0x -> var0x.toString().endsWith(".feature")).forEach(var2::add);
               } catch (Exception var11) {
               }
            }
         }
      }

      return var2;
   }

   public record PairRunOptions(Path a, List<String> b, String c, ContractSchema d, String e, String f, Object g, String h, DataProfile i, NondeterminismDetector j, List<String> k, String l, Map<String, String> m, String n, boolean o, Integer p, String q, String r, Path s, Path t, Map<String, Map<String, Object>> u, io.karatelabs.ext.pair.EndpointAliasSet v, UnaryOperator<Runner.Builder> w) {

      public PairRunOptions(
         Path var1,
         List<String> var2,
         String var3,
         ContractSchema var4,
         String var5,
         String var6,
         Object var7,
         String var8,
         DataProfile var9,
         NondeterminismDetector var10,
         List<String> var11,
         String var12,
         Map<String, String> var13,
         String var14,
         boolean var15,
         Integer var16,
         String var17,
         String var18,
         Path var19,
         Path var20,
         Map<String, Map<String, Object>> var21,
         io.karatelabs.ext.pair.EndpointAliasSet var22
      ) {
         this(
            var1,
            var2,
            var3,
            var4,
            var5,
            var6,
            var7,
            var8,
            var9,
            var10,
            var11,
            var12,
            var13,
            var14,
            var15,
            var16,
            var17,
            var18,
            var19,
            var20,
            var21,
            var22,
            var0 -> var0
         );
      }

      public PairRunOptions(
         Path var1,
         List<String> var2,
         String var3,
         ContractSchema var4,
         String var5,
         String var6,
         Object var7,
         String var8,
         DataProfile var9,
         NondeterminismDetector var10,
         List<String> var11,
         String var12,
         Map<String, String> var13,
         String var14,
         boolean var15,
         Integer var16,
         String var17,
         String var18,
         Path var19,
         Path var20,
         Map<String, Map<String, Object>> var21
      ) {
         this(
            var1,
            var2,
            var3,
            var4,
            var5,
            var6,
            var7,
            var8,
            var9,
            var10,
            var11,
            var12,
            var13,
            var14,
            var15,
            var16,
            var17,
            var18,
            var19,
            var20,
            var21,
            io.karatelabs.ext.pair.EndpointAliasSet.EMPTY
         );
      }

      public PairRunOptions(
         Path var1,
         List<String> var2,
         String var3,
         ContractSchema var4,
         String var5,
         String var6,
         Object var7,
         String var8,
         DataProfile var9,
         NondeterminismDetector var10,
         List<String> var11,
         String var12,
         Map<String, String> var13,
         String var14,
         boolean var15,
         Integer var16,
         String var17,
         String var18,
         Path var19,
         Path var20
      ) {
         this(
            var1,
            var2,
            var3,
            var4,
            var5,
            var6,
            var7,
            var8,
            var9,
            var10,
            var11,
            var12,
            var13,
            var14,
            var15,
            var16,
            var17,
            var18,
            var19,
            var20,
            Map.of(),
            io.karatelabs.ext.pair.EndpointAliasSet.EMPTY
         );
      }

   }
}
