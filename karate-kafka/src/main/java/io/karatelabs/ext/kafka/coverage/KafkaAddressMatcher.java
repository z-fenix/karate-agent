package io.karatelabs.ext.kafka.coverage;

import io.karatelabs.ext.contract.Contract;
import io.karatelabs.ext.contract.Operation;
import io.karatelabs.ext.coverage.Action;
import io.karatelabs.ext.coverage.AddressMatcher;
import io.karatelabs.ext.coverage.CoverageItem;
import io.karatelabs.ext.coverage.KafkaAction;
import io.karatelabs.ext.coverage.Warning;
import io.karatelabs.ext.coverage.graph.MatchKind;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class KafkaAddressMatcher implements AddressMatcher {
   public List<CoverageItem> buildItems(Contract contract) {
      List<CoverageItem> items = new ArrayList<>();

      for (Operation op : contract.operations()) {
         Map<String, Object> metadata = new LinkedHashMap<>();
         metadata.put("topic", op.address().primary());
         metadata.put("direction", op.address().qualifier());
         metadata.put("schema", op.ext().get("schema"));
         metadata.put("key", op.ext().get("key"));
         items.add(new CoverageItem(op.id(), op.id(), new LinkedHashSet(op.tags()), null, metadata));
      }

      return items;
   }

   public Map<String, Object> buildMetadata(Contract contract, List<CoverageItem> items) {
      Map<String, Object> meta = new LinkedHashMap<>();
      meta.put("specType", contract.ext().get("specType"));
      meta.put("title", contract.title());
      Object topics = contract.ext().getOrDefault("topics", List.of());
      meta.put("topics", new ArrayList((List)topics));
      Set<String> allTags = new LinkedHashSet<>();

      for (CoverageItem item : items) {
         allTags.addAll(item.tags());
      }

      meta.put("tags", new ArrayList<>(allTags));
      return meta;
   }

   public boolean accepts(Action action) {
      return action instanceof KafkaAction;
   }

   public CoverageItem match(Action action, List<CoverageItem> items) {
      if (action instanceof KafkaAction kafka) {
         String itemId = kafka.itemId();

         for (CoverageItem item : items) {
            if (itemId.equals(item.id())) {
               return item;
            }
         }

         return null;
      } else {
         return null;
      }
   }

   public List<Warning> warnings(Action action, CoverageItem matched, List<CoverageItem> items) {
      return List.of();
   }

   public MatchKind matchConfidence(Action action, CoverageItem matched, List<CoverageItem> items) {
      return MatchKind.EXACT;
   }

   public List<Warning> loadWarnings(List<CoverageItem> items) {
      return List.of();
   }

   public Map<String, Object> aggregate(List<Action> hits, CoverageItem item) {
      Map<String, Object> stats = new LinkedHashMap<>();
      stats.put("callCount", hits.size());
      long totalDuration = 0L;
      Map<String, Integer> kafkaStatuses = new LinkedHashMap<>();

      for (Action action : hits) {
         if (action instanceof KafkaAction kafka) {
            totalDuration += kafka.durationMs();
            if (kafka.status() != null && !kafka.status().isBlank()) {
               kafkaStatuses.merge(kafka.status(), 1, Integer::sum);
            }
         }
      }

      stats.put("avgDurationMs", hits.isEmpty() ? 0L : totalDuration / hits.size());
      if (!kafkaStatuses.isEmpty()) {
         stats.put("kafkaStatuses", kafkaStatuses);
      }

      stats.put("queryParamsUsed", new ArrayList());
      return stats;
   }
}
