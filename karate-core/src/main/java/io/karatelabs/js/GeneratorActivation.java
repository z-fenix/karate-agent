package io.karatelabs.js;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.LockSupport;

final class GeneratorActivation implements Runnable {
   private static final AtomicLong IDS = new AtomicLong();
   static volatile boolean anyGenerators;
   private static final ThreadLocal<GeneratorActivation> CURRENT = new ThreadLocal<>();
   final Engine engine;
   private final JsFunctionNode function;
   private final CoreContext functionContext;
   private final Object[] args;
   final AtomicReference<GeneratorActivation.State> state = new AtomicReference<>(GeneratorActivation.State.NOT_STARTED);
   private final CountDownLatch finished = new CountDownLatch(1);
   private volatile AtomicReference<GeneratorActivation.StepOutcome> stepCell;
   private GeneratorActivation.ResumeKind resumeKind;
   private Object resumeValue;
   private AsyncScope stepScope;
   private volatile boolean resumeReady;
   private volatile Thread thread;
   private volatile Thread driver;

   GeneratorActivation(Engine engine, JsFunctionNode function, CoreContext functionContext, Object[] args) {
      this.engine = engine;
      this.function = function;
      this.functionContext = functionContext;
      this.args = args;
   }

   static GeneratorActivation current() {
      return anyGenerators ? CURRENT.get() : null;
   }

   boolean isDone() {
      return this.state.get() == GeneratorActivation.State.DONE;
   }

   boolean isRunning() {
      return this.state.get() == GeneratorActivation.State.RUNNING;
   }

   boolean isStarted() {
      return this.state.get() != GeneratorActivation.State.NOT_STARTED;
   }

   void retire() {
      this.state.set(GeneratorActivation.State.DONE);
   }

   GeneratorActivation.StepOutcome step(GeneratorActivation.ResumeKind kind, Object value) {
      anyGenerators = true;
      GeneratorActivation.State s = this.state.get();
      if (s == GeneratorActivation.State.RUNNING) {
         throw JsErrorException.typeError("Generator is already running");
      } else if (s == GeneratorActivation.State.DONE) {
         return new GeneratorActivation.StepOutcome(GeneratorActivation.OutcomeKind.RETURNED, Terms.UNDEFINED);
      } else if (!this.state.compareAndSet(s, GeneratorActivation.State.RUNNING)) {
         throw JsErrorException.typeError("Generator is already running");
      } else {
         AtomicReference<GeneratorActivation.StepOutcome> cell = new AtomicReference<>();
         this.stepCell = cell;
         this.resumeKind = kind;
         this.resumeValue = value;
         this.driver = Thread.currentThread();
         AsyncScope scope = this.engine.currentScope();
         if (scope != null && !scope.addGenerator(this)) {
            this.state.set(GeneratorActivation.State.DONE);
            throw new EngineInterruptedException();
         } else {
            this.stepScope = scope;
            boolean firstStep = s == GeneratorActivation.State.NOT_STARTED;
            if (firstStep) {
               try {
                  Thread vthread = Thread.ofVirtual().name("js-gen-", IDS.incrementAndGet()).unstarted(this);
                  this.thread = vthread;
                  vthread.start();
               } catch (Throwable var12) {
                  this.deregister();
                  this.state.set(GeneratorActivation.State.DONE);
                  this.finished.countDown();
                  throw var12;
               }
            }

            int holds = this.engine.releaseJsLock();
            if (!firstStep) {
               this.resumeReady = true;
               LockSupport.unpark(this.thread);
            }

            try {
               while (cell.get() == null) {
                  if (Thread.interrupted()) {
                     Thread.currentThread().interrupt();
                     this.cancel();
                     throw new EngineInterruptedException();
                  }

                  LockSupport.park();
               }
            } finally {
               this.driver = null;
            }

            this.engine.reacquireJsLock(holds);
            return cell.get();
         }
      }
   }

   GeneratorActivation.ResumeSignal yieldAndReceive(Object value) {
      this.deregister();
      this.state.set(GeneratorActivation.State.SUSPENDED);
      this.publish(GeneratorActivation.OutcomeKind.YIELDED, value);
      int holds = this.engine.releaseJsLock();

      try {
         while (!this.resumeReady) {
            if (Thread.interrupted()) {
               Thread.currentThread().interrupt();
               throw new EngineInterruptedException();
            }

            LockSupport.park();
         }
      } finally {
         this.resumeReady = false;
      }

      this.engine.reacquireJsLock(holds);
      AsyncScope scope = this.stepScope;
      if (scope != null && !scope.isOpen()) {
         throw new EngineInterruptedException();
      } else {
         return new GeneratorActivation.ResumeSignal(this.resumeKind, this.resumeValue);
      }
   }

   private void publishFinal(GeneratorActivation.OutcomeKind kind, Object value) {
      this.state.set(GeneratorActivation.State.DONE);
      this.deregister();
      this.publish(kind, value);
   }

   private void publish(GeneratorActivation.OutcomeKind kind, Object value) {
      AtomicReference<GeneratorActivation.StepOutcome> cell = this.stepCell;
      if (cell != null && cell.compareAndSet(null, new GeneratorActivation.StepOutcome(kind, value))) {
         Thread d = this.driver;
         if (d != null) {
            LockSupport.unpark(d);
         }
      }
   }

   AsyncScope currentStepScope() {
      return this.stepScope;
   }

   private void deregister() {
      AsyncScope scope = this.stepScope;
      if (scope != null) {
         scope.removeGenerator(this);
      }
   }

   @Override
   public void run() {
      CURRENT.set(this);

      try {
         this.runBody();
      } finally {
         CURRENT.remove();
         this.state.set(GeneratorActivation.State.DONE);
         this.deregister();
         this.publish(GeneratorActivation.OutcomeKind.HOST_CANCELLED, null);
         this.finished.countDown();
         if (this.engine.jsLock.isHeldByCurrentThread()) {
            this.engine.releaseJsLock();
         }
      }
   }

   private void runBody() {
      try {
         this.engine.jsLock.lockInterruptibly();
      } catch (InterruptedException var10) {
         Thread.currentThread().interrupt();
         return;
      }

      Engine previous = Engine.enter(this.engine);

      try {
         AsyncScope scope = this.stepScope;
         if (scope == null || scope.isOpen()) {
            Object result = this.function.executeBody(this.functionContext, this.functionContext, this.args);
            if (this.functionContext.isError()) {
               Object reason = this.functionContext.getErrorThrown();
               this.functionContext.reset();
               this.publishFinal(GeneratorActivation.OutcomeKind.THREW, reason);
            } else {
               this.publishFinal(GeneratorActivation.OutcomeKind.RETURNED, result);
            }

            return;
         }
      } catch (StackOverflowError var11) {
         this.publishFinal(GeneratorActivation.OutcomeKind.THREW, JsErrorException.rangeError("Maximum call stack size exceeded").payload);
         return;
      } catch (Throwable var12) {
         if (!(var12 instanceof FlowControlSignal) && !AsyncSupport.isHostCancellation(var12)) {
            if (var12 instanceof Error err) {
               this.publishFinal(GeneratorActivation.OutcomeKind.HOST_CANCELLED, null);
               throw err;
            }

            this.publishFinal(GeneratorActivation.OutcomeKind.THREW, AsyncSupport.reasonOf(var12));
         } else {
            this.publishFinal(GeneratorActivation.OutcomeKind.HOST_CANCELLED, null);
         }

         return;
      } finally {
         Engine.exit(previous);
         if (this.engine.jsLock.isHeldByCurrentThread()) {
            this.engine.releaseJsLock();
         }
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
      return "generator["
         + (this.function.name == null ? "anonymous" : this.function.name)
         + ","
         + (t == null ? "unstarted" : t.getName())
         + ","
         + this.state.get()
         + "]";
   }

   static enum OutcomeKind {
      YIELDED,
      RETURNED,
      THREW,
      HOST_CANCELLED;
   }

   static enum ResumeKind {
      NEXT,
      THROW,
      RETURN;
   }

   static final class ResumeSignal {
      final GeneratorActivation.ResumeKind kind;
      final Object value;

      ResumeSignal(GeneratorActivation.ResumeKind kind, Object value) {
         this.kind = kind;
         this.value = value;
      }
   }

   static enum State {
      NOT_STARTED,
      RUNNING,
      SUSPENDED,
      DONE;
   }

   static final class StepOutcome {
      final GeneratorActivation.OutcomeKind kind;
      final Object value;

      StepOutcome(GeneratorActivation.OutcomeKind kind, Object value) {
         this.kind = kind;
         this.value = value;
      }
   }
}
