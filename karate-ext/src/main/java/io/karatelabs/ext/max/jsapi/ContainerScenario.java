package io.karatelabs.ext.max.jsapi;

import io.karatelabs.common.Resource;
import io.karatelabs.core.SuiteResult;
import io.karatelabs.gherkin.Feature;
import io.karatelabs.js.SimpleObject;
import io.karatelabs.js.Terms;
import io.karatelabs.output.LogContext;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class ContainerScenario {
   private static final Logger b = LoggerFactory.getLogger(ContainerScenario.class);
   static final String a = "__container";

   private ContainerScenario() {
   }

   static SuiteResult a(Path var0, Path var1, String var2, Runnable var3) {
      return a(var0, var1, var2, List.of(new io.karatelabs.ext.max.jsapi.ContainerScenario.ScenarioSegment(var2, io.karatelabs.ext.coverage.collect.ExecutionFragment.ExecutionFragmentKind.CONTAINER, 0, null, var3)));
   }

   static SuiteResult a(Path var0, Path var1, String var2, List<io.karatelabs.ext.max.jsapi.ContainerScenario.ScenarioSegment> var3) {
      LogContext var4 = LogContext.peekCurrent();
      io.karatelabs.ext.common.EvidenceContext.EvidenceEventSink var5 = io.karatelabs.ext.common.EvidenceContext.a(null);
      LogContext.clear();

      Object var7;
      try {
         Feature var6 = Feature.read(Resource.text(a(var2, var3), var0));
         return io.karatelabs.core.Runner.features(var6)
            .configDir(var0.toString())
            .workingDir(var0)
            .global("__container", new ContainerScenario.Body(var3))
            .outputJsonLines(true)
            .outputHtmlReport(true)
            .outputConsoleSummary(false)
            .backupOutputDir(false)
            .outputDir(var1)
            .parallel(1);
      } catch (Exception var11) {
         b.warn("container scenario not synthesized for {}: {}", var1, var11.getMessage());
         var7 = null;
      } finally {
         if (var4 != null) {
            LogContext.set(var4);
         } else {
            LogContext.clear();
         }

         io.karatelabs.ext.common.EvidenceContext.a(var5);
      }

      return (SuiteResult)var7;
   }

   static String a(String var0, String var1) {
      String var2 = var0 == null ? "" : var0.replaceAll("\\s+", " ").trim();
      if (var2.isEmpty()) {
         return var1;
      } else {
         return var2.length() > 120 ? var2.substring(0, 117) + "..." : var2;
      }
   }

   private static String a(String var0, List<io.karatelabs.ext.max.jsapi.ContainerScenario.ScenarioSegment> var1) {
      StringBuilder var2 = new StringBuilder("Feature: ").append(a(var0)).append("\n");

      for (int var3 = 0; var3 < var1.size(); var3++) {
         io.karatelabs.ext.max.jsapi.ContainerScenario.ScenarioSegment var4 = (io.karatelabs.ext.max.jsapi.ContainerScenario.ScenarioSegment)var1.get(var3);
         var2.append("  @").append("fragment").append('=').append(var4.b().tag()).append(" @").append("exec").append('=').append(var4.c()).append("\n");
         var2.append("  Scenario: ").append(a(var4.a())).append("\n");
         if (var4.d() != null) {
            var2.append("    * def __id = '").append(var4.d().replace("\\", "\\\\").replace("'", "\\'")).append("'\n");
         }

         var2.append("    * eval ").append("__container").append(".run(").append(var3).append(")\n");
      }

      return var2.toString();
   }

   private static String a(String var0) {
      return var0.replace("\\", "\\\\");
   }

   private static final class Body implements SimpleObject {
      private final List<io.karatelabs.ext.max.jsapi.ContainerScenario.ScenarioSegment> segments;

      Body(List<io.karatelabs.ext.max.jsapi.ContainerScenario.ScenarioSegment> var1) {
         this.segments = var1;
      }

      @Override
      public Object jsGet(String var1) {
         return "run".equals(var1) ? (io.karatelabs.js.JavaInvokable)var1x -> {
            int var2 = var1x.length > 0 && var1x[0] instanceof Number var3 ? var3.intValue() : 0;
            this.segments.get(var2).e().run();
            return Terms.UNDEFINED;
         } : Terms.UNDEFINED;
      }

      @Override
      public Collection<String> jsKeys() {
         return List.of("run");
      }
   }

   record ScenarioSegment(String a, io.karatelabs.ext.coverage.collect.ExecutionFragment.ExecutionFragmentKind b, int c, String d, Runnable e) {
   }
}
