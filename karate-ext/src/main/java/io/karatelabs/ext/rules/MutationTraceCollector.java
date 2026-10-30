package io.karatelabs.ext.rules;

import io.karatelabs.js.ContextListener;
import io.karatelabs.js.Engine;
import io.karatelabs.js.Event;
import io.karatelabs.js.EventType;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;

public final class MutationTraceCollector implements ContextListener {
   private final Map<Integer, Set<Boolean>> a = new ConcurrentHashMap<>();
   private final Map<Integer, Set<String>> b = new ConcurrentHashMap<>();
   private final Set<String> c = ConcurrentHashMap.newKeySet();
   private final Set<Integer> d = ConcurrentHashMap.newKeySet();
   private final ThreadLocal<String> e = new ThreadLocal<>();

   public static MutationTraceCollector a(Engine var0) {
      MutationTraceCollector var1 = new MutationTraceCollector();
      var0.setListener(var1);
      return var1;
   }

   @Override
   public void onEvent(Event var1) {
      if (var1.type == EventType.COMPARE && var1.value instanceof Object[] var2 && var2.length == 3) {
         String var8 = a(var2[2]);
         this.e.set(var8);
         this.c.add(var8);
         this.d.add(var1.node.get(1).getFirstToken().pos);
      } else if (var1.type == EventType.BRANCH && var1.value instanceof Boolean var3) {
         int var7 = var1.node.getFirstToken().pos;
         this.a.computeIfAbsent(var7, var0 -> ConcurrentHashMap.newKeySet()).add(var3);
         String var5 = this.e.get();
         if (var5 != null) {
            this.b.computeIfAbsent(var7, var0 -> ConcurrentHashMap.newKeySet()).add(var3 + "@" + var5);
         }
      }
   }

   public io.karatelabs.ext.rules.MutationTraceCollector.MutationTraceSnapshot a(String var1) {
      var var2 = RuleInputSpaceExplorer.b(var1);
      TreeMap<java.lang.String, java.util.List<java.lang.String>> var3 = new TreeMap();
      this.a.forEach((var2x, var3x) -> {
         String var4x = (String)var2.get(var2x);
         if (var4x != null) {
            var3.put(var4x, var3x.stream().sorted().map(String::valueOf).toList());
         }
      });
      TreeSet<java.lang.String> var4 = new TreeSet();
      this.b.forEach((var2x, var3x) -> {
         String var4x = (String)var2.get(var2x);
         if (var4x != null) {
            var3x.forEach(var2xx -> var4.add(var4x + ":" + var2xx));
         }
      });
      return new io.karatelabs.ext.rules.MutationTraceCollector.MutationTraceSnapshot(new LinkedHashMap<>(var3), new TreeSet<>(this.c), var4, new TreeSet<>(this.d));
   }

   public static String a(Object var0) {
      if (!(var0 instanceof Number var1)) {
         return String.valueOf(var0);
      } else {
         double var2 = var1.doubleValue();
         return var2 == Math.floor(var2) && Math.abs(var2) < 1.0E15 ? String.valueOf((long)var2) : String.valueOf(var2);
      }
   }

   public static boolean a(io.karatelabs.ext.rules.MutationGenerator.Mutant var0, List<io.karatelabs.ext.rules.MutationTraceCollector.MutationTraceSnapshot> var1) {
      if (var1 == null || var1.isEmpty()) {
         return false;
      } else {
         return !"NEGATE_CONDITIONALS".equals(var0.e()) && !"CONDITIONALS_BOUNDARY".equals(var0.e())
            ? a(var0.l(), var1)
            : var1.stream().anyMatch(var1x -> var1x.d().contains(var0.h()));
      }
   }

   public static boolean a(io.karatelabs.ext.rules.MutationGenerator.MutationWitness var0, List<io.karatelabs.ext.rules.MutationTraceCollector.MutationTraceSnapshot> var1) {
      if (var1 != null && !var1.isEmpty()) {
         String var2 = var0.b();

         return switch (var2) {
            case "operation-called" -> true;
            case "condition-evaluated" -> var1.stream().anyMatch(var1x -> var1x.a().containsKey(var0.c()));
            case "arm-entered" -> var1.stream().anyMatch(var1x -> {
               var var2x = var1x.a().get(var0.c());
               return var2x != null && var2x.contains(String.valueOf(var0.d()));
            });
            case "compare-operand" -> var1.stream().anyMatch(var1x -> var1x.b().contains(var0.f()));
            case "arm-operand" -> var1.stream().anyMatch(var1x -> var1x.c().contains(var0.c() + ":" + var0.d() + "@" + var0.f()));
            default -> false;
         };
      } else {
         return false;
      }
   }

   public record MutationTraceSnapshot(Map<String, List<String>> a, Set<String> b, Set<String> c, Set<Integer> d) {

      public MutationTraceSnapshot(Map<String, List<String>> a, Set<String> b, Set<String> c, Set<Integer> d) {
         this.a = a;
         this.b = b;
         this.c = c;
         this.d = d;
      }
   }
}
