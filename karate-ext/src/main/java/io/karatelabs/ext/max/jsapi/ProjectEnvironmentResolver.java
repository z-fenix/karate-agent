package io.karatelabs.ext.max.jsapi;

import io.karatelabs.common.Json;
import io.karatelabs.core.BootBinding;
import io.karatelabs.core.BootLoader;
import io.karatelabs.core.ConfigLoader;
import io.karatelabs.ext.common.Sha256;
import io.karatelabs.ext.max.apitest.KarateConfigBlock;
import java.io.IOException;
import java.io.InputStream;
import java.net.ConnectException;
import java.net.URI;
import java.net.UnknownHostException;
import java.net.http.HttpClient;
import java.net.http.HttpConnectTimeoutException;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.net.http.HttpClient.Redirect;
import java.net.http.HttpClient.Version;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.UnresolvedAddressException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

final class ProjectEnvironmentResolver {
   static final String a = "appUrl";
   static final Duration b = Duration.ofSeconds(2L);
   static final Duration c = Duration.ofSeconds(3L);
   static final String d = "No appUrl for this env: declare the app under test in karate-config.js — return { appUrl: 'https://…' } (branch on karate.env for one URL per env).";
   private final Path e;
   private final IntSupplier f;
   private final Supplier<ConsoleState> g;
   private final Map<String, io.karatelabs.ext.max.jsapi.ProjectEnvironmentResolver.ProjectEnvironmentSnapshot> h = new ConcurrentHashMap<>();
   private final ReentrantLock i = new ReentrantLock();
   private volatile io.karatelabs.ext.max.jsapi.ProjectEnvironmentResolver.ProjectEnvironmentSnapshot j;
   private static final Map<String, String> k = Map.of("bearer", "Bearer", "basic", "Basic", "apikey", "API key", "oauth2", "OAuth2");
   private static final String l = "karate-boot.js";

   ProjectEnvironmentResolver(Path var1, IntSupplier var2, Supplier<ConsoleState> var3) {
      this.e = var1;
      this.f = var2;
      this.g = var3;
   }

   void a() {
      this.h.clear();
   }

   Map<String, Object> a(String var1) {
      LinkedHashMap<java.lang.String, java.lang.Object> var2 = new LinkedHashMap();
      var2.put("env", var1);
      var2.put("var", "appUrl");
      io.karatelabs.ext.max.jsapi.ProjectEnvironmentResolver.ProjectEnvironmentSnapshot var3 = this.f(var1);
      var2.put("url", var3.b());
      if (var3.c() != null) {
         var2.put("error", var3.c());
      } else if (var3.b() == null) {
         var2.put(
            "hint",
            "No appUrl for this env: declare the app under test in karate-config.js — return { appUrl: 'https://…' } (branch on karate.env for one URL per env)."
         );
      }

      return var2;
   }

   Map<String, Object> b(String var1) {
      io.karatelabs.ext.max.jsapi.ProjectEnvironmentResolver.ProjectEnvironmentSnapshot var2 = this.f(var1);
      LinkedHashMap<java.lang.String, java.lang.Object> var3 = new LinkedHashMap();
      var3.put("env", var1);
      if (var2.c() != null) {
         var3.put("scheme", null);
         var3.put("error", var2.c());
         return var3;
      } else {
         var3.putAll(var2.d());
         return var3;
      }
   }

   Map<String, Object> c(String var1) {
      return this.f(var1).e();
   }

   private static Map<String, Object> a(Map<String, Object> var0) {
      LinkedHashMap<java.lang.String, java.lang.Object> var1 = new LinkedHashMap();
      var0.forEach((var1x, var2) -> {
         if ((var2 instanceof String || var2 instanceof Number || var2 instanceof Boolean) && !KarateConfigBlock.credentialNamed(var1x)) {
            var1.put(var1x, var2);
         }
      });
      return var1;
   }

   private io.karatelabs.ext.max.jsapi.ProjectEnvironmentResolver.ProjectEnvironmentSnapshot f(String var1) {
      String var2 = var1 == null ? "" : var1;
      String var3 = this.h(var1);
      io.karatelabs.ext.max.jsapi.ProjectEnvironmentResolver.ProjectEnvironmentSnapshot var4 = this.h.get(var2);
      if (var4 == null || !var4.a().equals(var3)) {
         this.i.lock();

         try {
            var4 = this.h.get(var2);
            if (var4 == null || !var4.a().equals(var3)) {
               try {
                  var var5 = ConfigLoader.configOnly(this.e, var1, null, this.g.get().overlay());
                  Object var6 = var5.get("appUrl");
                  var4 = new io.karatelabs.ext.max.jsapi.ProjectEnvironmentResolver.ProjectEnvironmentSnapshot(
                     var3, var6 != null && !String.valueOf(var6).isBlank() ? String.valueOf(var6).trim() : null, null, a(var5.get("auth"), var1), a(var5)
                  );
               } catch (Exception var10) {
                  var4 = new io.karatelabs.ext.max.jsapi.ProjectEnvironmentResolver.ProjectEnvironmentSnapshot(var3, null, "karate-config.js failed: " + a((Throwable)var10), null, Map.of());
               }

               this.h.put(var2, var4);
            }
         } finally {
            this.i.unlock();
         }
      }

      return var4;
   }

   static Map<String, Object> a(Object var0, String var1) {
      LinkedHashMap<java.lang.String, java.lang.Object> var2 = new LinkedHashMap();
      Object var3 = var0 instanceof Map var4 ? var4.get("scheme") : null;
      String var9 = var3 == null ? null : k.get(String.valueOf(var3).toLowerCase(Locale.ROOT));
      if (var9 == null) {
         var2.put("scheme", null);
         var2.put("label", "No sign-in");
         return var2;
      } else {
         Map<java.lang.String, Object> var5 = (Map)var0;
         String var7 = String.valueOf(var3).toLowerCase(Locale.ROOT);

         String var6 = switch (var7) {
            case "basic" -> a(var5.get("username"));
            case "oauth2" -> a(var5.get("clientId"));
            case "bearer" -> g(a(var5.get("token")));
            default -> null;
         };
         var2.put("scheme", String.valueOf(var3).toLowerCase(Locale.ROOT));
         if (var6 != null) {
            var2.put("identity", var6);
         }

         var7 = a(var5.get("login"));
         if (var7 != null) {
            var2.put("login", var7);
         }

         var2.put(
            "label",
            var9 + " · " + (var7 != null ? "from " + var7.substring(var7.lastIndexOf(47) + 1) : (var6 != null ? var6 : (var1 != null ? var1 : "default")))
         );
         return var2;
      }
   }

   private static String a(Object var0) {
      return var0 instanceof String var1 && !var1.isBlank() ? var1 : null;
   }

   private static String g(String var0) {
      String[] var1 = var0 == null ? new String[0] : var0.split("\\.");
      if (var1.length < 2) {
         return null;
      } else {
         try {
            String var2 = new String(Base64.getUrlDecoder().decode(var1[1]), StandardCharsets.UTF_8);
            return Json.parseStrict(var2, true) instanceof Map var3 ? a(var3.get("sub")) : null;
         } catch (RuntimeException var5) {
            return null;
         }
      }
   }

   Map<String, Object> d(String var1) {
      var var2 = this.a(var1);
      Object var3 = var2.get("url");
      if (var3 == null) {
         Object var4 = var2.remove("error");
         Object var5 = var2.remove("hint");
         var2.put("reachable", false);
         if (var4 != null) {
            var2.put("error", var4);
         }

         if (var5 != null) {
            var2.put("hint", var5);
         }

         return var2;
      } else if (this.a(i(String.valueOf(var3)))) {
         var2.put("reachable", true);
         return var2;
      } else {
         var2.putAll(e(String.valueOf(var3)));
         return var2;
      }
   }

   private String h(String var1) {
      List var2 = var1 == null ? List.of("karate-base.js", "karate-config.js") : List.of("karate-base.js", "karate-config.js", "karate-config-" + var1 + ".js");
      var var3 = new ArrayList<>(List.of(this.e.resolve("karate-boot.js")));
      io.karatelabs.ext.common.secrets.SecretStore var4 = this.g.get().store();
      if (var4 != null) {
         var3.add(var4.c());
      }

      Path var5 = this.b();

      for (Path var7 : var5.equals(this.e) ? List.of(this.e) : List.of(this.e, var5)) {
         for (String var9 : (Iterable<String>) (Iterable<?>) (var2)) {
            var3.add(var7.resolve(var9));
         }
      }

      return io.karatelabs.ext.common.Sha256.a(var3);
   }

   Path b() {
      String var1 = io.karatelabs.ext.common.Sha256.a(List.of(this.e.resolve("karate-boot.js")));
      io.karatelabs.ext.max.jsapi.ProjectEnvironmentResolver.ProjectEnvironmentSnapshot var2 = this.j;
      String var3;
      if (var2 != null && var2.a().equals(var1)) {
         var3 = var2.b();
      } else {
         try {
            BootBinding var4 = BootLoader.bootOnly(this.e, null);
            var3 = var4 == null ? null : var4.getClasspathDir();
         } catch (RuntimeException var5) {
            var3 = null;
         }

         this.j = new io.karatelabs.ext.max.jsapi.ProjectEnvironmentResolver.ProjectEnvironmentSnapshot(var1, var3, null, null, Map.of());
      }

      return var3 == null ? this.e : this.e.resolve(var3).normalize();
   }

   private boolean a(URI var1) {
      int var2 = this.f.getAsInt();
      if (var1 != null && var2 > 0) {
         int var3 = var1.getPort() != -1 ? var1.getPort() : ("https".equalsIgnoreCase(var1.getScheme()) ? 443 : 80);
         String var4 = var1.getHost();
         return var3 == var2 && ("localhost".equalsIgnoreCase(var4) || var4.startsWith("127.") || "[::1]".equals(var4) || "0.0.0.0".equals(var4));
      } else {
         return false;
      }
   }

   static Map<String, Object> e(String var0) {
      LinkedHashMap<java.lang.String, java.lang.Object> var1 = new LinkedHashMap();
      URI var2 = i(var0);
      if (var2 == null) {
         var1.put("reachable", false);
         var1.put("error", "appUrl '" + var0 + "' is not an http(s) URL — write it as 'http://host:port'");
         return var1;
      } else {
         long var3 = System.nanoTime();
         HttpClient var5 = HttpClient.newBuilder().connectTimeout(b).followRedirects(Redirect.NEVER).version(Version.HTTP_1_1).build();

         try {
            HttpResponse var6 = var5.send(HttpRequest.newBuilder(var2).timeout(c).GET().build(), BodyHandlers.ofInputStream());
            ((InputStream)var6.body()).close();
            var1.put("reachable", true);
            var1.put("status", var6.statusCode());
            var1.put("latencyMs", a(var3));
         } catch (HttpConnectTimeoutException var13) {
            a(var1, var3, "no connection to " + var2.getAuthority() + " within " + b.toMillis() + " ms");
         } catch (HttpTimeoutException var14) {
            a(var1, var3, "no answer from " + var2.getAuthority() + " within " + c.toMillis() + " ms");
         } catch (IOException var15) {
            a(var1, var3, "cannot reach " + var2.getAuthority() + ": " + a(var15));
         } catch (InterruptedException var16) {
            Thread.currentThread().interrupt();
            a(var1, var3, "the probe was interrupted");
         } finally {
            var5.shutdownNow();
         }

         return var1;
      }
   }

   private static URI i(String var0) {
      try {
         URI var1 = new URI(var0);
         String var2 = var1.getScheme();
         boolean var3 = "http".equalsIgnoreCase(var2) || "https".equalsIgnoreCase(var2);
         return var3 && var1.getHost() != null ? var1 : null;
      } catch (Exception var4) {
         return null;
      }
   }

   private static void a(Map<String, Object> var0, long var1, String var3) {
      var0.put("reachable", false);
      var0.put("latencyMs", a(var1));
      var0.put("error", var3);
   }

   private static long a(long var0) {
      return (System.nanoTime() - var0) / 1000000L;
   }

   private static String a(IOException var0) {
      Object var1 = var0;

      while (((java.io.IOException)var1).getCause() != null) {
         var1 = ((java.io.IOException)var1).getCause();
      }

      if (var1 instanceof UnresolvedAddressException || var1 instanceof UnknownHostException) {
         return "unknown host";
      } else {
         return var0 instanceof ConnectException && var1 instanceof ClosedChannelException ? "connection refused" : a((Throwable)var0);
      }
   }

   static String a(Throwable var0) {
      for (Throwable var1 = var0; var1 != null; var1 = var1.getCause()) {
         String var2 = var1.getMessage();
         if (var2 != null && !var2.isBlank()) {
            String var3 = var2.strip().replaceAll("\\s+", " ");
            return var3.length() > 300 ? var3.substring(0, 300) + "…" : var3;
         }
      }

      return var0.getClass().getSimpleName();
   }

   private record ProjectEnvironmentSnapshot(String a, String b, String c, Map<String, Object> d, Map<String, Object> e) {

   }
}
