package io.karatelabs.ext.requirements.jsapi;

import java.util.List;
import java.util.Map;

final class RequirementMatrixRenderer {
   private RequirementMatrixRenderer() {
   }

   static String a(List<Map<String, Object>> var0) {
      StringBuilder var1 = new StringBuilder();
      var1.append("# Requirements traceability matrix\n\n");
      if (var0.isEmpty()) {
         var1.append("_No requirements found — configure `cov.requirements` and run the suite._\n");
         return var1.toString();
      } else {
         var1.append("| Requirement | Title | Type | Status | Tests |\n");
         var1.append("|---|---|---|---|---|\n");

         for (Map var3 : var0) {
            var1.append("| ")
               .append(b(var3.get("id")))
               .append(" | ")
               .append(b(var3.get("title")))
               .append(" | ")
               .append(b(var3.get("type")))
               .append(" | ")
               .append(a(b(var3.get("status")), var3.get("oracleOnly"), var3.get("refusalOnly"), var3.get("modelOnly"), var3.get("notasserted")))
               .append(" | ")
               .append(a(var3.get("tests")))
               .append(" |\n");
         }

         return var1.toString();
      }
   }

   private static String a(String var0, Object var1, Object var2, Object var3, Object var4) {
      String var5 = a(var0);
      if (Boolean.TRUE.equals(var2)) {
         var5 = var5 + " (refusal only)";
      } else if (Boolean.TRUE.equals(var1)) {
         var5 = var5 + " (rules only)";
      } else if (Boolean.TRUE.equals(var3)) {
         var5 = var5 + " (model only)";
      }

      return Boolean.TRUE.equals(var4) ? var5 + " (notasserted)" : var5;
   }

   private static String a(String var0) {
      return switch (var0) {
         case "COVERED" -> "✅ COVERED";
         case "FAILING" -> "❌ FAILING";
         case "NOTRUN" -> "⚠️ NOTRUN";
         case "NOTCOVERED" -> "⬜ NOTCOVERED";
         default -> var0;
      };
   }

   private static String a(Object var0) {
      if (var0 instanceof List var1) {
         return var1.isEmpty() ? "—" : String.valueOf(var1.size());
      } else {
         return "—";
      }
   }

   private static String b(Object var0) {
      return var0 == null ? "" : String.valueOf(var0);
   }
}
