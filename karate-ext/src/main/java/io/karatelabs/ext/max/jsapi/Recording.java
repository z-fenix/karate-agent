package io.karatelabs.ext.max.jsapi;

import io.karatelabs.common.Json;
import io.karatelabs.driver.cdp.CdpDriver;
import io.karatelabs.ext.common.jsapi.JsApi;
import io.karatelabs.ext.common.jsapi.JsDispatch;
import io.karatelabs.ext.max.run.LiveRunSink;
import io.karatelabs.ext.robot.Bot;
import io.karatelabs.ext.robot.RevealRecorder;
import io.karatelabs.ext.robot.recording.TrafficRecorder;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Recording extends JsDispatch {
   private static final Logger logger = LoggerFactory.getLogger(Recording.class);
   private static final Set<String> REPLAYABLE = Set.of("nav", "click", "type", "select", "check", "uncheck", "submit", "key", "hover");
   private final Harness harness;
   private final Supplier<Bot> liveBot;
   private final Function<String, io.karatelabs.ext.agent.events.AgentEventEmitter> sinkFactory;
   private final Runnable onActivity;
   long fenceMillis = 10000L;
   private volatile RevealRecorder engine;
   private volatile Bot recordingBot;
   private volatile io.karatelabs.ext.agent.events.AgentEventEmitter recordingSink;
   private volatile TrafficRecorder traffic;
   private String recordingRunId;
   private String recordingName;
   private BiConsumer<String, String> evidence;
   private Supplier<io.karatelabs.ext.common.secrets.SecretStore> secrets;
   private io.karatelabs.ext.agent.llm.SecretRedactor sanitizer;
   BiFunction<Bot, Path, TrafficRecorder> captureFactory;
   private boolean noProject;
   private static final String DELTA_PII_HEADER = "{\"_warning\":\"raw reveal-delta evidence — may contain on-screen text/PII; local-only, never served (D209)\"}";
   private static final Pattern SECRET_REF = Pattern.compile("secret\\('[A-Za-z_][A-Za-z0-9_]*'\\)");
   private static final Pattern OVERLAY = Pattern.compile("overlay|backdrop|mask|spinner|loading|busy|progress");

   public Recording(Harness var1, Supplier<Bot> var2, Function<String, io.karatelabs.ext.agent.events.AgentEventEmitter> var3, Runnable var4) {
      this.harness = var1;
      this.liveBot = var2;
      this.sinkFactory = var3;
      this.onActivity = var4;
      var1.onRecordingEnd(this::endIfActive);
      this.fn("help", var0 -> JsApi.helpTopic(help(), var0));
      this.fn("start", var1x -> this.start(recordName(var1x), wantsDelta(var1x)));
      this.fn("note", var1x -> this.note(JsApi.str(var1x, 0)));
      this.fn("stop", var1x -> this.stop());
      this.fn("status", var1x -> this.status());
      this.fn("get", var1x -> this.get(JsApi.arg(var1x, 0) instanceof String var2x ? var2x : null));
   }

   public Recording withNoProject(boolean var1) {
      this.noProject = var1;
      return this;
   }

   public Recording withCapture(Supplier<io.karatelabs.ext.common.secrets.SecretStore> var1, io.karatelabs.ext.agent.llm.SecretRedactor var2) {
      this.secrets = var1;
      this.sanitizer = var2;
      return this;
   }

   public Recording withEvidence(BiConsumer<String, String> var1) {
      this.evidence = var1;
      return this;
   }

   private static String recordName(Object[] var0) {
      for (Object var4 : var0) {
         if (var4 instanceof String var5) {
            return var5;
         }
      }

      return null;
   }

   private static boolean wantsDelta(Object[] var0) {
      Object[] var1 = var0;
      int var2 = var0.length;

      for (int var3 = 0; var3 < var2; var3++) {
         if (var1[var3] instanceof Map var5) {
            Object var6 = var5.get("delta");
            if (var6 == null) {
               var6 = var5.get("advanced");
            }

            if (Boolean.TRUE.equals(var6) || "true".equals(String.valueOf(var6))) {
               return true;
            }
         }
      }

      return false;
   }

   Map<String, Object> start(String var1, boolean var2) {
      if (this.noProject) {
         return JsApi.noProjectSelected("Recording.start");
      } else {
         io.karatelabs.ext.common.keycheck.Main.requireProduct("recording", "robot");
         Bot var3 = this.liveBot.get();
         if (var3 == null || !var3.isAlive()) {
            return JsApi.missing("no live browser — create one first: var bot = Robot.create()");
         } else if (this.engine != null) {
            return this.status();
         } else {
            String var4 = this.harness.liveRunId();
            if (var4 != null && Files.exists(this.harness.runDir(var4).resolve("traffic.jsonl"))) {
               LinkedHashMap var12 = new LinkedHashMap();
               var12.put("error", "this run already holds a recording; start a new run");
               var12.put("code", "RECORDED");
               var12.put("status", 409);
               return var12;
            } else {
               var var5 = this.harness.startRecord(var1);
               if (var5.containsKey("error")) {
                  return var5;
               } else {
                  String var6 = String.valueOf(var5.get("runId"));
                  io.karatelabs.ext.agent.events.AgentEventEmitter var7 = this.sinkFactory.apply(var6);
                  RevealRecorder var8 = new RevealRecorder(var7, this.onActivity);
                  TrafficRecorder var9 = this.captureFactory != null ? this.captureFactory.apply(var3, this.harness.runDir(var6)) : null;
                  if (var9 == null && this.sanitizer != null && var3.getAgentDriver().getDriver() instanceof CdpDriver var10) {
                     var9 = new TrafficRecorder(this.harness.runDir(var6), io.karatelabs.ext.robot.recording.TrafficRecorder.TrafficRecorderLimits.a, io.karatelabs.ext.robot.recording.CdpPageApiFactory.a(var10), this.sanitizer);
                     io.karatelabs.ext.robot.recording.CdpPageApiFactory.a(var9, var10);
                  }

                  if (var9 != null) {
                     var8.a(var9, this.secrets);
                  }

                  if (var2) {
                     Path var13 = this.harness.runDir(var6);
                     var8.a((var1x, var2x) -> appendLocal(var13, var1x, var2x));
                  }

                  var3.getAgentDriver().startRecording(var8::a, var8::c, var2);
                  this.engine = var8;
                  this.recordingBot = var3;
                  this.recordingSink = var7;
                  this.traffic = var9;
                  this.recordingRunId = var6;
                  this.recordingName = var1;
                  logger.debug("recording started on run {}", var6);
                  LinkedHashMap var14 = new LinkedHashMap();
                  var14.put("runId", var6);
                  var14.put("recording", true);
                  if (var1 != null && !var1.isBlank()) {
                     var14.put("name", var1);
                  }

                  if (var2) {
                     var14.put("advanced", "reveal-delta capture on — raw evidence to reveal-deltas.local.jsonl (local-only, never served)");
                  }

                  return JsApi.next(var14, "Recording.status()", "Recording.note('label this step')", "Recording.stop()");
               }
            }
         }
      }
   }

   private static void appendLocal(Path var0, String var1, String var2) {
      try {
         Path var3 = var0.resolve(var1);
         boolean var4 = !Files.exists(var3);
         Files.createDirectories(var0);
         StringBuilder var5 = new StringBuilder();
         if (var4) {
            var5.append("{\"_warning\":\"raw reveal-delta evidence — may contain on-screen text/PII; local-only, never served (D209)\"}").append('\n');
         }

         var5.append(var2).append('\n');
         Files.writeString(var3, var5, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
      } catch (IOException var6) {
         logger.debug("reveal-delta sidecar append failed: {}", var6.getMessage());
      }
   }

   Map<String, Object> note(String var1) {
      RevealRecorder var2 = this.engine;
      if (var2 == null) {
         return JsApi.missing("not recording — Recording.start() first");
      } else if (var1 != null && !var1.isBlank()) {
         var2.a(var1);
         LinkedHashMap var3 = new LinkedHashMap();
         var3.put("noted", var1);
         return var3;
      } else {
         return JsApi.missing("Recording.note(text) requires the annotation text");
      }
   }

   Map<String, Object> stop() {
      RevealRecorder var1 = this.engine;
      int var2 = var1 != null ? var1.d() : 0;
      LinkedHashMap var3 = new LinkedHashMap();
      if (var1 != null) {
         var3.putAll(this.shutdown(var1, true, "stop"));
      }

      var var4 = this.harness.stopRecord();
      var3.putIfAbsent("stopped", true);
      var3.put("actions", var2);
      if (var4.get("runId") != null) {
         var3.put("runId", var4.get("runId"));
      }

      return var3;
   }

   private synchronized Map<String, Object> shutdown(RevealRecorder var1, boolean var2, String var3) {
      LinkedHashMap var4 = new LinkedHashMap();
      if (this.engine != var1) {
         return var4;
      } else {
         Bot var5 = this.recordingBot;
         TrafficRecorder var6 = this.traffic;
         io.karatelabs.ext.agent.events.AgentEventEmitter var7 = this.recordingSink;
         Runnable var8 = () -> {
            if (var6 != null) {
               var6.b();
            }

            var1.a();
         };
         ArrayList var9 = new ArrayList();

         try {
            if (var5 != null && var5.isAlive()) {
               var5.getAgentDriver().stopRecording(var2, var8);
            } else {
               var8.run();
            }
         } catch (Exception var22) {
            var8.run();
            var9.add(var22.getMessage());
         }

         if (!var1.a(this.fenceMillis)) {
            var9.add("the recorded actions did not all reach the transcript (" + (var1.d() - var1.e()) + " abandoned)");
         }

         if (var7 instanceof LiveRunSink var10 && !var10.drain(this.fenceMillis)) {
            var9.add("the recorded actions were not all written to the transcript");
         }

         if (var6 != null) {
            try {
               var4.put("capture", var6.a(var3, var9));
            } catch (RuntimeException var20) {
               logger.warn("traffic capture not finalized: {}", var20.toString());
               var9.add("the traffic capture was not written: " + var20.getMessage());
            } finally {
               if (var5 != null && var5.getAgentDriver().getDriver() instanceof CdpDriver var13) {
                  io.karatelabs.ext.robot.recording.CdpPageApiFactory.b(var6, var13);
               }
            }

            if (var4.get("capture") != null && this.evidence != null) {
               try {
                  this.evidence.accept(this.recordingRunId, this.recordingName);
               } catch (RuntimeException var19) {
                  logger.warn("the recording's calls were not queued as evidence: {}", var19.toString());
               }
            }

            if (var4.get("capture") instanceof Map var23 && var23.get("attribution") != null) {
               var4.put("attribution", var23.get("attribution"));
            }
         }

         if (!var1.b().isEmpty()) {
            var4.put("secretErrors", var1.b());
         }

         if (!var9.isEmpty()) {
            var4.put("stopped", false);
            var4.put("error", "Stop failed: " + String.join("; ", var9));
         }

         this.engine = null;
         this.recordingBot = null;
         this.recordingSink = null;
         this.traffic = null;
         return var4;
      }
   }

   void endIfActive() {
      RevealRecorder var1 = this.engine;
      if (var1 != null) {
         this.shutdown(var1, false, "run-settled");
         logger.debug("recording ended — its live run settled");
      }
   }

   public void endIfClosing(Bot var1) {
      RevealRecorder var2 = this.engine;
      if (var2 != null && var1 == this.recordingBot) {
         this.shutdown(var2, false, "run-settled");
      }
   }

   RevealRecorder engine() {
      return this.engine;
   }

   Map<String, Object> status() {
      RevealRecorder var1 = this.engine;
      LinkedHashMap var2 = new LinkedHashMap();
      var2.put("recording", var1 != null);
      if (var1 != null) {
         var2.put("runId", this.harness.liveRunId());
         var2.put("actions", var1.d());
      } else {
         var2.put("hint", "Recording.start([name]) to begin — needs a live browser (var bot = Robot.create())");
      }

      return var2;
   }

   Map<String, Object> get(String var1) {
      String var2 = var1 != null && !var1.isBlank() ? var1.trim() : this.harness.liveRunId();
      if (var2 == null) {
         return JsApi.missing("Recording.get([runId]) needs a run id — see Harness.runs() (or call it right after recording to use the active run)");
      } else {
         Path var3 = this.harness.runDir(var2).resolve("transcript.jsonl");
         if (!Files.isRegularFile(var3)) {
            return JsApi.notFound("no recording transcript for run '" + var2 + "' — see Harness.runs()");
         } else {
            io.karatelabs.ext.max.jsapi.Recording.RecordingDigest var4 = digest(var3);
            LinkedHashMap var5 = new LinkedHashMap();
            var5.put("runId", var2);
            if (var4.e() != null) {
               var5.put("startUrl", var4.e());
            }

            var5.put("steps", var4.b());
            var5.put("notes", var4.c());
            var5.put("captured", var4.d());
            var5.put("replayable", var4.b().size());
            var var6 = revealPlansForRun(this.harness.runDir(var2));
            if (!var6.isEmpty()) {
               var5.put("revealPlans", var6);
            }

            if (var4.b().isEmpty()) {
               var5.put(
                  "hint",
                  "no replayable actions captured — was the page driven while recording? (Recording.start() arms capture; drive the app; Recording.stop())"
               );
               return var5;
            } else {
               return JsApi.next(
                  var5,
                  "Skill.help('record-to-check')",
                  "author the proper check/feature from these steps (add waits, parameterize, Scenario-Outline loops, karate-config.js, karate-boot.js ties), then run it"
               );
            }
         }
      }
   }

   private static Map<String, Object> help() {
      return JsApi.markdown(
         "# Recording — capture a human UI flow, then author a check from it\n\nA human drives the real container Chrome (through the noVNC pane); every interaction is\ncaptured as a semantic, locator-anchored `human_action` event on the run's\n`transcript.jsonl` + the live SSE stream. The data is **not** replayable JS — it is rich\nenough that an LLM, given the recording, authors a proper `bot.*` check.\n\n**Capture:**\n- `Recording.start([name])` — start/attach recording. Creates a `mode=record` session on\n  the active live run (run `var bot = Robot.create()` first). Refused while an autonomous\n  `Harness.explore` loop is in flight. Returns `{runId, recording:true}`.\n- `Recording.note(text)` — emit an annotation; attaches to the last action when recent\n  (e.g. \"this dropdown is the offering tier — pick Gold\"), else stands alone.\n- `Recording.stop()` — stop capture + release the record slot. The live run stays alive.\n  Every recording also captures the page's web calls, masked, to `runs/<id>/traffic.jsonl`;\n  Stop answers its summary as `capture`. A typed password is stored and recorded as\n  `secret('API_PASSWORD')`. When the run settles, Coverage counts its kept calls\n  (`Traffic.sift`) as exercised by the recording (`Coverage.tests(id)` rows with `recording`),\n  never as verifying a criterion.\n- `Recording.status()` — `{recording, runId, actions}`.\n\n**Author from it:**\n- `Recording.get([runId])` — read a recording back as **authorable material**: the\n  replayable core (noise-filtered `human_action` steps — each `bot.act()`-ready, primary\n  locator + up to two fallback candidates — plus your annotations and `{captured, replayable}`\n  counts). Defaults to the run you just recorded; lean (no `look()`/DOM dump). Then pull\n  **`Skill.help('record-to-check')`** for the authoring playbook (waits, parameterization,\n  Scenario-Outline loops, `karate-config.js`, `karate-boot.js` ties). A ready-to-run JS is\n  **not** auto-generated; the LLM authors + verifies the proper artifact.\n\nRelated: `bot.reset()` recycles the browser in place (cookies/storage cleared +\nabout:blank, same target) and emits `browser_reset`. For an LLM-loop intervention,\n`Harness.takeControl()` pauses the loop so a human can drive, then `Harness.giveBack()`\nresumes it with a transcript-pull hint."
      );
   }

   static String authoringMaterial(String var0, Path var1) {
      io.karatelabs.ext.max.jsapi.Recording.RecordingDigest var2 = digest(var1);
      if (var2.a()) {
         return null;
      } else {
         StringBuilder var3 = new StringBuilder();
         var3.append("Recorded UI flow (run ")
            .append(var0)
            .append("): ")
            .append(var2.d())
            .append(" actions captured, ")
            .append(var2.b().size())
            .append(" replayable.\n");
         if (var2.e() != null) {
            var3.append("startUrl: ").append(var2.e()).append('\n');
         }

         var3.append("\nsteps (each is `<action> <locator>` — bot.act-ready; `alt:` are fallback candidates):\n");

         for (String var5 : var2.b()) {
            var3.append(var5).append('\n');
         }

         if (var2.b().stream().anyMatch(var0x -> var0x.contains("= secret('"))) {
            var3.append(
               "\na value written secret('NAME') is a stored secret: write it as the call, bot.act(locator, 'input', secret('NAME')), never as a literal.\n"
            );
         }

         if (!var2.c().isEmpty()) {
            var3.append("\nannotations (the human's intent for a step — use them to name/parameterize):\n");

            for (String var9 : var2.c()) {
               var3.append("- ").append(var9).append('\n');
            }
         }

         var var8 = revealPlansForRun(var1.getParent());
         if (!var8.isEmpty()) {
            var3.append(
               "\nreveal-plans (from advanced-record evidence — pass as bot.act(target,'click',{reveal:<plan>}); verified live, re-inferred on drift):\n"
            );

            for (Map var6 : (Iterable<Map>) (Iterable<?>) (var8)) {
               var3.append("- ").append(Json.of(var6).toString()).append('\n');
            }
         }

         return var3.toString();
      }
   }

   static List<Map<String, Object>> revealPlansForRun(Path var0) {
      Path var1 = var0.resolve("reveal-deltas.local.jsonl");
      if (!Files.isRegularFile(var1)) {
         return List.of();
      } else {
         List var2;
         try {
            var2 = Files.readAllLines(var1);
         } catch (Exception var10) {
            logger.warn("could not read reveal-delta sidecar {}: {}", var1, var10.getMessage());
            return List.of();
         }

         ArrayList var3 = new ArrayList();

         for (String var5 : (Iterable<String>) (Iterable<?>) (var2)) {
            if (!var5.isBlank()) {
               try {
                  if (Json.of(var5).value() instanceof Map var6 && var6.get("revealDelta") instanceof Map var11) {
                     var3.add(var11);
                  }
               } catch (Exception var9) {
               }
            }
         }

         return revealPlans(var3);
      }
   }

   static List<Map<String, Object>> revealPlans(List<Map<String, Object>> var0) {
      LinkedHashMap var1 = new LinkedHashMap();
      ArrayList var2 = null;
      String var3 = null;

      for (Map var5 : var0) {
         if (var5 != null) {
            String var6 = str(var5.get("owner"));
            String var7 = str(var5.get("target"));
            String var8 = str(var5.get("via"));
            if (var6 != null && var7 != null) {
               LinkedHashMap var9 = new LinkedHashMap();
               var9.put("owner", var6);
               var9.put("via", var8 != null ? var8 : "hover");
               if (var7.equals(var3)) {
                  var2.add(var9);
               } else {
                  var2 = new ArrayList();
                  var2.add(var9);
                  var3 = var7;
               }

               LinkedHashMap var10 = new LinkedHashMap();
               var10.put("reveal", new ArrayList(var2));
               var10.put("target", var7);
               var1.put(var7, var10);
            }
         }
      }

      return new ArrayList<>(var1.values());
   }

   static io.karatelabs.ext.max.jsapi.Recording.RecordingDigest digest(Path var0) {
      ArrayList var1 = new ArrayList();
      ArrayList var2 = new ArrayList();
      ArrayList var3 = new ArrayList();
      String var4 = null;
      int var5 = 0;
      String var6 = null;
      if (!Files.isRegularFile(var0)) {
         return new io.karatelabs.ext.max.jsapi.Recording.RecordingDigest(var1, var2, 0, null, var3);
      } else {
         List var7;
         try {
            var7 = Files.readAllLines(var0);
         } catch (Exception var15) {
            logger.warn("could not read transcript {}: {}", var0, var15.getMessage());
            return new io.karatelabs.ext.max.jsapi.Recording.RecordingDigest(var1, var2, 0, null, var3);
         }

         Iterator var8 = var7.iterator();

         while (true) {
            Map var10;
            while (true) {
               if (!var8.hasNext()) {
                  return new io.karatelabs.ext.max.jsapi.Recording.RecordingDigest(var1, var2, var5, var4, var3);
               }

               String var9 = (String)var8.next();
               if (!var9.isBlank()) {
                  try {
                     if (Json.of(var9).value() instanceof Map var11) {
                        var10 = var11;
                        break;
                     }
                  } catch (Exception var16) {
                  }
               }
            }

            String var17 = String.valueOf(var10.get("type"));
            if ("annotation".equals(var17)) {
               Object var19 = var10.get("text");
               if (var19 != null && !String.valueOf(var19).isBlank()) {
                  var2.add(String.valueOf(var19).trim());
                  var3.add(Map.of("note", String.valueOf(var19).trim()));
               }
            } else if ("human_action".equals(var17)) {
               var5++;
               String var18 = str(var10.get("action"));
               if (var18 != null
                  && REPLAYABLE.contains(var18)
                  && (
                     !"hover".equals(var18)
                        || !isTransientOverlay(str(var10.get("locator")), str(var10.get("name"))) && Boolean.TRUE.equals(var10.get("revealed"))
                  )) {
                  String var13 = str(var10.get("url"));
                  if (var13 != null && var4 == null) {
                     var4 = var13;
                  }

                  if ("nav".equals(var18)) {
                     if (var13 != null && !var13.equals(var6)) {
                        var6 = var13;
                        var1.add("nav    " + var13);
                        var3.add(Map.of("action", "nav", "url", var13));
                     }
                  } else {
                     var6 = null;
                     String var14 = str(var10.get("locator"));
                     if (var14 != null && !var14.toLowerCase().matches("(body|html)\\b.*")) {
                        var1.add(renderStep(var18, var14, var10));
                        var3.add(stepRow(var18, var14, var13, var10));
                     }
                  }
               }
            }
         }
      }
   }

   private static Map<String, Object> stepRow(String var0, String var1, String var2, Map<String, Object> var3) {
      LinkedHashMap var4 = new LinkedHashMap();
      var4.put("action", var0);
      var4.put("locator", var1);
      putIfSet(var4, "name", str(var3.get("name")));
      putIfSet(var4, "tag", str(var3.get("tag")));
      putIfSet(var4, "url", var2);
      if ("select".equals(var0) || "key".equals(var0)) {
         putIfSet(var4, "value", str(var3.get("value")));
         putIfSet(var4, "key", str(var3.get("key")));
      }

      return var4;
   }

   private static void putIfSet(Map<String, Object> var0, String var1, String var2) {
      if (var2 != null) {
         var0.put(var1, var2);
      }
   }

   private static String renderStep(String var0, String var1, Map<String, Object> var2) {
      StringBuilder var3 = new StringBuilder();
      var3.append(pad(var0)).append(var1);
      String var4 = str(var2.get("value"));
      if (var4 != null && "type".equals(var0) && SECRET_REF.matcher(var4).matches()) {
         var3.append("  = ").append(var4);
      } else if (var4 != null && ("type".equals(var0) || "select".equals(var0))) {
         var3.append("  = \"").append(var4.replace("\"", "\\\"")).append('"');
      }

      if ("key".equals(var0)) {
         String var5 = str(var2.get("key"));
         if (var5 != null) {
            var3.append("  (").append(var5).append(')');
         }
      }

      if ("hover".equals(var0)) {
         String var11 = str(var2.get("reveals"));
         if (var11 != null) {
            var3.append("  (reveals: ").append(var11).append(')');
         }
      }

      if (var2.get("candidates") instanceof List var12 && !var12.isEmpty()) {
         ArrayList var13 = new ArrayList();
         LinkedHashSet var7 = new LinkedHashSet();

         for (Object var9 : var12) {
            String var10 = str(var9);
            if (var10 != null && !var10.equals(var1) && var7.add(var10)) {
               var13.add(var10);
            }

            if (var13.size() == 2) {
               break;
            }
         }

         if (!var13.isEmpty()) {
            var3.append("   | alt: ").append(String.join(" , ", var13));
         }
      }

      return var3.toString();
   }

   private static String pad(String var0) {
      StringBuilder var1 = new StringBuilder(var0);

      while (var1.length() < 7) {
         var1.append(' ');
      }

      return var1.toString();
   }

   private static String str(Object var0) {
      if (var0 == null) {
         return null;
      } else {
         String var1 = String.valueOf(var0).trim();
         return var1.isEmpty() ? null : var1;
      }
   }

   private static boolean isTransientOverlay(String var0, String var1) {
      String var2 = ((var0 == null ? "" : var0) + " " + (var1 == null ? "" : var1)).toLowerCase();
      return OVERLAY.matcher(var2).find();
   }

   record RecordingDigest(List<String> b, List<String> c, int d, String e, List<Map<String, Object>> f) {


      boolean a() {
         return this.b.isEmpty();
      }

      public List<String> b() {
         return this.b;
      }

      public List<String> c() {
         return this.c;
      }

      public int d() {
         return this.d;
      }

      public String e() {
         return this.e;
      }

      public List<Map<String, Object>> f() {
         return this.f;
      }
   }
}
