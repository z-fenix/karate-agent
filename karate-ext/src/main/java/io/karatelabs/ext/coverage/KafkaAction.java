package io.karatelabs.ext.coverage;

import java.time.Instant;
import java.util.Set;

public record KafkaAction(String id, Set<String> tags, Instant timestamp, String featurePath, String scenarioName, int scenarioLine, String slug, String topic, String schema, String direction, String status, long durationMs, Object payload) implements Action {

   public KafkaAction(
      String var1,
      Set<String> var2,
      Instant var3,
      String var4,
      String var5,
      int var6,
      String var7,
      String var8,
      String var9,
      String var10,
      String var11,
      long var12
   ) {
      this(var1, var2, var3, var4, var5, var6, var7, var8, var9, var10, var11, var12, null);
   }


   @Override
   public String type() {
      return "kafka";
   }

   @Override
   public Object dimensionInput() {
      return this.payload;
   }

   public String itemId() {
      return this.topic + "#" + this.direction;
   }
}
