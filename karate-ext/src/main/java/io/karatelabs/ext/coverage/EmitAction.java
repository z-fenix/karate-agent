package io.karatelabs.ext.coverage;

import java.time.Instant;
import java.util.Map;
import java.util.Set;

public record EmitAction(String id, Set<String> tags, Instant timestamp, String featurePath, String scenarioName, int scenarioLine, String slug, String a, String b, Map<String, Object> c) implements Action {


   @Override
   public String type() {
      return "emit";
   }

   @Override
   public String id() {
      return this.id;
   }

   @Override
   public Set<String> tags() {
      return this.tags;
   }

   @Override
   public Instant timestamp() {
      return this.timestamp;
   }

   @Override
   public String featurePath() {
      return this.featurePath;
   }

   @Override
   public String scenarioName() {
      return this.scenarioName;
   }

   @Override
   public int scenarioLine() {
      return this.scenarioLine;
   }

   @Override
   public String slug() {
      return this.slug;
   }

   public String a() {
      return this.a;
   }

   public String b() {
      return this.b;
   }

   public Map<String, Object> c() {
      return this.c;
   }
}
