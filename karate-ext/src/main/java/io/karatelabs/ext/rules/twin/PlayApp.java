package io.karatelabs.ext.rules.twin;

import io.karatelabs.common.Json;
import io.karatelabs.common.Resource;
import io.karatelabs.core.KarateConfig;
import io.karatelabs.ext.common.jsapi.JsApi;
import io.karatelabs.ext.common.jsapi.JsDispatch;
import io.karatelabs.http.HttpClient;
import io.karatelabs.http.HttpRequest;
import io.karatelabs.http.HttpResponse;
import io.karatelabs.http.HttpServer;
import io.karatelabs.http.InMemorySessionStore;
import io.karatelabs.http.ServerConfig;
import io.karatelabs.http.ServerRequestHandler;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.Map.Entry;

public final class PlayApp {
   public static final String a = "APP_TARGET_UNSUPPORTED";
   static final String b = "APP_HEADER_REFUSED";
   static final String c = "default-src 'self'; script-src 'self' 'unsafe-inline' 'unsafe-eval' https://cdn.tailwindcss.com https://unpkg.com; style-src 'self' 'unsafe-inline'; img-src 'self' data:; connect-src 'self'; form-action 'self'; base-uri 'self'; frame-ancestors 'none'";
   private static final Set<String> d = Set.of(
      "Content-Type",
      "Set-Cookie",
      "Location",
      "HX-Redirect",
      "Cache-Control",
      "X-Content-Type-Options",
      "X-Frame-Options",
      "X-XSS-Protection",
      "Referrer-Policy",
      "Strict-Transport-Security"
   );
   private static final HttpClient e = new HttpClient() {
      @Override
      public HttpResponse invoke(HttpRequest var1) {
         throw new IllegalStateException("a page sends only through app.act");
      }

      @Override
      public void apply(KarateConfig var1) {
      }

      @Override
      public void abort() {
      }

      @Override
      public void close() {
      }
   };
   private final String f;
   private final TwinModel g;
   private final io.karatelabs.ext.rules.twin.OpenApiProjector.OpenApiProjection h;
   private final io.karatelabs.ext.common.MockRuntime i;
   private final Path j;
   private final Twin k;
   private final String l;
   private final AppResourceResolver m;
   private final io.karatelabs.ext.rules.HttpTwinTransport n;
   private final HttpServer o;
   private Map<String, Object> p;
   private String q;
   private io.karatelabs.ext.rules.RuleApiException r;

   private PlayApp(String var1, Path var2, io.karatelabs.ext.common.MockRuntime var3, TwinHttpRouter var4, Path var5, String var6, int var7) {
      this.f = var1;
      this.i = var3;
      this.j = var4.d();
      this.l = var6 == null ? null : var6.replaceAll("/+$", "");
      io.karatelabs.ext.rules.RulebookRepository var8 = new io.karatelabs.ext.rules.RulebookRepository(var2);
      this.g = TwinModelLoader.a(var8.b(var1).k());
      this.h = io.karatelabs.ext.rules.twin.OpenApiProjector.a(this.g, var1);
      if (!this.h.a()) {
         var var11 = this.h.f().get(0);
         throw new io.karatelabs.ext.rules.RuleApiException(
            400, String.valueOf(var11.get("code")), "rulebook '" + var1 + "' does not project, so the app has no contract: " + var11.get("reason")
         );
      } else {
         this.k = Twin.atRulesHome(var2).withRoot(this.j);
         this.m = new AppResourceResolver(var5);
         this.n = new io.karatelabs.ext.rules.HttpTwinTransport(var3.getUrl(), 10000);
         this.d();
         if (this.r != null) {
            throw this.r;
         } else {
            ServerConfig var9 = new ServerConfig("app")
               .devMode(true)
               .shellTemplate("layout.html")
               .sessionStore(new InMemorySessionStore())
               .templateRoute("/worlds/{id}/timeline", "timeline.html")
               .templateRoute("/worlds/{id}", "detail.html")
               .requestFilter((var1x, var2x) -> {
                  var1x.setHttpClient(e);
                  this.d();
                  if (this.r == null) {
                     return null;
                  } else {
                     HttpResponse var3x = HttpResponse.html(a(this.r, "Fix app/ui.js and refresh."));
                     var3x.setStatus(500);
                     return var3x;
                  }
               })
               .globalVariables(Map.of("app", new PlayApp.Global()));
            ServerRequestHandler var10 = new ServerRequestHandler(var9, this.m);
            this.o = HttpServer.start(var7, var1x -> a(var10.apply(var1x)));
         }
      }
   }

   private static HttpResponse a(HttpResponse var0) {
      var var1 = var0.getHeaders();
      String var2 = var1 == null ? null : var1.keySet().stream().filter(var0x -> !d.contains(var0x)).findFirst().orElse(null);
      if (var2 != null) {
         var0 = HttpResponse.html(
            a(
               new io.karatelabs.ext.rules.RuleApiException(
                  500, "APP_HEADER_REFUSED", "a page sets no response header — this one set '" + var2 + "'; the app owns its headers"
               ),
               "Remove the header from the page."
            )
         );
         var0.setStatus(500);
      }

      var0.setHeader(
         "Content-Security-Policy",
         "default-src 'self'; script-src 'self' 'unsafe-inline' 'unsafe-eval' https://cdn.tailwindcss.com https://unpkg.com; style-src 'self' 'unsafe-inline'; img-src 'self' data:; connect-src 'self'; form-action 'self'; base-uri 'self'; frame-ancestors 'none'"
      );
      return var0;
   }

   public static PlayApp a(String var0, Path var1, Object var2, Path var3, String var4, int var5) {
      if (!(var2 instanceof io.karatelabs.ext.common.MockRuntime var6 && var6.brain() instanceof TwinHttpRouter var7 && var7.c() && !var6.isStopped())) {
         throw new io.karatelabs.ext.rules.RuleApiException(
            400,
            "APP_TARGET_UNSUPPORTED",
            "Http.app target must be the live handle Http.mock({twin: '"
               + var0
               + "', record: true}) returned — the app reads each world's record off it, so a URL or a mock that does not record is refused"
         );
      } else if (!var0.equals(var7.b())) {
         throw new io.karatelabs.ext.rules.RuleApiException(400, "APP_TARGET_UNSUPPORTED", "Http.app target serves rulebook '" + var7.b() + "', not '" + var0 + "'");
      } else {
         return new PlayApp(var0, var1, var6, var7, var3, var4, var5);
      }
   }

   public String a() {
      return "http://localhost:" + this.o.getPort();
   }

   public int b() {
      return this.o.getPort();
   }

   public void c() {
      this.o.stopAndWait();
   }

   private synchronized void d() {
      Resource var1 = this.m.resolve("ui.js", null);
      String var2 = var1 == null ? null : var1.getText();
      if (this.p == null || !Objects.equals(var2, this.q)) {
         this.q = var2;

         try {
            this.p = io.karatelabs.ext.rules.twin.UiProjectionBuilder.a(this.g, this.f, var2);
            this.r = null;
         } catch (io.karatelabs.ext.rules.RuleApiException var4) {
            this.r = var4;
         }
      }
   }

   private static String a(io.karatelabs.ext.rules.RuleApiException var0, String var1) {
      String var2 = var0.getMessage().replace("&", "&amp;").replace("<", "&lt;");
      return "<!DOCTYPE html><html><head><title>"
         + var0.b
         + "</title></head><body style=\"font-family:system-ui;padding:2rem\"><h1 data-ui-error=\""
         + var0.b
         + "\">"
         + var0.b
         + "</h1><p>"
         + var2
         + "</p><p>"
         + var1
         + "</p></body></html>";
   }

   private synchronized List<Map<String, Object>> e() {
      ArrayList<java.util.Map<java.lang.String,java.lang.Object>> var1 = new ArrayList();

      for (Map var3 : this.h()) {
         Object var4 = this.e(var3.get("identity"));
         ArrayList<java.util.Map<java.lang.String,java.lang.Object>> var5 = new ArrayList();

         for (Object var7 : h(g(this.p.get("resource")).get("columns"))) {
            String var8 = String.valueOf(var7);
            var5.add(this.a(var8, var4 instanceof Map var9 ? var9.get(var8) : null));
         }

         Map<String, Object> var10 = this.b(var3);
         var10.put("cells", var5);
         var1.add(var10);
      }

      return var1;
   }

   private synchronized Map<String, Object> b(String var1) {
      var var2 = this.f(var1);
      if (var2 == null) {
         return null;
      } else {
         Map<java.lang.String, java.lang.Object> var3 = this.b(var2);
         var3.put("card", this.b(this.e(var2.get("identity"))));
         var var4 = this.i(String.valueOf(var2.get("id")));
         ArrayList<java.util.Map<java.lang.String,java.lang.Object>> var5 = new ArrayList();

         for (io.karatelabs.ext.rules.twin.TwinModel.CommandModel var7 : this.k()) {
            if (!var7.q().d()) {
               var5.add(this.a(var7, List.of(this.a(var1, (Map<String, Object>)var4.get(var7.a()), var7))));
            }
         }

         var3.put("actions", var5);
         return var3;
      }
   }

   private synchronized List<Map<String, Object>> f() {
      ArrayList<java.util.Map<java.lang.String,java.lang.Object>> var1 = new ArrayList();

      for (io.karatelabs.ext.rules.twin.TwinModel.CommandModel var3 : this.k()) {
         if (var3.q().d()) {
            var1.add(this.a(var3, List.of()));
         }
      }

      return var1;
   }

   private synchronized Map<String, Object> c(String var1) {
      var var2 = this.f(var1);
      if (var2 == null) {
         return null;
      } else {
         Map<java.lang.String, java.lang.Object> var3 = this.b(var2);
         ArrayList<java.util.LinkedHashMap> var4 = new ArrayList();

         try {
            io.karatelabs.ext.rules.twin.PlayRecordStore.PlayRecord var5 = io.karatelabs.ext.rules.twin.PlayRecordStore.a(this.j, String.valueOf(var2.get("id")));
            int var6 = 0;

            for (Map var8 : var5.d()) {
               LinkedHashMap<java.lang.String, java.lang.Object> var9 = new LinkedHashMap();
               var9.put("at", var6++);
               var9.put("command", var8.get("command"));
               var9.put("title", this.e(String.valueOf(var8.get("command"))));
               ArrayList<java.util.Map<java.lang.String,java.lang.Object>> var10 = new ArrayList();
               g(var8.get("args")).forEach((var2x, var3x) -> var10.add(this.a(var2x, var3x)));
               var9.put("args", var10);
               var9.put("outcome", var8.get("disposition"));
               var9.put("outcomeText", a(var8.get("disposition")));
               var9.put("stage", var8.get("stage"));
               var9.put("reason", var8.get("reason"));
               var9.put("code", var8.get("reason") == null ? null : a(this.g.f().get(String.valueOf(var8.get("command"))), String.valueOf(var8.get("reason"))));
               var9.put("reasonText", this.d(var8.get("reason")));
               var9.put("exchange", this.a(g(var8.get("exchange"))));
               var4.add(var9);
            }
         } catch (io.karatelabs.ext.rules.RuleApiException var11) {
            var3.put("error", var11.b + ": " + var11.getMessage());
         }

         var3.put("steps", var4);
         return var3;
      }
   }

   private synchronized Map<String, Object> g() {
      var var1 = g(io.karatelabs.ext.rules.twin.CanonicalJson.d(this.h.b()));
      var var2 = io.karatelabs.ext.openapi.OpenApiDocumentView.a(
         io.karatelabs.ext.openapi.OpenApiOperationCatalog.fromDocument(new io.karatelabs.ext.openapi.OpenApiDocument(Resource.text(this.h.d()), g(io.karatelabs.ext.rules.twin.CanonicalJson.d(var1))))
      );
      LinkedHashMap<java.lang.String, java.util.Map<java.lang.String,java.util.Map<java.lang.String,java.lang.Object>>> var3 = new LinkedHashMap();

      for (Map var5 : this.h()) {
         var3.put(f(var5.get("identity")), this.i(String.valueOf(var5.get("id"))));
      }

      var var19 = g(var1.get("paths"));

      for (Object var6 : h(var2.get("operations"))) {
         var var7 = g(var6);
         var var8 = g(g(var19.get(var7.get("path"))).get(String.valueOf(var7.get("method")).toLowerCase()));
         var7.put("description", var8.get("description"));
         Object var9 = var8.get("requestBody") instanceof Map var10 ? g(g(var10.get("content")).get("application/json")).get("schema") : null;
         var7.put("requestSchema", var9 == null ? null : Json.of(var9).toStringPretty());
         var var21 = g(var8.get("responses"));

         for (Object var12 : h(var7.get("responses"))) {
            var var13 = g(var12);
            Object var15 = g(var21.get(String.valueOf(var13.get("status")))).get("content") instanceof Map var17
                  && var17.get("application/json") instanceof Map var16
               ? var16.get("schema")
               : null;
            var13.put("schema", var15 == null ? null : Json.of(var15).toStringPretty());
            var13.remove("sample");
         }

         String var23 = String.valueOf(var7.get("opId"));
         io.karatelabs.ext.rules.twin.TwinModel.CommandModel var24 = this.j(var23);
         if (g(this.p.get("commands")).get(var24 != null ? var24.a() : var23) instanceof Map var26) {
            var7.put("summary", var26.get("title"));
            if (var26.get("help") != null) {
               var7.put("description", var26.get("help"));
            }
         }

         if (var24 != null) {
            ArrayList var27 = new ArrayList();
            if (!var24.q().d()) {
               var3.forEach((var3x, var4) -> var27.add(this.a(var3x, (Map<String, Object>)var4.get(var24.a()), var24)));
            }

            var7.put("form", this.a(var24, var27));
         } else if (this.i().contains(var23)) {
            var7.put("form", this.a(var23, var3.keySet()));
         }
      }

      var2.put("title", this.p.get("title"));
      var2.remove("description");
      var2.put("lifecycle", this.k.lifecycle(this.f));
      return var2;
   }

   private synchronized Map<String, Object> a(String var1, String var2, Object var3) {
      io.karatelabs.ext.rules.twin.TwinModel.CommandModel var4 = this.g.f().get(var2);
      LinkedHashMap<java.lang.String, java.lang.Object> var5 = new LinkedHashMap();
      var5.put("command", var2);
      var5.put("key", var1);
      if (var4 == null && this.i().contains(var2)) {
         return this.a(var5, var1, var2);
      } else if (var4 != null && var4.q() != null) {
         var5.put("title", this.e(var2));
         Object var6 = var3 instanceof Map var7 ? g(io.karatelabs.ext.rules.twin.CanonicalJson.d(var7)) : new LinkedHashMap();
         String var23 = a(var4, (Map<String, Object>)var6);
         if (var23 != null) {
            return a(var5, var23);
         } else {
            Object var9 = null;
            int var10 = 0;
            Object var8;
            if (var4.q().d()) {
               var8 = io.karatelabs.ext.rules.twin.CanonicalJson.d(this.g.l());
               var1 = null;
            } else {
               var8 = this.h(var1);
               if (var8 == null) {
                  return a(var5, "no world '" + var1 + "' in the target");
               }

               var10 = this.g(var1).size();
               var9 = this.e((Object)var1);
            }

            var var11 = g(var4.k().call(null, io.karatelabs.ext.rules.twin.CanonicalJson.d(var8), io.karatelabs.ext.rules.twin.CanonicalJson.d(var6)));
            Map<java.lang.String, Object> var12 = this.n.observe(var11);
            if (Boolean.TRUE.equals(var12.get("transportFailure"))) {
               var5.put("outcome", "error");
               var5.put("message", "the target did not answer");
               return var5;
            } else {
               var var13 = g(var12.get("response"));
               int var14 = ((Number)var13.get("status")).intValue();
               Object var15 = var13.get("body");
               boolean var16 = var14 == var4.q().e().a();
               if (var1 == null && var16 && var15 instanceof Map var17) {
                  var1 = f(var17.get(this.g.k().c()));
               }

               var var24 = a(var11, var14, var15);
               Map<java.lang.String, Object> var18 = var1 == null ? null : a(this.g(var1), var10, var24);
               var5.put("key", var1);
               var5.put("location", var1 == null ? "/" : k(var1));
               var5.put("status", var14);
               String var19 = var15 instanceof Map var21 && var21.get(this.g.k().f()) instanceof String var20 ? var20 : null;
               String var25 = var18 != null && var18.get("reason") != null ? String.valueOf(var18.get("reason")) : a(var4, var14, var19);
               var5.put("outcome", var18 != null ? var18.get("disposition") : (var16 ? "applied" : (var25 != null ? "refused" : "error")));
               var5.put("code", var19);
               var5.put("reason", var25);
               var5.put("reasonText", this.d((Object)var25));
               if (var16 && var15 instanceof Map) {
                  var5.put("card", this.b(var15));
                  if (var9 instanceof Map var26) {
                     var5.put("changes", this.a(var26, (Map<?, ?>)var15));
                  }
               }

               var5.put("exchange", this.a(var24));
               return var5;
            }
         }
      } else {
         return a(var5, "the model binds no command '" + var2 + "'");
      }
   }

   private List<Map<String, Object>> a(Map<?, ?> var1, Map<?, ?> var2) {
      LinkedHashSet<Object> var3 = new LinkedHashSet<>(g(this.p.get("fields")).keySet());
      var3.retainAll(var2.keySet());
      var3.addAll(var2.keySet());
      var3.addAll(var1.keySet());
      ArrayList<java.util.Map<java.lang.String,java.lang.Object>> var4 = new ArrayList();

      for (Object var6 : var3) {
         if (!io.karatelabs.ext.rules.twin.CanonicalJson.c(var1.get(var6)).equals(io.karatelabs.ext.rules.twin.CanonicalJson.c(var2.get(var6)))) {
            String var7 = String.valueOf(var6);
            LinkedHashMap<java.lang.String, java.lang.Object> var8 = new LinkedHashMap();
            var8.put("field", var7);
            var8.put("label", this.a(var7, null).get("label"));
            var8.put("before", this.a(var7, var1.get(var6)));
            var8.put("after", this.a(var7, var2.get(var6)));
            var4.add(var8);
         }
      }

      return var4;
   }

   private Map<String, Object> a(Map<String, Object> var1, String var2, String var3) {
      var1.put("title", this.e(var3));
      Object var4 = null;
      if (var3.equals(this.j())) {
         var4 = this.h(var2);
         if (var4 == null) {
            return a(var1, "no world '" + var2 + "' in the target");
         }
      } else {
         var1.put("key", null);
      }

      var var5 = io.karatelabs.ext.rules.twin.TwinSourceCompiler.a(this.g.k(), var4);
      Map<java.lang.String, Object> var6 = this.n.observe(var5);
      if (Boolean.TRUE.equals(var6.get("transportFailure"))) {
         var1.put("outcome", "error");
         var1.put("message", "the target did not answer");
         return var1;
      } else {
         var var7 = g(var6.get("response"));
         int var8 = ((Number)var7.get("status")).intValue();
         Object var9 = var7.get("body");
         String var10 = var9 instanceof Map var12 && var12.get(this.g.k().f()) instanceof String var11 ? var11 : null;
         var1.put("location", "/docs");
         var1.put("status", var8);
         var1.put("outcome", var8 == 200 ? "applied" : (var10 != null ? "refused" : "error"));
         var1.put("code", var10);
         var1.put("reasonText", var10);
         if (var8 == 200 && var9 instanceof Map) {
            var1.put("card", this.b(var9));
         } else if (var8 == 200) {
            LinkedHashMap<java.lang.String, java.lang.Object> var14 = new LinkedHashMap();
            var14.put("type", "array");
            var14.put("items", this.g.k().e());
            var1.put("value", this.a(var9, null, var14, g(this.p.get("fields"))));
         }

         var1.put("exchange", this.a(a(var5, var8, var9)));
         return var1;
      }
   }

   private static Map<String, Object> a(Map<String, Object> var0, int var1, Object var2) {
      LinkedHashMap<java.lang.String, java.lang.Object> var3 = new LinkedHashMap();
      var3.put("status", var1);
      var3.put("body", var2);
      LinkedHashMap<java.lang.String, java.lang.Object> var4 = new LinkedHashMap();
      var4.put("request", var0);
      var4.put("response", var3);
      return var4;
   }

   private static Map<String, Object> a(List<Map<String, Object>> var0, int var1, Map<String, Object> var2) {
      String var3 = io.karatelabs.ext.rules.twin.CanonicalJson.c(var2);

      for (int var4 = var1; var4 < var0.size(); var4++) {
         if (var3.equals(io.karatelabs.ext.rules.twin.CanonicalJson.c(((Map)var0.get(var4)).get("exchange")))) {
            return (Map<String, Object>)var0.get(var4);
         }
      }

      return null;
   }

   private static Map<String, Object> a(Map<String, Object> var0, String var1) {
      var0.put("outcome", "invalid");
      var0.put("message", var1);
      var0.put("location", "/");
      return var0;
   }

   private static String a(io.karatelabs.ext.rules.twin.TwinModel.CommandModel var0, Map<String, Object> var1) {
      List<io.karatelabs.ext.rules.twin.TwinModel.ArgumentAxis> var2 = var0.g() == null ? List.of() : var0.g();

      for (String var4 : var1.keySet()) {
         if (var2.stream().noneMatch(var1x -> var1x.a().equals(var4))) {
            return "'" + var4 + "' is not an argument of '" + var0.a() + "'";
         }
      }

      for (io.karatelabs.ext.rules.twin.TwinModel.ArgumentAxis var9 : (Iterable<io.karatelabs.ext.rules.twin.TwinModel.ArgumentAxis>) (Iterable<?>) (var2)) {
         boolean var5;
         try {
            var5 = var1.containsKey(var9.a()) && TwinModel.a(var9, var1.get(var9.a()));
         } catch (RuntimeException var7) {
            var5 = false;
         }

         if (!var5) {
            return "'" + var9.a() + "' is outside its declared domain";
         }
      }

      return null;
   }

   private static String a(io.karatelabs.ext.rules.twin.TwinModel.CommandModel var0, int var1, String var2) {
      for (Entry var4 : var0.q().e().b().entrySet()) {
         for (io.karatelabs.ext.rules.twin.TwinModel.WireRefusal var6 : (Iterable<io.karatelabs.ext.rules.twin.TwinModel.WireRefusal>) (Iterable<?>) ((List)var4.getValue())) {
            if (var6.a() == var1 && var6.b().equals(var2)) {
               return (String)var4.getKey();
            }
         }
      }

      return null;
   }

   private static String a(io.karatelabs.ext.rules.twin.TwinModel.CommandModel var0, String var1) {
      List var2 = var0 != null && var0.q() != null ? var0.q().e().b().get(var1) : null;
      return var2 != null && !var2.isEmpty() ? ((io.karatelabs.ext.rules.twin.TwinModel.WireRefusal)var2.get(0)).b() : var1;
   }

   private Map<String, Object> a(io.karatelabs.ext.rules.twin.TwinModel.CommandModel var1, List<Map<String, Object>> var2) {
      var var3 = g(g(this.p.get("commands")).get(var1.a()));
      LinkedHashMap<java.lang.String, java.lang.Object> var4 = new LinkedHashMap();
      LinkedHashMap<java.lang.String, java.lang.Object> var5 = new LinkedHashMap();
      LinkedHashMap<java.lang.String, java.lang.Object> var6 = new LinkedHashMap();
      LinkedHashMap<java.lang.String, java.lang.Object> var7 = new LinkedHashMap();
      LinkedHashMap<java.lang.String, java.lang.Object> var8 = new LinkedHashMap();
      LinkedHashMap<java.lang.String, java.lang.Object> var9 = new LinkedHashMap();

      for (Object var11 : h(var3.get("fields"))) {
         io.karatelabs.ext.rules.twin.TwinModel.ArgumentAxis var12 = b(var1, String.valueOf(var11));
         var var13 = g(g(this.p.get("fields")).get(var12.a()));
         var4.put(var12.a(), a(var12));
         Object[] var14 = a(var1, var12);
         var5.put(var12.a(), var14[0]);
         var6.put(var12.a(), var14[1]);
         var7.put(var12.a(), var13.get("label"));
         if (var13.get("help") != null) {
            var8.put(var12.a(), var13.get("help"));
         }

         if (var13.get("values") != null) {
            var9.put(var12.a(), var13.get("values"));
         }
      }

      LinkedHashMap<java.lang.String, java.lang.Object> var15 = new LinkedHashMap();
      var15.put("command", var1.a());
      var15.put("operation", var1.q().a());
      var15.put("title", var3.get("title"));
      var15.put("help", var3.get("help"));
      var15.put("creates", var1.q().d());
      var15.put("addressed", !var1.q().d());
      var15.put("schema", var4);
      var15.put("initial", var5);
      var15.put("provenance", var6);
      var15.put("labels", var7);
      var15.put("fieldHelp", var8);
      var15.put("valueLabels", var9);
      var15.put("worlds", var2);
      var15.put("pick", var2.isEmpty() ? null : ((Map)var2.get(0)).get("key"));
      Map<java.lang.String, Object> var16 = var2.isEmpty() ? this.a(null, Map.of("enabled", CommandExecutor.b(this.g, this.g.l(), var1, null)), var1) : (Map)var2.get(0);

      for (String var18 : List.of("enabled", "reason", "reasonText", "badge", "badgeText", "expects")) {
         var15.put(var18, var16.get(var18));
      }

      var15.put("data", io.karatelabs.ext.rules.twin.CanonicalJson.d(var15));
      var15.put("initialData", var5);
      return var15;
   }

   private Map<String, Object> a(String var1, Collection<String> var2) {
      boolean var3 = var1.equals(this.j());
      ArrayList<java.util.LinkedHashMap> var4 = new ArrayList();
      if (var3) {
         for (String var6 : var2) {
            LinkedHashMap<java.lang.String, java.lang.Object> var7 = new LinkedHashMap();
            var7.put("key", var6);
            var7.put("enabled", true);
            var7.put("badge", "available");
            var7.put("badgeText", a("available"));
            var4.add(var7);
         }
      }

      boolean var8 = !var3 || !var4.isEmpty();
      LinkedHashMap<java.lang.String, java.lang.Object> var9 = new LinkedHashMap();
      var9.put("command", var1);
      var9.put("operation", var1);
      var9.put("title", this.e(var1));
      var9.put("help", g(g(this.p.get("commands")).get(var1)).get("help"));
      var9.put("creates", false);
      var9.put("addressed", var3);
      var9.put("schema", Map.of());
      var9.put("initial", Map.of());
      var9.put("provenance", Map.of());
      var9.put("labels", Map.of());
      var9.put("fieldHelp", Map.of());
      var9.put("valueLabels", Map.of());
      var9.put("worlds", var4);
      var9.put("pick", var4.isEmpty() ? null : ((Map)var4.get(0)).get("key"));
      var9.put("enabled", var8);
      var9.put("reason", null);
      var9.put("reasonText", var8 ? null : "nothing to read yet");
      var9.put("badge", var8 ? "available" : "unavailable");
      var9.put("badgeText", a(String.valueOf(var9.get("badge"))));
      var9.put("expects", null);
      var9.put("data", io.karatelabs.ext.rules.twin.CanonicalJson.d(var9));
      var9.put("initialData", Map.of());
      return var9;
   }

   private static io.karatelabs.ext.rules.twin.TwinModel.ArgumentAxis b(io.karatelabs.ext.rules.twin.TwinModel.CommandModel var0, String var1) {
      for (io.karatelabs.ext.rules.twin.TwinModel.ArgumentAxis var3 : var0.g()) {
         if (var3.a().equals(var1)) {
            return var3;
         }
      }

      throw new IllegalStateException("no axis " + var1);
   }

   private static Object a(io.karatelabs.ext.rules.twin.TwinModel.ArgumentAxis var0) {
      if (var0.b().equals("int")) return "#int[" + var0.d() + "," + var0.e() + "]";
      if (var0.b().equals("bool")) return "#boolean";
      Map<String, Object> result = new LinkedHashMap<>();
      result.put("#enum", new ArrayList<>(var0.c()));
      return result;
      // $VF: Couldn't be decompiled
      // Please report this to the Vineflower issue tracker, at https://github.com/Vineflower/vineflower/issues with a copy of the class file (if you have the rights to distribute it!)
      // java.lang.RuntimeException: invalid constant type: Ljava/lang/Object; with value #boolean
      //   at org.jetbrains.java.decompiler.modules.decompiler.exps.ConstExprent.toJava(ConstExprent.java:356)
      //   at org.jetbrains.java.decompiler.modules.decompiler.exps.SwitchExprent.toJava(SwitchExprent.java:152)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.getCastedExprent(ExprProcessor.java:1014)
      //   at org.jetbrains.java.decompiler.modules.decompiler.exps.ExitExprent.toJava(ExitExprent.java:86)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.listToJava(ExprProcessor.java:891)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.BasicBlockStatement.toJava(BasicBlockStatement.java:91)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.jmpWrapper(ExprProcessor.java:829)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.SequenceStatement.toJava(SequenceStatement.java:107)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.RootStatement.toJava(RootStatement.java:36)
      //   at org.jetbrains.java.decompiler.main.ClassWriter.writeMethod(ClassWriter.java:1306)
      //
      // Bytecode:
      // 00: aload 0
      // 01: invokevirtual io/karatelabs/ext/rules/twin/KUpper$a.b ()Ljava/lang/String;
      // 04: astore 1
      // 05: bipush -1
      // 06: istore 2
      // 07: aload 1
      // 08: invokevirtual java/lang/String.hashCode ()I
      // 0b: lookupswitch 50 2 104431 25 3029738 39
      // 24: aload 1
      // 25: ldc "int"
      // 27: invokevirtual java/lang/String.equals (Ljava/lang/Object;)Z
      // 2a: ifeq 3d
      // 2d: bipush 0
      // 2e: istore 2
      // 2f: goto 3d
      // 32: aload 1
      // 33: ldc "bool"
      // 35: invokevirtual java/lang/String.equals (Ljava/lang/Object;)Z
      // 38: ifeq 3d
      // 3b: bipush 1
      // 3c: istore 2
      // 3d: iload 2
      // 3e: lookupswitch 47 2 0 26 1 42
      // 58: aload 0
      // 59: invokevirtual io/karatelabs/ext/rules/twin/KUpper$a.d ()Ljava/lang/Long;
      // 5c: aload 0
      // 5d: invokevirtual io/karatelabs/ext/rules/twin/KUpper$a.e ()Ljava/lang/Long;
      // 60: invokedynamic makeConcatWithConstants (Ljava/lang/Long;Ljava/lang/Long;)Ljava/lang/String; bsm=java/lang/invoke/StringConcatFactory.makeConcatWithConstants (Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite; args=[ "#int[\u0001,\u0001]" ]
      // 65: goto 8d
      // 68: ldc "#boolean"
      // 6a: goto 8d
      // 6d: new java/util/LinkedHashMap
      // 70: dup
      // 71: invokespecial java/util/LinkedHashMap.<init> ()V
      // 74: astore 3
      // 75: aload 3
      // 76: ldc "#enum"
      // 78: new java/util/ArrayList
      // 7b: dup
      // 7c: aload 0
      // 7d: invokevirtual io/karatelabs/ext/rules/twin/KUpper$a.c ()Ljava/util/List;
      // 80: invokespecial java/util/ArrayList.<init> (Ljava/util/Collection;)V
      // 83: invokeinterface java/util/Map.put (Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object; 3
      // 88: pop
      // 89: aload 3
      // 8a: goto 8d
      // 8d: areturn
   }

   private static Object[] a(io.karatelabs.ext.rules.twin.TwinModel.CommandModel var0, io.karatelabs.ext.rules.twin.TwinModel.ArgumentAxis var1) {
      if (var0.r() != null && var0.r().containsKey(var1.a())) {
         return new Object[]{var0.r().get(var1.a()), "example"};
      } else {
         List var2 = var1.f() != null && !var1.f().isEmpty() ? var1.f() : var1.c();
         if (var2 != null && !var2.isEmpty()) {
            return new Object[]{var2.get(0), "schema"};
         } else {
            return var1.d() != null ? new Object[]{var1.d(), "schema"} : new Object[]{null, "fallback"};
         }
      }
   }

   private List<Map<String, Object>> b(Object var1) {
      ArrayList<java.util.Map<java.lang.String,java.lang.Object>> var2 = new ArrayList();
      if (var1 instanceof Map var3) {
         ArrayList<Object> var4 = new ArrayList<>(g(this.p.get("fields")).keySet());
         var4.retainAll(var3.keySet());
         var3.keySet().stream().filter(var1x -> !var4.contains(var1x)).forEach(var4::add);

         for (Object var6 : var4) {
            var2.add(this.a(String.valueOf(var6), var3.get(var6)));
         }
      }

      return var2;
   }

   private Map<String, Object> a(String var1, Object var2) {
      return this.a(var1, var2, c(c(this.g.k().e()).get("properties")).get(var1), g(this.p.get("fields")).get(var1));
   }

   private Map<String, Object> a(String var1, Object var2, Object var3, Object var4) {
      var var5 = c(var4);
      var var6 = this.a(var2, var5.get("format"), c(var3), Map.of());
      String var7 = var2 != null && !(var2 instanceof Map) && !(var2 instanceof List) ? io.karatelabs.ext.rules.twin.UiProjectionBuilder.a(var2) : null;
      if (var7 != null && c(var5.get("values")).get(var7) instanceof String var8) {
         var6.put("text", var8);
         var6.put("member", var7);
      }

      var6.put("field", var1);
      Object var10 = c(var3).get("title");
      var6.put("label", var5.get("label") != null ? var5.get("label") : (var10 instanceof String var11 ? var11 : io.karatelabs.ext.rules.twin.UiProjectionBuilder.a(var1)));
      return var6;
   }

   private Map<String, Object> a(Object var1, Object var2, Map<String, Object> var3, Map<String, Object> var4) {
      LinkedHashMap<java.lang.String, java.lang.Object> var5 = new LinkedHashMap();
      if (var1 instanceof Map var6) {
         var var8 = c(var3.get("properties"));
         ArrayList<java.util.Map<java.lang.String,java.lang.Object>> var9 = new ArrayList();

         for (Object var11 : a(var8, var6.keySet())) {
            String var12 = String.valueOf(var11);
            var9.add(this.a(var12, var6.get(var11), var8.get(var12), var4.get(var12)));
         }

         var5.put("kind", "object");
         var5.put("entries", var9);
      } else if (var1 instanceof List var7 && !var7.isEmpty()) {
         var var20 = c(var3.get("items"));
         if (var7.stream().allMatch(var0 -> var0 instanceof Map)) {
            var var22 = c(var20.get("properties"));
            LinkedHashSet var24 = new LinkedHashSet();
            var7.forEach(var1x -> var24.addAll(((Map)var1x).keySet()));
            var var25 = a(var22, var24);
            ArrayList<java.util.Map<java.lang.String,java.lang.Object>> var26 = new ArrayList();

            for (Object var14 : var25) {
               var var15 = this.a(String.valueOf(var14), null, var22.get(String.valueOf(var14)), var4.get(String.valueOf(var14)));
               var26.add(Map.of("field", var15.get("field"), "label", var15.get("label")));
            }

            ArrayList<java.util.ArrayList> var27 = new ArrayList();

            for (Object var29 : var7) {
               ArrayList<java.util.Map<java.lang.String,java.lang.Object>> var16 = new ArrayList();

               for (Object var18 : var25) {
                  var16.add(this.a(String.valueOf(var18), ((Map)var29).get(var18), var22.get(String.valueOf(var18)), var4.get(String.valueOf(var18))));
               }

               var27.add(var16);
            }

            var5.put("kind", "table");
            var5.put("columns", var26);
            var5.put("rows", var27);
         } else {
            ArrayList<java.util.Map<java.lang.String,java.lang.Object>> var23 = new ArrayList();
            var7.forEach(var3x -> var23.add(this.a(var3x, null, var20, Map.of())));
            var5.put("kind", "list");
            var5.put("items", var23);
         }
      } else {
         var5.put("kind", "text");
         var5.put("format", var1 instanceof List ? null : var2);
         var5.put(
            "text", var1 instanceof List ? "—" : a(var2, var1, this.p.get("currency") instanceof String var19 ? io.karatelabs.ext.rules.twin.UiProjectionBuilder.b(var19) : null)
         );
      }

      return var5;
   }

   private static List<Object> a(Map<String, Object> var0, Collection<?> var1) {
      ArrayList<Object> var2 = new ArrayList<>(var0.keySet());
      var2.retainAll(var1);
      var1.stream().filter(var1x -> !var2.contains(var1x)).forEach(var2::add);
      return var2;
   }

   private static Map<String, Object> c(Object var0) {
      return var0 instanceof Map var1 ? g(var1) : Map.of();
   }

   static String a(Object var0, Object var1, String var2) {
      if (var1 == null) {
         return "—";
      } else if (var1 instanceof Boolean var10) {
         return var10 ? "Yes" : "No";
      } else {
         String var3 = var1 instanceof String var4 ? var4 : io.karatelabs.ext.rules.twin.CanonicalJson.c(var1);
         if (!(var1 instanceof Number)) {
            return var3;
         } else if ("percent".equals(var0)) {
            return var3 + "%";
         } else if ("money".equals(var0) && var3.matches("-?\\d+(\\.\\d+)?")) {
            boolean var11 = var3.startsWith("-");
            String var5 = var11 ? var3.substring(1) : var3;
            int var6 = var5.indexOf(46);
            String var7 = var6 < 0 ? var5 : var5.substring(0, var6);
            StringBuilder var8 = new StringBuilder();

            for (int var9 = 0; var9 < var7.length(); var9++) {
               if (var9 > 0 && (var7.length() - var9) % 3 == 0) {
                  var8.append(',');
               }

               var8.append(var7.charAt(var9));
            }

            return (var11 ? "-" : "") + (var2 == null ? "" : var2) + var8 + (var6 < 0 ? "" : var5.substring(var6));
         } else {
            return var3;
         }
      }
   }

   private Map<String, Object> a(Map<String, Object> var1) {
      var var2 = g(var1.get("request"));
      var var3 = g(var1.get("response"));
      String var4 = this.i.getUrl() + var2.get("path");
      String var5 = var2.containsKey("body") ? io.karatelabs.ext.rules.twin.CanonicalJson.c(var2.get("body")) : null;
      LinkedHashMap<java.lang.String, java.lang.Object> var6 = new LinkedHashMap();
      var6.put("method", var2.get("method"));
      var6.put("url", var4);
      var6.put("requestBody", var5);
      var6.put("status", var3.get("status"));
      var6.put("responseBody", var3.get("body") == null ? null : io.karatelabs.ext.rules.twin.CanonicalJson.c(var3.get("body")));
      var6.put("json", io.karatelabs.ext.rules.twin.CanonicalJson.c(var1));
      StringBuilder var7 = new StringBuilder("curl -X ").append(var2.get("method")).append(' ').append(d(var4));
      if (var5 != null) {
         var7.append(" -H 'Content-Type: application/json' -d ").append(d(var5));
      }

      var6.put("curl", var7.toString());
      return var6;
   }

   private static String d(String var0) {
      return "'" + var0.replace("'", "'\\''") + "'";
   }

   static String a(Object var0) {
      String var1 = String.valueOf(var0);

      return switch (var1) {
         case "applied" -> "Done";
         case "refused" -> "Refused";
         case "invalid" -> "Not sent";
         case "error" -> "Error";
         default -> String.valueOf(var0);
      };
   }

   static String a(String var0) {
      return switch (var0) {
         case "available" -> "Available";
         case "unavailable" -> "Not available";
         case "refusal expected" -> "Refusal expected";
         default -> var0;
      };
   }

   private String e(String var1) {
      return g(this.p.get("commands")).get(var1) instanceof Map var3 ? String.valueOf(var3.get("title")) : io.karatelabs.ext.rules.twin.UiProjectionBuilder.a(var1);
   }

   private Object d(Object var1) {
      return var1 == null ? null : g(this.p.get("reasons")).getOrDefault(String.valueOf(var1), var1);
   }

   private List<Map<String, Object>> h() {
      ArrayList<java.util.Map<java.lang.String,java.lang.Object>> var1 = new ArrayList();
      Object var3 = this.i.getVariable("records");
      if (var3 instanceof List) {
         for (Object var4 : (List)var3) {
            var1.add(g(var4));
         }
      }

      return var1;
   }

   private Map<String, Object> f(String var1) {
      for (Map var3 : this.h()) {
         if (f(var3.get("identity")).equals(var1)) {
            return var3;
         }
      }

      return null;
   }

   private List<Map<String, Object>> g(String var1) {
      var var2 = this.f(var1);
      if (var2 == null) {
         return List.of();
      } else {
         try {
            return io.karatelabs.ext.rules.twin.PlayRecordStore.a(this.j, String.valueOf(var2.get("id"))).d();
         } catch (io.karatelabs.ext.rules.RuleApiException var4) {
            return List.of();
         }
      }
   }

   private Map<String, Object> b(Map<String, Object> var1) {
      String var2 = f(var1.get("identity"));
      LinkedHashMap<java.lang.String, java.lang.Object> var3 = new LinkedHashMap();
      var3.put("key", var2);
      var3.put("identity", var1.get("identity"));
      var3.put("record", var1.get("id"));
      var3.put("href", k(var2));
      var3.put("play", this.l == null ? null : this.l + "/rulebooks/" + l(this.f) + "?ref=twin&play=" + l(String.valueOf(var1.get("id"))));
      return var3;
   }

   private Object e(Object var1) {
      Object var2 = this.h(f(var1));
      if (var2 == null) {
         return null;
      } else {
         return this.n.observe(io.karatelabs.ext.rules.twin.TwinSourceCompiler.a(this.g.k(), var2)).get("response") instanceof Map var4
               && Integer.valueOf(200).equals(var4.get("status"))
            ? var4.get("body")
            : null;
      }
   }

   private Object h(String var1) {
      return var1 != null && this.i.getVariable("worlds") instanceof Map var2 ? var2.get(var1) : null;
   }

   private List<String> i() {
      return io.karatelabs.ext.rules.twin.UiProjectionBuilder.a(this.g.k());
   }

   private String j() {
      return "read" + this.g.k().h();
   }

   private Map<String, Map<String, Object>> i(String var1) {
      LinkedHashMap<java.lang.String, java.util.Map<java.lang.String,java.lang.Object>> var2 = new LinkedHashMap();
      Object var3 = this.k.next(this.f, Map.of("record", var1)).get("commands");
      if (var3 instanceof List) {
         for (Object var6 : (List)var3) {
            var2.put(String.valueOf(g(var6).get("command")), g(var6));
         }
      }

      return var2;
   }

   private Map<String, Object> a(String var1, Map<String, Object> var2, io.karatelabs.ext.rules.twin.TwinModel.CommandModel var3) {
      LinkedHashMap<java.lang.String, java.lang.Object> var4 = new LinkedHashMap();
      var4.put("key", var1);
      boolean var5 = var2 != null && Boolean.TRUE.equals(var2.get("enabled"));
      var4.put("enabled", var5);
      String var6 = var5 ? null : var3.o().get("guard");
      var4.put("reason", var6);
      var4.put("reasonText", var5 ? null : (var6 == null ? "not available in this state" : this.d((Object)var6)));
      var4.put("badge", var5 ? "available" : "unavailable");
      var4.put("expects", null);
      if (var5 && var2.get("refusal") instanceof Map var7) {
         String var9 = String.valueOf(var7.get("reason"));
         var4.put("reasonText", "Available — the model expects a refusal: " + this.d((Object)var9));
         var4.put("badge", "refusal expected");
         var4.put("expects", a(var3, var9));
      }

      var4.put("badgeText", a(String.valueOf(var4.get("badge"))));
      return var4;
   }

   private List<io.karatelabs.ext.rules.twin.TwinModel.CommandModel> k() {
      ArrayList<io.karatelabs.ext.rules.twin.TwinModel.CommandModel> var1 = new ArrayList();

      for (io.karatelabs.ext.rules.twin.TwinModel.CommandModel var3 : this.g.c()) {
         if (var3.q() != null) {
            var1.add(var3);
         }
      }

      return var1;
   }

   private io.karatelabs.ext.rules.twin.TwinModel.CommandModel j(String var1) {
      for (io.karatelabs.ext.rules.twin.TwinModel.CommandModel var3 : this.k()) {
         if (var3.q().a().equals(var1)) {
            return var3;
         }
      }

      return null;
   }

   private static String f(Object var0) {
      return var0 instanceof String var1 ? var1 : io.karatelabs.ext.rules.twin.CanonicalJson.c(var0);
   }

   private static String k(String var0) {
      return "/worlds/" + l(var0);
   }

   private static String l(String var0) {
      return URLEncoder.encode(var0, StandardCharsets.UTF_8).replace("+", "%20");
   }

   private static Map<String, Object> g(Object var0) {
      return (Map<String, Object>)var0;
   }

   private static List<?> h(Object var0) {
      return var0 instanceof List var1 ? var1 : List.of();
   }

   private final class Global extends JsDispatch {
      Global() {
         this.lazy("ui", () -> io.karatelabs.ext.rules.twin.CanonicalJson.d(PlayApp.this.p));
         this.fn("worlds", var1x -> PlayApp.this.e());
         this.fn("world", var1x -> PlayApp.this.b(JsApi.str(var1x, 0)));
         this.fn("creates", var1x -> PlayApp.this.f());
         this.fn("timeline", var1x -> PlayApp.this.c(JsApi.str(var1x, 0)));
         this.fn("docs", var1x -> PlayApp.this.g());
         this.fn("act", var1x -> {
            var var2 = PlayApp.this.a(JsApi.str(var1x, 0), JsApi.str(var1x, 1), JsApi.arg(var1x, 2));
            var2.put("outcomeText", PlayApp.a(var2.get("outcome")));
            return var2;
         });
      }

      @Override
      public Collection<String> jsKeys() {
         return List.of("ui");
      }
   }
}
