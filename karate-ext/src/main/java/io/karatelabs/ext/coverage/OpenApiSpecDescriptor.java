package io.karatelabs.ext.coverage;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public record OpenApiSpecDescriptor(String a, String b, List<String> c) {

   public OpenApiSpecDescriptor(String a, String b, List<String> c) {
      this.a = a;
      this.b = b;
      this.c = c;
   }

   public static List<OpenApiSpecDescriptor> parse(Object var0) {
      ArrayList<io.karatelabs.ext.coverage.OpenApiSpecDescriptor> var1 = new ArrayList();
      if (var0 instanceof Map var2) {
         if (a(var2)) {
            var1.add(new OpenApiSpecDescriptor(null, String.valueOf(var2.get("path")), b(var2)));
         } else {
            var2.forEach(
               (var1x, var2x) -> {
                  if (var2x instanceof Map var3x) {
                     if (!a(var3x)) {
                        throw new IllegalArgumentException(
                           "cov.openapi '" + var1x + "': a map value must be a descriptor { path, basePath } — else give the spec path as a String"
                        );
                     }

                     var1.add(new OpenApiSpecDescriptor(String.valueOf(var1x), String.valueOf(var3x.get("path")), b(var3x)));
                  } else {
                     var1.add(new OpenApiSpecDescriptor(String.valueOf(var1x), String.valueOf(var2x), null));
                  }
               }
            );
         }
      } else if (!(var0 instanceof List var3 ? var3.isEmpty() : var0 == null || String.valueOf(var0).isBlank())) {
         var1.add(new OpenApiSpecDescriptor(null, String.valueOf(var0), null));
      }

      return var1;
   }

   private static boolean a(Map<?, ?> var0) {
      if (var0.get("path") == null) {
         return false;
      } else {
         for (Object var2 : var0.keySet()) {
            String var3 = String.valueOf(var2);
            if (!"path".equals(var3) && !"basePath".equals(var3)) {
               return false;
            }
         }

         return true;
      }
   }

   private static List<String> b(Map<?, ?> var0) {
      if (!var0.containsKey("basePath")) {
         return null;
      } else if (var0.get("basePath") instanceof String var1) {
         return io.karatelabs.ext.openapi.ServerBasePathResolver.a(List.of(var1));
      } else {
         throw new IllegalArgumentException("cov.openapi basePath must be one path prefix String ('' disables prefixing), not: " + var0.get("basePath"));
      }
   }
}
