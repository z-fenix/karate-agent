package io.karatelabs.js;

import java.util.IdentityHashMap;

class JsWeakMap extends JsObject {
   private final IdentityHashMap<Object, Object> entries = new IdentityHashMap<>();

   JsWeakMap() {
      super(null, JsWeakMapPrototype.INSTANCE);
   }

   static boolean canBeHeldWeakly(Object value) {
      return !Terms.isPrimitive(value);
   }

   boolean hasKey(Object key) {
      return canBeHeldWeakly(key) && this.entries.containsKey(key);
   }

   Object getValue(Object key) {
      return this.hasKey(key) ? this.entries.get(key) : Terms.UNDEFINED;
   }

   void setValue(Object key, Object value) {
      if (!canBeHeldWeakly(key)) {
         throw JsErrorException.typeError("Invalid value used as weak map key");
      } else {
         this.entries.put(key, value);
      }
   }

   boolean deleteKey(Object key) {
      if (!this.hasKey(key)) {
         return false;
      } else {
         this.entries.remove(key);
         return true;
      }
   }
}
