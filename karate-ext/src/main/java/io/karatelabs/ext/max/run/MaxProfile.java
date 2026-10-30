package io.karatelabs.ext.max.run;

import io.karatelabs.common.Json;
import io.karatelabs.core.BootBinding;
import io.karatelabs.core.BootLoader;
import io.karatelabs.core.Ext;
import io.karatelabs.ext.asyncapi.jsapi.Asyncapi;
import io.karatelabs.ext.agent.llm.SecretRedactor;
import io.karatelabs.ext.common.MaxPaths;
import io.karatelabs.ext.common.server.EventPublisher;
import io.karatelabs.ext.common.jsapi.JsApi;
import io.karatelabs.ext.common.jsapi.EvaluationMessageScope;
import io.karatelabs.ext.common.run.JsNamespaceProvider;
import io.karatelabs.ext.common.run.EngineRuntimeProfile;
import io.karatelabs.ext.coverage.Coverage;
import io.karatelabs.ext.coverage.CoverageExt;
import io.karatelabs.ext.coverage.AssertionEvidenceRecorder;
import io.karatelabs.ext.http.jsapi.Http;
import io.karatelabs.ext.max.apitest.ChatStore;
import io.karatelabs.ext.max.apitest.LiveSessions;
import io.karatelabs.ext.max.apitest.TriageGate;
import io.karatelabs.ext.max.jsapi.ActiveRuns;
import io.karatelabs.ext.max.jsapi.ApiChat;
import io.karatelabs.ext.max.jsapi.ApiTest;
import io.karatelabs.ext.max.jsapi.Campaign;
import io.karatelabs.ext.max.jsapi.Check;
import io.karatelabs.ext.max.jsapi.ConsoleState;
import io.karatelabs.ext.max.jsapi.File;
import io.karatelabs.ext.max.jsapi.Graph;
import io.karatelabs.ext.max.jsapi.Harness;
import io.karatelabs.ext.max.jsapi.Help;
import io.karatelabs.ext.max.jsapi.JsEngine;
import io.karatelabs.ext.max.jsapi.OverlayCandidateRunner;
import io.karatelabs.ext.max.jsapi.Postman;
import io.karatelabs.ext.max.jsapi.Recording;
import io.karatelabs.ext.max.jsapi.Report;
import io.karatelabs.ext.max.jsapi.Runner;
import io.karatelabs.ext.max.jsapi.Secret;
import io.karatelabs.ext.max.jsapi.Settings;
import io.karatelabs.ext.max.jsapi.Skill;
import io.karatelabs.ext.max.jsapi.Traffic;
import io.karatelabs.ext.max.jsapi.View;
import io.karatelabs.ext.max.jsapi.Work;
import io.karatelabs.ext.mcp.Mcp;
import io.karatelabs.ext.openapi.jsapi.Openapi;
import io.karatelabs.ext.pair.jsapi.Contract;
import io.karatelabs.ext.requirements.RequirementsExt;
import io.karatelabs.ext.requirements.jsapi.Requirement;
import io.karatelabs.ext.robot.Bot;
import io.karatelabs.ext.robot.MatchBinding;
import io.karatelabs.ext.robot.RobotFactory;
import io.karatelabs.ext.rules.Rule;
import io.karatelabs.ext.rules.Schema;
import io.karatelabs.ext.rules.twin.Twin;
import io.karatelabs.js.Engine;
import io.karatelabs.js.JavaInvokable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntSupplier;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class MaxProfile implements EngineRuntimeProfile {
   private static final Logger logger = LoggerFactory.getLogger(MaxProfile.class);
   private final Path root;
   private volatile Path classpathRoot;
   private final Path reportDir;
   private Path suiteOutputDir;
   private final Path rulesHome;
   private final List<Path> requirementsHomes;
   private final boolean requirementsHomesExplicit;
   private final EventPublisher publisher;
   private final MaxWorkspace workspace;
   private Harness harness;
   private final ConsoleState console = new ConsoleState(io.karatelabs.ext.common.secrets.SecretStore.a());
   private final LiveSessions sessions;
   private final ActiveRuns activeRuns = new ActiveRuns();
   private final Rule rule;
   private final Twin twin;
   private final Schema schema;
   private final Requirement requirement;
   private final Report report;
   private final Help help = new Help();
   private Check check;
   private Runner runner;
   private Campaign campaign;
   private Postman postman;
   private Traffic traffic;
   private final AtomicReference<Bot> liveBot = new AtomicReference<>();
   private volatile long lastLiveActivityMs = System.currentTimeMillis();
   private final long idleTimeoutMs = liveIdleTimeoutMs();
   private volatile ScheduledExecutorService idleWatchdog;
   private volatile SecretRedactor sanitizer;
   private static final String DEFAULT_REQUIREMENTS_SUBDIR = "requirements";
   private static final String DEFAULT_REPORT_SUBDIR = "target/karate-reports";
   private volatile Function<String, Object> reanchorFn = var0 -> Map.of("error", "re-anchor is not available (no serve host bound)");
   private volatile BiConsumer<String, String> viewSink = (var0, var1x) -> {};
   private volatile IntSupplier servePort = () -> -1;
   private volatile Supplier<Path> uploadRootSupplier;
   private Path chatDir;
   private Path secretsDir;
   private TriageGate triage;
   private volatile io.karatelabs.ext.common.EvidenceContext.EvidenceEventSink priorOwner;
   private final ThreadLocal<String> evalRun = new ThreadLocal<>();
   volatile int reaps;

   private SecretRedactor sanitizer() {
      SecretRedactor var1 = this.sanitizer;
      if (var1 == null) {
         this.sanitizer = var1 = new SecretRedactor(() -> this.console.store().a(this.configDirs(), System::getenv));
         this.console.withMask(var1);
      }

      return var1;
   }

   private List<Path> configDirs() {
      Path var1 = this.classpathRoot();
      return var1.equals(this.root) ? List.of(this.root) : List.of(this.root, var1);
   }

   private Path classpathRoot() {
      Path var1 = this.classpathRoot;
      if (var1 != null) {
         return var1;
      } else {
         Path var2 = this.root;

         try {
            BootBinding var3 = BootLoader.bootOnly(this.root, null);
            String var4 = var3 == null ? null : var3.getClasspathDir();
            if (var4 != null) {
               var2 = this.root.resolve(var4).normalize();
            }
         } catch (RuntimeException var5) {
            logger.debug("could not read boot.classpath for {}: {}", this.root, var5.getMessage());
         }

         this.classpathRoot = var2;
         return var2;
      }
   }

   private static List<Path> derivedRequirementsHomes(Path var0) {
      Path var1 = var0.resolve("requirements");
      if (!Files.exists(var0.resolve("karate-boot.js"))) {
         return List.of(var1);
      } else {
         try {
            BootBinding var2 = BootLoader.bootOnly(var0, null);
            if (var2 == null) {
               return List.of(var1);
            }

            Object var3 = null;
            Object var4 = null;

            for (Ext var6 : var2.getExts()) {
               if (var6 instanceof RequirementsExt var7 && var7.getHome() != null) {
                  var3 = var7.getHome();
               }

               if (var6 instanceof CoverageExt var10 && var10.getRequirements() != null) {
                  var4 = var10.getRequirements();
               }
            }

            Object var9 = var3 != null ? var3 : var4;
            if (var9 != null) {
               return RequirementsExt.resolveHomes(var0, var9, "requirements");
            }
         } catch (RuntimeException var8) {
            logger.debug("could not read the requirements home from karate-boot.js at {}: {}", var0, var8.getMessage());
         }

         return List.of(var1);
      }
   }

   private static long liveIdleTimeoutMs() {
      String var0 = System.getenv("KARATE_LIVE_IDLE_MINUTES");
      if (var0 != null && !var0.isBlank()) {
         try {
            return Math.max(0L, (long)(Double.parseDouble(var0.trim()) * 60000.0));
         } catch (NumberFormatException var2) {
            return 1800000L;
         }
      } else {
         return 1800000L;
      }
   }

   public MaxProfile(Path var1, Path var2, EventPublisher var3) {
      this(MaxWorkspace.at(var1), var2, var3);
   }

   public MaxProfile(Path var1, Path var2, Path var3, Path var4, EventPublisher var5) {
      this(MaxWorkspace.at(var1), var2, var3, var4, var5);
   }

   public MaxProfile(Path var1, Path var2, Path var3, List<Path> var4, EventPublisher var5) {
      this(MaxWorkspace.at(var1), var2, var3, var4, var5);
   }

   public MaxProfile(MaxWorkspace var1, Path var2, EventPublisher var3) {
      this(var1, var2, var1.rulebooksDir(), List.of(), var3);
   }

   public MaxProfile(MaxWorkspace var1, Path var2, Path var3, Path var4, EventPublisher var5) {
      this(var1, var2, var3, var4 == null ? List.of() : List.of(var4), var5);
   }

   public MaxProfile(MaxWorkspace var1, Path var2, Path var3, List<Path> var4, EventPublisher var5) {
      io.karatelabs.ext.mutation.RuleMutationAdapter.install();
      this.workspace = var1;
      this.root = var1.root();
      this.sessions = new LiveSessions(
         this.console, this.root, this::configDirs, () -> this.console.store().a(this.configDirs(), System::getenv), LiveSessions.Settings.fromEnv()
      );
      this.reportDir = var2;
      this.rulesHome = var3;
      this.requirementsHomesExplicit = !var4.isEmpty();
      this.requirementsHomes = this.requirementsHomesExplicit ? List.copyOf(var4) : derivedRequirementsHomes(this.root);
      this.publisher = var5;
      this.rule = Rule.atRulesHome(var3)
         .withRoot(this.root)
         .withReportDir(var2)
         .withRunPrep(() -> this.console.kickoff(null))
         .withRequirementsHomes(this.requirementsHomes);
      this.twin = Twin.atRulesHome(var3).withRoot(this.root).withMocks(io.karatelabs.ext.http.HttpMockFactory.a);
      this.schema = Schema.atRulesHome(var3);
      this.report = Report.atReportDir(var2).withRoot(this.root).withOracle(this.rule::producer);
      this.rule.withProjectBuilder(this.report::snapshot);
      this.harness = Harness.at(this.root, var5).withActiveRuns(this.activeRuns).withRulesHomes(var3, var2, this.requirementsHomes, this.report::snapshot);
      this.requirement = Requirement.atReportDir(var2)
         .withRequirementsHomes(this.requirementsHomes)
         .withProjectRoot(this.root)
         .withRunsDir(this.root.resolve("runs"))
         .withScopeBuilder(this.report::composeSide);
   }

   public MaxWorkspace workspace() {
      return this.workspace;
   }

   public List<Path> requirementsHomes() {
      return this.requirementsHomes;
   }

   private List<String> requirementsHomeRefs() {
      Path var1 = this.root.toAbsolutePath().normalize();
      ArrayList var2 = new ArrayList();

      for (Path var4 : this.requirementsHomes) {
         var2.add(var1.relativize(var4.toAbsolutePath().normalize()).toString().replace('\\', '/'));
      }

      return var2;
   }

   public MaxProfile atRoot(Path var1) {
      List var2 = this.requirementsHomesExplicit ? this.requirementsHomes.stream().map(var2x -> this.rebase(var2x, var1)).toList() : List.of();
      return new MaxProfile(this.workspace.reanchor(var1), var1.resolve("target/karate-reports"), this.rebase(this.rulesHome, var1), var2, this.publisher)
         .withChatDir(this.chatDir)
         .withSecretsDir(this.secretsDir);
   }

   private Path rebase(Path var1, Path var2) {
      Path var3 = this.root.toAbsolutePath().normalize();
      Path var4 = var1.toAbsolutePath().normalize();
      return var4.startsWith(var3) ? var2.resolve(var3.relativize(var4)).normalize() : var1;
   }

   public void bindReanchorFn(Function<String, Object> var1) {
      this.reanchorFn = var1;
   }

   public void bindViewSink(BiConsumer<String, String> var1) {
      this.viewSink = var1;
   }

   private void settleAndAnnounce(String var1) {
      var var2 = this.report.settle(var1);
      if (var2 != null && !var2.containsKey("error") && !Boolean.TRUE.equals(var2.get("superseded"))) {
         LinkedHashMap var3 = new LinkedHashMap();
         var3.put("type", "coverage");
         var3.put("runId", var1);
         var3.put("ts", System.currentTimeMillis());
         this.viewSink.accept("coverage", Json.stringifyStrict(var3));
      }
   }

   public void bindServePort(IntSupplier var1) {
      this.servePort = var1;
   }

   public void bindUploadRootSupplier(Supplier<Path> var1) {
      if (var1 != null) {
         this.uploadRootSupplier = var1;
      }
   }

   @Override
   public String reanchorBlockedReason() {
      return this.harness.runBusyReason();
   }

   @Override
   public void stop() {
      this.sessions.shutdown();
   }

   @Override
   public String name() {
      return "console";
   }

   public MaxProfile withHarness(Harness var1) {
      this.harness = var1.withActiveRuns(this.activeRuns).withRulesHomes(this.rulesHome, this.reportDir, this.requirementsHomes, this.report::snapshot);
      return this;
   }

   public MaxProfile withSuiteOutputDir(Path var1) {
      this.suiteOutputDir = var1;
      return this;
   }

   public MaxProfile withChatDir(Path var1) {
      this.chatDir = var1;
      return this;
   }

   public MaxProfile withSecretsDir(Path var1) {
      if (var1 != null) {
         this.console.useStore(new io.karatelabs.ext.common.secrets.SecretStore(var1));
      }

      this.secretsDir = var1;
      return this;
   }

   @Override
   public void apply(Engine var1) {
      EngineRuntimeProfile.super.apply(var1);
      File var2 = File.at(this.root);
      var1.putRootBinding(
         "read",
         (JavaInvokable)var1x -> {
            String var2x = JsApi.str(var1x, 0);
            if (var2x != null && !var2x.isBlank()) {
               io.karatelabs.ext.common.jsapi.EvaluationMessageScope.a(
                  "`read('"
                     + var2x
                     + "')` is the console alias for `File.read(path)` (JSON auto-parsed). File.* is the native file API here — it also lists (File.list), checks (File.exists), and guards writes (File.write)."
               );
               return var2.readParsed(var2x);
            } else {
               return JsApi.missing("read(path) needs a project-relative path — this is the console alias for File.read(path)");
            }
         }
      );
   }

   @Override
   public List<io.karatelabs.ext.common.run.NamespaceBinding> namespaces(Engine var1) {
      ConcurrentHashMap var2 = new ConcurrentHashMap();
      RobotFactory var3 = new RobotFactory(var1x -> this.streamLiveLog(this.harness.liveRunId(), var1x));
      var3.onCreateGuard(() -> {
         io.karatelabs.ext.common.keycheck.Main.requireProduct("robot", "robot");
         return this.harness.createBusyReason();
      });
      var3.onActiveBot(this.liveBot::get);
      this.harness.onLiveBotAlive(() -> {
         Bot var1x = this.liveBot.get();
         return var1x == null ? null : var1x.isAlive();
      });
      var3.onCreate(var2x -> {
         String var3x = this.harness.ownedLease();
         String var4x = var3x != null ? var3x : this.harness.startLive();
         if (var3x == null) {
            var2.put(var2x.getId(), var4x);
         }

         this.liveBot.set(var2x);
         this.touchLiveActivity();
         this.startIdleWatchdog();
         var2x.getAgentDriver().setScreenshotsDirSupplier(() -> {
            String var2xx = this.harness.liveRunId();
            return this.harness.runDir(var2xx != null ? var2xx : var4x).resolve("screenshots");
         });
         var2x.getAgentDriver().setScreenshotUrlResolver(var2xx -> {
            String var3xx = this.harness.liveRunId();
            return "/api/artifacts/runs/" + (var3xx != null ? var3xx : var4x) + "/screenshots/" + var2xx;
         });
         var2x.getAgentDriver().setUploadFileResolver(var1xx -> {
            Supplier var2xx = this.uploadRootSupplier;
            Path var3xx = var2xx != null ? (Path)var2xx.get() : this.root;
            if (var3xx == null) {
               var3xx = this.root;
            }

            return MaxPaths.resolveSandboxed(var3xx, var1xx).toString();
         });
         var2x.getAgentDriver().setScreenshotSavedListener((var1xx, var2xx) -> {
            LinkedHashMap var3xx = new LinkedHashMap();
            var3xx.put("filename", var1xx);
            if (var2xx != null) {
               var3xx.put("title", var2xx);
            }

            this.harness.recordEvent("screenshot", var3xx);
         });
         var2x.getAgentDriver().setHealListener((var1xx, var2xx, var3xx, var4xx) -> {
            LinkedHashMap var5x = new LinkedHashMap();
            var5x.put("from", var1xx);
            var5x.put("to", var2xx);
            var5x.put("action", var3xx);
            if (var4xx != null) {
               var5x.put("score", var4xx);
            }

            this.harness.recordEvent("healed", var5x);
         });
      });
      var3.onDestroy(var2x -> {
         String var3x = (String)var2.remove(var2x.getId());
         this.liveBot.compareAndSet(var2x, null);
         if (var3x != null && var3x.equals(this.harness.liveRunId())) {
            this.harness.settleLive("completed");
         }
      });
      this.harness.onLeasedMemberEnd(() -> {
         Bot var1x = this.liveBot.get();
         if (var1x != null) {
            var1x.reap();
         }
      });
      this.harness.onSettleLive(() -> {
         for (String var2x : new ArrayList<>(var1.getBindings().keySet())) {
            var1.getBindings().remove(var2x);
         }
      });
      io.karatelabs.ext.rules.Rule.StepListener var4 = (var1x, var2x, var3x) -> {
         LinkedHashMap var4x = new LinkedHashMap();
         var4x.put("name", var1x);
         if (var2x != null) {
            var4x.put("status", var2x);
         }

         if (var3x != null) {
            var4x.put("note", var3x);
         }

         this.harness.recordEvent("step", var4x);
      };
      var3.onStep(var4::step);
      this.rule.onStep(var4);
      MatchBinding var5 = new MatchBinding().onAssertion(var2x -> {
         Object var3x = var2x.get("label");
         if (var3x != null) {
            var4.step(String.valueOf(var3x), Boolean.TRUE.equals(var2x.get("pass")) ? "pass" : "fail", null);
         }

         var var4x = io.karatelabs.ext.coverage.AssertionEvidenceRecorder.a(var2x);
         var4x.remove("label");
         this.harness.recordEvent("assertion", var4x);
      });
      Http var6 = new Http().withRulesHome(this.rulesHome).withRoot(this.root, this.classpathRoot());
      this.harness.liveEvidenceCapture(var6::captureStart, var6::captureDrain);
      Consumer<String> var7 = this.suiteOutputDir != null ? this.report::aggregateRunInPlace : this::settleAndAnnounce;
      this.harness.onCheckGraph(var7);
      this.harness.onCloseBrowser(() -> {
         Bot var1x = this.liveBot.get();
         if (var1x != null) {
            var1x.reap();
         }
      });
      this.triage = new TriageGate(this.root, this.chatDir == null ? ChatStore.server() : ChatStore.at(this.chatDir))
         .withSanitizer(this.sanitizer())
         .withSessions(this.sessions)
         .withModelConfigured(this.harness::modelConfigured);
      this.harness.withTriage(this.triage);
      File var8 = File.at(this.root).withNoProject(this.workspace.noProject()).withEngine(var1).withConsole(this.console).withTriage(this.triage);
      Check var9 = Check.at(this.workspace.checksDir(), var1).withFile(var8).withBusy(this.harness::runWorkingOn).withConsole(this.console);
      var9.onRun(var2x -> {
         this.ensureLiveRunForCheck();
         this.harness.beginExecution(Check.definitionId(var2x));
         this.priorOwner = io.karatelabs.ext.common.EvidenceContext.a(this.harness.ownerSink());
         this.harness.stampLiveCheck(var2x);
         var6.captureStart();
      }).onSettle((var3x, var4x, var5x, var7x) -> {
         io.karatelabs.ext.common.EvidenceContext.a(this.priorOwner);
         this.priorOwner = null;
         return this.harness.recordCheckOutcome(var3x, var4x, var5x, var6.captureDrain(), var9.linkTags(var3x), null, var7x);
      });
      this.check = var9;
      LinkedHashMap var10 = new LinkedHashMap();
      var10.put(
         "Openapi",
         io.karatelabs.ext.common.run.NamespaceBinding.a(
            "Openapi",
            new Openapi()
               .withRoot(this.root, this.classpathRoot())
               .withNoProject(this.workspace.noProject())
               .withReportDir(this.reportDir)
               .withRulesHome(this.rulesHome)
         )
      );
      var10.put(
         "Asyncapi",
         io.karatelabs.ext.common.run.NamespaceBinding.a(
            "Asyncapi",
            new Asyncapi().withRoot(this.root, this.classpathRoot()).withNoProject(this.workspace.noProject()),
            "Author from an AsyncAPI (event) doc — Asyncapi.scaffold('asyncapi.yml', {out}) writes a runnable Kafka coverage project (channel×direction universe + payload field axes). Asyncapi.help(). (async jar only — needs the kafka leaf.)"
         )
      );
      var10.put(
         "Contract",
         io.karatelabs.ext.common.run.NamespaceBinding.a(
            "Contract",
            new Contract()
               .withRoot(this.root, this.classpathRoot())
               .withReportDir(this.reportDir)
               .withConsole(this.console)
               .withMocks(io.karatelabs.ext.http.HttpMockFactory.a),
            "Contract testing as ONE suite against TWO targets — Contract.pair({suite, spec, provider}) mints a pair (the provider leg runs its own mock leg beside it) and writes the measured divergence set as a dated, committable file; Contract.read(file, {asOf}) derives its freshness at read time; Contract.report(file) renders that same reading as a page in the report dir. Contract.help()."
         )
      );
      var10.put("Http", io.karatelabs.ext.common.run.NamespaceBinding.a("Http", var6));
      var10.put("Mcp", io.karatelabs.ext.common.run.NamespaceBinding.a("Mcp", new Mcp()));
      var10.put(
         "Robot",
         io.karatelabs.ext.common.run.NamespaceBinding.a(
            "Robot",
            var3,
            "Browser agent — live mode: run `var bot = Robot.create()` then drive it (bot.go/look/act/…). Robot.help() for the topic list. (Focused prompt: GET /api/prompts/browser.)"
         )
      );
      var10.put("Harness", io.karatelabs.ext.common.run.NamespaceBinding.a("Harness", this.harness));
      Recording var11 = new Recording(
            this.harness, this.liveBot::get, var1x -> new LiveRunSink((var2x, var3x) -> this.streamLive(var1x, var2x, var3x)), this::touchLiveActivity
         )
         .withNoProject(this.workspace.noProject())
         .withCapture(this.console::store, this.sanitizer());
      var3.onBeforeClose(var11::endIfClosing);
      var10.put(
         "Recording",
         io.karatelabs.ext.common.run.NamespaceBinding.a(
            "Recording",
            var11,
            "Record a human UI flow + author a check from it — Recording.start([name]) captures every interaction as semantic human_action events on the run transcript; Recording.get([runId]) reads it back as authorable material (an LLM then writes the check via Skill.help('record-to-check')). Recording.note(text)/stop()/status()/help()."
         )
      );
      var10.put(
         "File",
         io.karatelabs.ext.common.run.NamespaceBinding.a(
            "File", var8, "Sandboxed project-dir files — author/iterate checks/*.js, read/write, File.call a project .js. File.help()."
         )
      );
      var10.put(
         "Check",
         io.karatelabs.ext.common.run.NamespaceBinding.a(
            "Check", this.check, "Run reusable checks/*.js macros (deterministic, no LLM). Check.run('name.js', data?). Check.help()."
         )
      );
      ApiTest var12 = ApiTest.at(this.root)
         .withNoProject(this.workspace.noProject())
         .withSanitizer(this.sanitizer())
         .withSessions(this.sessions)
         .withTriage(this.triage)
         .withSecrets(this.console::store);
      var10.put("ApiTest", io.karatelabs.ext.common.run.NamespaceBinding.a("ApiTest", var12));
      io.karatelabs.ext.common.secrets.SecretStore var13 = this.console.store();
      this.harness.withSanitizer(this.sanitizer()).withConsole(this.console);
      var10.put(
         "Secret",
         io.karatelabs.ext.common.run.NamespaceBinding.a(
            "Secret",
            new Secret(var13, this.root).withConfigDirs(this::configDirs).withNoProject(this.workspace.noProject()),
            "Credential values behind karate-config's secret('NAME') references, kept off git — Secret.list() (names only), Secret.set(name, value), Secret.delete(name). Secret.help()."
         )
      );
      var10.put(
         "match",
         io.karatelabs.ext.common.run.NamespaceBinding.a(
            "match", var5, "Assert: match(actual, expected, label?) or match.contains/each/within(...) — throws on mismatch. match.help()."
         )
      );
      var10.put("Coverage", io.karatelabs.ext.common.run.NamespaceBinding.a("Coverage", Coverage.atReportDir(this.reportDir)));
      var10.put(
         "Requirement",
         io.karatelabs.ext.common.run.NamespaceBinding.a(
            "Requirement",
            this.requirement,
            "Requirements traceability — Requirement.load('requirements/x.md') parses the markdown; matrix()/gaps() read the coverage graph. Requirement.help()."
         )
      );
      var10.put("Report", io.karatelabs.ext.common.run.NamespaceBinding.a("Report", this.report));
      Runner var14 = Runner.at(this.root, this.publisher, this.activeRuns, var1)
         .withConsole(this.console)
         .withSanitizer(this.sanitizer())
         .withSessions(this.sessions)
         .withTriage(this.triage);
      this.runner = var14;
      var12.withRunner(var14);
      ApiChat var15 = new ApiChat(this.harness, this.root, var12, var14, this.requirement).withSecrets(this.console::store);
      this.harness.withApiChat(var15);
      this.triage
         .withRunner(new OverlayCandidateRunner(var14, this.harness, this.root).withChatTurn(var15::answerTurn).withChatIds(var15::ids))
         .withReportTurn(var15::reportTurn);
      var14.withDraftGuard(var6::draftMockLive);
      this.harness.withDraftGuard(var6::draftMockLive);
      var14.withSlotLease(this.harness);
      var14.withServePort(() -> this.servePort.getAsInt());
      var14.withProjectGraph(this.reportDir.resolve("ext/coverage/coverage-data.json"));
      this.harness.withProjectGraph(this.reportDir.resolve("ext/coverage/coverage-data.json"));
      if (this.suiteOutputDir != null) {
         var14.withOutputDir(this.suiteOutputDir);
      } else {
         var14.withAfterRun(var7);
      }

      var10.put("Runner", io.karatelabs.ext.common.run.NamespaceBinding.a("Runner", var14));
      this.postman = new Postman(this.root, var14).withNoProject(this.workspace.noProject()).withSanitizer(this.sanitizer()).withConsole(this.console);
      var10.put("Postman", io.karatelabs.ext.common.run.NamespaceBinding.a("Postman", this.postman));
      this.traffic = new Traffic(this.root, this.harness::runDir).withConsole(() -> {
         int var1x = this.servePort.getAsInt();
         return var1x < 0 ? List.of() : List.of("http://localhost:" + var1x, "http://127.0.0.1:" + var1x);
      }).withApp(() -> var14.app(null).get("url") instanceof String var1x ? var1x : null).withSpecs(this.report::openapiSources);
      var10.put("Traffic", io.karatelabs.ext.common.run.NamespaceBinding.a("Traffic", this.traffic));
      var11.withEvidence((var1x, var2x) -> this.harness.queueRecordingEvidence(var1x, var2x, this.traffic.keptExchanges(var1x)));
      var15.withPostman(this.postman).withTraffic(this.traffic);
      this.campaign = new Campaign(this.root, var14)
         .withHarness(this.harness)
         .withGraphs(this.report::compose)
         .withRequirements(() -> this.requirement.list(null))
         .withNoProject(this.workspace.noProject());
      var10.put(
         "Campaign",
         io.karatelabs.ext.common.run.NamespaceBinding.a(
            "Campaign",
            this.campaign,
            "Named test sets launched together — Campaign.create({name, tests|req}), Campaign.run(name, {async:true}) (only:'failed' relaunches the reds), Campaign.get(name) (each test's latest verdict + the criteria its tests verify). Campaign.help()."
         )
      );
      var10.put("Graph", io.karatelabs.ext.common.run.NamespaceBinding.a("Graph", Graph.atReportDir(this.reportDir)));
      var10.put("Rule", io.karatelabs.ext.common.run.NamespaceBinding.a("Rule", this.rule));
      var10.put("Twin", io.karatelabs.ext.common.run.NamespaceBinding.a("Twin", this.twin));
      var10.put("Schema", io.karatelabs.ext.common.run.NamespaceBinding.a("Schema", this.schema));
      var10.put(
         "Skill",
         io.karatelabs.ext.common.run.NamespaceBinding.a(
            "Skill",
            new Skill(),
            "Developer Flows — task-framed end-to-end recipes: Skill.flows() for the menu (match the task to one), Skill.help(name) to open it, Skill.list() for the full catalog."
         )
      );
      var10.put("Engine", io.karatelabs.ext.common.run.NamespaceBinding.a("Engine", new JsEngine(var1)));
      Work var16 = new Work(this::workspace, var1x -> this.reanchorFn.apply(var1x), () -> this.reportDir, this::requirementsHomes, () -> this.rulesHome)
         .withConsole(this.console);
      this.harness.withProjectBoot(() -> var16.boot(null));
      var10.put(
         "Work",
         io.karatelabs.ext.common.run.NamespaceBinding.a(
            "Work",
            var16,
            "Project workspace — Work.current() (the active project + its content dirs), Work.dirs() (where authored files go), Work.list() (the library under the /work ceiling), Work.browse(dir?), Work.use(path) (re-anchor the server). Work.help()."
         )
      );
      var10.put(
         "Help",
         io.karatelabs.ext.common.run.NamespaceBinding.a(
            "Help",
            this.help,
            "In-app product Help + datasheet — Help.pages() / Help.page('rules'|'datasheet'); Help.surface(id) navigates the human's view to /help#<id>. Help.topics() for the id index."
         )
      );
      var10.put(
         "View",
         io.karatelabs.ext.common.run.NamespaceBinding.a(
            "View",
            new View().withSink((var1x, var2x) -> this.viewSink.accept(var1x, var2x)),
            "Console map + steering — View.map()/sections('rules')/resolve('rules.reference.code') read the page/section registry; View.navigate(id)/highlight(id)/callout(id,text)/overlay({title,body}) steer the human's view (P27). View.help()."
         )
      );
      var10.put(
         "Settings",
         io.karatelabs.ext.common.run.NamespaceBinding.a(
            "Settings",
            new Settings(),
            "Read-only instance status — Settings.view() (license + LLM config), Settings.license() (licensee/products/days-left), Settings.llm() (provider key presence, masked). Also the config door a script parameterises through: Settings.sysenv(name[, default]) / Settings.sysprop(name[, default]). Settings.help()."
         )
      );

      for (JsNamespaceProvider var18 : JsNamespaceProvider.discover()) {
         var10.putIfAbsent(var18.name(), io.karatelabs.ext.common.run.NamespaceBinding.a(var18.name(), var18.namespace(this.root)));
      }

      var var22 = List.of(
         "Openapi",
         "Asyncapi",
         "Http",
         "Mcp",
         "Robot",
         "Harness",
         "Recording",
         "Traffic",
         "File",
         "Check",
         "ApiTest",
         "Postman",
         "Secret",
         "match",
         "Coverage",
         "Requirement",
         "Report",
         "Runner",
         "Campaign",
         "Graph",
         "Rule",
         "Twin",
         "Schema",
         "Skill",
         "Engine",
         "Work",
         "Help",
         "View",
         "Settings"
      );
      ArrayList var23 = new ArrayList();

      for (String var20 : (Iterable<String>) (Iterable<?>) (var22)) {
         io.karatelabs.ext.common.run.NamespaceBinding var21 = (io.karatelabs.ext.common.run.NamespaceBinding)var10.remove(var20);
         if (var21 != null) {
            var23.add(var21);
         }
      }

      var23.addAll(var10.values());
      return var23;
   }

   private void ensureLiveRunForCheck() {
      if (this.harness.liveRunId() == null && this.harness.leasedRunId() == null) {
         if (Boolean.TRUE.equals(this.harness.liveBotAlive())) {
            try {
               this.harness.startLive();
            } catch (RuntimeException var2) {
               logger.debug("could not claim a live run for the check: {}", var2.getMessage());
            }
         }
      }
   }

   private void streamLiveLog(String var1, String var2) {
      if (var1 != null && var2 != null && !var2.isBlank()) {
         LinkedHashMap var3 = new LinkedHashMap();
         var3.put("type", "log");
         var3.put("text", var2);
         var3.put("ts", System.currentTimeMillis());
         this.streamLive(var1, "log", Json.stringifyStrict(var3));
      }
   }

   @Override
   public void onEvalStart(String var1) {
      this.touchLiveActivity();
      String var2 = this.harness.liveRunId();
      this.evalRun.set(var2);
      if (var2 != null) {
         LinkedHashMap var3 = new LinkedHashMap();
         var3.put("type", "eval_start");
         var3.put("request", var1);
         var3.put("ts", System.currentTimeMillis());
         this.streamLive(var2, "eval_start", Json.stringifyStrict(var3));
      }
   }

   @Override
   public void onEval(String var1, String var2) {
      String var3 = this.harness.liveRunId();
      if (var3 == null) {
         var3 = this.evalRun.get();
      }

      this.evalRun.remove();
      if (var3 != null) {
         LinkedHashMap var4 = new LinkedHashMap();
         var4.put("type", "eval");
         var4.put("request", var1);
         var4.put("response", Json.of(var2).value());
         var4.put("ts", System.currentTimeMillis());
         this.streamLive(var3, "eval", Json.stringifyStrict(var4));
      }
   }

   private boolean streamLive(String var1, String var2, String var3) {
      return this.harness.transcript(var1, var2, var3);
   }

   private void touchLiveActivity() {
      this.lastLiveActivityMs = System.currentTimeMillis();
   }

   private synchronized void startIdleWatchdog() {
      if (this.idleTimeoutMs > 0L && this.idleWatchdog == null) {
         this.idleWatchdog = Executors.newSingleThreadScheduledExecutor(var0 -> {
            Thread var1x = new Thread(var0, "max-live-idle-reaper");
            var1x.setDaemon(true);
            return var1x;
         });
         long var1 = Math.max(5000L, Math.min(this.idleTimeoutMs, 60000L));
         this.idleWatchdog.scheduleWithFixedDelay(this::reapIfIdle, var1, var1, TimeUnit.MILLISECONDS);
      }
   }

   private void reapIfIdle() {
      this.reapIfIdle(System.currentTimeMillis());
   }

   void reapIfIdle(long var1) {
      try {
         this.harness.lock().lock();

         try {
            String var3 = this.harness.liveRunId();
            if (var3 == null) {
               return;
            }

            if (this.harness.executionOpen()) {
               return;
            }

            long var4 = var1 - this.lastLiveActivityMs;
            if (var4 >= this.idleTimeoutMs) {
               this.reaps++;
               Bot var6 = this.liveBot.get();
               logger.info("live run {} idle for {}ms (limit {}ms) — auto-settling", new Object[]{var3, var4, this.idleTimeoutMs});
               if (var6 != null && var6.isAlive()) {
                  var6.reap();
                  return;
               }

               this.harness.settleLive("completed");
               return;
            }
         } finally {
            this.harness.lock().unlock();
         }
      } catch (Exception var11) {
         logger.warn("idle reaper failed: {}", var11.getMessage());
      }
   }

   @Override
   public Map<String, Object> templateGlobals() {
      LinkedHashMap var1 = new LinkedHashMap();
      var1.put("harness", this.harness);
      var1.put("file", File.at(this.root).withNoProject(this.workspace.noProject()).withTriage(this.triage));
      if (this.check != null) {
         var1.put("check", this.check);
      }

      var1.put("rule", this.rule);
      var1.put(
         "apiTest",
         ApiTest.at(this.root)
            .withNoProject(this.workspace.noProject())
            .withSanitizer(this.sanitizer())
            .withSessions(this.sessions)
            .withTriage(this.triage)
            .withSecrets(this.console::store)
      );
      var1.put("secret", new Secret(this.console.store(), this.root).withConfigDirs(this::configDirs).withNoProject(this.workspace.noProject()));
      if (this.postman != null) {
         var1.put("postman", this.postman);
      }

      if (this.traffic != null) {
         var1.put("traffic", this.traffic);
      }

      var1.put("twin", new TwinPane(this.twin, new PlayRecordInspector(this.twin, this.rulesHome, this.root, this::requirementsHomes)));
      var1.put("schema", this.schema);
      var1.put("requirement", this.requirement);
      var1.put("coverage", Coverage.atReportDir(this.reportDir));
      var1.put("requirementsHomes", this.requirementsHomeRefs());
      var1.put("report", this.report);
      var1.put("help", this.help.forConsole());
      var1.put(
         "work",
         new Work(this::workspace, var1x -> this.reanchorFn.apply(var1x), () -> this.reportDir, this::requirementsHomes, () -> this.rulesHome)
            .withConsole(this.console)
      );
      var1.put("view", new View());
      var1.put(
         "openapi",
         new Openapi()
            .withRoot(this.root, this.classpathRoot())
            .withNoProject(this.workspace.noProject())
            .withReportDir(this.reportDir)
            .withRulesHome(this.rulesHome)
      );
      var1.put("settings", new Settings());
      if (this.runner != null) {
         var1.put("runner", new RunnerPane(this.runner));
      }

      if (this.campaign != null) {
         var1.put("campaign", new CampaignPane(this.campaign));
      }

      var1.put("rail", new RailPane(this.runner, this.harness, this.campaign));
      var1.put("recordings", new RecordingsPane(this.harness));
      return var1;
   }

   @Override
   public Map<String, Path> artifactRoots() {
      LinkedHashMap var1 = new LinkedHashMap();
      var1.put("runs", this.root.resolve("runs"));
      var1.put("reports", this.reportDir);
      return var1;
   }
}
