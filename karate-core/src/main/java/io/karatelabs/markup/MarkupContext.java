
package io.karatelabs.markup;

import io.karatelabs.js.JavaInvokable;

import io.karatelabs.js.SimpleObject;

public interface MarkupContext extends SimpleObject {
   String getTemplateName();

   String getCallerTemplateName();

   String read(String var1);

   byte[] readBytes(String var1);

   String toJson(Object var1);

   Object fromJson(String var1);

   default Object getContextSession() {
      return null;
   }

   default void setMarkupScope(MarkupScope scope) {
   }

   default MarkupScope getMarkupScope() {
      return null;
   }

   @Override
   default Object jsGet(String key) {
      return switch (key) {
         case "template" -> this.getTemplateName();
         case "caller" -> this.getCallerTemplateName();
         case "read" -> (JavaInvokable) args -> {
            if (args.length != 0 && args[0] != null) {
               return this.read(args[0].toString());
            } else {
               throw new RuntimeException("read() requires a path argument");
            }
         };
         case "readBytes" -> (JavaInvokable) args -> {
            if (args.length != 0 && args[0] != null) {
               return this.readBytes(args[0].toString());
            } else {
               throw new RuntimeException("readBytes() requires a path argument");
            }
         };
         case "toJson" -> (JavaInvokable) args -> {
            if (args.length == 0) {
               throw new RuntimeException("toJson() requires an object argument");
            } else {
               return this.toJson(args[0]);
            }
         };
         case "fromJson" -> (JavaInvokable) args -> {
            if (args.length != 0 && args[0] != null) {
               return this.fromJson(args[0].toString());
            } else {
               throw new RuntimeException("fromJson() requires a JSON string argument");
            }
         };
         case "get" -> (JavaInvokable) args -> {
            if (args.length != 0 && args[0] != null) {
               String name = args[0].toString();
               Object defaultValue = args.length > 1 ? args[1] : null;
               MarkupScope scope = this.getMarkupScope();
               if (scope != null) {
                  Object v = scope.lookup(name);
                  if (v != null) {
                     return v;
                  }
               }

               return defaultValue;
            } else {
               throw new RuntimeException("context.get() requires a name argument");
            }
         };
         case "set" -> (JavaInvokable) args -> {
            if (args.length != 0 && args[0] != null) {
               String name = args[0].toString();
               Object value = args.length > 1 ? args[1] : null;
               MarkupScope scope = this.getMarkupScope();
               if (scope != null) {
                  scope.set(name, value);
               }

               return null;
            } else {
               throw new RuntimeException("context.set() requires a name argument");
            }
         };
         default -> null;
      };
   }
}
