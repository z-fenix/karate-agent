package io.karatelabs.ext.rules.analysis;

import io.karatelabs.js.Terms;
import io.karatelabs.parser.Node;
import io.karatelabs.parser.NodeType;
import io.karatelabs.parser.TokenType;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

public final class CalcDialectAnalyzer {
   private static final Set<String> a = Set.of("calc", "Math", "undefined");
   private static final Set<String> b = Set.of("input", "output");
   private static final Set<String> c = Set.of("always", "sometimes", "log", "label", "req", "levels", "outcome", "round");
   private static final Map<String, Integer> d = Map.of("min", 2, "max", 2, "abs", 1, "floor", 1, "round", 1);
   private static final Set<String> e = Set.of("=", "+=", "-=", "*=", "/=");
   private static final Set<String> f = Set.of("<", "<=", ">", ">=", "===", "!==", "==", "!=");
   private static final Set<String> g = Set.of("+", "-", "*", "/", "%");
   private final Map<Node, Map<Node, Integer>> h = new LinkedHashMap<>();
   private Node i;
   private String j;

   private CalcDialectAnalyzer() {
   }

   public static io.karatelabs.ext.rules.analysis.CalcDialectAnalyzer.CalcDialectAnalysis a(Node var0) {
      return a(var0, Set.of());
   }

   public static io.karatelabs.ext.rules.analysis.CalcDialectAnalyzer.CalcDialectAnalysis a(Node var0, Set<String> var1) {
      CalcDialectAnalyzer var2 = new CalcDialectAnalyzer();
      io.karatelabs.ext.rules.analysis.CalcDialectAnalyzer.DialectAnalysisScope var3 = new io.karatelabs.ext.rules.analysis.CalcDialectAnalyzer.DialectAnalysisScope(null, 0, true, true);
      if (!var1.isEmpty()) {
         var3.e.put("lookup", null);
         var3.f.add("lookup");
      }

      var2.a(var0, var3);

      for (int var4 = 0; var4 < var0.size(); var4++) {
         Node var5 = var0.get(var4);
         if (!var5.isToken()) {
            var2.b(var5, var3);
         }
      }

      Node var6 = var3.e.get("execute");
      if (var6 == null) {
         throw new RuleAnalysisException("DIALECT_NO_EXECUTE", -1, "calc.js does not define an 'execute' function");
      } else {
         var2.a();
         LinkedHashMap<java.lang.String, io.karatelabs.parser.Node> var7 = new LinkedHashMap();
         var3.e.forEach((var1x, var2x) -> {
            if (var2x != null) {
               var7.put(var1x, var2x);
            }
         });
         return new io.karatelabs.ext.rules.analysis.CalcDialectAnalyzer.CalcDialectAnalysis(var0, var7);
      }
   }

   private void a(Node var1, io.karatelabs.ext.rules.analysis.CalcDialectAnalyzer.DialectAnalysisScope var2) {
      for (int var3 = 0; var3 < var1.size(); var3++) {
         Node var4 = var1.get(var3);
         if (var4.type == NodeType.STATEMENT && var4.size() > 0 && var4.getFirst().type == NodeType.FN_EXPR) {
            Node var5 = var4.getFirst();
            String var6 = b(var5);
            if (var6 == null) {
               a("DIALECT_STATEMENT", var5, "an anonymous function statement");
            }

            var2.e.put(var6, var5);
         }
      }
   }

   static String b(Node var0) {
      for (int var1 = 0; var1 < var0.size(); var1++) {
         Node var2 = var0.get(var1);
         if (var2.type == NodeType.FN_DECL_ARGS) {
            return null;
         }

         if (var2.isToken() && var2.token.type == TokenType.IDENT) {
            return var2.getText();
         }
      }

      return null;
   }

   private void b(Node var1, io.karatelabs.ext.rules.analysis.CalcDialectAnalyzer.DialectAnalysisScope var2) {
      Node var3 = var1.type == NodeType.STATEMENT ? var1.getFirst() : var1;
      if (!var3.isToken()) {
         switch (var3.type) {
            case VAR_STMT:
               this.c(var3, var2);
               break;
            case IF_STMT:
               this.d(var3.get(2), var2);
               this.b(var3.get(4), var2);
               if (var3.size() > 5) {
                  this.b(var3.get(6), var2);
               }
               break;
            case BLOCK:
               this.a(var3, var2, false);
               break;
            case RETURN_STMT:
               if (var3.size() > 1) {
                  this.d(var3.get(1), var2);
               }
               break;
            case THROW_STMT:
               this.d(var3.get(1), var2);
               break;
            case EXPR_LIST:
            case EXPR:
               this.d(var3, var2);
               break;
            case FN_EXPR:
               this.a(var3, var2, b(var3));
            case EOS:
               break;
            default:
               a("DIALECT_STATEMENT", var3, var3.type.name());
         }
      }
   }

   private void c(Node var1, io.karatelabs.ext.rules.analysis.CalcDialectAnalyzer.DialectAnalysisScope var2) {
      Node var3 = var1.getFirst();
      if (var3.isToken() && var3.token.type == TokenType.VAR) {
         a("DIALECT_VAR", var1, "'var' is function-scoped and hoisted");
      }

      boolean var4 = var3.isToken() && var3.token.type == TokenType.CONST;
      if (var1.findImmediateChildren(NodeType.VAR_DECL).size() > 1) {
         a("DIALECT_STATEMENT", var1, "a declaration binds one identifier");
      }

      for (int var5 = 0; var5 < var1.size(); var5++) {
         Node var6 = var1.get(var5);
         if (var6.type == NodeType.VAR_DECL) {
            Node var7 = var6.getFirst();
            if (!var7.isToken()) {
               a("DIALECT_EXPRESSION", var6, "a destructuring declaration");
            }

            String var8 = var7.getText();
            if (var4) {
               var2.f.add(var8);
            }

            if (var6.size() < 3) {
               if (var4) {
                  a("DIALECT_STATEMENT", var6, "a const without an initializer");
               }

               var2.e.put(var8, null);
            } else {
               Node var9 = d(var6.get(2));
               if (var9 != null) {
                  if (!var4) {
                     a("DIALECT_STATEMENT", var9, "a function expression bound to a 'let'");
                  }

                  var2.e.put(var8, var9);
                  this.a(var9, var2, var8);
               } else {
                  this.d(var6.get(2), var2);
                  var2.e.put(var8, null);
               }
            }
         }
      }
   }

   private static Node d(Node var0) {
      Node var1 = var0;

      while (!var1.isToken() && (var1.type == NodeType.EXPR || var1.type == NodeType.EXPR_LIST) && var1.size() == 1) {
         var1 = var1.getFirst();
      }

      return !var1.isToken() && var1.type == NodeType.FN_EXPR ? var1 : null;
   }

   private void a(Node var1, io.karatelabs.ext.rules.analysis.CalcDialectAnalyzer.DialectAnalysisScope var2, boolean var3) {
      io.karatelabs.ext.rules.analysis.CalcDialectAnalyzer.DialectAnalysisScope var4 = new io.karatelabs.ext.rules.analysis.CalcDialectAnalyzer.DialectAnalysisScope(var2, var2.b, false, var3);
      this.a(var1, var4);

      for (int var5 = 0; var5 < var1.size(); var5++) {
         Node var6 = var1.get(var5);
         if (!var6.isToken()) {
            this.b(var6, var4);
         }
      }
   }

   private void a(Node var1, io.karatelabs.ext.rules.analysis.CalcDialectAnalyzer.DialectAnalysisScope var2, String var3) {
      if (!var2.d) {
         a("DIALECT_STATEMENT", var1, "a function declared inside a block");
      }

      boolean var4 = var2.c && "execute".equals(var3);
      String var5 = this.j;
      this.j = null;
      io.karatelabs.ext.rules.analysis.CalcDialectAnalyzer.DialectAnalysisScope var6 = new io.karatelabs.ext.rules.analysis.CalcDialectAnalyzer.DialectAnalysisScope(var2, var2.b + 1, false, false);

      for (Node var8 : var1.findImmediateChildren(NodeType.FN_DECL_ARGS)) {
         for (int var9 = 0; var9 < var8.size(); var9++) {
            Node var10 = var8.get(var9);
            if (var10.type == NodeType.FN_DECL_ARG) {
               if (var10.size() < 1 || !var10.getFirst().isToken() || var10.getFirst().token.type != TokenType.IDENT) {
                  a("DIALECT_EXPRESSION", var10, "a default, rest or destructured parameter");
               }

               for (int var11 = 1; var11 < var10.size(); var11++) {
                  if (!var10.get(var11).isToken() || var10.get(var11).token.type != TokenType.COMMA) {
                     a("DIALECT_EXPRESSION", var10, "a default, rest or destructured parameter");
                  }
               }

               String var17 = var10.getFirst().getText();
               if (var4 && this.j == null) {
                  this.j = var17;
               }

               var6.e.put(var17, null);
            }
         }
      }

      Node var15 = var1.getLast();
      if (var15.isToken() || var15.type != NodeType.BLOCK) {
         a("DIALECT_STATEMENT", var1, "an expression-bodied function");
      }

      Node var16 = this.i;
      this.i = var1;

      try {
         this.a(var15, var6, true);
      } finally {
         this.i = var16;
         this.j = var5;
      }
   }

   private void d(Node var1, io.karatelabs.ext.rules.analysis.CalcDialectAnalyzer.DialectAnalysisScope var2) {
      switch (var1.type) {
         case EXPR_LIST:
            var var10 = var1.findImmediateChildren(NodeType.EXPR);
            if (var10.size() > 1) {
               a("DIALECT_EXPRESSION", var1, "the comma operator");
            }

            for (Node var5 : (Iterable<Node>) (Iterable<?>) (var10)) {
               this.d(var5, var2);
            }
            break;
         case EXPR:
            this.d(var1.getFirst(), var2);
            break;
         case FN_EXPR:
            a("DIALECT_STATEMENT", var1, "a function expression outside a const binding");
            break;
         case EOS:
         default:
            a("DIALECT_EXPRESSION", var1, var1.type.name());
            break;
         case PAREN_EXPR:
            this.d(var1.get(1), var2);
            break;
         case LIT_EXPR:
            this.e(var1, var2);
            break;
         case REF_EXPR:
            if (var1.size() > 0 && !var1.getFirst().isToken()) {
               a("DIALECT_STATEMENT", var1, "an arrow function");
            }

            String var9 = var1.getText();
            if (var9.equals("this")) {
               a("DIALECT_EXPRESSION", var1, "this");
            }

            this.b(var9, var1, var2);
            break;
         case REF_DOT_EXPR:
            this.g(var1, var2);
            break;
         case REF_BRACKET_EXPR:
            this.d(var1.get(0), var2);
            this.d(var1.get(2), var2);
            break;
         case FN_CALL_EXPR:
            this.h(var1, var2);
            break;
         case LOGIC_EXPR:
            String var8 = var1.get(1).getText();
            if (!f.contains(var8)) {
               a("DIALECT_EXPRESSION", var1, var8);
            }

            this.d(var1.get(0), var2);
            this.d(var1.get(2), var2);
            break;
         case LOGIC_AND_EXPR:
         case LOGIC_NULLISH_EXPR:
            this.d(var1.get(0), var2);
            this.d(var1.get(2), var2);
            break;
         case LOGIC_TERN_EXPR:
            this.d(var1.get(0), var2);
            this.d(var1.get(2), var2);
            this.d(var1.get(4), var2);
            break;
         case MATH_ADD_EXPR:
         case MATH_MUL_EXPR:
            String var7 = var1.get(1).getText();
            if (!g.contains(var7)) {
               a("DIALECT_EXPRESSION", var1, var7);
            }

            this.d(var1.get(0), var2);
            this.d(var1.get(2), var2);
            break;
         case MATH_PRE_EXPR:
            String var6 = var1.get(0).getText();
            if (!var6.equals("-") && !var6.equals("+")) {
               a("DIALECT_EXPRESSION", var1, var6);
            }

            Node var4 = var1.get(1);
            if (var4.isToken()) {
               this.e(var4);
            } else {
               this.d(var4, var2);
            }
            break;
         case UNARY_EXPR:
            String var3 = var1.getFirst().getText();
            if (!var3.equals("!")) {
               a("DIALECT_EXPRESSION", var1, var3);
            }

            this.d(var1.get(1), var2);
            break;
         case ASSIGN_EXPR:
            this.i(var1, var2);
            break;
         case FN_ARROW_EXPR:
            a("DIALECT_STATEMENT", var1, "an arrow function");
            break;
         case TYPEOF_EXPR:
            a("DIALECT_EXPRESSION", var1, "typeof");
            break;
         case IN_EXPR:
            a("DIALECT_EXPRESSION", var1, "in");
            break;
         case INSTANCEOF_EXPR:
            a("DIALECT_EXPRESSION", var1, "instanceof");
            break;
         case DELETE_EXPR:
            a("DIALECT_EXPRESSION", var1, "delete");
            break;
         case MATH_POST_EXPR:
            a("DIALECT_EXPRESSION", var1, var1.get(1).getText());
            break;
         case MATH_EXP_EXPR:
            a("DIALECT_EXPRESSION", var1, "**");
            break;
         case LOGIC_BIT_EXPR:
            a("DIALECT_EXPRESSION", var1, var1.get(1).getText());
            break;
         case NEW_EXPR:
            a("DIALECT_EXPRESSION", var1, "new");
      }
   }

   private void e(Node var1, io.karatelabs.ext.rules.analysis.CalcDialectAnalyzer.DialectAnalysisScope var2) {
      Node var3 = var1.getFirst();
      if (var3.isToken()) {
         this.e(var3);
      } else {
         switch (var3.type) {
            case LIT_OBJECT:
               for (int var8 = 1; var8 < var3.size() - 1; var8++) {
                  this.f(var3.get(var8), var2);
               }
               break;
            case LIT_ARRAY:
               for (int var7 = 1; var7 < var3.size() - 1; var7++) {
                  Node var9 = var3.get(var7);
                  if (var9.size() > 0 && var9.getFirst().isToken() && var9.getFirst().token.type == TokenType.DOT_DOT_DOT) {
                     a("DIALECT_EXPRESSION", var9, "a spread element");
                  }

                  var var6 = var9.findImmediateChildren(NodeType.EXPR);
                  if (var6.isEmpty()) {
                     a("DIALECT_EXPRESSION", var9, "an array hole");
                  }

                  this.d((Node)var6.get(0), var2);
               }
               break;
            case LIT_TEMPLATE:
               for (Node var5 : c(var3)) {
                  this.d(var5, var2);
               }
               break;
            case LIT_REGEX:
               a("DIALECT_EXPRESSION", var1, "a regular expression");
               break;
            default:
               a("DIALECT_EXPRESSION", var1, var3.type.name());
         }
      }
   }

   static List<Node> c(Node var0) {
      ArrayList<io.karatelabs.parser.Node> var1 = new ArrayList();

      for (int var2 = 0; var2 < var0.size(); var2++) {
         Node var3 = var0.get(var2);
         if (!var3.isToken()) {
            if (var3.type == NodeType.PLACEHOLDER) {
               for (int var4 = 0; var4 < var3.size(); var4++) {
                  if (!var3.get(var4).isToken()) {
                     var1.add(var3.get(var4));
                  }
               }
            } else {
               var1.add(var3);
            }
         }
      }

      return var1;
   }

   private void f(Node var1, io.karatelabs.ext.rules.analysis.CalcDialectAnalyzer.DialectAnalysisScope var2) {
      Node var3 = var1.getFirst();
      if (var3.isToken() && var3.token.type == TokenType.DOT_DOT_DOT) {
         a("DIALECT_EXPRESSION", var1, "a spread element");
      }

      if (var3.isToken() && var3.token.type == TokenType.L_BRACKET) {
         a("DIALECT_EXPRESSION", var1, "a computed key");
      }

      if (!var3.isToken()) {
         a("DIALECT_EXPRESSION", var1, "a computed key");
      }

      if (var3.token.type != TokenType.IDENT && var3.token.type != TokenType.S_STRING && var3.token.type != TokenType.D_STRING) {
         a("DIALECT_EXPRESSION", var1, "a key that is not an identifier or a string");
      }

      int var4 = -1;

      for (int var5 = 1; var5 < var1.size(); var5++) {
         if (!var1.get(var5).isToken()) {
            var4 = var5;
            break;
         }
      }

      if (var4 < 0) {
         this.b(var3.getText(), var1, var2);
      } else {
         if (var4 != 2 || !var1.get(1).isToken() || var1.get(1).token.type != TokenType.COLON) {
            a("DIALECT_EXPRESSION", var1, "a method or accessor in an object literal");
         }

         this.d(var1.get(2), var2);
      }
   }

   private void e(Node var1) {
      if (var1.token.type == TokenType.BIGINT) {
         a("DIALECT_BIGINT", var1, var1.getText());
      }

      Object var2 = Terms.literalValue(var1.token);
      if (var2 instanceof Number var3 && !Double.isFinite(var3.doubleValue())) {
         a("DIALECT_EXPRESSION", var1, "a non-finite number literal");
      }

      if (var2 != null && !(var2 instanceof Number) && !(var2 instanceof String) && !(var2 instanceof Boolean)) {
         a("DIALECT_EXPRESSION", var1, "a literal of type " + var2.getClass().getSimpleName());
      }
   }

   private void g(Node var1, io.karatelabs.ext.rules.analysis.CalcDialectAnalyzer.DialectAnalysisScope var2) {
      if (var1.size() > 1 && (!var1.get(1).isToken() || var1.get(1).token.type == TokenType.QUES_DOT)) {
         a("DIALECT_EXPRESSION", var1, "an optional chain");
      }

      String var3 = var1.get(2).getText();
      String var4 = this.f(var1.get(0));
      if ("calc".equals(var4)) {
         if (!b.contains(var3)) {
            a("DIALECT_HOST", var1, "calc." + var3);
         }
      } else {
         if ("Math".equals(var4)) {
            a("DIALECT_HOST", var1, "Math." + var3);
         }

         this.d(var1.get(0), var2);
      }
   }

   private String f(Node var1) {
      if (var1.type == NodeType.REF_EXPR && var1.size() != 0 && var1.getFirst().isToken()) {
         String var2 = var1.getText();
         if (var2.equals("Math")) {
            return "Math";
         } else {
            return !var2.equals("calc") && !var2.equals(this.j) ? null : "calc";
         }
      } else {
         return null;
      }
   }

   private void h(Node var1, io.karatelabs.ext.rules.analysis.CalcDialectAnalyzer.DialectAnalysisScope var2) {
      Node var3 = var1.get(0);
      ArrayList<io.karatelabs.parser.Node> var4 = new ArrayList();
      Node var5 = var1.get(2);

      for (int var6 = 0; var6 < var5.size(); var6++) {
         Node var7 = var5.get(var6);
         if (var7.type == NodeType.FN_CALL_ARG) {
            var var8 = var7.findImmediateChildren(NodeType.EXPR);
            if (var8.isEmpty()) {
               a("DIALECT_EXPRESSION", var7, "a spread argument");
            }

            var4.add((Node)var8.get(0));
         }
      }

      if (var3.type == NodeType.REF_EXPR) {
         if (var3.size() > 0 && !var3.getFirst().isToken()) {
            a("DIALECT_STATEMENT", var3, "an arrow function");
         }

         io.karatelabs.ext.rules.analysis.CalcDialectAnalyzer.DialectAnalysisScope var10 = this.a(var3.getText(), var2);
         if (var10 == null) {
            a("DIALECT_REFERENCE", var3, var3.getText());
         }

         Node var12 = var10.e.get(var3.getText());
         if (var12 == null) {
            a("DIALECT_EXPRESSION", var1, "a call on a non-function");
         }

         this.b(var3.getText(), var3, var2);
         this.h.computeIfAbsent(this.i, var0 -> new LinkedHashMap<>()).putIfAbsent(var12, j(var1));
         this.a(var4, var2);
      } else {
         if (var3.type != NodeType.REF_DOT_EXPR) {
            a("DIALECT_EXPRESSION", var1, "a call on " + var3.type.name());
         }

         if (var3.size() > 1 && (!var3.get(1).isToken() || var3.get(1).token.type == TokenType.QUES_DOT)) {
            a("DIALECT_EXPRESSION", var3, "an optional chain");
         }

         String var9 = var3.get(2).getText();
         String var11 = this.f(var3.get(0));
         if ("calc".equals(var11)) {
            if (!c.contains(var9)) {
               a("DIALECT_HOST", var1, "calc." + var9);
            }

            this.a(var9, var4, var1, var2);
         } else if ("Math".equals(var11)) {
            Integer var13 = d.get(var9);
            if (var13 == null) {
               a("DIALECT_HOST", var1, "Math." + var9);
            }

            if (var4.size() != var13) {
               a("DIALECT_HOST", var1, "Math." + var9 + " takes " + var13 + " argument(s)");
            }

            this.a(var4, var2);
         } else {
            if (!var9.equals("toFixed")) {
               a("DIALECT_HOST", var1, "a method call '." + var9 + "'");
            }

            if (var4.size() > 1) {
               a("DIALECT_HOST", var1, "toFixed takes at most one argument");
            }

            if (var4.size() == 1) {
               this.a((Node)var4.get(0), 100, var1);
               this.a(var4, var2);
            }

            this.d(var3.get(0), var2);
         }
      }
   }

   private void a(String var1, List<Node> var2, Node var3, io.karatelabs.ext.rules.analysis.CalcDialectAnalyzer.DialectAnalysisScope var4) {
      switch (var1) {
         case "always":
         case "sometimes":
            if (var2.size() < 2 || var2.size() > 3) {
               a("DIALECT_HOST", var3, "calc." + var1 + " takes a name, a predicate and optional options");
            }

            this.a((Node)var2.get(0), var3, "the property name");
            if (var2.size() == 3) {
               this.a((Node)var2.get(2), var3);
            }
            break;
         case "levels":
            if (var2.size() != 2) {
               a("DIALECT_HOST", var3, "calc.levels takes a name and an array of numbers");
            }

            this.a((Node)var2.get(0), var3, "the levels path");
            this.b((Node)var2.get(1), var3);
            break;
         case "round":
            if (var2.isEmpty() || var2.size() > 2) {
               a("DIALECT_HOST", var3, "calc.round takes a value and an optional precision");
            }

            if (var2.size() == 2) {
               this.a((Node)var2.get(1), 12, var3);
            }
      }

      this.a(var2, var4);
   }

   private void a(List<Node> var1, io.karatelabs.ext.rules.analysis.CalcDialectAnalyzer.DialectAnalysisScope var2) {
      for (Node var4 : var1) {
         this.d(var4, var2);
      }
   }

   private void a(Node var1, Node var2, String var3) {
      if (!(i(var1) instanceof String)) {
         a("DIALECT_HOST", var2, var3 + " must be a string literal");
      }
   }

   private void a(Node var1, Node var2) {
      Node var3 = h(var1);
      Node var4 = !var3.isToken() && var3.type == NodeType.LIT_EXPR && !var3.getFirst().isToken() && var3.getFirst().type == NodeType.LIT_OBJECT
         ? var3.getFirst()
         : null;
      if (var4 == null) {
         a("DIALECT_HOST", var2, "the property options must be an object literal");
      }

      for (int var5 = 1; var5 < var4.size() - 1; var5++) {
         Node var6 = var4.get(var5);
         if ("req".equals(g(var6)) && (var6.size() < 3 || !(i(var6.get(2)) instanceof String))) {
            a("DIALECT_HOST", var2, "req must be a string literal");
         }
      }
   }

   private static String g(Node var0) {
      Node var1 = var0.getFirst();
      if (!var1.isToken()) {
         return null;
      } else {
         return switch (var1.token.type) {
            case IDENT -> var1.getText();
            case S_STRING, D_STRING -> String.valueOf(Terms.literalValue(var1.token));
            default -> null;
         };
      }
   }

   private void b(Node var1, Node var2) {
      Node var3 = h(var1);
      Node var4 = !var3.isToken() && var3.type == NodeType.LIT_EXPR && !var3.getFirst().isToken() && var3.getFirst().type == NodeType.LIT_ARRAY
         ? var3.getFirst()
         : null;
      if (var4 == null) {
         a("DIALECT_HOST", var2, "calc.levels takes an array literal of number literals");
      }

      for (int var5 = 1; var5 < var4.size() - 1; var5++) {
         var var6 = var4.get(var5).findImmediateChildren(NodeType.EXPR);
         if (var6.isEmpty() || !(i((Node)var6.get(0)) instanceof Number)) {
            a("DIALECT_HOST", var2, "calc.levels takes an array literal of number literals");
         }
      }
   }

   private void a(Node var1, int var2, Node var3) {
      double var5 = i(var1) instanceof Number var7 ? var7.doubleValue() : Double.NaN;
      if (var5 != Math.rint(var5) || var5 < 0.0 || var5 > var2) {
         a("DIALECT_PRECISION", var3, "a precision must be a literal integer in 0.." + var2);
      }
   }

   private static Node h(Node var0) {
      Node var1 = var0;

      while (!var1.isToken() && var1.size() > 0) {
         if (var1.type == NodeType.PAREN_EXPR) {
            var1 = var1.get(1);
         } else if (var1.type == NodeType.EXPR) {
            var1 = var1.getFirst();
         } else {
            if (var1.type != NodeType.EXPR_LIST) {
               return var1;
            }

            var var2 = var1.findImmediateChildren(NodeType.EXPR);
            if (var2.size() != 1) {
               return var1;
            }

            var1 = (Node)var2.get(0);
         }
      }

      return var1;
   }

   private static Object i(Node var0) {
      Node var1 = h(var0);
      if (!var1.isToken() && var1.type == NodeType.LIT_EXPR && var1.getFirst().isToken()) {
         return var1.getFirst().token.type == TokenType.BIGINT ? null : Terms.literalValue(var1.getFirst().token);
      } else {
         return null;
      }
   }

   private void i(Node var1, io.karatelabs.ext.rules.analysis.CalcDialectAnalyzer.DialectAnalysisScope var2) {
      String var3 = var1.get(1).getText();
      if (!e.contains(var3)) {
         a("DIALECT_EXPRESSION", var1, var3);
      }

      Node var4 = var1.get(0);
      switch (var4.type) {
         case REF_EXPR:
            this.b(var4.getText(), var4, var2);
            this.a(var4.getText(), var4, var2);
            break;
         case REF_DOT_EXPR:
            if (var4.size() > 1 && (!var4.get(1).isToken() || var4.get(1).token.type == TokenType.QUES_DOT)) {
               a("DIALECT_EXPRESSION", var4, "an optional chain");
            }

            String var5 = this.f(var4.get(0));
            if (var5 != null && (!"calc".equals(var5) || !var4.get(2).getText().equals("output"))) {
               a("DIALECT_HOST", var4, var5 + "." + var4.get(2).getText() + " is not assignable");
            }

            if (var5 == null) {
               this.d(var4.get(0), var2);
            }
            break;
         default:
            a("DIALECT_EXPRESSION", var1, "assignment to " + var4.type.name());
      }

      this.d(var1.get(2), var2);
   }

   private void a(String var1, Node var2, io.karatelabs.ext.rules.analysis.CalcDialectAnalyzer.DialectAnalysisScope var3) {
      io.karatelabs.ext.rules.analysis.CalcDialectAnalyzer.DialectAnalysisScope var4 = this.a(var1, var3);
      if (var4 != null) {
         if (var4.e.get(var1) != null) {
            a("DIALECT_CONST", var2, "'" + var1 + "' is a function binding");
         }

         if (var4.f.contains(var1)) {
            a("DIALECT_CONST", var2, "'" + var1 + "' is a const binding");
         }
      }
   }

   private io.karatelabs.ext.rules.analysis.CalcDialectAnalyzer.DialectAnalysisScope a(String var1, io.karatelabs.ext.rules.analysis.CalcDialectAnalyzer.DialectAnalysisScope var2) {
      for (io.karatelabs.ext.rules.analysis.CalcDialectAnalyzer.DialectAnalysisScope var3 = var2; var3 != null; var3 = var3.a) {
         if (var3.e.containsKey(var1)) {
            return var3;
         }
      }

      return null;
   }

   private void b(String var1, Node var2, io.karatelabs.ext.rules.analysis.CalcDialectAnalyzer.DialectAnalysisScope var3) {
      if (var1.equals("calc") || var1.equals("Math") || var1.equals(this.j)) {
         a("DIALECT_HOST", var2, var1 + " is only the base of a member access");
      }

      io.karatelabs.ext.rules.analysis.CalcDialectAnalyzer.DialectAnalysisScope var4 = this.a(var1, var3);
      if (var4 == null) {
         if (!a.contains(var1)) {
            a("DIALECT_REFERENCE", var2, var1);
         }
      } else {
         if (!var4.c && var4.b != var3.b) {
            a("DIALECT_CAPTURE", var2, "'" + var1 + "' is bound outside the program scope");
         }
      }
   }

   private void a() {
      HashSet var1 = new HashSet();
      HashSet var2 = new HashSet();

      for (Node var4 : new ArrayList<>(this.h.keySet())) {
         this.a(var4, var1, var2);
      }
   }

   private void a(Node var1, Set<Node> var2, Set<Node> var3) {
      if (!var2.contains(var1)) {
         var3.add(var1);

         for (Entry var5 : this.h.getOrDefault(var1, Map.of()).entrySet()) {
            if (var3.contains(var5.getKey())) {
               throw new RuleAnalysisException("DIALECT_RECURSION", (Integer)var5.getValue(), "'" + b((Node)var5.getKey()) + "' is reachable from itself");
            }

            this.a((Node)var5.getKey(), var2, var3);
         }

         var3.remove(var1);
         var2.add(var1);
      }
   }

   private static int j(Node var0) {
      return var0.getFirstToken().line + 1;
   }

   private static void a(String var0, Node var1, String var2) {
      throw new RuleAnalysisException(var0, j(var1), var2, var2);
   }

   public record CalcDialectAnalysis(Node a, Map<String, Node> b) {

      public CalcDialectAnalysis(Node a, Map<String, Node> b) {
         this.a = a;
         this.b = b;
      }
   }

   private static final class DialectAnalysisScope {
      final io.karatelabs.ext.rules.analysis.CalcDialectAnalyzer.DialectAnalysisScope a;
      final int b;
      final boolean c;
      final boolean d;
      final Map<String, Node> e = new HashMap<>();
      final Set<String> f = new HashSet<>();

      DialectAnalysisScope(io.karatelabs.ext.rules.analysis.CalcDialectAnalyzer.DialectAnalysisScope var1, int var2, boolean var3, boolean var4) {
         this.a = var1;
         this.b = var2;
         this.c = var3;
         this.d = var4;
      }
   }
}
