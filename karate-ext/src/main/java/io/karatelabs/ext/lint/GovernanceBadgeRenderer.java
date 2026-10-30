package io.karatelabs.ext.lint;

import java.util.Locale;
import java.util.Map;

public final class GovernanceBadgeRenderer {
   private static final String LABEL = "governance";
   private static final String PASS_COLOR = "#4c1";
   private static final String WARN_COLOR = "#dfb317";
   private static final String FAIL_COLOR = "#e05d44";
   private static final double CHAR_WIDTH = 6.5;
   private static final int f = 10;

   private GovernanceBadgeRenderer() {
   }

   public static String render(Map<String, Object> var0) {
      String var1 = levelName(var0);
      String var2 = stringValue(var0.get("verdict"));

      String var3 = switch (var2) {
         case "READY" -> "#4c1";
         case "CONDITIONAL" -> "#dfb317";
         default -> "#e05d44";
      };
      int var4 = measureTextWidth("governance");
      int var8 = measureTextWidth(var1);
      int var6 = var4 + var8;
      String var7 = "governance: " + var1 + " · index " + formatIndex(var0.get("index")) + " · " + var2;
      return "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"%d\" height=\"20\" role=\"img\" aria-label=\"%s\"><title>%s</title><linearGradient id=\"s\" x2=\"0\" y2=\"100%%\"><stop offset=\"0\" stop-color=\"#bbb\" stop-opacity=\".1\"/><stop offset=\"1\" stop-opacity=\".1\"/></linearGradient><g shape-rendering=\"crispEdges\"><rect width=\"%d\" height=\"20\" fill=\"#555\"/><rect x=\"%d\" width=\"%d\" height=\"20\" fill=\"%s\"/></g><rect width=\"%d\" height=\"20\" fill=\"url(#s)\"/><g fill=\"#fff\" text-anchor=\"middle\" font-family=\"Verdana,DejaVu Sans,Geneva,sans-serif\" font-size=\"11\"><text x=\"%d\" y=\"14\">%s</text><text x=\"%d\" y=\"14\">%s</text></g></svg>\n"
         .formatted(var6, escapeXml(var7), escapeXml(var7), var4, var4, var8, var3, var6, var4 / 2, escapeXml("governance"), var4 + var8 / 2, escapeXml(var1));
   }

   private static String levelName(Map<String, Object> var0) {
      Object var2 = var0.get("level") instanceof Map var3 ? var3.get("name") : null;
      return var2 == null ? io.karatelabs.ext.lint.GovernanceScorer.GovernanceMaturityLevel.UNGOVERNED.label() : String.valueOf(var2);
   }

   private static String formatIndex(Object var0) {
      return var0 instanceof Number var1 ? String.format(Locale.ROOT, "%.2f", var1.doubleValue()) : "not measured";
   }

   private static int measureTextWidth(String var0) {
      return (int)Math.ceil(var0.length() * 6.5) + 20;
   }

   private static String stringValue(Object var0) {
      return var0 == null ? "" : String.valueOf(var0);
   }

   private static String escapeXml(String var0) {
      return var0.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&apos;");
   }
}
