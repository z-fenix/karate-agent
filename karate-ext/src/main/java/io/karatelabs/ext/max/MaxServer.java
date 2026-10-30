package io.karatelabs.ext.max;

import io.karatelabs.common.Json;
import io.karatelabs.common.Resource;
import io.karatelabs.common.ResourceType;
import io.karatelabs.ext.agent.llm.LlmProvider;
import io.karatelabs.ext.common.ExtMeta;
import io.karatelabs.ext.common.MaxPaths;
import io.karatelabs.ext.common.server.ArtifactRegistry;
import io.karatelabs.ext.common.server.HttpRequestRouter;
import io.karatelabs.ext.common.server.SseEventBroker;
import io.karatelabs.ext.common.jsapi.JsApi;
import io.karatelabs.ext.common.jsapi.JsDispatch;
import io.karatelabs.ext.mutation.RuleMutationAdapter;
import io.karatelabs.ext.max.demo.DemoApp;
import io.karatelabs.ext.max.demo.DemoOrdersPage;
import io.karatelabs.ext.max.jsapi.Check;
import io.karatelabs.ext.max.jsapi.ProjectBoot;
import io.karatelabs.ext.max.jsapi.ProjectConfig;
import io.karatelabs.ext.max.run.MaxProfile;
import io.karatelabs.ext.max.run.MaxWorkspace;
import io.karatelabs.ext.openapi.changes.OasdiffCrosswalk;
import io.karatelabs.http.HttpRequest;
import io.karatelabs.http.HttpResponse;
import io.karatelabs.http.HttpServer;
import io.karatelabs.http.InMemorySessionStore;
import io.karatelabs.http.ServerConfig;
import io.karatelabs.http.ServerRequestCycle;
import io.karatelabs.http.ServerRequestHandler;
import io.karatelabs.http.SseConnection;
import io.karatelabs.http.WsConnection;
import io.karatelabs.js.Engine;
import io.karatelabs.js.SimpleObject;
import io.karatelabs.markup.ResourceResolver;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MaxServer {
   private static final Logger logger = LoggerFactory.getLogger(MaxServer.class);
   private static final String WEB_CP_ROOT = "classpath:io/karatelabs/ext/max/web";
   private static final String SSE_PATH = "/api/events";
   private static final String ARTIFACTS_PREFIX = "/api/artifacts";
   private final Engine engine = new Engine();
   private final ReentrantLock evalLock = new ReentrantLock();
   private final SseEventBroker sseHub = new SseEventBroker();
   private final ArtifactRegistry artifacts = new ArtifactRegistry();
   private final DemoApp demoApp = new DemoApp();
   private final HttpRequestRouter machineRouter = new HttpRequestRouter()
      .a("POST", "/api/mcp", var1 -> this.mcpServer.a(var1.getBodyString(), var1::getHeader))
      .a("POST", "/api/eval", this::handleApiEval)
      .a("POST", "/api/write", this::handleApiWrite)
      .a("GET", "/demo", this.demoApp::page)
      .a("GET", "/demo/api/products", this.demoApp::products)
      .a("POST", "/demo/api/quote", this.demoApp::quote)
      .a("GET", "/demo/api/config", this.demoApp::getConfig)
      .a("POST", "/demo/api/config/reset", this.demoApp::resetConfig)
      .a("POST", "/demo/api/config", this.demoApp::setConfig)
      .a("POST", "/demo/api/login", this.demoApp::login)
      .a("GET", "/demo/api/stats", this.demoApp::stats)
      .a("POST", "/demo/api/orders", this.demoApp::createOrder)
      .b("GET", "/demo/api/orders/", this.demoApp::getOrder)
      .b("POST", "/demo/api/orders/", this.demoApp::orderAction)
      .a("GET", "/demo/orders", DemoOrdersPage::page)
      .a("GET", "/demo/orders.css", DemoOrdersPage::stylesheet)
      .a("GET", "/demo/orders-logo.svg", DemoOrdersPage::logo)
      .a("GET", "/demo/beacon", DemoOrdersPage::beacon)
      .a("GET", "/api/prompts", var1 -> this.handlePrompt(var1.getPath()))
      .b("GET", "/api/prompts/", var1 -> this.handlePrompt(var1.getPath()))
      .a("GET", "/api/health", var1 -> this.healthResponse())
      .a("GET", "/api/openapi/crosswalk", var0 -> HttpResponse.json(io.karatelabs.ext.openapi.changes.OasdiffCrosswalk.b()))
      .b("GET", "/api/artifacts", this::handleArtifacts)
      .b("GET", "/api/work-zip/", this::handleWorkZip)
      .b("GET", "/api/run-zip/", this::handleRunZip)
      .b(
         "GET",
         "/novnc/",
         var1 -> this.vncProxy != null ? this.vncProxy.serveStatic(var1) : HttpResponse.notFound("novnc not configured (set KARATE_NOVNC_ROOT)")
      )
      .a("/api/", 404);
   private io.karatelabs.ext.common.mcp.McpRequestHandler mcpServer;
   private String coldStartPrompt;
   private HttpServer httpServer;
   private ServerRequestHandler markupHandler;
   private final Map<String, Object> templateGlobals = new ConcurrentHashMap<>();
   private io.karatelabs.ext.common.run.EngineRuntimeProfile profile;
   private VncProxy vncProxy;
   private String trustedOrigins = System.getenv("KARATE_CONSOLE_ORIGINS");
   private String trustedHosts = System.getenv("KARATE_CONSOLE_HOSTS");
   private ConsoleTrust trust;

   DemoApp demoApp() {
      return this.demoApp;
   }

   public MaxServer() {
      io.karatelabs.ext.mutation.RuleMutationAdapter.install();
      this.mcpServer = new io.karatelabs.ext.common.mcp.McpRequestHandler(this::evalJs).a(this::resolveArtifactBytes);
   }

   public MaxServer applyProfile(io.karatelabs.ext.common.run.EngineRuntimeProfile var1) {
      this.profile = var1;
      if (var1 instanceof MaxProfile var2) {
         var2.bindReanchorFn(this::reanchor);
         var2.bindViewSink(this::publish);
         var2.bindUploadRootSupplier(this::currentRoot);
         var2.bindServePort(() -> this.httpServer == null ? -1 : this.httpServer.getPort());
      }

      var1.apply(this.engine);
      var1.artifactRoots().forEach(this.artifacts::a);
      this.coldStartPrompt = var1.a(io.karatelabs.ext.common.help.PromptTemplateRepository.PromptTemplateKind.LIVE);
      this.templateGlobals.putAll(var1.templateGlobals());
      this.mcpServer = new io.karatelabs.ext.common.mcp.McpRequestHandler(this::evalJs, this.coldStartPrompt).a(this::resolveArtifactBytes);
      return this;
   }

   public Object reanchor(String var1) {
      if (this.profile instanceof MaxProfile var2) {
         this.evalLock.lock();

         Map var13;
         try {
            MaxWorkspace var11;
            try {
               var11 = var2.workspace().reanchor(Path.of(var1));
            } catch (RuntimeException var9) {
               return Map.of("error", var9.getMessage());
            }

            if (!Files.isDirectory(var11.root())) {
               return Map.of("error", "no project folder at " + var11.root() + " — Work.list() names the projects");
            }

            String var4 = var2.reanchorBlockedReason();
            if (var4 == null) {
               this.applyProfile(var2.atRoot(var11.root()));
               this.bindDemoUrl();
               ProjectConfig.reset(this.engine);
               ProjectBoot.reset(this.engine);
               this.publish("reanchor", Json.stringifyStrict(Map.of("type", "reanchor", "root", var11.root().toString())));
               logger.info("re-anchored serve to {}", var11.root());
               return Map.of("root", var11.root().toString(), "status", "switched");
            }

            var13 = Map.of("error", "cannot switch project while a run is active — " + var4);
         } finally {
            this.evalLock.unlock();
         }

         return var13;
      } else {
         return Map.of("error", "re-anchor is not supported on this serve profile");
      }
   }

   public Path currentRoot() {
      return this.profile instanceof MaxProfile var1 ? var1.workspace().root() : null;
   }

   public Engine getEngine() {
      return this.engine;
   }

   public MaxServer bind(String var1, Object var2) {
      this.engine.putRootBinding(var1, var2);
      return this;
   }

   public MaxServer vnc(Path var1, String var2) {
      this.vncProxy = new VncProxy(var1, var2);
      return this;
   }

   public MaxServer trust(String var1, String var2) {
      this.trustedOrigins = var1;
      this.trustedHosts = var2;
      return this;
   }

   public String evalJs(String var1) {
      return this.evalJs(var1, null);
   }

   public String evalJs(String var1, Map<String, Object> var2) {
      if (ServerRequestCycle.get() != null) {
         throw new IllegalStateException(
            "the shared run engine (evalJs) was called from a page render or /ui/ handler — page data calls go through the /ui/ markup-handler channel on a fresh per-request engine (D98), never the eval lock"
         );
      } else {
         if (this.profile != null) {
            try {
               this.profile.onEvalStart(var1);
            } catch (Exception var16) {
               logger.debug("onEvalStart hook failed: {}", var16.toString());
            }
         }

         this.evalLock.lock();

         String var3;
         try {
            LinkedHashMap<java.lang.String, java.lang.Object> var4 = new LinkedHashMap();
            io.karatelabs.ext.common.jsapi.EvaluationMessageScope.b();

            try {
               if (var2 != null) {
                  var2.forEach(this.engine::putRootBinding);
               }

               Object var5 = this.engine.eval(var1);
               var4.put("command", "result");
               Object var20 = JsApi.toWire(var5);
               Object var21 = JsApi.extractParts(var20);
               var4.put("payload", var20);
               if (var21 != null) {
                  var4.put("parts", var21);
               }

               var var22 = JsApi.handleRef(var5);
               if (var22 != null) {
                  var4.put("ref", var22);
               }
            } catch (Throwable var17) {
               logger.debug("eval failed: {}", var17.toString());
               var4.put("command", "error");
               String var6 = JsApi.jsErrorMessage(var17);
               var4.put("payload", var6);
               String var7 = JsApi.jsErrorName(var17);
               if (var7 != null) {
                  var4.put("name", var7);
               }

               Check.Failure var8 = Check.Failure.in(var17);
               if (var8 != null && var8.verified() != null) {
                  var4.put("verified", var8.verified());
               }

               String var9 = featureBuiltinHint(var7, var6);
               if (var9 == null) {
                  var9 = this.memberHint(var7, var6);
               }

               if (var9 != null) {
                  var4.put("hint", var9);
               }
            }

            String var19 = io.karatelabs.ext.common.jsapi.EvaluationMessageScope.a();
            if (var19 != null && !var4.containsKey("hint")) {
               var4.put("hint", var19);
            }

            var3 = Json.stringifyStrict(var4);
         } finally {
            this.evalLock.unlock();
         }

         if (this.profile != null) {
            try {
               this.profile.onEval(var1, var3);
            } catch (Exception var15) {
               logger.debug("onEval hook failed: {}", var15.toString());
            }
         }

         return var3;
      }
   }

   private String memberHint(String var1, String var2) {
      if ("TypeError".equals(var1) && var2 != null) {
         int var3 = var2.indexOf(" is not a function");
         if (var3 <= 0) {
            return null;
         } else {
            String var4 = var2.substring(0, var3).trim();
            int var5 = var4.lastIndexOf(46);
            if (var5 > 0 && var5 != var4.length() - 1) {
               String var6 = var4.substring(0, var5);
               String var7 = var4.substring(var5 + 1);
               if (var7.matches("[A-Za-z_$][\\w$]*")) {
                  if (!(this.resolveBinding(var6) instanceof JsDispatch var9)) {
                     return null;
                  } else {
                     Object var10 = var9.jsGet(var7);
                     if (var10 instanceof JsDispatch var15) {
                        String var16 = String.join("`, `", var15.jsKeys());
                        String var13 = var9.jsKeys().contains("load")
                           ? " Those are writers — to READ this data, use `" + var6 + ".load(name)." + var7 + "`."
                           : "";
                        return "`" + var4 + "` is a sub-namespace, not a verb — it groups `" + var16 + "`, called as `" + var4 + ".<verb>(…)`." + var13;
                     } else if (var10 != null) {
                        return null;
                     } else {
                        String var11 = var9.jsKeys().contains("help") && !"help".equals(var7)
                           ? "(`" + var6 + ".help()` for the full surface)"
                           : "(`" + var6 + "` groups `" + String.join("` / `", var9.jsKeys()) + "`)";
                        var var12 = io.karatelabs.ext.common.jsapi.SuggestionEngine.a(var7, var9.jsKeys());
                        return !var12.isEmpty()
                           ? "`" + var6 + "` has no `" + var7 + "` — did you mean `" + String.join("` / `", var12) + "`? " + var11
                           : "`" + var6 + "` has no `" + var7 + "` — " + var11;
                     }
                  }
               } else if (var7.matches("[A-Za-z_$][\\w$]*\\(.*")) {
                  String var8 = var7.substring(0, var7.indexOf(40));
                  return "`"
                     + var6
                     + "."
                     + var8
                     + "(…)` ran, and its RESULT was then called as a function — which is what a line ending without `;` means when the next line opens with `(`. End that statement with a semicolon. (`"
                     + var6
                     + "."
                     + var8
                     + "` itself is fine.)";
               } else {
                  return null;
               }
            } else {
               return null;
            }
         }
      } else {
         return null;
      }
   }

   private Object resolveBinding(String var1) {
      Object var2 = null;

      for (String var6 : var1.split("\\.")) {
         if (var2 == null) {
            var2 = this.engine.getRootBindings().get(var6);
         } else {
            if (!(var2 instanceof SimpleObject var7)) {
               return null;
            }

            var2 = var7.jsGet(var6);
         }
      }

      return var2;
   }

   private static String featureBuiltinHint(String var0, String var1) {
      if ("ReferenceError".equals(var0) && var1 != null) {
         String var2 = JsApi.notDefinedIdentifier(var1);
         if (var2 == null) {
            return null;
         } else {
            return switch (var2) {
               case "read" -> "`read(path)` is a feature-runtime builtin — in this console use `File.read(path)` (`JSON.parse(File.read('x.json'))` for JSON).";
               case "karate" -> "`karate.*` is the feature-runtime API — in this console the equivalents are `File.*` (read/write, and `File.call(path, arg?)` for `karate.call`/`callSingle`), `Http.*` (requests), `Check.*` (run macros); call `help()` for the menu.";
               case "call", "callSingle", "callonce" -> "`"
                  + var2
                  + "` is a feature-runtime builtin — in this console run a project .js file with `File.call('/start-mock.js', arg?)` (karate call semantics: a file whose value is a function is invoked with the arg), a `checks/` macro with `Check.run('name.js', data)`, or a suite with `Runner.run(...)`.";
               case "request", "response", "url", "method" -> "`"
                  + var2
                  + "` is a gherkin/feature step keyword, not a console global — explore a live API here with `Http.*` (e.g. `Http.get(url)`).";
               case "def" -> "`def` is gherkin syntax — in this console just use plain JS (`var x = ...`); vars persist across calls (see `Engine.vars()`).";
               default -> null;
            };
         }
      } else {
         return null;
      }
   }

   public SseEventBroker getSseHub() {
      return this.sseHub;
   }

   public MaxServer publish(String var1, String var2) {
      this.sseHub.a(var1, var2);
      return this;
   }

   public MaxServer start(int var1) {
      return this.start(null, var1);
   }

   public MaxServer start(String var1, int var2) {
      this.markupHandler = this.buildMarkupHandler();
      this.trust = new ConsoleTrust(var1, () -> this.httpServer == null ? -1 : this.httpServer.getPort(), this.trustedOrigins, this.trustedHosts);
      this.httpServer = HttpServer.start(var1, var2, this::handleRequest, this::handleSse, this::handleWs);
      this.bindDemoUrl();
      logger.info("karate-max serving /api/eval + /api/mcp + SSR console + SSE on port {}", this.getPort());
      if (this.trust.rebindable()) {
         logger.warn(
            "console bound beyond loopback with {} unset: a DNS-rebinding page can reach it; set it to the origins browsers use (scheme://host[:port], comma-separated)",
            "KARATE_CONSOLE_ORIGINS"
         );
      }

      this.logLlmKeyHint();
      return this;
   }

   private void logLlmKeyHint() {
      for (LlmProvider var4 : LlmProvider.values()) {
         if (var4 != io.karatelabs.ext.agent.llm.LlmProvider.OLLAMA && var4.getApiKeyFromEnv() != null) {
            return;
         }
      }

      if (System.getenv("OLLAMA_HOST") == null) {
         logger.info(
            "no LLM provider key in the environment (ANTHROPIC_API_KEY / OPENAI_API_KEY / OPENROUTER_API_KEY) — Mode A (drive this server from your own MCP client) works as-is; the container's own autonomous runs (Mode B) need a key set via 'docker run -e', or a local Ollama"
         );
      }
   }

   private void bindDemoUrl() {
      if (this.httpServer != null) {
         System.setProperty("demo.url", "http://localhost:" + this.getPort());
      }
   }

   private ServerRequestHandler buildMarkupHandler() {
      ServerConfig var1 = new ServerConfig("classpath:io/karatelabs/ext/max/web")
         .shellTemplate("layout.html")
         .csrfEnabled(false)
         .securityHeadersEnabled(false)
         .sessionStore(new InMemorySessionStore())
         .sessionExpirySeconds(28800)
         .apiPrefix("/ui/")
         .templateRoute("/runs/{id}", "run.html")
         .templateRoute("/rulebooks/{name}", "rulebook.html")
         .templateRoute("/checks/crosswalk", "crosswalk.html")
         .templateRoute("/manual/runs/{id}", "manual/run.html")
         .rawPaths("/benchmark.html")
         .globalVariables(this.templateGlobals);
      ResourceResolver var2 = (var0, var1x) -> "pub/schema-form.js".equals(Resource.stripLeadingSlashes(var0))
         ? Resource.path("classpath:META-INF/karate/app/pub/schema-form.js")
         : Resource.path("classpath:io/karatelabs/ext/max/web/" + var0);
      return new ServerRequestHandler(var1, var2);
   }

   public int getPort() {
      return this.httpServer.getPort();
   }

   public void waitSync() {
      this.httpServer.waitSync();
   }

   public void stop() {
      this.sseHub.b();
      if (this.httpServer != null) {
         this.httpServer.stopAsync();
      }

      if (this.profile != null) {
         this.profile.stop();
      }
   }

   private HttpResponse handleRequest(HttpRequest var1) {
      String var2 = this.trust.check(var1, false);
      if (var2 != null) {
         return HttpResponse.json(403, Map.of("error", refusal(var2), "code", var2, "status", 403));
      } else {
         HttpResponse var3 = this.machineRouter
            .a(var1)
            .orElseGet(
               () -> this.markupHandler != null
                  ? this.markupHandler.apply(var1)
                  : HttpResponse.notFound("no route: " + var1.getMethod() + " " + var1.getPath())
            );
         String var4 = var1.getPath();
         if (var3 != null && var4 != null && (var4.endsWith("/pub/vendor/cm.js") || var4.endsWith("/pub/vendor/marked.min.js"))) {
            var3.setHeader("Cache-Control", "no-cache");
         }

         return var3;
      }
   }

   private static String refusal(String var0) {
      return "HOST_REFUSED".equals(var0)
         ? "the Host header names neither this console nor a configured origin (KARATE_CONSOLE_ORIGINS)"
         : "a state-changing request from another origin is refused — the console accepts writes only from its own pages, a configured origin (KARATE_CONSOLE_ORIGINS) or a client that sends no Origin";
   }

   private void handleSse(HttpRequest var1, SseConnection var2) {
      String var3 = this.trust.check(var1, false);
      if (var3 != null) {
         var2.reject(403, var3 + ": " + refusal(var3));
      } else if (!"/api/events".equals(var1.getPath())) {
         if ("/api/mcp".equals(var1.getPath())) {
            var2.reject(405, "Method Not Allowed — /api/mcp is POST-only (no server-initiated SSE)");
         } else {
            var2.reject(404, "no SSE route: " + var1.getPath());
         }
      } else {
         var2.open();
         String var4 = var1.getParam("run");
         if (var4 != null && !var4.isBlank()) {
            this.sseHub.a(var2, var4, parseSince(var1.getParam("since")));
         } else {
            this.sseHub.a(var2);
         }
      }
   }

   private void handleWs(HttpRequest var1, WsConnection var2) {
      String var3 = this.trust.check(var1, true);
      if (var3 != null) {
         var2.close(1008, var3);
      } else if (this.vncProxy != null && "/novnc/websockify".equals(var1.getPath())) {
         this.vncProxy.tunnel(var1, var2);
      } else {
         var2.close(1008, "no ws route: " + var1.getPath());
      }
   }

   private static long parseSince(String var0) {
      if (var0 != null && !var0.isBlank()) {
         try {
            return Long.parseLong(var0.trim());
         } catch (NumberFormatException var2) {
            return -1L;
         }
      } else {
         return -1L;
      }
   }

   public ArtifactRegistry getArtifacts() {
      return this.artifacts;
   }

   private byte[] resolveArtifactBytes(String var1) {
      if (var1 != null && var1.startsWith("/api/artifacts/")) {
         String var2 = var1.substring("/api/artifacts".length() + 1);
         int var3 = var2.indexOf(47);
         if (var3 < 0) {
            return null;
         } else {
            Path var4 = this.artifacts.a(var2.substring(0, var3), var2.substring(var3 + 1));
            if (var4 == null) {
               return null;
            } else {
               try {
                  return Files.readAllBytes(var4);
               } catch (Exception var6) {
                  logger.debug("artifact bytes read failed for {}: {}", var1, var6.getMessage());
                  return null;
               }
            }
         }
      } else {
         return null;
      }
   }

   private HttpResponse handleArtifacts(HttpRequest var1) {
      String var2 = var1.getPath();
      String var3 = var2.substring("/api/artifacts".length());
      if (!var3.isEmpty() && !"/".equals(var3)) {
         var3 = var3.substring(1);
         int var26 = var3.indexOf(47);
         if (var26 < 0) {
            return !this.artifacts.a().containsKey(var3)
               ? HttpResponse.notFound(
                  "no artifact root '" + var3 + "' — the roots are " + this.artifacts.a().keySet() + " (GET /api/artifacts lists them with their urls)"
               )
               : HttpResponse.json(Map.of("files", this.artifacts.b(var3, "/api/artifacts")));
         } else {
            String var5 = var3.substring(0, var26);
            String var6 = var3.substring(var26 + 1);
            Path var7 = this.artifacts.a(var5, var6);
            if (var7 == null) {
               return HttpResponse.notFound("no artifact: " + var2);
            } else {
               try {
                  ResourceType var8 = ResourceType.fromFileExtension(var7.getFileName().toString());
                  ResourceType var9 = var8 != null ? var8 : ResourceType.BINARY;
                  long var10 = Files.size(var7);
                  String var12 = var1.getHeader("Range");
                  if (var12 != null && var12.startsWith("bytes=")) {
                     String[] var27 = var12.substring("bytes=".length()).split("-", 2);
                     long var28 = var27[0].isEmpty() ? 0L : Long.parseLong(var27[0]);
                     long var16 = var27.length >= 2 && !var27[1].isEmpty() ? Long.parseLong(var27[1]) : var10 - 1L;
                     if (var28 >= var10) {
                        HttpResponse var30 = new HttpResponse();
                        var30.setStatus(416);
                        var30.setHeader("Content-Range", "bytes */" + var10);
                        return var30;
                     } else {
                        var16 = Math.min(var16, var10 - 1L);

                        byte[] var18;
                        try (InputStream var19 = Files.newInputStream(var7)) {
                           var19.skip(var28);
                           var18 = var19.readNBytes((int)(var16 - var28 + 1L));
                        }

                        HttpResponse var31 = new HttpResponse();
                        var31.setStatus(206);
                        var31.setHeader("Content-Range", "bytes " + var28 + "-" + var16 + "/" + var10);
                        var31.setHeader("Accept-Ranges", "bytes");
                        var31.setBody(var18, var9);
                        return var31;
                     }
                  } else {
                     byte[] var13 = Files.readAllBytes(var7);
                     HttpResponse var14 = HttpResponse.ok();
                     var14.setHeader("Accept-Ranges", "bytes");
                     var14.setBody(var13, var9);
                     return var14;
                  }
               } catch (Exception var24) {
                  logger.debug("artifact read failed for {}: {}", var2, var24.getMessage());
                  return HttpResponse.notFound("no artifact: " + var2);
               }
            }
         }
      } else {
         LinkedHashMap<java.lang.String, java.lang.String> var4 = new LinkedHashMap();
         this.artifacts.a().keySet().forEach(var1x -> var4.put(var1x, "/api/artifacts/" + var1x));
         return HttpResponse.json(
            Map.of(
               "roots",
               this.artifacts.a(),
               "urls",
               var4,
               "note",
               "GET /api/artifacts/<root> lists one root; GET /api/artifacts/<root>/<path relative to that root> serves the file. The `file://` path a JSAPI verb returns is the SERVER's filesystem — over HTTP, reach the same file here."
            )
         );
      }
   }

   private HttpResponse handleWorkZip(HttpRequest var1) {
      String var2 = var1.getPath().substring("/api/work-zip/".length());
      int var3 = var2.indexOf(47);
      if (var3 >= 0) {
         var2 = var2.substring(0, var3);
      }

      if (var2.endsWith(".zip")) {
         var2 = var2.substring(0, var2.length() - 4);
      }

      if (!var2.isBlank() && var2.matches("[a-zA-Z0-9][a-zA-Z0-9._-]*")) {
         if (this.profile instanceof MaxProfile var4) {
            Path var9 = var4.workspace().ceiling();

            Path var6;
            try {
               var6 = MaxPaths.within(var9, var9.resolve(var2));
            } catch (SecurityException var8) {
               return HttpResponse.notFound("no project: " + var2);
            }

            return !Files.isDirectory(var6) ? HttpResponse.notFound("no project: " + var2) : this.zipResponse(var6, var2);
         } else {
            return HttpResponse.notFound("no workspace");
         }
      } else {
         return HttpResponse.notFound("invalid project name");
      }
   }

   private HttpResponse handleRunZip(HttpRequest var1) {
      String var2 = var1.getPath().substring("/api/run-zip/".length());
      int var3 = var2.indexOf(47);
      if (var3 >= 0) {
         var2 = var2.substring(0, var3);
      }

      if (var2.endsWith(".zip")) {
         var2 = var2.substring(0, var2.length() - 4);
      }

      if (!var2.isBlank() && !var2.contains("..") && !var2.contains("/") && !var2.contains("\\")) {
         if (this.profile instanceof MaxProfile var4) {
            Path var9 = var4.workspace().root().resolve("runs").normalize();

            Path var6;
            try {
               var6 = MaxPaths.within(var9, var9.resolve(var2));
            } catch (SecurityException var8) {
               return HttpResponse.notFound("no run: " + var2);
            }

            return !Files.isDirectory(var6) ? HttpResponse.notFound("no run: " + var2) : this.zipResponse(var6, "run-" + var2);
         } else {
            return HttpResponse.notFound("no workspace");
         }
      } else {
         return HttpResponse.notFound("invalid run id");
      }
   }

   private HttpResponse zipResponse(Path var1, String var2) {
      try {
         byte[] var3 = zipDirectory(var1);
         ResourceType var4 = ResourceType.fromFileExtension(var2 + ".zip");
         HttpResponse var5 = HttpResponse.ok();
         var5.setHeader("Content-Disposition", "attachment; filename=\"" + var2 + ".zip\"");
         var5.setBody(var3, var4 != null ? var4 : ResourceType.BINARY);
         return var5;
      } catch (Exception var6) {
         logger.warn("zip failed for {}: {}", var1, var6.getMessage());
         return HttpResponse.notFound("could not zip: " + var2);
      }
   }

   private static byte[] zipDirectory(Path var0) throws IOException {
      List var1;
      try (var var2 = Files.walk(var0)) {
         var1 = var2.filter(var0x -> Files.isRegularFile(var0x)).filter(var0x -> !io.karatelabs.ext.common.server.ArtifactRegistry.a(var0x)).sorted().toList();
      }

      ByteArrayOutputStream var12 = new ByteArrayOutputStream();
      String var3 = var0.getFileName().toString();

      try (ZipOutputStream var4 = new ZipOutputStream(var12)) {
         for (Path var6 : (Iterable<Path>) (Iterable<?>) (var1)) {
            String var7 = var3 + "/" + var0.relativize(var6).toString().replace('\\', '/');
            var4.putNextEntry(new ZipEntry(var7));
            Files.copy(var6, var4);
            var4.closeEntry();
         }
      }

      return var12.toByteArray();
   }

   private HttpResponse healthResponse() {
      LinkedHashMap<java.lang.String, java.lang.String> var1 = new LinkedHashMap();
      var1.put("status", "ok");
      var1.put("server", "karate");
      var1.put("version", ExtMeta.version("karate-max-meta.properties"));
      var1.put("git", ExtMeta.property("karate-max-meta.properties", "git"));
      if (this.coldStartPrompt != null) {
         var1.put("prompt", "GET /api/prompts for the cold-start guide");
      }

      return HttpResponse.json(var1);
   }

   private HttpResponse handlePrompt(String var1) {
      String var2 = var1.startsWith("/api/prompts/") ? var1.substring("/api/prompts/".length()) : "";
      if (var2.isBlank()) {
         return this.coldStartPrompt == null
            ? HttpResponse.notFound("no cold-start prompt — server started without a run profile")
            : HttpResponse.text(this.coldStartPrompt);
      } else {
         String var3 = "browser".equals(var2) ? "robot" : var2;

         try {
            io.karatelabs.ext.common.help.PromptTemplateRepository var4 = io.karatelabs.ext.common.help.PromptTemplateRepository.a(var3);
            return HttpResponse.text(var4.a(io.karatelabs.ext.common.help.PromptTemplateRepository.PromptTemplateKind.LIVE) + "\n\n" + var4.a());
         } catch (RuntimeException var5) {
            return HttpResponse.notFound(
               "no prompt set '" + var2 + "' — GET /api/prompts (the app-wide guide) or /api/prompts/browser (drive the browser live)"
            );
         }
      }
   }

   private HttpResponse handleApiEval(HttpRequest var1) {
      String var2 = var1.getBodyString();
      if (var2 != null && !var2.isBlank()) {
         String var3 = var2.trim();
         if (var3.startsWith("{")) {
            try {
               if (Json.parseLenient(var3) instanceof Map var5 && var5.get("code") instanceof String var6) {
                  var3 = var6;
               }
            } catch (RuntimeException var8) {
            }
         }

         return HttpResponse.json(this.evalJs(var3));
      } else {
         return HttpResponse.badRequest("request body required");
      }
   }

   private HttpResponse handleApiWrite(HttpRequest var1) {
      String var2 = var1.getParam("path");
      if (var2 != null && !var2.isBlank()) {
         String var3 = var1.getBodyString();
         return HttpResponse.json(this.evalJs("File.write(__kWritePath, __kWriteBody)", Map.of("__kWritePath", var2, "__kWriteBody", var3 == null ? "" : var3)));
      } else {
         return HttpResponse.badRequest("POST /api/write requires a ?path=<relative-path> query param");
      }
   }
}
