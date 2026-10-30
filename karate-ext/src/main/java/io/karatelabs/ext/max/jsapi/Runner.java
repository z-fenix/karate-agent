package io.karatelabs.ext.max.jsapi;

import io.karatelabs.common.Json;
import io.karatelabs.common.Resource;
import io.karatelabs.core.ConfigLoader;
import io.karatelabs.core.FeatureResult;
import io.karatelabs.core.Suite;
import io.karatelabs.core.SuiteResult;
import io.karatelabs.ext.agent.llm.SecretRedactor;
import io.karatelabs.ext.common.MaxPaths;
import io.karatelabs.ext.common.jsapi.JsApi;
import io.karatelabs.ext.common.jsapi.JsDispatch;
import io.karatelabs.ext.coverage.Action;
import io.karatelabs.ext.coverage.HttpExchangeEvidenceRecorder;
import io.karatelabs.ext.mutation.ResponseMutationRunner;
import io.karatelabs.ext.max.apitest.FeatureStore;
import io.karatelabs.ext.max.apitest.LiveSessions;
import io.karatelabs.ext.max.apitest.TriageGate;
import io.karatelabs.ext.max.run.MaxWorkspace;
import io.karatelabs.ext.max.run.RunEventSseBridge;
import io.karatelabs.ext.robot.RobotFactory;
import io.karatelabs.ext.robot.VideoRecorder;
import io.karatelabs.gherkin.ExamplesTable;
import io.karatelabs.gherkin.Feature;
import io.karatelabs.gherkin.FeatureSection;
import io.karatelabs.gherkin.Tag;
import io.karatelabs.js.Engine;
import io.karatelabs.output.ResultListener;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.function.Consumer;
import java.util.function.IntSupplier;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Runner extends JsDispatch {
   private static final Logger logger = LoggerFactory.getLogger(Runner.class);
   private final Path workingDir;
   private final RunMetadataStore runs;
   private final Harness harness;
   private final io.karatelabs.ext.common.server.EventPublisher publisher;
   private final ActiveRuns activeRuns;
   private final Engine engine;
   private Path defaultOutputDir;
   private Consumer<String> afterRun;
   private Harness slot;
   private volatile Path projectGraph;
   private final io.karatelabs.ext.max.jsapi.ProjectEnvironmentResolver appTarget;
   private volatile ConsoleState console = new ConsoleState(null);
   private volatile IntSupplier servePort = () -> -1;
   private volatile UnaryOperator<String> sanitizer = new SecretRedactor(Map::of);
   private LiveSessions sessions;
   private TriageGate triage;
   private static final String OVERLAY = "__overlay";
   private static final Pattern ENV_NAME = Pattern.compile("[A-Za-z0-9._-]{1,64}");
   private final Deque<int[]> exitScopes = new ArrayDeque<>();
   private static final long COLLECTION_SNIFF_MAX = 4194304L;
   static final String JS = "js";
   static final String FEATURE = "feature";
   static final String POSTMAN = "postman";
   private static final String CLASSPATH = "classpath:";
   private Supplier<Boolean> draftGuard;

   public Runner(Path var1) {
      this(var1, null, new ActiveRuns());
   }

   public Runner(Path var1, io.karatelabs.ext.common.server.EventPublisher var2) {
      this(var1, var2, new ActiveRuns());
   }

   public Runner(Path var1, io.karatelabs.ext.common.server.EventPublisher var2, ActiveRuns var3) {
      this(var1, var2, var3, null);
   }

   public Runner(Path var1, io.karatelabs.ext.common.server.EventPublisher var2, ActiveRuns var3, Engine var4) {
      this.workingDir = var1;
      this.runs = new RunMetadataStore(var1, () -> this.sanitizer);
      this.harness = new Harness(var1, var2);
      this.publisher = var2;
      this.activeRuns = var3;
      this.engine = var4;
      this.appTarget = new io.karatelabs.ext.max.jsapi.ProjectEnvironmentResolver(var1, () -> this.servePort.getAsInt(), () -> this.console);
      this.fn("help", var0 -> JsApi.helpTopic(help(), var0));
      this.fn("run", var1x -> this.run(JsApi.arg(var1x, 0), JsApi.arg(var1x, 1)));
      this.fn("suite", var1x -> this.suite(JsApi.arg(var1x, 0), JsApi.arg(var1x, 1)));
      this.fn("mutate", var1x -> this.mutate(JsApi.arg(var1x, 0), JsApi.arg(var1x, 1)));
      this.fn("stop", var1x -> this.stop(JsApi.str(var1x, 0)));
      this.fn("harness", var1x -> this.harness.explore(JsApi.arg(var1x, 0), JsApi.arg(var1x, 1)));
      this.fn("config", var1x -> this.config(JsApi.str(var1x, 0)));
      this.fn("launch", var1x -> this.launch(JsApi.str(var1x, 0)));
      this.fn("exit", var1x -> this.exit(JsApi.arg(var1x, 0)));
      this.fn("target", var1x -> this.target(JsApi.str(var1x, 0)));
      this.fn("envs", var1x -> this.envs());
      this.fn("env", var1x -> var1x.length > 0 && var1x[0] == null ? this.resetEnv() : this.env(JsApi.str(var1x, 0)));
      this.fn("auth", var1x -> this.auth(JsApi.str(var1x, 0)));
      this.fn("tests", var1x -> this.tests(JsApi.arg(var1x, 0)));
   }

   public Runner withOutputDir(Path var1) {
      this.defaultOutputDir = var1;
      return this;
   }

   public Runner withAfterRun(Consumer<String> var1) {
      this.afterRun = var1;
      this.harness.onCheckGraph(var1);
      return this;
   }

   public Runner withConsole(ConsoleState var1) {
      this.console = var1;
      this.harness.withConsole(var1);
      this.appTarget.a();
      return this;
   }

   public Runner withSanitizer(UnaryOperator<String> var1) {
      this.sanitizer = var1;
      this.harness.withSanitizer(var1);
      return this;
   }

   public Runner withSessions(LiveSessions var1) {
      this.sessions = var1;
      return this;
   }

   public Runner withTriage(TriageGate var1) {
      this.triage = var1;
      return this;
   }

   public Map<String, Object> runCandidate(String var1, byte[] var2, Map<String, Object> var3) {
      return this.runCandidate(var1, var2, null, null, var3);
   }

   public Map<String, Object> runCandidate(String var1, byte[] var2, Path var3, byte[] var4, Map<String, Object> var5) {
      LinkedHashMap var6 = new LinkedHashMap(var5 == null ? Map.of() : var5);
      var6.remove("session");
      var6.put("__overlay", new io.karatelabs.ext.max.jsapi.Runner.FileOverlay(Path.of(this.resolvePath(stripLine(var1))), var2, var3, var4));
      return this.decorateSuiteResult(this.suite(var1, var6));
   }

   private static String runIdOf(Map<String, Object> var0) {
      return var0.get("runId") instanceof String var1 && var1.matches("[A-Za-z0-9._-]{1,64}") ? var1 : UUID.randomUUID().toString().substring(0, 8);
   }

   private Map<String, Object> pageRunTriage(LiveSessions.Capture var1, SuiteResult var2, String var3) {
      if (var1 != null && this.triage != null && var2 != null) {
         try {
            return this.triage.pageRun(var2, var3);
         } catch (RuntimeException var5) {
            logger.warn("the triage hook failed for run {}: {}", var3, var5.getMessage());
            return null;
         }
      } else {
         return null;
      }
   }

   private String sessionRequest(List<String> var1, String var2) {
      if (this.sessions == null) {
         return "{session} needs the console (serve): no live sessions here";
      } else if (!var2.matches("[A-Za-z0-9._-]{1,64}")) {
         return "{session} is a short id ([A-Za-z0-9._-]), got " + var2;
      } else if (var1.size() != 1 || ((String)var1.get(0)).startsWith("classpath:") || !stripLine((String)var1.get(0)).endsWith(".feature")) {
         return "{session} runs one flow: Runner.run('<path>.feature:<line>', {session: '" + var2 + "'})";
      } else {
         return !Files.isRegularFile(Path.of(this.resolvePath(stripLine((String)var1.get(0)))))
            ? "no feature '" + stripLine((String)var1.get(0)) + "' — File.list() shows what is there"
            : null;
      }
   }

   private static String stripLine(String var0) {
      int var1 = var0.indexOf(".feature:");
      return var1 < 0 ? var0 : var0.substring(0, var1 + ".feature".length());
   }

   private LiveSessions.Capture capture(io.karatelabs.ext.max.jsapi.Runner.RunSelectionPlan var1, Map<String, Object> var2, ConsoleState.Kickoff var3) {
      if (var2.get("session") != null && this.sessions != null) {
         Path var4 = Path.of(this.resolvePath(stripLine(var1.b().get(0))));

         try {
            return this.sessions.capture(String.valueOf(var2.get("session")), this.memberPath(var4), FeatureStore.read(var4), var3);
         } catch (IOException var6) {
            return null;
         }
      } else {
         return null;
      }
   }

   Harness harness() {
      return this.harness;
   }

   RunMetadataStore runStore() {
      return this.runs;
   }

   public Runner withServePort(IntSupplier var1) {
      this.servePort = var1;
      return this;
   }

   public Runner withProjectGraph(Path var1) {
      this.projectGraph = var1;
      return this;
   }

   public Runner withSlotLease(Harness var1) {
      this.slot = var1;
      return this;
   }

   public static Runner at(Path var0) {
      return new Runner(var0);
   }

   public static Runner at(Path var0, io.karatelabs.ext.common.server.EventPublisher var1) {
      return new Runner(var0, var1);
   }

   public static Runner at(Path var0, io.karatelabs.ext.common.server.EventPublisher var1, ActiveRuns var2) {
      return new Runner(var0, var1, var2);
   }

   public static Runner at(Path var0, io.karatelabs.ext.common.server.EventPublisher var1, ActiveRuns var2, Engine var3) {
      return new Runner(var0, var1, var2, var3);
   }

   Map<String, Object> config(String var1) {
      if (this.engine == null) {
         return JsApi.missing("Runner.config() needs the live serve engine — unavailable in this context");
      } else {
         String var2 = var1 != null && !var1.isBlank() ? var1.trim() : null;
         if (var2 != null && !ENV_NAME.matcher(var2).matches()) {
            return badEnv("Runner.config");
         } else {
            this.appTarget.a();

            try {
               var var3 = ProjectConfig.apply(this.engine, this.workingDir, var2, this.console);
               LinkedHashMap var4 = new LinkedHashMap();
               var4.put("loaded", new ArrayList(var3.keySet()));
               var4.put("count", var3.size());
               if (var2 == null) {
                  return var4;
               } else {
                  var4.put("env", var2);
                  return JsApi.next(
                     var4,
                     "Runner.suite(path, {env:'" + var2 + "'})",
                     "this reloaded the live engine only; a run takes its env per call, and an ambient karate.env wins"
                  );
               }
            } catch (Exception var5) {
               return this.sanitized(JsApi.error(new RuntimeException("Runner.config load failed: " + var5.getMessage(), var5)));
            }
         }
      }
   }

   public Map<String, Object> target(String var1) {
      String var2 = var1 != null && !var1.isBlank() ? var1.trim() : null;
      return var2 != null && !ENV_NAME.matcher(var2).matches() ? badEnv("Runner.target") : this.sanitized(this.appTarget.d(this.console.env(var2)));
   }

   public Map<String, Object> app(String var1) {
      String var2 = var1 != null && !var1.isBlank() ? var1.trim() : null;
      return var2 != null && !ENV_NAME.matcher(var2).matches() ? badEnv("Runner.target") : this.sanitized(this.appTarget.a(this.console.env(var2)));
   }

   public List<Map<String, Object>> envs() {
      Path var1 = this.appTarget.b();
      String var2 = this.console.env();
      ArrayList var3 = new ArrayList();
      if (io.karatelabs.ext.max.jsapi.ProjectEnvironmentScanner.a() == null) {
         LinkedHashMap var4 = new LinkedHashMap();
         var4.put("name", null);
         var4.put("current", var2 == null);
         var3.add(var4);
      }

      for (String var5 : io.karatelabs.ext.max.jsapi.ProjectEnvironmentScanner.a(var1.equals(this.workingDir) ? List.of(this.workingDir) : List.of(this.workingDir, var1), var2)) {
         LinkedHashMap var6 = new LinkedHashMap();
         var6.put("name", var5);
         var6.put("current", var5.equals(var2));
         var3.add(var6);
      }

      return var3;
   }

   public Map<String, Object> env(String var1) {
      return this.env(var1, true);
   }

   public Map<String, Object> resetEnv() {
      return this.reset(true);
   }

   public Map<String, Object> switchEnv(String var1) {
      return var1 == null ? this.reset(false) : this.env(var1, false);
   }

   private Map<String, Object> env(String var1, boolean var2) {
      String var3 = var1 != null && !var1.isBlank() ? var1.trim() : null;
      LinkedHashMap var4 = new LinkedHashMap();
      if (var3 == null) {
         var4.put("env", this.console.env());
         return var4;
      } else if (!ENV_NAME.matcher(var3).matches()) {
         return badEnv("Runner.env");
      } else {
         String var5 = io.karatelabs.ext.max.jsapi.ProjectEnvironmentScanner.a();
         if (var5 != null && !var5.equals(var3)) {
            return JsApi.missing(
               "Runner.env: the env is pinned to '"
                  + var5
                  + "' by -Dkarate.env or KARATE_ENV on the server; a run can still name its own, {env:'"
                  + var3
                  + "'}"
            );
         } else {
            ArrayList var6 = new ArrayList();

            for (Map var8 : this.envs()) {
               if (var8.get("name") != null) {
                  var6.add((String)var8.get("name"));
               }
            }

            if (!var6.contains(var3)) {
               var var9 = JsApi.notFound(
                  "no env '" + var3 + "' in karate-config: known " + var6 + "; declare one with ApiTest.config({name, value, env: '" + var3 + "', ifMatch})"
               );
               var9.put("envs", var6);
               return var9;
            } else {
               var4.put("env", var3);
               var4.put("previous", this.console.env());
               return this.reloaded(var4, this.console.switchEnv(var3), var2);
            }
         }
      }
   }

   private Map<String, Object> reset(boolean var1) {
      LinkedHashMap var2 = new LinkedHashMap();
      String var3 = this.console.env();
      ConsoleState.Snapshot var4 = this.console.switchEnv(null);
      var2.put("env", var4.env());
      var2.put("previous", var3);
      return this.reloaded(var2, var4, var1);
   }

   private Map<String, Object> reloaded(Map<String, Object> var1, ConsoleState.Snapshot var2, boolean var3) {
      if (var3 && this.engine != null) {
         String var4 = var2.env();

         try {
            ProjectConfig.reload(this.engine, this.workingDir, var2, this.console);
         } catch (Exception var6) {
            var1.put(
               "configError", this.sanitizer.apply("karate-config.js failed under '" + (var4 == null ? "the default env" : var4) + "': " + var6.getMessage())
            );
         }
      }

      return var1;
   }

   public Map<String, Object> auth(String var1) {
      String var2 = var1 != null && !var1.isBlank() ? var1.trim() : null;
      return var2 != null && !ENV_NAME.matcher(var2).matches() ? badEnv("Runner.auth") : this.sanitized(this.appTarget.b(this.console.env(var2)));
   }

   Map<String, Object> configValues() {
      return this.appTarget.c(this.console.env(null));
   }

   private Map<String, Object> sanitized(Map<String, Object> var1) {
      var1.replaceAll((var1x, var2) -> (Object)(var2 instanceof String var3 ? this.sanitizer.apply(var3) : var2));
      return var1;
   }

   private static Map<String, Object> badEnv(String var0) {
      return JsApi.missing(var0 + ": env must be a plain name — letters, digits, '.', '_' or '-', at most 64 — e.g. " + var0 + "('qa')");
   }

   private static Map<String, Object> envRefusal(Object var0) {
      Object var1 = var0 instanceof Map var2 ? var2.get("env") : null;
      return var1 != null && !String.valueOf(var1).isBlank() && !ENV_NAME.matcher(String.valueOf(var1).trim()).matches()
         ? JsApi.missing("opts.env must be a plain name — letters, digits, '.', '_' or '-', at most 64 — e.g. {env:'qa'}")
         : null;
   }

   public List<Map<String, Object>> tests(Object var1) {
      Map var2 = var1 instanceof Map var3 ? var3 : Map.of();
      String var13 = var2.get("req") instanceof String var4 && !var4.isBlank() ? var4.trim() : null;
      String var14 = var2.get("kind") instanceof String var15 && !var15.isBlank() ? var15.trim() : null;
      Path var16 = this.workingDir.toAbsolutePath().normalize();
      if ("postman".equals(var14)) {
         return var13 == null ? this.collectionRows(var16) : List.of();
      } else {
         ArrayList var17 = new ArrayList();

         for (Map var8 : Check.headers(MaxWorkspace.checksDir(this.workingDir), null)) {
            String var9 = String.valueOf(var8.get("name"));
            if (isTest(var9)) {
               LinkedHashMap var10 = new LinkedHashMap();
               var10.put("path", Check.canonicalName(var9));
               var10.put("kind", "js");
               declared(var10, "title", var8.get("title"));
               declared(var10, "description", var8.get("description"));
               var10.put("req", var8.get("req"));
               if (Boolean.TRUE.equals(var8.get("ai"))) {
                  var10.put("ai", true);
                  if (var8.get("aiBy") != null) {
                     var10.put("aiBy", var8.get("aiBy"));
                  }
               }

               var17.add(var10);
            }
         }

         for (String var20 : (Iterable<String>) (Iterable<?>) (io.karatelabs.ext.coverage.collect.ExecutionInventory.b(var16))) {
            if (!skippedWhole(var16.resolve(var20))) {
               var17.add(featureRow(var16, var20));
            }
         }

         Path var19 = this.projectGraph;
         var var21 = io.karatelabs.ext.max.jsapi.TestHistoryIndex.a(var19 == null ? null : io.karatelabs.ext.common.jsapi.CoverageGraphAccessor.a(var19).c());
         ArrayList var22 = new ArrayList();

         for (Map var11 : (Iterable<Map>) (Iterable<?>) (var17)) {
            if ((var14 == null || var14.equals(var11.get("kind"))) && (var13 == null || declares(var11, var13))) {
               io.karatelabs.ext.max.jsapi.TestHistoryIndex.TestHistorySummary var12 = (io.karatelabs.ext.max.jsapi.TestHistoryIndex.TestHistorySummary)var21.get(io.karatelabs.ext.max.jsapi.RunEvidenceInspector.a(String.valueOf(var11.get("path"))));
               if (var12 != null) {
                  var11.putAll(var12.a());
               } else {
                  var11.put("runs", 0);
                  var11.put("passed", 0);
               }

               var22.add(var11);
            }
         }

         var22.sort(
            Comparator.<Map>comparingLong(var0 -> var0.get("lastRunAt") instanceof Number var1x ? var1x.longValue() : Long.MIN_VALUE)
               .reversed()
               .thenComparing(var0 -> String.valueOf(var0.get("path")))
         );
         return var22;
      }
   }

   public List<String> testPaths() {
      ArrayList var1 = new ArrayList();

      for (String var3 : Check.names(MaxWorkspace.checksDir(this.workingDir))) {
         if (isTest(var3)) {
            var1.add(Check.canonicalName(var3));
         }
      }

      Path var5 = this.workingDir.toAbsolutePath().normalize();

      for (String var4 : (Iterable<String>) (Iterable<?>) (io.karatelabs.ext.coverage.collect.ExecutionInventory.b(var5))) {
         if (!skippedWhole(var5.resolve(var4))) {
            var1.add(var4);
         }
      }

      return var1;
   }

   static boolean skippedWhole(Path var0) {
      try {
         try (var var1 = Files.lines(var0)) {
            for (String var3 : (Iterable<String>) (var1.map(var0x -> var0x.replace("\ufeff", "").strip())::iterator)) {
               if (var3.startsWith("Feature:")) {
                  return false;
               }

               if (var3.startsWith("@")) {
                  for (String var7 : var3.split("\\s+")) {
                     if (var7.equals("@ignore") || var7.startsWith("@ignore=")) {
                        return true;
                     }
                  }
               }
            }
         }

         return false;
      } catch (UncheckedIOException | IOException var11) {
         return false;
      }
   }

   private static boolean isTest(String var0) {
      return var0.endsWith(".js") && !var0.endsWith(".karate.js");
   }

   static String anyDeveloperTest(Path var0) {
      Path var1 = MaxWorkspace.checksDir(var0);
      if (Files.isDirectory(var1)) {
         try (var var2 = Files.walk(var1)) {
            Path var3 = var2.filter(var0x -> Files.isRegularFile(var0x) && isTest(var0x.getFileName().toString()) && !"written".equals(Check.aiBy(var0x)))
               .findFirst()
               .orElse(null);
            if (var3 != null) {
               return Check.canonicalName(var1.relativize(var3).toString().replace('\\', '/'));
            }

            return io.karatelabs.ext.coverage.collect.ExecutionInventory.c(var0.toAbsolutePath().normalize());
         } catch (UncheckedIOException | IOException var7) {
            logger.warn("could not list the checks under {} ({})", var1, var7.toString());
         }
      }

      return io.karatelabs.ext.coverage.collect.ExecutionInventory.c(var0.toAbsolutePath().normalize());
   }

   private static Map<String, Object> featureRow(Path var0, String var1) {
      LinkedHashMap var2 = new LinkedHashMap();
      var2.put("path", var1);
      var2.put("kind", "feature");
      ArrayList var3 = new ArrayList();

      try {
         Feature var4 = Feature.read(Resource.from(var0.resolve(var1), var0));
         declared(var2, "title", var4.getName());
         declared(var2, "description", firstParagraph(var4.getDescription()));
         reqTags(var3, var4.getTags());

         for (FeatureSection var6 : var4.getSections()) {
            if (!var6.isOutline()) {
               reqTags(var3, var6.getScenario().getTags());
            } else {
               reqTags(var3, var6.getScenarioOutline().getTags());

               for (ExamplesTable var8 : var6.getScenarioOutline().getExamplesTables()) {
                  reqTags(var3, var8.getTags());
               }
            }
         }

         var2.put("req", var3);
         var2.put("scenarios", var4.getSections().size());
      } catch (RuntimeException var9) {
         logger.debug("'{}' does not parse: {}", var1, var9.getMessage());
         var2.put("req", var3);
      }

      return var2;
   }

   private static void reqTags(List<String> var0, List<Tag> var1) {
      for (Tag var3 : (Iterable<Tag>) (Iterable<?>) (var1 == null ? List.of() : var1)) {
         String var4 = var3.getText();
         if (var4 != null && var4.startsWith("req=")) {
            for (String var8 : var4.substring("req=".length()).split(",")) {
               String var9 = var8.trim();
               if (!var9.isEmpty() && !var0.contains(var9)) {
                  var0.add(var9);
               }
            }
         }
      }
   }

   static boolean declares(Map<String, Object> var0, String var1) {
      if (!(var0.get("req") instanceof List var2)) {
         return false;
      } else {
         for (Object var4 : var2) {
            String var5 = String.valueOf(var4);
            if (var5.equals(var1) || var5.startsWith(var1 + "/")) {
               return true;
            }
         }

         return false;
      }
   }

   private static void declared(Map<String, Object> var0, String var1, Object var2) {
      if (var2 instanceof String var3 && !var3.isBlank()) {
         var0.put(var1, var3.trim());
      }
   }

   private static String firstParagraph(String var0) {
      if (var0 == null) {
         return null;
      } else {
         StringBuilder var1 = new StringBuilder();

         for (String var5 : var0.split("\n")) {
            String var6 = var5.trim();
            if (!var6.startsWith("#")) {
               if (var6.isEmpty()) {
                  if (!var1.isEmpty()) {
                     break;
                  }
               } else {
                  var1.append(var1.isEmpty() ? "" : " ").append(var6);
               }
            }
         }

         return var1.isEmpty() ? null : var1.toString();
      }
   }

   private Map<String, Object> launch(String var1) {
      if (var1 == null || var1.isBlank()) {
         var var6 = JsApi.missing(
            "Runner.launch(path) requires the orchestrator script, e.g. Runner.launch('suite.karate.js') — the *.karate.js file that composes the suite"
         );
         var6.put("exitCode", 1);
         return var6;
      } else if (this.engine == null) {
         return JsApi.missing("Runner.launch() needs the live serve engine — unavailable in this context");
      } else {
         Path var2 = this.workingDir.resolve(var1).normalize();
         if (!Files.isRegularFile(var2)) {
            var var7 = JsApi.notFound("Runner.launch: no such script '" + var1 + "' under " + this.workingDir);
            var7.put("exitCode", 1);
            return var7;
         } else {
            Runner.Launched var3 = this.launchScope(() -> this.engine.eval(Files.readString(var2)));
            if (var3.error() != null) {
               Exception var8 = var3.error();
               var var9 = JsApi.error(new RuntimeException("Runner.launch('" + var1 + "'): " + var8.getMessage(), var8));
               var9.put("exitCode", var3.exitCode());
               return var9;
            } else {
               Object var4 = var3.value();
               LinkedHashMap var5 = new LinkedHashMap();
               var5.put("script", var1);
               var5.put("ok", var3.exitCode() == 0);
               var5.put("exitCode", var3.exitCode());
               if (var4 instanceof Map || var4 instanceof List || var4 instanceof String || var4 instanceof Number || var4 instanceof Boolean) {
                  var5.put("value", var4);
               }

               return var5;
            }
         }
      }
   }

   public Runner.Launched launchScope(Runner.LaunchBody var1) {
      int[] var2 = new int[]{0};
      synchronized (this.exitScopes) {
         this.exitScopes.addLast(var2);
      }

      Runner.Launched var4;
      try {
         Object var21 = var1.run();
         return new Runner.Launched(var21, var2[0], null);
      } catch (Exception var19) {
         record(var2, 1);
         var4 = new Runner.Launched(null, var2[0], var19);
      } finally {
         synchronized (this.exitScopes) {
            this.exitScopes.removeLastOccurrence(var2);
         }
      }

      return var4;
   }

   private static void record(int[] var0, int var1) {
      synchronized (var0) {
         var0[0] = Math.max(var0[0], var1);
      }
   }

   private Map<String, Object> exit(Object var1) {
      int[] var2;
      synchronized (this.exitScopes) {
         var2 = this.exitScopes.peekLast();
      }

      if (var2 == null) {
         return JsApi.missing(
            "Runner.exit(code) sets the exit code of a `karate launch` or Runner.launch script — no launch is running here, so nothing was recorded"
         );
      } else {
         double var7 = var1 instanceof Number var5 ? var5.doubleValue() : Double.NaN;
         if (var7 >= 0.0 && var7 <= 125.0 && var7 == Math.rint(var7)) {
            record(var2, (int)var7);
            LinkedHashMap var8 = new LinkedHashMap();
            var8.put("exitCode", var2[0]);
            return var8;
         } else {
            record(var2, 2);
            return JsApi.missing("Runner.exit(code) takes an integer 0–125, e.g. Runner.exit(r.gate.exitCode); got " + var1 + ", so 2 was recorded");
         }
      }
   }

   private static Map<String, Object> help() {
      return JsApi.markdown(
         "# Runner — kick off a karate suite (the composition + kickoff surface)\n\nOne JSAPI call starts work and returns a JS-native result the console, the run-a-test UI,\nand a `*.karate.js` file all consume identically (no per-case CLI subcommand, P13).\n\n- `Runner.run(path, [data|opts])` — **the unified front door**: the file extension picks the\n  engine. A `.js` runs as a check — wrapped as a synthetic one-scenario feature through core,\n  so it gets a native report (2nd arg = its `data`); a `.feature`, a folder, a **list\n  of paths** (`['a.feature','checks/login.js']` — the multi-select case, `.feature` and `.js`\n  freely mixed), or `'.'` (\"Run ALL\") runs as a **set**: one suite run, each `.js` member\n  wrapped as a member feature, a red member never stopping the rest (2nd arg = `opts`).\n  Both register one\n  `runs/<id>/` and feed the coverage graph identically, returning one shape:\n  `{runId, kind:'check'|'suite', ok, passed, failed, total, reportUrl?}`. This is the one\n  Run button the console / run-a-test UI sit on.\n- `Runner.tests([{req?, kind?}])` → `[{path, kind:'js'|'feature', title?, description?, req, ai?, aiBy?,\n  scenarios?, runs, passed, lastStatus?, lastRunAt?, lastRunId?, typicalMs?}]` — every replayable test\n  of this project, newest verdict first: the `checks/*.js` macros and the `.feature` suites (not an\n  `@ignore` feature: a mock or a called helper), each with\n  what it declares and its run history. A row's `path` goes straight to `Runner.run`. `{req:'ORD-001'}`\n  keeps the tests declaring it (a bare id matches its criteria), `{kind:'feature'}` one engine.\n- `Runner.envs()` → `[{name, current}]` — the envs karate-config declares: a `karate-config-<env>.js`, or a\n  `karate.env == '<env>'` branch in karate-config.js; `name: null` is the default (no env).\n- `Runner.env([name])` → `{env}`, the console's env. With a name it switches: every later run and\n  config read uses it until the server restarts; a run's own `opts.env` still wins. `Runner.env(null)`\n  goes back to the default. Tell the user before switching.\n- `Runner.auth([env])` → `{env, scheme, identity?, label}` — what karate-config's `auth` resolves to\n  (default: the console's env); never a credential.\n- `Runner.suite(path, [{tags?, threads?, env?, props?, output?, video?, formal?, req?, dryRun?, async?}])` — run a suite (a feature\n  file, a folder, a **list of paths** — `.feature` files and `.js` checks may be mixed, each\n  check running as one member — or `'.'` for everything runnable), JSONL on\n  (`-f karate:jsonl`, so the coverage graph is computed) +\n  the HTML report. **Pick by how LONG the suite runs, not by what you want back — both forms\n  return the same thing.** Blocking (the default) returns `{runId, passed, failed, total,\n  reportUrl, verdicts}` inline, where `verdicts` is the per-row read-back\n  `[{name, passed, error?}]` (a labeled `match`/`verify` failure is the row's `error`) and\n  `memberVerdicts` the same outcomes per selected member `[{path, kind, passed, error?}]`; it is\n  right for anything quick. **`async:true` for anything SLOW — a browser/UI suite, or minutes\n  of work.** A blocking call is one HTTP request held open for the whole run: no progress until\n  it ends, and if your client times out or you interrupt it you lose the handle to a suite that\n  is still running. `async:true` mints `runs/<id>`, runs on a virtual thread, returns\n  `{runId, status:'started'}` immediately and streams progress over SSE; you then poll\n  `Harness.get(runId)` (same `verdicts` + `reportUrl` when settled) as often as you like, and\n  the runId survives a dropped client. The run\n  registers `runs/<id>/` (the evidence store the `/runs` UI reads), JSONL in core's standard\n  `karate-json/` layout, so `Report.aggregate()` auto-discovers it.\n    - `path` — relative to the serve project root, or absolute / `classpath:`; also a list\n      of such paths (multi-select), or `'.'` for everything under the project (Run ALL). A\n      folder resolves recursively to its `.feature` files plus the `.js` checks under\n      `checks/`; the run's meta records the selector and the ordered members it resolved to.\n    - `tags` — a tag expression string or a list (e.g. `'@smoke'`, `['@smoke','~@slow']`).\n    - `threads` — parallel thread count (default 1; forced to 1 when a `.js` check is a member).\n    - `env` — the karate env (`karate.env`).\n    - `session` — an id: the run of one `'<path>.feature:<line>'` stays warm for `ApiTest.send`\n      (the result's `session` says so); `ApiTest.help('session')`.\n    - `props` — system properties for THIS suite only, as an object:\n      `{ props: { 'loan.url': 'http://host.docker.internal:9100' } }`. The JS-surface way to\n      point a suite at a different host/port/mode without touching its files or the JVM —\n      they overlay the process properties for this run and nothing else, so two runs in one\n      serve process never leak into each other. Read by `karate.sysprop(name[, default])` /\n      `karate.properties[name]` in `karate-config.js` and in the suite's features, **and** by\n      `boot.sysprop(...)` in `karate-boot.js`, so boot-time ext config (a host, a port, a\n      coverage source) is steerable too — this is how a `*.karate.js` launch script composes\n      a suite that the kit's own defaults don't describe. Two things it does NOT reach:\n      `sysenv` (env vars are read off the process, so an env-gated kit still needs a real env\n      var — have the kit read `sysprop(X, sysenv(X))` if a run should be able to override it),\n      and a `Check.run(...)` spawned from inside a scenario, which re-resolves the project\n      config on its own engine and so sees the kit's defaults, not this run's props. Suite\n      lane only — for a `.js` check the 2nd arg is its `data`.\n      To read a parameter INTO the script that is composing the run (the other direction —\n      how CI hands a launcher a target), use `Settings.sysenv(name[, default])` /\n      `Settings.sysprop(name[, default])`.\n    - `output` — where the report is written (CLI-aligned, karate/docs/CLI.md `-o`/`-f`). A dir\n      string (`'target/karate-reports'`) or a `{dir?, junitXml?}` block (the `karate-pom.json`\n      shape). Setting it makes the suite write ONE fixed report dir with JUnit ON (the CI Tests-tab\n      shape) — a drop-in for `karate run` — instead of the per-run `runs/<id>/` console store; in\n      `karate launch` this defaults to `target/karate-reports`. JSONL is always on (coverage/RTM).\n    - `video` — **opt-in** run video (default OFF, CI-safe): `true` records the whole run's display\n      to `<reportDir>/video.mp4`, published with the report. Needs an X display (the `karate launch`\n      container Xvfb, or a desktop) — fails soft to no-video otherwise. CI may also set `KARATE_VIDEO=true`.\n    - `formal` — mark this an RTM-counting run (vs dev noise) on its meta.\n    - `req` — stamp a requirement anchor on the run meta.\n    - `dryRun` — **parse-only: coverage WITHOUT executing anything**. Every scenario is\n      discovered and its tags read; no step, hook or `karate-config.js` runs, so nothing is\n      called and no SUT (or mock) is needed. You still get the JSONL, the report and the\n      coverage graph — but built from the **declared** edges only (`@req=` / `@cov=`), with\n      zero observed hits. Use it to answer *\"does a test exist that claims this item?\"* on\n      every PR, cheaply. Read the result as an INVENTORY, not a verdict:\n      **NOTRUN** = \"a test declares this, nobody ran it\" · **NOTCOVERED** = \"nothing in the\n      suite claims this at all\" (the authoring gap). A dry run can never produce a COVERED —\n      core stamps its steps `skipped`, not `passed` (the §2f gate: parse-only is not\n      evidence), and the result carries `skipped` + `dryRun:true` because `passed` counts\n      those skips. Two caveats: pair it with `tags` and the filtered-out scenarios' declared\n      edges never land (a COMPLETE declared RTM needs NO tag filter), and `@setup` scenarios\n      are exempt from the skip — they really execute. Contract coverage this way needs the\n      scenarios to carry `@cov=openapi:<operationId>`; without that tag an operation is\n      matched from observed traffic, which a dry run has none of. Same thing as the CLI's\n      `karate run -D`.\n\n- `Runner.stop(runId)` — **cooperatively stop an in-flight async suite**: the executing\n  scenario finishes, every scenario after it is skipped, and the run settles through its\n  normal path with status `stopped` (report + evidence for what DID run are kept; the\n  counts stay honest). Returns `{runId, status:'stopping'}` — poll `Harness.get(runId)`\n  for the settle. Suite lane only (the agentic loop stops via `Harness.stop()`). This is\n  the `/runs/<id>` page's Stop button.\n- `Runner.mutate(path, [{spec?, out?, timeoutMs?, strictResponseParsing?}])` — **response-tamper mutation testing:\n  would this suite's assertions notice the live API's responses being perturbed in flight?**\n  Needs nothing but the suite and the base URL it already targets. Baseline run first (must be\n  green), then the operator deck is derived from the observed responses — field drop/null,\n  type flip, numeric off-by-one, array-to-empty, status swap, malformed body — and the suite\n  re-runs once per mutant with that one seeded change injected in-client, applied to every\n  matching response of its operation — a mutant models an operation-level behavior change,\n  exactly as a rulebook mutant answers every request (no proxy, no mock, no re-pointed URL). Returns two rates over one denominator — `independentScore` (the\n  headline: kills by frozen authored assertions, or by a relation between two responses —\n  `Rule.help()`) and `rawKillRate` — plus the `worklist`\n  (every checked mutant not independently killed: the response changes nothing would notice)\n  and a report page. `spec` (an OpenAPI path) upgrades operation identity to `operationId` and\n  adds declared-status tampers; without it identity is method + path, and every face says so\n  (`disclosures`). `strictResponseParsing: true` declares that the suite runs with core's\n  `configure strictResponseParsing = true` — that unlocks the content-type tamper operator\n  (under karate's default lenient parse, no assertion could notice a declared-type change,\n  so it is only derived for strict suites). **What the number means:** it grades ASSERTION STRENGTH against perturbed\n  live responses — never the SUT's robustness, and never \"your tests would have found the bug\".\n  Runs are quarantined instrument runs under `out` (default `target/tamper/<stem>`), outside\n  `runs/` — they never touch coverage/RTM evidence. Idempotent traffic only (each mutant\n  re-executes the suite against the live target). The rulebook-seeded complement is\n  `Rule.mutate` (Tier-1 logic mutation — grades whether tests would catch business-logic\n  defects; this verb grades response-surface vigilance).\n- `Runner.harness(task, [opts])` — kick off the agentic loop (sugar over `Harness.explore`; see `Harness.help()`).\n- `Runner.launch(path)` — run a **`*.karate.js` orchestrator** (`Runner.launch('suite.karate.js')`):\n  the project's own one-command entry point, the JSAPI twin of `karate launch`. An orchestrator\n  composes `Runner.*`/`Harness.*` itself — stand up the app, run the matrix, one report — so it is\n  evaluated, not discovered as a suite, and `Runner.run`/`Runner.suite` refuse a `.karate.js` path.\n  **Reach for it before assembling a suite by hand:** when a project ships one, it is the definition\n  of \"run everything\", and re-deriving its feature list is how a run drifts from what CI does. It\n  evals in this live session, so vars it sets stay readable. Returns `{script, ok, exitCode}`; an\n  orchestrator reports failure by throwing (that message comes back verbatim as the error) or by\n  `Runner.exit`.\n- `Runner.exit(code)` — set the exit code (0–125) of the running `karate launch` or\n  `Runner.launch` script; the highest call wins and the script runs on. CI gate:\n  `Runner.exit(Openapi.diff('api/v1.yaml', 'api/v2.yaml', {failOn: 'breaking'}).gate.exitCode)`.\n  A child launch's code reaches its parent only as `Runner.exit(child.exitCode)`.\n\nThen `Report.generate()` / `Report.url()` render the coverage scorecard over the same run."
      );
   }

   Map<String, Object> run(Object var1, Object var2) {
      if (var1 instanceof List) {
         return this.decorateSuiteResult(this.suite(var1, var2));
      } else if (!(var1 instanceof String var3 && !var3.isBlank())) {
         return JsApi.missing(
            "Runner.run(path[, opts]) requires a path String or a list of paths, e.g. Runner.run('flow.feature'), Runner.run('checks/login.js', {user:'admin'}), or Runner.run(['a.feature','b.feature']) for a multi-feature suite"
         );
      } else if (var3.endsWith(".karate.js")) {
         return JsApi.missing(
            "'"
               + var3
               + "' is a *.karate.js orchestrator — it composes Runner.*/Harness.* itself, so run it with Runner.launch('"
               + var3
               + "') from here, or `karate launch "
               + var3
               + "` from a shell; not Runner.run"
         );
      } else if (var3.endsWith(".js")) {
         String var4 = var3.startsWith("checks/") ? var3.substring("checks/".length()) : var3;
         return this.runCheckAsFeature(var4, var2);
      } else {
         return var3.endsWith(".json") && this.collection(var3) != null ? this.postman(var3, var2) : this.decorateSuiteResult(this.suite(var3, var2));
      }
   }

   Map<String, Object> mutate(Object var1, Object var2) {
      List var3;
      if (var1 instanceof List var5 && !var5.isEmpty()) {
         var3 = var5.stream().map(String::valueOf).toList();
      } else {
         if (!(var1 instanceof String var4) || var4.isBlank()) {
            return JsApi.missing(
               "Runner.mutate(path[, {spec?, out?}]) requires a feature path (or a list of them) — the suite whose assertions get graded, e.g. Runner.mutate('checks/quote-api.feature', {spec: 'openapi.yaml'})"
            );
         }

         var3 = List.of(var4);
      }

      Map var14 = var2 instanceof Map var6 ? var6 : Map.of();
      String var15 = var14.get("spec") instanceof String var7 && !var7.isBlank() ? var7 : null;
      String var16 = ((String)var3.get(0)).replaceAll(".*/", "").replaceAll("\\.feature$", "").replaceAll("[^A-Za-z0-9._-]", "-");
      Path var17 = var14.get("out") instanceof String var9 && !var9.isBlank()
         ? this.workingDir.resolve(var9)
         : this.workingDir.resolve("target/tamper/" + var16);
      if (canonical(var17).startsWith(canonical(this.workingDir.resolve("runs")))) {
         return JsApi.missing(
            "Runner.mutate refuses out inside runs/ — mutation legs are quarantined instrument runs (seeded failures must never enter the governed evidence tree); use a path under target/ (default: target/tamper/<stem>)"
         );
      } else {
         long var18 = var14.get("timeoutMs") instanceof Number var11 ? var11.longValue() : 120000L;
         boolean var19 = Boolean.TRUE.equals(var14.get("strictResponseParsing"));

         try {
            var var20 = io.karatelabs.ext.mutation.ResponseMutationRunner.run(new io.karatelabs.ext.mutation.ResponseMutationRunner.ResponseMutationOptions(this.workingDir, var3, var15, var17, Map.of(), Set.of(), var18, Map.of(), var19, this.console.kickoff(null)));
            if (var20.get("error") == null) {
               var20.put(
                  "next",
                  "each worklist entry is a response change no assertion would independently notice — author a frozen literal/shape assertion for it, then re-run Runner.mutate to watch the independent score climb"
               );
            }

            return var20;
         } catch (Exception var13) {
            return JsApi.error(new RuntimeException("Runner.mutate failed: " + var13.getMessage(), var13));
         }
      }
   }

   private List<Map<String, Object>> collectionRows(Path var1) {
      LinkedHashMap var2 = new LinkedHashMap();

      for (Map var4 : this.runs.b()) {
         if ("postman".equals(var4.get("kind"))
            && var4.get("collection") instanceof String var5
            && !var2.containsKey(var5)
            && ("completed".equals(var4.get("status")) || "failed".equals(var4.get("status")))) {
            var2.put(var5, var4);
         }
      }

      ArrayList var18 = new ArrayList();
      var var19 = Set.of("target", "runs", "node_modules", ".git");

      try (var var20 = Files.walk(var1)) {
         for (Path var8 : (Iterable<Path>) (Iterable<?>) (var20.filter(var2x -> {
            Path var3 = var1.relativize(var2x);

            for (int var4x = 0; var4x < var3.getNameCount() - 1; var4x++) {
               if (var19.contains(var3.getName(var4x).toString())) {
                  return false;
               }
            }

            return var2x.getFileName().toString().endsWith(".json") && Files.isRegularFile(var2x);
         }).sorted().toList())) {
            String var9 = var1.relativize(var8).toString().replace(java.io.File.separatorChar, '/');
            io.karatelabs.ext.postman.editor.PostmanCollectionDocument var10 = Files.size(var8) > 4194304L && !var9.endsWith(".postman_collection.json") ? null : this.collection(var9);
            if (var10 != null) {
               LinkedHashMap var11 = new LinkedHashMap();
               var11.put("path", var9);
               var11.put("kind", "postman");
               declared(var11, "title", (var10.h() instanceof Map var13 ? var13.get("info") : null) instanceof Map var22 ? var22.get("name") : null);
               var11.put("req", List.of());
               Map var23 = (Map)var2.get(var9);
               if (var23 != null) {
                  var11.put("lastRunId", var23.get("runId"));
                  var11.put("lastStatus", "completed".equals(var23.get("status")) ? "passed" : "failed");
                  var11.put("lastRunAt", var23.get("finishedMs") != null ? var23.get("finishedMs") : var23.get("startedMs"));
               }

               var18.add(var11);
            }
         }
      } catch (UncheckedIOException | IOException var17) {
         logger.warn("could not list the collections under {} ({})", var1, var17.toString());
      }

      return var18;
   }

   private io.karatelabs.ext.postman.editor.PostmanCollectionDocument collection(String var1) {
      try {
         Path var2 = MaxPaths.resolveSandboxed(this.workingDir, var1);
         if (!Files.isRegularFile(var2)) {
            return null;
         } else {
            io.karatelabs.ext.postman.editor.PostmanCollectionDocument var3 = io.karatelabs.ext.postman.editor.PostmanCollectionDocument.a(var2);
            return var3.b(var2.getFileName().toString()) == null ? var3 : null;
         }
      } catch (Exception var4) {
         return null;
      }
   }

   public Map<String, Object> postman(String var1, Object var2) {
      if (var1 == null || var1.isBlank()) {
         return JsApi.missing("Postman.run(path, opts?) needs a project-relative collection path");
      } else if (var2 != null && !(var2 instanceof Map)) {
         return JsApi.missing("Postman.run(path, {item?, data?, env?, async?}): opts is an object");
      } else {
         Map var3 = var2 == null ? Map.of() : (Map)var2;

         Path var4;
         try {
            var4 = MaxPaths.resolveSandboxed(this.workingDir, var1);
         } catch (SecurityException var28) {
            return JsApi.missing("Postman.run denied: " + var28.getMessage());
         }

         if (!Files.isRegularFile(var4)) {
            return JsApi.notFound("no collection '" + var1 + "' — File.list() shows what is there");
         } else {
            io.karatelabs.ext.postman.editor.PostmanCollectionDocument var5;
            try {
               var5 = io.karatelabs.ext.postman.editor.PostmanCollectionDocument.a(var4);
            } catch (IOException var27) {
               var5 = null;
            }

            if (var5 != null && var5.b(var4.getFileName().toString()) == null) {
               String var6 = var3.get("item") == null ? "" : String.valueOf(var3.get("item"));
               io.karatelabs.ext.postman.PostmanCollectionNode var7 = io.karatelabs.ext.postman.PostmanCollectionTreeMapper.a(var5);
               io.karatelabs.ext.postman.PostmanCollectionNode var8 = io.karatelabs.ext.postman.PostmanCollectionTreeMapper.a(var7, var6);
               if (var8 == null) {
                  return JsApi.next(JsApi.notFound("no item " + var6 + " in " + var1), "Postman.read('" + var1 + "')");
               } else {
                  String var9 = var3.get("env") instanceof String var10 && !var10.isBlank() ? var10.trim() : null;
                  if (var9 != null && !ENV_NAME.matcher(var9).matches()) {
                     return badEnv("Postman.run");
                  } else {
                     List var29 = null;
                     String var30;
                     String var35 = var30 = var3.get("data") instanceof String var12 && !var12.isBlank() ? var12 : null;
                     if (var3.get("data") != null && var30 == null) {
                        return envelope("Postman.run: data is a project-relative .csv or .json file", "BAD_DATA", 400);
                     } else {
                        if (var30 != null) {
                           try {
                              var29 = io.karatelabs.ext.postman.PostmanDataFileParser.a(MaxPaths.resolveSandboxed(this.workingDir, var30), var30);
                           } catch (SecurityException | io.karatelabs.ext.postman.PostmanDataFileParser.PostmanDataFileException var26) {
                              return envelope("Postman.run: " + var26.getMessage(), "BAD_DATA", 400);
                           }
                        }

                        ConsoleState.Kickoff var31 = this.console.at(var9);

                        Map var32;
                        try {
                           var32 = ConfigLoader.configOnly(this.workingDir, var31.env(), null, var31.overlay());
                        } catch (Exception var25) {
                           logger.warn("postman run: karate-config.js did not resolve ({}), the environment scope is empty", var25.getMessage());
                           var32 = Map.of();
                        }

                        String var14 = UUID.randomUUID().toString().substring(0, 8);
                        io.karatelabs.ext.postman.PostmanCollectionRunner var15 = new io.karatelabs.ext.postman.PostmanCollectionRunner(
                           var7, var8, var4.getParent(), var32, var29, var1x -> this.sanitizer.apply(var1x), this.runs.a(var14).resolve("postman.jsonl")
                        );
                        if (var15.a().isEmpty()) {
                           return JsApi.missing("Postman.run: item '" + var6 + "' holds no request");
                        } else {
                           String var16 = this.workingDir.relativize(var4).toString().replace(java.io.File.separatorChar, '/');
                           long var17 = System.currentTimeMillis();
                           var var19 = postmanMeta(var14, var16, var6, var30, var5.c(), var17);
                           var19.put("status", "running");
                           var19.put("total", var15.a().size());
                           var19.put("passes", var15.b());
                           var19.remove("finishedMs");
                           this.runs.a(var14, io.karatelabs.ext.max.jsapi.EvidenceProtocolMetadata.a(var19));
                           this.activeRuns.start(var14);
                           if (Boolean.TRUE.equals(var3.get("async"))) {
                              String var33 = var5.c();
                              Thread.ofVirtual().name("postman-" + var14).start(() -> this.postmanBody(var15, var14, var16, var6, var30, var33, var31, var17));
                              LinkedHashMap var34 = new LinkedHashMap();
                              var34.put("runId", var14);
                              return var34;
                           } else {
                              var var20 = this.postmanBody(var15, var14, var16, var6, var30, var5.c(), var31, var17);
                              if (!Boolean.TRUE.equals(var20.get("ok"))) {
                                 int[] var21;
                                 synchronized (this.exitScopes) {
                                    var21 = this.exitScopes.peekLast();
                                 }

                                 if (var21 != null) {
                                    record(var21, 1);
                                 }
                              }

                              return var20;
                           }
                        }
                     }
                  }
               }
            } else {
               return envelope("'" + var1 + "' is not a Postman collection (a JSON object with info and item)", "NOT_COLLECTION", 422);
            }
         }
      }
   }

   private Map<String, Object> postmanBody(
      io.karatelabs.ext.postman.PostmanCollectionRunner var1, String var2, String var3, String var4, String var5, String var6, ConsoleState.Kickoff var7, long var8
   ) {
      String var10 = null;
      Path var11 = null;

      try {
         Feature var12 = Feature.read(Resource.text(var1.c(), this.workingDir));
         SuiteResult var13 = this.anchored(io.karatelabs.core.Runner.features(var12), var7)
            .global("__postman", var1.d())
            .outputJsonLines(true)
            .outputHtmlReport(true)
            .backupOutputDir(false)
            .outputDir(this.runs.a(var2))
            .parallel(1);
         var11 = var13.getReportDir();
         if (var13.getErrors() != null && !var13.getErrors().isEmpty()) {
            var10 = var13.getErrors().get(0);
         }
      } catch (Exception var20) {
         logger.warn("postman run {} ('{}') failed: {}", new Object[]{var2, var3, var20.getMessage()});
         var10 = var20.getMessage();
      }

      int var21 = 0;
      int var22 = 0;
      int var14 = 0;
      var var15 = var1.a(var10);

      for (Map var17 : (Iterable<Map>) (Iterable<?>) (var15)) {
         String var18 = String.valueOf(var17.get("status"));
         switch (var18) {
            case "passed":
               var21++;
               break;
            case "notchecked":
               var14++;
               break;
            default:
               var22++;
         }
      }

      boolean var23 = var21 == var15.size();
      var var24 = postmanMeta(var2, var3, var4, var5, var6, var8);
      var24.put("status", var23 ? "completed" : "failed");
      var24.put("passed", var21);
      var24.put("failed", var22);
      var24.put("notchecked", var14);
      var24.put("total", var15.size());
      var24.put("passes", var1.b());
      this.activeRuns.finish(var2);
      this.settleMeta(var2, var24);
      LinkedHashMap var25 = new LinkedHashMap();
      var25.put("runId", var2);
      var25.put("kind", "postman");
      var25.put("ok", var23);
      var25.put("passed", var21);
      var25.put("failed", var22);
      var25.put("notchecked", var14);
      var25.put("total", var15.size());
      if (var11 != null) {
         var25.put("reportUrl", var11.resolve("karate-summary.html").toUri().toString());
      }

      return JsApi.next(var25, "Postman.results('" + var2 + "')");
   }

   private static Map<String, Object> postmanMeta(String var0, String var1, String var2, String var3, String var4, long var5) {
      LinkedHashMap var7 = new LinkedHashMap();
      var7.put("runId", var0);
      var7.put("task", var1);
      var7.put("mode", "postman");
      var7.put("type", "postman");
      var7.put("kind", "postman");
      var7.put("collection", var1);
      var7.put("item", var2);
      if (var3 != null) {
         var7.put("data", var3);
      }

      var7.put("version", var4);
      var7.put("startedMs", var5);
      var7.put("finishedMs", System.currentTimeMillis());
      return var7;
   }

   private static Map<String, Object> envelope(String var0, String var1, int var2) {
      LinkedHashMap var3 = new LinkedHashMap();
      var3.put("error", var0);
      var3.put("code", var1);
      var3.put("status", var2);
      return var3;
   }

   private static Path canonical(Path var0) {
      try {
         return var0.toFile().getCanonicalFile().toPath();
      } catch (Exception var2) {
         return var0.toAbsolutePath().normalize();
      }
   }

   private Map<String, Object> decorateSuiteResult(Map<String, Object> var1) {
      if (!"error".equals(var1.get("command")) && !var1.containsKey("error")) {
         var1.put("kind", "suite");
         if (!"started".equals(var1.get("status"))) {
            var1.put("ok", asInt(var1.get("failed")) == 0);
         }

         return var1;
      } else {
         return var1;
      }
   }

   private Map<String, Object> runCheckAsFeature(String var1, Object var2) {
      String var3 = this.slot == null ? null : this.slot.runBusyReason();
      if (var3 != null) {
         return JsApi.missing(var3);
      } else {
         String var4 = UUID.randomUUID().toString().substring(0, 8);
         boolean var5 = this.draftQuarantined();
         Path var6 = var5 ? this.draftDir(var4) : this.runs.a(var4);
         long var7 = System.currentTimeMillis();
         if (!var5) {
            var var9 = checkMeta(var4, var1, true, var7);
            var9.put("status", "running");
            var9.remove("finishedMs");
            this.runs.a(var4, io.karatelabs.ext.max.jsapi.EvidenceProtocolMetadata.a(var9));
            if (this.activeRuns != null) {
               this.activeRuns.start(var4);
            }
         }

         long var19 = io.karatelabs.ext.common.EvidenceContext.b();

         SuiteResult var11;
         try {
            Feature var12 = this.checkFeature(var1, var2);
            var11 = this.anchored(io.karatelabs.core.Runner.features(var12), this.console.at(null))
               .onSuite(this::guardBots)
               .outputJsonLines(true)
               .outputHtmlReport(true)
               .backupOutputDir(false)
               .outputDir(var6)
               .parallel(1);
         } catch (Exception var18) {
            logger.warn("check run {} ('{}') failed to start: {}", new Object[]{var4, var1, var18.getMessage()});
            if (!var5) {
               if (this.activeRuns != null) {
                  this.activeRuns.finish(var4);
               }

               this.settleMeta(var4, checkMeta(var4, var1, false, var7));
            }

            return checkResult(var1, Map.of("ok", false, "runId", var4, "error", String.valueOf(var18.getMessage())), false);
         }

         boolean var20 = var11.getScenarioFailedCount() == 0;
         boolean var13 = false;
         if (!var5) {
            if (this.activeRuns != null) {
               this.activeRuns.finish(var4);
            }

            this.settleMeta(var4, checkMeta(var4, var1, var20, var7));
            var13 = this.foldIntoProjectGraph(var4);
         }

         LinkedHashMap var14 = new LinkedHashMap();
         var14.put("ok", var20);
         var14.put("runId", var4);
         var var15 = this.checkTally(var6, var1, var20, io.karatelabs.ext.common.EvidenceContext.b() - var19);
         if (var15 != null) {
            var14.put("verified", var15);
         }

         if (var5) {
            discloseQuarantine(var14, var6);
         }

         if (!var20) {
            var var16 = var11.getErrors();
            String var17 = var16 != null && !var16.isEmpty() ? String.join("; ", var16) : "check failed";
            var14.put("error", withBootHint(var17));
         }

         Path var21 = var11.getReportDir();
         if (var21 != null) {
            var14.put("reportUrl", var21.resolve("karate-summary.html").toUri().toString());
         }

         return checkResult(var1, var14, var13);
      }
   }

   static String withBootHint(String var0) {
      return var0 != null && var0.matches("(?s).*\\b(Check|Rule|match) is not defined\\b.*")
         ? var0
            + "\nThis project has no karate-boot.js (or it does not boot the rules ext), so the check's acceptance globals aren't bound in the run. Create karate-boot.js in the project root: `const r = boot.ext('rules'); r.home = 'rulebooks';` binds Check/Rule/match; add `boot.ext('robot')` for `bot`, and `const cov = boot.ext('coverage'); cov.requirements = 'requirements'` to land @req= coverage in the RTM."
         : var0;
   }

   private Feature checkFeature(String var1, Object var2) {
      var var3 = Check.at(MaxWorkspace.checksDir(this.workingDir), null).linkTags(var1);
      return Feature.read(Resource.text(syntheticCheckFeature(var1, var2, var3), this.workingDir));
   }

   private static String syntheticCheckFeature(String var0, Object var1, List<String> var2) {
      String var3 = var0.replace("\\", "\\\\").replace("'", "\\'");
      String var4 = null;
      if (var1 != null) {
         try {
            var4 = Json.stringifyStrict(var1);
         } catch (Exception var10) {
         }
      }

      String var5 = var4 == null ? "Check.run('" + var3 + "')" : "Check.run('" + var3 + "', " + var4 + ")";
      String var6 = Check.definitionId(var0).replace("\\", "\\\\").replace("'", "\\'");
      StringBuilder var7 = new StringBuilder();

      for (String var9 : var2) {
         if (io.karatelabs.ext.coverage.graph.TraceGraph.a(var9) == null) {
            var7.append(var7.isEmpty() ? "  " : " ").append('@').append(var9);
         }
      }

      return "Feature: "
         + var0
         + "\n"
         + (var7.isEmpty() ? "" : var7 + "\n")
         + "  Scenario: "
         + var0
         + "\n    * def __id = '"
         + var6
         + "'\n    * eval "
         + var5
         + "\n";
   }

   private static Map<String, Object> checkMeta(String var0, String var1, boolean var2, long var3) {
      LinkedHashMap var5 = new LinkedHashMap();
      var5.put("runId", var0);
      var5.put("task", var1);
      var5.put("mode", "check");
      var5.put("type", "check");
      var5.put("check", var1);
      var5.put("status", var2 ? "completed" : "failed");
      var5.put("startedMs", var3);
      var5.put("finishedMs", System.currentTimeMillis());
      return var5;
   }

   private Map<String, Object> checkTally(Path var1, String var2, boolean var3, long var4) {
      try {
         Path var6 = MaxPaths.eventsFile(var1);
         if (!Files.isRegularFile(var6)) {
            return null;
         } else {
            io.karatelabs.ext.coverage.collect.JsonlRunEvidenceReader var7 = new io.karatelabs.ext.coverage.collect.JsonlRunEvidenceReader(var6);
            if (var7.g().isEmpty()) {
               return null;
            } else {
               io.karatelabs.ext.coverage.collect.TestExecution var8 = var7.g().get(0);
               var var9 = Check.at(MaxWorkspace.checksDir(this.workingDir), null).linkTags(var2);
               LinkedHashSet var10 = new LinkedHashSet();
               ArrayList var11 = new ArrayList();

               for (Action var13 : var8.x()) {
                  if (var13 instanceof io.karatelabs.ext.coverage.EmitAction var14) {
                     if ("req".equals(var14.a()) && var14.b() != null) {
                        var10.add(var14.b());
                     }

                     if (var14.c() != null) {
                        var11.add(var14.c());
                     }
                  }
               }

               return Harness.tally(var8.z(), var10, var3, var8.x().size(), var9, var4, Harness.verdicts(var11));
            }
         }
      } catch (Exception var15) {
         logger.debug("no tally for check run in {}: {}", var1, var15.getMessage());
         return null;
      }
   }

   private static Map<String, Object> checkResult(String var0, Map<String, Object> var1, boolean var2) {
      boolean var3 = Boolean.TRUE.equals(var1.get("ok"));
      LinkedHashMap var4 = new LinkedHashMap();
      var4.put("runId", var1.get("runId"));
      var4.put("kind", "check");
      var4.put("check", var0);
      var4.put("ok", var3);
      var4.put("passed", var3 ? 1 : 0);
      var4.put("failed", var3 ? 0 : 1);
      var4.put("total", 1);
      if (var1.get("verified") != null) {
         var4.put("verified", var1.get("verified"));
      }

      if (var1.get("reportUrl") != null) {
         var4.put("reportUrl", var1.get("reportUrl"));
      }

      if (var1.get("error") != null) {
         var4.put("error", var1.get("error"));
      }

      if (Boolean.TRUE.equals(var1.get("quarantined"))) {
         var4.put("quarantined", true);
         var4.put("quarantineDir", var1.get("quarantineDir"));
         var4.put("quarantineReason", var1.get("quarantineReason"));
         return JsApi.next(
            var4,
            var3
               ? "the check passed AGAINST A DRAFT MOCK — this run is not evidence; mock.freeze('mock/<name>.js') promotes the mock, then re-run for the record"
               : "the check failed (against a draft mock) — open its report under target/draft/, fix, then re-run"
         );
      } else {
         String var5 = "the check failed — open its report, fix the check, then re-run";
         return !var2
            ? JsApi.next(var4, "Report.aggregate()", var3 ? "the check passed; open its report or aggregate it into the RTM" : var5)
            : JsApi.next(
               var4,
               "Requirement.matrix({run:'" + var1.get("runId") + "'})",
               var3 ? "the check passed and is folded into the project graph; the call above reads this run alone" : var5
            );
      }
   }

   private static int asInt(Object var0) {
      return var0 instanceof Number var1 ? var1.intValue() : 0;
   }

   Map<String, Object> suite(Object var1, Object var2) {
      var var3 = envRefusal(var2);
      if (var3 != null) {
         return var3;
      } else {
         var var4 = pathList(var1);
         if (var4.isEmpty()) {
            return JsApi.missing(
               "Runner.suite(path[, opts]) requires a suite path String or a list of paths, e.g. Runner.suite('api/'), Runner.suite('users.feature', {tags:'@smoke'}), Runner.suite(['a.feature','b.feature']), or Runner.suite('.') to run every feature"
            );
         } else {
            for (String var6 : (Iterable<String>) (Iterable<?>) (var4)) {
               if (var6.endsWith(".karate.js")) {
                  return JsApi.missing(
                     "'"
                        + var6
                        + "' is a *.karate.js orchestrator — it composes Runner.*/Harness.* itself, so run it with Runner.launch('"
                        + var6
                        + "') from here, or `karate launch "
                        + var6
                        + "` from a shell; not Runner.suite"
                  );
               }
            }

            var var8 = new LinkedHashMap<>(var2 instanceof Map var9 ? cast(var9) : Map.of());
            ConsoleState.Kickoff var10 = this.console.at(var8.get("env") == null ? null : String.valueOf(var8.get("env")));
            if (var10.env() != null) {
               var8.put("env", var10.env());
            }

            if (var8.get("session") != null) {
               String var7 = this.sessionRequest(var4, String.valueOf(var8.get("session")));
               if (var7 != null) {
                  return JsApi.missing("Runner.run: " + var7);
               }
            }

            io.karatelabs.ext.max.jsapi.Runner.RunSelectionPlan var11 = this.resolveSet(var4, var8);
            return isAsync(var8) ? this.suiteAsync(var11, var8, var10) : this.suiteSync(var11, var8, var10);
         }
      }
   }

   private io.karatelabs.ext.max.jsapi.Runner.RunSelectionPlan resolveSet(List<String> var1, Map<String, Object> var2) {
      LinkedHashMap var3 = new LinkedHashMap();

      for (String var5 : var1) {
         if (isClasspathDir(var5)) {
            for (io.karatelabs.ext.max.jsapi.Runner.RunResource var13 : this.scanClasspathDir(var5.substring("classpath:".length()))) {
               var3.putIfAbsent(var13.a(), var13);
            }
         } else {
            Path var6 = var5.startsWith("classpath:") ? null : Path.of(this.resolvePath(var5));
            if (var6 != null && Files.isDirectory(var6)) {
               for (io.karatelabs.ext.max.jsapi.Runner.RunResource var8 : this.scanDir(var6)) {
                  var3.putIfAbsent(var8.a(), var8);
               }
            } else {
               String var7 = var6 == null ? var5 : this.memberPath(var6);
               var3.putIfAbsent(var7, new io.karatelabs.ext.max.jsapi.Runner.RunResource(var7, var5.endsWith(".js") ? "js" : "feature"));
            }
         }
      }

      ArrayList<io.karatelabs.ext.max.jsapi.Runner.RunResource> var9 = new ArrayList<>(var3.values());
      boolean var10 = var9.stream().anyMatch(var0 -> "js".equals(var0.b()));
      return new io.karatelabs.ext.max.jsapi.Runner.RunSelectionPlan(var1, var9, var10 ? 1 : threadsOf(var2), var10);
   }

   private static boolean isClasspathDir(String var0) {
      if (var0.startsWith("classpath:") && !var0.endsWith(".js")) {
         String var1 = var0.substring("classpath:".length());
         int var2 = var1.indexOf(".feature:");
         if (var2 >= 0 && var1.substring(var2 + ".feature:".length()).matches("\\d+(:\\d+)*")) {
            var1 = var1.substring(0, var2 + ".feature".length());
         }

         return !var1.endsWith(".feature");
      } else {
         return false;
      }
   }

   private List<io.karatelabs.ext.max.jsapi.Runner.RunResource> scanClasspathDir(String var1) {
      ArrayList var2 = new ArrayList();

      for (Resource var4 : Resource.scanClasspath(var1, "feature", null, this.workingDir)) {
         var2.add(new io.karatelabs.ext.max.jsapi.Runner.RunResource(var4.getRelativePath(), "feature", var4));
      }

      var2.sort(Comparator.comparing(io.karatelabs.ext.max.jsapi.Runner.RunResource::a));
      return var2;
   }

   private List<io.karatelabs.ext.max.jsapi.Runner.RunResource> scanDir(Path var1) {
      Path var2 = this.workingDir.toAbsolutePath().normalize().resolve("checks");
      ArrayList var3 = new ArrayList();

      try (var var4 = Files.walk(var1)) {
         var4.filter(var0 -> Files.isRegularFile(var0)).forEach(var3x -> {
            String var4x = var3x.getFileName().toString();
            if (var4x.endsWith(".feature")) {
               var3.add(new io.karatelabs.ext.max.jsapi.Runner.RunResource(this.memberPath(var3x), "feature"));
            } else if (var4x.endsWith(".js") && !var4x.endsWith(".karate.js") && var3x.toAbsolutePath().normalize().startsWith(var2)) {
               var3.add(new io.karatelabs.ext.max.jsapi.Runner.RunResource(this.memberPath(var3x), "js"));
            }
         });
      } catch (Exception var9) {
         logger.warn("could not scan '{}' for suite members: {}", var1, var9.getMessage());
      }

      var3.sort(Comparator.comparing(io.karatelabs.ext.max.jsapi.Runner.RunResource::a));
      return var3;
   }

   private String memberPath(Path var1) {
      Path var2 = var1.toAbsolutePath().normalize();
      Path var3 = this.workingDir.toAbsolutePath().normalize();
      return (var2.startsWith(var3) ? var3.relativize(var2) : var2).toString().replace('\\', '/');
   }

   private List<Feature> memberFeatures(List<io.karatelabs.ext.max.jsapi.Runner.RunResource> var1) {
      ArrayList var2 = new ArrayList();

      for (io.karatelabs.ext.max.jsapi.Runner.RunResource var4 : var1) {
         if ("js".equals(var4.b())) {
            var2.add(this.checkFeature(var4.a(), null));
         } else if (var4.c() != null) {
            var2.add(Feature.read(var4.c()));
         } else {
            String var5 = this.resolvePath(var4.a());
            var2.add(Feature.read(var5.startsWith("classpath:") ? Resource.path(var5, this.workingDir) : Resource.from(Path.of(var5), this.workingDir)));
         }
      }

      return var2;
   }

   private boolean leases(io.karatelabs.ext.max.jsapi.Runner.RunSelectionPlan var1) {
      return var1.e() && this.slot != null;
   }

   private String leaseSlot(io.karatelabs.ext.max.jsapi.Runner.RunSelectionPlan var1, String var2) {
      return this.leases(var1) ? this.slot.leaseSlot(var2) : null;
   }

   private void guardBots(Suite var1) {
      if (this.slot != null && var1.getGlobal("Robot") instanceof RobotFactory var2) {
         var2.onCreateGuard(this.slot::createBusyReason);
      }
   }

   private Map<String, Object> suiteSync(io.karatelabs.ext.max.jsapi.Runner.RunSelectionPlan var1, Map<String, Object> var2, ConsoleState.Kickoff var3) {
      String var4 = runIdOf(var2);
      String var5 = this.leaseSlot(var1, var4);
      if (var5 != null) {
         return JsApi.missing(var5);
      } else {
         boolean var6 = this.leases(var1);
         if (var6) {
            this.slot.ownSlot(var4);
         }

         Map var7;
         try {
            var7 = this.suiteSyncBody(var1, var2, var4, var3);
         } finally {
            if (var6) {
               this.slot.disownSlot();
               this.slot.releaseSlot(var4);
            }
         }

         return var7;
      }
   }

   private Map<String, Object> suiteSyncBody(io.karatelabs.ext.max.jsapi.Runner.RunSelectionPlan var1, Map<String, Object> var2, String var3, ConsoleState.Kickoff var4) {
      boolean var5 = this.draftQuarantined();
      io.karatelabs.ext.max.jsapi.Runner.OutputPlan var6 = var5 ? new io.karatelabs.ext.max.jsapi.Runner.OutputPlan(this.draftDir(var3), true) : this.planOutput(var2, var3);
      long var7 = System.currentTimeMillis();
      String var9 = describe(var1.b());
      boolean var10 = dryRunEnabled(var2);
      if (var6.b()) {
         stampStreamProvenance(var6.a(), var3, var10, var5);
      } else {
         this.runs.a(var3, io.karatelabs.ext.max.jsapi.EvidenceProtocolMetadata.a(this.runningMeta(var3, var9, var1, var2, var7)));
         if (this.activeRuns != null) {
            this.activeRuns.start(var3);
         }
      }

      Process var11 = videoEnabled(var2) ? io.karatelabs.ext.robot.VideoRecorder.a(var6.a().resolve("video.mp4")) : null;
      LiveSessions.Capture var13 = this.capture(var1, var2, var4);

      SuiteResult var12;
      try {
         var12 = this.builder(
               var1, var2, var6.a(), junitXmlEnabled(var2, var6.b()), this.activeRuns == null ? null : var2x -> this.activeRuns.suite(var3, var2x), var4, var13
            )
            .parallel(var1.d());
      } finally {
         if (var13 != null) {
            this.sessions.publish(var13);
         }

         io.karatelabs.ext.robot.VideoRecorder.a(var11);
         if (this.activeRuns != null) {
            this.activeRuns.finish(var3);
         }
      }

      var var14 = this.pageRunTriage(var13, var12, var3);
      if (!var6.b()) {
         var var15 = this.suiteMeta(var3, var9, var1, var12, var2, var7);
         if (var14 != null) {
            var15.put("triage", var14);
         }

         this.settleMeta(var3, var15);
         if (!var10) {
            this.foldIntoProjectGraph(var3);
         }
      } else {
         stampGraphProvenance(var6.a(), var3, var10);
      }

      var var18 = runResult(var3, var1, var12, var10);
      if (var5) {
         discloseQuarantine(var18, var6.a());
      }

      if (var13 != null) {
         var18.put("session", var13.outcome());
      }

      if (var14 != null) {
         var18.put("triage", var14);
      }

      return var18;
   }

   private static boolean videoEnabled(Map<String, Object> var0) {
      Object var1 = var0.get("video");
      if (var1 != null) {
         return var1 instanceof Boolean var3 ? var3 : Boolean.parseBoolean(String.valueOf(var1));
      } else {
         String var2 = System.getenv("KARATE_VIDEO");
         return var2 != null && (var2.equalsIgnoreCase("true") || var2.equals("1") || var2.equalsIgnoreCase("on"));
      }
   }

   private Map<String, Object> suiteAsync(io.karatelabs.ext.max.jsapi.Runner.RunSelectionPlan var1, Map<String, Object> var2, ConsoleState.Kickoff var3) {
      String var4 = runIdOf(var2);
      String var5 = this.leaseSlot(var1, var4);
      if (var5 != null) {
         return JsApi.missing(var5);
      } else {
         boolean var6 = this.leases(var1);
         boolean var7 = this.draftQuarantined();
         io.karatelabs.ext.max.jsapi.Runner.OutputPlan var8 = var7 ? new io.karatelabs.ext.max.jsapi.Runner.OutputPlan(this.draftDir(var4), true) : this.planOutput(var2, var4);
         boolean var9 = !var8.b();
         long var10 = System.currentTimeMillis();
         String var12 = describe(var1.b());
         LiveSessions.Capture var14 = this.capture(var1, var2, var3);

         io.karatelabs.core.Runner.Builder var13;
         try {
            if (var9) {
               this.runs.a(var4, io.karatelabs.ext.max.jsapi.EvidenceProtocolMetadata.a(this.runningMeta(var4, var12, var1, var2, var10)));
            } else {
               stampStreamProvenance(var8.a(), var4, dryRunEnabled(var2), var7);
            }

            this.activeRuns.start(var4);
            var13 = this.builder(var1, var2, var8.a(), junitXmlEnabled(var2, var8.b()), var2x -> this.activeRuns.suite(var4, var2x), var3, var14);
            if (this.publisher != null) {
               var13.listener(new RunEventSseBridge((var2x, var3x) -> this.publisher.publish(var4, var2x, var3x)));
            }
         } catch (RuntimeException var20) {
            if (var6) {
               this.slot.releaseSlot(var4);
            }

            if (var9) {
               this.runs.a(var4, this.errorMeta(var4, var12, var1, var2, var10, var20.getMessage()));
            }

            this.activeRuns.finish(var4);
            this.publishError(var4, var20.getMessage());
            throw var20;
         }

         int var15 = var1.d();
         boolean var16 = videoEnabled(var2);
         boolean var17 = dryRunEnabled(var2);
         Path var18 = var8.a().resolve("video.mp4");
         Thread.ofVirtual()
            .name("suite-" + var4)
            .start(
               () -> {
                  if (var6) {
                     this.slot.ownSlot(var4);
                  }

                  Process var16x = var16 ? io.karatelabs.ext.robot.VideoRecorder.a(var18) : null;
                  String var17x = var16 && var16x == null
                     ? "the run video was requested but the display recorder did not start (no ffmpeg, or no X display) — this run has no video"
                     : null;

                  try {
                     SuiteResult var18x = var13.parallel(var15);
                     if (var14 != null) {
                        this.sessions.publish(var14);
                     }

                     boolean var27 = this.activeRuns.stopRequested(var4);
                     io.karatelabs.ext.robot.VideoRecorder.a(var16x);
                     if (var6) {
                        this.slot.disownSlot();
                        this.slot.releaseSlot(var4);
                     }

                     var var20x = this.pageRunTriage(var14, var18x, var4);
                     if (var9) {
                        var var21 = this.suiteMeta(var4, var12, var1, var18x, var2, var10);
                        if (var27) {
                           var21.put("status", "stopped");
                        }

                        if (var17x != null) {
                           var21.put("videoError", var17x);
                        }

                        if (var20x != null) {
                           var21.put("triage", var20x);
                        }

                        this.settleMeta(var4, var21);
                     } else {
                        stampGraphProvenance(var8.a(), var4, var17);
                     }

                     if (var9 && !var17) {
                        this.foldIntoProjectGraph(var4);
                     }

                     this.publishComplete(var4, var18x, var27);
                  } catch (Exception var25) {
                     logger.warn("suite run {} failed: {}", var4, var25.getMessage());
                     if (var6) {
                        this.slot.disownSlot();
                        this.slot.releaseSlot(var4);
                     }

                     if (var9) {
                        var var19x = this.errorMeta(var4, var12, var1, var2, var10, var25.getMessage());
                        if (var17x != null) {
                           var19x.put("videoError", var17x);
                        }

                        this.settleMeta(var4, var19x);
                     }

                     this.publishError(var4, var25.getMessage());
                  } finally {
                     io.karatelabs.ext.robot.VideoRecorder.a(var16x);
                     if (var6) {
                        this.slot.disownSlot();
                        this.slot.releaseSlot(var4);
                     }

                     this.activeRuns.finish(var4);
                  }
               }
            );
         LinkedHashMap var19 = new LinkedHashMap();
         var19.put("runId", var4);
         var19.put("status", "started");
         var19.put("mode", "suite");
         var19.put("task", var12);
         if (dryRunEnabled(var2)) {
            var19.put("dryRun", true);
         }

         if (var7) {
            discloseQuarantine(var19, var8.a());
         }

         return JsApi.next(
            var19,
            "Harness.get('" + var4 + "')",
            "the suite is running — poll `Harness.get('"
               + var4
               + "')`: when it settles it returns `status`, `passed`/`failed`, a ready `verdicts` array (`[{name, passed, error?}]` — the per-row read-back, no jsonl parsing needed), and `reportUrl`. Watch live at /runs/"
               + var4
               + ". Note: `Harness.status()` tracks only the agentic LOOP, not a suite run. TIP: for a QUICK suite the blocking `Runner.suite(path)` (no async) returns the same `{passed, failed, verdicts, reportUrl}` in one call; you chose async, which is the right call for a slow one — this runId keeps working even if your client drops."
         );
      }
   }

   Map<String, Object> stop(String var1) {
      if (var1 == null || var1.isBlank()) {
         return JsApi.missing("pass the id of the in-flight suite — Runner.stop('<runId>'); the /runs list (or Harness.runs()) shows what is running");
      } else if (!this.settledOnDisk(var1) && this.activeRuns.stop(var1)) {
         LinkedHashMap var2 = new LinkedHashMap();
         var2.put("runId", var1);
         var2.put("status", "stopping");
         return JsApi.next(var2, "Harness.get('" + var1 + "')", "the executing scenario finishes, the rest are skipped; poll until status reads 'stopped'");
      } else {
         return JsApi.missing("no in-flight suite '" + var1 + "' — it has settled (or never started); Harness.get('" + var1 + "') reads its record");
      }
   }

   private io.karatelabs.core.Runner.Builder anchored(io.karatelabs.core.Runner.Builder var1, ConsoleState.Kickoff var2) {
      return var2.apply(var1.configDir(this.workingDir.toString()).workingDir(this.workingDir)).listener(io.karatelabs.ext.coverage.HttpExchangeEvidenceRecorder.nonCoverageListener());
   }

   public Runner withDraftGuard(Supplier<Boolean> var1) {
      this.draftGuard = var1;
      return this;
   }

   private boolean draftQuarantined() {
      return this.draftGuard != null && Boolean.TRUE.equals(this.draftGuard.get());
   }

   private Path draftDir(String var1) {
      return this.workingDir.resolve("target").resolve("draft").resolve(var1);
   }

   private static void discloseQuarantine(Map<String, Object> var0, Path var1) {
      var0.put("quarantined", true);
      var0.put("quarantineDir", var1.toString());
      var0.put(
         "quarantineReason",
         "a DRAFT mock is live (session-authored, un-frozen) — this run wrote under target/draft/ and is excluded from evidence (runs/, coverage, RTM). mock.freeze('mock/<name>.js') promotes the program, then re-run for evidence."
      );
   }

   private io.karatelabs.ext.max.jsapi.Runner.OutputPlan planOutput(Map<String, Object> var1, String var2) {
      String var3 = outputDirOpt(var1);
      if (var3 != null) {
         return new io.karatelabs.ext.max.jsapi.Runner.OutputPlan(this.anchorOutput(var3), true);
      } else {
         return this.defaultOutputDir != null ? new io.karatelabs.ext.max.jsapi.Runner.OutputPlan(this.defaultOutputDir, true) : new io.karatelabs.ext.max.jsapi.Runner.OutputPlan(this.runs.a(var2), false);
      }
   }

   private static void stampStreamProvenance(Path var0, String var1, boolean var2) {
      stampStreamProvenance(var0, var1, var2, false);
   }

   private static void stampStreamProvenance(Path var0, String var1, boolean var2, boolean var3) {
      stampProvenance(var0.resolve("karate-json/run.json"), var1, var2, var3);
   }

   private static void stampGraphProvenance(Path var0, String var1, boolean var2) {
      stampProvenance(var0.resolve("ext/coverage/run.json"), var1, var2, false);
   }

   private static void stampProvenance(Path var0, String var1, boolean var2, boolean var3) {
      LinkedHashMap var4 = new LinkedHashMap();
      var4.put("runId", var1);
      var4.put("dryRun", var2);
      if (var3) {
         var4.put("draft", true);
      }

      try {
         Files.createDirectories(var0.getParent());
         Files.writeString(var0, Json.stringifyStrict(var4));
      } catch (Exception var6) {
         logger.warn("could not stamp run provenance at {}: {}", var0, var6.getMessage());
      }
   }

   private static String outputDirOpt(Map<String, Object> var0) {
      Object var1 = var0.get("output");
      if (var1 instanceof Map var2) {
         Object var3 = var2.get("dir");
         return var3 != null && !String.valueOf(var3).isBlank() ? String.valueOf(var3) : null;
      } else {
         return var1 != null && !String.valueOf(var1).isBlank() ? String.valueOf(var1) : null;
      }
   }

   private Path anchorOutput(String var1) {
      Path var2 = Path.of(var1);
      return (var2.isAbsolute() ? var2 : this.workingDir.resolve(var2)).normalize();
   }

   private static boolean junitXmlEnabled(Map<String, Object> var0, boolean var1) {
      if (var0.get("output") instanceof Map var3 && var3.get("junitXml") != null) {
         Object var4 = var3.get("junitXml");
         return var4 instanceof Boolean var5 ? var5 : Boolean.parseBoolean(String.valueOf(var4));
      } else {
         return var1;
      }
   }

   private io.karatelabs.core.Runner.Builder builder(
      io.karatelabs.ext.max.jsapi.Runner.RunSelectionPlan var1, Map<String, Object> var2, Path var3, boolean var4, Consumer<Suite> var5, ConsoleState.Kickoff var6, LiveSessions.Capture var7
   ) {
      io.karatelabs.core.Runner.Builder var8 = this.anchored(
            var1.e()
               ? io.karatelabs.core.Runner.builder().features(this.memberFeatures(var1.c()))
               : io.karatelabs.core.Runner.path(this.resolvePaths(var1.b())),
            var6
         )
         .outputJsonLines(true)
         .outputHtmlReport(true)
         .outputJunitXml(var4)
         .backupOutputDir(false)
         .outputDir(var3);
      if (var2.get("__overlay") instanceof io.karatelabs.ext.max.jsapi.Runner.FileOverlay var9) {
         var8.overlay(var9.a(), var9.b());
         if (var9.c() != null && var9.d() != null) {
            var8.overlay(var9.c(), var9.d());
         }
      }

      var8.onSuite(var2x -> {
         this.guardBots(var2x);
         if (var5 != null) {
            var5.accept(var2x);
         }
      });
      var var11 = tagList(var2.get("tags"));
      if (!var11.isEmpty()) {
         var8.tags(var11.toArray(new String[0]));
      }

      applyProps(var8, var2.get("props"));
      if (dryRunEnabled(var2)) {
         var8.dryRun(true);
      }

      if (var7 != null) {
         var8.httpClientFactory(this.sessions.factory()).listener(this.sessions.listener(var7));
      }

      if (this.leases(var1)) {
         var8.resultListener(new ResultListener() {
            @Override
            public void onFeatureEnd(FeatureResult var1) {
               Runner.this.slot.leasedMemberEnd();
            }
         });
      }

      return var8;
   }

   private static boolean dryRunEnabled(Map<String, Object> var0) {
      Object var1 = var0.get("dryRun");
      return Boolean.TRUE.equals(var1) || "true".equals(String.valueOf(var1));
   }

   private static void applyProps(io.karatelabs.core.Runner.Builder var0, Object var1) {
      if (var1 != null) {
         if (var1 instanceof Map var2) {
            for (Entry var4 : (Iterable<Entry>) (Iterable<?>) (var2.entrySet())) {
               if (var4.getKey() != null && var4.getValue() != null) {
                  var0.systemProperty(String.valueOf(var4.getKey()), String.valueOf(var4.getValue()));
               }
            }
         } else {
            throw new IllegalArgumentException(
               "Runner opts.props must be an object of name → value, e.g. { props: { 'loan.url': 'http://host.docker.internal:9100' } } — got: " + var1
            );
         }
      }
   }

   private static boolean isAsync(Map<String, Object> var0) {
      Object var1 = var0.get("async");
      return Boolean.TRUE.equals(var1) || "true".equals(String.valueOf(var1));
   }

   private static int threadsOf(Map<String, Object> var0) {
      return var0.get("threads") instanceof Number var1 ? Math.max(1, var1.intValue()) : 1;
   }

   private void publishComplete(String var1, SuiteResult var2) {
      this.publishComplete(var1, var2, false);
   }

   private void publishComplete(String var1, SuiteResult var2, boolean var3) {
      if (this.publisher != null) {
         LinkedHashMap var4 = new LinkedHashMap();
         var4.put("type", "complete");
         var4.put("status", var3 ? "stopped" : settledStatus(var2));
         var4.put("runId", var1);
         var4.put("passed", var2.getScenarioPassedCount());
         var4.put("failed", var2.getScenarioFailedCount());
         var4.put("total", var2.getScenarioCount());
         var4.put("ts", System.currentTimeMillis());
         this.publisher.publish(var1, "complete", Json.stringifyStrict(var4));
      }
   }

   private void publishError(String var1, String var2) {
      if (this.publisher != null) {
         LinkedHashMap var3 = new LinkedHashMap();
         var3.put("type", "complete");
         var3.put("status", "failed");
         var3.put("runId", var1);
         var3.put("error", var2);
         var3.put("ts", System.currentTimeMillis());
         this.publisher.publish(var1, "complete", Json.stringifyStrict(var3));
      }
   }

   private static String settledStatus(SuiteResult var0) {
      return var0.getScenarioCount() == 0 ? "empty" : (var0.getScenarioFailedCount() > 0 ? "failed" : "completed");
   }

   private Map<String, Object> suiteMeta(String var1, String var2, io.karatelabs.ext.max.jsapi.Runner.RunSelectionPlan var3, SuiteResult var4, Map<String, Object> var5, long var6) {
      var var8 = baseSuiteMeta(var1, var2, var3, var5, var6);
      var8.put("status", settledStatus(var4));
      var8.put("finishedMs", System.currentTimeMillis());
      var8.put("total", var4.getScenarioCount());
      var8.put("passed", var4.getScenarioPassedCount());
      var8.put("failed", var4.getScenarioFailedCount());
      Path var9 = var4.getReportDir();
      if (var9 != null) {
         var var10 = io.karatelabs.ext.max.jsapi.RunEvidenceInspector.a(var9, var3.a());
         if (!var10.isEmpty()) {
            var8.put("memberVerdicts", var10);
         }
      }

      return var8;
   }

   private Map<String, Object> runningMeta(String var1, String var2, io.karatelabs.ext.max.jsapi.Runner.RunSelectionPlan var3, Map<String, Object> var4, long var5) {
      var var7 = baseSuiteMeta(var1, var2, var3, var4, var5);
      var7.put("status", "running");
      return var7;
   }

   private Map<String, Object> errorMeta(String var1, String var2, io.karatelabs.ext.max.jsapi.Runner.RunSelectionPlan var3, Map<String, Object> var4, long var5, String var7) {
      var var8 = baseSuiteMeta(var1, var2, var3, var4, var5);
      var8.put("status", "failed");
      var8.put("finishedMs", System.currentTimeMillis());
      var8.put("error", var7);
      return var8;
   }

   private static Map<String, Object> baseSuiteMeta(String var0, String var1, io.karatelabs.ext.max.jsapi.Runner.RunSelectionPlan var2, Map<String, Object> var3, long var4) {
      LinkedHashMap var6 = new LinkedHashMap();
      var6.put("runId", var0);
      var6.put("task", var1);
      var6.put("mode", "suite");
      var6.put("type", "suite");
      var6.put("startedMs", var4);
      var6.put("selector", List.copyOf(var2.b()));
      var6.put("members", var2.a());
      var6.put("options", effectiveOptions(var2, var3));
      if (Boolean.TRUE.equals(var3.get("formal"))) {
         var6.put("formal", true);
      }

      if (var3.get("req") instanceof String var7 && !var7.isBlank()) {
         var6.put("req", var7.trim());
      }

      if (var3.get("campaign") instanceof String var9 && !var9.isBlank()) {
         var6.put("campaign", var9.trim());
      }

      if (var3.get("campaignId") instanceof String var10 && !var10.isBlank()) {
         var6.put("campaignId", var10.trim());
      }

      if (dryRunEnabled(var3)) {
         var6.put("dryRun", true);
      }

      return var6;
   }

   private static Map<String, Object> effectiveOptions(io.karatelabs.ext.max.jsapi.Runner.RunSelectionPlan var0, Map<String, Object> var1) {
      LinkedHashMap var2 = new LinkedHashMap();
      Object var3 = var1.get("env");
      String var4 = ProjectConfig.ambientEnv();
      if (var4 == null && var3 != null && !String.valueOf(var3).isBlank()) {
         var4 = String.valueOf(var3);
      }

      if (var4 != null) {
         var2.put("env", var4);
      }

      var var5 = tagList(var1.get("tags"));
      if (!var5.isEmpty()) {
         var2.put("tags", var5);
      }

      var2.put("threads", var0.d());
      if (videoEnabled(var1)) {
         var2.put("video", true);
      }

      return var2;
   }

   private static Map<String, Object> runResult(String var0, io.karatelabs.ext.max.jsapi.Runner.RunSelectionPlan var1, SuiteResult var2, boolean var3) {
      LinkedHashMap var4 = new LinkedHashMap();
      var4.put("runId", var0);
      var4.put("passed", var2.getScenarioPassedCount());
      var4.put("failed", var2.getScenarioFailedCount());
      var4.put("total", var2.getScenarioCount());
      int var5 = var2.getScenarioSkippedCount();
      if (var5 > 0) {
         var4.put("skipped", var5);
      }

      if (var3) {
         var4.put("dryRun", true);
      }

      Path var6 = var2.getReportDir();
      if (var6 != null) {
         var4.put("reportDir", var6.toString());
         Path var7 = var6.resolve("karate-summary.html");
         var4.put("reportUrl", var7.toUri().toString());
         var var8 = io.karatelabs.ext.max.jsapi.RunEvidenceInspector.a(var6);
         if (!var8.isEmpty()) {
            var4.put("verdicts", var8);
         }

         var var9 = io.karatelabs.ext.max.jsapi.RunEvidenceInspector.a(var6, var1.a());
         if (!var9.isEmpty()) {
            var4.put("memberVerdicts", var9);
         }
      }

      String var10 = "Requirement.matrix({run:'" + var0 + "'})";
      if (var3) {
         return JsApi.next(
            var4,
            var10,
            "NOTHING EXECUTED — this is the DECLARED graph (the @req=/@cov= tags alone, zero observed hits), so read it as an inventory, not a verdict: in "
               + var10
               + " a NOTRUN item is 'a test claims this, nobody ran it' and a NOTCOVERED item is 'nothing in the suite claims this at all' — the second list is the authoring gap. No item can read COVERED from a dry run. Re-run without dryRun to learn whether the claims hold. NOTE: `passed` counts the skipped scenarios (nothing failed because nothing ran) — `skipped` is the honest total here."
         );
      } else if (var2.getScenarioCount() == 0) {
         var4.put("matchedNothing", true);
         return JsApi.next(
            var4,
            var10,
            "NO SCENARIO WAS SELECTED — this run executed nothing, so `0 passed / 0 failed` is the absence of evidence, NOT a pass. Check the path (a directory that does not exist, or holds no `.feature`, matches silently) and any tag selection. Nothing here may be quoted as coverage, and re-reading the RTM will show whatever the PREVIOUS runs left behind, not this one."
         );
      } else {
         String var11 = var2.getScenarioFailedCount() > 0
            ? "read `verdicts` for the per-row pass/fail; the RTM for this run is " + var10 + " (and readiness({run:'" + var0 + "'}))"
            : "the RTM/readiness is readable now — "
               + var10
               + " / Requirement.readiness({run:'"
               + var0
               + "'}), or unscoped for the project graph this run was folded into; Report.generate()/Report.url() render the scorecard";
         return JsApi.next(var4, var10, var11);
      }
   }

   private void settleMeta(String var1, Map<String, Object> var2) {
      io.karatelabs.ext.max.jsapi.EvidenceProtocolMetadata.a(var2);
      var2.put("evidenceFinalized", true);
      this.runs.a(var1, var1x -> var1x.putAll(var2));
   }

   private boolean settledOnDisk(String var1) {
      Map var2 = this.activeRuns.isActive(var1) && this.runs.c(var1) ? this.runs.b(var1) : null;
      return var2 != null && !"running".equals(var2.get("status"));
   }

   private boolean foldIntoProjectGraph(String var1) {
      if (this.afterRun == null) {
         return false;
      } else {
         try {
            this.afterRun.accept(var1);
            return true;
         } catch (RuntimeException var3) {
            logger.warn(
               "run {} completed, but folding it into the project graph failed ({}) — an unscoped Requirement.matrix()/readiness() will not see it until Report.aggregate() runs",
               var1,
               var3.toString()
            );
            return false;
         }
      }
   }

   private String resolvePath(String var1) {
      return !var1.startsWith("classpath:") && !Path.of(var1).isAbsolute() ? this.workingDir.resolve(var1).toString() : var1;
   }

   private List<String> resolvePaths(List<String> var1) {
      ArrayList var2 = new ArrayList();

      for (String var4 : var1) {
         var2.add(this.resolvePath(var4));
      }

      return var2;
   }

   private static List<String> pathList(Object var0) {
      ArrayList var1 = new ArrayList();
      if (var0 instanceof String var2) {
         if (!var2.isBlank()) {
            var1.add(var2.trim());
         }
      } else if (var0 instanceof List) {
         for (Object var5 : (List)var0) {
            if (var5 != null && !String.valueOf(var5).isBlank()) {
               var1.add(String.valueOf(var5).trim());
            }
         }
      }

      return var1;
   }

   private static String describe(List<String> var0) {
      if (var0.size() != 1) {
         return var0.size() + " features";
      } else {
         String var1 = (String)var0.get(0);
         return !var1.equals(".") && !var1.equals("./") ? var1 : "all features";
      }
   }

   private static List<String> tagList(Object var0) {
      ArrayList var1 = new ArrayList();
      if (var0 instanceof String var2 && !var2.isBlank()) {
         var1.add(var2);
      } else if (var0 instanceof List) {
         for (Object var5 : (List)var0) {
            if (var5 != null && !String.valueOf(var5).isBlank()) {
               var1.add(String.valueOf(var5));
            }
         }
      }

      return var1;
   }

   private static Map<String, Object> cast(Map<?, ?> var0) {
      return (Map<String, Object>)(Map<?, ?>)var0;
   }

   @FunctionalInterface
   public interface LaunchBody {
      Object run() throws Exception;
   }

   public record Launched(Object value, int exitCode, Exception error) {

   }

   private record RunResource(String a, String b, Resource c) {

      RunResource(String var1, String var2) {
         this(var1, var2, null);
      }

   }

   private record OutputPlan(Path a, boolean b) {

   }

   private record FileOverlay(Path a, byte[] b, Path c, byte[] d) {

   }

   private record RunSelectionPlan(List<String> b, List<io.karatelabs.ext.max.jsapi.Runner.RunResource> c, int d, boolean e) {


      List<Map<String, Object>> a() {
         ArrayList var1 = new ArrayList();

         for (io.karatelabs.ext.max.jsapi.Runner.RunResource var3 : this.c) {
            var1.add(Map.of("path", var3.a(), "kind", var3.b()));
         }

         return var1;
      }

      public List<String> b() {
         return this.b;
      }

      public List<io.karatelabs.ext.max.jsapi.Runner.RunResource> c() {
         return this.c;
      }

      public int d() {
         return this.d;
      }

      public boolean e() {
         return this.e;
      }
   }
}
