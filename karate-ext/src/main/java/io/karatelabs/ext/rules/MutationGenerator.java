package io.karatelabs.ext.rules;

import io.karatelabs.js.Engine;
import io.karatelabs.parser.Node;
import io.karatelabs.parser.NodeType;
import io.karatelabs.parser.Token;
import io.karatelabs.parser.TokenType;
import java.lang.ref.SoftReference;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Map.Entry;
import java.util.function.Supplier;

public final class MutationGenerator {
   public static final String a = "NEGATE_CONDITIONALS";
   public static final String b = "CONDITIONALS_BOUNDARY";
   public static final String c = "REMOVE_CONDITIONALS";
   public static final String d = "INLINE_CONSTANT";
   public static final String e = "condition-evaluated";
   public static final String f = "arm-entered";
   public static final String g = "operation-called";
   public static final String h = "compare-operand";
   public static final String i = "arm-operand";
   private static final Set<TokenType> k = Set.of(
      TokenType.EQ_EQ, TokenType.EQ_EQ_EQ, TokenType.NOT_EQ, TokenType.NOT_EQ_EQ, TokenType.LT, TokenType.GT, TokenType.LT_EQ, TokenType.GT_EQ
   );
   private static final Map<String, String> l = Map.of("==", "!=", "!=", "==", "===", "!==", "!==", "===", "<", ">=", ">=", "<", ">", "<=", "<=", ">");
   private static final Map<String, String> m = Map.of("<", "<=", "<=", "<", ">", ">=", ">=", ">");
   public static final String j = "karate.mutation.tableThreshold";
   private final String n;
   private final RuleTableCatalog o;
   private int p = Integer.getInteger("karate.mutation.tableThreshold", 1000);
   private final List<Map<String, Object>> q = new ArrayList<>();
   private final Node r;
   private final Map<Integer, String> s;
   private final int t;
   private final int u;

   public MutationGenerator(String var1) {
      this(var1, RuleTableCatalog.c);
   }

   public MutationGenerator(String var1, RuleTableCatalog var2) {
      this.n = var1;
      this.o = var2;
      this.r = Engine.parse(var1);
      this.s = RuleInputSpaceExplorer.b(var1);
      int[] var3 = a(this.r);
      this.t = var3[0];
      this.u = var3[1];
   }

   public MutationGenerator a(int var1) {
      this.p = var1;
      return this;
   }

   public List<io.karatelabs.ext.rules.MutationGenerator.Mutant> a() {
      return this.a((io.karatelabs.ext.rules.MutationGenerator.MutationTarget)null);
   }

   public List<io.karatelabs.ext.rules.MutationGenerator.Mutant> a(io.karatelabs.ext.rules.MutationGenerator.MutationTarget var1) {
      this.q.clear();
      ArrayList<io.karatelabs.ext.rules.MutationGenerator.Mutant> var2 = new ArrayList();
      var2.addAll(this.c());
      var2.addAll(this.d());
      var2.addAll(this.e());
      var2.addAll(this.f());
      var2.addAll(this.b(var1));
      return var2;
   }

   public List<Map<String, Object>> b() {
      return List.copyOf(this.q);
   }

   private void a(String var1, String var2) {
      LinkedHashMap<String, Object> var3 = new LinkedHashMap();
      var3.put("operator", "INLINE_CONSTANT");
      var3.put("code", var1);
      var3.put("reason", var2);
      this.q.add(var3);
   }

   private List<io.karatelabs.ext.rules.MutationGenerator.Mutant> c() {
      return this.a("NEGATE_CONDITIONALS", l);
   }

   private List<io.karatelabs.ext.rules.MutationGenerator.Mutant> d() {
      return this.a("CONDITIONALS_BOUNDARY", m);
   }

   private List<io.karatelabs.ext.rules.MutationGenerator.Mutant> a(String var1, Map<String, String> var2) {
      ArrayList<io.karatelabs.ext.rules.MutationGenerator.Mutant> var3 = new ArrayList();
      LinkedHashMap<java.lang.Integer, java.lang.Integer> var4 = new LinkedHashMap();

      for (Node var6 : this.r.findAll(NodeType.LOGIC_EXPR)) {
         Token var7 = var6.get(1).getFirstToken();
         if (k.contains(var7.type) && var2.containsKey(var7.getText()) && !this.b(var7.pos) && !c(var6)) {
            int var8 = var7.line + 1;
            int var9 = var4.merge(var8, 1, Integer::sum) - 1;
            var3.add(this.a(var1, var2, var6, var7, var8, var9));
         }
      }

      return var3;
   }

   private io.karatelabs.ext.rules.MutationGenerator.Mutant a(String var1, Map<String, String> var2, Node var3, Token var4, int var5, int var6) {
      String var7 = var4.getText();
      String var8 = (String)var2.get(var7);
      String var9 = "L" + var5 + "#" + var6;
      String var10 = this.a(var4.pos, var4.pos + var4.length, var8);
      String var11 = f(var3.getText());
      return this.a(var1 + "@" + var9, var1, var9, var5, var4.pos, var7, var8, var11, this.b(var3), var10);
   }

   private List<io.karatelabs.ext.rules.MutationGenerator.Mutant> e() {
      ArrayList<io.karatelabs.ext.rules.MutationGenerator.Mutant> var1 = new ArrayList();

      for (Node var3 : this.r.findAll(NodeType.IF_STMT)) {
         Token var4 = var3.getFirstToken();
         if (!this.b(var4.pos)) {
            String var5 = this.s.get(var4.pos);
            if (var5 != null) {
               Node var6 = var3.get(2);
               Token var7 = var6.getFirstToken();
               Token var8 = var6.getLastToken();
               boolean var9 = var3.size() > 5;
               String var10 = f(var6.getText());
               String var11 = this.a(var7.pos, var8.pos + var8.length, "false");
               String var12 = var9 ? " (if/else — the false arm is forced)" : "";
               int var13 = var7.line + 1;
               var1.add(
                  this.a(
                     "REMOVE_CONDITIONALS@" + var5,
                     "REMOVE_CONDITIONALS",
                     var5,
                     var13,
                     var7.pos,
                     var10,
                     "false" + var12,
                     f("if (" + var10 + ")"),
                     new io.karatelabs.ext.rules.MutationGenerator.MutationWitness("condition-evaluated", var5, null, null),
                     var11
                  )
               );
            }
         }
      }

      return var1;
   }

   private List<io.karatelabs.ext.rules.MutationGenerator.Mutant> f() {
      ArrayList<io.karatelabs.ext.rules.MutationGenerator.Mutant> var1 = new ArrayList();
      LookupTableEditor var2 = new LookupTableEditor(this.n);
      if (!var2.a()) {
         if (this.o.a()) {
            this.a("LOOKUP_NONE", "no inline lookup and no tables/ — nothing to perturb");
         }

         return var1;
      } else {
         var var3 = var2.b();
         TreeMap var4 = new TreeMap();
         a("", var3, var4);

         for (Entry var6 : (Iterable<Entry>) (Iterable<?>) (var4.entrySet())) {
            String var7 = (String)var6.getKey();
            double var8 = ((Number)var6.getValue()).doubleValue();
            double var10 = var8 == 0.0 ? 1.0 : var8 * 2.0;
            String var12 = var10 == Math.floor(var10) && Math.abs(var10) < 1.0E15 ? String.valueOf((long)var10) : String.valueOf(var10);

            String var13;
            try {
               var13 = new LookupTableEditor(this.n).a(var7.split("\\."), var12);
            } catch (RuntimeException var16) {
               continue;
            }

            io.karatelabs.ext.rules.MutationGenerator.MutationWitness var14 = var3.get(c(var7)) instanceof List ? this.a(var7, var3, var12) : this.b(var7);
            if (var14 == null) {
               this.a("LOOKUP_NO_WITNESS", "lookup." + var7 + ": an array-row leaf with no sound traced witness");
            } else {
               Token var15 = var2.a(var7.split("\\.")).getFirstToken();
               var1.add(
                  this.a(
                     "INLINE_CONSTANT@lookup." + var7,
                     "INLINE_CONSTANT",
                     "lookup." + var7,
                     var15.line + 1,
                     var15.pos,
                     String.valueOf(var6.getValue()),
                     var12,
                     "lookup." + var7,
                     var14,
                     var13
                  )
               );
            }
         }

         return var1;
      }
   }

   private List<io.karatelabs.ext.rules.MutationGenerator.Mutant> b(io.karatelabs.ext.rules.MutationGenerator.MutationTarget var1) {
      ArrayList<io.karatelabs.ext.rules.MutationGenerator.Mutant> var2 = new ArrayList();
      if (this.o.a()) {
         return var2;
      } else {
         ArrayList<io.karatelabs.ext.rules.RuleTableCatalog.TableCellReference> var3 = new ArrayList();

         for (io.karatelabs.ext.rules.RuleTableCatalog.TableCellReference var5 : this.o.f()) {
            if (var5.h() instanceof Number) {
               var3.add(var5);
            }
         }

         for (io.karatelabs.ext.rules.RuleTableCatalog.TableDefinition var22 : this.o.b().values()) {
            if (var22.b().isEmpty()) {
               this.a("LOOKUP_NO_WITNESS", "lookup." + var22.a() + ": a table with no key columns has no addressable row");
            }
         }

         if (var1 == null && var3.size() > this.p) {
            throw new RuleApiException(
               422,
               "TABLE_TARGET_REQUIRED",
               var3.size()
                  + " table-value mutants exceed the untargeted threshold "
                  + this.p
                  + " — pass {target} to Rule.mutate: 'read' (the cells saved scenarios read), a list of cell addresses (a change set's cells), or 'all'"
            );
         } else {
            String var21 = a(this.n);

            for (io.karatelabs.ext.rules.RuleTableCatalog.TableCellReference var6 : (Iterable<io.karatelabs.ext.rules.RuleTableCatalog.TableCellReference>) (Iterable<?>) (var3)) {
               String var7 = var6.a();
               if (var1 == null || var1.b() || var1.c().contains(var7)) {
                  double var8 = ((Number)var6.h()).doubleValue();
                  double var10 = var8 == 0.0 ? 1.0 : var8 * 2.0;
                  String var12 = var10 == Math.floor(var10) && Math.abs(var10) < 1.0E15 ? String.valueOf((long)var10) : String.valueOf(var10);
                  var var13 = new TreeMap<>(this.o.c());
                  boolean var14 = true;
                  String var15 = null;

                  try {
                     this.o.b(var6, var10);
                     var13.put(var6.d(), io.karatelabs.ext.common.Sha256.a(this.o.a(var6.d(), Map.of(var6.f(), Map.of(var6.g(), var10)))));
                  } catch (RuntimeException var19) {
                     var14 = false;
                     var15 = "TABLE_INVALID";
                  }

                  var var16 = this.o.b(var6);
                  io.karatelabs.ext.rules.MutationGenerator.MutationWitness var17 = this.b(var16.isEmpty() ? var6.c() : (String)var16.get(0));
                  String var18 = RulebookRepository.a(var21, (Map<String, String>)(var14 ? var13 : this.o.c())).c();
                  var2.add(
                     new io.karatelabs.ext.rules.MutationGenerator.Mutant(
                        "INLINE_CONSTANT@lookup." + var7,
                        "INLINE_CONSTANT",
                        "lookup." + var7,
                        0,
                        -1,
                        String.valueOf(var6.h()),
                        var12,
                        "lookup." + var7,
                        var17,
                        this.n,
                        var18,
                        var14,
                        var15,
                        var14 ? a(() -> this.o.a(var6, var10)) : () -> this.o,
                        var6
                     )
                  );
               }
            }

            return var2;
         }
      }
   }

   public static Set<String> a(String var0, RuleTableCatalog var1, List<Map<String, Object>> var2) {
      TreeSet<java.lang.String> var3 = new TreeSet();
      if (var1.a()) {
         return var3;
      } else {
         for (Map var5 : var2) {
            try {
               io.karatelabs.ext.rules.CalcExecutor.a(var0, var1.a(var3::add), RulebookRepository.d(var5), null);
            } catch (RuntimeException var7) {
            }
         }

         return var3;
      }
   }

   private static void a(String var0, Object var1, Map<String, Number> var2) {
      if (var1 instanceof Map var3) {
         for (Entry var7 : (Iterable<Entry>) (Iterable<?>) (var3.entrySet())) {
            a(var0.isEmpty() ? String.valueOf(var7.getKey()) : var0 + "." + var7.getKey(), var7.getValue(), var2);
         }
      } else if (var1 instanceof List var4) {
         for (int var8 = 0; var8 < var4.size(); var8++) {
            a(var0 + "." + var8, var4.get(var8), var2);
         }
      } else if (var1 instanceof Number var5) {
         var2.put(var0, var5);
      }
   }

   private io.karatelabs.ext.rules.MutationGenerator.MutationWitness b(Node var1) {
      Node var2 = var1;

      for (Node var3 = var1.getParent(); var3 != null; var3 = var3.getParent()) {
         Token var4 = var3.getFirstToken();
         String var5 = var4 == null ? null : this.s.get(var4.pos);
         if (var5 != null) {
            if (var3.type == NodeType.LOGIC_AND_EXPR && var3.size() >= 3 && var2 == var3.get(2)) {
               boolean var7 = "||".equals(var3.get(1).getText());
               return new io.karatelabs.ext.rules.MutationGenerator.MutationWitness(
                  "arm-entered",
                  var5,
                  Boolean.TRUE,
                  var7 ? "rhs of || — evaluated only when the lhs did NOT hold" : "rhs of && — evaluated only when the lhs held"
               );
            }

            if (var3.type == NodeType.IF_STMT) {
               if (var2 == var3.get(2)) {
                  return new io.karatelabs.ext.rules.MutationGenerator.MutationWitness("condition-evaluated", var5, null, null);
               }

               boolean var6 = var3.size() > 4 && var2 == var3.get(4);
               return new io.karatelabs.ext.rules.MutationGenerator.MutationWitness("arm-entered", var5, var6, null);
            }

            return new io.karatelabs.ext.rules.MutationGenerator.MutationWitness("condition-evaluated", var5, null, null);
         }

         var2 = var3;
      }

      return new io.karatelabs.ext.rules.MutationGenerator.MutationWitness("operation-called", null, null, null);
   }

   private io.karatelabs.ext.rules.MutationGenerator.MutationWitness b(String var1) {
      String var2 = var1.contains(".") ? var1.substring(0, var1.indexOf(46)) : var1;
      ArrayList<io.karatelabs.parser.Node> var3 = new ArrayList();

      for (NodeType var5 : List.of(NodeType.REF_DOT_EXPR, NodeType.REF_BRACKET_EXPR)) {
         for (Node var7 : this.r.findAll(var5)) {
            if (!this.b(var7.getFirstToken().pos)) {
               String var8 = var7.getText();
               if (var8 != null && (var8.startsWith("lookup." + var2) || var8.startsWith("lookup[")) && var8.contains(var2)) {
                  var3.add(var7);
               }
            }
         }
      }

      String var9 = var2.equals(var1) ? null : "table-level witness for a per-row perturbation (" + var2 + ")";
      io.karatelabs.ext.rules.MutationGenerator.MutationWitness var10 = null;

      for (Node var12 : (Iterable<Node>) (Iterable<?>) (var3)) {
         io.karatelabs.ext.rules.MutationGenerator.MutationWitness var13 = this.d(var12);
         if (var13 == null) {
            return new io.karatelabs.ext.rules.MutationGenerator.MutationWitness("operation-called", null, null, var9);
         }

         if (var10 != null) {
            if (!var10.c().equals(var13.c()) || !var10.d().equals(var13.d())) {
               return new io.karatelabs.ext.rules.MutationGenerator.MutationWitness("operation-called", null, null, "reads in multiple arms of " + var2);
            }
         } else {
            var10 = new io.karatelabs.ext.rules.MutationGenerator.MutationWitness(var13.b(), var13.c(), var13.d(), var9);
         }
      }

      return var10 != null ? var10 : new io.karatelabs.ext.rules.MutationGenerator.MutationWitness("operation-called", null, null, var9);
   }

   private static String c(String var0) {
      return var0.contains(".") ? var0.substring(0, var0.indexOf(46)) : var0;
   }

   private io.karatelabs.ext.rules.MutationGenerator.MutationWitness a(String var1, Map<String, Object> var2, String var3) {
      String[] var4 = var1.split("\\.");
      if (var4.length != 3) {
         return null;
      } else {
         String var5 = var4[0];

         int var6;
         try {
            var6 = Integer.parseInt(var4[1]);
         } catch (NumberFormatException var18) {
            return null;
         }

         String var7 = var4[2];
         List var8 = (List)var2.get(var5);
         var var9 = this.g();
         if (var9.contains(var7)) {
            for (int var19 = 0; var19 < var8.size(); var19++) {
               if (var19 != var6 && var8.get(var19) instanceof Map var20 && a(var20.get(var7), var3)) {
                  return null;
               }
            }

            return new io.karatelabs.ext.rules.MutationGenerator.MutationWitness("compare-operand", null, null, "operand-keyed: a COMPARE with rhs " + var3 + " proves this row's " + var7 + " was read", var3);
         } else {
            for (String var11 : (Iterable<String>) (Iterable<?>) (var9)) {
               ArrayList<java.lang.String> var12 = new ArrayList();

               for (Object var14 : var8) {
                  if (var14 instanceof Map var15 && var15.get(var11) instanceof Number var16) {
                     var12.add(a(var16));
                  }
               }

               if (var12.size() == var8.size() && var12.stream().distinct().count() == var12.size()) {
                  String var22 = this.d(var11);
                  if (var22 != null) {
                     return new io.karatelabs.ext.rules.MutationGenerator.MutationWitness(
                        "arm-operand",
                        var22,
                        Boolean.TRUE,
                        "row-keyed by " + var11 + "=" + (String)var12.get(var6) + " (the arm fired for THIS row)",
                        (String)var12.get(var6)
                     );
                  }
               }
            }

            return null;
         }
      }
   }

   private Set<String> g() {
      LinkedHashSet<java.lang.String> var1 = new LinkedHashSet();

      for (Node var3 : this.r.findAll(NodeType.LOGIC_EXPR)) {
         Token var4 = var3.get(1).getFirstToken();
         if (k.contains(var4.type)) {
            for (Node var6 : var3.findAll(NodeType.REF_DOT_EXPR)) {
               String var7 = var6.getText();
               if (var7 != null && var7.contains(".")) {
                  var1.add(var7.substring(var7.lastIndexOf(46) + 1));
               }
            }
         }
      }

      return var1;
   }

   private String d(String var1) {
      for (Node var3 : this.r.findAll(NodeType.LOGIC_EXPR)) {
         Token var4 = var3.get(1).getFirstToken();
         if (k.contains(var4.type)) {
            boolean var5 = var3.findAll(NodeType.REF_DOT_EXPR).stream().anyMatch(var1x -> var1x.getText() != null && var1x.getText().endsWith("." + var1));
            if (var5) {
               for (Node var6 = var3.getParent(); var6 != null; var6 = var6.getParent()) {
                  if (var6.type == NodeType.IF_STMT) {
                     String var7 = this.s.get(var6.getFirstToken().pos);
                     if (var7 != null) {
                        return var7;
                     }
                  }
               }
            }
         }
      }

      return null;
   }

   private static String a(Number var0) {
      double var1 = var0.doubleValue();
      return var1 == Math.floor(var1) && Math.abs(var1) < 1.0E15 ? String.valueOf((long)var1) : String.valueOf(var1);
   }

   private static boolean a(Object var0, String var1) {
      return var0 instanceof Number var2 && a(var2).equals(var1);
   }

   private static boolean c(Node var0) {
      for (Node var1 = var0.getParent(); var1 != null; var1 = var1.getParent()) {
         if (var1.type == NodeType.FN_CALL_EXPR && !var1.isEmpty()) {
            String var2 = var1.getFirst().getText();
            if (var2 != null && (var2.startsWith("calc.always") || var2.startsWith("calc.sometimes"))) {
               return true;
            }
         }
      }

      return false;
   }

   private io.karatelabs.ext.rules.MutationGenerator.MutationWitness d(Node var1) {
      Node var2 = var1;

      for (Node var3 = var1.getParent(); var3 != null; var3 = var3.getParent()) {
         if (var3.type == NodeType.IF_STMT) {
            Token var4 = var3.getFirstToken();
            String var5 = this.s.get(var4.pos);
            if (var5 != null && var3.size() > 4) {
               if (var2 == var3.get(4)) {
                  return new io.karatelabs.ext.rules.MutationGenerator.MutationWitness("arm-entered", var5, Boolean.TRUE, null);
               }

               if (var3.size() > 6 && var2 == var3.get(6)) {
                  return new io.karatelabs.ext.rules.MutationGenerator.MutationWitness("arm-entered", var5, Boolean.FALSE, null);
               }
            }
         }

         var2 = var3;
      }

      return null;
   }

   private io.karatelabs.ext.rules.MutationGenerator.Mutant a(String var1, String var2, String var3, int var4, int var5, String var6, String var7, String var8, io.karatelabs.ext.rules.MutationGenerator.MutationWitness var9, String var10) {
      boolean var11 = true;
      String var12 = null;

      try {
         Engine.parse(var10);
      } catch (RuntimeException var14) {
         var11 = false;
         var12 = "PARSE_FAILED";
      }

      if (var11 && e(var10) != e(this.n)) {
         var11 = false;
         var12 = "LINE_SHIFT";
      }

      return new io.karatelabs.ext.rules.MutationGenerator.Mutant(var1, var2, var3, var4, var5, var6, var7, var8, var9, var10, a(var10), var11, var12, () -> this.o, null);
   }

   private static Supplier<RuleTableCatalog> a(final Supplier<RuleTableCatalog> var0) {
      return new Supplier<RuleTableCatalog>() {
         private SoftReference<RuleTableCatalog> b = new SoftReference<>(null);

         @Override
         public synchronized RuleTableCatalog get() {
            RuleTableCatalog var1 = this.b.get();
            if (var1 == null) {
               var1 = (RuleTableCatalog)var0.get();
               this.b = new SoftReference<>(var1);
            }

            return var1;
         }
      };
   }

   private String a(int var1, int var2, String var3) {
      return this.n.substring(0, var1) + var3 + this.n.substring(var2);
   }

   private boolean b(int var1) {
      return var1 >= this.t && var1 < this.u;
   }

   static int[] a(Node var0) {
      for (Node var2 : var0.findAll(NodeType.VAR_DECL)) {
         Token var3 = var2.getFirstToken();
         if (var3 != null && "lookup".equals(var3.getText())) {
            Token var4 = var2.getLastToken();
            return new int[]{var3.pos, var4.pos + var4.length};
         }
      }

      return new int[]{-1, -1};
   }

   private static int e(String var0) {
      int var1 = 1;

      for (int var2 = 0; var2 < var0.length(); var2++) {
         if (var0.charAt(var2) == '\n') {
            var1++;
         }
      }

      return var1;
   }

   private static String f(String var0) {
      if (var0 == null) {
         return "";
      } else {
         String var1 = var0.replaceAll("\\s+", " ").trim();
         return var1.length() > 80 ? var1.substring(0, 77) + "..." : var1;
      }
   }

   static String a(String var0) {
      return io.karatelabs.ext.common.Sha256.a(var0);
   }

   public record Mutant(String d, String e, String f, int g, int h, String i, String j, String k, io.karatelabs.ext.rules.MutationGenerator.MutationWitness l, String m, String n, boolean o, String p, Supplier<RuleTableCatalog> q, io.karatelabs.ext.rules.RuleTableCatalog.TableCellReference r) {

      public Mutant(String d, String e, String f, int g, int h, String i, String j, String k, io.karatelabs.ext.rules.MutationGenerator.MutationWitness l, String m, String n, boolean o, String p, Supplier<RuleTableCatalog> q, io.karatelabs.ext.rules.RuleTableCatalog.TableCellReference r) {
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
         this.q = q;
         this.r = r;
      }

      public RuleTableCatalog a() {
         return this.q.get();
      }

      public Map<String, Object> b() {
         return this.a().e();
      }

      public Map<String, Object> c() {
         LinkedHashMap<java.lang.String, java.lang.Object> var1 = new LinkedHashMap();
         var1.put("id", this.d);
         var1.put("mutatorName", this.e);
         var1.put("site", this.f);
         if (this.r == null) {
            var1.put("line", this.g);
         } else {
            var1.put("location", this.r.b());
         }

         var1.put("original", this.i);
         var1.put("mutated", this.j);
         var1.put("excerpt", this.k);
         var1.put("witness", this.l.a());
         var1.put("sha256", this.n);
         if (!this.o) {
            var1.put("status", "INVALID");
            var1.put("statusReason", this.p);
         }

         return var1;
      }

      public String d() {
         return this.d;
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

      public int h() {
         return this.h;
      }

      public String i() {
         return this.i;
      }

      public String j() {
         return this.j;
      }

      public String k() {
         return this.k;
      }

      public io.karatelabs.ext.rules.MutationGenerator.MutationWitness l() {
         return this.l;
      }

      public String m() {
         return this.m;
      }

      public String n() {
         return this.n;
      }

      public boolean o() {
         return this.o;
      }

      public String p() {
         return this.p;
      }

      public Supplier<RuleTableCatalog> q() {
         return this.q;
      }

      public io.karatelabs.ext.rules.RuleTableCatalog.TableCellReference r() {
         return this.r;
      }
   }

   public record MutationTarget(boolean b, Set<String> c) {

      public MutationTarget(boolean b, Set<String> c) {
         this.b = b;
         this.c = c;
      }

      public static io.karatelabs.ext.rules.MutationGenerator.MutationTarget a() {
         return new io.karatelabs.ext.rules.MutationGenerator.MutationTarget(true, Set.of());
      }

      public static io.karatelabs.ext.rules.MutationGenerator.MutationTarget a(Set<String> var0) {
         return new io.karatelabs.ext.rules.MutationGenerator.MutationTarget(false, Set.copyOf(var0));
      }

      public static io.karatelabs.ext.rules.MutationGenerator.MutationTarget a(Object var0, io.karatelabs.ext.rules.RulebookRepository.RulebookDocument var1) {
         if (var0 == null) {
            return null;
         } else if ("all".equals(var0)) {
            return a();
         } else if ("read".equals(var0)) {
            return a(io.karatelabs.ext.rules.MutationGenerator.a(var1.g(), var1.m(), var1.b()));
         } else if (!(var0 instanceof List var2)) {
            throw new RuleApiException(400, "INVALID_INPUT", "target is 'all', 'read' or a list of table cell addresses, not " + var0);
         } else {
            LinkedHashSet<java.lang.String> var3 = new LinkedHashSet();

            for (Object var5 : var2) {
               var3.add(String.valueOf(var5));
            }

            return a(var3);
         }
      }

      public boolean b() {
         return this.b;
      }

      public Set<String> c() {
         return this.c;
      }
   }

   public record MutationWitness(String b, String c, Boolean d, String e, String f) {

      public MutationWitness(String var1, String var2, Boolean var3, String var4) {
         this(var1, var2, var3, var4, null);
      }

      public MutationWitness(String b, String c, Boolean d, String e, String f) {
         this.b = b;
         this.c = c;
         this.d = d;
         this.e = e;
         this.f = f;
      }

      Map<String, Object> a() {
         LinkedHashMap<java.lang.String, java.lang.Object> var1 = new LinkedHashMap();
         var1.put("kind", this.b);
         if (this.c != null) {
            var1.put("site", this.c);
         }

         if (this.d != null) {
            var1.put("outcome", this.d);
         }

         if (this.f != null) {
            var1.put("operand", this.f);
         }

         if (this.e != null) {
            var1.put("grain", this.e);
         }

         return var1;
      }

      public String b() {
         return this.b;
      }

      public String c() {
         return this.c;
      }

      public Boolean d() {
         return this.d;
      }

      public String e() {
         return this.e;
      }

      public String f() {
         return this.f;
      }
   }
}
