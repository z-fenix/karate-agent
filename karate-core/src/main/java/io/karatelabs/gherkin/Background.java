package io.karatelabs.gherkin;

import java.util.List;

public class Background {
   public static final String TYPE = "background";
   public static final String KEYWORD = "Background";
   private int line;
   private SourceSpan span;
   private List<Step> steps;

   public int getLine() {
      return this.line;
   }

   public void setLine(int line) {
      this.line = line;
   }

   public SourceSpan getSpan() {
      return this.span;
   }

   public void setSpan(SourceSpan span) {
      this.span = span;
   }

   public List<Step> getSteps() {
      return this.steps;
   }

   public void setSteps(List<Step> steps) {
      this.steps = steps;
   }

   @Override
   public String toString() {
      return this.steps == null ? null : this.steps.toString();
   }
}
