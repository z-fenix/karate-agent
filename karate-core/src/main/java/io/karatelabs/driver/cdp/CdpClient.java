package io.karatelabs.driver.cdp;

import io.karatelabs.common.ThreadUtils;
import io.karatelabs.http.WsClient;
import io.karatelabs.http.WsClientOptions;
import io.karatelabs.http.WsException;
import java.time.Duration;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import net.minidev.json.JSONValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CdpClient {
   private static final Logger logger = LoggerFactory.getLogger(CdpClient.class);
   private final WsClient ws;
   private final AtomicInteger idGenerator = new AtomicInteger();
   private final ConcurrentHashMap<Integer, CdpClient.PendingRequest> pending = new ConcurrentHashMap<>();
   private final ConcurrentHashMap<String, List<Consumer<CdpEvent>>> eventHandlers = new ConcurrentHashMap<>();
   private final List<CdpEventListener> externalListeners = new CopyOnWriteArrayList<>();
   private final Duration defaultTimeout;
   private volatile String sessionId;
   private final ExecutorService frameRouter;
   private final ExecutorService eventDispatcher;
   private final Thread[] frameThread;
   private final Thread[] dispatchThread = new Thread[1];

   public static CdpClient connect(String webSocketUrl) {
      return connect(webSocketUrl, Duration.ofSeconds(30L));
   }

   public static CdpClient connect(String webSocketUrl, Duration defaultTimeout) {
      Thread[] frameThread = new Thread[1];
      ExecutorService frameRouter = Executors.newSingleThreadExecutor(tracking(ThreadUtils.daemonFactory("cdp-frame-"), frameThread));
      WsClientOptions options = WsClientOptions.builder(webSocketUrl).disablePing().maxPayloadSize(16777216).callbackExecutor(frameRouter).build();
      WsClient ws = WsClient.connect(options);
      CdpClient client = new CdpClient(ws, defaultTimeout, frameRouter, frameThread);
      client.waitForReady();
      return client;
   }

   static CdpClient detached(Duration defaultTimeout) {
      Thread[] frameThread = new Thread[1];
      return new CdpClient(null, defaultTimeout, Executors.newSingleThreadExecutor(tracking(ThreadUtils.daemonFactory("cdp-frame-"), frameThread)), frameThread);
   }

   private CdpClient(WsClient ws, Duration defaultTimeout, ExecutorService frameRouter, Thread[] frameThread) {
      this.ws = ws;
      this.defaultTimeout = defaultTimeout;
      this.frameRouter = frameRouter;
      this.frameThread = frameThread;
      this.eventDispatcher = Executors.newSingleThreadExecutor(tracking(ThreadUtils.daemonFactory("cdp-event-"), this.dispatchThread));
      if (ws != null) {
         this.setupMessageHandler();
      }
   }

   private static ThreadFactory tracking(ThreadFactory factory, Thread[] holder) {
      return r -> {
         Thread t = factory.newThread(r);
         holder[0] = t;
         return t;
      };
   }

   void route(String json) {
      this.frameRouter.execute(() -> this.handleMessage(json));
   }

   ExecutorService frameRouter() {
      return this.frameRouter;
   }

   public void awaitEventsDispatched(Runnable atFence) {
      Thread current = Thread.currentThread();
      if (current != this.frameThread[0] && current != this.dispatchThread[0]) {
         CompletableFuture<Void> fence = new CompletableFuture<>();

         try {
            this.frameRouter.execute(() -> {
               try {
                  this.eventDispatcher.execute(() -> {
                     try {
                        if (atFence != null) {
                           atFence.run();
                        }

                        fence.complete(null);
                     } catch (Throwable var3x) {
                        fence.completeExceptionally(var3x);
                     }
                  });
               } catch (RejectedExecutionException var4) {
                  fence.completeExceptionally(var4);
               }
            });
         } catch (RejectedExecutionException var7) {
            throw new IllegalStateException("cdp client closed", var7);
         }

         try {
            fence.get(this.defaultTimeout.toMillis(), TimeUnit.MILLISECONDS);
         } catch (TimeoutException var8) {
            throw new RuntimeException("CDP timeout for: event dispatch fence");
         } catch (InterruptedException var9) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("interrupted waiting for the event dispatch fence", var9);
         } catch (ExecutionException var10) {
            Throwable cause = var10.getCause();
            throw cause instanceof RuntimeException r ? r : new RuntimeException("event dispatch fence failed: " + cause, cause);
         }
      } else {
         throw new IllegalStateException("awaitEventsDispatched() cannot be called from the cdp " + current.getName() + " thread: it would deadlock");
      }
   }

   public void awaitEventsDispatched() {
      this.awaitEventsDispatched(null);
   }

   private void waitForReady() {
      try {
         Duration readyTimeout = this.defaultTimeout.compareTo(Duration.ofSeconds(10L)) > 0 ? Duration.ofSeconds(10L) : this.defaultTimeout;
         CdpMessage message = new CdpMessage(this, this.nextId(), "Browser.getVersion");
         message.timeout(readyTimeout);
         CdpResponse response = this.send(message);
         if (response.isError()) {
            logger.warn("CDP readiness check returned error: {}", response.getErrorMessage());
         } else {
            logger.debug("CDP ready, browser: {}", response.getResult().get("product"));
         }
      } catch (Exception var4) {
         throw new RuntimeException("CDP connection failed readiness check: " + var4.getMessage(), var4);
      }
   }

   private void setupMessageHandler() {
      this.ws.onMessage(frame -> {
         if (frame.isText()) {
            this.handleMessage(frame.getText());
         }
      });
      this.ws.onClose(() -> {
         for (CdpClient.PendingRequest pr : this.pending.values()) {
            pr.future.completeExceptionally(new WsException(WsException.Type.CONNECTION_CLOSED, "websocket closed"));
         }

         this.pending.clear();
      });
      this.ws.onError(error -> logger.error("CDP connection error: {}", error.getMessage()));
   }

   private void handleMessage(String json) {
      Map<String, Object> map;
      try {
         map = (Map<String, Object>)JSONValue.parseWithException(json);
      } catch (Exception var7) {
         logger.error("failed to parse CDP message: {}", var7.getMessage());
         return;
      }

      if (map.containsKey("id")) {
         int id = ((Number)map.get("id")).intValue();
         CdpClient.PendingRequest pr = this.pending.remove(id);
         if (pr != null) {
            CdpResponse response = new CdpResponse(map);
            pr.future.complete(response);
         } else {
            logger.trace("received response for fire-and-forget request id: {}", id);
         }
      } else if (map.containsKey("method")) {
         String method = (String)map.get("method");
         CdpEvent event = new CdpEvent(map);

         try {
            this.eventDispatcher.execute(() -> this.dispatchEvent(method, event));
         } catch (RejectedExecutionException var6) {
            logger.trace("event dropped after close: {}", method);
         }
      }
   }

   private void dispatchEvent(String method, CdpEvent event) {
      List<Consumer<CdpEvent>> handlers = this.eventHandlers.get(method);
      if (handlers != null) {
         for (Consumer<CdpEvent> handler : handlers) {
            try {
               handler.accept(event);
            } catch (Exception var9) {
               logger.error("event handler error for {}: {}", method, var9.getMessage());
            }
         }
      }

      if (!this.externalListeners.isEmpty()) {
         Map<String, Object> params = event.getParams();

         for (CdpEventListener listener : this.externalListeners) {
            try {
               listener.onEvent(method, params);
            } catch (Exception var8) {
               logger.error("external listener error for {}: {}", method, var8.getMessage());
            }
         }
      }
   }

   public int nextId() {
      return this.idGenerator.incrementAndGet();
   }

   public CdpMessage method(String method) {
      CdpMessage message = new CdpMessage(this, this.nextId(), method);
      if (this.sessionId != null) {
         message.sessionId(this.sessionId);
      }

      return message;
   }

   public CdpMessage browserMethod(String method) {
      return new CdpMessage(this, this.nextId(), method);
   }

   public String getSessionId() {
      return this.sessionId;
   }

   public void setSessionId(String sessionId) {
      this.sessionId = sessionId;
   }

   CdpResponse send(CdpMessage message) {
      try {
         return this.sendAsync(message).join();
      } catch (CompletionException var4) {
         Throwable cause = var4.getCause();
         if (cause instanceof TimeoutException) {
            throw new RuntimeException("CDP timeout for: " + message.getMethod());
         } else if (cause instanceof RuntimeException) {
            throw (RuntimeException)cause;
         } else {
            throw new RuntimeException("CDP error: " + cause.getMessage(), cause);
         }
      }
   }

   CompletableFuture<CdpResponse> sendAsync(CdpMessage message) {
      if (!this.isOpen()) {
         return CompletableFuture.failedFuture(new WsException(WsException.Type.CONNECTION_CLOSED, "websocket not open"));
      } else {
         CompletableFuture<CdpResponse> future = new CompletableFuture<>();
         int messageId = message.getId();
         this.pending.put(messageId, new CdpClient.PendingRequest(future, message.getMethod()));
         String json = message.toJson();
         logger.trace(">>> {}", json);

         try {
            this.ws.send(json);
         } catch (Exception var6) {
            this.pending.remove(messageId);
            return CompletableFuture.failedFuture(var6);
         }

         Duration timeout = message.getTimeout() != null ? message.getTimeout() : this.defaultTimeout;
         return future.orTimeout(timeout.toMillis(), TimeUnit.MILLISECONDS).whenComplete((result, ex) -> {
            if (ex instanceof TimeoutException) {
               this.pending.remove(messageId);
               logger.debug("CDP request {} ({}) timed out", messageId, message.getMethod());
            }
         });
      }
   }

   void sendWithoutWaiting(CdpMessage message) {
      String json = message.toJson();
      logger.trace(">>> {}", json);
      this.ws.send(json);
   }

   void cancelPendingEvaluations() {
      Iterator<Entry<Integer, CdpClient.PendingRequest>> iterator = this.pending.entrySet().iterator();

      while (iterator.hasNext()) {
         Entry<Integer, CdpClient.PendingRequest> entry = iterator.next();
         CdpClient.PendingRequest pr = entry.getValue();
         if ("Runtime.evaluate".equals(pr.method)) {
            logger.debug("cancelling pending Runtime.evaluate (id: {}) due to dialog", entry.getKey());
            pr.future.completeExceptionally(new DialogOpenedException("dialog opened while Runtime.evaluate was pending"));
            iterator.remove();
         }
      }
   }

   public void on(String eventName, Consumer<CdpEvent> handler) {
      this.eventHandlers.computeIfAbsent(eventName, k -> new CopyOnWriteArrayList<>()).add(handler);
   }

   public void off(String eventName, Consumer<CdpEvent> handler) {
      List<Consumer<CdpEvent>> handlers = this.eventHandlers.get(eventName);
      if (handlers != null) {
         handlers.remove(handler);
      }
   }

   public void offAll(String eventName) {
      this.eventHandlers.remove(eventName);
   }

   public void addExternalListener(CdpEventListener listener) {
      this.externalListeners.add(listener);
   }

   public void removeExternalListener(CdpEventListener listener) {
      this.externalListeners.remove(listener);
   }

   public void close() {
      if (this.ws != null) {
         this.ws.close();
      }

      for (CdpClient.PendingRequest pr : this.pending.values()) {
         pr.future.completeExceptionally(new WsException(WsException.Type.CONNECTION_CLOSED, "cdp client closed"));
      }

      this.pending.clear();
      this.frameRouter.shutdown();
      this.eventDispatcher.shutdown();
   }

   public boolean isOpen() {
      return this.ws != null && this.ws.isOpen();
   }

   public Duration getDefaultTimeout() {
      return this.defaultTimeout;
   }

   static class PendingRequest {
      final CompletableFuture<CdpResponse> future;
      final String method;

      PendingRequest(CompletableFuture<CdpResponse> future, String method) {
         this.future = future;
         this.method = method;
      }
   }
}
