package io.karatelabs.js;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Map.Entry;

class JsMapPrototype extends Prototype {
   static final JsMapPrototype INSTANCE = new JsMapPrototype();

   private JsMapPrototype() {
      super(JsObjectPrototype.INSTANCE);
      this.install("get", 1, this::get);
      this.install("set", 2, this::set);
      this.install("has", 1, this::has);
      this.install("delete", 1, this::delete);
      this.install("clear", 0, this::clear);
      this.install("forEach", 1, this::forEach);
      this.install("keys", 0, this::keys);
      this.install("values", 0, this::values);
      JsBuiltinMethod entries = new JsBuiltinMethod("entries", 0, this::entriesMethod);
      this.install("entries", entries);
      this.install("@@iterator", entries);
      this.install("getOrInsert", 2, this::getOrInsert);
      this.install("getOrInsertComputed", 2, this::getOrInsertComputed);
      this.installConstructor("Map");
   }

   private static JsMap asMap(Context context) {
      if (context.getThisObject() instanceof JsMap m) {
         return m;
      } else {
         throw JsErrorException.typeError("Method Map.prototype called on incompatible receiver");
      }
   }

   private Object get(Context context, Object[] args) {
      return asMap(context).getValue(args.length > 0 ? args[0] : Terms.UNDEFINED);
   }

   private Object set(Context context, Object[] args) {
      JsMap m = asMap(context);
      Object key = args.length > 0 ? args[0] : Terms.UNDEFINED;
      Object value = args.length > 1 ? args[1] : Terms.UNDEFINED;
      m.setValue(key, value);
      return m;
   }

   private Object has(Context context, Object[] args) {
      return asMap(context).hasKey(args.length > 0 ? args[0] : Terms.UNDEFINED);
   }

   private Object delete(Context context, Object[] args) {
      return asMap(context).deleteKey(args.length > 0 ? args[0] : Terms.UNDEFINED);
   }

   private Object clear(Context context, Object[] args) {
      asMap(context).clearAll();
      return Terms.UNDEFINED;
   }

   private Object forEach(Context context, Object[] args) {
      JsMap m = asMap(context);
      if (args.length != 0 && args[0] instanceof JsCallable cb) {
         for (int var8 = 0; var8 < m.entries.size(); var8++) {
            Iterator<Entry<Object, Object>> it = m.entries.entrySet().iterator();

            for (int i = 0; i < var8 && it.hasNext(); i++) {
               it.next();
            }

            if (!it.hasNext()) {
               break;
            }

            Entry<Object, Object> e = it.next();
            cb.call(context, new Object[]{e.getValue(), e.getKey(), m});
         }

         return Terms.UNDEFINED;
      } else {
         throw JsErrorException.typeError("Map.prototype.forEach: callback is not a function");
      }
   }

   private Object keys(Context context, Object[] args) {
      JsMap m = asMap(context);
      return IterUtils.toIteratorObject(mapIterator(m, JsMapPrototype.MapIteratorKind.KEY));
   }

   private Object values(Context context, Object[] args) {
      JsMap m = asMap(context);
      return IterUtils.toIteratorObject(mapIterator(m, JsMapPrototype.MapIteratorKind.VALUE));
   }

   private Object entriesMethod(Context context, Object[] args) {
      JsMap m = asMap(context);
      return IterUtils.toIteratorObject(mapIterator(m, JsMapPrototype.MapIteratorKind.KEY_VALUE));
   }

   private Object getOrInsert(Context context, Object[] args) {
      JsMap m = asMap(context);
      Object key = args.length > 0 ? args[0] : Terms.UNDEFINED;
      Object value = args.length > 1 ? args[1] : Terms.UNDEFINED;
      if (m.hasKey(key)) {
         return m.getValue(key);
      } else {
         m.setValue(key, value);
         return value;
      }
   }

   private Object getOrInsertComputed(Context context, Object[] args) {
      JsMap m = asMap(context);
      Object key = args.length > 0 ? args[0] : Terms.UNDEFINED;
      if ((args.length > 1 ? args[1] : Terms.UNDEFINED) instanceof JsCallable callable) {
         if (m.hasKey(key)) {
            return m.getValue(key);
         } else {
            Object canonicalKey = JsMap.normalizeKey(key);
            Object value = callable.call(context, new Object[]{canonicalKey});
            CoreContext cc = context instanceof CoreContext c ? c : null;
            if (cc != null && cc.isError()) {
               return Terms.UNDEFINED;
            } else {
               if (value == null) {
                  value = Terms.UNDEFINED;
               }

               m.setValue(canonicalKey, value);
               return value;
            }
         }
      } else {
         throw JsErrorException.typeError("Map.prototype.getOrInsertComputed: callback is not a function");
      }
   }

   private static JsIterator mapIterator(final JsMap m, final JsMapPrototype.MapIteratorKind kind) {
      return new JsIterator() {
         int cursor = 0;

         @Override
         public boolean hasNext() {
            return this.cursor < m.entries.size();
         }

         @Override
         public Object next() {
            if (this.cursor >= m.entries.size()) {
               throw new NoSuchElementException();
            } else {
               Iterator<Entry<Object, Object>> it = m.entries.entrySet().iterator();

               for (int i = 0; i < this.cursor; i++) {
                  it.next();
               }

               Entry<Object, Object> e = it.next();
               this.cursor++;

               return switch (kind) {
                  case KEY -> e.getKey();
                  case VALUE -> e.getValue();
                  case KEY_VALUE -> {
                     List<Object> pair = new ArrayList<>(2);
                     pair.add(e.getKey());
                     pair.add(e.getValue());
                     yield pair;
                  }
               };
            }
         }
      };
   }

   private static enum MapIteratorKind {
      KEY,
      VALUE,
      KEY_VALUE;
   }
}
