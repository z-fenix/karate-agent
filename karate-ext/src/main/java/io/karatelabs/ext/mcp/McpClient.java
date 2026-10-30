package io.karatelabs.ext.mcp;

import io.karatelabs.common.Json;
import io.karatelabs.http.Http;
import io.karatelabs.http.HttpResponse;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.BiFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class McpClient {
   private static final Logger g = LoggerFactory.getLogger("karate.mcp");
   public static final String a = "2026-07-28";
   public static final List<String> b = List.of("2025-11-25", "2025-06-18", "2025-03-26");
   static final String c = "io.modelcontextprotocol/";
   static final String d = "auto";
   static final int e = 512;
   private final String h;
   private Map<String, Object> i;
   private int j = 30000;
   private String k = "auto";
   private Map<String, Object> l = new LinkedHashMap<>();
   private final String m;
   private Map<String, Object> n;
   private String o;
   private String p;
   private String q;
   private Map<String, Object> r;
   private long s;
   public static final String f = "result";

   public McpClient(String var1, String var2) {
      this.h = var1;
      this.m = var2 == null ? "dev" : var2;
   }

   public String a() {
      return this.h;
   }

   public Map<String, Object> b() {
      return this.n;
   }

   public void a(Map<String, Object> var1) {
      this.i = var1;
      this.h();
   }

   public void a(int var1) {
      this.j = var1;
   }

   public void a(String var1) {
      this.k = var1 == null ? "auto" : var1;
      this.h();
   }

   public void b(Map<String, Object> var1) {
      this.l = (Map<String, Object>)(var1 == null ? new LinkedHashMap<>() : var1);
      this.h();
   }

   private void h() {
      this.n = null;
      this.o = null;
      this.p = null;
      this.q = null;
      this.r = null;
   }

   public Map<String, Object> c() {
      if (this.n != null) {
         return this.n;
      } else if ("2026-07-28".equals(this.k)) {
         var var4 = this.i();
         return var4.containsKey("ok")
            ? this.a("modern", "2026-07-28", (Map<String, Object>)var4.get("ok"), null)
            : this.a("modern pin: server/discover rejected", List.of(var4));
      } else if (!"auto".equals(this.k)) {
         var var3 = this.a(this.k, true);
         return var3.containsKey("ok") ? this.n : this.a("legacy pin " + this.k + ": initialize rejected", List.of(var3));
      } else {
         var var1 = this.i();
         if (var1.containsKey("ok")) {
            return this.a("modern", "2026-07-28", (Map<String, Object>)var1.get("ok"), null);
         } else if (!Boolean.TRUE.equals(var1.get("fallback"))) {
            return this.a("negotiation terminated by the modern probe", List.of(var1));
         } else {
            var var2 = this.a(b.get(0), false);
            return var2.containsKey("ok") ? this.n : this.a("both eras rejected the connection", List.of(var1, var2));
         }
      }
   }

   private Map<String, Object> i() {
      long var1 = this.k();
      var var3 = this.a("connect/modern-probe", "server/discover", new LinkedHashMap<>(), var1, "modern", null, null);
      Object var4 = var3.get("body");
      int var5 = (Integer)var3.get("status");
      if (var5 == 200 && a(var4, Long.valueOf(var1)) != null) {
         return Map.of("ok", a(var4, Long.valueOf(var1)));
      } else {
         Map<java.lang.String, java.lang.Object> var6 = this.a("modern", var3);
         var var7 = a(var4, var1, true);
         if (var7 != null && var7.get("code") instanceof Number var8 && var8.intValue() == -32601) {
            var6.put("fallback", true);
            return var6;
         } else if (var5 != 400) {
            return var6;
         } else {
            var var13 = a(var4, var1, false);
            Number var14 = var13 != null && var13.get("code") instanceof Number var10 ? var10 : null;
            if (var14 != null && var14.intValue() <= -32020 && var14.intValue() >= -32099) {
               if (var14.intValue() == -32022) {
                  Object var15 = var13.get("data") instanceof Map var16 ? var16.get("supported") : null;
                  boolean var17 = var15 instanceof List var18 && var18.contains("2026-07-28");
                  var6.put(
                     "message",
                     var17
                        ? "server rejected 2026-07-28 while listing it as supported (defective server)"
                        : "no mutually supported modern revision (server supports: " + var15 + ")"
                  );
               }

               return var6;
            } else {
               var6.put("fallback", true);
               return var6;
            }
         }
      }
   }

   private Map<String, Object> a(String var1, boolean var2) {
      long var3 = this.k();
      LinkedHashMap<java.lang.String, java.lang.Object> var5 = new LinkedHashMap();
      var5.put("protocolVersion", var1);
      var5.put("capabilities", this.l);
      var5.put("clientInfo", this.l());
      Map<java.lang.String, java.lang.Object> var6 = this.a("connect/legacy-initialize", "initialize", var5, var3, "bare", null, null);
      var var7 = a(var6.get("body"), Long.valueOf(var3));
      if (var7 == null) {
         return this.a("legacy", var6);
      } else {
         String var8 = var7.get("protocolVersion") instanceof String var9 ? var9 : null;
         boolean var15 = var2 ? var1.equals(var8) : b.contains(var8);
         if (!var15) {
            Map<java.lang.String, java.lang.Object> var17 = this.a("legacy", var6);
            var17.put("selectedVersion", var8);
            return var17;
         } else {
            Object var16 = var6.get("headers");
            String var11 = a(var16, "Mcp-Session-Id");
            this.o = "legacy";
            this.p = var8;
            this.q = var11;

            Map var12;
            try {
               var12 = this.a("connect/initialized", "notifications/initialized", new LinkedHashMap<>(), null, "legacy", null, null);
            } catch (McpException var14) {
               this.h();
               throw var14;
            }

            if ((Integer)var12.get("status") / 100 != 2) {
               this.h();
               Map<java.lang.String, java.lang.Object> var13 = this.a("legacy", var12);
               var13.put("message", "notifications/initialized rejected");
               return var13;
            } else {
               this.a("legacy", var8, var7, var11);
               return Map.of("ok", var7);
            }
         }
      }
   }

   private Map<String, Object> a(String var1, String var2, Map<String, Object> var3, String var4) {
      this.o = var1;
      this.p = var2;
      LinkedHashMap<java.lang.String, java.lang.Object> var5 = new LinkedHashMap();
      var5.put("lane", var1);
      var5.put("revision", var2);
      if (var3.get("supportedVersions") != null) {
         var5.put("supportedVersions", var3.get("supportedVersions"));
      }

      var5.put("capabilities", var3.get("capabilities"));
      if (var3.get("instructions") != null) {
         var5.put("instructions", var3.get("instructions"));
      }

      if (var4 != null) {
         var5.put("sessionId", var4);
      }

      this.n = var5;
      return var5;
   }

   private Map<String, Object> a(String var1, List<Map<String, Object>> var2) {
      this.h();
      boolean var3 = var2.stream().noneMatch(var0 -> a(var0.get("body")));
      LinkedHashMap<java.lang.String, java.lang.Object> var4 = new LinkedHashMap();
      var4.put("failed", true);
      var4.put("message", var3 ? c((Map<String, Object>)var2.get(0)) : var1);
      var4.put("attempts", var2);
      return var4;
   }

   private static String c(Map<String, Object> var0) {
      Object var1 = var0.get("contentType");
      return "not an MCP endpoint: HTTP " + var0.get("status") + " " + (var1 == null ? "no content-type" : var1);
   }

   private Map<String, Object> a(String var1, Map<String, Object> var2) {
      LinkedHashMap<java.lang.String, java.lang.Object> var3 = new LinkedHashMap();
      var3.put("lane", var1);
      var3.put("status", var2.get("status"));
      var3.put("contentType", var2.get("contentType"));
      a(var3, var2.get("body"));
      return var3;
   }

   static void a(Map<String, Object> var0, Object var1) {
      if (var1 instanceof String var2 && var2.length() > 512) {
         var0.put("body", var2.substring(0, 512));
         var0.put("bodyTruncated", true);
      } else {
         var0.put("body", var1);
      }
   }

   public Map<String, Object> a(String var1, Map<String, Object> var2, Map<String, Object> var3) {
      return this.a(var1, var2, var3, new LinkedHashMap<>());
   }

   public Map<String, Object> a(String var1, Map<String, Object> var2, Map<String, Object> var3, Map<String, Object> var4) {
      String var5 = this.o != null ? this.o : ("2026-07-28".equals(this.k) ? "modern" : ("auto".equals(this.k) ? "bare" : "legacy-pinned"));
      long var6 = this.k();
      var var8 = this.a(var5, var1, (Map<String, Object>)(var2 == null ? new LinkedHashMap<>() : var2), var6, var5, null, var3);
      if ("tools/call".equals(var1)) {
         var var9 = a(var8.get("body"), Long.valueOf(var6));
         if (var9 != null) {
            var4.put("result", var9);
         }
      }

      LinkedHashMap<java.lang.String, java.lang.Object> var10 = new LinkedHashMap();
      var10.put("status", var8.get("status"));
      var10.put("headers", var8.get("headers"));
      var10.put("body", var8.get("body"));
      return var10;
   }

   public Map<String, Object> d() {
      var var1 = this.d("tools/list");
      if (var1.get("ok") instanceof Map var2) {
         this.r = var2;
      }

      return var1;
   }

   public Map<String, Object> e() {
      return this.d("resources/list");
   }

   public Map<String, Object> f() {
      return this.d("prompts/list");
   }

   private Map<String, Object> d(String var1) {
      this.j();
      long var2 = this.k();
      var var4 = this.a(this.o, var1, new LinkedHashMap<>(), var2, this.o, null, null);
      return this.a(var4, var2);
   }

   public Map<String, Object> a(
      String var1, Map<String, Object> var2, Map<String, Object> var3, BiFunction<Map<String, Object>, Integer, Map<String, Object>> var4, int var5
   ) {
      return this.a(var1, var2, var3, var4, var5, new LinkedHashMap<>());
   }

   public Map<String, Object> a(
      String var1,
      Map<String, Object> var2,
      Map<String, Object> var3,
      BiFunction<Map<String, Object>, Integer, Map<String, Object>> var4,
      int var5,
      Map<String, Object> var6
   ) {
      this.j();
      Map var7 = null;
      if ("modern".equals(this.o)) {
         if (this.r == null) {
            this.d();
         }

         var var8 = this.e(var1);
         if (var8 != null) {
            Object var9 = io.karatelabs.ext.mcp.McpSchemaNormalizer.a(var8);
            if (var9 instanceof String var22) {
               return Map.of("refused", Map.of("tool", var1, "reason", var22));
            }

            Map var10 = (Map)var9;
            var7 = io.karatelabs.ext.mcp.McpSchemaNormalizer.a(var10, var2);
         }
      }

      LinkedHashMap<java.lang.String, java.lang.Object> var19 = new LinkedHashMap();
      var19.put("name", var1);
      var19.put("arguments", var2 == null ? new LinkedHashMap() : var2);
      int var20 = 0;
      boolean var21 = false;

      while (true) {
         long var11 = this.k();
         Map var13 = this.a(this.o, "tools/call", var19, var11, this.o, var7, null);
         Map<java.lang.String, java.lang.Object> var14 = this.a(var13, var11);
         Map<java.lang.String, Object> var15 = var14.get("ok") instanceof Map var16 ? d(var16) : null;
         if (var15 != null) {
            var6.put("result", var15);
         }

         if (var15 == null || !"input_required".equals(var15.get("resultType"))) {
            return var14;
         }

         if (var20 >= var5) {
            return var14;
         }

         Map var23 = var15.get("inputRequests") instanceof Map var24 ? d(var24) : null;
         Map var25 = null;
         if (var23 != null) {
            var25 = this.a(var23, var3, var4, var20);
            if (var25 == null || var25.isEmpty()) {
               return var14;
            }
         } else {
            if (var21) {
               return var14;
            }

            var21 = true;
         }

         var19 = new LinkedHashMap(var19);
         if (var25 != null) {
            var19.put("inputResponses", var25);
         } else {
            var19.remove("inputResponses");
         }

         if (var15.containsKey("requestState")) {
            var19.put("requestState", var15.get("requestState"));
         } else {
            var19.remove("requestState");
         }

         var20++;
      }
   }

   private Map<String, Object> a(
      Map<String, Object> var1, Map<String, Object> var2, BiFunction<Map<String, Object>, Integer, Map<String, Object>> var3, int var4
   ) {
      if (var3 != null) {
         return (Map<String, Object>)var3.apply(var1, var4);
      } else if (var2 == null) {
         return null;
      } else {
         LinkedHashMap<java.lang.String, java.lang.Object> var5 = new LinkedHashMap();

         for (String var7 : var1.keySet()) {
            if (var2.containsKey(var7)) {
               var5.put(var7, var2.get(var7));
            }
         }

         return var5;
      }
   }

   private Map<String, Object> e(String var1) {
      if (this.r != null) {
         Object var3 = this.r.get("tools");
         if (var3 instanceof List) {
            for (Object var4 : (List)var3) {
               if (var4 instanceof Map var5 && var1.equals(var5.get("name"))) {
                  return d(var5);
               }
            }

            return null;
         }
      }

      return null;
   }

   public void g() {
      if ("legacy".equals(this.o) && this.q != null) {
         LinkedHashMap<String, String> var1 = new LinkedHashMap<>();
         if (this.i != null) {
            this.i.forEach((var1x, var2x) -> a(var1, var1x, var2x == null ? null : var2x.toString()));
         }

         if (this.p != null) {
            a(var1, "MCP-Protocol-Version", this.p);
         }

         a(var1, "Mcp-Session-Id", this.q);

         try {
            Http var2 = Http.to(this.h).configure("readTimeout", this.j).configure("connectTimeout", this.j).configure("followRedirects", false).quiet();
            var1.forEach(var2::header);
            HttpResponse var3 = var2.method("DELETE");
            g.debug("{} {} -> {} {}", new Object[]{"farewell", "DELETE", var3.getStatus(), var3.getHeader("Content-Type")});
         } catch (Exception var4) {
            g.debug("legacy session delete failed: {}", var4.getMessage());
         }
      }

      this.h();
   }

   private void j() {
      if (this.n == null || this.n.containsKey("failed")) {
         throw new IllegalStateException("mcp session is not connected — call connect() first");
      }
   }

   private long k() {
      return ++this.s;
   }

   private Map<String, Object> l() {
      LinkedHashMap<java.lang.String, java.lang.Object> var1 = new LinkedHashMap();
      var1.put("name", "karate");
      var1.put("version", this.m);
      return var1;
   }

   private Map<String, Object> a(
      String var1, String var2, Map<String, Object> var3, Object var4, String var5, Map<String, String> var6, Map<String, Object> var7
   ) {
      LinkedHashMap<java.lang.String, java.lang.Object> var8 = new LinkedHashMap();
      var8.put("jsonrpc", "2.0");
      if (var4 != null) {
         var8.put("id", var4);
      }

      var8.put("method", var2);
      LinkedHashMap var9 = new LinkedHashMap();
      if (this.i != null) {
         this.i.forEach((var1x, var2x) -> a(var9, var1x, var2x == null ? null : var2x.toString()));
      }

      a(var9, "Accept", "application/json, text/event-stream");
      if ("modern".equals(var5)) {
         LinkedHashMap<java.lang.String, java.lang.Object> var10 = new LinkedHashMap();
         var10.put("io.modelcontextprotocol/protocolVersion", "2026-07-28");
         var10.put("io.modelcontextprotocol/clientInfo", this.l());
         var10.put("io.modelcontextprotocol/clientCapabilities", this.l);
         var3 = new LinkedHashMap((Map)var3);
         var3.put("_meta", var10);
         a(var9, "MCP-Protocol-Version", "2026-07-28");
         a(var9, "Mcp-Method", var2);
         String var11 = b(var2, (Map<String, Object>)var3);
         if (var11 != null) {
            a(var9, "Mcp-Name", io.karatelabs.ext.mcp.McpSchemaNormalizer.a(var11));
         }
      } else if ("legacy".equals(var5) || "legacy-pinned".equals(var5)) {
         String var14 = "legacy".equals(var5) && this.p != null ? this.p : this.k;
         if (!"initialize".equals(var2) && !"auto".equals(var14)) {
            a(var9, "MCP-Protocol-Version", var14);
         }

         if (this.q != null) {
            a(var9, "Mcp-Session-Id", this.q);
         }
      }

      if (!var3.isEmpty() || "modern".equals(var5)) {
         var8.put("params", var3);
      }

      if (var6 != null) {
         var6.forEach((var1x, var2x) -> a(var9, var1x, var2x));
      }

      if (var7 != null) {
         var7.forEach((var1x, var2x) -> a(var9, var1x, var2x == null ? null : var2x.toString()));
      }

      HttpResponse var15 = this.a(var8, (Map<String, String>)var9);
      String var16 = var15.getHeader("Content-Type");
      g.debug("{} {} -> {} {}", new Object[]{var1, var2, var15.getStatus(), var16});
      Object var12;
      if (var16 != null && var16.toLowerCase().contains("text/event-stream")) {
         var12 = this.a(var15.getBodyString(), var4);
      } else {
         var12 = f(var15.getBodyString());
      }

      LinkedHashMap<java.lang.String, java.lang.Object> var13 = new LinkedHashMap();
      var13.put("status", var15.getStatus());
      var13.put("headers", var15.getHeaders());
      var13.put("contentType", var16);
      var13.put("body", var12);
      return var13;
   }

   private static void a(Map<String, String> var0, String var1, String var2) {
      var0.keySet().removeIf(var1x -> var1x.equalsIgnoreCase(var1));
      if (var2 != null) {
         var0.put(var1, var2);
      }
   }

   private static String b(String var0, Map<String, Object> var1) {
      return switch (var0) {
         case "tools/call", "prompts/get" -> var1.get("name") instanceof String var6 ? var6 : null;
         case "resources/read" -> var1.get("uri") instanceof String var4 ? var4 : null;
         default -> null;
      };
   }

   private HttpResponse a(Map<String, Object> var1, Map<String, String> var2) {
      try {
         Http var3 = Http.to(this.h).configure("readTimeout", this.j).configure("connectTimeout", this.j).configure("followRedirects", false).quiet();
         var2.forEach(var3::header);
         return var3.postJson(Json.stringifyStrict(var1));
      } catch (Exception var5) {
         String var4 = !(var5.getCause() instanceof SocketTimeoutException) && !(var5 instanceof SocketTimeoutException) ? "connect" : "timeout";
         throw new McpException(var4, var5.getMessage(), var5);
      }
   }

   private static Object f(String var0) {
      if (var0 != null && !var0.isEmpty()) {
         try {
            return Json.parseLenient(var0);
         } catch (Exception var2) {
            return var0;
         }
      } else {
         return null;
      }
   }

   private Object a(String var1, Object var2) {
      if (var1 == null) {
         throw new McpException("decode", "empty event-stream response", null);
      } else {
         ArrayList<java.lang.Object> var3 = new ArrayList();

         for (String var7 : var1.replace("\r\n", "\n").replace('\r', '\n').split("\n\n")) {
            String var8 = "message";
            StringBuilder var9 = new StringBuilder();

            for (String var13 : var7.split("\n")) {
               if (!var13.startsWith(":")) {
                  if (var13.startsWith("event:")) {
                     var8 = var13.substring(6).trim();
                  } else if (var13.startsWith("data:")) {
                     if (var9.length() > 0) {
                        var9.append('\n');
                     }

                     var9.append(var13.startsWith("data: ") ? var13.substring(6) : var13.substring(5));
                  }
               }
            }

            if ("message".equals(var8) && var9.length() != 0) {
               Object var15;
               try {
                  var15 = Json.parseLenient(var9.toString());
               } catch (Exception var14) {
                  continue;
               }

               if (var15 instanceof List) {
                  throw new McpException("decode", "JSON-RPC batch in event stream (unsupported): " + var1, null);
               }

               if (var15 instanceof Map var16
                  && var16.get("id") != null
                  && String.valueOf(var16.get("id")).equals(String.valueOf(var2))
                  && (var16.containsKey("result") || var16.containsKey("error"))) {
                  var3.add(var15);
               }
            }
         }

         if (var3.size() != 1) {
            throw new McpException(
               "decode",
               var3.isEmpty()
                  ? "no terminal answer for id " + var2 + " in event stream: " + var1
                  : "multiple terminal answers for id " + var2 + " in event stream: " + var1,
               null
            );
         } else {
            return var3.get(0);
         }
      }
   }

   private Map<String, Object> a(Map<String, Object> var1, long var2) {
      var var4 = a(var1.get("body"), Long.valueOf(var2));
      if (var4 != null) {
         return Map.of("ok", var4);
      } else {
         var var5 = a(var1.get("body"), var2, false);
         if (var5 != null) {
            LinkedHashMap<java.lang.String, java.lang.Object> var7 = new LinkedHashMap();
            var7.put("status", var1.get("status"));
            var7.put("error", var5);
            return Map.of("error", var7);
         } else {
            LinkedHashMap<java.lang.String, java.lang.Object> var6 = new LinkedHashMap();
            var6.put("status", var1.get("status"));
            var6.put("contentType", var1.get("contentType"));
            var6.put("body", var1.get("body"));
            return Map.of("malformed", var6);
         }
      }
   }

   static boolean a(Object var0) {
      return var0 instanceof Map var1 && (var1.containsKey("jsonrpc") || var1.containsKey("result") || var1.containsKey("error"));
   }

   private static Map<String, Object> a(Object var0, Object var1) {
      return var0 instanceof Map var2 && var2.get("result") instanceof Map var3 && String.valueOf(var2.get("id")).equals(String.valueOf(var1)) ? d(var3) : null;
   }

   private static Map<String, Object> a(Object var0, Object var1, boolean var2) {
      if (var0 instanceof Map var3 && var3.get("error") instanceof Map var4) {
         Object var7 = var3.get("id");
         boolean var6 = var2 ? String.valueOf(var7).equals(String.valueOf(var1)) : var7 == null || String.valueOf(var7).equals(String.valueOf(var1));
         if (var6) {
            return d(var4);
         }
      }

      return null;
   }

   private static Map<String, Object> d(Map<?, ?> var0) {
      return (Map<String, Object>)(Map<?, ?>)var0;
   }

   private static String a(Object var0, String var1) {
      if (var0 instanceof Map var2) {
         for (Entry var4 : (Iterable<Entry>) (Iterable<?>) (var2.entrySet())) {
            if (String.valueOf(var4.getKey()).equalsIgnoreCase(var1)) {
               Object var5 = var4.getValue();
               if (var5 instanceof List var6 && !var6.isEmpty()) {
                  return String.valueOf(var6.get(0));
               }

               return var5 == null ? null : String.valueOf(var5);
            }
         }
      }

      return null;
   }

   static boolean b(String var0) {
      if (var0.startsWith("=?base64?") && var0.endsWith("?=")) {
         return true;
      } else if (!var0.equals(var0.strip())) {
         return true;
      } else {
         for (int var1 = 0; var1 < var0.length(); var1++) {
            char var2 = var0.charAt(var1);
            if (var2 < ' ' || var2 > '~') {
               return true;
            }
         }

         return false;
      }
   }

   static String c(String var0) {
      return "=?base64?" + Base64.getEncoder().encodeToString(var0.getBytes(StandardCharsets.UTF_8)) + "?=";
   }
}
