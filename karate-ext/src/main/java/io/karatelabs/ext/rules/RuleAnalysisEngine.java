package io.karatelabs.ext.rules;

import io.karatelabs.common.Json;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.Map.Entry;

final class RuleAnalysisEngine {
   private static final Set<String> a = Set.of("TIMEOUT", "INTERNAL");
   private static final List<String> b = List.of(
      "total",
      "premium",
      "grandtotal",
      "amountdue",
      "balance",
      "payable",
      "amount",
      "responsibility",
      "memberresponsibility",
      "cost",
      "price",
      "charge",
      "rate"
   );

   private RuleAnalysisEngine() {
   }

   static Map<String, Object> a(io.karatelabs.ext.rules.RulebookRepository.RulebookDocument var0, String var1) {
      var var2 = b(var0, var1).a();
      LinkedHashMap<java.lang.String, java.lang.Object> var3 = new LinkedHashMap();
      var3.put("rule", var0.d());
      Map<java.lang.String, Object> var4 = (Map)var2.get("ranges");
      var3.put("dimensions", var4 == null ? List.of() : var4.get("dimensions"));
      var3.put("notderivable", var2.getOrDefault("notderivable", List.of()));
      if (var2.containsKey("outcomes")) {
         var3.put("outcomes", var2.get("outcomes"));
      }

      return var3;
   }

   static Map<String, Object> a(io.karatelabs.ext.rules.RulebookRepository.RulebookDocument var0) {
      return a(var0, (Long)null);
   }

   static Map<String, Object> a(io.karatelabs.ext.rules.RulebookRepository.RulebookDocument var0, Long var1) {
      return a(var0, var1, null);
   }

   static Map<String, Object> a(io.karatelabs.ext.rules.RulebookRepository.RulebookDocument var0, Long var1, Integer var2) {
      return a(var0, var1, var2, null);
   }

   static Map<String, Object> a(io.karatelabs.ext.rules.RulebookRepository.RulebookDocument var0, Long var1, Integer var2, io.karatelabs.ext.rules.RuleInputSpaceCertificateBuilder.InputSpaceCertificateResult var3) {
      var var4 = a(var0, "pairwise", var1 != null ? var1 : 50000L, var2, var3).a();
      LinkedHashMap<java.lang.String, java.lang.Object> var5 = new LinkedHashMap();
      var5.put("rule", var0.d());
      Map var6 = (Map)var4.getOrDefault("labels", Map.of());
      Map var7 = (Map)var4.get("coverage");
      Map<String, List<String>> var8 = (Map)a(var7);
      Map<String, List<String>> var9 = (Map)var4.getOrDefault("seedCoverage", Map.of());
      List var10 = (List)var4.get("sites");
      ArrayList<Map<String, Object>> var11 = new ArrayList<>();
      ArrayList<Map<String, Object>> var12 = new ArrayList<>();

      for (Map var14 : (Iterable<Map>) (Iterable<?>) (var10)) {
         String var15 = (String)var14.get("id");
         HashSet var16 = new HashSet(var8.getOrDefault(var15, List.of()));
         HashSet var17 = new HashSet(var9.getOrDefault(var15, List.of()));

         for (String var21 : new String[]{"true", "false"}) {
            if (!var16.contains(var21)) {
               var12.add(a(var14, var21, var6));
            } else if (!var17.contains(var21)) {
               var11.add(a(var14, var21, var6));
            }
         }
      }

      a(var11, var4);
      a(var12, var4);
      var5.put("notused", var11);
      var5.put("notreachable", var12);
      var5.put("gaps", var12);
      io.karatelabs.ext.rules.RequirementClaimAnalyzer var24 = io.karatelabs.ext.rules.RequirementClaimAnalyzer.a(var0.g(), var0.m());
      var var25 = b(var4);
      ArrayList<java.util.LinkedHashMap> var26 = new ArrayList();

      for (Map var29 : (Iterable<Map>) (Iterable<?>) (var10)) {
         String var32 = (String)var29.get("id");

         for (boolean var22 : new boolean[]{true, false}) {
            if ("notclaimed".equals(var24.a(var32, var22, var25.contains(var32 + ":" + var22)))) {
               LinkedHashMap<java.lang.String, java.lang.Object> var23 = new LinkedHashMap();
               var23.put("label", a(var6, var29.get("line")));
               var23.put("source", var29.get("source"));
               var23.put("line", var29.get("line"));
               var23.put("outcome", var22);
               var26.add(var23);
            }
         }
      }

      var5.put("notclaimed", var26);
      LinkedHashMap<java.lang.Object, java.util.Map> var28 = new LinkedHashMap();

      for (Map var33 : (Iterable<Map>) (Iterable<?>) ((List)var4.get("sites"))) {
         var28.put((String)var33.get("id"), var33);
      }

      ArrayList<java.util.LinkedHashMap> var31 = new ArrayList();

      for (Entry var37 : (Iterable<Entry>) (Iterable<?>) (var8.entrySet())) {
         List var40 = (List)var37.getValue();
         if (var40.size() == 1) {
            Map<java.lang.String, Object> var47 = (Map)var28.get(var37.getKey());
            LinkedHashMap<java.lang.String, java.lang.Object> var51 = new LinkedHashMap();
            var51.put("siteId", var37.getKey());
            var51.put("line", var47 == null ? null : var47.get("line"));
            var51.put("arm", var40.get(0));
            var51.put("source", var47 == null ? null : var47.get("source"));
            var51.put("label", a(var6, var47 == null ? null : var47.get("line")));
            var31.add(var51);
         }
      }

      var5.put("deadBranches", var31);
      Object var38 = var4.get("outcomes") instanceof Map var41 ? var41.get("neverProduced") : null;
      var5.put("notproduced", var38 != null ? var38 : List.of());
      var5.put("universe", var4.get("universe"));
      if (var4.get("signatures") != null) {
         var5.put("signatures", var4.get("signatures"));
      }

      if (var4.get("search") != null) {
         var5.put("search", var4.get("search"));
      }

      var5.put("properties", var4.getOrDefault("properties", List.of()));
      if (var4.get("outOfDomain") instanceof List var42 && !var42.isEmpty()) {
         var5.put("outOfDomain", var42);
      }

      if (var4.get("propertyErrors") instanceof List var43 && !var43.isEmpty()) {
         var5.put("propertyErrors", var43);
      }

      var5.put("notderivable", var4.getOrDefault("notderivable", List.of()));
      var5.put("rejects", Schema.rejectVerdicts(var0));
      var5.put("accepts", Schema.acceptVerdict(var0));
      var5.put("expectations", b(var0));
      var5.put("coverage", var7);
      if (var4.get("robustness") instanceof Map var44) {
         var5.put("robustness", var44);
      }

      LinkedHashMap<java.lang.String, java.lang.Object> var45 = new LinkedHashMap();
      var45.put("rule", var5.get("rule"));
      var45.put("verdict", a(var5, var24.a()));
      var45.putAll(var5);
      return var45;
   }

   static Map<String, Object> a(Map<String, Object> var0, boolean var1) {
      ArrayList<java.lang.String> var2 = new ArrayList();
      ArrayList<String> var3 = new ArrayList<>();
      int var4 = b(var0.get("propertyErrors")).size();
      if (var4 > 0) {
         var2.add(var4 + " authoring defect(s) — see propertyErrors");
      }

      for (Object var6 : b(var0.get("properties"))) {
         if (var6 instanceof Map var7) {
            String var8 = String.valueOf(var7.get("name"));
            if ("sometimes".equals(String.valueOf(var7.get("kind")))) {
               if (!Boolean.TRUE.equals(var7.get("holds"))) {
                  var2.add("the situation '" + var8 + "' is never reached (calc.sometimes)");
               }
            } else {
               String var9 = String.valueOf(var7.get("status"));
               if (!"HOLDS".equals(var9) && !"COMPLETE".equals(var9)) {
                  var2.add("the guarantee '" + var8 + "' reads " + var9 + (var7.get("counterexample") != null ? " — " + a(var7.get("counterexample")) : ""));
               }
            }
         }
      }

      for (Object var16 : b(var0.get("rejects"))) {
         if (var16 instanceof Map var21 && !Boolean.TRUE.equals(var21.get("pass"))) {
            var2.add("the negative scenario '" + var21.get("_id") + "' reads " + var21.get("status"));
         }
      }

      if (var0.get("accepts") instanceof Map var13) {
         String var18 = String.valueOf(var13.get("status"));
         if ("REFUSED".equals(var18)) {
            var2.add(
               b(var13.get("refused")).size()
                  + " saved scenario(s) their own schema.js refuses — see accepts.refused (either those rows or the shape is wrong)"
            );
         } else if ("NOTCHECKED".equals(var18) && !b(var0.get("rejects")).isEmpty()) {
            var2.add(
               "the negative scenarios prove nothing: no saved scenario shows schema.js ACCEPTS valid input, so a shape that refused everything would read the same"
            );
         }
      }

      if (var0.get("expectations") instanceof Map var14 && "MISMATCHED".equals(String.valueOf(var14.get("status")))) {
         var2.add(
            b(var14.get("failed")).size()
               + " saved scenario(s) disagree with the figure stored on them — see expectations.failed (either the rules or the expectation is wrong)"
         );
      }

      int var15 = b(var0.get("notproduced")).size();
      if (var15 > 0) {
         var2.add(var15 + " declared outcome(s) no input produces — see notproduced");
      }

      int var20 = b(var0.get("notreachable")).size();
      if (var20 > 0) {
         var3.add(var20 + " unreached decision arm(s) — see notreachable (a `|| default` fallback is benign; a business rule is not)");
      }

      int var22 = b(var0.get("deadBranches")).size();
      if (var22 > 0) {
         var3.add(var22 + " site(s) stuck on one arm — see deadBranches");
      }

      int var23 = b(var0.get("notused")).size();
      if (var23 > 0) {
         var3.add(var23 + " reachable arm(s) no saved scenario exercises — see notused (add case data)");
      }

      int var24 = b(var0.get("notclaimed")).size();
      if (var24 > 0) {
         var3.add(
            var1
               ? var24 + " decision arm(s) no requirement claims — see notclaimed"
               : "no calc.req in this rulebook — " + var24 + " decision arm(s) notclaimed"
         );
      }

      var var10 = new ArrayList<>(var2);
      var10.addAll(var3);
      LinkedHashMap<java.lang.String, java.lang.Object> var11 = new LinkedHashMap();
      var11.put("status", !var2.isEmpty() ? "FINDINGS" : (var3.isEmpty() ? "CLEAN" : "REVIEW"));
      var11.put("findings", var2.size());
      var11.put("review", var3.size());
      var11.put("reasons", var10);
      return var11;
   }

   private static List<?> b(Object var0) {
      return var0 instanceof List var1 ? var1 : List.of();
   }

   static Map<String, Object> b(io.karatelabs.ext.rules.RulebookRepository.RulebookDocument var0) {
      ArrayList<java.util.Map> var1 = new ArrayList();

      for (Map var3 : var0.b()) {
         if (io.karatelabs.ext.rules.ExpectedResultMatcher.a(var3) != null) {
            var1.add(var3);
         }
      }

      LinkedHashMap<java.lang.String, java.lang.Object> var15 = new LinkedHashMap();
      ArrayList<java.util.LinkedHashMap> var16 = new ArrayList();
      ArrayList<java.util.LinkedHashMap> var4 = new ArrayList();
      int var5 = 0;
      if (!var1.isEmpty()) {
         try (RuleEvaluator var6 = new RuleEvaluator()) {
            for (Map var8 : (Iterable<Map>) (Iterable<?>) (var1)) {
               String var9 = null;

               String var10;
               try {
                  io.karatelabs.ext.rules.RuleEvaluator.RuleEvaluationResult var11 = var6.a(var0.g(), var0.m().e(), RulebookRepository.d(var8), null);
                  var10 = io.karatelabs.ext.rules.ExpectedResultMatcher.a(io.karatelabs.ext.rules.ExpectedResultMatcher.a(var8), false, var11.b(), var11.f(), var11.g());
               } catch (RuleApiException var13) {
                  if (a.contains(var13.b)) {
                     var9 = var13.b + ": " + var13.getMessage();
                     var10 = null;
                  } else {
                     var10 = io.karatelabs.ext.rules.ExpectedResultMatcher.a(io.karatelabs.ext.rules.ExpectedResultMatcher.a(var8), true, null, null, false);
                  }
               }

               LinkedHashMap<java.lang.String, java.lang.Object> var17 = new LinkedHashMap();
               var17.put("_id", var8.get("_id"));
               if (var8.get("_label") != null) {
                  var17.put("_label", var8.get("_label"));
               }

               if (var9 != null) {
                  var17.put("why", var9);
                  var4.add(var17);
               } else {
                  var5++;
                  if (var10 != null) {
                     var17.put("why", var10);
                     var16.add(var17);
                  }
               }
            }
         }
      }

      var15.put("status", !var16.isEmpty() ? "MISMATCHED" : (var5 == 0 ? "NOTCHECKED" : "MATCHED"));
      var15.put("checked", var5);
      var15.put("failed", var16);
      if (!var4.isEmpty()) {
         var15.put("notChecked", var4);
      }

      return var15;
   }

   static boolean a(String var0) {
      return a.contains(var0);
   }

   static Map<String, Object> c(io.karatelabs.ext.rules.RulebookRepository.RulebookDocument var0) {
      var var1 = b(var0, "pairwise").a();
      List<Map<String, Object>> var2 = (List)var1.getOrDefault("sites", List.of());
      Map var3 = (Map)var1.getOrDefault("coverage", Map.of());
      Map<String, List<String>> var4 = (Map)a(var3);
      Map var5 = (Map)var1.getOrDefault("labels", Map.of());
      var var6 = RuleInputSpaceExplorer.a(var0.g(), var0.m().e(), var0.e(), var0.j(), var0.b());
      var var7 = b(var1);
      io.karatelabs.ext.rules.RequirementClaimAnalyzer var8 = io.karatelabs.ext.rules.RequirementClaimAnalyzer.a(var0.g(), var0.m());
      ArrayList<java.util.LinkedHashMap> var9 = new ArrayList();
      int var10 = 0;
      int var11 = 0;
      int var12 = 0;
      int var13 = 0;

      for (Map var15 : (Iterable<Map>) (Iterable<?>) (var2)) {
         String var16 = (String)var15.get("id");
         HashSet var17 = new HashSet(var4.getOrDefault(var16, List.of()));

         for (String var21 : new String[]{"true", "false"}) {
            var12++;
            int var22 = var6.getOrDefault(var16 + ":" + var21, 0);
            String var23;
            if (var22 > 0) {
               var23 = "used";
               var10++;
               var11++;
            } else if (var17.contains(var21)) {
               var23 = "notused";
               var11++;
            } else {
               var23 = "notreached";
            }

            LinkedHashMap<java.lang.String, java.lang.Object> var24 = new LinkedHashMap();
            var24.put("label", a(var5, var15.get("line")));
            var24.put("source", var15.get("source"));
            var24.put("line", var15.get("line"));
            var24.put("outcome", Boolean.parseBoolean(var21));
            var24.put("status", var23);
            var24.put("scenarios", var22);
            boolean var25 = Boolean.parseBoolean(var21);
            String var26 = var8.a(var16, var25, var7.contains(var16 + ":" + var21));
            var24.put("claim", var26);
            var24.put("reqs", var8.a(var16, var25));
            if ("notclaimed".equals(var26)) {
               var13++;
            }

            if (var7.contains(var16 + ":" + var21)) {
               var24.put("robustness", true);
            }

            var9.add(var24);
         }
      }

      LinkedHashMap<java.lang.String, java.lang.Object> var27 = new LinkedHashMap();
      var27.put("used", var10);
      var27.put("reachable", var11);
      var27.put("total", var12);
      var27.put("notclaimed", var13);
      var27.put("arms", var9);
      return var27;
   }

   private static Map<String, Object> a(Map<String, Object> var0) {
      Object var1 = var0.get("domainPerSite");
      return (Map<String, Object>)(var1 instanceof Map ? var1 : var0.getOrDefault("perSite", Map.of()));
   }

   private static Set<String> b(Map<String, Object> var0) {
      if (var0.get("robustness") instanceof Map var1 && var1.get("coverageOnly") instanceof List var2) {
         HashSet<java.lang.String> var7 = new HashSet();

         for (Map var5 : (Iterable<Map>) (Iterable<?>) (var2)) {
            var7.add(var5.get("site") + ":" + var5.get("outcome"));
         }

         return var7;
      } else {
         return Set.of();
      }
   }

   private static void a(List<Map<String, Object>> var0, Map<String, Object> var1) {
      if (!var0.isEmpty()) {
         LinkedHashMap<java.lang.String, java.lang.String> var2 = new LinkedHashMap();

         for (Map var4 : c(var1)) {
            Object var5 = var4.get("path");

            for (Object var7 : (List<?>)var4.getOrDefault("gates", List.of())) {
               var2.put(String.valueOf(var7), String.valueOf(var5));
            }
         }

         LinkedHashMap<java.lang.String, java.lang.String> var10 = new LinkedHashMap();

         for (Map var13 : (Iterable<Map>) (Iterable<?>) (var1.getOrDefault("notderivable", List.of()))) {
            var10.putIfAbsent(String.valueOf(var13.get("site")), String.valueOf(var13.get("reason")));
         }

         for (Map var14 : var0) {
            String var15 = String.valueOf(var14.get("site"));
            String var16 = (String)var2.get(var15);
            if (var16 == null) {
               int var8 = var15.indexOf(35);
               var16 = var8 < 0 ? null : (String)var2.get(var15.substring(0, var8));
            }

            if (var16 != null) {
               var14.put("axis", var16);
            } else {
               String var17 = var15.indexOf(35) < 0 ? var15 : var15.substring(0, var15.indexOf(35));
               String var9 = (String)var10.get(var17);
               if (var9 != null) {
                  var14.put("axis", null);
                  var14.put("attribution", "notderivable");
                  var14.put("reason", var9);
               }
            }
         }
      }
   }

   private static Map<String, Object> a(Map<String, Object> var0, String var1, Map<String, Object> var2) {
      LinkedHashMap<java.lang.String, java.lang.Object> var3 = new LinkedHashMap();
      var3.put("site", var0.get("id"));
      var3.put("line", var0.get("line"));
      var3.put("outcome", Boolean.parseBoolean(var1));
      var3.put("source", var0.get("source"));
      var3.put("label", a(var2, var0.get("line")));
      return var3;
   }

   private static String a(Map<String, Object> var0, Object var1) {
      if (var0 != null && var1 != null) {
         int var2 = ((Number)var1).intValue();
         String var3 = null;
         int var4 = -1;

         for (Entry var6 : var0.entrySet()) {
            int var7;
            try {
               var7 = Integer.parseInt((String)var6.getKey());
            } catch (NumberFormatException var9) {
               continue;
            }

            if (var7 <= var2 && var7 >= var2 - 2 && var7 > var4) {
               var4 = var7;
               var3 = String.valueOf(var6.getValue());
            }
         }

         return var3;
      } else {
         return null;
      }
   }

   static Map<String, Object> a(io.karatelabs.ext.rules.RulebookRepository.RulebookDocument var0, RuleEvaluator var1, Map<String, Object> var2) {
      io.karatelabs.ext.rules.RuleInputSpaceExplorer.InputSpaceExplorationResult var3 = b(var0, "coverage");
      var var4 = c(var3.a());
      var var5 = a(var0, var2);
      String var6 = var2 == null ? null : g(var2.get("output"));
      LinkedHashMap<java.lang.String, java.lang.Object> var7 = new LinkedHashMap();
      var7.put("rule", var0.d());
      io.karatelabs.ext.rules.RuleAnalysisEngine.RuleEvaluationSnapshot var8 = a(var1, var0.g(), var0.m().e(), var5);
      io.karatelabs.ext.rules.RuleAnalysisEngine.OutputSelection var9 = a(var8.a, var6);
      var7.put("output", var9.a);
      var7.put("base", var9.b);
      String var10 = var2 == null ? null : g(var2.get("dim"));
      if (var10 != null) {
         var var23 = var4.stream().filter(var1x -> var10.equals(var1x.get("path"))).findFirst().orElse(null);
         if (var23 == null) {
            return c("unknown dimension '" + var10 + "' (try Rule.ranges)");
         } else {
            var7.put("dim", var10);
            var7.put("points", a(var1, var0.g(), var0.m().e(), var5, var23, var9.a));
            return var7;
         }
      } else {
         ArrayList<java.util.LinkedHashMap> var11 = new ArrayList();

         for (Map var13 : (Iterable<Map>) (Iterable<?>) (var4)) {
            List var14 = a(var1, var0.g(), var0.m().e(), var5, var13, var9.a);
            Double var15 = null;
            Double var16 = null;

            for (Map var18 : (Iterable<Map>) (Iterable<?>) (var14)) {
               if (var18.get("output") instanceof Number var20) {
                  double var21 = var20.doubleValue();
                  var15 = var15 == null ? var21 : Math.min(var15, var21);
                  var16 = var16 == null ? var21 : Math.max(var16, var21);
               }
            }

            if (var15 != null) {
               LinkedHashMap<java.lang.String, java.lang.Object> var24 = new LinkedHashMap();
               var24.put("dim", var13.get("path"));
               var24.put("low", b(var15.doubleValue()));
               var24.put("high", b(var16.doubleValue()));
               var24.put("spread", b(var16 - var15));
               var11.add(var24);
            }
         }

         var11.sort((var0x, var1x) -> Double.compare(f(var1x.get("spread")), f(var0x.get("spread"))));
         var7.put("tornado", var11);
         return var7;
      }
   }

   private static List<Map<String, Object>> a(
      RuleEvaluator var0, String var1, Map<String, Object> var2, Map<String, Object> var3, Map<String, Object> var4, String var5
   ) {
      String var6 = (String)var4.get("path");
      List var7 = (List)var4.get("classes");
      ArrayList<java.util.Map<java.lang.String,java.lang.Object>> var8 = new ArrayList();
      Object var9 = null;
      boolean var10 = true;

      for (Map var12 : (Iterable<Map>) (Iterable<?>) (var7)) {
         var var13 = d(var3);
         RuleInputSpaceExplorer.a(var13, var6, var12.get("sample"));
         io.karatelabs.ext.rules.RuleAnalysisEngine.RuleEvaluationSnapshot var14 = a(var0, var1, var2, var13);
         LinkedHashMap<java.lang.String, java.lang.Object> var15 = new LinkedHashMap();
         var15.put("class", var12.get("label"));
         var15.put("value", var12.get("sample"));
         var15.put("output", a(var14.a, var5).b);
         if (var14.c) {
            var15.put("outcome", var14.b);
         }

         boolean var16 = !var10 && var14.c && !Objects.equals(var9, var14.b);
         if (var16) {
            var15.put("flip", true);
         }

         var9 = var14.b;
         var10 = false;
         var8.add(var15);
      }

      return var8;
   }

   static Map<String, Object> b(io.karatelabs.ext.rules.RulebookRepository.RulebookDocument var0, RuleEvaluator var1, Map<String, Object> var2) {
      if (var2 != null && var2.get("path") != null) {
         var var3 = var0.h();
         if (var3 != null && !var3.isEmpty()) {
            var var4 = c(var2.get("path"));
            RuleTableCatalog var5 = var0.m();
            String var6 = String.valueOf(var2.get("path"));
            io.karatelabs.ext.rules.RuleTableCatalog.TableCellReference var7 = var5.a() ? null : var5.d(var2.get("path") instanceof String && var6.contains("/") ? var6 : String.join(".", var4));
            Object var8 = var7 != null ? var7.h() : a((Object)var3, var4);
            if (!(var8 instanceof Number var9)) {
               return c(a(var3, var4, var8));
            } else {
               double var10 = a(var9.doubleValue(), var2);
               String var12 = var0.g();
               RuleTableCatalog var13 = var5;
               if (var7 != null) {
                  try {
                     var13 = var5.c(var7, var10);
                  } catch (RuntimeException var32) {
                     return c("could not set the table cell: " + var32.getMessage());
                  }
               } else {
                  if (var5.c(String.join(".", var4)) != null) {
                     return c(
                        "lookup path '"
                           + String.join(".", var4)
                           + "' names no cell of a file table — address one as <table>.<row key>[.<column>] or <table>@<J>/<row key>[/<column>]"
                     );
                  }

                  LinkedHashMap<java.lang.String, java.lang.Object> var14 = new LinkedHashMap();

                  for (Entry var16 : (Iterable<Entry>) (Iterable<?>) (d(var3).entrySet())) {
                     if (var5.c((String)var16.getKey()) == null && !var5.d().containsKey(var16.getKey())) {
                        var14.put((String)var16.getKey(), var16.getValue());
                     }
                  }

                  b(var14, var4, b(var10));

                  try {
                     var12 = new LookupTableEditor(var0.g()).a(var14);
                  } catch (RuntimeException var31) {
                     return c("could not splice the lookup: " + var31.getMessage());
                  }
               }

               String var33 = g(var2.get("output"));
               ArrayList<Map<String, Object>> var34 = new ArrayList<>();
               double var35 = 0.0;
               double var18 = 0.0;

               for (Map var21 : (Iterable<Map>) (Iterable<?>) (var0.b() == null ? List.of() : var0.b())) {
                  io.karatelabs.ext.rules.RuleAnalysisEngine.RuleEvaluationSnapshot var22 = a(var1, var0.g(), var5.e(), var21);
                  io.karatelabs.ext.rules.RuleAnalysisEngine.RuleEvaluationSnapshot var23 = a(var1, var12, var13.e(), var21);
                  io.karatelabs.ext.rules.RuleAnalysisEngine.OutputSelection var24 = a(var22.a, var33);
                  io.karatelabs.ext.rules.RuleAnalysisEngine.OutputSelection var25 = a(var23.a, var33);
                  if (var24.b instanceof Number && var25.b instanceof Number) {
                     double var26 = ((Number)var24.b).doubleValue();
                     double var28 = ((Number)var25.b).doubleValue();
                     var35 += var26;
                     var18 += var28;
                     LinkedHashMap<java.lang.String, java.lang.Object> var30 = new LinkedHashMap();
                     var30.put("scenario", var21.getOrDefault("_label", var21.getOrDefault("_id", "scenario")));
                     var30.put("before", b(a(var26)));
                     var30.put("after", b(a(var28)));
                     var30.put("delta", b(a(var28 - var26)));
                     var30.put("pct", var26 == 0.0 ? null : b(a((var28 - var26) / var26 * 100.0)));
                     var34.add(var30);
                  }
               }

               LinkedHashMap<java.lang.String, java.lang.Object> var36 = new LinkedHashMap();
               var36.put("rule", var0.d());
               LinkedHashMap<java.lang.String, java.lang.Object> var37 = new LinkedHashMap();
               var37.put("path", var7 != null ? var7.a() : String.join(".", var4));
               if (var7 != null) {
                  var37.put("location", var7.b());
               }

               var37.put("before", b(var9.doubleValue()));
               var37.put("after", b(var10));
               var36.put("change", var37);
               var36.put("output", a(var33, var34));
               var36.put("scenarios", var34);
               LinkedHashMap<java.lang.String, java.lang.Object> var38 = new LinkedHashMap();
               var38.put("before", b(a(var35)));
               var38.put("after", b(a(var18)));
               var38.put("delta", b(a(var18 - var35)));
               var38.put("pct", var35 == 0.0 ? null : b(a((var18 - var35) / var35 * 100.0)));
               var36.put("total", var38);
               return var36;
            }
         } else {
            return c("rulebook '" + var0.d() + "' has no editable lookup table to perturb");
         }
      } else {
         return c("Rule.impact requires {path:'a.b.c', set|mul|add: <n>}");
      }
   }

   private static io.karatelabs.ext.rules.RuleAnalysisEngine.RuleEvaluationSnapshot a(RuleEvaluator var0, String var1, Map<String, Object> var2, Object var3) {
      try {
         io.karatelabs.ext.rules.RuleEvaluator.RuleEvaluationResult var4 = var0.a(var1, var2, var3, null);
         return new io.karatelabs.ext.rules.RuleAnalysisEngine.RuleEvaluationSnapshot(var4.b(), var4.f(), var4.g(), null);
      } catch (RuntimeException var5) {
         return new io.karatelabs.ext.rules.RuleAnalysisEngine.RuleEvaluationSnapshot(null, null, false, var5.getMessage());
      }
   }

   private static io.karatelabs.ext.rules.RuleInputSpaceExplorer.InputSpaceExplorationResult b(io.karatelabs.ext.rules.RulebookRepository.RulebookDocument var0, String var1) {
      return a(var0, var1, null);
   }

   private static io.karatelabs.ext.rules.RuleInputSpaceExplorer.InputSpaceExplorationResult a(io.karatelabs.ext.rules.RulebookRepository.RulebookDocument var0, String var1, Long var2) {
      return a(var0, var1, var2, null);
   }

   private static io.karatelabs.ext.rules.RuleInputSpaceExplorer.InputSpaceExplorationResult a(io.karatelabs.ext.rules.RulebookRepository.RulebookDocument var0, String var1, Long var2, Integer var3) {
      return a(var0, var1, var2, var3, null);
   }

   private static io.karatelabs.ext.rules.RuleInputSpaceExplorer.InputSpaceExplorationResult a(io.karatelabs.ext.rules.RulebookRepository.RulebookDocument var0, String var1, Long var2, Integer var3, io.karatelabs.ext.rules.RuleInputSpaceCertificateBuilder.InputSpaceCertificateResult var4) {
      List var5 = var0.b() == null ? List.of() : var0.b();
      return RuleInputSpaceExplorer.a(var0.g(), var0.m().e(), var0.e(), var0.j(), var5, var1, var2, var3 == null ? 512 : var3, 2, var4);
   }

   private static List<Map<String, Object>> c(Map<String, Object> var0) {
      Map<java.lang.String, java.util.List> var1 = (Map)var0.get("ranges");
      return var1 == null ? List.of() : (List)var1.get("dimensions");
   }

   private static Map<String, Object> a(io.karatelabs.ext.rules.RulebookRepository.RulebookDocument var0, Map<String, Object> var1) {
      Object var2 = var1 == null ? null : var1.get("base");
      if (var2 instanceof String var5) {
         var var4 = RulebookRepository.a(var0, var5);
         if (RulebookRepository.a(var4)) {
            throw new RuleApiException(
               400,
               "SCENARIO_IS_REJECT",
               "scenario '" + var5 + "' is a reject row (_expect: 'schema-reject') — it is deliberately invalid input, so it cannot be a what-if base row"
            );
         } else {
            return var4;
         }
      } else if (var2 instanceof Map var3) {
         return var3;
      } else {
         return var0.b() != null && !var0.b().isEmpty() ? var0.b().get(0) : RuleInputSpaceExplorer.a(var0.e(), var0.j());
      }
   }

   private static io.karatelabs.ext.rules.RuleAnalysisEngine.OutputSelection a(Object var0, String var1) {
      if (var1 != null) {
         Object var7 = var0 instanceof Map ? RuleCoverageMatrixBuilder.a(e(var0), var1) : null;
         return new io.karatelabs.ext.rules.RuleAnalysisEngine.OutputSelection(var1, var7);
      } else {
         if (var0 instanceof Map var2) {
            io.karatelabs.ext.rules.RuleAnalysisEngine.OutputSelection var3 = null;

            for (Entry var5 : (Iterable<Entry>) (Iterable<?>) (var2.entrySet())) {
               if (var5.getValue() instanceof Number) {
                  String var6 = String.valueOf(var5.getKey());
                  if (b.contains(var6.toLowerCase())) {
                     return new io.karatelabs.ext.rules.RuleAnalysisEngine.OutputSelection(var6, var5.getValue());
                  }

                  if (var3 == null) {
                     var3 = new io.karatelabs.ext.rules.RuleAnalysisEngine.OutputSelection(var6, var5.getValue());
                  }
               }
            }

            if (var3 != null) {
               return var3;
            }
         }

         return new io.karatelabs.ext.rules.RuleAnalysisEngine.OutputSelection("output", var0 instanceof Number ? var0 : null);
      }
   }

   private static String a(String var0, List<Map<String, Object>> var1) {
      return var0 != null ? var0 : "output";
   }

   private static List<String> c(Object var0) {
      if (!(var0 instanceof List var1)) {
         String var7 = String.valueOf(var0).replaceAll("\\[\\s*'?\"?([^\\[\\]'\"]+)'?\"?\\s*]", ".$1");
         ArrayList<java.lang.String> var8 = new ArrayList();

         for (String var6 : var7.split("\\.")) {
            if (!var6.isBlank()) {
               var8.add(var6);
            }
         }

         return var8;
      } else {
         ArrayList<java.lang.String> var2 = new ArrayList();

         for (Object var4 : var1) {
            var2.add(String.valueOf(var4));
         }

         return var2;
      }
   }

   private static String a(Map<String, Object> var0, List<String> var1, Object var2) {
      String var3 = String.join(".", var1);
      if (var2 != null) {
         var var11 = d(var2);
         return "lookup path '"
            + var3
            + "' holds "
            + a(var2)
            + ", not a number — impact perturbs ONE numeric cell"
            + (var11.isEmpty() ? "" : ", so go one step deeper: '" + var3 + "." + (String)var11.get(0) + "' (it has " + var11 + ")");
      } else {
         Object var4 = var0;
         ArrayList<java.lang.String> var5 = new ArrayList();

         for (String var7 : var1) {
            Object var8 = a(var4, List.of(var7));
            if (var8 == null) {
               String var9 = var5.isEmpty() ? "the lookup root" : "'" + String.join(".", var5) + "'";
               var var10 = d(var4);
               return "lookup path '"
                  + var3
                  + "' does not resolve: "
                  + var9
                  + " has no '"
                  + var7
                  + "'"
                  + (var10.isEmpty() ? "" : " (it has " + var10 + ")")
                  + " — steps are dot-separated and an array index is a step of its own ('bands.main.0.rate'; the bracket form 'bands.main[0].rate' means the same thing)";
            }

            var5.add(var7);
            var4 = var8;
         }

         return "lookup path '" + var3 + "' is not a number (got null)";
      }
   }

   private static List<String> d(Object var0) {
      ArrayList<java.lang.String> var1 = new ArrayList();
      if (var0 instanceof Map var2) {
         for (Object var5 : var2.keySet()) {
            var1.add(String.valueOf(var5));
         }
      } else if (var0 instanceof List var3) {
         for (int var6 = 0; var6 < var3.size(); var6++) {
            var1.add(String.valueOf(var6));
         }
      }

      return var1.size() > 8 ? new ArrayList<>(var1.subList(0, 8)) : var1;
   }

   private static Object a(Object var0, List<String> var1) {
      Object var2 = var0;

      for (String var4 : var1) {
         if (var2 instanceof Map var6) {
            var2 = var6.get(var4);
         } else {
            if (!(var2 instanceof List var5)) {
               return null;
            }

            int var7 = b(var4);
            var2 = var7 >= 0 && var7 < var5.size() ? var5.get(var7) : null;
         }
      }

      return var2;
   }

   private static void b(Map<String, Object> var0, List<String> var1, Object var2) {
      Object var3 = var0;

      for (int var4 = 0; var4 < var1.size() - 1; var4++) {
         String var5 = (String)var1.get(var4);
         var3 = var3 instanceof Map ? ((Map)var3).get(var5) : (var3 instanceof List ? ((List)var3).get(b(var5)) : null);
      }

      String var6 = (String)var1.get(var1.size() - 1);
      if (var3 instanceof Map) {
         ((Map)var3).put(var6, var2);
      } else if (var3 instanceof List) {
         ((List)var3).set(b(var6), var2);
      }
   }

   private static int b(String var0) {
      try {
         return Integer.parseInt(var0);
      } catch (NumberFormatException var2) {
         return -1;
      }
   }

   private static double a(double var0, Map<String, Object> var2) {
      if (var2.get("set") instanceof Number var7) {
         return var7.doubleValue();
      } else if (var2.get("mul") instanceof Number var6) {
         return var0 * var6.doubleValue();
      } else if (var2.get("add") instanceof Number var5) {
         return var0 + var5.doubleValue();
      } else {
         return var2.get("pct") instanceof Number var3 ? var0 * (1.0 + var3.doubleValue() / 100.0) : var0;
      }
   }

   private static Map<String, Object> e(Object var0) {
      return (Map<String, Object>)var0;
   }

   private static Map<String, Object> d(Map<String, Object> var0) {
      return (Map<String, Object>)io.karatelabs.ext.common.SandboxExecutor.a(var0);
   }

   private static double f(Object var0) {
      return var0 instanceof Number var1 ? var1.doubleValue() : 0.0;
   }

   private static double a(double var0) {
      return Math.round(var0 * 100.0) / 100.0;
   }

   private static Object b(double var0) {
      if (var0 == Math.rint(var0) && Math.abs(var0) < 9.223372E18F) {
         return (long)var0 == (int)((long)var0) ? (int)var0 : (long)var0;
      } else {
         return var0;
      }
   }

   private static String g(Object var0) {
      return var0 == null ? null : String.valueOf(var0);
   }

   private static Map<String, Object> c(String var0) {
      LinkedHashMap<java.lang.String, java.lang.Object> var1 = new LinkedHashMap();
      var1.put("error", var0);
      var1.put("code", "BAD_REQUEST");
      var1.put("status", 400);
      return var1;
   }

   static String a(Object var0) {
      return Json.stringifyStrict(var0);
   }

   private record OutputSelection(String a, Object b) {

      private OutputSelection(String a, Object b) {
         this.a = a;
         this.b = b;
      }
   }

   private record RuleEvaluationSnapshot(Object a, Object b, boolean c, String d) {

      private RuleEvaluationSnapshot(Object a, Object b, boolean c, String d) {
         this.a = a;
         this.b = b;
         this.c = c;
         this.d = d;
      }
   }
}
