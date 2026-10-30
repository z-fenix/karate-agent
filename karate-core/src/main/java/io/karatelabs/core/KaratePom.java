package io.karatelabs.core;

import io.karatelabs.common.Json;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class KaratePom {
   private List<String> paths = new ArrayList<>();
   private List<String> tags = new ArrayList<>();
   private String env;
   private int threads = 1;
   private String scenarioName;
   private String configDir;
   private boolean dryRun;
   private boolean clean;
   private String workingDir;
   private KaratePom.OutputPom output = new KaratePom.OutputPom();
   private KaratePom.LoggingPom logging;
   private List<String> listeners = new ArrayList<>();
   private List<String> listenerFactories = new ArrayList<>();

   public static KaratePom load(String configPath) {
      return load(Path.of(configPath));
   }

   public static KaratePom load(Path configPath) {
      try {
         String content = Files.readString(configPath);
         return parse(content);
      } catch (Exception var2) {
         throw new RuntimeException("Failed to load config from: " + configPath, var2);
      }
   }

   public static KaratePom parse(String json) {
      Json j = Json.of(json);
      if (!j.isObject()) {
         throw new RuntimeException("Invalid config: expected JSON object");
      } else {
         KaratePom config = new KaratePom();
         j.<List<String>>getOptional("paths").ifPresent(config::setPaths);
         j.<List<String>>getOptional("tags").ifPresent(config::setTags);
         j.<String>getOptional("env").ifPresent(config::setEnv);
         j.<Integer>getOptional("threads").ifPresent(config::setThreads);
         j.<String>getOptional("scenarioName").ifPresent(config::setScenarioName);
         j.<String>getOptional("configDir").ifPresent(config::setConfigDir);
         j.<Boolean>getOptional("dryRun").ifPresent(config::setDryRun);
         j.<Boolean>getOptional("clean").ifPresent(config::setClean);
         j.<String>getOptional("workingDir").ifPresent(config::setWorkingDir);
         if (j.pathExists("output")) {
            KaratePom.OutputPom output = config.getOutput();
            j.<String>getOptional("output.dir").ifPresent(output::setDir);
            j.<Boolean>getOptional("output.html").ifPresent(output::setHtml);
            j.<Boolean>getOptional("output.junitXml").ifPresent(output::setJunitXml);
            j.<Boolean>getOptional("output.cucumberJson").ifPresent(output::setCucumberJson);
            j.<Boolean>getOptional("output.jsonLines").ifPresent(output::setJsonLines);
            if (j.pathExists("output.logLevel")) {
               throw new RuntimeException(
                  "'output.logLevel' is no longer supported in karate-pom.json; use 'logging.report' (and optionally 'logging.console') at the top level (see docs/MIGRATION_GUIDE.md#logging)"
               );
            }
         }

         if (j.pathExists("logging")) {
            KaratePom.LoggingPom logging = new KaratePom.LoggingPom();
            j.<String>getOptional("logging.report").ifPresent(logging::setReport);
            j.<String>getOptional("logging.console").ifPresent(logging::setConsole);
            config.setLogging(logging);
         }

         j.<List<String>>getOptional("listeners").ifPresent(config::setListeners);
         j.<List<String>>getOptional("listenerFactories").ifPresent(config::setListenerFactories);
         return config;
      }
   }

   public Runner.Builder applyTo(Runner.Builder builder) {
      if (!this.paths.isEmpty()) {
         builder.path(this.paths);
      }

      if (!this.tags.isEmpty()) {
         builder.tags(this.tags.toArray(new String[0]));
      }

      if (this.env != null) {
         builder.karateEnv(this.env);
      }

      if (this.scenarioName != null) {
         builder.scenarioName(this.scenarioName);
      }

      if (this.configDir != null) {
         builder.configDir(this.configDir);
      }

      if (this.workingDir != null) {
         builder.workingDir(this.workingDir);
      }

      builder.dryRun(this.dryRun);
      builder.outputDir(this.output.dir);
      builder.outputHtmlReport(this.output.html);
      builder.outputJunitXml(this.output.junitXml);
      builder.outputCucumberJson(this.output.cucumberJson);
      builder.outputJsonLines(this.output.jsonLines);
      if (this.logging != null) {
         if (this.logging.report != null) {
            builder.logLevel(this.logging.report);
         }

         if (this.logging.console != null) {
            builder.consoleLevel(this.logging.console);
         }
      }

      for (String className : this.listeners) {
         builder.listenerFactory(className);
      }

      for (String className : this.listenerFactories) {
         builder.listenerFactory(className);
      }

      return builder;
   }

   public List<String> getPaths() {
      return this.paths;
   }

   public void setPaths(List<String> paths) {
      this.paths = paths != null ? new ArrayList<>(paths) : new ArrayList<>();
   }

   public List<String> getTags() {
      return this.tags;
   }

   public void setTags(List<String> tags) {
      this.tags = tags != null ? new ArrayList<>(tags) : new ArrayList<>();
   }

   public String getEnv() {
      return this.env;
   }

   public void setEnv(String env) {
      this.env = env;
   }

   public int getThreads() {
      return this.threads;
   }

   public void setThreads(int threads) {
      this.threads = threads;
   }

   public String getScenarioName() {
      return this.scenarioName;
   }

   public void setScenarioName(String scenarioName) {
      this.scenarioName = scenarioName;
   }

   public String getConfigDir() {
      return this.configDir;
   }

   public void setConfigDir(String configDir) {
      this.configDir = configDir;
   }

   public boolean isDryRun() {
      return this.dryRun;
   }

   public void setDryRun(boolean dryRun) {
      this.dryRun = dryRun;
   }

   public boolean isClean() {
      return this.clean;
   }

   public void setClean(boolean clean) {
      this.clean = clean;
   }

   public String getWorkingDir() {
      return this.workingDir;
   }

   public void setWorkingDir(String workingDir) {
      this.workingDir = workingDir;
   }

   public KaratePom.OutputPom getOutput() {
      return this.output;
   }

   public void setOutput(KaratePom.OutputPom output) {
      this.output = output != null ? output : new KaratePom.OutputPom();
   }

   public KaratePom.LoggingPom getLogging() {
      return this.logging;
   }

   public void setLogging(KaratePom.LoggingPom logging) {
      this.logging = logging;
   }

   public List<String> getListeners() {
      return this.listeners;
   }

   public void setListeners(List<String> listeners) {
      this.listeners = listeners != null ? new ArrayList<>(listeners) : new ArrayList<>();
   }

   public List<String> getListenerFactories() {
      return this.listenerFactories;
   }

   public void setListenerFactories(List<String> listenerFactories) {
      this.listenerFactories = listenerFactories != null ? new ArrayList<>(listenerFactories) : new ArrayList<>();
   }

   public static class LoggingPom {
      private String report;
      private String console;

      public String getReport() {
         return this.report;
      }

      public void setReport(String report) {
         this.report = report;
      }

      public String getConsole() {
         return this.console;
      }

      public void setConsole(String console) {
         this.console = console;
      }
   }

   public static class OutputPom {
      private String dir = "target/karate-reports";
      private boolean html = true;
      private boolean junitXml;
      private boolean cucumberJson;
      private boolean jsonLines;

      public String getDir() {
         return this.dir;
      }

      public void setDir(String dir) {
         this.dir = dir;
      }

      public boolean isHtml() {
         return this.html;
      }

      public void setHtml(boolean html) {
         this.html = html;
      }

      public boolean isJunitXml() {
         return this.junitXml;
      }

      public void setJunitXml(boolean junitXml) {
         this.junitXml = junitXml;
      }

      public boolean isCucumberJson() {
         return this.cucumberJson;
      }

      public void setCucumberJson(boolean cucumberJson) {
         this.cucumberJson = cucumberJson;
      }

      public boolean isJsonLines() {
         return this.jsonLines;
      }

      public void setJsonLines(boolean jsonLines) {
         this.jsonLines = jsonLines;
      }
   }
}
