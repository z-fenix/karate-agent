package io.karatelabs.markup;

public interface MarkupScope {
   Object lookup(String var1);

   default void set(String name, Object value) {
   }
}
