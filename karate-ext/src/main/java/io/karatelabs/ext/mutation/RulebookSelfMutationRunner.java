package io.karatelabs.ext.mutation;

import io.karatelabs.common.Json;
import io.karatelabs.ext.rules.MutantScreener;
import io.karatelabs.ext.rules.RulebookRepository;
import io.karatelabs.ext.rules.RuleEvaluator;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

public final class RulebookSelfMutationRunner {
   private static final String ALWAYS_PROPERTY_KIND = "always";
   private static final String SOMETIMES_PROPERTY_KIND = "sometimes";
   private static final String EXPECTATION_KIND = "expect";

   private RulebookSelfMutationRunner() {
   }

   public static Map<String, Object> run(Path var0, String var1, Path var2) {
      return runWithOptions(var0, var1, var2, null);
   }

   public static Map<String, Object> runWithOptions(Path var0, String var1, Path var2, Object var3) {
      io.karatelabs.ext.rules.RulebookRepository.RulebookDocument var4 = new RulebookRepository(var0.resolve("rulebooks")).b(var1);
      var var5 = new io.karatelabs.ext.rules.MutationGenerator(var4.g(), var4.m()).a(io.karatelabs.ext.rules.MutationGenerator.MutationTarget.a(var3, var4));
      var var6 = var4.b();
      RuleEvaluator var7 = new RuleEvaluator();

      Map var44;
      label146: {
         Map var46;
         label145: {
            LinkedHashMap var60;
            try {
               io.karatelabs.ext.rules.MutantScreener.ScreeningDeck var8 = MutantScreener.a(var4);
               var var9 = var8.a();
               var var10 = MutantScreener.a(var4.g(), var4.m().e(), var9, var7);
               ArrayList<io.karatelabs.ext.mutation.RulebookSelfMutationRunner.ProbeEvaluationResult> var11 = new ArrayList(var9.size());

               for (Map var13 : (Iterable<Map>) (Iterable<?>) (var9)) {
                  var11.add(evaluateRule(var4.g(), var4.m().e(), var13, var7));
               }

               LinkedHashMap<java.lang.String, java.lang.String> var36 = new LinkedHashMap();
               LinkedHashSet<java.lang.String> var37 = new LinkedHashSet();
               LinkedHashSet var14 = new LinkedHashSet();
               LinkedHashSet var15 = new LinkedHashSet();

               for (int var16 = 0; var16 < var11.size(); var16++) {
                  io.karatelabs.ext.mutation.RulebookSelfMutationRunner.ProbeEvaluationResult var17 = (io.karatelabs.ext.mutation.RulebookSelfMutationRunner.ProbeEvaluationResult)var11.get(var16);
                  boolean var18 = var8.b().contains(var16);
                  int rowIndex = var16;
                  var17.e().forEach((var5x, var6x) -> {
                     if (!var6x) {
                        if (var18) {
                           var37.add(var5x);
                        } else {
                           var36.putIfAbsent(var5x, "row " + rowIndex + ": " + Json.stringifyStrict(var9.get(rowIndex)));
                        }
                     }
                  });
                  var15.addAll(var17.f());
               }

               var37.removeAll(var36.keySet());
               LinkedHashSet var38 = new LinkedHashSet();

               for (io.karatelabs.ext.mutation.RulebookSelfMutationRunner.ProbeEvaluationResult var42 : (Iterable<io.karatelabs.ext.mutation.RulebookSelfMutationRunner.ProbeEvaluationResult>) (Iterable<?>) (var11)) {
                  var38.addAll(var42.e().keySet());
                  var14.addAll(var42.g());
               }

               if (!var36.isEmpty()) {
                  var var41 = var36.entrySet().stream().map(var0x -> (String)var0x.getKey() + " (" + (String)var0x.getValue() + ")").toList();
                  var44 = invalidBaseline("calc.always propert" + (var41.size() == 1 ? "y" : "ies") + " red on the UNTOUCHED rulebook: " + var41, var41);
                  break label146;
               }

               List var40 = var14.stream().filter(var1x -> !var15.contains(var1x)).toList();
               ArrayList<java.lang.String> var43 = new ArrayList();
               int var19 = 0;

               for (int var20 = 0; var20 < var6.size(); var20++) {
                  if (((Map)var6.get(var20)).get("_expect") instanceof Map var22) {
                     var19++;
                     io.karatelabs.ext.mutation.RulebookSelfMutationRunner.ProbeEvaluationResult var23 = (io.karatelabs.ext.mutation.RulebookSelfMutationRunner.ProbeEvaluationResult)var11.get(var20);
                     String var24 = io.karatelabs.ext.rules.ExpectedResultMatcher.a(var22, var23.a(), var23.b(), var23.c(), var23.d());
                     if (var24 != null) {
                        var43.add(((Map)var6.get(var20)).get("_id") + ": " + var24);
                     }
                  }
               }

               if (!var43.isEmpty()) {
                  var46 = invalidBaseline("stored _expect expectation(s) the UNTOUCHED rulebook itself violates: " + var43, var43);
                  break label145;
               }

               io.karatelabs.ext.rules.RequirementClaimAnalyzer var45 = io.karatelabs.ext.rules.RequirementClaimAnalyzer.a(var4.g(), var4.m());
               ArrayList<java.util.Map<java.lang.String,java.lang.Object>> var47 = new ArrayList();
               ArrayList<java.util.LinkedHashMap> var48 = new ArrayList();
               int var49 = 0;
               LinkedHashMap<java.lang.String, java.lang.Integer> var50 = new LinkedHashMap();

               for (String var26 : List.of("KILLED", "SURVIVED", "SCREENED", "INVALID")) {
                  var50.put(var26, 0);
               }

               for (io.karatelabs.ext.rules.MutationGenerator.Mutant var53 : (Iterable<io.karatelabs.ext.rules.MutationGenerator.Mutant>) (Iterable<?>) (var5)) {
                  var var27 = var53.c();
                  String var28;
                  if (!var53.o()) {
                     var28 = "INVALID";
                  } else {
                     io.karatelabs.ext.rules.MutantScreener.ScreeningResult var29 = MutantScreener.a(var53, var9, var10, var7, var8.b());
                     if (var29.c()) {
                        var28 = "SCREENED";
                        var27.put(
                           "statusReason", "indistinguishable on the probe universe (" + var29.d() + " rows) — no oracle in this lane could ever notice it"
                        );
                     } else {
                        List<Map<String, Object>> var30 = findKillOracles(var53, var9, var6, var11, var15, var37, var8.b(), var7);
                        if (var30.isEmpty()) {
                           var28 = "SURVIVED";
                           var60 = new LinkedHashMap();
                           var60.put("id", var53.d());
                           var60.put("mutatorName", var53.e());
                           var60.put("site", var53.f());
                           var var32 = io.karatelabs.ext.mutation.SuiteMutationResultBuilder.requirementsForMutant(var45, var53);
                           if (!var32.isEmpty()) {
                              var60.put("reqs", var32);
                           }

                           var60.put(
                              "finding",
                              "nothing in the rulebook's own oracle book would notice "
                                 + var53.k()
                                 + " changing ("
                                 + var53.i()
                                 + " -> "
                                 + var53.j()
                                 + ")"
                                 + (var32.isEmpty() ? "" : " — leaves " + String.join(", ", var32) + " un-defended")
                           );
                           LinkedHashMap<java.lang.String, java.lang.Object> var33 = new LinkedHashMap();
                           var33.put("input", var9.get(var29.e()));
                           var33.put("divergence", var29.f());
                           if (var29.g()) {
                              var33.put("domain", "out");
                           }

                           var60.put("counterexample", var33);
                           var60.put(
                              "fix",
                              var29.g()
                                 ? "this mutant diverges only outside the declared universe (D241), where no row decides a verdict — widen the generator's bounds if that input is real, else nothing in this lane can defend it"
                                 : "store the counterexample row as a scenario with an _expect, or declare the calc.always property that forbids it"
                           );
                           var48.add(var60);
                        } else {
                           var28 = "KILLED";
                           var27.put("oracles", var30);
                           boolean var59 = var30.stream().anyMatch(var0x -> !"expect".equals(var0x.get("kind")));
                           var27.put("killClass", var59 ? "independent" : "raw");
                           if (var59) {
                              var49++;
                           }
                        }
                     }
                  }

                  var50.merge(var28, 1, Integer::sum);
                  var27.put("status", var28);
                  var47.add(var27);
               }

               int var52 = (Integer)var50.get("KILLED");
               int var54 = var52 + (Integer)var50.get("SURVIVED");
               LinkedHashMap<java.lang.String, java.lang.Object> var55 = new LinkedHashMap();
               var55.put("kind", "self-run");
               var55.put("rulebook", var1);
               var55.put("counts", var50);
               var55.put("denominator", var54);
               var55.put("noticed", var52);
               var55.put("selfRunScore", io.karatelabs.ext.mutation.SuiteMutationResultBuilder.ratio(var52, var54));
               var55.put("independentlyKilled", var49);
               var55.put("rawKilled", var52);
               var55.put("independentScore", io.karatelabs.ext.mutation.SuiteMutationResultBuilder.ratio(var49, var54));
               var55.put("rawKillRate", io.karatelabs.ext.mutation.SuiteMutationResultBuilder.ratio(var52, var54));
               var55.put(
                  "independence",
                  "independent here = noticed by a declared calc.always/sometimes property, which must hold for every probe row; raw counts every notice, a stored _expect row included. The two are read side by side, never blended"
               );
               var55.put(
                  "framing",
                  "grades the rulebook's OWN oracles (stored _expect expectations + calc.always/sometimes properties) — no suite is involved, and this number is never comparable with a suite-graded mutation score"
               );
               LinkedHashMap<java.lang.String, java.lang.Object> var56 = new LinkedHashMap();
               var56.put("always", new ArrayList(var38));
               var56.put("sometimes", var14.stream().filter(var15::contains).toList());
               var56.put("inertSometimes", var40);
               if (!var37.isEmpty()) {
                  var56.put("robustnessAlways", new ArrayList(var37));
               }

               var56.put("expectations", var19);
               var55.put("oracleBook", var56);
               var55.put("screened", idsByStatus(var47, "SCREENED"));
               var55.put("invalid", idsByStatus(var47, "INVALID"));
               var55.put("mutants", var47);
               var55.put("worklist", var48);
               persistReport(var55, var2);
               Path var57 = io.karatelabs.ext.mutation.MutationHtmlReportWriter.writeReport(var55, var2);
               LinkedHashMap<java.lang.String, java.lang.String> var58 = new LinkedHashMap(var55);
               var58.put("reportDir", var2.toString());
               if (var57 != null) {
                  var58.put("reportFile", var57.toString());
                  var58.put("reportUrl", var57.toUri().toString());
               }

               var60 = var58;
            } catch (Throwable var35) {
               try {
                  var7.close();
               } catch (Throwable var34) {
                  var35.addSuppressed(var34);
               }

               throw var35;
            }

            var7.close();
            return var60;
         }

         var7.close();
         return var46;
      }

      var7.close();
      return var44;
   }

   private static List<Map<String, Object>> findKillOracles(
      io.karatelabs.ext.rules.MutationGenerator.Mutant var0,
      List<Map<String, Object>> var1,
      List<Map<String, Object>> var2,
      List<io.karatelabs.ext.mutation.RulebookSelfMutationRunner.ProbeEvaluationResult> var3,
      Set<String> var4,
      Set<String> var5,
      Set<Integer> var6,
      RuleEvaluator var7
   ) {
      ArrayList<io.karatelabs.ext.mutation.RulebookSelfMutationRunner.ProbeEvaluationResult> var8 = new ArrayList(var1.size());

      for (Map var10 : var1) {
         var8.add(evaluateRule(var0.m(), var0.b(), var10, var7));
      }

      ArrayList<java.util.Map<java.lang.String,java.lang.Object>> var18 = new ArrayList();
      LinkedHashSet<java.lang.String> var19 = new LinkedHashSet();

      for (int var11 = 0; var11 < var1.size(); var11++) {
         io.karatelabs.ext.mutation.RulebookSelfMutationRunner.ProbeEvaluationResult var12 = (io.karatelabs.ext.mutation.RulebookSelfMutationRunner.ProbeEvaluationResult)var3.get(var11);
         io.karatelabs.ext.mutation.RulebookSelfMutationRunner.ProbeEvaluationResult var13 = (io.karatelabs.ext.mutation.RulebookSelfMutationRunner.ProbeEvaluationResult)var8.get(var11);
         boolean var14 = var6.contains(var11);

         for (Entry var16 : var12.e().entrySet()) {
            if ((Boolean)var16.getValue() && (!var14 || !var5.contains(var16.getKey()))) {
               boolean var17 = var13.a() || Boolean.FALSE.equals(var13.e().get(var16.getKey()));
               if (var17 && var19.add("always:" + (String)var16.getKey())) {
                  var18.add(
                     oracleFinding(
                        "always",
                        (String)var16.getKey(),
                        var11,
                        var13.a() ? "the mutant run throws where baseline's held the property" : "the property evaluates false on the mutant"
                     )
                  );
               }
            }
         }
      }

      for (String var22 : var4) {
         boolean var24 = var8.stream().anyMatch(var1x -> var1x.f().contains(var22));
         if (!var24 && var19.add("sometimes:" + var22)) {
            var18.add(oracleFinding("sometimes", var22, null, "the situation is extinguished — no probe row reaches it on the mutant"));
         }
      }

      for (int var21 = 0; var21 < var2.size(); var21++) {
         if (((Map)var2.get(var21)).get("_expect") instanceof Map var25) {
            io.karatelabs.ext.mutation.RulebookSelfMutationRunner.ProbeEvaluationResult var26 = (io.karatelabs.ext.mutation.RulebookSelfMutationRunner.ProbeEvaluationResult)var8.get(var21);
            String var27 = io.karatelabs.ext.rules.ExpectedResultMatcher.a(var25, var26.a(), var26.b(), var26.c(), var26.d());
            String var28 = String.valueOf(((Map)var2.get(var21)).get("_id"));
            if (var27 != null && var19.add("expect:" + var28)) {
               var18.add(oracleFinding("expect", var28, var21, var27));
            }
         }
      }

      return var18;
   }

   private static Map<String, Object> oracleFinding(String var0, String var1, Integer var2, String var3) {
      LinkedHashMap<java.lang.String, java.lang.Object> var4 = new LinkedHashMap();
      var4.put("kind", var0);
      var4.put("name", var1);
      if (var2 != null) {
         var4.put("probeRow", var2);
      }

      var4.put("why", var3);
      return var4;
   }

   private static io.karatelabs.ext.mutation.RulebookSelfMutationRunner.ProbeEvaluationResult evaluateRule(String var0, Map<String, Object> var1, Map<String, Object> var2, RuleEvaluator var3) {
      try {
         io.karatelabs.ext.rules.RuleEvaluator.RuleEvaluationResult var4 = var3.a(var0, var1, var2, null);
         LinkedHashMap<java.lang.String, java.lang.Boolean> var5 = new LinkedHashMap();
         LinkedHashSet<java.lang.String> var6 = new LinkedHashSet();
         LinkedHashSet<java.lang.String> var7 = new LinkedHashSet();

         for (Object[] var9 : var4.i()) {
            String var10 = String.valueOf(var9[0]);
            boolean var11 = Boolean.TRUE.equals(var9[2]);
            String var12 = String.valueOf(var9[3]);
            if ("always".equals(var12)) {
               var5.merge(var10, var11, Boolean::logicalAnd);
            } else if ("sometimes".equals(var12)) {
               var7.add(var10);
               if (var11) {
                  var6.add(var10);
               }
            }
         }

         return new io.karatelabs.ext.mutation.RulebookSelfMutationRunner.ProbeEvaluationResult(false, var4.b(), var4.f(), var4.g(), var5, var6, var7);
      } catch (RuntimeException var13) {
         return new io.karatelabs.ext.mutation.RulebookSelfMutationRunner.ProbeEvaluationResult(true, null, null, false, Map.of(), Set.of(), Set.of());
      }
   }

   private static List<String> idsByStatus(List<Map<String, Object>> var0, String var1) {
      return var0.stream().filter(var1x -> var1.equals(var1x.get("status"))).map(var0x -> String.valueOf(var0x.get("id"))).toList();
   }

   private static Map<String, Object> invalidBaseline(String var0, List<String> var1) {
      LinkedHashMap<java.lang.String, java.lang.Object> var2 = new LinkedHashMap();
      var2.put("error", "INVALID_BASELINE");
      var2.put("code", "INVALID_BASELINE");
      var2.put("status", 422);
      var2.put("message", var0 + " — fix the rulebook (or the stored expectation) before any mutant is graded; no scores were emitted");
      var2.put("details", var1);
      return var2;
   }

   private static void persistReport(Map<String, Object> var0, Path var1) {
      try {
         Files.createDirectories(var1);
         Files.writeString(var1.resolve("mutation-self-report.json"), Json.stringifyStrict(var0));
      } catch (Exception var3) {
         throw new RuntimeException("could not persist the self-run report to " + var1 + ": " + var3, var3);
      }
   }

   private record ProbeEvaluationResult(boolean a, Object b, Object c, boolean d, Map<String, Boolean> e, Set<String> f, Set<String> g) {

   }
}
