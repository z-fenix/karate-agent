package io.karatelabs.ext.agent.runtime;

import java.util.List;
import java.util.Map;

public interface AgentExtension {
   default void init(AgentContext var1) {
   }

   default Map<String, Object> bindings() {
      return Map.of();
   }

   default String promptFragment() {
      return "";
   }

   default List<AgentHook> hooks() {
      return List.of();
   }

   default List<ExitCondition> exitConditions() {
      return List.of();
   }

   default List<String> terminalCallNames() {
      return List.of();
   }

   default String finishInstruction() {
      return null;
   }

   default String finishInstruction(int var1) {
      return this.finishInstruction();
   }

   default String closingInstruction(AgentOutcome var1) {
      return null;
   }

   default AgentOutcome proseOutcome(String var1) {
      return null;
   }

   default String refusal(AgentOutcome var1) {
      return null;
   }

   default long toolTimeoutMs() {
      return 0L;
   }

   default void close() {
   }
}
