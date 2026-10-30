package io.karatelabs.ext.requirements.jsapi;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class RequirementPostureAnalyzer {
   private RequirementPostureAnalyzer() {
   }

   static Map<String, Object> a(List<Map<String, Object>> var0, Object var1) {
      ArrayList<java.util.LinkedHashMap> var2 = new ArrayList();
      LinkedHashMap<java.lang.String, java.lang.Integer> var3 = new LinkedHashMap();
      var3.put("shared", 0);
      var3.put("attest", 0);
      var3.put("airgap", 0);
      int var4 = 0;
      int var5 = 0;
      int var6 = 0;
      int var7 = 0;
      int var8 = 0;

      for (Map var10 : var0) {
         String var11 = io.karatelabs.ext.requirements.EvidencePolicy.b(a(var10.get("criticality")));
         String var12 = a(var10.get("status"));
         String var13 = a(var10, var11, var1);
         LinkedHashMap<java.lang.String, java.lang.Object> var14 = new LinkedHashMap();
         var14.put("reqId", var10.get("reqId"));
         var14.put("title", var10.get("title"));
         var14.put("criticality", var11);
         var14.put("coverage", var12);
         var14.put("posture", var13);
         var3.merge(var13, 1, Integer::sum);
         if (io.karatelabs.ext.requirements.EvidencePolicy.c(var13)) {
            var4++;
            String var15 = a(var10.get("provenance"));
            if (var15.isEmpty()) {
               var15 = b(var10);
            }

            var14.put("provenance", var15);
            switch (var15) {
               case "exercised":
                  var5++;
                  break;
               case "partexercised":
                  var6++;
                  break;
               case "incidental":
                  var8++;
                  break;
               default:
                  var7++;
            }
         }

         var2.add(var14);
      }

      LinkedHashMap<java.lang.String, java.lang.Integer> var18 = new LinkedHashMap();
      var18.put("tracked", var4);
      var18.put("exercised", var5);
      var18.put("partexercised", var6);
      var18.put("incidental", var8);
      var18.put("notexercised", var7);
      LinkedHashMap<java.lang.String, java.lang.Object> var19 = new LinkedHashMap();
      var19.put("counts", var3);
      var19.put("independence", var18);
      var19.put("requirements", var2);
      return var19;
   }

   private static String a(Map<String, Object> var0, String var1, Object var2) {
      if (var2 != null) {
         return io.karatelabs.ext.requirements.EvidencePolicy.a(var1, var2);
      } else {
         Object var3 = var0.get("posture");
         return var3 != null && !String.valueOf(var3).isBlank() ? String.valueOf(var3) : io.karatelabs.ext.requirements.EvidencePolicy.a(var1);
      }
   }

   private static String b(Map<String, Object> var0) {
      String var1 = a(var0.get("exercisedLevel"));
      if (!var1.isEmpty()) {
         return var1;
      } else {
         String var2 = a(var0.get("status"));
         return !"COVERED".equals(var2) && !"FAILING".equals(var2) ? "notexercised" : "exercised";
      }
   }

   static String a(Map<String, Object> var0) {
      Map var1 = (Map)var0.get("counts");
      Map var2 = (Map)var0.get("independence");
      List var3 = (List)var0.get("requirements");
      StringBuilder var4 = new StringBuilder("# Governance posture & eval independence\n\n");
      var4.append("| Posture | Requirements |\n|---|---|\n");
      var4.append("| \ud83d\udd12 airgap | ").append(a(var1, "airgap")).append(" |\n");
      var4.append("| \ud83d\udccb attest | ").append(a(var1, "attest")).append(" |\n");
      var4.append("| \ud83e\udd1d shared | ").append(a(var1, "shared")).append(" |\n\n");
      int var5 = a(var2, "tracked");
      int var6 = a(var2, "notexercised");
      int var7 = a(var2, "partexercised");
      int var8 = a(var2, "incidental");
      var4.append("**Eval independence** (attest/airgap requirements): ").append(a(var2, "exercised")).append("/").append(var5).append(" exercised");
      if (var7 > 0) {
         var4.append(", ⚠️ ").append(var7).append(" partexercised (some `@real=` artifacts unexercised)");
      }

      if (var8 > 0) {
         var4.append(", ⚠️ ").append(var8).append(" incidental (no `@req=` anchor)");
      }

      if (var6 > 0) {
         var4.append(", ⚠️ ").append(var6).append(" notexercised (declared, never exercised)");
      }

      var4.append("\n");
      a(var4, var3, "incidental", "## Impl-shaped evals (credited via `@real=`, no `@req=` anchor)");
      a(var4, var3, "partexercised", "## Part-exercised evals (declared, only partly exercised)");
      a(var4, var3, "notexercised", "## Not-exercised evals (claimed-but-unverified)");
      return var4.toString();
   }

   private static void a(StringBuilder var0, List<Map<String, Object>> var1, String var2, String var3) {
      ArrayList<java.util.Map> var4 = new ArrayList();

      for (Map var6 : var1) {
         if (var2.equals(var6.get("provenance"))) {
            var4.add(var6);
         }
      }

      if (!var4.isEmpty()) {
         var0.append("\n").append(var3).append("\n\n");
         var0.append("| Requirement | Criticality | Posture | Coverage |\n|---|---|---|---|\n");

         for (Map var8 : (Iterable<Map>) (Iterable<?>) (var4)) {
            var0.append("| ")
               .append(a(var8.get("reqId")))
               .append(" ")
               .append(a(var8.get("title")))
               .append(" | ")
               .append(a(var8.get("criticality")))
               .append(" | ")
               .append(a(var8.get("posture")))
               .append(" | ")
               .append(a(var8.get("coverage")))
               .append(" |\n");
         }
      }
   }

   private static int a(Map<String, Object> var0, String var1) {
      return var0.get(var1) instanceof Number var3 ? var3.intValue() : 0;
   }

   private static String a(Object var0) {
      return var0 == null ? "" : String.valueOf(var0);
   }
}
