package io.karatelabs.ext.rules;

import io.karatelabs.common.Json;
import io.karatelabs.ext.common.ExtMeta;
import io.karatelabs.ext.common.jsapi.JsApi;
import io.karatelabs.ext.coverage.Action;
import io.karatelabs.ext.coverage.CoverageItem;
import io.karatelabs.ext.rules.twin.Twin;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public final class RulebookCoverageProvider implements io.karatelabs.ext.coverage.RulebookCoverageResult.RulebookCoverageProducer {
   public static final String a = "oracle:";
   private static final AtomicLong b = new AtomicLong();
   private static final AtomicLong c = new AtomicLong();
   private static final String d = ExtMeta.version("karate-max-meta.properties");
   private static final String e = "pairwise/2/50000/512";
   private final Path f;
   private final RulebookRepository g;
   private final RuleEvaluator h;
   private final Map<String, io.karatelabs.ext.rules.RulebookCoverageProvider.RulebookCoverageEntry> i = new ConcurrentHashMap<>();

   public static String a(String var0) {
      return "oracle:" + var0;
   }

   public static long a() {
      return c.get();
   }

   RulebookCoverageProvider(Path var1, RulebookRepository var2, RuleEvaluator var3) {
      this.f = var1;
      this.g = var2;
      this.h = var3;
   }

   public Path b() {
      return this.f;
   }

   public List<String> c() {
      ArrayList var1 = new ArrayList();

      for (io.karatelabs.ext.rules.RulebookRepository.RulebookSummary var3 : this.g.b()) {
         var1.add(var3.a());
      }

      return var1;
   }

   @Override
   public Map<String, io.karatelabs.ext.coverage.RulebookCoverageResult> a(io.karatelabs.ext.coverage.collect.RequirementsCoverageSource var1) {
      io.karatelabs.ext.rules.RulebookCoverageProvider.CoverageRequirementIds var2 = io.karatelabs.ext.rules.RulebookCoverageProvider.CoverageRequirementIds.a(var1);
      LinkedHashMap var3 = new LinkedHashMap();

      List var4;
      try {
         var4 = this.c();
      } catch (RuntimeException var7) {
         return var3;
      }

      for (String var6 : (Iterable<String>) (Iterable<?>) (var4)) {
         var3.put(var6, this.a(var6, var2));
      }

      return var3;
   }

   public io.karatelabs.ext.coverage.RulebookCoverageResult a(String var1, io.karatelabs.ext.coverage.collect.RequirementsCoverageSource var2) {
      return this.a(var1, io.karatelabs.ext.rules.RulebookCoverageProvider.CoverageRequirementIds.a(var2));
   }

   private io.karatelabs.ext.coverage.RulebookCoverageResult a(String var1, io.karatelabs.ext.rules.RulebookCoverageProvider.CoverageRequirementIds var2) {
      io.karatelabs.ext.rules.RulebookRepository.RulebookDocument var3;
      try {
         var3 = this.g.b(var1);
      } catch (RuntimeException var7) {
         return new io.karatelabs.ext.coverage.RulebookCoverageResult(var1, null, Map.of(), JsApi.error(var7));
      }

      String var4 = this.a(var1, var3, var2);
      io.karatelabs.ext.rules.RulebookCoverageProvider.RulebookCoverageEntry var5 = this.i.get(var1);
      if (var5 != null && var5.a().equals(var4)) {
         return var5.b();
      } else {
         io.karatelabs.ext.coverage.RulebookCoverageResult var6 = this.a(var1, var3, var2, var4);
         if (!var6.hasError()) {
            this.i.put(var1, new io.karatelabs.ext.rules.RulebookCoverageProvider.RulebookCoverageEntry(var4, var6));
         }

         return var6;
      }
   }

   private io.karatelabs.ext.coverage.RulebookCoverageResult a(String var1, io.karatelabs.ext.rules.RulebookRepository.RulebookDocument var2, io.karatelabs.ext.rules.RulebookCoverageProvider.CoverageRequirementIds var3, String var4) {
      long var5 = System.currentTimeMillis();
      String var7 = a(var1) + "#" + b.incrementAndGet();
      ArrayList var8 = new ArrayList();
      ArrayList var9 = new ArrayList();
      LinkedHashMap var10 = new LinkedHashMap();
      LinkedHashSet var11 = new LinkedHashSet();

      try {
         int var12 = 0;

         for (Map var14 : var2.b()) {
            String var15 = String.valueOf(var14.get("_id"));

            Object var16;
            try {
               var16 = this.h.a(var2.g(), var2.m().e(), var14, null).h();
            } catch (RuleApiException var20) {
               if (!(var20.getCause() instanceof io.karatelabs.ext.rules.CalcExecutor.CalcExecutionException var18)) {
                  throw var20;
               }

               var16 = var18.c;
            }

            LinkedHashSet var17 = new LinkedHashSet();

            for (String var29 : (Iterable<String>) (Iterable<?>) (var16)) {
               var17.add("req=" + var29);
               var11.add(var29);
            }

            var17.add("cov=rules:" + var1 + "/" + var15);
            var17.add("oracle=rules");
            var8.add(
               new io.karatelabs.ext.coverage.graph.TestNode(
                  "rulebook:" + var1 + "::" + var15, var15, "rulebook:" + var1, 0, var17, io.karatelabs.ext.coverage.graph.TestNode.TestStatus.PASSED
               )
            );
            var12++;
         }

         var10.put("rules", var1);
         var10.put("scenarios", var12);
         var10.put("criteria", new ArrayList(var11));
         Map var23 = a(var1, var2, var8);
         if (var23 != null) {
            var10.put("rejects", var23);
         }

         Map var25 = this.b(var1, var2, var8);
         if (var25 != null) {
            var10.put("sequences", var25);
         }

         if (var2.g() != null && (var2.g().contains("calc.always") || var2.g().contains("calc.sometimes"))) {
            LinkedHashMap var26 = new LinkedHashMap();
            var26.put("guarded", 0);
            var26.put("complete", 0);
            var26.put("holds", 0);
            var26.put("violated", 0);
            var26.put("notchecked", 0);
            var26.put("violations", new ArrayList());
            Map var27 = this.a(var1, var2, var3, var8, var9, var26, var4);
            if (var27 != null) {
               return new io.karatelabs.ext.coverage.RulebookCoverageResult(var1, null, Map.of(), var27);
            }

            var10.put("guarantees", var26);
            if ((Integer)var26.get("violated") > 0) {
               var10.put(
                  "next",
                  "a violated guarantee reads FAILING on the criterion it guards and blocks Requirement.readiness() — reproduce it with the repro call on each violations[] entry, fix the rules OR the invariant (both are reviewable artifacts), re-run Rule.cover, and keep the counterexample as a saved scenario so the defect class stays guarded"
               );
            }
         }
      } catch (RuntimeException var21) {
         return new io.karatelabs.ext.coverage.RulebookCoverageResult(var1, null, Map.of(), JsApi.error(var21));
      }

      LinkedHashMap var22 = new LinkedHashMap();
      var22.put("lane", "oracle");
      var22.put("fingerprint", var4);
      io.karatelabs.ext.coverage.collect.TestExecution var24 = new io.karatelabs.ext.coverage.collect.TestExecution(
         var7,
         a(var1),
         null,
         a(var1),
         var5,
         System.currentTimeMillis(),
         null,
         io.karatelabs.ext.coverage.collect.TestExecution.ExecutionPhase.TERMINAL,
         io.karatelabs.ext.coverage.collect.TestExecution.ExecutionOutcome.PASSED,
         false,
         null,
         a(var1),
         var1,
         "rulebook:" + var1,
         0,
         Set.of("oracle=rules"),
         "oracle",
         var9,
         List.of(),
         List.of(),
         List.of(),
         List.of(),
         var22,
         var8
      );
      return new io.karatelabs.ext.coverage.RulebookCoverageResult(var1, var24, var10, null);
   }

   private static Map<String, Object> a(String var0, io.karatelabs.ext.rules.RulebookRepository.RulebookDocument var1, List<io.karatelabs.ext.coverage.graph.TestNode> var2) {
      LinkedHashMap var3 = null;

      for (Map var5 : var1.c()) {
         if (var3 == null) {
            var3 = new LinkedHashMap();
            var3.put("linked", 0);
            var3.put("unlinked", 0);
         }

         List var6 = RulebookRepository.b(var5);
         if (var6.isEmpty()) {
            var3.merge("unlinked", 1, (var0x, var1x) -> (Integer)var0x + (Integer)var1x);
         } else {
            var3.merge("linked", 1, (var0x, var1x) -> (Integer)var0x + (Integer)var1x);
            String var7 = String.valueOf(var5.get("_id"));
            String var9 = String.valueOf(Schema.rejectVerdict(var1.e(), var5).get("status"));

            io.karatelabs.ext.coverage.graph.TestNode.TestStatus var8 = switch (var9) {
               case "REFUSED" -> io.karatelabs.ext.coverage.graph.TestNode.TestStatus.PASSED;
               case "ACCEPTED" -> io.karatelabs.ext.coverage.graph.TestNode.TestStatus.FAILED;
               default -> io.karatelabs.ext.coverage.graph.TestNode.TestStatus.SKIPPED;
            };
            LinkedHashSet var12 = new LinkedHashSet();

            for (String var11 : (Iterable<String>) (Iterable<?>) (var6)) {
               var12.add("req=" + var11);
            }

            var12.add("cov=rules:" + var0 + "/" + var7);
            var12.add("oracle=rules");
            var12.add("evidence=refusal");
            var2.add(new io.karatelabs.ext.coverage.graph.TestNode("rulebook:" + var0 + "::reject:" + var7, var7, "rulebook:" + var0, 0, var12, var8));
         }
      }

      return var3;
   }

   private Map<String, Object> b(String var1, io.karatelabs.ext.rules.RulebookRepository.RulebookDocument var2, List<io.karatelabs.ext.coverage.graph.TestNode> var3) {
      if (var2.l() != null && var2.l().get("sequences") instanceof List var4 && !var4.isEmpty()) {
         LinkedHashMap var20 = new LinkedHashMap();
         var20.put("linked", 0);
         var20.put("unlinked", 0);
         var20.put("unconfirmed", 0);
         ArrayList var6 = new ArrayList();

         for (Object var8 : var4) {
            Map var9 = (Map)var8;
            if (RulebookRepository.b(var9).isEmpty()) {
               var20.merge("unlinked", 1, (var0, var1x) -> (Integer)var0 + (Integer)var1x);
            } else if (!RulebookRepository.c(var9)) {
               var20.merge("unconfirmed", 1, (var0, var1x) -> (Integer)var0 + (Integer)var1x);
            } else {
               var20.merge("linked", 1, (var0, var1x) -> (Integer)var0 + (Integer)var1x);
               var6.add(var9);
            }
         }

         if (var6.isEmpty()) {
            return var20;
         } else {
            io.karatelabs.ext.rules.twin.TwinModel var21 = io.karatelabs.ext.rules.twin.TwinModelLoader.a(var2.k());

            try (TwinRuleRuntime var22 = new TwinRuleRuntime(this.g, var1)) {
               for (Map var10 : (Iterable<Map>) (Iterable<?>) (var6)) {
                  String var11 = String.valueOf(var10.get("_id"));
                  List var12 = Twin.expectMismatches(var21, var10, var22);
                  io.karatelabs.ext.coverage.graph.TestNode.TestStatus var13 = var12 == null
                     ? io.karatelabs.ext.coverage.graph.TestNode.TestStatus.SKIPPED
                     : (var12.isEmpty() ? io.karatelabs.ext.coverage.graph.TestNode.TestStatus.PASSED : io.karatelabs.ext.coverage.graph.TestNode.TestStatus.FAILED);
                  LinkedHashSet var14 = new LinkedHashSet();

                  for (String var16 : (Iterable<String>) (Iterable<?>) (RulebookRepository.b(var10))) {
                     var14.add("req=" + var16);
                  }

                  var14.add("cov=rules:" + var1 + "/" + var11);
                  var14.add("oracle=rules");
                  if (Boolean.TRUE.equals(var10.get("_ai"))) {
                     var14.add("ai");
                  }

                  String var24 = var10.get("_label") instanceof String var25 ? var25 : var11;
                  var3.add(new io.karatelabs.ext.coverage.graph.TestNode("rulebook:" + var1 + "::sequence:" + var11, var24, "rulebook:" + var1, 0, var14, var13));
               }
            }

            return var20;
         }
      } else {
         return null;
      }
   }

   private Map<String, Object> a(
      String var1, io.karatelabs.ext.rules.RulebookRepository.RulebookDocument var2, io.karatelabs.ext.rules.RulebookCoverageProvider.CoverageRequirementIds var3, List<io.karatelabs.ext.coverage.graph.TestNode> var4, List<Action> var5, Map<String, Object> var6, String var7
   ) {
      c.incrementAndGet();
      var var8 = RuleAnalysisEngine.a(var2, null, null, RuleInputSpaceCertificateBuilder.a(this.g.c(var1), var2.n().c(), var2.i().get("schema")));
      if (var8.get("error") != null) {
         return var8;
      } else if (var8.get("propertyErrors") instanceof List var9 && !var9.isEmpty()) {
         LinkedHashMap var25 = new LinkedHashMap();
         var25.put("error", "rulebook '" + var1 + "' has property authoring defects — fix calc.js: " + var9);
         var25.put("code", "PROPERTY_INVALID");
         var25.put("status", 400);
         var25.put("rule", var1);
         var25.put("propertyErrors", var9);
         return var25;
      } else {
         List var23 = (List)var6.get("violations");

         for (Map var11 : (Iterable<Map>) (Iterable<?>) (var8.getOrDefault("properties", List.of()))) {
            String var12 = var11.get("req") instanceof String var13 ? var13 : null;
            if (var12 != null) {
               if (!var3.a().contains(var12)) {
                  LinkedHashMap var27 = new LinkedHashMap();
                  boolean var29 = var3.b().contains(var12);
                  var27.put("error", "property '" + var11.get("name") + "' in rulebook '" + var1 + "' guards '" + a(var12, var29, var3.a()));
                  var27.put("code", var29 ? "REQ_NOTCRITERION" : "REQ_NOTFOUND");
                  var27.put("status", 400);
                  var27.put("rule", var1);
                  var27.put("property", var11.get("name"));
                  var27.put("req", var12);
                  return var27;
               }

               String var26 = String.valueOf(var11.get("name"));
               String var28 = String.valueOf(var11.get("kind"));
               boolean var15 = "sometimes".equals(var28);
               String var16 = String.valueOf(var11.get("status"));
               io.karatelabs.ext.coverage.graph.TestNode.TestStatus var37;
               if (var15) {
                  switch (var16) {
                     case "REACHED":
                        var37 = io.karatelabs.ext.coverage.graph.TestNode.TestStatus.PASSED;
                        break;
                     case "NOTREACHABLE":
                        var37 = io.karatelabs.ext.coverage.graph.TestNode.TestStatus.FAILED;
                        break;
                     default:
                        var37 = io.karatelabs.ext.coverage.graph.TestNode.TestStatus.SKIPPED;
                  }
               } else {
                  switch (var16) {
                     case "VIOLATED":
                        var37 = io.karatelabs.ext.coverage.graph.TestNode.TestStatus.FAILED;
                        break;
                     case "COMPLETE":
                     case "HOLDS":
                        var37 = io.karatelabs.ext.coverage.graph.TestNode.TestStatus.PASSED;
                        break;
                     default:
                        var37 = io.karatelabs.ext.coverage.graph.TestNode.TestStatus.SKIPPED;
                  }
               }

               io.karatelabs.ext.coverage.graph.TestNode.TestStatus var17 = var37;
               String var18 = "rulebook:" + var1 + "::property:" + var26;
               LinkedHashSet var31 = new LinkedHashSet();
               var31.add("oracle=rules");
               if (var17 == io.karatelabs.ext.coverage.graph.TestNode.TestStatus.SKIPPED) {
                  var31.add("req=" + var12);
               }

               var4.add(new io.karatelabs.ext.coverage.graph.TestNode(var18, var26, "rulebook:" + var1, 0, var31, var17));
               if (var17 != io.karatelabs.ext.coverage.graph.TestNode.TestStatus.SKIPPED) {
                  LinkedHashMap var20 = new LinkedHashMap();
                  var20.put("oracle", true);
                  var20.put("rule", var1);
                  var20.put("property", var26);
                  var20.put("kind", var28);
                  if (!var15 || var17 == io.karatelabs.ext.coverage.graph.TestNode.TestStatus.FAILED) {
                     var20.put("propertyStatus", var16);
                     if (Boolean.TRUE.equals(var11.get("domainRestricted"))) {
                        var20.put("domainRestricted", true);
                        var20.put("claim", var11.get("claim"));
                     }
                  }

                  var20.put("status", var17 == io.karatelabs.ext.coverage.graph.TestNode.TestStatus.PASSED ? "pass" : "fail");
                  if (var11.get("counterexample") instanceof Map var21) {
                     var20.put("counterexample", var21);
                  }

                  if (var15 && var11.get("witness") instanceof Map var33) {
                     var20.put("witness", var33);
                  }

                  var20.put("fingerprint", var7);
                  var5.add(new io.karatelabs.ext.coverage.EmitAction(var18 + "#emit", var31, Instant.EPOCH, "rulebook:" + var1, var26, 0, var18, "req", var12, var20));
               }

               if (!var15) {
                  var6.merge("guarded", 1, (var0, var1x) -> (Integer)var0 + (Integer)var1x);

                  String var32 = switch (var16) {
                     case "VIOLATED" -> "violated";
                     case "COMPLETE" -> "complete";
                     case "HOLDS" -> "holds";
                     default -> "notchecked";
                  };
                  var6.merge(var32, 1, (var0, var1x) -> (Integer)var0 + (Integer)var1x);
                  if (Boolean.TRUE.equals(var11.get("domainRestricted"))) {
                     var6.put("domainRestricted", true);
                  }

                  if ("VIOLATED".equals(var16)) {
                     LinkedHashMap var34 = new LinkedHashMap();
                     var34.put("rule", var1);
                     var34.put("property", var26);
                     var34.put("req", var12);
                     var34.put("counterexample", var11.get("counterexample"));
                     var34.put("repro", "Rule.execute('" + var1 + "', " + Json.stringifyStrict(var11.get("counterexample")) + ")");
                     var23.add(var34);
                  }
               }
            }
         }

         return null;
      }
   }

   static String a(String var0, boolean var1, Set<String> var2) {
      return var1
         ? var0
            + "' is a requirement, not an acceptance criterion — use one of its criteria: "
            + var2.stream().filter(var1x -> var1x.startsWith(var0 + "/")).toList()
         : var0 + "' — fix the {req:'<reqId>/<acId>'} id (list the real ids with Requirement.list())";
   }

   private String a(String var1, io.karatelabs.ext.rules.RulebookRepository.RulebookDocument var2, io.karatelabs.ext.rules.RulebookCoverageProvider.CoverageRequirementIds var3) {
      StringBuilder var4 = new StringBuilder(new TreeMap<>(var2.i()).toString());
      Path var5 = RuleInputSpaceCertificateBuilder.a(this.g.c(var1));

      try {
         var4.append("|certificate=").append(Files.isRegularFile(var5) ? RulebookRepository.b(var5) : "none");
      } catch (RuntimeException var12) {
         var4.append("|certificate=unreadable@").append(System.nanoTime());
      }

      for (String var7 : new TreeSet<>(this.c())) {
         String var8;
         try {
            var8 = this.g.d(var7).d().c();
         } catch (RuntimeException var11) {
            var8 = "none";
         }

         var4.append("|calc:").append(var7).append('=').append(var8);
      }

      var4.append("|settings=")
         .append("pairwise/2/50000/512")
         .append("|criteria=")
         .append(new TreeSet<>(var3.a()))
         .append("|requirements=")
         .append(new TreeSet<>(var3.b()))
         .append("|engine=")
         .append(d);

      try {
         MessageDigest var13 = MessageDigest.getInstance("SHA-256");
         return HexFormat.of().formatHex(var13.digest(var4.toString().getBytes(StandardCharsets.UTF_8)));
      } catch (NoSuchAlgorithmException var10) {
         throw new IllegalStateException(var10);
      }
   }

   private record RulebookCoverageEntry(String a, io.karatelabs.ext.coverage.RulebookCoverageResult b) {

      private RulebookCoverageEntry(String a, io.karatelabs.ext.coverage.RulebookCoverageResult b) {
         this.a = a;
         this.b = b;
      }
   }

   record CoverageRequirementIds(Set<String> a, Set<String> b) {

      CoverageRequirementIds(Set<String> a, Set<String> b) {
         this.a = a;
         this.b = b;
      }

      static io.karatelabs.ext.rules.RulebookCoverageProvider.CoverageRequirementIds a(io.karatelabs.ext.coverage.collect.RequirementsCoverageSource var0) {
         LinkedHashSet var1 = new LinkedHashSet();
         LinkedHashSet var2 = new LinkedHashSet();

         for (CoverageItem var4 : var0.a()) {
            if ("acceptance-criterion".equals(var4.getMetadata("kind"))) {
               var1.add(var4.id());
            } else {
               var2.add(var4.id());
            }
         }

         return new io.karatelabs.ext.rules.RulebookCoverageProvider.CoverageRequirementIds(var1, var2);
      }
   }
}
