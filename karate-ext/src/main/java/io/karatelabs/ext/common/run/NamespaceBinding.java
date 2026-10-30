package io.karatelabs.ext.common.run;

import io.karatelabs.js.Engine;
import java.util.function.Consumer;

public record NamespaceBinding(String a, Object b, String c, Consumer<Engine> d) {

   public NamespaceBinding(String a, Object b, String c, Consumer<Engine> d) {
      this.a = a;
      this.b = b;
      this.c = c;
      this.d = d;
   }

   public static NamespaceBinding a(String var0, Object var1) {
      return new NamespaceBinding(var0, var1, null, null);
   }

   public static NamespaceBinding a(String var0, Object var1, String var2) {
      return new NamespaceBinding(var0, var1, var2, null);
   }

   public NamespaceBinding a(Consumer<Engine> var1) {
      return new NamespaceBinding(this.a, this.b, this.c, var1);
   }
}
