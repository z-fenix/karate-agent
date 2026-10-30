package io.karatelabs.ext.coverage.collect;

import io.karatelabs.common.Json;
import io.karatelabs.ext.common.MaxPaths;
import io.karatelabs.ext.coverage.Action;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class RunEvidenceBundle {
   private static final Logger e = LoggerFactory.getLogger(RunEvidenceBundle.class);
   private static final Pattern f = Pattern.compile("@req=([A-Za-z0-9._\\-:/{},]+)");
   private static final Pattern g = Pattern.compile("@cov=([A-Za-z0-9._\\-:/{},]+)");
   static final String a = "run:";
   public static final String b = "execution-start";
   public static final String c = "execution-end";
   private final List<io.karatelabs.ext.coverage.graph.TestNode> h = new ArrayList<>();
   private final List<io.karatelabs.ext.coverage.graph.CoverageHit> i = new ArrayList<>();
   private final List<io.karatelabs.ext.coverage.graph.CoverageHit> j = new ArrayList<>();
   private final List<TestExecution> k = new ArrayList<>();
   private final List<Map<String, Object>> l = new ArrayList<>();
   private final Map<String, Object> m = new LinkedHashMap<>();
   private boolean n;
   private boolean o;
   private boolean p;
   public static final String d = "ambiguousAttribution";

   private RunEvidenceBundle() {
      this.m.put("unmanifested", new ArrayList());
      this.m.put("finalizationInferred", new ArrayList());
      this.m.put("incompleteEvidence", new ArrayList());
      this.m.put("unfinalized", new ArrayList());
      this.m.put("runningElsewhere", new ArrayList());
      this.m.put("duplicateExecutionIds", new ArrayList());
      this.m.put("ambiguousAttribution", new ArrayList());
   }

   public static RunEvidenceBundle a(Path var0) {
      return a(var0, false);
   }

   public static RunEvidenceBundle b(Path var0) {
      return a(var0, true);
   }

   private static RunEvidenceBundle a(Path var0, boolean var1) {
      RunEvidenceBundle var2 = new RunEvidenceBundle();
      var2.n = var1;
      if (!Files.isDirectory(var0)) {
         return var2;
      } else {
         try (var var3 = Files.list(var0)) {
            var3.filter(var0x -> Files.isDirectory(var0x)).sorted().forEach(var1x -> var2.b(var1x, null));
         } catch (Exception var8) {
            e.warn("run evidence: could not list {}: {}", var0, var8.getMessage());
         }

         return var2;
      }
   }

   public static RunEvidenceBundle c(Path var0) {
      return a(var0, null);
   }

   public static RunEvidenceBundle a(Path var0, List<ExecutionFragment> var1) {
      return a(var0, var1, false);
   }

   public static RunEvidenceBundle a(Path var0, List<ExecutionFragment> var1, boolean var2) {
      RunEvidenceBundle var3 = new RunEvidenceBundle();
      var3.o = var2;
      if (Files.isDirectory(var0)) {
         var3.b(var0, var1);
      }

      return var3;
   }

   public static RunEvidenceBundle d(Path var0) {
      RunEvidenceBundle var1 = new RunEvidenceBundle();
      var1.n = true;
      if (Files.isDirectory(var0)) {
         var1.b(var0, null);
      }

      return var1;
   }

   boolean a() {
      return this.a("unfinalized").isEmpty();
   }

   boolean b() {
      return this.a() && !this.p;
   }

   public List<io.karatelabs.ext.coverage.graph.TestNode> c() {
      return this.h;
   }

   public List<io.karatelabs.ext.coverage.graph.CoverageHit> d() {
      return this.i;
   }

   public List<TestExecution> e() {
      return this.k;
   }

   public Map<String, Object> f() {
      return this.m;
   }

   public List<Map<String, Object>> g() {
      return this.l;
   }

   public void a(io.karatelabs.ext.coverage.graph.TraceGraph var1) {
      for (TestExecution var3 : this.k) {
         var1.a(var3, null);
      }

      this.j.forEach(var1::a);
      var1.a(this.m);
   }

   private void b(Path var1, List<ExecutionFragment> var2) {
      String var3 = var1.getFileName().toString();
      if (!this.n) {
         for (RunArtifactParser var5 : (Iterable<RunArtifactParser>) (Iterable<?>) (io.karatelabs.ext.coverage.collect.RunArtifactParser.d())) {
            TestExecution var6 = var5.a(var1);
            if (var6 != null) {
               this.k.add(var6);
               this.p = this.p | var5.c();
               return;
            }
         }
      }

      EvidenceRunMetadata var36 = io.karatelabs.ext.coverage.collect.EvidenceRunMetadata.b(var1);
      Map<java.lang.String, Object> var37 = var36 == null ? null : var36.b();
      Path var38 = var1.resolve("transcript.jsonl");
      String var7 = var37 == null ? null : a(var37.get("req"));
      if (Files.isRegularFile(var38) || var7 != null) {
         boolean var8 = var36 != null && var36.c();
         boolean var9 = io.karatelabs.ext.coverage.collect.CoveragePathFilterRegistry.a(var3);
         if (!var9 && var8 && var36.c(var1)) {
            var9 = true;
            this.a("runningElsewhere").add(var3);
         }

         boolean var10 = var8 && var9 && !var36.d() && !this.o;
         String var11 = "run:" + var3;
         var var12 = c(var7);
         String var13 = var36 == null ? null : var36.d(var1);
         Long var14 = var37 != null && var37.get("startedMs") instanceof Number var15 ? var15.longValue() : null;
         io.karatelabs.ext.coverage.collect.RunEvidenceBundle.ExecutionAccumulator var39 = new io.karatelabs.ext.coverage.collect.RunEvidenceBundle.ExecutionAccumulator(io.karatelabs.ext.coverage.collect.EvidenceRunMetadata.a(var3, 0), null, null, var14);
         LinkedHashMap var40 = new LinkedHashMap();
         boolean[] var17 = new boolean[1];
         io.karatelabs.ext.coverage.collect.RunEvidenceBundle.ExecutionAccumulator[] var18 = new io.karatelabs.ext.coverage.collect.RunEvidenceBundle.ExecutionAccumulator[1];
         if (Files.isRegularFile(var38)) {
            try (var var19 = Files.lines(var38)) {
               var19.forEach(var8x -> this.a(var8x, var3, var11, var12, var39, var40, var18, var17));
            } catch (Exception var35) {
               e.warn("run evidence: could not read {}: {}", var38, var35.getMessage());
            }
         }

         Map<String, List<ExecutionFragment>> var41 = this.n ? Map.of() : (var2 != null ? a(var2) : e(var1));

         for (io.karatelabs.ext.coverage.collect.RunEvidenceBundle.ExecutionAccumulator var21 : (Iterable<io.karatelabs.ext.coverage.collect.RunEvidenceBundle.ExecutionAccumulator>) (Iterable<?>) (var40.values())) {
            ArrayList<io.karatelabs.ext.coverage.collect.ExecutionFragment> var22 = new ArrayList();
            var22.add(
               new ExecutionFragment(io.karatelabs.ext.coverage.collect.ExecutionFragment.ExecutionFragmentKind.ORIGINAL, var21.a, var21.f, var21.d, var21.e, var21.g, false, var21.h, null, null, null, 0, Set.of(), List.of(), List.of(), List.of())
            );
            var22.add(
               new ExecutionFragment(io.karatelabs.ext.coverage.collect.ExecutionFragment.ExecutionFragmentKind.TRANSCRIPT, var21.a, io.karatelabs.ext.coverage.collect.TestExecution.ExecutionOutcome.UNKNOWN, null, null, null, var17[0], var21.h, null, null, null, 0, Set.of(), var21.k, var21.l, var21.j)
            );
            var22.addAll(var41.getOrDefault(var21.a, List.of()));
            String var23 = a(b(var22), var36 == null ? null : var36.g(), var21.b);
            TestExecution var24 = io.karatelabs.ext.coverage.collect.ExecutionFragmentAggregator.a(
               new io.karatelabs.ext.coverage.collect.ExecutionFragmentAggregator.ExecutionDescriptor(var21.a, var3, "check", var21.b(), var21.b, var21.c, var23, "runs/" + var3, 0, var14), var22, var9
            );
            this.a(var10 ? var24.c() : var24, var13);
         }

         String var42 = var36 == null ? null : var36.f();
         List<ExecutionFragment> var43 = var41.getOrDefault(var39.a, List.of());
         boolean var44 = var43.stream().anyMatch(var0 -> var0.a() == io.karatelabs.ext.coverage.collect.ExecutionFragment.ExecutionFragmentKind.CONTAINER);
         boolean var45 = var39.a() || !var12.isEmpty() || var44 || var40.isEmpty() && !"check".equals(var42);
         if (var45) {
            io.karatelabs.ext.coverage.graph.TestNode.TestStatus var46 = a(var37);
            boolean var25 = var37 != null && "running".equals(var37.get("status"));

            io.karatelabs.ext.coverage.collect.TestExecution.ExecutionOutcome var26 = switch (var46) {
               case PASSED -> io.karatelabs.ext.coverage.collect.TestExecution.ExecutionOutcome.PASSED;
               case FAILED -> io.karatelabs.ext.coverage.collect.TestExecution.ExecutionOutcome.FAILED;
               default -> io.karatelabs.ext.coverage.collect.TestExecution.ExecutionOutcome.SKIPPED;
            };
            var39.f = var26;
            var39.e = var37 != null && var37.get("finishedMs") instanceof Number var27 ? var27.longValue() : null;
            var39.h = !var25 || !var9;
            ArrayList<io.karatelabs.ext.coverage.collect.ExecutionFragment> var47 = new ArrayList();
            var47.add(
               new ExecutionFragment(io.karatelabs.ext.coverage.collect.ExecutionFragment.ExecutionFragmentKind.ORIGINAL, var39.a, var26, var39.d, var39.e, null, false, var39.h, null, null, null, 0, Set.of("ai"), List.of(), List.of(), List.of())
            );
            var47.add(
               new ExecutionFragment(io.karatelabs.ext.coverage.collect.ExecutionFragment.ExecutionFragmentKind.TRANSCRIPT, var39.a, io.karatelabs.ext.coverage.collect.TestExecution.ExecutionOutcome.UNKNOWN, null, null, null, var17[0], var39.h, null, null, null, 0, Set.of(), var39.k, var39.l, var39.j)
            );
            var47.addAll(var43);
            String var48 = var37 == null ? var3 : a(a(var37.get("task")), a(var37.get("check")), var3);
            TestExecution var29 = io.karatelabs.ext.coverage.collect.ExecutionFragmentAggregator.a(new io.karatelabs.ext.coverage.collect.ExecutionFragmentAggregator.ExecutionDescriptor(var39.a, var3, "session", var11, null, null, var48, "runs/" + var3, 0, var14), var47, var9);
            if (!var8 && var36 != null && var36.g() != null && !"check".equals(var42)) {
               var29 = var29.a("ambiguousAttribution", "check " + var36.g() + " ran in this session with no bracket");
               this.a("ambiguousAttribution").add(var3);
            }

            this.a(var29, var13);

            for (String var31 : (Iterable<String>) (Iterable<?>) (var12)) {
               if (!var39.m.contains(var31)) {
                  io.karatelabs.ext.coverage.graph.CoverageHit var32 = io.karatelabs.ext.coverage.graph.CoverageHit.a(var31, var11, (Map<String, Object>)null).a(var39.a);
                  this.j.add(var32);
                  this.i.add(var32);
               }
            }
         }

         if (!var8) {
            this.a("unmanifested").add(var3);
            this.a("finalizationInferred").add(var3);
         } else if (!var36.d()) {
            this.a(var9 ? "unfinalized" : "finalizationInferred").add(var3);
         }
      }
   }

   private void a(TestExecution var1, String var2) {
      if (var2 != null) {
         var1 = var1.b(Map.of("report", var2));
      }

      this.k.add(var1);
      this.h.add(var1.d());
      this.i.addAll(var1.y());
      if (var1.a("incompleteEvidence")) {
         this.a("incompleteEvidence").add(var1.g());
      }
   }

   private List<String> a(String var1) {
      return (List<String>)this.m.get(var1);
   }

   private static Map<String, List<ExecutionFragment>> e(Path var0) {
      Path var1 = MaxPaths.eventsFile(var0);
      if (!Files.isRegularFile(var1)) {
         return Map.of();
      } else {
         try {
            return a(io.karatelabs.ext.coverage.collect.JsonlRunEvidenceReader.a(var1).h());
         } catch (Exception var3) {
            e.warn("run evidence: could not read {}: {}", var1, var3.getMessage());
            return Map.of();
         }
      }
   }

   private static Map<String, List<ExecutionFragment>> a(List<ExecutionFragment> var0) {
      LinkedHashMap<java.lang.String, java.util.List<io.karatelabs.ext.coverage.collect.ExecutionFragment>> var1 = new LinkedHashMap();

      for (ExecutionFragment var3 : var0) {
         var1.computeIfAbsent(var3.b(), var0x -> new ArrayList<>()).add(var3);
      }

      return var1;
   }

   private static String b(List<ExecutionFragment> var0) {
      for (ExecutionFragment var2 : var0) {
         if (var2.a() == io.karatelabs.ext.coverage.collect.ExecutionFragment.ExecutionFragmentKind.CHECK_BLOCK) {
            return var2.j();
         }
      }

      return null;
   }

   private void a(String var1, String var2, String var3, List<String> var4, io.karatelabs.ext.coverage.collect.RunEvidenceBundle.ExecutionAccumulator var5, Map<String, io.karatelabs.ext.coverage.collect.RunEvidenceBundle.ExecutionAccumulator> var6, io.karatelabs.ext.coverage.collect.RunEvidenceBundle.ExecutionAccumulator[] var7, boolean[] var8) {
      Map var9;
      try {
         if (!(Json.of(var1).value() instanceof Map var11)) {
            return;
         }

         var9 = var11;
      } catch (Exception var23) {
         return;
      }

      String var24 = a(var9.get("type"));
      if ("capture".equals(var24)) {
         var8[0] |= Boolean.TRUE.equals(var9.get("assertions"));
      } else if ("execution-start".equals(var24)) {
         String var27 = a(var9.get("executionId"));
         if (var27 != null) {
            io.karatelabs.ext.coverage.collect.RunEvidenceBundle.ExecutionAccumulator var29 = new io.karatelabs.ext.coverage.collect.RunEvidenceBundle.ExecutionAccumulator(
               var27, a(var9.get("definitionId")), a(var9.get("caseId")), var9.get("startedAt") instanceof Number var32 ? var32.longValue() : null
            );
            if (var6.containsKey(var27)) {
               this.a("duplicateExecutionIds").add(var27);
               var29.i = true;
            } else {
               var6.put(var27, var29);
            }

            var7[0] = var29;
         }
      } else if ("execution-end".equals(var24)) {
         io.karatelabs.ext.coverage.collect.RunEvidenceBundle.ExecutionAccumulator var26 = var7[0] != null && var7[0].i && var7[0].a.equals(a(var9.get("executionId"))) ? var7[0] : (io.karatelabs.ext.coverage.collect.RunEvidenceBundle.ExecutionAccumulator)var6.get(a(var9.get("executionId")));
         if (var26 != null && !var26.i) {
            var26.f = io.karatelabs.ext.coverage.collect.TestExecution.ExecutionOutcome.of(a(var9.get("outcome")));
            var26.e = var9.get("endedAt") instanceof Number var28 ? var28.longValue() : null;
            var26.g = a(var9.get("error"));
            var26.h = true;
         }

         if (var7[0] == var26) {
            var7[0] = null;
         }
      } else {
         io.karatelabs.ext.coverage.collect.RunEvidenceBundle.ExecutionAccumulator var25 = var7[0] != null ? var7[0] : var5;
         if (!var25.i) {
            String var12 = var25 == var5 ? var3 : var25.b();
            if (!this.n || !"emit".equals(var24)) {
               if ("assertion".equals(var24)) {
                  var25.j.add(var9);
               }

               if ("emit".equals(var24)) {
                  Action var30 = a(var9, var2, var12);
                  if (var30 != null) {
                     var25.k.add(var30);
                  }
               } else {
                  String var14 = null;
                  String var15 = null;
                  String var13;
                  if ("step".equals(var24)) {
                     var13 = a(var9.get("name"));
                     String var16 = a(var9.get("status"));
                     if ("pass".equals(var16) || "fail".equals(var16)) {
                        var15 = var16;
                     }
                  } else {
                     if (!"screenshot".equals(var24)) {
                        this.a(var9, var24, var2, var3);
                        return;
                     }

                     var13 = a(var9.get("title"));
                     String var34 = a(a(var9.get("file")), a(var9.get("filename")));
                     if (var34 != null) {
                        var14 = var2 + "/screenshots/" + var34;
                     }
                  }

                  var var35 = new LinkedHashSet<>(b(var13));
                  var35.addAll(a(var13, g));
                  List<String> var17 = var35.isEmpty() ? (var25 == var5 ? var4 : List.of()) : new ArrayList<>(var35);
                  if (!var17.isEmpty()) {
                     LinkedHashMap<java.lang.String, java.lang.Object> var18 = new LinkedHashMap();
                     if (var14 != null) {
                        var18.put("shot", var14);
                     }

                     String var19 = d(var13);
                     if (var19 != null && !var19.isBlank()) {
                        var18.put("note", var19);
                     }

                     if (var9.get("ts") instanceof Number var21) {
                        var18.put("t", var21.longValue());
                     }

                     if (var15 != null && (!var35.isEmpty() || "fail".equals(var15))) {
                        var18.put("status", var15);
                     }

                     for (String var22 : (Iterable<String>) (Iterable<?>) (var17)) {
                        var25.l.add(io.karatelabs.ext.coverage.graph.CoverageHit.a(var22, var12, var18.isEmpty() ? null : var18).a(var25.a));
                        var25.m.add(var22);
                     }
                  }
               }
            }
         }
      }
   }

   private static Action a(Map<String, Object> var0, String var1, String var2) {
      if ("coverage-emit".equals(var0.get("name")) && var0.get("payload") instanceof Map var3) {
         String var5 = a(var3.get("source"));
         String var6 = a(var3.get("key"));
         if (var5 != null && var6 != null) {
            Map var7 = var3.get("evidence") instanceof Map var8 ? var8 : null;
            long var12 = var0.get("ts") instanceof Number var10 ? var10.longValue() : 0L;
            return new io.karatelabs.ext.coverage.EmitAction(
               "emit-" + var1 + "-" + var12 + "-" + var6, Set.of(), Instant.ofEpochMilli(var12), "runs/" + var1, var2, 0, var2, var5, var6, var7
            );
         } else {
            return null;
         }
      } else {
         return null;
      }
   }

   private void a(Map<String, Object> var1, String var2, String var3, String var4) {
      for (Entry var6 : var1.entrySet()) {
         if (var6.getValue() instanceof String var7 && !var7.isBlank() && (f.matcher(var7).find() || g.matcher(var7).find())) {
            LinkedHashMap<java.lang.String, java.lang.Object> var9 = new LinkedHashMap();
            var9.put("run", var3);
            var9.put("test", var4);
            var9.put("eventType", var2 == null ? "?" : var2);
            var9.put("field", var6.getKey());
            var9.put("text", var7);
            var9.put(
               "detail",
               "assertion".equals(var2)
                  ? "an @req=/@cov= token in an assertion label attaches NO coverage — only a recorded step does. Stamp it with Robot.step('… @req=<id>/<criterion>', 'pass'|'fail'), or let Rule.execute('<rulebook>', row).verify(ok, note) emit it from the rulebook's own calc.req."
                  : "an @req=/@cov= token on a '" + var2 + "' event attaches NO coverage — only a recorded step (or screenshot title) is scanned for tokens."
            );
            this.l.add(var9);
         }
      }
   }

   private static List<String> b(String var0) {
      return a(var0, f);
   }

   private static List<String> a(String var0, Pattern var1) {
      ArrayList<java.lang.String> var2 = new ArrayList();
      if (var0 == null) {
         return var2;
      } else {
         Matcher var3 = var1.matcher(var0);

         while (var3.find()) {
            for (String var7 : var3.group(1).split(",")) {
               String var8 = var7.trim();
               if (!var8.isEmpty()) {
                  var2.add(io.karatelabs.ext.coverage.graph.TraceGraph.b(var8));
               }
            }
         }

         return var2;
      }
   }

   private static List<String> c(String var0) {
      if (var0 != null && !var0.isBlank()) {
         if (var0.contains("@req=")) {
            return b(var0);
         } else {
            ArrayList<java.lang.String> var1 = new ArrayList();

            for (String var5 : var0.split(",")) {
               String var6 = var5.trim();
               if (!var6.isEmpty() && !var6.contains(" ")) {
                  var1.add(io.karatelabs.ext.coverage.graph.TraceGraph.b(var6));
               }
            }

            return var1;
         }
      } else {
         return List.of();
      }
   }

   private static String d(String var0) {
      if (var0 == null) {
         return null;
      } else {
         String var1 = f.matcher(var0).replaceAll("");
         var1 = g.matcher(var1).replaceAll("");
         return var1.replaceAll("\\s{2,}", " ").trim();
      }
   }

   private static io.karatelabs.ext.coverage.graph.TestNode.TestStatus a(Map<String, Object> var0) {
      String var1 = var0 == null ? null : a(var0.get("status"));
      if ("completed".equals(var1)) {
         return Boolean.FALSE.equals(var0.get("succeeded")) ? io.karatelabs.ext.coverage.graph.TestNode.TestStatus.FAILED : io.karatelabs.ext.coverage.graph.TestNode.TestStatus.PASSED;
      } else {
         return "failed".equals(var1) ? io.karatelabs.ext.coverage.graph.TestNode.TestStatus.FAILED : io.karatelabs.ext.coverage.graph.TestNode.TestStatus.SKIPPED;
      }
   }

   private static String a(Object var0) {
      return var0 == null ? null : String.valueOf(var0);
   }

   private static String a(String... var0) {
      for (String var4 : var0) {
         if (var4 != null && !var4.isBlank()) {
            return var4;
         }
      }

      return null;
   }

   private static final class ExecutionAccumulator {
      final String a;
      final String b;
      final String c;
      final Long d;
      Long e;
      io.karatelabs.ext.coverage.collect.TestExecution.ExecutionOutcome f = io.karatelabs.ext.coverage.collect.TestExecution.ExecutionOutcome.UNKNOWN;
      String g;
      boolean h;
      boolean i;
      final List<Map<String, Object>> j = new ArrayList<>();
      final List<Action> k = new ArrayList<>();
      final List<io.karatelabs.ext.coverage.graph.CoverageHit> l = new ArrayList<>();
      final Set<String> m = new LinkedHashSet<>();

      ExecutionAccumulator(String var1, String var2, String var3, Long var4) {
         this.a = var1;
         this.b = var2;
         this.c = var3;
         this.d = var4;
      }

      boolean a() {
         return !this.j.isEmpty() || !this.k.isEmpty() || !this.l.isEmpty();
      }

      String b() {
         return this.b != null ? this.b : this.a;
      }
   }
}
