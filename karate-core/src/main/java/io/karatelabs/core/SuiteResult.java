package io.karatelabs.core;

import io.karatelabs.common.Json;
import io.karatelabs.output.Console;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class SuiteResult {
   private final List<FeatureResult> featureResults = Collections.synchronizedList(new ArrayList<>());
   private long startTime;
   private long endTime;
   private Path reportDir;
   private boolean htmlReportEnabled;
   private boolean dryRun;

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

   public void setReportDir(Path reportDir) {
      this.reportDir = reportDir;
   }

   public Path getReportDir() {
      return this.reportDir;
   }

   public void setHtmlReportEnabled(boolean htmlReportEnabled) {
      this.htmlReportEnabled = htmlReportEnabled;
   }

   public void setDryRun(boolean dryRun) {
      this.dryRun = dryRun;
   }

   public boolean isDryRun() {
      return this.dryRun;
   }

   public boolean isHtmlReportEnabled() {
      return this.htmlReportEnabled;
   }

   public synchronized void addFeatureResult(FeatureResult fr) {
      this.featureResults.add(fr);
   }

   public List<FeatureResult> getFeatureResults() {
      return this.featureResults;
   }

   public int getFeatureCount() {
      return this.featureResults.size();
   }

   public int getFeaturePassedCount() {
      return (int)this.featureResults.stream().filter(FeatureResult::isPassed).count();
   }

   public int getFeatureFailedCount() {
      return (int)this.featureResults.stream().filter(FeatureResult::isFailed).count();
   }

   public int getScenarioCount() {
      return this.featureResults.stream().mapToInt(FeatureResult::getScenarioCount).sum();
   }

   public int getScenarioPassedCount() {
      return this.featureResults.stream().mapToInt(FeatureResult::getPassedCount).sum();
   }

   public int getScenarioFailedCount() {
      return this.featureResults.stream().mapToInt(FeatureResult::getFailedCount).sum();
   }

   public int getScenarioSkippedCount() {
      return this.featureResults.stream().mapToInt(FeatureResult::getSkippedCount).sum();
   }

   public Integer getScenarioPassedRate() {
      return FeatureResult.passedRate(this.getScenarioPassedCount(), this.getScenarioFailedCount());
   }

   public boolean isPassed() {
      return this.featureResults.stream().noneMatch(FeatureResult::isFailed);
   }

   public boolean isFailed() {
      return this.featureResults.stream().anyMatch(FeatureResult::isFailed);
   }

   public long getDurationMillis() {
      return this.endTime - this.startTime;
   }

   public Map<String, Object> toJson() {
      Map<String, Object> map = new LinkedHashMap<>();
      List<Map<String, Object>> features = new ArrayList<>();

      for (FeatureResult fr : this.featureResults) {
         features.add(fr.toJson());
      }

      map.put("features", features);
      map.put("summary", this.summaryJson());
      return map;
   }

   public Map<String, Object> summaryJson() {
      Map<String, Object> summary = new LinkedHashMap<>();
      summary.put("featureCount", this.getFeatureCount());
      summary.put("featuresPassed", this.getFeaturePassedCount());
      summary.put("featuresFailed", this.getFeatureFailedCount());
      summary.put("scenarioCount", this.getScenarioCount());
      summary.put("scenariosPassed", this.getScenarioPassedCount());
      summary.put("scenariosFailed", this.getScenarioFailedCount());
      summary.put("scenariosSkipped", this.getScenarioSkippedCount());
      summary.put("passedRate", this.getScenarioPassedRate());
      summary.put("startTime", this.startTime);
      summary.put("endTime", this.endTime);
      summary.put("durationMillis", this.getDurationMillis());
      summary.put("passed", !this.isFailed());
      return summary;
   }

   public String toJsonString() {
      return Json.stringifyStrict(this.toJson());
   }

   public String toJsonPretty() {
      return Json.of(this.toJson()).toStringPretty();
   }

   public void printSummary() {
      this.printSummary(null, 1);
   }

   public void printSummary(String env, int threadCount) {
      String reportUrl = null;
      if (this.htmlReportEnabled && this.reportDir != null) {
         Path reportFile = this.reportDir.resolve("karate-summary.html");
         reportUrl = reportFile.toUri().toString();
      }

      int finalLineLength = Math.max(60, reportUrl != null ? reportUrl.length() : 0);
      List<FeatureResult> failedFeatures = this.getFailedFeatures();
      if (!failedFeatures.isEmpty()) {
         Console.println();
         Console.println(Console.fail("failed features:"));

         for (FeatureResult fr : failedFeatures) {
            String path;
            if (fr.getFeature().getResource() != null && fr.getFeature().getResource().getPath() != null) {
               path = fr.getFeature().getResource().getPath().toString();
            } else {
               path = fr.getFeature().getName();
            }

            Console.println("  " + Console.red(path));

            for (ScenarioResult sr : fr.getScenarioResults()) {
               if (sr.isFailed()) {
                  String scenarioName = sr.getScenario().getName();
                  if (scenarioName == null || scenarioName.isEmpty()) {
                     scenarioName = "line " + sr.getScenario().getLine();
                  }

                  Console.println("    - " + scenarioName);
                  String stepLocation = sr.getFailedStepLocation();
                  if (stepLocation != null) {
                     Console.println("      " + stepLocation);
                  }

                  String comment = sr.getFailedStepComment();
                  if (comment != null) {
                     Console.println("      " + Console.grey(comment));
                  }

                  String stepText = sr.getFailedStepText();
                  if (stepText != null) {
                     Console.println("      " + Console.cyan(stepText));
                  }

                  String docString = sr.getFailedStepDocString();
                  if (docString != null) {
                     Console.println("      " + Console.cyan(docString.replace("\n", "\n      ")));
                  }

                  String failureMessage = sr.getFailureReason();
                  if (failureMessage != null) {
                     Console.println("      " + Console.yellow(failureMessage.replace("\n", "\n      ")));
                  }
               }
            }
         }
      }

      Console.println();
      Console.println(Console.cyan(Console.line()));
      double elapsedSecs = this.getDurationMillis() / 1000.0;
      double threadTimeSecs = this.getThreadTimeMillis() / 1000.0;
      double efficiency = threadCount > 0 && elapsedSecs > 0.0 ? threadTimeSecs / (elapsedSecs * threadCount) : 1.0;
      Console.println(String.format("elapsed: %6.2fs | threads: %3d | efficiency: %.2f", elapsedSecs, threadCount, efficiency));
      int featureTotal = this.getFeatureCount();
      int featurePassed = this.getFeaturePassedCount();
      int featureFailed = this.getFeatureFailedCount();
      String featureStatus = featureFailed > 0
         ? Console.fail(featureFailed + " failed")
         : (this.dryRun ? Console.warn("parsed only") : Console.pass("all passed"));
      Console.println(String.format("features: %4d | passed: %4d | %s", featureTotal, featurePassed, featureStatus));
      int scenarioTotal = this.getScenarioCount();
      int scenarioPassed = this.getScenarioPassedCount();
      int scenarioFailed = this.getScenarioFailedCount();
      int scenarioSkipped = this.getScenarioSkippedCount();
      String scenarioStatus = scenarioFailed > 0
         ? Console.fail(scenarioFailed + " failed")
         : (this.dryRun ? Console.warn("DRY RUN — nothing was executed") : Console.pass("all passed"));
      if (scenarioSkipped > 0) {
         Console.println(String.format("scenarios: %3d | passed: %4d | skipped: %4d | %s", scenarioTotal, scenarioPassed, scenarioSkipped, scenarioStatus));
      } else {
         Console.println(String.format("scenarios: %3d | passed: %4d | %s", scenarioTotal, scenarioPassed, scenarioStatus));
      }

      Console.println(Console.cyan("-".repeat(60)));
      StringBuilder footer = new StringBuilder("Karate " + Globals.KARATE_VERSION);
      if (env != null && !env.isEmpty()) {
         footer.append(" | env: ").append(Console.cyan(env));
      }

      if (reportUrl != null) {
         footer.append(" | HTML report:");
         Console.println(footer.toString());
         Console.println(reportUrl);
      } else {
         Console.println(footer.toString());
      }

      String endLine = this.isFailed() ? Console.fail("=".repeat(finalLineLength)) : Console.pass("=".repeat(finalLineLength));
      Console.println(endLine);
   }

   public long getThreadTimeMillis() {
      return this.featureResults.stream().mapToLong(FeatureResult::getDurationMillis).sum();
   }

   public List<FeatureResult> getFailedFeatures() {
      List<FeatureResult> failed = new ArrayList<>();

      for (FeatureResult fr : this.featureResults) {
         if (fr.isFailed()) {
            failed.add(fr);
         }
      }

      return failed;
   }

   public List<String> getErrors() {
      List<String> errors = new ArrayList<>();

      for (FeatureResult fr : this.featureResults) {
         for (ScenarioResult sr : fr.getScenarioResults()) {
            if (sr.isFailed()) {
               String message = sr.getFailureMessageWithLocation();
               if (message != null) {
                  errors.add(message);
               }
            }
         }
      }

      return errors;
   }
}
