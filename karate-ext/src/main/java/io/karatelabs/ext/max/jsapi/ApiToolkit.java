package io.karatelabs.ext.max.jsapi;

import io.karatelabs.common.StringUtils;
import io.karatelabs.ext.agent.runtime.AgentContext;
import io.karatelabs.ext.agent.runtime.AgentHook;
import io.karatelabs.ext.agent.runtime.AgentOutcome;
import io.karatelabs.ext.agent.runtime.AgentExtension;
import io.karatelabs.ext.common.MaxPaths;
import io.karatelabs.ext.common.jsapi.JsApi;
import io.karatelabs.ext.common.jsapi.JsDispatch;
import io.karatelabs.ext.http.jsapi.Http;
import io.karatelabs.ext.max.apitest.CandidateRunner;
import io.karatelabs.ext.max.apitest.EditResult;
import io.karatelabs.ext.max.apitest.FeatureEdits;
import io.karatelabs.ext.max.apitest.FeatureSource;
import io.karatelabs.ext.max.apitest.TestId;
import io.karatelabs.ext.max.apitest.TriageGate;
import io.karatelabs.ext.max.apitest.WeakeningDiff;
import io.karatelabs.gherkin.Feature;
import io.karatelabs.gherkin.FeatureSection;
import io.karatelabs.gherkin.Step;
import io.karatelabs.gherkin.Tag;
import io.karatelabs.js.JavaInvokable;
import io.karatelabs.js.SimpleObject;
import io.karatelabs.js.Terms;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public final class ApiToolkit implements AgentExtension {
   static final int MAX_FIX_REDS = 3;
   static final long TOOL_TIMEOUT_MS = 180000L;
   public static final String RULES = "- Chain, never replay: read a value a call answered (an id, a token) from its response; never paste a recorded value where a chain or `secret` exists.\n- A credential is never in a step: it reads `secret('NAME')`; `ApiTest.config({name, secret: 'NAME', ifMatch})` sets it under each name the steps read (a migration's `needs`).\n- A test is saved only by a green run of the source saved.\n- Link a criterion only once each outcome it names is asserted (a migration's `proposed`).\n";
   static final String PROMPT = "You are the API test assistant of a manual tester who does not read code. One run answers one tester message.\n- A reply is plain words about what the tester sees: never a tool or call name, code, step syntax, a secret's name, or how the console works inside. Say saved only when this run's result says so.\n- A request: do it, then the obvious next step. A question (\"where do I start?\"): answer it and offer the next step; change nothing. Ask only what is the tester's to decide: bug or test, which criteria to link, weakening a check.\n- Say a test's or criterion's status only from this run's read: `ApiTest.read`, `Requirement.matrix()`. The task says what the page shows.\n- Build in order: read the project's API spec (`File.list()`, `File.read('openapi.yaml')`), Postman collection (`Postman.read(path)`) or tests before you probe or guess; probe each call; steps only from calls that answered 2xx.\n- Probe: `Http.get(url, {headers})`, `Http.post(url, body, {headers})` (also put, patch, delete) → `{status, body}`; a large body → `bodyShape`, `bodyTruncated` (`{full: true}` gets it). A 4xx body says what is wrong: change the request, never resend it unchanged. Never retype a masked value: use the variable (`r.body.token`), a typed credential `secret('NAME')`.\n- Read: `ApiTest.read(path)` → cards and `version`; `ApiTest.exchanges(path)` → the newest run: a failing card's `error` and exchange say why; `ApiTest.config()` → what is set (`set`, `entries`): keep it.\n- Write: `ApiTest.createFeature(path, {name, scenarioName, steps})`, then edit verbs (`insertStep`, `replaceStep` …, each `{…, ifMatch: version}`). `steps`: one step per string, each call's `# Title` the first line of its first step: `['# Sign in\\n* url baseUrl', \"* path 'login'\", '* method post']`. The host is `* url baseUrl` (unset: `ApiTest.config({name: 'baseUrl', value, ifMatch})`).\n- Chain, never replay: read a value a call answered (an id, a token) from its response; never paste a recorded value where a chain or `secret` exists.\n- A credential is never in a step: it reads `secret('NAME')`; `ApiTest.config({name, secret: 'NAME', ifMatch})` sets it under each name the steps read (a migration's `needs`).\n- A test is saved only by a green run of the source saved.\n- Link a criterion only once each outcome it names is asserted (a migration's `proposed`).\n- Edits and `ApiTest.config` change a copy. `ApiTest.run(path)` runs it, or the saved test as it is: `green` saves both. A red with `fix: true` is yours to fix, no question. Any other red holds the test: change nothing; reply with the failing call, expected and got, and ask: bug or test?\n- The tester answers with the page's buttons or in words; unclear: ask for \"it's a bug\" or \"the test is wrong\". Bug: `ApiTest.markBug(path, {note})` saves it marked if it fails the same way. Test: fix the copy and run it.\n- Weaken a check (loosen a match, drop a status) only once the tester says yes; then do it.\n- After a green save: `Api.proposeLinks(path, {scenario, reqs: ['ID/1']})` for what it proves, unless `linked`; a saved bug links nothing.\n- New criteria: `Requirement.list()`, probe the API, then `Api.draft(markdown, {file: 'name.md'})` with only what no requirement holds; the tester saves them.\n- A Postman folder (`Postman.migrate(path, {item, to?})`) or a recording (`Traffic.migrate(runId, {moves})`) becomes the copy at `to`: do its `next` (`unset`: from what `known` names); name each guessed chain and `needs` in the reply; never keep a left-out call without saying why.\n- Link pages by path: run report /manual/runs/<runId> (a result's `report`), Coverage /manual/coverage, Home /manual.\n- A refusal `{error, code}` changed nothing.";
   private static final List<String> HTTP_VERBS = List.of("get", "post", "put", "patch", "delete", "send", "configure", "cookies");
   private static final List<String> REQUIREMENT_READS = List.of("list", "get", "matrix", "gaps", "read", "readiness");
   private static final List<String> FILE_READS = List.of("read", "list", "exists");
   private static final List<String> EDIT_VERBS = List.of(
      "insertStep",
      "replaceStep",
      "deleteStep",
      "insertComment",
      "replaceComment",
      "deleteComment",
      "insertRow",
      "replaceRow",
      "deleteRow",
      "createScenario",
      "createFeature"
   );
   private final Path root;
   private final ApiTest api;
   private final TriageGate triage;
   private final Runner runner;
   private final SimpleObject requirement;
   private final Consumer<Map<String, Object>> cards;
   private final BiFunction<String, io.karatelabs.ext.agent.llm.LlmClient, Map<String, Object>> draft;
   private final ApiReplyToolkit reply = new ApiReplyToolkit();
   private final Http http;
   private final File files;
   private Postman postman;
   private Traffic traffic;
   private Function<String, String> secrets = System::getenv;
   private Supplier<Collection<String>> secretNames = List::of;
   private final Map<String, io.karatelabs.ext.max.jsapi.ApiToolkit.HeldTestVersion> held = new LinkedHashMap<>();
   private ApiTest.HeldConfig heldConfig;
   private String heldConfigOf;
   private ApiToolkit.Drafts drafts = new ApiToolkit.Drafts();
   private final Map<String, Integer> fixReds = new LinkedHashMap<>();
   private boolean ask;
   private final String turnRun;
   private AgentContext ctx;
   static final String REPEAT = "The source and settings are unchanged since the previous failed run: read this run's response (ApiTest.exchanges) before running again, or ask the tester.";
   private static final List<String> CREDENTIAL_KINDS = List.of("password", "token", "secret", "key");
   private static final Set<WeakeningDiff.Kind> WEAKENS = EnumSet.of(
      WeakeningDiff.Kind.MATCH_DROPPED, WeakeningDiff.Kind.MATCH_LOOSENED, WeakeningDiff.Kind.STATUS_DROPPED
   );
   private static final Set<String> UNWALKED = Set.of("target", "runs", "node_modules", ".git");

   static String js(Object var0) {
      if (var0 instanceof Map var3) {
         ArrayList var2 = new ArrayList();
         var3.forEach((var1x, var2x) -> var2.add(var1x + ": " + js(var2x)));
         return "{" + String.join(", ", var2) + "}";
      } else {
         return var0 instanceof List var1
            ? "[" + String.join(", ", var1.stream().map(ApiToolkit::js).toList()) + "]"
            : "'" + String.valueOf(var0).replace("\\", "\\\\").replace("'", "\\'") + "'";
      }
   }

   public ApiToolkit(
      Path var1,
      ApiTest var2,
      TriageGate var3,
      Runner var4,
      SimpleObject var5,
      Consumer<Map<String, Object>> var6,
      BiFunction<String, io.karatelabs.ext.agent.llm.LlmClient, Map<String, Object>> var7,
      String var8
   ) {
      this.turnRun = var8;
      this.root = var1.toAbsolutePath().normalize();
      this.api = var2;
      this.triage = var3;
      this.runner = var4;
      this.requirement = var5;
      this.cards = var6;
      this.draft = var7;
      this.http = new Http().withRoot(this.root).onExchange(this::callCard);
      this.files = File.at(this.root).withTriage(var3);
   }

   public ApiToolkit withPostman(Postman var1) {
      this.postman = var1;
      return this;
   }

   public ApiToolkit withTraffic(Traffic var1) {
      this.traffic = var1;
      return this;
   }

   public ApiToolkit withSecrets(Function<String, String> var1) {
      this.secrets = var1;
      return this;
   }

   public ApiToolkit withSecretNames(Supplier<Collection<String>> var1) {
      this.secretNames = var1;
      return this;
   }

   public ApiToolkit withDrafts(ApiToolkit.Drafts var1) {
      this.drafts = var1;
      this.heldConfig = var1.config();
      this.heldConfigOf = this.heldConfig == null ? null : var1.owner();
      return this;
   }

   public ApiToolkit withAsk(boolean var1) {
      this.ask = var1;
      return this;
   }

   @Override
   public void init(AgentContext var1) {
      this.ctx = var1;
      this.reply.init(var1);
   }

   @Override
   public String promptFragment() {
      return "You are the API test assistant of a manual tester who does not read code. One run answers one tester message.\n- A reply is plain words about what the tester sees: never a tool or call name, code, step syntax, a secret's name, or how the console works inside. Say saved only when this run's result says so.\n- A request: do it, then the obvious next step. A question (\"where do I start?\"): answer it and offer the next step; change nothing. Ask only what is the tester's to decide: bug or test, which criteria to link, weakening a check.\n- Say a test's or criterion's status only from this run's read: `ApiTest.read`, `Requirement.matrix()`. The task says what the page shows.\n- Build in order: read the project's API spec (`File.list()`, `File.read('openapi.yaml')`), Postman collection (`Postman.read(path)`) or tests before you probe or guess; probe each call; steps only from calls that answered 2xx.\n- Probe: `Http.get(url, {headers})`, `Http.post(url, body, {headers})` (also put, patch, delete) → `{status, body}`; a large body → `bodyShape`, `bodyTruncated` (`{full: true}` gets it). A 4xx body says what is wrong: change the request, never resend it unchanged. Never retype a masked value: use the variable (`r.body.token`), a typed credential `secret('NAME')`.\n- Read: `ApiTest.read(path)` → cards and `version`; `ApiTest.exchanges(path)` → the newest run: a failing card's `error` and exchange say why; `ApiTest.config()` → what is set (`set`, `entries`): keep it.\n- Write: `ApiTest.createFeature(path, {name, scenarioName, steps})`, then edit verbs (`insertStep`, `replaceStep` …, each `{…, ifMatch: version}`). `steps`: one step per string, each call's `# Title` the first line of its first step: `['# Sign in\\n* url baseUrl', \"* path 'login'\", '* method post']`. The host is `* url baseUrl` (unset: `ApiTest.config({name: 'baseUrl', value, ifMatch})`).\n- Chain, never replay: read a value a call answered (an id, a token) from its response; never paste a recorded value where a chain or `secret` exists.\n- A credential is never in a step: it reads `secret('NAME')`; `ApiTest.config({name, secret: 'NAME', ifMatch})` sets it under each name the steps read (a migration's `needs`).\n- A test is saved only by a green run of the source saved.\n- Link a criterion only once each outcome it names is asserted (a migration's `proposed`).\n- Edits and `ApiTest.config` change a copy. `ApiTest.run(path)` runs it, or the saved test as it is: `green` saves both. A red with `fix: true` is yours to fix, no question. Any other red holds the test: change nothing; reply with the failing call, expected and got, and ask: bug or test?\n- The tester answers with the page's buttons or in words; unclear: ask for \"it's a bug\" or \"the test is wrong\". Bug: `ApiTest.markBug(path, {note})` saves it marked if it fails the same way. Test: fix the copy and run it.\n- Weaken a check (loosen a match, drop a status) only once the tester says yes; then do it.\n- After a green save: `Api.proposeLinks(path, {scenario, reqs: ['ID/1']})` for what it proves, unless `linked`; a saved bug links nothing.\n- New criteria: `Requirement.list()`, probe the API, then `Api.draft(markdown, {file: 'name.md'})` with only what no requirement holds; the tester saves them.\n- A Postman folder (`Postman.migrate(path, {item, to?})`) or a recording (`Traffic.migrate(runId, {moves})`) becomes the copy at `to`: do its `next` (`unset`: from what `known` names); name each guessed chain and `needs` in the reply; never keep a left-out call without saying why.\n- Link pages by path: run report /manual/runs/<runId> (a result's `report`), Coverage /manual/coverage, Home /manual.\n- A refusal `{error, code}` changed nothing.\n"
         + this.reply.promptFragment();
   }

   @Override
   public List<String> terminalCallNames() {
      return this.reply.terminalCallNames();
   }

   @Override
   public long toolTimeoutMs() {
      return 180000L;
   }

   @Override
   public List<AgentHook> hooks() {
      return this.reply.hooks();
   }

   @Override
   public AgentOutcome proseOutcome(String var1) {
      return this.reply.proseOutcome(var1);
   }

   @Override
   public Map<String, Object> bindings() {
      Object var1 = ((SimpleObject)this.reply.bindings().get("Api")).jsGet("reply");
      LinkedHashMap var2 = new LinkedHashMap();
      var2.put(
         "Api",
         new ApiToolkit.Verbs(
            Map.of(
               "reply",
               var1,
               "proposeLinks",
               (JavaInvokable)var1x -> this.guarded("Api.proposeLinks", false, () -> this.proposeLinks(JsApi.str(var1x, 0), JsApi.arg(var1x, 1))),
               "draft",
               (JavaInvokable)var1x -> this.guarded("Api.draft", false, () -> this.draft(JsApi.arg(var1x, 0), JsApi.arg(var1x, 1)))
            )
         )
      );
      var2.put("Http", ApiToolkit.Verbs.of("Http", this.http, HTTP_VERBS, false, this.reply));
      var2.put("ApiTest", new ApiToolkit.CandidateApi());
      var2.put("secret", (JavaInvokable)var1x -> {
         String var2x = JsApi.str(var1x, 0);
         String var3x = var2x == null ? null : this.secrets.apply(var2x);
         if (var3x == null) {
            throw new RuntimeException("secret('" + var2x + "') is not stored: ask the tester for its value");
         } else {
            return var3x;
         }
      });
      var2.put("Requirement", ApiToolkit.Verbs.of("Requirement", this.requirement, REQUIREMENT_READS, true, this.reply));
      var2.put("File", ApiToolkit.Verbs.of("File", this.files, FILE_READS, true, this.reply));
      if (this.postman != null) {
         LinkedHashMap var3 = new LinkedHashMap();

         for (String var5 : List.of("read", "item")) {
            if (this.postman.jsGet(var5) instanceof JavaInvokable var6) {
               var3.put(var5, (JavaInvokable)var3x -> this.guarded("Postman." + var5, true, () -> var6.invoke(var3x)));
            }
         }

         var3.put("migrate", (JavaInvokable)var1x -> this.guarded("Postman.migrate", false, () -> this.migrate(JsApi.arg(var1x, 0), JsApi.arg(var1x, 1))));
         var2.put("Postman", new ApiToolkit.Verbs(var3));
      }

      if (this.traffic != null) {
         LinkedHashMap var8 = new LinkedHashMap();

         for (String var10 : List.of("read", "sift")) {
            if (this.traffic.jsGet(var10) instanceof JavaInvokable var11) {
               var8.put(var10, (JavaInvokable)var3x -> this.guarded("Traffic." + var10, true, () -> var11.invoke(var3x)));
            }
         }

         var8.put("migrate", (JavaInvokable)var1x -> this.guarded("Traffic.migrate", false, () -> this.trafficMigrate(JsApi.arg(var1x, 0), JsApi.arg(var1x, 1))));
         var2.put("Traffic", new ApiToolkit.Verbs(var8));
      }

      return var2;
   }

   private Object guarded(String var1, boolean var2, Supplier<Object> var3) {
      return this.reply.called(var1, var2, var3);
   }

   private void callCard(Map<String, Object> var1) {
      Map var2 = var1.get("request") instanceof Map var3 ? var3 : Map.of();
      Map var7 = var1.get("response") instanceof Map var8 ? var8 : Map.of();
      LinkedHashMap var9 = new LinkedHashMap();
      var9.put("kind", "call");
      var9.put("method", String.valueOf(var2.get("method")).toUpperCase());
      var9.put("url", String.valueOf(var2.get("url")));
      var9.put("status", var7.get("status"));
      if (var7.get("responseTime") instanceof Number var10) {
         var9.put("ms", var10.longValue());
      }

      var9.put("sent", clip(var2.get("body")));
      var9.put("got", clip(var7.get("body")));
      this.cards.accept(var9);
   }

   private static Object clip(Object var0) {
      if (var0 == null) {
         return null;
      } else {
         String var1 = var0 instanceof String var2 ? var2 : StringUtils.formatJson(var0, true, false, false);
         return var1.length() > 2000 ? var1.substring(0, 2000) + "…" : var1;
      }
   }

   private Object file(String var1, String var2) {
      if (var2 != null && var2.endsWith(".feature")) {
         try {
            return MaxPaths.resolveSandboxed(this.root, var2);
         } catch (SecurityException var4) {
            return JsApi.missing(var1 + " denied: " + var4.getMessage());
         }
      } else {
         return JsApi.missing(var1 + "(path, …) needs a project-relative .feature path");
      }
   }

   private static String rel(TestId var0) {
      return var0.path();
   }

   Object read(String var1, Object var2) {
      Object var3 = this.file("ApiTest.read", var1);
      if (var3 instanceof Map var8) {
         return var8;
      } else {
         byte[] var4 = this.candidate(this.triage.id((Path)var3));
         if (var4 == null) {
            return unseen(this.api.read(var1, var2));
         } else {
            Object var5 = this.api.readCandidate(var1, var4, var2);
            if (var5 instanceof Map var6 && var6.get("error") == null) {
               var6.put("unsaved", true);
            }

            return var5;
         }
      }
   }

   private byte[] candidate(TestId var1) {
      io.karatelabs.ext.max.jsapi.ApiToolkit.HeldTestVersion var2 = this.heldOf(var1);
      if (var2 != null) {
         return var2.a;
      } else {
         return this.reopened(this.triage.triage(var1)) ? this.triage.candidateBytes(var1) : null;
      }
   }

   Object exchanges(String var1, Object var2) {
      Object var3 = this.file("ApiTest.exchanges", var1);
      if (var3 instanceof Map var13) {
         return var13;
      } else {
         TestId var4 = this.triage.id((Path)var3);
         byte[] var5 = this.candidate(var4);
         Object var6 = var2 instanceof Map var7 ? var7.get("runId") : null;
         io.karatelabs.ext.max.jsapi.ApiToolkit.DraftRunRecord var14 = this.drafts.ran.get(rel(var4));
         var var8 = this.triage.triage(var4);
         if (var14 == null && this.reopened(var8) && var8.get("runId") instanceof String var9) {
            byte[] var17 = this.triage.candidateBytes(var4);
            var14 = new io.karatelabs.ext.max.jsapi.ApiToolkit.DraftRunRecord(var9, var17, var17, null, true);
         }

         if (var5 != null && (var6 == null || var14 != null && var6.equals(var14.a()))) {
            if (var14 == null) {
               LinkedHashMap var16 = new LinkedHashMap();
               var16.put("path", var4.path());
               var16.put("scenarios", List.of());
               var16.put("unsaved", true);
               return JsApi.next(var16, "ApiTest.run('" + var4.path() + "')");
            } else {
               Object var15 = this.api.exchangesRan(var1, var14.c(), var14.a(), var2);
               if (var15 instanceof Map var18 && var18.get("error") == null) {
                  boolean var12 = Arrays.equals(var14.b(), var5);
                  var18.put("current", var12);
                  if (!var12) {
                     var18.put("ranOn", var14.a());
                  }

                  var18.put("unsaved", true);
               }

               return var15;
            }
         } else {
            return unseen(this.api.exchanges(var1, var2));
         }
      }
   }

   private Object side(Object var1) {
      if (var1 instanceof String var2 && this.file("ApiTest.diff", var2) instanceof Path var3) {
         byte[] var5 = this.candidate(this.triage.id(var3));
         return var5 == null ? var1 : Map.of("text", new String(var5, StandardCharsets.UTF_8));
      } else {
         return var1;
      }
   }

   private static Object unseen(Object var0) {
      if (var0 instanceof Map var1
         && "NOT_FOUND".equals(var1.get("code"))
         && var1.get("error") instanceof String var2
         && var2.endsWith(" — File.list() shows what is there")) {
         var1.put("error", var2.replace(" — File.list() shows what is there", " — ApiTest.read(path) reads the test the task names"));
      }

      return var0;
   }

   private Object start(TestId var1) {
      var var2 = this.triage.triage(var1);
      String var3 = String.valueOf(var2.get("state"));
      boolean var4 = this.reopened(var2);
      if (!"none".equals(var3) && !var4) {
         var var8 = this.triage.check(var1.file());
         return var8 != null ? var8 : JsApi.missing("the triage of " + var1.path() + " is " + var3 + " — ask the tester to answer it");
      } else {
         io.karatelabs.ext.max.jsapi.ApiToolkit.HeldTestVersion var5 = this.heldOf(var1);
         if (var5 != null) {
            return var5;
         } else if (var4) {
            return new io.karatelabs.ext.max.jsapi.ApiToolkit.HeldTestVersion(this.triage.candidateBytes(var1), (String)var2.get("base"));
         } else {
            try {
               byte[] var6 = Files.isRegularFile(var1.file()) ? Files.readAllBytes(var1.file()) : null;
               return new io.karatelabs.ext.max.jsapi.ApiToolkit.HeldTestVersion(var6, var6 == null ? null : io.karatelabs.ext.common.Sha256.a(var6));
            } catch (IOException var7) {
               return JsApi.notFound("could not read " + var1.path() + ": " + var7.getMessage());
            }
         }
      }
   }

   private io.karatelabs.ext.max.jsapi.ApiToolkit.HeldTestVersion heldOf(TestId var1) {
      io.karatelabs.ext.max.jsapi.ApiToolkit.HeldTestVersion var2 = this.held.get(rel(var1));
      if (this.drafts.holds(rel(var1)) && (var2 == null || var2.b == null)) {
         String var3 = this.drafts.base.get(rel(var1));
         if (var3 == null && Files.exists(var1.file())) {
            this.held.remove(rel(var1));
            this.drafts.drop(rel(var1));
            this.drafts.ran.remove(rel(var1));
            if (rel(var1).equals(this.heldConfigOf)) {
               this.heldConfig = null;
               this.heldConfigOf = null;
            }

            return null;
         } else if (var2 != null) {
            return var2;
         } else {
            var2 = new io.karatelabs.ext.max.jsapi.ApiToolkit.HeldTestVersion(this.drafts.bytes.get(rel(var1)), var3);
            this.held.put(rel(var1), var2);
            return var2;
         }
      } else {
         return var2;
      }
   }

   private void hold(TestId var1, io.karatelabs.ext.max.jsapi.ApiToolkit.HeldTestVersion var2) {
      this.held.put(rel(var1), var2);
      this.drafts.bytes.put(rel(var1), var2.a);
      if (var2.b == null) {
         this.drafts.base.remove(rel(var1));
      } else {
         this.drafts.base.put(rel(var1), var2.b);
      }

      this.own();
   }

   private void own() {
      this.drafts.own(this.heldConfig);
      this.heldConfigOf = this.drafts.owner();
   }

   private ApiTest.HeldConfig heldConfig() {
      if (this.heldConfigOf != null) {
         this.heldOf(this.triage.id(this.root.resolve(this.heldConfigOf)));
      }

      return this.heldConfig;
   }

   private void release(TestId var1) {
      this.heldConfig = null;
      this.heldConfigOf = null;
      this.held.remove(rel(var1));
      this.drafts.drop(rel(var1));
      this.drafts.own(null);
   }

   private boolean reopened(Map<String, Object> var1) {
      return "answered".equals(var1.get("state"))
         && var1.get("answer") instanceof Map var2
         && "test".equals(var2.get("choice"))
         && (!Boolean.TRUE.equals(var1.get("busy")) || Objects.equals(var2.get("runId"), this.turnRun));
   }

   Object edit(String var1, String var2, Object var3) {
      Object var4 = this.file("ApiTest." + var1, var2);
      if (var4 instanceof Map var9) {
         return var9;
      } else {
         TestId var5 = this.triage.id((Path)var4);
         Object var6 = this.start(var5);
         if (var6 instanceof io.karatelabs.ext.max.jsapi.ApiToolkit.HeldTestVersion var7) {
            ApiTest.Candidate var8 = this.api.candidateEdit(var1, var2, var7.a, var3);
            if (var8.bytes() != null) {
               this.hold(var5, new io.karatelabs.ext.max.jsapi.ApiToolkit.HeldTestVersion(var8.bytes(), var7.b));
               var8.answer().put("unsaved", true);
               this.uncard(var5);
            }

            return var8.answer();
         } else {
            return var6;
         }
      }
   }

   private void uncard(TestId var1) {
      LinkedHashMap var2 = new LinkedHashMap();
      var2.put("kind", "uncard");
      var2.put("to", var1.path());
      this.cards.accept(var2);
   }

   Object migrate(Object var1, Object var2) {
      Object var3 = this.postman.migration(var1, var2);
      return var3 instanceof Postman.Migrated var4 ? this.holdMigration("Postman.migrate", var4, Map.of(), var0 -> false) : var3;
   }

   // $VF: One or more variable merging failures!
   // $VF: Could not properly define all variable types!
   // Please report this to the Vineflower issue tracker, at https://github.com/Vineflower/vineflower/issues with a copy of the class file (if you have the rights to distribute it!)
   Object trafficMigrate(Object var1, Object var2) {
      Predicate<byte[]> var3 = var1x -> String.valueOf(var1).equals(io.karatelabs.ext.robot.recording.RecordingScriptGenerator.a(var1x));
      Object var4 = this.traffic.candidate(var1, var2, var1x -> var3.test(io.karatelabs.ext.robot.recording.RecordingScriptGenerator.a(var1x)));
      if (!(var4 instanceof Traffic.Candidate var5)) {
         return var4;
      } else {
         Object var6 = this.holdMigration("Traffic.migrate", var5.migrated(), var5.config(), var3);
         if (var6 instanceof Map var7 && var7.get("error") == null) {
            LinkedHashMap var8 = new LinkedHashMap();
            ArrayList var9 = new ArrayList();
            if (var2 instanceof Map var10) {
               for (String var12 : List.of("to", "app", "hosts")) {
                  if (var10.get(var12) != null) {
                     var8.put(var12, var10.get(var12));
                  }
               }

               for (Object var20 : var10.get("moves") instanceof List ? (List)var10.get("moves") : List.of()) {
                  LinkedHashMap var21 = new LinkedHashMap();

                  for (String var15 : List.of("id", "to", "by", "reason")) {
                     if (var20 instanceof Map var16 && var16.get(var15) != null) {
                        var21.put(var15, String.valueOf(var16.get(var15)));
                     }
                  }

                  var9.add(var21);
               }
            }

            this.drafts.recorded.put(var5.migrated().to(), new io.karatelabs.ext.max.jsapi.ApiToolkit.TrafficDraft(String.valueOf(var1), var8, var9));
            LinkedHashMap var17 = new LinkedHashMap();
            var17.put("kind", "traffic");
            var17.put("to", var5.migrated().to());
            var17.put("sift", var5.migrated().answer().get("sift"));
            this.cards.accept(var17);
         }

         return var6;
      }
   }

   // $VF: One or more variable merging failures!
   // $VF: Could not properly define all variable types!
   // Please report this to the Vineflower issue tracker, at https://github.com/Vineflower/vineflower/issues with a copy of the class file (if you have the rights to distribute it!)
   private Object holdMigration(String var1, Postman.Migrated var2, Map<String, String> var3, Predicate<byte[]> var4) {
      Object var5 = this.file(var1, var2.to());
      if (var5 instanceof Map var17) {
         return var17;
      } else {
         TestId var6 = this.triage.id((Path)var5);
         Object var7 = this.start(var6);
         if (!(var7 instanceof io.karatelabs.ext.max.jsapi.ApiToolkit.HeldTestVersion)) {
            return var7;
         } else {
            String var8 = null;
            if (Files.exists(var6.file())) {
               byte[] var9;
               try {
                  var9 = Files.readAllBytes(var6.file());
               } catch (IOException var16) {
                  return JsApi.notFound("could not read " + var6.path() + ": " + var16.getMessage());
               }

               if (!var4.test(var9)) {
                  var var20 = JsApi.missing(var6.path() + " already exists — pass another to");
                  var20.put("code", "EXISTS");
                  var20.put("status", 409);
                  return var20;
               }

               var8 = io.karatelabs.ext.common.Sha256.a(var9);
            }

            this.hold(var6, new io.karatelabs.ext.max.jsapi.ApiToolkit.HeldTestVersion(var2.bytes(), var8));
            this.drafts.recorded.remove(rel(var6));
            this.fixReds.remove(rel(var6));
            ArrayList var18 = new ArrayList();

            for (Object var21 : var2.answer().get("proposed") instanceof List ? (List)var2.answer().get("proposed") : List.of()) {
               if (var21 instanceof Map var24) {
                  var18.add(var24);
               }
            }

            if (var18.isEmpty()) {
               this.drafts.outcomes.remove(rel(var6));
            } else {
               this.drafts.outcomes.put(rel(var6), new StepInsertionPlanner(var2.bytes(), var18));
            }

            List var19 = var2.answer().get("needs") instanceof List var22 ? var22.stream().map(String::valueOf).toList() : List.of();
            this.drafts.needs.put(rel(var6), var19);
            var var23 = new LinkedHashMap<>(var2.answer());
            var23.put("unsaved", true);
            ArrayList<String> var26 = new ArrayList<>();
            Set var13 = var3.isEmpty() ? Set.of() : this.configured();
            var3.forEach((var2x, var3x) -> {
               if (!var13.contains(var2x)) {
                  var26.add("ApiTest.config({name: '" + var2x + "', value: '" + var3x + "', ifMatch})");
               }
            });
            List var14 = this.unset(var19);
            if (!var14.isEmpty()) {
               var23.put("unset", var14);
               List var15 = this.known(var14);
               if (!var15.isEmpty()) {
                  var23.put("known", var15);
               }

               var26.add(
                  "ApiTest.config({name, value, ifMatch}) for each unset variable — a credential from what `known` names (`{name, secret: 'NAME', ifMatch}`), else ask the tester to type it"
               );
            }

            var26.add("ApiTest.run('" + var6.path() + "')");
            return Traffic.ordered(JsApi.next(var23, var26.toArray(String[]::new)));
         }
      }
   }

   // $VF: One or more variable merging failures!
   // $VF: Could not properly define all variable types!
   // Please report this to the Vineflower issue tracker, at https://github.com/Vineflower/vineflower/issues with a copy of the class file (if you have the rights to distribute it!)
   private Set<String> configured() {
      Object var1 = this.api.configCandidate(null, this.heldConfig()).answer();
      LinkedHashSet var2 = new LinkedHashSet();
      if (var1 instanceof Map var3) {
         for (String var5 : List.of("entries", "set")) {
            for (Object var9 : var3.get(var5) instanceof List ? (List)var3.get(var5) : List.of()) {
               if (var9 instanceof Map var10 && var10.get("name") != null) {
                  var2.add(String.valueOf(var10.get("name")));
               }
            }
         }
      }

      return var2;
   }

   private List<String> unset(List<String> var1) {
      if (var1 != null && !var1.isEmpty()) {
         var var2 = this.configured();
         return var1.stream().filter(var1x -> !var2.contains(var1x)).toList();
      } else {
         return List.of();
      }
   }

   private List<String> known(List<String> var1) {
      var var2 = this.configured();
      var var3 = this.secretNames.get();
      ArrayList var4 = new ArrayList();

      for (String var6 : var1) {
         String var7 = var6.toLowerCase(Locale.ROOT);
         String var8 = CREDENTIAL_KINDS.stream()
            .filter(var1x -> var7.contains(var1x) || "password".equals(var1x) && (var7.contains("pass") || var7.contains("pwd")))
            .findFirst()
            .orElse(null);
         if (var8 != null) {
            var var9 = var3.stream().filter(var1x -> var1x.toLowerCase(Locale.ROOT).contains(var8)).sorted().toList();
            var var10 = var2.stream().filter(var1x -> var1x.toLowerCase(Locale.ROOT).contains(var8)).toList();
            ArrayList var11 = new ArrayList();
            if (!var9.isEmpty()) {
               var11.add("stored as " + String.join(", ", var9));
            }

            if (!var10.isEmpty()) {
               var11.add("karate-config sets " + String.join(", ", var10));
            }

            if (!var11.isEmpty()) {
               var4.add(var6 + ": " + String.join("; ", var11));
            }
         }
      }

      return var4;
   }

   static byte[] marked(byte[] var0) {
      FeatureSource var1 = FeatureSource.of(var0);
      if (var1.feature().getTags() != null && var1.feature().getTags().stream().anyMatch(var0x -> "ai".equals(var0x.getName()))) {
         return var0;
      } else {
         EditResult var2 = FeatureEdits.tag(var1, null, List.of("@ai"), List.of(), var1.version());
         return var2.ok() ? var2.bytes() : var0;
      }
   }

   static byte[] weakenedMarked(byte[] var0, byte[] var1) {
      if (var0 == null) {
         return var1;
      } else {
         FeatureSource var2 = FeatureSource.of(var0);
         FeatureSource var3 = FeatureSource.of(var1);
         if (var2.feature() != null && var3.feature() != null) {
            int var4 = var3.feature().getSections().size();
            TreeSet var5 = new TreeSet();

            for (WeakeningDiff.Flag var7 : WeakeningDiff.diff(var2, var3)) {
               if (WEAKENS.contains(var7.kind())) {
                  if (var7.scenarioIndex() == null) {
                     for (int var8 = 0; var8 < var4; var8++) {
                        var5.add(var8);
                     }
                  } else if (var7.scenarioIndex() < var4) {
                     var5.add(var7.scenarioIndex());
                  }
               }
            }

            var var11 = var2.feature().getSections();

            for (int var12 = 0; var12 < Math.min(var11.size(), var4); var12++) {
               if (WeakeningDiff.marked((FeatureSection)var11.get(var12))) {
                  var5.add(var12);
               }
            }

            FeatureSource var13 = var3;

            for (int var9 : (Iterable<java.lang.Integer>) (Iterable<?>) (var5)) {
               if (!WeakeningDiff.marked(var13.feature().getSections().get(var9))) {
                  EditResult var10 = FeatureEdits.tag(var13, var9, List.of("@ai=weakened"), List.of(), var13.version());
                  if (!var10.ok()) {
                     break;
                  }

                  var13 = FeatureSource.of(var10.bytes());
               }
            }

            return var13 == var3 ? var1 : var13.bytes();
         } else {
            return var1;
         }
      }
   }

   private static byte[] saved(TestId var0, String var1) {
      if (var1 == null) {
         return null;
      } else {
         try {
            byte[] var2 = Files.isRegularFile(var0.file()) ? Files.readAllBytes(var0.file()) : null;
            return var2 != null && var1.equals(io.karatelabs.ext.common.Sha256.a(var2)) ? var2 : null;
         } catch (IOException var3) {
            return null;
         }
      }
   }

   Object run(String var1) {
      Object var2 = this.file("ApiTest.run", var1);
      if (var2 instanceof Map var24) {
         return var24;
      } else {
         TestId var3 = this.triage.id((Path)var2);
         boolean var4 = this.reopened(this.triage.triage(var3));
         Object var5 = this.start(var3);
         if (var5 instanceof io.karatelabs.ext.max.jsapi.ApiToolkit.HeldTestVersion var6) {
            if (var6.a == null) {
               return JsApi.missing("ApiTest.run: no test at " + var1 + " — ApiTest.createFeature first");
            } else {
               boolean var7 = !this.held.containsKey(rel(var3)) && var6.b != null && var6.b.equals(io.karatelabs.ext.common.Sha256.a(var6.a));
               boolean var8 = !var7 && (var4 || var6.b == null && !this.ask);
               int var9 = this.fixReds.getOrDefault(rel(var3), 0);
               if (var8 && var9 >= 3) {
                  var var25 = JsApi.missing(
                     "ApiTest.run: "
                        + var3.path()
                        + " did not pass "
                        + var9
                        + " times in this turn, so it is not run again now. Api.reply: tell the tester in plain words what blocks it — the call that failed, what it expected, what came back — and what the tester can do."
                  );
                  var25.put("code", "FIX_LIMIT");
                  var25.put("status", 409);
                  return var25;
               } else {
                  byte[] var10 = var7 ? var6.a : weakenedMarked(saved(var3, var6.b), marked(var6.a));
                  String var11 = UUID.randomUUID().toString().substring(0, 8);
                  ApiTest.HeldConfig var12 = this.heldConfig();
                  var var13 = this.triage.beginCandidate(var3, var10, var6.b, var11);
                  if (var13 != null) {
                     return var13;
                  } else {
                     CandidateRunner.Outcome var14 = this.execute(var3, var10, var11, var12);
                     boolean var15 = !"green".equals(var14.status());
                     boolean var16 = false;
                     if (!var7) {
                        byte[] var17 = var12 == null ? null : var12.bytes();
                        io.karatelabs.ext.max.jsapi.ApiToolkit.DraftRunRecord var18 = this.drafts.ran.put(rel(var3), new io.karatelabs.ext.max.jsapi.ApiToolkit.DraftRunRecord(var11, var6.a, var10, var17, var15));
                        var16 = var15 && var18 != null && var18.e() && Arrays.equals(var18.b(), var6.a) && Arrays.equals(var18.d(), var17);
                     }

                     boolean var26 = var8 && var15;
                     var var27 = this.triage.candidateDone(var3, var11, var14, !var26);
                     LinkedHashMap var19 = new LinkedHashMap();
                     var19.put("path", var3.path());
                     var19.put("runId", var11);
                     var19.put("report", "/manual/runs/" + var11);
                     if (var7) {
                        var19.put("unchanged", true);
                     }

                     if (var16) {
                        var19.put("repeat", true);
                     }

                     if (var26) {
                        this.fixReds.put(rel(var3), var9 + 1);
                        var19.put("status", "red");
                        var19.put("saved", false);
                        var19.put("fix", true);
                        var19.put("failing", var14.failing() != null ? var14.failing() : Map.of("actual", String.valueOf(var14.error())));
                        var var30 = this.unset(this.drafts.needs.get(rel(var3)));
                        if (!var30.isEmpty()) {
                           var19.put("unset", var30);
                        }

                        this.runCard(var3, var11, "red", var19.get("failing"), null, false, null, true);
                        if (var9 + 1 >= 3) {
                           String var31 = "Api.reply: what blocks it, in plain words — the call that failed, what it expected, what came back — and what the tester can do; it is not saved";
                           return var16
                              ? JsApi.next(
                                 var19,
                                 "The source and settings are unchanged since the previous failed run: read this run's response (ApiTest.exchanges) before running again, or ask the tester.",
                                 var31
                              )
                              : JsApi.next(var19, var31);
                        } else {
                           return var16
                              ? JsApi.next(
                                 var19,
                                 "The source and settings are unchanged since the previous failed run: read this run's response (ApiTest.exchanges) before running again, or ask the tester."
                              )
                              : JsApi.next(
                                 var19,
                                 "yours to fix, with no question to the tester: change the copy from the failing call (a variable karate-config does not set: ApiTest.config it; a credential: ask the tester to type it), then ApiTest.run('"
                                    + var3.path()
                                    + "')"
                              );
                        }
                     } else if (var15) {
                        this.release(var3);
                        if (var12 != null) {
                           var19.put("config", "not saved");
                        }

                        var19.put("status", "red");
                        var19.put("saved", false);
                        var19.put("failing", var14.failing() != null ? var14.failing() : Map.of("actual", String.valueOf(var14.error())));
                        var19.put("triage", var27.get("triage"));
                        this.runCard(var3, var11, "red", var19.get("failing"), null, var7, var12 == null ? null : false, false);
                        if ("bug".equals(var27.get("triage"))) {
                           return JsApi.next(var19, "Api.reply(text): the known suspected bug, still failing at its step; ask nothing");
                        } else {
                           String var29 = "Api.reply(the failing call, expected vs actual, then: bug or test?)";
                           return var16
                              ? JsApi.next(
                                 var19,
                                 "The source and settings are unchanged since the previous failed run: read this run's response (ApiTest.exchanges) before running again, or ask the tester.",
                                 var29
                              )
                              : JsApi.next(var19, var29);
                        }
                     } else {
                        var19.put("status", "green");
                        if (var12 != null) {
                           var var20 = this.api.landConfig(var12);
                           if (var20 != null) {
                              var19.put("saved", false);
                              var19.put("config", "not saved");
                              var19.putAll(var20);
                              this.runCard(var3, var11, "green", null, Map.of(), var7, false, false);
                              return JsApi.next(var19, "ApiTest.config()");
                           }
                        }

                        if (var7) {
                           this.release(var3);
                           this.drafts.ran.remove(rel(var3));
                           if (var12 != null) {
                              var19.put("config", "saved");
                           }

                           this.runCard(var3, var11, "green", null, null, true, var12 == null ? null : true, false);
                           return JsApi.next(var19, "Api.reply(text)");
                        } else {
                           var var28 = this.triage.land(var3, var10, var6.b, var11);
                           if (var28.get("error") != null && !Boolean.TRUE.equals(var28.get("landed"))) {
                              if (var12 != null) {
                                 this.api.restoreConfig(var12);
                              }

                              var19.putAll(var28);
                              this.runCard(var3, var11, "green", null, var28, false, var12 == null ? null : false, false);
                              return var19;
                           } else {
                              if (var6.b != null && this.drafts.recorded.containsKey(rel(var3))) {
                                 this.uncard(var3);
                              }

                              this.release(var3);
                              this.drafts.ran.remove(rel(var3));
                              var19.put("saved", true);
                              var19.put("version", var28.get("after"));
                              List var21 = var28.get("weakening") instanceof List var22 ? var22 : List.of();
                              if (!var21.isEmpty()) {
                                 var19.put("weakening", var21);
                              }

                              if (FeatureSource.of(var10).feature().getSections().stream().anyMatch(WeakeningDiff::marked)) {
                                 var19.put("weakened", "Coverage does not count it until a person removes @ai=weakened or Undo restores the check");
                              }

                              if (var12 != null) {
                                 var19.put("config", "saved");
                              }

                              this.runCard(var3, var11, "green", null, var28, false, var12 == null ? null : true, false);
                              var var32 = this.pending(var3, var10);
                              return !var32.isEmpty() && linked(var10).isEmpty() ? outcomesNext(var19, var3, var32) : linksNext(var19, var3, var10);
                           }
                        }
                     }
                  }
               }
            }
         } else {
            return var5;
         }
      }
   }

   private List<io.karatelabs.ext.max.jsapi.StepInsertionPlanner.PlannedStepInsertion> pending(TestId var1, byte[] var2) {
      StepInsertionPlanner var3 = this.drafts.outcomes.get(rel(var1));
      return var3 == null ? List.of() : var3.a(var2);
   }

   private static Map<String, Object> outcomesNext(Map<String, Object> var0, TestId var1, List<io.karatelabs.ext.max.jsapi.StepInsertionPlanner.PlannedStepInsertion> var2) {
      var0.put("proposed", io.karatelabs.ext.max.jsapi.StepInsertionPlanner.a(var2));
      var var3 = new ArrayList<>(io.karatelabs.ext.max.jsapi.StepInsertionPlanner.a(var1.path(), var2));
      var3.add("ApiTest.run('" + var1.path() + "')");
      var3.add("Api.proposeLinks('" + var1.path() + "', {scenario, reqs})");
      return JsApi.next(var0, var3.toArray(String[]::new));
   }

   private static Map<String, Object> linksNext(Map<String, Object> var0, TestId var1, byte[] var2) {
      var var3 = linked(var2);
      if (!var3.isEmpty()) {
         var0.put("linked", var3);
         return JsApi.next(var0, "Api.reply(text): it is already linked to " + String.join(", ", var3) + " — propose no links");
      } else {
         return JsApi.next(var0, "Api.proposeLinks('" + var1.path() + "', {scenario, reqs})", "Api.reply(text)");
      }
   }

   static List<String> linked(byte[] var0) {
      ArrayList var1 = new ArrayList();

      try {
         Feature var2 = FeatureSource.of(var0).feature();
         ArrayList var3 = new ArrayList(var2.getTags() == null ? List.of() : var2.getTags());

         for (FeatureSection var5 : var2.getSections()) {
            List var6 = var5.isOutline() ? var5.getScenarioOutline().getTags() : var5.getScenario().getTags();
            if (var6 != null) {
               var3.addAll(var6);
            }
         }

         for (Tag var9 : (Iterable<Tag>) (Iterable<?>) (var3)) {
            if ("req".equals(var9.getName()) && var9.getValues() != null) {
               var9.getValues().stream().map(String::strip).filter(var1x -> !var1x.isEmpty() && !var1.contains(var1x)).forEach(var1::add);
            }
         }

         return var1;
      } catch (RuntimeException var7) {
         return List.of();
      }
   }

   Object config(Object var1) {
      Map var2 = var1 instanceof Map var3 ? var3 : Map.of();
      var var8 = this.unresolved(var2);
      if (var8 == null) {
         var8 = this.sentByPostman(var2);
      }

      if (var8 != null) {
         return var8;
      } else {
         ApiTest.ConfigCandidate var4 = this.api.configCandidate(var1, this.heldConfig());
         if (var4.held() == null) {
            return var2.get("name") == null && var2.get("auth") == null && var2.get("remove") == null ? this.resolvedValues(var4.answer()) : var4.answer();
         } else {
            this.heldConfig = var4.held();
            this.own();
            LinkedHashMap var5 = new LinkedHashMap();
            var5.put("kind", "config");
            var5.put("name", var2.get("name") != null ? var2.get("name") : (var2.get("auth") != null ? "auth" : var2.get("remove")));
            if (var2.get("remove") != null && var2.get("name") == null) {
               var5.put("removed", true);
            } else if (var2.get("secret") != null) {
               var5.put("secret", true);
            } else if (var2.get("value") instanceof String var6 && var6.matches("(?i)https?://\\S+")) {
               var5.put("address", var6);
            }

            var5.put("saved", false);
            this.cards.accept(var5);
            Map var9 = (Map)var4.answer();
            return JsApi.next(var9, "ApiTest.run(path)");
         }
      }
   }

   private Object resolvedValues(Object var1) {
      if (var1 instanceof Map var2 && var2.get("set") instanceof List var3 && !var3.isEmpty()) {
         var var10 = this.runner.configValues();
         ArrayList var5 = new ArrayList();

         for (Object var7 : var3) {
            if (var7 instanceof Map var8 && var8.get("value") == null && var10.get(String.valueOf(var8.get("name"))) != null) {
               LinkedHashMap var9 = new LinkedHashMap(var8);
               var9.put("value", String.valueOf(var10.get(String.valueOf(var8.get("name")))));
               var5.add(var9);
            } else {
               var5.add(var7);
            }
         }

         LinkedHashMap var11 = new LinkedHashMap(var2);
         var11.put("set", var5);
         return var11;
      } else {
         return var1;
      }
   }

   private Map<String, Object> unresolved(Map<?, ?> var1) {
      ArrayList var2 = new ArrayList();
      if (var1.get("secret") != null && !var1.containsKey("value")) {
         var2.add(var1.get("secret"));
      }

      if (var1.get("auth") instanceof Map var3) {
         for (Object var5 : var3.values()) {
            if (var5 instanceof Map var6 && var6.get("secret") != null && !var6.containsKey("value")) {
               var2.add(var6.get("secret"));
            }
         }
      }

      for (Object var9 : var2) {
         if (this.secrets.apply(String.valueOf(var9)) == null) {
            var var10 = JsApi.missing(
               "ApiTest.config: no secret "
                  + var9
                  + " is stored, so nothing was changed. Ask the tester to type the credential here (\"the password is …\"): it is stored, and the tester's message reads secret('<NAME>') for you."
            );
            var10.put("code", "NO_SECRET");
            var10.put("status", 409);
            return var10;
         }
      }

      return null;
   }

   // $VF: One or more variable merging failures!
   // $VF: Could not properly define all variable types!
   // Please report this to the Vineflower issue tracker, at https://github.com/Vineflower/vineflower/issues with a copy of the class file (if you have the rights to distribute it!)
   private Map<String, Object> sentByPostman(Map<?, ?> var1) {
      if ((var1.get("name") != null ? var1.get("name") : var1.get("remove")) instanceof String var3
         && var3.matches("[A-Za-z_$][A-Za-z0-9_$]*")
         && !"auth".equals(var3)) {
         Object cfgAns = this.api.configCandidate(var1.get("file") != null ? Map.of("file", var1.get("file")) : null, this.heldConfig()).answer();

         for (Object var22 : cfgAns instanceof Map cfgMap && cfgMap.get("entries") instanceof List cfgList ? cfgList : List.of()) {
            if (var22 instanceof Map var25
               && var3.equals(var25.get("name"))
               && Objects.equals(var25.get("env"), var1.get("env"))
               && Objects.equals(var25.get("value"), var1.get("value"))
               && Objects.equals(var25.get("secret"), var1.get("secret"))) {
               return null;
            }
         }

         String var21;
         try {
            Path var23 = this.root.resolve(var1.get("file") == null ? "karate-config.js" : String.valueOf(var1.get("file"))).normalize();
            var21 = Files.isRegularFile(var23) ? Files.readString(var23) : "";
         } catch (IOException var17) {
            return null;
         }

         String var24 = Pattern.quote(var3);
         if (!Pattern.compile("(^|[^\\w$.])[\"']?" + var24 + "[\"']?\\s*:|\\." + var24 + "\\s*=[^=]").matcher(var21).find()) {
            return null;
         } else {
            String var26 = "{{" + var3 + "}}";

            try {
               try (var var9 = Files.walk(this.root)) {
                  for (Path var11 : (Iterable<Path>) (var9::iterator)) {
                     Path var12 = this.root.relativize(var11);
                     if ((var12.getNameCount() <= 1 || !UNWALKED.contains(var12.getName(0).toString()))
                        && var11.toString().endsWith(".json")
                        && Files.isRegularFile(var11)
                        && Files.size(var11) <= 4194304L) {
                        String var13 = Files.readString(var11);
                        if (var13.contains(var26) && (var11.toString().endsWith(".postman_collection.json") || var13.contains("getpostman.com"))) {
                           var var14 = JsApi.missing(
                              "ApiTest.config: the Postman collection "
                                 + var12.toString().replace('\\', '/')
                                 + " sends "
                                 + var26
                                 + ", which karate-config sets, so changing "
                                 + var3
                                 + " changes its requests; nothing was changed. Keep "
                                 + var3
                                 + " and add the rest of the address in the test (`* url "
                                 + var3
                                 + "` then `* path`), or set a variable of another name."
                           );
                           var14.put("code", "SHARED_VARIABLE");
                           var14.put("status", 409);
                           return var14;
                        }
                     }
                  }
               }

               return null;
            } catch (UncheckedIOException | IOException var19) {
               return null;
            }
         }
      } else {
         return null;
      }
   }

   private CandidateRunner.Outcome execute(TestId var1, byte[] var2, String var3, ApiTest.HeldConfig var4) {
      try {
         LinkedHashMap var5 = new LinkedHashMap();
         var5.put("runId", var3);
         return OverlayCandidateRunner.outcome(
            this.runner.runCandidate(var1.path(), var2, var4 == null ? null : var4.file(), var4 == null ? null : var4.bytes(), var5), var1, this.root
         );
      } catch (RuntimeException var6) {
         return CandidateRunner.Outcome.error(var6.getMessage());
      }
   }

   private void runCard(TestId var1, String var2, String var3, Object var4, Map<String, Object> var5, boolean var6, Boolean var7, boolean var8) {
      LinkedHashMap var9 = new LinkedHashMap();
      var9.put("kind", "run");
      var9.put("path", var1.path());
      var9.put("runId", var2);
      var9.put("status", var3);
      if (var6) {
         var9.put("unchanged", true);
      }

      if (var8) {
         var9.put("fix", true);
      }

      if (var7 != null) {
         var9.put("config", var7);
      }

      if (var4 != null) {
         var9.put("failing", var4);
      }

      if (var5 != null) {
         var9.put("saved", var5.get("after") != null);
         var9.put("created", var5.get("created"));
         var9.put("weakening", var5.get("weakening"));
      }

      this.cards.accept(var9);
   }

   Object markBug(String var1, Object var2) {
      Object var3 = this.file("ApiTest.markBug", var1);
      if (var3 instanceof Map var15) {
         return var15;
      } else {
         TestId var4 = this.triage.id((Path)var3);
         var var5 = this.triage.triage(var4);
         Map var6 = var5.get("answer") instanceof Map var7 ? var7 : null;
         if ("answered".equals(var5.get("state")) && var6 != null && "bug".equals(var6.get("choice"))) {
            Map var16 = var5.get("failing") instanceof Map var17 ? var17 : Map.of();
            String var18 = var2 instanceof Map var10 && var10.get("note") instanceof String var19 && !var19.isBlank()
               ? var19.strip()
               : String.valueOf(var16.getOrDefault("actual", "it failed")).lines().findFirst().orElse("it failed");
            byte[] var20 = annotate(
               this.triage.candidateBytes(var4), var16, var6.get("ticket") instanceof String var22 ? "@bug=" + var22 : "@bug=suspected", var18
            );
            if (var20 == null) {
               return JsApi.missing("ApiTest.markBug: the failing step of " + var4.path() + " could not be found");
            } else {
               var20 = marked(var20);
               String var23 = UUID.randomUUID().toString().substring(0, 8);
               var var25 = this.triage.beginVerification(var4, var20, var23);
               if (var25 != null) {
                  return var25;
               } else {
                  var var12 = this.triage.verificationDone(var4, var23, this.execute(var4, var20, var23, null));
                  LinkedHashMap var13 = new LinkedHashMap();
                  var13.put("kind", "bug");
                  var13.put("path", var4.path());
                  var13.put("runId", var23);
                  var13.put("saved", var12.get("after") != null);
                  if (var12.get("verify") != null) {
                     var13.put("verify", var12.get("verify"));
                  }

                  this.cards.accept(var13);
                  var var14 = new LinkedHashMap<>(var12);
                  var14.put("report", "/manual/runs/" + var23);
                  if (var12.get("after") != null) {
                     this.drafts.ran.remove(rel(var4));
                     var14.put("saved", true);
                     var14.put("version", var12.get("after"));
                     return JsApi.next(var14, "Api.reply(text)");
                  } else {
                     return var14;
                  }
               }
            }
         } else {
            return JsApi.missing("ApiTest.markBug: the tester has not answered bug for " + var4.path());
         }
      }
   }

   static byte[] annotate(byte[] var0, Map<String, Object> var1, String var2, String var3) {
      if (var0 != null && var1.get("step") instanceof Number var4) {
         FeatureSource var10 = FeatureSource.of(var0);
         Integer var6 = scenarioOf(var10, var4.intValue(), var1);
         if (var6 == null) {
            return null;
         } else {
            EditResult var7 = FeatureEdits.tag(var10, var6, List.of(var2), List.of(), var10.version());
            if (!var7.ok()) {
               return null;
            } else {
               FeatureSource var8 = FeatureSource.of(var7.bytes());
               EditResult var9 = FeatureEdits.insertComment(
                  var8, new FeatureEdits.StepRef(var6, var4.intValue()), "# suspected bug: " + var3.replaceAll("\\s+", " "), var8.version()
               );
               return var9.ok() ? var9.bytes() : null;
            }
         }
      } else {
         return null;
      }
   }

   private static Integer scenarioOf(FeatureSource var0, int var1, Map<String, Object> var2) {
      var var3 = var0.feature().getSections();
      if (var2.get("section") instanceof Number var4 && var4.intValue() >= 0 && var4.intValue() < var3.size()) {
         return var4.intValue();
      } else if (var3.size() == 1) {
         return 0;
      } else {
         Integer var14 = var2.get("line") instanceof Number var15 ? var15.intValue() : null;
         String var16 = var2.get("expected") instanceof String var17 ? var17.strip() : null;
         Integer var18 = null;

         for (int var19 = 0; var19 < var3.size(); var19++) {
            FeatureSection var8 = (FeatureSection)var3.get(var19);
            int var9 = var8.isOutline() ? var8.getScenarioOutline().getLine() : var8.getScenario().getLine();
            int var10 = var19 + 1 < var3.size()
               ? (
                  ((FeatureSection)var3.get(var19 + 1)).isOutline()
                     ? ((FeatureSection)var3.get(var19 + 1)).getScenarioOutline().getLine()
                     : ((FeatureSection)var3.get(var19 + 1)).getScenario().getLine()
               )
               : Integer.MAX_VALUE;
            if (var14 != null && var14 > var9 && var14 < var10) {
               return var19;
            }

            List var11 = var8.isOutline() ? var8.getScenarioOutline().getSteps() : var8.getScenario().getSteps();
            if (var16 != null && var11 != null && var1 < var11.size()) {
               Step var12 = (Step)var11.get(var1);
               String var13 = (var12.getKeyword() == null ? "" : var12.getKeyword().trim() + " ") + var12.getText();
               if (var16.equals(var13.strip()) && var18 == null) {
                  var18 = var19;
               }
            }
         }

         return var18;
      }
   }

   // $VF: One or more variable merging failures!
   // $VF: Could not properly define all variable types!
   // Please report this to the Vineflower issue tracker, at https://github.com/Vineflower/vineflower/issues with a copy of the class file (if you have the rights to distribute it!)
   Object proposeLinks(String var1, Object var2) {
      Object var3 = this.file("Api.proposeLinks", var1);
      if (var3 instanceof Map var19) {
         return var19;
      } else {
         TestId var4 = this.triage.id((Path)var3);
         if (!Files.isRegularFile(var4.file())) {
            return JsApi.notFound("Api.proposeLinks: " + var4.path() + " is not saved yet — ApiTest.run it green first");
         } else {
            List<io.karatelabs.ext.max.jsapi.StepInsertionPlanner.PlannedStepInsertion> var5;
            try {
               var5 = this.pending(var4, Files.readAllBytes(var4.file()));
            } catch (IOException var18) {
               return JsApi.notFound("Api.proposeLinks: could not read " + var4.path() + ": " + var18.getMessage());
            }

            Map var6 = var2 instanceof Map var7 ? var7 : Map.of();
            ArrayList<String> var21 = new ArrayList<>();

            Object skipVal = var6.get("skip");

            for (Object var23 : skipVal instanceof List ? (List)skipVal : skipVal instanceof String skipStr ? List.of(skipStr) : List.of()) {
               String var27 = String.valueOf(var23).strip();
               if (var5.stream().noneMatch(var1x -> var1x.d().strip().equals(var27))) {
                  Map var32 = JsApi.missing(
                     "Api.proposeLinks: skip names no pending proposal: " + var27 + " — pending: " + var5.stream().map(io.karatelabs.ext.max.jsapi.StepInsertionPlanner.PlannedStepInsertion::d).distinct().toList()
                  );
                  var32.put("code", "BAD_REQUEST");
                  var32.put("status", 400);
                  return var32;
               }

               if (!var21.contains(var27)) {
                  var21.add(var27);
               }
            }

            ArrayList var22 = new ArrayList();

            for (String var28 : (Iterable<String>) (Iterable<?>) (var21)) {
               LinkedHashMap var33 = new LinkedHashMap();
               var33.put("text", var28);
               var33.put(
                  "after",
                  var5.stream()
                     .filter(var1x -> var1x.d().strip().equals(var28) && var1x.f() != null)
                     .sorted(Comparator.comparingInt(io.karatelabs.ext.max.jsapi.StepInsertionPlanner.PlannedStepInsertion::b).thenComparingInt(io.karatelabs.ext.max.jsapi.StepInsertionPlanner.PlannedStepInsertion::c))
                     .map(io.karatelabs.ext.max.jsapi.StepInsertionPlanner.PlannedStepInsertion::f)
                     .distinct()
                     .toList()
               );
               var22.add(var33);
            }

            var5 = var5.stream().filter(var1x -> !var21.contains(var1x.d().strip())).toList();
            if (!var5.isEmpty()) {
               var var26 = JsApi.missing(
                  "Api.proposeLinks: links come after the outcomes are asserted — "
                     + var5.size()
                     + " proposed assertion(s) are not in "
                     + var4.path()
                     + "; insert them, or pass {skip: [text]} for one the criteria do not name"
               );
               var26.put("code", "OUTCOMES_UNASSERTED");
               var26.put("status", 409);
               return outcomesNext(var26, var4, var5);
            } else {
               int var25 = var6.get("scenario") instanceof Number var29 ? var29.intValue() : 0;
               List var30 = var6.get("reqs") instanceof List var12 ? var12 : (var6.get("reqs") instanceof String var35 ? List.of(var35) : List.of());
               if (var30.isEmpty()) {
                  return JsApi.missing("Api.proposeLinks(path, {scenario, reqs: ['ID/1']}) needs the criteria to propose");
               } else {
                  var var36 = this.criteria();
                  ArrayList var37 = new ArrayList();
                  ArrayList var39 = new ArrayList();

                  for (Object var15 : var30) {
                     String var16 = String.valueOf(var15).replaceFirst("^@req=", "").strip();
                     Map var17 = (Map)var36.get(var16);
                     if (var17 == null) {
                        var39.add(var16);
                     } else {
                        var37.add(var17);
                     }
                  }

                  if (!var39.isEmpty()) {
                     return JsApi.next(JsApi.notFound("Api.proposeLinks: no requirement criterion " + var39), "Requirement.list()");
                  } else {
                     LinkedHashMap var40 = new LinkedHashMap();
                     var40.put("kind", "links");
                     var40.put("path", var4.path());
                     var40.put("scenario", var25);
                     var40.put("items", var37);
                     if (!var21.isEmpty()) {
                        var40.put("skipped", var22);
                     }

                     this.cards.accept(var40);
                     LinkedHashMap var41 = new LinkedHashMap();
                     var41.put("proposed", var37.size());
                     return JsApi.next(var41, "Api.reply(ask the tester to tick the criteria the test proves)");
                  }
               }
            }
         }
      }
   }

   // $VF: One or more variable merging failures!
   // $VF: Could not properly define all variable types!
   // Please report this to the Vineflower issue tracker, at https://github.com/Vineflower/vineflower/issues with a copy of the class file (if you have the rights to distribute it!)
   private Map<String, Map<String, Object>> criteria() {
      LinkedHashMap var1 = new LinkedHashMap();

      Object listVal = this.requirement.jsGet("list");
      Object listResult = listVal instanceof JavaInvokable ji ? ji.invoke() : null;

      for (Object var12 : listResult instanceof List ? (List)listResult : List.of()) {
         if (var12 instanceof Map var5 && var5.get("id") != null) {
            String var6 = String.valueOf(var5.get("id"));
            var1.put(var6, item(var6, String.valueOf(var5.get("title")), null));

            for (Object var14 : var5.get("criteria") instanceof List ? (List)var5.get("criteria") : List.of()) {
               if (var14 instanceof Map var15 && var15.get("id") != null) {
                  String var10 = var6 + "/" + var15.get("id");
                  var1.put(var10, item(var10, var5.get("title") + " · criterion " + var15.get("id"), var15.get("text")));
               }
            }
         }
      }

      return var1;
   }

   private static Map<String, Object> item(String var0, String var1, Object var2) {
      LinkedHashMap var3 = new LinkedHashMap();
      var3.put("tag", "@req=" + var0);
      var3.put("label", var1);
      if (var2 != null) {
         var3.put("text", String.valueOf(var2));
      }

      return var3;
   }

   Object draft(Object var1, Object var2) {
      io.karatelabs.ext.agent.llm.LlmClient var3 = this.ctx == null ? null : this.ctx.llmClient();
      if (var3 == null) {
         return JsApi.missing("Api.draft needs the run's model");
      } else {
         var var4 = this.draft.apply(var1 instanceof String var5 ? var5 : null, var3);
         if (!Boolean.TRUE.equals(var4.get("ok"))) {
            return var4;
         } else {
            Map var9 = var2 instanceof Map var6 ? var6 : Map.of();
            String var10 = var9.get("file") instanceof String var7 && var7.matches("[A-Za-z0-9._-]{1,80}") ? var7 : "requirements.md";
            LinkedHashMap var11 = new LinkedHashMap();
            var11.put("kind", "draft");
            var11.put("file", var10.endsWith(".md") ? var10 : var10 + ".md");
            var11.put("markdown", var4.get("markdown"));
            var11.put("ids", var4.get("ids"));
            this.cards.accept(var11);
            LinkedHashMap var12 = new LinkedHashMap();
            var12.put("ok", true);
            var12.put("ids", var4.get("ids"));
            var12.put("file", var11.get("file"));
            return JsApi.next(var12, "Api.reply(ask the tester to check and save the criteria)");
         }
      }
   }

   private final class CandidateApi extends JsDispatch {
      CandidateApi() {
         this.fn("read", var1x -> ApiToolkit.this.guarded("ApiTest.read", true, () -> ApiToolkit.this.read(JsApi.str(var1x, 0), JsApi.arg(var1x, 1))));
         this.fn(
            "exchanges", var1x -> ApiToolkit.this.guarded("ApiTest.exchanges", true, () -> ApiToolkit.this.exchanges(JsApi.str(var1x, 0), JsApi.arg(var1x, 1)))
         );
         this.fn(
            "diff",
            var1x -> ApiToolkit.this.guarded(
               "ApiTest.diff",
               true,
               () -> ApiToolkit.unseen(ApiToolkit.this.api.diff(ApiToolkit.this.side(JsApi.arg(var1x, 0)), ApiToolkit.this.side(JsApi.arg(var1x, 1))))
            )
         );

         for (String var3 : ApiToolkit.EDIT_VERBS) {
            this.fn(var3, var2 -> ApiToolkit.this.guarded("ApiTest." + var3, false, () -> ApiToolkit.this.edit(var3, JsApi.str(var2, 0), JsApi.arg(var2, 1))));
         }

         this.fn("config", var1x -> ApiToolkit.this.guarded("ApiTest.config", false, () -> ApiToolkit.this.config(JsApi.arg(var1x, 0))));
         this.fn("run", var1x -> ApiToolkit.this.guarded("ApiTest.run", false, () -> ApiToolkit.this.run(JsApi.str(var1x, 0))));
         this.fn("markBug", var1x -> ApiToolkit.this.guarded("ApiTest.markBug", false, () -> ApiToolkit.this.markBug(JsApi.str(var1x, 0), JsApi.arg(var1x, 1))));
      }
   }

   public static final class Drafts {
      final Map<String, byte[]> bytes = new ConcurrentHashMap<>();
      final Map<String, String> base = new ConcurrentHashMap<>();
      final Map<String, List<String>> needs = new ConcurrentHashMap<>();
      final Map<String, StepInsertionPlanner> outcomes = new ConcurrentHashMap<>();
      final Map<String, io.karatelabs.ext.max.jsapi.ApiToolkit.TrafficDraft> recorded = new ConcurrentHashMap<>();
      final Map<String, io.karatelabs.ext.max.jsapi.ApiToolkit.DraftRunRecord> ran = new ConcurrentHashMap<>();
      private ApiTest.HeldConfig config;
      private String configOf;

      public boolean holds(String var1) {
         return var1 != null && this.bytes.containsKey(var1);
      }

      void drop(String var1) {
         this.bytes.remove(var1);
         this.base.remove(var1);
         this.needs.remove(var1);
         this.recorded.remove(var1);
         synchronized (this) {
            if (var1.equals(this.configOf)) {
               this.config = null;
               this.configOf = null;
            }
         }
      }

      synchronized ApiTest.HeldConfig config() {
         return this.config != null && this.bytes.size() == 1 && this.bytes.containsKey(this.configOf) ? this.config : null;
      }

      synchronized String owner() {
         return this.configOf;
      }

      synchronized void own(ApiTest.HeldConfig var1) {
         boolean var2 = var1 != null && this.bytes.size() == 1;
         this.configOf = var2 ? this.bytes.keySet().iterator().next() : null;
         this.config = var2 ? var1 : null;
      }

      public String moving(String var1) {
         io.karatelabs.ext.max.jsapi.ApiToolkit.TrafficDraft var2 = this.holds(var1) ? this.recorded.get(var1) : null;
         if (var2 == null) {
            return null;
         } else {
            StringBuilder var3 = new StringBuilder("Moving a call: Traffic.migrate(").append(ApiToolkit.js(var2.a())).append(", {");
            var2.b().forEach((var1x, var2x) -> var3.append(var1x).append(": ").append(ApiToolkit.js(var2x)).append(", "));
            var3.append("moves: [");
            var2.c().forEach(var1x -> var3.append(ApiToolkit.js(var1x)).append(", "));
            return var3.append("{id, to: 'kept'|'left', by: 'tester'}]}) — never hand-edit steps out.").toString();
         }
      }
   }

   static final class Verbs implements SimpleObject {
      private final Map<String, Object> members;

      Verbs(Map<String, Object> var1) {
         this.members = new LinkedHashMap<>(var1);
      }

      static ApiToolkit.Verbs of(String var0, SimpleObject var1, List<String> var2, boolean var3, ApiReplyToolkit var4) {
         LinkedHashMap var5 = new LinkedHashMap();

         for (String var7 : var2) {
            if (var1.jsGet(var7) instanceof JavaInvokable var8) {
               var5.put(var7, (JavaInvokable)var5x -> var4.called(var0 + "." + var7, var3, () -> var8.invoke(var5x)));
            }
         }

         return new ApiToolkit.Verbs(var5);
      }

      @Override
      public Object jsGet(String var1) {
         Object var2 = this.members.get(var1);
         return var2 == null ? Terms.UNDEFINED : var2;
      }

      @Override
      public Collection<String> jsKeys() {
         return this.members.keySet();
      }
   }

   private record HeldTestVersion(byte[] a, String b) {

   }

   record DraftRunRecord(String a, byte[] b, byte[] c, byte[] d, boolean e) {

   }

   record TrafficDraft(String a, Map<String, Object> b, List<Map<String, Object>> c) {

   }
}
