package io.karatelabs.ext.max.apitest;

import io.karatelabs.gherkin.GherkinParser;
import io.karatelabs.gherkin.MatchExpression;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class StepClassifier {
   private static final Set<String> a = Set.of(
      "url", "path", "param", "params", "header", "headers", "cookie", "cookies", "form field", "form fields", "request"
   );
   private static final Set<String> b = Set.of(
      "print",
      "def",
      "text",
      "json",
      "xml",
      "csv",
      "yaml",
      "string",
      "bytes",
      "table",
      "set",
      "remove",
      "copy",
      "replace",
      "configure",
      "multipart file",
      "multipart files",
      "multipart field",
      "multipart fields",
      "multipart entity",
      "doc",
      "listen",
      "soap action"
   );
   private static final Set<String> c = Set.of("def", "text", "json", "xml", "string", "bytes", "csv", "yaml", "table", "copy", "set", "remove", "replace");
   private static final Pattern d = Pattern.compile("^\\s*([A-Za-z_$][\\w$]*)");
   private static final Pattern e = Pattern.compile("(^|[^\\w$.])(response\\w*|\\$)(?![\\w$])");
   private static final Pattern f = Pattern.compile("[A-Za-z_$][\\w$]*");
   private static final Pattern g = Pattern.compile("'(?:[^'\\\\]|\\\\.)*'|\"(?:[^\"\\\\]|\\\\.)*\"");
   private static final Pattern h = Pattern.compile("#\\(([^)]*)\\)");
   private static final Set<String> i = Set.of(
      "true", "false", "null", "undefined", "new", "typeof", "function", "return", "var", "let", "const", "if", "else", "in", "of", "this"
   );

   private StepClassifier() {
   }

   static boolean a(String var0) {
      return var0 != null && a.contains(var0);
   }

   static io.karatelabs.ext.max.apitest.StepClassifier.StepKind a(io.karatelabs.ext.max.apitest.FeatureSnapshot.StepSnapshot var0) {
      String var1 = var0.d();
      if (var1 == null) {
         return io.karatelabs.ext.max.apitest.StepClassifier.StepKind.ADVANCED;
      } else if (a.contains(var1)) {
         return io.karatelabs.ext.max.apitest.StepClassifier.StepKind.CALL_INPUT;
      } else {
         switch (var1) {
            case "method":
               return io.karatelabs.ext.max.apitest.StepClassifier.StepKind.CALL_METHOD;
            case "status":
               return io.karatelabs.ext.max.apitest.StepClassifier.StepKind.CHECK_STATUS;
            case "match":
               return d(var0) == null ? io.karatelabs.ext.max.apitest.StepClassifier.StepKind.ADVANCED : io.karatelabs.ext.max.apitest.StepClassifier.StepKind.CHECK_MATCH;
            case "def":
               String var4 = f(var0);
               return var4 != null && e.matcher(var4).find() ? io.karatelabs.ext.max.apitest.StepClassifier.StepKind.REMEMBER : io.karatelabs.ext.max.apitest.StepClassifier.StepKind.ADVANCED;
            default:
               return io.karatelabs.ext.max.apitest.StepClassifier.StepKind.ADVANCED;
         }
      }
   }

   static io.karatelabs.ext.max.apitest.StepClassifier.AssertionClassification b(io.karatelabs.ext.max.apitest.FeatureSnapshot.StepSnapshot var0) {
      String var1 = var0.d();
      if (var1 == null) {
         return io.karatelabs.ext.max.apitest.StepClassifier.AssertionClassification.UNKNOWN;
      } else if ("assert".equals(var1) || "retry until".equals(var1) || "match".equals(var1)) {
         return io.karatelabs.ext.max.apitest.StepClassifier.AssertionClassification.KNOWN;
      } else if ("def".equals(var1)) {
         return c(f(var0)) ? io.karatelabs.ext.max.apitest.StepClassifier.AssertionClassification.NONE : io.karatelabs.ext.max.apitest.StepClassifier.AssertionClassification.UNKNOWN;
      } else {
         return !a.contains(var1) && !"method".equals(var1) && !"status".equals(var1) && !b.contains(var1)
            ? io.karatelabs.ext.max.apitest.StepClassifier.AssertionClassification.UNKNOWN
            : io.karatelabs.ext.max.apitest.StepClassifier.AssertionClassification.NONE;
      }
   }

   private static boolean c(String var0) {
      return var0 != null && g.matcher(var0).replaceAll(" ").indexOf(40) < 0;
   }

   static String c(io.karatelabs.ext.max.apitest.FeatureSnapshot.StepSnapshot var0) {
      if (var0.d() != null && c.contains(var0.d()) && var0.e() != null) {
         Matcher var1 = d.matcher(var0.e());
         return var1.find() ? var1.group(1) : null;
      } else {
         return null;
      }
   }

   static io.karatelabs.ext.max.apitest.StepClassifier.MatchExpressionSnapshot d(io.karatelabs.ext.max.apitest.FeatureSnapshot.StepSnapshot var0) {
      if ("match".equals(var0.d()) && var0.e() != null) {
         try {
            MatchExpression var1 = GherkinParser.parseMatchExpression(var0.e().strip());
            String var2 = var1.getExpectedExpr() == null ? "" : var1.getExpectedExpr().strip();
            if (var2.isEmpty()) {
               if (var0.f() == null) {
                  return null;
               }

               var2 = var0.f();
            }

            return new io.karatelabs.ext.max.apitest.StepClassifier.MatchExpressionSnapshot(var1.isEach(), var1.getActualExpr().strip(), var1.getOperator(), var2);
         } catch (RuntimeException var3) {
            return null;
         }
      } else {
         return null;
      }
   }

   static String e(io.karatelabs.ext.max.apitest.FeatureSnapshot.StepSnapshot var0) {
      if ("def".equals(var0.d()) && var0.e() != null) {
         int var1 = var0.e().indexOf(61);
         String var2 = (var1 < 0 ? var0.e() : var0.e().substring(0, var1)).strip();
         return f.matcher(var2).matches() ? var2 : null;
      } else {
         return null;
      }
   }

   static String f(io.karatelabs.ext.max.apitest.FeatureSnapshot.StepSnapshot var0) {
      if ("def".equals(var0.d()) && var0.e() != null) {
         int var1 = var0.e().indexOf(61);
         if (var1 < 0) {
            return null;
         } else {
            String var2 = var0.e().substring(var1 + 1).strip();
            return var2.isEmpty() ? var0.f() : var2;
         }
      } else {
         return null;
      }
   }

   static Set<String> b(String var0) {
      LinkedHashSet<java.lang.String> var1 = new LinkedHashSet();
      if (var0 == null) {
         return var1;
      } else {
         StringBuilder var2 = new StringBuilder(g.matcher(var0).replaceAll(" "));
         Matcher var3 = h.matcher(var0);

         while (var3.find()) {
            var2.append(' ').append(var3.group(1));
         }

         Matcher var4 = f.matcher(var2);

         while (var4.find()) {
            String var5 = var4.group();
            if (!i.contains(var5)) {
               var1.add(var5);
            }
         }

         return var1;
      }
   }

   static Set<String> g(io.karatelabs.ext.max.apitest.FeatureSnapshot.StepSnapshot var0) {
      var var1 = b(var0.e());
      var1.addAll(b(var0.f()));
      if (var0.g() != null) {
         for (List var3 : var0.g()) {
            for (String var5 : (Iterable<String>) (Iterable<?>) (var3)) {
               var1.addAll(b(var5));
            }
         }
      }

      return var1;
   }

   static enum AssertionClassification {
      NONE,
      KNOWN,
      UNKNOWN;
   }

   record MatchExpressionSnapshot(boolean a, String b, String c, String d) {
   }

   static enum StepKind {
      CALL_INPUT,
      CALL_METHOD,
      CHECK_STATUS,
      CHECK_MATCH,
      REMEMBER,
      ADVANCED;
   }
}
