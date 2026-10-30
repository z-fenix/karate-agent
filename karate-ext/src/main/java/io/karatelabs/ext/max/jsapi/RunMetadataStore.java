package io.karatelabs.ext.max.jsapi;

import io.karatelabs.common.Json;
import io.karatelabs.ext.common.MaxPaths;
import io.karatelabs.ext.common.TempFileCreator;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class RunMetadataStore {
   private static final Logger a = LoggerFactory.getLogger(RunMetadataStore.class);
   private static final String b = "meta.json";
   private final Path c;
   private final Supplier<UnaryOperator<String>> d;
   private static final Object e = new Object();

   RunMetadataStore(Path var1) {
      this(var1, UnaryOperator::identity);
   }

   RunMetadataStore(Path var1, Supplier<UnaryOperator<String>> var2) {
      this.c = var1.resolve("runs");
      this.d = var2;
   }

   Path a(String var1) {
      return this.c.resolve(var1);
   }

   void a() {
      try {
         Files.createDirectories(this.c);
         Path var1 = this.c.resolve(".karate-write-probe");
         Files.writeString(var1, "ok");
         Files.deleteIfExists(var1);
      } catch (Exception var3) {
         String var2 = System.getProperty("user.name", "?");
         throw new IllegalStateException(
            "cannot write runs to '"
               + this.c
               + "' — the working folder is not writable by this process (user '"
               + var2
               + "'). A run would otherwise execute but persist no record (no meta.json, transcript, or report), so it would not appear in the Runs list. If this is a Docker bind mount, make the mounted folder writable by the container user — chown it to the container uid, or run with a matching --user. Cause: "
               + var3,
            var3
         );
      }
   }

   void a(String var1, Map<String, Object> var2) {
      Path var3 = null;

      try {
         Path var4 = this.a(var1);
         Files.createDirectories(var4);
         var3 = io.karatelabs.ext.common.TempFileCreator.a(var4, "meta.json", ".tmp");
         Files.writeString(var3, this.d.get().apply(Json.stringifyStrict(var2)));
         Files.move(var3, var4.resolve("meta.json"), StandardCopyOption.ATOMIC_MOVE);
      } catch (Exception var13) {
         a.warn("could not write run meta {}: {}", var1, var13.getMessage());
      } finally {
         if (var3 != null) {
            try {
               Files.deleteIfExists(var3);
            } catch (Exception var12) {
            }
         }
      }
   }

   Map<String, Object> a(String var1, Consumer<Map<String, Object>> var2) {
      synchronized (e) {
         Map<String, Object> var4 = Files.isRegularFile(this.a(var1).resolve("meta.json")) ? this.b(var1) : null;
         if (var4 == null) {
            var4 = new LinkedHashMap();
            var4.put("runId", var1);
         }

         var2.accept(var4);
         this.a(var1, (Map<String, Object>)var4);
         return (Map<String, Object>)var4;
      }
   }

   Map<String, Object> b(String var1) {
      Path var2 = this.a(var1).resolve("meta.json");
      if (!Files.isRegularFile(var2)) {
         if (Files.isDirectory(this.a(var1))) {
            LinkedHashMap<java.lang.String, java.lang.Object> var9 = new LinkedHashMap();
            var9.put("runId", var1);
            var9.put("status", Files.exists(this.a(var1).resolve("transcript.jsonl")) ? "unknown" : "nometa");
            return var9;
         } else {
            return null;
         }
      } else {
         try {
            if (Json.of(Files.readString(var2)).value() instanceof Map var4) {
               if (var4.get("runId") instanceof String var7 && !var7.isBlank() && !var7.equals(var1)) {
                  a.debug("run folder '{}' holds a meta minted as '{}' — the folder name wins", var1, var7);
               }

               var4.put("runId", var1);
               return var4;
            } else {
               return null;
            }
         } catch (Exception var8) {
            a.warn("could not read run meta {}: {}", var1, var8.getMessage());
            return null;
         }
      }
   }

   List<Map<String, Object>> b() {
      ArrayList<java.util.Map<java.lang.String,java.lang.Object>> var1 = new ArrayList();
      if (!Files.isDirectory(this.c)) {
         return var1;
      } else {
         try (var var2 = Files.list(this.c)) {
            var2.filter(var0 -> Files.isDirectory(var0)).forEach(var2x -> {
               var var3 = this.b(var2x.getFileName().toString());
               if (var3 != null) {
                  var1.add(var3);
               }
            });
         } catch (Exception var7) {
            a.warn("could not list runs: {}", var7.getMessage());
         }

         var1.sort(Comparator.<Map>comparingLong(var0 -> a(var0.get("startedMs"))).reversed());
         return var1;
      }
   }

   int c() {
      if (!Files.isDirectory(this.c)) {
         return 0;
      } else {
         try {
            int var2;
            try (var var1 = Files.list(this.c)) {
               var2 = (int)var1.filter(var0 -> Files.isDirectory(var0)).count();
            }

            return var2;
         } catch (Exception var6) {
            a.warn("could not count runs: {}", var6.getMessage());
            return 0;
         }
      }
   }

   boolean c(String var1) {
      return Files.isRegularFile(this.a(var1).resolve("meta.json"));
   }

   boolean d(String var1) {
      return Files.isRegularFile(this.a(var1).resolve("video.mp4"));
   }

   boolean e(String var1) {
      if (var1 != null && !var1.isBlank() && !var1.contains("..") && !var1.contains("/") && !var1.contains("\\")) {
         Path var2;
         try {
            var2 = MaxPaths.within(this.c, this.a(var1));
         } catch (SecurityException var7) {
            return false;
         }

         if (!Files.isDirectory(var2)) {
            return false;
         } else {
            try {
               boolean var4;
               try (var var3 = Files.walk(var2)) {
                  var3.sorted(Comparator.reverseOrder()).forEach(var0 -> {
                     try {
                        Files.delete(var0);
                     } catch (Exception var2x) {
                        a.warn("could not delete {}: {}", var0, var2x.getMessage());
                     }
                  });
                  var4 = !Files.exists(var2);
               }

               return var4;
            } catch (Exception var9) {
               a.warn("could not delete run {}: {}", var1, var9.getMessage());
               return false;
            }
         }
      } else {
         return false;
      }
   }

   static long a(Object var0) {
      return var0 instanceof Number var1 ? var1.longValue() : 0L;
   }
}
