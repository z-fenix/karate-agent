package io.karatelabs.js;

public interface RunInterceptor<T> {
   RunInterceptor.Action beforeExecute(T var1);

   default void afterExecute(T context, Object result, Throwable error) {
   }

   default RunInterceptor.Action waitForResume() {
      return RunInterceptor.Action.PROCEED;
   }

   public static enum Action {
      PROCEED,
      SKIP,
      WAIT;
   }
}
