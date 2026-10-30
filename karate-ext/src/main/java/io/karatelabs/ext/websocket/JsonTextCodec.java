package io.karatelabs.ext.websocket;

import io.karatelabs.common.Json;
import io.karatelabs.ext.contract.WireCodec;

public class JsonTextCodec implements WireCodec {
   @Override
   public Object encode(Object var1) {
      return Json.of(var1).toString();
   }

   @Override
   public Object decode(Object var1) {
      return Json.of(String.valueOf(var1)).asMap();
   }
}
