package io.karatelabs.ext.coverage.collect;

import io.karatelabs.common.Json;
import io.karatelabs.ext.coverage.Action;
import io.karatelabs.ext.coverage.GrpcAction;
import io.karatelabs.ext.coverage.KafkaAction;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JsonlRunEvidenceReader implements io.karatelabs.ext.coverage.CoverageRunEvidence {
   private static final Logger b = LoggerFactory.getLogger(JsonlRunEvidenceReader.class);
   private static final String c = "karate-events";
   public static final String a = "recording:";
   private final List<Action> d;
   private final List<io.karatelabs.ext.coverage.TestExecutionSummary> e = new ArrayList<>();
   private final List<TestExecution> f = new ArrayList<>();
   private final List<ExecutionFragment> g = new ArrayList<>();
   private final boolean h;
   private final boolean i;
   private boolean j;
   private final Map<String, Object> k;
   private final List<String> l;
   private final List<String> m = new ArrayList<>();
   private final List<String> n = new ArrayList<>();
   private final List<String> o = new ArrayList<>();
   private final List<String> p = new ArrayList<>();
   private final List<String> q = new ArrayList<>();
   private final List<String> r = new ArrayList<>();
   private int s;
   private int t = 0;
   private String u;
   private boolean v;
   private io.karatelabs.ext.coverage.collect.JsonlRunEvidenceReader.RunParseContext w;
   private final Map<String, io.karatelabs.ext.coverage.collect.JsonlRunEvidenceReader.FeatureRecord> x = new HashMap<>();
   private final Map<String, Deque<io.karatelabs.ext.coverage.collect.JsonlRunEvidenceReader.ScenarioRecord>> y = new LinkedHashMap<>();

   public JsonlRunEvidenceReader(Path var1) throws IOException {
      this(List.of(var1));
   }

   public JsonlRunEvidenceReader(List<Path> var1) throws IOException {
      this(var1, false, false);
   }

   public static JsonlRunEvidenceReader a(Path var0) throws IOException {
      return new JsonlRunEvidenceReader(List.of(var0), true, false);
   }

   public static JsonlRunEvidenceReader b(Path var0) throws IOException {
      return new JsonlRunEvidenceReader(List.of(var0), false, true);
   }

   private JsonlRunEvidenceReader(List<Path> var1, boolean var2, boolean var3) throws IOException {
      this.h = var2;
      this.i = var3;
      this.l = var1.stream().map(Path::toString).toList();
      this.d = new ArrayList<>();
      HashSet var4 = new HashSet();

      for (Path var6 : var1) {
         Path var7;
         try {
            var7 = var6.toRealPath();
         } catch (IOException var9) {
            var7 = var6.toAbsolutePath().normalize();
         }

         if (!var4.add(var7)) {
            this.s++;
         } else if (!var2 && !var3) {
            this.a(io.karatelabs.ext.coverage.collect.EvidenceCache.a(var6, () -> c(var6)));
         } else {
            this.v = false;
            this.f(var6);
         }
      }

      this.k = this.l();
   }

   private static JsonlRunEvidenceReader c(Path var0) throws IOException {
      JsonlRunEvidenceReader var1 = new JsonlRunEvidenceReader(var0.toString());
      var1.f(var0);
      return var1;
   }

   private JsonlRunEvidenceReader(String var1) {
      this.h = false;
      this.i = false;
      this.l = List.of(var1);
      this.d = new ArrayList<>();
      this.k = this.l();
   }

   private void a(JsonlRunEvidenceReader var1) {
      this.d.addAll(var1.d);
      this.e.addAll(var1.e);
      this.f.addAll(var1.f);
      this.g.addAll(var1.g);
      this.m.addAll(var1.m);
      this.n.addAll(var1.n);
      this.o.addAll(var1.o);
      this.p.addAll(var1.p);
      this.q.addAll(var1.q);
      this.r.addAll(var1.r);
      this.j = this.j | var1.j;
   }

   boolean f() {
      return this.q.isEmpty() && !this.j;
   }

   @Override
   public List<Action> a() {
      return Collections.unmodifiableList(this.d);
   }

   @Override
   public List<io.karatelabs.ext.coverage.TestExecutionSummary> b() {
      return Collections.unmodifiableList(this.e);
   }

   public List<TestExecution> g() {
      return Collections.unmodifiableList(this.f);
   }

   public List<ExecutionFragment> h() {
      return Collections.unmodifiableList(this.g);
   }

   public Map<String, Object> i() {
      LinkedHashMap var1 = new LinkedHashMap();
      var1.put("unmanifested", this.m);
      var1.put("allocatedAtExit", this.n);
      var1.put("finalizationInferred", this.o);
      var1.put("incompleteEvidence", this.p);
      var1.put("unfinalized", this.q);
      var1.put("runningElsewhere", this.r);
      var1.put("duplicateInputs", this.s);
      return var1;
   }

   @Override
   public String c() {
      return "karate-events";
   }

   @Override
   public Map<String, Object> d() {
      return this.k;
   }

   private TestExecution a(TestExecution var1) {
      if (this.w.i != null) {
         var1 = var1.b(Map.of("report", this.w.i));
      }

      return this.w.f ? var1.c() : var1;
   }

   private io.karatelabs.ext.coverage.collect.JsonlRunEvidenceReader.RunParseContext d(Path var1) throws IOException {
      EvidenceRunMetadata var2 = io.karatelabs.ext.coverage.collect.EvidenceRunMetadata.a(var1);
      if (var2 == null) {
         return new io.karatelabs.ext.coverage.collect.JsonlRunEvidenceReader.RunParseContext("import:" + e(var1), null, false);
      } else {
         Path var3 = var1.toAbsolutePath().getParent().getParent();
         boolean var4 = io.karatelabs.ext.coverage.collect.CoveragePathFilterRegistry.a(var2.a());
         boolean var5 = !var4 && var2.c() && var2.c(var3);
         if (var5) {
            this.r.add(var2.a());
         }

         io.karatelabs.ext.coverage.collect.JsonlRunEvidenceReader.RunParseContext var6 = new io.karatelabs.ext.coverage.collect.JsonlRunEvidenceReader.RunParseContext(var2.a(), var2, var4 || var5);
         var6.f = var6.c && var6.d && !var2.d() && !this.i;
         var6.i = var2.d(var3);
         if (!var6.c) {
            String var7 = var2.f();
            var6.j = var2.g();
            var6.k = var6.j == null ? null : io.karatelabs.ext.coverage.collect.EvidenceRunMetadata.a(var6.j);
            var6.l = "check".equals(var7) && var6.k != null;
            if (!"check".equals(var7) && !"suite".equals(var7) && Files.isRegularFile(var3.resolve("transcript.jsonl"))) {
               var6.m = "live".equals(var7) ? io.karatelabs.ext.coverage.collect.ExecutionFragment.ExecutionFragmentKind.REPLAY : io.karatelabs.ext.coverage.collect.ExecutionFragment.ExecutionFragmentKind.CONTAINER;
            }
         }

         return var6;
      }
   }

   private static String e(Path var0) throws IOException {
      MessageDigest var1;
      try {
         var1 = MessageDigest.getInstance("SHA-256");
      } catch (Exception var7) {
         return Long.toHexString(Files.size(var0));
      }

      try (DigestInputStream var2 = new DigestInputStream(Files.newInputStream(var0), var1)) {
         var2.transferTo(OutputStream.nullOutputStream());
      }

      byte[] var8 = var1.digest();
      StringBuilder var3 = new StringBuilder();

      for (int var4 = 0; var4 < 6; var4++) {
         var3.append(String.format("%02x", var8[var4]));
      }

      return var3.toString();
   }

   private void f(Path var1) throws IOException {
      this.w = this.d(var1);
      this.y.clear();

      String var3;
      try (BufferedReader var2 = Files.newBufferedReader(var1, StandardCharsets.UTF_8)) {
         while ((var3 = var2.readLine()) != null) {
            if (!var3.trim().isEmpty()) {
               try {
                  this.a(var3);
               } catch (Exception var6) {
                  b.warn("skipping malformed event line in {}: {}", var1, var6.toString());
               }
            }
         }
      }

      this.k();
   }

   private void k() {
      if (this.h) {
         this.y.clear();
      } else {
         for (Deque var2 : this.y.values()) {
            for (io.karatelabs.ext.coverage.collect.JsonlRunEvidenceReader.ScenarioRecord var4 : (Iterable<io.karatelabs.ext.coverage.collect.JsonlRunEvidenceReader.ScenarioRecord>) (Iterable<?>) (var2)) {
               String var5 = this.a(var4, null);
               ExecutionFragment var6 = new ExecutionFragment(
                  io.karatelabs.ext.coverage.collect.ExecutionFragment.ExecutionFragmentKind.STREAM, var5, io.karatelabs.ext.coverage.collect.TestExecution.ExecutionOutcome.UNKNOWN, null, null, null, this.v, false, var4.e, var4.b, var4.a, var4.c, var4.g, List.of(), List.of(), List.of()
               );
               TestExecution var7 = io.karatelabs.ext.coverage.collect.ExecutionFragmentAggregator.a(
                  new io.karatelabs.ext.coverage.collect.ExecutionFragmentAggregator.ExecutionDescriptor(
                     var5,
                     this.w.a,
                     "scenario",
                     a(var4.e, var4.a, var4.b),
                     var4.f != null ? var4.f : var4.e,
                     var4.f != null ? String.valueOf(var4.d) : null,
                     var4.b,
                     var4.a,
                     var4.c,
                     this.w.e
                  ),
                  List.of(var6),
                  this.w.d
               );
               if (var7.a("incompleteEvidence")) {
                  this.p.add(var5);
               }

               this.f.add(this.a(var7));
               this.e.add(new io.karatelabs.ext.coverage.TestExecutionSummary(var4.b, var4.c, var4.g, var4.a, var7.n() == io.karatelabs.ext.coverage.collect.TestExecution.ExecutionPhase.RUNNING ? "running" : "unknown", var4.e, null));
            }
         }

         this.y.clear();
         if (!this.w.c) {
            this.m.add(this.w.a);
            this.o.add(this.w.a);
         } else if (!this.w.b.d()) {
            boolean var8 = this.g.stream().anyMatch(var0 -> var0.a() == io.karatelabs.ext.coverage.collect.ExecutionFragment.ExecutionFragmentKind.REPLAY);
            if (this.i && !var8 && !this.w.d) {
               return;
            }

            this.j = this.j | this.i;
            if (this.w.d) {
               this.q.add(this.w.a);
            } else {
               this.o.add(this.w.a);
            }
         }
      }
   }

   public boolean j() {
      return this.j;
   }

   private void a(String var1) {
      Json var2 = Json.of(var1);
      String var3 = var2.get("type", "");
      Number var4 = var2.get("timeStamp", 0L);
      Instant var5 = Instant.ofEpochMilli(var4.longValue());
      Map var6 = var2.get("data", null);
      if (var6 != null) {
         switch (var3) {
            case "SUITE_ENTER":
               this.a(var6);
               break;
            case "FEATURE_ENTER":
               this.b(var6);
               break;
            case "SCENARIO_ENTER":
               this.c(var6);
               break;
            case "FEATURE_EXIT":
               this.a(var6, var5);
         }
      }
   }

   private void a(Map<String, Object> var1) {
      this.v = false;
      Object var3 = var1.get("exts");
      if (var3 instanceof List) {
         for (Object var4 : (List)var3) {
            if (var4 instanceof Map var5 && Boolean.TRUE.equals(var5.get("assertions"))) {
               this.v = true;
               return;
            }
         }
      }
   }

   private void b(Map<String, Object> var1) {
      this.u = a(var1, "path", a(var1, "relativePath", a(var1, "prefixedPath", "")));
   }

   private void c(Map<String, Object> var1) {
      String var2 = a(var1, "name", "");
      int var3 = a(var1, "line", 0);
      var var4 = a((List<String>)var1.get("tags"));
      this.x.put(a(this.u, var2), new io.karatelabs.ext.coverage.collect.JsonlRunEvidenceReader.FeatureRecord(var2, var3, var4, this.u, ""));
      int var5 = a(var1, "executionIndex", -1);
      if (a(var1, "callDepth", 0) <= 0 || var5 > 0) {
         String var6 = a(var1, "feature", this.u);
         int var7 = a(var1, "exampleIndex", -1);
         io.karatelabs.ext.coverage.collect.JsonlRunEvidenceReader.ScenarioRecord var8 = new io.karatelabs.ext.coverage.collect.JsonlRunEvidenceReader.ScenarioRecord(
            var6,
            var2,
            var3,
            var7,
            a(var1, "slug", ""),
            var1.get("outlineSlug") == null ? null : String.valueOf(var1.get("outlineSlug")),
            var4,
            ++this.w.g,
            var5
         );
         this.y.computeIfAbsent(b(var6, var3, var7, var5), var0 -> new ArrayDeque<>()).add(var8);
      }
   }

   private void a(Map<String, Object> var1, Instant var2) {
      String var3 = a(var1, "relativePath", a(var1, "prefixedPath", this.u));
      if (var3 != null && !var3.isEmpty()) {
         this.u = var3;
      }

      boolean var4 = a(var1, "prefixedPath", "").startsWith("classpath:");
      String var5 = a(var1, "prefixedPath", "").startsWith("recording:") ? a(var1, "prefixedPath", "").substring("recording:".length()) : null;
      List var6 = (List)var1.get("scenarioResults");
      if (var6 != null) {
         for (Map var8 : (Iterable<Map>) (Iterable<?>) (var6)) {
            String var9 = a(var8, "name", "");
            int var10 = a(var8, "line", 0);
            String var11 = a(var8, "slug", "");
            if (var11.isEmpty()) {
               var11 = a(this.u, var9);
            }

            var var12 = a((List<String>)var8.get("tags"));
            io.karatelabs.ext.coverage.collect.JsonlRunEvidenceReader.FeatureRecord var13 = this.x.get(a(this.u, var9));
            Set var14 = var13 != null && !var13.c().isEmpty() ? var13.c() : var12;
            int var15 = var13 != null && var13.b() != 0 ? var13.b() : var10;
            io.karatelabs.ext.coverage.collect.JsonlRunEvidenceReader.FeatureRecord var16 = new io.karatelabs.ext.coverage.collect.JsonlRunEvidenceReader.FeatureRecord(var9, var15, var14, this.u, var11);
            Set var17 = !var12.isEmpty() ? var12 : var16.c();
            io.karatelabs.ext.coverage.collect.ExecutionFragment.ExecutionFragmentKind var18 = a(var17);
            boolean var19 = var18 == io.karatelabs.ext.coverage.collect.ExecutionFragment.ExecutionFragmentKind.STREAM && this.w.k != null && (this.w.l || var11.equals(this.w.j));
            String var20 = null;
            if (var18 == io.karatelabs.ext.coverage.collect.ExecutionFragment.ExecutionFragmentKind.STREAM && !var19 && this.w.m != null && var16.d().isBlank()) {
               var18 = this.w.m;
               var20 = io.karatelabs.ext.coverage.collect.EvidenceRunMetadata.a(this.w.a, 0);
            }

            if (!this.h || var18 != io.karatelabs.ext.coverage.collect.ExecutionFragment.ExecutionFragmentKind.STREAM) {
               ArrayList var21 = new ArrayList();
               ArrayList var22 = new ArrayList();
               List var23 = (List)var8.get("stepResults");

               for (Map var25 : (Iterable<Map>) (Iterable<?>) (var23 == null ? List.of() : var23)) {
                  List var26 = (List)var25.get("embeds");
                  if (var26 != null) {
                     for (Map var28 : (Iterable<Map>) (Iterable<?>) (var26)) {
                        String var29 = a(var28, "name", "");
                        boolean var30 = "http-exchange".equals(var29);
                        boolean var31 = "grpc-match".equals(var29);
                        boolean var32 = "kafka-match".equals(var29);
                        boolean var33 = "coverage-emit".equals(var29);
                        boolean var34 = "evidence".equals(var29);
                        if (var30 || var31 || var32 || var33 || var34) {
                           String var35 = d(var28);
                           if (!var35.isEmpty()) {
                              try {
                                 byte[] var36 = Base64.getDecoder().decode(var35);
                                 String var37 = new String(var36, StandardCharsets.UTF_8);
                                 var var38 = Json.of(var37).asMap();
                                 if (var34) {
                                    if ("assertion".equals(var38.get("kind"))) {
                                       var21.add(var38);
                                    }
                                 } else {
                                    Object var39 = var30
                                       ? this.a(var38, var16, var2)
                                       : (var31 ? this.b(var38, var16, var2) : (var32 ? this.c(var38, var16, var2) : this.d(var38, var16, var2)));
                                    if (var39 != null) {
                                       var22.add(var39);
                                    }
                                 }
                              } catch (Exception var40) {
                                 b.debug("skipping malformed '{}' embed in scenario '{}': {}", new Object[]{var29, var9, var40.toString()});
                              }
                           }
                        }
                     }
                  }
               }

               String var41 = e(var8);
               int var42 = a(var8, "exampleIndex", -1);
               io.karatelabs.ext.coverage.collect.JsonlRunEvidenceReader.ScenarioRecord var43 = this.a(this.u, var10, var42, a(var8, "executionIndex", -1));
               String var44 = a(var17, "exec");
               ExecutionFragment var45 = new ExecutionFragment(
                  var18,
                  null,
                  io.karatelabs.ext.coverage.collect.TestExecution.ExecutionOutcome.of(var41),
                  a(var8, "startTime"),
                  a(var8, "endTime"),
                  a(var8, "error", null),
                  this.v,
                  true,
                  var11,
                  var9,
                  var16.d(),
                  var10,
                  var17,
                  var22,
                  List.of(),
                  var21
               );
               if (var18 == io.karatelabs.ext.coverage.collect.ExecutionFragment.ExecutionFragmentKind.STREAM || var44 == null && var20 == null) {
                  String var47 = this.a(var43, var8);
                  String var48 = a(var43, var11, var42);
                  String var49 = var19 ? this.w.k : (var48 != null ? var48 : var11);
                  String var50 = var19 ? null : a(var48, var11, var42, var8.get("exampleData"));
                  String var51 = var19 ? this.w.k : a(var11, var16.d(), var9, var49, var50);
                  TestExecution var52 = io.karatelabs.ext.coverage.collect.ExecutionFragmentAggregator.a(
                     new io.karatelabs.ext.coverage.collect.ExecutionFragmentAggregator.ExecutionDescriptor(var47, this.w.a, var19 ? "check" : "scenario", var51, var49, var50, var9, var16.d(), var10, this.w.e),
                     List.of(a(var45, var47)),
                     this.w.d
                  );
                  if (var4) {
                     var52 = var52.a(Map.of("origin", "classpath"));
                  }

                  if (var5 != null) {
                     var52 = var52.a(Map.of("origin", "recording", "recording", var5));
                  }

                  String var53 = var50 != null && var50.equals(a(var8.get("exampleData"))) ? String.valueOf(((Map)var8.get("exampleData")).get("_deck")) : null;
                  if (var53 != null) {
                     var52 = var52.a(Map.of("deck", var53));
                  }

                  String var54 = var50 != null && var50.matches("\\d+") ? b(var8.get("exampleData")) : null;
                  if (var54 != null) {
                     var52 = var52.a(Map.of("exampleId", var54));
                  }

                  this.f.add(this.a(var52));
                  this.d.addAll(var22);
                  this.e.add(new io.karatelabs.ext.coverage.TestExecutionSummary(var9, var10, var17, var16.d(), var41, var11, var52.q()));
               } else {
                  String var46 = var20 != null ? var20 : (var44.matches("\\d+") ? io.karatelabs.ext.coverage.collect.EvidenceRunMetadata.a(this.w.a, Integer.parseInt(var44)) : var44);
                  this.g.add(a(var45, var46));
               }
            }
         }
      }
   }

   private static ExecutionFragment a(ExecutionFragment var0, String var1) {
      return new ExecutionFragment(
         var0.a(),
         var1,
         var0.c(),
         var0.d(),
         var0.e(),
         var0.f(),
         var0.g(),
         var0.h(),
         var0.i(),
         var0.j(),
         var0.k(),
         var0.l(),
         var0.m(),
         var0.n(),
         var0.o(),
         var0.p()
      );
   }

   private String a(io.karatelabs.ext.coverage.collect.JsonlRunEvidenceReader.ScenarioRecord var1, Map<String, Object> var2) {
      int var3 = var2 != null ? a(var2, "executionIndex", -1) : (var1 != null ? var1.i : -1);
      if (var3 > 0) {
         String var4 = io.karatelabs.ext.coverage.collect.EvidenceRunMetadata.a(this.w.a, var3);
         if (var2 != null && var1 == null) {
            this.n.add(var4);
         }

         return var4;
      } else {
         if (this.w.c && !this.w.h) {
            this.w.h = true;
            this.m.add(this.w.a);
         }

         return io.karatelabs.ext.coverage.collect.EvidenceRunMetadata.b(this.w.a, var1 != null ? var1.h : ++this.w.g);
      }
   }

   private static String a(io.karatelabs.ext.coverage.collect.JsonlRunEvidenceReader.ScenarioRecord var0, String var1, int var2) {
      if (var0 != null && var0.f != null) {
         return var0.f;
      } else {
         return var2 >= 0 && var1.endsWith(":" + var2) ? var1.substring(0, var1.length() - (":" + var2).length()) : null;
      }
   }

   private static String a(String var0, String var1, int var2, Object var3) {
      if (var0 == null) {
         return null;
      } else if (!(var0 + ":" + var2).equals(var1)) {
         return var1;
      } else {
         String var4 = a(var3);
         return var4 != null ? var4 : String.valueOf(var2);
      }
   }

   private static String a(Object var0) {
      if (!(var0 instanceof Map var1)) {
         return null;
      } else {
         Object var2 = var1.get("_deck");
         Object var3 = var1.get("_id");
         return var2 != null && var3 != null ? var2 + "/" + var3 : null;
      }
   }

   private static String b(Object var0) {
      return var0 instanceof Map var1 && var1.get("_deck") == null && var1.get("_id") != null ? String.valueOf(var1.get("_id")) : null;
   }

   private io.karatelabs.ext.coverage.collect.JsonlRunEvidenceReader.ScenarioRecord a(String var1, int var2, int var3, int var4) {
      Deque var5 = this.y.get(b(var1, var2, var3, var4));
      return var5 != null && !var5.isEmpty() ? (io.karatelabs.ext.coverage.collect.JsonlRunEvidenceReader.ScenarioRecord)var5.poll() : null;
   }

   private static String b(String var0, int var1, int var2, int var3) {
      return var3 > 0 ? "#" + var3 : var0 + ":" + var1 + ":" + var2;
   }

   private static String a(String var0, String var1, String var2) {
      return var0 != null && !var0.isEmpty() ? var0 : a(var1, var2);
   }

   private static String a(String var0, String var1, String var2, String var3, String var4) {
      return var4 != null && var4.contains("/") && !var0.equals(var4) && var0.equals(var3 + ":" + b(var0)) ? var3 + ":" + var4 : a(var0, var1, var2);
   }

   private static String b(String var0) {
      int var1 = var0.lastIndexOf(58);
      return var1 < 0 ? "" : var0.substring(var1 + 1);
   }

   private static io.karatelabs.ext.coverage.collect.ExecutionFragment.ExecutionFragmentKind a(Set<String> var0) {
      io.karatelabs.ext.coverage.collect.ExecutionFragment.ExecutionFragmentKind var1 = io.karatelabs.ext.coverage.collect.ExecutionFragment.ExecutionFragmentKind.ofTag(a(var0, "fragment"));
      return var1 == null ? io.karatelabs.ext.coverage.collect.ExecutionFragment.ExecutionFragmentKind.STREAM : var1;
   }

   private static String a(Set<String> var0, String var1) {
      for (String var3 : var0) {
         if (var3.startsWith(var1 + "=")) {
            return var3.substring(var1.length() + 1);
         }
      }

      return null;
   }

   private static Set<String> a(List<String> var0) {
      LinkedHashSet var1 = new LinkedHashSet();
      if (var0 != null) {
         for (String var3 : var0) {
            var1.add(var3.startsWith("@") ? var3.substring(1) : var3);
         }
      }

      return var1;
   }

   private static String d(Map<String, Object> var0) {
      if (var0.get("parts") instanceof List var2 && !var2.isEmpty()) {
         Map var3 = null;

         for (Object var5 : var2) {
            if (var5 instanceof Map var6 && "primary".equals(var6.get("role"))) {
               var3 = var6;
               break;
            }
         }

         if (var3 == null && var2.get(0) instanceof Map var7) {
            var3 = var7;
         }

         if (var3 != null) {
            return a(var3, "data", "");
         }
      }

      return a(var0, "data", "");
   }

   private io.karatelabs.ext.coverage.HttpAction a(Map<String, Object> var1, io.karatelabs.ext.coverage.collect.JsonlRunEvidenceReader.FeatureRecord var2, Instant var3) {
      Map var4 = (Map)var1.get("request");
      Map var5 = (Map)var1.get("response");
      if (var4 == null) {
         return null;
      } else {
         String var6 = a(var4, "method", "GET");
         String var7 = a(var4, "url", "");
         String var8 = "";
         LinkedHashMap var9 = new LinkedHashMap();

         try {
            URI var10 = URI.create(var7);
            var8 = var10.getPath();
            String var11 = var10.getQuery();
            if (var11 != null && !var11.isEmpty()) {
               for (String var15 : var11.split("&")) {
                  String[] var16 = var15.split("=", 2);
                  var9.put(var16[0], var16.length > 1 ? var16[1] : "");
               }
            }
         } catch (Exception var20) {
            b.debug("could not parse request URL '{}': {}", var7, var20.toString());
         }

         int var21 = 0;
         long var22 = 0L;
         if (var5 != null) {
            var21 = a(var5, "status", 0);
            var22 = a(var5, "responseTime", 0L);
         }

         String var23 = this.c("http");
         String var24 = var2 != null ? var2.d : this.u;
         String var25 = var2 != null ? var2.a : "";
         int var26 = var2 != null ? var2.b : 0;
         String var17 = var2 != null ? var2.e : "";
         Set var18 = var2 != null ? var2.c : Set.of();
         LinkedHashMap var19 = new LinkedHashMap();
         var19.put("request", var4);
         if (var5 != null) {
            var19.put("response", var5);
         }

         return new io.karatelabs.ext.coverage.HttpAction(
            var23, "http", var18, var3, var24, var25, var26, var17, var6.toUpperCase(), var7, var8, var9, var21, var22, var19
         );
      }
   }

   private GrpcAction b(Map<String, Object> var1, io.karatelabs.ext.coverage.collect.JsonlRunEvidenceReader.FeatureRecord var2, Instant var3) {
      String var4 = a(var1, "service", "");
      String var5 = a(var1, "method", "");
      if (!var4.isEmpty() && !var5.isEmpty()) {
         String var6 = a(var1, "status", "");
         long var7 = a(var1, "durationMs", 0L);
         String var9 = this.c("grpc");
         String var10 = var2 != null ? var2.d : this.u;
         String var11 = var2 != null ? var2.a : "";
         int var12 = var2 != null ? var2.b : 0;
         String var13 = var2 != null ? var2.e : "";
         Set var14 = var2 != null ? var2.c : Set.of();
         Object var15 = var1.get("request");
         return new GrpcAction(var9, var14, var3, var10, var11, var12, var13, var4, var5, var6, var7, var15);
      } else {
         return null;
      }
   }

   private KafkaAction c(Map<String, Object> var1, io.karatelabs.ext.coverage.collect.JsonlRunEvidenceReader.FeatureRecord var2, Instant var3) {
      String var4 = a(var1, "topic", "");
      String var5 = a(var1, "direction", "");
      if (!var4.isEmpty() && !var5.isEmpty()) {
         String var6 = a(var1, "schema", "");
         String var7 = a(var1, "status", "");
         long var8 = a(var1, "durationMs", 0L);
         String var10 = this.c("kafka");
         String var11 = var2 != null ? var2.d : this.u;
         String var12 = var2 != null ? var2.a : "";
         int var13 = var2 != null ? var2.b : 0;
         String var14 = var2 != null ? var2.e : "";
         Set var15 = var2 != null ? var2.c : Set.of();
         Object var16 = var1.get("payload");
         return new KafkaAction(var10, var15, var3, var11, var12, var13, var14, var4, var6.isEmpty() ? null : var6, var5, var7, var8, var16);
      } else {
         return null;
      }
   }

   private io.karatelabs.ext.coverage.EmitAction d(Map<String, Object> var1, io.karatelabs.ext.coverage.collect.JsonlRunEvidenceReader.FeatureRecord var2, Instant var3) {
      String var4 = a(var1, "source", "");
      String var5 = a(var1, "key", "");
      if (!var4.isEmpty() && !var5.isEmpty()) {
         Map var6 = var1.get("evidence") instanceof Map var7 ? var7 : null;
         String var13 = this.c("emit");
         String var14 = var2 != null ? var2.d : this.u;
         String var9 = var2 != null ? var2.a : "";
         int var10 = var2 != null ? var2.b : 0;
         String var11 = var2 != null ? var2.e : "";
         Set var12 = var2 != null ? var2.c : Set.of();
         return new io.karatelabs.ext.coverage.EmitAction(var13, var12, var3, var14, var9, var10, var11, var4, var5, var6);
      } else {
         return null;
      }
   }

   private String c(String var1) {
      return this.w.a + ":" + var1 + "-" + ++this.t;
   }

   private Map<String, Object> l() {
      LinkedHashMap var1 = new LinkedHashMap();
      var1.put("files", this.l);
      var1.put("generatedAt", Instant.now().toString());
      var1.put("actionCount", this.d.size());
      HashSet var2 = new HashSet();

      for (Action var4 : this.d) {
         if (var4.scenarioName() != null && !var4.scenarioName().isEmpty()) {
            var2.add(var4.scenarioName());
         }
      }

      var1.put("scenarioCount", var2.size());
      return Collections.unmodifiableMap(var1);
   }

   private static String a(String var0, String var1) {
      return (var0 == null ? "" : var0) + "::" + var1;
   }

   private static String a(Map<String, Object> var0, String var1, String var2) {
      Object var3 = var0.get(var1);
      return var3 != null ? var3.toString() : var2;
   }

   private static int a(Map<String, Object> var0, String var1, int var2) {
      return var0.get(var1) instanceof Number var4 ? var4.intValue() : var2;
   }

   private static long a(Map<String, Object> var0, String var1, long var2) {
      return var0.get(var1) instanceof Number var5 ? var5.longValue() : var2;
   }

   private static Long a(Map<String, Object> var0, String var1) {
      return var0.get(var1) instanceof Number var3 && var3.longValue() > 0L ? var3.longValue() : null;
   }

   private static String e(Map<String, Object> var0) {
      if (Boolean.TRUE.equals(var0.get("failed"))) {
         return "failed";
      } else {
         return Boolean.TRUE.equals(var0.get("skipped")) ? "skipped" : "passed";
      }
   }

   private record ScenarioRecord(String a, String b, int c, int d, String e, String f, Set<String> g, int h, int i) {

   }

   private static final class RunParseContext {
      final String a;
      final EvidenceRunMetadata b;
      final boolean c;
      final boolean d;
      final Long e;
      boolean f;
      int g;
      boolean h;
      String i;
      String j;
      String k;
      boolean l;
      io.karatelabs.ext.coverage.collect.ExecutionFragment.ExecutionFragmentKind m;

      RunParseContext(String var1, EvidenceRunMetadata var2, boolean var3) {
         this.a = var1;
         this.b = var2;
         this.c = var2 != null && var2.c();
         this.d = var3;
         this.e = var2 != null && var2.b().get("startedMs") instanceof Number var4 ? var4.longValue() : null;
      }
   }

   private record FeatureRecord(String a, int b, Set<String> c, String d, String e) {

   }
}
