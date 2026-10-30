package io.karatelabs.ext.rules.twin;

public class TwinRejectionException extends RuntimeException {
   public final String a;

   public TwinRejectionException(String var1) {
      super(var1);
      this.a = var1;
   }
}
