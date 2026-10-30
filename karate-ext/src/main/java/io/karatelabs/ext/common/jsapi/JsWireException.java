package io.karatelabs.ext.common.jsapi;

import java.util.Map;

public interface JsWireException {
   String a();

   String b();

   int c();

   default Map<String, Object> d() {
      return null;
   }
}
