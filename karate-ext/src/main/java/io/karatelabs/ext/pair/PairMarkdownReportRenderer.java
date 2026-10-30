package io.karatelabs.ext.pair;

import java.util.List;
import java.util.Map;

public final class PairMarkdownReportRenderer {
   private PairMarkdownReportRenderer() {
   }

   public static String render(Map<String, Object> var0) {
      var var1 = a(var0.get("scope"));
      var var2 = a(var0.get("claim"));
      var var3 = a(var0.get("outcomes"));
      List var4 = (List)(var0.getOrDefault("divergences", List.of()));
      List var5 = (List)(var0.getOrDefault("notMeasured", List.of()));
      StringBuilder var6 = new StringBuilder("# Paired run — mock vs provider\n\n");
      var6.append("`")
         .append(var0.get("pairId"))
         .append("` · minted ")
         .append(var0.get("mintedAt"))
         .append(" · provider `")
         .append(var1.get("provider"))
         .append("` · profile `")
         .append(var1.get("profile"))
         .append("`\n\n");
      var var7 = a(var0.get("headline"));
      if (!var7.isEmpty()) {
         var6.append("## ").append(var7.get("word")).append("\n\n").append(var7.get("sentence")).append("\n\n");
         var var8 = b(var7.get("chips"));
         if (!var8.isEmpty()) {
            StringBuilder var9 = new StringBuilder();

            for (Map var11 : (Iterable<Map>) (Iterable<?>) (var8)) {
               var9.append(var9.length() == 0 ? "" : " · ").append("**").append(var11.get("n")).append("** ").append(var11.get("label"));
            }

            var6.append((CharSequence)var9).append("\n\n");
         }
      }

      var var17 = b(var0.get("actions"));
      if (!var17.isEmpty()) {
         var6.append("### Next\n\n");

         for (Map var21 : (Iterable<Map>) (Iterable<?>) (var17)) {
            var6.append("1. **").append(var21.get("do")).append("** ").append(var21.get("why")).append("\n");
         }

         var6.append("\n");
      }

      var6.append("> ").append(var0.get("claimSentence")).append("\n\n");
      if (Boolean.TRUE.equals(var2.get("measured"))) {
         var var19 = a(var2.get("measuredProportion"));
         var var22 = a(var2.get("claimCoverage"));
         var6.append("| | Value | Of |\n|---|---:|---|\n")
            .append("| Divergence-free | ")
            .append(d(var19.get("value")))
            .append(" | ")
            .append(var19.get("divergenceFree"))
            .append(" of ")
            .append(var19.get("admissible"))
            .append(" admissible operations |\n")
            .append("| Coverage of the claim | ")
            .append(d(var22.get("value")))
            .append(" | ")
            .append(var22.get("exercised"))
            .append(" of ")
            .append(var22.get("declared"))
            .append(" declared operations exercised by THIS pair |\n\n");
      }

      var6.append("_").append(var2.get("note")).append("_\n\n");
      var var20 = a(var2.get("rungs"));
      if (!var20.isEmpty()) {
         var var23 = a(var20.get("aggregate"));
         var6.append("## Rung — ").append(var23.get("name")).append(var23.get("rung") == null ? "" : " (" + var23.get("rung") + " of 4)").append("\n\n");
         if (var23.get("means") != null) {
            var6.append(var23.get("means")).append("\n\n");
         }

         for (String var12 : List.of("cappedBy", "blockedBy", "note", "scope")) {
            if (var23.get(var12) != null) {
               var6.append("_").append(var23.get(var12)).append("_\n\n");
            }
         }

         var var26 = b(var20.get("items"));
         if (!var26.isEmpty()) {
            var6.append("| Operation | Rung | Held back by |\n|---|---|---|\n");

            for (Map var13 : (Iterable<Map>) (Iterable<?>) (var26)) {
               var6.append("| `")
                  .append(var13.get("item"))
                  .append("` | ")
                  .append(var13.get("name"))
                  .append(" | ")
                  .append(var13.get("blockedBy") == null ? "—" : String.valueOf(var13.get("blockedBy")))
                  .append(" |\n");
            }

            var6.append("\n");

            for (Map var36 : (Iterable<Map>) (Iterable<?>) (var26)) {
               var6.append("<details><summary>Bars under <code>")
                  .append(var36.get("item"))
                  .append("</code></summary>\n\n| Bar | Held | Counted |\n|---|---|---|\n");

               for (Map var15 : b(var36.get("bars"))) {
                  var6.append("| `")
                     .append(var15.get("bar"))
                     .append("` | ")
                     .append(Boolean.TRUE.equals(var15.get("held")) ? (var15.get("waived") != null ? "waived" : "yes") : "**no**")
                     .append(" | ")
                     .append(var15.get("counted"))
                     .append(" |\n");
               }

               var6.append("\n</details>\n\n");
            }
         }
      }

      var var24 = a(var0.get("warnings"));
      if (!var24.isEmpty()) {
         var6.append("## Warnings — read these BEFORE the divergences\n\n_").append(var24.get("note")).append("_\n\n");

         for (Map var31 : b(var24.get("mock"))) {
            var6.append("- `").append(var31.get("type")).append("` — ").append(var31.get("message")).append("\n");
         }

         var6.append("\n");
      }

      var var28 = a(var2.get("specMismatch"));
      if (!var28.isEmpty()) {
         var6.append("## The `spec` this pair is bound to is not the one it counted\n\n_").append(var28.get("note")).append("_\n\n");
      }

      if (var2.get("noSpec") != null) {
         var6.append("## This pair is bound to no contract document\n\n_").append(var2.get("noSpec")).append("_\n\n");
      }

      var6.append("## Outcomes\n\n| Class | Scenarios |\n|---|---:|\n");

      for (String var37 : io.karatelabs.ext.pair.PairOutcomeClassifier.OUTCOME_ORDER) {
         var6.append("| `").append(var37).append("` | ").append(var3.getOrDefault(var37, 0)).append(" |\n");
      }

      var6.append("\n");
      if (var4.isEmpty()) {
         var6.append(
            "_No divergence was found — over what this pair exercised, and nothing more. An empty divergence set is NOT evidence of fidelity: a pair whose suite never touched the diverging path emits exactly what a pair against a faithful mock emits, which is why the coverage of the claim travels beside the number above._\n\n"
         );
      } else {
         var6.append("## Divergences\n\n");

         for (Map var38 : (Iterable<Map>) (Iterable<?>) (var4)) {
            var6.append("### `")
               .append(var38.get("outcome"))
               .append("` — ")
               .append(var38.get("scenario"))
               .append("\n\n")
               .append(var38.get("detail"))
               .append("\n\n");
            if (var38.get("confirmation") != null) {
               var6.append("_Confirmation: ").append(var38.get("confirmation")).append("._\n\n");
            }

            List var42 = (List)(var38.getOrDefault("differences", List.of()));
            if (!var42.isEmpty()) {
               var6.append("| At | Provider | Mock |\n|---|---|---|\n");

               for (Map var16 : (Iterable<Map>) (Iterable<?>) (var42)) {
                  var6.append("| `")
                     .append(var16.get("at"))
                     .append("` | ")
                     .append(c(var16.get("provider")))
                     .append(" | ")
                     .append(c(var16.get("mock")))
                     .append(" |\n");
               }

               var6.append("\n");
            }
         }
      }

      if (!var5.isEmpty()) {
         var6.append("## Not measured — decided BEFORE anything was compared\n\n");

         for (Map var39 : (Iterable<Map>) (Iterable<?>) (var5)) {
            var6.append("- **")
               .append(var39.get("scenario"))
               .append("** (`")
               .append(var39.get("cause"))
               .append("`) — ")
               .append(var39.get("detail"))
               .append("\n");
         }

         var6.append(
            "\n_These produced no verdict about behaviour. They are neither divergence-free nor divergent, and they travel BESIDE the proportion rather than inside it — folding them in would depress the number exactly as divergence does._\n\n"
         );
      }

      var var35 = a(var0.get("ignored"));
      if (!var35.isEmpty()) {
         var6.append("## Ignored — a waiver register, not a filter\n\n");
         var6.append("_").append(var35.get("note")).append("_\n\n");
         List var40 = (List)(var35.getOrDefault("rules", List.of()));
         if (!var40.isEmpty()) {
            var6.append("| Rule | Scope | Marker | Matched | Reason | Owner |\n|---|---|---|---:|---|---|\n");

            for (Map var46 : (Iterable<Map>) (Iterable<?>) (var40)) {
               var6.append("| `")
                  .append(var46.get("id"))
                  .append("` | `")
                  .append(var46.get("scope") != null ? var46.get("scope") : "key: " + var46.get("key"))
                  .append("` | `")
                  .append(var46.get("marker"))
                  .append("` | ")
                  .append(var46.get("matched"))
                  .append(Boolean.TRUE.equals(var46.get("stale")) ? " ⚠️ stale" : "")
                  .append(" | ")
                  .append(var46.get("reason"))
                  .append(" | ")
                  .append(var46.get("owner"))
                  .append(" |\n");
            }

            var6.append("\n");
         }
      }

      var var41 = a(var0.get("excluded"));
      if (var41.get("note") != null) {
         var6.append("> ").append(var41.get("note")).append("\n\n");
      }

      var var44 = a(var0.get("freshness"));
      if (var44.get("status") != null) {
         var6.append("## Freshness at `")
            .append(var44.get("asOf"))
            .append("`\n\n**")
            .append(String.valueOf(var44.get("status")).toUpperCase())
            .append("** — ")
            .append(var44.get("note"))
            .append("\n\n");
         var var47 = a(var0.get("refresh"));
         if (Boolean.TRUE.equals(var47.get("available"))) {
            var6.append("Refresh it with:\n\n```js\n").append(var47.get("call")).append("\n```\n\n");
         } else if (var47.get("why") != null) {
            var6.append("_No call refreshes this: ").append(var47.get("why")).append("_\n\n");
         }
      }

      return var6.toString();
   }

   private static Map<String, Object> a(Object var0) {
      return var0 instanceof Map var1 ? var1 : Map.of();
   }

   private static List<Map<String, Object>> b(Object var0) {
      return var0 instanceof List var1 ? var1 : List.of();
   }

   private static String c(Object var0) {
      return var0 == null ? "_absent_" : "`" + String.valueOf(var0).replace("|", "\\|").replace("\n", " ") + "`";
   }

   private static String d(Object var0) {
      if (var0 instanceof Number var1) {
         double var2 = var1.doubleValue();
         return (var2 >= 1.0 ? 100 : Math.min(99, (int)Math.floor(var2 * 100.0))) + "%";
      } else {
         return "_not measured_";
      }
   }
}
