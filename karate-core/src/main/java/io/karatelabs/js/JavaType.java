package io.karatelabs.js;

public class JavaType implements ExternalAccess {
   private final Class<?> clazz;

   public JavaType(Class<?> clazz) {
      this.clazz = clazz;
   }

   @Override
   public Object getJavaValue() {
      return this.clazz;
   }

   public JavaType(String className) {
      Class<?> resolved = null;
      ClassNotFoundException last = null;
      ClassLoader tccl = Thread.currentThread().getContextClassLoader();
      if (tccl != null) {
         try {
            resolved = Class.forName(className, true, tccl);
         } catch (ClassNotFoundException var6) {
            last = var6;
         }
      }

      if (resolved == null) {
         try {
            resolved = Class.forName(className);
         } catch (ClassNotFoundException var7) {
            throw new RuntimeException(last != null ? last : var7);
         }
      }

      this.clazz = resolved;
   }

   @Override
   public Object construct(Object[] args) {
      return JavaUtils.construct(this.clazz, args);
   }

   @Override
   public Object invokeMethod(String name, Object[] args) {
      return JavaUtils.convertIfArray(JavaUtils.invokeStatic(this.clazz, name, args));
   }

   @Override
   public Object getProperty(String name) {
      return JavaUtils.convertIfArray(JavaUtils.getStatic(this.clazz, name));
   }

   @Override
   public void setProperty(String name, Object value) {
      JavaUtils.setStatic(this.clazz, name, value);
   }
}
