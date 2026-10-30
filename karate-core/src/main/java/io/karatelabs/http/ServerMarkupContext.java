
package io.karatelabs.http;

import io.karatelabs.js.JavaInvokable;

import io.karatelabs.common.FileUtils;
import io.karatelabs.common.Json;
import io.karatelabs.common.Resource;
import io.karatelabs.js.Engine;
import io.karatelabs.js.JsLazy;
import io.karatelabs.js.ObjectLike;
import io.karatelabs.markup.ActionDispatchHost;
import io.karatelabs.markup.MarkupContext;
import io.karatelabs.markup.MarkupScope;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;

public class ServerMarkupContext implements MarkupContext, ActionDispatchHost {
   public static final String FLASH_SESSION_KEY = "_flash";
   private final HttpRequest request;
   private final HttpResponse response;
   private final ServerConfig config;
   private Session session;
   private Function<String, Resource> resourceResolver;
   private String templateName;
   private String callerTemplateName;
   private boolean closed = false;
   private boolean switched = false;
   private String switchTemplate;
   private String redirectPath;
   private final List<String> logMessages = new ArrayList<>();
   private final Map<String, Object> flash = new HashMap<>();
   private final Map<String, Object> actions = new HashMap<>();
   private Engine engine;
   private boolean actionDispatched = false;
   private Consumer<String> eagerDispatchHook;
   private MarkupScope markupScope;

   @Override
   public void setMarkupScope(MarkupScope scope) {
      this.markupScope = scope;
   }

   @Override
   public MarkupScope getMarkupScope() {
      return this.markupScope;
   }

   public ServerMarkupContext(HttpRequest request, HttpResponse response, ServerConfig config) {
      this.request = request;
      this.response = response;
      this.config = config;
   }

   public Map<String, Object> toVars() {
      Map<String, Object> vars = new HashMap<>();
      if (this.config.getGlobalVariables() != null) {
         vars.putAll(this.config.getGlobalVariables());
      }

      vars.put("request", this.request);
      vars.put("response", this.response);
      vars.put("context", this);
      if (this.config.isSessionEnabled()) {
         vars.put("session", (JsLazy)() -> this.session);
      } else {
         vars.put("session", ServerMarkupContext.SessionUnavailableProxy.INSTANCE);
      }

      return vars;
   }

   @Override
   public String getTemplateName() {
      return this.templateName;
   }

   @Override
   public String getCallerTemplateName() {
      return this.callerTemplateName;
   }

   @Override
   public String read(String path) {
      if (this.resourceResolver == null) {
         throw new RuntimeException("Resource resolver not configured");
      } else {
         Resource resource = this.resourceResolver.apply(path);
         return resource.getText();
      }
   }

   @Override
   public byte[] readBytes(String path) {
      if (this.resourceResolver == null) {
         throw new RuntimeException("Resource resolver not configured");
      } else {
         Resource resource = this.resourceResolver.apply(path);
         return FileUtils.toBytes(resource.getStream());
      }
   }

   @Override
   public String toJson(Object obj) {
      return Json.stringifyStrict(obj);
   }

   @Override
   public Object fromJson(String json) {
      return Json.of(json).value();
   }

   @Override
   public Object getContextSession() {
      return this.session;
   }

   public void redirect(String path) {
      this.redirectPath = path;
      throw new TemplateFlowSignal(TemplateFlowSignal.Kind.REDIRECT);
   }

   public void log(Object... args) {
      StringBuilder sb = new StringBuilder();

      for (int i = 0; i < args.length; i++) {
         if (i > 0) {
            sb.append(" ");
         }

         sb.append(args[i]);
      }

      String message = sb.toString();
      this.logMessages.add(message);
      if (this.config != null && this.config.getLogHandler() != null) {
         this.config.getLogHandler().accept(message);
      } else {
         System.out.println("[ServerMarkupContext] " + message);
      }
   }

   public String uuid() {
      return UUID.randomUUID().toString();
   }

   public void init() {
      if (this.config != null && this.config.isSessionEnabled()) {
         this.session = this.config.createSession();
         this.closed = false;
      }
   }

   public void close() {
      if (this.session != null && this.config != null && this.config.getSessionStore() != null) {
         this.config.getSessionStore().delete(this.session.getId());
      }

      this.session = null;
      this.closed = true;
   }

   public void switchTemplate(String template) {
      this.switchTemplate = template;
      this.switched = true;
      throw new TemplateFlowSignal(TemplateFlowSignal.Kind.SWITCH);
   }

   public void setGlobal(String name, Object value) {
      if (this.engine != null) {
         this.engine.put(name, value);
      }
   }

   public Object getGlobal(String name, Object defaultValue) {
      if (this.engine != null) {
         Object v = this.engine.get(name);
         if (v != null) {
            return v;
         }
      }

      return defaultValue;
   }

   @Override
   public Object jsGet(String key) {
      Object baseResult = MarkupContext.super.jsGet(key);
      if (baseResult != null) {
         return baseResult;
      } else {
         return switch (key) {
            case "redirect" -> (JavaInvokable) args -> {
               if (args.length == 0) {
                  throw new RuntimeException("redirect() requires a URL argument");
               } else {
                  this.redirect(args[0].toString());
                  return null;
               }
            };
            case "log" -> (JavaInvokable) args -> {
               this.log(args);
               return null;
            };
            case "uuid" -> (JavaInvokable) args -> this.uuid();
            case "init" -> (JavaInvokable) args -> {
               this.init();
               return this.session;
            };
            case "close" -> (JavaInvokable) args -> {
               this.close();
               return null;
            };
            case "switch" -> (JavaInvokable) args -> {
               if (args.length == 0) {
                  throw new RuntimeException("switch() requires a template argument");
               } else {
                  this.switchTemplate(args[0].toString());
                  return null;
               }
            };
            case "setGlobal" -> (JavaInvokable) args -> {
               if (args.length != 0 && args[0] != null) {
                  String name = args[0].toString();
                  Object value = args.length > 1 ? args[1] : null;
                  this.setGlobal(name, value);
                  return null;
               } else {
                  throw new RuntimeException("context.setGlobal() requires a name argument");
               }
            };
            case "getGlobal" -> (JavaInvokable) args -> {
               if (args.length != 0 && args[0] != null) {
                  String name = args[0].toString();
                  Object defaultValue = args.length > 1 ? args[1] : null;
                  return this.getGlobal(name, defaultValue);
               } else {
                  throw new RuntimeException("context.getGlobal() requires a name argument");
               }
            };
            case "ajax" -> this.isAjax();
            case "sessionId" -> this.session != null ? this.session.getId() : null;
            case "closed" -> this.isClosed();
            case "switched" -> this.switched;
            case "flash" -> this.flash;
            case "actions" -> this.actionsView();
            case "request" -> this.request;
            case "response" -> this.response;
            case "session" -> this.session;
            case "csrf" -> this.getCsrfToken();
            default -> null;
         };
      }
   }

   public HttpRequest getRequest() {
      return this.request;
   }

   public HttpResponse getResponse() {
      return this.response;
   }

   public ServerConfig getConfig() {
      return this.config;
   }

   public Session getSession() {
      return this.session;
   }

   public void setSession(Session session) {
      this.session = session;
      this.closed = session == null;
   }

   public void setTemplateName(String templateName) {
      this.templateName = templateName;
   }

   public void setCallerTemplateName(String callerTemplateName) {
      this.callerTemplateName = callerTemplateName;
   }

   public void setResourceResolver(Function<String, Resource> resourceResolver) {
      this.resourceResolver = resourceResolver;
   }

   public void setEngine(Engine engine) {
      this.engine = engine;
   }

   public Engine getEngine() {
      return this.engine;
   }

   public String getRedirectPath() {
      return this.redirectPath;
   }

   public boolean hasRedirect() {
      return this.redirectPath != null;
   }

   public boolean isClosed() {
      return this.closed || this.session == null || this.session.isTemporary();
   }

   public boolean wasExplicitlyClosed() {
      return this.closed;
   }

   public boolean isSwitched() {
      return this.switched;
   }

   public String getSwitchTemplate() {
      return this.switchTemplate;
   }

   public List<String> getLogMessages() {
      return this.logMessages;
   }

   public Map<String, Object> getFlash() {
      return this.flash;
   }

   @Override
   public Map<String, Object> getActions() {
      return this.actions;
   }

   @Override
   public boolean isActionDispatched() {
      return this.actionDispatched;
   }

   @Override
   public void markActionDispatched() {
      this.actionDispatched = true;
   }

   @Override
   public void setEagerDispatchHook(Consumer<String> hook) {
      this.eagerDispatchHook = hook;
   }

   private ObjectLike actionsView() {
      return new ObjectLike() {
         @Override
         public Object getMember(String name) {
            return ServerMarkupContext.this.actions.get(name);
         }

         @Override
         public void putMember(String name, Object value) {
            ServerMarkupContext.this.actions.put(name, value);
            if (ServerMarkupContext.this.eagerDispatchHook != null) {
               ServerMarkupContext.this.eagerDispatchHook.accept(name);
            }
         }

         @Override
         public void removeMember(String name) {
            ServerMarkupContext.this.actions.remove(name);
         }

         @Override
         public Map<String, Object> toMap() {
            return new LinkedHashMap<>(ServerMarkupContext.this.actions);
         }
      };
   }

   public void loadFlashFromSession() {
      if (this.session != null) {
         Object stored = this.session.getMember("_flash");
         if (stored instanceof Map) {
            this.flash.putAll((Map<? extends String, ? extends Object>)stored);
            this.session.removeMember("_flash");
         }
      }
   }

   public void persistFlashToSession() {
      if (this.session != null && !this.flash.isEmpty()) {
         this.session.putMember("_flash", new HashMap<>(this.flash));
      }
   }

   public boolean isAjax() {
      if (this.request == null) {
         return false;
      } else {
         String hxRequest = this.request.getHeader("HX-Request");
         String xhrHeader = this.request.getHeader("X-Requested-With");
         return hxRequest != null || "XMLHttpRequest".equals(xhrHeader);
      }
   }

   public CsrfProtection.CsrfToken getCsrfToken() {
      return this.config != null && this.config.isCsrfEnabled() ? CsrfProtection.createTemplateToken(this.session) : null;
   }

   private static final class SessionUnavailableProxy implements ObjectLike {
      static final ServerMarkupContext.SessionUnavailableProxy INSTANCE = new ServerMarkupContext.SessionUnavailableProxy();

      @Override
      public Object getMember(String name) {
         throw sessionUnavailable(name, false);
      }

      @Override
      public void putMember(String name, Object value) {
         throw sessionUnavailable(name, true);
      }

      @Override
      public void removeMember(String name) {
         throw sessionUnavailable(name, true);
      }

      @Override
      public Map<String, Object> toMap() {
         throw sessionUnavailable("(toMap)", false);
      }

      private static RuntimeException sessionUnavailable(String name, boolean write) {
         return new RuntimeException(
            "session is unavailable: no sessionStore is configured. Call ServerConfig.sessionStore(...) at app startup to enable sessions. Attempted to "
               + (write ? "write" : "read")
               + " session."
               + name
         );
      }
   }
}
