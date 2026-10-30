package io.karatelabs.core;

import io.karatelabs.gherkin.Feature;
import java.util.LinkedHashMap;
import java.util.Map;

public record FeatureRunEvent(RunEventType type, FeatureRuntime source, FeatureResult result, long timeStamp) implements RunEvent {
   public static FeatureRunEvent enter(FeatureRuntime fr) {
      return new FeatureRunEvent(RunEventType.FEATURE_ENTER, fr, null, System.currentTimeMillis());
   }

   public static FeatureRunEvent exit(FeatureRuntime fr, FeatureResult result) {
      return new FeatureRunEvent(RunEventType.FEATURE_EXIT, fr, result, System.currentTimeMillis());
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
      if (this.type == RunEventType.FEATURE_EXIT && this.result != null) {
         return this.result.toJson();
      } else {
         Map<String, Object> map = new LinkedHashMap<>();
         if (this.source != null) {
            if (this.source.getFeature() != null) {
               Feature feature = this.source.getFeature();
               if (feature.getResource() != null) {
                  map.put("path", feature.getResource().getRelativePath());
               }

               map.put("slug", RunUtils.featureSlug(feature));
               map.put("name", feature.getName());
               map.put("description", feature.getDescription());
               map.put("tags", RunUtils.tagTexts(feature.getTags()));
               map.put("line", feature.getLine());
               map.put("sourceHash", feature.getSourceHash());
            }

            map.put("callDepth", this.source.getCallDepth());
         }

         return map;
      }
   }
}
