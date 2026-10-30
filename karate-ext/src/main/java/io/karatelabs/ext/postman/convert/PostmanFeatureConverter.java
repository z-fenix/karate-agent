package io.karatelabs.ext.postman.convert;

import io.karatelabs.ext.agent.llm.SecretRedactor;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class PostmanFeatureConverter {
   private static final Pattern a = Pattern.compile("^\\* def ([A-Za-z_$][\\w$]*) = .*");
   private static final Pattern b = Pattern.compile(
      "(?:pm\\.(?:environment|collectionVariables|variables|globals|iterationData)\\.get|postman\\.get(?:Environment|Global)Variable)\\s*\\(\\s*(['\"])([^'\"]+)\\1"
   );
   private static final Pattern c = Pattern.compile("pm\\.iterationData\\.get\\s*\\(\\s*(['\"])([^'\"]+)\\1");
   private static final Pattern d = Pattern.compile("[^A-Za-z0-9]+|(?<=[a-z0-9])(?=[A-Z])");
   private static final Set<String> e = Set.of("auth", "authorization", "key", "pass", "pwd", "credential", "credentials", "bearer", "jwt");
   private static final Pattern f = Pattern.compile(
      "^(@|#|\\||\"\"\"|\\*|(Feature|Background|Scenario|Scenario Outline|Examples|Rule|Given|When|Then|And|But)\\b).*"
   );

   private PostmanFeatureConverter() {
   }

   public static List<io.karatelabs.ext.postman.PostmanCollectionNode> a(io.karatelabs.ext.postman.PostmanCollectionNode var0) {
      ArrayList<io.karatelabs.ext.postman.PostmanCollectionNode> var1 = new ArrayList();
      a(var0, var1);
      return var1;
   }

   private static void a(io.karatelabs.ext.postman.PostmanCollectionNode var0, List<io.karatelabs.ext.postman.PostmanCollectionNode> var1) {
      if (var0.b()) {
         var1.add(var0);
      } else {
         for (io.karatelabs.ext.postman.PostmanCollectionNode var3 : var0.d) {
            a(var3, var1);
         }
      }
   }

   public static PostmanConversionResult a(io.karatelabs.ext.postman.PostmanCollectionNode var0, io.karatelabs.ext.postman.convert.PostmanFeatureConverter.MigrationOptions var1) {
      var var2 = a(var0);
      ArrayList<io.karatelabs.ext.postman.convert.PostmanConversionResult.NotConvertedRequest> var3 = new ArrayList();
      PostmanRequestConverter var4 = new PostmanRequestConverter((var1x, var2x) -> var3.add(new io.karatelabs.ext.postman.convert.PostmanConversionResult.NotConvertedRequest(var1x, var2x)));
      LinkedHashMap<java.lang.String, java.lang.Object> var5 = new LinkedHashMap();
      LinkedHashSet<java.lang.String> var6 = new LinkedHashSet();

      for (Map var8 : var0.c().G()) {
         if (var8.get("key") != null && !Boolean.TRUE.equals(var8.get("disabled"))) {
            String var9 = String.valueOf(var8.get("key"));
            var5.put(var9, var8.get("value") == null ? "" : String.valueOf(var8.get("value")));
            if ("secret".equals(var8.get("type")) || a(var9)) {
               var6.add(var9);
            }
         }
      }

      LinkedHashSet var24 = new LinkedHashSet(var1.c() == null ? List.of() : var1.c());
      ArrayList<java.util.Set<java.lang.String>> var25 = new ArrayList();
      ArrayList<java.util.LinkedHashSet> var26 = new ArrayList();
      LinkedHashSet var10 = new LinkedHashSet();
      LinkedHashSet var11 = new LinkedHashSet();
      LinkedHashSet var12 = new LinkedHashSet();

      for (io.karatelabs.ext.postman.PostmanCollectionNode var14 : (Iterable<io.karatelabs.ext.postman.PostmanCollectionNode>) (Iterable<?>) (var2)) {
         var var15 = b(var14);
         LinkedHashSet<java.lang.String> var16 = new LinkedHashSet();

         for (String var18 : List.of("prerequest", "test")) {
            for (String var20 : (Iterable<String>) (Iterable<?>) (io.karatelabs.ext.postman.convert.PostmanRequestConverter.a(var14, var18))) {
               for (String var22 : new PostmanScriptConverter(var0x -> {}).a(var20)) {
                  Matcher var23 = a.matcher(var22);
                  if (var23.matches()) {
                     var16.add(var23.group(1));
                  }
               }

               a(c, var20, var12);
            }
         }

         var25.add(var15);
         var26.add(var16);
         var10.addAll(var15);
         var11.addAll(var16);
      }

      boolean var27 = false;

      for (int var28 = 0; var28 < var2.size() && !var27; var28++) {
         for (int var30 = var28 + 1; var30 < var2.size() && !var27; var30++) {
            var27 = ((Set)var26.get(var28)).stream().anyMatch(((Set)var25.get(var30))::contains);
         }
      }

      boolean var29 = var1.b() != null && var10.stream().anyMatch(var24::contains);
      ArrayList<java.lang.String> var31 = new ArrayList();
      ArrayList<java.lang.String> var32 = new ArrayList();
      PostmanVariableTranslator var33 = new PostmanVariableTranslator(true);

      for (String var36 : (Iterable<String>) (Iterable<?>) (var10)) {
         if (!var29 || !var24.contains(var36)) {
            if (var5.containsKey(var36)) {
               String var40 = (String)var5.get(var36);
               if (io.karatelabs.ext.postman.convert.PostmanVariableTranslator.a(var36) && (!var40.isEmpty() || !var11.contains(var36))) {
                  if (var6.contains(var36)) {
                     var31.add("* def " + var36 + " = karate.get(" + io.karatelabs.ext.postman.convert.PostmanVariableTranslator.b(var36) + ")");
                     if (!var11.contains(var36)) {
                        var32.add(var36);
                     }
                  } else {
                     var31.add("* def " + var36 + " = karate.get(" + io.karatelabs.ext.postman.convert.PostmanVariableTranslator.b(var36) + ", " + var33.d(var40) + ")");
                  }
               }
            } else if (!var11.contains(var36)) {
               var32.add(var36);
            }
         }
      }

      var var35 = var32.stream().filter(var12::contains).toList();
      if (!var35.isEmpty()) {
         String var37 = "the data rows: pm.iterationData reads " + String.join(", ", var35) + " — migrate with a data file";
         var31.add("# not converted: " + var37);
         var3.add(new io.karatelabs.ext.postman.convert.PostmanConversionResult.NotConvertedRequest(io.karatelabs.ext.postman.convert.PostmanRequestConverter.a(var0.d()), var37));
      }

      for (String var41 : (Iterable<String>) (Iterable<?>) (var32)) {
         if (!var35.contains(var41) && !var6.contains(var41)) {
            var31.add("# " + var41 + ": not a collection variable — karate-config defines it");
         }
      }

      StringBuilder var39 = new StringBuilder();
      var39.append("Feature: ").append(io.karatelabs.ext.postman.convert.PostmanRequestConverter.a(var0.d())).append('\n');
      String var42 = var0.f() == null ? "" : var0.f().strip().split("\n")[0].strip();
      if (!var42.isEmpty() && !f.matcher(var42).matches()) {
         var39.append("  ").append(var42).append('\n');
      }

      var39.append("  Migrated from Postman: ").append(var1.a()).append('\n');
      if (!var31.isEmpty()) {
         var39.append("\n  Background:\n");
         var31.forEach(var1x -> var39.append("    ").append(var1x).append('\n'));
      }

      int var43 = 0;
      if (!var27 && var2.size() != 1) {
         for (int var44 = 0; var44 < var2.size(); var44++) {
            boolean var45 = var29 && ((Set)var25.get(var44)).stream().anyMatch(var24::contains);
            a(
               var39,
               io.karatelabs.ext.postman.convert.PostmanRequestConverter.a(((io.karatelabs.ext.postman.PostmanCollectionNode)var2.get(var44)).d()),
               List.of((io.karatelabs.ext.postman.PostmanCollectionNode)var2.get(var44)),
               var45,
               var1,
               var4
            );
            var43++;
         }
      } else {
         a(var39, io.karatelabs.ext.postman.convert.PostmanRequestConverter.a(var27 ? var0.d() : ((io.karatelabs.ext.postman.PostmanCollectionNode)var2.get(0)).d()), var2, var29, var1, var4);
         var43 = 1;
      }

      return new PostmanConversionResult(var39.toString(), var2.size(), var43, var3, var32);
   }

   static boolean a(String var0) {
      if (io.karatelabs.ext.agent.llm.SecretRedactor.isSensitiveName(var0)) {
         return true;
      } else {
         for (String var4 : d.split(var0)) {
            if (e.contains(var4.toLowerCase())) {
               return true;
            }
         }

         return false;
      }
   }

   private static void a(StringBuilder var0, String var1, List<io.karatelabs.ext.postman.PostmanCollectionNode> var2, boolean var3, io.karatelabs.ext.postman.convert.PostmanFeatureConverter.MigrationOptions var4, PostmanRequestConverter var5) {
      var0.append('\n').append(var3 ? "  Scenario Outline: " : "  Scenario: ").append(var1).append('\n');
      var5.a();

      for (int var6 = 0; var6 < var2.size(); var6++) {
         if (var6 > 0) {
            var0.append('\n');
         }

         for (String var8 : var5.a((io.karatelabs.ext.postman.PostmanCollectionNode)var2.get(var6))) {
            var0.append(var8.isEmpty() ? "" : "    ").append(var8).append('\n');
         }
      }

      if (var3) {
         var0.append("\n    Examples:\n      | read(").append(io.karatelabs.ext.postman.convert.PostmanVariableTranslator.b(var4.b())).append(") |\n");
      }
   }

   static Set<String> b(io.karatelabs.ext.postman.PostmanCollectionNode var0) {
      LinkedHashSet<java.lang.String> var1 = new LinkedHashSet();
      Object var2 = var0.b.get("request", null);
      a(var2, var1);
      Map var3 = io.karatelabs.ext.postman.convert.PostmanRequestConverter.b(var0);
      if (var3 != null) {
         a(var3, var1);
      }

      for (String var5 : List.of("prerequest", "test")) {
         for (String var7 : (Iterable<String>) (Iterable<?>) (io.karatelabs.ext.postman.convert.PostmanRequestConverter.a(var0, var5))) {
            a(b, var7, var1);
         }
      }

      return var1;
   }

   private static void a(Object var0, Set<String> var1) {
      if (var0 instanceof String var2) {
         io.karatelabs.ext.postman.convert.PostmanVariableTranslator.a(var2, var1);
      } else if (var0 instanceof Map var3) {
         for (Entry var6 : (Iterable<Entry>) (Iterable<?>) (var3.entrySet())) {
            if (!"description".equals(var6.getKey())) {
               a(var6.getValue(), var1);
            }
         }
      } else if (var0 instanceof List) {
         for (Object var9 : (List)var0) {
            if (!(var9 instanceof Map var7 && Boolean.TRUE.equals(var7.get("disabled")))) {
               a(var9, var1);
            }
         }
      }
   }

   private static void a(Pattern var0, String var1, Set<String> var2) {
      Matcher var3 = var0.matcher(var1);

      while (var3.find()) {
         var2.add(var3.group(2));
      }
   }

   public record MigrationOptions(String a, String b, List<String> c) {


      public static io.karatelabs.ext.postman.convert.PostmanFeatureConverter.MigrationOptions a(String var0) {
         return new io.karatelabs.ext.postman.convert.PostmanFeatureConverter.MigrationOptions(var0, null, List.of());
      }
   }
}
