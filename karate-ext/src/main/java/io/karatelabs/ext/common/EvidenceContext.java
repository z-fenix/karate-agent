package io.karatelabs.ext.common;

import io.karatelabs.core.ScenarioRuntime;
import io.karatelabs.output.LogContext;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

public final class EvidenceContext {
   private static final ThreadLocal<io.karatelabs.ext.common.EvidenceContext.EvidenceEventSink> a = new ThreadLocal<>();
   private static final AtomicLong b = new AtomicLong();

   private EvidenceContext() {
   }

   public static io.karatelabs.ext.common.EvidenceContext.EvidenceEventSink a(io.karatelabs.ext.common.EvidenceContext.EvidenceEventSink var0) {
      io.karatelabs.ext.common.EvidenceContext.EvidenceEventSink var1 = a.get();
      if (var0 == null) {
         a.remove();
      } else {
         a.set(var0);
      }

      return var1;
   }

   public static io.karatelabs.ext.common.EvidenceContext.EvidenceEventSink a() {
      return a.get();
   }

   public static long b() {
      return b.get();
   }

   static boolean a(String var0, Map<String, Object> var1) {
      io.karatelabs.ext.common.EvidenceContext.EvidenceEventSink var2 = a.get();
      if (var2 == null) {
         return false;
      } else {
         var2.event(var0, var1);
         return true;
      }
   }

   static LogContext c() {
      LogContext var0 = LogContext.peekCurrent();
      return var0 != null && ScenarioRuntime.currentOrNull() != null ? var0 : null;
   }

   static void d() {
      b.incrementAndGet();
   }

   @FunctionalInterface
   public interface EvidenceEventSink {
      void event(String var1, Map<String, Object> var2);
   }
}
