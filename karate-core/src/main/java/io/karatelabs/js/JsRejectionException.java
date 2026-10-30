package io.karatelabs.js;

public class JsRejectionException extends RuntimeException {
   private final transient Object reason;

   public JsRejectionException(Object reason) {
      super(describe(reason), reason instanceof JsError je ? je.getJavaCause() : null);
      this.reason = reason;
   }

   private static String describe(Object reason) {
      return reason instanceof JsError je ? je.toString() : String.valueOf(reason);
   }

   public Object getReason() {
      return this.reason;
   }
}
