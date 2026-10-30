package io.karatelabs.core;

import io.karatelabs.gherkin.Feature;
import io.karatelabs.gherkin.FeatureSection;
import io.karatelabs.gherkin.Scenario;
import io.karatelabs.gherkin.ScenarioOutline;
import io.karatelabs.gherkin.Tag;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class RunUtils {
   private RunUtils() {
   }

   public static String featureSlug(Feature feature) {
      return feature != null && feature.getResource() != null ? feature.getResource().getRelativePath() : "(unknown)";
   }

   public static String effectiveSlug(String stableId, Scenario scenario, Feature feature) {
      return stableId != null ? stableId : scenarioSlug(scenario, feature);
   }

   public static Map<String, Object> scenarioIdentity(Scenario scenario, String stableId) {
      Map<String, Object> data = new LinkedHashMap<>();
      data.put("name", scenario.getName());
      data.put("slug", effectiveSlug(stableId, scenario, scenario.getFeature()));
      data.put("description", scenario.getDescription());
      data.put("line", scenario.getLine());
      data.put("sectionIndex", scenario.getSection().getIndex());
      data.put("exampleIndex", scenario.getExampleIndex());
      Map<String, Object> exampleData = scenario.getExampleData();
      if (exampleData != null) {
         data.put("exampleData", exampleData);
      }

      return data;
   }

   public static String scenarioSlug(Scenario scenario, Feature feature) {
      if (scenario.isOutlineExample()) {
         ScenarioOutline outline = parentOutline(scenario);
         return outlineSlug(outline, feature) + ":" + scenario.getExampleIndex();
      } else {
         String featurePath = featureSlug(feature);
         String name = scenario.getName();
         return name != null && !name.isEmpty() ? featurePath + ":" + name : featurePath + "::L" + scenario.getLine();
      }
   }

   public static String outlineSlug(ScenarioOutline outline, Feature feature) {
      if (outline == null) {
         return featureSlug(feature) + "::outline";
      } else {
         String name = outline.getName();
         String featurePath = featureSlug(feature);
         return name != null && !name.isEmpty() ? featurePath + ":" + name : featurePath + "::L" + outline.getLine();
      }
   }

   public static ScenarioOutline parentOutline(Scenario scenario) {
      FeatureSection section = scenario.getSection();
      return section == null ? null : section.getScenarioOutline();
   }

   public static List<String> tagTexts(List<Tag> tags) {
      if (tags != null && !tags.isEmpty()) {
         List<String> out = new ArrayList<>(tags.size());

         for (Tag t : tags) {
            if (t != null && t.getText() != null && !t.getText().isEmpty()) {
               out.add(t.getText());
            }
         }

         return out;
      } else {
         return List.of();
      }
   }

   public static List<String> mergedTagTexts(List<Tag> a, List<Tag> b) {
      if (a != null && !a.isEmpty() || b != null && !b.isEmpty()) {
         List<String> out = new ArrayList<>();
         if (a != null) {
            for (Tag t : a) {
               if (t != null && t.getText() != null) {
                  out.add(t.getText());
               }
            }
         }

         if (b != null) {
            for (Tag tx : b) {
               if (tx != null && tx.getText() != null) {
                  out.add(tx.getText());
               }
            }
         }

         return out;
      } else {
         return List.of();
      }
   }
}
