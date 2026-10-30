package io.karatelabs.js;

import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

class JsObjectPrototype extends Prototype {
   static final JsCallable DEFAULT_TO_STRING = (context, args) -> {
      Object o = context.getThisObject();
      return o instanceof ObjectLike ol && ol.getMember("@@toStringTag") instanceof String s ? "[object " + s + "]" : "[object " + builtinTag(o) + "]";
   };
   static final JsObjectPrototype INSTANCE = new JsObjectPrototype();

   private static String builtinTag(Object o) {
      if (o == null) {
         return "Null";
      } else if (o == Terms.UNDEFINED) {
         return "Undefined";
      } else if (o instanceof JsArray || o instanceof List) {
         return "Array";
      } else if (o instanceof JsDate || o instanceof Date) {
         return "Date";
      } else if (o instanceof JsRegex) {
         return "RegExp";
      } else if (o instanceof JsError) {
         return "Error";
      } else if (o instanceof JsSymbol) {
         return "Symbol";
      } else if (o instanceof JsMap) {
         return "Map";
      } else if (o instanceof JsSet) {
         return "Set";
      } else if (o instanceof JsBoolean || o instanceof Boolean) {
         return "Boolean";
      } else if (o instanceof JsString || o instanceof String) {
         return "String";
      } else if (o instanceof JsNumber || o instanceof Number) {
         return "Number";
      } else if (o instanceof JsFunction) {
         return "Function";
      } else if (o instanceof JsObject jo && jo.isJsFunction()) {
         return "Function";
      } else {
         return !(o instanceof ObjectLike) && o instanceof JsCallable ? "Function" : "Object";
      }
   }

   private JsObjectPrototype() {
      super(null);
      this.install("toString", DEFAULT_TO_STRING);
      this.install("valueOf", 0, (context, args) -> {
         Object thisObj = context.getThisObject();
         Terms.requireObjectCoercible(thisObj, "Object.prototype.valueOf");
         return thisObj;
      });
      this.install("hasOwnProperty", 1, this::hasOwnProperty);
      this.install("isPrototypeOf", 1, this::isPrototypeOf);
      this.install("propertyIsEnumerable", 1, this::propertyIsEnumerable);
      this.install("toLocaleString", 0, this::toLocaleString);
      this.install("__defineGetter__", 2, (ctx, args) -> defineAccessor(ctx, args, true));
      this.install("__defineSetter__", 2, (ctx, args) -> defineAccessor(ctx, args, false));
      this.install("__lookupGetter__", 1, (ctx, args) -> lookupAccessor(ctx, args, true));
      this.install("__lookupSetter__", 1, (ctx, args) -> lookupAccessor(ctx, args, false));
      this.installConstructor("Object");
   }

   private static Object defineAccessor(Context context, Object[] args, boolean isGetter) {
      String label = isGetter ? "Object.prototype.__defineGetter__" : "Object.prototype.__defineSetter__";
      Object thisObj = context.getThisObject();
      Terms.requireObjectCoercible(thisObj, label);
      if ((args.length < 2 ? Terms.UNDEFINED : args[1]) instanceof JsCallable callable) {
         String key = args.length == 0 ? "undefined" : Terms.toPropertyKey(args[0]);
         Map<String, Object> desc = new LinkedHashMap<>();
         desc.put(isGetter ? "get" : "set", callable);
         desc.put("enumerable", true);
         desc.put("configurable", true);
         JsObjectConstructor objectConstructor = (JsObjectConstructor)context.getEngine().builtinConstructor("Object");
         objectConstructor.defineProperty(context, new Object[]{thisObj, key, desc});
         return Terms.UNDEFINED;
      } else {
         throw JsErrorException.typeError(label + ": Expecting function");
      }
   }

   private static Object lookupAccessor(Context context, Object[] args, boolean isGetter) {
      String label = isGetter ? "Object.prototype.__lookupGetter__" : "Object.prototype.__lookupSetter__";
      Object thisObj = context.getThisObject();
      Terms.requireObjectCoercible(thisObj, label);
      String key = args.length == 0 ? "undefined" : Terms.toPropertyKey(args[0]);
      ObjectLike start = thisObj instanceof ObjectLike ol ? ol : Terms.toObjectLike(thisObj);

      for (ObjectLike current = start; current != null; current = current.getPrototype()) {
         PropertySlot slot = PropertyAccess.ownSlot(current, key);
         if (slot instanceof AccessorSlot acc) {
            JsCallable fn = isGetter ? acc.getter : acc.setter;
            return fn == null ? Terms.UNDEFINED : fn;
         }

         if (slot != null) {
            return Terms.UNDEFINED;
         }
      }

      return Terms.UNDEFINED;
   }

   private Object toLocaleString(Context context, Object[] args) {
      Object thisObj = context.getThisObject();
      Terms.requireObjectCoercible(thisObj, "Object.prototype.toLocaleString");
      if (thisObj instanceof ObjectLike ol && ol.getMember("toString") instanceof JsCallable jsc) {
         CoreContext cc = context instanceof CoreContext c ? c : null;
         return cc != null ? Interpreter.invokeGetter(jsc, ol, cc) : jsc.call(context, new Object[0]);
      } else {
         return DEFAULT_TO_STRING.call(context, args);
      }
   }

   private Object isPrototypeOf(Context context, Object[] args) {
      if (args.length != 0 && args[0] instanceof ObjectLike target) {
         Object thisObj = context.getThisObject();
         if (!(thisObj instanceof ObjectLike)) {
            return false;
         } else {
            for (ObjectLike cur = target.getPrototype(); cur != null; cur = cur.getPrototype()) {
               if (cur == thisObj) {
                  return true;
               }
            }

            return false;
         }
      } else {
         return false;
      }
   }

   private Object propertyIsEnumerable(Context context, Object[] args) {
      if (args.length == 0) {
      }

      String prop = args.length == 0 ? "undefined" : Terms.toPropertyKey(args[0]);
      Object thisObj = context.getThisObject();
      Terms.requireObjectCoercible(thisObj, "Object.prototype.propertyIsEnumerable");
      if (!Terms.isTruthy(this.hasOwnProperty(context, args))) {
         return false;
      } else if (thisObj instanceof JsObject jo) {
         return jo.isEnumerable(prop);
      } else if (thisObj instanceof JsArray ja) {
         return ja.isEnumerable(prop);
      } else {
         return thisObj instanceof Prototype p ? (p.getOwnAttrs(prop) & 2) != 0 : true;
      }
   }

   private Object hasOwnProperty(Context context, Object[] args) {
      JsSymbol sym = args.length == 0 ? null : JsSymbol.keyedBy(args[0]);
      String prop = args.length == 0 ? "undefined" : Terms.toPropertyKey(args[0]);
      Object thisObj = context.getThisObject();
      Terms.requireObjectCoercible(thisObj, "Object.prototype.hasOwnProperty");
      if (sym == null) {
         if (thisObj instanceof ObjectLike ol) {
            return ol.isOwnProperty(prop);
         } else {
            return thisObj instanceof Map<?, ?> map ? map.containsKey(prop) : false;
         }
      } else {
         return thisObj instanceof JsObject jo && jo.hasSymbol(sym);
      }
   }
}
