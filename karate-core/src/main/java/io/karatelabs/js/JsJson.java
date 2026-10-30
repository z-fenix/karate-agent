package io.karatelabs.js;

import io.karatelabs.common.StringUtils;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

public class JsJson extends JsObject {
   private static final byte METHOD_ATTRS = 13;

   public JsJson() {
      this.defineOwn("stringify", new JsBuiltinMethod("stringify", 3, stringify()), (byte)13);
      this.defineOwn("parse", new JsBuiltinMethod("parse", 2, parse()), (byte)13);
   }

   private static JsCallable stringify() {
      return (context, args) -> {
         Object value = args[0];
         Object replacer = args.length > 1 ? args[1] : null;
         Object space = args.length > 2 ? args[2] : null;
         List<String> propertyList = replacer instanceof List<?> l ? replacerKeys(l) : null;
         JsCallable replacerFn = propertyList == null && replacer instanceof JsCallable fn ? fn : null;
         Object holder = replacerFn == null ? null : rootHolder(value);
         value = serializeProperty(context, holder, "", value, replacerFn, propertyList, Collections.newSetFromMap(new IdentityHashMap<>()));
         boolean pretty = false;
         String indentStr = "  ";
         if (space != null) {
            if (space instanceof Number) {
               int indent = Math.min(((Number)space).intValue(), 10);
               if (indent > 0) {
                  pretty = true;
                  indentStr = " ".repeat(indent);
               }
            } else if (space instanceof String spaceStr && !spaceStr.isEmpty()) {
               pretty = true;
               indentStr = spaceStr.substring(0, Math.min(spaceStr.length(), 10));
            }
         }

         if (!(value instanceof JsUndefined) && !(value instanceof JsFunction)) {
            return value instanceof String s ? StringUtils.formatJsonString(s) : StringUtils.formatJson(value, pretty, false, false, indentStr);
         } else {
            return Terms.UNDEFINED;
         }
      };
   }

   private static List<String> replacerKeys(List<?> replacer) {
      List<String> keys = new ArrayList<>(replacer.size());

      for (Object entry : replacer) {
         String k = replacerKey(entry);
         if (k != null && !keys.contains(k)) {
            keys.add(k);
         }
      }

      return keys;
   }

   private static String replacerKey(Object entry) {
      if (entry instanceof String s) {
         return s;
      } else if (entry instanceof Number n) {
         return Terms.numberToString(n);
      } else {
         return !(entry instanceof JsString) && !(entry instanceof JsNumber) ? null : replacerKey(((JsValue)entry).getJavaValue());
      }
   }

   private static JsObject rootHolder(Object value) {
      JsObject holder = new JsObject(new LinkedHashMap<>(1));
      holder.put("", value);
      return holder;
   }

   private static Object serializeProperty(
      Context context, Object holder, String key, Object value, JsCallable replacerFn, List<String> propertyList, Set<Object> seen
   ) {
      if (value instanceof ObjectLike ol && ol.getMember("toJSON", value, cc(context)) instanceof JsCallable toJson) {
         value = callWithThis(context, toJson, value, new Object[]{key});
      }

      if (replacerFn != null) {
         value = callWithThis(context, replacerFn, holder, new Object[]{key, value});
      }

      if (value instanceof JsFunction) {
         return value;
      } else if (value instanceof JsSymbol) {
         return Terms.UNDEFINED;
      } else {
         if (value instanceof JsValue jv && !(value instanceof JsUndefined)) {
            value = jv.getJavaValue();
         }

         if (!(value instanceof BigInteger) && !(value instanceof JsBigInt)) {
            if (value instanceof Number n && !Double.isFinite(n.doubleValue())) {
               return null;
            } else if (!(value instanceof Map) && !(value instanceof List)) {
               return value;
            } else if (!seen.add(value)) {
               throw JsErrorException.typeError("Converting circular structure to JSON");
            } else {
               Object var16;
               try {
                  var16 = value instanceof List<?> list
                     ? serializeArray(context, list, replacerFn, propertyList, seen)
                     : serializeObject(context, value, replacerFn, propertyList, seen);
               } finally {
                  seen.remove(value);
               }

               return var16;
            }
         } else {
            throw JsErrorException.typeError("Do not know how to serialize a BigInt");
         }
      }
   }

   private static Object serializeObject(Context context, Object value, JsCallable replacerFn, List<String> propertyList, Set<Object> seen) {
      boolean changed = propertyList != null;
      if (value instanceof JsObject jo) {
         CoreContext ctx = cc(context);
         Map<Object, Object> result;
         if (propertyList != null) {
            result = new LinkedHashMap<>(propertyList.size());

            for (String key : propertyList) {
               if (Terms.in(key, value)) {
                  Object child = jo.getMember(key, value, ctx);
                  result.put(key, serializeProperty(context, value, key, child, replacerFn, propertyList, seen));
               }
            }
         } else {
            result = new LinkedHashMap<>();

            for (KeyValue kv : jo.jsEntries(ctx)) {
               result.put(kv.key(), serializeProperty(context, value, kv.key(), kv.value(), replacerFn, propertyList, seen));
            }
         }

         return result;
      } else {
         Map<?, ?> src = value instanceof ObjectLike ol ? ol.toMap() : (Map)value;
         Map<Object, Object> result = new LinkedHashMap<>(src.size());
         if (propertyList != null) {
            for (String keyx : propertyList) {
               if (src.containsKey(keyx)) {
                  result.put(keyx, serializeProperty(context, value, keyx, src.get(keyx), replacerFn, propertyList, seen));
               }
            }
         } else {
            for (Entry<?, ?> entry : src.entrySet()) {
               Object child = entry.getValue();
               Object serialized = serializeProperty(context, value, String.valueOf(entry.getKey()), child, replacerFn, propertyList, seen);
               changed |= serialized != child;
               result.put(entry.getKey(), serialized);
            }
         }

         return changed ? result : value;
      }
   }

   private static Object serializeArray(Context context, List<?> list, JsCallable replacerFn, List<String> propertyList, Set<Object> seen) {
      JsArray array = list instanceof JsArray a ? a : null;
      int size = list.size();
      List<Object> result = new ArrayList<>(size);
      boolean changed = false;

      for (int i = 0; i < size; i++) {
         Object raw = array == null ? list.get(i) : array.list.get(i);
         Object child = array != null && raw == JsArray.HOLE ? array.getMember(String.valueOf(i), array, cc(context)) : JsArray.unwrapHole(raw);
         Object serialized = serializeProperty(context, list, String.valueOf(i), child, replacerFn, propertyList, seen);
         changed |= serialized != raw;
         result.add(serialized);
      }

      return changed ? result : list;
   }

   private static JsCallable parse() {
      return (context, args) -> {
         Object result = JsonParser.parse((String)args[0]);
         return args.length > 1 && args[1] instanceof JsCallable reviver ? internalize(context, rootHolder(result), "", result, reviver) : result;
      };
   }

   private static Object internalize(Context context, Object holder, String key, Object value, JsCallable reviver) {
      if (value instanceof List) {
         List<Object> list = (List<Object>) value;
         for (int i = 0; i < list.size(); i++) {
            list.set(i, internalize(context, list, String.valueOf(i), list.get(i), reviver));
         }
      } else if (value instanceof Map) {
         Map<String, Object> map = (Map<String, Object>) value;
         for (String k : new ArrayList<>(map.keySet())) {
            Object revived = internalize(context, map, k, map.get(k), reviver);
            if (revived == Terms.UNDEFINED) {
               map.remove(k);
            } else {
               map.put(k, revived);
            }
         }
      }

      return callWithThis(context, reviver, holder, new Object[]{key, value});
   }

   private static CoreContext cc(Context context) {
      return context instanceof CoreContext core ? core : null;
   }

   private static Object callWithThis(Context context, JsCallable fn, Object thisObject, Object[] args) {
      CoreContext core = cc(context);
      if (core == null) {
         return fn.call(context, args);
      } else {
         Object saved = core.thisObject;
         core.thisObject = thisObject;

         Object var6;
         try {
            var6 = fn.call(core, args);
         } finally {
            core.thisObject = saved;
         }

         return var6;
      }
   }
}
