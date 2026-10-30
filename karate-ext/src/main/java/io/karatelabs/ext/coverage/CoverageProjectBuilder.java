package io.karatelabs.ext.coverage;

import io.karatelabs.common.Resource;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class CoverageProjectBuilder {
   private CoverageProjectBuilder() {
   }

   public static io.karatelabs.ext.coverage.CoverageProjectBuilder.CoverageProjectBuildResult build(io.karatelabs.ext.coverage.CoverageProjectBuilder.CoverageProjectOptions var0) {
      long var1 = var0.o != null ? var0.o : (var0.m == null ? 0L : io.karatelabs.ext.coverage.CoverageSnapshotPublisher.nextSnapshotSequence(var0.m));
      CoverageRunEvidence var3 = b(var0);
      io.karatelabs.ext.coverage.graph.TraceGraph var4 = io.karatelabs.ext.coverage.graph.TraceGraph.a(var0.a, var3, var0.b);
      if (!var0.p.isEmpty()) {
         var4.a(var0.p);
      }

      var4.a(var0.h, var0.i);
      List var5 = var3 instanceof io.karatelabs.ext.coverage.collect.JsonlRunEvidenceReader var6 ? var6.h() : null;
      int var15 = 0;
      int var7 = 0;
      ArrayList var8 = new ArrayList();
      Path var9 = a(var0.d);

      for (Path var11 : var0.e) {
         boolean var12 = var11.toAbsolutePath().normalize().equals(var9);
         boolean var13 = var0.f && !var12;
         io.karatelabs.ext.coverage.collect.RunEvidenceBundle var14 = var13
            ? io.karatelabs.ext.coverage.collect.RunEvidenceBundle.d(var11)
            : (var12 ? io.karatelabs.ext.coverage.collect.RunEvidenceBundle.a(var11, var5, true) : io.karatelabs.ext.coverage.collect.EvidenceCache.a(var11, var5));
         var14.a(var4);
         var15 += var14.c().size();
         var7 += var14.d().size();
         var8.addAll(var14.g());
      }

      Map<String, RulebookCoverageResult> var16 = Map.of();
      io.karatelabs.ext.coverage.collect.RequirementsCoverageSource var17 = a(var0.a);
      if (var0.g != null && var17 != null) {
         var16 = var0.g.a(var17);

         for (RulebookCoverageResult var20 : (Iterable<RulebookCoverageResult>) (Iterable<?>) (var16.values())) {
            if (var20.hasError()) {
               LinkedHashMap<java.lang.String, java.lang.String> var22 = new LinkedHashMap();
               var22.put("book", var20.c());
               var22.put("code", var20.errorCode());
               var4.a(Map.of("producerRefused", List.of(var22)));
            } else {
               var4.a(var20.d(), null);
            }
         }
      }

      var var19 = CoverageExt.buildDimensionedRoot(var4, var0.a, var0.j, var0.k, var0.l);
      boolean var21 = var3 instanceof io.karatelabs.ext.coverage.collect.JsonlRunEvidenceReader var23 && var23.j();
      return new io.karatelabs.ext.coverage.CoverageProjectBuilder.CoverageProjectBuildResult(var19, var4, var16, var15, var7, var8, var21, var0.m, var0.n, var1);
   }

   private static Path a(Path var0) {
      Path var1 = var0 == null ? null : var0.toAbsolutePath().normalize().getParent();
      return var1 == null ? null : var1.getParent();
   }

   private static CoverageRunEvidence b(io.karatelabs.ext.coverage.CoverageProjectBuilder.CoverageProjectOptions var0) {
      try {
         if (var0.d != null) {
            return io.karatelabs.ext.coverage.collect.JsonlRunEvidenceReader.b(var0.d);
         }

         if (!var0.c.isEmpty()) {
            return new io.karatelabs.ext.coverage.collect.JsonlRunEvidenceReader(var0.c);
         }
      } catch (IOException var2) {
         throw new UncheckedIOException(var2);
      }

      return new CoverageRunEvidence() {
         @Override
         public List<Action> a() {
            return List.of();
         }

         @Override
         public String c() {
            return "aggregate";
         }

         @Override
         public Map<String, Object> d() {
            return Map.of("files", List.of());
         }
      };
   }

   private static io.karatelabs.ext.coverage.collect.RequirementsCoverageSource a(List<CoverageSource> var0) {
      for (CoverageSource var2 : var0) {
         if (var2 instanceof io.karatelabs.ext.coverage.collect.RequirementsCoverageSource var3) {
            return var3;
         }
      }

      return null;
   }

   public static final class CoverageProjectOptions {
      List<CoverageSource> a = new ArrayList<>();
      Object b;
      List<Path> c = new ArrayList<>();
      Path d;
      List<Path> e = new ArrayList<>();
      boolean f;
      io.karatelabs.ext.coverage.RulebookCoverageResult.RulebookCoverageProducer g;
      io.karatelabs.ext.coverage.graph.ExecutionSelection.SelectionOptions h = io.karatelabs.ext.coverage.graph.ExecutionSelection.SelectionOptions.a();
      io.karatelabs.ext.coverage.collect.ExecutionInventory i = io.karatelabs.ext.coverage.collect.ExecutionInventory.c;
      List<Resource> j = List.of();
      Path k;
      Path l;
      Path m;
      boolean n;
      Long o;
      Map<String, Object> p = Map.of();

      public io.karatelabs.ext.coverage.CoverageProjectBuilder.CoverageProjectOptions a(List<CoverageSource> var1) {
         this.a = var1;
         return this;
      }

      public io.karatelabs.ext.coverage.CoverageProjectBuilder.CoverageProjectOptions requirements(Object var1) {
         this.b = var1;
         return this;
      }

      public io.karatelabs.ext.coverage.CoverageProjectBuilder.CoverageProjectOptions b(List<Path> var1) {
         this.c = var1;
         return this;
      }

      public io.karatelabs.ext.coverage.CoverageProjectBuilder.CoverageProjectOptions eventFile(Path var1) {
         this.d = var1;
         return this;
      }

      public io.karatelabs.ext.coverage.CoverageProjectBuilder.CoverageProjectOptions a(List<Path> var1, boolean var2) {
         this.e = var1;
         this.f = var2;
         return this;
      }

      public io.karatelabs.ext.coverage.CoverageProjectBuilder.CoverageProjectOptions a(io.karatelabs.ext.coverage.RulebookCoverageResult.RulebookCoverageProducer var1) {
         this.g = var1;
         return this;
      }

      public io.karatelabs.ext.coverage.CoverageProjectBuilder.CoverageProjectOptions snapshotSequence(long var1) {
         this.o = var1;
         return this;
      }

      public io.karatelabs.ext.coverage.CoverageProjectBuilder.CoverageProjectOptions a(io.karatelabs.ext.coverage.graph.ExecutionSelection.SelectionOptions var1) {
         this.h = var1;
         return this;
      }

      public io.karatelabs.ext.coverage.CoverageProjectBuilder.CoverageProjectOptions a(Map<String, Object> var1) {
         this.p = var1;
         return this;
      }

      public io.karatelabs.ext.coverage.CoverageProjectBuilder.CoverageProjectOptions executionInventory(io.karatelabs.ext.coverage.collect.ExecutionInventory var1) {
         this.i = var1 == null ? io.karatelabs.ext.coverage.collect.ExecutionInventory.c : var1;
         return this;
      }

      public io.karatelabs.ext.coverage.CoverageProjectBuilder.CoverageProjectOptions a(List<Resource> var1, Path var2, Path var3) {
         this.j = var1 == null ? List.of() : var1;
         this.k = var2;
         this.l = var3;
         return this;
      }

      public io.karatelabs.ext.coverage.CoverageProjectBuilder.CoverageProjectOptions reportOptions(Path var1, boolean var2) {
         this.m = var1;
         this.n = var2;
         return this;
      }

      public io.karatelabs.ext.coverage.CoverageProjectBuilder.CoverageProjectOptions reportDirectory(Path var1) {
         return this.reportOptions(var1, var1 != null && Files.exists(var1.resolve("karate-summary.html")));
      }
   }

   public record CoverageProjectBuildResult(Map<String, Object> b, io.karatelabs.ext.coverage.graph.TraceGraph c, Map<String, RulebookCoverageResult> d, int e, int f, List<Map<String, Object>> g, boolean h, Path i, boolean j, long k) {


      public boolean publish() throws IOException {
         return io.karatelabs.ext.coverage.CoverageSnapshotPublisher.a(this.i, this.b, this.h, this.j, this.k);
      }

      public Map<String, Object> b() {
         return this.b;
      }

      public io.karatelabs.ext.coverage.graph.TraceGraph c() {
         return this.c;
      }

      public Map<String, RulebookCoverageResult> d() {
         return this.d;
      }

      public int e() {
         return this.e;
      }

      public int f() {
         return this.f;
      }

      public List<Map<String, Object>> g() {
         return this.g;
      }

      public boolean h() {
         return this.h;
      }

      public Path i() {
         return this.i;
      }

      public boolean j() {
         return this.j;
      }

      public long k() {
         return this.k;
      }
   }
}
