package io.karatelabs.js;

final class JsBuiltinMethod extends JsFunction {
   private final JsCallable delegate;
   private final boolean constructable;

   JsBuiltinMethod(String name, int length, JsCallable delegate) {
      this(name, length, delegate, false);
   }

   JsBuiltinMethod(String name, int length, JsCallable delegate, boolean constructable) {
      this.name = name;
      this.length = length;
      this.delegate = delegate;
      this.constructable = constructable;
   }

   JsCallable delegate() {
      return this.delegate;
   }

   @Override
   public Object call(Context context, Object[] args) {
      return this.delegate.call(context, args);
   }

   @Override
   public boolean isConstructable() {
      return this.constructable;
   }
}
