package io.karatelabs.ext.rules;

import io.karatelabs.js.Terms;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

final class FlattenedCsvCodec {
   static final Object a = new Object();

   private FlattenedCsvCodec() {
   }

   static String a(String var0, io.karatelabs.ext.rules.FlattenedCsvCodec.PathSegment var1) {
      if (var1.c()) {
         return var0 + "#";
      } else if (var1.a() == null) {
         return var0 + "[" + var1.b() + "]";
      } else {
         String var2 = var1.a();
         boolean var3 = var2.isEmpty();

         for (int var4 = 0; var4 < var2.length() && !var3; var4++) {
            var3 = ".[]\"#".indexOf(var2.charAt(var4)) >= 0;
         }

         if (var3) {
            return var0 + "[\"" + var2.replace("\\", "\\\\").replace("\"", "\\\"") + "\"]";
         } else {
            return var0.isEmpty() ? var2 : var0 + "." + var2;
         }
      }
   }

   static List<io.karatelabs.ext.rules.FlattenedCsvCodec.PathSegment> a(String var0) {
      ArrayList<io.karatelabs.ext.rules.FlattenedCsvCodec.PathSegment> var1 = new ArrayList();
      int var2 = 0;
      int var3 = var0.length();

      while (var2 < var3) {
         char var4 = var0.charAt(var2);
         if (var4 == '#') {
            if (var2 != var3 - 1 || var1.isEmpty()) {
               throw d(var0);
            }

            var1.add(io.karatelabs.ext.rules.FlattenedCsvCodec.PathSegment.a);
            var2++;
         } else if (var4 == '[') {
            if (var2 + 1 < var3 && var0.charAt(var2 + 1) == '"') {
               StringBuilder var9 = new StringBuilder();

               int var6;
               for (var6 = var2 + 2; var6 < var3 && var0.charAt(var6) != '"'; var6++) {
                  if (var0.charAt(var6) == '\\' && var6 + 1 < var3) {
                     var6++;
                  }

                  var9.append(var0.charAt(var6));
               }

               if (var6 + 1 >= var3 || var0.charAt(var6 + 1) != ']') {
                  throw d(var0);
               }

               var1.add(io.karatelabs.ext.rules.FlattenedCsvCodec.PathSegment.a(var9.toString()));
               var2 = var6 + 2;
            } else {
               int var8 = var0.indexOf(93, var2);
               if (var8 < 0) {
                  throw d(var0);
               }

               try {
                  var1.add(io.karatelabs.ext.rules.FlattenedCsvCodec.PathSegment.a(Integer.parseInt(var0.substring(var2 + 1, var8))));
               } catch (NumberFormatException var7) {
                  throw d(var0);
               }

               var2 = var8 + 1;
            }
         } else {
            if (var4 == '.') {
               if (var1.isEmpty()) {
                  throw d(var0);
               }

               var2++;
            } else if (!var1.isEmpty()) {
               throw d(var0);
            }

            int var5 = var2;

            while (var5 < var3 && ".[]\"#".indexOf(var0.charAt(var5)) < 0) {
               var5++;
            }

            if (var5 == var2) {
               throw d(var0);
            }

            var1.add(io.karatelabs.ext.rules.FlattenedCsvCodec.PathSegment.a(var0.substring(var2, var5)));
            var2 = var5;
         }
      }

      if (var1.isEmpty()) {
         throw d(var0);
      } else {
         return var1;
      }
   }

   private static RuleApiException d(String var0) {
      return new RuleApiException(
         400, "CSV_INVALID", "'" + var0 + "' is not a flattened path (dotted keys, [\"quoted\"] segments, [index], a trailing # for an array length)"
      );
   }

   static String b(String var0) {
      StringBuilder var1 = new StringBuilder();

      for (io.karatelabs.ext.rules.FlattenedCsvCodec.PathSegment var3 : a(var0)) {
         if (var3.c()) {
            var1.append('#');
         } else if (var3.a() == null) {
            var1.append("[]");
         } else {
            String var4 = a("", var3);
            var1.append(!var1.isEmpty() && !var4.startsWith("[") ? "." + var4 : var4);
         }
      }

      return var1.toString();
   }

   static Object a(Object var0, String var1) {
      Object var2 = var0;

      for (io.karatelabs.ext.rules.FlattenedCsvCodec.PathSegment var4 : a(var1)) {
         if (var4.c()) {
            return var2 instanceof List var7 ? var7.size() : null;
         }

         if (var4.a() == null) {
            if (!(var2 instanceof List var6) || var4.b() >= var6.size()) {
               return null;
            }

            var2 = var6.get(var4.b());
         } else {
            if (!(var2 instanceof Map var5)) {
               return null;
            }

            var2 = var5.get(var4.a());
         }
      }

      return var2;
   }

   static Map<String, Object> a(Map<String, Object> var0) {
      LinkedHashMap<java.lang.String, java.lang.Object> var1 = new LinkedHashMap();
      a(var0, "", var1);
      return var1;
   }

   private static void a(Object var0, String var1, Map<String, Object> var2) {
      if (var0 instanceof Map var3) {
         if (var3.isEmpty()) {
            var2.put(var1, a);
            return;
         }

         for (Entry var6 : (Iterable<Entry>) (Iterable<?>) (var3.entrySet())) {
            a(var6.getValue(), a(var1, io.karatelabs.ext.rules.FlattenedCsvCodec.PathSegment.a(String.valueOf(var6.getKey()))), var2);
         }
      } else if (var0 instanceof List var4) {
         var2.put(a(var1, io.karatelabs.ext.rules.FlattenedCsvCodec.PathSegment.a), var4.size());

         for (int var7 = 0; var7 < var4.size(); var7++) {
            a(var4.get(var7), a(var1, io.karatelabs.ext.rules.FlattenedCsvCodec.PathSegment.a(var7)), var2);
         }
      } else {
         var2.put(var1, var0);
      }
   }

   static Map<String, Integer> a(List<Map<String, Object>> var0) {
      LinkedHashMap<java.lang.String, java.lang.Integer> var1 = new LinkedHashMap();

      for (Map var3 : var0) {
         for (Entry var5 : (Iterable<Entry>) (Iterable<?>) (var3.entrySet())) {
            if (((String)var5.getKey()).endsWith("#") && var5.getValue() instanceof Integer var6) {
               String var8 = b(((String)var5.getKey()).substring(0, ((String)var5.getKey()).length() - 1));
               var1.merge(var8, var6, Math::max);
            }
         }
      }

      return var1;
   }

   static Map<String, Object> b(Map<String, Object> var0) {
      LinkedHashMap<java.lang.String, java.lang.Object> var1 = new LinkedHashMap();
      ArrayList<java.lang.String> var2 = new ArrayList();

      for (String var4 : var0.keySet()) {
         if (var4.endsWith("#")) {
            var2.add(var4);
         }
      }

      var2.sort((var0x, var1x) -> Integer.compare(a(var0x).size(), a(var1x).size()));

      for (String var12 : (Iterable<String>) (Iterable<?>) (var2)) {
         var var5 = a(var12);
         ArrayList var6 = new ArrayList();
         int var7 = var0.get(var12) instanceof Number var8 ? var8.intValue() : 0;

         for (int var15 = 0; var15 < var7; var15++) {
            var6.add(null);
         }

         a(var1, var5.subList(0, var5.size() - 1), var6);
      }

      for (Entry var13 : var0.entrySet()) {
         if (!((String)var13.getKey()).endsWith("#")) {
            Object var14 = var13.getValue() == a ? new LinkedHashMap() : var13.getValue();
            a(var1, a((String)var13.getKey()), var14);
         }
      }

      return var1;
   }

   private static void a(Object var0, List<io.karatelabs.ext.rules.FlattenedCsvCodec.PathSegment> var1, Object var2) {
      Object var3 = var0;

      for (int var4 = 0; var4 < var1.size(); var4++) {
         io.karatelabs.ext.rules.FlattenedCsvCodec.PathSegment var5 = (io.karatelabs.ext.rules.FlattenedCsvCodec.PathSegment)var1.get(var4);
         boolean var6 = var4 == var1.size() - 1;
         io.karatelabs.ext.rules.FlattenedCsvCodec.PathSegment var7 = var6 ? null : (io.karatelabs.ext.rules.FlattenedCsvCodec.PathSegment)var1.get(var4 + 1);
         if (var5.a() != null) {
            Map<java.lang.String, java.lang.Object> var9 = (Map)var3;
            if (var6) {
               var9.put(var5.a(), var2);
            } else {
               var3 = var9.computeIfAbsent(var5.a(), var1x -> a(var7));
            }
         } else {
            List var8 = (List)var3;

            while (var8.size() <= var5.b()) {
               var8.add(null);
            }

            if (var6) {
               var8.set(var5.b(), var2);
            } else {
               if (var8.get(var5.b()) == null) {
                  var8.set(var5.b(), a(var7));
               }

               var3 = var8.get(var5.b());
            }
         }
      }
   }

   private static Object a(io.karatelabs.ext.rules.FlattenedCsvCodec.PathSegment var0) {
      return var0.a() != null ? new LinkedHashMap() : new ArrayList();
   }

   static List<String> b(List<Map<String, Object>> var0) {
      LinkedHashSet var1 = new LinkedHashSet();

      for (Map var3 : var0) {
         var1.addAll(var3.keySet());
      }

      return new ArrayList<>(var1);
   }

   static String a(List<String> var0, List<Map<String, Object>> var1) {
      StringBuilder var2 = new StringBuilder();

      for (int var3 = 0; var3 < var0.size(); var3++) {
         if (var3 > 0) {
            var2.append(',');
         }

         var2.append(e((String)var0.get(var3)));
      }

      var2.append('\n');

      for (Map var4 : var1) {
         for (int var5 = 0; var5 < var0.size(); var5++) {
            if (var5 > 0) {
               var2.append(',');
            }

            String var6 = (String)var0.get(var5);
            if (var4.containsKey(var6)) {
               var2.append(a(var4.get(var6)));
            }
         }

         var2.append('\n');
      }

      return var2.toString();
   }

   private static String e(String var0) {
      boolean var1 = var0.isEmpty() || var0.chars().anyMatch(var0x -> var0x == 44 || var0x == 34 || var0x == 10 || var0x == 13);
      return var1 ? f(var0) : var0;
   }

   static String a(Object var0) {
      if (var0 == null) {
         return "null";
      } else if (var0 == a) {
         return "{}";
      } else if (var0 instanceof Boolean var2) {
         return var2.toString();
      } else {
         return var0 instanceof Number var1 ? Terms.numberToString(var1) : f(String.valueOf(var0));
      }
   }

   private static String f(String var0) {
      return "\"" + var0.replace("\"", "\"\"") + "\"";
   }

   static List<Map<String, Object>> c(String var0) {
      var var1 = g(var0);
      if (var1.isEmpty()) {
         throw new RuleApiException(400, "CSV_INVALID", "the CSV has no header row");
      } else {
         ArrayList<java.lang.String> var2 = new ArrayList();

         for (String[] var4 : (Iterable<String[]>) (Iterable<?>) ((List)var1.getFirst())) {
            var2.add(var4[0]);
         }

         ArrayList<java.util.Map<java.lang.String,java.lang.Object>> var10 = new ArrayList();

         for (int var11 = 1; var11 < var1.size(); var11++) {
            List var5 = (List)var1.get(var11);
            if (var5.size() != 1 || !((String[])var5.getFirst())[0].isEmpty() || ((String[])var5.getFirst())[1] != null) {
               if (var5.size() != var2.size()) {
                  throw new RuleApiException(400, "CSV_INVALID", "row " + var11 + " has " + var5.size() + " cells, the header " + var2.size());
               }

               LinkedHashMap<java.lang.String, java.lang.Object> var6 = new LinkedHashMap();

               for (int var7 = 0; var7 < var2.size(); var7++) {
                  String[] var8 = (String[])var5.get(var7);
                  boolean var9 = var8[1] != null;
                  if (var9) {
                     var6.put((String)var2.get(var7), var8[0]);
                  } else if (!var8[0].isEmpty()) {
                     var6.put((String)var2.get(var7), a(var8[0], var11, (String)var2.get(var7)));
                  }
               }

               var10.add(var6);
            }
         }

         return var10;
      }
   }

   private static Object a(String var0, int var1, String var2) {
      switch (var0) {
         case "null":
            return null;
         case "true":
            return Boolean.TRUE;
         case "false":
            return Boolean.FALSE;
         case "{}":
            return a;
         default:
            try {
               return a(new BigDecimal(var0));
            } catch (NumberFormatException var6) {
               throw new RuleApiException(
                  400,
                  "CSV_INVALID",
                  "row " + var1 + " column '" + var2 + "': '" + var0 + "' is no bare token (null, true, false, {}, a number) — a string is quoted"
               );
            }
      }
   }

   static Number a(BigDecimal var0) {
      if (var0.scale() > 0 && var0.stripTrailingZeros().scale() > 0) {
         return var0.doubleValue();
      } else {
         try {
            long var1 = var0.longValueExact();
            return var1 == (int)var1 ? Integer.valueOf((int)var1).intValue() : Long.valueOf(var1);
         } catch (ArithmeticException var3) {
            return var0.doubleValue();
         }
      }
   }

   private static List<List<String[]>> g(String var0) {
      ArrayList<java.util.List<java.lang.String[]>> var1 = new ArrayList();
      ArrayList<java.lang.String[]> var2 = new ArrayList();
      StringBuilder var3 = new StringBuilder();
      boolean var4 = false;
      int var5 = 0;
      int var6 = var0.length();

      while (var5 < var6) {
         char var7 = var0.charAt(var5);
         if (var7 == '"' && var3.isEmpty() && !var4) {
            var4 = true;
            var5++;

            while (var5 < var6) {
               char var8 = var0.charAt(var5);
               if (var8 == '"') {
                  if (var5 + 1 >= var6 || var0.charAt(var5 + 1) != '"') {
                     var5++;
                     break;
                  }

                  var3.append('"');
                  var5 += 2;
               } else {
                  var3.append(var8);
                  var5++;
               }
            }
         } else if (var7 == ',') {
            var2.add(new String[]{var3.toString(), var4 ? "" : null});
            var3.setLength(0);
            var4 = false;
            var5++;
         } else if (var7 != '\n' && var7 != '\r') {
            if (var4) {
               throw new RuleApiException(400, "CSV_INVALID", "text after a closing quote in record " + (var1.size() + 1));
            }

            var3.append(var7);
            var5++;
         } else {
            var2.add(new String[]{var3.toString(), var4 ? "" : null});
            var3.setLength(0);
            var4 = false;
            var1.add(var2);
            var2 = new ArrayList();
            var5 += var7 == '\r' && var5 + 1 < var6 && var0.charAt(var5 + 1) == '\n' ? 2 : 1;
         }
      }

      if (!var3.isEmpty() || var4 || !var2.isEmpty()) {
         var2.add(new String[]{var3.toString(), var4 ? "" : null});
         var1.add(var2);
      }

      return var1;
   }

   record PathSegment(String component1, int b, boolean c) {
      static final io.karatelabs.ext.rules.FlattenedCsvCodec.PathSegment a = new io.karatelabs.ext.rules.FlattenedCsvCodec.PathSegment(null, -1, true);

      PathSegment(String component1, int b, boolean c) {
         this.component1 = component1;
         this.b = b;
         this.c = c;
      }

      static io.karatelabs.ext.rules.FlattenedCsvCodec.PathSegment a(String var0) {
         return new io.karatelabs.ext.rules.FlattenedCsvCodec.PathSegment(var0, -1, false);
      }

      static io.karatelabs.ext.rules.FlattenedCsvCodec.PathSegment a(int var0) {
         return new io.karatelabs.ext.rules.FlattenedCsvCodec.PathSegment(null, var0, false);
      }

      public String a() {
         return this.component1;
      }

      public int b() {
         return this.b;
      }

      public boolean c() {
         return this.c;
      }
   }
}
