package io.karatelabs.ext.rules;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

public final class TwinRuleRuntime implements io.karatelabs.ext.rules.twin.RulebookRuntime {
   private final RulebookRepository a;
   private final String b;
   private final RuleEvaluator c;
   private final Map<String, io.karatelabs.ext.rules.RulebookRepository.RulebookSnapshot> d = new LinkedHashMap<>();
   private final Map<String, long[]> e = new LinkedHashMap<>();

   public TwinRuleRuntime(RulebookRepository var1, String var2) {
      this(var1, var2, null);
   }

   public TwinRuleRuntime(RulebookRepository var1, String var2, Long var3) {
      this.a = var1;
      this.b = var2;
      this.d.put(var2, var1.d(var2));
      this.c = var3 == null ? new RuleEvaluator() : new RuleEvaluator(var3);
   }

   @Override
   public io.karatelabs.ext.rules.twin.RulebookRuntime.RulebookInvocation a(String var1, Object var2) {
      String var3 = var1 == null ? this.b : var1;
      io.karatelabs.ext.rules.RulebookRepository.RulebookSnapshot var4 = this.d.computeIfAbsent(var3, this.a::d);
      long[] var5 = this.e.computeIfAbsent(var3, var0 -> new long[3]);
      var5[0]++;
      long var6 = System.nanoTime();

      io.karatelabs.ext.rules.twin.RulebookRuntime.RulebookInvocation var8;
      try {
         var8 = this.c
            .a(
               var4.a(),
               var4.c().e(),
               io.karatelabs.ext.rules.twin.CanonicalJson.d(var2),
               null,
               var2x -> new io.karatelabs.ext.rules.twin.RulebookRuntime.RulebookInvocation(var3, var4.d().c(), var2x.h() ? b(var3, var2x.g()) : null, b(var3, var2x.b()))
            );
      } catch (RuntimeException var13) {
         var5[1]++;
         io.karatelabs.ext.rules.twin.SnapshotException var9 = a(var13);
         throw var9 == null ? var13 : var9;
      } finally {
         var5[2] += System.nanoTime() - var6;
      }

      return var8;
   }

   @Override
   public Map<String, io.karatelabs.ext.rules.twin.RulebookRuntime.RulebookUsageStats> a() {
      LinkedHashMap<java.lang.String, io.karatelabs.ext.rules.twin.RulebookRuntime.RulebookUsageStats> var1 = new LinkedHashMap();

      for (Entry var3 : this.e.entrySet()) {
         long[] var4 = (long[])var3.getValue();
         var1.put(
            (String)var3.getKey(), new io.karatelabs.ext.rules.twin.RulebookRuntime.RulebookUsageStats(this.d.get(var3.getKey()).d().c(), (int)var4[0], (int)var4[1], var4[2] / 1000000L)
         );
      }

      return var1;
   }

   @Override
   public void close() {
      this.c.close();
   }

   private static Object b(String var0, Object var1) {
      try {
         io.karatelabs.ext.rules.twin.CanonicalJson.b(var1);
      } catch (io.karatelabs.ext.rules.twin.SnapshotException var3) {
         throw new io.karatelabs.ext.rules.twin.SnapshotException(var3.a, var3.b, var0);
      }

      return io.karatelabs.ext.rules.twin.CanonicalJson.d(var1);
   }

   private static io.karatelabs.ext.rules.twin.SnapshotException a(Throwable var0) {
      Throwable var1 = var0;

      for (int var2 = 0; var1 != null && var2 < 10; var2++) {
         if (var1 instanceof io.karatelabs.ext.rules.twin.SnapshotException var3) {
            return var3;
         }

         var1 = var1.getCause() == var1 ? null : var1.getCause();
      }

      return null;
   }
}
