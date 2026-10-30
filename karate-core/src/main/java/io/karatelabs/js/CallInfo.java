package io.karatelabs.js;

public class CallInfo {
   public final boolean constructor;
   public final Object called;

   CallInfo(boolean constructor, Object called) {
      this.constructor = constructor;
      this.called = called;
   }
}
