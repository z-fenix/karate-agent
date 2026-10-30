package io.karatelabs.ext.rules;

import io.karatelabs.common.Json;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

public class TwinOrdersRepository {
   private static final String b = ".baseline";
   private static final String c = "orders.json";
   public static final int a = 1;
   private static final Set<String> d = Set.of(
      "against", "sutRevision", "strength", "alphabet", "selection", "ceilings", "targets", "readBack", "hook", "sequences"
   );
   private static final Set<String> e = Set.of("against", "strength", "alphabet", "selection", "ceilings", "targets", "readBack", "sequences");
   private static final Set<String> f = Set.of("twinSha", "rulebooks", "inputSha", "planSha", "alphabetSha", "deckSha", "constructionVersion");
   private static final Set<String> g = Set.of("of", "planned", "applied", "unconfirmed", "residual");
   private static final Set<String> h = Set.of("id", "steps", "planned", "disposition", "checked", "credited", "applied", "confirmed", "shrink", "log");
   private static final Map<Path, ReentrantLock> i = new ConcurrentHashMap<>();
   private final Path j;
   private final ReentrantLock k;

   public TwinOrdersRepository(Path var1) {
      Path var2 = var1.toAbsolutePath().normalize();
      this.j = var2.resolve(".baseline").resolve("orders.json");
      this.k = i.computeIfAbsent(var2, var0 -> new ReentrantLock());
   }

   public io.karatelabs.ext.rules.TwinOrdersRepository.OrdersRecord a() {
      this.k.lock();

      io.karatelabs.ext.rules.TwinOrdersRepository.OrdersRecord var4;
      try {
         if (!Files.isRegularFile(this.j)) {
            return new io.karatelabs.ext.rules.TwinOrdersRepository.OrdersRecord(null, null);
         }

         Object var1;
         try {
            var1 = Json.parseStrict(Files.readString(this.j, StandardCharsets.UTF_8), true);
         } catch (RuntimeException | IOException var9) {
            throw new io.karatelabs.ext.rules.TwinOrdersRepository.OrdersRecordReadException(this.j, var9.toString());
         }

         if (!(var1 instanceof Map var2 && b(var2.get("version")) != null)) {
            throw new io.karatelabs.ext.rules.TwinOrdersRepository.OrdersRecordReadException(this.j, "not a versioned record document");
         }

         int var3 = b(var2.get("version"));
         if (var3 == 1) {
            if (!a(var2.get("orders"))) {
               throw new io.karatelabs.ext.rules.TwinOrdersRepository.OrdersRecordReadException(this.j, "the orders record is not the declared shape");
            }

            Map var12 = (Map)var2.get("orders");
            return new io.karatelabs.ext.rules.TwinOrdersRepository.OrdersRecord(var3, var12);
         }

         var4 = new io.karatelabs.ext.rules.TwinOrdersRepository.OrdersRecord(var3, null);
      } finally {
         this.k.unlock();
      }

      return var4;
   }

   public String a(Map<String, Object> var1) {
      if (!a((Object)var1)) {
         throw new IllegalStateException("the orders record is not the declared shape: " + var1);
      } else {
         this.k.lock();

         String var4;
         try {
            LinkedHashMap<java.lang.String, java.lang.Object> var2 = new LinkedHashMap();
            var2.put("version", 1);
            var2.put("orders", var1);
            String var3 = io.karatelabs.ext.rules.twin.CanonicalJson.c(var2);
            io.karatelabs.ext.rules.LiveRecordStore.a(this.j, var3);
            var4 = io.karatelabs.ext.rules.twin.CanonicalJson.a(var3);
         } finally {
            this.k.unlock();
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
         && var1.get("readBack") instanceof Boolean
         && var1.get("alphabet") instanceof Map var2
         && var2.keySet().equals(Set.of("labels", "of"))
         && var2.get("labels") instanceof List var3
         && Integer.valueOf(var3.size()).equals(b(var2.get("of")))
         && var1.get("selection") instanceof Map var4
         && f.equals(var4.keySet())
         && var4.get("rulebooks") instanceof Map var5
         && io.karatelabs.ext.rules.twin.CanonicalJson.a(io.karatelabs.ext.rules.twin.CanonicalJson.c(var5)).equals(var4.get("inputSha"))
         && var1.get("ceilings") instanceof Map var6
         && var6.keySet().equals(Set.of("hit", "modelSteps", "sequences"))
         && var1.get("targets") instanceof Map var7
         && g.equals(var7.keySet())
         && var1.get("sequences") instanceof List var8) {
         Integer var30 = b(var1.get("strength"));
         Integer var10 = b(var7.get("of"));
         Integer var11 = b(var7.get("planned"));
         Integer var12 = b(var7.get("applied"));
         Integer var13 = b(var7.get("unconfirmed"));
         Integer var14 = b(var7.get("residual"));
         if (var30 != null
            && var30 >= 2
            && var30 <= 3
            && var10 != null
            && var11 != null
            && var12 != null
            && var13 != null
            && var14 != null
            && var10 == a(var3.size(), var30)
            && var11 + var14 == var10
            && var12 + var13 == var11) {
            ArrayList<java.util.List> var15 = new ArrayList(var8.size());

            for (Object var17 : var8) {
               if (!(
                  var17 instanceof Map var18
                     && h.containsAll(var18.keySet())
                     && var18.keySet().containsAll(Set.of("id", "steps", "planned", "disposition", "checked", "credited", "applied"))
                     && var18.get("id") instanceof String
                     && var18.get("steps") instanceof List var19
                     && b(var18.get("planned")) != null
                     && b(var18.get("checked")) != null
                     && b(var18.get("applied")) != null
                     && var18.get("disposition") instanceof Map
                     && var18.get("credited") instanceof List var20
                     && (var18.get("confirmed") == null || var18.get("confirmed") instanceof Boolean)
               )) {
                  return false;
               }

               for (Object var22 : var20) {
                  Integer var23 = b(var22);
                  if (var23 == null || var23 < 0 || var23 >= var19.size()) {
                     return false;
                  }
               }

               var15.add(var19);
            }

            return io.karatelabs.ext.rules.twin.CanonicalJson.a(io.karatelabs.ext.rules.twin.CanonicalJson.c(var15)).equals(var4.get("deckSha"));
         } else {
            return false;
         }
      } else {
         return false;
      }
   }

   private static int a(int var0, int var1) {
      long var2 = 1L;

      for (int var4 = 0; var4 < var1; var4++) {
         var2 *= var0;
      }

      return var2 > 2147483647L ? -1 : (int)var2;
   }

   private static Integer b(Object var0) {
      return var0 instanceof Number var1 && var1.doubleValue() == Math.rint(var1.doubleValue()) && !(Math.abs(var1.doubleValue()) > 2.147483647E9)
         ? var1.intValue()
         : null;
   }

   public static class OrdersRecordReadException extends RuntimeException {
      OrdersRecordReadException(Path var1, String var2) {
         super("the orders record at " + var1 + " cannot be read (" + var2 + ")");
      }
   }

   public record OrdersRecord(Integer a, Map<String, Object> b) {

      public OrdersRecord(Integer a, Map<String, Object> b) {
         this.a = a;
         this.b = b;
      }
   }
}
