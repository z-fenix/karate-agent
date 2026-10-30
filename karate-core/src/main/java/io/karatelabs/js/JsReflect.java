
package io.karatelabs.js;

import io.karatelabs.js.JavaInvokable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

class JsReflect extends JsObject {
   private static final List<String> INTRINSIC_NAMES = List.of("construct", "apply", "ownKeys");

   @Override
   protected Object resolveOwnIntrinsic(String name) {
      return switch (name) {
         case "construct" -> (JsCallable) this::construct;
         case "apply" -> (JsCallable) this::apply;
         case "ownKeys" -> (JsCallable) this::ownKeys;
         default -> null;
      };
   }

   private Object ownKeys(Context context, Object[] args) {
      if (args.length >= 1 && args[0] instanceof ObjectLike target) {
         List<Object> keys = new ArrayList<>(JsObject.orderedOwnKeys(target instanceof JsArray ja ? ja.namedPropsView().keySet() : target.toMap().keySet()));
         if (target instanceof JsObject jo) {
            keys.addAll(jo.ownSymbols());
         }

         return new JsArray(keys);
      } else {
         throw JsErrorException.typeError("Reflect.ownKeys called on non-object");
      }
   }

   @Override
   protected Iterable<String> ownIntrinsicNames() {
      return INTRINSIC_NAMES;
   }

   private Object construct(Context context, Object[] args) {
      if (javaTypeAsConstructor(args.length < 1 ? Terms.UNDEFINED : args[0], context) instanceof JsCallable target) {
         Object[] cArgs = listToArray(args.length >= 2 ? args[1] : null);
         JsCallable check = target;
         if (args.length >= 3) {
            if (!(javaTypeAsConstructor(args[2], context) instanceof JsCallable nt)) {
               throw JsErrorException.typeError("Reflect.construct: newTarget is not a constructor");
            }

            check = nt;
         }

         if (!check.isConstructable()) {
            throw JsErrorException.typeError("Reflect.construct: target is not a constructor");
         } else if (context instanceof CoreContext cc) {
            return Interpreter.constructFromHost(target, cArgs, cc);
         } else {
            throw JsErrorException.typeError("Reflect.construct: bad context");
         }
      } else {
         throw JsErrorException.typeError("Reflect.construct: target is not a constructor");
      }
   }

   private static Object javaTypeAsConstructor(Object o, Context context) {
      return o instanceof ExternalAccess ea && ea.getJavaValue() instanceof Class && context instanceof CoreContext cc && cc.root.bridge != null
         ? PropertyAccess.externalConstructor(ea)
         : o;
   }

   private Object apply(Context context, Object[] args) {
      if (args.length >= 1 && args[0] instanceof JsCallable target) {
         Object thisArg = args.length >= 2 ? args[1] : Terms.UNDEFINED;
         Object[] cArgs = listToArray(args.length >= 3 ? args[2] : null);
         if (target.isExternal()) {
            Interpreter.marshalExternalArgs(cArgs);
         }

         if (context instanceof CoreContext cc) {
            cc.thisObject = thisArg;
         }

         return target.call(context, cArgs);
      } else {
         throw JsErrorException.typeError("Reflect.apply: target is not callable");
      }
   }

   private static Object[] listToArray(Object value) {
      if (value != null && value != Terms.UNDEFINED) {
         Object[] out = null;
         if (value instanceof List<?> list) {
            out = list.toArray();
         } else if (value instanceof Object[] arr) {
            out = Arrays.copyOf(arr, arr.length, Object[].class);
         }

         if (out == null) {
            throw JsErrorException.typeError("argumentsList must be an iterable object");
         } else {
            for (int i = 0; i < out.length; i++) {
               out[i] = JsArray.unwrapHole(out[i]);
            }

            return out;
         }
      } else {
         return new Object[0];
      }
   }
}
