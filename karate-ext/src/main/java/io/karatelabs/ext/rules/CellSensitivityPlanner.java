package io.karatelabs.ext.rules;

import io.karatelabs.js.Terms;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.function.Supplier;

final class CellSensitivityPlanner implements Function<Map<String, Supplier<List<Map<String, Object>>>>, List<Map<String, Object>>> {
   static final io.karatelabs.ext.rules.analysis.AbstractInterpreter.AnalysisBudget a = new io.karatelabs.ext.rules.analysis.AbstractInterpreter.AnalysisBudget(2000L, io.karatelabs.ext.rules.analysis.AbstractInterpreter.AnalysisBudget.a.b());
   private final io.karatelabs.ext.rules.RulebookRepository.RulebookDocument b;
   private final RuleEvaluator c;
   private final CellCoverageAnalyzer d;
   private final RuleTableCatalog e;
   private final Object f;
   private final List<io.karatelabs.ext.rules.RuleTableCatalog.TableCellReference> g;
   private final int h;
   private final List<String> i = new ArrayList<>();
   private List<io.karatelabs.ext.rules.CellCoverageAnalyzer.RowCoverageResult> j;
   private final LinkedHashSet<String> k = new LinkedHashSet<>();
   private final List<io.karatelabs.ext.rules.CellSensitivityPlanner.SensitivityCase> l = new ArrayList<>();
   private final Map<String, io.karatelabs.ext.rules.CellSensitivityPlanner.SensitivityCase> m = new HashMap<>();
   private final Set<String> n = new LinkedHashSet<>();
   private Iterator<Map<String, Object>> o;
   private Iterator<Entry<String, Supplier<List<Map<String, Object>>>>> p;
   private int q;
   private boolean r;
   private Map<String, Object> s;

   CellSensitivityPlanner(io.karatelabs.ext.rules.RulebookRepository.RulebookDocument var1, RuleEvaluator var2, Object var3, int var4) {
      this.b = var1;
      this.c = var2;
      this.d = new CellCoverageAnalyzer(var1, var2);
      this.e = var1.m();
      this.h = var4;
      this.f = var3 == null ? "unexercised" : var3;
      if (this.f instanceof List var5) {
         this.g = new ArrayList<>();

         for (Object var7 : var5) {
            io.karatelabs.ext.rules.RuleTableCatalog.TableCellReference var8 = this.d.a(String.valueOf(var7));
            if (var8 == null) {
               throw new RuleApiException(404, "LOOKUP_PATH_NOT_FOUND", "'" + var7 + "' names no numeric file-table cell (tables: " + this.e.b().keySet() + ")");
            }

            this.g.add(var8);
         }
      } else {
         if (!"all".equals(this.f) && !"unexercised".equals(this.f)) {
            throw new RuleApiException(400, "INVALID_INPUT", "target is 'all', 'unexercised' or a list of cell addresses, not " + var3);
         }

         this.g = null;
      }

      a(var1.e(), "", this.i);
   }

   Map<String, Object> a() {
      return this.s;
   }

   @Override
   public List<Map<String, Object>> apply(Map<String, Supplier<List<Map<String, Object>>>> plans) {
      return this.a(plans);
   }

   public List<Map<String, Object>> a(Map<String, Supplier<List<Map<String, Object>>>> var1) {
      this.p = var1.entrySet().iterator();
      List<Map<String, Object>> var2 = this.b.b() == null ? List.of() : this.b.b();
      this.j = this.d.a(var2, (Set<String>)null);
      LinkedHashSet<java.lang.String> var3 = null;
      if (this.g != null) {
         var3 = new LinkedHashSet();

         for (io.karatelabs.ext.rules.RuleTableCatalog.TableCellReference var5 : this.g) {
            var3.add(var5.a());
         }
      } else if ("unexercised".equals(this.f)) {
         var3 = new LinkedHashSet();

         for (io.karatelabs.ext.rules.CellCoverageAnalyzer.CellCoverage var19 : this.d.a(this.j, this.d.a()).values()) {
            if ("unread".equals(var19.c()) || "masked".equals(var19.c())) {
               var3.add(var19.b().a());
            }
         }
      }

      for (io.karatelabs.ext.rules.RuleTableCatalog.TableCellReference var20 : this.d.a()) {
         if (var3 == null || var3.contains(var20.a())) {
            this.k.add(var20.a());
         }
      }

      HashMap<java.lang.String, java.lang.Integer> var18 = new HashMap();

      for (String var6 : this.k) {
         var18.put(var6, var18.size());
      }

      List var22 = this.g != null ? this.c() : this.b();
      ArrayList<java.util.Map<java.lang.String,java.lang.Object>> var23 = new ArrayList();
      LinkedHashSet var7 = new LinkedHashSet();

      for (io.karatelabs.ext.rules.CellSensitivityPlanner.SensitivityCase var9 : (Iterable<io.karatelabs.ext.rules.CellSensitivityPlanner.SensitivityCase>) (Iterable<?>) (var22)) {
         LinkedHashMap<java.lang.String, java.lang.Object> var10 = new LinkedHashMap();
         var10.put("input", var9.a());
         var10.put("expected", this.c.a(this.b.g(), this.e.e(), var9.a(), null).b());
         var var11 = new ArrayList<>(var9.d());
         var11.sort(Comparator.comparingInt(var18::get));
         LinkedHashMap<java.lang.String, java.util.Map<java.lang.String,java.lang.Object>> var12 = new LinkedHashMap();

         for (String var14 : (Iterable<String>) (Iterable<?>) (var11)) {
            var var15 = var9.e().get(var14).a();
            var15.remove("row");
            var12.put(var14, var15);
         }

         var7.addAll(var11);
         var10.put("target", var11);
         var10.put("probes", var12);
         var23.add(var10);
      }

      this.s = this.a(var23.size(), var7);
      return var23;
   }

   private List<io.karatelabs.ext.rules.CellSensitivityPlanner.SensitivityCase> b() {
      for (String var2 : this.k) {
         if (!this.n.contains(var2) && this.a(this.d.a(var2)) == null) {
            this.a(var2);
         }
      }

      LinkedHashSet var8 = new LinkedHashSet();
      IdentityHashMap<io.karatelabs.ext.rules.CellSensitivityPlanner.SensitivityCase, java.lang.Integer> var9 = new IdentityHashMap();
      Comparator var3 = Comparator.<io.karatelabs.ext.rules.CellSensitivityPlanner.SensitivityCase>comparingInt(var1 -> -(Integer)var9.get(var1)).thenComparingInt(io.karatelabs.ext.rules.CellSensitivityPlanner.SensitivityCase::c).thenComparing(io.karatelabs.ext.rules.CellSensitivityPlanner.SensitivityCase::b);
      PriorityQueue var4 = new PriorityQueue(var3);

      for (io.karatelabs.ext.rules.CellSensitivityPlanner.SensitivityCase var6 : this.l) {
         var9.put(var6, a(var6, var8));
         if ((Integer)var9.get(var6) > 0) {
            var4.add(var6);
         }
      }

      ArrayList<io.karatelabs.ext.rules.CellSensitivityPlanner.SensitivityCase> var10 = new ArrayList();

      while (!var4.isEmpty()) {
         io.karatelabs.ext.rules.CellSensitivityPlanner.SensitivityCase var11 = (io.karatelabs.ext.rules.CellSensitivityPlanner.SensitivityCase)var4.poll();
         int var7 = a(var11, var8);
         if (var7 != 0) {
            if (var7 < (Integer)var9.get(var11)) {
               var9.put(var11, var7);
               var4.add(var11);
            } else {
               var10.add(var11);
               var8.addAll(var11.d());
            }
         }
      }

      return var10;
   }

   private List<io.karatelabs.ext.rules.CellSensitivityPlanner.SensitivityCase> c() {
      LinkedHashSet var1 = new LinkedHashSet();
      ArrayList<io.karatelabs.ext.rules.CellSensitivityPlanner.SensitivityCase> var2 = new ArrayList();

      for (String var4 : this.k) {
         if (!var1.contains(var4)) {
            io.karatelabs.ext.rules.CellSensitivityPlanner.SensitivityCase var5 = this.a(this.d.a(var4));
            if (var5 == null) {
               this.a(var4);

               for (io.karatelabs.ext.rules.CellSensitivityPlanner.SensitivityCase var7 : this.l) {
                  if (var7.d().contains(var4) && (var5 == null || this.a(var7, var5, var1))) {
                     var5 = var7;
                  }
               }
            }

            if (var5 != null) {
               var2.add(var5);
               var1.addAll(var5.d());
            }
         }
      }

      return var2;
   }

   private boolean a(io.karatelabs.ext.rules.CellSensitivityPlanner.SensitivityCase var1, io.karatelabs.ext.rules.CellSensitivityPlanner.SensitivityCase var2, Set<String> var3) {
      int var4 = a(var1, var3);
      int var5 = a(var2, var3);
      if (var4 != var5) {
         return var4 > var5;
      } else {
         return var1.c() != var2.c() ? var1.c() < var2.c() : var1.b().compareTo(var2.b()) < 0;
      }
   }

   private static int a(io.karatelabs.ext.rules.CellSensitivityPlanner.SensitivityCase var0, Set<String> var1) {
      int var2 = 0;

      for (String var4 : var0.d()) {
         if (!var1.contains(var4)) {
            var2++;
         }
      }

      return var2;
   }

   private io.karatelabs.ext.rules.CellSensitivityPlanner.SensitivityCase a(Map<String, Object> var1, int var2) {
      String var3 = io.karatelabs.ext.rules.twin.CanonicalJson.c(var1);
      if (this.m.containsKey(var3)) {
         return this.m.get(var3);
      } else {
         io.karatelabs.ext.rules.CellCoverageAnalyzer.RowCoverageResult var4 = this.d.a(var2, var1, this.k);
         io.karatelabs.ext.rules.CellSensitivityPlanner.SensitivityCase var5 = null;
         if (var4.a()) {
            LinkedHashSet<java.lang.String> var6 = new LinkedHashSet();

            for (Entry var8 : var4.f().entrySet()) {
               if ("exercised".equals(((io.karatelabs.ext.rules.CellCoverageAnalyzer.CellProbeResult)var8.getValue()).f())) {
                  var6.add((String)var8.getKey());
               }
            }

            var5 = new io.karatelabs.ext.rules.CellSensitivityPlanner.SensitivityCase(var4.c(), var3, var2, var6, var4.f());
            this.l.add(var5);
            this.n.addAll(var6);
         }

         this.m.put(var3, var5);
         return var5;
      }
   }

   private io.karatelabs.ext.rules.CellSensitivityPlanner.SensitivityCase a(io.karatelabs.ext.rules.RuleTableCatalog.TableCellReference var1) {
      for (int var2 = 0; var2 < this.j.size(); var2++) {
         io.karatelabs.ext.rules.CellCoverageAnalyzer.RowCoverageResult var3 = this.j.get(var2);
         Map var4 = var3.a() ? this.a(var3, var1) : null;
         if (var4 != null) {
            io.karatelabs.ext.rules.CellSensitivityPlanner.SensitivityCase var5 = this.a(var4, var2);
            if (var5 != null && var5.d().contains(var1.a())) {
               return var5;
            }
         }
      }

      return null;
   }

   private Map<String, Object> a(io.karatelabs.ext.rules.CellCoverageAnalyzer.RowCoverageResult var1, io.karatelabs.ext.rules.RuleTableCatalog.TableCellReference var2) {
      io.karatelabs.ext.rules.RuleTableCatalog.TableDefinition var3 = this.e.b().get(var2.c());
      boolean var4 = var3.d() != null;
      ArrayList<java.util.ArrayList> var5 = new ArrayList();

      for (Entry var7 : var1.e().entrySet()) {
         io.karatelabs.ext.rules.RuleTableCatalog.TableCellReference var8 = this.e.d((String)var7.getKey());
         if (var8 != null && var8.c().equals(var3.a())) {
            for (String var10 : (Iterable<String>) (Iterable<?>) (var4 ? (Set)var7.getValue() : Collections.singleton(null))) {
               ArrayList<java.lang.String> var11 = new ArrayList();
               if (var4) {
                  var11.add(var10);
               }

               var11.addAll(var8.f());
               var5.add(var11);
            }
         }
      }

      if (var5.isEmpty()) {
         return null;
      } else {
         ArrayList<java.lang.String> var14 = new ArrayList();
         if (var4) {
            String var15 = var2.e();
            if (var15 == null) {
               var var17 = this.e.a(var2);
               var15 = var17.contains(((List)var5.get(0)).get(0)) ? (String)((List)var5.get(0)).get(0) : (var17.isEmpty() ? null : (String)var17.get(0));
            }

            if (var15 == null) {
               return null;
            }

            var14.add(var15);
         }

         var14.addAll(var2.f());
         LinkedHashMap<java.lang.String, Object> var16 = new LinkedHashMap();
         a((Object)var1.c(), "", var16);
         LinkedHashMap<java.lang.String, java.lang.Object> var18 = new LinkedHashMap();

         for (int var19 = 0; var19 < var14.size(); var19++) {
            LinkedHashSet<java.lang.String> var21 = new LinkedHashSet();

            for (List var12 : (Iterable<List>) (Iterable<?>) (var5)) {
               var21.add((String)var12.get(var19));
            }

            if (var21.size() != 1) {
               return null;
            }

            String var23 = (String)var21.iterator().next();
            String var24 = this.a(var16, var23);
            if (var24 == null) {
               if (!var23.equals(var14.get(var19))) {
                  return null;
               }
            } else {
               Object var13 = a(var16.get(var24), (String)var14.get(var19));
               if (var13 == null || var18.containsKey(var24) && !a(var18.get(var24)).equals(a(var13))) {
                  return null;
               }

               var18.put(var24, var13);
            }
         }

         Map<java.lang.String, java.lang.Object> var20 = (Map)io.karatelabs.ext.common.SandboxExecutor.a(var1.c());
         var18.forEach((var1x, var2x) -> RuleInputSpaceExplorer.b(var20, var1x, var2x));
         return var20;
      }
   }

   private String a(Map<String, Object> var1, String var2) {
      String var3 = null;
      int var4 = Integer.MAX_VALUE;

      for (Entry var6 : var1.entrySet()) {
         if (var2.equals(a(var6.getValue()))) {
            int var7 = this.i.indexOf(var6.getKey());
            var7 = var7 < 0 ? 2147483646 : var7;
            if (var3 == null || var7 < var4) {
               var3 = (String)var6.getKey();
               var4 = var7;
            }
         }
      }

      return var3;
   }

   private static Object a(Object var0, String var1) {
      if (var0 instanceof Number) {
         try {
            return Terms.narrow(Double.parseDouble(var1));
         } catch (NumberFormatException var3) {
            return null;
         }
      } else if (var0 instanceof Boolean) {
         return "true".equals(var1) ? Boolean.TRUE : ("false".equals(var1) ? Boolean.FALSE : null);
      } else {
         return var1;
      }
   }

   private static String a(Object var0) {
      if (var0 instanceof Number var1) {
         return Terms.numberToString(var1);
      } else {
         return !(var0 instanceof String) && !(var0 instanceof Boolean) ? null : String.valueOf(var0);
      }
   }

   private static void a(Object var0, String var1, Object var2) {
      if (var0 instanceof Map var3) {
         for (Entry var5 : (Iterable<Entry>) (Iterable<?>) (var3.entrySet())) {
            String var6 = var1.isEmpty() ? String.valueOf(var5.getKey()) : var1 + "." + var5.getKey();
            if (var5.getValue() instanceof Map) {
               a(var5.getValue(), var6, var2);
            } else if (var2 instanceof Map var7) {
               var7.put(var6, var5.getValue());
            } else {
               ((List)var2).add(var6);
            }
         }
      }
   }

   private void a(String var1) {
      while (!this.n.contains(var1)) {
         while (this.o == null || !this.o.hasNext()) {
            if (!this.p.hasNext()) {
               return;
            }

            this.o = this.p.next().getValue().get().iterator();
         }

         var var2 = this.o.next();
         if (!this.m.containsKey(io.karatelabs.ext.rules.twin.CanonicalJson.c(var2))) {
            if (this.q >= this.h) {
               this.r = true;
               return;
            }

            this.a(var2, this.j.size() + this.q++);
         }
      }
   }

   private Map<String, Object> a(int var1, Set<String> var2) {
      LinkedHashMap<java.lang.String, java.lang.Object> var3 = new LinkedHashMap();
      var3.put("judgedOn", this.d.b());
      if (this.d.c() != null) {
         var3.put("figures", this.d.c());
      }

      var3.put("target", this.f);
      var3.put("rows", var1);
      var3.put("targets", this.k.size());
      ArrayList<java.util.LinkedHashMap> var4 = new ArrayList();
      LinkedHashMap<java.lang.String, java.lang.String> var5 = new LinkedHashMap();

      for (String var7 : this.k) {
         if (var2.contains(var7)) {
            var5.put(var7, "exercised");
         } else {
            io.karatelabs.ext.rules.RuleTableCatalog.TableCellReference var8 = this.d.a(var7);
            io.karatelabs.ext.rules.CellCoverageAnalyzer.AccessDenial var9 = this.d.a(var8, a);
            LinkedHashMap<java.lang.String, java.lang.Object> var10 = new LinkedHashMap();
            var10.put("address", var7);
            var10.putAll(var8.b());
            var10.put("status", var9 != null ? "unreachable" : "notfound");
            if (var9 != null) {
               var10.put("route", var9.a());
               var10.put("missing", var9.b());
            }

            var5.put(var7, (String)var10.get("status"));
            var4.add(var10);
         }
      }

      var3.putAll(a(var5.values()));
      LinkedHashMap<java.lang.String, java.util.Map<java.lang.String,java.lang.Object>> var13 = new LinkedHashMap();

      for (String var16 : this.e.b().keySet()) {
         ArrayList<String> var18 = new ArrayList<>();
         var5.forEach((var3x, var4x) -> {
            if (this.d.a(var3x).c().equals(var16)) {
               var18.add(var4x);
            }
         });
         if (!var18.isEmpty()) {
            var13.put(var16, a((Collection<String>)var18));
         }
      }

      var3.put("tables", var13);
      LinkedHashMap<java.lang.String, java.lang.Object> var15 = new LinkedHashMap();
      ArrayList<java.util.LinkedHashMap> var17 = new ArrayList();
      int var19 = 0;

      for (io.karatelabs.ext.rules.CellCoverageAnalyzer.RowCoverageResult var11 : this.j) {
         if (var11.a()) {
            var19++;
         } else {
            LinkedHashMap<java.lang.String, java.lang.Object> var12 = new LinkedHashMap();
            var12.put("row", var11.b());
            var12.put("reason", var11.d());
            var17.add(var12);
         }
      }

      var15.put("evaluated", var19);
      var15.put("excluded", var17);
      var3.put("saved", var15);
      LinkedHashMap<java.lang.String, java.lang.Object> var21 = new LinkedHashMap();
      var21.put("rows", this.q);
      var21.put("budget", this.h);
      var21.put("truncated", this.r);
      var21.put("maxBoxes", a.a());
      var3.put("search", var21);
      var3.put("cells", var4);
      return var3;
   }

   private static Map<String, Object> a(Collection<String> var0) {
      LinkedHashMap<java.lang.String, java.lang.Object> var1 = new LinkedHashMap();
      var1.put("of", var0.size());

      for (String var3 : List.of("exercised", "unreachable", "notfound")) {
         var1.put(var3, (int)var0.stream().filter(var3::equals).count());
      }

      return var1;
   }

   private record SensitivityCase(Map<String, Object> a, String b, int c, Set<String> d, Map<String, io.karatelabs.ext.rules.CellCoverageAnalyzer.CellProbeResult> e) {

      private SensitivityCase(Map<String, Object> a, String b, int c, Set<String> d, Map<String, io.karatelabs.ext.rules.CellCoverageAnalyzer.CellProbeResult> e) {
         this.a = a;
         this.b = b;
         this.c = c;
         this.d = d;
         this.e = e;
      }
   }
}
