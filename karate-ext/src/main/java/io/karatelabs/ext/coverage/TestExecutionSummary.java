package io.karatelabs.ext.coverage;

import java.util.Set;

public record TestExecutionSummary(String a, int b, Set<String> c, String d, String e, String f, AssertionStrengthSummary g) {

   public TestExecutionSummary(String var1, int var2, Set<String> var3, String var4, String var5, String var6) {
      this(var1, var2, var3, var4, var5, var6, null);
   }

}
