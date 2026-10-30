package io.karatelabs.ext.coverage;

import java.time.Instant;
import java.util.Set;

public sealed interface Action permits HttpAction, GrpcAction, KafkaAction, EmitAction {
   String id();

   String type();

   Set<String> tags();

   Instant timestamp();

   String featurePath();

   String scenarioName();

   int scenarioLine();

   String slug();

   default Object dimensionInput() {
      return null;
   }
}
