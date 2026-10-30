package io.karatelabs.js;

@FunctionalInterface
interface JsCallable {
   Object call(Context var1, Object[] var2);

   default boolean isExternal() {
      return false;
   }

   default boolean isConstructable() {
      return false;
   }
}
