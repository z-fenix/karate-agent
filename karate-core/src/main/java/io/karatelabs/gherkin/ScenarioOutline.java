package io.karatelabs.gherkin;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ScenarioOutline {
   private final Feature feature;
   private final FeatureSection section;
   private int line;
   private SourceSpan span;
   private List<Tag> tags;
   private String name;
   private String description;
   private List<Step> steps;
   private List<ExamplesTable> examplesTables;
   private int numScenarios = 0;

   public ScenarioOutline(Feature feature, FeatureSection section) {
      this.feature = feature;
      this.section = section;
   }

   public Scenario toScenario(String dynamicExpression, int exampleIndex, int updateLine, List<Tag> tagsForExamples) {
      Scenario s = new Scenario(this.feature, this.section, exampleIndex);
      s.setName(this.name);
      s.setDescription(this.description);
      s.setLine(updateLine);
      s.setDynamicExpression(dynamicExpression);
      if (this.tags != null || tagsForExamples != null) {
         List<Tag> temp = new ArrayList<>();
         if (this.tags != null) {
            temp.addAll(this.tags);
         }

         if (tagsForExamples != null) {
            temp.addAll(tagsForExamples);
         }

         s.setTags(temp);
      }

      List<Step> tempx = new ArrayList<>(this.steps.size());
      s.setSteps(tempx);

      for (Step original : this.steps) {
         Step step = new Step(s, original.getIndex());
         tempx.add(step);
         step.setLine(original.getLine());
         step.setEndLine(original.getEndLine());
         step.setPrefix(original.getPrefix());
         step.setKeyword(original.getKeyword());
         step.setText(original.getText());
         step.setDocString(original.getDocString());
         step.setDocStringLine(original.getDocStringLine());
         step.setTable(original.getTable());
         step.setComments(original.getComments());
         step.setSpan(original.getSpan());
         step.setCommentSpans(original.getCommentSpans());
      }

      this.numScenarios++;
      return s;
   }

   public FeatureSection getSection() {
      return this.section;
   }

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

   public List<Tag> getTags() {
      return this.tags;
   }

   public void setTags(List<Tag> tags) {
      this.tags = tags;
   }

   public String getName() {
      return this.name;
   }

   public void setName(String name) {
      this.name = name;
   }

   public String getDescription() {
      return this.description;
   }

   public void setDescription(String description) {
      this.description = description;
   }

   public List<Step> getSteps() {
      return this.steps;
   }

   public void setSteps(List<Step> steps) {
      this.steps = steps;
   }

   public List<ExamplesTable> getExamplesTables() {
      return this.examplesTables;
   }

   public int getNumExampleTables() {
      return this.examplesTables.size();
   }

   public List<Map<String, Object>> getAllExampleData() {
      List<Map<String, Object>> exampleData = new ArrayList<>();
      this.examplesTables.forEach(table -> exampleData.add(table.toJson()));
      return exampleData;
   }

   public void setExamplesTables(List<ExamplesTable> examplesTables) {
      this.examplesTables = examplesTables;
   }

   public int getNumScenarios() {
      return this.numScenarios;
   }
}
