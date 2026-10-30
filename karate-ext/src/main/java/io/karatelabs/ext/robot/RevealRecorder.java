package io.karatelabs.ext.robot;

import io.karatelabs.common.Json;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class RevealRecorder {
   private static final Logger b = LoggerFactory.getLogger(RevealRecorder.class);
   private static final long c = 15000L;
   public static final String a = "reveal-deltas.local.jsonl";
   private final io.karatelabs.ext.agent.events.AgentEventEmitter d;
   private final Runnable e;
   private volatile BiConsumer<String, String> f;
   private final AtomicInteger g = new AtomicInteger();
   private final AtomicInteger h = new AtomicInteger();
   private volatile long i;
   private final AtomicInteger j = new AtomicInteger();
   private volatile int k;
   private volatile io.karatelabs.ext.robot.recording.TrafficRecorder l;
   private volatile Supplier<io.karatelabs.ext.common.secrets.SecretStore> m;
   private volatile ExecutorService n;
   private volatile boolean o;
   private final List<String> p = new ArrayList<>();

   public RevealRecorder(io.karatelabs.ext.agent.events.AgentEventEmitter var1, Runnable var2) {
      this.d = var1;
      this.e = var2;
   }

   public RevealRecorder a(BiConsumer<String, String> var1) {
      this.f = var1;
      return this;
   }

   public RevealRecorder a(io.karatelabs.ext.robot.recording.TrafficRecorder var1, Supplier<io.karatelabs.ext.common.secrets.SecretStore> var2) {
      this.l = var1;
      this.m = var2;
      this.n = Executors.newSingleThreadExecutor(var0 -> {
         Thread var1x = new Thread(var0, "recorder-actions");
         var1x.setDaemon(true);
         return var1x;
      });
      return this;
   }

   public void a(String var1, String var2, String var3) {
      if (var1 != null && !var1.isBlank() && !this.o) {
         try {
            if (!(Json.of(var1).value() instanceof Map var5)) {
               return;
            }

            String var7 = String.valueOf(var5.get("action"));
            boolean var8 = !Boolean.FALSE.equals(var5.getOrDefault("isTop", Boolean.TRUE));
            if (!var8) {
               if (var3 != null) {
                  var5.put("frame", var3);
                  if (var5.get("locator") instanceof String var9 && !var9.isBlank()) {
                     var5.put("locator", var3 + " >> " + var9);
                  }
               } else if (var2 != null) {
                  var5.put("frame", var2);
               }
            }

            var5.remove("isTop");
            Object var12 = var5.remove("revealDelta");
            if (var12 != null) {
               this.a(var7, var5.get("locator"), var12);
            }

            if ("focus".equals(var7)) {
               io.karatelabs.ext.agent.events.AgentEvent.FocusEvent var13 = io.karatelabs.ext.agent.events.AgentEvent.FocusEvent.a(
                  String.valueOf(var5.getOrDefault("phase", "in")), a(var5.get("locator")), a(var5.get("frame"))
               );
               this.a(() -> this.a(var13));
            } else {
               this.i = io.karatelabs.ext.agent.events.AgentEvent.f();
               int var14 = this.j.incrementAndGet();
               this.k = var14;
               var5.put("seq", var14);
               this.g.incrementAndGet();
               this.a(() -> this.a(var14, var5));
            }
         } catch (Exception var11) {
            b.debug("recorder payload dropped: {} ({})", var11.getMessage(), var1);
         }
      }
   }

   private void a(Runnable var1) {
      ExecutorService var2 = this.n;
      if (var2 == null) {
         var1.run();
      } else {
         try {
            var2.execute(var1);
         } catch (RejectedExecutionException var4) {
            b.debug("recorder event after the action barrier dropped");
         }
      }
   }

   private void a(int var1, Map<String, Object> var2) {
      boolean var3 = Boolean.TRUE.equals(var2.remove("secret"));
      io.karatelabs.ext.robot.recording.TrafficRecorder var4 = this.l;
      if (var3 && var4 != null && var2.get("value") instanceof String var5 && !var5.isEmpty()) {
         io.karatelabs.ext.common.secrets.SecretStore var13 = this.m == null ? null : this.m.get();
         io.karatelabs.ext.robot.recording.TrafficRecorder.SecretResolutionResult var7 = var4.a(var5, var13);
         var2.put("value", var7.a());
         if (var7.b() != null) {
            var2.put("secretError", var7.b());
            synchronized (this.p) {
               this.p.add(var7.b());
            }
         }
      }

      this.a((io.karatelabs.ext.agent.events.AgentEvent)io.karatelabs.ext.agent.events.AgentEvent.HumanActionEvent.a(var2));
      this.h.incrementAndGet();
      if (var4 != null) {
         double var12 = var2.get("t") instanceof Number var14 ? var14.doubleValue() : System.currentTimeMillis();
         long var15 = var2.get("wall") instanceof Number var9 ? var9.longValue() : (long)var12;
         var4.a(new io.karatelabs.ext.robot.recording.TrafficRecorder.QueuedRecorderEvent(var1, var12, var15, new LinkedHashMap<>(var2)));
      }
   }

   public void a() {
      this.o = true;
   }

   public boolean a(long var1) {
      ExecutorService var3 = this.n;
      if (var3 == null) {
         return true;
      } else {
         var3.shutdown();

         try {
            return var3.awaitTermination(var1, TimeUnit.MILLISECONDS);
         } catch (InterruptedException var5) {
            Thread.currentThread().interrupt();
            return false;
         }
      }
   }

   public List<String> b() {
      synchronized (this.p) {
         return List.copyOf(this.p);
      }
   }

   private void a(String var1, Object var2, Object var3) {
      BiConsumer var4 = this.f;
      if (var4 != null) {
         try {
            LinkedHashMap<java.lang.String, java.lang.Object> var5 = new LinkedHashMap();
            var5.put("ts", io.karatelabs.ext.agent.events.AgentEvent.f());
            var5.put("action", var1);
            if (var2 != null) {
               var5.put("locator", var2);
            }

            var5.put("revealDelta", var3);
            var4.accept("reveal-deltas.local.jsonl", Json.of(var5).toString());
         } catch (Exception var6) {
            b.debug("reveal-delta sidecar write skipped: {}", var6.getMessage());
         }
      }
   }

   public void a(String var1) {
      if (var1 != null && !var1.isBlank()) {
         Long var2 = this.i > 0L && io.karatelabs.ext.agent.events.AgentEvent.f() - this.i <= 15000L ? this.i : null;
         int var3 = this.k;
         this.a(() -> {
            this.a((io.karatelabs.ext.agent.events.AgentEvent)io.karatelabs.ext.agent.events.AgentEvent.AnnotationEvent.a(var1, var2));
            io.karatelabs.ext.robot.recording.TrafficRecorder var4 = this.l;
            if (var4 != null && var2 != null) {
               var4.a(var3, var1);
            }
         });
      }
   }

   public void c() {
      this.a(() -> this.a((io.karatelabs.ext.agent.events.AgentEvent)io.karatelabs.ext.agent.events.AgentEvent.BrowserResetEvent.g()));
   }

   public void a(String var1, String var2) {
      this.a((io.karatelabs.ext.agent.events.AgentEvent)io.karatelabs.ext.agent.events.AgentEvent.ControlHandoffEvent.a(var1, var2));
   }

   public int d() {
      return this.g.get();
   }

   public int e() {
      return this.h.get();
   }

   private void a(io.karatelabs.ext.agent.events.AgentEvent var1) {
      try {
         this.d.emit(var1);
      } catch (RuntimeException var4) {
         b.debug("recorder emit dropped: {}", var4.getMessage());
      }

      if (this.e != null) {
         try {
            this.e.run();
         } catch (Exception var3) {
         }
      }
   }

   private static String a(Object var0) {
      return var0 == null ? null : String.valueOf(var0);
   }
}
