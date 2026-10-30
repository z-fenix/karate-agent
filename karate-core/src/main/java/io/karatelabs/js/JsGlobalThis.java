package io.karatelabs.js;

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Map.Entry;

final class JsGlobalThis extends JsObject {
   private final ContextRoot root;

   JsGlobalThis(ContextRoot root) {
      this.root = root;
   }

   private BindingsStore bindings() {
      return this.root.getEngine().bindings;
   }

   @Override
   public Object getMember(String name) {
      if ("__proto__".equals(name)) {
         return this.getPrototype();
      } else {
         BindingsStore b = this.bindings();
         if (b.isTombstoned(name)) {
            ObjectLike proto = this.getPrototype();
            return proto != null ? proto.getMember(name) : null;
         } else if (b.hasMember(name)) {
            return b.getMember(name);
         } else if (this.root.hasKey(name)) {
            return this.root.get(name);
         } else {
            Object own = super.getMember(name);
            if (own != null) {
               return own;
            } else {
               ObjectLike proto = this.getPrototype();
               return proto != null ? proto.getMember(name) : null;
            }
         }
      }
   }

   @Override
   public Object getMember(String name, Object receiver, CoreContext ctx) {
      if ("__proto__".equals(name)) {
         return this.getPrototype();
      } else {
         BindingsStore b = this.bindings();
         if (b.isTombstoned(name)) {
            ObjectLike proto = this.getPrototype();
            return proto != null ? proto.getMember(name, receiver, ctx) : null;
         } else if (b.hasMember(name)) {
            return b.getMember(name);
         } else if (this.root.hasKey(name)) {
            return this.root.get(name);
         } else {
            Object own = super.getMember(name, receiver, ctx);
            if (own != null) {
               return own;
            } else {
               ObjectLike proto = this.getPrototype();
               return proto != null ? proto.getMember(name, receiver, ctx) : null;
            }
         }
      }
   }

   @Override
   public boolean isOwnProperty(String name) {
      BindingsStore b = this.bindings();
      if (b.isTombstoned(name)) {
         return false;
      } else {
         return !b.hasMember(name) && !this.root.hasKey(name) ? super.isOwnProperty(name) : true;
      }
   }

   @Override
   public void putMember(String name, Object value) {
      this.putMember(name, value, null, false);
   }

   @Override
   public void putMember(String name, Object value, CoreContext ctx, boolean strict) {
      if ("__proto__".equals(name)) {
         super.putMember(name, value);
      } else {
         BindingSlot s = this.bindings().getSlot(name);
         if (s != null && s.attrsExplicit && (s.attrs & 1) == 0) {
            if (strict) {
               JsObject.failReadOnly(name);
            }
         } else {
            this.bindings().clearTombstone(name);
            this.bindings().putMember(name, value);
         }
      }
   }

   @Override
   void defineOwn(String name, Object value, byte attrs) {
      BindingsStore b = this.bindings();
      b.clearTombstone(name);
      b.putMember(name, value);
      BindingSlot s = b.getSlot(name);
      s.attrs = attrs;
      s.attrsExplicit = true;
   }

   @Override
   public void removeMember(String name) {
      this.removeMember(name, null, false);
   }

   @Override
   public void removeMember(String name, CoreContext ctx, boolean strict) {
      BindingsStore b = this.bindings();
      BindingSlot s = b.getSlot(name);
      if (s == null) {
         if (this.root.hasKey(name)) {
            b.tombstone(name);
         }
      } else {
         byte attrs = s.attrsExplicit ? s.attrs : 5;
         if ((attrs & 4) == 0) {
            if (strict) {
               JsObject.failNotConfigurable(name);
            }
         } else {
            if (this.root.hasKey(name)) {
               b.tombstone(name);
            } else {
               b.remove(name);
            }
         }
      }
   }

   @Override
   public byte getOwnAttrs(String name) {
      BindingSlot s = this.bindings().getSlot(name);
      if (s != null && !s.tombstoned && s.attrsExplicit) {
         return s.attrs;
      } else {
         return (s == null || s.tombstoned) && !this.root.hasKey(name) ? super.getOwnAttrs(name) : 5;
      }
   }

   @Override
   public Iterable<KeyValue> jsEntries(CoreContext ctx) {
      return this.jsEntries();
   }

   @Override
   public Iterable<KeyValue> jsEntries() {
      return () -> new Iterator<KeyValue>() {
         final Iterator<Entry<String, Object>> source = JsGlobalThis.this.bindings().getRawMap().entrySet().iterator();
         int index = 0;
         Entry<String, Object> peeked = null;

         private boolean advance() {
            while (this.source.hasNext()) {
               Entry<String, Object> e = this.source.next();
               if (!JsGlobalThis.this.bindings().isTombstoned(e.getKey()) && JsGlobalThis.this.isEnumerable(e.getKey())) {
                  this.peeked = e;
                  return true;
               }
            }

            this.peeked = null;
            return false;
         }

         @Override
         public boolean hasNext() {
            return this.peeked != null || this.advance();
         }

         public KeyValue next() {
            if (this.peeked == null && !this.advance()) {
               throw new NoSuchElementException();
            } else {
               Entry<String, Object> entry = this.peeked;
               this.peeked = null;
               return new KeyValue(JsGlobalThis.this, this.index++, entry.getKey(), entry.getValue());
            }
         }
      };
   }

   @Override
   public Map<String, Object> toMap() {
      Map<String, Object> raw = this.bindings().getRawMap();
      Map<String, Object> propsMap = super.toMap();
      if (propsMap.isEmpty()) {
         return raw.isEmpty() ? Collections.emptyMap() : raw;
      } else {
         Map<String, Object> merged = new LinkedHashMap<>(raw.size() + propsMap.size());
         merged.putAll(raw);
         merged.putAll(propsMap);
         return merged;
      }
   }

   @Override
   public int size() {
      return this.toMap().size();
   }

   @Override
   public boolean isEmpty() {
      return this.toMap().isEmpty();
   }

   @Override
   public boolean containsKey(Object key) {
      if (key instanceof String s) {
         BindingsStore b = this.bindings();
         return b.isTombstoned(s) ? false : b.hasMember(s) || this.root.hasKey(s);
      } else {
         return false;
      }
   }

   @Override
   public Object get(Object key) {
      if (key instanceof String s) {
         BindingsStore b = this.bindings();
         if (b.isTombstoned(s)) {
            return null;
         } else if (b.hasMember(s)) {
            return b.getMember(s);
         } else {
            return this.root.hasKey(s) ? this.root.get(s) : null;
         }
      } else {
         return null;
      }
   }
}
