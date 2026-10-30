package io.karatelabs.ext.pair;

import io.karatelabs.ext.openapi.OpenApiResponse;
import io.karatelabs.ext.openapi.OpenApiResponseHeader;
import java.util.LinkedHashSet;
import java.util.Set;

public interface ContractSchema {
   ContractSchema EMPTY = new ContractSchema() {
      @Override
      public String operationLabel(String var1, String var2, String var3) {
         return var1 + " " + var3;
      }

      @Override
      public Set<String> responseHeaderNames(String var1, String var2, int var3) {
         return Set.of();
      }

      @Override
      public int operationCount() {
         return 0;
      }
   };

   String operationLabel(String var1, String var2, String var3);

   Set<String> responseHeaderNames(String var1, String var2, int var3);

   int operationCount();

   static ContractSchema fromCatalog(final io.karatelabs.ext.openapi.OpenApiOperationCatalog var0) {
      return new ContractSchema() {
         @Override
         public String operationLabel(String var1, String var2, String var3) {
            io.karatelabs.ext.openapi.OpenApiOperationCatalog.OperationInfo var4 = var0.findOperation(var1, var2);
            if (var4 == null) {
               return var1 + " " + var3;
            } else {
               return var4.a() != null && !var4.a().isBlank() ? var4.a() : var1 + " " + var4.c();
            }
         }

         @Override
         public Set<String> responseHeaderNames(String var1, String var2, int var3) {
            io.karatelabs.ext.openapi.OpenApiOperationCatalog.OperationInfo var4 = var0.findOperation(var1, var2);
            if (var4 == null) {
               return Set.of();
            } else {
               for (int var5 = 0; var5 < 3; var5++) {
                  LinkedHashSet<java.lang.String> var6 = new LinkedHashSet();
                  boolean var7 = false;

                  for (io.karatelabs.ext.openapi.OpenApiPathItem var9 : var0.document().paths.values()) {
                     if (var9.c.equals(var4.c())) {
                        for (io.karatelabs.ext.openapi.OpenApiOperation var11 : var9.e) {
                           if (var11.b.equalsIgnoreCase(var4.b())) {
                              for (OpenApiResponse var13 : var11.d) {
                                 if (io.karatelabs.ext.pair.ContractSchema.statusCodeMatches(var13.b, var3, var5)) {
                                    var7 = true;

                                    for (OpenApiResponseHeader var15 : var13.g) {
                                       var6.add(var15.b.toLowerCase());
                                    }
                                 }
                              }
                           }
                        }
                     }
                  }

                  if (var7) {
                     return var6;
                  }
               }

               return Set.of();
            }
         }

         @Override
         public int operationCount() {
            return var0.operations().size();
         }
      };
   }

   private static boolean statusCodeMatches(String var0, int var1, int var2) {
      if (var0 == null) {
         return false;
      } else {
         String var3 = var0.trim();
         boolean var4 = "default".equalsIgnoreCase(var3);
         boolean var5 = var3.length() == 3 && Character.isDigit(var3.charAt(0)) && (var3.charAt(1) == 'X' || var3.charAt(1) == 'x');

         return switch (var2) {
            case 0 -> !var4 && !var5 && statusCodeEquals(var3, var1);
            case 1 -> var5 && var3.charAt(0) - '0' == var1 / 100;
            default -> var4;
         };
      }
   }

   private static boolean statusCodeEquals(String var0, int var1) {
      try {
         return Integer.parseInt(var0) == var1;
      } catch (NumberFormatException var3) {
         return false;
      }
   }
}
