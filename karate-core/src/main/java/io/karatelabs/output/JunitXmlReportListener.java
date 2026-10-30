package io.karatelabs.output;

import io.karatelabs.core.FeatureResult;
import io.karatelabs.core.Suite;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.UnaryOperator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JunitXmlReportListener implements ResultListener {
   private static final Logger logger = LoggerFactory.getLogger("karate.runtime");
   public static final String SUBFOLDER = "junit-xml";
   private final Path outputDir;
   private UnaryOperator<String> mask;

   public JunitXmlReportListener(Path outputDir) {
      this.outputDir = outputDir.resolve("junit-xml");
   }

   @Override
   public void onSuiteStart(Suite suite) {
      this.mask = suite.outputMask;

      try {
         if (!Files.exists(this.outputDir)) {
            Files.createDirectories(this.outputDir);
         }
      } catch (Exception var3) {
         logger.warn("Failed to create JUnit XML output directory: {}", var3.getMessage());
      }
   }

   @Override
   public void onFeatureEnd(FeatureResult result) {
      result.sortScenarioResults();

      try {
         String xml = JunitXmlWriter.serializeFeature(result);
         JunitXmlWriter.writeSerialized(JunitXmlWriter.fileNameFor(result), this.mask == null ? xml : this.mask.apply(xml), this.outputDir);
      } catch (Exception var3) {
         logger.warn("Failed to write JUnit XML for {}: {}", result.getDisplayName(), var3.getMessage());
      }
   }
}
