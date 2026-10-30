package io.karatelabs.ext.pair;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public final class PairRunEvidenceLoader {
   public static final String EVENTS_JSONL_PATH = "karate-json/karate-events.jsonl";

   private PairRunEvidenceLoader() {
   }

   public static PairRunEvidence load(String var0, String var1, Path var2) {
      Path var3 = var2.resolve("karate-json/karate-events.jsonl");
      if (!Files.isRegularFile(var3)) {
         return io.karatelabs.ext.pair.PairRunEvidence.failedLeg(var0, var1, var2, "no event stream at " + var3 + " — the leg produced no run to compare");
      } else {
         io.karatelabs.ext.common.KarateEventLogParser.KarateEventLogParseResult var4 = io.karatelabs.ext.common.KarateEventLogParser.a(var3);
         if (var4.b().isEmpty()) {
            return io.karatelabs.ext.pair.PairRunEvidence.failedLeg(
               var0,
               var1,
               var2,
               "no scenario could be read from "
                  + var3
                  + (
                     var4.c() == 0
                        ? " — the stream carries no completed feature, so the run produced no result to compare"
                        : " — "
                           + var4.d()
                           + " of "
                           + var4.c()
                           + " feature record(s) could not be parsed, so every observation this leg made was lost. This is a reader/stream mismatch, not a quiet run: an empty divergence set would read as evidence of fidelity."
                  )
            );
         } else {
            ArrayList<io.karatelabs.ext.pair.PairRunEvidence.ScenarioRunEvidence> var5 = new ArrayList();

            for (io.karatelabs.ext.common.KarateEventLogParser.KarateScenarioResult var7 : var4.b()) {
               var5.add(convertScenario(var7));
            }

            return new PairRunEvidence(var0, var1, var2, var5, null);
         }
      }
   }

   private static io.karatelabs.ext.pair.PairRunEvidence.ScenarioRunEvidence convertScenario(io.karatelabs.ext.common.KarateEventLogParser.KarateScenarioResult var0) {
      ArrayList<io.karatelabs.ext.pair.PairRunEvidence.ScenarioStepEvidence> var1 = new ArrayList();
      ArrayList<io.karatelabs.ext.pair.PairRunEvidence.RecordedHttpExchange> var2 = new ArrayList();

      for (io.karatelabs.ext.common.KarateEventLogParser.KarateStepResult var4 : var0.i()) {
         var1.add(new io.karatelabs.ext.pair.PairRunEvidence.ScenarioStepEvidence(var4.b(), var4.a(), var4.h(), var4.j(), var4.i()));

         for (Map var6 : var4.k()) {
            var2.add(convertExchange(var4.b(), var4.j(), var6));
         }
      }

      return new io.karatelabs.ext.pair.PairRunEvidence.ScenarioRunEvidence(var0.b(), var0.d(), var0.e(), var0.a(), var0.f(), var0.g(), var0.h(), var1, var2);
   }

   private static io.karatelabs.ext.pair.PairRunEvidence.RecordedHttpExchange convertExchange(int var0, boolean var1, Map<String, Object> var2) {
      var var3 = mapOrEmpty(var2.get("request"));
      var var4 = mapOrEmpty(var2.get("response"));
      return new io.karatelabs.ext.pair.PairRunEvidence.RecordedHttpExchange(
         var0,
         var1,
         stringOr(var3.get("method"), "").toUpperCase(),
         stringOr(var3.get("url"), ""),
         stringOr(var3.get("path"), ""),
         intOrZero(var4.get("status")),
         normalizeHeaders(var4.get("headers")),
         var4.get("body"),
         intOrZero(var4.get("responseTime"))
      );
   }

   private static Map<String, Object> normalizeHeaders(Object var0) {
      LinkedHashMap<java.lang.String, java.lang.Object> var1 = new LinkedHashMap();
      if (var0 instanceof Map var2) {
         for (Entry var4 : (Iterable<Entry>) (Iterable<?>) (var2.entrySet())) {
            String var5 = String.valueOf(var4.getKey()).toLowerCase();
            Object var6 = var4.getValue();
            if (var6 instanceof List var7) {
               var1.put(
                  var5,
                  var7.size() == 1 ? String.valueOf(var7.get(0)) : var7.stream().map(String::valueOf).reduce((var0x, var1x) -> var0x + ", " + var1x).orElse("")
               );
            } else if (var6 != null) {
               var1.put(var5, String.valueOf(var6));
            }
         }

         return var1;
      } else {
         return var1;
      }
   }

   private static Map<String, Object> mapOrEmpty(Object var0) {
      return var0 instanceof Map var1 ? var1 : Map.of();
   }

   private static String stringOr(Object var0, String var1) {
      return var0 == null ? var1 : String.valueOf(var0);
   }

   private static int intOrZero(Object var0) {
      return var0 instanceof Number var1 ? var1.intValue() : 0;
   }
}
