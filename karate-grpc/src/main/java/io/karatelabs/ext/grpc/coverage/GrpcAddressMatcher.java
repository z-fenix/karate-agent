package io.karatelabs.ext.grpc.coverage;

import io.karatelabs.ext.contract.Contract;
import io.karatelabs.ext.contract.GrpcBinding;
import io.karatelabs.ext.contract.Operation;
import io.karatelabs.ext.coverage.Action;
import io.karatelabs.ext.coverage.AddressMatcher;
import io.karatelabs.ext.coverage.CoverageItem;
import io.karatelabs.ext.coverage.GrpcAction;
import io.karatelabs.ext.coverage.Warning;
import io.karatelabs.ext.coverage.graph.MatchKind;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class GrpcAddressMatcher implements AddressMatcher {
   public List<CoverageItem> buildItems(Contract contract) {
      List<CoverageItem> items = new ArrayList<>();

      for (Operation op : contract.operations()) {
         GrpcBinding binding = op.ext().get("binding") instanceof GrpcBinding b ? b : null;
         Map<String, Object> metadata = new LinkedHashMap<>();
         metadata.put("service", binding == null ? null : binding.service());
         metadata.put("method", binding == null ? null : binding.method());
         metadata.put("package", op.ext().get("package"));
         metadata.put("requestType", op.ext().get("requestType"));
         metadata.put("responseType", op.ext().get("responseType"));
         Set<String> tags = new LinkedHashSet<>(op.tags());
         items.add(new CoverageItem(op.id(), op.id(), tags, null, metadata));
      }

      return items;
   }

   public Map<String, Object> buildMetadata(Contract contract, List<CoverageItem> items) {
      Map<String, Object> meta = new LinkedHashMap<>();
      meta.put("specType", contract.ext().get("specType"));
      meta.put("title", contract.title());
      Object services = contract.ext().getOrDefault("services", List.of());
      meta.put("services", new ArrayList((List)services));
      meta.put("tags", new ArrayList((List)services));
      return meta;
   }

   public boolean accepts(Action action) {
      return action instanceof GrpcAction;
   }

   public CoverageItem match(Action action, List<CoverageItem> items) {
      if (action instanceof GrpcAction grpc) {
         String itemId = grpc.itemId();

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
      Map<String, Integer> grpcStatuses = new LinkedHashMap<>();

      for (Action action : hits) {
         if (action instanceof GrpcAction grpc) {
            totalDuration += grpc.durationMs();
            grpcStatuses.merge(grpc.status(), 1, Integer::sum);
         }
      }

      stats.put("avgDurationMs", hits.isEmpty() ? 0L : totalDuration / hits.size());
      if (!grpcStatuses.isEmpty()) {
         stats.put("grpcStatuses", grpcStatuses);
      }

      stats.put("queryParamsUsed", new ArrayList());
      return stats;
   }
}
