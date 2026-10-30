package io.karatelabs.core;

import io.karatelabs.common.FileUtils;
import io.karatelabs.common.PathResource;
import io.karatelabs.common.Resource;
import io.karatelabs.driver.DriverProvider;
import io.karatelabs.gherkin.Feature;
import io.karatelabs.http.HttpClientFactory;
import io.karatelabs.js.DebugPointFactory;
import io.karatelabs.js.RunInterceptor;
import io.karatelabs.output.LogContext;
import io.karatelabs.output.LogLevel;
import io.karatelabs.output.ResultListener;
import java.io.File;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;
import org.slf4j.Logger;

public final class Runner {
   private Runner() {
   }

   public static Runner.Builder path(String... paths) {
      return new Runner.Builder().path(paths);
   }

   public static Runner.Builder path(List<String> paths) {
      return new Runner.Builder().path(paths);
   }

   public static Runner.Builder features(Feature... features) {
      return new Runner.Builder().features(features);
   }

   public static Runner.Builder builder() {
      return new Runner.Builder();
   }

   public static FeatureResult runFeature(String path, Map<String, Object> arg) {
      return runFeature(path, arg, null);
   }

   public static FeatureResult runFeature(String path, Map<String, Object> arg, PerfHook perfHook) {
      return runFeature(path, arg, perfHook, null);
   }

   public static FeatureResult runFeature(String path, Map<String, Object> arg, PerfHook perfHook, List<String> tags) {
      return runFeature(path, arg, perfHook, tags, null);
   }

   public static FeatureResult runFeature(String path, Map<String, Object> arg, PerfHook perfHook, List<String> tags, Runner.Builder template) {
      Runner.Builder builder = builder().outputHtmlReport(false).outputConsoleSummary(false).retainStepLogs(true);
      builder.setSkipBootFile(true);
      if (template != null) {
         if (template.getEnv() != null) {
            builder.karateEnv(template.getEnv());
         }

         if (template.getConfigDir() != null) {
            builder.configDir(template.getConfigDir());
         }

         if (template.getWorkingDir() != null) {
            builder.workingDir(template.getWorkingDir());
         }

         if (template.getHttpClientFactory() != null) {
            builder.httpClientFactory(template.getHttpClientFactory());
         }

         Map<String, String> sysProps = template.getSystemProperties();
         if (sysProps != null) {
            for (Entry<String, String> e : sysProps.entrySet()) {
               builder.systemProperty(e.getKey(), e.getValue());
            }
         }

         builder.systemEnv(template.getSystemEnv());
         if (template.getCallSingleCache() != null) {
            builder.callSingleCache(template.getCallSingleCache(), template.getCallSingleLock());
         }

         if (template.getCallOnceCacheStore() != null) {
            builder.callOnceCacheStore(template.getCallOnceCacheStore(), template.getCallOnceLockStore());
         }

         if (template.getSetupOnceCacheStore() != null) {
            builder.setupOnceCacheStore(template.getSetupOnceCacheStore());
         }

         if (template.getCaptureStepLogs() != null) {
            builder.captureStepLogs(template.getCaptureStepLogs());
         }

         builder.retainCallResults(template.isRetainCallResults());
      }

      if (tags != null && !tags.isEmpty()) {
         builder.tags(tags.toArray(new String[0]));
      }

      KarateOptionsHandler.applyEnvAndConfigDir(builder);
      Suite suite = builder.buildSuite();
      Feature feature = Feature.read(Resource.path(path, suite.getRoot(), suite.getClasspathRoot()));
      if (perfHook != null) {
         suite.setPerfHook(perfHook);
      }

      FeatureRuntime fr = new FeatureRuntime(suite, feature, null, null, false, arg, null);
      FeatureResult result = fr.call();
      ScenarioRuntime lastExecuted = fr.getLastExecuted();
      if (lastExecuted != null) {
         result.setResultVariables(lastExecuted.getAllVariables());
      }

      if (perfHook != null) {
         perfHook.afterFeature(result);
      }

      return result;
   }

   public static class Builder {
      private static final Logger logger = LogContext.RUNTIME_LOGGER;
      private final List<String> paths = new ArrayList<>();
      private final List<Feature> features = new ArrayList<>();
      private final List<RunListener> listeners = new ArrayList<>();
      private final List<RunListenerFactory> listenerFactories = new ArrayList<>();
      private final List<ResultListener> resultListeners = new ArrayList<>();
      private final Map<Path, byte[]> overlays = new HashMap<>();
      private String env;
      private List<String> tags;
      private String scenarioName;
      private String configDir;
      private Path outputDir;
      private Path workingDir;
      private boolean skipBootFile;
      private boolean dryRun;
      private boolean outputHtmlReport = true;
      private boolean outputJsonLines;
      private boolean outputJunitXml;
      private boolean outputCucumberJson;
      private boolean backupOutputDir = true;
      private boolean outputConsoleSummary = true;
      private boolean retainCallResults = false;
      private boolean retainStepLogs = false;
      private Boolean captureStepLogs;
      private Map<String, String> systemProperties;
      private Map<String, String> systemEnv;
      private UnaryOperator<String> outputMask;
      private final Map<String, Object> globals = new LinkedHashMap<>();
      private LogLevel logLevel = LogLevel.DEBUG;
      private String consoleLevel;
      private DriverProvider driverProvider;
      private HttpClientFactory httpClientFactory;
      private boolean skipTagFiltering;
      private int poolSize = -1;
      private Consumer<Suite> suiteConsumer;
      private RunInterceptor<?> debugInterceptor;
      private DebugPointFactory<?> debugPointFactory;
      private final Map<String, Set<Integer>> lineFilters = new HashMap<>();
      private List<Feature> resolvedFeatures;
      private Map<String, Object> callSingleCache;
      private ReentrantLock callSingleLock;
      private Map<String, Map<String, Object>> callOnceCacheStore;
      private Map<String, ReentrantLock> callOnceLockStore;
      private Map<String, Map<String, Object>> setupOnceCacheStore;

      Builder() {
      }

      public Runner.Builder path(String... values) {
         this.paths.addAll(Arrays.asList(values));
         return this;
      }

      public Runner.Builder path(List<String> values) {
         if (values != null) {
            this.paths.addAll(values);
         }

         return this;
      }

      public Runner.Builder features(Feature... values) {
         this.features.addAll(Arrays.asList(values));
         return this;
      }

      public Runner.Builder features(Collection<Feature> values) {
         if (values != null) {
            this.features.addAll(values);
         }

         return this;
      }

      public Runner.Builder overlay(Path path, byte[] bytes) {
         this.overlays.put(path.toAbsolutePath().normalize(), (byte[])bytes.clone());
         return this;
      }

      public Runner.Builder karateEnv(String env) {
         this.env = env;
         return this;
      }

      public Runner.Builder tags(String... tagExpressions) {
         if (tagExpressions != null && tagExpressions.length > 0) {
            if (this.tags == null) {
               this.tags = new ArrayList<>();
            }

            for (String tag : tagExpressions) {
               if (tag != null && !tag.isEmpty()) {
                  this.tags.add(tag);
               }
            }
         }

         return this;
      }

      public Runner.Builder scenarioName(String name) {
         this.scenarioName = name;
         return this;
      }

      public Runner.Builder configDir(String dir) {
         this.configDir = dir;
         return this;
      }

      public Runner.Builder systemProperty(String key, String value) {
         if (this.systemProperties == null) {
            this.systemProperties = new HashMap<>();
         }

         this.systemProperties.put(key, value);
         return this;
      }

      public Runner.Builder systemEnv(Map<String, String> env) {
         if (env != null && !env.isEmpty()) {
            if (this.systemEnv == null) {
               this.systemEnv = new HashMap<>();
            }

            env.forEach((k, v) -> {
               if (k != null && v != null) {
                  this.systemEnv.put(k, v);
               }
            });
         }

         return this;
      }

      public Runner.Builder outputMask(UnaryOperator<String> mask) {
         this.outputMask = mask;
         return this;
      }

      public Runner.Builder logLevel(LogLevel level) {
         if (level != null) {
            this.logLevel = level;
         }

         return this;
      }

      public Runner.Builder logLevel(String level) {
         if (level != null) {
            this.logLevel = LogLevel.valueOf(level.toUpperCase());
         }

         return this;
      }

      public Runner.Builder consoleLevel(String level) {
         this.consoleLevel = level;
         return this;
      }

      public Runner.Builder outputDir(String dir) {
         if (dir != null) {
            this.outputDir = Path.of(dir);
         }

         return this;
      }

      public Runner.Builder outputDir(Path dir) {
         if (dir != null) {
            this.outputDir = dir;
         }

         return this;
      }

      public Runner.Builder workingDir(String dir) {
         if (dir != null) {
            this.workingDir = Path.of(dir).toAbsolutePath().normalize();
         }

         return this;
      }

      public Runner.Builder workingDir(Path dir) {
         if (dir != null) {
            this.workingDir = dir.toAbsolutePath().normalize();
         }

         return this;
      }

      public Runner.Builder outputHtmlReport(boolean enabled) {
         this.outputHtmlReport = enabled;
         return this;
      }

      public Runner.Builder outputJsonLines(boolean enabled) {
         this.outputJsonLines = enabled;
         return this;
      }

      public Runner.Builder outputJunitXml(boolean enabled) {
         this.outputJunitXml = enabled;
         return this;
      }

      public Runner.Builder outputCucumberJson(boolean enabled) {
         this.outputCucumberJson = enabled;
         return this;
      }

      public Runner.Builder backupOutputDir(boolean enabled) {
         this.backupOutputDir = enabled;
         return this;
      }

      public Runner.Builder retainCallResults(boolean enabled) {
         this.retainCallResults = enabled;
         return this;
      }

      public Runner.Builder retainStepLogs(boolean enabled) {
         this.retainStepLogs = enabled;
         return this;
      }

      public Runner.Builder outputConsoleSummary(boolean enabled) {
         this.outputConsoleSummary = enabled;
         return this;
      }

      public Runner.Builder captureStepLogs(boolean enabled) {
         this.captureStepLogs = enabled;
         return this;
      }

      public Runner.Builder dryRun(boolean enabled) {
         this.dryRun = enabled;
         return this;
      }

      public Runner.Builder global(String name, Object instance) {
         this.globals.put(name, instance);
         return this;
      }

      public Runner.Builder listener(RunListener listener) {
         if (listener != null) {
            this.listeners.add(listener);
         }

         return this;
      }

      public Runner.Builder onSuite(Consumer<Suite> consumer) {
         this.suiteConsumer = consumer;
         return this;
      }

      public Runner.Builder listeners(Collection<RunListener> values) {
         if (values != null) {
            this.listeners.addAll(values);
         }

         return this;
      }

      public Runner.Builder listenerFactory(RunListenerFactory factory) {
         if (factory != null) {
            this.listenerFactories.add(factory);
         }

         return this;
      }

      public Runner.Builder listenerFactory(String className) {
         if (className != null && !className.isEmpty()) {
            try {
               Class<?> clazz = Class.forName(className);
               Object instance = clazz.getDeclaredConstructor().newInstance();
               if (instance instanceof RunListenerFactory factory) {
                  this.listenerFactories.add(factory);
               } else {
                  if (!(instance instanceof RunListener listener)) {
                     throw new IllegalArgumentException("Class " + className + " must implement RunListenerFactory or RunListener");
                  }

                  this.listeners.add(listener);
               }
            } catch (ClassNotFoundException var6) {
               throw new IllegalArgumentException("Class not found: " + className, var6);
            } catch (Exception var7) {
               throw new IllegalArgumentException("Failed to instantiate " + className, var7);
            }
         }

         return this;
      }

      public Runner.Builder resultListener(ResultListener listener) {
         if (listener != null) {
            this.resultListeners.add(listener);
         }

         return this;
      }

      public Runner.Builder resultListeners(Collection<ResultListener> values) {
         if (values != null) {
            this.resultListeners.addAll(values);
         }

         return this;
      }

      public Runner.Builder driverProvider(DriverProvider provider) {
         this.driverProvider = provider;
         return this;
      }

      public Runner.Builder pool() {
         this.poolSize = -1;
         return this;
      }

      public Runner.Builder pool(int size) {
         this.poolSize = size;
         return this;
      }

      public Runner.Builder httpClientFactory(HttpClientFactory factory) {
         this.httpClientFactory = factory;
         return this;
      }

      public Runner.Builder skipTagFiltering(boolean skip) {
         this.skipTagFiltering = skip;
         return this;
      }

      public <T> Runner.Builder debugSupport(RunInterceptor<T> interceptor, DebugPointFactory<T> factory) {
         this.debugInterceptor = interceptor;
         this.debugPointFactory = factory;
         return this;
      }

      public Runner.Builder callSingleCache(Map<String, Object> cache, ReentrantLock lock) {
         this.callSingleCache = cache;
         this.callSingleLock = lock;
         return this;
      }

      public Runner.Builder callOnceCacheStore(Map<String, Map<String, Object>> cacheStore, Map<String, ReentrantLock> lockStore) {
         this.callOnceCacheStore = cacheStore;
         this.callOnceLockStore = lockStore;
         return this;
      }

      public Runner.Builder setupOnceCacheStore(Map<String, Map<String, Object>> cacheStore) {
         this.setupOnceCacheStore = cacheStore;
         return this;
      }

      public List<Feature> getResolvedFeatures() {
         return this.resolveFeaturesAt(this.workingDir != null ? this.workingDir : FileUtils.WORKING_DIR.toPath(), null);
      }

      List<Feature> resolveFeaturesAt(Path root, Path classpathRoot) {
         if (this.resolvedFeatures == null) {
            this.resolvedFeatures = new ArrayList<>();

            for (Feature feature : this.features) {
               this.resolvedFeatures.add(this.reRoot(feature, root, classpathRoot));
            }

            for (String path : this.paths) {
               this.resolveFeatures(path, this.resolvedFeatures, root, classpathRoot);
            }
         }

         return Collections.unmodifiableList(this.resolvedFeatures);
      }

      private Feature reRoot(Feature feature, Path root, Path classpathRoot) {
         Resource resource = feature.getResource();
         Path cpRoot = classpathRoot != null ? classpathRoot : root;
         if (resource != null
            && resource.isFile()
            && root != null
            && (this.overlays.containsKey(resource.getPath()) || !root.equals(resource.getRoot()) || !cpRoot.equals(resource.getClasspathRoot()))) {
            try {
               byte[] bytes = this.overlays.get(resource.getPath());
               if (bytes == null && resource instanceof PathResource pr && pr.isOverlay()) {
                  bytes = pr.getBytes();
               }

               return Feature.read(bytes != null ? Resource.overlay(resource.getPath(), bytes, root, cpRoot) : Resource.from(resource.getPath(), root, cpRoot));
            } catch (Exception var8) {
               return feature;
            }
         } else {
            return feature;
         }
      }

      String getEnv() {
         return this.env;
      }

      List<String> getTags() {
         return this.tags;
      }

      String getConfigDir() {
         return this.configDir;
      }

      Path getOutputDir() {
         return this.outputDir;
      }

      Path getWorkingDir() {
         return this.workingDir;
      }

      boolean isSkipBootFile() {
         return this.skipBootFile;
      }

      boolean isDryRun() {
         return this.dryRun;
      }

      boolean isOutputHtmlReport() {
         return this.outputHtmlReport;
      }

      boolean isOutputJsonLines() {
         return this.outputJsonLines;
      }

      boolean isOutputJunitXml() {
         return this.outputJunitXml;
      }

      boolean isOutputCucumberJson() {
         return this.outputCucumberJson;
      }

      boolean isBackupOutputDir() {
         return this.backupOutputDir;
      }

      boolean isOutputConsoleSummary() {
         return this.outputConsoleSummary;
      }

      boolean isRetainCallResults() {
         return this.retainCallResults;
      }

      boolean isRetainStepLogs() {
         return this.retainStepLogs;
      }

      Boolean getCaptureStepLogs() {
         return this.captureStepLogs;
      }

      Map<String, String> getSystemProperties() {
         return this.systemProperties;
      }

      Map<String, String> getSystemEnv() {
         return this.systemEnv;
      }

      UnaryOperator<String> getOutputMask() {
         return this.outputMask;
      }

      Map<Path, byte[]> getOverlays() {
         return this.overlays;
      }

      Map<String, Object> getProgrammaticGlobals() {
         return this.globals;
      }

      List<RunListener> getListeners() {
         return this.listeners;
      }

      List<RunListenerFactory> getListenerFactories() {
         return this.listenerFactories;
      }

      List<ResultListener> getResultListeners() {
         return this.resultListeners;
      }

      DriverProvider getDriverProvider() {
         return this.driverProvider;
      }

      HttpClientFactory getHttpClientFactory() {
         return this.httpClientFactory;
      }

      boolean isSkipTagFiltering() {
         return this.skipTagFiltering;
      }

      Map<String, Set<Integer>> getLineFilters() {
         return this.lineFilters;
      }

      String getScenarioName() {
         return this.scenarioName;
      }

      RunInterceptor<?> getDebugInterceptor() {
         return this.debugInterceptor;
      }

      DebugPointFactory<?> getDebugPointFactory() {
         return this.debugPointFactory;
      }

      List<String> getPaths() {
         return this.paths;
      }

      Map<String, Object> getCallSingleCache() {
         return this.callSingleCache;
      }

      ReentrantLock getCallSingleLock() {
         return this.callSingleLock;
      }

      Map<String, Map<String, Object>> getCallOnceCacheStore() {
         return this.callOnceCacheStore;
      }

      Map<String, ReentrantLock> getCallOnceLockStore() {
         return this.callOnceLockStore;
      }

      Map<String, Map<String, Object>> getSetupOnceCacheStore() {
         return this.setupOnceCacheStore;
      }

      List<Feature> getFeatures() {
         return this.features;
      }

      LogLevel getLogLevel() {
         return this.logLevel;
      }

      void setEnv(String env) {
         this.env = env;
      }

      void setTags(List<String> tags) {
         this.tags = tags;
      }

      void setScenarioName(String name) {
         this.scenarioName = name;
      }

      void setConfigDir(String dir) {
         this.configDir = dir;
      }

      void setSkipBootFile(boolean skip) {
         this.skipBootFile = skip;
      }

      void setOutputDir(Path dir) {
         this.outputDir = dir;
      }

      void setDryRun(boolean dryRun) {
         this.dryRun = dryRun;
      }

      void setOutputHtmlReport(boolean enabled) {
         this.outputHtmlReport = enabled;
      }

      void setOutputJsonLines(boolean enabled) {
         this.outputJsonLines = enabled;
      }

      void setOutputJunitXml(boolean enabled) {
         this.outputJunitXml = enabled;
      }

      void setOutputCucumberJson(boolean enabled) {
         this.outputCucumberJson = enabled;
      }

      void setLogLevel(LogLevel level) {
         if (level != null) {
            this.logLevel = level;
         }
      }

      void clearPaths() {
         this.paths.clear();
         this.features.clear();
         this.lineFilters.clear();
         this.resolvedFeatures = null;
      }

      public SuiteResult parallel(int threadCount) {
         int effectiveThreads = threadCount;
         if (this.debugInterceptor == null) {
            effectiveThreads = KarateOptionsHandler.apply(this, threadCount);
         }

         LogContext.setLogLevel(this.logLevel);
         if (this.consoleLevel != null) {
            LogContext.setRuntimeLogLevel(this.consoleLevel);
         }

         SuiteResult debugResult = this.startDebugServerIfRequired(effectiveThreads);
         if (debugResult != null) {
            return debugResult;
         } else {
            Suite suite = new Suite(this, Math.max(1, effectiveThreads));
            if (this.suiteConsumer != null) {
               this.suiteConsumer.accept(suite);
            }

            SuiteResult result = suite.run();
            if (this.outputConsoleSummary) {
               result.printSummary(this.env, effectiveThreads);
            }

            return result;
         }
      }

      private SuiteResult startDebugServerIfRequired(int threadCount) {
         if (this.debugInterceptor != null) {
            return null;
         } else {
            String debugPortStr = System.getProperty("karate.debug.port");
            if (debugPortStr != null && !debugPortStr.trim().isEmpty()) {
               int debugPort = 0;

               try {
                  debugPort = Integer.parseInt(debugPortStr.trim());
               } catch (Exception var9) {
               }

               if (this.systemProperties != null) {
                  this.systemProperties.forEach(System::setProperty);
               }

               String[] args = this.buildDebugArgs(debugPort, threadCount);
               logger.debug("karate.debug.port detected, delegating to debug server: {}", Arrays.toString((Object[])args));

               try {
                  Class<?> mainClass = Class.forName("io.karatelabs.debug.Main");
                  Method runMethod = mainClass.getMethod("run", String[].class);
                  return (SuiteResult)runMethod.invoke(null, args);
               } catch (ClassNotFoundException var7) {
                  logger.warn("karate.debug.port is set but karate-ide JAR is not on the classpath");
                  return null;
               } catch (Exception var8) {
                  throw new RuntimeException("failed to start debug server", var8);
               }
            } else {
               return null;
            }
         }
      }

      private String[] buildDebugArgs(int debugPort, int threadCount) {
         List<String> args = new ArrayList<>();
         args.add("-d");
         args.add(String.valueOf(debugPort));
         if (this.env != null) {
            args.add("-e");
            args.add(this.env);
         }

         if (this.tags != null) {
            for (String tag : this.tags) {
               args.add("-t");
               args.add(tag);
            }
         }

         if (threadCount > 1) {
            args.add("-T");
            args.add(String.valueOf(threadCount));
         }

         args.addAll(this.paths);
         return args.toArray(new String[0]);
      }

      public Suite buildSuite() {
         return this.buildSuite(1);
      }

      Suite buildSuite(int threadCount) {
         LogContext.setLogLevel(this.logLevel);
         if (this.consoleLevel != null) {
            LogContext.setRuntimeLogLevel(this.consoleLevel);
         }

         return new Suite(this, Math.max(1, threadCount));
      }

      private void resolveFeatures(String path, List<Feature> target, Path root, Path classpathRoot) {
         String actualPath = path;
         Set<Integer> lines = new HashSet<>();
         if (path.contains(".feature:") && !path.startsWith("classpath:")) {
            int featureExtIndex = path.indexOf(".feature:");
            int colonIndex = featureExtIndex + ".feature".length();
            String potentialPath = path.substring(0, colonIndex);
            String remainder = path.substring(colonIndex + 1);
            if (remainder.matches("\\d+(:\\d+)*")) {
               actualPath = potentialPath;

               for (String lineStr : remainder.split(":")) {
                  try {
                     lines.add(Integer.parseInt(lineStr));
                  } catch (NumberFormatException var17) {
                     actualPath = path;
                     lines.clear();
                     break;
                  }
               }
            }
         } else if (path.startsWith("classpath:") && path.contains(".feature:")) {
            int featureExtIndex = path.indexOf(".feature:");
            int colonIndex = featureExtIndex + ".feature".length();
            String potentialPath = path.substring(0, colonIndex);
            String remainder = path.substring(colonIndex + 1);
            if (remainder.matches("\\d+(:\\d+)*")) {
               actualPath = potentialPath;

               for (String lineStr : remainder.split(":")) {
                  try {
                     lines.add(Integer.parseInt(lineStr));
                  } catch (NumberFormatException var16) {
                     actualPath = path;
                     lines.clear();
                     break;
                  }
               }
            }
         }

         File file = new File(actualPath);
         if (file.isDirectory()) {
            this.resolveDirectory(file, target, root, classpathRoot);
         } else if ((file.exists() || this.overlays.containsKey(file.toPath().toAbsolutePath().normalize())) && file.getName().endsWith(".feature")) {
            Feature feature = this.readFeature(file.toPath(), root, classpathRoot);
            target.add(feature);
            if (!lines.isEmpty()) {
               this.lineFilters.put(feature.getResource().getUri().toString(), lines);
            }
         } else if (actualPath.startsWith("classpath:")) {
            String classpathPath = actualPath.substring("classpath:".length());
            if (classpathPath.startsWith("/")) {
               classpathPath = classpathPath.substring(1);
            }

            if (classpathPath.endsWith(".feature")) {
               Resource resource = Resource.path(actualPath, root, classpathRoot);
               Feature feature = Feature.read(resource);
               target.add(feature);
               if (!lines.isEmpty()) {
                  this.lineFilters.put(feature.getResource().getUri().toString(), lines);
               }
            } else {
               for (Resource resource : Resource.scanClasspath(classpathPath, "feature", null, root)) {
                  Feature feature = Feature.read(resource.getPath() == null ? resource : Resource.from(resource.getPath(), root, classpathRoot));
                  target.add(feature);
               }
            }
         }
      }

      private void resolveDirectory(File dir, List<Feature> target, Path root, Path classpathRoot) {
         File[] files = dir.listFiles();
         if (files != null) {
            for (File file : files) {
               if (file.isDirectory()) {
                  this.resolveDirectory(file, target, root, classpathRoot);
               } else if (file.getName().endsWith(".feature")) {
                  Feature feature = this.readFeature(file.toPath(), root, classpathRoot);
                  target.add(feature);
               }
            }
         }
      }

      private Feature readFeature(Path file, Path root, Path classpathRoot) {
         byte[] bytes = this.overlays.get(file.toAbsolutePath().normalize());
         return Feature.read(bytes != null ? Resource.overlay(file, bytes, root, classpathRoot) : Resource.from(file, root, classpathRoot));
      }

      @Override
      public String toString() {
         return "Runner.Builder{paths=" + this.paths + ", features=" + this.features.size() + "}";
      }
   }
}
