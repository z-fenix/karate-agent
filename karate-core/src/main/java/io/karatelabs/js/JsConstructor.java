package io.karatelabs.js;

@FunctionalInterface
interface JsConstructor extends JsCallable {
   @Override
   default boolean isConstructable() {
      return true;
   }

   @Override
   default boolean isExternal() {
      return true;
   }
}
