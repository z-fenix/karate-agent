package io.karatelabs.ext.agent.http;

import java.nio.CharBuffer;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import org.apache.hc.client5.http.async.methods.AbstractCharResponseConsumer;
import org.apache.hc.client5.http.async.methods.SimpleRequestBuilder;
import org.apache.hc.client5.http.async.methods.SimpleRequestProducer;
import org.apache.hc.client5.http.impl.async.CloseableHttpAsyncClient;
import org.apache.hc.client5.http.impl.async.HttpAsyncClients;
import org.apache.hc.client5.http.impl.nio.PoolingAsyncClientConnectionManager;
import org.apache.hc.client5.http.impl.nio.PoolingAsyncClientConnectionManagerBuilder;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpResponse;
import org.apache.hc.core5.io.CloseMode;
import org.apache.hc.core5.util.TimeValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AgentHttpClient implements AutoCloseable {
   private static final Logger a = LoggerFactory.getLogger(io.karatelabs.ext.agent.http.AgentHttpClient.class);
   private final Object b = new Object();
   private io.karatelabs.ext.agent.http.AgentHttpClient.State c = io.karatelabs.ext.agent.http.AgentHttpClient.State.NEW;
   private CloseableHttpAsyncClient d;
   private volatile Future<Void> e;
   private final AtomicInteger f = new AtomicInteger();
   private volatile boolean g;

   private CloseableHttpAsyncClient getOrCreateClient() {
      synchronized (this.b) {
         if (this.c == io.karatelabs.ext.agent.http.AgentHttpClient.State.CLOSED) {
            throw new IllegalStateException("async http client is closed");
         } else {
            if (this.c == io.karatelabs.ext.agent.http.AgentHttpClient.State.NEW) {
               PoolingAsyncClientConnectionManager var2 = PoolingAsyncClientConnectionManagerBuilder.create().build();
               this.d = HttpAsyncClients.custom().setConnectionManager(var2).build();
               this.d.start();
               this.c = io.karatelabs.ext.agent.http.AgentHttpClient.State.RUNNING;
               a.debug("started async http client");
            }

            return this.d;
         }
      }
   }

   public void cancel() {
      if (this.f.get() > 0) {
         this.g = true;
      }

      Future var1 = this.e;
      if (var1 != null && !var1.isDone()) {
         var1.cancel(true);
      }
   }

   public void execute(AgentHttpRequest var1, Consumer<String> var2) {
      this.execute(var1, var2, null);
   }

   public void execute(AgentHttpRequest var1, final Consumer<String> var2, final Consumer<Exception> var3) {
      this.f.incrementAndGet();

      try {
         CloseableHttpAsyncClient var4 = this.getOrCreateClient();
         SimpleRequestBuilder var5 = SimpleRequestBuilder.create(var1.method()).setUri(var1.requestUri());
         if (var1.body() != null) {
            var5.setBody(var1.body(), ContentType.APPLICATION_JSON);
         }

         if (var1.headers() != null) {
            var1.headers().forEach((var1x, var2x) -> var2x.forEach(var2xx -> var5.addHeader(var1x, var2xx)));
         }

         SimpleRequestProducer var6 = SimpleRequestProducer.create(var5.build());
         this.e = var4.execute(var6, new AbstractCharResponseConsumer<Void>() {
            private int c;
            private boolean d;
            private final StringBuilder e = new StringBuilder();

            protected void start(HttpResponse var1, ContentType var2x) {
               this.c = var1.getCode();
               this.d = this.c >= 400;
               if (this.c != 200) {
                  io.karatelabs.ext.agent.http.AgentHttpClient.a.warn("HTTP response code: {}", this.c);
               }
            }

            protected int capacityIncrement() {
               return Integer.MAX_VALUE;
            }

            protected void data(CharBuffer var1, boolean var2x) {
               while (var1.hasRemaining()) {
                  char[] var3x = new char[var1.remaining()];
                  var1.get(var3x);
                  String var4x = new String(var3x);
                  if (this.d) {
                     this.e.append(var4x);
                  } else {
                     var2.accept(var4x);
                  }
               }
            }

            @Override
            protected Void buildResult() {
               if (this.d) {
                  AgentHttpException var1x = new AgentHttpException(this.c, this.e.toString().trim());
                  if (var3 == null) {
                     throw var1x;
                  }

                  var3.accept(var1x);
               }

               return null;
            }

            public void failed(Exception var1) {
               io.karatelabs.ext.agent.http.AgentHttpClient.a.error("HTTP request failed: {}", var1.getMessage());
               if (var3 != null) {
                  var3.accept(var1);
               }
            }

            public void releaseResources() {
            }
         }, null);
         if (this.g) {
            this.e.cancel(true);
         }

         this.e.get();
      } catch (Exception var10) {
         if (var3 == null) {
            throw new RuntimeException(var10);
         }

         var3.accept(var10);
      } finally {
         if (this.f.decrementAndGet() == 0) {
            this.g = false;
         }
      }
   }

   @Override
   public void close() {
      CloseableHttpAsyncClient var1;
      synchronized (this.b) {
         if (this.c == io.karatelabs.ext.agent.http.AgentHttpClient.State.CLOSED) {
            return;
         }

         var1 = this.c == io.karatelabs.ext.agent.http.AgentHttpClient.State.RUNNING ? this.d : null;
         this.c = io.karatelabs.ext.agent.http.AgentHttpClient.State.CLOSED;
         this.d = null;
      }

      if (var1 != null) {
         try {
            this.cancel();
            var1.initiateShutdown();

            try {
               var1.awaitShutdown(TimeValue.ofSeconds(5L));
            } catch (InterruptedException var4) {
               Thread.currentThread().interrupt();
            }

            var1.close(CloseMode.IMMEDIATE);
            a.debug("closed async http client");
         } catch (Exception var5) {
            a.warn("error closing http client: {}", var5.getMessage());
         }
      }
   }

   private static enum State {
      NEW,
      RUNNING,
      CLOSED;
   }
}
