package io.karatelabs.js;

import io.karatelabs.common.KarateLifecycle;
import io.karatelabs.common.Stoppable;
import io.karatelabs.common.ThreadUtils;
import io.karatelabs.parser.Node;
import io.karatelabs.parser.NodeType;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class AsyncSupport {
   private static final Logger logger = LoggerFactory.getLogger(AsyncSupport.class);
   private static final long POLL_MILLIS = 25L;
   private static final Object[] EMPTY_ARGS = new Object[0];
   private static final Node JOB_NODE = new Node(NodeType.ROOT);
   private static final Set<AsyncScope.TimerRecord> LIVE_TIMERS = ConcurrentHashMap.newKeySet();
   private static AsyncSupport.TimerScheduler scheduler;

   private AsyncSupport() {
   }

   static boolean onEngineThread(Engine engine) {
      return engine.jsLock.isHeldByCurrentThread();
   }

   static boolean isHostCancellation(Throwable e) {
      return e instanceof EngineInterruptedException;
   }

   static void checkStale(AsyncScope scope) {
      if (!scope.isOpen()) {
         throw new EngineInterruptedException();
      }
   }

   static void hostReturnFence() {
      AsyncActivation activation = AsyncActivation.current();
      if (activation != null && !activation.scope.isOpen()) {
         Thread.currentThread().interrupt();
         throw new EngineInterruptedException();
      }
   }

   static Object reasonOf(Throwable e) {
      if (e instanceof JsErrorException jex) {
         return jex.payload;
      } else {
         return e instanceof JsRejectionException jre ? jre.getReason() : Interpreter.buildCaughtError(e);
      }
   }

   static Object cancelledReason() {
      return JsErrorException.error("async activation cancelled").payload;
   }

   static Object settledResult(JsPromise promise) {
      promise.markRejectionHandled();
      Object value = promise.settledValue();
      if (promise.isRejected()) {
         throw new AsyncSupport.AwaitRejection(value);
      } else {
         return value;
      }
   }

   static CoreContext newJobContext(Engine engine) {
      return new CoreContext(engine.root(), null, 0, JOB_NODE, ContextScope.GLOBAL, engine.bindings);
   }

   static AsyncSupport.Completion invoke(Engine engine, JsCallable fn, Object[] args, Object thisObject) {
      CoreContext ctx = newJobContext(engine);
      if (thisObject != null) {
         ctx.thisObject = thisObject;
      }

      try {
         Object value = fn.call(ctx, args);
         hostReturnFence();
         return ctx.isError() ? new AsyncSupport.Completion(null, ctx.getErrorThrown(), true) : new AsyncSupport.Completion(value, null, false);
      } catch (RuntimeException var7) {
         if (!(var7 instanceof FlowControlSignal) && !isHostCancellation(var7)) {
            return var7 instanceof AsyncSupport.AwaitRejection ar
               ? new AsyncSupport.Completion(null, ar.reason, true)
               : new AsyncSupport.Completion(null, reasonOf(var7), true);
         } else {
            throw var7;
         }
      }
   }

   static Object callAsync(JsFunctionNode function, CoreContext functionContext, Object[] args) {
      Engine engine = functionContext.getEngine();
      if (engine == null) {
         throw JsErrorException.typeError("async function called outside an engine evaluation");
      } else {
         AsyncScope scope = engine.currentScope();
         return scope != null ? AsyncActivation.spawn(engine, scope, function, functionContext, args) : hostAsyncCall(engine, function, functionContext, args);
      }
   }

   private static Object hostAsyncCall(Engine engine, JsFunctionNode function, CoreContext functionContext, Object[] args) {
      engine.checkPoisoned();
      boolean outermost = engine.enterEvalScope();
      AsyncScope scope = engine.currentScope();

      JsPromise var8;
      try {
         engine.jsLock.lock();
         Engine previous = Engine.enter(engine);

         JsPromise promise;
         try {
            promise = AsyncActivation.spawn(engine, scope, function, functionContext, args);
         } finally {
            Engine.exit(previous);
            if (engine.jsLock.isHeldByCurrentThread()) {
               engine.jsLock.unlock();
            }
         }

         drain(engine, scope);
         promise.markRejectionHandled();
         reportUnhandledRejections(engine, scope);
         var8 = promise;
      } finally {
         if (outermost) {
            engine.closeScope(scope, "host async call complete");
         }

         engine.exitEvalScope(outermost);
      }

      return var8;
   }

   static Object await(Object value, CoreContext context) {
      Engine engine = context.getEngine();
      AsyncScope scope = engine == null ? null : engine.currentScope();
      if (scope == null) {
         return value;
      } else {
         JsPromise promise = toPromise(engine, scope, value);
         if (promise == null) {
            return value;
         } else {
            AsyncActivation activation = AsyncActivation.current();
            if (activation != null && activation.engine == engine) {
               return activation.awaitOn(promise);
            } else {
               return promise.isSettled() ? settledResult(promise) : pumpUntilSettled(engine, scope, promise);
            }
         }
      }
   }

   static JsPromise toPromise(Engine engine, AsyncScope scope, Object value) {
      if (value instanceof JsPromise promise) {
         return promise;
      } else if (value instanceof CompletionStage<?> stage) {
         return wrapStage(engine, scope, stage);
      } else if (value instanceof ObjectLike obj && readThen(engine, obj) instanceof JsCallable callable) {
         JsPromise target = new JsPromise(engine, scope);
         adoptThenable(target, obj, callable, null);
         return target;
      } else {
         return null;
      }
   }

   static void runJob(Engine engine, AsyncJob job) {
      engine.jsLock.lock();
      Engine previous = Engine.enter(engine);

      try {
         job.run(engine);
      } finally {
         Engine.exit(previous);
         engine.jsLock.unlock();
         if (job.token != null) {
            job.token.release();
         }
      }
   }

   static Object finishScope(Engine engine, AsyncScope scope, Object result) {
      drain(engine, scope);
      if (result instanceof JsPromise promise) {
         promise.markRejectionHandled();
         reportUnhandledRejections(engine, scope);
         if (!promise.isSettled()) {
            return promise;
         } else {
            Object value = promise.settledValue();
            if (promise.isRejected()) {
               reportFailure(engine, "uncaught (in promise)", value);
            }

            return value;
         }
      } else {
         reportUnhandledRejections(engine, scope);
         return result;
      }
   }

   static void drain(Engine engine, AsyncScope scope) {
      long deadlineNanos = drainDeadline(engine);

      while (!Thread.currentThread().isInterrupted()) {
         if (scope.isCancelRequested()) {
            throw new EngineInterruptedException();
         }

         if (scope.isQuiescent()) {
            return;
         }

         if (deadlineNanos != 0L && System.nanoTime() > deadlineNanos) {
            throw new EngineTimeoutException("async drain timed out");
         }

         AsyncJob job = scope.takeJob(25L);
         if (job instanceof AsyncJob.Control control) {
            job.token.release();
            throw new EngineInterruptedException(control.reason);
         }

         if (job != null) {
            runJob(engine, job);
         }
      }

      throw new EngineInterruptedException();
   }

   private static long drainDeadline(Engine engine) {
      Duration cap = engine.asyncDrainTimeout();
      return cap == null ? 0L : System.nanoTime() + cap.toNanos();
   }

   static Object pumpUntilSettled(Engine engine, AsyncScope scope, JsPromise promise) {
      long deadlineNanos = drainDeadline(engine);
      int holds = engine.releaseJsLock();

      while (!promise.isSettled()) {
         if (Thread.currentThread().isInterrupted() || scope.isCancelRequested()) {
            throw new EngineInterruptedException();
         }

         if (deadlineNanos != 0L && System.nanoTime() > deadlineNanos) {
            throw new EngineTimeoutException("async drain timed out");
         }

         AsyncJob job = scope.takeJob(25L);
         if (job instanceof AsyncJob.Control control) {
            job.token.release();
            throw new EngineInterruptedException(control.reason);
         }

         if (job != null) {
            runJob(engine, job);
         } else if (scope.isQuiescent() && !promise.isSettled()) {
            throw new EngineException("await on a promise that can never settle", null);
         }
      }

      engine.reacquireJsLock(holds);
      return settledResult(promise);
   }

   static void resolveValue(JsPromise target, Object value, AsyncToken retiring) {
      if (value == target) {
         target.settle(true, JsErrorException.typeError("Chaining cycle detected for promise").payload, retiring);
      } else if (value instanceof JsPromise inner) {
         adopt(target, inner, retiring);
      } else if (value instanceof CompletionStage<?> stage) {
         adopt(target, wrapStage(target.engine, target.scope, stage), retiring);
      } else {
         if (value instanceof ObjectLike obj) {
            Object thenFn;
            try {
               thenFn = readThen(target.engine, obj);
            } catch (RuntimeException var6) {
               if (!(var6 instanceof FlowControlSignal) && !isHostCancellation(var6)) {
                  target.settle(true, reasonOf(var6), retiring);
                  return;
               }

               throw var6;
            }

            if (thenFn instanceof JsCallable callable) {
               adoptThenable(target, obj, callable, retiring);
               return;
            }
         }

         target.settle(false, value, retiring);
      }
   }

   private static Object readThen(Engine engine, ObjectLike obj) {
      CoreContext ctx = newJobContext(engine);
      Object thenFn = obj.getMember("then", obj, ctx);
      if (ctx.isError()) {
         throw new JsErrorException(asJsError(ctx.getErrorThrown()));
      } else {
         return thenFn;
      }
   }

   private static JsError asJsError(Object reason) {
      return reason instanceof JsError je ? je : JsErrorException.error(String.valueOf(reason)).payload;
   }

   private static void adopt(JsPromise target, JsPromise inner, AsyncToken retiring) {
      inner.markRejectionHandled();
      if (inner.isSettled()) {
         target.settle(inner.isRejected(), inner.settledValue(), retiring);
      } else {
         inner.addReaction((rejected, value) -> {
            if (rejected) {
               target.settle(true, value, null);
            } else {
               resolveValue(target, value, null);
            }
         });
         if (retiring != null) {
            retiring.release();
         }
      }
   }

   private static void adoptThenable(JsPromise target, Object thenable, JsCallable then, AsyncToken retiring) {
      AtomicBoolean claimed = new AtomicBoolean();
      JsCallable resolveFn = (ctx, args) -> {
         if (claimed.compareAndSet(false, true)) {
            settleFromCallback(target, arg(args, 0), false);
         }

         return Terms.UNDEFINED;
      };
      JsCallable rejectFn = (ctx, args) -> {
         if (claimed.compareAndSet(false, true)) {
            settleFromCallback(target, arg(args, 0), true);
         }

         return Terms.UNDEFINED;
      };
      AsyncJob job = new AsyncJob.Microtask("thenable", () -> {
         AsyncSupport.Completion outcome = invoke(target.engine, then, new Object[]{resolveFn, rejectFn}, thenable);
         if (outcome.threw() && claimed.compareAndSet(false, true)) {
            target.settle(true, outcome.reason(), null);
         }
      });
      target.scope.publishSuccessor(retiring, AsyncJob.one(job));
   }

   static void settleFromCallback(JsPromise target, Object value, boolean rejected) {
      if (onEngineThread(target.engine)) {
         if (rejected) {
            target.settle(true, value, null);
         } else {
            resolveValue(target, value, null);
         }
      } else {
         target.scope.publish(new AsyncJob.Microtask("resolve", () -> {
            if (rejected) {
               target.settle(true, value, null);
            } else {
               resolveValue(target, value, null);
            }
         }));
      }
   }

   static Object arg(Object[] args, int index) {
      return args != null && args.length > index ? args[index] : Terms.UNDEFINED;
   }

   static JsPromise wrapStage(Engine engine, AsyncScope scope, CompletionStage<?> stage) {
      if (stage instanceof JsPromise.PromiseView view) {
         return view.owner;
      } else {
         JsPromise cached = scope.cachedStage(stage);
         if (cached != null) {
            return cached;
         } else {
            JsPromise promise = new JsPromise(engine, scope);
            scope.cacheStage(stage, promise);
            AsyncToken token = scope.acquire("subscription");
            if (token == null) {
               return promise;
            } else {
               stage.whenComplete((value, error) -> {
                  if (error == null) {
                     promise.settle(false, value, token);
                  } else {
                     try {
                        promise.settle(true, reasonFromStage(error), token);
                     } catch (EngineInterruptedException var6x) {
                        scope.requestCancel("interop stage cancelled");
                        token.release();
                     }
                  }
               });
               return promise;
            }
         }
      }
   }

   static Object reasonFromStage(Throwable error) {
      Throwable e = error;
      if ((error instanceof CompletionException || error instanceof ExecutionException) && error.getCause() != null) {
         e = error.getCause();
      }

      if (e instanceof JsRejectionException jre) {
         return jre.getReason();
      } else if (e instanceof EngineInterruptedException eie) {
         throw eie;
      } else {
         return e instanceof CancellationException ? JsErrorException.error("stage was cancelled").payload : Interpreter.buildCaughtError(e);
      }
   }

   static boolean retireTimer(AsyncScope.TimerRecord record) {
      LIVE_TIMERS.remove(record);
      if (!record.cancel()) {
         return false;
      } else {
         ScheduledFuture<?> future = record.future;
         if (future != null) {
            future.cancel(false);
         }

         record.token.release();
         return true;
      }
   }

   private static void forgetTimer(AsyncScope.TimerRecord record) {
      LIVE_TIMERS.remove(record);
   }

   private static synchronized AsyncSupport.TimerScheduler scheduler() {
      if (scheduler == null || scheduler.isClosed() || scheduler.executor.isShutdown()) {
         scheduler = new AsyncSupport.TimerScheduler(Executors.newSingleThreadScheduledExecutor(ThreadUtils.daemonFactory("js-timer-", false)));
         KarateLifecycle.register(scheduler);
      }

      return scheduler;
   }

   private static Engine timerEngine(Context context) {
      Engine engine = context == null ? null : context.getEngine();
      if (engine != null && engine == Engine.current()) {
         return engine;
      } else {
         throw JsErrorException.typeError("setTimeout: no engine is executing on this thread");
      }
   }

   static Object setTimeout(Context context, Object[] args) {
      Engine engine = timerEngine(context);
      AsyncScope scope = engine.currentScope();
      if (scope == null) {
         throw JsErrorException.typeError("setTimeout requires an active evaluation");
      } else if (arg(args, 0) instanceof JsCallable target) {
         long delay = coerceDelay(arg(args, 1));
         Object[] extra = args.length > 2 ? Arrays.copyOfRange(args, 2, args.length) : EMPTY_ARGS;
         if (KarateLifecycle.phase() != KarateLifecycle.Phase.RUNNING) {
            throw new EngineInterruptedException("setTimeout: karate is shutting down");
         } else {
            AsyncToken token = scope.acquire("timer");
            if (token == null) {
               throw new EngineInterruptedException();
            } else {
               AsyncActivation.anyAsync = true;
               int id = scope.nextTimerId();
               AsyncScope.TimerRecord record = new AsyncScope.TimerRecord(scope, id, token);
               scope.addTimer(record);
               AsyncSupport.TimerScheduler timer = scheduler();
               if (!timer.register(record)) {
                  scope.removeTimer(id);
                  retireTimer(record);
                  throw new EngineInterruptedException("setTimeout: karate is shutting down");
               } else {
                  ScheduledFuture<?> future;
                  try {
                     future = timer.schedule(() -> {
                        if (record.fire()) {
                           forgetTimer(record);
                           scope.removeTimer(id);
                           scope.publishSuccessor(record.token, AsyncJob.one(new AsyncJob.Timer(id, target, extra)));
                        }
                     }, delay);
                  } catch (RuntimeException var15) {
                     scope.removeTimer(id);
                     retireTimer(record);
                     throw new EngineInterruptedException("setTimeout: " + var15);
                  }

                  record.future = future;
                  if (!record.isLive()) {
                     future.cancel(false);
                  }

                  return id;
               }
            }
         }
      } else {
         throw JsErrorException.typeError("setTimeout: callback is not a function");
      }
   }

   static Object clearTimeout(Context context, Object[] args) {
      Engine engine = context == null ? null : context.getEngine();
      AsyncScope scope = engine == null ? null : engine.currentScope();
      if (scope == null) {
         return Terms.UNDEFINED;
      } else {
         Object id = arg(args, 0);
         Number n = id instanceof Number number ? number : Terms.objectToNumber(id);
         if (n != null && !Double.isNaN(n.doubleValue())) {
            AsyncScope.TimerRecord record = scope.removeTimer(n.intValue());
            if (record != null) {
               retireTimer(record);
            }

            return Terms.UNDEFINED;
         } else {
            return Terms.UNDEFINED;
         }
      }
   }

   static long coerceDelay(Object value) {
      Number n;
      try {
         n = Terms.objectToNumber(value);
      } catch (RuntimeException var4) {
         return 0L;
      }

      if (n == null) {
         return 0L;
      } else {
         double d = n.doubleValue();
         if (Double.isNaN(d) || d <= 0.0) {
            return 0L;
         } else {
            return Double.isInfinite(d) ? 2147483647L : (long)Math.floor(d);
         }
      }
   }

   static void invokeQueuedCallback(Engine engine, JsCallable callback, Object[] args) {
      AsyncSupport.Completion outcome = invoke(engine, callback, args, null);
      if (outcome.threw()) {
         reportFailure(engine, "uncaught exception in timer callback", outcome.reason());
      }
   }

   static void reportUnhandledRejections(Engine engine, AsyncScope scope) {
      List<JsPromise> tracked = scope.trackedRejections();
      if (!tracked.isEmpty()) {
         List<JsPromise> terminal = new ArrayList<>(1);

         for (JsPromise promise : tracked) {
            if (!promise.isRejectionHandled() && promise.claimRejectionReport()) {
               terminal.add(promise);
            }
         }

         if (!terminal.isEmpty()) {
            for (int i = 1; i < terminal.size(); i++) {
               warn(engine, "unhandled promise rejection: " + describe(terminal.get(i).settledValue()));
            }

            reportFailure(engine, "unhandled promise rejection", terminal.get(0).settledValue());
         }
      }
   }

   private static void reportFailure(Engine engine, String prefix, Object reason) {
      if (engine.asyncRejectionWarnOnly()) {
         warn(engine, prefix + ": " + describe(reason));
      } else {
         throw asEngineException(prefix, reason);
      }
   }

   private static void warn(Engine engine, String message) {
      logger.warn(message);
      Consumer<String> consumer = engine.root().onConsoleLog;
      if (consumer != null) {
         consumer.accept(message);
      }
   }

   static EngineException asEngineException(String prefix, Object reason) {
      String name = null;
      String jsMessage = null;
      if (reason instanceof ObjectLike obj) {
         if (obj.getMember("name") instanceof String s && !s.isEmpty()) {
            name = s;
         }

         if (obj.getMember("message") instanceof String s) {
            jsMessage = s;
         }
      }

      if (jsMessage == null) {
         jsMessage = String.valueOf(reason);
      }

      return new EngineException(prefix + ": " + (name == null ? "" : name + ": ") + jsMessage, null, name, jsMessage);
   }

   private static String describe(Object reason) {
      return reason instanceof ObjectLike obj && obj.getMember("message") instanceof String s
         ? (obj.getMember("name") instanceof String n && !n.isEmpty() ? n + ": " : "") + s
         : String.valueOf(reason);
   }

   static final class AwaitRejection extends RuntimeException {
      final transient Object reason;

      AwaitRejection(Object reason) {
         super(null, null, false, false);
         this.reason = reason;
      }
   }

   record Completion(Object value, Object reason, boolean threw) {
   }

   private static final class TimerScheduler implements Stoppable {
      private final Object gate = new Object();
      private final ScheduledExecutorService executor;
      private final Stoppable delegate;
      private boolean closed;

      TimerScheduler(ScheduledExecutorService executor) {
         this.executor = executor;
         this.delegate = KarateLifecycle.wrap("js-timer", "executor", executor);
      }

      boolean isClosed() {
         synchronized (this.gate) {
            return this.closed;
         }
      }

      boolean register(AsyncScope.TimerRecord record) {
         synchronized (this.gate) {
            if (this.closed) {
               return false;
            } else {
               AsyncSupport.LIVE_TIMERS.add(record);
               return true;
            }
         }
      }

      ScheduledFuture<?> schedule(Runnable command, long delayMillis) {
         return this.executor.schedule(command, delayMillis, TimeUnit.MILLISECONDS);
      }

      @Override
      public String lifecycleName() {
         return "js-timer";
      }

      @Override
      public String lifecycleKind() {
         return "executor";
      }

      @Override
      public ExecutorService lifecycleExecutor() {
         return this.executor;
      }

      @Override
      public void stop() {
         synchronized (this.gate) {
            this.closed = true;
         }

         this.delegate.stop();

         for (AsyncScope.TimerRecord record : new ArrayList<>(AsyncSupport.LIVE_TIMERS)) {
            record.scope.removeTimer(record.id);
            AsyncSupport.retireTimer(record);
         }
      }

      @Override
      public String toString() {
         return "js-timer";
      }
   }
}
