package io.karatelabs.js;

import io.karatelabs.parser.AsiHint;
import io.karatelabs.parser.Node;

class JsErrorException extends RuntimeException {
   final JsError payload;
   private static final byte MESSAGE_ATTRS = 5;

   JsErrorException(JsError payload) {
      super(javaMessage(payload), payload.getJavaCause());
      this.payload = payload;
   }

   private static String javaMessage(JsError payload) {
      return payload.toString();
   }

   private static JsError build(JsErrorPrototype proto, String message, Throwable cause) {
      JsError e = new JsError(proto, cause);
      if (message != null) {
         e.defineOwn("message", message, (byte)5);
      }

      return e;
   }

   static JsErrorException error(String message) {
      return new JsErrorException(build(JsErrorPrototype.ERROR, message, null));
   }

   static JsErrorException typeError(String message) {
      return new JsErrorException(build(JsErrorPrototype.TYPE_ERROR, message, null));
   }

   static JsErrorException rangeError(String message) {
      return new JsErrorException(build(JsErrorPrototype.RANGE_ERROR, message, null));
   }

   static JsErrorException syntaxError(String message) {
      return new JsErrorException(build(JsErrorPrototype.SYNTAX_ERROR, message, null));
   }

   static JsErrorException notAFunction(Node callee) {
      String hint = AsiHint.forOpener(callee.getLastToken().getNextPrimary());
      return typeError(callee.getTextIncludingWhitespace() + " is not a function" + (hint == null ? "" : " — " + hint));
   }

   static JsErrorException referenceError(String message) {
      return new JsErrorException(build(JsErrorPrototype.REFERENCE_ERROR, message, null));
   }

   static JsErrorException uninitialized(String name) {
      return new JsErrorException.Uninitialized(build(JsErrorPrototype.REFERENCE_ERROR, "cannot access '" + name + "' before initialization", null));
   }

   static JsErrorException wrap(Throwable cause) {
      String message = cause.getMessage() != null ? cause.getMessage() : cause.getClass().getSimpleName();
      return new JsErrorException(build(JsErrorPrototype.ERROR, message, cause));
   }

   static final class Uninitialized extends JsErrorException {
      Uninitialized(JsError payload) {
         super(payload);
      }
   }
}
