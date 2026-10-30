package io.karatelabs.core;

import io.karatelabs.js.SimpleObject;
import io.karatelabs.output.LogContext;
import io.karatelabs.output.LogLevel;
import io.karatelabs.output.LogMask;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.slf4j.Logger;

public class KarateConfig implements SimpleObject {
   private static final Logger logger = LogContext.RUNTIME_LOGGER;
   private static final List<String> KEYS = List.of(
      "url",
      "readTimeout",
      "connectTimeout",
      "followRedirects",
      "localAddress",
      "charset",
      "strictResponseParsing",
      "ssl",
      "proxy",
      "auth",
      "retry",
      "report",
      "logging",
      "callSingleCache",
      "headers",
      "cookies",
      "httpRetryEnabled",
      "continueOnStepFailure",
      "abortedStepsShouldPass",
      "abortSuiteOnFailure",
      "matchEachEmptyAllowed",
      "corsEnabled",
      "responseHeaders",
      "afterScenario",
      "afterScenarioOutline",
      "afterFeature",
      "onStepFailure",
      "driverConfig"
   );
   private String url;
   private int readTimeout = 30000;
   private int connectTimeout = 30000;
   private boolean followRedirects = true;
   private String localAddress;
   private Charset charset = StandardCharsets.UTF_8;
   private boolean strictResponseParsing;
   private Map<String, Object> ssl = new HashMap<>();
   private Map<String, Object> proxy = new HashMap<>();
   private Map<String, Object> auth = new HashMap<>();
   private Map<String, Object> retry = new HashMap<>(Map.of("interval", 3000, "count", 3));
   private Map<String, Object> report = new HashMap<>(Map.of("showLog", true, "showAllSteps", true));
   private Map<String, Object> logging = new HashMap<>(Map.of("report", "debug", "console", "info", "pretty", true));
   private LogMask compiledMask = LogMask.DEFAULT;
   private Map<String, Object> callSingleCache = new HashMap<>();
   private Object headers;
   private Object cookies;
   private boolean httpRetryEnabled;
   private boolean continueOnStepFailure;
   private boolean abortedStepsShouldPass;
   private boolean abortSuiteOnFailure;
   private boolean matchEachEmptyAllowed;
   private boolean corsEnabled;
   private Object responseHeaders;
   private Object beforeScenario;
   private Object afterScenario;
   private Object afterScenarioOutline;
   private Object afterFeature;
   private Object onStepFailure;
   private Object driverConfig;

   public KarateConfig copy() {
      KarateConfig copy = new KarateConfig();
      copy.copyFrom(this);
      return copy;
   }

   public void copyFrom(KarateConfig other) {
      if (other != null) {
         this.url = other.url;
         this.readTimeout = other.readTimeout;
         this.connectTimeout = other.connectTimeout;
         this.followRedirects = other.followRedirects;
         this.localAddress = other.localAddress;
         this.charset = other.charset;
         this.strictResponseParsing = other.strictResponseParsing;
         this.ssl = new HashMap<>(other.ssl);
         this.proxy = new HashMap<>(other.proxy);
         if (other.proxy.containsKey("nonProxyHosts") && other.proxy.get("nonProxyHosts") instanceof List<?> list) {
            this.proxy.put("nonProxyHosts", new ArrayList(list));
         }

         this.auth = new HashMap<>(other.auth);
         this.retry = new HashMap<>(other.retry);
         this.report = new HashMap<>(other.report);
         this.logging = new HashMap<>(other.logging);
         this.compiledMask = other.compiledMask;
         this.callSingleCache = new HashMap<>(other.callSingleCache);
         this.headers = other.headers;
         this.cookies = other.cookies;
         this.httpRetryEnabled = other.httpRetryEnabled;
         this.continueOnStepFailure = other.continueOnStepFailure;
         this.abortedStepsShouldPass = other.abortedStepsShouldPass;
         this.abortSuiteOnFailure = other.abortSuiteOnFailure;
         this.matchEachEmptyAllowed = other.matchEachEmptyAllowed;
         this.corsEnabled = other.corsEnabled;
         this.responseHeaders = other.responseHeaders;
         this.beforeScenario = other.beforeScenario;
         this.afterScenario = other.afterScenario;
         this.afterScenarioOutline = other.afterScenarioOutline;
         this.afterFeature = other.afterFeature;
         this.onStepFailure = other.onStepFailure;
         this.driverConfig = other.driverConfig;
      }
   }

   public void copyChangedFrom(KarateConfig before, KarateConfig after) {
      if (after != null) {
         if (before == null) {
            this.copyFrom(after);
         } else {
            if (!Objects.equals(before.url, after.url)) {
               this.url = after.url;
            }

            if (before.readTimeout != after.readTimeout) {
               this.readTimeout = after.readTimeout;
            }

            if (before.connectTimeout != after.connectTimeout) {
               this.connectTimeout = after.connectTimeout;
            }

            if (before.followRedirects != after.followRedirects) {
               this.followRedirects = after.followRedirects;
            }

            if (!Objects.equals(before.localAddress, after.localAddress)) {
               this.localAddress = after.localAddress;
            }

            if (!Objects.equals(before.charset, after.charset)) {
               this.charset = after.charset;
            }

            if (before.strictResponseParsing != after.strictResponseParsing) {
               this.strictResponseParsing = after.strictResponseParsing;
            }

            if (!before.ssl.equals(after.ssl)) {
               this.ssl = new HashMap<>(after.ssl);
            }

            if (!before.proxy.equals(after.proxy)) {
               this.proxy = new HashMap<>(after.proxy);
               if (after.proxy.get("nonProxyHosts") instanceof List<?> list) {
                  this.proxy.put("nonProxyHosts", new ArrayList(list));
               }
            }

            if (!before.auth.equals(after.auth)) {
               this.auth = new HashMap<>(after.auth);
            }

            if (!before.retry.equals(after.retry)) {
               this.retry = new HashMap<>(after.retry);
            }

            if (!before.report.equals(after.report)) {
               this.report = new HashMap<>(after.report);
            }

            if (!before.logging.equals(after.logging)) {
               this.logging = new HashMap<>(after.logging);
               this.compiledMask = after.compiledMask;
            }

            if (!before.callSingleCache.equals(after.callSingleCache)) {
               this.callSingleCache = new HashMap<>(after.callSingleCache);
            }

            if (!Objects.equals(before.headers, after.headers)) {
               this.headers = after.headers;
            }

            if (!Objects.equals(before.cookies, after.cookies)) {
               this.cookies = after.cookies;
            }

            if (before.httpRetryEnabled != after.httpRetryEnabled) {
               this.httpRetryEnabled = after.httpRetryEnabled;
            }

            if (before.continueOnStepFailure != after.continueOnStepFailure) {
               this.continueOnStepFailure = after.continueOnStepFailure;
            }

            if (before.abortedStepsShouldPass != after.abortedStepsShouldPass) {
               this.abortedStepsShouldPass = after.abortedStepsShouldPass;
            }

            if (before.abortSuiteOnFailure != after.abortSuiteOnFailure) {
               this.abortSuiteOnFailure = after.abortSuiteOnFailure;
            }

            if (before.matchEachEmptyAllowed != after.matchEachEmptyAllowed) {
               this.matchEachEmptyAllowed = after.matchEachEmptyAllowed;
            }

            if (before.corsEnabled != after.corsEnabled) {
               this.corsEnabled = after.corsEnabled;
            }

            if (!Objects.equals(before.responseHeaders, after.responseHeaders)) {
               this.responseHeaders = after.responseHeaders;
            }

            if (!Objects.equals(before.beforeScenario, after.beforeScenario)) {
               this.beforeScenario = after.beforeScenario;
            }

            if (!Objects.equals(before.afterScenario, after.afterScenario)) {
               this.afterScenario = after.afterScenario;
            }

            if (!Objects.equals(before.afterScenarioOutline, after.afterScenarioOutline)) {
               this.afterScenarioOutline = after.afterScenarioOutline;
            }

            if (!Objects.equals(before.afterFeature, after.afterFeature)) {
               this.afterFeature = after.afterFeature;
            }

            if (!Objects.equals(before.onStepFailure, after.onStepFailure)) {
               this.onStepFailure = after.onStepFailure;
            }

            if (!Objects.equals(before.driverConfig, after.driverConfig)) {
               this.driverConfig = after.driverConfig;
            }
         }
      }
   }

   public void configure(String key, Object value) {
      key = key != null ? key.trim() : "";
      switch (key) {
         case "ssl":
            this.configureSsl(value);
            break;
         case "proxy":
            this.configureProxy(value);
            break;
         case "readTimeout":
            this.readTimeout = toInt(value);
            break;
         case "connectTimeout":
            this.connectTimeout = toInt(value);
            break;
         case "followRedirects":
            this.followRedirects = toBoolean(value);
            break;
         case "strictResponseParsing":
            this.strictResponseParsing = toBoolean(value);
            break;
         case "localAddress":
            this.localAddress = toString(value);
            break;
         case "charset":
            this.charset = value == null ? null : Charset.forName(toString(value));
            break;
         case "ntlmAuth":
            if (value instanceof Map<?, ?> map) {
               Map<String, Object> ntlm = new HashMap<>();
               ntlm.put("type", "ntlm");
               ntlm.putAll((Map<? extends String, ? extends Object>)map);
               this.configureAuth(ntlm);
            } else if (value == null) {
               this.configureAuth(null);
            }
            break;
         case "auth":
            this.configureAuth(value);
            break;
         case "url":
            this.url = toString(value);
            break;
         case "headers":
            this.headers = value;
            break;
         case "cookies":
            this.cookies = value;
            break;
         case "retry":
            this.configureRetry(value);
            break;
         case "httpRetryEnabled":
            this.httpRetryEnabled = toBoolean(value);
            break;
         case "report":
            this.configureReport(value);
            break;
         case "logging":
            this.configureLogging(value);
            break;
         case "callSingleCache":
            this.configureCallSingleCache(value);
            break;
         case "continueOnStepFailure":
            this.continueOnStepFailure = toBoolean(value);
            break;
         case "abortedStepsShouldPass":
            this.abortedStepsShouldPass = toBoolean(value);
            break;
         case "abortSuiteOnFailure":
            this.abortSuiteOnFailure = toBoolean(value);
            break;
         case "matchEachEmptyAllowed":
            this.matchEachEmptyAllowed = toBoolean(value);
            break;
         case "cors":
            this.corsEnabled = toBoolean(value);
            break;
         case "responseHeaders":
            this.responseHeaders = value;
            break;
         case "beforeScenario":
            this.beforeScenario = value;
            break;
         case "afterScenario":
            this.afterScenario = value;
            break;
         case "afterScenarioOutline":
            this.afterScenarioOutline = value;
            break;
         case "afterFeature":
            this.afterFeature = value;
            break;
         case "onStepFailure":
            this.onStepFailure = value;
            break;
         case "driver":
            this.driverConfig = value;
            break;
         case "logPrettyRequest":
         case "logPrettyResponse":
            logger.warn("configure '{}' is deprecated; use 'configure logging = {{ pretty: true|false }}'", key);
            break;
         case "printEnabled":
            logger.warn(
               "configure 'printEnabled' is deprecated; print/karate.log go to the 'karate.scenario' SLF4J category - set its level to WARN in logback.xml to keep them off the console, or 'configure logging = {{ console: \"warn\" }}' to quiet all karate console output"
            );
            break;
         case "lowerCaseResponseHeaders":
            logger.warn(
               "configure 'lowerCaseResponseHeaders' is deprecated; 'match header X' is already case-insensitive and 'karate.lowerCase(responseHeaders)' covers direct map access"
            );
            break;
         case "logModifier":
            logger.warn(
               "configure 'logModifier' is removed; use the declarative form: 'configure logging = {{ mask: {{ headers: [...], jsonPaths: [...], patterns: [...] }} }}'"
            );
            break;
         default:
            throw new RuntimeException("unexpected 'configure' key: '" + key + "'");
      }
   }

   private void configureSsl(Object value) {
      if (value == null) {
         this.ssl.clear();
      } else if (value instanceof Boolean b) {
         this.ssl.clear();
         this.ssl.put("enabled", b);
         this.ssl.put("trustAll", true);
      } else if (value instanceof String s) {
         this.ssl.clear();
         this.ssl.put("enabled", true);
         this.ssl.put("algorithm", s);
         this.ssl.put("trustAll", true);
      } else {
         if (value instanceof Map<?, ?> map) {
            this.ssl.clear();
            this.ssl.put("enabled", true);
            if (map.containsKey("algorithm")) {
               this.ssl.put("algorithm", toString(map.get("algorithm")));
            }

            if (map.containsKey("keyStore")) {
               this.ssl.put("keyStore", toString(map.get("keyStore")));
            }

            if (map.containsKey("keyStorePassword")) {
               this.ssl.put("keyStorePassword", toString(map.get("keyStorePassword")));
            }

            if (map.containsKey("keyStoreType")) {
               this.ssl.put("keyStoreType", toString(map.get("keyStoreType")));
            }

            if (map.containsKey("trustStore")) {
               this.ssl.put("trustStore", toString(map.get("trustStore")));
            }

            if (map.containsKey("trustStorePassword")) {
               this.ssl.put("trustStorePassword", toString(map.get("trustStorePassword")));
            }

            if (map.containsKey("trustStoreType")) {
               this.ssl.put("trustStoreType", toString(map.get("trustStoreType")));
            }

            if (map.containsKey("trustAll")) {
               this.ssl.put("trustAll", toBoolean(map.get("trustAll")));
            } else {
               this.ssl.put("trustAll", true);
            }
         }
      }
   }

   private void configureProxy(Object value) {
      if (value == null) {
         this.proxy.clear();
      } else if (value instanceof String s) {
         this.proxy.clear();
         this.proxy.put("uri", s);
      } else {
         if (value instanceof Map<?, ?> map) {
            this.proxy.clear();
            if (map.containsKey("uri")) {
               this.proxy.put("uri", toString(map.get("uri")));
            }

            if (map.containsKey("username")) {
               this.proxy.put("username", toString(map.get("username")));
            }

            if (map.containsKey("password")) {
               this.proxy.put("password", toString(map.get("password")));
            }

            if (map.containsKey("nonProxyHosts") && map.get("nonProxyHosts") instanceof List<?> list) {
               List<String> hosts = new ArrayList<>();

               for (Object item : list) {
                  hosts.add(toString(item));
               }

               this.proxy.put("nonProxyHosts", hosts);
            }
         }
      }
   }

   private void configureAuth(Object value) {
      if (value == null) {
         this.auth.clear();
      } else {
         if (value instanceof Map<?, ?> map) {
            this.auth.clear();
            String type = toString(map.get("type"));
            this.auth.put("type", type);
            String var4 = type != null ? type : "";
            switch (var4) {
               case "basic":
                  this.auth.put("username", toString(map.get("username")));
                  this.auth.put("password", toString(map.get("password")));
                  break;
               case "bearer":
                  this.auth.put("token", toString(map.get("token")));
                  break;
               case "oauth2":
                  this.auth.put("grantType", toString(map.get("grantType")));
                  this.auth.put("accessTokenUrl", toString(map.get("accessTokenUrl")));
                  this.auth.put("clientId", toString(map.get("clientId")));
                  this.auth.put("clientSecret", toString(map.get("clientSecret")));
                  if (map.containsKey("scope")) {
                     this.auth.put("scope", toString(map.get("scope")));
                  }
                  break;
               case "ntlm":
                  this.auth.put("username", toString(map.get("username")));
                  this.auth.put("password", toString(map.get("password")));
                  if (map.containsKey("domain")) {
                     this.auth.put("domain", toString(map.get("domain")));
                  }

                  if (map.containsKey("workstation")) {
                     this.auth.put("workstation", toString(map.get("workstation")));
                  }
            }
         }
      }
   }

   private void configureRetry(Object value) {
      if (value instanceof Map<?, ?> map) {
         if (map.containsKey("interval")) {
            this.retry.put("interval", toInt(map.get("interval")));
         }

         if (map.containsKey("count")) {
            this.retry.put("count", toInt(map.get("count")));
         }
      }
   }

   private void configureReport(Object value) {
      if (value instanceof Boolean b) {
         this.report.put("showLog", b);
         this.report.put("showAllSteps", b);
      } else {
         if (value instanceof Map<?, ?> map) {
            if (map.containsKey("logLevel")) {
               throw new RuntimeException(
                  "'configure report = { logLevel: ... }' is no longer supported; use 'configure logging = { report: \"info\" }' instead (see docs/MIGRATION_GUIDE.md#logging)"
               );
            }

            if (map.containsKey("showLog")) {
               this.report.put("showLog", toBoolean(map.get("showLog")));
            }

            if (map.containsKey("showAllSteps")) {
               this.report.put("showAllSteps", toBoolean(map.get("showAllSteps")));
            }
         }
      }
   }

   private void configureLogging(Object value) {
      if (value instanceof Map<?, ?> raw) {
         if (raw.containsKey("report")) {
            String levelStr = toString(raw.get("report"));
            this.logging.put("report", levelStr);
            LogContext.setLogLevel(LogLevel.valueOf(levelStr.toUpperCase()));
         }

         if (raw.containsKey("console")) {
            String levelStr = toString(raw.get("console"));
            this.logging.put("console", levelStr);
            LogContext.setRuntimeLogLevel(levelStr);
         }

         if (raw.containsKey("pretty")) {
            boolean pretty = toBoolean(raw.get("pretty"));
            this.logging.put("pretty", pretty);
            LogContext.get().setPretty(pretty);
         }

         if (raw.containsKey("mask")) {
            Object maskValue = raw.get("mask");
            if (maskValue == null || Boolean.TRUE.equals(maskValue)) {
               this.logging.remove("mask");
               this.compiledMask = LogMask.DEFAULT;
               LogContext.get().setMask(LogMask.DEFAULT);
            } else if (Boolean.FALSE.equals(maskValue)) {
               this.logging.put("mask", false);
               this.compiledMask = null;
               LogContext.get().setMask(null);
            } else {
               if (!(maskValue instanceof Map<?, ?> maskMap)) {
                  throw new RuntimeException("configure 'logging.mask' expects a map or false, got: " + maskValue.getClass().getName());
               }

               LogMask compiled = LogMask.fromMap((Map<String, Object>)maskMap);
               this.logging.put("mask", maskMap);
               this.compiledMask = compiled;
               LogContext.get().setMask(compiled);
            }
         }
      } else {
         throw new RuntimeException("configure 'logging' expects a map, got: " + (value == null ? "null" : value.getClass().getName()));
      }
   }

   public void applyLoggingToContext(LogContext ctx) {
      if (ctx != null) {
         ctx.setMask(this.compiledMask);
         Object prettyVal = this.logging.get("pretty");
         ctx.setPretty(prettyVal == null ? true : toBoolean(prettyVal));
      }
   }

   public LogMask getCompiledMask() {
      return this.compiledMask;
   }

   private void configureCallSingleCache(Object value) {
      if (value instanceof Map<?, ?> map) {
         if (map.containsKey("minutes")) {
            this.callSingleCache.put("minutes", toInt(map.get("minutes")));
         }

         if (map.containsKey("dir")) {
            this.callSingleCache.put("dir", toString(map.get("dir")));
         }
      }
   }

   private static String toString(Object value) {
      return value == null ? null : value.toString();
   }

   private static boolean toBoolean(Object value) {
      if (value == null) {
         return false;
      } else {
         return value instanceof Boolean b ? b : Boolean.parseBoolean(value.toString());
      }
   }

   private static int toInt(Object value) {
      if (value == null) {
         return 0;
      } else {
         return value instanceof Number n ? n.intValue() : Integer.parseInt(value.toString());
      }
   }

   @Override
   public Collection<String> jsKeys() {
      return KEYS;
   }

   @Override
   public Object jsGet(String name) {
      return switch (name) {
         case "url" -> this.url;
         case "readTimeout" -> this.readTimeout;
         case "connectTimeout" -> this.connectTimeout;
         case "followRedirects" -> this.followRedirects;
         case "localAddress" -> this.localAddress;
         case "charset" -> this.charset != null ? this.charset.name() : null;
         case "strictResponseParsing" -> this.strictResponseParsing;
         case "ssl" -> this.ssl;
         case "proxy" -> this.proxy;
         case "ntlmAuth" -> "ntlm".equals(this.auth.get("type")) ? this.auth : null;
         case "auth" -> this.auth;
         case "retry" -> this.retry;
         case "report" -> this.report;
         case "logging" -> this.logging;
         case "callSingleCache" -> this.callSingleCache;
         case "sslEnabled" -> this.ssl.getOrDefault("enabled", false);
         case "sslAlgorithm" -> this.ssl.getOrDefault("algorithm", "TLS");
         case "sslKeyStore" -> this.ssl.get("keyStore");
         case "sslKeyStorePassword" -> this.ssl.get("keyStorePassword");
         case "sslKeyStoreType" -> this.ssl.get("keyStoreType");
         case "sslTrustStore" -> this.ssl.get("trustStore");
         case "sslTrustStorePassword" -> this.ssl.get("trustStorePassword");
         case "sslTrustStoreType" -> this.ssl.get("trustStoreType");
         case "sslTrustAll" -> this.ssl.getOrDefault("trustAll", true);
         case "proxyUri" -> this.proxy.get("uri");
         case "proxyUsername" -> this.proxy.get("username");
         case "proxyPassword" -> this.proxy.get("password");
         case "nonProxyHosts" -> this.proxy.get("nonProxyHosts");
         case "ntlmUsername" -> "ntlm".equals(this.auth.get("type")) ? this.auth.get("username") : null;
         case "ntlmPassword" -> "ntlm".equals(this.auth.get("type")) ? this.auth.get("password") : null;
         case "ntlmDomain" -> "ntlm".equals(this.auth.get("type")) ? this.auth.get("domain") : null;
         case "ntlmWorkstation" -> "ntlm".equals(this.auth.get("type")) ? this.auth.get("workstation") : null;
         case "authType" -> this.auth.get("type");
         case "authUsername" -> this.auth.get("username");
         case "authPassword" -> this.auth.get("password");
         case "authToken" -> this.auth.get("token");
         case "authAccessTokenUrl" -> this.auth.get("accessTokenUrl");
         case "authClientId" -> this.auth.get("clientId");
         case "authClientSecret" -> this.auth.get("clientSecret");
         case "authScope" -> this.auth.get("scope");
         case "retryInterval" -> this.retry.getOrDefault("interval", 3000);
         case "retryCount" -> this.retry.getOrDefault("count", 3);
         case "showLog" -> this.report.getOrDefault("showLog", true);
         case "showAllSteps" -> this.report.getOrDefault("showAllSteps", true);
         case "callSingleCacheMinutes" -> this.callSingleCache.getOrDefault("minutes", 0);
         case "callSingleCacheDir" -> this.callSingleCache.get("dir");
         case "headers" -> this.headers;
         case "cookies" -> this.cookies;
         case "httpRetryEnabled" -> this.httpRetryEnabled;
         case "continueOnStepFailure" -> this.continueOnStepFailure;
         case "abortedStepsShouldPass" -> this.abortedStepsShouldPass;
         case "abortSuiteOnFailure" -> this.abortSuiteOnFailure;
         case "matchEachEmptyAllowed" -> this.matchEachEmptyAllowed;
         case "corsEnabled" -> this.corsEnabled;
         case "responseHeaders" -> this.responseHeaders;
         case "beforeScenario" -> this.beforeScenario;
         case "afterScenario" -> this.afterScenario;
         case "afterScenarioOutline" -> this.afterScenarioOutline;
         case "afterFeature" -> this.afterFeature;
         case "onStepFailure" -> this.onStepFailure;
         case "driverConfig" -> this.driverConfig;
         default -> null;
      };
   }

   public String getUrl() {
      return this.url;
   }

   public int getReadTimeout() {
      return this.readTimeout;
   }

   public int getConnectTimeout() {
      return this.connectTimeout;
   }

   public boolean isFollowRedirects() {
      return this.followRedirects;
   }

   public boolean isStrictResponseParsing() {
      return this.strictResponseParsing;
   }

   public String getLocalAddress() {
      return this.localAddress;
   }

   public Charset getCharset() {
      return this.charset;
   }

   public Map<String, Object> getSsl() {
      return this.ssl;
   }

   public boolean isSslEnabled() {
      return toBoolean(this.ssl.get("enabled"));
   }

   public String getSslAlgorithm() {
      Object val = this.ssl.get("algorithm");
      return val != null ? val.toString() : "TLS";
   }

   public String getSslKeyStore() {
      return toString(this.ssl.get("keyStore"));
   }

   public String getSslKeyStorePassword() {
      return toString(this.ssl.get("keyStorePassword"));
   }

   public String getSslKeyStoreType() {
      return toString(this.ssl.get("keyStoreType"));
   }

   public String getSslTrustStore() {
      return toString(this.ssl.get("trustStore"));
   }

   public String getSslTrustStorePassword() {
      return toString(this.ssl.get("trustStorePassword"));
   }

   public String getSslTrustStoreType() {
      return toString(this.ssl.get("trustStoreType"));
   }

   public boolean isSslTrustAll() {
      Object val = this.ssl.get("trustAll");
      return val == null || toBoolean(val);
   }

   public Map<String, Object> getProxy() {
      return this.proxy;
   }

   public String getProxyUri() {
      return toString(this.proxy.get("uri"));
   }

   public String getProxyUsername() {
      return toString(this.proxy.get("username"));
   }

   public String getProxyPassword() {
      return toString(this.proxy.get("password"));
   }

   public List<String> getNonProxyHosts() {
      Object val = this.proxy.get("nonProxyHosts");
      return val instanceof List ? (List)val : null;
   }

   public Map<String, Object> getNtlmAuth() {
      return "ntlm".equals(this.auth.get("type")) ? this.auth : null;
   }

   public String getNtlmUsername() {
      return "ntlm".equals(this.auth.get("type")) ? toString(this.auth.get("username")) : null;
   }

   public String getNtlmPassword() {
      return "ntlm".equals(this.auth.get("type")) ? toString(this.auth.get("password")) : null;
   }

   public String getNtlmDomain() {
      return "ntlm".equals(this.auth.get("type")) ? toString(this.auth.get("domain")) : null;
   }

   public String getNtlmWorkstation() {
      return "ntlm".equals(this.auth.get("type")) ? toString(this.auth.get("workstation")) : null;
   }

   public Map<String, Object> getAuth() {
      return this.auth;
   }

   public String getAuthType() {
      return toString(this.auth.get("type"));
   }

   public String getAuthUsername() {
      return toString(this.auth.get("username"));
   }

   public String getAuthPassword() {
      return toString(this.auth.get("password"));
   }

   public String getAuthToken() {
      return toString(this.auth.get("token"));
   }

   public String getAuthAccessTokenUrl() {
      return toString(this.auth.get("accessTokenUrl"));
   }

   public String getAuthClientId() {
      return toString(this.auth.get("clientId"));
   }

   public String getAuthClientSecret() {
      return toString(this.auth.get("clientSecret"));
   }

   public String getAuthScope() {
      return toString(this.auth.get("scope"));
   }

   public Map<String, Object> getRetry() {
      return this.retry;
   }

   public int getRetryInterval() {
      return toInt(this.retry.getOrDefault("interval", 3000));
   }

   public int getRetryCount() {
      return toInt(this.retry.getOrDefault("count", 3));
   }

   public Map<String, Object> getReport() {
      return this.report;
   }

   public boolean isShowLog() {
      return toBoolean(this.report.getOrDefault("showLog", true));
   }

   public boolean isShowAllSteps() {
      return toBoolean(this.report.getOrDefault("showAllSteps", true));
   }

   public Map<String, Object> getLogging() {
      return this.logging;
   }

   public String getLoggingReportLevel() {
      return toString(this.logging.getOrDefault("report", "debug"));
   }

   public String getLoggingConsoleLevel() {
      return toString(this.logging.getOrDefault("console", "info"));
   }

   public boolean isLoggingPretty() {
      return toBoolean(this.logging.getOrDefault("pretty", true));
   }

   public Map<String, Object> getCallSingleCache() {
      return this.callSingleCache;
   }

   public int getCallSingleCacheMinutes() {
      return toInt(this.callSingleCache.getOrDefault("minutes", 0));
   }

   public String getCallSingleCacheDir() {
      return toString(this.callSingleCache.get("dir"));
   }

   public Object getHeaders() {
      return this.headers;
   }

   public Object getCookies() {
      return this.cookies;
   }

   public boolean isHttpRetryEnabled() {
      return this.httpRetryEnabled;
   }

   public boolean isContinueOnStepFailure() {
      return this.continueOnStepFailure;
   }

   public boolean isAbortedStepsShouldPass() {
      return this.abortedStepsShouldPass;
   }

   public boolean isAbortSuiteOnFailure() {
      return this.abortSuiteOnFailure;
   }

   public boolean isMatchEachEmptyAllowed() {
      return this.matchEachEmptyAllowed;
   }

   public boolean isCorsEnabled() {
      return this.corsEnabled;
   }

   public Object getResponseHeaders() {
      return this.responseHeaders;
   }

   public Object getBeforeScenario() {
      return this.beforeScenario;
   }

   public Object getAfterScenario() {
      return this.afterScenario;
   }

   public Object getAfterScenarioOutline() {
      return this.afterScenarioOutline;
   }

   public Object getAfterFeature() {
      return this.afterFeature;
   }

   public Object getOnStepFailure() {
      return this.onStepFailure;
   }

   public Object getDriverConfig() {
      return this.driverConfig;
   }
}
