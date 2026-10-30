package io.karatelabs.ext.rules;

import io.karatelabs.ext.rules.twin.Twin;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

final class RuleAnalysisReportGenerator {
   private static final String a = "io/karatelabs/ext/max/web/pub/json-table.js";
   private static final String b = "io/karatelabs/ext/max/web/pub/lifecycle-pane.js";
   private static final String c = "META-INF/karate-ext/static/rule-analysis-report.js";
   private static final String d = "META-INF/karate-ext/static/rule-analysis-report.css";
   private static final String e = "META-INF/karate-ext/static/help-affordance.js";

   private RuleAnalysisReportGenerator() {
   }

   static Map<String, Object> a(
      io.karatelabs.ext.rules.RulebookRepository.RulebookDocument var0,
      RuleEvaluator var1,
      Map<String, Object> var2,
      Twin var3,
      Map<String, Object> var4,
      Object var5,
      List<Map<String, Object>> var6,
      List<Map<String, Object>> var7,
      List<Map<String, Object>> var8
   ) {
      var var9 = a(var0, var1, var3, var4, var5, var2 == null ? null : var2.get("sutRevision"));
      if (var6 != null && !var6.isEmpty()) {
         var9.put("match", var6);
         var9.put("rowsMax", Rule.tableRowsMax());
      }

      if (var7 != null && !var7.isEmpty()) {
         var9.put("editions", var7);
      }

      if (var8 != null && !var8.isEmpty()) {
         var9.put("tables", var8);
      }

      String var10 = a(var9);
      Path var11 = a(var0.d(), var2);

      try {
         Files.createDirectories(var11.getParent());
         Files.writeString(var11, var10, StandardCharsets.UTF_8);
      } catch (IOException var13) {
         throw new UncheckedIOException(var13);
      }

      LinkedHashMap<java.lang.String, java.lang.Object> var12 = new LinkedHashMap();
      var12.put("file", var11.toString());
      var12.put("url", var11.toUri().toString());
      var12.put("rule", var0.d());
      return var12;
   }

   static Map<String, Object> a(io.karatelabs.ext.rules.RulebookRepository.RulebookDocument var0, RuleEvaluator var1) {
      return a(var0, var1, null, null);
   }

   static Map<String, Object> a(io.karatelabs.ext.rules.RulebookRepository.RulebookDocument var0, RuleEvaluator var1, Twin var2) {
      return a(var0, var1, var2, null);
   }

   static Map<String, Object> a(io.karatelabs.ext.rules.RulebookRepository.RulebookDocument var0, RuleEvaluator var1, Twin var2, Map<String, Object> var3) {
      return a(var0, var1, var2, var3, null, null);
   }

   static Map<String, Object> a(io.karatelabs.ext.rules.RulebookRepository.RulebookDocument var0, RuleEvaluator var1, Twin var2, Map<String, Object> var3, Object var4, Object var5) {
      List<Map<String, Object>> var6 = var0.b() == null ? List.of() : var0.b();
      io.karatelabs.ext.rules.RuleInputSpaceExplorer.InputSpaceExplorationResult var7 = RuleInputSpaceExplorer.a(var0.g(), var0.m().e(), var0.e(), var0.j(), var6, "coverage", null, 512, 2, null);
      var var8 = var7.a();
      List var9 = (List)var8.get("suite");
      var var10 = b(var8);
      var var11 = c(var8);
      Map<java.lang.String, java.lang.Object> var12 = RuleCoverageMatrixBuilder.a(var0.g(), var0.m().e(), var0.e(), var6, var9, var1, var10, var11);
      var12.put("coverage", var8.get("coverage"));
      var12.put("notcovered", var8.get("notcovered"));
      var var13 = RuleAnalysisEngine.a(var0, "coverage");
      var var14 = RuleAnalysisEngine.a(var0);
      var var15 = RuleAnalysisEngine.a(var0, var1, null);
      ArrayList<java.util.Map> var16 = new ArrayList();

      for (Object var18 : (Iterable<Object>)(Iterable<?>)(var13.getOrDefault("dimensions", List.of()))) {
         Map<java.lang.String, java.util.List> var19 = (Map)var18;
         List var20 = (List)var19.get("classes");
         if (var20 != null && var20.size() > 1) {
            LinkedHashMap<java.lang.String, java.lang.Object> var21 = new LinkedHashMap();
            var21.put("dim", var19.get("path"));
            Map<java.lang.String, Object> var22 = RuleAnalysisEngine.a(var0, var1, var21);
            if (var22.get("points") != null) {
               var16.add(var22);
            }
         }
      }

      LinkedHashMap<java.lang.String, java.lang.Object> var23 = new LinkedHashMap();
      var23.put("output", var15.get("output"));
      var23.put("base", var15.get("base"));
      var23.put("tornado", var15.getOrDefault("tornado", List.of()));
      var23.put("sweeps", var16);
      LinkedHashMap<java.lang.String, java.lang.Object> var24 = new LinkedHashMap();
      var24.put("rule", var0.d());
      var24.put("generator", var0.j() != null);
      var24.put("table", var12);
      var24.put("ranges", var13);
      var24.put("check", var14);
      var24.put("whatif", var23);
      if (var2 != null) {
         var24.put("twin", var2.section(var0.d(), var4, var5));
         if (var0.k() != null) {
            var24.put("twinDocs", var2.lifecycle(var0.d(), var4));
         }
      }

      if (var3 != null && var3.get("error") == null) {
         var24.put("mutation", var3);
      }

      if (!var0.m().a()) {
         CellCoverageAnalyzer var25 = new CellCoverageAnalyzer(var0, var1);
         List<io.karatelabs.ext.rules.CellCoverageAnalyzer.RowCoverageResult> var26 = var25.a(var6, (java.util.Set<String>)null);
         Map var27 = var25.a(var26, var25.a());
         if (!var27.isEmpty()) {
            var24.put("cells", var25.a(var26, var27, Rule.tableRowsMax()));
            if (var24.get("mutation") != null) {
               var24.put("mutationReads", a(var3, var27));
            }
         }
      }

      var24.put("summary", var7.c());
      return var24;
   }

   private static Map<String, Object> a(Map<String, Object> var0, Map<String, io.karatelabs.ext.rules.CellCoverageAnalyzer.CellCoverage> var1) {
      LinkedHashMap<java.lang.String, java.lang.Object> var2 = new LinkedHashMap();
      Object var4 = var0.get("worklist");
      if (var4 instanceof List) {
         for (Object var5 : (List)var4) {
            if (var5 instanceof Map var6 && var6.get("site") instanceof String var7 && var7.startsWith("lookup.")) {
               io.karatelabs.ext.rules.CellCoverageAnalyzer.CellCoverage var10 = (io.karatelabs.ext.rules.CellCoverageAnalyzer.CellCoverage)var1.get(var7.substring("lookup.".length()));
               if (var10 != null) {
                  var2.put(var10.b().a(), !"unread".equals(var10.c()));
               }
            }
         }
      }

      return var2;
   }

   private static String a(Map<String, Object> var0) {
      return io.karatelabs.ext.common.ReportHtmlBuilder.a(
         "Rule Analysis — " + var0.get("rule"),
         List.of("io/karatelabs/ext/max/web/pub/tailwind.css", "META-INF/karate-ext/static/rule-analysis-report.css"),
         "KARATE_RULE_ANALYSIS_DATA",
         var0,
         List.of(
            "io/karatelabs/ext/max/web/pub/json-table.js",
            "META-INF/karate-ext/static/help-affordance.js",
            "io/karatelabs/ext/max/web/pub/lifecycle-pane.js",
            "META-INF/karate-ext/static/rule-analysis-report.js"
         )
      );
   }

   private static Path a(String var0, Map<String, Object> var1) {
      Object var2 = var1 == null ? null : var1.get("out");
      if (var2 == null) {
         return Path.of("target", "rule-analysis", var0 + "-rule-analysis.html");
      } else {
         Path var3 = Path.of(String.valueOf(var2));
         return !Files.isDirectory(var3) && !String.valueOf(var2).endsWith("/") ? var3 : var3.resolve(var0 + "-rule-analysis.html");
      }
   }

   private static List<String> b(Map<String, Object> var0) {
      if (var0.get("dimensions") instanceof List var1) {
         ArrayList<java.lang.String> var6 = new ArrayList();

         for (Object var4 : var1) {
            if (var4 instanceof Map var5 && !"array".equals(var5.get("type"))) {
               var6.add(String.valueOf(var5.get("path")));
            }
         }

         return var6;
      } else {
         return null;
      }
   }

   private static Map<Integer, String> c(Map<String, Object> var0) {
      LinkedHashMap<java.lang.Integer, java.lang.String> var1 = new LinkedHashMap();
      if (var0.get("labels") instanceof Map var2) {
         for (Entry var4 : (Iterable<Entry>) (Iterable<?>) (var2.entrySet())) {
            try {
               var1.put(Integer.parseInt(String.valueOf(var4.getKey())), String.valueOf(var4.getValue()));
            } catch (NumberFormatException var6) {
            }
         }
      }

      return var1;
   }
}
