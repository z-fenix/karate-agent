package io.karatelabs.ext.mutation;

import io.karatelabs.common.Json;
import io.karatelabs.core.Runner;
import io.karatelabs.ext.mock.MockInstance;
import io.karatelabs.ext.mock.OpenApiMockHandler;
import io.karatelabs.ext.rules.MutantScreener;
import io.karatelabs.ext.rules.RulebookRepository;
import io.karatelabs.ext.rules.RuleEvaluator;
import io.karatelabs.ext.rules.Rule;
import io.karatelabs.http.Session;
import io.karatelabs.js.Engine;
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

public final class SuiteMutationRunner {
   private static final Logger LOGGER = LoggerFactory.getLogger(SuiteMutationRunner.class);

   private SuiteMutationRunner() {
   }

   public static Map<String, Object> run(io.karatelabs.ext.mutation.SuiteMutationRunner.SuiteMutationOptions var0) {
      Path var1 = var0.a().resolve("rulebooks");
      io.karatelabs.ext.rules.RulebookRepository.RulebookDocument var2 = new RulebookRepository(var1).b(var0.b());
      var var3 = new io.karatelabs.ext.rules.MutationGenerator(var2.g(), var2.m()).a(io.karatelabs.ext.rules.MutationGenerator.MutationTarget.a(var0.m(), var2));
      LinkedHashMap<java.lang.String, io.karatelabs.ext.mutation.SuiteMutationResultBuilder.MutantRunResult> var4 = new LinkedHashMap();
      RuleEvaluator var5 = new RuleEvaluator();

      try {
         var var6 = MutantScreener.b(var2);
         var var7 = MutantScreener.a(var2.g(), var2.m().e(), var6, var5);

         for (io.karatelabs.ext.rules.MutationGenerator.Mutant var9 : (Iterable<io.karatelabs.ext.rules.MutationGenerator.Mutant>) (Iterable<?>) (var3)) {
            if (var9.o()) {
               io.karatelabs.ext.rules.MutantScreener.ScreeningResult var10 = MutantScreener.a(var9, var6, var7, var5);
               var4.put(var9.d(), new io.karatelabs.ext.mutation.SuiteMutationResultBuilder.MutantRunResult(var10, null, null, null));
            }
         }
      } catch (Throwable var15) {
         try {
            var5.close();
         } catch (Throwable var13) {
            var15.addSuppressed(var13);
         }

         throw var15;
      }

      var5.close();
      io.karatelabs.ext.mutation.SuiteMutationRunner.SuiteRunResult var16 = runSuite(var0, var1, null, var0.f().resolve("baseline"));
      if (var16.c() != null) {
         return invalidBaseline("baseline run failed: " + var16.c(), List.of());
      } else {
         ArrayList<java.lang.String> var17 = new ArrayList();

         for (io.karatelabs.ext.mutation.AssertionEvidenceExtractor.FeatureEvidence var21 : var16.b()) {
            if ("failed".equals(var21.e()) && !var0.h().contains(var21.b())) {
               var17.add(var21.a() + "::" + var21.b());
            }
         }

         if (!var17.isEmpty()) {
            return invalidBaseline("the baseline suite is not green against the untouched rulebook", var17);
         } else {
            for (io.karatelabs.ext.rules.MutationGenerator.Mutant var22 : (Iterable<io.karatelabs.ext.rules.MutationGenerator.Mutant>) (Iterable<?>) (var3)) {
               io.karatelabs.ext.mutation.SuiteMutationResultBuilder.MutantRunResult var24 = (io.karatelabs.ext.mutation.SuiteMutationResultBuilder.MutantRunResult)var4.get(var22.d());
               if (var22.o() && var24 != null && !var24.a().c()) {
                  Path var26 = io.karatelabs.ext.mutation.MutantHomeMaterializer.materialize(var1.resolve(var0.b()), var22, var0.f().resolve("mutants"));
                  io.karatelabs.ext.mutation.SuiteMutationRunner.SuiteRunResult var11 = runSuite(var0, var26, var22.d(), var0.f().resolve("mutants").resolve(var22.d()));
                  if (var11.c() != null && var11.c().startsWith("REPLAY_TIMEOUT_UNKILLABLE")) {
                     LinkedHashMap<java.lang.String, java.lang.Object> var12 = new LinkedHashMap();
                     var12.put("error", "ABORTED_UNKILLABLE_REPLAY");
                     var12.put(
                        "message",
                        "the replay of "
                           + var22.d()
                           + " timed out and its thread could not be terminated; no scores were emitted (a hung run overlapping later mutants would corrupt attribution)"
                     );
                     return var12;
                  }

                  var4.put(var22.d(), new io.karatelabs.ext.mutation.SuiteMutationResultBuilder.MutantRunResult(var24.a(), var11.a(), var11.b(), var11.c()));
               }
            }

            var var20 = io.karatelabs.ext.mutation.SuiteMutationResultBuilder.buildResult(var3, var16.b(), var4, var0.g(), var2.g());
            LinkedHashMap<java.lang.String, java.lang.Object> var23 = new LinkedHashMap();
            var23.put("kind", "suite");
            var23.put("rulebook", var0.b());
            var23.put("suites", var0.e());
            var23.putAll(var20);

            try {
               Files.createDirectories(var0.f());
               Files.writeString(var0.f().resolve("mutation-report.json"), Json.stringifyStrict(var23));
            } catch (Exception var14) {
               LOGGER.warn("mutation: could not persist report: {}", var14.toString());
            }

            Path var25 = io.karatelabs.ext.mutation.MutationHtmlReportWriter.writeReport(var23, var0.f());
            LinkedHashMap<java.lang.String, java.lang.Object> var27 = new LinkedHashMap(var23);
            var27.put("reportDir", var0.f().toString());
            if (var25 != null) {
               var27.put("reportFile", var25.toString());
               var27.put("reportUrl", var25.toUri().toString());
            }

            return var27;
         }
      }
   }

   private static Map<String, Object> invalidBaseline(String var0, List<String> var1) {
      LinkedHashMap<java.lang.String, java.lang.Object> var2 = new LinkedHashMap();
      var2.put("error", "INVALID_BASELINE");
      var2.put("message", var0 + " — fix the baseline (or record an expected failure in the corpus) before any mutant is scored; no scores were emitted");
      if (!var1.isEmpty()) {
         var2.put("failures", var1);
      }

      return var2;
   }

   private static Map<String, Object> loadHandlerOverlay(io.karatelabs.ext.mutation.SuiteMutationRunner.SuiteMutationOptions var0, Rule var1) {
      String var2;
      try {
         var2 = Files.readString(var0.a().resolve(var0.j()));
      } catch (Exception var5) {
         throw new IllegalArgumentException("mutation: handlers overlay unreadable: " + var5);
      }

      Engine var3 = new Engine();
      var3.put("Rule", var1);
      Object var4 = var3.eval(var2);
      if (!(var4 instanceof Map)) {
         throw new IllegalArgumentException("mutation: " + var0.j() + " must evaluate to the operationId -> handler map");
      } else {
         return (Map<String, Object>)var4;
      }
   }

   private static io.karatelabs.ext.mutation.SuiteMutationRunner.SuiteRunResult runSuite(io.karatelabs.ext.mutation.SuiteMutationRunner.SuiteMutationOptions var0, Path var1, String var2, Path var3) {
      ArrayList<io.karatelabs.ext.mutation.SuiteMutationResultBuilder.RuleTraceEvent> var4 = new ArrayList();
      RuleEvaluator var5 = new RuleEvaluator();

      io.karatelabs.ext.mutation.SuiteMutationRunner.SuiteRunResult var16;
      label249: {
         io.karatelabs.ext.mutation.SuiteMutationRunner.SuiteRunResult var36;
         label248: {
            label247: {
               label246: {
                  io.karatelabs.ext.mutation.SuiteMutationRunner.SuiteRunResult var13;
                  try (Rule var6 = var0.j() == null ? null : Rule.atRulesHome(var1)) {
                     var var7 = Map.of(var0.d(), var0.b());
                     Map<String, Object> var8 = Map.of();
                     if (var6 != null) {
                        var6.onServeTrace(var2x -> var4.add(new io.karatelabs.ext.mutation.SuiteMutationResultBuilder.RuleTraceEvent(var0.d(), var4.size(), var2x.b(), var2x.c())));
                        var8 = loadHandlerOverlay(var0, var6);
                        var7 = Map.of();
                     }

                     OpenApiMockHandler var9 = new OpenApiMockHandler(
                           io.karatelabs.ext.openapi.OpenApiOperationCatalog.loadFromPath(var0.a().resolve(var0.c()).toString()),
                           var7,
                           var8,
                           new RulebookRepository(var1),
                           var5,
                           Session.inMemory(),
                           var0.k(),
                           var0.l(),
                           null
                        )
                        .withRuleTrace((var1x, var2x, var3x, var4x) -> var4.add(new io.karatelabs.ext.mutation.SuiteMutationResultBuilder.RuleTraceEvent(var1x, var2x, var3x, var4x)));
                     if (var2 != null) {
                        var9.withMutantId(var2);
                     }

                     MockInstance var10 = MockInstance.start(var9, 0, null, var0x -> null);
                     ExecutorService var11 = Executors.newSingleThreadExecutor();

                     try {
                        var var12 = var0.e().stream().map(var1x -> var0.a().resolve(var1x).toString()).toList();
                        Future var34 = var11.submit(
                           () -> var0.n()
                              .apply(
                                 Runner.path(var12.toArray(String[]::new))
                                    .systemProperty("baseUrl", var10.getUrl())
                                    .configDir(var0.a().toString())
                                    .workingDir(var0.a().toAbsolutePath())
                                    .outputDir(var3.resolve("run").toString())
                                    .outputJsonLines(true)
                                    .listener(io.karatelabs.ext.coverage.HttpExchangeEvidenceRecorder.embeddingListener())
                              )
                              .parallel(1)
                        );

                        try {
                           var34.get(var0.i(), TimeUnit.MILLISECONDS);
                        } catch (TimeoutException var29) {
                           var34.cancel(true);
                           var11.shutdownNow();

                           boolean var15;
                           try {
                              var15 = var11.awaitTermination(10L, TimeUnit.SECONDS);
                           } catch (InterruptedException var28) {
                              Thread.currentThread().interrupt();
                              var15 = false;
                           }

                           var16 = new io.karatelabs.ext.mutation.SuiteMutationRunner.SuiteRunResult(
                              var4,
                              List.of(),
                              var15
                                 ? "REPLAY_TIMEOUT after " + var0.i() + "ms"
                                 : "REPLAY_TIMEOUT_UNKILLABLE after " + var0.i() + "ms — the suite thread did not terminate; aborting the invocation"
                           );
                           break label249;
                        }

                        Path var14 = var3.resolve("run/karate-json/karate-events.jsonl");
                        if (!Files.exists(var14)) {
                           var36 = new io.karatelabs.ext.mutation.SuiteMutationRunner.SuiteRunResult(var4, List.of(), "MISSING_JSONL");
                           break label248;
                        }

                        io.karatelabs.ext.mutation.AssertionEvidenceExtractor.EvidenceExtractionResult var35 = io.karatelabs.ext.mutation.AssertionEvidenceExtractor.extract(var14);
                        if (!var35.isValid()) {
                           var16 = new io.karatelabs.ext.mutation.SuiteMutationRunner.SuiteRunResult(
                              var4,
                              List.of(),
                              "MALFORMED_EVIDENCE: "
                                 + var35.d()
                                 + " feature record(s) and "
                                 + var35.e()
                                 + " exchange embed(s) unreadable"
                                 + (var35.b().isEmpty() ? "; no scenario read" : "")
                           );
                           break label247;
                        }

                        var16 = new io.karatelabs.ext.mutation.SuiteMutationRunner.SuiteRunResult(var4, var35.b(), null);
                        break label246;
                     } catch (Exception var30) {
                        var13 = new io.karatelabs.ext.mutation.SuiteMutationRunner.SuiteRunResult(var4, List.of(), "RUN_FAILED: " + var30.getMessage());
                     } finally {
                        var11.shutdownNow();
                        var10.stop();
                     }
                  } catch (Throwable var33) {
                     try {
                        var5.close();
                     } catch (Throwable var26) {
                        var33.addSuppressed(var26);
                     }

                     throw var33;
                  }

                  var5.close();
                  return var13;
               }

               var5.close();
               return var16;
            }

            var5.close();
            return var16;
         }

         var5.close();
         return var36;
      }

      var5.close();
      return var16;
   }

   public record SuiteMutationOptions(Path a, String b, String c, String d, List<String> e, Path f, Map<String, String> g, Set<String> h, long i, String j, boolean k, boolean l, Object m, UnaryOperator<Runner.Builder> n) {

      public SuiteMutationOptions(Path var1, String var2, String var3, String var4, List<String> var5, Path var6, Map<String, String> var7, Set<String> var8, long var9) {
         this(var1, var2, var3, var4, var5, var6, var7, var8, var9, null, false, false, null, var0 -> var0);
      }


      public static io.karatelabs.ext.mutation.SuiteMutationRunner.SuiteMutationOptions create(Path var0, String var1, String var2, String var3, List<String> var4, Path var5) {
         return new io.karatelabs.ext.mutation.SuiteMutationRunner.SuiteMutationOptions(var0, var1, var2, var3, var4, var5, Map.of(), Set.of(), 120000L);
      }

      public io.karatelabs.ext.mutation.SuiteMutationRunner.SuiteMutationOptions withHandlerOverlay(String var1) {
         return new io.karatelabs.ext.mutation.SuiteMutationRunner.SuiteMutationOptions(this.a, this.b, this.c, this.d, this.e, this.f, this.g, this.h, this.i, var1, true, false, this.m, this.n);
      }

      public io.karatelabs.ext.mutation.SuiteMutationRunner.SuiteMutationOptions withMockArg(Object var1) {
         return new io.karatelabs.ext.mutation.SuiteMutationRunner.SuiteMutationOptions(this.a, this.b, this.c, this.d, this.e, this.f, this.g, this.h, this.i, this.j, this.k, this.l, var1, this.n);
      }

      public io.karatelabs.ext.mutation.SuiteMutationRunner.SuiteMutationOptions withRunnerConfig(UnaryOperator<Runner.Builder> var1) {
         return new io.karatelabs.ext.mutation.SuiteMutationRunner.SuiteMutationOptions(this.a, this.b, this.c, this.d, this.e, this.f, this.g, this.h, this.i, this.j, this.k, this.l, this.m, var1);
      }
   }

   private record SuiteRunResult(List<io.karatelabs.ext.mutation.SuiteMutationResultBuilder.RuleTraceEvent> a, List<io.karatelabs.ext.mutation.AssertionEvidenceExtractor.FeatureEvidence> b, String c) {

   }
}
