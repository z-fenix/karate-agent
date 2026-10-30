package io.karatelabs.js;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicInteger;

final class AsyncScope {
   private final Engine engine;
   private final Object lock = new Object();
   private boolean closed;
   private boolean cancelRequested;
   private String cancelReason;
   private int liveTokens;
   private ArrayDeque<AsyncJob> microtasks;
   private ArrayDeque<AsyncJob> macrotasks;
   private List<AsyncActivation> activations;
   private List<GeneratorActivation> generators;
   private Map<Integer, AsyncScope.TimerRecord> timers;
   private Map<CompletionStage<?>, JsPromise> stageCache;
   private List<JsPromise> trackedRejections;
   private final AtomicInteger timerIds = new AtomicInteger();

   AsyncScope(Engine engine) {
      this.engine = engine;
   }

   Engine engine() {
      return this.engine;
   }

   boolean isOpen() {
      synchronized (this.lock) {
         return !this.closed;
      }
   }

   boolean isCancelRequested() {
      synchronized (this.lock) {
         return this.cancelRequested;
      }
   }

   String cancelReason() {
      synchronized (this.lock) {
         return this.cancelReason;
      }
   }

   boolean isQuiescent() {
      synchronized (this.lock) {
         return this.liveTokens == 0 && this.queuesEmpty();
      }
   }

   private boolean queuesEmpty() {
      return (this.microtasks == null || this.microtasks.isEmpty()) && (this.macrotasks == null || this.macrotasks.isEmpty());
   }

   private ArrayDeque<AsyncJob> queueFor(AsyncJob job) {
      if (job.macrotask()) {
         if (this.macrotasks == null) {
            this.macrotasks = new ArrayDeque<>();
         }

         return this.macrotasks;
      } else {
         if (this.microtasks == null) {
            this.microtasks = new ArrayDeque<>();
         }

         return this.microtasks;
      }
   }

   int liveTokenCount() {
      synchronized (this.lock) {
         return this.liveTokens;
      }
   }

   AsyncToken acquire(String kind) {
      synchronized (this.lock) {
         if (this.closed) {
            return null;
         } else {
            this.liveTokens++;
            return new AsyncToken(this, kind);
         }
      }
   }

   void decrement() {
      synchronized (this.lock) {
         this.liveTokens--;
         this.lock.notifyAll();
      }
   }

   boolean publishSuccessor(AsyncToken oldToken, List<AsyncJob> work) {
      synchronized (this.lock) {
         boolean published = false;
         if (!this.closed && work != null && !work.isEmpty()) {
            for (AsyncJob job : work) {
               this.liveTokens++;
               job.token = new AsyncToken(this, "job");
               this.queueFor(job).add(job);
            }

            published = true;
            this.lock.notifyAll();
         }

         if (oldToken != null) {
            oldToken.release();
         }

         return published;
      }
   }

   boolean publish(AsyncJob job) {
      return this.publishSuccessor(null, AsyncJob.one(job));
   }

   AsyncJob takeJob(long timeoutMillis) {
      synchronized (this.lock) {
         long deadline = System.nanoTime() + timeoutMillis * 1000000L;

         while (this.microtasks == null || this.microtasks.isEmpty()) {
            if (this.macrotasks != null && !this.macrotasks.isEmpty()) {
               return this.macrotasks.poll();
            }

            if (this.closed || this.liveTokens == 0) {
               return null;
            }

            long remaining = (deadline - System.nanoTime()) / 1000000L;
            if (remaining <= 0L) {
               return null;
            }

            try {
               this.lock.wait(remaining);
            } catch (InterruptedException var10) {
               Thread.currentThread().interrupt();
               throw new EngineInterruptedException();
            }
         }

         return this.microtasks.poll();
      }
   }

   void addActivation(AsyncActivation activation) {
      synchronized (this.lock) {
         if (this.activations == null) {
            this.activations = new ArrayList<>(2);
         }

         this.activations.add(activation);
      }
   }

   boolean addGenerator(GeneratorActivation activation) {
      synchronized (this.lock) {
         if (this.closed) {
            return false;
         } else {
            if (this.generators == null) {
               this.generators = new ArrayList<>(2);
            }

            this.generators.add(activation);
            return true;
         }
      }
   }

   void removeGenerator(GeneratorActivation activation) {
      synchronized (this.lock) {
         if (this.generators != null) {
            this.generators.remove(activation);
         }
      }
   }

   void removeActivation(AsyncActivation activation) {
      synchronized (this.lock) {
         if (this.activations != null) {
            this.activations.remove(activation);
         }
      }
   }

   void requestCancel(String reason) {
      synchronized (this.lock) {
         if (!this.closed && !this.cancelRequested) {
            this.cancelRequested = true;
            this.cancelReason = reason;
            AsyncJob control = new AsyncJob.Control(reason);
            this.liveTokens++;
            control.token = new AsyncToken(this, "control");
            this.queueFor(control).add(control);
            this.lock.notifyAll();
         }
      }
   }

   int nextTimerId() {
      return this.timerIds.incrementAndGet();
   }

   void addTimer(AsyncScope.TimerRecord record) {
      synchronized (this.lock) {
         if (this.timers == null) {
            this.timers = new LinkedHashMap<>();
         }

         this.timers.put(record.id, record);
      }
   }

   AsyncScope.TimerRecord removeTimer(int id) {
      synchronized (this.lock) {
         return this.timers == null ? null : this.timers.remove(id);
      }
   }

   JsPromise cachedStage(CompletionStage<?> stage) {
      synchronized (this.lock) {
         return this.stageCache == null ? null : this.stageCache.get(stage);
      }
   }

   void cacheStage(CompletionStage<?> stage, JsPromise promise) {
      synchronized (this.lock) {
         if (!this.closed) {
            if (this.stageCache == null) {
               this.stageCache = new IdentityHashMap<>();
            }

            this.stageCache.put(stage, promise);
         }
      }
   }

   int stageCacheSize() {
      synchronized (this.lock) {
         return this.stageCache == null ? 0 : this.stageCache.size();
      }
   }

   void trackRejection(JsPromise promise) {
      synchronized (this.lock) {
         if (!this.closed) {
            if (this.trackedRejections == null) {
               this.trackedRejections = new ArrayList<>(2);
            }

            this.trackedRejections.add(promise);
         }
      }
   }

   List<JsPromise> trackedRejections() {
      synchronized (this.lock) {
         return (List<JsPromise>)(this.trackedRejections == null ? List.of() : new ArrayList<>(this.trackedRejections));
      }
   }

   List<Object> close(String reason, long teardownMillis) {
      List<AsyncJob> pending = null;
      List<AsyncScope.TimerRecord> timerSnapshot = null;
      List<AsyncActivation> live;
      List<GeneratorActivation> liveGenerators;
      synchronized (this.lock) {
         if (this.closed) {
            return List.of();
         }

         this.closed = true;
         if (this.cancelReason == null) {
            this.cancelReason = reason;
         }

         live = (List<AsyncActivation>)(this.activations == null ? List.of() : new ArrayList<>(this.activations));
         liveGenerators = (List<GeneratorActivation>)(this.generators == null ? List.of() : new ArrayList<>(this.generators));
         if (!this.queuesEmpty()) {
            pending = new ArrayList<>();
            if (this.microtasks != null) {
               pending.addAll(this.microtasks);
               this.microtasks.clear();
            }

            if (this.macrotasks != null) {
               pending.addAll(this.macrotasks);
               this.macrotasks.clear();
            }
         }

         if (this.timers != null && !this.timers.isEmpty()) {
            timerSnapshot = new ArrayList<>(this.timers.values());
            this.timers.clear();
         }

         this.stageCache = null;
         this.lock.notifyAll();
      }

      if (timerSnapshot != null) {
         for (AsyncScope.TimerRecord record : timerSnapshot) {
            AsyncSupport.retireTimer(record);
         }
      }

      if (pending != null) {
         for (AsyncJob job : pending) {
            if (job.token != null) {
               job.token.release();
            }
         }
      }

      if (live.isEmpty() && liveGenerators.isEmpty()) {
         return List.of();
      } else {
         long deadline = System.nanoTime() + teardownMillis * 1000000L;

         for (AsyncActivation activation : live) {
            activation.cancel();
         }

         for (GeneratorActivation g : liveGenerators) {
            g.cancel();
         }

         List<Object> stuck = new ArrayList<>(0);
         boolean wasInterrupted = Thread.interrupted();

         try {
            for (AsyncActivation activation : live) {
               long remaining = (deadline - System.nanoTime()) / 1000000L;
               if (!activation.awaitTermination(Math.max(remaining, 1L))) {
                  stuck.add(activation);
               }

               wasInterrupted |= Thread.interrupted();
            }

            for (GeneratorActivation g : liveGenerators) {
               long remaining = (deadline - System.nanoTime()) / 1000000L;
               if (!g.awaitTermination(Math.max(remaining, 1L))) {
                  stuck.add(g);
               }

               wasInterrupted |= Thread.interrupted();
            }
         } finally {
            if (wasInterrupted) {
               Thread.currentThread().interrupt();
            }
         }

         return stuck;
      }
   }

   static final class TimerRecord {
      private static final int LIVE = 0;
      private static final int FIRED = 1;
      private static final int CANCELLED = 2;
      private final AtomicInteger state = new AtomicInteger(0);
      final AsyncScope scope;
      final AsyncToken token;
      final int id;
      volatile ScheduledFuture<?> future;

      TimerRecord(AsyncScope scope, int id, AsyncToken token) {
         this.scope = scope;
         this.id = id;
         this.token = token;
      }

      boolean fire() {
         return this.state.compareAndSet(0, 1);
      }

      boolean cancel() {
         return this.state.compareAndSet(0, 2);
      }

      boolean isLive() {
         return this.state.get() == 0;
      }
   }
}
