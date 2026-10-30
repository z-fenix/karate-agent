package io.karatelabs.ext.mcp;

import io.karatelabs.common.Json;
import io.karatelabs.ext.common.CoverageEmit;
import io.karatelabs.ext.common.ExtMeta;
import io.karatelabs.js.JavaCallable;
import io.karatelabs.js.SimpleObject;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class McpSession implements SimpleObject {
   private static final Logger logger = LoggerFactory.getLogger("karate.mcp");
   static final String VERSION = ExtMeta.version("karate-max-meta.properties");
   private static final String URL = "url";
   private static final String HEADERS = "headers";
   private static final String TIMEOUT = "timeout";
   private static final String REVISION = "revision";
   private static final String CAPABILITIES = "capabilities";
   private static final String SERVER = "server";
   private static final String CONNECT = "connect";
   private static final String TOOLS = "tools";
   private static final String RESOURCES = "resources";
   private static final String PROMPTS = "prompts";
   private static final String INVOKE = "invoke";
   private static final String CALL = "call";
   private static final String STOP = "stop";
   private static final String HELP = "help";
   String url;
   Map<String, Object> headers;
   Integer timeoutMs;
   String revision;
   Map<String, Object> capabilities;
   boolean lenient;
   private McpClient client;

   private McpClient client() {
      if (this.client == null) {
         if (this.url == null) {
            throw new RuntimeException("mcp session: 'url' is required (e.g. session.url = 'http://localhost:8080/api/mcp')");
         }

         this.client = new McpClient(this.url, VERSION);
         if (this.headers != null) {
            this.client.a(this.headers);
         }

         if (this.timeoutMs != null) {
            this.client.a(this.timeoutMs.intValue());
         }

         if (this.revision != null) {
            this.client.a(this.revision);
         }

         if (this.capabilities != null) {
            this.client.b(this.capabilities);
         }
      }

      return this.client;
   }

   public Map<String, Object> connect() {
      var var1 = this.client().c();
      if (!Boolean.TRUE.equals(var1.get("failed"))) {
         return var1;
      } else {
         var var2 = connectError(var1);
         if (this.lenient) {
            return var2;
         } else {
            throw connectFailure(var2);
         }
      }
   }

   public Object tools() {
      Object var1 = this.connected();
      return var1 != null ? var1 : this.unwrap("tools", this.client.d());
   }

   public Object resources() {
      Object var1 = this.connected();
      return var1 != null ? var1 : this.unwrap("resources", this.client.e());
   }

   public Object prompts() {
      Object var1 = this.connected();
      return var1 != null ? var1 : this.unwrap("prompts", this.client.f());
   }

   public Object invoke(String var1, Map<String, Object> var2, Map<String, Object> var3) {
      Object var4 = this.connected();
      if (var4 != null) {
         return var4;
      } else {
         Map var5 = null;
         BiFunction var6 = null;
         int var7 = 3;
         if (var3 != null) {
            if (var3.get("inputs") instanceof Map var8) {
               var5 = var8;
            }

            if (var3.get("onInputRequired") instanceof JavaCallable var13) {
               var6 = (var1x, var2x) -> var13.call(null, var1x, var2x) instanceof Map var3x ? var3x : null;
            }

            if (var3.get("maxRounds") instanceof Number var14) {
               var7 = var14.intValue();
            }
         }

         LinkedHashMap var15 = new LinkedHashMap();

         Map var18;
         try {
            var18 = this.client.a(var1, var2, var5, var6, var7, var15);
         } finally {
            emitToolHit(var1, new LinkedHashMap<>(), var15);
         }

         return this.unwrap("invoke '" + var1 + "'", var18);
      }
   }

   public Map<String, Object> call(String var1, Map<String, Object> var2, Map<String, Object> var3) {
      LinkedHashMap var4 = new LinkedHashMap();
      Map var5 = this.client().a(var1, var2, var3, var4);
      if ("tools/call".equals(var1) && var2 != null && var2.get("name") instanceof String var6) {
         LinkedHashMap var8 = new LinkedHashMap();
         var8.put("raw", true);
         emitToolHit(var6, var8, var4);
      }

      return var5;
   }

   public Void stop() {
      if (this.client != null) {
         this.client.g();
      }

      return null;
   }

   private static void emitToolHit(String var0, Map<String, Object> var1, Map<String, Object> var2) {
      if (var2.get("result") instanceof Map var3) {
         if (var3.get("resultType") != null) {
            var1.put("resultType", var3.get("resultType"));
         }

         var1.put("isError", Boolean.TRUE.equals(var3.get("isError")));
         CoverageEmit.emit("mcp", var0, var1);
      }
   }

   private Object connected() {
      if (this.client().b() != null) {
         return null;
      } else {
         var var1 = this.client.c();
         if (!Boolean.TRUE.equals(var1.get("failed"))) {
            return null;
         } else {
            var var2 = connectError(var1);
            if (this.lenient) {
               return var2;
            } else {
               throw connectFailure(var2);
            }
         }
      }
   }

   private Object unwrap(String var1, Map<String, Object> var2) {
      if (var2.containsKey("ok")) {
         return var2.get("ok");
      } else {
         var var3 = flatten(var2);
         if (this.lenient) {
            return var3;
         } else {
            String var4 = (String)var3.get("kind");
            if ("refused".equals(var4)) {
               throw new RuntimeException("mcp " + var1 + " refused: " + var3.get("error"));
            } else if ("malformed".equals(var4)) {
               throw new RuntimeException(
                  "mcp "
                     + var1
                     + " failed: "
                     + var3.get("error")
                     + " [http "
                     + var3.get("status")
                     + " "
                     + var3.get("contentType")
                     + "] body: "
                     + var3.get("body")
               );
            } else {
               throw new RuntimeException(
                  "mcp "
                     + var1
                     + " failed: "
                     + var3.get("error")
                     + " [jsonrpc "
                     + var3.get("code")
                     + ", http "
                     + var3.get("status")
                     + "]"
                     + (var3.containsKey("data") ? " data: " + Json.stringifyStrict(var3.get("data")) : "")
               );
            }
         }
      }
   }

   private static Map<String, Object> flatten(Map<String, Object> var0) {
      LinkedHashMap var1 = new LinkedHashMap();
      if (var0.get("error") instanceof Map var2) {
         Map var5 = (Map)var2.get("error");
         var1.put("error", var5.get("message"));
         var1.put("code", var5.get("code"));
         var1.put("kind", "jsonrpc");
         var1.put("status", var2.get("status"));
         if (var5.containsKey("data")) {
            var1.put("data", var5.get("data"));
         }
      } else if (var0.get("malformed") instanceof Map var3) {
         var1.put("error", "not a JSON-RPC envelope");
         var1.put("kind", "malformed");
         var1.put("status", var3.get("status"));
         if (var3.get("contentType") != null) {
            var1.put("contentType", var3.get("contentType"));
         }

         io.karatelabs.ext.mcp.McpClient.a(var1, var3.get("body"));
      } else {
         Map var7 = (Map)var0.get("refused");
         var1.put("error", var7.get("reason"));
         var1.put("kind", "refused");
         var1.put("tool", var7.get("tool"));
      }

      return var1;
   }

   private static Map<String, Object> connectError(Map<String, Object> var0) {
      LinkedHashMap var1 = new LinkedHashMap();
      var1.put("error", var0.get("message"));
      var1.put("kind", "connect");
      var1.put("attempts", var0.get("attempts"));
      return var1;
   }

   private static RuntimeException connectFailure(Map<String, Object> var0) {
      return new RuntimeException("mcp connect failed: " + var0.get("error") + " — attempts: " + Json.stringifyStrict(var0.get("attempts")));
   }

   private static Map<String, Object> asMap(Object[] var0, int var1) {
      return var0.length > var1 && var0[var1] instanceof Map var2 ? var2 : null;
   }

   @Override
   public Collection<String> jsKeys() {
      return List.of(
         "url", "headers", "timeout", "revision", "capabilities", "server", "connect", "tools", "resources", "prompts", "invoke", "call", "stop", "help"
      );
   }

   @Override
   public Object jsGet(String var1) {
      return switch (var1) {
         case "url" -> this.url;
         case "headers" -> this.headers;
         case "timeout" -> this.timeoutMs;
         case "revision" -> this.revision;
         case "capabilities" -> this.capabilities;
         case "server" -> this.client == null ? null : this.client.b();
         case "connect" -> (io.karatelabs.js.JavaInvokable) var1x -> this.connect();
         case "tools" -> (io.karatelabs.js.JavaInvokable) var1x -> this.tools();
         case "resources" -> (io.karatelabs.js.JavaInvokable) var1x -> this.resources();
         case "prompts" -> (io.karatelabs.js.JavaInvokable) var1x -> this.prompts();
         case "invoke" -> (io.karatelabs.js.JavaInvokable) var1x -> this.invoke(String.valueOf(var1x[0]), asMap(var1x, 1), asMap(var1x, 2));
         case "call" -> (io.karatelabs.js.JavaInvokable) var1x -> this.call(String.valueOf(var1x[0]), asMap(var1x, 1), asMap(var1x, 2));
         case "stop" -> (io.karatelabs.js.JavaInvokable) var1x -> this.stop();
         case "help" -> (io.karatelabs.js.JavaInvokable) var0 -> Mcp.helpMarkdown();
         default -> null;
      };
   }

   @Override
   public void putMember(String var1, Object var2) {
      switch (var1) {
         case "url":
            this.url = var2 != null ? var2.toString() : null;
            this.client = null;
            break;
         case "headers":
            this.headers = var2 instanceof Map ? (Map)var2 : null;
            if (this.client != null) {
               this.client.a(this.headers);
            }
            break;
         case "timeout":
            this.timeoutMs = var2 instanceof Number var5 ? var5.intValue() : Integer.parseInt(String.valueOf(var2));
            if (this.client != null) {
               this.client.a(this.timeoutMs.intValue());
            }
            break;
         case "revision":
            this.revision = var2 != null ? var2.toString() : null;
            if (this.client != null) {
               this.client.a(this.revision);
            }
            break;
         case "capabilities":
            this.capabilities = var2 instanceof Map ? (Map)var2 : null;
            if (this.client != null) {
               this.client.b(this.capabilities);
            }
            break;
         default:
            logger.warn("put not supported on mcp session: {} - {}", var1, var2);
      }
   }
}
