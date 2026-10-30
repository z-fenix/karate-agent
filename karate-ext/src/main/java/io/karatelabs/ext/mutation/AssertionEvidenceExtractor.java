package io.karatelabs.ext.mutation;

import io.karatelabs.gherkin.GherkinParser;
import io.karatelabs.gherkin.MatchExpression;
import io.karatelabs.js.Engine;
import io.karatelabs.match.Match;
import io.karatelabs.match.Result;
import io.karatelabs.parser.Node;
import io.karatelabs.parser.NodeType;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class AssertionEvidenceExtractor {
   public static final String STATUS_KIND = "status";
   public static final String MATCH_KIND = "match";
   public static final String REPLAY_CHANNEL = "replay";
   public static final String RUNTIME_CHANNEL = "runtime";
   public static final String NOT_EVALUATED_CHANNEL = "not-evaluated";
   public static final String AUTHORED_CLASS = "authored";
   public static final String RECOMPUTATION_CLASS = "recomputation";
   public static final String CAPTURE_REUSE_CLASS = "capture-reuse";
   public static final String REQUEST_ORIGIN_CLASS = "request-origin";
   public static final String RELATIONAL_CLASS = "relational";
   private static final Pattern ASSIGNMENT_PATTERN = Pattern.compile("(\\w+)\\s*=\\s*(.*)");
   private static final Set<NodeType> LITERAL_NODE_TYPES = Set.of(
      NodeType.LIT_OBJECT,
      NodeType.OBJECT_ELEM,
      NodeType.LIT_ARRAY,
      NodeType.ARRAY_ELEM,
      NodeType.LIT_EXPR,
      NodeType.PAREN_EXPR,
      NodeType.UNARY_EXPR,
      NodeType.EXPR,
      NodeType.EXPR_LIST,
      NodeType.STATEMENT,
      NodeType.EOS
   );
   private static final Pattern REFERENCE_SEGMENT_PATTERN = Pattern.compile("\\.(\\w+)|\\[(\\d+)]|\\['([^']*)']|\\[\"([^\"]*)\"]");

   private AssertionEvidenceExtractor() {
   }

   public static io.karatelabs.ext.mutation.AssertionEvidenceExtractor.EvidenceExtractionResult extract(Path var0) {
      io.karatelabs.ext.common.KarateEventLogParser.KarateEventLogParseResult var1 = io.karatelabs.ext.common.KarateEventLogParser.a(var0);
      ArrayList var2 = new ArrayList();

      for (io.karatelabs.ext.common.KarateEventLogParser.KarateScenarioResult var4 : var1.b()) {
         var2.add(extractFeatureEvidence(var4));
      }

      return new io.karatelabs.ext.mutation.AssertionEvidenceExtractor.EvidenceExtractionResult(var2, var1.c(), var1.d(), var1.e());
   }

   private static io.karatelabs.ext.mutation.AssertionEvidenceExtractor.FeatureEvidence extractFeatureEvidence(io.karatelabs.ext.common.KarateEventLogParser.KarateScenarioResult var0) {
      ArrayList var1 = new ArrayList();
      Map var2 = null;
      int var3 = -1;
      LinkedHashSet var4 = new LinkedHashSet();
      LinkedHashSet var5 = new LinkedHashSet();
      io.karatelabs.ext.mutation.RelationalAssertionAnalyzer.ExchangeContext var6 = null;
      LinkedHashMap var7 = new LinkedHashMap();
      ArrayList var8 = new ArrayList();

      for (io.karatelabs.ext.common.KarateEventLogParser.KarateStepResult var10 : var0.i()) {
         String var11 = var10.d();
         String var12 = var10.e();
         String var13 = var10.a();
         Map var14 = var10.k().isEmpty() ? null : var10.k().get(var10.k().size() - 1);
         boolean var15 = "call".equals(var11) || "callonce".equals(var11);
         boolean var16 = true;
         if ("method".equals(var11)) {
            var3 = var10.b();
            var2 = var14;
         } else if (var15 && var10.l() != 0) {
            var3 = var10.b();
            var2 = null;
         } else if (var14 != null) {
            var3 = var10.b();
            var2 = var14;
         } else {
            var16 = false;
         }

         if (var16) {
            var6 = new io.karatelabs.ext.mutation.RelationalAssertionAnalyzer.ExchangeContext(var3, var8.size(), var2);
         }

         var8.add(new io.karatelabs.ext.mutation.RelationalAssertionAnalyzer.StepExecutionContext(var10, var6));
         switch (var11) {
            case "def":
               Matcher var24 = ASSIGNMENT_PATTERN.matcher(var12);
               if (var24.matches()) {
                  String var25 = var24.group(1);
                  String var21 = var24.group(2);
                  if (var21.startsWith("response")) {
                     var4.add(var25);
                  } else if (var21.contains("Rule.")) {
                     var5.add(var25);
                  }

                  io.karatelabs.ext.mutation.RelationalAssertionAnalyzer.CaptureRelation var22 = io.karatelabs.ext.mutation.RelationalAssertionAnalyzer.analyzeCaptureRelation(var21, var6);
                  if (var22 != null) {
                     var7.put(var25, var22);
                  } else {
                     var7.remove(var25);
                  }
               }
               break;
            case "status":
            case "match":
               io.karatelabs.ext.mutation.AssertionEvidenceExtractor.AssertionEvidence var23 = extractAssertionEvidence(var0, var10, var3, var11, var12, var13, var2, var4, var5, var6, var7, var8);
               if (var23 != null) {
                  var1.add(var23);
               }
               break;
            case "assert":
               String var19 = classifyExpectedSource(var12, var4, var5);
               io.karatelabs.ext.mutation.AssertionEvidenceExtractor.AssertionEvidence var20 = runtimeOnlyEvidence(var0, var10, var3, var13, var19, "assert is arbitrary JS — runtime verdict only");
               var1.add("capture-reuse".equals(var19) ? applyRelationAnalysis(var20, io.karatelabs.ext.mutation.RelationalAssertionAnalyzer.analyzeAssertRelation(var12, var6, var7, var8)) : var20);
               break;
            default:
               if (!"method".equals(var11) && referencesKnownValue(var12, var5)) {
                  var1.add(runtimeOnlyEvidence(var0, var10, var3, var13, "recomputation", "rulebook recomputation — runtime verdict only"));
               }
         }
      }

      return new io.karatelabs.ext.mutation.AssertionEvidenceExtractor.FeatureEvidence(var0.a(), var0.b(), var0.c(), var0.d(), var0.g(), var1);
   }

   private static boolean referencesKnownValue(String var0, Set<String> var1) {
      if (var0.contains("Rule.")) {
         return true;
      } else {
         for (String var3 : var1) {
            if (referencesIdentifier(var0, var3)) {
               return true;
            }
         }

         return false;
      }
   }

   private static io.karatelabs.ext.mutation.AssertionEvidenceExtractor.AssertionEvidence extractAssertionEvidence(
      io.karatelabs.ext.common.KarateEventLogParser.KarateScenarioResult var0,
      io.karatelabs.ext.common.KarateEventLogParser.KarateStepResult var1,
      int var2,
      String var3,
      String var4,
      String var5,
      Map<String, Object> var6,
      Set<String> var7,
      Set<String> var8,
      io.karatelabs.ext.mutation.RelationalAssertionAnalyzer.ExchangeContext var9,
      Map<String, io.karatelabs.ext.mutation.RelationalAssertionAnalyzer.CaptureRelation> var10,
      List<io.karatelabs.ext.mutation.RelationalAssertionAnalyzer.StepExecutionContext> var11
   ) {
      if ("status".equals(var3)) {
         Integer var25 = parseInteger(var4.trim());
         if (var25 == null) {
            return null;
         } else if (var6 == null) {
            return new io.karatelabs.ext.mutation.AssertionEvidenceExtractor.AssertionEvidence(var0.a(), var0.b(), var0.c(), var1.b(), var2, "status", var5, "not-evaluated", null, "authored", "no captured exchange");
         } else {
            int var26 = intValue(responseEnvelope(var6).get("status"));
            return new io.karatelabs.ext.mutation.AssertionEvidenceExtractor.AssertionEvidence(var0.a(), var0.b(), var0.c(), var1.b(), var2, "status", var5, "replay", var26 == var25, "authored", null);
         }
      } else {
         MatchExpression var12;
         try {
            var12 = GherkinParser.parseMatchExpression(var4);
         } catch (RuntimeException var24) {
            return runtimeOnlyEvidence(var0, var1, var2, var5, "authored", "unparseable match expression");
         }

         String var13 = var12.getActualExpr();
         if (!var13.equals("response") && !var13.startsWith("response.") && !var13.startsWith("response[")) {
            String var27 = classifyExpectedSource(var13, var7, var8);
            io.karatelabs.ext.mutation.AssertionEvidenceExtractor.AssertionEvidence var29 = runtimeOnlyEvidence(var0, var1, var2, var5, var27, "actual side is not the captured response");
            return "capture-reuse".equals(var27) ? applyRelationAnalysis(var29, io.karatelabs.ext.mutation.RelationalAssertionAnalyzer.RelationAnalysisResult.refusal("not a relation form: the actual side is not a response path")) : var29;
         } else {
            String var14 = var12.getExpectedExpr();
            if (!isLiteralExpression(var14)) {
               String var28 = classifyExpressionSource(var14, var7, var8);
               io.karatelabs.ext.mutation.AssertionEvidenceExtractor.AssertionEvidence var30 = runtimeOnlyEvidence(
                  var0,
                  var1,
                  var2,
                  var5,
                  var28 == null ? "authored" : var28,
                  var28 == null ? "expected side is not literal-only" : "expected side references " + var28 + " state"
               );
               return "capture-reuse".equals(var28) ? applyRelationAnalysis(var30, io.karatelabs.ext.mutation.RelationalAssertionAnalyzer.analyzeMatchRelation(var13, var12.getMatchTypeName(), var14, var9, var10, var11)) : var30;
            } else if (var6 == null) {
               return new io.karatelabs.ext.mutation.AssertionEvidenceExtractor.AssertionEvidence(var0.a(), var0.b(), var0.c(), var1.b(), var2, "match", var5, "not-evaluated", null, "authored", "no captured exchange");
            } else {
               Object var15 = responseEnvelope(var6).get("body");
               io.karatelabs.ext.mutation.AssertionEvidenceExtractor.ResolvedPath var16 = resolveResponsePath(var15, var13);

               Object var17;
               try {
                  var17 = new Engine().eval("(" + var14 + ")");
               } catch (RuntimeException var23) {
                  return runtimeOnlyEvidence(var0, var1, var2, var5, "authored", "expected side did not evaluate as a literal");
               }

               if (!"#present".equals(var17) && !"#notpresent".equals(var17)) {
                  Match.Type var31;
                  try {
                     var31 = Match.Type.valueOf(var12.getMatchTypeName());
                  } catch (RuntimeException var22) {
                     return runtimeOnlyEvidence(var0, var1, var2, var5, "authored", "unsupported match operator: " + var12.getOperator());
                  }

                  Result var19;
                  try {
                     var19 = Match.execute(new Engine(), var31, var16.a() ? var16.b() : null, var17);
                  } catch (RuntimeException var21) {
                     return runtimeOnlyEvidence(var0, var1, var2, var5, "authored", "match execution failed: " + var21.getMessage());
                  }

                  return replayMatchResult(var0, var1, var2, var5, var19.pass, var16, var6, var17);
               } else {
                  boolean var18 = "#present".equals(var17) == var16.a();
                  return replayMatchResult(var0, var1, var2, var5, var18, var16, var6, var17);
               }
            }
         }
      }
   }

   private static io.karatelabs.ext.mutation.AssertionEvidenceExtractor.AssertionEvidence replayMatchResult(
      io.karatelabs.ext.common.KarateEventLogParser.KarateScenarioResult var0,
      io.karatelabs.ext.common.KarateEventLogParser.KarateStepResult var1,
      int var2,
      String var3,
      boolean var4,
      io.karatelabs.ext.mutation.AssertionEvidenceExtractor.ResolvedPath var5,
      Map<String, Object> var6,
      Object var7
   ) {
      String var8 = "authored";
      if (!var5.c().isEmpty() && var7 != null && !(var7 instanceof String var9 && var9.startsWith("#"))) {
         Object var10 = var6.get("request") instanceof Map var11 ? var11.get("body") : null;
         io.karatelabs.ext.mutation.AssertionEvidenceExtractor.ResolvedPath var13 = resolvePath(var10, var5.c());
         if (var13.a() && valuesEqual(var13.b(), var7)) {
            var8 = "request-origin";
         }
      }

      return new io.karatelabs.ext.mutation.AssertionEvidenceExtractor.AssertionEvidence(var0.a(), var0.b(), var0.c(), var1.b(), var2, "match", var3, "replay", var4, var8, null);
   }

   private static io.karatelabs.ext.mutation.AssertionEvidenceExtractor.AssertionEvidence applyRelationAnalysis(io.karatelabs.ext.mutation.AssertionEvidenceExtractor.AssertionEvidence var0, io.karatelabs.ext.mutation.RelationalAssertionAnalyzer.RelationAnalysisResult var1) {
      return var1.a() == null
         ? new io.karatelabs.ext.mutation.AssertionEvidenceExtractor.AssertionEvidence(var0.c(), var0.d(), var0.e(), var0.f(), var0.g(), var0.h(), var0.i(), var0.j(), var0.k(), var0.l(), var0.m(), -1, null, var1.f())
         : new io.karatelabs.ext.mutation.AssertionEvidenceExtractor.AssertionEvidence(var0.c(), var0.d(), var0.e(), var0.f(), var0.g(), var0.h(), var0.i(), var1.b(), var1.c(), var1.a(), var1.f(), var1.d(), var1.e(), null);
   }

   private static io.karatelabs.ext.mutation.AssertionEvidenceExtractor.AssertionEvidence runtimeOnlyEvidence(io.karatelabs.ext.common.KarateEventLogParser.KarateScenarioResult var0, io.karatelabs.ext.common.KarateEventLogParser.KarateStepResult var1, int var2, String var3, String var4, String var5) {
      String var7 = var1.h();

      Boolean var6 = switch (var7) {
         case "passed" -> Boolean.TRUE;
         case "failed" -> Boolean.FALSE;
         default -> null;
      };
      var7 = var6 == null ? "not-evaluated" : "runtime";
      return new io.karatelabs.ext.mutation.AssertionEvidenceExtractor.AssertionEvidence(var0.a(), var0.b(), var0.c(), var1.b(), var2, "match", var3, var7, var6, var4, var5);
   }

   private static String classifyExpressionSource(String var0, Set<String> var1, Set<String> var2) {
      for (String var4 : var1) {
         if (referencesIdentifier(var0, var4)) {
            return "capture-reuse";
         }
      }

      for (String var6 : var2) {
         if (referencesIdentifier(var0, var6)) {
            return "recomputation";
         }
      }

      return var0.contains("Rule.") ? "recomputation" : null;
   }

   private static String classifyExpectedSource(String var0, Set<String> var1, Set<String> var2) {
      for (String var4 : var1) {
         if (referencesIdentifier(var0, var4)) {
            return "capture-reuse";
         }
      }

      for (String var6 : var2) {
         if (referencesIdentifier(var0, var6)) {
            return "recomputation";
         }
      }

      return "authored";
   }

   static boolean referencesIdentifier(String var0, String var1) {
      return Pattern.compile("\\b" + Pattern.quote(var1) + "\\b").matcher(var0).find();
   }

   static boolean isLiteralExpression(String var0) {
      Node var1;
      try {
         var1 = Engine.parse("(" + var0 + ")");
      } catch (RuntimeException var3) {
         return false;
      }

      return isLiteralNode(var1, true);
   }

   private static boolean isLiteralNode(Node var0, boolean var1) {
      if (var0.isToken()) {
         return true;
      } else if (!var1 && !LITERAL_NODE_TYPES.contains(var0.type)) {
         return false;
      } else {
         for (int var2 = 0; var2 < var0.size(); var2++) {
            if (!isLiteralNode(var0.get(var2), false)) {
               return false;
            }
         }

         return true;
      }
   }

   static io.karatelabs.ext.mutation.AssertionEvidenceExtractor.ResolvedPath resolveResponsePath(Object var0, String var1) {
      ArrayList var2 = new ArrayList();
      if (var1.equals("response")) {
         return new io.karatelabs.ext.mutation.AssertionEvidenceExtractor.ResolvedPath(true, var0, var2);
      } else {
         String var3 = var1.substring("response".length());
         Matcher var4 = REFERENCE_SEGMENT_PATTERN.matcher(var3);
         int var5 = 0;

         while (var4.find()) {
            if (var4.start() != var5) {
               return new io.karatelabs.ext.mutation.AssertionEvidenceExtractor.ResolvedPath(false, null, var2);
            }

            var5 = var4.end();
            String var6 = var4.group(1) != null ? var4.group(1) : (var4.group(3) != null ? var4.group(3) : var4.group(4));
            var2.add(var6 != null ? var6 : Integer.parseInt(var4.group(2)));
         }

         if (var5 != var3.length()) {
            return new io.karatelabs.ext.mutation.AssertionEvidenceExtractor.ResolvedPath(false, null, var2);
         } else {
            io.karatelabs.ext.mutation.AssertionEvidenceExtractor.ResolvedPath var7 = resolvePath(var0, var2);
            return new io.karatelabs.ext.mutation.AssertionEvidenceExtractor.ResolvedPath(var7.a(), var7.b(), var2);
         }
      }
   }

   static io.karatelabs.ext.mutation.AssertionEvidenceExtractor.ResolvedPath resolvePath(Object var0, List<Object> var1) {
      Object var2 = var0;

      for (Object var4 : var1) {
         if (var4 instanceof String var5) {
            if (!(var2 instanceof Map var8 && var8.containsKey(var5))) {
               return new io.karatelabs.ext.mutation.AssertionEvidenceExtractor.ResolvedPath(false, null, var1);
            }

            var2 = var8.get(var5);
         } else {
            int var6 = (Integer)var4;
            if (!(var2 instanceof List var7 && var6 < var7.size())) {
               return new io.karatelabs.ext.mutation.AssertionEvidenceExtractor.ResolvedPath(false, null, var1);
            }

            var2 = var7.get(var6);
         }
      }

      return new io.karatelabs.ext.mutation.AssertionEvidenceExtractor.ResolvedPath(true, var2, var1);
   }

   private static Map<String, Object> responseEnvelope(Map<String, Object> var0) {
      return var0.get("response") instanceof Map var1 ? var1 : Map.of();
   }

   private static boolean valuesEqual(Object var0, Object var1) {
      return var0 instanceof Number var2 && var1 instanceof Number var3 ? var2.doubleValue() == var3.doubleValue() : Objects.equals(var0, var1);
   }

   private static Integer parseInteger(String var0) {
      try {
         return Integer.parseInt(var0);
      } catch (NumberFormatException var2) {
         return null;
      }
   }

   private static int intValue(Object var0) {
      return var0 instanceof Number var1 ? var1.intValue() : -1;
   }

   public record AssertionEvidence(String c, String d, int e, int f, int g, String h, String i, String j, Boolean k, String l, String m, int n, List<String> o, String p) {

      AssertionEvidence(String var1, String var2, int var3, int var4, int var5, String var6, String var7, String var8, Boolean var9, String var10, String var11) {
         this(var1, var2, var3, var4, var5, var6, var7, var8, var9, var10, var11, -1, null, null);
      }


      public String identity() {
         return this.c + "::" + this.d + "#" + this.e + "@step" + this.f + "/x" + this.g + (this.n >= 0 ? "/r" + this.n : "");
      }

      public Map<String, Object> toMap() {
         LinkedHashMap var1 = new LinkedHashMap();
         var1.put("identity", this.identity());
         var1.put("kind", this.h);
         var1.put("text", this.i);
         var1.put("channel", this.j);
         var1.put("pass", this.k);
         var1.put("class", this.l);
         if (this.m != null) {
            var1.put("refusalReason", this.m);
         }

         if (this.o != null) {
            var1.put("axes", this.o);
         }

         if (this.p != null) {
            var1.put("relationRefusal", this.p);
         }

         return var1;
      }

      public String c() {
         return this.c;
      }

      public String d() {
         return this.d;
      }

      public int e() {
         return this.e;
      }

      public int f() {
         return this.f;
      }

      public int g() {
         return this.g;
      }

      public String h() {
         return this.h;
      }

      public String i() {
         return this.i;
      }

      public String j() {
         return this.j;
      }

      public Boolean k() {
         return this.k;
      }

      public String l() {
         return this.l;
      }

      public String m() {
         return this.m;
      }

      public int n() {
         return this.n;
      }

      public List<String> o() {
         return this.o;
      }

      public String p() {
         return this.p;
      }
   }

   record ResolvedPath(boolean a, Object b, List<Object> c) {

   }

   public record EvidenceExtractionResult(List<io.karatelabs.ext.mutation.AssertionEvidenceExtractor.FeatureEvidence> b, int c, int d, int e) {


      public boolean isValid() {
         return this.d == 0 && this.e == 0 && !this.b.isEmpty();
      }

      public List<io.karatelabs.ext.mutation.AssertionEvidenceExtractor.FeatureEvidence> b() {
         return this.b;
      }

      public int c() {
         return this.c;
      }

      public int d() {
         return this.d;
      }

      public int e() {
         return this.e;
      }
   }

   public record FeatureEvidence(String a, String b, int c, String d, String e, List<io.karatelabs.ext.mutation.AssertionEvidenceExtractor.AssertionEvidence> f) {

   }
}
