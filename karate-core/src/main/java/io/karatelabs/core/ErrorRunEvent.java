package io.karatelabs.core;

import java.util.LinkedHashMap;
import java.util.Map;

public record ErrorRunEvent(ScenarioRuntime scenarioRuntime, Throwable error, long timeStamp) implements RunEvent {
   public static ErrorRunEvent of(Throwable error, ScenarioRuntime sr) {
      return new ErrorRunEvent(sr, error, System.currentTimeMillis());
   }

   @Override
   public RunEventType getType() {
      return RunEventType.ERROR;
   }

   @Override
   public long getTimeStamp() {
      return this.timeStamp;
   }

   @Override
   public Map<String, Object> toJson() {
      Map<String, Object> map = new LinkedHashMap<>();
      if (this.scenarioRuntime != null) {
         FeatureRuntime fr = this.scenarioRuntime.getFeatureRuntime();
         if (fr != null && fr.getFeature() != null && fr.getFeature().getResource() != null) {
            map.put("feature", fr.getFeature().getResource().getRelativePath());
         }

         if (this.scenarioRuntime.getScenario() != null) {
            map.put("scenario", this.scenarioRuntime.getScenario().getName());
         }
      }

      if (this.error != null) {
         map.put("message", this.error.getMessage());
         map.put("type", this.error.getClass().getSimpleName());
      }

      return map;
   }
}
