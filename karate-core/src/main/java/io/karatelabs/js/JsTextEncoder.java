package io.karatelabs.js;

import java.nio.charset.StandardCharsets;
import java.util.List;

class JsTextEncoder extends JsObject implements JsCallable {
   private static final List<String> INTRINSIC_NAMES = List.of("encode");

   @Override
   protected Object resolveOwnIntrinsic(String name) {
      return "encode".equals(name) ? (JavaInvokable) args -> {
         String text = args[0].toString();
         return text.getBytes(StandardCharsets.UTF_8);
      } : null;
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
