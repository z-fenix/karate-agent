package io.karatelabs.js;

import java.util.LinkedHashMap;
import java.util.List;

class JsMap extends JsObject {
   final LinkedHashMap<Object, Object> entries = new LinkedHashMap<>();
   private static final List<String> INTRINSIC_NAMES = List.of("size");
   private static final Object NOT_FOUND = new Object();

   JsMap() {
      super(null, JsMapPrototype.INSTANCE);
   }

   @Override
   protected Object resolveOwnIntrinsic(String name) {
      return "size".equals(name) ? this.entries.size() : null;
   }

   @Override
   protected Iterable<String> ownIntrinsicNames() {
      return INTRINSIC_NAMES;
   }

   static Object normalizeKey(Object key) {
      if (key instanceof JsPrimitive jp) {
         key = jp.getJavaValue();
      }

      if (key instanceof Number n) {
         double d = n.doubleValue();
         if (d == 0.0 && Double.doubleToRawLongBits(d) != 0L) {
            if (key instanceof Double) {
               return 0.0;
            }

            if (key instanceof Float) {
               return 0.0F;
            }
         }
      }

      return key;
   }

   private Object findStoredKey(Object normalized) {
      if (this.entries.containsKey(normalized)) {
         return normalized;
      } else if (!(normalized instanceof Number)) {
         return NOT_FOUND;
      } else {
         for (Object k : this.entries.keySet()) {
            if (Terms.eq(k, normalized, true)) {
               return k;
            }
         }

         return NOT_FOUND;
      }
   }

   boolean hasKey(Object key) {
      return this.findStoredKey(normalizeKey(key)) != NOT_FOUND;
   }

   Object getValue(Object key) {
      Object stored = this.findStoredKey(normalizeKey(key));
      return stored == NOT_FOUND ? Terms.UNDEFINED : this.entries.get(stored);
   }

   void setValue(Object key, Object value) {
      Object normalized = normalizeKey(key);
      Object stored = this.findStoredKey(normalized);
      this.entries.put(stored != NOT_FOUND ? stored : normalized, value);
   }

   boolean deleteKey(Object key) {
      Object stored = this.findStoredKey(normalizeKey(key));
      if (stored == NOT_FOUND) {
         return false;
      } else {
         this.entries.remove(stored);
         return true;
      }
   }

   void clearAll() {
      this.entries.clear();
   }
}
