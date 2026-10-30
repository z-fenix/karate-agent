package io.karatelabs.ext.rules.analysis;

import io.karatelabs.js.JsScalars;
import io.karatelabs.js.Terms;
import io.karatelabs.parser.Node;
import io.karatelabs.parser.NodeType;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.Map.Entry;

public final class AbstractExpressionEvaluator {
   private final Node a;
   private final Map<String, io.karatelabs.ext.rules.analysis.AbstractValue> b;
   private final Set<Integer> c;
   private final io.karatelabs.ext.rules.analysis.AbstractBox d;
   private final String e;
   private final Map<String, Integer> f = new HashMap<>();
   private final IdentityHashMap<io.karatelabs.ext.rules.analysis.AbstractValue, io.karatelabs.ext.rules.analysis.AbstractExpressionEvaluator.ExpressionProvenance> g = new IdentityHashMap<>();
   private final Set<Map<String, io.karatelabs.ext.rules.analysis.AbstractValue>> h = Collections.newSetFromMap(new IdentityHashMap<>());
   private final io.karatelabs.ext.rules.analysis.AbstractExpressionEvaluator.EvaluationListener i;
   private final IdentityHashMap<Map<String, io.karatelabs.ext.rules.analysis.AbstractValue>, List<LinkedHashMap<String, io.karatelabs.ext.rules.analysis.AbstractValue>>> j = new IdentityHashMap<>();
   private Deque<Map<String, io.karatelabs.ext.rules.analysis.AbstractValue>> k = new ArrayDeque<>();
   private int l;
   private final Map<Integer, Set<Boolean>> m = new HashMap<>();
   private final List<io.karatelabs.ext.rules.analysis.AbstractExpressionEvaluator.PropertyObservation> n = new ArrayList<>();
   private long o;
   private io.karatelabs.ext.rules.analysis.AbstractValue p = new io.karatelabs.ext.rules.analysis.AbstractValue.NullAbstractValue();
   private io.karatelabs.ext.rules.analysis.AbstractValue q = new io.karatelabs.ext.rules.analysis.AbstractValue.NullAbstractValue();
   private boolean r;

   private AbstractExpressionEvaluator(Node var1, Map<String, io.karatelabs.ext.rules.analysis.AbstractValue> var2, Set<Integer> var3, io.karatelabs.ext.rules.analysis.AbstractBox var4, String var5, io.karatelabs.ext.rules.analysis.AbstractExpressionEvaluator.EvaluationListener var6) {
      this.a = var1;
      this.b = var2;
      this.c = var3;
      this.d = var4;
      this.e = var5;
      this.i = var6;

      for (int var7 = 0; var7 < var4.a.size(); var7++) {
         this.f.put(var4.a.get(var7).c(), var7);
      }
   }

   public static AbstractExpressionEvaluator.EvaluationResult a(io.karatelabs.ext.rules.analysis.CalcDialectAnalyzer.CalcDialectAnalysis var0, Set<Integer> var1, io.karatelabs.ext.rules.analysis.AbstractBox var2, String var3) {
      return a(var0, Map.of(), var1, var2, var3);
   }

   public static AbstractExpressionEvaluator.EvaluationResult a(io.karatelabs.ext.rules.analysis.CalcDialectAnalyzer.CalcDialectAnalysis var0, Map<String, io.karatelabs.ext.rules.analysis.AbstractValue> var1, Set<Integer> var2, io.karatelabs.ext.rules.analysis.AbstractBox var3, String var4) {
      return a(var0, var1, var2, var3, var4, io.karatelabs.ext.rules.analysis.AbstractExpressionEvaluator.EvaluationListener.i);
   }

   public static AbstractExpressionEvaluator.EvaluationResult a(
      io.karatelabs.ext.rules.analysis.CalcDialectAnalyzer.CalcDialectAnalysis var0, Map<String, io.karatelabs.ext.rules.analysis.AbstractValue> var1, Set<Integer> var2, io.karatelabs.ext.rules.analysis.AbstractBox var3, String var4, io.karatelabs.ext.rules.analysis.AbstractExpressionEvaluator.EvaluationListener var5
   ) {
      return new AbstractExpressionEvaluator(var0.a(), var1, var2, var3, var4, var5).a();
   }

   public static Map<String, io.karatelabs.ext.rules.analysis.AbstractValue> a(Map<String, Object> var0) {
      LinkedHashMap var1 = new LinkedHashMap();

      for (Entry var3 : var0.entrySet()) {
         var1.put((String)var3.getKey(), a(var3.getValue()));
      }

      return Collections.unmodifiableMap(var1);
   }

   private static io.karatelabs.ext.rules.analysis.AbstractValue a(Object var0) {
      if (var0 == null) {
         return new io.karatelabs.ext.rules.analysis.AbstractValue.NullAbstractValue();
      } else if (var0 instanceof Number var6) {
         return io.karatelabs.ext.rules.analysis.AbstractValue.NumericIntervalAbstractValue.a(var6.doubleValue());
      } else if (var0 instanceof String || var0 instanceof Boolean) {
         return io.karatelabs.ext.rules.analysis.AbstractValue.ScalarSetAbstractValue.a(var0);
      } else if (var0 instanceof Map var5) {
         LinkedHashMap var7 = new LinkedHashMap();

         for (Entry var9 : (Iterable<Entry>) (Iterable<?>) (var5.entrySet())) {
            var7.put(String.valueOf(var9.getKey()), a(var9.getValue()));
         }

         return new io.karatelabs.ext.rules.analysis.AbstractValue.ObjectAbstractValue(var7, 0L, true);
      } else if (!(var0 instanceof List var1)) {
         throw new io.karatelabs.ext.rules.analysis.RuleAnalysisException("COERCION", -1, "a table cell of " + var0.getClass().getSimpleName());
      } else {
         ArrayList var2 = new ArrayList();

         for (Object var4 : var1) {
            var2.add(a(var4));
         }

         return new io.karatelabs.ext.rules.analysis.AbstractValue.ArrayAbstractValue(var2);
      }
   }

   private AbstractExpressionEvaluator.EvaluationResult a() {
      HashMap var1 = new HashMap();
      var1.put("calc", new io.karatelabs.ext.rules.analysis.AbstractValue.SymbolicReferenceValue("calc"));
      this.k.push(var1);
      boolean var2 = false;
      int var3 = -1;

      try {
         this.a(this.a);

         for (int var4 = 0; var4 < this.a.size(); var4++) {
            Node var5 = this.a.get(var4);
            if (!var5.isToken() && !b(var5)) {
               this.c(var5);
            }
         }

         if (!this.b.isEmpty()) {
            this.b(var1);
         }

         if (!(this.a("execute", this.a) instanceof io.karatelabs.ext.rules.analysis.AbstractValue.FunctionAbstractValue var7)) {
            throw new IllegalStateException("admission bound no 'execute' function");
         }

         this.a(var7, List.of(new io.karatelabs.ext.rules.analysis.AbstractValue.SymbolicReferenceValue("calc")));
      } catch (io.karatelabs.ext.rules.analysis.AbstractExpressionEvaluator.AbstractThrowException var6) {
         var2 = true;
         var3 = var6.a;
      }

      return new AbstractExpressionEvaluator.EvaluationResult(this.m, this.n, var2, var3, this.o, this.p, this.r);
   }

   private void b(Map<String, io.karatelabs.ext.rules.analysis.AbstractValue> var1) {
      io.karatelabs.ext.rules.analysis.AbstractValue var2 = (io.karatelabs.ext.rules.analysis.AbstractValue)var1.get("lookup");
      if (var2 == null) {
         var1.put("lookup", new io.karatelabs.ext.rules.analysis.AbstractValue.ObjectAbstractValue(new LinkedHashMap<>(this.b), 0L, true));
      } else {
         if (!(var2 instanceof io.karatelabs.ext.rules.analysis.AbstractValue.ObjectAbstractValue var3)) {
            throw new IllegalStateException("'lookup' is bound to a " + g(var2));
         }

         var3.b().putAll(this.b);
      }
   }

   private void a(Node var1) {
      for (int var2 = 0; var2 < var1.size(); var2++) {
         Node var3 = var1.get(var2);
         if (b(var3)) {
            this.k.peek().put(io.karatelabs.ext.rules.analysis.CalcDialectAnalyzer.b(var3.getFirst()), new io.karatelabs.ext.rules.analysis.AbstractValue.FunctionAbstractValue(var3.getFirst()));
         }
      }
   }

   private static boolean b(Node var0) {
      if (var0.type == NodeType.STATEMENT && var0.size() != 0) {
         Node var1 = var0.getFirst();
         return var1.type == NodeType.FN_EXPR && io.karatelabs.ext.rules.analysis.CalcDialectAnalyzer.b(var1) != null;
      } else {
         return false;
      }
   }

   private void c(Node var1) {
      Node var2 = var1.type == NodeType.STATEMENT ? var1.getFirst() : var1;
      if (!var2.isToken()) {
         switch (var2.type) {
            case VAR_STMT:
               this.d(var2);
               break;
            case IF_STMT:
               TernaryBoolean var3 = this.a(var2, this.f(var2.get(2)));
               if (var3 == io.karatelabs.ext.rules.analysis.TernaryBoolean.TRUE) {
                  this.c(var2.get(4));
               } else if (var2.size() > 5) {
                  this.c(var2.get(6));
               }
               break;
            case BLOCK:
               this.e(var2);
               break;
            case RETURN_STMT:
               throw new io.karatelabs.ext.rules.analysis.AbstractExpressionEvaluator.AbstractReturnException((io.karatelabs.ext.rules.analysis.AbstractValue)(var2.size() > 1 ? this.f(var2.get(1)) : new io.karatelabs.ext.rules.analysis.AbstractValue.UndefinedAbstractValue()));
            case THROW_STMT:
               this.f(var2.get(1));
               throw new io.karatelabs.ext.rules.analysis.AbstractExpressionEvaluator.AbstractThrowException(n(var2));
            case EXPR_LIST:
            case EXPR:
               this.f(var2);
            case FN_EXPR:
            case EOS:
               break;
            default:
               throw o(var2);
         }
      }
   }

   private void d(Node var1) {
      for (int var2 = 0; var2 < var1.size(); var2++) {
         Node var3 = var1.get(var2);
         if (var3.type == NodeType.VAR_DECL) {
            Object var4 = var3.size() > 2 ? this.f(var3.get(2)) : new io.karatelabs.ext.rules.analysis.AbstractValue.UndefinedAbstractValue();
            this.k.peek().put(var3.getFirst().getText(), (io.karatelabs.ext.rules.analysis.AbstractValue)var4);
         }
      }
   }

   private void e(Node var1) {
      this.k.push(new HashMap<>());

      try {
         this.a(var1);

         for (int var2 = 0; var2 < var1.size(); var2++) {
            Node var3 = var1.get(var2);
            if (var3.type == NodeType.STATEMENT && !b(var3)) {
               this.c(var3);
            }
         }
      } finally {
         this.k.pop();
      }
   }

   private io.karatelabs.ext.rules.analysis.AbstractValue f(Node var1) {
      switch (var1.type) {
         case EXPR_LIST:
            Object var13 = new io.karatelabs.ext.rules.analysis.AbstractValue.UndefinedAbstractValue();

            for (int var21 = 0; var21 < var1.size(); var21++) {
               if (var1.get(var21).type == NodeType.EXPR) {
                  var13 = this.f(var1.get(var21));
               }
            }

            return (io.karatelabs.ext.rules.analysis.AbstractValue)var13;
         case EXPR:
            return this.f(var1.getFirst());
         case FN_EXPR:
            return new io.karatelabs.ext.rules.analysis.AbstractValue.FunctionAbstractValue(var1);
         case EOS:
         default:
            throw o(var1);
         case PAREN_EXPR:
            return this.f(var1.get(1));
         case LIT_EXPR:
            return this.g(var1);
         case REF_EXPR:
            return this.a(var1.getText(), var1);
         case REF_DOT_EXPR:
            return this.a(this.f(var1.get(0)), var1.get(2).getText(), var1);
         case REF_BRACKET_EXPR:
            return this.a(this.f(var1.get(0)), this.f(var1.get(2)), var1);
         case FN_CALL_EXPR:
            return this.l(var1);
         case LOGIC_EXPR:
            io.karatelabs.ext.rules.analysis.AbstractValue var12 = this.f(var1.get(0));
            io.karatelabs.ext.rules.analysis.AbstractValue var20 = this.f(var1.get(2));
            String var25 = var1.get(1).getText();
            io.karatelabs.ext.rules.analysis.AbstractValue var26 = a(this.c(var12, var20, var25, var1), var12.a() | var20.a());
            this.g.put(var26, new io.karatelabs.ext.rules.analysis.AbstractExpressionEvaluator.ExpressionProvenance(var25, var12, var20, n(var1)));
            return var26;
         case LOGIC_AND_EXPR:
            io.karatelabs.ext.rules.analysis.AbstractValue var11 = this.f(var1.get(0));
            boolean var19 = var1.get(1).getText().equals("&&");
            if (this.i(var1) && this.a(var11) == io.karatelabs.ext.rules.analysis.TernaryBoolean.MAYBE && !k(var1.get(2))) {
               io.karatelabs.ext.rules.analysis.AbstractValue var23 = this.j(var1.get(2));
               if (var23 != null && d(var11) && d(var23)) {
                  TernaryBoolean var5 = this.a(var23);
                  TernaryBoolean var6 = var19
                     ? (var5 == io.karatelabs.ext.rules.analysis.TernaryBoolean.FALSE ? io.karatelabs.ext.rules.analysis.TernaryBoolean.FALSE : io.karatelabs.ext.rules.analysis.TernaryBoolean.MAYBE)
                     : (var5 == io.karatelabs.ext.rules.analysis.TernaryBoolean.TRUE ? io.karatelabs.ext.rules.analysis.TernaryBoolean.TRUE : io.karatelabs.ext.rules.analysis.TernaryBoolean.MAYBE);
                  return a(var6, var11.a() | var23.a());
               }
            }

            TernaryBoolean var24 = this.a(var1, var11);
            if (var19) {
               return var24 == io.karatelabs.ext.rules.analysis.TernaryBoolean.TRUE ? this.f(var1.get(2)) : var11;
            }

            return var24 == io.karatelabs.ext.rules.analysis.TernaryBoolean.TRUE ? var11 : this.f(var1.get(2));
         case LOGIC_NULLISH_EXPR:
            io.karatelabs.ext.rules.analysis.AbstractValue var10 = this.f(var1.get(0));
            boolean var18 = !(var10 instanceof io.karatelabs.ext.rules.analysis.AbstractValue.NullAbstractValue) && !(var10 instanceof io.karatelabs.ext.rules.analysis.AbstractValue.UndefinedAbstractValue);
            this.a(var1, io.karatelabs.ext.rules.analysis.TernaryBoolean.of(var18), var10.a());
            return var18 ? var10 : this.f(var1.get(2));
         case LOGIC_TERN_EXPR:
            io.karatelabs.ext.rules.analysis.AbstractValue var9 = this.f(var1.get(0));
            if (this.i(var1) && this.a(var9) == io.karatelabs.ext.rules.analysis.TernaryBoolean.MAYBE && !k(var1.get(2)) && !k(var1.get(4))) {
               io.karatelabs.ext.rules.analysis.AbstractValue var16 = this.j(var1.get(2));
               io.karatelabs.ext.rules.analysis.AbstractValue var22 = var16 == null ? null : this.j(var1.get(4));
               if (var22 != null) {
                  return a(this.a(List.of(var16, var22), var1), var9.a());
               }
            }

            TernaryBoolean var17 = this.a(var1, var9);
            return var17 == io.karatelabs.ext.rules.analysis.TernaryBoolean.TRUE ? this.f(var1.get(2)) : this.f(var1.get(4));
         case MATH_ADD_EXPR:
         case MATH_MUL_EXPR:
            io.karatelabs.ext.rules.analysis.AbstractValue var8 = this.f(var1.get(0));
            io.karatelabs.ext.rules.analysis.AbstractValue var15 = this.f(var1.get(2));
            return this.b(var8, var15, var1.get(1).getText(), var1);
         case MATH_PRE_EXPR:
            Node var7 = var1.get(1).isToken() ? var1.get(1) : var1.get(1).getFirst();
            io.karatelabs.ext.rules.analysis.AbstractValue var14 = var7.isToken() ? this.h(var7) : this.f(var7);
            if (var1.get(0).getText().equals("-")) {
               return new io.karatelabs.ext.rules.analysis.AbstractValue.NumericIntervalAbstractValue(this.a(var14, var1).b(), var14.a());
            }

            this.a(var14, var1);
            return var14;
         case UNARY_EXPR:
            io.karatelabs.ext.rules.analysis.AbstractValue var2 = this.f(var1.get(1));
            io.karatelabs.ext.rules.analysis.AbstractValue var3 = a(this.a(var2).not(), var2.a());
            io.karatelabs.ext.rules.analysis.AbstractExpressionEvaluator.ExpressionProvenance var4 = this.g.get(var2);
            if (var4 != null) {
               this.g.put(var3, var4);
            }

            return var3;
         case ASSIGN_EXPR:
            return this.m(var1);
      }
   }

   private io.karatelabs.ext.rules.analysis.AbstractValue g(Node var1) {
      Node var2 = var1.getFirst();
      if (var2.isToken()) {
         return this.h(var2);
      } else {
         switch (var2.type) {
            case LIT_OBJECT:
               LinkedHashMap var12 = new LinkedHashMap();

               for (int var13 = 1; var13 < var2.size() - 1; var13++) {
                  Node var14 = var2.get(var13);
                  Node var15 = var14.getFirst();
                  Object var16 = Terms.literalValue(var15.token);
                  String var8 = var16 == null ? var15.getText() : String.valueOf(var16);
                  boolean var9 = true;

                  for (int var10 = 1; var10 < var14.size(); var10++) {
                     var9 &= var14.get(var10).isToken();
                  }

                  var12.put(var8, var9 ? this.a(var15.getText(), var14) : this.f(var14.get(2)));
               }

               return io.karatelabs.ext.rules.analysis.AbstractValue.ObjectAbstractValue.a(var12);
            case LIT_ARRAY:
               ArrayList var11 = new ArrayList();

               for (int var4 = 1; var4 < var2.size() - 1; var4++) {
                  var11.add(this.f(var2.get(var4).getFirst()));
               }

               return new io.karatelabs.ext.rules.analysis.AbstractValue.ArrayAbstractValue(var11);
            case LIT_TEMPLATE:
               long var3 = 0L;

               for (Node var6 : io.karatelabs.ext.rules.analysis.CalcDialectAnalyzer.c(var2)) {
                  io.karatelabs.ext.rules.analysis.AbstractValue var7 = this.f(var6);
                  if (!e(var7)) {
                     throw new io.karatelabs.ext.rules.analysis.RuleAnalysisException("COERCION", n(var6), "a " + g(var7) + " in a template substitution");
                  }

                  var3 |= var7.a();
               }

               return new io.karatelabs.ext.rules.analysis.AbstractValue.DerivedStringAbstractValue(var3);
            default:
               throw o(var1);
         }
      }
   }

   private io.karatelabs.ext.rules.analysis.AbstractValue h(Node var1) {
      Object var2 = Terms.literalValue(var1.token);
      if (var2 instanceof Number var3) {
         return io.karatelabs.ext.rules.analysis.AbstractValue.NumericIntervalAbstractValue.a(var3.doubleValue());
      } else if (var2 instanceof String || var2 instanceof Boolean) {
         return io.karatelabs.ext.rules.analysis.AbstractValue.ScalarSetAbstractValue.a(var2);
      } else if (var2 == null) {
         return (io.karatelabs.ext.rules.analysis.AbstractValue)(var1.getText().equals("undefined") ? new io.karatelabs.ext.rules.analysis.AbstractValue.UndefinedAbstractValue() : new io.karatelabs.ext.rules.analysis.AbstractValue.NullAbstractValue());
      } else {
         throw o(var1);
      }
   }

   private io.karatelabs.ext.rules.analysis.AbstractValue a(String var1, Node var2) {
      for (Map var4 : this.k) {
         io.karatelabs.ext.rules.analysis.AbstractValue var5 = (io.karatelabs.ext.rules.analysis.AbstractValue)var4.get(var1);
         if (var5 != null) {
            return var5;
         }
      }
      return (io.karatelabs.ext.rules.analysis.AbstractValue)(switch (var1) {
         case "Math" -> new io.karatelabs.ext.rules.analysis.AbstractValue.SymbolicReferenceValue("Math");
         case "undefined" -> new io.karatelabs.ext.rules.analysis.AbstractValue.UndefinedAbstractValue();
         default -> throw new IllegalStateException("admission left '" + var1 + "' unbound at line " + n(var2));
      });
   }

   private io.karatelabs.ext.rules.analysis.AbstractValue a(io.karatelabs.ext.rules.analysis.AbstractValue var1, String var2, Node var3) {
      if (var1 instanceof io.karatelabs.ext.rules.analysis.AbstractValue.SymbolicReferenceValue var10) {
         String var11 = var10.b();

         return (io.karatelabs.ext.rules.analysis.AbstractValue)(switch (var11) {
            case "calc" -> {
               switch (var2) {
                  case "input":
                     yield new io.karatelabs.ext.rules.analysis.AbstractValue.SymbolicReferenceValue("input");
                  case "output":
                     yield this.p;
                  default:
                     throw o(var3);
               }
            }
            case "input" -> this.b(var2, var3);
            default -> throw o(var3);
         });
      } else if (var1 instanceof io.karatelabs.ext.rules.analysis.AbstractValue.ObjectAbstractValue var9) {
         io.karatelabs.ext.rules.analysis.AbstractValue var5 = var9.b().get(var2);
         if (var5 == null) {
            throw new io.karatelabs.ext.rules.analysis.RuleAnalysisException("LOOKUP_MISS", n(var3), "no property '" + var2 + "'");
         } else {
            this.a(var9, List.of(var2));
            return a(var5, var9.c());
         }
      } else if (var1 instanceof io.karatelabs.ext.rules.analysis.AbstractValue.ArrayAbstractValue var4 && var2.equals("length")) {
         return io.karatelabs.ext.rules.analysis.AbstractValue.NumericIntervalAbstractValue.a(var4.b().size());
      } else if (!(var1 instanceof io.karatelabs.ext.rules.analysis.AbstractValue.NullAbstractValue) && !(var1 instanceof io.karatelabs.ext.rules.analysis.AbstractValue.UndefinedAbstractValue)) {
         throw new io.karatelabs.ext.rules.analysis.RuleAnalysisException("COERCION", n(var3), "member '" + var2 + "' of a " + g(var1));
      } else {
         throw new io.karatelabs.ext.rules.analysis.RuleAnalysisException("NULL_MEMBER", n(var3), "reading '" + var2 + "' of null/undefined");
      }
   }

   private io.karatelabs.ext.rules.analysis.AbstractValue b(String var1, Node var2) {
      Integer var3 = this.f.get(var1);
      if (var3 == null) {
         throw new io.karatelabs.ext.rules.analysis.RuleAnalysisException("UNKNOWN_INPUT", n(var2), "input." + var1 + " is not a declared axis");
      } else if (this.d.e(var3)) {
         throw this.a(1L << var3, null);
      } else {
         return this.d.d(var3);
      }
   }

   private io.karatelabs.ext.rules.analysis.AbstractValue a(io.karatelabs.ext.rules.analysis.AbstractValue var1, io.karatelabs.ext.rules.analysis.AbstractValue var2, Node var3) {
      if (var1 instanceof io.karatelabs.ext.rules.analysis.AbstractValue.NullAbstractValue || var1 instanceof io.karatelabs.ext.rules.analysis.AbstractValue.UndefinedAbstractValue) {
         throw new io.karatelabs.ext.rules.analysis.RuleAnalysisException("NULL_MEMBER", n(var3), "indexing null/undefined");
      } else if (var1 instanceof io.karatelabs.ext.rules.analysis.AbstractValue.ObjectAbstractValue var11) {
         if (var2 instanceof io.karatelabs.ext.rules.analysis.AbstractValue.ScalarSetAbstractValue var14) {
            ArrayList var16 = new ArrayList();
            ArrayList var17 = new ArrayList();

            for (Object var9 : var14.d()) {
               io.karatelabs.ext.rules.analysis.AbstractValue var10 = var11.b().get(String.valueOf(var9));
               if (var10 == null) {
                  throw new io.karatelabs.ext.rules.analysis.RuleAnalysisException("LOOKUP_MISS", n(var3), "no property '" + var9 + "'");
               }

               var16.add(var10);
               var17.add(String.valueOf(var9));
            }

            this.a(var11, var17);
            return a(this.a(var16, var3), var14.a() | var11.c());
         } else if (var2 instanceof io.karatelabs.ext.rules.analysis.AbstractValue.NumericIntervalAbstractValue var12 && var12.b().a()) {
            String var15 = a(var12.b().g());
            io.karatelabs.ext.rules.analysis.AbstractValue var7 = var11.b().get(var15);
            if (var7 == null) {
               throw new io.karatelabs.ext.rules.analysis.RuleAnalysisException("LOOKUP_MISS", n(var3), "no property '" + var15 + "'");
            } else {
               this.a(var11, List.of(var15));
               return a(var7, var12.a() | var11.c());
            }
         } else if (var2 instanceof io.karatelabs.ext.rules.analysis.AbstractValue.NumericIntervalAbstractValue var13 && var13.a() != 0L) {
            throw this.a(var13.a(), new io.karatelabs.ext.rules.analysis.AbstractExpressionEvaluator.ExpressionProvenance(null, var13, null, n(var3)));
         } else if (!(var2 instanceof io.karatelabs.ext.rules.analysis.AbstractValue.DerivedStringAbstractValue) && !(var2 instanceof io.karatelabs.ext.rules.analysis.AbstractValue.NumericIntervalAbstractValue)) {
            throw new io.karatelabs.ext.rules.analysis.RuleAnalysisException("COERCION", n(var3), "an index of kind " + g(var2));
         } else {
            throw new io.karatelabs.ext.rules.analysis.RuleAnalysisException("LOOKUP_INDEX_TOP", n(var3), "an index the evaluator no longer tracks");
         }
      } else if (var1 instanceof io.karatelabs.ext.rules.analysis.AbstractValue.ArrayAbstractValue var4) {
         if (var2 instanceof io.karatelabs.ext.rules.analysis.AbstractValue.NumericIntervalAbstractValue var5 && var5.b().a() && var5.b().i()) {
            int var6 = (int)var5.b().g();
            if (var6 >= 0 && var6 < var4.b().size()) {
               return a(var4.b().get(var6), var5.a());
            } else {
               throw new io.karatelabs.ext.rules.analysis.RuleAnalysisException("LOOKUP_MISS", n(var3), "index " + var6);
            }
         } else {
            throw new io.karatelabs.ext.rules.analysis.RuleAnalysisException("LOOKUP_INDEX_TOP", n(var3), "an array indexed by a range");
         }
      } else {
         throw new io.karatelabs.ext.rules.analysis.RuleAnalysisException("COERCION", n(var3), "indexing a " + g(var1));
      }
   }

   private static String a(double var0) {
      return Terms.numberToString(var0);
   }

   private static io.karatelabs.ext.rules.analysis.AbstractValue a(io.karatelabs.ext.rules.analysis.AbstractValue var0, long var1) {
      if (var1 == 0L) {
         return var0;
      } else if (var0 instanceof io.karatelabs.ext.rules.analysis.AbstractValue.NumericIntervalAbstractValue var6) {
         return new io.karatelabs.ext.rules.analysis.AbstractValue.NumericIntervalAbstractValue(var6.b(), var6.a() | var1);
      } else if (var0 instanceof io.karatelabs.ext.rules.analysis.AbstractValue.ScalarSetAbstractValue var5) {
         return new io.karatelabs.ext.rules.analysis.AbstractValue.ScalarSetAbstractValue(var5.d(), var5.a() | var1);
      } else if (var0 instanceof io.karatelabs.ext.rules.analysis.AbstractValue.DerivedStringAbstractValue var4) {
         return new io.karatelabs.ext.rules.analysis.AbstractValue.DerivedStringAbstractValue(var4.a() | var1);
      } else {
         return (io.karatelabs.ext.rules.analysis.AbstractValue)(var0 instanceof io.karatelabs.ext.rules.analysis.AbstractValue.ObjectAbstractValue var3
            ? new io.karatelabs.ext.rules.analysis.AbstractValue.ObjectAbstractValue(var3.b(), var3.c() | var1, var3.d())
            : var0);
      }
   }

   private io.karatelabs.ext.rules.analysis.AbstractValue a(List<io.karatelabs.ext.rules.analysis.AbstractValue> var1, Node var2) {
      if (var1.size() == 1) {
         return (io.karatelabs.ext.rules.analysis.AbstractValue)var1.get(0);
      } else {
         io.karatelabs.ext.rules.analysis.AbstractValue var3 = (io.karatelabs.ext.rules.analysis.AbstractValue)var1.get(0);
         if (var3 instanceof io.karatelabs.ext.rules.analysis.AbstractValue.NumericIntervalAbstractValue) {
            NumericInterval var14 = null;
            long var17 = 0L;

            for (io.karatelabs.ext.rules.analysis.AbstractValue var26 : var1) {
               if (!(var26 instanceof io.karatelabs.ext.rules.analysis.AbstractValue.NumericIntervalAbstractValue var28)) {
                  throw new io.karatelabs.ext.rules.analysis.RuleAnalysisException("LOOKUP_MIXED", n(var2), "a lookup selects values of more than one type");
               }

               var14 = var14 == null ? var28.b() : var14.a(var28.b());
               var17 |= var28.a();
            }

            return new io.karatelabs.ext.rules.analysis.AbstractValue.NumericIntervalAbstractValue(var14, var17);
         } else if (var3 instanceof io.karatelabs.ext.rules.analysis.AbstractValue.ScalarSetAbstractValue) {
            LinkedHashSet var13 = new LinkedHashSet();
            long var16 = 0L;
            Class var22 = null;

            for (io.karatelabs.ext.rules.analysis.AbstractValue var27 : var1) {
               if (!(var27 instanceof io.karatelabs.ext.rules.analysis.AbstractValue.ScalarSetAbstractValue var29)) {
                  throw new io.karatelabs.ext.rules.analysis.RuleAnalysisException("LOOKUP_MIXED", n(var2), "a lookup selects values of more than one type");
               }

               for (Object var12 : var29.d()) {
                  if (var22 == null) {
                     var22 = var12.getClass();
                  } else if (var22 != var12.getClass()) {
                     throw new io.karatelabs.ext.rules.analysis.RuleAnalysisException("LOOKUP_MIXED", n(var2), "a lookup selects members of more than one type");
                  }
               }

               var13.addAll(var29.d());
               var16 |= var29.a();
            }

            return new io.karatelabs.ext.rules.analysis.AbstractValue.ScalarSetAbstractValue(new ArrayList<>(var13), var16);
         } else if (!(var3 instanceof io.karatelabs.ext.rules.analysis.AbstractValue.ObjectAbstractValue)) {
            throw new io.karatelabs.ext.rules.analysis.RuleAnalysisException("LOOKUP_MIXED", n(var2), "a lookup selects values the abstraction cannot join (" + g(var3) + ")");
         } else {
            LinkedHashSet var4 = new LinkedHashSet();

            for (io.karatelabs.ext.rules.analysis.AbstractValue var6 : var1) {
               if (!(var6 instanceof io.karatelabs.ext.rules.analysis.AbstractValue.ObjectAbstractValue var7)) {
                  throw new io.karatelabs.ext.rules.analysis.RuleAnalysisException("LOOKUP_MIXED", n(var2), "a lookup selects values of more than one type");
               }

               var4.addAll(var7.b().keySet());
               this.a(var7);
            }

            LinkedHashMap var15 = new LinkedHashMap();

            for (String var20 : (Iterable<String>) (Iterable<?>) (var4)) {
               ArrayList var8 = new ArrayList();

               for (io.karatelabs.ext.rules.analysis.AbstractValue var10 : var1) {
                  io.karatelabs.ext.rules.analysis.AbstractValue var11 = ((io.karatelabs.ext.rules.analysis.AbstractValue.ObjectAbstractValue)var10).b().get(var20);
                  if (var11 == null) {
                     var8 = null;
                     break;
                  }

                  var8.add(var11);
               }

               if (var8 != null) {
                  var15.put(var20, this.a(var8, var2));
               }
            }

            if (this.i != io.karatelabs.ext.rules.analysis.AbstractExpressionEvaluator.EvaluationListener.i) {
               ArrayList var19 = new ArrayList();

               for (io.karatelabs.ext.rules.analysis.AbstractValue var24 : var1) {
                  var19.addAll(this.a(((io.karatelabs.ext.rules.analysis.AbstractValue.ObjectAbstractValue)var24).b()));
               }

               this.j.put(var15, var19);
            }

            return new io.karatelabs.ext.rules.analysis.AbstractValue.ObjectAbstractValue(var15, 0L, true);
         }
      }
   }

   private void a(io.karatelabs.ext.rules.analysis.AbstractValue.ObjectAbstractValue var1, List<String> var2) {
      if (this.i != io.karatelabs.ext.rules.analysis.AbstractExpressionEvaluator.EvaluationListener.i) {
         for (LinkedHashMap var4 : this.a(var1.b())) {
            this.i.selected(var4, var2);
         }
      }
   }

   private List<LinkedHashMap<String, io.karatelabs.ext.rules.analysis.AbstractValue>> a(LinkedHashMap<String, io.karatelabs.ext.rules.analysis.AbstractValue> var1) {
      var var2 = this.j.get(var1);
      return var2 == null ? List.of(var1) : var2;
   }

   private void a(io.karatelabs.ext.rules.analysis.AbstractValue.ObjectAbstractValue var1) {
      if (this.h.add(var1.b())) {
         for (io.karatelabs.ext.rules.analysis.AbstractValue var3 : var1.b().values()) {
            if (var3 instanceof io.karatelabs.ext.rules.analysis.AbstractValue.ObjectAbstractValue var4) {
               this.a(var4);
            }
         }
      }
   }

   private boolean i(Node var1) {
      return this.l > 0 && !this.c.contains(var1.getFirstToken().pos);
   }

   private io.karatelabs.ext.rules.analysis.AbstractValue j(Node var1) {
      try {
         return this.f(var1);
      } catch (io.karatelabs.ext.rules.analysis.RuleAnalysisException var3) {
         return null;
      }
   }

   private static boolean k(Node var0) {
      if (var0.isToken()) {
         return false;
      } else {
         switch (var0.type) {
            case IF_STMT:
            case FN_CALL_EXPR:
            case LOGIC_AND_EXPR:
            case LOGIC_NULLISH_EXPR:
            case LOGIC_TERN_EXPR:
            case ASSIGN_EXPR:
               return true;
            default:
               for (int var1 = 0; var1 < var0.size(); var1++) {
                  if (k(var0.get(var1))) {
                     return true;
                  }
               }

               return false;
         }
      }
   }

   private io.karatelabs.ext.rules.analysis.AbstractValue l(Node var1) {
      Node var2 = var1.get(0);
      Node var3 = var1.get(2);
      ArrayList var4 = new ArrayList();

      for (int var5 = 0; var5 < var3.size(); var5++) {
         Node var6 = var3.get(var5);
         if (var6.type == NodeType.FN_CALL_ARG) {
            var4.add(var6.getFirst());
         }
      }

      if (var2.type == NodeType.REF_DOT_EXPR) {
         return this.a(this.f(var2.get(0)), var2.get(2).getText(), var4, var1);
      } else if (var2.type == NodeType.REF_EXPR && this.a(var2.getText(), var2) instanceof io.karatelabs.ext.rules.analysis.AbstractValue.FunctionAbstractValue var7) {
         return this.a(var7, this.a((List<Node>)var4));
      } else {
         throw o(var1);
      }
   }

   private io.karatelabs.ext.rules.analysis.AbstractValue a(io.karatelabs.ext.rules.analysis.AbstractValue var1, String var2, List<Node> var3, Node var4) {
      if (var2.equals("toFixed")) {
         return this.a(var1, var3, var4);
      } else if (var1 instanceof io.karatelabs.ext.rules.analysis.AbstractValue.SymbolicReferenceValue var5) {
         String var6 = var5.b();

         return switch (var6) {
            case "calc" -> this.a(var2, var3, var4);
            case "Math" -> this.b(var2, this.a(var3), var4);
            default -> throw o(var4);
         };
      } else {
         throw new io.karatelabs.ext.rules.analysis.RuleAnalysisException("COERCION", n(var4), "a method call on a " + g(var1));
      }
   }

   private io.karatelabs.ext.rules.analysis.AbstractValue a(String var1, List<Node> var2, Node var3) {
      switch (var1) {
         case "req":
         case "log":
         case "label":
         case "levels":
            this.a(var2);
            return new io.karatelabs.ext.rules.analysis.AbstractValue.NullAbstractValue();
         case "outcome":
            var var9 = this.a(var2);
            this.q = (io.karatelabs.ext.rules.analysis.AbstractValue)(var9.isEmpty() ? new io.karatelabs.ext.rules.analysis.AbstractValue.NullAbstractValue() : (io.karatelabs.ext.rules.analysis.AbstractValue)var9.get(0));
            this.r = true;
            return this.q;
         case "always":
         case "sometimes":
            return this.c(var1, var2, var3);
         case "round":
            var var6 = this.a(var2);
            io.karatelabs.ext.rules.analysis.AbstractValue var7 = (io.karatelabs.ext.rules.analysis.AbstractValue)var6.get(0);
            if (!(var7 instanceof io.karatelabs.ext.rules.analysis.AbstractValue.NullAbstractValue) && !(var7 instanceof io.karatelabs.ext.rules.analysis.AbstractValue.UndefinedAbstractValue)) {
               int var8 = var6.size() > 1 ? (int)this.a((io.karatelabs.ext.rules.analysis.AbstractValue)var6.get(1), var3).g() : 2;
               return new io.karatelabs.ext.rules.analysis.AbstractValue.NumericIntervalAbstractValue(this.a(var7, var3).a(var8), var7.a());
            }

            return var7;
         default:
            throw o(var3);
      }
   }

   private io.karatelabs.ext.rules.analysis.AbstractValue b(String var1, List<io.karatelabs.ext.rules.analysis.AbstractValue> var2, Node var3) {
      long var4 = 0L;

      for (io.karatelabs.ext.rules.analysis.AbstractValue var7 : var2) {
         var4 |= var7.a();
      }
      return switch (var1) {
         case "min" -> new io.karatelabs.ext.rules.analysis.AbstractValue.NumericIntervalAbstractValue(
            this.a((io.karatelabs.ext.rules.analysis.AbstractValue)var2.get(0), var3).e(this.a((io.karatelabs.ext.rules.analysis.AbstractValue)var2.get(1), var3)), var4
         );
         case "max" -> new io.karatelabs.ext.rules.analysis.AbstractValue.NumericIntervalAbstractValue(
            this.a((io.karatelabs.ext.rules.analysis.AbstractValue)var2.get(0), var3).f(this.a((io.karatelabs.ext.rules.analysis.AbstractValue)var2.get(1), var3)), var4
         );
         case "abs" -> new io.karatelabs.ext.rules.analysis.AbstractValue.NumericIntervalAbstractValue(this.a((io.karatelabs.ext.rules.analysis.AbstractValue)var2.get(0), var3).c(), var4);
         case "floor" -> new io.karatelabs.ext.rules.analysis.AbstractValue.NumericIntervalAbstractValue(this.a((io.karatelabs.ext.rules.analysis.AbstractValue)var2.get(0), var3).d(), var4);
         case "round" -> new io.karatelabs.ext.rules.analysis.AbstractValue.NumericIntervalAbstractValue(this.a((io.karatelabs.ext.rules.analysis.AbstractValue)var2.get(0), var3).e(), var4);
         default -> throw o(var3);
      };
   }

   private io.karatelabs.ext.rules.analysis.AbstractValue a(io.karatelabs.ext.rules.analysis.AbstractValue var1, List<Node> var2, Node var3) {
      var var4 = this.a(var2);
      if (!(var1 instanceof io.karatelabs.ext.rules.analysis.AbstractValue.NumericIntervalAbstractValue var5)) {
         throw new io.karatelabs.ext.rules.analysis.RuleAnalysisException("COERCION", n(var3), "toFixed on a " + g(var1));
      } else {
         int var6 = var4.isEmpty() ? 0 : (int)this.a((io.karatelabs.ext.rules.analysis.AbstractValue)var4.get(0), var3).g();

         try {
            JsScalars.toFixed(var5.b().g(), var6);
            JsScalars.toFixed(var5.b().h(), var6);
         } catch (RuntimeException var11) {
            throw new IllegalStateException("admission let an out-of-range toFixed precision through", var11);
         }

         long var7 = var5.a();

         for (io.karatelabs.ext.rules.analysis.AbstractValue var10 : (Iterable<io.karatelabs.ext.rules.analysis.AbstractValue>) (Iterable<?>) (var4)) {
            var7 |= var10.a();
         }

         return new io.karatelabs.ext.rules.analysis.AbstractValue.DerivedStringAbstractValue(var7);
      }
   }

   private List<io.karatelabs.ext.rules.analysis.AbstractValue> a(List<Node> var1) {
      ArrayList var2 = new ArrayList();

      for (Node var4 : var1) {
         var2.add(this.f(var4));
      }

      return var2;
   }

   private io.karatelabs.ext.rules.analysis.AbstractValue c(String var1, List<Node> var2, Node var3) {
      io.karatelabs.ext.rules.analysis.AbstractValue var4 = this.f((Node)var2.get(0));
      String var5 = String.valueOf(((io.karatelabs.ext.rules.analysis.AbstractValue.ScalarSetAbstractValue)var4).d().get(0));
      this.l++;

      io.karatelabs.ext.rules.analysis.AbstractValue var6;
      try {
         var6 = this.f((Node)var2.get(1));
      } finally {
         this.l--;
      }

      TernaryBoolean var7 = this.a(var6);
      String var8 = null;
      var var9 = this.a(var2.subList(Math.min(2, var2.size()), var2.size()));
      if (!var9.isEmpty()
         && var9.get(0) instanceof io.karatelabs.ext.rules.analysis.AbstractValue.ObjectAbstractValue var10
         && var10.b().get("req") instanceof io.karatelabs.ext.rules.analysis.AbstractValue.ScalarSetAbstractValue var11
         && var11.b()) {
         var8 = String.valueOf(var11.d().get(0));
      }

      long var15 = var6.a();
      if (var7 == io.karatelabs.ext.rules.analysis.TernaryBoolean.MAYBE && var5.equals(this.e)) {
         throw this.a(var15, this.g.get(var6));
      } else {
         this.o = this.o | (var7 == io.karatelabs.ext.rules.analysis.TernaryBoolean.MAYBE ? 0L : var15);
         this.n.add(new io.karatelabs.ext.rules.analysis.AbstractExpressionEvaluator.PropertyObservation(var5, n(var3), var7, var1, var8, var15));
         return a(var7, var15);
      }
   }

   private io.karatelabs.ext.rules.analysis.AbstractValue a(io.karatelabs.ext.rules.analysis.AbstractValue.FunctionAbstractValue var1, List<io.karatelabs.ext.rules.analysis.AbstractValue> var2) {
      Node var3 = var1.b();
      HashMap var4 = new HashMap();
      int var5 = 0;

      for (Node var7 : var3.findImmediateChildren(NodeType.FN_DECL_ARGS)) {
         for (int var8 = 0; var8 < var7.size(); var8++) {
            Node var9 = var7.get(var8);
            if (var9.type == NodeType.FN_DECL_ARG) {
               var4.put(var9.getFirst().getText(), var5 < var2.size() ? (io.karatelabs.ext.rules.analysis.AbstractValue)var2.get(var5) : new io.karatelabs.ext.rules.analysis.AbstractValue.UndefinedAbstractValue());
               var5++;
            }
         }
      }

      Deque var15 = this.k;
      ArrayDeque var16 = new ArrayDeque();
      var16.push((Map)var15.peekLast());
      var16.push(var4);
      this.k = var16;

      io.karatelabs.ext.rules.analysis.AbstractValue var18;
      try {
         this.e(var3.getLast());
         return new io.karatelabs.ext.rules.analysis.AbstractValue.UndefinedAbstractValue();
      } catch (io.karatelabs.ext.rules.analysis.AbstractExpressionEvaluator.AbstractReturnException var13) {
         var18 = var13.a;
      } finally {
         this.k = var15;
      }

      return var18;
   }

   private io.karatelabs.ext.rules.analysis.AbstractValue m(Node var1) {
      Node var2 = var1.get(0);
      String var3 = var1.get(1).getText();
      boolean var4 = !var3.equals("=");
      if (var2.type == NodeType.REF_EXPR) {
         String var10 = var2.getText();
         var var11 = this.a(var10);
         if (var11 == null) {
            throw new IllegalStateException("admission left '" + var10 + "' unbound at line " + n(var1));
         } else {
            io.karatelabs.ext.rules.analysis.AbstractValue var13 = var4 ? (io.karatelabs.ext.rules.analysis.AbstractValue)var11.get(var10) : null;
            io.karatelabs.ext.rules.analysis.AbstractValue var15 = this.a(var13, this.f(var1.get(2)), var3, var1);
            var11.put(var10, var15);
            return var15;
         }
      } else if (var2.type != NodeType.REF_DOT_EXPR) {
         throw o(var1);
      } else {
         io.karatelabs.ext.rules.analysis.AbstractValue var5 = this.f(var2.get(0));
         String var6 = var2.get(2).getText();
         if (var5 instanceof io.karatelabs.ext.rules.analysis.AbstractValue.SymbolicReferenceValue var12) {
            if (var12.b().equals("calc") && var6.equals("output")) {
               io.karatelabs.ext.rules.analysis.AbstractValue var14 = var4 ? this.p : null;
               this.p = this.a(var14, this.f(var1.get(2)), var3, var1);
               return this.p;
            } else {
               throw o(var1);
            }
         } else if (var5 instanceof io.karatelabs.ext.rules.analysis.AbstractValue.NullAbstractValue || var5 instanceof io.karatelabs.ext.rules.analysis.AbstractValue.UndefinedAbstractValue) {
            throw new io.karatelabs.ext.rules.analysis.RuleAnalysisException("NULL_MEMBER", n(var1), "assigning '" + var6 + "' on null/undefined");
         } else if (var5 instanceof io.karatelabs.ext.rules.analysis.AbstractValue.ObjectAbstractValue var7) {
            io.karatelabs.ext.rules.analysis.AbstractValue var8 = null;
            if (var4) {
               var8 = var7.b().get(var6);
               if (var8 == null) {
                  throw new io.karatelabs.ext.rules.analysis.RuleAnalysisException("LOOKUP_MISS", n(var1), "no property '" + var6 + "'");
               }
            }

            io.karatelabs.ext.rules.analysis.AbstractValue var9 = this.a(var8, this.f(var1.get(2)), var3, var1);
            if (!var7.d() && !this.h.contains(var7.b())) {
               var7.b().put(var6, var9);
               return var9;
            } else {
               throw new io.karatelabs.ext.rules.analysis.RuleAnalysisException("LOOKUP_ALIAS", n(var1), "assigning '" + var6 + "' on an object a lookup or a ternary joined");
            }
         } else {
            throw new io.karatelabs.ext.rules.analysis.RuleAnalysisException("COERCION", n(var1), "assigning '" + var6 + "' on a " + g(var5));
         }
      }
   }

   private io.karatelabs.ext.rules.analysis.AbstractValue a(io.karatelabs.ext.rules.analysis.AbstractValue var1, io.karatelabs.ext.rules.analysis.AbstractValue var2, String var3, Node var4) {
      return var1 == null ? var2 : this.b(var1, var2, var3.substring(0, 1), var4);
   }

   private Map<String, io.karatelabs.ext.rules.analysis.AbstractValue> a(String var1) {
      for (Map var3 : this.k) {
         if (var3.containsKey(var1)) {
            return var3;
         }
      }

      return null;
   }

   private io.karatelabs.ext.rules.analysis.AbstractValue b(io.karatelabs.ext.rules.analysis.AbstractValue var1, io.karatelabs.ext.rules.analysis.AbstractValue var2, String var3, Node var4) {
      long var5 = var1.a() | var2.a();
      if (!var3.equals("+") || !c(var1) && !c(var2)) {
         NumericInterval var7 = this.a(var1, var4);
         NumericInterval var8 = this.a(var2, var4);

         NumericInterval var9 = switch (var3) {
            case "+" -> var7.b(var8);
            case "-" -> var7.c(var8);
            case "*" -> var7.d(var8);
            case "/" -> var7.a(var8, n(var4));
            case "%" -> var7.b(var8, n(var4));
            default -> throw o(var4);
         };
         return new io.karatelabs.ext.rules.analysis.AbstractValue.NumericIntervalAbstractValue(var9, var5);
      } else if (e(var1) && e(var2)) {
         return new io.karatelabs.ext.rules.analysis.AbstractValue.DerivedStringAbstractValue(var5);
      } else {
         throw new io.karatelabs.ext.rules.analysis.RuleAnalysisException("COERCION", n(var4), "a " + g(e(var1) ? var2 : var1) + " concatenated to a string");
      }
   }

   private TernaryBoolean c(io.karatelabs.ext.rules.analysis.AbstractValue var1, io.karatelabs.ext.rules.analysis.AbstractValue var2, String var3, Node var4) {
      if (var1 instanceof io.karatelabs.ext.rules.analysis.AbstractValue.NumericIntervalAbstractValue var5 && var2 instanceof io.karatelabs.ext.rules.analysis.AbstractValue.NumericIntervalAbstractValue var16) {
         return switch (var3) {
            case "<" -> var5.b().g(var16.b());
            case "<=" -> var5.b().h(var16.b());
            case ">" -> var16.b().g(var5.b());
            case ">=" -> var16.b().h(var5.b());
            case "===", "==" -> var5.b().i(var16.b());
            case "!==", "!=" -> var5.b().i(var16.b()).not();
            default -> throw o(var4);
         };
      } else {
         boolean var15 = var3.equals("===") || var3.equals("!==");
         boolean var6 = var3.equals("!==") || var3.equals("!=");
         if (!var15 && !var3.equals("==") && !var3.equals("!=")) {
            throw new io.karatelabs.ext.rules.analysis.RuleAnalysisException("COERCION", n(var4), "an ordering comparison over non-numbers");
         } else {
            TernaryBoolean var7;
            if (!(var1 instanceof io.karatelabs.ext.rules.analysis.AbstractValue.NullAbstractValue) && !(var1 instanceof io.karatelabs.ext.rules.analysis.AbstractValue.UndefinedAbstractValue)
               || !(var2 instanceof io.karatelabs.ext.rules.analysis.AbstractValue.NullAbstractValue) && !(var2 instanceof io.karatelabs.ext.rules.analysis.AbstractValue.UndefinedAbstractValue)) {
               if (!(var1 instanceof io.karatelabs.ext.rules.analysis.AbstractValue.NullAbstractValue)
                  && !(var1 instanceof io.karatelabs.ext.rules.analysis.AbstractValue.UndefinedAbstractValue)
                  && !(var2 instanceof io.karatelabs.ext.rules.analysis.AbstractValue.NullAbstractValue)
                  && !(var2 instanceof io.karatelabs.ext.rules.analysis.AbstractValue.UndefinedAbstractValue)) {
                  String var8 = f(var1);
                  String var9 = f(var2);
                  if (var8 == null || var9 == null) {
                     throw new io.karatelabs.ext.rules.analysis.RuleAnalysisException("COERCION", n(var4), "equality between a " + g(var1) + " and a " + g(var2));
                  }

                  if (!var8.equals(var9)) {
                     if (!var15) {
                        throw new io.karatelabs.ext.rules.analysis.RuleAnalysisException("COERCION", n(var4), "a coercing equality across types");
                     }

                     var7 = io.karatelabs.ext.rules.analysis.TernaryBoolean.FALSE;
                  } else if (!(var1 instanceof io.karatelabs.ext.rules.analysis.AbstractValue.ScalarSetAbstractValue var10 && var2 instanceof io.karatelabs.ext.rules.analysis.AbstractValue.ScalarSetAbstractValue var11)) {
                     var7 = io.karatelabs.ext.rules.analysis.TernaryBoolean.MAYBE;
                  } else if (var10.b() && var11.b()) {
                     var7 = io.karatelabs.ext.rules.analysis.TernaryBoolean.of(var10.d().get(0).equals(var11.d().get(0)));
                  } else {
                     boolean var12 = false;

                     for (Object var14 : var10.d()) {
                        if (var11.d().contains(var14)) {
                           var12 = true;
                           break;
                        }
                     }

                     var7 = var12 ? io.karatelabs.ext.rules.analysis.TernaryBoolean.MAYBE : io.karatelabs.ext.rules.analysis.TernaryBoolean.FALSE;
                  }
               } else {
                  var7 = io.karatelabs.ext.rules.analysis.TernaryBoolean.FALSE;
               }
            } else {
               var7 = var15 ? io.karatelabs.ext.rules.analysis.TernaryBoolean.of(var1.getClass() == var2.getClass()) : io.karatelabs.ext.rules.analysis.TernaryBoolean.TRUE;
            }

            return var6 ? var7.not() : var7;
         }
      }
   }

   private TernaryBoolean a(io.karatelabs.ext.rules.analysis.AbstractValue var1) {
      if (var1 instanceof io.karatelabs.ext.rules.analysis.AbstractValue.NumericIntervalAbstractValue var10) {
         return var10.b().f();
      } else if (!(var1 instanceof io.karatelabs.ext.rules.analysis.AbstractValue.ScalarSetAbstractValue var2)) {
         if (var1 instanceof io.karatelabs.ext.rules.analysis.AbstractValue.NullAbstractValue || var1 instanceof io.karatelabs.ext.rules.analysis.AbstractValue.UndefinedAbstractValue) {
            return io.karatelabs.ext.rules.analysis.TernaryBoolean.FALSE;
         } else {
            return var1 instanceof io.karatelabs.ext.rules.analysis.AbstractValue.DerivedStringAbstractValue ? io.karatelabs.ext.rules.analysis.TernaryBoolean.MAYBE : io.karatelabs.ext.rules.analysis.TernaryBoolean.TRUE;
         }
      } else {
         boolean var3 = false;
         boolean var4 = false;

         for (Object var6 : var2.d()) {
            boolean var7 = var6 instanceof Boolean var9 ? var9 : !(var6 instanceof String var8 && var8.isEmpty());
            var3 |= var7;
            var4 |= !var7;
         }

         return var3 && var4 ? io.karatelabs.ext.rules.analysis.TernaryBoolean.MAYBE : io.karatelabs.ext.rules.analysis.TernaryBoolean.of(var3);
      }
   }

   private TernaryBoolean a(Node var1, io.karatelabs.ext.rules.analysis.AbstractValue var2) {
      TernaryBoolean var3 = this.a(var2);
      if (var3 == io.karatelabs.ext.rules.analysis.TernaryBoolean.MAYBE) {
         throw this.a(var2.a(), this.g.get(var2));
      } else {
         this.a(var1, var3, var2.a());
         return var3;
      }
   }

   private io.karatelabs.ext.rules.analysis.AbstractExpressionEvaluator.BranchSplitException a(long var1, io.karatelabs.ext.rules.analysis.AbstractExpressionEvaluator.ExpressionProvenance var3) {
      ArrayList var4 = new ArrayList(this.d.a());

      for (int var5 = 0; var5 < this.d.a(); var5++) {
         io.karatelabs.ext.rules.analysis.AbstractBox.BoxDimension var6 = this.d.a.get(var5);
         var4.add(new io.karatelabs.ext.rules.analysis.AbstractExecutionState.ExecutionDimension(var6.c(), var6.d(), this.d.b[var5], this.d.c[var5], var6.g()));
      }

      return new io.karatelabs.ext.rules.analysis.AbstractExpressionEvaluator.BranchSplitException(
         new io.karatelabs.ext.rules.analysis.AbstractExecutionState(
            var4,
            var3 == null ? -1 : var3.d(),
            var3 == null ? null : var3.a(),
            var3 == null ? null : b(var3.b()),
            var3 == null ? null : b(var3.c()),
            var1,
            this.l > 0
         )
      );
   }

   private static io.karatelabs.ext.rules.analysis.AbstractValue b(io.karatelabs.ext.rules.analysis.AbstractValue var0) {
      if (var0 instanceof io.karatelabs.ext.rules.analysis.AbstractValue.ScalarSetAbstractValue var1) {
         return new io.karatelabs.ext.rules.analysis.AbstractValue.ScalarSetAbstractValue(var1.d(), var1.a());
      } else {
         return !(var0 instanceof io.karatelabs.ext.rules.analysis.AbstractValue.NumericIntervalAbstractValue)
               && !(var0 instanceof io.karatelabs.ext.rules.analysis.AbstractValue.DerivedStringAbstractValue)
               && !(var0 instanceof io.karatelabs.ext.rules.analysis.AbstractValue.NullAbstractValue)
               && !(var0 instanceof io.karatelabs.ext.rules.analysis.AbstractValue.UndefinedAbstractValue)
            ? null
            : var0;
      }
   }

   private void a(Node var1, TernaryBoolean var2, long var3) {
      this.o |= var3;
      int var5 = var1.getFirstToken().pos;
      if (this.c.contains(var5)) {
         this.m.computeIfAbsent(var5, var0 -> new TreeSet<>()).add(var2 == io.karatelabs.ext.rules.analysis.TernaryBoolean.TRUE);
      }
   }

   private static io.karatelabs.ext.rules.analysis.AbstractValue a(TernaryBoolean var0, long var1) {
      return switch (var0) {
         case TRUE -> new io.karatelabs.ext.rules.analysis.AbstractValue.ScalarSetAbstractValue(List.of(Boolean.TRUE), var1);
         case FALSE -> new io.karatelabs.ext.rules.analysis.AbstractValue.ScalarSetAbstractValue(List.of(Boolean.FALSE), var1);
         case MAYBE -> new io.karatelabs.ext.rules.analysis.AbstractValue.ScalarSetAbstractValue(List.of(Boolean.FALSE, Boolean.TRUE), var1);
      };
   }

   private NumericInterval a(io.karatelabs.ext.rules.analysis.AbstractValue var1, Node var2) {
      if (var1 instanceof io.karatelabs.ext.rules.analysis.AbstractValue.NumericIntervalAbstractValue var3) {
         return var3.b();
      } else {
         throw new io.karatelabs.ext.rules.analysis.RuleAnalysisException("COERCION", n(var2), "a " + g(var1) + " used as a number");
      }
   }

   private static boolean c(io.karatelabs.ext.rules.analysis.AbstractValue var0) {
      return var0 instanceof io.karatelabs.ext.rules.analysis.AbstractValue.DerivedStringAbstractValue
         || var0 instanceof io.karatelabs.ext.rules.analysis.AbstractValue.ScalarSetAbstractValue var1 && !var1.d().isEmpty() && var1.d().get(0) instanceof String;
   }

   private static boolean d(io.karatelabs.ext.rules.analysis.AbstractValue var0) {
      return var0 instanceof io.karatelabs.ext.rules.analysis.AbstractValue.ScalarSetAbstractValue var1 && var1.c();
   }

   private static boolean e(io.karatelabs.ext.rules.analysis.AbstractValue var0) {
      return var0 instanceof io.karatelabs.ext.rules.analysis.AbstractValue.NumericIntervalAbstractValue
         || var0 instanceof io.karatelabs.ext.rules.analysis.AbstractValue.ScalarSetAbstractValue
         || var0 instanceof io.karatelabs.ext.rules.analysis.AbstractValue.DerivedStringAbstractValue
         || var0 instanceof io.karatelabs.ext.rules.analysis.AbstractValue.NullAbstractValue
         || var0 instanceof io.karatelabs.ext.rules.analysis.AbstractValue.UndefinedAbstractValue;
   }

   private static String f(io.karatelabs.ext.rules.analysis.AbstractValue var0) {
      if (var0 instanceof io.karatelabs.ext.rules.analysis.AbstractValue.NumericIntervalAbstractValue) {
         return "number";
      } else if (var0 instanceof io.karatelabs.ext.rules.analysis.AbstractValue.DerivedStringAbstractValue) {
         return "string";
      } else if (var0 instanceof io.karatelabs.ext.rules.analysis.AbstractValue.ScalarSetAbstractValue var1) {
         return var1.c() ? "boolean" : "string";
      } else {
         return null;
      }
   }

   private static String g(io.karatelabs.ext.rules.analysis.AbstractValue var0) {
      return var0 == null ? "null" : var0.getClass().getSimpleName();
   }

   private static int n(Node var0) {
      return var0.getFirstToken().line + 1;
   }

   private static IllegalStateException o(Node var0) {
      return new IllegalStateException("admission let " + var0.type + " through at line " + n(var0));
   }

   public record EvaluationResult(Map<Integer, Set<Boolean>> a, List<io.karatelabs.ext.rules.analysis.AbstractExpressionEvaluator.PropertyObservation> b, boolean c, int d, long e, io.karatelabs.ext.rules.analysis.AbstractValue f, boolean g) {

      public EvaluationResult(Map<Integer, Set<Boolean>> a, List<io.karatelabs.ext.rules.analysis.AbstractExpressionEvaluator.PropertyObservation> b, boolean c, int d, long e, io.karatelabs.ext.rules.analysis.AbstractValue f, boolean g) {
         this.a = a;
         this.b = b;
         this.c = c;
         this.d = d;
         this.e = e;
         this.f = f;
         this.g = g;
      }
   }

   public interface EvaluationListener {
      io.karatelabs.ext.rules.analysis.AbstractExpressionEvaluator.EvaluationListener i = (var0, var1) -> {};

      void selected(LinkedHashMap<String, io.karatelabs.ext.rules.analysis.AbstractValue> var1, List<String> var2);
   }

   public static final class BranchSplitException extends RuntimeException {
      public final io.karatelabs.ext.rules.analysis.AbstractExecutionState a;

      BranchSplitException(io.karatelabs.ext.rules.analysis.AbstractExecutionState var1) {
         super(null, null, false, false);
         this.a = var1;
      }
   }

   private record ExpressionProvenance(String a, io.karatelabs.ext.rules.analysis.AbstractValue b, io.karatelabs.ext.rules.analysis.AbstractValue c, int d) {

      private ExpressionProvenance(String a, io.karatelabs.ext.rules.analysis.AbstractValue b, io.karatelabs.ext.rules.analysis.AbstractValue c, int d) {
         this.a = a;
         this.b = b;
         this.c = c;
         this.d = d;
      }
   }

   public record PropertyObservation(String a, int b, TernaryBoolean c, String d, String e, long f) {

      public PropertyObservation(String a, int b, TernaryBoolean c, String d, String e, long f) {
         this.a = a;
         this.b = b;
         this.c = c;
         this.d = d;
         this.e = e;
         this.f = f;
      }
   }

   private static final class AbstractReturnException extends RuntimeException {
      final io.karatelabs.ext.rules.analysis.AbstractValue a;

      AbstractReturnException(io.karatelabs.ext.rules.analysis.AbstractValue var1) {
         super(null, null, false, false);
         this.a = var1;
      }
   }

   private static final class AbstractThrowException extends RuntimeException {
      final int a;

      AbstractThrowException(int var1) {
         super(null, null, false, false);
         this.a = var1;
      }
   }
}
