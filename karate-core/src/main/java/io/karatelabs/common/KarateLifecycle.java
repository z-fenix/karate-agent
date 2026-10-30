package io.karatelabs.common;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class KarateLifecycle {
   private static final Logger logger = LoggerFactory.getLogger(KarateLifecycle.class);
   public static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(10L);
   private static final List<Stoppable> REGISTERED = new ArrayList<>();
   private static final Object LOCK = new Object();
   private static KarateLifecycle.Phase phase = KarateLifecycle.Phase.RUNNING;
   private static long deadlineNanos;
   private static CompletableFuture<List<KarateLifecycle.StopResult>> inProgress;
   private static List<KarateLifecycle.StopResult> results = List.of();
   private static final ThreadLocal<Boolean> SHUTDOWN_THREAD = ThreadLocal.withInitial(() -> false);
   private static final AtomicBoolean HOOK_INSTALLED = new AtomicBoolean();
   private static ExecutorService stopPool;

   private KarateLifecycle() {
   }

   public static void register(Stoppable stoppable) {
      if (stoppable != null) {
         KarateLifecycle.Phase current;
         long budgetNanos;
         synchronized (LOCK) {
            current = phase;
            if (current == KarateLifecycle.Phase.RUNNING) {
               synchronized (REGISTERED) {
                  if (indexOf(stoppable) < 0) {
                     REGISTERED.add(stoppable);
                  }
               }

               return;
            }

            budgetNanos = current == KarateLifecycle.Phase.SHUTTING_DOWN ? Math.max(deadlineNanos - System.nanoTime(), 0L) : DEFAULT_TIMEOUT.toNanos();
         }

         logger.warn("registered while {}, stopping immediately: {}", current, name(stoppable));
         stopBounded(stoppable, budgetNanos);
      }
   }

   public static void unregister(Stoppable stoppable) {
      if (stoppable != null) {
         synchronized (REGISTERED) {
            int index = indexOf(stoppable);
            if (index >= 0) {
               REGISTERED.remove(index);
            }
         }
      }
   }

   public static List<Stoppable> running() {
      synchronized (REGISTERED) {
         return List.copyOf(REGISTERED);
      }
   }

   public static KarateLifecycle.Phase phase() {
      synchronized (LOCK) {
         return phase;
      }
   }

   public static void reset() {
      synchronized (LOCK) {
         if (phase == KarateLifecycle.Phase.SHUTTING_DOWN) {
            throw new IllegalStateException("cannot reset while shutting down");
         }

         phase = KarateLifecycle.Phase.RUNNING;
         results = List.of();
      }

      synchronized (REGISTERED) {
         REGISTERED.clear();
      }
   }

   public static List<KarateLifecycle.StopResult> shutdownAll() {
      return shutdownAll(DEFAULT_TIMEOUT);
   }

   public static List<KarateLifecycle.StopResult> shutdownAll(Duration timeout) {
      Duration limit = timeout != null && !timeout.isNegative() && !timeout.isZero() ? timeout : DEFAULT_TIMEOUT;
      CompletableFuture<List<KarateLifecycle.StopResult>> joinable = null;
      synchronized (LOCK) {
         if (phase == KarateLifecycle.Phase.SHUTTING_DOWN) {
            if (SHUTDOWN_THREAD.get()) {
               logger.debug("shutdown already in progress on this thread, returning partial summary");
               return List.copyOf(results);
            }

            joinable = inProgress;
         } else {
            phase = KarateLifecycle.Phase.SHUTTING_DOWN;
            deadlineNanos = System.nanoTime() + limit.toNanos();
            inProgress = new CompletableFuture<>();
            results = new ArrayList<>();
         }
      }

      return joinable != null ? join(joinable) : drain();
   }

   private static List<KarateLifecycle.StopResult> join(CompletableFuture<List<KarateLifecycle.StopResult>> other) {
      try {
         return other.get();
      } catch (InterruptedException var7) {
         Thread.currentThread().interrupt();
         synchronized (LOCK) {
            return List.copyOf(results);
         }
      } catch (ExecutionException var8) {
         logger.warn("shutdown in progress failed: {}", var8.getCause() == null ? var8 : var8.getCause().getMessage());
         synchronized (LOCK) {
            return List.copyOf(results);
         }
      }
   }

   private static List<KarateLifecycle.StopResult> drain() {
      List<Stoppable> snapshot;
      synchronized (REGISTERED) {
         snapshot = new ArrayList<>(REGISTERED);
         REGISTERED.clear();
      }

      Collections.reverse(snapshot);
      if (!snapshot.isEmpty()) {
         logger.debug("stopping {} registered component(s)", snapshot.size());
      }

      SHUTDOWN_THREAD.set(true);

      try {
         for (int i = 0; i < snapshot.size(); i++) {
            long remaining;
            synchronized (LOCK) {
               remaining = deadlineNanos - System.nanoTime();
            }

            long budgetNanos = remaining <= 0L ? 0L : remaining / (snapshot.size() - i);
            KarateLifecycle.StopResult result = stopBounded(snapshot.get(i), budgetNanos);
            synchronized (LOCK) {
               results.add(result);
            }
         }
      } finally {
         SHUTDOWN_THREAD.remove();
         List<KarateLifecycle.StopResult> summary;
         CompletableFuture<List<KarateLifecycle.StopResult>> future;
         synchronized (LOCK) {
            summary = List.copyOf(results);
            results = summary;
            future = inProgress;
            inProgress = null;
            phase = KarateLifecycle.Phase.STOPPED;
         }

         if (future != null) {
            future.complete(summary);
         }
      }

      synchronized (LOCK) {
         return List.copyOf(results);
      }
   }

   private static KarateLifecycle.StopResult stopBounded(Stoppable stoppable, long budgetNanos) {
      String name = name(stoppable);
      String kind = kind(stoppable);
      long start = System.nanoTime();
      if (budgetNanos <= 0L) {
         logger.warn("no time left to stop {}", name);
         return new KarateLifecycle.StopResult(name, kind, KarateLifecycle.Outcome.TIMED_OUT, Duration.ZERO);
      } else {
         logger.debug("stopping {} [{}]", name, kind);
         Future<?> future = pool().submit(() -> {
            SHUTDOWN_THREAD.set(true);

            try {
               stoppable.stop();
            } finally {
               SHUTDOWN_THREAD.remove();
            }
         });

         KarateLifecycle.Outcome outcome;
         try {
            future.get(budgetNanos, TimeUnit.NANOSECONDS);
            outcome = KarateLifecycle.Outcome.STOPPED;
         } catch (TimeoutException var11) {
            future.cancel(true);
            outcome = KarateLifecycle.Outcome.TIMED_OUT;
            logger.warn("timed out stopping {} after {}ms", name, TimeUnit.NANOSECONDS.toMillis(budgetNanos));
         } catch (ExecutionException var12) {
            Throwable cause = (Throwable)(var12.getCause() == null ? var12 : var12.getCause());
            outcome = KarateLifecycle.Outcome.FAILED;
            logger.warn("failed to stop {}: {}", name, cause.getMessage());
         } catch (InterruptedException var13) {
            Thread.currentThread().interrupt();
            future.cancel(true);
            outcome = KarateLifecycle.Outcome.TIMED_OUT;
            logger.warn("interrupted while stopping {}", name);
         }

         return new KarateLifecycle.StopResult(name, kind, outcome, Duration.ofNanos(System.nanoTime() - start));
      }
   }

   private static ExecutorService pool() {
      synchronized (LOCK) {
         if (stopPool == null) {
            stopPool = Executors.newCachedThreadPool(ThreadUtils.daemonFactory("karate-lifecycle-"));
         }

         return stopPool;
      }
   }

   public static void installShutdownHook() {
      if (HOOK_INSTALLED.compareAndSet(false, true)) {
         Thread hook = new Thread(() -> logSummary(shutdownAll(DEFAULT_TIMEOUT)), "karate-shutdown-hook");
         Runtime.getRuntime().addShutdownHook(hook);
         logger.debug("shutdown hook installed");
      }
   }

   private static void logSummary(List<KarateLifecycle.StopResult> summary) {
      if (!summary.isEmpty()) {
         int stopped = 0;
         int failed = 0;
         int timedOut = 0;

         for (KarateLifecycle.StopResult result : summary) {
            switch (result.outcome()) {
               case STOPPED:
                  stopped++;
                  break;
               case FAILED:
                  failed++;
                  break;
               case TIMED_OUT:
                  timedOut++;
            }
         }

         logger.info("shutdown complete: {} stopped, {} failed, {} timed out", new Object[]{stopped, failed, timedOut});
      }
   }

   public static Stoppable wrap(String name, String kind, ExecutorService executor) {
      return new KarateLifecycle.ExecutorStoppable(name, kind, executor);
   }

   private static int indexOf(Stoppable stoppable) {
      for (int i = 0; i < REGISTERED.size(); i++) {
         if (REGISTERED.get(i) == stoppable) {
            return i;
         }
      }

      return -1;
   }

   private static String name(Stoppable stoppable) {
      try {
         return stoppable.lifecycleName();
      } catch (Exception var2) {
         return stoppable.getClass().getName();
      }
   }

   private static String kind(Stoppable stoppable) {
      try {
         return stoppable.lifecycleKind();
      } catch (Exception var2) {
         return "unknown";
      }
   }

   private static final class ExecutorStoppable implements Stoppable {
      private static final long DRAIN_MILLIS = 5000L;
      private final String name;
      private final String kind;
      private final ExecutorService executor;

      private ExecutorStoppable(String name, String kind, ExecutorService executor) {
         this.name = name;
         this.kind = kind;
         this.executor = executor;
      }

      @Override
      public String lifecycleName() {
         return this.name;
      }

      @Override
      public String lifecycleKind() {
         return this.kind;
      }

      @Override
      public ExecutorService lifecycleExecutor() {
         return this.executor;
      }

      @Override
      public void stop() {
         if (this.executor != null && !this.executor.isTerminated()) {
            this.executor.shutdownNow();

            try {
               if (!this.executor.awaitTermination(5000L, TimeUnit.MILLISECONDS)) {
                  KarateLifecycle.logger.warn("executor did not terminate: {}", this.name);
               }
            } catch (InterruptedException var2) {
               Thread.currentThread().interrupt();
            }
         }
      }

      @Override
      public String toString() {
         return this.name;
      }
   }

   public static enum Outcome {
      STOPPED,
      FAILED,
      TIMED_OUT;
   }

   public static enum Phase {
      RUNNING,
      SHUTTING_DOWN,
      STOPPED;
   }

   public record StopResult(String name, String kind, KarateLifecycle.Outcome outcome, Duration duration) {
      @Override
      public String toString() {
         return this.name + " [" + this.kind + "] " + this.outcome + " in " + this.duration.toMillis() + "ms";
      }
   }
}
