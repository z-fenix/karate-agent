package io.karatelabs.ext.agent.events;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class AgentEventBus implements AutoCloseable {
   private static final Logger a = LoggerFactory.getLogger(AgentEventBus.class);
   private static final int b = 4096;
   private static final long c = 2000L;
   private final String d;
   private final int e;
   private final List<io.karatelabs.ext.agent.events.AgentEventBus.Subscriber> f = new CopyOnWriteArrayList<>();
   private final AtomicBoolean g = new AtomicBoolean();

   public AgentEventBus() {
      this(newRunId(), 4096);
   }

   public AgentEventBus(String var1) {
      this(var1, 4096);
   }

   public AgentEventBus(String var1, int var2) {
      this.d = var1;
      this.e = var2;
   }

   public String runId() {
      return this.d;
   }

   public AgentEventBus subscribe(AgentEventListener var1) {
      return this.subscribe(var1, false);
   }

   public AgentEventBus subscribe(AgentEventListener var1, boolean var2) {
      if (this.g.get()) {
         throw new IllegalStateException("event bus is closed");
      } else {
         this.f.add(new io.karatelabs.ext.agent.events.AgentEventBus.Subscriber(var1, var2, this.e));
         return this;
      }
   }

   public void publish(AgentEvent var1) {
      if (!this.g.get()) {
         for (io.karatelabs.ext.agent.events.AgentEventBus.Subscriber var3 : this.f) {
            var3.a(var1);
         }
      }
   }

   @Override
   public void close() {
      if (this.g.compareAndSet(false, true)) {
         for (io.karatelabs.ext.agent.events.AgentEventBus.Subscriber var2 : this.f) {
            var2.a();
         }
      }
   }

   private static String newRunId() {
      return UUID.randomUUID().toString().substring(0, 8);
   }

   private static final class Subscriber {
      private final AgentEventListener a;
      private final boolean b;
      private final BlockingQueue<AgentEvent> c;
      private final Thread d;
      private final AtomicBoolean e = new AtomicBoolean();
      private final AtomicLong f = new AtomicLong();

      Subscriber(AgentEventListener var1, boolean var2, int var3) {
         this.a = var1;
         this.b = var2;
         if (var2) {
            this.c = null;
            this.d = null;
         } else {
            this.c = new ArrayBlockingQueue<>(var3);
            this.d = Thread.ofVirtual().name("agent-event-" + System.identityHashCode(var1)).start(this::b);
         }
      }

      void a(AgentEvent var1) {
         if (this.b) {
            this.b(var1);
         } else {
            while (!this.c.offer(var1)) {
               AgentEvent var2 = this.c.poll();
               if (var2 != null && this.f.getAndIncrement() == 0L) {
                  io.karatelabs.ext.agent.events.AgentEventBus.a.warn("event subscriber {} is slow — queue full, dropping oldest events", this.a.getClass().getSimpleName());
               }
            }
         }
      }

      private void b() {
         try {
            while (!this.e.get() || !this.c.isEmpty()) {
               AgentEvent var1 = this.c.poll(50L, TimeUnit.MILLISECONDS);
               if (var1 != null) {
                  this.b(var1);
               }
            }
         } catch (InterruptedException var2) {
            Thread.currentThread().interrupt();
         }
      }

      private void b(AgentEvent var1) {
         try {
            this.a.onEvent(var1);
         } catch (Throwable var3) {
            io.karatelabs.ext.agent.events.AgentEventBus.a.warn("event subscriber {} threw on {}: {}", new Object[]{this.a.getClass().getSimpleName(), var1.a(), var3.toString()});
         }
      }

      void a() {
         boolean var1 = true;
         if (!this.b) {
            this.e.set(true);

            try {
               this.d.join(2000L);
            } catch (InterruptedException var6) {
               Thread.currentThread().interrupt();
            }

            if (this.d.isAlive()) {
               var1 = false;
               io.karatelabs.ext.agent.events.AgentEventBus.a
                  .warn("event subscriber {} wedged — abandoned after {}ms on close (its close() skipped)", this.a.getClass().getSimpleName(), 2000L);
               this.d.interrupt();
            }
         }

         long var2 = this.f.get();
         if (var2 > 0L) {
            io.karatelabs.ext.agent.events.AgentEventBus.a.warn("event subscriber {} dropped {} event(s) under load", this.a.getClass().getSimpleName(), var2);
         }

         if (var1) {
            try {
               this.a.close();
            } catch (Throwable var5) {
               io.karatelabs.ext.agent.events.AgentEventBus.a.warn("event subscriber {} threw on close: {}", this.a.getClass().getSimpleName(), var5.toString());
            }
         }
      }
   }
}
