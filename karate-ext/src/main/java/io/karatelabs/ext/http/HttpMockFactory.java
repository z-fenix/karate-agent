package io.karatelabs.ext.http;

import io.karatelabs.common.Json;
import io.karatelabs.core.MockHandler;
import io.karatelabs.ext.common.MaxPaths;
import io.karatelabs.ext.common.MockStarter;
import io.karatelabs.ext.common.jsapi.JsApi;
import io.karatelabs.ext.mutation.FaultInjectionHandler;
import io.karatelabs.ext.mock.JsRequestHandler;
import io.karatelabs.ext.mock.MockInstance;
import io.karatelabs.ext.mock.OpenApiMockHandler;
import io.karatelabs.ext.mock.ResourceResolver;
import io.karatelabs.ext.openapi.OpenApiOperationCatalog;
import io.karatelabs.ext.openapi.OpenApiDocument;
import io.karatelabs.ext.rules.RuleApiException;
import io.karatelabs.ext.rules.RulebookRepository;
import io.karatelabs.ext.rules.TwinRuleRuntime;
import io.karatelabs.ext.rules.RuleEvaluator;
import io.karatelabs.ext.rules.twin.TwinModelLoader;
import io.karatelabs.ext.rules.twin.TwinHttpRouter;
import io.karatelabs.ext.rules.twin.TwinModel;
import io.karatelabs.ext.rules.twin.OpenApiProjector;
import io.karatelabs.gherkin.Feature;
import io.karatelabs.http.HttpRequest;
import io.karatelabs.http.HttpResponse;
import io.karatelabs.http.Session;
import io.karatelabs.js.Engine;
import java.net.BindException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class HttpMockFactory {
   private Path b = Path.of("rules");
   private Path c;
   private Path d;
   private final List<MockInstance> e = new CopyOnWriteArrayList<>();
   private Consumer<MockInstance> f;
   public static final MockStarter a = (var0, var1, var2, var3, var4) -> new HttpMockFactory().a(var0, var1).a(var2).a(var3, var4);
   private static final List<String> g = List.of("twin", "port", "record");

   public boolean a() {
      this.e.removeIf(MockInstance::isStopped);
      return this.e.stream().anyMatch(MockInstance::isDraftLive);
   }

   public HttpMockFactory a(Path var1) {
      if (var1 != null) {
         this.b = var1;
      }

      return this;
   }

   public HttpMockFactory a(Path var1, Path var2) {
      this.c = var1;
      this.d = var2;
      return this;
   }

   public HttpMockFactory a(Consumer<MockInstance> var1) {
      this.f = var1;
      return this;
   }

   public Object a(Object var1, Object var2) {
      try {
         if (var1 instanceof String var3 && !var3.isBlank()) {
            return a(var3) ? this.a(var2 instanceof Map var10 ? var10 : Map.of(), var3) : this.a(var3, var2);
         }

         if (var1 instanceof Map var7) {
            if (var7.get("twin") instanceof String var4 && !var4.isBlank()) {
               return this.a(var4, var7);
            }

            if (var7.get("openapi") instanceof String var8 && !var8.isBlank()) {
               return this.a(var8, var7);
            }

            if (var7.get("feature") instanceof String var9 && !var9.isBlank()) {
               return this.a(var7, var9);
            }

            if (this.f(var7) != null) {
               return this.a(var7);
            }
         }
      } catch (RuntimeException var6) {
         if (a((Throwable)var6)) {
            return JsApi.missing(
               "Http.mock: the port is still in use. A just-stopped mock releases its port ASYNCHRONOUSLY — retry in a moment, use {port: 0} for a dynamic port, or (raw-JS flavor) skip the stop+recreate dance entirely: mock.reprogram({js|file, state:'keep'|'reset'}) changes the RUNNING mock in place."
            );
         }

         throw var6;
      }

      return JsApi.missing(
         "Http.mock(spec | {openapi} | {feature} | {js|file}, opts?) — a spec path / {openapi:'api.yaml'} stands up a spec-fulfilling mock; a '.feature' path / {feature:'mock/api.feature'} serves a karate mock feature; {js:'…'} / {file:'mock.js'} a raw JS-handler mock"
      );
   }

   private static boolean a(Throwable var0) {
      for (Throwable var1 = var0; var1 != null; var1 = var1.getCause()) {
         if (var1 instanceof BindException) {
            return true;
         }
      }

      return false;
   }

   public static boolean a(String var0) {
      return MaxPaths.isFeature(var0);
   }

   private Object a(Object var1) {
      if (this.f != null && var1 instanceof MockInstance var2) {
         this.f.accept(var2);
      }

      return var1;
   }

   private Object a(Map<?, ?> var1) {
      String var2 = this.f(var1);
      if (var2 == null) {
         return JsApi.missing("Http.mock({js|file}) needs `js` (inline JS source) or `file` (path to a .js handler)");
      } else {
         FaultInjectionHandler var3;
         try {
            var3 = this.h(var1);
         } catch (RuntimeException var12) {
            return JsApi.missing("Http.mock faults: " + var12.getMessage());
         }

         int var4 = g(var1);
         Map var5 = var1.get("arg") instanceof Map var6 ? Map.copyOf(var6) : Map.of();
         AtomicReference var13 = new AtomicReference<>(b(var5));
         Runnable var14 = () -> {
            Session var2x = (Session)var13.get();
            ReentrantLock var3x = JsRequestHandler.lockOf(var2x);
            var3x.lock();

            try {
               var2x.getData().clear();
               var2x.getData().putAll(var5);
            } finally {
               var3x.unlock();
            }
         };
         JsRequestHandler var8 = new JsRequestHandler(var2, (Session)var13.get());
         MockInstance var9 = MockInstance.start(
               var8, var4, var14, var1x -> ((Session)var13.get()).getData().get(var1x), List.of(), var3 == null ? null : var3::withDelegate, a(var1, var3)
            )
            .withState(List.of("session"), a(var3), null);
         var9.setDraft(c(var1));
         AtomicReference var10 = new AtomicReference<>(var2);
         ReentrantLock var11 = new ReentrantLock();
         var9.withVerb("reprogram", var6x -> {
            var11.lock();

            Object var7;
            try {
               var7 = this.a(var9, JsApi.arg(var6x, 0), var5, var13, var10);
            } finally {
               var11.unlock();
            }

            return var7;
         });
         var9.withVerb("freeze", var4x -> {
            var11.lock();

            Object var5x;
            try {
               var5x = this.a(var9, JsApi.arg(var4x, 0), JsApi.arg(var4x, 1), var10);
            } finally {
               var11.unlock();
            }

            return var5x;
         });
         this.e.add(var9);
         return this.a(a(var9, var3));
      }
   }

   private static Session b(Map<String, Object> var0) {
      Session var1 = Session.inMemory();
      var1.getData().putAll(var0);
      return var1;
   }

   private static boolean c(Map<?, ?> var0) {
      return var0.get("js") instanceof String var1 && !var1.isBlank();
   }

   private Object a(MockInstance var1, Object var2, Map<String, Object> var3, AtomicReference<Session> var4, AtomicReference<String> var5) {
      if (var2 instanceof Map var6) {
         String var7;
         try {
            var7 = this.f(var6);
         } catch (RuntimeException var19) {
            return JsApi.missing("mock.reprogram could not read the program: " + var19.getMessage());
         }

         if (var7 == null) {
            return JsApi.missing("mock.reprogram needs {js: '<source>'} or {file: 'mock/x.js'} — the new program");
         } else {
            Object var8 = var6.get("state");
            if (!"keep".equals(var8) && !"reset".equals(var8)) {
               return JsApi.missing(
                  "mock.reprogram requires state:'keep' or state:'reset' — say explicitly whether the session store carries across the swap ('keep') or restores to the boot seed ('reset')"
               );
            } else {
               try {
                  Engine.parse(var7);
               } catch (Exception var18) {
                  return b("does not parse: " + var18.getMessage());
               }

               boolean var9 = c(var6);
               if ("reset".equals(var8)) {
                  String var10 = a(var7, null, var3);
                  if (var10 != null) {
                     return b(c(var10));
                  }

                  Session var11 = b(var3);
                  if (var9) {
                     var1.setDraft(true);
                  }

                  var1.swapHandler(new JsRequestHandler(var7, var11));
                  var4.set(var11);
               } else {
                  Session var21 = (Session)var4.get();
                  ReentrantLock var23 = JsRequestHandler.lockOf(var21);
                  var23.lock();

                  try {
                     String var12 = a(var7, var21, var3);
                     if (var12 != null) {
                        return b(c(var12));
                     }

                     if (var9) {
                        var1.setDraft(true);
                     }

                     var1.swapHandler(new JsRequestHandler(var7, var21));
                  } finally {
                     var23.unlock();
                  }
               }

               var5.set(var7);
               if (!var9) {
                  var1.setDraft(false);
               }

               LinkedHashMap var22 = new LinkedHashMap();
               var22.put("reprogrammed", true);
               var22.put("state", var8);
               var22.put("calls", var1.getCalls().size());
               var22.put("draft", var1.isDraft());
               var22.put(
                  "note",
                  "swap is atomic on the running port (in-flight requests finish on the prior program + its state); the probe catches a program that fails on ANY request, but a route-specific runtime error still surfaces per request — probe your routes before relying on it"
                     + (
                        var1.isDraft()
                           ? ". This mock is a DRAFT: runs against it are quarantined out of evidence until mock.freeze(path) promotes the program to a file"
                           : ""
                     )
               );
               return var22;
            }
         }
      } else {
         return JsApi.missing(
            "mock.reprogram({js|file, state:'keep'|'reset'}) — the new program (inline js, or a .js file path) plus the EXPLICIT state choice"
         );
      }
   }

   private static Map<String, Object> b(String var0) {
      var var1 = JsApi.missing("reprogram REJECTED — the new source " + var0);
      var1.put("serving", "the prior program is untouched and keeps serving");
      return var1;
   }

   private static String c(String var0) {
      return "fails on evaluation (probed with one synthetic request, against the state it would actually run with, before the swap): " + var0;
   }

   private static String a(String var0, Session var1, Map<String, Object> var2) {
      Session var3;
      if (var1 == null) {
         var3 = b(var2);
      } else {
         try {
            ReentrantLock var5 = JsRequestHandler.lockOf(var1);
            var5.lock();

            Map var4;
            try {
               var4 = Json.of(Json.stringifyStrict(var1.getData())).value();
            } finally {
               var5.unlock();
            }

            var3 = Session.inMemory();
            var3.getData().putAll(var4);
         } catch (Exception var12) {
            var3 = b(var2);
         }
      }

      try {
         HttpRequest var13 = new HttpRequest();
         var13.setMethod("GET");
         var13.setUrl("http://probe.local/__karate/reprogram-probe");
         new JsRequestHandler(var0, var3).apply(var13);
         return null;
      } catch (Exception var10) {
         return String.valueOf(var10.getMessage());
      }
   }

   private Object a(MockInstance var1, Object var2, Object var3, AtomicReference<String> var4) {
      if (!var1.isDraft()) {
         return JsApi.missing("nothing to freeze — this mock already serves a program of record on disk (freeze promotes a session-authored draft)");
      } else if (var2 instanceof String var5 && !var5.isBlank()) {
         String var6 = var5.trim().replace('\\', '/');
         if (!var6.endsWith(".js")) {
            return JsApi.missing("mock.freeze target must be a .js file (got '" + var6 + "')");
         } else {
            Path var7;
            try {
               var7 = MaxPaths.within(this.c, this.c.resolve(var6));
            } catch (SecurityException var14) {
               return JsApi.missing("mock.freeze denied: " + var14.getMessage());
            }

            boolean var8 = var3 instanceof Map var9 && Boolean.TRUE.equals(var9.get("overwrite"));
            if (Files.exists(var7) && !var8) {
               return JsApi.missing("mock.freeze: '" + var6 + "' already exists — pick another path, or pass {overwrite:true} to replace it deliberately");
            } else {
               String var15 = "// @ai — LLM-authored mock, human review pending (a human commit clears this marker)\n// frozen from a live Http.mock session (run binding unbound — record the run id in the commit if known); serve it with Http.mock({file: '"
                  + var6
                  + "'})\n";

               try {
                  Files.createDirectories(var7.getParent());
                  Path var10 = var7.resolveSibling(var7.getFileName() + ".freezing");
                  Files.writeString(var10, var15 + (String)var4.get() + "\n");

                  try {
                     Files.move(var10, var7, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
                  } catch (AtomicMoveNotSupportedException var12) {
                     Files.move(var10, var7, StandardCopyOption.REPLACE_EXISTING);
                  }
               } catch (Exception var13) {
                  return JsApi.missing("mock.freeze('" + var6 + "') failed: " + var13.getMessage());
               }

               var1.setDraft(false);
               LinkedHashMap var16 = new LinkedHashMap();
               var16.put("frozen", true);
               var16.put("path", var6);
               var16.put("draft", false);
               return JsApi.next(
                  var16,
                  "re-run your suite NOW — the quarantine is lifted, so this run records as evidence",
                  "review " + var6 + " and clear its @ai line in a human commit (the review gate)",
                  "Http.mock({file: '" + var6 + "'}) serves the frozen artifact from now on"
               );
            }
         }
      } else {
         return JsApi.missing(
            "mock.freeze('mock/<name>.js') needs the target file — the project-relative path the frozen program lives at, e.g. mock.freeze('mock/payment.js')"
         );
      }
   }

   private Object a(Map<?, ?> var1, String var2) {
      Feature var3;
      try {
         var3 = Feature.read(MaxPaths.resolveResource(this.c, this.d, var2));
      } catch (RuntimeException var12) {
         return JsApi.missing("Http.mock could not read the mock feature '" + var2 + "': " + var12.getMessage());
      }

      Map var4 = var1.get("arg") instanceof Map var5 ? Map.copyOf(var5) : null;
      String var13;
      String var17 = var13 = var1.get("pathPrefix") instanceof String var14 && !var14.isBlank() ? var14 : null;
      Supplier var15 = () -> new MockHandler(
         List.of(var3), var4, var13, Boolean.TRUE.equals(var1.get("javaBridge")), Boolean.TRUE.equals(var1.get("requestExpressions"))
      );

      FaultInjectionHandler var16;
      try {
         var16 = this.h(var1);
      } catch (RuntimeException var11) {
         return JsApi.missing("Http.mock faults: " + var11.getMessage());
      }

      AtomicReference var8 = new AtomicReference<>((MockHandler)var15.get());
      MockInstance var9 = MockInstance.start(
         (Function<HttpRequest, HttpResponse>)var8.get(),
         g(var1),
         null,
         var1x -> ((MockHandler)var8.get()).getVariable(var1x),
         List.of(),
         var16 == null ? null : var16::withDelegate,
         a(var1, var16)
      );
      var var10 = new ArrayList<>(List.of("the mock feature's Background state (its `def` globals + any table rows it built)"));
      var10.addAll(a(var16));
      var9.withState(null, var10, () -> {
         MockHandler var3x = (MockHandler)var15.get();
         var8.set(var3x);
         var9.swapHandler(var3x);
      });
      return this.a(a(var9, var16));
   }

   private Object a(String var1, Map<?, ?> var2) {
      for (Object var4 : var2.keySet()) {
         String var5 = String.valueOf(var4);
         if (!g.contains(var5) && !"rules".equals(var5)) {
            return JsApi.missing("Http.mock({twin}) takes twin · port · record, not '" + var5 + "'");
         }
      }

      if (var2.containsKey("rules") && !(var2.get("rules") instanceof Map)) {
         return JsApi.missing("Http.mock({twin}) takes twin · port · record, not 'rules'");
      } else {
         RulebookRepository var13 = new RulebookRepository(this.b);

         io.karatelabs.ext.rules.RulebookRepository.RulebookDocument var14;
         TwinModel var15;
         try {
            var14 = var13.b(var1);
            if (var14.k() == null) {
               throw new RuleApiException(404, "TWIN_NOT_FOUND", "rulebook '" + var1 + "' has no twin.js — author one with Rule.twin.update(name, source)");
            }

            var15 = TwinModelLoader.a(var14.k());
         } catch (RuntimeException var12) {
            return JsApi.error(var12);
         }

         String var6 = TwinHttpRouter.a(var15);
         if (var6 != null) {
            var var16 = JsApi.missing(
               "rulebook '" + var1 + "' declares " + var6 + ", which the twin-backed mock cannot run — a fresh instance IS its preparation"
            );
            var16.put("code", "MOCK_HOOK_UNSUPPORTED");
            var16.put("hook", var6);
            return var16;
         } else {
            io.karatelabs.ext.rules.twin.OpenApiProjector.OpenApiProjection var7;
            try {
               var7 = io.karatelabs.ext.rules.twin.OpenApiProjector.a(var15, var1);
            } catch (RuntimeException var11) {
               return JsApi.error(var11);
            }

            if (!var7.a()) {
               var var17 = var7.f().get(0);
               var var19 = JsApi.missing("rulebook '" + var1 + "' does not project, so no contract can route the mock: " + var17.get("reason"));
               var19.put("code", String.valueOf(var17.get("code")));
               var19.put("refusals", var7.f());
               return var19;
            } else {
               var var8 = a(var15, var2);
               if (!var8.isEmpty()) {
                  var var18 = JsApi.missing("lifecycle logic has one home: the twin already binds " + var8);
                  var18.put("code", "MOCK_BRAIN_CONFLICT");
                  var18.put("operations", var8);
                  return var18;
               } else {
                  TwinRuleRuntime var9 = new TwinRuleRuntime(var13, var1);
                  TwinHttpRouter var10 = new TwinHttpRouter(
                     var15,
                     var1,
                     var7,
                     var14.i(),
                     Map.of(var1, var14.n().c()),
                     this.c == null ? Path.of(".") : this.c,
                     Boolean.TRUE.equals(var2.get("record")),
                     var9
                  );
                  return this.a(
                     MockInstance.start(var10, g(var2), var10::a, var10::a)
                        .withState(List.of("the twin worlds", "the in-memory play records"), null, null)
                        .withBrain(var10)
                        .onStop(var9::close)
                  );
               }
            }
         }
      }
   }

   private static List<String> a(TwinModel var0, Map<?, ?> var1) {
      if (var1.get("rules") instanceof Map var2) {
         ArrayList var6 = new ArrayList();

         for (io.karatelabs.ext.rules.twin.TwinModel.CommandModel var5 : var0.c()) {
            if (var5.q() != null && var2.containsKey(var5.q().a())) {
               var6.add(var5.q().a());
            }
         }

         return var6;
      } else {
         return List.of();
      }
   }

   private Object a(String var1, Object var2) {
      io.karatelabs.ext.common.keycheck.Main.requireProduct("http.mock", "openapi");
      List var3 = null;
      if (var2 instanceof Map var4 && var4.containsKey("basePath")) {
         if (!(var4.get("basePath") instanceof String var5)) {
            return JsApi.missing("Http.mock basePath must be one path prefix String ('' disables prefixing), not: " + var4.get("basePath"));
         }

         var3 = io.karatelabs.ext.openapi.ServerBasePathResolver.a(List.of(var5));
      }

      OpenApiOperationCatalog var21;
      try {
         OpenApiDocument var22 = new OpenApiDocument(MaxPaths.resolveResource(this.c, this.d, var1));
         var21 = var3 == null ? io.karatelabs.ext.openapi.OpenApiOperationCatalog.fromDocument(var22) : io.karatelabs.ext.openapi.OpenApiOperationCatalog.fromDocument(var22, var3);
      } catch (RuntimeException var20) {
         return JsApi.missing("Http.mock could not parse spec '" + var1 + "': " + var20.getMessage());
      }

      Map<String, Object> var23 = Map.of();
      Map<String, Object> var24 = Map.of();
      ResourceResolver var7 = null;
      int var8 = 0;
      boolean var9 = true;
      boolean var10 = false;
      if (var2 instanceof Map var11) {
         if (var11.get("rules") instanceof Map var12) {
            var23 = d(var12);
         }

         if (var11.get("handlers") instanceof Map var26) {
            var24 = e(var26);
         }

         if (var11.get("crud") instanceof Boolean var27) {
            var9 = var27;
         }

         if (var11.get("validate") instanceof Boolean var28) {
            var10 = var28;
         }

         Object var29 = var11.get("resources");
         if (Boolean.FALSE.equals(var29)) {
            var9 = false;
         } else {
            var7 = ResourceResolver.from(var29);
         }

         var8 = g(var11);
      }

      Session var25 = Session.inMemory();
      Map var30 = var2 instanceof Map var14 && var14.get("arg") instanceof Map var34 ? Map.copyOf(var34) : Map.of();
      var25.getData().putAll(var30);
      ArrayList var35 = new ArrayList();
      var35.addAll(OpenApiMockHandler.orphanWarnings(var21, var24, "handler"));
      var35.addAll(OpenApiMockHandler.orphanWarnings(var21, var23, "rule"));

      FaultInjectionHandler var36;
      try {
         var36 = var2 instanceof Map var37 ? this.h(var37) : null;
      } catch (RuntimeException var19) {
         return JsApi.missing("Http.mock faults: " + var19.getMessage());
      }

      if (var36 != null) {
         for (String var17 : io.karatelabs.ext.mutation.FaultInjectionHandler.filterControlPaths(var21.operations().stream().map(io.karatelabs.ext.openapi.OpenApiOperationCatalog.OperationInfo::c).toList())) {
            LinkedHashMap var18 = new LinkedHashMap();
            var18.put("type", "reserved_path_shadowed");
            var18.put("key", var17);
            var18.put(
               "message",
               "operation path '" + var17 + "' collides with the reserved fault-control endpoint /__karate/fault and is shadowed while faults are armed"
            );
            var35.add(var18);
         }
      }

      OpenApiMockHandler var39 = new OpenApiMockHandler(var21, var23, var24, new RulebookRepository(this.b), new RuleEvaluator(), var25, var9, var10, var7);
      return this.a(
         a(
            MockInstance.start(var39, var8, () -> {
                  var25.getData().clear();
                  var25.getData().putAll(var30);
               }, var1x -> var25.getData().get(var1x), var35, var36 == null ? null : var36::withDelegate, a(var2 instanceof Map var40 ? var40 : null, var36))
               .withState(List.of("the CRUD store"), a(var36), null),
            var36
         )
      );
   }

   private static Map<String, Object> d(Map<?, ?> var0) {
      LinkedHashMap var1 = new LinkedHashMap();

      for (Entry var3 : var0.entrySet()) {
         if (var3.getKey() != null && var3.getValue() != null) {
            var1.put(String.valueOf(var3.getKey()), String.valueOf(var3.getValue()));
         }
      }

      return var1;
   }

   private static Map<String, Object> e(Map<?, ?> var0) {
      LinkedHashMap var1 = new LinkedHashMap();

      for (Entry var3 : var0.entrySet()) {
         if (var3.getKey() != null && var3.getValue() != null) {
            var1.put(String.valueOf(var3.getKey()), var3.getValue());
         }
      }

      return var1;
   }

   private String f(Map<?, ?> var1) {
      if (var1.get("js") instanceof String var2 && !var2.isBlank()) {
         return var2;
      } else {
         return var1.get("file") instanceof String var4 && !var4.isBlank() ? MaxPaths.resolveResource(this.c, this.d, var4).getText() : null;
      }
   }

   private static int g(Map<?, ?> var0) {
      return var0.get("port") instanceof Number var1 ? var1.intValue() : 0;
   }

   private FaultInjectionHandler h(Map<?, ?> var1) {
      if (var1.get("faults") instanceof String var2 && !var2.isBlank()) {
         String var6;
         try {
            var6 = MaxPaths.resolveResource(this.c, this.d, var2).getText();
         } catch (RuntimeException var5) {
            throw new IllegalArgumentException("could not read '" + var2 + "': " + var5.getMessage());
         }

         return new FaultInjectionHandler(io.karatelabs.ext.mutation.FaultManifest.parse(var6));
      } else {
         return null;
      }
   }

   private static String a(Map<?, ?> var0, FaultInjectionHandler var1) {
      if (var0 != null && var0.get("host") instanceof String var2 && !var2.isBlank()) {
         return var2;
      } else {
         return var1 == null ? null : "localhost";
      }
   }

   private static List<String> a(FaultInjectionHandler var0) {
      return var0 == null ? List.of() : List.of("the fault feed (armed fault + served log — mock.fault(null) disarms)");
   }

   private static MockInstance a(MockInstance var0, FaultInjectionHandler var1) {
      return var1 == null ? var0 : var0.withVerb("fault", var1x -> var1x.length == 0 ? var1.status() : var1.armFault(var1x[0] == null ? null : String.valueOf(var1x[0])));
   }
}
