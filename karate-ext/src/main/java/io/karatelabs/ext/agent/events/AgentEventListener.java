package io.karatelabs.ext.agent.events;

@FunctionalInterface
public interface AgentEventListener extends AutoCloseable {
   void onEvent(AgentEvent var1);

   @Override
   default void close() {
   }
}
