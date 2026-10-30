package io.karatelabs.js;

class JsPerformance extends JsObject {
   private static final byte CONSTANT_ATTRS = 8;
   private static final byte METHOD_ATTRS = 13;

   JsPerformance(long nanoOrigin, double timeOrigin) {
      this.defineOwn("now", new JsBuiltinMethod("now", 0, (JsInvokable)args -> (System.nanoTime() - nanoOrigin) / 1000000.0), (byte)13);
      this.defineOwn("timeOrigin", timeOrigin, (byte)8);
   }
}
