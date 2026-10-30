package io.karatelabs.ext.agent.llm;

public enum ClaudeModel {
   OPUS("claude-opus-4-8", "anthropic/claude-opus-4.8"),
   SONNET("claude-sonnet-4-6", "anthropic/claude-sonnet-4.6"),
   HAIKU("claude-haiku-4-5-20251001", "anthropic/claude-haiku-4.5");

   private final String anthropicId;
   private final String openRouterId;

   private ClaudeModel(String var3, String var4) {
      this.anthropicId = var3;
      this.openRouterId = var4;
   }

   public String getAnthropicId() {
      return this.anthropicId;
   }

   public String getOpenRouterId() {
      return this.openRouterId;
   }

   public String getModelId(LlmProvider var1) {
      return switch (var1) {
         case ANTHROPIC -> this.anthropicId;
         case OPENROUTER, OLLAMA, OPENAI -> this.openRouterId;
      };
   }

   public static ClaudeModel fromString(String var0) {
      if (var0 == null) {
         return null;
      } else {
         String var1 = var0.toUpperCase().replace("-", "_").replace(".", "_");

         try {
            return valueOf(var1);
         } catch (IllegalArgumentException var5) {
            String var3 = var0.toLowerCase();

            return switch (var3) {
               case "sonnet", "claude-sonnet" -> SONNET;
               case "opus", "claude-opus" -> OPUS;
               case "haiku", "claude-haiku" -> HAIKU;
               default -> null;
            };
         }
      }
   }

   public static String resolve(String var0, LlmProvider var1) {
      ClaudeModel var2 = fromString(var0);
      return var2 != null ? var2.getModelId(var1) : var0;
   }
}
