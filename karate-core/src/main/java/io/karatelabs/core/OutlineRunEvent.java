package io.karatelabs.core;

import io.karatelabs.gherkin.Feature;
import io.karatelabs.gherkin.ScenarioOutline;
import java.util.LinkedHashMap;
import java.util.Map;

public record OutlineRunEvent(FeatureRuntime source, ScenarioOutline outline, long timeStamp) implements RunEvent {
   public static OutlineRunEvent enter(FeatureRuntime fr, ScenarioOutline outline) {
      return new OutlineRunEvent(fr, outline, System.currentTimeMillis());
   }

   @Override
   public RunEventType getType() {
      return RunEventType.OUTLINE_ENTER;
   }

   @Override
   public long getTimeStamp() {
      return this.timeStamp;
   }

   @Override
   public Map<String, Object> toJson() {
      Map<String, Object> map = new LinkedHashMap<>();
      Feature feature = this.source != null ? this.source.getFeature() : null;
      if (feature != null && feature.getResource() != null) {
         map.put("feature", feature.getResource().getRelativePath());
      }

      if (this.outline != null) {
         map.put("slug", RunUtils.outlineSlug(this.outline, feature));
         map.put("name", this.outline.getName());
         map.put("description", this.outline.getDescription());
         map.put("line", this.outline.getLine());
         map.put("numExamples", this.outline.getNumScenarios());
         map.put("tags", RunUtils.mergedTagTexts(feature != null ? feature.getTags() : null, this.outline.getTags()));
      }

      map.put("callDepth", this.source != null ? this.source.getCallDepth() : 0);
      return map;
   }
}
