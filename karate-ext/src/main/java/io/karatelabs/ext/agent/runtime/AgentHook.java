package io.karatelabs.ext.agent.runtime;

import java.util.List;
import java.util.Optional;

public interface AgentHook {
   default void a(AgentContext var1) {
   }

   default Optional<String> afterTool(AgentContext var1, String var2, ToolResult var3, String var4) {
      return Optional.empty();
   }

   default String a(AgentContext var1, String var2, ToolResult var3, String var4) {
      return var4;
   }

   default List<io.karatelabs.ext.agent.llm.LlmMessage> a(AgentContext var1, List<io.karatelabs.ext.agent.llm.LlmMessage> var2) {
      return var2;
   }

   default Optional<String> a(String var1) {
      return Optional.empty();
   }
}
