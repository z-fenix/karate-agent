package io.karatelabs.ext.agent.runtime;

import java.util.Optional;

public final class BudgetExitCondition implements ExitCondition {
   private final long totalTokens;
   private final long inputTokens;
   private final long outputTokens;
   private final double maxCost;
   private final double inputPricePerMillion;
   private final double outputPricePerMillion;

   private BudgetExitCondition(io.karatelabs.ext.agent.runtime.BudgetExitCondition.Builder var1) {
      this.totalTokens = var1.totalTokens;
      this.inputTokens = var1.inputTokens;
      this.outputTokens = var1.outputTokens;
      this.maxCost = var1.maxCost;
      this.inputPricePerMillion = var1.inputPricePerMillion;
      this.outputPricePerMillion = var1.outputPricePerMillion;
   }

   @Override
   public Optional<AgentOutcome> check(AgentRunState var1) {
      long var2 = var1.inputTokens();
      long var4 = var1.outputTokens();
      long var6 = var2 + var4;
      if (this.totalTokens > 0L && var6 >= this.totalTokens) {
         return budgetExceeded("total tokens", var6, this.totalTokens);
      } else if (this.inputTokens > 0L && var2 >= this.inputTokens) {
         return budgetExceeded("input tokens", var2, this.inputTokens);
      } else if (this.outputTokens > 0L && var4 >= this.outputTokens) {
         return budgetExceeded("output tokens", var4, this.outputTokens);
      } else {
         if (this.maxCost > 0.0 && this.hasPricing()) {
            double var8 = this.estimateCost(var2, var4);
            if (var8 >= this.maxCost) {
               return Optional.of(io.karatelabs.ext.agent.runtime.AgentOutcome.terminated(String.format("budget exceeded: cost $%.4f >= $%.4f", var8, this.maxCost)));
            }
         }

         return Optional.empty();
      }
   }

   public double estimateCost(long var1, long var3) {
      return !this.hasPricing() ? 0.0 : var1 / 1000000.0 * this.inputPricePerMillion + var3 / 1000000.0 * this.outputPricePerMillion;
   }

   private boolean hasPricing() {
      return this.inputPricePerMillion > 0.0 || this.outputPricePerMillion > 0.0;
   }

   private static Optional<AgentOutcome> budgetExceeded(String var0, long var1, long var3) {
      return Optional.of(io.karatelabs.ext.agent.runtime.AgentOutcome.terminated("budget exceeded: " + var0 + " " + var1 + " >= " + var3));
   }

   public static io.karatelabs.ext.agent.runtime.BudgetExitCondition.Builder builder() {
      return new io.karatelabs.ext.agent.runtime.BudgetExitCondition.Builder();
   }

   public static final class Builder {
      private long totalTokens;
      private long inputTokens;
      private long outputTokens;
      private double maxCost;
      private double inputPricePerMillion;
      private double outputPricePerMillion;

      public io.karatelabs.ext.agent.runtime.BudgetExitCondition.Builder totalTokens(long var1) {
         this.totalTokens = var1;
         return this;
      }

      public io.karatelabs.ext.agent.runtime.BudgetExitCondition.Builder inputTokens(long var1) {
         this.inputTokens = var1;
         return this;
      }

      public io.karatelabs.ext.agent.runtime.BudgetExitCondition.Builder outputTokens(long var1) {
         this.outputTokens = var1;
         return this;
      }

      public io.karatelabs.ext.agent.runtime.BudgetExitCondition.Builder maxCost(double var1) {
         this.maxCost = var1;
         return this;
      }

      public io.karatelabs.ext.agent.runtime.BudgetExitCondition.Builder tokenPrices(double var1, double var3) {
         this.inputPricePerMillion = var1;
         this.outputPricePerMillion = var3;
         return this;
      }

      public BudgetExitCondition build() {
         return new BudgetExitCondition(this);
      }
   }
}
