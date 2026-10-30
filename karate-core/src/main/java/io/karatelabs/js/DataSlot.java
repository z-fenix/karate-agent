package io.karatelabs.js;

final class DataSlot extends PropertySlot {
   Object value;

   DataSlot(String name) {
      super(name);
   }

   DataSlot(String name, Object value) {
      super(name);
      this.value = value;
   }

   @Override
   boolean isAccessor() {
      return false;
   }

   @Override
   Object read(Object receiver, CoreContext ctx) {
      return this.value;
   }

   @Override
   void write(Object receiver, Object newValue, CoreContext ctx, boolean strict) {
      if (!this.isWritable()) {
         if (strict) {
            throw JsErrorException.typeError("Cannot assign to read only property '" + this.name + "'");
         }
      } else {
         this.value = newValue;
      }
   }

   @Override
   public String toString() {
      return this.name + "=" + this.value;
   }
}
