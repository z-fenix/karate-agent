package io.karatelabs.ext.max.run;

import io.karatelabs.ext.agent.events.AgentEvent;
import io.karatelabs.ext.agent.events.AgentEventEmitter;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiPredicate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class LiveRunSink implements AgentEventEmitter {
   private static final Logger logger = LoggerFactory.getLogger(LiveRunSink.class);
   private final BiPredicate<String, String> stream;
   private final ExecutorService writer;
   private final AtomicInteger failed = new AtomicInteger();

   public LiveRunSink(BiPredicate<String, String> var1) {
      this.stream = var1;
      this.writer = Executors.newSingleThreadExecutor(var0 -> {
         Thread var1x = new Thread(var0, "max-recorder-sink");
         var1x.setDaemon(true);
         return var1x;
      });
   }

   @Override
   public void emit(AgentEvent var1) {
      String var2 = var1.a();
      String var3 = var1.e();

      try {
         this.writer.submit(() -> {
            try {
               if (!this.stream.test(var2, var3)) {
                  this.failed.incrementAndGet();
               }
            } catch (Exception var4) {
               this.failed.incrementAndGet();
               logger.warn("recorder sink write failed ({}): {}", var2, var4.getMessage());
            }
         });
      } catch (RejectedExecutionException var5) {
         this.failed.incrementAndGet();
         logger.warn("recorder sink closed — {} not written", var2);
      }
   }

   public void close() {
      this.drain(2000L);
   }

   public boolean drain(long var1) {
      this.writer.shutdown();

      try {
         return this.writer.awaitTermination(var1, TimeUnit.MILLISECONDS) && this.failed.get() == 0;
      } catch (InterruptedException var4) {
         Thread.currentThread().interrupt();
         return false;
      }
   }
}
