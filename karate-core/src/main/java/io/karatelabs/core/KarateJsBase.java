package io.karatelabs.core;

import io.karatelabs.common.Resource;
import io.karatelabs.common.StringUtils;
import io.karatelabs.common.Xml;
import io.karatelabs.gherkin.Feature;
import io.karatelabs.gherkin.FeatureSection;
import io.karatelabs.gherkin.Scenario;
import io.karatelabs.gherkin.ScenarioOutline;
import io.karatelabs.gherkin.Tag;
import io.karatelabs.http.HttpClient;
import io.karatelabs.http.HttpRequest;
import io.karatelabs.http.HttpRequestBuilder;
import io.karatelabs.http.HttpResponse;
import io.karatelabs.js.Context;
import io.karatelabs.js.Engine;
import io.karatelabs.js.ExternalBridge;
import io.karatelabs.js.JavaInvokable;
import io.karatelabs.js.JsValue;
import io.karatelabs.js.SimpleObject;
import io.karatelabs.markup.Markup;
import io.karatelabs.markup.ResourceResolver;
import io.karatelabs.match.Result;
import io.karatelabs.output.LogContext;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.w3c.dom.Node;

abstract class KarateJsBase implements SimpleObject {
   static final Logger logger = LogContext.RUNTIME_LOGGER;
   public final Resource root;
   public final Engine engine;
   public final HttpClient client;
   public final HttpRequestBuilder http;
   ResourceResolver resourceResolver;
   Markup _markup;
   Consumer<String> onDoc;
   BiConsumer<Context, Result> onMatch;
   KarateJsContext context;
   String env;
   String overrideOutputDir;
   MockHandler mockHandler;
   HttpRequest prevRequest;
   HttpResponse prevResponse;
   KarateJsLog logFacade;
   boolean javaBridgeEnabled = true;
   static final ExternalBridge DEFAULT_BRIDGE = new ExternalBridge() {};
   private static final LogContext.LogWriter SCENARIO_LOG = LogContext.with(LogContext.SCENARIO_LOGGER);

   KarateJsBase(Resource root, HttpClient client) {
      this.root = root;
      this.client = client;
      this.http = new HttpRequestBuilder(client).configSupplier(this::getConfig);
      this.engine = new Engine();
      this.engine.setLeadingFunctionExpression(true);
      this.engine.setOnConsoleLog(s -> SCENARIO_LOG.info(s));
      this.engine.setExternalBridge(DEFAULT_BRIDGE);
   }

   public void setJavaBridgeEnabled(boolean enabled) {
      this.engine.setExternalBridge(enabled ? DEFAULT_BRIDGE : null);
      this.javaBridgeEnabled = enabled;
   }

   void checkProcessEnabled(String name) {
      if (!this.javaBridgeEnabled) {
         throw new RuntimeException(
            name + "() is not enabled - process execution is off by default in a mock, use 'configure javaBridgeEnabled = true' to opt in"
         );
      }
   }

   public void setOnDoc(Consumer<String> onDoc) {
      this.onDoc = onDoc;
   }

   public void setResourceResolver(ResourceResolver resourceResolver) {
      this.resourceResolver = resourceResolver;
   }

   public void setOnMatch(BiConsumer<Context, Result> onMatch) {
      this.onMatch = onMatch;
   }

   public void setContext(KarateJsContext context) {
      this.context = context;
   }

   public void setEnv(String env) {
      this.env = env;
   }

   public void setMockHandler(MockHandler handler) {
      this.mockHandler = handler;
   }

   public void setOutputDir(String outputDir) {
      this.overrideOutputDir = outputDir;
   }

   ScenarioRuntime getRuntime() {
      return this.context != null ? this.context.getRuntime() : null;
   }

   Scenario getScenario() {
      ScenarioRuntime rt = this.getRuntime();
      return rt != null ? rt.getScenario() : null;
   }

   Resource getCurrentResource() {
      ScenarioRuntime rt = this.getRuntime();
      return rt != null && rt.getFeatureRuntime() != null ? rt.getFeatureRuntime().getFeature().getResource() : this.root;
   }

   String getOutputDir() {
      if (this.overrideOutputDir != null) {
         return this.overrideOutputDir;
      } else {
         ScenarioRuntime rt = this.getRuntime();
         return rt != null && rt.getFeatureRuntime() != null && rt.getFeatureRuntime().getSuite() != null
            ? rt.getFeatureRuntime().getSuite().getOutputDir()
            : "target";
      }
   }

   Map<String, Object> getInfo() {
      Scenario s = this.getScenario();
      if (s == null) {
         return Map.of();
      } else {
         Map<String, Object> info = new LinkedHashMap<>();
         Resource featureResource = s.getFeature().getResource();
         if (featureResource.isFile() && featureResource.getPath() != null) {
            info.put("featureDir", featureResource.getPath().getParent().toString());
            info.put("featureFileName", featureResource.getPath().getFileName().toString());
         }

         info.put("scenarioName", s.getName());
         info.put("scenarioDescription", s.getDescription());
         ScenarioRuntime rt = this.getRuntime();
         if (rt != null && rt.getError() != null) {
            info.put("errorMessage", rt.getError().getMessage());
         }

         return info;
      }
   }

   Map<String, Object> getScenarioData() {
      Scenario s = this.getScenario();
      if (s == null) {
         return Map.of();
      } else {
         ScenarioRuntime rt = this.getRuntime();
         String stableId = rt != null ? rt.resolveStableId() : null;
         return RunUtils.scenarioIdentity(s, stableId);
      }
   }

   Map<String, Object> getSuiteData() {
      ScenarioRuntime rt = this.getRuntime();
      if (rt != null && rt.getFeatureRuntime() != null) {
         Suite suite = rt.getFeatureRuntime().getSuite();
         if (suite == null) {
            return Map.of();
         } else {
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("threadCount", suite.threadCount);
            data.put("dryRun", suite.dryRun);
            return data;
         }
      } else {
         return Map.of();
      }
   }

   Map<String, Object> getFeatureData() {
      Scenario s = this.getScenario();
      if (s == null) {
         return Map.of();
      } else {
         Feature feature = s.getFeature();
         Map<String, Object> data = new LinkedHashMap<>();
         data.put("name", feature.getName());
         data.put("description", feature.getDescription());
         Resource resource = feature.getResource();
         data.put("prefixedPath", resource.getPrefixedPath());
         if (resource.isFile() && resource.getPath() != null) {
            data.put("fileName", resource.getPath().getFileName().toString());
            data.put("parentDir", resource.getPath().getParent().toString());
         }

         return data;
      }
   }

   List<String> getTags() {
      Scenario s = this.getScenario();
      if (s == null) {
         return List.of();
      } else {
         List<String> result = new ArrayList<>();

         for (Tag tag : s.getTagsEffective()) {
            result.add(tag.getText());
         }

         return result;
      }
   }

   Map<String, List<String>> getTagValues() {
      Scenario s = this.getScenario();
      if (s == null) {
         return Map.of();
      } else {
         Map<String, List<String>> result = new LinkedHashMap<>();
         List<Tag> featureTags = s.getFeature().getTags();
         if (featureTags != null) {
            for (Tag tag : featureTags) {
               result.computeIfAbsent(tag.getName(), k -> new ArrayList<>()).addAll(tag.getValues());
            }
         }

         List<Tag> scenarioTags = s.getTags();
         if (scenarioTags != null) {
            Set<String> seenAtScenarioLevel = new HashSet<>();

            for (Tag tag : scenarioTags) {
               String name = tag.getName();
               if (seenAtScenarioLevel.add(name)) {
                  result.put(name, new ArrayList<>(tag.getValues()));
               } else {
                  result.get(name).addAll(tag.getValues());
               }
            }
         }

         return result;
      }
   }

   Map<String, Object> getScenarioOutlineData() {
      Scenario s = this.getScenario();
      if (s != null && s.isOutlineExample()) {
         FeatureSection section = s.getSection();
         if (!section.isOutline()) {
            return null;
         } else {
            ScenarioOutline outline = section.getScenarioOutline();
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("name", outline.getName());
            data.put("description", outline.getDescription());
            data.put("line", outline.getLine());
            data.put("sectionIndex", section.getIndex());
            data.put("exampleTableCount", outline.getNumExampleTables());
            data.put("exampleTables", outline.getAllExampleData());
            data.put("numScenariosToExecute", outline.getNumScenarios());
            return data;
         }
      } else {
         return null;
      }
   }

   KarateConfig getConfig() {
      if (this.context != null) {
         KarateConfig cfg = this.context.getConfig();
         if (cfg != null) {
            return cfg.copy();
         }
      }

      return new KarateConfig();
   }

   JavaInvokable abort() {
      return args -> {
         ScenarioRuntime rt = this.getRuntime();
         if (rt != null) {
            rt.abort();
         }

         return null;
      };
   }

   JavaInvokable setup() {
      return args -> {
         ScenarioRuntime rt = this.getRuntime();
         if (rt == null) {
            throw new RuntimeException("karate.setup() is not available in this context");
         } else {
            String name = args.length > 0 && args[0] != null ? args[0].toString() : null;
            return rt.executeSetup(name);
         }
      };
   }

   JavaInvokable setupOnce() {
      return args -> {
         ScenarioRuntime rt = this.getRuntime();
         if (rt == null) {
            throw new RuntimeException("karate.setupOnce() is not available in this context");
         } else {
            String name = args.length > 0 && args[0] != null ? args[0].toString() : null;
            return rt.executeSetupOnce(name);
         }
      };
   }

   JavaInvokable callonce() {
      return args -> {
         ScenarioRuntime rt = this.getRuntime();
         if (rt == null) {
            throw new RuntimeException("karate.callonce() is not available in this context");
         } else if (args.length == 0) {
            throw new RuntimeException("karate.callonce() requires at least one argument (feature path)");
         } else {
            String path = args[0].toString();
            Object arg = args.length > 1 ? args[1] : null;
            return rt.executeJsCallOnce(path, arg);
         }
      };
   }

   JavaInvokable callSingle() {
      return args -> {
         ScenarioRuntime rt = this.getRuntime();
         if (rt == null) {
            throw new RuntimeException("karate.callSingle() is not available in this context");
         } else if (args.length == 0) {
            throw new RuntimeException("karate.callSingle() requires at least one argument (path)");
         } else {
            String path = args[0].toString();
            Object arg = args.length > 1 ? args[1] : null;
            return rt.executeCallSingle(path, arg);
         }
      };
   }

   JavaInvokable configure() {
      return args -> {
         if (args.length < 2) {
            throw new RuntimeException("configure() needs two arguments: key and value");
         } else {
            String key = args[0].toString();
            Object value = args[1];
            ScenarioRuntime rt = this.getRuntime();
            if (rt != null) {
               rt.configure(key, value);
            }

            return null;
         }
      };
   }

   Map<String, String> getProperties() {
      Suite suite = this.getSuite();
      if (suite != null) {
         return suite.getSystemProperties();
      } else {
         Map<String, String> props = new LinkedHashMap<>();
         System.getProperties().forEach((k, v) -> props.put(k.toString(), v.toString()));
         return props;
      }
   }

   String getProperty(String name) {
      Suite suite = this.getSuite();
      return suite != null ? suite.getSystemProperty(name) : System.getProperty(name);
   }

   String getEnvVar(String name) {
      Suite suite = this.getSuite();
      return suite != null ? suite.getEnvVar(name) : System.getenv(name);
   }

   private Suite getSuite() {
      ScenarioRuntime rt = this.getRuntime();
      return rt != null && rt.getFeatureRuntime() != null ? rt.getFeatureRuntime().getSuite() : null;
   }

   JavaInvokable log() {
      return args -> {
         StringBuilder sb = new StringBuilder();

         for (int i = 0; i < args.length; i++) {
            if (i > 0) {
               sb.append(" ");
            }

            Object arg = args[i];
            if (arg instanceof Node) {
               sb.append(Xml.toString((Node)arg, true));
            } else if (!(arg instanceof Map) && !(arg instanceof List)) {
               sb.append(arg);
            } else {
               sb.append(StringUtils.formatJson(arg));
            }
         }

         SCENARIO_LOG.info(sb.toString());
         return null;
      };
   }

   JavaInvokable embed() {
      return args -> {
         if (args.length < 1) {
            throw new RuntimeException("embed() needs at least one argument: data");
         } else {
            Object first = unwrapJs(args[0]);
            if (first instanceof Map<?, ?> map && map.get("parts") instanceof List) {
               LogContext.get().embed(this.toMultiPartEmbed(map));
               return null;
            } else {
               String mimeType = args.length > 1 && args[1] != null ? args[1].toString() : KarateJsUtils.detectMimeType(first);
               String name = args.length > 2 ? args[2].toString() : null;
               byte[] data = KarateJsUtils.convertToBytes(first);
               LogContext.get().embed(data, mimeType, name);
               return null;
            }
         }
      };
   }

   private StepResult.Embed toMultiPartEmbed(Map<?, ?> map) {
      String name = map.get("name") != null ? map.get("name").toString() : null;
      List<StepResult.Part> parts = new ArrayList<>();

      for (Object partObj : (List)map.get("parts")) {
         if (!(unwrapJs(partObj) instanceof Map<?, ?> pm)) {
            throw new RuntimeException("embed(): each 'parts' entry must be an object");
         }

         Object roleObj = pm.get("role");
         if (roleObj == null) {
            throw new RuntimeException("embed(): each part needs a 'role'");
         }

         String role = roleObj.toString();
         String mime = pm.get("mime") != null ? pm.get("mime").toString() : null;
         Object url = pm.get("url");
         if (url != null) {
            parts.add(new StepResult.Part(role, mime, url.toString()));
         } else {
            Object dataObj = pm.get("data");
            Object pathObj = pm.get("path");
            byte[] bytes;
            if (dataObj != null) {
               bytes = KarateJsUtils.convertToBytes(unwrapJs(dataObj));
            } else {
               if (pathObj == null) {
                  throw new RuntimeException("embed(): part '" + role + "' needs 'data', 'path', or 'url'");
               }

               bytes = this.readPathBytes(pathObj.toString());
            }

            parts.add(new StepResult.Part(role, mime != null ? mime : KarateJsUtils.detectMimeType(bytes), bytes));
         }
      }

      Object meta = map.get("meta");
      Map<String, Object> metaMap = meta instanceof Map ? (Map)meta : null;
      return new StepResult.Embed(name, parts, metaMap);
   }

   private byte[] readPathBytes(String path) {
      try {
         byte[] var3;
         try (InputStream is = this.getCurrentResource().resolve(path).getStream()) {
            var3 = is.readAllBytes();
         }

         return var3;
      } catch (Exception var7) {
         throw new RuntimeException("embed(): failed to read part path '" + path + "': " + var7.getMessage(), var7);
      }
   }

   private static Object unwrapJs(Object o) {
      return o instanceof JsValue jv ? jv.getJavaValue() : o;
   }

   JavaInvokable signal() {
      return args -> {
         ScenarioRuntime rt = this.getRuntime();
         if (rt != null && args.length > 0) {
            rt.setListenResult(args[0]);
         }

         return null;
      };
   }

   KarateJsLog getLogger() {
      if (this.logFacade == null) {
         this.logFacade = new KarateJsLog();
      }

      return this.logFacade;
   }

   Map<String, Object> getPrevRequest() {
      if (this.prevRequest == null) {
         return null;
      } else {
         Map<String, Object> map = new LinkedHashMap<>();
         map.put("method", this.prevRequest.getMethod());
         map.put("url", this.prevRequest.getUrlAndPath());
         map.put("headers", this.prevRequest.getHeaders());
         map.put("body", this.prevRequest.getBody());
         return map;
      }
   }
}
