package io.karatelabs.ext.postman.convert;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class PostmanScriptConverter {
   static final String a = "# not converted: ";
   private static final Pattern b = Pattern.compile("(?s)^(?:var|let|const)\\s+([A-Za-z_$][\\w$]*)\\s*=\\s*(.+)$");
   private static final Pattern c = Pattern.compile("(?s)^([A-Za-z_$][\\w$]*)\\s*=(?!=)\\s*(.+)$");
   private static final Pattern d = Pattern.compile(
      "pm\\.(?:environment|collectionVariables|variables|globals|iterationData)\\.get\\s*\\(\\s*(['\"])([^'\"]+)\\1\\s*\\)"
   );
   private static final Pattern e = Pattern.compile("postman\\.get(?:Environment|Global)Variable\\s*\\(\\s*(['\"])([^'\"]+)\\1\\s*\\)");
   private static final Pattern f = Pattern.compile(
      "(?s)^(?:pm\\.(?:environment|collectionVariables|variables|globals)\\.set|postman\\.set(?:Environment|Global)Variable)\\s*\\((.*)\\)$"
   );
   private static final Pattern g = Pattern.compile("(?s)^console\\.(?:log|info|warn|error|debug)\\s*\\((.*)\\)$");
   private static final Pattern h = Pattern.compile("(?s)^tests\\s*\\[\\s*(['\"])(.+?)\\1\\s*]\\s*=\\s*(.+)$");
   private static final Pattern i = Pattern.compile("(?s)^pm\\.response\\.to(\\..*)$");
   private static final Pattern j = Pattern.compile("(?<![\\w$.])(?:pm\\.|postman\\.|tests\\s*\\[|responseBody\\b|responseCode\\b|request\\.)");
   private static final Set<String> k = Set.of(
      "to", "be", "been", "is", "that", "which", "and", "has", "have", "with", "at", "of", "same", "does", "still", "also", "but", "own"
   );
   private static final Map<String, String> l = Map.ofEntries(
      Map.entry("ok", "Then status 200"),
      Map.entry("accepted", "Then status 202"),
      Map.entry("badRequest", "Then status 400"),
      Map.entry("unauthorized", "Then status 401"),
      Map.entry("unauthorised", "Then status 401"),
      Map.entry("forbidden", "Then status 403"),
      Map.entry("notFound", "Then status 404"),
      Map.entry("rateLimited", "Then status 429"),
      Map.entry("success", "* assert responseStatus >= 200 && responseStatus < 300"),
      Map.entry("info", "* assert responseStatus >= 100 && responseStatus < 200"),
      Map.entry("redirection", "* assert responseStatus >= 300 && responseStatus < 400"),
      Map.entry("clientError", "* assert responseStatus >= 400 && responseStatus < 500"),
      Map.entry("serverError", "* assert responseStatus >= 500 && responseStatus < 600"),
      Map.entry("error", "* assert responseStatus >= 400")
   );
   private static final Pattern m = Pattern.compile("(?s)^pm\\.request\\.(?:headers\\.(?:add|upsert)|addHeader)\\s*\\((.*)\\)$");
   private static final Pattern n = Pattern.compile("pm\\.request\\.headers\\.has\\s*\\(\\s*(['\"])([^'\"]+)\\1\\s*\\)");
   private static final Pattern o = Pattern.compile("^\\* header (\\S+) = (.+)$");
   private final Consumer<String> p;
   private final String q;
   private final Set<String> r;

   public PostmanScriptConverter(Consumer<String> var1) {
      this(var1, null, Set.of());
   }

   public PostmanScriptConverter(Consumer<String> var1, String var2, Set<String> var3) {
      this.p = var1;
      this.q = var2;
      this.r = var3;
   }

   public List<String> a(String var1) {
      ArrayList<java.lang.String> var2 = new ArrayList();
      if (var1 == null) {
         return var2;
      } else {
         for (io.karatelabs.ext.postman.convert.JavaScriptStatementParser.JavaScriptStatement var4 : (Iterable<io.karatelabs.ext.postman.convert.JavaScriptStatementParser.JavaScriptStatement>) (Iterable<?>) (io.karatelabs.ext.postman.convert.JavaScriptStatementParser.a(var1))) {
            if (var4.b()) {
               a(var4.a(), var2);
            } else {
               var var5 = this.c(var4.a());
               if (var5 == null) {
                  for (String var9 : var4.a().split("\n")) {
                     if (!var9.isBlank()) {
                        String var10 = var9.strip();
                        var2.add("# not converted: " + var10);
                        this.p.accept(var10);
                     }
                  }
               } else {
                  var2.addAll(var5);
               }
            }
         }

         return var2;
      }
   }

   private static void a(String var0, List<String> var1) {
      for (String var5 : var0.split("\n")) {
         String var6 = var5.strip().replaceFirst("^(//+|/\\*+|\\*+/?)\\s*", "").replaceFirst("\\s*\\*/$", "").strip();
         if (!var6.isEmpty()) {
            var1.add("# " + var6);
         }
      }
   }

   private List<String> c(String var1) {
      if (this.q != null) {
         var1 = var1.replace("pm.info.requestName", io.karatelabs.ext.postman.convert.PostmanVariableTranslator.b(this.q));
         var1 = n.matcher(var1).replaceAll(var1x -> this.r.contains(var1x.group(2).toLowerCase()) ? "true" : "false");
      }

      if (var1.matches("(?s)^if\\s*\\(.*")) {
         return this.e(var1);
      } else {
         Matcher var2 = m.matcher(var1);
         if (var2.matches()) {
            return d(var2.group(1));
         } else if (var1.startsWith("pm.test")) {
            return this.h(var1);
         } else if (var1.startsWith("pm.expect")) {
            String var13 = l(var1);
            return var13 == null ? null : List.of(var13);
         } else {
            Matcher var3 = i.matcher(var1);
            if (var3.matches()) {
               String var17 = k(var3.group(1));
               return var17 == null ? null : List.of(var17);
            } else {
               var3 = f.matcher(var1);
               if (var3.matches()) {
                  List var16 = io.karatelabs.ext.postman.convert.JavaScriptStatementParser.b(var3.group(1));
                  String var18 = var16.size() == 2 ? io.karatelabs.ext.postman.convert.JavaScriptStatementParser.c((String)var16.get(0)) : null;
                  String var19 = var16.size() == 2 ? b((String)var16.get(1)) : null;
                  return var18 != null && io.karatelabs.ext.postman.convert.PostmanVariableTranslator.a(var18) && var19 != null ? List.of("* def " + var18 + " = " + var19) : null;
               } else {
                  var3 = g.matcher(var1);
                  if (!var3.matches()) {
                     var3 = h.matcher(var1);
                     if (var3.matches()) {
                        String var15 = b(var3.group(3));
                        return var15 == null ? null : List.of("# " + var3.group(2), "* assert " + var15);
                     } else {
                        var3 = b.matcher(var1);
                        if (!var3.matches()) {
                           var3 = c.matcher(var1);
                        }

                        if (var3.matches()) {
                           String var14 = var3.group(1);
                           String var5 = var3.group(2).strip();
                           Matcher var6 = d.matcher(var5);
                           if (var6.matches() && var6.group(2).equals(var14)) {
                              return List.of("# " + var14 + ": " + (var5.contains("iterationData") ? "the data row's value" : "the variable as defined above"));
                           } else {
                              String var7 = b(var5);
                              return var7 == null ? null : List.of("* def " + var14 + " = " + var7);
                           }
                        } else {
                           return null;
                        }
                     }
                  } else {
                     String var4 = b(var3.group(1));
                     return var4 != null && !var4.isBlank() ? List.of("* print " + var4) : null;
                  }
               }
            }
         }
      }
   }

   private static List<String> d(String var0) {
      String var1 = var0.strip();
      if (var1.startsWith("{") && io.karatelabs.ext.postman.convert.JavaScriptStatementParser.b(var1, 0) == var1.length() - 1) {
         String var2 = null;
         String var3 = null;

         for (String var5 : (Iterable<String>) (Iterable<?>) (io.karatelabs.ext.postman.convert.JavaScriptStatementParser.b(var1.substring(1, var1.length() - 1)))) {
            int var6 = var5.indexOf(58);
            if (var6 < 0) {
               return null;
            }

            String var7 = var5.substring(0, var6).strip().replaceAll("^(['\"])(.*)\\1$", "$2");
            String var8 = var5.substring(var6 + 1).strip();
            if (var7.equals("key")) {
               var2 = io.karatelabs.ext.postman.convert.JavaScriptStatementParser.c(var8);
            } else if (!var7.equals("value")) {
               if (!var7.equals("disabled") || !var8.equals("false")) {
                  return null;
               }
            } else {
               var3 = b(var8);
            }
         }

         return var2 != null && var3 != null && var2.matches("[A-Za-z0-9!#$%&'*+.^_`|~-]+") ? List.of("* header " + var2 + " = " + var3) : null;
      } else {
         return null;
      }
   }

   private List<String> e(String var1) {
      int var2 = var1.indexOf(40);
      int var3 = io.karatelabs.ext.postman.convert.JavaScriptStatementParser.b(var1, var2);
      if (var3 < 0) {
         return null;
      } else {
         String var4 = var1.substring(var3 + 1).strip();
         String var5;
         if (var4.startsWith("{")) {
            int var6 = io.karatelabs.ext.postman.convert.JavaScriptStatementParser.b(var4, 0);
            if (var6 != var4.length() - 1) {
               return null;
            }

            var5 = var4.substring(1, var6);
         } else {
            var5 = var4;
         }

         if (var5.isBlank()) {
            return null;
         } else {
            ArrayList<java.lang.String> var13 = new ArrayList();

            for (String var8 : f(var1.substring(var2 + 1, var3))) {
               String var9 = b(var8);
               if (var9 == null) {
                  return null;
               }

               Boolean var10 = g(var9);
               if (Boolean.FALSE.equals(var10)) {
                  return List.of();
               }

               if (var10 == null) {
                  var13.add(var9);
               }
            }

            ArrayList var14 = new ArrayList();
            var var15 = new PostmanScriptConverter(var14::add, this.q, this.r).a(var5);
            if (!var14.isEmpty()) {
               return null;
            } else if (var13.isEmpty()) {
               return var15;
            } else {
               StringBuilder var16 = new StringBuilder();

               for (String var11 : (Iterable<String>) (Iterable<?>) (var15)) {
                  if (!var11.startsWith("# ")) {
                     Matcher var12 = o.matcher(var11);
                     if (!var12.matches()) {
                        return null;
                     }

                     var16.append(var16.isEmpty() ? "" : ", ").append(io.karatelabs.ext.postman.convert.PostmanVariableTranslator.b(var12.group(1))).append(": ").append(var12.group(2));
                  }
               }

               return var16.isEmpty() ? null : List.of("* headers (" + String.join(" && ", var13) + ") ? { " + var16 + " } : {}");
            }
         }
      }
   }

   private static List<String> f(String var0) {
      ArrayList<java.lang.String> var1 = new ArrayList();
      int var2 = 0;
      int var3 = 0;
      int var4 = 0;

      while (var4 < var0.length()) {
         char var5 = var0.charAt(var4);
         if (var5 != '\'' && var5 != '"' && var5 != '`') {
            if (var5 == '(' || var5 == '[' || var5 == '{') {
               var2++;
            } else if (var5 != ')' && var5 != ']' && var5 != '}') {
               if (var2 == 0 && (var0.startsWith("||", var4) || var5 == '?')) {
                  return List.of(var0.strip());
               }

               if (var2 == 0 && var0.startsWith("&&", var4)) {
                  var1.add(var0.substring(var3, var4).strip());
                  var3 = var4 + 2;
                  var4++;
               }
            } else {
               var2--;
            }

            var4++;
         } else {
            var4 = io.karatelabs.ext.postman.convert.JavaScriptStatementParser.a(var0, var4);
         }
      }

      var1.add(var0.substring(var3).strip());
      return var1;
   }

   private static Boolean g(String var0) {
      String var1 = var0.strip();

      boolean var2;
      for (var2 = false; var1.startsWith("!") && !var1.startsWith("!="); var1 = var1.substring(1).strip()) {
         var2 = !var2;
      }

      if (var1.equals("true") || var1.equals("false")) {
         return var2 != var1.equals("true");
      } else if (var2) {
         return null;
      } else {
         Matcher var3 = Pattern.compile("(?s)^(.+?)\\s*(===|!==|==|!=)\\s*(.+)$").matcher(var1);
         if (var3.matches()) {
            String var4 = io.karatelabs.ext.postman.convert.JavaScriptStatementParser.c(var3.group(1));
            String var5 = io.karatelabs.ext.postman.convert.JavaScriptStatementParser.c(var3.group(3));
            if (var4 != null && var5 != null) {
               return var4.equals(var5) == var3.group(2).startsWith("=");
            }
         }

         return null;
      }
   }

   private List<String> h(String var1) {
      int var2 = var1.indexOf(40);
      int var3 = var2 < 0 ? -1 : io.karatelabs.ext.postman.convert.JavaScriptStatementParser.b(var1, var2);
      if (var3 >= 0 && var1.substring(var3 + 1).isBlank() && var1.substring(0, var2).strip().equals("pm.test")) {
         List var4 = io.karatelabs.ext.postman.convert.JavaScriptStatementParser.b(var1.substring(var2 + 1, var3));
         String var5 = var4.size() == 2 ? io.karatelabs.ext.postman.convert.JavaScriptStatementParser.c((String)var4.get(0)) : null;
         if (var5 == null) {
            return null;
         } else {
            String var6 = (String)var4.get(1);
            int var8 = var6.indexOf(123);
            String var7;
            if (var6.matches("(?s)^(function\\s*[\\w$]*\\s*\\([^)]*\\)|\\([^)]*\\)\\s*=>|[A-Za-z_$][\\w$]*\\s*=>)\\s*\\{.*")) {
               int var9 = io.karatelabs.ext.postman.convert.JavaScriptStatementParser.b(var6, var8);
               if (var9 != var6.length() - 1) {
                  return null;
               }

               var7 = var6.substring(var8 + 1, var9);
            } else {
               if (!var6.matches("(?s)^\\(\\s*\\)\\s*=>.*")) {
                  return null;
               }

               var7 = var6.substring(var6.indexOf("=>") + 2);
            }

            ArrayList<java.lang.String> var10 = new ArrayList();
            var10.add("# " + var5.replace('\n', ' '));
            var10.addAll(this.a(var7));
            return var10;
         }
      } else {
         return null;
      }
   }

   static String b(String var0) {
      String var1 = var0.strip();
      var1 = var1.replace("pm.response.json()", "response")
         .replaceAll("JSON\\.parse\\s*\\(\\s*(?:responseBody|pm\\.response\\.text\\(\\))\\s*\\)", "response")
         .replace("pm.response.code", "responseStatus")
         .replace("pm.response.responseTime", "responseTime")
         .replace("responseCode.code", "responseStatus")
         .replaceAll("pm\\.response\\.headers\\.get\\s*\\(", "karate.response.header(");
      var1 = d.matcher(var1).replaceAll(var0x -> Matcher.quoteReplacement(i(var0x.group(2))));
      var1 = e.matcher(var1).replaceAll(var0x -> Matcher.quoteReplacement(i(var0x.group(2))));
      String var2 = j(var1);
      return !j.matcher(var2).find() && (!var1.contains("\n") || !var2.contains("//") && !var1.contains("`")) ? var1.replaceAll("\\s*\\n\\s*", " ") : null;
   }

   private static String i(String var0) {
      return io.karatelabs.ext.postman.convert.PostmanVariableTranslator.a(var0) ? var0 : "karate.get(" + io.karatelabs.ext.postman.convert.PostmanVariableTranslator.b(var0) + ")";
   }

   private static String j(String var0) {
      StringBuilder var1 = new StringBuilder();
      int var2 = 0;

      while (var2 < var0.length()) {
         char var3 = var0.charAt(var2);
         if (var3 != '\'' && var3 != '"' && var3 != '`') {
            var1.append(var3);
            var2++;
         } else {
            int var4 = io.karatelabs.ext.postman.convert.JavaScriptStatementParser.a(var0, var2);
            var1.append("''");
            var2 = var4;
         }
      }

      return var1.toString();
   }

   private static String k(String var0) {
      var var1 = m(var0);
      if (var1 == null) {
         return null;
      } else {
         boolean var2 = false;
         String[] var3 = null;

         for (String[] var5 : (Iterable<String[]>) (Iterable<?>) (var1)) {
            if (!k.contains(var5[0]) || var5[1] != null) {
               if ("not".equals(var5[0]) && var5[1] == null) {
                  var2 = !var2;
               } else {
                  if (var3 != null) {
                     return null;
                  }

                  var3 = var5;
               }
            }
         }

         if (var3 == null) {
            return null;
         } else {
            Object var10 = var3[0];
            Object var11 = var3[1];
            if ("status".equals(var10) && var11 != null && ((java.lang.String)var11).strip().matches("\\d{3}")) {
               return var2 ? "* assert responseStatus != " + ((java.lang.String)var11).strip() : "Then status " + ((java.lang.String)var11).strip();
            } else if ("header".equals(var10) && var11 != null) {
               List var6 = io.karatelabs.ext.postman.convert.JavaScriptStatementParser.b((String)var11);
               String var7 = var6.isEmpty() ? null : b((String)var6.get(0));
               if (var7 != null && var6.size() <= 2) {
                  String var8 = "karate.response.header(" + var7 + ")";
                  if (var6.size() == 1) {
                     return "* match " + var8 + (var2 ? " == '#null'" : " == '#notnull'");
                  } else {
                     String var9 = b((String)var6.get(1));
                     return var9 == null ? null : "* match " + var8 + (var2 ? " != " : " == ") + var9;
                  }
               } else {
                  return null;
               }
            } else {
               return var11 == null && !var2 && l.containsKey(var10) ? l.get(var10) : null;
            }
         }
      }
   }

   private static String l(String var0) {
      int var1 = var0.indexOf(40);
      int var2 = io.karatelabs.ext.postman.convert.JavaScriptStatementParser.b(var0, var1);
      if (var2 >= 0 && var0.substring(0, var1).strip().equals("pm.expect")) {
         List var3 = io.karatelabs.ext.postman.convert.JavaScriptStatementParser.b(var0.substring(var1 + 1, var2));
         if (!var3.isEmpty() && var3.size() <= 2) {
            String var4 = b((String)var3.get(0));
            var var5 = m(var0.substring(var2 + 1));
            if (var4 != null && var5 != null) {
               boolean var6 = false;
               boolean var7 = false;
               String[] var8 = null;

               for (String[] var10 : (Iterable<String[]>) (Iterable<?>) (var5)) {
                  String var11 = var10[0];
                  if (var10[1] != null || !k.contains(var11)) {
                     if (var10[1] == null && "not".equals(var11)) {
                        var6 = !var6;
                     } else if (var10[1] == null && "deep".equals(var11)) {
                        var7 = true;
                     } else if (var10[1] == null
                        && ("length".equals(var11) || "lengthOf".equals(var11))
                        && var8 == null
                        && var5.indexOf(var10) < var5.size() - 1) {
                        var4 = var4 + ".length";
                     } else {
                        if (var8 != null) {
                           return null;
                        }

                        var8 = var10;
                     }
                  }
               }

               return var8 == null ? null : a(var4, var8[0], var8[1], var6, var7);
            } else {
               return null;
            }
         } else {
            return null;
         }
      } else {
         return null;
      }
   }

   private static String a(String var0, String var1, String var2, boolean var3, boolean var4) {
      String var5 = var3 ? " != " : " == ";
      if (var2 == null) {
         return switch (var1) {
            case "true" -> "* match " + var0 + var5 + "true";
            case "false" -> "* match " + var0 + var5 + "false";
            case "null" -> "* match " + var0 + (var3 ? " == '#notnull'" : " == '#null'");
            case "undefined" -> "* match " + var0 + (var3 ? " == '#present'" : " == '#notpresent'");
            case "ok", "exist", "exists" -> "* match " + var0 + (var3 ? " == '#null'" : " == '#notnull'");
            case "empty" -> "* assert " + var0 + ".length " + (var3 ? "> 0" : "== 0");
            default -> null;
         };
      } else {
         List var6 = io.karatelabs.ext.postman.convert.JavaScriptStatementParser.b(var2);
         String var7 = var6.isEmpty() ? null : b((String)var6.get(0));
         if (var7 == null) {
            return null;
         } else {
            return switch (var1) {
               case "equal", "equals", "eq", "eql", "eqls" -> "* match " + var0 + var5 + var7;
               case "a", "an" -> {
                  String var16 = io.karatelabs.ext.postman.convert.JavaScriptStatementParser.c((String)var6.get(0));
                  String var19;
                  if (var16 == null) {
                     var19 = null;
                  } else {
                     String var18 = var16.toLowerCase();
                     switch (var18) {
                        case "string":
                        case "number":
                        case "boolean":
                        case "object":
                        case "array":
                        case "null":
                           var19 = "'#" + var16.toLowerCase() + "'";
                           break;
                        case "undefined":
                           var19 = "'#notpresent'";
                           break;
                        default:
                           var19 = null;
                     }
                  }

                  String var17 = var19;
                  yield var17 == null ? null : "* match " + var0 + var5 + var17;
               }
               case "include", "includes", "contain", "contains" -> "* match "
                  + var0
                  + (var3 ? " !contains " : (var4 ? " contains deep " : " contains "))
                  + var7;
               case "property" -> {
                  String var15 = io.karatelabs.ext.postman.convert.JavaScriptStatementParser.c((String)var6.get(0));
                  if (var15 != null && var6.size() <= 2) {
                     String var11 = var0 + (io.karatelabs.ext.postman.convert.PostmanVariableTranslator.a(var15) ? "." + var15 : "[" + io.karatelabs.ext.postman.convert.PostmanVariableTranslator.b(var15) + "]");
                     if (var6.size() == 2) {
                        String var12 = b((String)var6.get(1));
                        yield var12 == null ? null : "* match " + var11 + var5 + var12;
                     } else {
                        yield "* match " + var11 + (var3 ? " == '#notpresent'" : " == '#present'");
                     }
                  } else {
                     yield null;
                  }
               }
               case "length", "lengthOf" -> "* assert " + var0 + ".length" + (var3 ? " != " : " == ") + var7;
               case "match", "matches" -> {
                  String var10 = ((String)var6.get(0)).strip();
                  yield !var10.matches("^/.+/$")
                     ? null
                     : "* match " + var0 + var5 + io.karatelabs.ext.postman.convert.PostmanVariableTranslator.b("#regex " + var10.substring(1, var10.length() - 1));
               }
               case "above", "gt", "greaterThan" -> "* assert " + var0 + (var3 ? " <= " : " > ") + var7;
               case "below", "lt", "lessThan" -> "* assert " + var0 + (var3 ? " >= " : " < ") + var7;
               case "least", "gte" -> "* assert " + var0 + (var3 ? " < " : " >= ") + var7;
               case "most", "lte" -> "* assert " + var0 + (var3 ? " > " : " <= ") + var7;
               case "oneOf" -> "* match " + var7 + (var3 ? " !contains " : " contains ") + var0;
               default -> null;
            };
         }
      }
   }

   private static List<String[]> m(String var0) {
      String var1 = var0.strip();
      ArrayList<java.lang.String[]> var2 = new ArrayList();
      int var3 = 0;

      while (var3 < var1.length()) {
         if (var1.charAt(var3) != '.') {
            return null;
         }

         int var4 = ++var3;

         while (var3 < var1.length() && (Character.isLetterOrDigit(var1.charAt(var3)) || var1.charAt(var3) == '_' || var1.charAt(var3) == '$')) {
            var3++;
         }

         if (var3 == var4) {
            return null;
         }

         String var5 = var1.substring(var4, var3);
         String var6 = null;
         if (var3 < var1.length() && var1.charAt(var3) == '(') {
            int var7 = io.karatelabs.ext.postman.convert.JavaScriptStatementParser.b(var1, var3);
            if (var7 < 0) {
               return null;
            }

            var6 = var1.substring(var3 + 1, var7);
            var3 = var7 + 1;
         }

         var2.add(new String[]{var5, var6});
      }

      return var2.isEmpty() ? null : var2;
   }
}
