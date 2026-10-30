package io.karatelabs.ext.agent.llm.providers;

import io.karatelabs.ext.agent.llm.LlmMessage;
import io.karatelabs.ext.agent.llm.LlmResponse;
import io.karatelabs.ext.agent.llm.LlmProvider;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class AnthropicLlmClient extends io.karatelabs.ext.agent.llm.providers.BaseLlmClient {
   private static final String i = "2023-06-01";

   public AnthropicLlmClient(String var1, String var2, String var3, int var4, double var5, List<String> var7) {
      super(var1, var2, var3, var4, var5, var7);
   }

   @Override
   public LlmProvider a() {
      return io.karatelabs.ext.agent.llm.LlmProvider.ANTHROPIC;
   }

   @Override
   protected Map<String, Object> b(List<LlmMessage> var1, String var2) {
      LinkedHashMap<java.lang.String, java.lang.Object> var3 = new LinkedHashMap();
      var3.put("model", this.c);
      var3.put("max_tokens", this.e);
      var3.put("stream", true);
      if (this.f >= 0.0) {
         var3.put("temperature", this.f);
      }

      if (!this.g.isEmpty()) {
         var3.put("stop_sequences", this.g);
      }

      if (var2 != null && !var2.isEmpty()) {
         LinkedHashMap<java.lang.String, java.lang.Object> var4 = new LinkedHashMap();
         var4.put("type", "text");
         var4.put("text", var2);
         var4.put("cache_control", Map.of("type", "ephemeral"));
         var3.put("system", List.of(var4));
      }

      ArrayList<java.util.Map<java.lang.String,java.lang.Object>> var7 = new ArrayList();

      for (LlmMessage var6 : var1) {
         if (var6.role() != io.karatelabs.ext.agent.llm.LlmMessage.Role.SYSTEM) {
            var7.add(this.a(var6));
         }
      }

      var3.put("messages", var7);
      return var3;
   }

   @Override
   protected void a(io.karatelabs.ext.agent.http.AgentHttpRequestBuilder var1) {
      var1.header("x-api-key", this.d).header("anthropic-version", "2023-06-01");
   }

   @Override
   protected void a(Map<String, Object> var1, StringBuilder var2, io.karatelabs.ext.agent.llm.LlmResponse.Builder var3, Consumer<String> var4) {
      String var5 = (String)var1.get("type");
      if (var5 != null) {
         switch (var5) {
            case "content_block_delta":
               Map<java.lang.String, java.lang.String> var14 = (Map)var1.get("delta");
               if (var14 != null) {
                  String var17 = (String)var14.get("type");
                  if ("text_delta".equals(var17)) {
                     String var19 = (String)var14.get("text");
                     if (var19 != null) {
                        var2.append(var19);
                        if (var4 != null) {
                           var4.accept(var19);
                        }
                     }
                  }
               }
               break;
            case "message_delta":
               Map<java.lang.String, java.lang.String> var13 = (Map)var1.get("delta");
               if (var13 != null) {
                  String var15 = (String)var13.get("stop_reason");
                  if (var15 != null) {
                     var3.rawFinishReason(var15).finishReason(this.a(var15));
                  }
               }

               Map<java.lang.String, java.lang.Number> var16 = (Map)var1.get("usage");
               if (var16 != null) {
                  Number var18 = (Number)var16.get("output_tokens");
                  if (var18 != null) {
                     var3.outputTokens(var18.intValue());
                  }
               }
               break;
            case "message_start":
               Map<java.lang.String, java.util.Map> var8 = (Map)var1.get("message");
               if (var8 != null) {
                  Map<java.lang.String, java.lang.Number> var9 = (Map)var8.get("usage");
                  if (var9 != null) {
                     Number var10 = (Number)var9.get("input_tokens");
                     if (var10 != null) {
                        var3.inputTokens(var10.intValue());
                     }

                     Number var11 = (Number)var9.get("cache_read_input_tokens");
                     if (var11 != null) {
                        var3.cacheReadTokens(var11.intValue());
                     }

                     Number var12 = (Number)var9.get("cache_creation_input_tokens");
                     if (var12 != null) {
                        var3.cacheCreationTokens(var12.intValue());
                     }
                  }
               }
         }
      }
   }

   @Override
   protected Map<String, Object> a(io.karatelabs.ext.agent.llm.LlmMessage.Part var1) {
      LinkedHashMap<java.lang.String, java.lang.Object> var2 = new LinkedHashMap();
      var2.put("type", "image");
      LinkedHashMap<java.lang.String, java.lang.String> var3 = new LinkedHashMap();
      var3.put("type", "base64");
      var3.put("media_type", var1.mediaType());
      var3.put("data", var1.base64());
      var2.put("source", var3);
      return var2;
   }

   @Override
   protected io.karatelabs.ext.agent.llm.LlmResponse.FinishReason a(String var1) {
      if (var1 == null) {
         return io.karatelabs.ext.agent.llm.LlmResponse.FinishReason.UNKNOWN;
      } else {
         return switch (var1) {
            case "end_turn" -> io.karatelabs.ext.agent.llm.LlmResponse.FinishReason.END_TURN;
            case "max_tokens" -> io.karatelabs.ext.agent.llm.LlmResponse.FinishReason.MAX_TOKENS;
            case "stop_sequence" -> io.karatelabs.ext.agent.llm.LlmResponse.FinishReason.STOP_SEQUENCE;
            case "tool_use" -> io.karatelabs.ext.agent.llm.LlmResponse.FinishReason.TOOL_USE;
            case "refusal" -> io.karatelabs.ext.agent.llm.LlmResponse.FinishReason.REFUSAL;
            default -> io.karatelabs.ext.agent.llm.LlmResponse.FinishReason.UNKNOWN;
         };
      }
   }
}
