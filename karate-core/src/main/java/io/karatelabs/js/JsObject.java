package io.karatelabs.js;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.AbstractMap.SimpleEntry;
import java.util.Map.Entry;

class JsObject implements ObjectLike, Map<String, Object> {
   static final byte WRITABLE = 1;
   static final byte ENUMERABLE = 2;
   static final byte CONFIGURABLE = 4;
   static final byte ATTRS_DEFAULT = 7;
   private Map<String, PropertySlot> props;
   private Map<PrivateName, Object> privates;
   private Map<JsSymbol, PropertySlot> symbols;
   private ObjectLike __proto__ = JsObjectPrototype.INSTANCE;
   private boolean nonExtensible;
   private boolean sealed;
   private boolean frozen;

   JsObject() {
   }

   JsObject(Map<String, Object> seed) {
      if (seed != null && !seed.isEmpty()) {
         this.props = new LinkedHashMap<>(seed.size());

         for (Entry<String, Object> e : seed.entrySet()) {
            this.props.put(e.getKey(), new DataSlot(e.getKey(), e.getValue()));
         }
      }
   }

   protected JsObject(Map<String, Object> seed, ObjectLike proto) {
      this(seed);
      this.__proto__ = proto;
   }

   @Override
   public ObjectLike getPrototype() {
      return this.__proto__;
   }

   public void setPrototype(ObjectLike proto) {
      this.__proto__ = proto;
   }

   boolean isJsFunction() {
      return false;
   }

   public boolean hasOwnIntrinsic(String name) {
      return this.resolveOwnIntrinsic(name) != null;
   }

   boolean hasPrivate(PrivateName pn) {
      return this.privates != null && this.privates.containsKey(pn);
   }

   Object getPrivate(PrivateName pn) {
      return this.privates == null ? null : this.privates.get(pn);
   }

   void putPrivate(PrivateName pn, Object value) {
      if (this.privates == null) {
         this.privates = new HashMap<>(4);
      }

      this.privates.put(pn, value);
   }

   boolean hasSymbol(JsSymbol sym) {
      return this.ownSymbolSlot(sym) != null;
   }

   PropertySlot ownSymbolSlot(JsSymbol sym) {
      PropertySlot s = this.symbols == null ? null : this.symbols.get(sym);
      return s != null && !s.tombstoned ? s : null;
   }

   Object getSymbol(JsSymbol sym, CoreContext ctx) {
      PropertySlot s = this.ownSymbolSlot(sym);
      return s == null ? Terms.UNDEFINED : s.read(this, ctx);
   }

   Object getSymbolMember(JsSymbol sym, Object receiver, CoreContext ctx) {
      for (ObjectLike o = this; o != null; o = o.getPrototype()) {
         if (o instanceof JsObject jo) {
            PropertySlot s = jo.ownSymbolSlot(sym);
            if (s != null) {
               return s.read(receiver, ctx);
            }
         }
      }

      return Terms.UNDEFINED;
   }

   void setSymbol(JsSymbol sym, Object value, CoreContext ctx, boolean strict) {
      for (ObjectLike o = this; o != null; o = o.getPrototype()) {
         if (o instanceof JsObject jo) {
            PropertySlot s = jo.ownSymbolSlot(sym);
            if (s != null) {
               if (s instanceof AccessorSlot acc) {
                  acc.write(this, value, ctx, strict);
                  return;
               }

               if (!s.isWritable()) {
                  if (strict) {
                     throw JsErrorException.typeError("Cannot assign to read only property '" + sym + "'");
                  }

                  return;
               }

               if (jo == this) {
                  s.write(this, value, ctx, strict);
                  return;
               }
               break;
            }
         }
      }

      if (!this.frozen && !this.sealed && !this.nonExtensible) {
         this.defineOwnSymbol(sym, value, (byte)7);
      } else if (strict) {
         throw JsErrorException.typeError("Cannot add property " + sym + ", object is not extensible");
      }
   }

   void defineOwnSymbol(JsSymbol sym, Object value, byte attrs) {
      if (this.symbols == null) {
         this.symbols = new LinkedHashMap<>(4);
      }

      PropertySlot existing = this.symbols.get(sym);
      DataSlot s;
      if (existing instanceof DataSlot ds) {
         s = ds;
      } else {
         s = new DataSlot(sym.toString());
         this.symbols.put(sym, s);
      }

      s.value = value;
      s.attrs = attrs;
      s.tombstoned = false;
   }

   void defineOwnSymbolAccessor(JsSymbol sym, JsCallable getter, JsCallable setter, byte attrs) {
      if (this.symbols == null) {
         this.symbols = new LinkedHashMap<>(4);
      }

      PropertySlot existing = this.symbols.get(sym);
      AccessorSlot s;
      if (existing instanceof AccessorSlot as) {
         s = as;
      } else {
         s = new AccessorSlot(sym.toString());
         this.symbols.put(sym, s);
      }

      s.getter = getter;
      s.setter = setter;
      s.attrs = attrs;
      s.tombstoned = false;
   }

   boolean removeSymbol(JsSymbol sym, boolean strict) {
      PropertySlot s = this.ownSymbolSlot(sym);
      if (s == null) {
         return true;
      } else if (!s.isConfigurable()) {
         if (strict) {
            throw JsErrorException.typeError("Cannot delete property '" + sym + "'");
         } else {
            return false;
         }
      } else {
         this.symbols.remove(sym);
         return true;
      }
   }

   List<JsSymbol> ownEnumerableSymbols() {
      if (this.symbols == null) {
         return List.of();
      } else {
         List<JsSymbol> out = new ArrayList<>(this.symbols.size());

         for (Entry<JsSymbol, PropertySlot> e : this.symbols.entrySet()) {
            PropertySlot slot = e.getValue();
            if (!slot.tombstoned && slot.isEnumerable()) {
               out.add(e.getKey());
            }
         }

         return out;
      }
   }

   byte ownSymbolAttrs(JsSymbol sym) {
      PropertySlot s = this.ownSymbolSlot(sym);
      return s == null ? 0 : s.attrs;
   }

   List<JsSymbol> ownSymbols() {
      if (this.symbols == null) {
         return List.of();
      } else {
         List<JsSymbol> out = new ArrayList<>(this.symbols.size());

         for (Entry<JsSymbol, PropertySlot> e : this.symbols.entrySet()) {
            if (!e.getValue().tombstoned) {
               out.add(e.getKey());
            }
         }

         return out;
      }
   }

   @Override
   public Object getMember(String name) {
      PropertySlot s = this.props == null ? null : this.props.get(name);
      if (s != null) {
         if (s.tombstoned) {
            return this.__proto__ != null ? this.__proto__.getMember(name) : null;
         } else {
            return s instanceof DataSlot ds ? ds.value : null;
         }
      } else if ("__proto__".equals(name)) {
         return this.__proto__;
      } else {
         Object intrinsic = this.resolveOwnIntrinsic(name);
         if (intrinsic != null) {
            return intrinsic;
         } else {
            return this.__proto__ != null ? this.__proto__.getMember(name) : null;
         }
      }
   }

   @Override
   public Object getMember(String name, Object receiver, CoreContext ctx) {
      PropertySlot s = this.props == null ? null : this.props.get(name);
      if (s != null) {
         if (s.tombstoned) {
            return this.__proto__ != null ? this.__proto__.getMember(name, receiver, ctx) : null;
         } else {
            return s.read(receiver, ctx);
         }
      } else if ("__proto__".equals(name)) {
         return this.__proto__;
      } else {
         Object intrinsic = this.resolveOwnIntrinsic(name);
         if (intrinsic != null) {
            return intrinsic;
         } else {
            return this.__proto__ != null ? this.__proto__.getMember(name, receiver, ctx) : null;
         }
      }
   }

   protected Object resolveOwnIntrinsic(String name) {
      return null;
   }

   protected Iterable<String> ownIntrinsicNames() {
      return Collections.emptyList();
   }

   PropertySlot getOwnSlot(String name) {
      PropertySlot s = this.props == null ? null : this.props.get(name);
      return s != null && !s.tombstoned ? s : null;
   }

   @Override
   public boolean isOwnProperty(String name) {
      PropertySlot s = this.props == null ? null : this.props.get(name);
      return s != null ? !s.tombstoned : this.hasOwnIntrinsic(name);
   }

   boolean isTombstoned(String name) {
      PropertySlot s = this.props == null ? null : this.props.get(name);
      return s != null && s.tombstoned;
   }

   void clearTombstone(String name) {
      PropertySlot s = this.props == null ? null : this.props.get(name);
      if (s != null && s.tombstoned) {
         this.props.remove(name);
      }
   }

   boolean ownContainsKey(String name) {
      PropertySlot s = this.props == null ? null : this.props.get(name);
      return s != null && !s.tombstoned;
   }

   static void failReadOnly(String name) {
      throw JsErrorException.typeError("Cannot assign to read only property '" + name + "'");
   }

   static void failNotExtensible(String name) {
      throw JsErrorException.typeError("Cannot add property " + name + ", object is not extensible");
   }

   static void failNotConfigurable(String name) {
      throw JsErrorException.typeError("Cannot delete property '" + name + "' of [object Object]");
   }

   @Override
   public void putMember(String name, Object value) {
      this.putMember(name, value, null, false);
   }

   @Override
   public void putMember(String name, Object value, CoreContext ctx, boolean strict) {
      if ("__proto__".equals(name)) {
         if (value instanceof ObjectLike proto) {
            this.__proto__ = proto;
         } else if (value == null) {
            this.__proto__ = null;
         }
      } else if (this.frozen) {
         if (strict) {
            failReadOnly(name);
         }
      } else {
         PropertySlot s = this.props == null ? null : this.props.get(name);
         boolean keyExists = s != null && !s.tombstoned;
         boolean intrinsic = !keyExists && this.hasOwnIntrinsic(name);
         if (this.nonExtensible && !keyExists && !intrinsic) {
            if (strict) {
               failNotExtensible(name);
            }
         } else if (keyExists && s instanceof AccessorSlot acc) {
            acc.write(this, value, ctx, strict);
         } else if (keyExists && !s.isWritable()) {
            if (strict) {
               failReadOnly(name);
            }
         } else if (intrinsic && (this.getOwnAttrs(name) & 1) == 0) {
            if (strict) {
               failReadOnly(name);
            }
         } else {
            if (s == null) {
               if (this.props == null) {
                  this.props = new LinkedHashMap<>();
               }

               this.props.put(name, new DataSlot(name, value));
            } else {
               ((DataSlot)s).value = value;
               s.tombstoned = false;
            }
         }
      }
   }

   byte getAttrs(String name) {
      PropertySlot s = this.props == null ? null : this.props.get(name);
      return s == null ? 7 : s.attrs;
   }

   public byte getOwnAttrs(String name) {
      return this.getAttrs(name);
   }

   boolean hasExplicitAttrs(String name) {
      PropertySlot s = this.props == null ? null : this.props.get(name);
      return s != null && s.attrs != 7;
   }

   void setAttrs(String name, byte attrs) {
      PropertySlot s = this.props == null ? null : this.props.get(name);
      if (s == null) {
         if (attrs == 7) {
            return;
         }

         if (this.props == null) {
            this.props = new LinkedHashMap<>();
         }

         s = new DataSlot(name);
         this.props.put(name, s);
      }

      s.attrs = attrs;
   }

   boolean isWritable(String name) {
      return (this.getAttrs(name) & 1) != 0;
   }

   boolean isEnumerable(String name) {
      return (this.getOwnAttrs(name) & 2) != 0;
   }

   boolean isConfigurable(String name) {
      return (this.getAttrs(name) & 4) != 0;
   }

   void defineOwn(String name, Object value, byte attrs) {
      if (this.props == null) {
         this.props = new LinkedHashMap<>();
      }

      PropertySlot existing = this.props.get(name);
      DataSlot s;
      if (existing instanceof DataSlot ds) {
         s = ds;
      } else {
         s = new DataSlot(name);
         this.props.put(name, s);
      }

      s.value = value;
      s.attrs = attrs;
      s.tombstoned = false;
   }

   void defineOwnAccessor(String name, JsCallable getter, JsCallable setter, byte attrs) {
      if (this.props == null) {
         this.props = new LinkedHashMap<>();
      }

      PropertySlot existing = this.props.get(name);
      AccessorSlot s;
      if (existing instanceof AccessorSlot as) {
         s = as;
      } else {
         s = new AccessorSlot(name);
         this.props.put(name, s);
      }

      s.getter = getter;
      s.setter = setter;
      s.attrs = attrs;
      s.tombstoned = false;
   }

   @Override
   public boolean isExtensible() {
      return !this.nonExtensible;
   }

   @Override
   public boolean isSealed() {
      return this.sealed || this.frozen;
   }

   @Override
   public boolean isFrozen() {
      return this.frozen;
   }

   @Override
   public void setExtensible(boolean extensible) {
      if (!extensible) {
         this.nonExtensible = true;
      }
   }

   @Override
   public void setSealed(boolean sealed) {
      if (sealed) {
         this.nonExtensible = true;
         this.sealed = true;
         if (this.props != null) {
            for (PropertySlot s : this.props.values()) {
               if (!s.tombstoned) {
                  s.attrs &= -5;
               }
            }
         }

         if (this.symbols != null) {
            for (PropertySlot sx : this.symbols.values()) {
               if (!sx.tombstoned) {
                  sx.attrs &= -5;
               }
            }
         }
      }
   }

   @Override
   public void setFrozen(boolean frozen) {
      if (frozen) {
         this.nonExtensible = true;
         this.sealed = true;
         this.frozen = true;
         if (this.props != null) {
            for (PropertySlot s : this.props.values()) {
               if (!s.tombstoned) {
                  s.attrs &= -5;
                  if (s instanceof DataSlot) {
                     s.attrs &= -2;
                  }
               }
            }
         }

         if (this.symbols != null) {
            for (PropertySlot sx : this.symbols.values()) {
               if (!sx.tombstoned) {
                  sx.attrs &= -5;
                  if (sx instanceof DataSlot) {
                     sx.attrs &= -2;
                  }
               }
            }
         }
      }
   }

   protected static JsBuiltinMethod method(String methodName, int length, JsCallable delegate) {
      return new JsBuiltinMethod(methodName, length, delegate);
   }

   @Override
   public void removeMember(String name) {
      this.removeMember(name, null, false);
   }

   @Override
   public void removeMember(String name, CoreContext ctx, boolean strict) {
      PropertySlot s = this.props == null ? null : this.props.get(name);
      if (s == null || !s.tombstoned) {
         boolean inMap = s != null;
         boolean intrinsic = this.hasOwnIntrinsic(name);
         if (inMap || intrinsic) {
            byte attrs = inMap ? s.attrs : this.getOwnAttrs(name);
            if ((attrs & 4) == 0) {
               if (strict) {
                  failNotConfigurable(name);
               }
            } else {
               if (intrinsic) {
                  DataSlot ds;
                  if (s instanceof DataSlot existing) {
                     ds = existing;
                  } else {
                     if (this.props == null) {
                        this.props = new LinkedHashMap<>();
                     }

                     ds = new DataSlot(name);
                     this.props.put(name, ds);
                  }

                  ds.value = null;
                  ds.attrs = 7;
                  ds.tombstoned = true;
               } else {
                  this.props.remove(name);
               }
            }
         }
      }
   }

   @Override
   public Map<String, Object> toMap() {
      if (this.props != null && !this.props.isEmpty()) {
         Map<String, Object> view = new LinkedHashMap<>(this.props.size());

         for (PropertySlot s : this.props.values()) {
            if (!s.tombstoned) {
               view.put(s.name, s instanceof DataSlot ds ? ds.value : null);
            }
         }

         return view;
      } else {
         return Collections.emptyMap();
      }
   }

   @Override
   public int size() {
      if (this.props == null) {
         return 0;
      } else {
         int n = 0;

         for (PropertySlot s : this.props.values()) {
            if (!s.tombstoned) {
               n++;
            }
         }

         return n;
      }
   }

   @Override
   public boolean isEmpty() {
      return this.size() == 0;
   }

   @Override
   public boolean containsKey(Object key) {
      return key instanceof String name ? this.ownContainsKey(name) : false;
   }

   @Override
   public boolean containsValue(Object value) {
      if (this.props == null) {
         return false;
      } else {
         for (PropertySlot s : this.props.values()) {
            if (!s.tombstoned && s instanceof DataSlot ds) {
               Object unwrapped = Engine.toJava(ds.value);
               if (Objects.equals(unwrapped, value)) {
                  return true;
               }
            }
         }

         return false;
      }
   }

   @Override
   public Object get(Object key) {
      PropertySlot s = this.props != null && key instanceof String name ? this.props.get(name) : null;
      if (s != null && !s.tombstoned) {
         return s instanceof DataSlot ds ? Engine.toJava(ds.value) : null;
      } else {
         return null;
      }
   }

   public Object put(String key, Object value) {
      if (this.props == null) {
         this.props = new LinkedHashMap<>();
      }

      PropertySlot s = this.props.get(key);
      Object previous = null;
      if (s instanceof DataSlot ds && !ds.tombstoned) {
         previous = ds.value;
      }

      if (s instanceof DataSlot ds) {
         ds.value = value;
         ds.tombstoned = false;
      } else {
         this.props.put(key, new DataSlot(key, value));
      }

      return Engine.toJava(previous);
   }

   @Override
   public Object remove(Object key) {
      if (this.props != null && key instanceof String name) {
         PropertySlot s = this.props.remove(name);
         if (s != null && !s.tombstoned) {
            return s instanceof DataSlot ds ? Engine.toJava(ds.value) : null;
         } else {
            return null;
         }
      } else {
         return null;
      }
   }

   @Override
   public void putAll(Map<? extends String, ?> m) {
      if (this.props == null) {
         this.props = new LinkedHashMap<>();
      }

      for (Entry<? extends String, ?> e : m.entrySet()) {
         this.put(e.getKey(), e.getValue());
      }
   }

   @Override
   public void clear() {
      if (this.props != null) {
         this.props.clear();
      }
   }

   @Override
   public Set<String> keySet() {
      if (this.props != null && !this.props.isEmpty()) {
         Set<String> keys = new LinkedHashSet<>(this.props.size());

         for (PropertySlot s : this.props.values()) {
            if (!s.tombstoned) {
               keys.add(s.name);
            }
         }

         return keys;
      } else {
         return Collections.emptySet();
      }
   }

   @Override
   public Collection<Object> values() {
      if (this.props != null && !this.props.isEmpty()) {
         List<Object> unwrapped = new ArrayList<>(this.props.size());

         for (PropertySlot s : this.props.values()) {
            if (!s.tombstoned) {
               unwrapped.add(s instanceof DataSlot ds ? Engine.toJava(ds.value) : null);
            }
         }

         return unwrapped;
      } else {
         return Collections.emptyList();
      }
   }

   @Override
   public Set<Entry<String, Object>> entrySet() {
      if (this.props != null && !this.props.isEmpty()) {
         Set<Entry<String, Object>> out = new LinkedHashSet<>(this.props.size());

         for (PropertySlot s : this.props.values()) {
            if (!s.tombstoned) {
               Object v = s instanceof DataSlot ds ? Engine.toJava(ds.value) : null;
               out.add(new SimpleEntry<>(s.name, v));
            }
         }

         return out;
      } else {
         return Collections.emptySet();
      }
   }

   static Set<Entry<String, Object>> getEntries(Map<String, Object> map) {
      Map<String, Object> unwrapped = new LinkedHashMap<>();

      for (Entry<String, Object> entry : map.entrySet()) {
         unwrapped.put(entry.getKey(), Engine.toJava(entry.getValue()));
      }

      return unwrapped.entrySet();
   }

   public Iterable<KeyValue> jsEntries() {
      return this.jsEntries(null);
   }

   public static Set<String> orderedOwnKeys(Set<String> insertionOrder) {
      if (insertionOrder != null && insertionOrder.size() >= 2) {
         TreeMap<Integer, String> intKeys = null;
         List<String> stringKeys = null;

         for (String k : insertionOrder) {
            int idx = JsArray.parseIndex(k);
            if (idx >= 0) {
               if (intKeys == null) {
                  intKeys = new TreeMap<>();
               }

               intKeys.put(idx, k);
            } else {
               if (stringKeys == null) {
                  stringKeys = new ArrayList<>(insertionOrder.size());
               }

               stringKeys.add(k);
            }
         }

         if (intKeys == null) {
            return insertionOrder;
         } else {
            LinkedHashSet<String> ordered = new LinkedHashSet<>(insertionOrder.size());
            ordered.addAll(intKeys.values());
            if (stringKeys != null) {
               ordered.addAll(stringKeys);
            }

            return ordered;
         }
      } else {
         return insertionOrder;
      }
   }

   public Iterable<KeyValue> jsEntries(CoreContext ctx) {
      return () -> new Iterator<KeyValue>() {
         final Iterator<PropertySlot> source = JsObject.this.orderedSlotsForIteration().iterator();
         int index = 0;
         PropertySlot peeked = null;

         private boolean advance() {
            while (this.source.hasNext()) {
               PropertySlot s = this.source.next();
               if (!s.tombstoned && JsObject.this.isEnumerable(s.name)) {
                  this.peeked = s;
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
               PropertySlot s = this.peeked;
               this.peeked = null;
               Object v = ctx != null ? s.read(JsObject.this, ctx) : (s instanceof DataSlot ds ? ds.value : null);
               return new KeyValue(JsObject.this, this.index++, s.name, v);
            }
         }
      };
   }

   private List<PropertySlot> orderedSlotsForIteration() {
      if (this.props != null && !this.props.isEmpty()) {
         TreeMap<Integer, PropertySlot> intSlots = null;
         List<PropertySlot> stringSlots = null;

         for (PropertySlot s : this.props.values()) {
            if (!s.tombstoned) {
               int idx = JsArray.parseIndex(s.name);
               if (idx >= 0) {
                  if (intSlots == null) {
                     intSlots = new TreeMap<>();
                  }

                  intSlots.put(idx, s);
               } else {
                  if (stringSlots == null) {
                     stringSlots = new ArrayList<>();
                  }

                  stringSlots.add(s);
               }
            }
         }

         if (intSlots == null) {
            return stringSlots != null ? stringSlots : Collections.emptyList();
         } else {
            List<PropertySlot> out = new ArrayList<>(intSlots.size() + (stringSlots == null ? 0 : stringSlots.size()));
            out.addAll(intSlots.values());
            if (stringSlots != null) {
               out.addAll(stringSlots);
            }

            return out;
         }
      } else {
         return Collections.emptyList();
      }
   }

   @Override
   public int hashCode() {
      return System.identityHashCode(this);
   }

   @Override
   public boolean equals(Object obj) {
      return this == obj;
   }
}
