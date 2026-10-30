package io.karatelabs.ext.agent.llm;

public class LlmResponse {
   private final String text;
   private final String reasoning;
   private final io.karatelabs.ext.agent.llm.LlmResponse.FinishReason finishReason;
   private final String rawFinishReason;
   private final String refusal;
   private final int inputTokens;
   private final int outputTokens;
   private final int cacheReadTokens;
   private final int cacheCreationTokens;
   private final String model;

   private LlmResponse(io.karatelabs.ext.agent.llm.LlmResponse.Builder var1) {
      this.text = var1.text;
      this.reasoning = var1.reasoning == null ? null : var1.reasoning.toString();
      this.finishReason = var1.finishReason;
      this.rawFinishReason = var1.rawFinishReason;
      this.refusal = var1.refusal;
      this.inputTokens = var1.inputTokens;
      this.outputTokens = var1.outputTokens;
      this.cacheReadTokens = var1.cacheReadTokens;
      this.cacheCreationTokens = var1.cacheCreationTokens;
      this.model = var1.model;
   }

   public String text() {
      return this.text;
   }

   public String reasoning() {
      return this.reasoning;
   }

   public io.karatelabs.ext.agent.llm.LlmResponse.FinishReason finishReason() {
      return this.finishReason;
   }

   public String rawFinishReason() {
      return this.rawFinishReason;
   }

   public String refusal() {
      return this.refusal;
   }

   public int inputTokens() {
      return this.inputTokens;
   }

   public int outputTokens() {
      return this.outputTokens;
   }

   public int totalTokens() {
      return this.inputTokens + this.outputTokens;
   }

   public int cacheReadTokens() {
      return this.cacheReadTokens;
   }

   public int cacheCreationTokens() {
      return this.cacheCreationTokens;
   }

   public String model() {
      return this.model;
   }

   public static io.karatelabs.ext.agent.llm.LlmResponse.Builder builder() {
      return new io.karatelabs.ext.agent.llm.LlmResponse.Builder();
   }

   public static class Builder {
      private String text;
      private StringBuilder reasoning;
      private io.karatelabs.ext.agent.llm.LlmResponse.FinishReason finishReason = io.karatelabs.ext.agent.llm.LlmResponse.FinishReason.UNKNOWN;
      private String rawFinishReason;
      private String refusal;
      private int inputTokens;
      private int outputTokens;
      private int cacheReadTokens;
      private int cacheCreationTokens;
      private String model;

      public io.karatelabs.ext.agent.llm.LlmResponse.Builder text(String var1) {
         this.text = var1;
         return this;
      }

      public io.karatelabs.ext.agent.llm.LlmResponse.Builder appendReasoning(String var1) {
         if (this.reasoning == null) {
            this.reasoning = new StringBuilder();
         }

         this.reasoning.append(var1);
         return this;
      }

      public io.karatelabs.ext.agent.llm.LlmResponse.Builder finishReason(io.karatelabs.ext.agent.llm.LlmResponse.FinishReason var1) {
         this.finishReason = var1;
         return this;
      }

      public io.karatelabs.ext.agent.llm.LlmResponse.Builder rawFinishReason(String var1) {
         this.rawFinishReason = var1;
         return this;
      }

      public io.karatelabs.ext.agent.llm.LlmResponse.Builder refusal(String var1) {
         this.refusal = var1;
         return this;
      }

      public io.karatelabs.ext.agent.llm.LlmResponse.Builder appendRefusal(String var1) {
         this.refusal = (this.refusal == null ? "" : this.refusal) + var1;
         return this;
      }

      public io.karatelabs.ext.agent.llm.LlmResponse.Builder inputTokens(int var1) {
         this.inputTokens = var1;
         return this;
      }

      public io.karatelabs.ext.agent.llm.LlmResponse.Builder outputTokens(int var1) {
         this.outputTokens = var1;
         return this;
      }

      public io.karatelabs.ext.agent.llm.LlmResponse.Builder cacheReadTokens(int var1) {
         this.cacheReadTokens = var1;
         return this;
      }

      public io.karatelabs.ext.agent.llm.LlmResponse.Builder cacheCreationTokens(int var1) {
         this.cacheCreationTokens = var1;
         return this;
      }

      public io.karatelabs.ext.agent.llm.LlmResponse.Builder model(String var1) {
         this.model = var1;
         return this;
      }

      public LlmResponse build() {
         return new LlmResponse(this);
      }
   }

   public static enum FinishReason {
      END_TURN,
      MAX_TOKENS,
      STOP_SEQUENCE,
      TOOL_USE,
      REFUSAL,
      UNKNOWN;
   }
}
