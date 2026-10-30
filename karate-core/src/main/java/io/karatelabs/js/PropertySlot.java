package io.karatelabs.js;

abstract sealed class PropertySlot permits DataSlot, AccessorSlot {
   static final byte WRITABLE = 1;
   static final byte ENUMERABLE = 2;
   static final byte CONFIGURABLE = 4;
   static final byte INTRINSIC = 8;
   static final byte ATTRS_DEFAULT = 7;
   final String name;
   byte attrs = 7;
   boolean tombstoned;

   PropertySlot(String name) {
      this.name = name;
   }

   boolean isWritable() {
      return (this.attrs & 1) != 0;
   }

   boolean isEnumerable() {
      return (this.attrs & 2) != 0;
   }

   boolean isConfigurable() {
      return (this.attrs & 4) != 0;
   }

   boolean isIntrinsic() {
      return (this.attrs & 8) != 0;
   }

   abstract boolean isAccessor();

   abstract Object read(Object var1, CoreContext var2);

   abstract void write(Object var1, Object var2, CoreContext var3, boolean var4);
}
