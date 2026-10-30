package io.karatelabs.ext.lint;

import io.karatelabs.js.SimpleObject;
import io.karatelabs.js.Terms;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.regex.Pattern;

public final class LintFns implements SimpleObject {
   public static final List<String> NAMES = List.of(
      "truthy",
      "falsy",
      "defined",
      "undefined",
      "pattern",
      "casing",
      "length",
      "enumeration",
      "alphabetical",
      "schema",
      "xor",
      "or",
      "unreferencedReusableObject"
   );
   private final Supplier<Object> document;
   private static final Map<String, String[]> CASES = new LinkedHashMap<>();

   LintFns(Supplier<Object> var1) {
      this.document = var1;
   }

   @Override
   public Collection<String> jsKeys() {
      return NAMES;
   }

   @Override
   public Object jsGet(String var1) {
      return switch (var1) {
         case "truthy" -> (io.karatelabs.js.JavaInvokable) var0 -> truthy(arg(var0, 0));
         case "falsy" -> (io.karatelabs.js.JavaInvokable) var0 -> falsy(arg(var0, 0));
         case "defined" -> (io.karatelabs.js.JavaInvokable) var0 -> defined(arg(var0, 0));
         case "undefined" -> (io.karatelabs.js.JavaInvokable) var0 -> undefined(arg(var0, 0));
         case "pattern" -> (io.karatelabs.js.JavaInvokable) var0 -> pattern(arg(var0, 0), opts(var0, 1));
         case "casing" -> (io.karatelabs.js.JavaInvokable) var0 -> casing(arg(var0, 0), opts(var0, 1));
         case "length" -> (io.karatelabs.js.JavaInvokable) var0 -> length(arg(var0, 0), opts(var0, 1));
         case "enumeration" -> (io.karatelabs.js.JavaInvokable) var0 -> enumeration(arg(var0, 0), opts(var0, 1));
         case "alphabetical" -> (io.karatelabs.js.JavaInvokable) var0 -> alphabetical(arg(var0, 0), opts(var0, 1));
         case "schema" -> (io.karatelabs.js.JavaInvokable) var0 -> schema(arg(var0, 0), opts(var0, 1));
         case "xor" -> (io.karatelabs.js.JavaInvokable) var0 -> xor(arg(var0, 0), opts(var0, 1));
         case "or" -> (io.karatelabs.js.JavaInvokable) var0 -> or(arg(var0, 0), opts(var0, 1));
         case "unreferencedReusableObject" -> (io.karatelabs.js.JavaInvokable) var1x -> this.unreferenced(arg(var1x, 0), opts(var1x, 1));
         default -> null;
      };
   }

   static Object truthy(Object var0) {
      return isTruthy(var0) ? null : "must be truthy, is " + show(var0);
   }

   static Object falsy(Object var0) {
      return isTruthy(var0) ? "must be falsy, is " + show(var0) : null;
   }

   static Object defined(Object var0) {
      return var0 == null ? "must be defined" : null;
   }

   static Object undefined(Object var0) {
      return var0 == null ? null : "must be undefined, is " + show(var0);
   }

   static Object pattern(Object var0, Map<String, Object> var1) {
      if (var0 == null) {
         return null;
      } else {
         String var2 = String.valueOf(var0);
         String var3 = str(var1, "match");
         String var4 = str(var1, "notMatch");
         if (var3 == null && var4 == null) {
            throw new LintException("fn.pattern needs {match} and/or {notMatch}");
         } else if (var3 != null && !io.karatelabs.ext.jsonschema.validation.EcmaRegex.find(var3, var2)) {
            return "'" + var2 + "' must match /" + var3 + "/";
         } else {
            return var4 != null && io.karatelabs.ext.jsonschema.validation.EcmaRegex.find(var4, var2) ? "'" + var2 + "' must not match /" + var4 + "/" : null;
         }
      }
   }

   static Object casing(Object var0, Map<String, Object> var1) {
      if (var0 == null) {
         return null;
      } else {
         String var2 = String.valueOf(var0);
         if (var2.isEmpty()) {
            return null;
         } else {
            String var3 = str(var1, "type");
            if (var3 == null) {
               throw new LintException("fn.casing needs {type} — one of " + String.join("/", CASES.keySet()));
            } else {
               String[] var4 = CASES.get(var3);
               if (var4 == null) {
                  throw new LintException("fn.casing: unknown type '" + var3 + "' (known: " + String.join(", ", CASES.keySet()) + ")");
               } else {
                  boolean var5 = !isTruthy(raw(var1, "disallowDigits"));
                  String var6 = var5 ? var4[0] : var4[1];
                  String var7 = "^" + var6 + "$";
                  String var8 = var2;
                  if (raw(var1, "separator") instanceof Map var10) {
                     Object var11 = var10.get("char");
                     if (var11 != null && var11 != Terms.UNDEFINED && !String.valueOf(var11).isEmpty()) {
                        String var12 = String.valueOf(var11);
                        boolean var13 = isTruthy(var10.get("allowLeading") == Terms.UNDEFINED ? null : var10.get("allowLeading"));
                        if (var2.startsWith(var12)) {
                           if (!var13) {
                              return "'" + var2 + "' must not start with '" + var12 + "'";
                           }

                           var8 = var2.substring(var12.length());
                        }

                        for (String var17 : var8.split(Pattern.quote(var12))) {
                           if (!var17.matches(var7)) {
                              return "'" + var2 + "' is not " + var3 + " case (at '" + var17 + "')";
                           }
                        }

                        return null;
                     } else {
                        throw new LintException("fn.casing: {separator} needs a `char`");
                     }
                  } else {
                     return var2.matches(var7) ? null : "'" + var2 + "' is not " + var3 + " case";
                  }
               }
            }
         }
      }
   }

   static Object length(Object var0, Map<String, Object> var1) {
      if (var0 == null) {
         return null;
      } else {
         Integer var2 = num(var1, "min");
         Integer var3 = num(var1, "max");
         if (var2 == null && var3 == null) {
            throw new LintException("fn.length needs {min} and/or {max}");
         } else {
            int var4;
            String var5;
            if (var0 instanceof Number var6) {
               var4 = var6.intValue();
               var5 = "value";
            } else if (var0 instanceof Collection var7) {
               var4 = var7.size();
               var5 = "length";
            } else if (var0 instanceof Map var8) {
               var4 = var8.size();
               var5 = "property count";
            } else {
               var4 = String.valueOf(var0).length();
               var5 = "length";
            }

            if (var2 != null && var4 < var2) {
               return var5 + " " + var4 + " is below the minimum of " + var2;
            } else {
               return var3 != null && var4 > var3 ? var5 + " " + var4 + " is above the maximum of " + var3 : null;
            }
         }
      }
   }

   static Object enumeration(Object var0, Map<String, Object> var1) {
      if (var0 == null) {
         return null;
      } else if (raw(var1, "values") instanceof Collection var3 && !var3.isEmpty()) {
         for (Object var5 : var3) {
            if (String.valueOf(var5).equals(String.valueOf(var0))) {
               return null;
            }
         }

         ArrayList<java.lang.String> var6 = new ArrayList();
         var3.forEach(var1x -> var6.add(String.valueOf(var1x)));
         return "'" + var0 + "' is not one of " + String.join(", ", var6);
      } else {
         throw new LintException("fn.enumeration needs a non-empty {values} list");
      }
   }

   static Object alphabetical(Object var0, Map<String, Object> var1) {
      String var2 = str(var1, "keyedBy");
      ArrayList<java.lang.Object> var3 = new ArrayList();
      ArrayList<java.lang.Object> var4 = new ArrayList();
      if (var0 instanceof Collection var6) {
         int var7 = 0;

         for (Object var9 : var6) {
            Object var10 = var2 != null && var9 instanceof Map var11 ? var11.get(var2) : var9;
            var3.add(var10 == null ? "" : String.valueOf(var10));
            var4.add(var7++);
         }
      } else {
         if (!(var0 instanceof Map var5)) {
            return null;
         }

         for (Object var15 : var5.keySet()) {
            var3.add(String.valueOf(var15));
            var4.add(String.valueOf(var15));
         }
      }

      ArrayList<java.util.Map<java.lang.String,java.lang.Object>> var12 = new ArrayList();

      for (int var14 = 1; var14 < var3.size(); var14++) {
         if (((String)var3.get(var14 - 1)).compareTo((String)var3.get(var14)) > 0) {
            var12.add(finding("'" + (String)var3.get(var14) + "' must sort before '" + (String)var3.get(var14 - 1) + "'", List.of(var4.get(var14))));
         }
      }

      return var12.isEmpty() ? null : var12;
   }

   static Object xor(Object var0, Map<String, Object> var1) {
      var var2 = present(var0, names(var1, "fn.xor"));
      if (var2.size() == 1) {
         return null;
      } else {
         return var2.isEmpty()
            ? "exactly one of " + String.join("/", names(var1, "fn.xor")) + " must be present, none is"
            : "exactly one of " + String.join("/", names(var1, "fn.xor")) + " must be present, found " + String.join(" + ", var2);
      }
   }

   static Object or(Object var0, Map<String, Object> var1) {
      var var2 = names(var1, "fn.or");
      return present(var0, var2).isEmpty() ? "at least one of " + String.join("/", var2) + " must be present" : null;
   }

   private static List<String> present(Object var0, List<String> var1) {
      ArrayList<java.lang.String> var2 = new ArrayList();
      if (var0 instanceof Map var3) {
         for (String var5 : var1) {
            if (var3.get(var5) != null) {
               var2.add(var5);
            }
         }
      }

      return var2;
   }

   private static List<String> names(Map<String, Object> var0, String var1) {
      if (raw(var0, "properties") instanceof Collection var3 && !var3.isEmpty()) {
         ArrayList<java.lang.String> var4 = new ArrayList(var3.size());
         var3.forEach(var1x -> var4.add(String.valueOf(var1x)));
         return var4;
      } else {
         throw new LintException(var1 + " needs a non-empty {properties} list");
      }
   }

   static Object schema(Object var0, Map<String, Object> var1) {
      Object var2 = raw(var1, "schema");
      if (var2 == null) {
         throw new LintException("fn.schema needs a {schema}");
      } else {
         String var3 = str(var1, "dialect");
         io.karatelabs.ext.jsonschema.validation.SchemaDialect var4 = null;
         if (var3 != null) {
            var4 = io.karatelabs.ext.jsonschema.validation.SchemaDialect.fromId(var3);
            if (var4 == null) {
               throw new LintException("fn.schema: unknown dialect '" + var3 + "'");
            }
         }

         io.karatelabs.ext.jsonschema.validation.ValidationResult var5 = io.karatelabs.ext.jsonschema.validation.CompiledSchema.compile(var2, var4).withFormatAssertion(isTruthy(raw(var1, "assertFormat"))).validate(var0);
         if (var5.valid) {
            return null;
         } else {
            ArrayList<java.util.Map<java.lang.String,java.lang.Object>> var6 = new ArrayList();

            for (io.karatelabs.ext.jsonschema.validation.ValidationError var8 : var5.errors) {
               var6.add(finding(var8.toString(), pointerSegments(var8.instancePath)));
            }

            return var6;
         }
      }
   }

   Object unreferenced(Object var1, Map<String, Object> var2) {
      if (!(var1 instanceof Map var3)) {
         return null;
      } else {
         String var4 = str(var2, "reusableObjectsLocation");
         if (var4 == null) {
            throw new LintException("fn.unreferencedReusableObject needs {reusableObjectsLocation}, e.g. '#/components/schemas'");
         } else {
            String var5 = var4.endsWith("/") ? var4 : var4 + "/";
            ArrayList var6 = new ArrayList();
            collectRefs(this.document.get(), var6);
            ArrayList<java.util.Map<java.lang.String,java.lang.Object>> var7 = new ArrayList();

            for (Object var9 : var3.keySet()) {
               String var10 = String.valueOf(var9);
               String var11 = var5 + var10.replace("~", "~0").replace("/", "~1");
               boolean var12 = false;

               for (String var14 : (Iterable<String>) (Iterable<?>) (var6)) {
                  if (var14.equals(var11) || var14.startsWith(var11 + "/")) {
                     var12 = true;
                     break;
                  }
               }

               if (!var12) {
                  var7.add(finding("'" + var10 + "' is declared under " + var4 + " and nothing references it", List.of(var10)));
               }
            }

            return var7.isEmpty() ? null : var7;
         }
      }
   }

   private static void collectRefs(Object var0, List<String> var1) {
      if (var0 instanceof Map var2) {
         if (var2.get("$ref") instanceof String var5) {
            var1.add(var5);
         }

         var2.values().forEach(var1x -> collectRefs(var1x, var1));
      } else if (var0 instanceof Collection var3) {
         var3.forEach(var1x -> collectRefs(var1x, var1));
      }
   }

   static Map<String, Object> finding(String var0, List<Object> var1) {
      LinkedHashMap<java.lang.String, java.lang.Object> var2 = new LinkedHashMap();
      var2.put("message", var0);
      var2.put("path", var1);
      return var2;
   }

   private static List<Object> pointerSegments(String var0) {
      ArrayList<java.lang.Object> var1 = new ArrayList();
      if (var0 != null && !var0.isEmpty() && !"/".equals(var0)) {
         for (String var5 : var0.substring(1).split("/")) {
            var1.add(var5.replace("~1", "/").replace("~0", "~"));
         }

         return var1;
      } else {
         return var1;
      }
   }

   private static Object arg(Object[] var0, int var1) {
      return var0 != null && var0.length > var1 ? var0[var1] : null;
   }

   private static Map<String, Object> opts(Object[] var0, int var1) {
      Object var2 = arg(var0, var1);
      if (var2 == null) {
         return Map.of();
      } else if (var2 instanceof Map var3) {
         return var3;
      } else {
         throw new LintException("an fn.* helper's second argument must be an options object, got " + var2.getClass().getSimpleName());
      }
   }

   private static Object raw(Map<String, Object> var0, String var1) {
      Object var2 = var0.get(var1);
      return var2 == Terms.UNDEFINED ? null : var2;
   }

   private static String str(Map<String, Object> var0, String var1) {
      Object var2 = raw(var0, var1);
      return var2 == null ? null : String.valueOf(var2);
   }

   private static Integer num(Map<String, Object> var0, String var1) {
      Object var2 = raw(var0, var1);
      if (var2 == null) {
         return null;
      } else if (var2 instanceof Number var3) {
         return var3.intValue();
      } else {
         throw new LintException("fn option '" + var1 + "' must be a number, got '" + var2 + "'");
      }
   }

   private static boolean isTruthy(Object var0) {
      if (var0 == null || Boolean.FALSE.equals(var0)) {
         return false;
      } else if (var0 instanceof CharSequence var2) {
         return var2.length() > 0;
      } else {
         return !(var0 instanceof Number var1) ? true : var1.doubleValue() != 0.0 && !Double.isNaN(var1.doubleValue());
      }
   }

   private static String show(Object var0) {
      if (var0 == null) {
         return "undefined";
      } else {
         String var1 = String.valueOf(var0);
         return var1.length() > 60 ? "'" + var1.substring(0, 57) + "…'" : (var0 instanceof CharSequence ? "'" + var1 + "'" : var1);
      }
   }

   static {
      CASES.put("flat", new String[]{"[a-z][a-z0-9]*", "[a-z]+"});
      CASES.put("camel", new String[]{"[a-z][a-z0-9]*(?:[A-Z][a-z0-9]*)*", "[a-z]+(?:[A-Z][a-z]*)*"});
      CASES.put("pascal", new String[]{"[A-Z][a-z0-9]*(?:[A-Z][a-z0-9]*)*", "[A-Z][a-z]*(?:[A-Z][a-z]*)*"});
      CASES.put("kebab", new String[]{"[a-z][a-z0-9]*(?:-[a-z0-9]+)*", "[a-z]+(?:-[a-z]+)*"});
      CASES.put("cobol", new String[]{"[A-Z][A-Z0-9]*(?:-[A-Z0-9]+)*", "[A-Z]+(?:-[A-Z]+)*"});
      CASES.put("snake", new String[]{"[a-z][a-z0-9]*(?:_[a-z0-9]+)*", "[a-z]+(?:_[a-z]+)*"});
      CASES.put("macro", new String[]{"[A-Z][A-Z0-9]*(?:_[A-Z0-9]+)*", "[A-Z]+(?:_[A-Z]+)*"});
   }
}
