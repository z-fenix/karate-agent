package io.karatelabs.js;

public interface ExternalBridge {
   default ExternalAccess forType(String className) {
      try {
         return new JavaType(className);
      } catch (Exception var3) {
         return null;
      }
   }

   default ExternalAccess forInstance(Object object) {
      if (object == null) {
         throw new NullPointerException("object is null");
      } else {
         return new JavaObject(object);
      }
   }
}
