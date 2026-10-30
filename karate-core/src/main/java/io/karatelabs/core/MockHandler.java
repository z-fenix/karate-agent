package io.karatelabs.core;

import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.Predicate;
import io.karatelabs.common.Resource;
import io.karatelabs.common.StringUtils;
import io.karatelabs.common.Xml;
import io.karatelabs.gherkin.Feature;
import io.karatelabs.gherkin.FeatureSection;
import io.karatelabs.gherkin.Scenario;
import io.karatelabs.gherkin.Step;
import io.karatelabs.http.HttpRequest;
import io.karatelabs.http.HttpResponse;
import io.karatelabs.js.Engine;
import io.karatelabs.js.JavaCallable;
import io.karatelabs.js.JavaInvokable;
import io.karatelabs.js.JsLazy;
import io.karatelabs.output.LogContext;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;
import org.slf4j.Logger;
import org.w3c.dom.Node;

public class MockHandler implements Function<HttpRequest, HttpResponse> {
   private static final Logger logger = LogContext.MOCK_LOGGER;
   private static final String ALLOWED_METHODS = "GET, HEAD, POST, PUT, DELETE, PATCH, OPTIONS";
   public static final String KARATE_MOCK_HEADER = "Karate-Mock";
   private HttpRequest currentRequest;
   private boolean proceeded;
   private final List<Feature> features = new ArrayList<>();
   private final Map<String, Object> globals = new LinkedHashMap<>();
   private final MockConfig config = new MockConfig();
   private final ReentrantLock requestLock = new ReentrantLock();
   private final String pathPrefix;
   private final boolean javaBridgeEnabled;
   private final boolean requestExpressionsEnabled;
   private final Map<Feature, ScenarioRuntime> runtimes = new LinkedHashMap<>();

   public HttpRequest getCurrentRequest() {
      return this.currentRequest;
   }

   public MockHandler(String featurePath) {
      this(Feature.read(featurePath), null);
   }

   public MockHandler(Feature feature) {
      this(feature, null);
   }

   public MockHandler(Feature feature, Map<String, Object> args) {
      this(List.of(feature), args, null);
   }

   public MockHandler(List<Feature> features, Map<String, Object> args, String pathPrefix) {
      this(features, args, pathPrefix, false, false);
   }

   public MockHandler(List<Feature> features, Map<String, Object> args, String pathPrefix, boolean javaBridgeEnabled, boolean requestExpressionsEnabled) {
      this.pathPrefix = pathPrefix;
      this.javaBridgeEnabled = javaBridgeEnabled;
      this.requestExpressionsEnabled = requestExpressionsEnabled;

      for (Feature feature : features) {
         this.features.add(feature);
         ScenarioRuntime runtime = this.initRuntime(feature, args);
         this.runtimes.put(feature, runtime);
      }

      logger.info("mock handler initialized with {} feature(s), cors: {}", features.size(), this.config.isCorsEnabled());
   }

   void releaseHttpClients() {
      for (ScenarioRuntime runtime : this.runtimes.values()) {
         runtime.releaseHttpClient();
      }
   }

   private ScenarioRuntime initRuntime(Feature feature, Map<String, Object> args) {
      FeatureRuntime featureRuntime = new FeatureRuntime(null, feature);
      Scenario scenario = this.getFirstScenario(feature);
      ScenarioRuntime runtime = new ScenarioRuntime(featureRuntime, scenario);
      runtime.getKarate().setMockHandler(this);
      runtime.getKarate().setJavaBridgeEnabled(this.javaBridgeEnabled);
      runtime.setRequestExpressionsEnabled(this.requestExpressionsEnabled);
      Engine engine = runtime.getEngine();
      engine.put("pathMatches", (JavaInvokable)a -> {
         if (this.currentRequest == null) {
            return false;
         } else {
            boolean matched = this.currentRequest.pathMatches(a[0] + "");
            if (matched) {
               engine.put("pathParams", markRequestDerived(runtime, this.currentRequest.getPathParams()));
            }

            return matched;
         }
      });
      engine.put("methodIs", (JavaInvokable)a -> this.currentRequest != null && (a[0] + "").equalsIgnoreCase(this.currentRequest.getMethod()));
      engine.put("typeContains", (JavaInvokable)a -> {
         if (this.currentRequest == null) {
            return false;
         } else {
            String contentType = this.currentRequest.getContentType();
            return contentType != null && contentType.contains(a[0] + "");
         }
      });
      engine.put("acceptContains", (JavaInvokable)a -> {
         if (this.currentRequest == null) {
            return false;
         } else {
            String accept = this.currentRequest.getHeader("Accept");
            return accept != null && accept.contains(a[0] + "");
         }
      });
      engine.put("headerContains", (JavaInvokable)a -> {
         if (this.currentRequest == null) {
            return false;
         } else {
            List<String> values = this.currentRequest.getHeaderValues(a[0] + "");
            if (values != null) {
               String search = a[1] + "";

               for (String v : values) {
                  if (v.contains(search)) {
                     return true;
                  }
               }
            }

            return false;
         }
      });
      engine.put(
         "headerValue",
         (JavaInvokable)a -> this.currentRequest != null && a.length > 0 ? markRequestDerived(runtime, this.currentRequest.getHeader(a[0] + "")) : null
      );
      engine.put("paramValue", (JavaInvokable)a -> this.currentRequest != null ? markRequestDerived(runtime, this.currentRequest.getParam(a[0] + "")) : null);
      engine.put("paramExists", (JavaInvokable)a -> {
         if (this.currentRequest == null) {
            return false;
         } else {
            List<String> values = this.currentRequest.getParamValues(a[0] + "");
            return values != null && !values.isEmpty();
         }
      });
      engine.put("bodyPath", (JavaInvokable)a -> {
         if (this.currentRequest == null) {
            return null;
         } else {
            Object body = this.currentRequest.getBodyConverted();
            if (body == null) {
               return null;
            } else {
               String path = a[0] + "";
               if (path.startsWith("/")) {
                  return body instanceof Node ? markRequestDerived(runtime, Xml.getTextValueByPath((Node)body, path)) : null;
               } else {
                  try {
                     return markRequestDerived(runtime, JsonPath.read(body, path, new Predicate[0]));
                  } catch (Exception var6x) {
                     logger.debug("bodyPath evaluation failed: {}", var6x.getMessage());
                     return null;
                  }
               }
            }
         }
      });
      engine.put("request", (JsLazy)() -> this.currentRequest != null ? markRequestDerived(runtime, this.currentRequest.getBodyConverted()) : null);
      engine.put("requestBytes", (JsLazy)() -> this.currentRequest != null ? this.currentRequest.getBody() : null);
      engine.put("requestPath", (JsLazy)() -> this.currentRequest != null ? markRequestDerived(runtime, this.currentRequest.getPath()) : null);
      engine.put("requestUri", (JsLazy)() -> this.currentRequest != null ? markRequestDerived(runtime, this.currentRequest.getPathRaw()) : null);
      engine.put("requestUrlBase", (JsLazy)() -> this.currentRequest != null ? markRequestDerived(runtime, this.currentRequest.jsGet("urlBase")) : null);
      engine.put("requestMethod", (JsLazy)() -> this.currentRequest != null ? markRequestDerived(runtime, this.currentRequest.getMethod()) : null);
      engine.put("requestHeaders", (JsLazy)() -> this.currentRequest != null ? markRequestDerived(runtime, this.currentRequest.getHeaders()) : null);
      engine.put("requestParams", (JsLazy)() -> this.currentRequest != null ? markRequestDerived(runtime, this.currentRequest.getParams()) : null);
      engine.put("requestParts", (JsLazy)() -> this.currentRequest != null ? markRequestDerived(runtime, this.currentRequest.getMultiParts()) : null);
      engine.put("requestCookies", (JsLazy)() -> this.currentRequest != null ? markRequestDerived(runtime, this.currentRequest.getCookies()) : null);
      if (args != null) {
         this.globals.putAll(args);

         for (Entry<String, Object> entry : args.entrySet()) {
            engine.put(entry.getKey(), entry.getValue());
         }
      }

      StepExecutor executor = new StepExecutor(runtime);
      if (feature.isBackgroundPresent()) {
         for (Step step : feature.getBackground().getSteps()) {
            StepResult result = executor.execute(step);
            if (result.isFailed()) {
               throw new RuntimeException("mock background failed at line " + step.getLine() + ": " + result.getError().getMessage(), result.getError());
            }
         }

         this.saveGlobals(engine);
         KarateConfig karateConfig = runtime.getConfig();
         if (karateConfig.isCorsEnabled()) {
            this.config.setCorsEnabled(true);
         }

         Object responseHeaders = karateConfig.getResponseHeaders();
         if (responseHeaders instanceof Map) {
            this.config.setResponseHeaders((Map<String, Object>)responseHeaders);
         }

         if (karateConfig.getBeforeScenario() instanceof JavaCallable callable) {
            this.config.setBeforeScenario(callable);
         }

         if (karateConfig.getAfterScenario() instanceof JavaCallable callable) {
            this.config.setAfterScenario(callable);
         }
      }

      logger.debug("initialized feature: {}", feature);
      return runtime;
   }

   private Scenario getFirstScenario(Feature feature) {
      for (FeatureSection section : feature.getSections()) {
         if (section.getScenario() != null) {
            return section.getScenario();
         }
      }

      Feature minimalFeature = Feature.read(Resource.text("Feature: Mock\nScenario: dummy\n* def x = 1"));
      return minimalFeature.getSections().getFirst().getScenario();
   }

   private void saveGlobals(Engine engine) {
      Map<String, Object> bindings = engine.getBindings();

      for (Entry<String, Object> entry : bindings.entrySet()) {
         String key = entry.getKey();
         if (!this.isBuiltInVariable(key)) {
            this.globals.put(key, entry.getValue());
         }
      }
   }

   private boolean isBuiltInVariable(String name) {
      return name.equals("karate")
         || name.equals("read")
         || name.equals("match")
         || name.startsWith("request")
         || name.startsWith("response")
         || name.equals("pathParams")
         || name.equals("pathMatches")
         || name.equals("methodIs")
         || name.equals("typeContains")
         || name.equals("acceptContains")
         || name.equals("headerContains")
         || name.equals("headerValue")
         || name.equals("paramValue")
         || name.equals("paramExists")
         || name.equals("bodyPath");
   }

   public HttpResponse apply(HttpRequest request) {
      this.requestLock.lock();

      HttpResponse var3;
      try {
         this.proceeded = false;
         HttpResponse response = this.handleRequest(request);
         if (this.config.isMockHeaderEnabled() && !this.proceeded) {
            response.setHeader("Karate-Mock", "true");
         }

         var3 = response;
      } finally {
         this.requestLock.unlock();
      }

      return var3;
   }

   private HttpResponse handleRequest(HttpRequest request) {
      if (this.config.isCorsEnabled() && "OPTIONS".equalsIgnoreCase(request.getMethod())) {
         return this.handleCorsPreFlight(request);
      } else {
         if (this.pathPrefix != null && request.getPath().startsWith(this.pathPrefix)) {
            request.setPath(request.getPath().substring(this.pathPrefix.length()));
         }

         request.processBody();

         for (Feature feature : this.features) {
            ScenarioRuntime runtime = this.runtimes.get(feature);
            Engine engine = runtime.getEngine();
            runtime.clearRequestDerived();
            this.setupRequestVariables(engine, request);

            for (FeatureSection section : feature.getSections()) {
               if (section.isOutline()) {
                  logger.warn("skipping scenario outline in mock - {}:{}", feature, section.getScenarioOutline().getLine());
               } else {
                  Scenario scenario = section.getScenario();
                  if (this.isMatchingScenario(scenario, engine)) {
                     return this.executeScenario(runtime, scenario, request);
                  }
               }
            }
         }

         logger.warn("no scenarios matched, returning 404: {} {}", request.getMethod(), request.getPath());
         return this.createNotFoundResponse();
      }
   }

   private HttpResponse handleCorsPreFlight(HttpRequest request) {
      HttpResponse response = new HttpResponse();
      response.setStatus(200);
      response.setHeader("Allow", "GET, HEAD, POST, PUT, DELETE, PATCH, OPTIONS");
      response.setHeader("Access-Control-Allow-Origin", "*");
      response.setHeader("Access-Control-Allow-Methods", "GET, HEAD, POST, PUT, DELETE, PATCH, OPTIONS");
      List<String> requestHeaders = request.getHeaderValues("Access-Control-Request-Headers");
      if (requestHeaders != null && !requestHeaders.isEmpty()) {
         response.setHeader("Access-Control-Allow-Headers", requestHeaders.toArray(new String[0]));
      }

      return response;
   }

   private static Object markRequestDerived(ScenarioRuntime runtime, Object value) {
      runtime.markRequestDerived(value);
      return value;
   }

   private void setupRequestVariables(Engine engine, HttpRequest request) {
      this.currentRequest = request;
      request.processBody();

      for (Entry<String, Object> entry : this.globals.entrySet()) {
         engine.put(entry.getKey(), entry.getValue());
      }

      engine.put("response", null);
      engine.put("responseStatus", 200);
      engine.put("responseStatusText", null);
      engine.put("responseHeaders", new HashMap());
      engine.put("responseDelay", 0);
      engine.put("pathParams", new HashMap());
   }

   private boolean isMatchingScenario(Scenario scenario, Engine engine) {
      String expression = StringUtils.trimToNull(scenario.getName());
      if (expression == null) {
         logger.debug("catch-all scenario matched at line: {}", scenario.getLine());
         return true;
      } else {
         try {
            Object result = engine.eval(expression);
            if (Boolean.TRUE.equals(result)) {
               logger.debug("scenario matched at line {}: {}", scenario.getLine(), expression);
               return true;
            } else {
               logger.trace("scenario skipped at line {}: {}", scenario.getLine(), expression);
               return false;
            }
         } catch (Exception var5) {
            logger.warn("scenario match evaluation failed at line {}: {} - {}", new Object[]{scenario.getLine(), expression, var5.getMessage()});
            return false;
         }
      }
   }

   private HttpResponse executeScenario(ScenarioRuntime runtime, Scenario scenario, HttpRequest request) {
      Engine engine = runtime.getEngine();
      StepExecutor executor = new StepExecutor(runtime);
      Exception beforeError = this.invokeMockHook(this.config.getBeforeScenario(), "beforeScenario");
      if (beforeError != null) {
         return this.hookErrorResponse("beforeScenario", beforeError);
      } else {
         for (Step step : scenario.getSteps()) {
            StepResult result = executor.execute(step);
            if (result.isFailed()) {
               logger.error("step execution failed at line {}: {}", step.getLine(), result.getError().getMessage());
               return HttpResponse.error(500, result.getError().getMessage());
            }
         }

         this.saveGlobals(engine);
         Exception afterError = this.invokeMockHook(this.config.getAfterScenario(), "afterScenario");
         return afterError != null ? this.hookErrorResponse("afterScenario", afterError) : this.buildResponse(runtime, engine, request);
      }
   }

   private Exception invokeMockHook(JavaCallable hook, String hookName) {
      if (hook == null) {
         return null;
      } else {
         try {
            hook.call(null);
            return null;
         } catch (Exception var4) {
            logger.warn("{} hook failed: {}", hookName, var4.getMessage());
            return var4;
         }
      }
   }

   private HttpResponse hookErrorResponse(String hookName, Exception error) {
      return HttpResponse.error(500, hookName + " hook failed: " + error.getMessage());
   }

   private HttpResponse buildResponse(ScenarioRuntime runtime, Engine engine, HttpRequest request) {
      HttpResponse response = new HttpResponse();
      Object responseBody = engine.get("response");
      Object responseStatus = engine.get("responseStatus");
      Object responseStatusText = engine.get("responseStatusText");
      Object responseHeaders = engine.get("responseHeaders");
      Object responseDelay = engine.get("responseDelay");
      if (responseBody instanceof HttpResponse proceedResponse) {
         this.proceeded = true;
         response.setStatus(proceedResponse.getStatus());
         response.setStatusText(proceedResponse.getStatusText());
         response.setBody(proceedResponse.getBodyBytes(), proceedResponse.getResourceType());
         if (proceedResponse.getHeaders() != null) {
            response.setHeaders(proceedResponse.getHeaders());
         }

         if (this.config.isCorsEnabled()) {
            response.setHeader("Access-Control-Allow-Origin", "*");
         }

         return response;
      } else {
         if (responseStatus instanceof Number) {
            response.setStatus(((Number)responseStatus).intValue());
         }

         if (responseStatusText instanceof String s) {
            response.setStatusText(s);
         }

         if (responseBody != null) {
            if (runtime.hasFailedEmbedded()) {
               String stranded = strandedEmbedded(runtime, responseBody, "");
               if (stranded != null) {
                  return HttpResponse.error(500, strandedMessage(runtime, stranded));
               }
            }

            response.setBodyDynamic(responseBody);
         }

         Map<String, Object> configuredHeaders = this.config.getResponseHeaders();
         if (configuredHeaders != null) {
            response.setHeaders(configuredHeaders);
         }

         if (responseHeaders instanceof Map) {
            response.setHeaders((Map<String, ?>)responseHeaders);
         }

         if (this.config.isCorsEnabled()) {
            response.setHeader("Access-Control-Allow-Origin", "*");
         }

         if (responseDelay instanceof Number) {
            int delay = ((Number)responseDelay).intValue();
            if (delay > 0) {
               response.setDelay(delay);
            }
         }

         return response;
      }
   }

   private static String strandedEmbedded(ScenarioRuntime runtime, Object value, String path) {
      if (value instanceof String s && runtime.failedEmbedded(s) != null) {
         return path + "\u0000" + s;
      } else if (value instanceof Node node) {
         String xml = Xml.toString(node, false);

         for (String placeholder : runtime.failedEmbeddedKeys()) {
            if (xml.contains(placeholder)) {
               return path + "\u0000" + placeholder;
            }
         }

         return null;
      } else if (value instanceof Map<?, ?> map) {
         for (Entry<?, ?> e : map.entrySet()) {
            String found = strandedEmbedded(runtime, e.getValue(), path + "/" + e.getKey());
            if (found != null) {
               return found;
            }
         }

         return null;
      } else {
         if (value instanceof List<?> list) {
            for (int i = 0; i < list.size(); i++) {
               String found = strandedEmbedded(runtime, list.get(i), path + "/" + i);
               if (found != null) {
                  return found;
               }
            }
         }

         return null;
      }
   }

   private static String strandedMessage(ScenarioRuntime runtime, String stranded) {
      int sep = stranded.indexOf(0);
      String at = stranded.substring(0, sep);
      String placeholder = stranded.substring(sep + 1);
      Exception error = runtime.failedEmbedded(placeholder);
      return "mock response would have served an UNEVALUATED embedded expression as data"
         + (at.isEmpty() ? "" : " at " + at)
         + ": "
         + placeholder
         + (error != null && error.getMessage() != null ? " — " + error.getMessage() : "")
         + ". A mock body is never re-resolved by the match engine, so this would have gone out on the wire as the literal string. Fix the expression, or quote it differently if the literal text really is what you meant to return.";
   }

   private HttpResponse createNotFoundResponse() {
      return HttpResponse.notFound("no matching scenario");
   }

   public MockConfig getConfig() {
      return this.config;
   }

   public Map<String, Object> getGlobals() {
      return this.globals;
   }

   public Object getVariable(String name) {
      return this.globals.get(name);
   }

   public Object getVariableSnapshot(String name) {
      this.requestLock.lock();

      Object var2;
      try {
         var2 = StepUtils.deepCopy(this.globals.get(name));
      } finally {
         this.requestLock.unlock();
      }

      return var2;
   }
}
