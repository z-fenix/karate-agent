package io.karatelabs.http;

import io.karatelabs.common.Resource;
import io.karatelabs.common.ResourceNotFoundException;
import io.karatelabs.common.ResourceType;
import io.karatelabs.js.Engine;
import io.karatelabs.js.FlowControlSignal;
import io.karatelabs.js.JsLazy;
import io.karatelabs.markup.Markup;
import io.karatelabs.markup.ResourceResolver;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ServerRequestCycle {
   private static final Logger logger = LoggerFactory.getLogger(ServerRequestCycle.class);
   private static final ThreadLocal<ServerRequestCycle> THREAD_LOCAL = new ThreadLocal<>();
   private final Engine engine;
   private final HttpRequest request;
   private final HttpResponse response;
   private final ServerMarkupContext context;
   private final ServerConfig config;
   private final ResourceResolver resolver;
   private final Markup markup;

   public static ServerRequestCycle get() {
      return THREAD_LOCAL.get();
   }

   public static ServerRequestCycle init(ServerConfig config, ServerMarkupContext context, ResourceResolver resolver, Markup markup) {
      ServerRequestCycle rc = new ServerRequestCycle(config, context, resolver, markup);
      THREAD_LOCAL.set(rc);
      return rc;
   }

   private ServerRequestCycle(ServerConfig config, ServerMarkupContext context, ResourceResolver resolver, Markup markup) {
      this.config = config;
      this.request = context.getRequest();
      this.response = context.getResponse();
      this.context = context;
      this.resolver = resolver;
      this.markup = markup;
      this.engine = this.createEngine();
   }

   private Engine createEngine() {
      Engine engine = new Engine();
      if (this.config.getGlobalVariables() != null) {
         this.config.getGlobalVariables().forEach(engine::putRootBinding);
      }

      engine.putRootBinding("request", this.request);
      engine.putRootBinding("response", this.response);
      engine.putRootBinding("context", this.context);
      engine.putRootBinding("session", (JsLazy) this.context::getSession);
      this.context.setEngine(engine);
      return engine;
   }

   public HttpResponse handle() {
      HttpResponse var3;
      try {
         String path = this.request.getPath();
         if (this.config.isApiPath(path)) {
            return this.handleApi();
         }

         String routedJs = this.resolveJsRoute(path);
         if (routedJs == null) {
            return this.handleTemplate();
         }

         var3 = this.handleJs(routedJs, path);
      } catch (Exception var7) {
         return this.handleError(var7);
      } finally {
         this.close();
      }

      return var3;
   }

   private HttpResponse handleApi() {
      String path = this.request.getPath();
      if (!PathSecurity.isSafe(path)) {
         return this.forbidden("Invalid path");
      } else {
         String apiPrefix = this.config.getApiPrefix();
         String afterPrefix = path.substring(apiPrefix.length());
         if (afterPrefix.endsWith(".js")) {
            afterPrefix = afterPrefix.substring(0, afterPrefix.length() - 3);
         }

         int slashPos = afterPrefix.indexOf(47);
         String jsPath;
         if (slashPos != -1) {
            jsPath = apiPrefix.substring(1) + afterPrefix.substring(0, slashPos) + ".js";
         } else {
            jsPath = apiPrefix.substring(1) + afterPrefix + ".js";
         }

         this.request.setPath("/" + afterPrefix);
         return this.handleJs(jsPath, path);
      }
   }

   private HttpResponse handleJs(String jsPath, String originalPath) {
      if (!PathSecurity.isSafe(originalPath)) {
         return this.forbidden("Invalid path");
      } else {
         try {
            Resource resource;
            try {
               resource = this.resolver.resolve(jsPath, null);
            } catch (RuntimeException var6) {
               return this.notFound(originalPath);
            }

            if (resource == null) {
               return this.notFound(originalPath);
            } else {
               String jsCode = resource.getText();
               this.engine.eval(jsCode);
               if (this.response.getHeader("Content-Type") == null) {
                  Object body = this.response.getBody();
                  if (body instanceof Map || body instanceof List) {
                     this.response.setHeader("Content-Type", "application/json");
                  }
               }

               return this.response;
            }
         } catch (Exception var7) {
            return containsFlowControlSignal(var7) ? this.response : this.handleError(var7);
         }
      }
   }

   private String resolveJsRoute(String path) {
      LinkedHashMap<String, String> routes = this.config.getJsRoutes();
      if (routes == null) {
         return null;
      } else {
         for (Entry<String, String> entry : routes.entrySet()) {
            if (this.request.pathMatches(entry.getKey())) {
               return entry.getValue();
            }
         }

         return null;
      }
   }

   private HttpResponse handleTemplate() {
      String path = this.request.getPath();
      if (!PathSecurity.isSafe(path)) {
         return this.forbidden("Invalid path");
      } else {
         String templatePath = this.resolveTemplateRoute(path);
         if (templatePath == null) {
            templatePath = path.equals("/") ? "index.html" : path.substring(1);
            if (!templatePath.endsWith(".html")) {
               templatePath = templatePath + ".html";
            }

            if (!this.resourceExists(templatePath)) {
               String indexVariant = templatePath.substring(0, templatePath.length() - ".html".length()) + "/index.html";
               if (this.resourceExists(indexVariant)) {
                  templatePath = indexVariant;
               }
            }
         }

         try {
            Map<String, Object> vars = this.context.toVars();
            String contentHtml = this.renderContent(templatePath, vars);
            if (this.context.hasRedirect()) {
               return this.response;
            } else {
               String contentTemplate = this.context.isSwitched() ? this.context.getSwitchTemplate() : templatePath;
               String shellTemplate = this.config.getShellTemplate();
               boolean wrapWithShell = shellTemplate != null
                  && !this.request.isAjax()
                  && !this.config.isRawPath(path)
                  && !contentTemplate.equals(shellTemplate);
               String html;
               if (!wrapWithShell) {
                  html = contentHtml;
               } else {
                  String shellWrappedContent = contentHtml;
                  if (this.config.isDevMode() && this.config.isDevTrace()) {
                     String safe = contentTemplate == null ? "" : contentTemplate.replace("-->", "--&gt;");
                     shellWrappedContent = "<!-- ka:fragment-begin (shell) "
                        + safe
                        + " depth=0 -->\n"
                        + contentHtml
                        + "\n<!-- ka:fragment-end (shell) "
                        + safe
                        + " depth=0 -->";
                  }

                  vars.put("content", shellWrappedContent);
                  vars.put("contentTemplate", contentTemplate);
                  this.context.setTemplateName(shellTemplate);

                  try {
                     html = this.markup.processPath(shellTemplate, vars);
                  } catch (RuntimeException var11) {
                     if (!containsFlowControlSignal(var11)) {
                        throw var11;
                     }

                     html = "";
                  }
               }

               this.response.setBody(html, ResourceType.HTML);
               return this.response;
            }
         } catch (ResourceNotFoundException var12) {
            return this.notFound(path);
         } catch (Exception var13) {
            return this.handleError(var13);
         }
      }
   }

   private String renderContent(String templatePath, Map<String, Object> vars) {
      this.context.setTemplateName(templatePath);

      String html;
      try {
         html = this.markup.processPath(templatePath, vars);
      } catch (RuntimeException var5) {
         if (!containsFlowControlSignal(var5)) {
            throw var5;
         }

         html = "";
      }

      if (this.context.isSwitched()) {
         String newTemplate = this.context.getSwitchTemplate();
         this.context.setTemplateName(newTemplate);
         html = this.markup.processPath(newTemplate, vars);
      }

      return html;
   }

   private boolean resourceExists(String path) {
      try {
         Resource r = this.resolver.resolve(path, null);
         return r != null && r.exists();
      } catch (RuntimeException var3) {
         return false;
      }
   }

   private static boolean containsFlowControlSignal(Throwable e) {
      for (Throwable t = e; t != null; t = t.getCause()) {
         if (t instanceof FlowControlSignal) {
            return true;
         }
      }

      return false;
   }

   private HttpResponse handleError(Exception e) {
      logger.error("request handler failed: {} {}", new Object[]{this.request.getMethod(), this.request.getPath(), e});
      this.response.setStatus(500);
      if (this.config.getErrorTemplate500() != null) {
         try {
            Map<String, Object> vars = this.context.toVars();
            vars.put("error", e.getMessage());
            String html = this.markup.processPath(this.config.getErrorTemplate500(), vars);
            this.response.setBody(html, ResourceType.HTML);
            return this.response;
         } catch (Exception var4) {
         }
      }

      String body;
      if (this.config.isDevMode()) {
         String detail = e.getMessage();
         body = detail != null ? "Internal Server Error\n" + detail : "Internal Server Error";
      } else {
         body = "Internal Server Error";
      }

      this.response.setBody(body, ResourceType.TEXT);
      return this.response;
   }

   private String resolveTemplateRoute(String path) {
      LinkedHashMap<String, String> routes = this.config.getTemplateRoutes();
      if (routes == null) {
         return null;
      } else {
         for (Entry<String, String> entry : routes.entrySet()) {
            if (this.request.pathMatches(entry.getKey())) {
               return entry.getValue();
            }
         }

         return null;
      }
   }

   private HttpResponse notFound(String path) {
      this.response.setStatus(404);
      if (this.config.getErrorTemplate404() != null) {
         try {
            Map<String, Object> vars = this.context.toVars();
            vars.put("path", path);
            String html = this.markup.processPath(this.config.getErrorTemplate404(), vars);
            this.response.setBody(html, ResourceType.HTML);
            return this.response;
         } catch (Exception var4) {
         }
      }

      this.response.setBody("Not Found: " + path, ResourceType.TEXT);
      return this.response;
   }

   private HttpResponse forbidden(String message) {
      this.response.setStatus(403);
      this.response.setBody("Forbidden: " + message, ResourceType.TEXT);
      return this.response;
   }

   private void close() {
      THREAD_LOCAL.remove();
   }

   public Engine getEngine() {
      return this.engine;
   }

   public HttpRequest getRequest() {
      return this.request;
   }

   public HttpResponse getResponse() {
      return this.response;
   }

   public ServerMarkupContext getContext() {
      return this.context;
   }

   public ServerConfig getConfig() {
      return this.config;
   }
}
