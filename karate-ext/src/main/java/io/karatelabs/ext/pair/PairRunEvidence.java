package io.karatelabs.ext.pair;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public record PairRunEvidence(String component1, String c, Path d, List<io.karatelabs.ext.pair.PairRunEvidence.ScenarioRunEvidence> e, String f) {
   public static final String PROVIDER = "provider";
   public static final String MOCK = "mock";


   public static PairRunEvidence failedLeg(String var0, String var1, Path var2, String var3) {
      return new PairRunEvidence(var0, var1, var2, List.of(), var3);
   }

   public Map<String, io.karatelabs.ext.pair.PairRunEvidence.ScenarioRunEvidence> operationsById() {
      LinkedHashMap<String, io.karatelabs.ext.pair.PairRunEvidence.ScenarioRunEvidence> var1 = new LinkedHashMap<>();
      LinkedHashMap<java.lang.String, java.lang.Integer> var2 = new LinkedHashMap();

      for (io.karatelabs.ext.pair.PairRunEvidence.ScenarioRunEvidence var4 : this.e) {
         int var5 = var2.merge(var4.e(), 1, Integer::sum);
         var1.put(var5 == 1 ? var4.e() : var4.e() + "#" + var5, var4);
      }

      return var1;
   }

   public String b() {
      return this.component1;
   }

   public String c() {
      return this.c;
   }

   public Path d() {
      return this.d;
   }

   public List<io.karatelabs.ext.pair.PairRunEvidence.ScenarioRunEvidence> e() {
      return this.e;
   }

   public String f() {
      return this.f;
   }

   public record RecordedHttpExchange(int a, boolean b, String c, String d, String e, int f, Map<String, Object> g, Object h, long i) {

   }

   public record ScenarioRunEvidence(String e, String f, int g, String h, Set<String> i, String j, String k, List<io.karatelabs.ext.pair.PairRunEvidence.ScenarioStepEvidence> l, List<io.karatelabs.ext.pair.PairRunEvidence.RecordedHttpExchange> m) {


      public boolean isSkipped() {
         return "skipped".equals(this.j);
      }

      public boolean isPassed() {
         return "passed".equals(this.j);
      }

      public List<io.karatelabs.ext.pair.PairRunEvidence.RecordedHttpExchange> passedObservations() {
         return this.m.stream().filter(io.karatelabs.ext.pair.PairRunEvidence.RecordedHttpExchange::b).toList();
      }

      public List<io.karatelabs.ext.pair.PairRunEvidence.RecordedHttpExchange> failedObservations() {
         return this.m.stream().filter(var0 -> !var0.b()).toList();
      }

      public String e() {
         return this.e;
      }

      public String f() {
         return this.f;
      }

      public int g() {
         return this.g;
      }

      public String h() {
         return this.h;
      }

      public Set<String> i() {
         return this.i;
      }

      public String j() {
         return this.j;
      }

      public String k() {
         return this.k;
      }

      public List<io.karatelabs.ext.pair.PairRunEvidence.ScenarioStepEvidence> l() {
         return this.l;
      }

      public List<io.karatelabs.ext.pair.PairRunEvidence.RecordedHttpExchange> m() {
         return this.m;
      }
   }

   public record ScenarioStepEvidence(int c, String d, String e, boolean f, String g) {


      public boolean isPassed() {
         return "passed".equals(this.e);
      }

      public boolean isFailed() {
         return "failed".equals(this.e);
      }

      public int c() {
         return this.c;
      }

      public String d() {
         return this.d;
      }

      public String e() {
         return this.e;
      }

      public boolean f() {
         return this.f;
      }

      public String g() {
         return this.g;
      }
   }
}
