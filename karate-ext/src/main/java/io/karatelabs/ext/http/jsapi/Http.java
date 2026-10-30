package io.karatelabs.ext.http.jsapi;

import io.karatelabs.ext.common.MaxPaths;
import io.karatelabs.ext.common.jsapi.JsApi;
import io.karatelabs.ext.common.jsapi.JsDispatch;
import io.karatelabs.ext.jsonschema.validation.ValidationDirection;
import io.karatelabs.ext.jsonschema.validation.ValidationException;
import io.karatelabs.ext.jsonschema.validation.ValidationResult;
import io.karatelabs.ext.http.HttpMockFactory;
import io.karatelabs.ext.mock.MockInstance;
import io.karatelabs.ext.openapi.OpenApiDocumentQueries;
import io.karatelabs.ext.openapi.OpenApiOperationCatalog;
import io.karatelabs.ext.openapi.OpenApiSchemaNode;
import io.karatelabs.ext.rules.twin.PlayApp;
import io.karatelabs.http.HttpRequest;
import io.karatelabs.http.HttpResponse;
import io.karatelabs.http.HttpServer;
import io.karatelabs.js.JavaInvokable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Consumer;

public class Http extends JsDispatch {
   private static final int BODY_INLINE_LIMIT = 1500;
   private static final int ARRAY_SAMPLE = 2;
   private static final int SHAPE_DEPTH = 2;
   private String baseUrl;
   private final Map<String, Object> configuredHeaders = new LinkedHashMap<>();
   private JavaInvokable headerFn;
   private final Map<String, String> cookieJar = new LinkedHashMap<>();
   private OpenApiOperationCatalog specModel;
   private String specConfigured;
   private List<Map<String, Object>> capture;
   private Consumer<Map<String, Object>> onExchange;
   private Path rulesHome = Path.of("rules");
   private Path root;
   private Path classpathRoot;
   private final HttpMockFactory mocks = new HttpMockFactory();
   private Consumer<Runnable> onAppStart;
   private static final List<String> APP_OPTIONS = List.of("twin", "target", "console", "dir", "port");
   private static final List<Map<String, Object>> HELP_TOPICS = List.of(
      Map.of("id", "mock", "summary", "Http.mock — the two flavors, and what a handler script sees (request/response/session)"),
      Map.of("id", "app", "summary", "Http.app — the playground app over a recording twin mock, and its three levels")
   );

   public boolean draftMockLive() {
      return this.mocks.a();
   }

   public Http() {
      this.fn("get", var1 -> this.send("GET", JsApi.arg(var1, 0), null, JsApi.arg(var1, 1)));
      this.fn("post", var1 -> this.send("POST", JsApi.arg(var1, 0), JsApi.arg(var1, 1), JsApi.arg(var1, 2)));
      this.fn("put", var1 -> this.send("PUT", JsApi.arg(var1, 0), JsApi.arg(var1, 1), JsApi.arg(var1, 2)));
      this.fn("patch", var1 -> this.send("PATCH", JsApi.arg(var1, 0), JsApi.arg(var1, 1), JsApi.arg(var1, 2)));
      this.fn("delete", var1 -> this.send("DELETE", JsApi.arg(var1, 0), null, JsApi.arg(var1, 1)));
      this.fn("send", var1 -> this.sendSpec(JsApi.arg(var1, 0)));
      this.fn("configure", var1 -> this.configure(JsApi.arg(var1, 0)));
      this.fn("cookies", var1 -> new LinkedHashMap<>(this.cookieJar));
      this.fn("mock", var1 -> this.mock(JsApi.arg(var1, 0), JsApi.arg(var1, 1)));
      this.fn("serve", var1 -> this.serve(JsApi.arg(var1, 0), JsApi.arg(var1, 1)));
      this.fn("app", var1 -> this.app(JsApi.arg(var1, 0)));
      this.fn("help", var0 -> help(JsApi.str(var0, 0)));
   }

   public Http withBaseUrl(String var1) {
      this.baseUrl = var1;
      return this;
   }

   public Http withRulesHome(Path var1) {
      if (var1 != null) {
         this.rulesHome = var1;
      }

      this.mocks.a(var1);
      return this;
   }

   public Http withRoot(Path var1) {
      return this.withRoot(var1, var1);
   }

   public Http withRoot(Path var1, Path var2) {
      this.root = var1;
      this.classpathRoot = var2;
      this.mocks.a(var1, var2);
      return this;
   }

   public Http onMockStart(Consumer<MockInstance> var1) {
      this.mocks.a(var1);
      return this;
   }

   public Http onExchange(Consumer<Map<String, Object>> var1) {
      this.onExchange = var1;
      return this;
   }

   public Http onAppStart(Consumer<Runnable> var1) {
      this.onAppStart = var1;
      return this;
   }

   private Object send(String var1, Object var2, Object var3, Object var4) {
      if (var2 instanceof String var5 && !var5.isBlank()) {
         String var6 = this.resolve(var5);
         if (var6 == null) {
            return JsApi.missing(
               "'" + var5 + "' is not an absolute url and no baseUrl is set — pass a full http(s):// url, or call Http.configure({baseUrl:'…'}) first"
            );
         } else {
            Map var7 = var4 instanceof Map var8 ? var8 : Map.of();
            boolean var14 = Boolean.TRUE.equals(var7.get("full"));

            try {
               io.karatelabs.http.Http var9 = io.karatelabs.http.Http.to(var6);
               this.effectiveHeaders(var7).forEach((var1x, var2x) -> var9.header(var1x, String.valueOf(var2x)));
               if ((var7.get("query") != null ? var7.get("query") : var7.get("params")) instanceof Map var11) {
                  var11.forEach((var1x, var2x) -> var9.param(String.valueOf(var1x), String.valueOf(var2x)));
               }

               HttpResponse var15 = var9.method(var1, var3);
               this.captureCookies(var15);
               this.recordExchange(var15);
               var var12 = this.projected(var15, var14);
               this.specCheck(var15, var12);
               return var12;
            } catch (RuntimeException var13) {
               return this.errorResult(var1, var6, var13);
            }
         }
      } else {
         return JsApi.missing(
            "Http."
               + var1.toLowerCase()
               + "(url, …) requires a url String, e.g. Http."
               + var1.toLowerCase()
               + "('"
               + (this.baseUrl != null ? "/users" : "http://localhost:8080/users")
               + "')"
         );
      }
   }

   private void specCheck(HttpResponse var1, Map<String, Object> var2) {
      if (this.specModel != null) {
         try {
            HttpRequest var3 = var1.getRequest();
            if (var3 == null) {
               return;
            }

            LinkedHashMap var4 = new LinkedHashMap();
            io.karatelabs.ext.openapi.OpenApiOperationCatalog.OperationInfo var5 = this.specModel.findOperation(var3.getMethod(), var3.getUrlAndPath());
            if (var5 == null) {
               var4.put("matched", false);
               var4.put("note", "no declared operation matches this request — the probe went off-spec");
               var2.put("spec", var4);
               return;
            }

            String var6 = var5.a() != null ? var5.a() : var5.b().toUpperCase() + " " + var5.c();
            var4.put("opId", var6);
            OpenApiSchemaNode var7 = OpenApiDocumentQueries.a(this.specModel.document(), var6, String.valueOf(var1.getStatus()));
            if (var7 == null) {
               var7 = OpenApiDocumentQueries.a(this.specModel.document(), var6, "default");
            }

            Object var8 = safeBody(var1);
            if (var7 == null) {
               var4.put("conforms", null);
               var4.put("note", "the spec declares no response schema for status " + var1.getStatus() + " — nothing to validate against");
            } else if (!(var8 instanceof Map) && !(var8 instanceof List)) {
               var4.put("conforms", null);
               var4.put("note", "non-JSON body — schema validation skipped");
            } else {
               try {
                  ValidationResult var9 = var7.validateWithDirection(var8, io.karatelabs.ext.jsonschema.validation.ValidationDirection.RESPONSE);
                  var4.put("conforms", var9.valid);
                  if (!var9.valid) {
                     var4.put("errors", var9.toMap().get("errors"));
                  }
               } catch (ValidationException var10) {
                  var4.put("conforms", null);
                  var4.put("note", "the declared schema could not be evaluated: " + var10.getMessage());
               }
            }

            var2.put("spec", var4);
         } catch (RuntimeException var11) {
         }
      }
   }

   private Object sendSpec(Object var1) {
      if (var1 instanceof Map var2) {
         String var3 = var2.get("method") instanceof String var4 ? var4.toUpperCase() : "GET";
         return this.send(var3, var2.get("url"), var2.get("body"), var2);
      } else {
         return JsApi.missing("Http.send({method, url, body?, headers?, query?, full?}) needs an options object");
      }
   }

   private String resolve(String var1) {
      if (var1.startsWith("http://") || var1.startsWith("https://")) {
         return var1;
      } else if (this.baseUrl != null && !this.baseUrl.isBlank()) {
         String var2 = this.baseUrl.endsWith("/") ? this.baseUrl.substring(0, this.baseUrl.length() - 1) : this.baseUrl;
         return var2 + (var1.startsWith("/") ? var1 : "/" + var1);
      } else {
         return null;
      }
   }

   private Map<String, Object> effectiveHeaders(Map<?, ?> var1) {
      var var2 = new LinkedHashMap<>(this.configuredHeaders);
      if (this.headerFn != null) {
         try {
            if (this.headerFn.invoke() instanceof Map var4) {
               var4.forEach((var1x, var2x) -> var2.put(String.valueOf(var1x), var2x));
            }
         } catch (RuntimeException var6) {
         }
      }

      String var7 = this.cookieHeader();
      if (var7 != null) {
         var2.putIfAbsent("Cookie", var7);
      }

      if (var1.get("headers") instanceof Map var8) {
         var8.forEach((var1x, var2x) -> var2.put(String.valueOf(var1x), var2x));
      }

      return var2;
   }

   private String cookieHeader() {
      if (this.cookieJar.isEmpty()) {
         return null;
      } else {
         StringBuilder var1 = new StringBuilder();
         this.cookieJar.forEach((var1x, var2) -> {
            if (var1.length() > 0) {
               var1.append("; ");
            }

            var1.append(var1x).append('=').append(var2);
         });
         return var1.toString();
      }
   }

   private void captureCookies(HttpResponse var1) {
      var var2 = var1.getHeaders();
      if (var2 != null) {
         for (Entry var4 : (Iterable<Entry>) (Iterable<?>) (var2.entrySet())) {
            if ("set-cookie".equalsIgnoreCase((String)var4.getKey()) && var4.getValue() != null) {
               for (String var6 : (Iterable<String>) (Iterable<?>) ((List)var4.getValue())) {
                  String var7 = var6.split(";", 2)[0].trim();
                  int var8 = var7.indexOf(61);
                  if (var8 > 0) {
                     this.cookieJar.put(var7.substring(0, var8).trim(), var7.substring(var8 + 1).trim());
                  }
               }
            }
         }
      }
   }

   public void captureStart() {
      if (this.capture == null) {
         this.capture = new ArrayList<>();
      }
   }

   public List<Map<String, Object>> captureDrain() {
      List var1 = this.capture == null ? List.of() : this.capture;
      this.capture = null;
      return var1;
   }

   private void recordExchange(HttpResponse var1) {
      if ((this.capture != null || this.onExchange != null) && var1.getRequest() != null) {
         try {
            LinkedHashMap var2 = new LinkedHashMap();
            var2.put("request", var1.getRequest().toMap());
            var2.put("response", var1.toMap());
            if (this.capture != null) {
               this.capture.add(var2);
            }

            if (this.onExchange != null) {
               this.onExchange.accept(var2);
            }
         } catch (RuntimeException var3) {
         }
      }
   }

   private Map<String, Object> projected(HttpResponse var1, boolean var2) {
      LinkedHashMap var3 = new LinkedHashMap();
      var3.put("status", var1.getStatus());
      var3.put("ok", var1.getStatus() < 400);
      if (var1.getContentType() != null) {
         var3.put("contentType", var1.getContentType());
      }

      var3.put("timeMs", var1.getResponseTime());
      var3.put("headers", flatHeaders(var1.getHeaders()));
      var var4 = requestEcho(var1);
      if (var4 != null) {
         var3.put("request", var4);
      }

      String var5 = var1.getBodyString();
      int var6 = var5 == null ? 0 : var5.length();
      Object var7 = safeBody(var1);
      if (var7 instanceof byte[] var11) {
         var3.put("bodyBinary", true);
         var3.put("bodyBytes", var11.length);
         return JsApi.hint(var3, "binary body (" + var1.getContentType() + ", " + var11.length + " bytes) — not inlined");
      } else {
         if (var7 != null
            && !(var7 instanceof Map)
            && !(var7 instanceof List)
            && !(var7 instanceof String)
            && !(var7 instanceof Number)
            && !(var7 instanceof Boolean)) {
            var7 = var5;
         }

         boolean var8 = false;
         if (var7 == null) {
            var3.put("body", null);
         } else if (var2 || var6 <= 1500) {
            var3.put("body", var7);
         } else if (var7 instanceof String var9) {
            var3.put("body", var9.substring(0, 1500) + "…");
            var8 = true;
         } else {
            var3.put("bodyShape", shapeOf(var7, 2));
            if (var7 instanceof List var10 && !var10.isEmpty()) {
               var3.put("sample", new ArrayList(var10.subList(0, Math.min(2, var10.size()))));
            }

            var8 = true;
         }

         if (var8) {
            var3.put("bodyTruncated", true);
            var3.put("bodyChars", var6);
            JsApi.hint(var3, "large body summarized to its shape (" + var6 + " chars) — pass {full:true} in opts to get the whole body");
         }

         return var3;
      }
   }

   private static Map<String, Object> requestEcho(HttpResponse var0) {
      HttpRequest var1 = var0.getRequest();
      if (var1 == null) {
         return null;
      } else {
         LinkedHashMap var2 = new LinkedHashMap();
         var2.put("method", var1.getMethod());
         var2.put("url", var1.getUrlAndPath());
         var2.put("headers", flatHeaders(var1.getHeaders()));
         return var2;
      }
   }

   private static Object safeBody(HttpResponse var0) {
      try {
         return var0.getBodyConverted();
      } catch (RuntimeException var2) {
         return var0.getBodyString();
      }
   }

   private static Object shapeOf(Object var0, int var1) {
      if (var0 == null) {
         return "null";
      } else if (var0 instanceof Map var4) {
         if (var1 <= 0) {
            return "object{" + var4.size() + " keys}";
         } else {
            LinkedHashMap var5 = new LinkedHashMap();
            var4.forEach((var2x, var3x) -> var5.put(String.valueOf(var2x), shapeOf(var3x, var1 - 1)));
            return var5;
         }
      } else if (var0 instanceof List var2) {
         LinkedHashMap var3 = new LinkedHashMap();
         var3.put("array", var2.size());
         var3.put("of", var2.isEmpty() ? "?" : shapeOf(var2.get(0), var1));
         return var3;
      } else if (var0 instanceof Boolean) {
         return "boolean";
      } else {
         return var0 instanceof Number ? "number" : "string";
      }
   }

   private static Map<String, Object> flatHeaders(Map<String, List<String>> var0) {
      LinkedHashMap var1 = new LinkedHashMap();
      if (var0 != null) {
         var0.forEach((var1x, var2) -> var1.put(var1x, var2 != null && var2.size() == 1 ? var2.get(0) : var2));
      }

      return var1;
   }

   private Map<String, Object> errorResult(String var1, String var2, RuntimeException var3) {
      LinkedHashMap var4 = new LinkedHashMap();
      var4.put("ok", false);
      var4.put("error", JsApi.jsErrorMessage(var3));
      var4.put("method", var1);
      var4.put("url", var2);
      return JsApi.hint(var4, "the request failed before/while connecting — check the url/port (is the mock or server up?) and the headers");
   }

   private Object configure(Object var1) {
      if (!(var1 instanceof Map var2)) {
         return JsApi.missing(
            "Http.configure({baseUrl?, headers?, spec?}) needs an options object — e.g. Http.configure({baseUrl:'http://localhost:8080', headers:{Authorization:'Bearer '+token}})"
         );
      } else {
         if (var2.get("baseUrl") instanceof String var3) {
            this.baseUrl = var3;
         }

         Object var9 = var2.get("headers");
         if (var9 instanceof JavaInvokable var10) {
            this.headerFn = var10;
         } else if (var9 instanceof Map var5) {
            this.headerFn = null;
            this.configuredHeaders.clear();
            var5.forEach((var1x, var2x) -> this.configuredHeaders.put(String.valueOf(var1x), var2x));
         }

         if (var2.containsKey("spec")) {
            Object var11 = var2.get("spec");
            if (var11 != null && !(var11 instanceof String var6 && var6.isBlank())) {
               if (!(var11 instanceof String var13)) {
                  return JsApi.missing("Http.configure: `spec` must be a spec path String (or null to disarm)");
               }

               io.karatelabs.ext.common.keycheck.Main.requireProduct("http.spec", "openapi");

               try {
                  this.specModel = io.karatelabs.ext.openapi.OpenApiOperationCatalog.loadFromResource(MaxPaths.resolveResource(this.root, this.classpathRoot, var13));
                  this.specConfigured = var13;
               } catch (RuntimeException var8) {
                  return JsApi.missing("Http.configure: could not parse spec '" + var13 + "': " + var8.getMessage());
               }
            } else {
               this.specModel = null;
               this.specConfigured = null;
            }
         }

         LinkedHashMap var12 = new LinkedHashMap();
         var12.put("baseUrl", this.baseUrl);
         var12.put("headers", this.headerFn != null ? "function (re-evaluated per request)" : new LinkedHashMap<>(this.configuredHeaders));
         if (this.specConfigured != null) {
            var12.put("spec", this.specConfigured);
         }

         return JsApi.hint(
            var12,
            "configured — Http.get/post/… now use these defaults; cookies set by responses persist across calls automatically (Http.cookies() to inspect)"
               + (this.specConfigured != null ? "; every response now carries a spec:{opId, conforms|errors} contract check" : "")
         );
      }
   }

   public Object mock(Object var1, Object var2) {
      return this.mocks.a(var1, var2);
   }

   Object app(Object var1) {
      if (var1 instanceof Map var2 && var2.get("twin") instanceof String var3 && !var3.isBlank()) {
         for (Object var5 : var2.keySet()) {
            if (!APP_OPTIONS.contains(String.valueOf(var5))) {
               return JsApi.missing("Http.app takes twin · target · console · dir · port, not '" + var5 + "'");
            }
         }

         String var12 = var2.get("console") instanceof String var13 && !var13.isBlank() ? var13 : null;
         int var14 = var2.get("port") instanceof Number var15 ? var15.intValue() : 0;

         PlayApp var16;
         try {
            Path var17 = MaxPaths.resolvePath(
               this.root == null ? Path.of(".") : this.root, var2.get("dir") instanceof String var8 && !var8.isBlank() ? var8 : "app"
            );
            var16 = PlayApp.a(var3, this.rulesHome, var2.get("target"), var17, var12, var14);
         } catch (RuntimeException var10) {
            return JsApi.error(var10);
         }

         if (this.onAppStart != null) {
            this.onAppStart.accept(var16::c);
         }

         return new Http.AppHandle(var16);
      } else {
         return JsApi.missing(
            "Http.app({twin, target, console?, dir?, port?}) — twin names the rulebook, target is the handle Http.mock({twin, record: true}) returned"
         );
      }
   }

   Object serve(Object var1, Object var2) {
      if (var1 == null) {
         return JsApi.missing("Http.serve(dir[, port]) needs a directory to serve, e.g. Http.serve('sut', 9100)");
      } else {
         Path var3 = MaxPaths.resolvePath(this.root, String.valueOf(var1));
         if (!Files.isDirectory(var3)) {
            return JsApi.missing("Http.serve: not a directory: " + var3);
         } else {
            int var4 = var2 instanceof Number ? ((Number)var2).intValue() : 0;
            HttpServer var5 = io.karatelabs.ext.common.StaticFileServer.a(var3, var4);
            return new Http.StaticServerHandle(var5, var3);
         }
      }
   }

   private static Map<String, Object> help(String var0) {
      if (var0 == null || var0.isBlank()) {
         var var4 = new LinkedHashMap<>(help());
         var4.put("topics", HELP_TOPICS);
         var4.put("usage", "Http.help('topic')");
         return var4;
      } else if ("mock".equalsIgnoreCase(var0.trim())) {
         var var3 = new LinkedHashMap<>(mockHelp());
         var3.put("topic", "mock");
         return var3;
      } else if ("app".equalsIgnoreCase(var0.trim())) {
         var var2 = new LinkedHashMap<>(appHelp());
         var2.put("topic", "app");
         return var2;
      } else {
         LinkedHashMap var1 = new LinkedHashMap();
         var1.put("error", "unknown topic: '" + var0 + "'. Call Http.help() to list topics.");
         var1.put("topics", HELP_TOPICS);
         return var1;
      }
   }

   private static Map<String, Object> mockHelp() {
      return JsApi.markdown(
         "# Http.mock — a live HTTP mock, and what a handler script sees\n\nThree flavors, one live handle — data: `mock.url` · `mock.port` · `mock.ssl` · `mock.calls`\n(recomputed per read) · `mock.warnings` · `mock.draft`; verbs: `mock.stop()` · `mock.reset()` ·\n`mock.restart()` · `mock.var('pets')` (peek at one session key from outside) ·\n`mock.reprogram(…)` · `mock.freeze(…)` (raw-JS flavor):\n\n**Change a running raw-JS mock's program IN PLACE — `mock.reprogram({js|file,\nstate:'keep'|'reset'})`.** Same port, `mock.calls` history kept; `state` is REQUIRED —\n`'keep'` carries the live `session` store across the swap, `'reset'` pairs the new program\nwith a fresh boot-seeded store. A program that fails to parse — or dies on a pre-swap probe\nrequest — is REJECTED and the prior program keeps serving. This is the iterate verb: never\n`stop()` + recreate to change behavior (a stopped mock also releases its port\nasynchronously, so an immediate rebind can fail).\n\n**Draft vs frozen — `mock.draft` + `mock.freeze('mock/<name>.js')`.** Inline `js` (at start\nor reprogram) is a DRAFT: session-authored, backed by no file — while any draft mock is\nlive, runs it serves are quarantined under `target/draft/` and never enter evidence (the\nrun's own reply says so). `freeze` writes the current program to the named project file\n(headed by the `@ai` review-pending marker — a human commit clears it), flips `draft` off,\nand from then on runs count. An existing target needs `{overwrite:true}`. A `{file:...}`\nmock is already the program of record.\n\n**`restart()` is the clean start; `reset()` says how close it got.** `mock.restart()` puts a\nmock back in the state it booted in — for a **mock feature** it rebuilds the handler, so the\n`Background` runs again from the same parsed feature — **on the same port**, so the URL and\nthis handle stay valid. Reach for it between phases, and between two paired runs against one\nstand-in: that is the shape this bites, and an inherited row reads as a divergence about the\nmock rather than about your setup.\n\n`mock.reset()` clears the **call log** and the flavor's **store** (the `session` for a raw-JS\nhandler, the CRUD store for a spec-fulfilling mock — seed `arg` restored, not dropped). It\ndoes **not** reach a mock feature's `Background` variables (a `seq` counter, a `policies`\nlist): those live in the engine the feature built, not in the session. So `reset()` **answers\nwhat it did** — `{calls, cleared, survives?, clean, restart?}` — and `clean:false` names\n`restart()`. Read the answer: zero calls is also what a contaminated mock looks like.\n\nAn edited mock feature is a different question — `restart()` deliberately re-runs the feature\nthat was parsed at start, so it is deterministic; to pick up a file change, `stop()` and\n`Http.mock(...)` again (a raw-JS mock takes `mock.reprogram({file: '...'})` instead — no\nstop, no rebind).\n\n- **spec-fulfilling** — `Http.mock('api.yaml')` / `Http.mock({openapi:'api.yaml', …opts})`.\n  Schema-valid sample responses, stateful **CRUD by convention** (POST creates → GET returns\n  it → PUT/PATCH update → DELETE removes), per-operation `rules`/`handlers` overlays, and\n  `validate:true` to reject a request that violates the spec. (openapi-gated.)\n- **mock feature** — `Http.mock('mock/api.feature')` / `Http.mock({feature:'mock/api.feature'})`.\n  The karate mock feature your project already ships, served from **here** — the console has\n  no feature runtime, so `karate.start(...)` (and any project starter script that calls it)\n  is not available to you; this is the console-side equivalent. (ungated.)\n- **raw JS handler** — `Http.mock({js:'…source…'})` / `Http.mock({file:'mock.js'})`. ONE\n  script, evaluated for **every** request; the script does its own routing. (ungated.)\n- **twin-backed** — `Http.mock({twin:'claims', port?, record?})`. `Rule.project`'s spec is\n  the router, the twin's commands answer, `mock.var('worlds')` is the state; `record:true`\n  writes each world's play under `target/draft/play/`. `rules` beside it is\n  `MOCK_BRAIN_CONFLICT`.\n\n`opts`: `rules` · `handlers` · `resources` · `crud` · `validate` · `port` (0 = pick a free\none) · `host` (bind interface — default: every interface, EXCEPT a fault-armed mock which\nbinds loopback; see the fault feed below) · `arg` · `faults` (below) · (spec flavor)\n`basePath` — the mock serves under the spec's declared `servers`/`basePath` prefix; pass one\nto serve under that instead, `''` to serve unprefixed · (feature flavor)\n`pathPrefix` · `javaBridge` ·\n`requestExpressions`. In a\n`karate run` suite, `boot.ext('http')` binds `Http` in-run and auto-stops the mock at suite\nend. A project starter script that is plain JS runs with `File.call('/start-mock.js')` — but\none that calls `karate.start(...)` will not, because `karate.*` is the feature runtime; reach\nfor the **mock feature** flavor above instead.\n\n## The fault feed — the mock told to misbehave (resilience testing)\n\n`Http.mock(…, {faults:'mutation/fault-manifest.json'})` arms ANY flavor with a **frozen fault\ndeck** — authored ways the mocked dependency will deliberately misbehave, so what gets graded\nis **the consumer that depends on it** (its verdicts are the resilience suite's own authored\nassertions: a fault the consumer tolerates is a PASS). The manifest is\n`{faults:[{id?, operator, operation, …}]}` with `operation` = `\"METHOD /path\"` (`{braces}`\nsegments match anything); operators: the response tampers `FIELD_DROP`/`FIELD_NULL`/`TYPE_FLIP`/\n`VALUE_OFF_BY_ONE`/`CARDINALITY_EMPTY`/`ENUM_SUBSTITUTION` (`field`, ENUM needs `value`) ·\n`STATUS_SWAP` (`to`, optional `from`) · `MALFORMED_BODY` · `CONTENT_TYPE_TAMPER` — plus the\ntransport faults `TRANSPORT_DELAY` (`delayMs` — slow or timeout, your magnitude) and\n`TRANSPORT_RESET` (the connection dies mid-request, nothing written). A manifest that could\nsilently do nothing refuses to load.\n\nOne fault is **armed** at a time (arming replaces — a request is owned by the fault armed\nwhen it arrived; run judging scenarios sequentially), via either face of the same state:\nthe handle verb — `mock.fault('id')` arms, `mock.fault(null)`\ndisarms, `mock.fault()` reads `{armed, faults, served}` — or the mock's own wire endpoint\n`POST <mock.url>/__karate/fault` with `{id: '<fault id>'}` (`{id: null}` disarms, GET\nreads; any other body is refused with 400 — nothing else changes state), so a suite that\nonly knows the URL can drive it. Every tampered response is stamped\n`Karate-Mutant: <id>`, and the `served` log\nattributes each fault to the calls it hit (a connection-reset fault writes nothing, so it\nis attributed in the served log only). Fault-fed runs are **instrument runs** — write\ntheir output under `target/`, never `runs/`, and none of this ever enters a mutation score.\n\nThree rails to know: the wire control is **unauthenticated**, so a fault-armed mock binds\n**loopback by default** — pass `host` (e.g. `'0.0.0.0'`) only when the misbehaving\ndependency must be reachable off-box, knowingly exposing the control to that network. The\ncontrol path `/__karate/fault` is **reserved** — a mock route at exactly that path is\nshadowed while faults are declared. And fault state deliberately **survives\n`mock.reset()`/`restart()`** (arming is per-scenario config, not mock state) — the reset\nanswer's `survives` list says so, and `mock.fault(null)` disarms.\n\n## The handler contract\n\nA handler script is plain JS evaluated **per request** with three globals in scope —\n`request` (read), `response` (write), `session` (read/write, persists).\n**The script's return value is ignored** — you *mutate `response`*; there is nothing to\nreturn. Requests are serialized on the session, so read-then-write state\n(`push`/`splice`) is safe.\n\n**`request`** — everything about the call:\n\n| member | is |\n|---|---|\n| `request.method` | `'GET'` / `'POST'` … (uppercase) |\n| `request.get` `.post` `.put` `.delete` `.patch` `.head` `.options` `.trace` | booleans — the terse method test |\n| `request.path` | the path, no query string · `request.pathRaw` keeps the raw form |\n| `request.url` · `request.urlBase` | full url · scheme+host+port |\n| `request.pathMatches('/pets/{id}')` | boolean AND it fills `request.pathParams` — the routing primitive |\n| `request.pathParams` | `{id:'42'}` after a `pathMatches` |\n| `request.body` | the parsed body (JSON → object/array) · `.bodyString` raw text · `.bodyJson` · `.bodyBytes` |\n| `request.param('q')` · `.paramInt('n')` · `.paramValues('tag')` · `.params` | query params (one · as int · repeated · all) |\n| `request.header('X-Token')` · `.headerValues('accept')` · `.headers` | headers (case-insensitive lookup) |\n| `request.cookies` | the cookie map |\n| `request.multiPart('f')` · `.multiParts` · `.file('f')` · `.files('f')` | multipart / upload parts (one · all · one file · repeated files) |\n| `request.proceed(url?)` | forward this request on and return the real response — a pass-through/record shim |\n\n**`response`** — assign to it:\n\n- `response.status = 201` (default 200).\n- `response.body = {…}` — a **map/list** sends JSON, a **String** text, an **XML node** XML,\n  a **byte[]** raw bytes; the Content-Type follows the type you assign.\n- `response.headers = {'X-Trace':'abc'}` — merged into what is already there.\n- Readable too (`response.status`, `response.body`, `response.bodyString`) — which is what\n  makes the *overlay* form below possible. `response.body` is a **live view**: mutate it in\n  place (`response.body.x = 1`) and that is what gets served.\n\n**`session`** — a plain mutable store that lives as long as the mock: `session.pets`,\n`session.nextId`, anything. Undefined keys read as `null`, so the idiom is\n`session.pets = session.pets || []`. Seed it up front with `Http.mock({js:…, arg:{pets:[…]}})`,\nread it back from outside with `mock.var('pets')`, and wipe it with `mock.reset()`.\n\n**A handler script sees `request`, `response` and `session` and NOTHING else** — no\n`Rule`, no `Http`, no `File`. To reach one, seed a **function** through `arg`: it is\nbuilt here and keeps its own scope, so the handler calls it as `session.calc(row)` —\nthe way a raw-JS mock computes a response from a Rulebook.\n\n```js\nHttp.mock({file:'mock.js', arg:{calc: function(row) { return Rule.execute('pricing', row) }}})\n```\n\n```js\nvar mock = Http.mock({js:\n  \"if (request.get && request.pathMatches('/pets/{id}')) {\" +\n  \"  var found = (session.pets || []).find(function(p) { return p.id == request.pathParams.id });\" +\n  \"  if (found) { response.body = found } else { response.status = 404; response.body = {error:'no such pet'} }\" +\n  \"} else if (request.post && request.pathMatches('/pets')) {\" +\n  \"  session.pets = session.pets || []; session.nextId = (session.nextId || 0) + 1;\" +\n  \"  var pet = request.body; pet.id = session.nextId; session.pets.push(pet);\" +\n  \"  response.status = 201; response.body = pet\" +\n  \"} else { response.status = 404 }\"\n})\nHttp.post(mock.url + '/pets', {name:'Rex'}).body.id   // 1\n```\n\nLong scripts belong in a file — `Http.mock({file:'mock-handler.js'})` (project-relative)\nkeeps them editable with `File.read`/`File.write`.\n\n## Per-operation handlers (the spec-fulfilling mock)\n\n`Http.mock('api.yaml', {handlers:{…}})` overlays ONE operation, keyed by **operationId** or\n`\"METHOD /path\"` (`{'POST /pets': \"…\"}`). The same three globals are in scope, with one\ndifference that is the whole point: **`response` arrives pre-filled** by CRUD/the sample, so\na handler *tweaks* rather than builds —\n\n```js\nHttp.mock('api.yaml', {handlers: {\n  // the default is already there — just tweak it\n  'POST /pets': \"response.body.status = 'PENDING'\",\n  getPetById: \"if (request.pathMatches('/pets/{id}') && request.pathParams.id == '999')\"\n            + \" { response.status = 410; response.body = {error:'gone'} }\"\n}})\n```\n\nNote the `pathMatches` even here: this mock routes to the operation internally and does\n**not** fill `request.pathParams` for you — that map is populated by your own `pathMatches`\ncall, in either flavor.\n\nA handler may also be a **function** (from `karate-boot.js`), called as\n`function(request, response, session) { … }`. A key matching no operation in the spec comes\nback on `mock.warnings` rather than failing silently.\n\nFor domain logic that a test would otherwise pin as a golden number, prefer `rules` over a\nhandler — `Http.mock('api.yaml', {rules:{opId:'rulebook'}})` computes the response from the\nsame Rulebook that `Rule.execute` runs as the oracle (see the `rule-authoring` skill)."
      );
   }

   private static Map<String, Object> appHelp() {
      return JsApi.markdown(
         "# Http.app — the playground app over a recording twin mock\n\n`Http.app({twin, target, console?, dir?, port?})` → `{url, port, stop()}`. `target` is the\nhandle `Http.mock({twin, record: true})` returned; a URL or a mock that does not record is\n`APP_TARGET_UNSUPPORTED`. `console` is the served console's URL — each timeline then links\nto its record there. `dir` is the project's app folder (default `app/`); `port` 0 = ephemeral.\n\nThree levels, none of which changes what is sent — every submit goes through the command's\nbinding:\n1. **derived** — nothing authored: a table of the resource, a detail page whose actions are\n   enabled per `Twin.next`, a timeline per record, `/docs` with a try-it per operation.\n2. **`app/ui.js`** — `ui = {title, subtitle, currency, resource, fields, commands, reasons}`:\n   labels, help, order, columns, formats, enum value labels and refusal wording, keyed by the\n   model's names; an undeclared name is `UI_UNKNOWN`.\n3. **shadow pages** — `app/<page>.html` replaces the base page of that name.\n\nThe running app re-reads `ui.js` and shadow pages on every request: write the file\n(`File.write`) and refresh. `Twin.check(rule).findings.ui` lints both without a boot. Author\nit: `Skill.help('app-authoring')`."
      );
   }

   private static Map<String, Object> help() {
      return JsApi.markdown(
         "# Http — explore a live API, then write the karate test\n\nA **stateful** HTTP client for exploring an API surface from limited info — a mock you just\nstood up (`Http.mock`), or an under-documented real backend — to discover its business\nrules and stateful behavior, then crystallize what you learn into a karate `.feature` test\n(which asserts with the Gherkin `match` keyword). Prefer this over `curl` for anything\niterative: it is token-frugal, stateful, and keeps you in karate's model (a one-off probe with\n`curl` is fine).\n\n**Verbs:**\n- `Http.get(url, opts?)` · `Http.post(url, body, opts?)` · `Http.put(url, body, opts?)` ·\n  `Http.patch(url, body, opts?)` · `Http.delete(url, opts?)`\n- `Http.send({method, url, body?, headers?, query?, full?})` — the generic form.\n- `Http.configure({baseUrl?, headers?, spec?})` — session defaults. `headers` may be a **map**\n  or a **function** (`function(){ return {Authorization:'Bearer '+token} }`) re-run per request.\n  `spec:'api.yaml'` arms **per-response contract validation**: every response gains a\n  `spec` block — `{opId, conforms:true}` (matches the declared response schema),\n  `{opId, conforms:false, errors}` (the contract DRIFTED — the thing curl + eyeballing\n  can't tell you), `{opId, conforms:null, note}` (nothing declared for that status), or\n  `{matched:false}` (the probe went off-spec). `spec:null` disarms.\n- `Http.cookies()` — the current cookie jar (session auth captured from `Set-Cookie`).\n- `Http.mock(arg, opts?)` — stand up a live HTTP mock (returns a `mock` handle: `mock.url`,\n  `mock.calls`, `mock.stop()`). **Polymorphic:**\n  - a **spec path** / `{openapi:'api.yaml'}` → a **spec-fulfilling** mock — schema-valid samples,\n    stateful **CRUD by convention**, per-op `rules`/`handlers`, `validate:true` (openapi-gated).\n  - a **mock feature path** / `{feature:'mock/api.feature'}` → the karate mock feature your\n    project already ships, served from here (ungated). This is how you stand up the mock a\n    project OWNS: the console has no feature runtime, so `karate.start(...)` — and any starter\n    script that calls it — is not available to you.\n  - `{js:'…source…'}` / `{file:'mock.js'}` → a **raw JS-handler** mock — one JS source per\n    request with `request`/`response`/`session` in scope (ungated).\n  - `{twin:'claims', port?, record?}` → the **twin-backed** mock: the projected spec is the\n    router and the rulebook's twin answers, each refusal the model's own.\n  - **`Http.help('mock')` is the mock page** — the handler contract (every `request.*`\n    member, what `response.body`/`status` accept, the `session` store, per-op handlers).\n  - opts: `rules`/`handlers`/`resources`/`crud`/`validate`/`port`/`arg`/`faults` (a frozen\n    fault manifest arms ANY flavor as a misbehaving dependency for resilience testing —\n    `mock.fault(id?)` + the `POST <mock.url>/__karate/fault` wire control; details on the\n    mock page). In a `karate run`\n    suite, `boot.ext('http')` makes `Http` available in-run + auto-stops the mock at suite end.\n  - **result shape (branch, don't blind-cast):** `Http.mock` returns **either** a live `mock`\n    handle (envelope `ref:{kind:'mock'}`, `payload` = `url`/`port`/`calls`) **or** an `{error}`\n    payload (bad spec, missing arg) — check for `error`/the `ref` before using it.\n\n- `Http.app({twin, target, console?, dir?, port?})` — the **playground app** over a recording\n  twin mock (`target` = the `Http.mock({twin, record: true})` handle): screens a business user\n  drives, every action a real call through the binding. **`Http.help('app')`**.\n\n- `Http.serve(dir, port?)` — stand up a **static-file** server over a directory (a kit's\n  `sut/` assets), returning `{url, port, stop()}`. Lets a `*.karate.js` launcher serve the SUT\n  so a browser check drives a real `http://…` app — no external `python -m http.server`. Call\n  `stop()` after the run (a one-shot launcher). (This is a server; the verbs above are the client.)\n\n**`opts`:** `headers` (map, per-call), `query`/`params` (map → query string), `full` (boolean —\nreturn the whole body instead of the summarized shape).\n\n**Why it beats curl (when iterating):**\n- **token-frugal** — a large body comes back as `bodyShape` (e.g. `{array:200, of:{id:'string',\n  name:'string'}}`) + a small `sample`, not 200 full objects; pass `{full:true}` for the lot.\n- **stateful** — cookies/session auth persist across calls automatically (no `-b/-c` jar).\n- **header injection** — a configured header *function* re-applies a refreshed token to every call.\n- **composable** — the result is JS you compose against (`r.body.id`), one transform from a\n  `.feature` step.\n- **transparent** — `r.request` echoes what was *actually sent* (method, url, and the headers\n  including the cookie/auth the stateful client injected), so you can verify the session —\n  `curl -v`, but structured.\n\n**Response shape:** `{ status, ok, contentType, timeMs, headers, request{method,url,headers\nsent}, body | (bodyShape + sample + bodyTruncated + bodyChars) }`.\n\n**The loop:** `Http.mock` to stand up a sandbox → `Http.*` to probe → infer the rules/stateful\nbehavior → write a `.feature` with `match` assertions that lock the response to the spec's shape\n(see the `api` skill). Related: `Openapi.summary` (orient in the spec first).\n\n**Making a probe count as coverage — it needs a live run around it.** Exploration is not\nthrown away, but a bare `Http.get` from the console is *not* in the graph: evidence capture\nis armed by the **live run**, so bracket the probes —\n`Harness.startLive()` → your `Http.*` calls → `Harness.stop()` — and the stop folds the\noperations you touched into the project graph as observed coverage (`mockOnly:true` when only\nyour own `Http.mock` answered). Without the bracket `Coverage.status(...)` stays `NOTCOVERED`\nwith `callCount:0`, however many probes you sent."
      );
   }

   private static final class AppHandle extends JsDispatch {
      AppHandle(PlayApp var1) {
         this.val("url", var1.a());
         this.val("port", var1.b());
         this.fn("stop", var1x -> {
            var1.c();
            return null;
         });
      }

      @Override
      public Collection<String> jsKeys() {
         return List.of("url", "port");
      }

      @Override
      public String jsKind() {
         return "app";
      }
   }

   private static final class StaticServerHandle extends JsDispatch {
      StaticServerHandle(HttpServer var1, Path var2) {
         this.val("url", "http://localhost:" + var1.getPort());
         this.val("port", var1.getPort());
         this.val("dir", var2.toString());
         this.fn("stop", var1x -> {
            var1.stopAndWait();
            return null;
         });
      }

      @Override
      public Collection<String> jsKeys() {
         return List.of("url", "port", "dir");
      }

      @Override
      public String jsKind() {
         return "server";
      }
   }
}
