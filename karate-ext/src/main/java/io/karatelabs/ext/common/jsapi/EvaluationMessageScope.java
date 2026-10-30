package io.karatelabs.ext.common.jsapi;

public final class EvaluationMessageScope {
   private static final ThreadLocal<String> a = new ThreadLocal<>();

   private EvaluationMessageScope() {
   }

   public static void a(String var0) {
      if (var0 != null && a.get() == null) {
         a.set(var0);
      }
   }

   public static String a() {
      String var0 = a.get();
      a.remove();
      return var0;
   }

   public static void b() {
      a.remove();
   }
}
