package io.karatelabs.ext.robot.recording;

import io.karatelabs.common.Json;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.Map.Entry;

final class RecordedExchange {
   static final Set<String> a = Set.of("XHR", "Fetch", "Document");
   final String b;
   final List<io.karatelabs.ext.robot.recording.RecordedExchange.ExchangeBuildState> c = new ArrayList<>();
   final List<Map<String, Object>> d = new ArrayList<>();
   final List<Map<String, Object>> e = new ArrayList<>();
   boolean f;
   String g;
   String h;
   String i;
   boolean j;
   boolean k;
   String l;
   Long m;
   double n;
   io.karatelabs.ext.robot.recording.RecordedExchange.RecordedBody o;
   io.karatelabs.ext.robot.recording.RecordedExchange.RecordedBody p;
   boolean q;
   boolean r;
   Set<String> s = Set.of();

   RecordedExchange(String var1) {
      this.b = var1;
   }

   io.karatelabs.ext.robot.recording.RecordedExchange.ExchangeBuildState a() {
      return this.c.isEmpty() ? null : this.c.get(this.c.size() - 1);
   }

   io.karatelabs.ext.robot.recording.RecordedExchange.ExchangeBuildState b() {
      return this.c.isEmpty() ? null : this.c.get(0);
   }

   Double c() {
      io.karatelabs.ext.robot.recording.RecordedExchange.ExchangeBuildState var1 = this.b();
      return var1 != null && !(var1.c <= 0.0) ? var1.c * 1000.0 : null;
   }

   Map<String, Object> d() {
      io.karatelabs.ext.robot.recording.RecordedExchange.ExchangeBuildState var1 = this.a();
      return var1 == null ? null : var1.d;
   }

   boolean e() {
      io.karatelabs.ext.robot.recording.RecordedExchange.ExchangeBuildState var1 = this.b();
      return var1 != null && var1.a != null && (var1.a.get("postData") != null || Boolean.TRUE.equals(var1.a.get("hasPostData")));
   }

   boolean f() {
      return !this.f && (this.g != null && a.contains(this.g) || this.e());
   }

   boolean g() {
      return this.k && this.d() == null;
   }

   boolean h() {
      var var1 = this.d();
      io.karatelabs.ext.robot.recording.RecordedExchange.ExchangeBuildState var2 = this.b();
      return (var1 == null ? null : var1.get("status")) instanceof Number var4 && (var4.intValue() == 204 || var4.intValue() == 205 || var4.intValue() == 304)
         || var2 != null && var2.a != null && "HEAD".equals(var2.a.get("method"));
   }

   boolean i() {
      return !this.f && !this.k && this.d() == null;
   }

   boolean j() {
      if (this.f || this.k || "EventSource".equals(this.g)) {
         return false;
      } else if (this.d() != null && this.j) {
         int var1 = this.k();
         return this.e.size() < var1 || this.d.size() < var1 || this.q || this.r;
      } else {
         return true;
      }
   }

   boolean a(Map<String, Object> var1, double var2, Map<String, Object> var4) {
      io.karatelabs.ext.robot.recording.RecordedExchange.ExchangeBuildState var5 = this.a();
      if (var5 != null && this.c.size() >= 2 && var5.b == var2 && var5.a != null && var1 != null) {
         var var6 = this.c.get(this.c.size() - 2).d;
         return Objects.equals(var5.a.get("url"), var1.get("url")) && Objects.equals(var5.a.get("method"), var1.get("method")) && a(var6, var4);
      } else {
         return false;
      }
   }

   static boolean a(Map<String, Object> var0, Map<String, Object> var1) {
      return var0 != null && var1 != null && Objects.equals(var0.get("status"), var1.get("status")) && Objects.equals(var0.get("url"), var1.get("url"));
   }

   int k() {
      int var1 = 0;

      for (io.karatelabs.ext.robot.recording.RecordedExchange.ExchangeBuildState var3 : this.c) {
         if (var3.e) {
            var1++;
         }
      }

      return var1;
   }

   Map<String, Object> a(String var1) {
      LinkedHashMap<java.lang.String, java.lang.Object> var2 = new LinkedHashMap();
      var2.put("row", "exchange");
      var2.put("id", this.b);
      var2.put("after", null);
      var2.put("start", this.c());
      var2.put("type", this.f ? "WebSocket" : this.g);
      var2.put("initiator", this.h);
      if ("Document".equals(this.g) && this.i != null && var1 != null) {
         var2.put("frame", this.i.equals(var1) ? "main" : "sub");
      }

      ArrayList<java.util.Map<java.lang.String,java.lang.Object>> var3 = new ArrayList();
      ArrayList<io.karatelabs.ext.robot.recording.RecordedExchange.ExchangeBuildState> var4 = new ArrayList();

      for (io.karatelabs.ext.robot.recording.RecordedExchange.ExchangeBuildState var6 : this.c) {
         if (var6.e) {
            var4.add(var6);
         }
      }

      boolean var22 = !this.f && this.d.size() == var4.size() && !this.s.contains("Network.requestWillBeSentExtraInfo");
      if (!this.f && !var22) {
         for (int var23 = 0; var23 < this.c.size(); var23++) {
            var3.add(a(var23, "request"));
         }
      }

      for (int var24 = this.e.size(); var24 < var4.size(); var24++) {
         var3.add(a(this.c.indexOf(var4.get(var24)), "response"));
      }

      io.karatelabs.ext.robot.recording.RecordedExchange.ExchangeBuildState var25 = this.b();
      LinkedHashMap<java.lang.String, java.lang.Object> var7 = new LinkedHashMap();
      Map<String, Object> var8 = var25 != null && var25.a != null ? var25.a : Map.of();
      String var9 = a(var8.get("url"));
      var7.put("url", var9 == null ? null : (var9.indexOf(63) < 0 ? var9 : var9.substring(0, var9.indexOf(63))));
      var7.put("method", this.f ? "GET" : a(var8.get("method")));
      var7.put("headers", b((Map<String, Object>)var8.get("headers"), var22 && var25 != null && var25.e ? (Map)this.d.get(0).get("headers") : null));
      var7.put("params", b(var9));
      var7.put("body", this.o == null ? null : this.o.b);
      var2.put("request", var7);
      io.karatelabs.ext.robot.recording.RecordedExchange.ExchangeBuildState var10 = this.a();
      var var11 = this.d();
      if (var11 != null) {
         LinkedHashMap<java.lang.String, java.lang.Object> var12 = new LinkedHashMap();
         var12.put("status", var11.get("status"));
         Map var13 = var10.e && this.e.size() >= var4.size() ? (Map)this.e.get(var4.size() - 1).get("headers") : null;
         var12.put("headers", b((Map<String, Object>)var11.get("headers"), var13));
         var12.put("body", this.p == null ? null : this.p.b);
         var12.put("responseTime", this.n > 0.0 && var10.b > 0.0 ? Math.round((this.n - var10.b) * 1000.0) : null);
         var2.put("response", var12);
      } else if (this.f) {
         var3.add(a(0, "response"));
      }

      io.karatelabs.ext.robot.recording.RecordedExchange.RecordedBody var26 = this.o == null ? io.karatelabs.ext.robot.recording.RecordedExchange.RecordedBody.a : this.o;
      io.karatelabs.ext.robot.recording.RecordedExchange.RecordedBody var27 = this.p == null ? io.karatelabs.ext.robot.recording.RecordedExchange.RecordedBody.a : this.p;
      LinkedHashMap<java.lang.String, java.lang.Object> var14 = new LinkedHashMap();
      var14.put("request", var26.component1);
      var14.put("response", var27.component1);
      LinkedHashMap<java.lang.String, java.lang.String> var15 = new LinkedHashMap();
      if (var26.e != null) {
         var15.put("request", var26.e);
      }

      if (var27.e != null) {
         var15.put("response", var27.e);
      }

      if (!var15.isEmpty()) {
         var14.put("why", var15);
      }

      var2.put("bodyState", var14);
      var2.put("size", a(var26.c, var27.c));
      var2.put("mime", a(var26.d, var27.d));
      ArrayList<java.util.LinkedHashMap> var16 = new ArrayList();
      int var17 = 0;

      for (io.karatelabs.ext.robot.recording.RecordedExchange.ExchangeBuildState var19 : this.c) {
         if (var19.f) {
            LinkedHashMap<java.lang.String, java.lang.Object> var20 = new LinkedHashMap();
            var20.put("status", var19.d == null ? null : var19.d.get("status"));
            Map var21 = b(var19.d == null ? null : (Map)var19.d.get("headers"), var19.e && var17 < this.e.size() ? (Map)this.e.get(var17).get("headers") : null);
            if (var19.e) {
               var17++;
            }

            var20.put("location", a(var21, "location"));
            var20.put("setCookie", b(var21, "set-cookie"));
            var16.add(var20);
         }
      }

      var2.put("redirects", var16);
      var2.put("hops", this.c.size());
      String var28 = var11 != null
         ? null
         : (this.l != null ? this.l : (this.i() ? (this.s.contains("Network.responseReceived") ? "event dropped" : "in-flight at Stop") : null));
      if (var28 != null) {
         var2.put("error", var28);
      }

      if (!var3.isEmpty()) {
         var2.put("headersPartial", var3);
      }

      return var2;
   }

   private static Map<String, Object> a(int var0, String var1) {
      LinkedHashMap<java.lang.String, java.lang.Object> var2 = new LinkedHashMap();
      var2.put("hop", var0);
      var2.put("side", var1);
      return var2;
   }

   private static Map<String, Object> a(Object var0, Object var1) {
      LinkedHashMap<java.lang.String, java.lang.Object> var2 = new LinkedHashMap();
      if (var0 != null) {
         var2.put("request", var0);
      }

      if (var1 != null) {
         var2.put("response", var1);
      }

      return var2;
   }

   static Map<String, Object> b(Map<String, Object> var0, Map<String, Object> var1) {
      LinkedHashMap<java.lang.String, java.lang.Object> var2 = new LinkedHashMap();
      if (var0 != null) {
         var0.forEach((var1x, var2x) -> var2.put(var1x, b(var2x)));
      }

      if (var1 != null) {
         var1.forEach((var1x, var2x) -> {
            var2.keySet().removeIf(var1x::equalsIgnoreCase);
            var2.put(var1x, b(var2x));
         });
      }

      return var2;
   }

   private static List<String> b(Object var0) {
      ArrayList<java.lang.String> var1 = new ArrayList();
      if (var0 != null) {
         for (String var5 : String.valueOf(var0).split("\n")) {
            var1.add(var5);
         }
      }

      return var1;
   }

   static String a(Map<String, Object> var0, String var1) {
      var var2 = b(var0, var1);
      return var2.isEmpty() ? null : (String)var2.get(0);
   }

   static List<String> b(Map<String, Object> var0, String var1) {
      for (Entry var3 : var0.entrySet()) {
         if (((String)var3.getKey()).equalsIgnoreCase(var1)) {
            return b(var3.getValue() instanceof List var4 ? String.join("\n", var4.stream().map(String::valueOf).toList()) : var3.getValue());
         }
      }

      return List.of();
   }

   static Map<String, Object> b(String var0) {
      LinkedHashMap<java.lang.String, java.lang.Object> var1 = new LinkedHashMap();
      if (var0 != null && var0.indexOf(63) >= 0) {
         String var2 = var0.substring(var0.indexOf(63) + 1);
         int var3 = var2.indexOf(35);
         if (var3 >= 0) {
            var2 = var2.substring(0, var3);
         }

         for (String var7 : var2.split("&")) {
            if (!var7.isEmpty()) {
               int var8 = var7.indexOf(61);
               String var9 = d(var8 < 0 ? var7 : var7.substring(0, var8));
               String var10 = var8 < 0 ? "" : d(var7.substring(var8 + 1));
               ((java.util.List)(var1.computeIfAbsent(var9, var0x -> new ArrayList<>()))).add(var10);
            }
         }

         return var1;
      } else {
         return var1;
      }
   }

   private static String d(String var0) {
      try {
         return URLDecoder.decode(var0, StandardCharsets.UTF_8);
      } catch (IllegalArgumentException var2) {
         return var0;
      }
   }

   static String a(Object var0) {
      return var0 == null ? null : String.valueOf(var0);
   }

   static boolean c(String var0) {
      if (var0 == null) {
         return true;
      } else {
         String var1 = var0.toLowerCase(Locale.ROOT);
         return var1.startsWith("text/")
            || var1.contains("json")
            || var1.contains("xml")
            || var1.contains("javascript")
            || var1.contains("x-www-form-urlencoded")
            || var1.contains("html")
            || var1.contains("graphql");
      }
   }

   static io.karatelabs.ext.robot.recording.RecordedExchange.RecordedBody a(String var0, String var1, long var2, long var4) {
      if (var0 != null && !var0.isEmpty()) {
         long var6 = var0.getBytes(StandardCharsets.UTF_8).length;
         if (var6 <= var2 && var6 <= var4) {
            Object var8 = var0;
            if (var1 != null && var1.toLowerCase(Locale.ROOT).contains("json")) {
               try {
                  Object var9 = Json.parseStrict(var0, true);
                  if (var9 instanceof Map || var9 instanceof List) {
                     var8 = var9;
                  }
               } catch (RuntimeException var10) {
               }
            }

            return new io.karatelabs.ext.robot.recording.RecordedExchange.RecordedBody("full", var8, var6, var1, null);
         } else {
            return new io.karatelabs.ext.robot.recording.RecordedExchange.RecordedBody("capped", null, var6, var1, null);
         }
      } else {
         return new io.karatelabs.ext.robot.recording.RecordedExchange.RecordedBody("none", null, null, var1, null);
      }
   }

   static io.karatelabs.ext.robot.recording.RecordedExchange.RecordedBody a(String var0, boolean var1, String var2) {
      long var3 = var0 == null ? 0L : (var1 ? Base64.getDecoder().decode(var0).length : var0.getBytes(StandardCharsets.UTF_8).length);
      return new io.karatelabs.ext.robot.recording.RecordedExchange.RecordedBody("binary", null, var3, var2, null);
   }

   record RecordedBody(String component1, Object b, Long c, String d, String e) {
      static final io.karatelabs.ext.robot.recording.RecordedExchange.RecordedBody a = new io.karatelabs.ext.robot.recording.RecordedExchange.RecordedBody("none", null, null, null, null);


      static io.karatelabs.ext.robot.recording.RecordedExchange.RecordedBody a(String var0, Long var1, String var2) {
         return new io.karatelabs.ext.robot.recording.RecordedExchange.RecordedBody("unavailable", null, var1, var2, var0);
      }

      public String a() {
         return this.component1;
      }

      public Object b() {
         return this.b;
      }

      public Long c() {
         return this.c;
      }

      public String d() {
         return this.d;
      }

      public String e() {
         return this.e;
      }
   }

   static final class ExchangeBuildState {
      Map<String, Object> a;
      double b;
      double c;
      Map<String, Object> d;
      boolean e;
      boolean f;
   }
}
