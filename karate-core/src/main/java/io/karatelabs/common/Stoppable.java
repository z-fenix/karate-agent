package io.karatelabs.common;

import java.util.concurrent.ExecutorService;

public interface Stoppable extends AutoCloseable {
   String lifecycleName();

   String lifecycleKind();

   void stop();

   @Override
   default void close() {
      this.stop();
   }

   default ExecutorService lifecycleExecutor() {
      return null;
   }
}
