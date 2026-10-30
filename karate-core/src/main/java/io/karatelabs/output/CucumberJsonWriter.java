package io.karatelabs.output;

import io.karatelabs.common.Json;
import io.karatelabs.core.FeatureResult;
import io.karatelabs.core.ScenarioResult;
import io.karatelabs.core.StepResult;
import io.karatelabs.gherkin.Feature;
import io.karatelabs.gherkin.Scenario;
import io.karatelabs.gherkin.Step;
import io.karatelabs.gherkin.Table;
import io.karatelabs.gherkin.Tag;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class CucumberJsonWriter {
   private static final Logger logger = LoggerFactory.getLogger("karate.runtime");
   private static final DateTimeFormatter START_TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").withZone(ZoneOffset.UTC);

   private CucumberJsonWriter() {
   }

   public static String serializeFeature(FeatureResult result) {
      return featureToJson(result);
   }

   public static String fileNameFor(FeatureResult result) {
      return result.getFeature().getResource().getPackageQualifiedName() + ".json";
   }

   public static void writeSerialized(String fileName, String content, Path outputDir) {
      try {
         if (!Files.exists(outputDir)) {
            Files.createDirectories(outputDir);
         }

         Files.writeString(outputDir.resolve(fileName), content);
      } catch (Exception var4) {
         logger.warn("Failed to write json for {}: {}", fileName, var4.getMessage());
      }
   }

   public static void writeFeature(FeatureResult result, Path outputDir) {
      try {
         if (!Files.exists(outputDir)) {
            Files.createDirectories(outputDir);
         }

         String fileName = result.getFeature().getResource().getPackageQualifiedName() + ".json";
         Path jsonPath = outputDir.resolve(fileName);
         String json = featureToJson(result);
         Files.writeString(jsonPath, json);
         logger.debug("Cucumber JSON written: {}", jsonPath);
      } catch (Exception var5) {
         logger.warn("Failed to write Cucumber JSON for {}: {}", result.getDisplayName(), var5.getMessage());
      }
   }

   public static String featureToJson(FeatureResult result) {
      List<Map<String, Object>> features = Collections.singletonList(featureToMap(result));
      return Json.of(features).toStringPretty();
   }

   private static Map<String, Object> featureToMap(FeatureResult fr) {
      Map<String, Object> map = new LinkedHashMap<>();
      Feature feature = fr.getFeature();
      String id = toId(feature.getName());
      String uri = feature.getResource().getRelativePath();
      map.put("id", id);
      map.put("uri", uri);
      map.put("name", feature.getName() != null ? feature.getName() : "");
      map.put("description", feature.getDescription() != null ? feature.getDescription() : "");
      map.put("keyword", "Feature");
      map.put("line", feature.getLine());
      List<Tag> tags = feature.getTags();
      if (tags != null && !tags.isEmpty()) {
         map.put("tags", tagsToList(tags, feature.getLine()));
      }

      List<Map<String, Object>> elements = new ArrayList<>();

      for (ScenarioResult sr : fr.getScenarioResults()) {
         elements.add(scenarioToMap(sr, id));
      }

      map.put("elements", elements);
      return map;
   }

   private static Map<String, Object> scenarioToMap(ScenarioResult sr, String featureId) {
      Map<String, Object> map = new LinkedHashMap<>();
      Scenario scenario = sr.getScenario();
      String scenarioId = toId(scenario.getName());
      String fullId = featureId + ";" + scenarioId;
      long startTime = sr.getStartTime();
      if (startTime > 0L) {
         map.put("start_timestamp", START_TIMESTAMP_FORMAT.format(Instant.ofEpochMilli(startTime)));
      }

      map.put("id", fullId);
      map.put("name", scenario.getName() != null ? scenario.getName() : "");
      map.put("description", scenario.getDescription() != null ? scenario.getDescription() : "");
      map.put("keyword", scenario.isOutlineExample() ? "Scenario Outline" : "Scenario");
      map.put("line", scenario.getLine());
      map.put("type", "scenario");
      List<Tag> tags = scenario.getTags();
      if (tags != null && !tags.isEmpty()) {
         map.put("tags", tagsToList(tags, scenario.getLine()));
      }

      List<Map<String, Object>> steps = new ArrayList<>();
      if (sr.isReportDisabled()) {
         if (sr.isFailed()) {
            Map<String, Object> redacted = new LinkedHashMap<>();
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("status", "failed");
            result.put("error_message", "output suppressed by @report=false (full detail in runtime logs)");
            redacted.put("keyword", "* ");
            redacted.put("name", "output suppressed by @report=false (full detail in runtime logs)");
            redacted.put("line", scenario.getLine());
            redacted.put("result", result);
            steps.add(redacted);
         }
      } else {
         for (StepResult stepResult : sr.getStepResults()) {
            steps.add(stepToMap(stepResult));
            appendCallResultSteps(steps, stepResult, 1);
         }
      }

      map.put("steps", steps);
      return map;
   }

   private static void appendCallResultSteps(List<Map<String, Object>> steps, StepResult parent, int depth) {
      if (parent.hasCallResults()) {
         String marker = ">".repeat(depth) + " ";
         int parentLine = parent.getStep() != null ? parent.getStep().getLine() : 0;

         for (FeatureResult fr : parent.getCallResults()) {
            Map<String, Object> header = new LinkedHashMap<>();
            header.put("keyword", marker);
            header.put("name", callNameForReport(fr));
            header.put("line", parentLine);
            Map<String, Object> headerResult = new LinkedHashMap<>();
            headerResult.put("status", "passed");
            headerResult.put("duration", 0L);
            header.put("result", headerResult);
            steps.add(header);

            for (ScenarioResult childScenario : fr.getScenarioResults()) {
               for (StepResult childStep : childScenario.getStepResults()) {
                  Map<String, Object> childMap = stepToMap(childStep);
                  Object originalKeyword = childMap.get("keyword");
                  childMap.put("keyword", marker + (originalKeyword == null ? "" : originalKeyword));
                  steps.add(childMap);
                  appendCallResultSteps(steps, childStep, depth + 1);
               }
            }
         }
      }
   }

   private static String callNameForReport(FeatureResult fr) {
      String name = fr.getDisplayName();
      int loopIndex = fr.getLoopIndex();
      return loopIndex < 0 ? name : "[" + loopIndex + "] " + name;
   }

   private static Map<String, Object> stepToMap(StepResult sr) {
      Map<String, Object> map = new LinkedHashMap<>();
      Step step = sr.getStep();
      if (step == null) {
         map.put("keyword", "* ");
         String name;
         if (sr.isHook()) {
            name = sr.getHookName();
         } else if (sr.getSyntheticText() != null) {
            name = sr.getSyntheticText();
         } else if (sr.getLog() != null && !sr.getLog().isEmpty()) {
            name = Console.stripAnsi(sr.getLog()).trim();
         } else {
            name = "";
         }

         map.put("name", name);
         map.put("line", 0);
      } else {
         map.put("keyword", mapToCucumberKeyword(step.getPrefix()));
         map.put("name", buildStepName(step));
         map.put("line", step.getLine());
         if (step.getDocString() != null) {
            Map<String, Object> docString = new LinkedHashMap<>();
            docString.put("value", step.getDocString());
            docString.put("content_type", "");
            docString.put("line", step.getLine() + 1);
            map.put("doc_string", docString);
         }

         if (step.getTable() != null) {
            map.put("rows", tableToRows(step.getTable(), step.getLine()));
         }
      }

      Map<String, Object> result = new LinkedHashMap<>();
      result.put("status", mapStatus(sr.getStatus()));
      result.put("duration", sr.getDurationNanos());
      if (sr.isFailed() && sr.getError() != null) {
         result.put("error_message", sr.getError().getMessage());
      }

      map.put("result", result);
      List<Map<String, Object>> embeddings = new ArrayList<>();
      if (sr.getLog() != null && !sr.getLog().isEmpty()) {
         Map<String, Object> logEmbed = new LinkedHashMap<>();
         logEmbed.put("mime_type", "text/plain");
         logEmbed.put("data", Base64.getEncoder().encodeToString(Console.stripAnsi(sr.getLog()).getBytes()));
         embeddings.add(logEmbed);
      }

      if (sr.getEmbeds() != null) {
         for (StepResult.Embed embed : sr.getEmbeds()) {
            for (StepResult.Part part : embed.getParts()) {
               if (part.getData() != null) {
                  Map<String, Object> embedMap = new LinkedHashMap<>();
                  embedMap.put("mime_type", part.getMime());
                  embedMap.put("data", Base64.getEncoder().encodeToString(part.getData()));
                  if (embed.getName() != null) {
                     embedMap.put("name", embed.getName());
                  }

                  embeddings.add(embedMap);
               }
            }
         }
      }

      if (!embeddings.isEmpty()) {
         map.put("embeddings", embeddings);
      }

      return map;
   }

   private static List<Map<String, Object>> tagsToList(List<Tag> tags, int line) {
      List<Map<String, Object>> result = new ArrayList<>();

      for (Tag tag : tags) {
         Map<String, Object> tagMap = new LinkedHashMap<>();
         tagMap.put("name", tag.toString());
         tagMap.put("line", line);
         result.add(tagMap);
      }

      return result;
   }

   private static List<Map<String, Object>> tableToRows(Table table, int startLine) {
      List<Map<String, Object>> rows = new ArrayList<>();
      List<List<String>> data = table.getRows();
      int line = startLine + 1;

      for (List<String> row : data) {
         Map<String, Object> rowMap = new LinkedHashMap<>();
         rowMap.put("cells", row);
         rowMap.put("line", line++);
         rows.add(rowMap);
      }

      return rows;
   }

   private static String buildStepName(Step step) {
      String keyword = step.getKeyword();
      String text = step.getText();
      if (keyword != null && !keyword.isEmpty()) {
         return keyword + " " + (text != null ? text : "");
      } else {
         return text != null ? text : "";
      }
   }

   private static String mapToCucumberKeyword(String prefix) {
      if (prefix == null) {
         return "* ";
      } else {
         return switch (prefix) {
            case "Given" -> "Given ";
            case "When" -> "When ";
            case "Then" -> "Then ";
            case "And" -> "And ";
            case "But" -> "But ";
            default -> "* ";
         };
      }
   }

   private static String mapStatus(StepResult.Status status) {
      return switch (status) {
         case PASSED -> "passed";
         case FAILED -> "failed";
         case SKIPPED -> "skipped";
      };
   }

   private static String toId(String name) {
      return name != null && !name.isEmpty() ? name.toLowerCase().replaceAll("[^a-z0-9]+", "-").replaceAll("^-|-$", "") : "unknown";
   }
}
