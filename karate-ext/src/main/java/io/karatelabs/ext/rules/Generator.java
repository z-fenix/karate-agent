package io.karatelabs.ext.rules;

import io.karatelabs.js.Engine;
import io.karatelabs.js.JavaCallable;
import io.karatelabs.js.SimpleObject;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class Generator {
   static final String a = "KARATE_RULES_GENERATOR_TIMEOUT_MS";
   private static final io.karatelabs.ext.common.SandboxExecutor.SandboxFailureMapper b = new io.karatelabs.ext.common.SandboxExecutor.SandboxFailureMapper() {
      @Override
      public RuntimeException a(long var1) {
         return new RuleApiException(
            408,
            "GENERATOR_TIMEOUT",
            "generator.js: generate(g) exceeded " + var1 + "ms — it declares the input domain and must return promptly; check for a loop that never ends"
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
   };

   private Generator() {
   }

   private static RuleApiException b(String var0) {
      return new RuleApiException(400, "GENERATOR_INVALID", var0);
   }

   public static io.karatelabs.ext.rules.Generator.GeneratorPlan a(String var0) {
      return a(var0, io.karatelabs.ext.common.SandboxExecutor.a("KARATE_RULES_GENERATOR_TIMEOUT_MS", 5000L));
   }

   static io.karatelabs.ext.rules.Generator.GeneratorPlan a(String var0, long var1) {
      io.karatelabs.ext.rules.Generator.GeneratorPlan var4;
      try (io.karatelabs.ext.common.SandboxExecutor.SandboxExecutionScope var3 = new io.karatelabs.ext.common.SandboxExecutor.SandboxExecutionScope("rule-generator", var1)) {
         var4 = var3.a(() -> c(var0), b);
      }

      return var4;
   }

   private static io.karatelabs.ext.rules.Generator.GeneratorPlan c(String var0) {
      Engine var1 = new Engine();
      var1.eval(var0);
      if (var1.get("generate") instanceof JavaCallable var3) {
         ArrayList var4 = new ArrayList();
         LinkedHashMap var5 = new LinkedHashMap();
         Generator.Builder var6 = new Generator.Builder("", var5, var4);
         var3.call(null, var6);
         return new io.karatelabs.ext.rules.Generator.GeneratorPlan(var4, var5);
      } else {
         throw b("generator.js does not define a 'generate' function");
      }
   }

   static final class Builder implements SimpleObject {
      private final String prefix;
      private final Map<String, Object> base;
      private final List<io.karatelabs.ext.rules.Generator.GeneratorDimension> dims;

      Builder(String var1, Map<String, Object> var2, List<io.karatelabs.ext.rules.Generator.GeneratorDimension> var3) {
         this.prefix = var1;
         this.base = var2;
         this.dims = var3;
      }

      private String path(String var1) {
         return this.prefix.isEmpty() ? var1 : this.prefix + "." + var1;
      }

      @Override
      public Object jsGet(String var1) {
         return switch (var1) {
            case "enum", "pick" -> (io.karatelabs.js.JavaCallable) (var1x, var2) -> this.enumDim(str(var2, 0), list(var2, 1));
            case "int" -> (io.karatelabs.js.JavaCallable) (var1x, var2) -> this.numberDim(str(var2, 0), var2, true);
            case "number", "num" -> (io.karatelabs.js.JavaCallable) (var1x, var2) -> this.numberDim(str(var2, 0), var2, false);
            case "bool", "boolean" -> (io.karatelabs.js.JavaCallable) (var1x, var2) -> this.boolDim(str(var2, 0));
            case "const", "fixed" -> (io.karatelabs.js.JavaCallable) (var1x, var2) -> this.constDim(str(var2, 0), arg(var2, 1));
            case "string", "text" -> (io.karatelabs.js.JavaCallable) (var1x, var2) -> this.stringDim(str(var2, 0), list(var2, 1));
            case "object", "group" -> (io.karatelabs.js.JavaCallable) (var1x, var2) -> this.objectDim(str(var2, 0), arg(var2, 1));
            case "array", "list" -> (io.karatelabs.js.JavaCallable) (var1x, var2) -> this.arrayDim(str(var2, 0), var2);
            default -> null;
         };
      }

      private Object enumDim(String var1, List<Object> var2) {
         if (var1 == null) {
            return null;
         } else {
            List var3;
            try {
               var3 = io.karatelabs.ext.rules.NumericBoundsParser.a(var2);
            } catch (io.karatelabs.ext.rules.NumericBoundsParser.NumericBoundsException var5) {
               throw Generator.b("generator.js: g.enum('" + var1 + "') needs a non-empty values array — an empty enum would erase the axis");
            }

            this.add(new io.karatelabs.ext.rules.Generator.GeneratorDimension(this.path(var1), "enum", var3, null, null, null, false));
            this.base.put(var1, var3.get(0));
            return null;
         }
      }

      private Object numberDim(String var1, Object[] var2, boolean var3) {
         if (var1 == null) {
            return null;
         } else {
            String var4 = var3 ? "g.int" : "g.number";
            Double var5 = bound(var4, var1, "lo", arg(var2, 1), var3);
            Double var6 = bound(var4, var1, "hi", arg(var2, 2), var3);
            if (var5 != null && var6 != null && var5 > var6) {
               throw Generator.b(
                  "generator.js: " + var4 + "('" + var1 + "', " + render(var5) + ", " + render(var6) + ") — lo is above hi, so the declared domain is empty"
               );
            } else {
               var var7 = list(var2, 3);
               if (var7 != null) {
                  for (Object var9 : var7) {
                     Double var10 = num(var9);
                     if (var10 != null && (var5 != null && var10 < var5 || var6 != null && var10 > var6)) {
                        throw Generator.b(
                           "generator.js: "
                              + var4
                              + "('"
                              + var1
                              + "') — the boundary "
                              + render(var10)
                              + " lies outside the declared domain "
                              + (var5 == null ? "" : render(var5))
                              + ".."
                              + (var6 == null ? "" : render(var6))
                        );
                     }
                  }
               }

               this.add(new io.karatelabs.ext.rules.Generator.GeneratorDimension(this.path(var1), "number", var7 == null ? List.of() : var7, var5, var6, null, var3));
               this.base.put(var1, var5 != null ? render(var5) : 0);
               return null;
            }
         }
      }

      private void add(io.karatelabs.ext.rules.Generator.GeneratorDimension var1) {
         for (io.karatelabs.ext.rules.Generator.GeneratorDimension var3 : this.dims) {
            if (var3.a().equals(var1.a())) {
               throw Generator.b("generator.js: '" + var1.a() + "' is declared twice — one axis, one declared domain");
            }
         }

         this.dims.add(var1);
      }

      private static Double bound(String var0, String var1, String var2, Object var3, boolean var4) {
         try {
            return io.karatelabs.ext.rules.NumericBoundsParser.a(var3, var4);
         } catch (io.karatelabs.ext.rules.NumericBoundsParser.NumericBoundsException var8) {
            String var6 = var8.a;

            throw Generator.b("generator.js: " + var0 + "('" + var1 + "') — the " + var2 + " bound " + switch (var6) {
               case "notNumber" -> "must be a number, got " + var8.b;
               case "notFinite" -> "must be finite, got " + var8.b;
               default -> "must be a whole number, got " + var8.b + "; use g.number for a real-valued axis";
            });
         }
      }

      private Object boolDim(String var1) {
         if (var1 == null) {
            return null;
         } else {
            this.add(new io.karatelabs.ext.rules.Generator.GeneratorDimension(this.path(var1), "boolean", List.of(false, true), null, null, null, false));
            this.base.put(var1, false);
            return null;
         }
      }

      private Object constDim(String var1, Object var2) {
         if (var1 != null) {
            this.base.put(var1, var2);
         }

         return null;
      }

      private Object stringDim(String var1, List<Object> var2) {
         if (var1 == null) {
            return null;
         } else {
            List<Object> var3 = var2 != null && !var2.isEmpty() ? new ArrayList<>(var2) : List.of("x");
            this.add(new io.karatelabs.ext.rules.Generator.GeneratorDimension(this.path(var1), "string", (List<Object>)var3, null, null, null, false));
            this.base.put(var1, var3.get(0));
            return null;
         }
      }

      private Object objectDim(String var1, Object var2) {
         if (var1 != null && var2 instanceof JavaCallable var3) {
            LinkedHashMap var4 = new LinkedHashMap();
            this.base.put(var1, var4);
            var3.call(null, new Generator.Builder(this.path(var1), var4, this.dims));
            return null;
         } else {
            return null;
         }
      }

      private Object arrayDim(String var1, Object[] var2) {
         if (var1 == null) {
            return null;
         } else {
            int var3 = cardinality(var1, "min", arg(var2, 1), 0);
            int var4 = cardinality(var1, "max", arg(var2, 2), Math.max(var3, 1));
            if (var3 > var4) {
               throw Generator.b(
                  "generator.js: g.array('"
                     + var1
                     + "', "
                     + var3
                     + ", "
                     + var4
                     + ") — the minimum cardinality is above the maximum, so no array length is legal"
               );
            } else {
               Object var5 = arg(var2, 3);
               String var6 = this.path(var1);
               this.add(new io.karatelabs.ext.rules.Generator.GeneratorDimension(var6 + "#", "cardinality", List.of(var3, var4), null, null, var6, false));
               LinkedHashMap var7 = new LinkedHashMap();
               if (var5 instanceof JavaCallable var8) {
                  var8.call(null, new Generator.Builder(var6 + "[]", var7, this.dims));
               }

               int var11 = Math.min(Math.max(1, var3), Math.max(var3, var4));
               ArrayList<java.lang.Object> var9 = new ArrayList();

               for (int var10 = 0; var10 < var11; var10++) {
                  var9.add(io.karatelabs.ext.common.SandboxExecutor.a(var7));
               }

               this.base.put(var1, var9);
               return null;
            }
         }
      }

      private static int cardinality(String var0, String var1, Object var2, int var3) {
         try {
            return io.karatelabs.ext.rules.NumericBoundsParser.a(var2, var3);
         } catch (io.karatelabs.ext.rules.NumericBoundsParser.NumericBoundsException var7) {
            String var5 = var7.a;

            throw Generator.b("generator.js: g.array('" + var0 + "') — the " + var1 + " cardinality " + switch (var5) {
               case "notFinite" -> "must be a finite number, got " + var7.b;
               case "notWhole" -> "must be a whole number, got " + var7.b;
               default -> "cannot be negative, got " + var7.b;
            });
         }
      }

      private static Object arg(Object[] var0, int var1) {
         return var0 != null && var1 < var0.length ? var0[var1] : null;
      }

      private static String str(Object[] var0, int var1) {
         Object var2 = arg(var0, var1);
         return var2 == null ? null : String.valueOf(var2);
      }

      private static List<Object> list(Object[] var0, int var1) {
         Object var2 = arg(var0, var1);
         return var2 instanceof List ? (List)var2 : null;
      }

      private static Double num(Object var0) {
         return var0 instanceof Number var1 ? var1.doubleValue() : null;
      }

      private static Object render(double var0) {
         return var0 == Math.rint(var0) && Math.abs(var0) < 2.147483647E9 ? (int)var0 : var0;
      }
   }

   public record GeneratorDimension(String a, String b, List<Object> c, Double d, Double e, String f, boolean g) {

      public GeneratorDimension(String a, String b, List<Object> c, Double d, Double e, String f, boolean g) {
         this.a = a;
         this.b = b;
         this.c = c;
         this.d = d;
         this.e = e;
         this.f = f;
         this.g = g;
      }
   }

   public record GeneratorPlan(List<io.karatelabs.ext.rules.Generator.GeneratorDimension> a, Map<String, Object> b) {

      public GeneratorPlan(List<io.karatelabs.ext.rules.Generator.GeneratorDimension> a, Map<String, Object> b) {
         this.a = a;
         this.b = b;
      }
   }
}
