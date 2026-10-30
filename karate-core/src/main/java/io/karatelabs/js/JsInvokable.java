package io.karatelabs.js;

@FunctionalInterface
interface JsInvokable extends JsCallable {
   Object invoke(Object[] var1);

   @Override
   default Object call(Context context, Object[] args) {
      return this.invoke(args);
   }
}
