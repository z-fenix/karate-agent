package io.karatelabs.ext.max.jsapi;

import io.karatelabs.common.Json;
import io.karatelabs.core.SuiteResult;
import io.karatelabs.ext.agent.llm.SecretRedactor;
import io.karatelabs.ext.common.MaxPaths;
import io.karatelabs.ext.common.jsapi.JsApi;
import io.karatelabs.ext.common.jsapi.JsDispatch;
import io.karatelabs.ext.coverage.CoverageProjectBuilder;
import io.karatelabs.ext.coverage.collect.EvidenceRunMetadata;
import io.karatelabs.ext.max.apitest.TriageGate;
import io.karatelabs.ext.max.run.HarnessSseBridge;
import io.karatelabs.ext.max.run.LiveRunSink;
import io.karatelabs.ext.max.run.MaxWorkspace;
import io.karatelabs.ext.robot.BotDriver;
import io.karatelabs.ext.robot.RobotHarnessFactory;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TreeSet;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.BiFunction;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Harness extends JsDispatch {
   private static final Logger logger = LoggerFactory.getLogger(Harness.class);
   private final Path workingDir;
   private final HarnessRun run;
   private final RunMetadataStore store;
   private final Map<String, Thread> asyncThreads = new ConcurrentHashMap<>();
   private volatile String liveRunId;
   private volatile String leasedRunId;
   private final InheritableThreadLocal<String> leaseOwner = new InheritableThreadLocal<>();
   private Runnable onLeasedMemberEnd;
   private volatile String lastCheckRunId;
   private Runnable onSettleLive;
   private volatile boolean clearedVars;
   private Runnable onRecordingEnd;
   private Runnable liveCaptureStart;
   private Supplier<List<Map<String, Object>>> liveCaptureDrain;
   private Consumer<String> checkGraphBuilder;
   private final List<Map<String, Object>> pendingLiveEvidence = new ArrayList<>();
   private final ReentrantLock lock = new ReentrantLock();
   private final List<Map<String, Object>> sessionExchanges = new ArrayList<>();
   private Supplier<Boolean> liveBotAlive;
   private volatile io.karatelabs.ext.max.jsapi.Harness.ExecutionEvidenceContext bracket;
   private int executionSeq;
   private Runnable closeBrowser;
   private volatile Process liveVideo;
   private volatile boolean recording;
   private volatile BotDriver interventionBot;
   private volatile LiveRunSink interventionSink;
   private final BiFunction<io.karatelabs.ext.agent.harness.AgentHarness.Builder, String, io.karatelabs.ext.agent.harness.HarnessResult> executor;
   private final io.karatelabs.ext.common.server.EventPublisher publisher;
   private ActiveRuns activeRuns;
   private Path rulesHome;
   private Path rulesReportDir;
   private List<Path> requirementsHomes;
   private Function<Path, io.karatelabs.ext.coverage.CoverageProjectBuilder.CoverageProjectBuildResult> projectBuilder;
   static final List<String> DIAGNOSABLE_STATUSES = List.of("failed", "interrupted", "terminated", "stuck");
   private static final int MAX_SEEDED_RULES_CHARS = 40000;
   private static final int MAX_SEEDED_STEPS_CHARS = 20000;
   private static final int MAX_SEEDED_STEPLOG_CHARS = 12000;
   private static final Pattern RULEBOOK_REF = Pattern.compile(
      "@cov=rules:([A-Za-z0-9_.-]+)|Rule\\.(?:execute|load|run|audit|check)\\(\\s*\\\\?['\"]([A-Za-z0-9_.-]+)"
   );
   private volatile Supplier<Object> projectBoot;
   private volatile UnaryOperator<String> sanitizer = new SecretRedactor(Map::of);
   private volatile ConsoleState console = new ConsoleState(null);
   private volatile TriageGate triage;
   private volatile ApiChat apiChat;
   private volatile Function<String, String> env = System::getenv;
   private volatile BooleanSupplier modelProbe;
   private volatile Path projectGraph;
   private static final List<String> LOOP_MODES = List.of("browser", "headless", "loop");
   private static final List<String> REQUIRES_KEYS = List.of("properties", "scenarios", "rejects", "complete", "maxProduct", "mayBlock", "keepWitnesses");
   private static final double MAX_EXACT_INTEGER = 9.007199E15F;
   static final int REVISE_REQUEST_MAX = 1000;
   private final Map<String, Boolean> recordableByTranscript = Collections.synchronizedMap(new LinkedHashMap<String, Boolean>(64, 0.75F, true) {
      @Override
      protected boolean removeEldestEntry(Entry<String, Boolean> var1) {
         return this.size() > 4096;
      }
   });
   static final int DRAFT_MAX_CHARS = 12000;
   long draftDeadlineMs = 180000L;
   private static final AtomicBoolean DRAFTING = new AtomicBoolean();
   static final String DRAFT_PROMPT = "Rewrite the requirements draft below in the karate requirements markdown format. Answer with the markdown only: no preamble, no code fence.\n\nEach requirement:\n#### <ID>: <what must be true, in one line>\n@criticality=<high|medium|low>\n\n**Acceptance:**\n- 1: WHEN <trigger> THE SYSTEM SHALL <observable response>\n\nRules:\n- Keep every requirement the draft holds and its meaning; add none it does not ask for. Keep the author's words where they are clear.\n- Ids unique: keep the draft's, renumber a repeated one to the next free number of its prefix, never use a taken id.\n- Keep a @criticality the author gave; where the draft gives none, write no @criticality line. Keep the draft's other @key=value tags; never write @status=approved.\n- One EARS line per acceptance criterion, specific and checkable from outside the system. Where a criterion is a placeholder or missing, write the ones the title implies.\n- Keep a \"# \" title line the draft has.\n";
   private static final int TAKEN_RUN = 3;
   private static final Pattern NUMBERED = Pattern.compile("(.*?)(\\d{1,18})");
   private static final Pattern THINK = Pattern.compile("(?s)<think>.*?</think>");
   private static final Pattern FENCE = Pattern.compile("(?m)^`{3,}([^`\\n]*)$");
   private static final Pattern FIRST_HEADING = Pattern.compile("(?m)^#{1,6}\\s");
   private static final Pattern SUB_HEADING = Pattern.compile("(?m)^#{2,6}[ \\t]+(.*)$");
   private static final Pattern APPROVED = Pattern.compile("(?i)@status=approved(?=\\s|$)");
   private static final Pattern LEGACY_TAGS = Pattern.compile("(?i)^(\\s*-\\s*tags:\\s*)(.*)$");
   private static final Pattern AI_MARK = Pattern.compile("(^|\\s)@ai=\\S");
   private static final Pattern AI_TAG = Pattern.compile("(?i)(^|\\s)@ai=\\S*");
   private static final Pattern REQ_TOKEN = Pattern.compile("@req=([A-Za-z0-9._\\-:/{},]+)");
   private static final String CAPTURE_MARKER = "{\"type\":\"capture\",\"assertions\":true}";
   private Supplier<Boolean> draftGuard;
   private static final String AI_REVIEW = "human review pending; a human commit clears this line";

   public Harness(Path var1) {
      this(var1, null);
   }

   public Harness(Path var1, io.karatelabs.ext.common.server.EventPublisher var2) {
      this(var1, var2, new HarnessRun(), io.karatelabs.ext.agent.harness.AgentHarness.Builder::run);
   }

   Harness(Path var1, io.karatelabs.ext.common.server.EventPublisher var2, HarnessRun var3, BiFunction<io.karatelabs.ext.agent.harness.AgentHarness.Builder, String, io.karatelabs.ext.agent.harness.HarnessResult> var4) {
      this.workingDir = var1;
      this.publisher = var2 == null ? null : (var2x, var3x, var4x) -> {
         String var5 = this.sanitized(var2x, var4x);
         if (var5 != null) {
            var2.publish(var2x, var3x, var5);
         }
      };
      this.run = var3;
      this.executor = var4;
      this.store = new RunMetadataStore(var1, () -> this.sanitizer);
      io.karatelabs.ext.coverage.collect.CoveragePathFilterRegistry.a(this::inFlightOrActive);
      this.fn("help", var0 -> JsApi.helpTopic(help(), var0));
      this.fn("explore", var1x -> this.explore(JsApi.arg(var1x, 0), JsApi.arg(var1x, 1)));
      this.fn("author", var1x -> this.author(JsApi.arg(var1x, 0), JsApi.arg(var1x, 1)));
      this.fn("heal", var1x -> this.heal(JsApi.arg(var1x, 0), JsApi.arg(var1x, 1)));
      this.fn("revise", var1x -> this.revise(JsApi.arg(var1x, 0), JsApi.arg(var1x, 1), JsApi.arg(var1x, 2)));
      this.fn("troubleshoot", var1x -> this.troubleshoot(JsApi.arg(var1x, 0), JsApi.arg(var1x, 1)));
      this.fn("codify", var1x -> this.codify(JsApi.arg(var1x, 0), JsApi.arg(var1x, 1)));
      this.fn("status", var1x -> this.status());
      this.fn("runs", var1x -> this.runs(JsApi.arg(var1x, 0)));
      this.fn("get", var1x -> this.get(JsApi.str(var1x, 0)));
      this.fn("pause", var1x -> {
         var3.pause();
         return var3.toMap();
      });
      this.fn("resume", var1x -> {
         var3.resume(JsApi.arg(var1x, 0) instanceof String var2x ? var2x : null);
         return var3.toMap();
      });
      this.fn("takeControl", var1x -> this.takeControl());
      this.fn("giveBack", var1x -> this.giveBack(JsApi.arg(var1x, 0) instanceof String var2x ? var2x : null));
      this.fn("stampLiveGoal", var2x -> {
         this.stampLiveGoal(JsApi.str(var2x, 0));
         return var3.toMap();
      });
      this.fn("stop", var1x -> this.stop(JsApi.arg(var1x, 0) instanceof String var2x ? var2x : null));
      this.fn("providers", var1x -> this.providers());
      this.fn("ping", var1x -> this.ping(JsApi.arg(var1x, 0)));
      this.fn("draft", var1x -> this.draft(JsApi.arg(var1x, 0), JsApi.arg(var1x, 1)));
      this.fn(
         "api",
         var1x -> this.apiChat == null
            ? JsApi.missing("Harness.api: the side chat runs on the console (serve)")
            : this.apiChat.send(JsApi.arg(var1x, 0), JsApi.arg(var1x, 1))
      );
      this.fn(
         "chat",
         var1x -> this.apiChat == null ? JsApi.missing("Harness.chat: the side chat runs on the console (serve)") : this.apiChat.read(JsApi.arg(var1x, 0))
      );
      this.fn("delete", var1x -> this.delete(JsApi.str(var1x, 0)));
      this.fn("clear", var1x -> this.clear());
      this.fn("recordLive", var1x -> this.recordLive());
      this.fn("startLive", var1x -> {
         LinkedHashMap var2x = new LinkedHashMap();
         var2x.put("runId", this.startLive());
         return var2x;
      });
   }

   public Harness withProjectBoot(Supplier<Object> var1) {
      this.projectBoot = var1;
      return this;
   }

   public Harness withSanitizer(UnaryOperator<String> var1) {
      if (var1 != null) {
         this.sanitizer = var1;
      }

      return this;
   }

   public UnaryOperator<String> sanitizer() {
      return this.sanitizer;
   }

   public Harness withConsole(ConsoleState var1) {
      this.console = var1;
      return this;
   }

   private String storedSecret(String var1) {
      io.karatelabs.ext.common.secrets.SecretStore var2 = this.console.store();
      return var2 == null ? null : var2.e().get(var1);
   }

   public Harness withTriage(TriageGate var1) {
      this.triage = var1;
      return this;
   }

   public TriageGate triage() {
      return this.triage;
   }

   public String claimApiRun(String var1, String var2) {
      this.lock.lock();

      String var4;
      try {
         String var3 = this.runBusyReason();
         if (var3 == null) {
            Path var9 = this.workingDir.resolve("runs").resolve(var1).resolve("transcript.jsonl");
            return this.run.tryStart(var1, var9, "api", var2) ? null : this.loopActiveReason();
         }

         var4 = var3;
      } finally {
         this.lock.unlock();
      }

      return var4;
   }

   public void settleApiRun(String var1, String var2) {
      this.lock.lock();

      try {
         if (this.run.isRunning() && var1.equals(this.run.id())) {
            this.run.settle(var2);
         }
      } finally {
         this.lock.unlock();
      }
   }

   public Harness withApiChat(ApiChat var1) {
      this.apiChat = var1;
      return this;
   }

   Harness withEnv(Function<String, String> var1) {
      this.env = var1;
      return this;
   }

   public Harness withModelProbe(BooleanSupplier var1) {
      this.modelProbe = var1;
      return this;
   }

   public void publish(String var1, String var2, String var3) {
      if (this.publisher != null) {
         this.publisher.publish(var1, var2, var3);
      }
   }

   public io.karatelabs.ext.agent.harness.HarnessResult runApiTurn(String var1, String var2, Map<String, Object> var3, Consumer<io.karatelabs.ext.agent.harness.AgentHarness.Builder> var4) {
      Path var5 = this.store.a(var1).resolve("transcript.jsonl");
      var var6 = baseMeta(var1, "api", var2, System.currentTimeMillis());
      var6.put("kind", "api");
      var6.putAll(var3);
      String var7 = "failed";

      io.karatelabs.ext.agent.harness.HarnessResult var19;
      try {
         Files.createDirectories(var5.getParent());
         this.store.a(var1, var6);
         io.karatelabs.ext.agent.harness.AgentHarness.Builder var8 = io.karatelabs.ext.agent.harness.AgentHarness.builder().runId(var1).transcriptPath(var5).onLoopCreated(this.run::attachLoop);
         var4.accept(var8);
         this.select(var8, null, null);
         this.withLlm(var6, null, null);
         this.store.a(var1, var6);
         io.karatelabs.ext.agent.harness.HarnessResult var18 = this.executor.apply(var8.redactor(this.sanitizer), var2);
         var7 = HarnessRun.terminalStatus(var18, false);
         var6.put("status", var7);
         var6.put("finishedMs", System.currentTimeMillis());
         if (var18 != null) {
            var6.put("iterations", var18.state().iterations());
            var6.put("succeeded", var18.isSuccess());
            var6.put("tokens", var18.totalTokens());
            var6.put(var18.isSuccess() ? "result" : "error", var18.isSuccess() ? var18.result() : var18.errorMessage());
            putIfPresent(var6, "errorShort", var18.g());
            putIfPresent(var6, "errorHint", var18.h());
         }

         this.store.a(var1, var6);
         var19 = var18;
      } catch (Exception var16) {
         var6.put("status", "failed");
         var6.put("finishedMs", System.currentTimeMillis());
         var6.put("error", var16.getMessage());
         io.karatelabs.ext.agent.llm.LlmException var9 = io.karatelabs.ext.agent.llm.LlmException.a(var16);
         if (var9 != null && var9.b() > 0) {
            putIfPresent(var6, "errorShort", var9.f());
            putIfPresent(var6, "errorHint", var9.g());
         }

         try {
            this.store.a(var1, var6);
         } catch (RuntimeException var15) {
            logger.warn("could not persist api run {} meta: {}", var1, var15.getMessage());
         }

         throw var16 instanceof RuntimeException var10 ? var10 : new RuntimeException(var16);
      } finally {
         this.settleApiRun(var1, var7);
      }

      return var19;
   }

   Object[] select(String var1, String var2, Function<String, String> var3) {
      io.karatelabs.ext.agent.llm.LlmProvider var4 = parseProviderLenient(var2);
      if (var1 == null) {
         List var5 = (List)this.providers(var3).get("providers");
         Map var6 = null;

         for (Map var8 : (Iterable<Map>) (Iterable<?>) (var5)) {
            boolean var9 = var4 == null ? Boolean.TRUE.equals(var8.get("default")) : var4.name().equalsIgnoreCase(String.valueOf(var8.get("name")));
            if (var9) {
               var6 = var8;
               break;
            }
         }

         if (var6 == null && var4 == null && !var5.isEmpty()) {
            var6 = (Map)var5.get(0);
         }

         if (var6 != null) {
            var4 = parseProviderLenient(String.valueOf(var6.get("name")));
            var1 = String.valueOf(((Map)((List)var6.get("models")).get(0)).get("id"));
         }
      }

      return io.karatelabs.ext.agent.harness.AgentHarness.b(var1, var4, var3);
   }

   private void select(io.karatelabs.ext.agent.harness.AgentHarness.Builder var1, String var2, String var3) {
      Object[] var4 = this.select(var3, var2, this.env);
      var1.provider((io.karatelabs.ext.agent.llm.LlmProvider)var4[0]);
      if (var4[1] != null) {
         var1.model((String)var4[1]);
      }
   }

   public boolean modelConfigured() {
      BooleanSupplier var1 = this.modelProbe;
      if (var1 != null) {
         return var1.getAsBoolean();
      } else {
         try {
            return this.providers().get("providers") instanceof List var2 && !var2.isEmpty();
         } catch (RuntimeException var4) {
            return false;
         }
      }
   }

   public Harness withProjectGraph(Path var1) {
      this.projectGraph = var1;
      return this;
   }

   public Harness withActiveRuns(ActiveRuns var1) {
      this.activeRuns = var1;
      return this;
   }

   public Harness withRulesHomes(Path var1, Path var2, List<Path> var3, Function<Path, io.karatelabs.ext.coverage.CoverageProjectBuilder.CoverageProjectBuildResult> var4) {
      this.rulesHome = var1;
      this.rulesReportDir = var2;
      this.requirementsHomes = var3 == null ? null : List.copyOf(var3);
      this.projectBuilder = var4;
      return this;
   }

   private Path rulesHome() {
      return this.rulesHome != null ? this.rulesHome : MaxWorkspace.rulebooksDir(this.workingDir);
   }

   private Path rulesReportDir() {
      return this.rulesReportDir != null ? this.rulesReportDir : this.workingDir.resolve("target/karate-reports");
   }

   private List<Path> requirementsHomes() {
      return this.requirementsHomes != null ? this.requirementsHomes : List.of(MaxWorkspace.requirementsDir(this.workingDir));
   }

   public static Harness at(Path var0) {
      return new Harness(var0);
   }

   public static Harness at(Path var0, io.karatelabs.ext.common.server.EventPublisher var1) {
      return new Harness(var0, var1);
   }

   private static Map<String, Object> help() {
      return JsApi.markdown(
         "# Harness — kick off the toolkit-agnostic agentic loop\n\nRuns the LLM ↔ tool loop and returns its terminal outcome. The browser\ntoolkit (`Robot`) + the live, SSE-streamed run console ride this in E1-b.\n\n**Single-user:** this container has one observable browser (one display/video/noVNC),\nso **one run at a time**. While a loop OR a live (`Robot.create()`) run is in flight,\na new `Harness.explore` or `Robot.create()` is **refused** — stop the current one first\n(`Harness.stop()` / `bot.destroy()`), or use a second container. `Harness.status()` reports\nthat one in-flight run (loop or live).\n\n- `Harness.explore(task, [{robot?, async?, runId?, model?, provider?, maxIterations?, transcript?, record?, hints?, req?}])`\n    - `task` — the goal text, OR the name of a markdown file in the project (e.g.\n      `'gwpc/ca/demo.md'`, `'docs/tour.md'`): the file's content becomes the task — a\n      user-authored prompt with app advice/credentials (loop-only; the deterministic\n      `Check.run` path never reads markdown). A bare name is checks-relative\n      (`'demo.md'` → `checks/demo.md`); any other path resolves from the project root.\n      A `*.md` name that does not resolve is REFUSED — it never runs as literal text.\n    - `robot:true` — drive a real browser (attaches to `KARATE_CDP_URL` in-container). The loop\n      also gets `Check` + `File` (replay deterministic `checks/*.js` macros, read `checks/*.md` notes).\n    - `async:true` — claim the single run slot, run on a background thread, return\n      `{runId, status:'started', transcript}`; watch over noVNC + review `Harness.status()`.\n    - sync (default) — returns `{runId, status, iterations, succeeded, result|error, tokens, transcript}`.\n    - `runId` — name the run (else a short id is generated); it is how you address the\n      run's artifacts — the transcript downloads at `/api/artifacts/runs/<runId>/transcript.jsonl`.\n    - `record` — a `robot` run auto-records its display to `runs/<id>/video.mp4`\n      (played back on the run page); `record:false` opts out.\n    - `hints` — a project `*.md` file (same resolution as `task`) appended to the task as\n      \"App-specific hints\"; one that does not resolve is refused, never silently dropped.\n    - `req` — a requirement anchor (`'ORD-001'` or criterion-level `'ORD-001/1'`): stamped on\n      the run meta so the run's evidence (steps, screenshots) attributes to that\n      requirement in the traceability graph as `@ai` attested hits; an inline `@req=` token in\n      a `Robot.step` name / screenshot title refines it per-criterion.\n    - `provider` (`anthropic`|`openrouter`|`ollama`) · `model` · `maxIterations` · `transcript`.\n    - loop bounds (any mode): `deadlineMs` — a wall-clock ceiling; the loop ends gracefully\n      (status `TERMINATED`) at the next turn boundary, so a blocking caller always gets the\n      partial result back. `maxTokens` — total-token cap. `maxCostUsd` — spend cap, needs\n      `rates: {inputPer1M, outputPer1M}` (your model's USD prices — pricing is never built in).\n    - `goal` (any mode) — a **pre-registered measured exit**: `{name?, expr}` where `expr` is\n      YOUR JS predicate over `result` (each tool step's structured value). The run ends\n      COMPLETED the moment a step's result satisfies it — e.g.\n      `{goal: {name: 'suite-green', expr: \"result && result.failed === 0 && result.passed >= 3\"}}`\n      fires on the `Runner.run` step that goes green. Deterministic (it reads verb output,\n      never judges text); the in-loop model can't see or change it.\n- `Harness.troubleshoot(runId, [{fix?, deadlineMs?, maxIterations?, model?, provider?, async?}])` —\n  diagnose a settled FAILED run from its artifacts (transcript, screenshots, report verdicts):\n  **headless** (no browser), BYO-LLM, CI-runnable. Writes `runs/<runId>/diagnosis.md` — YAML\n  front matter carrying the three-cause `verdict` (`sut-bug` | `rules-wrong` | `bot-misread`,\n  or `inconclusive`) + an optional drifted `check`, then the evidence — and ends the moment\n  it lands; sync by default so a `launch` tail blocks on it (`result` = the parsed diagnosis),\n  with `deadlineMs: 300000` defaulted ON for that lane (pass `deadlineMs: 0` to disable;\n  `async:true` runs it unbounded under console watch instead). `{fix:true}` then heals the\n  check the diagnosis names (never a `sut-bug` — a real defect is reported, not papered over)\n  via the same engine as `Harness.heal`; the outcome rides back under `fix`.\n- `Harness.codify(requirement, {book, requires?, model?, provider?, maxIterations?, async?})` —\n  turn a stated requirement into a **verified rulebook**, as one autonomous run. Headless\n  (no browser), BYO-LLM, CI-runnable. `requirement` is the prose — or the name of a project\n  `*.md` holding it, resolved exactly as `explore`'s task is. `book` names the rulebook the\n  run must deliver. The loop gets `Rule`/`Schema`/`Requirement`/`Skill` (plus `File`/`Check`/\n  `match`) and authors + verifies the book itself.\n    - **The exit is computed here, not read back from the model.** When the book ON DISK\n      clears the bar the run ends COMPLETED — the verdict comes from this side calling\n      `Rule.check` itself, so nothing the loop returns can end the run. That is why\n      `{goal}` is refused in this mode.\n    - The bar's floor is fail-closed and always on: at least one declared guarantee and\n      one saved scenario; every `calc.always` `HOLDS`/`COMPLETE` and every `calc.sometimes`\n      satisfied; no `notreachable`, no `notproduced`, no `propertyErrors`; every declared\n      negative scenario actually refused; and every guarantee the book already declared at\n      kickoff still present (a green you reach by DELETING an obligation is not reachable);\n      no saved scenario outside the domain `generator.js` declares (the book's own\n      evidence and its own declared universe must agree); and no counterexample this run\n      PROVED still breaking a guarantee that now reads green from inside the declared\n      domain. A green run's `retired[]` discloses every contradiction that stopped being\n      reported and what stopped it — the rules changing, the schema refusing the input, a\n      `throw` precondition rejecting it, or only the declared domain moving.\n    - `requires` raises it: `{properties, scenarios, rejects}` (minimum counts),\n      `{complete:true}` (demand `COMPLETE` — proof over a closed, fully-walked universe —\n      rather than the sampled `HOLDS`), `{maxProduct}` (the walk's row cap).\n      `complete:true` asks for proof, so it needs a genuinely closed, in-cap universe:\n      a book with a free-text or array axis cannot meet it however its guarantees are\n      written, and `Rule.check`'s `universe.causes` names the reason.\n      `{keepWitnesses:true}` adds: a counterexample this run proved must be ANSWERED,\n      not merely put outside the declared domain — fix the rules, or say plainly that\n      the input cannot occur (refuse it in `schema.js`, or `throw '<reason>'` in\n      `calc.js`). Default off: narrowing a domain is an ordinary authoring correction\n      unless you expect the contradictions to survive.\n    - **A third outcome, opt-in and not a failure:** with `{requires: {mayBlock:true}}`,\n      a run whose book is finished in every other way and whose only remaining shortfall\n      is a declared guarantee the rules provably VIOLATE ends `BLOCKED`, carrying the\n      proof — `blocker.contradictions[]` names each property, its `{req}` when it has\n      one, and the engine's in-domain counterexample. Use it when the requirement you\n      are codifying might genuinely contradict itself and you want that reported rather\n      than iterated against. **Default off**, because a contradiction nobody anticipated\n      is a bug to fix, not an ending — and because what the engine proves is bounded:\n      the rules violate THAT predicate. It cannot prove the predicate is faithful to\n      your requirement, since the loop wrote it — so read the artifact. `succeeded`\n      stays false either way, so nothing reads it as green.\n    - Async by default; **both lanes are recorded** (`runs/<id>/` + `transcript.jsonl`),\n      because this mode is judged off its transcript. `maxIterations` defaults 30.\n    - **What a green run certifies:** the rulebook is internally consistent and\n      non-vacuously verified *against its own declared universe*, **as far as that\n      universe was walked** — where `check.universe.complete` is false, an `always` at a\n      sampled `HOLDS` means \"the walk found nothing\", not \"there is nothing\". It does NOT\n      certify that the interpretation matches business intent: a predicate weakened to\n      `true`, or a domain narrowed past the counterexample, still reads green. Both edges\n      are what the human reviewing the interpretation is for. A run that completes with\n      `iterations: 1` and **zero tokens** never reached the model at all — the book\n      already cleared the bar, which is the baseline-red gate reading red.\n- `Harness.author(recordingRunId, [{name?, overwrite?, model?, provider?, maxIterations?, async?}])` —\n  drive the **record→check authoring loop** as a goal: hand it a recording (its run id, from\n  `Recording.stop()`/`Harness.runs()`) and the harness authors + verifies a real check ITSELF\n  (the in-product twin of an external coding agent using the `record-to-check` skill). It runs\n  browser-autonomous, digests the recording into the task, writes `checks/<name>.js`, and\n  iterates `Check.run` until green. `name` names the check (default `recorded-flow`); a name a\n  check already has is refused 409 `EXISTS` unless `overwrite:true`. Async by default with a\n  generous iteration budget. The record's `changed` says whether it wrote that file.\n- `Harness.heal(target, [{model?, provider?, maxIterations?, async?, record?}])` — **run-or-heal**\n  an existing deterministic check (the *Maintain* cell; human label **\"Run with AI\"**). Runs\n  `checks/<target>.js`; GREEN ⇒ confirms + exits cheaply (a non-drifted check isn't rewritten);\n  RED ⇒ re-drives the flow over the live bot using the file's steps/locators as HINTS, then\n  repairs the source back to **honest** green (no loosened assertions, no SUT-bug-as-spec). The\n  only Harness-over-a-deterministic-runnable, opt-in. UI `.js` only for now (API/`.feature`\n  deferred). `target` is a check name, e.g. `Harness.heal('storefront-ui.js')`; the record's\n  `changed` says whether it rewrote the file.\n- `Harness.revise(target, request, [{model?, provider?, maxIterations?}])` — change an existing\n  `.js` check as a person asked, async. Unless the run completes, the check is put back as it\n  was — or kept, when it was changed outside the run meanwhile (`restored:false`). The record\n  carries `request`, `changed`, `restored?`, `conflict?` (a change made outside the run, on any\n  ending), and `verifiedBy` — the first replay after it — since the loop's own word is not a\n  verdict. A run working on the check refuses it 409 `BUSY`.\n- `Harness.providers()` — the **LLM** providers usable here (env keys present) + the default\n  selection. Not API providers: the thing a contract pair runs against is named by\n  `Contract.pair({provider})` and lives in your project, not in this namespace.\n- `Harness.ping([{provider?, model?}])` — the **cheapest** provider preflight: resolves\n  provider/model exactly as a run would and sends ONE minimal completion. Returns\n  `{ok:true, provider, model, latencyMs}`, or `{ok:false, provider, model, status?, error, hint?}`\n  — `error` is the provider's own body verbatim and `hint` one plain-English sentence for a\n  recognized failure (a rejected key, a resource firewall blocking this machine, an unknown\n  deployment/model, an exhausted quota). It never throws, and a missing key is answered with\n  no network call. Use it before `explore` when a run failed on the LLM call.\n- `Harness.draft(markdown[, {provider?, model?, taken?}])` — ONE completion that rewrites a\n  requirements draft (notes, placeholders, repeated ids) in the requirements format, for a person\n  in the console; you write requirements yourself. `{ok:true, provider, model, markdown, count, ids}`\n  — every requirement marked `@ai=draft`, none approved — or ping's `{ok:false, …}`. Writes\n  nothing: check it with `Requirement.write(name, markdown, {dryRun:true})`. `taken`: the ids other\n  files hold.\n- `Harness.api(message, {test?, chat?})` — one side-chat run for a tester's message on an API test:                 `{chat, turn, runId, status: 'started'}`, or `BUSY` while a run holds the slot.                 `Harness.chat({chat?, test?})` → the chat's turns, their cards and replies.\n- `Harness.status()` — the one in-flight run (loop OR live), else the last settled run.\n- `Harness.runs([{selector?, limit?}])` — runs on disk, newest first: `{runs:[{runId, task,\n  kind, ai, status, startedMs, finishedMs?, live, …}], count, active, inFlight}`. `kind` is\n  author · heal · revise · explore · troubleshoot · codify · api · live · check · suite; `ai` is true when an\n  LLM loop drove the run. `selector` keeps only runs of that exact selection (a path or path\n  list, order counts); `limit` caps the rows. `active` is the connectable run (the loop, else\n  the live run), else null; `inFlight` lists every running id, a leased set or async suite too.\n- `Harness.get(id)` — one run's record: `status` (completed · failed · stopped · terminated,\n  its reason in `error` · blocked · empty, nothing ran · interrupted, its process died),\n  `passed`/`failed`, `durationMs`, `verdicts` `[{name, passed, durationMs, error?}]` (a labeled\n  `match`/`verify` failure is the row's `error`), a set's `memberVerdicts` `[{path, kind,\n  passed, durationMs, error?}]` (the member's scenarios summed), `checks` `[{label, actual?,\n  pass}]` + `checkTotals` — what the run asserted (labeled matches, Gherkin `match`/`assert`\n  steps, pass/fail `Robot.step`s, each once across a loop's attempts) — and `reportUrl`, its\n  native karate report.\n- `Harness.pause()` / `Harness.resume([message])` — steer an in-flight async loop (the console\n  Pause/Resume buttons); resume optionally injects a user message. No-op if nothing is running.\n- `Harness.takeControl()` / `Harness.giveBack([note])` — human↔agent handoff (UI recording):\n  take control pauses the in-flight loop so a human can drive the container Chrome; give back\n  resumes it with a transcript-pull hint so it can see what the human did. To record a manual\n  flow standalone (no loop), see the `Recording.*` namespace.\n- `Harness.stop([status])` — stops the in-flight loop within ~100ms (its record settles\n  `stopped`) or settles the live run (`completed`, or the given status, e.g.\n  `Harness.stop('failed')`).\n- `Harness.delete(id)` — delete one settled run's folder (meta + transcript + artifacts);\n  refuses the in-flight one. `Harness.clear()` — delete ALL settled runs.\n\nThe simplest benchmark: `Harness.explore('open /benchmark.html, type \"hello\" in the message box,\nsubmit, and confirm the page shows \"received: hello\"', {robot:true, async:true})`, then watch\nnoVNC. Needs an LLM key (`OPENROUTER_API_KEY` / `ANTHROPIC_API_KEY`); a missing key is recorded\non the run. Also reachable as `Runner.harness(task, opts)`."
      );
   }

   Map<String, Object> explore(Object var1, Object var2) {
      if (var1 instanceof String var3 && !var3.isBlank()) {
         Map var4 = var2 instanceof Map var5 ? cast(var5) : Map.of();
         if (bool(var4, "robot")) {
            io.karatelabs.ext.common.keycheck.Main.requireProduct("robot", "robot");
         }

         var var12 = this.runsNotWritable();
         if (var12 != null) {
            return var12;
         } else {
            Map var6 = budgetNotUsable(var4);
            if (var6 != null) {
               return var6;
            } else {
               Map var7 = goalNotUsable(var4);
               if (var7 != null) {
                  return var7;
               } else {
                  Map var8 = this.codifyNotUsable(var4);
                  if (var8 != null) {
                     return var8;
                  } else {
                     Map var9 = this.reviseNotUsable(var4);
                     if (var9 != null) {
                        return var9;
                     } else {
                        Map var10 = this.mdRefNotResolvable(var3, var4);
                        if (var10 != null) {
                           return var10;
                        } else {
                           String var11 = this.resolveTask(var3, var4);
                           if (bool(var4, "async")) {
                              return this.runAsync(var11, var4);
                           } else {
                              return recordedSync(var4) ? this.runSyncRecorded(var11, var4) : this.runSync(var11, var4);
                           }
                        }
                     }
                  }
               }
            }
         }
      } else {
         return JsApi.missing(
            "Harness.explore(task[, opts]) requires a task String, e.g. Harness.explore('summarise the open requirements', {maxIterations: 8})"
         );
      }
   }

   static String modeOf(Map<String, Object> var0) {
      if (var0.get("troubleshoot") != null) {
         return "troubleshoot";
      } else if (var0.get("codify") != null) {
         return "codify";
      } else {
         return bool(var0, "robot") ? "browser" : "headless";
      }
   }

   static String kindOf(Map<String, Object> var0) {
      if (var0.get("troubleshoot") instanceof String var1 && !var1.isBlank()) {
         return "troubleshoot";
      } else if (var0.get("codify") != null) {
         return "codify";
      } else if (bool(var0, "heal")) {
         return "heal";
      } else if (bool(var0, "revise")) {
         return "revise";
      } else {
         Object var3 = var0.get("author");
         return var3 != null && !String.valueOf(var3).isBlank() ? "author" : "explore";
      }
   }

   static String recordKind(Map<String, Object> var0) {
      if (var0.get("kind") instanceof String var1 && !var1.isBlank()) {
         return var1;
      } else {
         return var0.get("mode") instanceof String var3 && !LOOP_MODES.contains(var3) ? var3 : null;
      }
   }

   static boolean aiOf(Map<String, Object> var0, String var1) {
      return var1 != null
         ? List.of("author", "heal", "revise", "explore", "troubleshoot", "codify", "api").contains(var1)
         : var0.get("mode") instanceof String var2 && LOOP_MODES.contains(var2);
   }

   private static boolean recordedSync(Map<String, Object> var0) {
      String var1 = modeOf(var0);
      return "troubleshoot".equals(var1) || "codify".equals(var1);
   }

   Map<String, Object> troubleshoot(Object var1, Object var2) {
      if (var1 instanceof String var3 && !var3.isBlank()) {
         String var4 = var3.trim();
         var var5 = this.store.b(var4);
         if (var5 == null) {
            return JsApi.notFound("no run '" + var4 + "' — see Harness.runs()");
         } else {
            String var6 = String.valueOf(var5.get("status"));
            if (this.inFlight(var4) || this.run.isRunning() && var4.equals(this.run.id())) {
               return JsApi.missing(
                  "run '" + var4 + "' is still in flight — troubleshoot reads a SETTLED run's artifacts; wait for it to finish (or Harness.stop() it) first"
               );
            } else if ("completed".equals(var6)) {
               return JsApi.missing(
                  "run '" + var4 + "' completed successfully — troubleshoot diagnoses a FAILED run. Its record is Harness.get('" + var4 + "')."
               );
            } else {
               if ("running".equals(var6)) {
                  var6 = "interrupted";
               }

               if (!DIAGNOSABLE_STATUSES.contains(var6)) {
                  return JsApi.missing(
                     "run '"
                        + var4
                        + "' has status '"
                        + var6
                        + "' — troubleshoot diagnoses a settled failed run (status "
                        + String.join("/", DIAGNOSABLE_STATUSES)
                        + "). See Harness.get('"
                        + var4
                        + "')."
                  );
               } else {
                  Map var7 = var2 instanceof Map var8 ? cast(var8) : Map.of();
                  boolean var13 = bool(var7, "fix");
                  if (var13 && bool(var7, "async")) {
                     return JsApi.missing(
                        "{fix:true} needs the sync lane (the fix runs AFTER the diagnosis settles) — drop {async:true}, or run the diagnosis async and Harness.heal the check it names yourself"
                     );
                  } else {
                     LinkedHashMap var9 = new LinkedHashMap(var7);
                     String var10 = var7.get("runId") instanceof String var11 && !var11.isBlank() ? var11 : UUID.randomUUID().toString().substring(0, 8);
                     if (var10.equals(var4)) {
                        return JsApi.missing(
                           "{runId: '" + var10 + "'} is the failed run's own id — the troubleshoot run needs a FRESH id (or omit runId and one is generated)"
                        );
                     } else if (this.store.b(var10) != null) {
                        return JsApi.missing(
                           "a run '" + var10 + "' already exists — the troubleshoot run needs a FRESH id (or omit runId and one is generated)"
                        );
                     } else {
                        var9.put("runId", var10);
                        var9.put("troubleshoot", var4);
                        var9.put("troubleshootRunId", var10);
                        var9.putIfAbsent("maxIterations", 25);
                        if (!bool(var9, "async")) {
                           var9.putIfAbsent("deadlineMs", 300000);
                        }

                        var var14 = this.explore(this.troubleshootTask(var4, var5), var9);
                        if (var13 && var14 != null && !var14.containsKey("error")) {
                           var14.put("fix", this.runFix(var4, var10, var7));
                        }

                        return var14;
                     }
                  }
               }
            }
         }
      } else {
         return JsApi.missing("Harness.troubleshoot(runId[, opts]) requires a failed run's id — see Harness.runs()");
      }
   }

   private Map<String, Object> runFix(String var1, String var2, Map<String, Object> var3) {
      TroubleshootToolkit var4 = new TroubleshootToolkit(this.runDir(var1).resolve("diagnosis.md"), var1, var2);
      var var5 = var4.readValidDiagnosis();
      if (var5 == null) {
         return noFix("no valid diagnosis was produced — nothing to act on");
      } else {
         String var6 = String.valueOf(var5.get("verdict"));
         if ("sut-bug".equals(var6)) {
            return noFix("verdict is sut-bug — a genuine application defect is reported, never healed over (the check is right; the SUT is wrong)");
         } else if ("inconclusive".equals(var6)) {
            return noFix("verdict is inconclusive — healing without a diagnosed cause would guess");
         } else if (var5.get("check") instanceof String var7 && !var7.isBlank()) {
            LinkedHashMap var11 = new LinkedHashMap();
            var11.put("async", false);
            if (var3.get("model") instanceof String var9 && !var9.isBlank()) {
               var11.put("model", var9);
            }

            if (var3.get("provider") instanceof String var12 && !var12.isBlank()) {
               var11.put("provider", var12);
            }

            var var13 = this.heal(var7, var11);
            LinkedHashMap var15 = new LinkedHashMap();
            var15.put("ran", !var13.containsKey("error") || var13.containsKey("status"));
            var15.put("check", var7);
            var15.putAll(var13);
            return var15;
         } else {
            return noFix("the diagnosis names no drifted checks/*.js to heal (verdict " + var6 + ")");
         }
      }
   }

   private static Map<String, Object> noFix(String var0) {
      LinkedHashMap var1 = new LinkedHashMap();
      var1.put("ran", false);
      var1.put("reason", var0);
      return var1;
   }

   Map<String, Object> codify(Object var1, Object var2) {
      if (var1 instanceof String var3 && !var3.isBlank()) {
         Map var4 = var2 instanceof Map var5 ? cast(var5) : Map.of();
         if (var4.get("book") instanceof String var10 && !var10.isBlank()) {
            String var11 = var10.trim();
            LinkedHashMap var7 = new LinkedHashMap(var4);
            var7.put("codify", var11);
            var7.putIfAbsent("async", Boolean.TRUE);
            var7.putIfAbsent("maxIterations", 30);
            if (var7.get("runId") instanceof String var8 && !var8.isBlank()) {
               if (this.store.b(var8.trim()) != null) {
                  return JsApi.missing("a run '" + var8.trim() + "' already exists — a codify run needs a FRESH id (or omit runId and one is generated)");
               }
            } else if (!bool(var7, "async")) {
               var7.put("runId", UUID.randomUUID().toString().substring(0, 8));
            }

            return this.explore(var3, var7);
         } else {
            return JsApi.missing(
               "Harness.codify needs {book: '<name>'} — the rulebook this run must deliver. The exit is a check of THAT book on disk, so it needs an identity: Harness.codify(task, {book: 'late-fee'}). See Rule.list() for what exists."
            );
         }
      } else {
         return JsApi.missing(
            "Harness.codify(task, {book}) requires the requirement to codify — the prose a rulebook must express, or the name of a project *.md holding it, e.g. Harness.codify('requirements/late-fee.md', {book: 'late-fee'})"
         );
      }
   }

   Map<String, Object> codifyNotUsable(Map<String, Object> var1) {
      if (var1.get("codify") instanceof String var2 && !var2.isBlank()) {
         io.karatelabs.ext.common.keycheck.Main.requireProduct("rules", "rules");
         return var1.get("goal") != null
            ? JsApi.missing(
               "the codify mode does not take {goal} — its exit is computed on this side, by checking the rulebook on disk, so no value the loop returns can end the run. A {goal} predicate reads whatever the model's last statement evaluated to, which would be a second, forgeable way out. Pre-register the BAR instead: {requires: {properties: 2, scenarios: 6, rejects: 2, complete: true}}."
            )
            : requiresNotUsable(var1);
      } else {
         return null;
      }
   }

   static CodifyToolkit.Requires requiresFrom(Map<String, Object> var0) {
      if (var0.get("requires") instanceof Map var1) {
         CodifyToolkit.Requires var5 = CodifyToolkit.Requires.defaults();
         return new CodifyToolkit.Requires(
            intOr(var1.get("properties"), var5.properties()),
            intOr(var1.get("scenarios"), var5.scenarios()),
            intOr(var1.get("rejects"), var5.rejects()),
            Boolean.TRUE.equals(var1.get("complete")),
            var1.get("maxProduct") instanceof Number var3 ? var3.longValue() : null,
            Boolean.TRUE.equals(var1.get("mayBlock")),
            Boolean.TRUE.equals(var1.get("keepWitnesses"))
         );
      } else {
         return CodifyToolkit.Requires.defaults();
      }
   }

   private static int intOr(Object var0, int var1) {
      return var0 instanceof Number var2 ? var2.intValue() : var1;
   }

   static Map<String, Object> requiresNotUsable(Map<String, Object> var0) {
      Object var1 = var0.get("requires");
      if (var1 == null) {
         return null;
      } else if (var1 instanceof Map var2) {
         for (Object var4 : var2.keySet()) {
            String var5 = String.valueOf(var4);
            if (!REQUIRES_KEYS.contains(var5)) {
               return JsApi.missing(
                  "{requires."
                     + var5
                     + "} is not part of the bar — a key this mode ignored would leave you believing you raised it when you did not. Known: "
                     + String.join(", ", REQUIRES_KEYS)
                     + "."
               );
            }
         }

         for (String var8 : List.of("properties", "scenarios", "rejects")) {
            String var9 = notAWholeCount(var2.get(var8), 0L, 2.147483647E9);
            if (var9 != null) {
               return JsApi.missing("{requires." + var8 + "} " + var9 + " — it is the minimum this run must reach. Got " + var2.get(var8));
            }
         }

         if (var2.get("mayBlock") != null && !(var2.get("mayBlock") instanceof Boolean)) {
            return JsApi.missing(
               "{requires.mayBlock} must be true/false — whether a guarantee the rules provably violate may END the run (BLOCKED, carrying the counterexample) instead of being work still to do. Default false: a contradiction nobody anticipated is a bug to fix, not an ending. Got "
                  + var2.get("mayBlock")
            );
         } else if (var2.get("keepWitnesses") != null && !(var2.get("keepWitnesses") instanceof Boolean)) {
            return JsApi.missing(
               "{requires.keepWitnesses} must be true/false — whether a counterexample this run PROVED must still be answered when it stops being reported, rather than merely put outside the declared domain. Default false: narrowing a domain is an ordinary authoring correction unless the caller expects the contradictions to survive. Got "
                  + var2.get("keepWitnesses")
            );
         } else if (var2.get("complete") != null && !(var2.get("complete") instanceof Boolean)) {
            return JsApi.missing(
               "{requires.complete} must be true/false — whether every guarantee must read COMPLETE (proof over a closed, fully-walked universe) rather than the sampled HOLDS. Got "
                  + var2.get("complete")
            );
         } else {
            String var7 = notAWholeCount(var2.get("maxProduct"), 1L, 9.007199E15F);
            return var7 != null
               ? JsApi.missing("{requires.maxProduct} " + var7 + " — it is the full-product row cap passed to Rule.check. Got " + var2.get("maxProduct"))
               : null;
         }
      } else {
         return JsApi.missing(
            "{requires} must be an object — the acceptance bar this run must clear, e.g. {requires: {properties: 2, scenarios: 6, rejects: 2, complete: true}}"
         );
      }
   }

   private static String notAWholeCount(Object var0, long var1, double var3) {
      if (var0 == null) {
         return null;
      } else if (var0 instanceof Number var5) {
         double var6 = var5.doubleValue();
         if (Double.isNaN(var6) || Double.isInfinite(var6)) {
            return "must be a finite number";
         } else if (var6 != Math.floor(var6)) {
            return "must be a whole number";
         } else if (var6 < var1) {
            return "must be " + var1 + " or more";
         } else {
            return var6 > var3 ? "is too large to hold exactly (max " + (long)var3 + ")" : null;
         }
      } else {
         return "must be a number";
      }
   }

   String troubleshootTask(String var1, Map<String, Object> var2) {
      StringBuilder var3 = new StringBuilder("Diagnose the FAILED run '").append(var1).append("'.\n\n");
      var3.append("Its run record:\n");

      for (String var5 : List.of("task", "mode", "status", "check", "error", "errorShort")) {
         Object var6 = var2.get(var5);
         if (var6 != null && !String.valueOf(var6).isBlank()) {
            var3.append("- ").append(var5).append(": ").append(var6).append('\n');
         }
      }

      var var7 = io.karatelabs.ext.max.jsapi.RunEvidenceInspector.a(this.store.a(var1));
      if (!var7.isEmpty()) {
         var3.append("\nPer-scenario verdicts:\n");

         for (Map var10 : (Iterable<Map>) (Iterable<?>) (var7)) {
            var3.append("- ").append(Boolean.TRUE.equals(var10.get("passed")) ? "PASS " : "FAIL ").append(var10.get("name"));
            if (var10.get("error") != null) {
               var3.append(" — ").append(var10.get("error"));
            }

            var3.append('\n');
         }
      }

      this.appendFailedSteps(var3, var1);
      var3.append("\nIts artifacts, under runs/")
         .append(var1)
         .append("/ (read with File.read, e.g. File.read('runs/")
         .append(var1)
         .append("/transcript.jsonl')):\n");
      var var9 = this.runArtifactInventory(var1);
      if (var9.isEmpty()) {
         var3.append("- (none on disk — the run left no artifacts; diagnose from the record above and say so if the evidence is insufficient)\n");
      } else {
         var9.forEach(var1x -> var3.append("- ").append(var1x).append('\n'));
      }

      if (!"suite".equals(var2.get("mode")) && !var9.stream().anyMatch(var0 -> var0.startsWith("transcript.jsonl"))) {
         var3.append("NOTE: this run has NO transcript.jsonl.\n");
      }

      if (var9.stream().anyMatch(var0 -> var0.startsWith("diagnosis.md"))) {
         var3.append(
            "NOTE: a diagnosis.md from an EARLIER troubleshoot run exists — you may read it as context, but you must write your OWN, with YOUR run's front-matter.\n"
         );
      }

      if (Files.isRegularFile(this.store.a(var1).resolve("karate-summary.html"))) {
         var3.append(
            "The full record is karate-json/karate-events.jsonl: its FEATURE_EXIT event carries `data.scenarioResults[].stepResults[]`, each with `step`, `result.status`, `result.errorMessage` and `stepLog` (what the step printed). The rendered HTML report draws that same tree and is not listed.\n"
         );
      }

      this.appendRulebookSources(var3, var1, var2, var7);
      var3.append("No git diff is seeded in this build — diagnose from the run artifacts alone.\n");
      var3.append("\nWrite your diagnosis to runs/").append(var1).append("/diagnosis.md exactly per your instructions — writing it ends the run.");
      return var3.toString();
   }

   private List<String> runArtifactInventory(String var1) {
      Path var2 = this.store.a(var1);
      if (!Files.isDirectory(var2)) {
         return List.of();
      } else {
         try {
            List var4;
            try (var var3 = Files.walk(var2, 3)) {
               var4 = var3.filter(var0 -> Files.isRegularFile(var0))
                  .filter(
                     var1x -> !var1x.getFileName().toString().endsWith(".html")
                        && !var1x.getFileName().toString().endsWith(".css")
                        && !var2.relativize(var1x).startsWith("res")
                  )
                  .sorted()
                  .limit(40L)
                  .map(var1x -> {
                     String var2x = var2.relativize(var1x).toString().replace('\\', '/');

                     try {
                        return var2x + " (" + Files.size(var1x) + " bytes)";
                     } catch (Exception var4x) {
                        return var2x;
                     }
                  })
                  .toList();
            }

            return var4;
         } catch (Exception var8) {
            logger.debug("artifact inventory failed for {}: {}", var1, var8.getMessage());
            return List.of();
         }
      }
   }

   private void appendFailedSteps(StringBuilder var1, String var2) {
      Path var3 = this.store.a(var2).resolve("karate-json/karate-events.jsonl");
      if (Files.isRegularFile(var3)) {
         StringBuilder var4 = new StringBuilder();

         try (var var5 = Files.lines(var3)) {
            for (String var7 : (Iterable<String>) (var5::iterator)) {
               if (var7.contains("\"FEATURE_EXIT\"")
                  && var4.length() <= 20000
                  && Json.of(var7).value() instanceof Map var8
                  && var8.get("data") instanceof Map var9) {
                  Object var20 = var9.get("scenarioResults");
                  if (var20 instanceof List) {
                     for (Object var12 : (List)var20) {
                        if (var12 instanceof Map var13 && !Boolean.TRUE.equals(var13.get("passed")) && var13.get("stepResults") instanceof List var14) {
                           var4.append('\n').append(var13.get("name")).append('\n');
                           var14.forEach(var1x -> appendStep(var4, var1x));
                        }
                     }
                  }
               }
            }
         } catch (Exception var18) {
            logger.debug("step trail failed for {}: {}", var2, var18.getMessage());
            return;
         }

         if (var4.length() > 0) {
            var1.append(
                  "\nThe failed scenario's steps, as the run recorded them — status · step · what it logged. This IS the event stream's record of them; read it here:\n"
               )
               .append((CharSequence)var4);
         }
      }
   }

   private static void appendStep(StringBuilder var0, Object var1) {
      if (var1 instanceof Map var2 && var2.get("result") instanceof Map var3) {
         String var7 = var2.get("step") instanceof Map var5 ? String.valueOf(var5.get("text")) : "?";
         var0.append("  ").append(var3.get("status")).append("  ").append(var7).append('\n');
         if (var2.get("stepLog") instanceof String var8 && !var8.isBlank()) {
            var0.append("      log: ").append(clip(var8, 12000)).append('\n');
         }

         if (var3.get("errorMessage") instanceof String var9 && !var9.isBlank()) {
            var0.append("      error: ").append(clip(var9, 12000)).append('\n');
         }
      }
   }

   private static String clip(String var0, int var1) {
      String var2 = var0.strip();
      if (var2.length() > var1) {
         int var3 = var1 / 4;
         var2 = var2.substring(0, var3) + "\n…(clipped)…\n" + var2.substring(var2.length() - (var1 - var3));
      }

      return var2.replace("\n", "\n           ");
   }

   private void appendRulebookSources(StringBuilder var1, String var2, Map<String, Object> var3, List<Map<String, Object>> var4) {
      var var5 = this.rulebooksUsed(var2, var3);
      if (!var5.isEmpty()) {
         var1.append(
            "\nThe rulebook below is this run's ORACLE: the expected values come from its calc.js, so `rules-wrong` means a value or arm in THAT file disagrees with the filed rates. `Rule.list/load/execute/run/audit/check` are bound read-only; `Rule.audit(name, scenarioId)` shows which arm a row took.\n"
         );
         int var6 = 40000;

         for (String var8 : (Iterable<String>) (Iterable<?>) (var5)) {
            Path var9 = this.rulesHome().resolve(var8);
            String var10 = gradedScenarios(var9, var4, var6);
            if (var10 != null) {
               var6 -= var10.length();
               var1.append("\nRulebook `")
                  .append(var8)
                  .append("`, its rows prefixed with THIS run's verdict — what a FAIL row reaches and a PASS row does not is the discriminator:\n")
                  .append(var10);
            }

            String var11 = readCapped(var9.resolve("calc.js"), var6);
            if (var11 != null) {
               var6 -= var11.length();
               var1.append("\n").append(this.displayPath(var9.resolve("calc.js"))).append(":\n```js\n").append(var11).append("\n```\n");
            }
         }
      }
   }

   private List<String> rulebooksUsed(String var1, Map<String, Object> var2) {
      LinkedHashSet<String> var3 = new LinkedHashSet<>();
      Path var4 = this.store.a(var1);
      scanForRulebooks(var4.resolve("karate-json/karate-events.jsonl"), var3);
      scanForRulebooks(var4.resolve("transcript.jsonl"), var3);
      if (var3.isEmpty()) {
         Object var6 = var2.get("members");
         if (var6 instanceof List) {
            for (Object var7 : (List)var6) {
               if (var7 instanceof Map var8 && var8.get("path") instanceof String var9) {
                  scanForRulebooks(this.memberFile(var9), var3);
               }
            }
         }
      }

      return var3.stream().filter(var1x -> Files.isRegularFile(this.rulesHome().resolve(var1x).resolve("calc.js"))).toList();
   }

   private Path memberFile(String var1) {
      try {
         Path var2 = Path.of(var1);
         return var2.isAbsolute() ? var2 : MaxPaths.resolveSandboxed(this.workingDir, var1);
      } catch (RuntimeException var3) {
         return null;
      }
   }

   private static void scanForRulebooks(Path var0, Set<String> var1) {
      if (var0 != null && Files.isRegularFile(var0)) {
         try (var var2 = Files.lines(var0)) {
            var2.forEach(var1x -> {
               Matcher var2x = RULEBOOK_REF.matcher(var1x);

               while (var2x.find()) {
                  var1.add(var2x.group(1) != null ? var2x.group(1) : var2x.group(2));
               }
            });
         } catch (Exception var7) {
            logger.debug("rulebook scan failed for {}: {}", var0, var7.getMessage());
         }
      }
   }

   private static String gradedScenarios(Path var0, List<Map<String, Object>> var1, int var2) {
      LinkedHashMap var3 = new LinkedHashMap();

      for (Map var5 : var1) {
         if (var5.get("name") != null) {
            var3.put(String.valueOf(var5.get("name")), Boolean.TRUE.equals(var5.get("passed")));
         }
      }

      StringBuilder var13 = new StringBuilder();

      try {
         Object var14 = Json.parseStrict(Files.readString(var0.resolve("scenarios.json")), true);
         if (!(var14 instanceof List)) {
            return null;
         }

         for (Object var8 : (List)var14) {
            if (var8 instanceof Map var9) {
               Boolean var10 = (Boolean)var3.get(String.valueOf(var9.get("_label")));
               if (var10 == null) {
                  var10 = (Boolean)var3.get(String.valueOf(var9.get("_id")));
               }

               String var11 = var10 == null ? "----" : (var10 ? "PASS" : "FAIL");
               var13.append(var11).append(' ').append(Json.of(var9)).append('\n');
               if (var13.length() > var2) {
                  return var13.substring(0, var2) + "\n… (rows truncated — read scenarios.json)\n";
               }
            }
         }
      } catch (Exception var12) {
         logger.debug("no gradable rows in {}: {}", var0, var12.getMessage());
         return null;
      }

      return var13.isEmpty() ? null : var13.toString();
   }

   private static String readCapped(Path var0, int var1) {
      if (var1 > 0 && Files.isRegularFile(var0)) {
         try {
            String var2 = Files.readString(var0);
            return var2.length() <= var1 ? var2 : var2.substring(0, var1) + "\n… (truncated at " + var1 + " chars — read the rest with File.read)";
         } catch (Exception var3) {
            return null;
         }
      } else {
         return null;
      }
   }

   private String displayPath(Path var1) {
      Path var2 = var1.toAbsolutePath().normalize();
      Path var3 = this.workingDir.toAbsolutePath().normalize();
      return var2.startsWith(var3) ? var3.relativize(var2).toString().replace('\\', '/') : var2.toString();
   }

   Map<String, Object> author(Object var1, Object var2) {
      if (var1 instanceof String var3 && !var3.isBlank()) {
         String var4 = var3.trim();
         Path var5 = this.runDir(var4).resolve("transcript.jsonl");
         if (!Files.isRegularFile(var5)) {
            return JsApi.notFound("no recording transcript for run '" + var4 + "' — see Harness.runs()");
         } else {
            String var6 = Recording.authoringMaterial(var4, var5);
            if (var6 == null) {
               return JsApi.missing(
                  "recording '"
                     + var4
                     + "' has no replayable actions to author from — inspect it with Recording.get('"
                     + var4
                     + "') (was the app driven while recording?)"
               );
            } else {
               Map var7 = var2 instanceof Map var8 ? cast(var8) : Map.of();
               String var14 = var7.get("name") instanceof String var9 && !var9.isBlank() ? var9.trim().replaceAll("[^A-Za-z0-9_./-]", "-") : "recorded-flow";
               if (!var14.contains("..") && !var14.startsWith("/")) {
                  boolean var15 = Files.isRegularFile(MaxWorkspace.checksDir(this.workingDir).resolve(var14 + ".js"));
                  if (var15 && !Boolean.TRUE.equals(var7.get("overwrite"))) {
                     LinkedHashMap var17 = new LinkedHashMap();
                     var17.put("error", "Harness.author: checks/" + var14 + ".js already exists — pass {overwrite:true} to replace it, or another name");
                     var17.put("code", "EXISTS");
                     var17.put("status", 409);
                     return var17;
                  } else {
                     String var16 = "Author a maintainable karate check from the recorded UI flow below. The deliverable is the ENTRY check `checks/"
                        + var14
                        + ".js` — it must end GREEN via Check.run('"
                        + var14
                        + ".js'), and that green run is your exit. But you OWN the checks/ folder and DECIDE the file layout: do NOT feel obliged to cram everything into one file. You may create as many additional helper / stage checks as the flow warrants (e.g. `checks/"
                        + var14
                        + "/login.js`, `.../navigate.js`, one per wizard step) and compose them in order from the entry check via nested `Check.run('<stage>.js', __arg)` (nested checks share the live `bot`) — splitting a long flow into small, independently-runnable stages is encouraged. Build it stage-by-stage, going green at each stage. Copy the locators verbatim, add waits where a step navigates or loads, default each input on its own (`__arg.user || 'su'`, never `__arg || {…}` — a run with no arg gets an empty, truthy __arg), and pin any ambient entity context (the exact account/policy that was open) by DRIVING the pin live — it is NOT in the recording. The flow runs unconditionally (no guard that can skip it) and the entry check ends by asserting the landed page with a locator or URL you saw in look(); Robot.step narrates, it asserts nothing. A check holds no Robot.success/failure: call Robot.success from the loop once Check.run('"
                        + var14
                        + ".js', null, {reset:true}) is green — the test is replayed with no arg.\n\n"
                        + var6;
                     LinkedHashMap var11 = new LinkedHashMap(var7);
                     var11.put("robot", true);
                     var11.put("author", var4);
                     var11.put("check", var14 + ".js");
                     var11.remove("replaces");
                     if (var15) {
                        var11.put("replaces", true);
                     }

                     var11.putIfAbsent("async", Boolean.TRUE);
                     var11.putIfAbsent("maxIterations", 40);
                     var11.remove("booted");
                     if (this.projectBoot != null && this.projectBoot.get() instanceof Map var12 && "written".equals(var12.get("status"))) {
                        var11.put("booted", var12.get("path"));
                     }

                     Object var18 = this.explore(var16, var11);
                     if (var11.get("booted") != null && var18 != null && ((java.util.Map)var18).get("runId") != null) {
                        var18 = new LinkedHashMap((Map)var18);
                        ((java.util.Map)var18).put("booted", var11.get("booted"));
                     }

                     return (Map<String, Object>)var18;
                  }
               } else {
                  return JsApi.missing(
                     "Harness.author: {name:'" + var14 + "'} must stay under checks/ — a relative name without '..', e.g. 'login' or 'flows/login'"
                  );
               }
            }
         }
      } else {
         return JsApi.missing(
            "Harness.author(recordingRunId[, opts]) requires the recording's run id — record a flow then pass its id (see Harness.runs() / Recording.stop())"
         );
      }
   }

   Map<String, Object> heal(Object var1, Object var2) {
      if (var1 instanceof String var3 && !var3.isBlank()) {
         String var4 = var3.trim();
         if (!var4.endsWith(".js")) {
            return JsApi.missing("Harness.heal currently heals a UI `.js` check only — API/`.feature` heal is deferred. Got '" + var4 + "'.");
         } else {
            String var5 = var4.startsWith("checks/") ? var4.substring("checks/".length()) : var4;
            String var6 = this.readCheckSource(var5);
            if (var6 == null) {
               return JsApi.notFound("no check 'checks/" + var5 + "' to heal — see Check.list()");
            } else {
               String var7 = "Run-or-heal the existing deterministic UI check `checks/"
                  + var5
                  + "`. Run it first; if it is already green you are done, otherwise repair it to honest green per the discipline above. Its current source (your INTENT reference — the stale values are what drifted) is:\n\n```javascript\n"
                  + var6
                  + "\n```";
               Map var8 = var2 instanceof Map var9 ? cast(var9) : Map.of();
               LinkedHashMap var10 = new LinkedHashMap(var8);
               var10.put("robot", true);
               var10.put("heal", true);
               var10.put("check", var5);
               var10.putIfAbsent("async", Boolean.TRUE);
               var10.putIfAbsent("maxIterations", 40);
               return this.explore(var7, var10);
            }
         }
      } else {
         return JsApi.missing("Harness.heal(target[, opts]) requires a check name — e.g. Harness.heal('storefront-ui.js')");
      }
   }

   Map<String, Object> revise(Object var1, Object var2, Object var3) {
      String var4 = "Harness.revise(target, request[, {model?, provider?, maxIterations?}]) changes a check as asked, e.g. Harness.revise('login.js', 'also check that the cart shows 2 items')";
      if (var1 instanceof String var5 && !var5.isBlank()) {
         String var6 = var5.trim();
         if (!var6.endsWith(".js")) {
            return JsApi.missing(
               "Harness.revise changes a `.js` check — '"
                  + var6
                  + "' is not one. A `.feature` is plain text: edit it with File.write, or its tags with File.tag"
            );
         } else if (var2 instanceof String var7 && !var7.isBlank()) {
            String var8 = var7.trim();
            if (var8.length() > 1000) {
               return JsApi.missing(
                  "Harness.revise: the request is " + var8.length() + " characters; say the change in at most 1000 — it rides every iteration of the run"
               );
            } else {
               Map var9 = var3 instanceof Map var10 ? cast(var10) : Map.of();
               if (Boolean.FALSE.equals(var9.get("async"))) {
                  return JsApi.missing("Harness.revise runs async — it records its run and holds the browser; read the outcome with Harness.get(runId)");
               } else {
                  String var14 = var6.startsWith("checks/") ? var6.substring("checks/".length()) : var6;
                  String var11 = this.readCheckSource(var14);
                  if (var11 == null) {
                     return JsApi.notFound("no check 'checks/" + var14 + "' to change — see Check.list()");
                  } else {
                     String var12 = "Change the existing deterministic check `checks/"
                        + var14
                        + "` as asked, per the discipline above. The request:\n\n> "
                        + var8.replace("\n", "\n> ")
                        + "\n\nIts current source:\n\n```javascript\n"
                        + var11
                        + "\n```";
                     LinkedHashMap var13 = new LinkedHashMap(var9);
                     var13.put("robot", true);
                     var13.put("revise", true);
                     var13.put("check", var14);
                     var13.put("request", var8);
                     var13.put("async", Boolean.TRUE);
                     var13.putIfAbsent("maxIterations", 25);
                     return this.explore(var12, var13);
                  }
               }
            }
         } else {
            return JsApi.missing(var4);
         }
      } else {
         return JsApi.missing(var4);
      }
   }

   private byte[] reviseCopy(String var1, Map<String, Object> var2) throws IOException {
      if (!bool(var2, "revise")) {
         return null;
      } else {
         Path var3 = this.reviseTarget(var2);
         ReentrantLock var4 = Check.lockOf(var3);
         var4.lock();

         byte[] var6;
         try {
            byte[] var5 = Files.readAllBytes(var3);
            Files.write(this.store.a(var1).resolve("revise-before.js"), var5);
            var6 = var5;
         } finally {
            var4.unlock();
         }

         return var6;
      }
   }

   private void reviseSettle(Map<String, Object> var1, Map<String, Object> var2, io.karatelabs.ext.max.jsapi.Harness.FileRevisionWatcher var3, boolean var4) {
      if (var3 != null && var3.c != null && !var1.containsKey("changed")) {
         Path var5 = this.reviseTarget(var2);
         ReentrantLock var6 = Check.lockOf(var5);
         var6.lock();

         try {
            byte[] var7 = Files.isRegularFile(var5) ? Files.readAllBytes(var5) : null;
            boolean var8 = !Arrays.equals(var7, var3.c);
            boolean var9 = !var3.a(var7);
            String var10 = "checks/" + var2.get("check") + " was changed outside this run while it worked";
            if (var8 && !var4) {
               if (!var9) {
                  io.karatelabs.ext.common.AtomicFileWriter.a(var5, var3.c);
                  var1.put("restored", true);
                  var8 = false;
               } else {
                  var1.put("restored", false);
                  var10 = var10
                     + ", so it was kept; the copy from before the run is "
                     + this.displayPath(this.store.a(String.valueOf(var1.get("runId"))).resolve("revise-before.js"));
               }
            }

            if (var9) {
               if (var3.f) {
                  var1.put("theirs", this.displayPath(var3.b));
                  var10 = var10 + "; the version the run wrote over is kept at " + this.displayPath(var3.b);
               }

               var1.put("conflict", var10);
            }

            var1.put("changed", var8);
         } catch (Exception var14) {
            logger.warn("could not settle the revise of {}: {}", var5, var14.getMessage());
            var1.put("restoreError", var14.getMessage());
         } finally {
            var6.unlock();
         }
      }
   }

   private static byte[] bytesOrNull(Path var0) {
      try {
         return Files.isRegularFile(var0) ? Files.readAllBytes(var0) : null;
      } catch (IOException var2) {
         return null;
      }
   }

   Map<String, Object> reviseNotUsable(Map<String, Object> var1) {
      if (!bool(var1, "revise")) {
         return null;
      } else {
         String var2 = var1.get("check") instanceof String var3 ? var3 : null;
         if (this.readCheckSource(var2) == null) {
            return JsApi.missing("a revise run changes one existing `.js` check under checks/ — {check:'" + var2 + "'} is not one; see Check.list()");
         } else {
            var var6 = this.runWorkingOn(var2);
            if (var6 == null) {
               return null;
            } else {
               String var7 = String.valueOf(var6.get("runId"));
               LinkedHashMap var5 = new LinkedHashMap();
               var5.put("error", "Harness.revise: run " + var7 + " is working on checks/" + var2 + " — change it once that run is done");
               var5.put("code", "BUSY");
               var5.put("status", 409);
               var5.put("runId", var7);
               return JsApi.next(var5, "Harness.get('" + var7 + "')");
            }
         }
      }
   }

   private Path reviseTarget(Map<String, Object> var1) {
      Path var2 = MaxWorkspace.checksDir(this.workingDir);
      String var3 = String.valueOf(var1.get("check"));
      if (var3.contains("..")) {
         throw new SecurityException("path traversal denied: " + var3);
      } else {
         return MaxPaths.within(var2, var2.resolve(var3));
      }
   }

   private io.karatelabs.ext.max.jsapi.Harness.FileBeforeSnapshot checkBefore(Map<String, Object> var1) {
      String var2 = kindOf(var1);
      if (("author".equals(var2) || "heal".equals(var2)) && var1.get("check") instanceof String var3) {
         Path var8 = MaxWorkspace.checksDir(this.workingDir).toAbsolutePath().normalize();
         Path var5 = var8.resolve(var3).normalize();
         if (!var5.startsWith(var8)) {
            return null;
         } else {
            try {
               return new io.karatelabs.ext.max.jsapi.Harness.FileBeforeSnapshot(var5, Files.isRegularFile(var5) ? Files.readAllBytes(var5) : null);
            } catch (IOException var7) {
               logger.warn("could not read {} before the run: {}", var5, var7.getMessage());
               return null;
            }
         }
      } else {
         return null;
      }
   }

   private static void checkSettle(Map<String, Object> var0, io.karatelabs.ext.max.jsapi.Harness.FileBeforeSnapshot var1) {
      if (var1 != null && !var0.containsKey("changed")) {
         try {
            byte[] var2 = Files.isRegularFile(var1.a()) ? Files.readAllBytes(var1.a()) : null;
            var0.put("changed", !Arrays.equals(var2, var1.b()));
         } catch (IOException var3) {
            logger.warn("could not read {} to settle the run: {}", var1.a(), var3.getMessage());
         }
      }
   }

   private String readCheckSource(String var1) {
      if (var1 != null && var1.endsWith(".js") && !var1.contains("..") && !var1.contains("\n")) {
         Path var2 = MaxWorkspace.checksDir(this.workingDir);

         Path var3;
         try {
            var3 = MaxPaths.within(var2, var2.resolve(var1));
         } catch (SecurityException var6) {
            return null;
         }

         if (!Files.isRegularFile(var3)) {
            return null;
         } else {
            try {
               String var4 = Files.readString(var3).strip();
               return var4.isEmpty() ? null : var4;
            } catch (Exception var5) {
               logger.warn("could not read check source {}: {}", var3, var5.getMessage());
               return null;
            }
         }
      } else {
         return null;
      }
   }

   String resolveTask(String var1, Map<String, Object> var2) {
      String var3 = var1;
      String var4 = this.readProjectMd(var1.trim());
      if (var4 != null) {
         var3 = var4;
      }

      if (var2.get("hints") instanceof String var5 && !var5.isBlank()) {
         String var7 = this.readProjectMd(var5.trim());
         if (var7 != null) {
            var3 = var3 + "\n\nApp-specific hints:\n" + var7;
         }
      }

      if (bool(var2, "study")) {
         var3 = "Before driving the UI, call bot.study() to learn the page's patterns, then act on the dossier it returns (adopt any pattern-pack choreographies/helpers it hands you). Then:\n\n"
            + var3;
      }

      return var3;
   }

   private static boolean namesMdFile(String var0) {
      String var1 = var0.strip();
      return var1.endsWith(".md") && var1.indexOf(32) < 0 && var1.indexOf(10) < 0 && var1.indexOf(9) < 0;
   }

   Map<String, Object> mdRefNotResolvable(String var1, Map<String, Object> var2) {
      if (namesMdFile(var1) && this.readProjectMd(var1) == null) {
         return JsApi.notFound(
            "no markdown file '"
               + var1.strip()
               + "' in this project — a task that names a *.md file is read as a file (its content becomes the goal). Looked under checks/ and the project root; see File.list('**/*.md')"
         );
      } else {
         return var2.get("hints") instanceof String var3 && !var3.isBlank() && this.readProjectMd(var3) == null
            ? JsApi.notFound("no hints file '" + var3.strip() + "' in this project — {hints} names a *.md file appended to the task; see File.list('**/*.md')")
            : null;
      }
   }

   private String readProjectMd(String var1) {
      String var2 = var1.strip();
      if (var2.endsWith(".md") && !var2.contains("..") && !var2.contains("\n")) {
         Path var3 = MaxWorkspace.checksDir(this.workingDir);
         String var4 = var2.startsWith("checks/") ? var2.substring("checks/".length()) : var2;
         String var5 = this.readMdWithin(var3, var4);
         return var5 != null ? var5 : this.readMdWithin(this.workingDir, var2);
      } else {
         return null;
      }
   }

   private String readMdWithin(Path var1, String var2) {
      Path var3;
      try {
         var3 = MaxPaths.within(var1, var1.resolve(var2));
      } catch (SecurityException var6) {
         return null;
      }

      if (!Files.isRegularFile(var3)) {
         return null;
      } else {
         try {
            String var4 = Files.readString(var3).strip();
            return var4.isEmpty() ? null : var4;
         } catch (Exception var5) {
            logger.warn("could not read {}: {}", var3, var5.getMessage());
            return null;
         }
      }
   }

   Map<String, Object> runs() {
      return this.runs(null);
   }

   Map<String, Object> runs(Object var1) {
      List var2 = null;
      int var3 = Integer.MAX_VALUE;
      if (var1 != null) {
         String var4 = "Harness.runs([{selector?, limit?}]) — selector is a path or a list of paths (the exact selection a run recorded), limit a whole number > 0, e.g. Harness.runs({selector: ['checks/login.js'], limit: 5})";
         if (!(var1 instanceof Map var5)) {
            return JsApi.missing(var4);
         }

         for (Object var7 : var5.keySet()) {
            if (!"selector".equals(var7) && !"limit".equals(var7)) {
               return JsApi.missing(var4 + " — unknown key '" + var7 + "'");
            }
         }

         if (var5.containsKey("selector")) {
            var2 = selectorOf(var5.get("selector"));
            if (var2 == null) {
               return JsApi.missing(var4);
            }
         }

         if (var5.containsKey("limit")) {
            if (!(var5.get("limit") instanceof Number var11) || var11.doubleValue() < 1.0 || var11.doubleValue() != Math.rint(var11.doubleValue())) {
               return JsApi.missing(var4);
            }

            var3 = (int)Math.min(2147483647L, var11.longValue());
         }
      }

      ArrayList var8 = new ArrayList();

      for (Map var12 : this.store.b()) {
         if (var8.size() >= var3) {
            break;
         }

         if (var2 == null || var2.equals(var12.get("selector"))) {
            var8.add(var12);
         }
      }

      String var10 = this.run.isRunning() ? this.run.id() : null;

      for (Map var16 : (Iterable<Map>) (Iterable<?>) (var8)) {
         this.reconcile(var16, var10);
      }

      LinkedHashMap var14 = new LinkedHashMap();
      var14.put("runs", var8);
      var14.put("count", var8.size());
      String var17 = this.liveRunId;
      var14.put("active", var10 != null ? var10 : var17);
      var14.put("inFlight", List.copyOf(this.inFlightIds(var10)));
      return var14;
   }

   public Map<String, Object> count() {
      String var1 = this.run.isRunning() ? this.run.id() : null;
      LinkedHashMap var2 = new LinkedHashMap();
      var2.put("count", this.store.c());
      var2.put("active", var1 != null ? var1 : this.liveRunId);
      var2.put("inFlight", List.copyOf(this.inFlightIds(var1)));
      return var2;
   }

   public Map<String, Boolean> recorded(List<String> var1) {
      LinkedHashMap var2 = new LinkedHashMap();

      for (String var4 : var1) {
         var2.put(var4, runFolder(var4) && this.recordable(var4));
      }

      return var2;
   }

   public List<Map<String, Object>> recordedSteps(String var1) {
      return runFolder(var1) ? Recording.digest(this.store.a(var1).resolve("transcript.jsonl")).f() : List.of();
   }

   private static boolean runFolder(String var0) {
      return var0 != null && !var0.isBlank() && !var0.contains("/") && !var0.contains("\\") && !var0.startsWith(".");
   }

   private boolean recordable(String var1) {
      Path var2 = this.store.a(var1).resolve("transcript.jsonl");

      String var3;
      try {
         if (!Files.isRegularFile(var2)) {
            return false;
         }

         var3 = var1 + "\n" + Files.size(var2) + "\n" + Files.getLastModifiedTime(var2).toMillis();
      } catch (IOException var5) {
         return false;
      }

      Boolean var4 = this.recordableByTranscript.get(var3);
      if (var4 == null) {
         var4 = !Recording.digest(var2).a();
         this.recordableByTranscript.put(var3, var4);
      }

      return var4;
   }

   private Set<String> inFlightIds(String var1) {
      io.karatelabs.ext.max.jsapi.Harness.ExecutionEvidenceContext var2 = this.bracket;
      LinkedHashSet var3 = new LinkedHashSet();

      for (String var5 : Arrays.asList(var1, this.liveRunId, this.leasedRunId, var2 == null ? null : var2.d)) {
         if (var5 != null) {
            var3.add(var5);
         }
      }

      if (this.activeRuns != null) {
         var3.addAll(new TreeSet<>(this.activeRuns.ids()));
      }

      return var3;
   }

   public Map<String, Object> runWorkingOn(String var1) {
      if (var1 != null && !var1.isBlank()) {
         String var2 = io.karatelabs.ext.max.jsapi.RunEvidenceInspector.a(Check.canonicalName(var1));
         String var3 = this.run.isRunning() ? this.run.id() : null;

         for (String var5 : this.inFlightIds(var3)) {
            var var6 = this.store.b(var5);
            if (var6 != null && holds(var6, var2)) {
               LinkedHashMap var7 = new LinkedHashMap();
               var7.put("runId", var5);
               String var8 = recordKind(var6);
               if (var8 != null) {
                  var7.put("kind", var8);
               }

               return var7;
            }
         }

         String var9 = this.run.check();
         return var3 != null && var9 != null && var2.equals(io.karatelabs.ext.max.jsapi.RunEvidenceInspector.a(Check.canonicalName(var9))) ? new LinkedHashMap<>(Map.of("runId", var3)) : null;
      } else {
         return null;
      }
   }

   // $VF: One or more variable merging failures!
   // $VF: Could not properly define all variable types!
   // Please report this to the Vineflower issue tracker, at https://github.com/Vineflower/vineflower/issues with a copy of the class file (if you have the rights to distribute it!)
   static boolean holds(Map<String, Object> var0, String var1) {
      if (var0.get("check") instanceof String var2 && !var2.isBlank() && var1.equals(io.karatelabs.ext.max.jsapi.RunEvidenceInspector.a(Check.canonicalName(var2)))) {
         return true;
      } else {
         for (Object var10 : var0.get("selector") instanceof List ? (List)var0.get("selector") : List.of()) {
            if (var10 instanceof String var13 && var1.equals(io.karatelabs.ext.max.jsapi.RunEvidenceInspector.a(var13))) {
               return true;
            }
         }

         for (Object var12 : var0.get("members") instanceof List ? (List)var0.get("members") : List.of()) {
            if (var12 instanceof Map var15 && var15.get("path") instanceof String var5 && var1.equals(io.karatelabs.ext.max.jsapi.RunEvidenceInspector.a(var5))) {
               return true;
            }
         }

         return false;
      }
   }

   private static List<String> selectorOf(Object var0) {
      List var1 = var0 instanceof List var3 ? var3 : (var0 instanceof String var2 ? List.of(var2) : null);
      if (var1 != null && !var1.isEmpty()) {
         ArrayList var6 = new ArrayList();
         Iterator var7 = var1.iterator();

         while (var7.hasNext()) {
            if (!(var7.next() instanceof String var5) || var5.isBlank()) {
               return null;
            }

            var6.add(var5.trim());
         }

         return var6;
      } else {
         return null;
      }
   }

   Map<String, Object> get(String var1) {
      if (var1 != null && !var1.isBlank()) {
         var var2 = this.store.b(var1);
         if (var2 == null) {
            return JsApi.notFound("no run '" + var1 + "' — see Harness.runs()");
         } else {
            this.reconcile(var2, this.run.isRunning() ? this.run.id() : null);
            var2.put("screenshots", screenshotManifest(this.store.a(var1).resolve("screenshots")));
            var var3 = io.karatelabs.ext.max.jsapi.RunEvidenceInspector.a(this.store.a(var1));
            if (!var3.isEmpty()) {
               var2.put("verdicts", var3);
            }

            boolean var4 = Boolean.TRUE.equals(var2.get("live"));
            if (!var4 && var2.get("members") instanceof List var5 && !var5.isEmpty() && Files.isRegularFile(MaxPaths.eventsFile(this.store.a(var1)))) {
               ArrayList var12 = new ArrayList();

               for (Object var8 : var5) {
                  if (var8 instanceof Map var9) {
                     var12.add(cast(var9));
                  }
               }

               List var15 = io.karatelabs.ext.max.jsapi.RunEvidenceInspector.a(this.store.a(var1), var12);
               if (!var15.isEmpty()) {
                  var2.put("memberVerdicts", var15);
               }
            }

            if (!var4) {
               io.karatelabs.ext.max.jsapi.RunEvidenceInspector.CheckEvidenceSummary var10 = io.karatelabs.ext.max.jsapi.RunEvidenceInspector.b(this.store.a(var1));
               if (var10.b() + var10.c() > 0) {
                  var2.put("checks", var10.a());
                  LinkedHashMap var13 = new LinkedHashMap();
                  var13.put("passed", var10.b());
                  var13.put("failed", var10.c());
                  var2.put("checkTotals", var13);
               }
            }

            Path var11 = this.runDir(var1).resolve("transcript.jsonl");
            if (Files.isRegularFile(var11) && !Recording.digest(var11).a()) {
               var2.put("recordable", true);
            } else {
               var2.remove("recordable");
            }

            if (capturedTraffic(this.runDir(var1).resolve("traffic.jsonl"))) {
               var2.put("traffic", true);
            }

            if (!var4
               && List.of("author", "heal", "revise").contains(String.valueOf(var2.get("kind")))
               && var2.get("check") instanceof String var14
               && var2.get("finishedMs") instanceof Number var16) {
               var var19 = this.firstReplayAfter(var14, var16.longValue());
               if (var19 != null) {
                  var2.put("verifiedBy", var19);
               }
            }

            return var2;
         }
      } else {
         return JsApi.missing("Harness.get(id) requires a run id — see Harness.runs()");
      }
   }

   private static boolean capturedTraffic(Path var0) {
      if (!Files.isRegularFile(var0)) {
         return false;
      } else {
         try {
            boolean var2;
            try (var var1 = Files.lines(var0)) {
               var2 = var1.anyMatch(var0x -> var0x.contains("\"row\":\"exchange\""));
            }

            return var2;
         } catch (UncheckedIOException | IOException var6) {
            return false;
         }
      }
   }

   Map<String, Object> firstReplayAfter(String var1, long var2) {
      Path var4 = this.projectGraph;
      if (var4 != null && Files.isRegularFile(var4)) {
         String var5 = Check.definitionId(var1);
         LinkedHashMap var6 = null;
         long var7 = Long.MAX_VALUE;

         for (Map var10 : io.karatelabs.ext.common.jsapi.CoverageGraphAccessor.a(io.karatelabs.ext.common.jsapi.CoverageGraphAccessor.a(var4).c(), "executions")) {
            if (var5.equals(var10.get("test")) && !"running".equals(var10.get("lifecycle"))) {
               long var11 = var10.get("startedAt") instanceof Number var14
                  ? var14.longValue()
                  : (var10.get("endedAt") instanceof Number var13 ? var13.longValue() : 0L);
               if (var11 > var2 && var11 < var7) {
                  var7 = var11;
                  String var16 = String.valueOf(var10.get("outcome"));
                  var6 = new LinkedHashMap();
                  var6.put("runId", var10.get("runId"));
                  var6.put("status", !"passed".equals(var16) && !"failed".equals(var16) ? "notrun" : var16);
               }
            }
         }

         return var6;
      } else {
         return null;
      }
   }

   private static List<Map<String, Object>> screenshotManifest(Path var0) {
      ArrayList var1 = new ArrayList();
      Path var2 = var0.resolve("screenshots.jsonl");
      if (!Files.isRegularFile(var2)) {
         return var1;
      } else {
         try {
            for (String var4 : Files.readAllLines(var2)) {
               if (!var4.isBlank()) {
                  try {
                     if (Json.of(var4).value() instanceof Map var5 && var5.get("filename") != null) {
                        LinkedHashMap var11 = new LinkedHashMap();
                        var11.put("filename", String.valueOf(var5.get("filename")));
                        if (var5.get("title") != null) {
                           var11.put("title", String.valueOf(var5.get("title")));
                        }

                        var11.put("ts", var5.get("timestamp") instanceof Number var7 ? var7.longValue() : 0L);
                        var1.add(var11);
                     }
                  } catch (Exception var9) {
                  }
               }
            }
         } catch (Exception var10) {
            logger.debug("screenshot manifest read failed: {}", var10.getMessage());
         }

         return var1;
      }
   }

   Map<String, Object> providers() {
      return this.providers(this.env);
   }

   private Map<String, Object> providers(Function<String, String> var1) {
      io.karatelabs.ext.agent.llm.LlmRegistry var2 = io.karatelabs.ext.agent.llm.LlmRegistry.a(this.workingDir, var1);
      ArrayList var3 = new ArrayList();
      String var4 = null;

      for (io.karatelabs.ext.agent.llm.LlmRegistry.LlmProviderRegistration var6 : var2.a()) {
         io.karatelabs.ext.agent.llm.LlmProvider var7 = var6.b;
         if (var7 != null) {
            String var8 = (String)var1.apply(var7 == io.karatelabs.ext.agent.llm.LlmProvider.OLLAMA ? var7.getBaseUrlEnvVar() : var7.getEnvVar());
            if (var8 != null && !var8.isBlank()) {
               var var9 = resolveModels(var6, var7, var1);
               if (!var9.isEmpty()) {
                  LinkedHashMap var10 = new LinkedHashMap();
                  var10.put("name", var6.a);
                  var10.put("models", var9);
                  var10.put("default", var6.e);
                  var3.add(var10);
                  if (var6.e) {
                     var4 = (String)((Map)var9.get(0)).get("id");
                  }
               }
            }
         }
      }

      if (var4 == null && !var3.isEmpty()) {
         List var11 = (List)((Map)var3.get(0)).get("models");
         var4 = (String)((Map)var11.get(0)).get("id");
      }

      LinkedHashMap var12 = new LinkedHashMap();
      var12.put("providers", var3);
      var12.put("defaultModel", var4 != null ? var4 : "sonnet");
      return var12;
   }

   Map<String, Object> ping(Object var1) {
      return this.ping(var1, this.env);
   }

   Map<String, Object> ping(Object var1, Function<String, String> var2) {
      Map var3 = var1 instanceof Map var4 ? cast(var4) : Map.of();
      Object[] var28 = this.select(
         var3.get("model") instanceof String var5 && !var5.isBlank() ? var5.trim() : null, var3.get("provider") instanceof String var29 ? var29 : null, var2
      );
      io.karatelabs.ext.agent.llm.LlmProvider var30 = (io.karatelabs.ext.agent.llm.LlmProvider)var28[0];
      String var32 = (String)var28[1];
      String var7 = io.karatelabs.ext.agent.llm.ClaudeModel.resolve(var32, var30);
      LinkedHashMap var8 = new LinkedHashMap();
      var8.put("provider", var30.name().toLowerCase(Locale.ROOT));
      putIfPresent(var8, "model", var7);
      String var9 = (String)var2.apply(var30.getEnvVar());
      if ((var9 == null || var9.isBlank()) && var30 != io.karatelabs.ext.agent.llm.LlmProvider.OLLAMA) {
         return fail(var8, null, io.karatelabs.ext.agent.llm.LlmClient.a(var30), io.karatelabs.ext.agent.llm.LlmException.a(var30));
      } else {
         long var10 = System.currentTimeMillis();
         io.karatelabs.ext.agent.llm.LlmClient var12 = null;

         Map var15;
         try {
            var12 = io.karatelabs.ext.agent.harness.AgentHarness.a(var32, var30, var2).apiKey(var9 != null && !var9.isBlank() ? var9 : "ollama").maxOutputTokens(1).build();
            var12.a("ping", null);
            LinkedHashMap var13 = new LinkedHashMap();
            var13.put("ok", true);
            var13.putAll(var8);
            var13.put("latencyMs", System.currentTimeMillis() - var10);
            return var13;
         } catch (Exception var26) {
            io.karatelabs.ext.agent.llm.LlmException var14 = io.karatelabs.ext.agent.llm.LlmException.a(var26);
            if (var14 != null) {
               return fail(var8, var14.b() > 0 ? var14.b() : null, var14.c() != null ? var14.c() : var14.getMessage(), var14.g());
            }

            var15 = fail(var8, null, String.valueOf(var26.getMessage()), null);
         } finally {
            if (var12 != null) {
               try {
                  var12.close();
               } catch (Exception var25) {
                  logger.debug("ping client close failed: {}", var25.toString());
               }
            }
         }

         return var15;
      }
   }

   private static Map<String, Object> fail(Map<String, Object> var0, Integer var1, String var2, String var3) {
      LinkedHashMap var4 = new LinkedHashMap();
      var4.put("ok", false);
      var4.putAll(var0);
      if (var1 != null) {
         var4.put("status", var1);
      }

      var4.put("error", var2);
      if (var3 != null) {
         var4.put("hint", var3);
      }

      return var4;
   }

   Map<String, Object> draft(Object var1, Object var2) {
      return this.draft(var1, var2, this.env);
   }

   Map<String, Object> draft(Object var1, Object var2, Function<String, String> var3) {
      var var4 = draftRefusal(var1);
      if (var4 != null) {
         return var4;
      } else {
         String var5 = (String)var1;
         Map var6 = var2 instanceof Map var7 ? cast(var7) : Map.of();
         Object[] var16 = this.select(
            var6.get("model") instanceof String var8 && !var8.isBlank() ? var8.trim() : null, var6.get("provider") instanceof String var17 ? var17 : null, var3
         );
         io.karatelabs.ext.agent.llm.LlmProvider var18 = (io.karatelabs.ext.agent.llm.LlmProvider)var16[0];
         String var20 = (String)var16[1];
         LinkedHashMap var10 = new LinkedHashMap();
         var10.put("provider", var18.name().toLowerCase(Locale.ROOT));
         putIfPresent(var10, "model", io.karatelabs.ext.agent.llm.ClaudeModel.resolve(var20, var18));
         String var11 = (String)var3.apply(var18.getEnvVar());
         if ((var11 == null || var11.isBlank()) && var18 != io.karatelabs.ext.agent.llm.LlmProvider.OLLAMA) {
            return fail(var10, null, io.karatelabs.ext.agent.llm.LlmClient.a(var18), io.karatelabs.ext.agent.llm.LlmException.a(var18));
         } else if (!DRAFTING.compareAndSet(false, true)) {
            return fail(var10, 409, "another draft is being written: try again in a moment", null);
         } else {
            Map var12;
            try {
               var12 = this.askDraft(
                  var5,
                  var6,
                  var10,
                  () -> io.karatelabs.ext.agent.harness.AgentHarness.a(var20, var18, var3).apiKey(var11 != null && !var11.isBlank() ? var11 : "ollama").maxOutputTokens(8192).build(),
                  true
               );
            } finally {
               DRAFTING.set(false);
            }

            return var12;
         }
      }
   }

   private static Map<String, Object> draftRefusal(Object var0) {
      if (var0 instanceof String var1 && !var1.isBlank()) {
         return var1.length() > 12000 ? JsApi.missing("Harness.draft takes at most 12000 characters: draft one requirements file at a time") : null;
      } else {
         return JsApi.missing("Harness.draft(markdown[, opts]) requires the requirements draft as text");
      }
   }

   public Map<String, Object> draft(Object var1, Object var2, io.karatelabs.ext.agent.llm.LlmClient var3) {
      var var4 = draftRefusal(var1);
      if (var4 != null) {
         return var4;
      } else {
         String var5 = (String)var1;
         Map var6 = var2 instanceof Map var7 ? cast(var7) : Map.of();
         LinkedHashMap var12 = new LinkedHashMap();
         var12.put("provider", var3.a().name().toLowerCase(Locale.ROOT));
         putIfPresent(var12, "model", var3.b());
         if (!DRAFTING.compareAndSet(false, true)) {
            return fail(var12, 409, "another draft is being written: try again in a moment", null);
         } else {
            Map var8;
            try {
               var8 = this.askDraft(var5, var6, var12, () -> var3, false);
            } finally {
               DRAFTING.set(false);
            }

            return var8;
         }
      }
   }

   private Map<String, Object> askDraft(String var1, Map<String, Object> var2, Map<String, Object> var3, Supplier<io.karatelabs.ext.agent.llm.LlmClient> var4, boolean var5) {
      String var6 = "Taken ids: " + takenIds(var2.get("taken")) + "\n\nDraft:\n" + var1;
      final AtomicBoolean var7 = new AtomicBoolean();
      Timer var8 = new Timer("harness-draft-deadline", true);
      io.karatelabs.ext.agent.llm.LlmClient var9 = null;

      Map var39;
      try {
         var9 = (io.karatelabs.ext.agent.llm.LlmClient)var4.get();
         final io.karatelabs.ext.agent.llm.RedactingLlmClient var10 = new io.karatelabs.ext.agent.llm.RedactingLlmClient(
            (io.karatelabs.ext.agent.llm.LlmClient)(var9 instanceof io.karatelabs.ext.agent.llm.RedactingLlmClient var36 ? var36.e() : var9), this.sanitizer
         );
         var8.schedule(new TimerTask() {
            @Override
            public void run() {
               var7.set(true);
               var10.c();
            }
         }, this.draftDeadlineMs, 50L);
         io.karatelabs.ext.agent.llm.LlmResponse var37 = var10.a(
            var6,
            "Rewrite the requirements draft below in the karate requirements markdown format. Answer with the markdown only: no preamble, no code fence.\n\nEach requirement:\n#### <ID>: <what must be true, in one line>\n@criticality=<high|medium|low>\n\n**Acceptance:**\n- 1: WHEN <trigger> THE SYSTEM SHALL <observable response>\n\nRules:\n- Keep every requirement the draft holds and its meaning; add none it does not ask for. Keep the author's words where they are clear.\n- Ids unique: keep the draft's, renumber a repeated one to the next free number of its prefix, never use a taken id.\n- Keep a @criticality the author gave; where the draft gives none, write no @criticality line. Keep the draft's other @key=value tags; never write @status=approved.\n- One EARS line per acceptance criterion, specific and checkable from outside the system. Where a criterion is a placeholder or missing, write the ones the title implies.\n- Keep a \"# \" title line the draft has.\n"
         );
         if (var37.finishReason() == io.karatelabs.ext.agent.llm.LlmResponse.FinishReason.MAX_TOKENS) {
            return fail(var3, null, "the AI's answer was cut off before its end: draft a shorter text", null);
         }

         if (var37.finishReason() == io.karatelabs.ext.agent.llm.LlmResponse.FinishReason.END_TURN || var37.finishReason() == io.karatelabs.ext.agent.llm.LlmResponse.FinishReason.STOP_SEQUENCE) {
            String var40 = markDraft(answerOf(var37.text()));
            io.karatelabs.ext.requirements.model.RequirementsBundle var42 = io.karatelabs.ext.requirements.RequirementsMarkdownParser.a(var40);
            if (var42.d().isEmpty()) {
               return fail(var3, null, "the AI's answer holds no requirement in the requirements format", null);
            }

            ArrayList var14 = new ArrayList();

            for (io.karatelabs.ext.requirements.model.RequirementNode var16 : var42.d()) {
               if (!"draft".equals(var16.i().get("ai")) || "approved".equalsIgnoreCase(var16.f())) {
                  return fail(var3, null, "the AI's answer could not be marked as AI work: try again", null);
               }

               var14.add(var16.b());
            }

            LinkedHashMap var44 = new LinkedHashMap();
            var44.put("ok", true);
            var44.putAll(var3);
            var44.put("markdown", var40);
            var44.put("count", var14.size());
            var44.put("ids", var14);
            return var44;
         }

         var39 = fail(var3, null, "the AI's answer did not finish: try again", null);
      } catch (Exception var33) {
         if (var7.get()) {
            return fail(var3, null, "the AI did not answer within " + this.draftDeadlineMs / 1000L + " s", null);
         }

         io.karatelabs.ext.agent.llm.LlmException var11 = io.karatelabs.ext.agent.llm.LlmException.a(var33);
         if (var11 != null) {
            var39 = fail(var3, var11.b() > 0 ? var11.b() : null, var11.c() != null ? var11.c() : var11.getMessage(), var11.g());
            putIfPresent(var39, "errorShort", var11.f());
            return var39;
         }

         return fail(var3, null, String.valueOf(var33.getMessage()), null);
      } finally {
         var8.cancel();
         if (var9 != null && var5) {
            try {
               var9.close();
            } catch (Exception var32) {
               logger.debug("draft client close failed: {}", var32.toString());
            }
         }
      }

      return var39;
   }

   // $VF: One or more variable merging failures!
   // $VF: Could not properly define all variable types!
   // Please report this to the Vineflower issue tracker, at https://github.com/Vineflower/vineflower/issues with a copy of the class file (if you have the rights to distribute it!)
   static String takenIds(Object var0) {
      LinkedHashMap var1 = new LinkedHashMap();
      LinkedHashSet var2 = new LinkedHashSet();

      for (Object var11 : var0 instanceof List ? (List)var0 : List.of()) {
         String var5 = var11 == null ? "" : String.valueOf(var11).trim();
         Matcher var6 = NUMBERED.matcher(var5);
         if (var6.matches()) {
            ((java.util.Set)(var1.computeIfAbsent(new io.karatelabs.ext.max.jsapi.Harness.NumberedIdPrefix(var6.group(1), var6.group(2).length()), var0x -> new TreeSet<>()))).add(Long.parseLong(var6.group(2)));
         } else if (!var5.isEmpty()) {
            var2.add(var5);
         }
      }

      ArrayList var10 = new ArrayList();

      for (Entry var14 : (Iterable<Entry>) (Iterable<?>) (var1.entrySet())) {
         ArrayList<Long> var16 = new ArrayList((Collection)var14.getValue());
         int var7 = 0;

         while (var7 < var16.size()) {
            int var8 = var7;

            while (var8 + 1 < var16.size() && var16.get(var8 + 1) == var16.get(var8) + 1L) {
               var8++;
            }

            if (var8 - var7 + 1 >= 3) {
               var10.add(((io.karatelabs.ext.max.jsapi.Harness.NumberedIdPrefix)var14.getKey()).a((Long)var16.get(var7)) + " to " + ((io.karatelabs.ext.max.jsapi.Harness.NumberedIdPrefix)var14.getKey()).a((Long)var16.get(var8)));
            } else {
               for (int var9 = var7; var9 <= var8; var9++) {
                  var10.add(((io.karatelabs.ext.max.jsapi.Harness.NumberedIdPrefix)var14.getKey()).a((Long)var16.get(var9)));
               }
            }

            var7 = var8 + 1;
         }
      }

      var10.addAll(var2);
      StringBuilder var13 = new StringBuilder();

      for (String var17 : (Iterable<String>) (Iterable<?>) (var10)) {
         if (var13.length() + var17.length() + 2 > 12000) {
            return var13.append(", and more").toString();
         }

         var13.append(var13.isEmpty() ? "" : ", ").append(var17);
      }

      return var13.isEmpty() ? "none" : var13.toString();
   }

   static String answerOf(String var0) {
      String var1 = THINK.matcher(var0 == null ? "" : var0).replaceAll("");
      ArrayList var2 = new ArrayList();
      ArrayList var3 = new ArrayList();
      Matcher var4 = SUB_HEADING.matcher(var1);

      while (var4.find()) {
         var2.add(var4.start());
         if (io.karatelabs.ext.requirements.RequirementsMarkdownParser.c(var4.group(1).trim())) {
            var3.add(var4.start());
         }
      }

      ArrayList var5 = var3.isEmpty() ? var2 : var3;
      int var6 = var5.isEmpty() ? -1 : (Integer)var5.get(0);
      int var7 = var5.isEmpty() ? -1 : (Integer)var5.get(var5.size() - 1);
      Matcher var8 = FENCE.matcher(var1);
      int var9 = 0;
      int var10 = -1;
      ArrayList var11 = new ArrayList();

      while (var6 >= 0 && var8.find() && (var8.start() < var6 || var9 > 0)) {
         boolean var12 = var8.group(1).isBlank();
         if (var8.start() < var6) {
            if (var12 && var9 > 0) {
               var9--;
            } else if (var9++ == 0) {
               var10 = var8.end();
            }
         } else if (var12 && var9 > 1) {
            var9--;
         } else {
            if (var12 && var8.start() > var7) {
               var11.add(var8.start());
            }

            var9++;
         }
      }

      if (var9 == 0) {
         Matcher var17 = FIRST_HEADING.matcher(var1);
         return (var17.find() ? var1.substring(var17.start()) : var1).strip() + "\n";
      } else {
         int var16 = var11.isEmpty() ? var1.length() : (Integer)var11.get(var11.size() - 1);
         int var13 = criteriaIn(var1.substring(var10, var16));

         for (int var15 : (Iterable<java.lang.Integer>) (Iterable<?>) (var11)) {
            if (criteriaIn(var1.substring(var10, var15)) == var13) {
               var16 = var15;
               break;
            }
         }

         return var1.substring(var10, var16).strip() + "\n";
      }
   }

   private static int criteriaIn(String var0) {
      int var1 = 0;

      for (io.karatelabs.ext.requirements.model.RequirementNode var3 : io.karatelabs.ext.requirements.RequirementsMarkdownParser.a(var0).d()) {
         var1 += var3.m().size();
      }

      return var1;
   }

   static String markDraft(String var0) {
      var var1 = new ArrayList<>(List.of(var0.split("\n", -1)));

      for (int var2 = 0; var2 < var1.size(); var2++) {
         String var3 = (String)var1.get(var2);
         Matcher var4 = LEGACY_TAGS.matcher(var3);
         if (var4.matches()) {
            ArrayList var17 = new ArrayList();
            boolean var19 = false;

            for (String var10 : var4.group(2).split(",")) {
               String var11 = var10.trim().replaceAll("\\s+", "");
               if (var11.toLowerCase().startsWith("ai=")) {
                  if (!var19) {
                     var17.add("ai=draft");
                     var19 = true;
                  }
               } else if (!var11.equalsIgnoreCase("status=approved")) {
                  var17.add(var10.trim());
               }
            }

            var1.set(var2, var4.group(1) + String.join(", ", var17));
         } else if (var3.trim().startsWith("@") && var3.contains("=")) {
            Matcher var5 = AI_TAG.matcher(APPROVED.matcher(var3).replaceAll(""));
            StringBuilder var6 = new StringBuilder();

            for (boolean var7 = true; var5.find(); var7 = false) {
               var5.appendReplacement(var6, var7 ? "$1@ai=draft" : "$1");
            }

            var1.set(var2, var5.appendTail(var6).toString().replaceAll("\\s{2,}", " ").stripTrailing());
         }
      }

      ArrayList var12 = new ArrayList();

      for (io.karatelabs.ext.requirements.model.RequirementNode var15 : io.karatelabs.ext.requirements.RequirementsMarkdownParser.a(String.join("\n", var1)).d()) {
         var12.add(var15.r() - 1);
      }

      var12.sort(Comparator.reverseOrder());

      for (int var16 : (Iterable<java.lang.Integer>) (Iterable<?>) (var12)) {
         int var18 = var16 + 1;

         while (var18 < var1.size() && ((String)var1.get(var18)).isBlank()) {
            var18++;
         }

         String var20 = var18 < var1.size() ? ((String)var1.get(var18)).trim() : "";
         if (!var20.startsWith("@") || !var20.contains("=")) {
            var1.add(var16 + 1, "@ai=draft");
         } else if (!AI_MARK.matcher(var20).find()) {
            var1.set(var18, ((String)var1.get(var18)).stripTrailing() + " @ai=draft");
         }
      }

      return String.join("\n", var1);
   }

   private static List<Map<String, Object>> resolveModels(io.karatelabs.ext.agent.llm.LlmRegistry.LlmProviderRegistration var0, io.karatelabs.ext.agent.llm.LlmProvider var1, Function<String, String> var2) {
      ArrayList var3 = new ArrayList();
      if (var0.d) {
         if (var1 == io.karatelabs.ext.agent.llm.LlmProvider.OLLAMA) {
            for (String var5 : io.karatelabs.ext.agent.llm.LlmRegistry.a((String)var2.apply(var1.getBaseUrlEnvVar()))) {
               var3.add(modelRow(var5, var5, var1));
            }
         } else {
            for (String var8 : fallbackModels(var1)) {
               var3.add(modelRow(var8, shortLabel(var8), var1));
            }
         }
      } else if (var0.c != null) {
         for (String var9 : (Iterable<String>) (Iterable<?>) (var0.c)) {
            var3.add(modelRow(var9, shortLabel(var9), var1));
         }
      }

      return var3;
   }

   static List<String> fallbackModels(io.karatelabs.ext.agent.llm.LlmProvider var0) {
      return switch (var0) {
         case ANTHROPIC -> List.of("sonnet", "haiku", "opus");
         case OPENAI -> io.karatelabs.ext.agent.llm.LlmRegistry.b(System::getenv);
         case OPENROUTER -> io.karatelabs.ext.agent.llm.LlmRegistry.d;
         default -> List.of();
      };
   }

   private static String shortLabel(String var0) {
      if ("google/gemma-4-26b-a4b-it".equals(var0)) {
         return "gemma";
      } else if ("qwen/qwen3.6-35b-a3b".equals(var0)) {
         return "qwen";
      } else {
         int var1 = var0.lastIndexOf(47);
         return var1 >= 0 && var1 < var0.length() - 1 ? var0.substring(var1 + 1) : var0;
      }
   }

   private static Map<String, Object> modelRow(String var0, String var1, io.karatelabs.ext.agent.llm.LlmProvider var2) {
      LinkedHashMap var3 = new LinkedHashMap();
      var3.put("id", var0);
      var3.put("label", var1);
      var3.put("resolved", io.karatelabs.ext.agent.llm.ClaudeModel.resolve(var0, var2));
      return var3;
   }

   Map<String, Object> stop(String var1) {
      if (this.run.isRunning() && !this.recording) {
         this.run.stop();
         return this.run.toMap();
      } else {
         String var2 = this.liveRunId;
         if (var2 != null) {
            this.clearedVars = false;
            if (this.hostsLive(this.bracket) && this.closeBrowser != null) {
               try {
                  this.closeBrowser.run();
               } catch (Exception var4) {
                  logger.warn("could not close the live browser for {}: {}", var2, var4.getMessage());
               }
            }

            this.settleLive(var1 != null && !var1.isBlank() ? var1 : "completed");
            var var3 = this.get(var2);
            if (this.clearedVars && var3 != null) {
               var3.put(
                  "varsCleared",
                  "the live run has settled, and with it EVERY top-level `var` and helper in this session — not only the ones this run created: bindings are working memory scoped to a live run, so anything you defined BEFORE it started is gone too (`bot`, captured handles, helper fns; namespaces and help() remain). Re-create what you still need, and capture RESULTS you want to keep before you stop — read a value out, then re-assign it after this call."
               );
            }

            return var3;
         } else {
            return JsApi.missing("nothing to stop — no loop run or live run is in flight; see Harness.status()");
         }
      }
   }

   Map<String, Object> takeControl() {
      if (!this.run.isRunning()) {
         return JsApi.missing("no loop run is in flight to take control of — Harness.status()");
      } else {
         this.run.pause();
         this.publishHandoff("human", null);
         Thread.ofVirtual().name("intervention-" + this.run.id()).start(() -> {
            try {
               if (!this.run.awaitLoopPaused(10000L)) {
                  logger.warn("take control: loop {} did not pause within 10s — capture not attached", this.run.id());
                  return;
               }

               BotDriver var1 = this.run.loopBotDriver();
               if (var1 == null) {
                  logger.debug("take control: loop {} has no live browser — nothing to capture", this.run.id());
                  return;
               }

               String var2 = this.run.id();
               Path var3 = this.run.transcript();
               LiveRunSink var4 = new LiveRunSink((var3x, var4x) -> this.recordIntervention(var2, var3, var3x, var4x));
               io.karatelabs.ext.robot.RevealRecorder var5 = new io.karatelabs.ext.robot.RevealRecorder(var4, null);
               this.interventionSink = var4;
               this.interventionBot = var1;
               var1.startRecording(var5::a, var5::c);
               logger.debug("intervention capture attached to loop {}", var2);
            } catch (Exception var6) {
               logger.warn("take control: capture attach failed: {}", var6.getMessage());
            }
         });
         return this.run.toMap();
      }
   }

   boolean recordIntervention(String var1, Path var2, String var3, String var4) {
      var4 = this.sanitized(var1, var4);
      if (var4 == null) {
         return false;
      } else {
         boolean var5 = true;

         try {
            Files.writeString(var2, var4 + "\n", StandardOpenOption.CREATE, StandardOpenOption.APPEND);
         } catch (Exception var7) {
            logger.warn("could not persist intervention event for run {}: {}", var1, var7.toString());
            var5 = false;
         }

         if (this.publisher != null) {
            this.publisher.publish(var1, var3, var4);
         }

         return var5;
      }
   }

   private void detachIntervention() {
      BotDriver var1 = this.interventionBot;
      if (var1 != null) {
         try {
            var1.stopRecording();
         } catch (Exception var3) {
            logger.debug("intervention stopRecording failed: {}", var3.getMessage());
         }

         this.interventionBot = null;
      }

      LiveRunSink var2 = this.interventionSink;
      if (var2 != null) {
         var2.close();
         this.interventionSink = null;
      }
   }

   Map<String, Object> giveBack(String var1) {
      if (!this.run.isRunning()) {
         return JsApi.missing("no loop run is in flight to give back to — Harness.status()");
      } else {
         this.detachIntervention();
         String var2 = "The human drove the browser manually. Read the run transcript (runs/"
            + this.run.id()
            + "/transcript.jsonl — the human_action events) to see what they did, then continue / re-attempt the flow."
            + (var1 != null && !var1.isBlank() ? " Note: " + var1 : "");
         this.run.resume(var2);
         this.publishHandoff("agent", var1);
         return this.run.toMap();
      }
   }

   private void publishHandoff(String var1, String var2) {
      if (this.publisher != null) {
         String var3 = this.run.id();
         if (var3 != null) {
            this.publisher.publish(var3, "control_handoff", io.karatelabs.ext.agent.events.AgentEvent.ControlHandoffEvent.a(var1, var2).e());
         }
      }
   }

   Map<String, Object> delete(String var1) {
      if (var1 == null || var1.isBlank()) {
         return JsApi.missing("Harness.delete(id) requires a run id — see Harness.runs()");
      } else if (this.inFlight(var1)) {
         return JsApi.missing("run '" + var1 + "' is in flight — Harness.stop() it first");
      } else if (this.store.b(var1) == null) {
         return JsApi.notFound("no run '" + var1 + "' — see Harness.runs()");
      } else {
         LinkedHashMap var2 = new LinkedHashMap();
         var2.put("deleted", this.store.e(var1));
         var2.put("runId", var1);
         return var2;
      }
   }

   Map<String, Object> clear() {
      int var1 = 0;

      for (Map var3 : this.store.b()) {
         String var4 = String.valueOf(var3.get("runId"));
         if (!this.inFlight(var4) && this.store.e(var4)) {
            var1++;
         }
      }

      LinkedHashMap var5 = new LinkedHashMap();
      var5.put("deleted", var1);
      return var5;
   }

   private boolean inFlight(String var1) {
      io.karatelabs.ext.max.jsapi.Harness.ExecutionEvidenceContext var2 = this.bracket;
      return this.run.isRunning() && var1.equals(this.run.id()) || var1.equals(this.liveRunId) || var2 != null && var1.equals(var2.d);
   }

   private boolean inFlightOrActive(String var1) {
      return this.inFlight(var1) || this.activeRuns != null && this.activeRuns.isActive(var1);
   }

   private void reconcile(Map<String, Object> var1, String var2) {
      String var3 = String.valueOf(var1.get("runId"));
      boolean var4 = var2 != null && var2.equals(var3) || this.inFlightOrActive(var3);
      var1.put("live", var4);
      if (!var4 && "running".equals(var1.get("status"))) {
         var1.put("status", "interrupted");
      }

      String var5 = recordKind(var1);
      if (var5 != null) {
         var1.put("kind", var5);
      }

      var1.put("ai", aiOf(var1, var5));
      if (!var4 && var1.get("startedMs") instanceof Number var6 && var1.get("finishedMs") instanceof Number var7) {
         var1.put("durationMs", Math.max(0L, var7.longValue() - var6.longValue()));
      }

      var1.put("hasVideo", this.store.d(var3));
      var1.put("hasTranscript", Files.isRegularFile(this.store.a(var3).resolve("transcript.jsonl")));
      String var9 = nativeReportUrl(this.store.a(var3), var3);
      var1.put("hasReport", var9 != null);
      if (var9 != null) {
         var1.put("reportUrl", var9);
      }
   }

   private static String nativeReportUrl(Path var0, String var1) {
      if (!Files.isRegularFile(var0.resolve("karate-summary.html"))) {
         return null;
      } else {
         String var2 = "/api/artifacts/runs/" + urlSegment(var1) + "/";
         Path var3 = var0.resolve("feature-html");

         try (var var4 = Files.list(var3)) {
            var var5 = var4.filter(var0x -> var0x.toString().endsWith(".html")).toList();
            return var5.size() == 1 ? var2 + "feature-html/" + ((Path)var5.getFirst()).getFileName() : var2 + "karate-summary.html";
         } catch (Exception var9) {
            return var2 + "karate-summary.html";
         }
      }
   }

   private static String urlSegment(String var0) {
      return URLEncoder.encode(var0, StandardCharsets.UTF_8).replace("+", "%20");
   }

   Map<String, Object> status() {
      if (this.run.isRunning()) {
         return this.slotView();
      } else {
         String var1 = this.liveRunId;
         return var1 != null ? this.get(var1) : this.slotView();
      }
   }

   private Map<String, Object> slotView() {
      var var1 = this.run.toMap();
      String var2 = this.run.id();
      Map var3 = var2 != null && this.store.c(var2) ? this.store.b(var2) : null;
      if (var3 != null) {
         String var4 = recordKind(var3);
         if (var4 != null) {
            var1.put("kind", var4);
         }

         var1.put("ai", aiOf(var3, var4));
      }

      return var1;
   }

   public String runBusyReason() {
      if (this.run.isRunning()) {
         return this.loopActiveReason();
      } else {
         String var1 = this.leasedRunId;
         if (var1 != null) {
            return leasedReason(var1);
         } else if (this.liveRunId != null) {
            if (!this.hostsLive(this.bracket) && this.liveBotAlive != null && Boolean.FALSE.equals(this.liveBotAlive.get())) {
               logger.warn("live run {} has no live browser — reconciling the stale slot", this.liveRunId);
               this.settleLive("completed");
               return null;
            } else {
               return "a live browser run is already active ("
                  + this.liveRunId
                  + ") — this container is single-user (one run at a time); end it (bot.destroy() / Harness.stop()) or run parallel work in a second container";
            }
         } else {
            return null;
         }
      }
   }

   public String createBusyReason() {
      if (this.ownedLease() != null) {
         return null;
      } else {
         return this.hostsLive(this.bracket) && !Boolean.TRUE.equals(this.liveBotAlive()) ? null : this.runBusyReason();
      }
   }

   public String leaseSlot(String var1) {
      this.lock.lock();

      String var4;
      try {
         String var2 = this.leasedRunId;
         if (var2 != null) {
            return leasedReason(var2);
         }

         String var3 = this.runBusyReason();
         if (var3 == null) {
            this.leasedRunId = var1;
            logger.debug("set run {} leased the browser slot", var1);
            return null;
         }

         var4 = var3;
      } finally {
         this.lock.unlock();
      }

      return var4;
   }

   private String loopActiveReason() {
      return "an autonomous loop run is active (" + this.run.id() + ") — stop it with Harness.stop() before starting another run";
   }

   private static String leasedReason(String var0) {
      return "a check set is running ("
         + var0
         + ") and holds the browser for its whole run — this container is single-user (one run at a time); wait for it, or stop it with Runner.stop('"
         + var0
         + "')";
   }

   public void releaseSlot(String var1) {
      this.lock.lock();

      try {
         if (var1 != null && var1.equals(this.leasedRunId)) {
            this.leasedRunId = null;
         }
      } finally {
         this.lock.unlock();
      }
   }

   public void ownSlot(String var1) {
      this.leaseOwner.set(var1);
   }

   public void disownSlot() {
      this.leaseOwner.remove();
   }

   public String ownedLease() {
      String var1 = this.leasedRunId;
      return var1 != null && var1.equals(this.leaseOwner.get()) ? var1 : null;
   }

   public String leasedRunId() {
      return this.leasedRunId;
   }

   public Harness onLeasedMemberEnd(Runnable var1) {
      this.onLeasedMemberEnd = var1;
      return this;
   }

   public void leasedMemberEnd() {
      Runnable var1 = this.onLeasedMemberEnd;
      if (var1 != null && this.ownedLease() != null) {
         var1.run();
      }
   }

   public Harness onLiveBotAlive(Supplier<Boolean> var1) {
      this.liveBotAlive = var1;
      return this;
   }

   public Boolean liveBotAlive() {
      return this.liveBotAlive == null ? null : this.liveBotAlive.get();
   }

   public String startLive() {
      this.lock.lock();

      String var2;
      try {
         io.karatelabs.ext.max.jsapi.Harness.ExecutionEvidenceContext var1 = this.bracket;
         if (!this.hostsLive(var1)) {
            if (this.run.isRunning() && !this.recording) {
               throw new IllegalStateException(this.loopActiveReason());
            }

            var2 = this.leasedRunId;
            if (var2 != null) {
               throw new IllegalStateException(leasedReason(var2));
            }

            this.store.a();
            this.settleLive("completed");
            String var3;
            if (var1 != null && var1.d != null && !var1.e) {
               var3 = var1.d;
               var1.e = true;
               this.store.a(var3, var0 -> {
                  var0.put("mode", "live");
                  var0.put("status", "running");
               });

               for (String[] var5 : var1.f) {
                  this.transcript(var3, var5[0], var5[1]);
               }

               var1.f.clear();
            } else {
               var3 = UUID.randomUUID().toString().substring(0, 8);
               this.store.a(var3, io.karatelabs.ext.max.jsapi.EvidenceProtocolMetadata.a(baseMeta(var3, "live", "live run", System.currentTimeMillis())));
               this.executionSeq = 0;
            }

            this.liveRunId = var3;
            if (this.liveCaptureStart != null) {
               this.liveCaptureStart.run();
            }

            logger.debug("live run started: {}", var3);
            return var3;
         }

         var2 = var1.d;
      } finally {
         this.lock.unlock();
      }

      return var2;
   }

   private boolean hostsLive(io.karatelabs.ext.max.jsapi.Harness.ExecutionEvidenceContext var1) {
      return var1 != null && var1.e && var1.d != null && var1.d.equals(this.liveRunId);
   }

   public String beginExecution(String var1) {
      this.lock.lock();

      Object var2;
      try {
         if (this.bracket == null) {
            long var13 = System.currentTimeMillis();
            String var4 = this.liveRunId;
            boolean var5 = var4 != null;
            if (var4 == null && !this.draftQuarantined()) {
               var4 = UUID.randomUUID().toString().substring(0, 8);
               this.store.a(var4, io.karatelabs.ext.max.jsapi.EvidenceProtocolMetadata.a(baseMeta(var4, "check", "Check: " + var1, var13)));
               this.executionSeq = 0;
            }

            String var6 = var4 == null ? "draft#" + var13 : io.karatelabs.ext.coverage.collect.EvidenceRunMetadata.a(var4, ++this.executionSeq);
            io.karatelabs.ext.max.jsapi.Harness.ExecutionEvidenceContext var7 = new io.karatelabs.ext.max.jsapi.Harness.ExecutionEvidenceContext(var6, var1, var13, var4, var5);
            this.bracket = var7;
            if (var5 && this.liveCaptureDrain != null) {
               this.sessionExchanges.addAll(this.liveCaptureDrain.get());
               if (this.liveCaptureStart != null) {
                  this.liveCaptureStart.run();
               }
            }

            LinkedHashMap var8 = new LinkedHashMap();
            var8.put("executionId", var6);
            var8.put("definitionId", var1);
            var8.put("startedAt", var13);
            this.record(var7, "execution-start", var8, var13);
            return var6;
         }

         var2 = null;
      } finally {
         this.lock.unlock();
      }

      return (String)var2;
   }

   public String executionOwner() {
      io.karatelabs.ext.max.jsapi.Harness.ExecutionEvidenceContext var1 = this.bracket;
      return var1 == null ? null : var1.a;
   }

   public boolean executionOpen() {
      return this.bracket != null;
   }

   public io.karatelabs.ext.common.EvidenceContext.EvidenceEventSink ownerSink() {
      return this::recordEvent;
   }

   public boolean recordEvent(String var1, Map<String, Object> var2) {
      this.lock.lock();

      boolean var7;
      try {
         long var3 = System.currentTimeMillis();
         var2 = frozen(var2);
         io.karatelabs.ext.max.jsapi.Harness.ExecutionEvidenceContext var5 = this.bracket;
         if (var5 != null) {
            this.record(var5, var1, var2, var3);
            return true;
         }

         String var6 = this.liveRunId;
         if (var6 != null) {
            this.transcript(var6, var1, envelope(var1, var2, var3));
            return true;
         }

         var7 = false;
      } finally {
         this.lock.unlock();
      }

      return var7;
   }

   private void record(io.karatelabs.ext.max.jsapi.Harness.ExecutionEvidenceContext var1, String var2, Map<String, Object> var3, long var4) {
      switch (var2) {
         case "assertion":
            var1.h.add(var3);
            break;
         case "emit":
            var1.i.add(var3);
            if (var3.get("payload") instanceof Map var14 && "req".equals(var14.get("source")) && var14.get("key") != null) {
               var1.j.add(String.valueOf(var14.get("key")));
            }
            break;
         case "step":
         case "screenshot":
            String var8 = String.valueOf(var3.getOrDefault("name", var3.get("title")));
            Matcher var9 = REQ_TOKEN.matcher(var8);

            while (var9.find()) {
               for (String var13 : var9.group(1).split(",")) {
                  if (!var13.isBlank()) {
                     var1.j.add(var13.trim());
                  }
               }
            }
      }

      String var6 = envelope(var2, var3, var4);
      if (var1.d != null) {
         if (var1.e) {
            this.transcript(var1.d, var2, var6);
         } else {
            var1.f.add(new String[]{var2, var6});
         }
      }
   }

   private static Map<String, Object> frozen(Map<String, Object> var0) {
      try {
         return Json.of(Json.stringifyStrict(var0)).value();
      } catch (RuntimeException var2) {
         return new LinkedHashMap<>(var0);
      }
   }

   private static String envelope(String var0, Map<String, Object> var1, long var2) {
      LinkedHashMap var4 = new LinkedHashMap();
      var4.put("type", var0);
      var4.put("ts", var2);
      var1.forEach((var1x, var2x) -> {
         if (!"type".equals(var1x) && !"ts".equals(var1x)) {
            var4.put(var1x, var2x);
         }
      });
      return Json.stringifyStrict(var4);
   }

   public boolean transcript(String var1, String var2, String var3) {
      var3 = this.sanitized(var1, var3);
      if (var3 == null) {
         return false;
      } else {
         boolean var4 = true;

         try {
            Path var5 = this.store.a(var1).resolve("transcript.jsonl");
            Files.createDirectories(var5.getParent());
            if (!Files.exists(var5)) {
               Files.writeString(var5, "{\"type\":\"capture\",\"assertions\":true}\n", StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            }

            Files.writeString(var5, var3 + "\n", StandardOpenOption.CREATE, StandardOpenOption.APPEND);
         } catch (Exception var6) {
            logger.warn("could not persist transcript event for run {}: {}", var1, var6.toString());
            var4 = false;
         }

         if (this.publisher != null) {
            this.publisher.publish(var1, var2, var3);
         }

         return var4;
      }
   }

   private String sanitized(String var1, String var2) {
      if (var2 == null) {
         return null;
      } else {
         try {
            return this.sanitizer.apply(var2);
         } catch (RuntimeException var4) {
            logger.warn("event for run {} dropped, it could not be sanitized: {}", var1, var4.toString());
            return null;
         }
      }
   }

   public Harness onCloseBrowser(Runnable var1) {
      this.closeBrowser = var1;
      return this;
   }

   public Harness liveEvidenceCapture(Runnable var1, Supplier<List<Map<String, Object>>> var2) {
      this.liveCaptureStart = var1;
      this.liveCaptureDrain = var2;
      return this;
   }

   public String liveRunId() {
      return this.liveRunId;
   }

   public Map<String, Object> startRecord(String var1) {
      this.lock.lock();

      try {
         String var2 = this.liveRunId;
         if (var2 == null) {
            return JsApi.missing("no live browser run to record — create one first (var bot = Robot.create())");
         } else if (this.run.isRunning() && !this.recording) {
            return JsApi.missing("an autonomous loop run is active (" + this.run.id() + ") — stop it (Harness.stop()) before recording");
         } else {
            if (!this.recording) {
               Path var3 = this.store.a(var2).resolve("transcript.jsonl");
               if (!this.run.tryStart(var2, var3, "record", var1 != null && !var1.isBlank() ? var1 : "record")) {
                  return JsApi.missing("a harness run is already active (" + this.run.id() + ")");
               }

               this.recording = true;
               if (var1 != null && !var1.isBlank()) {
                  this.store.a(var2, var1x -> var1x.put("name", var1.strip()));
               }

               logger.debug("record session claimed the slot on live run {}", var2);
            }

            LinkedHashMap var8 = new LinkedHashMap();
            var8.put("runId", var2);
            var8.put("recording", true);
            if (var1 != null && !var1.isBlank()) {
               var8.put("name", var1);
            }

            return var8;
         }
      } finally {
         this.lock.unlock();
      }
   }

   public Map<String, Object> stopRecord() {
      this.lock.lock();

      LinkedHashMap var3;
      try {
         boolean var1 = this.recording;
         if (this.recording) {
            this.run.settle("completed");
            this.recording = false;
            logger.debug("record session released the slot");
         }

         LinkedHashMap var2 = new LinkedHashMap();
         var2.put("stopped", var1);
         if (this.liveRunId != null) {
            var2.put("runId", this.liveRunId);
         }

         var3 = var2;
      } finally {
         this.lock.unlock();
      }

      return var3;
   }

   public boolean isRecording() {
      return this.recording;
   }

   public void stampLiveGoal(String var1) {
      String var2 = this.liveRunId;
      if (var2 != null && var1 != null && !var1.isBlank()) {
         var var3 = this.store.b(var2);
         if (var3 != null) {
            if ("live run".equals(var3.get("task"))) {
               var3.put("task", var1.trim());
               this.store.a(var2, var3);
            }
         }
      }
   }

   public void stampLiveCheck(String var1) {
      io.karatelabs.ext.max.jsapi.Harness.ExecutionEvidenceContext var2 = this.bracket;
      String var3 = var2 != null && var2.d != null ? var2.d : this.liveRunId;
      if (var3 != null && var1 != null && !var1.isBlank()) {
         this.store.a(var3, var1x -> {
            var1x.put("check", var1);
            if ("live run".equals(var1x.get("task")) || String.valueOf(var1x.get("task")).startsWith("Check: check:")) {
               var1x.put("task", "Check: " + var1);
            }
         });
      }
   }

   public void recordCheckOutcome(String var1, boolean var2, long var3, List<Map<String, Object>> var5, List<String> var6) {
      this.recordCheckOutcome(var1, var2, var3, var5, var6, null, null);
   }

   public Map<String, Object> recordCheckOutcome(
      String var1, boolean var2, long var3, List<Map<String, Object>> var5, List<String> var6, List<Map<String, Object>> var7, String var8
   ) {
      io.karatelabs.ext.max.jsapi.Harness.ExecutionEvidenceContext var9 = this.bracket;
      if (var9 != null) {
         return this.endExecution(var9, var1, var2, var3, var5, var6, var8);
      } else {
         boolean var10 = this.draftQuarantined();
         if (this.liveRunId != null) {
            this.stampLiveCheck(var1);
            this.lastCheckRunId = this.liveRunId;
            if (var10) {
               logger.debug("draft mock live — {} exchange(s) from check '{}' not queued as evidence", var5 == null ? 0 : var5.size(), var1);
            } else {
               this.queueLiveEvidence(var1, var2, var3, var5, var6, var7);
            }

            if (!this.run.isRunning()) {
               this.settleLive(var2 ? "completed" : "failed", false);
            } else if (this.liveCaptureStart != null) {
               this.liveCaptureStart.run();
            }

            return null;
         } else {
            String var11 = UUID.randomUUID().toString().substring(0, 8);
            var var12 = baseMeta(var11, "check", "Check: " + var1, var3);
            var12.put("check", var1);
            var12.put("status", var2 ? "completed" : "failed");
            var12.put("finishedMs", System.currentTimeMillis());
            if (var10) {
               this.writeQuarantined(var11, var12, var1, var2, var3, var5, var6, var7, List.of(), -1);
               return null;
            } else {
               this.store.a(var11, var12);
               this.writeCheckEvents(this.store.a(var11), var1, var2, var3, var5, var6, var7, List.of(), -1);
               this.lastCheckRunId = var11;
               this.buildCheckGraph(var11);
               return null;
            }
         }
      }
   }

   private void writeQuarantined(
      String var1,
      Map<String, Object> var2,
      String var3,
      boolean var4,
      long var5,
      List<Map<String, Object>> var7,
      List<String> var8,
      List<Map<String, Object>> var9,
      List<Map<String, Object>> var10,
      int var11
   ) {
      Path var12 = this.workingDir.resolve("target").resolve("draft").resolve(var1);
      var2.put("quarantined", true);

      try {
         Files.createDirectories(var12);
         Files.writeString(var12.resolve("meta.json"), Json.stringifyStrict(var2));
      } catch (Exception var14) {
         logger.warn("could not write quarantined check meta {}: {}", var1, var14.getMessage());
      }

      this.writeCheckEvents(var12, var3, var4, var5, var7, var8, var9, var10, var11);
      this.lastCheckRunId = var1;
   }

   private Map<String, Object> endExecution(
      io.karatelabs.ext.max.jsapi.Harness.ExecutionEvidenceContext var1, String var2, boolean var3, long var4, List<Map<String, Object>> var6, List<String> var7, String var8
   ) {
      this.lock.lock();

      Map var9;
      try {
         try {
            var9 = this.closeExecution(var1, var2, var3, var4, var6, var7, var8);
         } finally {
            this.bracket = null;
         }
      } finally {
         this.lock.unlock();
      }

      return var9;
   }

   private Map<String, Object> closeExecution(
      io.karatelabs.ext.max.jsapi.Harness.ExecutionEvidenceContext var1, String var2, boolean var3, long var4, List<Map<String, Object>> var6, List<String> var7, String var8
   ) {
      long var9 = System.currentTimeMillis();
      String var11 = var3 ? "passed" : "failed";
      LinkedHashMap var12 = new LinkedHashMap();
      var12.put("executionId", var1.a);
      var12.put("outcome", var11);
      var12.put("endedAt", var9);
      if (var8 != null) {
         var12.put("error", var8);
      }

      this.record(var1, "execution-end", var12, var9);
      var var13 = tally(var1, var3, var6, var7);
      this.bracket = null;
      List var14 = var6 == null ? List.of() : var6;
      int var15 = ordinalOf(var1.a);
      if (var1.d == null) {
         String var18 = UUID.randomUUID().toString().substring(0, 8);
         var var19 = baseMeta(var18, "check", "Check: " + var2, var4);
         var19.put("check", var2);
         var19.put("status", var3 ? "completed" : "failed");
         var19.put("finishedMs", var9);
         this.writeQuarantined(var18, var19, var2, var3, var4, var14, var7, var1.h, var1.i, var15);
         return var13;
      } else {
         this.lastCheckRunId = var1.d;
         if (var1.e) {
            if (this.draftQuarantined()) {
               logger.debug("draft mock live — {} exchange(s) from check '{}' not queued as evidence", var14.size(), var2);
            } else {
               this.queueLiveEvidence(var2, var3, var4, var14, var7, var1.h, var1.i, var15);
            }

            if (!this.run.isRunning()) {
               this.settleLive(var1.g != null ? var1.g : "completed", false);
            } else if (this.liveCaptureStart != null) {
               this.liveCaptureStart.run();
            }

            return var13;
         } else {
            for (String[] var17 : var1.f) {
               this.transcript(var1.d, var17[0], var17[1]);
            }

            var1.f.clear();
            this.writeCheckEvents(this.store.a(var1.d), var2, var3, var4, var14, var7, var1.h, var1.i, var15);
            this.store.a(var1.d, var4x -> {
               var4x.put("check", var2);
               var4x.put("status", var3 ? "completed" : "failed");
               var4x.put("finishedMs", var9);
               var4x.put("evidenceFinalized", true);
            });
            this.buildCheckGraph(var1.d);
            return var13;
         }
      }
   }

   private static Map<String, Object> tally(io.karatelabs.ext.max.jsapi.Harness.ExecutionEvidenceContext var0, boolean var1, List<Map<String, Object>> var2, List<String> var3) {
      ArrayList var4 = new ArrayList();

      for (Map var6 : var0.i) {
         if (var6.get("payload") instanceof Map var7 && var7.get("evidence") instanceof Map var8) {
            var4.add(var8);
         }
      }

      return tally(var0.h, var0.j, var1, (var2 == null ? 0 : var2.size()) + var0.i.size(), var3, io.karatelabs.ext.common.EvidenceContext.b() - var0.k, verdicts(var4));
   }

   public static int[] verdicts(List<Map<?, ?>> var0) {
      LinkedHashSet var1 = new LinkedHashSet();
      LinkedHashSet var2 = new LinkedHashSet();
      int var3 = 0;
      int var4 = 0;

      for (Map var6 : var0) {
         if (Boolean.TRUE.equals(var6.get("verified"))) {
            boolean var7 = "fail".equals(var6.get("status"));
            Object var8 = var6.get("verdict");
            if (var8 == null) {
               var3++;
               var4 += var7 ? 1 : 0;
            } else {
               var1.add(var8);
               if (var7) {
                  var2.add(var8);
               }
            }
         }
      }

      return new int[]{var1.size() + var3, var2.size() + var4};
   }

   public static Map<String, Object> tally(List<Map<String, Object>> var0, Set<String> var1, boolean var2, int var3, List<String> var4, long var5, int[] var7) {
      int var8 = var7[1];

      for (Map var10 : var0) {
         if (!Boolean.TRUE.equals(var10.get("pass"))) {
            var8++;
         }
      }

      var var17 = new LinkedHashSet<>(var1);

      for (String var11 : (Iterable<String>) (Iterable<?>) (var4 == null ? List.of() : var4)) {
         String var12 = var11.startsWith("@") ? var11.substring(1) : var11;
         if (var12.startsWith("req=")) {
            for (String var16 : var12.substring(4).split(",")) {
               if (!var16.isBlank()) {
                  var17.add(var16.trim());
               }
            }
         }
      }

      LinkedHashMap var19 = new LinkedHashMap();
      var19.put("assertions", var0.size());
      var19.put("verdicts", var7[0]);
      var19.put("verdictsFailed", var7[1]);
      var19.put("failed", var8);
      double var20 = io.karatelabs.ext.coverage.AssertionStrengthSummary.a(var0, var2).b();
      if (var20 >= 0.0) {
         var19.put("assertedProportion", var20);
      }

      var19.put("criteria", new ArrayList(var17));
      LinkedHashMap var21 = new LinkedHashMap();
      var21.put("observed", var3);
      var21.put("declared", var4 == null ? 0 : var4.size());
      var19.put("hits", var21);
      var19.put("dropped", var5);
      return var19;
   }

   private static int ordinalOf(String var0) {
      int var1 = var0.lastIndexOf(35);

      try {
         return Integer.parseInt(var0.substring(var1 + 1));
      } catch (RuntimeException var3) {
         return -1;
      }
   }

   public Harness onCheckGraph(Consumer<String> var1) {
      this.checkGraphBuilder = var1;
      return this;
   }

   public Harness withDraftGuard(Supplier<Boolean> var1) {
      this.draftGuard = var1;
      return this;
   }

   private boolean draftQuarantined() {
      return this.draftGuard != null && Boolean.TRUE.equals(this.draftGuard.get());
   }

   private void buildCheckGraph(String var1) {
      if (this.checkGraphBuilder != null) {
         try {
            this.checkGraphBuilder.accept(var1);
         } catch (Exception var3) {
            logger.warn("per-run coverage graph not built for check run {}: {}", var1, var3.getMessage());
         }
      }
   }

   public String lastCheckRunId() {
      return this.lastCheckRunId;
   }

   private void writeCheckEvents(
      Path var1,
      String var2,
      boolean var3,
      long var4,
      List<Map<String, Object>> var6,
      List<String> var7,
      List<Map<String, Object>> var8,
      List<Map<String, Object>> var9,
      int var10
   ) {
      boolean var11 = var10 >= 0;
      if (var11 || var6 != null && !var6.isEmpty()) {
         try {
            ArrayList var12 = new ArrayList();

            for (Map var14 : (Iterable<Map>) (Iterable<?>) (var6 == null ? List.of() : var6)) {
               var12.add(jsonEmbed("http-exchange", var14));
            }

            if (var8 != null) {
               for (Map var23 : var8) {
                  var12.add(jsonEmbed("evidence", var23));
               }
            }

            for (Map var24 : (Iterable<Map>) (Iterable<?>) (var9 == null ? List.of() : var9)) {
               if (var24.get("payload") instanceof Map var15) {
                  var12.add(jsonEmbed(String.valueOf(var24.get("name")), var15));
               }
            }

            LinkedHashMap var22 = new LinkedHashMap();
            var22.put("embeds", var12);
            LinkedHashMap var25 = new LinkedHashMap();
            var25.put("name", var2);
            var25.put("line", 1);
            ArrayList var26 = new ArrayList(var7 == null ? List.of() : var7);
            if (var11) {
               var25.put("slug", Check.definitionId(var2));
               var26.add("fragment=" + io.karatelabs.ext.coverage.collect.ExecutionFragment.ExecutionFragmentKind.CHECK_BLOCK.tag());
               var26.add("exec=" + var10);
            } else {
               var25.put("slug", var2);
            }

            var25.put("tags", var26);
            var25.put("failed", !var3);
            var25.put("stepResults", List.of(var22));
            LinkedHashMap var27 = new LinkedHashMap();
            var27.put("relativePath", var2);
            var27.put("scenarioResults", List.of(var25));
            Path var17 = MaxPaths.eventsFile(var1);
            Files.createDirectories(var17.getParent());
            String var18 = eventLine("SUITE_ENTER", var4, Map.of("exts", List.of(Map.of("name", "check", "assertions", var8 != null))))
               + eventLine("FEATURE_ENTER", var4, Map.of("path", var2, "relativePath", var2))
               + eventLine("FEATURE_EXIT", System.currentTimeMillis(), var27);
            Files.writeString(var17, var18, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
         } catch (Exception var19) {
            logger.warn("could not write check event stream for {}: {}", var2, var19.getMessage());
         }
      }
   }

   private void queueLiveEvidence(String var1, boolean var2, long var3, List<Map<String, Object>> var5, List<String> var6, List<Map<String, Object>> var7) {
      this.lock.lock();

      try {
         this.queueLiveEvidence(var1, var2, var3, var5, var6, var7, List.of(), -1);
      } finally {
         this.lock.unlock();
      }
   }

   private void queueLiveEvidence(
      String var1,
      boolean var2,
      long var3,
      List<Map<String, Object>> var5,
      List<String> var6,
      List<Map<String, Object>> var7,
      List<Map<String, Object>> var8,
      int var9
   ) {
      this.lock.lock();

      try {
         if (var9 >= 0 || var5 != null && !var5.isEmpty()) {
            LinkedHashMap var10 = new LinkedHashMap();
            var10.put("name", var1);
            var10.put("ok", var2);
            var10.put("startedMs", var3);
            var10.put("exchanges", var5);
            var10.put("tags", var6 == null ? List.of() : var6);
            var10.put("assertions", var7);
            var10.put("emits", var8);
            var10.put("ordinal", var9);
            this.pendingLiveEvidence.add(var10);
            return;
         }
      } finally {
         this.lock.unlock();
      }
   }

   public void queueRecordingEvidence(String var1, String var2, List<Map<String, Object>> var3) {
      if (var1 != null && var3 != null && !var3.isEmpty()) {
         this.lock.lock();

         try {
            LinkedHashMap var4 = new LinkedHashMap();
            var4.put("recording", var1);
            var4.put("name", var2 != null && !var2.isBlank() ? var2 : var1);
            var4.put("startedMs", System.currentTimeMillis());
            var4.put("exchanges", var3);
            this.pendingLiveEvidence.add(var4);
         } finally {
            this.lock.unlock();
         }
      }
   }

   private void writeRecordingEvents(Path var1, String var2, String var3, long var4, List<Map<String, Object>> var6) {
      try {
         ArrayList var7 = new ArrayList();

         for (Map var9 : var6) {
            String var10 = this.sanitized(var2, Json.stringifyStrict(var9));
            if (var10 == null) {
               return;
            }

            var7.add(jsonEmbed("http-exchange", Json.of(var10).asMap()));
         }

         String var16 = "recording:" + var2;
         LinkedHashMap var17 = new LinkedHashMap();
         var17.put("name", var3);
         var17.put("line", 1);
         var17.put("slug", var16);
         var17.put("tags", List.of());
         var17.put("failed", false);
         var17.put("stepResults", List.of(Map.of("embeds", var7)));
         LinkedHashMap var18 = new LinkedHashMap();
         var18.put("relativePath", var16);
         var18.put("prefixedPath", var16);
         var18.put("scenarioResults", List.of(var17));
         StringBuilder var11 = new StringBuilder();

         for (String var13 : List.of(
            eventLine("SUITE_ENTER", var4, Map.of("exts", List.of(Map.of("name", "recording", "assertions", true)))),
            eventLine("FEATURE_ENTER", var4, Map.of("path", var16, "relativePath", var16)),
            eventLine("FEATURE_EXIT", System.currentTimeMillis(), var18)
         )) {
            String var14 = this.sanitized(var2, var13);
            if (var14 == null) {
               return;
            }

            var11.append(var14);
         }

         Path var19 = MaxPaths.eventsFile(var1);
         Files.createDirectories(var19.getParent());
         Files.writeString(var19, var11, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
      } catch (Exception var15) {
         logger.warn("could not write the recording's event stream for {}: {}", var2, var15.getMessage());
      }
   }

   private void drainLiveEvidenceInto(Path var1) {
      this.lock.lock();

      try {
         for (Map var3 : this.pendingLiveEvidence) {
            if (var3.get("recording") instanceof String var4) {
               this.writeRecordingEvents(
                  var1, var4, (String)var3.get("name"), ((Number)var3.get("startedMs")).longValue(), (List<Map<String, Object>>)var3.get("exchanges")
               );
            } else {
               this.writeCheckEvents(
                  var1,
                  (String)var3.get("name"),
                  Boolean.TRUE.equals(var3.get("ok")),
                  ((Number)var3.get("startedMs")).longValue(),
                  (List<Map<String, Object>>)var3.get("exchanges"),
                  (List<String>)var3.get("tags"),
                  (List<Map<String, Object>>)var3.get("assertions"),
                  (List<Map<String, Object>>)var3.get("emits"),
                  ((Number)var3.get("ordinal")).intValue()
               );
            }
         }

         this.pendingLiveEvidence.clear();
      } finally {
         this.lock.unlock();
      }
   }

   private static String eventLine(String var0, long var1, Map<String, Object> var3) {
      LinkedHashMap var4 = new LinkedHashMap();
      var4.put("type", var0);
      var4.put("timeStamp", var1);
      var4.put("data", var3);
      return Json.stringifyStrict(var4) + "\n";
   }

   private static Map<String, Object> jsonEmbed(String var0, Map<String, Object> var1) {
      String var2 = Base64.getEncoder().encodeToString(Json.stringifyStrict(var1).getBytes(StandardCharsets.UTF_8));
      LinkedHashMap var3 = new LinkedHashMap();
      var3.put("role", "primary");
      var3.put("mime", "application/json");
      var3.put("data", var2);
      LinkedHashMap var4 = new LinkedHashMap();
      var4.put("name", var0);
      var4.put("parts", List.of(var3));
      return var4;
   }

   public Harness onSettleLive(Runnable var1) {
      this.onSettleLive = var1;
      return this;
   }

   public Harness onRecordingEnd(Runnable var1) {
      this.onRecordingEnd = var1;
      return this;
   }

   public ReentrantLock lock() {
      return this.lock;
   }

   Map<String, Object> recordLive() {
      String var1 = this.liveRunId;
      if (var1 == null) {
         return JsApi.missing("no live run to record — Robot.create() registers one");
      } else {
         if (this.liveVideo == null || !this.liveVideo.isAlive()) {
            this.liveVideo = io.karatelabs.ext.robot.VideoRecorder.a(this.store.a(var1).resolve("video.mp4"));
         }

         LinkedHashMap var2 = new LinkedHashMap();
         var2.put("recording", this.liveVideo != null);
         var2.put("runId", var1);
         return var2;
      }
   }

   public void settleLive(String var1) {
      this.lock.lock();

      try {
         this.settleLive(var1, true);
      } finally {
         this.lock.unlock();
      }
   }

   public void settleLive(String var1, boolean var2) {
      this.lock.lock();

      try {
         String var3 = this.liveRunId;
         if (var3 == null) {
            return;
         }

         io.karatelabs.ext.max.jsapi.Harness.ExecutionEvidenceContext var4 = this.bracket;
         if (!this.hostsLive(var4)) {
            if (this.recording) {
               this.run.settle("completed");
               this.recording = false;
               if (this.onRecordingEnd != null) {
                  try {
                     this.onRecordingEnd.run();
                  } catch (Exception var20) {
                     logger.warn("onRecordingEnd hook failed for {}: {}", var3, var20.getMessage());
                  }
               }
            }

            io.karatelabs.ext.robot.VideoRecorder.a(this.liveVideo);
            this.liveVideo = null;

            try {
               Path var5 = this.store.a(var3).resolve("video.mp4");
               if (Files.isRegularFile(var5) && Files.size(var5) == 0L) {
                  Files.delete(var5);
               }
            } catch (Exception var19) {
            }

            var var22 = this.store.b(var3);
            if (var22 != null) {
               var22.put("status", var1);
               var22.put("finishedMs", System.currentTimeMillis());
               if (var22.get("check") == null) {
                  var22.put("recordable", !Recording.digest(this.store.a(var3).resolve("transcript.jsonl")).a());
               }

               this.store.a(var3, var22);

               try {
                  if (Files.isRegularFile(this.store.a(var3).resolve("transcript.jsonl"))) {
                     io.karatelabs.ext.max.jsapi.AgentTranscriptReplayer.a(this.workingDir, this.store.a(var3), var22);
                  }
               } catch (Exception var18) {
                  logger.warn("live report not synthesized for {}: {}", var3, var18.getMessage());
               }
            }

            var var6 = new ArrayList<>(this.sessionExchanges);
            this.sessionExchanges.clear();
            if (this.liveCaptureDrain != null) {
               var6.addAll(this.liveCaptureDrain.get());
            }

            if (!var6.isEmpty()) {
               long var7 = var22 != null && var22.get("startedMs") instanceof Number var9 ? var9.longValue() : System.currentTimeMillis();
               this.queueLiveEvidence("API exploration", !"failed".equals(var1), var7, var6, List.of(), null);
            }

            this.drainLiveEvidenceInto(this.store.a(var3));
            io.karatelabs.ext.max.jsapi.EvidenceProtocolMetadata.a(this.store, var3);
            this.buildCheckGraph(var3);
            this.liveRunId = null;
            if (var2 && this.onSettleLive != null) {
               try {
                  this.onSettleLive.run();
                  this.clearedVars = true;
               } catch (Exception var17) {
                  logger.warn("onSettleLive hook failed for {}: {}", var3, var17.getMessage());
               }
            }

            if (this.publisher != null) {
               LinkedHashMap var23 = new LinkedHashMap();
               var23.put("type", "complete");
               var23.put("status", var1);
               var23.put("runId", var3);
               var23.put("ts", System.currentTimeMillis());
               this.publisher.publish(var3, "complete", Json.stringifyStrict(var23));
            }

            return;
         }

         var4.g = var1;
         logger.debug("live run {} settle ({}) deferred to execution-end of {}", new Object[]{var3, var1, var4.a});
      } finally {
         this.lock.unlock();
      }
   }

   public Path runDir(String var1) {
      return this.store.a(var1);
   }

   private Map<String, Object> runsNotWritable() {
      try {
         this.store.a();
         return null;
      } catch (RuntimeException var2) {
         return JsApi.missing(var2.getMessage());
      }
   }

   private static Map<String, Object> baseMeta(String var0, String var1, String var2, long var3) {
      LinkedHashMap var5 = new LinkedHashMap();
      var5.put("runId", var0);
      var5.put("task", var2);
      var5.put("mode", var1);
      var5.put("status", "running");
      var5.put("startedMs", var3);
      return var5;
   }

   private static Map<String, Object> loopMeta(String var0, String var1, Map<String, Object> var2, String var3, long var4) {
      var var6 = baseMeta(var0, var1, var3, var4);
      var6.put("kind", kindOf(var2));
      return var6;
   }

   private static Map<String, Object> withDeadline(Map<String, Object> var0, Map<String, Object> var1) {
      if (var1.get("deadlineMs") instanceof Number var2 && var2.longValue() > 0L) {
         var0.put("deadlineMs", var2.longValue());
      }

      return var0;
   }

   private static Map<String, Object> withLoopFacts(Map<String, Object> var0, Map<String, Object> var1) {
      if (var1.get("check") instanceof String var2 && !var2.isBlank()) {
         var0.put("check", var2);
      }

      if (Boolean.TRUE.equals(var1.get("replaces"))) {
         var0.put("replaces", true);
      }

      if (var1.get("booted") instanceof String var4) {
         var0.put("booted", var4);
      }

      if (var1.get("maxIterations") instanceof Number var5) {
         var0.put("maxIterations", var5.intValue());
      }

      if (var1.get("request") instanceof String var6 && !var6.isBlank()) {
         var0.put("request", var6);
      }

      return var0;
   }

   private static Map<String, Object> withReq(Map<String, Object> var0, String var1) {
      if (var1 != null) {
         var0.put("req", var1);
      }

      return var0;
   }

   private Map<String, Object> withLlm(Map<String, Object> var1, String var2, String var3) {
      Object[] var4 = this.select(var3, var2, this.env);
      io.karatelabs.ext.agent.llm.LlmProvider var5 = (io.karatelabs.ext.agent.llm.LlmProvider)var4[0];
      var1.put("provider", var5.name().toLowerCase(Locale.ROOT));
      if (var4[1] != null) {
         var1.put("model", io.karatelabs.ext.agent.llm.ClaudeModel.resolve((String)var4[1], var5));
      }

      return var1;
   }

   private static io.karatelabs.ext.agent.llm.LlmProvider parseProviderLenient(String var0) {
      if (var0 != null && !var0.isBlank()) {
         try {
            return io.karatelabs.ext.agent.llm.LlmProvider.valueOf(var0.trim().toUpperCase(Locale.ROOT));
         } catch (IllegalArgumentException var2) {
            return null;
         }
      } else {
         return null;
      }
   }

   private Map<String, Object> runSyncRecorded(String var1, Map<String, Object> var2) {
      String var3 = var2.get("runId") instanceof String var4 && !var4.isBlank() ? var4.trim() : UUID.randomUUID().toString().substring(0, 8);
      String var22 = this.runBusyReason();
      if (var22 != null) {
         return JsApi.missing(var22);
      } else {
         Path var23 = this.store.a(var3).resolve("transcript.jsonl");
         String var6 = modeOf(var2);
         if (!this.run.tryStart(var3, var23, var6, var1, var2.get("check") instanceof String var7 ? var7 : null)) {
            return JsApi.missing("a harness run is already active (" + this.run.id() + ") — one active run per container; wait for it or restart the container");
         } else {
            long var24 = System.currentTimeMillis();
            var var9 = loopMeta(var3, var6, var2, var1, var24);

            Map var13;
            try {
               Files.createDirectories(var23.getParent());
               String var10 = var2.get("provider") instanceof String var25 && !var25.isBlank() ? var25 : null;
               String var26 = var2.get("model") instanceof String var29 && !var29.isBlank() ? var29 : null;
               var9 = withLoopFacts(withDeadline(this.withLlm(loopMeta(var3, var6, var2, var1, var24), var10, var26), var2), var2);
               this.store.a(var3, var9);
               io.karatelabs.ext.agent.harness.AgentHarness.Builder var30 = this.configure(var2).runId(var3).transcriptPath(var23).onLoopCreated(this.run::attachLoop);
               io.karatelabs.ext.agent.harness.HarnessResult var32 = this.executor.apply(var30, var1);
               var9.put("status", this.run.complete(var32));
               var9.put("finishedMs", System.currentTimeMillis());
               if (var32 != null) {
                  var9.put("iterations", var32.state().iterations());
                  var9.put("succeeded", var32.isSuccess());
                  var9.put("tokens", var32.totalTokens());
                  var9.put(var32.isSuccess() ? "result" : "error", var32.isSuccess() ? var32.result() : var32.errorMessage());
                  putIfPresent(var9, "blocker", var32.blockedResult());
                  putIfPresent(var9, "statement", var32.statement());
                  putIfPresent(var9, "errorShort", var32.g());
                  putIfPresent(var9, "errorHint", var32.h());
               }

               this.store.a(var3, var9);
               return project(var32);
            } catch (Exception var20) {
               this.run.fail(var20.getMessage());
               var9.put("status", "failed");
               var9.put("finishedMs", System.currentTimeMillis());
               var9.put("error", var20.getMessage());
               io.karatelabs.ext.agent.llm.LlmException var11 = io.karatelabs.ext.agent.llm.LlmException.a(var20);
               if (var11 != null && var11.b() > 0) {
                  putIfPresent(var9, "errorShort", var11.f());
                  putIfPresent(var9, "errorHint", var11.g());
               }

               try {
                  this.store.a(var3, var9);
               } catch (Exception var19) {
                  logger.warn("could not persist failed {} meta {}: {}", new Object[]{var6, var3, var19.getMessage()});
               }

               if (var11 == null) {
                  throw var20 instanceof RuntimeException var27 ? var27 : new RuntimeException(var20);
               }

               var var12 = JsApi.missing("Harness." + var6 + " could not start: " + var20.getMessage());
               putIfPresent(var12, "hint", var11.g());
               var13 = var12;
            } finally {
               this.buildCheckGraph(var3);
            }

            return var13;
         }
      }
   }

   private Map<String, Object> runSync(String var1, Map<String, Object> var2) {
      io.karatelabs.ext.agent.harness.AgentHarness.Builder var3 = this.configure(var2);
      Object var4 = var2.get("transcript");
      if (var4 != null && !String.valueOf(var4).isBlank()) {
         var3.transcriptPath(this.resolve(String.valueOf(var4)));
      }

      try {
         return project(this.executor.apply(var3, var1));
      } catch (io.karatelabs.ext.agent.llm.LlmException var7) {
         var var6 = JsApi.missing("Harness." + kindOf(var2) + " could not start: " + var7.getMessage());
         putIfPresent(var6, "hint", var7.g());
         return var6;
      }
   }

   private Map<String, Object> runAsync(String var1, Map<String, Object> var2) {
      boolean var3 = bool(var2, "robot");
      String var4 = var2.get("runId") instanceof String var5 && !var5.isBlank() ? var5 : UUID.randomUUID().toString().substring(0, 8);
      String var11 = this.runBusyReason();
      if (var11 != null) {
         return JsApi.missing(var11);
      } else {
         Path var12 = this.workingDir.resolve("runs").resolve(var4).resolve("transcript.jsonl");
         String var7 = modeOf(var2);
         if (!this.run.tryStart(var4, var12, var7, var1, var2.get("check") instanceof String var8 ? var8 : null)) {
            return JsApi.missing("a harness run is already active (" + this.run.id() + ") — one active run per container; wait for it or restart the container");
         } else {
            try {
               return this.startClaimed(var4, var1, var2, var3, var7, var12);
            } catch (Error | RuntimeException var10) {
               this.run.fail(var10.getMessage());
               throw var10;
            }
         }
      }
   }

   private Map<String, Object> startClaimed(String var1, String var2, Map<String, Object> var3, boolean var4, String var5, Path var6) {
      AtomicBoolean var7 = var4 ? new AtomicBoolean() : null;
      io.karatelabs.ext.max.jsapi.Harness.FileRevisionWatcher var8 = bool(var3, "revise") ? new io.karatelabs.ext.max.jsapi.Harness.FileRevisionWatcher(this.reviseTarget(var3), this.store.a(var1).resolve("revise-outside.js")) : null;
      io.karatelabs.ext.max.jsapi.Harness.FileBeforeSnapshot var9 = var8 == null ? this.checkBefore(var3) : null;
      io.karatelabs.ext.agent.harness.AgentHarness.Builder var10 = this.configure(var3, this.store.a(var1).resolve("screenshots"), var7, var8).runId(var1).transcriptPath(var6).onLoopCreated(this.run::attachLoop);
      if (this.publisher != null) {
         var10.addEventListener(new HarnessSseBridge((var2x, var3x) -> this.publisher.publish(var1, var2x, var3x)));
      }

      try {
         Files.createDirectories(var6.getParent());
      } catch (Exception var20) {
         logger.warn("could not create run dir {}: {}", var6.getParent(), var20.getMessage());
      }

      long var11 = System.currentTimeMillis();
      String var13 = var3.get("provider") instanceof String var14 && !var14.isBlank() ? var14 : null;
      String var21 = var3.get("model") instanceof String var22 && !var22.isBlank() ? var22 : null;
      String var23 = var3.get("req") instanceof String var24 && !var24.isBlank() ? var24.trim() : null;
      var var25 = io.karatelabs.ext.max.jsapi.EvidenceProtocolMetadata.a(
         withLoopFacts(withGoal(withDeadline(withReq(this.withLlm(loopMeta(var1, var5, var3, var2, var11), var13, var21), var23), var3), var3), var3)
      );
      this.store.a(var1, var25);
      boolean var26 = var4 && !Boolean.FALSE.equals(var3.get("record")) && !"false".equals(String.valueOf(var3.get("record")));
      this.asyncThreads.values().removeIf(var0 -> !var0.isAlive());
      Thread var18 = Thread.ofVirtual().name("harness-" + var1).unstarted(() -> {
         var var11x = new LinkedHashMap<>(var25);
         Process var12 = var26 ? io.karatelabs.ext.robot.VideoRecorder.a(this.workingDir.resolve("runs").resolve(var1).resolve("video.mp4")) : null;
         boolean var13x = true;

         try {
            if (var8 != null) {
               var8.c = this.reviseCopy(var1, var3);
            }

            io.karatelabs.ext.agent.harness.HarnessResult[] var14x = new io.karatelabs.ext.agent.harness.HarnessResult[1];
            if (var4) {
               RuntimeException[] var24x = new RuntimeException[1];
               boolean[] var16 = new boolean[1];
               String var17 = ContainerScenario.a(var2, "Agent run");
               SuiteResult var18x = ContainerScenario.a(this.workingDir, this.store.a(var1), var17, () -> {
                  var16[0] = true;

                  try {
                     var14x[0] = this.executor.apply(var10, var2);
                  } catch (RuntimeException var7xx) {
                     var24x[0] = var7xx;
                     throw var7xx;
                  }
               });
               if (var24x[0] != null) {
                  throw var24x[0];
               }

               if (!var16[0]) {
                  throw new RuntimeException(scenarioSetupError(var18x));
               }
            } else {
               var14x[0] = this.executor.apply(var10, var2);
            }

            io.karatelabs.ext.agent.harness.HarnessResult var25x = var14x[0];
            var13x = var25x == null || !var25x.isSuccess();
            this.reviseSettle(var11x, var3, var8, !var13x);
            checkSettle(var11x, var9);
            var11x.put("status", this.run.complete(var25x));
            var11x.put("finishedMs", System.currentTimeMillis());
            if (var25x != null) {
               var11x.put("iterations", var25x.state().iterations());
               var11x.put("succeeded", var25x.isSuccess());
               var11x.put("tokens", var25x.totalTokens());
               var11x.put(var25x.isSuccess() ? "result" : "error", var25x.isSuccess() ? var25x.result() : var25x.errorMessage());
               putIfPresent(var11x, "blocker", var25x.blockedResult());
               putIfPresent(var11x, "statement", var25x.statement());
               putIfPresent(var11x, "errorShort", var25x.g());
               putIfPresent(var11x, "errorHint", var25x.h());
            }

            var11x.put("evidenceFinalized", true);
            this.store.a(var1, var11x);
         } catch (Exception var22x) {
            logger.warn("harness run {} failed: {}", var1, var22x.getMessage());
            this.reviseSettle(var11x, var3, var8, false);
            checkSettle(var11x, var9);
            this.run.fail(var22x.getMessage());
            var11x.put("status", "failed");
            var11x.put("finishedMs", System.currentTimeMillis());
            var11x.put("error", var22x.getMessage());
            io.karatelabs.ext.agent.llm.LlmException var15 = io.karatelabs.ext.agent.llm.LlmException.a(var22x);
            if (var15 != null && var15.b() > 0) {
               putIfPresent(var11x, "errorShort", var15.f());
               putIfPresent(var11x, "errorHint", var15.g());
            }

            var11x.put("evidenceFinalized", true);
            this.store.a(var1, var11x);
            this.publishComplete(var1, "failed", var22x.getMessage(), (String)var11x.get("errorShort"));
         } finally {
            io.karatelabs.ext.robot.VideoRecorder.a(var12);
            cleanupVideo(this.workingDir.resolve("runs").resolve(var1).resolve("video.mp4"), var13x, var7);
            this.buildCheckGraph(var1);
         }
      });
      this.asyncThreads.put(var1, var18);
      var18.start();
      LinkedHashMap var19 = new LinkedHashMap();
      var19.put("runId", var1);
      var19.put("status", "started");
      var19.put("mode", var5);
      var19.put("task", var2);
      var19.put("transcript", var6.toString());
      return JsApi.next(
         var19,
         "Harness.status()",
         var4 ? "watch the run over noVNC (the container Chrome), then review the transcript" : "review the transcript / Harness.status() when it settles"
      );
   }

   boolean awaitQuiescent(String var1, long var2) {
      Thread var4 = this.asyncThreads.get(var1);
      if (var4 == null) {
         return true;
      } else {
         try {
            var4.join(Duration.ofMillis(var2));
         } catch (InterruptedException var6) {
            Thread.currentThread().interrupt();
            return false;
         }

         return !var4.isAlive();
      }
   }

   static boolean cleanupVideo(Path var0, boolean var1, AtomicBoolean var2) {
      try {
         if (!Files.isRegularFile(var0)) {
            return false;
         }

         if (Files.size(var0) == 0L || var1 && var2 != null && !var2.get()) {
            Files.delete(var0);
            return true;
         }
      } catch (Exception var4) {
         logger.debug("video cleanup skipped for {}: {}", var0, var4.toString());
      }

      return false;
   }

   private static String scenarioSetupError(SuiteResult var0) {
      if (var0 == null) {
         return "run setup failed: the container scenario could not be synthesized (see the run's report)";
      } else {
         var var1 = var0.getErrors();
         return var1 != null && !var1.isEmpty()
            ? "run setup failed before the agent started: " + String.join("; ", var1)
            : "run setup failed before the agent started — no events were produced (see karate-summary.html)";
      }
   }

   private void publishComplete(String var1, String var2, String var3, String var4) {
      if (this.publisher != null) {
         LinkedHashMap var5 = new LinkedHashMap();
         var5.put("type", "complete");
         var5.put("status", var2);
         var5.put("runId", var1);
         if (var3 != null) {
            var5.put("error", var3);
         }

         if (var4 != null) {
            var5.put("errorShort", var4);
         }

         var5.put("ts", System.currentTimeMillis());
         this.publisher.publish(var1, "complete", Json.stringifyStrict(var5));
      }
   }

   io.karatelabs.ext.agent.harness.AgentHarness.Builder configure(Map<String, Object> var1) {
      return this.configure(var1, null);
   }

   UnaryOperator<String> uploadFileResolver() {
      return var1 -> MaxPaths.resolveSandboxed(this.workingDir, var1).toString();
   }

   io.karatelabs.ext.agent.harness.AgentHarness.Builder configure(Map<String, Object> var1, Path var2) {
      return this.configure(var1, var2, null, null);
   }

   io.karatelabs.ext.agent.harness.AgentHarness.Builder configure(Map<String, Object> var1, Path var2, AtomicBoolean var3, File.Watcher var4) {
      boolean var6 = bool(var1, "heal");
      boolean var7 = bool(var1, "revise");
      Object var8 = var1.get("author");
      boolean var9 = var8 != null && !String.valueOf(var8).isBlank();
      AuthorToolkit var10 = !var6 && !var7 && !var9
         ? null
         : new AuthorToolkit(
            var6 ? AuthorToolkit.Mode.HEAL : (var7 ? AuthorToolkit.Mode.REVISE : AuthorToolkit.Mode.AUTHOR),
            var1.get("check") instanceof String var11 ? var11 : null,
            MaxWorkspace.checksDir(this.workingDir)
         );
      io.karatelabs.ext.agent.harness.AgentHarness.Builder var5;
      if (var1.get("troubleshoot") instanceof String var17 && !var17.isBlank()) {
         String var23 = String.valueOf(var1.get("troubleshootRunId"));
         var5 = io.karatelabs.ext.agent.harness.AgentHarness.builder()
            .systemPrompt(TroubleshootToolkit.preamble())
            .addExtension(new CheckToolkit(this.workingDir).withConsole(this.console).withTriage(this.triage))
            .historyWindow(6);
         if (Files.isDirectory(this.rulesHome())) {
            var5.addExtension(
               new RulesToolkit(this.workingDir, this.rulesHome(), this.rulesReportDir(), this.requirementsHomes())
                  .withProjectBuilder(this.projectBuilder)
                  .readOnly()
            );
         }

         var5.addExtension(new TroubleshootToolkit(this.runDir(var17).resolve("diagnosis.md"), var17, var23));
      } else if (var1.get("codify") instanceof String var19 && !var19.isBlank()) {
         var5 = io.karatelabs.ext.agent.harness.AgentHarness.builder()
            .systemPrompt(CodifyToolkit.preamble())
            .addExtension(new CheckToolkit(this.workingDir).withConsole(this.console).withTriage(this.triage))
            .addExtension(new RulesToolkit(this.workingDir, this.rulesHome(), this.rulesReportDir(), this.requirementsHomes()).withProjectBuilder(this.projectBuilder))
            .addExtension(new CodifyToolkit(this.rulesHome(), var19, requiresFrom(var1)))
            .historyWindow(6);
      } else if (bool(var1, "robot")) {
         String var22 = var6
            ? "repaired by Harness.heal — human review pending; a human commit clears this line"
            : (
               var7
                  ? "changed by Harness.revise — human review pending; a human commit clears this line"
                  : (var9 ? "written by Harness.author — human review pending; a human commit clears this line" : null)
            );
         var5 = io.karatelabs.ext.robot.RobotHarnessFactory.a(var2, this.uploadFileResolver(), var3, var10 == null ? null : var10::exitRefusal, this::storedSecret)
            .addExtension(
               new CheckToolkit(this.workingDir)
                  .withConsole(this.console)
                  .withAiNote(var22)
                  .withWatcher(var4)
                  .withVerdict(var10 == null ? null : var10.verdict())
                  .withTriage(this.triage)
            );
      } else {
         var5 = io.karatelabs.ext.agent.harness.AgentHarness.builder();
      }

      if (var10 != null) {
         var5.addExtension(var10);
         var5.historyWindow(6);
      }

      Object var18 = var1.get("provider");
      if (var18 != null && !String.valueOf(var18).isBlank()) {
         io.karatelabs.ext.agent.llm.LlmProvider.valueOf(String.valueOf(var18).toUpperCase(Locale.ROOT));
      }

      Object var20 = var1.get("model");
      this.select(
         var5,
         var18 != null && !String.valueOf(var18).isBlank() ? String.valueOf(var18) : null,
         var20 != null && !String.valueOf(var20).isBlank() ? String.valueOf(var20) : null
      );
      if (var1.get("maxIterations") instanceof Number var24) {
         var5.maxIterations(var24.intValue());
      }

      if (var1.get("deadlineMs") instanceof Number var25 && var25.longValue() > 0L) {
         var5.deadlineMillis(var25.longValue());
      }

      io.karatelabs.ext.agent.runtime.BudgetExitCondition var26 = budgetFrom(var1);
      if (var26 != null) {
         var5.budget(var26);
      }

      if (var1.get("goal") instanceof Map var28) {
         var5.addExitCondition(new io.karatelabs.ext.agent.runtime.GoalExitCondition(var28.get("name") instanceof String var29 ? var29 : null, var28.get("expr") instanceof String var30 ? var30 : null));
      }

      return var5.redactor(this.sanitizer);
   }

   static Map<String, Object> goalNotUsable(Map<String, Object> var0) {
      Object var1 = var0.get("goal");
      if (var1 == null) {
         return null;
      } else if (var1 instanceof Map var2) {
         try {
            new io.karatelabs.ext.agent.runtime.GoalExitCondition(var2.get("name") instanceof String var3 ? var3 : null, var2.get("expr") instanceof String var6 ? var6 : null);
            return null;
         } catch (IllegalArgumentException var5) {
            return JsApi.missing("{goal} rejected: " + var5.getMessage());
         }
      } else {
         return JsApi.missing(
            "{goal} must be {name?, expr} — a caller-authored JS predicate over `result` (the last tool step's structured value), e.g. {goal: {name: 'suite-green', expr: 'result && result.failed === 0'}}"
         );
      }
   }

   private static Map<String, Object> withGoal(Map<String, Object> var0, Map<String, Object> var1) {
      if (var1.get("goal") instanceof Map var2 && var2.get("expr") instanceof String var3) {
         LinkedHashMap var8 = new LinkedHashMap();
         var8.put("name", var2.get("name") instanceof String var5 && !var5.isBlank() ? var5 : "goal");
         var8.put("exprHash", shortHash(var3));
         var0.put("goal", var8);
      }

      return var0;
   }

   private static String shortHash(String var0) {
      try {
         byte[] var1 = MessageDigest.getInstance("SHA-256").digest(var0.getBytes(StandardCharsets.UTF_8));
         StringBuilder var2 = new StringBuilder();

         for (int var3 = 0; var3 < 6; var3++) {
            var2.append(String.format("%02x", var1[var3]));
         }

         return var2.toString();
      } catch (Exception var4) {
         return "unhashed";
      }
   }

   static io.karatelabs.ext.agent.runtime.BudgetExitCondition budgetFrom(Map<String, Object> var0) {
      long var1 = var0.get("maxTokens") instanceof Number var3 ? var3.longValue() : 0L;
      double var8 = var0.get("maxCostUsd") instanceof Number var5 ? var5.doubleValue() : 0.0;
      if (var1 <= 0L && var8 <= 0.0) {
         return null;
      } else {
         io.karatelabs.ext.agent.runtime.BudgetExitCondition.Builder var9 = io.karatelabs.ext.agent.runtime.BudgetExitCondition.builder();
         if (var1 > 0L) {
            var9.totalTokens(var1);
         }

         if (var8 > 0.0 && var0.get("rates") instanceof Map var10) {
            var9.maxCost(var8).tokenPrices(ratePer1M(var10, "inputPer1M"), ratePer1M(var10, "outputPer1M"));
         }

         return var9.build();
      }
   }

   private static double ratePer1M(Map<?, ?> var0, String var1) {
      return var0.get(var1) instanceof Number var2 ? var2.doubleValue() : 0.0;
   }

   static Map<String, Object> budgetNotUsable(Map<String, Object> var0) {
      double var1 = var0.get("maxCostUsd") instanceof Number var3 ? var3.doubleValue() : 0.0;
      if (var1 <= 0.0) {
         return null;
      } else {
         return !(var0.get("rates") instanceof Map var5 && (ratePer1M(var5, "inputPer1M") > 0.0 || ratePer1M(var5, "outputPer1M") > 0.0))
            ? JsApi.missing(
               "{maxCostUsd} needs {rates: {inputPer1M, outputPer1M}} — your model's per-1M-token USD prices (pricing is model-specific and never hardcoded), e.g. {maxCostUsd: 2, rates: {inputPer1M: 3, outputPer1M: 15}}. For a price-free cap use {maxTokens} instead."
            )
            : null;
      }
   }

   private static void putIfPresent(Map<String, Object> var0, String var1, String var2) {
      if (var2 != null && !var2.isBlank()) {
         var0.put(var1, var2);
      }
   }

   private static boolean bool(Map<String, Object> var0, String var1) {
      Object var2 = var0.get(var1);
      return Boolean.TRUE.equals(var2) || "true".equals(String.valueOf(var2));
   }

   private static void putIfPresent(Map<String, Object> var0, String var1, Object var2) {
      if (var2 != null) {
         var0.put(var1, var2);
      }
   }

   static Map<String, Object> project(io.karatelabs.ext.agent.harness.HarnessResult var0) {
      LinkedHashMap var1 = new LinkedHashMap();
      var1.put("runId", var0.runId());
      var1.put("status", var0.state().status().name());
      var1.put("iterations", var0.state().iterations());
      var1.put("succeeded", var0.isSuccess());
      if (var0.isSuccess()) {
         var1.put("result", var0.result());
      } else {
         var1.put("error", var0.errorMessage());
         putIfPresent(var1, "errorShort", var0.g());
         putIfPresent(var1, "errorHint", var0.h());
         putIfPresent(var1, "blocker", var0.blockedResult());
      }

      putIfPresent(var1, "statement", var0.statement());
      var1.put("tokens", var0.totalTokens());
      if (var0.transcriptPath() != null) {
         var1.put("transcript", var0.transcriptPath().toString());
      }

      return var1;
   }

   private Path resolve(String var1) {
      Path var2 = Path.of(var1);
      return var2.isAbsolute() ? var2 : this.workingDir.resolve(var1);
   }

   private static Map<String, Object> cast(Map<?, ?> var0) {
      return (Map<String, Object>)(Map<?, ?>)var0;
   }

   private static final class ExecutionEvidenceContext {
      final String a;
      final String b;
      final long c;
      final String d;
      boolean e;
      final List<String[]> f = new ArrayList<>();
      String g;
      final List<Map<String, Object>> h = new ArrayList<>();
      final List<Map<String, Object>> i = new ArrayList<>();
      final Set<String> j = new LinkedHashSet<>();
      final long k;

      ExecutionEvidenceContext(String var1, String var2, long var3, String var5, boolean var6) {
         this.a = var1;
         this.b = var2;
         this.c = var3;
         this.d = var5;
         this.e = var6;
         this.k = io.karatelabs.ext.common.EvidenceContext.b();
      }
   }

   private record FileBeforeSnapshot(Path a, byte[] b) {

   }

   private record NumberedIdPrefix(String a, int b) {


      String a(long var1) {
         return this.a + String.format("%0" + this.b + "d", var1);
      }
   }

   private static final class FileRevisionWatcher implements File.Watcher {
      private final Path a;
      private final Path b;
      private volatile byte[] c;
      private volatile Optional<byte[]> d;
      private volatile boolean e;
      private volatile boolean f;

      FileRevisionWatcher(Path var1, Path var2) {
         this.a = var1.toAbsolutePath().normalize();
         this.b = var2;
      }

      private byte[] a() {
         Optional var1 = this.d;
         return var1 == null ? this.c : (byte[])var1.orElse(null);
      }

      @Override
      public void changing(Path var1) {
         if (this.a.startsWith(var1)) {
            byte[] var2 = Harness.bytesOrNull(this.a);
            if (!Arrays.equals(var2, this.a())) {
               this.e = true;
               if (var2 != null) {
                  try {
                     Files.write(this.b, var2);
                     this.f = true;
                  } catch (IOException var4) {
                     Harness.logger.warn("could not keep {} before the revise changed it: {}", this.a, var4.getMessage());
                  }
               }
            }
         }
      }

      @Override
      public void changed(Path var1) {
         if (this.a.startsWith(var1)) {
            this.d = Optional.ofNullable(Harness.bytesOrNull(this.a));
         }
      }

      boolean a(byte[] var1) {
         return !this.e && Arrays.equals(var1, this.a());
      }
   }
}
