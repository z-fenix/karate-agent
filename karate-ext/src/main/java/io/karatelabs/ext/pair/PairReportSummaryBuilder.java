package io.karatelabs.ext.pair;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class PairReportSummaryBuilder {
   public static final String WITHDRAWN = "withdrawn";
   public static final String NOT_MEASURED = "notmeasured";
   public static final String DIVERGED = "diverged";
   public static final String MATCHED = "matched";

   private PairReportSummaryBuilder() {
   }

   public static Map<String, Object> headline(Map<String, Object> var0, Map<String, Object> var1, Map<String, Object> var2) {
      var var3 = a(var0.get("claim"));
      var var4 = a(var0.get("outcomes"));
      var var5 = a(var3.get("claimCoverage"));
      var var6 = a(var0.get("ignored"));
      var var7 = b(var0.get("divergences"));
      var var8 = b(var5.get("notexercised"));
      int var9 = a(var4, "agreed");
      int var10 = a(var4, "agreedFailure");
      int var11 = a(var4, "staleMock") + a(var4, "mockBug") + a(var4, "notassertedDivergence");
      int var12 = a(var4, "notmeasured");
      int var13 = var9 + var10 + var11;
      boolean var14 = Boolean.TRUE.equals(var3.get("measured"));
      boolean var15 = var2 != null && var2.get("withdrawnBy") != null;
      LinkedHashMap var16 = new LinkedHashMap();
      String var17;
      String var18;
      String var19;
      if (var15) {
         var17 = "withdrawn";
         var18 = "EVIDENCE OUT OF DATE";
         var19 = "This pair no longer supports a claim. The evidence is "
            + var1.get("status")
            + " at this asOf. The numbers below are what the run measured. Run the comparison again for a current claim.";
      } else if (var11 > 0 || !var7.isEmpty()) {
         var17 = "diverged";
         var18 = "DIFFERENCES FOUND";
         int var20 = Math.max(var11, var7.size());
         var19 = "The mock and the provider gave different answers in "
            + var20
            + a(var20, " scenario", " scenarios")
            + ". "
            + (var14 ? a(var5, var8) : "This run could not take a proportion, so the difference set below stands on its own as a list.");
      } else if (!var14) {
         var17 = "notmeasured";
         var18 = "NOTHING WAS COMPARED";
         var19 = "This paired run produced no verdict about behaviour. An empty result is an absence of evidence. It does not show that the mock and the provider agree. Read the not-compared rows for the causes.";
      } else {
         var17 = "matched";
         var18 = "MATCHED, WHERE TESTED";
         var19 = "The mock and the provider gave the same answers in " + var13 + a(var13, " compared scenario", " compared scenarios") + ". " + a(var5, var8);
      }

      var16.put("state", var17);
      var16.put("word", var18);
      var16.put("sentence", var19.trim());
      ArrayList var22 = new ArrayList();
      if (var13 > 0 || var14) {
         a(var22, var9, a(var9, "scenario matched", "scenarios matched"), "ok");
      }

      if (var11 > 0) {
         a(var22, var11, a(var11, "difference", "differences"), "no");
      }

      if (var10 > 0) {
         a(var22, var10, a(var10, "scenario failed on both legs", "scenarios failed on both legs"), "warn");
      }

      if (var12 > 0) {
         a(var22, var12, a(var12, "scenario not compared", "scenarios not compared"), "warn");
      }

      if (!var8.isEmpty()) {
         a(var22, var8.size(), a(var8.size(), "operation not tested", "operations not tested"), "warn");
      }

      int var21 = a(var6, "count");
      if (var21 > 0) {
         a(var22, var21, a(var21, "expected difference", "expected differences"), "");
      }

      var16.put("chips", var22);
      return var16;
   }

   private static String a(Map<String, Object> var0, List<?> var1) {
      Object var2 = var0.get("exercised");
      Object var3 = var0.get("declared");
      if (var2 != null && var3 != null) {
         String var4 = "This run compared " + var2 + " of " + var3 + " operations.";
         if (!var1.isEmpty()) {
            var4 = var4 + " It did not test " + var1.size() + ".";
         }

         return var4;
      } else {
         return "";
      }
   }

   public static List<Map<String, Object>> actions(Map<String, Object> var0, Map<String, Object> var1, Map<String, Object> var2) {
      var var3 = a(var0.get("claim"));
      var var4 = a(var0.get("outcomes"));
      var var5 = a(var3.get("claimCoverage"));
      var var6 = a(var0.get("ignored"));
      var var7 = b(var5.get("notexercised"));
      ArrayList var8 = new ArrayList();
      boolean var9 = var2 != null && var2.get("withdrawnBy") != null;
      if (var9) {
         a(var8, "Run the comparison again.", "The evidence is " + var1.get("status") + " at this asOf, so it supports no claim today.");
      }

      int var10 = a(var4, "staleMock") + a(var4, "mockBug") + a(var4, "notassertedDivergence");
      if (var10 > 0) {
         a(var8, "Triage " + var10 + a(var10, " difference", " differences") + ".", "The mock and the provider disagreed. Each row reports which leg differed.");
      }

      int var11 = a(var4, "notmeasured");
      if (var11 > 0) {
         a(
            var8,
            "Fix " + var11 + a(var11, " scenario", " scenarios") + " that could not be compared.",
            "These produced no verdict, so they count in neither half of the proportion."
         );
      }

      for (Object var13 : var7) {
         a(var8, "Add a test for " + var13 + ".", "This run did not exercise it, so nothing here reports whether the mock matches the provider for it.");
      }

      int var16 = a(var6, "count");
      int var17 = b(var6.get("rules")).size();
      if (var16 > 0) {
         a(
            var8,
            "Review " + var17 + a(var17, " ignore rule", " ignore rules") + ".",
            "They waived " + var16 + a(var16, " difference", " differences") + ". Each waiver carries a reason and an owner."
         );
      }

      int var14 = a(var6, "stale");
      if (var14 > 0) {
         a(
            var8,
            "Delete " + var14 + a(var14, " stale ignore rule", " stale ignore rules") + ".",
            "They waived nothing in this run. A stale rule waives a difference that no longer occurs."
         );
      }

      String var15 = var0.get("rehearsalCheck") == null ? null : String.valueOf(var0.get("rehearsalCheck"));
      if (var0.get("rehearsal") == null && var15 != null) {
         a(
            var8,
            "Record which deployment you tested.",
            var15.startsWith("Not checked.")
               ? "The provider leg recorded no HTTP exchange, so nothing was established about what answered. Set `providerEnv` and `providerBuild`."
               : "The wire shows only that the responder was not one of our mocks. Record the rest in `providerEnv` and `providerBuild`."
         );
      }

      return var8;
   }

   private static void a(List<Map<String, Object>> var0, String var1, String var2) {
      LinkedHashMap var3 = new LinkedHashMap();
      var3.put("do", var1);
      var3.put("why", var2);
      var0.add(var3);
   }

   private static void a(List<Map<String, Object>> var0, int var1, String var2, String var3) {
      LinkedHashMap var4 = new LinkedHashMap();
      var4.put("n", var1);
      var4.put("label", var2);
      var4.put("tone", var3);
      var0.add(var4);
   }

   private static String a(int var0, String var1, String var2) {
      return var0 == 1 ? var1 : var2;
   }

   private static int a(Map<String, Object> var0, String var1) {
      return var0.get(var1) instanceof Number var2 ? var2.intValue() : 0;
   }

   private static Map<String, Object> a(Object var0) {
      return var0 instanceof Map var1 ? var1 : Map.of();
   }

   private static List<?> b(Object var0) {
      return var0 instanceof List var1 ? var1 : List.of();
   }
}
