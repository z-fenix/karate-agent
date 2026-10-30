package io.karatelabs.ext.max.jsapi;

import io.karatelabs.core.Suite;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class ActiveRuns {
   private static final Object PENDING = new Object();
   private final ConcurrentHashMap<String, Object> runs = new ConcurrentHashMap<>();
   private final Set<String> stopRequested = ConcurrentHashMap.newKeySet();

   public void start(String var1) {
      if (var1 != null) {
         this.runs.put(var1, PENDING);
      }
   }

   public void suite(String var1, Suite var2) {
      if (var1 != null && var2 != null) {
         this.runs.computeIfPresent(var1, (var1x, var2x) -> var2);
         if (this.stopRequested.contains(var1)) {
            var2.abort();
         }
      }
   }

   public void finish(String var1) {
      if (var1 != null) {
         this.runs.remove(var1);
         this.stopRequested.remove(var1);
      }
   }

   public boolean isActive(String var1) {
      return var1 != null && this.runs.containsKey(var1);
   }

   public Set<String> ids() {
      return Set.copyOf(this.runs.keySet());
   }

   public boolean stop(String var1) {
      if (var1 != null && this.runs.containsKey(var1)) {
         this.stopRequested.add(var1);
         Object var2 = this.runs.get(var1);
         if (var2 instanceof Suite var3) {
            var3.abort();
         }

         if (var2 == null) {
            this.stopRequested.remove(var1);
            return false;
         } else {
            return true;
         }
      } else {
         return false;
      }
   }

   public boolean stopRequested(String var1) {
      return var1 != null && this.stopRequested.contains(var1);
   }
}
