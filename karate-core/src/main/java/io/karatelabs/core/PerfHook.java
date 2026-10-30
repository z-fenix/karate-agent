package io.karatelabs.core;

import io.karatelabs.http.HttpRequest;

public interface PerfHook {
   String getPerfEventName(HttpRequest var1, ScenarioRuntime var2);

   void reportPerfEvent(PerfEvent var1);

   default void submit(Runnable task) {
      task.run();
   }

   default void afterFeature(FeatureResult result) {
   }

   default void pause(Number millis) {
      try {
         Thread.sleep(millis.longValue());
      } catch (InterruptedException var3) {
         Thread.currentThread().interrupt();
      }
   }
}
