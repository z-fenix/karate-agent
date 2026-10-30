package io.karatelabs.ext.rules;

import io.karatelabs.js.Engine;
import io.karatelabs.js.Terms;
import io.karatelabs.parser.Node;
import io.karatelabs.parser.NodeType;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class WorksheetMatchDiagnostics {
   public static final String a = "aligned";
   public static final String b = "notAvailable";
   public static final String c = "unavailable";
   public static final String d = "applicability";
   public static final String e = "amount";
   public static final String f = "unparseable";
   public static final String g = "ranked";
   public static final String h = "change-set";
   public static final String i = "probe-sensitive";
   public static final String j = "read";
   private static final Pattern k = Pattern.compile("-?\\d+(\\.\\d+)?");
   private static final Comparator<io.karatelabs.ext.rules.WorksheetMatchDiagnostics.CarrierMatchCandidate> l = Comparator.comparingInt(io.karatelabs.ext.rules.WorksheetMatchDiagnostics.CarrierMatchCandidate::a).thenComparingInt(io.karatelabs.ext.rules.WorksheetMatchDiagnostics.CarrierMatchCandidate::b).thenComparing(io.karatelabs.ext.rules.WorksheetMatchDiagnostics.CarrierMatchCandidate::c, WorksheetMatchDiagnostics::b);

   private WorksheetMatchDiagnostics() {
   }

   public static boolean a(String var0) {
      Node var1;
      try {
         var1 = Engine.parse(var0);
      } catch (RuntimeException var11) {
         return false;
      }

      for (Node var3 : var1.findAll(NodeType.FN_CALL_EXPR)) {
         Node var4 = var3.get(0);
         if (var4.type == NodeType.REF_DOT_EXPR && var4.getText().endsWith(".log") && var3.size() >= 3) {
            var var5 = var3.get(2).findImmediateChildren(NodeType.FN_CALL_ARG);
            if (var5.size() >= 2) {
               for (Node var7 : ((Node)var5.get(1)).findAll(NodeType.LIT_OBJECT)) {
                  for (int var8 = 1; var8 < var7.size() - 1; var8++) {
                     Node var9 = var7.get(var8).getFirst();
                     Object var10 = var9.isToken() ? Terms.literalValue(var9.token) : null;
                     if ("step".equals(var10 == null ? var9.getText() : String.valueOf(var10))) {
                        return true;
                     }
                  }
               }
            }
         }
      }

      return false;
   }

   public static List<ResultTreeNode> a(List<ResultTreeNode> var0) {
      ArrayList<io.karatelabs.ext.rules.ResultTreeNode> var1 = new ArrayList();
      a(var0 == null ? List.of() : var0, var1);
      return var1;
   }

   private static void a(List<ResultTreeNode> var0, List<ResultTreeNode> var1) {
      for (ResultTreeNode var3 : var0) {
         if (var3.f() != null) {
            var1.add(var3);
         }

         a(var3.k(), var1);
      }
   }

   public static Map<String, Object> a(boolean var0, List<ResultTreeNode> var1, List<?> var2, List<String> var3, int var4) {
      if (!var0) {
         return c("notAvailable", "no-structured-steps");
      } else if (var2 == null) {
         return c("notAvailable", "no-carrier-worksheet");
      } else {
         ArrayList<java.util.regex.Pattern> var5 = new ArrayList();

         for (String var7 : (Iterable<String>) (Iterable<?>) (var3 == null ? List.of() : var3)) {
            var5.add(b(var7));
         }

         LinkedHashMap<java.lang.String, io.karatelabs.ext.rules.WorksheetMatchDiagnostics.WorksheetPatternMatch> var22 = new LinkedHashMap();
         HashMap<java.lang.String, java.lang.Double> var23 = new HashMap();

         for (ResultTreeNode var9 : a(var1)) {
            io.karatelabs.ext.rules.WorksheetMatchDiagnostics.WorksheetPatternMatch var10 = a(var9.f(), var5);
            if (var10 != null) {
               if (var22.put(var10.a(), var10) != null) {
                  return b(var10.a(), "oracle");
               }

               var23.put(var10.a(), var9.g());
            }
         }

         LinkedHashMap<java.lang.String, io.karatelabs.ext.rules.WorksheetMatchDiagnostics.WorksheetPatternMatch> var24 = new LinkedHashMap();
         HashMap<java.lang.String, java.lang.Object> var25 = new HashMap();
         LinkedHashSet<java.lang.String> var26 = new LinkedHashSet();

         for (Object var12 : var2) {
            Map<java.lang.String, Object> var13 = var12 instanceof Map var14 ? var14 : Map.of();
            String var30 = var13.get("step") == null ? null : String.valueOf(var13.get("step"));
            io.karatelabs.ext.rules.WorksheetMatchDiagnostics.WorksheetPatternMatch var15 = var30 == null ? null : a(var30, var5);
            if (var15 == null) {
               var26.add(String.valueOf(var30));
            } else {
               if (var24.put(var15.a(), var15) != null) {
                  return b(var15.a(), "carrier");
               }

               var25.put(var15.a(), var13.get("amount"));
            }
         }

         var var27 = new ArrayList<>(var22.keySet());
         ArrayList<java.util.Map<java.lang.String,java.lang.Object>> var28 = new ArrayList();
         ArrayList<io.karatelabs.ext.rules.WorksheetMatchDiagnostics.CarrierMatchCandidate> var29 = new ArrayList();
         ArrayList<java.util.Map<java.lang.String,java.lang.Object>> var31 = new ArrayList();
         int var32 = 0;

         for (int var16 = 0; var16 < var27.size(); var16++) {
            String var17 = (String)var27.get(var16);
            Double var18 = (Double)var23.get(var17);
            Map<java.lang.String, java.lang.Object> var19 = null;
            if (!var24.containsKey(var17)) {
               var19 = a(var17, "applicability");
               var19.put("side", "oracle");
               if (var18 != null) {
                  var19.put("ours", a(var18, var4));
               }
            } else {
               var32++;
               Object var20 = var25.get(var17);
               BigDecimal var21 = a(var20);
               if (var18 != null && var20 != null && (var21 == null || var21.scale() > var4)) {
                  var19 = a(var17, "unparseable");
                  var19.put("reason", var21 == null ? "unparseable" : "scale");
                  var19.put("ours", a(var18, var4));
                  var19.put("theirs", String.valueOf(var20));
               } else if (var18 != null && var21 != null && a(var18, var4).compareTo(var21) != 0) {
                  var19 = a(var17, "amount");
                  var19.put("ours", a(var18, var4));
                  var19.put("theirs", var21);
                  var19.put("difference", var21.subtract(a(var18, var4)));
               }
            }

            if (var19 != null) {
               var28.add(var19);
               var29.add(new io.karatelabs.ext.rules.WorksheetMatchDiagnostics.CarrierMatchCandidate(var16, 0, List.of()));
            }
         }

         for (io.karatelabs.ext.rules.WorksheetMatchDiagnostics.WorksheetPatternMatch var35 : (Iterable<io.karatelabs.ext.rules.WorksheetMatchDiagnostics.WorksheetPatternMatch>) (Iterable<?>) (var24.values())) {
            if (!var22.containsKey(var35.a())) {
               var var38 = a(var35.a(), "applicability");
               var38.put("side", "carrier");
               BigDecimal var40 = a(var25.get(var35.a()));
               if (var40 != null) {
                  var38.put("theirs", var40);
               }

               io.karatelabs.ext.rules.WorksheetMatchDiagnostics.CarrierMatchCandidate var41 = a(var35, var27, var22);
               if (var41 == null) {
                  var31.add(var38);
               } else {
                  var28.add(var38);
                  var29.add(var41);
               }
            }
         }

         ArrayList<java.lang.Integer> var34 = new ArrayList();

         for (int var36 = 0; var36 < var28.size(); var36++) {
            var34.add(var36);
         }

         var34.sort(Comparator.comparing(var29::get, l));
         ArrayList<java.util.Map> var37 = new ArrayList();
         var34.forEach(var2x -> var37.add((Map)var28.get(var2x)));
         var37.addAll(var31);
         LinkedHashMap<java.lang.String, java.lang.Object> var39 = new LinkedHashMap();
         var39.put("status", "aligned");
         var39.put("aligned", var32);
         var39.put("unmapped", new ArrayList(var26));
         var39.put("ordered", var31.isEmpty());
         if (var31.isEmpty() && !var37.isEmpty()) {
            var39.put("first", ((Map)var37.get(0)).get("step"));
         }

         var39.put("divergences", var37);
         return var39;
      }
   }

   private static Pattern b(String var0) {
      StringBuilder var1 = new StringBuilder();
      String[] var2 = var0.split("\\*", -1);

      for (int var3 = 0; var3 < var2.length; var3++) {
         if (var3 > 0) {
            var1.append("([^\\[\\]]+)");
         }

         var1.append(Pattern.quote(var2[var3]));
      }

      return Pattern.compile(var1.toString());
   }

   private static io.karatelabs.ext.rules.WorksheetMatchDiagnostics.WorksheetPatternMatch a(String var0, List<Pattern> var1) {
      for (int var2 = 0; var2 < var1.size(); var2++) {
         Matcher var3 = ((Pattern)var1.get(var2)).matcher(var0);
         if (var3.matches()) {
            ArrayList<java.lang.Long> var4 = new ArrayList();

            for (int var5 = 1; var5 <= var3.groupCount(); var5++) {
               try {
                  var4.add(Long.parseLong(var3.group(var5)));
               } catch (NumberFormatException var7) {
                  return new io.karatelabs.ext.rules.WorksheetMatchDiagnostics.WorksheetPatternMatch(var0, var2, null);
               }
            }

            return new io.karatelabs.ext.rules.WorksheetMatchDiagnostics.WorksheetPatternMatch(var0, var2, var4);
         }
      }

      return null;
   }

   private static io.karatelabs.ext.rules.WorksheetMatchDiagnostics.CarrierMatchCandidate a(io.karatelabs.ext.rules.WorksheetMatchDiagnostics.WorksheetPatternMatch var0, List<String> var1, Map<String, io.karatelabs.ext.rules.WorksheetMatchDiagnostics.WorksheetPatternMatch> var2) {
      if (var0.c() != null && !var0.c().isEmpty()) {
         int var3 = -1;
         int var4 = -1;

         for (int var5 = 0; var5 < var1.size(); var5++) {
            io.karatelabs.ext.rules.WorksheetMatchDiagnostics.WorksheetPatternMatch var6 = (io.karatelabs.ext.rules.WorksheetMatchDiagnostics.WorksheetPatternMatch)var2.get(var1.get(var5));
            if (var6.b() == var0.b() && var6.c() != null) {
               int var7 = b(var6.c(), var0.c());
               if (var7 >= 0 || var3 >= 0 && b(var6.c(), ((io.karatelabs.ext.rules.WorksheetMatchDiagnostics.WorksheetPatternMatch)var2.get(var1.get(var3))).c()) <= 0) {
                  if (var7 > 0 && (var4 < 0 || b(var6.c(), ((io.karatelabs.ext.rules.WorksheetMatchDiagnostics.WorksheetPatternMatch)var2.get(var1.get(var4))).c()) < 0)) {
                     var4 = var5;
                  }
               } else {
                  var3 = var5;
               }
            }
         }

         if (var3 >= 0) {
            return new io.karatelabs.ext.rules.WorksheetMatchDiagnostics.CarrierMatchCandidate(var3, 1, var0.c());
         } else {
            return var4 >= 0 ? new io.karatelabs.ext.rules.WorksheetMatchDiagnostics.CarrierMatchCandidate(var4, -1, var0.c()) : null;
         }
      } else {
         return null;
      }
   }

   private static int b(List<Long> var0, List<Long> var1) {
      for (int var2 = 0; var2 < Math.min(var0.size(), var1.size()); var2++) {
         int var3 = Long.compare(var0.get(var2), var1.get(var2));
         if (var3 != 0) {
            return var3;
         }
      }

      return Integer.compare(var0.size(), var1.size());
   }

   private static BigDecimal a(double var0, int var2) {
      return BigDecimal.valueOf(var0).setScale(var2, RoundingMode.HALF_UP);
   }

   private static BigDecimal a(Object var0) {
      if (var0 == null) {
         return null;
      } else {
         String var1 = !(var0 instanceof Double) && !(var0 instanceof Float)
            ? String.valueOf(var0).trim()
            : BigDecimal.valueOf(((Number)var0).doubleValue()).toPlainString();
         return k.matcher(var1).matches() ? new BigDecimal(var1) : null;
      }
   }

   private static Map<String, Object> a(String var0, String var1) {
      LinkedHashMap<java.lang.String, java.lang.Object> var2 = new LinkedHashMap();
      var2.put("step", var0);
      var2.put("kind", var1);
      return var2;
   }

   private static Map<String, Object> b(String var0, String var1) {
      var var2 = c("unavailable", "repeated-step");
      var2.put("step", var0);
      var2.put("side", var1);
      return var2;
   }

   private static Map<String, Object> c(String var0, String var1) {
      LinkedHashMap<java.lang.String, java.lang.Object> var2 = new LinkedHashMap();
      var2.put("status", var0);
      var2.put("reason", var1);
      return var2;
   }

   public static Map<String, Object> a(boolean var0, Collection<String> var1, Collection<String> var2, Map<String, io.karatelabs.ext.rules.CellCoverageAnalyzer.CellProbeResult> var3) {
      if (!var0) {
         return c("notChecked", "no-table-files");
      } else {
         Object var4 = var2 == null ? Set.of() : new LinkedHashSet(var2);
         ArrayList<java.util.LinkedHashMap> var5 = new ArrayList();

         for (String var7 : List.of("change-set", "probe-sensitive", "read")) {
            for (String var9 : var1) {
               if (var7.equals(a(var9, (Set<String>)var4, var3))) {
                  LinkedHashMap<java.lang.String, java.lang.String> var10 = new LinkedHashMap();
                  var10.put("address", var9);
                  var10.put("rank", var7);
                  var5.add(var10);
               }
            }
         }

         LinkedHashMap<java.lang.String, java.lang.Object> var11 = new LinkedHashMap();
         var11.put("status", "ranked");
         var11.put("cells", var5);
         return var11;
      }
   }

   public static Map<String, Object> a(CellCoverageAnalyzer var0, Object var1, Map<String, Object> var2, Collection<String> var3) {
      if (var0.a().isEmpty()) {
         return a(false, List.of(), var3, Map.of());
      } else {
         io.karatelabs.ext.rules.CellCoverageAnalyzer.RowCoverageResult var4 = var0.a(var1, var2, null);
         return !var4.a() ? c("notChecked", var4.d()) : a(true, var4.e().keySet(), var3, var4.f());
      }
   }

   public static Map<String, Object> a(io.karatelabs.ext.rules.RulebookRepository.RulebookDocument var0, RuleEvaluator var1, Map<String, Object> var2, Map<String, Object> var3, EvidenceMapping var4, Collection<String> var5) {
      HashMap<java.lang.String, java.util.Map> var6 = new HashMap();

      for (Map var8 : (Iterable<Map>) (Iterable<?>) ((List)var2.get("rows"))) {
         if (var8.get("input") instanceof Map var9) {
            var6.put(String.valueOf(var8.get("id")), var9);
         }
      }

      boolean var17 = a(var0.g());
      CellCoverageAnalyzer var18 = null;

      for (Map var20 : (Iterable<Map>) (Iterable<?>) ((List)var3.get("cases"))) {
         Map var11 = (Map)var6.get(String.valueOf(var20.get("case")));
         if ("mismatch".equals(var20.get("class")) && var11 != null) {
            List var12;
            try {
               var12 = var1.a(var0.g(), var0.m().e(), var11, null).a();
            } catch (RuntimeException var16) {
               var12 = List.of();
            }

            if (var18 == null) {
               var18 = new CellCoverageAnalyzer(var0, var1);
            }

            LinkedHashMap<java.lang.String, java.util.Map> var13 = new LinkedHashMap();
            var13.put("worksheet", a(var17, var12, var20.get("worksheet") instanceof List var14 ? var14 : null, var4.i(), var4.g()));
            var13.put("cells", a(var18, var20.get("case"), var11, var5));
            var20.put("diagnosis", var13);
         }
      }

      return var3;
   }

   private static String a(String var0, Set<String> var1, Map<String, io.karatelabs.ext.rules.CellCoverageAnalyzer.CellProbeResult> var2) {
      if (var1.contains(var0)) {
         return "change-set";
      } else {
         io.karatelabs.ext.rules.CellCoverageAnalyzer.CellProbeResult var3 = var2 == null ? null : (io.karatelabs.ext.rules.CellCoverageAnalyzer.CellProbeResult)var2.get(var0);
         return var3 != null && "exercised".equals(var3.f()) ? "probe-sensitive" : "read";
      }
   }

   private record WorksheetPatternMatch(String a, int b, List<Long> c) {

      private WorksheetPatternMatch(String a, int b, List<Long> c) {
         this.a = a;
         this.b = b;
         this.c = c;
      }
   }

   private record CarrierMatchCandidate(int a, int b, List<Long> c) {

      private CarrierMatchCandidate(int a, int b, List<Long> c) {
         this.a = a;
         this.b = b;
         this.c = c;
      }
   }
}
