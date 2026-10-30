package io.karatelabs.ext.max.apitest;

import io.karatelabs.ext.common.MaxPaths;
import io.karatelabs.ext.common.Sha256;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;
import java.util.function.Supplier;

public final class FeatureStore {
   private static final ConcurrentHashMap<Path, ReentrantLock> LOCKS = new ConcurrentHashMap<>();

   private FeatureStore() {
   }

   public static FeatureSource read(Path var0) throws IOException {
      return FeatureSource.of(Files.readAllBytes(var0));
   }

   public static Map<String, Object> write(Path var0, byte[] var1, String var2) {
      return locked(var0, () -> {
         try {
            var var3 = stale(var0, var2);
            return var3 != null ? var3 : land(var0, var1, var2 == null);
         } catch (IOException var4) {
            return refuse("IO_ERROR", 500, "could not write " + var0 + ": " + var4.getMessage(), null);
         }
      });
   }

   public static Map<String, Object> delete(Path var0, String var1) {
      return locked(var0, () -> {
         try {
            var var2 = stale(var0, var1);
            if (var2 != null) {
               return var2;
            } else {
               Files.delete(var0);
               return null;
            }
         } catch (IOException var3) {
            return refuse("IO_ERROR", 500, "could not delete " + var0 + ": " + var3.getMessage(), null);
         }
      });
   }

   private static Map<String, Object> stale(Path var0, String var1) throws IOException {
      if (var1 == null) {
         return null;
      } else if (!Files.isRegularFile(var0)) {
         return refuse("STALE", 409, var0 + " is gone since it was read", null);
      } else {
         String var2 = io.karatelabs.ext.common.Sha256.a(Files.readAllBytes(var0));
         if (!var2.equals(var1)) {
            LinkedHashMap var3 = new LinkedHashMap();
            var3.put("expectedVersion", var1);
            var3.put("currentVersion", var2);
            return refuse("STALE", 409, var0 + " changed since it was read — read it again and re-apply the edit", var3);
         } else {
            return null;
         }
      }
   }

   public static EditResult apply(Path var0, Function<FeatureSource, EditResult> var1) {
      return locked(var0, () -> {
         FeatureSource var2;
         try {
            var2 = read(var0);
         } catch (IOException var5) {
            return EditResult.refuse("NOT_FOUND", 404, "could not read " + var0 + ": " + var5.getMessage(), null);
         }

         EditResult var3 = (EditResult)var1.apply(var2);
         if (!var3.ok()) {
            return var3;
         } else {
            var var4 = write(var0, var3.bytes(), var2.version());
            return var4 == null ? var3 : EditResult.refuse(var4);
         }
      });
   }

   public static <T> T locked(Path var0, Supplier<T> var1) {
      ReentrantLock var2 = lockOf(var0);
      var2.lock();

      Object var3;
      try {
         var3 = var1.get();
      } finally {
         var2.unlock();
      }

      return (T)var3;
   }

   private static Map<String, Object> land(Path var0, byte[] var1, boolean var2) throws IOException {
      Path var3 = var0.toAbsolutePath().getParent();
      Files.createDirectories(var3);
      Path var4 = Files.createTempFile(var3, "." + var0.getFileName(), ".tmp");

      try {
         Files.write(var4, var1);
         if (var2) {
            try {
               Files.createLink(var0, var4);
            } catch (FileAlreadyExistsException var12) {
               return refuse("EXISTS", 409, var0 + " already exists — read it and edit it instead", null);
            } catch (UnsupportedOperationException var13) {
               return refuse("IO_ERROR", 500, "could not create " + var0 + " atomically: hard links are not supported here", null);
            }
         } else {
            try {
               Files.move(var4, var0, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException var14) {
               return refuse("IO_ERROR", 500, "could not replace " + var0 + " atomically: " + var14.getMessage(), null);
            }
         }

         return null;
      } finally {
         Files.deleteIfExists(var4);
      }
   }

   static ReentrantLock lockOf(Path var0) {
      return LOCKS.computeIfAbsent(MaxPaths.lockKey(var0), var0x -> new ReentrantLock());
   }

   private static Map<String, Object> refuse(String var0, int var1, String var2, Map<String, Object> var3) {
      LinkedHashMap var4 = new LinkedHashMap();
      var4.put("error", var2);
      var4.put("code", var0);
      var4.put("status", var1);
      if (var3 != null) {
         var4.put("detail", var3);
      }

      return var4;
   }
}
