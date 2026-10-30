package io.karatelabs.cli;

import io.karatelabs.common.FileUtils;
import io.karatelabs.core.KaratePom;
import io.karatelabs.core.Runner;
import io.karatelabs.core.SuiteResult;
import io.karatelabs.output.Console;
import io.karatelabs.output.LogContext;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(
   name = "run",
   mixinStandardHelpOptions = true,
   description = {"Run Karate tests"}
)
public class RunCommand implements Callable<Integer> {
   public static final String DEFAULT_POM_FILE = "karate-pom.json";
   @Parameters(
      description = {"Feature files or directories to run"},
      arity = "0..*"
   )
   List<String> paths;
   @Option(
      names = {"-P", "--path"},
      split = ",",
      description = {"Feature file or directory to run. Repeatable and/or comma-separated. Combines with any positional paths. For paths containing commas, use the repeated form."}
   )
   List<String> pathOptions;
   @Option(
      names = {"-t", "--tags"},
      description = {"Tag expression to filter scenarios (e.g., '@smoke', '~@slow')"}
   )
   List<String> tags;
   @Option(
      names = {"-T", "--threads"},
      description = {"Number of parallel threads (default: 1)"}
   )
   Integer threads;
   @Option(
      names = {"-e", "--env"},
      description = {"Value of 'karate.env'"}
   )
   String env;
   @Option(
      names = {"-n", "--name"},
      description = {"Scenario name filter (exact match, leading/trailing whitespace ignored). Bypasses tag filters so @ignore/@env do not exclude the match."}
   )
   String scenarioName;
   @Option(
      names = {"-o", "--output"},
      description = {"Output directory for reports (default: target/karate-reports)"}
   )
   String outputDir;
   @Option(
      names = {"-g", "--configdir"},
      description = {"Directory containing karate-config.js"}
   )
   String configDir;
   @Option(
      names = {"-w", "--workdir"},
      description = {"Working directory for relative path resolution (default: current directory)"}
   )
   String workingDir;
   @Option(
      names = {"-C", "--clean"},
      description = {"Clean output directory before running"}
   )
   Boolean clean;
   @Option(
      names = {"-B", "--backup-reportdir"},
      defaultValue = "true",
      fallbackValue = "true",
      arity = "0..1",
      description = {"Backup report directory before running tests (default: true)"}
   )
   Boolean backup;
   @Option(
      names = {"-D", "--dryrun"},
      description = {"Dry run mode (parse but don't execute)"}
   )
   Boolean dryRun;
   @Option(
      names = {"-p", "--pom"},
      description = {"Path to project file (default: karate-pom.json)"}
   )
   String pomFile;
   @Option(
      names = {"--no-pom"},
      description = {"Ignore karate-pom.json even if present"}
   )
   boolean noPom;
   @Option(
      names = {"--log-report"},
      description = {"Threshold for what gets captured into reports (HTML/JSONL/etc): trace, debug, info, warn, error (default: debug)"}
   )
   String logReport;
   @Option(
      names = {"--log-console"},
      description = {"Threshold for SLF4J/console output via Logback: trace, debug, info, warn, error, off/none (default: info)"}
   )
   String logConsole;
   @Option(
      names = {"-f", "--format"},
      split = ",",
      description = {"Comma-separated output formats. Use ~ to disable. e.g. '-f ~html,cucumber:json' Formats: html (default), cucumber:json, junit:xml, karate:jsonl"}
   )
   List<String> formats;
   @Option(
      names = {"--listener"},
      split = ",",
      description = {"Comma-separated RunListener class names (must have no-arg constructor)"}
   )
   List<String> listeners;
   @Option(
      names = {"--listener-factory"},
      split = ",",
      description = {"Comma-separated RunListenerFactory class names (must have no-arg constructor). Creates per-thread listener instances."}
   )
   List<String> listenerFactories;
   private KaratePom pom;

   public Integer call() {
      if (this.logConsole != null) {
         LogContext.setRuntimeLogLevel(this.logConsole);
      }

      if (!this.noPom) {
         this.loadPom();
      }

      List<String> effectivePaths = this.resolvePaths();
      String effectiveOutputDir = this.resolveOutputDir();
      String effectiveWorkingDir = this.resolveWorkingDir();
      int effectiveThreads = this.resolveThreads();
      boolean effectiveClean = this.resolveClean();
      if (effectiveClean) {
         this.cleanOutputDir(effectiveOutputDir, effectiveWorkingDir);
      }

      if (effectivePaths != null && !effectivePaths.isEmpty()) {
         try {
            SuiteResult result = this.toBuilder().parallel(effectiveThreads);
            return result.isFailed() ? 1 : 0;
         } catch (Exception var7) {
            Console.println(Console.fail("Error: " + var7.getMessage()));
            var7.printStackTrace();
            return 3;
         }
      } else {
         Console.println(Console.yellow("No test paths specified."));
         Console.println("Usage: karate run [options] <paths...>");
         Console.println("       karate run -p karate-pom.json");
         Console.println("Run 'karate run --help' for more information.");
         return 0;
      }
   }

   Runner.Builder toBuilder() {
      Runner.Builder builder = Runner.builder();
      builder.path(this.resolvePaths());
      builder.outputDir(this.resolveOutputDir());
      if (this.resolveDryRun()) {
         builder.dryRun(true);
      }

      if (this.env != null) {
         builder.karateEnv(this.env);
      } else if (this.pom != null && this.pom.getEnv() != null) {
         builder.karateEnv(this.pom.getEnv());
      }

      if (this.tags != null && !this.tags.isEmpty()) {
         builder.tags(this.tags.toArray(new String[0]));
      } else if (this.pom != null && !this.pom.getTags().isEmpty()) {
         builder.tags(this.pom.getTags().toArray(new String[0]));
      }

      if (this.scenarioName != null) {
         builder.scenarioName(this.scenarioName);
      } else if (this.pom != null && this.pom.getScenarioName() != null) {
         builder.scenarioName(this.pom.getScenarioName());
      }

      if (this.configDir != null) {
         builder.configDir(this.configDir);
      } else if (this.pom != null && this.pom.getConfigDir() != null) {
         builder.configDir(this.pom.getConfigDir());
      }

      String effectiveWorkingDir = this.resolveWorkingDir();
      if (effectiveWorkingDir != null) {
         builder.workingDir(effectiveWorkingDir);
      }

      builder.backupOutputDir(this.resolveBackup());
      String effectiveLogReport = this.resolveLogReport();
      if (effectiveLogReport != null) {
         builder.logLevel(effectiveLogReport);
      }

      String effectiveLogConsole = this.resolveLogConsole();
      if (effectiveLogConsole != null) {
         builder.consoleLevel(effectiveLogConsole);
      }

      if (this.formats != null) {
         builder.outputHtmlReport(this.isFormatEnabled("html", true));
         builder.outputCucumberJson(this.isFormatEnabled("cucumber:json", false));
         builder.outputJunitXml(this.isFormatEnabled("junit:xml", false));
         builder.outputJsonLines(this.isFormatEnabled("karate:jsonl", false));
      } else if (this.pom != null) {
         KaratePom.OutputPom output = this.pom.getOutput();
         builder.outputHtmlReport(output.isHtml());
         builder.outputCucumberJson(output.isCucumberJson());
         builder.outputJunitXml(output.isJunitXml());
         builder.outputJsonLines(output.isJsonLines());
      }

      List<String> effectiveListeners = this.resolveListeners();
      if (effectiveListeners != null) {
         for (String className : effectiveListeners) {
            builder.listenerFactory(className);
         }
      }

      List<String> effectiveListenerFactories = this.resolveListenerFactories();
      if (effectiveListenerFactories != null) {
         for (String className : effectiveListenerFactories) {
            builder.listenerFactory(className);
         }
      }

      return builder;
   }

   void loadPom() {
      String file = this.pomFile != null ? this.pomFile : "karate-pom.json";
      Path pomPath;
      if (this.workingDir != null) {
         pomPath = Path.of(this.workingDir).resolve(file);
      } else {
         pomPath = Path.of(file);
      }

      if (Files.exists(pomPath)) {
         try {
            this.pom = KaratePom.load(pomPath);
            Console.println(Console.info(pomPath.getFileName() + " loaded"));
         } catch (Exception var4) {
            Console.println(Console.warn("Failed to load pom: " + var4.getMessage()));
         }
      }
   }

   private List<String> resolvePaths() {
      List<String> combined = new ArrayList<>();
      if (this.paths != null) {
         combined.addAll(this.paths);
      }

      if (this.pathOptions != null) {
         combined.addAll(this.pathOptions);
      }

      if (!combined.isEmpty()) {
         return combined;
      } else {
         return this.pom != null && !this.pom.getPaths().isEmpty() ? this.pom.getPaths() : null;
      }
   }

   private String resolveOutputDir() {
      if (this.outputDir != null) {
         return this.outputDir;
      } else {
         return this.pom != null && this.pom.getOutput().getDir() != null ? this.pom.getOutput().getDir() : FileUtils.getBuildDir() + "/karate-reports";
      }
   }

   private String resolveWorkingDir() {
      if (this.workingDir != null) {
         return this.workingDir;
      } else {
         return this.pom != null && this.pom.getWorkingDir() != null ? this.pom.getWorkingDir() : null;
      }
   }

   private int resolveThreads() {
      if (this.threads != null) {
         return this.threads;
      } else {
         return this.pom != null && this.pom.getThreads() > 0 ? this.pom.getThreads() : 1;
      }
   }

   private boolean resolveClean() {
      if (this.clean != null) {
         return this.clean;
      } else {
         return this.pom != null ? this.pom.isClean() : false;
      }
   }

   private boolean resolveDryRun() {
      if (this.dryRun != null) {
         return this.dryRun;
      } else {
         return this.pom != null ? this.pom.isDryRun() : false;
      }
   }

   private boolean resolveBackup() {
      return this.backup != null ? this.backup : true;
   }

   private String resolveLogReport() {
      if (this.logReport != null) {
         return this.logReport;
      } else {
         return this.pom != null && this.pom.getLogging() != null && this.pom.getLogging().getReport() != null ? this.pom.getLogging().getReport() : null;
      }
   }

   private String resolveLogConsole() {
      if (this.logConsole != null) {
         return this.logConsole;
      } else {
         return this.pom != null && this.pom.getLogging() != null && this.pom.getLogging().getConsole() != null ? this.pom.getLogging().getConsole() : null;
      }
   }

   private List<String> resolveListeners() {
      if (this.listeners != null && !this.listeners.isEmpty()) {
         return this.listeners;
      } else {
         return this.pom != null && !this.pom.getListeners().isEmpty() ? this.pom.getListeners() : null;
      }
   }

   private List<String> resolveListenerFactories() {
      if (this.listenerFactories != null && !this.listenerFactories.isEmpty()) {
         return this.listenerFactories;
      } else {
         return this.pom != null && !this.pom.getListenerFactories().isEmpty() ? this.pom.getListenerFactories() : null;
      }
   }

   private boolean isFormatEnabled(String format, boolean defaultValue) {
      return isFormatEnabled(this.formats, format, defaultValue);
   }

   public static boolean isFormatEnabled(List<String> formats, String format, boolean defaultValue) {
      if (formats == null) {
         return defaultValue;
      } else if (formats.contains("~" + format)) {
         return false;
      } else {
         return formats.contains(format) ? true : defaultValue;
      }
   }

   public Integer runWithPaths(List<String> legacyPaths) {
      this.paths = legacyPaths;
      return this.call();
   }

   private void cleanOutputDir(String dir, String workDir) {
      Path output;
      if (workDir != null) {
         output = Path.of(workDir).resolve(dir);
      } else {
         output = Path.of(dir);
      }

      if (Files.exists(output)) {
         try {
            this.deleteDirectory(output.toFile());
            Console.println(Console.info("Cleaned: " + output));
         } catch (Exception var5) {
            Console.println(Console.warn("Failed to clean output directory: " + var5.getMessage()));
         }
      }
   }

   private void deleteDirectory(File dir) {
      File[] files = dir.listFiles();
      if (files != null) {
         for (File file : files) {
            if (file.isDirectory()) {
               this.deleteDirectory(file);
            } else {
               file.delete();
            }
         }
      }

      dir.delete();
   }

   public List<String> getPaths() {
      return this.paths;
   }

   public List<String> getTags() {
      return this.tags;
   }

   public Integer getThreads() {
      return this.threads;
   }

   public String getEnv() {
      return this.env;
   }

   public String getScenarioName() {
      return this.scenarioName;
   }

   public String getOutputDir() {
      return this.outputDir;
   }

   public String getConfigDir() {
      return this.configDir;
   }

   public Boolean isDryRun() {
      return this.dryRun;
   }

   public String getPomFile() {
      return this.pomFile;
   }

   public String getWorkingDir() {
      return this.workingDir;
   }

   public List<String> getPathOptions() {
      return this.pathOptions;
   }

   public List<String> getFormats() {
      return this.formats;
   }

   public String getLogReport() {
      return this.logReport;
   }

   public String getLogConsole() {
      return this.logConsole;
   }
}
