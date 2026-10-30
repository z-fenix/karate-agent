package io.karatelabs.http;

import io.karatelabs.common.FileUtils;
import io.karatelabs.common.Resource;
import io.karatelabs.common.ResourceType;
import io.karatelabs.markup.HxDialect;
import io.karatelabs.markup.Markup;
import io.karatelabs.markup.MarkupConfig;
import io.karatelabs.markup.ResourceResolver;
import io.karatelabs.output.HttpLogger;
import io.karatelabs.output.LogContext;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ServerRequestHandler implements Function<HttpRequest, HttpResponse> {
   private static final Logger logger = LoggerFactory.getLogger(ServerRequestHandler.class);
   private final ServerConfig config;
   private final ResourceResolver resolver;
   private final Markup markup;
   private final HttpLogger httpLogger = new HttpLogger(LogContext.SERVER_LOGGER);

   public ServerRequestHandler(ServerConfig config, ResourceResolver resolver) {
      this.config = config;
      this.resolver = resolver;
      MarkupConfig markupConfig = new MarkupConfig();
      markupConfig.setResolver(resolver);
      markupConfig.setDevMode(config.isDevMode());
      markupConfig.setDevTrace(config.isDevTrace());
      markupConfig.setEngineSupplier(config.getEngineSupplier());
      this.markup = Markup.init(markupConfig, new HxDialect(markupConfig));
      if (config.isDevMode()) {
         logger.warn("karate dev-mode enabled — templates hot-reload on every request");
         if (config.isDevTrace()) {
            logger.info("karate dev-trace enabled — fragment boundaries emitted as HTML comments (never enable in production)");
         }
      } else if (config.isDevTrace()) {
         logger.info("karate dev-trace ignored — requires devMode(true)");
      }
   }

   public HttpResponse apply(HttpRequest request) {
      long start = System.currentTimeMillis();
      this.httpLogger.logRequest(request);
      HttpResponse response = this.handle(request);
      response.setRequest(request);
      response.setStartTime(start);
      response.setResponseTime(System.currentTimeMillis() - start);
      this.httpLogger.logResponse(response);
      return response;
   }

   private HttpResponse handle(HttpRequest request) {
      HttpResponse response = new HttpResponse();
      String path = request.getPath();

      try {
         request.processBody();
         if (this.config.isStaticPath(path)) {
            return this.handleStatic(request, response);
         } else {
            ServerMarkupContext context = this.createContext(request, response);
            this.loadSession(request, context);
            context.loadFlashFromSession();
            if (this.config.getRequestFilter() != null) {
               HttpResponse filterResponse = this.config.getRequestFilter().apply(request, context);
               if (filterResponse != null) {
                  return filterResponse;
               }
            }

            HttpResponse csrfError = this.validateCsrf(request, context);
            if (csrfError != null) {
               return csrfError;
            } else {
               if (this.config.getRequestInterceptor() != null) {
                  this.config.getRequestInterceptor().accept(request);
               }

               ServerRequestCycle rc = ServerRequestCycle.init(this.config, context, this.resolver, this.markup);
               HttpResponse result = rc.handle();
               if (context.hasRedirect()) {
                  context.persistFlashToSession();
                  applyRedirect(request, result, context.getRedirectPath());
               }

               this.saveSession(context, result);
               if (SecurityHeaders.isHtmlResponse(result.getHeader("Content-Type"))) {
                  SecurityHeaders.apply(result, this.config);
               }

               return result;
            }
         }
      } catch (ServerRequestHandler.RedirectException var8) {
         applyRedirect(request, response, var8.getLocation());
         return response;
      } catch (Exception var9) {
         response.setStatus(500);
         response.setBody("Internal Server Error: " + var9.getMessage(), ResourceType.TEXT);
         return response;
      }
   }

   private ServerMarkupContext createContext(HttpRequest request, HttpResponse response) {
      ServerMarkupContext context = new ServerMarkupContext(request, response, this.config);
      context.setResourceResolver(path -> this.resolver.resolve(path, null));
      return context;
   }

   private void loadSession(HttpRequest request, ServerMarkupContext context) {
      if (this.config.isSessionEnabled()) {
         String sessionId = this.getSessionIdFromCookie(request);
         if (sessionId != null) {
            Session session = this.config.getSessionStore().get(sessionId);
            if (session != null) {
               context.setSession(session);
            }
         }
      }
   }

   private String getSessionIdFromCookie(HttpRequest request) {
      String cookieHeader = request.getHeader("Cookie");
      if (cookieHeader == null) {
         return null;
      } else {
         String cookieName = this.config.getSessionCookieName();

         for (String part : cookieHeader.split(";")) {
            String trimmed = part.trim();
            if (trimmed.startsWith(cookieName + "=")) {
               return trimmed.substring(cookieName.length() + 1);
            }
         }

         return null;
      }
   }

   static void applyRedirect(HttpRequest request, HttpResponse response, String location) {
      if (request != null && request.isAjax()) {
         response.setStatus(200);
         response.setHeader("HX-Redirect", location);
      } else {
         response.setStatus(302);
         response.setHeader("Location", location);
      }
   }

   private HttpResponse validateCsrf(HttpRequest request, ServerMarkupContext context) {
      if (!this.config.isCsrfEnabled()) {
         return null;
      } else {
         String method = request.getMethod();
         if (!CsrfProtection.requiresValidation(method)) {
            return null;
         } else if (this.config.isCsrfExemptPath(request.getPath())) {
            return null;
         } else {
            Session session = context.getSession();
            if (session == null || session.isTemporary()) {
               return null;
            } else {
               return !CsrfProtection.validate(request, session) ? HttpResponse.text(403, "Forbidden: Invalid or missing CSRF token") : null;
            }
         }
      }
   }

   private void saveSession(ServerMarkupContext context, HttpResponse response) {
      Session session = context.getSession();
      if (session != null && !session.isTemporary()) {
         this.config.getSessionStore().save(session);
         SameSite sameSite = this.config.getSessionSameSite();
         String cookieValue = this.config.getSessionCookieName() + "=" + session.getId() + "; Path=/; HttpOnly; SameSite=" + sameSite.headerValue();
         if (isSecure(context.getRequest(), sameSite)) {
            cookieValue = cookieValue + "; Secure";
         }

         appendSetCookie(response, cookieValue);
      } else {
         if (context.wasExplicitlyClosed()) {
            this.clearSessionCookie(context.getRequest(), response);
         }
      }
   }

   private void clearSessionCookie(HttpRequest request, HttpResponse response) {
      SameSite sameSite = this.config.getSessionSameSite();
      String cookieValue = this.config.getSessionCookieName() + "=; Path=/; HttpOnly; Max-Age=0; SameSite=" + sameSite.headerValue();
      if (isSecure(request, sameSite)) {
         cookieValue = cookieValue + "; Secure";
      }

      appendSetCookie(response, cookieValue);
   }

   private static boolean isSecure(HttpRequest request, SameSite sameSite) {
      if (sameSite == SameSite.NONE) {
         return true;
      } else {
         String urlBase = request == null ? null : request.getUrlBase();
         return urlBase != null && urlBase.regionMatches(true, 0, "https:", 0, 6);
      }
   }

   private static void appendSetCookie(HttpResponse response, String cookieValue) {
      Map<String, List<String>> headers = response.getHeaders();
      List<String> existing = headers == null ? null : headers.get("Set-Cookie");
      if (existing != null && !existing.isEmpty()) {
         List<String> merged = new ArrayList<>(existing);
         merged.add(cookieValue);
         response.setHeader("Set-Cookie", merged);
      } else {
         response.setHeader("Set-Cookie", cookieValue);
      }
   }

   private HttpResponse handleStatic(HttpRequest request, HttpResponse response) {
      String path = request.getPath();
      if (!PathSecurity.isSafe(path)) {
         return this.forbidden(response, "Invalid path");
      } else {
         String resourcePath = path.startsWith("/") ? path.substring(1) : path;

         try {
            Resource resource = this.resolver.resolve(resourcePath, null);
            if (resource == null) {
               return this.notFound(response, path);
            } else {
               byte[] content = FileUtils.toBytes(resource.getStream());
               ResourceType resourceType = ResourceType.fromFileExtension(resourcePath);
               if (resourceType != null) {
                  response.setBody(content, resourceType);
               } else {
                  response.setBody(content, ResourceType.BINARY);
                  response.setHeader("Content-Type", this.getContentType(resourcePath));
               }

               if (this.config.isDevMode()) {
                  response.setHeader("Cache-Control", "no-cache, no-store");
               } else {
                  response.setHeader("Cache-Control", "public, max-age=86400");
               }

               return response;
            }
         } catch (Exception var8) {
            return this.notFound(response, path);
         }
      }
   }

   private HttpResponse notFound(HttpResponse response, String path) {
      response.setStatus(404);
      response.setBody("Not Found: " + path, ResourceType.TEXT);
      return response;
   }

   private HttpResponse forbidden(HttpResponse response, String message) {
      response.setStatus(403);
      response.setBody("Forbidden: " + message, ResourceType.TEXT);
      return response;
   }

   private String getContentType(String path) {
      ResourceType type = ResourceType.fromFileExtension(path);
      if (type != null) {
         return type.contentType;
      } else if (path.endsWith(".js")) {
         return "application/javascript";
      } else if (path.endsWith(".css")) {
         return "text/css";
      } else if (path.endsWith(".html")) {
         return "text/html";
      } else if (path.endsWith(".json")) {
         return "application/json";
      } else if (path.endsWith(".png")) {
         return "image/png";
      } else if (path.endsWith(".jpg") || path.endsWith(".jpeg")) {
         return "image/jpeg";
      } else if (path.endsWith(".svg")) {
         return "image/svg+xml";
      } else if (path.endsWith(".ico")) {
         return "image/x-icon";
      } else if (path.endsWith(".woff")) {
         return "font/woff";
      } else {
         return path.endsWith(".woff2") ? "font/woff2" : "application/octet-stream";
      }
   }

   public ServerConfig getConfig() {
      return this.config;
   }

   public static class RedirectException extends RuntimeException {
      private final String location;

      public RedirectException(String location) {
         super("Redirect to: " + location);
         this.location = location;
      }

      public String getLocation() {
         return this.location;
      }
   }
}
