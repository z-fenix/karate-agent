package io.karatelabs.ext.coverage.collect;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

public final class ExecutionFragmentAggregator {
   public static final String a = "fragmentConflict";
   public static final String b = "incompleteEvidence";

   private ExecutionFragmentAggregator() {
   }

   public static TestExecution a(io.karatelabs.ext.coverage.collect.ExecutionFragmentAggregator.ExecutionDescriptor var0, List<ExecutionFragment> var1, boolean var2) {
      ExecutionFragment var3 = a(var1, io.karatelabs.ext.coverage.collect.ExecutionFragment.ExecutionFragmentKind.ORIGINAL);
      ExecutionFragment var4 = var3 != null ? var3 : a(var1, io.karatelabs.ext.coverage.collect.ExecutionFragment.ExecutionFragmentKind.STREAM, io.karatelabs.ext.coverage.collect.ExecutionFragment.ExecutionFragmentKind.CONTAINER, io.karatelabs.ext.coverage.collect.ExecutionFragment.ExecutionFragmentKind.CHECK_BLOCK);
      ArrayList<java.util.Map<java.lang.String,java.lang.Object>> var5 = new ArrayList();
      io.karatelabs.ext.coverage.collect.TestExecution.ExecutionOutcome var6 = var4 == null ? io.karatelabs.ext.coverage.collect.TestExecution.ExecutionOutcome.UNKNOWN : var4.c();

      for (ExecutionFragment var8 : var1) {
         if (var8.a() == io.karatelabs.ext.coverage.collect.ExecutionFragment.ExecutionFragmentKind.ORIGINAL && var8 != var3 && var8.c() != var6 && var8.c() != io.karatelabs.ext.coverage.collect.TestExecution.ExecutionOutcome.UNKNOWN && var6 != io.karatelabs.ext.coverage.collect.TestExecution.ExecutionOutcome.UNKNOWN) {
            var var9 = a("fragmentConflict", "original records disagree on outcome");
            var9.put("field", "outcome");
            var9.put("fragments", List.of(var3.a().tag(), var8.a().tag()));
            var5.add(var9);
            var6 = var6.worse(var8.c());
         }
      }

      Long var24 = var4 == null ? null : var4.d();
      Long var25 = var4 == null ? null : var4.e();
      io.karatelabs.ext.coverage.collect.TestExecution.ExecutionPhase var26;
      if (var4 != null && var4.h()) {
         var26 = io.karatelabs.ext.coverage.collect.TestExecution.ExecutionPhase.TERMINAL;
      } else if (var2) {
         var26 = io.karatelabs.ext.coverage.collect.TestExecution.ExecutionPhase.RUNNING;
         var6 = io.karatelabs.ext.coverage.collect.TestExecution.ExecutionOutcome.UNKNOWN;
      } else {
         var26 = io.karatelabs.ext.coverage.collect.TestExecution.ExecutionPhase.TERMINAL;
         var6 = io.karatelabs.ext.coverage.collect.TestExecution.ExecutionOutcome.UNKNOWN;
         var5.add(a("incompleteEvidence", "no execution-end was recorded"));
      }

      ExecutionFragment var10 = a(var1, true, io.karatelabs.ext.coverage.collect.ExecutionFragment.ExecutionFragmentKind.CHECK_BLOCK);
      if (var10 == null) {
         var10 = a(var1);
      }

      if (var10 == null) {
         var10 = a(var1, true, io.karatelabs.ext.coverage.collect.ExecutionFragment.ExecutionFragmentKind.TRANSCRIPT, io.karatelabs.ext.coverage.collect.ExecutionFragment.ExecutionFragmentKind.STREAM, io.karatelabs.ext.coverage.collect.ExecutionFragment.ExecutionFragmentKind.CONTAINER);
      }

      boolean var11 = var10 != null && var10.g();
      ExecutionFragment var12 = var10 == null ? null : a(var1, var10);
      if (var12 != null) {
         var var13 = a("fragmentConflict", "carriers disagree on capture");
         var13.put("field", "capture");
         var13.put("fragments", List.of(var10.a().tag(), var12.a().tag()));
         var5.add(var13);
         var11 = false;
      }

      ExecutionFragment var27 = var10 == null ? null : b(var1, var10);
      if (var27 != null) {
         var var14 = a("fragmentConflict", "carriers disagree on assertions");
         var14.put("field", "assertions");
         var14.put("fragments", List.of(var10.a().tag(), var27.a().tag()));
         var5.add(var14);
         var11 = false;
      }

      List var28 = var10 == null ? List.of() : var10.p();
      io.karatelabs.ext.coverage.AssertionStrengthSummary var15 = null;
      if (var10 == null) {
         if (var26 == io.karatelabs.ext.coverage.collect.TestExecution.ExecutionPhase.TERMINAL && !a(var5, "incompleteEvidence")) {
            var5.add(a("incompleteEvidence", "no complete assertion carrier"));
         }
      } else if (var11) {
         var15 = io.karatelabs.ext.coverage.AssertionStrengthSummary.a(var28, var6 == io.karatelabs.ext.coverage.collect.TestExecution.ExecutionOutcome.PASSED);
      }

      ArrayList var16 = new ArrayList();
      ArrayList var17 = new ArrayList();
      LinkedHashSet<java.lang.String> var18 = new LinkedHashSet();
      ExecutionFragment var19 = a(var1, true, io.karatelabs.ext.coverage.collect.ExecutionFragment.ExecutionFragmentKind.CHECK_BLOCK);

      for (ExecutionFragment var21 : var1) {
         if (var21.a() != io.karatelabs.ext.coverage.collect.ExecutionFragment.ExecutionFragmentKind.REPLAY) {
            if (var21.a() != io.karatelabs.ext.coverage.collect.ExecutionFragment.ExecutionFragmentKind.TRANSCRIPT || var19 == null) {
               var16.addAll(var21.n());
            }

            var17.addAll(var21.o());

            for (String var23 : var21.m()) {
               if (!var23.startsWith("fragment=") && !var23.startsWith("exec=")) {
                  var18.add(var23);
               }
            }
         }
      }

      return new TestExecution(
         var0.a(),
         var0.e(),
         var0.f(),
         var0.b(),
         var24,
         var25,
         var0.j(),
         var26,
         var6,
         var11,
         var15,
         var0.d(),
         var0.g(),
         var0.h(),
         var0.i(),
         var18,
         var0.c(),
         var16,
         var17,
         var28,
         var1,
         var5
      );
   }

   private static ExecutionFragment a(List<ExecutionFragment> var0, ExecutionFragment var1) {
      for (ExecutionFragment var3 : var0) {
         if (var3 != var1 && var3.h() && !a(var3) && (var3.a() == io.karatelabs.ext.coverage.collect.ExecutionFragment.ExecutionFragmentKind.CHECK_BLOCK || var3.a() == io.karatelabs.ext.coverage.collect.ExecutionFragment.ExecutionFragmentKind.TRANSCRIPT) && var3.g() != var1.g()) {
            return var3;
         }
      }

      return null;
   }

   private static ExecutionFragment b(List<ExecutionFragment> var0, ExecutionFragment var1) {
      if (!var1.g()) {
         return null;
      } else {
         for (ExecutionFragment var3 : var0) {
            if (var3 != var1 && var3.h() && var3.g() && !a(var3) && var3.a() != io.karatelabs.ext.coverage.collect.ExecutionFragment.ExecutionFragmentKind.REPLAY && var3.a() != io.karatelabs.ext.coverage.collect.ExecutionFragment.ExecutionFragmentKind.ORIGINAL && !a(var3.p(), var1.p())) {
               return var3;
            }
         }

         return null;
      }
   }

   private static boolean a(List<Map<String, Object>> var0, List<Map<String, Object>> var1) {
      if (var0.size() != var1.size()) {
         return false;
      } else {
         for (int var2 = 0; var2 < var0.size(); var2++) {
            if (!a((Map<String, Object>)var0.get(var2)).equals(a((Map<String, Object>)var1.get(var2)))) {
               return false;
            }
         }

         return true;
      }
   }

   private static Map<String, Object> a(Map<String, Object> var0) {
      var var1 = new LinkedHashMap<>(var0);
      var1.remove("type");
      var1.remove("ts");
      return var1;
   }

   private static boolean a(ExecutionFragment var0) {
      return var0.a() == io.karatelabs.ext.coverage.collect.ExecutionFragment.ExecutionFragmentKind.TRANSCRIPT && !var0.g() && var0.p().isEmpty();
   }

   private static ExecutionFragment a(List<ExecutionFragment> var0) {
      for (ExecutionFragment var2 : var0) {
         if (var2.a() == io.karatelabs.ext.coverage.collect.ExecutionFragment.ExecutionFragmentKind.CONTAINER && var2.h() && var2.g()) {
            return var2;
         }
      }

      return null;
   }

   private static ExecutionFragment a(List<ExecutionFragment> var0, io.karatelabs.ext.coverage.collect.ExecutionFragment.ExecutionFragmentKind... var1) {
      return a(var0, false, var1);
   }

   private static ExecutionFragment a(List<ExecutionFragment> var0, boolean var1, io.karatelabs.ext.coverage.collect.ExecutionFragment.ExecutionFragmentKind... var2) {
      for (io.karatelabs.ext.coverage.collect.ExecutionFragment.ExecutionFragmentKind var6 : var2) {
         for (ExecutionFragment var8 : var0) {
            if (var8.a() == var6 && (!var1 || var8.h())) {
               return var8;
            }
         }
      }

      return null;
   }

   static Map<String, Object> a(String var0, String var1) {
      LinkedHashMap<java.lang.String, java.lang.Object> var2 = new LinkedHashMap();
      var2.put("kind", var0);
      var2.put("detail", var1);
      return var2;
   }

   private static boolean a(List<Map<String, Object>> var0, String var1) {
      return var0.stream().anyMatch(var1x -> var1.equals(var1x.get("kind")));
   }

   public record ExecutionDescriptor(String a, String b, String c, String d, String e, String f, String g, String h, int i, Long j) {
   }
}
