package io.karatelabs.ext.websocket;

import io.karatelabs.ext.contract.WireCodec;

public class RawCodec implements WireCodec {
   @Override
   public Object encode(Object var1) {
      return var1;
   }

   @Override
   public Object decode(Object var1) {
      return var1;
   }
}
