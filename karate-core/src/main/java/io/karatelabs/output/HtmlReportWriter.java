package io.karatelabs.output;

import io.karatelabs.common.Json;
import io.karatelabs.common.ResourceType;
import io.karatelabs.core.FeatureResult;
import io.karatelabs.core.Globals;
import io.karatelabs.core.ReportAssets;
import io.karatelabs.core.ScenarioResult;
import io.karatelabs.core.StepResult;
import io.karatelabs.core.SuiteResult;
import io.karatelabs.gherkin.Tag;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Writer;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.UnaryOperator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class HtmlReportWriter {
   private static final Logger logger = LoggerFactory.getLogger("karate.runtime");
   private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());
   private static final String RESOURCE_ROOT = "io/karatelabs/output/";
   private static final String[] STATIC_RESOURCES = new String[]{
      "alpine.min.js", "prism.min.js", "prism-karate.css", "karate-report.css", "karate-report.js", "karate-logo.svg", "favicon.ico"
   };
   private static final String DATA_PLACEHOLDER = "/* KARATE_DATA */";
   private static final String ICONS_PLACEHOLDER = "<!-- KARATE_ICONS -->";
   private static final String EXTS_PLACEHOLDER = "<!-- KARATE_EXTS -->";
   private static final String NAV_PLACEHOLDER = "<!-- KARATE_NAV -->";
   private static final String CTA_PLACEHOLDER = "<!-- KARATE_CTA -->";
   private static volatile String iconsSprite;
   private static final AtomicInteger embedCounter = new AtomicInteger(0);
   private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss.SSS").withZone(ZoneId.systemDefault());

   private HtmlReportWriter() {
   }

   public static void resetEmbedCounter() {
      embedCounter.set(0);
   }

   public static void write(SuiteResult result, Path outputDir, String env) {
      try {
         Map<String, Object> suiteData = buildSuiteData(result, env);
         List<Map<String, Object>> features = buildFeaturesList(result);
         writeReports(suiteData, features, outputDir);
         logger.debug("HTML report written to: {}", outputDir.resolve("karate-summary.html"));
      } catch (Exception var5) {
         logger.warn("Failed to write HTML report: {}", var5.getMessage());
         if (logger.isDebugEnabled()) {
            logger.debug("HTML report error details", var5);
         }
      }
   }

   public static void writeFeatureHtml(FeatureResult result, Path outputDir) throws IOException {
      writeFeatureHtml(result, outputDir, Collections.emptyMap());
   }

   public static void writeFeatureHtml(FeatureResult result, Path outputDir, Map<String, ReportAssets> reportAssets) throws IOException {
      renderFeatureHtml(prepareFeatureData(result, outputDir), outputDir, reportAssets);
   }

   public static Map<String, Object> prepareFeatureData(FeatureResult result, Path outputDir) throws IOException {
      return prepareFeatureData(result, outputDir, null);
   }

   public static Map<String, Object> prepareFeatureData(FeatureResult result, Path outputDir, UnaryOperator<String> mask) throws IOException {
      Path featuresDir = outputDir.resolve("feature-html");
      Path embedsDir = outputDir.resolve("embeds");
      Files.createDirectories(featuresDir);
      writeEmbedFiles(result, embedsDir, mask);
      return buildFeatureData(result);
   }

   public static void renderFeatureHtml(Map<String, Object> featureData, Path outputDir, Map<String, ReportAssets> reportAssets) throws IOException {
      renderFeatureHtml(featureData, outputDir, reportAssets, null);
   }

   public static void renderFeatureHtml(Map<String, Object> featureData, Path outputDir, Map<String, ReportAssets> reportAssets, UnaryOperator<String> mask) throws IOException {
      Path featuresDir = outputDir.resolve("feature-html");
      Files.createDirectories(featuresDir);
      String template = loadTemplate("karate-feature.html");
      String fileName = featureFileName(featureData) + ".html";
      writeInlined(featuresDir.resolve(fileName), template, featureData, reportAssets, "../", mask);
   }

   private static boolean hasEmbeds(FeatureResult result) {
      for (ScenarioResult sr : result.getScenarioResults()) {
         if (!sr.isReportDisabled()) {
            for (StepResult step : sr.getStepResults()) {
               if (step.getEmbeds() != null && !step.getEmbeds().isEmpty()) {
                  return true;
               }

               if (step.hasCallResults()) {
                  for (FeatureResult fr : step.getCallResults()) {
                     if (hasEmbeds(fr)) {
                        return true;
                     }
                  }
               }
            }
         }
      }

      return false;
   }

   private static void writeEmbedFile(StepResult.Embed embed, Path embedsDir, UnaryOperator<String> mask) throws IOException {
      String baseName = embed.getName() != null ? embed.getName().replaceAll("[^a-zA-Z0-9_-]", "_") : "embed";
      List<StepResult.Part> parts = embed.getParts();
      boolean multi = parts.size() > 1;

      for (StepResult.Part part : parts) {
         if (part.getFileName() == null && part.getData() != null && !isJsonMime(part.getMime())) {
            ResourceType rt = ResourceType.fromContentType(part.getMime());
            String ext = rt != null && rt.getExtension() != null ? rt.getExtension() : "bin";
            String suffix = multi && part.getRole() != null ? "_" + part.getRole() : "";
            String fileName = String.format("%03d_%s%s.%s", embedCounter.incrementAndGet(), baseName, suffix, ext);
            byte[] bytes = part.getData();
            if (mask != null && rt != null && !rt.isBinary()) {
               bytes = maskText(bytes, mask);
            }

            Files.write(embedsDir.resolve(fileName), bytes);
            part.setFileName(fileName);
         }
      }
   }

   private static byte[] maskText(byte[] bytes, UnaryOperator<String> mask) {
      String text;
      try {
         text = StandardCharsets.UTF_8.newDecoder().decode(ByteBuffer.wrap(bytes)).toString();
      } catch (CharacterCodingException var4) {
         return bytes;
      }

      String masked = mask.apply(text);
      return masked.equals(text) ? bytes : masked.getBytes(StandardCharsets.UTF_8);
   }

   public static void externalizeEmbeds(FeatureResult result, Path outputDir, UnaryOperator<String> mask) {
      try {
         writeEmbedFiles(result, outputDir.resolve("embeds"), mask);
      } catch (IOException var4) {
         logger.warn("Failed to externalize embeds: {}", var4.getMessage());
      }
   }

   private static void writeEmbedFiles(FeatureResult result, Path embedsDir, UnaryOperator<String> mask) throws IOException {
      if (hasEmbeds(result)) {
         Files.createDirectories(embedsDir);

         for (ScenarioResult sr : result.getScenarioResults()) {
            if (!sr.isReportDisabled()) {
               for (StepResult step : sr.getStepResults()) {
                  if (step.hasCallResults()) {
                     for (FeatureResult fr : step.getCallResults()) {
                        writeEmbedFiles(fr, embedsDir, mask);
                     }
                  }

                  if (step.getEmbeds() != null) {
                     for (StepResult.Embed embed : step.getEmbeds()) {
                        writeEmbedFile(embed, embedsDir, mask);
                     }
                  }
               }
            }
         }
      }
   }

   private static boolean isJsonMime(String mimeType) {
      return mimeType != null && mimeType.toLowerCase().contains("json");
   }

   public static void writeSummaryPages(List<Map<String, Object>> features, SuiteResult result, Path outputDir, String env) throws IOException {
      writeSummaryPages(features, result, outputDir, env, Collections.emptyMap());
   }

   public static void writeSummaryPages(
      List<Map<String, Object>> features, SuiteResult result, Path outputDir, String env, Map<String, ReportAssets> reportAssets
   ) throws IOException {
      writeSummaryPages(features, result, outputDir, env, reportAssets, Collections.emptyList());
   }

   public static void writeSummaryPages(
      List<Map<String, Object>> features,
      SuiteResult result,
      Path outputDir,
      String env,
      Map<String, ReportAssets> reportAssets,
      List<Map<String, Object>> summaryCards
   ) throws IOException {
      writeSummaryPages(features, result, outputDir, env, reportAssets, summaryCards, null);
   }

   public static void writeSummaryPages(
      List<Map<String, Object>> features,
      SuiteResult result,
      Path outputDir,
      String env,
      Map<String, ReportAssets> reportAssets,
      List<Map<String, Object>> summaryCards,
      UnaryOperator<String> mask
   ) throws IOException {
      copyExtAssets(reportAssets, outputDir);
      Map<String, Object> suiteData = new LinkedHashMap<>();
      suiteData.put("env", env);
      suiteData.put("reportDate", DATE_FORMAT.format(Instant.ofEpochMilli(result.getStartTime())));
      suiteData.put("karateVersion", Globals.KARATE_VERSION);
      Map<String, Object> summary = new LinkedHashMap<>();
      summary.put("feature_count", result.getFeatureCount());
      summary.put("feature_passed", result.getFeaturePassedCount());
      summary.put("feature_failed", result.getFeatureFailedCount());
      summary.put("scenario_count", result.getScenarioCount());
      summary.put("scenario_passed", result.getScenarioPassedCount());
      summary.put("scenario_failed", result.getScenarioFailedCount());
      summary.put("scenario_skipped", result.getScenarioSkippedCount());
      summary.put("duration_millis", result.getDurationMillis());
      summary.put("status", result.isFailed() ? "failed" : "passed");
      suiteData.put("summary", summary);
      List<Map<String, Object>> featureSummaryList = buildFeatureSummaryList(features);
      Map<String, Object> summaryPageData = new LinkedHashMap<>(suiteData);
      summaryPageData.put("features", featureSummaryList);
      if (summaryCards != null && !summaryCards.isEmpty()) {
         summaryPageData.put("summaryCards", summaryCards);
      }

      String summaryTemplate = loadTemplate("karate-summary.html");
      writeInlined(outputDir.resolve("karate-summary.html"), summaryTemplate, summaryPageData, reportAssets, "", mask);
      writeIndexRedirect(outputDir);
   }

   private static void writeReports(Map<String, Object> suiteData, List<Map<String, Object>> features, Path outputDir) throws IOException {
      Path featuresDir = outputDir.resolve("feature-html");
      Path resDir = outputDir.resolve("res");
      Files.createDirectories(featuresDir);
      Files.createDirectories(resDir);
      copyStaticResources(resDir);
      writeSummaryHtml(suiteData, features, outputDir);

      for (Map<String, Object> feature : features) {
         writeFeatureHtml(feature, featuresDir);
      }

      writeIndexRedirect(outputDir);
   }

   private static void writeSummaryHtml(Map<String, Object> suiteData, List<Map<String, Object>> features, Path outputDir) throws IOException {
      Map<String, Object> pageData = new LinkedHashMap<>(suiteData);
      pageData.put("features", buildFeatureSummaryList(features));
      String template = loadTemplate("karate-summary.html");
      writeInlined(outputDir.resolve("karate-summary.html"), template, pageData);
   }

   private static void writeFeatureHtml(Map<String, Object> feature, Path featuresDir) throws IOException {
      String template = loadTemplate("karate-feature.html");
      String fileName = featureFileName(feature) + ".html";
      writeInlined(featuresDir.resolve(fileName), template, feature);
   }

   private static void writeIndexRedirect(Path outputDir) throws IOException {
      String html = "<!DOCTYPE html>\n<html>\n<head>\n    <meta http-equiv=\"refresh\" content=\"0; url=karate-summary.html\">\n</head>\n<body>\n    <a href=\"karate-summary.html\">Redirecting to Karate Summary Report...</a>\n</body>\n</html>\n";
      Files.writeString(outputDir.resolve("index.html"), html);
   }

   private static void writeInlined(Path target, String template, Object data) throws IOException {
      writeInlined(target, template, data, null, "", null);
   }

   private static void writeInlined(
      Path target, String template, Object data, Map<String, ReportAssets> reportAssets, String relPrefix, UnaryOperator<String> mask
   ) throws IOException {
      String shell = template.replace("<!-- KARATE_ICONS -->", loadIconsSprite())
         .replace("<!-- KARATE_EXTS -->", buildExtsHtml(reportAssets, relPrefix))
         .replace("<!-- KARATE_NAV -->", buildNavHtml(reportAssets, relPrefix))
         .replace("<!-- KARATE_CTA -->", buildCtaHtml(reportAssets));
      int at = shell.indexOf("/* KARATE_DATA */");

      try (Writer writer = Files.newBufferedWriter(target, StandardCharsets.UTF_8)) {
         if (at < 0) {
            writer.write(shell);
            return;
         }

         writer.write(shell, 0, at);
         String json = Json.of(data).toString(false, false, false);
         writeScriptSafe(writer, mask == null ? json : mask.apply(json));
         int rest = at + "/* KARATE_DATA */".length();
         writer.write(shell, rest, shell.length() - rest);
      }
   }

   private static void writeScriptSafe(Writer writer, String json) throws IOException {
      int from = 0;

      int at;
      while ((at = json.indexOf("</", from)) >= 0) {
         writer.write(json, from, at - from);
         writer.write("<\\/");
         from = at + 2;
      }

      writer.write(json, from, json.length() - from);
   }

   private static String buildCtaHtml(Map<String, ReportAssets> reportAssets) {
      if (reportAssets != null) {
         for (ReportAssets a : reportAssets.values()) {
            if (a.ctaHref() != null && a.ctaLabel() != null) {
               String label = escapeHtml(a.ctaLabel());
               String href = escapeHtml(a.ctaHref());
               return "<a href=\""
                  + href
                  + "\" target=\"_blank\" rel=\"noopener\" title=\""
                  + label
                  + "\" class=\"inline-flex items-center gap-1.5 px-2.5 py-1 rounded text-xs font-medium border border-slate-300 dark:border-white/20 text-slate-500 dark:text-white/60 hover:text-slate-800 dark:hover:text-white hover:border-slate-400 dark:hover:border-white/40 transition-colors\"><svg class=\"w-4 h-4\"><use href=\"#icon-sparkle\"/></svg><span>"
                  + label
                  + "</span></a>";
            }
         }
      }

      return "<a href=\"https://karatelabs.io/karate-enterprise?utm_source=karate-os&utm_medium=report&utm_campaign=enterprise-cta\" target=\"_blank\" rel=\"noopener\" class=\"group inline-flex items-center gap-1.5 px-2.5 py-1 rounded text-xs font-medium bg-accent text-white hover:bg-accent-dark transition-colors\" title=\"Karate Open Source vs Karate Enterprise — see what's included\"><svg class=\"w-4 h-4\"><use href=\"#icon-sparkle\"/></svg><span><span class=\"hidden md:inline\">Try </span>Karate Enterprise</span><svg class=\"w-3 h-3 transition-transform group-hover:translate-x-0.5\"><use href=\"#icon-arrow-right\"/></svg></a>";
   }

   private static String buildExtsHtml(Map<String, ReportAssets> reportAssets, String relPrefix) {
      if (reportAssets != null && !reportAssets.isEmpty()) {
         StringBuilder sb = new StringBuilder();

         for (ReportAssets assets : reportAssets.values()) {
            String base = relPrefix + "ext/" + assets.name() + "/";
            String cssHref = assets.cssHref();
            if (cssHref != null) {
               sb.append("<link rel=\"stylesheet\" href=\"").append(base).append(cssHref).append("\">\n");
            }

            sb.append("<script src=\"").append(base).append(assets.jsHref()).append("\" defer></script>\n");
         }

         return sb.toString();
      } else {
         return "";
      }
   }

   private static String buildNavHtml(Map<String, ReportAssets> reportAssets, String relPrefix) {
      if (reportAssets != null && !reportAssets.isEmpty()) {
         StringBuilder sb = new StringBuilder();

         for (ReportAssets assets : reportAssets.values()) {
            for (ReportAssets.Page page : assets.pages()) {
               if ("nav.pages".equals(page.slot()) && page.href() != null) {
                  String href = relPrefix + "ext/" + assets.name() + "/" + page.href();
                  sb.append("<a class=\"text-sm text-slate-600 hover:text-slate-900 dark:text-white/80 dark:hover:text-white\" href=\"")
                     .append(escapeHtml(href))
                     .append("\">")
                     .append(escapeHtml(page.title()))
                     .append("</a>\n");
               }
            }
         }

         return sb.toString();
      } else {
         return "";
      }
   }

   private static String escapeHtml(String s) {
      return s == null ? "" : s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
   }

   private static void copyExtAssets(Map<String, ReportAssets> reportAssets, Path outputDir) {
      if (reportAssets != null && !reportAssets.isEmpty()) {
         for (ReportAssets assets : reportAssets.values()) {
            try {
               assets.copyTo(outputDir.resolve("ext").resolve(assets.name()));
            } catch (Exception var5) {
               logger.warn("Failed to copy ext assets for '{}': {}", assets.name(), var5.getMessage());
            }
         }
      }
   }

   private static String loadTemplate(String name) throws IOException {
      return loadClasspathResource("io/karatelabs/output/" + name, true);
   }

   private static String loadClasspathResource(String path, boolean required) throws IOException {
      String var6;
      try (InputStream is = HtmlReportWriter.class.getClassLoader().getResourceAsStream(path)) {
         if (is == null) {
            if (required) {
               throw new IOException("Resource not found: " + path);
            }

            return "";
         }

         try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();

            String line;
            while ((line = reader.readLine()) != null) {
               sb.append(line).append("\n");
            }

            var6 = sb.toString();
         }
      }

      return var6;
   }

   private static String loadIconsSprite() {
      String cached = iconsSprite;
      if (cached != null) {
         return cached;
      } else {
         try {
            cached = loadClasspathResource("io/karatelabs/output/_icons.svg", false);
         } catch (IOException var2) {
            logger.warn("Failed to load icons sprite: {}", var2.getMessage());
            cached = "";
         }

         iconsSprite = cached;
         return cached;
      }
   }

   public static void copyStaticResources(Path resDir) throws IOException {
      for (String resourceName : STATIC_RESOURCES) {
         String resourcePath = "io/karatelabs/output/res/" + resourceName;

         try (InputStream is = HtmlReportWriter.class.getClassLoader().getResourceAsStream(resourcePath)) {
            if (is != null) {
               Files.copy(is, resDir.resolve(resourceName), StandardCopyOption.REPLACE_EXISTING);
            } else {
               logger.debug("Static resource not found: {}", resourcePath);
            }
         }
      }
   }

   private static Map<String, Object> buildSuiteData(SuiteResult result, String env) {
      Map<String, Object> data = new LinkedHashMap<>();
      data.put("env", env);
      data.put("reportDate", DATE_FORMAT.format(Instant.ofEpochMilli(result.getStartTime())));
      data.put("karateVersion", Globals.KARATE_VERSION);
      Map<String, Object> summary = new LinkedHashMap<>();
      summary.put("feature_count", result.getFeatureCount());
      summary.put("feature_passed", result.getFeaturePassedCount());
      summary.put("feature_failed", result.getFeatureFailedCount());
      summary.put("scenario_count", result.getScenarioCount());
      summary.put("scenario_passed", result.getScenarioPassedCount());
      summary.put("scenario_failed", result.getScenarioFailedCount());
      summary.put("duration_millis", result.getDurationMillis());
      summary.put("status", result.isFailed() ? "failed" : "passed");
      data.put("summary", summary);
      return data;
   }

   private static List<Map<String, Object>> buildFeaturesList(SuiteResult result) {
      List<Map<String, Object>> features = new ArrayList<>();

      for (FeatureResult fr : result.getFeatureResults()) {
         features.add(buildFeatureData(fr));
      }

      return features;
   }

   private static Map<String, Object> buildFeatureData(FeatureResult fr) {
      Map<String, Object> data = new LinkedHashMap<>();
      data.put("path", fr.getDisplayName());
      data.put("name", fr.getFeature().getName());
      data.put("relativePath", fr.getFeature().getResource().getRelativePath());
      data.put("passed", fr.isPassed());
      data.put("durationMillis", fr.getDurationMillis());
      long startTime = fr.getStartTime();
      if (startTime == 0L) {
         for (ScenarioResult sr : fr.getScenarioResults()) {
            long st = sr.getStartTime();
            if (st > 0L && (startTime == 0L || st < startTime)) {
               startTime = st;
            }
         }
      }

      if (startTime > 0L) {
         data.put("reportDate", DATE_FORMAT.format(Instant.ofEpochMilli(startTime)));
      }

      data.put("karateVersion", Globals.KARATE_VERSION);
      List<Map<String, Object>> scenarios = new ArrayList<>();

      for (ScenarioResult srx : fr.getScenarioResults()) {
         scenarios.add(buildScenarioData(srx));
      }

      data.put("scenarios", scenarios);
      return data;
   }

   private static Map<String, Object> buildScenarioData(ScenarioResult sr) {
      Map<String, Object> data = new LinkedHashMap<>();
      data.put("name", sr.getScenario().getName());
      data.put("line", sr.getScenario().getLine());
      data.put("passed", sr.isPassed());
      data.put("durationMillis", sr.getDurationMillis());
      data.put("startTime", sr.getStartTime());
      data.put("endTime", sr.getEndTime());
      data.put("refId", sr.getScenario().getRefId());
      data.put("sectionIndex", sr.getScenario().getSection().getIndex() + 1);
      data.put("exampleIndex", sr.getScenario().getExampleIndex());
      data.put("isOutlineExample", sr.getScenario().isOutlineExample());
      if (sr.getThreadName() != null) {
         data.put("thread", sr.getThreadName());
      }

      List<String> tagNames = new ArrayList<>();

      for (Tag tag : sr.getScenario().getTagsEffective()) {
         tagNames.add(tag.toString());
      }

      if (sr.isSkipped() && !tagNames.contains("@skipped")) {
         tagNames.add("@skipped");
      }

      if (!tagNames.isEmpty()) {
         data.put("tags", tagNames);
      }

      data.put("skipped", sr.isSkipped());
      if (sr.isReportDisabled()) {
         data.put("reportDisabled", true);
         data.put("steps", new ArrayList());
         if (sr.isFailed()) {
            data.put("error", "output suppressed by @report=false (full detail in runtime logs)");
         }

         return data;
      } else {
         List<Map<String, Object>> steps = new ArrayList<>();

         for (StepResult step : sr.getStepResults()) {
            steps.add(buildStepData(step));
         }

         data.put("steps", steps);
         if (sr.isFailed() && sr.getFailureMessage() != null) {
            data.put("error", sr.getFailureMessage());
         }

         return data;
      }
   }

   private static Map<String, Object> buildStepData(StepResult step) {
      Map<String, Object> data = new LinkedHashMap<>();
      if (step.getStep() != null) {
         data.put("prefix", step.getStep().getPrefix());
         data.put("keyword", step.getStep().getKeyword());
         data.put("text", step.getStep().getText());
         data.put("line", step.getStep().getLine());
         List<String> comments = step.getStep().getComments();
         if (comments != null && !comments.isEmpty()) {
            data.put("comments", comments);
         }
      } else {
         data.put("prefix", "*");
         if (step.isHook()) {
            data.put("keyword", "");
            data.put("text", step.getHookName());
            data.put("hook", step.getHookName());
         } else if (step.getSyntheticText() != null) {
            data.put("keyword", "");
            data.put("text", step.getSyntheticText());
         } else {
            data.put("keyword", "*");
            data.put("text", step.getLog() != null ? Console.stripAnsi(step.getLog()) : "");
         }

         data.put("line", 0);
      }

      data.put("status", step.getStatus().name().toLowerCase());
      data.put("durationMillis", step.getDurationNanos() / 1000000L);
      boolean hasLogs = step.getLog() != null && !step.getLog().isEmpty();
      data.put("hasLogs", hasLogs);
      if (hasLogs) {
         data.put("logSegments", Console.splitLog(step.getLog()));
      }

      if (step.getError() != null) {
         data.put("error", step.getError().getMessage());
      }

      boolean hasEmbeds = step.getEmbeds() != null && !step.getEmbeds().isEmpty();
      data.put("hasEmbeds", hasEmbeds);
      if (hasEmbeds) {
         List<Map<String, Object>> embedList = new ArrayList<>();

         for (StepResult.Embed embed : step.getEmbeds()) {
            embedList.add(embed.toMap());
         }

         data.put("embeds", embedList);
      }

      boolean hasCallResults = step.hasCallResults();
      data.put("hasCallResults", hasCallResults);
      if (hasCallResults) {
         List<Map<String, Object>> callList = new ArrayList<>();

         for (FeatureResult fr : step.getCallResults()) {
            callList.add(buildFeatureData(fr));
         }

         data.put("callResults", callList);
      }

      return data;
   }

   private static List<Map<String, Object>> buildFeatureSummaryList(List<Map<String, Object>> features) {
      List<Map<String, Object>> summaryList = new ArrayList<>();

      for (Map<String, Object> feature : features) {
         Map<String, Object> summary = new LinkedHashMap<>();
         summary.put("name", feature.get("name"));
         summary.put("relativePath", feature.get("relativePath"));
         summary.put("fileName", featureFileName(feature));
         summary.put("passed", Boolean.TRUE.equals(feature.get("passed")));
         summary.put("failed", Boolean.TRUE.equals(feature.get("failed")));
         Object duration = feature.get("durationMillis");
         summary.put("durationMillis", duration instanceof Number ? ((Number)duration).longValue() : 0L);
         List<Map<String, Object>> scenarioResults = (List<Map<String, Object>>)feature.get("scenarioResults");
         int total = scenarioResults != null ? scenarioResults.size() : 0;
         int passed = 0;
         int failed = 0;
         int skipped = 0;
         List<Map<String, Object>> scenarioSummaries = new ArrayList<>();
         if (scenarioResults != null) {
            for (Map<String, Object> s : scenarioResults) {
               boolean scenarioPassed = Boolean.TRUE.equals(s.get("passed"));
               boolean scenarioSkipped = Boolean.TRUE.equals(s.get("skipped"));
               if (scenarioPassed) {
                  passed++;
               } else {
                  failed++;
               }

               if (scenarioSkipped) {
                  skipped++;
               }

               Map<String, Object> scenarioSummary = new LinkedHashMap<>();
               scenarioSummary.put("name", s.get("name"));
               scenarioSummary.put("refId", s.get("refId"));
               scenarioSummary.put("passed", scenarioPassed);
               scenarioSummary.put("skipped", scenarioSkipped);
               scenarioSummary.put("durationMillis", s.get("durationMillis"));
               Object rawTags = s.get("tags");
               if (!(rawTags instanceof List<?> tagList)) {
                  scenarioSummary.put("tags", rawTags);
               } else {
                  List<String> displayTags = new ArrayList<>(tagList.size());

                  for (Object t : tagList) {
                     String text = String.valueOf(t);
                     displayTags.add(text.startsWith("@") ? text : "@" + text);
                  }

                  scenarioSummary.put("tags", displayTags);
               }

               scenarioSummaries.add(scenarioSummary);
            }
         }

         summary.put("scenarioCount", total);
         summary.put("passedCount", passed);
         summary.put("failedCount", failed);
         summary.put("skippedCount", skipped);
         summary.put("passedRate", FeatureResult.passedRate(passed, failed));
         summary.put("scenarios", scenarioSummaries);
         summaryList.add(summary);
      }

      return summaryList;
   }

   static String featureFileName(Map<String, Object> feature) {
      String path = (String)feature.get("relativePath");
      if (path == null || path.isEmpty()) {
         path = (String)feature.get("name");
      }

      if (path == null || path.isEmpty()) {
         path = "unknown";
      }
      String logicalPath = path.toLowerCase(Locale.ROOT).endsWith(".feature")
         ? path.substring(0, path.length() - ".feature".length())
         : path;
      String slug = logicalPath.replace("/", ".").replace("\\", ".")
         .replaceAll("[^a-zA-Z0-9_.-]", "_").toLowerCase(Locale.ROOT);
      // Sanitizing non-ASCII path segments can collapse distinct paths to the same name.
      // Keep a stable fingerprint of the original path so every feature owns one HTML file.
      String fingerprint = UUID.nameUUIDFromBytes(path.getBytes(StandardCharsets.UTF_8))
         .toString().replace("-", "").substring(0, 10);
      return slug + "-" + fingerprint;
   }

   public static void writeTimelineHtml(List<Map<String, Object>> features, SuiteResult result, Path outputDir, String env, int threadCount) throws IOException {
      writeTimelineHtml(features, result, outputDir, env, threadCount, Collections.emptyMap());
   }

   public static void writeTimelineHtml(
      List<Map<String, Object>> features, SuiteResult result, Path outputDir, String env, int threadCount, Map<String, ReportAssets> reportAssets
   ) throws IOException {
      writeTimelineHtml(features, result, outputDir, env, threadCount, reportAssets, null);
   }

   public static void writeTimelineHtml(
      List<Map<String, Object>> features,
      SuiteResult result,
      Path outputDir,
      String env,
      int threadCount,
      Map<String, ReportAssets> reportAssets,
      UnaryOperator<String> mask
   ) throws IOException {
      Map<String, Object> timelineData = buildTimelineData(features, result, env, threadCount);
      String template = loadTemplate("karate-timeline.html");
      writeInlined(outputDir.resolve("karate-timeline.html"), template, timelineData, reportAssets, "", mask);
   }

   private static Map<String, Object> buildTimelineData(List<Map<String, Object>> features, SuiteResult result, String env, int threadCount) {
      Map<String, Object> data = new LinkedHashMap<>();
      data.put("env", env);
      data.put("reportDate", DATE_FORMAT.format(Instant.ofEpochMilli(result.getStartTime())));
      data.put("threads", threadCount);
      data.put("karateVersion", Globals.KARATE_VERSION);
      Map<String, Object> summary = new LinkedHashMap<>();
      summary.put("feature_count", result.getFeatureCount());
      summary.put("feature_passed", result.getFeaturePassedCount());
      summary.put("feature_failed", result.getFeatureFailedCount());
      summary.put("scenario_count", result.getScenarioCount());
      summary.put("scenario_passed", result.getScenarioPassedCount());
      summary.put("scenario_failed", result.getScenarioFailedCount());
      summary.put("scenario_skipped", result.getScenarioSkippedCount());
      summary.put("duration_millis", result.getDurationMillis());
      summary.put("status", result.isFailed() ? "failed" : "passed");
      data.put("summary", summary);
      Map<String, Integer> threadToGroupId = new LinkedHashMap<>();
      List<Map<String, Object>> groups = new ArrayList<>();
      List<Map<String, Object>> items = new ArrayList<>();
      List<Map<String, Object>> allScenarios = new ArrayList<>();
      long serialMillis = 0L;
      int itemId = 0;

      for (Map<String, Object> feature : features) {
         String path = (String)feature.get("relativePath");
         String featureFileName = extractFileName(path);
         String featureHtmlName = featureFileName(feature);
         List<Map<String, Object>> scenarios = (List<Map<String, Object>>)feature.get("scenarioResults");
         if (scenarios != null) {
            for (Map<String, Object> scenario : scenarios) {
               String threadName = (String)scenario.get("executorName");
               if (threadName == null || threadName.isEmpty()) {
                  threadName = "main";
               }

               Integer groupId = threadToGroupId.get(threadName);
               if (groupId == null) {
                  groupId = threadToGroupId.size() + 1;
                  threadToGroupId.put(threadName, groupId);
                  Map<String, Object> group = new LinkedHashMap<>();
                  group.put("id", groupId);
                  String label = threadName.matches("\\d+") ? "Thread " + threadName : threadName;
                  group.put("content", label);
                  groups.add(group);
               }

               Map<String, Object> item = new LinkedHashMap<>();
               item.put("id", ++itemId);
               item.put("group", groupId);
               String refId = (String)scenario.get("refId");
               String content = featureFileName + refId;
               item.put("content", content);
               long startTime = ((Number)scenario.get("startTime")).longValue();
               long endTime = ((Number)scenario.get("endTime")).longValue() - 1L;
               if (endTime < startTime) {
                  endTime = startTime;
               }

               item.put("start", startTime);
               item.put("end", endTime);
               String startTimeStr = TIME_FORMAT.format(Instant.ofEpochMilli(startTime));
               long durationMs = endTime - startTime + 1L;
               String title = content + " " + startTimeStr + " (" + durationMs + " ms)";
               String scenarioName = (String)scenario.get("name");
               if (scenarioName != null && !scenarioName.isEmpty()) {
                  title = title + " " + scenarioName;
               }

               item.put("title", title);
               boolean passed = Boolean.TRUE.equals(scenario.get("passed"));
               item.put("className", passed ? "passed" : "failed");
               items.add(item);
               long scenarioDuration = endTime - startTime + 1L;
               serialMillis += scenarioDuration;
               Map<String, Object> s = new LinkedHashMap<>();
               s.put("featureFileName", featureFileName);
               s.put("featureHtmlName", featureHtmlName);
               s.put("refId", refId);
               s.put("name", scenarioName);
               s.put("durationMillis", scenarioDuration);
               s.put("passed", passed);
               allScenarios.add(s);
            }
         }
      }

      data.put("groups", groups);
      data.put("items", items);
      data.put("scenarios", allScenarios);
      data.put("serialDurationMillis", serialMillis);
      return data;
   }

   private static String extractFileName(String path) {
      if (path != null && !path.isEmpty()) {
         int lastSlash = path.lastIndexOf(47);
         if (lastSlash >= 0) {
            path = path.substring(lastSlash + 1);
         }

         lastSlash = path.lastIndexOf(92);
         if (lastSlash >= 0) {
            path = path.substring(lastSlash + 1);
         }

         if (path.endsWith(".feature")) {
            path = path.substring(0, path.length() - 8);
         }

         return path;
      } else {
         return "unknown";
      }
   }
}
