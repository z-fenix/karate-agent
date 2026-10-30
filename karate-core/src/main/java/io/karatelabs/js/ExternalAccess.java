package io.karatelabs.js;

public interface ExternalAccess {
   Object construct(Object[] var1);

   Object getProperty(String var1);

   default Object getPropertyOrNotFound(String name) {
      return this.getProperty(name);
   }

   void setProperty(String var1, Object var2);

   Object invokeMethod(String var1, Object[] var2);

   default JavaInvokable getMethod(String name) {
      return args -> this.invokeMethod(name, args);
   }

   Object getJavaValue();
}
