package io.karatelabs.js;

public class JsJava implements SimpleObject {
   final ExternalBridge bridge;

   JsJava(ExternalBridge bridge) {
      if (bridge == null) {
         throw new RuntimeException("java bridge not enabled");
      } else {
         this.bridge = bridge;
      }
   }

   @Override
   public Object jsGet(String name) {
      return switch (name) {
         case "type" -> (JsInvokable) args -> {
            String className = (String)args[0];
            ExternalAccess type = this.bridge.forType(className);
            if (type == null) {
               throw JsErrorException.typeError("Java.type: class not found: " + className);
            } else {
               return type;
            }
         };
         case "to" -> (JsInvokable) args -> args[0] instanceof ExternalAccess ja ? ja.getJavaValue() : null;
         default -> throw JsErrorException.typeError("no such api on Java: " + name);
      };
   }
}
