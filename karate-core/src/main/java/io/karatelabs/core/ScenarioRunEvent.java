package io.karatelabs.core;

import io.karatelabs.gherkin.Feature;
import io.karatelabs.gherkin.Scenario;
import java.util.LinkedHashMap;
import java.util.Map;

public record ScenarioRunEvent(RunEventType type, ScenarioRuntime source, ScenarioResult result, long timeStamp) implements RunEvent {
   public static ScenarioRunEvent enter(ScenarioRuntime sr) {
      return new ScenarioRunEvent(RunEventType.SCENARIO_ENTER, sr, null, System.currentTimeMillis());
   }

   public static ScenarioRunEvent exit(ScenarioRuntime sr, ScenarioResult result) {
      return new ScenarioRunEvent(RunEventType.SCENARIO_EXIT, sr, result, System.currentTimeMillis());
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
      if (this.source != null && this.source.getScenario() != null) {
         Scenario scenario = this.source.getScenario();
         FeatureRuntime fr = this.source.getFeatureRuntime();
         Feature feature = fr != null ? fr.getFeature() : null;
         boolean reportDisabled = this.result != null && this.result.isReportDisabled();
         if (feature != null && feature.getResource() != null) {
            map.put("feature", feature.getResource().getRelativePath());
         }

         String stableId = this.result != null ? this.result.getStableId() : null;
         map.put("slug", RunUtils.effectiveSlug(stableId, scenario, feature));
         ScenarioResult own = this.result != null ? this.result : this.source.getResult();
         if (own != null && own.getExecutionIndex() > 0) {
            map.put("executionIndex", own.getExecutionIndex());
         }

         map.put("name", scenario.getName());
         map.put("description", reportDisabled ? null : scenario.getDescription());
         map.put("line", scenario.getLine());
         map.put("refId", scenario.getRefId());
         map.put("callDepth", fr != null ? fr.getCallDepth() : 0);
         map.put("tags", RunUtils.tagTexts(scenario.getTagsEffective()));
         if (scenario.isOutlineExample()) {
            map.put("isOutlineExample", true);
            map.put("exampleIndex", scenario.getExampleIndex());
            map.put("outlineSlug", RunUtils.outlineSlug(RunUtils.parentOutline(scenario), feature));
         }
      }

      if (this.type == RunEventType.SCENARIO_EXIT && this.result != null) {
         map.put("passed", !this.result.isFailed());
         map.put("skipped", this.result.isSkipped());
         map.put("durationMillis", this.result.getDurationMillis());
         if (this.result.isFailed()) {
            if (this.result.isReportDisabled()) {
               map.put("error", "output suppressed by @report=false (full detail in runtime logs)");
               map.put("errorReason", "output suppressed by @report=false (full detail in runtime logs)");
            } else {
               map.put("error", this.result.getFailureMessageWithLocation());
               map.put("errorReason", this.result.getFailureReason());
            }
         }
      }

      return map;
   }
}
