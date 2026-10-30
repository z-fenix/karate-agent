package io.karatelabs.ext.coverage.graph;

import io.karatelabs.ext.coverage.Action;
import io.karatelabs.ext.coverage.CoverageItem;
import io.karatelabs.ext.coverage.Warning;
import io.karatelabs.ext.coverage.HttpAction;
import io.karatelabs.ext.coverage.CoverageMapResolver;
import io.karatelabs.ext.coverage.TestExecutionSummary;
import io.karatelabs.ext.coverage.TagCoverageStat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.function.UnaryOperator;
import java.util.regex.Pattern;

public class TraceGraph {
   private final LinkedHashMap<String, CoverageItem> i = new LinkedHashMap<>();
   private final LinkedHashMap<String, String> j = new LinkedHashMap<>();
   private final LinkedHashMap<String, TestNode> k = new LinkedHashMap<>();
   private final List<CoverageHit> l = new ArrayList<>();
   private final LinkedHashMap<String, io.karatelabs.ext.coverage.CoverageSource> m = new LinkedHashMap<>();
   private final LinkedHashMap<String, List<Action>> n = new LinkedHashMap<>();
   private final LinkedHashMap<String, List<Warning>> o = new LinkedHashMap<>();
   private io.karatelabs.ext.coverage.CoverageRunEvidence p;
   private final LinkedHashMap<String, io.karatelabs.ext.coverage.collect.TestExecution> q = new LinkedHashMap<>();
   private final Map<String, Object> r = new LinkedHashMap<>();
   private io.karatelabs.ext.coverage.graph.ExecutionSelection.SelectionOptions s = io.karatelabs.ext.coverage.graph.ExecutionSelection.SelectionOptions.a();
   private io.karatelabs.ext.coverage.collect.ExecutionInventory t = io.karatelabs.ext.coverage.collect.ExecutionInventory.c;
   private io.karatelabs.ext.coverage.graph.ExecutionSelection.SelectionResult u;
   private LinkedHashMap<String, TestNode> v;
   private Map<String, io.karatelabs.ext.coverage.graph.ExecutionSelection.SelectedExecution> w;
   private List<CoverageHit> x;
   private static final Set<String> y = Set.of("req", "cov");
   public static final String a = "A-Za-z0-9._\\-:/{},";
   public static final Set<String> b = Set.of("req", "cov", "ref", "real");
   private static final Pattern z = Pattern.compile("[A-Za-z0-9_.-]+");
   public static final String c = "oracle=rules";
   public static final String d = "evidence=refusal";
   public static final String e = "evidence=model";
   public static final String f = "proof=";
   public static final String g = "ai";
   public static final String h = "ai=weakened";

   public static String a(String var0, String var1) {
      return var0 + ":" + var1;
   }

   private static String a(String var0, String var1, String var2) {
      return var0 != null && !var0.isBlank() ? var0 : io.karatelabs.ext.coverage.graph.TestNode.a(var1, var2);
   }

   public static TraceGraph a(List<io.karatelabs.ext.coverage.CoverageSource> var0, io.karatelabs.ext.coverage.CoverageRunEvidence var1) {
      return a(var0, var1, null);
   }

   public static TraceGraph a(List<io.karatelabs.ext.coverage.CoverageSource> var0, io.karatelabs.ext.coverage.CoverageRunEvidence var1, Object var2) {
      TraceGraph var3 = new TraceGraph();
      var3.p = var1;

      for (io.karatelabs.ext.coverage.CoverageSource var5 : var0) {
         var3.b(var5);
      }

      List var7;
      if (var1 instanceof io.karatelabs.ext.coverage.collect.JsonlRunEvidenceReader var8) {
         var7 = var8.g();
         var3.a(var8.i());
      } else {
         var7 = a(var1);
      }

      for (io.karatelabs.ext.coverage.collect.TestExecution var6 : (Iterable<io.karatelabs.ext.coverage.collect.TestExecution>) (Iterable<?>) (var7)) {
         var3.a(var6, var2);
      }

      var3.j();
      return var3;
   }

   private static List<io.karatelabs.ext.coverage.collect.TestExecution> a(io.karatelabs.ext.coverage.CoverageRunEvidence var0) {
      LinkedHashMap<java.lang.String, java.util.ArrayList> var1 = new LinkedHashMap();

      for (Action var3 : var0.a()) {
         var1.computeIfAbsent(a(var3.slug(), var3.featurePath(), var3.scenarioName()), var0x -> new ArrayList<>()).add(var3);
      }

      var var11 = var0.b();
      LinkedHashMap<java.lang.String, java.lang.Integer> var12 = new LinkedHashMap();

      for (int var4 = 0; var4 < var11.size(); var4++) {
         TestExecutionSummary var5 = (TestExecutionSummary)var11.get(var4);
         var12.put(a(var5.f(), var5.d(), var5.a()), var4);
      }

      String var13 = var0.c();
      ArrayList<io.karatelabs.ext.coverage.collect.TestExecution> var14 = new ArrayList();

      for (int var6 = 0; var6 < var11.size(); var6++) {
         TestExecutionSummary var7 = (TestExecutionSummary)var11.get(var6);
         String var8 = a(var7.f(), var7.d(), var7.a());
         List var9 = var12.get(var8) == var6 ? (List)var1.remove(var8) : null;
         boolean var10 = "running".equals(var7.e());
         var14.add(
            new io.karatelabs.ext.coverage.collect.TestExecution(
               var13 + "#" + (var6 + 1),
               var8,
               null,
               var13,
               null,
               null,
               null,
               var10 ? io.karatelabs.ext.coverage.collect.TestExecution.ExecutionPhase.RUNNING : io.karatelabs.ext.coverage.collect.TestExecution.ExecutionPhase.TERMINAL,
               var10 ? io.karatelabs.ext.coverage.collect.TestExecution.ExecutionOutcome.UNKNOWN : io.karatelabs.ext.coverage.collect.TestExecution.ExecutionOutcome.of(var7.e()),
               var7.g() != null,
               var7.g(),
               var8,
               var7.a(),
               var7.d(),
               var7.b(),
               var7.c() == null ? Set.of() : var7.c(),
               var13,
               var9 == null ? List.of() : var9,
               List.of(),
               List.of(),
               List.of(),
               List.of()
            )
         );
      }

      int var15 = 0;

      for (Entry var17 : (Iterable<Entry>) (Iterable<?>) (var1.entrySet())) {
         Action var18 = (Action)((List)var17.getValue()).get(0);
         LinkedHashMap<String, Object> var19 = new LinkedHashMap();
         var19.put("kind", "unattributed");
         var19.put("detail", "an observed action with no scenario result to attribute it to");
         var14.add(
            new io.karatelabs.ext.coverage.collect.TestExecution(
               var13 + "#u" + ++var15,
               (String)var17.getKey(),
               null,
               var13,
               null,
               null,
               null,
               io.karatelabs.ext.coverage.collect.TestExecution.ExecutionPhase.TERMINAL,
               io.karatelabs.ext.coverage.collect.TestExecution.ExecutionOutcome.UNKNOWN,
               false,
               null,
               (String)var17.getKey(),
               var18.scenarioName(),
               var18.featurePath(),
               var18.scenarioLine(),
               var18.tags() == null ? Set.of() : var18.tags(),
               var13,
               (List<Action>)var17.getValue(),
               List.of(),
               List.of(),
               List.of(),
               List.of(var19)
            )
         );
      }

      return var14;
   }

   public static String a(String var0) {
      if (var0 != null && !var0.isBlank()) {
         String var1 = var0.startsWith("@") ? var0.substring(1) : var0;
         if (var1.chars().anyMatch(Character::isWhitespace)) {
            return "'" + var0 + "' contains whitespace — a tag is one token";
         } else {
            int var2 = var1.indexOf(61);
            String var3 = var2 < 0 ? var1 : var1.substring(0, var2);
            if (!z.matcher(var3).matches()) {
               return "'" + var0 + "' has no valid tag name before '=' (letters, digits, '.', '-', '_')";
            } else if (var2 < 0) {
               return null;
            } else {
               String var4 = var1.substring(var2 + 1);
               if (var4.isEmpty()) {
                  return "'" + var0 + "' has no value";
               } else if (!var4.matches("[A-Za-z0-9._\\-:/{},]+")) {
                  return "'" + var0 + "' has a character outside the tag charset (letters, digits, '.', '-', '_', ':', '/', '{', '}', ',')";
               } else if (!b.contains(var3)) {
                  return null;
               } else {
                  boolean var5 = false;
                  boolean var6 = false;

                  for (String var10 : var4.split(",", -1)) {
                     if (var10.isEmpty()) {
                        return "'" + var0 + "' has an empty comma-separated value";
                     }

                     int var11 = var10.indexOf(58);
                     if (var11 == 0) {
                        return "'" + var0 + "' has an empty namespace — use the bare form '@" + var3 + "=" + var10.substring(1) + "'";
                     }

                     if (var11 > 0) {
                        var5 = true;
                     } else {
                        var6 = true;
                     }
                  }

                  return var5 && var6
                     ? "'" + var0 + "' mixes namespaced and bare values — a multi-value @" + var3 + "= repeats the namespace on every value"
                     : null;
               }
            }
         }
      } else {
         return "a tag must be a non-blank token like '@req=ORD-001/1'";
      }
   }

   public void a(io.karatelabs.ext.coverage.collect.TestExecution var1, Object var2) {
      if (this.a(var1)) {
         if (!var1.D().isEmpty()) {
            for (Action var12 : var1.x()) {
               this.a(var12, a(var12.slug(), var12.featurePath(), var12.scenarioName()), var1.g());
            }

            for (TestNode var13 : var1.D()) {
               for (String var16 : a(var13.e())) {
                  this.l.add(io.karatelabs.ext.coverage.graph.CoverageHit.a(var16, var13.a()).a(var1.g()));
               }
            }

            this.k();
         } else {
            for (Action var4 : var1.x()) {
               this.a(var4, var1.r(), var1.g());
            }

            if ("recording".equals(var1.C().get("origin"))) {
               this.k();
            } else {
               var var7 = a(var1.v());
               if (var2 != null) {
                  TestExecutionSummary var10 = new TestExecutionSummary(var1.s(), var1.u(), var1.v(), var1.t(), var1.o().wire(), var1.r(), var1.q());

                  for (String var6 : io.karatelabs.ext.coverage.CoverageMapResolver.a(var2, io.karatelabs.ext.coverage.CoverageMapResolver.a(var10, var1.r()))) {
                     var7.add(b(var6));
                  }
               }

               for (String var14 : (Iterable<String>) (Iterable<?>) (var7)) {
                  this.l.add(io.karatelabs.ext.coverage.graph.CoverageHit.a(var14, var1.r()).a(var1.g()));
               }

               this.l.addAll(var1.y());
               this.k();
            }
         }
      }
   }

   private void j() {
      LinkedHashMap<java.lang.String, java.util.Map> var1 = new LinkedHashMap();

      for (io.karatelabs.ext.coverage.collect.TestExecution var3 : this.q.values()) {
         for (String var5 : var3.v()) {
            int var6 = var5.indexOf(61);
            if (var6 > 0 && "dim".equals(var5.substring(0, var6).trim())) {
               String var7 = var5.substring(var6 + 1).trim();
               int var8 = var7.indexOf(58);
               if (var8 > 0 && var8 != var7.length() - 1) {
                  String var9 = var7.substring(0, var8).trim();
                  String var10 = var7.substring(var8 + 1).trim();
                  if (!var9.isEmpty() && !var10.isEmpty()) {
                     var1.computeIfAbsent(var3.r(), var0 -> new LinkedHashMap<>()).put(var9, var10);
                  }
               }
            }
         }
      }

      if (!var1.isEmpty()) {
         this.a(var1x -> {
            if (var1x.c() != io.karatelabs.ext.coverage.graph.HitOrigin.OBSERVED) {
               return var1x;
            } else {
               Map var2 = (Map)var1.get(var1x.b());
               return var2 == null ? var1x : var1x.b(var2);
            }
         });
      }
   }

   public static Set<String> a(Set<String> var0) {
      LinkedHashSet<java.lang.String> var1 = new LinkedHashSet();

      for (String var3 : (Iterable<String>) (Iterable<?>) (var0 == null ? Set.of() : var0)) {
         int var4 = var3.indexOf(61);
         if (var4 > 0) {
            String var5 = var3.substring(0, var4).trim();
            if (y.contains(var5)) {
               for (String var9 : var3.substring(var4 + 1).split(",")) {
                  String var10 = var9.trim();
                  if (!var10.isEmpty()) {
                     var1.add(b(var10));
                  }
               }
            }
         }
      }

      return var1;
   }

   public static String b(String var0) {
      int var1 = var0.indexOf(58);
      return var1 > 0 ? a(var0.substring(0, var1), var0.substring(var1 + 1)) : a("req", var0);
   }

   private void b(io.karatelabs.ext.coverage.CoverageSource var1) {
      String var2 = var1.f();
      this.m.put(var2, var1);
      this.n.put(var2, new ArrayList<>());
      this.o.put(var2, new ArrayList<>());

      for (CoverageItem var4 : var1.a()) {
         this.a(var2, var4);
      }

      this.o.get(var2).addAll(var1.g());
   }

   public void a(String var1, CoverageItem var2) {
      String var3 = a(var1, var2.id());
      this.i.put(var3, var2);
      this.j.put(var3, var1);
   }

   public TestNode a(Action var1) {
      String var2 = a(var1.slug(), var1.featurePath(), var1.scenarioName());
      this.k();
      return this.k.computeIfAbsent(var2, var2x -> new TestNode(var2, var1.scenarioName(), var1.featurePath(), var1.scenarioLine(), var1.tags(), io.karatelabs.ext.coverage.graph.TestNode.TestStatus.PASSED));
   }

   public TestNode a(TestNode var1) {
      this.k.put(var1.a(), var1);
      this.k();
      return var1;
   }

   public void a(CoverageHit var1) {
      this.l.add(var1);
      this.k();
   }

   public void a(Action var1, String var2) {
      this.a(var1, var2, null);
   }

   public void a(Action var1, String var2, String var3) {
      for (io.karatelabs.ext.coverage.CoverageSource var5 : this.m.values()) {
         if (var5.a(var1)) {
            String var6 = var5.f();
            CoverageItem var7 = var5.b(var1);
            if (var7 != null) {
               this.l.add(io.karatelabs.ext.coverage.graph.CoverageHit.a(a(var6, var7.id()), var2, var1, var5.b(var1, var7)).a(var3));
               this.o.get(var6).addAll(var5.a(var1, var7));
            } else {
               this.n.get(var6).add(var1);
            }
         }
      }

      this.k();
   }

   public void a(String var1, Set<String> var2) {
      this.a(var1, var2, null);
   }

   public void a(String var1, Set<String> var2, String var3) {
      for (String var5 : a(var2)) {
         this.l.add(io.karatelabs.ext.coverage.graph.CoverageHit.a(var5, var1).a(var3));
      }

      this.k();
   }

   public boolean a(io.karatelabs.ext.coverage.collect.TestExecution var1) {
      this.k();
      if (this.q.putIfAbsent(var1.g(), var1) != null) {
         this.a(Map.of("duplicateExecutionIds", List.of(var1.g())));
         return false;
      } else {
         return true;
      }
   }

   public Collection<io.karatelabs.ext.coverage.collect.TestExecution> a() {
      return this.l().f().values();
   }

   public void a(Map<String, Object> var1) {
      for (Entry var3 : var1.entrySet()) {
         Object var4 = this.r.get(var3.getKey());
         if (var3.getValue() instanceof List var5) {
            ArrayList<java.lang.Object> var12 = var4 instanceof List var8 ? new ArrayList(var8) : new ArrayList();

            for (Object var14 : var5) {
               if (!var12.contains(var14)) {
                  var12.add(var14);
               }
            }

            this.r.put((String)var3.getKey(), var12);
         } else if (var3.getValue() instanceof Number var6) {
            long var11 = (var4 instanceof Number var9 ? var9.longValue() : 0L) + var6.longValue();
            this.r.put((String)var3.getKey(), var11);
         } else {
            this.r.put((String)var3.getKey(), var3.getValue());
         }
      }

      this.k();
   }

   public Map<String, Object> b() {
      return this.r;
   }

   public void a(UnaryOperator<CoverageHit> var1) {
      this.l.replaceAll(var1);
      this.k();
   }

   public void a(String var1, CoverageItem var2, Action var3) {
      this.a(var1, var2, var3, MatchKind.EXACT);
   }

   public void a(String var1, CoverageItem var2, Action var3, MatchKind var4) {
      TestNode var5 = this.a(var3);
      this.l.add(io.karatelabs.ext.coverage.graph.CoverageHit.a(a(var1, var2.id()), var5.a(), var3, var4));
      this.k();
   }

   public void a(io.karatelabs.ext.coverage.graph.ExecutionSelection.SelectionOptions var1) {
      this.a(var1, io.karatelabs.ext.coverage.collect.ExecutionInventory.c);
   }

   public void a(io.karatelabs.ext.coverage.graph.ExecutionSelection.SelectionOptions var1, io.karatelabs.ext.coverage.collect.ExecutionInventory var2) {
      this.s = var1;
      this.t = var2 == null ? io.karatelabs.ext.coverage.collect.ExecutionInventory.c : var2;
      this.k();
   }

   public io.karatelabs.ext.coverage.graph.ExecutionSelection.SelectionOptions c() {
      return this.s;
   }

   public Map<String, Object> d() {
      return this.l().e();
   }

   public io.karatelabs.ext.coverage.graph.ExecutionSelection.SelectedExecution c(String var1) {
      this.l();
      io.karatelabs.ext.coverage.graph.ExecutionSelection.SelectedExecution var2 = this.u.a().get(var1);
      return var2 != null ? var2 : this.w.get(var1);
   }

   public boolean d(String var1) {
      return this.l().b().contains(var1);
   }

   public String e(String var1) {
      return this.l().c().get(var1);
   }

   public boolean f(String var1) {
      return this.l().d().contains(var1);
   }

   private void k() {
      this.u = null;
   }

   private io.karatelabs.ext.coverage.graph.ExecutionSelection.SelectionResult l() {
      if (this.u == null) {
         this.u = io.karatelabs.ext.coverage.graph.ExecutionSelection.a(this.q.values(), this.s, this.t, this.r);
         this.v = new LinkedHashMap<>(this.k);
         this.w = new LinkedHashMap<>();

         for (io.karatelabs.ext.coverage.graph.ExecutionSelection.SelectedExecution var2 : this.u.a().values()) {
            if (var2.b().D().isEmpty()) {
               this.v.put(var2.a(), var2.b().d());
            } else {
               for (TestNode var4 : var2.b().D()) {
                  this.v.put(var4.a(), var4);
                  this.w.put(var4.a(), var2);
               }
            }
         }

         this.x = new ArrayList<>();

         for (CoverageHit var6 : this.l) {
            if (var6.i() == null || this.u.b().contains(var6.i())) {
               String var7 = this.u.g().get(var6.i());
               this.x.add(var7 == null ? var6 : var6.b(var7));
            }
         }
      }

      return this.u;
   }

   private Map<String, TestNode> m() {
      this.l();
      return this.v;
   }

   private List<CoverageHit> n() {
      this.l();
      return this.x;
   }

   public Collection<CoverageItem> e() {
      return this.i.values();
   }

   public Collection<TestNode> f() {
      return this.m().values();
   }

   public List<CoverageHit> g() {
      return List.copyOf(this.n());
   }

   public List<CoverageHit> g(String var1) {
      ArrayList<io.karatelabs.ext.coverage.graph.CoverageHit> var2 = new ArrayList();

      for (CoverageHit var4 : this.n()) {
         if (var4.a().equals(var1)) {
            var2.add(var4);
         }
      }

      return var2;
   }

   public List<Action> h(String var1) {
      return this.n.getOrDefault(var1, List.of());
   }

   public List<Warning> i(String var1) {
      return this.o.getOrDefault(var1, List.of());
   }

   public List<String> h() {
      return new ArrayList<>(this.m.keySet());
   }

   public Map<String, CoverageItem> i() {
      return new LinkedHashMap<>(this.i);
   }

   public String j(String var1) {
      return this.j.get(var1);
   }

   public List<Action> k(String var1) {
      ArrayList<io.karatelabs.ext.coverage.Action> var2 = new ArrayList();

      for (CoverageHit var4 : this.n()) {
         if (var4.c() == io.karatelabs.ext.coverage.graph.HitOrigin.OBSERVED && var4.d() != null && var4.a().equals(var1)) {
            var2.add(var4.d());
         }
      }

      return var2;
   }

   public List<String> l(String var1) {
      LinkedHashSet<java.lang.String> var2 = new LinkedHashSet();

      for (String var4 : this.v(var1)) {
         var2.addAll(this.l(var4));
      }

      for (CoverageHit var6 : this.n()) {
         if (var6.a().equals(var1)) {
            var2.add(var6.b());
         }
      }

      return new ArrayList<>(var2);
   }

   public Map<String, Object> m(String var1) {
      CoverageItem var2 = this.i.get(var1);
      var var3 = this.k(var1);
      io.karatelabs.ext.coverage.CoverageSource var4 = this.m.get(this.j(var1));
      if (var4 != null) {
         return var4.a(var3, var2);
      } else {
         LinkedHashMap<java.lang.String, java.lang.Object> var5 = new LinkedHashMap();
         var5.put("callCount", var3.size());
         var5.put("avgDurationMs", 0L);
         var5.put("queryParamsUsed", new ArrayList());
         return var5;
      }
   }

   public CoverageStatus n(String var1) {
      var var2 = this.v(var1);
      if (!var2.isEmpty()) {
         return this.a(var2);
      } else {
         var var3 = this.g(var1);
         if (var3.isEmpty()) {
            return io.karatelabs.ext.coverage.graph.CoverageStatus.NOTCOVERED;
         } else {
            boolean var4 = false;

            for (CoverageHit var6 : (Iterable<CoverageHit>) (Iterable<?>) (var3)) {
               String var7 = this.c(var6);
               if ("fail".equals(var7)) {
                  return io.karatelabs.ext.coverage.graph.CoverageStatus.FAILING;
               }

               var4 |= "pass".equals(var7) && !this.b(var6);
            }

            return var4 ? io.karatelabs.ext.coverage.graph.CoverageStatus.COVERED : io.karatelabs.ext.coverage.graph.CoverageStatus.NOTRUN;
         }
      }
   }

   public boolean o(String var1) {
      var var2 = this.v(var1);
      if (!var2.isEmpty()) {
         for (String var6 : (Iterable<String>) (Iterable<?>) (var2)) {
            if (this.o(var6)) {
               return true;
            }
         }

         return false;
      } else if (this.n(var1) != io.karatelabs.ext.coverage.graph.CoverageStatus.NOTRUN) {
         return false;
      } else {
         for (CoverageHit var4 : this.g(var1)) {
            if ("pass".equals(this.c(var4)) && this.b(var4)) {
               return true;
            }
         }

         return false;
      }
   }

   private boolean b(CoverageHit var1) {
      TestNode var2 = this.m().get(var1.b());
      return var2 != null && var2.e().contains("ai=weakened");
   }

   private String c(CoverageHit var1) {
      String var2 = d(var1);
      if (var2 != null) {
         return var2;
      } else {
         TestNode var3 = this.m().get(var1.b());
         if (var3 == null) {
            return null;
         } else {
            return switch (var3.f()) {
               case PASSED -> "pass";
               case FAILED -> "fail";
               default -> null;
            };
         }
      }
   }

   private static String d(CoverageHit var0) {
      if (var0.f() && var0.g() != null && var0.g().get("status") != null) {
         return String.valueOf(var0.g().get("status"));
      } else {
         return !(
               var0.d() instanceof io.karatelabs.ext.coverage.EmitAction var1
                  && var1.c() != null
                  && (Boolean.TRUE.equals(var1.c().get("verified")) || Boolean.TRUE.equals(var1.c().get("oracle")) && var1.c().get("propertyStatus") != null)
            )
            ? null
            : String.valueOf(var1.c().get("status"));
      }
   }

   private List<String> v(String var1) {
      String var2 = var1 + "/";
      ArrayList<java.lang.String> var3 = new ArrayList();

      for (Entry var5 : this.i.entrySet()) {
         if (((String)var5.getKey()).startsWith(var2) && "acceptance-criterion".equals(((CoverageItem)var5.getValue()).getMetadata("kind"))) {
            var3.add((String)var5.getKey());
         }
      }

      return var3;
   }

   public boolean p(String var1) {
      var var2 = this.v(var1);
      if (!var2.isEmpty()) {
         for (String var8 : (Iterable<String>) (Iterable<?>) (var2)) {
            if (this.p(var8)) {
               return true;
            }
         }

         return false;
      } else {
         boolean var3 = false;

         for (CoverageHit var5 : this.g(var1)) {
            String var6 = this.c(var5);
            if (var6 != null && !this.e(var5)) {
               var3 = true;
               if ("pass".equals(var6) && this.f(var5)) {
                  return false;
               }
            }
         }

         return var3;
      }
   }

   public boolean q(String var1) {
      var var2 = this.v(var1);
      if (!var2.isEmpty()) {
         for (String var7 : (Iterable<String>) (Iterable<?>) (var2)) {
            if (this.q(var7)) {
               return true;
            }
         }

         return false;
      } else {
         boolean var3 = false;

         for (CoverageHit var5 : this.g(var1)) {
            if (this.c(var5) != null) {
               var3 = true;
               if (!this.e(var5)) {
                  return false;
               }
            }
         }

         return var3;
      }
   }

   private boolean e(CoverageHit var1) {
      TestNode var2 = this.m().get(var1.b());
      return var2 != null && var2.e().contains("evidence=model");
   }

   public boolean r(String var1) {
      var var2 = this.v(var1);
      if (!var2.isEmpty()) {
         for (String var8 : (Iterable<String>) (Iterable<?>) (var2)) {
            if (this.r(var8)) {
               return true;
            }
         }

         return false;
      } else {
         boolean var3 = false;

         for (CoverageHit var5 : this.g(var1)) {
            if (this.c(var5) != null && !this.e(var5)) {
               var3 = true;
               TestNode var6 = this.m().get(var5.b());
               if (var6 == null || !var6.e().contains("evidence=refusal")) {
                  return false;
               }
            }
         }

         return var3;
      }
   }

   public boolean s(String var1) {
      var var2 = this.v(var1);
      if (!var2.isEmpty()) {
         for (String var7 : (Iterable<String>) (Iterable<?>) (var2)) {
            if (this.s(var7)) {
               return true;
            }
         }

         return false;
      } else {
         boolean var3 = false;

         for (TestNode var5 : this.w(var1)) {
            if (var5.g() == null) {
               return false;
            }

            if (var5.g().b() != 0.0) {
               return false;
            }

            var3 = true;
         }

         return var3;
      }
   }

   public Double t(String var1) {
      var var2 = this.v(var1);
      if (!var2.isEmpty()) {
         Double var8 = null;

         for (String var10 : (Iterable<String>) (Iterable<?>) (var2)) {
            if (this.n(var10) == io.karatelabs.ext.coverage.graph.CoverageStatus.COVERED) {
               Double var11 = this.t(var10);
               if (var11 == null) {
                  return null;
               }

               var8 = var8 == null ? var11 : Math.min(var8, var11);
            }
         }

         return var8;
      } else {
         Double var3 = null;

         for (TestNode var5 : this.w(var1)) {
            if (var5.g() != null) {
               double var6 = var5.g().b();
               var3 = var3 == null ? var6 : Math.max(var3, var6);
            }
         }

         return var3;
      }
   }

   private Collection<TestNode> w(String var1) {
      LinkedHashSet<java.lang.String> var2 = new LinkedHashSet();
      var2.add(var1);
      CoverageItem var3 = this.i.get(var1);
      if (var3 != null && var3.getMetadata("real") instanceof Collection var4) {
         var4.forEach(var1x -> var2.add(String.valueOf(var1x)));
      }

      LinkedHashMap<java.lang.String, io.karatelabs.ext.coverage.graph.TestNode> var8 = new LinkedHashMap();

      for (CoverageHit var6 : this.n()) {
         if (var2.contains(var6.a())) {
            TestNode var7 = this.m().get(var6.b());
            if (var7 != null && var7.f() == io.karatelabs.ext.coverage.graph.TestNode.TestStatus.PASSED && !var7.e().contains("oracle=rules") && !var7.e().contains("evidence=model")) {
               var8.put(var7.a(), var7);
            }
         }
      }

      return var8.values();
   }

   private boolean f(CoverageHit var1) {
      TestNode var2 = this.m().get(var1.b());
      return a(var1.d() instanceof io.karatelabs.ext.coverage.EmitAction var3 ? var3.c() : null, var2 == null ? Set.of() : var2.e());
   }

   public static boolean a(Map<String, Object> var0, Collection<?> var1) {
      return var0 != null && Boolean.TRUE.equals(var0.get("verified"))
         ? "pass".equals(String.valueOf(var0.get("status")))
         : (var0 == null || !Boolean.TRUE.equals(var0.get("oracle"))) && (var1 == null || !var1.contains("oracle=rules") && !var1.contains("evidence=model"));
   }

   public boolean u(String var1) {
      var var2 = this.v(var1);
      if (!var2.isEmpty()) {
         for (String var9 : (Iterable<String>) (Iterable<?>) (var2)) {
            if (this.u(var9)) {
               return true;
            }
         }

         return false;
      } else {
         boolean var3 = false;

         for (CoverageHit var5 : this.g(var1)) {
            if (var5.c() == io.karatelabs.ext.coverage.graph.HitOrigin.OBSERVED && var5.d() instanceof HttpAction var6 && this.c(var5) != null) {
               var3 = true;
               if (!var6.wasServedByMock()) {
                  return false;
               }
            }
         }

         return var3;
      }
   }

   private CoverageStatus a(List<String> var1) {
      boolean var2 = false;
      boolean var3 = false;
      boolean var4 = false;

      for (String var6 : var1) {
         switch (this.n(var6)) {
            case FAILING:
               var2 = true;
               break;
            case NOTCOVERED:
               var3 = true;
               break;
            case NOTRUN:
               var4 = true;
         }
      }

      if (var2) {
         return io.karatelabs.ext.coverage.graph.CoverageStatus.FAILING;
      } else if (var3) {
         return io.karatelabs.ext.coverage.graph.CoverageStatus.NOTCOVERED;
      } else {
         return var4 ? io.karatelabs.ext.coverage.graph.CoverageStatus.NOTRUN : io.karatelabs.ext.coverage.graph.CoverageStatus.COVERED;
      }
   }

   public io.karatelabs.ext.coverage.CoverageSourceResult a(io.karatelabs.ext.coverage.CoverageSource var1) {
      String var2 = var1.f();
      LinkedHashMap<java.lang.String, java.util.List> var3 = new LinkedHashMap();

      for (CoverageHit var5 : this.n()) {
         if (var5.c() == io.karatelabs.ext.coverage.graph.HitOrigin.OBSERVED && var2.equals(this.j.get(var5.a()))) {
            var3.computeIfAbsent(var5.a(), var0 -> new ArrayList<>()).add(var5.d());
         }
      }

      ArrayList<io.karatelabs.ext.coverage.CoveredItemDetail> var10 = new ArrayList();
      ArrayList<io.karatelabs.ext.coverage.CoverageItem> var11 = new ArrayList();
      HashSet<java.lang.String> var6 = new HashSet();

      for (CoverageItem var8 : var1.a()) {
         List var9 = (List)var3.get(a(var2, var8.id()));
         if (var9 != null && !var9.isEmpty()) {
            var10.add(new io.karatelabs.ext.coverage.CoveredItemDetail(var8, var9, var1.a(var9, var8)));
            var6.add(var8.id());
         } else {
            var11.add(var8);
         }
      }

      var var12 = this.a(var1, var6);
      var var13 = this.o.getOrDefault(var2, List.of());
      io.karatelabs.ext.coverage.CoverageSummary var14 = io.karatelabs.ext.coverage.CoverageSummary.a(var1.a().size(), var10.size(), var12, var13);
      return new io.karatelabs.ext.coverage.CoverageSourceResult(var1, this.p, var10, var11, this.h(var2), var14);
   }

   private Map<String, TagCoverageStat> a(io.karatelabs.ext.coverage.CoverageSource var1, Set<String> var2) {
      LinkedHashMap<java.lang.String, java.util.LinkedHashSet> var3 = new LinkedHashMap();

      for (CoverageItem var5 : var1.a()) {
         for (String var7 : var5.tags()) {
            var3.computeIfAbsent(var7, var0 -> new LinkedHashSet<>()).add(var5.id());
         }
      }

      LinkedHashMap<java.lang.String, io.karatelabs.ext.coverage.TagCoverageStat> var10 = new LinkedHashMap();

      for (Entry var12 : (Iterable<Entry>) (Iterable<?>) (var3.entrySet())) {
         Set var13 = (Set)var12.getValue();
         int var8 = var13.size();
         int var9 = (int)var13.stream().filter(var2::contains).count();
         var10.put((String)var12.getKey(), io.karatelabs.ext.coverage.TagCoverageStat.create((String)var12.getKey(), var8, var9));
      }

      return var10;
   }
}
