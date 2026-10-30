package io.karatelabs.ext.mutation;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class ResponseMutationResultBuilder {
   private ResponseMutationResultBuilder() {
   }

   public static Map<String, Object> buildResult(
      List<ResponseMutationSpec> var0, List<io.karatelabs.ext.mutation.AssertionEvidenceExtractor.FeatureEvidence> var1, Map<String, io.karatelabs.ext.mutation.ResponseMutationResultBuilder.ResponseMutationOutcome> var2, Map<String, String> var3, Set<String> var4, String var5, String var6, List<String> var7
   ) {
      io.karatelabs.ext.mutation.SuiteMutationResultBuilder.BaselineAssertionInventory var8 = io.karatelabs.ext.mutation.SuiteMutationResultBuilder.BaselineAssertionInventory.baselineInventory(var1);
      ArrayList<java.util.Map> var9 = new ArrayList();
      ArrayList<java.util.LinkedHashMap> var10 = new ArrayList();
      LinkedHashMap<java.lang.String, java.lang.Integer> var11 = new LinkedHashMap();

      for (String var13 : List.of("KILLED", "SURVIVED", "NOTCOVERED", "TIMEOUT", "INVALID", "SCREENED")) {
         var11.put(var13, 0);
      }

      int var29 = 0;
      int var30 = 0;
      int var14 = 0;
      int var15 = 0;
      ArrayList<java.lang.String> var16 = new ArrayList();

      for (ResponseMutationSpec var18 : var0) {
         io.karatelabs.ext.mutation.ResponseMutationResultBuilder.ResponseMutationOutcome var19 = (io.karatelabs.ext.mutation.ResponseMutationResultBuilder.ResponseMutationOutcome)var2.get(var18.d());
         String var21 = null;
         String var22 = null;
         String var23 = null;
         List var24 = null;
         String var20;
         if (var19 == null) {
            var20 = "INVALID";
            var21 = "NEVER_EXECUTED";
         } else if (var19.c() != null) {
            boolean var25 = var19.c().startsWith("REPLAY_TIMEOUT");
            var20 = var25 ? "TIMEOUT" : "INVALID";
            var21 = var19.c();
         } else if (var19.a() != null && !var19.a().isEmpty()) {
            io.karatelabs.ext.mutation.SuiteMutationResultBuilder.KillMatch var37 = io.karatelabs.ext.mutation.SuiteMutationResultBuilder.findKill(var19.b(), var8.a(), var3);
            if (var37 != null) {
               var20 = "KILLED";
               var22 = var37.a();
               var23 = var37.b();
               var24 = var37.c();
               if ("independent".equals(var37.a())) {
                  var29++;
               } else {
                  var30++;
               }

               if (var24 != null) {
                  var16.add(var18.d());
               }
            } else {
               var20 = "SURVIVED";
            }

            io.karatelabs.ext.mutation.SuiteMutationResultBuilder.KillMatch var26 = io.karatelabs.ext.mutation.SuiteMutationResultBuilder.findKill(var19.b(), var8.a(), var3, false);
            if (var26 != null) {
               var15++;
               if ("independent".equals(var26.a())) {
                  var14++;
               }
            }
         } else {
            boolean var36 = var19.b() != null && var19.b().stream().anyMatch(var1x -> "failed".equals(var1x.e()) && !var4.contains(var1x.b()));
            if (var36) {
               var20 = "INVALID";
               var21 = "the suite failed with no tamper applied — transport/infrastructure on this leg, not a coverage fact";
            } else {
               var20 = "NOTCOVERED";
               var21 = "no suite response carried the mutated site (" + var18.f() + " " + var18.g() + ")";
            }
         }

         var11.merge(var20, 1, Integer::sum);
         Map<java.lang.String, java.lang.Object> var38 = var18.toMap();
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

         if (var19 != null && var19.a() != null && !var19.a().isEmpty()) {
            var38.put("appliedCount", var19.a().size());
         }

         var9.add(var38);
         boolean var39 = "KILLED".equals(var20) || "SURVIVED".equals(var20);
         if (var39 && !"independent".equals(var22)) {
            LinkedHashMap<java.lang.String, java.lang.String> var27 = new LinkedHashMap();
            var27.put("id", var18.d());
            var27.put("mutatorName", var18.e());
            var27.put("site", var18.operationText());
            var27.put("finding", "no test would independently notice " + var18.operationText() + " changing (" + var18.k() + " -> " + var18.l() + ")");
            String var28 = io.karatelabs.ext.mutation.SuiteMutationResultBuilder.relationHint(var19 == null ? null : var19.b(), var8.a());
            if (var28 != null) {
               var27.put("hint", var28);
            }

            var27.put("status", var20);
            var10.add(var27);
         }
      }

      int var31 = (Integer)var11.get("KILLED") + (Integer)var11.get("SURVIVED");
      LinkedHashMap<java.lang.String, java.util.List> var32 = new LinkedHashMap();

      for (String var35 : List.of("NOTCOVERED", "INVALID", "TIMEOUT")) {
         var32.put(var35, var9.stream().filter(var1x -> var35.equals(var1x.get("status"))).map(var0x -> String.valueOf(var0x.get("id"))).toList());
      }

      LinkedHashMap<java.lang.String, java.lang.Object> var34 = new LinkedHashMap();
      var34.put("counts", var11);
      var34.put("denominator", var31);
      var34.put("independentlyKilled", var29);
      var34.put("rawKilled", var30 + var29);
      var34.put("independentScore", io.karatelabs.ext.mutation.SuiteMutationResultBuilder.ratio(var29, var31));
      var34.put("rawKillRate", io.karatelabs.ext.mutation.SuiteMutationResultBuilder.ratio(var30 + var29, var31));
      if (!var16.isEmpty()) {
         var34.put("relations", io.karatelabs.ext.mutation.SuiteMutationResultBuilder.relationSummary(var16, var14, var15, var31));
      }

      var34.put("baselineFailedAssertions", var8.b());
      var34.put("identityMode", var5);
      var34.put("rung", var6);
      var34.put("disclosures", var7);
      var34.put(
         "framing",
         "grades assertion strength against perturbed live responses; it never measures the SUT's robustness, and a survivor means no assertion would notice this response change"
      );
      var34.put("notCovered", var32.get("NOTCOVERED"));
      var34.put("screened", List.of());
      var34.put("invalid", var32.get("INVALID"));
      var34.put("timeout", var32.get("TIMEOUT"));
      var34.put("mutants", var9);
      var34.put("worklist", var10);
      return var34;
   }

   public record ResponseMutationOutcome(List<io.karatelabs.ext.mutation.ResponseMutatingHttpClient.AppliedMutationEvent> a, List<io.karatelabs.ext.mutation.AssertionEvidenceExtractor.FeatureEvidence> b, String c) {
   }
}
