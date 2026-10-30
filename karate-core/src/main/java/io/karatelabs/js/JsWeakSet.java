package io.karatelabs.js;

import java.util.IdentityHashMap;

class JsWeakSet extends JsObject {
   private static final Object PRESENT = new Object();
   private final IdentityHashMap<Object, Object> elements = new IdentityHashMap<>();

   JsWeakSet() {
      super(null, JsWeakSetPrototype.INSTANCE);
   }

   boolean has(Object value) {
      return JsWeakMap.canBeHeldWeakly(value) && this.elements.containsKey(value);
   }

   void addValue(Object value) {
      if (!JsWeakMap.canBeHeldWeakly(value)) {
         throw JsErrorException.typeError("Invalid value used in weak set");
      } else {
         this.elements.put(value, PRESENT);
      }
   }

   boolean deleteValue(Object value) {
      if (!this.has(value)) {
         return false;
      } else {
         this.elements.remove(value);
         return true;
      }
   }
}
