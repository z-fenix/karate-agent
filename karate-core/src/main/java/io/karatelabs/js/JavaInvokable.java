package io.karatelabs.js;

@FunctionalInterface
public interface JavaInvokable extends JavaCallable {
   Object invoke(Object... var1);

   @Override
   default Object call(Context context, Object[] args) {
      return this.invoke(args);
   }
}
