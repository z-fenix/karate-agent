package io.karatelabs.ext.common;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class SandboxExecutor {
   private static final Logger a = LoggerFactory.getLogger(SandboxExecutor.class);

   private SandboxExecutor() {
   }

   public static Object a(Object var0) {
      return a(var0, false, new IdentityHashMap<>());
   }

   public static Object b(Object var0) {
      return a(var0, true, new IdentityHashMap<>());
   }

   private static Object a(Object var0, boolean var1, IdentityHashMap<Object, Boolean> var2) {
      if (var0 instanceof Map var7) {
         a(var0, var2);
         LinkedHashMap<java.lang.String, java.lang.Object> var8 = new LinkedHashMap();

         for (Entry var10 : (Iterable<Entry>) (Iterable<?>) (var7.entrySet())) {
            var8.put(String.valueOf(var10.getKey()), a(var10.getValue(), var1, var2));
         }

         var2.remove(var0);
         return var1 ? Collections.unmodifiableMap(var8) : var8;
      } else if (!(var0 instanceof List var3)) {
         return var0;
      } else {
         a(var0, var2);
         ArrayList<java.lang.Object> var4 = new ArrayList(var3.size());

         for (Object var6 : var3) {
            var4.add(a(var6, var1, var2));
         }

         var2.remove(var0);
         return var1 ? Collections.unmodifiableList(var4) : var4;
      }
   }

   private static void a(Object var0, IdentityHashMap<Object, Boolean> var1) {
      if (var1.put(var0, Boolean.TRUE) != null) {
         throw new IllegalStateException(
            "cannot make a defensive copy of a value that contains itself (a recursive YAML anchor, or a hand-built cyclic object)"
         );
      }
   }

   public static long a(String var0, long var1) {
      return a(var0, System.getenv(var0), var1);
   }

   static long a(String var0, String var1, long var2) {
      if (var1 != null && !var1.isBlank()) {
         try {
            long var4 = Long.parseLong(var1);
            if (var4 > 0L) {
               return var4;
            }

            a.warn("ignoring non-positive {}: {}", var0, var1);
         } catch (NumberFormatException var6) {
            a.warn("ignoring non-numeric {}: {}", var0, var1);
         }
      }

      return var2;
   }

   public interface SandboxFailureMapper {
      RuntimeException a(long var1);

      RuntimeException a(Throwable var1);

      default RuntimeException a() {
         return this.a(new InterruptedException("sandbox call interrupted"));
      }
   }

   public static final class SandboxExecutionScope implements AutoCloseable {
      private final ExecutorService a;
      private final long b;

      public SandboxExecutionScope(String var1, long var2) {
         this.b = var2;
         this.a = Executors.newSingleThreadExecutor(var1x -> {
            Thread var2x = new Thread(var1x, var1);
            var2x.setDaemon(true);
            return var2x;
         });
      }

      public long a() {
         return this.b;
      }

      public <T> T a(Callable<T> var1, io.karatelabs.ext.common.SandboxExecutor.SandboxFailureMapper var2) {
         Future var3 = this.a.submit(var1);

         try {
            return (T)var3.get(this.b, TimeUnit.MILLISECONDS);
         } catch (TimeoutException var5) {
            var3.cancel(true);
            throw var2.a(this.b);
         } catch (ExecutionException var6) {
            throw var2.a((Throwable)(var6.getCause() == null ? var6 : var6.getCause()));
         } catch (InterruptedException var7) {
            Thread.currentThread().interrupt();
            var3.cancel(true);
            throw var2.a();
         }
      }

      @Override
      public void close() {
         this.a.shutdownNow();
      }
   }
}
