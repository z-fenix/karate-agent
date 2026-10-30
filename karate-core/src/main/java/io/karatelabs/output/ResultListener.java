package io.karatelabs.output;

import io.karatelabs.core.FeatureResult;
import io.karatelabs.core.ScenarioResult;
import io.karatelabs.core.Suite;
import io.karatelabs.core.SuiteResult;
import io.karatelabs.gherkin.Feature;
import io.karatelabs.gherkin.Scenario;

public interface ResultListener {
   default void onSuiteStart(Suite suite) {
   }

   default void onSuiteEnd(SuiteResult result) {
   }

   default void onFeatureStart(Feature feature) {
   }

   default void onFeatureEnd(FeatureResult result) {
   }

   default void onScenarioStart(Scenario scenario) {
   }

   default void onScenarioEnd(ScenarioResult result) {
   }
}
