package io.karatelabs.core;

import io.karatelabs.gherkin.Feature;

public class FeatureCall {
   private final Feature feature;
   private final String tagSelector;

   public FeatureCall(Feature feature, String tagSelector) {
      this.feature = feature;
      this.tagSelector = tagSelector;
   }

   public Feature getFeature() {
      return this.feature;
   }

   public String getTagSelector() {
      return this.tagSelector;
   }

   public boolean isSameFile() {
      return this.feature == null;
   }

   @Override
   public String toString() {
      return this.feature == null ? this.tagSelector : this.feature.toString() + (this.tagSelector != null ? this.tagSelector : "");
   }
}
