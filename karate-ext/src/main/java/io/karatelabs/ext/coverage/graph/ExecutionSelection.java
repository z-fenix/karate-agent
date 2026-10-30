package io.karatelabs.ext.coverage.graph;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

public final class ExecutionSelection {
   public static final String a = "latest/v1";
   public static final String b = "unattributed";
   private static final Comparator<io.karatelabs.ext.coverage.collect.TestExecution> c = Comparator.<io.karatelabs.ext.coverage.collect.TestExecution, Long>comparing(
         ExecutionSelection::a, Comparator.nullsFirst(Comparator.naturalOrder())
      )
      .thenComparingInt(io.karatelabs.ext.coverage.collect.TestExecution::e)
      .thenComparing(io.karatelabs.ext.coverage.collect.TestExecution::g);

   private ExecutionSelection() {
   }

   private static Long a(io.karatelabs.ext.coverage.collect.TestExecution var0) {
      return var0.k() != null ? var0.k() : var0.m();
   }

   static io.karatelabs.ext.coverage.graph.ExecutionSelection.SelectionResult a(Collection<io.karatelabs.ext.coverage.collect.TestExecution> var0, io.karatelabs.ext.coverage.graph.ExecutionSelection.SelectionOptions var1, io.karatelabs.ext.coverage.collect.ExecutionInventory var2, Map<String, Object> var3) {
      LinkedHashMap var4 = new LinkedHashMap();
      List<io.karatelabs.ext.coverage.collect.TestExecution> var5 = a(var0, var4);
      LinkedHashSet<java.lang.String> var6 = new LinkedHashSet();
      if (!var1.h().isEmpty()) {
         var var7 = Set.copyOf(var1.h());
         ArrayList<io.karatelabs.ext.coverage.collect.TestExecution> var8 = new ArrayList();

         for (io.karatelabs.ext.coverage.collect.TestExecution var10 : (Iterable<io.karatelabs.ext.coverage.collect.TestExecution>) (Iterable<?>) (var5)) {
            String var11 = b(var10);
            if (var11 != null && var7.contains(var11)) {
               var8.add(var10);
            } else {
               var6.add(var10.r());
            }
         }

         var5 = var8;
      }

      LinkedHashSet<java.lang.String> var33 = new LinkedHashSet();
      LinkedHashSet<java.lang.String> var34 = new LinkedHashSet();
      LinkedHashSet<java.lang.String> var35 = new LinkedHashSet();
      boolean var36 = "project".equals(var1.b());
      ArrayList<io.karatelabs.ext.coverage.collect.TestExecution> var37 = new ArrayList();

      for (io.karatelabs.ext.coverage.collect.TestExecution var13 : (Iterable<io.karatelabs.ext.coverage.collect.TestExecution>) (Iterable<?>) (var5)) {
         switch (var2.a(var13)) {
            case RETIRED:
               var33.add(var13.g());
               var34.add(var13.r());
               if (var36) {
                  continue;
               }
               break;
            case UNKNOWN:
               var35.add(var13.r());
            case PRESENT:
         }

         var37.add(var13);
      }

      LinkedHashMap<java.lang.String, java.util.ArrayList> var38 = new LinkedHashMap();
      ArrayList<java.lang.String> var39 = new ArrayList();
      int var14 = 0;

      for (io.karatelabs.ext.coverage.collect.TestExecution var16 : (Iterable<io.karatelabs.ext.coverage.collect.TestExecution>) (Iterable<?>) (var37)) {
         String var17 = var16.a() ? "\u0000" + var16.g() : var16.h() + "\u0000" + (var16.i() == null ? "" : var16.i());
         var38.computeIfAbsent(var17, var0x -> new ArrayList<>()).add(var16);
         if (var16.k() == null && var16.m() != null) {
            var39.add(var16.g());
         }

         if (var16.a("unattributed")) {
            var14++;
         }
      }

      LinkedHashMap<String, Set<String>> var40 = new LinkedHashMap<>();

      for (io.karatelabs.ext.coverage.collect.TestExecution var43 : (Iterable<io.karatelabs.ext.coverage.collect.TestExecution>) (Iterable<?>) (var37)) {
         if (var43.i() != null) {
            var40.computeIfAbsent(var43.h(), var0x -> new LinkedHashSet<>()).add(var43.i());
         }
      }

      ArrayList<java.lang.String> var42 = new ArrayList();

      for (List var18 : (Iterable<List>) (Iterable<?>) (var38.values())) {
         io.karatelabs.ext.coverage.collect.TestExecution var19 = (io.karatelabs.ext.coverage.collect.TestExecution)var18.get(0);
         if (var19.i() != null && var19.i().matches("\\d+") && var40.get(var19.h()).stream().anyMatch(var0x -> var0x.contains("/"))) {
            var42.add(var19.r());
         }
      }

      LinkedHashMap<java.lang.String, io.karatelabs.ext.coverage.graph.ExecutionSelection.SelectedExecution> var45 = new LinkedHashMap();
      LinkedHashSet<java.lang.String> var46 = new LinkedHashSet();
      LinkedHashMap<java.lang.String, java.lang.String> var47 = new LinkedHashMap();
      ArrayList<java.lang.String> var20 = new ArrayList();
      ArrayList<java.lang.String> var21 = new ArrayList();
      ArrayList<java.lang.String> var22 = new ArrayList();
      ArrayList<java.lang.String> var23 = new ArrayList();

      for (List var25 : (Iterable<List>) (Iterable<?>) (var38.values())) {
         ArrayList var26 = new ArrayList(var25);
         var26.sort(c.reversed());
         io.karatelabs.ext.coverage.collect.TestExecution var27 = null;
         boolean var28 = false;
         boolean var29 = false;
         io.karatelabs.ext.coverage.collect.TestExecution var30 = null;

         for (io.karatelabs.ext.coverage.collect.TestExecution var32 : (Iterable<io.karatelabs.ext.coverage.collect.TestExecution>) (Iterable<?>) (var26)) {
            if (!var32.b()) {
               var21.add(var32.g());
               var28 = true;
            } else if (var27 == null) {
               var27 = var32;
            } else {
               if (var30 == null) {
                  var30 = var32;
               }

               var47.put(var32.g(), var27.g());
               var29 |= var32.o() == io.karatelabs.ext.coverage.collect.TestExecution.ExecutionOutcome.FAILED;
            }
         }

         String var56 = ((io.karatelabs.ext.coverage.collect.TestExecution)var26.get(0)).r();
         if (var27 == null) {
            var22.add(var56);
         } else {
            var46.add(var27.g());
            boolean var57 = var27.o() == io.karatelabs.ext.coverage.collect.TestExecution.ExecutionOutcome.PASSED && var29;
            var45.put(var27.r(), new io.karatelabs.ext.coverage.graph.ExecutionSelection.SelectedExecution(var27.r(), var27, var57, var28, var33.contains(var27.g()), var35.contains(var27.r())));
            if (var57) {
               var20.add(var27.r());
            }

            if (var30 != null && a(var27, var30) && var27.e() == var30.e()) {
               var23.add(var27.r());
            }
         }
      }

      LinkedHashMap<java.lang.String, java.lang.Object> var48 = new LinkedHashMap();
      var48.put("scope", var1.b());
      if (!var1.c().isEmpty()) {
         var48.put("runs", var1.c());
      }

      var48.put("policy", "latest/v1");
      var48.put("executions", var5.size());
      var48.put("selected", var46.size());
      var48.put("superseded", var47.size());
      var48.put("previouslyFailed", var20);
      var48.put("pending", var21);
      var48.put("noTerminal", var22);
      var48.put("excluded", var1.d());
      if (var1.e() != null) {
         var48.put("formalPolicy", var1.e());
      }

      var48.put("informalOmitted", var1.f());
      var48.put("retired", new ArrayList(var34));
      var48.put("unknown", new ArrayList(var35));
      var48.put("inventory", var2.e());
      var48.put("startFallback", var39);
      var48.put("ambiguousChronology", var23);
      var48.put("legacyCaseKey", var42);
      var48.put("dryRunsExcluded", var1.g());
      if (!var1.h().isEmpty()) {
         var48.put("definitions", var1.h());
         var48.put("outsideDefinitions", new ArrayList(var6));
      }

      var48.put("unattributed", var14 + a(var3.get("unattributed")));

      for (String var52 : List.of("allocatedAtExit", "unmanifested", "finalizationInferred", "incompleteEvidence")) {
         var48.put(var52, var3.getOrDefault(var52, List.of()));
      }

      var48.put("duplicateInputs", a(var3.get("duplicateInputs")));
      var48.put("producerRefused", var3.getOrDefault("producerRefused", List.of()));

      for (Entry var53 : var3.entrySet()) {
         var48.putIfAbsent((String)var53.getKey(), var53.getValue());
      }

      LinkedHashMap<java.lang.String, io.karatelabs.ext.coverage.collect.TestExecution> var51 = new LinkedHashMap();

      for (io.karatelabs.ext.coverage.collect.TestExecution var55 : (Iterable<io.karatelabs.ext.coverage.collect.TestExecution>) (Iterable<?>) (var5)) {
         var51.put(var55.g(), var55);
      }

      return new io.karatelabs.ext.coverage.graph.ExecutionSelection.SelectionResult(var45, var46, var47, var33, var48, var51, var4);
   }

   private static List<io.karatelabs.ext.coverage.collect.TestExecution> a(Collection<io.karatelabs.ext.coverage.collect.TestExecution> var0, Map<String, String> var1) {
      LinkedHashMap<java.lang.String, java.util.Set> var2 = new LinkedHashMap();

      for (io.karatelabs.ext.coverage.collect.TestExecution var4 : var0) {
         Object var5 = var4.C().get("deck");
         if (var5 != null) {
            var2.computeIfAbsent(var4.h(), var0x -> new LinkedHashSet<>()).add(String.valueOf(var5));
         }
      }

      ArrayList<io.karatelabs.ext.coverage.collect.TestExecution> var10 = new ArrayList(var0.size());

      for (io.karatelabs.ext.coverage.collect.TestExecution var12 : var0) {
         Object var6 = var12.C().get("exampleId");
         Set var7 = var12.i() != null && var12.i().matches("\\d+") && var6 != null ? (Set)var2.get(var12.h()) : null;
         if (var7 != null && var7.size() == 1) {
            String var8 = (String)var7.iterator().next() + "/" + var6;
            String var9 = var12.h() + ":" + var8;
            var1.put(var12.g(), var9);
            var10.add(var12.b(var8, var9));
         } else {
            var10.add(var12);
         }
      }

      return var10;
   }

   private static String b(io.karatelabs.ext.coverage.collect.TestExecution var0) {
      if (var0.a()) {
         return null;
      } else if (var0.h().startsWith("check:")) {
         return var0.h().substring("check:".length());
      } else {
         String var1 = var0.t() == null ? null : var0.t().replace('\\', '/');
         return var1 != null && var1.startsWith("classpath:") ? var1.substring("classpath:".length()) : var1;
      }
   }

   private static boolean a(io.karatelabs.ext.coverage.collect.TestExecution var0, io.karatelabs.ext.coverage.collect.TestExecution var1) {
      Long var2 = a(var0);
      Long var3 = a(var1);
      return var2 == null ? var3 == null : var2.equals(var3);
   }

   private static long a(Object var0) {
      return var0 instanceof Number var1 ? var1.longValue() : 0L;
   }

   public record SelectionResult(Map<String, io.karatelabs.ext.coverage.graph.ExecutionSelection.SelectedExecution> a, Set<String> b, Map<String, String> c, Set<String> d, Map<String, Object> e, Map<String, io.karatelabs.ext.coverage.collect.TestExecution> f, Map<String, String> g) {

   }

   public record SelectedExecution(String a, io.karatelabs.ext.coverage.collect.TestExecution b, boolean c, boolean d, boolean e, boolean f) {

   }

   public record SelectionOptions(String b, List<String> c, List<Map<String, Object>> d, String e, List<String> f, int g, List<String> h) {


      public static io.karatelabs.ext.coverage.graph.ExecutionSelection.SelectionOptions a(String var0) {
         return new io.karatelabs.ext.coverage.graph.ExecutionSelection.SelectionOptions(var0, List.of(), List.of(), null, List.of(), 0, List.of());
      }

      public static io.karatelabs.ext.coverage.graph.ExecutionSelection.SelectionOptions a() {
         return a("project");
      }

      public static io.karatelabs.ext.coverage.graph.ExecutionSelection.SelectionOptions a(List<String> var0) {
         return new io.karatelabs.ext.coverage.graph.ExecutionSelection.SelectionOptions("run", List.copyOf(var0), List.of(), null, List.of(), 0, List.of());
      }

      public io.karatelabs.ext.coverage.graph.ExecutionSelection.SelectionOptions b(List<Map<String, Object>> var1) {
         return new io.karatelabs.ext.coverage.graph.ExecutionSelection.SelectionOptions(this.b, this.c, List.copyOf(var1), this.e, this.f, this.g, this.h);
      }

      public io.karatelabs.ext.coverage.graph.ExecutionSelection.SelectionOptions a(String var1, List<String> var2) {
         return new io.karatelabs.ext.coverage.graph.ExecutionSelection.SelectionOptions(this.b, this.c, this.d, var1, List.copyOf(var2), this.g, this.h);
      }

      public io.karatelabs.ext.coverage.graph.ExecutionSelection.SelectionOptions a(int var1) {
         return new io.karatelabs.ext.coverage.graph.ExecutionSelection.SelectionOptions(this.b, this.c, this.d, this.e, this.f, var1, this.h);
      }

      public io.karatelabs.ext.coverage.graph.ExecutionSelection.SelectionOptions c(List<String> var1) {
         return new io.karatelabs.ext.coverage.graph.ExecutionSelection.SelectionOptions(this.b, this.c, this.d, this.e, this.f, this.g, List.copyOf(var1));
      }

      public String b() {
         return this.b;
      }

      public List<String> c() {
         return this.c;
      }

      public List<Map<String, Object>> d() {
         return this.d;
      }

      public String e() {
         return this.e;
      }

      public List<String> f() {
         return this.f;
      }

      public int g() {
         return this.g;
      }

      public List<String> h() {
         return this.h;
      }
   }
}
