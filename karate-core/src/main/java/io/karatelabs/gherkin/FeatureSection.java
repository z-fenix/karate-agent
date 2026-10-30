package io.karatelabs.gherkin;

public class FeatureSection {
   private int index;
   private Scenario scenario;
   private ScenarioOutline scenarioOutline;

   public int getLine() {
      return this.scenarioOutline != null ? this.scenarioOutline.getLine() : this.scenario.getLine();
   }

   public int getIndex() {
      return this.index;
   }

   public void setIndex(int index) {
      this.index = index;
   }

   public boolean isOutline() {
      return this.scenarioOutline != null;
   }

   public Scenario getScenario() {
      return this.scenario;
   }

   public void setScenario(Scenario scenario) {
      this.scenario = scenario;
   }

   public ScenarioOutline getScenarioOutline() {
      return this.scenarioOutline;
   }

   public void setScenarioOutline(ScenarioOutline scenarioOutline) {
      this.scenarioOutline = scenarioOutline;
   }
}
