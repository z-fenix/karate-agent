package io.karatelabs.ext.agent.runtime;

import java.util.Optional;

@FunctionalInterface
public interface OutcomeValidator {
   Optional<String> a(AgentContext var1, AgentOutcome var2);
}
