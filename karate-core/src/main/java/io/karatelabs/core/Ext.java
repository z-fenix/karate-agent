package io.karatelabs.core;

import java.util.Collections;
import java.util.Map;

public interface Ext extends RunListener {
   default void onBoot(Suite suite) {
   }

   default void onShutdown() {
   }

   default boolean requiresJsonlEvents() {
      return false;
   }

   default Map<String, Object> getManifest() {
      return Collections.emptyMap();
   }

   @Override
   default boolean onEvent(RunEvent event) {
      return true;
   }
}
