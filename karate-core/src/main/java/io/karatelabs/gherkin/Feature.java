package io.karatelabs.gherkin;

import io.karatelabs.common.Resource;
import io.karatelabs.common.StringUtils;
import java.io.File;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HexFormat;
import java.util.List;

public class Feature {
   public static final String KEYWORD = "Feature";
   public static final String KARATE_JSON_SUFFIX = ".karate-json.txt";
   private final Resource resource;
   private int line;
   private List<SourceSpan> commentSpans = List.of();
   private List<Tag> tags;
   private String name;
   private String description;
   private Background background;
   private List<FeatureSection> sections = new ArrayList<>();
   private volatile String sourceHash;

   public static Feature read(String path) {
      return read(Resource.path(path));
   }

   public static Feature read(File file) {
      return read(Resource.from(file.toPath()));
   }

   public static Feature read(Resource resource) {
      GherkinParser parser = new GherkinParser(resource);
      return parser.parse();
   }

   public Feature(Resource resource) {
      this.resource = resource;
   }

   public Resource getResource() {
      return this.resource;
   }

   public String getSourceHash() {
      String temp = this.sourceHash;
      if (temp == null && this.resource != null) {
         try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(this.resource.getBytes());
            temp = HexFormat.of().formatHex(digest);
            this.sourceHash = temp;
         } catch (Exception var3) {
            return null;
         }
      }

      return temp;
   }

   public String getPackageQualifiedName() {
      return this.resource.getPackageQualifiedName();
   }

   public String getKarateJsonFileName() {
      return this.getPackageQualifiedName() + ".karate-json.txt";
   }

   public boolean isBackgroundPresent() {
      return this.background != null && this.background.getSteps() != null;
   }

   public String getNameAndDescription() {
      String temp = "";
      if (!StringUtils.isBlank(this.name)) {
         temp = temp + this.name;
      }

      if (!StringUtils.isBlank(this.description)) {
         if (!temp.isEmpty()) {
            temp = temp + " | ";
         }

         temp = temp + this.description;
      }

      return temp;
   }

   public String getNameForReport() {
      return this.name == null ? "[" + this.resource.getFileNameWithoutExtension() + "]" : "[" + this.resource.getFileNameWithoutExtension() + "] " + this.name;
   }

   public Step findStepByLine(int line) {
      for (FeatureSection section : this.sections) {
         for (Step step : section.isOutline() ? section.getScenarioOutline().getSteps() : section.getScenario().getStepsIncludingBackground()) {
            if (step.getLine() == line) {
               return step;
            }
         }
      }

      return null;
   }

   public Scenario getSetup(String name) {
      for (FeatureSection section : this.sections) {
         if (!section.isOutline()) {
            Scenario scenario = section.getScenario();
            List<Tag> foundTags = scenario.getTags();
            if (foundTags != null) {
               for (Tag tag : foundTags) {
                  if ("setup".equals(tag.getName())) {
                     if (name == null) {
                        return scenario;
                     }

                     if (tag.getValues().contains(name)) {
                        return scenario;
                     }
                  }
               }
            }
         }
      }

      return null;
   }

   public void addSection(FeatureSection section) {
      section.setIndex(this.sections.size());
      this.sections.add(section);
   }

   public void addTag(Tag tag) {
      if (this.tags == null) {
         this.tags = new ArrayList<>();
      }

      this.tags.add(tag);
   }

   public List<Tag> getTags() {
      return this.tags == null ? Collections.emptyList() : this.tags;
   }

   public void setTags(List<Tag> tags) {
      this.tags = tags;
   }

   public FeatureSection getSection(int sectionIndex) {
      return this.sections.get(sectionIndex);
   }

   public int getLine() {
      return this.line;
   }

   public void setLine(int line) {
      this.line = line;
   }

   public List<SourceSpan> getCommentSpans() {
      return this.commentSpans;
   }

   public void setCommentSpans(List<SourceSpan> commentSpans) {
      this.commentSpans = commentSpans;
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

   public Background getBackground() {
      return this.background;
   }

   public void setBackground(Background background) {
      this.background = background;
   }

   public List<FeatureSection> getSections() {
      return this.sections;
   }

   public void setSections(List<FeatureSection> sections) {
      this.sections = sections;
   }

   @Override
   public String toString() {
      return this.resource.getRelativePath();
   }
}
