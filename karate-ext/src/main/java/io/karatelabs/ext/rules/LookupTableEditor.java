package io.karatelabs.ext.rules;

import io.karatelabs.js.Engine;
import io.karatelabs.js.JavaCallable;
import io.karatelabs.js.JsFunction;
import io.karatelabs.parser.Node;
import io.karatelabs.parser.NodeType;
import io.karatelabs.parser.Token;
import io.karatelabs.parser.TokenType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.Map.Entry;

public final class LookupTableEditor {
   private final String b;
   private final Node c;
   private final List<io.karatelabs.ext.rules.LookupTableEditor.LookupEntry> d;
   static final String a = "KARATE_RULES_LOOKUPS_TIMEOUT_MS";
   private static final io.karatelabs.ext.common.SandboxExecutor.SandboxFailureMapper e = new io.karatelabs.ext.common.SandboxExecutor.SandboxFailureMapper() {
      @Override
      public RuntimeException a(long var1) {
         return new RuleApiException(
            408, "TIMEOUT", "calc.js exceeded " + var1 + "ms while reading its `lookup` table; check for a loop that never ends at its top level"
         );
      }

      @Override
      public RuntimeException a(Throwable var1) {
         for (Throwable var2 = var1; var2 != null; var2 = var2.getCause()) {
            if (var2 instanceof RuleApiException var3) {
               return var3;
            }
         }

         return (RuntimeException)(var1 instanceof RuntimeException var4 ? var4 : new IllegalStateException(var1));
      }

      @Override
      public RuntimeException a() {
         return new RuleApiException(500, "INTERNAL", "calc.js eval interrupted");
      }
   };

   public LookupTableEditor(String var1) {
      this.b = var1;
      Node var2 = Engine.parse(var1);
      this.c = a(var2);
      this.d = this.c == null ? List.of() : b(this.c);
   }

   public boolean a() {
      return this.c != null;
   }

   public Map<String, Object> b() {
      return this.a(io.karatelabs.ext.common.SandboxExecutor.a("KARATE_RULES_LOOKUPS_TIMEOUT_MS", 5000L));
   }

   Map<String, Object> a(long var1) {
      if (this.c == null) {
         throw new RuleApiException(400, "LOOKUP_BLOCK_MISSING", "calc.js has no `const lookup = {...}` declaration");
      } else {
         Object var3;
         try (io.karatelabs.ext.common.SandboxExecutor.SandboxExecutionScope var4 = new io.karatelabs.ext.common.SandboxExecutor.SandboxExecutionScope("rule-lookups", var1)) {
            var3 = var4.a(this::d, e);
         }

         Object var9 = c(var3);
         return (Map<String, Object>)var9;
      }
   }

   private Object d() {
      Engine var1 = new Engine();

      try {
         var1.eval(this.b);
      } catch (RuntimeException var3) {
         throw new RuleApiException(400, "PARSE_FAILED", "calc.js eval failed: " + var3.getMessage(), var3);
      }

      Object var2 = var1.get("lookup");
      if (!(var2 instanceof Map)) {
         throw new RuleApiException(400, "LOOKUP_BLOCK_MISSING", "calc.js `lookup` is not an object");
      } else {
         return var2;
      }
   }

   public String a(String[] var1, String var2) {
      this.e();
      Node var3 = this.b(var1);
      return this.a(var3.getFirstToken(), var3.getLastToken(), var2);
   }

   public String a(String var1, Object var2) {
      this.e();
      io.karatelabs.ext.rules.LookupTableEditor.LookupEntry var3 = this.a(var1);
      if (var3 == null) {
         throw new RuleApiException(400, "LOOKUP_PATH_NOT_FOUND", var1);
      } else {
         int var4 = var3.b.getFirstToken().col - 1;
         String var5 = io.karatelabs.ext.rules.JsLiteralWriter.a(var2, var4);
         return this.a(var3.c.getFirstToken(), var3.c.getLastToken(), var5);
      }
   }

   public String a(Map<String, Object> var1) {
      this.e();
      var var2 = this.b();
      ArrayList<io.karatelabs.ext.rules.LookupTableEditor.TextEdit> var3 = new ArrayList();

      for (Entry var5 : var1.entrySet()) {
         String var6 = (String)var5.getKey();
         Object var7 = var5.getValue();
         io.karatelabs.ext.rules.LookupTableEditor.LookupEntry var8 = this.a(var6);
         if (var8 == null) {
            throw new RuleApiException(400, "LOOKUP_PATH_NOT_FOUND", "top-level lookup key not found: " + var6);
         }

         if (var8.d) {
            throw new RuleApiException(400, "LOOKUP_PATH_NOT_FOUND", "cannot modify function-valued lookup helper: " + var6);
         }

         Object var9 = var2.get(var6);
         if (a(var9, var7)) {
            this.a(var8.c, var9, var7, var3);
         } else {
            int var10 = var8.b.getFirstToken().col - 1;
            String var11 = io.karatelabs.ext.rules.JsLiteralWriter.a(var7, var10);
            var3.add(new io.karatelabs.ext.rules.LookupTableEditor.TextEdit(var8.c.getFirstToken().pos, a(var8.c.getLastToken()), var11));
         }
      }

      var3.sort(Comparator.<LookupTableEditor.TextEdit>comparingInt(var0 -> var0.a).reversed());
      StringBuilder var12 = new StringBuilder(this.b);

      for (io.karatelabs.ext.rules.LookupTableEditor.TextEdit var14 : (Iterable<io.karatelabs.ext.rules.LookupTableEditor.TextEdit>) (Iterable<?>) (var3)) {
         var12.replace(var14.a, var14.b, var14.c);
      }

      return var12.toString();
   }

   private static Node a(Node var0) {
      for (Node var2 : var0.findAll(NodeType.VAR_STMT)) {
         for (Node var4 : var2.findAll(NodeType.VAR_DECL)) {
            if (!var4.isEmpty()) {
               Node var5 = var4.getFirst();
               if (var5.isToken() && "lookup".equals(var5.token.getText())) {
                  return var4.findFirstChild(NodeType.LIT_OBJECT);
               }
            }
         }
      }

      return null;
   }

   private static List<io.karatelabs.ext.rules.LookupTableEditor.LookupEntry> b(Node var0) {
      ArrayList<io.karatelabs.ext.rules.LookupTableEditor.LookupEntry> var1 = new ArrayList();

      for (Node var3 : var0.findImmediateChildren(NodeType.OBJECT_ELEM)) {
         String var4 = c(var3);
         if (var4 != null) {
            Node var5 = d(var3);
            boolean var6 = var5 != null && (var5.type == NodeType.FN_EXPR || var5.type == NodeType.FN_ARROW_EXPR);
            var1.add(new io.karatelabs.ext.rules.LookupTableEditor.LookupEntry(var4, var3, var5, var6));
         }
      }

      return var1;
   }

   private static String c(Node var0) {
      if (var0.isEmpty()) {
         return null;
      } else {
         Node var1 = var0.getFirst();
         if (!var1.isToken()) {
            return null;
         } else {
            TokenType var2 = var1.token.type;
            if (var2 != TokenType.S_STRING && var2 != TokenType.D_STRING) {
               return var1.token.getText();
            } else {
               String var3 = var1.token.getText();
               return a(var3.substring(1, var3.length() - 1), var3.charAt(0));
            }
         }
      }
   }

   private static String a(String var0, char var1) {
      StringBuilder var2 = new StringBuilder(var0.length());

      for (int var3 = 0; var3 < var0.length(); var3++) {
         char var4 = var0.charAt(var3);
         if (var4 == '\\' && var3 + 1 < var0.length()) {
            char var5 = var0.charAt(++var3);
            switch (var5) {
               case '\\':
                  var2.append('\\');
                  break;
               case 'n':
                  var2.append('\n');
                  break;
               case 'r':
                  var2.append('\r');
                  break;
               case 't':
                  var2.append('\t');
                  break;
               default:
                  var2.append(var5);
            }
         } else {
            var2.append(var4);
         }
      }

      return var2.toString();
   }

   private static Node d(Node var0) {
      if (var0.size() < 2) {
         return null;
      } else {
         Node var1 = var0.get(1);
         if (var1.type == NodeType.FN_EXPR) {
            return var1;
         } else {
            return var0.size() < 3 ? null : e(var0.get(2));
         }
      }
   }

   private static Node e(Node var0) {
      while (var0 != null && var0.size() == 1 && (var0.type == NodeType.EXPR || var0.type == NodeType.LIT_EXPR)) {
         var0 = var0.getFirst();
      }

      return var0;
   }

   private io.karatelabs.ext.rules.LookupTableEditor.LookupEntry a(String var1) {
      for (io.karatelabs.ext.rules.LookupTableEditor.LookupEntry var3 : this.d) {
         if (var3.a.equals(var1)) {
            return var3;
         }
      }

      return null;
   }

   public Set<String> c() {
      LinkedHashSet<java.lang.String> var1 = new LinkedHashSet();

      for (io.karatelabs.ext.rules.LookupTableEditor.LookupEntry var3 : this.d) {
         var1.add(var3.a);
      }

      return var1;
   }

   Node a(String... var1) {
      if (this.c == null) {
         return null;
      } else {
         for (int var2 = var1.length; var2 > 0; var2--) {
            try {
               return this.b(Arrays.copyOf(var1, var2));
            } catch (RuleApiException var4) {
            }
         }

         return null;
      }
   }

   private Node b(String[] var1) {
      if (var1 != null && var1.length != 0) {
         Node var2 = this.c;

         for (int var3 = 0; var3 < var1.length; var3++) {
            String var4 = var1[var3];
            if (var2.type == NodeType.LIT_OBJECT) {
               Node var5 = null;

               for (Node var7 : var2.findImmediateChildren(NodeType.OBJECT_ELEM)) {
                  if (var4.equals(c(var7))) {
                     var5 = d(var7);
                     break;
                  }
               }

               if (var5 == null) {
                  throw new RuleApiException(400, "LOOKUP_PATH_NOT_FOUND", "no property '" + var4 + "' at depth " + var3);
               }

               var2 = var5;
            } else {
               if (var2.type != NodeType.LIT_ARRAY) {
                  throw new RuleApiException(400, "LOOKUP_PATH_NOT_FOUND", "path enters non-object/array at depth " + var3 + " (" + var2.type + ")");
               }

               int var9;
               try {
                  var9 = Integer.parseInt(var4);
               } catch (NumberFormatException var8) {
                  throw new RuleApiException(400, "LOOKUP_PATH_NOT_FOUND", "non-integer index '" + var4 + "' at depth " + var3);
               }

               var var10 = var2.findImmediateChildren(NodeType.ARRAY_ELEM);
               if (var9 < 0 || var9 >= var10.size()) {
                  throw new RuleApiException(400, "LOOKUP_PATH_NOT_FOUND", "array index " + var9 + " out of range at depth " + var3);
               }

               Node var11 = (Node)var10.get(var9);
               if (var11.isEmpty()) {
                  throw new RuleApiException(400, "LOOKUP_PATH_NOT_FOUND", "empty array element at index " + var9);
               }

               var2 = e(var11.getFirst());
            }
         }

         return var2;
      } else {
         throw new RuleApiException(400, "LOOKUP_PATH_NOT_FOUND", "empty path");
      }
   }

   private static boolean a(Object var0, Object var1) {
      if (var0 != null && var1 != null) {
         if (var0 instanceof Map var2 && var1 instanceof Map var7) {
            if (!var2.keySet().equals(var7.keySet())) {
               return false;
            } else {
               for (Object var5 : var2.keySet()) {
                  if (!a(var2.get(var5), var7.get(var5))) {
                     return false;
                  }
               }

               return true;
            }
         } else if (!(var0 instanceof List var6 && var1 instanceof List var3)) {
            return a(var0).equals(a(var1));
         } else if (var6.size() != var3.size()) {
            return false;
         } else {
            for (int var4 = 0; var4 < var6.size(); var4++) {
               if (!a(var6.get(var4), var3.get(var4))) {
                  return false;
               }
            }

            return true;
         }
      } else {
         return var0 == null && var1 == null;
      }
   }

   private static String a(Object var0) {
      if (var0 instanceof Number) {
         return "number";
      } else if (var0 instanceof CharSequence) {
         return "string";
      } else if (var0 instanceof Boolean) {
         return "boolean";
      } else {
         return var0 == null ? "null" : var0.getClass().getName();
      }
   }

   private void a(Node var1, Object var2, Object var3, List<io.karatelabs.ext.rules.LookupTableEditor.TextEdit> var4) {
      if (var1.type == NodeType.LIT_OBJECT) {
         Map<java.lang.String, Object> var5 = (Map)var2;
         Map<java.lang.String, Object> var6 = (Map)var3;

         for (Node var8 : var1.findImmediateChildren(NodeType.OBJECT_ELEM)) {
            String var9 = c(var8);
            if (var9 != null) {
               Node var10 = d(var8);
               if (var10 != null && var10.type != NodeType.FN_EXPR && var10.type != NodeType.FN_ARROW_EXPR) {
                  this.a(var10, var5.get(var9), var6.get(var9), var4);
               }
            }
         }
      } else if (var1.type == NodeType.LIT_ARRAY) {
         List var11 = (List)var2;
         List var12 = (List)var3;
         var var13 = var1.findImmediateChildren(NodeType.ARRAY_ELEM);

         for (int var14 = 0; var14 < var13.size(); var14++) {
            Node var15 = (Node)var13.get(var14);
            if (!var15.isEmpty()) {
               Node var16 = e(var15.getFirst());
               this.a(var16, var11.get(var14), var12.get(var14), var4);
            }
         }
      } else if (!Objects.equals(b(var2), b(var3))) {
         var4.add(new io.karatelabs.ext.rules.LookupTableEditor.TextEdit(var1.getFirstToken().pos, a(var1.getLastToken()), io.karatelabs.ext.rules.JsLiteralWriter.a(var3)));
      }
   }

   private static Object b(Object var0) {
      if (var0 instanceof Number var1) {
         double var2 = var1.doubleValue();
         return var2 == Math.floor(var2) && Math.abs(var2) < 1.0E16 ? (long)var2 : var2;
      } else {
         return var0;
      }
   }

   private static int a(Token var0) {
      return var0.pos + var0.length;
   }

   private String a(Token var1, Token var2, String var3) {
      int var4 = var1.pos;
      int var5 = var2.pos + var2.length;
      return this.b.substring(0, var4) + var3 + this.b.substring(var5);
   }

   private void e() {
      if (this.c == null) {
         throw new RuleApiException(400, "LOOKUP_BLOCK_MISSING", "calc.js has no `const lookup = {...}` declaration");
      }
   }

   private static Object c(Object var0) {
      if (!(var0 instanceof JsFunction) && !(var0 instanceof JavaCallable)) {
         if (var0 instanceof Map var6) {
            LinkedHashMap<java.lang.String, java.lang.Object> var7 = new LinkedHashMap();

            for (Entry var9 : (Iterable<Entry>) (Iterable<?>) (var6.entrySet())) {
               Object var5 = var9.getValue();
               if (!(var5 instanceof JsFunction) && !(var5 instanceof JavaCallable)) {
                  var7.put(var9.getKey().toString(), c(var5));
               }
            }

            return var7;
         } else if (!(var0 instanceof List var1)) {
            return var0;
         } else {
            ArrayList<java.lang.Object> var2 = new ArrayList(var1.size());

            for (Object var4 : var1) {
               var2.add(c(var4));
            }

            return var2;
         }
      } else {
         return null;
      }
   }

   private record TextEdit(int a, int b, String c) {

      private TextEdit(int a, int b, String c) {
         this.a = a;
         this.b = b;
         this.c = c;
      }
   }

   private record LookupEntry(String a, Node b, Node c, boolean d) {

      private LookupEntry(String a, Node b, Node c, boolean d) {
         this.a = a;
         this.b = b;
         this.c = c;
         this.d = d;
      }
   }
}
