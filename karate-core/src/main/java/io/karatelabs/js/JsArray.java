package io.karatelabs.js;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

class JsArray implements ObjectLike, JsCallable, List<Object> {
   static final Object HOLE = new Object() {
      @Override
      public String toString() {
         return "<<hole>>";
      }
   };
   final List<Object> list;
   private Map<String, PropertySlot> namedProps;
   private boolean lengthWritable = true;
   private boolean nonExtensible;
   private boolean sealed;
   private boolean frozen;
   private ObjectLike __proto__ = JsArrayPrototype.INSTANCE;
   static final int DENSE_PAD_LIMIT = 10000000;

   public JsArray(List<Object> list) {
      this.list = list;
   }

   public JsArray() {
      this(new ArrayList<>());
   }

   @Override
   public ObjectLike getPrototype() {
      return this.__proto__;
   }

   public void setPrototype(ObjectLike proto) {
      this.__proto__ = proto;
   }

   @Override
   public Object getMember(String name) {
      if (this.namedProps != null) {
         PropertySlot s = this.namedProps.get(name);
         if (s != null) {
            return s instanceof DataSlot ds ? ds.value : null;
         }
      }

      if ("__proto__".equals(name)) {
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
      if (this.namedProps != null) {
         PropertySlot s = this.namedProps.get(name);
         if (s != null) {
            return s.read(receiver, ctx);
         }
      }

      if ("__proto__".equals(name)) {
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
      if ("length".equals(name)) {
         return this.list.size();
      } else {
         if (!name.isEmpty()) {
            char c0 = name.charAt(0);
            if (c0 >= '0' && c0 <= '9') {
               int i = parseIndex(name);
               if (this.hasOwnIndexedSlot(i)) {
                  return this.getElement(i);
               }
            }
         }

         return null;
      }
   }

   PropertySlot getOwnSlot(String name) {
      return this.namedProps == null ? null : this.namedProps.get(name);
   }

   void checkDensePad(long index) {
      if (index - this.list.size() > 10000000L) {
         throw JsErrorException.rangeError("array index too large for dense storage: " + index);
      }
   }

   static int parseIndex(String s) {
      int n = s.length();
      if (n == 0) {
         return -1;
      } else if (n > 10) {
         return -1;
      } else {
         long v = 0L;

         for (int i = 0; i < n; i++) {
            char c = s.charAt(i);
            if (c < '0' || c > '9') {
               return -1;
            }

            v = v * 10L + (c - '0');
         }

         if (n > 1 && s.charAt(0) == '0') {
            return -1;
         } else {
            return v > 2147483647L ? -1 : (int)v;
         }
      }
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
            JsObject.failReadOnly(name);
         }
      } else if ("length".equals(name)) {
         this.handleLengthAssign(value, null);
      } else {
         PropertySlot existing = this.namedProps == null ? null : this.namedProps.get(name);
         if (existing instanceof AccessorSlot acc) {
            acc.write(this, value, ctx, strict);
         } else if (existing != null && !existing.isWritable()) {
            if (strict) {
               JsObject.failReadOnly(name);
            }
         } else {
            int i = parseIndex(name);
            if (i >= 0 && existing == null) {
               boolean indexExists = i < this.list.size() && this.list.get(i) != HOLE;
               if (this.nonExtensible && !indexExists) {
                  if (strict) {
                     JsObject.failNotExtensible(name);
                  }
               } else {
                  this.checkDensePad(i);

                  while (this.list.size() < i) {
                     this.list.add(HOLE);
                  }

                  if (i < this.list.size()) {
                     this.list.set(i, value);
                  } else {
                     this.list.add(value);
                  }
               }
            } else if (existing == null && this.nonExtensible) {
               if (strict) {
                  JsObject.failNotExtensible(name);
               }
            } else {
               if (existing instanceof DataSlot ds) {
                  ds.value = value;
               } else {
                  if (this.namedProps == null) {
                     this.namedProps = new LinkedHashMap<>();
                  }

                  this.namedProps.put(name, new DataSlot(name, value));
               }
            }
         }
      }
   }

   void defineOwn(String name, Object value, byte attrs) {
      if ("length".equals(name)) {
         JsArray.ArrayLength.handleAssign(this, value, null);
         this.setAttrs("length", attrs);
      } else {
         int i = parseIndex(name);
         if (i >= 0) {
            if (this.namedProps != null) {
               this.namedProps.remove(name);
            }

            this.checkDensePad(i);

            while (this.list.size() < i) {
               this.list.add(HOLE);
            }

            if (i < this.list.size()) {
               this.list.set(i, value);
            } else {
               this.list.add(value);
            }

            if (attrs != 7) {
               if (this.namedProps == null) {
                  this.namedProps = new LinkedHashMap<>();
               }

               DataSlot s = new DataSlot(name, value);
               s.attrs = attrs;
               this.namedProps.put(name, s);
            }
         } else {
            if (this.namedProps == null) {
               this.namedProps = new LinkedHashMap<>();
            }

            PropertySlot existing = this.namedProps.get(name);
            DataSlot s;
            if (existing instanceof DataSlot ds) {
               s = ds;
            } else {
               s = new DataSlot(name);
               this.namedProps.put(name, s);
            }

            s.value = value;
            s.attrs = attrs;
         }
      }
   }

   void defineOwnAccessor(String name, JsCallable getter, JsCallable setter, byte attrs) {
      int i = parseIndex(name);
      if (i >= 0) {
         this.checkDensePad(i);

         while (this.list.size() <= i) {
            this.list.add(HOLE);
         }
      }

      if (this.namedProps == null) {
         this.namedProps = new LinkedHashMap<>();
      }

      PropertySlot existing = this.namedProps.get(name);
      AccessorSlot s;
      if (existing instanceof AccessorSlot as) {
         s = as;
      } else {
         s = new AccessorSlot(name);
         this.namedProps.put(name, s);
      }

      s.getter = getter;
      s.setter = setter;
      s.attrs = attrs;
   }

   byte getAttrs(String name) {
      PropertySlot s = this.namedProps == null ? null : this.namedProps.get(name);
      return s == null ? 7 : s.attrs;
   }

   void setAttrs(String name, byte attrs) {
      if ("length".equals(name)) {
         this.lengthWritable = (attrs & 1) != 0;
      } else {
         PropertySlot s = this.namedProps == null ? null : this.namedProps.get(name);
         if (s != null) {
            s.attrs = attrs;
         }
      }
   }

   boolean handleLengthAssign(Object value, CoreContext context) {
      return JsArray.ArrayLength.handleAssign(this, value, context);
   }

   boolean defineLength(int newLen, byte attrs) {
      return JsArray.ArrayLength.defineProperty(this, newLen, attrs);
   }

   static long coerceToUint32(Object value, CoreContext context) {
      return JsArray.ArrayLength.coerceToUint32(value, context);
   }

   @Override
   public void removeMember(String name) {
      this.removeMember(name, null, false);
   }

   @Override
   public void removeMember(String name, CoreContext ctx, boolean strict) {
      PropertySlot s = this.namedProps == null ? null : this.namedProps.get(name);
      int i = parseIndex(name);
      boolean denseSlot = i >= 0 && i < this.list.size() && this.list.get(i) != HOLE;
      if (s != null || denseSlot) {
         byte attrs = s != null ? s.attrs : this.getOwnAttrs(name);
         if ((attrs & 4) == 0) {
            if (strict) {
               JsObject.failNotConfigurable(name);
            }
         } else {
            if (s != null) {
               this.namedProps.remove(name);
            }

            if (denseSlot) {
               this.list.set(i, HOLE);
            }
         }
      }
   }

   @Override
   public Map<String, Object> toMap() {
      Map<String, Object> view = new LinkedHashMap<>(this.list.size() + (this.namedProps == null ? 0 : this.namedProps.size()));

      for (int i = 0; i < this.list.size(); i++) {
         Object v = this.list.get(i);
         view.put(Integer.toString(i), v == HOLE ? null : v);
      }

      if (this.namedProps != null) {
         for (PropertySlot s : this.namedProps.values()) {
            view.put(s.name, s instanceof DataSlot ds ? ds.value : null);
         }
      }

      return view;
   }

   Map<String, Object> namedPropsView() {
      if (this.namedProps != null && !this.namedProps.isEmpty()) {
         Map<String, Object> view = new LinkedHashMap<>(this.namedProps.size());

         for (PropertySlot s : this.namedProps.values()) {
            view.put(s.name, s instanceof DataSlot ds ? ds.value : null);
         }

         return view;
      } else {
         return Collections.emptyMap();
      }
   }

   @Override
   public int size() {
      return this.list.size();
   }

   @Override
   public boolean isEmpty() {
      return this.list.isEmpty();
   }

   @Override
   public boolean contains(Object o) {
      return this.list.contains(o);
   }

   @Override
   public Iterator<Object> iterator() {
      return this.list.iterator();
   }

   @Override
   public Object[] toArray() {
      return this.list.toArray();
   }

   @Override
   public <T> T[] toArray(T[] a) {
      return (T[])this.list.toArray(a);
   }

   @Override
   public boolean add(Object o) {
      return this.list.add(o);
   }

   @Override
   public boolean remove(Object o) {
      return this.list.remove(o);
   }

   @Override
   public boolean containsAll(Collection<?> c) {
      return this.list.containsAll(c);
   }

   @Override
   public boolean addAll(Collection<? extends Object> c) {
      return this.list.addAll(c);
   }

   @Override
   public boolean addAll(int index, Collection<? extends Object> c) {
      return this.list.addAll(index, c);
   }

   @Override
   public boolean removeAll(Collection<?> c) {
      return this.list.removeAll(c);
   }

   @Override
   public boolean retainAll(Collection<?> c) {
      return this.list.retainAll(c);
   }

   @Override
   public void clear() {
      this.list.clear();
   }

   @Override
   public Object get(int index) {
      Object v = this.list.get(index);
      return v == HOLE ? null : Engine.toJava(v);
   }

   @Override
   public Object set(int index, Object element) {
      return this.list.set(index, element);
   }

   @Override
   public void add(int index, Object element) {
      this.list.add(index, element);
   }

   @Override
   public Object remove(int index) {
      return this.list.remove(index);
   }

   @Override
   public int indexOf(Object o) {
      return this.list.indexOf(o);
   }

   @Override
   public int lastIndexOf(Object o) {
      return this.list.lastIndexOf(o);
   }

   @Override
   public ListIterator<Object> listIterator() {
      return this.list.listIterator();
   }

   @Override
   public ListIterator<Object> listIterator(int index) {
      return this.list.listIterator(index);
   }

   @Override
   public List<Object> subList(int fromIndex, int toIndex) {
      return new JsArray(new ArrayList<>(this.list.subList(fromIndex, toIndex)));
   }

   static Object unwrapHole(Object v) {
      return v == HOLE ? Terms.UNDEFINED : v;
   }

   public Object getElement(int index) {
      return index >= 0 && index < this.list.size() ? unwrapHole(this.list.get(index)) : Terms.UNDEFINED;
   }

   Object getIndexedValue(int index, Object receiver, CoreContext ctx) {
      if (this.namedProps != null && index >= 0) {
         PropertySlot s = this.namedProps.get(Integer.toString(index));
         if (s != null) {
            return s.read(receiver, ctx);
         }
      }

      if (this.hasOwnIndexedSlot(index)) {
         return this.getElement(index);
      } else {
         if (this.__proto__ != null && index >= 0) {
            Object inherited = this.__proto__.getMember(Integer.toString(index), receiver, ctx);
            if (inherited != null) {
               return inherited;
            }
         }

         return Terms.UNDEFINED;
      }
   }

   boolean hasIndexedDescriptor(int index) {
      return index >= 0 && this.namedProps != null ? this.namedProps.containsKey(Integer.toString(index)) : false;
   }

   boolean hasAnyDescriptor() {
      return this.namedProps != null && !this.namedProps.isEmpty();
   }

   boolean denseAppendable() {
      return this.lengthWritable && !this.nonExtensible && !this.sealed && !this.frozen;
   }

   @Override
   public boolean isOwnProperty(String name) {
      if ("length".equals(name)) {
         return true;
      } else {
         return this.namedProps != null && this.namedProps.containsKey(name) ? true : this.hasOwnIndexedSlot(parseIndex(name));
      }
   }

   boolean hasOwnIndexedSlot(int index) {
      return index >= 0 && index < this.list.size() && this.list.get(index) != HOLE;
   }

   public byte getOwnAttrs(String name) {
      if ("length".equals(name)) {
         return (byte)(this.lengthWritable ? 1 : 0);
      } else {
         if (this.namedProps == null || !this.namedProps.containsKey(name)) {
            int i = parseIndex(name);
            if (i >= 0 && i < this.list.size() && this.list.get(i) != HOLE) {
               byte attrs = 7;
               if (this.frozen) {
                  attrs &= -6;
               } else if (this.sealed) {
                  attrs &= -5;
               }

               return attrs;
            }
         }

         return this.getAttrs(name);
      }
   }

   boolean isEnumerable(String name) {
      return (this.getOwnAttrs(name) & 2) != 0;
   }

   public void setElement(int index, Object value) {
      this.list.set(index, value);
   }

   public void addElement(Object value) {
      this.list.add(value);
   }

   public void removeElement(int index) {
      this.list.remove(index);
   }

   public List<Object> toList() {
      return this.list;
   }

   public Iterable<KeyValue> jsEntries() {
      return this.jsEntries(null);
   }

   public Iterable<KeyValue> jsEntries(CoreContext ctx) {
      return () -> new Iterator<KeyValue>() {
         int index = 0;
         Iterator<PropertySlot> namedIter = null;
         KeyValue peeked = null;
         int yieldCount = 0;

         private boolean advance() {
            while (this.index < JsArray.this.list.size()) {
               Object v = JsArray.this.list.get(this.index);
               PropertySlot s = JsArray.this.namedProps == null ? null : JsArray.this.namedProps.get(Integer.toString(this.index));
               if (v != JsArray.HOLE || s != null && !s.tombstoned) {
                  if (s == null || s.tombstoned || (s.attrs & 2) != 0) {
                     int i = this.index++;
                     Object val;
                     if (s != null && !s.tombstoned) {
                        val = ctx != null ? s.read(JsArray.this, ctx) : (s instanceof DataSlot ds ? ds.value : null);
                     } else {
                        val = v;
                     }

                     this.peeked = new KeyValue(JsArray.this, this.yieldCount++, i + "", val);
                     return true;
                  }

                  this.index++;
               } else {
                  this.index++;
               }
            }

            if (this.namedIter == null) {
               this.namedIter = JsArray.this.namedProps == null ? Collections.emptyIterator() : JsArray.this.namedProps.values().iterator();
            }

            while (this.namedIter.hasNext()) {
               PropertySlot s = this.namedIter.next();
               if (!s.tombstoned && (s.attrs & 2) != 0 && JsArray.parseIndex(s.name) < 0 && !"length".equals(s.name)) {
                  Object val = ctx != null ? s.read(JsArray.this, ctx) : (s instanceof DataSlot ds ? ds.value : null);
                  this.peeked = new KeyValue(JsArray.this, this.yieldCount++, s.name, val);
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
               KeyValue kv = this.peeked;
               this.peeked = null;
               return kv;
            }
         }
      };
   }

   static JsArray toArray(Map<String, Object> map) {
      List<Object> list = new ArrayList<>();
      if (map.containsKey("length")) {
         Object length = map.get("length");
         if (length instanceof Number) {
            int size = ((Number)length).intValue();

            for (int i = 0; i < size; i++) {
               list.add(Terms.UNDEFINED);
            }
         }
      }

      Set<Integer> indexes = new HashSet<>();

      for (String key : map.keySet()) {
         try {
            int index = Integer.parseInt(key);
            indexes.add(index);
         } catch (Exception var6) {
         }
      }

      for (int index : indexes) {
         list.add(index, map.get(index + ""));
      }

      return new JsArray(list);
   }

   @Override
   public Object call(Context context, Object[] args) {
      return create(args);
   }

   static JsArray create(Object[] args) {
      if (args.length == 1 && args[0] instanceof Number n) {
         double d = n.doubleValue();
         if (Double.isNaN(d) || Double.isInfinite(d) || d < 0.0 || d > 4.294967295E9 || d != Math.floor(d)) {
            throw JsErrorException.rangeError("Invalid array length");
         } else if (d > 2.147483647E9) {
            throw JsErrorException.rangeError("Invalid array length");
         } else {
            int count = (int)d;
            List<Object> list = new ArrayList<>(count);

            for (int i = 0; i < count; i++) {
               list.add(HOLE);
            }

            return new JsArray(list);
         }
      } else {
         return new JsArray(new ArrayList<>(Arrays.asList(args)));
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
         if (this.namedProps != null) {
            for (PropertySlot s : this.namedProps.values()) {
               if (!s.tombstoned) {
                  s.attrs &= -5;
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
         this.lengthWritable = false;
         if (this.namedProps != null) {
            for (PropertySlot s : this.namedProps.values()) {
               if (!s.tombstoned) {
                  s.attrs &= -5;
                  if (s instanceof DataSlot) {
                     s.attrs &= -2;
                  }
               }
            }
         }
      }
   }

   static final class ArrayLength {
      private ArrayLength() {
      }

      static boolean handleAssign(JsArray arr, Object value, CoreContext context) {
         long u32 = coerceToUint32(value, context);
         return applySet(arr, (int)u32);
      }

      static boolean defineProperty(JsArray arr, int newLen, byte attrs) {
         boolean ok = applySet(arr, newLen);
         arr.setAttrs("length", attrs);
         return ok;
      }

      static long coerceToUint32(Object value, CoreContext context) {
         Object first = value instanceof ObjectLike && context != null ? Terms.toPrimitive(value, "number", context) : value;
         Number n1 = Terms.objectToNumber(first);
         double d1 = n1 == null ? Double.NaN : n1.doubleValue();
         long u32;
         if (!Double.isNaN(d1) && !Double.isInfinite(d1)) {
            double truncated = d1 < 0.0 ? -Math.floor(-d1) : Math.floor(d1);
            long modded = (long)(truncated - Math.floor(truncated / 4.2949673E9F) * 4.2949673E9F);
            if (modded < 0L) {
               modded += 4294967296L;
            }

            u32 = modded;
         } else {
            u32 = 0L;
         }

         Object second = value instanceof ObjectLike && context != null ? Terms.toPrimitive(value, "number", context) : value;
         Number n2 = Terms.objectToNumber(second);
         double d2 = n2 == null ? Double.NaN : n2.doubleValue();
         if (d2 != u32) {
            throw JsErrorException.rangeError("Invalid array length");
         } else if (u32 > 2147483647L) {
            throw JsErrorException.rangeError("Invalid array length");
         } else {
            return u32;
         }
      }

      static boolean applySet(JsArray arr, int newLen) {
         if ((arr.getOwnAttrs("length") & 1) == 0) {
            return false;
         } else {
            int oldLen = arr.list.size();
            if (newLen >= oldLen) {
               if (newLen > oldLen && arr.nonExtensible) {
                  return false;
               } else {
                  arr.checkDensePad(newLen);

                  while (arr.list.size() < newLen) {
                     arr.list.add(JsArray.HOLE);
                  }

                  return true;
               }
            } else {
               int blockingIndex = -1;

               for (int i = oldLen - 1; i >= newLen; i--) {
                  if (!isIndexConfigurable(arr, i)) {
                     blockingIndex = i;
                     break;
                  }
               }

               if (blockingIndex >= 0) {
                  int retainTo = blockingIndex + 1;
                  if (retainTo < arr.list.size()) {
                     arr.list.subList(retainTo, arr.list.size()).clear();
                     if (arr.namedProps != null) {
                        for (int ix = retainTo; ix < oldLen; ix++) {
                           arr.namedProps.remove(Integer.toString(ix));
                        }
                     }
                  }

                  return false;
               } else {
                  arr.list.subList(newLen, oldLen).clear();
                  if (arr.namedProps != null) {
                     for (int ix = newLen; ix < oldLen; ix++) {
                        arr.namedProps.remove(Integer.toString(ix));
                     }
                  }

                  return true;
               }
            }
         }
      }

      private static boolean isIndexConfigurable(JsArray arr, int i) {
         PropertySlot s = arr.namedProps == null ? null : arr.namedProps.get(Integer.toString(i));
         return s == null || s.isConfigurable();
      }
   }
}
