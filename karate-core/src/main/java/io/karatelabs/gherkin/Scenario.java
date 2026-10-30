package io.karatelabs.gherkin;

import io.karatelabs.common.Resource;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Scenario {
   private final Feature feature;
   private final FeatureSection section;
   private final int exampleIndex;
   private int line;
   private SourceSpan span;
   private List<Tag> tags;
   private String name;
   private String description;
   private List<Step> steps;
   private Map<String, Object> exampleData;
   private String dynamicExpression;
   private Boolean selected;

   public Scenario(Feature feature, FeatureSection section, int exampleIndex) {
      this.feature = feature;
      this.section = section;
      this.exampleIndex = exampleIndex;
   }

   public static Scenario createError(Feature feature, String errorName, int line) {
      FeatureSection section = new FeatureSection();
      section.setIndex(0);
      Scenario scenario = new Scenario(feature, section, -1);
      scenario.setName(errorName);
      scenario.setLine(line);
      scenario.setSteps(Collections.emptyList());
      section.setScenario(scenario);
      return scenario;
   }

   public boolean isEqualTo(Scenario other) {
      return other.section.getIndex() == this.section.getIndex() && other.exampleIndex == this.exampleIndex;
   }

   public String getNameAndDescription() {
      String temp = "";
      if (this.name != null) {
         temp = temp + this.name;
      }

      if (this.description != null) {
         if (!temp.isEmpty()) {
            temp = temp + " ";
         }

         temp = temp + this.description;
      }

      return temp;
   }

   public String getRefIdAndName() {
      return this.name == null ? this.getRefId() : this.getRefId() + " " + this.name;
   }

   public Scenario copy(int exampleIndex) {
      Scenario s = new Scenario(this.feature, this.section, exampleIndex);
      s.name = this.name;
      s.description = this.description;
      s.tags = this.tags;
      s.line = this.line;
      s.span = this.span;
      s.dynamicExpression = this.dynamicExpression;
      s.steps = new ArrayList<>(this.steps.size());

      for (Step step : this.steps) {
         Step temp = new Step(s, step.getIndex());
         s.steps.add(temp);
         temp.setLine(step.getLine());
         temp.setEndLine(step.getEndLine());
         temp.setPrefix(step.getPrefix());
         temp.setKeyword(step.getKeyword());
         temp.setText(step.getText());
         temp.setDocString(step.getDocString());
         temp.setDocStringLine(step.getDocStringLine());
         temp.setTable(step.getTable());
         temp.setComments(step.getComments());
         temp.setSpan(step.getSpan());
         temp.setCommentSpans(step.getCommentSpans());
      }

      return s;
   }

   public void replace(String token, String value) {
      if (value != null) {
         if (this.name != null) {
            this.name = this.name.replace(token, value);
         }

         for (Step step : this.steps) {
            String text = step.getText();
            if (text != null) {
               step.setText(text.replace(token, value));
            }

            String docString = step.getDocString();
            if (docString != null) {
               step.setDocString(docString.replace(token, value));
            }

            Table table = step.getTable();
            if (table != null) {
               step.setTable(table.replace(token, value));
            }
         }
      }
   }

   public Step getStepByLine(int line) {
      for (Step step : this.getStepsIncludingBackground()) {
         if (step.getLine() == line) {
            return step;
         }
      }

      return null;
   }

   public String getRefId() {
      int num = this.section.getIndex() + 1;
      String meta = "[" + num;
      if (this.exampleIndex != -1) {
         meta = meta + "." + (this.exampleIndex + 1);
      }

      return meta + ":" + this.line + "]";
   }

   public String getDebugInfo() {
      return this.feature + ":" + this.line;
   }

   public String getUniqueId() {
      Resource resource = this.feature.getResource();
      if (resource == null) {
         return "";
      } else {
         String id = resource.getPackageQualifiedName() + "_" + (this.section.getIndex() + 1);
         return this.exampleIndex == -1 ? id : id + "_" + (this.exampleIndex + 1);
      }
   }

   public List<Step> getBackgroundSteps() {
      return this.feature.isBackgroundPresent() ? this.feature.getBackground().getSteps() : Collections.emptyList();
   }

   public List<Step> getStepsIncludingBackground() {
      List<Step> background = this.feature.isBackgroundPresent() ? this.feature.getBackground().getSteps() : null;
      int count = background == null ? this.steps.size() : this.steps.size() + background.size();
      List<Step> temp = new ArrayList<>(count);
      if (background != null) {
         temp.addAll(background);
      }

      temp.addAll(this.steps);
      return temp;
   }

   public FeatureSection getSection() {
      return this.section;
   }

   public Feature getFeature() {
      return this.feature;
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

   public Boolean getSelected() {
      return this.selected;
   }

   public void setSelected(Boolean selected) {
      this.selected = selected;
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

   public boolean isOutlineExample() {
      return this.exampleIndex != -1;
   }

   public boolean isDynamic() {
      return this.dynamicExpression != null;
   }

   public String getDynamicExpression() {
      return this.dynamicExpression;
   }

   public void setDynamicExpression(String dynamicExpression) {
      this.dynamicExpression = dynamicExpression;
   }

   public Map<String, Object> getExampleData() {
      return this.exampleData;
   }

   public void setExampleData(Map<String, Object> exampleData) {
      this.exampleData = exampleData;
   }

   public int getExampleIndex() {
      return this.exampleIndex;
   }

   @Override
   public String toString() {
      return this.feature.toString() + this.getRefId();
   }

   public URI getUriToLineNumber() {
      return URI.create(this.feature.getResource().getUri() + "?line=" + this.line);
   }

   public boolean isSetup() {
      return this.getTags() != null && this.getTags().stream().map(Tag::getName).anyMatch(t -> t.equals("setup"));
   }

   public boolean isFail() {
      return this.getTagsEffective().stream().map(Tag::getName).anyMatch("fail"::equals);
   }

   public List<Tag> getTagsEffective() {
      List<Tag> featureTags = this.feature != null ? this.feature.getTags() : null;
      boolean noFeature = featureTags == null || featureTags.isEmpty();
      boolean noScenario = this.tags == null || this.tags.isEmpty();
      if (noFeature) {
         return noScenario ? Collections.emptyList() : this.tags;
      } else if (noScenario) {
         return featureTags;
      } else {
         List<Tag> merged = new ArrayList<>(featureTags);
         Set<String> seen = new HashSet<>();

         for (Tag t : featureTags) {
            seen.add(t.getText());
         }

         for (Tag t : this.tags) {
            if (seen.add(t.getText())) {
               merged.add(t);
            }
         }

         return merged;
      }
   }

   public boolean isIgnore() {
      return this.getTagsEffective().stream().map(Tag::getName).anyMatch("ignore"::equals);
   }
}
