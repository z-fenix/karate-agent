package io.karatelabs.output;

import io.karatelabs.core.FeatureResult;
import io.karatelabs.core.Suite;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.UnaryOperator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CucumberJsonReportListener implements ResultListener {
   private static final Logger logger = LoggerFactory.getLogger("karate.runtime");
   public static final String SUBFOLDER = "cucumber-json";
   private final Path outputDir;
   private UnaryOperator<String> mask;

   public CucumberJsonReportListener(Path outputDir) {
      this.outputDir = outputDir.resolve("cucumber-json");
   }

   @Override
   public void onSuiteStart(Suite suite) {
      this.mask = suite.outputMask;

      try {
         if (!Files.exists(this.outputDir)) {
            Files.createDirectories(this.outputDir);
         }
      } catch (Exception var3) {
         logger.warn("Failed to create Cucumber JSON output directory: {}", var3.getMessage());
      }
   }

   @Override
   public void onFeatureEnd(FeatureResult result) {
      result.sortScenarioResults();

      try {
         String json = CucumberJsonWriter.serializeFeature(result);
         CucumberJsonWriter.writeSerialized(CucumberJsonWriter.fileNameFor(result), this.mask == null ? json : this.mask.apply(json), this.outputDir);
      } catch (Exception var3) {
         logger.warn("Failed to write Cucumber JSON for {}: {}", result.getDisplayName(), var3.getMessage());
      }
   }
}
