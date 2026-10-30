package io.karatelabs.ext.requirements;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public final class ReadinessEvaluator {
   private static final String a = "HIGH";
   private static final String b = "MEDIUM";
   private static final String c = "LOW";
   private static final String d = "NONE";
   private static final String e = "READY";
   private static final String f = "CONDITIONAL";
   private static final String g = "NOT_READY";

   private ReadinessEvaluator() {
   }

   public static Map<String, Object> a(List<Map<String, Object>> var0) {
      ArrayList<java.util.LinkedHashMap> var1 = new ArrayList();
      ArrayList<java.util.LinkedHashMap> var2 = new ArrayList();
      ArrayList<java.util.LinkedHashMap> var3 = new ArrayList();
      ArrayList<java.util.Map> var4 = new ArrayList();
      ArrayList<java.util.Map> var5 = new ArrayList();
      LinkedHashMap<java.lang.String, java.lang.Integer> var6 = new LinkedHashMap();
      var6.put("HIGH", 0);
      var6.put("MEDIUM", 0);
      var6.put("LOW", 0);
      var6.put("NONE", 0);

      for (Map var8 : var0) {
         String var9 = io.karatelabs.ext.requirements.EvidencePolicy.b(a(var8.get("criticality")));
         String var10 = a(var8.get("status"));
         String var11 = a(var8.get("provenance"));
         boolean var12 = Boolean.TRUE.equals(var8.get("oracleOnly"));
         boolean var13 = Boolean.TRUE.equals(var8.get("refusalOnly"));
         boolean var14 = Boolean.TRUE.equals(var8.get("modelOnly"));
         boolean var15 = Boolean.TRUE.equals(var8.get("notasserted"));
         String var16 = a(var9, var10, var11, var12, var13, var14, var15);
         LinkedHashMap<java.lang.String, java.lang.Object> var17 = new LinkedHashMap();
         var17.put("reqId", var8.get("reqId"));
         var17.put("title", var8.get("title"));
         var17.put("criticality", var9);
         var17.put("coverage", var10);
         if (var8.get("exercisedLevel") != null) {
            var17.put("exercisedLevel", var8.get("exercisedLevel"));
         }

         if (!var11.isEmpty()) {
            var17.put("provenance", var11);
         }

         if ("COVERED".equals(var10) && "notexercised".equals(var11)) {
            var17.put("trustGap", "declared COVERED but no @real= artifact was observed (looks green, not verified)");
         }

         if ("COVERED".equals(var10) && "notresolved".equals(var11)) {
            var17.put(
               "trustGap",
               "declared COVERED but the @real= target names no known artifact (typo? — fix the id or remove the tag; @real= values are namespaced + space-free, e.g. openapi:<opId>). List the real ids with Coverage.items('openapi') / Coverage.items('grpc') — the ids are generated for a spec op with no operationId, so they cannot be guessed from the spec text"
            );
         }

         if (var15) {
            var17.put("notasserted", true);
            String var18 = "covered only by tests that assert nothing — a pass certifies traffic, not behaviour (add a match that locks a value or a shape)";
            Object var19 = var17.get("trustGap");
            var17.put("trustGap", var19 == null ? var18 : var19 + "; also " + var18);
            var4.add(c(var8));
         }

         if (var12 || var13) {
            if (var12) {
               var17.put("oracleOnly", true);
            }

            if (var13) {
               var17.put("refusalOnly", true);
            }

            String var26 = var13
               ? "proved only by refusal — the shape refuses the reject row(s) this criterion named, which does not show the behaviour it describes works (add a scenario whose calc.req reaches it, or a test)"
               : "realized by the rules only — nothing outside the rulebook checked it (add a test, or stamp the comparison with Rule.execute(…).verify(…) — which records evidence only inside a RUN: called from the console it returns a verdict and persists nothing)";
            Object var30 = var17.get("trustGap");
            var17.put("trustGap", var30 == null ? var26 : var30 + "; also " + var26);
         }

         if (var14) {
            var17.put("modelOnly", true);
            String var27 = "checked only against the model — the checker's verdict covers the design, nothing exercised the implementation (add a test, or stamp the comparison with Rule.execute(…).verify(…) inside a run)";
            Object var31 = var17.get("trustGap");
            var17.put("trustGap", var31 == null ? var27 : var31 + "; also " + var27);
         }

         if (var8.get("proofs") instanceof List var28 && !var28.isEmpty()) {
            var17.put("proofs", var28);
            String var33 = c(var28);
            if (var33 != null) {
               var16 = "HIGH";
               Object var20 = var17.get("trustGap");
               var17.put("trustGap", var20 == null ? var33 : var20 + "; also " + var33);
            }
         }

         List var29 = d(var8);
         if (!var29.isEmpty()) {
            var17.put("claimedUnverified", var29);

            for (Map var40 : (Iterable<Map>) (Iterable<?>) (var29)) {
               LinkedHashMap<java.lang.String, java.lang.Object> var21 = new LinkedHashMap(var40);
               var21.put("reqId", var8.get("reqId"));
               var21.put("title", var8.get("title"));
               var3.add(var21);
            }

            String var35 = d(var29);
            Object var41 = var17.get("trustGap");
            var17.put("trustGap", var41 == null ? var35 : var41 + "; also " + var35);
         }

         if (var8.get("counterexamples") instanceof List var36 && !var36.isEmpty()) {
            var17.put("counterexamples", var36);
            String var43 = b(var36);
            Object var48 = var17.get("trustGap");
            var17.put("trustGap", var48 == null ? var43 : var48 + "; also " + var43);
         }

         if (var8.get("notreachable") instanceof List var37 && !var37.isEmpty()) {
            var17.put("notreachable", var37);
            Object var45 = var37.get(0) instanceof Map var49 ? var49.get("property") : null;
            String var50 = "situation "
               + (var45 == null ? "" : "'" + var45 + "' ")
               + "NOTREACHABLE — no input of the closed universe produces it (notreachable[])";
            Object var51 = var17.get("trustGap");
            var17.put("trustGap", var51 == null ? var50 : var51 + "; also " + var50);
         }

         if (Boolean.TRUE.equals(var8.get("derived"))) {
            var17.put("derived", true);
            String var38 = "a spec-derived stub (@source=derived) — ground it: real title, @criticality, acceptance; then drop the marker";
            Object var46 = var17.get("trustGap");
            var17.put("trustGap", var46 == null ? var38 : var46 + "; also " + var38);
            var5.add(c(var8));
         }

         if (Boolean.TRUE.equals(var8.get("domainRestricted"))) {
            var17.put("domainRestricted", true);
         }

         if (var8.get("executions") instanceof List var39 && !var39.isEmpty()) {
            var17.put("executions", var39);
         }

         var17.put("risk", var16);
         var1.add(var17);
         var6.merge(var16, 1, Integer::sum);
         if ("HIGH".equals(var16)) {
            var2.add(var17);
         }
      }

      int var23 = e(var6);
      String var24 = !var2.isEmpty() ? "NOT_READY" : (var23 <= 0 && var5.isEmpty() ? "READY" : "CONDITIONAL");
      LinkedHashMap<java.lang.String, java.lang.Object> var25 = new LinkedHashMap();
      var25.put("ready", "READY".equals(var24));
      var25.put("state", var24);
      var25.put("verdict", a(var24, var2.size(), var23, var5.size()));
      var25.put("counts", var6);
      var25.put("blockers", var2);
      if (!var3.isEmpty()) {
         var25.put("claimedUnverified", var3);
      }

      if (!var4.isEmpty()) {
         var25.put("notasserted", var4);
      }

      if (!var5.isEmpty()) {
         var25.put("derived", var5);
      }

      var25.put("requirements", var1);
      return var25;
   }

   private static Map<String, Object> c(Map<String, Object> var0) {
      LinkedHashMap<java.lang.String, java.lang.Object> var1 = new LinkedHashMap();
      var1.put("reqId", var0.get("reqId"));
      var1.put("title", var0.get("title"));
      return var1;
   }

   private static List<Map<String, Object>> d(Map<String, Object> var0) {
      return var0.get("claimedUnverified") instanceof List var2 ? var2 : List.of();
   }

   private static String b(List<?> var0) {
      String var1 = null;
      String var2 = null;

      for (Object var4 : var0) {
         Map<java.lang.String, java.lang.Object> var5 = var4 instanceof Map var6 ? var6 : Map.of();
         Object var7 = var5.get("property");
         if (var5.get("definition") != null) {
            var2 = var2 != null
               ? var2
               : "model obligation "
                  + (var7 == null ? "" : "'" + var7 + "' ")
                  + "of "
                  + var5.get("definition")
                  + " has a COUNTEREXAMPLE — the model admits the trace attached (counterexamples[]); the design, not a run, is what failed";
         } else {
            var1 = var1 != null
               ? var1
               : "guarantee " + (var7 == null ? "" : "'" + var7 + "' ") + "VIOLATED — the breaking input is attached (counterexamples[])";
         }
      }

      return var1 == null ? var2 : (var2 == null ? var1 : var1 + "; also " + var2);
   }

   private static String c(List<?> var0) {
      ArrayList<java.lang.String> var1 = new ArrayList();

      for (Object var3 : var0) {
         if (var3 instanceof Map var4 && !"pass".equals(var4.get("proof"))) {
            var1.add("required proof " + var4.get("definition") + " for criterion " + var4.get("criterion") + " is " + var4.get("proof"));
         }
      }

      return var1.isEmpty() ? null : String.join("; ", var1) + " (run the checker from its runbook and ingest its result)";
   }

   private static String d(List<Map<String, Object>> var0) {
      Map<java.lang.String, Object> var1 = (Map)var0.get(0);
      String var2 = var1.get("task") == null ? "a completed task" : "task " + var1.get("task");
      return var0.size()
         + (var0.size() == 1 ? " criterion" : " criteria")
         + " marked done by "
         + (var0.size() == 1 ? var2 : "a completed task")
         + " but not verified (criterion "
         + var1.get("criterion")
         + " is "
         + var1.get("status")
         + ") — the checkbox is a claim, not evidence; add the check that exercises it";
   }

   public static String a(String var0, String var1, String var2, boolean var3, boolean var4, boolean var5) {
      return a(var0, var1, var2, var3, var4, false, var5);
   }

   public static String a(String var0, String var1, String var2, boolean var3, boolean var4, boolean var5, boolean var6) {
      boolean var7 = "COVERED".equals(var1) && ("notexercised".equals(var2) || "notresolved".equals(var2) || var3 || var4 || var5 || var6);
      if ("COVERED".equals(var1) && !var7) {
         return "NONE";
      } else {
         boolean var8 = "FAILING".equals(var1);

         return switch (var0) {
            case "high" -> "HIGH";
            case "low" -> var8 ? "MEDIUM" : "LOW";
            default -> var8 ? "HIGH" : "MEDIUM";
         };
      }
   }

   private static String a(String var0, int var1, int var2, int var3) {
      String var4 = b(var0, var1, var2, var3);
      return var4 == null ? "READY" : ("CONDITIONAL".equals(var0) ? "CONDITIONAL" : "NOT READY") + " — " + var4;
   }

   private static String b(String var0, int var1, int var2, int var3) {
      return switch (var0) {
         case "READY" -> null;
         case "CONDITIONAL" -> (var2 > 0 ? var2 + " requirement" + (var2 == 1 ? "" : "s") + " not yet covered" + (var3 > 0 ? ", " : "") : "")
            + (var3 > 0 ? var3 + " spec-derived stub" + (var3 == 1 ? "" : "s") + " not yet grounded" : "")
            + " (no high-criticality blocker)";
         default -> var1 + " high-risk requirement" + (var1 == 1 ? "" : "s") + " must be addressed before release";
      };
   }

   public static String a(Map<?, ?> var0) {
      if (var0.get("state") instanceof String var1) {
         int var7 = var0.get("blockers") instanceof List var3 ? var3.size() : 0;
         int var8 = var0.get("counts") instanceof Map var9 ? e(var9) : 0;
         int var10 = var0.get("derived") instanceof List var11 ? var11.size() : 0;
         return b(var1, var7, var8, var10);
      } else {
         return null;
      }
   }

   private static int e(Map<?, ?> var0) {
      int var1 = 0;

      for (Entry var3 : var0.entrySet()) {
         if (!"NONE".equals(var3.getKey()) && var3.getValue() instanceof Number var4) {
            var1 += var4.intValue();
         }
      }

      return var1;
   }

   public static String b(Map<String, Object> var0) {
      String var1 = a(var0.get("state"));
      String var2 = a(var0.get("verdict"));
      Map var3 = (Map)var0.get("counts");
      List var4 = (List)var0.get("blockers");
      StringBuilder var5 = new StringBuilder("# Release readiness\n\n");

      var5.append(switch (var1) {
         case "READY" -> "## ✅ READY TO SHIP\n\n";
         case "CONDITIONAL" -> "## ⚠️ CONDITIONAL\n\n";
         default -> "## \ud83d\udeab NOT READY\n\n";
      });
      if (!"READY".equals(var1)) {
         var5.append(var2).append("\n\n");
      }

      var5.append("| Risk | Requirements |\n|---|---|\n");
      var5.append("| \ud83d\udd34 HIGH | ").append(a(var3, "HIGH")).append(" |\n");
      var5.append("| \ud83d\udfe0 MEDIUM | ").append(a(var3, "MEDIUM")).append(" |\n");
      var5.append("| \ud83d\udfe1 LOW | ").append(a(var3, "LOW")).append(" |\n");
      var5.append("| \ud83d\udfe2 NONE | ").append(a(var3, "NONE")).append(" |\n");
      List var6 = (List)(var0.getOrDefault("claimedUnverified", List.of()));
      if (!var6.isEmpty()) {
         var5.append("\n## Claimed done, not verified\n\n");
         var5.append("_A task is marked complete but the criteria it names have no passing check._\n\n");
         var5.append("| Task | Requirement | Criterion | Status |\n|---|---|---|---|\n");

         for (Map var8 : (Iterable<Map>) (Iterable<?>) (var6)) {
            var5.append("| ")
               .append(var8.get("task") == null ? "—" : a(var8.get("task")))
               .append(" ")
               .append(a(var8.get("taskTitle")))
               .append(" | ")
               .append(a(var8.get("reqId")))
               .append(" | ")
               .append(a(var8.get("criterion")))
               .append(" — ")
               .append(a(var8.get("text")))
               .append(" | ")
               .append(a(var8.get("status")))
               .append(" |\n");
         }
      }

      List var11 = (List)(var0.getOrDefault("derived", List.of()));
      if (!var11.isEmpty()) {
         var5.append("\n## Spec-derived stubs, not yet grounded\n\n");
         var5.append("_Each restates the API, not the intent: give it a real title, `@criticality` and acceptance, then drop `@source=derived`._\n\n");

         for (Map var9 : (Iterable<Map>) (Iterable<?>) (var11)) {
            var5.append("- **").append(a(var9.get("reqId"))).append("** ").append(a(var9.get("title"))).append("\n");
         }
      }

      if (!var4.isEmpty()) {
         var5.append("\n## Blockers (high risk)\n\n");
         var5.append("| Requirement | Criticality | Coverage | Note |\n|---|---|---|---|\n");

         for (Map var14 : (Iterable<Map>) (Iterable<?>) (var4)) {
            var5.append("| ")
               .append(a(var14.get("reqId")))
               .append(" ")
               .append(a(var14.get("title")))
               .append(" | ")
               .append(a(var14.get("criticality")))
               .append(" | ")
               .append(a(var14.get("coverage")))
               .append(" | ")
               .append(a(var14.get("trustGap")))
               .append(" |\n");
         }
      }

      return var5.toString();
   }

   private static int a(Map<String, Object> var0, String var1) {
      return var0.get(var1) instanceof Number var3 ? var3.intValue() : 0;
   }

   private static String a(Object var0) {
      return var0 == null ? "" : String.valueOf(var0);
   }
}
