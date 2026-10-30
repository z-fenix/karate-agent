package io.karatelabs.core;

import io.karatelabs.gherkin.Scenario;
import io.karatelabs.gherkin.Tag;
import io.karatelabs.output.LogContext;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.slf4j.Logger;

public class ScenarioLockManager {
   private static final Logger logger = LogContext.RUNTIME_LOGGER;
   public static final String EXCLUSIVE_LOCK = "*";
   private final ConcurrentHashMap<String, ReentrantLock> namedLocks = new ConcurrentHashMap<>();
   private final ReentrantReadWriteLock globalLock = new ReentrantReadWriteLock();

   public ScenarioLockManager.LockHandle acquire(Scenario scenario) {
      List<String> lockNames = this.getLockNames(scenario);
      String scenarioName = scenario.getName();
      boolean exclusive = lockNames.contains("*");
      if (exclusive) {
         logger.info("Acquiring exclusive lock for scenario: {}", scenarioName);
         this.globalLock.writeLock().lock();
         logger.info("Acquired exclusive lock for scenario: {}", scenarioName);
         return new ScenarioLockManager.LockHandle(scenarioName, Collections.emptyList(), true);
      } else {
         this.globalLock.readLock().lock();
         if (lockNames.isEmpty()) {
            return new ScenarioLockManager.LockHandle(scenarioName, Collections.emptyList(), false);
         } else {
            List<String> sortedNames = new ArrayList<>(lockNames);
            Collections.sort(sortedNames);
            List<ReentrantLock> acquiredLocks = new ArrayList<>();

            for (String name : sortedNames) {
               ReentrantLock lock = this.namedLocks.computeIfAbsent(name, k -> new ReentrantLock());
               logger.info("Acquiring lock '{}' for scenario: {}", name, scenarioName);
               lock.lock();
               logger.info("Acquired lock '{}' for scenario: {}", name, scenarioName);
               acquiredLocks.add(lock);
            }

            return new ScenarioLockManager.LockHandle(scenarioName, acquiredLocks, false);
         }
      }
   }

   public void release(ScenarioLockManager.LockHandle handle) {
      if (handle != null) {
         if (handle.exclusive) {
            logger.info("Releasing exclusive lock for scenario: {}", handle.scenarioName);
            this.globalLock.writeLock().unlock();
         } else {
            List<ReentrantLock> locks = handle.locks;

            for (int i = locks.size() - 1; i >= 0; i--) {
               locks.get(i).unlock();
            }

            logger.debug("Released lock(s) for scenario: {}", handle.scenarioName);
            this.globalLock.readLock().unlock();
         }
      }
   }

   private List<String> getLockNames(Scenario scenario) {
      List<Tag> tags = scenario.getTagsEffective();
      boolean hasLockTag = false;

      for (Tag tag : tags) {
         if ("lock".equals(tag.getName())) {
            hasLockTag = true;
            break;
         }
      }

      if (!hasLockTag) {
         return Collections.emptyList();
      } else {
         List<String> lockNames = new ArrayList<>();

         for (Tag tagx : tags) {
            if ("lock".equals(tagx.getName())) {
               List<String> values = tagx.getValues();
               if (!values.isEmpty()) {
                  lockNames.addAll(values);
               }
            }
         }

         return lockNames;
      }
   }

   public static class LockHandle {
      private final String scenarioName;
      private final List<ReentrantLock> locks;
      private final boolean exclusive;

      LockHandle(String scenarioName, List<ReentrantLock> locks, boolean exclusive) {
         this.scenarioName = scenarioName;
         this.locks = locks;
         this.exclusive = exclusive;
      }
   }
}
