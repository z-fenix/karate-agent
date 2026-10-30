package io.karatelabs.ext.openapi;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.regex.Pattern;

public final class JsonSchemaInferrer {
   private static final Pattern a = Pattern.compile("\\d{4}-\\d{2}-\\d{2}[Tt]\\d{2}:\\d{2}:\\d{2}(\\.\\d+)?([Zz]|[+-]\\d{2}:?\\d{2})?");
   private static final Pattern b = Pattern.compile("[a-zA-Z][a-zA-Z0-9+.-]*://\\S+");

   private JsonSchemaInferrer() {
   }

   public static Map<String, Object> a(Object var0) {
      return b(Collections.singletonList(var0));
   }

   public static Map<String, Object> a(List<Object> var0) {
      return b(var0 == null ? List.of() : var0);
   }

   private static Map<String, Object> b(List<Object> var0) {
      boolean var1 = false;
      ArrayList<java.lang.Object> var2 = new ArrayList();

      for (Object var4 : var0) {
         if (var4 == null) {
            var1 = true;
         } else {
            var2.add(var4);
         }
      }

      if (var2.isEmpty()) {
         return a("null");
      } else {
         LinkedHashSet<java.lang.String> var6 = new LinkedHashSet();

         for (Object var5 : var2) {
            var6.add(b(var5));
         }

         if (var6.contains("number")) {
            var6.remove("integer");
         }

         Map var8 = var6.size() == 1 ? a((String)var6.iterator().next(), var2) : a(var6, var2);
         return var1 ? a(var8) : var8;
      }
   }

   private static Map<String, Object> a(String var0, List<Object> var1) {
      return (Map<String, Object>)(switch (var0) {
         case "object" -> c(var1);
         case "array" -> d(var1);
         case "string" -> e(var1);
         case "integer", "number", "boolean", "null" -> a(var0);
         default -> new LinkedHashMap();
      });
   }

   private static Map<String, Object> c(List<Object> var0) {
      LinkedHashMap<java.lang.String, java.util.ArrayList> var1 = new LinkedHashMap();

      for (Object var3 : var0) {
         for (Entry var5 : (Iterable<Entry>) (Iterable<?>) (((Map)var3).entrySet())) {
            var1.computeIfAbsent((String)var5.getKey(), var0x -> new ArrayList<>()).add(var5.getValue());
         }
      }

      LinkedHashMap<java.lang.String, java.util.Map<java.lang.String,java.lang.Object>> var8 = new LinkedHashMap();
      ArrayList<java.lang.String> var9 = new ArrayList();

      for (Entry var12 : (Iterable<Entry>) (Iterable<?>) (var1.entrySet())) {
         var8.put((String)var12.getKey(), b((List<Object>)var12.getValue()));
         boolean var6 = ((List)var12.getValue()).size() == var0.size();
         boolean var7 = ((List)var12.getValue()).contains(null);
         if (var6 && !var7) {
            var9.add((String)var12.getKey());
         }
      }

      var var11 = a("object");
      var11.put("properties", var8);
      if (!var9.isEmpty()) {
         var11.put("required", var9);
      }

      return var11;
   }

   private static Map<String, Object> d(List<Object> var0) {
      ArrayList var1 = new ArrayList();

      for (Object var3 : var0) {
         var1.addAll((List)var3);
      }

      var var4 = a("array");
      if (!var1.isEmpty()) {
         var4.put("items", b(var1));
      }

      return var4;
   }

   private static Map<String, Object> e(List<Object> var0) {
      var var1 = a("string");
      String var2 = f(var0);
      if (var2 != null) {
         var1.put("format", var2);
      }

      return var1;
   }

   private static String f(List<Object> var0) {
      String[] var1 = new String[]{"uuid", "date-time", "date", "email", "uri"};

      for (String var5 : var1) {
         boolean var6 = true;
         var var7 = var0.iterator();

         while (var7.hasNext()) {
            if (!(var7.next() instanceof String var9) || !a(var5, var9)) {
               var6 = false;
               break;
            }
         }

         if (var6) {
            return var5;
         }
      }

      return null;
   }

   private static boolean a(String var0, String var1) {
      return switch (var0) {
         case "uuid", "date", "email" -> io.karatelabs.ext.jsonschema.validation.FormatValidator.isValid(var0, var1);
         case "date-time" -> a.matcher(var1).matches();
         case "uri" -> b.matcher(var1).matches();
         default -> false;
      };
   }

   private static Map<String, Object> a(Set<String> var0, List<Object> var1) {
      ArrayList<java.util.Map> var2 = new ArrayList();

      for (String var4 : var0) {
         ArrayList<java.lang.Object> var5 = new ArrayList();

         for (Object var7 : var1) {
            String var8 = b(var7);
            if (var4.equals(var8) || "number".equals(var4) && "integer".equals(var8)) {
               var5.add(var7);
            }
         }

         var2.add(a(var4, var5));
      }

      LinkedHashMap<java.lang.String, java.lang.Object> var9 = new LinkedHashMap();
      var9.put("anyOf", var2);
      return var9;
   }

   private static Map<String, Object> a(Map<String, Object> var0) {
      Object var1 = var0.get("type");
      if (var1 instanceof String var2) {
         if ("null".equals(var2)) {
            return var0;
         }

         var0.put("type", List.of(var2, "null"));
      } else if (var1 instanceof List var3) {
         if (!var3.contains("null")) {
            ArrayList<java.lang.String> var4 = new ArrayList(var3);
            var4.add("null");
            var0.put("type", var4);
         }
      } else if (var0.containsKey("anyOf")) {
         ((List)var0.get("anyOf")).add(a("null"));
      } else {
         var0.put("type", "null");
      }

      return var0;
   }

   private static Map<String, Object> a(String var0) {
      LinkedHashMap<java.lang.String, java.lang.Object> var1 = new LinkedHashMap();
      var1.put("type", var0);
      return var1;
   }

   static String b(Object var0) {
      if (var0 == null) {
         return "null";
      } else if (var0 instanceof Map) {
         return "object";
      } else if (var0 instanceof List) {
         return "array";
      } else if (var0 instanceof String) {
         return "string";
      } else if (var0 instanceof Boolean) {
         return "boolean";
      } else if (var0 instanceof Number var1) {
         return a(var1) ? "integer" : "number";
      } else {
         return "string";
      }
   }

   private static boolean a(Number var0) {
      if (var0 instanceof Integer || var0 instanceof Long || var0 instanceof Short || var0 instanceof Byte || var0 instanceof BigInteger) {
         return true;
      } else if (var0 instanceof BigDecimal var3) {
         return var3.stripTrailingZeros().scale() <= 0;
      } else {
         double var1 = var0.doubleValue();
         return var1 == Math.rint(var1) && !Double.isInfinite(var1);
      }
   }
}
