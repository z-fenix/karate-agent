package io.karatelabs.core;

import io.karatelabs.common.Json;
import io.karatelabs.common.Resource;
import io.karatelabs.gherkin.ExamplesTable;
import io.karatelabs.gherkin.Feature;
import io.karatelabs.gherkin.FeatureSection;
import io.karatelabs.gherkin.Scenario;
import io.karatelabs.gherkin.ScenarioOutline;
import io.karatelabs.gherkin.Table;
import io.karatelabs.gherkin.Tag;
import io.karatelabs.js.JavaCallable;
import io.karatelabs.output.LogContext;
import io.karatelabs.output.ResultListener;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;
import org.slf4j.Logger;

public class FeatureRuntime implements Callable<FeatureResult> {
   private static final Logger logger = LogContext.RUNTIME_LOGGER;
   private final Suite suite;
   private final Feature feature;
   private final FeatureRuntime caller;
   private final ScenarioRuntime callerScenario;
   private final boolean sharedScope;
   private final Map<String, Object> callArg;
   private final String callTagSelector;
   private final Set<Integer> callLineFilters;
   private final Map<String, Object> CALLONCE_CACHE_LOCAL = new ConcurrentHashMap<>();
   private final Map<String, Object> SETUPONCE_CACHE_LOCAL = new ConcurrentHashMap<>();
   private final ReentrantLock callOnceLock = new ReentrantLock();
   private volatile ScenarioRuntime lastExecuted;
   private FeatureResult result;
   private final Map<Integer, Integer> outlineCompletedCounts = new HashMap<>();
   private final Set<Integer> outlineEnteredSections = new HashSet<>();
   private int loopIndex = -1;
   private boolean exitFired;

   public FeatureRuntime(Feature feature) {
      this(null, feature, null, null, false, null);
   }

   public FeatureRuntime(Suite suite, Feature feature) {
      this(suite, feature, null, null, false, null);
   }

   public FeatureRuntime(Suite suite, Feature feature, FeatureRuntime caller, Map<String, Object> callArg) {
      this(suite, feature, caller, null, false, callArg);
   }

   public FeatureRuntime(Suite suite, Feature feature, FeatureRuntime caller, ScenarioRuntime callerScenario, boolean sharedScope, Map<String, Object> callArg) {
      this(suite, feature, caller, callerScenario, sharedScope, callArg, null);
   }

   public FeatureRuntime(
      Suite suite,
      Feature feature,
      FeatureRuntime caller,
      ScenarioRuntime callerScenario,
      boolean sharedScope,
      Map<String, Object> callArg,
      String callTagSelector
   ) {
      this(suite, feature, caller, callerScenario, sharedScope, callArg, callTagSelector, null);
   }

   public FeatureRuntime(
      Suite suite,
      Feature feature,
      FeatureRuntime caller,
      ScenarioRuntime callerScenario,
      boolean sharedScope,
      Map<String, Object> callArg,
      String callTagSelector,
      Set<Integer> callLineFilters
   ) {
      this.suite = suite;
      this.feature = feature;
      this.caller = caller;
      this.callerScenario = callerScenario;
      this.sharedScope = sharedScope;
      this.callArg = callArg;
      this.callTagSelector = callTagSelector;
      this.callLineFilters = callLineFilters;
      this.result = new FeatureResult(feature);
      this.result.setCallDepth(this.getCallDepth());
   }

   public int getCallDepth() {
      int depth = 0;

      for (FeatureRuntime fr = this.caller; fr != null; fr = fr.caller) {
         depth++;
      }

      return depth;
   }

   public boolean isTopLevel() {
      return this.caller == null;
   }

   public boolean isCalled() {
      return this.caller != null;
   }

   private void notifyListeners(Consumer<ResultListener> action) {
      if (this.suite != null && this.isTopLevel()) {
         for (ResultListener listener : this.suite.getResultListeners()) {
            action.accept(listener);
         }
      }
   }

   public static FeatureRuntime of(Feature feature) {
      return new FeatureRuntime(feature);
   }

   public static FeatureRuntime of(Suite suite, Feature feature) {
      return new FeatureRuntime(suite, feature);
   }

   boolean exitFired() {
      return this.exitFired;
   }

   public FeatureResult call() {
      this.result.setStartTime(System.currentTimeMillis());
      this.notifyListeners(listener -> listener.onFeatureStart(this.feature));

      try {
         if (this.suite != null) {
            this.suite.fireEvent(FeatureRunEvent.enter(this));
         }

         try {
            if (this.suite != null
               && this.suite.parallel
               && this.isTopLevel()
               && this.suite.getScenarioExecutor() != null
               && this.suite.getScenarioSemaphore() != null) {
               logger.debug("Running scenarios in parallel mode for feature: {}", this.feature.getName());
               this.runScenariosParallel();
            } else {
               if (this.suite != null && this.suite.parallel && this.isTopLevel()) {
                  logger.warn(
                     "Falling back to sequential execution for feature '{}' - executor: {}, semaphore: {}",
                     new Object[]{this.feature.getName(), this.suite.getScenarioExecutor() != null, this.suite.getScenarioSemaphore() != null}
                  );
               }

               for (Scenario scenario : this.selectedScenarios()) {
                  if (this.suite != null && this.suite.isAborted()) {
                     break;
                  }

                  this.maybeFireOutlineEnter(scenario);
                  this.executeScenario(scenario);
               }
            }
         } catch (Exception var6) {
            logger.error("Error during scenario iteration: {}", var6.getMessage());
            ScenarioResult errorResult = this.createErrorScenarioResult(var6);
            this.result.addScenarioResult(errorResult);
         }

         if (this.lastExecuted != null && this.isTopLevel()) {
            this.invokeAfterFeatureHook(this.lastExecuted);
         }

         this.result.setEndTime(System.currentTimeMillis());
         if (this.suite != null) {
            this.exitFired = true;
            this.suite.fireEvent(FeatureRunEvent.exit(this, this.result));
         }
      } finally {
         this.notifyListeners(listener -> listener.onFeatureEnd(this.result));
         if (this.suite != null && this.isTopLevel() && !this.suite.retainCallResults) {
            this.result.releaseCallResults();
         }

         if (this.suite != null && this.isTopLevel() && !this.suite.retainStepLogs && !this.suite.isPerfMode()) {
            this.result.releaseStepLogs();
         }
      }

      return this.result;
   }

   private void runScenariosParallel() {
      ExecutorService executor = this.suite.getScenarioExecutor();
      Semaphore semaphore = this.suite.getScenarioSemaphore();
      List<Future<ScenarioResult>> futures = new ArrayList<>();

      for (Scenario scenario : this.selectedScenarios()) {
         if (this.suite.isAborted()) {
            break;
         }

         this.maybeFireOutlineEnter(scenario);
         Future<ScenarioResult> future = executor.submit(() -> {
            String scenarioName = scenario.getName();
            logger.debug("Waiting for semaphore permit: {} (available: {})", scenarioName, semaphore.availablePermits());
            semaphore.acquire();
            logger.debug("Acquired semaphore permit: {} (available: {})", scenarioName, semaphore.availablePermits());
            this.suite.acquireLane();
            boolean ownListeners = false;

            ScenarioResult var5x;
            try {
               ownListeners = this.suite.initThreadListenersIfAbsent();
               var5x = this.executeScenarioParallel(scenario);
            } finally {
               if (ownListeners) {
                  this.suite.cleanupThreadListeners();
               }

               this.suite.releaseLane();
               semaphore.release();
               logger.debug("Released semaphore permit: {} (available: {})", scenarioName, semaphore.availablePermits());
            }

            return var5x;
         });
         futures.add(future);
      }

      for (Future<ScenarioResult> future : futures) {
         try {
            ScenarioResult scenarioResult = future.get();
            this.result.addScenarioResult(scenarioResult);
         } catch (Exception var8) {
            logger.error("Error collecting scenario result: {}", var8.getMessage());
            ScenarioResult errorResult = this.createErrorScenarioResult(var8);
            this.result.addScenarioResult(errorResult);
         }
      }
   }

   private ScenarioResult executeScenarioParallel(Scenario scenario) {
      this.notifyListeners(listener -> listener.onScenarioStart(scenario));
      ScenarioLockManager.LockHandle lockHandle = this.suite.getLockManager().acquire(scenario);

      ScenarioResult scenarioResult;
      try {
         ScenarioRuntime sr = new ScenarioRuntime(this, scenario);
         scenarioResult = sr.call();
         this.lastExecuted = sr;
      } catch (Exception var8) {
         logger.error("Error executing scenario '{}': {}", scenario.getName(), var8.getMessage());
         scenarioResult = this.createErrorScenarioResult(scenario, var8);
      } finally {
         if (lockHandle != null) {
            this.suite.getLockManager().release(lockHandle);
         }
      }

      ScenarioResult completed = scenarioResult;
      this.notifyListeners(listener -> listener.onScenarioEnd(completed));
      this.releaseCallResultsIfUnwatched(completed);
      return scenarioResult;
   }

   private void executeScenario(Scenario scenario) {
      this.notifyListeners(listener -> listener.onScenarioStart(scenario));
      ScenarioLockManager.LockHandle lockHandle = null;
      if (this.suite != null && this.isTopLevel()) {
         lockHandle = this.suite.getLockManager().acquire(scenario);
      }

      ScenarioResult scenarioResult;
      try {
         ScenarioRuntime sr = new ScenarioRuntime(this, scenario);
         scenarioResult = sr.call();
         this.lastExecuted = sr;
         if (this.isTopLevel() && this.isLastScenarioInOutline(scenario)) {
            this.invokeAfterScenarioOutlineHook(sr);
         }
      } catch (Exception var8) {
         logger.error("Error executing scenario '{}': {}", scenario.getName(), var8.getMessage());
         scenarioResult = this.createErrorScenarioResult(scenario, var8);
      } finally {
         if (this.isTopLevel() && lockHandle != null) {
            this.suite.getLockManager().release(lockHandle);
         }
      }

      this.result.addScenarioResult(scenarioResult);
      ScenarioResult completed = scenarioResult;
      this.notifyListeners(listener -> listener.onScenarioEnd(completed));
      this.releaseCallResultsIfUnwatched(completed);
   }

   private void releaseCallResultsIfUnwatched(ScenarioResult completed) {
      if (completed != null && this.suite != null && this.suite.canReleaseCallResultsAtScenarioEnd()) {
         completed.releaseCallResults();
      }
   }

   private ScenarioResult createErrorScenarioResult(Exception error) {
      Scenario errorScenario = this.feature.getSections().isEmpty()
         ? Scenario.createError(this.feature, "Feature execution failed", this.feature.getLine())
         : (
            this.feature.getSections().get(0).isOutline()
               ? this.feature.getSections().get(0).getScenarioOutline().toScenario(null, 0, this.feature.getLine(), null)
               : this.feature.getSections().get(0).getScenario()
         );
      return this.createErrorScenarioResult(errorScenario, error);
   }

   private ScenarioResult createErrorScenarioResult(Scenario scenario, Exception error) {
      long now = System.currentTimeMillis();
      ScenarioResult scenarioResult = new ScenarioResult(scenario);
      if (this.suite != null) {
         scenarioResult.setExecutionIndex(this.suite.nextExecutionIndex());
      }

      scenarioResult.setStartTime(now);
      scenarioResult.setEndTime(now);
      scenarioResult.setThreadName(Thread.currentThread().getName());
      String errorMessage = "Scenario execution failed: " + error.getMessage();
      scenarioResult.addStepResult(StepResult.fakeFailure(errorMessage, now, error));
      return scenarioResult;
   }

   private void invokeAfterFeatureHook(ScenarioRuntime sr) {
      KarateConfig config = sr.getConfig();
      if (config.getAfterFeature() instanceof JavaCallable callable) {
         sr.runAsCurrent(() -> {
            try {
               callable.call(null);
            } catch (Exception var4x) {
               logger.warn("afterFeature hook failed: {}", var4x.getMessage());
               this.appendHookFailure(sr.getResult(), "afterFeature", var4x);
            }
         });
      }
   }

   private void maybeFireOutlineEnter(Scenario scenario) {
      if (this.suite != null && scenario.isOutlineExample()) {
         FeatureSection section = scenario.getSection();
         if (section != null) {
            ScenarioOutline outline = section.getScenarioOutline();
            if (outline != null) {
               if (this.outlineEnteredSections.add(section.getIndex())) {
                  this.suite.fireEvent(OutlineRunEvent.enter(this, outline));
               }
            }
         }
      }
   }

   private boolean isLastScenarioInOutline(Scenario scenario) {
      if (!scenario.isOutlineExample()) {
         return false;
      } else {
         FeatureSection section = scenario.getSection();
         int sectionIndex = section.getIndex();
         ScenarioOutline outline = section.getScenarioOutline();
         int completed = this.outlineCompletedCounts.merge(sectionIndex, 1, Integer::sum);
         return completed == outline.getNumScenarios();
      }
   }

   private void invokeAfterScenarioOutlineHook(ScenarioRuntime sr) {
      KarateConfig config = sr.getConfig();
      if (config.getAfterScenarioOutline() instanceof JavaCallable callable) {
         try {
            callable.call(null);
         } catch (Exception var6) {
            logger.warn("afterScenarioOutline hook failed: {}", var6.getMessage());
            this.appendHookFailure(sr.getResult(), "afterScenarioOutline", var6);
         }
      }
   }

   private void appendHookFailure(ScenarioResult scenarioResult, String hookName, Throwable error) {
      long now = System.currentTimeMillis();
      scenarioResult.addStepResult(StepResult.fakeFailure(hookName + " hook failed: " + error.getMessage(), now, error));
      scenarioResult.setEndTime(now);
   }

   private Iterable<Scenario> selectedScenarios() {
      return () -> new FeatureRuntime.ScenarioIterator();
   }

   public Resource resolve(String path) {
      if (path.startsWith("this:")) {
         path = path.substring(5);
      }

      return this.feature.getResource().resolve(path);
   }

   public Suite getSuite() {
      return this.suite;
   }

   public Feature getFeature() {
      return this.feature;
   }

   public FeatureRuntime getCaller() {
      return this.caller;
   }

   public ScenarioRuntime getCallerScenario() {
      return this.callerScenario;
   }

   public boolean isSharedScope() {
      return this.sharedScope;
   }

   public Map<String, Object> getCallArg() {
      return this.callArg;
   }

   public int getLoopIndex() {
      return this.loopIndex;
   }

   public void setLoopIndex(int loopIndex) {
      this.loopIndex = loopIndex;
      this.result.setLoopIndex(loopIndex);
   }

   public ScenarioRuntime getLastExecuted() {
      return this.lastExecuted;
   }

   public FeatureResult getResult() {
      return this.result;
   }

   public ReentrantLock getCallOnceLock() {
      return this.suite != null && this.isTopLevel() ? this.suite.getCallOnceLock(this.featureKey()) : this.callOnceLock;
   }

   public Map<String, Object> getCallOnceCache() {
      return this.suite != null && this.isTopLevel() ? this.suite.getCallOnceCache(this.featureKey()) : this.CALLONCE_CACHE_LOCAL;
   }

   public Map<String, Object> getSetupOnceCache() {
      return this.suite != null && this.isTopLevel() ? this.suite.getSetupOnceCache(this.featureKey()) : this.SETUPONCE_CACHE_LOCAL;
   }

   private String featureKey() {
      Resource resource = this.feature.getResource();
      URI uri = resource.getUri();
      return uri != null ? uri.toString() : resource.getPrefixedPath();
   }

   private class ScenarioIterator implements Iterator<Scenario> {
      private final List<FeatureSection> sections;
      private int sectionIndex = 0;
      private int exampleTableIndex = 0;
      private int exampleRowIndex = 0;
      private Scenario nextScenario = null;
      private List<?> dynamicData = null;
      private Scenario dynamicTemplateScenario = null;

      ScenarioIterator() {
         this.sections = FeatureRuntime.this.feature.getSections();
         this.advance();
      }

      @Override
      public boolean hasNext() {
         return this.nextScenario != null;
      }

      public Scenario next() {
         Scenario current = this.nextScenario;
         this.advance();
         return current;
      }

      private void advance() {
         this.nextScenario = null;
         if (this.dynamicData == null || this.dynamicTemplateScenario == null || !this.processDynamicData()) {
            while (this.sectionIndex < this.sections.size()) {
               FeatureSection section = this.sections.get(this.sectionIndex);
               if (section.isOutline()) {
                  ScenarioOutline outline = section.getScenarioOutline();
                  List<ExamplesTable> tables = outline.getExamplesTables();

                  while (this.exampleTableIndex < tables.size()) {
                     ExamplesTable table = tables.get(this.exampleTableIndex);
                     if (table.getTable().isDynamic()) {
                        Scenario templateScenario = outline.toScenario(table.getTable().getDynamicExpression(), -1, table.getLine(), table.getTags());
                        List<?> data = this.evaluateDynamicExpression(templateScenario);
                        if (data != null && !data.isEmpty()) {
                           this.dynamicData = data;
                           this.dynamicTemplateScenario = templateScenario;
                           this.exampleRowIndex = 0;
                           if (this.processDynamicData()) {
                              this.exampleTableIndex++;
                              return;
                           }
                        }

                        this.exampleTableIndex++;
                        this.exampleRowIndex = 0;
                        this.dynamicData = null;
                        this.dynamicTemplateScenario = null;
                     } else {
                        int rowCount = table.getTable().getRows().size() - 1;
                        if (this.exampleRowIndex >= rowCount) {
                           this.exampleTableIndex++;
                           this.exampleRowIndex = 0;
                        } else {
                           Map<String, Object> exampleData = table.getTable().getExampleData(this.exampleRowIndex);
                           Map<String, String> rawData = table.getTable().getRawExampleData(this.exampleRowIndex);
                           int exampleIndex = this.exampleRowIndex++;
                           Scenario scenario = outline.toScenario(null, exampleIndex, table.getLine(), table.getTags());
                           scenario.setExampleData(exampleData);

                           for (String key : exampleData.keySet()) {
                              String resolved = rawData.get(key);
                              if (resolved == null) {
                                 resolved = "";
                              }

                              scenario.replace("<" + key + ">", resolved);
                              scenario.replace("${" + key + "}", resolved);
                           }

                           if (this.shouldSelect(scenario)) {
                              this.nextScenario = scenario;
                              return;
                           }
                        }
                     }
                  }

                  this.sectionIndex++;
                  this.exampleTableIndex = 0;
                  this.exampleRowIndex = 0;
               } else {
                  Scenario scenario = section.getScenario();
                  this.sectionIndex++;
                  boolean explicitlyTargeted = FeatureRuntime.this.callTagSelector != null
                     || FeatureRuntime.this.callLineFilters != null && !FeatureRuntime.this.callLineFilters.isEmpty();
                  if ((!scenario.isSetup() || explicitlyTargeted) && this.shouldSelect(scenario)) {
                     this.nextScenario = scenario;
                     return;
                  }
               }
            }
         }
      }

      private boolean processDynamicData() {
         while (this.exampleRowIndex < this.dynamicData.size()) {
            Object item = this.dynamicData.get(this.exampleRowIndex);
            int rowIndex = this.exampleRowIndex++;
            if (item instanceof Map<?, ?> rawExampleData) {
               Map<String, Object> exampleData = (Map<String, Object>) rawExampleData;
               Scenario scenario = this.dynamicTemplateScenario.copy(rowIndex);
               scenario.setExampleData(exampleData);

               for (String key : exampleData.keySet()) {
                  String resolved = Json.stringifyStrict(exampleData.get(key));
                  scenario.replace("<" + key + ">", resolved);
                  scenario.replace("${" + key + "}", resolved);
               }

               if (this.shouldSelect(scenario)) {
                  this.nextScenario = scenario;
                  return true;
               }
            }
         }

         this.dynamicData = null;
         this.dynamicTemplateScenario = null;
         return false;
      }

      private List<?> evaluateDynamicExpression(Scenario templateScenario) {
         try {
            ScenarioRuntime sr = new ScenarioRuntime(FeatureRuntime.this, templateScenario);
            sr.setSkipBackground(true);
            String expression = templateScenario.getDynamicExpression();

            Object result;
            try {
               result = sr.eval(expression);
            } finally {
               sr.releaseHttpClient();
            }

            if (result instanceof List) {
               return (List<?>)result;
            } else if (result instanceof JavaCallable) {
               return this.evaluateGeneratorFunction(result);
            } else {
               throw new RuntimeException(
                  "Dynamic expression must return a list or function: " + expression + ", got: " + (result != null ? result.getClass().getName() : "null")
               );
            }
         } catch (Exception var9) {
            throw new RuntimeException("Failed to evaluate dynamic expression: " + templateScenario.getDynamicExpression() + " - " + var9.getMessage(), var9);
         }
      }

      private List<Map<String, Object>> evaluateGeneratorFunction(Object function) {
         List<Map<String, Object>> results = new ArrayList<>();
         JavaCallable callable = (JavaCallable)function;
         int index = 0;

         while (true) {
            Object rowValue;
            try {
               rowValue = callable.call(null, index);
            } catch (Exception var7) {
               FeatureRuntime.logger.warn("Generator function threw exception at index {}: {}", index, var7.getMessage());
               break;
            }

            if (rowValue == null) {
               break;
            }

            if (!(rowValue instanceof Map)) {
               FeatureRuntime.logger.debug("Generator function returned non-map at index {}, stopping: {}", index, rowValue);
               break;
            }

            results.add((Map<String, Object>)rowValue);
            index++;
         }

         return results;
      }

      private boolean shouldSelect(Scenario scenario) {
         Set<Integer> lines = null;
         boolean isCallScoped = false;
         if (FeatureRuntime.this.callLineFilters != null && !FeatureRuntime.this.callLineFilters.isEmpty()) {
            lines = FeatureRuntime.this.callLineFilters;
            isCallScoped = true;
         } else if (FeatureRuntime.this.suite != null && !FeatureRuntime.this.suite.lineFilters.isEmpty() && FeatureRuntime.this.isTopLevel()) {
            String featureUri = FeatureRuntime.this.feature.getResource().getUri().toString();
            lines = FeatureRuntime.this.suite.lineFilters.get(featureUri);
         }

         if (lines != null && !lines.isEmpty()) {
            return !this.matchesLineFilter(scenario, lines)
               ? false
               : isCallScoped || FeatureRuntime.this.suite == null || FeatureRuntime.this.suite.scenarioName == null || this.matchesScenarioName(scenario);
         } else if (FeatureRuntime.this.callTagSelector != null) {
            return this.matchesCallTag(scenario, FeatureRuntime.this.callTagSelector);
         } else if (FeatureRuntime.this.isCalled()) {
            return true;
         } else if (FeatureRuntime.this.suite != null && FeatureRuntime.this.suite.scenarioName != null) {
            return this.matchesScenarioName(scenario);
         } else if (FeatureRuntime.this.suite != null && FeatureRuntime.this.suite.isSkipTagFiltering()) {
            return true;
         } else {
            Boolean cached = scenario.getSelected();
            if (cached != null) {
               return cached;
            } else {
               List<Tag> tags = scenario.getTagsEffective();
               TagSelector selector = new TagSelector(tags);
               String karateEnv = FeatureRuntime.this.suite != null ? FeatureRuntime.this.suite.env : null;
               return selector.evaluate(FeatureRuntime.this.suite != null ? FeatureRuntime.this.suite.tagSelector : null, karateEnv);
            }
         }
      }

      private boolean matchesLineFilter(Scenario scenario, Set<Integer> lines) {
         int scenarioLine = scenario.getLine();
         if (lines.contains(scenarioLine)) {
            return true;
         } else {
            if (scenario.isOutlineExample() && scenario.getSection() != null && scenario.getSection().isOutline()) {
               ScenarioOutline outline = scenario.getSection().getScenarioOutline();
               if (lines.contains(outline.getLine())) {
                  return true;
               }

               for (ExamplesTable exTable : outline.getExamplesTables()) {
                  if (exTable.getLine() == scenarioLine) {
                     Table t = exTable.getTable();
                     int dataRowIndex = scenario.getExampleIndex() + 1;
                     if (dataRowIndex < t.getRows().size()) {
                        int rowLine = t.getLineNumberForRow(dataRowIndex);
                        if (lines.contains(rowLine)) {
                           return true;
                        }
                     }
                     break;
                  }
               }
            }

            if (scenario.getSteps() != null && !scenario.getSteps().isEmpty()) {
               int firstStepLine = scenario.getSteps().get(0).getLine();
               int lastStepLine = scenario.getSteps().get(scenario.getSteps().size() - 1).getLine();
               int rangeStart = Math.min(scenarioLine, firstStepLine);

               for (int line : lines) {
                  if (line >= rangeStart && line <= lastStepLine) {
                     return true;
                  }
               }
            }

            return false;
         }
      }

      private boolean matchesScenarioName(Scenario scenario) {
         String name = scenario.getName();
         return name == null ? false : name.trim().equals(FeatureRuntime.this.suite.scenarioName);
      }

      private boolean matchesCallTag(Scenario scenario, String tagSelector) {
         List<Tag> tags = scenario.getTagsEffective();
         if (tags.isEmpty()) {
            return !tagSelector.startsWith("@");
         } else if (tagSelector.startsWith("~")) {
            String required = tagSelector.substring(1);

            for (Tag tag : tags) {
               if (this.matchesTag(tag, required)) {
                  return false;
               }
            }

            return true;
         } else {
            String selector = tagSelector.startsWith("@") ? tagSelector.substring(1) : tagSelector;

            for (Tag tagx : tags) {
               if (this.matchesTag(tagx, selector)) {
                  return true;
               }
            }

            return false;
         }
      }

      private boolean matchesTag(Tag tag, String selector) {
         int eqPos = selector.indexOf(61);
         if (eqPos == -1) {
            return tag.getName().equals(selector);
         } else {
            String selectorName = selector.substring(0, eqPos);
            String selectorValue = selector.substring(eqPos + 1);
            return tag.getName().equals(selectorName) && tag.getValues().contains(selectorValue);
         }
      }
   }
}
