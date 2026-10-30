package io.karatelabs.ext.agent.llm;

import java.util.List;
import java.util.function.Consumer;

public interface LlmClient extends AutoCloseable {
   LlmResponse a(List<LlmMessage> var1, String var2, Consumer<String> var3);

   default LlmResponse a(List<LlmMessage> var1, String var2) {
      return this.a(var1, var2, null);
   }

   default LlmResponse a(String var1, String var2, Consumer<String> var3) {
      return this.a(List.of(io.karatelabs.ext.agent.llm.LlmMessage.user(var1)), var2, var3);
   }

   default LlmResponse a(String var1, String var2) {
      return this.a(var1, var2, null);
   }

   LlmProvider a();

   String b();

   void c();

   static io.karatelabs.ext.agent.llm.LlmClient.Builder d() {
      return new io.karatelabs.ext.agent.llm.LlmClient.Builder();
   }

   static String a(LlmProvider var0) {
      StringBuilder var1 = new StringBuilder();

      for (LlmProvider var5 : LlmProvider.values()) {
         if (var5 != var0 && var5 != io.karatelabs.ext.agent.llm.LlmProvider.OLLAMA) {
            var1.append(var1.length() == 0 ? "" : ", ").append(var5.getEnvVar());
         }
      }

      return "No API key for LLM provider '"
         + var0.name().toLowerCase()
         + "'. Set "
         + var0.getEnvVar()
         + " — or use a different provider by setting its key ("
         + var1
         + "), or a local Ollama via "
         + io.karatelabs.ext.agent.llm.LlmProvider.OLLAMA.getBaseUrlEnvVar()
         + " (no key needed). You can also pass a key explicitly via apiKey().";
   }

   public static class Builder {
      private LlmProvider provider = io.karatelabs.ext.agent.llm.LlmProvider.ANTHROPIC;
      private String model = "sonnet";
      private String apiKey;
      private String endpoint;
      private int maxOutputTokens = 8192;
      private double temperature = -1.0;
      private List<String> stopSequences = List.of();
      private int extraTokenHeadroom;

      public io.karatelabs.ext.agent.llm.LlmClient.Builder provider(LlmProvider var1) {
         this.provider = var1;
         return this;
      }

      public io.karatelabs.ext.agent.llm.LlmClient.Builder anthropic() {
         return this.provider(io.karatelabs.ext.agent.llm.LlmProvider.ANTHROPIC);
      }

      public io.karatelabs.ext.agent.llm.LlmClient.Builder openRouter() {
         return this.provider(io.karatelabs.ext.agent.llm.LlmProvider.OPENROUTER);
      }

      public io.karatelabs.ext.agent.llm.LlmClient.Builder ollama() {
         return this.provider(io.karatelabs.ext.agent.llm.LlmProvider.OLLAMA);
      }

      public io.karatelabs.ext.agent.llm.LlmClient.Builder model(String var1) {
         this.model = var1;
         return this;
      }

      public io.karatelabs.ext.agent.llm.LlmClient.Builder model(ClaudeModel var1) {
         this.model = var1.name().toLowerCase();
         return this;
      }

      public io.karatelabs.ext.agent.llm.LlmClient.Builder apiKey(String var1) {
         this.apiKey = var1;
         return this;
      }

      public io.karatelabs.ext.agent.llm.LlmClient.Builder apiKeyFromEnv(String var1) {
         this.apiKey = System.getenv(var1);
         return this;
      }

      public io.karatelabs.ext.agent.llm.LlmClient.Builder endpoint(String var1) {
         this.endpoint = var1;
         return this;
      }

      public io.karatelabs.ext.agent.llm.LlmClient.Builder maxOutputTokens(int var1) {
         this.maxOutputTokens = var1;
         return this;
      }

      public io.karatelabs.ext.agent.llm.LlmClient.Builder temperature(double var1) {
         this.temperature = var1;
         return this;
      }

      public io.karatelabs.ext.agent.llm.LlmClient.Builder stopSequences(List<String> var1) {
         this.stopSequences = var1 == null ? List.of() : var1;
         return this;
      }

      public io.karatelabs.ext.agent.llm.LlmClient.Builder extraTokenHeadroom(int var1) {
         this.extraTokenHeadroom = var1;
         return this;
      }

      public LlmClient build() {
         String var1 = this.apiKey;
         if (var1 == null) {
            var1 = this.provider.getApiKeyFromEnv();
         }

         if (var1 == null) {
            if (this.provider != io.karatelabs.ext.agent.llm.LlmProvider.OLLAMA) {
               throw new LlmException(io.karatelabs.ext.agent.llm.LlmException.LlmErrorKind.AUTHENTICATION, io.karatelabs.ext.agent.llm.LlmClient.a(this.provider), 0, null, null, this.provider);
            }

            var1 = "ollama";
         }

         String var2 = this.endpoint;
         if (var2 == null) {
            var2 = this.provider.getDefaultEndpoint();
         }

         String var3 = io.karatelabs.ext.agent.llm.ClaudeModel.resolve(this.model, this.provider);

         return (LlmClient)(switch (this.provider) {
            case ANTHROPIC -> new io.karatelabs.ext.agent.llm.providers.AnthropicLlmClient(var2, var3, var1, this.maxOutputTokens, this.temperature, this.stopSequences);
            case OPENAI, OPENROUTER, OLLAMA -> new io.karatelabs.ext.agent.llm.providers.OpenAiCompatibleLlmClient(var2, var3, var1, this.maxOutputTokens, this.temperature, this.stopSequences, this.provider, this.extraTokenHeadroom);
         });
      }
   }
}
