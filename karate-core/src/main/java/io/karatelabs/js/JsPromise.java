package io.karatelabs.js;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class JsPromise extends JsObject {
   final Engine engine;
   final AsyncScope scope;
   private final CompletableFuture<Object> cf = new CompletableFuture<>();
   private List<JsPromise.Reaction> reactions;
   private boolean rejectionHandled;
   private boolean rejectionReported;
   private JsPromise.PromiseView javaView;

   JsPromise(Engine engine, AsyncScope scope) {
      super(null, JsPromisePrototype.INSTANCE);
      this.engine = engine;
      this.scope = scope;
   }

   boolean isSettled() {
      return this.cf.isDone();
   }

   boolean isRejected() {
      return this.cf.isCompletedExceptionally();
   }

   CompletableFuture<Object> future() {
      return this.cf;
   }

   Object settledValue() {
      try {
         return this.cf.getNow(null);
      } catch (CompletionException var2) {
         return AsyncSupport.reasonOf(var2.getCause());
      } catch (RuntimeException var3) {
         return AsyncSupport.reasonOf(var3);
      }
   }

   void settle(boolean rejected, Object value, AsyncToken retiring) {
      boolean won = rejected ? this.cf.completeExceptionally(new JsRejectionException(value)) : this.cf.complete(value);
      if (!won) {
         if (retiring != null) {
            retiring.release();
         }
      } else {
         if (AsyncSupport.onEngineThread(this.engine)) {
            this.flush(retiring);
         } else {
            this.scope.publishSuccessor(retiring, AsyncJob.one(new AsyncJob.Microtask("settle", () -> this.flush(null))));
         }
      }
   }

   private void flush(AsyncToken retiring) {
      List<JsPromise.Reaction> pending = this.reactions;
      this.reactions = null;
      boolean rejected = this.isRejected();
      Object value = this.settledValue();
      if (rejected) {
         this.scope.trackRejection(this);
      }

      List<AsyncJob> jobs = null;
      if (pending != null && !pending.isEmpty()) {
         jobs = new ArrayList<>(pending.size());

         for (JsPromise.Reaction reaction : pending) {
            jobs.add(this.reactionJob(reaction, rejected, value));
         }
      }

      this.scope.publishSuccessor(retiring, jobs);
   }

   private AsyncJob reactionJob(JsPromise.Reaction reaction, boolean rejected, Object value) {
      return new AsyncJob.Microtask("reaction", () -> reaction.react(rejected, value));
   }

   void addReaction(JsPromise.Reaction reaction) {
      this.rejectionHandled = true;
      if (this.cf.isDone()) {
         this.scope.publish(this.reactionJob(reaction, this.isRejected(), this.settledValue()));
      } else {
         if (this.reactions == null) {
            this.reactions = new ArrayList<>(2);
         }

         this.reactions.add(reaction);
      }
   }

   boolean isRejectionHandled() {
      return this.rejectionHandled;
   }

   void markRejectionHandled() {
      this.rejectionHandled = true;
   }

   boolean claimRejectionReport() {
      if (this.rejectionReported) {
         return false;
      } else {
         this.rejectionReported = true;
         return true;
      }
   }

   public CompletableFuture<Object> toFuture() {
      synchronized (this.cf) {
         if (this.javaView == null) {
            JsPromise.PromiseView view = new JsPromise.PromiseView(this);
            this.cf.whenComplete((value, error) -> {
               if (error == null) {
                  view.complete(Engine.toJava(value));
               } else {
                  Throwable cause = error instanceof CompletionException && error.getCause() != null ? error.getCause() : error;
                  view.completeExceptionally(cause);
               }
            });
            this.javaView = view;
         }

         return this.javaView;
      }
   }

   public Object await() {
      return this.join(null);
   }

   public Object join(Duration timeout) {
      try {
         return timeout == null ? this.toFuture().get() : this.toFuture().get(Math.max(timeout.toMillis(), 0L), TimeUnit.MILLISECONDS);
      } catch (InterruptedException var5) {
         Thread.currentThread().interrupt();
         throw new EngineInterruptedException();
      } catch (TimeoutException var6) {
         throw new EngineTimeoutException("timed out waiting for promise");
      } catch (ExecutionException var7) {
         Throwable cause = (Throwable)(var7.getCause() == null ? var7 : var7.getCause());
         if (cause instanceof RuntimeException re) {
            throw re;
         } else {
            throw new EngineException(cause.getMessage(), cause);
         }
      }
   }

   @Override
   public String toString() {
      if (!this.cf.isDone()) {
         return "Promise { <pending> }";
      } else {
         return this.isRejected() ? "Promise { <rejected> " + this.settledValue() + " }" : "Promise { " + this.settledValue() + " }";
      }
   }

   static final class PromiseView extends CompletableFuture<Object> {
      final JsPromise owner;

      PromiseView(JsPromise owner) {
         this.owner = owner;
      }
   }

   interface Reaction {
      void react(boolean var1, Object var2);
   }
}
