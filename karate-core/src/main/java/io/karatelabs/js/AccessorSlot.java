package io.karatelabs.js;

final class AccessorSlot extends PropertySlot {
   JsCallable getter;
   JsCallable setter;

   AccessorSlot(String name) {
      super(name);
   }

   AccessorSlot(String name, JsCallable getter, JsCallable setter) {
      super(name);
      this.getter = getter;
      this.setter = setter;
   }

   @Override
   boolean isAccessor() {
      return true;
   }

   @Override
   Object read(Object receiver, CoreContext ctx) {
      return this.getter != null && ctx != null ? Interpreter.invokeGetter(this.getter, receiver, ctx) : Terms.UNDEFINED;
   }

   @Override
   void write(Object receiver, Object newValue, CoreContext ctx, boolean strict) {
      if (this.setter == null) {
         if (strict) {
            throw JsErrorException.typeError("Cannot set property '" + this.name + "' which has only a getter");
         }
      } else {
         if (ctx != null) {
            Interpreter.invokeSetter(this.setter, receiver, newValue, ctx);
         }
      }
   }

   @Override
   public String toString() {
      return this.name + "={get=" + this.getter + ", set=" + this.setter + "}";
   }
}
