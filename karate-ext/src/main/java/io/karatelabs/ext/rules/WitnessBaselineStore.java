package io.karatelabs.ext.rules;

import io.karatelabs.common.Json;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

public class WitnessBaselineStore {
   private static final String b = ".baseline";
   private static final String c = "witness.json";
   public static final int a = 3;
   private static final Set<String> d = Set.of(
      "against", "sutRevision", "kinds", "selection", "transitions", "pairs", "refusals", "capturedCollapsed", "captureClasses", "shrink", "obligations"
   );
   private static final Set<String> e = Set.of("against", "kinds", "selection", "capturedCollapsed", "captureClasses", "obligations");
   private static final Set<String> f = Set.of("walkSha", "sequencesSha", "planSha");
   private static final Set<String> g = Set.of("witnessed", "compatible", "missed", "refuted", "blocked");
   private static final Set<String> h = Set.of("transitions", "pairs", "refusals");
   private static final Set<String> i = Set.of("kind", "key", "credit", "obligation", "steps", "reason", "code", "at", "mismatch", "confirmed", "shrink", "log");
   private static final Map<Path, ReentrantLock> j = new ConcurrentHashMap<>();
   private final Path k;
   private final ReentrantLock l;

   public WitnessBaselineStore(Path var1) {
      Path var2 = var1.toAbsolutePath().normalize();
      this.k = var2.resolve(".baseline").resolve("witness.json");
      this.l = j.computeIfAbsent(var2, var0 -> new ReentrantLock());
   }

   public io.karatelabs.ext.rules.WitnessBaselineStore.WitnessSnapshot a() {
      this.l.lock();

      io.karatelabs.ext.rules.WitnessBaselineStore.WitnessSnapshot var4;
      try {
         if (!Files.isRegularFile(this.k)) {
            return new io.karatelabs.ext.rules.WitnessBaselineStore.WitnessSnapshot(null, null);
         }

         Object var1;
         try {
            var1 = Json.parseStrict(Files.readString(this.k, StandardCharsets.UTF_8), true);
         } catch (RuntimeException | IOException var9) {
            throw new io.karatelabs.ext.rules.WitnessBaselineStore.WitnessReadException(this.k, var9.toString());
         }

         if (!(var1 instanceof Map var2 && d(var2.get("version")) != null)) {
            throw new io.karatelabs.ext.rules.WitnessBaselineStore.WitnessReadException(this.k, "not a versioned witness record document");
         }

         int var3 = d(var2.get("version"));
         if (var3 == 3) {
            if (!a(var2.get("witness"))) {
               throw new io.karatelabs.ext.rules.WitnessBaselineStore.WitnessReadException(this.k, "the witness is not the declared shape");
            }

            Map var12 = (Map)var2.get("witness");
            return new io.karatelabs.ext.rules.WitnessBaselineStore.WitnessSnapshot(var3, var12);
         }

         var4 = new io.karatelabs.ext.rules.WitnessBaselineStore.WitnessSnapshot(var3, null);
      } finally {
         this.l.unlock();
      }

      return var4;
   }

   public String a(Map<String, Object> var1) {
      if (!a((Object)var1)) {
         throw new IllegalStateException("the witness record is not the declared shape: " + var1);
      } else {
         this.l.lock();

         String var4;
         try {
            LinkedHashMap<java.lang.String, java.lang.Object> var2 = new LinkedHashMap();
            var2.put("version", 3);
            var2.put("witness", var1);
            String var3 = io.karatelabs.ext.rules.twin.CanonicalJson.c(var2);
            io.karatelabs.ext.rules.LiveRecordStore.a(this.k, var3);
            var4 = io.karatelabs.ext.rules.twin.CanonicalJson.a(var3);
         } finally {
            this.l.unlock();
         }

         return var4;
      }
   }

   public static boolean a(Object var0) {
      if (var0 instanceof Map var1
         && d.containsAll(var1.keySet())
         && var1.keySet().containsAll(e)
         && ("mock".equals(var1.get("against")) || "live".equals(var1.get("against")))
         && (var1.get("sutRevision") == null || var1.get("sutRevision") instanceof String)
         && c(var1.get("kinds"))
         && var1.get("selection") instanceof Map var2
         && f.equals(var2.keySet())
         && var2.get("walkSha") instanceof String
         && var2.get("planSha") instanceof String
         && d(var1.get("capturedCollapsed")) != null
         && var1.get("captureClasses") instanceof List var3
         && var1.get("obligations") instanceof List var4) {
         for (Object var6 : h) {
            boolean var7 = ((List)var1.get("kinds")).contains(var6);
            if (var7 != var1.containsKey(var6) || var7 && !b(var1.get(var6))) {
               return false;
            }
         }

         Iterator var13 = var3.iterator();

         while (var13.hasNext()) {
            if (!(var13.next() instanceof Map var17)
               || !var17.keySet().equals(Set.of("key", "members", "maskedFields"))
               || !(var17.get("key") instanceof String)
               || !(var17.get("members") instanceof List var8)
               || var8.size() < 2
               || !(var17.get("maskedFields") instanceof List)) {
               return false;
            }
         }

         var13 = var4.iterator();

         while (var13.hasNext()) {
            if (!(var13.next() instanceof Map var18)
               || !i.containsAll(var18.keySet())
               || !h.contains(var18.get("kind"))
               || !(var18.get("key") instanceof String)
               || !g.contains(var18.get("credit"))
               || !(var18.get("obligation") instanceof Map)
               || !(var18.get("steps") instanceof List)
               || "refuted".equals(var18.get("credit")) != (var18.get("confirmed") instanceof Boolean)
               || "refuted".equals(var18.get("credit")) && !(var18.get("log") instanceof List)) {
               return false;
            }
         }

         return true;
      } else {
         return false;
      }
   }

   private static boolean b(Object var0) {
      if (var0 instanceof Map var1 && var1.keySet().containsAll(g) && d(var1.get("of")) != null) {
         for (Object var3 : g) {
            if (d(var1.get(var3)) == null) {
               return false;
            }
         }

         return true;
      } else {
         return false;
      }
   }

   private static boolean c(Object var0) {
      if (var0 instanceof List var1 && !var1.isEmpty()) {
         for (Object var3 : var1) {
            if (!h.contains(var3)) {
               return false;
            }
         }

         return var1.size() == Set.copyOf(var1).size();
      } else {
         return false;
      }
   }

   private static Integer d(Object var0) {
      return var0 instanceof Number var1 && var1.doubleValue() == Math.rint(var1.doubleValue()) && !(Math.abs(var1.doubleValue()) > 2.147483647E9)
         ? var1.intValue()
         : null;
   }

   public static class WitnessReadException extends RuntimeException {
      WitnessReadException(Path var1, String var2) {
         super("the witness record at " + var1 + " cannot be read (" + var2 + ")");
      }
   }

   public record WitnessSnapshot(Integer a, Map<String, Object> b) {

      public WitnessSnapshot(Integer a, Map<String, Object> b) {
         this.a = a;
         this.b = b;
      }
   }
}
