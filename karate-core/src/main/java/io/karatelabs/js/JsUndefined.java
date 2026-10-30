package io.karatelabs.js;

public final class JsUndefined implements JsValue {
   public static final JsUndefined INSTANCE = new JsUndefined();

   private JsUndefined() {
   }

   @Override
   public Object getJavaValue() {
      return null;
   }

   @Override
   public Object getJsValue() {
      return this;
   }

   @Override
   public String toString() {
      return "undefined";
   }
}
