package io.karatelabs.ext.rules;

import io.karatelabs.js.Engine;
import io.karatelabs.parser.Node;
import io.karatelabs.parser.NodeType;
import io.karatelabs.parser.Token;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Predicate;
import java.util.function.ToIntFunction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class RequirementClaimAnalyzer {
   private static final Set<String> a = Set.of("0", "''", "\"\"", "[]", "{}", "null", "false");
   private static final Pattern b = Pattern.compile("'([^']*)'|\"([^\"]*)\"|`((?:[^`$]|\\$(?!\\{))*)`");
   private static final Pattern c = Pattern.compile("^lookup((?:\\.[A-Za-z_$][\\w$]*)+)");
   private final Map<String, List<String>> d = new LinkedHashMap<>();
   private final Map<String, String> e = new LinkedHashMap<>();
   private final List<io.karatelabs.ext.rules.RequirementClaimAnalyzer.ClaimedSourceSpan> f = new ArrayList<>();
   private final List<RequirementClaimAnalyzer.LookupValueSpan> g = new ArrayList<>();
   private final List<io.karatelabs.ext.rules.RequirementClaimAnalyzer.LookupClaim> h = new ArrayList<>();
   private final RuleTableCatalog i;
   private int j = -1;
   private int k = -1;
   private boolean l;

   public static RequirementClaimAnalyzer a(String var0) {
      return new RequirementClaimAnalyzer(var0, RuleTableCatalog.c);
   }

   public static RequirementClaimAnalyzer a(String var0, RuleTableCatalog var1) {
      return new RequirementClaimAnalyzer(var0, var1);
   }

   public List<String> a(io.karatelabs.ext.rules.RuleTableCatalog.TableCellReference var1) {
      TreeSet var2 = new TreeSet();

      for (String var4 : this.i.b(var1)) {
         for (io.karatelabs.ext.rules.RequirementClaimAnalyzer.LookupClaim var6 : this.h) {
            if (var4.equals(var6.a()) || var4.startsWith(var6.a() + ".") || var6.a().startsWith(var4 + ".")) {
               var2.addAll(var6.b());
            }
         }
      }

      return List.copyOf(var2);
   }

   public boolean a() {
      return this.l;
   }

   public List<String> a(String var1, boolean var2) {
      return List.copyOf(this.d.getOrDefault(b(var1, var2), List.of()));
   }

   public String a(String var1, boolean var2, boolean var3) {
      if (!this.a(var1, var2).isEmpty()) {
         return "claimed";
      } else {
         return var3 ? "guard" : this.e.getOrDefault(b(var1, var2), "notclaimed");
      }
   }

   public List<String> a(int var1) {
      if (var1 >= this.j && var1 < this.k) {
         TreeSet var2 = new TreeSet();

         for (RequirementClaimAnalyzer.LookupValueSpan var4 : this.g) {
            if (var1 >= var4.a() && var1 < var4.b()) {
               var2.addAll(var4.c());
            }
         }

         return List.copyOf(var2);
      } else {
         return this.a(var1x -> var1 >= var1x.c() && var1 < var1x.d(), var0 -> var0.d() - var0.c());
      }
   }

   public List<String> b(int var1) {
      return this.a(var1x -> var1 >= var1x.a() && var1 <= var1x.b(), var0 -> var0.b() - var0.a());
   }

   private List<String> a(Predicate<io.karatelabs.ext.rules.RequirementClaimAnalyzer.ClaimedSourceSpan> var1, ToIntFunction<io.karatelabs.ext.rules.RequirementClaimAnalyzer.ClaimedSourceSpan> var2) {
      io.karatelabs.ext.rules.RequirementClaimAnalyzer.ClaimedSourceSpan var3 = null;

      for (io.karatelabs.ext.rules.RequirementClaimAnalyzer.ClaimedSourceSpan var5 : this.f) {
         if (var1.test(var5) && (var3 == null || a(var5, var3, var2))) {
            var3 = var5;
         }
      }

      return var3 == null ? List.of() : var3.g();
   }

   private static boolean a(io.karatelabs.ext.rules.RequirementClaimAnalyzer.ClaimedSourceSpan var0, io.karatelabs.ext.rules.RequirementClaimAnalyzer.ClaimedSourceSpan var1, ToIntFunction<io.karatelabs.ext.rules.RequirementClaimAnalyzer.ClaimedSourceSpan> var2) {
      if (var2.applyAsInt(var0) != var2.applyAsInt(var1)) {
         return var2.applyAsInt(var0) < var2.applyAsInt(var1);
      } else {
         return var0.e() != var1.e() ? var0.e() > var1.e() : var0.f() && !var1.f();
      }
   }

   private static String b(String var0, boolean var1) {
      return var0 + ":" + var1;
   }

   private RequirementClaimAnalyzer(String var1, RuleTableCatalog var2) {
      this.i = var2;
      Node var3 = Engine.parse(var1);
      var var4 = RuleInputSpaceExplorer.a(var3);
      Map<java.lang.Integer, java.lang.String> var5 = RuleInputSpaceExplorer.a(var4);
      this.a(var3, var1, var5);
      this.b(var3, var5);

      for (Node var7 : (Iterable<Node>) (Iterable<?>) (var4)) {
         String var8 = (String)var5.get(var7.getFirstToken().pos);
         this.a(var7, var8, true);
         this.a(var7, var8, false);
      }

      this.c(var3, var5);
      this.a(var3, var1);
   }

   private void a(Node var1, String var2, Map<Integer, String> var3) {
      String var4 = RuleInputSpaceExplorer.a(var2) + ".req";

      for (Node var6 : var1.findAll(NodeType.FN_CALL_EXPR)) {
         if (var6.size() >= 3 && var4.equals(var6.get(0).getText())) {
            this.l = true;
            Matcher var7 = b.matcher(var6.get(2).getText());
            if (var7.matches()) {
               String var8 = var7.group(1) != null ? var7.group(1) : (var7.group(2) != null ? var7.group(2) : var7.group(3));
               String var9 = a(var6, var3);
               if (var9 != null) {
                  var var10 = this.d.computeIfAbsent(var9, var0 -> new ArrayList<>());
                  if (!var10.contains(var8)) {
                     var10.add(var8);
                  }
               }
            }
         }
      }
   }

   private static String a(Node var0, Map<Integer, String> var1) {
      Node var2 = var0;

      for (Node var3 = var0.getParent(); var3 != null; var3 = var3.getParent()) {
         if (var3.type == NodeType.FN_EXPR || var3.type == NodeType.FN_ARROW_EXPR) {
            return null;
         }

         if (var3.type == NodeType.DEFAULT_BLOCK && b(var3, var2) >= 2) {
            return g(var3);
         }

         String var4 = (String)var1.get(var3.getFirstToken().pos);
         if (var4 != null) {
            if (var3.type == NodeType.IF_STMT) {
               if (var2 == var3.get(4)) {
                  return b(var4, true);
               }

               if (var3.size() > 5 && var2 == var3.get(6)) {
                  return b(var4, false);
               }
            } else if (var3.type == NodeType.CASE_BLOCK && b(var3, var2) >= 3) {
               return b(var4, true);
            }
         }

         var2 = var3;
      }

      return null;
   }

   private void b(Node var1, Map<Integer, String> var2) {
      for (Node var4 : var1.findAll(NodeType.SWITCH_STMT)) {
         var var5 = f(var4);

         for (int var6 = var5.size() - 1; var6 >= 0; var6--) {
            Node var7 = (Node)var5.get(var6);
            if (var7.type == NodeType.CASE_BLOCK && var7.size() <= 3 && var6 + 1 < var5.size()) {
               String var8 = (String)var2.get(var7.getFirstToken().pos);
               Node var9 = (Node)var5.get(var6 + 1);
               List var10 = var9.type == NodeType.CASE_BLOCK ? this.d.get(b((String)var2.get(var9.getFirstToken().pos), true)) : this.d.get(g(var9));
               if (var8 != null && var10 != null) {
                  this.d.put(b(var8, true), var10);
               }
            }
         }
      }
   }

   private void a(Node var1, String var2, boolean var3) {
      if (var2 != null) {
         boolean var4 = var1.type != NodeType.IF_STMT && var1.type != NodeType.CASE_BLOCK;
         if (var4 && b(var1)) {
            this.e.put(b(var2, var3), "implied");
         } else {
            Node var5 = a(var1, var3);
            if (var5 != null && (var4 || !a(var1, var5))) {
               if (!var3 && a(var1) && a.contains(var5.getText())) {
                  this.e.put(b(var2, var3), "default");
               } else {
                  this.e.put(b(var2, var3), "notclaimed");
               }
            } else {
               this.e.put(b(var2, var3), "implied");
            }
         }
      }
   }

   private static Node a(Node var0, boolean var1) {
      switch (var0.type) {
         case IF_STMT:
            return var1 ? var0.get(4) : (var0.size() > 5 ? var0.get(6) : null);
         case LOGIC_TERN_EXPR:
            return var1 ? var0.get(2) : var0.get(4);
         case LOGIC_AND_EXPR:
            boolean var2 = "&&".equals(var0.get(1).getText());
            return var1 == var2 ? var0.get(2) : null;
         case LOGIC_NULLISH_EXPR:
            return var1 ? null : var0.get(2);
         case CASE_BLOCK:
            return var1 && var0.size() > 3 ? var0.get(3) : null;
         default:
            return null;
      }
   }

   private static boolean a(Node var0, Node var1) {
      Node var2 = var0.type == NodeType.CASE_BLOCK ? a(var0, 3) : var1;

      while (var2 != null) {
         if (var2.type == NodeType.STATEMENT) {
            var2 = var2.getFirst();
         } else {
            if (var2.type != NodeType.BLOCK) {
               return var2.type == NodeType.IF_STMT || var2.type == NodeType.SWITCH_STMT;
            }

            var2 = a(var2, 0);
         }
      }

      return false;
   }

   private static Node a(Node var0, int var1) {
      Node var2 = null;

      for (int var3 = var1; var3 < var0.size(); var3++) {
         if (var0.get(var3).type == NodeType.STATEMENT) {
            if (var2 != null) {
               return null;
            }

            var2 = var0.get(var3);
         }
      }

      return var2;
   }

   private static boolean a(Node var0) {
      return var0.type == NodeType.LOGIC_NULLISH_EXPR || var0.type == NodeType.LOGIC_AND_EXPR && "||".equals(var0.get(1).getText());
   }

   private static boolean b(Node var0) {
      Node var1 = var0;

      for (Node var2 = var0.getParent(); var2 != null; var2 = var2.getParent()) {
         switch (var2.type) {
            case IF_STMT:
               if (var1 == var2.get(2)) {
                  return true;
               }

               if (var1 == var2.get(4) || var2.size() > 5 && var1 == var2.get(6)) {
                  return false;
               }
               break;
            case LOGIC_TERN_EXPR:
               if (var1 == var2.get(0)) {
                  return true;
               }
            case LOGIC_AND_EXPR:
            case LOGIC_NULLISH_EXPR:
            default:
               break;
            case CASE_BLOCK:
               if (var1 == var2.get(1)) {
                  return true;
               }

               if (b(var2, var1) >= 3) {
                  return false;
               }
               break;
            case SWITCH_STMT:
               return var1 == var2.get(2);
            case FN_EXPR:
            case FN_ARROW_EXPR:
               return false;
         }

         var1 = var2;
      }

      return false;
   }

   private void c(Node var1, Map<Integer, String> var2) {
      for (Node var4 : var1.findAll(NodeType.IF_STMT)) {
         String var5 = (String)var2.get(var4.getFirstToken().pos);
         if (var5 != null) {
            this.a(var4.get(2), true, a(this.a(var5, true), this.a(var5, false)));
            this.a(var4.get(4), false, this.a(var5, true));
            if (var4.size() > 5) {
               this.a(var4.get(6), false, this.a(var5, false));
            }
         }
      }

      for (Node var13 : var1.findAll(NodeType.SWITCH_STMT)) {
         var var14 = f(var13);
         List<String> var6 = List.of();

         for (Node var8 : (Iterable<Node>) (Iterable<?>) (var14)) {
            String var9 = var8.type == NodeType.CASE_BLOCK ? (String)var2.get(var8.getFirstToken().pos) : null;
            var6 = a(var6, var9 == null ? this.d.getOrDefault(g(var8), List.of()) : this.a(var9, true));
         }

         this.a(var13.get(2), true, var6);

         for (Node var16 : (Iterable<Node>) (Iterable<?>) (var14)) {
            int var17 = var16.type == NodeType.CASE_BLOCK ? 3 : 2;
            if (var16.type == NodeType.CASE_BLOCK) {
               this.a(var16.get(1), true, var6);
            }

            String var10 = var16.type == NodeType.CASE_BLOCK ? (String)var2.get(var16.getFirstToken().pos) : null;
            if (var16.size() > var17) {
               List var11 = var10 == null ? this.d.getOrDefault(g(var16), List.of()) : this.a(var10, true);
               this.a(var16.get(var17), var16, false, var11);
            }
         }
      }
   }

   private void a(Node var1, boolean var2, List<String> var3) {
      this.a(var1, var1, var2, var3);
   }

   private void a(Node var1, Node var2, boolean var3, List<String> var4) {
      Token var5 = var1.getFirstToken();
      Token var6 = var2.getLastToken();
      this.f.add(new io.karatelabs.ext.rules.RequirementClaimAnalyzer.ClaimedSourceSpan(var5.line + 1, var6.line + 1, var5.pos, var6.pos + var6.length, c(var1), var3, var4));
   }

   private static int c(Node var0) {
      int var1 = 0;

      for (Node var2 = var0.getParent(); var2 != null; var2 = var2.getParent()) {
         var1++;
      }

      return var1;
   }

   private static List<String> a(List<String> var0, List<String> var1) {
      var var2 = new LinkedHashSet<>(var0);
      var2.addAll(var1);
      return List.copyOf(var2);
   }

   private void a(Node var1, String var2) {
      int[] var3 = io.karatelabs.ext.rules.MutationGenerator.a(var1);
      this.j = var3[0];
      this.k = var3[1];
      if (this.j >= 0 || !this.i.a()) {
         LookupTableEditor var4 = this.j < 0 ? null : new LookupTableEditor(var2);

         for (NodeType var6 : List.of(NodeType.REF_DOT_EXPR, NodeType.REF_BRACKET_EXPR)) {
            for (Node var8 : var1.findAll(var6)) {
               int var9 = var8.getFirstToken().pos;
               Matcher var10 = c.matcher(var8.getText());
               if ((var9 < this.j || var9 >= this.k) && var10.find() && !d(var8)) {
                  String var11 = var10.group(1).substring(1);
                  if (this.b(var11)) {
                     var var12 = this.b(var8, var9);
                     if (!var12.isEmpty()) {
                        this.h.add(new io.karatelabs.ext.rules.RequirementClaimAnalyzer.LookupClaim(var11, var12));
                     }
                  } else if (var4 != null) {
                     Node var16 = var4.a(var10.group(1).substring(1).split("\\."));
                     List var13 = var16 == null ? List.of() : this.b(var8, var9);
                     if (!var13.isEmpty()) {
                        Token var14 = var16.getFirstToken();
                        Token var15 = var16.getLastToken();
                        this.g.add(new RequirementClaimAnalyzer.LookupValueSpan(var14.pos, var15.pos + var15.length, var13));
                     }
                  }
               }
            }
         }
      }
   }

   private boolean b(String var1) {
      if (this.i.c(var1) != null) {
         return true;
      } else {
         for (String var3 : this.i.b().keySet()) {
            if (var3.startsWith(var1 + ".")) {
               return true;
            }
         }

         return false;
      }
   }

   private static boolean d(Node var0) {
      Node var1 = var0.getParent();
      return var1 != null && (var1.type == NodeType.REF_DOT_EXPR || var1.type == NodeType.REF_BRACKET_EXPR) && var1.getFirst() == var0 && e(var0) == null;
   }

   private List<String> b(Node var1, int var2) {
      Node var3 = e(var1);
      if (var3 == null) {
         return this.a(var2);
      } else {
         Token var4 = var3.getLastToken();
         int var5 = var3.getFirstToken().pos;
         int var6 = var4.pos + var4.length;
         TreeSet var7 = new TreeSet();

         for (io.karatelabs.ext.rules.RequirementClaimAnalyzer.ClaimedSourceSpan var9 : this.f) {
            if (var9.c() >= var5 && var9.d() <= var6) {
               var7.addAll(var9.g());
            }
         }

         return List.copyOf(var7);
      }
   }

   private static Node e(Node var0) {
      Node var1 = var0.getParent();
      if (var1 != null && var1.type == NodeType.REF_DOT_EXPR) {
         Node var2 = var1.getParent();
         if (var2 != null && var2.type == NodeType.FN_CALL_EXPR && var2.getFirst() == var1) {
            return var2.findAll(NodeType.FN_EXPR).isEmpty() && var2.findAll(NodeType.FN_ARROW_EXPR).isEmpty() ? null : var2;
         } else {
            return null;
         }
      } else {
         return null;
      }
   }

   private static List<Node> f(Node var0) {
      ArrayList<io.karatelabs.parser.Node> var1 = new ArrayList();

      for (int var2 = 0; var2 < var0.size(); var2++) {
         if (var0.get(var2).type == NodeType.CASE_BLOCK || var0.get(var2).type == NodeType.DEFAULT_BLOCK) {
            var1.add(var0.get(var2));
         }
      }

      return var1;
   }

   private static String g(Node var0) {
      return "default@" + var0.getFirstToken().pos;
   }

   private static int b(Node var0, Node var1) {
      for (int var2 = 0; var2 < var0.size(); var2++) {
         if (var0.get(var2) == var1) {
            return var2;
         }
      }

      return -1;
   }

   private record LookupValueSpan(int a, int b, List<String> c) {

      private LookupValueSpan(int a, int b, List<String> c) {
         this.a = a;
         this.b = b;
         this.c = c;
      }
   }

   private record ClaimedSourceSpan(int a, int b, int c, int d, int e, boolean f, List<String> g) {

      private ClaimedSourceSpan(int a, int b, int c, int d, int e, boolean f, List<String> g) {
         this.a = a;
         this.b = b;
         this.c = c;
         this.d = d;
         this.e = e;
         this.f = f;
         this.g = g;
      }
   }

   private record LookupClaim(String a, List<String> b) {

      private LookupClaim(String a, List<String> b) {
         this.a = a;
         this.b = b;
      }
   }
}
