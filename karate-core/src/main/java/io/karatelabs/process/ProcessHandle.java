
package io.karatelabs.process;

import io.karatelabs.js.JavaInvokable;

import io.karatelabs.js.JavaCallable;
import io.karatelabs.js.SimpleObject;
import io.karatelabs.output.LogContext;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Predicate;
import org.slf4j.Logger;

public class ProcessHandle implements SimpleObject {
   private static final Logger logger = LogContext.RUNTIME_LOGGER;
   private static final List<String> KEYS = List.of(
      "stdOut",
      "stdErr",
      "exitCode",
      "alive",
      "pid",
      "waitSync",
      "waitForOutput",
      "close",
      "signal",
      "start",
      "waitForPort",
      "waitForHttp",
      "onStdOut",
      "onStdErr"
   );
   private static final Set<ProcessHandle> LIVE_HANDLES = ConcurrentHashMap.newKeySet();
   private final ProcessConfig config;
   private Process process;
   private final CompletableFuture<Integer> exitFuture;
   private final StringBuilder stdoutBuffer = new StringBuilder();
   private final StringBuilder stderrBuffer = new StringBuilder();
   private final AtomicBoolean closed = new AtomicBoolean(false);
   private final AtomicBoolean started = new AtomicBoolean(false);
   private final CopyOnWriteArrayList<Predicate<String>> waitPredicates = new CopyOnWriteArrayList<>();
   private final ConcurrentHashMap<Predicate<String>, CompletableFuture<String>> waitFutures = new ConcurrentHashMap<>();
   private final CopyOnWriteArrayList<Consumer<String>> stdOutListeners = new CopyOnWriteArrayList<>();
   private final CopyOnWriteArrayList<Consumer<String>> stdErrListeners = new CopyOnWriteArrayList<>();
   private Consumer<Object> signalConsumer;
   private ExecutorService executor;
   private volatile int exitCode = -1;
   private CompletableFuture<Void> stdoutReaderDone;
   private CompletableFuture<Void> stderrReaderDone;
   private final LogContext capturedLogContext;

   private ProcessHandle(ProcessConfig config) {
      this.config = config;
      this.exitFuture = new CompletableFuture<>();
      this.capturedLogContext = config.logToContext() ? LogContext.get() : null;
   }

   public static ProcessHandle create(ProcessConfig config) {
      return new ProcessHandle(config);
   }

   public static ProcessHandle start(ProcessConfig config) {
      ProcessHandle handle = new ProcessHandle(config);
      handle.start();
      return handle;
   }

   public ProcessHandle start() {
      if (!this.started.compareAndSet(false, true)) {
         throw new RuntimeException("process already started");
      } else {
         try {
            java.lang.ProcessBuilder pb = new java.lang.ProcessBuilder(this.config.args());
            if (this.config.workingDir() != null) {
               pb.directory(this.config.workingDir().toFile());
            }

            if (!this.config.env().isEmpty()) {
               pb.environment().putAll(this.config.env());
            }

            pb.redirectErrorStream(this.config.redirectErrorStream());
            logger.debug("starting process: {}", this.config.args());
            this.process = pb.start();
            this.executor = Executors.newVirtualThreadPerTaskExecutor();
            LIVE_HANDLES.add(this);
            this.startStreamReaders();
            this.startExitWaiter();
            return this;
         } catch (Exception var2) {
            throw new RuntimeException("failed to start process: " + var2.getMessage(), var2);
         }
      }
   }

   public ProcessHandle onStdOut(Consumer<String> listener) {
      this.stdOutListeners.add(listener);
      return this;
   }

   public ProcessHandle onStdErr(Consumer<String> listener) {
      this.stdErrListeners.add(listener);
      return this;
   }

   private void startStreamReaders() {
      this.stdoutReaderDone = new CompletableFuture<>();
      this.executor.submit(() -> {
         Thread.currentThread().setName("process-stdout-reader");

         try {
            String line;
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(this.process.getInputStream()))) {
               while ((line = reader.readLine()) != null) {
                  this.handleStdout(line);
               }
            } catch (Exception var11) {
               if (!this.closed.get()) {
                  logger.warn("stdout reader error: {}", var11.getMessage());
               }
            }
         } finally {
            this.stdoutReaderDone.complete(null);
         }
      });
      if (!this.config.redirectErrorStream()) {
         this.stderrReaderDone = new CompletableFuture<>();
         this.executor.submit(() -> {
            Thread.currentThread().setName("process-stderr-reader");

            try {
               String line;
               try (BufferedReader reader = new BufferedReader(new InputStreamReader(this.process.getErrorStream()))) {
                  while ((line = reader.readLine()) != null) {
                     this.handleStderr(line);
                  }
               } catch (Exception var11) {
                  if (!this.closed.get()) {
                     logger.warn("stderr reader error: {}", var11.getMessage());
                  }
               }
            } finally {
               this.stderrReaderDone.complete(null);
            }
         });
      }
   }

   private void startExitWaiter() {
      this.executor.submit(() -> {
         Thread.currentThread().setName("process-exit-waiter");

         try {
            int code = this.process.waitFor();
            this.exitCode = code;
            this.exitFuture.complete(code);
            logger.debug("process exited with code: {}", code);
         } catch (Exception var5) {
            this.exitFuture.completeExceptionally(var5);
         } finally {
            LIVE_HANDLES.remove(this);
         }
      });
   }

   private void handleStdout(String line) {
      synchronized (this.stdoutBuffer) {
         this.stdoutBuffer.append(line).append('\n');
      }

      if (this.capturedLogContext != null) {
         this.capturedLogContext.log(line);
      }

      if (this.config.listener() != null) {
         try {
            this.config.listener().accept(line);
         } catch (Exception var6) {
            logger.warn("listener error: {}", var6.getMessage());
         }
      }

      for (Consumer<String> listener : this.stdOutListeners) {
         try {
            listener.accept(line);
         } catch (Exception var5) {
            logger.warn("stdOut listener error: {}", var5.getMessage());
         }
      }

      this.checkWaitPredicates(line);
   }

   private void handleStderr(String line) {
      synchronized (this.stderrBuffer) {
         this.stderrBuffer.append(line).append('\n');
      }

      if (this.capturedLogContext != null) {
         this.capturedLogContext.log(line);
      }

      if (this.config.errorListener() != null) {
         try {
            this.config.errorListener().accept(line);
         } catch (Exception var6) {
            logger.warn("error listener error: {}", var6.getMessage());
         }
      }

      for (Consumer<String> listener : this.stdErrListeners) {
         try {
            listener.accept(line);
         } catch (Exception var5) {
            logger.warn("stdErr listener error: {}", var5.getMessage());
         }
      }

      this.checkWaitPredicates(line);
   }

   private void checkWaitPredicates(String line) {
      for (Predicate<String> predicate : this.waitPredicates) {
         try {
            if (predicate.test(line)) {
               CompletableFuture<String> future = this.waitFutures.remove(predicate);
               if (future != null) {
                  this.waitPredicates.remove(predicate);
                  future.complete(line);
               }
            }
         } catch (Exception var5) {
            logger.warn("waitForOutput predicate error: {}", var5.getMessage());
         }
      }
   }

   public int waitSync() {
      try {
         int code = this.exitFuture.get();
         this.waitForStreamReaders();
         return code;
      } catch (Exception var2) {
         throw new RuntimeException("error waiting for process", var2);
      }
   }

   public int waitSync(long timeoutMillis) {
      try {
         int code = this.exitFuture.get(timeoutMillis, TimeUnit.MILLISECONDS);
         this.waitForStreamReaders();
         return code;
      } catch (TimeoutException var4) {
         throw new RuntimeException("process timed out after " + timeoutMillis + "ms");
      } catch (Exception var5) {
         throw new RuntimeException("error waiting for process", var5);
      }
   }

   private void waitForStreamReaders() {
      try {
         if (this.stdoutReaderDone != null && !this.stdoutReaderDone.isDone()) {
            this.stdoutReaderDone.get(500L, TimeUnit.MILLISECONDS);
         }

         if (this.stderrReaderDone != null && !this.stderrReaderDone.isDone()) {
            this.stderrReaderDone.get(500L, TimeUnit.MILLISECONDS);
         }
      } catch (TimeoutException var2) {
         logger.warn("timeout waiting for stream readers - this should not happen");
      } catch (Exception var3) {
         logger.debug("error waiting for stream readers: {}", var3.getMessage());
      }
   }

   public String waitForOutput(Predicate<String> predicate) {
      return this.waitForOutput(predicate, 0L);
   }

   public String waitForOutput(Predicate<String> predicate, long timeoutMillis) {
      CompletableFuture<String> future = new CompletableFuture<>();
      this.waitPredicates.add(predicate);
      this.waitFutures.put(predicate, future);

      try {
         return timeoutMillis > 0L ? future.get(timeoutMillis, TimeUnit.MILLISECONDS) : future.get();
      } catch (TimeoutException var6) {
         this.waitPredicates.remove(predicate);
         this.waitFutures.remove(predicate);
         throw new RuntimeException("waitForOutput timed out after " + timeoutMillis + "ms");
      } catch (Exception var7) {
         this.waitPredicates.remove(predicate);
         this.waitFutures.remove(predicate);
         throw new RuntimeException("error in waitForOutput", var7);
      }
   }

   public String getStdOut() {
      synchronized (this.stdoutBuffer) {
         return this.stdoutBuffer.toString();
      }
   }

   public String getStdErr() {
      synchronized (this.stderrBuffer) {
         return this.stderrBuffer.toString();
      }
   }

   public int getExitCode() {
      return this.exitCode;
   }

   public boolean isAlive() {
      return this.process.isAlive();
   }

   public void close() {
      this.close(false);
   }

   public void close(boolean force) {
      if (this.closed.compareAndSet(false, true)) {
         if (force) {
            this.process.destroyForcibly();
         } else {
            this.process.destroy();
         }

         this.executor.shutdownNow();
         LIVE_HANDLES.remove(this);
         logger.debug("process closed (force={})", force);
      }
   }

   public long getPid() {
      return this.process.pid();
   }

   public CompletableFuture<Integer> getExitFuture() {
      return this.exitFuture;
   }

   public void setSignalConsumer(Consumer<Object> consumer) {
      this.signalConsumer = consumer;
   }

   public void signal(Object result) {
      if (this.signalConsumer != null) {
         this.signalConsumer.accept(result);
      }
   }

   public boolean waitForPort(String host, int port, int attempts, int intervalMs) {
      return PortUtils.waitForPort(host, port, attempts, intervalMs, this::isAlive);
   }

   public boolean waitForHttp(String url, int attempts, int intervalMs) {
      return PortUtils.waitForHttp(url, attempts, intervalMs, this::isAlive);
   }

   @Override
   public Collection<String> jsKeys() {
      return KEYS;
   }

   @Override
   public Object jsGet(String key) {
      return switch (key) {
         case "stdOut" -> this.getStdOut();
         case "stdErr" -> this.getStdErr();
         case "exitCode" -> this.getExitCode();
         case "alive" -> this.isAlive();
         case "pid" -> this.getPid();
         case "start" -> (JavaCallable) (ctx, args) -> {
            this.start();
            return this;
         };
         case "onStdOut" -> (JavaCallable) (ctx, args) -> {
            if (args.length != 0 && args[0] instanceof JavaCallable listener) {
               this.onStdOut(line -> {
                  try {
                     listener.call(ctx, line);
                  } catch (Exception var4) {
                     logger.warn("onStdOut listener error: {}", var4.getMessage());
                  }
               });
               return this;
            } else {
               throw new RuntimeException("onStdOut requires a function argument");
            }
         };
         case "onStdErr" -> (JavaCallable) (ctx, args) -> {
            if (args.length != 0 && args[0] instanceof JavaCallable listener) {
               this.onStdErr(line -> {
                  try {
                     listener.call(ctx, line);
                  } catch (Exception var4) {
                     logger.warn("onStdErr listener error: {}", var4.getMessage());
                  }
               });
               return this;
            } else {
               throw new RuntimeException("onStdErr requires a function argument");
            }
         };
         case "waitSync" -> (JavaCallable) (ctx, args) -> args.length > 0 && args[0] instanceof Number ? this.waitSync(((Number)args[0]).longValue()) : this.waitSync();
         case "waitForOutput" -> (JavaCallable) (ctx, args) -> {
            if (args.length != 0 && args[0] instanceof JavaCallable predicate) {
               long timeout = args.length > 1 && args[1] instanceof Number ? ((Number)args[1]).longValue() : 0L;
               return this.waitForOutput(line -> {
                  Object res = predicate.call(ctx, line);
                  return Boolean.TRUE.equals(res);
               }, timeout);
            } else {
               throw new RuntimeException("waitForOutput requires a function argument");
            }
         };
         case "close" -> (JavaCallable) (ctx, args) -> {
            boolean force = args.length > 0 && Boolean.TRUE.equals(args[0]);
            this.close(force);
            return null;
         };
         case "signal" -> (JavaCallable) (ctx, args) -> {
            if (args.length > 0) {
               this.signal(args[0]);
            }

            return null;
         };
         case "waitForPort" -> (JavaCallable) (ctx, args) -> {
            String host = args.length > 0 ? args[0].toString() : "localhost";
            int port = args.length > 1 ? ((Number)args[1]).intValue() : 8080;
            int attempts = args.length > 2 ? ((Number)args[2]).intValue() : 30;
            int interval = args.length > 3 ? ((Number)args[3]).intValue() : 250;
            return this.waitForPort(host, port, attempts, interval);
         };
         case "waitForHttp" -> (JavaCallable) (ctx, args) -> {
            String url = args.length > 0 ? args[0].toString() : "http://localhost:8080";
            int attempts = args.length > 1 ? ((Number)args[1]).intValue() : 30;
            int interval = args.length > 2 ? ((Number)args[2]).intValue() : 1000;
            return this.waitForHttp(url, attempts, interval);
         };
         default -> null;
      };
   }

   static {
      Runtime.getRuntime().addShutdownHook(new Thread(() -> {
         for (ProcessHandle handle : LIVE_HANDLES) {
            try {
               if (handle.process != null && handle.process.isAlive()) {
                  handle.process.destroyForcibly();
               }
            } catch (Throwable var3) {
            }
         }
      }, "karate-process-shutdown"));
   }
}
