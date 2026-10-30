package io.karatelabs.ext.rules;

import io.karatelabs.common.Json;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class RuleInputSpaceCertificateBuilder {
   static final String a = "certificate.json";
   public static final String b = "5";

   private RuleInputSpaceCertificateBuilder() {
   }

   public static List<io.karatelabs.ext.rules.analysis.AbstractBox.BoxDimension> a(Object var0) {
      if (var0 instanceof Map var1) {
         LinkedHashMap var2 = new LinkedHashMap();
         var1.forEach(var2::put);
         return io.karatelabs.ext.rules.analysis.AbstractBox.a(var2);
      } else {
         throw new io.karatelabs.ext.rules.analysis.RuleAnalysisException("OPEN_AXIS", -1, "schema.js declares no object shape");
      }
   }

   public static boolean b(Object var0) {
      try {
         a(var0);
         return true;
      } catch (io.karatelabs.ext.rules.analysis.RuleAnalysisException var2) {
         return false;
      }
   }

   public static Map<String, Object> a(String var0, Object var1, String var2, String var3, io.karatelabs.ext.rules.analysis.AbstractInterpreter.AnalysisBudget var4) {
      return a(var0, var1, var2, var3, var4, io.karatelabs.ext.rules.analysis.SplitTreeListener.b);
   }

   public static Map<String, Object> a(
      String var0, Object var1, String var2, String var3, io.karatelabs.ext.rules.analysis.AbstractInterpreter.AnalysisBudget var4, io.karatelabs.ext.rules.analysis.SplitTreeListener var5
   ) {
      return a(var0, Map.of(), var1, var2, var3, var4, var5);
   }

   public static Map<String, Object> a(
      String var0, Map<String, Object> var1, Object var2, String var3, String var4, io.karatelabs.ext.rules.analysis.AbstractInterpreter.AnalysisBudget var5, io.karatelabs.ext.rules.analysis.SplitTreeListener var6
   ) {
      var var7 = a(var2);
      io.karatelabs.ext.rules.analysis.AbstractInterpreter.AbstractInterpretationResult var8 = io.karatelabs.ext.rules.analysis.AbstractInterpreter.a(var0, var1, var7, var5, var6);
      LinkedHashMap<java.lang.String, java.lang.Object> var9 = new LinkedHashMap();
      var9.put("calcSha256", var3);
      var9.put("schemaSha256", var4);
      var9.put("evaluator", "5");
      var9.put("axes", a(var7));
      var9.put("declared", var8.e());
      var9.put("refused", var8.f());
      var9.put("accepted", var8.g());
      var9.put("leaves", (long)var8.a().size());
      var9.put("concrete", var8.k());
      var9.put("boxes", var8.h());
      var9.put("treeNodes", var8.i());
      var9.put("refinementNodes", var8.j());
      var9.put("guardRuns", var8.l());
      var9.put("noProgress", var8.m());
      var9.put("budget", var5.a());
      var9.put("propertyBudget", var5.b());
      var9.put("partial", var8.n());
      var9.put("unresolved", var8.o());
      ArrayList<java.util.LinkedHashMap> var10 = new ArrayList();

      for (io.karatelabs.ext.rules.analysis.AbstractInterpreter.SignatureAggregate var12 : var8.b().values()) {
         LinkedHashMap<java.lang.String, java.lang.Object> var13 = new LinkedHashMap();
         var13.put("key", a(var12.a()));
         var13.put("rows", var12.b());
         var13.put("leaves", var12.c());
         var13.put("witness", var12.d());
         var10.add(var13);
      }

      var9.put("signatures", var10);
      var9.put("refusedRegions", b(var8.c()));
      ArrayList<java.util.LinkedHashMap> var15 = new ArrayList();

      for (io.karatelabs.ext.rules.analysis.AbstractInterpreter.PropertyPartition var17 : var8.d()) {
         LinkedHashMap<java.lang.String, java.lang.Object> var14 = new LinkedHashMap();
         var14.put("name", var17.b());
         var14.put("kind", var17.c());
         if (var17.d() != null) {
            var14.put("req", var17.d());
         }

         var14.put("trueRows", var17.e());
         var14.put("falseRows", var17.f());
         var14.put("unknownRows", var17.g());
         if (var8.n()) {
            var14.put("unresolvedRows", var8.o());
         }

         var14.put("executedRows", var17.a());
         var14.put("boxes", var17.i());
         if (var17.h() != null) {
            var14.put("counterexample", var17.h());
         }

         var15.add(var14);
      }

      var9.put("properties", var15);
      return var9;
   }

   private static List<Map<String, Object>> a(List<io.karatelabs.ext.rules.analysis.AbstractBox.BoxDimension> var0) {
      ArrayList<java.util.Map<java.lang.String,java.lang.Object>> var1 = new ArrayList();

      for (io.karatelabs.ext.rules.analysis.AbstractBox.BoxDimension var3 : var0) {
         LinkedHashMap<java.lang.String, java.lang.Object> var4 = new LinkedHashMap();
         var4.put("name", var3.c());
         var4.put("kind", var3.d().name());
         var4.put("lo", var3.e());
         var4.put("hi", var3.f());
         var4.put("members", var3.g());
         var1.add(var4);
      }

      return var1;
   }

   private static List<Map<String, Object>> b(List<io.karatelabs.ext.rules.analysis.AbstractInterpreter.BoxInterpretationResult> var0) {
      LinkedHashMap<String, Map<String, Object>> var1 = new LinkedHashMap<>();

      for (io.karatelabs.ext.rules.analysis.AbstractInterpreter.BoxInterpretationResult var3 : var0) {
         String var4 = var3.f() + "|" + var3.c();
         Map<String, Object> var5 = var1.get(var4);
         if (var5 == null) {
            var5 = new LinkedHashMap();
            ((java.util.Map)var5).put("box", var3.b().toString());
            ((java.util.Map)var5).put("line", var3.f());
            ((java.util.Map)var5).put("signature", a(var3.c()));
            ((java.util.Map)var5).put("rows", 0L);
            ((java.util.Map)var5).put("leaves", 0L);
            var1.put(var4, var5);
         }

         ((java.util.Map)var5).put("rows", (Long)((java.util.Map)var5).get("rows") + var3.a());
         ((java.util.Map)var5).put("leaves", (Long)((java.util.Map)var5).get("leaves") + 1L);
      }

      return new ArrayList<>(var1.values());
   }

   static Path a(Path var0) {
      return var0.resolve(".baseline").resolve("certificate.json");
   }

   static void a(Path var0, Map<String, Object> var1) {
      Path var2 = a(var0);

      try {
         Files.createDirectories(var2.getParent());
         Files.writeString(var2, Json.stringifyStrict(var1), StandardCharsets.UTF_8);
      } catch (IOException var4) {
         throw new UncheckedIOException(var4);
      }
   }

   static io.karatelabs.ext.rules.RuleInputSpaceCertificateBuilder.InputSpaceCertificateResult a(Path var0, String var1, String var2) {
      Path var3 = a(var0);
      if (!Files.isRegularFile(var3)) {
         return null;
      } else {
         Map var4;
         try {
            if (!(Json.parseStrict(Files.readString(var3, StandardCharsets.UTF_8), true) instanceof Map var6)) {
               return null;
            }

            var4 = var6;
         } catch (RuntimeException | IOException var7) {
            return null;
         }

         return a(var4, var1, var2) ? io.karatelabs.ext.rules.RuleInputSpaceCertificateBuilder.InputSpaceCertificateResult.a(var4) : io.karatelabs.ext.rules.RuleInputSpaceCertificateBuilder.InputSpaceCertificateResult.b(var4);
      }
   }

   static boolean a(Map<String, Object> var0, String var1, String var2) {
      return var1 != null && var1.equals(var0.get("calcSha256")) && var2 != null && var2.equals(var0.get("schemaSha256")) && "5".equals(var0.get("evaluator"));
   }

   static String a(String var0) {
      ArrayList<java.lang.String> var1 = new ArrayList();

      for (String var5 : var0.split(";")) {
         if (!var5.isEmpty()) {
            var1.add(var5);
         }
      }

      var1.sort((var0x, var1x) -> RuleCoverageMatrixBuilder.a(var0x.substring(0, var0x.indexOf(61)), var1x.substring(0, var1x.indexOf(61))));
      StringBuilder var6 = new StringBuilder();

      for (String var8 : (Iterable<String>) (Iterable<?>) (var1)) {
         var6.append(var8).append(';');
      }

      return var6.toString();
   }

   public record InputSpaceCertificateResult(Map<String, Object> c, boolean d, String e, int f, String g, String h) {

      public InputSpaceCertificateResult(Map<String, Object> c, boolean d, String e, int f, String g, String h) {
         this.c = c;
         this.d = d;
         this.e = e;
         this.f = f;
         this.g = g;
         this.h = h;
      }

      public boolean a() {
         return this.c != null && !this.d;
      }

      public boolean b() {
         return this.c != null && Boolean.TRUE.equals(this.c.get("partial"));
      }

      static io.karatelabs.ext.rules.RuleInputSpaceCertificateBuilder.InputSpaceCertificateResult a(Map<String, Object> var0) {
         return new io.karatelabs.ext.rules.RuleInputSpaceCertificateBuilder.InputSpaceCertificateResult(var0, false, null, -1, null, null);
      }

      static io.karatelabs.ext.rules.RuleInputSpaceCertificateBuilder.InputSpaceCertificateResult b(Map<String, Object> var0) {
         return new io.karatelabs.ext.rules.RuleInputSpaceCertificateBuilder.InputSpaceCertificateResult(var0, true, null, -1, null, null);
      }

      static io.karatelabs.ext.rules.RuleInputSpaceCertificateBuilder.InputSpaceCertificateResult a(io.karatelabs.ext.rules.analysis.RuleAnalysisException var0) {
         return new io.karatelabs.ext.rules.RuleInputSpaceCertificateBuilder.InputSpaceCertificateResult(null, false, var0.a, var0.b, var0.getMessage(), var0.c);
      }

      public Map<String, Object> c() {
         return this.c;
      }

      public boolean d() {
         return this.d;
      }

      public String e() {
         return this.e;
      }

      public int f() {
         return this.f;
      }

      public String g() {
         return this.g;
      }

      public String h() {
         return this.h;
      }
   }
}
