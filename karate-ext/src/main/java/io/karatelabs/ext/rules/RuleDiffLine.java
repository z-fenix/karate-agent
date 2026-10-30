package io.karatelabs.ext.rules;

public record RuleDiffLine(String component1, String component2) {
   public static final String a = "unchanged";
   public static final String b = "removed";
   public static final String c = "added";

   public RuleDiffLine(String component1, String component2) {
      this.component1 = component1;
      this.component2 = component2;
   }

   public String a() {
      return this.component1;
   }

   public String b() {
      return this.component2;
   }
}
