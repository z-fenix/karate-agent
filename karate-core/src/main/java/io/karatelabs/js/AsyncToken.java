package io.karatelabs.js;

import java.util.concurrent.atomic.AtomicBoolean;

final class AsyncToken {
   private final AsyncScope scope;
   private final AtomicBoolean live = new AtomicBoolean(true);
   final String kind;

   AsyncToken(AsyncScope scope, String kind) {
      this.scope = scope;
      this.kind = kind;
   }

   boolean release() {
      if (this.live.compareAndSet(true, false)) {
         this.scope.decrement();
         return true;
      } else {
         return false;
      }
   }

   boolean isLive() {
      return this.live.get();
   }

   @Override
   public String toString() {
      return "token[" + this.kind + (this.live.get() ? ",live]" : ",released]");
   }
}
