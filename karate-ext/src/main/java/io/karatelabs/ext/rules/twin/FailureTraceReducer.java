package io.karatelabs.ext.rules.twin;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class FailureTraceReducer {
   static final int a = 32;
   private static final String b = "readBackExplainsNoCandidate";
   private static final String c = "CONFIRMED";
   private static final String d = "UNSTABLE";

   private FailureTraceReducer() {
   }

   static Map<String, Object> a(io.karatelabs.ext.rules.twin.FailureTraceReducer.FailureReductionContext var0, List<Map<String, Object>> var1, io.karatelabs.ext.rules.twin.TwinReplayEngine.ReplayResult var2, int var3, io.karatelabs.ext.rules.twin.FailureTraceReducer.ShrinkBudgetCounter var4) {
      return a(var0, var1, var2, var3, var4, null);
   }

   static Map<String, Object> a(io.karatelabs.ext.rules.twin.FailureTraceReducer.FailureReductionContext var0, List<Map<String, Object>> var1, io.karatelabs.ext.rules.twin.TwinReplayEngine.ReplayResult var2, int var3, io.karatelabs.ext.rules.twin.FailureTraceReducer.ShrinkBudgetCounter var4, io.karatelabs.ext.rules.twin.TwinReplayEngine.ReplayResult var5) {
      io.karatelabs.ext.rules.twin.FailureTraceReducer.FailureSignature var6 = a(var2);
      List<Map<String, Object>> var7 = List.of();
      var var8 = var1;
      if (var6.b() != null) {
         int var9 = ((Number)var2.a().get("at")).intValue();
         var7 = List.of(var1.get(var9));
         var8 = var1.subList(0, var9);
      }

      int var19 = 1;
      io.karatelabs.ext.rules.twin.TwinReplayEngine.ReplayResult var10 = var5 == null ? a(var0, var1, var4) : a(var5, var4);
      if (!var6.equals(a(var10))) {
         return a(var1, var1.size(), var19, "UNSTABLE", var10, false);
      } else {
         List var11 = a(var8, var7);
         boolean var12 = false;

         label50:
         while (var8.size() >= 2 && !var12) {
            int var13 = var8.size() / 2;
            var var14 = List.of(var8.subList(0, var13), var8.subList(var13, var8.size()));
            boolean var15 = false;
            var var16 = var14.iterator();

            while (true) {
               if (var16.hasNext()) {
                  List var17 = (List)var16.next();
                  if (var19 >= var3) {
                     var12 = true;
                  } else {
                     List var18 = a(var17, var7);
                     var19++;
                     if (!var6.equals(a(a(var0, var18, var4)))) {
                        continue;
                     }

                     var8 = var17;
                     var11 = var18;
                     var15 = true;
                  }
               }

               if (!var15) {
                  break label50;
               }
               break;
            }
         }

         io.karatelabs.ext.rules.twin.TwinReplayEngine.ReplayResult var21 = a(var0, var11, var4);
         return a(var11, var1.size(), ++var19, var6.equals(a(var21)) ? "CONFIRMED" : "UNSTABLE", var21, var12);
      }
   }

   private static io.karatelabs.ext.rules.twin.TwinReplayEngine.ReplayResult a(io.karatelabs.ext.rules.twin.FailureTraceReducer.FailureReductionContext var0, List<Map<String, Object>> var1, io.karatelabs.ext.rules.twin.FailureTraceReducer.ShrinkBudgetCounter var2) {
      return a(var0.a(var1).b(), var2);
   }

   private static io.karatelabs.ext.rules.twin.TwinReplayEngine.ReplayResult a(io.karatelabs.ext.rules.twin.TwinReplayEngine.ReplayResult var0, io.karatelabs.ext.rules.twin.FailureTraceReducer.ShrinkBudgetCounter var1) {
      var1.a++;

      for (Map var3 : var0.b()) {
         var1.b = var1.b + (var3.containsKey("request") ? 1 : 0);
      }

      return var0;
   }

   static io.karatelabs.ext.rules.twin.FailureTraceReducer.FailureSignature a(io.karatelabs.ext.rules.twin.TwinReplayEngine.ReplayResult var0) {
      var var1 = var0.a();
      if (!"FAIL".equals(var1.get("kind"))) {
         return null;
      } else {
         Map var2 = (Map)var1.get("mismatch");
         String var3 = String.valueOf(var2.get("kind"));
         if ("readBackExplainsNoCandidate".equals(var3)) {
            return new io.karatelabs.ext.rules.twin.FailureTraceReducer.FailureSignature(var3, null, null, null);
         } else {
            var var4 = var0.b().get(var0.b().size() - 1);
            Object var5 = var2.containsKey("reason") ? var2.get("reason") : var2.get("observed");
            return new io.karatelabs.ext.rules.twin.FailureTraceReducer.FailureSignature(var3, a(var4.get("command")), a(var4.get("outcome")), a(var5));
         }
      }
   }

   private static String a(Object var0) {
      return var0 == null ? null : String.valueOf(var0);
   }

   private static List<Map<String, Object>> a(List<Map<String, Object>> var0, List<Map<String, Object>> var1) {
      ArrayList var2 = new ArrayList(var0.size() + var1.size());
      var2.addAll(var0);
      var2.addAll(var1);
      return var2;
   }

   private static Map<String, Object> a(List<Map<String, Object>> var0, int var1, int var2, String var3, io.karatelabs.ext.rules.twin.TwinReplayEngine.ReplayResult var4, boolean var5) {
      LinkedHashMap var6 = new LinkedHashMap();
      ArrayList var7 = new ArrayList(var0.size());

      for (Map var9 : var0) {
         LinkedHashMap var10 = new LinkedHashMap();
         var10.put("command", var9.get("command"));
         if (var9.get("args") != null) {
            var10.put("args", var9.get("args"));
         }

         var7.add(var10);
      }

      var6.put("CONFIRMED".equals(var3) ? "steps" : "attempted", var7);
      var6.put("from", var1);
      var6.put("to", var7.size());
      var6.put("replays", var2);
      var6.put("verified", var3);
      Object var11 = var4.a().get("mismatch");
      if (var11 != null) {
         var6.put("mismatch", var11);
      }

      if (var5) {
         var6.put("budgetExhausted", true);
      }

      return var6;
   }

   record FailureReductionContext(TwinModel a, io.karatelabs.ext.rules.twin.TwinReplayEngine.ReplayObserver b, int c, RulebookRuntime d, ResetHookRunner e) {

      FailureReductionContext(TwinModel a, io.karatelabs.ext.rules.twin.TwinReplayEngine.ReplayObserver b, int c, RulebookRuntime d, ResetHookRunner e) {
         this.a = a;
         this.b = b;
         this.c = c;
         this.d = d;
         this.e = e;
      }

      io.karatelabs.ext.rules.twin.FailureTraceReducer.FailureReductionContext.ResetAndReplayResult a(List<Map<String, Object>> var1) {
         io.karatelabs.ext.rules.twin.ResetHookRunner.ResetHookResult var2 = this.e == null ? null : this.e.b();
         return new io.karatelabs.ext.rules.twin.FailureTraceReducer.FailureReductionContext.ResetAndReplayResult(var2, var2 != null && var2.a() ? io.karatelabs.ext.rules.twin.TwinReplayEngine.resetHookFailure(var2.c(), var2.d()) : io.karatelabs.ext.rules.twin.TwinReplayEngine.replay(this.a, this.b, var1, this.c, this.d));
      }

      record ResetAndReplayResult(io.karatelabs.ext.rules.twin.ResetHookRunner.ResetHookResult a, io.karatelabs.ext.rules.twin.TwinReplayEngine.ReplayResult b) {

         ResetAndReplayResult(io.karatelabs.ext.rules.twin.ResetHookRunner.ResetHookResult a, io.karatelabs.ext.rules.twin.TwinReplayEngine.ReplayResult b) {
            this.a = a;
            this.b = b;
         }
      }
   }

   record FailureSignature(String a, String b, String c, String d) {

      FailureSignature(String a, String b, String c, String d) {
         this.a = a;
         this.b = b;
         this.c = c;
         this.d = d;
      }
   }

   static final class ShrinkBudgetCounter {
      private int a;
      private int b;

      Map<String, Object> a() {
         LinkedHashMap var1 = new LinkedHashMap();
         var1.put("replays", this.a);
         var1.put("exchanges", this.b);
         return var1;
      }
   }
}
