package io.karatelabs.js;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.LockSupport;

final class AsyncActivation implements Runnable {
   private static final AtomicLong IDS = new AtomicLong();
   static volatile boolean anyAsync;
   private static final ThreadLocal<AsyncActivation> CURRENT = new ThreadLocal<>();
   final Engine engine;
   final AsyncScope scope;
   private final AsyncToken token;
   private final JsFunctionNode function;
   private final CoreContext functionContext;
   private final Object[] args;
   private final JsPromise result;
   private final AtomicReference<AsyncActivation.Outcome> outcome = new AtomicReference<>();
   private final CountDownLatch finished = new CountDownLatch(1);
   private volatile Thread caller;
   private volatile Thread thread;

   AsyncActivation(Engine engine, AsyncScope scope, AsyncToken token, JsFunctionNode function, CoreContext functionContext, Object[] args, JsPromise result) {
      this.engine = engine;
      this.scope = scope;
      this.token = token;
      this.function = function;
      this.functionContext = functionContext;
      this.args = args;
      this.result = result;
   }

   static AsyncActivation current() {
      return anyAsync ? CURRENT.get() : null;
   }

   static JsPromise spawn(Engine engine, AsyncScope scope, JsFunctionNode function, CoreContext functionContext, Object[] args) {
      anyAsync = true;
      JsPromise promise = new JsPromise(engine, scope);
      AsyncToken token = scope.acquire("activation");
      if (token == null) {
         throw new EngineInterruptedException();
      } else {
         AsyncActivation activation = new AsyncActivation(engine, scope, token, function, functionContext, args, promise);
         activation.caller = Thread.currentThread();
         scope.addActivation(activation);

         try {
            Thread vthread = Thread.ofVirtual().name("js-async-", IDS.incrementAndGet()).unstarted(activation);
            activation.thread = vthread;
            vthread.start();
         } catch (Throwable var12) {
            scope.removeActivation(activation);
            promise.settle(true, AsyncSupport.reasonOf(var12), token);
            activation.finished.countDown();
            return promise;
         }

         int holds = engine.releaseJsLock();

         try {
            activation.awaitOutcome();
         } catch (EngineInterruptedException var11) {
            scope.requestCancel("caller interrupted while starting " + function.name);
            throw var11;
         }

         engine.reacquireJsLock(holds);
         return promise;
      }
   }

   private void awaitOutcome() {
      while (this.outcome.get() == null) {
         if (Thread.interrupted()) {
            Thread.currentThread().interrupt();
            throw new EngineInterruptedException();
         }

         LockSupport.park();
      }
   }

   private void publish(AsyncActivation.Outcome value) {
      if (this.outcome.compareAndSet(null, value)) {
         LockSupport.unpark(this.caller);
      }
   }

   @Override
   public void run() {
      CURRENT.set(this);

      try {
         this.runBody();
      } finally {
         CURRENT.remove();
         this.publish(AsyncActivation.Outcome.FAILED);
         this.token.release();
         this.scope.removeActivation(this);
         this.finished.countDown();
      }
   }

   private void runBody() {
      if (!this.scope.isOpen()) {
         this.publish(AsyncActivation.Outcome.FAILED);
      } else {
         try {
            this.engine.jsLock.lockInterruptibly();
         } catch (InterruptedException var8) {
            Thread.currentThread().interrupt();
            this.result.settle(true, AsyncSupport.cancelledReason(), null);
            this.publish(AsyncActivation.Outcome.FAILED);
            return;
         }

         Engine previous = Engine.enter(this.engine);

         try {
            if (this.scope.isOpen()) {
               Object value = this.function.executeBody(this.functionContext, this.functionContext, this.args);
               if (this.functionContext.isError()) {
                  Object reason = this.functionContext.getErrorThrown();
                  this.functionContext.reset();
                  this.settle(true, reason);
               } else {
                  this.settle(false, value);
               }

               this.publish(AsyncActivation.Outcome.COMPLETED);
               return;
            }
         } catch (Throwable var9) {
            if (var9 instanceof FlowControlSignal || AsyncSupport.isHostCancellation(var9)) {
               this.publish(AsyncActivation.Outcome.FAILED);
               return;
            } else {
               if (var9 instanceof Error) {
                  this.scope.requestCancel("fatal error in " + this + ": " + var9);
                  this.publish(AsyncActivation.Outcome.FAILED);
               } else {
                  this.settle(true, AsyncSupport.reasonOf(var9));
                  this.publish(AsyncActivation.Outcome.FAILED);
               }

               return;
            }
         } finally {
            Engine.exit(previous);
            if (this.engine.jsLock.isHeldByCurrentThread()) {
               this.engine.releaseJsLock();
            }
         }
      }
   }

   private void settle(boolean rejected, Object value) {
      if (rejected) {
         this.result.settle(true, value, this.token);
      } else {
         AsyncSupport.resolveValue(this.result, value, this.token);
      }
   }

   Object awaitOn(JsPromise promise) {
      CompletableFuture<Object> cf = promise.future();
      if (cf.isDone()) {
         return AsyncSupport.settledResult(promise);
      } else {
         Thread self = Thread.currentThread();
         cf.whenComplete((value, error) -> LockSupport.unpark(self));
         this.publish(AsyncActivation.Outcome.SUSPENDED);

         int holds = 0;

         try {
            AsyncSupport.checkStale(this.scope);
         } finally {
            holds = this.engine.releaseJsLock();
         }

         while (!cf.isDone()) {
            if (Thread.interrupted() || !this.scope.isOpen()) {
               Thread.currentThread().interrupt();
               throw new EngineInterruptedException();
            }

            LockSupport.park();
         }

         AsyncSupport.checkStale(this.scope);
         this.engine.reacquireJsLock(holds);
         AsyncSupport.checkStale(this.scope);
         return AsyncSupport.settledResult(promise);
      }
   }

   void cancel() {
      Thread t = this.thread;
      if (t != null) {
         t.interrupt();
         LockSupport.unpark(t);
      }
   }

   boolean awaitTermination(long millis) {
      long deadline = System.nanoTime() + millis * 1000000L;
      boolean interrupted = false;

      try {
         while (true) {
            long remaining = deadline - System.nanoTime();
            if (remaining <= 0L) {
               return this.finished.getCount() == 0L;
            }

            try {
               return this.finished.await(remaining, TimeUnit.NANOSECONDS);
            } catch (InterruptedException var12) {
               interrupted = true;
            }
         }
      } finally {
         if (interrupted) {
            Thread.currentThread().interrupt();
         }
      }
   }

   @Override
   public String toString() {
      Thread t = this.thread;
      return "activation[" + (this.function.name == null ? "anonymous" : this.function.name) + "," + (t == null ? "unstarted" : t.getName()) + "]";
   }

   static enum Outcome {
      SUSPENDED,
      COMPLETED,
      FAILED;
   }
}
