package io.karatelabs.ext.agent.events;

import io.karatelabs.ext.agent.runtime.AgentRunState;
import java.util.Map;

public final class AgentEventReporter implements io.karatelabs.ext.agent.runtime.AgentRunListener {
   private final AgentEventBus a;
   private volatile int b;
   private volatile long c;

   public AgentEventReporter(AgentEventBus var1) {
      this.a = var1;
   }

   @Override
   public void onIterationStart(int var1) {
      this.b = var1;
      this.a.publish(io.karatelabs.ext.agent.events.AgentEvent.IterationStartEvent.a(var1));
   }

   @Override
   public void onContextTrim(String var1) {
      this.a.publish(io.karatelabs.ext.agent.events.AgentEvent.ContextTrimEvent.a(this.b, var1));
   }

   @Override
   public void onLlmRequest(int var1) {
      this.c = io.karatelabs.ext.agent.events.AgentEvent.f();
      this.a.publish(io.karatelabs.ext.agent.events.AgentEvent.LlmRequestEvent.a(var1));
   }

   @Override
   public void onLlmResponseStart(int var1) {
      this.a.publish(io.karatelabs.ext.agent.events.AgentEvent.LlmResponseStartEvent.a(var1));
   }

   @Override
   public void onLlmResponse(int var1, io.karatelabs.ext.agent.llm.LlmResponse var2) {
      long var3 = this.c;
      long var5 = var3 > 0L ? io.karatelabs.ext.agent.events.AgentEvent.f() - var3 : 0L;
      this.a.publish(new io.karatelabs.ext.agent.events.AgentEvent.LlmResponseEvent(io.karatelabs.ext.agent.events.AgentEvent.f(), var1, var2.text(), var2.model(), var2.inputTokens(), var2.outputTokens(), var2.cacheReadTokens(), var2.cacheCreationTokens(), var5, var2.rawFinishReason(), var2.refusal(), var2.reasoning()));
   }

   @Override
   public void onTextChunk(String var1, String var2) {
      this.a.publish(io.karatelabs.ext.agent.events.AgentEvent.TextChunkEvent.a(this.b, var1));
   }

   @Override
   public void onToolRequest(String var1) {
      this.a.publish(io.karatelabs.ext.agent.events.AgentEvent.ToolRequestEvent.a(this.b, var1));
   }

   @Override
   public void onToolResult(String var1, String var2) {
      this.a.publish(io.karatelabs.ext.agent.events.AgentEvent.ToolResultEvent.a(this.b, var1, var2));
   }

   @Override
   public void onScreenshot(int var1, String var2, String var3) {
      this.a.publish(io.karatelabs.ext.agent.events.AgentEvent.ScreenshotEvent.a(var1, var2, var3));
   }

   @Override
   public void onHealed(int var1, String var2, String var3, String var4, Double var5) {
      this.a.publish(io.karatelabs.ext.agent.events.AgentEvent.HealedEvent.a(var1, var2, var3, var4, var5));
   }

   @Override
   public void onArtifact(int var1, io.karatelabs.ext.agent.runtime.AgentArtifact var2) {
      String var3 = var2.c() != null ? var2.c().toString() : null;
      this.a.publish(io.karatelabs.ext.agent.events.AgentEvent.ArtifactEvent.a(var1, var2.d(), var2.e(), var2.f(), var3));
   }

   @Override
   public void onConsole(int var1, String var2) {
      this.a.publish(io.karatelabs.ext.agent.events.AgentEvent.ConsoleEvent.a(var1, var2));
   }

   @Override
   public void onHint(int var1, String var2, String var3) {
      this.a.publish(io.karatelabs.ext.agent.events.AgentEvent.HintEvent.a(var1, var2, var3));
   }

   @Override
   public void onStep(String var1, String var2, String var3) {
      this.a.publish(io.karatelabs.ext.agent.events.AgentEvent.StepEvent.a(this.b, var1, var2, var3));
   }

   @Override
   public void onAssertion(Map<String, Object> var1) {
      this.a.publish(io.karatelabs.ext.agent.events.AgentEvent.AssertionEvent.a(this.b, var1));
   }

   @Override
   public void onPaused() {
      this.a.publish(io.karatelabs.ext.agent.events.AgentEvent.PausedEvent.a(this.b));
   }

   @Override
   public void onResumed(String var1) {
      this.a.publish(io.karatelabs.ext.agent.events.AgentEvent.ResumedEvent.a(this.b, var1));
   }

   @Override
   public void onComplete(AgentRunState var1) {
      this.a.publish(new io.karatelabs.ext.agent.events.AgentEvent.CompleteEvent(io.karatelabs.ext.agent.events.AgentEvent.f(), var1.status().name(), var1.finalResult(), var1.iterations(), var1.inputTokens(), var1.outputTokens(), var1.cacheReadTokens(), var1.cacheCreationTokens(), var1.lastError()));
   }

   @Override
   public void onError(io.karatelabs.ext.agent.llm.LlmException var1) {
      this.a.publish(io.karatelabs.ext.agent.events.AgentEvent.ErrorEvent.a(this.b, var1.getMessage(), var1.e(), var1.g()));
   }
}
