package io.karatelabs.ext.rules.twin;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class TransitionPairIndex {
   public static final String a = "sequencePairOutsideWalk";
   public static final String b = "transitionPairFloor";
   private final Map<io.karatelabs.ext.rules.twin.TransitionPairIndex.TransitionPairKey, io.karatelabs.ext.rules.twin.TransitionPairIndex.TransitionPairWitness> c;

   private TransitionPairIndex(Map<io.karatelabs.ext.rules.twin.TransitionPairIndex.TransitionPairKey, io.karatelabs.ext.rules.twin.TransitionPairIndex.TransitionPairWitness> var1) {
      this.c = var1;
   }

   public static TransitionPairIndex a(TwinWalkResult var0) {
      LinkedHashMap<Integer, List<io.karatelabs.ext.rules.twin.TwinWalkResult.WalkTransition>> var1 = new LinkedHashMap<>();

      for (io.karatelabs.ext.rules.twin.TwinWalkResult.WalkTransition var3 : var0.f()) {
         if (var3.d() != null) {
            var1.computeIfAbsent(var3.a(), var0x -> new ArrayList<>()).add(var3);
         }
      }

      LinkedHashMap<io.karatelabs.ext.rules.twin.TransitionPairIndex.TransitionPairKey, io.karatelabs.ext.rules.twin.TransitionPairIndex.TransitionPairWitness> var9 = new LinkedHashMap();

      for (io.karatelabs.ext.rules.twin.TwinWalkResult.WalkTransition var4 : var0.f()) {
         if (var4.d() != null) {
            String var5 = var0.e().get(var4.a()).b();
            String var6 = var0.e().get(var4.d()).b();

            for (io.karatelabs.ext.rules.twin.TwinWalkResult.WalkTransition var8 : (Iterable<io.karatelabs.ext.rules.twin.TwinWalkResult.WalkTransition>) (Iterable<?>) (var1.getOrDefault(var4.d(), List.of()))) {
               var9.putIfAbsent(new io.karatelabs.ext.rules.twin.TransitionPairIndex.TransitionPairKey(var5, var4.b(), var6, var8.b()), new io.karatelabs.ext.rules.twin.TransitionPairIndex.TransitionPairWitness(var4.a(), var4.c(), var8.c()));
            }
         }
      }

      return new TransitionPairIndex(var9);
   }

   public int a() {
      return this.c.size();
   }

   public List<io.karatelabs.ext.rules.twin.TransitionPairIndex.TransitionPairKey> b() {
      return List.copyOf(this.c.keySet());
   }

   public boolean a(io.karatelabs.ext.rules.twin.TransitionPairIndex.TransitionPairKey var1) {
      return this.c.containsKey(var1);
   }

   public List<Map<String, Object>> a(io.karatelabs.ext.rules.twin.TwinWalker.ReverseTransitionPath var1, Set<io.karatelabs.ext.rules.twin.TransitionPairIndex.TransitionPairKey> var2) {
      ArrayList<java.util.Map<java.lang.String,java.lang.Object>> var3 = new ArrayList();

      for (io.karatelabs.ext.rules.twin.TransitionPairIndex.TransitionPairKey var5 : this.c.keySet()) {
         if (!var2.contains(var5)) {
            LinkedHashMap<java.lang.String, java.util.List<java.util.Map<java.lang.String,java.lang.Object>>> var6 = new LinkedHashMap();
            var6.put("steps", this.a(var1, var5));
            var var7 = b(var5);
            var7.put("candidate", var6);
            var3.add(var7);
         }
      }

      return var3;
   }

   public List<Map<String, Object>> a(io.karatelabs.ext.rules.twin.TwinWalker.ReverseTransitionPath var1, io.karatelabs.ext.rules.twin.TransitionPairIndex.TransitionPairKey var2) {
      io.karatelabs.ext.rules.twin.TransitionPairIndex.TransitionPairWitness var3 = this.c.get(var2);
      var var4 = var1.a(var3.a());
      var4.add(TwinWalker.a(var2.b(), var3.b()));
      var4.add(TwinWalker.a(var2.d(), var3.c()));
      return var4;
   }

   public static void a(TwinModel var0, String var1, ReplayTrace var2, List<Map<String, Object>> var3) {
      var var4 = var2.a();
      ArrayList<java.lang.String> var5 = new ArrayList(var4.size());
      String var6 = io.karatelabs.ext.rules.twin.CanonicalJson.c(TwinWalker.a(var0));

      for (io.karatelabs.ext.rules.twin.ReplayTrace.ReplayStep var8 : (Iterable<io.karatelabs.ext.rules.twin.ReplayTrace.ReplayStep>) (Iterable<?>) (var4)) {
         var5.add(var6);
         var6 = io.karatelabs.ext.rules.twin.CanonicalJson.c(var8.i());
      }

      for (int var11 = 0; var11 + 1 < var4.size(); var11++) {
         io.karatelabs.ext.rules.twin.ReplayTrace.ReplayStep var12 = (io.karatelabs.ext.rules.twin.ReplayTrace.ReplayStep)var4.get(var11);
         io.karatelabs.ext.rules.twin.ReplayTrace.ReplayStep var9 = (io.karatelabs.ext.rules.twin.ReplayTrace.ReplayStep)var4.get(var11 + 1);
         if ("applied".equals(var12.c()) && "applied".equals(var9.c())) {
            LinkedHashMap<java.lang.String, java.lang.Object> var10 = new LinkedHashMap();
            var10.put("sequenceId", var1);
            var10.put("at", var11);
            var10.put("fromKey", var5.get(var11));
            var10.put("command1", var12.a());
            var10.put("midKey", var5.get(var11 + 1));
            var10.put("command2", var9.a());
            var3.add(var10);
         }
      }
   }

   public static io.karatelabs.ext.rules.twin.TransitionPairIndex.TransitionPairKey a(Map<String, Object> var0) {
      return new io.karatelabs.ext.rules.twin.TransitionPairIndex.TransitionPairKey(a(var0.get("fromKey")), a(var0.get("command1")), a(var0.get("midKey")), a(var0.get("command2")));
   }

   public static Map<String, Object> b(Map<String, Object> var0) {
      LinkedHashMap<java.lang.String, java.lang.Object> var1 = new LinkedHashMap();
      var1.put("kind", "sequencePairOutsideWalk");
      var1.put("sequenceId", var0.get("sequenceId"));
      var1.put("at", var0.get("at"));
      var1.putAll(b(a(var0)));
      return var1;
   }

   public static Map<String, Object> b(TwinWalkResult var0) {
      int var1 = var0.i() == null ? 0 : var0.i().a();
      boolean var2 = var0.j() != null && !"exhausted".equals(var0.j().a());
      if (!var2 && var1 == 0) {
         return null;
      } else {
         LinkedHashMap<java.lang.String, java.lang.Object> var3 = new LinkedHashMap();
         var3.put("kind", "transitionPairFloor");
         var3.put("depthPruned", var1);
         return var3;
      }
   }

   private static Map<String, Object> b(io.karatelabs.ext.rules.twin.TransitionPairIndex.TransitionPairKey var0) {
      LinkedHashMap<java.lang.String, java.lang.Object> var1 = new LinkedHashMap();
      var1.put("fromKey", var0.a());
      var1.put("command1", var0.b());
      var1.put("midKey", var0.c());
      var1.put("command2", var0.d());
      return var1;
   }

   private static String a(Object var0) {
      return var0 == null ? null : String.valueOf(var0);
   }

   private record TransitionPairWitness(int a, Map<String, Object> b, Map<String, Object> c) {

      private TransitionPairWitness(int a, Map<String, Object> b, Map<String, Object> c) {
         this.a = a;
         this.b = b;
         this.c = c;
      }
   }

   public record TransitionPairKey(String a, String b, String c, String d) {

      public TransitionPairKey(String a, String b, String c, String d) {
         this.a = a;
         this.b = b;
         this.c = c;
         this.d = d;
      }
   }
}
