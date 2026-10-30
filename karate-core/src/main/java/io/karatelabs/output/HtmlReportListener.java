package io.karatelabs.output;

import io.karatelabs.core.FeatureResult;
import io.karatelabs.core.ReportAssets;
import io.karatelabs.core.Suite;
import io.karatelabs.core.SuiteResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.UnaryOperator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HtmlReportListener implements ResultListener {
   private static final Logger logger = LoggerFactory.getLogger("karate.runtime");
   public static final String SUBFOLDER = "feature-html";
   private final Path outputDir;
   private final String env;
   private final List<Map<String, Object>> featureMaps = new CopyOnWriteArrayList<>();
   private long suiteStartTime;
   private int threadCount;
   private boolean resourcesCopied = false;
   private Map<String, ReportAssets> reportAssets = Collections.emptyMap();
   private UnaryOperator<String> mask;
   private Suite suite;

   public HtmlReportListener(Path outputDir, String env) {
      this.outputDir = outputDir;
      this.env = env;
   }

   @Override
   public void onSuiteStart(Suite suite) {
      this.suiteStartTime = System.currentTimeMillis();
      this.threadCount = suite.threadCount;
      this.reportAssets = suite.getReportAssets();
      this.mask = suite.outputMask;
      this.suite = suite;
      HtmlReportWriter.resetEmbedCounter();

      try {
         Files.createDirectories(this.outputDir.resolve("feature-html"));
         Files.createDirectories(this.outputDir.resolve("res"));
      } catch (Exception var3) {
         logger.warn("Failed to create report directories: {}", var3.getMessage());
      }
   }

   @Override
   public void onFeatureEnd(FeatureResult result) {
      result.sortScenarioResults();
      this.featureMaps.add(summaryJson(result));

      try {
         this.ensureResourcesCopied();
         HtmlReportWriter.renderFeatureHtml(
            HtmlReportWriter.prepareFeatureData(result, this.outputDir, this.mask), this.outputDir, this.reportAssets, this.mask
         );
      } catch (Exception var3) {
         logger.warn("Failed to write feature HTML for {}: {}", result.getDisplayName(), var3.getMessage());
      }
   }

   private static Map<String, Object> summaryJson(FeatureResult result) {
      Map<String, Object> featureMap = result.toJson();
      if (featureMap.get("scenarioResults") instanceof List<?> scenarioList) {
         ArrayList reduced = new ArrayList(scenarioList.size());

         for (Object scenario : scenarioList) {
            if (scenario instanceof Map<?, ?> scenarioMap) {
               Map<String, Object> copy = new LinkedHashMap<>((Map<? extends String, ? extends Object>)scenarioMap);
               copy.remove("stepResults");
               reduced.add(copy);
            }
         }

         featureMap.put("scenarioResults", reduced);
         return featureMap;
      } else {
         return featureMap;
      }
   }

   @Override
   public void onSuiteEnd(SuiteResult result) {
      try {
         this.ensureResourcesCopied();
         List<Map<String, Object>> summaryCards = this.suite != null ? this.suite.getSummaryCards() : Collections.emptyList();
         HtmlReportWriter.writeSummaryPages(this.featureMaps, result, this.outputDir, this.env, this.reportAssets, summaryCards, this.mask);
         HtmlReportWriter.writeTimelineHtml(this.featureMaps, result, this.outputDir, this.env, this.threadCount, this.reportAssets, this.mask);
         logger.debug("HTML report written to: {}", this.outputDir.resolve("karate-summary.html"));
      } catch (Exception var3) {
         logger.warn("Failed to write HTML summary: {}", var3.getMessage());
      }
   }

   private synchronized void ensureResourcesCopied() {
      if (!this.resourcesCopied) {
         try {
            HtmlReportWriter.copyStaticResources(this.outputDir.resolve("res"));
            this.resourcesCopied = true;
         } catch (Exception var2) {
            logger.warn("Failed to copy static resources: {}", var2.getMessage());
         }
      }
   }
}
