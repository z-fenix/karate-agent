package io.karatelabs.markup;

import java.util.Map;
import java.util.function.Consumer;

public interface ActionDispatchHost {
   Map<String, Object> getActions();

   boolean isActionDispatched();

   void markActionDispatched();

   default void setEagerDispatchHook(Consumer<String> hook) {
   }
}
