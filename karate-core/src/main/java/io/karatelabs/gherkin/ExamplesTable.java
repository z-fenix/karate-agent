package io.karatelabs.gherkin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ExamplesTable {
   private ScenarioOutline outline;
   private Table table;
   private List<Tag> tags;
   private int line;
   private SourceSpan span;
   private String name;

   public ExamplesTable() {
      this.tags = new ArrayList<>();
   }

   public ExamplesTable(ScenarioOutline outline, Table table) {
      this.outline = outline;
      this.table = table;
      this.tags = new ArrayList<>();
   }

   public ScenarioOutline getOutline() {
      return this.outline;
   }

   public List<Tag> getTags() {
      return this.tags;
   }

   public void setTags(List<Tag> tags) {
      this.tags = tags;
   }

   public Table getTable() {
      return this.table;
   }

   public void setTable(Table table) {
      this.table = table;
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

   public String getName() {
      return this.name;
   }

   public void setName(String name) {
      this.name = name;
   }

   public void setOutline(ScenarioOutline outline) {
      this.outline = outline;
   }

   public Map<String, Object> toJson() {
      Map<String, Object> map = new HashMap<>();
      List<String> tagStrings = new ArrayList<>();
      if (this.tags != null) {
         this.tags.forEach(tag -> tagStrings.add(tag.toString()));
      }

      map.put("tags", tagStrings);
      map.put("data", this.table.getRowsAsMapsConverted());
      return map;
   }
}
