package io.karatelabs.ext.max.jsapi;

import io.karatelabs.js.JavaInvokable;
import io.karatelabs.common.Json;
import io.karatelabs.common.StringUtils;
import io.karatelabs.ext.agent.runtime.ConversationEntry;
import io.karatelabs.ext.common.MaxPaths;
import io.karatelabs.ext.common.Sha256;
import io.karatelabs.ext.common.jsapi.JsApi;
import io.karatelabs.ext.max.apitest.CandidateRunner;
import io.karatelabs.ext.max.apitest.TestId;
import io.karatelabs.ext.max.apitest.TriageGate;
import io.karatelabs.ext.max.run.HarnessSseBridge;
import io.karatelabs.js.SimpleObject;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ApiChat {
   private static final Logger logger = LoggerFactory.getLogger(ApiChat.class);
   static final int MAX_ITERATIONS = 16;
   static final int SUMMARY_CHARS = 2000;
   static final String RESTARTED = "The chat restarted; your test is as last saved.";
   static final String STOPPED = "The assistant stopped without an answer. Ask again, or say it another way.";
   static final String PREAMBLE = "You help a manual tester build and run API tests in the Karate console.\nRespond with exactly one ```js block, then stop: it runs, and you see its result before your next turn. Variables persist across turns.";
   private final Harness harness;
   private final Path root;
   private final ApiTest api;
   private final TriageGate triage;
   private final Runner runner;
   private final SimpleObject requirement;
   private final Map<String, io.karatelabs.ext.max.jsapi.ApiChat.ChatSession> chats = new ConcurrentHashMap<>();
   private final Map<String, String> byTest = new ConcurrentHashMap<>();
   private final ReentrantLock lock = new ReentrantLock();
   private final Map<String, String> typed = new ConcurrentHashMap<>();
   private Postman postman;
   private Traffic traffic;
   private Supplier<io.karatelabs.ext.common.secrets.SecretStore> secrets = () -> null;
   private static final Pattern SAYS_TEST = Pattern.compile(
      "\\b(not an? (api )?bug|(the )?test('s| is| was) (wrong|outdated|out of date)|(fix|change|update) the test)\\b"
   );
   private static final Pattern SAYS_BUG = Pattern.compile(
      "^(an? )?bug\\b|\\b(it'?s|it is|that'?s|that is|this is|looks like|is) (an? )?(real |genuine |api )?bug\\b|\\bbug in the (api|server|service)\\b"
   );
   private static final Pattern TICKET = Pattern.compile("\\b[A-Z][A-Z0-9]+-\\d+\\b");
   private static final Pattern KEEP_TEST = Pattern.compile("\\b(don'?t|do not|no need to|never|not) (fix|change|update|touch) the test\\b");
   private static final Pattern SENTENCE = Pattern.compile("(?<=[.!?;])\\s+|\\n+");

   public ApiChat(Harness var1, Path var2, ApiTest var3, Runner var4, SimpleObject var5) {
      this.harness = var1;
      this.root = var2.toAbsolutePath().normalize();
      this.api = var3;
      this.triage = var1.triage();
      this.runner = var4;
      this.requirement = var5;
   }

   public ApiChat withPostman(Postman var1) {
      this.postman = var1;
      return this;
   }

   public ApiChat withTraffic(Traffic var1) {
      this.traffic = var1;
      return this;
   }

   public ApiChat withSecrets(Supplier<io.karatelabs.ext.common.secrets.SecretStore> var1) {
      this.secrets = var1;
      return this;
   }

   public Map<String, Object> send(Object var1, Object var2) {
      if (var1 instanceof String var3 && !var3.isBlank()) {
         Map var4 = var2 instanceof Map var5 ? var5 : Map.of();
         String var20 = this.testOf(var4.get("test"));
         if (var4.get("test") != null && var20 == null) {
            return JsApi.missing("Harness.api: test is a project-relative .feature path, got " + var4.get("test"));
         } else {
            io.karatelabs.ext.max.jsapi.ApiChat.ChatSession var6 = var4.get("chat") instanceof String var7 ? this.chats.get(var7) : null;
            if (var20 == null && var4.get("test") == null && var6 != null && (this.unsaved(var6.b) || var6.f.holds(var6.b))) {
               var20 = var6.b;
            }

            if (!this.harness.modelConfigured()) {
               return refuse("NO_MODEL", "No AI model is connected: connect one in Settings, then ask again.");
            } else {
               io.karatelabs.ext.common.secrets.SecretStore var21 = this.secrets.get();

               io.karatelabs.ext.common.secrets.SecretExtractor.SecretExtractionResult var22;
               try {
                  var22 = var21 == null
                     ? new io.karatelabs.ext.common.secrets.SecretExtractor.SecretExtractionResult(var3.strip(), var3.strip())
                     : io.karatelabs.ext.common.secrets.SecretExtractor.a(var3.strip(), var21.a(this.root));
               } catch (UncheckedIOException | IllegalStateException var19) {
                  return refuse("SECRET_STORE", "The credential you typed could not be kept in the secret store, so nothing was sent: " + var19.getMessage());
               }

               String var9 = null;
               if (var4.get("from") != null) {
                  var9 = recording(var4.get("from"));
                  if (var9 == null) {
                     return JsApi.missing("Harness.api: from is {source: '<recording run id>', moves?, hosts?, reqs?}");
                  }
               }

               Map var10 = var20 != null && var9 == null ? this.typedAnswer(var4.get("chat") instanceof String var11 ? var11 : null, var20, var3) : null;
               if (var10 != null) {
                  return var10;
               } else {
                  String var23 = newRunId();
                  String var24 = this.harness.claimApiRun(var23, "api: " + clip(var22.b(), 80));
                  if (var24 != null) {
                     var var25 = refuse("BUSY", "The assistant is still answering — send this again when it is done.");
                     var25.put("reason", var24);
                     return var25;
                  } else {
                     boolean var13 = var4.get("chat") instanceof String var14 && !var14.isBlank() && !this.chats.containsKey(var14);
                     io.karatelabs.ext.max.jsapi.ApiChat.ChatSession var26 = this.chatFor(var4.get("chat") instanceof String var27 ? var27 : null, var20);
                     String var28 = this.rebind(var26, var20);
                     String var29 = var9 == null ? var22.a() : var22.a() + "\n" + var9;
                     io.karatelabs.ext.max.jsapi.ApiChat.ChatTurn var17 = this.admit(var26, var23, "message", var22.b(), var29);
                     this.start(var26, var17, this.task(var26, var28, var29, var4.get("sent")), null);
                     var var18 = this.started(var26, var17);
                     if (var13) {
                        var18.put("restarted", "The chat restarted; your test is as last saved.");
                     }

                     return var18;
                  }
               }
            }
         }
      } else {
         return JsApi.missing("Harness.api(message, {test, chat?}) needs the tester's message");
      }
   }

   private static String recording(Object var0) {
      if (var0 instanceof Map var1 && var1.get("source") instanceof String var2 && !var2.isBlank()) {
         LinkedHashMap var8 = new LinkedHashMap();

         for (String var5 : List.of("hosts", "moves")) {
            if (var1.get(var5) instanceof List var6 && !var6.isEmpty()) {
               var8.put(var5, var6);
            } else if (var1.get(var5) != null && !(var1.get(var5) instanceof List)) {
               return null;
            }
         }

         String var9 = "The calls the tester confirmed in the recording: Traffic.migrate("
            + ApiToolkit.js(var2)
            + (var8.isEmpty() ? "" : ", " + ApiToolkit.js(var8))
            + ")";
         if (var1.get("reqs") instanceof List var10 && !var10.isEmpty()) {
            var9 = var9
               + "\nThe criteria the tester chose for it, linked only through Api.proposeLinks(path, {scenario, reqs: "
               + ApiToolkit.js(var10)
               + "}) once each outcome they name is asserted";
         } else if (var1.get("reqs") != null && !(var1.get("reqs") instanceof List)) {
            return null;
         }

         return var9;
      } else {
         return null;
      }
   }

   // $VF: One or more variable merging failures!
   // $VF: Could not properly define all variable types!
   // Please report this to the Vineflower issue tracker, at https://github.com/Vineflower/vineflower/issues with a copy of the class file (if you have the rights to distribute it!)
   static Map<String, Object> result(List<Map<String, Object>> var0) {
      String var1 = "unsaved";
      String var2 = null;
      String var3 = null;

      for (Map var5 : var0) {
         boolean var6 = false;
         var3 = null;
         var1 = "failed".equals(var1) ? "unsaved" : var1;

         for (Object var15 : var5.get("cards") instanceof List ? (List)var5.get("cards") : List.of()) {
            if (var15 instanceof Map var16) {
               boolean var10 = "run".equals(var16.get("kind"));
               if (var10 && Boolean.TRUE.equals(var16.get("saved")) && var16.get("path") instanceof String var11) {
                  var2 = var11;
                  var1 = "saved";
                  var6 = true;
               } else if ("bug".equals(var16.get("kind")) && Boolean.TRUE.equals(var16.get("saved"))) {
                  var1 = "bug";
                  var6 = true;
               } else if (var10
                  && "red".equals(var16.get("status"))
                  && !Boolean.TRUE.equals(var16.get("fix"))
                  && var16.get("path") instanceof String var12
                  && !"saved".equals(var1)) {
                  var2 = var12;
                  var1 = "red";
               }
            }
         }

         if ("running".equals(var5.get("status"))) {
            var1 = !"saved".equals(var1) && !"bug".equals(var1) ? "writing" : var1;
         } else if (!var6 && var5.get("reply") == null && var5.get("error") != null && !"saved".equals(var1) && !"bug".equals(var1)) {
            var1 = "failed";
            var3 = String.valueOf(var5.get("error"));
         }
      }

      LinkedHashMap var14 = new LinkedHashMap();
      var14.put("status", var1);
      if (var2 != null) {
         var14.put("test", var2);
      }

      if (var3 != null) {
         var14.put("error", var3);
      }

      return var14;
   }

   public Map<String, Object> read(Object var1) {
      Map var2 = var1 instanceof Map var4 ? var4 : (var1 instanceof String var3 ? Map.of("chat", var3) : Map.of());
      io.karatelabs.ext.max.jsapi.ApiChat.ChatSession var13 = var2.get("chat") instanceof String var14 ? this.chats.get(var14) : null;
      if (var13 == null) {
         String var17 = this.testOf(var2.get("test"));
         if (var17 instanceof String && this.byTest.get(var17) != null) {
            var13 = this.chats.get(this.byTest.get(var17));
         }
      }

      if (var13 == null) {
         var var16 = JsApi.notFound("no chat here" + (var2.get("chat") != null ? ": The chat restarted; your test is as last saved." : ""));
         var16.put("model", this.harness.modelConfigured());
         return var16;
      } else {
         this.lock.lock();

         LinkedHashMap var19;
         try {
            LinkedHashMap var15 = new LinkedHashMap();
            var15.put("chat", var13.a);
            var15.put("test", var13.b);
            var15.put("model", this.harness.modelConfigured());
            var15.put("events", var13.h);
            ArrayList var18 = new ArrayList();
            boolean var6 = false;

            for (io.karatelabs.ext.max.jsapi.ApiChat.ChatTurn var8 : var13.g) {
               LinkedHashMap var9 = new LinkedHashMap();
               var9.put("turn", var8.a);
               var9.put("runId", var8.b);
               var9.put("kind", var8.c);
               var9.put("message", var8.d);
               var9.put("status", var8.f);
               if (var8.g != null) {
                  var9.put("reply", this.harness.sanitizer().apply(var8.g));
               }

               if (var8.h != null) {
                  var9.put("error", this.harness.sanitizer().apply(var8.h));
               }

               var9.put("cards", new ArrayList<>(var8.i));
               var18.add(var9);
               var6 |= "running".equals(var8.f);
            }

            var15.put("turns", var18);
            var15.put("busy", var6);
            var15.put("result", result(var18));
            var19 = var15;
         } finally {
            this.lock.unlock();
         }

         return var19;
      }
   }

   static String triageChoice(String var0) {
      String var1 = null;

      for (String var5 : SENTENCE.split(var0.toLowerCase(Locale.ROOT).replace('’', '\'').strip())) {
         String var6 = KEEP_TEST.matcher(var5.strip()).replaceAll("");
         boolean var7 = SAYS_TEST.matcher(var6).find();
         String var8 = var7 ? "test" : (SAYS_BUG.matcher(var6).find() ? "bug" : null);
         if (var8 != null) {
            if (var6.endsWith("?") || var1 != null && !var1.equals(var8)) {
               return null;
            }

            var1 = var8;
         }
      }

      return var1;
   }

   private Map<String, Object> typedAnswer(String var1, String var2, String var3) {
      String var4 = triageChoice(var3);
      if (var4 == null) {
         return null;
      } else {
         TestId var5 = this.triage.id(this.root.resolve(var2));
         var var6 = this.triage.triage(var5);
         if (var6.get("answers") instanceof List var7 && var7.contains(var4)) {
            io.karatelabs.ext.max.jsapi.ApiChat.ChatSession var22 = this.chatFor(var1, var2);
            this.rebind(var22, var2);
            LinkedHashMap var9 = new LinkedHashMap();
            var9.put("answer", var4);
            var9.put("candidate", var6.get("candidate"));
            var9.put("base", var6.get("base"));
            Matcher var10 = TICKET.matcher(var3);
            if ("bug".equals(var4) && var10.find()) {
               var9.put("ticket", var10.group());
            }

            this.typed.put(var2, var3);

            Map var11;
            try {
               var11 = this.triage.answer(var5, var9);
            } finally {
               this.typed.remove(var2);
            }

            if ("BUSY".equals(var11.get("code"))) {
               return refuse("BUSY", "The assistant is still answering — send this again when it is done.");
            } else if (var11.get("error") != null) {
               return null;
            } else {
               this.lock.lock();

               label117: {
                  Map var14;
                  try {
                     var var12 = var22.g.iterator();

                     io.karatelabs.ext.max.jsapi.ApiChat.ChatTurn var13;
                     do {
                        if (!var12.hasNext()) {
                           break label117;
                        }

                        var13 = (io.karatelabs.ext.max.jsapi.ApiChat.ChatTurn)var12.next();
                     } while (!var13.b.equals(var11.get("runId")));

                     var14 = this.started(var22, var13);
                  } finally {
                     this.lock.unlock();
                  }

                  return var14;
               }

               LinkedHashMap var23 = new LinkedHashMap();
               var23.put("chat", var22.a);
               var23.put("status", "started");
               return var23;
            }
         } else {
            return null;
         }
      }
   }

   public void reportTurn(TestId var1, String var2) {
      if (this.harness.modelConfigured()) {
         String var3 = newRunId();
         if (this.harness.claimApiRun(var3, "api: report " + var1.path()) == null) {
            io.karatelabs.ext.max.jsapi.ApiChat.ChatSession var4 = this.chatFor(null, var1.path());
            this.rebind(var4, var1.path());
            io.karatelabs.ext.max.jsapi.ApiChat.ChatTurn var5 = this.admit(var4, var3, "report", "Run the test");
            var var6 = this.triage.triage(var1);
            String var7 = describe(var6.get("failing"));
            this.start(
               var4,
               var5,
               "The tester ran "
                  + var1.path()
                  + " and it failed (its report: /manual/runs/"
                  + var2
                  + "). "
                  + var7
                  + "\nChange nothing. Api.reply: what failed, in plain words, what was expected and what came back, then ask: bug or test?",
               null
            );
         }
      }
   }

   public CandidateRunner.Claim ids(TestId var1, final CandidateRunner.Claim var2) {
      final io.karatelabs.ext.max.jsapi.ApiChat.ChatSession var3 = this.chatFor(null, var1.path());
      this.lock.lock();

      final int var4;
      try {
         var4 = var3.g.size() + 1;
      } finally {
         this.lock.unlock();
      }

      return new CandidateRunner.Claim() {
         @Override
         public String runId() {
            return var2.runId();
         }

         @Override
         public String chat() {
            return var3.a;
         }

         @Override
         public Integer turn() {
            return var4;
         }

         @Override
         public void release() {
            var2.release();
         }

         @Override
         public void start(CandidateRunner.Request var1) {
            var2.start(var1);
         }
      };
   }

   public void answerTurn(CandidateRunner.Claim var1, CandidateRunner.Request var2) {
      io.karatelabs.ext.max.jsapi.ApiChat.ChatSession var3 = this.chatFor(null, var2.test().path());
      this.rebind(var3, var2.test().path());
      Map var4 = var2.answer() == null ? Map.of() : var2.answer();
      String var5 = String.valueOf(var4.get("choice"));
      String var6 = var4.get("ticket") instanceof String var7 ? var7 : null;
      String var14 = var2.test().path();
      String var15 = this.typed.get(var14);
      io.karatelabs.ext.max.jsapi.ApiChat.ChatTurn var9 = this.admit(
         var3,
         var1.runId(),
         "answer",
         var15 != null ? var15 : ("bug".equals(var5) ? "It's a bug" + (var6 == null ? "" : " (" + var6 + ")") : "The test is wrong")
      );
      var var10 = this.triage.triage(var2.test());
      String var11 = var10.get("runId") instanceof String var12 ? " The red run's report: /manual/runs/" + var12 + "." : "";
      String var16 = (
            "bug".equals(var5)
               ? "The tester answered bug"
                  + (var6 == null ? "" : ", ticket " + var6)
                  + " for "
                  + var14
                  + "."
                  + var11
                  + " Call ApiTest.markBug('"
                  + var14
                  + "', {note}) with a one-line note, then Api.reply: the answer is recorded, and whether the test was saved as a suspected bug."
               : "The tester answered test for "
                  + var14
                  + ": the test is wrong, not the API."
                  + var11
                  + " Fix the copy with the edit verbs, ApiTest.run('"
                  + var14
                  + "') until green, then Api.reply."
         )
         + (var15 == null ? "" : "\nTester: " + var9.d);
      this.start(var3, var9, var16, var2);
   }

   private void start(io.karatelabs.ext.max.jsapi.ApiChat.ChatSession var1, io.karatelabs.ext.max.jsapi.ApiChat.ChatTurn var2, String var3, CandidateRunner.Request var4) {
      Thread.ofVirtual().name("api-chat-" + var2.b).start(() -> this.runTurn(var1, var2, var3, var4));
   }

   private void runTurn(io.karatelabs.ext.max.jsapi.ApiChat.ChatSession var1, io.karatelabs.ext.max.jsapi.ApiChat.ChatTurn var2, String var3, CandidateRunner.Request var4) {
      String var5 = "failed";
      String var6 = null;
      String var7 = null;

      try {
         io.karatelabs.ext.max.jsapi.ApiChat.ChatTurn var8 = this.previous(var1, var2);
         io.karatelabs.ext.agent.events.AgentRunContext var26 = new io.karatelabs.ext.agent.events.AgentRunContext(var1.a, String.valueOf(var2.a), var8 == null ? null : var8.b);
         var var10 = this.history(var1, var2);
         ApiToolkit var11 = new ApiToolkit(
               this.root,
               this.api,
               this.triage,
               this.runner,
               this.requirement,
               var3x -> this.card(var1, var2, var3x),
               (var1x, var2x) -> this.harness.draft(var1x, Map.of("taken", this.takenIds()), var2x),
               var2.b
            )
            .withPostman(this.postman)
            .withTraffic(this.traffic)
            .withSecrets(this::secret)
            .withSecretNames(this::secretNames)
            .withDrafts(var1.f)
            .withAsk("message".equals(var2.c) && "bug".equals(triageChoice(var2.e)));
         LinkedHashMap var12 = new LinkedHashMap();
         var12.put("chatId", var1.a);
         var12.put("turnId", var2.a);
         if (var8 != null) {
            var12.put("prevRunId", var8.b);
         }

         if (var1.b != null) {
            var12.put("test", var1.b);
         }

         io.karatelabs.ext.agent.harness.HarnessResult var13 = this.harness
            .runApiTurn(
               var2.b,
               var3,
               var12,
               var5x -> var5x.systemPrompt(
                     "You help a manual tester build and run API tests in the Karate console.\nRespond with exactly one ```js block, then stop: it runs, and you see its result before your next turn. Variables persist across turns."
                  )
                  .addExtension(var11)
                  .conversationHistory(var10)
                  .runContext(var26)
                  .maxIterations(16)
                  .historyWindow(4)
                  .addEventListener(new HarnessSseBridge((var2xx, var3xx) -> this.harness.publish(var1.a, var2xx, var3xx)))
            );
         var5 = var13 == null ? "failed" : HarnessRun.terminalStatus(var13, false);
         if (var13 != null && var13.isSuccess() && var13.result() != null) {
            var6 = String.valueOf(var13.result());
         } else if (var13 != null) {
            var7 = var13.g() != null ? this.providerFailed(var2.b, var13.g(), var13.h()) : null;
         }
      } catch (RuntimeException var23) {
         logger.warn("api chat turn {} failed: {}", var2.b, var23.getMessage());
         io.karatelabs.ext.agent.llm.LlmException var9 = io.karatelabs.ext.agent.llm.LlmException.a(var23);
         var7 = var9 != null && var9.b() > 0 ? this.providerFailed(var2.b, var9.f(), var9.g()) : null;
      } finally {
         if (var4 != null) {
            var4.onDone().accept(CandidateRunner.Outcome.green());
         }
      }

      var1.c = var1.b;
      var1.d = this.digest(var1.b);
      if (var1.d != null) {
         var1.e.add(var1.b);
      }

      String var25 = var6 == null ? null : this.harness.sanitizer().apply(clean(var6));
      this.lock.lock();

      try {
         var2.f = var5;
         var2.g = var25;
         var2.h = var25 != null ? null : (var7 != null ? var7 : "The assistant stopped without an answer. Ask again, or say it another way.");
      } finally {
         this.lock.unlock();
      }

      LinkedHashMap var27 = new LinkedHashMap();
      var27.put("kind", "reply");
      var27.put("status", var5);
      if (var2.g != null) {
         var27.put("text", var2.g);
      } else {
         var27.put("error", var2.h);
      }

      this.card(var1, var2, var27);
   }

   private String providerFailed(String var1, String var2, String var3) {
      var var4 = this.harness.get(var1);
      String var5 = var4.get("provider") == null ? "The model" : var4.get("provider") + (var4.get("model") == null ? "" : " · " + var4.get("model"));
      return var5
         + " failed: "
         + var2
         + (var2.endsWith(".") ? " " : ". ")
         + (var3 == null ? "" : var3 + " ")
         + "The model is set in karate-agent.json at the work-folder root.";
   }

   private String secret(String var1) {
      io.karatelabs.ext.common.secrets.SecretStore var2 = this.secrets.get();
      String var3 = var2 == null ? null : var2.e().get(var1);
      return var3 != null ? var3 : System.getenv(var1);
   }

   private Collection<String> secretNames() {
      io.karatelabs.ext.common.secrets.SecretStore var1 = this.secrets.get();
      return (Collection<String>)(var1 == null ? List.of() : var1.e().keySet());
   }

   private String task(io.karatelabs.ext.max.jsapi.ApiChat.ChatSession var1, String var2, String var3, Object var4) {
      StringBuilder var5 = new StringBuilder();
      if (var2 != null) {
         var5.append("Earlier turns were about ")
            .append(var2)
            .append(
               this.digest(var2) != null
                  ? "; the tester is on another page now.\n"
                  : (
                     var1.e.contains(var2)
                        ? ", which was deleted since: it is not saved and has no passing run.\n"
                        : ", which was never saved: no run of it passed.\n"
                  )
            );
      }

      String var6 = this.digest(var1.b);
      if (var6 != null) {
         var1.e.add(var1.b);
      }

      if (var1.b == null) {
         var5.append("The tester's page is a new API test: no test file yet.");
      } else if (var6 == null && var1.b.equals(var1.c) && var1.d != null) {
         var5.append("The tester's page names ").append(var1.b).append(", which was deleted since your last reply: it is not saved.");
      } else if (var6 == null) {
         var5.append("The tester's page is a new API test, to be saved as ")
            .append(var1.b)
            .append(" once it passes.")
            .append(var1.f.holds(var1.b) ? " Your copy of it from earlier turns is held: ApiTest.read shows it." : "");
         String var7 = var1.f.moving(var1.b);
         if (var7 != null) {
            var5.append('\n').append(this.harness.sanitizer().apply(var7));
         }
      } else {
         var5.append("The tester's test is ").append(var1.b).append('.');
         if (var2 == null && var1.b.equals(var1.c) && !Objects.equals(var1.d, var6)) {
            var5.append(" It changed since your last reply (an Undo or an edit): read it again before you describe it.");
         }

         if (var1.f.holds(var1.b)) {
            var5.append(" Your copy of it from earlier turns is held: ApiTest.read shows it.");
         }
      }

      return var5.append(this.page(var1.b, var4)).append("\nTester: ").append(var3).toString();
   }

   // $VF: One or more variable merging failures!
   // $VF: Could not properly define all variable types!
   // Please report this to the Vineflower issue tracker, at https://github.com/Vineflower/vineflower/issues with a copy of the class file (if you have the rights to distribute it!)
   private String page(String var1, Object var2) {
      StringBuilder var3 = new StringBuilder();
      if (var1 != null) {
         TestId var4 = this.triage.id(this.root.resolve(var1));
         Object var5 = this.triage.triage(var4).get("state");
         if ("open".equals(var5)) {
            var3.append("\nThe page asks the tester the open question on ")
               .append(var1)
               .append(": bug or test? The tester's words \"it's a bug\" or \"the test is wrong\" answer it; you cannot.");
         } else if ("lost".equals(var5)) {
            var3.append("\nThe page asks the tester to discard a change to ").append(var1).append(" that was lost; nothing changes until the tester does.");
         }

         ArrayList var6 = new ArrayList();

         for (Object var11 : this.triage.change(var4).get("weakening") instanceof List ? (List)this.triage.change(var4).get("weakening") : List.of()) {
            if (var11 instanceof Map var12) {
               var6.add(clip(String.valueOf(var12.get("message") != null ? var12.get("message") : var12.get("kind")), 160));
            }
         }

         if (!var6.isEmpty()) {
            var3.append("\nThe page flags your last change to ").append(var1).append(" as weakening a check: ").append(String.join("; ", var6)).append('.');
         }
      }

      if (var2 instanceof Map var10 && var10.get("call") != null) {
         var3.append("\nThe tester just re-sent ")
            .append(clip(String.valueOf(var10.get("call")), 120))
            .append(" on the page: it answered ")
            .append(clip(String.valueOf(var10.get("status")), 40))
            .append(var10.get("body") == null ? "." : ": " + clip(String.valueOf(var10.get("body")), 600));
      }

      return this.harness.sanitizer().apply(var3.toString());
   }

   private boolean unsaved(String var1) {
      return var1 != null && this.digest(var1) == null && !"none".equals(this.triage.triage(this.triage.id(this.root.resolve(var1))).get("state"));
   }

   private String rebind(io.karatelabs.ext.max.jsapi.ApiChat.ChatSession var1, String var2) {
      this.lock.lock();

      Object var4;
      try {
         String var3 = var1.b;
         if (!Objects.equals(var3, var2)) {
            var1.b = var2;
            if (var2 != null) {
               this.byTest.put(var2, var1.a);
            }

            return var3;
         }

         var4 = null;
      } finally {
         this.lock.unlock();
      }

      return (String)var4;
   }

   private String digest(String var1) {
      if (var1 == null) {
         return null;
      } else {
         try {
            Path var2 = this.root.resolve(var1);
            return Files.isRegularFile(var2) ? io.karatelabs.ext.common.Sha256.a(Files.readAllBytes(var2)) : null;
         } catch (IOException var3) {
            return null;
         }
      }
   }

   private List<ConversationEntry> history(io.karatelabs.ext.max.jsapi.ApiChat.ChatSession var1, io.karatelabs.ext.max.jsapi.ApiChat.ChatTurn var2) {
      ArrayList var3 = new ArrayList();
      if (var1.b != null) {
         var var4 = this.triage.triage(this.triage.id(this.root.resolve(var1.b)));
         if (!"none".equals(var4.get("state"))) {
            var3.add(
               ConversationEntry.pinned(
                  "Open triage of "
                     + var1.b
                     + ": "
                     + String.valueOf(var4.get("state"))
                     + ", candidate "
                     + String.valueOf(var4.get("candidate"))
                     + ", base "
                     + String.valueOf(var4.get("base"))
                     + ". "
                     + describe(var4.get("failing"))
                     + (
                        var4.get("answer") instanceof Map var5
                           ? " Tester answered " + var5.get("choice") + (var5.get("ticket") != null ? " (" + var5.get("ticket") + ")" : "") + "."
                           : ""
                     ),
                  "Noted."
               )
            );
         }
      }

      Object var11 = new ArrayList();
      this.lock.lock();

      try {
         for (io.karatelabs.ext.max.jsapi.ApiChat.ChatTurn var14 : var1.g) {
            if (var14 != var2 && !"running".equals(var14.f)) {
               ((java.util.List)var11).add(var14);
            }
         }
      } finally {
         this.lock.unlock();
      }

      byte var13 = 9;
      if (((java.util.List)var11).size() > var13) {
         StringBuilder var15 = new StringBuilder("Earlier in this chat:");

         for (io.karatelabs.ext.max.jsapi.ApiChat.ChatTurn var8 : (Iterable<io.karatelabs.ext.max.jsapi.ApiChat.ChatTurn>) (Iterable<?>) (((java.util.List)var11).subList(0, ((java.util.List)var11).size() - var13))) {
            var15.append("\n- ").append(clip(var8.e, 120)).append(" → ").append(clip(var8.g == null ? var8.f : var8.g, 160));
         }

         var3.add(ConversationEntry.pinned(clip(var15.toString(), 2000), "Noted."));
         var11 = ((java.util.List)var11).subList(((java.util.List)var11).size() - var13, ((java.util.List)var11).size());
      }

      for (io.karatelabs.ext.max.jsapi.ApiChat.ChatTurn var17 : (Iterable<io.karatelabs.ext.max.jsapi.ApiChat.ChatTurn>) (Iterable<?>) (var11)) {
         var3.add(ConversationEntry.of(var17.e, var17.g == null ? "(no answer: the run " + var17.f + ")" : var17.g));
      }

      return var3;
   }

   private io.karatelabs.ext.max.jsapi.ApiChat.ChatSession chatFor(String var1, String var2) {
      this.lock.lock();

      io.karatelabs.ext.max.jsapi.ApiChat.ChatSession var4;
      try {
         io.karatelabs.ext.max.jsapi.ApiChat.ChatSession var3 = var1 == null ? null : this.chats.get(var1);
         if (var3 == null && var2 != null && this.byTest.get(var2) != null) {
            var3 = this.chats.get(this.byTest.get(var2));
         }

         if (var3 == null) {
            var3 = new io.karatelabs.ext.max.jsapi.ApiChat.ChatSession("chat-" + UUID.randomUUID().toString().replace("-", "").substring(0, 8), var2);
            this.chats.put(var3.a, var3);
         }

         if (var3.b == null && var2 != null) {
            var3.b = var2;
         }

         if (var3.b != null) {
            this.byTest.put(var3.b, var3.a);
         }

         var4 = var3;
      } finally {
         this.lock.unlock();
      }

      return var4;
   }

   private io.karatelabs.ext.max.jsapi.ApiChat.ChatTurn admit(io.karatelabs.ext.max.jsapi.ApiChat.ChatSession var1, String var2, String var3, String var4) {
      return this.admit(var1, var2, var3, var4, var4);
   }

   private io.karatelabs.ext.max.jsapi.ApiChat.ChatTurn admit(io.karatelabs.ext.max.jsapi.ApiChat.ChatSession var1, String var2, String var3, String var4, String var5) {
      this.lock.lock();

      io.karatelabs.ext.max.jsapi.ApiChat.ChatTurn var6;
      try {
         var6 = new io.karatelabs.ext.max.jsapi.ApiChat.ChatTurn(var1.g.size() + 1, var2, var3, this.harness.sanitizer().apply(var4), var5);
         var1.g.add(var6);
      } finally {
         this.lock.unlock();
      }

      LinkedHashMap var7 = new LinkedHashMap();
      var7.put("kind", "message");
      var7.put("from", var3);
      var7.put("text", var6.d);
      this.card(var1, var6, var7);
      return var6;
   }

   private io.karatelabs.ext.max.jsapi.ApiChat.ChatTurn previous(io.karatelabs.ext.max.jsapi.ApiChat.ChatSession var1, io.karatelabs.ext.max.jsapi.ApiChat.ChatTurn var2) {
      this.lock.lock();

      io.karatelabs.ext.max.jsapi.ApiChat.ChatTurn var4;
      try {
         int var3 = var1.g.indexOf(var2);
         var4 = var3 > 0 ? var1.g.get(var3 - 1) : null;
      } finally {
         this.lock.unlock();
      }

      return var4;
   }

   private void card(io.karatelabs.ext.max.jsapi.ApiChat.ChatSession var1, io.karatelabs.ext.max.jsapi.ApiChat.ChatTurn var2, Map<String, Object> var3) {
      LinkedHashMap var4 = new LinkedHashMap();
      var4.put("type", "chat");
      var4.put("chat", var1.a);
      var4.put("turn", var2.a);
      this.lock.lock();

      String var5;
      label149: {
         try {
            boolean var6 = "uncard".equals(var3.get("kind"));
            Object var7 = (var6 || "traffic".equals(var3.get("kind"))) && var3.get("to") != null ? uncard(var1, var3.get("to")) : null;
            if (!var6 || var7 != null) {
               var4.put("n", ++var1.h);
               if (var7 != null) {
                  var4.put("replaces", var7);
               }

               if (!var6) {
                  var4.putAll(var3);
               }

               var5 = this.harness.sanitizer().apply(StringUtils.formatJson(var4, false, false, false));
               if (!var6 && !"message".equals(var3.get("kind")) && !"reply".equals(var3.get("kind")) && Json.parseStrict(var5, true) instanceof Map var9) {
                  var9.remove("replaces");
                  var2.i.add(var9);
               }

               if ("run".equals(var3.get("kind")) && var3.get("path") instanceof String var14 && Boolean.TRUE.equals(var3.get("saved"))) {
                  var1.e.add(var14);
               }

               if ("run".equals(var3.get("kind"))
                  && var3.get("path") instanceof String var15
                  && (Boolean.TRUE.equals(var3.get("created")) || "red".equals(var3.get("status")) && var1.b == null)) {
                  var1.b = var15;
                  this.byTest.put(var15, var1.a);
               }

               if ("traffic".equals(var3.get("kind")) && var3.get("to") instanceof String var16 && var1.b == null) {
                  var1.b = var16;
                  this.byTest.put(var16, var1.a);
               }
               break label149;
            }
         } finally {
            this.lock.unlock();
         }

         return;
      }

      this.harness.publish(var1.a, "chat", var5);
   }

   private static Object uncard(io.karatelabs.ext.max.jsapi.ApiChat.ChatSession var0, Object var1) {
      for (io.karatelabs.ext.max.jsapi.ApiChat.ChatTurn var3 : var0.g) {
         var var4 = var3.i.iterator();

         while (var4.hasNext()) {
            Map var5 = (Map)var4.next();
            if ("traffic".equals(var5.get("kind")) && var1.equals(var5.get("to"))) {
               var4.remove();
               return var5.get("n");
            }
         }
      }

      return null;
   }

   private Map<String, Object> started(io.karatelabs.ext.max.jsapi.ApiChat.ChatSession var1, io.karatelabs.ext.max.jsapi.ApiChat.ChatTurn var2) {
      LinkedHashMap var3 = new LinkedHashMap();
      var3.put("chat", var1.a);
      var3.put("turn", var2.a);
      var3.put("runId", var2.b);
      var3.put("status", "started");
      if (var1.b != null) {
         var3.put("test", var1.b);
      }

      return var3;
   }

   private String testOf(Object var1) {
      if (var1 instanceof String var2 && !var2.isBlank()) {
         String var3 = var2.replaceFirst("^/+", "");
         if (!var3.endsWith(".feature")) {
            return null;
         } else {
            try {
               return this.triage.id(MaxPaths.resolveSandboxed(this.root, var3)).path();
            } catch (SecurityException var5) {
               return null;
            }
         }
      } else {
         return null;
      }
   }

   // $VF: One or more variable merging failures!
   // $VF: Could not properly define all variable types!
   // Please report this to the Vineflower issue tracker, at https://github.com/Vineflower/vineflower/issues with a copy of the class file (if you have the rights to distribute it!)
   private List<String> takenIds() {
      ArrayList var1 = new ArrayList();

      Object listVal = this.requirement.jsGet("list");
      Object listResult = listVal instanceof JavaInvokable ji ? ji.invoke() : null;

      for (Object var8 : listResult instanceof List ? (List)listResult : List.of()) {
         if (var8 instanceof Map var5 && var5.get("id") != null) {
            var1.add(String.valueOf(var5.get("id")));
         }
      }

      return var1;
   }

   private static String describe(Object var0) {
      return var0 instanceof Map var1
         ? "Failing step " + var1.get("step") + ": " + var1.get("expected") + "; it got: " + clip(String.valueOf(var1.get("actual")), 600)
         : "";
   }

   private static String clean(String var0) {
      return var0.replaceAll("(?s)```.*?```", "").strip();
   }

   private static String clip(String var0, int var1) {
      if (var0 == null) {
         return "";
      } else {
         String var2 = var0.replaceAll("\\s+", " ").strip();
         return var2.length() > var1 ? var2.substring(0, var1) + "…" : var2;
      }
   }

   private static String newRunId() {
      return UUID.randomUUID().toString().substring(0, 8);
   }

   private static Map<String, Object> refuse(String var0, String var1) {
      LinkedHashMap var2 = new LinkedHashMap();
      var2.put("error", var1);
      var2.put("code", var0);
      var2.put("status", 409);
      return var2;
   }

   static final class ChatSession {
      final String a;
      volatile String b;
      volatile String c;
      volatile String d;
      final Set<String> e = ConcurrentHashMap.newKeySet();
      final ApiToolkit.Drafts f = new ApiToolkit.Drafts();
      final List<io.karatelabs.ext.max.jsapi.ApiChat.ChatTurn> g = new ArrayList<>();
      int h;

      ChatSession(String var1, String var2) {
         this.a = var1;
         this.b = var2;
      }
   }

   static final class ChatTurn {
      final int a;
      final String b;
      final String c;
      final String d;
      final String e;
      volatile String f = "running";
      volatile String g;
      volatile String h;
      final List<Map<String, Object>> i = new ArrayList<>();

      ChatTurn(int var1, String var2, String var3, String var4, String var5) {
         this.a = var1;
         this.b = var2;
         this.c = var3;
         this.d = var4;
         this.e = var5;
      }
   }
}
