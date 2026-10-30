package io.karatelabs.ext.openapi;

import io.karatelabs.ext.contract.Partitions;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class SchemaPartitionGenerator {
   public static final String a = "spec";

   private SchemaPartitionGenerator() {
   }

   public static Map<String, Object> a(OpenApiSchemaNode var0, String var1) {
      LinkedHashMap var2 = new LinkedHashMap();
      if (var0 != null) {
         a(var0, "", var2, a(var1));
      }

      return var2;
   }

   private static void a(OpenApiSchemaNode var0, String var1, Map<String, Object> var2, LocalDate var3) {
      String var4 = var0.type();
      if ("object".equals(var4)) {
         for (OpenApiSchemaNode var12 : var0.children) {
            String var7 = var12.name();
            if (var7 != null) {
               a(var12, var1.isEmpty() ? var7 : var1 + "." + var7, var2, var3);
            }
         }
      } else if (var4 == null || "oneOf".equals(var4) || "anyOf".equals(var4) || "allOf".equals(var4)) {
         for (OpenApiSchemaNode var11 : var0.children) {
            a(var11, var1, var2, var3);
         }
      } else if ("array".equals(var4)) {
         var var8 = a(var0);
         String var6 = var1.isEmpty() ? "(body)" : var1;
         if (var8 != null) {
            var2.put(var6, var8);
         }

         if (!var0.children.isEmpty()) {
            a(var0.children.getFirst(), var1.isEmpty() ? "[]" : var1 + "[]", var2, var3);
         }
      } else {
         var var5 = a(var0, var3);
         if (var5 != null) {
            var2.put(var1.isEmpty() ? "(body)" : var1, var5);
         }
      }
   }

   static Map<String, Object> a(OpenApiSchemaNode var0, LocalDate var1) {
      SchemaDefinition var2 = var0.schemaDefinition;
      String var3 = var0.type();
      var var4 = var2.enumValues();
      Map var5;
      if (!var4.isEmpty()) {
         var5 = a(var4);
      } else if ("boolean".equals(var3)) {
         var5 = a();
      } else if ("integer".equals(var3) || "number".equals(var3)) {
         var5 = a(var2);
      } else if ("string".equals(var3)) {
         String var6 = var2.format();
         if (!"date".equals(var6) && !"date-time".equals(var6)) {
            var5 = b(var2);
         } else {
            var5 = a(var6, var1);
         }
      } else {
         var5 = null;
      }

      if (var5 == null) {
         return null;
      } else {
         a(var5, var0);
         return var5;
      }
   }

   private static Map<String, Object> a(List<Object> var0) {
      ArrayList var1 = new ArrayList();
      ArrayList var2 = new ArrayList();

      for (Object var4 : var0) {
         var1.add(String.valueOf(var4));
         var2.add(var4);
      }

      return a("enum", var1, var2, null);
   }

   private static Map<String, Object> a() {
      var var0 = List.of("true", "false");
      var var1 = List.of(true, false);
      return a("boolean", new ArrayList<>(var0), new ArrayList<>(var1), null);
   }

   private static Map<String, Object> a(SchemaDefinition var0) {
      ArrayList var1 = new ArrayList();
      if (var0.minimum() != null) {
         var1.add(var0.minimum().doubleValue());
      }

      if (var0.maximum() != null) {
         var1.add(var0.maximum().doubleValue());
      }

      return var1.isEmpty() ? null : a("number", var1, false);
   }

   private static Map<String, Object> a(OpenApiSchemaNode var0) {
      SchemaDefinition var1 = var0.schemaDefinition;
      ArrayList var2 = new ArrayList();
      if (var1.minItems() != null) {
         var2.add((double)var1.minItems().intValue());
      }

      if (var1.maxItems() != null) {
         var2.add((double)var1.maxItems().intValue());
      }

      return var2.isEmpty() ? null : a("cardinality", var2, true);
   }

   private static Map<String, Object> a(String var0, List<Double> var1, boolean var2) {
      return a(Partitions.bva(var0, var1, var2));
   }

   private static Map<String, Object> b(SchemaDefinition var0) {
      return a(Partitions.string(var0.minLength(), var0.maxLength(), var0.format(), var0.pattern()));
   }

   private static Map<String, Object> a(Map<String, Object> var0) {
      if (var0 != null) {
         var0.put("source", "spec");
      }

      return var0;
   }

   private static Map<String, Object> a(String var0, LocalDate var1) {
      var var2 = List.of("past", "today", "future", "leap-day", "period-boundary", "far-future");
      Map var3;
      if (var1 == null) {
         var3 = a("date", new ArrayList<>(var2), null, null);
      } else {
         boolean var4 = "date-time".equals(var0);
         ArrayList var5 = new ArrayList();
         var5.add(a(var1.minusMonths(1L), var4));
         var5.add(a(var1, var4));
         var5.add(a(var1.plusMonths(1L), var4));
         var5.add(a(a(var1), var4));
         var5.add(a(var1.withDayOfMonth(1), var4));
         var5.add(a(var1.plusYears(100L), var4));
         var3 = a("date", new ArrayList<>(var2), var5, null);
      }

      return var3;
   }

   private static LocalDate a(LocalDate var0) {
      for (int var1 = var0.getYear(); var1 < var0.getYear() + 8; var1++) {
         if (LocalDate.ofYearDay(var1, 1).isLeapYear()) {
            return LocalDate.of(var1, 2, 29);
         }
      }

      return LocalDate.of(2024, 2, 29);
   }

   private static String a(LocalDate var0, boolean var1) {
      return var1 ? var0 + "T00:00:00Z" : var0.toString();
   }

   private static void a(Map<String, Object> var0, OpenApiSchemaNode var1) {
      if (var1.schemaDefinition.nullable()) {
         a(var0, "null", null);
      }

      if (var1.parent != null && "object".equals(var1.parent.type()) && !var1.isRequired()) {
         a(var0, "absent", null);
      }
   }

   private static void a(Map<String, Object> var0, String var1, Object var2) {
      List var3 = (List)var0.get("classes");
      if (!var3.contains(var1)) {
         var3.add(var1);
         if (var0.get("samples") instanceof List) {
            ((List)var0.get("samples")).add(var2);
         }
      }
   }

   private static Map<String, Object> a(String var0, List<String> var1, List<Object> var2, List<Object> var3) {
      LinkedHashMap var4 = new LinkedHashMap();
      var4.put("kind", var0);
      var4.put("classes", var1);
      if (var2 != null) {
         var4.put("samples", var2);
      }

      if (var3 != null) {
         var4.put("boundaries", var3);
      }

      var4.put("source", "spec");
      return var4;
   }

   private static LocalDate a(String var0) {
      if (var0 != null && !var0.isBlank()) {
         try {
            return LocalDate.parse(var0.length() > 10 ? var0.substring(0, 10) : var0);
         } catch (DateTimeParseException var2) {
            return null;
         }
      } else {
         return null;
      }
   }
}
