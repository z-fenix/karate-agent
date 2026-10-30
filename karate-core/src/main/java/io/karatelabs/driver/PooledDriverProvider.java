package io.karatelabs.driver;

import io.karatelabs.core.ScenarioRuntime;
import io.karatelabs.driver.cdp.CdpDriver;
import io.karatelabs.driver.cdp.CdpDriverOptions;
import io.karatelabs.driver.w3c.W3cBrowserType;
import io.karatelabs.driver.w3c.W3cDriver;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PooledDriverProvider implements DriverProvider {
   private static final Logger logger = LoggerFactory.getLogger(PooledDriverProvider.class);
   private volatile int poolSize = -1;
   private volatile ArrayBlockingQueue<Driver> availableDrivers;
   private final ConcurrentHashMap<Driver, ScenarioRuntime> assignedDrivers = new ConcurrentHashMap<>();
   private final AtomicInteger createdCount = new AtomicInteger(0);
   private final Object initLock = new Object();
   private final ReentrantLock createLock = new ReentrantLock();
   private volatile boolean shutdown = false;

   public PooledDriverProvider() {
   }

   public PooledDriverProvider(int poolSize) {
      if (poolSize < 1) {
         throw new IllegalArgumentException("Pool size must be at least 1");
      } else {
         this.poolSize = poolSize;
         this.availableDrivers = new ArrayBlockingQueue<>(poolSize);
      }
   }

   @Override
   public Driver acquire(ScenarioRuntime runtime, Map<String, Object> config) {
      if (this.shutdown) {
         throw new IllegalStateException("Provider has been shut down");
      } else {
         this.ensurePoolInitialized(runtime);

         for (Entry<Driver, ScenarioRuntime> entry : this.assignedDrivers.entrySet()) {
            if (entry.getValue() == runtime && !entry.getKey().isTerminated()) {
               logger.debug("Returning existing driver for scenario: {}", runtime.getScenario().getName());
               return entry.getKey();
            }
         }

         Driver driver = this.takeOrCreate(config, runtime.getScenario().getName());
         this.assignedDrivers.put(driver, runtime);
         return driver;
      }
   }

   private void reapTerminated() {
      for (Driver driver : this.assignedDrivers.keySet()) {
         if (driver.isTerminated() && this.assignedDrivers.remove(driver) != null) {
            logger.debug("Freeing pool slot of quit driver");
            this.createdCount.decrementAndGet();
         }
      }
   }

   private Driver takeHealthyFromPool(String scenarioName) {
      return this.healthyOrDiscard(this.availableDrivers.poll(), scenarioName);
   }

   private Driver healthyOrDiscard(Driver driver, String scenarioName) {
      if (driver == null) {
         return null;
      } else if (driver.isTerminated()) {
         logger.debug("Discarding terminated driver from pool");
         this.createdCount.decrementAndGet();
         return null;
      } else if (!this.resetDriver(driver)) {
         this.closeDriverQuietly(driver);
         this.createdCount.decrementAndGet();
         return null;
      } else {
         logger.debug("Reusing pooled driver for scenario: {}", scenarioName);
         return driver;
      }
   }

   private void ensurePoolInitialized(ScenarioRuntime runtime) {
      if (this.availableDrivers == null) {
         synchronized (this.initLock) {
            if (this.availableDrivers == null) {
               if (this.poolSize < 1 && runtime.getFeatureRuntime() != null && runtime.getFeatureRuntime().getSuite() != null) {
                  this.poolSize = runtime.getFeatureRuntime().getSuite().threadCount;
                  logger.info("Auto-detected pool size from Suite: {}", this.poolSize);
               }

               if (this.poolSize < 1) {
                  this.poolSize = 1;
                  logger.warn("Could not detect pool size, defaulting to 1");
               }

               this.availableDrivers = new ArrayBlockingQueue<>(this.poolSize);
            }
         }
      }
   }

   private Driver takeOrCreate(Map<String, Object> config, String scenarioName) {
      long deadline = 0L;

      Driver driver;
      do {
         driver = this.takeHealthyFromPool(scenarioName);
         if (driver != null) {
            return driver;
         }

         this.reapTerminated();
         if (this.reserveSlot()) {
            this.createLock.lock();

            try {
               driver = this.createDriver(config);
            } catch (Error | RuntimeException var11) {
               this.createdCount.decrementAndGet();
               throw var11;
            } finally {
               this.createLock.unlock();
            }

            logger.info("Created driver {}/{} for scenario: {}", new Object[]{this.createdCount.get(), this.poolSize, scenarioName});
            return driver;
         }

         if (deadline == 0L) {
            logger.warn("Pool exhausted for scenario '{}', waiting for available driver... [{}]", scenarioName, this.getStats());
            deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(30L);
         } else if (System.nanoTime() > deadline) {
            throw new RuntimeException("Timeout waiting for available driver for scenario '" + scenarioName + "' [" + this.getStats() + "]");
         }

         try {
            driver = this.healthyOrDiscard(this.availableDrivers.poll(100L, TimeUnit.MILLISECONDS), scenarioName);
         } catch (InterruptedException var13) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while waiting for driver", var13);
         }
      } while (driver == null);

      return driver;
   }

   private boolean reserveSlot() {
      int count;
      do {
         count = this.createdCount.get();
         if (count >= this.poolSize) {
            return false;
         }
      } while (!this.createdCount.compareAndSet(count, count + 1));

      return true;
   }

   @Override
   public void release(ScenarioRuntime runtime, Driver driver) {
      boolean known = this.assignedDrivers.remove(driver) != null;
      if (this.shutdown) {
         this.closeDriverQuietly(driver);
      } else if (known) {
         if (driver.isTerminated()) {
            logger.debug("Not returning terminated driver to pool");
            this.createdCount.decrementAndGet();
         } else if (!driver.isResponsive()) {
            logger.warn("Driver failed liveness probe on release, discarding instead of returning to pool");
            this.closeDriverQuietly(driver);
            this.createdCount.decrementAndGet();
         } else {
            this.cleanScenarioState(driver);
            boolean offered = this.availableDrivers.offer(driver);
            if (!offered) {
               logger.warn("Pool full, closing excess driver");
               this.closeDriverQuietly(driver);
               this.createdCount.decrementAndGet();
            } else if (this.shutdown && this.availableDrivers.remove(driver)) {
               this.closeDriverQuietly(driver);
            } else {
               logger.debug("Returned driver to pool for scenario: {}", runtime.getScenario().getName());
            }
         }
      }
   }

   protected void cleanScenarioState(Driver driver) {
      try {
         driver.stopIntercept();
      } catch (UnsupportedOperationException var5) {
      } catch (Exception var6) {
         logger.debug("stopIntercept on release failed (non-fatal): {}", var6.getMessage());
      }

      try {
         driver.onDialog(null);
      } catch (UnsupportedOperationException var3) {
      } catch (Exception var4) {
         logger.debug("onDialog(null) on release failed (non-fatal): {}", var4.getMessage());
      }
   }

   @Override
   public void shutdown() {
      this.shutdown = true;
      int count = this.createdCount.get();
      logger.debug("Shutting down PooledDriverProvider, closing {} drivers", count);

      for (Driver driver : this.assignedDrivers.keySet()) {
         this.closeDriverQuietly(driver);
      }

      this.assignedDrivers.clear();
      Driver driver;
      if (this.availableDrivers != null) {
         while ((driver = this.availableDrivers.poll()) != null) {
            this.closeDriverQuietly(driver);
         }
      }
   }

   protected boolean resetDriver(Driver driver) {
      try {
         Dialog dialog = driver.getDialog();
         if (dialog != null) {
            logger.debug("Dismissing stale dialog before reset");

            try {
               dialog.dismiss();
            } catch (Exception var4) {
               logger.debug("Dialog dismiss during reset (may already be handled): {}", var4.getMessage());
            }
         }

         driver.setUrl("about:blank");
         driver.clearCookies();
         driver.waitUntilReady();
      } catch (Exception var5) {
         logger.warn("Error resetting driver state, will discard: {}", var5.getMessage());
         return false;
      }

      if (!driver.isResponsive()) {
         logger.warn("Driver failed post-reset liveness probe, will discard");
         return false;
      } else {
         return true;
      }
   }

   protected Driver createDriver(Map<String, Object> config) {
      String type = (String)config.getOrDefault("type", "chrome");
      if (W3cBrowserType.isW3cType(type)) {
         return W3cDriver.start(config);
      } else {
         CdpDriverOptions options = CdpDriverOptions.fromMap(config);
         String wsUrl = options.getWebSocketUrl();
         if (wsUrl == null || wsUrl.isEmpty()) {
            return CdpDriver.start(options);
         } else if (wsUrl.contains("/devtools/browser/")) {
            return CdpDriver.connectNewContext(wsUrl, options);
         } else {
            if (this.poolSize > 1) {
               logger.warn(
                  "pool size {} but webSocketUrl names a single page — all slots will drive the SAME tab. Pass a browser-level endpoint (see /json/version) for isolated slots: {}",
                  this.poolSize,
                  wsUrl
               );
            }

            return CdpDriver.connect(wsUrl, options);
         }
      }
   }

   private void closeDriverQuietly(Driver driver) {
      try {
         if (!driver.isTerminated()) {
            driver.quit();
         }
      } catch (Exception var3) {
         logger.warn("Error closing driver: {}", var3.getMessage());
      }
   }

   public int getPoolSize() {
      return this.poolSize;
   }

   public String getStats() {
      return String.format(
         "PooledDriverProvider[size=%d, created=%d, available=%d, assigned=%d]",
         this.poolSize,
         this.createdCount.get(),
         this.availableDrivers != null ? this.availableDrivers.size() : 0,
         this.assignedDrivers.size()
      );
   }
}
