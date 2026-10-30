package io.karatelabs.core;

import io.karatelabs.common.Resource;
import io.karatelabs.gherkin.Scenario;
import io.karatelabs.gherkin.Step;
import io.karatelabs.gherkin.Tag;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ScenarioResult implements Comparable<ScenarioResult> {
   public static final String EXPECT_TEST_TO_FAIL_BECAUSE_OF_FAIL_TAG = "Expect test to fail because of @fail tag";
   public static final String SUPPRESSED_FAILURE_MESSAGE = "output suppressed by @report=false (full detail in runtime logs)";
   private final Scenario scenario;
   private final List<StepResult> stepResults = new ArrayList<>();
   private long startTime;
   private long endTime;
   private String threadName;
   private boolean failTagApplied;
   private boolean aborted;
   private boolean reportDisabled;
   private String stableId;
   private int executionIndex;
   private boolean perfFailureReported;
   private static final int REASON_SUMMARY_MAX = 200;

   public ScenarioResult(Scenario scenario) {
      this.scenario = scenario;
   }

   public int getExecutionIndex() {
      return this.executionIndex;
   }

   public void setExecutionIndex(int executionIndex) {
      this.executionIndex = executionIndex;
   }

   public String getStableId() {
      return this.stableId;
   }

   public void setStableId(String stableId) {
      this.stableId = stableId;
   }

   public void setStartTime(long startTime) {
      this.startTime = startTime;
   }

   public void setEndTime(long endTime) {
      this.endTime = endTime;
   }

   public void setThreadName(String threadName) {
      this.threadName = threadName;
   }

   public boolean isAborted() {
      return this.aborted;
   }

   public void setAborted(boolean aborted) {
      this.aborted = aborted;
   }

   public boolean isReportDisabled() {
      return this.reportDisabled;
   }

   public void setReportDisabled(boolean reportDisabled) {
      this.reportDisabled = reportDisabled;
   }

   public Scenario getScenario() {
      return this.scenario;
   }

   public List<StepResult> getStepResults() {
      return this.stepResults;
   }

   public void releaseCallResults() {
      for (StepResult stepResult : this.stepResults) {
         stepResult.setCallResults(null);
      }
   }

   public void addStepResult(StepResult sr) {
      this.stepResults.add(sr);
   }

   public long getStartTime() {
      return this.startTime;
   }

   public long getEndTime() {
      return this.endTime;
   }

   public String getThreadName() {
      return this.threadName;
   }

   public boolean isPassed() {
      return this.failTagApplied && !this.stepResults.isEmpty()
         ? this.stepResults.getLast().isPassed()
         : this.stepResults.stream().noneMatch(StepResult::isFailed);
   }

   public boolean isFailed() {
      return this.failTagApplied && !this.stepResults.isEmpty()
         ? this.stepResults.getLast().isFailed()
         : this.stepResults.stream().anyMatch(StepResult::isFailed);
   }

   public boolean isSkipped() {
      if (this.failTagApplied || this.isFailed()) {
         return false;
      } else {
         return this.aborted ? true : this.stepResults.stream().noneMatch(s -> s.isPassed() || s.isFailed());
      }
   }

   public long getDurationMillis() {
      return this.endTime - this.startTime;
   }

   public long getDurationNanos() {
      return this.stepResults.stream().mapToLong(StepResult::getDurationNanos).sum();
   }

   private StepResult firstFailedStep() {
      return this.stepResults.stream().filter(StepResult::isFailed).findFirst().orElse(null);
   }

   public String getFailureMessage() {
      StepResult failedStep = this.firstFailedStep();
      return failedStep == null ? null : failedStep.getErrorMessage();
   }

   private String failedFeaturePath() {
      StepResult failedStep = this.firstFailedStep();
      if (failedStep != null && failedStep.getStep() != null) {
         Resource resource = this.scenario.getFeature().getResource();
         return resource.getPath() != null ? resource.getPath().toString() : resource.getRelativePath();
      } else {
         return null;
      }
   }

   public String getFailedStepLocation() {
      String featurePath = this.failedFeaturePath();
      return featurePath == null ? null : featurePath + ":" + this.firstFailedStep().getStep().getLine();
   }

   public int getFailedStepLine() {
      StepResult failedStep = this.firstFailedStep();
      return failedStep != null && failedStep.getStep() != null ? failedStep.getStep().getLine() : -1;
   }

   public String getFailedStepComment() {
      StepResult failedStep = this.firstFailedStep();
      if (failedStep != null && failedStep.getStep() != null) {
         List<String> comments = failedStep.getStep().getComments();
         return comments != null && !comments.isEmpty() ? comments.getLast() : null;
      } else {
         return null;
      }
   }

   public String getFailedStepText() {
      StepResult failedStep = this.firstFailedStep();
      if (failedStep == null) {
         return null;
      } else {
         Step step = failedStep.getStep();
         if (step != null && step.getText() != null) {
            StringBuilder sb = new StringBuilder();
            if (step.getPrefix() != null) {
               sb.append(step.getPrefix()).append(' ');
            }

            if (step.getKeyword() != null) {
               sb.append(step.getKeyword()).append(' ');
            }

            sb.append(step.getText());
            return sb.toString();
         } else {
            return null;
         }
      }
   }

   public String getFailedStepDocString() {
      StepResult failedStep = this.firstFailedStep();
      if (failedStep != null && failedStep.getStep() != null) {
         String docString = failedStep.getStep().getDocString();
         return docString == null ? null : "\"\"\"\n" + docString + "\n\"\"\"";
      } else {
         return null;
      }
   }

   public String getFailureMessageForDisplay() {
      String stepText = this.getFailedStepText();
      if (stepText == null) {
         return null;
      } else {
         String location = this.getFailedStepLocation();
         return location != null ? location + " " + stepText : stepText;
      }
   }

   public String getFailureMessageWithLocation() {
      String message = this.getFailureMessage();
      if (message == null) {
         return null;
      } else {
         String location = this.getFailedStepLocation();
         if (location == null) {
            return message;
         } else {
            StringBuilder sb = new StringBuilder(location);
            String stepText = this.getFailedStepText();
            if (stepText != null) {
               String comment = this.getFailedStepComment();
               if (comment != null) {
                  sb.append('\n').append(comment);
               }

               sb.append('\n').append(stepText);
               message = this.getFailureReason();
            }

            return sb.append('\n').append(message).toString();
         }
      }
   }

   public String getFailureReason() {
      String message = this.getFailureMessage();
      String comment = this.getFailedStepComment();
      return comment != null && message != null && message.startsWith(comment + "\n") ? message.substring(comment.length() + 1) : message;
   }

   public String getFailureReasonSummary() {
      String reason = this.getFailureReason();
      if (reason == null) {
         return null;
      } else {
         String firstLine = reason.lines().map(String::trim).filter(line -> !line.isEmpty()).findFirst().orElse(null);
         if ("js failed:".equals(firstLine)) {
            firstLine = reason.lines()
               .map(String::trim)
               .filter(line -> line.startsWith("Error: "))
               .findFirst()
               .map(line -> line.substring("Error: ".length()).trim())
               .orElse(firstLine);
         }

         if (firstLine == null) {
            return null;
         } else {
            return firstLine.length() > 200 ? firstLine.substring(0, 200) + "..." : firstLine;
         }
      }
   }

   public String getPerfFailureMessage() {
      String reason = this.getFailureReasonSummary();
      String display = this.getFailureMessageForDisplay();
      if (display == null) {
         return reason;
      } else {
         return reason == null ? display : display + " - " + reason;
      }
   }

   public void setPerfFailureReported(boolean perfFailureReported) {
      this.perfFailureReported = perfFailureReported;
   }

   public boolean isPerfFailureReported() {
      if (this.perfFailureReported) {
         return true;
      } else {
         StepResult failedStep = this.firstFailedStep();
         if (failedStep != null && failedStep.hasCallResults()) {
            for (FeatureResult called : failedStep.getCallResults()) {
               for (ScenarioResult sr : called.getScenarioResults()) {
                  if (sr.isFailed() && sr.isPerfFailureReported()) {
                     this.perfFailureReported = true;
                     return true;
                  }
               }
            }

            return false;
         } else {
            return false;
         }
      }
   }

   public Throwable getError() {
      StepResult failedStep = this.firstFailedStep();
      return failedStep == null ? null : failedStep.getError();
   }

   public Throwable getErrorWithLocation() {
      StepResult failedStep = this.firstFailedStep();
      if (failedStep == null) {
         return null;
      } else {
         Throwable error = failedStep.getError();
         String featurePath = this.failedFeaturePath();
         return (Throwable)(error != null && featurePath != null
            ? KarateException.forStep(error, featurePath, failedStep.getStep().getLine(), this.getFailedStepText())
            : error);
      }
   }

   public int getPassedCount() {
      return (int)this.stepResults.stream().filter(StepResult::isPassed).count();
   }

   public int getFailedCount() {
      return (int)this.stepResults.stream().filter(StepResult::isFailed).count();
   }

   public int getSkippedCount() {
      return (int)this.stepResults.stream().filter(StepResult::isSkipped).count();
   }

   public void applyFailTag() {
      boolean originallyFailed = this.stepResults.stream().anyMatch(StepResult::isFailed);
      if (originallyFailed) {
         this.stepResults.add(StepResult.fakeSuccess("Expect test to fail because of @fail tag", System.currentTimeMillis()));
      } else {
         this.stepResults
            .add(
               StepResult.fakeFailure(
                  "Expect test to fail because of @fail tag", System.currentTimeMillis(), new RuntimeException("Expect test to fail because of @fail tag")
               )
            );
      }

      this.failTagApplied = true;
   }

   public boolean isFailTagApplied() {
      return this.failTagApplied;
   }

   public Map<String, Object> toJson() {
      Map<String, Object> map = RunUtils.scenarioIdentity(this.scenario, this.stableId);
      map.put("passed", this.isPassed());
      map.put("failed", this.isFailed());
      map.put("skipped", this.isSkipped());
      map.put("durationMillis", this.getDurationMillis());
      if (this.isFailed()) {
         map.put("error", this.reportDisabled ? "output suppressed by @report=false (full detail in runtime logs)" : this.getFailureMessage());
      }

      if (this.reportDisabled) {
         map.put("reportDisabled", true);
      }

      if (this.executionIndex > 0) {
         map.put("executionIndex", this.executionIndex);
      }

      map.put("refId", this.scenario.getRefId());
      map.put("isOutlineExample", this.scenario.isOutlineExample());
      map.put("executorName", this.threadName);
      map.put("startTime", this.startTime);
      map.put("endTime", this.endTime);
      List<Tag> effectiveTags = this.scenario.getTagsEffective();
      List<String> tagNames = new ArrayList<>();
      if (effectiveTags != null) {
         for (Tag tag : effectiveTags) {
            tagNames.add(tag.getText());
         }
      }

      if (this.isSkipped() && !tagNames.contains("skipped")) {
         tagNames.add("skipped");
      }

      if (!tagNames.isEmpty()) {
         map.put("tags", tagNames);
      }

      List<Map<String, Object>> stepResultsList = new ArrayList<>();
      if (!this.reportDisabled) {
         for (StepResult sr : this.stepResults) {
            stepResultsList.add(sr.toJson());
         }
      }

      map.put("stepResults", stepResultsList);
      return map;
   }

   public int compareTo(ScenarioResult other) {
      if (other == null) {
         return 1;
      } else {
         int sectionCmp = Integer.compare(this.scenario.getSection().getIndex(), other.scenario.getSection().getIndex());
         if (sectionCmp != 0) {
            return sectionCmp;
         } else {
            int exampleCmp = Integer.compare(this.scenario.getExampleIndex(), other.scenario.getExampleIndex());
            return exampleCmp != 0 ? exampleCmp : Integer.compare(this.scenario.getLine(), other.scenario.getLine());
         }
      }
   }
}
