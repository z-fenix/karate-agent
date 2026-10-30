package io.karatelabs.ext.robot;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;

public final class StepDiagnosticsScope {
   private static final ThreadLocal<Deque<List<Map<String, Object>>>> a = new ThreadLocal<>();

   private StepDiagnosticsScope() {
   }

   public static void a() {
      Object var0 = a.get();
      if (var0 == null) {
         var0 = new ArrayDeque();
         a.set((Deque<List<Map<String, Object>>>)var0);
      }

      ((java.util.Deque)var0).push(new ArrayList());
   }

   public static List<Map<String, Object>> b() {
      Deque var0 = a.get();
      if (var0 == null) {
         return List.of();
      } else {
         List var1 = (List)var0.poll();
         if (var0.isEmpty()) {
            a.remove();
         } else if (var1 != null) {
            ((List)var0.peek()).addAll(var1);
         }

         return var1 == null ? List.of() : var1;
      }
   }

   static boolean c() {
      Deque var0 = a.get();
      return var0 != null && !var0.isEmpty();
   }

   static void a(Map<String, Object> var0) {
      Deque var1 = a.get();
      List<java.util.Map<java.lang.String,java.lang.Object>> var2 = var1 == null ? null : (List)var1.peek();
      if (var2 != null) {
         var2.add(var0);
      }
   }
}
