package io.karatelabs.ext.rules.twin;

import io.karatelabs.js.Engine;
import io.karatelabs.js.JavaCallable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

public final class RequiredBehaviorVerifier {
   private static final String STATE_KIND = "state";
   private static final String TRANSITION_KIND = "transition";
   private static final String REJECTION_KIND = "rejection";
   private static final String ASSERTION_KIND = "assertion";
   private static final String INVARIANT_KIND = "invariant";
   private static final String APPLIED_STATUS = "applied";
   private static final String REFUSED_STATUS = "refused";
   private static final int MAX_OBSERVATION_SAMPLES = 16;
   private static final String REQUIREMENT_KIND = "req";
   private static final String NOT_REACHED_STATUS = "notreached";
   private static final String FAILED_STATUS = "failed";
   private static final Map<String, List<String>> ALLOWED_ROW_KEYS = Map.of(
      "state",
      List.of("id", "kind", "prose", "state"),
      "transition",
      List.of("id", "kind", "prose", "from", "to", "operation", "alternatives", "args", "world", "request", "outcome"),
      "rejection",
      List.of("id", "kind", "prose", "from", "operation", "alternatives", "args", "world", "reason", "request", "outcome"),
      "assertion",
      List.of("id", "kind", "prose", "appliesTo", "predicate"),
      "invariant",
      List.of("id", "kind", "prose", "predicate")
   );
   private static final List<String> ALLOWED_OUTCOME_KEYS = List.of("kind", "status", "reason", "body");
   private static final List<String> VALID_ROW_KINDS = List.of("state", "transition", "rejection", "assertion", "invariant");
   private static final List<String> REQUIRED_DISCLOSURE_KEYS = List.of(
      "halting",
      "notExpanded",
      "errors",
      "identity",
      "snapshotViolations",
      "states",
      "transitions",
      "guards",
      "scopeGuards",
      "invariants",
      "clock",
      "plan",
      "undeclaredOpenFields"
   );
   private final List<io.karatelabs.ext.rules.twin.RequiredBehaviorVerifier.RequiredBehaviorRow> rows;
   private final String requiredSha;
   private final Map<String, List<String>> transitionIdsByScope;
   private final TwinModel model;
   private final Map<Object, Object> requestCache = new LinkedHashMap<>();
   private final Map<String, String> decisionCache = new LinkedHashMap<>();
   private List<Map<String, Object>> syntheticObservations;

   private RequiredBehaviorVerifier(List<io.karatelabs.ext.rules.twin.RequiredBehaviorVerifier.RequiredBehaviorRow> var1, String var2, Map<String, List<String>> var3, TwinModel var4) {
      this.rows = var1;
      this.requiredSha = var2;
      this.transitionIdsByScope = var3;
      this.model = var4;
   }

   public static RequiredBehaviorVerifier fromConfig(Object var0, TwinModel var1) {
      if (!(var0 instanceof Map var2)) {
         throw TwinException.b(null, "required", "must be an object");
      } else {
         for (Object var4 : var2.keySet()) {
            if (!"rows".equals(var4) && !"sha".equals(var4)) {
               throw TwinException.b(null, String.valueOf(var4), "unknown key");
            }
         }

         if (!(var2.get("rows") instanceof List var12)) {
            throw TwinException.b(null, "rows", "must be an array");
         } else if (var2.containsKey("sha") && !(var2.get("sha") instanceof String)) {
            throw TwinException.b(null, "sha", "must be a string");
         } else {
            Engine var14 = new Engine();
            ArrayList<io.karatelabs.ext.rules.twin.RequiredBehaviorVerifier.RequiredBehaviorRow> var5 = new ArrayList(var12.size());
            LinkedHashSet var6 = new LinkedHashSet();
            LinkedHashSet<java.lang.String> var7 = new LinkedHashSet();

            for (Object var9 : var12) {
               io.karatelabs.ext.rules.twin.RequiredBehaviorVerifier.RequiredBehaviorRow var10 = parseRow(var9, var1, var14, var6);
               var5.add(var10);
               if ("transition".equals(var10.kind())) {
                  var7.add(var10.id());
               }
            }

            for (io.karatelabs.ext.rules.twin.RequiredBehaviorVerifier.RequiredBehaviorRow var16 : (Iterable<io.karatelabs.ext.rules.twin.RequiredBehaviorVerifier.RequiredBehaviorRow>) (Iterable<?>) (var5)) {
               for (String var11 : var16.appliesTo()) {
                  if (!var7.contains(var11)) {
                     throw TwinException.b(var16.id(), "appliesTo", "names no transition row: " + var11);
                  }
               }
            }

            return new RequiredBehaviorVerifier(List.copyOf(var5), (String)var2.get("sha"), buildTransitionIdsByScope(var1), var1);
         }
      }
   }

   public static RequiredBehaviorVerifier empty(TwinModel var0) {
      return new RequiredBehaviorVerifier(List.of(), null, buildTransitionIdsByScope(var0), var0);
   }

   private static Map<String, List<String>> buildTransitionIdsByScope(TwinModel var0) {
      LinkedHashMap<java.lang.String, java.util.List<java.lang.String>> var1 = new LinkedHashMap();

      for (io.karatelabs.ext.rules.twin.TwinModel.CommandModel var3 : var0.c()) {
         if (var3.j() != null) {
            var1.computeIfAbsent(var3.j(), var0x -> new ArrayList<>()).add(var3.a());
         }
      }

      return var1;
   }

   public Set<String> rowIds() {
      LinkedHashSet<java.lang.String> var1 = new LinkedHashSet();

      for (io.karatelabs.ext.rules.twin.RequiredBehaviorVerifier.RequiredBehaviorRow var3 : this.rows) {
         var1.add(var3.id());
      }

      return var1;
   }

   public String sha() {
      return this.requiredSha;
   }

   public static String canonicalize(Object var0) {
      return io.karatelabs.ext.rules.twin.CanonicalJson.a(io.karatelabs.ext.rules.twin.CanonicalJson.c(var0));
   }

   public String kindOf(String var1) {
      for (io.karatelabs.ext.rules.twin.RequiredBehaviorVerifier.RequiredBehaviorRow var3 : this.rows) {
         if (var3.id().equals(var1)) {
            return var3.kind();
         }
      }

      return null;
   }

   public List<Map<String, Object>> verifyRow(TwinWalkResult var1, io.karatelabs.ext.rules.twin.TwinWalker.ReverseTransitionPath var2, String var3) {
      for (io.karatelabs.ext.rules.twin.RequiredBehaviorVerifier.RequiredBehaviorRow var5 : this.rows) {
         if (var5.id().equals(var3)) {
            return this.pathForRow(var1, var2, var5, "transition".equals(var5.kind()) ? this.findTransitionWitnesses(var1, var5) : List.of());
         }
      }

      return null;
   }

   public int rowCount() {
      return this.rows.size();
   }

   public Map<String, Object> verifyRows(TwinWalkResult var1, io.karatelabs.ext.rules.twin.TwinWalker.ReverseTransitionPath var2, List<String> var3) {
      return this.verifyRowsWithCoverage(var1, var2, var3, io.karatelabs.ext.rules.twin.RequiredBehaviorVerifier.LaneCoverage.a);
   }

   public Map<String, Object> verifyRowsWithCoverage(TwinWalkResult var1, io.karatelabs.ext.rules.twin.TwinWalker.ReverseTransitionPath var2, List<String> var3, io.karatelabs.ext.rules.twin.RequiredBehaviorVerifier.LaneCoverage var4) {
      LinkedHashMap<java.lang.String, java.util.List<io.karatelabs.ext.rules.twin.RequiredBehaviorVerifier.TransitionWitness>> var5 = new LinkedHashMap();

      for (io.karatelabs.ext.rules.twin.RequiredBehaviorVerifier.RequiredBehaviorRow var7 : this.rows) {
         if ("transition".equals(var7.kind())) {
            var5.put(var7.id(), this.findTransitionWitnesses(var1, var7));
         }
      }

      ArrayList<java.util.Map<java.lang.String,java.lang.Object>> var15 = new ArrayList();
      ArrayList<java.util.LinkedHashMap> var16 = new ArrayList();

      for (io.karatelabs.ext.rules.twin.RequiredBehaviorVerifier.RequiredBehaviorRow var9 : this.rows) {
         if (isSelected(var3, var9.id())) {
            String var10 = this.unreachableReason(var1, var9);
            if (var10 != null) {
               var var11 = createRowResult(var9, "notreached");
               var11.put("reason", var10);
               var15.add(var11);
            } else {
               var var21 = this.pathForRow(var1, var2, var9, var5.getOrDefault(var9.id(), List.of()));
               String var13 = var9.kind();

               Map var12 = switch (var13) {
                  case "state", "transition" -> var21 == null ? createRowResult(var9, "notreached") : null;
                  case "rejection" -> var21 == null ? notReachedResult(var9) : null;
                  case "assertion" -> this.assertionResult(var9, var5);
                  default -> this.invariantResult(var1, var9);
               };
               if (var21 != null) {
                  LinkedHashMap<java.lang.String, java.lang.Object> var22 = new LinkedHashMap();
                  var22.put("id", var9.id());
                  var22.put("path", var21);
                  addProse(var22, var9);
                  var16.add(var22);
               }

               if (var12 != null) {
                  var15.add(var12);
               }
            }
         }
      }

      var var17 = this.rowIds();

      for (Entry var20 : this.transitionIdsByScope.entrySet()) {
         if (!var17.contains(var20.getKey()) && isSelected(var3, (String)var20.getKey()) && !hasObservedTransition(var1, (List<String>)var20.getValue())) {
            var15.add(createStatusResult((String)var20.getKey(), "req", "notreached"));
         }
      }

      LinkedHashMap<java.lang.String, java.lang.Object> var19 = new LinkedHashMap();
      var19.put("requiredNotreached", var15);
      var19.put("requiredWitness", var16);
      var19.put("evidence", coverageSummary(var1, var4));
      var19.put("incomplete", incompleteReasons(var1));
      var19.put("ci", buildVerificationVerdict(var15, List.of(), List.of(), List.of(), List.of(), List.of(), var1));
      return var19;
   }

   private static boolean isSelected(List<String> var0, String var1) {
      return var0 == null || var0.isEmpty() || var0.contains(var1);
   }

   private static boolean hasObservedTransition(TwinWalkResult var0, List<String> var1) {
      for (io.karatelabs.ext.rules.twin.TwinWalkResult.WalkTransition var3 : var0.f()) {
         if (var1.contains(var3.b())) {
            return true;
         }
      }

      return false;
   }

   private String unreachableReason(TwinWalkResult var1, io.karatelabs.ext.rules.twin.RequiredBehaviorVerifier.RequiredBehaviorRow var2) {
      if (var2.undeclaredReason() != null) {
         return var2.undeclaredReason();
      } else if (var2.routePattern() != null) {
         for (Map var4 : this.syntheticObservations(var1)) {
            if (routeMatches(var2.routePattern(), var4)) {
               return null;
            }
         }

         return "undeclared:operation:" + var2.routePattern().method() + " /" + String.join("/", var2.routePattern().pathSegments());
      } else {
         return var2.expectedOutcome() != null && (var2.expectedOutcome().status() != null || var2.expectedOutcome().body() != null) && this.observedCommandIds().isEmpty() ? "undeclared:observe" : null;
      }
   }

   private List<Map<String, Object>> pathForRow(TwinWalkResult var1, io.karatelabs.ext.rules.twin.TwinWalker.ReverseTransitionPath var2, io.karatelabs.ext.rules.twin.RequiredBehaviorVerifier.RequiredBehaviorRow var3, List<io.karatelabs.ext.rules.twin.RequiredBehaviorVerifier.TransitionWitness> var4) {
      String var5 = var3.kind();
      switch (var5) {
         case "state":
            Integer var9 = findStateNode(var1, var3);
            return var9 == null ? null : var2.a(var9);
         case "transition":
            if (var4.isEmpty()) {
               return null;
            }

            io.karatelabs.ext.rules.twin.TwinWalkResult.WalkTransition var8 = ((io.karatelabs.ext.rules.twin.RequiredBehaviorVerifier.TransitionWitness)var4.get(0)).transition();
            return appendNodePath(var2.a(var8.a()), var8.b(), var8.c());
         case "rejection":
            io.karatelabs.ext.rules.twin.TwinWalkResult.WalkRefusal var7 = this.findRejectionWitness(var1, var3);
            return var7 == null ? null : appendNodePath(var2.a(var7.a()), var7.b(), var7.c());
         default:
            return null;
      }
   }

   private static List<Map<String, Object>> appendNodePath(List<Map<String, Object>> var0, String var1, Map<String, Object> var2) {
      var0.add(TwinWalker.a(var1, var2));
      return var0;
   }

   private static Integer findStateNode(TwinWalkResult var0, io.karatelabs.ext.rules.twin.RequiredBehaviorVerifier.RequiredBehaviorRow var1) {
      for (io.karatelabs.ext.rules.twin.TwinWalkResult.WalkState var3 : var0.e()) {
         if (var3.e().contains(var1.state())) {
            return var3.a();
         }
      }

      return null;
   }

   private List<io.karatelabs.ext.rules.twin.RequiredBehaviorVerifier.TransitionWitness> findTransitionWitnesses(TwinWalkResult var1, io.karatelabs.ext.rules.twin.RequiredBehaviorVerifier.RequiredBehaviorRow var2) {
      ArrayList<io.karatelabs.ext.rules.twin.RequiredBehaviorVerifier.TransitionWitness> var3 = new ArrayList();

      for (io.karatelabs.ext.rules.twin.TwinWalkResult.WalkTransition var5 : var1.f()) {
         if (var5.d() != null && matchesTransitionScope(var2, var5.b())) {
            io.karatelabs.ext.rules.twin.TwinWalkResult.WalkState var6 = var1.e().get(var5.a());
            io.karatelabs.ext.rules.twin.TwinWalkResult.WalkState var7 = var1.e().get(var5.d());
            if (nodeMatchesCoverage(var2.from(), var6) && nodeMatchesCoverage(var2.to(), var7) && this.outcomeMatches(var2, var5, var5.b(), var5.c(), var6.c()) && this.transitionOutcomeMatches(var2, var5.b(), null)) {
               var3.add(new io.karatelabs.ext.rules.twin.RequiredBehaviorVerifier.TransitionWitness(var5, var6.c(), var7.c(), var2.from(), var2.to()));
            }
         }
      }

      return var3;
   }

   private io.karatelabs.ext.rules.twin.TwinWalkResult.WalkRefusal findRejectionWitness(TwinWalkResult var1, io.karatelabs.ext.rules.twin.RequiredBehaviorVerifier.RequiredBehaviorRow var2) {
      for (io.karatelabs.ext.rules.twin.TwinWalkResult.WalkRefusal var4 : var1.g()) {
         if ("apply".equals(var4.d()) && matchesTransitionScope(var2, var4.b())) {
            io.karatelabs.ext.rules.twin.TwinWalkResult.WalkState var5 = var1.e().get(var4.a());
            if (nodeMatchesCoverage(var2.from(), var5) && this.transitionOutcomeMatches(var2, var4.b(), var4.e()) && this.outcomeMatches(var2, var4, var4.b(), var4.c(), var5.c())) {
               return var4;
            }
         }
      }

      return null;
   }

   private static boolean matchesTransitionScope(io.karatelabs.ext.rules.twin.RequiredBehaviorVerifier.RequiredBehaviorRow var0, String var1) {
      return var0.commands().isEmpty() ? var0.routePattern() != null || var0.expectedOutcome() != null || var0.request() != null : var0.commands().contains(var1);
   }

   private static boolean nodeMatchesCoverage(String var0, io.karatelabs.ext.rules.twin.TwinWalkResult.WalkState var1) {
      return var0 == null || var1.e().contains(var0);
   }

   private boolean outcomeMatches(io.karatelabs.ext.rules.twin.RequiredBehaviorVerifier.RequiredBehaviorRow var1, Object var2, String var3, Map<String, Object> var4, Object var5) {
      if (var1.args() != null && !io.karatelabs.ext.rules.twin.ExpectationEvaluator.a(var1.args(), var4)) {
         return false;
      } else if (var1.world() != null && !io.karatelabs.ext.rules.twin.ExpectationEvaluator.a(var1.world(), var5)) {
         return false;
      } else if (var1.routePattern() == null && var1.request() == null) {
         return true;
      } else {
         var var6 = this.resolveRequest(var2, var3, var5, var4);
         if (var1.routePattern() != null && !routeMatches(var1.routePattern(), var6)) {
            return false;
         } else {
            Object var7 = var6 == null ? var4 : var6.getOrDefault("body", Map.of());
            return var1.request() == null || io.karatelabs.ext.rules.twin.ExpectationEvaluator.a(var1.request(), var7);
         }
      }
   }

   private boolean transitionOutcomeMatches(io.karatelabs.ext.rules.twin.RequiredBehaviorVerifier.RequiredBehaviorRow var1, String var2, String var3) {
      io.karatelabs.ext.rules.twin.RequiredBehaviorVerifier.ExpectedOutcome var4 = var1.expectedOutcome();
      if (var4 == null) {
         return true;
      } else if (var4.reason() != null && !var4.reason().equals(var3)) {
         return false;
      } else if (var4.status() == null && var4.body() == null) {
         return true;
      } else {
         String var5 = this.decisionOutcome(var2, var4.status(), var4.body());
         if (var5 == null) {
            return false;
         } else if (!"refused".equals(var4.kind())) {
            return "applied".equals(var5);
         } else {
            return var4.body() == null ? var5.startsWith("refused") : var5.equals("refused:" + var3);
         }
      }
   }

   private Map<String, Object> resolveRequest(Object var1, String var2, Object var3, Map<String, Object> var4) {
      Object var5 = this.requestCache.computeIfAbsent(var1 == null ? var2 : var1, var4x -> this.buildRequest(var2, var3, var4));
      return var5 instanceof Map ? asMap(var5) : null;
   }

   private Object buildRequest(String var1, Object var2, Map<String, Object> var3) {
      io.karatelabs.ext.rules.twin.TwinModel.CommandModel var4 = this.model.f().get(var1);
      if (var4 != null && var4.k() != null) {
         try {
            Object var5 = io.karatelabs.ext.rules.twin.CanonicalJson.d(var4.k().call(null, io.karatelabs.ext.rules.twin.CanonicalJson.d(var2), io.karatelabs.ext.rules.twin.CanonicalJson.d(var3)));
            return var5 instanceof Map var6 && var6.get("method") instanceof String && var6.get("path") instanceof String ? var5 : Boolean.FALSE;
         } catch (RuntimeException var7) {
            return Boolean.FALSE;
         }
      } else {
         return Boolean.FALSE;
      }
   }

   private List<Map<String, Object>> syntheticObservations(TwinWalkResult var1) {
      if (this.syntheticObservations != null) {
         return this.syntheticObservations;
      } else {
         ArrayList<java.util.Map<java.lang.String,java.lang.Object>> var2 = new ArrayList();

         for (io.karatelabs.ext.rules.twin.TwinWalkResult.WalkTransition var4 : var1.f()) {
            addUnique(var2, this.resolveRequest(var4, var4.b(), var1.e().get(var4.a()).c(), var4.c()));
         }

         for (io.karatelabs.ext.rules.twin.TwinWalkResult.WalkRefusal var11 : var1.g()) {
            addUnique(var2, this.resolveRequest(var11, var11.b(), var1.e().get(var11.a()).c(), var11.c()));
         }

         Object var10 = io.karatelabs.ext.rules.twin.TwinReplayEngine.initialState(this.model);

         for (io.karatelabs.ext.rules.twin.TwinModel.CommandModel var5 : this.model.c()) {
            var var6 = var5.f();

            for (int var7 = 0; var7 < Math.min(16, var6.size()); var7++) {
               Object var8 = this.buildRequest(var5.a(), var10, (Map<String, Object>)var6.get(var7));
               addUnique(var2, var8 instanceof Map ? asMap(var8) : null);
            }
         }

         this.syntheticObservations = var2;
         return var2;
      }
   }

   private static void addUnique(List<Map<String, Object>> var0, Map<String, Object> var1) {
      if (var1 != null && !var0.contains(var1)) {
         var0.add(var1);
      }
   }

   private List<String> observedCommandIds() {
      ArrayList<java.lang.String> var1 = new ArrayList();

      for (io.karatelabs.ext.rules.twin.TwinModel.CommandModel var3 : this.model.c()) {
         if (var3.l() != null) {
            var1.add(var3.a());
         }
      }

      return var1;
   }

   private String decisionOutcome(String var1, Integer var2, Object var3) {
      String var4 = this.decisionCache.computeIfAbsent(var1 + "|" + var2 + "|" + io.karatelabs.ext.rules.twin.CanonicalJson.c(var3), var4x -> this.computeDecisionOutcome(var1, var2, var3));
      return var4.isEmpty() ? null : var4;
   }

   private String computeDecisionOutcome(String var1, Integer var2, Object var3) {
      io.karatelabs.ext.rules.twin.TwinModel.CommandModel var4 = this.model.f().get(var1);
      if (var4 != null && var4.l() != null) {
         LinkedHashMap<java.lang.String, java.lang.Object> var5 = new LinkedHashMap();
         if (var2 != null) {
            var5.put("status", var2);
         }

         if (var3 != null) {
            var5.put("body", io.karatelabs.ext.rules.twin.CanonicalJson.d(var3));
         }

         Object var6;
         try {
            var6 = io.karatelabs.ext.rules.twin.CanonicalJson.d(var4.l().call(null, var5));
         } catch (RuntimeException var10) {
            return "";
         }

         if (var6 instanceof Map var7) {
            if ("applied".equals(var7.get("kind"))) {
               return "applied";
            } else if (!"refused".equals(var7.get("kind"))) {
               return "";
            } else {
               return var7.get("reason") instanceof String var8 ? "refused:" + var8 : "refused";
            }
         } else {
            return "";
         }
      } else {
         return "";
      }
   }

   private static boolean routeMatches(io.karatelabs.ext.rules.twin.RequiredBehaviorVerifier.RoutePattern var0, Map<String, Object> var1) {
      if (var1 != null && var0.method().equals(var1.get("method"))) {
         String var2 = (String)var1.get("path");
         int var3 = var2.indexOf(63);
         var var4 = splitPathSegments(var3 < 0 ? var2 : var2.substring(0, var3));
         if (var4.size() != var0.pathSegments().size()) {
            return false;
         } else {
            for (int var5 = 0; var5 < var4.size(); var5++) {
               String var6 = var0.pathSegments().get(var5);
               boolean var7 = var6.length() > 1 && var6.charAt(0) == '{' && var6.endsWith("}");
               if (var7 ? ((String)var4.get(var5)).isEmpty() : !var6.equals(var4.get(var5))) {
                  return false;
               }
            }

            return true;
         }
      } else {
         return false;
      }
   }

   private static io.karatelabs.ext.rules.twin.RequiredBehaviorVerifier.RoutePattern parseRoutePattern(String var0) {
      int var1 = var0.indexOf(32);
      if (var1 > 0 && var1 + 1 < var0.length() && var0.charAt(var1 + 1) == '/') {
         String var2 = var0.substring(0, var1);

         for (int var3 = 0; var3 < var2.length(); var3++) {
            if (var2.charAt(var3) < 'A' || var2.charAt(var3) > 'Z') {
               return null;
            }
         }

         return new io.karatelabs.ext.rules.twin.RequiredBehaviorVerifier.RoutePattern(var2, splitPathSegments(var0.substring(var1 + 1)));
      } else {
         return null;
      }
   }

   private static List<String> splitPathSegments(String var0) {
      ArrayList<java.lang.String> var1 = new ArrayList();

      for (String var5 : var0.split("/", -1)) {
         if (!var5.isEmpty()) {
            var1.add(var5);
         }
      }

      return var1;
   }

   private static Map<String, Object> notReachedResult(io.karatelabs.ext.rules.twin.RequiredBehaviorVerifier.RequiredBehaviorRow var0) {
      var var1 = createRowResult(var0, "notreached");
      if (var0.reason() != null) {
         var1.put("reason", var0.reason());
      }

      return var1;
   }

   private Map<String, Object> assertionResult(io.karatelabs.ext.rules.twin.RequiredBehaviorVerifier.RequiredBehaviorRow var1, Map<String, List<io.karatelabs.ext.rules.twin.RequiredBehaviorVerifier.TransitionWitness>> var2) {
      ArrayList var3 = new ArrayList();

      for (String var5 : var1.appliesTo()) {
         var3.addAll(var2.getOrDefault(var5, List.of()));
      }

      if (var3.isEmpty()) {
         return createRowResult(var1, "notreached");
      } else {
         for (io.karatelabs.ext.rules.twin.RequiredBehaviorVerifier.TransitionWitness var10 : (Iterable<io.karatelabs.ext.rules.twin.RequiredBehaviorVerifier.TransitionWitness>) (Iterable<?>) (var3)) {
            io.karatelabs.ext.rules.twin.RequiredBehaviorVerifier.PredicateResult var6 = evaluatePredicate(
               var1.predicate(),
               io.karatelabs.ext.rules.twin.CanonicalJson.d(var10.fromState()),
               io.karatelabs.ext.rules.twin.CanonicalJson.d(var10.toState()),
               io.karatelabs.ext.rules.twin.CanonicalJson.d(var10.transition().c())
            );
            if (!var6.passed()) {
               var var7 = createRowResult(var1, "failed");
               LinkedHashMap<java.lang.String, java.lang.String> var8 = new LinkedHashMap();
               var8.put("from", var10.fromProfile());
               var8.put("command", var10.transition().b());
               var8.put("to", var10.toProfile());
               var7.put("edge", var8);
               addPredicateError(var7, var6);
               return var7;
            }
         }

         return null;
      }
   }

   private Map<String, Object> invariantResult(TwinWalkResult var1, io.karatelabs.ext.rules.twin.RequiredBehaviorVerifier.RequiredBehaviorRow var2) {
      for (io.karatelabs.ext.rules.twin.TwinWalkResult.WalkState var4 : var1.e()) {
         io.karatelabs.ext.rules.twin.RequiredBehaviorVerifier.PredicateResult var5 = evaluatePredicate(var2.predicate(), io.karatelabs.ext.rules.twin.CanonicalJson.d(var4.c()));
         if (!var5.passed()) {
            var var6 = createRowResult(var2, "failed");
            var6.put("node", var4.a());
            addPredicateError(var6, var5);
            return var6;
         }
      }

      return null;
   }

   private static io.karatelabs.ext.rules.twin.RequiredBehaviorVerifier.PredicateResult evaluatePredicate(JavaCallable var0, Object... var1) {
      Object var2;
      try {
         var2 = var0.call(null, var1);
      } catch (RuntimeException var4) {
         return new io.karatelabs.ext.rules.twin.RequiredBehaviorVerifier.PredicateResult(false, io.karatelabs.ext.rules.twin.ThrowableUtils.a(var4));
      }

      return var2 instanceof Boolean var3 ? new io.karatelabs.ext.rules.twin.RequiredBehaviorVerifier.PredicateResult(var3, null) : new io.karatelabs.ext.rules.twin.RequiredBehaviorVerifier.PredicateResult(false, "the predicate returned no boolean");
   }

   private static Map<String, Object> createRowResult(io.karatelabs.ext.rules.twin.RequiredBehaviorVerifier.RequiredBehaviorRow var0, String var1) {
      var var2 = createStatusResult(var0.id(), var0.kind(), var1);
      addProse(var2, var0);
      return var2;
   }

   private static Map<String, Object> createStatusResult(String var0, String var1, String var2) {
      LinkedHashMap<java.lang.String, java.lang.Object> var3 = new LinkedHashMap();
      var3.put("id", var0);
      var3.put("kind", var1);
      var3.put("status", var2);
      return var3;
   }

   private static void addProse(Map<String, Object> var0, io.karatelabs.ext.rules.twin.RequiredBehaviorVerifier.RequiredBehaviorRow var1) {
      if (var1.prose() != null) {
         var0.put("prose", var1.prose());
      }
   }

   private static void addPredicateError(Map<String, Object> var0, io.karatelabs.ext.rules.twin.RequiredBehaviorVerifier.PredicateResult var1) {
      if (var1.error() != null) {
         var0.put("error", var1.error());
      }
   }

   private static Map<String, Object> coverageSummary(TwinWalkResult var0, io.karatelabs.ext.rules.twin.RequiredBehaviorVerifier.LaneCoverage var1) {
      var var2 = mapField(var0.k(), "states");
      int var3 = 0;

      for (Object var5 : var2.values()) {
         var3 += intField(var5, "observed") > 0 ? 1 : 0;
      }

      var var9 = mapField(var0.k(), "commands");
      int var10 = 0;
      int var6 = 0;

      for (Object var8 : var9.values()) {
         var10 += intField(var8, "oracle");
         var6 += intField(var8, "oracle") + intField(var8, "bare");
      }

      LinkedHashMap<java.lang.String, java.lang.Object> var11 = new LinkedHashMap();
      var11.put("model", ratio(var3, var2.size()));
      var11.put("oracle", ratio(var10, var6));
      var11.put("mock", ratio(var1.a(), var1.total()));
      var11.put("live", ratio(var1.liveObserved(), var1.total()));
      return var11;
   }

   private static List<Map<String, Object>> incompleteReasons(TwinWalkResult var0) {
      ArrayList<java.util.Map<java.lang.String,java.lang.Object>> var1 = new ArrayList();
      io.karatelabs.ext.rules.twin.TwinWalkResult.WalkCeiling var2 = var0.j();
      if (var2 != null && !"exhausted".equals(var2.a())) {
         LinkedHashMap<java.lang.String, java.lang.Object> var3 = new LinkedHashMap();
         var3.put("kind", "ceiling");
         var3.put("hit", var2.a());
         var3.put("alsoTripped", var2.b());
         var1.add(var3);
         LinkedHashMap<java.lang.String, java.lang.Object> var4 = new LinkedHashMap();
         var4.put("kind", "frontier");
         var4.put("frontier", var2.c());
         var1.add(var4);
      }

      for (Object var9 : listOrEmpty(mapField(var0.l(), "guards").get("notChecked"))) {
         LinkedHashMap<java.lang.String, java.lang.Object> var5 = new LinkedHashMap();
         var5.put("kind", "guardNotChecked");
         var5.put("command", var9);
         var1.add(var5);
      }

      for (Object var10 : listOrEmpty(var0.l() == null ? null : var0.l().get("undeclaredOpenFields"))) {
         if (var10 instanceof Map var11) {
            LinkedHashMap<java.lang.String, java.lang.Object> var6 = new LinkedHashMap();
            var6.put("kind", "undeclaredOpenField");
            var6.put("field", var11.get("field"));
            var6.put("distinct", var11.get("distinct"));
            var1.add(var6);
         }
      }

      return var1;
   }

   public static Map<String, Object> buildVerificationVerdict(List<?> var0, List<?> var1, List<?> var2, List<?> var3, List<?> var4, List<?> var5, TwinWalkResult var6) {
      var var7 = new ArrayList<>(disclosureProblems(var6));
      boolean var8 = var7.isEmpty();

      for (Object var10 : var0) {
         var7.add(formatIncompleteReason(var10));
      }

      for (Object var20 : var1) {
         Object var11 = var20 instanceof Map var12 ? var12.get("sequenceId") : null;
         var7.add(var11 == null ? "a sequence drifted" : "sequence " + var11 + " drifted");
      }

      for (Object var21 : var2) {
         Map<java.lang.String, java.lang.Object> var26 = var21 instanceof Map var31 ? var31 : Map.of();
         Object var32 = var26.get("lane");
         Object var13 = var32 == null ? var26.get("sequenceId") : var26.get("id");
         var7.add(var13 == null ? "a live mismatch was observed" : (var32 == null ? "live" : var32) + " mismatch on " + var13);
      }

      for (Object var22 : var3) {
         Map<java.lang.String, Object> var27 = var22 instanceof Map var33 ? var33 : Map.of();
         var7.add(var27.get("kind") + " on " + var27.get("id"));
      }

      for (Object var23 : var4) {
         Map<java.lang.String, Object> var28 = var23 instanceof Map var34 ? var34 : Map.of();
         var7.add(var28.get("kind") + " on " + var28.get("id"));
      }

      for (Object var24 : var5) {
         Object var29 = var24 instanceof Map var35 ? var35.get("id") : null;
         var7.add("sequence " + var29 + " _expect mismatches the model");
      }

      boolean var19 = !var0.isEmpty() || !var1.isEmpty() || !var2.isEmpty() || !var3.isEmpty() || !var4.isEmpty() || !var5.isEmpty();
      String var25 = !var8 ? "INVALID" : (var19 ? "FAIL" : "PASS");
      LinkedHashMap<java.lang.String, java.lang.Object> var30 = new LinkedHashMap();
      var30.put("verdict", var25);
      var30.put("pass", "PASS".equals(var25));
      var30.put("reasons", var7);
      return var30;
   }

   private static List<String> disclosureProblems(TwinWalkResult var0) {
      Map<java.lang.String, Object> var1 = var0 == null ? null : var0.l();
      ArrayList<java.lang.String> var2 = new ArrayList();

      for (String var4 : REQUIRED_DISCLOSURE_KEYS) {
         if (var1 == null || var1.get(var4) == null) {
            var2.add("disclosure " + var4 + " is missing");
         }
      }

      if ((var1 == null ? null : var1.get("identity")) instanceof Map var6 && !"content".equals(var6.get("kind"))) {
         var2.add("disclosure identity does not name content identity");
      }

      return var2;
   }

   private static String formatIncompleteReason(Object var0) {
      Map<java.lang.String, Object> var1 = (Map)var0;
      String var2 = "required " + var1.get("id") + " (" + var1.get("kind") + ") ";
      if ("notreached".equals(var1.get("status"))) {
         return var2 + "notreached" + ("req".equals(var1.get("kind")) ? " — no command carrying it applied" : "");
      } else {
         return var1.get("edge") instanceof Map var3
            ? var2 + "failed on edge " + var3.get("from") + " →" + var3.get("command") + "→ " + var3.get("to")
            : var2 + "failed at node " + var1.get("node");
      }
   }

   private static io.karatelabs.ext.rules.twin.RequiredBehaviorVerifier.RequiredBehaviorRow parseRow(Object var0, TwinModel var1, Engine var2, Set<String> var3) {
      if (!(var0 instanceof Map var4)) {
         throw TwinException.b(null, "rows", "every row must be an object");
      } else if (!(var4.get("id") instanceof String var5 && !var5.isEmpty())) {
         throw TwinException.b(null, "id", "must be a non-empty string");
      } else if (!var3.add(var5)) {
         throw TwinException.b(var5, "id", "is declared twice");
      } else if (var4.get("kind") instanceof String var19 && ALLOWED_ROW_KEYS.containsKey(var19)) {
         for (Object var8 : var4.keySet()) {
            if (!ALLOWED_ROW_KEYS.get(var19).contains(var8)) {
               throw TwinException.b(var5, String.valueOf(var8), "unknown key for a " + var19 + " row");
            }
         }

         io.karatelabs.ext.rules.twin.RequiredBehaviorVerifier.UndeclaredMarker var21 = new io.karatelabs.ext.rules.twin.RequiredBehaviorVerifier.UndeclaredMarker();
         String var22 = parseStateReference(var5, var4, "state", var1, var21);
         String var9 = parseStateReference(var5, var4, "from", var1, var21);
         String var10 = parseStateReference(var5, var4, "to", var1, var21);
         io.karatelabs.ext.rules.twin.RequiredBehaviorVerifier.OperationSelector var11 = parseOperationSelector(var5, var4, var1, var21);
         var var12 = parseAppliesTo(var5, var4);
         JavaCallable var13 = compilePredicate(var5, var4, var2);
         var var14 = optionalMap(var5, var4, "request");
         io.karatelabs.ext.rules.twin.RequiredBehaviorVerifier.ExpectedOutcome var15 = parseExpectedOutcome(var5, var4, var19);
         boolean var16 = var11.routePattern() != null || var14 != null || var15 != null;
         switch (var19) {
            case "state":
               if (var22 == null) {
                  throw TwinException.b(var5, "state", "is required");
               }
               break;
            case "transition":
               if (!var16 && var9 == null) {
                  throw TwinException.b(var5, "from", "is required");
               }

               if (!var16 && var10 == null) {
                  throw TwinException.b(var5, "to", "is required");
               }

               if (!var16 && !var11.declared()) {
                  throw TwinException.b(var5, "operation", "operation or a non-empty alternatives is required");
               }
               break;
            case "rejection":
               if (!var16 && !var11.declared()) {
                  throw TwinException.b(var5, "operation", "is required");
               }
               break;
            case "assertion":
               if (var12.isEmpty()) {
                  throw TwinException.b(var5, "appliesTo", "at least one transition row is required");
               }

               if (var13 == null) {
                  throw TwinException.b(var5, "predicate", "is required");
               }
               break;
            default:
               if (var13 == null) {
                  throw TwinException.b(var5, "predicate", "is required");
               }
         }

         return new io.karatelabs.ext.rules.twin.RequiredBehaviorVerifier.RequiredBehaviorRow(
            var5,
            var19,
            optionalString(var5, var4, "prose"),
            var22,
            var9,
            var10,
            var11.commands(),
            var11.routePattern(),
            optionalMap(var5, var4, "args"),
            optionalMap(var5, var4, "world"),
            var14,
            var15,
            var12,
            var13,
            optionalString(var5, var4, "reason"),
            var21.a
         );
      } else {
         throw TwinException.b(var5, "kind", "must be one of " + String.join(" | ", VALID_ROW_KINDS));
      }
   }

   private static io.karatelabs.ext.rules.twin.RequiredBehaviorVerifier.OperationSelector parseOperationSelector(String var0, Map<?, ?> var1, TwinModel var2, io.karatelabs.ext.rules.twin.RequiredBehaviorVerifier.UndeclaredMarker var3) {
      ArrayList var4 = new ArrayList();
      ArrayList var5 = new ArrayList();
      io.karatelabs.ext.rules.twin.RequiredBehaviorVerifier.RoutePattern var6 = null;
      boolean var7 = false;
      if (var1.containsKey("operation")) {
         if (!(var1.get("operation") instanceof String var8)) {
            throw TwinException.b(var0, "operation", "must be a string");
         }

         var7 = true;
         var6 = parseRoutePattern(var8);
         if (var6 == null) {
            (var2.f().containsKey(var8) ? var4 : var5).add(var8);
         }
      }

      if (var1.containsKey("alternatives")) {
         if (!(var1.get("alternatives") instanceof List var12) || var12.isEmpty()) {
            throw TwinException.b(var0, "alternatives", "must be a non-empty array");
         }

         var7 = true;

         for (Object var10 : var12) {
            if (!(var10 instanceof String var11)) {
               throw TwinException.b(var0, "alternatives", "must be strings");
            }

            (var2.f().containsKey(var11) ? var4 : var5).add(var11);
         }
      }

      if (var4.isEmpty() && !var5.isEmpty()) {
         var3.markIfAbsent("command", (String)var5.get(0));
      }

      return new io.karatelabs.ext.rules.twin.RequiredBehaviorVerifier.OperationSelector(List.copyOf(var4), var6, var7);
   }

   private static io.karatelabs.ext.rules.twin.RequiredBehaviorVerifier.ExpectedOutcome parseExpectedOutcome(String var0, Map<?, ?> var1, String var2) {
      if (!var1.containsKey("outcome")) {
         return null;
      } else if (var1.get("outcome") instanceof Map var3) {
         for (Object var5 : var3.keySet()) {
            if (!ALLOWED_OUTCOME_KEYS.contains(var5)) {
               throw TwinException.b(var0, "outcome", "unknown key '" + var5 + "'");
            }
         }

         String var10 = "transition".equals(var2) ? "applied" : "refused";
         if (var3.containsKey("kind") && !var10.equals(var3.get("kind"))) {
            throw TwinException.b(var0, "outcome", "kind must be '" + var10 + "' on a " + var2 + " row");
         } else {
            Integer var11 = null;
            if (var3.containsKey("status")) {
               if (!(var3.get("status") instanceof Number var6)) {
                  throw TwinException.b(var0, "outcome", "status must be a number");
               }

               var11 = var6.intValue();
            }

            if (var3.containsKey("reason") && !(var3.get("reason") instanceof String)) {
               throw TwinException.b(var0, "outcome", "reason must be a string");
            } else if ("applied".equals(var10) && var3.containsKey("reason")) {
               throw TwinException.b(var0, "outcome", "reason reads a refusal, never an applied step");
            } else {
               Object var12 = var3.get("body");
               if (var12 != null) {
                  try {
                     io.karatelabs.ext.rules.twin.CanonicalJson.b(var12);
                  } catch (SnapshotException var8) {
                     throw TwinException.b(var0, "outcome", "body: " + var8.getMessage());
                  }

                  var12 = io.karatelabs.ext.rules.twin.CanonicalJson.d(var12);
               }

               return new io.karatelabs.ext.rules.twin.RequiredBehaviorVerifier.ExpectedOutcome(var10, var11, var12, (String)var3.get("reason"));
            }
         }
      } else {
         throw TwinException.b(var0, "outcome", "must be an object");
      }
   }

   private static String optionalString(String var0, Map<?, ?> var1, String var2) {
      if (!var1.containsKey(var2)) {
         return null;
      } else if (var1.get(var2) instanceof String var3) {
         return var3;
      } else {
         throw TwinException.b(var0, var2, "must be a string");
      }
   }

   private static String parseStateReference(String var0, Map<?, ?> var1, String var2, TwinModel var3, io.karatelabs.ext.rules.twin.RequiredBehaviorVerifier.UndeclaredMarker var4) {
      String var5 = optionalString(var0, var1, var2);
      if (var5 == null) {
         return null;
      } else {
         for (io.karatelabs.ext.rules.twin.TwinModel.StateModel var7 : var3.b()) {
            if (var7.a().equals(var5)) {
               return var5;
            }
         }

         var4.markIfAbsent("state", var5);
         return var5;
      }
   }

   private static List<String> parseAppliesTo(String var0, Map<?, ?> var1) {
      if (!var1.containsKey("appliesTo")) {
         return List.of();
      } else if (var1.get("appliesTo") instanceof List var2) {
         ArrayList<java.lang.String> var7 = new ArrayList(var2.size());

         for (Object var5 : var2) {
            if (!(var5 instanceof String var6)) {
               throw TwinException.b(var0, "appliesTo", "must be strings");
            }

            var7.add(var6);
         }

         return var7;
      } else {
         throw TwinException.b(var0, "appliesTo", "must be an array");
      }
   }

   private static Map<String, Object> optionalMap(String var0, Map<?, ?> var1, String var2) {
      if (!var1.containsKey(var2)) {
         return null;
      } else if (var1.get(var2) instanceof Map var3) {
         try {
            io.karatelabs.ext.rules.twin.CanonicalJson.b(var3);
         } catch (SnapshotException var5) {
            throw TwinException.b(var0, var2, var5.getMessage());
         }

         return asMap(io.karatelabs.ext.rules.twin.CanonicalJson.d(var3));
      } else {
         throw TwinException.b(var0, var2, "must be an object");
      }
   }

   private static JavaCallable compilePredicate(String var0, Map<?, ?> var1, Engine var2) {
      if (!var1.containsKey("predicate")) {
         return null;
      } else if (var1.get("predicate") instanceof String var3) {
         Object var7;
         try {
            var7 = var2.eval("(" + var3 + ")");
         } catch (RuntimeException var6) {
            throw TwinException.b(var0, "predicate", "does not compile");
         }

         if (var7 instanceof JavaCallable var5) {
            return var5;
         } else {
            throw TwinException.b(var0, "predicate", "must evaluate to a function");
         }
      } else {
         throw TwinException.b(var0, "predicate", "must be a JS function source");
      }
   }

   private static Map<String, Object> ratio(int var0, int var1) {
      LinkedHashMap<java.lang.String, java.lang.Object> var2 = new LinkedHashMap();
      var2.put("count", var0);
      var2.put("of", var1);
      return var2;
   }

   private static int intField(Object var0, String var1) {
      return var0 instanceof Map var3 && var3.get(var1) instanceof Number var2 ? var2.intValue() : 0;
   }

   private static Map<?, ?> mapField(Map<String, Object> var0, String var1) {
      return var0 != null && var0.get(var1) instanceof Map var2 ? var2 : Map.of();
   }

   private static List<?> listOrEmpty(Object var0) {
      return var0 instanceof List var1 ? var1 : List.of();
   }

   private static Map<String, Object> asMap(Object var0) {
      return (Map<String, Object>)var0;
   }

   public record LaneCoverage(int mockObserved, int liveObserved, int total) {
      public static final io.karatelabs.ext.rules.twin.RequiredBehaviorVerifier.LaneCoverage a = new io.karatelabs.ext.rules.twin.RequiredBehaviorVerifier.LaneCoverage(0, 0, 0);

      public LaneCoverage(int mockObserved, int liveObserved, int total) {
         this.mockObserved = mockObserved;
         this.liveObserved = liveObserved;
         this.total = total;
      }

      public int a() {
         return this.mockObserved;
      }

      public int liveObserved() {
         return this.liveObserved;
      }

      public int total() {
         return this.total;
      }
   }

   private record TransitionWitness(io.karatelabs.ext.rules.twin.TwinWalkResult.WalkTransition transition, Object fromState, Object toState, String fromProfile, String toProfile) {

      private TransitionWitness(io.karatelabs.ext.rules.twin.TwinWalkResult.WalkTransition transition, Object fromState, Object toState, String fromProfile, String toProfile) {
         this.transition = transition;
         this.fromState = fromState;
         this.toState = toState;
         this.fromProfile = fromProfile;
         this.toProfile = toProfile;
      }
   }

   private record OperationSelector(List<String> commands, io.karatelabs.ext.rules.twin.RequiredBehaviorVerifier.RoutePattern routePattern, boolean declared) {

      private OperationSelector(List<String> commands, io.karatelabs.ext.rules.twin.RequiredBehaviorVerifier.RoutePattern routePattern, boolean declared) {
         this.commands = commands;
         this.routePattern = routePattern;
         this.declared = declared;
      }
   }

   private record ExpectedOutcome(String kind, Integer status, Object body, String reason) {

      private ExpectedOutcome(String kind, Integer status, Object body, String reason) {
         this.kind = kind;
         this.status = status;
         this.body = body;
         this.reason = reason;
      }
   }

   private record PredicateResult(boolean passed, String error) {

      private PredicateResult(boolean passed, String error) {
         this.passed = passed;
         this.error = error;
      }
   }

   private record RequiredBehaviorRow(String id, String kind, String prose, String state, String from, String to, List<String> commands, io.karatelabs.ext.rules.twin.RequiredBehaviorVerifier.RoutePattern routePattern, Map<String, Object> args, Map<String, Object> world, Map<String, Object> request, io.karatelabs.ext.rules.twin.RequiredBehaviorVerifier.ExpectedOutcome expectedOutcome, List<String> appliesTo, JavaCallable predicate, String reason, String undeclaredReason) {

      private RequiredBehaviorRow(String id, String kind, String prose, String state, String from, String to, List<String> commands, io.karatelabs.ext.rules.twin.RequiredBehaviorVerifier.RoutePattern routePattern, Map<String, Object> args, Map<String, Object> world, Map<String, Object> request, io.karatelabs.ext.rules.twin.RequiredBehaviorVerifier.ExpectedOutcome expectedOutcome, List<String> appliesTo, JavaCallable predicate, String reason, String undeclaredReason) {
         this.id = id;
         this.kind = kind;
         this.prose = prose;
         this.state = state;
         this.from = from;
         this.to = to;
         this.commands = commands;
         this.routePattern = routePattern;
         this.args = args;
         this.world = world;
         this.request = request;
         this.expectedOutcome = expectedOutcome;
         this.appliesTo = appliesTo;
         this.predicate = predicate;
         this.reason = reason;
         this.undeclaredReason = undeclaredReason;
      }
   }

   private static final class UndeclaredMarker {
      private String a;

      void markIfAbsent(String var1, String var2) {
         if (this.a == null) {
            this.a = "undeclared:" + var1 + ":" + var2;
         }
      }
   }

   private record RoutePattern(String method, List<String> pathSegments) {

      private RoutePattern(String method, List<String> pathSegments) {
         this.method = method;
         this.pathSegments = pathSegments;
      }
   }
}
