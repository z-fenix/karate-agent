package io.karatelabs.js;

public non-sealed class JsBoolean extends JsObject implements JsPrimitive {
   final boolean value;

   JsBoolean() {
      this(false);
   }

   JsBoolean(boolean value) {
      super(null, JsBooleanPrototype.INSTANCE);
      this.value = value;
   }

   @Override
   public Object getJavaValue() {
      return this.value;
   }
}
