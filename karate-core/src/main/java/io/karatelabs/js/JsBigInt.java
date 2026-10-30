package io.karatelabs.js;

import java.math.BigInteger;

non-sealed class JsBigInt extends JsObject implements JsPrimitive {
   final BigInteger value;

   JsBigInt(BigInteger value) {
      super(null, JsBigIntPrototype.INSTANCE);
      this.value = value;
   }

   @Override
   public String toString() {
      return this.value.toString();
   }

   @Override
   public Object getJavaValue() {
      return this.value;
   }
}
