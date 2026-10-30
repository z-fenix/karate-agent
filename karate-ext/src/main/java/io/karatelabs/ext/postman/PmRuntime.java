package io.karatelabs.ext.postman;

import io.karatelabs.common.FileUtils;
import io.karatelabs.common.Json;
import io.karatelabs.common.Resource;
import io.karatelabs.core.KarateJs;
import io.karatelabs.http.AuthHandler;
import io.karatelabs.http.AuthorizationCodeAuthHandler;
import io.karatelabs.http.BasicAuthHandler;
import io.karatelabs.http.BearerAuthHandler;
import io.karatelabs.http.ClientCredentialsAuthHandler;
import io.karatelabs.http.HttpRequest;
import io.karatelabs.http.HttpResponse;
import io.karatelabs.http.OAuth2BrowserCallback;
import io.karatelabs.js.Context;
import io.karatelabs.js.ContextListener;
import io.karatelabs.js.ContextScope;
import io.karatelabs.js.Event;
import io.karatelabs.js.EventType;
import io.karatelabs.js.JavaCallable;
import io.karatelabs.js.JsLazy;
import io.karatelabs.js.SimpleObject;
import io.karatelabs.js.Terms;
import io.karatelabs.markup.RootResourceResolver;
import io.karatelabs.match.Result;
import io.karatelabs.parser.Node;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Map.Entry;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PmRuntime implements SimpleObject {
   private static final Logger logger = LoggerFactory.getLogger(PmRuntime.class);
   private final String KARATE_COLON = "karate:";
   final Resource root;
   final KarateJs karate;
   final PostmanVariableScope _globals = new PostmanVariableScope("pm.globals", null);
   final PostmanVariableScope _collectionVariables = new PostmanVariableScope("pm.collectionVariables", this._globals);
   final PostmanVariableScope _environmentVariables = new PostmanVariableScope("pm.environment", this._collectionVariables);
   final PostmanVariableScope _dataVariables = new PostmanVariableScope("pm.iterationData", this._environmentVariables);
   final PostmanVariableScope _variables = new PostmanVariableScope("pm.variables", this._dataVariables);
   final BiConsumer<Context, Result> onMatch;
   final SimpleObject tests;
   final SimpleObject responseCode;
   final SimpleObject postman;
   Supplier<List<Map<String, Object>>> environmentVariablesSupplier;
   Consumer<String> htmlConsumer;
   Runnable cancelCallback;
   BiConsumer<PostmanCollectionNode, PostmanRequestRunResult> responseCallback;
   OAuth2BrowserCallback oauth2BrowserCallback;
   private final Map<String, AuthHandler> authHandlerCache = new HashMap<>();
   String _eventName;
   PostmanCollectionNode _item;
   HttpRequest _request;
   HttpResponse _response;
   PostmanRequestRunResult _executionResult;
   boolean stop;
   boolean skip;
   private Node _currentStatementNode;
   int iteration = -1;
   List<Map<String, Object>> iterationSource;
   final SimpleObject iterationData = object("pm.iterationData", var1x -> {
      if ("get".equals(var1x)) {
         return (JavaCallable)(var1xx, var2) -> {
            var var3 = this.getIterationRow();
            return var3 == null ? null : var3.get(var2[0] + "");
         };
      } else if ("toJSON".equals(var1x)) {
         return (JavaCallable)(var1xx, var2) -> this.getIterationRow();
      } else {
         throw new UnsupportedPostmanFeatureException("pm.iterationData." + var1x);
      }
   });
   final SimpleObject info = object("pm.info", var1x -> {
      return switch (var1x) {
         case "eventName" -> this._eventName;
         case "iteration" -> this.iteration;
         case "iterationCount" -> this.iterationSource == null ? 0 : this.iterationSource.size();
         case "requestName" -> this._item.d();
         case "requestId" -> this._item.e();
         default -> throw new UnsupportedPostmanFeatureException("pm.info." + var1x);
      };
   }, "eventName", "iteration", "iterationCount", "requestName", "requestId");
   final SimpleObject executionLocation = object("pm.execution.location", var1x -> {
      return switch (var1x) {
         case "folder" -> {
            PostmanCollectionNode var4 = this._item.c;
            yield var4 != null ? var4.d() : null;
         }
         case "request" -> this._item.d();
         case "iteration" -> this.iteration;
         default -> throw new UnsupportedPostmanFeatureException("pm.execution.location." + var1x);
      };
   }, "folder", "request", "iteration");
   final SimpleObject execution = object("pm.execution", var1x -> {
      return switch (var1x) {
         case "skipRequest" -> (JavaCallable) (var1xx, var2) -> this.skip = true;
         case "location" -> this.executionLocation;
         case "getCurrentRequest" -> (JavaCallable) (var1xx, var2) -> new PmRequest(this, this._item);
         default -> throw new UnsupportedPostmanFeatureException("pm.execution." + var1x);
      };
   });
   boolean _testFailed;
   String _testMessage;
   JavaCallable test = (var1x, var2) -> {
      String var3 = var2[0] + "";
      JavaCallable var4 = (JavaCallable)var2[1];
      this.onTest(var3);
      this._testFailed = false;
      this._testMessage = null;
      UnsupportedPostmanFeatureException var5 = null;

      try {
         var4.call(null);
      } catch (Exception var7) {
         var5 = io.karatelabs.ext.postman.UnsupportedPostmanFeatureException.a(var7);
         this._testFailed = true;
         this._testMessage = var5 == null ? var7.getMessage() : var5.getMessage();
         this.htmlError(this._testMessage);
      }

      this.recordTest(var3, !this._testFailed, this._testMessage, var5 != null);
      return null;
   };
   JavaCallable sendRequest = (var1x, var2) -> {
      Json var3 = Json.object();
      Map var4 = (Map)var2[0];
      if (var4.get("header") instanceof Map var6) {
         ArrayList var7 = new ArrayList();
         var4.put("header", var7);

         for (Entry var9 : (Iterable<Entry>) (Iterable<?>) (var6.entrySet())) {
            HashMap var10 = new HashMap();
            var7.add(var10);
            var10.put("key", var9.getKey());
            var10.put("value", var9.getValue());
         }
      }

      var3.set("request", var4);
      PostmanCollectionNode var12 = new PostmanCollectionNode(null, var3);
      JavaCallable var13 = null;
      if (var2.length > 1 && var2[1] instanceof JavaCallable var14) {
         var13 = var14;
      }

      try {
         HttpResponse var15 = this.sendRequest(var12);
         if (var13 != null) {
            var13.call(var1x, null, new PmResponse(this, var15));
         }
      } catch (Exception var11) {
         if (var13 != null) {
            var13.call(var1x, var11.getMessage());
         }
      }

      return null;
   };
   public static final List<String> AUTH_TYPES = List.of("basic", "bearer", "apikey", "oauth2");

   public HttpResponse getResponse() {
      return this._response;
   }

   public void setEnvironmentVariablesSupplier(Supplier<List<Map<String, Object>>> var1) {
      this.environmentVariablesSupplier = var1;
   }

   public void setIterationSource(List<Map<String, Object>> var1) {
      this.iterationSource = var1;
   }

   public void setStop(boolean var1) {
      this.stop = var1;
   }

   public Resource getRoot() {
      return this.root;
   }

   static SimpleObject object(final String var0, final Function<String, Object> var1, final String... var2) {
      return new SimpleObject() {
         @Override
         public Object jsGet(String var1x) {
            return var1.apply(var1x);
         }

         @Override
         public Collection<String> jsKeys() {
            return Arrays.asList(var2);
         }

         @Override
         public void putMember(String var1x, Object var2x) {
            throw new UnsupportedPostmanFeatureException(var0 + "." + var1x + " =");
         }
      };
   }

   public PmRuntime(final Resource var1) {
      this.root = var1;
      this.karate = new KarateJs(var1);
      this._globals.a(this);
      this._collectionVariables.a(this);
      this._environmentVariables.a(this);
      this._dataVariables.a(this);
      this._variables.a(this);
      this.karate.setResourceResolver(new RootResourceResolver(var1.getPrefixedPath()) {
         @Override
         public Resource resolve(String var1x, Resource var2) {
            if (var1x.startsWith("karate:")) {
               var1x = var1x.substring("karate:".length());
               if (!var1x.endsWith(".html")) {
                  var1x = var1x + ".html";
               }

               URL var3 = io.karatelabs.ext.postman.PostmanResourceLoader.b("html/" + var1x);
               return Resource.from(var3, var1.getPath());
            } else {
               return super.resolve(var1x, var2);
            }
         }
      });
      this.karate.engine.setListener(new ContextListener() {
         @Override
         public void onEvent(Event var1) {
            if (var1.type == EventType.STATEMENT_ENTER) {
               PmRuntime.this._currentStatementNode = var1.node;
            }
         }
      });
      this.onMatch = (var1x, var2) -> {
         if (!var2.pass) {
            if (!this._testFailed) {
               this._testMessage = var2.message;
            }

            this._testFailed = true;
            if (this._executionResult != null) {
               this._executionResult.d();
            }
         }

         if (this.htmlConsumer != null) {
            HashMap var3 = new HashMap();
            var3.put("result", var2.toMap());
            Node var4;
            if (var1x.getScope() == ContextScope.ROOT) {
               var4 = this._currentStatementNode;
            } else {
               var4 = var1x.getNode() != null ? var1x.getNode().getParent() : null;
            }

            String var5 = var4 != null ? var4.getTextIncludingWhitespace() : "";
            var3.put("source", var5);
            String var6 = this.karate.markup().processPath("karate:match", var3);
            this.htmlConsumer.accept(var6);
            if (this._executionResult != null) {
               this._executionResult.a(var6);
            }
         }
      };
      this.karate.engine.setOnConsoleLog(var1x -> {
         if (this.htmlConsumer != null) {
            Map<String, Object> var2 = Collections.singletonMap("log", var1x);
            String var3 = this.karate.markup().processPath("karate:console", var2);
            this.htmlConsumer.accept(var3);
            if (this._executionResult != null) {
               this._executionResult.a(var3);
            }
         }

         logger.debug(var1x);
      });
      this.tests = new SimpleObject() {
         @Override
         public Object jsGet(String var1) {
            return null;
         }

         @Override
         public void putMember(String var1, Object var2) {
            Node var3 = PmRuntime.this._currentStatementNode;
            PmRuntime.this.onTest(var1);
            Result var4 = Terms.isTruthy(var2) ? Result.PASS : Result.fail("assertion failed");
            PmRuntime.this.recordTest(var1, var4.pass, var4.message, false);
            PmRuntime.this._currentStatementNode = var3;
            PmRuntime.this.onMatch.accept(PmRuntime.this.karate.engine.getRootContext(), var4);
         }
      };
      this.responseCode = object("responseCode", var1x -> {
         if ("code".equals(var1x)) {
            return this._response.getStatus();
         } else if ("name".equals(var1x)) {
            return this._response.getStatusText();
         } else {
            throw new UnsupportedPostmanFeatureException("responseCode." + var1x);
         }
      }, "code", "name");
      this.postman = object("postman", var1x -> {
         return switch (var1x) {
            case "setEnvironmentVariable" -> (JavaCallable) (var1xx, var2) -> {
               this._environmentVariables.putMember(var2[0] + "", var2[1]);
               return null;
            };
            case "getEnvironmentVariable" -> (JavaCallable) (var1xx, var2) -> this._environmentVariables.getMember(var2[0] + "");
            case "setGlobalVariable" -> (JavaCallable) (var1xx, var2) -> {
               this._globals.putMember(var2[0] + "", var2[1]);
               return null;
            };
            case "getGlobalVariable" -> (JavaCallable) (var1xx, var2) -> this._globals.b(var2[0] + "");
            case "clearEnvironmentVariable" -> (JavaCallable) (var1xx, var2) -> {
               this._environmentVariables.removeMember(var2[0] + "");
               return null;
            };
            case "clearGlobalVariable" -> (JavaCallable) (var1xx, var2) -> {
               this._globals.removeMember(var2[0] + "");
               return null;
            };
            default -> throw new UnsupportedPostmanFeatureException("postman." + var1x);
         };
      });
      this.karate.engine.put("pm", this);
      this.karate.engine.put("tests", this.tests);
      this.karate.engine.put("responseCode", this.responseCode);
      this.karate.engine.put("postman", this.postman);
      this.karate.engine.put("request", (JsLazy)() -> new PmRequest(this, this._item));
      this.karate.engine.put("response", (JsLazy)() -> this.responseCode);
      this.karate.engine.put("responseTime", (JsLazy)() -> this._response.getResponseTime());
      this.karate.engine.put("responseBody", (JsLazy)() -> this._response.getBodyString());
   }

   @Override
   public Object jsGet(String var1) {
      return switch (var1) {
         case "test" -> this.test;
         case "expect" -> (io.karatelabs.js.JavaCallable) (var1x, var2) -> new PmExpect(this, var2[0]);
         case "request" -> new PmRequest(this, this._item);
         case "response" -> new PmResponse(this, this._response);
         case "collectionVariables" -> this._collectionVariables;
         case "globals" -> this._globals;
         case "environment" -> this._environmentVariables;
         case "variables" -> this._variables;
         case "sendRequest" -> this.sendRequest;
         case "info" -> this.info;
         case "execution" -> this.execution;
         case "iterationData" -> this.iterationData;
         default -> throw new UnsupportedPostmanFeatureException("pm." + var1);
      };
   }

   @Override
   public void putMember(String var1, Object var2) {
      throw new UnsupportedPostmanFeatureException("pm." + var1 + " =");
   }

   private Map<String, Object> getIterationRow() {
      if (this.iterationSource != null && this.iteration != -1) {
         return this.iteration < this.iterationSource.size() ? this.iterationSource.get(this.iteration) : null;
      } else {
         return null;
      }
   }

   void onTest(String var1) {
      if (this.htmlConsumer != null) {
         Map<String, Object> var2 = Collections.singletonMap("label", var1);
         String var3 = this.karate.markup().processPath("karate:pm-test", var2);
         this.htmlConsumer.accept(var3);
      }
   }

   private void recordTest(String var1, boolean var2, String var3, boolean var4) {
      if (this._executionResult != null) {
         this._executionResult.k().add(new PostmanTestResult(var1, var2, var2 ? null : var3, var4));
         if (var2) {
            this._executionResult.i();
         } else {
            this._executionResult.j();
            this._executionResult.d();
         }
      }
   }

   private void onUnsupported(UnsupportedPostmanFeatureException var1) {
      if (this._executionResult != null) {
         this._executionResult.l().add(var1.a);
         this._executionResult.d();
      }

      this.htmlError(var1.getMessage());
   }

   public void setHtmlConsumer(Consumer<String> var1) {
      this.htmlConsumer = var1;
   }

   public void setOAuth2BrowserCallback(OAuth2BrowserCallback var1) {
      this.oauth2BrowserCallback = var1;
   }

   public void setCancelCallback(Runnable var1) {
      this.cancelCallback = var1;
   }

   public void setResponseCallback(BiConsumer<PostmanCollectionNode, PostmanRequestRunResult> var1) {
      this.responseCallback = var1;
   }

   public void cancel() {
      this.stop = true;
      this.karate.client.abort();
      if (this.cancelCallback != null) {
         this.cancelCallback.run();
      }
   }

   public void setEnvironmentVariables(List<Map<String, Object>> var1) {
      this.setVariables("environment", var1);
   }

   public void setCollectionVariable(String var1, Object var2) {
      this._collectionVariables.putMember(var1, var2);
   }

   public List<Map<String, Object>> getEnvironmentVariables() {
      return this._environmentVariables.toMap().entrySet().stream().map(var0 -> {
         Map<String, Object> var1 = new HashMap<>();
         var1.put("key", var0.getKey());
         var1.put("value", var0.getValue());
         var1.put("disabled", false);
         return var1;
      }).toList();
   }

   private void setVariables(String var1, List<Map<String, Object>> var2) {
      PostmanVariableScope var3 = switch (var1) {
         case "collection" -> this._collectionVariables;
         case "environment" -> this._environmentVariables;
         default -> this._variables;
      };
      var3.a();

      for (Map var8 : var2) {
         String var6 = (String)var8.get("key");
         Object var7 = var8.get("value");
         var3.putMember(var6, var7);
      }
   }

   public void initCollectionVariables(PostmanCollectionNode var1) {
      if (var1 != null) {
         for (Map var3 : var1.c().G()) {
            String var4 = (String)var3.get("key");
            Object var5 = var3.get("value");
            this._collectionVariables.putMember(var4, var5);
         }
      }
   }

   public void reset(PostmanCollectionNode var1) {
      this._collectionVariables.a();
      this.initCollectionVariables(var1);
      this._variables.a();
      this._globals.a();
      this.stop = false;
      this.skip = false;
      this.iteration = -1;
      this.iterationSource = null;
   }

   public void iteration(int var1, List<Map<String, Object>> var2) {
      this.iterationSource = var2;
      this.iteration = var1;
      this._dataVariables.a();
      if (var2 != null) {
         var2.get(var1).forEach(this._dataVariables::putMember);
      }
   }

   public void executeAll(PostmanCollectionNode var1) {
      if (this.environmentVariablesSupplier != null) {
         this.setEnvironmentVariables(this.environmentVariablesSupplier.get());
      }

      if (this.iterationSource != null && !this.iterationSource.isEmpty()) {
         for (int var2 = 0; var2 < this.iterationSource.size() && !this.stop; var2++) {
            this.iteration = var2;
            var var3 = this.iterationSource.get(var2);
            this._dataVariables.a();

            for (Entry var5 : (Iterable<Entry>) (Iterable<?>) (var3.entrySet())) {
               this._dataVariables.putMember((String)var5.getKey(), var5.getValue());
            }

            this.htmlInfo("Iteration " + (var2 + 1) + " of " + this.iterationSource.size());
            this.execute(var1);
         }
      } else {
         this.execute(var1);
      }
   }

   private void htmlHeading(String var1) {
      if (this.htmlConsumer != null) {
         Map<String, Object> var2 = Collections.singletonMap("text", var1);
         String var3 = this.karate.markup().processPath("karate:heading", var2);
         this.htmlConsumer.accept(var3);
         if (this._executionResult != null) {
            this._executionResult.a(var3);
         }
      }
   }

   private void htmlInfo(String var1) {
      if (this.htmlConsumer != null) {
         Map<String, Object> var2 = Collections.singletonMap("log", var1);
         String var3 = this.karate.markup().processPath("karate:console", var2);
         this.htmlConsumer.accept(var3);
         if (this._executionResult != null) {
            this._executionResult.a(var3);
         }
      }
   }

   private void htmlLine() {
      if (this.htmlConsumer != null) {
         String var1 = "<hr class=\"my-2 border border-light border-2\">";
         this.htmlConsumer.accept(var1);
         if (this._executionResult != null) {
            this._executionResult.a(var1);
         }
      }
   }

   private void htmlRequest() {
      if (this.htmlConsumer != null) {
         Map<String, Object> var1 = Collections.singletonMap("request", this._request);
         String var2 = this.karate.markup().processPath("karate:http-request.html", var1);
         this.htmlConsumer.accept(var2);
         if (this._executionResult != null) {
            this._executionResult.a(var2);
         }
      }
   }

   private void htmlResponse() {
      if (this.htmlConsumer != null) {
         Map<String, Object> var1 = Collections.singletonMap("response", this._response);
         String var2 = this.karate.markup().processPath("karate:http-response.html", var1);
         this.htmlConsumer.accept(var2);
         if (this._executionResult != null) {
            this._executionResult.a(var2);
         }
      }
   }

   private void htmlTestSummary() {
      if (this.htmlConsumer != null) {
         HashMap var1 = new HashMap();
         int var2 = this._executionResult.g();
         int var3 = this._executionResult.h();
         var1.put("passCount", var2);
         var1.put("failCount", var3);
         var1.put("totalCount", var2 + var3);
         String var4 = this.karate.markup().processPath("karate:pm-test-summary.html", var1);
         this.htmlConsumer.accept(var4);
         if (this._executionResult != null) {
            this._executionResult.a(var4);
         }
      }
   }

   private void htmlError(String var1) {
      if (this.htmlConsumer != null) {
         Map<String, Object> var2 = Collections.singletonMap("error", var1);
         String var3 = this.karate.markup().processPath("karate:error", var2);
         this.htmlConsumer.accept(var3);
         if (this._executionResult != null) {
            this._executionResult.a(var3);
         }
      }
   }

   private void execute(PostmanCollectionNode var1) {
      if (this.stop) {
         logger.debug("stop requested, exit: {}", var1.d());
         this.htmlInfo("stop requested");
      } else {
         if (var1.b()) {
            this.sendRequest(var1);
            this.htmlLine();
         }

         for (PostmanCollectionNode var3 : var1.d) {
            this.execute(var3);
         }
      }
   }

   HttpRequest buildRequest(PostmanCollectionNode var1, boolean var2) throws Exception {
      this.karate.http.reset();
      if (var2) {
         var var3 = this.collectScriptsWithInheritance(var1, "prerequest");
         var1 = new PostmanCollectionNode(var1.c, Json.of(Json.copy(var1.b.value(), true, false)));
         this._item = var1;
         this._eventName = "prerequest";

         for (io.karatelabs.ext.postman.PmRuntime.InheritedScript var5 : (Iterable<io.karatelabs.ext.postman.PmRuntime.InheritedScript>) (Iterable<?>) (var3)) {
            try {
               this.karate.engine.eval(wrapInIife(var5.b));
               logger.debug("eval done: before script for {}", var5.a.d());
            } catch (Exception var22) {
               logger.debug("before script failed for {}: {}", var5.a.d(), var22.getMessage());
               UnsupportedPostmanFeatureException var7 = io.karatelabs.ext.postman.UnsupportedPostmanFeatureException.a(var22);
               if (var7 == null) {
                  throw var22;
               }

               this.onUnsupported(var7);
            }

            if (this.skip) {
               logger.debug("skipping request");
               return null;
            }
         }
      }

      this._item = var1;
      String var23 = var1.a(this._variables);
      int var24 = var23.indexOf(63);
      if (var24 != -1) {
         String var25 = var23.substring(0, var24);
         String var6 = var23.substring(var24 + 1);
         LinkedHashMap<String, List<String>> var28 = new LinkedHashMap<>();
         if (!var6.isEmpty()) {
            String[] var8 = var6.split("&");

            for (String var12 : var8) {
               int var13 = var12.indexOf(61);
               String var14;
               String var15;
               if (var13 != -1) {
                  var14 = var12.substring(0, var13);
                  var15 = var12.substring(var13 + 1);
               } else {
                  var14 = var12;
                  var15 = "";
               }

               var28.computeIfAbsent(var14, var0 -> new ArrayList<>()).add(var15);
            }
         }

         this.karate.http.url(var25);
         var var31 = var1.n();
         if (var31 != null && !var31.isEmpty()) {
            LinkedHashMap<String, List<String>> var34 = new LinkedHashMap<>();

            for (Map var48 : (Iterable<Map>) (Iterable<?>) (var31)) {
               Boolean var56 = (Boolean)var48.get("disabled");
               if (var56 == null || !var56) {
                  String var64 = (String)var48.get("key");
                  String var73 = (String)var48.get("value");
                  if (var64 != null) {
                     var64 = this._variables.c(var64);
                     if (var73 != null) {
                        var73 = this._variables.c(var73);
                     } else {
                        var73 = "";
                     }

                     var34.computeIfAbsent(var64, var0 -> new ArrayList<>()).add(var73);
                  }
               }
            }

            this.karate.http.params(var34);
         } else {
            this.karate.http.params(var28);
         }
      } else {
         this.karate.http.url(var23);
      }

      String var26 = var1.g();
      if (var26 == null) {
         var26 = "GET";
      }

      this.karate.http.method(var26);
      var var27 = var1.p();
      if (var27 != null) {
         for (Map var32 : (Iterable<Map>) (Iterable<?>) (var27)) {
            Boolean var35 = (Boolean)var32.get("disabled");
            if (var35 == null || !var35) {
               String var39 = (String)var32.get("key");
               String var49 = (String)var32.get("value");
               if (var39 != null) {
                  var39 = this._variables.c(var39);
                  if (var49 == null) {
                     var49 = "";
                  }

                  var49 = this._variables.c(var49);
                  this.karate.http.header(var39, var49);
               }
            }
         }
      }

      String var30 = var1.z();
      if (var30 != null) {
         switch (var30) {
            case "raw":
               String var46 = var1.A();
               if (var46 != null) {
                  var46 = this._variables.c(var46);
                  this.karate.http.body(var46);
               }
               break;
            case "urlencoded":
               var var45 = var1.B();
               if (var45 != null) {
                  for (Map var59 : (Iterable<Map>) (Iterable<?>) (var45)) {
                     Boolean var68 = (Boolean)var59.get("disabled");
                     if (var68 == null || !var68) {
                        String var76 = (String)var59.get("key");
                        Object var84 = var59.get("value");
                        String var90 = var84 != null ? var84.toString() : null;
                        if (var76 != null) {
                           var76 = this._variables.c(var76);
                           if (var90 == null) {
                              var90 = "";
                           }

                           var90 = this._variables.c(var90);
                           this.karate.http.formField(var76, var90);
                        }
                     }
                  }
               }
               break;
            case "formdata":
               var var44 = var1.C();
               if (var44 != null) {
                  for (Map var58 : (Iterable<Map>) (Iterable<?>) (var44)) {
                     Boolean var67 = (Boolean)var58.get("disabled");
                     if (var67 == null || !var67) {
                        String var75 = (String)var58.get("key");
                        if (var75 != null) {
                           var75 = this._variables.c(var75);
                        }

                        String var83 = (String)var58.get("type");
                        HashMap var16 = new HashMap();
                        var16.put("name", var75);
                        if ("file".equals(var83)) {
                           String var17 = (String)var58.get("src");
                           if (var17 != null) {
                              var17 = this._variables.c(var17);
                           }

                           Resource var18 = this.root.resolve(var17);
                           var16.put("value", var18.getPath().toFile());
                        } else {
                           Object var97 = var58.get("value");
                           String var102 = var97 != null ? var97.toString() : null;
                           if (var102 != null) {
                              var102 = this._variables.c(var102);
                           }

                           var16.put("value", var102);
                        }

                        this.karate.http.multiPart(var16);
                     }
                  }
               }
               break;
            case "file":
            case "binary":
               String var42 = var1.D();
               if (var42 != null) {
                  var42 = this._variables.c(var42);
                  Resource var52 = this.root.resolve(var42);
                  this.karate.http.body(FileUtils.toBytes(var52.getStream()));
               }
               break;
            case "graphql":
               String var41 = var1.E();
               if (var41 != null) {
                  LinkedHashMap var51 = new LinkedHashMap();
                  var51.put("query", var41);
                  String var57 = var1.F();
                  if (var57 != null) {
                     try {
                        Map var66 = (Map)Json.parseLenient(var57);
                        var51.put("variables", var66);
                     } catch (Exception var21) {
                     }
                  }

                  this.karate.http.body(var51);
               }
         }
      }

      PostmanCollectionNode var33 = resolveAuth(var1);
      if (var33 != null) {
         String var37 = var33.q();
         switch (var37) {
            case "basic":
               var var63 = var33.r();
               String var72 = null;
               String var82 = null;

               for (Map var96 : (Iterable<Map>) (Iterable<?>) (var63)) {
                  String var101 = (String)var96.get("key");
                  Object var107 = var96.get("value");
                  String var108 = var107 != null ? var107.toString() : null;
                  if (var108 != null) {
                     var108 = this._variables.c(var108);
                  }

                  if ("username".equals(var101)) {
                     var72 = var108;
                  } else if ("password".equals(var101)) {
                     var82 = var108;
                  }
               }

               if (var72 != null && var82 != null) {
                  this.karate.http.auth(new BasicAuthHandler(var72, var82));
               }
               break;
            case "bearer":
               var var62 = var33.s();
               String var71 = null;

               for (Map var88 : (Iterable<Map>) (Iterable<?>) (var62)) {
                  String var95 = (String)var88.get("key");
                  Object var100 = var88.get("value");
                  String var106 = var100 != null ? var100.toString() : null;
                  if (var106 != null) {
                     var106 = this._variables.c(var106);
                  }

                  if ("token".equals(var95)) {
                     var71 = var106;
                  }
               }

               if (var71 != null) {
                  this.karate.http.auth(new BearerAuthHandler(var71));
               }
               break;
            case "apikey":
               String var61 = null;
               String var70 = null;
               String var80 = null;

               for (Map var94 : var33.t()) {
                  Object var99 = var94.get("value");
                  String var105 = var99 != null ? this._variables.c(var99.toString()) : null;
                  String var19 = String.valueOf(var94.get("key"));
                  switch (var19) {
                     case "key":
                        var61 = var105;
                        break;
                     case "value":
                        var70 = var105;
                        break;
                     case "in":
                        var80 = var105;
                  }
               }

               if (var61 != null && !var61.isEmpty()) {
                  if ("query".equals(var80)) {
                     this.karate.http.param(var61, var70 == null ? "" : var70);
                  } else {
                     this.karate.http.header(var61, var70 == null ? "" : var70);
                  }
               }
               break;
            case "oauth2":
               var var60 = var33.u();
               HashMap<String, Object> var69 = new HashMap<>();

               for (Map var85 : (Iterable<Map>) (Iterable<?>) (var60)) {
                  String var92 = (String)var85.get("key");
                  Object var98 = var85.get("value");
                  String var103 = var98 != null ? var98.toString() : null;
                  if (var103 != null) {
                     var103 = this._variables.c(var103);
                  }

                  var69.put(var92, var103);
               }

               var69.putIfAbsent("url", var69.get("accessTokenUrl"));
               var69.putIfAbsent("client_id", var69.get("clientId"));
               var69.putIfAbsent("client_secret", var69.get("clientSecret"));
               var69.values().removeIf(Objects::isNull);
               String var79 = (String)var69.getOrDefault("grant_type", "client_credentials");
               String var86 = "oauth2:" + var79 + ":" + var69.get("client_id");
               Object var93 = this.authHandlerCache.get(var86);
               if (var93 == null) {
                  switch (var79) {
                     case "client_credentials":
                        var93 = new ClientCredentialsAuthHandler(var69);
                        break;
                     case "authorization_code":
                     case "authorization_code_pkce":
                        if (this.oauth2BrowserCallback != null) {
                           var93 = new AuthorizationCodeAuthHandler(var69, this.oauth2BrowserCallback);
                        }
                  }

                  if (var93 != null) {
                     this.authHandlerCache.put(var86, (AuthHandler)var93);
                  }
               }

               if (var93 == null) {
                  this.onUnsupported(new UnsupportedPostmanFeatureException("auth: oauth2 " + var79));
               } else {
                  this.karate.http.auth((AuthHandler)var93);
               }
               break;
            default:
               this.onUnsupported(new UnsupportedPostmanFeatureException("auth: " + var37));
         }
      }

      return this.karate.http.build();
   }

   public String toCurlCommand(PostmanCollectionNode var1) throws Exception {
      return this.toCurlCommand(var1, "sh");
   }

   public String toCurlCommand(PostmanCollectionNode var1, String var2) throws Exception {
      HttpRequest var3 = this.buildRequest(var1, false);
      return var3 == null ? null : this.karate.http.toCurlCommandPreview(var2);
   }

   public static PostmanCollectionNode resolveAuth(PostmanCollectionNode var0) {
      for (PostmanCollectionNode var1 = var0; var1 != null; var1 = var1.c) {
         String var2 = var1.q();
         if ("noauth".equals(var2)) {
            return null;
         }

         if (var2 != null && !"inherit".equals(var2)) {
            List var3 = switch (var2) {
               case "basic" -> var1.r();
               case "bearer" -> var1.s();
               case "apikey" -> var1.t();
               case "oauth2" -> var1.u();
               default -> null;
            };
            if (var3 == null || !var3.isEmpty()) {
               return var1;
            }

            if (var1 == var0) {
               return null;
            }
         }
      }

      return null;
   }

   List<io.karatelabs.ext.postman.PmRuntime.InheritedScript> collectScriptsWithInheritance(PostmanCollectionNode var1, String var2) {
      ArrayList var3 = new ArrayList();
      ArrayList var4 = new ArrayList();

      for (PostmanCollectionNode var5 = var1; var5 != null; var5 = var5.c) {
         var4.add(var5);
      }

      Collections.reverse(var4);

      for (PostmanCollectionNode var7 : (Iterable<PostmanCollectionNode>) (Iterable<?>) (var4)) {
         for (String var9 : var7.r(var2)) {
            var3.add(new io.karatelabs.ext.postman.PmRuntime.InheritedScript(var7, var9, var2));
         }
      }

      return var3;
   }

   private static Map<String, Object> sent(HttpRequest var0) {
      LinkedHashMap var1 = new LinkedHashMap();
      var1.put("method", var0.getMethod());
      String var2 = var0.getUrlAndPath();
      var var3 = var0.getParams();
      if (var3 != null && !var3.isEmpty() && var2 != null && !((java.lang.String)var2).contains("?")) {
         StringBuilder var4 = new StringBuilder();
         var3.forEach((var1x, var2x) -> var2x.forEach(var2xx -> var4.append((char)(var4.isEmpty() ? '?' : '&')).append(var1x).append('=').append(var2xx)));
         var2 = var2 + var4;
      }

      var1.put("url", var2);
      LinkedHashMap var6 = new LinkedHashMap();
      if (var0.getHeaders() != null) {
         var0.getHeaders().forEach((var1x, var2x) -> var6.put(var1x, var2x != null && var2x.size() == 1 ? var2x.get(0) : var2x));
      }

      var1.put("headers", var6);
      String var5 = var0.getBodyDisplay() != null ? var0.getBodyDisplay() : (var0.getBody() == null ? null : var0.getBodyString());
      var1.put("body", var5);
      return var1;
   }

   private static String wrapInIife(String var0) {
      return "(function(){" + var0 + "})()";
   }

   public PostmanRequestRunResult request(PostmanCollectionNode var1) {
      this._variables.a();

      try {
         this.sendRequest(var1);
      } catch (RuntimeException var3) {
         logger.debug("request {} failed: {}", var1.d(), var3.getMessage());
      }

      return this._executionResult;
   }

   private HttpResponse sendRequest(PostmanCollectionNode var1) {
      this._executionResult = new PostmanRequestRunResult();
      this._response = null;

      try {
         this.skip = false;
         this.htmlHeading(var1.d());
         this._request = this.buildRequest(var1, true);
         if (this._request == null) {
            this._executionResult.b(true);
            this.htmlInfo("skip request: " + var1.d());
            if (this.responseCallback != null) {
               this.responseCallback.accept(var1, this._executionResult);
            }

            return null;
         } else {
            this._executionResult.a(this._request);
            this.htmlRequest();
            this._response = this.karate.client.invoke(this._request);
            this._response.setRequest(this._request);
            this._executionResult.a(sent(this._request));
            this._executionResult.a(this._response);
            this.htmlResponse();
            var var2 = this.collectScriptsWithInheritance(var1, "test");
            this._eventName = "test";

            for (io.karatelabs.ext.postman.PmRuntime.InheritedScript var4 : (Iterable<io.karatelabs.ext.postman.PmRuntime.InheritedScript>) (Iterable<?>) (var2)) {
               try {
                  this.karate.engine.eval(wrapInIife(var4.b));
                  logger.debug("eval done: after script for {}", var4.a.d());
               } catch (Exception var7) {
                  logger.debug("after script failed for {}: {}", var4.a.d(), var7.getMessage());
                  UnsupportedPostmanFeatureException var6 = io.karatelabs.ext.postman.UnsupportedPostmanFeatureException.a(var7);
                  if (var6 == null) {
                     this._executionResult.a(var7);
                     this.htmlError(var7.getMessage());
                  } else {
                     this.onUnsupported(var6);
                  }
               }
            }

            this.htmlTestSummary();
            if (this.responseCallback != null) {
               this.responseCallback.accept(var1, this._executionResult);
            }

            return this._response;
         }
      } catch (Exception var8) {
         this._executionResult.a(var8);
         this.htmlError(var8.getMessage());
         logger.error("{}", var8.getMessage(), var8);
         if (this.responseCallback != null) {
            this.responseCallback.accept(var1, this._executionResult);
         }

         if (var8 instanceof RuntimeException) {
            throw (RuntimeException)var8;
         } else {
            throw new RuntimeException(var8);
         }
      }
   }

   static class InheritedScript {
      final PostmanCollectionNode a;
      final String b;
      final String c;

      InheritedScript(PostmanCollectionNode var1, String var2, String var3) {
         this.a = var1;
         this.b = var2;
         this.c = var3;
      }
   }
}
