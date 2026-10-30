package io.karatelabs.ext.pair;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class PairReportAssembler {
   public static final List<String> PROVENANCE_FIELDS = List.of("dataProfile", "mockIdentity", "probeVerified");

   private PairReportAssembler() {
   }

   private static Map<String, Object> a(String var0, boolean var1, String var2) {
      LinkedHashMap<java.lang.String, java.lang.Object> var3 = new LinkedHashMap();
      var3.put("bar", var0);
      var3.put("held", var1);
      var3.put("counted", var2);
      return var3;
   }

   private static Map<String, Object> a(String var0, String var1, Map<String, Object> var2) {
      var var3 = a(var0, true, var1);
      var3.put("waived", var2);
      var3.put("note", "This bar did not hold. A waiver stood it down, so the rung above it rests on that exception and not on evidence.");
      return var3;
   }

   public static io.karatelabs.ext.pair.PairReportAssembler.ScenarioAssessment assessScenario(String var0, List<io.karatelabs.ext.pair.PairOutcomeClassifier.ScenarioClassification> var1, io.karatelabs.ext.pair.PairReportAssembler.PairAssessmentContext var2) {
      ArrayList<java.util.Map<java.lang.String,java.lang.Object>> var3 = new ArrayList();
      var3.add(a("declared", true, var2.i() ? "declared by the contract" : "declared by the coverage graph — this pair binds no contract document"));
      var var4 = var1.stream().filter(io.karatelabs.ext.pair.PairOutcomeClassifier.ScenarioClassification::b).toList();
      if (var4.isEmpty()) {
         var3.add(
            a("admissible", false, "0 of " + var1.size() + " scenario(s) that exercised it produced a verdict about behaviour — every one was `notmeasured`")
         );
         return new io.karatelabs.ext.pair.PairReportAssembler.ScenarioAssessment(
            var0,
            io.karatelabs.ext.pair.PairReportAssembler.MaturityRung.SHAPE,
            var3,
            "no scenario exercising this operation produced a verdict about behaviour, so nothing above `shape-conformant` was measured. Read the per-scenario `cause` — an environment refusal, a failed precondition probe and an excluded destructive operation are three different problems."
         );
      } else {
         var3.add(a("admissible", true, var4.size() + " of " + var1.size() + " scenario(s) that exercised it produced a verdict about behaviour"));
         long var5 = var4.stream().filter(PairReportAssembler::a).count();
         var3.add(a("mockGreen", var5 == var4.size(), var5 + " of " + var4.size() + " admissible scenario(s) were green on the MOCK leg"));
         if (var5 != var4.size()) {
            return new io.karatelabs.ext.pair.PairReportAssembler.ScenarioAssessment(var0, io.karatelabs.ext.pair.PairReportAssembler.MaturityRung.SHAPE, var3, "the mock leg was red for this operation, so it is not even verified against the mock" + a(var4, var2));
         } else {
            boolean var7 = var2.f() > 0 && var2.e() == var2.f();
            var3.add(
               a(
                  "adherence",
                  var7,
                  var2.f() == 0
                     ? "not checked: the provider leg recorded no HTTP exchange, so nothing shows it addressed the target it was given"
                     : var2.e() + " of " + var2.f() + " provider-leg scenario(s) that made a call addressed the target they were handed"
               )
            );
            if (!var7) {
               return new io.karatelabs.ext.pair.PairReportAssembler.ScenarioAssessment(
                  var0,
                  io.karatelabs.ext.pair.PairReportAssembler.MaturityRung.MOCK,
                  var3,
                  "the provider leg did not demonstrably answer to the target it was handed, so nothing it reported is a verdict about that provider. This bar cannot be waived — wiring is prior to behaviour."
               );
            } else {
               long var8 = var4.stream().filter(PairReportAssembler::b).count();
               var3.add(a("providerGreen", var8 == var4.size(), var8 + " of " + var4.size() + " admissible scenario(s) were green on the PROVIDER leg"));
               if (var8 != var4.size()) {
                  return new io.karatelabs.ext.pair.PairReportAssembler.ScenarioAssessment(var0, io.karatelabs.ext.pair.PairReportAssembler.MaturityRung.MOCK, var3, "the provider leg was red for this operation, so it is verified against the mock only" + a(var4, var2));
               } else if (var2.b()) {
                  var3.add(a("rehearsal", false, "the provider leg disclosed itself as a karate mock on the wire (the Karate-Mock header)"));
                  return new io.karatelabs.ext.pair.PairReportAssembler.ScenarioAssessment(
                     var0,
                     io.karatelabs.ext.pair.PairReportAssembler.MaturityRung.PROVIDER,
                     var3,
                     "the `provider` was itself a karate mock, so this pair is a REHEARSAL. Two instances of one program agree by construction, which is why this bar cannot be waived at any price. Point `provider` at the real deployment."
                  );
               } else {
                  var3.add(
                     a(
                        "rehearsal",
                        true,
                        "the provider leg carried no Karate-Mock header, so it was not one of our own mocks (which is all the wire can establish)"
                     )
                  );
                  long var10 = var4.stream().filter(var0x -> "agreed".equals(var0x.h())).count();
                  var3.add(
                     a(
                        "divergenceFree",
                        var10 == var4.size(),
                        var10 + " of " + var4.size() + " admissible scenario(s) were identical at both the verdict layer and the response layer"
                     )
                  );
                  if (var10 != var4.size()) {
                     return new io.karatelabs.ext.pair.PairReportAssembler.ScenarioAssessment(
                        var0,
                        io.karatelabs.ext.pair.PairReportAssembler.MaturityRung.PROVIDER,
                        var3,
                        "both legs passed, but their responses differ. The suite does not assert the field that differs. Tighten the assertion and re-mint; until then this operation is verified against the provider but not proven substitutable for it."
                     );
                  } else {
                     io.karatelabs.ext.pair.PairReportAssembler.ScenarioAssessment var12 = a(
                        var0,
                        var3,
                        "dataProfile",
                        var2.a() == io.karatelabs.ext.pair.DataProfile.SEEDED,
                        "the data profile is `" + var2.a().wire() + "`" + (var2.a() == io.karatelabs.ext.pair.DataProfile.SEEDED ? "" : ", not `seeded`"),
                        var2,
                        "only the `seeded` profile reaches the top rung: under `fixture` the two legs were pre-loaded rather than building their own state, and under `ambient` the provider holds state we did not create and equivalence cannot be established at all (D223d)."
                     );
                     if (var12 != null) {
                        return var12;
                     } else {
                        long var13 = var4.stream().filter(io.karatelabs.ext.pair.PairOutcomeClassifier.ScenarioClassification::a).count();
                        var12 = a(
                           var0,
                           var3,
                           "probeVerified",
                           var13 == var4.size(),
                           var13
                              + " of "
                              + var4.size()
                              + " admissible scenario(s) had their starting state observed to be equivalent, by a Background read-back",
                           var2,
                           "a scenario whose two Backgrounds made no comparable call had its state equivalence assumed rather than verified. Nothing was found wrong because nothing was checked. Put the suite's setup read-backs in the Background: that is the precondition probe."
                        );
                        if (var12 != null) {
                           return var12;
                        } else {
                           var12 = a(
                              var0,
                              var3,
                              "mockIdentity",
                              var2.c(),
                              "the mock leg was served by " + var2.d(),
                              var2,
                              "the top rung is a claim about the mock your project SHIPS. A mock synthesized from the same document the assertions were written against agrees with them by construction, and a mock already standing at a URL is a program this harness cannot describe — neither answers the buyer's question."
                           );
                           return var12 != null ? var12 : new io.karatelabs.ext.pair.PairReportAssembler.ScenarioAssessment(var0, io.karatelabs.ext.pair.PairReportAssembler.MaturityRung.SUBSTITUTABLE, var3, null);
                        }
                     }
                  }
               }
            }
         }
      }
   }

   private static io.karatelabs.ext.pair.PairReportAssembler.ScenarioAssessment a(String var0, List<Map<String, Object>> var1, String var2, boolean var3, String var4, io.karatelabs.ext.pair.PairReportAssembler.PairAssessmentContext var5, String var6) {
      if (var3) {
         var1.add(a(var2, true, var4));
         return null;
      } else {
         Map var7 = var5.h() == null ? null : var5.h().get(var2);
         if (var7 != null) {
            var1.add(a(var2, var4, var7));
            return null;
         } else {
            var1.add(a(var2, false, var4));
            return new io.karatelabs.ext.pair.PairReportAssembler.ScenarioAssessment(
               var0,
               io.karatelabs.ext.pair.PairReportAssembler.MaturityRung.PROVIDER,
               var1,
               var6
                  + " Stand the bar down deliberately if your situation warrants it — `bars: {"
                  + var2
                  + ": {reason, owner}}` — and the rung will say it rests on a waiver rather than on evidence."
            );
         }
      }
   }

   private static String a(List<io.karatelabs.ext.pair.PairOutcomeClassifier.ScenarioClassification> var0, io.karatelabs.ext.pair.PairReportAssembler.PairAssessmentContext var1) {
      if (var1.g()) {
         return ". The failing leg was re-run to confirm it, so this is divergence rather than flakiness.";
      } else {
         boolean var2 = var0.stream().anyMatch(var0x -> "staleMock".equals(var0x.h()) || "mockBug".equals(var0x.h()));
         return var2 ? ". Confirmation was OFF, so this may be flakiness rather than divergence — re-mint with {confirm:true} before acting on it." : "";
      }
   }

   private static boolean a(io.karatelabs.ext.pair.PairOutcomeClassifier.ScenarioClassification var0) {
      return "agreed".equals(var0.h()) || "staleMock".equals(var0.h()) || "notassertedDivergence".equals(var0.h());
   }

   private static boolean b(io.karatelabs.ext.pair.PairOutcomeClassifier.ScenarioClassification var0) {
      return "agreed".equals(var0.h()) || "mockBug".equals(var0.h()) || "notassertedDivergence".equals(var0.h());
   }

   private static List<Map<String, Object>> a(List<io.karatelabs.ext.pair.PairReportAssembler.ScenarioAssessment> var0) {
      ArrayList<java.util.Map<java.lang.String,java.lang.Object>> var1 = new ArrayList();

      for (io.karatelabs.ext.pair.PairReportAssembler.ScenarioAssessment var3 : var0) {
         for (Map var5 : var3.d()) {
            if (var5.get("waived") instanceof Map var6) {
               LinkedHashMap<java.lang.String, java.lang.Object> var8 = new LinkedHashMap();
               var8.put("bar", var5.get("bar"));
               var8.put("item", var3.b());
               var8.putAll(var6);
               var1.add(var8);
            }
         }
      }

      return var1;
   }

   public static Map<String, Object> summarize(List<io.karatelabs.ext.pair.PairReportAssembler.ScenarioAssessment> var0, int var1, List<String> var2) {
      return summarize(var0, var1, var2, true);
   }

   public static Map<String, Object> summarize(List<io.karatelabs.ext.pair.PairReportAssembler.ScenarioAssessment> var0, int var1, List<String> var2, boolean var3) {
      LinkedHashMap<java.lang.String, java.lang.Object> var4 = new LinkedHashMap();
      var var5 = var0.stream().filter(var0x -> var0x.c() != null).toList();
      if (var5.isEmpty()) {
         var4.put("rung", null);
         var4.put("name", "not measured");
         var4.put(
            "note",
            "This pair exercised nothing, so it holds no rung. An operation that nothing exercised has the status `not measured`, never `substitutable`."
         );
         return var4;
      } else {
         io.karatelabs.ext.pair.PairReportAssembler.ScenarioAssessment var6 = var5.stream().min((var0x, var1x) -> var0x.c().rung() - var1x.c().rung()).orElseThrow();
         io.karatelabs.ext.pair.PairReportAssembler.MaturityRung var7 = var6.c();
         boolean var8 = !var2.isEmpty() && var7 == io.karatelabs.ext.pair.PairReportAssembler.MaturityRung.SUBSTITUTABLE;
         io.karatelabs.ext.pair.PairReportAssembler.MaturityRung var9 = var8 ? io.karatelabs.ext.pair.PairReportAssembler.MaturityRung.PROVIDER : var7;
         var4.put("rung", var9.rung());
         var4.put("name", var9.label());
         var4.put("means", var9.means());
         var4.put("floorOf", var5.size());
         var var10 = a(var5);
         if (!var10.isEmpty()) {
            var4.put("waivedBars", var10);
            var4.put(
               "restsOnWaiver",
               "this rung rests on "
                  + var10.size()
                  + " stood-down bar(s) — "
                  + var10.stream().map(var0x -> String.valueOf(var0x.get("bar"))).distinct().reduce((var0x, var1x) -> var0x + ", " + var1x).orElse("")
                  + ". A waiver is a signed exception, not evidence. The bar did not hold, and the rung above it stands because somebody accepted that. `git blame` the pair to see who."
            );
         }

         if (var8) {
            var4.put(
               "cappedBy",
               "This pair did not exercise "
                  + var2.size()
                  + " of "
                  + var1
                  + " declared operations, so the artifact cannot claim the top rung. That limit applies even if every operation it did exercise was divergence-free. The rung for each operation below continues to apply."
            );
         } else if (var9 != io.karatelabs.ext.pair.PairReportAssembler.MaturityRung.SUBSTITUTABLE) {
            var4.put("floorSetBy", var6.b());
            var4.put("blockedBy", var6.e());
         }

         io.karatelabs.ext.pair.PairReportAssembler.MaturityRung var11 = var9.rung() < io.karatelabs.ext.pair.PairReportAssembler.MaturityRung.SUBSTITUTABLE.rung() ? io.karatelabs.ext.pair.PairReportAssembler.MaturityRung.values()[var9.ordinal() + 1] : null;
         if (var11 != null) {
            var4.put("next", Map.of("rung", var11.rung(), "name", var11.label(), "needs", var11.means()));
         }

         if (var9 == io.karatelabs.ext.pair.PairReportAssembler.MaturityRung.SUBSTITUTABLE) {
            var4.put(
               "scope",
               var3
                  ? "this rung is a claim about CONTRACT BEHAVIOUR: status, body, and the headers the contract itself declares. It is silent by construction on the transport-adjacent differences a stand-in can still have — header canonicalisation, charset, chunking, compression, connection reuse — because comparing every header would make infrastructure read as infidelity. And it is RELEASE-TIME, not a live badge: it is derived from a dated artifact that decays when the spec, the suite or the mock moves."
                  : "this pair binds no contract document (D233): the claim covers status and body only — NO response headers were compared, because the spec is what declares them. It is RELEASE-TIME, not a live badge: it is derived from a dated artifact that decays when the suite or the mock moves."
            );
         }

         return var4;
      }
   }

   public static Map<String, Object> applyFreshness(Map<String, Object> var0, Map<String, Object> var1) {
      Map<java.lang.String, Object> var2 = var0.get("claim") instanceof Map var3 ? var3 : Map.of();
      Map<java.lang.String, Object> var7 = var2.get("rungs") instanceof Map var8 ? var8 : null;
      if (var7 == null) {
         return null;
      } else {
         Map<java.lang.String, Object> var9 = var7.get("aggregate") instanceof Map var10 ? var10 : Map.of();
         String var11 = String.valueOf(var1 == null ? "fresh" : var1.get("status"));
         LinkedHashMap<java.lang.String, java.lang.Object> var12 = new LinkedHashMap(var9);
         if ("fresh".equals(var11)) {
            return var12;
         } else {
            var12.put("observed", var9.get("rung"));
            var12.put("observedName", var9.get("name"));
            var12.put("rung", null);
            var12.put("name", "not measured");
            var12.put("withdrawnBy", var11);
            var12.keySet().removeAll(List.of("means", "blockedBy", "cappedBy", "scope", "floorOf", "waivedBars"));
            var12.put(
               "note",
               "The rung this pair earned is withdrawn, not lowered. The evidence is "
                  + var11
                  + (var1 == null ? "" : " — " + var1.get("note"))
                  + " A rung is a claim about a subject, and this evidence is no longer about the subject this project has. Re-mint it: `refresh.call` on this payload carries the same scope."
            );
            return var12;
         }
      }
   }

   public record PairAssessmentContext(io.karatelabs.ext.pair.DataProfile a, boolean b, boolean c, String d, int e, int f, boolean g, Map<String, Map<String, Object>> h, boolean i) {

      public PairAssessmentContext(io.karatelabs.ext.pair.DataProfile var1, boolean var2, boolean var3, String var4, int var5, int var6, boolean var7, Map<String, Map<String, Object>> var8) {
         this(var1, var2, var3, var4, var5, var6, var7, var8, true);
      }

   }

   public record ScenarioAssessment(String b, io.karatelabs.ext.pair.PairReportAssembler.MaturityRung c, List<Map<String, Object>> d, String e) {


      public Map<String, Object> a() {
         LinkedHashMap<java.lang.String, java.lang.Object> var1 = new LinkedHashMap();
         var1.put("item", this.b);
         var1.put("rung", this.c == null ? null : this.c.rung());
         var1.put("name", this.c == null ? "not measured" : this.c.label());
         var1.put("bars", this.d);
         if (this.e != null) {
            var1.put("blockedBy", this.e);
         }

         return var1;
      }

      public String b() {
         return this.b;
      }

      public io.karatelabs.ext.pair.PairReportAssembler.MaturityRung c() {
         return this.c;
      }

      public List<Map<String, Object>> d() {
         return this.d;
      }

      public String e() {
         return this.e;
      }
   }

   public static enum MaturityRung {
      SHAPE(1, "shape-conformant", "The contract declares this operation. This rung reports nothing about its behaviour."),
      MOCK(2, "verified-against-mock", "A suite exercised this operation against the mock, and the mock leg passed. The provider leg did not verify it."),
      PROVIDER(
         3, "verified-against-provider", "The provider leg exercised this operation and sent its requests to the target it was given, and those tests passed."
      ),
      SUBSTITUTABLE(
         4,
         "proven-substitutable",
         "The run compared the two legs, and they did not differ. They agreed at the verdict layer and at the response layer, under conditions strict enough for the claim."
      );

      private final int rung;
      private final String label;
      private final String means;

      private MaturityRung(int var3, String var4, String var5) {
         this.rung = var3;
         this.label = var4;
         this.means = var5;
      }

      public int rung() {
         return this.rung;
      }

      public String label() {
         return this.label;
      }

      public String means() {
         return this.means;
      }
   }
}
