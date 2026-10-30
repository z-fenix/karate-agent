package io.karatelabs.ext.mutation;

import io.karatelabs.common.Json;
import io.karatelabs.core.Runner;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.UnaryOperator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ResponseMutationRunner {
   private static final Logger LOGGER = LoggerFactory.getLogger(ResponseMutationRunner.class);

   private ResponseMutationRunner() {
   }

   public static Map<String, Object> run(io.karatelabs.ext.mutation.ResponseMutationRunner.ResponseMutationOptions var0) {
      Path var1 = canonicalPath(var0.a().resolve("runs"));
      return canonicalPath(var0.d()).startsWith(var1)
         ? errorResult(
            "QUARANTINE_VIOLATION",
            "outDir must not point into the governed runs/ tree — seeded mutation failures would enter readiness/RTM/provenance on the next aggregate; use a path under target/",
            List.of()
         )
         : runChecked(var0);
   }

   private static Map<String, Object> runChecked(io.karatelabs.ext.mutation.ResponseMutationRunner.ResponseMutationOptions var0) {
      io.karatelabs.ext.openapi.OpenApiOperationCatalog var1 = var0.c() == null ? null : io.karatelabs.ext.openapi.OpenApiOperationCatalog.loadFromPath(var0.a().resolve(var0.c()).toString());
      io.karatelabs.ext.mutation.ResponseMutatingHttpClient.RouteMatcher var2 = (var1x, var2x) -> io.karatelabs.ext.mutation.ResponseMutationFactory.operationIdentity(var1, var1x, var2x);
      io.karatelabs.ext.mutation.ResponseMutationRunner.ResponseMutationRunResult var3 = runMutation(var0, null, var2, var0.d().resolve("baseline"));
      if (var3.d() != null) {
         return errorResult("INVALID_BASELINE", "baseline run failed: " + var3.d() + " — fix the suite against the live SUT before any mutant is scored", List.of());
      } else {
         ArrayList<java.lang.String> var4 = new ArrayList();

         for (io.karatelabs.ext.mutation.AssertionEvidenceExtractor.FeatureEvidence var6 : var3.c()) {
            if ("failed".equals(var6.e()) && !var0.f().contains(var6.b())) {
               var4.add(var6.a() + "::" + var6.b());
            }
         }

         if (!var4.isEmpty()) {
            return errorResult(
               "INVALID_BASELINE",
               "the baseline suite is not green against the live SUT — fix it (or record an expected failure) before any mutant is scored; no scores were emitted",
               var4
            );
         } else {
            var var19 = io.karatelabs.ext.mutation.ResponseMutationFactory.collectOperations(var3.b(), var1);
            var var20 = io.karatelabs.ext.mutation.ResponseMutationFactory.generateMutants(var19, var1, var0.i());
            if (var20.isEmpty()) {
               return errorResult("EMPTY_DECK", "the baseline run produced no observable HTTP responses to derive a tamper deck from — nothing to score", List.of());
            } else {
               String var7 = var1 == null ? "method+path" : "operationId";
               String var8 = var1 == null ? "0K" : "1";
               var var9 = new ArrayList<>(
                  var1 == null
                     ? List.of(
                        "no spec armed: operation identity is method + path, and the deck is observed-shape operators only — declared error paths and enum universes not exercised",
                        "response-surface vigilance only — business logic not fault-seeded (a rulebook unlocks Tier-1 logic mutation)"
                     )
                     : List.of(
                        "spec armed: declared-status tampers and declared-enum substitutions derived; fields without a declared response schema ride the observed shape",
                        "response-surface vigilance only — business logic not fault-seeded (a rulebook unlocks Tier-1 logic mutation)"
                     )
               );
               if (!var0.i()) {
                  var9.add(
                     "content-type tampers not derived — under karate's default lenient parse no assertion could notice one; configure strictResponseParsing = true in the suite and declare it here to include them"
                  );
               }

               ArrayList<java.lang.String> var10 = new ArrayList();
               LinkedHashMap<java.lang.String, java.lang.Object> var11 = new LinkedHashMap();
               var11.put("seam", "in-client");
               var11.put("identityMode", var7);
               var11.put("rung", var8);
               var11.put("strictResponseParsing", var0.i());
               var11.put("suites", suitePaths(var0));
               var11.put("disclosures", var9);
               var11.put("mutants", var20.stream().map(ResponseMutationSpec::toMap).toList());

               try {
                  Files.createDirectories(var0.d());
                  Files.writeString(var0.d().resolve("tamper-manifest.json"), Json.stringifyStrict(var11));
               } catch (Exception var18) {
                  LOGGER.warn("tamper: could not persist manifest: {}", var18.toString());
                  var10.add("tamper-manifest.json: " + var18);
               }

               LinkedHashMap<java.lang.String, io.karatelabs.ext.mutation.ResponseMutationResultBuilder.ResponseMutationOutcome> var12 = new LinkedHashMap();

               for (ResponseMutationSpec var14 : (Iterable<ResponseMutationSpec>) (Iterable<?>) (var20)) {
                  io.karatelabs.ext.mutation.ResponseMutationRunner.ResponseMutationRunResult var15 = runMutation(var0, var14, var2, var0.d().resolve("mutants").resolve(var14.artifactKey()));
                  if (var15.d() != null && var15.d().startsWith("REPLAY_TIMEOUT_UNKILLABLE")) {
                     return errorResult(
                        "ABORTED_UNKILLABLE_REPLAY",
                        "the replay of "
                           + var14.d()
                           + " timed out and its thread could not be terminated; no scores were emitted (a hung run overlapping later mutants would corrupt attribution)",
                        List.of()
                     );
                  }

                  var12.put(var14.d(), new io.karatelabs.ext.mutation.ResponseMutationResultBuilder.ResponseMutationOutcome(var15.a(), var15.c(), var15.d()));
               }

               Map var21 = io.karatelabs.ext.mutation.ResponseMutationResultBuilder.buildResult(var20, var3.c(), var12, var0.e(), var0.f(), var7, var8, var9);
               LinkedHashMap<java.lang.String, java.lang.Object> var22 = new LinkedHashMap();
               var22.put("kind", "tamper");
               var22.put("seam", "in-client");
               var22.put("subject", "in-client response tamper");
               var22.put("suites", suitePaths(var0));
               var22.putAll(var21);

               try {
                  Files.writeString(var0.d().resolve("mutation-report.json"), Json.stringifyStrict(var22));
               } catch (Exception var17) {
                  LOGGER.warn("tamper: could not persist report: {}", var17.toString());
                  var10.add("mutation-report.json: " + var17);
               }

               Path var23 = io.karatelabs.ext.mutation.MutationHtmlReportWriter.writeReport(var22, var0.d());
               LinkedHashMap<java.lang.String, java.lang.Object> var16 = new LinkedHashMap(var22);
               var16.put("reportDir", var0.d().toString());
               if (var23 != null) {
                  var16.put("reportFile", var23.toString());
                  var16.put("reportUrl", var23.toUri().toString());
               } else {
                  var16.put("warning", "mutation-report.html: write failed (see log)");
               }

               if (!var10.isEmpty()) {
                  var16.put("error", "PERSIST_FAILED");
                  var16.put(
                     "message",
                     "scoring completed but required artifacts failed to persist ("
                        + String.join("; ", var10)
                        + ") — the returned numbers are valid in memory; nothing on disk should be trusted for this run"
                  );
               }

               return var16;
            }
         }
      }
   }

   private static List<String> suitePaths(io.karatelabs.ext.mutation.ResponseMutationRunner.ResponseMutationOptions var0) {
      Path var1 = var0.a().toAbsolutePath().normalize();
      return var0.b().stream().map(var1x -> {
         try {
            Path var2 = Path.of(var1x);
            if (var2.isAbsolute()) {
               Path var3 = var2.normalize();
               return var3.startsWith(var1) ? var1.relativize(var3).toString() : var3.getFileName().toString();
            }
         } catch (RuntimeException var4) {
         }

         return (String)var1x;
      }).toList();
   }

   private static Path canonicalPath(Path var0) {
      try {
         return var0.toFile().getCanonicalFile().toPath();
      } catch (Exception var2) {
         return var0.toAbsolutePath().normalize();
      }
   }

   private static Map<String, Object> errorResult(String var0, String var1, List<String> var2) {
      LinkedHashMap<java.lang.String, java.lang.Object> var3 = new LinkedHashMap();
      var3.put("error", var0);
      var3.put("message", var1);
      if (!var2.isEmpty()) {
         var3.put("failures", var2);
      }

      return var3;
   }

   private static io.karatelabs.ext.mutation.ResponseMutationRunner.ResponseMutationRunResult runMutation(io.karatelabs.ext.mutation.ResponseMutationRunner.ResponseMutationOptions var0, ResponseMutationSpec var1, io.karatelabs.ext.mutation.ResponseMutatingHttpClient.RouteMatcher var2, Path var3) {
      ResponseMutatingHttpClientFactory var4 = new ResponseMutatingHttpClientFactory(var1, var2);
      ExecutorService var5 = Executors.newSingleThreadExecutor();

      io.karatelabs.ext.mutation.ResponseMutationRunner.ResponseMutationRunResult var27;
      try {
         var var6 = var0.b().stream().map(var1x -> var0.a().resolve(var1x).toString()).toList();
         Future var24 = var5.submit(
            () -> {
               Runner.Builder var4x = Runner.path(var6.toArray(String[]::new))
                  .configDir(var0.a().toString())
                  .workingDir(var0.a().toAbsolutePath())
                  .outputDir(var3.resolve("run").toString())
                  .outputJsonLines(true)
                  .listener(io.karatelabs.ext.coverage.HttpExchangeEvidenceRecorder.embeddingListener())
                  .httpClientFactory(var4);
               var0.h().forEach(var4x::systemProperty);
               var0.j().apply(var4x);
               return var4x.parallel(1);
            }
         );

         try {
            var24.get(var0.g(), TimeUnit.MILLISECONDS);
         } catch (TimeoutException var21) {
            var24.cancel(true);
            var4.abortAll();
            var5.shutdownNow();

            boolean var9;
            try {
               var9 = var5.awaitTermination(10L, TimeUnit.SECONDS);
            } catch (InterruptedException var20) {
               Thread.currentThread().interrupt();
               var9 = false;
            }

            return new io.karatelabs.ext.mutation.ResponseMutationRunner.ResponseMutationRunResult(
               var4.appliedEvents(),
               List.of(),
               List.of(),
               var9
                  ? "REPLAY_TIMEOUT after " + var0.g() + "ms"
                  : "REPLAY_TIMEOUT_UNKILLABLE after " + var0.g() + "ms — the suite thread did not terminate; aborting the invocation"
            );
         }

         Path var8 = var3.resolve("run/karate-json/karate-events.jsonl");
         if (!Files.exists(var8)) {
            return new io.karatelabs.ext.mutation.ResponseMutationRunner.ResponseMutationRunResult(var4.appliedEvents(), List.of(), List.of(), "MISSING_JSONL");
         }

         io.karatelabs.ext.mutation.AssertionEvidenceExtractor.EvidenceExtractionResult var25 = io.karatelabs.ext.mutation.AssertionEvidenceExtractor.extract(var8);
         if (var25.isValid()) {
            ArrayList var28 = new ArrayList();

            for (io.karatelabs.ext.common.KarateEventLogParser.KarateScenarioResult var12 : io.karatelabs.ext.common.KarateEventLogParser.a(var8).b()) {
               for (io.karatelabs.ext.common.KarateEventLogParser.KarateStepResult var14 : var12.i()) {
                  var28.addAll(var14.k());
               }
            }

            return new io.karatelabs.ext.mutation.ResponseMutationRunner.ResponseMutationRunResult(var4.appliedEvents(), var28, var25.b(), null);
         }

         var27 = new io.karatelabs.ext.mutation.ResponseMutationRunner.ResponseMutationRunResult(
            var4.appliedEvents(),
            List.of(),
            List.of(),
            "MALFORMED_EVIDENCE: "
               + var25.d()
               + " feature record(s) and "
               + var25.e()
               + " exchange embed(s) unreadable"
               + (var25.b().isEmpty() ? "; no scenario read" : "")
         );
      } catch (Exception var22) {
         return new io.karatelabs.ext.mutation.ResponseMutationRunner.ResponseMutationRunResult(var4.appliedEvents(), List.of(), List.of(), "RUN_FAILED: " + var22.getMessage());
      } finally {
         var5.shutdownNow();
      }

      return var27;
   }

   private record ResponseMutationRunResult(List<io.karatelabs.ext.mutation.ResponseMutatingHttpClient.AppliedMutationEvent> a, List<Map<String, Object>> b, List<io.karatelabs.ext.mutation.AssertionEvidenceExtractor.FeatureEvidence> c, String d) {

   }

   public record ResponseMutationOptions(Path a, List<String> b, String c, Path d, Map<String, String> e, Set<String> f, long g, Map<String, String> h, boolean i, UnaryOperator<Runner.Builder> j) {


      public static io.karatelabs.ext.mutation.ResponseMutationRunner.ResponseMutationOptions defaults(Path var0, List<String> var1, String var2, Path var3) {
         return new io.karatelabs.ext.mutation.ResponseMutationRunner.ResponseMutationOptions(var0, var1, var2, var3, Map.of(), Set.of(), 120000L, Map.of(), false, var0x -> var0x);
      }
   }
}
