package io.karatelabs.ext.rules;

import io.karatelabs.common.Json;
import io.karatelabs.js.Engine;
import io.karatelabs.js.Terms;
import io.karatelabs.parser.Node;
import io.karatelabs.parser.NodeType;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.Map.Entry;
import java.util.function.Function;

public final class CellCoverageAnalyzer implements io.karatelabs.ext.rules.analysis.AbstractInterpreter.AbstractExecutionListener {
   public static final String a = "exercised";
   public static final String b = "masked";
   public static final String c = "notChecked";
   public static final String d = "unread";
   public static final String e = "unreachable";
   public static final String f = "notfound";
   static final List<String> g = List.of("exercised", "masked", "notChecked", "unread");
   private final io.karatelabs.ext.rules.RulebookRepository.RulebookDocument j;
   private final RuleEvaluator k;
   private final RuleTableCatalog l;
   private final Map<String, io.karatelabs.ext.rules.RuleTableCatalog.TableCellReference> m = new LinkedHashMap<>();
   private final Map<String, String> n = new HashMap<>();
   private Function<Map<String, Object>, List<Map<String, Object>>> o;
   private Map<String, List<io.karatelabs.ext.rules.CellCoverageAnalyzer.AccessPath>> p;
   private Map<String, List<io.karatelabs.ext.rules.CellCoverageAnalyzer.AccessTraceStep>> q;
   private Boolean r;
   private IdentityHashMap<Map<String, io.karatelabs.ext.rules.analysis.AbstractValue>, io.karatelabs.ext.rules.CellCoverageAnalyzer.LookupAccessKey> s;
   private final Map<io.karatelabs.ext.rules.CellCoverageAnalyzer.LookupAccessKey, Set<String>> t = new HashMap<>();
   private final Set<String> u = new HashSet<>();

   public CellCoverageAnalyzer(io.karatelabs.ext.rules.RulebookRepository.RulebookDocument var1, RuleEvaluator var2) {
      this.j = var1;
      this.k = var2;
      this.l = var1.m();

      for (io.karatelabs.ext.rules.RuleTableCatalog.TableCellReference var4 : this.l.f()) {
         if (var4.h() instanceof Number) {
            this.m.put(var4.a(), var4);
         }
      }
   }

   public List<io.karatelabs.ext.rules.RuleTableCatalog.TableCellReference> a() {
      return List.copyOf(this.m.values());
   }

   public io.karatelabs.ext.rules.RuleTableCatalog.TableCellReference a(String var1) {
      io.karatelabs.ext.rules.RuleTableCatalog.TableCellReference var2 = this.l.a() ? null : this.l.d(var1);
      return var2 == null ? null : this.m.get(var2.a());
   }

   public String b() {
      return this.j.o() == null ? "output" : "figures";
   }

   public List<String> c() {
      return this.j.o();
   }

   public List<io.karatelabs.ext.rules.CellCoverageAnalyzer.RowCoverageResult> a(List<Map<String, Object>> var1, Set<String> var2) {
      ArrayList<io.karatelabs.ext.rules.CellCoverageAnalyzer.RowCoverageResult> var3 = new ArrayList();

      for (int var4 = 0; var4 < var1.size(); var4++) {
         Map<java.lang.String, Object> var5 = (Map)var1.get(var4);
         var3.add(this.a(var5.get("_id") != null ? var5.get("_id") : var4, var5, var2));
      }

      return var3;
   }

   public io.karatelabs.ext.rules.CellCoverageAnalyzer.RowCoverageResult a(Object var1, Map<String, Object> var2, Set<String> var3) {
      var var4 = RulebookRepository.d(var2);
      if (!Schema.validateAgainst(this.j.e(), var4).pass) {
         return new io.karatelabs.ext.rules.CellCoverageAnalyzer.RowCoverageResult(var1, var4, "schema-invalid", Map.of(), Map.of());
      } else {
         var var5 = this.d().apply(var4);
         if (!var5.isEmpty()) {
            return new io.karatelabs.ext.rules.CellCoverageAnalyzer.RowCoverageResult(var1, var4, "outside-domain: " + Json.stringifyStrict(var5), Map.of(), Map.of());
         } else {
            LinkedHashMap<String, Set<String>> var6 = new LinkedHashMap<>();

            Object var7;
            try {
               var7 = this.k.a(this.j.g(), this.l.b(var1x -> {
                  Set<java.lang.String> var2x = var6.computeIfAbsent(var1x.a().a(), var0x -> new TreeSet());
                  if (var1x.b() != null) {
                     var2x.add(var1x.b());
                  }
               }), var4, null).b();
            } catch (RuntimeException var12) {
               return new io.karatelabs.ext.rules.CellCoverageAnalyzer.RowCoverageResult(var1, var4, "execution-failed: " + a(var12), Map.of(), Map.of());
            }

            LinkedHashMap<java.lang.String, io.karatelabs.ext.rules.CellCoverageAnalyzer.CellProbeResult> var8 = new LinkedHashMap();

            for (String var10 : (Iterable<String>) (Iterable<?>) (var6.keySet())) {
               io.karatelabs.ext.rules.RuleTableCatalog.TableCellReference var11 = this.m.get(var10);
               if (var11 != null && (var3 == null || var3.contains(var10))) {
                  var8.put(var10, this.a(var1, var11, var4, var7));
               }
            }

            return new io.karatelabs.ext.rules.CellCoverageAnalyzer.RowCoverageResult(var1, var4, null, Collections.unmodifiableMap(var6), Collections.unmodifiableMap(var8));
         }
      }
   }

   private Function<Map<String, Object>, List<Map<String, Object>>> d() {
      if (this.o == null) {
         this.o = RuleInputSpaceExplorer.a(this.j.g(), this.l.e(), this.j.e(), this.j.j());
      }

      return this.o;
   }

   private io.karatelabs.ext.rules.CellCoverageAnalyzer.CellProbeResult a(Object var1, io.karatelabs.ext.rules.RuleTableCatalog.TableCellReference var2, Map<String, Object> var3, Object var4) {
      double var5 = ((Number)var2.h()).doubleValue();
      Number var7 = Terms.narrow(var5 == 0.0 ? 1.0 : var5 * 2.0);
      String var8 = this.n.computeIfAbsent(var2.a(), var3x -> {
         try {
            this.l.b(var2, var7);
            return "";
         } catch (RuntimeException var5x) {
            return a(var5x);
         }
      });
      if (!var8.isEmpty()) {
         return new io.karatelabs.ext.rules.CellCoverageAnalyzer.CellProbeResult(var1, var7, null, null, "notChecked", "invalid-perturbation", var8);
      } else {
         Object var9;
         try {
            var9 = this.k.a(this.j.g(), this.l.d(var2, var7), var3, null).b();
         } catch (RuntimeException var20) {
            return new io.karatelabs.ext.rules.CellCoverageAnalyzer.CellProbeResult(var1, var7, null, null, "notChecked", "execution-failed", a(var20));
         }

         if (var4 instanceof Map var10 && var9 instanceof Map var11) {
            Object var12 = this.j.o();
            if (var12 == null) {
               LinkedHashSet<java.lang.String> var13 = new LinkedHashSet();
               var10.keySet().forEach(var1x -> var13.add(String.valueOf(var1x)));
               var11.keySet().forEach(var1x -> var13.add(String.valueOf(var1x)));
               var12 = new ArrayList(var13);
            }

            LinkedHashMap<java.lang.String, java.lang.Object> var21 = new LinkedHashMap();
            LinkedHashMap<java.lang.String, java.lang.Object> var14 = new LinkedHashMap();
            boolean var15 = false;

            for (String var17 : (Iterable<String>) (Iterable<?>) (var12)) {
               Object var18 = var10.get(var17);
               Object var19 = var11.get(var17);
               if (var18 == null || var19 == null) {
                  return new io.karatelabs.ext.rules.CellCoverageAnalyzer.CellProbeResult(var1, var7, null, null, "notChecked", "missing-figure", "'" + var17 + "' is not on both outputs");
               }

               var21.put(var17, var18);
               var14.put(var17, var19);
               var15 |= !a(var18, var19);
            }

            return new io.karatelabs.ext.rules.CellCoverageAnalyzer.CellProbeResult(var1, var7, var21, var14, var15 ? "exercised" : "masked", null, null);
         } else {
            return new io.karatelabs.ext.rules.CellCoverageAnalyzer.CellProbeResult(var1, var7, null, null, "notChecked", "missing-figure", "the output is not an object");
         }
      }
   }

   private static boolean a(Object var0, Object var1) {
      return var0 instanceof Number var2 && var1 instanceof Number var3
         ? var2.doubleValue() == var3.doubleValue()
         : Json.stringifyStrict(var0).equals(Json.stringifyStrict(var1));
   }

   private static String a(RuntimeException var0) {
      return var0.getMessage() == null ? var0.getClass().getSimpleName() : var0.getMessage();
   }

   public Map<String, io.karatelabs.ext.rules.CellCoverageAnalyzer.CellCoverage> a(List<io.karatelabs.ext.rules.CellCoverageAnalyzer.RowCoverageResult> var1, Collection<io.karatelabs.ext.rules.RuleTableCatalog.TableCellReference> var2) {
      LinkedHashMap<java.lang.String, io.karatelabs.ext.rules.CellCoverageAnalyzer.CellCoverage> var3 = new LinkedHashMap();

      for (io.karatelabs.ext.rules.RuleTableCatalog.TableCellReference var5 : var2) {
         io.karatelabs.ext.rules.CellCoverageAnalyzer.CellProbeResult var6 = null;
         boolean var7 = false;
         TreeSet var8 = new TreeSet();

         for (io.karatelabs.ext.rules.CellCoverageAnalyzer.RowCoverageResult var10 : var1) {
            Set var11 = var10.a() ? var10.e().get(var5.a()) : null;
            if (var11 != null) {
               var7 = true;
               var8.addAll(var11);
               io.karatelabs.ext.rules.CellCoverageAnalyzer.CellProbeResult var12 = var10.f().get(var5.a());
               if (var12 != null && (var6 == null || g.indexOf(var12.f()) < g.indexOf(var6.f()))) {
                  var6 = var12;
               }
            }
         }

         String var13 = var6 != null ? var6.f() : (var7 ? "notChecked" : "unread");
         String var14 = var6 != null ? var6.g() : (var7 ? "not-probed" : null);
         var3.put(var5.a(), new io.karatelabs.ext.rules.CellCoverageAnalyzer.CellCoverage(var5, var13, var6, var14, var8));
      }

      return var3;
   }

   // $VF: One or more variable merging failures!
   // $VF: Could not properly define all variable types!
   // Please report this to the Vineflower issue tracker, at https://github.com/Vineflower/vineflower/issues with a copy of the class file (if you have the rights to distribute it!)
   public Map<String, Object> a(Object var1, Object var2, List<Map<String, Object>> var3, int var4) {
      ArrayList<io.karatelabs.ext.rules.RuleTableCatalog.TableCellReference> var5 = null;
      LinkedHashSet<java.lang.String> var6 = null;
      if (var1 instanceof List var7) {
         var5 = new ArrayList();
         var6 = new LinkedHashSet();

         for (Object var9 : var7) {
            io.karatelabs.ext.rules.RuleTableCatalog.TableCellReference var10 = this.a(String.valueOf(var9));
            if (var10 == null) {
               throw this.a(var9);
            }

            var5.add(var10);
            var6.add(var10.a());
         }
      } else if (var1 != null && !"all".equals(var1) && !"unexercised".equals(var1)) {
         throw new RuleApiException(400, "INVALID_INPUT", "target is 'all', 'unexercised' or a list of cell addresses, not " + var1);
      }

      var var17 = this.a(var3, var6);
      Map<java.lang.String, io.karatelabs.ext.rules.CellCoverageAnalyzer.CellCoverage> var18 = this.a(var17, (Collection<io.karatelabs.ext.rules.RuleTableCatalog.TableCellReference>)(var5 != null ? var5 : this.m.values()));
      ArrayList<io.karatelabs.ext.rules.CellCoverageAnalyzer.CellCoverage> var19 = new ArrayList();

      for (io.karatelabs.ext.rules.CellCoverageAnalyzer.CellCoverage var11 : (Iterable<io.karatelabs.ext.rules.CellCoverageAnalyzer.CellCoverage>) (Iterable<?>) (var18.values())) {
         if (!"unexercised".equals(var1) || "unread".equals(var11.c()) || "masked".equals(var11.c())) {
            var19.add(var11);
         }
      }

      Map<java.lang.String, java.lang.Object> var21 = this.a(var1 == null ? "all" : var1, var17, var19, var5 == null);
      if (var2 instanceof Map var22) {
         int var12 = Math.max(0, Math.min(var22.get("offset") instanceof Number var13 ? var13.intValue() : 0, var19.size()));
         int var24 = var22.get("limit") instanceof Number var26 ? var26.intValue() : 0;
         int var27 = Math.max(0, Math.min(var24 <= 0 ? var4 : var24, var4));
         int var30 = Math.min(var19.size(), var12 + var27);
         var21.put("offset", var12);
         var21.put("limit", var27);
         var21.put("cells", var19.subList(var12, var30).stream().map(io.karatelabs.ext.rules.CellCoverageAnalyzer.CellCoverage::a).toList());
         var21.put("truncated", var30 < var19.size());
         if (var30 < var19.size()) {
            var21.put("next", var30);
         }
      } else if (var2 != null) {
         ArrayList<java.util.Map<java.lang.String,java.lang.Object>> var23 = new ArrayList();

         for (Object var29 : var2 instanceof List<?> values ? values : List.of(var2)) {
            io.karatelabs.ext.rules.RuleTableCatalog.TableCellReference var31 = this.a(String.valueOf(var29));
            io.karatelabs.ext.rules.CellCoverageAnalyzer.CellCoverage var16 = var31 == null ? null : (io.karatelabs.ext.rules.CellCoverageAnalyzer.CellCoverage)var18.get(var31.a());
            if (var16 == null) {
               throw this.a(var29);
            }

            var23.add(var16.a());
         }

         var21.put("cells", var23);
      }

      return var21;
   }

   public Map<String, Object> a(List<io.karatelabs.ext.rules.CellCoverageAnalyzer.RowCoverageResult> var1, Map<String, io.karatelabs.ext.rules.CellCoverageAnalyzer.CellCoverage> var2, int var3) {
      var var4 = this.a("all", var1, new ArrayList<>(var2.values()), true);
      ArrayList<java.util.Map<java.lang.String,java.lang.Object>> var5 = new ArrayList();
      int var6 = 0;
      var var7 = new ArrayList<>(var2.values());
      var7.sort(Comparator.comparingInt(var0 -> g.indexOf(var0.c())));

      for (io.karatelabs.ext.rules.CellCoverageAnalyzer.CellCoverage var9 : (Iterable<io.karatelabs.ext.rules.CellCoverageAnalyzer.CellCoverage>) (Iterable<?>) (var7)) {
         if (!"exercised".equals(var9.c()) && var6++ < var3) {
            var var10 = var9.a();
            if ("unread".equals(var9.c())) {
               io.karatelabs.ext.rules.CellCoverageAnalyzer.AccessDenial var11 = this.a(var9.b());
               if (var11 != null) {
                  var10.put("status", "unreachable");
                  var10.put("missing", var11.b());
                  var10.put("route", var11.a());
               }
            }

            var5.add(var10);
         }
      }

      var4.put("cells", var5);
      var4.put("truncated", var6 > var5.size());
      return var4;
   }

   private Map<String, Object> a(Object var1, List<io.karatelabs.ext.rules.CellCoverageAnalyzer.RowCoverageResult> var2, List<io.karatelabs.ext.rules.CellCoverageAnalyzer.CellCoverage> var3, boolean var4) {
      LinkedHashMap<java.lang.String, java.lang.Object> var5 = new LinkedHashMap();
      var5.put("judgedOn", this.b());
      if (this.j.o() != null) {
         var5.put("figures", this.j.o());
      }

      var5.put("target", var1);
      LinkedHashMap<java.lang.String, java.lang.Object> var6 = new LinkedHashMap();
      ArrayList<java.util.LinkedHashMap> var7 = new ArrayList();
      int var8 = 0;

      for (io.karatelabs.ext.rules.CellCoverageAnalyzer.RowCoverageResult var10 : var2) {
         if (var10.a()) {
            var8++;
         } else {
            LinkedHashMap<java.lang.String, java.lang.Object> var11 = new LinkedHashMap();
            var11.put("row", var10.b());
            var11.put("reason", var10.d());
            var7.add(var11);
         }
      }

      var6.put("evaluated", var8);
      var6.put("excluded", var7);
      var5.put("rows", var6);
      var5.putAll(a(var3));
      LinkedHashMap<java.lang.String, java.util.Map<java.lang.String,java.lang.Object>> var13 = new LinkedHashMap();

      for (String var15 : this.l.b().keySet()) {
         var var12 = var3.stream().filter(var1x -> var1x.b().c().equals(var15)).toList();
         if (!var12.isEmpty() || var4) {
            var13.put(var15, a(var12));
         }
      }

      var5.put("tables", var13);
      return var5;
   }

   private RuleApiException a(Object var1) {
      return new RuleApiException(404, "LOOKUP_PATH_NOT_FOUND", "'" + var1 + "' names no numeric file-table cell (tables: " + this.l.b().keySet() + ")");
   }

   private static Map<String, Object> a(List<io.karatelabs.ext.rules.CellCoverageAnalyzer.CellCoverage> var0) {
      LinkedHashMap<java.lang.String, java.lang.Object> var1 = new LinkedHashMap();
      var1.put("of", var0.size());

      for (String var3 : g) {
         var1.put(var3, (int)var0.stream().filter(var1x -> var3.equals(var1x.c())).count());
      }

      return var1;
   }

   static void a(List<String> var0, String var1) {
      if (var0 != null) {
         Set var2;
         try {
            var2 = b(var1);
         } catch (RuntimeException var5) {
            return;
         }

         for (String var4 : var0) {
            if (!var2.isEmpty() && !var2.contains(var4)) {
               throw new RuleApiException(400, "FIGURE_UNKNOWN", "schema.js figures names '" + var4 + "', which calc.js never sets on calc.output (fields: " + var2 + ")");
            }
         }
      }
   }

   static Set<String> b(String var0) {
      LinkedHashSet<String> var1 = new LinkedHashSet<>();
      Node var2 = Engine.parse(var0);
      HashMap<java.lang.String, io.karatelabs.parser.Node> var3 = new HashMap();

      for (Node var5 : var2.findAll(NodeType.VAR_DECL)) {
         if (var5.size() > 2 && a(var5.get(2)) != null) {
            var3.put(var5.getFirst().getText(), a(var5.get(2)));
         }
      }

      for (Node var13 : var2.findAll(NodeType.ASSIGN_EXPR)) {
         Node var6 = var13.get(0);
         if (var6.type == NodeType.REF_DOT_EXPR && var6.getText().equals("calc.output")) {
            Node var14 = c(var13.get(2));
            Node var8 = a(var14);
            if (var8 == null && var14.type == NodeType.REF_EXPR) {
               var8 = (Node)var3.get(var14.getText());
            }

            if (var8 != null) {
               for (int var9 = 1; var9 < var8.size() - 1; var9++) {
                  Node var10 = var8.get(var9).getFirst();
                  Object var11 = var10.isToken() ? Terms.literalValue(var10.token) : null;
                  var1.add(var11 == null ? var10.getText() : String.valueOf(var11));
               }
            }
         } else if ((var6.type == NodeType.REF_DOT_EXPR || var6.type == NodeType.REF_BRACKET_EXPR) && var6.get(0).getText().equals("calc.output")) {
            String var7 = b(var6);
            if (var7 != null) {
               var1.add(var7);
            }
         }
      }

      return var1;
   }

   private static Node a(Node var0) {
      var0 = c(var0);
      return var0.type == NodeType.LIT_EXPR && var0.getFirst().type == NodeType.LIT_OBJECT ? var0.getFirst() : null;
   }

   public io.karatelabs.ext.rules.CellCoverageAnalyzer.AccessDenial a(io.karatelabs.ext.rules.RuleTableCatalog.TableCellReference var1) {
      return this.a(var1, io.karatelabs.ext.rules.analysis.AbstractInterpreter.AnalysisBudget.a);
   }

   public io.karatelabs.ext.rules.CellCoverageAnalyzer.AccessDenial a(io.karatelabs.ext.rules.RuleTableCatalog.TableCellReference var1, io.karatelabs.ext.rules.analysis.AbstractInterpreter.AnalysisBudget var2) {
      io.karatelabs.ext.rules.CellCoverageAnalyzer.AccessDenial var3 = this.b(var1);
      if (var3 == null) {
         var3 = this.c(var1);
      }

      return var3 != null ? var3 : this.b(var1, var2);
   }

   public io.karatelabs.ext.rules.CellCoverageAnalyzer.AccessDenial b(io.karatelabs.ext.rules.RuleTableCatalog.TableCellReference var1) {
      var var2 = this.e().get(var1.c());
      if (var2 == null) {
         return null;
      } else {
         io.karatelabs.ext.rules.RuleTableCatalog.TableDefinition var3 = this.l.b().get(var1.c());
         if (var2.isEmpty()) {
            return new io.karatelabs.ext.rules.CellCoverageAnalyzer.AccessDenial("static", "no access route reads " + var3.a());
         } else {
            String var4 = null;

            for (String var6 : this.l.a(var1)) {
               var var7 = a(var3, var6, var1);
               String var8 = null;

               for (io.karatelabs.ext.rules.CellCoverageAnalyzer.AccessPath var10 : (Iterable<io.karatelabs.ext.rules.CellCoverageAnalyzer.AccessPath>) (Iterable<?>) (var2)) {
                  String var11 = null;

                  for (int var12 = 0; var12 < var7.size() && var11 == null; var12++) {
                     io.karatelabs.ext.rules.CellCoverageAnalyzer.AccessTraceStep var13 = var10.a().get(var12);
                     if (var13.a((String)var7.get(var12))) {
                        var11 = (var13.b() != null ? var13.b() : a(var3, var12)) + " ∌ '" + (String)var7.get(var12) + "'";
                        if (var13.b() != null) {
                           var8 = var11;
                        }
                     }
                  }

                  if (var11 == null) {
                     return null;
                  }

                  if (var8 == null) {
                     var8 = var11;
                  }
               }

               if (var4 == null) {
                  var4 = var8;
               }
            }

            return var4 == null ? null : new io.karatelabs.ext.rules.CellCoverageAnalyzer.AccessDenial("static", var4);
         }
      }
   }

   public io.karatelabs.ext.rules.CellCoverageAnalyzer.AccessDenial c(io.karatelabs.ext.rules.RuleTableCatalog.TableCellReference var1) {
      io.karatelabs.ext.rules.RuleTableCatalog.TableDefinition var2 = this.l.b().get(var1.c());
      if (var2.d() != null && var1.e() == null) {
         this.e();
         var var3 = this.q.get(var2.a());
         if (var3 != null && !var3.isEmpty()) {
            TreeSet<java.lang.String> var4 = new TreeSet();
            LinkedHashSet<java.lang.String> var5 = new LinkedHashSet();

            for (io.karatelabs.ext.rules.CellCoverageAnalyzer.AccessTraceStep var7 : (Iterable<io.karatelabs.ext.rules.CellCoverageAnalyzer.AccessTraceStep>) (Iterable<?>) (var3)) {
               if (var7.a() != null) {
                  var4.add(var7.a());
               } else {
                  var4.addAll(var7.c());
                  var5.add(var7.b());
               }
            }

            String var10 = String.join("/", var1.f());

            for (String var8 : (Iterable<String>) (Iterable<?>) (var4)) {
               String var9 = var2.h().get(var8 + "/" + var10);
               if (var9 == null || var9.equals(var1.d())) {
                  return null;
               }
            }

            return new io.karatelabs.ext.rules.CellCoverageAnalyzer.AccessDenial(
               "shadow",
               "shadowed: every jurisdiction in "
                  + (var5.isEmpty() ? "jurisdiction" : String.join(" | ", var5))
                  + " ∈ {"
                  + String.join(",", var4)
                  + "} overrides it"
            );
         } else {
            return null;
         }
      } else {
         return null;
      }
   }

   public io.karatelabs.ext.rules.CellCoverageAnalyzer.AccessDenial b(io.karatelabs.ext.rules.RuleTableCatalog.TableCellReference var1, io.karatelabs.ext.rules.analysis.AbstractInterpreter.AnalysisBudget var2) {
      if (this.r == null) {
         this.a(var2);
      }

      if (this.r && !this.u.contains(var1.a())) {
         io.karatelabs.ext.rules.RuleTableCatalog.TableDefinition var3 = this.l.b().get(var1.c());
         String var4 = null;

         for (String var6 : this.l.a(var1)) {
            var var7 = a(var3, var6, var1);
            String var8 = null;

            for (int var9 = 0; var9 < var7.size() && var8 == null; var9++) {
               var var10 = this.t.getOrDefault(new io.karatelabs.ext.rules.CellCoverageAnalyzer.LookupAccessKey(var3.a(), var7.subList(0, var9)), Set.of());
               if (!var10.contains(var7.get(var9))) {
                  var8 = a(var3, var9) + " ∌ '" + (String)var7.get(var9) + "'";
               }
            }

            if (var8 == null) {
               return null;
            }

            if (var4 == null) {
               var4 = var8;
            }
         }

         return var4 == null ? null : new io.karatelabs.ext.rules.CellCoverageAnalyzer.AccessDenial("walk", var4);
      } else {
         return null;
      }
   }

   private void a(io.karatelabs.ext.rules.analysis.AbstractInterpreter.AnalysisBudget var1) {
      this.r = false;

      try {
         io.karatelabs.ext.rules.analysis.AbstractInterpreter.AbstractInterpretationResult var2 = io.karatelabs.ext.rules.analysis.AbstractInterpreter.a(
            this.j.g(), this.l.e(), RuleInputSpaceCertificateBuilder.a(this.j.e()), var1, io.karatelabs.ext.rules.analysis.SplitTreeListener.b, this
         );
         this.r = !var2.n() && var2.o() == 0L;
      } catch (io.karatelabs.ext.rules.analysis.RuleAnalysisException var3) {
      }
   }

   @Override
   public void a(Map<String, io.karatelabs.ext.rules.analysis.AbstractValue> var1) {
      this.s = new IdentityHashMap<>();

      for (io.karatelabs.ext.rules.RuleTableCatalog.TableDefinition var3 : this.l.b().values()) {
         io.karatelabs.ext.rules.analysis.AbstractValue var4 = null;

         for (String var8 : var3.a().split("\\.")) {
            var4 = var4 == null
               ? (io.karatelabs.ext.rules.analysis.AbstractValue)var1.get(var8)
               : (var4 instanceof io.karatelabs.ext.rules.analysis.AbstractValue.ObjectAbstractValue var9 ? var9.b().get(var8) : null);
         }

         if (var4 instanceof io.karatelabs.ext.rules.analysis.AbstractValue.ObjectAbstractValue var10) {
            this.a(var10, var3.a(), List.<String>of(), (var3.d() != null ? 1 : 0) + var3.b().size());
         }
      }
   }

   private void a(io.karatelabs.ext.rules.analysis.AbstractValue.ObjectAbstractValue var1, String var2, List<String> var3, int var4) {
      if (var3.size() < var4) {
         this.s.put(var1.b(), new io.karatelabs.ext.rules.CellCoverageAnalyzer.LookupAccessKey(var2, var3));

         for (Entry var6 : var1.b().entrySet()) {
            if (var6.getValue() instanceof io.karatelabs.ext.rules.analysis.AbstractValue.ObjectAbstractValue var7) {
               var var9 = new ArrayList<>(var3);
               var9.add((String)var6.getKey());
               this.a(var7, var2, List.copyOf(var9), var4);
            }
         }
      }
   }

   @Override
   public void selected(LinkedHashMap<String, io.karatelabs.ext.rules.analysis.AbstractValue> var1, List<String> var2) {
      io.karatelabs.ext.rules.CellCoverageAnalyzer.LookupAccessKey var3 = this.s.get(var1);
      if (var3 != null) {
         this.t.computeIfAbsent(var3, var0 -> new HashSet<>()).addAll(var2);
      }
   }

   @Override
   public void b(Map<String, Object> var1) {
      try {
         io.karatelabs.ext.rules.CalcExecutor.a(this.j.g(), this.l.a(this.u::add), var1, null);
      } catch (RuntimeException var3) {
      }
   }

   private static List<String> a(io.karatelabs.ext.rules.RuleTableCatalog.TableDefinition var0, String var1, io.karatelabs.ext.rules.RuleTableCatalog.TableCellReference var2) {
      if (var1 == null) {
         return var2.f();
      } else {
         ArrayList<java.lang.String> var3 = new ArrayList();
         var3.add(var1);
         var3.addAll(var2.f());
         return var3;
      }
   }

   private static String a(io.karatelabs.ext.rules.RuleTableCatalog.TableDefinition var0, int var1) {
      if (var0.d() != null) {
         return var1 == 0 ? "jurisdiction" : var0.b().get(var1 - 1);
      } else {
         return var0.b().get(var1);
      }
   }

   private Map<String, List<io.karatelabs.ext.rules.CellCoverageAnalyzer.AccessPath>> e() {
      if (this.p != null) {
         return this.p;
      } else {
         LinkedHashMap<java.lang.String, java.util.List<io.karatelabs.ext.rules.CellCoverageAnalyzer.AccessPath>> var1 = new LinkedHashMap();
         HashMap<String, List<io.karatelabs.ext.rules.CellCoverageAnalyzer.AccessTraceStep>> var2 = new HashMap<>();

         for (io.karatelabs.ext.rules.RuleTableCatalog.TableDefinition var4 : this.l.b().values()) {
            var1.put(var4.a(), new ArrayList());
            if (var4.d() != null) {
               var2.put(var4.a(), new ArrayList());
            }
         }

         Node var15 = Engine.parse(this.j.g());
         var var16 = e(var15);

         for (Node var6 : var15.findAll(NodeType.REF_EXPR)) {
            if (var6.getText().equals("lookup")) {
               ArrayList<io.karatelabs.parser.Node> var7 = new ArrayList();
               Node var8 = var6;

               for (Node var9 = var6.getParent();
                  var9 != null && (var9.type == NodeType.REF_DOT_EXPR || var9.type == NodeType.REF_BRACKET_EXPR) && var9.get(0) == var8;
                  var9 = var9.getParent()
               ) {
                  var7.add(var9);
                  var8 = var9;
               }

               String var17 = "";
               int var18 = 0;

               io.karatelabs.ext.rules.RuleTableCatalog.TableDefinition var10;
               for (var10 = null; var18 < var7.size() && var10 == null; var18++) {
                  String var11 = b((Node)var7.get(var18));
                  if (var11 == null) {
                     break;
                  }

                  var17 = var17.isEmpty() ? var11 : var17 + "." + var11;
                  var10 = this.l.b().get(var17);
               }

               if (var10 == null) {
                  for (String var21 : this.l.b().keySet()) {
                     if (var17.isEmpty() || var21.startsWith(var17 + ".")) {
                        var1.remove(var21);
                        var2.remove(var21);
                     }
                  }
               } else {
                  int var19 = (var10.d() != null ? 1 : 0) + var10.b().size();
                  ArrayList<io.karatelabs.ext.rules.CellCoverageAnalyzer.AccessTraceStep> var12 = new ArrayList();

                  for (int var13 = 0; var13 < var19 && var12 != null; var13++) {
                     io.karatelabs.ext.rules.CellCoverageAnalyzer.AccessTraceStep var14 = var18 + var13 < var7.size() ? this.a((Node)var7.get(var18 + var13), var16) : null;
                     if (var14 == null) {
                        var12 = null;
                     } else {
                        var12.add(var14);
                     }
                  }

                  if (var12 == null) {
                     var1.remove(var10.a());
                  } else if (var1.containsKey(var10.a())) {
                     ((List)var1.get(var10.a())).add(new io.karatelabs.ext.rules.CellCoverageAnalyzer.AccessPath(var12));
                  }

                  if (var2.containsKey(var10.a())) {
                     io.karatelabs.ext.rules.CellCoverageAnalyzer.AccessTraceStep var22 = var18 < var7.size() ? this.a((Node)var7.get(var18), var16) : null;
                     if (var22 == null) {
                        var2.remove(var10.a());
                     } else {
                        ((List)var2.get(var10.a())).add(var22);
                     }
                  }
               }
            }
         }

         this.q = var2;
         this.p = var1;
         return var1;
      }
   }

   private io.karatelabs.ext.rules.CellCoverageAnalyzer.AccessTraceStep a(Node var1, Map<String, List<String>> var2) {
      String var3 = b(var1);
      if (var3 != null) {
         return new io.karatelabs.ext.rules.CellCoverageAnalyzer.AccessTraceStep(var3, null, null);
      } else if (var1.type != NodeType.REF_BRACKET_EXPR) {
         return null;
      } else {
         var var4 = d(var1.get(2));
         List var5 = var4 == null ? null : b(var4, var2, 0);
         Set var6 = var5 == null ? null : a(this.j.e(), var5);
         return var6 == null ? null : new io.karatelabs.ext.rules.CellCoverageAnalyzer.AccessTraceStep(null, String.join(".", var5), var6);
      }
   }

   private static String b(Node var0) {
      if (var0.type == NodeType.REF_DOT_EXPR) {
         return var0.get(2).getText();
      } else {
         Node var1 = c(var0.get(2));
         if (var1.type == NodeType.LIT_EXPR && var1.getFirst().isToken()) {
            Object var2 = Terms.literalValue(var1.getFirst().token);
            if (var2 instanceof String var4) {
               return var4;
            }

            if (var2 instanceof Number var3) {
               return Terms.numberToString(var3);
            }
         }

         return null;
      }
   }

   private static Node c(Node var0) {
      while (var0.type == NodeType.EXPR || var0.type == NodeType.PAREN_EXPR) {
         var0 = var0.type == NodeType.EXPR ? var0.getFirst() : var0.get(1);
      }

      return var0;
   }

   private static List<String> d(Node var0) {
      var0 = c(var0);
      if (var0.type == NodeType.REF_EXPR) {
         ArrayList<java.lang.String> var4 = new ArrayList();
         var4.add(var0.getText());
         return var4;
      } else if (var0.type != NodeType.REF_DOT_EXPR && var0.type != NodeType.REF_BRACKET_EXPR) {
         return null;
      } else {
         String var1 = b(var0);
         List<java.lang.String> var2 = var1 == null ? null : d(var0.get(0));
         if (var2 != null) {
            var2.add(var1);
         }

         return var2;
      }
   }

   private static List<String> b(List<String> var0, Map<String, List<String>> var1, int var2) {
      if (!((String)var0.get(0)).equals("calc")) {
         List var3 = (List)var1.get(var0.get(0));
         if (var3 != null && var2 <= 8) {
            ArrayList var4 = new ArrayList(var3);
            var4.addAll(var0.subList(1, var0.size()));
            return b(var4, var1, var2 + 1);
         } else {
            return null;
         }
      } else {
         return var0.size() > 2 && ((String)var0.get(1)).equals("input") ? var0.subList(2, var0.size()) : null;
      }
   }

   private static Map<String, List<String>> e(Node var0) {
      HashMap<java.lang.Object, java.lang.Integer> var1 = new HashMap();
      HashMap<java.lang.String, java.util.List<java.lang.String>> var2 = new HashMap();

      for (Node var4 : var0.findAll(NodeType.VAR_DECL)) {
         String var5 = var4.getFirst().getText();
         var1.merge(var5, 1, Integer::sum);
         List var6 = var4.size() > 2 ? d(var4.get(2)) : null;
         if (var6 != null) {
            var2.put(var5, var6);
         }
      }

      for (Node var9 : var0.findAll(NodeType.ASSIGN_EXPR)) {
         if (var9.get(0).type == NodeType.REF_EXPR) {
            var1.merge(var9.get(0).getText(), 1, Integer::sum);
         }
      }

      for (Node var10 : var0.findAll(NodeType.FN_DECL_ARG)) {
         var1.merge(var10.getFirst().getText(), 1, Integer::sum);
      }

      var2.keySet().removeIf(var1x -> (Integer)var1.get(var1x) != 1);
      return var2;
   }

   private static Set<String> a(Object var0, List<String> var1) {
      Object var2 = var0;

      for (String var4 : var1) {
         var2 = var2 instanceof Map var5 ? var5.get(var4) : null;
      }

      if (var2 instanceof List var10 && !var10.isEmpty()) {
         HashSet<java.lang.String> var11 = new HashSet();

         for (Object var6 : var10) {
            if (var6 instanceof String var8 && !var8.startsWith("#")) {
               var11.add(var8);
            } else if (var6 instanceof Number var9) {
               var11.add(Terms.numberToString(var9));
            } else {
               if (!(var6 instanceof Boolean var7)) {
                  return null;
               }

               var11.add(var7.toString());
            }
         }

         return var11;
      } else {
         return null;
      }
   }

   private record LookupAccessKey(String a, List<String> b) {

      private LookupAccessKey(String a, List<String> b) {
         this.a = a;
         this.b = b;
      }
   }

   public record CellProbeResult(Object b, Object c, Map<String, Object> d, Map<String, Object> e, String f, String g, String h) {

      public CellProbeResult(Object b, Object c, Map<String, Object> d, Map<String, Object> e, String f, String g, String h) {
         this.b = b;
         this.c = c;
         this.d = d;
         this.e = e;
         this.f = f;
         this.g = g;
         this.h = h;
      }

      public Map<String, Object> a() {
         LinkedHashMap<java.lang.String, java.lang.Object> var1 = new LinkedHashMap();
         var1.put("row", this.b);
         var1.put("perturbed", this.c);
         if (this.d != null) {
            var1.put("before", this.d);
            var1.put("after", this.e);
         }

         var1.put("status", this.f);
         if (this.g != null) {
            var1.put("reason", this.g);
            var1.put("detail", this.h);
         }

         return var1;
      }

      public Object b() {
         return this.b;
      }

      public Object c() {
         return this.c;
      }

      public Map<String, Object> d() {
         return this.d;
      }

      public Map<String, Object> e() {
         return this.e;
      }

      public String f() {
         return this.f;
      }

      public String g() {
         return this.g;
      }

      public String h() {
         return this.h;
      }
   }

   public record AccessDenial(String a, String b) {

      public AccessDenial(String a, String b) {
         this.a = a;
         this.b = b;
      }
   }

   private record AccessPath(List<io.karatelabs.ext.rules.CellCoverageAnalyzer.AccessTraceStep> a) {

      private AccessPath(List<io.karatelabs.ext.rules.CellCoverageAnalyzer.AccessTraceStep> a) {
         this.a = a;
      }
   }

   public record RowCoverageResult(Object b, Map<String, Object> c, String d, Map<String, Set<String>> e, Map<String, io.karatelabs.ext.rules.CellCoverageAnalyzer.CellProbeResult> f) {

      public RowCoverageResult(Object b, Map<String, Object> c, String d, Map<String, Set<String>> e, Map<String, io.karatelabs.ext.rules.CellCoverageAnalyzer.CellProbeResult> f) {
         this.b = b;
         this.c = c;
         this.d = d;
         this.e = e;
         this.f = f;
      }

      public boolean a() {
         return this.d == null;
      }

      public Object b() {
         return this.b;
      }

      public Map<String, Object> c() {
         return this.c;
      }

      public String d() {
         return this.d;
      }

      public Map<String, Set<String>> e() {
         return this.e;
      }

      public Map<String, io.karatelabs.ext.rules.CellCoverageAnalyzer.CellProbeResult> f() {
         return this.f;
      }
   }

   private record AccessTraceStep(String a, String b, Set<String> c) {

      private AccessTraceStep(String a, String b, Set<String> c) {
         this.a = a;
         this.b = b;
         this.c = c;
      }

      boolean a(String var1) {
         return this.a != null ? !this.a.equals(var1) : !this.c.contains(var1);
      }
   }

   public record CellCoverage(io.karatelabs.ext.rules.RuleTableCatalog.TableCellReference b, String c, io.karatelabs.ext.rules.CellCoverageAnalyzer.CellProbeResult d, String e, Set<String> f) {

      public CellCoverage(io.karatelabs.ext.rules.RuleTableCatalog.TableCellReference b, String c, io.karatelabs.ext.rules.CellCoverageAnalyzer.CellProbeResult d, String e, Set<String> f) {
         this.b = b;
         this.c = c;
         this.d = d;
         this.e = e;
         this.f = f;
      }

      public Map<String, Object> a() {
         LinkedHashMap<java.lang.String, java.lang.Object> var1 = new LinkedHashMap();
         var1.put("address", this.b.a());
         var1.putAll(this.b.b());
         var1.put("status", this.c);
         if (this.d != null) {
            var1.put("probe", this.d.a());
         }

         if (this.e != null) {
            var1.put("reason", this.e);
         }

         if (!this.f.isEmpty()) {
            var1.put("jurisdictions", new ArrayList<>(this.f));
         }

         return var1;
      }

      public io.karatelabs.ext.rules.RuleTableCatalog.TableCellReference b() {
         return this.b;
      }

      public String c() {
         return this.c;
      }

      public io.karatelabs.ext.rules.CellCoverageAnalyzer.CellProbeResult d() {
         return this.d;
      }

      public String e() {
         return this.e;
      }

      public Set<String> f() {
         return this.f;
      }
   }
}
