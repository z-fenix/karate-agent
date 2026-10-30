package io.karatelabs.ext.rules;

import io.karatelabs.common.Json;
import io.karatelabs.http.Http;
import io.karatelabs.http.HttpResponse;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class HttpTwinTransport implements io.karatelabs.ext.rules.twin.TwinReplayEngine.ReplayObserver {
   private final String a;
   private final int b;

   public HttpTwinTransport(String var1, int var2) {
      if (var1 != null && !var1.isBlank()) {
         String var3 = var1.trim();

         while (var3.endsWith("/")) {
            var3 = var3.substring(0, var3.length() - 1);
         }

         this.a = var3;
         this.b = var2;
      } else {
         throw new io.karatelabs.ext.rules.twin.TwinException(400, "TWIN_INVALID", "baseUrl is required");
      }
   }

   @Override
   public Map<String, Object> observe(Map<String, Object> var1) {
      String var2 = b(var1);
      String var3 = c(var1);
      Http var4 = Http.to(this.a + var3);
      var4.configure("readTimeout", this.b);
      var4.configure("connectTimeout", this.b);

      HttpResponse var5;
      try {
         if (var1.containsKey("body")) {
            var4.contentType("application/json");
            var5 = var4.method(var2, io.karatelabs.ext.rules.twin.CanonicalJson.c(var1.get("body")));
         } else {
            var5 = var4.method(var2);
         }
      } catch (RuntimeException var7) {
         return a(var1, null);
      }

      return a(var1, var5);
   }

   private static Map<String, Object> a(Map<String, Object> var0, HttpResponse var1) {
      LinkedHashMap<java.lang.String, java.lang.Object> var2 = new LinkedHashMap();
      var2.put("request", var0);
      if (var1 == null) {
         var2.put("transportFailure", true);
         return var2;
      } else {
         LinkedHashMap<java.lang.String, java.lang.Object> var3 = new LinkedHashMap();
         var3.put("status", var1.getStatus());
         String var4 = a(var1);
         if (var4 != null && !var4.isEmpty()) {
            var3.put("body", a(var4));
         }

         var2.put("response", var3);
         return var2;
      }
   }

   private static String a(HttpResponse var0) {
      try {
         return var0.getBodyString();
      } catch (RuntimeException var2) {
         return null;
      }
   }

   private static Object a(String var0) {
      try {
         return Json.parseStrict("[" + var0 + "]", true) instanceof List var2 && var2.size() == 1 ? var2.get(0) : var0;
      } catch (RuntimeException var3) {
         return var0;
      }
   }

   private static String b(Map<String, Object> var0) {
      Object var1 = var0 == null ? null : var0.get("method");
      if (var1 instanceof String var2 && var2.matches("[A-Z][A-Z0-9_-]*")) {
         return var2;
      } else {
         throw new io.karatelabs.ext.rules.twin.TwinException(400, "TWIN_INVALID", "request.method must be an upper-case ASCII token, got: " + var1);
      }
   }

   private static String c(Map<String, Object> var0) {
      Object var1 = var0.get("path");
      if (var1 instanceof String var2 && var2.startsWith("/") && !var2.startsWith("//")) {
         return var2;
      } else {
         throw new io.karatelabs.ext.rules.twin.TwinException(400, "TWIN_INVALID", "request.path must be relative and start with '/', got: " + var1);
      }
   }
}
