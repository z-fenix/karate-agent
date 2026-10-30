package io.karatelabs.ext.agent.runtime;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class CodeExtractor {
   private static final Pattern a = Pattern.compile("(?i)^\\s*`{3,}[ \\t]*(?:js|javascript)\\b[ \\t]*(.*?)[ \\t]*`{3,}\\s*$");
   private static final Pattern b = Pattern.compile("^\\s*`{3,}[ \\t]*([A-Za-z0-9_+#.-]*)[ \\t]*$");
   private static final Pattern c = Pattern.compile("^\\s*`{3,}\\s*$");
   private static final Pattern d = Pattern.compile("(?s)<(?:[A-Za-z_]+:)?invoke\\b[^>]*>(.*?)(?:</(?:[A-Za-z_]+:)?invoke>|\\z)");
   private static final Pattern e = Pattern.compile("(?s)<(?:[A-Za-z_]+:)?parameter\\b[^>]*>(.*?)(?:</(?:[A-Za-z_]+:)?parameter>|\\z)");
   private static final Pattern f = Pattern.compile("<(?:[A-Za-z_]+:)?(?:invoke|parameter|function_calls)\\b");
   private static final Pattern g = Pattern.compile(
      "^\\s*(?:(?:var|let|const|function|if|for|while|return|try|await)\\b|[A-Za-z_$][\\w$]*(?:\\.[\\w$]+)*\\s*\\(.*\\)\\s*;?\\s*$|[A-Za-z_$][\\w$.]*(?:\\[[^\\]]*])?\\s*[-+*/]?=(?!=)|[{}]|.*;\\s*$)"
   );

   private CodeExtractor() {
   }

   public static List<String> a(String var0) {
      return a(var0, io.karatelabs.ext.agent.runtime.CodeExtractor.ExtractionMode.LENIENT);
   }

   public static List<String> a(String var0, io.karatelabs.ext.agent.runtime.CodeExtractor.ExtractionMode var1) {
      return a(var0, var1, List.of());
   }

   public static List<String> a(String var0, io.karatelabs.ext.agent.runtime.CodeExtractor.ExtractionMode var1, List<String> var2) {
      if (var0 != null && !var0.isEmpty()) {
         var var3 = d(var0);
         ArrayList<java.lang.String> var4 = new ArrayList();
         ArrayList<java.lang.String> var5 = new ArrayList();

         for (io.karatelabs.ext.agent.runtime.CodeExtractor.CodeBlock var7 : (Iterable<io.karatelabs.ext.agent.runtime.CodeExtractor.CodeBlock>) (Iterable<?>) (var3)) {
            String var8 = var7.b.trim();
            if (!var8.isEmpty()) {
               if (c(var7.a)) {
                  var4.add(var8);
               } else if (var7.a.isEmpty()) {
                  var5.add(var8);
               }
            }
         }

         if (!var4.isEmpty()) {
            return var4;
         } else if (var1 != io.karatelabs.ext.agent.runtime.CodeExtractor.ExtractionMode.LENIENT) {
            return List.of();
         } else if (!var5.isEmpty()) {
            return var5;
         } else {
            var var9 = b(var0);
            if (!var9.isEmpty()) {
               return var9;
            } else if (f.matcher(var0).find()) {
               return List.of();
            } else {
               String var10 = a(var0, var2);
               return var10 != null ? List.of(var10) : List.of();
            }
         }
      } else {
         return List.of();
      }
   }

   private static List<String> b(String var0) {
      ArrayList<java.lang.String> var1 = new ArrayList();
      Matcher var2 = d.matcher(var0);

      while (var2.find()) {
         Matcher var3 = e.matcher(var2.group(1));
         ArrayList<java.lang.String> var4 = new ArrayList();

         while (var3.find()) {
            var4.add(var3.group(1).trim());
         }

         if (var4.size() == 1 && !((String)var4.get(0)).isEmpty()) {
            var1.add((String)var4.get(0));
         }
      }

      return var1;
   }

   private static String a(String var0, List<String> var1) {
      if (var1.isEmpty()) {
         return null;
      } else {
         StringBuilder var2 = new StringBuilder();

         for (String var4 : var1) {
            if (var2.length() > 0) {
               var2.append('|');
            }

            var2.append(Pattern.quote(var4));
         }

         Pattern var12 = Pattern.compile("^\\s*(?:await\\s+)?((?:" + var2 + ")\\(.*\\))\\s*;?\\s*$");
         boolean var13 = false;
         String var5 = null;

         for (String var9 : var0.split("\n", -1)) {
            String var10 = e(var9);
            if (b.matcher(var10).matches() || c.matcher(var10).matches()) {
               var13 = !var13;
            } else if (!var13) {
               Matcher var11 = var12.matcher(var10);
               if (var5 == null && var11.matches()) {
                  var5 = var11.group(1);
               } else if (g.matcher(var10).find()) {
                  return null;
               }
            }
         }

         return var5;
      }
   }

   private static boolean c(String var0) {
      return var0.equalsIgnoreCase("js") || var0.equalsIgnoreCase("javascript");
   }

   private static List<io.karatelabs.ext.agent.runtime.CodeExtractor.CodeBlock> d(String var0) {
      ArrayList<io.karatelabs.ext.agent.runtime.CodeExtractor.CodeBlock> var1 = new ArrayList();
      String[] var2 = var0.split("\n", -1);
      boolean var3 = false;
      String var4 = null;
      StringBuilder var5 = null;

      for (String var9 : var2) {
         String var10 = e(var9);
         if (!var3) {
            Matcher var11 = a.matcher(var10);
            if (var11.matches()) {
               var1.add(new io.karatelabs.ext.agent.runtime.CodeExtractor.CodeBlock("js", var11.group(1)));
            } else {
               Matcher var12 = b.matcher(var10);
               if (var12.matches()) {
                  var3 = true;
                  var4 = var12.group(1);
                  var5 = new StringBuilder();
               }
            }
         } else if (c.matcher(var10).matches()) {
            var1.add(new io.karatelabs.ext.agent.runtime.CodeExtractor.CodeBlock(var4, var5.toString()));
            var3 = false;
            var4 = null;
            var5 = null;
         } else {
            if (var5.length() > 0) {
               var5.append('\n');
            }

            var5.append(var10);
         }
      }

      if (var3 && var5 != null && !var5.isEmpty()) {
         var1.add(new io.karatelabs.ext.agent.runtime.CodeExtractor.CodeBlock(var4, var5.toString()));
      }

      return var1;
   }

   private static String e(String var0) {
      return var0.endsWith("\r") ? var0.substring(0, var0.length() - 1) : var0;
   }

   private record CodeBlock(String a, String b) {
   }

   public static enum ExtractionMode {
      STRICT,
      LENIENT;
   }
}
