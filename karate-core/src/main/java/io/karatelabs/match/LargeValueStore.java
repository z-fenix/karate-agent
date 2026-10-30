package io.karatelabs.match;

import java.io.Closeable;
import java.util.Iterator;

public interface LargeValueStore extends Closeable {
   int size();

   Object get(int var1);

   Iterator<Object> iterator();

   @Override
   void close();
}
