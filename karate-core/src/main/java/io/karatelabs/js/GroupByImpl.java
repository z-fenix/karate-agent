package io.karatelabs.js;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

final class GroupByImpl {
   private GroupByImpl() {
   }

   static List<GroupByImpl.Group> run(Object items, Object callback, boolean propertyMode, Context context) {
      if (items != null && items != Terms.UNDEFINED) {
         if (!(callback instanceof JsCallable cb)) {
            throw JsErrorException.typeError("groupBy callback is not a function");
         } else {
            CoreContext cc = context instanceof CoreContext c ? c : null;
            JsIterator iter = IterUtils.getIterator(items, context);
            List<GroupByImpl.Group> groups = new ArrayList<>();

            for (long k = 0L; iter.hasNext(); k++) {
               Object value = iter.next();
               Object rawKey = cb.call(context, new Object[]{value, (double)k});
               if (cc != null && cc.isError()) {
                  return groups;
               }

               Object key;
               if (propertyMode) {
                  key = Terms.toPropertyKey(rawKey, cc);
                  if (cc != null && cc.isError()) {
                     return groups;
                  }
               } else {
                  key = JsMap.normalizeKey(rawKey);
               }

               GroupByImpl.Group existing = null;

               for (GroupByImpl.Group g : groups) {
                  if (Terms.sameValue(g.key, key)) {
                     existing = g;
                     break;
                  }
               }

               if (existing == null) {
                  existing = new GroupByImpl.Group(key);
                  groups.add(existing);
               }

               existing.values.add(value);
            }

            return groups;
         }
      } else {
         throw JsErrorException.typeError("groupBy called with null or undefined items");
      }
   }

   static JsObject toNullProtoObject(List<GroupByImpl.Group> groups) {
      JsObject result = new JsObject();
      result.setPrototype(null);

      for (GroupByImpl.Group g : groups) {
         String name = (String)g.key;
         JsArray arr = new JsArray(new ArrayList<>(g.values));
         result.putMember(name, arr);
      }

      return result;
   }

   static JsMap toMap(List<GroupByImpl.Group> groups) {
      JsMap map = new JsMap();

      for (GroupByImpl.Group g : groups) {
         JsArray arr = new JsArray(new ArrayList<>(g.values));
         map.entries.put(g.key, arr);
      }

      return map;
   }

   static LinkedHashMap<String, List<Object>> toStringKeyedMap(List<GroupByImpl.Group> groups) {
      LinkedHashMap<String, List<Object>> result = new LinkedHashMap<>();

      for (GroupByImpl.Group g : groups) {
         result.put((String)g.key, g.values);
      }

      return result;
   }

   static final class Group {
      final Object key;
      final List<Object> values = new ArrayList<>();

      Group(Object key) {
         this.key = key;
      }
   }
}
