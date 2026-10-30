package io.karatelabs.ext.postman;

public class UnsupportedPostmanFeatureException extends RuntimeException {
   public final String a;

   public UnsupportedPostmanFeatureException(String var1) {
      super("not supported: " + var1);
      this.a = var1;
   }

   static UnsupportedPostmanFeatureException a(Throwable var0) {
      while (var0 != null) {
         if (var0 instanceof UnsupportedPostmanFeatureException var1) {
            return var1;
         }

         var0 = var0.getCause();
      }

      return null;
   }
}
