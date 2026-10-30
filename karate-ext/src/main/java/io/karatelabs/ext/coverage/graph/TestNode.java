package io.karatelabs.ext.coverage.graph;

import java.util.Set;

public record TestNode(String a, String b, String c, int d, Set<String> e, io.karatelabs.ext.coverage.graph.TestNode.TestStatus f, io.karatelabs.ext.coverage.AssertionStrengthSummary g) {

   public TestNode(String var1, String var2, String var3, int var4, Set<String> var5, io.karatelabs.ext.coverage.graph.TestNode.TestStatus var6) {
      this(var1, var2, var3, var4, var5, var6, null);
   }


   public static String a(String var0, String var1) {
      return (var0 == null ? "" : var0) + "::" + (var1 == null ? "" : var1);
   }

   public static enum TestStatus {
      PASSED,
      FAILED,
      SKIPPED;
   }
}
