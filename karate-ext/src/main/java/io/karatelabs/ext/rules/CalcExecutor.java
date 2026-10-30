package io.karatelabs.ext.rules;

import io.karatelabs.js.BindEvent;
import io.karatelabs.js.ContextListener;
import io.karatelabs.js.Engine;
import io.karatelabs.js.EngineException;
import io.karatelabs.js.Event;
import io.karatelabs.js.ExitResult;
import io.karatelabs.js.JavaCallable;
import io.karatelabs.js.Terms;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

public final class CalcExecutor {
   private CalcExecutor() {
   }

   public static io.karatelabs.ext.rules.CalcExecutor.CalcExecutionResult a(String var0, Object var1) {
      return a(var0, var1, null);
   }

   public static io.karatelabs.ext.rules.CalcExecutor.CalcExecutionResult a(String var0, Object var1, ContextListener var2) {
      return a(var0, Map.of(), var1, var2);
   }

   public static io.karatelabs.ext.rules.CalcExecutor.CalcExecutionResult a(String var0, Map<String, Object> var1, Object var2, ContextListener var3) {
      Object var4 = io.karatelabs.ext.common.SandboxExecutor.a(var2);
      long var5 = System.currentTimeMillis();
      Engine var7 = new Engine();
      Calc var8 = new Calc(var4);
      var7.put("calc", var8);
      var7.setListener((ContextListener)(var3 == null ? var8 : new io.karatelabs.ext.rules.CalcExecutor.CompositeContextListener(var8, var3)));

      try {
         var7.eval(var0);
         if (!var1.isEmpty()) {
            a(var7, var1);
         }

         if (!(var7.get("execute") instanceof JavaCallable var10)) {
            throw new IllegalStateException("calc.js does not define an 'execute' function");
         }

         var10.call(null, var8);
      } catch (EngineException var11) {
         if (var11.isAuthoredThrow()) {
            throw new io.karatelabs.ext.rules.CalcExecutor.CalcExecutionException(var11.getMessage(), var11, var11.getThrownValue(), var11.getThrowLine(), new LinkedHashSet<>(var8.getReqs()));
         }

         throw var11;
      }

      long var12 = System.currentTimeMillis() - var5;
      return new io.karatelabs.ext.rules.CalcExecutor.CalcExecutionResult(
         var8.getRoot(),
         var8.getOutput(),
         var4,
         var12,
         var8.getDeclaredLevels(),
         var8.getLabels(),
         var8.getOutcome(),
         var8.isOutcomeSet(),
         var8.getProperties(),
         var8.getReqs()
      );
   }

   private static void a(Engine var0, Map<String, Object> var1) {
      Object var2 = var0.get("lookup");
      if (var2 != null && var2 != Terms.UNDEFINED) {
         if (!(var2 instanceof Map var3)) {
            throw new IllegalStateException("calc.js binds 'lookup' to a " + var2.getClass().getSimpleName());
         } else {
            for (Entry var5 : var1.entrySet()) {
               if (var3.containsKey(var5.getKey())) {
                  throw new RuleApiException(
                     400, "LOOKUP_KEY_COLLISION", "table key '" + (String)var5.getKey() + "' collides with the inline key '" + (String)var5.getKey() + "'"
                  );
               }

               var3.put((String)var5.getKey(), var5.getValue());
            }
         }
      } else {
         var0.put("lookup", var1);
      }
   }

   public static class CalcExecutionException extends RuntimeException {
      public final Object a;
      public final int b;
      public final Set<String> c;

      public CalcExecutionException(String var1, Throwable var2, Object var3, int var4, Set<String> var5) {
         super(var1, var2);
         this.a = var3;
         this.b = var4;
         this.c = var5;
      }
   }

   public record CalcExecutionResult(ExecutionTraceNode a, Object b, Object c, long d, List<Object[]> e, Map<Integer, String> f, Object g, boolean h, List<Object[]> i, Set<String> j) {

      public CalcExecutionResult(ExecutionTraceNode a, Object b, Object c, long d, List<Object[]> e, Map<Integer, String> f, Object g, boolean h, List<Object[]> i, Set<String> j) {
         this.a = a;
         this.b = b;
         this.c = c;
         this.d = d;
         this.e = e;
         this.f = f;
         this.g = g;
         this.h = h;
         this.i = i;
         this.j = j;
      }
   }

   private record CompositeContextListener(ContextListener a, ContextListener b) implements ContextListener {

      private CompositeContextListener(ContextListener a, ContextListener b) {
         this.a = a;
         this.b = b;
      }

      @Override
      public void onEvent(Event var1) {
         this.a.onEvent(var1);
         this.b.onEvent(var1);
      }

      @Override
      public ExitResult onError(Event var1, Exception var2) {
         ExitResult var3 = this.a.onError(var1, var2);
         return var3 != null ? var3 : this.b.onError(var1, var2);
      }

      @Override
      public void onBind(BindEvent var1) {
         this.a.onBind(var1);
         this.b.onBind(var1);
      }
   }
}
