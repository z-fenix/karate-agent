package io.karatelabs.ext.max.apitest;

import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

public class ScenarioDataTest {
   @Test
   public void scenarioAndStepSnapshotsCompareByValue() {
      FeatureSnapshot.StepSnapshot first = new FeatureSnapshot.StepSnapshot("Given", "get", "/pets", null, null, List.of());
      FeatureSnapshot.StepSnapshot copy = new FeatureSnapshot.StepSnapshot("Given", "get", "/pets", null, null, List.of());
      assertEquals(first, copy);
      assertEquals(first.hashCode(), copy.hashCode());
      FeatureSnapshot.ScenarioSnapshot scenario = new FeatureSnapshot.ScenarioSnapshot(false, List.of("smoke"), "Find pets", null, List.of(first), List.of());
      FeatureSnapshot.ScenarioSnapshot same = new FeatureSnapshot.ScenarioSnapshot(false, List.of("smoke"), "Find pets", null, List.of(copy), List.of());
      assertEquals(scenario, same);
      assertEquals(scenario.hashCode(), same.hashCode());
   }
}
