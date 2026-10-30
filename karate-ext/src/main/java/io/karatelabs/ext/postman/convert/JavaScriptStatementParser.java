package io.karatelabs.ext.postman.convert;

import java.util.ArrayList;
import java.util.List;

final class JavaScriptStatementParser {
   private JavaScriptStatementParser() {
   }

   static List<io.karatelabs.ext.postman.convert.JavaScriptStatementParser.JavaScriptStatement> a(String var0) {
      ArrayList<io.karatelabs.ext.postman.convert.JavaScriptStatementParser.JavaScriptStatement> var1 = new ArrayList();
      String var2 = var0.replace("\r\n", "\n").replace('\r', '\n');
      int var3 = var2.length();
      int var4 = 0;

      while (var4 < var3) {
         while (var4 < var3 && (Character.isWhitespace(var2.charAt(var4)) || var2.charAt(var4) == ';')) {
            var4++;
         }

         if (var4 >= var3) {
            break;
         }

         if (var2.startsWith("//", var4)) {
            int var5 = var2.indexOf(10, var4);
            var5 = var5 < 0 ? var3 : var5;
            var1.add(new io.karatelabs.ext.postman.convert.JavaScriptStatementParser.JavaScriptStatement(var2.substring(var4, var5).strip(), true));
            var4 = var5;
         } else if (var2.startsWith("/*", var4)) {
            int var8 = var2.indexOf("*/", var4 + 2);
            var8 = var8 < 0 ? var3 : var8 + 2;
            var1.add(new io.karatelabs.ext.postman.convert.JavaScriptStatementParser.JavaScriptStatement(var2.substring(var4, var8).strip(), true));
            var4 = var8;
         } else {
            int var10 = c(var2, var4);
            String var6 = var2.substring(var4, var10).strip();
            if (var6.endsWith(";")) {
               var6 = var6.substring(0, var6.length() - 1).strip();
            }

            if (!var6.isEmpty()) {
               var1.add(new io.karatelabs.ext.postman.convert.JavaScriptStatementParser.JavaScriptStatement(var6, false));
            }

            var4 = var10;
         }
      }

      return var1;
   }

   private static int c(String var0, int var1) {
      int var2 = var0.length();
      int var3 = 0;
      char var4 = 0;
      boolean var5 = false;
      int var6 = var1;

      while (var6 < var2) {
         char var7 = var0.charAt(var6);
         if (var7 != '\'' && var7 != '"' && var7 != '`') {
            if (var7 == '/' && var6 + 1 < var2 && var0.charAt(var6 + 1) == '/') {
               int var9 = var0.indexOf(10, var6);
               if (var3 == 0) {
                  return var6;
               }

               var6 = var9 < 0 ? var2 : var9;
            } else if (var7 == '/' && var6 + 1 < var2 && var0.charAt(var6 + 1) == '*') {
               int var8 = var0.indexOf("*/", var6 + 2);
               var6 = var8 < 0 ? var2 : var8 + 2;
            } else if (var7 == '/' && a(var4)) {
               var6 = d(var0, var6);
               var4 = '/';
               var5 = true;
            } else {
               if (var7 == '(' || var7 == '[' || var7 == '{') {
                  var3++;
               } else if (var7 != ')' && var7 != ']' && var7 != '}') {
                  if (var7 == ';' && var3 == 0) {
                     return var6 + 1;
                  }

                  if (var7 == '\n' && var3 == 0 && a(var0, var1, var6, var5)) {
                     return var6;
                  }
               } else {
                  var3--;
                  if (var3 == 0 && var7 == '}' && a(var0, var1, var6)) {
                     return var6 + 1;
                  }
               }

               if (!Character.isWhitespace(var7)) {
                  var4 = var7;
                  var5 = false;
               }

               var6++;
            }
         } else {
            var6 = a(var0, var6);
            var4 = var7;
            var5 = false;
         }
      }

      return var2;
   }

   private static boolean a(String var0, int var1, int var2) {
      String var3 = var0.substring(var1, Math.min(var0.length(), var1 + 12));
      if (!var3.matches("(?s)^(if|for|while|function|try|switch|do|else)\\b.*")) {
         return false;
      } else {
         String var4 = var0.substring(var2 + 1).stripLeading();
         return !var4.startsWith("else") && !var4.startsWith("catch") && !var4.startsWith("finally") && !var4.startsWith("while");
      }
   }

   private static boolean a(String var0, int var1, int var2, boolean var3) {
      String var4 = var0.substring(var1, var2).strip();
      if (var4.isEmpty()) {
         return false;
      } else {
         char var5 = var4.charAt(var4.length() - 1);
         if (!var3 && "+-*/%=,.(&|?:!<>{[".indexOf(var5) >= 0) {
            return false;
         } else {
            String var6 = var0.substring(var2 + 1).stripLeading();
            if (var6.isEmpty()) {
               return true;
            } else if (var6.matches("(?s)^(else|catch|finally)\\b.*")) {
               return false;
            } else {
               char var7 = var6.charAt(0);
               return ".?:+-*/%=&|,)]}".indexOf(var7) < 0;
            }
         }
      }
   }

   private static boolean a(char var0) {
      return var0 == 0 || "(,=:[!&|?{};".indexOf(var0) >= 0;
   }

   static int a(String var0, int var1) {
      char var2 = var0.charAt(var1);
      int var3 = var1 + 1;

      while (var3 < var0.length()) {
         char var4 = var0.charAt(var3);
         if (var4 == '\\') {
            var3 += 2;
         } else {
            if (var4 == var2) {
               return var3 + 1;
            }

            if (var4 == '\n' && var2 != '`') {
               return var3;
            }

            var3++;
         }
      }

      return var0.length();
   }

   private static int d(String var0, int var1) {
      int var2 = var1 + 1;
      boolean var3 = false;

      while (var2 < var0.length()) {
         char var4 = var0.charAt(var2);
         if (var4 == '\\') {
            var2 += 2;
         } else {
            if (var4 == '\n') {
               return var2;
            }

            if (var4 == '[') {
               var3 = true;
            } else if (var4 == ']') {
               var3 = false;
            } else if (var4 == '/' && !var3) {
               var2++;

               while (var2 < var0.length() && Character.isLetter(var0.charAt(var2))) {
                  var2++;
               }

               return var2;
            }

            var2++;
         }
      }

      return var0.length();
   }

   static int b(String var0, int var1) {
      int var2 = 0;
      int var3 = var1;

      while (var3 < var0.length()) {
         char var4 = var0.charAt(var3);
         if (var4 != '\'' && var4 != '"' && var4 != '`') {
            if (var4 == '(' || var4 == '[' || var4 == '{') {
               var2++;
            } else if (var4 == ')' || var4 == ']' || var4 == '}') {
               if (--var2 == 0) {
                  return var3;
               }
            }

            var3++;
         } else {
            var3 = a(var0, var3);
         }
      }

      return -1;
   }

   static List<String> b(String var0) {
      ArrayList<java.lang.String> var1 = new ArrayList();
      int var2 = 0;
      int var3 = 0;
      int var4 = 0;

      while (var4 < var0.length()) {
         char var5 = var0.charAt(var4);
         if (var5 != '\'' && var5 != '"' && var5 != '`') {
            if (var5 == '(' || var5 == '[' || var5 == '{') {
               var2++;
            } else if (var5 == ')' || var5 == ']' || var5 == '}') {
               var2--;
            } else if (var5 == ',' && var2 == 0) {
               var1.add(var0.substring(var3, var4).strip());
               var3 = var4 + 1;
            }

            var4++;
         } else {
            var4 = a(var0, var4);
         }
      }

      String var6 = var0.substring(var3).strip();
      if (!var6.isEmpty() || !var1.isEmpty()) {
         var1.add(var6);
      }

      return var1;
   }

   static String c(String var0) {
      String var1 = var0.strip();
      if (var1.length() < 2) {
         return null;
      } else {
         char var2 = var1.charAt(0);
         if ((var2 == '\'' || var2 == '"' || var2 == '`') && var1.charAt(var1.length() - 1) == var2 && a(var1, 0) == var1.length()) {
            String var3 = var1.substring(1, var1.length() - 1);
            if (var2 == '`' && var3.contains("${")) {
               return null;
            } else {
               StringBuilder var4 = new StringBuilder();

               for (int var5 = 0; var5 < var3.length(); var5++) {
                  char var6 = var3.charAt(var5);
                  if (var6 == '\\' && var5 + 1 < var3.length()) {
                     char var7 = var3.charAt(++var5);

                     var4.append(switch (var7) {
                        case 'n' -> '\n';
                        case 'r' -> '\r';
                        case 't' -> '\t';
                        default -> var7;
                     });
                  } else {
                     var4.append(var6);
                  }
               }

               return var4.toString();
            }
         } else {
            return null;
         }
      }
   }

   record JavaScriptStatement(String a, boolean b) {
   }
}
