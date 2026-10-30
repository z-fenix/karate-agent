package io.karatelabs.js;

public sealed interface JsValue permits JsUndefined, JsPrimitive, JsDateValue, JsBinaryValue {
   Object getJavaValue();

   default Object getJsValue() {
      return this.getJavaValue();
   }

   default Object getOriginalJavaValue() {
      return null;
   }
}
