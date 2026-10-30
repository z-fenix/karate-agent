package io.karatelabs.ext.rules.analysis;

public class BoxSplitSelector {
   public io.karatelabs.ext.rules.analysis.BoxSplitSelector.BoxSplitPoint a(AbstractExecutionState var1) {
      io.karatelabs.ext.rules.analysis.BoxSplitSelector.BoxSplitPoint var2 = b(var1);
      if (var2 != null) {
         return var2;
      } else {
         int var3 = a(var1, var1.g());
         return var3 < 0 ? null : new io.karatelabs.ext.rules.analysis.BoxSplitSelector.BoxSplitPoint(var3, var1.a(var3) + (var1.c(var3) - 1L) / 2L + 1L);
      }
   }

   private static int a(AbstractExecutionState var0, long var1) {
      int var3 = -1;
      long var4 = 1L;

      for (int var6 = 0; var6 < var0.a(); var6++) {
         if ((var1 & 1L << var6) != 0L) {
            long var7 = var0.c(var6);
            if (var7 > var4) {
               var4 = var7;
               var3 = var6;
            }
         }
      }

      return var3;
   }

   private static io.karatelabs.ext.rules.analysis.BoxSplitSelector.BoxSplitPoint b(AbstractExecutionState var0) {
      if (var0.d() == null) {
         return null;
      } else {
         int var1 = a(var0, var0.e());
         io.karatelabs.ext.rules.analysis.AbstractValue var2 = var0.f();
         boolean var3 = false;
         if (var1 < 0) {
            var1 = a(var0, var0.f());
            var2 = var0.e();
            var3 = true;
         }

         if (var1 < 0) {
            return null;
         } else {
            long var4 = var0.a(var1);
            long var6 = var0.b(var1);
            if (var4 == var6) {
               return null;
            } else {
               long var8;
               if (var0.b().get(var1).c() == AbstractBox.DimensionKind.INT) {
                  if (!(var2 instanceof io.karatelabs.ext.rules.analysis.AbstractValue.NumericIntervalAbstractValue var10) || !var10.b().a()) {
                     return null;
                  }

                  double var11 = var10.b().g();
                  String var13 = var3 ? a(var0.d()) : var0.d();
                  switch (var13) {
                     case "<":
                     case ">=":
                        var8 = (long)Math.ceil(var11);
                        break;
                     case "<=":
                     case ">":
                        var8 = (long)Math.floor(var11) + 1L;
                        break;
                     case "===":
                     case "!==":
                     case "==":
                     case "!=":
                        if (var11 != Math.rint(var11)) {
                           return null;
                        }

                        var8 = (long)var11 == var4 ? var4 + 1L : (long)var11;
                        break;
                     default:
                        return null;
                  }
               } else {
                  if (!(var2 instanceof io.karatelabs.ext.rules.analysis.AbstractValue.ScalarSetAbstractValue var15) || var15.d().size() != 1) {
                     return null;
                  }

                  int var16 = var0.b().get(var1).f().indexOf(var15.d().get(0));
                  if (var16 < var4 || var16 > var6) {
                     return null;
                  }

                  var8 = var16 == var4 ? var4 + 1L : var16;
               }

               return var8 > var4 && var8 <= var6 ? new io.karatelabs.ext.rules.analysis.BoxSplitSelector.BoxSplitPoint(var1, var8) : null;
            }
         }
      }
   }

   private static int a(AbstractExecutionState var0, io.karatelabs.ext.rules.analysis.AbstractValue var1) {
      if (var1 == null) {
         return -1;
      } else {
         long var2 = var1.a();
         if (var2 != 0L && (var2 & var2 - 1L) == 0L) {
            int var4 = Long.numberOfTrailingZeros(var2);
            if (var4 >= var0.a()) {
               return -1;
            } else {
               io.karatelabs.ext.rules.analysis.AbstractExecutionState.ExecutionDimension var5 = var0.b().get(var4);
               io.karatelabs.ext.rules.analysis.AbstractValue var6 = io.karatelabs.ext.rules.analysis.AbstractBox.a(var5.c(), var5.f(), var4, var5.d(), var5.e());
               if (var1 instanceof io.karatelabs.ext.rules.analysis.AbstractValue.NumericIntervalAbstractValue var7 && var6 instanceof io.karatelabs.ext.rules.analysis.AbstractValue.NumericIntervalAbstractValue var10) {
                  return var7.b().g() == var10.b().g() && var7.b().h() == var10.b().h() ? var4 : -1;
               } else if (var1 instanceof io.karatelabs.ext.rules.analysis.AbstractValue.ScalarSetAbstractValue var9 && var6 instanceof io.karatelabs.ext.rules.analysis.AbstractValue.ScalarSetAbstractValue var8) {
                  return var9.d().equals(var8.d()) ? var4 : -1;
               } else {
                  return -1;
               }
            }
         } else {
            return -1;
         }
      }
   }

   private static String a(String var0) {
      return switch (var0) {
         case "<" -> ">";
         case "<=" -> ">=";
         case ">" -> "<";
         case ">=" -> "<=";
         default -> var0;
      };
   }

   public record BoxSplitPoint(int a, long b) {

      public BoxSplitPoint(int a, long b) {
         this.a = a;
         this.b = b;
      }
   }
}
