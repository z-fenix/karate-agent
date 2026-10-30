package io.karatelabs.core;

import io.karatelabs.gherkin.Feature;
import io.karatelabs.gherkin.Scenario;
import io.karatelabs.gherkin.Tag;
import io.karatelabs.output.Console;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public class FeatureResult {
   private static final DateTimeFormatter RESULT_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd hh:mm:ss a", Locale.US).withZone(ZoneId.systemDefault());
   private final Feature feature;
   private final List<ScenarioResult> scenarioResults = Collections.synchronizedList(new ArrayList<>());
   private int callDepth;
   private int loopIndex = -1;
   private Object callArg;
   private Map<String, Object> resultVariables;
   private long startTime;
   private long endTime;

   public FeatureResult(Feature feature) {
      this.feature = feature;
   }

   public static FeatureResult fromException(Feature feature, Throwable error, long startTime) {
      FeatureResult result = new FeatureResult(feature);
      result.setStartTime(startTime);
      result.setEndTime(System.currentTimeMillis());
      Scenario scenario = getFirstScenarioOrCreate(feature);
      ScenarioResult scenarioResult = new ScenarioResult(scenario);
      scenarioResult.setStartTime(startTime);
      scenarioResult.setEndTime(System.currentTimeMillis());
      scenarioResult.setThreadName(Thread.currentThread().getName());
      String errorMessage = "Feature execution failed: " + error.getMessage();
      scenarioResult.addStepResult(StepResult.fakeFailure(errorMessage, startTime, error));
      result.addScenarioResult(scenarioResult);
      return result;
   }

   private static Scenario getFirstScenarioOrCreate(Feature feature) {
      if (feature.getSections().isEmpty()) {
         return Scenario.createError(feature, "Feature execution failed", feature.getLine());
      } else {
         return feature.getSections().get(0).isOutline()
            ? feature.getSections().get(0).getScenarioOutline().toScenario(null, 0, feature.getLine(), null)
            : feature.getSections().get(0).getScenario();
      }
   }

   public Feature getFeature() {
      return this.feature;
   }

   public void setStartTime(long startTime) {
      this.startTime = startTime;
   }

   public void setEndTime(long endTime) {
      this.endTime = endTime;
   }

   public long getStartTime() {
      return this.startTime;
   }

   public long getEndTime() {
      return this.endTime;
   }

   public int getCallDepth() {
      return this.callDepth;
   }

   public void setCallDepth(int callDepth) {
      this.callDepth = callDepth;
   }

   public int getLoopIndex() {
      return this.loopIndex;
   }

   public void setLoopIndex(int loopIndex) {
      this.loopIndex = loopIndex;
   }

   public Object getCallArg() {
      return this.callArg;
   }

   public void setCallArg(Object callArg) {
      this.callArg = callArg;
   }

   public Map<String, Object> getResultVariables() {
      return this.resultVariables;
   }

   public void setResultVariables(Map<String, Object> resultVariables) {
      this.resultVariables = resultVariables;
   }

   public synchronized void addScenarioResult(ScenarioResult sr) {
      this.scenarioResults.add(sr);
   }

   public List<ScenarioResult> getScenarioResults() {
      return this.scenarioResults;
   }

   public void sortScenarioResults() {
      synchronized (this.scenarioResults) {
         Collections.sort(this.scenarioResults);
      }
   }

   public int getScenarioCount() {
      return this.scenarioResults.size();
   }

   public int getPassedCount() {
      return (int)this.scenarioResults.stream().filter(ScenarioResult::isPassed).count();
   }

   public int getFailedCount() {
      return (int)this.scenarioResults.stream().filter(ScenarioResult::isFailed).count();
   }

   public int getSkippedCount() {
      return (int)this.scenarioResults.stream().filter(ScenarioResult::isSkipped).count();
   }

   public Integer getPassedRate() {
      return passedRate(this.getPassedCount(), this.getFailedCount());
   }

   public static Integer passedRate(int passed, int failed) {
      int executed = passed + failed;
      if (executed == 0) {
         return null;
      } else {
         int rate = (int)Math.round(passed * 100.0 / executed);
         if (failed > 0) {
            rate = Math.min(rate, 99);
         }

         if (passed > 0) {
            rate = Math.max(rate, 1);
         }

         return rate;
      }
   }

   public boolean isPassed() {
      return this.scenarioResults.stream().noneMatch(ScenarioResult::isFailed);
   }

   public void releaseCallResults() {
      for (ScenarioResult scenarioResult : this.scenarioResults) {
         for (StepResult stepResult : scenarioResult.getStepResults()) {
            stepResult.setCallResults(null);
         }
      }
   }

   public void releaseStepLogs() {
      for (ScenarioResult scenarioResult : this.scenarioResults) {
         for (StepResult stepResult : scenarioResult.getStepResults()) {
            stepResult.releaseLogAndEmbeds();
            List<FeatureResult> callResults = stepResult.getCallResults();
            if (callResults != null) {
               for (FeatureResult called : callResults) {
                  called.releaseStepLogs();
               }
            }
         }
      }
   }

   public boolean isFailed() {
      return this.scenarioResults.stream().anyMatch(ScenarioResult::isFailed);
   }

   public boolean isEmpty() {
      return this.scenarioResults.isEmpty();
   }

   public long getDurationMillis() {
      return this.endTime - this.startTime;
   }

   public String getDisplayName() {
      return this.feature.getResource().getRelativePath();
   }

   public String getFailureMessage() {
      return this.firstFailedScenario().map(ScenarioResult::getFailureMessage).orElse(null);
   }

   public String getFailureMessageForDisplay() {
      return this.firstFailedScenario().map(ScenarioResult::getFailureMessageForDisplay).orElse(null);
   }

   public String getFailureReason() {
      return this.firstFailedScenario().map(ScenarioResult::getFailureReason).orElse(null);
   }

   public String getFailureReasonSummary() {
      return this.firstFailedScenario().map(ScenarioResult::getFailureReasonSummary).orElse(null);
   }

   public String getFailedStepComment() {
      return this.firstFailedScenario().map(ScenarioResult::getFailedStepComment).orElse(null);
   }

   public int getFailedStepLine() {
      return this.firstFailedScenario().map(ScenarioResult::getFailedStepLine).orElse(-1);
   }

   private Optional<ScenarioResult> firstFailedScenario() {
      return this.scenarioResults.stream().filter(ScenarioResult::isFailed).findFirst();
   }

   public Map<String, Object> toJson() {
      Map<String, Object> map = new LinkedHashMap<>();
      map.put("name", this.feature.getName());
      map.put("description", this.feature.getDescription());
      map.put("passed", this.isPassed());
      map.put("failed", this.isFailed());
      map.put("durationMillis", this.getDurationMillis());
      map.put("passedCount", this.getPassedCount());
      map.put("failedCount", this.getFailedCount());
      map.put("skippedCount", this.getSkippedCount());
      map.put("passedRate", this.getPassedRate());
      map.put("packageQualifiedName", this.feature.getResource().getPackageQualifiedName());
      map.put("relativePath", this.feature.getResource().getRelativePath());
      map.put("resultDate", RESULT_DATE_FORMAT.format(Instant.ofEpochMilli(this.endTime)));
      map.put("prefixedPath", this.feature.getResource().getPrefixedPath());
      map.put("sourceHash", this.feature.getSourceHash());
      map.put("startTime", this.startTime);
      map.put("endTime", this.endTime);
      map.put("loopIndex", this.loopIndex);
      map.put("callDepth", this.callDepth);
      if (this.callArg != null) {
         map.put("callArg", this.callArg);
      }

      map.put("line", this.feature.getLine());
      map.put("id", this.feature.getResource().getRelativePath().replace('/', '_').replace('.', '_'));
      List<Tag> tags = this.feature.getTags();
      if (tags != null && !tags.isEmpty()) {
         List<Map<String, Object>> tagList = new ArrayList<>();

         for (Tag tag : tags) {
            Map<String, Object> tagMap = new LinkedHashMap<>();
            tagMap.put("name", tag.toString());
            tagMap.put("line", this.feature.getLine());
            tagList.add(tagMap);
         }

         map.put("tags", tagList);
      }

      List<Map<String, Object>> scenarioResultsList = new ArrayList<>();

      for (ScenarioResult sr : this.scenarioResults) {
         scenarioResultsList.add(sr.toJson());
      }

      map.put("scenarioResults", scenarioResultsList);
      return map;
   }

   public void printSummary() {
      this.printSummary(false);
   }

   public void printSummary(boolean dryRun) {
      String path = this.getDisplayName();
      int passed = this.getPassedCount();
      int failed = this.getFailedCount();
      int skipped = this.getSkippedCount();
      int total = this.getScenarioCount();
      double secs = this.getDurationMillis() / 1000.0;
      String status = failed > 0 ? Console.red(failed + " failed") : (dryRun ? Console.yellow("parsed, not run") : Console.green("passed"));
      String featureLine = failed > 0 ? Console.red(path) : Console.green(path);
      Console.println(Console.grey("-".repeat(57)));
      Console.println("feature: " + featureLine);
      if (skipped > 0) {
         Console.println(String.format("scenarios: %2d | passed: %2d | skipped: %2d | %s | time: %.4f", total, passed, skipped, status, secs));
      } else {
         Console.println(String.format("scenarios: %2d | passed: %2d | %s | time: %.4f", total, passed, status, secs));
      }

      Console.println(Console.grey("=".repeat(57)));
   }
}
