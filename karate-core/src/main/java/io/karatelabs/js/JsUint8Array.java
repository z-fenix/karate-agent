package io.karatelabs.js;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

non-sealed class JsUint8Array extends JsArray implements JsBinaryValue {
   private final byte[] buffer;

   JsUint8Array(int length) {
      this.buffer = new byte[length];
   }

   JsUint8Array(byte[] bytes) {
      this.buffer = (byte[])bytes.clone();
   }

   @Override
   public Object getElement(int index) {
      return index >= 0 && index < this.buffer.length ? this.buffer[index] & 255 : Terms.UNDEFINED;
   }

   @Override
   boolean hasOwnIndexedSlot(int index) {
      return index >= 0 && index < this.buffer.length;
   }

   @Override
   public void setElement(int index, Object value) {
      if (index >= 0 && index < this.buffer.length && value instanceof Number v) {
         this.buffer[index] = (byte)(v.intValue() & 0xFF);
      }
   }

   @Override
   public Iterable<KeyValue> jsEntries() {
      return () -> new Iterator<KeyValue>() {
         int index = 0;

         @Override
         public boolean hasNext() {
            return this.index < JsUint8Array.this.buffer.length;
         }

         public KeyValue next() {
            int i = this.index++;
            return new KeyValue(JsUint8Array.this, i, i + "", JsUint8Array.this.buffer[i] & 255);
         }
      };
   }

   @Override
   public Iterable<KeyValue> jsEntries(CoreContext ctx) {
      return this.jsEntries();
   }

   @Override
   public List<Object> toList() {
      ArrayList<Object> list = new ArrayList<>(this.buffer.length);

      for (KeyValue kv : this.jsEntries()) {
         list.add(kv.value());
      }

      return list;
   }

   @Override
   public int size() {
      return this.buffer.length;
   }

   @Override
   public boolean isEmpty() {
      return this.buffer.length == 0;
   }

   @Override
   public boolean contains(Object o) {
      if (o instanceof Number n) {
         int val = n.intValue() & 0xFF;

         for (byte b : this.buffer) {
            if ((b & 255) == val) {
               return true;
            }
         }
      }

      return false;
   }

   @Override
   public Iterator<Object> iterator() {
      return new Iterator<Object>() {
         int index = 0;

         @Override
         public boolean hasNext() {
            return this.index < JsUint8Array.this.buffer.length;
         }

         @Override
         public Object next() {
            return JsUint8Array.this.buffer[this.index++] & 255;
         }
      };
   }

   @Override
   public Object[] toArray() {
      Object[] result = new Object[this.buffer.length];

      for (int i = 0; i < this.buffer.length; i++) {
         result[i] = this.buffer[i] & 255;
      }

      return result;
   }

   @Override
   public <T> T[] toArray(T[] a) {
      int size = this.buffer.length;
      T[] result = (T[])(a.length >= size ? a : (Object[])Array.newInstance(a.getClass().getComponentType(), size));

      for (int i = 0; i < size; i++) {
         result[i] = (T)(Object) Integer.valueOf(this.buffer[i] & 255);
      }

      if (a.length > size) {
         result[size] = null;
      }

      return result;
   }

   @Override
   public Object get(int index) {
      if (index >= 0 && index < this.buffer.length) {
         return this.buffer[index] & 255;
      } else {
         throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + this.buffer.length);
      }
   }

   @Override
   public Object set(int index, Object element) {
      if (index >= 0 && index < this.buffer.length) {
         int previous = this.buffer[index] & 255;
         if (element instanceof Number n) {
            this.buffer[index] = (byte)(n.intValue() & 0xFF);
         }

         return previous;
      } else {
         throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + this.buffer.length);
      }
   }

   @Override
   public int indexOf(Object o) {
      if (o instanceof Number n) {
         int val = n.intValue() & 0xFF;

         for (int i = 0; i < this.buffer.length; i++) {
            if ((this.buffer[i] & 255) == val) {
               return i;
            }
         }
      }

      return -1;
   }

   @Override
   public int lastIndexOf(Object o) {
      if (o instanceof Number n) {
         int val = n.intValue() & 0xFF;

         for (int i = this.buffer.length - 1; i >= 0; i--) {
            if ((this.buffer[i] & 255) == val) {
               return i;
            }
         }
      }

      return -1;
   }

   @Override
   public boolean add(Object o) {
      throw new UnsupportedOperationException("Uint8Array is fixed-size");
   }

   @Override
   public boolean remove(Object o) {
      throw new UnsupportedOperationException("Uint8Array is fixed-size");
   }

   @Override
   public void add(int index, Object element) {
      throw new UnsupportedOperationException("Uint8Array is fixed-size");
   }

   @Override
   public Object remove(int index) {
      throw new UnsupportedOperationException("Uint8Array is fixed-size");
   }

   @Override
   public void clear() {
      throw new UnsupportedOperationException("Uint8Array is fixed-size");
   }

   @Override
   public boolean addAll(Collection<? extends Object> c) {
      throw new UnsupportedOperationException("Uint8Array is fixed-size");
   }

   @Override
   public boolean addAll(int index, Collection<? extends Object> c) {
      throw new UnsupportedOperationException("Uint8Array is fixed-size");
   }

   @Override
   public boolean removeAll(Collection<?> c) {
      throw new UnsupportedOperationException("Uint8Array is fixed-size");
   }

   @Override
   public boolean retainAll(Collection<?> c) {
      throw new UnsupportedOperationException("Uint8Array is fixed-size");
   }

   @Override
   public ListIterator<Object> listIterator() {
      return new JsUint8Array.Uint8ListIterator(0);
   }

   @Override
   public ListIterator<Object> listIterator(int index) {
      return new JsUint8Array.Uint8ListIterator(index);
   }

   @Override
   public List<Object> subList(int fromIndex, int toIndex) {
      List<Object> result = new ArrayList<>(toIndex - fromIndex);

      for (int i = fromIndex; i < toIndex; i++) {
         result.add(this.buffer[i] & 255);
      }

      return result;
   }

   @Override
   protected Object resolveOwnIntrinsic(String name) {
      return "length".equals(name) ? this.buffer.length : super.resolveOwnIntrinsic(name);
   }

   @Override
   public Object getJavaValue() {
      return this.buffer;
   }

   @Override
   public boolean isConstructable() {
      return true;
   }

   @Override
   public Object call(Context context, Object[] args) {
      if (args.length == 0) {
         return new byte[0];
      } else {
         Object arg = args[0];
         if (arg instanceof Number n) {
            return new byte[n.intValue()];
         } else if (arg instanceof List<?> items) {
            byte[] bytes = new byte[items.size()];

            for (int i = 0; i < items.size(); i++) {
               if (items.get(i) instanceof Number n) {
                  bytes[i] = (byte)(n.intValue() & 0xFF);
               } else {
                  bytes[i] = 0;
               }
            }

            return bytes;
         } else {
            return new byte[0];
         }
      }
   }

   private class Uint8ListIterator implements ListIterator<Object> {
      private int index;

      Uint8ListIterator(int index) {
         this.index = index;
      }

      @Override
      public boolean hasNext() {
         return this.index < JsUint8Array.this.buffer.length;
      }

      @Override
      public Object next() {
         return JsUint8Array.this.buffer[this.index++] & 255;
      }

      @Override
      public boolean hasPrevious() {
         return this.index > 0;
      }

      @Override
      public Object previous() {
         return JsUint8Array.this.buffer[--this.index] & 255;
      }

      @Override
      public int nextIndex() {
         return this.index;
      }

      @Override
      public int previousIndex() {
         return this.index - 1;
      }

      @Override
      public void remove() {
         throw new UnsupportedOperationException("Uint8Array is fixed-size");
      }

      @Override
      public void set(Object o) {
         if (o instanceof Number n) {
            JsUint8Array.this.buffer[this.index - 1] = (byte)(n.intValue() & 0xFF);
         }
      }

      @Override
      public void add(Object o) {
         throw new UnsupportedOperationException("Uint8Array is fixed-size");
      }
   }
}
