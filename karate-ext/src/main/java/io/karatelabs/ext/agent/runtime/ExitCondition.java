package io.karatelabs.ext.agent.runtime;

import java.util.Optional;

@FunctionalInterface
public interface ExitCondition {
   Optional<AgentOutcome> check(AgentRunState var1);

   default Optional<AgentOutcome> a(AgentRunState var1, ToolStep var2) {
      return this.check(var1);
   }
}
