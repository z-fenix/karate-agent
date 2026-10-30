package io.karatelabs.ext.sim;

import io.karatelabs.common.Json;
import io.karatelabs.ext.coverage.Action;
import io.karatelabs.ext.coverage.GrpcAction;
import io.karatelabs.ext.coverage.EmitAction;
import io.karatelabs.ext.coverage.CoverageRunEvidence;
import io.karatelabs.ext.coverage.HttpAction;
import io.karatelabs.ext.coverage.TestExecutionSummary;
import io.karatelabs.ext.coverage.collect.RequirementsCoverageSource;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

public class SimulationBuilder {
   private static final Instant a = Instant.EPOCH;
   private final List<io.karatelabs.ext.sim.SimulationBuilder.SimulatedScenario> b = new ArrayList<>();
   private final List<io.karatelabs.ext.sim.SimulationBuilder.RequirementBuilder> c = new ArrayList<>();

   public static io.karatelabs.ext.sim.SimulationBuilder a() {
      return new io.karatelabs.ext.sim.SimulationBuilder();
   }

   public io.karatelabs.ext.sim.SimulationBuilder.SimulatedScenario a(String var1, String var2) {
      io.karatelabs.ext.sim.SimulationBuilder.SimulatedScenario var3 = new io.karatelabs.ext.sim.SimulationBuilder.SimulatedScenario(var1, var2);
      this.b.add(var3);
      return var3;
   }

   public io.karatelabs.ext.sim.SimulationBuilder.RequirementBuilder a(String var1) {
      io.karatelabs.ext.sim.SimulationBuilder.RequirementBuilder var2 = new io.karatelabs.ext.sim.SimulationBuilder.RequirementBuilder(var1);
      this.c.add(var2);
      return var2;
   }

   public static io.karatelabs.ext.sim.SimulationBuilder.SimulationSpecBuilder b() {
      return new io.karatelabs.ext.sim.SimulationBuilder.SimulationSpecBuilder();
   }

   public CoverageRunEvidence c() {
      final ArrayList var1 = new ArrayList();
      final ArrayList var2 = new ArrayList();
      int var3 = 0;

      for (io.karatelabs.ext.sim.SimulationBuilder.SimulatedScenario var5 : this.b) {
         var2.add(new TestExecutionSummary(var5.b, var5.c, new LinkedHashSet<>(var5.e), var5.a, var5.f, var5.d()));

         for (io.karatelabs.ext.sim.SimulationBuilder.SimulatedAction var7 : var5.g) {
            var1.add(a(var5, var7, ++var3));
         }
      }

      return new CoverageRunEvidence() {
         @Override
         public List<Action> a() {
            return var1;
         }

         @Override
         public List<TestExecutionSummary> b() {
            return var2;
         }

         @Override
         public String c() {
            return "sim";
         }

         @Override
         public Map<String, Object> d() {
            return Map.of("source", "sim");
         }
      };
   }

   public String d() {
      LinkedHashMap var1 = new LinkedHashMap();

      for (io.karatelabs.ext.sim.SimulationBuilder.SimulatedScenario var3 : this.b) {
         ((java.util.List)(var1.computeIfAbsent(var3.a, var0 -> new ArrayList<>()))).add(var3);
      }

      StringBuilder var8 = new StringBuilder();

      for (Entry var4 : (Iterable<Entry>) (Iterable<?>) (var1.entrySet())) {
         ArrayList var5 = new ArrayList();

         for (io.karatelabs.ext.sim.SimulationBuilder.SimulatedScenario var7 : (Iterable<io.karatelabs.ext.sim.SimulationBuilder.SimulatedScenario>) (Iterable<?>) ((List)var4.getValue())) {
            var5.add(a(var7));
         }

         LinkedHashMap var10 = new LinkedHashMap();
         var10.put("relativePath", var4.getKey());
         var10.put("scenarioResults", var5);
         LinkedHashMap var11 = new LinkedHashMap();
         var11.put("type", "FEATURE_EXIT");
         var11.put("timeStamp", 0);
         var11.put("data", var10);
         var8.append(Json.stringifyStrict(var11)).append('\n');
      }

      return var8.toString();
   }

   public io.karatelabs.ext.coverage.graph.TraceGraph a(io.karatelabs.ext.coverage.CoverageSource... var1) {
      return io.karatelabs.ext.coverage.graph.TraceGraph.a(List.of(var1), this.c());
   }

   public String e() {
      StringBuilder var1 = new StringBuilder("# Requirements\n");

      for (io.karatelabs.ext.sim.SimulationBuilder.RequirementBuilder var3 : this.c) {
         var1.append("\n## ").append(var3.a);
         if (var3.b != null && !var3.b.isBlank()) {
            var1.append(": ").append(var3.b);
         }

         var1.append('\n');
         StringBuilder var4 = new StringBuilder();
         if (var3.c != null) {
            var4.append("@status=").append(var3.c).append(' ');
         }

         if (var3.d != null) {
            var4.append("@criticality=").append(var3.d).append(' ');
         }

         if (var3.e != null) {
            var4.append("@parent=").append(var3.e).append(' ');
         }

         if (!var3.h.isEmpty()) {
            var4.append("@real=").append(String.join(",", var3.h)).append(' ');
         }

         if (!var4.isEmpty()) {
            var1.append(var4.toString().trim()).append('\n');
         }

         if (var3.f != null && !var3.f.isBlank()) {
            var1.append(var3.f.trim()).append('\n');
         }

         if (!var3.g.isEmpty()) {
            var1.append("\n**Acceptance:**\n");

            for (String var6 : var3.g) {
               var1.append("- ").append(var6).append('\n');
            }
         }
      }

      return var1.toString();
   }

   private static Action a(io.karatelabs.ext.sim.SimulationBuilder.SimulatedScenario var0, io.karatelabs.ext.sim.SimulationBuilder.SimulatedAction var1, int var2) {
      var var3 = new LinkedHashSet<>(var0.e);
      String var4 = var0.d();
      String var5 = var1.a;
      switch (var5) {
         case "http":
            String var7 = var1.c;
            LinkedHashMap var8 = new LinkedHashMap();
            int var9 = var7.indexOf(63);
            String var10 = var7;
            if (var9 >= 0) {
               var10 = var7.substring(0, var9);

               for (String var14 : var7.substring(var9 + 1).split("&")) {
                  String[] var15 = var14.split("=", 2);
                  var8.put(var15[0], var15.length > 1 ? var15[1] : "");
               }
            }

            return new HttpAction(
               "http-" + var2, "http", var3, a, var0.a, var0.b, var0.c, var4, var1.b.toUpperCase(), "http://localhost" + var7, var10, var8, var1.d, var1.e
            );
         case "grpc":
            return new GrpcAction("grpc-" + var2, var3, a, var0.a, var0.b, var0.c, var4, var1.i, var1.b, var1.j, var1.e);
         default:
            return new EmitAction("emit-" + var2, var3, a, var0.a, var0.b, var0.c, var4, var1.f, var1.g, var1.h);
      }
   }

   private static Map<String, Object> a(io.karatelabs.ext.sim.SimulationBuilder.SimulatedScenario var0) {
      LinkedHashMap var1 = new LinkedHashMap();
      var1.put("name", var0.b);
      var1.put("line", var0.c);
      var1.put("slug", var0.d());
      ArrayList var2 = new ArrayList();

      for (String var4 : var0.e) {
         var2.add("@" + var4);
      }

      var1.put("tags", var2);
      var1.put("failed", "failed".equals(var0.f));
      var1.put("skipped", "skipped".equals(var0.f));
      ArrayList var7 = new ArrayList();
      if (!var0.g.isEmpty()) {
         ArrayList var8 = new ArrayList();

         for (io.karatelabs.ext.sim.SimulationBuilder.SimulatedAction var6 : var0.g) {
            var8.add(a(var6));
         }

         var7.add(Map.of("embeds", var8));
      }

      var1.put("stepResults", var7);
      return var1;
   }

   private static Map<String, Object> a(io.karatelabs.ext.sim.SimulationBuilder.SimulatedAction var0) {
      LinkedHashMap var1 = new LinkedHashMap();
      String var3 = var0.a;
      String var2;
      switch (var3) {
         case "http":
            var2 = "http-exchange";
            var1.put("request", Map.of("method", var0.b.toUpperCase(), "url", "http://localhost" + var0.c));
            var1.put("response", Map.of("status", var0.d, "responseTime", var0.e));
            break;
         case "grpc":
            var2 = "grpc-match";
            var1.put("service", var0.i);
            var1.put("method", var0.b);
            var1.put("status", var0.j);
            var1.put("durationMs", var0.e);
            break;
         default:
            var2 = "coverage-emit";
            var1.put("source", var0.f);
            var1.put("key", var0.g);
            var1.put("evidence", var0.h == null ? Map.of() : var0.h);
      }

      var3 = Base64.getEncoder().encodeToString(Json.stringifyStrict(var1).getBytes(StandardCharsets.UTF_8));
      return Map.of("name", var2, "parts", List.of(Map.of("role", "primary", "mime", "application/json", "data", var3)));
   }

   private record TopologyExpectation(String a, String b) {

   }

   private record SimulatedAction(String a, String b, String c, int d, long e, String f, String g, Map<String, Object> h, String i, String j) {


      static io.karatelabs.ext.sim.SimulationBuilder.SimulatedAction a(String var0, String var1, int var2, long var3) {
         return new io.karatelabs.ext.sim.SimulationBuilder.SimulatedAction("http", var0, var1, var2, var3, null, null, null, null, null);
      }

      static io.karatelabs.ext.sim.SimulationBuilder.SimulatedAction a(String var0, String var1, Map<String, Object> var2) {
         return new io.karatelabs.ext.sim.SimulationBuilder.SimulatedAction("emit", null, null, 0, 0L, var0, var1, var2, null, null);
      }

      static io.karatelabs.ext.sim.SimulationBuilder.SimulatedAction a(String var0, String var1, String var2, long var3) {
         return new io.karatelabs.ext.sim.SimulationBuilder.SimulatedAction("grpc", var1, null, 0, var3, null, null, null, var0, var2);
      }
   }

   public static final class RequirementBuilder {
      private final String a;
      private String b;
      private String c;
      private String d;
      private String e;
      private String f;
      private final List<String> g = new ArrayList<>();
      private final List<String> h = new ArrayList<>();

      private RequirementBuilder(String var1) {
         this.a = var1;
      }

      public io.karatelabs.ext.sim.SimulationBuilder.RequirementBuilder a(String var1) {
         this.b = var1;
         return this;
      }

      public io.karatelabs.ext.sim.SimulationBuilder.RequirementBuilder b(String var1) {
         this.e = var1;
         return this;
      }

      public io.karatelabs.ext.sim.SimulationBuilder.RequirementBuilder c(String var1) {
         this.c = var1;
         return this;
      }

      public io.karatelabs.ext.sim.SimulationBuilder.RequirementBuilder d(String var1) {
         this.d = var1;
         return this;
      }

      public io.karatelabs.ext.sim.SimulationBuilder.RequirementBuilder e(String var1) {
         this.f = var1;
         return this;
      }

      public io.karatelabs.ext.sim.SimulationBuilder.RequirementBuilder a(String... var1) {
         for (String var5 : var1) {
            this.g.add(var5);
         }

         return this;
      }

      public io.karatelabs.ext.sim.SimulationBuilder.RequirementBuilder b(String... var1) {
         for (String var5 : var1) {
            if (var5 != null && !var5.isBlank()) {
               this.h.add(var5.trim());
            }
         }

         return this;
      }
   }

   public static final class SimulationRowBuilder {
      private final io.karatelabs.ext.sim.SimulationBuilder.SimulationSpecBuilder a;
      private String b = "sim.feature";
      private String c;
      private String d;
      private String e = "passed";
      private boolean f;
      private String g;
      private String h;
      private String i;

      private SimulationRowBuilder(io.karatelabs.ext.sim.SimulationBuilder.SimulationSpecBuilder var1) {
         this.a = var1;
      }

      public io.karatelabs.ext.sim.SimulationBuilder.SimulationRowBuilder a(String var1) {
         this.b = var1;
         return this;
      }

      public io.karatelabs.ext.sim.SimulationBuilder.SimulationRowBuilder b(String var1) {
         this.c = var1;
         return this;
      }

      public io.karatelabs.ext.sim.SimulationBuilder.SimulationRowBuilder c(String var1) {
         this.d = var1;
         return this;
      }

      public io.karatelabs.ext.sim.SimulationBuilder.SimulationRowBuilder a() {
         return this.e("passed");
      }

      public io.karatelabs.ext.sim.SimulationBuilder.SimulationRowBuilder b() {
         return this.e("failed");
      }

      public io.karatelabs.ext.sim.SimulationBuilder.SimulationRowBuilder c() {
         return this.e("skipped");
      }

      public io.karatelabs.ext.sim.SimulationBuilder.SimulationRowBuilder d(String var1) {
         this.g = var1;
         return this;
      }

      private io.karatelabs.ext.sim.SimulationBuilder.SimulationRowBuilder e(String var1) {
         this.e = var1;
         this.f = true;
         return this;
      }

      public io.karatelabs.ext.sim.SimulationBuilder.SimulationSpecBuilder a(String var1, String var2) {
         this.h = var1;
         this.i = var2;
         this.a.a(this);
         return this.a;
      }
   }

   public static final class SimulatedScenario {
      private final String a;
      private final String b;
      private int c;
      private String d;
      private final Set<String> e = new LinkedHashSet<>();
      private String f = "passed";
      private final List<io.karatelabs.ext.sim.SimulationBuilder.SimulatedAction> g = new ArrayList<>();

      private SimulatedScenario(String var1, String var2) {
         this.a = var1;
         this.b = var2;
      }

      public io.karatelabs.ext.sim.SimulationBuilder.SimulatedScenario a(int var1) {
         this.c = var1;
         return this;
      }

      public io.karatelabs.ext.sim.SimulationBuilder.SimulatedScenario a(String var1) {
         this.d = var1;
         return this;
      }

      public io.karatelabs.ext.sim.SimulationBuilder.SimulatedScenario a(String... var1) {
         for (String var5 : var1) {
            this.e.add(var5.startsWith("@") ? var5.substring(1) : var5);
         }

         return this;
      }

      public io.karatelabs.ext.sim.SimulationBuilder.SimulatedScenario a() {
         this.f = "passed";
         return this;
      }

      public io.karatelabs.ext.sim.SimulationBuilder.SimulatedScenario b() {
         this.f = "failed";
         return this;
      }

      public io.karatelabs.ext.sim.SimulationBuilder.SimulatedScenario c() {
         this.f = "skipped";
         return this;
      }

      public io.karatelabs.ext.sim.SimulationBuilder.SimulatedScenario a(String var1, String var2, int var3) {
         return this.a(var1, var2, var3, 0L);
      }

      public io.karatelabs.ext.sim.SimulationBuilder.SimulatedScenario a(String var1, String var2, int var3, long var4) {
         this.g.add(io.karatelabs.ext.sim.SimulationBuilder.SimulatedAction.a(var1, var2, var3, var4));
         return this;
      }

      public io.karatelabs.ext.sim.SimulationBuilder.SimulatedScenario a(String var1, String var2) {
         return this.a(var1, var2, Map.of());
      }

      public io.karatelabs.ext.sim.SimulationBuilder.SimulatedScenario a(String var1, String var2, Map<String, Object> var3) {
         this.g.add(io.karatelabs.ext.sim.SimulationBuilder.SimulatedAction.a(var1, var2, var3));
         return this;
      }

      public io.karatelabs.ext.sim.SimulationBuilder.SimulatedScenario a(String var1, String var2, String var3) {
         return this.a(var1, var2, var3, 0L);
      }

      public io.karatelabs.ext.sim.SimulationBuilder.SimulatedScenario a(String var1, String var2, String var3, long var4) {
         this.g.add(io.karatelabs.ext.sim.SimulationBuilder.SimulatedAction.a(var1, var2, var3, var4));
         return this;
      }

      String d() {
         return this.d != null && !this.d.isBlank() ? this.d : this.a + ":" + this.b;
      }
   }

   public static final class SimulationSpecBuilder {
      private final io.karatelabs.ext.sim.SimulationBuilder a = new io.karatelabs.ext.sim.SimulationBuilder();
      private final List<io.karatelabs.ext.sim.SimulationBuilder.TopologyExpectation> b = new ArrayList<>();
      private boolean c;
      private int d;

      public io.karatelabs.ext.sim.SimulationBuilder.SimulationSpecBuilder a(String var1, String var2, String var3, String... var4) {
         this.a.a(var1).a(var2).d(var3).a(var4);
         this.c = true;
         return this;
      }

      public io.karatelabs.ext.sim.SimulationBuilder.SimulationRowBuilder a() {
         return new io.karatelabs.ext.sim.SimulationBuilder.SimulationRowBuilder(this);
      }

      public void a(Path var1, io.karatelabs.ext.coverage.CoverageSource... var2) {
         var var3 = this.b(var1, var2);
         if (!var3.isEmpty()) {
            ArrayList var4 = new ArrayList();

            for (Map var6 : (Iterable<Map>) (Iterable<?>) (var3)) {
               var4.add(var6.get("item") + ": expected " + var6.get("expected") + " but was " + var6.get("actual"));
            }

            throw new AssertionError("topology mismatch (" + var3.size() + "/" + this.b.size() + " rows):\n  " + String.join("\n  ", var4));
         }
      }

      public List<Map<String, Object>> b(Path var1, io.karatelabs.ext.coverage.CoverageSource... var2) {
         ArrayList var3 = new ArrayList();
         if (this.c) {
            try {
               Files.createDirectories(var1);
               Files.writeString(var1.resolve("store.md"), this.a.e());
            } catch (IOException var11) {
               throw new UncheckedIOException(var11);
            }

            var3.add(new RequirementsCoverageSource(var1));
         }

         var3.addAll(List.of(var2));
         io.karatelabs.ext.coverage.graph.TraceGraph var4 = io.karatelabs.ext.coverage.graph.TraceGraph.a(var3, this.a.c());
         ArrayList var5 = new ArrayList();

         for (io.karatelabs.ext.sim.SimulationBuilder.TopologyExpectation var7 : this.b) {
            io.karatelabs.ext.coverage.graph.CoverageStatus var8 = var4.n(var7.a());
            String var9 = var8 == null ? null : var8.name();
            if (!var7.b().equals(var9)) {
               LinkedHashMap var10 = new LinkedHashMap();
               var10.put("item", var7.a());
               var10.put("expected", var7.b());
               var10.put("actual", var9);
               var5.add(var10);
            }
         }

         return var5;
      }

      public int b() {
         return this.b.size();
      }

      private void a(io.karatelabs.ext.sim.SimulationBuilder.SimulationRowBuilder var1) {
         if (var1.c != null || var1.f || var1.g != null) {
            String var2 = var1.c != null ? var1.c : "row-" + ++this.d;
            io.karatelabs.ext.sim.SimulationBuilder.SimulatedScenario var3 = this.a.a(var1.b, var2);
            if (var1.d != null) {
               var3.a("@req=" + var1.d);
            }

            String var4 = var1.e;
            switch (var4) {
               case "failed":
                  var3.b();
                  break;
               case "skipped":
                  var3.c();
                  break;
               default:
                  var3.a();
            }

            if (var1.g != null) {
               String[] var6 = var1.g.trim().split("\\s+");
               if (var6.length >= 3) {
                  var3.a(var6[0], var6[1], Integer.parseInt(var6[2]));
               }
            }
         }

         if (var1.h != null) {
            this.b.add(new io.karatelabs.ext.sim.SimulationBuilder.TopologyExpectation(var1.h, var1.i));
         }
      }
   }
}
