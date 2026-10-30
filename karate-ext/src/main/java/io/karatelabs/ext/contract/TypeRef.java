package io.karatelabs.ext.contract;

import java.util.function.Supplier;

public record TypeRef(String name, Supplier<TypeModel> resolve) {

   public TypeRef(String name, Supplier<TypeModel> resolve) {
      this.name = name;
      this.resolve = resolve;
   }

   public static TypeRef nameOnly(String var0) {
      return new TypeRef(var0, null);
   }

   public TypeModel model() {
      return this.resolve == null ? null : this.resolve.get();
   }
}
