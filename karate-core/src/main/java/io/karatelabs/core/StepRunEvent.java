package io.karatelabs.core;

import io.karatelabs.gherkin.Step;
import java.util.LinkedHashMap;
import java.util.Map;

public record StepRunEvent(RunEventType type, Step step, ScenarioRuntime scenarioRuntime, StepResult result, long timeStamp) implements RunEvent {
   public static StepRunEvent enter(Step step, ScenarioRuntime sr) {
      return new StepRunEvent(RunEventType.STEP_ENTER, step, sr, null, System.currentTimeMillis());
   }

   public static StepRunEvent exit(StepResult result, ScenarioRuntime sr) {
      Step step = result != null ? result.getStep() : null;
      return new StepRunEvent(RunEventType.STEP_EXIT, step, sr, result, System.currentTimeMillis());
   }

   @Override
   public RunEventType getType() {
      return this.type;
   }

   @Override
   public long getTimeStamp() {
      return this.timeStamp;
   }

   @Override
   public Map<String, Object> toJson() {
      Map<String, Object> map = new LinkedHashMap<>();
      if (this.step != null) {
         map.put("line", this.step.getLine());
         map.put("prefix", this.step.getPrefix());
         map.put("text", this.step.getText());
      }

      if (this.type == RunEventType.STEP_EXIT && this.result != null) {
         map.put("status", this.result.getStatus().name().toLowerCase());
         map.put("durationMillis", this.result.getDurationMillis());
      }

      return map;
   }
}
