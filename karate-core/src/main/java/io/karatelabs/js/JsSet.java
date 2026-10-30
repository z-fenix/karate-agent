package io.karatelabs.js;

import java.util.LinkedHashMap;
import java.util.List;

class JsSet extends JsObject {
   private static final Object PRESENT = new Object();
   final LinkedHashMap<Object, Object> elements = new LinkedHashMap<>();
   private static final List<String> INTRINSIC_NAMES = List.of("size");
   private static final Object NOT_FOUND = new Object();

   JsSet() {
      super(null, JsSetPrototype.INSTANCE);
   }

   @Override
   protected Object resolveOwnIntrinsic(String name) {
      return "size".equals(name) ? this.elements.size() : null;
   }

   @Override
   protected Iterable<String> ownIntrinsicNames() {
      return INTRINSIC_NAMES;
   }

   private Object findStoredValue(Object normalized) {
      if (this.elements.containsKey(normalized)) {
         return normalized;
      } else if (!(normalized instanceof Number)) {
         return NOT_FOUND;
      } else {
         for (Object v : this.elements.keySet()) {
            if (Terms.eq(v, normalized, true)) {
               return v;
            }
         }

         return NOT_FOUND;
      }
   }

   boolean has(Object value) {
      return this.findStoredValue(JsMap.normalizeKey(value)) != NOT_FOUND;
   }

   boolean addValue(Object value) {
      Object normalized = JsMap.normalizeKey(value);
      if (this.findStoredValue(normalized) != NOT_FOUND) {
         return false;
      } else {
         this.elements.put(normalized, PRESENT);
         return true;
      }
   }

   boolean deleteValue(Object value) {
      Object stored = this.findStoredValue(JsMap.normalizeKey(value));
      if (stored == NOT_FOUND) {
         return false;
      } else {
         this.elements.remove(stored);
         return true;
      }
   }

   void clearAll() {
      this.elements.clear();
   }
}
