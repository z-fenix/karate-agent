package io.karatelabs.core;

import io.karatelabs.gherkin.Step;
import io.karatelabs.output.Console;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class StepResult {
   private final Step step;
   private final StepResult.Status status;
   private final long startTime;
   private final long durationNanos;
   private final Throwable error;
   private String log;
   private List<StepResult.Embed> embeds;
   private List<FeatureResult> callResults;
   private String hookName;
   private String syntheticText;

   private StepResult(Step step, StepResult.Status status, long startTime, long durationNanos, Throwable error) {
      this.step = step;
      this.status = status;
      this.startTime = startTime;
      this.durationNanos = durationNanos;
      this.error = error;
   }

   public static StepResult passed(Step step, long startTime, long durationNanos) {
      return new StepResult(step, StepResult.Status.PASSED, startTime, durationNanos, null);
   }

   public static StepResult failed(Step step, long startTime, long durationNanos, Throwable error) {
      return new StepResult(step, StepResult.Status.FAILED, startTime, durationNanos, error);
   }

   public static StepResult skipped(Step step, long startTime) {
      return new StepResult(step, StepResult.Status.SKIPPED, startTime, 0L, null);
   }

   public static StepResult fakeSuccess(String message, long startTime) {
      StepResult sr = new StepResult(null, StepResult.Status.PASSED, startTime, 0L, null);
      sr.log = message;
      return sr;
   }

   public static StepResult fakeFailure(String message, long startTime, Throwable error) {
      StepResult sr = new StepResult(null, StepResult.Status.FAILED, startTime, 0L, error);
      sr.log = message;
      return sr;
   }

   public static StepResult hook(String hookName, StepResult.Status status, long startTime, long durationNanos, Throwable error) {
      StepResult sr = new StepResult(null, status, startTime, durationNanos, error);
      sr.hookName = hookName;
      return sr;
   }

   public static StepResult synthetic(String text, StepResult.Status status, long startTime, Throwable error) {
      StepResult sr = new StepResult(null, status, startTime, 0L, error);
      sr.syntheticText = text;
      return sr;
   }

   public Step getStep() {
      return this.step;
   }

   public StepResult.Status getStatus() {
      return this.status;
   }

   public long getStartTime() {
      return this.startTime;
   }

   public long getDurationNanos() {
      return this.durationNanos;
   }

   public double getDurationMillis() {
      return this.durationNanos / 1000000.0;
   }

   public Throwable getError() {
      return this.error;
   }

   public String getLog() {
      return this.log;
   }

   public void setLog(String log) {
      this.log = log;
   }

   public String getSyntheticText() {
      return this.syntheticText;
   }

   public void appendLog(String message) {
      if (this.log != null && !this.log.isEmpty()) {
         this.log = this.log + message + "\n";
      } else {
         this.log = message + "\n";
      }
   }

   public List<StepResult.Embed> getEmbeds() {
      return this.embeds;
   }

   void releaseLogAndEmbeds() {
      this.log = null;
      this.embeds = null;
   }

   public void addEmbed(StepResult.Embed embed) {
      if (this.embeds == null) {
         this.embeds = new ArrayList<>();
      }

      this.embeds.add(embed);
   }

   public List<FeatureResult> getCallResults() {
      return this.callResults;
   }

   public void setCallResults(List<FeatureResult> callResults) {
      this.callResults = callResults;
   }

   public boolean hasCallResults() {
      return this.callResults != null && !this.callResults.isEmpty();
   }

   public String getHookName() {
      return this.hookName;
   }

   public boolean isHook() {
      return this.hookName != null;
   }

   public boolean isPassed() {
      return this.status == StepResult.Status.PASSED;
   }

   public boolean isFailed() {
      return this.status == StepResult.Status.FAILED;
   }

   public boolean isSkipped() {
      return this.status == StepResult.Status.SKIPPED;
   }

   public String getErrorMessage() {
      return this.error != null ? this.error.getMessage() : null;
   }

   public Map<String, Object> toJson() {
      Map<String, Object> map = new LinkedHashMap<>();
      if (this.step != null) {
         map.put("step", this.step.toJson());
      } else {
         Map<String, Object> fakeStep = new LinkedHashMap<>();
         fakeStep.put("index", -1);
         fakeStep.put("line", 0);
         fakeStep.put("prefix", "*");
         if (this.hookName != null) {
            fakeStep.put("text", this.hookName);
            fakeStep.put("hook", this.hookName);
         } else if (this.syntheticText != null) {
            fakeStep.put("text", this.syntheticText);
         } else {
            fakeStep.put("text", this.log != null ? this.log : "");
         }

         map.put("step", fakeStep);
      }

      Map<String, Object> resultMap = new LinkedHashMap<>();
      resultMap.put("status", this.status.name().toLowerCase());
      resultMap.put("millis", this.durationNanos / 1000000L);
      resultMap.put("nanos", this.durationNanos);
      resultMap.put("startTime", this.startTime);
      long endTime = this.startTime + this.durationNanos / 1000000L;
      resultMap.put("endTime", endTime);
      if (this.error != null) {
         resultMap.put("errorMessage", this.error.getMessage());
      }

      map.put("result", resultMap);
      if (this.log != null && !this.log.isEmpty()) {
         map.put("stepLog", Console.stripAnsi(this.log));
      }

      if (this.embeds != null && !this.embeds.isEmpty()) {
         List<Map<String, Object>> embedList = new ArrayList<>();

         for (StepResult.Embed embed : this.embeds) {
            embedList.add(embed.toMap());
         }

         map.put("embeds", embedList);
      }

      if (this.callResults != null && !this.callResults.isEmpty()) {
         List<Map<String, Object>> callResultsList = new ArrayList<>();

         for (FeatureResult fr : this.callResults) {
            callResultsList.add(fr.toJson());
         }

         map.put("callResults", callResultsList);
      }

      return map;
   }

   public static class Embed {
      private final String name;
      private final List<StepResult.Part> parts;
      private final Map<String, Object> meta;

      public Embed(byte[] data, String mimeType, String name) {
         this.name = name;
         this.parts = new ArrayList<>();
         this.parts.add(new StepResult.Part("primary", mimeType, data));
         this.meta = null;
      }

      public Embed(String name, List<StepResult.Part> parts, Map<String, Object> meta) {
         this.name = name;
         this.parts = (List<StepResult.Part>)(parts != null ? parts : new ArrayList<>());
         this.meta = meta;
      }

      private StepResult.Part primary() {
         return this.parts.isEmpty() ? null : this.parts.get(0);
      }

      public byte[] getData() {
         StepResult.Part p = this.primary();
         return p == null ? null : p.data;
      }

      public String getMimeType() {
         StepResult.Part p = this.primary();
         return p == null ? null : p.mime;
      }

      public String getName() {
         return this.name;
      }

      public void setFileName(String fileName) {
         StepResult.Part p = this.primary();
         if (p != null) {
            p.fileName = fileName;
         }
      }

      public String getFileName() {
         StepResult.Part p = this.primary();
         return p == null ? null : p.fileName;
      }

      public List<StepResult.Part> getParts() {
         return this.parts;
      }

      public Map<String, Object> getMeta() {
         return this.meta;
      }

      public Map<String, Object> toMap() {
         Map<String, Object> map = new LinkedHashMap<>();
         if (this.name != null) {
            map.put("name", this.name);
         }

         List<Map<String, Object>> partList = new ArrayList<>();

         for (StepResult.Part p : this.parts) {
            partList.add(p.toMap());
         }

         map.put("parts", partList);
         if (this.meta != null && !this.meta.isEmpty()) {
            map.put("meta", this.meta);
         }

         return map;
      }
   }

   public static class Part {
      private final String role;
      private final String mime;
      private final byte[] data;
      private final String url;
      private String fileName;

      public Part(String role, String mime, byte[] data) {
         this.role = role;
         this.mime = mime;
         this.data = data;
         this.url = null;
      }

      public Part(String role, String mime, String url) {
         this.role = role;
         this.mime = mime;
         this.url = url;
         this.data = null;
      }

      public String getRole() {
         return this.role;
      }

      public String getMime() {
         return this.mime;
      }

      public byte[] getData() {
         return this.data;
      }

      public String getUrl() {
         return this.url;
      }

      public void setFileName(String fileName) {
         this.fileName = fileName;
      }

      public String getFileName() {
         return this.fileName;
      }

      Map<String, Object> toMap() {
         Map<String, Object> map = new LinkedHashMap<>();
         map.put("role", this.role);
         map.put("mime", this.mime);
         if (this.fileName != null) {
            map.put("file", this.fileName);
         } else if (this.url != null) {
            map.put("url", this.url);
         } else if (this.data != null) {
            map.put("data", Base64.getEncoder().encodeToString(this.data));
         }

         return map;
      }
   }

   public static enum Status {
      PASSED,
      FAILED,
      SKIPPED;
   }
}
