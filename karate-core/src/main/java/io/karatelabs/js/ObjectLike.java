package io.karatelabs.js;

import java.util.Map;

public interface ObjectLike {
   Object getMember(String var1);

   default Object getMember(String name, Object receiver, CoreContext ctx) {
      return this.getMember(name);
   }

   void putMember(String var1, Object var2);

   default void putMember(String name, Object value, CoreContext ctx, boolean strict) {
      this.putMember(name, value);
   }

   void removeMember(String var1);

   default void removeMember(String name, CoreContext ctx, boolean strict) {
      this.removeMember(name);
   }

   Map<String, Object> toMap();

   default ObjectLike getPrototype() {
      return null;
   }

   default boolean isOwnProperty(String name) {
      return this.toMap().containsKey(name);
   }

   default boolean isExtensible() {
      return true;
   }

   default boolean isSealed() {
      return false;
   }

   default boolean isFrozen() {
      return false;
   }

   default void setExtensible(boolean extensible) {
   }

   default void setSealed(boolean sealed) {
   }

   default void setFrozen(boolean frozen) {
   }
}
