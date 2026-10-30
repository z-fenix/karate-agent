package io.karatelabs.ext.common.server;

import io.karatelabs.http.SseConnection;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class SseEventBroker implements EventPublisher {
   private static final Logger a = LoggerFactory.getLogger(SseEventBroker.class);
   private static final int b = 1000;
   private static final int c = 5;
   private static final List<String> d = List.of("navigate", "highlight", "overlay");
   private final Set<SseConnection> e = ConcurrentHashMap.newKeySet();
   private final Map<String, io.karatelabs.ext.common.server.SseEventBroker.SseTopicState> f = new ConcurrentHashMap<>();
   private final Map<String, String> g = new ConcurrentHashMap<>();

   public void a(SseConnection var1) {
      var1.sendComment("subscribed");
      if (this.e.size() >= 5) {
         try {
            var1.send("tab-limit", "{\"type\":\"tab-limit\",\"cap\":5}");
         } catch (RuntimeException var5) {
         }
      } else {
         this.e.add(var1);
         var1.onDisconnect(() -> this.e.remove(var1));

         for (String var3 : d) {
            String var4 = this.g.get(var3);
            if (var4 != null) {
               a(var1, this.e, var3, var4);
            }
         }
      }
   }

   public void a(SseConnection var1, String var2, long var3) {
      io.karatelabs.ext.common.server.SseEventBroker.SseTopicState var5 = this.f.computeIfAbsent(var2, var0 -> new io.karatelabs.ext.common.server.SseEventBroker.SseTopicState());
      synchronized (var5) {
         var5.a.add(var1);
         var1.onDisconnect(() -> var5.a.remove(var1));
         var1.sendComment("subscribed:" + var2);

         for (io.karatelabs.ext.common.server.SseEventBroker.BufferedSseEvent var8 : var5.b) {
            if (var8.a > var3) {
               a(var1, var5.a, var8.b, var8.c);
            }
         }
      }
   }

   public void a(String var1, String var2) {
      if (d.contains(var1)) {
         this.g.put(var1, var2);
      } else if ("view-clear".equals(var1)) {
         this.g.clear();
      }

      a(this.e, var1, var2);
   }

   @Override
   public void publish(String var1, String var2, String var3) {
      io.karatelabs.ext.common.server.SseEventBroker.SseTopicState var4 = this.f.computeIfAbsent(var1, var0 -> new io.karatelabs.ext.common.server.SseEventBroker.SseTopicState());
      synchronized (var4) {
         long var6 = ++var4.c;
         String var8 = a(var3, var6);
         var4.b.addLast(new io.karatelabs.ext.common.server.SseEventBroker.BufferedSseEvent(var6, var2, var8));
         if (var4.b.size() > 1000) {
            var4.b.removeFirst();
         }

         a(var4.a, var2, var8);
         a(this.e, var2, var8);
      }
   }

   public int a() {
      return this.e.size();
   }

   public int a(String var1) {
      io.karatelabs.ext.common.server.SseEventBroker.SseTopicState var2 = this.f.get(var1);
      return var2 == null ? 0 : var2.a.size();
   }

   public void b() {
      a(this.e);

      for (io.karatelabs.ext.common.server.SseEventBroker.SseTopicState var2 : this.f.values()) {
         synchronized (var2) {
            a(var2.a);
         }
      }

      this.f.clear();
   }

   private static String a(String var0, long var1) {
      if (var0 != null && var0.startsWith("{")) {
         String var3 = var0.substring(1).stripLeading();
         return var3.startsWith("}") ? "{\"seq\":" + var1 + var3 : "{\"seq\":" + var1 + "," + var3;
      } else {
         return "{\"seq\":" + var1 + ",\"data\":" + (var0 == null ? "null" : var0) + "}";
      }
   }

   private static void a(Set<SseConnection> var0, String var1, String var2) {
      for (SseConnection var4 : var0) {
         a(var4, var0, var1, var2);
      }
   }

   private static void a(SseConnection var0, Set<SseConnection> var1, String var2, String var3) {
      if (!var0.isOpen()) {
         var1.remove(var0);
      } else {
         try {
            var0.send(var2, var3);
         } catch (RuntimeException var5) {
            a.debug("dropping SSE subscriber after send failure: {}", var5.getMessage());
            var1.remove(var0);
         }
      }
   }

   private static void a(Set<SseConnection> var0) {
      for (SseConnection var2 : var0) {
         try {
            var2.close();
         } catch (RuntimeException var4) {
         }
      }

      var0.clear();
   }

   private record BufferedSseEvent(long a, String b, String c) {
   }

   private static final class SseTopicState {
      final Set<SseConnection> a = ConcurrentHashMap.newKeySet();
      final Deque<io.karatelabs.ext.common.server.SseEventBroker.BufferedSseEvent> b = new ArrayDeque<>();
      long c;
   }
}
