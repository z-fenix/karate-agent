package io.karatelabs.ext.common.mcp;

import io.karatelabs.common.Json;
import io.karatelabs.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class McpRequestHandler {
   private static final Logger d = LoggerFactory.getLogger(McpRequestHandler.class);
   private static final String e = "2026-07-28";
   private static final String f = "2025-11-25";
   private static final List<String> g = List.of("2025-11-25", "2025-06-18", "2025-03-26");
   private static final String h = "io.modelcontextprotocol/";
   private static final String i = "karate";
   private static final String j = "1.0.0";
   static final String a = "The karate MCP server. Run JavaScript with the karate_eval tool — the same surface as POST /api/eval ('one API, many faces'). Call help() for the menu of namespaces (e.g. Coverage.*, Skill.*). Returns JSON with command (result/error) and payload.";
   static final String b = "Run JavaScript in the karate run engine. The single, stable entry point — capabilities surface as JS namespaces (call help() for the menu), not new tools. Returns JSON with command (result/error) and payload.";
   static final String c = "JavaScript to run. Call help() for the namespace menu, then Noun.help() for one namespace; e.g. Coverage.summary()";
   private final Function<String, String> k;
   private final String l;
   private final String m;
   private final String n;
   private Function<String, byte[]> o;

   public McpRequestHandler(Function<String, String> var1) {
      this(
         var1,
         "The karate MCP server. Run JavaScript with the karate_eval tool — the same surface as POST /api/eval ('one API, many faces'). Call help() for the menu of namespaces (e.g. Coverage.*, Skill.*). Returns JSON with command (result/error) and payload.",
         "Run JavaScript in the karate run engine. The single, stable entry point — capabilities surface as JS namespaces (call help() for the menu), not new tools. Returns JSON with command (result/error) and payload.",
         "JavaScript to run. Call help() for the namespace menu, then Noun.help() for one namespace; e.g. Coverage.summary()"
      );
   }

   public McpRequestHandler(Function<String, String> var1, String var2) {
      this(
         var1,
         var2,
         "Run JavaScript in the karate run engine. The single, stable entry point — capabilities surface as JS namespaces (call help() for the menu), not new tools. Returns JSON with command (result/error) and payload.",
         "JavaScript to run. Call help() for the namespace menu, then Noun.help() for one namespace; e.g. Coverage.summary()"
      );
   }

   public McpRequestHandler(Function<String, String> var1, String var2, String var3, String var4) {
      this.k = var1;
      this.l = var2;
      this.m = var3;
      this.n = var4;
   }

   public McpRequestHandler a(Function<String, byte[]> var1) {
      this.o = var1;
      return this;
   }

   public HttpResponse a(String var1) {
      return this.a(var1, var0 -> null);
   }

   public HttpResponse a(String var1, Function<String, String> var2) {
      Object var3;
      try {
         var3 = Json.parseLenient(var1);
      } catch (Exception var10) {
         return this.a(400, this.a(null, -32700, "Parse error: " + var10.getMessage()));
      }

      if (!(var3 instanceof Map var4)) {
         return this.a(400, this.a(null, -32600, "Invalid Request: not a JSON-RPC object"));
      } else {
         try {
            Object var5 = var4.get("id");
            if (var4.get("method") instanceof String var6) {
               d.debug("MCP request: method={}, id={}", var6, var5);
               String var11 = this.c((String)var2.apply("MCP-Protocol-Version"));
               if (this.a(var4, "protocolVersion") || "2026-07-28".equals(var11)) {
                  return this.a(var6, var4, var5, var11, var2);
               } else if (var11 != null && !g.contains(var11)) {
                  return this.a(400, this.a(var5, -32022, "Unsupported protocol version", this.b(var11)));
               } else if (var5 == null) {
                  return this.d(var6);
               } else {
                  String var8 = this.a(var6, var4, var5);
                  return this.a(200, var8);
               }
            } else {
               return this.a(400, this.a(var5, -32600, "Invalid Request: missing method"));
            }
         } catch (Exception var9) {
            d.error("MCP request failed", var9);
            return this.a(200, this.a(null, -32603, "Internal error: " + var9.getMessage()));
         }
      }
   }

   private HttpResponse a(String var1, Map<String, Object> var2, Object var3, String var4, Function<String, String> var5) {
      if (var4 == null) {
         return this.a(400, this.a(var3, -32020, "MCP-Protocol-Version header missing"));
      } else {
         String var6 = this.b(var2, "protocolVersion");
         if (var6 == null || !(this.c(var2, "clientCapabilities") instanceof Map)) {
            return this.a(
               400,
               this.a(
                  var3,
                  -32602,
                  "Invalid params: _meta must carry io.modelcontextprotocol/protocolVersion (string) and io.modelcontextprotocol/clientCapabilities (object)"
               )
            );
         } else if (!var6.equals(var4)) {
            return this.a(400, this.a(var3, -32020, "MCP-Protocol-Version header not matching the _meta protocol version"));
         } else if (!"2026-07-28".equals(var6)) {
            return this.a(400, this.a(var3, -32022, "Unsupported protocol version", this.b(var6)));
         } else {
            String var7 = this.c((String)var5.apply("Mcp-Method"));
            if (!var1.equals(var7)) {
               return this.a(400, this.a(var3, -32020, "Mcp-Method header missing or not matching the request method"));
            } else {
               switch (var1) {
                  case "server/discover":
                     LinkedHashMap var13 = new LinkedHashMap();
                     var13.put("supportedVersions", this.a());
                     var13.put("capabilities", Map.of("tools", Map.of()));
                     var13.put("instructions", this.l);
                     var13.put("ttlMs", 300000);
                     var13.put("cacheScope", "public");
                     return this.a(200, this.a(var3, var13));
                  case "tools/list":
                     var var12 = this.b();
                     var12.put("ttlMs", 300000);
                     var12.put("cacheScope", "public");
                     return this.a(200, this.a(var3, var12));
                  case "tools/call":
                     Map var10 = (Map)var2.get("params");
                     if (var10 == null) {
                        return this.a(200, this.a(var3, -32602, "Invalid params: missing params"));
                     } else {
                        String var11 = this.c((String)var5.apply("Mcp-Name"));
                        if (var11 != null && var11.equals(var10.get("name"))) {
                           return this.a(200, this.a(var3, this.a(var10)));
                        }

                        return this.a(400, this.a(var3, -32020, "Mcp-Name header missing or not matching params.name"));
                     }
                  default:
                     return this.a(404, this.a(var3, -32601, "Method not found: " + var1));
               }
            }
         }
      }
   }

   private List<String> a() {
      ArrayList var1 = new ArrayList();
      var1.add("2026-07-28");
      var1.addAll(g);
      return var1;
   }

   private Map<String, Object> b(String var1) {
      LinkedHashMap var2 = new LinkedHashMap();
      var2.put("requested", var1);
      var2.put("supported", this.a());
      return var2;
   }

   private String c(String var1) {
      if (var1 != null && var1.startsWith("=?base64?") && var1.endsWith("?=")) {
         try {
            String var2 = var1.substring(9, var1.length() - 2);
            return new String(Base64.getDecoder().decode(var2), StandardCharsets.UTF_8);
         } catch (IllegalArgumentException var3) {
            return var1;
         }
      } else {
         return var1;
      }
   }

   private boolean a(Map<String, Object> var1, String var2) {
      return var1.get("params") instanceof Map var4 && var4.get("_meta") instanceof Map var3 && var3.containsKey("io.modelcontextprotocol/" + var2);
   }

   private String b(Map<String, Object> var1, String var2) {
      return this.c(var1, var2) instanceof String var3 ? var3 : null;
   }

   private Object c(Map<String, Object> var1, String var2) {
      return var1.get("params") instanceof Map var3 && var3.get("_meta") instanceof Map var4 ? var4.get("io.modelcontextprotocol/" + var2) : null;
   }

   private String a(Object var1, Map<String, Object> var2) {
      var2.put("resultType", "complete");
      var2.put("_meta", Map.of("io.modelcontextprotocol/serverInfo", Map.of("name", "karate", "version", "1.0.0")));
      return this.b(var1, var2);
   }

   private HttpResponse d(String var1) {
      switch (var1) {
         case "notifications/initialized":
            d.debug("MCP client initialized");
            break;
         case "notifications/cancelled":
            d.debug("MCP client cancelled request");
            break;
         default:
            d.debug("MCP unknown notification: {}", var1);
      }

      HttpResponse var2 = new HttpResponse();
      var2.setStatus(202);
      return var2;
   }

   private String a(String var1, Map<String, Object> var2, Object var3) {
      return switch (var1) {
         case "initialize" -> this.a(var2, var3);
         case "ping" -> this.b(var3, new LinkedHashMap<>());
         case "tools/list" -> this.a(var3);
         case "tools/call" -> this.b(var2, var3);
         default -> this.a(var3, -32601, "Method not found: " + var1);
      };
   }

   private String a(Map<String, Object> var1, Object var2) {
      String var3 = var1.get("params") instanceof Map var5 && var5.get("protocolVersion") instanceof String var4 ? var4 : null;
      LinkedHashMap var7 = new LinkedHashMap();
      var7.put("protocolVersion", var3 != null && g.contains(var3) ? var3 : "2025-11-25");
      var7.put("capabilities", Map.of("tools", Map.of()));
      var7.put("serverInfo", Map.of("name", "karate", "version", "1.0.0"));
      var7.put("instructions", this.l);
      return this.b(var2, var7);
   }

   private String a(Object var1) {
      return this.b(var1, this.b());
   }

   private Map<String, Object> b() {
      LinkedHashMap var1 = new LinkedHashMap();
      var1.put("type", "string");
      var1.put("description", this.n);
      LinkedHashMap var2 = new LinkedHashMap();
      var2.put("type", "object");
      var2.put("properties", Map.of("code", var1));
      var2.put("required", List.of("code"));
      LinkedHashMap var3 = new LinkedHashMap();
      var3.put("name", "karate_eval");
      var3.put("description", this.m);
      var3.put("inputSchema", var2);
      LinkedHashMap var4 = new LinkedHashMap();
      var4.put("tools", List.of(var3));
      return var4;
   }

   private String b(Map<String, Object> var1, Object var2) {
      Map var3 = (Map)var1.get("params");
      return var3 == null ? this.a(var2, -32602, "Invalid params: missing params") : this.b(var2, this.a(var3));
   }

   private Map<String, Object> a(Map<String, Object> var1) {
      String var2 = (String)var1.get("name");
      if (!"karate_eval".equals(var2)) {
         return this.a("Unknown tool: " + var2, true, List.of());
      } else {
         Map var3 = (Map)var1.get("arguments");
         if (var3 != null && var3.containsKey("code")) {
            String var4 = String.valueOf(var3.get("code"));
            d.debug("MCP karate_eval: {}", var4.length() > 100 ? var4.substring(0, 100) + "..." : var4);

            try {
               String var5 = this.k.apply(var4);
               boolean var6 = var5 != null && var5.contains("\"command\":\"error\"");
               return this.a(var5, var6, this.e(var5));
            } catch (Exception var7) {
               d.error("karate_eval failed", var7);
               return this.a("Evaluation failed: " + var7.getMessage(), true, List.of());
            }
         } else {
            return this.a("Missing required argument: code", true, List.of());
         }
      }
   }

   private List<Map<String, Object>> e(String var1) {
      ArrayList var2 = new ArrayList();
      if (var1 != null && var1.contains("\"parts\"")) {
         try {
            if (Json.parseLenient(var1) instanceof Map var4 && var4.get("parts") instanceof List var5) {
               Iterator var16 = var5.iterator();

               while (var16.hasNext()) {
                  if (var16.next() instanceof Map var8) {
                     String var9 = String.valueOf(var8.get("type"));
                     switch (var9) {
                        case "text":
                           LinkedHashMap var18 = new LinkedHashMap();
                           var18.put("type", "text");
                           var18.put("text", String.valueOf(var8.get("text")));
                           var2.add(var18);
                           break;
                        case "image":
                           String var17 = var8.get("mimeType") != null ? String.valueOf(var8.get("mimeType")) : "image/png";
                           String var19 = this.b(var8);
                           if (var19 != null) {
                              LinkedHashMap var14 = new LinkedHashMap();
                              var14.put("type", "image");
                              var14.put("data", var19);
                              var14.put("mimeType", var17);
                              var2.add(var14);
                           }
                           break;
                        case "resource":
                           LinkedHashMap var12 = new LinkedHashMap();
                           var12.put("uri", String.valueOf(var8.get("uri")));
                           if (var8.get("mimeType") != null) {
                              var12.put("mimeType", String.valueOf(var8.get("mimeType")));
                           }

                           LinkedHashMap var13 = new LinkedHashMap();
                           var13.put("type", "resource");
                           var13.put("resource", var12);
                           var2.add(var13);
                           break;
                        default:
                           d.debug("MCP: skipping unknown part type {}", var9);
                     }
                  }
               }

               return var2;
            } else {
               return var2;
            }
         } catch (Exception var15) {
            d.debug("MCP parts mapping failed (returning text only): {}", var15.getMessage());
            return var2;
         }
      } else {
         return var2;
      }
   }

   private String b(Map<?, ?> var1) {
      if (var1.get("data") != null) {
         return String.valueOf(var1.get("data"));
      } else {
         if (var1.get("url") != null && this.o != null) {
            try {
               byte[] var2 = this.o.apply(String.valueOf(var1.get("url")));
               if (var2 != null && var2.length > 0) {
                  return Base64.getEncoder().encodeToString(var2);
               }
            } catch (Exception var3) {
               d.debug("MCP image url resolve failed: {}", var3.getMessage());
            }
         }

         return null;
      }
   }

   private String b(Object var1, Map<String, Object> var2) {
      LinkedHashMap var3 = new LinkedHashMap();
      var3.put("jsonrpc", "2.0");
      var3.put("id", var1);
      var3.put("result", var2);
      return Json.stringifyStrict(var3);
   }

   private String a(Object var1, int var2, String var3) {
      return this.a(var1, var2, var3, null);
   }

   private String a(Object var1, int var2, String var3, Map<String, Object> var4) {
      LinkedHashMap var5 = new LinkedHashMap();
      var5.put("code", var2);
      var5.put("message", var3);
      if (var4 != null) {
         var5.put("data", var4);
      }

      LinkedHashMap var6 = new LinkedHashMap();
      var6.put("jsonrpc", "2.0");
      var6.put("id", var1);
      var6.put("error", var5);
      return Json.stringifyStrict(var6);
   }

   private Map<String, Object> a(String var1, boolean var2, List<Map<String, Object>> var3) {
      LinkedHashMap var4 = new LinkedHashMap();
      var4.put("type", "text");
      var4.put("text", var1);
      ArrayList var5 = new ArrayList();
      var5.add(var4);
      if (var3 != null) {
         var5.addAll(var3);
      }

      LinkedHashMap var6 = new LinkedHashMap();
      var6.put("content", var5);
      if (var2) {
         var6.put("isError", true);
      }

      return var6;
   }

   private HttpResponse a(int var1, String var2) {
      return HttpResponse.json(var1, var2);
   }
}
