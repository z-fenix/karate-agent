package io.karatelabs.ext.pair;

import io.karatelabs.common.Json;
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

public final class CoverageClaimBuilder {
   public static final String COVERAGE_DATA_PATH = "ext/coverage/coverage-data.json";

   private CoverageClaimBuilder() {
   }

   public static Map<String, Object> buildClaim(Path var0, List<io.karatelabs.ext.pair.PairOutcomeClassifier.ScenarioClassification> var1, List<String> var2, boolean var3, ContractSchema var4, io.karatelabs.ext.pair.PairReportAssembler.PairAssessmentContext var5) {
      var var6 = buildClaimInternal(var0, var1, var2, var3, var4, var5);
      if (var4 == io.karatelabs.ext.pair.ContractSchema.EMPTY) {
         var6.put(
            "noSpec",
            "this pair was minted without a `spec`, so it is bound to no contract document: operations are named by method and path, and no response headers were compared. Freshness cannot watch a spec that was never named. To bind the claim to a contract, commit an OpenAPI document and pass it as `spec` on the next mint."
         );
      }

      return var6;
   }

   private static Map<String, Object> buildClaimInternal(Path var0, List<io.karatelabs.ext.pair.PairOutcomeClassifier.ScenarioClassification> var1, List<String> var2, boolean var3, ContractSchema var4, io.karatelabs.ext.pair.PairReportAssembler.PairAssessmentContext var5) {
      var var6 = loadCoverageData(var0);
      if (var6 == null) {
         return a(
            a(
               "no coverage graph was readable under "
                  + var0.resolve("ext/coverage/coverage-data.json")
                  + ", so there is no denominator and no percentage. The divergence set below stands on its own as a list. To measure the proportion, run the pair over a project whose coverage source is bound (a `cov.openapi` in karate-boot.js) so the run writes the graph the denominator is read off."
            ),
            var2,
            var3
         );
      } else {
         List var7 = (List)(var6.getOrDefault("items", List.of()));
         List var8 = (List)(var6.getOrDefault("hits", List.of()));
         TreeSet<java.lang.String> var9 = new TreeSet();

         for (Map var11 : (Iterable<Map>) (Iterable<?>) (var7)) {
            String var12 = String.valueOf(var11.get("id"));
            if (b(var12)) {
               var9.add(var12);
            }
         }

         if (var9.isEmpty()) {
            return a(
               a(
                  "the coverage graph under "
                     + var0.resolve("ext/coverage/coverage-data.json")
                     + " declares no API operations, so there is nothing to take a proportion over. Bind the spec as a coverage source (`cov.openapi`) and re-run the pair."
               ),
               var2,
               var3
            );
         } else {
            LinkedHashMap<java.lang.String, java.util.LinkedHashSet> var22 = new LinkedHashMap();

            for (Map var25 : (Iterable<Map>) (Iterable<?>) (var8)) {
               String var13 = String.valueOf(var25.get("item"));
               if (var9.contains(var13) && "OBSERVED".equals(String.valueOf(var25.get("kind")))) {
                  var22.computeIfAbsent(var13, var0x -> new LinkedHashSet<>()).add(String.valueOf(var25.get("test")));
               }
            }

            LinkedHashMap<java.lang.String, io.karatelabs.ext.pair.PairOutcomeClassifier.ScenarioClassification> var24 = new LinkedHashMap();

            for (io.karatelabs.ext.pair.PairOutcomeClassifier.ScenarioClassification var28 : var1) {
               var24.putIfAbsent(var28.e(), var28);
            }

            ArrayList<java.lang.String> var27 = new ArrayList();
            ArrayList<java.lang.String> var29 = new ArrayList();
            ArrayList<java.lang.String> var14 = new ArrayList();

            for (Entry var16 : (Iterable<Entry>) (Iterable<?>) (var22.entrySet())) {
               boolean var17 = false;
               boolean var18 = false;

               for (String var20 : (Iterable<String>) (Iterable<?>) ((Set)var16.getValue())) {
                  io.karatelabs.ext.pair.PairOutcomeClassifier.ScenarioClassification var21 = (io.karatelabs.ext.pair.PairOutcomeClassifier.ScenarioClassification)var24.get(var20);
                  if (var21 != null && var21.b()) {
                     var17 = true;
                     var18 |= var21.c();
                  }
               }

               if (!var17) {
                  var14.add((String)var16.getKey());
               } else {
                  var29.add((String)var16.getKey());
                  if (var18) {
                     var27.add((String)var16.getKey());
                  }
               }
            }

            var var30 = new ArrayList<>(var9);
            var30.removeAll(var22.keySet());
            ArrayList<io.karatelabs.ext.pair.PairReportAssembler.ScenarioAssessment> var31 = new ArrayList();

            for (Entry var35 : (Iterable<Entry>) (Iterable<?>) (var22.entrySet())) {
               List var37 = ((Set)var35.getValue()).stream().map(var24::get).filter(Objects::nonNull).toList();
               var31.add(io.karatelabs.ext.pair.PairReportAssembler.assessScenario((String)var35.getKey(), var37, var5));
            }

            if (var22.isEmpty()) {
               return a(
                  a(
                     "this pair exercised NO declared operation: of the "
                        + var9.size()
                        + " the spec declares, not one was reached by this pair — so there is nothing to take a proportion over and no per-operation claim to make. Read `scenarios` FIRST: if it is empty the suite selected nothing (check the path and any tag expression — both match silently); if it is full of `notmeasured` rows, each names its own cause (`environment` for an unreachable target). Either way the empty divergence set is the absence of evidence, never agreement."
                  ),
                  var2,
                  var3
               );
            } else if (var29.isEmpty()) {
               var var34 = a(
                  "this pair exercised "
                     + var22.size()
                     + " of "
                     + var9.size()
                     + " declared operations, and none of them was admissible. Every scenario that touched them was `notmeasured`, so there is no proportion to take. Read the per-scenario causes. `0 of 0` means the check had nothing to check, and it never reports as a passed check."
               );
               var34.put("claimCoverage", a(var22.size(), var9.size(), var30));
               var34.put("notMeasured", new ArrayList(var22.keySet()));
               a(var34, var2, var3);
               return var34;
            } else {
               LinkedHashMap<java.lang.String, java.lang.Object> var33 = new LinkedHashMap();
               var33.put("measured", true);
               var33.put("unit", "operation");
               LinkedHashMap<java.lang.String, java.lang.Object> var36 = new LinkedHashMap();
               int var38 = var29.size() - var27.size();
               var36.put("divergenceFree", var38);
               var36.put("admissible", var29.size());
               var36.put("value", var29.isEmpty() ? null : b(var38, var29.size()));
               var36.put("divergent", var27);
               var33.put("measuredProportion", var36);
               var33.put("claimCoverage", a(var22.size(), var9.size(), var30));
               var33.put("notMeasured", var14);
               a(var33, var2, var3);
               if (var4 != io.karatelabs.ext.pair.ContractSchema.EMPTY && var4.operationCount() != var9.size()) {
                  int var39 = var4.operationCount();
                  LinkedHashMap<java.lang.String, java.lang.Object> var41 = new LinkedHashMap();
                  var41.put("specDeclares", var39);
                  var41.put("graphDeclares", var9.size());
                  var41.put(
                     "note",
                     "the numbers above are the COVERAGE GRAPH's — it declares "
                        + var9.size()
                        + " operation(s) — while the `spec` this pair is bound to declares "
                        + var39
                        + ". Those should agree. If they do not, check that the `spec` you passed is the same document your project binds as its coverage source: the pair's freshness watches the spec you named, so a mismatched one means edits to the document these figures actually came from will never decay this evidence."
                  );
                  var33.put("specMismatch", var41);
               }

               LinkedHashMap<String, Object> var40 = new LinkedHashMap<>();
               var40.put("aggregate", io.karatelabs.ext.pair.PairReportAssembler.summarize(var31, var9.size(), var30, var4 != io.karatelabs.ext.pair.ContractSchema.EMPTY));
               var40.put("items", var31.stream().map(io.karatelabs.ext.pair.PairReportAssembler.ScenarioAssessment::a).toList());
               var33.put("rungs", var40);
               var33.put(
                  "note",
                  "These are two numbers, and they apply only together. "
                     + var38
                     + " of "
                     + var29.size()
                     + " admissible operations were divergence-free. This pair exercised "
                     + var22.size()
                     + " of "
                     + var9.size()
                     + " declared operations"
                     + (var2.isEmpty() ? "" : " (" + var2.size() + " scenario" + (var2.size() == 1 ? "" : "s") + " excluded by tag)")
                     + ". Do not quote the proportion without the coverage. A `notexercised` operation has the status `not measured`. It never has the status `substitutable`."
               );
               return var33;
            }
         }
      }
   }

   private static Map<String, Object> a(Map<String, Object> var0, List<String> var1, boolean var2) {
      if (!var2) {
         var0.put(
            "excludedNote",
            "the destructive-scenario inventory failed, so scenarios may have been held out of both legs without this pair being able to name them. Treat the coverage below as an upper bound: it cannot account for what it could not enumerate."
         );
      } else if (!var1.isEmpty()) {
         var0.put("excluded", var1);
      }

      return var0;
   }

   private static Map<String, Object> a(int var0, int var1, List<String> var2) {
      LinkedHashMap<java.lang.String, java.lang.Object> var3 = new LinkedHashMap();
      var3.put("exercised", var0);
      var3.put("declared", var1);
      var3.put("value", b(var0, var1));
      var3.put("notexercised", var2);
      return var3;
   }

   private static Map<String, Object> a(String var0) {
      LinkedHashMap<java.lang.String, java.lang.Object> var1 = new LinkedHashMap();
      var1.put("measured", false);
      var1.put("unit", "operation");
      var1.put("note", var0);
      return var1;
   }

   private static boolean b(String var0) {
      return var0.startsWith("openapi:") || var0.startsWith("asyncapi:") || var0.startsWith("grpc:");
   }

   private static Map<String, Object> loadCoverageData(Path var0) {
      Path var1 = var0.resolve("ext/coverage/coverage-data.json");
      if (!Files.isRegularFile(var1)) {
         return null;
      } else {
         try {
            return Json.of(Files.readString(var1)).asMap();
         } catch (Exception var3) {
            return null;
         }
      }
   }

   static double a(double var0, double var2) {
      return b(var0, var2);
   }

   private static double b(double var0, double var2) {
      if (var2 <= 0.0) {
         return 0.0;
      } else {
         return var0 >= var2 ? 1.0 : Math.min(0.999, Math.floor(var0 / var2 * 1000.0) / 1000.0);
      }
   }
}
