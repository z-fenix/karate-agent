package io.karatelabs.ext.agent.runtime;

public record RetryPolicy(int maxRetries, long initialBackoffMillis, double multiplier, long maxBackoffMillis) {

   public RetryPolicy(int maxRetries, long initialBackoffMillis, double multiplier, long maxBackoffMillis) {
      if (maxRetries < 0) {
         throw new IllegalArgumentException("maxRetries must be >= 0: " + maxRetries);
      } else if (initialBackoffMillis < 0L || maxBackoffMillis < 0L) {
         throw new IllegalArgumentException("backoff must be >= 0");
      } else if (multiplier < 1.0) {
         throw new IllegalArgumentException("multiplier must be >= 1.0: " + multiplier);
      } else {
         this.maxRetries = maxRetries;
         this.initialBackoffMillis = initialBackoffMillis;
         this.multiplier = multiplier;
         this.maxBackoffMillis = maxBackoffMillis;
      }
   }

   public static RetryPolicy defaults() {
      return new RetryPolicy(2, 500L, 2.0, 30000L);
   }

   public static RetryPolicy noRetries() {
      return new RetryPolicy(0, 0L, 1.0, 0L);
   }

   public long backoffMillis(int var1) {
      double var2 = this.initialBackoffMillis * Math.pow(this.multiplier, var1);
      return (long)Math.min(var2, (double)this.maxBackoffMillis);
   }

   public int maxRetries() {
      return this.maxRetries;
   }

   public long initialBackoffMillis() {
      return this.initialBackoffMillis;
   }

   public double multiplier() {
      return this.multiplier;
   }

   public long maxBackoffMillis() {
      return this.maxBackoffMillis;
   }
}
