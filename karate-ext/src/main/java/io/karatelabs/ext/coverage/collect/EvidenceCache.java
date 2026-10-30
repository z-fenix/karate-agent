package io.karatelabs.ext.coverage.collect;

import io.karatelabs.ext.common.MaxPaths;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

public final class EvidenceCache {
   private static final int a = 256;
   private static final Map<io.karatelabs.ext.coverage.collect.EvidenceCache.EvidenceCacheKey, JsonlRunEvidenceReader> b = b();
   private static final Map<io.karatelabs.ext.coverage.collect.EvidenceCache.EvidenceCacheKey, RunEvidenceBundle> c = b();
   private static final Map<Path, Integer> d = new ConcurrentHashMap<>();

   private EvidenceCache() {
   }

   public static int a(Path var0) {
      return d.getOrDefault(b(var0), 0);
   }

   private static Path b(Path var0) {
      try {
         return var0.toRealPath();
      } catch (IOException var2) {
         return var0.toAbsolutePath().normalize();
      }
   }

   public static void a() {
      synchronized (b) {
         b.clear();
      }

      synchronized (c) {
         c.clear();
      }

      d.clear();
   }

   static JsonlRunEvidenceReader a(Path var0, io.karatelabs.ext.coverage.collect.EvidenceCache.IoSupplier<JsonlRunEvidenceReader> var1) throws IOException {
      Path var2 = b(var0);
      Path var3 = var2.getParent() == null ? null : var2.getParent().getParent();
      io.karatelabs.ext.coverage.collect.EvidenceCache.EvidenceCacheKey var4 = new io.karatelabs.ext.coverage.collect.EvidenceCache.EvidenceCacheKey(var2, a(var2, var3 == null ? null : var3.resolve("meta.json")));
      synchronized (b) {
         JsonlRunEvidenceReader var6 = b.get(var4);
         if (var6 != null) {
            return var6;
         }
      }

      JsonlRunEvidenceReader var11 = (JsonlRunEvidenceReader)var1.get();
      d.merge(var2, 1, Integer::sum);
      if (var11.f()) {
         synchronized (b) {
            b.put(var4, var11);
         }
      }

      return var11;
   }

   public static RunEvidenceBundle a(Path var0, List<ExecutionFragment> var1) {
      Path var2 = b(var0);
      io.karatelabs.ext.coverage.collect.EvidenceCache.EvidenceCacheKey var3 = new io.karatelabs.ext.coverage.collect.EvidenceCache.EvidenceCacheKey(var2, a(var2.resolve("transcript.jsonl"), var2.resolve("meta.json"), MaxPaths.eventsFile(var2)));
      synchronized (c) {
         RunEvidenceBundle var5 = c.get(var3);
         if (var5 != null) {
            return var5;
         }
      }

      RunEvidenceBundle var10 = io.karatelabs.ext.coverage.collect.RunEvidenceBundle.a(var2, var1);
      if (var10.b()) {
         synchronized (c) {
            c.put(var3, var10);
         }
      }

      return var10;
   }

   private static List<Long> a(Path... var0) {
      Long[] var1 = new Long[var0.length * 2];

      for (int var2 = 0; var2 < var0.length; var2++) {
         long var3 = -1L;
         long var5 = -1L;

         try {
            if (var0[var2] != null && Files.isRegularFile(var0[var2])) {
               var3 = Files.size(var0[var2]);
               var5 = Files.getLastModifiedTime(var0[var2]).toMillis();
            }
         } catch (IOException var8) {
         }

         var1[2 * var2] = var3;
         var1[2 * var2 + 1] = var5;
      }

      return List.of(var1);
   }

   private static <T> Map<io.karatelabs.ext.coverage.collect.EvidenceCache.EvidenceCacheKey, T> b() {
      return new LinkedHashMap<io.karatelabs.ext.coverage.collect.EvidenceCache.EvidenceCacheKey, T>(64, 0.75F, true) {
         @Override
         protected boolean removeEldestEntry(Entry<io.karatelabs.ext.coverage.collect.EvidenceCache.EvidenceCacheKey, T> var1) {
            return this.size() > 256;
         }
      };
   }

   private record EvidenceCacheKey(Path a, List<Long> b) {}

   interface IoSupplier<T> {
      T get() throws IOException;
   }
}
