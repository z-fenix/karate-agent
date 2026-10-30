package io.karatelabs.js;

import java.util.List;

abstract class AsyncJob {
   AsyncToken token;

   abstract void run(Engine var1);

   boolean macrotask() {
      return false;
   }

   static List<AsyncJob> one(AsyncJob job) {
      return List.of(job);
   }

   static final class Control extends AsyncJob {
      final String reason;

      Control(String reason) {
         this.reason = reason;
      }

      @Override
      void run(Engine engine) {
      }

      @Override
      public String toString() {
         return "control[" + this.reason + "]";
      }
   }

   static final class Microtask extends AsyncJob {
      private final Runnable body;
      private final String label;

      Microtask(String label, Runnable body) {
         this.label = label;
         this.body = body;
      }

      @Override
      void run(Engine engine) {
         this.body.run();
      }

      @Override
      public String toString() {
         return "microtask[" + this.label + "]";
      }
   }

   static final class Timer extends AsyncJob {
      private final JsCallable callback;
      private final Object[] args;
      private final int id;

      Timer(int id, JsCallable callback, Object[] args) {
         this.id = id;
         this.callback = callback;
         this.args = args;
      }

      @Override
      void run(Engine engine) {
         AsyncSupport.invokeQueuedCallback(engine, this.callback, this.args);
      }

      @Override
      boolean macrotask() {
         return true;
      }

      @Override
      public String toString() {
         return "timer[" + this.id + "]";
      }
   }
}
