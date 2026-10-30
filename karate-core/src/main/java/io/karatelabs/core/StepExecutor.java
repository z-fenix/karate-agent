package io.karatelabs.core;

import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.PathNotFoundException;
import com.jayway.jsonpath.Predicate;
import io.karatelabs.common.DataUtils;
import io.karatelabs.common.Json;
import io.karatelabs.common.Resource;
import io.karatelabs.common.StringUtils;
import io.karatelabs.common.Xml;
import io.karatelabs.driver.Driver;
import io.karatelabs.gherkin.Feature;
import io.karatelabs.gherkin.GherkinParser;
import io.karatelabs.gherkin.MatchExpression;
import io.karatelabs.gherkin.Step;
import io.karatelabs.gherkin.Table;
import io.karatelabs.http.AuthHandler;
import io.karatelabs.http.BasicAuthHandler;
import io.karatelabs.http.BearerAuthHandler;
import io.karatelabs.http.ClientCredentialsAuthHandler;
import io.karatelabs.http.HttpRequest;
import io.karatelabs.http.HttpRequestBuilder;
import io.karatelabs.http.HttpResponse;
import io.karatelabs.js.DebugPointFactory;
import io.karatelabs.js.EngineException;
import io.karatelabs.js.JavaCallable;
import io.karatelabs.js.RunInterceptor;
import io.karatelabs.match.Match;
import io.karatelabs.match.Result;
import io.karatelabs.output.LogContext;
import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.locks.ReentrantLock;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class StepExecutor {
   private static final Logger logger = LogContext.RUNTIME_LOGGER;
   private final ScenarioRuntime runtime;
   private List<FeatureResult> stepCallResults;
   private static final Object XPATH_NOT_PRESENT = new Object();
   private static final LogContext.LogWriter SCENARIO_LOG = LogContext.with(LogContext.SCENARIO_LOGGER);
   private static final Pattern BARE_REFERENCE = Pattern.compile("[A-Za-z_$][\\w$]*(?:\\.[A-Za-z_$][\\w$]*|\\[\\d+])*");
   private static final Set<String> HTTP_METHODS = Set.of("GET", "POST", "PUT", "DELETE", "PATCH", "HEAD", "OPTIONS", "CONNECT", "TRACE");
   private static final Object REMOVE_MARKER = new Object();

   public StepExecutor(ScenarioRuntime runtime) {
      this.runtime = runtime;
   }

   void addCallResult(FeatureResult result) {
      StepExecutor target = this.collectingExecutor();
      if (target.stepCallResults == null) {
         target.stepCallResults = new ArrayList<>();
      }

      target.stepCallResults.add(result);
   }

   private StepExecutor collectingExecutor() {
      ScenarioRuntime current = ScenarioRuntime.currentOrNull();
      return current != null ? current.getExecutor() : this;
   }

   void resetCallResults() {
      this.stepCallResults = null;
   }

   List<FeatureResult> drainCallResults() {
      List<FeatureResult> out = this.stepCallResults;
      this.stepCallResults = null;
      return out;
   }

   private Suite getSuite() {
      FeatureRuntime fr = this.runtime.getFeatureRuntime();
      return fr != null ? fr.getSuite() : null;
   }

   public StepResult execute(Step step) {
      long startTime = System.currentTimeMillis();
      long startNanos = System.nanoTime();
      boolean isFake = step.isFake();
      Suite suite = isFake ? null : this.getSuite();
      if (suite != null) {
         boolean proceed = suite.fireEvent(StepRunEvent.enter(step, this.runtime));
         if (!proceed) {
            StepResult skipped = StepResult.skipped(step, startTime);
            this.collectLogsAndEmbeds(skipped);
            return skipped;
         }
      }

      RunInterceptor<Object> interceptor = (RunInterceptor<Object>)(isFake ? null : this.runtime.getInterceptor());
      DebugPointFactory<Object> pointFactory = null;
      Object point = null;
      if (interceptor != null && this.runtime.getPointFactory() != null) {
         DebugPointFactory<Object> factory = (DebugPointFactory<Object>)this.runtime.getPointFactory();
         pointFactory = factory;
         String sourcePath = step.getFeature().getResource().getRelativePath();
         point = factory.create(2, step.getLine(), sourcePath, step, null);
         RunInterceptor.Action action = interceptor.beforeExecute(point);
         if (action == RunInterceptor.Action.SKIP) {
            StepResult skipped = StepResult.skipped(step, startTime);
            this.collectLogsAndEmbeds(skipped);
            if (suite != null) {
               suite.fireEvent(StepRunEvent.exit(skipped, this.runtime));
            }

            return skipped;
         }

         if (action == RunInterceptor.Action.WAIT) {
            action = interceptor.waitForResume();
            if (action == RunInterceptor.Action.SKIP) {
               StepResult skipped = StepResult.skipped(step, startTime);
               this.collectLogsAndEmbeds(skipped);
               if (suite != null) {
                  suite.fireEvent(StepRunEvent.exit(skipped, this.runtime));
               }

               return skipped;
            }
         }
      }

      RunInterceptor<Object> finalInterceptor = interceptor;
      DebugPointFactory<Object> finalPointFactory = pointFactory;
      Object finalPoint = point;

      try {
         String keyword = step.getKeyword();
         String text = step.getText();
         if (keyword != null && StepUtils.hasPunctuation(keyword)) {
            String fullExpr = keyword + text;
            if (step.getDocString() != null) {
               fullExpr = fullExpr + step.getDocString();
            }

            this.runtime.eval(fullExpr, step);
         } else if (keyword == null) {
            this.executeExpression(step);
         } else {
            switch (keyword) {
               case "def":
                  this.executeDef(step);
                  break;
               case "set":
                  this.executeSet(step);
                  break;
               case "remove":
                  this.executeRemove(step);
                  break;
               case "text":
                  this.executeText(step);
                  break;
               case "json":
                  this.executeJson(step);
                  break;
               case "xml":
                  this.executeXml(step);
                  break;
               case "xmlstring":
                  this.executeXmlString(step);
                  break;
               case "string":
                  this.executeString(step);
                  break;
               case "bytes":
                  this.executeBytes(step);
                  break;
               case "csv":
                  this.executeCsv(step);
                  break;
               case "yaml":
                  this.executeYaml(step);
                  break;
               case "copy":
                  this.executeCopy(step);
                  break;
               case "table":
                  this.executeTable(step);
                  break;
               case "replace":
                  this.executeReplace(step);
                  break;
               case "match":
                  this.executeMatch(step);
                  break;
               case "assert":
                  this.executeAssert(step);
                  break;
               case "print":
                  this.executePrint(step);
                  break;
               case "url":
                  this.executeUrl(step);
                  break;
               case "path":
                  this.executePath(step);
                  break;
               case "param":
                  this.executeParam(step);
                  break;
               case "params":
                  this.executeParams(step);
                  break;
               case "header":
                  this.executeHeader(step);
                  break;
               case "headers":
                  this.executeHeaders(step);
                  break;
               case "cookie":
                  this.executeCookie(step);
                  break;
               case "cookies":
                  this.executeCookies(step);
                  break;
               case "form field":
                  this.executeFormField(step);
                  break;
               case "form fields":
                  this.executeFormFields(step);
                  break;
               case "request":
                  this.executeRequest(step);
                  break;
               case "retry until":
                  this.executeRetryUntil(step);
                  break;
               case "method":
                  this.executeMethod(step);
                  break;
               case "soap action":
                  this.executeSoapAction(step);
                  break;
               case "status":
                  this.executeStatus(step);
                  break;
               case "multipart file":
                  this.executeMultipartFile(step);
                  break;
               case "multipart field":
                  this.executeMultipartField(step);
                  break;
               case "multipart fields":
                  this.executeMultipartFields(step);
                  break;
               case "multipart files":
                  this.executeMultipartFiles(step);
                  break;
               case "multipart entity":
                  this.executeMultipartEntity(step);
                  break;
               case "call":
                  this.executeCall(step);
                  break;
               case "callonce":
                  this.executeCallOnce(step);
                  break;
               case "eval":
                  this.executeEval(step);
                  break;
               case "doc":
                  this.executeDoc(step);
                  break;
               case "configure":
                  this.executeConfigure(step);
                  break;
               case "driver":
                  this.executeDriver(step);
                  break;
               default:
                  if (text == null || !text.trim().startsWith("(")) {
                     throw new RuntimeException("unknown keyword: " + keyword);
                  }

                  this.runtime.eval(keyword + text, step);
            }
         }

         long elapsedNanos = System.nanoTime() - startNanos;
         StepResult result = StepResult.passed(step, startTime, elapsedNanos);
         this.collectLogsAndEmbeds(result);
         if (finalInterceptor != null && finalPointFactory != null) {
            Object afterPoint = finalPoint;
            if (finalPoint == null) {
               String sourcePathx = step.getFeature().getResource().getRelativePath();
               afterPoint = finalPointFactory.create(2, step.getLine(), sourcePathx, step, null);
            }

            finalInterceptor.afterExecute(afterPoint, result, null);
         }

         if (suite != null) {
            suite.fireEvent(StepRunEvent.exit(result, this.runtime));
         }

         return result;
      } catch (Exception | AssertionError var21) {
         long elapsedNanosx = System.nanoTime() - startNanos;
         Throwable failure = ExtHint.decorate(var21, suite);
         StepResult resultx = StepResult.failed(step, startTime, elapsedNanosx, failure);
         this.collectLogsAndEmbeds(resultx);
         if (interceptor != null && pointFactory != null) {
            Object afterPoint = point;
            if (point == null) {
               String sourcePathx = step.getFeature().getResource().getRelativePath();
               afterPoint = pointFactory.create(2, step.getLine(), sourcePathx, step, null);
            }

            interceptor.afterExecute(afterPoint, resultx, failure);
         }

         if (suite != null) {
            suite.fireEvent(StepRunEvent.exit(resultx, this.runtime));
         }

         return resultx;
      }
   }

   private void collectLogsAndEmbeds(StepResult result) {
      LogContext ctx = LogContext.get();
      result.setLog(ctx.collect());
      List<StepResult.Embed> embeds = ctx.collectEmbeds();
      if (embeds != null) {
         for (StepResult.Embed embed : embeds) {
            result.addEmbed(embed);
         }
      }

      if (this.stepCallResults != null && !this.stepCallResults.isEmpty()) {
         result.setCallResults(this.stepCallResults);
      }

      this.stepCallResults = null;
   }

   private void executeExpression(Step step) {
      this.runtime.eval(step.getText(), step);
   }

   private void executeDef(Step step) {
      String text = step.getText();
      int eqIndex = StepUtils.findAssignmentOperator(text);
      if (eqIndex < 0) {
         throw new RuntimeException("def requires '=' assignment: " + text);
      } else {
         String name = text.substring(0, eqIndex).trim();
         this.validateVariableName(name);
         String expr = text.substring(eqIndex + 1).trim();
         String docString = step.getDocString();
         boolean rhsIsDocString = false;
         if (expr.isEmpty() && docString != null) {
            expr = docString;
            docString = null;
            rhsIsDocString = true;
         }

         if (expr.startsWith("call ")) {
            String callExpr = appendDocStringCallArg(expr.substring(5).trim(), docString);
            this.executeCallWithResult(callExpr, name);
         } else if (expr.startsWith("callonce ")) {
            String callExpr = appendDocStringCallArg(expr.substring(9).trim(), docString);
            this.executeCallOnceWithResult(callExpr, name);
         } else {
            Object value = this.evalKarateExpression(expr, rhsIsDocString ? step : null);
            this.runtime.setVariable(name, value);
         }
      }
   }

   private void executeCallWithResult(String callExpr, String resultVar) {
      StepUtils.CallParts parts = StepUtils.splitCallExpr(callExpr);
      String firstToken = parts.callable();
      if (!StepUtils.isLiteralFeatureRead(callExpr)) {
         Object evaluated = null;

         try {
            evaluated = this.runtime.eval(firstToken);
         } catch (Exception var9) {
            logger.debug("call target probe failed, treating as feature path: {}", var9.getMessage());
         }

         if (evaluated instanceof JavaCallable fn) {
            Object arg = parts.arg() != null ? this.evalKarateExpression(parts.arg()) : null;
            Object result = arg != null ? fn.call(null, arg) : fn.call(null);
            this.runtime.setVariable(resultVar, result);
            return;
         }

         if (evaluated instanceof FeatureCall || evaluated instanceof Feature) {
            this.executeFeatureCall(evaluated, parts.arg(), resultVar);
            return;
         }
      }

      StepExecutor.CallExpression call = this.parseCallExpression(callExpr);
      call.resultVar = resultVar;
      FeatureRuntime fr = this.runtime.getFeatureRuntime();
      Feature calledFeature;
      if (call.sameFile) {
         calledFeature = fr != null ? fr.getFeature() : null;
         if (calledFeature == null) {
            throw new RuntimeException("call with tag selector requires a feature context");
         }
      } else {
         Resource calledResource = fr != null ? fr.resolve(call.path) : Resource.path(call.path);
         calledFeature = Feature.read(calledResource);
      }

      if (call.argList != null) {
         this.runtime.setVariable(resultVar, this.callFeatureLoop(calledFeature, call.argList, call.tagSelector, call.lineFilters));
      } else {
         Map<String, Object> resultVars = this.callFeatureSingle(calledFeature, call.arg, call.tagSelector, call.lineFilters);
         if (resultVars != null) {
            this.runtime.setVariable(resultVar, resultVars);
         }
      }
   }

   private void executeCallOnceWithResult(String callExpr, String resultVar) {
      String cacheKey = "callonce:" + callExpr;
      FeatureRuntime fr = this.runtime.getFeatureRuntime();
      if (fr == null) {
         this.executeCallWithResult(callExpr, resultVar);
      } else {
         Map<String, Object> cache = fr.getCallOnceCache();
         ReentrantLock lock = fr.getCallOnceLock();
         Object cached = cache.get(cacheKey);
         if (cached != null) {
            this.runtime.setVariable(resultVar, StepUtils.deepCopy(cached));
         } else {
            lock.lock();

            try {
               Object rechecked = cache.get(cacheKey);
               if (rechecked == null) {
                  this.executeCallWithResult(callExpr, resultVar);
                  Object resultValue = this.runtime.getVariable(resultVar);
                  if (resultValue != null) {
                     cache.put(cacheKey, StepUtils.deepCopy(resultValue));
                  }

                  return;
               }

               this.runtime.setVariable(resultVar, StepUtils.deepCopy(rechecked));
            } finally {
               lock.unlock();
            }
         }
      }
   }

   private FeatureRuntime runNestedFeature(
      Feature calledFeature, Map<String, Object> callArg, boolean sharedScope, String tagSelector, Set<Integer> lineFilters, int loopIndex
   ) {
      FeatureRuntime fr = this.runtime.getFeatureRuntime();
      FeatureRuntime nestedFr = new FeatureRuntime(
         fr != null ? fr.getSuite() : null, calledFeature, fr, this.runtime, sharedScope, callArg, tagSelector, lineFilters
      );
      if (loopIndex >= 0) {
         nestedFr.setLoopIndex(loopIndex);
      }

      FeatureResult featureResult = nestedFr.call();
      this.addCallResult(featureResult);
      this.checkFeatureResult(featureResult);
      return nestedFr;
   }

   private static Map<String, Object> lastVars(FeatureRuntime nestedFr) {
      return calleeResult(nestedFr.getLastExecuted());
   }

   static Map<String, Object> calleeResult(ScenarioRuntime callee) {
      return callee == null ? null : StepUtils.calleeDelta(callee.getInheritedVariables(), callee.getAllVariables(), callee.getAssignedVariables());
   }

   Map<String, Object> callFeatureSingle(Feature calledFeature, Map<String, Object> callArg, String tagSelector, Set<Integer> lineFilters) {
      return lastVars(this.runNestedFeature(calledFeature, callArg, false, tagSelector, lineFilters, -1));
   }

   List<Map<String, Object>> callFeatureLoop(Feature calledFeature, List<?> argList, String tagSelector, Set<Integer> lineFilters) {
      List<Map<String, Object>> results = new ArrayList<>();
      int loopIndex = 0;

      for (Object item : argList) {
         Map<String, Object> callArg = item instanceof Map ? (Map)item : null;
         Map<String, Object> vars = lastVars(this.runNestedFeature(calledFeature, callArg, false, tagSelector, lineFilters, loopIndex));
         if (vars != null) {
            results.add(vars);
         }

         loopIndex++;
      }

      return results;
   }

   void checkFeatureResult(FeatureResult featureResult) {
      if (featureResult.isFailed()) {
         String msg = featureResult.getFailureMessage();
         throw new RuntimeException("called feature failed: " + featureResult.getDisplayName() + (msg != null ? " - " + msg : ""));
      }
   }

   private void propagateFromCallee(ScenarioRuntime calleeRuntime, String resultVar) {
      if (calleeRuntime != null) {
         if (resultVar != null) {
            this.runtime.setVariable(resultVar, calleeResult(calleeRuntime));
         } else {
            Map<String, Object> delta = calleeResult(calleeRuntime);

            for (Entry<String, Object> entry : calleeRuntime.getAllVariables().entrySet()) {
               if (delta.containsKey(entry.getKey())) {
                  this.runtime.setVariable(entry.getKey(), entry.getValue());
               } else {
                  this.runtime.putUnclaimedVariable(entry.getKey(), entry.getValue());
               }
            }

            this.runtime.getConfig().copyFrom(calleeRuntime.getConfig());
            this.runtime.getKarate().client.apply(this.runtime.getConfig());
            this.runtime.getCookieJar().putAll(calleeRuntime.getCookieJar());
            if (calleeRuntime.getDriverIfPresent() != null) {
               this.runtime.setDriverFromCallee(calleeRuntime);
            }
         }
      }
   }

   private void executeSet(Step step) {
      String text = step.getText().trim();
      Table table = step.getTable();
      if (table != null) {
         this.executeSetWithTable(text, table);
      } else {
         int eqIndex = StepUtils.findAssignmentOperator(text);
         if (eqIndex > 0) {
            String leftPart = text.substring(0, eqIndex).trim();
            String valueExpr = text.substring(eqIndex + 1).trim();
            if (valueExpr.isEmpty() && step.getDocString() != null) {
               valueExpr = step.getDocString();
               text = leftPart + " = " + valueExpr;
            }

            int spaceSlashIdx = leftPart.indexOf(" /");
            if (spaceSlashIdx > 0) {
               String varName = leftPart.substring(0, spaceSlashIdx).trim();
               String xpath = leftPart.substring(spaceSlashIdx + 1).trim();
               Object target = this.runtime.getVariable(varName);
               if (target == null) {
                  throw new RuntimeException("variable is null or not set: " + varName);
               }

               if (!(target instanceof Node)) {
                  throw new RuntimeException("cannot set xpath on non-XML variable: " + varName);
               }

               Object value = this.evalKarateExpression(valueExpr);
               Document doc = target instanceof Document ? (Document)target : ((Node)target).getOwnerDocument();
               if (value instanceof Node) {
                  Xml.setByPath(doc, xpath, (Node)value);
               } else {
                  Xml.setByPath(doc, xpath, value == null ? "" : value.toString());
               }

               return;
            }

            if (StepUtils.isPureJsonPath(leftPart)) {
               StepUtils.VarAndPath vp = StepUtils.splitVarAndJsonPath(leftPart);
               Object targetx = this.runtime.getVariable(vp.var);
               if (targetx instanceof Map || targetx instanceof List) {
                  Object value = this.evalKarateExpression(valueExpr);
                  Json.of(targetx).set(vp.path, value);
                  return;
               }
            }

            if (StepUtils.isPlainIdentifier(leftPart)) {
               Object value = this.evalKarateExpression(valueExpr);
               this.runtime.setVariable(leftPart, value);
               return;
            }
         }

         this.runtime.eval(text);
      }
   }

   private void executeSetWithTable(String varExpr, Table table) {
      List<String> headers = new ArrayList<>(table.getKeys());
      List<List<String>> rows = table.getRows();
      String basePath = null;
      int spaceIdx = varExpr.indexOf(32);
      int dotIdx = varExpr.indexOf(46);
      String varName;
      if (spaceIdx > 0) {
         varName = varExpr.substring(0, spaceIdx);
         basePath = varExpr.substring(spaceIdx + 1).trim();
      } else if (dotIdx > 0) {
         varName = varExpr.substring(0, dotIdx);
         basePath = varExpr.substring(dotIdx + 1);
      } else {
         varName = varExpr;
      }

      if (basePath != null && basePath.startsWith("/")) {
         this.executeSetXmlWithTable(varName, basePath, table);
      } else {
         boolean isPathValueFormat = headers.contains("path") && headers.contains("value");
         boolean isArrayFormat = !isPathValueFormat && headers.size() > 1;
         Object target = this.runtime.getVariable(varName);
         if (target == null) {
            target = isArrayFormat ? new ArrayList() : new LinkedHashMap();
            this.runtime.setVariable(varName, target);
         }

         if (basePath != null && target instanceof Map) {
            String cleanPath = basePath.startsWith("$.") ? basePath.substring(2) : basePath;
            Object nested = StepUtils.getOrCreatePath((Map<String, Object>)target, cleanPath);
            target = nested;
         }

         if (isPathValueFormat) {
            int pathIdx = headers.indexOf("path");
            int valueIdx = headers.indexOf("value");

            for (int i = 1; i < rows.size(); i++) {
               List<String> row = rows.get(i);
               String path = row.get(pathIdx);
               String valueExpr = valueIdx < row.size() ? row.get(valueIdx) : "";
               if (valueExpr != null && !valueExpr.isEmpty()) {
                  boolean keepNull = valueExpr.startsWith("(") && valueExpr.endsWith(")");
                  Object value = this.runtime.eval(wrapJsonLikeExpression(valueExpr));
                  if (value != null || keepNull) {
                     StepUtils.setValueAtPath(target, path, value);
                  }
               }
            }
         } else if (isArrayFormat) {
            int pathIdx = headers.indexOf("path");
            if (pathIdx < 0) {
               pathIdx = 0;
            }

            List<Object> resultList;
            if (target instanceof List<?> rawList) {
               resultList = (List<Object>)(Object) rawList;
            } else {
               resultList = new ArrayList<>();
               this.runtime.setVariable(varName, resultList);
            }

            int maxIdx = 0;

            for (int ix = 0; ix < headers.size(); ix++) {
               if (ix != pathIdx) {
                  try {
                     int idx = Integer.parseInt(headers.get(ix));
                     maxIdx = Math.max(maxIdx, idx + 1);
                  } catch (NumberFormatException var24) {
                     maxIdx = Math.max(maxIdx, ix);
                  }
               }
            }

            while (resultList.size() < maxIdx) {
               resultList.add(new LinkedHashMap());
            }

            for (int rowIdx = 1; rowIdx < rows.size(); rowIdx++) {
               List<String> row = rows.get(rowIdx);
               String path = row.get(pathIdx);

               for (int ixx = 0; ixx < headers.size(); ixx++) {
                  if (ixx != pathIdx && ixx < row.size()) {
                     String valueExpr = row.get(ixx);
                     if (valueExpr != null && !valueExpr.isEmpty()) {
                        int targetIdx;
                        try {
                           targetIdx = Integer.parseInt(headers.get(ixx));
                        } catch (NumberFormatException var25) {
                           targetIdx = ixx - (pathIdx < ixx ? 1 : 0);
                        }

                        while (resultList.size() <= targetIdx) {
                           resultList.add(new LinkedHashMap());
                        }

                        boolean keepNull = valueExpr.startsWith("(") && valueExpr.endsWith(")");
                        Object value = this.runtime.eval(wrapJsonLikeExpression(valueExpr));
                        if (value != null || keepNull) {
                           Object element = resultList.get(targetIdx);
                           if (element == null) {
                              element = new LinkedHashMap();
                              resultList.set(targetIdx, element);
                           }

                           StepUtils.setValueAtPath(element, path, value);
                        }
                     }
                  }
               }
            }
         }
      }
   }

   private void executeSetXmlWithTable(String varName, String basePath, Table table) {
      List<String> headers = new ArrayList<>(table.getKeys());
      List<List<String>> rows = table.getRows();
      Object existing = this.runtime.getVariable(varName);
      Document doc;
      if (existing instanceof Document existingDoc) {
         doc = existingDoc;
      } else if (existing instanceof Node existingNode) {
         doc = existingNode.getOwnerDocument();
      } else {
         doc = Xml.newDocument();
         this.runtime.setVariable(varName, doc);
      }

      Xml.getNodeByPath(doc, basePath, true);
      int pathIdx = headers.indexOf("path");
      int valueIdx = headers.indexOf("value");
      if (pathIdx < 0) {
         pathIdx = 0;
      }

      List<Integer> indexColumns = new ArrayList<>();

      for (int col = 0; col < headers.size(); col++) {
         if (col != pathIdx) {
            String header = headers.get(col);

            try {
               Integer.parseInt(header);
               indexColumns.add(col);
            } catch (NumberFormatException var20) {
            }
         }
      }

      if (!indexColumns.isEmpty()) {
         for (int i = 1; i < rows.size(); i++) {
            List<String> row = rows.get(i);
            if (row.size() > pathIdx) {
               String rowPath = row.get(pathIdx);

               for (int colx : indexColumns) {
                  if (colx < row.size()) {
                     String valueExpr = row.get(colx);
                     if (valueExpr != null && !valueExpr.isEmpty()) {
                        String indexStr = headers.get(colx);
                        String fullPath = basePath + "[" + indexStr + "]/" + rowPath;
                        Object value = this.evalKarateExpression(valueExpr);
                        if (value instanceof Node) {
                           Xml.setByPath(doc, fullPath, (Node)value);
                        } else {
                           Xml.setByPath(doc, fullPath, value == null ? "" : value.toString());
                        }
                     }
                  }
               }
            }
         }
      } else {
         if (valueIdx < 0) {
            valueIdx = 1;
         }

         for (int ix = 1; ix < rows.size(); ix++) {
            List<String> row = rows.get(ix);
            if (row.size() > pathIdx) {
               String rowPath = row.get(pathIdx);
               String valueExpr = valueIdx < row.size() ? row.get(valueIdx) : "";
               if (valueExpr != null && !valueExpr.isEmpty()) {
                  String fullPath = basePath + "/" + rowPath;
                  Object value = this.evalKarateExpression(valueExpr);
                  if (value instanceof Node) {
                     Xml.setByPath(doc, fullPath, (Node)value);
                  } else {
                     Xml.setByPath(doc, fullPath, value == null ? "" : value.toString());
                  }
               }
            }
         }
      }
   }

   private void executeRemove(Step step) {
      String text = step.getText().trim();
      int spaceIndex = text.indexOf(32);
      if (spaceIndex > 0) {
         String varName = text.substring(0, spaceIndex);
         String path = text.substring(spaceIndex + 1).trim();
         Object target = this.runtime.getVariable(varName);
         if (target == null) {
            return;
         }

         if (path.startsWith("/") && target instanceof Node node) {
            Document doc = node instanceof Document ? (Document)node : node.getOwnerDocument();
            Xml.removeByPath(doc, path);
            return;
         }

         if (path.startsWith("$")) {
            if (target instanceof Map || target instanceof List) {
               Json.of(target).remove(path);
               return;
            }

            String pathWithoutDollar = path.substring(1);
            if (pathWithoutDollar.startsWith(".")) {
               pathWithoutDollar = pathWithoutDollar.substring(1);
            }

            if (target instanceof Map) {
               Map<String, Object> map = (Map<String, Object>) target;
               StepUtils.removeAtPath(map, pathWithoutDollar);
            }
         }
      } else {
         if (StepUtils.containsJsonPathWildcard(text)) {
            StepUtils.VarAndPath vp = StepUtils.splitVarAndJsonPath(text);
            Object targetx = this.runtime.getVariable(vp.var);
            if (targetx instanceof Map || targetx instanceof List) {
               Json.of(targetx).remove(vp.path);
               return;
            }
         }

         if (StepUtils.isPureJsonPath(text)) {
            StepUtils.VarAndPath vp = StepUtils.splitVarAndJsonPath(text);
            Object targetx = this.runtime.getVariable(vp.var);
            if (targetx instanceof Map || targetx instanceof List) {
               Json.of(targetx).remove(vp.path);
               return;
            }
         }

         int dotIndex = text.indexOf(46);
         if (dotIndex < 0) {
            this.runtime.setVariable(text, null);
         } else {
            this.runtime.eval("delete " + text);
         }
      }
   }

   private void executeText(Step step) {
      String text = step.getText();
      int eqIndex = StepUtils.findAssignmentOperator(text);
      String name = text.substring(0, eqIndex).trim();
      String value = step.getDocString();
      if (value == null) {
         value = text.substring(eqIndex + 1).trim();
      }

      this.runtime.setVariable(name, value);
   }

   private void executeJson(Step step) {
      String text = step.getText();
      int eqIndex = StepUtils.findAssignmentOperator(text);
      String name = text.substring(0, eqIndex).trim();
      String expr = step.getDocString() != null ? step.getDocString() : text.substring(eqIndex + 1).trim();
      Object value = this.evalKarateExpression(expr);
      if (value instanceof Node) {
         value = Xml.toObject((Node)value);
      } else if (value instanceof String s) {
         value = Json.of(s).value();
      } else if (!(value instanceof Map) && !(value instanceof List) && value != null) {
         value = Json.of(value).value();
      }

      this.runtime.setVariable(name, value);
   }

   private void executeXml(Step step) {
      String text = step.getText();
      int eqIndex = StepUtils.findAssignmentOperator(text);
      String name = text.substring(0, eqIndex).trim();
      String expr;
      if (step.getDocString() != null) {
         expr = step.getDocString();
      } else {
         expr = text.substring(eqIndex + 1).trim();
      }

      Object value = this.evalKarateExpression(expr);
      if (value instanceof String) {
         value = Xml.toXmlDoc((String)value);
      } else if (!(value instanceof Node)) {
         if (value instanceof Map) {
            value = Xml.fromMap((Map<String, Object>)value);
         } else if (value != null) {
            Object jsonValue = Json.of(value).value();
            value = Xml.fromObject("root", jsonValue);
         }
      }

      this.runtime.setVariable(name, value);
   }

   private void executeXmlString(Step step) {
      String text = step.getText();
      int eqIndex = StepUtils.findAssignmentOperator(text);
      String name = text.substring(0, eqIndex).trim();
      String expr = text.substring(eqIndex + 1).trim();
      Object value = this.runtime.eval(wrapJsonLikeExpression(expr));
      String xmlString;
      if (value instanceof Node) {
         xmlString = Xml.toString((Node)value, false);
      } else {
         xmlString = value.toString();
      }

      this.runtime.setVariable(name, xmlString);
   }

   private void executeString(Step step) {
      String text = step.getText();
      int eqIndex = StepUtils.findAssignmentOperator(text);
      String name = text.substring(0, eqIndex).trim();
      String expr = text.substring(eqIndex + 1).trim();
      Object value = this.runtime.eval(wrapJsonLikeExpression(expr));
      value = this.processEmbeddedExpressions(value);
      String stringValue;
      if (value instanceof Node) {
         stringValue = Xml.toString((Node)value, false);
      } else {
         stringValue = Json.stringifyStrict(value);
      }

      this.runtime.setVariable(name, stringValue);
   }

   private void executeBytes(Step step) {
      String text = step.getText();
      int eqIndex = StepUtils.findAssignmentOperator(text);
      String name = text.substring(0, eqIndex).trim();
      Object value;
      if (step.getDocString() != null) {
         value = step.getDocString();
      } else {
         String expr = text.substring(eqIndex + 1).trim();
         value = this.runtime.eval(wrapJsonLikeExpression(expr));
         value = this.processEmbeddedExpressions(value);
      }

      this.runtime.setVariable(name, Json.toBytes(value));
   }

   private void executeCsv(Step step) {
      String text = step.getText();
      int eqIndex = StepUtils.findAssignmentOperator(text);
      String name = text.substring(0, eqIndex).trim();
      String csvText;
      if (step.getDocString() != null) {
         csvText = step.getDocString();
      } else {
         String expr = text.substring(eqIndex + 1).trim();
         Object value = this.runtime.eval(expr);
         if (value instanceof byte[]) {
            csvText = new String((byte[])value, StandardCharsets.UTF_8);
         } else {
            csvText = value.toString();
         }
      }

      List<Map<String, Object>> result = DataUtils.fromCsv(csvText);
      this.runtime.setVariable(name, result);
   }

   private void executeYaml(Step step) {
      String text = step.getText();
      int eqIndex = StepUtils.findAssignmentOperator(text);
      String name = text.substring(0, eqIndex).trim();
      String yamlText;
      if (step.getDocString() != null) {
         yamlText = step.getDocString();
      } else {
         String expr = text.substring(eqIndex + 1).trim();
         Object value = this.runtime.eval(expr);
         yamlText = value.toString();
      }

      Object result = DataUtils.fromYaml(yamlText);
      this.runtime.setVariable(name, result);
   }

   private void executeCopy(Step step) {
      String text = step.getText();
      int eqIndex = StepUtils.findAssignmentOperator(text);
      String name = text.substring(0, eqIndex).trim();
      String expr = text.substring(eqIndex + 1).trim();
      Object value = this.runtime.eval(wrapJsonLikeExpression(expr));
      Object copy = StepUtils.deepCopy(value);
      this.runtime.setVariable(name, copy);
   }

   private void executeTable(Step step) {
      String text = step.getText().trim();
      Table table = step.getTable();
      if (table == null) {
         throw new RuntimeException("table keyword requires a data table");
      } else {
         List<Map<String, String>> rawRows = table.getRowsAsMaps();
         List<Map<String, Object>> result = new ArrayList<>(rawRows.size());

         for (Map<String, String> rawRow : rawRows) {
            Map<String, Object> row = new LinkedHashMap<>(rawRow.size());

            for (Entry<String, String> entry : rawRow.entrySet()) {
               String expr = entry.getValue();
               if (expr != null && !expr.isEmpty()) {
                  boolean keepNull = expr.startsWith("(") && expr.endsWith(")");
                  Object value = this.runtime.eval(wrapJsonLikeExpression(expr));
                  if (value != null || keepNull) {
                     row.put(entry.getKey(), value);
                  }
               }
            }

            result.add(row);
         }

         this.runtime.setVariable(text, result);
      }
   }

   private void executeReplace(Step step) {
      String text = step.getText();
      Table table = step.getTable();
      if (table != null) {
         String varName = text.trim();
         Object varValue = this.runtime.getVariable(varName);
         if (varValue == null) {
            throw new RuntimeException("no variable found with name: " + varName);
         }

         String varText;
         if (varValue instanceof Node) {
            varText = Xml.toString((Node)varValue, false);
         } else if (!(varValue instanceof Map) && !(varValue instanceof List)) {
            varText = varValue.toString();
         } else {
            varText = Json.stringifyStrict(varValue);
         }

         for (Map<String, String> row : table.getRowsAsMaps()) {
            String token = row.get("token");
            String valueExpr = row.get("value");
            Object replaceValue = this.runtime.eval(valueExpr);
            String replacement = replaceValue != null ? replaceValue.toString() : "";
            if (token != null && !token.isEmpty() && Character.isLetterOrDigit(token.charAt(0))) {
               token = "<" + token + ">";
            }

            if (token != null) {
               varText = varText.replace(token, replacement);
            }
         }

         this.runtime.setVariable(varName, varText);
      } else {
         int eqIndex = StepUtils.findAssignmentOperator(text);
         if (eqIndex < 0) {
            throw new RuntimeException("replace requires '=' assignment: " + text);
         }

         String nameAndToken = text.substring(0, eqIndex).trim();
         String replaceExpr = text.substring(eqIndex + 1).trim();
         int dotIndex = nameAndToken.indexOf(46);
         if (dotIndex < 0) {
            throw new RuntimeException("replace requires varName.token syntax: " + nameAndToken);
         }

         String varNamex = nameAndToken.substring(0, dotIndex).trim();
         String tokenx = nameAndToken.substring(dotIndex + 1).trim();
         Object varValuex = this.runtime.getVariable(varNamex);
         if (varValuex == null) {
            throw new RuntimeException("no variable found with name: " + varNamex);
         }

         String varText;
         if (varValuex instanceof Node) {
            varText = Xml.toString((Node)varValuex, false);
         } else if (!(varValuex instanceof Map) && !(varValuex instanceof List)) {
            varText = varValuex.toString();
         } else {
            varText = Json.stringifyStrict(varValuex);
         }

         Object replaceValuex = this.runtime.eval(replaceExpr);
         String replacementx = replaceValuex != null ? replaceValuex.toString() : "";
         if (!tokenx.isEmpty() && Character.isLetterOrDigit(tokenx.charAt(0))) {
            tokenx = "<" + tokenx + ">";
         }

         String replaced = varText.replace(tokenx, replacementx);
         this.runtime.setVariable(varNamex, replaced);
      }
   }

   private void executeMatch(Step step) {
      Result result = this.evalMatchString(step.getText(), step.getDocString());
      if (!result.pass) {
         throw new AssertionError(withCommentLabel(step, result.message));
      }
   }

   private static String withCommentLabel(Step step, String message) {
      List<String> comments = step.getComments();
      return comments != null && !comments.isEmpty() ? comments.getLast() + "\n" + message : message;
   }

   public Result evalMatchString(String expression, String docString) {
      MatchExpression expr = GherkinParser.parseMatchExpression(expression);
      Object actual = this.evalMatchExpression(expr.getActualExpr(), false);
      Object expected;
      if (docString == null || expr.getExpectedExpr() != null && !expr.getExpectedExpr().isEmpty()) {
         expected = this.evalMatchExpression(expr.getExpectedExpr(), true);
      } else {
         expected = this.evalMatchExpression(docString, true);
      }

      Match.Type matchType = Match.Type.valueOf(expr.getMatchTypeName());
      boolean matchEachEmptyAllowed = this.runtime.getConfig().isMatchEachEmptyAllowed();
      Result result = Match.execute(this.runtime.getEngine(), this.runtime.markerPolicy(), matchType, actual, expected, matchEachEmptyAllowed);
      String expectedExpr = docString == null || expr.getExpectedExpr() != null && !expr.getExpectedExpr().isEmpty() ? expr.getExpectedExpr() : docString;
      this.fireMatchEvent(matchType, expr.getActualExpr(), expectedExpr, actual, expected, result);
      return result;
   }

   private void fireMatchEvent(Match.Type matchType, String actualExpr, String expectedExpr, Object actual, Object expected, Result result) {
      Suite suite = this.getSuite();
      if (suite != null) {
         try {
            suite.fireEvent(MatchRunEvent.exit(matchType, actualExpr, expectedExpr, actual, expected, result, this.runtime));
         } catch (Exception var9) {
            logger.debug("match event listener failed: {}", var9.getMessage());
         }
      }
   }

   private Object evalMatchExpression(String expr, boolean forMatch) {
      if (expr == null || expr.isEmpty()) {
         return null;
      } else if (expr.startsWith("<")) {
         return Xml.toXmlDoc(expr);
      } else {
         if (StringUtils.looksLikeJson(expr)) {
            try {
               Object value = Json.of(expr).value();
               this.warnBareReferences(expr, value);
               return this.processEmbeddedExpressions(value, false, forMatch);
            } catch (Exception var15) {
            }
         }

         if (expr.startsWith("header ")) {
            String headerName = expr.substring(7).trim();
            return this.getResponseHeader(headerName);
         } else if (expr.startsWith("get[") || expr.startsWith("get ")) {
            return this.evalGetExpression(expr);
         } else if (expr.startsWith("$")) {
            return this.evalDollarPrefixedExpression(expr);
         } else {
            if (expr.startsWith("/")) {
               Object response = this.runtime.getVariable("response");
               if (response instanceof Node) {
                  return KarateJsUtils.evalXmlPath((Node)response, expr);
               }
            }

            if (expr.endsWith(" /") || expr.equals("/")) {
               String varName = expr.endsWith(" /") ? expr.substring(0, expr.length() - 2).trim() : null;
               if (varName != null) {
                  Object target = this.runtime.getVariable(varName);
                  if (target instanceof Node) {
                     return target;
                  }
               }
            }

            Object xpathResult = this.tryEvalXPathExpression(expr);
            if (xpathResult == null) {
               int spaceIdx = expr.indexOf(32);
               if (spaceIdx > 0) {
                  String varName = expr.substring(0, spaceIdx);
                  String remainder = expr.substring(spaceIdx + 1).trim();
                  if (remainder.startsWith("/") || this.isXPathFunction(remainder)) {
                     Object target = this.runtime.getVariable(varName);
                     if (target instanceof Node) {
                        return this.evalXmlPathWithFunction((Node)target, remainder);
                     }
                  }
               }

               if (expr.contains("[*]") || expr.contains("[?") || expr.contains("..")) {
                  int bracketIdx = expr.indexOf(91);
                  int doubleDotIdx = expr.indexOf("..");
                  int splitIdx;
                  if (bracketIdx <= 0 || doubleDotIdx >= 0 && bracketIdx >= doubleDotIdx) {
                     splitIdx = doubleDotIdx;
                  } else {
                     splitIdx = bracketIdx;
                  }

                  if (splitIdx > 0) {
                     String basePath = expr.substring(0, splitIdx);
                     int firstDot = basePath.indexOf(46);
                     String varName = firstDot > 0 ? basePath.substring(0, firstDot) : basePath;
                     if (varName.matches("[a-zA-Z_][a-zA-Z0-9_]*")) {
                        Object target;
                        if (firstDot > 0) {
                           target = this.runtime.eval(basePath);
                        } else {
                           target = this.runtime.getVariable(varName);
                        }

                        if (target != null) {
                           String jsonPath = "$" + expr.substring(splitIdx);
                           return JsonPath.read(target, jsonPath, new Predicate[0]);
                        }
                     }
                  }
               }

               int dollarIdx = expr.indexOf(" $");
               if (dollarIdx > 0) {
                  String varName = expr.substring(0, dollarIdx).trim();
                  String jsonPath = expr.substring(dollarIdx + 1).trim();
                  if (StepUtils.isValidVariableName(varName) && jsonPath.startsWith("$")) {
                     Object targetx = this.runtime.getVariable(varName);
                     if (targetx != null) {
                        return JsonPath.read(targetx, jsonPath, new Predicate[0]);
                     }
                  }
               }

               boolean propertyAccessChain = expr.contains(".") && !expr.contains("(");

               Object result;
               try {
                  result = this.runtime.eval(wrapJsonLikeExpression(expr));
               } catch (Exception var14) {
                  if (propertyAccessChain && this.rootVariableDefined(expr)) {
                     return "#notpresent";
                  }

                  throw var14;
               }

               if (result == null && propertyAccessChain) {
                  int lastDot = expr.lastIndexOf(46);
                  if (lastDot > 0) {
                     String basePath = expr.substring(0, lastDot);
                     String propName = expr.substring(lastDot + 1);

                     try {
                        if (!(this.runtime.eval(basePath) instanceof Map<?, ?> baseMap)) {
                           return "#notpresent";
                        }

                        if (!baseMap.containsKey(propName)) {
                           return "#notpresent";
                        }
                     } catch (Exception var13) {
                        return "#notpresent";
                     }
                  }
               }

               return result;
            } else {
               return xpathResult == XPATH_NOT_PRESENT ? "#notpresent" : xpathResult;
            }
         }
      }
   }

   private boolean rootVariableDefined(String expr) {
      int i;
      for (i = 0; i < expr.length(); i++) {
         char c = expr.charAt(i);
         if (!Character.isLetterOrDigit(c) && c != '_' && c != '$') {
            break;
         }
      }

      String root = expr.substring(0, i);
      return !root.isEmpty() && this.runtime.getAllVariables().containsKey(root);
   }

   private Object tryEvalXPathExpression(String expr) {
      int spaceSlashIdx = expr.indexOf(" /");
      if (spaceSlashIdx > 0) {
         String varName = expr.substring(0, spaceSlashIdx).trim();
         String xpath = expr.substring(spaceSlashIdx + 1).trim();
         if (StepUtils.isValidVariableName(varName)) {
            Object target = this.runtime.getVariable(varName);
            if (target instanceof Node) {
               Object result = KarateJsUtils.evalXmlPath((Node)target, xpath);
               return result == null ? XPATH_NOT_PRESENT : result;
            }
         }
      }

      int slashIdx = expr.indexOf(47);
      if (slashIdx > 0 && (spaceSlashIdx < 0 || slashIdx < spaceSlashIdx) && expr.charAt(slashIdx - 1) != ' ') {
         String varName = expr.substring(0, slashIdx);
         if (StepUtils.isValidVariableName(varName)) {
            Object target = this.runtime.getVariable(varName);
            if (target instanceof Node) {
               String xpath = expr.substring(slashIdx);
               Object result = KarateJsUtils.evalXmlPath((Node)target, xpath);
               return result == null ? XPATH_NOT_PRESENT : result;
            }
         }
      }

      return null;
   }

   private boolean isXPathFunction(String text) {
      return text.matches("^[a-z-]+\\(.+");
   }

   private Object evalXmlPathWithFunction(Node node, String path) {
      try {
         Object result = KarateJsUtils.evalXmlPath(node, path);
         if (result != null) {
            return result;
         }
      } catch (Exception var7) {
      }

      String textValue = Xml.getTextValueByPath(node, path);
      if (textValue == null || textValue.isEmpty()) {
         return null;
      } else if (!path.startsWith("count(") && !path.startsWith("count ")) {
         try {
            return textValue.contains(".") ? Double.parseDouble(textValue) : Integer.parseInt(textValue);
         } catch (NumberFormatException var5) {
            return textValue;
         }
      } else {
         try {
            return Integer.parseInt(textValue.split("\\.")[0]);
         } catch (NumberFormatException var6) {
            return textValue;
         }
      }
   }

   private Object evalDollarPrefixedExpression(String expr) {
      try {
         return this.evalDollarPrefixedExpressionInternal(expr);
      } catch (PathNotFoundException var3) {
         return "#notpresent";
      }
   }

   private Object evalDollarPrefixedExpressionInternal(String expr) {
      String withoutDollar = expr.substring(1);
      if (withoutDollar.isEmpty()) {
         return this.runtime.getVariable("response");
      } else if (!withoutDollar.startsWith("[") && !withoutDollar.startsWith(".")) {
         int spaceSlashIdx = withoutDollar.indexOf(" /");
         int slashIdx = withoutDollar.indexOf(47);
         int dotIdx = withoutDollar.indexOf(46);
         int bracketIdx = withoutDollar.indexOf(91);
         boolean isXPath = false;
         String varName;
         String path;
         if (spaceSlashIdx > 0) {
            varName = withoutDollar.substring(0, spaceSlashIdx);
            path = withoutDollar.substring(spaceSlashIdx + 1).trim();
            isXPath = true;
         } else if (slashIdx <= 0 || dotIdx >= 0 && slashIdx >= dotIdx || bracketIdx >= 0 && slashIdx >= bracketIdx) {
            if (dotIdx <= 0 || bracketIdx >= 0 && dotIdx >= bracketIdx) {
               if (bracketIdx <= 0) {
                  return this.runtime.getVariable(withoutDollar);
               }

               varName = withoutDollar.substring(0, bracketIdx);
               path = "$" + withoutDollar.substring(bracketIdx);
            } else {
               varName = withoutDollar.substring(0, dotIdx);
               path = "$" + withoutDollar.substring(dotIdx);
            }
         } else {
            varName = withoutDollar.substring(0, slashIdx);
            path = withoutDollar.substring(slashIdx);
            isXPath = true;
         }

         Object target = this.runtime.getVariable(varName);
         if (target == null) {
            return null;
         } else if (!isXPath) {
            return JsonPath.read(this.toJsonForJsonPath(target), path, new Predicate[0]);
         } else {
            return target instanceof Node
               ? KarateJsUtils.evalXmlPath((Node)target, path)
               : JsonPath.read(this.toJsonForJsonPath(target), "$" + path, new Predicate[0]);
         }
      } else {
         Object target = this.runtime.getVariable("response");
         String pathx = "$" + withoutDollar;
         return JsonPath.read(this.toJsonForJsonPath(target), pathx, new Predicate[0]);
      }
   }

   private Object toJsonForJsonPath(Object value) {
      if (value instanceof Node node) {
         return Xml.toObject(node);
      } else {
         if (value instanceof byte[] bytes) {
            value = new String(bytes, StandardCharsets.UTF_8);
         }

         if (value instanceof String s && StringUtils.looksLikeJson(s)) {
            try {
               return this.runtime.eval("(" + s + ")");
            } catch (Exception var4) {
               return value;
            }
         } else {
            return value;
         }
      }
   }

   private void validateVariableName(String name) {
      if (!StepUtils.isValidVariableName(name)) {
         throw new RuntimeException("invalid variable name: " + name);
      } else if ("karate".equals(name)) {
         throw new RuntimeException("'karate' is a reserved name");
      }
   }

   Object evalKarateExpression(String expr) {
      return this.evalKarateExpression(expr, null);
   }

   Object evalKarateExpression(String expr, Step docStringStep) {
      if (expr != null && !expr.isEmpty()) {
         expr = expr.trim();
         if (expr.startsWith("call ")) {
            return this.evalCallExpression(expr.substring(5).trim());
         } else if (expr.startsWith("callonce ")) {
            return this.evalCallOnceExpression(expr.substring(9).trim());
         } else if (expr.startsWith("$")) {
            return this.evalDollarPrefixedExpression(expr);
         } else if (expr.startsWith("get[") || expr.startsWith("get ")) {
            return this.evalGetExpression(expr);
         } else if (expr.startsWith("<")) {
            Object value = Xml.toXmlDoc(expr);
            return this.processEmbeddedExpressions(value);
         } else {
            if (expr.startsWith("/")) {
               Object response = this.runtime.getVariable("response");
               if (response instanceof Node) {
                  return KarateJsUtils.evalXmlPath((Node)response, expr);
               }
            }

            Object xpathResult = this.tryEvalXPathExpression(expr);
            if (xpathResult != null) {
               return xpathResult == XPATH_NOT_PRESENT ? null : xpathResult;
            } else {
               if (StringUtils.looksLikeJson(expr)) {
                  try {
                     Object value = Json.of(expr).value();
                     this.warnBareReferences(expr, value);
                     return this.processEmbeddedExpressions(value);
                  } catch (Exception var5) {
                  }
               }

               Object value = docStringStep == null
                  ? this.runtime.eval(wrapJsonLikeExpression(expr))
                  : this.runtime.evalDocString(wrapJsonLikeExpression(expr), docStringStep);
               if (value instanceof Map || value instanceof List) {
                  value = this.processEmbeddedExpressions(value, true, isParenWrappedJson(expr));
               }

               return value;
            }
         }
      } else {
         return null;
      }
   }

   private static boolean isParenWrappedJson(String expr) {
      return expr.length() >= 3 && expr.charAt(0) == '(' && expr.charAt(expr.length() - 1) == ')'
         ? StringUtils.looksLikeJson(expr.substring(1, expr.length() - 1).trim())
         : false;
   }

   private void executeAssert(Step step) {
      if (this.runtime.eval(step.getText()) instanceof Boolean b) {
         String message = "assert failed: " + step.getText();
         this.fireMatchEvent(null, step.getText(), null, b, null, b ? Result.PASS : Result.fail(message));
         if (!b) {
            throw new AssertionError(withCommentLabel(step, message));
         }
      } else {
         throw new RuntimeException("assert expression must return boolean: " + step.getText());
      }
   }

   private void warnBareReferences(String expr, Object value) {
      if (value instanceof Map<?, ?> map) {
         map.values().forEach(v -> this.warnBareReferences(expr, v));
      } else if (value instanceof List<?> list) {
         list.forEach(v -> this.warnBareReferences(expr, v));
      } else if (value instanceof String s && BARE_REFERENCE.matcher(s).matches()) {
         Pattern unquoted = Pattern.compile("[:\\[,]\\s*" + Pattern.quote(s) + "\\s*[,}\\]]");
         if (!unquoted.matcher(expr).find()) {
            return;
         }

         String head = s.split("[.\\[]", 2)[0];

         Object defined;
         try {
            defined = this.runtime.getVariable(head);
         } catch (RuntimeException var10) {
            return;
         }

         if (defined != null) {
            SCENARIO_LOG.warn(
               "unquoted '"
                  + s
                  + "' in a JSON literal is the string \""
                  + s
                  + "\", not the value of variable '"
                  + head
                  + "' - use '#("
                  + s
                  + ")', or ( … ) for JS"
            );
         }
      }
   }

   private void executePrint(Step step) {
      Object value = this.runtime.eval("[" + step.getText() + "]");
      if (value instanceof List<?> list) {
         StringBuilder sb = new StringBuilder();

         for (int i = 0; i < list.size(); i++) {
            if (i > 0) {
               sb.append(' ');
            }

            sb.append(StepUtils.stringify(list.get(i)));
         }

         SCENARIO_LOG.info(sb.toString());
      } else {
         SCENARIO_LOG.info(StepUtils.stringify(value));
      }
   }

   private HttpRequestBuilder http() {
      return this.runtime.getHttp();
   }

   private void applyCookiesFromMap(Map<?, ?> cookieMap) {
      for (Entry<?, ?> entry : cookieMap.entrySet()) {
         this.http().cookie(entry.getKey().toString(), entry.getValue().toString());
      }
   }

   private AuthHandler buildAuthHandler(KarateConfig config) {
      Map<String, Object> auth = config.getAuth();
      if (auth != null && !auth.isEmpty()) {
         String type = (String)auth.get("type");
         if (type == null) {
            return null;
         } else {
            return (AuthHandler)(switch (type) {
               case "basic" -> new BasicAuthHandler((String)auth.get("username"), (String)auth.get("password"));
               case "bearer" -> new BearerAuthHandler((String)auth.get("token"));
               case "oauth2" -> {
                  Map<String, Object> oauth2Config = new HashMap<>();
                  oauth2Config.put("url", auth.get("accessTokenUrl"));
                  oauth2Config.put("client_id", auth.get("clientId"));
                  oauth2Config.put("client_secret", auth.get("clientSecret"));
                  if (auth.containsKey("scope")) {
                     oauth2Config.put("scope", auth.get("scope"));
                  }

                  yield new ClientCredentialsAuthHandler(oauth2Config);
               }
               default -> null;
            });
         }
      } else {
         return null;
      }
   }

   private void executeUrl(Step step) {
      String urlExpr = step.getText();
      String url = (String)this.runtime.eval(urlExpr);
      this.http().url(url);
   }

   private void executePath(Step step) {
      String pathExpr = step.getText().trim();
      Object path;
      if (pathExpr.contains(",")) {
         path = this.runtime.eval("[" + pathExpr + "]");
      } else {
         path = this.runtime.eval(pathExpr);
      }

      if (path instanceof List) {
         for (Object item : (List)path) {
            this.http().path(item.toString());
         }
      } else {
         this.http().path(path.toString());
      }
   }

   private void executeParam(Step step) {
      String text = step.getText();
      int eqIndex = StepUtils.findAssignmentOperator(text);
      String name = text.substring(0, eqIndex).trim();
      Object value = this.runtime.eval(wrapJsonLikeExpression(text.substring(eqIndex + 1).trim()));
      if (value != null) {
         if (value instanceof List) {
            for (Object item : (List)value) {
               if (item != null) {
                  this.http().param(name, item.toString());
               }
            }
         } else {
            this.http().param(name, value.toString());
         }
      }
   }

   private void executeParams(Step step) {
      if (this.evalKarateExpression(step.getText()) instanceof Map<?, ?> map) {
         for (Entry<?, ?> entry : map.entrySet()) {
            String name = entry.getKey().toString();
            Object v = entry.getValue();
            if (v != null) {
               if (v instanceof List) {
                  for (Object item : (List)v) {
                     if (item != null) {
                        this.http().param(name, item.toString());
                     }
                  }
               } else {
                  this.http().param(name, v.toString());
               }
            }
         }
      }
   }

   private void executeHeader(Step step) {
      Table table = step.getTable();
      if (table != null) {
         for (Map<String, String> row : table.getRowsAsMaps()) {
            String name = row.get("name");
            String value = row.get("value");
            this.http().header(name, value);
         }
      } else {
         String text = step.getText();
         int eqIndex = StepUtils.findAssignmentOperator(text);
         String name = text.substring(0, eqIndex).trim();
         Object value = this.evalKarateExpression(text.substring(eqIndex + 1).trim());
         if (value instanceof List) {
            List<String> list = ((List)value).stream().map(Object::toString).toList();
            this.http().header(name, list);
         } else {
            this.http().header(name, value.toString());
         }
      }
   }

   private void executeHeaders(Step step) {
      if (this.evalKarateExpression(step.getText()) instanceof Map<?, ?> map) {
         this.http().headers((Map<String, Object>)map);
      }
   }

   private void executeCookie(Step step) {
      String text = step.getText();
      int eqIndex = StepUtils.findAssignmentOperator(text);
      String name = text.substring(0, eqIndex).trim();
      Object value = this.runtime.eval(wrapJsonLikeExpression(text.substring(eqIndex + 1).trim()));
      if (value instanceof Map<?, ?> rawMap) {
            Map<String, Object> map = (Map<String, Object>) rawMap;
         map.put("name", name);
         this.http().cookie(map);
      } else {
         this.http().cookie(name, value.toString());
      }
   }

   private void executeCookies(Step step) {
      if (this.evalKarateExpression(step.getText()) instanceof Map<?, ?> map) {
         for (Entry<?, ?> entry : map.entrySet()) {
            this.http().cookie(entry.getKey().toString(), entry.getValue().toString());
         }
      }
   }

   private void executeFormField(Step step) {
      String text = step.getText();
      int eqIndex = StepUtils.findAssignmentOperator(text);
      String name = text.substring(0, eqIndex).trim();
      Object value = this.runtime.eval(wrapJsonLikeExpression(text.substring(eqIndex + 1).trim()));
      this.http().formField(name, value.toString());
   }

   private void executeFormFields(Step step) {
      if (this.evalKarateExpression(step.getText()) instanceof Map<?, ?> map) {
         for (Entry<?, ?> entry : map.entrySet()) {
            this.http().formField(entry.getKey().toString(), entry.getValue().toString());
         }
      }
   }

   private void executeRequest(Step step) {
      String expr = step.getDocString() != null ? step.getDocString() : step.getText();
      Object body = this.evalKarateExpression(expr);
      this.http().body(body);
   }

   private void executeRetryUntil(Step step) {
      String condition = step.getText().trim();
      this.http().retryUntil(condition);
   }

   private void executeSoapAction(Step step) {
      String text = step.getText();
      String action = "";
      if (text != null && !text.isBlank()) {
         Object result = this.runtime.eval(text.trim());
         action = result != null ? result.toString() : "";
      }

      this.http().header("SOAPAction", action);
      this.http().contentType("text/xml");
      this.doMethod("POST");
   }

   private void executeMethod(Step step) {
      String text = step.getText().trim();
      String method = text.toUpperCase();
      if (!HTTP_METHODS.contains(method) && !this.isLiteralMethod(text)) {
         Object result = this.runtime.eval(text, step);
         if (result == null) {
            throw new RuntimeException("http method expression evaluated to null: " + text);
         }

         method = result.toString().trim().toUpperCase();
      }

      this.doMethod(method);
   }

   private boolean isLiteralMethod(String text) {
      for (int i = 0; i < text.length(); i++) {
         if (!Character.isLetter(text.charAt(i))) {
            return false;
         }
      }

      return !this.runtime.getAllVariables().containsKey(text);
   }

   private void doMethod(String method) {
      KarateConfig config = this.runtime.getConfig();
      this.http().method(method);
      Map<String, Map<String, Object>> cookieJar = this.runtime.getCookieJar();

      for (Entry<String, Map<String, Object>> entry : cookieJar.entrySet()) {
         this.http().cookie(entry.getValue());
      }

      Object configCookies = config.getCookies();
      if (configCookies instanceof JavaCallable cookiesFn) {
         if (cookiesFn.call(null) instanceof Map<?, ?> cookieMap) {
            this.applyCookiesFromMap(cookieMap);
         }
      } else if (configCookies instanceof Map<?, ?> cookieMap) {
         this.applyCookiesFromMap(this.resolveConfigMap(cookieMap));
      }

      Object configHeaders = config.getHeaders();
      if (configHeaders instanceof JavaCallable headersFn) {
         HttpRequest request = this.http().build();
         if (headersFn.call(null, request) instanceof Map<?, ?> headersMap) {
            this.http().headers((Map<String, Object>)headersMap);
         }
      } else if (configHeaders instanceof Map<?, ?> headersMap) {
         this.http().headers(this.resolveConfigMap(headersMap));
      }

      AuthHandler authHandler = this.buildAuthHandler(config);
      if (authHandler != null) {
         this.http().auth(authHandler);
      }

      String perfEventName = null;
      if (this.runtime.isPerfMode()) {
         HttpRequest builtRequest = this.http().build();
         PerfHook perfHook = this.runtime.getPerfHook();
         if (perfHook != null) {
            perfEventName = perfHook.getPerfEventName(builtRequest, this.runtime);
         }
      }

      String retryUntil = this.http().getRetryUntil();
      HttpResponse response;
      if (retryUntil != null) {
         response = this.executeMethodWithRetry(method, retryUntil, config);
      } else {
         HttpRequest request = this.http().build();
         Suite suite = this.getSuite();
         boolean shouldProceed = true;
         if (suite != null) {
            shouldProceed = suite.fireEvent(HttpRunEvent.enter(request, this.runtime));
         }

         if (shouldProceed) {
            response = this.http().invoke(method);
            response.setStrictParsing(this.runtime.getConfig().isStrictResponseParsing());
         } else {
            response = HttpResponse.skipped(request);
         }

         if (suite != null) {
            suite.fireEvent(HttpRunEvent.exit(response.getRequest(), response, this.runtime));
         }
      }

      if (response.getRequest() != null) {
         this.runtime.getKarate().setPrevRequest(response.getRequest());
      }

      this.runtime.getKarate().setPrevResponse(response);
      this.setResponseVariables(response);
      if (perfEventName != null) {
         PerfEvent perfEvent = new PerfEvent(response.getStartTime(), response.getStartTime() + response.getResponseTime(), perfEventName, response.getStatus());
         this.runtime.capturePerfEvent(perfEvent);
      }

      LogContext ctx = LogContext.get();
      if (response.getRequest() != null) {
         ctx.log("{} {}", method, response.getRequest().getUrlAndPath());
      }

      ctx.log("{} ({} ms)", response.getStatus(), response.getResponseTime());
   }

   private void setResponseVariables(HttpResponse response) {
      Object body = response.getBodyConverted();
      this.runtime.setVariable("response", body);
      this.runtime.setVariable("responseStatus", response.getStatus());
      this.runtime.setVariable("responseHeaders", response.getHeaders());
      this.runtime.setVariable("responseTime", response.getResponseTime());
      this.runtime.setHiddenVariable("responseBytes", response.getBodyBytes());
      this.runtime.setHiddenVariable("responseStatusText", response.getStatusText());
      Object responseCookies = response.getCookies();
      this.runtime.setHiddenVariable("responseCookies", responseCookies);
      this.runtime.setHiddenVariable("requestTimeStamp", response.getStartTime());
      this.runtime.updateCookieJar(responseCookies);
      String responseType;
      if (body instanceof byte[]) {
         responseType = "binary";
      } else if (body instanceof Map || body instanceof List) {
         responseType = "json";
      } else if (body instanceof Node) {
         responseType = "xml";
      } else {
         responseType = "string";
      }

      this.runtime.setHiddenVariable("responseType", responseType);
   }

   private HttpResponse executeMethodWithRetry(String method, String retryUntil, KarateConfig config) {
      int maxRetries = config.getRetryCount();
      int sleepInterval = config.getRetryInterval();
      int retryCount = 0;

      for (Suite suite = this.getSuite(); retryCount != maxRetries; retryCount++) {
         if (retryCount > 0) {
            try {
               logger.debug("sleeping {} ms before retry #{}", sleepInterval, retryCount);
               Thread.sleep(sleepInterval);
            } catch (InterruptedException var14) {
               Thread.currentThread().interrupt();
               throw new RuntimeException("retry interrupted", var14);
            }
         }

         HttpRequestBuilder httpCopy = this.http().copy();
         HttpRequest request = this.http().build();
         boolean shouldProceed = true;
         if (suite != null) {
            shouldProceed = suite.fireEvent(HttpRunEvent.enter(request, this.runtime));
         }

         HttpResponse response;
         if (shouldProceed) {
            response = this.http().invoke(method);
            response.setStrictParsing(config.isStrictResponseParsing());
         } else {
            response = HttpResponse.skipped(request);
         }

         if (suite != null) {
            suite.fireEvent(HttpRunEvent.exit(response.getRequest(), response, this.runtime));
         }

         this.setResponseVariables(response);

         boolean conditionMet;
         try {
            Object result = this.runtime.eval(retryUntil);
            conditionMet = Boolean.TRUE.equals(result);
         } catch (Exception var15) {
            logger.warn("retry condition evaluation failed: {}", var15.getMessage());
            conditionMet = false;
         }

         if (conditionMet) {
            if (retryCount > 0) {
               logger.debug("retry condition satisfied after {} attempts", retryCount + 1);
            }

            return response;
         }

         logger.debug("retry condition not satisfied: {}", retryUntil);
         this.http().restoreFrom(httpCopy);
      }

      throw new RuntimeException("retry failed after " + maxRetries + " attempts: " + retryUntil);
   }

   private void executeStatus(Step step) {
      int expected = Integer.parseInt(step.getText().trim());
      Object statusObj = this.runtime.getVariable("responseStatus");
      int actual = statusObj instanceof Number n ? n.intValue() : Integer.parseInt(statusObj.toString());
      if (actual != expected) {
         throw new AssertionError("expected status: " + expected + ", actual: " + actual);
      }
   }

   private void executeMultipartFile(Step step) {
      String text = step.getText();
      int eqIndex = StepUtils.findAssignmentOperator(text);
      if (eqIndex < 0) {
         throw new RuntimeException("multipart file requires '=' assignment: " + text);
      } else {
         String name = text.substring(0, eqIndex).trim();
         String expr = text.substring(eqIndex + 1).trim();
         Object value = this.evalKarateExpression(expr);
         Map<String, Object> multipartMap = new HashMap<>();
         multipartMap.put("name", name);
         if (value instanceof Map<?, ?> fileMap) {
            Object readPath = fileMap.get("read");
            if (readPath != null) {
               Resource resource = this.resolveResource(readPath.toString());
               File file = this.getFileFromResource(resource);
               if (file != null) {
                  multipartMap.put("value", file);
               } else {
                  try (InputStream is = resource.getStream()) {
                     byte[] bytes = is.readAllBytes();
                     multipartMap.put("value", bytes);
                  } catch (Exception var17) {
                     throw new RuntimeException("failed to read file: " + readPath, var17);
                  }
               }
            } else if (fileMap.get("value") != null) {
               multipartMap.put("value", fileMap.get("value"));
            }

            if (fileMap.get("filename") != null) {
               multipartMap.put("filename", fileMap.get("filename"));
            }

            if (fileMap.get("contentType") != null) {
               multipartMap.put("contentType", fileMap.get("contentType"));
            }

            if (fileMap.get("charset") != null) {
               multipartMap.put("charset", fileMap.get("charset"));
            }

            if (fileMap.get("transferEncoding") != null) {
               multipartMap.put("transferEncoding", fileMap.get("transferEncoding"));
            }
         } else if (value instanceof String) {
            multipartMap.put("value", value);
         } else if (value instanceof byte[]) {
            multipartMap.put("value", value);
         } else {
            multipartMap.put("value", value);
         }

         this.http().multiPart(multipartMap);
      }
   }

   private void executeMultipartField(Step step) {
      String text = step.getText();
      int eqIndex = StepUtils.findAssignmentOperator(text);
      if (eqIndex < 0) {
         throw new RuntimeException("multipart field requires '=' assignment: " + text);
      } else {
         String name = text.substring(0, eqIndex).trim();
         String expr = text.substring(eqIndex + 1).trim();
         Object value = this.runtime.eval(wrapJsonLikeExpression(expr));
         Map<String, Object> multipartMap = new HashMap<>();
         multipartMap.put("name", name);
         multipartMap.put("value", value);
         this.http().multiPart(multipartMap);
      }
   }

   private void executeMultipartFields(Step step) {
      if (this.evalKarateExpression(step.getText()) instanceof Map<?, ?> rawFields) {
         Map<String, Object> fields = (Map<String, Object>) rawFields;
         for (Entry<String, Object> entry : fields.entrySet()) {
            Map<String, Object> multipartMap = new HashMap<>();
            multipartMap.put("name", entry.getKey());
            Object fieldValue = entry.getValue();
            if (fieldValue instanceof Map) {
               multipartMap.putAll((Map<? extends String, ? extends Object>)fieldValue);
            } else {
               multipartMap.put("value", fieldValue);
            }

            this.http().multiPart(multipartMap);
         }
      } else {
         throw new RuntimeException("multipart fields expects a map: " + step.getText());
      }
   }

   private void executeMultipartFiles(Step step) {
      Object value = this.evalKarateExpression(step.getText());
      if (value instanceof List) {
         for (Object item : (List)value) {
            this.processMultipartFileEntry(item, null);
         }
      } else {
         if (!(value instanceof Map<?, ?> rawFilesMap)) {
            throw new RuntimeException("multipart files expects a list or map: " + step.getText());
         }

         for (Entry<String, Object> entry : ((Map<String, Object>) rawFilesMap).entrySet()) {
            this.processMultipartFileEntry(entry.getValue(), entry.getKey());
         }
      }
   }

   private void processMultipartFileEntry(Object item, String defaultName) {
      if (item instanceof Map<?, ?> fileMap) {
         Map<String, Object> multipartMap = new HashMap<>();
         String name = defaultName != null ? defaultName : (String)fileMap.get("name");
         if (name == null) {
            throw new RuntimeException("multipart files entry requires 'name': " + item);
         } else {
            multipartMap.put("name", name);
            Object readPath = fileMap.get("read");
            if (readPath != null) {
               Resource resource = this.resolveResource(readPath.toString());
               File file = this.getFileFromResource(resource);
               if (file != null) {
                  multipartMap.put("value", file);
               } else {
                  try (InputStream is = resource.getStream()) {
                     byte[] bytes = is.readAllBytes();
                     multipartMap.put("value", bytes);
                  } catch (Exception var14) {
                     throw new RuntimeException("failed to read file: " + readPath, var14);
                  }
               }
            } else if (fileMap.get("value") != null) {
               multipartMap.put("value", fileMap.get("value"));
            }

            if (fileMap.get("filename") != null) {
               multipartMap.put("filename", fileMap.get("filename"));
            }

            if (fileMap.get("contentType") != null) {
               multipartMap.put("contentType", fileMap.get("contentType"));
            }

            this.http().multiPart(multipartMap);
         }
      } else {
         throw new RuntimeException("multipart files entry must be a map: " + item);
      }
   }

   private void executeMultipartEntity(Step step) {
      String expr = step.getDocString() != null ? step.getDocString() : step.getText();
      Object value = this.evalKarateExpression(expr);
      if (value instanceof Map) {
         this.http().multiPart((Map<String, Object>)value);
      } else {
         Map<String, Object> multipartMap = new HashMap<>();
         multipartMap.put("name", "file");
         multipartMap.put("value", value);
         this.http().multiPart(multipartMap);
      }
   }

   private Resource resolveResource(String path) {
      FeatureRuntime fr = this.runtime.getFeatureRuntime();
      return fr != null ? fr.resolve(path) : Resource.path(path);
   }

   private File getFileFromResource(Resource resource) {
      if (resource.isLocalFile()) {
         Path path = resource.getPath();
         if (path != null && Files.exists(path)) {
            return path.toFile();
         }
      }

      return null;
   }

   private static String appendDocStringCallArg(String callExpr, String docString) {
      return docString != null && !docString.isBlank() && StepUtils.splitCallExpr(callExpr).arg() == null ? callExpr + " " + docString.trim() : callExpr;
   }

   private void executeCall(Step step) {
      String text = appendDocStringCallArg(step.getText().trim(), step.getDocString());
      StepUtils.CallParts parts = StepUtils.splitCallExpr(text);
      String firstToken = parts.callable();
      if (!StepUtils.isLiteralFeatureRead(text)) {
         Object evaluated = null;

         try {
            evaluated = this.runtime.eval(firstToken);
         } catch (Exception var12) {
            logger.debug("call target probe failed, treating as feature path: {}", var12.getMessage());
         }

         if (evaluated instanceof JavaCallable fn) {
            Object arg = parts.arg() != null ? this.evalKarateExpression(parts.arg()) : null;
            if ((arg != null ? fn.call(null, arg) : fn.call(null)) instanceof Map<?, ?> rawMap) {
               Map<String, Object> map = (Map<String, Object>) rawMap;
               for (Entry<String, Object> entry : map.entrySet()) {
                  this.runtime.setVariable(entry.getKey(), entry.getValue());
               }
            }

            return;
         }

         if (evaluated instanceof FeatureCall || evaluated instanceof Feature) {
            this.executeFeatureCall(evaluated, parts.arg(), null);
            return;
         }
      }

      StepExecutor.CallExpression call = this.parseCallExpression(text);
      FeatureRuntime fr = this.runtime.getFeatureRuntime();
      Feature calledFeature;
      if (call.sameFile) {
         calledFeature = fr != null ? fr.getFeature() : null;
         if (calledFeature == null) {
            throw new RuntimeException("call with tag selector requires a feature context");
         }
      } else {
         Resource calledResource = fr != null ? fr.resolve(call.path) : Resource.path(call.path);
         calledFeature = Feature.read(calledResource);
      }

      if (call.argList != null) {
         List<Map<String, Object>> results = this.callFeatureLoop(calledFeature, call.argList, call.tagSelector, call.lineFilters);
         if (call.resultVar != null) {
            this.runtime.setVariable(call.resultVar, results);
         }
      } else {
         boolean sharedScope = call.resultVar == null;
         FeatureRuntime nestedFr = this.runNestedFeature(calledFeature, call.arg, sharedScope, call.tagSelector, call.lineFilters, -1);
         this.propagateFromCallee(nestedFr.getLastExecuted(), call.resultVar);
      }
   }

   private void executeFeatureCall(Object featureObj, String argExpr, String resultVar) {
      FeatureRuntime fr = this.runtime.getFeatureRuntime();
      String tagSelector = null;
      Feature calledFeature;
      if (featureObj instanceof FeatureCall fc) {
         if (fc.isSameFile()) {
            calledFeature = fr != null ? fr.getFeature() : null;
            if (calledFeature == null) {
               throw new RuntimeException("same-file tag call requires a feature context");
            }
         } else {
            calledFeature = fc.getFeature();
         }

         tagSelector = fc.getTagSelector();
      } else if (featureObj instanceof Feature feature) {
         calledFeature = feature;
      } else {
         throw new RuntimeException("expected Feature or FeatureCall, got: " + featureObj.getClass());
      }

      Object argObj = argExpr == null ? null : this.evalKarateExpression(argExpr);
      if (argObj instanceof List) {
         List<Map<String, Object>> results = this.callFeatureLoop(calledFeature, (List<?>)argObj, tagSelector, null);
         if (resultVar != null) {
            this.runtime.setVariable(resultVar, results);
         }
      } else {
         Map<String, Object> callArg = null;
         if (argObj instanceof Map) {
            callArg = (Map<String, Object>)argObj;
         }

         boolean sharedScope = resultVar == null;
         FeatureRuntime nestedFr = this.runNestedFeature(calledFeature, callArg, sharedScope, tagSelector, null, -1);
         this.propagateFromCallee(nestedFr.getLastExecuted(), resultVar);
      }
   }

   private void executeCallOnce(Step step) {
      String text = appendDocStringCallArg(step.getText().trim(), step.getDocString());
      String cacheKey = text;
      FeatureRuntime fr = this.runtime.getFeatureRuntime();
      if (fr == null) {
         this.executeCall(step);
      } else {
         Map<String, Object> cache = fr.getCallOnceCache();
         ReentrantLock lock = fr.getCallOnceLock();
         StepExecutor.CallOnceResult cached = (StepExecutor.CallOnceResult)cache.get(text);
         if (cached != null) {
            this.applyCachedCallOnceResult(cached);
         } else {
            lock.lock();

            try {
               StepExecutor.CallOnceResult rechecked = (StepExecutor.CallOnceResult)cache.get(cacheKey);
               if (rechecked == null) {
                  Map<String, Object> preCallVars = this.runtime.getAllVariables();
                  KarateConfig preCallConfig = this.runtime.getConfig().copy();
                  this.executeCall(step);
                  Map<String, Object> delta = StepUtils.calleeDelta(preCallVars, this.runtime.getAllVariables());
                  Map<String, Object> vars = (Map<String, Object>)StepUtils.deepCopy(delta);
                  Map<String, Map<String, Object>> cookieJarCopy = (Map<String, Map<String, Object>>)StepUtils.deepCopy(this.runtime.getCookieJar());
                  cache.put(cacheKey, new StepExecutor.CallOnceResult(vars, preCallConfig, this.runtime.getConfig().copy(), cookieJarCopy));
                  return;
               }

               this.applyCachedCallOnceResult(rechecked);
            } finally {
               lock.unlock();
            }
         }
      }
   }

   private void applyCachedCallOnceResult(StepExecutor.CallOnceResult cached) {
      Map<String, Object> copies = (Map<String, Object>)StepUtils.deepCopy(cached.vars());

      for (Entry<String, Object> entry : copies.entrySet()) {
         this.runtime.setVariable(entry.getKey(), entry.getValue());
      }

      this.runtime.getConfig().copyChangedFrom(cached.configBefore(), cached.configAfter());
      this.runtime.getKarate().client.apply(this.runtime.getConfig());
      if (cached.cookieJar() != null) {
         Map<String, Map<String, Object>> cookieJarCopy = (Map<String, Map<String, Object>>)StepUtils.deepCopy(cached.cookieJar());
         this.runtime.getCookieJar().putAll(cookieJarCopy);
      }
   }

   private StepExecutor.CallExpression parseCallExpression(String text) {
      StepExecutor.CallExpression expr = new StepExecutor.CallExpression();
      if (text.startsWith("read(")) {
         int closeParen = StepUtils.findReadCloseParen(text);
         if (closeParen > 0) {
            String readArg = text.substring(5, closeParen).trim();
            String rawPath;
            if (StepUtils.isSingleQuotedLiteral(readArg)) {
               rawPath = readArg.substring(1, readArg.length() - 1);
            } else {
               Object pathObj = this.runtime.eval(readArg);
               rawPath = pathObj != null ? pathObj.toString() : readArg;
            }

            this.parsePathAndTag(rawPath, expr);
            Object argObj = this.evalKarateExpression(text.substring(closeParen + 1).trim());
            if (argObj instanceof List) {
               expr.argList = (List<?>)argObj;
            } else if (argObj instanceof Map<?, ?> argMap) {
               expr.arg = (Map<String, Object>) argMap;
            }
         }
      } else {
         Object result = this.runtime.eval(text);
         if (!(result instanceof String)) {
            throw new RuntimeException("call expression must resolve to a feature path: " + text);
         }

         this.parsePathAndTag((String)result, expr);
      }

      return expr;
   }

   private void parsePathAndTag(String rawPath, StepExecutor.CallExpression expr) {
      StepUtils.ParsedFeaturePath parsed = StepUtils.parseFeaturePath(rawPath);
      expr.path = parsed.path();
      expr.tagSelector = parsed.tagSelector();
      expr.sameFile = parsed.sameFile();
      expr.lineFilters = parsed.lineFilters();
   }

   private void executeEval(Step step) {
      String expr = step.getDocString();
      if (expr != null) {
         this.runtime.evalDocString(expr, step);
      } else {
         expr = step.getText();
         this.runtime.eval(expr, step);
      }
   }

   private void executeDoc(Step step) {
      String text = step.getText();
      Object value = this.runtime.eval(wrapJsonLikeExpression(text));
      String html;
      if (value instanceof String path) {
         html = this.runtime.getKarate().doc(Map.of("read", path));
      } else {
         if (!(value instanceof Map)) {
            throw new RuntimeException("doc requires a string path or map with 'read' key, got: " + value);
         }

         html = this.runtime.getKarate().doc((Map<String, Object>)value);
      }

      if (html != null && !html.isEmpty()) {
         LogContext.get().embed(html.getBytes(StandardCharsets.UTF_8), "text/html");
      }
   }

   private void executeConfigure(Step step) {
      String text = step.getText();
      int eqIndex = StepUtils.findAssignmentOperator(text);
      if (eqIndex < 0) {
         throw new RuntimeException("configure requires '=' assignment: " + text);
      } else {
         String key = text.substring(0, eqIndex).trim();
         String expr = text.substring(eqIndex + 1).trim();
         if (expr.isEmpty() && step.getDocString() != null) {
            expr = step.getDocString();
         }

         if (key.equals("javaBridgeEnabled")) {
            Object enabled = this.evalKarateExpression(expr);
            this.runtime.getKarate().setJavaBridgeEnabled(Boolean.parseBoolean(String.valueOf(enabled)));
         } else if (key.equals("requestExpressionsEnabled")) {
            Object enabled = this.evalKarateExpression(expr);
            this.runtime.setRequestExpressionsEnabled(Boolean.parseBoolean(String.valueOf(enabled)));
         } else {
            Object value;
            if (!key.equals("headers") && !key.equals("cookies")) {
               value = this.evalKarateExpression(expr);
            } else {
               value = this.evalConfigReferenceExpression(expr);
            }

            this.runtime.configure(key, value);
         }
      }
   }

   private Object evalConfigReferenceExpression(String expr) {
      if (expr != null && !expr.isEmpty()) {
         expr = expr.trim();
         return StringUtils.looksLikeJson(expr) ? this.evalKarateExpression(expr) : this.runtime.eval(wrapJsonLikeExpression(expr));
      } else {
         return null;
      }
   }

   private void executeDriver(Step step) {
      String text = step.getText();
      if (text != null && !text.trim().isEmpty()) {
         Object result = this.runtime.eval(text.trim());
         if (result == null) {
            throw new RuntimeException("driver URL evaluated to null: " + text);
         } else {
            String url = result.toString();
            Driver driver = this.runtime.getDriver();
            driver.setUrl(url);
         }
      } else {
         throw new RuntimeException("driver keyword requires a URL argument");
      }
   }

   private Object getResponseHeader(String headerName) {
      if (this.runtime.getVariable("responseHeaders") instanceof Map<?, ?> headers) {
         Object headerValue = StringUtils.getIgnoreKeyCase((Map<String, Object>) headers, headerName);
         if (headerValue instanceof List<?> list) {
            return list.isEmpty() ? null : list.get(0);
         } else {
            return headerValue;
         }
      } else {
         return null;
      }
   }

   private Object evalCallExpression(String callExpr) {
      String tempVar = "__callResult__" + System.nanoTime();
      this.executeCallWithResult(callExpr, tempVar);
      return this.runtime.getVariable(tempVar);
   }

   private Object evalCallOnceExpression(String callExpr) {
      String tempVar = "__callOnceResult__" + System.nanoTime();
      this.executeCallOnceWithResult(callExpr, tempVar);
      return this.runtime.getVariable(tempVar);
   }

   private Object evalGetExpression(String expr) {
      int index = -1;
      String remainder;
      if (expr.startsWith("get[")) {
         int closeBracket = expr.indexOf(93);
         if (closeBracket < 0) {
            throw new RuntimeException("Invalid get expression, missing ]: " + expr);
         }

         index = Integer.parseInt(expr.substring(4, closeBracket));
         remainder = expr.substring(closeBracket + 1).trim();
      } else {
         remainder = expr.substring(4).trim();
      }

      String path;
      String varName;
      if (remainder.startsWith("$")) {
         varName = "response";
         path = remainder;
      } else {
         int spaceIdx = remainder.indexOf(32);
         int dotIdx = remainder.indexOf(46);
         int bracketIdx = remainder.indexOf(91);
         if (spaceIdx <= 0 || dotIdx >= 0 && spaceIdx >= dotIdx || bracketIdx >= 0 && spaceIdx >= bracketIdx) {
            StepUtils.VarAndPath vp = StepUtils.splitVarAndJsonPath(remainder);
            varName = vp.var;
            path = vp.path;
         } else {
            varName = remainder.substring(0, spaceIdx);
            path = remainder.substring(spaceIdx).trim();
         }
      }

      Object target = this.runtime.getVariable(varName);
      if (target == null) {
         return null;
      } else if (target instanceof Node && path != null && path.startsWith("/")) {
         Object result = KarateJsUtils.evalXmlPath((Node)target, path);
         return applyGetIndex(result, index);
      } else {
         String jsonPath;
         if (path == null) {
            jsonPath = "$";
         } else if (path.startsWith("$")) {
            jsonPath = path;
         } else {
            jsonPath = "$" + path;
         }

         Object result;
         try {
            result = JsonPath.read(this.toJsonForJsonPath(target), jsonPath, new Predicate[0]);
         } catch (PathNotFoundException var10) {
            return "#notpresent";
         }

         return applyGetIndex(result, index);
      }
   }

   private static Object applyGetIndex(Object result, int index) {
      if (index >= 0 && result instanceof List<?> list) {
         if (list.isEmpty()) {
            return list;
         } else {
            return index < list.size() ? list.get(index) : null;
         }
      } else {
         return result;
      }
   }

   static String wrapJsonLikeExpression(String expr) {
      if (expr != null && !expr.isEmpty()) {
         char c = expr.charAt(0);
         if (c == '{' || c == '[') {
            return "(" + expr + ")";
         }
      }

      return expr;
   }

   private Map<String, Object> resolveConfigMap(Map<?, ?> configMap) {
      return (Map<String, Object>)this.processEmbeddedExpressions(configMap, true);
   }

   private Object processEmbeddedExpressions(Object value) {
      return this.processEmbeddedExpressions(value, false, false);
   }

   private Object processEmbeddedExpressions(Object value, boolean lenient) {
      return this.processEmbeddedExpressions(value, lenient, false);
   }

   private Object processEmbeddedExpressions(Object value, boolean lenient, boolean forMatch) {
      return this.processEmbeddedExpressions(value, lenient, forMatch, null);
   }

   private Object processEmbeddedExpressions(Object value, boolean lenient, boolean forMatch, IdentityHashMap<Object, Object> ancestors) {
      if (!this.runtime.isRequestExpressionsEnabled() && this.runtime.isRequestDerived(value)) {
         return value;
      } else if (value instanceof Node) {
         this.processXmlEmbeddedExpressions((Node)value);
         return value;
      } else if (value instanceof JavaCallable) {
         return value;
      } else if (!(value instanceof Map) && !(value instanceof List)) {
         return value instanceof String str ? this.processEmbeddedString(str, lenient, forMatch) : value;
      } else {
         if (ancestors == null) {
            ancestors = new IdentityHashMap<>();
         } else if (ancestors.containsKey(value)) {
            return ancestors.get(value);
         }

         Object result;
         if (value instanceof Map<?, ?> rawMap) {
            Map<String, Object> map = (Map<String, Object>) rawMap;
            Map<String, Object> copy = new LinkedHashMap<>();
            ancestors.put(value, copy);

            for (Entry<String, Object> entry : map.entrySet()) {
               Object processed = this.processEmbeddedExpressions(entry.getValue(), lenient, forMatch, ancestors);
               if (processed != REMOVE_MARKER) {
                  copy.put(entry.getKey(), processed);
               }
            }

            result = copy;
         } else {
            List<Object> list = (List<Object>)value;
            List<Object> copy = new ArrayList<>();
            ancestors.put(value, copy);

            for (Object item : list) {
               Object processed = this.processEmbeddedExpressions(item, lenient, forMatch, ancestors);
               if (processed != REMOVE_MARKER) {
                  copy.add(processed);
               }
            }

            result = copy;
         }

         ancestors.remove(value);
         return result;
      }
   }

   private Object processEmbeddedString(String str, boolean lenient, boolean forMatch) {
      if (lenient && str.startsWith("#") && !this.runtime.embeddedTrusted(str)) {
         return str;
      } else if (str.startsWith("##(") && str.endsWith(")")) {
         String expr = str.substring(3, str.length() - 1);

         try {
            Object result = this.runtime.eval(expr);
            if (result == null) {
               return REMOVE_MARKER;
            } else {
               return !forMatch || !(result instanceof Map) && !(result instanceof List) && !(result instanceof Node) ? result : str;
            }
         } catch (Exception var8) {
            if (lenient) {
               return str;
            } else if (var8 instanceof EngineException ee && "ReferenceError".equals(ee.getJsErrorName())) {
               return REMOVE_MARKER;
            } else {
               throw var8;
            }
         }
      } else if (str.startsWith("#(") && str.endsWith(")")) {
         String expr = str.substring(2, str.length() - 1);

         try {
            return this.runtime.eval(expr);
         } catch (Exception var7) {
            this.runtime.recordFailedEmbedded(str, var7);
            return str;
         }
      } else if (str.startsWith("#")) {
         return str;
      } else {
         return str.contains("#(") ? this.processInlineEmbedded(str, lenient) : str;
      }
   }

   private Object processInlineEmbedded(String str) {
      return this.processInlineEmbedded(str, false);
   }

   private Object processInlineEmbedded(String str, boolean lenient) {
      StringBuilder result = new StringBuilder();
      int i = 0;

      while (i < str.length()) {
         int hashPos = str.indexOf(35, i);
         if (hashPos < 0) {
            result.append(str.substring(i));
            break;
         }

         result.append(str.substring(i, hashPos));
         boolean optional = false;
         int exprStart;
         if (hashPos + 1 < str.length() && str.charAt(hashPos + 1) == '#') {
            if (hashPos + 2 >= str.length() || str.charAt(hashPos + 2) != '(') {
               result.append("##");
               i = hashPos + 2;
               continue;
            }

            optional = true;
            exprStart = hashPos + 3;
         } else {
            if (hashPos + 1 >= str.length() || str.charAt(hashPos + 1) != '(') {
               result.append('#');
               i = hashPos + 1;
               continue;
            }

            exprStart = hashPos + 2;
         }

         int depth = 1;

         int j;
         for (j = exprStart; j < str.length() && depth > 0; j++) {
            char c = str.charAt(j);
            if (c == '(') {
               depth++;
            } else if (c == ')') {
               depth--;
            }
         }

         if (depth != 0) {
            result.append(optional ? "##(" : "#(");
            i = exprStart;
         } else {
            String expr = str.substring(exprStart, j - 1);
            if (lenient && !this.runtime.embeddedTrusted(str.substring(hashPos, j))) {
               result.append(str, hashPos, j);
               i = j;
            } else {
               Object value;
               try {
                  value = this.runtime.eval(expr);
               } catch (Exception var13) {
                  if (lenient) {
                     result.append(str, hashPos, j);
                     i = j;
                     continue;
                  }

                  throw var13;
               }

               if ((!optional || value != null) && value != null) {
                  result.append(StepUtils.stringify(value));
               }

               i = j;
            }
         }
      }

      return result.toString();
   }

   private void processXmlEmbeddedExpressions(Node node) {
      if (node.getNodeType() == 9) {
         node = node.getFirstChild();
      }

      if (node != null) {
         NamedNodeMap attribs = node.getAttributes();
         if (attribs != null) {
            List<Attr> toRemove = new ArrayList<>();

            for (int i = 0; i < attribs.getLength(); i++) {
               Attr attrib = (Attr)attribs.item(i);
               String value = attrib.getValue();
               if (value != null && value.contains("#(")) {
                  boolean optional = value.startsWith("##(");
                  if (!value.startsWith("#(") && !optional) {
                     attrib.setValue(this.processInlineEmbedded(value).toString());
                  } else {
                     String expr = value.substring(optional ? 3 : 2, value.length() - 1);

                     try {
                        Object result = this.runtime.eval(expr);
                        if (optional && result == null) {
                           toRemove.add(attrib);
                        } else {
                           attrib.setValue(result == null ? "" : StepUtils.stringify(result));
                        }
                     } catch (Exception var15) {
                        this.runtime.recordFailedEmbedded(value, var15);
                     }
                  }
               }
            }

            for (Attr attr : toRemove) {
               attribs.removeNamedItem(attr.getName());
            }
         }

         NodeList children = node.getChildNodes();
         List<Node> childList = new ArrayList<>();

         for (int ix = 0; ix < children.getLength(); ix++) {
            childList.add(children.item(ix));
         }

         List<Node> elementsToRemove = new ArrayList<>();

         for (Node child : childList) {
            String value = child.getNodeValue();
            if (value != null) {
               value = value.trim();
               if (value.startsWith("#(") || value.startsWith("##(")) {
                  boolean optional = value.startsWith("##(");
                  String expr = value.substring(optional ? 3 : 2, value.length() - 1);

                  try {
                     Object result = this.runtime.eval(expr);
                     if (optional && result == null) {
                        elementsToRemove.add(child);
                     } else if (result instanceof Node evalNode && child.getNodeType() != 4) {
                        if (evalNode.getNodeType() == 9) {
                           evalNode = evalNode.getFirstChild();
                        }

                        Node var31 = node.getOwnerDocument().importNode(evalNode, true);
                        child.getParentNode().replaceChild(var31, child);
                     } else {
                        String strResult = result instanceof Node ? Xml.toString((Node)result, false) : (result == null ? "" : StepUtils.stringify(result));
                        child.setNodeValue(strResult);
                     }
                  } catch (Exception var14) {
                     this.runtime.recordFailedEmbedded(value, var14);
                  }
               } else if (value.contains("#(")) {
                  child.setNodeValue(this.processInlineEmbedded(value).toString());
               }
            } else if (child.hasChildNodes() || child.hasAttributes()) {
               this.processXmlEmbeddedExpressions(child);
            }
         }

         for (Node toRemove : elementsToRemove) {
            Node parent = toRemove.getParentNode();
            if (parent != null) {
               Node grandParent = parent.getParentNode();
               if (grandParent != null) {
                  grandParent.removeChild(parent);
               }
            }
         }
      }
   }

   private static class CallExpression {
      String path;
      Map<String, Object> arg;
      List<?> argList;
      String resultVar;
      String tagSelector;
      boolean sameFile;
      Set<Integer> lineFilters;
   }

   private record CallOnceResult(Map<String, Object> vars, KarateConfig configBefore, KarateConfig configAfter, Map<String, Map<String, Object>> cookieJar) {
   }
}
