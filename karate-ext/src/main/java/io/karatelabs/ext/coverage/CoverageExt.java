package io.karatelabs.ext.coverage;

import io.karatelabs.common.Resource;
import io.karatelabs.core.Ext;
import io.karatelabs.core.ReportAssets;
import io.karatelabs.core.RunEvent;
import io.karatelabs.core.RunEventType;
import io.karatelabs.core.RunListener;
import io.karatelabs.core.Suite;
import io.karatelabs.ext.common.ExtMeta;
import io.karatelabs.ext.common.MaxPaths;
import io.karatelabs.ext.contract.ContractLoader;
import io.karatelabs.ext.contract.Protocol;
import io.karatelabs.ext.coverage.dim.DimensionHandlers;
import io.karatelabs.ext.openapi.SpecBinding;
import io.karatelabs.ext.rules.RuleArmCoverageCatalog;
import io.karatelabs.ext.rules.Rule;
import io.karatelabs.ext.rules.RulebookCoverageSource;
import io.karatelabs.js.SimpleObject;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CoverageExt implements Ext, io.karatelabs.ext.lint.LintConfigProvider, SimpleObject {
   private static final Logger logger = LoggerFactory.getLogger(CoverageExt.class);
   static final String VERSION = ExtMeta.version("karate-max-meta.properties");
   private final RunListener enrichment = io.karatelabs.ext.coverage.HttpExchangeEvidenceRecorder.embeddingListener();
   private final RunListener assertions = io.karatelabs.ext.coverage.AssertionEvidenceRecorder.listener();
   private Suite suite;
   private boolean traceabilityRegistered;
   private Object openapi;
   private Object grpc;
   private Object kafka;
   private Object asyncapi;
   private String mcp;
   private String rules;
   private Object requirements;
   private String runs;
   private String dimensions;
   private Object criticalityPolicy;
   private Object real;
   private Object requireProofs;
   private Object map;
   private Object lintRules;

   @Override
   public Object jsGet(String var1) {
      return switch (var1) {
         case "openapi" -> this.openapi;
         case "grpc" -> this.grpc;
         case "kafka" -> this.kafka;
         case "asyncapi" -> this.asyncapi;
         case "mcp" -> this.mcp;
         case "rules" -> this.rules;
         case "requirements" -> this.requirements;
         case "runs" -> this.runs;
         case "dimensions" -> this.dimensions;
         case "criticalityPolicy" -> this.criticalityPolicy;
         case "real" -> this.real;
         case "requireProofs" -> this.requireProofs;
         case "map" -> this.map;
         case "lintRules" -> this.lintRules;
         default -> null;
      };
   }

   @Override
   public void putMember(String var1, Object var2) {
      switch (var1) {
         case "openapi":
            this.openapi = var2;
            break;
         case "grpc":
            this.grpc = var2;
            break;
         case "kafka":
            this.kafka = var2;
            break;
         case "asyncapi":
            this.asyncapi = var2;
            break;
         case "mcp":
            this.mcp = str(var2);
            break;
         case "rules":
            this.rules = str(var2);
            break;
         case "requirements":
            this.requirements = var2;
            break;
         case "runs":
            this.runs = str(var2);
            break;
         case "dimensions":
            this.dimensions = str(var2);
            break;
         case "criticalityPolicy":
            this.criticalityPolicy = var2;
            break;
         case "real":
            this.real = var2;
            break;
         case "requireProofs":
            this.requireProofs = var2;
            break;
         case "map":
            this.map = var2;
            break;
         case "lintRules":
            this.lintRules = var2;
            break;
         default:
            throw new IllegalArgumentException(
               "unknown coverage config key: cov."
                  + var1
                  + " (known: openapi, grpc, kafka, asyncapi, mcp, rules, requirements, runs, dimensions, criticalityPolicy, real, requireProofs, map, lintRules)"
            );
      }
   }

   @Override
   public Collection<String> jsKeys() {
      return List.of(
         "openapi",
         "grpc",
         "kafka",
         "asyncapi",
         "mcp",
         "rules",
         "requirements",
         "runs",
         "dimensions",
         "criticalityPolicy",
         "real",
         "requireProofs",
         "map",
         "lintRules"
      );
   }

   @Override
   public Object getOpenapi() {
      return this.openapi;
   }

   @Override
   public Object getLintRules() {
      return this.lintRules;
   }

   public Object getGrpc() {
      return this.grpc;
   }

   public Object getKafka() {
      return this.kafka;
   }

   public Object getAsyncapi() {
      return this.asyncapi;
   }

   public String getMcp() {
      return this.mcp;
   }

   public String getRules() {
      return this.rules;
   }

   public Object getRequirements() {
      return this.requirements;
   }

   public String getRuns() {
      return this.runs;
   }

   public Object getCriticalityPolicy() {
      return this.criticalityPolicy;
   }

   public Object getReal() {
      return this.real;
   }

   public Object getRequireProofs() {
      return this.requireProofs;
   }

   public Object getMap() {
      return this.map;
   }

   private static String str(Object var0) {
      return var0 == null ? null : String.valueOf(var0);
   }

   @Override
   public void onBoot(Suite var1) {
      io.karatelabs.ext.common.keycheck.Main.requireLicensed("coverage");
      this.suite = var1;
      var1.registerReportAssets(
         ReportAssets.named("coverage")
            .js("static/coverage-report.js")
            .css("static/coverage-report.css")
            .asset("static/coverage.css")
            .asset("static/help-affordance.js")
            .page("nav.pages", "Coverage", "pages/coverage.html")
            .cta("Karate Agent", "https://karatelabs.io/agent"),
         this.getClass().getClassLoader()
      );
      if (var1.getGlobal("spec") instanceof SpecBinding var2) {
         var2.addSource(() -> this.configuredSpecPaths(var1));
      } else {
         var1.registerGlobal("spec", new SpecBinding(() -> this.configuredSpecPaths(var1)));
      }

      logger.info("coverage ext booted (openapi={})", this.openapi);
   }

   private List<String> configuredSpecPaths(Suite var1) {
      ArrayList var2 = new ArrayList();

      for (OpenApiSpecDescriptor var4 : (Iterable<OpenApiSpecDescriptor>) (Iterable<?>) (OpenApiSpecDescriptor.parse(this.openapi))) {
         var2.add(MaxPaths.resolveRef(var1.getRoot(), var4.b()));
      }

      return var2;
   }

   @Override
   public boolean requiresJsonlEvents() {
      return true;
   }

   @Override
   public boolean onEvent(RunEvent var1) {
      if (var1.getType() == RunEventType.SUITE_ENTER && !this.traceabilityRegistered && configured(this.requirements)) {
         this.traceabilityRegistered = true;
         this.suite
            .registerReportAssets(
               ReportAssets.named("traceability")
                  .js("static/traceability-report.js")
                  .css("static/traceability-report.css")
                  .asset("static/coverage.css")
                  .asset("static/help-affordance.js")
                  .page("nav.pages", "Traceability", "pages/traceability.html"),
               this.getClass().getClassLoader()
            );
      }

      this.assertions.onEvent(var1);
      return this.enrichment.onEvent(var1);
   }

   @Override
   public void onShutdown() {
      boolean var1 = configured(this.openapi);
      boolean var2 = configured(this.grpc);
      boolean var3 = this.kafka instanceof List var4 ? !var4.isEmpty() : configured(this.kafka);
      boolean var22 = configured(this.asyncapi);
      boolean var23 = this.mcp != null && !this.mcp.isBlank();
      boolean var6 = this.rules != null && !this.rules.isBlank();
      boolean var7 = configured(this.requirements);
      if (!var1 && !var2 && !var3 && !var22 && !var23 && !var6 && !var7) {
         logger.warn(
            "coverage: nothing configured (set cov.openapi / cov.grpc / cov.kafka / cov.asyncapi / cov.mcp / cov.rules / cov.requirements in karate-boot.js) — skipping"
         );
      } else {
         try {
            Path var8 = this.suite.getRoot();
            Path var9 = this.suite.getClasspathRoot();
            Path var10 = Path.of(this.suite.getOutputDir());
            Path var11 = var10.resolve("karate-json").resolve("karate-events.jsonl");
            if (!Files.exists(var11)) {
               logger.warn("coverage: {} not found — was outputJsonLines(true) set? skipping", var11);
               return;
            }

            var var12 = this.buildConfiguredSources(var8, var9);
            requireDistinctNamespaces(var12);
            ArrayList var13 = new ArrayList();
            if (this.runs != null && !this.runs.isBlank()) {
               Path var14 = resolve(var8, var9, this.runs);
               if (Files.isDirectory(var14)) {
                  try (var var15 = Files.list(var14)) {
                     var15.filter(var0 -> Files.isDirectory(var0)).sorted().forEach(var13::add);
                  }
               } else {
                  logger.warn("coverage: cov.runs dir {} not found — skipping run evidence", var14);
               }
            }

            List var24 = var1 ? specPaths(this.openapi, var8, var9) : List.of();
            Path var25 = var6 ? resolve(var8, var9, this.rules) : null;
            io.karatelabs.ext.coverage.CoverageProjectBuilder.CoverageProjectBuildResult var16 = io.karatelabs.ext.coverage.CoverageProjectBuilder.build(
               new io.karatelabs.ext.coverage.CoverageProjectBuilder.CoverageProjectOptions()
                  .a(var12)
                  .requirements(this.map)
                  .eventFile(var11)
                  .a(var13, true)
                  .a(io.karatelabs.ext.coverage.graph.ExecutionSelection.SelectionOptions.a("shutdown"))
                  .executionInventory(io.karatelabs.ext.coverage.collect.ExecutionInventory.a(var8, var25))
                  .a(var24, this.configuredDimensionsPath(var8, var9), var25)
                  .reportOptions(var10, true)
            );
            if (!var13.isEmpty()) {
               logger.info("coverage: ingested {} run(s), {} attested hit(s)", var16.e(), var16.f());

               for (Map var18 : var16.g()) {
                  logger.warn(
                     "coverage: @req=/@cov= token attached NO coverage — run {} {} '{}': {}",
                     new Object[]{var18.get("run"), var18.get("field"), var18.get("text"), var18.get("detail")}
                  );
               }
            }

            var var26 = var16.b();
            var16.publish();
            logger.info("coverage: wrote {} ({} source(s))", var10.resolve("ext/coverage/coverage-data.json"), var12.size());
            this.contributeSummaryCards(var26);
         } catch (Exception var21) {
            logger.warn("coverage: analysis failed — {}", var21.getMessage(), var21);
         }
      }
   }

   public List<CoverageSource> buildConfiguredSources(Path var1, Path var2) throws IOException {
      boolean var3 = configured(this.openapi);
      boolean var4 = configured(this.grpc);
      boolean var5 = this.kafka instanceof List var6 ? !var6.isEmpty() : configured(this.kafka);
      boolean var16 = configured(this.asyncapi);
      boolean var17 = this.mcp != null && !this.mcp.isBlank();
      boolean var8 = this.rules != null && !this.rules.isBlank();
      boolean var9 = configured(this.requirements);
      ArrayList var10 = new ArrayList();
      if (var3) {
         io.karatelabs.ext.common.keycheck.Main.requireProduct("cov.openapi", "openapi");
         var10.addAll(openApiSources(this.openapi, var1, var2));
      }

      if (var4) {
         io.karatelabs.ext.common.keycheck.Main.requireProduct("cov.grpc", "grpc");
         var var11 = protoPaths(this.grpc, var1, var2);
         ContractLoader var12 = io.karatelabs.ext.coverage.collect.ProtocolProviderRegistry.a(Protocol.GRPC);
         if (var11.isEmpty()) {
            logger.warn("coverage: cov.grpc={} resolved to no .proto files — skipping gRPC coverage", this.grpc);
         } else if (var12 == null) {
            logger.warn(
               "coverage: cov.grpc is set but no gRPC protocol provider is on the classpath (the karate-grpc leaf is absent — e.g. the lean agent jar) — skipping gRPC coverage"
            );
         } else {
            var10.add(new io.karatelabs.ext.coverage.collect.ContractCoverageSource(var12.load(var11, ContractLoader.LoaderOptions.empty())));
         }
      }

      if (var5) {
         io.karatelabs.ext.common.keycheck.Main.requireProduct("cov.kafka", "kafka");
         ContractLoader var18 = io.karatelabs.ext.coverage.collect.ProtocolProviderRegistry.a(Protocol.KAFKA);
         if (var18 == null) {
            logger.warn(
               "coverage: cov.kafka is set but no Kafka protocol provider is on the classpath (the karate-kafka leaf is absent — e.g. the lean agent jar) — skipping Kafka coverage"
            );
         } else {
            var10.add(new io.karatelabs.ext.coverage.collect.ContractCoverageSource(var18.load(List.of(), new ContractLoader.LoaderOptions(null, null, Map.of("kafka", this.kafka)))));
         }
      }

      if (var16) {
         io.karatelabs.ext.common.keycheck.Main.requireProduct("cov.asyncapi", "openapi");
         var var19 = asyncApiPaths(this.asyncapi, var1, var2);
         if (var19.isEmpty()) {
            logger.warn("coverage: cov.asyncapi={} resolved to no docs — skipping AsyncAPI coverage", this.asyncapi);
         }

         boolean var20 = var19.size() > 1;

         for (Path var14 : (Iterable<Path>) (Iterable<?>) (var19)) {
            String var15 = var20 ? stem(var14) : null;
            var10.add(new io.karatelabs.ext.coverage.collect.ContractCoverageSource(io.karatelabs.ext.coverage.collect.AsyncApiContractLoader.a(var14), var15));
         }
      }

      if (var17) {
         io.karatelabs.ext.common.keycheck.Main.requireProduct("cov.mcp", "openapi");
         var10.add(new io.karatelabs.ext.coverage.collect.McpCoverageSource(resolve(var1, var2, this.mcp)));
      }

      if (var8) {
         var10.add(new RulebookCoverageSource(resolve(var1, var2, this.rules)));
      }

      if (var9) {
         var10.add(
            new io.karatelabs.ext.coverage.collect.RequirementsCoverageSource(requirementHomes(this.requirements, var1, var2), this.criticalityPolicy, this.real, var1, this.requireProofs)
         );
      }

      return var10;
   }

   private void contributeSummaryCards(Map<String, Object> var1) {
      Object var3 = var1.get("sources");
      if (var3 instanceof List) {
         for (Object var4 : (List)var3) {
            if (var4 instanceof Map var5) {
               String var6 = String.valueOf(var5.get("type"));
               long var7 = asLong(var5.get("total"));
               long var9 = asLong(var5.get("covered"));
               LinkedHashMap var11 = new LinkedHashMap();
               if ("req".equals(var6)) {
                  var11.put("label", "Requirements");
                  var11.put("value", var9 + "/" + var7);
                  var11.put("sub", "covered");
                  var11.put("href", "ext/traceability/pages/traceability.html");
               } else {
                  if (!"openapi".equals(var6) && !"grpc".equals(var6)) {
                     continue;
                  }

                  var11.put("label", "openapi".equals(var6) ? "API coverage" : "gRPC coverage");
                  var11.put("value", Math.round(asDouble(var5.get("percentage"))) + "%");
                  var11.put("sub", var9 + "/" + var7 + ("openapi".equals(var6) ? " endpoints" : " methods"));
                  var11.put("href", "ext/coverage/pages/coverage.html");
               }

               var11.put("status", var7 > 0L && var9 >= var7 ? "ok" : (var9 > 0L ? "warn" : "fail"));
               if (var5.get("combos") instanceof Map var12) {
                  long var20 = asLong(var12.get("required"));
                  long var15 = asLong(var12.get("covered"));
                  if (var20 > 0L) {
                     var11.put("sub", var11.get("sub") + " · " + Math.round(var15 * 100.0 / var20) + "% combos");
                     if (var15 < var20 && "ok".equals(var11.get("status"))) {
                        var11.put("status", "warn");
                     }
                  }
               }

               if (var5.get("inputs") instanceof Map var18) {
                  long var22 = asLong(var18.get("total"));
                  long var25 = asLong(var18.get("covered"));
                  if (var22 > 0L) {
                     var11.put("sub", var11.get("sub") + " · " + Math.round(var25 * 100.0 / var22) + "% inputs");
                     if (var25 < var22 && "ok".equals(var11.get("status"))) {
                        var11.put("status", "warn");
                     }
                  }
               }

               if (var5.get("errorPaths") instanceof Map var19) {
                  long var24 = asLong(var19.get("total"));
                  long var26 = asLong(var19.get("covered"));
                  if (var24 > 0L) {
                     var11.put("sub", var11.get("sub") + " · " + Math.round(var26 * 100.0 / var24) + "% error paths");
                     if (var26 < var24 && "ok".equals(var11.get("status"))) {
                        var11.put("status", "warn");
                     }
                  }
               }

               this.suite.addSummaryCard(var11);
            }
         }
      }
   }

   public Path configuredDimensionsPath(Path var1, Path var2) {
      return this.dimensions != null && !this.dimensions.isBlank() ? resolve(var1, var2, this.dimensions) : null;
   }

   public Path configuredRulesHome(Path var1, Path var2) {
      return this.rules != null && !this.rules.isBlank() ? resolve(var1, var2, this.rules) : null;
   }

   public static Map<String, Object> buildDimensionedRoot(io.karatelabs.ext.coverage.graph.TraceGraph var0, List<CoverageSource> var1, List<Resource> var2, Path var3, Path var4) {
      Map<String, Map<String, Object>> var5 = Map.of();
      if (var2 != null && !var2.isEmpty()) {
         io.karatelabs.ext.coverage.dim.OpenApiDimensionSource var6 = io.karatelabs.ext.coverage.dim.OpenApiDimensionSource.a(var2);
         if (!var6.a()) {
            var5 = var6.a(var0);
            var6.b(var0);
         }
      }

      var var19 = io.karatelabs.ext.coverage.dim.ContractDimensionExtractor.a(var1, var0);
      if (!var19.isEmpty()) {
         io.karatelabs.ext.coverage.dim.ContractDimensionExtractor.b(var1, var0);
      }

      Map<String, Map<String, Object>> var7 = Map.of();
      Map<String, List<String>> var8 = Map.of();
      Map<String, List<Map<String, Object>>> var9 = Map.of();
      Map<String, String> var10 = Map.of();
      Map<String, List<String>> var11 = Map.of();
      if (var3 != null && Files.isRegularFile(var3)) {
         try {
            DimensionHandlers var12 = new DimensionHandlers(Files.readString(var3)).a(declaredBasePaths(var1));
            io.karatelabs.ext.coverage.dim.DimensionReplay.a(var0, var12);
            var8 = io.karatelabs.ext.coverage.dim.DimensionUniverse.a(var0, var12);
            var10 = io.karatelabs.ext.coverage.dim.DimensionUniverse.c(var0, var12);
            var11 = io.karatelabs.ext.coverage.dim.DimensionUniverse.b(var0, var12);
            if (!var12.b().isEmpty()) {
               if (var4 != null) {
                  try (Rule var13 = Rule.atRulesHome(var4)) {
                     var7 = io.karatelabs.ext.coverage.dim.DimensionUniverse.b(var0, var12, var13);
                     var9 = io.karatelabs.ext.coverage.dim.DimensionUniverse.a(var0, var12, var13);
                  }
               } else {
                  logger.warn("coverage: cov.dimensions declares rulebook bindings but cov.rules is unset — closed-axis universes skipped");
               }
            }
         } catch (Exception var18) {
            logger.warn("coverage: cov.dimensions handlers {} failed to load — skipping: {}", var3, var18.toString());
         }
      }

      var7 = io.karatelabs.ext.coverage.dim.OpenApiDimensionSource.a(var5, var7);
      var7 = io.karatelabs.ext.coverage.dim.OpenApiDimensionSource.a(var19, var7);
      Map var22 = io.karatelabs.ext.coverage.TraceDocumentBuilder.a(var0, var1, var7, var8, var9, var10);
      stampIgnore(var22, var11);
      io.karatelabs.ext.coverage.dim.DimensionCoverageProjector.a(var22);
      io.karatelabs.ext.coverage.ErrorPathCoverageAnalyzer.a(var22);
      if (var4 != null) {
         var var23 = RuleArmCoverageCatalog.a(var4);
         if (!var23.isEmpty()) {
            var22.put("ruleCoverage", var23);
         }
      }

      return var22;
   }

   private static void stampIgnore(Map<String, Object> var0, Map<String, List<String>> var1) {
      if (!var1.isEmpty()) {
         Object var3 = var0.get("items");
         if (var3 instanceof List) {
            for (Object var4 : (List)var3) {
               if (var4 instanceof Map var5) {
                  List var6 = (List)var1.get(String.valueOf(var5.get("id")));
                  if (var6 != null && !var6.isEmpty()) {
                     var5.put("dimIgnore", var6);
                  }
               }
            }

            return;
         }
      }
   }

   private static List<String> declaredBasePaths(List<CoverageSource> var0) {
      LinkedHashSet var1 = new LinkedHashSet();

      for (CoverageSource var3 : var0) {
         if (var3 instanceof io.karatelabs.ext.coverage.collect.ContractCoverageSource var4 && var4.h().ext().get("basePaths") instanceof List var5) {
            var1.addAll(var5);
         }
      }

      return var1.isEmpty() ? io.karatelabs.ext.openapi.ServerBasePathResolver.a : List.copyOf(var1);
   }

   private static long asLong(Object var0) {
      return var0 instanceof Number var1 ? var1.longValue() : 0L;
   }

   private static double asDouble(Object var0) {
      return var0 instanceof Number var1 ? var1.doubleValue() : 0.0;
   }

   private static Path resolve(Path var0, Path var1, String var2) {
      return MaxPaths.resolvePath(var0, var2);
   }

   private static Resource specResource(Path var0, Path var1, String var2) {
      return MaxPaths.resolveResource(var0, var1, var2);
   }

   private static boolean configured(Object var0) {
      if (var0 instanceof Map var2) {
         return !var2.isEmpty();
      } else {
         return var0 instanceof List var1 ? !var1.isEmpty() : var0 != null && !String.valueOf(var0).isBlank();
      }
   }

   // $VF: One or more variable merging failures!
   // $VF: Could not properly define all variable types!
   // Please report this to the Vineflower issue tracker, at https://github.com/Vineflower/vineflower/issues with a copy of the class file (if you have the rights to distribute it!)
   public static List<Path> requirementHomes(Object var0, Path var1, Path var2) {
      ArrayList var3 = new ArrayList();

      for (Object var7 : var0 instanceof List ? (List)var0 : List.of(var0)) {
         String var6 = var7 == null ? null : String.valueOf(var7);
         if (var6 != null && !var6.isBlank()) {
            var3.add(resolve(var1, var2, var6));
         }
      }

      return var3;
   }

   public static List<io.karatelabs.ext.coverage.collect.OpenApiCoverageSource> openApiSources(Object var0, Path var1) {
      return openApiSources(var0, var1, var1);
   }

   public static List<io.karatelabs.ext.coverage.collect.OpenApiCoverageSource> openApiSources(Object var0, Path var1, Path var2) {
      ArrayList var3 = new ArrayList();

      for (OpenApiSpecDescriptor var5 : (Iterable<OpenApiSpecDescriptor>) (Iterable<?>) (OpenApiSpecDescriptor.parse(var0))) {
         var3.add(new io.karatelabs.ext.coverage.collect.OpenApiCoverageSource(specResource(var1, var2, var5.b()), var5.a(), var5.c()));
      }

      return var3;
   }

   public static List<Path> protoPaths(Object var0, Path var1, Path var2) throws IOException {
      ArrayList var3 = new ArrayList();
      if (var0 instanceof List var4) {
         var3.addAll(var4);
      } else if (var0 != null) {
         var3.add(var0);
      }

      ArrayList var14 = new ArrayList();

      for (Object var6 : var3) {
         Path var7 = resolve(var1, var2, String.valueOf(var6));
         if (Files.isDirectory(var7)) {
            ArrayList var8 = new ArrayList();

            try (DirectoryStream var9 = Files.newDirectoryStream(var7, "*.proto")) {
               for (Path var11 : (Iterable<Path>) (Iterable<?>) (var9)) {
                  var8.add(var11);
               }
            }

            var8.sort(null);
            var14.addAll(var8);
         } else {
            var14.add(var7);
         }
      }

      return var14;
   }

   static List<Path> asyncApiPaths(Object var0, Path var1, Path var2) throws IOException {
      ArrayList var3 = new ArrayList();
      if (var0 instanceof List var4) {
         var3.addAll(var4);
      } else if (var0 != null) {
         var3.add(var0);
      }

      ArrayList var14 = new ArrayList();

      for (Object var6 : var3) {
         Path var7 = resolve(var1, var2, String.valueOf(var6));
         if (Files.isDirectory(var7)) {
            ArrayList var8 = new ArrayList();

            try (DirectoryStream var9 = Files.newDirectoryStream(var7, "*.{yml,yaml,json}")) {
               for (Path var11 : (Iterable<Path>) (Iterable<?>) (var9)) {
                  var8.add(var11);
               }
            }

            var8.sort(null);
            var14.addAll(var8);
         } else {
            var14.add(var7);
         }
      }

      return var14;
   }

   private static String stem(Path var0) {
      String var1 = var0.getFileName().toString();
      int var2 = var1.lastIndexOf(46);
      return var2 > 0 ? var1.substring(0, var2) : var1;
   }

   static void requireDistinctNamespaces(List<CoverageSource> var0) {
      LinkedHashSet var1 = new LinkedHashSet();

      for (CoverageSource var3 : var0) {
         if (!var1.add(var3.f())) {
            throw new IllegalStateException(
               "coverage: two sources claim the same graph namespace '"
                  + var3.f()
                  + "' — disambiguate them (alias your cov.openapi via { alias: path }, give cov.asyncapi docs distinct filenames, or don't declare the same topics in both cov.kafka and a kafka cov.asyncapi doc)."
            );
         }
      }
   }

   public static List<Resource> specPaths(Object var0, Path var1, Path var2) {
      ArrayList var3 = new ArrayList();

      for (OpenApiSpecDescriptor var5 : (Iterable<OpenApiSpecDescriptor>) (Iterable<?>) (OpenApiSpecDescriptor.parse(var0))) {
         var3.add(specResource(var1, var2, var5.b()));
      }

      return var3;
   }

   public List<Resource> configuredSpecPaths(Path var1, Path var2) {
      return specPaths(this.openapi, var1, var2);
   }

   @Override
   public Map<String, Object> getManifest() {
      LinkedHashMap var1 = new LinkedHashMap();
      var1.put("version", VERSION);
      var1.put("assertions", true);
      if (this.openapi != null) {
         var1.put("openapi", this.openapi);
      }

      if (this.grpc != null) {
         var1.put("grpc", this.grpc);
      }

      if (this.kafka != null) {
         var1.put("kafka", this.kafka);
      }

      if (this.asyncapi != null) {
         var1.put("asyncapi", this.asyncapi);
      }

      if (this.mcp != null) {
         var1.put("mcp", this.mcp);
      }

      if (this.rules != null) {
         var1.put("rules", this.rules);
      }

      if (this.requirements != null) {
         var1.put("requirements", this.requirements);
      }

      if (this.runs != null) {
         var1.put("runs", this.runs);
      }

      return var1;
   }
}
