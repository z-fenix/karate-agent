package io.karatelabs.ext.rules;

import java.util.List;

final class ResultTreeMarkdownRenderer {
   private ResultTreeMarkdownRenderer() {
   }

   static String a(List<ResultTreeNode> var0) {
      StringBuilder var1 = new StringBuilder();

      for (ResultTreeNode var3 : var0) {
         a(var3, var1, 0);
      }

      return var1.toString().strip();
   }

   private static void a(ResultTreeNode var0, StringBuilder var1, int var2) {
      int var3 = var2;
      String var4 = var0.e();
      if (var4 != null && !var4.isBlank()) {
         if (var0.c() > 0) {
            var1.append("#".repeat(Math.min(var0.c(), 6))).append(' ').append(var4).append("\n\n");
            var3 = 0;
         } else {
            var1.append("  ".repeat(var2)).append("- ").append(var4).append('\n');
            var3 = var2 + 1;
         }
      }

      for (ResultTreeNode var6 : var0.k()) {
         a(var6, var1, var3);
      }
   }
}
