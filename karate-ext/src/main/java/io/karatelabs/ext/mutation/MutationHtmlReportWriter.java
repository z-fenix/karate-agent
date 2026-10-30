package io.karatelabs.ext.mutation;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class MutationHtmlReportWriter {
   private static final Logger LOGGER = LoggerFactory.getLogger(MutationHtmlReportWriter.class);
   private static final String CSS_RESOURCE = "META-INF/karate-ext/static/mutation-report.css";
   private static final String JS_RESOURCE = "META-INF/karate-ext/static/mutation-report.js";

   private MutationHtmlReportWriter() {
   }

   public static Path writeReport(Map<String, Object> var0, Path var1) {
      try {
         String var2 = io.karatelabs.ext.common.ReportHtmlBuilder.a(
            "Mutation Report — " + var0.getOrDefault("rulebook", var0.getOrDefault("subject", "")),
            List.of("io/karatelabs/ext/max/web/pub/tailwind.css", "META-INF/karate-ext/static/mutation-report.css"),
            "KARATE_MUTATION_DATA",
            var0,
            List.of("META-INF/karate-ext/static/mutation-report.js")
         );
         Files.createDirectories(var1);
         Path var3 = var1.resolve("mutation-report.html");
         Files.writeString(var3, var2, StandardCharsets.UTF_8);
         return var3;
      } catch (Exception var4) {
         LOGGER.warn("mutation: could not write report page: {}", var4.toString());
         return null;
      }
   }
}
