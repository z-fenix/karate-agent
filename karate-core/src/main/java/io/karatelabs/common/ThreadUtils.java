package io.karatelabs.common;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public final class ThreadUtils {
   private ThreadUtils() {
   }

   public static ThreadFactory daemonFactory(String prefix) {
      return daemonFactory(prefix, true);
   }

   public static ThreadFactory daemonFactory(String name, boolean numbered) {
      if (!numbered) {
         return r -> {
            Thread t = new Thread(r, name);
            t.setDaemon(true);
            return t;
         };
      } else {
         AtomicInteger counter = new AtomicInteger();
         return r -> {
            Thread t = new Thread(r, name + counter.incrementAndGet());
            t.setDaemon(true);
            return t;
         };
      }
   }
}
