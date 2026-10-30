package io.karatelabs.ext.max.jsapi;

import io.karatelabs.common.Json;
import io.karatelabs.core.SuiteResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class AgentTranscriptReplayer {
   private static final Logger a = LoggerFactory.getLogger(AgentTranscriptReplayer.class);

   private AgentTranscriptReplayer() {
   }

   static SuiteResult a(Path var0, Path var1, Map<String, Object> var2) {
      Object var3 = var2 == null ? null : var2.get("task");
      String var4 = ContainerScenario.a(var3 == null ? null : String.valueOf(var3), "Agent run");
      var var5 = a(var1.resolve("transcript.jsonl"));
      ArrayList var6 = new ArrayList();

      for (io.karatelabs.ext.max.jsapi.AgentTranscriptReplayer.ExecutionTranscript var8 : (Iterable<io.karatelabs.ext.max.jsapi.AgentTranscriptReplayer.ExecutionTranscript>) (Iterable<?>) (var5)) {
         io.karatelabs.ext.max.jsapi.AgentTranscriptReplayer.TranscriptEvidenceReplayer var9 = new io.karatelabs.ext.max.jsapi.AgentTranscriptReplayer.TranscriptEvidenceReplayer(var8.c);
         Runnable var10 = () -> {
            var9.a();
            if ("failed".equals(var8.d)) {
               throw new RuntimeException(var8.e != null ? var8.e : "the check failed");
            }
         };
         var6.add(
            var8.a == null
               ? new io.karatelabs.ext.max.jsapi.ContainerScenario.ScenarioSegment(var4, io.karatelabs.ext.coverage.collect.ExecutionFragment.ExecutionFragmentKind.REPLAY, 0, null, var10)
               : new io.karatelabs.ext.max.jsapi.ContainerScenario.ScenarioSegment(ContainerScenario.a("Check: " + var8.a, "Check"), io.karatelabs.ext.coverage.collect.ExecutionFragment.ExecutionFragmentKind.REPLAY, var8.b, var8.a, var10)
         );
      }

      return ContainerScenario.a(var0, var1, var4, var6);
   }

   private static List<io.karatelabs.ext.max.jsapi.AgentTranscriptReplayer.ExecutionTranscript> a(Path var0) {
      ArrayList var1 = new ArrayList();
      io.karatelabs.ext.max.jsapi.AgentTranscriptReplayer.ExecutionTranscript var2 = new io.karatelabs.ext.max.jsapi.AgentTranscriptReplayer.ExecutionTranscript(null, 0);
      var1.add(var2);
      if (!Files.isRegularFile(var0)) {
         return var1;
      } else {
         List var3;
         try {
            var3 = Files.readAllLines(var0);
         } catch (Exception var15) {
            a.debug("loop transcript read failed: {}", var15.getMessage());
            return var1;
         }

         LinkedHashMap var4 = new LinkedHashMap();
         io.karatelabs.ext.max.jsapi.AgentTranscriptReplayer.ExecutionTranscript var5 = null;

         for (String var7 : (Iterable<String>) (Iterable<?>) (var3)) {
            if (!var7.isBlank()) {
               try {
                  if (Json.of(var7).value() instanceof Map var8) {
                     String var16 = String.valueOf(var8.get("type"));
                     if ("execution-start".equals(var16)) {
                        String var10 = String.valueOf(var8.get("executionId"));
                        String var11 = var8.get("definitionId") == null ? var10 : String.valueOf(var8.get("definitionId"));
                        int var12 = var10.lastIndexOf(35);
                        int var13 = var12 < 0 ? var4.size() + 1 : Integer.parseInt(var10.substring(var12 + 1));
                        var5 = new io.karatelabs.ext.max.jsapi.AgentTranscriptReplayer.ExecutionTranscript(var11, var13);
                        var4.put(var10, var5);
                        var1.add(var5);
                     } else if ("execution-end".equals(var16)) {
                        io.karatelabs.ext.max.jsapi.AgentTranscriptReplayer.ExecutionTranscript var17 = (io.karatelabs.ext.max.jsapi.AgentTranscriptReplayer.ExecutionTranscript)var4.get(String.valueOf(var8.get("executionId")));
                        if (var17 != null && var17.d == null) {
                           var17.d = var8.get("outcome") == null ? null : String.valueOf(var8.get("outcome"));
                           var17.e = var8.get("error") == null ? null : String.valueOf(var8.get("error"));
                        }

                        var5 = null;
                     } else {
                        (var5 != null ? var5 : var2).c.add(var8);
                     }
                  }
               } catch (Exception var14) {
               }
            }
         }

         return var1;
      }
   }

   static final class TranscriptEvidenceReplayer {
      private final List<Map<?, ?>> a;

      TranscriptEvidenceReplayer(List<Map<?, ?>> var1) {
         this.a = var1;
      }

      void a() {
         for (Map var2 : this.a) {
            try {
               this.a(var2);
            } catch (Exception var4) {
            }
         }
      }

      private void a(Map<?, ?> var1) {
         String var2 = String.valueOf(var1.get("type"));
         switch (var2) {
            case "step":
               io.karatelabs.ext.common.EvidenceReporter.a(a(var1.get("name"), "step"), a(var1.get("status"), null), a(var1.get("note"), null));
               break;
            case "assertion":
               io.karatelabs.ext.common.EvidenceReporter.b(b(var1));
               break;
            case "screenshot":
               Object var4 = var1.get("filename");
               if (var4 != null) {
                  io.karatelabs.ext.common.EvidenceReporter.a("screenshots/" + var4, a(var1.get("title"), "screenshot"));
               }
         }
      }

      private static Map<String, Object> b(Map<?, ?> var0) {
         LinkedHashMap var1 = new LinkedHashMap();

         for (Entry var3 : var0.entrySet()) {
            String var4 = String.valueOf(var3.getKey());
            if (!"type".equals(var4) && !"ts".equals(var4) && !"iteration".equals(var4)) {
               var1.put(var4, var3.getValue());
            }
         }

         return var1;
      }

      private static String a(Object var0, String var1) {
         return var0 == null ? var1 : String.valueOf(var0);
      }
   }

   private static final class ExecutionTranscript {
      final String a;
      final int b;
      final List<Map<?, ?>> c = new ArrayList<>();
      String d;
      String e;

      ExecutionTranscript(String var1, int var2) {
         this.a = var1;
         this.b = var2;
      }
   }
}
