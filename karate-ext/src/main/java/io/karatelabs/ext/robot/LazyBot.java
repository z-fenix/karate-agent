package io.karatelabs.ext.robot;

import io.karatelabs.js.JavaInvokable;
import io.karatelabs.js.SimpleObject;
import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;

public final class LazyBot implements SimpleObject {
   private final Supplier<Object> creator;
   private volatile SimpleObject real;

   public LazyBot(Supplier<Object> var1) {
      this.creator = var1;
   }

   private synchronized SimpleObject real() {
      if (this.real == null) {
         this.real = (SimpleObject)this.creator.get();
      }

      return this.real;
   }

   @Override
   public Object jsGet(String var1) {
      return this.real().jsGet(var1);
   }

   @Override
   public Collection<String> jsKeys() {
      SimpleObject var1 = this.real;
      return (Collection<String>)(var1 == null ? List.of() : var1.jsKeys());
   }

   public boolean isLaunched() {
      return this.real != null;
   }

   public void release() {
      SimpleObject var1 = this.real;
      if (var1 != null && var1.jsGet("destroy") instanceof JavaInvokable var2) {
         var2.invoke();
      }
   }
}
