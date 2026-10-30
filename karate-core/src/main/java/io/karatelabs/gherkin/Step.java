package io.karatelabs.gherkin;

import io.karatelabs.common.Resource;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Step {
   public static final List<String> PREFIXES = Arrays.asList("*", "Given", "When", "Then", "And", "But");
   private final Feature feature;
   private final Scenario scenario;
   private final int index;
   private int line;
   private int endLine;
   private SourceSpan span;
   private List<String> comments;
   private List<SourceSpan> commentSpans = List.of();
   private String prefix;
   private String keyword;
   private String text;
   private String docString;
   private int docStringLine = -1;
   private Table table;

   public void parseAndUpdateFrom(String text) {
      String stepText = text.trim();
      boolean hasPrefix = PREFIXES.stream().anyMatch(stepText::startsWith);
      if (!hasPrefix) {
         text = "* " + stepText;
      }

      Resource resource = Resource.text("Feature:\nScenario:\n" + text);
      Feature tempFeature = Feature.read(resource);
      Step tempStep = null;
      if (!tempFeature.getSections().isEmpty()) {
         FeatureSection section = tempFeature.getSections().get(0);
         if (!section.isOutline() && section.getScenario() != null) {
            List<Step> steps = section.getScenario().getSteps();
            if (steps != null && !steps.isEmpty()) {
               tempStep = steps.get(0);
            }
         }
      }

      if (tempStep == null) {
         throw new RuntimeException("invalid expression: " + text);
      } else {
         this.prefix = tempStep.prefix;
         this.keyword = tempStep.keyword;
         this.text = tempStep.text;
         this.docString = tempStep.docString;
         this.docStringLine = tempStep.docStringLine;
         this.table = tempStep.table;
      }
   }

   public String getDebugInfo() {
      return this.feature + ":" + this.line;
   }

   public boolean isPrint() {
      return this.text != null && this.text.startsWith("print");
   }

   public boolean isPrefixStar() {
      return "*".equals(this.prefix);
   }

   public Feature getFeature() {
      return this.feature;
   }

   public Step(Feature feature, int index) {
      this.feature = feature;
      this.scenario = null;
      this.index = index;
   }

   public Step(Scenario scenario, int index) {
      this.scenario = scenario;
      this.feature = scenario.getFeature();
      this.index = index;
   }

   public static Step fromKarateJson(Scenario scenario, Map<String, Object> map) {
      int index = (Integer)map.get("index");
      Boolean background = (Boolean)map.get("background");
      if (background == null) {
         background = false;
      }

      Step step = background ? new Step(scenario.getFeature(), index) : new Step(scenario, index);
      int line = (Integer)map.get("line");
      step.setLine(line);
      Integer endLine = (Integer)map.get("endLine");
      if (endLine == null) {
         endLine = line;
      }

      step.setEndLine(endLine);
      if (map.get("comments") instanceof List) {
         step.setComments((List<String>)map.get("comments"));
      }

      step.setPrefix((String)map.get("prefix"));
      step.setKeyword((String)map.get("keyword"));
      step.setText((String)map.get("text"));
      step.setDocString((String)map.get("docString"));
      if (map.get("table") instanceof List) {
         List<Map<String, Object>> table = (List<Map<String, Object>>)map.get("table");
         if (table != null) {
            step.setTable(Table.fromKarateJson(table));
         }
      }

      return step;
   }

   public Map<String, Object> toJson() {
      Map<String, Object> map = new HashMap<>();
      if (this.isBackground()) {
         map.put("background", true);
      }

      map.put("index", this.index);
      map.put("line", this.line);
      if (this.endLine != this.line) {
         map.put("endLine", this.endLine);
      }

      if (this.comments != null && !this.comments.isEmpty()) {
         map.put("comments", this.comments);
      }

      map.put("prefix", this.prefix);
      if (this.keyword != null) {
         map.put("keyword", this.keyword);
      }

      map.put("text", this.text);
      if (this.docString != null) {
         map.put("docString", this.docString);
      }

      if (this.table != null) {
         map.put("table", this.table.toJson());
      }

      return map;
   }

   public boolean isBackground() {
      return this.scenario == null;
   }

   public boolean isOutline() {
      return this.scenario != null && this.scenario.isOutlineExample();
   }

   public int getIndex() {
      return this.index;
   }

   public int getLine() {
      return this.line;
   }

   public void setLine(int line) {
      this.line = line;
   }

   public int getLineCount() {
      return this.endLine - this.line + 1;
   }

   public int getEndLine() {
      return this.endLine;
   }

   public void setEndLine(int endLine) {
      this.endLine = endLine;
   }

   public SourceSpan getSpan() {
      return this.span;
   }

   public void setSpan(SourceSpan span) {
      this.span = span;
   }

   public String getPrefix() {
      return this.prefix;
   }

   public void setPrefix(String prefix) {
      this.prefix = prefix;
   }

   public String getKeyword() {
      return this.keyword;
   }

   public void setKeyword(String keyword) {
      this.keyword = keyword;
   }

   public String getText() {
      return this.text;
   }

   public void setText(String text) {
      this.text = text;
   }

   public String getDocString() {
      return this.docString;
   }

   public void setDocString(String docString) {
      this.docString = docString;
   }

   public int getDocStringLine() {
      return this.docStringLine;
   }

   public void setDocStringLine(int docStringLine) {
      this.docStringLine = docStringLine;
   }

   public Table getTable() {
      return this.table;
   }

   public void setTable(Table table) {
      this.table = table;
   }

   public List<String> getComments() {
      return this.comments;
   }

   public void setComments(List<String> comments) {
      this.comments = comments;
   }

   public List<SourceSpan> getCommentSpans() {
      return this.commentSpans;
   }

   public void setCommentSpans(List<SourceSpan> commentSpans) {
      this.commentSpans = commentSpans;
   }

   public boolean isFake() {
      return this.getIndex() == -1;
   }

   public boolean isSetup() {
      return this.scenario != null && this.scenario.isSetup();
   }

   @Override
   public String toString() {
      String temp = this.prefix;
      if (this.keyword != null) {
         temp = temp + " " + this.keyword;
      }

      temp = temp + " " + this.text;
      if (this.docString != null) {
         temp = temp + "\n\"\"\"\n" + this.docString + "\n\"\"\"";
      }

      if (this.table != null) {
         temp = temp + " " + this.table.toString();
      }

      return temp;
   }
}
