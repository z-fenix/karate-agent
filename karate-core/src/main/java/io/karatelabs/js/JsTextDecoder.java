package io.karatelabs.js;

import java.nio.charset.StandardCharsets;
import java.util.List;

class JsTextDecoder extends JsObject implements JsCallable {
   private static final List<String> INTRINSIC_NAMES = List.of("encoding", "decode");

   @Override
   protected Object resolveOwnIntrinsic(String name) {
      if ("encoding".equals(name)) {
         return "utf-8";
      } else {
         return "decode".equals(name)
            ? (JsInvokable) args -> args.length == 0
               ? ""
               : args[0] instanceof byte[] bytes ? new String(bytes, StandardCharsets.UTF_8) : ""
            : null;
      }
   }

   @Override
   protected Iterable<String> ownIntrinsicNames() {
      return INTRINSIC_NAMES;
   }

   @Override
   public Object call(Context context, Object[] args) {
      return this;
   }

   @Override
   public boolean isConstructable() {
      return true;
   }
}
