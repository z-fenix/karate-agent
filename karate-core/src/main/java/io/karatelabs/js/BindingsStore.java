package io.karatelabs.js;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

class BindingsStore {
   private final Map<String, BindingSlot> map;
   private final boolean immutable;
   private BindingSlot[] levelHeads;

   BindingsStore() {
      this.map = new HashMap<>();
      this.immutable = false;
   }

   BindingsStore(Map<String, Object> source) {
      this.map = new HashMap<>();
      this.immutable = false;
      if (source != null) {
         for (Entry<String, Object> e : source.entrySet()) {
            this.map.put(e.getKey(), new BindingSlot(e.getKey(), e.getValue()));
         }
      }
   }

   BindingsStore(BindingsStore other) {
      this.map = new HashMap<>();
      this.immutable = false;

      for (Entry<String, BindingSlot> e : other.map.entrySet()) {
         BindingSlot copy = new BindingSlot(e.getValue());
         this.map.put(e.getKey(), copy);
         this.linkLevel(copy, copy.level);
      }
   }

   private BindingsStore(Map<String, BindingSlot> map, boolean immutable) {
      this.map = map;
      this.immutable = immutable;
   }

   static BindingsStore captured(Map<String, BindingSlot> snapshot) {
      return new BindingsStore(snapshot, true);
   }

   Object getMember(String key) {
      BindingSlot s = this.map.get(key);
      return s != null ? s.value : null;
   }

   boolean hasMember(String key) {
      return this.map.containsKey(key);
   }

   BindingSlot getSlot(String key) {
      return this.map.get(key);
   }

   boolean isHidden(String key) {
      BindingSlot s = this.map.get(key);
      return s != null && s.hidden;
   }

   Map<String, Object> getRawMap() {
      Map<String, Object> result = new HashMap<>(this.map.size());

      for (Entry<String, BindingSlot> e : this.map.entrySet()) {
         result.put(e.getKey(), e.getValue().value);
      }

      return result;
   }

   Map<String, Object> getRawMap(boolean hidden) {
      Map<String, Object> result = new HashMap<>(this.map.size());

      for (Entry<String, BindingSlot> e : this.map.entrySet()) {
         if (e.getValue().hidden == hidden) {
            result.put(e.getKey(), e.getValue().value);
         }
      }

      return result;
   }

   void putMember(String key, Object value) {
      this.putMember(key, value, null, true);
   }

   void putMember(String key, Object value, BindScope scope, boolean initialized) {
      BindingSlot existing = this.map.get(key);
      if (existing != null) {
         existing.value = value;
         if (scope != null) {
            existing.scope = scope;
            existing.initialized = initialized;
         }
      } else {
         if (this.immutable) {
            return;
         }

         if (scope != null) {
            this.map.put(key, new BindingSlot(key, value, scope, initialized));
         } else {
            this.map.put(key, new BindingSlot(key, value));
         }
      }
   }

   void putHidden(String key, Object value) {
      if (!this.immutable) {
         BindingSlot existing = this.map.get(key);
         if (existing != null) {
            existing.value = value;
            existing.hidden = true;
         } else {
            BindingSlot s = new BindingSlot(key, value);
            s.hidden = true;
            this.map.put(key, s);
         }
      }
   }

   void remove(String key) {
      if (!this.immutable) {
         this.map.remove(key);
      }
   }

   void tombstone(String key) {
      if (!this.immutable) {
         BindingSlot s = this.map.get(key);
         if (s == null) {
            s = new BindingSlot(key);
            this.map.put(key, s);
         }

         s.value = null;
         s.tombstoned = true;
      }
   }

   boolean isTombstoned(String key) {
      BindingSlot s = this.map.get(key);
      return s != null && s.tombstoned;
   }

   void clearTombstone(String key) {
      BindingSlot s = this.map.get(key);
      if (s != null && s.tombstoned) {
         s.tombstoned = false;
      }
   }

   void clearBindingScope(String key) {
      BindingSlot s = this.map.get(key);
      if (s != null) {
         s.scope = null;
         s.initialized = true;
      }
   }

   void pushBinding(String key, Object value, BindScope scope, int level) {
      if (!this.immutable) {
         BindingSlot existing = this.map.get(key);
         BindingSlot newSlot = new BindingSlot(key, value, scope, true, level, existing);
         this.map.put(key, newSlot);
         this.linkLevel(newSlot, level);
      }
   }

   void pushBinding(String key, Object value, BindScope scope, int level, boolean initialized) {
      if (!this.immutable) {
         BindingSlot existing = this.map.get(key);
         BindingSlot newSlot = new BindingSlot(key, value, scope, initialized, level, existing);
         this.map.put(key, newSlot);
         this.linkLevel(newSlot, level);
      }
   }

   private void linkLevel(BindingSlot s, int level) {
      if (level > 0) {
         if (this.levelHeads == null) {
            this.levelHeads = new BindingSlot[Math.max(8, level + 1)];
         } else if (level >= this.levelHeads.length) {
            this.levelHeads = Arrays.copyOf(this.levelHeads, Math.max(this.levelHeads.length * 2, level + 1));
         }

         s.nextInLevel = this.levelHeads[level];
         this.levelHeads[level] = s;
      }
   }

   void popLevel(int level) {
      if (!this.immutable && this.levelHeads != null && level < this.levelHeads.length) {
         BindingSlot s = this.levelHeads[level];
         this.levelHeads[level] = null;

         while (s != null) {
            BindingSlot next = s.nextInLevel;
            s.nextInLevel = null;
            if (s.deferredPop) {
               s.deferredPop = false;
               s.nextInLevel = this.levelHeads[level];
               this.levelHeads[level] = s;
            } else if (this.map.get(s.name) == s) {
               if (s.previous != null) {
                  this.map.put(s.name, s.previous);
                  if (s.previous.level == level) {
                     s.previous.deferredPop = true;
                  }
               } else {
                  this.map.remove(s.name);
               }
            }

            s = next;
         }
      }
   }

   boolean isEmpty() {
      return this.map.isEmpty();
   }

   Iterable<String> keys() {
      return this.map.keySet();
   }

   void clear() {
      this.map.clear();
      this.levelHeads = null;
   }
}
