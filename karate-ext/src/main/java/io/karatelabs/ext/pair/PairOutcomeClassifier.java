package io.karatelabs.ext.pair;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.regex.Pattern;

public final class PairOutcomeClassifier {
   public static final String AGREED = "agreed";
   public static final String STALE_MOCK = "staleMock";
   public static final String MOCK_BUG = "mockBug";
   public static final String AGREED_FAILURE = "agreedFailure";
   public static final String NOT_ASSERTED_DIVERGENCE = "notassertedDivergence";
   public static final String NOT_MEASURED = "notmeasured";
   public static final List<String> OUTCOME_ORDER = List.of("agreed", "staleMock", "mockBug", "notassertedDivergence", "agreedFailure", "notmeasured");
   private static final int j = 240;
   public static final String ABSENT_PLACEHOLDER = "(absent)";
   public static final int ABSENT_STATUS = -1;
   private static final Set<Integer> RETRYABLE_HTTP_STATUSES = Set.of(401, 403, 407, 429, 502, 503, 504);
   private static final List<String> TRANSPORT_FAILURE_MARKERS = List.of(
      "connection refused",
      "connect timed out",
      "unknownhost",
      "no route to host",
      "connectexception",
      "sockettimeout",
      "sslhandshake",
      "network is unreachable",
      "read timed out",
      "too many requests"
   );
   private static final Pattern ASSERTION_PREFIX_PATTERN = Pattern.compile("^\\s*(status|match|assert)\\b", 2);

   private PairOutcomeClassifier() {
   }

   public static io.karatelabs.ext.pair.PairOutcomeClassifier.PairClassificationResult classify(PairRunEvidence var0, PairRunEvidence var1, NondeterminismDetector var2, ContractSchema var3, io.karatelabs.ext.pair.DataProfile var4, io.karatelabs.ext.pair.PairOutcomeClassifier.ConfirmationRunner var5) {
      return classify(var0, var1, var2, var3, var4, var5, io.karatelabs.ext.pair.EndpointAliasSet.EMPTY);
   }

   public static io.karatelabs.ext.pair.PairOutcomeClassifier.PairClassificationResult classify(
      PairRunEvidence var0, PairRunEvidence var1, NondeterminismDetector var2, ContractSchema var3, io.karatelabs.ext.pair.DataProfile var4, io.karatelabs.ext.pair.PairOutcomeClassifier.ConfirmationRunner var5, io.karatelabs.ext.pair.EndpointAliasSet var6
   ) {
      if (var0.f() == null && var1.f() == null) {
         if (!var0.e().isEmpty() && !var1.e().isEmpty()) {
            var var7 = var1.operationsById();
            LinkedHashSet<java.lang.String> var8 = new LinkedHashSet();
            ArrayList<io.karatelabs.ext.pair.PairOutcomeClassifier.ScenarioClassification> var9 = new ArrayList();

            for (Entry var11 : var0.operationsById().entrySet()) {
               var8.add((String)var11.getKey());
               var9.add(a((io.karatelabs.ext.pair.PairRunEvidence.ScenarioRunEvidence)var11.getValue(), (io.karatelabs.ext.pair.PairRunEvidence.ScenarioRunEvidence)var7.get(var11.getKey()), var2, var3, var4, var5, var0.c(), var1.c(), var6));
            }

            for (Entry var14 : (Iterable<Entry>) (Iterable<?>) (var7.entrySet())) {
               io.karatelabs.ext.pair.PairRunEvidence.ScenarioRunEvidence var12 = (io.karatelabs.ext.pair.PairRunEvidence.ScenarioRunEvidence)var14.getValue();
               if (!var8.contains(var14.getKey())) {
                  var9.add(
                     new io.karatelabs.ext.pair.PairOutcomeClassifier.ScenarioClassification(
                        var12.e(),
                        var12.f(),
                        var12.h(),
                        "notmeasured",
                        "notpaired",
                        "the mock leg ran this scenario and the provider leg did not — the two legs did not execute the same suite, so there is nothing to compare",
                        List.of(),
                        List.of(),
                        null
                     )
                  );
               }
            }

            return new io.karatelabs.ext.pair.PairOutcomeClassifier.PairClassificationResult(var9);
         } else {
            throw new PairException(
               "no pair: the "
                  + (var0.e().isEmpty() ? "provider" : "mock")
                  + " leg ran no scenario, so there is nothing to compare. An empty divergence set would read as evidence of fidelity — check that the suite paths resolve and that the tag selection matches."
            );
         }
      } else {
         throw new PairException(
            "no pair: the "
               + (var0.f() != null ? "provider" : "mock")
               + " leg produced no run ("
               + (var0.f() != null ? var0.f() : var1.f())
               + "). A leg that cannot run yields NO PAIR, not an empty divergence set — an empty one would read as evidence of fidelity."
         );
      }
   }

   private static io.karatelabs.ext.pair.PairOutcomeClassifier.ScenarioClassification a(
      io.karatelabs.ext.pair.PairRunEvidence.ScenarioRunEvidence var0,
      io.karatelabs.ext.pair.PairRunEvidence.ScenarioRunEvidence var1,
      NondeterminismDetector var2,
      ContractSchema var3,
      io.karatelabs.ext.pair.DataProfile var4,
      io.karatelabs.ext.pair.PairOutcomeClassifier.ConfirmationRunner var5,
      String var6,
      String var7,
      io.karatelabs.ext.pair.EndpointAliasSet var8
   ) {
      if (var1 == null) {
         return a(var0, "notpaired", "the provider leg ran this scenario and the mock leg did not");
      } else if (!var0.isSkipped() && !var1.isSkipped()) {
         String var9 = io.karatelabs.ext.pair.EndpointIdentityResolver.a(var0, var6, "provider", var8);
         if (var9 == null) {
            var9 = io.karatelabs.ext.pair.EndpointIdentityResolver.a(var1, var7, "mock", var8);
         }

         if (var9 != null) {
            return a(var0, "targetNotAddressed", var9);
         } else {
            String var10 = b(var0, "provider");
            if (var10 == null) {
               var10 = b(var1, "mock");
            }

            if (var10 != null) {
               return a(var0, "environment", var10);
            } else {
               String var11 = a(var0, "provider");
               if (var11 == null) {
                  var11 = a(var1, "mock");
               }

               if (var11 != null) {
                  return a(var0, "setupFailure", var11);
               } else {
                  if (var4 == io.karatelabs.ext.pair.DataProfile.AMBIENT) {
                     String var12 = a(var0);
                     if (var12 != null) {
                        return a(var0, "ambientRead", var12);
                     }
                  }

                  ArrayList var20 = new ArrayList();
                  List var13 = a(var0.passedObservations(), var1.passedObservations(), var3, var2, var20, "/probe");
                  int var14 = Math.min(var0.passedObservations().size(), var1.passedObservations().size());
                  if (!var13.isEmpty()) {
                     return new io.karatelabs.ext.pair.PairOutcomeClassifier.ScenarioClassification(
                        var0.e(),
                        var0.f(),
                        var0.h(),
                        "notmeasured",
                        "preconditionMismatch",
                        "the two legs disagreed in the Background — the precondition probe — so this scenario was never in a comparable state and nothing about its behaviour is measured. Read the differences on this row first. They report what disagreed, and two very different causes look alike from here. (a) Leftover state — a stand-in carrying the previous pair's rows; a mock you started holds its state until mock.restart() (same port, the handle stays valid), and a provider only you can restart. (b) A real difference between the two implementations that happens to surface in a read-back — server-minted ids being the classic one. (b) is a genuine finding this class cannot report as one, and the answer to it is an `ignore` rule with a reason and an owner, which re-classifies the difference here exactly as it does anywhere else.",
                        var13,
                        var20,
                        null,
                        Math.max(var0.passedObservations().size(), var1.passedObservations().size()),
                        false
                     );
                  } else {
                     List var15 = a(var0.failedObservations(), var1.failedObservations(), var3, var2, var20, "");
                     String var16 = a(var0, var1);
                     if (var16 != null) {
                        String var17 = var0.isPassed() ? "mockBug" : "staleMock";
                        String var18 = var0.isPassed() ? "mock" : "provider";
                        io.karatelabs.ext.pair.PairOutcomeClassifier.ConfirmationOutcome var19 = var5.confirm(var0.isPassed() ? var1 : var0, var18);
                        if (var19 == io.karatelabs.ext.pair.PairOutcomeClassifier.ConfirmationOutcome.FLAKY) {
                           return new io.karatelabs.ext.pair.PairOutcomeClassifier.ScenarioClassification(
                              var0.e(),
                              var0.f(),
                              var0.h(),
                              "notmeasured",
                              "flaky",
                              "the "
                                 + var18
                                 + " leg's failure did not reproduce on a confirmation re-run, so this is flakiness rather than divergence. It is quarantined, and it is not counted as agreement",
                              var15,
                              var20,
                              var19,
                              var14
                           );
                        } else {
                           return var19 == io.karatelabs.ext.pair.PairOutcomeClassifier.ConfirmationOutcome.NOT_COMPARABLE
                              ? new io.karatelabs.ext.pair.PairOutcomeClassifier.ScenarioClassification(
                                 var0.e(),
                                 var0.f(),
                                 var0.h(),
                                 "notmeasured",
                                 "preconditionMismatch",
                                 "the confirmation re-run met different state than the paired run did, so it says nothing about flakiness in either direction",
                                 var15,
                                 var20,
                                 var19,
                                 var14
                              )
                              : new io.karatelabs.ext.pair.PairOutcomeClassifier.ScenarioClassification(var0.e(), var0.f(), var0.h(), var17, null, var16, var15, var20, var19, var14);
                        }
                     } else if (!var0.isPassed()) {
                        return new io.karatelabs.ext.pair.PairOutcomeClassifier.ScenarioClassification(
                           var0.e(),
                           var0.f(),
                           var0.h(),
                           "agreedFailure",
                           null,
                           "both legs failed the same way — agreement, not divergence: " + b(var0.k()),
                           var15,
                           var20,
                           null,
                           var14
                        );
                     } else {
                        return !var15.isEmpty()
                           ? new io.karatelabs.ext.pair.PairOutcomeClassifier.ScenarioClassification(
                              var0.e(),
                              var0.f(),
                              var0.h(),
                              "notassertedDivergence",
                              null,
                              "both legs passed and the responses differ — the assertions are too loose to see it. The owner of this finding is the SUITE, not either target.",
                              var15,
                              var20,
                              null,
                              var14
                           )
                           : new io.karatelabs.ext.pair.PairOutcomeClassifier.ScenarioClassification(var0.e(), var0.f(), var0.h(), "agreed", null, null, List.of(), var20, null, var14);
                     }
                  }
               }
            }
         }
      } else {
         return a(var0, "notRun", "a leg skipped this scenario, so it produced no verdict");
      }
   }

   private static io.karatelabs.ext.pair.PairOutcomeClassifier.ScenarioClassification a(io.karatelabs.ext.pair.PairRunEvidence.ScenarioRunEvidence var0, String var1, String var2) {
      return new io.karatelabs.ext.pair.PairOutcomeClassifier.ScenarioClassification(var0.e(), var0.f(), var0.h(), "notmeasured", var1, var2, List.of(), List.of(), null);
   }

   private static String a(io.karatelabs.ext.pair.PairRunEvidence.ScenarioRunEvidence var0, io.karatelabs.ext.pair.PairRunEvidence.ScenarioRunEvidence var1) {
      int var2 = Math.max(var0.l().size(), var1.l().size());

      for (int var3 = 0; var3 < var2; var3++) {
         io.karatelabs.ext.pair.PairRunEvidence.ScenarioStepEvidence var4 = var3 < var0.l().size() ? var0.l().get(var3) : null;
         io.karatelabs.ext.pair.PairRunEvidence.ScenarioStepEvidence var5 = var3 < var1.l().size() ? var1.l().get(var3) : null;
         boolean var6 = var4 != null && var4.isFailed();
         boolean var7 = var5 != null && var5.isFailed();
         if (var6 != var7) {
            io.karatelabs.ext.pair.PairRunEvidence.ScenarioStepEvidence var8 = var6 ? var4 : var5;
            return "step " + (var3 + 1) + " `" + var8.d() + "` failed on the " + (var6 ? "provider" : "mock") + " leg and passed on the other: " + b(var8.g());
         }
      }

      return null;
   }

   private static List<Map<String, Object>> a(List<io.karatelabs.ext.pair.PairRunEvidence.RecordedHttpExchange> var0, List<io.karatelabs.ext.pair.PairRunEvidence.RecordedHttpExchange> var1, ContractSchema var2, NondeterminismDetector var3, List<Map<String, Object>> var4, String var5) {
      ArrayList<java.util.Map<java.lang.String,java.lang.Object>> var6 = new ArrayList();
      int var7 = Math.max(var0.size(), var1.size());

      for (int var8 = 0; var8 < var7; var8++) {
         String var9 = var5 + "/exchanges/" + var8;
         io.karatelabs.ext.pair.PairRunEvidence.RecordedHttpExchange var10 = var8 < var0.size() ? (io.karatelabs.ext.pair.PairRunEvidence.RecordedHttpExchange)var0.get(var8) : null;
         io.karatelabs.ext.pair.PairRunEvidence.RecordedHttpExchange var11 = var8 < var1.size() ? (io.karatelabs.ext.pair.PairRunEvidence.RecordedHttpExchange)var1.get(var8) : null;
         if (var10 != null && var11 != null) {
            String var17 = a(var2, var10);
            String var13 = a(var2, var11);
            if (!var17.equals(var13)) {
               var6.add(a(var9, var17, var13, "the legs called different operations at the same point in the scenario — the call sequence itself diverged"));
            } else {
               a(var9 + "/response/status", "/response/status", var10.f(), var11.f(), var3, var6, var4);
               var var14 = new LinkedHashSet<>(var2.responseHeaderNames(var10.c(), var10.d(), var10.f()));
               if (var10.f() != var11.f()) {
                  var14.addAll(var2.responseHeaderNames(var11.c(), var11.d(), var11.f()));
               }

               for (String var16 : (Iterable<String>) (Iterable<?>) (var14)) {
                  a(var9 + "/response/headers/" + var16, "/response/headers/" + var16, var10.g().get(var16), var11.g().get(var16), var3, var6, var4);
               }

               a(var9 + "/response/body", "/response/body", var10.h(), var11.h(), var3, var6, var4);
            }
         } else {
            io.karatelabs.ext.pair.PairRunEvidence.RecordedHttpExchange var12 = var10 != null ? var10 : var11;
            var6.add(
               a(
                  var9,
                  var10 == null ? null : a(var2, var10),
                  var11 == null ? null : a(var2, var11),
                  "one leg made a call the other did not: " + var12.c() + " " + var12.e()
               )
            );
         }
      }

      return var6;
   }

   public static boolean samePreconditions(io.karatelabs.ext.pair.PairRunEvidence.ScenarioRunEvidence var0, io.karatelabs.ext.pair.PairRunEvidence.ScenarioRunEvidence var1, ContractSchema var2, NondeterminismDetector var3) {
      return a(var0.passedObservations(), var1.passedObservations(), var2, var3.copy(), new ArrayList<>(), "/probe").isEmpty();
   }

   private static String a(ContractSchema var0, io.karatelabs.ext.pair.PairRunEvidence.RecordedHttpExchange var1) {
      return var0.operationLabel(var1.c(), var1.d(), var1.e());
   }

   private static void a(String var0, String var1, Object var2, Object var3, NondeterminismDetector var4, List<Map<String, Object>> var5, List<Map<String, Object>> var6) {
      if (!a(var2, var3)) {
         if (var2 instanceof Map var7 && var3 instanceof Map var18) {
            LinkedHashSet<java.lang.String> var19 = new LinkedHashSet();
            var7.keySet().forEach(var1x -> var19.add(String.valueOf(var1x)));
            var18.keySet().forEach(var1x -> var19.add(String.valueOf(var1x)));

            for (String var11 : (Iterable<String>) (Iterable<?>) (var19)) {
               String var12 = "/" + var11.replace("~", "~0").replace("/", "~1");
               boolean var13 = a(var7, var11);
               boolean var14 = a(var18, var11);
               if (var13 != var14) {
                  var5.add(
                     a(
                        var0 + var12,
                        var13 ? var7.get(var11) : "(absent)",
                        var14 ? var18.get(var11) : "(absent)",
                        "the field is present on the "
                           + (var13 ? "provider" : "mock")
                           + " leg only — an explicit null and an absent key are different responses to a client that checks"
                     )
                  );
               } else {
                  a(var0 + var12, var1 + var12, var7.get(var11), var18.get(var11), var4, var5, var6);
               }
            }
         } else if (var2 instanceof List var15 && var3 instanceof List var17) {
            if (var15.size() != var17.size()) {
               var5.add(a(var0 + "/length", var15.size(), var17.size(), "the two legs returned different numbers of elements"));
            }

            for (int var9 = 0; var9 < Math.min(var15.size(), var17.size()); var9++) {
               a(var0 + "/" + var9, var1 + "/" + var9, var15.get(var9), var17.get(var9), var4, var5, var6);
            }
         } else {
            io.karatelabs.ext.pair.NondeterminismDetector.IgnoreRule var16 = var4.matchRule(var1, var2, var3);
            if (var16 != null) {
               var var8 = a(var0, var2, var3, null);
               var8.put("ignoredBy", var16.b());
               var8.put("reason", var16.f());
               var8.put("owner", var16.g());
               var6.add(var8);
            } else {
               var5.add(a(var0, var2, var3, null));
            }
         }
      }
   }

   private static boolean a(Map<?, ?> var0, String var1) {
      for (Object var3 : var0.keySet()) {
         if (var1.equals(String.valueOf(var3))) {
            return true;
         }
      }

      return false;
   }

   private static Map<String, Object> a(String var0, Object var1, Object var2, String var3) {
      LinkedHashMap<java.lang.String, java.lang.Object> var4 = new LinkedHashMap();
      var4.put("at", var0);
      var4.put("provider", a(var1));
      var4.put("mock", a(var2));
      if (var3 != null) {
         var4.put("note", var3);
      }

      return var4;
   }

   private static boolean a(Object var0, Object var1) {
      if (var0 != null && var1 != null) {
         if (!(var0 instanceof Number var2 && var1 instanceof Number var3)) {
            return var0.equals(var1);
         } else {
            return a(var2) && a(var3)
               ? new BigDecimal(var2.toString()).compareTo(new BigDecimal(var3.toString())) == 0
               : var2.doubleValue() == var3.doubleValue();
         }
      } else {
         return var0 == var1;
      }
   }

   private static boolean a(Number var0) {
      return var0 instanceof Integer
         || var0 instanceof Long
         || var0 instanceof Short
         || var0 instanceof Byte
         || var0 instanceof BigInteger
         || var0 instanceof BigDecimal var1 && var1.scale() <= 0;
   }

   private static Object a(Object var0) {
      if (var0 instanceof String var2) {
         return a(var2);
      } else if (!(var0 instanceof Map) && !(var0 instanceof List)) {
         return var0;
      } else {
         String var1 = String.valueOf(var0);
         return var1.length() > 240 ? a(var1) : var0;
      }
   }

   private static String a(String var0) {
      return var0.length() > 240 ? var0.substring(0, 240) + " …(elided)" : var0;
   }

   private static String a(io.karatelabs.ext.pair.PairRunEvidence.ScenarioRunEvidence var0, String var1) {
      return !var0.isPassed() && var0.m().isEmpty()
         ? "the "
            + var1
            + " leg's run of this scenario failed before it made a single HTTP call ("
            + b(var0.k())
            + ") — a setup or harness error, not a verdict about behaviour. Nothing here was compared: fix the scenario's setup and re-mint."
         : null;
   }

   private static String b(io.karatelabs.ext.pair.PairRunEvidence.ScenarioRunEvidence var0, String var1) {
      if (var0.isPassed()) {
         return null;
      } else {
         String var2 = var0.k() == null ? "" : var0.k().toLowerCase(Locale.ROOT);

         for (String var4 : TRANSPORT_FAILURE_MARKERS) {
            if (var2.contains(var4)) {
               return "the " + var1 + " leg could not reach its target (" + b(var0.k()) + ") — an environment failure, which is not a verdict about behaviour";
            }
         }

         if ("provider".equals(var1) && !var0.m().isEmpty()) {
            io.karatelabs.ext.pair.PairRunEvidence.RecordedHttpExchange var5 = var0.m().get(var0.m().size() - 1);
            return RETRYABLE_HTTP_STATUSES.contains(var5.f()) && !a(var0, var5.a())
               ? "the "
                  + var1
                  + " leg was refused by its environment (HTTP "
                  + var5.f()
                  + " on "
                  + var5.c()
                  + " "
                  + var5.e()
                  + ") — auth, a rate limit or an unavailable dependency, which is not a verdict about behaviour"
               : null;
         } else {
            return null;
         }
      }
   }

   private static boolean a(io.karatelabs.ext.pair.PairRunEvidence.ScenarioRunEvidence var0, int var1) {
      for (io.karatelabs.ext.pair.PairRunEvidence.ScenarioStepEvidence var3 : var0.l()) {
         if (var3.c() > var1 && var3.isPassed() && ASSERTION_PREFIX_PATTERN.matcher(var3.d()).find()) {
            return true;
         }
      }

      return false;
   }

   private static String a(io.karatelabs.ext.pair.PairRunEvidence.ScenarioRunEvidence var0) {
      boolean var1 = false;

      for (io.karatelabs.ext.pair.PairRunEvidence.RecordedHttpExchange var3 : var0.m()) {
         String var4 = var3.c();
         if (("GET".equals(var4) || "HEAD".equals(var4)) && !var1) {
            return "the data profile is `ambient` and this scenario read state it did not create ("
               + var4
               + " "
               + var3.e()
               + "), so the two legs cannot be shown to have started equal. Seed through the API to lift this scenario out of `notmeasured`.";
         }

         if (("POST".equals(var4) || "PUT".equals(var4)) && var3.f() >= 200 && var3.f() < 300) {
            var1 = true;
         }
      }

      return null;
   }

   private static String b(String var0) {
      return var0 == null ? "(no error message)" : a(var0.strip().lines().findFirst().orElse(var0.strip()));
   }

   public static enum ConfirmationOutcome {
      CONFIRMED,
      FLAKY,
      NOT_COMPARABLE,
      SKIPPED;
   }

   @FunctionalInterface
   public interface ConfirmationRunner {
      io.karatelabs.ext.pair.PairOutcomeClassifier.ConfirmationRunner a = (var0, var1) -> io.karatelabs.ext.pair.PairOutcomeClassifier.ConfirmationOutcome.SKIPPED;

      io.karatelabs.ext.pair.PairOutcomeClassifier.ConfirmationOutcome confirm(io.karatelabs.ext.pair.PairRunEvidence.ScenarioRunEvidence var1, String var2);
   }

   public record PairClassificationResult(List<io.karatelabs.ext.pair.PairOutcomeClassifier.ScenarioClassification> c) {


      public Map<String, Integer> a() {
         LinkedHashMap<java.lang.String, java.lang.Integer> var1 = new LinkedHashMap();

         for (String var3 : io.karatelabs.ext.pair.PairOutcomeClassifier.OUTCOME_ORDER) {
            var1.put(var3, 0);
         }

         for (io.karatelabs.ext.pair.PairOutcomeClassifier.ScenarioClassification var5 : this.c) {
            var1.merge(var5.h(), 1, Integer::sum);
         }

         return var1;
      }

      public List<io.karatelabs.ext.pair.PairOutcomeClassifier.ScenarioClassification> b() {
         return this.c.stream().filter(io.karatelabs.ext.pair.PairOutcomeClassifier.ScenarioClassification::c).toList();
      }

      public List<io.karatelabs.ext.pair.PairOutcomeClassifier.ScenarioClassification> c() {
         return this.c;
      }
   }

   public record ScenarioClassification(String e, String f, String g, String h, String i, String j, List<Map<String, Object>> k, List<Map<String, Object>> l, io.karatelabs.ext.pair.PairOutcomeClassifier.ConfirmationOutcome m, int n, boolean o) {

      public ScenarioClassification(
         String var1,
         String var2,
         String var3,
         String var4,
         String var5,
         String var6,
         List<Map<String, Object>> var7,
         List<Map<String, Object>> var8,
         io.karatelabs.ext.pair.PairOutcomeClassifier.ConfirmationOutcome var9
      ) {
         this(var1, var2, var3, var4, var5, var6, var7, var8, var9, -1, false);
      }

      public ScenarioClassification(
         String var1,
         String var2,
         String var3,
         String var4,
         String var5,
         String var6,
         List<Map<String, Object>> var7,
         List<Map<String, Object>> var8,
         io.karatelabs.ext.pair.PairOutcomeClassifier.ConfirmationOutcome var9,
         int var10
      ) {
         this(var1, var2, var3, var4, var5, var6, var7, var8, var9, var10, true);
      }


      public boolean a() {
         return this.n > 0;
      }

      public boolean b() {
         return !"notmeasured".equals(this.h);
      }

      public boolean c() {
         return "staleMock".equals(this.h) || "mockBug".equals(this.h) || "notassertedDivergence".equals(this.h);
      }

      public Map<String, Object> d() {
         LinkedHashMap<java.lang.String, java.lang.Object> var1 = new LinkedHashMap();
         var1.put("scenario", this.f);
         var1.put("slug", this.e);
         var1.put("feature", this.g);
         var1.put("outcome", this.h);
         if (this.i != null) {
            var1.put("cause", this.i);
         }

         if (this.j != null) {
            var1.put("detail", this.j);
         }

         if (this.m != null && this.m != io.karatelabs.ext.pair.PairOutcomeClassifier.ConfirmationOutcome.CONFIRMED) {
            var1.put("confirmation", this.m.name().toLowerCase(Locale.ROOT));
         }

         if (!this.k.isEmpty()) {
            var1.put("differences", this.k);
         }

         if (!this.l.isEmpty()) {
            var1.put("ignored", this.l);
         }

         if (this.n != -1) {
            LinkedHashMap<java.lang.String, java.lang.Object> var2 = new LinkedHashMap();
            var2.put("compared", this.n);
            var2.put("verified", this.a() && this.o);
            var2.put("state", !this.o ? "disagreed" : (this.a() ? "agreed" : "notchecked"));
            var1.put("probe", var2);
         }

         return var1;
      }

      public String e() {
         return this.e;
      }

      public String f() {
         return this.f;
      }

      public String g() {
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

      public List<Map<String, Object>> k() {
         return this.k;
      }

      public List<Map<String, Object>> l() {
         return this.l;
      }

      public io.karatelabs.ext.pair.PairOutcomeClassifier.ConfirmationOutcome m() {
         return this.m;
      }

      public int n() {
         return this.n;
      }

      public boolean o() {
         return this.o;
      }
   }
}
