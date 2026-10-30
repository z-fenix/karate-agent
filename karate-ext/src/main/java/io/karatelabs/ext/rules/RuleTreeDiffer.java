package io.karatelabs.ext.rules;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Map.Entry;

public final class RuleTreeDiffer {
   private RuleTreeDiffer() {
   }

   public static io.karatelabs.ext.rules.RuleTreeDiffer.RuleTreeDiffResult a(List<ResultTreeNode> var0, RuleBaselineSnapshot var1) {
      List var2 = var0 == null ? List.of() : var0;
      Map var3 = var1 == null ? Map.of() : var1.a();
      Map var4 = a(var3);
      HashMap var5 = new HashMap();
      ArrayList<io.karatelabs.ext.rules.ResultTreeNode> var6 = new ArrayList();

      for (ResultTreeNode var8 : (Iterable<ResultTreeNode>) (Iterable<?>) (var2)) {
         var6.add(a(var8, var3, var1 != null, var5));
      }

      if (!var3.isEmpty()) {
         Map var9 = a(var2);
         a(var6, var9, var5, var3, var4);
      }

      return new io.karatelabs.ext.rules.RuleTreeDiffer.RuleTreeDiffResult(var6, c(b(var2)));
   }

   private static ResultTreeNode a(ResultTreeNode var0, Map<String, ExecutionTraceSnapshotNode> var1, boolean var2, Map<String, List<ResultTreeNode>> var3) {
      ExecutionTraceSnapshotNode var4 = var0.a() == null ? null : (ExecutionTraceSnapshotNode)var1.get(var0.a());
      String var6 = null;
      List var7 = null;
      String var5;
      if (!var2) {
         var5 = "unchanged";
      } else if (var4 == null) {
         var5 = "added";
      } else if (Objects.equals(var4.e(), var0.e())) {
         var5 = "unchanged";
      } else {
         var5 = "changed";
         var6 = var4.e();
         var7 = a(var6, var0.e());
      }

      ArrayList<io.karatelabs.ext.rules.ResultTreeNode> var8 = new ArrayList(var0.k().size());
      if (var0.a() != null) {
         var3.put(var0.a(), var8);
      }

      for (ResultTreeNode var10 : var0.k()) {
         var8.add(a(var10, var1, var2, var3));
      }

      return new ResultTreeNode(var0.a(), var0.b(), var0.c(), var0.d(), var0.e(), var0.f(), var0.g(), var5, var6, var7, var8);
   }

   private static void a(List<ResultTreeNode> var0, Map<String, ResultTreeNode> var1, Map<String, List<ResultTreeNode>> var2, Map<String, ExecutionTraceSnapshotNode> var3, Map<String, String> var4) {
      for (Entry var6 : var3.entrySet()) {
         String var7 = (String)var6.getKey();
         if (!var1.containsKey(var7)) {
            String var8 = (String)var4.get(var7);
            if (var8 == null || var1.containsKey(var8)) {
               List<io.karatelabs.ext.rules.ResultTreeNode> var9 = var8 == null ? var0 : (List)var2.get(var8);
               if (var9 == null) {
                  var9 = var0;
               }

               var9.add(a((ExecutionTraceSnapshotNode)var6.getValue()));
            }
         }
      }
   }

   private static ResultTreeNode a(ExecutionTraceSnapshotNode var0) {
      int var1 = b(var0);
      String var2 = var0.e();
      if (var1 > 0) {
         var2 = (var2 != null && !var2.isEmpty() ? var2 + " " : "") + "(+" + var1 + " removed children)";
      }

      return new ResultTreeNode(var0.a(), var0.b(), var0.c(), var0.d(), var2, null, null, "removed", null, null, List.of());
   }

   private static int b(ExecutionTraceSnapshotNode var0) {
      int var1 = 0;

      for (ExecutionTraceSnapshotNode var3 : var0.f()) {
         var1 += 1 + b(var3);
      }

      return var1;
   }

   static List<RuleDiffLine> a(String var0, String var1) {
      var var2 = a(var0 == null ? "" : var0);
      var var3 = a(var1 == null ? "" : var1);
      int var4 = var2.size();
      int var5 = var3.size();
      int[][] var6 = new int[var4 + 1][var5 + 1];

      for (int var7 = var4 - 1; var7 >= 0; var7--) {
         for (int var8 = var5 - 1; var8 >= 0; var8--) {
            if (((String)var2.get(var7)).equals(var3.get(var8))) {
               var6[var7][var8] = var6[var7 + 1][var8 + 1] + 1;
            } else {
               var6[var7][var8] = Math.max(var6[var7 + 1][var8], var6[var7][var8 + 1]);
            }
         }
      }

      ArrayList<io.karatelabs.ext.rules.RuleDiffLine> var10 = new ArrayList();
      int var11 = 0;
      int var9 = 0;

      while (var11 < var4 && var9 < var5) {
         if (((String)var2.get(var11)).equals(var3.get(var9))) {
            var10.add(new RuleDiffLine("unchanged", (String)var2.get(var11)));
            var11++;
            var9++;
         } else if (var6[var11 + 1][var9] >= var6[var11][var9 + 1]) {
            var10.add(new RuleDiffLine("removed", (String)var2.get(var11)));
            var11++;
         } else {
            var10.add(new RuleDiffLine("added", (String)var3.get(var9)));
            var9++;
         }
      }

      while (var11 < var4) {
         var10.add(new RuleDiffLine("removed", (String)var2.get(var11++)));
      }

      while (var9 < var5) {
         var10.add(new RuleDiffLine("added", (String)var3.get(var9++)));
      }

      return var10;
   }

   static List<String> a(String var0) {
      ArrayList<java.lang.String> var1 = new ArrayList();
      int var2 = var0.length();
      int var3 = 0;

      while (var3 < var2) {
         while (var3 < var2 && Character.isWhitespace(var0.charAt(var3))) {
            var3++;
         }

         int var4 = var3;

         while (var3 < var2 && !Character.isWhitespace(var0.charAt(var3))) {
            var3++;
         }

         if (var3 > var4) {
            var1.add(var0.substring(var4, var3));
         }
      }

      return var1;
   }

   static Map<String, ResultTreeNode> a(List<ResultTreeNode> var0) {
      HashMap<java.lang.String, io.karatelabs.ext.rules.ResultTreeNode> var1 = new HashMap();

      for (ResultTreeNode var3 : var0) {
         a(var3, var1);
      }

      return var1;
   }

   private static void a(ResultTreeNode var0, Map<String, ResultTreeNode> var1) {
      if (var0.a() != null) {
         var1.put(var0.a(), var0);
      }

      for (ResultTreeNode var3 : var0.k()) {
         a(var3, var1);
      }
   }

   public static List<ExecutionTraceSnapshotNode> b(List<ResultTreeNode> var0) {
      ArrayList<io.karatelabs.ext.rules.ExecutionTraceSnapshotNode> var1 = new ArrayList(var0.size());

      for (ResultTreeNode var3 : var0) {
         var1.add(a(var3));
      }

      return var1;
   }

   private static ExecutionTraceSnapshotNode a(ResultTreeNode var0) {
      ArrayList<io.karatelabs.ext.rules.ExecutionTraceSnapshotNode> var1 = new ArrayList(var0.k().size());

      for (ResultTreeNode var3 : var0.k()) {
         var1.add(a(var3));
      }

      return new ExecutionTraceSnapshotNode(var0.a(), var0.b(), var0.c(), var0.d(), var0.e(), var1);
   }

   public static Map<String, ExecutionTraceSnapshotNode> c(List<ExecutionTraceSnapshotNode> var0) {
      LinkedHashMap<java.lang.String, io.karatelabs.ext.rules.ExecutionTraceSnapshotNode> var1 = new LinkedHashMap();

      for (ExecutionTraceSnapshotNode var3 : var0) {
         a(var3, var1);
      }

      return var1;
   }

   private static void a(ExecutionTraceSnapshotNode var0, Map<String, ExecutionTraceSnapshotNode> var1) {
      if (var0.a() != null) {
         var1.put(var0.a(), var0);
      }

      for (ExecutionTraceSnapshotNode var3 : var0.f()) {
         a(var3, var1);
      }
   }

   static Map<String, String> a(Map<String, ExecutionTraceSnapshotNode> var0) {
      HashMap<java.lang.String, java.lang.String> var1 = new HashMap();

      for (ExecutionTraceSnapshotNode var3 : var0.values()) {
         for (ExecutionTraceSnapshotNode var5 : var3.f()) {
            var1.put(var5.a(), var3.a());
         }
      }

      return var1;
   }

   public record RuleTreeDiffResult(List<ResultTreeNode> a, Map<String, ExecutionTraceSnapshotNode> b) {

      public RuleTreeDiffResult(List<ResultTreeNode> a, Map<String, ExecutionTraceSnapshotNode> b) {
         this.a = a;
         this.b = b;
      }
   }
}
