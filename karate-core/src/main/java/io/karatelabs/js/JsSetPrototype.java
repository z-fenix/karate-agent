package io.karatelabs.js;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.NoSuchElementException;

class JsSetPrototype extends Prototype {
   static final JsSetPrototype INSTANCE = new JsSetPrototype();

   private JsSetPrototype() {
      super(JsObjectPrototype.INSTANCE);
      this.install("add", 1, this::add);
      this.install("has", 1, this::has);
      this.install("delete", 1, this::delete);
      this.install("clear", 0, this::clear);
      this.install("forEach", 1, this::forEach);
      this.install("keys", 0, this::values);
      JsBuiltinMethod values = new JsBuiltinMethod("values", 0, this::values);
      this.install("values", values);
      this.install("entries", 0, this::entriesMethod);
      this.install("@@iterator", values);
      this.install("union", 1, this::union);
      this.install("intersection", 1, this::intersection);
      this.install("difference", 1, this::difference);
      this.install("symmetricDifference", 1, this::symmetricDifference);
      this.install("isSubsetOf", 1, this::isSubsetOf);
      this.install("isSupersetOf", 1, this::isSupersetOf);
      this.install("isDisjointFrom", 1, this::isDisjointFrom);
      this.installConstructor("Set");
   }

   private static JsSet asSet(Context context) {
      if (context.getThisObject() instanceof JsSet s) {
         return s;
      } else {
         throw JsErrorException.typeError("Method Set.prototype called on incompatible receiver");
      }
   }

   private Object add(Context context, Object[] args) {
      JsSet s = asSet(context);
      s.addValue(args.length > 0 ? args[0] : Terms.UNDEFINED);
      return s;
   }

   private Object has(Context context, Object[] args) {
      return asSet(context).has(args.length > 0 ? args[0] : Terms.UNDEFINED);
   }

   private Object delete(Context context, Object[] args) {
      return asSet(context).deleteValue(args.length > 0 ? args[0] : Terms.UNDEFINED);
   }

   private Object clear(Context context, Object[] args) {
      asSet(context).clearAll();
      return Terms.UNDEFINED;
   }

   private Object forEach(Context context, Object[] args) {
      JsSet s = asSet(context);
      if (args.length != 0 && args[0] instanceof JsCallable cb) {
         Object thisArg = args.length > 1 ? args[1] : Terms.UNDEFINED;
         CoreContext cc = context instanceof CoreContext c ? c : null;
         Object savedThis = cc != null ? cc.thisObject : null;

         try {
            for (int cursor = 0; cursor < s.elements.size(); cursor++) {
               Iterator<Object> it = s.elements.keySet().iterator();

               for (int i = 0; i < cursor && it.hasNext(); i++) {
                  it.next();
               }

               if (!it.hasNext()) {
                  break;
               }

               Object v = it.next();
               if (cc != null) {
                  cc.thisObject = thisArg;
               }

               cb.call(context, new Object[]{v, v, s});
            }
         } finally {
            if (cc != null) {
               cc.thisObject = savedThis;
            }
         }

         return Terms.UNDEFINED;
      } else {
         throw JsErrorException.typeError("Set.prototype.forEach: callback is not a function");
      }
   }

   private Object values(Context context, Object[] args) {
      JsSet s = asSet(context);
      return IterUtils.toIteratorObject(setIterator(s, false));
   }

   private Object entriesMethod(Context context, Object[] args) {
      JsSet s = asSet(context);
      return IterUtils.toIteratorObject(setIterator(s, true));
   }

   private static JsSetPrototype.SetRecord getSetRecord(Object other, Context context) {
      if (other instanceof ObjectLike obj) {
         CoreContext cc = context instanceof CoreContext c ? c : null;
         Object rawSize = obj.getMember("size", obj, cc);
         Number numSize = Terms.toNumberCoerce(rawSize, cc);
         double d = numSize.doubleValue();
         if (Double.isNaN(d)) {
            throw JsErrorException.typeError("Set-like object's size is not a number");
         } else {
            long intSize;
            if (Double.isInfinite(d)) {
               intSize = d > 0.0 ? Long.MAX_VALUE : Long.MIN_VALUE;
            } else {
               intSize = (long)d;
            }

            if (intSize < 0L) {
               throw JsErrorException.rangeError("Set-like object's size is negative");
            } else if (obj.getMember("has", obj, cc) instanceof JsCallable has) {
               if (obj.getMember("keys", obj, cc) instanceof JsCallable keys) {
                  return new JsSetPrototype.SetRecord(obj, intSize, has, keys);
               } else {
                  throw JsErrorException.typeError("Set-like object's 'keys' is not callable");
               }
            } else {
               throw JsErrorException.typeError("Set-like object's 'has' is not callable");
            }
         }
      } else {
         throw JsErrorException.typeError("Set.prototype method called with non-object");
      }
   }

   private static JsIterator keysIter(JsSetPrototype.SetRecord rec, Context ctx) {
      return IterUtils.iteratorFromCallable(rec.keys, rec.setObj, ctx);
   }

   private static Object normalize(Object v) {
      return JsMap.normalizeKey(v);
   }

   private static void rawAdd(JsSet s, Object value) {
      Object normalized = normalize(value);

      for (Object existing : s.elements.keySet()) {
         if (Terms.eq(existing, normalized, true)) {
            return;
         }
      }

      s.elements.put(normalized, Boolean.TRUE);
   }

   private static boolean otherHas(JsSetPrototype.SetRecord rec, Object value, Context ctx) {
      CoreContext cc = ctx instanceof CoreContext c ? c : null;
      Object savedThis = cc != null ? cc.thisObject : null;

      boolean var6;
      try {
         if (cc != null) {
            cc.thisObject = rec.setObj;
         }

         Object r = rec.has.call(ctx, new Object[]{value});
         var6 = Terms.isTruthy(r);
      } finally {
         if (cc != null) {
            cc.thisObject = savedThis;
         }
      }

      return var6;
   }

   private Object union(Context ctx, Object[] args) {
      JsSet thisSet = asSet(ctx);
      Object other = args.length > 0 ? args[0] : Terms.UNDEFINED;
      JsSetPrototype.SetRecord rec = getSetRecord(other, ctx);
      JsSet result = new JsSet();

      for (Object v : thisSet.elements.keySet()) {
         result.elements.put(v, Boolean.TRUE);
      }

      JsIterator it = keysIter(rec, ctx);

      while (it.hasNext()) {
         Object next = normalize(it.next());
         rawAdd(result, next);
      }

      return result;
   }

   private Object intersection(Context ctx, Object[] args) {
      JsSet thisSet = asSet(ctx);
      Object other = args.length > 0 ? args[0] : Terms.UNDEFINED;
      JsSetPrototype.SetRecord rec = getSetRecord(other, ctx);
      JsSet result = new JsSet();
      if (thisSet.elements.size() <= rec.intSize) {
         for (Object e : new ArrayList<>(thisSet.elements.keySet())) {
            if (thisSet.elements.containsKey(e) && otherHas(rec, e, ctx)) {
               result.elements.put(e, Boolean.TRUE);
            }
         }
      } else {
         JsIterator it = keysIter(rec, ctx);
         LinkedHashSet<Object> seen = new LinkedHashSet<>();

         while (it.hasNext()) {
            Object next = normalize(it.next());
            if (!seen.contains(next)) {
               seen.add(next);
               if (thisSet.has(next)) {
                  rawAdd(result, next);
               }
            }
         }
      }

      return result;
   }

   private Object difference(Context ctx, Object[] args) {
      JsSet thisSet = asSet(ctx);
      Object other = args.length > 0 ? args[0] : Terms.UNDEFINED;
      JsSetPrototype.SetRecord rec = getSetRecord(other, ctx);
      JsSet result = new JsSet();

      for (Object v : thisSet.elements.keySet()) {
         result.elements.put(v, Boolean.TRUE);
      }

      if (thisSet.elements.size() <= rec.intSize) {
         for (Object e : new ArrayList<>(thisSet.elements.keySet())) {
            if (result.elements.containsKey(e) && otherHas(rec, e, ctx)) {
               result.elements.remove(e);
            }
         }
      } else {
         JsIterator it = keysIter(rec, ctx);

         while (it.hasNext()) {
            Object next = normalize(it.next());
            Object stored = null;
            if (result.elements.containsKey(next)) {
               stored = next;
            } else if (next instanceof Number) {
               for (Object k : result.elements.keySet()) {
                  if (Terms.eq(k, next, true)) {
                     stored = k;
                     break;
                  }
               }
            }

            if (stored != null) {
               result.elements.remove(stored);
            }
         }
      }

      return result;
   }

   private Object symmetricDifference(Context ctx, Object[] args) {
      JsSet thisSet = asSet(ctx);
      Object other = args.length > 0 ? args[0] : Terms.UNDEFINED;
      JsSetPrototype.SetRecord rec = getSetRecord(other, ctx);
      JsSet result = new JsSet();

      for (Object v : thisSet.elements.keySet()) {
         result.elements.put(v, Boolean.TRUE);
      }

      JsIterator it = keysIter(rec, ctx);

      while (it.hasNext()) {
         Object next = normalize(it.next());
         Object stored = null;
         if (result.elements.containsKey(next)) {
            stored = next;
         } else if (next instanceof Number) {
            for (Object k : result.elements.keySet()) {
               if (Terms.eq(k, next, true)) {
                  stored = k;
                  break;
               }
            }
         }

         if (stored != null) {
            result.elements.remove(stored);
         } else {
            result.elements.put(next, Boolean.TRUE);
         }
      }

      return result;
   }

   private Object isSubsetOf(Context ctx, Object[] args) {
      JsSet thisSet = asSet(ctx);
      Object other = args.length > 0 ? args[0] : Terms.UNDEFINED;
      JsSetPrototype.SetRecord rec = getSetRecord(other, ctx);
      if (thisSet.elements.size() > rec.intSize) {
         return false;
      } else {
         for (Object e : new ArrayList<>(thisSet.elements.keySet())) {
            if (!otherHas(rec, e, ctx)) {
               return false;
            }
         }

         return true;
      }
   }

   private Object isSupersetOf(Context ctx, Object[] args) {
      JsSet thisSet = asSet(ctx);
      Object other = args.length > 0 ? args[0] : Terms.UNDEFINED;
      JsSetPrototype.SetRecord rec = getSetRecord(other, ctx);
      if (thisSet.elements.size() < rec.intSize) {
         return false;
      } else {
         JsIterator it = keysIter(rec, ctx);

         while (it.hasNext()) {
            Object next = normalize(it.next());
            if (!thisSet.has(next)) {
               return false;
            }
         }

         return true;
      }
   }

   private Object isDisjointFrom(Context ctx, Object[] args) {
      JsSet thisSet = asSet(ctx);
      Object other = args.length > 0 ? args[0] : Terms.UNDEFINED;
      JsSetPrototype.SetRecord rec = getSetRecord(other, ctx);
      if (thisSet.elements.size() <= rec.intSize) {
         for (Object e : new ArrayList<>(thisSet.elements.keySet())) {
            if (otherHas(rec, e, ctx)) {
               return false;
            }
         }
      } else {
         JsIterator it = keysIter(rec, ctx);

         while (it.hasNext()) {
            Object next = normalize(it.next());
            if (thisSet.has(next)) {
               return false;
            }
         }
      }

      return true;
   }

   private static JsIterator setIterator(final JsSet s, final boolean entries) {
      return new JsIterator() {
         int cursor = 0;

         @Override
         public boolean hasNext() {
            return this.cursor < s.elements.size();
         }

         @Override
         public Object next() {
            if (this.cursor >= s.elements.size()) {
               throw new NoSuchElementException();
            } else {
               Iterator<Object> it = s.elements.keySet().iterator();

               for (int i = 0; i < this.cursor; i++) {
                  it.next();
               }

               Object v = it.next();
               this.cursor++;
               if (entries) {
                  List<Object> pair = new ArrayList<>(2);
                  pair.add(v);
                  pair.add(v);
                  return pair;
               } else {
                  return v;
               }
            }
         }
      };
   }

   private static final class SetRecord {
      final ObjectLike setObj;
      final long intSize;
      final JsCallable has;
      final JsCallable keys;

      SetRecord(ObjectLike setObj, long intSize, JsCallable has, JsCallable keys) {
         this.setObj = setObj;
         this.intSize = intSize;
         this.has = has;
         this.keys = keys;
      }
   }
}
