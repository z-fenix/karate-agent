package io.karatelabs.ext.lint;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public final class GovernanceGradeMarkdownRenderer {
   private GovernanceGradeMarkdownRenderer() {
   }

   public static String render(Map<String, Object> var0) {
      Map<java.lang.String, Object> var1 = (Map)var0.get("level");
      Map var2 = (Map)var0.get("dimensions");
      Map<java.lang.String, Object> var3 = (Map)var0.get("weights");
      List var4 = (List)var0.get("gates");
      Object var5 = var0.get("index");
      String var6 = String.valueOf(var0.get("verdict"));
      StringBuilder var7 = new StringBuilder("# API governance grade\n\n");
      Map<java.lang.String, java.util.List> var8 = (Map)var0.get("headline");
      if (var8 != null) {
         var7.append("## ").append(var8.get("word")).append("\n\n").append(var8.get("sentence")).append("\n\n");
         List var9 = (List)var8.get("chips");
         if (var9 != null && !var9.isEmpty()) {
            StringBuilder var10 = new StringBuilder();

            for (Map var12 : (Iterable<Map>) (Iterable<?>) (var9)) {
               var10.append(var10.length() == 0 ? "" : " · ").append("**").append(var12.get("n")).append("** ").append(var12.get("label"));
            }

            var7.append((CharSequence)var10).append("\n\n");
         }
      }

      List var14 = (List)var0.get("actions");
      if (var14 != null && !var14.isEmpty()) {
         var7.append("### Next\n\n");

         for (Map var20 : (Iterable<Map>) (Iterable<?>) (var14)) {
            var7.append("1. **").append(var20.get("do")).append("** ").append(var20.get("why")).append("\n");
         }

         var7.append("\n");
      }

      var7.append("**")
         .append(verdictLabel(var6))
         .append("** · ")
         .append(var5 == null ? "index _not scored_" : "index **" + percent(var5) + "**")
         .append(" · level ")
         .append(var1.get("rung"))
         .append(" — **")
         .append(var1.get("name"))
         .append("**\n\n")
         .append("> ")
         .append(var1.get("means"))
         .append("\n\n");
      if (var1.get("blockedBy") != null) {
         var7.append("_Held here by: ").append(var1.get("blockedBy")).append("._\n\n");
      }

      if (var0.get("verdictNote") != null) {
         var7.append("_").append(var0.get("verdictNote")).append("_\n\n");
      }

      if (var1.get("next") instanceof Map var16) {
         var7.append("_Next (level ")
            .append(var16.get("rung"))
            .append(", ")
            .append(var16.get("name"))
            .append("): ")
            .append(var16.get("needs"))
            .append("._\n\n");
      }

      if (var1.get("ceiling") != null) {
         var7.append("_").append(var1.get("ceiling")).append("_\n\n");
      }

      var7.append("| Dimension | Score | Weight | What it grades |\n|---|---:|---:|---|\n");

      for (Entry var22 : (Iterable<Entry>) (Iterable<?>) (var2.entrySet())) {
         Map<java.lang.String, Object> var25 = (Map)var22.getValue();
         boolean var13 = Boolean.TRUE.equals(var25.get("measured"));
         var7.append("| `")
            .append((String)var22.getKey())
            .append("` | ")
            .append(var13 ? percent(var25.get("score")) : "_not measured_")
            .append(" | ")
            .append(var13 ? percent(var25.get("effectiveWeight")) : "—")
            .append(" | ")
            .append(var25.get("label"))
            .append(" |\n");
      }

      var7.append("\n");

      for (Entry var23 : (Iterable<Entry>) (Iterable<?>) (var2.entrySet())) {
         Map<java.lang.String, java.util.List> var26 = (Map)var23.getValue();
         if (var26.get("note") != null) {
            var7.append("> ⚠️ **").append((String)var23.getKey()).append("** — ").append(var26.get("note")).append("\n>\n");
         }

         if (var26.get("cappedAt") != null) {
            var7.append("> \ud83d\udd12 **")
               .append((String)var23.getKey())
               .append("** capped at ")
               .append(percent(var26.get("cappedAt")))
               .append(" by `")
               .append(String.join("`, `", (List)var26.get("cappedBy")))
               .append("` — it scored ")
               .append(percent(var26.get("uncappedScore")))
               .append(" on its own rules.\n>\n");
         }
      }

      if (var3.get("note") != null) {
         var7.append("> ").append(var3.get("note")).append("\n\n");
      }

      Map<java.lang.String, Object> var19 = (Map)var0.get("waived");
      if (var19 != null && intValue(var19.get("count")) > 0) {
         var7.append("> \ud83d\udcdd ").append(var19.get("note")).append("\n\n");
      }

      Map<java.lang.String, Object> var24 = (Map)var0.get("rehearsal");
      if (var24 != null && var24.get("note") != null) {
         var7.append("> \ud83c\udfad ").append(var24.get("note")).append("\n\n");
      }

      if (!var4.isEmpty()) {
         var7.append("## Gates — why the verdict is what it is\n\n");

         for (Map var29 : (Iterable<Map>) (Iterable<?>) (var4)) {
            var7.append("- **").append(var29.get("gate")).append("** — ").append(var29.get("detail")).append("\n");
         }

         var7.append("\n_A gate decides the verdict; it never changes the index. The index above is the arithmetic over the dimensions, unaltered._\n\n");
      }

      Map<java.lang.String, Object> var28 = (Map)var0.get("judged");
      if (var28 != null && !Boolean.TRUE.equals(var28.get("available"))) {
         var7.append("_").append(var28.get("note")).append("_\n");
      }

      return var7.toString();
   }

   private static String verdictLabel(String var0) {
      return switch (var0) {
         case "READY" -> "✅ READY";
         case "CONDITIONAL" -> "⚠️ CONDITIONAL";
         default -> "\ud83d\udeab NOT READY";
      };
   }

   private static int intValue(Object var0) {
      return var0 instanceof Number var1 ? var1.intValue() : 0;
   }

   private static String percent(Object var0) {
      return var0 instanceof Number var1 ? Math.round(var1.doubleValue() * 100.0) + "%" : "_n/a_";
   }
}
