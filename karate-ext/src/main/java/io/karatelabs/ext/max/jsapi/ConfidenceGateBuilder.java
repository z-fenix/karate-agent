package io.karatelabs.ext.max.jsapi;

import io.karatelabs.common.Json;
import io.karatelabs.ext.lint.LintMarkdownRenderer;
import io.karatelabs.ext.requirements.jsapi.Requirement;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class ConfidenceGateBuilder {
   static final String a = "karate-gate/v1";
   static final String b = "confidence-gate";
   private static final String c = "success";
   private static final String d = "neutral";
   private static final String e = "failure";
   private static final String f = "warning";
   private static final String g = "notice";
   private static final String h = "evaluated";
   private static final String i = "empty";
   private static final String j = "partial";
   private static final String k = "failed";
   private static final String l = "notrequested";
   private static final int m = 255;
   private static final int n = 65536;
   private static final int o = 65535;
   private static final Comparator<Map<String, Object>> p = Comparator.<Map<String, Object>>comparingInt(var0 -> {
         String var1 = d(var0.get("level"));

         return switch (var1) {
            case "failure" -> 0;
            case "warning" -> 1;
            default -> 2;
         };
      })
      .thenComparing(var0 -> d(var0.get("path")))
      .thenComparingInt(var0 -> e(var0.get("line")))
      .thenComparing(var0 -> d(var0.get("source")))
      .thenComparing(var0 -> d(var0.get("title")));

   private ConfidenceGateBuilder() {
   }

   static Map<String, Object> a(
      Map<String, Object> var0, Map<String, Object> var1, Map<String, Object> var2, Map<String, List<Map<String, Object>>> var3, Object var4
   ) {
      return a(var0, var1, null, var2, var3, var4);
   }

   static Map<String, Object> a(
      Map<String, Object> var0,
      Map<String, Object> var1,
      Map<String, Object> var2,
      Map<String, Object> var3,
      Map<String, List<Map<String, Object>>> var4,
      Object var5
   ) {
      String var6 = d(a(var5, "pathPrefix"));
      String var7 = a(var0);
      String var8 = b(var1);
      String var9 = c(var2);
      ArrayList var10 = new ArrayList();
      ArrayList var11 = new ArrayList();
      int var12 = 0;
      int var13 = 0;
      if (!"failed".equals(var8) && var1 != null) {
         var12 = b(var1, "error");
         var13 = b(var1, "warn");
         a(var1, var6, var10, var11);
      }

      if (!"failed".equals(var7)) {
         a(var0, var3, var4 == null ? Map.of() : var4, var6, var10, var11);
      }

      int var14 = 0;
      int var15 = 0;
      if ("evaluated".equals(var9) || "partial".equals(var9)) {
         var var16 = b(var2.get("summary"));
         var14 = e(var16.get("breaking"));
         var15 = e(var16.get("warnings"));
         b(var2, var6, var10, var11);
      }

      var10.sort(p);
      String var20 = a(a(a(var0, var7), a(var8, var12, var13)), b(var9, var14, var15));
      String var17 = a(var0, var7, var8, var12, var13, c(var9, var14, var15));
      LinkedHashMap<java.lang.String, java.lang.Object> var18 = new LinkedHashMap();
      var18.put("schema", "karate-gate/v1");
      var18.put("name", "confidence-gate");
      var18.put("conclusion", var20);
      var18.put("title", a(var17, 255));
      String var19 = a(var0, var7, var1, var2, var11);
      var18.put("summary", a(var19, 65535));
      var18.put("annotations", var10);
      var18.put("sources", a(var7, var0, var8, var1, var9, var2));
      var18.put("readiness", var0);
      if (var1 != null) {
         var18.put("lint", var1);
      }

      if (var2 != null) {
         var18.put("diff", var2);
      }

      var18.put("notannotated", var11);
      return var18;
   }

   private static String a(Map<String, Object> var0) {
      if (var0 != null && var0.get("error") == null) {
         var var1 = b(var0.get("counts"));
         if (var1 == null) {
            return "failed";
         } else {
            for (Object var3 : var1.values()) {
               if (var3 instanceof Number var4 && var4.intValue() > 0) {
                  return "evaluated";
               }
            }

            return "empty";
         }
      } else {
         return "failed";
      }
   }

   private static String b(Map<String, Object> var0) {
      if (var0 == null) {
         return "notrequested";
      } else if (var0.get("error") != null) {
         return "failed";
      } else {
         var var1 = b(var0.get("project"));
         if (var1 != null && Boolean.FALSE.equals(var1.get("bootFileReadable"))) {
            return "partial";
         } else {
            for (Map var3 : j(var0)) {
               if (Boolean.TRUE.equals(var3.get("ruleError"))) {
                  return "partial";
               }
            }

            return "evaluated";
         }
      }
   }

   private static String c(Map<String, Object> var0) {
      if (var0 == null) {
         return "notrequested";
      } else if (var0.get("error") != null) {
         return "failed";
      } else {
         return Boolean.TRUE.equals(var0.get("complete")) ? "evaluated" : "partial";
      }
   }

   private static Map<String, Object> a(String var0, Map<String, Object> var1, String var2, Map<String, Object> var3, String var4, Map<String, Object> var5) {
      LinkedHashMap<java.lang.String, java.lang.Object> var6 = new LinkedHashMap();
      switch (var0) {
         case "empty":
            var6.put("readiness", "no requirements were found — a gate over nothing is not a pass");
            break;
         case "failed":
            var6.put("readiness", var1 == null ? "readiness did not evaluate" : d(var1.get("error")));
      }

      switch (var2) {
         case "notrequested":
            var6.put("lint", "no spec was given — API governance was not evaluated (pass Report.gate({spec:'…'}))");
            break;
         case "partial":
            var6.put("lint", d(var3));
            break;
         case "failed":
            var6.put("lint", d(var3.get("error")));
      }

      switch (var4) {
         case "notrequested":
            var6.put("diff", "no baseline was given — API changes were not evaluated (pass Report.gate({diff:{baseline:'…'}}))");
            break;
         case "partial":
            var6.put(
               "diff",
               e(b(var5.get("summary")).get("notEvaluated"))
                  + " construct(s) were not evaluated — this check does not fail on that alone; Openapi.diff's failOn gate does"
            );
            break;
         case "failed":
            var6.put("diff", d(var5.get("error")));
      }

      LinkedHashMap<java.lang.String, java.lang.Object> var7 = new LinkedHashMap();
      var7.put("readiness", var0);
      var7.put("lint", var2);
      var7.put("diff", var4);
      if (!var6.isEmpty()) {
         var7.put("notes", var6);
      }

      return var7;
   }

   private static String d(Map<String, Object> var0) {
      var var1 = b(var0.get("project"));
      if (var1 != null && Boolean.FALSE.equals(var1.get("bootFileReadable"))) {
         return d(var1.get("note"));
      } else {
         ArrayList<java.lang.String> var2 = new ArrayList();

         for (Map var4 : j(var0)) {
            if (Boolean.TRUE.equals(var4.get("ruleError"))) {
               var2.add(d(var4.get("rule")));
            }
         }

         return "the policy did not fully evaluate — these rules raised an error instead of a verdict: " + String.join(", ", var2);
      }
   }

   private static String a(Map<String, Object> var0, String var1) {
      if ("failed".equals(var1)) {
         return "failure";
      } else {
         String var3 = d(var0.get("state"));

         String var2 = switch (var3) {
            case "NOT_READY" -> "failure";
            case "CONDITIONAL" -> "neutral";
            default -> "success";
         };
         return "empty".equals(var1) ? a(var2, "neutral") : var2;
      }
   }

   private static String a(String var0, int var1, int var2) {
      if ("failed".equals(var0)) {
         return "failure";
      } else {
         String var3 = var1 > 0 ? "failure" : (var2 > 0 ? "neutral" : "success");
         return "partial".equals(var0) ? a(var3, "neutral") : var3;
      }
   }

   private static String b(String var0, int var1, int var2) {
      if ("failed".equals(var0)) {
         return "failure";
      } else {
         String var3 = var1 > 0 ? "failure" : (var2 > 0 ? "neutral" : "success");
         return "partial".equals(var0) ? a(var3, "neutral") : var3;
      }
   }

   private static String a(String var0, String var1) {
      return a(var0) >= a(var1) ? var0 : var1;
   }

   private static int a(String var0) {
      return switch (var0) {
         case "failure" -> 2;
         case "neutral" -> 1;
         default -> 0;
      };
   }

   private static void a(Map<String, Object> var0, String var1, List<Map<String, Object>> var2, List<Map<String, Object>> var3) {
      String var4 = d(var0.get("spec"));

      for (Map var6 : j(var0)) {
         if (!Boolean.TRUE.equals(var6.get("waived"))) {
            String var8 = d(var6.get("severity"));

            String var7 = switch (var8) {
               case "error" -> "failure";
               case "warn" -> "warning";
               default -> "notice";
            };
            int var12 = e(var6.get("line"));
            String var13 = e(var6);
            if (!b(var4)) {
               var3.add(a("lint", d(var6.get("rule")), var7, var13, "the spec reference '" + var4 + "' is not a repository path"));
            } else if (var12 < 1) {
               var3.add(a("lint", d(var6.get("rule")), var7, var13, "the finding has no position in the document"));
            } else {
               var var10 = b(var6.get("position"));
               int var11 = var10 == null ? var12 : Math.max(var12, e(var10.get("endLine")));
               var2.add(a(c(var1, var4), var12, var11, var7, var13, d(var6.get("rule")), "lint"));
            }
         }
      }
   }

   private static String e(Map<String, Object> var0) {
      var var1 = b(var0.get("position"));
      String var2 = d(var0.get("message"));
      if (var1 != null && Boolean.FALSE.equals(var1.get("exact"))) {
         var2 = var2 + "\n(this line is the nearest enclosing node" + (var1.get("pointer") == null ? "" : " " + var1.get("pointer")) + ", not the leaf itself)";
      }

      return var2;
   }

   private static void b(Map<String, Object> var0, String var1, List<Map<String, Object>> var2, List<Map<String, Object>> var3) {
      String var4 = d(b(var0.get("current")).get("source"));

      for (String var6 : List.of("breaking", "warnings")) {
         String var7 = "breaking".equals(var6) ? "failure" : "warning";

         for (Map var9 : c(var0.get(var6))) {
            if (!Boolean.TRUE.equals(var9.get("waived"))) {
               String var10 = d(var9.get("kind"));
               String var11 = a(var9, "failure".equals(var7));
               var var12 = b(var9.get("currentLocation"));
               String var13 = var12 == null
                  ? "baselineonly"
                  : (
                     !b(var4)
                        ? "the spec reference '" + var4 + "' is not a repository path"
                        : (e(var12.get("line")) < 1 ? "the finding has no position in the document" : null)
                  );
               if (var13 != null) {
                  var3.add(a("diff", d(var9.get("id")), var7, var11, var13));
               } else {
                  int var14 = e(var12.get("line"));
                  var2.add(a(c(var1, var4), var14, var14, var7, var11, var10, "diff"));
               }
            }
         }
      }
   }

   private static String a(Map<String, Object> var0, boolean var1) {
      String var2 = d(var0.get("path"));
      var var3 = b(var0.get("subject"));
      String var4 = var3 == null ? "" : d(var3.get("key"));
      String var5 = var2.startsWith(var4 + " ") ? var2.substring(var4.length() + 1) : (var2.equals(var4) ? "" : var2);
      String var6 = var0.get("detail") == null ? null : d(var0.get("detail"));
      String var7 = var5.isEmpty() ? (var6 == null ? var2 : var6) : (var6 == null ? var5 : var5 + " — " + var6);
      StringBuilder var8 = new StringBuilder(d(var0.get("kind"))).append(" · ").append(var7);
      var var9 = b(var0.get("witness"));
      if (var1 && var9 != null && !Boolean.TRUE.equals(var9.get("truncated"))) {
         Object var10 = var9.get("instance");
         String var11 = var10 == null ? "null" : Json.stringifyStrict(var10);
         if (var11.length() <= 120) {
            var8.append("\ncounterexample (valid under ").append(d(var9.get("validUnder"))).append("): ").append(var11);
         }
      }

      var var14 = b(var0.get("join"));
      ArrayList<java.lang.String> var15 = new ArrayList();

      for (Map var13 : c(var14 == null ? null : var14.get("tests"))) {
         var15.add(d(var13.get("file")) + ":" + d(var13.get("line")));
      }

      if (!var15.isEmpty()) {
         var8.append("\ntests: ").append(String.join(", ", var15));
      }

      var var16 = b(var0.get("currentLocation"));
      if (var16 != null && Boolean.FALSE.equals(var16.get("exact"))) {
         var8.append("\n(this line is the nearest enclosing node, not ").append(d(var0.get("pointer"))).append(" itself)");
      }

      return var8.toString();
   }

   private static void a(
      Map<String, Object> var0,
      Map<String, Object> var1,
      Map<String, List<Map<String, Object>>> var2,
      String var3,
      List<Map<String, Object>> var4,
      List<Map<String, Object>> var5
   ) {
      var var6 = c(var0 == null ? null : var0.get("requirements"));
      if (!var6.isEmpty()) {
         var var7 = c(var1, "tests");
         var var8 = c(var1, "items");
         LinkedHashMap var9 = new LinkedHashMap();

         for (Map var11 : (Iterable<Map>) (Iterable<?>) (var6)) {
            String var13 = d(var11.get("risk"));

            String var12 = switch (var13) {
               case "HIGH" -> "failure";
               case "MEDIUM" -> "warning";
               default -> null;
            };
            if (var12 != null) {
               var13 = d(var11.get("reqId"));
               String var26 = "req:" + var13;
               var var15 = a((List<Map<String, Object>>)var2.get(var26));
               String var16 = a(var11, var15);
               var var17 = a(var1, var7, var26, var15);
               boolean var18 = false;

               for (Map var20 : (Iterable<Map>) (Iterable<?>) (var17)) {
                  String var21 = d(var20.get("feature"));
                  int var22 = e(var20.get("line"));
                  if (b(var21) && var22 >= 1) {
                     a(var9, c(var3, var21), var22, var12, var13, var11, var16, d(var20.get("name")));
                     var18 = true;
                  }
               }

               if (!var18) {
                  Map<java.lang.String, Object> var27 = (Map)var8.get(var26);
                  String var28 = var27 == null ? null : d(var27.get("sourceFile"));
                  int var29 = var27 == null ? 0 : e(var27.get("line"));
                  if (b(var28) && var29 >= 1) {
                     a(var9, c(var3, var28), var29, var12, var13, var11, var16, null);
                  } else {
                     var5.add(a("readiness", var13, var12, var16, "no covering test with a repository line, and no requirement heading line"));
                  }
               }
            }
         }

         for (Map var24 : (Iterable<Map>) (Iterable<?>) (var9.values())) {
            var4.add(g(var24));
         }
      }
   }

   private static List<Map<String, Object>> a(List<Map<String, Object>> var0) {
      ArrayList<java.util.Map<java.lang.String,java.lang.Object>> var1 = new ArrayList();

      for (Map var3 : c(var0)) {
         if (f(var3) != null) {
            var1.add(var3);
         }
      }

      return var1;
   }

   private static String f(Map<String, Object> var0) {
      String var1 = d(var0.get("status"));
      if (!"COVERED".equals(var1)) {
         return switch (var1) {
            case "NOTCOVERED" -> "notcovered — no test covers it";
            case "NOTRUN" -> "notrun — its covering test did not run";
            case "FAILING" -> "failing — its covering test failed";
            default -> null;
         };
      } else if (Boolean.TRUE.equals(var0.get("notasserted"))) {
         return "notasserted — every covering test passed without asserting";
      } else if (Boolean.TRUE.equals(var0.get("oracleOnly"))) {
         return "oracleOnly — only the rulebook realizes it; nothing outside it checked";
      } else {
         return Boolean.TRUE.equals(var0.get("refusalOnly")) ? "refusalOnly — the only evidence is the shape refusing what must be refused" : null;
      }
   }

   private static List<Map<String, Object>> a(Map<String, Object> var0, Map<String, Map<String, Object>> var1, String var2, List<Map<String, Object>> var3) {
      LinkedHashSet<java.lang.String> var4 = new LinkedHashSet();
      var4.add(var2);

      for (Map var6 : var3) {
         var4.add(var2 + "/" + d(var6.get("id")));
      }

      ArrayList<java.util.Map<java.lang.String,java.lang.Object>> var11 = new ArrayList();
      LinkedHashSet<java.lang.String> var12 = new LinkedHashSet();

      for (Map var8 : io.karatelabs.ext.common.jsapi.CoverageGraphAccessor.a(var0, "hits")) {
         if (var4.contains(d(var8.get("item")))) {
            String var9 = d(var8.get("test"));
            Map var10 = (Map)var1.get(var9);
            if (var10 != null && var12.add(var9)) {
               var11.add(var10);
            }
         }
      }

      return var11;
   }

   private static String a(Map<String, Object> var0, List<Map<String, Object>> var1) {
      StringBuilder var2 = new StringBuilder();
      var2.append(d(var0.get("reqId")));
      if (var0.get("title") != null) {
         var2.append(" · ").append(var0.get("title"));
      }

      for (Map var4 : var1) {
         var2.append("\n").append(d(var4.get("id"))).append(": ").append(f(var4));
      }

      if (var0.get("trustGap") != null) {
         var2.append("\n").append(var0.get("trustGap"));
      }

      for (Map var7 : c(var0.get("claimedUnverified"))) {
         var2.append("\nclaimed done by task ")
            .append(d(var7.get("task")))
            .append(" but not verified: criterion ")
            .append(d(var7.get("criterion")))
            .append(" is ")
            .append(d(var7.get("status")));
      }

      for (Map var8 : c(var0.get("counterexamples"))) {
         var2.append("\ncounterexample for ")
            .append(d(var8.get("rule")))
            .append("/")
            .append(d(var8.get("property")))
            .append(": ")
            .append(a(var8.get("counterexample")));
      }

      return var2.toString();
   }

   private static void a(
      Map<String, Map<String, Object>> var0, String var1, int var2, String var3, String var4, Map<String, Object> var5, String var6, String var7
   ) {
      Map<java.lang.String, java.lang.Object> var8 = var0.computeIfAbsent(var1 + "\u0000" + var2, var3x -> {
         LinkedHashMap<java.lang.String, java.lang.Object> var4x = new LinkedHashMap();
         var4x.put("path", var1);
         var4x.put("line", var2);
         var4x.put("level", var3);
         var4x.put("blocks", new ArrayList());
         var4x.put("reqs", new LinkedHashSet());
         var4x.put("titles", new ArrayList());
         var4x.put("names", new LinkedHashSet());
         return var4x;
      });
      var8.put("level", b(d(var8.get("level")), var3));
      if (((Set)var8.get("reqs")).add(var4)) {
         ((List)var8.get("blocks")).add(var6);
         ((List)var8.get("titles")).add(var5.get("title") == null ? var4 : var4 + " · " + var5.get("title"));
      }

      if (var7 != null && !var7.isBlank()) {
         ((Set)var8.get("names")).add(var7);
      }
   }

   private static Map<String, Object> g(Map<String, Object> var0) {
      List var1 = (List)var0.get("blocks");
      Set var2 = (Set)var0.get("names");
      List var3 = (List)var0.get("titles");
      StringBuilder var4 = new StringBuilder(String.join("\n\n", var1));
      if (var2.size() > 1) {
         var4.append("\n\ncovering tests on this line: ").append(String.join(", ", var2));
      }

      String var5 = (String)var3.get(0) + (var3.size() > 1 ? " (+" + (var3.size() - 1) + " more)" : "");
      return a(d(var0.get("path")), e(var0.get("line")), e(var0.get("line")), d(var0.get("level")), var4.toString(), var5, "readiness");
   }

   private static String b(String var0, String var1) {
      return !"failure".equals(var0) && !"failure".equals(var1) ? (!"warning".equals(var0) && !"warning".equals(var1) ? "notice" : "warning") : "failure";
   }

   private static Map<String, Object> a(String var0, int var1, int var2, String var3, String var4, String var5, String var6) {
      LinkedHashMap<java.lang.String, java.lang.Object> var7 = new LinkedHashMap();
      var7.put("path", var0);
      var7.put("line", var1);
      var7.put("endLine", var2);
      var7.put("level", var3);
      var7.put("message", b(var4, 65536));
      var7.put("title", a(var5, 255));
      var7.put("source", var6);
      return var7;
   }

   private static Map<String, Object> a(String var0, String var1, String var2, String var3, String var4) {
      LinkedHashMap<java.lang.String, java.lang.Object> var5 = new LinkedHashMap();
      var5.put("source", var0);
      var5.put("ref", var1);
      var5.put("level", var2);
      var5.put("message", var3);
      var5.put("reason", var4);
      return var5;
   }

   private static boolean b(String var0) {
      if (var0 != null && !var0.isBlank() && !var0.startsWith("/") && !var0.startsWith("\\")) {
         int var1 = var0.indexOf(58);
         int var2 = var0.indexOf(47);
         return var1 < 0 || var2 >= 0 && var2 < var1;
      } else {
         return false;
      }
   }

   private static String c(String var0, String var1) {
      String var2 = var1.startsWith("./") ? var1.substring(2) : var1;
      if (var0 != null && !var0.isBlank()) {
         String var3 = var0.endsWith("/") ? var0.substring(0, var0.length() - 1) : var0;
         return var3 + "/" + var2;
      } else {
         return var2;
      }
   }

   private static String a(Map<String, Object> var0, String var1, String var2, int var3, int var4, List<String> var5) {
      ArrayList<java.lang.String> var7 = new ArrayList();
      String var6;
      if ("failed".equals(var1)) {
         var6 = "READINESS UNAVAILABLE";
      } else if ("empty".equals(var1)) {
         var6 = "NOTHING TO GATE";
      } else {
         String var8 = d(var0.get("state"));
         var6 = "NOT_READY".equals(var8) ? "NOT READY" : var8;
         int var9 = c(var0.get("blockers")).size();
         if (var9 > 0) {
            var7.add(var9 + " blocker" + (var9 == 1 ? "" : "s"));
         }
      }

      if ("failed".equals(var2)) {
         var7.add("lint unavailable");
      } else {
         if (var3 > 0) {
            var7.add(var3 + " lint error" + (var3 == 1 ? "" : "s"));
         }

         if (var4 > 0) {
            var7.add(var4 + " lint warning" + (var4 == 1 ? "" : "s"));
         }

         if ("partial".equals(var2)) {
            var7.add("lint partial");
         }
      }

      var7.addAll(var5);
      return var7.isEmpty() ? var6 : var6 + " — " + String.join(", ", var7);
   }

   private static List<String> c(String var0, int var1, int var2) {
      ArrayList<java.lang.String> var3 = new ArrayList();
      if ("failed".equals(var0)) {
         var3.add("API diff unavailable");
         return var3;
      } else {
         if (var1 > 0) {
            var3.add(var1 + " breaking API change" + (var1 == 1 ? "" : "s"));
         }

         if (var2 > 0) {
            var3.add(var2 + " API warning" + (var2 == 1 ? "" : "s"));
         }

         if ("partial".equals(var0)) {
            var3.add("API diff partial");
         }

         return var3;
      }
   }

   private static String a(Map<String, Object> var0, String var1, Map<String, Object> var2, Map<String, Object> var3, List<Map<String, Object>> var4) {
      StringBuilder var5 = new StringBuilder();
      if (var2 != null) {
         var5.append("## API governance\n\n");
         if (var2.get("error") != null) {
            var5.append("**❌ NOT EVALUATED** — ").append(d(var2.get("error"))).append("\n");
         } else {
            var5.append(io.karatelabs.ext.lint.LintMarkdownRenderer.statusLine(var2)).append(i(var2));
         }

         var5.append("\n");
      }

      String var6 = a(var0, var1, var4);
      if (var3 != null) {
         if (var3.get("error") != null) {
            var5.append("## API changes\n\n**❌ NOT EVALUATED** — ").append(d(var3.get("error"))).append("\n");
         } else {
            var5.append(a(var3, 65535 - var5.length() - var6.length() - 1));
         }

         var5.append("\n");
      }

      return var5.append(var6).toString();
   }

   private static String a(Map<String, Object> var0, int var1) {
      return io.karatelabs.ext.openapi.changes.ApiChangelog.a(var0, Math.max(0, var1));
   }

   private static String a(Map<String, Object> var0, String var1, List<Map<String, Object>> var2) {
      StringBuilder var3 = new StringBuilder();
      if ("failed".equals(var1)) {
         var3.append("# Release readiness\n\n## \ud83d\udeab NOT EVALUATED\n\n")
            .append(var0 == null ? "readiness did not evaluate" : d(var0.get("error")))
            .append("\n");
      } else {
         String var4 = Requirement.readinessMarkdown(var0);
         var3.append("empty".equals(var1) ? c(var4) : var4);
         var3.append(h(var0));
      }

      var3.append(b(var2));
      return var3.toString();
   }

   private static String c(String var0) {
      int var1 = var0.indexOf("\n## ");
      if (var1 < 0) {
         return var0;
      } else {
         int var2 = var0.indexOf(10, var1 + 1);
         return var0.substring(0, var1 + 1) + "## ⚠️ NOTHING TO GATE\n\nno requirements were found — a gate over nothing is not a pass" + var0.substring(var2);
      }
   }

   private static String h(Map<String, Object> var0) {
      StringBuilder var1 = new StringBuilder();

      for (Map var3 : c(var0.get("requirements"))) {
         for (Map var5 : c(var3.get("counterexamples"))) {
            if (var1.isEmpty()) {
               var1.append("\n## Counterexamples\n\n")
                  .append("| Requirement | Criterion | Rule | Property | Breaking input |\n")
                  .append("|---|---|---|---|---|\n");
            }

            var1.append("| ")
               .append(d(var3.get("reqId")))
               .append(" | ")
               .append(d(var5.get("criterion")))
               .append(" | ")
               .append(d(var5.get("rule")))
               .append(" | ")
               .append(d(var5.get("property")))
               .append(" | `")
               .append(a(var5.get("counterexample")).replace("|", "\\|"))
               .append("` |\n");
         }
      }

      return var1.toString();
   }

   private static String i(Map<String, Object> var0) {
      StringBuilder var1 = new StringBuilder();
      StringBuilder var2 = new StringBuilder();

      for (Map var4 : c(var0.get("waivers"))) {
         StringBuilder var5 = var4.get("rejected") != null ? var2 : var1;
         if (var5.isEmpty()) {
            var5.append(
                  var4.get("rejected") != null
                     ? "\n### Rejected waivers — nothing suppressed\n\n| Rule | Scope | Owner | Rejected because | Reason |\n"
                     : "\n### Waivers applied\n\n| Rule | Scope | Owner | Applied | Reason |\n"
               )
               .append("|---|---|---|---|---|\n");
         }

         var5.append("| ")
            .append(var4.get("rule") == null ? "_(unreadable directive)_" : d(var4.get("rule")))
            .append(" | ")
            .append(d(var4.get("scope")).isEmpty() ? "_(whole document)_" : "`" + var4.get("scope") + "`")
            .append(" | ")
            .append(d(var4.get("owner")))
            .append(" | ")
            .append(var4.get("rejected") != null ? d(var4.get("rejected")) : d(var4.get("applied")))
            .append(" | ")
            .append(d(var4.get("reason")).replace("|", "\\|"))
            .append(" |\n");
      }

      return var1.toString() + var2;
   }

   private static String b(List<Map<String, Object>> var0) {
      if (var0.isEmpty()) {
         return "";
      } else {
         StringBuilder var1 = new StringBuilder("\n## Not annotated — no repository line to land on\n\n");

         for (Map var3 : var0) {
            var1.append("- **")
               .append(d(var3.get("level")))
               .append("** · ")
               .append(d(var3.get("source")))
               .append(" · `")
               .append(d(var3.get("ref")))
               .append("` — ")
               .append(d(var3.get("reason")))
               .append("\n");
         }

         return var1.toString();
      }
   }

   private static String a(int var0) {
      return "… [truncated, " + var0 + " chars]";
   }

   static String a(String var0, int var1) {
      if (var0 != null && var0.length() > var1) {
         int var2 = c(var0, var1 - a(var0.length()).length());
         return var0.substring(0, var2) + a(var0.length() - var2);
      } else {
         return var0;
      }
   }

   static String b(String var0, int var1) {
      if (var0 != null && d(var0) > var1) {
         int var2 = var1 - d(a(var0.length()));
         int var3 = 0;
         int var4 = var0.length();

         while (var3 < var4) {
            int var5 = var3 + var4 + 1 >>> 1;
            if (d(var0.substring(0, var5)) <= var2) {
               var3 = var5;
            } else {
               var4 = var5 - 1;
            }
         }

         int var6 = c(var0, var3);
         return var0.substring(0, var6) + a(var0.length() - var6);
      } else {
         return var0;
      }
   }

   private static int c(String var0, int var1) {
      int var2 = Math.max(0, Math.min(var1, var0.length()));
      return var2 > 0 && Character.isHighSurrogate(var0.charAt(var2 - 1)) ? var2 - 1 : var2;
   }

   private static int d(String var0) {
      return var0.getBytes(StandardCharsets.UTF_8).length;
   }

   private static int b(Map<String, Object> var0, String var1) {
      int var2 = 0;

      for (Map var4 : j(var0)) {
         if (!Boolean.TRUE.equals(var4.get("waived")) && var1.equals(d(var4.get("severity")))) {
            var2++;
         }
      }

      return var2;
   }

   private static List<Map<String, Object>> j(Map<String, Object> var0) {
      return c(var0 == null ? null : var0.get("findings"));
   }

   private static Map<String, Map<String, Object>> c(Map<String, Object> var0, String var1) {
      LinkedHashMap<java.lang.String, java.util.Map<java.lang.String,java.lang.Object>> var2 = new LinkedHashMap();

      for (Map var4 : io.karatelabs.ext.common.jsapi.CoverageGraphAccessor.a(var0, var1)) {
         var2.putIfAbsent(d(var4.get("id")), var4);
      }

      return var2;
   }

   private static String a(Object var0) {
      return var0 == null ? "" : (var0 instanceof String var1 ? var1 : Json.stringifyStrict(var0));
   }

   private static Object a(Object var0, String var1) {
      return var0 instanceof Map var2 ? var2.get(var1) : null;
   }

   private static Map<String, Object> b(Object var0) {
      return var0 instanceof Map var1 ? var1 : null;
   }

   private static List<Map<String, Object>> c(Object var0) {
      return var0 instanceof List var1 ? var1 : List.of();
   }

   private static String d(Object var0) {
      return var0 == null ? "" : String.valueOf(var0);
   }

   private static int e(Object var0) {
      return var0 instanceof Number var1 ? var1.intValue() : 0;
   }
}
