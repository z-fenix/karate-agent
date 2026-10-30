package io.karatelabs.ext.rules.twin;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class SequenceExplorer {
   static final int a = 64;
   static final int b = 200000;
   static final int c = 160;
   static final int d = 1;
   static final String e = "exhausted";
   static final String f = "stagnant";
   static final String g = "UNSTABLE";

   private SequenceExplorer() {
   }

   static List<Map<String, Object>> a(TwinModel var0, TwinWalkPlan var1) {
      LinkedHashMap var2 = new LinkedHashMap();

      for (io.karatelabs.ext.rules.twin.TwinModel.CommandModel var4 : var0.c()) {
         for (Map var6 : TwinWalker.a(var4, var1)) {
            var2.putIfAbsent(a(var4.a(), var6), TwinWalker.a(var4.a(), var6));
         }
      }

      return new ArrayList<>(var2.values());
   }

   static String a(String var0, Map<String, Object> var1) {
      return var0 + "|" + io.karatelabs.ext.rules.twin.CanonicalJson.c(var1);
   }

   static io.karatelabs.ext.rules.twin.SequenceExplorer.SequenceExplorationResult a(TwinModel var0, TwinWalkPlan var1, int var2, RulebookRuntime var3) {
      return a(var0, var1, var2, var3, 200000, 64);
   }

   static io.karatelabs.ext.rules.twin.SequenceExplorer.SequenceExplorationResult a(TwinModel var0, TwinWalkPlan var1, int var2, RulebookRuntime var3, int var4, int var5) {
      return new io.karatelabs.ext.rules.twin.SequenceExplorer.SequenceSearchEngine(var0, var1, var2, var3, var4, var5).a();
   }

   static int a(io.karatelabs.ext.rules.twin.SequenceExplorer.CoverageLedger var0, List<String> var1, List<Map<String, Object>> var2, List<Integer> var3) {
      var0.a();
      int var4 = 0;

      for (int var6 : var3) {
         Map var7 = (Map)var2.get(var6);
         var4 += var0.b(var1.indexOf(a(String.valueOf(var7.get("command")), a(var7))));
      }

      return var4;
   }

   private static Map<String, Object> a(Map<String, Object> var0) {
      return var0.get("args") instanceof Map var2 ? b(var2) : Map.of();
   }

   private static Map<String, Object> b(Map<?, ?> var0) {
      return (Map<String, Object>)(Map<?, ?>)var0;
   }

   private static final class SequenceSearchEngine {
      private final TwinModel a;
      private final int b;
      private final RulebookRuntime c;
      private final List<Map<String, Object>> d;
      private final List<String> e = new ArrayList<>();
      private final io.karatelabs.ext.rules.twin.SequenceExplorer.CoverageLedger f;
      private final List<io.karatelabs.ext.rules.twin.SequenceExplorer.GeneratedSequence> g = new ArrayList<>();
      private final int h;
      private final int i;
      private final int j;
      private int k;
      private boolean l;

      SequenceSearchEngine(TwinModel var1, TwinWalkPlan var2, int var3, RulebookRuntime var4, int var5, int var6) {
         this.a = var1;
         this.b = var3;
         this.c = var4;
         this.i = var5;
         this.j = var6;
         this.d = io.karatelabs.ext.rules.twin.SequenceExplorer.a(var1, var2);

         for (Map var8 : this.d) {
            this.e.add(io.karatelabs.ext.rules.twin.SequenceExplorer.a(String.valueOf(var8.get("command")), io.karatelabs.ext.rules.twin.SequenceExplorer.a(var8)));
         }

         this.f = new io.karatelabs.ext.rules.twin.SequenceExplorer.CoverageLedger(this.d.size(), var3);
         this.h = Math.min(4 * this.d.size(), 160);
      }

      io.karatelabs.ext.rules.twin.SequenceExplorer.SequenceExplorationResult a() {
         while (true) {
            String var1 = this.b();
            if (var1 == null) {
               io.karatelabs.ext.rules.twin.SequenceExplorer.GeneratedSequence var2 = this.a(this.g.size() + 1);
               if (var2.c() != 0) {
                  this.g.add(var2);
                  continue;
               }

               var1 = this.b();
               if (var1 == null) {
                  var1 = "stagnant";
               }
            }

            ArrayList var5 = new ArrayList(this.g.size());

            for (io.karatelabs.ext.rules.twin.SequenceExplorer.GeneratedSequence var4 : this.g) {
               var5.add(var4.b());
            }

            return new io.karatelabs.ext.rules.twin.SequenceExplorer.SequenceExplorationResult(
               List.copyOf(this.e),
               this.c(),
               List.copyOf(this.g),
               this.f.b(),
               io.karatelabs.ext.rules.twin.CanonicalJson.a(io.karatelabs.ext.rules.twin.CanonicalJson.c(this.e)),
               io.karatelabs.ext.rules.twin.CanonicalJson.a(io.karatelabs.ext.rules.twin.CanonicalJson.c(var5)),
               var1,
               this.k
            );
         }
      }

      private String b() {
         if (this.f.b() >= this.c()) {
            return "exhausted";
         } else if (this.g.size() >= this.j) {
            return "sequences";
         } else {
            return !this.l && this.k < this.i ? null : "modelSteps";
         }
      }

      private int c() {
         long var1 = 1L;

         for (int var3 = 0; var3 < this.b; var3++) {
            var1 *= this.d.size();
         }

         return Math.toIntExact(var1);
      }

      private io.karatelabs.ext.rules.twin.SequenceExplorer.GeneratedSequence a(int var1) {
         Object var2 = TwinWalker.a(this.a);
         String var3 = io.karatelabs.ext.rules.twin.CanonicalJson.c(var2);
         ArrayList var4 = new ArrayList();
         int[] var5 = new int[this.d.size()];
         this.f.a();
         int var6 = 0;
         int var7 = 0;

         while (var4.size() < this.h && var7 < this.d.size()) {
            var var8 = this.a(var2, var3);
            if (var8 == null || var8.isEmpty()) {
               break;
            }

            io.karatelabs.ext.rules.twin.SequenceExplorer.SequenceMoveCandidate var9 = a(var8, var5);
            if (var9 == null) {
               break;
            }

            var4.add(this.d.get(var9.a()));
            var5[var9.a()]++;
            var6 += this.f.b(var9.a());
            var2 = io.karatelabs.ext.rules.twin.CanonicalJson.d(var9.b().j());
            var3 = var9.b().k();
            var7 = var9.c() == 0 ? var7 + 1 : 0;
         }

         return new io.karatelabs.ext.rules.twin.SequenceExplorer.GeneratedSequence("o-" + var1, List.copyOf(var4), var6);
      }

      private List<io.karatelabs.ext.rules.twin.SequenceExplorer.SequenceMoveCandidate> a(Object var1, String var2) {
         ArrayList var3 = new ArrayList();

         for (int var4 = 0; var4 < this.d.size(); var4++) {
            var var5 = this.d.get(var4);
            io.karatelabs.ext.rules.twin.TwinModel.CommandModel var6 = this.a.f().get(String.valueOf(var5.get("command")));
            var var7 = io.karatelabs.ext.rules.twin.SequenceExplorer.a(var5);
            if (TwinWalker.a(this.a, var1, var7) && (!var6.c() || CommandExecutor.b(this.a, var1, var6, var7))) {
               if (this.k >= this.i) {
                  this.l = true;
                  return null;
               }

               this.k++;
               io.karatelabs.ext.rules.twin.CommandExecutor.CommandResult var8 = CommandExecutor.a(this.a, var1, var6, var7, this.c);
               if (CommandExecutor.a(var8)) {
                  throw new TwinException(408, "TIMEOUT", "the deck construction consulted a rulebook that timed out (" + var8.e() + ") — nothing was written");
               }

               if (var8.a() && !a(var8)) {
                  var3.add(new io.karatelabs.ext.rules.twin.SequenceExplorer.SequenceMoveCandidate(var4, var8, this.f.a(var4), !var8.k().equals(var2)));
               }
            }
         }

         return var3;
      }

      private static io.karatelabs.ext.rules.twin.SequenceExplorer.SequenceMoveCandidate a(List<io.karatelabs.ext.rules.twin.SequenceExplorer.SequenceMoveCandidate> var0, int[] var1) {
         io.karatelabs.ext.rules.twin.SequenceExplorer.SequenceMoveCandidate var2 = null;

         for (io.karatelabs.ext.rules.twin.SequenceExplorer.SequenceMoveCandidate var4 : var0) {
            if (var2 == null || var4.c() > var2.c()) {
               var2 = var4;
            }
         }

         if (var2.c() > 0) {
            return var2;
         } else {
            io.karatelabs.ext.rules.twin.SequenceExplorer.SequenceMoveCandidate var6 = null;

            for (io.karatelabs.ext.rules.twin.SequenceExplorer.SequenceMoveCandidate var5 : var0) {
               if (var5.d() && (var6 == null || var1[var5.a()] < var1[var6.a()])) {
                  var6 = var5;
               }
            }

            return var6;
         }
      }

      private static boolean a(io.karatelabs.ext.rules.twin.CommandExecutor.CommandResult var0) {
         for (io.karatelabs.ext.rules.twin.CommandExecutor.InvariantEvaluation var2 : var0.p()) {
            if ("failed".equals(var2.b())) {
               return true;
            }
         }

         return false;
      }
   }

   private record SequenceMoveCandidate(int a, io.karatelabs.ext.rules.twin.CommandExecutor.CommandResult b, int c, boolean d) {

      private SequenceMoveCandidate(int a, io.karatelabs.ext.rules.twin.CommandExecutor.CommandResult b, int c, boolean d) {
         this.a = a;
         this.b = b;
         this.c = c;
         this.d = d;
      }
   }

   record SequenceExplorationResult(List<String> a, int b, List<io.karatelabs.ext.rules.twin.SequenceExplorer.GeneratedSequence> c, int d, String e, String f, String g, int h) {

      SequenceExplorationResult(List<String> a, int b, List<io.karatelabs.ext.rules.twin.SequenceExplorer.GeneratedSequence> c, int d, String e, String f, String g, int h) {
         this.a = a;
         this.b = b;
         this.c = c;
         this.d = d;
         this.e = e;
         this.f = f;
         this.g = g;
         this.h = h;
      }
   }

   record GeneratedSequence(String a, List<Map<String, Object>> b, int c) {

      GeneratedSequence(String a, List<Map<String, Object>> b, int c) {
         this.a = a;
         this.b = b;
         this.c = c;
      }
   }

   static final class CoverageLedger {
      private final int a;
      private final int b;
      private final Set<Long> c = new HashSet<>();
      private final List<Set<Long>> d = new ArrayList<>();

      CoverageLedger(int var1, int var2) {
         this.a = var1;
         this.b = var2;

         for (int var3 = 1; var3 < var2; var3++) {
            this.d.add(new LinkedHashSet<>());
         }
      }

      void a() {
         for (Set var2 : this.d) {
            var2.clear();
         }
      }

      int a(int var1) {
         int var2 = 0;

         for (long var4 : this.d.get(this.b - 2)) {
            var2 += this.c.contains(var4 * this.a + var1) ? 0 : 1;
         }

         return var2;
      }

      int b(int var1) {
         int var2 = 0;

         for (long var4 : this.d.get(this.b - 2)) {
            var2 += this.c.add(var4 * this.a + var1) ? 1 : 0;
         }

         for (int var8 = this.b - 1; var8 >= 2; var8--) {
            ArrayList var9 = new ArrayList();

            for (long var6 : this.d.get(var8 - 2)) {
               var9.add(var6 * this.a + var1);
            }

            this.d.get(var8 - 1).addAll(var9);
         }

         this.d.get(0).add((long)var1);
         return var2;
      }

      int b() {
         return this.c.size();
      }
   }
}
