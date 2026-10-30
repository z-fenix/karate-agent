package io.karatelabs.ext.agent.runtime;

import java.util.Map;

public interface AgentRunListener {
   default void onIterationStart(int var1) {
   }

   default void onContextTrim(String var1) {
   }

   default void onLlmRequest(int var1) {
   }

   default void onLlmResponseStart(int var1) {
   }

   default void onLlmResponse(int var1, io.karatelabs.ext.agent.llm.LlmResponse var2) {
   }

   default void onTextChunk(String var1, String var2) {
   }

   default void onToolRequest(String var1) {
   }

   default void onToolResult(String var1, String var2) {
   }

   default void onScreenshot(int var1, String var2, String var3) {
   }

   default void onHealed(int var1, String var2, String var3, String var4, Double var5) {
   }

   default void onArtifact(int var1, AgentArtifact var2) {
   }

   default void onConsole(int var1, String var2) {
   }

   default void onHint(int var1, String var2, String var3) {
   }

   default void onComplete(AgentRunState var1) {
   }

   default void onError(io.karatelabs.ext.agent.llm.LlmException var1) {
   }

   default void onStep(String var1, String var2, String var3) {
   }

   default void onAssertion(Map<String, Object> var1) {
   }

   default void onPaused() {
   }

   default void onResumed(String var1) {
   }
}
