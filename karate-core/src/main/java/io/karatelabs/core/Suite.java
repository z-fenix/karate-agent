package io.karatelabs.core;

import io.karatelabs.common.FileUtils;
import io.karatelabs.common.Resource;
import io.karatelabs.common.ResourceNotFoundException;
import io.karatelabs.driver.DriverProvider;
import io.karatelabs.driver.PooledDriverProvider;
import io.karatelabs.gherkin.ExamplesTable;
import io.karatelabs.gherkin.Feature;
import io.karatelabs.gherkin.FeatureSection;
import io.karatelabs.gherkin.Scenario;
import io.karatelabs.gherkin.Tag;
import io.karatelabs.http.HttpClientFactory;
import io.karatelabs.js.DebugPointFactory;
import io.karatelabs.js.RunInterceptor;
import io.karatelabs.output.CucumberJsonReportListener;
import io.karatelabs.output.HtmlReportListener;
import io.karatelabs.output.HtmlReportWriter;
import io.karatelabs.output.JunitXmlReportListener;
import io.karatelabs.output.LogContext;
import io.karatelabs.output.ResultListener;
import io.karatelabs.parser.JsParser;
import io.karatelabs.parser.Node;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.UnaryOperator;
import org.slf4j.Logger;

public class Suite {
   private static final Logger logger = LogContext.RUNTIME_LOGGER;
   public static final String KARATE_JSON_SUBFOLDER = "karate-json";
   public final List<Feature> features;
   public final String env;
   public final String tagSelector;
   public final int threadCount;
   public final boolean parallel;
   public final boolean dryRun;
   public final String configPath;
   public final Path outputDir;
   public final Path workingDir;
   public final Path root;
   public final Path classpathRoot;
   public final boolean outputHtmlReport;
   public final boolean outputJsonLines;
   public final boolean outputJunitXml;
   public final boolean outputCucumberJson;
   public final boolean backupReportDir;
   public final boolean outputConsoleSummary;
   public final boolean retainCallResults;
   public final boolean retainStepLogs;
   private final Boolean captureStepLogs;
   public final Map<String, String> systemProperties;
   private final Map<String, String> systemEnv;
   public final UnaryOperator<String> outputMask;
   private final Map<Path, byte[]> overlays;
   public final List<RunListener> listeners;
   public final List<RunListenerFactory> listenerFactories;
   public final DriverProvider driverProvider;
   public final HttpClientFactory httpClientFactory;
   public final boolean skipTagFiltering;
   public final Map<String, Set<Integer>> lineFilters;
   public final String scenarioName;
   public final RunInterceptor<?> debugInterceptor;
   public final DebugPointFactory<?> debugPointFactory;
   public final Resource baseResource;
   public final Resource configResource;
   public final Resource configEnvResource;
   private final Map<Resource, Suite.ConfigScript> configScripts = new ConcurrentHashMap<>();
   private final List<ResultListener> resultListeners;
   private final Map<String, Object> CALLSINGLE_CACHE;
   private final ReentrantLock callSingleLock;
   private final Map<String, Map<String, Object>> callOnceCacheStore;
   private final Map<String, ReentrantLock> callOnceLockStore;
   private final Map<String, Map<String, Object>> setupOnceCacheStore;
   private final ScenarioLockManager lockManager = new ScenarioLockManager();
   private final AtomicBoolean aborted = new AtomicBoolean();
   private final List<MockServer> servers = new CopyOnWriteArrayList<>();
   private final boolean sharedCallSingleCache;
   private final ThreadLocal<List<RunListener>> threadListeners = new ThreadLocal<>();
   private final Map<String, ChannelFactory> channelFactories = new ConcurrentHashMap<>();
   private final AtomicInteger executionSequence = new AtomicInteger();
   private volatile ExecutorService scenarioExecutor;
   private volatile Semaphore scenarioSemaphore;
   private volatile Queue<Integer> availableLanes;
   private final ThreadLocal<Integer> currentLane = new ThreadLocal<>();
   private SuiteResult result;
   private PerfHook perfHook;
   private final BootBinding bootBinding;
   private final Map<String, Object> globals = new LinkedHashMap<>();
   private final Map<String, ReportAssets> reportAssets = new LinkedHashMap<>();
   private final List<Map<String, Object>> summaryCards = new ArrayList<>();
   private final List<RunListener> mutableListeners = new ArrayList<>();
   private static final Set<String> RESERVED_GLOBAL_NAMES = Set.of("karate", "read", "driver");
   private static final DateTimeFormatter BACKUP_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

   public int nextExecutionIndex() {
      return this.executionSequence.incrementAndGet();
   }

   Suite(Runner.Builder builder, int threadCount) {
      this.sharedCallSingleCache = builder.getCallSingleCache() != null;
      this.CALLSINGLE_CACHE = (Map<String, Object>)(builder.getCallSingleCache() != null ? builder.getCallSingleCache() : new ConcurrentHashMap<>());
      this.callSingleLock = builder.getCallSingleLock() != null ? builder.getCallSingleLock() : new ReentrantLock();
      this.callOnceCacheStore = (Map<String, Map<String, Object>>)(builder.getCallOnceCacheStore() != null
         ? builder.getCallOnceCacheStore()
         : new ConcurrentHashMap<>());
      this.callOnceLockStore = (Map<String, ReentrantLock>)(builder.getCallOnceLockStore() != null ? builder.getCallOnceLockStore() : new ConcurrentHashMap<>());
      this.setupOnceCacheStore = (Map<String, Map<String, Object>>)(builder.getSetupOnceCacheStore() != null
         ? builder.getSetupOnceCacheStore()
         : new ConcurrentHashMap<>());
      this.env = builder.getEnv();
      this.tagSelector = builder.getTags() != null && !builder.getTags().isEmpty() ? TagSelector.fromKarateOptionsTags(builder.getTags()) : null;
      this.threadCount = Math.max(1, threadCount);
      this.parallel = this.threadCount > 1;
      this.dryRun = builder.isDryRun();
      this.configPath = this.resolveConfigPath(builder.getConfigDir());
      this.outputDir = builder.getOutputDir() != null ? builder.getOutputDir() : Path.of(FileUtils.getBuildDir(), "karate-reports");
      this.outputHtmlReport = builder.isOutputHtmlReport();
      this.outputJsonLines = builder.isOutputJsonLines();
      this.outputJunitXml = builder.isOutputJunitXml();
      this.outputCucumberJson = builder.isOutputCucumberJson();
      this.backupReportDir = builder.isBackupOutputDir();
      this.outputConsoleSummary = builder.isOutputConsoleSummary();
      this.retainCallResults = builder.isRetainCallResults();
      this.retainStepLogs = builder.isRetainStepLogs();
      this.captureStepLogs = builder.getCaptureStepLogs();
      this.systemProperties = builder.getSystemProperties() != null ? Collections.unmodifiableMap(new HashMap<>(builder.getSystemProperties())) : null;
      this.systemEnv = builder.getSystemEnv() != null ? Map.copyOf(builder.getSystemEnv()) : Map.of();
      this.outputMask = builder.getOutputMask();
      this.overlays = Map.copyOf(builder.getOverlays());
      this.listeners = List.copyOf(builder.getListeners());
      this.listenerFactories = List.copyOf(builder.getListenerFactories());
      builder.getProgrammaticGlobals().forEach(this::registerGlobal);
      this.resultListeners = new ArrayList<>(builder.getResultListeners());
      DriverProvider configuredProvider = builder.getDriverProvider();
      if (configuredProvider != null) {
         this.driverProvider = configuredProvider;
      } else {
         this.driverProvider = new PooledDriverProvider(threadCount);
         logger.debug("Auto-created PooledDriverProvider with pool size: {}", threadCount);
      }

      this.httpClientFactory = builder.getHttpClientFactory();
      this.skipTagFiltering = builder.isSkipTagFiltering();
      String rawScenarioName = builder.getScenarioName();
      this.scenarioName = rawScenarioName != null && !rawScenarioName.trim().isEmpty() ? rawScenarioName.trim() : null;
      this.debugInterceptor = builder.getDebugInterceptor();
      this.debugPointFactory = builder.getDebugPointFactory();
      this.root = computeRoot(builder, this.configPath);
      this.bootBinding = builder.isSkipBootFile() ? null : BootLoader.evalIfPresent(this, bootstrapWorkingDir(builder), this.env);
      String declaredClasspathDir = this.bootBinding == null ? null : this.bootBinding.getClasspathDir();
      this.classpathRoot = declaredClasspathDir == null ? this.root : this.root.resolve(Resource.stripLeadingSlashes(declaredClasspathDir)).normalize();
      this.workingDir = builder.getWorkingDir() != null ? builder.getWorkingDir() : (declaredClasspathDir != null ? this.root : FileUtils.WORKING_DIR.toPath());
      this.baseResource = this.tryLoadConfigResource(this.getBasePath(this.configPath), false);
      if (this.baseResource != null) {
         logger.info("{} processed", this.getBasePath(this.configPath));
      }

      this.configResource = this.tryLoadConfigResource(this.configPath, true);
      if (this.configResource != null) {
         logger.info("{} processed", this.configPath);
      }

      if (this.env != null && !this.env.isEmpty()) {
         String envConfigPath = this.configPath.replace(".js", "-" + this.env + ".js");
         this.configEnvResource = this.tryLoadConfigResource(envConfigPath, false);
         if (this.configEnvResource != null) {
            logger.info("{} processed", envConfigPath);
         }
      } else {
         this.configEnvResource = null;
      }

      this.features = List.copyOf(builder.resolveFeaturesAt(this.root, this.classpathRoot));
      this.lineFilters = builder.getLineFilters() != null ? Collections.unmodifiableMap(new HashMap<>(builder.getLineFilters())) : Collections.emptyMap();
   }

   private String resolveConfigPath(String configDir) {
      if (configDir == null) {
         return "classpath:karate-config.js";
      } else {
         return configDir.endsWith(".js") ? configDir : configDir + "/karate-config.js";
      }
   }

   private static Path bootstrapWorkingDir(Runner.Builder builder) {
      if (builder.getWorkingDir() != null) {
         return builder.getWorkingDir();
      } else {
         String configDir = builder.getConfigDir();
         if (configDir != null && !configDir.startsWith("classpath:")) {
            String fsPath = configDir.startsWith("file:") ? configDir.substring("file:".length()) : configDir;
            Path given = Path.of(fsPath).toAbsolutePath().normalize();
            Path dir = fsPath.endsWith(".js") ? given.getParent() : given;
            if (dir != null) {
               return dir;
            }
         }

         return FileUtils.WORKING_DIR.toPath();
      }
   }

   private static Path computeRoot(Runner.Builder builder, String configPath) {
      String configDir = builder.getConfigDir();
      if (configDir != null && !configDir.startsWith("classpath:")) {
         String fsPath = configDir.startsWith("file:") ? configDir.substring("file:".length()) : configDir;
         Path given = Path.of(fsPath).toAbsolutePath().normalize();
         Path dir = fsPath.endsWith(".js") ? given.getParent() : given;
         if (dir != null) {
            return dir;
         }
      }

      if (configPath != null) {
         try {
            Resource probe = Resource.path(configPath);
            Path found = probe.isFile() ? probe.getPath() : null;
            if (found != null && found.getFileSystem() == FileSystems.getDefault() && found.getParent() != null) {
               return found.getParent().toAbsolutePath().normalize();
            }
         } catch (Exception var6) {
         }
      }

      return builder.getWorkingDir() != null ? builder.getWorkingDir() : FileUtils.WORKING_DIR.toPath();
   }

   private String getBasePath(String configPath) {
      return configPath.endsWith("karate-config.js") ? configPath.replace("karate-config.js", "karate-base.js") : "classpath:karate-base.js";
   }

   private Resource tryLoadConfigResource(String path, boolean warnIfMissing) {
      try {
         Resource resource = Resource.optional(path);
         if (resource != null && resource.exists()) {
            return resource.isFile() && resource.getPath() != null && resource.getPath().getFileSystem() == FileSystems.getDefault()
               ? this.configFile(resource.getPath())
               : resource;
         }
      } catch (ResourceNotFoundException var8) {
      } catch (Exception var9) {
         logger.warn("Error loading config {}: {}", path, var9.getMessage());
         return null;
      }

      String fileName = this.configFallbackName(path);
      if (fileName != null) {
         for (Path dir : this.classpathRoot.equals(this.workingDir) ? List.of(this.workingDir) : List.of(this.workingDir, this.classpathRoot)) {
            try {
               Path candidate = dir.resolve(fileName);
               if (Files.exists(candidate) || this.overlays.containsKey(candidate.toAbsolutePath().normalize())) {
                  return this.configFile(candidate);
               }
            } catch (Exception var7) {
               logger.debug("Could not load config from {}: {}", dir, var7.getMessage());
            }
         }
      }

      if (warnIfMissing) {
         String probed = fileName == null
            ? path
            : path + " (also looked in: " + (this.classpathRoot.equals(this.workingDir) ? this.workingDir : this.workingDir + ", " + this.classpathRoot) + ")";
         logger.info("{} not found - no config variables will be set", probed);
      }

      return null;
   }

   private Resource configFile(Path file) {
      byte[] bytes = this.overlays.get(file.toAbsolutePath().normalize());
      return bytes != null ? Resource.overlay(file, bytes, this.root, this.classpathRoot) : Resource.from(file, this.root, this.classpathRoot);
   }

   Suite.ConfigScript configScript(Resource resource) {
      return this.configScripts.computeIfAbsent(resource, Suite.ConfigScript::new);
   }

   private String configFallbackName(String path) {
      String stripped = Resource.removePrefix(path);
      Path asGiven = Path.of(stripped);
      if (!asGiven.isAbsolute()) {
         return stripped;
      } else {
         Path normalized = asGiven.normalize();
         return normalized.startsWith(this.root) ? this.root.relativize(normalized).toString() : null;
      }
   }

   public SuiteResult run() {
      this.result = new SuiteResult();
      this.result.setStartTime(System.currentTimeMillis());
      this.result.setReportDir(this.outputDir);
      this.result.setHtmlReportEnabled(this.outputHtmlReport);
      this.result.setDryRun(this.dryRun);
      this.clearScenarioSelectionCache();
      if (this.backupReportDir) {
         this.backupReportDirIfExists();
      }

      if (this.outputHtmlReport) {
         this.resultListeners.add(new HtmlReportListener(this.outputDir, this.env));
      }

      if (this.outputCucumberJson) {
         this.resultListeners.add(new CucumberJsonReportListener(this.outputDir));
      }

      if (this.outputJunitXml) {
         this.resultListeners.add(new JunitXmlReportListener(this.outputDir));
      }

      JsonLinesEventWriter jsonlWriter = null;
      boolean jsonlEnabled = this.outputJsonLines;
      if (!jsonlEnabled && this.bootBinding != null) {
         for (Ext ext : this.bootBinding.getExts()) {
            if (ext.requiresJsonlEvents()) {
               logger.info("JSONL event stream auto-enabled by ext: {}", ext.getClass().getSimpleName());
               jsonlEnabled = true;
               break;
            }
         }
      }

      if (jsonlEnabled) {
         jsonlWriter = new JsonLinesEventWriter(this.outputDir, this.env, this.threadCount, this.outputMask);

         try {
            jsonlWriter.init();
            this.addJsonlListener(jsonlWriter);
         } catch (Exception var21) {
            logger.warn("Failed to initialize JSONL event stream: {}", var21.getMessage());
            jsonlWriter = null;
         }
      }

      try {
         HtmlReportWriter.resetEmbedCounter();

         for (ResultListener listener : this.resultListeners) {
            listener.onSuiteStart(this);
         }

         this.fireEvent(SuiteRunEvent.enter(this));
         if (this.parallel && this.threadCount > 1) {
            this.runParallel();
         } else {
            this.runSequential();
         }

         this.result.setEndTime(System.currentTimeMillis());
         this.fireEvent(SuiteRunEvent.exit(this, this.result));
      } finally {
         try {
            this.teardown(jsonlWriter);
         } finally {
            this.stopServers();
         }
      }

      return this.result;
   }

   private void teardown(JsonLinesEventWriter jsonlWriter) {
      if (this.result.getEndTime() == 0L) {
         this.result.setEndTime(System.currentTimeMillis());
      }

      if (this.driverProvider != null) {
         this.driverProvider.shutdown();
      }

      if (jsonlWriter != null) {
         try {
            this.removeJsonlListener(jsonlWriter);
            jsonlWriter.close();
         } catch (Exception var6) {
            logger.warn("Failed to close JSONL event stream: {}", var6.getMessage());
         }
      }

      if (this.bootBinding != null) {
         for (Ext ext : this.bootBinding.getExts()) {
            try {
               ext.onShutdown();
            } catch (Exception var5) {
               logger.warn("ext onShutdown failed ({}): {}", ext.getClass().getName(), var5.getMessage());
            }

            this.removeJsonlListener(ext);
         }
      }

      for (ResultListener listener : this.resultListeners) {
         listener.onSuiteEnd(this.result);
      }
   }

   private void addJsonlListener(RunListener listener) {
      synchronized (this.mutableListeners) {
         this.mutableListeners.add(listener);
      }
   }

   public void registerExtListener(Ext ext) {
      this.addJsonlListener(ext);
   }

   public Path getWorkingDir() {
      return this.workingDir;
   }

   public Path getRoot() {
      return this.root;
   }

   public Path getClasspathRoot() {
      return this.classpathRoot;
   }

   public Path getConfigDir() {
      if (this.configResource != null) {
         try {
            Path p = this.configResource.getPath();
            if (p != null && p.getParent() != null) {
               return p.getParent();
            }
         } catch (RuntimeException var3) {
         }
      }

      if (this.configPath != null && !this.configPath.startsWith("classpath:")) {
         String fsPath = this.configPath.startsWith("file:") ? this.configPath.substring("file:".length()) : this.configPath;
         Path parent = Path.of(fsPath).getParent();
         if (parent != null) {
            return parent;
         }
      }

      return this.workingDir;
   }

   public BootBinding getBootBinding() {
      return this.bootBinding;
   }

   public void registerChannelFactory(String type, ChannelFactory factory) {
      this.channelFactories.put(type, factory);
   }

   public ChannelFactory getChannelFactory(String type) {
      return this.channelFactories.get(type);
   }

   public void registerGlobal(String name, Object instance) {
      if (name == null || name.isBlank()) {
         throw new IllegalArgumentException("registerGlobal: name is null or empty");
      } else if (RESERVED_GLOBAL_NAMES.contains(name)) {
         throw new RuntimeException("ext global '" + name + "' collides with built-in '" + name + "'");
      } else {
         this.globals.put(name, instance);
      }
   }

   public void registerGlobal(String name, ExtGlobalFactory factory) {
      if (name == null || name.isBlank()) {
         throw new IllegalArgumentException("registerGlobal: name is null or empty");
      } else if (RESERVED_GLOBAL_NAMES.contains(name)) {
         throw new RuntimeException("ext global '" + name + "' collides with built-in '" + name + "'");
      } else {
         this.globals.put(name, factory);
      }
   }

   public Object getGlobal(String name) {
      return this.globals.get(name);
   }

   public Map<String, Object> getGlobals() {
      return Collections.unmodifiableMap(this.globals);
   }

   public void registerReportAssets(ReportAssets assets, ClassLoader classLoader) {
      if (assets == null) {
         throw new IllegalArgumentException("registerReportAssets: assets is null");
      } else {
         assets.validateAndBind(classLoader);
         this.reportAssets.put(assets.name(), assets);
      }
   }

   public Map<String, ReportAssets> getReportAssets() {
      return Collections.unmodifiableMap(this.reportAssets);
   }

   public void addSummaryCard(Map<String, Object> card) {
      if (card != null) {
         this.summaryCards.add(card);
      }
   }

   public List<Map<String, Object>> getSummaryCards() {
      return Collections.unmodifiableList(this.summaryCards);
   }

   private void removeJsonlListener(RunListener listener) {
      synchronized (this.mutableListeners) {
         this.mutableListeners.remove(listener);
      }
   }

   private void runSequential() {
      this.initThreadListeners();

      try {
         for (Feature feature : this.features) {
            if (!this.isAborted() && !this.isFeatureIgnored(feature)) {
               FeatureResult featureResult = this.runFeatureSafely(feature);
               this.result.addFeatureResult(featureResult);
               if (this.outputConsoleSummary) {
                  featureResult.printSummary(this.dryRun);
               }
            }
         }
      } finally {
         this.cleanupThreadListeners();
      }
   }

   private FeatureResult runFeatureSafely(Feature feature) {
      long startTime = System.currentTimeMillis();
      FeatureRuntime fr = null;

      try {
         fr = new FeatureRuntime(this, feature);
         return fr.call();
      } catch (Exception var10) {
         logger.error("Unexpected error running feature '{}': {}", new Object[]{feature.getName(), var10.getMessage(), var10});
         if (fr != null && fr.exitFired()) {
            return fr.getResult();
         } else {
            FeatureResult result = FeatureResult.fromException(feature, var10, startTime);

            for (ScenarioResult sr : result.getScenarioResults()) {
               if (sr.getExecutionIndex() == 0) {
                  sr.setExecutionIndex(this.nextExecutionIndex());
               }
            }

            if (fr == null || !fr.exitFired()) {
               try {
                  this.fireEvent(FeatureRunEvent.exit(fr, result));
               } catch (RuntimeException var9) {
                  logger.error("FEATURE_EXIT listener failed for '{}': {}", feature.getName(), var9.getMessage());
               }
            }

            return result;
         }
      }
   }

   private void runParallel() {
      this.availableLanes = new ConcurrentLinkedQueue<>();

      for (int i = 1; i <= this.threadCount; i++) {
         this.availableLanes.add(i);
      }

      this.scenarioSemaphore = new Semaphore(this.threadCount);
      logger.info("Parallel execution initialized: threadCount={}, semaphore permits={}", this.threadCount, this.scenarioSemaphore.availablePermits());

      try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
         this.scenarioExecutor = executor;
         List<Future<FeatureResult>> futures = new ArrayList<>();

         for (Feature feature : this.features) {
            if (!this.isAborted() && !this.isFeatureIgnored(feature)) {
               Future<FeatureResult> future = executor.submit(() -> {
                  this.initThreadListeners();

                  FeatureResult var3;
                  try {
                     FeatureResult featureResult = this.runFeatureSafely(feature);
                     if (this.outputConsoleSummary) {
                        featureResult.printSummary(this.dryRun);
                     }

                     var3 = featureResult;
                  } finally {
                     this.cleanupThreadListeners();
                  }

                  return var3;
               });
               futures.add(future);
            }
         }

         for (Future<FeatureResult> future : futures) {
            try {
               FeatureResult featureResult = future.get();
               this.result.addFeatureResult(featureResult);
            } catch (Exception var12) {
               logger.error("Unexpected error collecting feature result: {}", var12.getMessage(), var12);
            }
         }
      } finally {
         this.scenarioExecutor = null;
         this.scenarioSemaphore = null;
      }
   }

   private boolean isFeatureIgnored(Feature feature) {
      if (!this.skipTagFiltering && !this.hasLineFilter(feature) && (this.scenarioName == null || !hasScenarioNamed(feature, this.scenarioName))) {
         for (Tag tag : feature.getTags()) {
            if ("ignore".equals(tag.getName())) {
               return true;
            }
         }
      }

      return this.allSectionsExcluded(feature);
   }

   private boolean hasLineFilter(Feature feature) {
      if (this.lineFilters.isEmpty()) {
         return false;
      } else {
         Set<Integer> lines = this.lineFilters.get(feature.getResource().getUri().toString());
         return lines != null && !lines.isEmpty();
      }
   }

   private static boolean hasScenarioNamed(Feature feature, String name) {
      List<FeatureSection> sections = feature.getSections();
      if (sections == null) {
         return false;
      } else {
         for (FeatureSection section : sections) {
            if (section.isOutline()) {
               String outlineName = section.getScenarioOutline().getName();
               if (outlineName != null && (outlineName.contains("<") || outlineName.contains("${") || outlineName.trim().equals(name))) {
                  return true;
               }
            } else {
               String sectionName = section.getScenario().getName();
               if (sectionName != null && sectionName.trim().equals(name)) {
                  return true;
               }
            }
         }

         return false;
      }
   }

   private boolean allSectionsExcluded(Feature feature) {
      if (this.tagSelector == null) {
         return false;
      } else if (!this.skipTagFiltering && this.scenarioName == null && !this.hasLineFilter(feature)) {
         List<FeatureSection> sections = feature.getSections();
         if (sections != null && !sections.isEmpty()) {
            List<Tag> featureTags = feature.getTags();
            boolean anyPassed = false;

            for (FeatureSection section : sections) {
               if (this.sectionCanMatch(section, featureTags)) {
                  anyPassed = true;
               }
            }

            return !anyPassed;
         } else {
            return false;
         }
      } else {
         return false;
      }
   }

   private boolean sectionCanMatch(FeatureSection section, List<Tag> featureTags) {
      if (section.isOutline()) {
         List<Tag> base = mergeTags(featureTags, section.getScenarioOutline().getTags());
         List<ExamplesTable> tables = section.getScenarioOutline().getExamplesTables();
         if (tables != null && !tables.isEmpty()) {
            boolean anyTablePasses = false;

            for (ExamplesTable table : tables) {
               if (this.tagSelectorPasses(mergeTags(base, table.getTags()))) {
                  anyTablePasses = true;
               }
            }

            return anyTablePasses;
         } else {
            return this.tagSelectorPasses(base);
         }
      } else {
         Scenario scenario = section.getScenario();
         boolean passes = this.tagSelectorPasses(mergeTags(featureTags, scenario.getTags()));
         scenario.setSelected(passes);
         return passes;
      }
   }

   private boolean tagSelectorPasses(List<Tag> tags) {
      return new TagSelector(tags).evaluate(this.tagSelector, this.env);
   }

   private void clearScenarioSelectionCache() {
      for (Feature feature : this.features) {
         List<FeatureSection> sections = feature.getSections();
         if (sections != null) {
            for (FeatureSection section : sections) {
               if (!section.isOutline() && section.getScenario() != null) {
                  section.getScenario().setSelected(null);
               }
            }
         }
      }
   }

   private static List<Tag> mergeTags(List<Tag> a, List<Tag> b) {
      if (a == null || a.isEmpty()) {
         return b == null ? Collections.emptyList() : b;
      } else if (b != null && !b.isEmpty()) {
         List<Tag> merged = new ArrayList<>(a.size() + b.size());
         merged.addAll(a);
         merged.addAll(b);
         return merged;
      } else {
         return a;
      }
   }

   void initThreadListeners() {
      if (!this.listenerFactories.isEmpty()) {
         List<RunListener> perThread = new ArrayList<>();

         for (RunListenerFactory factory : this.listenerFactories) {
            perThread.add(factory.create());
         }

         this.threadListeners.set(perThread);
      }
   }

   void cleanupThreadListeners() {
      this.threadListeners.remove();
   }

   boolean initThreadListenersIfAbsent() {
      if (!this.listenerFactories.isEmpty() && this.threadListeners.get() == null) {
         this.initThreadListeners();
         return true;
      } else {
         return false;
      }
   }

   public boolean fireEvent(RunEvent event) {
      if (event.getType() == RunEventType.FEATURE_EXIT
         && event instanceof FeatureRunEvent fre
         && fre.result() != null
         && fre.source() != null
         && fre.source().getCallDepth() == 0) {
         HtmlReportWriter.externalizeEmbeds(fre.result(), this.outputDir, this.outputMask);
      }

      boolean proceed = true;
      RuntimeException failed = null;

      for (RunListener listener : this.listeners) {
         try {
            if (!listener.onEvent(event)) {
               proceed = false;
            }
         } catch (RuntimeException var12) {
            failed = failed == null ? var12 : failed;
         }
      }

      synchronized (this.mutableListeners) {
         for (RunListener listener : this.mutableListeners) {
            try {
               if (!listener.onEvent(event)) {
                  proceed = false;
               }
            } catch (RuntimeException var10) {
               failed = failed == null ? var10 : failed;
            }
         }
      }

      List<RunListener> perThread = this.threadListeners.get();
      if (perThread != null) {
         for (RunListener listener : perThread) {
            try {
               if (!listener.onEvent(event)) {
                  proceed = false;
               }
            } catch (RuntimeException var9) {
               failed = failed == null ? var9 : failed;
            }
         }
      }

      if (failed != null) {
         throw failed;
      } else {
         return proceed;
      }
   }

   private void backupReportDirIfExists() {
      if (Files.exists(this.outputDir)) {
         String timestamp = LocalDateTime.now().format(BACKUP_DATE_FORMAT);
         String baseName = this.outputDir.getFileName() + "_" + timestamp;
         Path backupPath = this.outputDir.resolveSibling(baseName);

         for (int suffix = 1; Files.exists(backupPath); suffix++) {
            backupPath = this.outputDir.resolveSibling(baseName + "_" + suffix);
         }

         try {
            Files.move(this.outputDir, backupPath);
            logger.info("backed up existing output to: {}", backupPath);
         } catch (Exception var6) {
            logger.warn("Failed to backup existing dir '{}': {}", this.outputDir, var6.getMessage());
         }
      }
   }

   public String getOutputDir() {
      return this.outputDir.toString();
   }

   public Map<String, Object> getCallSingleCache() {
      return this.CALLSINGLE_CACHE;
   }

   public ReentrantLock getCallSingleLock() {
      return this.callSingleLock;
   }

   public Map<String, Object> getCallOnceCache(String featureKey) {
      return this.callOnceCacheStore.computeIfAbsent(featureKey, k -> new ConcurrentHashMap<>());
   }

   public ReentrantLock getCallOnceLock(String featureKey) {
      return this.callOnceLockStore.computeIfAbsent(featureKey, k -> new ReentrantLock());
   }

   public Map<String, Object> getSetupOnceCache(String featureKey) {
      return this.setupOnceCacheStore.computeIfAbsent(featureKey, k -> new ConcurrentHashMap<>());
   }

   public ScenarioLockManager getLockManager() {
      return this.lockManager;
   }

   public ExecutorService getScenarioExecutor() {
      return this.scenarioExecutor;
   }

   public Semaphore getScenarioSemaphore() {
      return this.scenarioSemaphore;
   }

   public void acquireLane() {
      if (this.availableLanes != null) {
         Integer lane = this.availableLanes.poll();
         if (lane != null) {
            this.currentLane.set(lane);
         }
      }
   }

   public void releaseLane() {
      if (this.availableLanes != null) {
         Integer lane = this.currentLane.get();
         if (lane != null) {
            this.currentLane.remove();
            this.availableLanes.add(lane);
         }
      }
   }

   public String getCurrentLaneName() {
      Integer lane = this.currentLane.get();
      return lane != null ? String.valueOf(lane) : null;
   }

   public void abort() {
      this.aborted.set(true);
   }

   public boolean isAborted() {
      return this.aborted.get();
   }

   public void ownServer(MockServer server) {
      if (!this.sharedCallSingleCache || !this.callSingleLock.isHeldByCurrentThread()) {
         this.servers.add(server);
      }
   }

   boolean releaseServer(MockServer server) {
      return this.servers.remove(server);
   }

   public List<MockServer> detachServers() {
      List<MockServer> out = new ArrayList<>();

      for (MockServer server : this.servers) {
         if (this.servers.remove(server)) {
            out.add(server);
         }
      }

      return out;
   }

   private void stopServers() {
      for (MockServer server : this.detachServers()) {
         try {
            server.stopAndWait();
         } catch (RuntimeException var4) {
            logger.warn("mock server on port {} did not stop: {}", server.getPort(), var4.getMessage());
         }
      }
   }

   public SuiteResult getResult() {
      return this.result;
   }

   public boolean canReleaseCallResultsAtScenarioEnd() {
      return !this.retainCallResults && this.resultListeners.isEmpty();
   }

   public List<ResultListener> getResultListeners() {
      return this.resultListeners;
   }

   public Map<String, String> getSystemProperties() {
      return new Suite.SystemPropertiesView(this.systemProperties);
   }

   public Map<String, String> getSystemEnv() {
      return this.systemEnv;
   }

   public String getEnvVar(String name) {
      String value = this.systemEnv.get(name);
      return value != null ? value : System.getenv(name);
   }

   public String getSystemProperty(String key) {
      if (this.systemProperties != null) {
         String value = this.systemProperties.get(key);
         if (value != null) {
            return value;
         }
      }

      return System.getProperty(key);
   }

   public DriverProvider getDriverProvider() {
      return this.driverProvider;
   }

   public PerfHook getPerfHook() {
      return this.perfHook;
   }

   void setPerfHook(PerfHook hook) {
      this.perfHook = hook;
   }

   public HttpClientFactory getHttpClientFactory() {
      return this.httpClientFactory;
   }

   public boolean isPerfMode() {
      return this.perfHook != null;
   }

   public boolean isCaptureStepLogs() {
      return this.captureStepLogs != null ? this.captureStepLogs : !this.isPerfMode();
   }

   public boolean isSkipTagFiltering() {
      return this.skipTagFiltering;
   }

   static final class ConfigScript {
      final Node wrapped;
      private final Resource resource;
      private Node direct;

      private ConfigScript(Resource resource) {
         this.resource = resource;
         this.wrapped = parse(Resource.embedded("(" + resource.getText() + ")", resource, 0));
      }

      synchronized Node direct() {
         if (this.direct == null) {
            this.direct = parse(this.resource);
         }

         return this.direct;
      }

      private static Node parse(Resource resource) {
         try {
            return new JsParser(resource).leadingFunctionExpression().parse();
         } catch (Exception var2) {
            return null;
         }
      }
   }

   private static final class SystemPropertiesView extends AbstractMap<String, String> {
      private final Map<String, String> overrides;

      private SystemPropertiesView(Map<String, String> overrides) {
         this.overrides = overrides;
      }

      public String get(Object key) {
         if (key instanceof String name) {
            return this.overrides != null && this.overrides.containsKey(name) ? this.overrides.get(name) : System.getProperty(name);
         } else {
            return null;
         }
      }

      @Override
      public boolean containsKey(Object key) {
         return this.overrides != null && this.overrides.containsKey(key) ? true : key instanceof String name && System.getProperty(name) != null;
      }

      public String put(String key, String value) {
         Suite.logger
            .warn("karate.properties is read-only, ignoring write of '{}' — use System.setProperty() or Runner.Builder.systemProperty() to set one", key);
         return this.get(key);
      }

      @Override
      public Set<Entry<String, String>> entrySet() {
         Map<String, String> merged = new HashMap<>();
         System.getProperties().forEach((k, v) -> merged.put(k.toString(), v.toString()));
         if (this.overrides != null) {
            merged.putAll(this.overrides);
         }

         return merged.entrySet();
      }
   }
}
