package io.karatelabs.ext.agent.llm;

import java.net.URI;
import java.util.Locale;
import java.util.function.Function;

public enum LlmProvider {
   ANTHROPIC("ANTHROPIC_API_KEY", "https://api.anthropic.com/v1/messages", "ANTHROPIC_BASE_URL", "/messages"),
   OPENAI("OPENAI_API_KEY", "https://api.openai.com/v1/chat/completions", "OPENAI_BASE_URL", "/chat/completions"),
   OPENROUTER("OPENROUTER_API_KEY", "https://openrouter.ai/api/v1/chat/completions", "OPENROUTER_BASE_URL", "/chat/completions"),
   OLLAMA("OLLAMA_API_KEY", "http://localhost:11434/v1/chat/completions", "OLLAMA_HOST", "/chat/completions");

   private final String envVar;
   private final String defaultEndpoint;
   private final String baseUrlEnvVar;
   private final String pathSuffix;
   public static final String GENERIC_ENDPOINT_ENV = "KARATE_LLM_ENDPOINT";
   private static final String AZURE_HOST_SUFFIX = ".openai.azure.com";
   private static final String AZURE_DEPLOYMENTS = "/deployments/";

   private LlmProvider(String var3, String var4, String var5, String var6) {
      this.envVar = var3;
      this.defaultEndpoint = var4;
      this.baseUrlEnvVar = var5;
      this.pathSuffix = var6;
   }

   public String getEnvVar() {
      return this.envVar;
   }

   public String getDefaultEndpoint() {
      return this.defaultEndpoint;
   }

   public String getBaseUrlEnvVar() {
      return this.baseUrlEnvVar;
   }

   public String getApiKeyFromEnv() {
      return System.getenv(this.envVar);
   }

   public String configuredEndpoint(Function<String, String> var1) {
      String var2 = (String)var1.apply("KARATE_LLM_ENDPOINT");
      if (var2 != null && !var2.isBlank()) {
         return var2.trim();
      } else {
         String var3 = (String)var1.apply(this.baseUrlEnvVar);
         return var3 != null && !var3.isBlank() ? var3.trim() : null;
      }
   }

   public String resolveEndpoint(Function<String, String> var1) {
      String var2 = this.configuredEndpoint(var1);
      return var2 == null ? this.defaultEndpoint : normalizeEndpoint(var2, this.pathSuffix);
   }

   static String normalizeEndpoint(String var0, String var1) {
      if (!var0.contains("/chat/completions") && !var0.contains("/messages")) {
         while (var0.endsWith("/")) {
            var0 = var0.substring(0, var0.length() - 1);
         }

         String var2 = azureRoot(var0);
         if (var2 != null) {
            return var2 + "/openai/v1" + var1;
         } else {
            return var0.endsWith("/v1") ? var0 + var1 : var0 + "/v1" + var1;
         }
      } else {
         return var0;
      }
   }

   private static String azureRoot(String var0) {
      try {
         URI var1 = URI.create(var0);
         String var2 = var1.getHost();
         if (var2 != null && var2.toLowerCase(Locale.ROOT).endsWith(".openai.azure.com")) {
            String var3 = var1.getScheme() + "://" + var2;
            return var1.getPort() > 0 ? var3 + ":" + var1.getPort() : var3;
         } else {
            return null;
         }
      } catch (IllegalArgumentException var4) {
         return null;
      }
   }

   public static String azureDeployment(String var0) {
      if (var0 != null && azureRoot(var0) != null) {
         int var1 = var0.indexOf("/deployments/");
         if (var1 < 0) {
            return null;
         } else {
            String var2 = var0.substring(var1 + "/deployments/".length());
            int var3 = var2.length();

            for (int var4 = 0; var4 < var2.length(); var4++) {
               char var5 = var2.charAt(var4);
               if (var5 == '/' || var5 == '?' || var5 == '#') {
                  var3 = var4;
                  break;
               }
            }

            String var6 = var2.substring(0, var3);
            return var6.isBlank() ? null : var6;
         }
      } else {
         return null;
      }
   }

   public static Object[] parseModel(String var0, LlmProvider var1) {
      if (var0 == null || var0.isBlank()) {
         return new Object[]{var1, var0};
      } else if (var0.startsWith("openrouter/")) {
         return new Object[]{OPENROUTER, var0.substring("openrouter/".length())};
      } else if (var0.startsWith("anthropic/")) {
         return new Object[]{ANTHROPIC, var0.substring("anthropic/".length())};
      } else if (var0.startsWith("openai/")) {
         return new Object[]{OPENAI, var0.substring("openai/".length())};
      } else {
         return var0.startsWith("ollama/") ? new Object[]{OLLAMA, var0.substring("ollama/".length())} : new Object[]{var1, var0};
      }
   }
}
