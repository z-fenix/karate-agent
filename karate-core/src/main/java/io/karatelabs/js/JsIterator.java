package io.karatelabs.js;

import java.util.Iterator;

public interface JsIterator extends Iterator<Object> {
   default void close(Context context, boolean dueToError) {
   }
}
