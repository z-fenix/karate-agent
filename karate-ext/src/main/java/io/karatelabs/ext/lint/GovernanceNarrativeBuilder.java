package io.karatelabs.ext.lint;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class GovernanceNarrativeBuilder {
   public static final String BLOCKED_STATE = "blocked";
   public static final String FAILING_STATE = "failing";
   public static final String ISSUES_STATE = "issues";
   public static final String WAIVED_STATE = "waived";
   public static final String NOTES_STATE = "notes";
   public static final String NOT_GRADED_STATE = "notGraded";
   public static final String CLEAN_STATE = "clean";

   private GovernanceNarrativeBuilder() {
   }

   public static Map<String, Object> buildHeadline(Map<String, Object> var0) {
      var var1 = asMap(var0.get("lint"));
      var var2 = asMap(var1.get("summary"));
      var var3 = asList(var0.get("gates"));
      var var4 = asList(var0.get("notMeasured"));
      int var5 = intValue(var2, "rules");
      int var6 = intValue(var2, "errors");
      int var7 = intValue(var2, "warnings");
      int var8 = intValue(var2, "infos");
      int var9 = intValue(var2, "waived");
      int var10 = intValue(var2, "rejectedWaivers");
      LinkedHashMap var11 = new LinkedHashMap();
      String var15 = ruleSummarySentence(var5, var4);
      String var12;
      String var13;
      String var14;
      if (!var3.isEmpty()) {
         var12 = "blocked";
         var13 = "BLOCKED";
         var14 = gateSummarySentence(var3) + " " + var15;
      } else if (var6 > 0) {
         var12 = "failing";
         var13 = "ERRORS FOUND";
         var14 = var6 + pluralize(var6, " rule reports an error", " rules report an error") + " against this document. " + var15;
      } else if (var7 > 0) {
         var12 = "issues";
         var13 = "ISSUES FOUND";
         var14 = var7 + pluralize(var7, " warning", " warnings") + " stand against this document. " + var15;
      } else if (var10 > 0) {
         var12 = "issues";
         var13 = "WAIVERS REFUSED";
         var14 = var10 + pluralize(var10, " waiver was refused", " waivers were refused") + ", so the rules they name still apply. " + var15;
      } else if (var9 > 0) {
         var12 = "waived";
         var13 = "CLEAN, ON WAIVERS";
         var14 = "No finding stands, and " + var9 + pluralize(var9, " is waived", " are waived") + ". Each waiver carries a reason and an owner. " + var15;
      } else if (var5 == 0) {
         var12 = "notGraded";
         var13 = "NOTHING WAS CHECKED";
         var14 = "No rule ran against this document, so nothing here is a finding and nothing here is a pass.";
      } else if (var8 > 0) {
         var12 = "notes";
         var13 = "NOTES ONLY";
         var14 = "No error and no warning stands. " + var8 + pluralize(var8, " info finding", " info findings") + " remains. " + var15;
      } else {
         var12 = "clean";
         var13 = "CLEAN, AS FAR AS CHECKED";
         var14 = "No rule reports a finding against this document. " + var15;
      }

      var11.put("state", var12);
      var11.put("word", var13);
      var11.put("sentence", var14.trim());
      ArrayList var16 = new ArrayList();
      addChip(var16, var5, pluralize(var5, "rule ran", "rules ran"), var3.isEmpty() && var6 + var7 + var8 == 0 && var5 > 0 ? "ok" : "");
      if (var6 > 0) {
         addChip(var16, var6, pluralize(var6, "error", "errors"), "no");
      }

      if (var7 > 0) {
         addChip(var16, var7, pluralize(var7, "warning", "warnings"), "warn");
      }

      if (var8 > 0) {
         addChip(var16, var8, pluralize(var8, "info finding", "info findings"), "");
      }

      if (var9 > 0) {
         addChip(var16, var9, pluralize(var9, "waived", "waived"), "warn");
      }

      if (!var4.isEmpty()) {
         addChip(var16, var4.size(), pluralize(var4.size(), "dimension not measured", "dimensions not measured"), "warn");
      }

      var11.put("chips", var16);
      return var11;
   }

   private static String ruleSummarySentence(int var0, List<?> var1) {
      String var2 = var0 + pluralize(var0, " rule ran.", " rules ran.");
      if (!var1.isEmpty()) {
         var2 = var2 + " " + var1.size() + pluralize(var1.size(), " dimension", " dimensions") + " had no evidence to grade: " + joinValues(var1) + ".";
      }

      return var2;
   }

   private static String gateSummarySentence(List<?> var0) {
      Object var1 = var0.get(0);
      String var2 = ensureSentencePunctuation(var1 instanceof Map var3 ? String.valueOf(var3.get("detail")) : String.valueOf(var1));
      return var0.size() == 1 ? var2 : var2 + " " + (var0.size() - 1) + pluralize(var0.size() - 1, " more gate is open.", " more gates are open.");
   }

   private static String ensureSentencePunctuation(String var0) {
      String var1 = var0 == null ? "" : var0.trim();
      return !var1.isEmpty() && !var1.endsWith(".") && !var1.endsWith("!") && !var1.endsWith("?") ? var1 + "." : var1;
   }

   public static List<Map<String, Object>> buildActions(Map<String, Object> var0) {
      var var1 = asMap(var0.get("lint"));
      var var2 = asMap(var1.get("summary"));
      var var3 = asMap(var1.get("graph"));
      var var4 = asList(var0.get("gates"));
      var var5 = asList(var0.get("notMeasured"));
      ArrayList var6 = new ArrayList();

      for (Object var8 : var4) {
         var var9 = asMap(var8);
         addAction(
            var6,
            "Clear the " + var9.get("gate") + " gate.",
            ensureSentencePunctuation(String.valueOf(var9.get("detail"))) + " A gate decides the verdict on its own; it is never folded into the index."
         );
      }

      int var12 = intValue(var2, "errors");
      if (var12 > 0 && var4.isEmpty()) {
         addAction(
            var6,
            "Fix " + var12 + pluralize(var12, " error-severity finding.", " error-severity findings."),
            "Each one names the rule, the line and the JSON pointer it lands on."
         );
      }

      int var13 = intValue(var2, "warnings");
      if (var13 > 0) {
         addAction(var6, "Review " + var13 + pluralize(var13, " warning.", " warnings."), "A warning does not block the verdict. It does lower the dimension it belongs to.");
      }

      int var14 = intValue(var2, "rejectedWaivers");
      if (var14 > 0) {
         addAction(
            var6,
            "Repair " + var14 + pluralize(var14, " refused waiver.", " refused waivers."),
            "A refused waiver grants nothing, so the rule it names still applies. The waiver register says why each was refused."
         );
      }

      for (Object var11 : var5) {
         addAction(var6, "Measure the " + var11 + " dimension.", unmeasuredDimensionAdvice(String.valueOf(var11)));
      }

      if (Boolean.FALSE.equals(var3.get("available")) && var3.get("note") != null) {
         addAction(var6, "Run the suite before grading.", String.valueOf(var3.get("note")));
      }

      int var15 = intValue(var2, "waived");
      if (var15 > 0) {
         addAction(
            var6,
            "Re-read " + var15 + pluralize(var15, " waiver.", " waivers."),
            "A waiver is a signed exception, not a fix. It carries a reason and an owner, and it is yours to defend at the next review."
         );
      }

      return var6;
   }

   private static String unmeasuredDimensionAdvice(String var0) {
      return switch (var0) {
         case "provenness" -> "Nothing graded it because there is no run behind this grade. Run the suite so the coverage graph exists, then grade again.";
         case "contract" -> "Nothing graded it because this project holds no paired run for this document. Record one with Contract.pair({suite, spec, provider}).";
         default -> "No rule in the pack graded it against this document. A dimension with no evidence reads `not measured`, never zero — it is not a score you have earned.";
      };
   }

   private static void addAction(List<Map<String, Object>> var0, String var1, String var2) {
      LinkedHashMap var3 = new LinkedHashMap();
      var3.put("do", var1);
      var3.put("why", var2);
      var0.add(var3);
   }

   private static void addChip(List<Map<String, Object>> var0, int var1, String var2, String var3) {
      LinkedHashMap var4 = new LinkedHashMap();
      var4.put("n", var1);
      var4.put("label", var2);
      var4.put("tone", var3);
      var0.add(var4);
   }

   private static String joinValues(List<?> var0) {
      return var0.stream().map(String::valueOf).reduce((var0x, var1) -> var0x + ", " + var1).orElse("");
   }

   private static String pluralize(int var0, String var1, String var2) {
      return var0 == 1 ? var1 : var2;
   }

   private static int intValue(Map<String, Object> var0, String var1) {
      return var0.get(var1) instanceof Number var2 ? var2.intValue() : 0;
   }

   private static Map<String, Object> asMap(Object var0) {
      return var0 instanceof Map var1 ? var1 : Map.of();
   }

   private static List<?> asList(Object var0) {
      return var0 instanceof List var1 ? var1 : List.of();
   }
}
