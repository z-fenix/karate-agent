package io.karatelabs.ext.agent.harness;

import io.karatelabs.ext.agent.events.AgentEvent;
import io.karatelabs.ext.agent.events.AgentEventListener;
import io.karatelabs.ext.agent.events.AgentEventReporter;
import io.karatelabs.ext.agent.events.AgentEventFileWriter;
import io.karatelabs.ext.agent.llm.LlmProvider;
import io.karatelabs.ext.agent.runtime.ExitCondition;
import io.karatelabs.ext.agent.runtime.AgentLoop;
import io.karatelabs.ext.agent.runtime.AgentRunState;
import io.karatelabs.ext.agent.runtime.ConversationEntry;
import io.karatelabs.ext.agent.runtime.RetryPolicy;
import io.karatelabs.ext.agent.runtime.AgentExtension;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class AgentHarness {
   private static final Logger b = LoggerFactory.getLogger(AgentHarness.class);
   private final String c;
   private final List<AgentExtension> d;
   private final io.karatelabs.ext.agent.llm.LlmClient e;
   private final LlmProvider f;
   private final String g;
   private final int h;
   private final int i;
   private final io.karatelabs.ext.agent.runtime.BudgetExitCondition j;
   private final long k;
   private final List<ExitCondition> l;
   private final RetryPolicy m;
   private final Path n;
   private final boolean o;
   private final String p;
   private final List<AgentEventListener> q;
   private final Consumer<AgentLoop> r;
   private final UnaryOperator<String> s;
   static final String a = "A value reading ‹masked› or ‹secret:NAME› is hidden from you on purpose; the run uses the real one. Type a stored secret as secret('NAME'); never write a ‹…› label or guess the value.";
   private final List<ConversationEntry> t;
   private final io.karatelabs.ext.agent.events.AgentRunContext u;

   private AgentHarness(io.karatelabs.ext.agent.harness.AgentHarness.Builder var1) {
      this.c = var1.systemPrompt;
      this.d = List.copyOf(var1.extensions);
      this.e = var1.llmClient;
      this.f = var1.provider;
      this.g = var1.model;
      this.h = var1.maxIterations;
      this.i = var1.historyWindow;
      this.j = var1.budget;
      this.k = var1.deadlineMillis;
      this.l = List.copyOf(var1.exitConditions);
      this.m = var1.retryPolicy;
      this.n = var1.transcriptPath;
      this.o = var1.logEvents;
      this.p = var1.runId;
      this.q = List.copyOf(var1.eventListeners);
      this.r = var1.onLoopCreated;
      this.s = var1.redactor;
      this.t = List.copyOf(var1.conversationHistory);
      this.u = var1.runContext;
   }

   public io.karatelabs.ext.agent.harness.HarnessResult run(String var1) {
      io.karatelabs.ext.agent.events.AgentEventBus var2 = this.p != null ? new io.karatelabs.ext.agent.events.AgentEventBus(this.p) : new io.karatelabs.ext.agent.events.AgentEventBus();
      Object var3 = this.e != null ? this.e : this.c();
      boolean var4 = this.e == null;
      if (this.s != null || this.n != null) {
         var3 = new io.karatelabs.ext.agent.llm.RedactingLlmClient(
            (io.karatelabs.ext.agent.llm.LlmClient)var3, this.s != null ? this.s : UnaryOperator.identity(), this.n == null ? null : var1x -> var2.publish(a(var1x))
         );
      }

      io.karatelabs.ext.agent.harness.HarnessResult var8;
      try {
         if (this.n != null) {
            var2.subscribe(new AgentEventFileWriter(this.n, var2.runId(), var1, ((io.karatelabs.ext.agent.llm.LlmClient)var3).b(), io.karatelabs.ext.agent.events.AgentEventFileWriter.a, this.s, this.u));
         }

         if (this.o) {
            var2.subscribe(new io.karatelabs.ext.agent.events.AgentEventLogger(), true);
         }

         for (AgentEventListener var6 : this.q) {
            var2.subscribe(var6);
         }

         io.karatelabs.ext.agent.runtime.AgentLoop.Builder var17 = io.karatelabs.ext.agent.runtime.AgentLoop.builder().llmClient((io.karatelabs.ext.agent.llm.LlmClient)var3).listener(new AgentEventReporter(var2)).maxIterations(this.h).historyWindow(this.i).conversationHistory(this.t);
         if (this.s != null) {
            var17.toolExecutor(new io.karatelabs.ext.agent.runtime.JavaScriptExecutor(this.s));
            var17.systemPrompt(
               this.c == null
                  ? "A value reading ‹masked› or ‹secret:NAME› is hidden from you on purpose; the run uses the real one. Type a stored secret as secret('NAME'); never write a ‹…› label or guess the value."
                  : this.c
                     + "\nA value reading ‹masked› or ‹secret:NAME› is hidden from you on purpose; the run uses the real one. Type a stored secret as secret('NAME'); never write a ‹…› label or guess the value."
            );
         } else if (this.c != null) {
            var17.systemPrompt(this.c);
         }

         if (this.j != null) {
            var17.addExitCondition(this.j);
         }

         if (this.k > 0L) {
            var17.addExitCondition(new io.karatelabs.ext.agent.runtime.DeadlineExitCondition(this.k));
         }

         for (ExitCondition var7 : this.l) {
            var17.addExitCondition(var7);
         }

         if (this.m != null) {
            var17.retryPolicy(this.m);
         }

         for (AgentExtension var21 : this.d) {
            var17.addExtension(var21);
         }

         b.info("harness run start (run={}, model={}, task={})", new Object[]{var2.runId(), ((io.karatelabs.ext.agent.llm.LlmClient)var3).b(), var1});
         AgentLoop var20 = var17.build();
         if (this.r != null) {
            this.r.accept(var20);
         }

         AgentRunState var22 = var20.run(var1);
         b.info("harness run end (run={}, status={}, iterations={})", new Object[]{var2.runId(), var22.status(), var22.iterations()});
         var8 = new io.karatelabs.ext.agent.harness.HarnessResult(var2.runId(), var22, this.n);
      } finally {
         var2.close();
         if (var4) {
            try {
               ((io.karatelabs.ext.agent.llm.LlmClient)var3).close();
            } catch (Exception var15) {
               b.debug("llm client close failed: {}", var15.toString());
            }
         }
      }

      return var8;
   }

   private static io.karatelabs.ext.agent.events.AgentEvent.RequestFrameEvent a(io.karatelabs.ext.agent.llm.LlmRequest var0) {
      ArrayList var1 = new ArrayList(var0.messages().size());

      for (io.karatelabs.ext.agent.llm.LlmMessage var3 : var0.messages()) {
         LinkedHashMap var4 = new LinkedHashMap();
         var4.put("role", var3.role().getValue());
         var4.put("text", var3.text());
         if (var3.hasImages()) {
            var4.put("images", var3.parts().stream().filter(io.karatelabs.ext.agent.llm.LlmMessage.Part::isImage).count());
         }

         var1.add(var4);
      }

      return io.karatelabs.ext.agent.events.AgentEvent.RequestFrameEvent.a(var0.stage(), var0.systemPrompt(), var1);
   }

   private io.karatelabs.ext.agent.llm.LlmClient c() {
      return a(this.g, this.f, System::getenv).stopSequences(List.of("\n```\n")).extraTokenHeadroom(24576).build();
   }

   public static io.karatelabs.ext.agent.llm.LlmClient.Builder a(String var0, LlmProvider var1, Function<String, String> var2) {
      Object[] var3 = b(var0, var1, var2);
      LlmProvider var4 = (LlmProvider)var3[0];
      String var5 = (String)var3[1];
      io.karatelabs.ext.agent.llm.LlmClient.Builder var6 = io.karatelabs.ext.agent.llm.LlmClient.d().provider(var4);
      if (var5 != null) {
         var6.model(var5);
      }

      return var6.endpoint(var4.resolveEndpoint(var2));
   }

   public static Object[] b(String var0, LlmProvider var1, Function<String, String> var2) {
      if (var1 == io.karatelabs.ext.agent.llm.LlmProvider.OPENROUTER && var0 != null && !var0.startsWith("openrouter/")) {
         return new Object[]{io.karatelabs.ext.agent.llm.LlmProvider.OPENROUTER, var0};
      } else {
         LlmProvider var3 = var1 != null ? var1 : a(var2);
         Object[] var4 = io.karatelabs.ext.agent.llm.LlmProvider.parseModel(var0, var3);
         if (var4[1] == null) {
            var4[1] = io.karatelabs.ext.agent.llm.LlmRegistry.a((LlmProvider)var4[0], var2);
         }

         return var4;
      }
   }

   static LlmProvider a() {
      return a(System::getenv);
   }

   static LlmProvider a(Function<String, String> var0) {
      if (var0.apply(io.karatelabs.ext.agent.llm.LlmProvider.ANTHROPIC.getEnvVar()) != null) {
         return io.karatelabs.ext.agent.llm.LlmProvider.ANTHROPIC;
      } else if (var0.apply(io.karatelabs.ext.agent.llm.LlmProvider.OPENAI.getEnvVar()) != null) {
         return io.karatelabs.ext.agent.llm.LlmProvider.OPENAI;
      } else {
         return var0.apply(io.karatelabs.ext.agent.llm.LlmProvider.OPENROUTER.getEnvVar()) != null ? io.karatelabs.ext.agent.llm.LlmProvider.OPENROUTER : io.karatelabs.ext.agent.llm.LlmProvider.ANTHROPIC;
      }
   }

   public static io.karatelabs.ext.agent.harness.AgentHarness.Builder builder() {
      return new io.karatelabs.ext.agent.harness.AgentHarness.Builder();
   }

   public static final class Builder {
      private String systemPrompt;
      private final List<AgentExtension> extensions = new ArrayList<>();
      private io.karatelabs.ext.agent.llm.LlmClient llmClient;
      private LlmProvider provider;
      private String model;
      private int maxIterations = 50;
      private int historyWindow = 1;
      private io.karatelabs.ext.agent.runtime.BudgetExitCondition budget;
      private long deadlineMillis;
      private final List<ExitCondition> exitConditions = new ArrayList<>();
      private RetryPolicy retryPolicy;
      private Path transcriptPath;
      private boolean logEvents = true;
      private String runId;
      private final List<AgentEventListener> eventListeners = new ArrayList<>();
      private Consumer<AgentLoop> onLoopCreated;
      private UnaryOperator<String> redactor;
      private final List<ConversationEntry> conversationHistory = new ArrayList<>();
      private io.karatelabs.ext.agent.events.AgentRunContext runContext;

      public io.karatelabs.ext.agent.harness.AgentHarness.Builder systemPrompt(String var1) {
         this.systemPrompt = var1;
         return this;
      }

      public io.karatelabs.ext.agent.harness.AgentHarness.Builder addExtension(AgentExtension var1) {
         this.extensions.add(var1);
         return this;
      }

      public List<AgentExtension> extensions() {
         return Collections.unmodifiableList(this.extensions);
      }

      public io.karatelabs.ext.agent.harness.AgentHarness.Builder llmClient(io.karatelabs.ext.agent.llm.LlmClient var1) {
         this.llmClient = var1;
         return this;
      }

      public io.karatelabs.ext.agent.harness.AgentHarness.Builder provider(LlmProvider var1) {
         this.provider = var1;
         return this;
      }

      public io.karatelabs.ext.agent.harness.AgentHarness.Builder model(String var1) {
         this.model = var1;
         return this;
      }

      public io.karatelabs.ext.agent.harness.AgentHarness.Builder maxIterations(int var1) {
         this.maxIterations = var1;
         return this;
      }

      public io.karatelabs.ext.agent.harness.AgentHarness.Builder historyWindow(int var1) {
         this.historyWindow = var1;
         return this;
      }

      public io.karatelabs.ext.agent.harness.AgentHarness.Builder budget(io.karatelabs.ext.agent.runtime.BudgetExitCondition var1) {
         this.budget = var1;
         return this;
      }

      public io.karatelabs.ext.agent.harness.AgentHarness.Builder deadlineMillis(long var1) {
         this.deadlineMillis = var1;
         return this;
      }

      public io.karatelabs.ext.agent.harness.AgentHarness.Builder addExitCondition(ExitCondition var1) {
         this.exitConditions.add(var1);
         return this;
      }

      public io.karatelabs.ext.agent.harness.AgentHarness.Builder retryPolicy(RetryPolicy var1) {
         this.retryPolicy = var1;
         return this;
      }

      public io.karatelabs.ext.agent.harness.AgentHarness.Builder transcriptPath(Path var1) {
         this.transcriptPath = var1;
         return this;
      }

      public io.karatelabs.ext.agent.harness.AgentHarness.Builder logEvents(boolean var1) {
         this.logEvents = var1;
         return this;
      }

      public io.karatelabs.ext.agent.harness.AgentHarness.Builder runId(String var1) {
         this.runId = var1;
         return this;
      }

      public io.karatelabs.ext.agent.harness.AgentHarness.Builder addEventListener(AgentEventListener var1) {
         this.eventListeners.add(var1);
         return this;
      }

      public io.karatelabs.ext.agent.harness.AgentHarness.Builder onLoopCreated(Consumer<AgentLoop> var1) {
         this.onLoopCreated = var1;
         return this;
      }

      public io.karatelabs.ext.agent.harness.AgentHarness.Builder redactor(UnaryOperator<String> var1) {
         if (this.redactor == null) {
            this.redactor = var1;
         }

         return this;
      }

      public io.karatelabs.ext.agent.harness.AgentHarness.Builder conversationHistory(List<ConversationEntry> var1) {
         this.conversationHistory.addAll(var1);
         return this;
      }

      public io.karatelabs.ext.agent.harness.AgentHarness.Builder runContext(io.karatelabs.ext.agent.events.AgentRunContext var1) {
         this.runContext = var1;
         return this;
      }

      public AgentHarness build() {
         return new AgentHarness(this);
      }

      public io.karatelabs.ext.agent.harness.HarnessResult run(String var1) {
         return this.build().run(var1);
      }
   }
}
