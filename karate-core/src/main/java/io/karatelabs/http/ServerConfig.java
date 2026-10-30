package io.karatelabs.http;

import io.karatelabs.js.Engine;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class ServerConfig {
   private String resourceRoot;
   private String contextPath = "";
   private boolean devMode = false;
   private boolean devTrace = "true".equalsIgnoreCase(System.getenv("KARATE_DEV_TRACE"));
   private int sessionExpirySeconds = 600;
   private String sessionCookieName = "karate.sid";
   private SessionStore sessionStore;
   private String apiPrefix = "/api/";
   private boolean apiPrefixEnabled = true;
   private String staticPrefix = "/pub/";
   private boolean staticPrefixEnabled = true;
   private boolean csrfEnabled = true;
   private Set<String> csrfExemptPaths;
   private SameSite sessionSameSite = SameSite.LAX;
   private String[] allowedOrigins;
   private boolean securityHeadersEnabled = true;
   private String contentSecurityPolicy;
   private String xFrameOptions = "DENY";
   private String referrerPolicy = "strict-origin-when-cross-origin";
   private boolean hstsEnabled = false;
   private long hstsMaxAge = 31536000L;
   private boolean hstsIncludeSubDomains = true;
   private String errorTemplate404;
   private String errorTemplate500;
   private LinkedHashMap<String, String> templateRoutes;
   private LinkedHashMap<String, String> jsRoutes;
   private String shellTemplate;
   private Set<String> rawPaths;
   private Consumer<HttpRequest> requestInterceptor;
   private BiFunction<HttpRequest, ServerMarkupContext, HttpResponse> requestFilter;
   private Consumer<String> logHandler;
   private Supplier<Engine> engineSupplier = () -> ServerRequestCycle.get().getEngine();
   private Map<String, Object> globalVariables;

   public ServerConfig() {
   }

   public ServerConfig(String resourceRoot) {
      this.resourceRoot = resourceRoot;
   }

   public String getResourceRoot() {
      return this.resourceRoot;
   }

   public String getContextPath() {
      return this.contextPath;
   }

   public boolean isDevMode() {
      return this.devMode;
   }

   public boolean isDevTrace() {
      return this.devTrace;
   }

   public int getSessionExpirySeconds() {
      return this.sessionExpirySeconds;
   }

   public String getSessionCookieName() {
      return this.sessionCookieName;
   }

   public SessionStore getSessionStore() {
      return this.sessionStore;
   }

   public String getApiPrefix() {
      return this.apiPrefix;
   }

   public boolean isApiPrefixEnabled() {
      return this.apiPrefixEnabled;
   }

   public String getStaticPrefix() {
      return this.staticPrefix;
   }

   public boolean isStaticPrefixEnabled() {
      return this.staticPrefixEnabled;
   }

   public boolean isCsrfEnabled() {
      return this.csrfEnabled;
   }

   public SameSite getSessionSameSite() {
      return this.sessionSameSite;
   }

   public boolean isCsrfExemptPath(String path) {
      return this.csrfExemptPaths != null && this.csrfExemptPaths.contains(path);
   }

   public String[] getAllowedOrigins() {
      return this.allowedOrigins;
   }

   public boolean isSecurityHeadersEnabled() {
      return this.securityHeadersEnabled;
   }

   public String getContentSecurityPolicy() {
      return this.contentSecurityPolicy;
   }

   public String getXFrameOptions() {
      return this.xFrameOptions;
   }

   public String getReferrerPolicy() {
      return this.referrerPolicy;
   }

   public boolean isHstsEnabled() {
      return this.hstsEnabled;
   }

   public long getHstsMaxAge() {
      return this.hstsMaxAge;
   }

   public boolean isHstsIncludeSubDomains() {
      return this.hstsIncludeSubDomains;
   }

   public String getErrorTemplate404() {
      return this.errorTemplate404;
   }

   public String getErrorTemplate500() {
      return this.errorTemplate500;
   }

   public LinkedHashMap<String, String> getTemplateRoutes() {
      return this.templateRoutes;
   }

   public LinkedHashMap<String, String> getJsRoutes() {
      return this.jsRoutes;
   }

   public String getShellTemplate() {
      return this.shellTemplate;
   }

   public Set<String> getRawPaths() {
      return this.rawPaths;
   }

   public Consumer<HttpRequest> getRequestInterceptor() {
      return this.requestInterceptor;
   }

   public BiFunction<HttpRequest, ServerMarkupContext, HttpResponse> getRequestFilter() {
      return this.requestFilter;
   }

   public Consumer<String> getLogHandler() {
      return this.logHandler;
   }

   public Supplier<Engine> getEngineSupplier() {
      return this.engineSupplier;
   }

   public Map<String, Object> getGlobalVariables() {
      return this.globalVariables;
   }

   public ServerConfig resourceRoot(String resourceRoot) {
      this.resourceRoot = resourceRoot;
      return this;
   }

   public ServerConfig contextPath(String contextPath) {
      this.contextPath = contextPath != null ? contextPath : "";
      return this;
   }

   public ServerConfig devMode(boolean devMode) {
      this.devMode = devMode;
      return this;
   }

   public ServerConfig devTrace(boolean devTrace) {
      this.devTrace = devTrace;
      return this;
   }

   public ServerConfig sessionExpirySeconds(int sessionExpirySeconds) {
      this.sessionExpirySeconds = sessionExpirySeconds;
      return this;
   }

   public ServerConfig sessionCookieName(String sessionCookieName) {
      this.sessionCookieName = sessionCookieName;
      return this;
   }

   public ServerConfig sessionStore(SessionStore sessionStore) {
      this.sessionStore = sessionStore;
      return this;
   }

   public ServerConfig apiPrefix(String apiPrefix) {
      this.apiPrefix = apiPrefix;
      return this;
   }

   public ServerConfig apiPrefixEnabled(boolean apiPrefixEnabled) {
      this.apiPrefixEnabled = apiPrefixEnabled;
      return this;
   }

   public ServerConfig staticPrefix(String staticPrefix) {
      this.staticPrefix = staticPrefix;
      return this;
   }

   public ServerConfig staticPrefixEnabled(boolean staticPrefixEnabled) {
      this.staticPrefixEnabled = staticPrefixEnabled;
      return this;
   }

   public ServerConfig csrfEnabled(boolean csrfEnabled) {
      this.csrfEnabled = csrfEnabled;
      return this;
   }

   public ServerConfig csrfExemptPaths(String... csrfExemptPaths) {
      if (csrfExemptPaths != null && csrfExemptPaths.length != 0) {
         this.csrfExemptPaths = new HashSet<>(Arrays.asList(csrfExemptPaths));
      } else {
         this.csrfExemptPaths = null;
      }

      return this;
   }

   public ServerConfig sessionSameSite(SameSite sessionSameSite) {
      this.sessionSameSite = sessionSameSite == null ? SameSite.LAX : sessionSameSite;
      return this;
   }

   public ServerConfig allowedOrigins(String... allowedOrigins) {
      this.allowedOrigins = allowedOrigins;
      return this;
   }

   public ServerConfig securityHeadersEnabled(boolean securityHeadersEnabled) {
      this.securityHeadersEnabled = securityHeadersEnabled;
      return this;
   }

   public ServerConfig contentSecurityPolicy(String contentSecurityPolicy) {
      this.contentSecurityPolicy = contentSecurityPolicy;
      return this;
   }

   public ServerConfig xFrameOptions(String xFrameOptions) {
      this.xFrameOptions = xFrameOptions;
      return this;
   }

   public ServerConfig referrerPolicy(String referrerPolicy) {
      this.referrerPolicy = referrerPolicy;
      return this;
   }

   public ServerConfig hstsEnabled(boolean hstsEnabled) {
      this.hstsEnabled = hstsEnabled;
      return this;
   }

   public ServerConfig hstsMaxAge(long hstsMaxAge) {
      this.hstsMaxAge = hstsMaxAge;
      return this;
   }

   public ServerConfig hstsIncludeSubDomains(boolean hstsIncludeSubDomains) {
      this.hstsIncludeSubDomains = hstsIncludeSubDomains;
      return this;
   }

   public ServerConfig errorTemplate404(String errorTemplate404) {
      this.errorTemplate404 = errorTemplate404;
      return this;
   }

   public ServerConfig errorTemplate500(String errorTemplate500) {
      this.errorTemplate500 = errorTemplate500;
      return this;
   }

   public ServerConfig templateRoute(String pathPattern, String templateName) {
      if (this.templateRoutes == null) {
         this.templateRoutes = new LinkedHashMap<>();
      }

      this.templateRoutes.put(pathPattern, templateName);
      return this;
   }

   public ServerConfig jsRoute(String pathPattern, String jsFile) {
      if (this.jsRoutes == null) {
         this.jsRoutes = new LinkedHashMap<>();
      }

      this.jsRoutes.put(pathPattern, jsFile);
      return this;
   }

   public ServerConfig shellTemplate(String shellTemplate) {
      this.shellTemplate = shellTemplate;
      return this;
   }

   public ServerConfig rawPaths(String... rawPaths) {
      if (rawPaths != null && rawPaths.length != 0) {
         this.rawPaths = new HashSet<>(Arrays.asList(rawPaths));
      } else {
         this.rawPaths = null;
      }

      return this;
   }

   public ServerConfig requestInterceptor(Consumer<HttpRequest> requestInterceptor) {
      this.requestInterceptor = requestInterceptor;
      return this;
   }

   public ServerConfig requestFilter(BiFunction<HttpRequest, ServerMarkupContext, HttpResponse> requestFilter) {
      this.requestFilter = requestFilter;
      return this;
   }

   public ServerConfig logHandler(Consumer<String> logHandler) {
      this.logHandler = logHandler;
      return this;
   }

   public ServerConfig engineSupplier(Supplier<Engine> engineSupplier) {
      this.engineSupplier = engineSupplier;
      return this;
   }

   public ServerConfig globalVariables(Map<String, Object> globalVariables) {
      this.globalVariables = globalVariables;
      return this;
   }

   public boolean isApiPath(String path) {
      return this.apiPrefixEnabled && this.apiPrefix != null ? path.startsWith(this.apiPrefix) : false;
   }

   public boolean isStaticPath(String path) {
      return this.staticPrefixEnabled && this.staticPrefix != null ? path.startsWith(this.staticPrefix) : false;
   }

   public boolean isRawPath(String path) {
      return this.rawPaths != null && this.rawPaths.contains(path);
   }

   public boolean isSessionEnabled() {
      return this.sessionStore != null;
   }

   public Session createSession() {
      return this.sessionStore == null ? null : this.sessionStore.create(this.sessionExpirySeconds);
   }

   @Override
   public String toString() {
      return "ServerConfig{resourceRoot='"
         + this.resourceRoot
         + "', contextPath='"
         + this.contextPath
         + "', devMode="
         + this.devMode
         + ", apiPrefix='"
         + this.apiPrefix
         + "', staticPrefix='"
         + this.staticPrefix
         + "', csrfEnabled="
         + this.csrfEnabled
         + ", allowedOrigins="
         + Arrays.toString((Object[])this.allowedOrigins)
         + "}";
   }
}
