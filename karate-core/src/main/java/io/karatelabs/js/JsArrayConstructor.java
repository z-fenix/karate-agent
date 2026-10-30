package io.karatelabs.js;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

class JsArrayConstructor extends JsFunction {
   private static final byte METHOD_ATTRS = 13;

   JsArrayConstructor() {
      this.name = "Array";
      this.length = 1;
      this.installIntrinsics();
   }

   private void installIntrinsics() {
      this.defineOwn("from", new JsBuiltinMethod("from", 1, this::from), (byte)13);
      this.defineOwn("isArray", new JsBuiltinMethod("isArray", 1, (ctx, args) -> this.isArray(args)), (byte)13);
      this.defineOwn("of", new JsBuiltinMethod("of", 0, (ctx, args) -> this.of(args)), (byte)13);
      this.defineOwn("prototype", JsArrayPrototype.INSTANCE, (byte)8);
   }

   @Override
   public Object call(Context context, Object[] args) {
      return JsArray.create(args);
   }

   private Object from(Context context, Object[] args) {
      if (args.length != 0 && args[0] != null && args[0] != Terms.UNDEFINED) {
         Object source = args[0];
         JsCallable mapFn = args.length > 1 && args[1] instanceof JsCallable ? (JsCallable)args[1] : null;
         List<Object> results = new ArrayList<>();
         JsIterator iter = IterUtils.tryGetIterator(source, context);
         if (iter != null) {
            for (int index = 0; iter.hasNext(); index++) {
               Object v = iter.next();
               Object mapped = mapFn == null ? v : mapFn.call(context, new Object[]{v, index});
               results.add(mapped);
            }

            return new JsArray(results);
         } else if (source instanceof Map) {
            JsArray array = JsArray.toArray((Map<String, Object>)source);
            int index = 0;

            for (Object v : array.list) {
               Object mapped = mapFn == null ? v : mapFn.call(context, new Object[]{v, index});
               results.add(mapped);
               index++;
            }

            return new JsArray(results);
         } else {
            throw JsErrorException.typeError(source + " is not iterable");
         }
      } else {
         throw JsErrorException.typeError("Array.from requires an iterable or array-like object, not " + (args.length == 0 ? "undefined" : args[0]));
      }
   }

   private Object isArray(Object[] args) {
      return args.length > 0 && args[0] instanceof List;
   }

   private Object of(Object[] args) {
      List<Object> list = new ArrayList<>(args.length);

      for (Object arg : args) {
         list.add(arg);
      }

      return new JsArray(list);
   }
}
