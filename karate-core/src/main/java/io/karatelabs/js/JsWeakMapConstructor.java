package io.karatelabs.js;

import java.util.List;

class JsWeakMapConstructor extends JsFunction {
   JsWeakMapConstructor() {
      this.name = "WeakMap";
      this.defineOwn("prototype", JsWeakMapPrototype.INSTANCE, (byte)8);
   }

   @Override
   public Object call(Context context, Object[] args) {
      CallInfo callInfo = context.getCallInfo();
      boolean isNew = callInfo != null && callInfo.constructor;
      if (!isNew) {
         throw JsErrorException.typeError("Constructor WeakMap requires 'new'");
      } else {
         JsWeakMap map = new JsWeakMap();
         if (args.length != 0 && args[0] != null && args[0] != Terms.UNDEFINED) {
            if (!(map.getMember("set") instanceof JsCallable adder)) {
               throw JsErrorException.typeError("WeakMap.prototype.set is not callable");
            } else {
               JsIterator iter = IterUtils.getIterator(args[0], context);
               CoreContext cc = context instanceof CoreContext c ? c : null;
               Object savedThis = cc != null ? cc.thisObject : null;

               try {
                  while (iter.hasNext()) {
                     Object entry = iter.next();
                     if (!(entry instanceof List) && !(entry instanceof ObjectLike)) {
                        throw JsErrorException.typeError("Iterator value " + entry + " is not an entry object");
                     }

                     Object k;
                     Object v;
                     if (entry instanceof List<?> list) {
                        k = list.isEmpty() ? Terms.UNDEFINED : list.get(0);
                        v = list.size() < 2 ? Terms.UNDEFINED : list.get(1);
                     } else {
                        ObjectLike ol = (ObjectLike)entry;
                        k = ol.getMember("0");
                        v = ol.getMember("1");
                        if (k == null) {
                           k = Terms.UNDEFINED;
                        }

                        if (v == null) {
                           v = Terms.UNDEFINED;
                        }
                     }

                     if (cc != null) {
                        cc.thisObject = map;
                     }

                     adder.call(context, new Object[]{k, v});
                     if (cc != null && cc.isError()) {
                        return map;
                     }
                  }
               } finally {
                  if (cc != null) {
                     cc.thisObject = savedThis;
                  }
               }

               return map;
            }
         } else {
            return map;
         }
      }
   }
}
