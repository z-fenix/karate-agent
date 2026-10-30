package io.karatelabs.core;

import io.karatelabs.common.FileUtils;
import io.karatelabs.common.Json;
import io.karatelabs.common.Resource;
import io.karatelabs.common.StringUtils;
import io.karatelabs.common.Xml;
import io.karatelabs.driver.Driver;
import io.karatelabs.driver.DriverApi;
import io.karatelabs.driver.DriverProvider;
import io.karatelabs.driver.PooledDriverProvider;
import io.karatelabs.driver.cdp.CdpDriver;
import io.karatelabs.driver.cdp.CdpDriverOptions;
import io.karatelabs.driver.w3c.W3cBrowserType;
import io.karatelabs.driver.w3c.W3cDriver;
import io.karatelabs.driver.w3c.W3cDriverOptions;
import io.karatelabs.gherkin.Feature;
import io.karatelabs.gherkin.Scenario;
import io.karatelabs.gherkin.Step;
import io.karatelabs.gherkin.Tag;
import io.karatelabs.http.DefaultHttpClientFactory;
import io.karatelabs.http.HttpClientFactory;
import io.karatelabs.http.HttpRequestBuilder;
import io.karatelabs.js.DebugPointFactory;
import io.karatelabs.js.Engine;
import io.karatelabs.js.JavaCallable;
import io.karatelabs.js.JavaInvokable;
import io.karatelabs.js.RunInterceptor;
import io.karatelabs.match.MarkerPolicy;
import io.karatelabs.output.LogContext;
import io.karatelabs.output.LogMask;
import io.karatelabs.parser.Node;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.regex.Pattern;
import org.slf4j.Logger;

public class ScenarioRuntime implements Callable<ScenarioResult>, KarateJsContext {
   private static final Logger logger = LogContext.RUNTIME_LOGGER;
   private static final ThreadLocal<ScenarioRuntime> CURRENT = new ThreadLocal<>();
   private final FeatureRuntime featureRuntime;
   private final Scenario scenario;
   private final KarateJs karate;
   private final StepExecutor executor;
   private final ScenarioResult result;
   private final KarateConfig config;
   private Step currentStep;
   private boolean stopped;
   private boolean aborted;
   private boolean skipBackground;
   private Throwable error;
   private boolean hasIgnoredFailure;
   private Throwable firstIgnoredError;
   private final boolean reportDisabled;
   private volatile Object listenResult;
   private CountDownLatch listenLatch = new CountDownLatch(1);
   private Driver driver;
   private String driverScope = "scenario";
   private List<Channel> channels;
   private List<MockServer> servers;
   private final ScenarioRuntime setupCaller;
   private PerfEvent prevPerfEvent;
   private boolean driverFromProvider;
   private boolean driverInherited;
   private final Map<String, Map<String, Object>> cookieJar = new LinkedHashMap<>();
   private final Set<Object> requestDerived = Collections.newSetFromMap(new IdentityHashMap<>());
   private final Set<String> requestDerivedStrings = new HashSet<>();
   private final Map<String, Exception> failedEmbedded = new HashMap<>();
   private boolean requestExpressionsEnabled;
   private ScenarioRuntime mockCaller;
   private StringBuilder authorText;
   private final Set<String> authorTextsAdded = new LinkedHashSet<>();
   private List<Step> steps;
   private int stepIndex;
   private RunInterceptor<?> interceptor;
   private DebugPointFactory<?> pointFactory;
   private final HttpClientFactory httpClientFactory;
   private final boolean ownsHttpClient;
   private boolean httpClientReleased;
   private final Map<String, Object> configVars = new LinkedHashMap<>();
   private Throwable configError;
   private final Map<String, Object> inheritedVariables = new LinkedHashMap<>();
   private final Set<String> assignedVariables = new LinkedHashSet<>();
   private static final Pattern JS_NAME_PLACEHOLDER = Pattern.compile("\\$\\{.*?}");
   private final MarkerPolicy markerPolicy = new MarkerPolicy() {
      @Override
      public boolean trusted(String marker) {
         return ScenarioRuntime.this.authorText().indexOf(marker) != -1;
      }

      @Override
      public RuntimeException untrustedFailure(RuntimeException e) {
         return new RuntimeException(
            e.getMessage()
               + " - in a mock, a match marker sees the mock's variables only when written in the feature itself; 'configure requestExpressionsEnabled = true' opts in",
            e
         );
      }
   };

   public static ScenarioRuntime currentOrNull() {
      return CURRENT.get();
   }

   void runAsCurrent(Runnable task) {
      ScenarioRuntime outerRuntime = CURRENT.get();
      CURRENT.set(this);

      try {
         task.run();
      } finally {
         if (outerRuntime != null) {
            CURRENT.set(outerRuntime);
         } else {
            CURRENT.remove();
         }
      }
   }

   ScenarioRuntime getSetupCaller() {
      return this.setupCaller;
   }

   public ScenarioRuntime(FeatureRuntime featureRuntime, Scenario scenario) {
      this(featureRuntime, scenario, null);
   }

   private ScenarioRuntime(FeatureRuntime featureRuntime, Scenario scenario, ScenarioRuntime setupCaller) {
      this.featureRuntime = featureRuntime;
      this.scenario = scenario;
      this.setupCaller = setupCaller;
      ScenarioRuntime callerScenario = featureRuntime != null ? featureRuntime.getCallerScenario() : null;
      this.reportDisabled = isReportDisabledTag(scenario) || callerScenario != null && callerScenario.reportDisabled;
      Resource featureResource = scenario.getFeature().getResource();
      Suite suite = featureRuntime != null ? featureRuntime.getSuite() : null;
      HttpClientFactory factory = suite != null ? suite.getHttpClientFactory() : null;
      this.httpClientFactory = (HttpClientFactory)(factory != null ? factory : new DefaultHttpClientFactory());
      this.karate = new KarateJs(featureResource, this.httpClientFactory);
      this.ownsHttpClient = true;
      if (callerScenario != null && callerScenario.isMock()) {
         this.inheritMockPosture(callerScenario);
      }

      this.executor = new StepExecutor(this);
      this.result = new ScenarioResult(scenario);
      this.result.setReportDisabled(this.reportDisabled);
      this.config = new KarateConfig();
      if (this.config.getCharset() != null) {
         this.karate.http.charset(this.config.getCharset().name());
      }

      if (suite != null && suite.debugInterceptor != null && suite.debugPointFactory != null) {
         RunInterceptor<Object> debugIntcptr = (RunInterceptor<Object>)suite.debugInterceptor;
         DebugPointFactory<Object> debugFactory = (DebugPointFactory<Object>)suite.debugPointFactory;
         this.setDebugSupport(debugIntcptr, debugFactory);
      }

      this.initEngine();
   }

   public ScenarioRuntime(KarateJs karate, Scenario scenario) {
      this.featureRuntime = null;
      this.scenario = scenario;
      this.setupCaller = null;
      this.reportDisabled = isReportDisabledTag(scenario);
      this.karate = karate;
      this.httpClientFactory = null;
      this.ownsHttpClient = false;
      this.executor = new StepExecutor(this);
      this.result = new ScenarioResult(scenario);
      this.result.setReportDisabled(this.reportDisabled);
      this.config = new KarateConfig();
      if (this.config.getCharset() != null) {
         karate.http.charset(this.config.getCharset().name());
      }

      karate.setContext(this);
   }

   private void initEngine() {
      this.karate.setContext(this);
      if (this.featureRuntime != null && this.featureRuntime.getSuite() != null) {
         this.karate.setEnv(this.featureRuntime.getSuite().env);
      }

      if (this.featureRuntime != null && this.featureRuntime.getSuite() != null) {
         for (Entry<String, Object> entry : this.featureRuntime.getSuite().getGlobals().entrySet()) {
            Object value = entry.getValue();
            Object binding = value instanceof ExtGlobalFactory factory ? factory.create(this) : value;
            this.karate.engine.putRootBinding(entry.getKey(), binding);
         }
      }

      label187:
      if (this.featureRuntime != null && this.featureRuntime.getSuite() != null && this.featureRuntime.isTopLevel() && !this.isDryRunSkip()) {
         ScenarioRuntime outerRuntime = CURRENT.get();
         CURRENT.set(this);

         try {
            this.evalConfig();
            break label187;
         } catch (Exception var9) {
            this.configError = var9;
         } finally {
            if (outerRuntime != null) {
               CURRENT.set(outerRuntime);
            } else {
               CURRENT.remove();
            }
         }

         return;
      }

      if (this.featureRuntime != null && this.featureRuntime.isCalled()) {
         this.inheritVariables();
      }

      Map<String, Object> callArg = this.featureRuntime != null ? this.featureRuntime.getCallArg() : null;
      boolean isCalled = this.featureRuntime != null && this.featureRuntime.isCalled();
      if (callArg != null || isCalled) {
         this.karate.engine.putRootBinding("__arg", callArg);
      }

      if (isCalled) {
         this.karate.engine.putRootBinding("__loop", this.featureRuntime.getLoopIndex());
      }

      if (callArg != null) {
         for (Entry<String, Object> entry : callArg.entrySet()) {
            this.assignedVariables.add(entry.getKey());
            this.karate.engine.put((String) entry.getKey(), entry.getValue());
         }
      }

      if (this.scenario.getExampleData() != null) {
         Map<String, Object> exampleData = this.scenario.getExampleData();

         for (Entry<String, Object> entry : exampleData.entrySet()) {
            this.karate.engine.put((String) entry.getKey(), entry.getValue());
         }

         this.karate.engine.putRootBinding("__row", exampleData);
         this.karate.engine.putRootBinding("__num", this.scenario.getExampleIndex());
      }
   }

   private static boolean isReportDisabledTag(Scenario scenario) {
      List<Tag> tags = scenario.getTagsEffective();
      if (tags != null && !tags.isEmpty()) {
         for (Tag tag : tags) {
            if ("report".equals(tag.getName())) {
               List<String> values = tag.getValues();
               if (values != null && values.contains("false")) {
                  return true;
               }
            }
         }

         return false;
      } else {
         return false;
      }
   }

   public boolean isReportDisabled() {
      return this.reportDisabled;
   }

   boolean isDryRunSkip() {
      Suite suite = this.featureRuntime != null ? this.featureRuntime.getSuite() : null;
      return suite != null && suite.dryRun && !this.scenario.isSetup();
   }

   Throwable getConfigError() {
      return this.configError;
   }

   public Map<String, Object> getConfigVars() {
      return this.configVars;
   }

   public Map<String, Object> getInheritedVariables() {
      return this.inheritedVariables;
   }

   public Set<String> getAssignedVariables() {
      return this.assignedVariables;
   }

   private void evalConfig() {
      Suite suite = this.featureRuntime.getSuite();
      if (suite.baseResource != null) {
         this.mergeConfig(this.evalConfigJs(suite.baseResource));
      }

      if (suite.configResource != null) {
         this.mergeConfig(this.evalConfigJs(suite.configResource));
      }

      if (suite.configEnvResource != null) {
         this.mergeConfig(this.evalConfigJs(suite.configEnvResource));
      }
   }

   private void mergeConfig(Map<String, Object> vars) {
      if (vars != null) {
         this.configVars.putAll(vars);
      }
   }

   private Map<String, Object> evalConfigJs(Resource resource) {
      String displayName = resource.getRelativePath();
      if (displayName == null) {
         displayName = resource.getPrefixedPath();
      }

      Suite.ConfigScript script = this.featureRuntime.getSuite().configScript(resource);

      try {
         Object fn = null;
         boolean parseFailed = script.wrapped == null;
         if (!parseFailed) {
            try {
               fn = this.karate.engine.eval(script.wrapped);
            } catch (Exception var13) {
               parseFailed = true;
            }
         }

         Object result;
         if (parseFailed) {
            Object fnBefore = this.karate.engine.getRawBindings().get("fn");
            Node direct = script.direct();
            result = direct == null ? this.karate.engine.eval(resource) : this.karate.engine.eval(direct);
            if (result instanceof JavaCallable) {
               boolean declaredHere = this.karate.engine.getRawBindings().get("fn") != fnBefore;
               result = (declaredHere ? this.karate.engine.get("fn") : null) instanceof JavaCallable callable ? callable.call(null) : null;
            }
         } else if (fn instanceof JavaCallable callable) {
            result = callable.call(null);
         } else {
            result = fn;
         }

         if (!(result instanceof Map<?, ?> rawVars)) {
            if (result != null) {
               logger.warn("{} did not return an object, got: {}", displayName, result.getClass().getSimpleName());
            }

            return null;
         } else {
            Map<String, Object> vars = (Map<String, Object>) rawVars;
            for (Entry<?, ?> entry : vars.entrySet()) {
               this.karate.engine.put((String) entry.getKey(), entry.getValue());
            }

            logger.debug("Evaluated {}: {} variables", displayName, vars.size());
            return vars;
         }
      } catch (Exception var14) {
         logger.warn("Failed to evaluate {}: {}", displayName, var14.getMessage());
         throw new RuntimeException("Config evaluation failed: " + displayName + " - " + var14.getMessage(), var14);
      }
   }

   public Map<String, Object> executeSetup(String name) {
      if (this.featureRuntime == null) {
         throw new RuntimeException("karate.setup() requires a feature context");
      } else {
         Scenario setupScenario = this.scenario.getFeature().getSetup(name);
         if (setupScenario == null) {
            String message = "no scenario found with @setup tag";
            if (name != null) {
               message = message + " and name '" + name + "'";
            }

            throw new RuntimeException(message);
         } else {
            ScenarioRuntime sr = new ScenarioRuntime(this.featureRuntime, setupScenario, this);
            sr.setSkipBackground(true);
            sr.call();
            Throwable setupConfigError = sr.getConfigError();
            if (setupConfigError != null) {
               throw setupConfigError instanceof RuntimeException re ? re : new RuntimeException(setupConfigError);
            } else {
               return sr.getAllVariables();
            }
         }
      }
   }

   public Map<String, Object> executeSetupOnce(String name) {
      if (this.featureRuntime == null) {
         throw new RuntimeException("karate.setupOnce() requires a feature context");
      } else {
         String cacheKey = name == null ? "__default__" : name;
         Map<String, Object> setupCache = this.featureRuntime.getSetupOnceCache();
         Map<String, Object> cached = (Map<String, Object>)setupCache.get(cacheKey);
         if (cached != null) {
            return new HashMap<>(cached);
         } else {
            synchronized (setupCache) {
               cached = (Map<String, Object>)setupCache.get(cacheKey);
               if (cached != null) {
                  return new HashMap<>(cached);
               } else {
                  Map<String, Object> result = this.executeSetup(name);
                  setupCache.put(cacheKey, result);
                  return new HashMap<>(result);
               }
            }
         }
      }
   }

   public Object executeJsCall(String path, Object arg) {
      if (this.featureRuntime == null) {
         throw new RuntimeException("karate.call() requires a feature context");
      } else {
         StepUtils.ParsedFeaturePath parsed = StepUtils.parseFeaturePath(path);
         String tagSelector = parsed.tagSelector();
         Set<Integer> lineFilters = parsed.lineFilters();
         Feature calledFeature;
         if (parsed.sameFile()) {
            calledFeature = this.featureRuntime.getFeature();
         } else {
            Resource calledResource = this.featureRuntime.resolve(parsed.path());
            if ("js".equals(calledResource.getExtension())) {
               Object jsTarget = this.karate.engine.eval(calledResource);
               if (jsTarget instanceof JavaCallable fn) {
                  return arg == null ? fn.call(null) : fn.call(null, arg);
               }

               return jsTarget;
            }

            calledFeature = Feature.read(calledResource);
         }

         if (arg instanceof List) {
            return this.executor.callFeatureLoop(calledFeature, (List<?>)arg, tagSelector, lineFilters);
         } else {
            Map<String, Object> callArg = null;
            if (arg != null) {
               if (!(arg instanceof Map)) {
                  throw new RuntimeException("karate.call() arg must be a map or list, got: " + arg.getClass());
               }

               callArg = (Map<String, Object>)arg;
            }

            Map<String, Object> resultVars = this.executor.callFeatureSingle(calledFeature, callArg, tagSelector, lineFilters);
            return resultVars != null ? resultVars : new HashMap();
         }
      }
   }

   public Object executeJsCallOnce(String path, Object arg) {
      if (this.featureRuntime == null) {
         throw new RuntimeException("karate.callonce() requires a feature context");
      } else {
         String cacheKey = "callonce:call read('" + path + "')";
         Map<String, Object> cache = this.featureRuntime.getCallOnceCache();
         ReentrantLock lock = this.featureRuntime.getCallOnceLock();
         Object cached = cache.get(cacheKey);
         if (cached != null) {
            return StepUtils.deepCopy(cached);
         } else {
            lock.lock();

            Object result;
            try {
               Object rechecked = cache.get(cacheKey);
               if (rechecked == null) {
                  result = this.executeJsCall(path, arg);
                  cache.put(cacheKey, StepUtils.deepCopy(result));
                  return result;
               }

               result = StepUtils.deepCopy(rechecked);
            } finally {
               lock.unlock();
            }

            return result;
         }
      }
   }

   public Object executeCallSingle(String path, Object arg) {
      if (this.featureRuntime != null && this.featureRuntime.getSuite() != null) {
         Suite suite = this.featureRuntime.getSuite();
         Map<String, Object> cache = suite.getCallSingleCache();
         ReentrantLock lock = suite.getCallSingleLock();
         int cacheMinutes = this.config.getCallSingleCacheMinutes();
         String cacheDir = this.config.getCallSingleCacheDir();
         if (cacheDir == null || cacheDir.isEmpty()) {
            cacheDir = FileUtils.getBuildDir() + "/karate-temp/cache";
         }

         if (cache.containsKey(path)) {
            logger.trace("[callSingle] memory cache hit: {}", path);
            return this.unwrapCachedResult(cache.get(path));
         } else {
            long startWait = System.currentTimeMillis();
            logger.debug("[callSingle] waiting for lock: {}", path);
            lock.lock();

            String cleanedName;
            try {
               if (!cache.containsKey(path)) {
                  Object result = null;
                  File cacheFile = null;
                  if (cacheMinutes > 0) {
                     cleanedName = StringUtils.toIdString(path);
                     cacheFile = new File(cacheDir, cleanedName + ".txt");
                     long staleThreshold = System.currentTimeMillis() - cacheMinutes * 60L * 1000L;
                     if (cacheFile.exists()) {
                        long lastModified = cacheFile.lastModified();
                        if (lastModified > staleThreshold) {
                           try {
                              String json = Files.readString(cacheFile.toPath());
                              result = Json.parseLenient(json);
                              logger.info("[callSingle] disk cache hit: {}", cacheFile);
                           } catch (Exception var25) {
                              result = null;
                              logger.warn("[callSingle] disk cache read failed: {} - {}", cacheFile, var25.getMessage());
                           }
                        } else {
                           logger.info(
                              "[callSingle] disk cache stale: {} (modified {}ms ago, threshold {}min)",
                              new Object[]{cacheFile, System.currentTimeMillis() - lastModified, cacheMinutes}
                           );
                        }
                     } else {
                        logger.debug("[callSingle] disk cache miss, will create: {}", cacheFile);
                     }
                  }

                  if (result == null) {
                     logger.info("[callSingle] >> executing: {}", path);
                     long startExec = System.currentTimeMillis();

                     try {
                        result = this.executeCallSingleInternal(path, arg);
                     } catch (Exception var24) {
                        logger.warn("[callSingle] caching exception for: {} - {}", path, var24.getMessage());
                        cache.put(path, new ScenarioRuntime.CallSingleException(var24));
                        throw var24;
                     }

                     long execTime = System.currentTimeMillis() - startExec;
                     logger.info("[callSingle] << executed in {}ms: {}", execTime, path);
                     if (cacheMinutes > 0 && cacheFile != null) {
                        String notJsonLike = Json.isMapOrList(result) ? findNonJsonValue(result, "$") : "the result itself";
                        if (notJsonLike == null) {
                           try {
                              cacheFile.getParentFile().mkdirs();
                              String json = StringUtils.formatJson(result, false, false, false);
                              Files.writeString(cacheFile.toPath(), json);
                              logger.info("[callSingle] disk cache write: {}", cacheFile);
                           } catch (IOException var23) {
                              logger.warn("[callSingle] disk cache write failed: {} - {}", cacheFile, var23.getMessage());
                           }
                        } else {
                           logger.warn("[callSingle] disk cache skipped (not JSON-like at {}): {}", notJsonLike, path);
                        }
                     }
                  }

                  cache.put(path, result);
                  logger.debug("[callSingle] memory cached: {}", path);
                  return StepUtils.deepCopy(result);
               }

               long waitTime = System.currentTimeMillis() - startWait;
               logger.info("[callSingle] lock acquired after {}ms, memory cache hit: {}", waitTime, path);
               cleanedName = (String)this.unwrapCachedResult(cache.get(path));
            } finally {
               lock.unlock();
            }

            return cleanedName;
         }
      } else {
         throw new RuntimeException("karate.callSingle() requires a Suite context");
      }
   }

   private Object executeCallSingleInternal(String path, Object arg) {
      String filePath = path.contains("?") ? path.substring(0, path.indexOf(63)) : path;
      StepUtils.ParsedFeaturePath parsed = StepUtils.parseFeaturePath(filePath);
      filePath = parsed.path() != null ? parsed.path() : filePath;
      String tagSelector = parsed.tagSelector();
      Object content = this.karate.engine.eval("read('" + filePath.replace("'", "\\'") + "')");
      if (content instanceof Feature calledFeature) {
         Map<String, Object> callArg = arg != null ? (Map)arg : null;
         FeatureRuntime nestedFr = new FeatureRuntime(this.featureRuntime.getSuite(), calledFeature, this.featureRuntime, this, false, callArg, tagSelector);
         FeatureResult fr = nestedFr.call();
         this.executor.addCallResult(fr);
         if (fr.isFailed()) {
            String failureMsg = fr.getScenarioResults()
               .stream()
               .filter(ScenarioResult::isFailed)
               .findFirst()
               .map(ScenarioResult::getFailureMessage)
               .orElse("callSingle feature failed");
            throw new RuntimeException("callSingle failed: " + path + " - " + failureMsg);
         } else {
            return nestedFr.getLastExecuted() != null ? StepExecutor.calleeResult(nestedFr.getLastExecuted()) : new HashMap();
         }
      } else {
         return content instanceof JavaCallable fn ? fn.call(null, arg == null ? new Object[0] : new Object[]{arg}) : content;
      }
   }

   private static String findNonJsonValue(Object value, String path) {
      return findNonJsonValue(value, path, Collections.newSetFromMap(new IdentityHashMap<>()));
   }

   private static String findNonJsonValue(Object value, String path, Set<Object> visited) {
      if (value instanceof Double d) {
         return !d.isNaN() && !d.isInfinite() ? null : path;
      } else if (value instanceof Float f) {
         return !f.isNaN() && !f.isInfinite() ? null : path;
      } else if (value == null || value instanceof CharSequence || value instanceof Number || value instanceof Boolean) {
         return null;
      } else if (value instanceof JavaCallable) {
         return null;
      } else if (!visited.add(value)) {
         return path;
      } else {
         String var7;
         try {
            if (!(value instanceof Map<?, ?> map)) {
               if (!(value instanceof List<?> list)) {
                  return path;
               }

               for (int i = 0; i < list.size(); i++) {
                  String found = findNonJsonValue(list.get(i), path + "[" + i + "]", visited);
                  if (found != null) {
                     return found;
                  }
               }

               return null;
            }

            Iterator ix = map.entrySet().iterator();

            String found;
            do {
               if (!ix.hasNext()) {
                  return null;
               }

               Entry<?, ?> entry = (Entry<?, ?>)ix.next();
               if (!(entry.getKey() instanceof String)) {
                  return path + "." + entry.getKey();
               }

               found = findNonJsonValue(entry.getValue(), path + "." + entry.getKey(), visited);
            } while (found == null);

            var7 = found;
         } finally {
            visited.remove(value);
         }

         return var7;
      }
   }

   private Object unwrapCachedResult(Object cached) {
      if (cached instanceof ScenarioRuntime.CallSingleException) {
         throw new RuntimeException(((ScenarioRuntime.CallSingleException)cached).cause.getMessage(), ((ScenarioRuntime.CallSingleException)cached).cause);
      } else {
         return StepUtils.deepCopy(cached);
      }
   }

   private void inheritVariables() {
      boolean sharedScope = this.featureRuntime.isSharedScope();
      ScenarioRuntime callerScenario = this.featureRuntime.getCallerScenario();
      if (callerScenario != null) {
         this.inheritConfigFromCaller(callerScenario, sharedScope);
         Map<String, Object> parentVars = callerScenario.getAllVariables();

         for (Entry<String, Object> entry : parentVars.entrySet()) {
            Object value = entry.getValue();
            if (!sharedScope) {
               value = shallowCopy(value);
            }

            this.inheritedVariables.put(entry.getKey(), value);
            this.karate.engine.put(entry.getKey(), value);
         }

         this.inheritCookieJarFromCaller(callerScenario);
         this.inheritDriverFromCaller(callerScenario);
      } else {
         FeatureRuntime caller = this.featureRuntime.getCaller();
         if (caller != null && caller.getLastExecuted() != null) {
            ScenarioRuntime lastExecuted = caller.getLastExecuted();
            this.inheritConfigFromCaller(lastExecuted, sharedScope);
            Map<String, Object> parentVars = lastExecuted.getAllVariables();

            for (Entry<String, Object> entry : parentVars.entrySet()) {
               Object value = entry.getValue();
               if (!sharedScope) {
                  value = shallowCopy(value);
               }

               this.inheritedVariables.put(entry.getKey(), value);
               this.karate.engine.put(entry.getKey(), value);
            }

            this.inheritCookieJarFromCaller(lastExecuted);
            this.inheritDriverFromCaller(lastExecuted);
         }
      }
   }

   private void inheritConfigFromCaller(ScenarioRuntime callerScenario, boolean sharedScope) {
      this.config.copyFrom(callerScenario.config);
      this.karate.client.apply(this.config);
      logger.debug("Inherited config from caller");
   }

   private void inheritCookieJarFromCaller(ScenarioRuntime callerScenario) {
      Map<String, Map<String, Object>> parentCookies = callerScenario.getCookieJar();
      if (!parentCookies.isEmpty()) {
         this.cookieJar.putAll(parentCookies);
      }
   }

   private void inheritDriverFromCaller(ScenarioRuntime callerScenario) {
      if (callerScenario.driver != null && !callerScenario.driver.isTerminated()) {
         this.driver = callerScenario.driver;
         this.driverInherited = true;
         logger.debug("Inherited driver from caller: {}", this.driver);
         Map<String, Object> parentRootBindings = callerScenario.karate.engine.getRootBindings();

         for (Entry<String, Object> entry : parentRootBindings.entrySet()) {
            String key = entry.getKey();
            if (!"karate".equals(key) && !"read".equals(key) && !"match".equals(key)) {
               this.karate.engine.putRootBinding(key, entry.getValue());
            }
         }
      }
   }

   private static Object shallowCopy(Object value) {
      if (value instanceof JavaCallable) {
         return value;
      } else if (value instanceof Map) {
         return new LinkedHashMap((Map)value);
      } else {
         return value instanceof List ? new ArrayList((List)value) : value;
      }
   }

   public ScenarioResult call() {
      LogContext outerLogContext = LogContext.get();
      String configTimeLog = outerLogContext.collect();
      List<StepResult.Embed> configTimeEmbeds = outerLogContext.collectEmbeds();
      boolean nestedCall = this.featureRuntime != null && this.featureRuntime.isCalled();
      ScenarioRuntime outerRuntime = CURRENT.get();
      CURRENT.set(this);
      LogContext.set(new LogContext());
      Suite suite = this.featureRuntime != null ? this.featureRuntime.getSuite() : null;
      if (suite != null) {
         LogContext.get().setCapture(suite.isCaptureStepLogs());
      }

      this.config.applyLoggingToContext(LogContext.get());
      if (configTimeLog != null && !configTimeLog.isEmpty()) {
         LogContext.get().appendCaptured(configTimeLog);
      }

      if (configTimeEmbeds != null) {
         for (StepResult.Embed e : configTimeEmbeds) {
            LogContext.get().embed(e.getData(), e.getMimeType(), e.getName());
         }
      }

      if (this.config.getCompiledMask() != null && this.config.getCompiledMask() != LogMask.DEFAULT && logger.isDebugEnabled()) {
         logger.debug("http log mask active: {}", this.config.getCompiledMask().describe());
      }

      LogContext.Snapshot loggingSnapshot = LogContext.snapshot();
      this.result.setStartTime(System.currentTimeMillis());
      String laneName = suite != null ? suite.getCurrentLaneName() : null;
      String threadName;
      if (laneName != null) {
         threadName = laneName;
      } else {
         Thread currentThread = Thread.currentThread();
         threadName = currentThread.getName();
         if (threadName == null || threadName.isEmpty() || "main".equals(threadName) || threadName.isBlank()) {
            threadName = "thread-" + currentThread.threadId();
         }
      }

      this.result.setThreadName(threadName);
      if (suite != null && this.result.getExecutionIndex() == 0) {
         this.result.setExecutionIndex(suite.nextExecutionIndex());
      }

      if (suite != null && suite.isAborted() && this.featureRuntime.isTopLevel()) {
         this.stopped = true;
         this.result.setAborted(true);
         this.result.setEndTime(System.currentTimeMillis());
         this.restoreLogContext(loggingSnapshot, outerLogContext, nestedCall, outerRuntime);
         this.stopServers();
         this.releaseHttpClient();
         return this.result;
      } else if (this.configError != null) {
         long now = System.currentTimeMillis();
         StepResult failure = StepResult.synthetic(
            "Scenario execution failed: " + this.configError.getMessage(), StepResult.Status.FAILED, now, this.configError
         );
         LogContext ctx = LogContext.get();
         failure.setLog(ctx.collect());
         List<StepResult.Embed> configEmbeds = ctx.collectEmbeds();
         if (configEmbeds != null) {
            for (StepResult.Embed e : configEmbeds) {
               failure.addEmbed(e);
            }
         }

         List<FeatureResult> configCallResults = this.executor.drainCallResults();
         if (configCallResults != null && !configCallResults.isEmpty()) {
            failure.setCallResults(configCallResults);
         }

         this.result.addStepResult(failure);
         this.result.setEndTime(now);
         this.stopped = true;
         this.restoreLogContext(loggingSnapshot, outerLogContext, nestedCall, outerRuntime);
         this.stopServers();
         this.releaseHttpClient();
         return this.result;
      } else {
         boolean scenarioStarted = false;
         boolean topLevel = this.featureRuntime == null || this.featureRuntime.isTopLevel();
         boolean exitFired = false;

         try {
            if (suite != null) {
               boolean proceed = suite.fireEvent(ScenarioRunEvent.enter(this));
               if (!proceed) {
                  this.stopped = true;
               }
            }

            if (!this.stopped) {
               scenarioStarted = true;
               if (topLevel && !this.isDryRunSkip()) {
                  Throwable hookError = this.invokeAndRecordHook(this.config.getBeforeScenario(), "beforeScenario");
                  if (hookError != null && !this.config.isContinueOnStepFailure()) {
                     this.stopped = true;
                  }
               }
            }

            this.steps = this.skipBackground ? this.scenario.getSteps() : this.scenario.getStepsIncludingBackground();
            int count = this.steps.size();
            this.stepIndex = 0;

            while (this.stepIndex < count) {
               if (this.stopped || this.aborted || suite != null && suite.isAborted() && this.featureRuntime.isTopLevel()) {
                  while (this.stepIndex < count) {
                     StepResult sr = StepResult.skipped(this.steps.get(this.stepIndex), System.currentTimeMillis());
                     this.result.addStepResult(sr);
                     this.stepIndex++;
                  }
                  break;
               }

               this.currentStep = this.steps.get(this.stepIndex);
               this.stepIndex++;
               StepResult sr = this.isDryRunSkip() ? StepResult.skipped(this.currentStep, System.currentTimeMillis()) : this.executor.execute(this.currentStep);
               this.result.addStepResult(sr);
               List<StepResult> appended = LogContext.get().collectPendingSteps();
               if (appended != null) {
                  for (StepResult ap : appended) {
                     this.result.addStepResult(ap);
                  }
               }

               if (sr.isFailed()) {
                  boolean softAssert = this.runStepFailurePipeline(sr);
                  if (softAssert) {
                     if (!this.hasIgnoredFailure) {
                        this.hasIgnoredFailure = true;
                        this.firstIgnoredError = sr.getError();
                     }
                  } else {
                     this.stopped = true;
                     this.error = sr.getError();
                  }
               }
            }

            if (this.hasIgnoredFailure && this.error == null) {
               this.error = this.firstIgnoredError;
            }

            this.evaluateScenarioName();
            if (this.aborted) {
               this.result.setAborted(true);
            }

            this.result.setEndTime(System.currentTimeMillis());
            this.result.setStableId(this.resolveStableId());
            exitFired = true;
            if (suite != null) {
               suite.fireEvent(ScenarioRunEvent.exit(this, this.result));
            }
         } catch (Throwable var24) {
            this.error = var24;
            this.stopped = true;
            if (!exitFired) {
               StepResult failurex = StepResult.synthetic("Scenario execution failed: " + var24, StepResult.Status.FAILED, System.currentTimeMillis(), var24);
               failurex.setLog(LogContext.get().collect());
               this.result.addStepResult(failurex);
               if (this.aborted) {
                  this.result.setAborted(true);
               }

               this.result.setEndTime(System.currentTimeMillis());
               this.result.setStableId(this.resolveStableId());
               if (suite != null) {
                  try {
                     suite.fireEvent(ScenarioRunEvent.exit(this, this.result));
                  } catch (Throwable var23) {
                     logger.error("SCENARIO_EXIT listener failed: {}", var23.getMessage());
                  }
               }
            }
         } finally {
            this.reportPerfFailure(this.perfFailureMessage());
            if (scenarioStarted && topLevel && !this.isDryRunSkip()) {
               Throwable hookError = this.invokeAndRecordHook(this.config.getAfterScenario(), "afterScenario");
               if (hookError != null) {
                  this.result.setEndTime(System.currentTimeMillis());
               }

               this.reportPerfFailure(hookError != null ? this.perfFailureMessage() : null);
            }

            this.closeChannels();
            this.closeDriver();
            this.stopServers();
            this.releaseHttpClient();
            if (this.scenario.isFail()) {
               this.result.applyFailTag();
            }

            if (this.result.getEndTime() == 0L) {
               this.result.setEndTime(System.currentTimeMillis());
            }

            if (this.result.isFailed() && this.config.isAbortSuiteOnFailure() && suite != null && this.featureRuntime.isTopLevel()) {
               suite.abort();
            }

            this.restoreLogContext(loggingSnapshot, outerLogContext, nestedCall, outerRuntime);
         }

         return this.result;
      }
   }

   private void restoreLogContext(LogContext.Snapshot loggingSnapshot, LogContext outerLogContext, boolean nestedCall, ScenarioRuntime outerRuntime) {
      loggingSnapshot.restore();
      if (nestedCall) {
         LogContext.set(outerLogContext);
      } else {
         LogContext.clear();
      }

      if (outerRuntime != null) {
         CURRENT.set(outerRuntime);
      } else {
         CURRENT.remove();
      }
   }

   private void evaluateScenarioName() {
      String name = this.scenario.getName();
      if (name != null) {
         String trimmed = name.trim();
         boolean wrappedByBackTick = trimmed.length() > 1 && trimmed.charAt(0) == '`' && trimmed.charAt(trimmed.length() - 1) == '`';
         boolean hasPlaceholder = JS_NAME_PLACEHOLDER.matcher(trimmed).find();
         if (wrappedByBackTick || hasPlaceholder) {
            String eval = wrappedByBackTick ? trimmed : "`" + trimmed + "`";

            try {
               Object evaluated = this.karate.engine.eval(eval);
               if (evaluated != null) {
                  this.scenario.setName(evaluated.toString());
               }
            } catch (Exception var10) {
               String warning = "[WARN] Failed to evaluate scenario name '" + trimmed + "': " + var10.getMessage();
               logger.warn(warning);
               List<StepResult> steps = this.result.getStepResults();
               if (!steps.isEmpty()) {
                  StepResult lastStep = steps.get(steps.size() - 1);
                  lastStep.appendLog(warning);
               }
            }
         }
      }
   }

   public Object eval(String expression) {
      return this.karate.engine.eval(expression);
   }

   public Object eval(String expression, Step step) {
      Resource featureResource = step.getFeature().getResource();
      int lineOffset = step.getLine() - 1;
      Resource embedded = Resource.embedded(expression, featureResource, lineOffset);
      return this.karate.engine.eval(embedded);
   }

   public StepResult evalAsStep(String text) {
      long startTime = System.currentTimeMillis();
      String trimmed = text == null ? "" : text.trim();
      boolean hasPrefix = false;

      for (String prefix : Step.PREFIXES) {
         if (trimmed.startsWith(prefix)) {
            hasPrefix = true;
            break;
         }
      }

      if (!hasPrefix) {
         trimmed = "* " + trimmed;
      }

      Step parsed;
      Scenario synthScenario;
      try {
         Resource resource = Resource.text("Feature:\nScenario:\n" + trimmed);
         Feature feature = Feature.read(resource);
         if (feature.getSections().isEmpty()) {
            throw new RuntimeException("no step parsed");
         }

         synthScenario = feature.getSection(0).getScenario();
         if (synthScenario == null || synthScenario.getSteps() == null || synthScenario.getSteps().isEmpty()) {
            throw new RuntimeException("no step parsed");
         }

         parsed = synthScenario.getSteps().get(0);
      } catch (Exception var10) {
         Step fakeStep = new Step(this.scenario, -1);
         return StepResult.failed(fakeStep, startTime, 0L, var10);
      }

      Step fakeStep = new Step(synthScenario, -1);
      fakeStep.setLine(parsed.getLine());
      fakeStep.setEndLine(parsed.getEndLine());
      fakeStep.setPrefix(parsed.getPrefix());
      fakeStep.setKeyword(parsed.getKeyword());
      fakeStep.setText(parsed.getText());
      fakeStep.setDocString(parsed.getDocString());
      fakeStep.setDocStringLine(parsed.getDocStringLine());
      fakeStep.setTable(parsed.getTable());
      fakeStep.setComments(parsed.getComments());
      return this.executor.execute(fakeStep);
   }

   public Object evalDocString(String expression, Step step) {
      Resource featureResource = step.getFeature().getResource();
      int lineOffset = step.getDocStringLine();
      if (lineOffset < 0) {
         lineOffset = step.getLine();
      }

      String text = alignToSourceLines(expression, featureResource, step.getDocStringLine());
      Resource embedded = Resource.embedded(text, featureResource, lineOffset);
      return this.karate.engine.eval(embedded);
   }

   static String alignToSourceLines(String text, Resource featureResource, int firstLine) {
      if (firstLine >= 0 && text.indexOf(10) >= 0) {
         String source;
         try {
            source = featureResource.getText();
         } catch (Exception var10) {
            return text;
         }

         if (source == null) {
            return text;
         } else {
            String[] sourceLines = source.replace("\r", "").split("\n", -1);
            String[] lines = text.split("\n", -1);
            if (firstLine >= sourceLines.length) {
               return text;
            } else {
               StringBuilder sb = new StringBuilder(text.length() + 16);
               int next = 0;
               boolean blankAdded = false;

               for (int i = firstLine; i < sourceLines.length && next < lines.length; i++) {
                  if (sourceLines[i].isEmpty()) {
                     sb.append('\n');
                     blankAdded = true;
                  } else {
                     sb.append(lines[next++]);
                     if (next < lines.length) {
                        sb.append('\n');
                     }
                  }
               }

               if (next < lines.length) {
                  return text;
               } else {
                  return blankAdded ? sb.toString() : text;
               }
            }
         }
      } else {
         return text;
      }
   }

   public void setVariable(String name, Object value) {
      this.assignedVariables.add(name);
      this.karate.engine.put(name, value);
   }

   public void putUnclaimedVariable(String name, Object value) {
      this.karate.engine.put(name, value);
   }

   public void setHiddenVariable(String name, Object value) {
      this.karate.engine.putRootBinding(name, value);
   }

   public Object getVariable(String name) {
      return this.karate.engine.get(name);
   }

   String resolveStableId() {
      Object value;
      try {
         value = this.karate.engine.get("__id");
      } catch (RuntimeException var3) {
         return null;
      }

      if (value == null) {
         return null;
      } else {
         String id = value.toString();
         return id.isBlank() ? null : id;
      }
   }

   public Map<String, Object> getAllVariables() {
      return new LinkedHashMap<>(this.karate.engine.getBindings());
   }

   public Engine getEngine() {
      return this.karate.engine;
   }

   public HttpRequestBuilder getHttp() {
      return this.karate.http;
   }

   public KarateJs getKarate() {
      return this.karate;
   }

   public boolean isRequestExpressionsEnabled() {
      return this.requestExpressionsEnabled;
   }

   public void setRequestExpressionsEnabled(boolean requestExpressionsEnabled) {
      this.requestExpressionsEnabled = requestExpressionsEnabled;
   }

   public void clearRequestDerived() {
      if (!this.requestDerived.isEmpty()) {
         this.requestDerived.clear();
      }

      if (!this.requestDerivedStrings.isEmpty()) {
         this.requestDerivedStrings.clear();
      }

      if (!this.failedEmbedded.isEmpty()) {
         this.failedEmbedded.clear();
      }
   }

   public void recordFailedEmbedded(String placeholder, Exception error) {
      if (!this.isRequestDerived(placeholder)) {
         this.failedEmbedded.put(placeholder, error);
      }
   }

   public Exception failedEmbedded(String placeholder) {
      return this.failedEmbedded.get(placeholder);
   }

   public boolean hasFailedEmbedded() {
      return !this.failedEmbedded.isEmpty();
   }

   public Set<String> failedEmbeddedKeys() {
      return this.failedEmbedded.keySet();
   }

   public void markRequestDerived(Object value) {
      if (value instanceof Map<?, ?> map) {
         if (this.requestDerived.add(value)) {
            for (Object v : map.values()) {
               this.markRequestDerived(v);
            }
         }
      } else if (value instanceof List<?> list) {
         if (this.requestDerived.add(value)) {
            for (Object v : list) {
               this.markRequestDerived(v);
            }
         }
      } else if (value instanceof String str) {
         if (str.contains("#(")) {
            this.requestDerivedStrings.add(str);
         }
      } else if (value instanceof org.w3c.dom.Node node && this.requestDerived.add(node)) {
         this.markRequestDerived(Xml.toObject(node));
      }
   }

   public boolean isRequestDerived(Object value) {
      if (!this.requestDerived.isEmpty() && this.requestDerived.contains(value)) {
         return true;
      } else {
         return !this.requestDerivedStrings.isEmpty() && value instanceof String str && this.requestDerivedStrings.contains(str)
            ? true
            : this.mockCaller != null && this.mockCaller.isRequestDerived(value);
      }
   }

   public boolean isMock() {
      return this.karate != null && this.karate.mockHandler != null;
   }

   private void inheritMockPosture(ScenarioRuntime mock) {
      this.mockCaller = mock;
      this.karate.setMockHandler(mock.karate.mockHandler);
      this.karate.setJavaBridgeEnabled(mock.karate.javaBridgeEnabled);
      this.requestExpressionsEnabled = mock.requestExpressionsEnabled;
   }

   public MarkerPolicy markerPolicy() {
      return this.isMock() && !this.requestExpressionsEnabled ? this.markerPolicy : null;
   }

   public boolean embeddedTrusted(String token) {
      MarkerPolicy policy = this.markerPolicy();
      return policy == null || policy.trusted(token);
   }

   public void addAuthorText(String text) {
      if (this.isMock() && this.authorTextsAdded.add(text)) {
         this.authorText().append('\n').append(text);
      }
   }

   private StringBuilder authorText() {
      if (this.authorText == null) {
         this.authorText = new StringBuilder(withoutComments(this.scenario.getFeature().getResource().getText()));
      }

      return this.authorText;
   }

   private static String withoutComments(String text) {
      StringBuilder sb = new StringBuilder();
      boolean docString = false;

      for (String line : text.split("\n")) {
         String trimmed = line.trim();
         if (trimmed.startsWith("\"\"\"")) {
            docString = !docString;
         }

         if (docString || !trimmed.startsWith("#")) {
            sb.append(line).append('\n');
         }
      }

      return sb.toString();
   }

   public Map<String, Map<String, Object>> getCookieJar() {
      return this.cookieJar;
   }

   public void updateCookieJar(Object responseCookies) {
      if (responseCookies instanceof Map<?, ?> cookies) {
         for (Entry<?, ?> entry : cookies.entrySet()) {
            String name = entry.getKey().toString();
            Object cookieData = entry.getValue();
            if (cookieData instanceof Map) {
               this.cookieJar.put(name, (Map<String, Object>)cookieData);
            }
         }
      }
   }

   public void clearCookieJar() {
      this.cookieJar.clear();
   }

   public void configure(String key, Object value) {
      this.config.configure(key, value);
      this.karate.client.apply(this.config);
      if ("cookies".equals(key) && value == null) {
         this.clearCookieJar();
      }

      if ("charset".equals(key)) {
         this.karate.http.charset(value == null ? null : value.toString());
      }

      if ("continueOnStepFailure".equals(key) && !toBoolean(value) && this.hasIgnoredFailure) {
         this.stopped = true;
         this.error = this.firstIgnoredError;
      }
   }

   private static boolean toBoolean(Object value) {
      if (value == null) {
         return false;
      } else {
         return value instanceof Boolean b ? b : Boolean.parseBoolean(value.toString());
      }
   }

   @Override
   public ScenarioRuntime getRuntime() {
      return this;
   }

   @Override
   public KarateConfig getConfig() {
      return this.config;
   }

   public StepExecutor getExecutor() {
      return this.executor;
   }

   @Override
   public Resource getWorkingDir() {
      return this.scenario.getFeature().getResource();
   }

   public Scenario getScenario() {
      return this.scenario;
   }

   public FeatureRuntime getFeatureRuntime() {
      return this.featureRuntime;
   }

   public ScenarioResult getResult() {
      return this.result;
   }

   public Step getCurrentStep() {
      return this.currentStep;
   }

   public boolean isStopped() {
      return this.stopped;
   }

   public boolean isAborted() {
      return this.aborted;
   }

   public Throwable getError() {
      return this.error;
   }

   public void abort() {
      this.aborted = true;
   }

   public void stop() {
      this.stopped = true;
   }

   public boolean hotReload() {
      if (this.steps == null) {
         return false;
      } else {
         boolean success = false;
         Feature feature = this.scenario.getFeature();
         Resource resource = feature.getResource();
         if (resource.getPath() == null) {
            return false;
         } else {
            Feature reloaded = Feature.read(Resource.from(resource.getPath()));

            for (Step oldStep : this.steps) {
               Step newStep = reloaded.findStepByLine(oldStep.getLine());
               if (newStep != null) {
                  String oldText = canonicalStepText(oldStep);
                  String newText = canonicalStepText(newStep);
                  if (!oldText.equals(newText)) {
                     try {
                        oldStep.parseAndUpdateFrom(newText);
                        logger.info("hot reloaded line: {} - {}", newStep.getLine(), newText);
                        success = true;
                     } catch (Exception var11) {
                        logger.warn("failed to hot reload step: {}", var11.getMessage());
                     }
                  }
               }
            }

            return success;
         }
      }
   }

   private static String canonicalStepText(Step step) {
      String keyword = step.getKeyword();
      String text = step.getText() == null ? "" : step.getText();
      return keyword == null ? text : keyword + " " + text;
   }

   public void stepBack() {
      this.stopped = false;
      this.stepIndex -= 2;
      if (this.stepIndex < 0) {
         this.stepIndex = 0;
      }
   }

   public void stepReset() {
      this.stopped = false;
      this.stepIndex--;
      if (this.stepIndex < 0) {
         this.stepIndex = 0;
      }
   }

   public void stepProceed() {
      this.stopped = false;
   }

   public int getStepIndex() {
      return this.stepIndex;
   }

   public int getStepCount() {
      return this.steps != null ? this.steps.size() : 0;
   }

   public <T> void setDebugSupport(RunInterceptor<T> interceptor, DebugPointFactory<T> factory) {
      this.interceptor = interceptor;
      this.pointFactory = factory;
      this.karate.engine.setDebugSupport(interceptor, factory);
   }

   public RunInterceptor<?> getInterceptor() {
      return this.interceptor;
   }

   public DebugPointFactory<?> getPointFactory() {
      return this.pointFactory;
   }

   public void setSkipBackground(boolean skipBackground) {
      this.skipBackground = skipBackground;
   }

   public boolean isSkipBackground() {
      return this.skipBackground;
   }

   private boolean runStepFailurePipeline(StepResult sr) {
      boolean innermost = !sr.hasCallResults();
      if (innermost) {
         this.captureScreenshotOnFailure(sr);
      }

      ScenarioRuntime.FailureDecision decision = new ScenarioRuntime.FailureDecision();
      if (innermost) {
         this.invokeOnStepFailureHook(sr, decision);
      }

      this.fireErrorEvent(sr);
      return decision.resolve(this.config.isContinueOnStepFailure());
   }

   private void captureScreenshotOnFailure(StepResult sr) {
      if (this.driver != null && !this.driver.isTerminated()) {
         try {
            if (!this.isScreenshotOnFailureEnabled()) {
               return;
            }

            byte[] bytes = this.driver.failureScreenshot();
            if (bytes != null && bytes.length > 0) {
               sr.addEmbed(new StepResult.Embed(bytes, "image/png", "screenshot.png"));
            }
         } catch (Throwable var3) {
            logger.warn("screenshotOnFailure: capture failed, continuing: {}", var3.toString());
         }
      }
   }

   private boolean isScreenshotOnFailureEnabled() {
      if (this.config.getDriverConfig() instanceof Map map && map.containsKey("screenshotOnFailure")) {
         Object v = map.get("screenshotOnFailure");
         return v == null || Boolean.parseBoolean(String.valueOf(v));
      } else {
         try {
            return this.driver.getOptions().isScreenshotOnFailure();
         } catch (Exception var4) {
            return true;
         }
      }
   }

   private void invokeOnStepFailureHook(StepResult sr, ScenarioRuntime.FailureDecision decision) {
      if (this.config.getOnStepFailure() instanceof JavaCallable callable) {
         try {
            Map<String, Object> info = this.buildStepFailureInfo(sr, decision);
            callable.call(null, info);
         } catch (Throwable var6) {
            logger.warn("onStepFailure hook threw, continuing: {}", var6.toString());
         }
      }
   }

   private Map<String, Object> buildStepFailureInfo(StepResult sr, ScenarioRuntime.FailureDecision decision) {
      Map<String, Object> info = new LinkedHashMap<>();
      Throwable err = sr.getError();
      info.put("error", err == null ? null : err.getMessage());
      Step step = sr.getStep();
      if (step != null) {
         Map<String, Object> stepInfo = new LinkedHashMap<>();
         stepInfo.put("line", step.getLine());
         stepInfo.put("text", step.getText());
         stepInfo.put("prefix", step.getPrefix());
         info.put("step", stepInfo);
      }

      info.put("scenarioName", this.scenario == null ? null : this.scenario.getName());
      if (this.scenario != null && this.scenario.getFeature() != null) {
         info.put("featureName", this.scenario.getFeature().getName());
      }

      info.put("embed", (JavaInvokable)args -> {
         if (args.length >= 1 && args[0] != null) {
            byte[] bytes = KarateJsUtils.convertToBytes(args[0]);
            String mime = args.length > 1 && args[1] != null ? args[1].toString() : KarateJsUtils.detectMimeType(args[0]);
            String name = args.length > 2 && args[2] != null ? args[2].toString() : null;
            sr.addEmbed(new StepResult.Embed(bytes, mime, name));
            return null;
         } else {
            return null;
         }
      });
      info.put("proceed", (JavaInvokable)args -> {
         decision.proceed();
         return null;
      });
      info.put("stop", (JavaInvokable)args -> {
         decision.stop();
         return null;
      });
      return info;
   }

   private void fireErrorEvent(StepResult sr) {
      Suite suite = this.featureRuntime == null ? null : this.featureRuntime.getSuite();
      if (suite != null) {
         if (!this.reportDisabled) {
            try {
               suite.fireEvent(ErrorRunEvent.of(sr.getError(), this));
            } catch (Throwable var4) {
               logger.warn("ErrorRunEvent dispatch failed, continuing: {}", var4.toString());
            }
         }
      }
   }

   public Driver getDriver() {
      if (this.driver != null && this.driver.isTerminated()) {
         this.closeDriver();
      }

      if (this.driver == null) {
         this.driver = this.initDriver();
      }

      return this.driver;
   }

   private Driver initDriver() {
      Object driverConfig = this.config.getDriverConfig();
      if (driverConfig == null) {
         throw new RuntimeException("driver not configured - use: * configure driver = { type: 'chrome' }");
      } else {
         Map<String, Object> configMap = null;
         HashMap var11;
         if (driverConfig instanceof Map) {
            var11 = new HashMap((Map)driverConfig);
         } else {
            var11 = new HashMap();
         }

         DriverProvider provider = null;
         if (this.featureRuntime != null && this.featureRuntime.getSuite() != null) {
            provider = this.featureRuntime.getSuite().getDriverProvider();
            if (!var11.containsKey("userDataDir")) {
               Path tempDir = Path.of(FileUtils.getBuildDir(), "karate-temp", "chrome-" + UUID.randomUUID().toString().substring(0, 8));
               var11.put("userDataDir", tempDir.toAbsolutePath().toString());
            }
         }

         boolean stopFlag = !Boolean.FALSE.equals(var11.get("stop"));
         if (!stopFlag && provider != null) {
            logger.warn("configure driver = {{ stop: false }} — bypassing driver pool; browser will be left running on scenario exit (debug use only)");
            provider = null;
         }

         String driverType = (String) var11.getOrDefault("type", "chrome");
         boolean isW3c = W3cBrowserType.isW3cType(driverType);
         boolean builtInDispatch = provider == null || provider.getClass() == PooledDriverProvider.class;
         if (builtInDispatch && !isW3c && !"chrome".equals(driverType)) {
            throw new RuntimeException(unsupportedDriverTypeMessage(driverType));
         } else {
            if (isW3c) {
               W3cDriverOptions w3cOptions = W3cDriverOptions.fromMap(var11);
               this.driverScope = w3cOptions.getScope();
            } else {
               CdpDriverOptions cdpOptions = CdpDriverOptions.fromMap(var11);
               this.driverScope = cdpOptions.getScope();
            }

            Driver driver;
            if (provider != null) {
               driver = provider.acquire(this, var11);
               this.driverFromProvider = true;
               logger.info("Acquired driver from provider for scenario: {} (scope: {})", this.scenario.getName(), this.driverScope);
            } else {
               if (isW3c) {
                  logger.info("Starting W3C WebDriver: type={}", driverType);
                  driver = W3cDriver.start(var11);
               } else {
                  CdpDriverOptions cdpOptions = CdpDriverOptions.fromMap(var11);
                  String wsUrl = cdpOptions.getWebSocketUrl();
                  if (wsUrl != null && !wsUrl.isEmpty()) {
                     logger.info("Connecting to existing browser: {}", wsUrl);
                     driver = CdpDriver.connect(wsUrl, cdpOptions);
                  } else {
                     logger.info("Starting browser with options: headless={}", cdpOptions.isHeadless());
                     driver = CdpDriver.start(cdpOptions);
                  }
               }

               this.driverFromProvider = false;
            }

            this.karate.engine.putRootBinding("driver", driver);
            DriverApi.bindJsHelpers(this.karate.engine, driver);
            return driver;
         }
      }
   }

   static String unsupportedDriverTypeMessage(String type) {
      StringBuilder sb = new StringBuilder("unsupported driver type: '").append(type).append("'");
      if ("android".equals(type) || "ios".equals(type) || "winappdriver".equals(type)) {
         sb.append(" - native app automation (Appium / WinAppDriver) is not supported in Karate v2");
      }

      sb.append(", supported types: chrome");

      for (W3cBrowserType bt : W3cBrowserType.values()) {
         sb.append(", ").append(bt.getKarateType());
      }

      return sb.toString();
   }

   public void registerChannel(Channel channel) {
      if (this.channels == null) {
         this.channels = new ArrayList<>();
      }

      this.channels.add(channel);
   }

   private Throwable invokeHook(Object hookRef, String hookName) {
      if (hookRef instanceof JavaCallable callable) {
         try {
            callable.call(null);
            return null;
         } catch (Exception var5) {
            logger.warn("{} hook failed: {}", hookName, var5.getMessage());
            return var5;
         }
      } else {
         return null;
      }
   }

   private Throwable invokeAndRecordHook(Object hookRef, String hookName) {
      if (!(hookRef instanceof JavaCallable)) {
         return null;
      } else {
         long startTime = System.currentTimeMillis();
         long startNanos = System.nanoTime();
         LogContext ctx = LogContext.get();
         Throwable err = this.invokeHook(hookRef, hookName);
         long durationNanos = System.nanoTime() - startNanos;
         StepResult.Status status = err == null ? StepResult.Status.PASSED : StepResult.Status.FAILED;
         StepResult sr = StepResult.hook(hookName, status, startTime, durationNanos, err);
         List<FeatureResult> calls = this.executor.drainCallResults();
         if (calls != null && !calls.isEmpty()) {
            sr.setCallResults(calls);
         }

         String log = ctx.collect();
         if (log != null && !log.isEmpty()) {
            sr.setLog(log);
         }

         List<StepResult.Embed> embeds = ctx.collectEmbeds();
         if (embeds != null) {
            for (StepResult.Embed e : embeds) {
               sr.addEmbed(e);
            }
         }

         this.result.addStepResult(sr);
         return err;
      }
   }

   void releaseHttpClient() {
      if (this.ownsHttpClient && !this.httpClientReleased && this.karate != null && this.karate.client != null) {
         this.httpClientReleased = true;

         try {
            this.httpClientFactory.release(this.karate.client);
         } catch (Exception var2) {
            logger.warn("error releasing http client: {}", var2.getMessage());
         }
      }
   }

   void ownServer(MockServer server) {
      if (this.servers == null) {
         this.servers = new ArrayList<>();
      }

      this.servers.add(server);
   }

   void stopServers() {
      if (this.servers != null) {
         Suite suite = this.featureRuntime != null ? this.featureRuntime.getSuite() : null;

         for (MockServer server : this.servers) {
            if (suite == null || suite.releaseServer(server)) {
               try {
                  server.stopAndWait();
               } catch (RuntimeException var5) {
                  logger.warn("mock server on port {} did not stop: {}", server.getPort(), var5.getMessage());
               }
            }
         }

         this.servers = null;
      }
   }

   private void closeChannels() {
      if (this.channels != null) {
         for (Channel channel : this.channels) {
            try {
               channel.afterScenario();
            } catch (Exception var4) {
               logger.warn("error closing channel: {}", var4.getMessage());
            }
         }

         this.channels = null;
      }
   }

   private void closeDriver() {
      if (this.driver != null) {
         if (this.driverInherited) {
            this.driver = null;
         } else {
            try {
               if (!this.driver.getOptions().isStop()) {
                  logger.warn("driver.stop=false — leaving browser running, scenario will not close it: {}", this.scenario.getName());
                  this.driver = null;
                  return;
               }
            } catch (Exception var5) {
               logger.debug("could not read driver options for stop check: {}", var5.getMessage());
            }

            if (this.featureRuntime != null
               && this.featureRuntime.isSharedScope()
               && this.featureRuntime.getCallerScenario() != null
               && !this.driver.isTerminated()) {
               this.featureRuntime.getCallerScenario().setDriverFromCallee(this);
               this.driver = null;
            } else {
               if (this.driverFromProvider) {
                  DriverProvider provider = null;
                  if (this.featureRuntime != null && this.featureRuntime.getSuite() != null) {
                     provider = this.featureRuntime.getSuite().getDriverProvider();
                  }

                  if (provider != null) {
                     try {
                        provider.release(this, this.driver);
                        logger.debug("Released driver to provider for scenario: {}", this.scenario.getName());
                     } catch (Exception var4) {
                        logger.warn("Error releasing driver to provider: {}", var4.getMessage());
                     }
                  }
               } else {
                  try {
                     this.driver.quit();
                  } catch (Exception var3) {
                     logger.warn("Error closing driver: {}", var3.getMessage());
                  }
               }

               this.driver = null;
            }
         }
      }
   }

   public Driver getDriverIfPresent() {
      return this.driver;
   }

   public boolean hasChannels() {
      return this.channels != null && !this.channels.isEmpty();
   }

   public String getDriverScope() {
      return this.driverScope;
   }

   public boolean isCallerScope() {
      return "caller".equals(this.driverScope);
   }

   public void setDriverFromCallee(ScenarioRuntime callee) {
      if (callee.driver != null && !callee.driver.isTerminated()) {
         if (this.driver != null && this.driver != callee.driver && this.driver.isTerminated()) {
            this.closeDriver();
         }

         this.driver = callee.driver;
         this.driverFromProvider = callee.driverFromProvider;
         this.driverInherited = false;
         callee.driverInherited = true;
         logger.debug("Driver propagated from callee to caller");
         Map<String, Object> calleeRootBindings = callee.karate.engine.getRootBindings();

         for (Entry<String, Object> entry : calleeRootBindings.entrySet()) {
            String key = entry.getKey();
            if (!"karate".equals(key) && !"read".equals(key) && !"match".equals(key)) {
               this.karate.engine.putRootBinding(key, entry.getValue());
            }
         }
      }
   }

   public void setListenResult(Object result) {
      this.listenResult = result;
      this.listenLatch.countDown();
   }

   public Object waitForListenResult(long timeoutMs) {
      try {
         if (this.listenLatch.await(timeoutMs, TimeUnit.MILLISECONDS)) {
            Object result = this.listenResult;
            this.listenResult = null;
            this.listenLatch = new CountDownLatch(1);
            return result;
         } else {
            throw new RuntimeException("listen timed out after " + timeoutMs + "ms");
         }
      } catch (InterruptedException var4) {
         Thread.currentThread().interrupt();
         throw new RuntimeException("listen interrupted", var4);
      }
   }

   public Object getListenResult() {
      return this.listenResult;
   }

   public boolean isPerfMode() {
      return this.featureRuntime != null && this.featureRuntime.getSuite() != null && this.featureRuntime.getSuite().isPerfMode();
   }

   public PerfHook getPerfHook() {
      return this.featureRuntime != null && this.featureRuntime.getSuite() != null ? this.featureRuntime.getSuite().getPerfHook() : null;
   }

   public void capturePerfEvent(PerfEvent event) {
      this.logLastPerfEvent(null);
      this.prevPerfEvent = event;
   }

   public boolean logLastPerfEvent(String failureMessage) {
      if (this.prevPerfEvent == null) {
         return false;
      } else if (!this.isPerfMode()) {
         this.prevPerfEvent = null;
         return false;
      } else {
         boolean failed = failureMessage != null;
         if (failed) {
            this.prevPerfEvent.setFailed(true);
            this.prevPerfEvent.setMessage(failureMessage);
         }

         PerfHook hook = this.getPerfHook();
         if (hook != null) {
            hook.reportPerfEvent(this.prevPerfEvent);
         }

         this.prevPerfEvent = null;
         return failed;
      }
   }

   private String perfFailureMessage() {
      if (!this.result.isFailed()) {
         return null;
      } else {
         String display = this.result.getPerfFailureMessage();
         return display != null ? display : "scenario failed";
      }
   }

   private void reportPerfFailure(String failureMessage) {
      if (this.logLastPerfEvent(failureMessage)) {
         this.result.setPerfFailureReported(true);
      }

      if (failureMessage != null
         && !this.result.isPerfFailureReported()
         && this.isPerfMode()
         && this.featureRuntime != null
         && this.featureRuntime.isTopLevel()) {
         this.reportSyntheticFailurePerfEvent(failureMessage);
         this.result.setPerfFailureReported(true);
      }
   }

   private void reportSyntheticFailurePerfEvent(String failureMessage) {
      PerfHook hook = this.getPerfHook();
      if (hook != null) {
         long start = this.result.getStartTime();
         long end = this.result.getEndTime();
         if (end < start) {
            end = start;
         }

         String name = this.scenario.getFeature().getResource().getRelativePath();
         PerfEvent event = new PerfEvent(start, end, name, 0);
         event.setFailed(true);
         event.setMessage(failureMessage);
         hook.reportPerfEvent(event);
      }
   }

   public void captureCustomPerfEvent(String name, long startTime, long endTime) {
      if (this.isPerfMode()) {
         this.capturePerfEvent(new PerfEvent(startTime, endTime, name, 200));
      }
   }

   private static class CallSingleException {
      final Exception cause;

      CallSingleException(Exception cause) {
         this.cause = cause;
      }
   }

   private static final class FailureDecision {
      private Boolean override;

      void proceed() {
         this.override = Boolean.TRUE;
      }

      void stop() {
         this.override = Boolean.FALSE;
      }

      boolean resolve(boolean configDefault) {
         return this.override == null ? configDefault : this.override;
      }
   }
}
