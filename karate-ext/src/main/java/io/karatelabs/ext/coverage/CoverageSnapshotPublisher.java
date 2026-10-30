package io.karatelabs.ext.coverage;

import io.karatelabs.common.Json;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class CoverageSnapshotPublisher {
   private static final Logger d = LoggerFactory.getLogger(CoverageSnapshotPublisher.class);
   public static final String a = "ext/coverage/coverage-data.json";
   public static final String b = "ext/coverage/coverage-data.js";
   public static final String c = "ext/traceability/traceability-data.js";
   private static final String e = "../../../karate-summary.html";
   private static final Map<Path, Object> f = new ConcurrentHashMap<>();
   private static final Map<Path, AtomicLong> g = new ConcurrentHashMap<>();
   private static final Map<Path, Long> h = new ConcurrentHashMap<>();

   private CoverageSnapshotPublisher() {
   }

   public static long nextSnapshotSequence(Path var0) {
      return g.computeIfAbsent(b(var0), var0x -> new AtomicLong()).incrementAndGet();
   }

   public static void a(Path var0, Map<String, Object> var1, boolean var2) throws IOException {
      a(var0, var1, var2, Files.exists(var0.resolve("karate-summary.html")));
   }

   public static void a(Path var0, Map<String, Object> var1, boolean var2, boolean var3) throws IOException {
      a(var0, var1, var2, var3, nextSnapshotSequence(var0));
   }

   public static boolean a(Path var0, Map<String, Object> var1, boolean var2, boolean var3, long var4) throws IOException {
      var var6 = new LinkedHashMap<>(var1);
      var6.put("sequence", var4);
      var6.remove("provisional");
      if (var2) {
         var6.put("provisional", true);
      }

      if (var6.get("selection") instanceof Map var7) {
         LinkedHashMap<java.lang.String, java.lang.Boolean> var22 = new LinkedHashMap(var7);
         var22.remove("provisional");
         if (var2) {
            var22.put("provisional", true);
         }

         var6.put("selection", var22);
      }

      String var21 = Json.stringifyStrict(var6);
      Map var23 = (Map)Json.parseStrict(var21, true);
      String var9 = "window.KARATE_COVERAGE_DATA = " + Json.stringifyStrict(b(var0, var23, var3)) + ";\n";
      var var10 = io.karatelabs.ext.requirements.RequirementsReportBuilder.a((Map<String, Object>)Json.parseStrict(var21, true), var0);
      if (var2) {
         var10.put("provisional", true);
      }

      if (var3) {
         var10.put("karateSummary", "../../../karate-summary.html");
      }

      var10.put("coverageHref", "../../coverage/pages/coverage.html");
      String var11 = "window.KARATE_TRACE = " + Json.stringifyStrict(var10) + ";\n";
      Path var12 = b(var0);
      synchronized (f.computeIfAbsent(var12, var0x -> new Object())) {
         long var14 = h.getOrDefault(var12, 0L);
         if (var4 < var14) {
            d.info("snapshot {} into {} not published — {} is already there", new Object[]{var4, var12, var14});
            return false;
         }

         Path var16 = a(var12.resolve("ext/coverage/coverage-data.json"), var21);
         Path var17 = a(var12.resolve("ext/coverage/coverage-data.js"), var9);
         Path var18 = a(var12.resolve("ext/traceability/traceability-data.js"), var11);
         a(var16, var12.resolve("ext/coverage/coverage-data.json"));
         a(var17, var12.resolve("ext/coverage/coverage-data.js"));
         a(var18, var12.resolve("ext/traceability/traceability-data.js"));
         h.put(var12, var4);
      }

      d.debug("published {} snapshot {} into {}", new Object[]{var2 ? "provisional" : "final", var4, var12});
      return true;
   }

   private static Path b(Path var0) {
      return var0.toAbsolutePath().normalize();
   }

   private static Map<String, Object> b(Path var0, Map<String, Object> var1, boolean var2) {
      var var3 = new LinkedHashMap<>(var1);
      if (var2) {
         var3.put("karateSummary", "../../../karate-summary.html");
      }

      Object var4 = io.karatelabs.ext.coverage.dim.DimensionCoverageProjector.a(var1, null).get("items");
      if (var4 instanceof List var5 && !var5.isEmpty()) {
         var3.put("dimensions", var4);
      }

      if (io.karatelabs.ext.coverage.dim.CoveringArrayGenerator.a(var1, null, null).get("items") instanceof List var6) {
         ArrayList<java.lang.Object> var7 = new ArrayList();

         for (Object var9 : var6) {
            if (var9 instanceof Map var10 && var10.get("rows") instanceof List) {
               var7.add(var9);
            }
         }

         if (!var7.isEmpty()) {
            var3.put("covering", var7);
         }
      }

      var var12 = io.karatelabs.ext.requirements.RequirementsReportBuilder.a(var0);
      if (!var12.isEmpty()) {
         var3.put("links", var12);
      }

      var3.put("assertionStrength", io.karatelabs.ext.requirements.RequirementsReportBuilder.d(var1));
      var var13 = io.karatelabs.ext.requirements.RequirementsReportBuilder.a(var1);
      if (var13 != null) {
         var3.put("readiness", var13);
         var3.put("hasTraceability", true);
         var3.put("traceabilityHref", "../../traceability/pages/traceability.html");
      }

      return var3;
   }

   private static Path a(Path var0, String var1) throws IOException {
      Files.createDirectories(var0.getParent());
      Path var2 = io.karatelabs.ext.common.TempFileCreator.a(var0.getParent(), "." + var0.getFileName(), ".tmp");
      Files.writeString(var2, var1, StandardCharsets.UTF_8);
      return var2;
   }

   private static void a(Path var0, Path var1) throws IOException {
      try {
         Files.move(var0, var1, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
      } catch (AtomicMoveNotSupportedException var3) {
         Files.move(var0, var1, StandardCopyOption.REPLACE_EXISTING);
      }
   }
}
