package io.karatelabs.output;

import io.karatelabs.core.FeatureResult;
import io.karatelabs.core.ScenarioResult;
import io.karatelabs.core.StepResult;
import io.karatelabs.gherkin.Tag;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class JunitXmlWriter {
   private static final Logger logger = LoggerFactory.getLogger("karate.runtime");

   private JunitXmlWriter() {
   }

   public static String serializeFeature(FeatureResult result) {
      return featureToXml(result);
   }

   public static String fileNameFor(FeatureResult result) {
      return result.getFeature().getResource().getPackageQualifiedName() + ".xml";
   }

   public static void writeSerialized(String fileName, String content, Path outputDir) {
      try {
         if (!Files.exists(outputDir)) {
            Files.createDirectories(outputDir);
         }

         Files.writeString(outputDir.resolve(fileName), content);
      } catch (Exception var4) {
         logger.warn("Failed to write xml for {}: {}", fileName, var4.getMessage());
      }
   }

   public static void writeFeature(FeatureResult result, Path outputDir) {
      try {
         if (!Files.exists(outputDir)) {
            Files.createDirectories(outputDir);
         }

         String fileName = result.getFeature().getResource().getPackageQualifiedName() + ".xml";
         Path xmlPath = outputDir.resolve(fileName);
         String xml = featureToXml(result);
         Files.writeString(xmlPath, xml);
         logger.debug("JUnit XML written: {}", xmlPath);
      } catch (Exception var5) {
         logger.warn("Failed to write JUnit XML for {}: {}", result.getDisplayName(), var5.getMessage());
      }
   }

   public static String featureToXml(FeatureResult result) {
      DecimalFormat formatter = (DecimalFormat)NumberFormat.getNumberInstance(Locale.US);
      formatter.applyPattern("0.######");
      String packageQualifiedName = result.getFeature().getResource().getPackageQualifiedName();
      StringBuilder xml = new StringBuilder();
      String featureName = result.getFeature().getName();
      if (featureName == null || featureName.isEmpty()) {
         featureName = result.getDisplayName();
      }

      xml.append("<testsuite");
      xml.append(" name=\"").append(escape(featureName)).append("\"");
      xml.append(" tests=\"").append(result.getScenarioCount()).append("\"");
      xml.append(" failures=\"").append(result.getFailedCount()).append("\"");
      xml.append(" time=\"").append(formatter.format(result.getDurationMillis() / 1000.0)).append("\"");
      xml.append(" skipped=\"0\"");
      xml.append(">");

      for (ScenarioResult sr : result.getScenarioResults()) {
         writeTestcase(xml, sr, packageQualifiedName, formatter);
      }

      xml.append("</testsuite>");
      return xml.toString();
   }

   private static void writeTestcase(StringBuilder xml, ScenarioResult sr, String classname, DecimalFormat formatter) {
      String scenarioName = sr.getScenario().getName();
      if (scenarioName == null || scenarioName.isEmpty()) {
         scenarioName = sr.getScenario().getRefId();
      }

      xml.append("<testcase");
      xml.append(" classname=\"").append(escape(classname)).append("\"");
      xml.append(" name=\"").append(escape(scenarioName)).append("\"");
      xml.append(" time=\"").append(formatter.format(sr.getDurationMillis() / 1000.0)).append("\"");
      xml.append(">");
      List<Tag> tags = sr.getScenario().getTagsEffective();
      if (tags != null && !tags.isEmpty()) {
         StringBuilder plain = new StringBuilder();
         StringBuilder kv = new StringBuilder();

         for (Tag tag : tags) {
            List<String> values = tag.getValues();
            if (values != null && !values.isEmpty()) {
               kv.append("<property name=\"")
                  .append(escape(tag.getName()))
                  .append("\"")
                  .append(" value=\"")
                  .append(escape(String.join(",", values)))
                  .append("\"/>");
            } else {
               if (plain.length() > 0) {
                  plain.append(',');
               }

               plain.append(tag.toString());
            }
         }

         if (plain.length() > 0 || kv.length() > 0) {
            xml.append("<properties>");
            if (plain.length() > 0) {
               xml.append("<property name=\"tags\" value=\"").append(escape(plain.toString())).append("\"/>");
            }

            xml.append((CharSequence)kv);
            xml.append("</properties>");
         }
      }

      if (sr.isReportDisabled()) {
         if (sr.isFailed()) {
            xml.append("<failure message=\"").append(escape("output suppressed by @report=false (full detail in runtime logs)")).append("\"/>");
         }

         xml.append("</testcase>\n");
      } else {
         StringBuilder stepLogs = new StringBuilder();
         appendStepLogs(sr.getStepResults(), stepLogs);
         if (stepLogs.length() > 0) {
            if (sr.isFailed()) {
               xml.append("<failure message=\"").append(escape(truncate(sr.getFailureMessage(), 1000))).append("\">");
               xml.append(escape(stepLogs.toString()));
               xml.append("</failure>");
            } else {
               xml.append("<system-out>");
               xml.append(escape(stepLogs.toString()));
               xml.append("</system-out>");
            }
         } else if (sr.isFailed()) {
            writeFailure(xml, sr);
         }

         xml.append("</testcase>\n");
      }
   }

   private static void appendStepLogs(List<StepResult> stepResults, StringBuilder sb) {
      if (stepResults != null) {
         for (StepResult sr : stepResults) {
            boolean syntheticLogIsDescription = false;
            if (sr.getStep() != null) {
               sb.append(sr.getStep().getPrefix()).append(" ").append(sr.getStep().getText()).append("\n");
            } else if (sr.isHook()) {
               sb.append("* ").append(sr.getHookName()).append("\n");
            } else if (sr.getSyntheticText() != null) {
               sb.append("* ").append(sr.getSyntheticText()).append("\n");
            } else if (sr.getLog() != null && !sr.getLog().isEmpty()) {
               sb.append("* ").append(Console.stripAnsi(sr.getLog()).trim()).append("\n");
               syntheticLogIsDescription = true;
            } else {
               sb.append("*\n");
            }

            String stepLog = sr.getLog();
            if (!syntheticLogIsDescription && stepLog != null && !stepLog.isEmpty()) {
               sb.append(Console.stripAnsi(stepLog));
               if (!stepLog.endsWith("\n")) {
                  sb.append("\n");
               }
            }

            List<FeatureResult> callResults = sr.getCallResults();
            if (callResults != null) {
               for (FeatureResult fr : callResults) {
                  for (ScenarioResult nestedSr : fr.getScenarioResults()) {
                     appendStepLogs(nestedSr.getStepResults(), sb);
                  }
               }
            }
         }
      }
   }

   private static void writeFailure(StringBuilder xml, ScenarioResult sr) {
      String message = sr.getFailureMessage();
      if (message == null) {
         message = "Unknown failure";
      }

      Throwable error = sr.getError();
      xml.append("<failure message=\"").append(escape(truncate(message, 1000))).append("\">");
      if (error != null) {
         xml.append(escape(getStackTrace(error)));
      } else {
         xml.append(escape(message));
      }

      xml.append("</failure>");
   }

   private static String escape(String text) {
      return text == null ? "" : text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&apos;");
   }

   private static String truncate(String text, int maxLength) {
      return text != null && text.length() > maxLength ? text.substring(0, maxLength - 3) + "..." : text;
   }

   private static String getStackTrace(Throwable t) {
      StringWriter sw = new StringWriter();
      t.printStackTrace(new PrintWriter(sw));
      return sw.toString();
   }
}
