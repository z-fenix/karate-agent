package io.karatelabs.ext.max.apitest;

import io.karatelabs.ext.common.MaxPaths;
import java.nio.file.Path;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

public final class RepoLock {
   public static final long WAIT_MS = 5000L;
   private static final ConcurrentHashMap<Path, ReentrantLock> LOCKS = new ConcurrentHashMap<>();

   private RepoLock() {
   }

   public static ReentrantLock of(Path var0) {
      return LOCKS.computeIfAbsent(MaxPaths.dirLockKey(var0), var0x -> new ReentrantLock());
   }

   public static boolean acquire(Path var0) {
      try {
         return of(var0).tryLock(5000L, TimeUnit.MILLISECONDS);
      } catch (InterruptedException var2) {
         Thread.currentThread().interrupt();
         return false;
      }
   }

   public static void release(Path var0) {
      of(var0).unlock();
   }
}
