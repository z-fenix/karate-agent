package io.karatelabs.ext.agent.llm.providers;

import io.karatelabs.common.Json;
import io.karatelabs.ext.agent.llm.LlmMessage;
import io.karatelabs.ext.agent.llm.LlmResponse;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class BaseLlmClient implements io.karatelabs.ext.agent.llm.LlmClient {
   protected final Logger a = LoggerFactory.getLogger(this.getClass());
   protected final String b;
   protected final String c;
   protected final String d;
   protected final int e;
   protected final double f;
   protected final List<String> g;
   protected final io.karatelabs.ext.agent.http.AgentHttpClient h;
   private static final int i = 65536;

   protected BaseLlmClient(String var1, String var2, String var3, int var4, double var5) {
      this(var1, var2, var3, var4, var5, List.of());
   }

   protected BaseLlmClient(String var1, String var2, String var3, int var4, double var5, List<String> var7) {
      this.b = var1;
      this.c = var2;
      this.d = var3;
      this.e = var4;
      this.f = var5;
      this.g = var7 == null ? List.of() : List.copyOf(var7);
      this.h = new io.karatelabs.ext.agent.http.AgentHttpClient();
   }

   @Override
   public String b() {
      return this.c;
   }

   @Override
   public void c() {
      this.h.cancel();
   }

   @Override
   public void close() {
      try {
         this.h.close();
      } catch (Exception var2) {
         this.a.warn("Error closing HTTP client", var2);
      }
   }

   @Override
   public LlmResponse a(List<LlmMessage> var1, String var2, Consumer<String> var3) {
      var var4 = this.b(var1, var2);
      io.karatelabs.ext.agent.http.AgentHttpRequestBuilder var5 = io.karatelabs.ext.agent.http.AgentHttpRequestBuilder.post(this.b).contentType("application/json").body(var4);
      this.a(var5);
      io.karatelabs.ext.agent.http.AgentHttpRequest var6 = var5.build();
      StringBuilder var7 = new StringBuilder();
      io.karatelabs.ext.agent.llm.LlmResponse.Builder var8 = io.karatelabs.ext.agent.llm.LlmResponse.builder().model(this.c);
      io.karatelabs.ext.agent.llm.providers.BaseLlmClient.SseLineBuffer var9 = new io.karatelabs.ext.agent.llm.providers.BaseLlmClient.SseLineBuffer();
      Exception[] var10 = new Exception[1];
      io.karatelabs.ext.agent.llm.providers.BaseLlmClient.StreamParseState var11 = new io.karatelabs.ext.agent.llm.providers.BaseLlmClient.StreamParseState();
      this.h.execute(var6, var6x -> {
         for (String var8x : (Iterable<String>) (Iterable<?>) (var9.a(var6x))) {
            this.a(var8x, var7, var8, var3, var11);
         }
      }, var1x -> var10[0] = var1x);
      this.a(var9.a(), var7, var8, var3, var11);
      if (var10[0] != null) {
         throw this.a(var10[0]);
      } else {
         if (var11.a == null && var7.isEmpty() && !var11.b.isEmpty()) {
            try {
               if (Json.parseStrict(var11.b.toString()) instanceof Map var12 && var12.get("error") != null) {
                  var11.a = this.a(var12.get("error"));
               }
            } catch (Exception var14) {
               this.a.debug("Unframed response body is not JSON: {}", var11.b);
            }
         }

         if (var11.a != null) {
            throw var11.a;
         } else {
            var8.text(var7.toString());
            return var8.build();
         }
      }
   }

   private void a(String var1, StringBuilder var2, io.karatelabs.ext.agent.llm.LlmResponse.Builder var3, Consumer<String> var4, io.karatelabs.ext.agent.llm.providers.BaseLlmClient.StreamParseState var5) {
      if (var1 != null && var5.a == null) {
         if (var1.startsWith("data:")) {
            String var10 = var1.substring(5).trim();
            if (!var10.isEmpty() && !var10.equals("[DONE]")) {
               Map var7;
               try {
                  var7 = (Map)Json.parseStrict(var10);
               } catch (Exception var9) {
                  this.a.debug("Failed to parse SSE event: {}", var10);
                  return;
               }

               if (var7.get("error") != null) {
                  var5.a = this.a(var7.get("error"));
               } else {
                  this.a(var7, var2, var3, var4);
               }
            }
         } else {
            boolean var6 = var1.isEmpty() || var1.startsWith(":") || var1.startsWith("event:") || var1.startsWith("id:") || var1.startsWith("retry:");
            if (!var6 && var5.b.length() < 65536) {
               var5.b.append(var1).append('\n');
            }
         }
      }
   }

   private io.karatelabs.ext.agent.llm.LlmException a(Object var1) {
      Map var2 = var1 instanceof Map var3 ? var3 : Map.of("message", String.valueOf(var1));
      return io.karatelabs.ext.agent.llm.LlmException.a(a(var2.get("code"), var2.get("type")), Json.stringifyStrict(Map.of("error", var2)), this.a());
   }

   static int a(Object var0, Object var1) {
      if (var0 instanceof Number var2 && var2.intValue() >= 400 && var2.intValue() < 600) {
         return var2.intValue();
      } else if (var0 instanceof String var5 && var5.matches("[45]\\d\\d")) {
         return Integer.parseInt(var5);
      } else {
         String var6 = (var0 instanceof String var4 ? var4 : "") + " " + (var1 instanceof String var3 ? var3 : "");
         if (var6.contains("rate_limit")) {
            return 429;
         } else if (var6.contains("overloaded")) {
            return 529;
         } else if (var6.contains("authentication")) {
            return 401;
         } else if (var6.contains("permission")) {
            return 403;
         } else if (var6.contains("not_found")) {
            return 404;
         } else {
            return var6.contains("invalid_request") ? 400 : 502;
         }
      }
   }

   private io.karatelabs.ext.agent.llm.LlmException a(Exception var1) {
      if (var1 instanceof io.karatelabs.ext.agent.llm.LlmException var3) {
         return var3;
      } else {
         return var1 instanceof io.karatelabs.ext.agent.http.AgentHttpException var2
            ? io.karatelabs.ext.agent.llm.LlmException.a(var2.statusCode(), var2.responseBody(), this.a())
            : new io.karatelabs.ext.agent.llm.LlmException(io.karatelabs.ext.agent.llm.LlmException.LlmErrorKind.NETWORK, "LLM streaming request failed: " + var1.getMessage(), 0, var1);
      }
   }

   protected Map<String, Object> a(LlmMessage var1) {
      LinkedHashMap var2 = new LinkedHashMap();
      var2.put("role", var1.role().getValue());
      if (var1.isTextOnly()) {
         var2.put("content", var1.text());
      } else {
         ArrayList var3 = new ArrayList();

         for (io.karatelabs.ext.agent.llm.LlmMessage.Part var5 : var1.parts()) {
            if (var5.isText()) {
               LinkedHashMap var6 = new LinkedHashMap();
               var6.put("type", "text");
               var6.put("text", var5.text());
               var3.add(var6);
            } else if (var5.isImage()) {
               var3.add(this.a(var5));
            }
         }

         var2.put("content", var3);
      }

      return var2;
   }

   protected abstract Map<String, Object> b(List<LlmMessage> var1, String var2);

   protected abstract void a(io.karatelabs.ext.agent.http.AgentHttpRequestBuilder var1);

   protected abstract void a(Map<String, Object> var1, StringBuilder var2, io.karatelabs.ext.agent.llm.LlmResponse.Builder var3, Consumer<String> var4);

   protected abstract Map<String, Object> a(io.karatelabs.ext.agent.llm.LlmMessage.Part var1);

   protected abstract io.karatelabs.ext.agent.llm.LlmResponse.FinishReason a(String var1);

   static final class SseLineBuffer {
      private final StringBuilder a = new StringBuilder();

      List<String> a(String var1) {
         ArrayList var2 = new ArrayList();
         if (var1 != null && !var1.isEmpty()) {
            this.a.append(var1);

            int var3;
            while ((var3 = this.a.indexOf("\n")) >= 0) {
               String var4 = this.a.substring(0, var3);
               if (!var4.isEmpty() && var4.charAt(var4.length() - 1) == '\r') {
                  var4 = var4.substring(0, var4.length() - 1);
               }

               var2.add(var4);
               this.a.delete(0, var3 + 1);
            }

            return var2;
         } else {
            return var2;
         }
      }

      String a() {
         String var1 = this.a.toString();
         this.a.setLength(0);
         if (!var1.isEmpty() && var1.charAt(var1.length() - 1) == '\r') {
            var1 = var1.substring(0, var1.length() - 1);
         }

         return var1;
      }
   }

   private static final class StreamParseState {
      io.karatelabs.ext.agent.llm.LlmException a;
      final StringBuilder b = new StringBuilder();
   }
}
