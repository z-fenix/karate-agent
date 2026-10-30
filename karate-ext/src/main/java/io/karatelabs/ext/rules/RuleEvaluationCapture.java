package io.karatelabs.ext.rules;

import java.util.Map;
import java.util.function.Consumer;

public final class RuleEvaluationCapture {
   private RuleEvaluationCapture() {
   }

   public static io.karatelabs.ext.rules.RuleEvaluationCapture.CapturedRuleEvaluation a(RuleEvaluator var0, String var1, Object var2) {
      return a(var0, var1, Map.of(), var2);
   }

   public static io.karatelabs.ext.rules.RuleEvaluationCapture.CapturedRuleEvaluation a(RuleEvaluator var0, String var1, Map<String, Object> var2, Object var3) {
      MutationTraceCollector var4 = new MutationTraceCollector();
      io.karatelabs.ext.rules.RuleEvaluator.RuleEvaluationResult var5 = null;
      boolean var6 = false;

      try {
         var5 = var0.a(var1, var2, var3, var4);
      } catch (RuntimeException var8) {
         var6 = true;
      }

      return a(var4, var1, var5, var6);
   }

   public static io.karatelabs.ext.rules.RuleEvaluator.RuleEvaluationResult a(RuleEvaluator var0, String var1, Map<String, Object> var2, Object var3, Consumer<io.karatelabs.ext.rules.RuleEvaluationCapture.CapturedRuleEvaluation> var4) {
      MutationTraceCollector var5 = new MutationTraceCollector();

      try {
         io.karatelabs.ext.rules.RuleEvaluator.RuleEvaluationResult var6 = var0.a(var1, var2, var3, var5);
         var4.accept(a(var5, var1, var6, false));
         return var6;
      } catch (RuntimeException var7) {
         var4.accept(a(var5, var1, null, true));
         throw var7;
      }
   }

   private static io.karatelabs.ext.rules.RuleEvaluationCapture.CapturedRuleEvaluation a(MutationTraceCollector var0, String var1, io.karatelabs.ext.rules.RuleEvaluator.RuleEvaluationResult var2, boolean var3) {
      return new io.karatelabs.ext.rules.RuleEvaluationCapture.CapturedRuleEvaluation(var2, var0.a(var1), var3);
   }

   public record CapturedRuleEvaluation(io.karatelabs.ext.rules.RuleEvaluator.RuleEvaluationResult a, io.karatelabs.ext.rules.MutationTraceCollector.MutationTraceSnapshot b, boolean c) {

      public CapturedRuleEvaluation(io.karatelabs.ext.rules.RuleEvaluator.RuleEvaluationResult a, io.karatelabs.ext.rules.MutationTraceCollector.MutationTraceSnapshot b, boolean c) {
         this.a = a;
         this.b = b;
         this.c = c;
      }
   }
}
