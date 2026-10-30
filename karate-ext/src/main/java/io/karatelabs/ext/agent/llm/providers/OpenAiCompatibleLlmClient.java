package io.karatelabs.ext.agent.llm.providers;

import io.karatelabs.ext.agent.llm.LlmMessage;
import io.karatelabs.ext.agent.llm.LlmResponse;
import io.karatelabs.ext.agent.llm.LlmProvider;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class OpenAiCompatibleLlmClient extends io.karatelabs.ext.agent.llm.providers.BaseLlmClient {
   private final LlmProvider i;
   private final int j;
   private volatile boolean k;
   private volatile boolean l = true;

   public OpenAiCompatibleLlmClient(String var1, String var2, String var3, int var4, double var5, List<String> var7, LlmProvider var8) {
      this(var1, var2, var3, var4, var5, var7, var8, 0);
   }

   public OpenAiCompatibleLlmClient(String var1, String var2, String var3, int var4, double var5, List<String> var7, LlmProvider var8, int var9) {
      super(var1, var2, var3, var4, var5, var7);
      this.i = var8 == null ? io.karatelabs.ext.agent.llm.LlmProvider.OPENROUTER : var8;
      this.j = var9;
      this.k = var2 != null && var2.startsWith("anthropic/");
   }

   @Override
   public LlmProvider a() {
      return this.i;
   }

   @Override
   public LlmResponse a(List<LlmMessage> var1, String var2, Consumer<String> var3) {
      boolean var4 = this.k && this.l && this.j > 0;

      try {
         return super.a(var1, var2, var3);
      } catch (io.karatelabs.ext.agent.llm.LlmException var6) {
         if (var4 && var6.a() == io.karatelabs.ext.agent.llm.LlmException.LlmErrorKind.INVALID_REQUEST && b(var6.getMessage())) {
            this.a.warn("{} refused max_tokens {}; retrying at the reply ceiling {}", new Object[]{this.c, this.e + this.j, this.e});
            this.l = false;
            return super.a(var1, var2, var3);
         } else {
            throw var6;
         }
      }
   }

   private static boolean b(String var0) {
      String var1 = var0 == null ? "" : var0.toLowerCase();
      return var1.contains("max_tokens") || var1.contains("max_completion_tokens") || var1.contains("output tokens") || var1.contains("completion tokens");
   }

   @Override
   protected Map<String, Object> b(List<LlmMessage> var1, String var2) {
      LinkedHashMap<java.lang.String, java.lang.Object> var3 = new LinkedHashMap();
      var3.put("model", this.c);
      var3.put("max_tokens", this.k && this.l ? this.e + this.j : this.e);
      var3.put("stream", true);
      var3.put("stream_options", Map.of("include_usage", true));
      if (this.f >= 0.0) {
         var3.put("temperature", this.f);
      }

      if (!this.g.isEmpty()) {
         var3.put("stop", this.g);
      }

      ArrayList<java.util.Map<java.lang.String,java.lang.Object>> var4 = new ArrayList();
      if (var2 != null && !var2.isEmpty()) {
         LinkedHashMap<java.lang.String, java.lang.Object> var5 = new LinkedHashMap();
         var5.put("role", "system");
         LinkedHashMap<java.lang.String, java.lang.Object> var6 = new LinkedHashMap();
         var6.put("type", "text");
         var6.put("text", var2);
         var6.put("cache_control", Map.of("type", "ephemeral"));
         var5.put("content", List.of(var6));
         var4.add(var5);
      }

      for (LlmMessage var8 : var1) {
         if (var8.role() != io.karatelabs.ext.agent.llm.LlmMessage.Role.SYSTEM) {
            var4.add(this.a(var8));
         }
      }

      var3.put("messages", var4);
      return var3;
   }

   @Override
   protected void a(io.karatelabs.ext.agent.http.AgentHttpRequestBuilder var1) {
      var1.bearerToken(this.d).header("HTTP-Referer", "https://karatelabs.io").header("X-Title", "Karate Max Agent");
   }

   @Override
   protected void a(Map<String, Object> var1, StringBuilder var2, io.karatelabs.ext.agent.llm.LlmResponse.Builder var3, Consumer<String> var4) {
      Map<java.lang.String, java.lang.Object> var5 = (Map)var1.get("usage");
      if (var5 != null) {
         Number var6 = (Number)var5.get("completion_tokens");
         if (var6 != null) {
            var3.outputTokens(var6.intValue());
         }

         int var7 = 0;
         Number var8 = (Number)var5.get("prompt_tokens");
         if (var8 != null) {
            var7 = var8.intValue();
         }

         int var9 = 0;
         Map<java.lang.String, java.lang.Number> var10 = (Map)var5.get("prompt_tokens_details");
         if (var10 != null) {
            Number var11 = (Number)var10.get("cached_tokens");
            if (var11 != null) {
               var9 = var11.intValue();
            }
         }

         if (var8 != null) {
            var3.inputTokens(Math.max(0, var7 - var9));
         }

         if (var9 > 0) {
            var3.cacheReadTokens(var9);
         }
      }

      List var12 = (List)var1.get("choices");
      if (var12 != null && !var12.isEmpty()) {
         Map<java.lang.String, java.lang.Object> var13 = (Map)var12.get(0);
         Map<java.lang.String, java.lang.String> var14 = (Map)var13.get("delta");
         if (var14 != null) {
            String var15 = (String)var14.get("content");
            if (var15 != null && !var15.isEmpty()) {
               var2.append(var15);
               if (var4 != null) {
                  var4.accept(var15);
               }
            }

            String var17 = (String)var14.get("refusal");
            if (var17 != null && !var17.isEmpty()) {
               var3.appendRefusal(var17);
            }

            String var18 = (String)var14.get("reasoning");
            if (var18 != null && !var18.isEmpty()) {
               this.k = true;
               var3.appendReasoning(var18);
               if (var4 != null) {
                  var4.accept(var18);
               }
            }
         }

         String var16 = (String)var13.get("finish_reason");
         if (var16 != null) {
            var3.rawFinishReason(var16).finishReason(this.a(var16));
         }
      }
   }

   @Override
   protected Map<String, Object> a(io.karatelabs.ext.agent.llm.LlmMessage.Part var1) {
      LinkedHashMap<java.lang.String, java.lang.Object> var2 = new LinkedHashMap();
      var2.put("type", "image_url");
      LinkedHashMap<java.lang.String, java.lang.String> var3 = new LinkedHashMap();
      var3.put("url", "data:" + var1.mediaType() + ";base64," + var1.base64());
      var2.put("image_url", var3);
      return var2;
   }

   @Override
   protected io.karatelabs.ext.agent.llm.LlmResponse.FinishReason a(String var1) {
      if (var1 == null) {
         return io.karatelabs.ext.agent.llm.LlmResponse.FinishReason.UNKNOWN;
      } else {
         return switch (var1) {
            case "stop" -> io.karatelabs.ext.agent.llm.LlmResponse.FinishReason.END_TURN;
            case "length" -> io.karatelabs.ext.agent.llm.LlmResponse.FinishReason.MAX_TOKENS;
            case "tool_calls", "function_call" -> io.karatelabs.ext.agent.llm.LlmResponse.FinishReason.TOOL_USE;
            case "content_filter" -> io.karatelabs.ext.agent.llm.LlmResponse.FinishReason.REFUSAL;
            default -> io.karatelabs.ext.agent.llm.LlmResponse.FinishReason.UNKNOWN;
         };
      }
   }
}
