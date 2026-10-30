package io.karatelabs.ext.rules.analysis;

import io.karatelabs.ext.rules.RuleInputSpaceExplorer;
import io.karatelabs.ext.rules.RuleCoverageMatrixBuilder;
import io.karatelabs.js.ContextListener;
import io.karatelabs.js.Engine;
import io.karatelabs.js.Event;
import io.karatelabs.js.EventType;
import io.karatelabs.parser.Node;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Map.Entry;

public final class AbstractInterpreter {
   private final String a;
   private final Map<String, Object> b;
   private final Map<String, io.karatelabs.ext.rules.analysis.AbstractValue> c;
   private final io.karatelabs.ext.rules.analysis.CalcDialectAnalyzer.CalcDialectAnalysis d;
   private final Set<Integer> e;
   private final Map<Integer, String> f;
   private final List<String> g;
   private final io.karatelabs.ext.rules.analysis.AbstractInterpreter.AnalysisBudget h;
   private final io.karatelabs.ext.rules.analysis.BoxSplitSelector i;
   private final SplitTreeListener j;
   private final io.karatelabs.ext.rules.analysis.AbstractInterpreter.AbstractExecutionListener k;
   private final Map<String, String[]> l = new LinkedHashMap<>();
   private long m;
   private long n = 1L;
   private long o;
   private long p;
   private long q;
   private long r;
   private String s;

   private AbstractInterpreter(String var1, Map<String, Object> var2, io.karatelabs.ext.rules.analysis.AbstractInterpreter.AnalysisBudget var3, io.karatelabs.ext.rules.analysis.BoxSplitSelector var4, SplitTreeListener var5, io.karatelabs.ext.rules.analysis.AbstractInterpreter.AbstractExecutionListener var6) {
      this.a = var1;
      this.b = var2;
      this.i = var4;
      this.j = var5;
      this.k = var6;
      Node var7 = Engine.parse(var1);
      this.d = io.karatelabs.ext.rules.analysis.CalcDialectAnalyzer.a(var7, var2.keySet());
      this.c = io.karatelabs.ext.rules.analysis.AbstractExpressionEvaluator.a(var2);
      var6.a(this.c);
      var var8 = RuleInputSpaceExplorer.a(var7);
      this.f = RuleInputSpaceExplorer.a(var8);
      this.e = new HashSet<>(this.f.keySet());
      this.g = new ArrayList<>(new TreeSet<>(this.f.values()));
      this.h = var3;
   }

   public static io.karatelabs.ext.rules.analysis.AbstractInterpreter.AbstractInterpretationResult a(String var0, List<io.karatelabs.ext.rules.analysis.AbstractBox.BoxDimension> var1, io.karatelabs.ext.rules.analysis.AbstractInterpreter.AnalysisBudget var2) {
      return a(var0, var1, var2, io.karatelabs.ext.rules.analysis.SplitTreeListener.b);
   }

   public static io.karatelabs.ext.rules.analysis.AbstractInterpreter.AbstractInterpretationResult a(String var0, List<io.karatelabs.ext.rules.analysis.AbstractBox.BoxDimension> var1, io.karatelabs.ext.rules.analysis.AbstractInterpreter.AnalysisBudget var2, SplitTreeListener var3) {
      return a(var0, Map.of(), var1, var2, var3);
   }

   public static io.karatelabs.ext.rules.analysis.AbstractInterpreter.AbstractInterpretationResult a(String var0, Map<String, Object> var1, List<io.karatelabs.ext.rules.analysis.AbstractBox.BoxDimension> var2, io.karatelabs.ext.rules.analysis.AbstractInterpreter.AnalysisBudget var3, SplitTreeListener var4) {
      return a(var0, var1, var2, var3, var4, io.karatelabs.ext.rules.analysis.AbstractInterpreter.AbstractExecutionListener.h);
   }

   public static io.karatelabs.ext.rules.analysis.AbstractInterpreter.AbstractInterpretationResult a(String var0, Map<String, Object> var1, List<io.karatelabs.ext.rules.analysis.AbstractBox.BoxDimension> var2, io.karatelabs.ext.rules.analysis.AbstractInterpreter.AnalysisBudget var3, SplitTreeListener var4, io.karatelabs.ext.rules.analysis.AbstractInterpreter.AbstractExecutionListener var5) {
      return new AbstractInterpreter(var0, var1, var3, new io.karatelabs.ext.rules.analysis.BoxSplitSelector(), var4, var5).a(new io.karatelabs.ext.rules.analysis.AbstractBox(var2));
   }

   static io.karatelabs.ext.rules.analysis.AbstractInterpreter.AbstractInterpretationResult a(String var0, List<io.karatelabs.ext.rules.analysis.AbstractBox.BoxDimension> var1, io.karatelabs.ext.rules.analysis.AbstractInterpreter.AnalysisBudget var2, io.karatelabs.ext.rules.analysis.BoxSplitSelector var3) {
      return new AbstractInterpreter(var0, Map.of(), var2, var3, io.karatelabs.ext.rules.analysis.SplitTreeListener.b, io.karatelabs.ext.rules.analysis.AbstractInterpreter.AbstractExecutionListener.h).a(new io.karatelabs.ext.rules.analysis.AbstractBox(var1));
   }

   static io.karatelabs.ext.rules.analysis.AbstractInterpreter.AbstractInterpretationResult a(String var0, String var1, List<io.karatelabs.ext.rules.analysis.AbstractBox.BoxDimension> var2, io.karatelabs.ext.rules.analysis.AbstractInterpreter.AnalysisBudget var3) {
      AbstractInterpreter var4 = new AbstractInterpreter(var0, Map.of(), var3, new io.karatelabs.ext.rules.analysis.BoxSplitSelector(), io.karatelabs.ext.rules.analysis.SplitTreeListener.b, io.karatelabs.ext.rules.analysis.AbstractInterpreter.AbstractExecutionListener.h);
      var4.s = var1;
      return var4.a(new io.karatelabs.ext.rules.analysis.AbstractBox(var2));
   }

   private io.karatelabs.ext.rules.analysis.AbstractInterpreter.AbstractInterpretationResult a(io.karatelabs.ext.rules.analysis.AbstractBox var1) {
      ArrayList var2 = new ArrayList();
      ArrayDeque var3 = new ArrayDeque();
      var3.push(var1);
      this.j.a(var1.a, (long[])var1.b.clone(), (long[])var1.c.clone());
      boolean var4 = false;
      long var5 = 0L;

      while (!var3.isEmpty()) {
         io.karatelabs.ext.rules.analysis.AbstractBox var7 = (io.karatelabs.ext.rules.analysis.AbstractBox)var3.pop();
         if (this.m >= this.h.a()) {
            var4 = true;
            var5 += var7.b();
            this.j.a((long[])var7.b.clone(), (long[])var7.c.clone(), io.karatelabs.ext.rules.analysis.SplitTreeListener.SplitOutcome.UNRESOLVED, null, -1);
         } else {
            this.m++;
            if (var7.c()) {
               this.p++;
               var2.add(this.a(this.c(var7)));
            } else {
               io.karatelabs.ext.rules.analysis.AbstractExpressionEvaluator.EvaluationResult var8;
               try {
                  var8 = io.karatelabs.ext.rules.analysis.AbstractExpressionEvaluator.a(this.d, this.c, this.e, var7, null, this.k);
               } catch (io.karatelabs.ext.rules.analysis.AbstractExpressionEvaluator.BranchSplitException var18) {
                  io.karatelabs.ext.rules.analysis.AbstractInterpreter.BoxSplitPlan var10 = this.a(var7, var18.a);
                  this.n += 2L;
                  this.j.a(var10.a(), var10.b());
                  var3.push(var10.d());
                  var3.push(var10.c());
                  continue;
               }

               io.karatelabs.ext.rules.analysis.AbstractInterpreter.BoxInterpretationResult var9 = this.a(var7, var8);
               this.b(var9);
               var2.add(this.a(var9));
            }
         }
      }

      long var19 = var1.b();
      TreeMap var20 = new TreeMap();
      ArrayList var21 = new ArrayList();
      long var11 = 0L;
      long var13 = 0L;

      for (io.karatelabs.ext.rules.analysis.AbstractInterpreter.BoxInterpretationResult var16 : (Iterable<io.karatelabs.ext.rules.analysis.AbstractInterpreter.BoxInterpretationResult>) (Iterable<?>) (var2)) {
         var13 += var16.a();
         if (var16.e()) {
            var21.add(var16);
            var11 += var16.a();
         } else {
            io.karatelabs.ext.rules.analysis.AbstractInterpreter.SignatureAggregate var17 = (io.karatelabs.ext.rules.analysis.AbstractInterpreter.SignatureAggregate)var20.get(var16.c());
            var20.put(
               var16.c(),
               var17 == null ? new io.karatelabs.ext.rules.analysis.AbstractInterpreter.SignatureAggregate(var16.c(), var16.a(), 1L, var16.b().d()) : new io.karatelabs.ext.rules.analysis.AbstractInterpreter.SignatureAggregate(var17.a(), var17.b() + var16.a(), var17.c() + 1L, var17.d())
            );
         }
      }

      long var22 = var13 - var11;
      if (var22 + var11 + var5 != var19) {
         throw new IllegalStateException(
            "the frontier covers " + var13 + " rows of " + var19 + " (accepted " + var22 + ", refused " + var11 + ", unresolved " + var5 + ")"
         );
      } else {
         List var23 = this.a(var2);
         return new io.karatelabs.ext.rules.analysis.AbstractInterpreter.AbstractInterpretationResult(var2, var20, var21, var23, var19, var11, var22, this.m, this.n, this.o, this.p, this.q, this.r, var4, var5, this.g);
      }
   }

   private io.karatelabs.ext.rules.analysis.AbstractInterpreter.BoxInterpretationResult a(io.karatelabs.ext.rules.analysis.AbstractInterpreter.BoxInterpretationResult var1) {
      this.j.a((long[])var1.b().b.clone(), (long[])var1.b().c.clone(), var1.e() ? io.karatelabs.ext.rules.analysis.SplitTreeListener.SplitOutcome.REFUSED : io.karatelabs.ext.rules.analysis.SplitTreeListener.SplitOutcome.ACCEPTED, var1.c(), var1.f());
      return var1;
   }

   private io.karatelabs.ext.rules.analysis.AbstractInterpreter.BoxSplitPlan a(io.karatelabs.ext.rules.analysis.AbstractBox var1, io.karatelabs.ext.rules.analysis.AbstractExecutionState var2) {
      io.karatelabs.ext.rules.analysis.BoxSplitSelector.BoxSplitPoint var3;
      try {
         var3 = this.i.a(var2);
      } catch (StackOverflowError | RuntimeException var5) {
         var3 = null;
      }

      if (var3 != null && var1.b(var3.a(), var3.b())) {
         return a(var1, var3.a(), var3.b());
      } else {
         this.r++;
         int var4 = var1.a(-1L);
         return a(var1, var4, var1.c(var4));
      }
   }

   private static io.karatelabs.ext.rules.analysis.AbstractInterpreter.BoxSplitPlan a(io.karatelabs.ext.rules.analysis.AbstractBox var0, int var1, long var2) {
      io.karatelabs.ext.rules.analysis.AbstractBox[] var4 = var0.a(var1, var2);
      return new io.karatelabs.ext.rules.analysis.AbstractInterpreter.BoxSplitPlan(var1, var2, var4[0], var4[1]);
   }

   private List<AbstractInterpreter.PropertyPartition> a(List<io.karatelabs.ext.rules.analysis.AbstractInterpreter.BoxInterpretationResult> var1) {
      ArrayList var2 = new ArrayList();

      for (Entry var4 : this.l.entrySet()) {
         String var5 = (String)var4.getKey();
         long var6 = 0L;
         long var8 = 0L;
         long var10 = 0L;
         long var12 = 0L;
         Map var14 = null;

         for (io.karatelabs.ext.rules.analysis.AbstractInterpreter.BoxInterpretationResult var16 : var1) {
            if (!var16.e()) {
               TernaryBoolean var17 = var16.d().get(var5);
               if (var17 != null) {
                  if (var17 == io.karatelabs.ext.rules.analysis.TernaryBoolean.TRUE) {
                     var6 += var16.a();
                  } else if (var17 == io.karatelabs.ext.rules.analysis.TernaryBoolean.FALSE) {
                     var8 += var16.a();
                     if (var14 == null) {
                        var14 = this.a(var5, var16.b());
                     }
                  } else {
                     io.karatelabs.ext.rules.analysis.AbstractInterpreter.UnknownPropertyAllocation var18 = this.a(var5, var16, this.h.b() - var12);
                     var6 += var18.a();
                     var8 += var18.b();
                     var10 += var18.c();
                     var12 += var18.d();
                     if (var14 == null) {
                        var14 = var18.e();
                     }
                  }
               }
            }
         }

         var2.add(new AbstractInterpreter.PropertyPartition(var5, ((String[])var4.getValue())[0], ((String[])var4.getValue())[1], var6, var8, var10, var14, var12));
      }

      return var2;
   }

   private io.karatelabs.ext.rules.analysis.AbstractInterpreter.UnknownPropertyAllocation a(String var1, io.karatelabs.ext.rules.analysis.AbstractInterpreter.BoxInterpretationResult var2, long var3) {
      long[] var5 = new long[4];
      Map var6 = null;
      ArrayDeque var7 = new ArrayDeque();
      var7.push(var2.b());

      while (!var7.isEmpty()) {
         io.karatelabs.ext.rules.analysis.AbstractBox var8 = (io.karatelabs.ext.rules.analysis.AbstractBox)var7.pop();
         if (var5[3] >= var3) {
            var5[2] += var8.b();
         } else {
            var5[3]++;
            TernaryBoolean var9;
            if (var8.c()) {
               this.p++;
               var9 = this.c(var8).d().get(var1);
            } else {
               io.karatelabs.ext.rules.analysis.AbstractExpressionEvaluator.EvaluationResult var10;
               try {
                  var10 = io.karatelabs.ext.rules.analysis.AbstractExpressionEvaluator.a(this.d, this.c, this.e, var8, var1, this.k);
               } catch (io.karatelabs.ext.rules.analysis.AbstractExpressionEvaluator.BranchSplitException var13) {
                  io.karatelabs.ext.rules.analysis.AbstractInterpreter.BoxSplitPlan var12 = this.a(var8, var13.a);
                  this.o += 2L;
                  var7.push(var12.d());
                  var7.push(var12.c());
                  continue;
               }

               io.karatelabs.ext.rules.analysis.AbstractInterpreter.BoxInterpretationResult var11 = this.a(var8, var10);
               this.b(var11);
               var9 = a(var10, var1);
            }

            if (var9 != null) {
               if (var9 == io.karatelabs.ext.rules.analysis.TernaryBoolean.TRUE) {
                  var5[0] += var8.b();
               } else if (var9 == io.karatelabs.ext.rules.analysis.TernaryBoolean.FALSE) {
                  var5[1] += var8.b();
                  if (var6 == null) {
                     var6 = this.a(var1, var8);
                  }
               } else {
                  var5[2] += var8.b();
               }
            }
         }
      }

      return new io.karatelabs.ext.rules.analysis.AbstractInterpreter.UnknownPropertyAllocation(var5[0], var5[1], var5[2], var5[3], var6);
   }

   private Map<String, Object> a(String var1, io.karatelabs.ext.rules.analysis.AbstractBox var2) {
      var var3 = var2.d();
      this.q++;
      io.karatelabs.ext.rules.analysis.AbstractInterpreter.BoxInterpretationResult var4 = this.c(var2.c() ? var2 : b(var2));
      if (var4.d().get(var1) != io.karatelabs.ext.rules.analysis.TernaryBoolean.FALSE) {
         throw new io.karatelabs.ext.rules.analysis.RuleAnalysisException(
            "CERTIFICATE_MISMATCH", -1, "the checker refuted '" + var1 + "' on " + var3 + " and the concrete engine did not"
         );
      } else {
         return var3;
      }
   }

   private static io.karatelabs.ext.rules.analysis.AbstractBox b(io.karatelabs.ext.rules.analysis.AbstractBox var0) {
      io.karatelabs.ext.rules.analysis.AbstractBox var1 = var0;

      while (!var1.c()) {
         var1 = var1.b(var1.a(-1L))[0];
      }

      return var1;
   }

   private static TernaryBoolean a(io.karatelabs.ext.rules.analysis.AbstractExpressionEvaluator.EvaluationResult var0, String var1) {
      TernaryBoolean var2 = null;

      for (io.karatelabs.ext.rules.analysis.AbstractExpressionEvaluator.PropertyObservation var4 : var0.b()) {
         if (var4.a().equals(var1)) {
            var2 = var2 == null
               ? var4.c()
               : (
                  var4.c() != io.karatelabs.ext.rules.analysis.TernaryBoolean.FALSE && var2 != io.karatelabs.ext.rules.analysis.TernaryBoolean.FALSE
                     ? (
                        var4.c() != io.karatelabs.ext.rules.analysis.TernaryBoolean.MAYBE && var2 != io.karatelabs.ext.rules.analysis.TernaryBoolean.MAYBE
                           ? io.karatelabs.ext.rules.analysis.TernaryBoolean.TRUE
                           : io.karatelabs.ext.rules.analysis.TernaryBoolean.MAYBE
                     )
                     : io.karatelabs.ext.rules.analysis.TernaryBoolean.FALSE
               );
         }
      }

      return var2;
   }

   private io.karatelabs.ext.rules.analysis.AbstractInterpreter.BoxInterpretationResult a(io.karatelabs.ext.rules.analysis.AbstractBox var1, io.karatelabs.ext.rules.analysis.AbstractExpressionEvaluator.EvaluationResult var2) {
      HashMap var3 = new HashMap();

      for (Entry var5 : var2.a().entrySet()) {
         var3.put(this.f.get(var5.getKey()), (Set)var5.getValue());
      }

      String var9 = RuleCoverageMatrixBuilder.a(this.g, var3);
      LinkedHashMap var10 = new LinkedHashMap();

      for (io.karatelabs.ext.rules.analysis.AbstractExpressionEvaluator.PropertyObservation var7 : var2.b()) {
         this.l.putIfAbsent(var7.a(), new String[]{var7.d(), var7.e()});
         TernaryBoolean var8 = (TernaryBoolean)var10.get(var7.a());
         var10.put(
            var7.a(),
            var8 == null
               ? var7.c()
               : (
                  var8 != io.karatelabs.ext.rules.analysis.TernaryBoolean.FALSE && var7.c() != io.karatelabs.ext.rules.analysis.TernaryBoolean.FALSE
                     ? (
                        var8 != io.karatelabs.ext.rules.analysis.TernaryBoolean.MAYBE && var7.c() != io.karatelabs.ext.rules.analysis.TernaryBoolean.MAYBE
                           ? io.karatelabs.ext.rules.analysis.TernaryBoolean.TRUE
                           : io.karatelabs.ext.rules.analysis.TernaryBoolean.MAYBE
                     )
                     : io.karatelabs.ext.rules.analysis.TernaryBoolean.FALSE
               )
         );
      }

      return new io.karatelabs.ext.rules.analysis.AbstractInterpreter.BoxInterpretationResult(var1, var9, var10, var2.c(), var2.d(), var2.e());
   }

   private io.karatelabs.ext.rules.analysis.AbstractInterpreter.BoxInterpretationResult c(io.karatelabs.ext.rules.analysis.AbstractBox var1) {
      final HashMap var2 = new HashMap();
      ContextListener var3 = new ContextListener() {
         @Override
         public void onEvent(Event var1) {
            if (var1.type == EventType.BRANCH && var1.node != null && var1.value instanceof Boolean var2x) {
               int var4 = var1.node.getFirstToken().pos;
               if (AbstractInterpreter.this.e.contains(var4)) {
                  ((java.util.Set)(var2.computeIfAbsent(var4, var0 -> new TreeSet<>()))).add(var2x);
               }
            }
         }
      };
      ArrayList var4 = new ArrayList();
      boolean var5 = false;
      int var6 = -1;
      this.k.b(var1.d());

      try {
         io.karatelabs.ext.rules.CalcExecutor.CalcExecutionResult var7 = io.karatelabs.ext.rules.CalcExecutor.a(this.s == null ? this.a : this.s, this.b, var1.d(), var3);

         for (Object[] var9 : var7.i()) {
            var4.add(
               new io.karatelabs.ext.rules.analysis.AbstractExpressionEvaluator.PropertyObservation(
                  String.valueOf(var9[0]),
                  var9[1] instanceof Number var10 ? var10.intValue() : -1,
                  io.karatelabs.ext.rules.analysis.TernaryBoolean.of(Boolean.TRUE.equals(var9[2])),
                  String.valueOf(var9[3]),
                  var9[4] == null ? null : String.valueOf(var9[4]),
                  0L
               )
            );
         }
      } catch (io.karatelabs.ext.rules.CalcExecutor.CalcExecutionException var12) {
         var5 = true;
         var6 = var12.b;
      } catch (RuntimeException var13) {
         throw new io.karatelabs.ext.rules.analysis.RuleAnalysisException("RUNTIME_ERROR", -1, "on " + var1.d() + ": " + var13.getMessage());
      }

      return this.a(var1, new io.karatelabs.ext.rules.analysis.AbstractExpressionEvaluator.EvaluationResult(var2, var4, var5, var6, 0L, null, false));
   }

   private void b(io.karatelabs.ext.rules.analysis.AbstractInterpreter.BoxInterpretationResult var1) {
      this.q++;
      io.karatelabs.ext.rules.analysis.AbstractInterpreter.BoxInterpretationResult var2 = this.c(b(var1.b()));
      if (var1.e() != var2.e()) {
         throw new io.karatelabs.ext.rules.analysis.RuleAnalysisException(
            "CERTIFICATE_MISMATCH", var1.f(), "refusal differs on " + var1.b().d() + ": abstract " + var1.e() + ", concrete " + var2.e()
         );
      } else if (!var1.c().equals(var2.c())) {
         throw new io.karatelabs.ext.rules.analysis.RuleAnalysisException(
            "CERTIFICATE_MISMATCH", -1, "signature differs on " + var1.b().d() + ": abstract " + var1.c() + ", concrete " + var2.c()
         );
      } else {
         for (Entry var4 : var1.d().entrySet()) {
            if (((TernaryBoolean)var4.getValue()).definite()) {
               TernaryBoolean var5 = var2.d().get(var4.getKey());
               if (var5 == null ? !var1.e() : var5 != var4.getValue()) {
                  throw new io.karatelabs.ext.rules.analysis.RuleAnalysisException(
                     "CERTIFICATE_MISMATCH",
                     -1,
                     "property '" + (String)var4.getKey() + "' differs on " + var1.b().d() + ": abstract " + var4.getValue() + ", concrete " + var5
                  );
               }
            }
         }
      }
   }

   public record PropertyPartition(String b, String c, String d, long e, long f, long g, Map<String, Object> h, long i) {

      public PropertyPartition(String b, String c, String d, long e, long f, long g, Map<String, Object> h, long i) {
         this.b = b;
         this.c = c;
         this.d = d;
         this.e = e;
         this.f = f;
         this.g = g;
         this.h = h;
         this.i = i;
      }

      public long a() {
         return this.e + this.f + this.g;
      }

      public String b() {
         return this.b;
      }

      public String c() {
         return this.c;
      }

      public String d() {
         return this.d;
      }

      public long e() {
         return this.e;
      }

      public long f() {
         return this.f;
      }

      public long g() {
         return this.g;
      }

      public Map<String, Object> h() {
         return this.h;
      }

      public long i() {
         return this.i;
      }
   }

   public record AbstractInterpretationResult(List<io.karatelabs.ext.rules.analysis.AbstractInterpreter.BoxInterpretationResult> a, Map<String, io.karatelabs.ext.rules.analysis.AbstractInterpreter.SignatureAggregate> b, List<io.karatelabs.ext.rules.analysis.AbstractInterpreter.BoxInterpretationResult> c, List<AbstractInterpreter.PropertyPartition> d, long e, long f, long g, long h, long i, long j, long k, long l, long m, boolean n, long o, List<String> p) {

      public AbstractInterpretationResult(List<io.karatelabs.ext.rules.analysis.AbstractInterpreter.BoxInterpretationResult> a, Map<String, io.karatelabs.ext.rules.analysis.AbstractInterpreter.SignatureAggregate> b, List<io.karatelabs.ext.rules.analysis.AbstractInterpreter.BoxInterpretationResult> c, List<AbstractInterpreter.PropertyPartition> d, long e, long f, long g, long h, long i, long j, long k, long l, long m, boolean n, long o, List<String> p) {
         this.a = a;
         this.b = b;
         this.c = c;
         this.d = d;
         this.e = e;
         this.f = f;
         this.g = g;
         this.h = h;
         this.i = i;
         this.j = j;
         this.k = k;
         this.l = l;
         this.m = m;
         this.n = n;
         this.o = o;
         this.p = p;
      }
   }

   public record BoxInterpretationResult(io.karatelabs.ext.rules.analysis.AbstractBox b, String c, Map<String, TernaryBoolean> d, boolean e, int f, long g) {

      public BoxInterpretationResult(io.karatelabs.ext.rules.analysis.AbstractBox b, String c, Map<String, TernaryBoolean> d, boolean e, int f, long g) {
         this.b = b;
         this.c = c;
         this.d = d;
         this.e = e;
         this.f = f;
         this.g = g;
      }

      public long a() {
         return this.b.b();
      }

      public io.karatelabs.ext.rules.analysis.AbstractBox b() {
         return this.b;
      }

      public String c() {
         return this.c;
      }

      public Map<String, TernaryBoolean> d() {
         return this.d;
      }

      public boolean e() {
         return this.e;
      }

      public int f() {
         return this.f;
      }

      public long g() {
         return this.g;
      }
   }

   public record AnalysisBudget(long component1, long b) {
      public static final io.karatelabs.ext.rules.analysis.AbstractInterpreter.AnalysisBudget a = new io.karatelabs.ext.rules.analysis.AbstractInterpreter.AnalysisBudget(1000000L, 250000L);

      public AnalysisBudget(long component1, long b) {
         this.component1 = component1;
         this.b = b;
      }

      public long a() {
         return this.component1;
      }

      public long b() {
         return this.b;
      }
   }

   public interface AbstractExecutionListener extends io.karatelabs.ext.rules.analysis.AbstractExpressionEvaluator.EvaluationListener {
      io.karatelabs.ext.rules.analysis.AbstractInterpreter.AbstractExecutionListener h = new io.karatelabs.ext.rules.analysis.AbstractInterpreter.AbstractExecutionListener() {
         @Override
         public void a(Map<String, io.karatelabs.ext.rules.analysis.AbstractValue> var1) {
         }

         @Override
         public void b(Map<String, Object> var1) {
         }

         @Override
         public void selected(LinkedHashMap<String, io.karatelabs.ext.rules.analysis.AbstractValue> var1, List<String> var2) {
         }
      };

      void a(Map<String, io.karatelabs.ext.rules.analysis.AbstractValue> var1);

      void b(Map<String, Object> var1);
   }

   private record UnknownPropertyAllocation(long a, long b, long c, long d, Map<String, Object> e) {

      private UnknownPropertyAllocation(long a, long b, long c, long d, Map<String, Object> e) {
         this.a = a;
         this.b = b;
         this.c = c;
         this.d = d;
         this.e = e;
      }
   }

   public record SignatureAggregate(String a, long b, long c, Map<String, Object> d) {

      public SignatureAggregate(String a, long b, long c, Map<String, Object> d) {
         this.a = a;
         this.b = b;
         this.c = c;
         this.d = d;
      }
   }

   private record BoxSplitPlan(int a, long b, io.karatelabs.ext.rules.analysis.AbstractBox c, io.karatelabs.ext.rules.analysis.AbstractBox d) {

      private BoxSplitPlan(int a, long b, io.karatelabs.ext.rules.analysis.AbstractBox c, io.karatelabs.ext.rules.analysis.AbstractBox d) {
         this.a = a;
         this.b = b;
         this.c = c;
         this.d = d;
      }
   }
}
