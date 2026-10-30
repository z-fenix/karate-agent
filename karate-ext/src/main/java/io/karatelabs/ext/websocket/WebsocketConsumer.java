package io.karatelabs.ext.websocket;

import io.karatelabs.ext.contract.WireCodec;
import io.karatelabs.http.WsClient;
import io.karatelabs.http.WsClientOptions;
import io.karatelabs.http.WsFrame;
import io.karatelabs.js.JavaCallable;
import io.karatelabs.js.SimpleObject;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WebsocketConsumer implements SimpleObject {
   private static final Logger logger = LoggerFactory.getLogger("karate.websocket");
   private static final String URL = "url";
   private static final String FILTER = "filter";
   private static final String CODEC = "codec";
   private static final String SUBPROTOCOL = "subProtocol";
   private static final String SSL = "ssl";
   private static final String COUNT = "count";
   private static final String TIMEOUT = "timeout";
   private static final String COLLECT = "collect";
   private static final String HEADER = "header";
   private static final String HEADERS = "headers";
   private static final String START = "start";
   private static final String SEND = "send";
   private static final String STOP = "stop";
   private static final String POP = "pop";
   String url;
   String subProtocol;
   Boolean trustAll;
   int messageCount = 1;
   Long timeoutMs;
   Map<String, Object> headers;
   WireCodec codec;
   private JavaCallable filterFunction;
   private WsClient client;
   private final Object lock = new Object();
   private final List<Object> results = new ArrayList<>();
   private volatile CompletableFuture<List<Object>> resultsFuture = new CompletableFuture<>();

   public Void start() {
      if (this.url == null) {
         throw new RuntimeException("websocket session: 'url' is required (e.g. session.url = 'ws://localhost:8080/ws')");
      } else {
         if (this.codec == null) {
            this.codec = new RawCodec();
         }

         WsClientOptions.Builder var1 = WsClientOptions.builder(this.url);
         if (this.headers != null) {
            var1.headers(stringHeaders(this.headers));
         }

         if (this.subProtocol != null) {
            var1.subProtocol(this.subProtocol);
         }

         if (this.trustAll != null) {
            var1.trustAllCerts(this.trustAll);
         }

         var1.onMessage(this::onFrame);
         WsClientOptions var2 = var1.build();

         try {
            this.client = WsClient.connect(var2);
         } catch (Exception var5) {
            String var4 = io.karatelabs.ext.websocket.WebsocketConnectionDiagnostics.a(var2, var5);
            logger.error(var4);
            throw new RuntimeException(var4, var5);
         }

         if (this.codec instanceof WebsocketLifecycle var3) {
            var3.onStart(this);
         }

         logger.debug("connected: {}", this.url);
         return null;
      }
   }

   public Void send(Object var1) {
      if (this.client == null) {
         this.start();
      }

      Object var2 = this.codec.encode(var1);
      logger.debug(">> send: {}", var2);
      if (var2 instanceof byte[] var3) {
         this.client.send(var3);
      } else {
         this.client.send(String.valueOf(var2));
      }

      return null;
   }

   private void onFrame(WsFrame var1) {
      Object var2 = var1.isText() ? var1.getText() : var1.getBytes();
      Object var3 = this.codec.decode(var2);
      if (this.codec instanceof WebsocketLifecycle var4) {
         var4.onMessage(this, var3);
      } else {
         this.receive(var3);
      }
   }

   public void receive(Object var1) {
      if (!this.shouldConsume(var1)) {
         logger.debug("<< ignoring: {}", var1);
      } else {
         logger.debug("<< {}", var1);
         CompletableFuture var2 = null;
         ArrayList var3 = null;
         synchronized (this.lock) {
            this.results.add(var1);
            if (this.results.size() >= this.messageCount) {
               var2 = this.resultsFuture;
               var3 = new ArrayList<>(this.results);
            }
         }

         if (var2 != null) {
            var2.complete(var3);
         }
      }
   }

   public Void stop() {
      if (this.codec instanceof WebsocketLifecycle var1) {
         var1.onStop(this);
      }

      if (this.client != null) {
         this.client.close();
         this.client = null;
      }

      CompletableFuture var6;
      ArrayList var7;
      synchronized (this.lock) {
         var6 = this.resultsFuture;
         var7 = new ArrayList<>(this.results);
      }

      if (!var6.isDone()) {
         var6.complete(var7);
      }

      return null;
   }

   private boolean shouldConsume(Object var1) {
      if (this.filterFunction == null) {
         return true;
      } else {
         try {
            Object var2 = this.filterFunction.call(null, var1);
            return var2 instanceof Boolean var3 ? var3 : Boolean.parseBoolean(String.valueOf(var2));
         } catch (Exception var4) {
            logger.error("filter expression failed: {}", var4.getMessage());
            return false;
         }
      }
   }

   public List<Object> collect() {
      CompletableFuture var1 = this.resultsFuture;

      try {
         List var2 = this.timeoutMs != null ? (List)var1.get(this.timeoutMs, TimeUnit.MILLISECONDS) : (List)var1.get();
         ArrayList<java.lang.Object> var10 = new ArrayList(var2);
         synchronized (this.lock) {
            this.resultsFuture = new CompletableFuture<>();
            this.results.clear();
         }

         logger.debug("<< collect {}", var10);
         return var10;
      } catch (Exception var9) {
         int var3;
         synchronized (this.lock) {
            var3 = this.results.size();
         }

         throw new RuntimeException(
            "websocket collect failed (received "
               + var3
               + " of "
               + this.messageCount
               + (this.timeoutMs != null ? " within " + this.timeoutMs + "ms" : "")
               + "): "
               + var9,
            var9
         );
      }
   }

   public Object pop() {
      var var1 = this.collect();
      return var1.isEmpty() ? null : var1.get(0);
   }

   public void header(String var1, String var2) {
      if (this.headers == null) {
         this.headers = new LinkedHashMap<>();
      }

      if (var2 == null) {
         this.headers.remove(var1);
      } else {
         this.headers.put(var1, var2);
      }
   }

   private static Map<String, String> stringHeaders(Map<String, Object> var0) {
      LinkedHashMap<java.lang.String, java.lang.String> var1 = new LinkedHashMap();
      var0.forEach((var1x, var2) -> {
         if (var2 != null) {
            var1.put(var1x, var2.toString());
         }
      });
      return var1;
   }

   @Override
   public Collection<String> jsKeys() {
      return List.of("url", "filter", "codec", "subProtocol", "ssl", "count", "timeout", "collect", "header", "headers", "start", "send", "stop", "pop");
   }

   @Override
   public Object jsGet(String var1) {
      return switch (var1) {
         case "url" -> this.url;
         case "codec" -> this.codec;
         case "header" -> (io.karatelabs.js.JavaInvokable) var1x -> {
            this.header(var1x[0].toString(), var1x.length > 1 && var1x[1] != null ? var1x[1].toString() : null);
            return null;
         };
         case "send" -> (io.karatelabs.js.JavaInvokable) var1x -> this.send(var1x[0]);
         case "start" -> (io.karatelabs.js.JavaInvokable) var1x -> this.start();
         case "collect" -> (io.karatelabs.js.JavaInvokable) var1x -> this.collect();
         case "pop" -> (io.karatelabs.js.JavaInvokable) var1x -> this.pop();
         case "stop" -> (io.karatelabs.js.JavaInvokable) var1x -> this.stop();
         default -> null;
      };
   }

   @Override
   public void putMember(String var1, Object var2) {
      switch (var1) {
         case "url":
            this.url = var2 != null ? var2.toString() : null;
            break;
         case "codec":
            this.codec = (WireCodec)var2;
            break;
         case "subProtocol":
            this.subProtocol = var2 != null ? var2.toString() : null;
            break;
         case "ssl":
            if (var2 instanceof Boolean var11) {
               this.trustAll = var11;
            } else if (var2 instanceof Map var6 && var6.get("trustAll") instanceof Boolean var7) {
               this.trustAll = var7;
            }
            break;
         case "count":
            this.messageCount = var2 instanceof Number var10 ? var10.intValue() : Integer.parseInt(var2.toString());
            break;
         case "timeout":
            this.timeoutMs = var2 instanceof Number var9 ? var9.longValue() : Long.parseLong(var2.toString());
            break;
         case "filter":
            if (var2 instanceof JavaCallable var5) {
               this.filterFunction = var5;
            }
            break;
         case "headers":
            if (var2 == null) {
               this.headers = null;
            } else if (var2 instanceof Map) {
               this.headers = (Map<String, Object>)var2;
            }
            break;
         default:
            logger.warn("put not supported on websocket session: {} - {}", var1, var2);
      }
   }
}
