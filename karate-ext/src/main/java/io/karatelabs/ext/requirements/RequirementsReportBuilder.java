package io.karatelabs.ext.requirements;

import io.karatelabs.common.Json;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class RequirementsReportBuilder {
   private static final List<String> a = List.of("pass", "notchecked", "stale", "unbindable", "counterexample");

   public static Map<String, Object> a(Map<String, Object> var0, Path var1) {
      var var2 = b(var0);
      LinkedHashMap var3 = new LinkedHashMap();
      var3.put("graph", var0);
      var3.put("readiness", io.karatelabs.ext.requirements.ReadinessEvaluator.a(var2));
      var3.put("requirements", var2);
      var3.put("assertionStrength", d(var0));
      var var4 = a(var1);
      if (!var4.isEmpty()) {
         var3.put("links", var4);
      }

      return var3;
   }

   public static Map<String, Object> a(Path var0) {
      try {
         Path var1 = var0.resolve("ext").resolve("requirements").resolve("provider.json");
         if (Files.exists(var1) && Json.of(Files.readString(var1)).value() instanceof Map var3 && var3.get("links") instanceof Map var4) {
            return var4;
         }
      } catch (Exception var6) {
      }

      return Map.of();
   }

   public static Map<String, Object> a(Map<String, Object> var0) {
      var var1 = b(var0);
      return var1.isEmpty() ? null : io.karatelabs.ext.requirements.ReadinessEvaluator.a(var1);
   }

   private static Set<String> a(List<Map<String, Object>> var0) {
      HashSet var1 = new HashSet();

      for (Map var3 : var0) {
         if (var3.get("parent") != null) {
            var1.add(String.valueOf(var3.get("parent")));
         }
      }

      return var1;
   }

   public static List<Map<String, Object>> b(Map<String, Object> var0) {
      var var1 = e(var0);
      io.karatelabs.ext.requirements.CoverageAnalyzer.RequirementCoverageAnalysis var2 = io.karatelabs.ext.requirements.CoverageAnalyzer.a(var0);
      var var3 = a(var1);
      HashSet var4 = new HashSet();
      HashSet var5 = new HashSet();

      for (Map var7 : io.karatelabs.ext.common.jsapi.CoverageGraphAccessor.a(var0, "tests")) {
         String var8 = String.valueOf(var7.get("status"));
         if (!c(var7)) {
            (!"PASSED".equals(var8) && !"FAILED".equals(var8) ? var5 : var4).add(String.valueOf(var7.get("id")));
         }
      }

      var var15 = io.karatelabs.ext.common.jsapi.CoverageGraphAccessor.b(var0);
      ArrayList var16 = new ArrayList();

      for (Map var9 : (Iterable<Map>) (Iterable<?>) (var1)) {
         Map var10 = a(var9, var15, var0);
         a(var10, var2, String.valueOf(var9.get("id")));
         var10.put("exercisedLevel", a(var0, var9, var4, var5));
         List var11 = d(var0, var9);
         if (!var11.isEmpty()) {
            var10.put("claimedUnverified", var11);
         }

         List var12 = b(var0, var9, "VIOLATED");
         var12.addAll(b(var0, var9));
         if (!var12.isEmpty()) {
            var10.put("counterexamples", var12);
         }

         List var13 = c(var0, var9);
         if (!var13.isEmpty()) {
            var10.put("proofs", var13);
         }

         List var14 = b(var0, var9, "NOTREACHABLE");
         if (!var14.isEmpty()) {
            var10.put("notreachable", var14);
         }

         if (a(var0, var9)) {
            var10.put("domainRestricted", true);
         }

         if (!var3.contains(String.valueOf(var10.get("reqId")))) {
            var16.add(var10);
         }
      }

      return var16;
   }

   private static boolean a(Map<String, Object> var0, Map<String, Object> var1) {
      String var2 = var1.get("id") + "/";

      for (Map var4 : io.karatelabs.ext.common.jsapi.CoverageGraphAccessor.a(var0, "hits")) {
         String var5 = String.valueOf(var4.get("item"));
         if ((var5.equals(var1.get("id")) || var5.startsWith(var2))
            && var4.get("evidence") instanceof Map var6
            && Boolean.TRUE.equals(var6.get("domainRestricted"))) {
            return true;
         }
      }

      return false;
   }

   private static List<Map<String, Object>> b(Map<String, Object> var0, Map<String, Object> var1, String var2) {
      String var3 = String.valueOf(var1.get("id"));
      String var4 = var3 + "/";
      HashSet var5 = new HashSet();
      var5.add(var3);

      for (Map var7 : io.karatelabs.ext.common.jsapi.CoverageGraphAccessor.a(var0, "items")) {
         if (String.valueOf(var7.get("id")).startsWith(var4)) {
            var5.add(String.valueOf(var7.get("id")));
         }
      }

      ArrayList var11 = new ArrayList();

      for (Map var8 : io.karatelabs.ext.common.jsapi.CoverageGraphAccessor.a(var0, "hits")) {
         if (var5.contains(String.valueOf(var8.get("item"))) && var8.get("evidence") instanceof Map var9 && var2.equals(var9.get("propertyStatus"))) {
            LinkedHashMap var13 = new LinkedHashMap();
            var13.put("criterion", var8.get("item"));
            var13.put("property", var9.get("property"));
            var13.put("rule", var9.get("rule"));
            if (var9.get("counterexample") != null) {
               var13.put("counterexample", var9.get("counterexample"));
            }

            if (var9.get("claim") != null) {
               var13.put("claim", var9.get("claim"));
            }

            var11.add(var13);
         }
      }

      return var11;
   }

   private static List<Map<String, Object>> b(Map<String, Object> var0, Map<String, Object> var1) {
      var var2 = f(var0);
      ArrayList var3 = new ArrayList();

      for (Map var5 : io.karatelabs.ext.common.jsapi.CoverageGraphAccessor.a(var0, "hits")) {
         Map var6 = (Map)var2.get(String.valueOf(var5.get("test")));
         if (var6 != null && a(var1, var5.get("item")) && "counterexample".equals(g(var6))) {
            String var7 = String.valueOf(var6.get("id"));
            int var8 = var7.indexOf("::");
            LinkedHashMap var9 = new LinkedHashMap();
            var9.put("criterion", var5.get("item"));
            var9.put("property", var8 < 0 ? var7 : var7.substring(var8 + 2));
            var9.put("definition", var8 < 0 ? var7 : var7.substring(0, var8));
            var9.put("subject", var7);
            var3.add(var9);
         }
      }

      return var3;
   }

   private static List<Map<String, Object>> c(Map<String, Object> var0, Map<String, Object> var1) {
      ArrayList var2 = new ArrayList();
      Map var3 = null;
      String var4 = var1.get("id") + "/";

      for (Map var6 : io.karatelabs.ext.common.jsapi.CoverageGraphAccessor.a(var0, "items")) {
         if (String.valueOf(var6.get("id")).startsWith(var4) && var6.get("proofs") instanceof List var7) {
            if (var3 == null) {
               var3 = f(var0);
            }

            String var17 = String.valueOf(var6.get("id"));

            for (Object var10 : var7) {
               String var11 = String.valueOf(var10);
               String var12 = "missing";

               for (Map var14 : io.karatelabs.ext.common.jsapi.CoverageGraphAccessor.a(var0, "hits")) {
                  Map var15 = (Map)var3.get(String.valueOf(var14.get("test")));
                  if (var15 != null && var17.equals(var14.get("item")) && String.valueOf(var15.get("id")).startsWith(var11 + "::")) {
                     String var16 = g(var15);
                     var12 = !"missing".equals(var12) && a.indexOf(var16) <= a.indexOf(var12) ? var12 : var16;
                  }
               }

               LinkedHashMap var18 = new LinkedHashMap();
               var18.put("criterion", var17.startsWith("req:") ? var17.substring(4) : var17);
               var18.put("definition", var11);
               var18.put("proof", var12);
               var2.add(var18);
            }
         }
      }

      return var2;
   }

   private static Map<String, Map<String, Object>> f(Map<String, Object> var0) {
      LinkedHashMap var1 = new LinkedHashMap();

      for (Map var3 : io.karatelabs.ext.common.jsapi.CoverageGraphAccessor.a(var0, "tests")) {
         if (c(var3)) {
            var1.put(String.valueOf(var3.get("id")), var3);
         }
      }

      return var1;
   }

   public static boolean c(Map<String, Object> var0) {
      return var0.get("tags") instanceof List var1 && var1.contains("evidence=model");
   }

   private static String g(Map<String, Object> var0) {
      Object var2 = var0.get("tags");
      if (var2 instanceof List) {
         for (Object var3 : (List)var2) {
            if (String.valueOf(var3).startsWith("proof=")) {
               return String.valueOf(var3).substring("proof=".length());
            }
         }
      }

      return "notchecked";
   }

   private static boolean a(Map<String, Object> var0, Object var1) {
      String var2 = String.valueOf(var1);
      return var2.equals(var0.get("id")) || var2.startsWith(var0.get("id") + "/");
   }

   private static List<Map<String, Object>> d(Map<String, Object> var0, Map<String, Object> var1) {
      ArrayList var2 = new ArrayList();
      String var3 = String.valueOf(var1.get("id"));

      for (Map var5 : a(var0, var3)) {
         if (Boolean.TRUE.equals(var5.get("claimed")) && !"COVERED".equals(var5.get("status"))) {
            LinkedHashMap var6 = new LinkedHashMap();
            var6.put("criterion", var5.get("id"));
            var6.put("status", var5.get("status"));
            var6.put("text", var5.get("text"));
            if (var5.get("claimedBy") != null) {
               var6.put("task", var5.get("claimedBy"));
            }

            if (var5.get("claimedTask") != null) {
               var6.put("taskTitle", var5.get("claimedTask"));
            }

            var2.add(var6);
         }
      }

      return var2;
   }

   private static String a(Map<String, Object> var0, Map<String, Object> var1, Set<String> var2, Set<String> var3) {
      String var4 = String.valueOf(var1.get("id"));
      boolean var5 = false;

      for (Map var7 : io.karatelabs.ext.common.jsapi.CoverageGraphAccessor.a(var0, "hits")) {
         if (var4.equals(var7.get("item")) && a(var7, var2, var3)) {
            var5 = true;
            break;
         }
      }

      var var11 = a(var0, var4);
      if (var11.isEmpty()) {
         return var5 ? "exercised" : "notexercised";
      } else {
         int var12 = 0;

         for (Map var9 : (Iterable<Map>) (Iterable<?>) (var11)) {
            String var10 = String.valueOf(var9.get("status"));
            if (("COVERED".equals(var10) || "FAILING".equals(var10)) && a(var0, var4 + "/" + var9.get("id"), var2, var3)) {
               var12++;
            }
         }

         if (var12 == var11.size()) {
            return "exercised";
         } else {
            return var12 <= 0 && !var5 ? "notexercised" : "partexercised";
         }
      }
   }

   private static boolean a(Map<String, Object> var0, String var1, Set<String> var2, Set<String> var3) {
      for (Map var5 : io.karatelabs.ext.common.jsapi.CoverageGraphAccessor.a(var0, "hits")) {
         if (var1.equals(var5.get("item"))
            && a(var5, var2, var3)
            && (
               !(var5.get("evidence") instanceof Map var6)
                  || (!Boolean.TRUE.equals(var5.get("attested")) || var6.get("status") == null)
                     && !Boolean.TRUE.equals(var6.get("verified"))
                     && (!Boolean.TRUE.equals(var6.get("oracle")) || var6.get("propertyStatus") == null)
                  || "pass".equals(var6.get("status"))
                  || "fail".equals(var6.get("status"))
            )) {
            return true;
         }
      }

      return false;
   }

   private static boolean a(Map<String, Object> var0, Set<String> var1, Set<String> var2) {
      String var3 = String.valueOf(var0.get("test"));
      return var1.contains(var3)
         ? true
         : var2.contains(var3)
            && Boolean.TRUE.equals(var0.get("attested"))
            && var0.get("evidence") instanceof Map var4
            && ("pass".equals(var4.get("status")) || "fail".equals(var4.get("status")));
   }

   public static Map<String, Object> d(Map<String, Object> var0) {
      int var1 = 0;
      int var2 = 0;
      int var3 = 0;

      for (Map var5 : io.karatelabs.ext.common.jsapi.CoverageGraphAccessor.a(var0, "tests")) {
         if ("PASSED".equals(String.valueOf(var5.get("status"))) && !c(var5)) {
            if (var5.get("assertedProportion") instanceof Number var6) {
               var1++;
               if (var6.doubleValue() == 0.0) {
                  var3++;
               }
            } else {
               var2++;
            }
         }
      }

      LinkedHashMap var8 = new LinkedHashMap();
      var8.put("graded", var1);
      var8.put("ungraded", var2);
      var8.put("notasserted", var3);
      var8.put("notMeasured", List.of("fieldFraction", "expectedOrigin", "perHitAttribution"));
      return var8;
   }

   public static Map<String, Object> a(Map<String, Object> var0, Map<String, String> var1, Map<String, Object> var2) {
      LinkedHashMap var3 = new LinkedHashMap();
      String var4 = String.valueOf(var0.get("id"));
      String var5 = var4.startsWith("req:") ? var4.substring(4) : var4;
      var3.put("id", var5);
      var3.put("reqId", var5);
      var3.put("title", var0.get("name"));
      a(var3, var0, "type");
      var3.put("status", var0.get("status"));
      a(var3, var0, "oracleOnly");
      a(var3, var0, "refusalOnly");
      a(var3, var0, "modelOnly");
      a(var3, var0, "mockOnly");
      a(var3, var0, "notasserted");
      a(var3, var0, "weakened");
      a(var3, var0, "assertedProportion");
      a(var3, var0, "reqStatus", "authoredStatus");
      a(var3, var0, "priority");
      a(var3, var0, "criticality");
      a(var3, var0, "derived");
      a(var3, var0, "posture");
      a(var3, var0, "parent");
      a(var3, var0, "ref");
      a(var3, var0, "real");
      a(var3, var0, "acceptance");
      var3.put("tests", io.karatelabs.ext.common.jsapi.CoverageGraphAccessor.c(var1, var0.get("tests")));
      var3.put("testIds", var0.get("tests") instanceof List var6 ? new ArrayList(var6) : List.of());
      var var8 = io.karatelabs.ext.common.jsapi.CoverageGraphAccessor.a(var2, var0.get("tests"));
      if (!var8.isEmpty()) {
         var3.put("executions", var8);
      }

      return var3;
   }

   public static void a(Map<String, Object> var0, io.karatelabs.ext.requirements.CoverageAnalyzer.RequirementCoverageAnalysis var1, String var2) {
      io.karatelabs.ext.requirements.CoverageAnalyzer.RequirementCoverage var3 = var1.a().get(var2);
      if (var3 != null) {
         if (var3.a() != null) {
            var0.put("provenance", var3.a());
         }

         if (var3.c()) {
            var0.put("status", var3.b());
            var0.put("coveredVia", "real");
         }
      }
   }

   public static List<Map<String, Object>> e(Map<String, Object> var0) {
      ArrayList var1 = new ArrayList();

      for (Map var3 : io.karatelabs.ext.common.jsapi.CoverageGraphAccessor.a(var0, "items")) {
         if ("req".equals(var3.get("source")) && !"acceptance-criterion".equals(var3.get("kind"))) {
            var1.add(var3);
         }
      }

      return var1;
   }

   public static List<Map<String, Object>> a(Map<String, Object> var0, String var1) {
      String var2 = var1 + "/";
      ArrayList var3 = new ArrayList();

      for (Map var5 : io.karatelabs.ext.common.jsapi.CoverageGraphAccessor.a(var0, "items")) {
         if ("acceptance-criterion".equals(var5.get("kind")) && String.valueOf(var5.get("id")).startsWith(var2)) {
            LinkedHashMap var6 = new LinkedHashMap();
            var6.put("id", var5.get("criterion"));
            var6.put("text", var5.get("name"));
            var6.put("status", var5.get("status"));
            if (Boolean.TRUE.equals(var5.get("oracleOnly"))) {
               var6.put("oracleOnly", true);
            }

            if (Boolean.TRUE.equals(var5.get("refusalOnly"))) {
               var6.put("refusalOnly", true);
            }

            if (Boolean.TRUE.equals(var5.get("modelOnly"))) {
               var6.put("modelOnly", true);
            }

            if (Boolean.TRUE.equals(var5.get("mockOnly"))) {
               var6.put("mockOnly", true);
            }

            if (Boolean.TRUE.equals(var5.get("notasserted"))) {
               var6.put("notasserted", true);
            }

            if (Boolean.TRUE.equals(var5.get("weakened"))) {
               var6.put("weakened", true);
            }

            a(var6, var5, "assertedProportion");
            var var7 = io.karatelabs.ext.common.jsapi.CoverageGraphAccessor.a(var0, var5.get("tests"));
            if (!var7.isEmpty()) {
               var6.put("executions", var7);
            }

            if (Boolean.TRUE.equals(var5.get("claimed"))) {
               var6.put("claimed", true);
               a(var6, var5, "claimedBy");
               a(var6, var5, "claimedTask");
            }

            a(var0, String.valueOf(var5.get("id")), var6);
            var3.add(var6);
         }
      }

      return var3;
   }

   private static void a(Map<String, Object> var0, String var1, Map<String, Object> var2) {
      boolean var3 = false;
      boolean var4 = false;
      ArrayList var5 = new ArrayList();

      for (Map var7 : io.karatelabs.ext.common.jsapi.CoverageGraphAccessor.a(var0, "hits")) {
         if (var1.equals(var7.get("item"))) {
            if (Boolean.TRUE.equals(var7.get("attested"))) {
               var3 = true;
               if (var7.get("evidence") instanceof Map var8) {
                  LinkedHashMap var10 = new LinkedHashMap(var8);
                  var10.put("test", var7.get("test"));
                  var5.add(var10);
               }
            } else {
               var4 = true;
            }
         }
      }

      if (var3 && !var4) {
         var2.put("attested", true);
      }

      if (!var5.isEmpty()) {
         var2.put("evidence", var5);
      }
   }

   public static void a(Map<String, Object> var0, Map<String, Object> var1, String var2) {
      if (var1.get(var2) != null) {
         var0.put(var2, var1.get(var2));
      }
   }

   private static void a(Map<String, Object> var0, Map<String, Object> var1, String var2, String var3) {
      if (var1.get(var2) != null) {
         var0.put(var3, var1.get(var2));
      }
   }

   private RequirementsReportBuilder() {
   }
}
