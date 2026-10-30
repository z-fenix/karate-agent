package io.karatelabs.ext.lint;

import io.karatelabs.js.JavaCallable;
import io.karatelabs.js.Terms;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

public final class JsonPathPlusSelector {
   static final int MAX_DESCENDANT_NODES = 500000;
   private final String expression;
   private final List<io.karatelabs.ext.lint.JsonPathPlusSelector.SelectorStep> steps;
   static final String[][] FILTER_BINDINGS = new String[][]{
      {"@parentProperty", "_pprop"}, {"@parent", "_parent"}, {"@property", "_prop"}, {"@root", "_root"}, {"@path", "_path"}, {"@", "_v"}
   };

   private JsonPathPlusSelector(String var1, List<io.karatelabs.ext.lint.JsonPathPlusSelector.SelectorStep> var2) {
      this.expression = var1;
      this.steps = var2;
   }

   public static JsonPathPlusSelector parse(String var0) {
      if (var0 != null && !var0.isBlank()) {
         return new JsonPathPlusSelector(var0, parseSteps(var0));
      } else {
         throw new LintException("a jsonpath-plus selector cannot be empty");
      }
   }

   public String expression() {
      return this.expression;
   }

   public List<io.karatelabs.ext.lint.JsonPathPlusSelector.NodeSelection> select(Object var1, io.karatelabs.ext.lint.JsonPathPlusSelector.FilterCompiler var2) {
      ArrayList<io.karatelabs.ext.lint.JsonPathPlusSelector.NodeSelection> var3 = new ArrayList();
      var3.add(new io.karatelabs.ext.lint.JsonPathPlusSelector.NodeSelection(var1, List.of()));

      for (io.karatelabs.ext.lint.JsonPathPlusSelector.SelectorStep var5 : this.steps) {
         ArrayList<io.karatelabs.ext.lint.JsonPathPlusSelector.NodeSelection> var6 = new ArrayList();
         LinkedHashSet var7 = new LinkedHashSet();

         for (io.karatelabs.ext.lint.JsonPathPlusSelector.NodeSelection var9 : (Iterable<io.karatelabs.ext.lint.JsonPathPlusSelector.NodeSelection>) (Iterable<?>) (var3)) {
            if (var5.b) {
               for (io.karatelabs.ext.lint.JsonPathPlusSelector.NodeSelection var11 : descendants(var9)) {
                  this.traverseStep(var5, var11, var1, var2, var6, var7);
               }
            } else {
               this.traverseStep(var5, var9, var1, var2, var6, var7);
            }
         }

         var3 = var6;
         if (var6.isEmpty()) {
            return var6;
         }
      }

      return var3;
   }

   private static List<io.karatelabs.ext.lint.JsonPathPlusSelector.NodeSelection> descendants(io.karatelabs.ext.lint.JsonPathPlusSelector.NodeSelection var0) {
      ArrayList<io.karatelabs.ext.lint.JsonPathPlusSelector.NodeSelection> var1 = new ArrayList();
      collectDescendants(var0, var1);
      return var1;
   }

   private static void collectDescendants(io.karatelabs.ext.lint.JsonPathPlusSelector.NodeSelection var0, List<io.karatelabs.ext.lint.JsonPathPlusSelector.NodeSelection> var1) {
      if (var1.size() > 500000) {
         throw new LintException("a `..` selector walked more than 500000 nodes — the document is too deep or too large for a descendant scan");
      } else {
         var1.add(var0);
         if (var0.d instanceof Map var2) {
            for (Entry var5 : (Iterable<Entry>) (Iterable<?>) (var2.entrySet())) {
               collectDescendants(new io.karatelabs.ext.lint.JsonPathPlusSelector.NodeSelection(var5.getValue(), appendPathSegment(var0.e, String.valueOf(var5.getKey()))), var1);
            }
         } else if (var0.d instanceof List var3) {
            for (int var8 = 0; var8 < var3.size(); var8++) {
               collectDescendants(new io.karatelabs.ext.lint.JsonPathPlusSelector.NodeSelection(var3.get(var8), appendPathSegment(var0.e, Integer.valueOf(var8))), var1);
            }
         }
      }
   }

   private void traverseStep(io.karatelabs.ext.lint.JsonPathPlusSelector.SelectorStep var1, io.karatelabs.ext.lint.JsonPathPlusSelector.NodeSelection var2, Object var3, io.karatelabs.ext.lint.JsonPathPlusSelector.FilterCompiler var4, List<io.karatelabs.ext.lint.JsonPathPlusSelector.NodeSelection> var5, Set<String> var6) {
      switch (var1.a) {
         case CHILD:
            followObjectKey(var2, var1.c, var5, var6);
            break;
         case WILDCARD:
            if (var2.d instanceof Map var15) {
               for (Entry var24 : (Iterable<Entry>) (Iterable<?>) (var15.entrySet())) {
                  addUniqueMatch(var5, var6, new io.karatelabs.ext.lint.JsonPathPlusSelector.NodeSelection(var24.getValue(), appendPathSegment(var2.e, String.valueOf(var24.getKey()))));
               }
            } else if (var2.d instanceof List var18) {
               for (int var23 = 0; var23 < var18.size(); var23++) {
                  addUniqueMatch(var5, var6, new io.karatelabs.ext.lint.JsonPathPlusSelector.NodeSelection(var18.get(var23), appendPathSegment(var2.e, Integer.valueOf(var23))));
               }
            }
            break;
         case INDEX:
            followArrayIndex(var2, var1.d, var5, var6);
            break;
         case SLICE:
            if (var2.d instanceof List var14) {
               int var17 = var14.size();
               int var19 = var1.e[2] == 0 ? 1 : var1.e[2];
               int var10 = clampSliceBound(var1.e[0], var17, var19, true);
               int var11 = clampSliceBound(var1.e[1], var17, var19, false);
               if (var19 > 0) {
                  for (int var12 = var10; var12 < var11; var12 += var19) {
                     addUniqueMatch(var5, var6, new io.karatelabs.ext.lint.JsonPathPlusSelector.NodeSelection(var14.get(var12), appendPathSegment(var2.e, Integer.valueOf(var12))));
                  }
               } else {
                  for (int var25 = var10; var25 > var11; var25 += var19) {
                     addUniqueMatch(var5, var6, new io.karatelabs.ext.lint.JsonPathPlusSelector.NodeSelection(var14.get(var25), appendPathSegment(var2.e, Integer.valueOf(var25))));
                  }
               }
            }
            break;
         case UNION:
            for (Object var8 : var1.f) {
               if (var8 instanceof Integer var9) {
                  followArrayIndex(var2, var9, var5, var6);
               } else {
                  followObjectKey(var2, String.valueOf(var8), var5, var6);
               }
            }
            break;
         case FILTER:
            this.traverseFilterStep(var1, var2, var3, var4, var5, var6);
            break;
         case KEYS:
            if (var2.lastPathSegment() != null) {
               addUniqueMatch(var5, var6, new io.karatelabs.ext.lint.JsonPathPlusSelector.NodeSelection(var2.lastPathSegment(), var2.e));
            }
            break;
         case PARENT:
            if (!var2.e.isEmpty()) {
               var var7 = var2.e.subList(0, var2.e.size() - 1);
               addUniqueMatch(var5, var6, new io.karatelabs.ext.lint.JsonPathPlusSelector.NodeSelection(resolvePath(var3, var7), List.copyOf(var7)));
            }
      }
   }

   private void traverseFilterStep(io.karatelabs.ext.lint.JsonPathPlusSelector.SelectorStep var1, io.karatelabs.ext.lint.JsonPathPlusSelector.NodeSelection var2, Object var3, io.karatelabs.ext.lint.JsonPathPlusSelector.FilterCompiler var4, List<io.karatelabs.ext.lint.JsonPathPlusSelector.NodeSelection> var5, Set<String> var6) {
      if (var4 == null) {
         throw new LintException(
            "selector '" + this.expression + "' has a filter, but no filter evaluator was supplied — a filter body is JavaScript and only the lint sandbox may run it"
         );
      } else {
         JavaCallable var7 = var4.compile(var1.g);
         ArrayList<io.karatelabs.ext.lint.JsonPathPlusSelector.NodeSelection> var8 = new ArrayList();
         if (var2.d instanceof Map var9) {
            for (Entry var12 : (Iterable<Entry>) (Iterable<?>) (var9.entrySet())) {
               var8.add(new io.karatelabs.ext.lint.JsonPathPlusSelector.NodeSelection(var12.getValue(), appendPathSegment(var2.e, String.valueOf(var12.getKey()))));
            }
         } else if (var2.d instanceof List var10) {
            for (int var19 = 0; var19 < var10.size(); var19++) {
               var8.add(new io.karatelabs.ext.lint.JsonPathPlusSelector.NodeSelection(var10.get(var19), appendPathSegment(var2.e, Integer.valueOf(var19))));
            }
         }

         Object var15 = var2.lastPathSegment();

         for (io.karatelabs.ext.lint.JsonPathPlusSelector.NodeSelection var20 : (Iterable<io.karatelabs.ext.lint.JsonPathPlusSelector.NodeSelection>) (Iterable<?>) (var8)) {
            Object var21;
            try {
               var21 = var7.call(null, var20.d, var20.lastPathSegment(), var2.d, var15, var3, var20.jsonPath());
            } catch (RuntimeException var14) {
               continue;
            }

            if (Terms.isTruthy(var21)) {
               addUniqueMatch(var5, var6, var20);
            }
         }
      }
   }

   private static void followObjectKey(io.karatelabs.ext.lint.JsonPathPlusSelector.NodeSelection var0, String var1, List<io.karatelabs.ext.lint.JsonPathPlusSelector.NodeSelection> var2, Set<String> var3) {
      if (var0.d instanceof Map var4 && var4.containsKey(var1)) {
         addUniqueMatch(var2, var3, new io.karatelabs.ext.lint.JsonPathPlusSelector.NodeSelection(var4.get(var1), appendPathSegment(var0.e, var1)));
      }
   }

   private static void followArrayIndex(io.karatelabs.ext.lint.JsonPathPlusSelector.NodeSelection var0, int var1, List<io.karatelabs.ext.lint.JsonPathPlusSelector.NodeSelection> var2, Set<String> var3) {
      if (var0.d instanceof List var4) {
         int var6 = var1 < 0 ? var4.size() + var1 : var1;
         if (var6 >= 0 && var6 < var4.size()) {
            addUniqueMatch(var2, var3, new io.karatelabs.ext.lint.JsonPathPlusSelector.NodeSelection(var4.get(var6), appendPathSegment(var0.e, Integer.valueOf(var6))));
         }
      }
   }

   private static void addUniqueMatch(List<io.karatelabs.ext.lint.JsonPathPlusSelector.NodeSelection> var0, Set<String> var1, io.karatelabs.ext.lint.JsonPathPlusSelector.NodeSelection var2) {
      if (var1.add(toJsonPointer(var2.e))) {
         var0.add(var2);
      }
   }

   private static int clampSliceBound(int var0, int var1, int var2, boolean var3) {
      if (var0 == Integer.MIN_VALUE) {
         return var3 ? (var2 > 0 ? 0 : var1 - 1) : (var2 > 0 ? var1 : -1);
      } else {
         int var4 = var0 < 0 ? var1 + var0 : var0;
         int var5 = var2 > 0 ? 0 : -1;
         int var6 = var2 > 0 ? var1 : var1 - 1;
         return Math.max(var5, Math.min(var4, var6));
      }
   }

   private static List<Object> appendPathSegment(List<Object> var0, Object var1) {
      ArrayList<java.lang.Object> var2 = new ArrayList(var0.size() + 1);
      var2.addAll(var0);
      var2.add(var1);
      return List.copyOf(var2);
   }

   static Object resolvePath(Object var0, List<Object> var1) {
      Object var2 = var0;

      for (Object var4 : var1) {
         if (!(var2 instanceof Map var7)) {
            if (!(var2 instanceof List var5) || !(var4 instanceof Integer var6) || var6 < 0 || var6 >= var5.size()) {
               return null;
            }

            var2 = var5.get(var6);
         } else {
            var2 = var7.get(String.valueOf(var4));
         }
      }

      return var2;
   }

   static String toJsonPointer(List<Object> var0) {
      StringBuilder var1 = new StringBuilder();

      for (Object var3 : var0) {
         var1.append('/').append(String.valueOf(var3).replace("~", "~0").replace("/", "~1"));
      }

      return var1.toString();
   }

   private static List<io.karatelabs.ext.lint.JsonPathPlusSelector.SelectorStep> parseSteps(String var0) {
      ArrayList<io.karatelabs.ext.lint.JsonPathPlusSelector.SelectorStep> var1 = new ArrayList();
      int var2 = var0.length();
      int var3 = 0;

      while (var3 < var2 && Character.isWhitespace(var0.charAt(var3))) {
         var3++;
      }

      if (var3 < var2 && var0.charAt(var3) == '$') {
         var3++;
      }

      boolean var4 = false;

      while (var3 < var2) {
         char var5 = var0.charAt(var3);
         if (Character.isWhitespace(var5)) {
            var3++;
         } else if (var5 == '.') {
            if (var3 + 1 < var2 && var0.charAt(var3 + 1) == '.') {
               var4 = true;
               var3 += 2;
            } else {
               var3++;
               if ((var3 >= var2 || var0.charAt(var3) != '[') && var3 >= var2) {
                  throw selectorException(var0, "trailing '.' selects nothing");
               }
            }
         } else if (var5 == '[') {
            int var7 = findClosingDelimiter(var0, var3, '[', ']');
            var1.add(parseBracketStep(var0, var0.substring(var3 + 1, var7).trim(), var4));
            var4 = false;
            var3 = var7 + 1;
         } else if (var5 == '~') {
            var1.add(new io.karatelabs.ext.lint.JsonPathPlusSelector.SelectorStep(io.karatelabs.ext.lint.JsonPathPlusSelector.SelectorStepKind.KEYS, var4, null, null, null, null, null));
            var4 = false;
            var3++;
         } else if (var5 == '^') {
            var1.add(new io.karatelabs.ext.lint.JsonPathPlusSelector.SelectorStep(io.karatelabs.ext.lint.JsonPathPlusSelector.SelectorStepKind.PARENT, var4, null, null, null, null, null));
            var4 = false;
            var3++;
         } else if (var5 == '*') {
            var1.add(new io.karatelabs.ext.lint.JsonPathPlusSelector.SelectorStep(io.karatelabs.ext.lint.JsonPathPlusSelector.SelectorStepKind.WILDCARD, var4, null, null, null, null, null));
            var4 = false;
            var3++;
         } else {
            int var6 = var3;

            while (var3 < var2 && isNameCharacter(var0.charAt(var3))) {
               var3++;
            }

            if (var3 == var6) {
               throw selectorException(var0, "unexpected '" + var5 + "' at position " + var3);
            }

            var1.add(new io.karatelabs.ext.lint.JsonPathPlusSelector.SelectorStep(io.karatelabs.ext.lint.JsonPathPlusSelector.SelectorStepKind.CHILD, var4, var0.substring(var6, var3), null, null, null, null));
            var4 = false;
         }
      }

      if (var4) {
         throw selectorException(var0, "trailing '..' selects nothing");
      } else if (var1.isEmpty() && !var0.trim().equals("$")) {
         throw selectorException(var0, "no segments");
      } else {
         return var1;
      }
   }

   private static io.karatelabs.ext.lint.JsonPathPlusSelector.SelectorStep parseBracketStep(String var0, String var1, boolean var2) {
      if (var1.isEmpty()) {
         throw selectorException(var0, "empty []");
      } else if (var1.equals("*")) {
         return new io.karatelabs.ext.lint.JsonPathPlusSelector.SelectorStep(io.karatelabs.ext.lint.JsonPathPlusSelector.SelectorStepKind.WILDCARD, var2, null, null, null, null, null);
      } else if (var1.charAt(0) == '?') {
         String var7 = var1.substring(1).trim();
         if (var7.startsWith("(")) {
            int var9 = findClosingDelimiter(var7, 0, '(', ')');
            String var11 = var7.substring(var9 + 1).trim();
            if (!var11.isEmpty()) {
               throw selectorException(var0, "the filter ends at its closing ')' but '" + var11 + "' follows — wrap the whole condition in one `?( … )`");
            }

            var7 = var7.substring(1, var9).trim();
         }

         if (var7.isEmpty()) {
            throw selectorException(var0, "empty filter");
         } else {
            return new io.karatelabs.ext.lint.JsonPathPlusSelector.SelectorStep(io.karatelabs.ext.lint.JsonPathPlusSelector.SelectorStepKind.FILTER, var2, null, null, null, null, bindFilterVariables(var7));
         }
      } else {
         var var3 = splitTopLevel(var1);
         if (var3.size() <= 1) {
            int[] var8 = parseSliceBounds(var1);
            if (var8 != null) {
               return new io.karatelabs.ext.lint.JsonPathPlusSelector.SelectorStep(io.karatelabs.ext.lint.JsonPathPlusSelector.SelectorStepKind.SLICE, var2, null, null, var8, null, null);
            } else {
               Object var10 = parseBracketValue(var1);
               return var10 instanceof Integer var12
                  ? new io.karatelabs.ext.lint.JsonPathPlusSelector.SelectorStep(io.karatelabs.ext.lint.JsonPathPlusSelector.SelectorStepKind.INDEX, var2, null, var12, null, null, null)
                  : new io.karatelabs.ext.lint.JsonPathPlusSelector.SelectorStep(io.karatelabs.ext.lint.JsonPathPlusSelector.SelectorStepKind.CHILD, var2, String.valueOf(var10), null, null, null, null);
            }
         } else {
            ArrayList<java.lang.Object> var4 = new ArrayList(var3.size());

            for (String var6 : (Iterable<String>) (Iterable<?>) (var3)) {
               var4.add(parseBracketValue(var6.trim()));
            }

            return new io.karatelabs.ext.lint.JsonPathPlusSelector.SelectorStep(io.karatelabs.ext.lint.JsonPathPlusSelector.SelectorStepKind.UNION, var2, null, null, null, List.copyOf(var4), null);
         }
      }
   }

   private static Object parseBracketValue(String var0) {
      String var1 = var0.trim();
      if (var1.length() >= 2 && (var1.charAt(0) == '\'' || var1.charAt(0) == '"') && var1.charAt(var1.length() - 1) == var1.charAt(0)) {
         return unescapeQuoted(var1.substring(1, var1.length() - 1));
      } else {
         return var1.matches("-?\\d+") ? Integer.valueOf(var1) : var1;
      }
   }

   private static int[] parseSliceBounds(String var0) {
      if (var0.indexOf(58) < 0) {
         return null;
      } else {
         String[] var1 = var0.split(":", -1);
         if (var1.length >= 2 && var1.length <= 3) {
            int[] var2 = new int[]{Integer.MIN_VALUE, Integer.MIN_VALUE, 1};

            for (int var3 = 0; var3 < var1.length; var3++) {
               String var4 = var1[var3].trim();
               if (!var4.isEmpty()) {
                  if (!var4.matches("-?\\d+")) {
                     return null;
                  }

                  var2[var3] = Integer.parseInt(var4);
               }
            }

            if (var1.length < 3 || var1[2].isBlank()) {
               var2[2] = 1;
            }

            return var2;
         } else {
            return null;
         }
      }
   }

   private static boolean isNameCharacter(char var0) {
      return var0 != '.' && var0 != '[' && var0 != ']' && var0 != '~' && var0 != '^' && var0 != '*' && !Character.isWhitespace(var0);
   }

   static String bindFilterVariables(String var0) {
      StringBuilder var1 = new StringBuilder(var0.length() + 16);
      char var2 = 0;

      for (int var3 = 0; var3 < var0.length(); var3++) {
         char var4 = var0.charAt(var3);
         if (var4 != '\'' && var4 != '"') {
            if (var4 == '/' && canStartRegex(var2)) {
               int var10 = findRegexEnd(var0, var3);
               if (var10 > var3) {
                  var1.append(var0, var3, var10 + 1);
                  var3 = var10;
                  var2 = '/';
                  continue;
               }
            }

            if (var4 != '@') {
               var1.append(var4);
               if (!Character.isWhitespace(var4)) {
                  var2 = var4;
               }
            } else {
               boolean var11 = false;

               for (String[] var9 : FILTER_BINDINGS) {
                  if (var0.startsWith(var9[0], var3)) {
                     var1.append(var9[1]);
                     var3 += var9[0].length() - 1;
                     var11 = true;
                     break;
                  }
               }

               if (!var11) {
                  throw new LintException("filter body has an unbindable '@': " + var0);
               }

               var2 = 'x';
            }
         } else {
            int var5 = findStringEnd(var0, var3);
            var1.append(var0, var3, var5 + 1);
            var3 = var5;
            var2 = var4;
         }
      }

      return var1.toString();
   }

   static int findClosingDelimiter(String var0, int var1, char var2, char var3) {
      int var4 = 0;
      char var5 = 0;

      for (int var6 = var1; var6 < var0.length(); var6++) {
         char var7 = var0.charAt(var6);
         if (var7 != '\'' && var7 != '"') {
            if (var7 == '/' && canStartRegex(var5)) {
               int var8 = findRegexEnd(var0, var6);
               if (var8 > var6) {
                  var6 = var8;
                  var5 = '/';
                  continue;
               }
            }

            if (var7 == var2) {
               var4++;
            } else if (var7 == var3) {
               if (--var4 == 0) {
                  return var6;
               }
            }

            if (!Character.isWhitespace(var7)) {
               var5 = var7;
            }
         } else {
            var6 = findStringEnd(var0, var6);
            var5 = var7;
         }
      }

      throw selectorException(var0, "unbalanced '" + var2 + "' at position " + var1);
   }

   private static List<String> splitTopLevel(String var0) {
      ArrayList<java.lang.String> var1 = new ArrayList();
      int var2 = 0;
      char var3 = 0;

      for (int var4 = 0; var4 < var0.length(); var4++) {
         char var5 = var0.charAt(var4);
         if (var5 != '\'' && var5 != '"') {
            if (var5 == '/' && canStartRegex(var3)) {
               int var6 = findRegexEnd(var0, var4);
               if (var6 > var4) {
                  var4 = var6;
                  var3 = '/';
                  continue;
               }
            }

            if (var5 == ',') {
               var1.add(var0.substring(var2, var4));
               var2 = var4 + 1;
            }

            if (!Character.isWhitespace(var5)) {
               var3 = var5;
            }
         } else {
            var4 = findStringEnd(var0, var4);
            var3 = var5;
         }
      }

      var1.add(var0.substring(var2));
      return var1;
   }

   private static int findStringEnd(String var0, int var1) {
      char var2 = var0.charAt(var1);

      for (int var3 = var1 + 1; var3 < var0.length(); var3++) {
         char var4 = var0.charAt(var3);
         if (var4 == '\\') {
            var3++;
         } else if (var4 == var2) {
            return var3;
         }
      }

      throw selectorException(var0, "unterminated string literal at position " + var1);
   }

   private static int findRegexEnd(String var0, int var1) {
      boolean var2 = false;

      for (int var3 = var1 + 1; var3 < var0.length(); var3++) {
         char var4 = var0.charAt(var3);
         if (var4 == '\\') {
            var3++;
         } else if (var4 == '[') {
            var2 = true;
         } else if (var4 == ']') {
            var2 = false;
         } else {
            if (var4 == '/' && !var2) {
               int var5 = var3 + 1;

               while (var5 < var0.length() && Character.isLetter(var0.charAt(var5))) {
                  var5++;
               }

               return var5 - 1;
            }

            if (var4 == '\n') {
               return var1;
            }
         }
      }

      return var1;
   }

   private static boolean canStartRegex(char var0) {
      return var0 == 0
         ? true
         : !Character.isLetterOrDigit(var0) && var0 != '_' && var0 != '$' && var0 != ')' && var0 != ']' && var0 != '}' && var0 != '\'' && var0 != '"';
   }

   private static String unescapeQuoted(String var0) {
      return var0.replace("\\'", "'").replace("\\\"", "\"").replace("\\\\", "\\");
   }

   private static LintException selectorException(String var0, String var1) {
      return new LintException("bad jsonpath-plus selector '" + var0 + "': " + var1);
   }

   public interface FilterCompiler {
      JavaCallable compile(String var1);
   }

   public record NodeSelection(Object d, List<Object> e) {


      public Object lastPathSegment() {
         return this.e.isEmpty() ? null : this.e.get(this.e.size() - 1);
      }

      public String jsonPointer() {
         return io.karatelabs.ext.lint.JsonPathPlusSelector.toJsonPointer(this.e);
      }

      public String jsonPath() {
         StringBuilder var1 = new StringBuilder("$");

         for (Object var3 : this.e) {
            if (var3 instanceof Integer) {
               var1.append('[').append(var3).append(']');
            } else {
               var1.append("['").append(String.valueOf(var3).replace("'", "\\'")).append("']");
            }
         }

         return var1.toString();
      }

      public Object d() {
         return this.d;
      }

      public List<Object> e() {
         return this.e;
      }
   }

   private static enum SelectorStepKind {
      CHILD,
      WILDCARD,
      INDEX,
      SLICE,
      UNION,
      FILTER,
      KEYS,
      PARENT;
   }

   private record SelectorStep(io.karatelabs.ext.lint.JsonPathPlusSelector.SelectorStepKind a, boolean b, String c, Integer d, int[] e, List<Object> f, String g) {

   }
}
