package io.karatelabs.js;

final class PrivateName {
   static final Object BRAND = new Object();
   final String name;
   PrivateName.Kind kind;
   JsCallable method;
   JsCallable getter;
   JsCallable setter;

   PrivateName(String name, PrivateName.Kind kind) {
      this.name = name;
      this.kind = kind;
   }

   @Override
   public String toString() {
      return this.name;
   }

   static enum Kind {
      FIELD,
      METHOD,
      ACCESSOR;
   }
}
