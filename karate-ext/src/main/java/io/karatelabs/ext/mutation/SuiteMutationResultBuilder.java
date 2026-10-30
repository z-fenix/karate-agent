package io.karatelabs.ext.mutation;

import io.karatelabs.ext.rules.RuleTableCatalog;
import io.karatelabs.ext.rules.MutationTraceCollector;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class SuiteMutationResultBuilder {
   public static final String KILLED = "KILLED";
   public static final String SURVIVED = "SURVIVED";
   public static final String NOT_COVERED = "NOTCOVERED";
   public static final String TIMEOUT = "TIMEOUT";
   public static final String INVALID = "INVALID";
   public static final String SCREENED = "SCREENED";
   private static final List<String> SHARED_CAPTURE_SOURCES = List.of("copied-response", "shared-rule", "request-origin");

   private SuiteMutationResultBuilder() {
   }

   static io.karatelabs.ext.mutation.SuiteMutationResultBuilder.KillMatch findKill(List<io.karatelabs.ext.mutation.AssertionEvidenceExtractor.FeatureEvidence> var0, Map<String, io.karatelabs.ext.mutation.AssertionEvidenceExtractor.AssertionEvidence> var1, Map<String, String> var2) {
      return findKill(var0, var1, var2, true);
   }

   static io.karatelabs.ext.mutation.SuiteMutationResultBuilder.KillMatch findKill(List<io.karatelabs.ext.mutation.AssertionEvidenceExtractor.FeatureEvidence> var0, Map<String, io.karatelabs.ext.mutation.AssertionEvidenceExtractor.AssertionEvidence> var1, Map<String, String> var2, boolean var3) {
      String var4 = null;
      String var5 = null;
      List var6 = null;

      for (io.karatelabs.ext.mutation.AssertionEvidenceExtractor.FeatureEvidence var8 : var0) {
         for (io.karatelabs.ext.mutation.AssertionEvidenceExtractor.AssertionEvidence var10 : var8.f()) {
            if (Boolean.FALSE.equals(var10.k()) && var1.containsKey(var10.identity()) && (var3 || var10.n() < 0)) {
               String var11 = (String)var2.get(var10.identity());
               if (var11 == null) {
                  var11 = (String)var2.get("feature:" + var10.c());
               }

               String var12 = classifyKill(var10, (io.karatelabs.ext.mutation.AssertionEvidenceExtractor.AssertionEvidence)var1.get(var10.identity()), var11);
               if (var12 != null) {
                  boolean var13 = "relational".equals(var10.l());
                  if ("independent".equals(var12) && !var13) {
                     var4 = var12;
                     var5 = var10.identity();
                     var6 = null;
                  } else if ("independent".equals(var12)) {
                     if (!"independent".equals(var4) || var6 != null) {
                        var4 = var12;
                        var5 = var10.identity();
                        var6 = var10.o();
                     }
                  } else if (var4 == null) {
                     var4 = var12;
                     var5 = var10.identity();
                  }
               }
            }
         }
      }

      return var4 == null ? null : new io.karatelabs.ext.mutation.SuiteMutationResultBuilder.KillMatch(var4, var5, var6);
   }

   static String relationHint(List<io.karatelabs.ext.mutation.AssertionEvidenceExtractor.FeatureEvidence> var0, Map<String, io.karatelabs.ext.mutation.AssertionEvidenceExtractor.AssertionEvidence> var1) {
      if (var0 == null) {
         return null;
      } else {
         for (io.karatelabs.ext.mutation.AssertionEvidenceExtractor.FeatureEvidence var3 : var0) {
            for (io.karatelabs.ext.mutation.AssertionEvidenceExtractor.AssertionEvidence var5 : var3.f()) {
               if (var5.p() != null && Boolean.FALSE.equals(var5.k()) && var1.containsKey(var5.identity())) {
                  return var5.p() + " — make it a relation: change one input on the same operation and compare the two responses";
               }
            }
         }

         return null;
      }
   }

   static Map<String, Object> relationSummary(List<String> var0, int var1, int var2, int var3) {
      LinkedHashMap<java.lang.String, java.lang.Object> var4 = new LinkedHashMap();
      var4.put("independentlyKilled", var1);
      var4.put("rawKilled", var2);
      var4.put("independentScore", ratio(var1, var3));
      var4.put("rawKillRate", ratio(var2, var3));
      LinkedHashMap<java.lang.String, java.lang.Object> var5 = new LinkedHashMap();
      var5.put("killed", var0);
      var5.put("captureReuseReading", var4);
      var5.put("disclosure", "partial oracle: a mutant that moves both sides of a relation consistently escapes");
      return var5;
   }

   static List<String> requirementsForMutant(io.karatelabs.ext.rules.RequirementClaimAnalyzer var0, io.karatelabs.ext.rules.MutationGenerator.Mutant var1) {
      if (var1.r() != null) {
         return var0.a(var1.r());
      } else if (var1.h() >= 0) {
         return var0.a(var1.h());
      } else {
         return var1.g() > 0 ? var0.b(var1.g()) : List.of();
      }
   }

   public static Map<String, Object> buildResult(List<io.karatelabs.ext.rules.MutationGenerator.Mutant> var0, List<io.karatelabs.ext.mutation.AssertionEvidenceExtractor.FeatureEvidence> var1, Map<String, io.karatelabs.ext.mutation.SuiteMutationResultBuilder.MutantRunResult> var2, Map<String, String> var3, String var4) {
      io.karatelabs.ext.mutation.SuiteMutationResultBuilder.BaselineAssertionInventory var5 = io.karatelabs.ext.mutation.SuiteMutationResultBuilder.BaselineAssertionInventory.baselineInventory(var1);
      var var6 = var5.a();
      int var7 = var5.b();
      io.karatelabs.ext.rules.RequirementClaimAnalyzer var8 = io.karatelabs.ext.rules.RequirementClaimAnalyzer.a(var4, var0.isEmpty() ? RuleTableCatalog.c : ((io.karatelabs.ext.rules.MutationGenerator.Mutant)var0.get(0)).a());
      ArrayList<java.util.Map<java.lang.String,java.lang.Object>> var9 = new ArrayList();
      ArrayList<java.util.LinkedHashMap> var10 = new ArrayList();
      LinkedHashMap<java.lang.String, java.lang.Integer> var11 = new LinkedHashMap();

      for (String var13 : List.of("KILLED", "SURVIVED", "NOTCOVERED", "TIMEOUT", "INVALID", "SCREENED")) {
         var11.put(var13, 0);
      }

      int var30 = 0;
      int var31 = 0;
      int var14 = 0;
      int var15 = 0;
      ArrayList<java.lang.String> var16 = new ArrayList();

      for (io.karatelabs.ext.rules.MutationGenerator.Mutant var18 : var0) {
         io.karatelabs.ext.mutation.SuiteMutationResultBuilder.MutantRunResult var19 = (io.karatelabs.ext.mutation.SuiteMutationResultBuilder.MutantRunResult)var2.get(var18.d());
         String var21 = null;
         String var22 = null;
         String var23 = null;
         List var24 = null;
         String var20;
         if (!var18.o()) {
            var20 = "INVALID";
            var21 = var18.p();
         } else if (var19 != null && var19.d() != null) {
            boolean var37 = var19.d().startsWith("REPLAY_TIMEOUT");
            var20 = var37 ? "TIMEOUT" : "INVALID";
            var21 = var19.d();
         } else if (var19 != null && var19.a() != null && var19.a().c()) {
            var20 = "SCREENED";
            var21 = "indistinguishable on the enumerated deck (" + var19.a().d() + " rows)";
         } else if (var19 == null) {
            var20 = "INVALID";
            var21 = "NEVER_EXECUTED";
         } else if (!isCovered(var18, var19.b())) {
            var20 = "NOTCOVERED";
            var21 = "no suite request evaluated the mutated site (" + var18.l().b() + (var18.l().c() != null ? " " + var18.l().c() : "") + ")";
         } else {
            io.karatelabs.ext.mutation.SuiteMutationResultBuilder.KillMatch var25 = findKill(var19.c(), var6, var3);
            if (var25 != null) {
               var20 = "KILLED";
               var22 = var25.a();
               var23 = var25.b();
               var24 = var25.c();
               if ("independent".equals(var25.a())) {
                  var30++;
               } else {
                  var31++;
               }

               if (var24 != null) {
                  var16.add(var18.d());
               }
            } else {
               var20 = "SURVIVED";
            }

            io.karatelabs.ext.mutation.SuiteMutationResultBuilder.KillMatch var26 = findKill(var19.c(), var6, var3, false);
            if (var26 != null) {
               var15++;
               if ("independent".equals(var26.a())) {
                  var14++;
               }
            }
         }

         var11.merge(var20, 1, Integer::sum);
         var var38 = var18.c();
         var38.put("status", var20);
         if (var21 != null) {
            var38.put("statusReason", var21);
         }

         if (var22 != null) {
            var38.put("killClass", var22);
            var38.put("killedBy", var23);
         }

         if (var24 != null) {
            var38.put("relation", Map.of("axes", var24));
         }

         var9.add(var38);
         boolean var39 = "KILLED".equals(var20) || "SURVIVED".equals(var20);
         if (var39 && !"independent".equals(var22)) {
            LinkedHashMap<java.lang.String, java.lang.Object> var27 = new LinkedHashMap();
            var27.put("id", var18.d());
            var27.put("mutatorName", var18.e());
            var27.put("site", var18.f());
            var var28 = requirementsForMutant(var8, var18);
            if (!var28.isEmpty()) {
               var27.put("reqs", var28);
            }

            var27.put(
               "finding",
               "no test would independently notice "
                  + var18.k()
                  + " changing ("
                  + var18.i()
                  + " -> "
                  + var18.j()
                  + ")"
                  + (var28.isEmpty() ? "" : " — leaves " + String.join(", ", var28) + " un-defended")
            );
            String var29 = relationHint(var19 == null ? null : var19.c(), var6);
            if (var29 != null) {
               var27.put("hint", var29);
            }

            var27.put("status", var20);
            var10.add(var27);
         }
      }

      int var32 = (Integer)var11.get("KILLED") + (Integer)var11.get("SURVIVED");
      LinkedHashMap<java.lang.String, java.util.List> var33 = new LinkedHashMap();

      for (String var36 : List.of("NOTCOVERED", "SCREENED", "INVALID", "TIMEOUT")) {
         var33.put(var36, var9.stream().filter(var1x -> var36.equals(var1x.get("status"))).map(var0x -> String.valueOf(var0x.get("id"))).toList());
      }

      LinkedHashMap<java.lang.String, java.lang.Object> var35 = new LinkedHashMap();
      var35.put("counts", var11);
      var35.put("denominator", var32);
      var35.put("independentlyKilled", var30);
      var35.put("rawKilled", var31 + var30);
      var35.put("independentScore", ratio(var30, var32));
      var35.put("rawKillRate", ratio(var31 + var30, var32));
      if (!var16.isEmpty()) {
         var35.put("relations", relationSummary(var16, var14, var15, var32));
      }

      var35.put("baselineFailedAssertions", var7);
      var35.put("framing", "grades assertion strength against the rulebook; transfer to the real API = rulebook fidelity");
      var35.put("notCovered", var33.get("NOTCOVERED"));
      var35.put("screened", var33.get("SCREENED"));
      var35.put("invalid", var33.get("INVALID"));
      var35.put("timeout", var33.get("TIMEOUT"));
      var35.put("mutants", var9);
      var35.put("worklist", var10);
      return var35;
   }

   private static String classifyKill(io.karatelabs.ext.mutation.AssertionEvidenceExtractor.AssertionEvidence var0, io.karatelabs.ext.mutation.AssertionEvidenceExtractor.AssertionEvidence var1, String var2) {
      if ("capture-reuse".equals(var0.l())) {
         return null;
      } else if (var2 != null && SHARED_CAPTURE_SOURCES.contains(var2)) {
         return "raw";
      } else {
         boolean var3 = "relational".equals(var0.l());
         if (var3 && !"relational".equals(var1.l())) {
            return "capture-reuse".equals(var1.l()) ? null : "raw";
         } else {
            return (var3 || "authored".equals(var0.l())) && "replay".equals(var0.j()) ? "independent" : "raw";
         }
      }
   }

   private static boolean isCovered(io.karatelabs.ext.rules.MutationGenerator.Mutant var0, List<io.karatelabs.ext.mutation.SuiteMutationResultBuilder.RuleTraceEvent> var1) {
      return var1 != null && io.karatelabs.ext.rules.MutationTraceCollector.a(var0, var1.stream().map(io.karatelabs.ext.mutation.SuiteMutationResultBuilder.RuleTraceEvent::c).toList());
   }

   static double ratio(int var0, int var1) {
      return var1 == 0 ? 0.0 : Math.round(var0 * 10000.0 / var1) / 10000.0;
   }

   record BaselineAssertionInventory(Map<String, io.karatelabs.ext.mutation.AssertionEvidenceExtractor.AssertionEvidence> a, int b) {


      static io.karatelabs.ext.mutation.SuiteMutationResultBuilder.BaselineAssertionInventory baselineInventory(List<io.karatelabs.ext.mutation.AssertionEvidenceExtractor.FeatureEvidence> var0) {
         LinkedHashMap<java.lang.String, io.karatelabs.ext.mutation.AssertionEvidenceExtractor.AssertionEvidence> var1 = new LinkedHashMap();
         int var2 = 0;

         for (io.karatelabs.ext.mutation.AssertionEvidenceExtractor.FeatureEvidence var4 : var0) {
            for (io.karatelabs.ext.mutation.AssertionEvidenceExtractor.AssertionEvidence var6 : var4.f()) {
               if (Boolean.TRUE.equals(var6.k())) {
                  var1.put(var6.identity(), var6);
               } else if (Boolean.FALSE.equals(var6.k())) {
                  var2++;
               }
            }
         }

         return new io.karatelabs.ext.mutation.SuiteMutationResultBuilder.BaselineAssertionInventory(var1, var2);
      }
   }

   public record MutantRunResult(io.karatelabs.ext.rules.MutantScreener.ScreeningResult a, List<io.karatelabs.ext.mutation.SuiteMutationResultBuilder.RuleTraceEvent> b, List<io.karatelabs.ext.mutation.AssertionEvidenceExtractor.FeatureEvidence> c, String d) {

   }

   record KillMatch(String a, String b, List<String> c) {

   }

   public record RuleTraceEvent(String a, int b, io.karatelabs.ext.rules.MutationTraceCollector.MutationTraceSnapshot c, boolean d) {

   }
}
