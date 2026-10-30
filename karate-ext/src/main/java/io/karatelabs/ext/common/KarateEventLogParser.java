package io.karatelabs.ext.common;

import io.karatelabs.common.Json;
import java.io.BufferedReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class KarateEventLogParser {
   public static final String a = "karate-json/karate-events.jsonl";
   public static final String b = "karate-json/run.json";
   private static final Pattern c = Pattern.compile("^\\[(\\d+)(?:\\.(\\d+))?:");

   private static List<List<String>> a(Object var0) {
      if (!(var0 instanceof List var1)) {
         return null;
      } else {
         ArrayList var2 = new ArrayList();

         for (Object var4 : var1) {
            ArrayList var5 = new ArrayList();
            if (var4 instanceof Map var6) {
               Object var8 = var6.get("row");
               if (var8 instanceof List) {
                  for (Object var9 : (List)var8) {
                     var5.add(String.valueOf(var9));
                  }
               }
            }

            var2.add(var5);
         }

         return var2;
      }
   }

   private KarateEventLogParser() {
   }

   public static io.karatelabs.ext.common.KarateEventLogParser.KarateEventLogParseResult a(Path var0) {
      Path var1 = Files.isDirectory(var0) ? var0.resolve("karate-json/karate-events.jsonl") : var0;
      ArrayList var2 = new ArrayList();
      LinkedHashMap var3 = new LinkedHashMap();
      int var4 = 0;
      int var5 = 0;
      int[] var6 = new int[]{0};

      String var8;
      try (BufferedReader var7 = Files.newBufferedReader(var1, StandardCharsets.UTF_8)) {
         while ((var8 = var7.readLine()) != null) {
            if (!var8.isBlank() && var8.contains("\"FEATURE_EXIT\"")) {
               var4++;

               try {
                  a(var8, var2, var3, var6);
               } catch (RuntimeException var11) {
                  var5++;
               }
            }
         }
      } catch (Exception var13) {
         return new io.karatelabs.ext.common.KarateEventLogParser.KarateEventLogParseResult(List.of(), var4, var5 + 1, var6[0]);
      }

      return new io.karatelabs.ext.common.KarateEventLogParser.KarateEventLogParseResult(var2, var4, var5, var6[0]);
   }

   private static void a(String var0, List<io.karatelabs.ext.common.KarateEventLogParser.KarateScenarioResult> var1, Map<String, Integer> var2, int[] var3) {
      if (Json.of(var0).value() instanceof Map var4 && "FEATURE_EXIT".equals(var4.get("type")) && var4.get("data") instanceof Map var5) {
         String var12 = a(var5.get("relativePath"), a(var5.get("prefixedPath"), ""));
         Object var8 = var5.get("scenarioResults");
         if (var8 instanceof List) {
            for (Object var9 : (List)var8) {
               if (var9 instanceof Map var10) {
                  var1.add(a(var12, var10, var2, var3));
               }
            }
         }
      }
   }

   private static io.karatelabs.ext.common.KarateEventLogParser.KarateScenarioResult a(String var0, Map<String, Object> var1, Map<String, Integer> var2, int[] var3) {
      String var4 = a(var1.get("name"), "");
      int var5 = d(var1.get("line"));
      String var6 = a(var1.get("slug"), "");
      if (var6.isEmpty()) {
         var6 = var0 + "::" + var4;
      }

      int var7 = var2.merge(var0 + "::" + var6, 1, Integer::sum) - 1;
      LinkedHashSet var8 = new LinkedHashSet();
      Object var10 = var1.get("tags");
      if (var10 instanceof List) {
         for (Object var11 : (List)var10) {
            String var12 = String.valueOf(var11);
            var8.add(var12.startsWith("@") ? var12.substring(1) : var12);
         }
      }

      ArrayList var20 = new ArrayList();
      var10 = null;
      if (var1.get("stepResults") instanceof List var23) {
         int var26 = 0;

         for (Object var14 : var23) {
            if (var14 instanceof Map var15) {
               var var17 = c(var15.get("step"));
               var var18 = c(var15.get("result"));
               String var19 = var18.get("errorMessage") == null ? null : String.valueOf(var18.get("errorMessage"));
               if (var19 != null && var10 == null) {
                  var10 = var19;
               }

               var20.add(
                  new io.karatelabs.ext.common.KarateEventLogParser.KarateStepResult(
                     var26,
                     a(var17.get("prefix"), ""),
                     a(var17.get("keyword"), ""),
                     a(var17.get("text"), "").trim(),
                     var17.get("docString") == null ? null : String.valueOf(var17.get("docString")),
                     a(var17.get("table")),
                     a(var18.get("status"), "skipped"),
                     var19,
                     Boolean.TRUE.equals(var17.get("background")),
                     a(var15, var3),
                     b(var15)
                  )
               );
               var26++;
            }
         }
      }

      String var24 = Boolean.TRUE.equals(var1.get("skipped"))
         ? "skipped"
         : (Boolean.TRUE.equals(var1.get("failed")) ? "failed" : (Boolean.TRUE.equals(var1.get("passed")) ? "passed" : "failed"));
      Matcher var27 = c.matcher(a(var1.get("refId"), ""));
      boolean var28 = var27.find();
      int var29 = var28 ? Integer.parseInt(var27.group(1)) - 1 : -1;
      int var30 = var28 && var27.group(2) != null ? Integer.parseInt(var27.group(2)) - 1 : -1;
      return new io.karatelabs.ext.common.KarateEventLogParser.KarateScenarioResult(var0, var6, var7, var4, var5, var8, var24, (String)var10, var20, var30, var29);
   }

   private static int b(Map<String, Object> var0) {
      return var0.get("callResults") instanceof List var1 ? b(var1) : -1;
   }

   private static int b(Object var0) {
      int var1 = 0;
      if (var0 instanceof Map var2) {
         for (Entry var5 : (Iterable<Entry>) (Iterable<?>) (var2.entrySet())) {
            if ("embeds".equals(var5.getKey())) {
               Object var7 = var5.getValue();
               if (var7 instanceof List) {
                  for (Object var8 : (List)var7) {
                     if (var8 instanceof Map var9 && "http-exchange".equals(var9.get("name"))) {
                        var1++;
                     }
                  }
                  continue;
               }
            }

            var1 += b(var5.getValue());
         }
      } else if (var0 instanceof List) {
         for (Object var11 : (List)var0) {
            var1 += b(var11);
         }
      }

      return var1;
   }

   private static List<Map<String, Object>> a(Map<String, Object> var0, int[] var1) {
      ArrayList var2 = new ArrayList();
      Object var4 = var0.get("embeds");
      if (var4 instanceof List) {
         for (Object var5 : (List)var4) {
            if (var5 instanceof Map var6 && "http-exchange".equals(var6.get("name"))) {
               String var7 = a(var6);
               if (!var7.isEmpty()) {
                  try {
                     String var8 = new String(Base64.getDecoder().decode(var7), StandardCharsets.UTF_8);
                     if (Json.of(var8).value() instanceof Map var9) {
                        var2.add(var9);
                     }
                  } catch (RuntimeException var11) {
                     var1[0]++;
                  }
               }
            }
         }

         return var2;
      } else {
         return var2;
      }
   }

   public static String a(Map<String, Object> var0) {
      if (var0.get("parts") instanceof List var1 && !var1.isEmpty()) {
         for (Object var3 : var1) {
            if (var3 instanceof Map var4 && "primary".equals(var4.get("role"))) {
               return a(var4.get("data"), "");
            }
         }

         if (var1.get(0) instanceof Map var6) {
            return a(var6.get("data"), "");
         }
      }

      return a(var0.get("data"), "");
   }

   private static Map<String, Object> c(Object var0) {
      return var0 instanceof Map var1 ? var1 : Map.of();
   }

   private static String a(Object var0, String var1) {
      return var0 == null ? var1 : String.valueOf(var0);
   }

   private static int d(Object var0) {
      return var0 instanceof Number var1 ? var1.intValue() : 0;
   }

   public record KarateEventLogParseResult(List<io.karatelabs.ext.common.KarateEventLogParser.KarateScenarioResult> b, int c, int d, int e) {


      public boolean a() {
         return this.d == 0 && this.e == 0;
      }

      public List<io.karatelabs.ext.common.KarateEventLogParser.KarateScenarioResult> b() {
         return this.b;
      }

      public int c() {
         return this.c;
      }

      public int d() {
         return this.d;
      }

      public int e() {
         return this.e;
      }
   }

   public record KarateScenarioResult(String a, String b, int c, String d, int e, Set<String> f, String g, String h, List<io.karatelabs.ext.common.KarateEventLogParser.KarateStepResult> i, int j, int k) {

   }

   public record KarateStepResult(int b, String c, String d, String e, String f, List<List<String>> g, String h, String i, boolean j, List<Map<String, Object>> k, int l) {


      public String a() {
         return this.d.isEmpty() ? this.e : this.d + " " + this.e;
      }

      public int b() {
         return this.b;
      }

      public String c() {
         return this.c;
      }

      public String d() {
         return this.d;
      }

      public String e() {
         return this.e;
      }

      public String f() {
         return this.f;
      }

      public List<List<String>> g() {
         return this.g;
      }

      public String h() {
         return this.h;
      }

      public String i() {
         return this.i;
      }

      public boolean j() {
         return this.j;
      }

      public List<Map<String, Object>> k() {
         return this.k;
      }

      public int l() {
         return this.l;
      }
   }
}
