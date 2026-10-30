package io.karatelabs.ext.rules.twin;

import io.karatelabs.ext.common.jsapi.JsApi;

final class ThrowableUtils {
   private ThrowableUtils() {
   }

   static <T extends Throwable> T a(Throwable var0, Class<T> var1) {
      Throwable var2 = var0;

      for (int var3 = 0; var2 != null && var3 < 10; var3++) {
         if (var1.isInstance(var2)) {
            return (T)var1.cast(var2);
         }

         if (var2.getCause() == var2) {
            break;
         }

         var2 = var2.getCause();
      }

      return null;
   }

   static String a(Throwable var0) {
      return JsApi.jsErrorMessage(var0);
   }
}
