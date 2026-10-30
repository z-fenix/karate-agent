package io.karatelabs.js;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.Map.Entry;

public class Bindings implements Map<String, Object> {
   private final BindingsStore store;

   Bindings(BindingsStore store) {
      this.store = store;
   }

   BindingsStore getStore() {
      return this.store;
   }

   @Override
   public int size() {
      return this.store.getRawMap(false).size();
   }

   @Override
   public boolean isEmpty() {
      return this.size() == 0;
   }

   @Override
   public boolean containsKey(Object key) {
      return !(key instanceof String s) ? false : this.store.hasMember(s) && !this.store.isHidden(s);
   }

   @Override
   public boolean containsValue(Object value) {
      for (Entry<String, Object> e : this.store.getRawMap(false).entrySet()) {
         if (Objects.equals(Engine.toJava(e.getValue()), value)) {
            return true;
         }
      }

      return false;
   }

   @Override
   public Object get(Object key) {
      if (key instanceof String s) {
         return this.store.hasMember(s) && !this.store.isHidden(s) ? Engine.toJava(this.store.getMember(s)) : null;
      } else {
         return null;
      }
   }

   public Object put(String key, Object value) {
      Object previous = this.store.hasMember(key) && !this.store.isHidden(key) ? Engine.toJava(this.store.getMember(key)) : null;
      this.store.putMember(key, value);
      return previous;
   }

   @Override
   public Object remove(Object key) {
      if (key instanceof String s) {
         if (this.store.hasMember(s) && !this.store.isHidden(s)) {
            Object previous = Engine.toJava(this.store.getMember(s));
            this.store.remove(s);
            return previous;
         } else {
            return null;
         }
      } else {
         return null;
      }
   }

   @Override
   public void putAll(Map<? extends String, ?> m) {
      for (Entry<? extends String, ?> e : m.entrySet()) {
         this.store.putMember(e.getKey(), e.getValue());
      }
   }

   @Override
   public void clear() {
      this.store.clear();
   }

   @Override
   public Set<String> keySet() {
      return this.store.getRawMap(false).keySet();
   }

   @Override
   public Collection<Object> values() {
      Map<String, Object> raw = this.store.getRawMap(false);
      List<Object> list = new ArrayList<>(raw.size());

      for (Object v : raw.values()) {
         list.add(Engine.toJava(v));
      }

      return list;
   }

   @Override
   public Set<Entry<String, Object>> entrySet() {
      return JsObject.getEntries(this.store.getRawMap(false));
   }

   @Override
   public boolean equals(Object o) {
      return this == o;
   }

   @Override
   public int hashCode() {
      return System.identityHashCode(this);
   }

   @Override
   public String toString() {
      return this.store.getRawMap(false).toString();
   }
}
