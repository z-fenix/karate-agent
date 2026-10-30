package io.karatelabs.ext.max.apitest;

import io.karatelabs.core.HttpRunEvent;
import io.karatelabs.core.MockServer;
import io.karatelabs.core.RunEvent;
import io.karatelabs.core.RunEventType;
import io.karatelabs.core.RunListener;
import io.karatelabs.core.Runner;
import io.karatelabs.core.ScenarioRunEvent;
import io.karatelabs.core.ScenarioRuntime;
import io.karatelabs.core.StepResult;
import io.karatelabs.core.StepRunEvent;
import io.karatelabs.core.SuiteRunEvent;
import io.karatelabs.ext.agent.llm.SecretRedactor;
import io.karatelabs.ext.common.Sha256;
import io.karatelabs.ext.common.jsapi.JsApi;
import io.karatelabs.ext.coverage.HttpExchangeEvidenceRecorder;
import io.karatelabs.ext.max.jsapi.ConsoleState;
import io.karatelabs.gherkin.FeatureSection;
import io.karatelabs.gherkin.Scenario;
import io.karatelabs.gherkin.Step;
import io.karatelabs.http.DefaultHttpClientFactory;
import io.karatelabs.http.HttpClient;
import io.karatelabs.http.HttpClientFactory;
import io.karatelabs.output.LogContext;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class LiveSessions {
   private static final Logger logger = LoggerFactory.getLogger(LiveSessions.class);
   public static final String DEFAULT_SESSION = "default";
   private final ConsoleState console;
   private final Path root;
   private final Supplier<List<Path>> configDirs;
   private final Supplier<Map<String, String>> known;
   private final LiveSessions.Settings settings;
   private final LiveSessions.Registry registry;
   private final io.karatelabs.ext.max.apitest.LiveSessions.LiveSessionHttpClientFactory factory = new io.karatelabs.ext.max.apitest.LiveSessions.LiveSessionHttpClientFactory();
   private final Map<LiveSessions.Key, io.karatelabs.ext.max.apitest.LiveSessions.LiveSessionState> sessions = new ConcurrentHashMap<>();
   private final Map<ScenarioRuntime, io.karatelabs.ext.max.apitest.LiveSessions.LiveSessionState> byRuntime = new ConcurrentHashMap<>();
   private final Map<LiveSessions.Key, Object> restarting = new ConcurrentHashMap<>();
   private final Map<String, Map<String, Object>> lastOutcome = new ConcurrentHashMap<>();
   private final AtomicBoolean sweeping = new AtomicBoolean();
   private volatile boolean closed;
   volatile Runnable beforeDecide;
   volatile Runnable beforeAdmit;
   volatile Runnable beforeUse;
   private static final Pattern CALLS_FEATURE = Pattern.compile("\\bcall(once)?\\b|karate\\.call(Single)?\\(");

   public LiveSessions(ConsoleState var1, Path var2, Supplier<List<Path>> var3, Supplier<Map<String, String>> var4, LiveSessions.Settings var5) {
      this(var1, var2, var3, var4, var5, LiveSessions.Registry.GLOBAL);
   }

   public LiveSessions(
      ConsoleState var1, Path var2, Supplier<List<Path>> var3, Supplier<Map<String, String>> var4, LiveSessions.Settings var5, LiveSessions.Registry var6
   ) {
      this.console = var1;
      this.root = var2.toAbsolutePath().normalize();
      this.configDirs = var3;
      this.known = var4 == null ? Map::of : var4;
      this.settings = var5;
      this.registry = var6;
      var6.instances.add(this);
   }

   public LiveSessions.Use use(LiveSessions.Key var1) {
      return new LiveSessions.Use(var1);
   }

   public LiveSessions.Settings settings() {
      return this.settings;
   }

   public int size() {
      this.sweep();
      return this.sessions.size();
   }

   public HttpClientFactory factory() {
      return this.factory;
   }

   public LiveSessions.Capture capture(String var1, String var2, FeatureSource var3, ConsoleState.Kickoff var4) {
      return new LiveSessions.Capture(var1, var2, var3, var4, null, null, null);
   }

   public RunListener listener(LiveSessions.Capture var1) {
      return var2 -> {
         try {
            return this.onEvent(var1, var2);
         } catch (RuntimeException var4) {
            logger.warn("live session listener failed: {}", var4.getMessage());
            return true;
         }
      };
   }

   private boolean onEvent(LiveSessions.Capture var1, RunEvent var2) {
      if (var2 instanceof HttpRunEvent var3 && var3.type() == RunEventType.HTTP_EXIT) {
         var var13 = io.karatelabs.ext.coverage.HttpExchangeEvidenceRecorder.a(var3);
         io.karatelabs.ext.max.apitest.LiveSessions.LiveSessionState var5 = this.byRuntime.get(var3.scenarioRuntime());
         List<java.util.Map<java.lang.String,java.lang.Object>> var6 = var5 == null ? null : var5.n;
         if (var6 != null) {
            var6.add(var13);
         } else if (var1 != null && topLevel(var3.scenarioRuntime()) && var1.retained == null) {
            var1.exchanges.computeIfAbsent(var1.currentStep, var0 -> new ArrayList<>()).add(var13);
         }

         return true;
      } else if (var1 == null) {
         return true;
      } else if (var2 instanceof ScenarioRunEvent var9 && var9.type() == RunEventType.SCENARIO_ENTER) {
         return var1.row == null || !topLevel(var9.source()) || OutlineRows.rowOf(var9.source().getScenario()) == var1.row;
      } else if (var2 instanceof StepRunEvent var10 && var10.type() == RunEventType.STEP_ENTER && topLevel(var10.scenarioRuntime())) {
         Step var4 = var10.step();
         var1.currentStep = var4.isBackground() ? -1 : var4.getIndex();
         return var1.cursor == null || var4.isBackground() || var4.getIndex() < var1.cursor;
      } else {
         if (var2 instanceof ScenarioRunEvent var11 && var11.type() == RunEventType.SCENARIO_EXIT && topLevel(var11.source())) {
            this.retain(var1, var11.source());
         }

         if (var2 instanceof SuiteRunEvent var12 && var12.type() == RunEventType.SUITE_EXIT) {
            synchronized (var1) {
               if (var1.pending != null) {
                  var1.pending.h.addAll(var12.source().detachServers());
               }
            }
         }

         return true;
      }
   }

   private static boolean topLevel(ScenarioRuntime var0) {
      return var0 != null && var0.getFeatureRuntime() != null && var0.getFeatureRuntime().isTopLevel();
   }

   private void retain(LiveSessions.Capture var1, ScenarioRuntime var2) {
      if (var1.row == null || OutlineRows.rowOf(var2.getScenario()) == var1.row) {
         Runnable var3 = this.beforeDecide;
         if (var3 != null) {
            var3.run();
         }

         io.karatelabs.ext.max.apitest.LiveSessions.LiveSessionState var4 = null;
         synchronized (var1) {
            if (var1.pending != null || var1.refused != null) {
               var4 = var1.pending;
               var1.pending = null;
               var1.refused = "MULTI_SCENARIO";
               var1.reason = "the run executed more than one scenario; run one flow: Runner.run('<path>:<line>', {session})";
            } else if (var2.getDriverIfPresent() == null && !var2.hasChannels()) {
               Scenario var6 = var2.getScenario();
               LiveSessions.Key var7 = new LiveSessions.Key(
                  var1.session, var1.feature, var6.getFeature().getSections().indexOf(var6.getSection()), OutlineRows.rowOf(var6)
               );
               int var8 = var1.cursor != null ? Math.min(var1.cursor, var6.getSteps().size()) : var6.getSteps().size();
               var1.pending = new io.karatelabs.ext.max.apitest.LiveSessions.LiveSessionState(var7, var2, var1.at, var1.configStamp, var1.scratch, var1.source, var8);
               if (var1.pending.c != null) {
                  this.factory.a.add(var1.pending.c);
               }
            } else {
               var1.refused = "NOT_HTTP";
               var1.reason = "the flow drives a browser or a channel, which the run closed; a live session is HTTP only";
            }
         }

         if (var4 != null) {
            this.factory.a(var4.c);
         }
      }
   }

   public void publish(LiveSessions.Capture var1) {
      io.karatelabs.ext.max.apitest.LiveSessions.LiveSessionState var2;
      synchronized (var1) {
         var2 = var1.pending;
         var1.pending = null;
      }

      if (var2 == null) {
         this.remember(var1);
      } else {
         while (true) {
            io.karatelabs.ext.max.apitest.LiveSessions.LiveSessionState var3 = this.sessions.get(var2.a);
            if (var3 != null && !this.lock(var3)) {
               this.refuse(var1, var2, "BUSY", "the session " + describe(var2.a) + " is in use — retry");
               return;
            }

            label144: {
               try {
                  Runnable var4 = this.beforeAdmit;
                  if (var4 != null) {
                     var4.run();
                  }

                  synchronized (this.registry) {
                     if (this.sessions.get(var2.a) != var3) {
                        continue;
                     }

                     this.sweep();
                     String var6 = this.closed ? "the console is stopping" : this.admit();
                     if (var6 == null) {
                        if (var3 != null) {
                           this.close(var3);
                        }

                        this.sessions.put(var2.a, var2);
                        this.byRuntime.put(var2.b, var2);
                        break label144;
                     }

                     this.refuse(var1, var2, this.closed ? "SHUTDOWN" : "CAPACITY", var6);
                  }
               } finally {
                  if (var3 != null) {
                     this.release(var3);
                  }
               }

               return;
            }

            var1.retained = var2;
            this.lastOutcome.remove(var1.session + "\n" + var1.feature);
            this.startSweeper();
            return;
         }
      }
   }

   private void refuse(LiveSessions.Capture var1, io.karatelabs.ext.max.apitest.LiveSessions.LiveSessionState var2, String var3, String var4) {
      var1.refused = var3;
      var1.reason = var4;
      this.factory.a(var2.c);
      stop(var2.h);
      deleteQuietly(var2.g);
      this.remember(var1);
   }

   private void remember(LiveSessions.Capture var1) {
      this.lastOutcome.put(var1.session + "\n" + var1.feature, var1.outcome());
   }

   public Map<String, Object> state(LiveSessions.Key var1, Path var2, Integer var3) {
      return this.use(var1).state(var2, var3);
   }

   private Map<String, Object> state(LiveSessions.Use var1, Path var2, Integer var3) {
      LiveSessions.Key var4 = var1.key;
      this.sweep();
      io.karatelabs.ext.max.apitest.LiveSessions.LiveSessionState var5 = this.sessions.get(var4);
      var1.ran(var5);
      FeatureSource var6 = read(var2);
      LinkedHashMap<java.lang.String, java.lang.Object> var7 = new LinkedHashMap();
      var7.put("session", var4.session());
      var7.put("scenario", var4.scenario());
      if (var4.row() >= 0) {
         var7.put("row", var4.row());
      }

      if (var5 == null) {
         var7.put("state", "none");
         var var8 = this.lastOutcome.get(var4.session() + "\n" + var4.feature());
         if (this.closed) {
            var7.put("code", "SHUTDOWN");
            var7.put("reason", "the console is stopping");
         } else if (var8 != null && var8.get("code") != null) {
            var7.put("code", var8.get("code"));
            var7.put("reason", var8.get("reason"));
         }
      } else {
         String var9 = var5.i.isLocked() ? null : this.staleness(var5, var6);
         var7.put("state", var5.i.isLocked() ? "busy" : (var9 == null ? "warm" : "stale"));
         if (var9 != null) {
            var7.put("reason", var9);
         }

         var7.put("cursor", var5.k);
         var7.put("version", var9 == null ? var6.version() : var5.j.version());
         var7.put("env", var5.d.env());
         var7.put("idleMs", System.currentTimeMillis() - var5.l);
      }

      if (var6 != null && var6.feature() != null) {
         Cards.FeatureView var10 = Cards.read(var6, var4.feature(), var4.scenario(), var4.row());
         if (var4.scenario() < var10.flows().size()) {
            var7.put("replays", replays(var10, var10.flows().get(var4.scenario()), var3));
         }
      }

      return var7;
   }

   public Map<String, Object> send(LiveSessions.Key var1, Path var2, int var3) {
      return this.use(var1).send(var2, var3);
   }

   private Map<String, Object> send(LiveSessions.Use var1, Path var2, int var3) {
      LiveSessions.Key var4 = var1.key;
      this.sweep();
      Runnable var5 = this.beforeUse;
      if (var5 != null) {
         var5.run();
      }

      io.karatelabs.ext.max.apitest.LiveSessions.LiveSessionState var6 = this.sessions.get(var4);
      if (var6 == null) {
         return this.refused(
            var4, var2, var3, "NO_SESSION", "no live session for " + describe(var4) + " — Run it (Runner.run with {session}) or ApiTest.restart"
         );
      } else if (!this.lock(var6)) {
         return this.busy(var4);
      } else {
         Map var13;
         try {
            if (this.sessions.get(var4) != var6) {
               return this.refused(var4, var2, var3, "NO_SESSION", "the session " + describe(var4) + " was closed — ApiTest.restart");
            }

            var1.ran(var6);
            FeatureSource var7 = read(var2);
            String var8 = this.staleness(var6, var7);
            if (var8 != null) {
               return this.refused(var4, var2, var3, "STALE_SESSION", "the session is stale: " + var8 + " — ApiTest.restart");
            }

            Cards.FeatureView var9 = Cards.read(var7, var4.feature(), var4.scenario(), var4.row());
            if (var4.scenario() >= var9.flows().size()) {
               return JsApi.notFound("no scenario " + var4.scenario() + " in " + var4.feature());
            }

            Cards.Flow var10 = var9.flows().get(var4.scenario());
            var var11 = sent(var10, var3);
            if (var11 == null) {
               return JsApi.notFound("no call at step " + var3 + " of scenario " + var4.scenario() + "; calls start at steps " + callSteps(var10));
            }

            Scenario var12 = bound(var7, var4);
            if (var12 != null) {
               int var32 = ((Cards.Card)var11.get(0)).source().firstStep();
               int var14 = ((Cards.Card)var11.get(var11.size() - 1)).source().lastStep();
               var var15 = new ArrayList<>(Collections.nCopies(var12.getSteps().size(), (io.karatelabs.ext.max.apitest.LiveSessions.StepOutcome)null));
               ArrayList var16 = new ArrayList();
               var6.n = var16;
               LogContext.set(new LogContext());
               var6.b.getConfig().applyLoggingToContext(LogContext.get());
               var6.b.getHttp().reset();

               try {
                  boolean var17 = false;

                  for (int var18 = var32; var18 <= var14; var18++) {
                     if (var17) {
                        var15.set(var18, new io.karatelabs.ext.max.apitest.LiveSessions.StepOutcome("skipped", null, List.of()));
                     } else {
                        var16.clear();
                        StepResult var19 = var6.b.getExecutor().execute(fake(var12, var12.getSteps().get(var18)));
                        var15.set(var18, outcome(var19, List.copyOf(var16)));
                        var17 = var19.isFailed();
                     }
                  }
               } finally {
                  var6.b.getHttp().reset();
                  var6.n = null;
                  LogContext.clear();
               }

               var6.k = var14 + 1;
               var6.j = var7;
               var6.l = System.currentTimeMillis();
               LinkedHashMap<java.lang.String, java.lang.Object> var33 = new LinkedHashMap();
               var33.put("session", var4.session());
               var33.put("state", "warm");
               var33.put("cursor", var6.k);
               var33.put("version", var7.version());
               var33.put("env", var6.d.env());
               var33.put("cards", RunExchanges.cards(var11, var15));
               return var33;
            }

            var13 = noRow(var7, var4);
         } finally {
            this.release(var6);
         }

         return var13;
      }
   }

   public Map<String, Object> restart(LiveSessions.Key var1, Path var2, Integer var3) {
      return this.use(var1).restart(var2, var3);
   }

   private Map<String, Object> restart(LiveSessions.Use var1, Path var2, Integer var3) {
      LiveSessions.Key var4 = var1.key;
      this.sweep();
      FeatureSource var5 = read(var2);
      if (var5 != null && var5.feature() != null) {
         Cards.FeatureView var6 = Cards.read(var5, var4.feature(), var4.scenario(), var4.row());
         if (var4.scenario() >= var6.flows().size()) {
            return JsApi.notFound("no scenario " + var4.scenario() + " in " + var4.feature());
         } else {
            Cards.Flow var7 = var6.flows().get(var4.scenario());
            Integer var8 = null;
            if (var3 != null) {
               Cards.Card var9 = callAt(var7, var3);
               if (var9 == null) {
                  return JsApi.notFound("no call at step " + var3 + " of scenario " + var4.scenario() + "; calls start at steps " + callSteps(var7));
               }

               var8 = var9.source().firstStep();
            }

            if (var7.outline() == var4.row() < 0) {
               return JsApi.missing(
                  var7.outline()
                     ? "scenario " + var4.scenario() + " is an Outline: pass row, its Examples row"
                     : "scenario " + var4.scenario() + " has no rows: drop row"
               );
            } else if (var7.outline() && bound(var5, var4) == null) {
               return noRow(var5, var4);
            } else if (this.closed) {
               return envelope("SHUTDOWN", 503, "the console is stopping");
            } else if (this.restarting.putIfAbsent(var4, Boolean.TRUE) != null) {
               return this.busy(var4);
            } else {
               try {
                  io.karatelabs.ext.max.apitest.LiveSessions.LiveSessionState var35 = this.sessions.get(var4);
                  if (var35 != null) {
                     if (!this.lock(var35)) return this.busy(var4);
                     try {
                        this.close(var35);
                     } finally {
                        this.release(var35);
                     }
                  }
                  {
                     synchronized (this.registry) {
                        this.sweep();
                        String var11 = this.admit();
                        if (var11 != null) {
                           return JsApi.next(envelope("CAPACITY", 503, var11), "ApiTest.close(path, {scenario, session})");
                        }
                     }

                     Path var36;
                     try {
                        var36 = Files.createTempDirectory("karate-live");
                     } catch (IOException var31) {
                        return envelope("INTERNAL", 500, "no scratch dir for the replay: " + var31.getMessage());
                     }

                     LiveSessions.Capture var38 = new LiveSessions.Capture(
                        var4.session(),
                        var4.feature(),
                        var5,
                        this.console.at(null),
                        var7.outline() ? var4.row() : null,
                        var8 == null ? Integer.MAX_VALUE : var8,
                        var36
                     );

                     try {
                        Runner.Builder var39 = Runner.path(var2.toString() + ":" + var7.line())
                           .configDir(this.root.toString())
                           .workingDir(this.root)
                           .outputDir(var36)
                           .backupOutputDir(false)
                           .outputHtmlReport(false)
                           .outputJsonLines(false)
                           .outputConsoleSummary(false)
                           .httpClientFactory(this.factory)
                           .listener(this.listener(var38));
                        var38.at.apply(var39).parallel(1);
                     } catch (RuntimeException var32) {
                        deleteQuietly(var36);
                        return envelope("INTERNAL", 500, "the replay did not run: " + var32.getMessage());
                     }

                     this.publish(var38);
                     io.karatelabs.ext.max.apitest.LiveSessions.LiveSessionState var40 = var38.retained;
                     var1.ran(var40);
                     if (var40 == null) {
                        deleteQuietly(var36);
                        String var43 = var38.refused == null ? "NO_SESSION" : var38.refused;
                        return envelope(
                           var43,
                           !"CAPACITY".equals(var43) && !"SHUTDOWN".equals(var43) ? 409 : 503,
                           var38.reason != null ? var38.reason : "the replay left no session: the scenario did not run (its row or line did not match)"
                        );
                     } else {
                        ScenarioRuntime var42 = var40.b;
                        var40.k = var8 == null ? var42.getScenario().getSteps().size() : var8;
                        LinkedHashMap<java.lang.String, java.lang.Object> var14 = new LinkedHashMap();
                        var14.put("session", var4.session());
                        var14.put("state", "warm");
                        var14.put("cursor", var40.k);
                        var14.put("version", var5.version());
                        var14.put("env", var40.d.env());
                        var14.put("replayed", replays(var6, var7, var8));
                        var var15 = replayOutcomes(var42, var38);
                        ArrayList<io.karatelabs.ext.max.apitest.Cards.Card> var16 = new ArrayList();

                        for (Cards.Card var18 : var7.cards()) {
                           if (var18.kind() != Cards.Kind.DATA_TABLE && var18.source().lastStep() < var40.k) {
                              var16.add(var18);
                           }
                        }

                        var14.put("cards", RunExchanges.cards(var16, var15));
                        String var45 = var42.getResult().isFailed() ? "failed" : "passed";
                        var14.put("status", var45);
                        if (var42.getResult().isFailed() && var42.getResult().getError() != null) {
                           var14.put("error", String.valueOf(var42.getResult().getError().getMessage()));
                        }

                        return var14;
                     }
                  }
               } finally {
                  this.restarting.remove(var4);
               }
            }
         }
      } else {
         return JsApi.notFound(var4.feature() + (var5 == null ? " cannot be read" : " does not parse: " + var5.notEditable()));
      }
   }

   public Map<String, Object> close(LiveSessions.Key var1) {
      return this.use(var1).close();
   }

   private Map<String, Object> close(LiveSessions.Use var1) {
      LiveSessions.Key var2 = var1.key;
      io.karatelabs.ext.max.apitest.LiveSessions.LiveSessionState var3 = this.sessions.get(var2);
      LinkedHashMap<java.lang.String, java.lang.Object> var4 = new LinkedHashMap();
      var4.put("session", var2.session());
      if (var3 == null) {
         var4.put("closed", false);
         return var4;
      } else if (!this.lock(var3)) {
         return this.busy(var2);
      } else {
         try {
            this.close(var3);
         } finally {
            this.release(var3);
         }

         var4.put("closed", true);
         return var4;
      }
   }

   public void invalidate(String var1, String var2) {
      for (io.karatelabs.ext.max.apitest.LiveSessions.LiveSessionState var4 : this.sessions.values()) {
         if (var1 == null || var1.equals(var4.a.feature())) {
            var4.m = var2;
         }
      }
   }

   public void shutdown() {
      this.closed = true;

      for (io.karatelabs.ext.max.apitest.LiveSessions.LiveSessionState var2 : List.copyOf(this.sessions.values())) {
         if (this.lock(var2)) {
            try {
               this.close(var2);
            } finally {
               this.release(var2);
            }
         }
      }

      this.unregisterWhenDone();
   }

   public void closeAll() {
      this.shutdown();
   }

   private String staleness(io.karatelabs.ext.max.apitest.LiveSessions.LiveSessionState var1, FeatureSource var2) {
      if (var1.m != null) {
         return var1.m;
      } else if (this.console.switches() != var1.d.switches()) {
         return "the env was switched";
      } else if (!this.configStamp().equals(var1.f)) {
         return "karate-config changed";
      } else if (var2 == null) {
         return "the feature cannot be read";
      } else if (var2.feature() == null) {
         return "the feature does not parse: " + var2.notEditable();
      } else if (var2.version().equals(var1.j.version())) {
         return null;
      } else {
         io.karatelabs.ext.max.apitest.FeatureSnapshot var3 = io.karatelabs.ext.max.apitest.FeatureSnapshot.a(var1.j);
         io.karatelabs.ext.max.apitest.FeatureSnapshot var4 = io.karatelabs.ext.max.apitest.FeatureSnapshot.a(var2);
         if (!Objects.equals(steps(var3.d()), steps(var4.d()))) {
            return "the Background changed";
         } else if (var1.a.scenario() >= var4.e().size()) {
            return "the scenario is gone";
         } else {
            io.karatelabs.ext.max.apitest.FeatureSnapshot.ScenarioSnapshot var5 = var3.e().get(var1.a.scenario());
            io.karatelabs.ext.max.apitest.FeatureSnapshot.ScenarioSnapshot var6 = var4.e().get(var1.a.scenario());
            if (var5.b() == var6.b() && Objects.equals(var5.g(), var6.g())) {
               var var7 = steps(var5);
               var var8 = steps(var6);
               int var9 = Math.min(var1.k, var7.size());
               if (var8.size() < var9) {
                  return "a step at or above the cursor was deleted";
               } else {
                  for (int var10 = 0; var10 < var9; var10++) {
                     if (!((io.karatelabs.ext.max.apitest.FeatureSnapshot.StepSnapshot)var7.get(var10)).equals(var8.get(var10))) {
                        return "step " + var10 + " changed at or above the cursor";
                     }
                  }

                  return null;
               }
            } else {
               return "the Examples changed";
            }
         }
      }
   }

   private static List<io.karatelabs.ext.max.apitest.FeatureSnapshot.StepSnapshot> steps(io.karatelabs.ext.max.apitest.FeatureSnapshot.ScenarioSnapshot var0) {
      if (var0 == null) {
         return List.of();
      } else {
         ArrayList<io.karatelabs.ext.max.apitest.FeatureSnapshot.StepSnapshot> var1 = new ArrayList();

         for (io.karatelabs.ext.max.apitest.FeatureSnapshot.StepSnapshot var3 : var0.f()) {
            var1.add(var3.a());
         }

         return var1;
      }
   }

   private String configStamp() {
      StringBuilder var1 = new StringBuilder();

      for (Path var4 : this.configDirs == null ? List.of(this.root) : this.configDirs.get()) {
         var var5 = new ArrayList<>(List.of(var4.resolve("karate-base.js"), var4.resolve("karate-config.js")));

         try (var var6 = Files.list(var4)) {
            var6.filter(var0 -> var0.getFileName().toString().matches("karate-config-.*\\.js")).sorted().forEach(var5::add);
         } catch (IOException var12) {
         }

         for (Path var7 : (Iterable<Path>) (Iterable<?>) (var5)) {
            if (Files.isRegularFile(var7)) {
               try {
                  var1.append(var7).append('=').append(io.karatelabs.ext.common.Sha256.a(Files.readAllBytes(var7))).append('\n');
               } catch (IOException var9) {
                  var1.append(var7).append("=unreadable\n");
               }
            }
         }
      }

      return io.karatelabs.ext.common.Sha256.a(var1.toString());
   }

   private static Scenario bound(FeatureSource var0, LiveSessions.Key var1) {
      FeatureSection var2 = var0.feature().getSections().get(var1.scenario());
      if (!var2.isOutline()) {
         return var1.row() < 0 ? var2.getScenario() : null;
      } else {
         OutlineRows.Bound var3 = OutlineRows.bind(var2.getScenarioOutline(), var1.row());
         return var3 == null ? null : var3.scenario();
      }
   }

   private static Map<String, Object> noRow(FeatureSource var0, LiveSessions.Key var1) {
      FeatureSection var2 = var0.feature().getSections().get(var1.scenario());
      return var2.isOutline() && !OutlineRows.addressable(var2.getScenarioOutline())
         ? envelope(
            "DYNAMIC_EXAMPLES",
            409,
            "the Examples of scenario "
               + var1.scenario()
               + " of "
               + var1.feature()
               + " are computed at run time, so no row can be sent or restarted live — Run the Outline for every row's exchanges, or write the row as a literal Examples table"
         )
         : JsApi.notFound("no row " + var1.row() + " in scenario " + var1.scenario() + " of " + var1.feature());
   }

   private static Step fake(Scenario var0, Step var1) {
      Step var2 = new Step(var0, -1);
      var2.setLine(var1.getLine());
      var2.setEndLine(var1.getEndLine());
      var2.setPrefix(var1.getPrefix());
      var2.setKeyword(var1.getKeyword());
      var2.setText(var1.getText());
      var2.setDocString(var1.getDocString());
      var2.setDocStringLine(var1.getDocStringLine());
      var2.setTable(var1.getTable());
      var2.setComments(var1.getComments());
      return var2;
   }

   private static io.karatelabs.ext.max.apitest.LiveSessions.StepOutcome outcome(StepResult var0, List<Map<String, Object>> var1) {
      String var2 = var0.getStatus() == null ? "failed" : var0.getStatus().name().toLowerCase();
      return new io.karatelabs.ext.max.apitest.LiveSessions.StepOutcome(var2, var0.isFailed() ? var0.getErrorMessage() : null, var1);
   }

   private static List<io.karatelabs.ext.max.apitest.LiveSessions.StepOutcome> replayOutcomes(ScenarioRuntime var0, LiveSessions.Capture var1) {
      var var2 = new ArrayList<>(Collections.nCopies(var0.getScenario().getSteps().size(), (io.karatelabs.ext.max.apitest.LiveSessions.StepOutcome)null));

      for (StepResult var4 : var0.getResult().getStepResults()) {
         Step var5 = var4.getStep();
         if (var5 != null && !var5.isBackground() && var5.getIndex() >= 0 && var5.getIndex() < var2.size()) {
            var2.set(var5.getIndex(), outcome(var4, var1.exchanges.getOrDefault(var5.getIndex(), List.of())));
         }
      }

      return var2;
   }

   private static Cards.Card callAt(Cards.Flow var0, int var1) {
      for (Cards.Card var3 : var0.cards()) {
         if (var3.kind() == Cards.Kind.CALL && var3.source().firstStep() <= var1 && var1 <= var3.source().lastStep()) {
            return var3;
         }
      }

      return null;
   }

   private static List<Cards.Card> sent(Cards.Flow var0, int var1) {
      Cards.Card var2 = callAt(var0, var1);
      if (var2 == null) {
         return null;
      } else {
         ArrayList<io.karatelabs.ext.max.apitest.Cards.Card> var3 = new ArrayList();
         var3.add(var2);
         boolean var4 = false;

         for (Cards.Card var6 : var0.cards()) {
            if (var6 == var2) {
               var4 = true;
            } else if (var4) {
               if (var6.kind() != Cards.Kind.CHECK && var6.kind() != Cards.Kind.REMEMBER) {
                  break;
               }

               var3.add(var6);
            }
         }

         return var3;
      }
   }

   private static List<Integer> callSteps(Cards.Flow var0) {
      ArrayList<java.lang.Integer> var1 = new ArrayList();

      for (Cards.Card var3 : var0.cards()) {
         if (var3.kind() == Cards.Kind.CALL) {
            var1.add(var3.source().firstStep());
         }
      }

      return var1;
   }

   private static List<Map<String, Object>> replays(Cards.FeatureView var0, Cards.Flow var1, Integer var2) {
      ArrayList<java.util.Map<java.lang.String,java.lang.Object>> var3 = new ArrayList();
      if (var0.background() != null) {
         for (Cards.Card var5 : var0.background().cards()) {
            boolean var6 = var5.kind() == Cards.Kind.CALL && var5.detail() instanceof Cards.Call var7 && var7.method() != null;
            if (var6 || var5.kind() == Cards.Kind.ADVANCED && var5.steps().stream().anyMatch(var0x -> CALLS_FEATURE.matcher(var0x).find())) {
               var3.add(replay(var5, true));
            }
         }
      }

      Cards.Card var9 = var2 == null ? null : callAt(var1, var2);

      for (Cards.Card var11 : var1.cards()) {
         if (var11 == var9) {
            break;
         }

         if (var11.kind() == Cards.Kind.CALL) {
            var3.add(replay(var11, false));
         }
      }

      return var3;
   }

   private static Map<String, Object> replay(Cards.Card var0, boolean var1) {
      LinkedHashMap<java.lang.String, java.lang.Object> var2 = new LinkedHashMap();
      var2.put("step", var0.source().firstStep());
      var2.put("title", var0.title());
      if (var0.detail() instanceof Cards.Call var3 && var3.method() != null) {
         var2.put("method", var3.method().toUpperCase());
      }

      if (var1) {
         var2.put("background", true);
      }

      return var2;
   }

   private boolean lock(io.karatelabs.ext.max.apitest.LiveSessions.LiveSessionState var1) {
      try {
         return var1.i.tryLock(this.settings.busyWaitMs(), TimeUnit.MILLISECONDS);
      } catch (InterruptedException var3) {
         Thread.currentThread().interrupt();
         return false;
      }
   }

   private String admit() {
      while (this.registry.total() >= this.registry.max) {
         io.karatelabs.ext.max.apitest.LiveSessions.LiveSessionState var1 = null;
         LiveSessions var2 = null;

         for (LiveSessions var4 : this.registry.instances) {
            for (io.karatelabs.ext.max.apitest.LiveSessions.LiveSessionState var6 : var4.sessions.values()) {
               if (!var6.i.isLocked() && (var1 == null || var6.l < var1.l)) {
                  var1 = var6;
                  var2 = var4;
               }
            }
         }

         if (var1 == null) {
            return "every one of the " + this.registry.max + " live sessions this server allows is in use";
         }

         if (!var2.closeIfIdle(var1, 0L)) {
            return "every one of the " + this.registry.max + " live sessions this server allows is in use";
         }
      }

      return null;
   }

   private void sweep() {
      for (io.karatelabs.ext.max.apitest.LiveSessions.LiveSessionState var2 : this.sessions.values()) {
         this.closeIfIdle(var2, this.settings.idleMs());
      }
   }

   private boolean closeIfIdle(io.karatelabs.ext.max.apitest.LiveSessions.LiveSessionState var1, long var2) {
      if (System.currentTimeMillis() - var1.l >= var2 && var1.i.tryLock()) {
         boolean var4;
         try {
            if (System.currentTimeMillis() - var1.l < var2) {
               return false;
            }

            this.close(var1);
            var4 = true;
         } finally {
            this.release(var1);
         }

         return var4;
      } else {
         return false;
      }
   }

   private void release(io.karatelabs.ext.max.apitest.LiveSessions.LiveSessionState var1) {
      try {
         if (this.closed) {
            this.close(var1);
         }
      } finally {
         var1.i.unlock();
      }
   }

   private void close(io.karatelabs.ext.max.apitest.LiveSessions.LiveSessionState var1) {
      if (this.sessions.remove(var1.a, var1)) {
         this.byRuntime.remove(var1.b, var1);
         this.factory.a(var1.c);
         stop(var1.h);
         deleteQuietly(var1.g);
         this.unregisterWhenDone();
      }
   }

   private void unregisterWhenDone() {
      if (this.closed && this.sessions.isEmpty()) {
         this.registry.instances.remove(this);
      }
   }

   private static void stop(List<MockServer> var0) {
      for (MockServer var2 : var0) {
         try {
            var2.stopAndWait();
         } catch (RuntimeException var4) {
            logger.warn("the session's mock on port {} did not stop: {}", var2.getPort(), var4.getMessage());
         }
      }

      var0.clear();
   }

   private void startSweeper() {
      if (this.sweeping.compareAndSet(false, true)) {
         Thread.ofVirtual().name("live-sessions").start(() -> {
            try {
               while (!this.sessions.isEmpty()) {
                  Thread.sleep(Math.max(250L, Math.min(this.settings.idleMs() / 2L, 30000L)));
                  this.sweep();
               }
            } catch (InterruptedException var5) {
            } finally {
               this.sweeping.set(false);
               if (!this.sessions.isEmpty()) {
                  this.startSweeper();
               }
            }
         });
      }
   }

   private static void deleteQuietly(Path var0) {
      if (var0 != null && Files.isDirectory(var0)) {
         try (var var1 = Files.walk(var0)) {
            var1.sorted(Comparator.reverseOrder()).forEach(var0x -> {
               try {
                  Files.deleteIfExists(var0x);
               } catch (IOException var2) {
               }
            });
         } catch (IOException var6) {
         }
      }
   }

   private static FeatureSource read(Path var0) {
      try {
         return FeatureStore.read(var0);
      } catch (IOException var2) {
         return null;
      }
   }

   private static String describe(LiveSessions.Key var0) {
      return var0.feature() + " scenario " + var0.scenario() + (var0.row() >= 0 ? " row " + var0.row() : "") + " (session '" + var0.session() + "')";
   }

   private Map<String, Object> refused(LiveSessions.Key var1, Path var2, int var3, String var4, String var5) {
      var var6 = envelope(var4, 409, var5);
      FeatureSource var7 = read(var2);
      if (var7 != null && var7.feature() != null) {
         Cards.FeatureView var8 = Cards.read(var7, var1.feature(), var1.scenario(), var1.row());
         if (var1.scenario() < var8.flows().size()) {
            var6.put("replays", replays(var8, var8.flows().get(var1.scenario()), var3));
         }
      }

      return JsApi.next(var6, "ApiTest.restart(path, {scenario, step, session})");
   }

   private Map<String, Object> busy(LiveSessions.Key var1) {
      return envelope("BUSY", 409, "the session " + describe(var1) + " is in use — retry");
   }

   private static Map<String, Object> envelope(String var0, int var1, String var2) {
      LinkedHashMap<java.lang.String, java.lang.Object> var3 = new LinkedHashMap();
      var3.put("error", var2);
      var3.put("code", var0);
      var3.put("status", var1);
      return var3;
   }

   public final class Capture {
      private final String session;
      private final String feature;
      private final FeatureSource source;
      private final ConsoleState.Kickoff at;
      private final String configStamp;
      private final Integer row;
      private final Integer cursor;
      private final Path scratch;
      private final Map<Integer, List<Map<String, Object>>> exchanges = new ConcurrentHashMap<>();
      private volatile int currentStep = -1;
      private io.karatelabs.ext.max.apitest.LiveSessions.LiveSessionState pending;
      private volatile io.karatelabs.ext.max.apitest.LiveSessions.LiveSessionState retained;
      private volatile String refused;
      private volatile String reason;

      private Capture(String var2, String var3, FeatureSource var4, ConsoleState.Kickoff var5, Integer var6, Integer var7, Path var8) {
         this.session = var2;
         this.feature = var3;
         this.source = var4;
         this.at = var5;
         this.configStamp = LiveSessions.this.configStamp();
         this.row = var6;
         this.cursor = var7;
         this.scratch = var8;
      }

      public Map<String, Object> outcome() {
         LinkedHashMap<java.lang.String, java.lang.Object> var1 = new LinkedHashMap();
         var1.put("session", this.session);
         io.karatelabs.ext.max.apitest.LiveSessions.LiveSessionState var2 = this.retained;
         if (var2 != null) {
            var1.put("state", "warm");
            var1.put("scenario", var2.a.scenario());
            if (var2.a.row() >= 0) {
               var1.put("row", var2.a.row());
            }

            var1.put("cursor", var2.k);
            return var1;
         } else {
            var1.put("state", "none");
            if (this.refused != null) {
               var1.put("code", this.refused);
               var1.put("reason", this.reason);
            }

            return var1;
         }
      }
   }

   public record Key(String session, String feature, int scenario, int row) {

   }

   public static final class Registry {
      public static final LiveSessions.Registry GLOBAL = new LiveSessions.Registry(LiveSessions.Settings.fromEnv().max());
      final int max;
      final Set<LiveSessions> instances = ConcurrentHashMap.newKeySet();

      public Registry(int var1) {
         this.max = var1;
      }

      public int total() {
         int var1 = 0;

         for (LiveSessions var3 : this.instances) {
            var1 += var3.sessions.size();
         }

         return var1;
      }
   }

   public record Settings(long idleMs, int max, long busyWaitMs) {
      public static final LiveSessions.Settings DEFAULTS = new LiveSessions.Settings(1800000L, 8, 5000L);


      public static LiveSessions.Settings fromEnv() {
         long var0 = DEFAULTS.idleMs();
         int var2 = DEFAULTS.max();

         try {
            String var3 = System.getenv("KARATE_SESSION_IDLE_MINUTES");
            if (var3 != null && !var3.isBlank()) {
               var0 = Math.max(1000L, (long)(Double.parseDouble(var3.trim()) * 60000.0));
            }

            var3 = System.getenv("KARATE_SESSION_MAX");
            if (var3 != null && !var3.isBlank()) {
               var2 = Math.max(1, Integer.parseInt(var3.trim()));
            }
         } catch (NumberFormatException var4) {
         }

         return new LiveSessions.Settings(var0, var2, DEFAULTS.busyWaitMs());
      }
   }

   public final class Use implements UnaryOperator<String> {
      private final LiveSessions.Key key;
      private volatile Map<String, String> secrets = Map.of();

      private Use(LiveSessions.Key var2) {
         this.key = var2;
      }

      public Map<String, Object> state(Path var1, Integer var2) {
         return LiveSessions.this.state(this, var1, var2);
      }

      public Map<String, Object> send(Path var1, int var2) {
         return LiveSessions.this.send(this, var1, var2);
      }

      public Map<String, Object> restart(Path var1, Integer var2) {
         return LiveSessions.this.restart(this, var1, var2);
      }

      public Map<String, Object> close() {
         return LiveSessions.this.close(this);
      }

      private void ran(io.karatelabs.ext.max.apitest.LiveSessions.LiveSessionState var1) {
         if (var1 != null) {
            this.secrets = var1.e;
         }
      }

      public String apply(String var1) {
         return new SecretRedactor(() -> {
            var var1x = new HashMap<>(LiveSessions.this.known.get());
            this.secrets.forEach((var1xx, var2) -> {
               if (var2 != null && !var2.equals(var1x.get(var1xx))) {
                  var1x.put(var1xx + "#kickoff", var2);
               }
            });
            return var1x;
         }).redactKnownAndGeneric(var1);
      }
   }

   record StepOutcome(String a, String b, List<Map<String, Object>> c) {

   }

   private static final class LiveSessionHttpClientFactory extends DefaultHttpClientFactory {
      final Set<HttpClient> a = ConcurrentHashMap.newKeySet();

      @Override
      public void release(HttpClient var1) {
         if (var1 != null && !this.a.contains(var1)) {
            super.release(var1);
         }
      }

      void a(HttpClient var1) {
         if (var1 != null) {
            this.a.remove(var1);
            super.release(var1);
         }
      }
   }

   private final class LiveSessionState {
      final LiveSessions.Key a;
      final ScenarioRuntime b;
      final HttpClient c;
      final ConsoleState.Snapshot d;
      final Map<String, String> e;
      final String f;
      final Path g;
      final List<MockServer> h = new ArrayList<>();
      final ReentrantLock i = new ReentrantLock(true);
      volatile FeatureSource j;
      volatile int k;
      volatile long l = System.currentTimeMillis();
      volatile String m;
      volatile List<Map<String, Object>> n;

      LiveSessionState(LiveSessions.Key var2, ScenarioRuntime var3, ConsoleState.Kickoff var4, String var5, Path var6, FeatureSource var7, int var8) {
         this.a = var2;
         this.b = var3;
         this.c = var3.getKarate().client;
         this.d = var4.snapshot();
         this.e = var4.overlay() == null ? Map.of() : var4.overlay();
         this.f = var5;
         this.g = var6;
         this.j = var7;
         this.k = var8;
      }
   }
}
