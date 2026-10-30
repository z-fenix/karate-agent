package io.karatelabs.ext.postman;

import io.karatelabs.common.Json;
import io.karatelabs.common.Resource;
import io.karatelabs.common.ResourceType;
import io.karatelabs.common.StringUtils;
import io.karatelabs.http.HttpResponse;
import io.karatelabs.js.SimpleObject;
import io.karatelabs.output.LogContext;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.io.UncheckedIOException;
import java.net.ConnectException;
import java.net.NoRouteToHostException;
import java.net.URI;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.UnaryOperator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.net.ssl.SSLException;

public final class PostmanCollectionRunner {
   public static final String a = "__postman";
   public static final String b = "postman.jsonl";
   private static final int c = 4096;
   private static final Pattern d = Pattern.compile("^\\s*\\{\\{([^{}]+)}}");
   private static final Pattern e = Pattern.compile("^[A-Za-z][A-Za-z0-9+.-]*://([^/?#]+)");
   private final PmRuntime f;
   private final List<io.karatelabs.ext.postman.PostmanCollectionRunner.RequestScenario> g = new ArrayList<>();
   private final List<Map<String, Object>> h;
   private final UnaryOperator<String> i;
   private final Path j;
   private final String k;
   private final Map<String, Object>[] l;
   private int m = -1;

   public PostmanCollectionRunner(PostmanCollectionNode var1, PostmanCollectionNode var2, Path var3, Map<String, Object> var4, List<Map<String, Object>> var5, UnaryOperator<String> var6, Path var7) {
      this.h = var5;
      this.i = var6;
      this.j = var7;
      this.k = var1.d() == null ? "collection" : var1.d();
      this.f = new PmRuntime(Resource.path(var3.toString()));
      io.karatelabs.ext.postman.PostmanAuthConverter.a(var1, var4);
      this.f.initCollectionVariables(var1);
      ArrayList var8 = new ArrayList();
      if (var4 != null) {
         var4.forEach((var1x, var2x) -> {
            if (var2x instanceof String || var2x instanceof Number || var2x instanceof Boolean) {
               LinkedHashMap var3x = new LinkedHashMap();
               var3x.put("key", var1x);
               var3x.put("value", var2x);
               var8.add(var3x);
            }
         });
      }

      this.f.setEnvironmentVariables(var8);
      ArrayList var9 = new ArrayList();
      a(var2, var9);
      int var10 = var5 == null ? 1 : var5.size();

      for (int var11 = 0; var11 < var10; var11++) {
         for (PostmanCollectionNode var13 : (Iterable<PostmanCollectionNode>) (Iterable<?>) (var9)) {
            this.g.add(new io.karatelabs.ext.postman.PostmanCollectionRunner.RequestScenario(var11, var13, io.karatelabs.ext.postman.PostmanCollectionTreeMapper.a(var13), a(var13, var11)));
         }
      }

      this.l = new Map[this.g.size()];
   }

   private static void a(PostmanCollectionNode var0, List<PostmanCollectionNode> var1) {
      if (var0.b()) {
         var1.add(var0);
      }

      for (PostmanCollectionNode var3 : var0.d) {
         a(var3, var1);
      }
   }

   private static String a(PostmanCollectionNode var0, int var1) {
      ArrayList var2 = new ArrayList();

      for (PostmanCollectionNode var3 = var0; var3.c != null; var3 = var3.c) {
         var2.add(0, var3.d() == null ? "(unnamed)" : var3.d());
      }

      return b(String.join(" / ", var2)) + " [row " + var1 + "]";
   }

   private static String b(String var0) {
      return var0.replaceAll("[\\r\\n]+", " ");
   }

   public List<io.karatelabs.ext.postman.PostmanCollectionRunner.RequestScenario> a() {
      return this.g;
   }

   public int b() {
      return this.h == null ? 1 : this.h.size();
   }

   public String c() {
      StringBuilder var1 = new StringBuilder("Feature: ").append(b(this.k)).append('\n');

      for (int var2 = 0; var2 < this.g.size(); var2++) {
         var1.append("\n  Scenario: ")
            .append(this.g.get(var2).d())
            .append('\n')
            .append("    * eval ")
            .append("__postman")
            .append(".send(")
            .append(var2)
            .append(")\n");
      }

      return var1.toString();
   }

   public SimpleObject d() {
      return var1 -> "send".equals(var1) ? (io.karatelabs.js.JavaCallable) (var1x, var2) -> this.a(((Number)var2[0]).intValue()) : null;
   }

   public synchronized Object a(int var1) {
      io.karatelabs.ext.postman.PostmanCollectionRunner.RequestScenario var2 = this.g.get(var1);
      if (var2.a() != this.m) {
         this.m = var2.a();
         this.f.iteration(this.m, this.h);
      }

      LogContext var3 = LogContext.get();
      boolean var4 = var3.isCapture();
      var3.setCapture(false);

      PostmanRequestRunResult var5;
      try {
         var5 = this.f.request(var2.b());
      } finally {
         var3.setCapture(var4);
      }

      var var6 = this.a(var1, this.a(var2, var5));
      a(var3, var6);
      String var7 = (String)var6.get("status");
      switch (var7) {
         case "passed":
            return null;
         case "notchecked":
            throw new RuntimeException("not checked: " + var6.get("reason"));
         case "error":
            throw new RuntimeException("error: " + var6.get("error"));
         default:
            throw new RuntimeException("failed: " + a(var6));
      }
   }

   public synchronized List<Map<String, Object>> a(String var1) {
      for (int var2 = 0; var2 < this.l.length; var2++) {
         if (this.l[var2] == null) {
            io.karatelabs.ext.postman.PostmanCollectionRunner.RequestScenario var3 = this.g.get(var2);
            var var4 = this.a(var3);
            var4.put("status", "error");
            var4.put("error", var1 == null ? "did not run" : "did not run: " + var1);
            var4.put("tests", List.of());
            this.a(var2, var4);
         }
      }

      return List.of(this.l);
   }

   private Map<String, Object> a(io.karatelabs.ext.postman.PostmanCollectionRunner.RequestScenario var1) {
      LinkedHashMap var2 = new LinkedHashMap();
      var2.put("row", var1.a());
      var2.put("item", var1.c());
      var2.put("name", var1.b().d());
      return var2;
   }

   private Map<String, Object> a(io.karatelabs.ext.postman.PostmanCollectionRunner.RequestScenario var1, PostmanRequestRunResult var2) {
      var var3 = this.a(var1);
      String var4 = var2.o();
      var3.put("status", var4);
      if ("notchecked".equals(var4)) {
         var3.put("reason", var2.p());
      }

      if (var2.f() != null) {
         var3.put("error", String.valueOf(var2.f().getMessage()));
         if ("error".equals(var4)) {
            var var5 = a(var2.f(), var2.a() == null ? null : var2.a().getUrlAndPath(), var1.b().h());
            if (var5 != null) {
               var3.put("transport", var5);
            }
         }
      }

      ArrayList var8 = new ArrayList();

      for (PostmanTestResult var7 : var2.k()) {
         var8.add(a(var7.a(), var7.b() ? "passed" : (var7.d() ? "notsupported" : "failed"), var7.c()));
      }

      for (String var11 : var2.l()) {
         var8.add(a(var11, "notsupported", "not supported: " + var11));
      }

      var3.put("tests", var8);
      if (var2.m() != null) {
         var3.put("sent", var2.m());
      }

      HttpResponse var10 = var2.b();
      if (var10 != null) {
         var3.put("response", a(var10));
      }

      return var3;
   }

   static Map<String, Object> a(Throwable var0, String var1, String var2) {
      String var3 = null;

      for (Throwable var4 = var0; var4 != null && var3 == null; var4 = var4.getCause() == var4 ? null : var4.getCause()) {
         if (var4 instanceof UnknownHostException) {
            var3 = "dns";
         } else if (var4 instanceof SSLException) {
            var3 = "tls";
         } else if (var4 instanceof ConnectException || var4 instanceof NoRouteToHostException) {
            var3 = "unreachable";
         } else if (var4 instanceof InterruptedIOException) {
            var3 = "timeout";
         }
      }

      if (var3 == null) {
         return null;
      } else {
         LinkedHashMap var7 = new LinkedHashMap();
         var7.put("kind", var3);
         String var5 = c(var1);
         if (var5 != null) {
            var7.put("host", var5);
            var7.put("url", var1);
         }

         Matcher var6 = var2 == null ? null : d.matcher(var2);
         if (var6 != null && var6.find()) {
            var7.put("variable", var6.group(1).trim());
         }

         return var7;
      }
   }

   private static String c(String var0) {
      if (var0 != null && !var0.isBlank()) {
         try {
            URI var1 = URI.create(var0.trim());
            if (var1.getHost() != null) {
               return var1.getPort() < 0 ? var1.getHost() : var1.getHost() + ":" + var1.getPort();
            }
         } catch (IllegalArgumentException var2) {
         }

         Matcher var3 = e.matcher(var0.trim());
         return var3.find() ? var3.group(1) : null;
      } else {
         return null;
      }
   }

   private static Map<String, Object> a(String var0, String var1, String var2) {
      LinkedHashMap var3 = new LinkedHashMap();
      var3.put("name", var0);
      var3.put("status", var1);
      if (var2 != null) {
         var3.put("message", var2);
      }

      return var3;
   }

   private static Map<String, Object> a(HttpResponse var0) {
      LinkedHashMap var1 = new LinkedHashMap();
      var1.put("status", var0.getStatus());
      var1.put("timeMs", var0.getResponseTime());
      byte[] var2 = var0.getBodyBytes();
      var1.put("size", var2 == null ? 0 : var2.length);
      LinkedHashMap var3 = new LinkedHashMap();
      if (var0.getHeaders() != null) {
         var0.getHeaders().forEach((var1x, var2x) -> var3.put(var1x, var2x != null && var2x.size() == 1 ? var2x.get(0) : var2x));
      }

      var1.put("headers", var3);
      ResourceType var4 = var0.getResourceType();
      String var5 = var2 == null || var2.length == 0
         ? "empty"
         : (var4 != null && var4.isJson() ? "json" : (var4 != null && var4.isXml() ? "xml" : (var4 != null && var4.isBinary() ? "binary" : "text")));
      var1.put("bodyShape", var5);
      if (!"empty".equals(var5) && !"binary".equals(var5) && var2.length <= 4096) {
         var1.put("body", new String(var2, StandardCharsets.UTF_8));
      }

      return var1;
   }

   private Map<String, Object> a(int var1, Map<String, Object> var2) {
      String var3 = this.i.apply(StringUtils.formatJson(var2, false, false, false));
      Map var4 = (Map)Json.parseStrict(var3, true);
      this.l[var1] = var4;

      try {
         Files.createDirectories(this.j.getParent());
         Files.writeString(
            this.j, StringUtils.formatJson(var4, false, false, false) + "\n", StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND
         );
         return var4;
      } catch (IOException var6) {
         throw new UncheckedIOException(var6);
      }
   }

   private static void a(LogContext var0, Map<String, Object> var1) {
      StringBuilder var2 = new StringBuilder();
      if (var1.get("sent") instanceof Map var3) {
         var2.append(var3.get("method")).append(' ').append(var3.get("url")).append('\n');
         if (var3.get("headers") instanceof Map var8) {
            var8.forEach((var1x, var2x) -> var2.append("> ").append(var1x).append(": ").append(var2x).append('\n'));
         }

         if (var3.get("body") != null) {
            var2.append(var3.get("body")).append('\n');
         }
      }

      if (var1.get("response") instanceof Map var6) {
         var2.append("< ").append(var6.get("status")).append(" (").append(var6.get("timeMs")).append(" ms)\n");
         if (var6.get("body") != null) {
            var2.append(var6.get("body")).append('\n');
         }
      }

      if (var1.get("error") != null) {
         var2.append("error: ").append(var1.get("error")).append('\n');
      }

      var0.log(var2.toString());

      for (Map var10 : (Iterable<Map>) (Iterable<?>) ((List)var1.get("tests"))) {
         String var11 = var10.get("status") + ": " + var10.get("name") + (var10.get("message") == null ? "" : " — " + var10.get("message"));
         var0.step(var11, "passed".equals(var10.get("status")));
      }
   }

   private static String a(Map<String, Object> var0) {
      ArrayList var1 = new ArrayList();

      for (Map var3 : (Iterable<Map>) (Iterable<?>) ((List)var0.get("tests"))) {
         if ("failed".equals(var3.get("status"))) {
            var1.add(var3.get("name") + (var3.get("message") == null ? "" : " (" + var3.get("message") + ")"));
         }
      }

      if (var0.get("error") != null) {
         var1.add("script error: " + var0.get("error"));
      }

      return String.join("; ", var1);
   }

   public record RequestScenario(int a, PostmanCollectionNode b, String c, String d) {
   }
}
