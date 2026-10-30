package io.karatelabs.ext.lint;

import io.karatelabs.js.Terms;
import java.util.ArrayList;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public final class LintEngine {
   private final Object inputDocument;
   private final ReadOnlyCoverage coverage;
   private final WaiverMatcher waiverMatcher;
   private final io.karatelabs.ext.sourcedoc.SourceDocument sourceDocument;
   private final Long timeoutMillis;
   private final String sourceFormat;
   private boolean coverageAvailable;
   private final List<String> skippedRuleIds = new ArrayList<>();
   private LintSandbox copySandbox;
   private LintSandbox prewrappedSandbox;
   private final Map<LintSandbox, IdentityHashMap<Object, List<Object>>> nodePathCache = new IdentityHashMap<>();
   private static final String RULE_ERROR_MARKER = "\u0000ruleError\u0000";
   private static final List<Object> EMPTY_PATH = List.of();

   private LintEngine(Object var1, ReadOnlyCoverage var2, WaiverMatcher var3, io.karatelabs.ext.sourcedoc.SourceDocument var4, Long var5) {
      this.inputDocument = var1;
      this.coverage = var2;
      this.waiverMatcher = var3;
      this.sourceDocument = var4;
      this.timeoutMillis = var5;
      this.sourceFormat = detectDocumentFormat(var1, var4);
   }

   private static String detectDocumentFormat(Object var0, io.karatelabs.ext.sourcedoc.SourceDocument var1) {
      if (var0 instanceof Map var2) {
         Object var3 = var2.get("openapi");
         if (var3 != null) {
            String var4 = var1 == null ? null : var1.scalarTextAt("/openapi");
            if (var4 == null) {
               var4 = String.valueOf(var3);
            }

            return var4.startsWith("3.0") ? "oas3_0" : "oas3_1";
         } else if (var2.get("swagger") != null) {
            return "oas2";
         } else {
            return var2.get("asyncapi") != null ? "asyncapi2" : null;
         }
      } else {
         return null;
      }
   }

   private boolean supportsFormat(LintRule var1) {
      return var1.i().isEmpty() || this.sourceFormat != null && var1.i().contains(this.sourceFormat);
   }

   public static Map<String, Object> lint(Object var0, List<LintRule> var1, ReadOnlyCoverage var2, WaiverMatcher var3) {
      return lint(var0, var1, var2, var3, null, null);
   }

   public static Map<String, Object> lint(Object var0, List<LintRule> var1, ReadOnlyCoverage var2, WaiverMatcher var3, io.karatelabs.ext.sourcedoc.SourceDocument var4) {
      return lint(var0, var1, var2, var3, var4, null);
   }

   static Map<String, Object> lint(Object var0, List<LintRule> var1, ReadOnlyCoverage var2, WaiverMatcher var3, io.karatelabs.ext.sourcedoc.SourceDocument var4, Long var5) {
      LintEngine var6 = new LintEngine(var0, var2, var3 == null ? io.karatelabs.ext.lint.WaiverMatcher.fromList(List.of()) : var3, var4, var5);

      Map var7;
      try {
         var7 = var6.executeRules(var1);
      } finally {
         var6.closeSandboxes();
      }

      return var7;
   }

   private Map<String, Object> executeRules(List<LintRule> var1) {
      this.coverageAvailable = this.coverage.available();
      ArrayList var2 = new ArrayList();
      ArrayList<java.util.Map<java.lang.String,java.lang.Object>> var3 = new ArrayList();
      ArrayList<io.karatelabs.ext.lint.LintRule> var4 = new ArrayList();

      for (LintRule var6 : var1) {
         if (!this.supportsFormat(var6)) {
            this.skippedRuleIds.add(var6.b());
         } else {
            var4.add(var6);
            io.karatelabs.ext.lint.LintEngine.RuleEvaluationResult var7 = this.evaluateRule(var6);
            var2.addAll(var7.a());
            var3.add(this.describeRuleRun(var6, var7));
         }
      }

      this.waiverMatcher.markStaleWaivers(var4);
      LinkedHashMap<java.lang.String, java.lang.Object> var8 = new LinkedHashMap();
      var8.put("findings", var2);
      var8.put("rules", var3);
      var8.put("waivers", this.waiverMatcher.report());
      var8.put("graph", this.graphStatus(var1));
      var8.put("summary", this.summarizeResults(var4, var2, var3));
      var8.put("capabilities", io.karatelabs.ext.lint.SandboxCapabilityStatement.statement());
      return var8;
   }

   private io.karatelabs.ext.lint.LintEngine.RuleEvaluationResult evaluateRule(LintRule var1) {
      LintSandbox var2 = this.sandbox(var1.n());
      ArrayList<java.util.Map<java.lang.String,java.lang.Object>> var3 = new ArrayList();

      try {
         io.karatelabs.ext.lint.LintEngine.PredicateBatch var4 = var2.runBounded(() -> this.executeRulePredicates(var1, var2));
         LinkedHashSet<java.lang.String> var10 = new LinkedHashSet();

         for (Object[] var7 : var4.a()) {
            int var8 = var3.size();
            this.appendPredicateFinding(var1, var3, (String)var7[0], castToList(var7[1]));
            if (var3.size() > var8 && !Boolean.TRUE.equals(((Map)var3.get(var3.size() - 1)).get("waived"))) {
               var10.add(String.valueOf(var7[2]));
            }
         }

         return new io.karatelabs.ext.lint.LintEngine.RuleEvaluationResult(var3, var4.b(), var10.size(), true);
      } catch (RuntimeException var9) {
         var var5 = this.createFinding(var1, "rule failed to evaluate: " + exceptionMessage(var9), null);
         var5.put("severity", io.karatelabs.ext.lint.FindingSeverity.ERROR.wire());
         var5.put("ruleError", true);
         var3.add(var5);
         return new io.karatelabs.ext.lint.LintEngine.RuleEvaluationResult(var3, 0, 0, false);
      }
   }

   private Map<String, Object> describeRuleRun(LintRule var1, io.karatelabs.ext.lint.LintEngine.RuleEvaluationResult var2) {
      boolean var3 = io.karatelabs.ext.lint.RulePackLoader.usesCoverageGraph(var1) && !this.coverageAvailable;
      LinkedHashMap<java.lang.String, java.lang.Object> var4 = new LinkedHashMap();
      var4.put("id", var1.b());
      var4.put("dimension", var1.j().wire());
      var4.put("severity", var1.e().wire());
      if (var1.k() != null) {
         var4.put("cap", var1.k());
      }

      var4.put("selected", var2.b());
      var4.put("findings", var2.a().size());
      long var5 = var2.a().stream().filter(var0 -> Boolean.TRUE.equals(var0.get("waived"))).count();
      var4.put("waived", (int)var5);
      var4.put("unwaived", var2.a().size() - (int)var5);
      var4.put("failed", var2.c());
      var4.put("graded", var2.d() && !var3);
      if (var3) {
         var4.put("inert", "no project coverage graph was readable — this rule asked nothing");
      }

      return var4;
   }

   private io.karatelabs.ext.lint.LintEngine.PredicateBatch executeRulePredicates(LintRule var1, LintSandbox var2) {
      ArrayList<java.lang.Object[]> var3 = new ArrayList();
      var var4 = this.selectNodes(var1, var2);

      for (int var5 = 0; var5 < var4.size(); var5++) {
         io.karatelabs.ext.lint.LintEngine.NodeSelection var6 = (io.karatelabs.ext.lint.LintEngine.NodeSelection)var4.get(var5);
         String var7 = var6.b == null ? "#" + var5 : io.karatelabs.ext.lint.JsonPathPlusSelector.toJsonPointer(var6.b);

         Object var8;
         try {
            var8 = var2.callPredicate(var1, var6.a, this.createFindingContext(var1, var6));
         } catch (RuntimeException var10) {
            var3.add(new Object[]{"\u0000ruleError\u0000" + exceptionMessage(var10), var6.b, var7});
            continue;
         }

         this.collectPredicateFindings(var8, var6, var7, var3);
      }

      return new io.karatelabs.ext.lint.LintEngine.PredicateBatch(var3, var4.size());
   }

   private Map<String, Object> createFindingContext(LintRule var1, io.karatelabs.ext.lint.LintEngine.NodeSelection var2) {
      LinkedHashMap<java.lang.String, java.lang.Object> var3 = new LinkedHashMap();
      var3.put("path", var2.b);
      var3.put("pointer", var2.b == null ? null : io.karatelabs.ext.lint.JsonPathPlusSelector.toJsonPointer(var2.b));
      var3.put("key", var2.b != null && !var2.b.isEmpty() ? var2.b.get(var2.b.size() - 1) : null);
      var3.put("parent", var2.b != null && !var2.b.isEmpty() ? io.karatelabs.ext.lint.JsonPathPlusSelector.resolvePath(this.sandbox(var1.n()).boundDocument(), var2.b.subList(0, var2.b.size() - 1)) : null);
      var3.put("document", this.sandbox(var1.n()).boundDocument());
      var3.put("params", var1.l());
      var3.put("rule", var1.b());
      var3.put("severity", var1.e().wire());
      var3.put("format", this.sourceFormat);
      var3.put("graph", this.coverageAvailable);
      return var3;
   }

   private List<io.karatelabs.ext.lint.LintEngine.NodeSelection> selectNodes(LintRule var1, LintSandbox var2) {
      if (var1.d() == io.karatelabs.ext.lint.SelectorDialect.JSONPATH_PLUS) {
         ArrayList<io.karatelabs.ext.lint.LintEngine.NodeSelection> var8 = new ArrayList();

         for (io.karatelabs.ext.lint.JsonPathPlusSelector.NodeSelection var10 : io.karatelabs.ext.lint.JsonPathPlusSelector.parse(var1.c()).select(var2.boundDocument(), var2.filterCompiler())) {
            var8.add(new io.karatelabs.ext.lint.LintEngine.NodeSelection(var10.d(), var10.e()));
         }

         return var8;
      } else {
         Object var3 = var2.evaluateSelector(var1, var1.c());
         ArrayList<io.karatelabs.ext.lint.LintEngine.NodeSelection> var4 = new ArrayList();
         if (var3 instanceof Collection) {
            for (Object var7 : (Collection)var3) {
               var4.add(new io.karatelabs.ext.lint.LintEngine.NodeSelection(var7, this.findNodePath(var2, var7)));
            }
         } else if (var3 != null && var3 != Terms.UNDEFINED) {
            var4.add(new io.karatelabs.ext.lint.LintEngine.NodeSelection(var3, this.findNodePath(var2, var3)));
         }

         return var4;
      }
   }

   private List<Object> findNodePath(LintSandbox var1, Object var2) {
      if (!(var2 instanceof Map) && !(var2 instanceof Collection)) {
         return null;
      } else {
         IdentityHashMap<java.lang.Object, java.util.List> var3 = this.nodePathCache.computeIfAbsent(var1, var0 -> {
            IdentityHashMap var1x = new IdentityHashMap();
            indexContainerPaths(var0.boundDocument(), List.of(), var1x);
            return var1x;
         });
         List var4 = (List)var3.get(var2);
         return var4 == EMPTY_PATH ? null : var4;
      }
   }

   private static void indexContainerPaths(Object var0, List<Object> var1, IdentityHashMap<Object, List<Object>> var2) {
      if (var0 instanceof Map || var0 instanceof Collection) {
         if (var2.putIfAbsent(var0, var1) != null) {
            markContainersOnCycle(var0, var2);
         } else {
            if (var0 instanceof Map var3) {
               for (Entry var5 : (Iterable<Entry>) (Iterable<?>) (var3.entrySet())) {
                  indexContainerPaths(var5.getValue(), appendPath(var1, String.valueOf(var5.getKey())), var2);
               }
            } else {
               int var7 = 0;

               for (Object var6 : (Collection)var0) {
                  indexContainerPaths(var6, appendPath(var1, var7++), var2);
               }
            }
         }
      }
   }

   private static void markContainersOnCycle(Object var0, IdentityHashMap<Object, List<Object>> var1) {
      if (var0 instanceof Map || var0 instanceof Collection) {
         if (var1.get(var0) != EMPTY_PATH) {
            var1.put(var0, EMPTY_PATH);
            if (var0 instanceof Map var2) {
               var2.values().forEach(var1x -> markContainersOnCycle(var1x, var1));
            } else {
               ((Collection)var0).forEach(var1x -> markContainersOnCycle(var1x, var1));
            }
         }
      }
   }

   private void collectPredicateFindings(Object var1, io.karatelabs.ext.lint.LintEngine.NodeSelection var2, String var3, List<Object[]> var4) {
      if (Terms.isTruthy(var1)) {
         if (!(var1 instanceof Collection)) {
            if (var1 instanceof Map var8) {
               Object var9 = var8.get("message");
               Object var10 = var8.get("path");
               var4.add(new Object[]{var9 == null ? null : String.valueOf(var9), appendNodePaths(var2.b, var10), var3});
            } else {
               var4.add(new Object[]{Boolean.TRUE.equals(var1) ? null : String.valueOf(var1), var2.b, var3});
            }
         } else {
            for (Object var7 : (Collection)var1) {
               this.collectPredicateFindings(var7, var2, var3, var4);
            }
         }
      }
   }

   private void appendPredicateFinding(LintRule var1, List<Map<String, Object>> var2, String var3, List<Object> var4) {
      if (var3 != null && var3.startsWith("\u0000ruleError\u0000")) {
         var var8 = this.createFinding(var1, "rule failed on this node: " + var3.substring("\u0000ruleError\u0000".length()), var4);
         var8.put("severity", io.karatelabs.ext.lint.FindingSeverity.ERROR.wire());
         var8.put("ruleError", true);
         var2.add(var8);
      } else {
         var var5 = this.createFinding(var1, var3, var4);
         String var6 = var4 == null ? null : io.karatelabs.ext.lint.JsonPathPlusSelector.toJsonPointer(var4);
         io.karatelabs.ext.lint.WaiverMatcher.WaiverMatch var7 = this.waiverMatcher.isEmpty() ? null : this.waiverMatcher.findMatch(var1, var6 == null ? "" : var6);
         if (var7 != null) {
            var5.put("waived", true);
            var5.put("waiver", var7.toMap());
         }

         var2.add(var5);
      }
   }

   private Map<String, Object> createFinding(LintRule var1, String var2, List<Object> var3) {
      LinkedHashMap<java.lang.String, java.lang.Object> var4 = new LinkedHashMap();
      var4.put("rule", var1.b());
      var4.put("severity", var1.e().wire());
      var4.put("message", var2 != null ? var2 : (var1.f() != null ? var1.f() : (var1.g() != null ? var1.g() : "violates '" + var1.b() + "'")));
      if (var3 != null) {
         var4.put("path", var3);
         var4.put("pointer", io.karatelabs.ext.lint.JsonPathPlusSelector.toJsonPointer(var3));
         this.addSourceLocation(var4, var1, io.karatelabs.ext.lint.JsonPathPlusSelector.toJsonPointer(var3));
      }

      if (var1.h() != null) {
         var4.put("documentationUrl", var1.h());
      }

      return var4;
   }

   private void addSourceLocation(Map<String, Object> var1, LintRule var2, String var3) {
      if (this.sourceDocument != null) {
         io.karatelabs.ext.sourcedoc.SourceDocument.ResolvedLocation var4 = this.sourceDocument.resolveLocation(var3, var2.m());
         if (var4 != null) {
            var1.put("line", var4.span().line());
            var1.put("col", var4.span().column());
            var1.put("position", var4.toMap());
         }
      }
   }

   private Map<String, Object> summarizeResults(List<LintRule> var1, List<Map<String, Object>> var2, List<Map<String, Object>> var3) {
      int var4 = 0;
      int var5 = 0;
      int var6 = 0;
      int var7 = 0;
      LinkedHashMap<java.lang.String, java.lang.Integer> var8 = new LinkedHashMap();

      for (Map var10 : var2) {
         var8.merge(String.valueOf(var10.get("rule")), 1, Integer::sum);
         if (Boolean.TRUE.equals(var10.get("waived"))) {
            var7++;
         } else {
            String var11 = String.valueOf(var10.get("severity"));
            switch (var11) {
               case "error":
                  var4++;
                  break;
               case "warn":
                  var5++;
                  break;
               default:
                  var6++;
            }
         }
      }

      LinkedHashMap<java.lang.String, java.lang.Object> var13 = new LinkedHashMap();
      var13.put("rules", var1.size());
      var13.put("errors", var4);
      var13.put("warnings", var5);
      var13.put("infos", var6);
      var13.put("waived", var7);
      var13.put("rejectedWaivers", this.waiverMatcher.rejectedCount());
      var13.put("total", var2.size());
      var13.put("passed", var4 == 0);
      var13.put("clean", var2.isEmpty() && var7 == 0);
      var13.put("byRule", var8);
      ArrayList<java.lang.String> var14 = new ArrayList();

      for (Map var17 : var3) {
         if (var17.get("inert") == null && Integer.valueOf(0).equals(var17.get("selected"))) {
            var14.add(String.valueOf(var17.get("id")));
         }
      }

      if (!var14.isEmpty()) {
         LinkedHashMap<java.lang.String, java.lang.Object> var16 = new LinkedHashMap();
         var16.put("rules", var14);
         var16.put(
            "note",
            var14.size()
               + " rule(s) selected NO node in this document, so they neither passed nor failed — they are counted inert in the grade rather than scored a perfect mark for never asking. Two causes look identical from here: the construct they look at is genuinely absent, or the selector is wrong (a stray character in a `given` reads as a clean document). Check the rule's `given` against `rules[].selected`."
         );
         var13.put("selectedNothing", var16);
      }

      return var13;
   }

   private Map<String, Object> graphStatus(List<LintRule> var1) {
      ArrayList<java.lang.String> var2 = new ArrayList();

      for (LintRule var4 : var1) {
         if (io.karatelabs.ext.lint.RulePackLoader.usesCoverageGraph(var4)) {
            var2.add(var4.b());
         }
      }

      boolean var5 = !var2.isEmpty() && this.coverageAvailable;
      LinkedHashMap<java.lang.String, java.lang.Object> var6 = new LinkedHashMap();
      var6.put("available", var5);
      var6.put("graphReachingRules", var2);
      if (!this.skippedRuleIds.isEmpty()) {
         var6.put("skippedByFormat", List.copyOf(this.skippedRuleIds));
      }

      if (!var2.isEmpty() && !var5) {
         var6.put(
            "note",
            "no project coverage graph was readable, so these rules asked nothing and raised nothing — that is INERT, not clean. Call Report.aggregate() to write the graph (with no runs yet it still renders the declared universe); for real evidence, run your suite (Runner.suite('checks')) or probe inside a live run (Harness.startLive() → Http.* → Harness.stop()) — on the console each settled run folds into the graph."
         );
      }

      return var6;
   }

   private LintSandbox sandbox(boolean var1) {
      if (var1) {
         if (this.prewrappedSandbox == null) {
            this.prewrappedSandbox = io.karatelabs.ext.lint.LintSandbox.createForPrewrappedDocument(io.karatelabs.ext.lint.LocalRefResolver.resolve(this.inputDocument), this.coverage, this.timeoutMillis);
         }

         return this.prewrappedSandbox;
      } else {
         if (this.copySandbox == null) {
            this.copySandbox = io.karatelabs.ext.lint.LintSandbox.createForPrewrappedDocument(io.karatelabs.ext.common.SandboxExecutor.b(this.inputDocument), this.coverage, this.timeoutMillis);
         }

         return this.copySandbox;
      }
   }

   private void closeSandboxes() {
      if (this.copySandbox != null) {
         this.copySandbox.close();
      }

      if (this.prewrappedSandbox != null) {
         this.prewrappedSandbox.close();
      }
   }

   private static List<Object> castToList(Object var0) {
      return (List<Object>)var0;
   }

   private static List<Object> appendNodePaths(List<Object> var0, Object var1) {
      if (var1 != null && var0 != null) {
         var var2 = new ArrayList<>(var0);
         if (var1 instanceof Collection var3) {
            var2.addAll(var3);
         } else {
            var2.add(var1);
         }

         return List.copyOf(var2);
      } else {
         return var0;
      }
   }

   private static List<Object> appendPath(List<Object> var0, Object var1) {
      ArrayList<java.lang.Object> var2 = new ArrayList(var0.size() + 1);
      var2.addAll(var0);
      var2.add(var1);
      return List.copyOf(var2);
   }

   private static String exceptionMessage(RuntimeException var0) {
      return var0.getMessage() == null ? var0.getClass().getSimpleName() : var0.getMessage();
   }

   private record RuleEvaluationResult(List<Map<String, Object>> a, int b, int c, boolean d) {

   }

   private record PredicateBatch(List<Object[]> a, int b) {

   }

   private record NodeSelection(Object a, List<Object> b) {

   }
}
