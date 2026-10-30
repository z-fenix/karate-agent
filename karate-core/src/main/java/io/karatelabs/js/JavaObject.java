package io.karatelabs.js;

import java.util.Map;

public class JavaObject implements ExternalAccess, ObjectLike {
   private final Object object;

   public JavaObject(Object object) {
      this.object = object;
   }

   @Override
   public Object getJavaValue() {
      return this.object;
   }

   @Override
   public Object construct(Object[] args) {
      return JavaUtils.construct(this.object.getClass(), args);
   }

   @Override
   public Object invokeMethod(String name, Object[] args) {
      return JavaUtils.convertIfArray(JavaUtils.invoke(this.object, name, args));
   }

   @Override
   public Object getProperty(String name) {
      return this.getMember(name);
   }

   @Override
   public void setProperty(String name, Object value) {
      this.putMember(name, value);
   }

   @Override
   public Object getMember(String name) {
      return JavaUtils.convertIfArray(JavaUtils.get(this.object, name));
   }

   @Override
   public Object getPropertyOrNotFound(String name) {
      Object result = JavaUtils.getOrNotFound(this.object, name);
      return result == JavaUtils.NOT_FOUND ? result : JavaUtils.convertIfArray(result);
   }

   @Override
   public void putMember(String name, Object value) {
      JavaUtils.set(this.object, name, value);
   }

   @Override
   public void removeMember(String name) {
      throw JsErrorException.typeError("cannot delete property on " + JavaUtils.jsTypeName(this.object.getClass()));
   }

   @Override
   public Map<String, Object> toMap() {
      return (Map<String, Object>)JavaUtils.toMap(this.object);
   }
}
