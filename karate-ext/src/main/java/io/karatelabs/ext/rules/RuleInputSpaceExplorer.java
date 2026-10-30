package io.karatelabs.ext.rules;

import io.karatelabs.common.Json;
import io.karatelabs.js.ContextListener;
import io.karatelabs.js.Engine;
import io.karatelabs.js.Event;
import io.karatelabs.js.EventType;
import io.karatelabs.parser.Node;
import io.karatelabs.parser.NodeType;
import io.karatelabs.parser.Token;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class RuleInputSpaceExplorer {
   private static final int d = 8;
   private static final int e = 500;
   private static final int f = 10;
   public static final int a = 2;
   public static final long b = 50000L;
   private static final String g = "2026-01-01";
   private static final Set<String> h = Set.of("==", "===", "!=", "!==");
   private static final NodeType[] i = new NodeType[]{
      NodeType.IF_STMT, NodeType.LOGIC_TERN_EXPR, NodeType.LOGIC_AND_EXPR, NodeType.LOGIC_NULLISH_EXPR, NodeType.CASE_BLOCK
   };
   private static final Set<String> j = Set.of("calc.always", "calc.sometimes");
   private static final Pattern k = Pattern.compile(
      "\\b(?:var|let|const)\\s+([A-Za-z_$][\\w$]*)\\s*=\\s*(?:calc\\.)?input\\.([\\w$]+(?:\\.[\\w$]+)*)\\s*(?=[;\\r\\n])"
   );
   private static final Pattern l = Pattern.compile("\\b(?:var|let|const)\\s+([A-Za-z_$][\\w$]*)\\s*=\\s*calc\\.input\\s*(?=[;\\r\\n])");
   private final String m;
   private final Map<String, Object> n;
   private final Map<String, List<String>> o;
   private final List<String> p;
   private final Object q;
   private final List<io.karatelabs.ext.rules.RuleInputSpaceExplorer.SchemaSite> r = new ArrayList<>();
   private final Map<Integer, io.karatelabs.ext.rules.RuleInputSpaceExplorer.SchemaSite> s = new LinkedHashMap<>();
   private final List<io.karatelabs.ext.rules.RuleInputSpaceExplorer.InputDimension> t = new ArrayList<>();
   private final Map<Integer, Set<Boolean>> u = new LinkedHashMap<>();
   private final Map<Integer, Set<Boolean>> v = new LinkedHashMap<>();
   private final List<Map<String, Object>> w = new ArrayList<>();
   private final Map<String, Map<String, Object>> x = new TreeMap<>();
   private final List<Map<String, Object>> y = new ArrayList<>();
   private Map<String, Object> z;
   private final Map<Integer, String> A = new TreeMap<>();
   private final Set<Object> B = new LinkedHashSet<>();
   private final List<String> C;
   private final Map<String, io.karatelabs.ext.rules.RuleInputSpaceExplorer.PropertyExecutionStats> D = new LinkedHashMap<>();
   private int E;
   private int F;
   private boolean G;
   private final Long H;
   public static final int c = 512;
   private static final int I = 8;
   private int J = 512;
   private Function<Map<String, Supplier<List<Map<String, Object>>>>, List<Map<String, Object>>> K;
   private int L = 2;
   private int M;
   private long N;
   private int O;
   private final Set<String> P = new HashSet<>();
   private long Q;
   private boolean R;
   private int S;
   private boolean T;
   private boolean U;
   private int V;
   private int W;
   private boolean X;
   private boolean Y = true;
   private int Z;
   private int aa;
   private int ab;
   private long ac;
   private final Map<String, Map<String, Object>> ad = new LinkedHashMap<>();
   private int ae;
   private int af;
   private int ag;
   private final Map<Integer, Set<Boolean>> ah = new LinkedHashMap<>();
   private boolean ai;
   private final List<Map<String, Object>> aj = new ArrayList<>();
   private final List<Map<String, Object>> ak = new ArrayList<>();
   private io.karatelabs.ext.rules.RuleInputSpaceCertificateBuilder.InputSpaceCertificateResult al;
   private final Set<String> am = new LinkedHashSet<>();
   private final List<Map<String, Object>> an = new ArrayList<>();
   private static final Set<String> ao = Set.of(
      "array", "boolean", "ignore", "notnull", "null", "number", "object", "present", "notpresent", "string", "uuid", "regex", "date"
   );

   private RuleInputSpaceExplorer(String var1, Map<String, Object> var2, Object var3, String var4, Long var5) {
      this.m = var1;
      this.n = var2;
      this.q = var3;
      this.H = var5;
      this.C = h(var1);
      this.e(var1);
      this.a();
      if (var4 != null && !var4.isBlank()) {
         io.karatelabs.ext.rules.Generator.GeneratorPlan var6 = Generator.a(var4);
         this.z = b(io.karatelabs.ext.common.SandboxExecutor.a(var6.b()));
         this.a(var6);
      } else {
         this.e(var3, "");
         this.z = b(a(var3));
      }

      this.t.sort((var0, var1x) -> var0.a.compareTo(var1x.a));
      this.o = c(var1);
      this.p = d(var1);
   }

   private static Map<String, List<String>> c(String var0) {
      LinkedHashMap<java.lang.String, java.util.List<java.lang.String>> var1 = new LinkedHashMap();
      if (var0 == null) {
         return var1;
      } else {
         Matcher var2 = k.matcher(var0);

         while (var2.find()) {
            String var3 = var2.group(1);
            if (a(var0, var3)) {
               var1.computeIfAbsent(var2.group(2), var0x -> new ArrayList<>()).add(var3);
            }
         }

         return var1;
      }
   }

   private static List<String> d(String var0) {
      ArrayList<java.lang.String> var1 = new ArrayList();
      if (var0 == null) {
         return var1;
      } else {
         Matcher var2 = l.matcher(var0);

         while (var2.find()) {
            String var3 = var2.group(1);
            if (a(var0, var3)) {
               var1.add(var3);
            }
         }

         return var1;
      }
   }

   private static boolean a(String var0, String var1) {
      Matcher var2 = Pattern.compile("\\b" + Pattern.quote(var1) + "\\s*=(?!=)").matcher(var0);
      int var3 = 0;

      while (var2.find() && var3 < 2) {
         var3++;
      }

      return var3 == 1;
   }

   private boolean a(String var1, io.karatelabs.ext.rules.RuleInputSpaceExplorer.InputDimension var2) {
      if (c(var1, "input." + var2.a)) {
         return true;
      } else {
         for (String var4 : this.p) {
            if (c(var1, var4 + "." + var2.a)) {
               return true;
            }
         }

         for (String var6 : this.o.getOrDefault(var2.a, List.of())) {
            if (c(var1, var6)) {
               return true;
            }
         }

         return false;
      }
   }

   public static io.karatelabs.ext.rules.RuleInputSpaceExplorer.InputSpaceExplorationResult a(String var0, Object var1, List<Map<String, Object>> var2) {
      return a(var0, var1, null, var2, "coverage");
   }

   public static io.karatelabs.ext.rules.RuleInputSpaceExplorer.InputSpaceExplorationResult a(String var0, Object var1, List<Map<String, Object>> var2, String var3) {
      return a(var0, var1, null, var2, var3);
   }

   public static io.karatelabs.ext.rules.RuleInputSpaceExplorer.InputSpaceExplorationResult a(String var0, Object var1, String var2, List<Map<String, Object>> var3, String var4) {
      return a(var0, var1, var2, var3, var4, null);
   }

   public static io.karatelabs.ext.rules.RuleInputSpaceExplorer.InputSpaceExplorationResult a(String var0, Object var1, String var2, List<Map<String, Object>> var3, String var4, Long var5) {
      return a(var0, var1, var2, var3, var4, var5, 512);
   }

   static io.karatelabs.ext.rules.RuleInputSpaceExplorer.InputSpaceExplorationResult a(String var0, Object var1, String var2, List<Map<String, Object>> var3, String var4, Long var5, int var6) {
      return a(var0, var1, var2, var3, var4, var5, var6, 2);
   }

   public static io.karatelabs.ext.rules.RuleInputSpaceExplorer.InputSpaceExplorationResult a(String var0, Object var1, String var2, List<Map<String, Object>> var3, String var4, Long var5, int var6, int var7) {
      return a(var0, var1, var2, var3, var4, var5, var6, var7, null);
   }

   public static io.karatelabs.ext.rules.RuleInputSpaceExplorer.InputSpaceExplorationResult a(String var0, Object var1, String var2, List<Map<String, Object>> var3, String var4, Long var5, int var6, int var7, io.karatelabs.ext.rules.RuleInputSpaceCertificateBuilder.InputSpaceCertificateResult var8) {
      return a(var0, Map.of(), var1, var2, var3, var4, var5, var6, var7, var8);
   }

   public static io.karatelabs.ext.rules.RuleInputSpaceExplorer.InputSpaceExplorationResult a(
      String var0,
      Map<String, Object> var1,
      Object var2,
      String var3,
      List<Map<String, Object>> var4,
      String var5,
      Long var6,
      int var7,
      int var8,
      io.karatelabs.ext.rules.RuleInputSpaceCertificateBuilder.InputSpaceCertificateResult var9
   ) {
      return a(var0, var1, var2, var3, var4, var5, var6, var7, var8, var9, null);
   }

   static io.karatelabs.ext.rules.RuleInputSpaceExplorer.InputSpaceExplorationResult a(
      String var0,
      Map<String, Object> var1,
      Object var2,
      String var3,
      List<Map<String, Object>> var4,
      String var5,
      Long var6,
      int var7,
      int var8,
      io.karatelabs.ext.rules.RuleInputSpaceCertificateBuilder.InputSpaceCertificateResult var9,
      Function<Map<String, Supplier<List<Map<String, Object>>>>, List<Map<String, Object>>> var10
   ) {
      RuleInputSpaceExplorer var11 = new RuleInputSpaceExplorer(var0, var1, var2, var3, var6);
      var11.J = var7;
      var11.L = var8;
      var11.al = var9;
      var11.K = var10;
      return var11.a(var2, var4, var5);
   }

   private static Object b(Object var0, String var1) {
      Object var2 = var0;

      for (String var6 : var1.split("\\.")) {
         boolean var7 = var6.endsWith("[]");
         if (var7) {
            var6 = var6.substring(0, var6.length() - 2);
         }

         if (!(var2 instanceof Map var8)) {
            return null;
         }

         var2 = var8.get(var6);
         if (var7) {
            if (!(var2 instanceof List var9) || var9.isEmpty()) {
               return null;
            }

            var2 = var9.get(0);
         }
      }

      return var2;
   }

   private static List<Object> c(Object var0, String var1) {
      return b(var0, var1) instanceof List var2 && var2.stream().noneMatch(var0x -> var0x instanceof Map || var0x instanceof List)
         ? new ArrayList<>(var2)
         : null;
   }

   private static boolean d(Object var0, String var1) {
      return b(var0, var1) instanceof String var2 && ("#string".equals(var2) || "##string".equals(var2));
   }

   private io.karatelabs.ext.rules.NumericBoundsParser.NumericBounds a(io.karatelabs.ext.rules.Generator.GeneratorDimension var1) {
      io.karatelabs.ext.rules.NumericBoundsParser.NumericBounds var2 = g(this.q, var1.a());
      String var3 = var1.a();
      if (var2 != null) {
         for (Object var5 : "enum".equals(var1.b()) ? var1.c() : List.of()) {
            if (var5 instanceof Number var6 && !a(var2, var6.doubleValue())) {
               throw a(var3, "the sampled value " + a(var6.doubleValue()), var2);
            }
         }

         if (var1.d() != null && var2.a() != null && var1.d() < var2.a()) {
            throw a(var3, "lo " + a(var1.d().doubleValue()), var2);
         }

         if (var1.e() != null && var2.b() != null && var1.e() > var2.b()) {
            throw a(var3, "hi " + a(var1.e().doubleValue()), var2);
         }
      }

      List var7 = "enum".equals(var1.b()) ? c(this.q, var1.a()) : null;
      if (var7 != null) {
         for (Object var9 : var1.c()) {
            if (var9 != null && var7.stream().noneMatch(var1x -> a(var1x, var9))) {
               throw new RuleApiException(
                  400,
                  "GENERATOR_INVALID",
                  "generator.js: g.enum('"
                     + var3
                     + "') samples "
                     + io.karatelabs.ext.rules.JsLiteralWriter.a(var9)
                     + ", which schema.js does not declare "
                     + io.karatelabs.ext.rules.JsLiteralWriter.a(var7)
                     + " — schema.js declares the domain, the generator may only narrow it"
               );
            }
         }
      }

      return var2;
   }

   private static RuleApiException a(String var0, String var1, io.karatelabs.ext.rules.NumericBoundsParser.NumericBounds var2) {
      return new RuleApiException(
         400,
         "GENERATOR_INVALID",
         "generator.js: '"
            + var0
            + "' declares "
            + var1
            + ", outside the schema's "
            + a(var2)
            + " — schema.js declares the domain, the generator may only narrow it"
      );
   }

   private static String a(io.karatelabs.ext.rules.NumericBoundsParser.NumericBounds var0) {
      return (var0.c() ? "#int[" : "#number[")
         + (var0.a() == null ? "" : a(var0.a().doubleValue()))
         + ","
         + (var0.b() == null ? "" : a(var0.b().doubleValue()))
         + "]";
   }

   private static boolean a(io.karatelabs.ext.rules.NumericBoundsParser.NumericBounds var0, double var1) {
      return (var0.a() == null || var1 >= var0.a()) && (var0.b() == null || var1 <= var0.b()) && (!var0.c() || var1 == Math.rint(var1));
   }

   private static boolean a(Object var0, Object var1) {
      return var0 instanceof Number var3 && var1 instanceof Number var2 ? var3.doubleValue() == var2.doubleValue() : Objects.equals(var0, var1);
   }

   private void a(io.karatelabs.ext.rules.Generator.GeneratorPlan var1) {
      for (io.karatelabs.ext.rules.Generator.GeneratorDimension var3 : var1.a()) {
         String var4 = var3.b();
         switch (var4) {
            case "enum":
               this.a(var3);
               io.karatelabs.ext.rules.RuleInputSpaceExplorer.InputDimension var16 = new io.karatelabs.ext.rules.RuleInputSpaceExplorer.InputDimension(var3.a(), "enum", new ArrayList<>(var3.c()), io.karatelabs.ext.rules.RuleInputSpaceExplorer.InputDomainConstraint.a(c(this.q, var3.a())));
               var16.m = d(this.q, var3.a());
               this.t.add(var16);
               break;
            case "boolean":
               this.t.add(new io.karatelabs.ext.rules.RuleInputSpaceExplorer.InputDimension(var3.a(), "boolean", null));
               break;
            case "string":
               io.karatelabs.ext.rules.RuleInputSpaceExplorer.InputDimension var15 = new io.karatelabs.ext.rules.RuleInputSpaceExplorer.InputDimension(var3.a(), "string", null);

               for (Object var20 : var3.c()) {
                  var15.f.add(String.valueOf(var20));
               }

               this.t.add(var15);
               break;
            case "number":
               io.karatelabs.ext.rules.NumericBoundsParser.NumericBounds var14 = this.a(var3);
               Double var17 = var3.d() == null && var14 != null ? var14.a() : var3.d();
               Double var19 = var3.e() == null && var14 != null ? var14.b() : var3.e();
               boolean var9 = var3.g() || var14 != null && var14.c();
               io.karatelabs.ext.rules.RuleInputSpaceExplorer.InputDimension var10 = new io.karatelabs.ext.rules.RuleInputSpaceExplorer.InputDimension(var3.a(), "number", null, io.karatelabs.ext.rules.RuleInputSpaceExplorer.InputDomainConstraint.a(var17, var19, var9));
               if (var17 != null) {
                  var10.e.add(var17);
                  var10.g.add(var17);
               }

               if (var19 != null) {
                  var10.e.add(var19);
                  var10.g.add(var19);
               }

               for (Object var12 : var3.c()) {
                  if (var12 instanceof Number var13) {
                     this.a(var10, var13.doubleValue());
                  }
               }

               this.t.add(var10);
               break;
            case "cardinality":
               int var6 = ((Number)var3.c().get(0)).intValue();
               int var7 = ((Number)var3.c().get(1)).intValue();
               io.karatelabs.ext.rules.RuleInputSpaceExplorer.InputDimension var8 = new io.karatelabs.ext.rules.RuleInputSpaceExplorer.InputDimension(var3.a(), "cardinality", null, io.karatelabs.ext.rules.RuleInputSpaceExplorer.InputDomainConstraint.a(var6, var7));
               var8.j = var3.f();
               var8.h.add(var6);
               if (var6 + 1 <= var7) {
                  var8.h.add(var6 + 1);
               }

               var8.h.add(var7);
               this.t.add(var8);
         }
      }
   }

   private void a(io.karatelabs.ext.rules.RuleInputSpaceExplorer.InputDimension var1, double var2) {
      var1.g.add(var2);
      double var4 = var2 == Math.rint(var2) ? 1.0 : 0.01;
      if (var1.e.add(var2 - var4) | var1.e.add(var2) | var1.e.add(var2 + var4)) {
         this.G = true;
      }
   }

   private void e(String var1) {
      io.karatelabs.ext.rules.RuleInputSpaceExplorer.MaskedCalcSource var2 = f(var1);
      String var3 = var2.a();
      String var4 = Pattern.quote(a(var3));
      Matcher var5 = Pattern.compile(var4 + "\\.(always|sometimes)\\(\\s*['\"]([^'\"]+)['\"]").matcher(var3);

      while (var5.find()) {
         if (!var2.b()[var5.start()]) {
            String var6 = var5.group(2);
            int var7 = (int)var3.chars().limit(var5.start()).filter(var0 -> var0 == 10).count() + 1;
            io.karatelabs.ext.rules.RuleInputSpaceExplorer.PropertyExecutionStats var8 = this.D.get(var6);
            if (var8 != null) {
               this.a(var6, "duplicate property name", List.of(var8.b, var7));
            } else {
               io.karatelabs.ext.rules.RuleInputSpaceExplorer.PropertyExecutionStats var9 = new io.karatelabs.ext.rules.RuleInputSpaceExplorer.PropertyExecutionStats();
               var9.a = var5.group(1);
               var9.b = var7;
               var9.r = a(var3, var5.end());
               this.D.put(var6, var9);
            }
         }
      }
   }

   static String a(String var0) {
      Matcher var1 = Pattern.compile(
            "(?:const|let|var)\\s+execute\\s*=\\s*(?:function\\s*)?\\(\\s*([A-Za-z_$][\\w$]*)|function\\s+execute\\s*\\(\\s*([A-Za-z_$][\\w$]*)|(?:const|let|var)\\s+execute\\s*=\\s*([A-Za-z_$][\\w$]*)\\s*=>"
         )
         .matcher(var0);
      if (var1.find()) {
         for (int var2 = 1; var2 <= var1.groupCount(); var2++) {
            if (var1.group(var2) != null) {
               return var1.group(var2);
            }
         }
      }

      return "calc";
   }

   private void a(String var1, String var2, List<Integer> var3) {
      for (Map var5 : this.an) {
         if (var1.equals(var5.get("name")) && var2.equals(var5.get("error"))) {
            return;
         }
      }

      LinkedHashMap<java.lang.String, java.lang.Object> var6 = new LinkedHashMap();
      var6.put("name", var1);
      var6.put("error", var2);
      var6.put("lines", var3);
      this.an.add(var6);
   }

   private static io.karatelabs.ext.rules.RuleInputSpaceExplorer.MaskedCalcSource f(String var0) {
      char[] var1 = var0.toCharArray();
      boolean[] var2 = new boolean[var0.length()];
      int var3 = 0;
      int var4 = var0.length();

      while (var3 < var4) {
         char var5 = var0.charAt(var3);
         char var6 = var3 + 1 < var4 ? var0.charAt(var3 + 1) : 0;
         if (var5 != '\'' && var5 != '"' && var5 != '`') {
            if (var5 == '/' && var6 == '/') {
               while (var3 < var4 && var0.charAt(var3) != '\n') {
                  var1[var3] = ' ';
                  var3++;
               }
               continue;
            }

            if (var5 == '/' && var6 == '*') {
               while (var3 < var4 && (var0.charAt(var3) != '*' || var3 + 1 >= var4 || var0.charAt(var3 + 1) != '/')) {
                  var1[var3] = (char)(var0.charAt(var3) == '\n' ? 10 : 32);
                  var3++;
               }

               if (var3 + 1 < var4) {
                  var1[var3] = ' ';
                  var1[var3 + 1] = ' ';
                  var3++;
               }
            }
         } else {
            var2[var3] = true;
            var3++;

            for (; var3 < var4 && var0.charAt(var3) != var5; var3++) {
               var2[var3] = true;
               if (var0.charAt(var3) == '\\' && var3 + 1 < var4) {
                  var2[var3 + 1] = true;
                  var3++;
               }
            }

            if (var3 < var4) {
               var2[var3] = true;
            }
         }

         var3++;
      }

      return new io.karatelabs.ext.rules.RuleInputSpaceExplorer.MaskedCalcSource(new String(var1), var2);
   }

   private static String a(String var0, int var1) {
      int var2 = 1;
      int var3 = var1;

      ArrayList var4;
      for (var4 = new ArrayList(); var3 < var0.length() && var2 > 0; var3++) {
         char var5 = var0.charAt(var3);
         if (var5 != '\'' && var5 != '"' && var5 != '`') {
            if (var5 == '(' || var5 == '[' || var5 == '{') {
               var2++;
            } else if (var5 == ')' || var5 == ']' || var5 == '}') {
               var2--;
            } else if (var5 == ',' && var2 == 1) {
               var4.add(var3);
            }
         } else {
            var3++;

            while (var3 < var0.length() && var0.charAt(var3) != var5) {
               var3 += var0.charAt(var3) == '\\' ? 2 : 1;
            }
         }
      }

      return var4.size() < 2 ? null : g(var0.substring((Integer)var4.get(1) + 1, Math.max((Integer)var4.get(1) + 1, var3 - 1)));
   }

   private static String g(String var0) {
      int var1 = 0;
      int var2 = var0.length();
      char var3 = 0;

      while (var1 < var2) {
         char var4 = var0.charAt(var1);
         if (var4 == '\'' || var4 == '"' || var4 == '`') {
            var1++;

            while (var1 < var2 && var0.charAt(var1) != var4) {
               var1 += var0.charAt(var1) == '\\' ? 2 : 1;
            }

            var1++;
            var3 = 'v';
         } else if (Character.isWhitespace(var4)) {
            var1++;
         } else {
            if ((var3 == 0 || var3 == '{' || var3 == ',')
               && var0.startsWith("req", var1)
               && (var1 + 3 >= var2 || !Character.isJavaIdentifierPart(var0.charAt(var1 + 3)))) {
               int var5 = var1 + 3;

               while (var5 < var2 && Character.isWhitespace(var0.charAt(var5))) {
                  var5++;
               }

               if (var5 < var2 && var0.charAt(var5) == ':') {
                  var5++;

                  while (var5 < var2 && Character.isWhitespace(var0.charAt(var5))) {
                     var5++;
                  }

                  if (var5 < var2 && (var0.charAt(var5) == '\'' || var0.charAt(var5) == '"')) {
                     char var6 = var0.charAt(var5);
                     StringBuilder var7 = new StringBuilder();

                     for (int var8 = var5 + 1; var8 < var2 && var0.charAt(var8) != var6; var8++) {
                        if (var0.charAt(var8) == '\\' && var8 + 1 < var2) {
                           var8++;
                        }

                        var7.append(var0.charAt(var8));
                     }

                     return var7.isEmpty() ? null : var7.toString();
                  }

                  return null;
               }
            }

            var3 = var4;
            var1++;
         }
      }

      return null;
   }

   private static List<String> h(String var0) {
      ArrayList<java.lang.String> var1 = new ArrayList();
      Matcher var2 = Pattern.compile(Pattern.quote(a(var0)) + "\\.outcome\\(\\s*['\"]([^'\"]*)['\"]").matcher(var0);

      while (var2.find()) {
         if (!var1.contains(var2.group(1))) {
            var1.add(var2.group(1));
         }
      }

      return var1;
   }

   private static List<Node> i(String var0) {
      return a(Engine.parse(var0));
   }

   public static List<Node> a(Node var0) {
      ArrayList<io.karatelabs.parser.Node> var1 = new ArrayList();

      for (NodeType var5 : i) {
         for (Node var7 : var0.findAll(var5)) {
            if (!b(var7)) {
               var1.add(var7);
            }
         }
      }

      var1.sort((var0x, var1x) -> Integer.compare(var0x.getFirstToken().pos, var1x.getFirstToken().pos));
      return var1;
   }

   static Map<Integer, String> b(String var0) {
      return a(i(var0));
   }

   public static Map<String, Integer> a(String var0, List<Map<String, Object>> var1) {
      return a(var0, Map.of(), null, null, var1);
   }

   public static Map<String, Integer> a(String var0, Map<String, Object> var1, Object var2, String var3, List<Map<String, Object>> var4) {
      var var5 = a(i(var0));
      LinkedHashMap<java.lang.String, java.lang.Integer> var6 = new LinkedHashMap();

      for (String var8 : (Iterable<String>) (Iterable<?>) (var5.values())) {
         var6.put(var8 + ":true", 0);
         var6.put(var8 + ":false", 0);
      }

      RuleInputSpaceExplorer var16 = var2 == null ? null : new RuleInputSpaceExplorer(var0, var1, var2, var3, null);
      if (var4 != null) {
         for (Map var9 : var4) {
            if (var16 == null || var16.c(var9)) {
               io.karatelabs.ext.rules.RuleInputSpaceExplorer.CalcEventCollector var10 = new io.karatelabs.ext.rules.RuleInputSpaceExplorer.CalcEventCollector();

               try {
                  io.karatelabs.ext.rules.CalcExecutor.a(var0, var1, var9, var10);
               } catch (RuntimeException var15) {
               }

               LinkedHashSet<java.lang.String> var11 = new LinkedHashSet();

               for (Entry var13 : var10.a) {
                  String var14 = (String)var5.get(var13.getKey());
                  if (var14 != null) {
                     var11.add(var14 + ":" + var13.getValue());
                  }
               }

               for (String var19 : (Iterable<String>) (Iterable<?>) (var11)) {
                  var6.merge(var19, 1, Integer::sum);
               }
            }
         }
      }

      return var6;
   }

   public static Map<Integer, String> a(List<Node> var0) {
      LinkedHashMap<java.lang.Integer, java.lang.String> var1 = new LinkedHashMap();
      LinkedHashMap<java.lang.Integer, java.lang.Integer> var2 = new LinkedHashMap();

      for (Node var4 : var0) {
         Token var5 = var4.getFirstToken();
         int var6 = var5.line + 1;
         int var7 = var2.merge(var6, 1, Integer::sum) - 1;
         var1.put(var5.pos, "L" + var6 + "#" + var7);
      }

      return var1;
   }

   private void a() {
      var var1 = i(this.m);
      Map<java.lang.Integer, java.lang.String> var2 = a(var1);

      for (Node var4 : (Iterable<Node>) (Iterable<?>) (var1)) {
         Token var5 = var4.getFirstToken();
         String var6 = var4.getText().replaceAll("\\s+", " ");
         if (var6.length() > 60) {
            var6 = var6.substring(0, 60) + "...";
         }

         io.karatelabs.ext.rules.RuleInputSpaceExplorer.SchemaSite var7 = new io.karatelabs.ext.rules.RuleInputSpaceExplorer.SchemaSite((String)var2.get(var5.pos), var5.pos, var5.line + 1, var6);
         this.r.add(var7);
         this.s.put(var5.pos, var7);
         this.u.put(var5.pos, new TreeSet<>());
         this.ah.put(var5.pos, new TreeSet<>());
      }
   }

   private void e(Object var1, String var2) {
      if (var1 instanceof Map var3) {
         for (Entry var5 : (Iterable<Entry>) (Iterable<?>) (var3.entrySet())) {
            String var6 = var2.isEmpty() ? (String)var5.getKey() : var2 + "." + var5.getKey();
            Object var7 = var5.getValue();
            if (var7 instanceof Map) {
               this.e(var7, var6);
            } else if (var7 instanceof List var8) {
               if (var8.stream().anyMatch(var0 -> var0 instanceof Map || var0 instanceof List)) {
                  this.t.add(new io.karatelabs.ext.rules.RuleInputSpaceExplorer.InputDimension(var6, "array", null));
               } else {
                  this.t.add(new io.karatelabs.ext.rules.RuleInputSpaceExplorer.InputDimension(var6, "enum", List.copyOf(var8), io.karatelabs.ext.rules.RuleInputSpaceExplorer.InputDomainConstraint.a(List.copyOf(var8))));
               }
            } else {
               io.karatelabs.ext.rules.NumericBoundsParser.NumericBounds var11 = a(var6, var7);
               if (var11 instanceof io.karatelabs.ext.rules.NumericBoundsParser.NumericBounds) {
                  io.karatelabs.ext.rules.RuleInputSpaceExplorer.InputDimension var12 = new io.karatelabs.ext.rules.RuleInputSpaceExplorer.InputDimension(var6, "number", null, io.karatelabs.ext.rules.RuleInputSpaceExplorer.InputDomainConstraint.a(var11.a(), var11.b(), var11.c()));
                  a(var12, var11.a(), var11.b());
                  this.t.add(var12);
               } else if (f(var7, "#number")) {
                  io.karatelabs.ext.rules.RuleInputSpaceExplorer.InputDimension var13 = new io.karatelabs.ext.rules.RuleInputSpaceExplorer.InputDimension(var6, "number", null);
                  var13.e.add(0.0);
                  this.t.add(var13);
               } else if (f(var7, "#boolean")) {
                  this.t.add(new io.karatelabs.ext.rules.RuleInputSpaceExplorer.InputDimension(var6, "boolean", null));
               } else if (f(var7, "#date")) {
                  this.t.add(new io.karatelabs.ext.rules.RuleInputSpaceExplorer.InputDimension(var6, "date", null));
               } else if (f(var7, "#string")) {
                  io.karatelabs.ext.rules.RuleInputSpaceExplorer.InputDimension var14 = new io.karatelabs.ext.rules.RuleInputSpaceExplorer.InputDimension(var6, "string", null);
                  var14.f.add("x");
                  this.t.add(var14);
               } else {
                  if (var7 instanceof String var10 && var10.startsWith("#") && !j(var10)) {
                     throw new RuleApiException(
                        400,
                        "SCHEMA_INVALID",
                        "schema.js: '"
                           + var6
                           + "' declares "
                           + var10
                           + ", which is not a shape this engine reads — use '#string', '#number', '#boolean', '#date', a range ('#int[0,6]') or a literal list"
                     );
                  }

                  io.karatelabs.ext.rules.RuleInputSpaceExplorer.InputDimension var15 = new io.karatelabs.ext.rules.RuleInputSpaceExplorer.InputDimension(var6, "string", null);
                  var15.f.add("x");
                  this.t.add(var15);
               }
            }
         }
      } else {
         throw new IllegalArgumentException("schema must be an object, got: " + var1);
      }
   }

   private static boolean f(Object var0, String var1) {
      return var1.equals(var0) || ("#" + var1).equals(var0);
   }

   private static boolean j(String var0) {
      String var1 = var0.replaceFirst("^#+", "");
      if (!var1.isEmpty() && Character.isLetter(var1.charAt(0))) {
         int var2 = 0;

         while (var2 < var1.length() && Character.isLetter(var1.charAt(var2))) {
            var2++;
         }

         return ao.contains(var1.substring(0, var2)) && (var2 == var1.length() || var1.charAt(var2) == ' ');
      } else {
         return true;
      }
   }

   private static io.karatelabs.ext.rules.NumericBoundsParser.NumericBounds a(String var0, Object var1) {
      if (var1 instanceof String var2) {
         try {
            return io.karatelabs.ext.rules.NumericBoundsParser.a(var2);
         } catch (io.karatelabs.ext.rules.NumericBoundsParser.NumericBoundsException var4) {
            throw new RuleApiException(
               400,
               "SCHEMA_INVALID",
               "schema.js: '"
                  + var0
                  + "' declares "
                  + var2
                  + (
                     var4.a.equals("notExact")
                        ? ", whose integer bound is beyond 2^53 — no double counts that far one by one"
                        : ", which is not a readable range — write #int[lo,hi] or #number[lo,hi]; either side may be left empty"
                  )
            );
         }
      } else {
         return null;
      }
   }

   private static io.karatelabs.ext.rules.NumericBoundsParser.NumericBounds g(Object var0, String var1) {
      return a(var1, b(var0, var1));
   }

   private static void a(io.karatelabs.ext.rules.RuleInputSpaceExplorer.InputDimension var0, Double var1, Double var2) {
      if (var1 != null) {
         var0.e.add(var1);
         var0.g.add(var1);
      }

      if (var2 != null) {
         var0.e.add(var2);
         var0.g.add(var2);
      }

      if (var1 == null && var2 == null) {
         var0.e.add(0.0);
      }
   }

   private static Object a(Object var0) {
      if (!(var0 instanceof Map var1)) {
         if (var0 instanceof List var6) {
            Object var7 = var6.getFirst();
            if (!(var7 instanceof Map) && !(var7 instanceof List)) {
               return var7;
            } else {
               ArrayList<java.lang.Object> var8 = new ArrayList();
               var8.add(a(var7));
               return var8;
            }
         } else {
            io.karatelabs.ext.rules.NumericBoundsParser.NumericBounds var5 = a("", var0);
            if (var5 != null) {
               return a(var5.a() != null ? var5.a() : (var5.b() != null ? Math.min(0.0, var5.b()) : 0.0));
            } else if (f(var0, "#number")) {
               return 0;
            } else if (f(var0, "#boolean")) {
               return false;
            } else {
               return f(var0, "#date") ? "2026-01-01" : "x";
            }
         }
      } else {
         LinkedHashMap<java.lang.String, java.lang.Object> var2 = new LinkedHashMap();

         for (Entry var4 : (Iterable<Entry>) (Iterable<?>) (var1.entrySet())) {
            var2.put((String)var4.getKey(), a(var4.getValue()));
         }

         return var2;
      }
   }

   private static Object b(Object var0, Object var1) {
      if (!(var0 instanceof Map var2)) {
         return var1 != null ? var1 : a(var0);
      } else {
         Map<java.lang.String, Object> var3 = var1 instanceof Map ? (Map)var1 : Map.of();
         LinkedHashMap<java.lang.String, java.lang.Object> var4 = new LinkedHashMap();

         for (Entry var6 : (Iterable<Entry>) (Iterable<?>) (var2.entrySet())) {
            String var7 = (String)var6.getKey();
            var4.put(var7, b(var6.getValue(), var3.get(var7)));
         }

         return var4;
      }
   }

   private Map<String, Object> b() {
      return b(io.karatelabs.ext.common.SandboxExecutor.a(this.z));
   }

   private static List<Object> a(io.karatelabs.ext.rules.RuleInputSpaceExplorer.InputDimension var0) {
      ArrayList<java.lang.Object> var1 = new ArrayList();
      String var2 = var0.b;
      switch (var2) {
         case "enum":
            if (var0.c != null) {
               var1.addAll(var0.c);
            }
            break;
         case "boolean":
            var1.addAll(List.of(false, true));
            break;
         case "cardinality":
            Integer var6 = var0.d.e();
            Integer var7 = var0.d.f();
            if (var6 != null) {
               var1.add(var6);
            }

            if (var7 != null && !var7.equals(var6)) {
               var1.add(var7);
            }
            break;
         default:
            Double var4 = var0.a();
            Double var5 = var0.b();
            if (var4 != null && var5 != null) {
               var1.add(a(var0.d.c() ? (long)var4.doubleValue() : var4));
               if (!var4.equals(var5)) {
                  var1.add(a(var0.d.c() ? (long)var5.doubleValue() : var5));
               }
            } else if (var0.c != null && !var0.c.isEmpty()) {
               var1.addAll(var0.c);
            }
      }

      return var1;
   }

   private void c() {
      if (this.H != null && !this.T && !this.t.isEmpty() && this.J > 0) {
         ArrayList<java.util.List<java.lang.Object>> var1 = new ArrayList();
         long var2 = 1L;

         for (io.karatelabs.ext.rules.RuleInputSpaceExplorer.InputDimension var5 : this.t) {
            var var6 = a(var5);
            var1.add(var6);
            if (!var6.isEmpty()) {
               var2 = var2 > Long.MAX_VALUE / var6.size() ? Long.MAX_VALUE : var2 * var6.size();
            }
         }

         ArrayList<java.lang.Integer> var7 = new ArrayList();

         for (int var8 = 0; var8 < var1.size(); var8++) {
            if (((List)var1.get(var8)).size() > 1) {
               var7.add(var8);
            }
         }

         this.Q = var2;

         for (int var10 : a(var7.size())) {
            if (!this.a(var1, var7, var10)) {
               this.R = true;
               return;
            }
         }
      }
   }

   static List<Integer> a(int var0) {
      ArrayList<java.lang.Integer> var1 = new ArrayList();
      int var2 = 0;

      for (int var3 = var0; var2 <= var3; var3--) {
         var1.add(var2);
         if (var3 != var2) {
            var1.add(var3);
         }

         var2++;
      }

      return var1;
   }

   private boolean a(List<List<Object>> var1, List<Integer> var2, int var3) {
      int[] var4 = new int[var3];
      int var5 = 0;

      while (var5 < var3) {
         var4[var5] = var5++;
      }

      while (this.a(var1, var2, var4)) {
         var5 = var3 - 1;

         while (var5 >= 0 && var4[var5] == var2.size() - var3 + var5) {
            var5--;
         }

         if (var5 < 0) {
            return true;
         }

         var4[var5]++;

         for (int var6 = var5 + 1; var6 < var3; var6++) {
            var4[var6] = var4[var6 - 1] + 1;
         }
      }

      return false;
   }

   private boolean a(List<List<Object>> var1, List<Integer> var2, int[] var3) {
      int var4 = var3.length;
      int[] var5 = new int[var4];
      Arrays.fill(var5, 1);

      while (this.M < this.J && ++this.N <= this.J * 8L) {
         var var6 = this.b();

         for (int var7 = 0; var7 < var1.size(); var7++) {
            if (!((List)var1.get(var7)).isEmpty()) {
               int var8 = 0;

               for (int var9 = 0; var9 < var4; var9++) {
                  if ((Integer)var2.get(var3[var9]) == var7) {
                     var8 = var5[var9];
                  }
               }

               a(var6, this.t.get(var7), ((List)var1.get(var7)).get(var8));
            }
         }

         if (!this.P.add(Json.stringifyStrict(var6))) {
            this.O++;
         } else {
            this.a(var6);
         }

         int var10;
         for (var10 = var4 - 1; var10 >= 0 && ++var5[var10] == ((List)var1.get((Integer)var2.get(var3[var10]))).size(); var10--) {
            var5[var10] = 1;
         }

         if (var10 < 0) {
            return true;
         }
      }

      return false;
   }

   private void a(Map<String, Object> var1) {
      this.M++;
      var var2 = this.d(var1);
      if (!var2.isEmpty()) {
         this.ak.addAll(var2);
      } else {
         io.karatelabs.ext.rules.CalcExecutor.CalcExecutionResult var3;
         try {
            var3 = io.karatelabs.ext.rules.CalcExecutor.a(this.m, this.n, var1, new io.karatelabs.ext.rules.RuleInputSpaceExplorer.CalcEventCollector());
         } catch (RuntimeException var12) {
            this.S++;
            return;
         }

         HashSet<java.lang.String> var4 = new HashSet();
         HashSet<java.lang.String> var5 = new HashSet();

         for (Object[] var7 : var3.i()) {
            String var8 = (String)var7[0];
            boolean var9 = (Boolean)var7[2];
            if ("always".equals(var7[3])) {
               io.karatelabs.ext.rules.RuleInputSpaceExplorer.PropertyExecutionStats var10 = this.D.get(var8);
               if (var10 != null) {
                  if (var4.add(var8)) {
                     var10.n++;
                  }

                  if (!var9 && var5.add(var8)) {
                     var10.o++;
                     String var11 = Json.stringifyStrict(var1);
                     if (var10.p == null || b(var11, var10.p)) {
                        var10.p = var11;
                        var10.q = b(io.karatelabs.ext.common.SandboxExecutor.a(var1));
                     }
                  }
               }
            }
         }
      }
   }

   private Map<String, Object> d() {
      LinkedHashMap<java.lang.String, java.lang.Object> var1 = new LinkedHashMap();
      var1.put("rows", this.M);
      var1.put("corners", this.Q);
      var1.put("maxSearch", this.J);
      var1.put("truncated", this.R);
      if (this.O > 0) {
         var1.put("duplicates", this.O);
      }

      if (this.S > 0) {
         var1.put("failures", this.S);
      }

      return var1;
   }

   private io.karatelabs.ext.rules.RuleInputSpaceExplorer.InputSpaceExplorationResult a(Object var1, List<Map<String, Object>> var2, String var3) {
      ArrayList<java.util.Map<java.lang.String,java.lang.Object>> var4 = new ArrayList();
      var4.add(this.b());

      for (int var5 = 0; var5 < var2.size(); var5++) {
         var var6 = b(b(var1, var2.get(var5)));
         var4.add(var6);
         var var7 = this.d(var6);
         if (!var7.isEmpty()) {
            LinkedHashMap<java.lang.String, java.lang.Object> var8 = new LinkedHashMap();
            Object var9 = ((Map)var2.get(var5)).get("_id");
            if (var9 != null) {
               var8.put("id", String.valueOf(var9));
            }

            var8.put("index", var5);
            var8.put("offending", var7);
            var8.put("outOfDomain", d(var7));
            this.aj.add(var8);
         }
      }

      this.a(var4, false);

      for (Entry var14 : this.ah.entrySet()) {
         this.v.put((Integer)var14.getKey(), new TreeSet<>((Collection<? extends Boolean>)var14.getValue()));
      }

      for (int var12 = 1; var12 <= 8 && this.E < 500; var12++) {
         this.F = var12;
         this.G = false;
         boolean var15 = this.a(this.e(), true);
         if (!var15 && !this.G) {
            break;
         }
      }

      List var13 = null;
      boolean var16 = "product".equals(var3);
      if ((this.H != null || var16) && this.g() && this.h() <= (this.H != null ? this.H : 50000L) && (var16 || this.x.isEmpty() && this.aa == 0)) {
         var13 = this.i();
         this.U = true;

         for (Map var19 : (Iterable<Map>) (Iterable<?>) (var13)) {
            this.b(var19);
         }

         this.U = false;
         this.T = true;
      }

      if ("pairwise".equals(var3)) {
         this.c(this.e());
      } else if ("boundary".equals(var3)) {
         this.c(this.f());
      } else if (var16) {
         this.c(var13 == null ? List.of() : var13);
      } else if ("cells".equals(var3)) {
         var var18 = this.w.stream().map(var0 -> b(var0.get("input"))).toList();
         LinkedHashMap<java.lang.String, java.util.function.Supplier<java.util.List<java.util.Map<java.lang.String,java.lang.Object>>>> var20 = new LinkedHashMap();
         var20.put("coverage", () -> var18);
         var20.put("pairwise", this::e);
         var20.put("boundary", this::f);
         long var21 = this.H != null ? this.H : 50000L;
         if (this.g() && this.h() <= var21) {
            var20.put("product", this::i);
         }

         this.b(this.K.apply(var20));
      }

      return this.q();
   }

   private void b(List<Map<String, Object>> var1) {
      this.w.clear();

      for (Map var3 : var1) {
         LinkedHashMap var4 = new LinkedHashMap(var3);
         this.a(var4, b(var3.get("input")));
         this.w.add(var4);
      }
   }

   private void c(List<Map<String, Object>> var1) {
      this.w.clear();

      for (Map var3 : var1) {
         LinkedHashMap<String, Object> var4 = new LinkedHashMap();
         var4.put("input", var3);
         this.a(var4, var3);
         this.w.add(var4);
      }
   }

   private void a(Map<String, Object> var1, Map<String, Object> var2) {
      var var3 = this.d(var2);
      if (!var3.isEmpty()) {
         var1.put("domain", "out");
         var1.put("outOfDomain", d(var3));
      }
   }

   private static List<String> d(List<Map<String, Object>> var0) {
      return var0.stream().map(var0x -> String.valueOf(var0x.get("axis"))).toList();
   }

   private static Map<String, Object> b(Object var0) {
      return (Map<String, Object>)var0;
   }

   private boolean a(List<Map<String, Object>> var1, boolean var2) {
      boolean var3 = false;

      for (Map var5 : var1) {
         if (this.E >= 500) {
            break;
         }

         List var6 = this.b(var5);
         if (!var6.isEmpty()) {
            var3 = true;
            if (var2) {
               LinkedHashMap<java.lang.String, java.lang.Object> var7 = new LinkedHashMap();
               var7.put("input", var5);
               var7.put("newlyCovered", var6);
               this.a(var7, var5);
               this.w.add(var7);
            }
         }
      }

      return var3;
   }

   private List<String> b(Map<String, Object> var1) {
      this.Y = this.c(var1);
      if (this.U) {
         this.V++;
         this.ai = this.ai | !this.Y;
      } else {
         this.E++;
      }

      if (!this.Y) {
         this.Z++;
      }

      io.karatelabs.ext.rules.RuleInputSpaceExplorer.CalcEventCollector var2 = new io.karatelabs.ext.rules.RuleInputSpaceExplorer.CalcEventCollector();
      io.karatelabs.ext.rules.CalcExecutor.CalcExecutionResult var3 = null;
      int var4 = -1;

      try {
         var3 = io.karatelabs.ext.rules.CalcExecutor.a(this.m, this.n, var1, var2);
      } catch (io.karatelabs.ext.rules.CalcExecutor.CalcExecutionException var9) {
         if (this.Y) {
            var4 = var9.b;
            this.ab++;
            if (this.U) {
               this.ac++;
            }
         } else {
            this.W++;
            this.ae++;
            if (this.ag++ < 10) {
               LinkedHashMap<java.lang.String, java.lang.Object> var11 = new LinkedHashMap();
               var11.put("input", var1);
               var11.put("error", var9.getMessage() == null ? var9.getClass().getSimpleName() : var9.getMessage());
               var11.put("domain", "out");
               this.y.add(var11);
            }
         }
      } catch (RuntimeException var10) {
         this.W++;
         boolean var6;
         if (this.Y) {
            this.aa++;
            var6 = this.af++ < 10;
         } else {
            this.ae++;
            var6 = this.ag++ < 10;
         }

         if (var6) {
            LinkedHashMap<java.lang.String, java.lang.Object> var7 = new LinkedHashMap();
            var7.put("input", var1);
            var7.put("error", var10.getMessage() == null ? var10.getClass().getSimpleName() : var10.getMessage());
            if (!this.Y) {
               var7.put("domain", "out");
            }

            this.y.add(var7);
         }
      }

      ArrayList<java.lang.String> var5 = new ArrayList();

      for (Entry var14 : var2.a) {
         io.karatelabs.ext.rules.RuleInputSpaceExplorer.SchemaSite var8 = this.s.get(var14.getKey());
         if (var8 != null) {
            if (this.u.get(var14.getKey()).add((Boolean)var14.getValue())) {
               var5.add(var8.a + ":" + var14.getValue());
            }

            if (this.Y) {
               this.ah.get(var14.getKey()).add((Boolean)var14.getValue());
            }
         }
      }

      for (Event var15 : var2.b) {
         this.a(var15);
      }

      if (var4 >= 0 && this.U) {
         this.a(var1, var2, var4);
      } else if (!this.U && this.al != null && this.al.a()) {
         this.am.add(this.a(var2));
      }

      if (var3 != null) {
         this.a(var3, var1);
      }

      return var5;
   }

   private String a(io.karatelabs.ext.rules.RuleInputSpaceExplorer.CalcEventCollector var1) {
      LinkedHashMap<String, Set<Boolean>> var2 = new LinkedHashMap();

      for (Entry var4 : var1.a) {
         io.karatelabs.ext.rules.RuleInputSpaceExplorer.SchemaSite var5 = this.s.get(var4.getKey());
         if (var5 != null) {
            var2.computeIfAbsent(var5.a, var0 -> new TreeSet<>()).add((Boolean)var4.getValue());
         }
      }

      var var6 = this.r.stream().map(var0 -> var0.a).sorted(RuleCoverageMatrixBuilder::a).toList();
      return RuleInputSpaceCertificateBuilder.a(RuleCoverageMatrixBuilder.a(var6, var2));
   }

   private void a(Map<String, Object> var1, io.karatelabs.ext.rules.RuleInputSpaceExplorer.CalcEventCollector var2, int var3) {
      String var4 = this.a(var2);
      Map<java.lang.String, java.lang.Object> var5 = this.ad.computeIfAbsent(var3 + "|" + var4, var2x -> {
         LinkedHashMap<java.lang.String, java.lang.Object> var3x = new LinkedHashMap();
         var3x.put("hull", new LinkedHashMap());
         var3x.put("line", var3);
         var3x.put("signature", var4);
         var3x.put("rows", 0L);
         return var3x;
      });
      var5.put("rows", (Long)var5.get("rows") + 1L);
      Map<java.lang.String, java.util.LinkedHashSet> var6 = (Map)var5.get("hull");

      for (io.karatelabs.ext.rules.RuleInputSpaceExplorer.InputDimension var8 : this.t) {
         var6.computeIfAbsent(var8.a, var0 -> new LinkedHashSet<>()).add(RuleCoverageMatrixBuilder.a(var1, var8.a));
      }
   }

   private void a(io.karatelabs.ext.rules.CalcExecutor.CalcExecutionResult var1, Map<String, Object> var2) {
      HashSet<java.lang.String> var3 = this.U ? new HashSet() : null;

      for (Object[] var5 : var1.e()) {
         this.b((String)var5[0], (List<?>)var5[1]);
      }

      this.A.putAll(var1.f());
      if (var1.h()) {
         this.B.add(var1.g());
      }

      for (Object[] var16 : var1.i()) {
         String var6 = (String)var16[0];
         int var7 = (Integer)var16[1];
         boolean var8 = (Boolean)var16[2];
         String var9 = (String)var16[3];
         String var10 = var16.length > 4 && var16[4] != null ? String.valueOf(var16[4]) : null;
         io.karatelabs.ext.rules.RuleInputSpaceExplorer.PropertyExecutionStats var11 = this.D.get(var6);
         if (var11 == null) {
            this.a(var6, "dynamic property name — declare with a string literal", List.of(var7));
         } else {
            if (this.Y) {
               var11.c++;
               if (this.U && var3.add(var6)) {
                  var11.d++;
               }
            } else {
               var11.i++;
            }

            if (var10 != null && var11.r == null) {
               this.a(var6, "req not statically derivable — declare {req: '<literal>'} inline", List.of(var11.b));
            } else if (var10 != null && !var11.r.equals(var10)) {
               this.a(var6, "req conflict: '" + var11.r + "' vs '" + var10 + "'", List.of(var11.b, var7));
            } else if (var10 == null && var11.r != null) {
               this.a(var6, "req conflict: statically '" + var11.r + "' but none at runtime", List.of(var11.b));
            }

            boolean var12 = "sometimes".equals(var9);
            boolean var13 = var12 == var8;
            if (!var8 && !var12) {
               if (this.Y) {
                  var11.e++;
               } else {
                  var11.j++;
               }
            }

            if (var8 && var12) {
               if (this.Y) {
                  var11.f++;
               } else {
                  var11.k++;
               }
            }

            if (var13) {
               String var14 = Json.stringifyStrict(var2);
               if (this.Y) {
                  if (b(var14, var11.g)) {
                     var11.g = var14;
                     var11.h = b(io.karatelabs.ext.common.SandboxExecutor.a(var2));
                  }
               } else if (b(var14, var11.l)) {
                  var11.l = var14;
                  var11.m = b(io.karatelabs.ext.common.SandboxExecutor.a(var2));
               }
            }
         }
      }
   }

   private static boolean b(String var0, String var1) {
      return var1 == null || var0.length() < var1.length() || var0.length() == var1.length() && var0.compareTo(var1) < 0;
   }

   private void b(String var1, List<?> var2) {
      if (var1 != null && var2 != null) {
         io.karatelabs.ext.rules.RuleInputSpaceExplorer.InputDimension var3 = this.t.stream().filter(var1x -> var1x.a.equals(var1)).findFirst().orElse(null);
         boolean var4 = !var2.isEmpty() && var2.stream().allMatch(var0 -> var0 instanceof Number);
         if (var3 == null) {
            var3 = new io.karatelabs.ext.rules.RuleInputSpaceExplorer.InputDimension(var1, var4 ? "number" : "string", null);
            this.t.add(var3);
            this.t.sort((var0, var1x) -> var0.a.compareTo(var1x.a));
            this.G = true;
         }

         for (Object var6 : var2) {
            if ("number".equals(var3.b) && var6 instanceof Number var7) {
               this.a(var3, var7.doubleValue());
            } else if ("string".equals(var3.b)) {
               String var8 = String.valueOf(var6);
               if ((!var3.l || !var3.d().equals(var8)) && var3.f.add(var8)) {
                  this.G = true;
               }
            }
         }
      }
   }

   private void a(Event var1) {
      Node var2 = var1.node;
      Object[] var3 = (Object[])var1.value;
      String var4 = String.valueOf(var3[1]);
      String var5 = var2.get(0).getText();
      String var6 = var2.get(2).getText();
      boolean var7 = false;

      for (io.karatelabs.ext.rules.RuleInputSpaceExplorer.InputDimension var9 : this.t) {
         Object var10 = null;
         String var11 = null;
         if (this.a(var5, var9)) {
            var10 = var3[2];
            var11 = var6;
         } else if (this.a(var6, var9)) {
            var10 = var3[0];
            var11 = var5;
         }

         if (var10 != null) {
            var7 = true;
            int var12 = var2.getFirstToken().line + 1;

            for (io.karatelabs.ext.rules.RuleInputSpaceExplorer.SchemaSite var14 : this.r) {
               if (var14.c == var12) {
                  var9.i.add(var14.a);
               }
            }

            if ("number".equals(var9.b) && var10 instanceof Number var15) {
               this.a(var9, var15.doubleValue());
            } else if (var9.e() && var10 instanceof String var16) {
               if (!this.d(var11, var16) && var9.f.add(var16)) {
                  this.G = true;
               }

               if (h.contains(var4) && !var9.l) {
                  var9.l = true;
                  this.G = true;
               }
            } else if ("date".equals(var9.b) || "array".equals(var9.b)) {
               this.a(var2, var3, var9.b + "-boundary-unscoped");
            }
         }
      }

      if (!var7) {
         this.a(var2, var3, "no-input-attribution");
      }
   }

   private static boolean b(Node var0) {
      for (Node var1 = var0.getParent(); var1 != null; var1 = var1.getParent()) {
         if (var1.type == NodeType.FN_CALL_EXPR && j.contains(var1.get(0).getText().replaceAll("\\s+", ""))) {
            return true;
         }
      }

      return false;
   }

   private static boolean c(String var0, String var1) {
      for (int var2 = var0.indexOf(var1); var2 >= 0; var2 = var0.indexOf(var1, var2 + 1)) {
         int var3 = var2 + var1.length();
         if (var3 >= var0.length() || !a(var0.charAt(var3))) {
            return true;
         }
      }

      return false;
   }

   private static boolean a(char var0) {
      return Character.isLetterOrDigit(var0) || var0 == '_' || var0 == '$' || var0 == '.' || var0 == '[';
   }

   private boolean d(String var1, String var2) {
      if (var1 != null && var1.contains("input.")) {
         for (io.karatelabs.ext.rules.RuleInputSpaceExplorer.InputDimension var4 : this.t) {
            if (var4.l && var4.d().equals(var2) && c(var1, "input." + var4.a)) {
               return true;
            }
         }

         return false;
      } else {
         return false;
      }
   }

   private void a(Node var1, Object[] var2, String var3) {
      if (!b(var1)) {
         Token var4 = var1.getFirstToken();
         String var5 = var2[0] + " " + var2[1] + " " + var2[2];
         String var6 = "L" + (var4.line + 1) + ":" + var1.getText() + ":" + var5;
         this.x.computeIfAbsent(var6, var4x -> {
            LinkedHashMap<String, Object> var5x = new LinkedHashMap();
            var5x.put("site", "L" + (var4.line + 1));
            var5x.put("source", var1.getText());
            var5x.put("reason", var3);
            var5x.put("observed", var5);
            return var5x;
         });
      }
   }

   static Object a(double var0) {
      return var0 == Math.rint(var0) && Math.abs(var0) < 2.147483647E9 ? (int)var0 : var0;
   }

   private List<Map<String, Object>> e() {
      var var1 = this.t.stream().filter(var0 -> !var0.f().isEmpty()).toList();
      var var2 = var1.stream().map(io.karatelabs.ext.rules.RuleInputSpaceExplorer.InputDimension::f).toList();
      int var3 = var1.size();
      int var4 = Math.max(1, Math.min(this.L, var3));
      var var5 = a(var3, var4);
      LinkedHashSet var6 = new LinkedHashSet();

      for (List var8 : (Iterable<List>) (Iterable<?>) (var5)) {
         a(var6, var8, var2, new ArrayList<>());
      }

      ArrayList<java.util.Map<java.lang.String,java.lang.Object>> var19 = new ArrayList();

      while (!var6.isEmpty()) {
         io.karatelabs.ext.rules.RuleInputSpaceExplorer.InputVectorKey var20 = (io.karatelabs.ext.rules.RuleInputSpaceExplorer.InputVectorKey)var6.iterator().next();
         int[] var9 = new int[var3];
         Arrays.fill(var9, -1);

         for (int var10 = 0; var10 < var4; var10++) {
            var9[var20.a().get(var10)] = var20.b().get(var10);
         }

         for (int var21 = 0; var21 < var3; var21++) {
            if (var9[var21] < 0) {
               ArrayList<java.lang.Integer> var11 = new ArrayList();

               for (int var12 = 0; var12 < var3; var12++) {
                  if (var9[var12] >= 0) {
                     var11.add(var12);
                  }
               }

               var var24 = a(var11.size(), var4 - 1);
               int var13 = 0;
               int var14 = -1;

               for (int var15 = 0; var15 < ((List)var2.get(var21)).size(); var15++) {
                  int var16 = 0;

                  for (List var18 : (Iterable<List>) (Iterable<?>) (var24)) {
                     if (var6.contains(a(var11, var18, var9, var21, var15))) {
                        var16++;
                     }
                  }

                  if (var16 > var14) {
                     var14 = var16;
                     var13 = var15;
                  }
               }

               var9[var21] = var13;
            }
         }

         for (List var23 : (Iterable<List>) (Iterable<?>) (var5)) {
            var6.remove(a(var23, var9));
         }

         var19.add(this.a(var1, var9, var2));
      }

      return var19;
   }

   private static List<List<Integer>> a(int var0, int var1) {
      ArrayList<java.util.List<java.lang.Integer>> var2 = new ArrayList();
      a(0, var0, var1, new ArrayList<>(), var2);
      return var2;
   }

   private static void a(int var0, int var1, int var2, List<Integer> var3, List<List<Integer>> var4) {
      if (var3.size() == var2) {
         var4.add(List.copyOf(var3));
      } else {
         for (int var5 = var0; var5 < var1; var5++) {
            var3.add(var5);
            a(var5 + 1, var1, var2, var3, var4);
            var3.removeLast();
         }
      }
   }

   private static void a(Set<io.karatelabs.ext.rules.RuleInputSpaceExplorer.InputVectorKey> var0, List<Integer> var1, List<List<Object>> var2, List<Integer> var3) {
      if (var3.size() == var1.size()) {
         var0.add(new io.karatelabs.ext.rules.RuleInputSpaceExplorer.InputVectorKey(var1, List.copyOf(var3)));
      } else {
         int var4 = ((List)var2.get((Integer)var1.get(var3.size()))).size();

         for (int var5 = 0; var5 < var4; var5++) {
            var3.add(var5);
            a(var0, var1, var2, var3);
            var3.removeLast();
         }
      }
   }

   private static io.karatelabs.ext.rules.RuleInputSpaceExplorer.InputVectorKey a(List<Integer> var0, int[] var1) {
      ArrayList<java.lang.Integer> var2 = new ArrayList(var0.size());

      for (int var4 : var0) {
         var2.add(var1[var4]);
      }

      return new io.karatelabs.ext.rules.RuleInputSpaceExplorer.InputVectorKey(var0, var2);
   }

   private static io.karatelabs.ext.rules.RuleInputSpaceExplorer.InputVectorKey a(List<Integer> var0, List<Integer> var1, int[] var2, int var3, int var4) {
      ArrayList<java.lang.Integer> var5 = new ArrayList(var1.size() + 1);

      for (int var7 : var1) {
         var5.add((Integer)var0.get(var7));
      }

      int var10 = 0;

      while (var10 < var5.size() && var5.get(var10) < var3) {
         var10++;
      }

      var5.add(var10, var3);
      ArrayList<java.lang.Integer> var11 = new ArrayList(var5.size());

      for (int var9 : (Iterable<java.lang.Integer>) (Iterable<?>) (var5)) {
         var11.add(var9 == var3 ? var4 : var2[var9]);
      }

      return new io.karatelabs.ext.rules.RuleInputSpaceExplorer.InputVectorKey(var5, var11);
   }

   private Map<String, Object> a(List<io.karatelabs.ext.rules.RuleInputSpaceExplorer.InputDimension> var1, int[] var2, List<List<Object>> var3) {
      var var4 = this.b();

      for (int var5 = 0; var5 < var1.size(); var5++) {
         if ("cardinality".equals(((io.karatelabs.ext.rules.RuleInputSpaceExplorer.InputDimension)var1.get(var5)).b)) {
            a(var4, (io.karatelabs.ext.rules.RuleInputSpaceExplorer.InputDimension)var1.get(var5), ((List)var3.get(var5)).get(var2[var5]));
         }
      }

      for (int var6 = 0; var6 < var1.size(); var6++) {
         if (!"cardinality".equals(((io.karatelabs.ext.rules.RuleInputSpaceExplorer.InputDimension)var1.get(var6)).b)) {
            a(var4, (io.karatelabs.ext.rules.RuleInputSpaceExplorer.InputDimension)var1.get(var6), ((List)var3.get(var6)).get(var2[var6]));
         }
      }

      return var4;
   }

   private boolean c(Map<String, Object> var1) {
      return this.d(var1).isEmpty();
   }

   static List<List<Map<String, Object>>> b(String var0, Map<String, Object> var1, Object var2, String var3, List<Map<String, Object>> var4) {
      Function var5 = a(var0, var1, var2, var3);
      ArrayList<java.util.List<java.util.Map<java.lang.String,java.lang.Object>>> var6 = new ArrayList();

      for (Map var8 : var4) {
         var6.add((List)var5.apply(var8));
      }

      return var6;
   }

   static Function<Map<String, Object>, List<Map<String, Object>>> a(String var0, Map<String, Object> var1, Object var2, String var3) {
      RuleInputSpaceExplorer var4 = new RuleInputSpaceExplorer(var0, var1, var2, var3, null);
      return var4::d;
   }

   private List<Map<String, Object>> d(Map<String, Object> var1) {
      ArrayList<java.util.Map<java.lang.String,java.lang.Object>> var2 = new ArrayList();

      for (io.karatelabs.ext.rules.RuleInputSpaceExplorer.InputDimension var4 : this.t) {
         if (!"array".equals(var4.b)) {
            if ("cardinality".equals(var4.b)) {
               if (RuleCoverageMatrixBuilder.a(var1, var4.j) instanceof List var5 && !var4.a(var5.size())) {
                  var2.add(a(var4, var5.size(), -1));
               }
            } else if (var4.k) {
               this.a(var2, var4, var1);
            } else {
               Object var7 = RuleCoverageMatrixBuilder.a(var1, var4.a);
               if (var7 != null && !var4.a(var7)) {
                  var2.add(a(var4, var7, -1));
               }
            }
         }
      }

      return var2;
   }

   private void a(List<Map<String, Object>> var1, io.karatelabs.ext.rules.RuleInputSpaceExplorer.InputDimension var2, Map<String, Object> var3) {
      int var4 = var2.a.indexOf("[]");
      String var5 = var2.a.substring(0, var4);
      String var6 = var2.a.substring(var4 + 2);
      if (var6.startsWith(".")) {
         var6 = var6.substring(1);
      }

      if (RuleCoverageMatrixBuilder.a(var3, var5) instanceof List var7) {
         for (int var10 = 0; var10 < var7.size(); var10++) {
            if (var7.get(var10) instanceof Map) {
               Object var9 = RuleCoverageMatrixBuilder.a(b(var7.get(var10)), var6);
               if (var9 != null && !var2.a(var9)) {
                  var1.add(a(var2, var9, var10));
                  return;
               }
            }
         }
      }
   }

   private static Map<String, Object> a(io.karatelabs.ext.rules.RuleInputSpaceExplorer.InputDimension var0, Object var1, int var2) {
      LinkedHashMap<java.lang.String, java.lang.Object> var3 = new LinkedHashMap();
      var3.put("axis", var0.a);
      if (var2 >= 0) {
         var3.put("element", var2);
      }

      var3.put("value", var1);
      if (var0.a() != null) {
         var3.put("lo", a(var0.a().doubleValue()));
      }

      if (var0.b() != null) {
         var3.put("hi", a(var0.b().doubleValue()));
      }

      if (var0.d.e() != null) {
         var3.put("min", var0.d.e());
         var3.put("max", var0.d.f());
      }

      String var4 = var0.b;

      var3.put("reason", switch (var4) {
         case "enum" -> "not a declared member";
         case "boolean" -> "not a boolean";
         case "cardinality" -> "array length outside the declared cardinality";
         case "number" -> a(var0, var1);
         default -> "outside the declared bounds";
      });
      return var3;
   }

   private static String a(io.karatelabs.ext.rules.RuleInputSpaceExplorer.InputDimension var0, Object var1) {
      if (var1 instanceof Number var2) {
         double var3 = var2.doubleValue();
         if (Double.isNaN(var3) || Double.isInfinite(var3)) {
            return "not a finite number";
         } else {
            return var0.d.c() && var3 != Math.rint(var3) ? "not a whole number, on an integer axis (g.int)" : "outside the declared bounds";
         }
      } else {
         return "not a number";
      }
   }

   private static void a(Map<String, Object> var0, io.karatelabs.ext.rules.RuleInputSpaceExplorer.InputDimension var1, Object var2) {
      if ("cardinality".equals(var1.b) && var2 instanceof Number var3) {
         a(var0, var1.j, var3.intValue());
      } else {
         b(var0, var1.a, var2);
      }
   }

   static void a(Map<String, Object> var0, String var1, int var2) {
      Object var4 = RuleCoverageMatrixBuilder.a(var0, var1) instanceof List var5 && !var5.isEmpty() ? var5.get(0) : null;
      ArrayList<java.lang.Object> var7 = new ArrayList();

      for (int var6 = 0; var6 < var2; var6++) {
         var7.add(var4 == null ? new LinkedHashMap() : io.karatelabs.ext.common.SandboxExecutor.a(var4));
      }

      b(var0, var1, var7);
   }

   static Map<String, Object> a(Object var0, String var1) {
      return var1 != null && !var1.isBlank() ? b(io.karatelabs.ext.common.SandboxExecutor.a(Generator.a(var1).b())) : b(a(var0));
   }

   static void a(Map<String, Object> var0, String var1, Object var2) {
      if (var1.endsWith("#") && var2 instanceof Number var3) {
         a(var0, var1.substring(0, var1.length() - 1), var3.intValue());
      } else {
         b(var0, var1, var2);
      }
   }

   static void b(Map<String, Object> var0, String var1, Object var2) {
      int var3 = var1.indexOf("[]");
      if (var3 >= 0) {
         String var10 = var1.substring(0, var3);
         String var11 = var1.substring(var3 + 2);
         if (var11.startsWith(".")) {
            var11 = var11.substring(1);
         }

         Object var12 = RuleCoverageMatrixBuilder.a(var0, var10);
         if (var12 instanceof List) {
            for (Object var9 : (List)var12) {
               if (var9 instanceof Map) {
                  b(b(var9), var11, var2);
               }
            }
         }
      } else {
         String[] var4 = var1.split("\\.");
         var var5 = var0;

         for (int var6 = 0; var6 < var4.length - 1; var6++) {
            var5 = b(var5.computeIfAbsent(var4[var6], var0x -> new LinkedHashMap()));
         }

         var5.put(var4[var4.length - 1], var2);
      }
   }

   private List<Map<String, Object>> f() {
      ArrayList<java.util.Map<java.lang.String,java.lang.Object>> var1 = new ArrayList();
      LinkedHashSet<java.lang.String> var2 = new LinkedHashSet();

      for (io.karatelabs.ext.rules.RuleInputSpaceExplorer.InputDimension var4 : this.t) {
         if ("number".equals(var4.b)) {
            for (Object var6 : var4.f()) {
               var var7 = this.b();
               a(var7, var4, var6);
               if (var2.add(Json.stringifyStrict(var7))) {
                  var1.add(var7);
               }
            }
         }
      }

      return var1;
   }

   private static io.karatelabs.ext.rules.RuleInputSpaceExplorer.InputDomainCardinality b(io.karatelabs.ext.rules.RuleInputSpaceExplorer.InputDimension var0) {
      if (!var0.k && !"array".equals(var0.b) && !"cardinality".equals(var0.b)) {
         String var1 = var0.b;

         return switch (var1) {
            case "enum" -> {
               var var5 = var0.d.d();
               yield var5 != null && new HashSet<>(var0.c).containsAll(var5)
                  ? new io.karatelabs.ext.rules.RuleInputSpaceExplorer.InputDomainCardinality(true, (long)var5.size(), null)
                  : new io.karatelabs.ext.rules.RuleInputSpaceExplorer.InputDomainCardinality(false, 0L, var0.m ? "freetext" : "sampled");
            }
            case "boolean" -> new io.karatelabs.ext.rules.RuleInputSpaceExplorer.InputDomainCardinality(true, 2L, null);
            case "number" -> {
               if (var0.a() != null && var0.b() != null) {
                  if (var0.c() && var0.a() == Math.rint(var0.a()) && var0.b() == Math.rint(var0.b())) {
                     long var3 = (long)var0.b().doubleValue() - (long)var0.a().doubleValue() + 1L;
                     yield var3 >= 1L ? new io.karatelabs.ext.rules.RuleInputSpaceExplorer.InputDomainCardinality(true, var3, null) : new io.karatelabs.ext.rules.RuleInputSpaceExplorer.InputDomainCardinality(false, 0L, "unbounded");
                  } else {
                     yield new io.karatelabs.ext.rules.RuleInputSpaceExplorer.InputDomainCardinality(false, 0L, "notinteger");
                  }
               } else {
                  yield new io.karatelabs.ext.rules.RuleInputSpaceExplorer.InputDomainCardinality(false, 0L, "unbounded");
               }
            }
            case "date" -> new io.karatelabs.ext.rules.RuleInputSpaceExplorer.InputDomainCardinality(false, 0L, "date");
            default -> new io.karatelabs.ext.rules.RuleInputSpaceExplorer.InputDomainCardinality(false, 0L, "freetext");
         };
      } else {
         return new io.karatelabs.ext.rules.RuleInputSpaceExplorer.InputDomainCardinality(false, 0L, "array");
      }
   }

   private boolean g() {
      return this.t.stream().allMatch(var0 -> b(var0).a());
   }

   private long h() {
      long var1 = 1L;

      for (io.karatelabs.ext.rules.RuleInputSpaceExplorer.InputDimension var4 : this.t) {
         io.karatelabs.ext.rules.RuleInputSpaceExplorer.InputDomainCardinality var5 = b(var4);
         if (var5.a()) {
            try {
               var1 = Math.multiplyExact(var1, var5.b());
            } catch (ArithmeticException var7) {
               return Long.MAX_VALUE;
            }
         }
      }

      return var1;
   }

   private static List<Object> c(io.karatelabs.ext.rules.RuleInputSpaceExplorer.InputDimension var0) {
      String var1 = var0.b;

      return (List<Object>)(switch (var1) {
         case "enum" -> var0.c;
         case "boolean" -> List.of(false, true);
         default -> {
            ArrayList<java.lang.Object> var3 = new ArrayList();

            for (long var4 = (long)var0.a().doubleValue(); var4 <= (long)var0.b().doubleValue(); var4++) {
               var3.add(a((double)var4));
            }

            yield var3;
         }
      });
   }

   private List<Map<String, Object>> i() {
      var var1 = this.t.stream().map(RuleInputSpaceExplorer::c).toList();
      ArrayList<java.util.Map<java.lang.String,java.lang.Object>> var2 = new ArrayList();
      int var3 = this.t.size();
      int[] var4 = new int[var3];

      int var7;
      do {
         var var5 = this.b();

         for (int var6 = 0; var6 < var3; var6++) {
            a(var5, this.t.get(var6), ((List)var1.get(var6)).get(var4[var6]));
         }

         var2.add(var5);

         for (var7 = var3 - 1; var7 >= 0 && ++var4[var7] == ((List)var1.get(var7)).size(); var7--) {
            var4[var7] = 0;
         }
      } while (var7 >= 0);

      return var2;
   }

   private Map<String, Object> j() {
      LinkedHashMap<java.lang.String, java.lang.Object> var1 = new LinkedHashMap();
      ArrayList<java.util.Map<java.lang.String,java.lang.Object>> var2 = new ArrayList();
      boolean var3 = true;

      for (io.karatelabs.ext.rules.RuleInputSpaceExplorer.InputDimension var5 : this.t) {
         io.karatelabs.ext.rules.RuleInputSpaceExplorer.InputDomainCardinality var6 = b(var5);
         if (!var6.a()) {
            var3 = false;
            LinkedHashMap<java.lang.String, java.lang.Object> var7 = new LinkedHashMap();
            var7.put("axis", var5.a);
            var7.put("cause", var6.c());
            if (var5.l && var5.e()) {
               var7.put("complement", true);
               var7.put("covers", "the 'other' class stands for every value the calc never names");
            }

            var2.add(var7);
         }
      }

      var1.put("enumerable", var3);
      long var9 = this.H != null ? this.H : 50000L;
      var1.put("maxProduct", var9);
      if (var3) {
         long var10 = this.h();
         var1.put("product", var10);
         if (var10 > var9) {
            LinkedHashMap<java.lang.String, java.lang.Object> var8 = new LinkedHashMap();
            var8.put("cause", "overcap");
            var8.put("product", var10);
            var2.add(var8);
         }
      }

      if (!this.x.isEmpty()) {
         var2.add(k("notderivable"));
      }

      if (this.aa > 0) {
         var2.add(k("calcfailure"));
      }

      if (this.ai) {
         var2.add(k("walkmembership"));
      }

      this.X = this.T && this.x.isEmpty() && this.aa == 0 && !this.ai;
      if (var3 && !this.T && var2.isEmpty()) {
         var2.add(k("notwalked"));
      }

      var1.put("walked", this.T);
      var1.put("complete", this.X);
      var1.put("causes", var2);
      if (this.ac > 0L) {
         LinkedHashMap<java.lang.String, java.lang.Object> var11 = new LinkedHashMap();
         var11.put("rows", this.ac);
         var11.put("regions", this.l());
         var1.put("refused", var11);
      }

      var var12 = this.m();
      if (var12 != null) {
         var1.put("certified", var12);
      }

      return var1;
   }

   private long k() {
      return this.V - this.ac;
   }

   private List<Map<String, Object>> l() {
      ArrayList<java.util.Map<java.lang.String,java.lang.Object>> var1 = new ArrayList();

      for (Map var3 : this.ad.values()) {
         LinkedHashMap<java.lang.String, java.lang.Object> var4 = new LinkedHashMap();
         Map var5 = (Map)var3.get("hull");
         StringBuilder var6 = new StringBuilder("{");

         for (Entry var8 : (Iterable<Entry>) (Iterable<?>) (var5.entrySet())) {
            if (var6.length() > 1) {
               var6.append(", ");
            }

            ArrayList var9 = new ArrayList((Collection)var8.getValue());
            var6.append((String)var8.getKey()).append(':').append(var9.size() == 1 ? var9.getFirst() : var9);
         }

         var4.put("box", var6.append('}').toString());
         var4.put("line", var3.get("line"));
         var4.put("signature", var3.get("signature"));
         var4.put("rows", var3.get("rows"));
         var1.add(var4);
      }

      return var1;
   }

   private Map<String, Object> m() {
      if (this.al == null) {
         if (!this.X && RuleInputSpaceCertificateBuilder.b(this.q)) {
            LinkedHashMap<java.lang.String, java.lang.Object> var5 = new LinkedHashMap();
            var5.put("available", false);
            var5.put("hint", "Rule.check(name, {certify: true}) walks the declared domain once and stores the certificate; every later check reads it");
            return var5;
         } else {
            return null;
         }
      } else {
         LinkedHashMap<java.lang.String, java.lang.Object> var1 = new LinkedHashMap();
         var1.put("available", this.al.a());
         if (this.al.e() != null) {
            var1.put("code", this.al.e());
            if (this.al.h() != null) {
               var1.put("type", this.al.h());
            }

            if (this.al.f() > 0) {
               var1.put("line", this.al.f());
            }

            var1.put("error", this.al.g());
            return var1;
         } else if (this.al.d()) {
            var1.put("stale", true);
            var1.put("hint", "calc.js, schema.js or the evaluator moved since the certificate was written — re-run Rule.check(name, {certify: true})");
            return var1;
         } else {
            var var2 = this.al.c();
            var1.put("leaves", var2.get("leaves"));
            var1.put("concrete", var2.get("concrete"));
            var1.put("signatures", (long)this.n().size());
            var1.put("rows", var2.get("declared"));
            var1.put("boxes", var2.get("boxes"));
            var1.put("budget", var2.get("budget"));
            var1.put("partial", var2.get("partial"));
            var1.put("noProgress", var2.get("noProgress"));
            if (Boolean.TRUE.equals(var2.get("partial"))) {
               var1.put("code", "BOX_BUDGET");
            }

            LinkedHashMap<java.lang.String, java.lang.Object> var3 = new LinkedHashMap();
            var3.put("declared", var2.get("declared"));
            var3.put("refused", var2.get("refused"));
            var3.put("accepted", var2.get("accepted"));
            if (Boolean.TRUE.equals(var2.get("partial"))) {
               var3.put("unresolved", var2.get("unresolved"));
            }

            var1.put("domain", var3);
            LinkedHashMap<java.lang.String, java.lang.Object> var4 = new LinkedHashMap();
            var4.put("rows", var2.get("refused"));
            var4.put("regions", var2.getOrDefault("refusedRegions", List.of()));
            var1.put("refused", var4);
            return var1;
         }
      }
   }

   private List<Map<String, Object>> n() {
      if (this.al != null && this.al.a()) {
         return this.al.c().get("signatures") instanceof List var2 ? var2 : List.of();
      } else {
         return List.of();
      }
   }

   private Map<String, Map<String, Object>> o() {
      if (this.al != null && this.al.a()) {
         LinkedHashMap<java.lang.String, java.util.Map<java.lang.String,java.lang.Object>> var1 = new LinkedHashMap();
         Object var3 = this.al.c().get("properties");
         if (var3 instanceof List) {
            for (Object var4 : (List)var3) {
               if (var4 instanceof Map var5) {
                  var1.put(String.valueOf(var5.get("name")), var5);
               }
            }
         }

         return var1;
      } else {
         return Map.of();
      }
   }

   private Map<String, Object> p() {
      var var1 = this.n();
      if (var1.isEmpty()) {
         return null;
      } else if (this.al.b()) {
         return RuleCoverageMatrixBuilder.a(this.al);
      } else {
         ArrayList<java.util.LinkedHashMap> var2 = new ArrayList();

         for (Map var4 : (Iterable<Map>) (Iterable<?>) (var1)) {
            LinkedHashMap<java.lang.String, java.lang.Object> var5 = new LinkedHashMap();
            var5.put("key", var4.get("key"));
            var5.put("rows", var4.get("rows"));
            var5.put("witness", var4.get("witness"));
            var2.add(var5);
         }

         LinkedHashMap<java.lang.String, java.lang.Object> var6 = new LinkedHashMap();
         var6.put("feasible", var1.size());
         var6.put("universe", var2);
         var6.putAll(RuleCoverageMatrixBuilder.a(this.am, var1));
         return var6;
      }
   }

   private static Map<String, Object> k(String var0) {
      LinkedHashMap<java.lang.String, java.lang.Object> var1 = new LinkedHashMap();
      var1.put("cause", var0);
      return var1;
   }

   private io.karatelabs.ext.rules.RuleInputSpaceExplorer.InputSpaceExplorationResult q() {
      LinkedHashMap<java.lang.String, java.lang.Object> var1 = new LinkedHashMap();
      ArrayList<java.util.LinkedHashMap> var2 = new ArrayList();
      LinkedHashMap<java.lang.String, java.util.List<java.lang.String>> var3 = new LinkedHashMap();
      int var4 = 0;

      for (io.karatelabs.ext.rules.RuleInputSpaceExplorer.SchemaSite var6 : this.r) {
         LinkedHashMap<java.lang.String, java.lang.Object> var7 = new LinkedHashMap();
         var7.put("id", var6.a);
         var7.put("line", var6.c);
         var7.put("source", var6.d);
         var2.add(var7);
         var var8 = this.u.get(var6.b);
         var4 += var8.size();
         var3.put(var6.a, var8.stream().map(String::valueOf).toList());
      }

      int var17 = this.r.size() * 2;
      var1.put("sites", var2);
      LinkedHashMap<java.lang.String, java.lang.Object> var18 = new LinkedHashMap();
      var18.put("covered", var4);
      var18.put("total", var17);
      var18.put("percent", var17 == 0 ? 100 : a(Math.round(var4 * 10000.0 / var17) / 100.0));
      var18.put("perSite", var3);
      if (this.Z > 0) {
         LinkedHashMap<java.lang.String, java.util.List<java.lang.String>> var19 = new LinkedHashMap();

         for (io.karatelabs.ext.rules.RuleInputSpaceExplorer.SchemaSite var9 : this.r) {
            var19.put(var9.a, this.ah.getOrDefault(var9.b, Set.of()).stream().map(String::valueOf).toList());
         }

         var18.put("domainPerSite", var19);
      }

      var1.put("coverage", var18);
      LinkedHashMap<java.lang.String, java.util.List<java.lang.String>> var20 = new LinkedHashMap();

      for (io.karatelabs.ext.rules.RuleInputSpaceExplorer.SchemaSite var24 : this.r) {
         var var10 = this.v.getOrDefault(var24.b, Set.of());
         var20.put(var24.a, var10.stream().map(String::valueOf).toList());
      }

      var1.put("seedCoverage", var20);
      ArrayList<java.util.LinkedHashMap> var23 = new ArrayList();

      for (io.karatelabs.ext.rules.RuleInputSpaceExplorer.SchemaSite var27 : this.r) {
         var var11 = this.u.get(var27.b);

         for (boolean var15 : new boolean[]{true, false}) {
            if (!var11.contains(var15)) {
               LinkedHashMap<java.lang.String, java.lang.Object> var16 = new LinkedHashMap();
               var16.put("site", var27.a);
               var16.put("line", var27.c);
               var16.put("outcome", var15);
               var16.put("source", var27.d);
               var23.add(var16);
            }
         }
      }

      var1.put("notcovered", var23);
      ArrayList<java.util.LinkedHashMap> var26 = new ArrayList();

      for (io.karatelabs.ext.rules.RuleInputSpaceExplorer.InputDimension var30 : this.t) {
         LinkedHashMap<java.lang.String, java.lang.Object> var32 = new LinkedHashMap();
         var32.put("path", var30.a);
         var32.put("type", var30.b);
         var32.put("levels", var30.f());
         var26.add(var32);
      }

      var26.sort(Comparator.comparing(var0 -> String.valueOf(var0.get("path"))));
      var1.put("dimensions", var26);
      var var29 = this.j();
      var1.put("ranges", this.t());
      var1.put("universe", var29);
      var var31 = this.p();
      if (var31 != null) {
         var1.put("signatures", var31);
      }

      var1.put("suite", this.w);
      var1.put("notderivable", new ArrayList<>(this.x.values()));
      if (!this.A.isEmpty()) {
         LinkedHashMap<java.lang.String, java.lang.String> var33 = new LinkedHashMap();
         this.A.forEach((var1x, var2x) -> var33.put(String.valueOf(var1x), var2x));
         var1.put("labels", var33);
      }

      if (!this.C.isEmpty() || !this.B.isEmpty()) {
         LinkedHashMap<java.lang.String, java.lang.Object> var34 = new LinkedHashMap();
         var34.put("declared", this.C);
         var34.put("observed", new ArrayList<>(this.B));
         var var36 = this.C.stream().filter(var1x -> !this.B.contains(var1x)).map(var0 -> var0).toList();
         var34.put("neverProduced", var36);
         var1.put("outcomes", var34);
      }

      if (!this.D.isEmpty()) {
         this.c();
         var1.put("properties", this.u());
         if (this.M > 0 || this.Q > 0L) {
            var1.put("search", this.d());
         }
      }

      if (!this.an.isEmpty()) {
         var1.put("propertyErrors", new ArrayList<>(this.an));
      }

      if (!this.y.isEmpty()) {
         var1.put("failures", new ArrayList<>(this.y));
      }

      var var35 = this.s();
      if (var35 != null) {
         var1.put("robustness", var35);
      }

      var var37 = this.r();
      if (!var37.isEmpty()) {
         var1.put("outOfDomain", var37);
      }

      LinkedHashMap<java.lang.String, java.lang.Integer> var38 = new LinkedHashMap();
      var38.put("rounds", this.F);
      var38.put("runs", this.E);
      if (this.T) {
         var38.put("walkRuns", this.V);
      }

      var1.put("stats", var38);
      return new io.karatelabs.ext.rules.RuleInputSpaceExplorer.InputSpaceExplorationResult(var1, Json.stringifyStrict(var1), this.f(var1));
   }

   private List<Map<String, Object>> r() {
      LinkedHashMap<String, Map<String, Object>> var1 = new LinkedHashMap<>();

      for (Map var3 : this.w) {
         if ("out".equals(var3.get("domain"))) {
            this.a((Map<String, Map<String, Object>>)var1, this.d(b(var3.get("input"))), "suite");
         }
      }

      for (Map var7 : this.aj) {
         this.a((Map<String, Map<String, Object>>)var1, e(var7), "saved");
      }

      for (io.karatelabs.ext.rules.RuleInputSpaceExplorer.PropertyExecutionStats var8 : this.D.values()) {
         if (var8.m != null) {
            this.a((Map<String, Map<String, Object>>)var1, this.d(var8.m), "witness");
         }
      }

      this.a((Map<String, Map<String, Object>>)var1, this.ak, "search");
      var var6 = this.t.stream().map(var0 -> var0.a).toList();
      return var1.values().stream().sorted(Comparator.comparingInt(var1x -> var6.indexOf(String.valueOf(var1x.get("axis"))))).toList();
   }

   private static List<Map<String, Object>> e(Map<String, Object> var0) {
      return (List<Map<String, Object>>)var0.get("offending");
   }

   private void a(Map<String, Map<String, Object>> var1, List<Map<String, Object>> var2, String var3) {
      for (Map var5 : var2) {
         String var6 = String.valueOf(var5.get("axis"));
         Map<String, Object> var7 = var1.get(var6);
         if (var7 == null) {
            var7 = new LinkedHashMap();
            var7.put("axis", var6);

            for (String var9 : List.of("lo", "hi", "min", "max")) {
               if (var5.get(var9) != null) {
                  var7.put(var9, var5.get(var9));
               }
            }

            var var10 = this.t.stream().filter(var1x -> var1x.a.equals(var6)).findFirst().map(var0 -> var0.d.d()).orElse(null);
            if (var10 != null) {
               var7.put("members", var10);
            }

            var7.put("reason", var5.get("reason"));
            var7.put("rows", 0);
            var7.put("sources", new ArrayList());
            var1.put(var6, var7);
         }

         var7.put("rows", (Integer)var7.get("rows") + 1);
         List<java.lang.String> var11 = (List)var7.get("sources");
         if (!var11.contains(var3)) {
            var11.add(var3);
         }
      }
   }

   private Map<String, Object> s() {
      ArrayList<java.util.LinkedHashMap> var1 = new ArrayList();

      for (Entry var3 : this.D.entrySet()) {
         io.karatelabs.ext.rules.RuleInputSpaceExplorer.PropertyExecutionStats var4 = (io.karatelabs.ext.rules.RuleInputSpaceExplorer.PropertyExecutionStats)var3.getValue();
         boolean var5 = "sometimes".equals(var4.a);
         if (var5 ? var4.k != 0 : var4.j != 0) {
            LinkedHashMap<java.lang.String, java.lang.Object> var6 = new LinkedHashMap();
            var6.put("name", var3.getKey());
            var6.put("kind", var4.a);
            var6.put("checks", var4.i);
            if (var5) {
               var6.put("satisfied", var4.k);
               if (var4.m != null) {
                  var6.put("witness", var4.m);
               }
            } else {
               var6.put("violations", var4.j);
               if (var4.m != null) {
                  var6.put("counterexample", var4.m);
               }
            }

            if (var4.m != null) {
               var6.put("outOfDomain", d(this.d(var4.m)));
            }

            var1.add(var6);
         }
      }

      ArrayList<java.util.LinkedHashMap> var12 = new ArrayList();

      for (io.karatelabs.ext.rules.RuleInputSpaceExplorer.SchemaSite var15 : this.r) {
         var var18 = this.u.get(var15.b);
         var var20 = this.ah.getOrDefault(var15.b, Set.of());

         for (boolean var10 : new boolean[]{false, true}) {
            if (var18.contains(var10) && !var20.contains(var10)) {
               LinkedHashMap<java.lang.String, java.lang.Object> var11 = new LinkedHashMap();
               var11.put("site", var15.a);
               var11.put("outcome", var10);
               var11.put("line", var15.c);
               var12.add(var11);
            }
         }
      }

      ArrayList<java.util.LinkedHashMap> var14 = new ArrayList();

      for (io.karatelabs.ext.rules.RuleInputSpaceExplorer.InputDimension var19 : this.t) {
         if ("number".equals(var19.b) && (var19.a() != null || var19.b() != null)) {
            for (Double var22 : var19.g) {
               if (var19.a() != null && var22 < var19.a() || var19.b() != null && var22 > var19.b()) {
                  LinkedHashMap<java.lang.String, java.lang.Object> var23 = new LinkedHashMap();
                  var23.put("axis", var19.a);
                  var23.put("value", a(var22.doubleValue()));
                  var14.add(var23);
               }
            }
         }
      }

      if (this.Z == 0 && var1.isEmpty() && var12.isEmpty() && var14.isEmpty() && this.aj.isEmpty()) {
         return null;
      } else {
         LinkedHashMap<java.lang.String, java.lang.Object> var17 = new LinkedHashMap();
         var17.put("rows", this.Z);
         var17.put("failures", this.ae);
         if (!var1.isEmpty()) {
            var17.put("properties", var1);
         }

         if (!this.aj.isEmpty()) {
            var17.put("savedRows", new ArrayList<>(this.aj));
         }

         if (!var12.isEmpty()) {
            var17.put("coverageOnly", var12);
         }

         if (!var14.isEmpty()) {
            var17.put("unreachableBoundaries", var14);
         }

         return var17;
      }
   }

   private Map<String, Object> t() {
      ArrayList<java.util.LinkedHashMap> var1 = new ArrayList();

      for (io.karatelabs.ext.rules.RuleInputSpaceExplorer.InputDimension var3 : this.t) {
         if (!"array".equals(var3.b)) {
            LinkedHashMap<java.lang.String, java.lang.Object> var4 = new LinkedHashMap();
            var4.put("path", var3.a);
            var4.put("label", var3.a);
            var4.put("type", var3.b);
            io.karatelabs.ext.rules.RuleInputSpaceExplorer.InputDomainCardinality var5 = b(var3);
            var4.put("exhaustive", var5.a());
            if (var5.a()) {
               var4.put("size", var5.b());
            } else {
               var4.put("cause", var5.c());
            }

            if (var3.a() != null) {
               var4.put("lo", a(var3.a().doubleValue()));
            }

            if (var3.b() != null) {
               var4.put("hi", a(var3.b().doubleValue()));
            }

            var4.put("classes", e(var3));
            var4.put("gates", new ArrayList<>(var3.i));
            var1.add(var4);
         }
      }

      LinkedHashMap<java.lang.String, java.lang.Object> var6 = new LinkedHashMap();
      var6.put("dimensions", var1);
      return var6;
   }

   private static Map<String, Object> d(io.karatelabs.ext.rules.RuleInputSpaceExplorer.InputDimension var0) {
      var var1 = a("other", var0.d(), false);
      var1.put("complement", true);
      return var1;
   }

   private static List<Map<String, Object>> e(io.karatelabs.ext.rules.RuleInputSpaceExplorer.InputDimension var0) {
      ArrayList<java.util.Map<java.lang.String,java.lang.Object>> var1 = new ArrayList();
      String var2 = var0.b;
      switch (var2) {
         case "enum":
            var0.c.forEach(var1x -> var1.add(a(String.valueOf(var1x), var1x, false)));
            if (var0.l && var0.e()) {
               var1.add(d(var0));
            }
            break;
         case "boolean":
            var1.add(a("true", true, false));
            var1.add(a("false", false, false));
            break;
         case "cardinality":
            var0.h.forEach(var1x -> var1.add(a(var1x + (var1x == 1 ? " item" : " items"), var1x, false)));
            break;
         case "date":
            var1.add(a("(fixed)", "2026-01-01", false));
            break;
         case "number":
            a(var0, (List<Map<String, Object>>)var1);
            break;
         default:
            if (!var0.l && (var0.f.isEmpty() || var0.f.size() == 1 && var0.f.contains("x"))) {
               var1.add(a("any", var0.f.isEmpty() ? "x" : var0.f.first(), false));
            } else {
               var0.f.forEach(var1x -> var1.add(a("= " + var1x, var1x, true)));
            }

            if (var0.l) {
               var1.add(d(var0));
            }
      }

      return var1;
   }

   private static void a(io.karatelabs.ext.rules.RuleInputSpaceExplorer.InputDimension var0, List<Map<String, Object>> var1) {
      Double var2 = var0.a();
      Double var3 = var0.b();
      ArrayList<java.lang.Double> var4 = new ArrayList();

      for (Double var6 : var0.g) {
         if ((var2 == null || var6 >= var2) && (var3 == null || var6 <= var3)) {
            var4.add(var6);
         }
      }

      if (var4.isEmpty()) {
         var1.add(a("any", var2 != null ? a(var2.doubleValue()) : 0, false));
      } else {
         double var12 = (Double)var4.get(0);
         if (var2 == null) {
            var1.add(a("< " + a(var12), a(var12 - b(var12)), false));
         } else {
            a(var0, var2 - b(var2.doubleValue()), var12, a(var2.doubleValue()) + "–" + a(var12), var1);
         }

         for (int var7 = 0; var7 < var4.size(); var7++) {
            double var8 = (Double)var4.get(var7);
            var1.add(a("= " + a(var8), a(var8), true));
            if (var7 < var4.size() - 1) {
               double var10 = (Double)var4.get(var7 + 1);
               a(var0, var8, var10, a(var8) + "–" + a(var10), var1);
            }
         }

         double var13 = (Double)var4.get(var4.size() - 1);
         if (var3 == null) {
            var1.add(a("> " + a(var13), a(var13 + b(var13)), false));
         } else {
            a(var0, var13, var3 + b(var3.doubleValue()), a(var13) + "–" + a(var3.doubleValue()), var1);
         }
      }
   }

   private static void a(io.karatelabs.ext.rules.RuleInputSpaceExplorer.InputDimension var0, double var1, double var3, String var5, List<Map<String, Object>> var6) {
      if (var0.c()) {
         long var7 = (long)Math.floor(var1) + 1L;
         long var9 = (long)Math.ceil(var3) - 1L;
         if (var7 <= var9) {
            var6.add(a(var5, a((double)(var7 + (var9 - var7) / 2L)), false));
         }
      } else {
         if (var1 < var3) {
            var6.add(a(var5, a((var1 + var3) / 2.0), false));
         }
      }
   }

   private static double b(double var0) {
      return var0 == Math.rint(var0) ? 1.0 : 0.01;
   }

   private static Map<String, Object> a(String var0, Object var1, boolean var2) {
      LinkedHashMap<java.lang.String, java.lang.Object> var3 = new LinkedHashMap();
      var3.put("label", var0);
      var3.put("sample", var1);
      var3.put("boundary", var2);
      return var3;
   }

   private List<Map<String, Object>> u() {
      ArrayList<java.util.Map<java.lang.String,java.lang.Object>> var1 = new ArrayList();
      var var2 = this.o();

      for (Entry var4 : this.D.entrySet()) {
         io.karatelabs.ext.rules.RuleInputSpaceExplorer.PropertyExecutionStats var5 = (io.karatelabs.ext.rules.RuleInputSpaceExplorer.PropertyExecutionStats)var4.getValue();
         boolean var6 = "sometimes".equals(var5.a);
         LinkedHashMap<java.lang.String, java.lang.Object> var7 = new LinkedHashMap();
         var7.put("name", var4.getKey());
         var7.put("kind", var5.a);
         var7.put("line", var5.b);
         var7.put("checks", var5.c);
         if (var5.i > 0) {
            var7.put("robustnessChecks", var5.i);
         }

         if (var6) {
            var7.put("satisfied", var5.f);
            var7.put("holds", var5.f > 0);
            String var8 = var5.f > 0 ? "REACHED" : (this.X ? "NOTREACHABLE" : (var5.c == 0 ? "NOTCHECKED" : "NOTREACHED"));
            var7.put("status", var8);
            if ("NOTREACHABLE".equals(var8) && this.ac > 0L) {
               var7.put("domainRestricted", true);
               var7.put("claim", "proven for " + (this.V - this.ac) + " of " + this.V + " declared rows; " + this.ac + " refused by the oracle");
            }

            if (var5.h != null) {
               var7.put("witness", var5.h);
            }
         } else {
            var7.put("violations", var5.e);
            boolean var18 = var5.e > 0 || var5.o > 0;
            var7.put("holds", var5.c > 0 && !var18);
            Map<java.lang.String, Object> var9 = (Map)var2.get(var4.getKey());
            String var10 = var18 ? "VIOLATED" : (var5.c == 0 ? "NOTCHECKED" : (this.X && var5.d == this.k() ? "COMPLETE" : "HOLDS"));
            if (var9 != null) {
               var10 = this.a(var9, var18);
            }

            var7.put("status", var10);
            if (this.T) {
               var7.put("walkChecks", var5.d);
            }

            if (var9 != null) {
               LinkedHashMap<java.lang.String, java.lang.Object> var11 = new LinkedHashMap();
               var11.put("trueRows", var9.get("trueRows"));
               var11.put("falseRows", var9.get("falseRows"));
               var11.put("unknownRows", var9.get("unknownRows"));
               if (var9.get("unresolvedRows") != null) {
                  var11.put("unresolvedRows", var9.get("unresolvedRows"));
               } else if (this.al.b()) {
                  var11.put("unresolvedRows", this.al.c().get("unresolved"));
               }

               var11.put("executedRows", var9.get("executedRows"));
               var7.put("certified", var11);
            }

            long var19 = var9 != null ? c(this.al.c().get("declared")) : this.V;
            long var13 = var9 != null ? c(this.al.c().get("refused")) : this.ac;
            if ("COMPLETE".equals(var10) && var13 > 0L) {
               var7.put("domainRestricted", true);
               var7.put("claim", "proven for " + (var19 - var13) + " of " + var19 + " declared rows; " + var13 + " refused by the oracle");
            }

            if (var5.n > 0) {
               var7.put("searched", var5.n);
               if (var5.o > 0) {
                  var7.put("searchViolations", var5.o);
               }
            }

            Map var15 = var5.h != null ? var5.h : var5.q;
            if (var15 == null && var9 != null && var9.get("counterexample") instanceof Map var16) {
               var15 = b((Object)var16);
            }

            if (var15 != null) {
               var7.put("counterexample", var15);
            }
         }

         if (var5.r != null) {
            var7.put("req", var5.r);
         }

         var1.add(var7);
      }

      return var1;
   }

   private String a(Map<String, Object> var1, boolean var2) {
      long var3 = c(this.al.c().get("accepted"));
      long var5 = c(var1.get("trueRows"));
      long var7 = c(var1.get("falseRows"));
      long var9 = c(var1.get("unknownRows")) + c(var1.get("unresolvedRows"));
      long var11 = c(var1.get("executedRows"));
      if (var7 > 0L || var2) {
         return "VIOLATED";
      } else if (var3 == 0L || var11 == 0L) {
         return "NOTCHECKED";
      } else if (this.al.b()) {
         return "HOLDS";
      } else {
         return var9 == 0L && var5 == var3 && var11 == var3 ? "COMPLETE" : "HOLDS";
      }
   }

   private static long c(Object var0) {
      return var0 instanceof Number var1 ? var1.longValue() : 0L;
   }

   private String f(Map<String, Object> var1) {
      StringBuilder var2 = new StringBuilder();
      Map<java.lang.String, Object> var3 = (Map)var1.get("coverage");
      var2.append("RulePathExplorer: ")
         .append(var3.get("covered"))
         .append('/')
         .append(var3.get("total"))
         .append(" site-outcomes (")
         .append(var3.get("percent"))
         .append("%), suite of ")
         .append(this.w.size())
         .append(" rows, ")
         .append(this.F)
         .append(" rounds, ")
         .append(this.E)
         .append(" runs\n");
      var2.append("sites:\n");

      for (io.karatelabs.ext.rules.RuleInputSpaceExplorer.SchemaSite var5 : this.r) {
         var2.append("  ").append(var5.a).append("  ").append(this.u.get(var5.b)).append("  ").append(var5.d).append('\n');
      }

      var2.append("dimensions:\n");

      for (io.karatelabs.ext.rules.RuleInputSpaceExplorer.InputDimension var7 : this.t) {
         var2.append("  ").append(var7.a).append(" (").append(var7.b).append("): ").append(var7.f()).append('\n');
      }

      var2.append("notderivable: ").append(this.x.isEmpty() ? "none" : this.x.values()).append('\n');
      if (!this.y.isEmpty()) {
         var2.append("failures: ").append(this.y.size()).append(" row(s) threw — first: ").append(this.y.getFirst().get("error")).append('\n');
      }

      return var2.toString();
   }

   private record InputDomainCardinality(boolean a, long b, String c) {

      private InputDomainCardinality(boolean a, long b, String c) {
         this.a = a;
         this.b = b;
         this.c = c;
      }
   }

   private static final class InputDimension {
      final String a;
      final String b;
      final List<Object> c;
      final io.karatelabs.ext.rules.RuleInputSpaceExplorer.InputDomainConstraint d;
      final TreeSet<Double> e = new TreeSet<>();
      final TreeSet<String> f = new TreeSet<>();
      final TreeSet<Double> g = new TreeSet<>();
      final TreeSet<Integer> h = new TreeSet<>();
      final TreeSet<String> i = new TreeSet<>(RuleCoverageMatrixBuilder::a);
      String j;
      boolean k;
      boolean l;
      boolean m;

      InputDimension(String var1, String var2, List<Object> var3) {
         this(var1, var2, var3, io.karatelabs.ext.rules.RuleInputSpaceExplorer.InputDomainConstraint.a);
      }

      InputDimension(String var1, String var2, List<Object> var3, io.karatelabs.ext.rules.RuleInputSpaceExplorer.InputDomainConstraint var4) {
         this.a = var1;
         this.b = var2;
         this.c = var3;
         this.d = var4;
         this.k = var1.contains("[]");
      }

      Double a() {
         return this.d.a();
      }

      Double b() {
         return this.d.b();
      }

      boolean c() {
         return this.d.c();
      }

      boolean a(Object var1) {
         String var2 = this.b;

         return switch (var2) {
            case "enum" -> this.d.d() == null || this.d.d().contains(var1);
            case "boolean" -> var1 instanceof Boolean;
            case "number" -> {
               if (this.d.a() == null && this.d.b() == null && !this.d.c()) {
                  yield true;
               } else if (var1 instanceof Number var4) {
                  double var5 = var4.doubleValue();
                  yield Double.isNaN(var5) || Double.isInfinite(var5)
                     ? false
                     : (
                        this.d.c() && var5 != Math.rint(var5)
                           ? false
                           : (this.d.a() != null && var5 < this.d.a() ? false : this.d.b() == null || var5 <= this.d.b())
                     );
               } else {
                  yield false;
               }
            }
            default -> true;
         };
      }

      boolean a(int var1) {
         return (this.d.e() == null || var1 >= this.d.e()) && (this.d.f() == null || var1 <= this.d.f());
      }

      String d() {
         String var1 = "other";

         for (int var2 = 2; this.f.contains(var1) || this.a(var1); var2++) {
            var1 = "other" + var2;
         }

         return var1;
      }

      private boolean a(String var1) {
         return this.c != null && this.c.stream().anyMatch(var1::equals);
      }

      boolean e() {
         return "string".equals(this.b) ? true : "enum".equals(this.b) && this.m && this.d.d() == null;
      }

      List<Object> f() {
         String var1 = this.b;

         return (List<Object>)(switch (var1) {
            case "enum" -> {
               if (this.l && this.e()) {
                  var var4 = new ArrayList<>(this.c);
                  var4.add(this.d());
                  yield var4;
               } else {
                  yield this.c;
               }
            }
            case "boolean" -> List.of(false, true);
            case "number" -> this.e.stream().map(RuleInputSpaceExplorer::a).toList();
            case "cardinality" -> new ArrayList(this.h);
            case "date" -> List.of("2026-01-01");
            case "array" -> List.of();
            default -> {
               if (!this.l) {
                  yield List.copyOf(this.f);
               } else {
                  var var3 = new ArrayList<>(this.f);
                  var3.add(this.d());
                  yield var3;
               }
            }
         });
      }
   }

   private record InputDomainConstraint(Double component1, Double b, boolean c, List<Object> d, Integer e, Integer f) {
      static final io.karatelabs.ext.rules.RuleInputSpaceExplorer.InputDomainConstraint a = new io.karatelabs.ext.rules.RuleInputSpaceExplorer.InputDomainConstraint(null, null, false, null, null, null);

      private InputDomainConstraint(Double component1, Double b, boolean c, List<Object> d, Integer e, Integer f) {
         this.component1 = component1;
         this.b = b;
         this.c = c;
         this.d = d;
         this.e = e;
         this.f = f;
      }

      static io.karatelabs.ext.rules.RuleInputSpaceExplorer.InputDomainConstraint a(Double var0, Double var1, boolean var2) {
         return new io.karatelabs.ext.rules.RuleInputSpaceExplorer.InputDomainConstraint(var0, var1, var2, null, null, null);
      }

      static io.karatelabs.ext.rules.RuleInputSpaceExplorer.InputDomainConstraint a(List<Object> var0) {
         return new io.karatelabs.ext.rules.RuleInputSpaceExplorer.InputDomainConstraint(null, null, false, var0 == null ? null : Collections.unmodifiableList(new ArrayList<>(var0)), null, null);
      }

      static io.karatelabs.ext.rules.RuleInputSpaceExplorer.InputDomainConstraint a(int var0, int var1) {
         return new io.karatelabs.ext.rules.RuleInputSpaceExplorer.InputDomainConstraint(null, null, true, null, var0, var1);
      }

      public Double a() {
         return this.component1;
      }

      public Double b() {
         return this.b;
      }

      public boolean c() {
         return this.c;
      }

      public List<Object> d() {
         return this.d;
      }

      public Integer e() {
         return this.e;
      }

      public Integer f() {
         return this.f;
      }
   }

   private static final class CalcEventCollector implements ContextListener {
      final Set<Entry<Integer, Boolean>> a = new LinkedHashSet<>();
      final List<Event> b = new ArrayList<>();

      @Override
      public void onEvent(Event var1) {
         if (var1.type == EventType.BRANCH) {
            this.a.add(Map.entry(var1.node.getFirstToken().pos, (Boolean)var1.value));
         } else if (var1.type == EventType.COMPARE) {
            this.b.add(var1);
         }
      }
   }

   private static final class PropertyExecutionStats {
      String a;
      int b;
      int c;
      int d;
      int e;
      int f;
      String g;
      Map<String, Object> h;
      int i;
      int j;
      int k;
      String l;
      Map<String, Object> m;
      int n;
      int o;
      String p;
      Map<String, Object> q;
      String r;
   }

   public record InputSpaceExplorationResult(Map<String, Object> a, String b, String c) {

      public InputSpaceExplorationResult(Map<String, Object> a, String b, String c) {
         this.a = a;
         this.b = b;
         this.c = c;
      }
   }

   private record MaskedCalcSource(String a, boolean[] b) {

      private MaskedCalcSource(String a, boolean[] b) {
         this.a = a;
         this.b = b;
      }
   }

   private record SchemaSite(String a, int b, int c, String d) {

      private SchemaSite(String a, int b, int c, String d) {
         this.a = a;
         this.b = b;
         this.c = c;
         this.d = d;
      }
   }

   private record InputVectorKey(List<Integer> a, List<Integer> b) {

      private InputVectorKey(List<Integer> a, List<Integer> b) {
         this.a = a;
         this.b = b;
      }
   }
}
