package io.karatelabs.core;

import io.karatelabs.cli.RunCommand;
import io.karatelabs.output.LogContext;
import io.karatelabs.output.LogLevel;
import io.karatelabs.process.ProcessBuilder;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import org.slf4j.Logger;
import picocli.CommandLine;
import picocli.CommandLine.ParameterException;

public final class KarateOptionsHandler {
   private static final Logger logger = LogContext.RUNTIME_LOGGER;
   public static final String PROP_OPTIONS = "karate.options";
   public static final String PROP_ENV = "karate.env";
   public static final String PROP_CONFIG_DIR = "karate.config.dir";
   public static final String ENV_OPTIONS = "KARATE_OPTIONS";
   public static final String ENV_ENV = "KARATE_ENV";
   public static final String ENV_CONFIG_DIR = "KARATE_CONFIG_DIR";
   static Function<String, String> sysPropReader = System::getProperty;
   static Function<String, String> envVarReader = System::getenv;

   private KarateOptionsHandler() {
   }

   public static int apply(Runner.Builder builder, int threadCount) {
      applyEnvAndConfigDir(builder);
      String source = null;
      String raw = trimToNull(sysPropReader.apply("karate.options"));
      if (raw != null) {
         source = "system property 'karate.options'";
      } else {
         raw = trimToNull(envVarReader.apply("KARATE_OPTIONS"));
         if (raw != null) {
            source = "environment variable 'KARATE_OPTIONS'";
         }
      }

      if (raw == null) {
         return threadCount;
      } else {
         logger.info("using {}: {}", source, raw);
         return parseAndApplyOptions(builder, raw, threadCount);
      }
   }

   public static void applyEnvAndConfigDir(Runner.Builder builder) {
      applyIndividualEnv(builder);
      applyIndividualConfigDir(builder);
   }

   private static void applyIndividualEnv(Runner.Builder builder) {
      String value = trimToNull(sysPropReader.apply("karate.env"));
      String source = null;
      if (value != null) {
         source = "system property 'karate.env'";
      } else {
         value = trimToNull(envVarReader.apply("KARATE_ENV"));
         if (value != null) {
            source = "environment variable 'KARATE_ENV'";
         }
      }

      if (value != null) {
         logger.info("using {}: {}", source, value);
         if (logger.isDebugEnabled()) {
            logger.debug("karate.env override: '{}' (Builder) -> '{}' ({})", new Object[]{builder.getEnv(), value, source});
         }

         builder.setEnv(value);
      }
   }

   private static void applyIndividualConfigDir(Runner.Builder builder) {
      String value = trimToNull(sysPropReader.apply("karate.config.dir"));
      String source = null;
      if (value != null) {
         source = "system property 'karate.config.dir'";
      } else {
         value = trimToNull(envVarReader.apply("KARATE_CONFIG_DIR"));
         if (value != null) {
            source = "environment variable 'KARATE_CONFIG_DIR'";
         }
      }

      if (value != null) {
         logger.info("using {}: {}", source, value);
         if (logger.isDebugEnabled()) {
            logger.debug("karate.config.dir override: '{}' (Builder) -> '{}' ({})", new Object[]{builder.getConfigDir(), value, source});
         }

         builder.setConfigDir(value);
      }
   }

   static int parseAndApplyOptions(Runner.Builder builder, String raw, int threadCount) {
      List<String> tokens = ProcessBuilder.tokenize(raw);
      String[] argv = tokens.toArray(new String[0]);
      RunCommand parsed = new RunCommand();

      try {
         new CommandLine(parsed).parseArgs(argv);
      } catch (ParameterException var12) {
         logger.warn("invalid karate.options ignored: {}", var12.getMessage());
         return threadCount;
      }

      List<String> summary = new ArrayList<>();
      List<String> combinedPaths = new ArrayList<>();
      if (parsed.getPaths() != null) {
         combinedPaths.addAll(parsed.getPaths());
      }

      if (parsed.getPathOptions() != null) {
         combinedPaths.addAll(parsed.getPathOptions());
      }

      if (!combinedPaths.isEmpty()) {
         List<String> before = new ArrayList<>(builder.getPaths());
         builder.clearPaths();

         for (String p : combinedPaths) {
            builder.path(p);
         }

         summary.add("paths=" + combinedPaths);
         if (logger.isDebugEnabled()) {
            logger.debug("karate.options override: paths: {} (Builder) -> {} (karate.options)", before, combinedPaths);
         }
      }

      if (parsed.getTags() != null && !parsed.getTags().isEmpty()) {
         List<String> before = builder.getTags() == null ? null : new ArrayList<>(builder.getTags());
         builder.setTags(new ArrayList<>(parsed.getTags()));
         summary.add("tags=" + parsed.getTags());
         if (logger.isDebugEnabled()) {
            logger.debug("karate.options override: tags: {} (Builder) -> {} (karate.options)", before, parsed.getTags());
         }
      }

      if (parsed.getEnv() != null) {
         String before = builder.getEnv();
         builder.setEnv(parsed.getEnv());
         summary.add("env=" + parsed.getEnv());
         if (logger.isDebugEnabled()) {
            logger.debug("karate.options override: env: '{}' (Builder) -> '{}' (karate.options)", before, parsed.getEnv());
         }
      }

      int effectiveThreads = threadCount;
      if (parsed.getThreads() != null) {
         effectiveThreads = parsed.getThreads();
         summary.add("threads=" + parsed.getThreads());
         if (logger.isDebugEnabled()) {
            logger.debug("karate.options override: threads: {} (parallel param) -> {} (karate.options)", threadCount, parsed.getThreads());
         }
      }

      if (parsed.getScenarioName() != null) {
         builder.setScenarioName(parsed.getScenarioName());
         summary.add("name=" + parsed.getScenarioName());
      }

      if (parsed.getOutputDir() != null) {
         Path before = builder.getOutputDir();
         builder.setOutputDir(Path.of(parsed.getOutputDir()));
         summary.add("output=" + parsed.getOutputDir());
         if (logger.isDebugEnabled()) {
            logger.debug("karate.options override: outputDir: '{}' (Builder) -> '{}' (karate.options)", before, parsed.getOutputDir());
         }
      }

      if (parsed.getConfigDir() != null) {
         String before = builder.getConfigDir();
         builder.setConfigDir(parsed.getConfigDir());
         summary.add("configDir=" + parsed.getConfigDir());
         if (logger.isDebugEnabled()) {
            logger.debug("karate.options override: configDir: '{}' (Builder) -> '{}' (karate.options)", before, parsed.getConfigDir());
         }
      }

      if (parsed.isDryRun() != null) {
         builder.setDryRun(parsed.isDryRun());
         summary.add("dryRun=" + parsed.isDryRun());
      }

      List<String> parsedFormats = parsed.getFormats();
      if (parsedFormats != null && !parsedFormats.isEmpty()) {
         builder.setOutputHtmlReport(RunCommand.isFormatEnabled(parsedFormats, "html", true));
         builder.setOutputCucumberJson(RunCommand.isFormatEnabled(parsedFormats, "cucumber:json", false));
         builder.setOutputJunitXml(RunCommand.isFormatEnabled(parsedFormats, "junit:xml", false));
         builder.setOutputJsonLines(RunCommand.isFormatEnabled(parsedFormats, "karate:jsonl", false));
         summary.add("formats=" + parsedFormats);
      }

      if (parsed.getLogReport() != null) {
         try {
            builder.setLogLevel(LogLevel.valueOf(parsed.getLogReport().toUpperCase()));
            summary.add("logReport=" + parsed.getLogReport());
         } catch (IllegalArgumentException var11) {
            logger.warn("invalid karate.options log-report ignored: {}", parsed.getLogReport());
         }
      }

      if (parsed.getLogConsole() != null) {
         LogContext.setRuntimeLogLevel(parsed.getLogConsole());
         summary.add("logConsole=" + parsed.getLogConsole());
      }

      if (!summary.isEmpty()) {
         logger.info("karate.options applied: {}", String.join(", ", summary));
      }

      return effectiveThreads;
   }

   private static String trimToNull(String s) {
      if (s == null) {
         return null;
      } else {
         String t = s.trim();
         return t.isEmpty() ? null : t;
      }
   }
}
