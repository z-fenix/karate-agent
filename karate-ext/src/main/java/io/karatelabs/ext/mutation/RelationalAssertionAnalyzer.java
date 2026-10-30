package io.karatelabs.ext.mutation;

import io.karatelabs.js.Engine;
import io.karatelabs.match.Match;
import io.karatelabs.parser.Node;
import io.karatelabs.parser.NodeType;
import io.karatelabs.parser.TokenType;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class RelationalAssertionAnalyzer {
   static final String PARTIAL_ORACLE_WARNING = "partial oracle: a mutant that moves both sides of a relation consistently escapes";
   private static final String RESPONSE_ROOT = "response";
   private static final Set<String> MUTATING_STEPS = Set.of("def", "set", "copy", "text", "json", "xml", "xmlstring", "string", "yaml", "csv", "table", "replace", "remove");
   private static final Pattern DECLARATION_PATTERN = Pattern.compile("(\\w+)(?:[.\\[][^=]*)?\\s*=?\\s*(.*)", 32);
   private static final Pattern RESPONSE_REFERENCE_PATTERN = Pattern.compile("\\bresponse\\w*|\\$|karate\\.get\\s*\\(");
   private static final Pattern MUTATION_CALL_PATTERN = Pattern.compile("karate\\.(set|configure)\\s*\\(");
   private static final Object ABSENT_MARKER = new Object();
   private static final Set<TokenType> MATH_OPERATORS = Set.of(TokenType.PLUS, TokenType.MINUS, TokenType.STAR, TokenType.SLASH, TokenType.PERCENT, TokenType.STAR_STAR);
   private static final Set<TokenType> COMPARISON_OPERATORS = Set.of(
      TokenType.LT, TokenType.GT, TokenType.LT_EQ, TokenType.GT_EQ, TokenType.EQ_EQ, TokenType.EQ_EQ_EQ, TokenType.NOT_EQ, TokenType.NOT_EQ_EQ
   );
   private static final Set<TokenType> LOGICAL_OPERATORS = Set.of(TokenType.AMP_AMP, TokenType.PIPE_PIPE);
   private static final Set<TokenType> LITERAL_TOKEN_TYPES = Set.of(TokenType.NUMBER, TokenType.S_STRING, TokenType.D_STRING, TokenType.TRUE, TokenType.FALSE, TokenType.NULL);

   private RelationalAssertionAnalyzer() {
   }

   static io.karatelabs.ext.mutation.RelationalAssertionAnalyzer.CaptureRelation analyzeCaptureRelation(String var0, io.karatelabs.ext.mutation.RelationalAssertionAnalyzer.ExchangeContext var1) {
      if (var1 == null) {
         return null;
      } else {
         Node var2 = parseExpression(var0);
         LinkedHashSet var3 = new LinkedHashSet();
         if (var2 != null && validateExpression(var2, true, var3) == null && var3.equals(Set.of("response"))) {
            io.karatelabs.ext.mutation.RelationalAssertionAnalyzer.ExpressionPath var4 = parsePath(var2);
            return new io.karatelabs.ext.mutation.RelationalAssertionAnalyzer.CaptureRelation(var2, var4 != null ? var4.b() : null, var1);
         } else {
            return null;
         }
      }
   }

   static io.karatelabs.ext.mutation.RelationalAssertionAnalyzer.RelationAnalysisResult analyzeAssertRelation(String var0, io.karatelabs.ext.mutation.RelationalAssertionAnalyzer.ExchangeContext var1, Map<String, io.karatelabs.ext.mutation.RelationalAssertionAnalyzer.CaptureRelation> var2, List<io.karatelabs.ext.mutation.RelationalAssertionAnalyzer.StepExecutionContext> var3) {
      return buildRelation((io.karatelabs.ext.mutation.RelationalAssertionAnalyzer.ExpressionPath)null, null, var0, var1, var2, var3);
   }

   static io.karatelabs.ext.mutation.RelationalAssertionAnalyzer.RelationAnalysisResult analyzeMatchRelation(
      String var0, String var1, String var2, io.karatelabs.ext.mutation.RelationalAssertionAnalyzer.ExchangeContext var3, Map<String, io.karatelabs.ext.mutation.RelationalAssertionAnalyzer.CaptureRelation> var4, List<io.karatelabs.ext.mutation.RelationalAssertionAnalyzer.StepExecutionContext> var5
   ) {
      Node var6 = parseExpression(var0);
      io.karatelabs.ext.mutation.RelationalAssertionAnalyzer.ExpressionPath var7 = var6 == null ? null : parsePath(var6);
      if (var7 == null || !"response".equals(var7.a())) {
         return io.karatelabs.ext.mutation.RelationalAssertionAnalyzer.RelationAnalysisResult.refusal("not a relation form: the actual side is not a response path");
      } else {
         return !"EQUALS".equals(var1) && !"NOT_EQUALS".equals(var1)
            ? io.karatelabs.ext.mutation.RelationalAssertionAnalyzer.RelationAnalysisResult.refusal("not a relation form: only == and != relate two responses")
            : buildRelation(var7, var1, var2, var3, var4, var5);
      }
   }

   private static io.karatelabs.ext.mutation.RelationalAssertionAnalyzer.RelationAnalysisResult buildRelation(
      io.karatelabs.ext.mutation.RelationalAssertionAnalyzer.ExpressionPath var0,
      String var1,
      String var2,
      io.karatelabs.ext.mutation.RelationalAssertionAnalyzer.ExchangeContext var3,
      Map<String, io.karatelabs.ext.mutation.RelationalAssertionAnalyzer.CaptureRelation> var4,
      List<io.karatelabs.ext.mutation.RelationalAssertionAnalyzer.StepExecutionContext> var5
   ) {
      try {
         if (var2.contains("#(")) {
            throw new io.karatelabs.ext.mutation.RelationalAssertionAnalyzer.RelationShapeException("not a relation form: an embedded #(...)");
         } else {
            Node var6 = parseExpression(var2);
            if (var6 == null) {
               throw new io.karatelabs.ext.mutation.RelationalAssertionAnalyzer.RelationShapeException("not a relation form: a compound expression");
            } else {
               LinkedHashSet var7 = new LinkedHashSet();
               String var8 = validateExpression(var6, false, var7);
               if (var8 != null) {
                  throw new io.karatelabs.ext.mutation.RelationalAssertionAnalyzer.RelationShapeException("not a relation form: " + var8);
               } else {
                  for (String var10 : (Iterable<String>) (Iterable<?>) (var7)) {
                     if (!"response".equals(var10) && !var4.containsKey(var10)) {
                        throw new io.karatelabs.ext.mutation.RelationalAssertionAnalyzer.RelationShapeException("reads " + var10 + ", which is not a relation variable");
                     }
                  }

                  if (var3 == null) {
                     throw new io.karatelabs.ext.mutation.RelationalAssertionAnalyzer.RelationShapeException("no bound exchange");
                  } else {
                     io.karatelabs.ext.mutation.RelationalAssertionAnalyzer.ExchangeContext var19 = null;
                     boolean var20 = var0 != null;

                     for (String var12 : (Iterable<String>) (Iterable<?>) (var7)) {
                        io.karatelabs.ext.mutation.RelationalAssertionAnalyzer.CaptureRelation var13 = (io.karatelabs.ext.mutation.RelationalAssertionAnalyzer.CaptureRelation)var4.get(var12);
                        if (var13 != null && var13.c().b() != var3.b()) {
                           if (var19 != null && var19.b() != var13.c().b()) {
                              throw new io.karatelabs.ext.mutation.RelationalAssertionAnalyzer.RelationShapeException("relates more than two exchanges");
                           }

                           var19 = var13.c();
                        } else {
                           var20 = true;
                        }
                     }

                     if (var19 != null && var20) {
                        assertNoCrossExchangeDataFlow(var19, var3, var5);
                        if (var19.c() != null && var3.c() != null) {
                           var var21 = diffRequests(requestForExchange(var19), requestForExchange(var3));
                           ArrayList<io.karatelabs.ext.mutation.RelationalAssertionAnalyzer.ResponseReadSite> var22 = new ArrayList();
                           if (var0 != null) {
                              var22.add(new io.karatelabs.ext.mutation.RelationalAssertionAnalyzer.ResponseReadSite(var3, var0.b()));
                           }

                           collectReadSites(var6, var3, var4, var22);
                           ArrayList<java.lang.Object> var23 = new ArrayList();

                           for (io.karatelabs.ext.mutation.RelationalAssertionAnalyzer.ResponseReadSite var15 : (Iterable<io.karatelabs.ext.mutation.RelationalAssertionAnalyzer.ResponseReadSite>) (Iterable<?>) (var22)) {
                              if (var15.a().b() == var19.b()) {
                                 io.karatelabs.ext.mutation.AssertionEvidenceExtractor.ResolvedPath var16 = io.karatelabs.ext.mutation.AssertionEvidenceExtractor.resolvePath(responseBodyForExchange(var19), var15.b());
                                 if (var16.a() && var16.b() != null) {
                                    var23.add(var16.b());
                                 }
                              }
                           }

                           for (Entry var26 : (Iterable<Entry>) (Iterable<?>) (var21.entrySet())) {
                              for (Object var17 : var23) {
                                 if (var26.getValue() != ABSENT_MARKER && containsValue(var26.getValue(), var17)) {
                                    throw new io.karatelabs.ext.mutation.RelationalAssertionAnalyzer.RelationShapeException("carries a value read off the first response into " + (String)var26.getKey());
                                 }
                              }
                           }

                           boolean var25 = true;

                           for (io.karatelabs.ext.mutation.RelationalAssertionAnalyzer.ResponseReadSite var30 : (Iterable<io.karatelabs.ext.mutation.RelationalAssertionAnalyzer.ResponseReadSite>) (Iterable<?>) (var22)) {
                              var25 &= readComesFromRequest(var30);
                           }

                           String var28 = var25 ? "request-origin" : "relational";
                           boolean var31 = evaluateRelationMatch(var0, var1, var6, var7, var3, var4);
                           return new io.karatelabs.ext.mutation.RelationalAssertionAnalyzer.RelationAnalysisResult(var28, "replay", var31, var19.a(), List.copyOf(var21.keySet()), null);
                        } else {
                           return new io.karatelabs.ext.mutation.RelationalAssertionAnalyzer.RelationAnalysisResult("relational", "not-evaluated", null, var19.a(), null, "a relation exchange was never captured");
                        }
                     } else {
                        throw new io.karatelabs.ext.mutation.RelationalAssertionAnalyzer.RelationShapeException("relates one exchange to itself");
                     }
                  }
               }
            }
         }
      } catch (io.karatelabs.ext.mutation.RelationalAssertionAnalyzer.RelationShapeException var18) {
         return io.karatelabs.ext.mutation.RelationalAssertionAnalyzer.RelationAnalysisResult.refusal(var18.getMessage());
      }
   }

   private static void assertNoCrossExchangeDataFlow(io.karatelabs.ext.mutation.RelationalAssertionAnalyzer.ExchangeContext var0, io.karatelabs.ext.mutation.RelationalAssertionAnalyzer.ExchangeContext var1, List<io.karatelabs.ext.mutation.RelationalAssertionAnalyzer.StepExecutionContext> var2) {
      int var3 = -1;

      for (int var4 = 0; var4 < var1.b() && var4 < var2.size(); var4++) {
         if ("method".equals(((io.karatelabs.ext.mutation.RelationalAssertionAnalyzer.StepExecutionContext)var2.get(var4)).a().d())) {
            var3 = var4;
         }
      }

      HashSet<java.lang.String> var13 = new HashSet();

      for (int var5 = var0.b(); var5 <= var1.b() && var5 < var2.size(); var5++) {
         io.karatelabs.ext.common.KarateEventLogParser.KarateStepResult var6 = ((io.karatelabs.ext.mutation.RelationalAssertionAnalyzer.StepExecutionContext)var2.get(var5)).a();
         String var7 = var6.d();
         String var8 = var6.e() == null ? "" : var6.e();
         boolean var9 = ((io.karatelabs.ext.mutation.RelationalAssertionAnalyzer.StepExecutionContext)var2.get(var5)).b() != null && ((io.karatelabs.ext.mutation.RelationalAssertionAnalyzer.StepExecutionContext)var2.get(var5)).b().b() == var0.b();
         Matcher var10 = var7 != null && MUTATING_STEPS.contains(var7) ? DECLARATION_PATTERN.matcher(var8.trim()) : null;
         if (var10 != null && var10.matches()) {
            String var14 = var10.group(2);
            String var12 = findCaptureReference(var14, var13);
            if (var12 != null && var5 > var3) {
               throw new io.karatelabs.ext.mutation.RelationalAssertionAnalyzer.RelationShapeException("carries " + var12 + " from the first response into the second request");
            }

            if (var12 != null || var9 && RESPONSE_REFERENCE_PATTERN.matcher(var14).find()) {
               var13.add(var10.group(1));
            }
         } else {
            String var11 = findCaptureReference(var8, var13);
            if (var11 == null && var9 && ("configure".equals(var7) || MUTATION_CALL_PATTERN.matcher(var8).find()) && RESPONSE_REFERENCE_PATTERN.matcher(var8).find()) {
               var11 = "the first response";
            }

            if (var11 != null) {
               throw new io.karatelabs.ext.mutation.RelationalAssertionAnalyzer.RelationShapeException("carries " + var11 + " from the first response into the second request");
            }
         }
      }
   }

   private static String findCaptureReference(String var0, Set<String> var1) {
      for (String var3 : (Iterable<String>) (Iterable<?>) (new TreeSet(var1))) {
         if (io.karatelabs.ext.mutation.AssertionEvidenceExtractor.referencesIdentifier(var0, var3)) {
            return var3;
         }
      }

      return null;
   }

   private static Map<String, Object> diffRequests(Map<String, Object> var0, Map<String, Object> var1) {
      for (String var3 : List.of("method", "urlBase", "path")) {
         if (!Objects.equals(var0.get(var3), var1.get(var3))) {
            throw new io.karatelabs.ext.mutation.RelationalAssertionAnalyzer.RelationShapeException("the two requests are not the same operation (" + var3 + ")");
         }
      }

      LinkedHashMap<java.lang.String, java.lang.Object> var8 = new LinkedHashMap();
      Map<java.lang.String, Object> var9 = var0.get("params") instanceof Map var4 ? var4 : Map.of();
      Map<java.lang.String, Object> var10 = var1.get("params") instanceof Map var11 ? var11 : Map.of();
      TreeSet<java.lang.String> var12 = new TreeSet();
      var9.keySet().forEach(var1x -> var12.add(String.valueOf(var1x)));
      var10.keySet().forEach(var1x -> var12.add(String.valueOf(var1x)));

      for (String var7 : (Iterable<String>) (Iterable<?>) (var12)) {
         if (!deepEquals(var9.get(var7), var10.get(var7))) {
            var8.put("?" + var7, var10.containsKey(var7) ? var10.get(var7) : ABSENT_MARKER);
         }
      }

      Object var14 = var0.get("body");
      Object var15 = var1.get("body");
      if (isStructuredValue(var14) && isStructuredValue(var15)) {
         diffStructuredValues(var14, var15, "", var8);
      } else if (!deepEquals(var14, var15)) {
         throw new io.karatelabs.ext.mutation.RelationalAssertionAnalyzer.RelationShapeException("the two requests differ in a non-JSON body");
      }

      if (var8.isEmpty()) {
         throw new io.karatelabs.ext.mutation.RelationalAssertionAnalyzer.RelationShapeException("the two requests differ on no named axis");
      } else {
         return var8;
      }
   }

   private static boolean isStructuredValue(Object var0) {
      return var0 instanceof Map || var0 instanceof List;
   }

   private static void diffStructuredValues(Object var0, Object var1, String var2, Map<String, Object> var3) {
      if (var0 instanceof Map var4 && var1 instanceof Map var5) {
         LinkedHashSet var12 = new LinkedHashSet(var4.keySet());
         var12.addAll(var5.keySet());

         for (Object var10 : var12) {
            String var11 = var2.isEmpty() ? String.valueOf(var10) : var2 + "." + var10;
            if (var4.containsKey(var10) && var5.containsKey(var10)) {
               diffStructuredValues(var4.get(var10), var5.get(var10), var11, var3);
            } else {
               recordDifference(var5.containsKey(var10) ? var5.get(var10) : var4.get(var10), var11, !var5.containsKey(var10), var3);
            }
         }
      } else if (var0 instanceof List var6 && var1 instanceof List var7) {
         for (int var8 = 0; var8 < Math.max(var6.size(), var7.size()); var8++) {
            String var9 = var2 + "[" + var8 + "]";
            if (var8 < var6.size() && var8 < var7.size()) {
               diffStructuredValues(var6.get(var8), var7.get(var8), var9, var3);
            } else {
               recordDifference(var8 < var7.size() ? var7.get(var8) : var6.get(var8), var9, var8 >= var7.size(), var3);
            }
         }
      } else if (!deepEquals(var0, var1)) {
         var3.put(var2.isEmpty() ? "body" : var2, var1);
      }
   }

   private static void recordDifference(Object var0, String var1, boolean var2, Map<String, Object> var3) {
      if (var0 instanceof Map var4 && !var4.isEmpty()) {
         var4.forEach((var3x, var4x) -> recordDifference(var4x, var1 + "." + var3x, var2, var3));
      } else if (var0 instanceof List var5 && !var5.isEmpty()) {
         for (int var6 = 0; var6 < var5.size(); var6++) {
            recordDifference(var5.get(var6), var1 + "[" + var6 + "]", var2, var3);
         }
      } else {
         var3.put(var1, var2 ? ABSENT_MARKER : var0);
      }
   }

   private static boolean containsValue(Object var0, Object var1) {
      return var0 instanceof List var2 && !(var1 instanceof List)
         ? var2.stream().anyMatch(var1x -> containsValue(var1x, var1))
         : deepEquals(var0, var1) || isScalar(var0) && isScalar(var1) && canonicalScalar(var0).equals(canonicalScalar(var1));
   }

   private static boolean isScalar(Object var0) {
      return var0 instanceof String || var0 instanceof Number || var0 instanceof Boolean;
   }

   private static String canonicalScalar(Object var0) {
      return var0 instanceof Number var1 ? new BigDecimal(var1.toString()).stripTrailingZeros().toPlainString() : String.valueOf(var0);
   }

   private static void collectReadSites(Node var0, io.karatelabs.ext.mutation.RelationalAssertionAnalyzer.ExchangeContext var1, Map<String, io.karatelabs.ext.mutation.RelationalAssertionAnalyzer.CaptureRelation> var2, List<io.karatelabs.ext.mutation.RelationalAssertionAnalyzer.ResponseReadSite> var3) {
      ArrayList var4 = new ArrayList();
      collectPaths(var0, var4);

      for (io.karatelabs.ext.mutation.RelationalAssertionAnalyzer.ExpressionPath var6 : (Iterable<io.karatelabs.ext.mutation.RelationalAssertionAnalyzer.ExpressionPath>) (Iterable<?>) (var4)) {
         if ("response".equals(var6.a())) {
            var3.add(new io.karatelabs.ext.mutation.RelationalAssertionAnalyzer.ResponseReadSite(var1, var6.b()));
         } else {
            io.karatelabs.ext.mutation.RelationalAssertionAnalyzer.CaptureRelation var7 = (io.karatelabs.ext.mutation.RelationalAssertionAnalyzer.CaptureRelation)var2.get(var6.a());
            if (var7.b() != null) {
               var var8 = new ArrayList<>(var7.b());
               var8.addAll(var6.b());
               var3.add(new io.karatelabs.ext.mutation.RelationalAssertionAnalyzer.ResponseReadSite(var7.c(), var8));
            } else {
               ArrayList<io.karatelabs.ext.mutation.RelationalAssertionAnalyzer.ExpressionPath> var9 = new ArrayList<>();
               collectPaths(var7.a(), var9);
               var9.forEach(var2x -> var3.add(new io.karatelabs.ext.mutation.RelationalAssertionAnalyzer.ResponseReadSite(var7.c(), var2x.b())));
            }
         }
      }
   }

   private static boolean readComesFromRequest(io.karatelabs.ext.mutation.RelationalAssertionAnalyzer.ResponseReadSite var0) {
      io.karatelabs.ext.mutation.AssertionEvidenceExtractor.ResolvedPath var1 = io.karatelabs.ext.mutation.AssertionEvidenceExtractor.resolvePath(responseBodyForExchange(var0.a()), var0.b());
      if (!var1.a()) {
         return false;
      } else {
         Object var2 = requestForExchange(var0.a()).get("body");
         int var3 = var0.b().size();

         for (int var4 = 0; var4 <= Math.max(0, var3 - 1); var4++) {
            io.karatelabs.ext.mutation.AssertionEvidenceExtractor.ResolvedPath var5 = io.karatelabs.ext.mutation.AssertionEvidenceExtractor.resolvePath(var2, var0.b().subList(var4, var3));
            if (var5.a() && deepEquals(var5.b(), var1.b())) {
               return true;
            }
         }

         return var3 > 0
            && var0.b().get(var3 - 1) instanceof String var8
            && requestForExchange(var0.a()).get("params") instanceof Map var7
            && var7.containsKey(var8)
            && var1.b() != null
            && containsValue(var7.get(var8), var1.b());
      }
   }

   private static boolean evaluateRelationMatch(
      io.karatelabs.ext.mutation.RelationalAssertionAnalyzer.ExpressionPath var0, String var1, Node var2, Set<String> var3, io.karatelabs.ext.mutation.RelationalAssertionAnalyzer.ExchangeContext var4, Map<String, io.karatelabs.ext.mutation.RelationalAssertionAnalyzer.CaptureRelation> var5
   ) {
      try {
         LinkedHashMap<java.lang.String, java.lang.Object> var6 = new LinkedHashMap();

         for (String var8 : var3) {
            if (!"response".equals(var8)) {
               io.karatelabs.ext.mutation.RelationalAssertionAnalyzer.CaptureRelation var9 = (io.karatelabs.ext.mutation.RelationalAssertionAnalyzer.CaptureRelation)var5.get(var8);
               var6.put(var8, evaluateExpression(var9.a(), responseBodyForExchange(var9.c()), Map.of()));
            }
         }

         Object var11 = responseBodyForExchange(var4);
         if (var0 != null) {
            io.karatelabs.ext.mutation.AssertionEvidenceExtractor.ResolvedPath var13 = io.karatelabs.ext.mutation.AssertionEvidenceExtractor.resolvePath(var11, var0.b());
            if (var13.a() && var13.b() != null) {
               Object var15 = evaluateExpression(var2, var11, var6);
               return Match.execute(new Engine(), Match.Type.valueOf(var1), var13.b(), var15).pass;
            } else {
               return false;
            }
         } else {
            return evaluateExpression(var2, var11, var6) instanceof Boolean var12 && var12;
         }
      } catch (RuntimeException var10) {
         return false;
      }
   }

   private static Object evaluateExpression(Node var0, Object var1, Map<String, Object> var2) {
      Node var3 = unwrapExpression(var0);
      io.karatelabs.ext.mutation.RelationalAssertionAnalyzer.ExpressionPath var4 = parsePath(var3);
      if (var4 != null) {
         Object var9 = "response".equals(var4.a()) ? var1 : var2.get(var4.a());
         io.karatelabs.ext.mutation.AssertionEvidenceExtractor.ResolvedPath var11 = io.karatelabs.ext.mutation.AssertionEvidenceExtractor.resolvePath(var9, var4.b());
         if (var11.a() && var11.b() != null) {
            return var11.b();
         } else {
            throw new io.karatelabs.ext.mutation.RelationalAssertionAnalyzer.ExpressionEvaluationException();
         }
      } else {
         switch (var3.type) {
            case LIT_EXPR:
               return new Engine().eval("(" + var3.getText() + ")");
            case MATH_PRE_EXPR:
               double var8 = asDouble(evaluateExpression(var3.get(1), var1, var2));
               return var3.get(0).token.type == TokenType.MINUS ? -var8 : var8;
            case UNARY_EXPR:
               if (evaluateExpression(var3.get(1), var1, var2) instanceof Boolean var7) {
                  return !var7;
               }

               throw new io.karatelabs.ext.mutation.RelationalAssertionAnalyzer.ExpressionEvaluationException();
            default:
               Object var5 = evaluateExpression(var3.get(0), var1, var2);

               for (byte var6 = 1; var6 + 1 < var3.size(); var6 += 2) {
                  var5 = applyOperator(var3.get(var6).token.type, var5, evaluateExpression(var3.get(var6 + 1), var1, var2));
               }

               return var5;
         }
      }
   }

   private static Object applyOperator(TokenType var0, Object var1, Object var2) {
      return switch (var0) {
         case PLUS -> asDouble(var1) + asDouble(var2);
         case MINUS -> asDouble(var1) - asDouble(var2);
         case STAR -> asDouble(var1) * asDouble(var2);
         case SLASH -> asDouble(var1) / asDouble(var2);
         case PERCENT -> asDouble(var1) % asDouble(var2);
         case STAR_STAR -> Math.pow(asDouble(var1), asDouble(var2));
         case LT -> asDouble(var1) < asDouble(var2);
         case GT -> asDouble(var1) > asDouble(var2);
         case LT_EQ -> asDouble(var1) <= asDouble(var2);
         case GT_EQ -> asDouble(var1) >= asDouble(var2);
         case EQ_EQ, EQ_EQ_EQ -> deepEquals(var1, var2);
         case NOT_EQ, NOT_EQ_EQ -> !deepEquals(var1, var2);
         case AMP_AMP -> asBoolean(var1) && asBoolean(var2);
         case PIPE_PIPE -> asBoolean(var1) || asBoolean(var2);
         default -> throw new io.karatelabs.ext.mutation.RelationalAssertionAnalyzer.ExpressionEvaluationException();
      };
   }

   private static double asDouble(Object var0) {
      if (var0 instanceof Number var1) {
         return var1.doubleValue();
      } else {
         throw new io.karatelabs.ext.mutation.RelationalAssertionAnalyzer.ExpressionEvaluationException();
      }
   }

   private static boolean asBoolean(Object var0) {
      if (var0 instanceof Boolean var1) {
         return var1;
      } else {
         throw new io.karatelabs.ext.mutation.RelationalAssertionAnalyzer.ExpressionEvaluationException();
      }
   }

   private static String validateExpression(Node var0, boolean var1, Set<String> var2) {
      Node var3 = unwrapExpression(var0);
      if (var3 == null) {
         return "a compound expression";
      } else {
         io.karatelabs.ext.mutation.RelationalAssertionAnalyzer.ExpressionPath var4 = parsePath(var3);
         if (var4 != null) {
            var2.add(var4.a());
            return null;
         } else {
            switch (var3.type) {
               case LIT_EXPR:
                  Node var7 = var3.get(0);
                  return (var7.isToken() ? !LITERAL_TOKEN_TYPES.contains(var7.token.type) : var1 || !io.karatelabs.ext.mutation.AssertionEvidenceExtractor.isLiteralExpression(var3.getText())) ? "a non-literal value" : null;
               case MATH_PRE_EXPR:
                  TokenType var6 = var3.get(0).isToken() ? var3.get(0).token.type : null;
                  return var3.size() != 2 || var6 != TokenType.MINUS && var6 != TokenType.PLUS ? "an unsupported operator" : validateExpression(var3.get(1), var1, var2);
               case UNARY_EXPR:
                  boolean var5 = var3.size() == 2 && var3.get(0).isToken() && var3.get(0).token.type == TokenType.NOT;
                  return var5 && !var1 ? validateExpression(var3.get(1), var1, var2) : "an unsupported operator";
               case MATH_ADD_EXPR:
               case MATH_MUL_EXPR:
               case MATH_EXP_EXPR:
                  return validateOperatorExpression(var3, MATH_OPERATORS, var1, var2);
               case LOGIC_EXPR:
                  return var1 ? "a comparison" : validateOperatorExpression(var3, COMPARISON_OPERATORS, false, var2);
               case LOGIC_AND_EXPR:
                  return var1 ? "a logical operator" : validateOperatorExpression(var3, LOGICAL_OPERATORS, false, var2);
               default:
                  return "an unsupported construct (" + var3.type + ")";
            }
         }
      }
   }

   private static String validateOperatorExpression(Node var0, Set<TokenType> var1, boolean var2, Set<String> var3) {
      if (var0.size() >= 3 && var0.size() % 2 != 0) {
         for (int var4 = 0; var4 < var0.size(); var4++) {
            Node var5 = var0.get(var4);
            if (var4 % 2 == 1) {
               if (!var5.isToken() || !var1.contains(var5.token.type)) {
                  return "an unsupported operator";
               }
            } else {
               String var6 = validateExpression(var5, var2, var3);
               if (var6 != null) {
                  return var6;
               }
            }
         }

         return null;
      } else {
         return "an unsupported construct (" + var0.type + ")";
      }
   }

   private static io.karatelabs.ext.mutation.RelationalAssertionAnalyzer.ExpressionPath parsePath(Node var0) {
      Node var1 = unwrapExpression(var0);
      if (var1 == null) {
         return null;
      } else {
         switch (var1.type) {
            case REF_EXPR:
               return var1.size() == 1 && var1.get(0).isToken() && var1.get(0).token.type == TokenType.IDENT
                  ? new io.karatelabs.ext.mutation.RelationalAssertionAnalyzer.ExpressionPath(var1.get(0).getText(), List.of())
                  : null;
            case REF_DOT_EXPR:
               if (var1.size() == 3
                  && var1.get(1).isToken()
                  && var1.get(1).token.type == TokenType.DOT
                  && var1.get(2).isToken()
                  && var1.get(2).token.type == TokenType.IDENT) {
                  return appendPathSegment(parsePath(var1.get(0)), var1.get(2).getText());
               }

               return null;
            case REF_BRACKET_EXPR:
               if (var1.size() != 4) {
                  return null;
               } else {
                  Node var2 = unwrapExpression(var1.get(2));
                  if (var2 != null && var2.type == NodeType.LIT_EXPR && var2.get(0).isToken()) {
                     TokenType var3 = var2.get(0).token.type;
                     if (var3 == TokenType.NUMBER && var2.getText().matches("\\d+")) {
                        return appendPathSegment(parsePath(var1.get(0)), Integer.parseInt(var2.getText()));
                     }

                     if (var3 != TokenType.S_STRING && var3 != TokenType.D_STRING) {
                        return null;
                     }

                     return appendPathSegment(parsePath(var1.get(0)), String.valueOf(new Engine().eval(var2.getText())));
                  }

                  return null;
               }
            default:
               return null;
         }
      }
   }

   private static io.karatelabs.ext.mutation.RelationalAssertionAnalyzer.ExpressionPath appendPathSegment(io.karatelabs.ext.mutation.RelationalAssertionAnalyzer.ExpressionPath var0, Object var1) {
      if (var0 == null) {
         return null;
      } else {
         var var2 = new ArrayList<>(var0.b());
         var2.add(var1);
         return new io.karatelabs.ext.mutation.RelationalAssertionAnalyzer.ExpressionPath(var0.a(), var2);
      }
   }

   private static void collectPaths(Node var0, List<io.karatelabs.ext.mutation.RelationalAssertionAnalyzer.ExpressionPath> var1) {
      if (!var0.isToken()) {
         io.karatelabs.ext.mutation.RelationalAssertionAnalyzer.ExpressionPath var2 = parsePath(var0);
         if (var2 != null) {
            var1.add(var2);
         } else {
            for (int var3 = 0; var3 < var0.size(); var3++) {
               collectPaths(var0.get(var3), var1);
            }
         }
      }
   }

   private static Node parseExpression(String var0) {
      try {
         return unwrapExpression(Engine.parse("(" + var0 + ")"));
      } catch (RuntimeException var2) {
         return null;
      }
   }

   private static Node unwrapExpression(Node var0) {
      Node var1 = var0;

      while (var1 != null && !var1.isToken()) {
         switch (var1.type) {
            case PROGRAM:
            case STATEMENT:
            case EXPR_LIST:
            case EXPR:
            case PAREN_EXPR:
               Node var2 = null;
               int var3 = 0;

               for (; var3 < var1.size(); var3++) {
                  Node var4 = var1.get(var3);
                  if (!var4.isToken() && var4.type != NodeType.EOS) {
                     if (var2 != null) {
                        return null;
                     }

                     var2 = var4;
                  }
               }

               var1 = var2;
               break;
            default:
               return var1;
         }
      }

      return var1;
   }

   private static Map<String, Object> requestForExchange(io.karatelabs.ext.mutation.RelationalAssertionAnalyzer.ExchangeContext var0) {
      return var0.c().get("request") instanceof Map var1 ? var1 : Map.of();
   }

   private static Object responseBodyForExchange(io.karatelabs.ext.mutation.RelationalAssertionAnalyzer.ExchangeContext var0) {
      return var0.c().get("response") instanceof Map var1 ? var1.get("body") : null;
   }

   static boolean deepEquals(Object var0, Object var1) {
      if (var0 instanceof Number var2 && var1 instanceof Number var9) {
         return var2.doubleValue() == var9.doubleValue();
      } else if (var0 instanceof Map var6 && var1 instanceof Map var8) {
         if (!var6.keySet().equals(var8.keySet())) {
            return false;
         } else {
            for (Object var5 : var6.keySet()) {
               if (!deepEquals(var6.get(var5), var8.get(var5))) {
                  return false;
               }
            }

            return true;
         }
      } else if (!(var0 instanceof List var7 && var1 instanceof List var3)) {
         return Objects.equals(var0, var1);
      } else if (var7.size() != var3.size()) {
         return false;
      } else {
         for (int var4 = 0; var4 < var7.size(); var4++) {
            if (!deepEquals(var7.get(var4), var3.get(var4))) {
               return false;
            }
         }

         return true;
      }
   }

   private record ResponseReadSite(io.karatelabs.ext.mutation.RelationalAssertionAnalyzer.ExchangeContext a, List<Object> b) {

   }

   private record ExpressionPath(String a, List<Object> b) {

   }

   private static final class RelationShapeException extends RuntimeException {
      RelationShapeException(String var1) {
         super(var1, null, false, false);
      }
   }

   record StepExecutionContext(io.karatelabs.ext.common.KarateEventLogParser.KarateStepResult a, io.karatelabs.ext.mutation.RelationalAssertionAnalyzer.ExchangeContext b) {

   }

   private static final class ExpressionEvaluationException extends RuntimeException {
      ExpressionEvaluationException() {
         super(null, null, false, false);
      }
   }

   record CaptureRelation(Node a, List<Object> b, io.karatelabs.ext.mutation.RelationalAssertionAnalyzer.ExchangeContext c) {

   }

   record RelationAnalysisResult(String a, String b, Boolean c, int d, List<String> e, String f) {


      static io.karatelabs.ext.mutation.RelationalAssertionAnalyzer.RelationAnalysisResult refusal(String var0) {
         return new io.karatelabs.ext.mutation.RelationalAssertionAnalyzer.RelationAnalysisResult(null, null, null, -1, null, var0);
      }
   }

   record ExchangeContext(int a, int b, Map<String, Object> c) {

   }
}
