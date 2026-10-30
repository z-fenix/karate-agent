package io.karatelabs.ext.rules;

import io.karatelabs.js.ContextListener;
import io.karatelabs.js.JavaCallable;
import io.karatelabs.js.JsFunction;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Function;

public final class RuleEvaluator implements AutoCloseable {
   static final int a = 10000;
   static final int b = 1000;
   static final String c = "KARATE_RULES_CALC_TIMEOUT_MS";
   private static final io.karatelabs.ext.common.SandboxExecutor.SandboxFailureMapper d = new io.karatelabs.ext.common.SandboxExecutor.SandboxFailureMapper() {
      @Override
      public RuntimeException a(long var1) {
         return new RuleApiException(408, "TIMEOUT", "calc.js exceeded " + var1 + "ms timeout");
      }

      @Override
      public RuntimeException a(Throwable var1) {
         if (var1 instanceof RuleApiException var3) {
            return var3;
         } else {
            String var2 = var1.getMessage() == null ? var1.getClass().getSimpleName() : var1.getClass().getSimpleName() + ": " + var1.getMessage();
            return new RuleApiException(422, "RUN_FAILED", var2, var1);
         }
      }

      @Override
      public RuntimeException a() {
         return new RuleApiException(500, "INTERNAL", "run interrupted");
      }
   };
   private final io.karatelabs.ext.common.SandboxExecutor.SandboxExecutionScope e;

   public RuleEvaluator() {
      this(io.karatelabs.ext.common.SandboxExecutor.a("KARATE_RULES_CALC_TIMEOUT_MS", 5000L));
   }

   RuleEvaluator(long var1) {
      this.e = new io.karatelabs.ext.common.SandboxExecutor.SandboxExecutionScope("rule-runner", var1);
   }

   public io.karatelabs.ext.rules.RuleEvaluator.RuleEvaluationResult a(String var1, Object var2) {
      return this.a(var1, var2, null);
   }

   public io.karatelabs.ext.rules.RuleEvaluator.RuleEvaluationResult a(String var1, Object var2, ContextListener var3) {
      return this.a(var1, Map.of(), var2, var3);
   }

   public io.karatelabs.ext.rules.RuleEvaluator.RuleEvaluationResult a(String var1, Map<String, Object> var2, Object var3, ContextListener var4) {
      return this.a(var1, var2, var3, var4, RuleEvaluator::a);
   }

   public <T> T a(String var1, Map<String, Object> var2, Object var3, ContextListener var4, Function<io.karatelabs.ext.rules.CalcExecutor.CalcExecutionResult, T> var5) {
      return this.e.a(() -> (T)var5.apply(io.karatelabs.ext.rules.CalcExecutor.a(var1, var2, var3, var4)), d);
   }

   private static io.karatelabs.ext.rules.RuleEvaluator.RuleEvaluationResult a(io.karatelabs.ext.rules.CalcExecutor.CalcExecutionResult var0) {
      List var1 = var0.a() == null ? List.of() : List.of(a(var0.a()));
      io.karatelabs.ext.rules.RuleEvaluator.EvaluationTree var2 = a(var1, var0.b());
      return new io.karatelabs.ext.rules.RuleEvaluator.RuleEvaluationResult(
         var2.a, var2.b, var0.d(), System.currentTimeMillis(), var2.c, var0.g(), var0.h(), new ArrayList<>(var0.j()), new ArrayList<>(var0.i())
      );
   }

   @Override
   public void close() {
      this.e.close();
   }

   static ResultTreeNode a(ExecutionTraceNode var0) {
      ArrayList<io.karatelabs.ext.rules.ResultTreeNode> var1 = new ArrayList(var0.f.size());

      for (ExecutionTraceNode var3 : var0.f) {
         var1.add(a(var3));
      }

      return new ResultTreeNode(var0.a, var0.b, var0.g, var0.d, var0.e, var0.h, var0.i, var1);
   }

   static io.karatelabs.ext.rules.RuleEvaluator.EvaluationTree a(List<ResultTreeNode> var0, Object var1) {
      boolean var2 = false;
      var var3 = var0;
      int var4 = a(var0);
      if (var4 > 10000) {
         var3 = List.of(new ResultTreeNode(null, 0, 0, false, "(truncated — " + var4 + " nodes exceeded the 10000-node limit)", List.of()));
         var2 = true;
      }

      io.karatelabs.ext.rules.RuleEvaluator.TruncationState var5 = new io.karatelabs.ext.rules.RuleEvaluator.TruncationState();
      Object var6 = var1 == null ? null : a(var1, var5);
      if (var5.a) {
         var2 = true;
      }

      return new io.karatelabs.ext.rules.RuleEvaluator.EvaluationTree(var3, var6, var2);
   }

   private static int a(List<ResultTreeNode> var0) {
      int var1 = 0;

      for (ResultTreeNode var3 : var0) {
         var1 += 1 + a(var3.k());
      }

      return var1;
   }

   private static Object a(Object var0, io.karatelabs.ext.rules.RuleEvaluator.TruncationState var1) {
      if (var0 instanceof JsFunction || var0 instanceof JavaCallable) {
         return var0;
      } else if (var0 instanceof Map var6) {
         LinkedHashMap<java.lang.String, java.lang.Object> var8 = new LinkedHashMap();

         for (Entry var11 : (Iterable<Entry>) (Iterable<?>) (var6.entrySet())) {
            var8.put(var11.getKey().toString(), a(var11.getValue(), var1));
         }

         return var8;
      } else if (!(var0 instanceof List var2)) {
         return var0;
      } else if (var2.size() > 1000) {
         ArrayList<java.lang.Object> var7 = new ArrayList(1001);

         for (int var9 = 0; var9 < 1000; var9++) {
            var7.add(a(var2.get(var9), var1));
         }

         var7.add("… truncated");
         var1.a = true;
         return var7;
      } else {
         ArrayList<java.lang.Object> var3 = new ArrayList(var2.size());

         for (Object var5 : var2) {
            var3.add(a(var5, var1));
         }

         return var3;
      }
   }

   public record RuleEvaluationResult(List<ResultTreeNode> a, Object b, long c, long d, boolean e, Object f, boolean g, List<String> h, List<Object[]> i) {

      public RuleEvaluationResult(List<ResultTreeNode> a, Object b, long c, long d, boolean e, Object f, boolean g, List<String> h, List<Object[]> i) {
         this.a = a;
         this.b = b;
         this.c = c;
         this.d = d;
         this.e = e;
         this.f = f;
         this.g = g;
         this.h = h;
         this.i = i;
      }
   }

   record EvaluationTree(List<ResultTreeNode> a, Object b, boolean c) {

      EvaluationTree(List<ResultTreeNode> a, Object b, boolean c) {
         this.a = a;
         this.b = b;
         this.c = c;
      }
   }

   private static final class TruncationState {
      boolean a;
   }
}
