package io.karatelabs.ext.requirements.jsapi;

import java.util.List;
import java.util.Map;

final class RequirementCsvRenderer {
   private static final String a = "Requirement,Title,Type,Status,Criticality,Posture,Tests,Acceptance";
   private static final String b = "=+-@\t\r";

   private RequirementCsvRenderer() {
   }

   static String a(List<Map<String, Object>> var0) {
      StringBuilder var1 = new StringBuilder("Requirement,Title,Type,Status,Criticality,Posture,Tests,Acceptance").append("\r\n");

      for (Map var3 : var0) {
         var1.append(a(var3.get("id")))
            .append(',')
            .append(a(var3.get("title")))
            .append(',')
            .append(a(var3.get("type")))
            .append(',')
            .append(a(var3.get("status")))
            .append(',')
            .append(a(var3.get("criticality")))
            .append(',')
            .append(a(var3.get("posture")))
            .append(',')
            .append(a(b(var3.get("tests"))))
            .append(',')
            .append(a(c(var3.get("criteria"))))
            .append("\r\n");
      }

      return var1.toString();
   }

   private static String b(Object var0) {
      if (var0 instanceof List var1) {
         StringBuilder var2 = new StringBuilder();

         for (Object var4 : var1) {
            var2.append(var2.isEmpty() ? "" : " | ").append(var4);
         }

         return var2.toString();
      } else {
         return "";
      }
   }

   private static String c(Object var0) {
      if (var0 instanceof List var1 && !var1.isEmpty()) {
         long var2 = var1.stream().filter(var0x -> var0x instanceof Map var1x && "COVERED".equals(var1x.get("status"))).count();
         return var2 + "/" + var1.size();
      } else {
         return "";
      }
   }

   static String a(Object var0) {
      String var1 = var0 == null ? "" : String.valueOf(var0);
      StringBuilder var2 = new StringBuilder(var1.length() + 2);
      boolean var3 = true;

      for (int var4 = 0; var4 < var1.length(); var4++) {
         char var5 = var1.charAt(var4);
         if (var3 && "=+-@\t\r".indexOf(var5) >= 0) {
            var2.append('\'');
         }

         var2.append(var5);
         var3 = var5 == ';' || var5 == '\t' || var5 == '\r' || var5 == '\n' || var3 && (var5 == ' ' || var5 == '"');
      }

      var1 = var2.toString();
      if (var1.indexOf(44) >= 0 || var1.indexOf(34) >= 0 || var1.indexOf(13) >= 0 || var1.indexOf(10) >= 0 || var1.indexOf(59) >= 0) {
         var1 = "\"" + var1.replace("\"", "\"\"") + "\"";
      }

      return var1;
   }
}
