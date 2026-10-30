package io.karatelabs.driver;

import io.karatelabs.js.JavaCallable;
import io.karatelabs.js.SimpleObject;
import java.time.Duration;

class RetryableDriver implements SimpleObject {
   private final Driver driver;
   private final Duration timeout;

   RetryableDriver(Driver driver, Integer count, Integer interval) {
      this.driver = driver;
      this.timeout = timeout(driver, count, interval);
   }

   static Duration timeout(Driver driver, Integer count, Integer interval) {
      int effectiveCount = count != null ? count : driver.getOptions().getRetryCount();
      int effectiveInterval = interval != null ? interval : driver.getOptions().getRetryInterval();
      return Duration.ofMillis((long)effectiveCount * effectiveInterval);
   }

   @Override
   public Object jsGet(String name) {
      return switch (name) {
         case "waitUntil" -> (JavaCallable) (ctx, args) -> args.length == 1
            ? this.driver.waitUntil(String.valueOf(args[0]), this.timeout)
            : this.driver.waitUntil(String.valueOf(args[0]), String.valueOf(args[1]), this.timeout);
         case "waitFor" -> (JavaCallable) (ctx, args) -> this.driver.waitFor(String.valueOf(args[0]), this.timeout);
         case "waitForText" -> (JavaCallable) (ctx, args) -> this.driver.waitForText(String.valueOf(args[0]), String.valueOf(args[1]), this.timeout);
         case "waitForEnabled" -> (JavaCallable) (ctx, args) -> this.driver.waitForEnabled(String.valueOf(args[0]), this.timeout);
         case "waitForUrl" -> (JavaCallable) (ctx, args) -> this.driver.waitForUrl(String.valueOf(args[0]), this.timeout);
         case "waitForResultCount" -> (JavaCallable) (ctx, args) -> this.driver.waitForResultCount(String.valueOf(args[0]), ((Number)args[1]).intValue(), this.timeout);
         case "click" -> (JavaCallable) (ctx, args) -> {
            String locator = String.valueOf(args[0]);
            this.driver.waitFor(locator, this.timeout);
            return this.driver.click(locator);
         };
         case "input" -> (JavaCallable) (ctx, args) -> {
            String locator = String.valueOf(args[0]);
            this.driver.waitFor(locator, this.timeout);
            return this.driver.input(locator, args.length > 1 ? String.valueOf(args[1]) : "");
         };
         default -> this.driver.jsGet(name);
      };
   }
}
