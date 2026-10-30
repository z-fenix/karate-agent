package io.karatelabs.gherkin;

public class MatchExpression {
   private final boolean each;
   private final String actualExpr;
   private final String operator;
   private final String expectedExpr;

   public MatchExpression(boolean each, String actualExpr, String operator, String expectedExpr) {
      this.each = each;
      this.actualExpr = actualExpr;
      this.operator = operator;
      this.expectedExpr = expectedExpr;
   }

   public boolean isEach() {
      return this.each;
   }

   public String getActualExpr() {
      return this.actualExpr;
   }

   public String getOperator() {
      return this.operator;
   }

   public String getExpectedExpr() {
      return this.expectedExpr;
   }

   public String getMatchTypeName() {
      String var2 = this.operator;

      String base = switch (var2) {
         case "==" -> "EQUALS";
         case "!=" -> "NOT_EQUALS";
         case "contains" -> "CONTAINS";
         case "!contains" -> "NOT_CONTAINS";
         case "contains deep" -> "CONTAINS_DEEP";
         case "contains only" -> "CONTAINS_ONLY";
         case "contains only deep" -> "CONTAINS_ONLY_DEEP";
         case "contains any" -> "CONTAINS_ANY";
         case "contains any deep" -> "CONTAINS_ANY_DEEP";
         case "within" -> "WITHIN";
         case "!within" -> "NOT_WITHIN";
         default -> throw new RuntimeException("Unknown operator: " + this.operator);
      };
      return this.each ? "EACH_" + base : base;
   }

   @Override
   public String toString() {
      return (this.each ? "each " : "") + this.actualExpr + " " + this.operator + " " + this.expectedExpr;
   }
}
