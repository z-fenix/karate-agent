package io.karatelabs.js;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

public interface JavaCallable extends JsCallable, Function<Object, Object>, Predicate<Object>, Consumer<Object>, Supplier<Object>, Runnable {
   @Override
   Object call(Context var1, Object... var2);

   @Override
   default boolean isExternal() {
      return true;
   }

   @Override
   default Object apply(Object t) {
      return Engine.toJava(this.call(null, t));
   }

   @Override
   default boolean test(Object t) {
      return Terms.isTruthy(this.call(null, t));
   }

   @Override
   default void accept(Object t) {
      this.call(null, t);
   }

   @Override
   default Object get() {
      return Engine.toJava(this.call(null));
   }

   @Override
   default void run() {
      this.call(null);
   }
}
