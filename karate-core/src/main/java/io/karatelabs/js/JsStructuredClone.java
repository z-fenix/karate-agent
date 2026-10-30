package io.karatelabs.js;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

final class JsStructuredClone {
   private static final byte DATA_ATTRS = 5;

   private JsStructuredClone() {
   }

   static Object call(Context context, Object[] args) {
      if (args.length == 0) {
         throw JsErrorException.typeError("structuredClone requires at least 1 argument");
      } else {
         return clone(args[0], new IdentityHashMap<>(), context instanceof CoreContext cc ? cc : null);
      }
   }

   private static Object clone(Object value, IdentityHashMap<Object, Object> memo, CoreContext ctx) {
      String type = Terms.typeOf(value);
      if (!"function".equals(type) && !"symbol".equals(type)) {
         Object seen = memo.get(value);
         if (seen != null) {
            return seen;
         } else if (value instanceof JsPrimitive jp) {
            return remember(memo, value, boxed(jp));
         } else if (value instanceof JsDate d) {
            return remember(memo, value, new JsDate(d.getTimeValue()));
         } else if (value instanceof JsRegex r) {
            return remember(memo, value, new JsRegex(r.pattern, r.flags));
         } else if (value instanceof JsArray a) {
            JsArray copy = new JsArray(new ArrayList<>(a.list.size()));
            memo.put(value, copy);
            copyOwn(a.jsEntries(ctx), copy, memo, ctx);

            while (copy.list.size() < a.list.size()) {
               copy.list.add(JsArray.HOLE);
            }

            return copy;
         } else if (value instanceof JsMap m) {
            JsMap copy = new JsMap();
            memo.put(value, copy);

            for (Entry<Object, Object> e : m.entries.entrySet()) {
               copy.setValue(clone(e.getKey(), memo, ctx), clone(e.getValue(), memo, ctx));
            }

            copyOwn(m.jsEntries(ctx), copy, memo, ctx);
            return copy;
         } else if (value instanceof JsSet s) {
            JsSet copy = new JsSet();
            memo.put(value, copy);

            for (Object e : s.elements.keySet()) {
               copy.addValue(clone(e, memo, ctx));
            }

            copyOwn(s.jsEntries(ctx), copy, memo, ctx);
            return copy;
         } else if (value instanceof JsObject o) {
            JsErrorPrototype ep = builtinErrorPrototype(o);
            if (ep == null) {
               JsObject copy = new JsObject();
               memo.put(value, copy);
               copyOwn(o.jsEntries(ctx), copy, memo, ctx);
               return copy;
            } else {
               JsError copy = new JsError(ep);
               memo.put(value, copy);
               if (PropertyAccess.ownSlot(o, "message") instanceof DataSlot ds
                  && !ds.tombstoned
                  && !(ds.value instanceof JsObject mo && builtinErrorPrototype(mo) != null)) {
                  copy.defineOwn("message", Terms.toStringCoerce(ds.value, ctx), (byte)5);
               }

               for (String key : JsObject.orderedOwnKeys(o.keySet())) {
                  if (!"message".equals(key) && o.isOwnProperty(key) && o.isEnumerable(key)) {
                     copy.putMember(key, clone(o.getMember(key, o, ctx), memo, ctx));
                  }
               }

               return copy;
            }
         } else if (value instanceof Map<?, ?> m) {
            Map<Object, Object> copy = new LinkedHashMap<>();
            memo.put(value, copy);

            for (Entry<?, ?> e : m.entrySet()) {
               copy.put(e.getKey(), clone(e.getValue(), memo, ctx));
            }

            return copy;
         } else if (!(value instanceof List<?> l)) {
            return value;
         } else {
            List<Object> copy = new ArrayList<>(l.size());
            memo.put(value, copy);

            for (Object e : l) {
               copy.add(clone(e, memo, ctx));
            }

            return copy;
         }
      } else {
         throw dataCloneError(type);
      }
   }

   private static Object remember(IdentityHashMap<Object, Object> memo, Object source, Object copy) {
      memo.put(source, copy);
      return copy;
   }

   private static void copyOwn(Iterable<KeyValue> entries, ObjectLike target, IdentityHashMap<Object, Object> memo, CoreContext ctx) {
      for (KeyValue kv : entries) {
         target.putMember(kv.key(), clone(kv.value(), memo, ctx));
      }
   }

   private static JsErrorPrototype builtinErrorPrototype(JsObject o) {
      for (ObjectLike p = o.getPrototype(); p != null; p = p.getPrototype()) {
         if (p instanceof JsErrorPrototype ep) {
            return ep;
         }
      }

      return null;
   }

   private static Object boxed(JsPrimitive jp) {
      Object v = jp.getJavaValue();
      if (v instanceof String s) {
         return new JsString(s);
      } else if (v instanceof Boolean b) {
         return new JsBoolean(b);
      } else if (v instanceof BigInteger bi) {
         return new JsBigInt(bi);
      } else {
         return v instanceof Number n ? new JsNumber(n) : jp;
      }
   }

   private static JsErrorException dataCloneError(String type) {
      JsError e = new JsError(JsErrorPrototype.ERROR);
      e.defineOwn("message", "DataCloneError: a " + type + " could not be cloned", (byte)5);
      e.defineOwn("name", "DataCloneError", (byte)5);
      return new JsErrorException(e);
   }
}
