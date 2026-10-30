package io.karatelabs.ext.agent.runtime;

import java.util.Map;

public interface AgentContext {
   AgentRunState state();

   int maxIterations();

   ToolExecutor toolExecutor();

   void submitOutcome(AgentOutcome var1);

   default io.karatelabs.ext.agent.llm.LlmClient llmClient() {
      return null;
   }

   default boolean lastResponseTruncated() {
      return false;
   }

   void hint(String var1, String var2);

   void step(String var1, String var2, String var3);

   void assertion(Map<String, Object> var1);

   void screenshot(String var1, String var2);

   void healed(String var1, String var2, String var3, Double var4);
}
