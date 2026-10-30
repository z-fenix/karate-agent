package io.karatelabs.ext.lint;

import java.util.List;
import java.util.Map;

public final class LintMarkdownRenderer {
   private LintMarkdownRenderer() {
   }

   public static String render(Map<String, Object> var0) {
      Map var1 = (Map)var0.get("summary");
      List var2 = (List)var0.get("findings");
      List var3 = (List)var0.get("waivers");
      Map<java.lang.String, java.util.List> var4 = (Map)var0.get("graph");
      StringBuilder var5 = new StringBuilder("# API governance\n\n");
      var5.append(statusLine(var0)).append("\n");
      if (var4 != null && Boolean.FALSE.equals(var4.get("available")) && var4.get("note") != null) {
         var5.append("> ⚠️ ")
            .append(var4.get("note"))
            .append("\n>\n> Rules affected: `")
            .append(String.join("`, `", (List)var4.get("graphReachingRules")))
            .append("`\n\n");
      }

      if (var2.isEmpty()) {
         var5.append("_No findings — the artifact is clean against every rule that ran._\n");
      } else {
         var5.append("| Rule | Severity | Line | Where | Message |\n|---|---|---|---|---|\n");

         for (Map var7 : (Iterable<Map>) (Iterable<?>) (var2)) {
            Object var8 = var7.get("pointer");
            var5.append("| ")
               .append(stringValue(var7.get("rule")))
               .append(" | ")
               .append(severityLabel(stringValue(var7.get("severity")), Boolean.TRUE.equals(var7.get("waived"))))
               .append(" | ")
               .append(location(var7))
               .append(" | ")
               .append(var8 == null ? "_(document)_" : "`" + var8 + "`")
               .append(" | ")
               .append(stringValue(var7.get("message")).replace("|", "\\|"))
               .append(" |\n");
         }
      }

      if (var3 != null && !var3.isEmpty()) {
         var5.append("\n## Waivers\n\n| Rule | Scope | Source | Owner | Applied | Reason |\n|---|---|---|---|---|---|\n");

         for (Map var10 : (Iterable<Map>) (Iterable<?>) (var3)) {
            var5.append("| ")
               .append(var10.get("rule") == null ? "_(unreadable directive at line " + stringValue(var10.get("line")) + ")_" : stringValue(var10.get("rule")))
               .append(" | ")
               .append(var10.get("scope") != null && !String.valueOf(var10.get("scope")).isEmpty() ? "`" + var10.get("scope") + "`" : "_(whole document)_")
               .append(" | ")
               .append(stringValue(var10.get("source")))
               .append(" | ")
               .append(stringValue(var10.get("owner")))
               .append(" | ")
               .append(var10.get("rejected") != null ? "❌ **REJECTED** — " + var10.get("rejected") : stringValue(var10.get("applied")))
               .append(" | ")
               .append(stringValue(var10.get("reason")))
               .append(" |\n");
         }
      }

      return var5.toString();
   }

   public static String statusLine(Map<String, Object> var0) {
      Map var1 = (Map)var0.get("summary");
      int var2 = intValue(var1, "errors");
      int var3 = intValue(var1, "warnings");
      int var4 = intValue(var1, "waived");
      String var5 = var2 > 0
         ? "❌ FAIL"
         : (
            var3 > 0
               ? "⚠️ ISSUES"
               : (
                  intValue(var1, "rejectedWaivers") > 0
                     ? "⚠️ WAIVERS REFUSED"
                     : (var4 > 0 ? "⚠️ PASSES WITH WAIVERS" : (intValue(var1, "infos") > 0 ? "ℹ️ NOTES" : "✅ CLEAN"))
               )
         );
      Map<java.lang.String, Object> var6 = (Map)var0.get("ruleset");
      return "**"
         + var5
         + "** — "
         + intValue(var1, "rules")
         + " rules · "
         + var2
         + " errors · "
         + var3
         + " warnings · "
         + intValue(var1, "infos")
         + " info · "
         + var4
         + " waived"
         + (var6 == null ? "" : " · ruleset " + io.karatelabs.ext.lint.RuleSetManifest.shortDigest(var6.get("digest")))
         + "\n";
   }

   private static String location(Map<String, Object> var0) {
      if (var0.get("line") == null) {
         return "";
      } else {
         Map<java.lang.String, Object> var1 = (Map)var0.get("position");
         boolean var2 = var1 == null || !Boolean.FALSE.equals(var1.get("exact"));
         return var0.get("line") + ":" + stringValue(var0.get("col")) + (var2 ? "" : " _(≈)_");
      }
   }

   private static String severityLabel(String var0, boolean var1) {
      String var2 = switch (var0) {
         case "error" -> "❌ error";
         case "warn" -> "⚠️ warn";
         default -> "ℹ️ info";
      };
      return var1 ? var2 + " _(waived)_" : var2;
   }

   private static int intValue(Map<String, Object> var0, String var1) {
      return (var0 == null ? null : var0.get(var1)) instanceof Number var3 ? var3.intValue() : 0;
   }

   private static String stringValue(Object var0) {
      return var0 == null ? "" : String.valueOf(var0);
   }
}
