package io.karatelabs.ext.max.jsapi;

import io.karatelabs.common.Json;
import io.karatelabs.common.StringUtils;
import io.karatelabs.ext.agent.llm.SecretRedactor;
import io.karatelabs.ext.common.MaxPaths;
import io.karatelabs.ext.common.Sha256;
import io.karatelabs.ext.common.jsapi.JsApi;
import io.karatelabs.ext.common.jsapi.JsDispatch;
import io.karatelabs.ext.max.apitest.CandidateRunner;
import io.karatelabs.ext.max.apitest.Cards;
import io.karatelabs.ext.max.apitest.EditResult;
import io.karatelabs.ext.max.apitest.FeatureEdits;
import io.karatelabs.ext.max.apitest.FeatureSource;
import io.karatelabs.ext.max.apitest.FeatureStore;
import io.karatelabs.ext.max.apitest.KarateConfigBlock;
import io.karatelabs.ext.max.apitest.LiveSessions;
import io.karatelabs.ext.max.apitest.OutlineRows;
import io.karatelabs.ext.max.apitest.RunExchanges;
import io.karatelabs.ext.max.apitest.TestId;
import io.karatelabs.ext.max.apitest.TriageGate;
import io.karatelabs.ext.max.apitest.WeakeningDiff;
import io.karatelabs.gherkin.FeatureSection;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ApiTest extends JsDispatch {
   private static final Logger logger = LoggerFactory.getLogger(ApiTest.class);
   private static final String CONFIG = "karate-config.js";
   private static final String LANDED = "land.feature.txt";
   private static final List<String> LOGIN_TAGS = List.of("@ignore");
   private final Path root;
   private final Map<String, io.karatelabs.ext.max.jsapi.ApiTest.FileEditOperation> edits = new LinkedHashMap<>();
   private boolean noProject;
   private UnaryOperator<String> sanitizer = new SecretRedactor(Map::of);
   private LiveSessions sessions;
   private TriageGate triage;
   private Runner runner;
   private Supplier<io.karatelabs.ext.common.secrets.SecretStore> secrets = () -> null;
   volatile Runnable afterLandRun;
   private static final Map<String, String> SECRET_NAMES = Map.of(
      "token", "API_TOKEN", "password", "API_PASSWORD", "key", "API_KEY", "clientSecret", "CLIENT_SECRET"
   );

   public ApiTest(Path var1) {
      this.root = var1.toAbsolutePath().normalize();
      this.fn("read", var1x -> this.read(JsApi.str(var1x, 0), JsApi.arg(var1x, 1)));
      this.fn("exchanges", var1x -> this.exchanges(JsApi.str(var1x, 0), JsApi.arg(var1x, 1)));
      this.edit("insertStep", (var0, var1x, var2) -> FeatureEdits.insertStep(var0, var1x.a(), var1x.a("at"), var1x.c("text"), var2));
      this.edit("replaceStep", (var0, var1x, var2) -> FeatureEdits.replaceStep(var0, var1x.d(), var1x.c("text"), var2));
      this.edit("deleteStep", (var0, var1x, var2) -> FeatureEdits.deleteStep(var0, var1x.d(), var2));
      this.edit("insertComment", (var0, var1x, var2) -> FeatureEdits.insertComment(var0, var1x.d(), var1x.c("text"), var2));
      this.edit("replaceComment", (var0, var1x, var2) -> FeatureEdits.replaceComment(var0, var1x.d(), var1x.a("comment"), var1x.c("text"), var2));
      this.edit("deleteComment", (var0, var1x, var2) -> FeatureEdits.deleteComment(var0, var1x.d(), var1x.a("comment"), var2));
      this.edit("insertRow", (var0, var1x, var2) -> FeatureEdits.insertExamplesRow(var0, var1x.b(), var1x.a("block"), var1x.a("at"), var1x.e("cells"), var2));
      this.edit("replaceRow", (var0, var1x, var2) -> FeatureEdits.replaceExamplesRow(var0, var1x.e(), var1x.e("cells"), var2));
      this.edit("deleteRow", (var0, var1x, var2) -> FeatureEdits.deleteExamplesRow(var0, var1x.e(), var2));
      this.edit("createScenario", (var0, var1x, var2) -> FeatureEdits.createScenario(var0, var1x.d("name"), var1x.f("tags"), var1x.e("steps"), var2));
      this.edit("link", (var0, var1x, var2) -> FeatureEdits.tag(var0, var1x.c(), var1x.e("tags"), List.of(), var2));
      this.edit("unlink", (var0, var1x, var2) -> FeatureEdits.tag(var0, var1x.c(), List.of(), var1x.e("tags"), var2));
      this.fn("createFeature", var1x -> this.createFeature(JsApi.str(var1x, 0), JsApi.arg(var1x, 1), false));
      this.fn("createLoginFeature", var1x -> this.createFeature(JsApi.str(var1x, 0), JsApi.arg(var1x, 1), true));
      this.fn("land", var1x -> this.land(JsApi.str(var1x, 0), JsApi.arg(var1x, 1), JsApi.arg(var1x, 2)));
      this.fn("diff", var1x -> this.diff(JsApi.arg(var1x, 0), JsApi.arg(var1x, 1)));
      this.fn("config", var1x -> this.config(JsApi.arg(var1x, 0)));
      this.fn("send", var1x -> this.live("ApiTest.send", JsApi.str(var1x, 0), JsApi.arg(var1x, 1), (var0, var1xx, var2) -> var1xx.send(var2, var0.a("step"))));
      this.fn(
         "restart",
         var1x -> this.live("ApiTest.restart", JsApi.str(var1x, 0), JsApi.arg(var1x, 1), (var0, var1xx, var2) -> var1xx.restart(var2, var0.b("step")))
      );
      this.fn(
         "session", var1x -> this.live("ApiTest.session", JsApi.str(var1x, 0), JsApi.arg(var1x, 1), (var0, var1xx, var2) -> var1xx.state(var2, var0.b("step")))
      );
      this.fn("close", var1x -> this.live("ApiTest.close", JsApi.str(var1x, 0), JsApi.arg(var1x, 1), (var0, var1xx, var2) -> var1xx.close()));
      this.fn("triage", var1x -> this.gated("ApiTest.triage", JsApi.str(var1x, 0), (var1xx, var2) -> this.triage.triage(var1xx)));
      this.fn("answer", var1x -> this.gated("ApiTest.answer", JsApi.str(var1x, 0), JsApi.arg(var1x, 1), (var1xx, var2) -> this.triage.answer(var1xx, var2.g())));
      this.fn(
         "recheck",
         var1x -> this.gated("ApiTest.recheck", JsApi.str(var1x, 0), JsApi.arg(var1x, 1), (var1xx, var2) -> this.triage.recheck(var1xx, var2.c("candidate")))
      );
      this.fn("change", var1x -> this.gated("ApiTest.change", JsApi.str(var1x, 0), (var1xx, var2) -> this.triage.change(var1xx)));
      this.fn(
         "undo", var1x -> this.gated("ApiTest.undo", JsApi.str(var1x, 0), JsApi.arg(var1x, 1), (var1xx, var2) -> this.triage.undo(var1xx, var2.c("after")))
      );
      this.fn("help", this::help);
   }

   private Object gated(String var1, String var2, io.karatelabs.ext.max.jsapi.ApiTest.TestOperation var3) {
      return this.gated(var1, var2, Map.of(), var3);
   }

   private Object gated(String var1, String var2, Object var3, io.karatelabs.ext.max.jsapi.ApiTest.TestOperation var4) {
      if (this.triage == null) {
         return JsApi.missing(var1 + ": the side chat runs on the console (serve); here there is none");
      } else {
         Object var5 = this.feature(var1, var2, false);
         if (var5 instanceof Map var8) {
            return var8;
         } else {
            try {
               io.karatelabs.ext.max.jsapi.ApiTest.OperationOptions var6 = io.karatelabs.ext.max.jsapi.ApiTest.OperationOptions.a(var1, var3 == null ? Map.of() : var3);
               return var4.apply(this.triage.id((Path)var5), var6);
            } catch (io.karatelabs.ext.max.jsapi.ApiTest.OperationOptions.InvalidOperationOptionsException var7) {
               return JsApi.missing(var7.getMessage());
            }
         }
      }
   }

   public static ApiTest at(Path var0) {
      return new ApiTest(var0);
   }

   public ApiTest withNoProject(boolean var1) {
      this.noProject = var1;
      return this;
   }

   public ApiTest withSanitizer(UnaryOperator<String> var1) {
      this.sanitizer = var1;
      return this;
   }

   public ApiTest withSessions(LiveSessions var1) {
      this.sessions = var1;
      return this;
   }

   public ApiTest withSecrets(Supplier<io.karatelabs.ext.common.secrets.SecretStore> var1) {
      this.secrets = var1;
      return this;
   }

   public ApiTest withTriage(TriageGate var1) {
      this.triage = var1;
      return this;
   }

   public ApiTest withRunner(Runner var1) {
      this.runner = var1;
      return this;
   }

   private Object live(String var1, String var2, Object var3, io.karatelabs.ext.max.jsapi.ApiTest.LiveSessionOperation var4) {
      UnaryOperator<String> var5 = this.sanitizer;
      Object var7 = this.feature(var1, var2, true);
      Map var6;
      if (var7 instanceof Map var8) {
         var6 = var8;
      } else if (this.sessions == null) {
         var6 = JsApi.missing(var1 + ": live sessions run on the console (serve); here there is none");
      } else {
         try {
            io.karatelabs.ext.max.jsapi.ApiTest.OperationOptions var9 = io.karatelabs.ext.max.jsapi.ApiTest.OperationOptions.a(var1, var3);
            String var10 = var9.d("session");
            if (var10 == null) {
               var10 = "default";
            }

            if (!var10.matches("[A-Za-z0-9._-]{1,64}")) {
               var6 = JsApi.missing(var1 + ": session is a short id ([A-Za-z0-9._-]), got " + var10);
            } else {
               Integer var11 = var9.b("row");
               LiveSessions.Use var12 = this.sessions.use(new LiveSessions.Key(var10, this.rel((Path)var7), var9.b(), var11 == null ? -1 : var11));
               var5 = var2x -> var12.apply(this.sanitizer.apply(var2x));
               var6 = var4.apply(var9, var12, (Path)var7);
            }
         } catch (io.karatelabs.ext.max.jsapi.ApiTest.OperationOptions.InvalidOperationOptionsException var13) {
            var6 = JsApi.missing(var13.getMessage());
         }
      }

      return Json.parseStrict(var5.apply(StringUtils.formatJson(var6, false, false, false)), true);
   }

   Object read(String var1, Object var2) {
      Object var3 = this.feature("ApiTest.read", var1, true);
      if (var3 instanceof Map var4) {
         return var4;
      } else {
         try {
            return this.read((Path)var3, FeatureStore.read((Path)var3), var2);
         } catch (IOException var5) {
            return JsApi.notFound("ApiTest.read('" + var1 + "') failed: " + var5.getMessage());
         }
      }
   }

   private Object read(Path var1, FeatureSource var2, Object var3) {
      boolean var4 = var3 instanceof Map var5 && Boolean.TRUE.equals(var5.get("full"));
      Integer var16 = null;
      int var6 = -1;
      if (var3 instanceof Map var7 && (var7.get("scenario") != null || var7.get("row") != null)) {
         try {
            io.karatelabs.ext.max.jsapi.ApiTest.OperationOptions var8 = io.karatelabs.ext.max.jsapi.ApiTest.OperationOptions.a("ApiTest.read", var3);
            var16 = var8.b();
            Integer var9 = var8.b("row");
            var6 = var9 == null ? 0 : var9;
         } catch (io.karatelabs.ext.max.jsapi.ApiTest.OperationOptions.InvalidOperationOptionsException var15) {
            return JsApi.missing(var15.getMessage());
         }
      }

      Cards.FeatureView var17 = Cards.read(var2, this.rel(var1), var16, var6);
      if (var16 != null) {
         Object var18 = rowRefusal(var2, var17, var16, var6);
         if (var18 != null) {
            return var18;
         }
      }

      Map var19 = var4 ? var17.toJson() : var17.toCompactJson();
      List var20 = var17.background() == null ? List.of() : Cards.channels(var17.background());
      List var10 = (List)var19.get("scenarios");

      for (int var11 = 0; var11 < var10.size(); var11++) {
         ArrayList var12 = new ArrayList();

         for (String var14 : (Iterable<String>) (Iterable<?>) (var20)) {
            if (!channelAvailable(var14)) {
               var12.add(var14);
            }
         }

         for (String var22 : Cards.channels(var17.flows().get(var11))) {
            if (!var12.contains(var22) && !channelAvailable(var22)) {
               var12.add(var22);
            }
         }

         if (!var12.isEmpty()) {
            ((Map)var10.get(var11)).put("unsupported", var12);
         }
      }

      return var19;
   }

   private static Object rowRefusal(FeatureSource var0, Cards.FeatureView var1, int var2, int var3) {
      if (var2 >= var1.flows().size()) {
         return JsApi.notFound("ApiTest.read: no scenario " + var2 + " — read without opts lists them");
      } else {
         FeatureSection var4 = var0.feature().getSections().get(var2);
         return var4.isOutline() && OutlineRows.addressable(var4.getScenarioOutline()) && var1.flows().get(var2).row() == null
            ? JsApi.notFound("ApiTest.read: scenario " + var2 + " has no Examples row " + var3)
            : null;
      }
   }

   static boolean channelAvailable(String var0) {
      try {
         Class.forName("io.karatelabs.ext." + var0 + "." + Character.toUpperCase(var0.charAt(0)) + var0.substring(1) + "ChannelFactory");
         return true;
      } catch (ClassNotFoundException var2) {
         return false;
      }
   }

   Object exchanges(String var1, Object var2) {
      return Json.parseStrict(this.sanitizer.apply(StringUtils.formatJson(this.exchangesOf(var1, var2), false, false, false)), true);
   }

   private Object exchangesOf(String var1, Object var2) {
      Map var3 = var2 instanceof Map var4 ? var4 : Map.of();
      Path var12 = this.root.resolve("runs");
      String var5 = var3.get("runId") == null ? null : String.valueOf(var3.get("runId"));
      if (var5 != null && !var5.matches("[A-Za-z0-9._-]{1,64}")) {
         return JsApi.missing("ApiTest.exchanges: runId is a run id from Runner.run, got " + var5);
      } else if (var5 != null && Files.isRegularFile(var12.resolve(var5).resolve("land.feature.txt"))) {
         Object var13 = this.feature("ApiTest.exchanges", var1, false);
         if (var13 instanceof Map var15) {
            return var15;
         } else {
            try {
               return evidence(
                  this.rel((Path)var13), var12.resolve(var5), FeatureSource.of(Files.readAllBytes(var12.resolve(var5).resolve("land.feature.txt"))), var3
               );
            } catch (IOException var10) {
               return JsApi.notFound("ApiTest.exchanges('" + var1 + "') failed: " + var10.getMessage());
            }
         }
      } else {
         Object var6 = this.feature("ApiTest.exchanges", var1, true);
         if (var6 instanceof Map var14) {
            return var14;
         } else {
            String var7 = this.rel((Path)var6);
            Path var8;
            if (var5 != null) {
               var8 = var12.resolve(var5);
            } else {
               var8 = RunExchanges.newest(var12, var7);
               if (var8 == null) {
                  return JsApi.next(JsApi.notFound("no run of " + var7 + " yet"), "Runner.run('" + var7 + "')");
               }
            }

            try {
               return evidence(var7, var8, FeatureStore.read((Path)var6), var3);
            } catch (IOException var11) {
               return JsApi.notFound("ApiTest.exchanges('" + var1 + "') failed: " + var11.getMessage());
            }
         }
      }
   }

   public Object exchangesRan(String var1, byte[] var2, String var3, Object var4) {
      Object var5 = this.feature("ApiTest.exchanges", var1, false);
      Object var6 = var5 instanceof Map var8
         ? var8
         : evidence(this.rel((Path)var5), this.root.resolve("runs").resolve(var3), FeatureSource.of(var2), var4 instanceof Map var7 ? var7 : Map.of());
      return Json.parseStrict(this.sanitizer.apply(StringUtils.formatJson(var6, false, false, false)), true);
   }

   private static Object evidence(String var0, Path var1, FeatureSource var2, Map<?, ?> var3) {
      Integer var4 = var3.get("scenario") instanceof Number var5 ? var5.intValue() : null;
      var var7 = RunExchanges.read(var1, var0, var2, Cards.read(var2, var0));
      if (var7.isEmpty()) {
         return JsApi.notFound("run " + var1.getFileName() + " has no result for " + var0);
      } else {
         LinkedHashMap var8 = new LinkedHashMap();
         var8.put("path", var0);
         var8.put("runId", var1.getFileName().toString());
         var8.put("scenarios", var4 == null ? var7 : var7.stream().filter(var1x -> var4.equals(var1x.get("index"))).toList());
         return var8;
      }
   }

   private void edit(String var1, io.karatelabs.ext.max.jsapi.ApiTest.FileEditOperation var2) {
      this.edits.put(var1, var2);
      this.fn(var1, var3 -> this.edit(var1, JsApi.str(var3, 0), JsApi.arg(var3, 1), var2));
   }

   public List<String> editVerbs() {
      var var1 = new ArrayList<>(this.edits.keySet());
      var1.addAll(List.of("createFeature", "createLoginFeature"));
      return var1;
   }

   public ApiTest.Candidate candidateEdit(String var1, String var2, byte[] var3, Object var4) {
      String var5 = "ApiTest." + var1;
      Object var6 = this.feature(var5, var2, false);
      if (var6 instanceof Map var13) {
         return new ApiTest.Candidate(cast(var13), null);
      } else {
         String var7 = this.rel((Path)var6);
         if (!"createFeature".equals(var1) && !"createLoginFeature".equals(var1)) {
            io.karatelabs.ext.max.jsapi.ApiTest.FileEditOperation var14 = this.edits.get(var1);
            if (var14 == null) {
               return new ApiTest.Candidate(JsApi.missing(var5 + " is not an edit verb"), null);
            } else if (var3 == null) {
               return new ApiTest.Candidate(JsApi.notFound("no feature '" + var2 + "' — create it with ApiTest.createFeature"), null);
            } else {
               try {
                  io.karatelabs.ext.max.jsapi.ApiTest.OperationOptions var9 = io.karatelabs.ext.max.jsapi.ApiTest.OperationOptions.a(var5, var4);
                  FeatureSource var10 = FeatureSource.of(var3);
                  EditResult var11 = var14.apply(var10, var9, var9.c("ifMatch"));
                  return new ApiTest.Candidate(landed(var7, var11, var10), var11.ok() ? var11.bytes() : null);
               } catch (io.karatelabs.ext.max.jsapi.ApiTest.OperationOptions.InvalidOperationOptionsException var12) {
                  return new ApiTest.Candidate(JsApi.missing(var12.getMessage()), null);
               }
            }
         } else if (var3 != null) {
            return new ApiTest.Candidate(JsApi.missing(var5 + ": " + var7 + " exists — edit it with the step verbs"), null);
         } else {
            var var8 = this.createdBytes(var5, var7, var4, "createLoginFeature".equals(var1));
            return new ApiTest.Candidate(var8, (byte[])var8.remove("__bytes"));
         }
      }
   }

   public Object readCandidate(String var1, byte[] var2, Object var3) {
      Object var4 = this.feature("ApiTest.read", var1, false);
      return var4 instanceof Map var5 ? var5 : this.read((Path)var4, FeatureSource.of(var2), var3);
   }

   private static Map<String, Object> cast(Map<?, ?> var0) {
      return (Map<String, Object>)(Map<?, ?>)var0;
   }

   private Object edit(String var1, String var2, Object var3, io.karatelabs.ext.max.jsapi.ApiTest.FileEditOperation var4) {
      String var5 = "ApiTest." + var1;
      if (this.noProject) {
         return JsApi.noProjectSelected(var5);
      } else {
         Object var6 = this.feature(var5, var2, true);
         if (var6 instanceof Map var14) {
            return var14;
         } else {
            io.karatelabs.ext.max.jsapi.ApiTest.OperationOptions var7;
            try {
               var7 = io.karatelabs.ext.max.jsapi.ApiTest.OperationOptions.a(var5, var3);
               var7.c("ifMatch");
            } catch (io.karatelabs.ext.max.jsapi.ApiTest.OperationOptions.InvalidOperationOptionsException var13) {
               return JsApi.missing(var13.getMessage());
            }

            FeatureSource[] var8 = new FeatureSource[1];
            boolean var9 = "link".equals(var1) || "unlink".equals(var1);

            Object var10;
            try {
               var10 = this.gated((Path)var6, () -> {
                  EditResult var6x = FeatureStore.apply((Path)var6, var3xx -> {
                     var8[0] = var3xx;
                     return var4.apply(var3xx, var7, var7.c("ifMatch"));
                  });
                  if (var9 && this.triage != null && var6x instanceof EditResult var7x && var7x.ok() && var8[0] != null) {
                     this.triage.followTags((Path)var6, io.karatelabs.ext.common.Sha256.a(var8[0].bytes()), io.karatelabs.ext.common.Sha256.a(var7x.bytes()));
                  }

                  return var6x;
               });
            } catch (io.karatelabs.ext.max.jsapi.ApiTest.OperationOptions.InvalidOperationOptionsException var12) {
               return JsApi.missing(var12.getMessage());
            }

            return var10 instanceof EditResult var11 ? landed(this.rel((Path)var6), var11, var8[0]) : var10;
         }
      }
   }

   private Object gated(Path var1, Supplier<?> var2) {
      return this.triage == null ? var2.get() : this.triage.write(var1, var2);
   }

   private static Map<String, Object> landed(String var0, EditResult var1, FeatureSource var2) {
      LinkedHashMap var3 = new LinkedHashMap();
      var3.put("path", var0);
      var3.putAll(var1.toJson());
      if (!var1.ok()) {
         return (Map<String, Object>)("STALE".equals(var1.error().get("code")) ? JsApi.next(var3, "ApiTest.read('" + var0 + "')") : var3);
      } else {
         if (var2 != null) {
            ArrayList var4 = new ArrayList();

            for (WeakeningDiff.Flag var6 : WeakeningDiff.diff(var2, FeatureSource.of(var1.bytes()))) {
               var4.add(var6.toJson());
            }

            if (!var4.isEmpty()) {
               var3.put("weakening", var4);
            }
         }

         return var3;
      }
   }

   Object createFeature(String var1, Object var2, boolean var3) {
      String var4 = var3 ? "ApiTest.createLoginFeature" : "ApiTest.createFeature";
      if (this.noProject) {
         return JsApi.noProjectSelected(var4);
      } else {
         Object var5 = this.feature(var4, var1, false);
         if (var5 instanceof Map var11) {
            return var11;
         } else {
            var var6 = this.createdBytes(var4, this.rel((Path)var5), var2, var3);
            byte[] var7 = (byte[])var6.remove("__bytes");
            if (var7 != null && this.gated((Path)var5, () -> FeatureStore.write((Path)var5, var7, null)) instanceof Map var9) {
               LinkedHashMap var10 = new LinkedHashMap();
               var10.put("path", var6.get("path"));
               var9.forEach((var1x, var2x) -> var10.put(String.valueOf(var1x), var2x));
               return var10;
            } else {
               return var6;
            }
         }
      }
   }

   private Map<String, Object> createdBytes(String var1, String var2, Object var3, boolean var4) {
      if (var2.indexOf(47) < 0) {
         String var10 = var4 ? "auth" : "checks";
         return JsApi.missing(
            var1
               + ": a new "
               + (var4 ? "login feature" : "test")
               + " is saved under "
               + var10
               + "/, not the project root — "
               + var1
               + "('"
               + var10
               + "/"
               + var2
               + "', …)"
         );
      } else {
         String var6 = null;

         EditResult var5;
         try {
            io.karatelabs.ext.max.jsapi.ApiTest.OperationOptions var7 = io.karatelabs.ext.max.jsapi.ApiTest.OperationOptions.a(var1, var3);
            if (var4) {
               var6 = var7.d("variable");
               var6 = var6 == null ? "login" : var6;
               if (!var6.matches("[A-Za-z_$][A-Za-z0-9_$]*")) {
                  return JsApi.missing(var1 + ": variable must be a JS identifier, got " + var6);
               }

               String var8 = var7.d("name");
               var5 = FeatureEdits.createFeature(var8 == null ? "login" : var8, LOGIN_TAGS, var8 == null ? "login" : var8, List.of(), var7.e("steps"));
            } else {
               var5 = FeatureEdits.createFeature(var7.d("name"), var7.f("tags"), var7.d("scenarioName"), var7.f("scenarioTags"), var7.e("steps"));
            }
         } catch (io.karatelabs.ext.max.jsapi.ApiTest.OperationOptions.InvalidOperationOptionsException var9) {
            return JsApi.missing(var9.getMessage());
         }

         var var12 = landed(var2, var5, null);
         if (var4 && var5.ok()) {
            var12.put("step", FeatureEdits.callonceStep(var6, "/" + var2));
         }

         if (var5.ok()) {
            var12.put("__bytes", var5.bytes());
         }

         return var12;
      }
   }

   Object land(String var1, Object var2, Object var3) {
      String var4 = "ApiTest.land";
      if (this.noProject) {
         return JsApi.noProjectSelected(var4);
      } else {
         Object var5 = this.feature(var4, var1, false);
         if (var5 instanceof Map var33) {
            return var33;
         } else if (!(var2 instanceof String var6 && !var6.isBlank())) {
            return JsApi.missing(var4 + "(path, source, {ifMatch?}): source is the feature's text");
         } else if (var3 != null && !(var3 instanceof Map)) {
            return JsApi.missing(var4 + "(path, source, {ifMatch?}): opts is an object");
         } else {
            Object var7 = var3 instanceof Map var8 ? var8.get("ifMatch") : null;
            FeatureSource var34 = FeatureSource.of(var6);
            if (var34.feature() != null && var34.feature().getLine() >= 1 && !var34.feature().getSections().isEmpty()) {
               io.karatelabs.ext.agent.llm.SecretRedactor.RedactionLocation var9 = (this.sanitizer instanceof SecretRedactor var10 ? var10 : new SecretRedactor(Map::of)).findFirstLiteralSecret(var6);
               if (var9 != null) {
                  LinkedHashMap var36 = new LinkedHashMap();
                  var36.put("path", this.rel((Path)var5));
                  var36.put(
                     "error",
                     var4
                        + ": line "
                        + var9.lineNumber()
                        + " holds a literal credential ("
                        + var9.lineText()
                        + ") — keep it in config with ApiTest.config({name, secret: 'ENV_VAR', value, ifMatch}) and read the variable"
                  );
                  var36.put("code", "LITERAL_SECRET");
                  var36.put("status", 400);
                  return this.masked(var36);
               } else if (this.runner == null) {
                  return JsApi.missing(var4 + ": a land runs on the console (serve); here there is none");
               } else {
                  Path var35 = (Path)var5;
                  String var37 = this.rel(var35);

                  byte[] var12;
                  try {
                     var12 = Files.isRegularFile(var35) ? Files.readAllBytes(var35) : null;
                  } catch (IOException var31) {
                     return JsApi.notFound(var4 + " could not read " + var37 + ": " + var31.getMessage());
                  }

                  String var13 = var12 == null ? null : io.karatelabs.ext.common.Sha256.a(var12);
                  if (var12 == null ? var7 == null : var13.equals(var7)) {
                     byte[] var38 = ApiToolkit.weakenedMarked(var12, ApiToolkit.marked(var34.bytes()));
                     TestId var15 = this.triage != null ? this.triage.id(var35) : new TestId(this.root, var37);
                     String var16 = UUID.randomUUID().toString().substring(0, 8);
                     if (this.triage != null) {
                        var var17 = this.triage.check(var35);
                        if (var17 == null) {
                           var17 = this.triage.beginCandidate(var15, var38, var13, var16);
                        }

                        if (var17 != null) {
                           LinkedHashMap var41 = new LinkedHashMap();
                           var41.put("path", var37);
                           var41.putAll(var17);
                           return var41;
                        }
                     }

                     CandidateRunner.Outcome var39 = CandidateRunner.Outcome.error("the run did not finish");

                     try {
                        var39 = OverlayCandidateRunner.outcome(this.runner.runCandidate(var37, var38, Map.of("runId", var16)), var15, this.root);
                     } catch (RuntimeException var30) {
                        var39 = CandidateRunner.Outcome.error(String.valueOf(var30.getMessage()));
                     } finally {
                        if (this.triage != null) {
                           this.triage.candidateDone(var15, var16, var39, false);
                        }
                     }

                     try {
                        Path var18 = Files.createDirectories(this.root.resolve("runs").resolve(var16));
                        Files.write(var18.resolve("land.feature.txt"), var38);
                     } catch (IOException var29) {
                        logger.warn("land {}: the bytes run {} ran were not kept: {}", new Object[]{var37, var16, var29.getMessage()});
                     }

                     LinkedHashMap var40 = new LinkedHashMap();
                     var40.put("path", var37);
                     var40.put("runId", var16);
                     String var19 = "ApiTest.exchanges('" + var37 + "', {runId: '" + var16 + "'})";
                     String var20 = "ApiTest.land('" + var37 + "', source" + (var13 == null ? "" : ", {ifMatch: '" + var13 + "'}") + ")";
                     if (!"green".equals(var39.status())) {
                        var40.put("status", "red");
                        var40.put("saved", false);
                        var40.put("failing", var39.failing() != null ? var39.failing() : Map.of("actual", String.valueOf(var39.error())));
                        return this.masked(JsApi.next(var40, var19 + " says why", "fix the source, then " + var20));
                     } else {
                        var40.put("status", "green");
                        Runnable var21 = this.afterLandRun;
                        if (var21 != null) {
                           var21.run();
                        }

                        if (this.feature(var4, var1, false) instanceof Map var43) {
                           var40.put("saved", false);
                           var43.forEach((var1x, var2x) -> var40.putIfAbsent(String.valueOf(var1x), var2x));
                           return this.masked(var40);
                        } else {
                           Map var23 = this.triage != null ? this.triage.land(var15, var38, var13, var16) : FeatureStore.write(var35, var38, var13);
                           var23 = var23 == null ? Map.of() : var23;
                           if (var23.get("error") != null && !Boolean.TRUE.equals(var23.get("landed"))) {
                              var40.put("saved", false);
                              var23.forEach((var1x, var2x) -> var40.putIfAbsent(var1x, var2x));
                              return this.masked(
                                 JsApi.next(var40, "ApiTest.read('" + var37 + "')", "then ApiTest.land('" + var37 + "', source, {ifMatch: version})")
                              );
                           } else {
                              var40.put("saved", true);
                              var40.put("version", io.karatelabs.ext.common.Sha256.a(var38));
                              var40.put("created", var13 == null);
                              if (var12 != null) {
                                 ArrayList var24 = new ArrayList();
                                 WeakeningDiff.diff(FeatureSource.of(var12), FeatureSource.of(var38)).forEach(var1x -> var24.add(var1x.toJson()));
                                 if (!var24.isEmpty()) {
                                    var40.put("weakening", var24);
                                 }
                              }

                              if (FeatureSource.of(var38).feature().getSections().stream().anyMatch(WeakeningDiff::marked)) {
                                 var40.put("weakened", "Coverage does not count it until a person removes @ai=weakened");
                              }

                              return this.masked(var40);
                           }
                        }
                     }
                  } else {
                     LinkedHashMap var14 = new LinkedHashMap();
                     var14.put("path", var37);
                     var14.put(
                        "error",
                        var4
                           + ": "
                           + (
                              var12 == null
                                 ? var37 + " does not exist — land it with no ifMatch"
                                 : "ifMatch is not " + var37 + "'s version — ApiTest.read it and pass its version"
                           )
                     );
                     var14.put("code", "STALE");
                     var14.put("status", 409);
                     return JsApi.next(var14, "ApiTest.read('" + var37 + "')");
                  }
               }
            } else {
               return JsApi.missing(var4 + ": source is a feature with a Scenario" + (var34.feature() == null ? " — " + var34.notEditable() : ""));
            }
         }
      }
   }

   private Object masked(Map<String, Object> var1) {
      return Json.parseStrict(this.sanitizer.apply(StringUtils.formatJson(var1, false, false, false)), true);
   }

   Object diff(Object var1, Object var2) {
      Object var3 = this.source("ApiTest.diff", var1);
      if (var3 instanceof Map var8) {
         return var8;
      } else {
         Object var4 = this.source("ApiTest.diff", var2);
         if (var4 instanceof Map var9) {
            return var9;
         } else {
            ArrayList var5 = new ArrayList();

            for (WeakeningDiff.Flag var7 : WeakeningDiff.diff((FeatureSource)var3, (FeatureSource)var4)) {
               var5.add(var7.toJson());
            }

            return var5;
         }
      }
   }

   private Object source(String var1, Object var2) {
      if (var2 instanceof Map var3 && var3.get("text") instanceof String var8) {
         return FeatureSource.of(var8.getBytes(StandardCharsets.UTF_8));
      } else if (var2 instanceof String var7) {
         Object var4 = this.feature(var1, var7, true);
         if (var4 instanceof Map var9) {
            return var9;
         } else {
            try {
               return FeatureStore.read((Path)var4);
            } catch (IOException var6) {
               return JsApi.notFound(var1 + " could not read '" + var7 + "': " + var6.getMessage());
            }
         }
      } else {
         return JsApi.missing(var1 + "(a, b): each side is a project path or {text}");
      }
   }

   Object config(Object var1) {
      Object var2 = this.configChange(var1, null);
      if (var2 instanceof io.karatelabs.ext.max.jsapi.ApiTest.PendingConfigUpdate var3) {
         var var4 = var3.a();
         if (this.gated(var3.b(), () -> FeatureStore.write(var3.b(), var3.d(), var3.e() == null ? null : String.valueOf(var3.e()))) instanceof Map var7) {
            var7.forEach((var1x, var2x) -> var4.put(String.valueOf(var1x), var2x));
            return "STALE".equals(var7.get("code")) ? JsApi.next(var4, "ApiTest.config()") : var4;
         } else {
            String var6 = this.store(var3.f());
            return var6 != null ? JsApi.missing("ApiTest.config wrote " + this.rel(var3.b()) + " but not its secrets: " + var6) : answered(var3);
         }
      } else {
         return var2;
      }
   }

   public ApiTest.ConfigCandidate configCandidate(Object var1, ApiTest.HeldConfig var2) {
      Object var3 = this.configChange(var1, var2);
      if (var3 instanceof io.karatelabs.ext.max.jsapi.ApiTest.PendingConfigUpdate var4) {
         if (var4.c() != null && !io.karatelabs.ext.common.Sha256.a(var4.c()).equals(String.valueOf(var4.e()))) {
            var var7 = var4.a();
            var7.put("error", "ApiTest.config: ifMatch is not the version ApiTest.config() answered");
            var7.put("code", "STALE");
            var7.put("status", 409);
            return new ApiTest.ConfigCandidate(JsApi.next(var7, "ApiTest.config()"), null);
         } else {
            String var5 = this.store(var4.f());
            if (var5 != null) {
               return new ApiTest.ConfigCandidate(JsApi.missing("ApiTest.config: the credential was not stored: " + var5), null);
            } else {
               var var6 = answered(var4);
               var6.put("unsaved", true);
               return new ApiTest.ConfigCandidate(var6, new ApiTest.HeldConfig(var4.b(), var2 != null ? var2.before() : var4.c(), var4.d()));
            }
         }
      } else {
         return new ApiTest.ConfigCandidate(var3, null);
      }
   }

   public Map<String, Object> landConfig(ApiTest.HeldConfig var1) {
      return refusal(this.gated(var1.file(), () -> FeatureStore.write(var1.file(), var1.bytes(), var1.before() == null ? null : io.karatelabs.ext.common.Sha256.a(var1.before()))));
   }

   public Map<String, Object> restoreConfig(ApiTest.HeldConfig var1) {
      String var2 = io.karatelabs.ext.common.Sha256.a(var1.bytes());
      return refusal(
         this.gated(var1.file(), () -> var1.before() == null ? FeatureStore.delete(var1.file(), var2) : FeatureStore.write(var1.file(), var1.before(), var2))
      );
   }

   private static Map<String, Object> refusal(Object var0) {
      return var0 instanceof Map var1 ? var1 : null;
   }

   private String store(Map<String, String> var1) {
      if (var1.isEmpty()) {
         return null;
      } else {
         try {
            var1.forEach(this.secrets.get().a(this.root)::a);
            return null;
         } catch (UncheckedIOException | IllegalStateException var3) {
            return var3.getMessage();
         }
      }
   }

   private static Map<String, Object> answered(io.karatelabs.ext.max.jsapi.ApiTest.PendingConfigUpdate var0) {
      var var1 = var0.a();
      var1.put("version", io.karatelabs.ext.common.Sha256.a(var0.d()));
      var1.put("basedOn", var0.c() == null ? null : io.karatelabs.ext.common.Sha256.a(var0.c()));
      var1.put("entries", var0.g());
      if (!var0.f().isEmpty()) {
         var1.put("stored", new ArrayList<>(var0.f().keySet()));
      }

      return var1;
   }

   private List<Map<String, Object>> resolving(List<Map<String, Object>> var1) {
      List var2;
      try {
         var2 = this.secrets.get() == null ? List.of() : this.secrets.get().d();
      } catch (RuntimeException var8) {
         var2 = List.of();
      }

      ArrayList var3 = new ArrayList();

      for (Map var5 : var1) {
         if (var5.get("secret") instanceof String var6) {
            LinkedHashMap var9 = new LinkedHashMap(var5);
            var9.put("resolves", var2.contains(var6) || System.getenv(var6) != null);
            var3.add(var9);
         } else {
            var3.add(var5);
         }
      }

      return var3;
   }

   private Object configChange(Object var1, ApiTest.HeldConfig var2) {
      Map var3 = var1 == null ? Map.of() : (var1 instanceof Map var4 ? var4 : null);
      if (var3 == null) {
         return JsApi.missing("ApiTest.config(opts?): opts is an object — ApiTest.help('config')");
      } else {
         String var18 = var3.get("file") == null ? "karate-config.js" : String.valueOf(var3.get("file"));

         Path var5;
         try {
            var5 = MaxPaths.resolveSandboxed(this.root, var18);
         } catch (SecurityException var16) {
            return JsApi.missing("ApiTest.config denied: " + var16.getMessage());
         }

         if (!"karate-config.js".equals(var5.getFileName().toString())) {
            return JsApi.missing("ApiTest.config edits a karate-config.js, not " + var18);
         } else {
            byte[] var6;
            try {
               var6 = var2 != null && var2.file().equals(var5) ? var2.bytes() : (Files.isRegularFile(var5) ? Files.readAllBytes(var5) : null);
            } catch (IOException var17) {
               return JsApi.notFound("ApiTest.config could not read " + var18 + ": " + var17.getMessage());
            }

            String var7 = var6 == null ? null : new String(var6, StandardCharsets.UTF_8);
            String var8 = var6 == null ? null : io.karatelabs.ext.common.Sha256.a(var6);
            LinkedHashMap var9 = new LinkedHashMap(var3);
            var9.remove("file");
            Object var10 = var9.remove("ifMatch");
            LinkedHashMap var11 = new LinkedHashMap();
            var11.put("path", this.rel(var5));
            if (!var9.containsKey("name") && !var9.containsKey("auth") && !var9.containsKey("remove")) {
               KarateConfigBlock.State var19 = KarateConfigBlock.read(var7);
               if (var19.error() != null) {
                  var11.putAll(var19.error());
                  return var11;
               } else {
                  var11.put("version", var8);
                  var11.put("set", KarateConfigBlock.handWritten(var7));
                  var11.put("entries", this.resolving(var19.entries()));
                  if (var2 != null) {
                     var11.put("unsaved", true);
                  }

                  return var11;
               }
            } else if (this.noProject) {
               return JsApi.noProjectSelected("ApiTest.config");
            } else if (var7 != null && var10 == null) {
               return JsApi.missing("ApiTest.config: pass ifMatch, the version ApiTest.config() answered");
            } else {
               LinkedHashMap var12 = new LinkedHashMap();
               if (var9.get("auth") instanceof Map var13) {
                  LinkedHashMap var22 = new LinkedHashMap(var13);
                  Map var15 = storedCredentials(var22, var9.get("env"), var12);
                  if (var15 != null) {
                     return var15;
                  }

                  var9.put("auth", var22);
               }

               if (var9.get("name") != null && var9.get("secret") != null && var9.containsKey("value")) {
                  String var20 = String.valueOf(var9.get("secret"));
                  if (!(var9.remove("value") instanceof String var23) || var23.isEmpty()) {
                     return JsApi.missing("ApiTest.config: value beside secret is the credential, a non-empty string");
                  }

                  if (!io.karatelabs.ext.common.secrets.SecretStore.d.matcher(var20).matches()) {
                     return JsApi.missing("ApiTest.config: secret names an environment variable, got " + var20);
                  }

                  var12.put(var20, var23);
               }

               if (!var12.isEmpty() && this.secrets.get() == null) {
                  return JsApi.missing(
                     "ApiTest.config: a credential's value goes to the console's secret store, and there is none here — pass {secret: '<ENV_VAR>'} and set the variable"
                  );
               } else {
                  KarateConfigBlock.Result var21 = KarateConfigBlock.apply(var7, var9);
                  if (var21.error() != null) {
                     var11.putAll(var21.error());
                     return var11;
                  } else {
                     return new io.karatelabs.ext.max.jsapi.ApiTest.PendingConfigUpdate(var11, var5, var6, var21.text().getBytes(StandardCharsets.UTF_8), var10, var12, var21.entries());
                  }
               }
            }
         }
      }
   }

   private static Map<String, Object> storedCredentials(Map<String, Object> var0, Object var1, Map<String, String> var2) {
      for (String var4 : KarateConfigBlock.CREDENTIALS) {
         if (var0.get(var4) instanceof Map var5 && var5.containsKey("value")) {
            if (!(var5.get("value") instanceof String var8) || var8.isEmpty()) {
               return JsApi.missing("ApiTest.config: auth." + var4 + ".value is the credential, a non-empty string");
            }

            String var9 = var5.get("secret") != null
               ? String.valueOf(var5.get("secret"))
               : (var1 == null ? "" : String.valueOf(var1).toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]", "_") + "_") + SECRET_NAMES.get(var4);
            if (!io.karatelabs.ext.common.secrets.SecretStore.d.matcher(var9).matches()) {
               return JsApi.missing("ApiTest.config: auth." + var4 + ".secret names an environment variable, got " + var9);
            }

            var2.put(var9, var8);
            var0.put(var4, Map.of("secret", var9));
         }
      }

      return null;
   }

   private Object feature(String var1, String var2, boolean var3) {
      if (var2 == null || var2.isBlank()) {
         return JsApi.missing(var1 + "(path, …) needs a project-relative .feature path");
      } else if (!var2.endsWith(".feature")) {
         return JsApi.missing(var1 + " works on a .feature file, not " + var2);
      } else {
         Path var4;
         try {
            var4 = MaxPaths.resolveSandboxed(this.root, var2);
         } catch (SecurityException var6) {
            return JsApi.missing(var1 + " denied: " + var6.getMessage());
         }

         return var3 && !Files.isRegularFile(var4) ? JsApi.notFound("no feature '" + var2 + "' — File.list() shows what is there") : var4;
      }
   }

   private String rel(Path var1) {
      return this.root.relativize(var1).toString().replace('\\', '/');
   }

   private Object help(Object[] var1) {
      String var2 = JsApi.str(var1, 0);
      if (var2 != null && "session".equals(var2.trim())) {
         var var11 = new LinkedHashMap<>(this.sessionHelp());
         var11.put("topic", "session");
         return var11;
      } else if (var2 != null && "triage".equals(var2.trim())) {
         var var10 = new LinkedHashMap<>(this.triageHelp());
         var10.put("topic", "triage");
         return var10;
      } else {
         Object var3 = var2 == null ? null : JsApi.helpTopic(this.sessionHelp(), var1);
         if (var3 instanceof Map var4 && var4.get("markdown") != null) {
            return var3;
         } else {
            Object var12 = var2 == null ? null : JsApi.helpTopic(this.triageHelp(), var1);
            if (var12 instanceof Map var5 && var5.get("markdown") != null) {
               return var12;
            } else {
               Object var13 = JsApi.helpTopic(this.help(), var1);
               if (var13 instanceof Map var6 && var6.get("topics") instanceof List var7) {
                  LinkedHashMap var14 = new LinkedHashMap();
                  var6.forEach((var1x, var2x) -> var14.put(String.valueOf(var1x), var2x));
                  ArrayList var9 = new ArrayList(var7);
                  var9.addAll(List.of("session", "restart", "close", "triage", "answer", "recheck", "change", "undo"));
                  var14.put("topics", var9);
                  return var14;
               } else {
                  return var13;
               }
            }
         }
      }
   }

   private Map<String, Object> triageHelp() {
      return JsApi.markdown(
         "# ApiTest triage — a red the chat found holds the test until the tester answers\n\nWhile a triage is open every write to the test is refused `TRIAGE_PENDING` (`BUSY` while its run is in flight): the edit verbs, `File.*`, land, Undo. Human verbs, not bound in the chat.\n\n- `ApiTest.triage(path)` → `{state: none|running|open|answered|verifying|lost, failing?, question?, candidate?, base?, doc?, answers, chat?, verify?}`; `answers` are the answers it takes now.\n- `ApiTest.answer(path, {answer: 'bug'|'test'|'discard', ticket?, candidate, base})` — `candidate`/`base` as `triage` answered them, else `STALE`. `bug` annotates and verifies, `test` reopens the candidate to the chat, `discard` drops it and lifts the gate.\n- `ApiTest.answer(path, {answer: 'discard', doc})` — the recovery discard a `lost` triage takes.\n- `ApiTest.recheck(path, {candidate})` — one more verification run of an answered bug.\n- `ApiTest.change(path)` → `{before, after, runId, weakening, undoable, reason?}` — the chat's last change.\n- `ApiTest.undo(path, {after})` — one level: restores the bytes before it, or deletes a file it created."
      );
   }

   private Map<String, Object> help() {
      return JsApi.markdown(
         "# ApiTest — a .feature as cards; edits change only the bytes they name\n\n- `ApiTest.read(path)` → `{version, background?, scenarios:[{index, name, req, checks, cards}]}`. A card's `kind` is call · check · remember · data table · advanced; it covers steps `step`..`end` of its scenario (a data table: Examples `block`, `rows:[{row, edit, cells}]`). `editable: false`: no card form; a `replaceStep` of it answers `meaningUnknown`. `{scenario, row}` binds an Outline flow to that `row` (default 0); `{full: true}` adds spans, comments and raw steps.\n- `ApiTest.exchanges(path, {runId?, scenario?})` → cards' `status` and `exchanges` (newest run or `runId`); a call's `verdict` counts its checks.\n\nEvery edit is `ApiTest.<verb>(path, {…, ifMatch})`: `ifMatch` is the `version` you read, the answer's `version` the next. An insert or delete shifts indexes: read again. A refusal `{error, code}` wrote nothing; `STALE`: read and redo it. `weakening:[…]`: a check dropped or loosened: say so. Address steps by `scenario: <index>`, or `background: true` (every scenario).\n\n- `ApiTest.insertStep(path, {scenario, at, text, ifMatch})` — `text` is one step, e.g. `And match response.status == 'paid'`; `at: card.end + 1` lands right after a card.\n- `ApiTest.replaceStep(path, {scenario, step, text, ifMatch})` — `# …` lines replace its comments.\n- `ApiTest.deleteStep(path, {scenario, step, ifMatch})` — and its comments.\n- `ApiTest.insertComment(path, {scenario, step, text, ifMatch})` — above the step.\n- `ApiTest.replaceComment(path, {scenario, step, comment, text, ifMatch})` — as a card's `comments` list them.\n- `ApiTest.deleteComment(path, {scenario, step, comment, ifMatch})`\n- `ApiTest.insertRow(path, {scenario, block, at, cells, ifMatch})` — `{block, row}` as a data-table row's `edit` (0 is the header).\n- `ApiTest.replaceRow(path, {scenario, block, row, cells, ifMatch})`\n- `ApiTest.deleteRow(path, {scenario, block, row, ifMatch})`\n- `ApiTest.createScenario(path, {name, tags, steps, ifMatch})`\n- `ApiTest.createFeature(path, {name, tags, scenarioName, scenarioTags, steps})` — a new file, no `ifMatch`.\n- `ApiTest.createLoginFeature(path, {steps, variable})` — an `@ignore` login feature (`variable` default `login`); answers `step`, the `callonce` line for each caller's Background.\n- `ApiTest.land(path, source, {ifMatch?})` — runs `source`; only green writes it (red: `ApiTest.exchanges(path, {runId})`).\n- `ApiTest.link(path, {scenario, tags, ifMatch})` — `tags: ['@req=ORDER-001/1']`; `feature: true` in place of `scenario` tags the feature.\n- `ApiTest.unlink(path, {scenario, tags, ifMatch})`\n- `ApiTest.diff(a, b)` → weakening flags, a to b; a side is a path or `{text}`.\n- `ApiTest.config()` → `{version, set, entries}`; a `secret` entry `resolves` if set. `ApiTest.config({name, value, env, ifMatch})` sets a variable; `{name, secret: 'ENV_VAR', value?}` commits `secret('ENV_VAR')`, `value` to the secret store; `{auth: {scheme: 'bearer', token: {secret: 'API_TOKEN'}}, env, ifMatch}` sets the auth preset (bearer: token or login · basic: username, password · apikey: header or query, key · oauth2: tokenUrl, clientId, clientSecret, scope). `{remove: name, env, ifMatch}` drops one. No `env`: every env. No `ifMatch` only for a new file. Never `File.write` karate-config.js.\n\n- `ApiTest.send(…)` · `ApiTest.restart(…)` · `ApiTest.session(…)` · `ApiTest.close(…)` — resend one call of a flow run: `ApiTest.help('session')`."
      );
   }

   private Map<String, Object> sessionHelp() {
      LiveSessions.Settings var1 = this.sessions == null ? LiveSessions.Settings.DEFAULTS : this.sessions.settings();
      return JsApi.markdown(
         "# ApiTest live session — send one call again without replaying the flow\n\n`Runner.run('<path>.feature:<line>', {session: 'id'})` runs one flow and keeps it warm: its result's `session` is `{state: 'warm', cursor}`, or `{state: 'none', code, reason}`. `session` defaults to `default`; `row` is an Outline's Examples row, as `ApiTest.exchanges` answers it. A refusal names the call that fixes it in `next`.\n\n- `ApiTest.send(path, {scenario, step, row?, session?})` — sends the call card at `step`, then the checks after it, with the session's variables → `{state, cursor, cards}` (`cards` as `exchanges` shapes them). Nothing before it is sent again.\n- `ApiTest.restart(path, {scenario, step?, row?, session?})` — a fresh session: sends the calls before `step` (all when omitted) again, listed as `replayed`.\n- `ApiTest.session(path, {scenario, row?, step?, session?})` → `{state: none|warm|stale|busy, reason?, cursor, replays}`; `replays` are the calls a restart at `step` sends.\n- `ApiTest.close(path, {scenario, row?, session?})`\n\nAn edit at or above the cursor, a Background, karate-config or env change, or an Undo makes the session stale. Idle expiry %d min; at most %d per server."
            .formatted(var1.idleMs() / 60000L, var1.max())
      );
   }

   public record Candidate(Map<String, Object> answer, byte[] bytes) {

   }

   public record ConfigCandidate(Object answer, ApiTest.HeldConfig held) {

   }

   public record HeldConfig(Path file, byte[] before, byte[] bytes) {

   }

   private record PendingConfigUpdate(Map<String, Object> a, Path b, byte[] c, byte[] d, Object e, Map<String, String> f, List<Map<String, Object>> g) {

   }

   private interface FileEditOperation {
      EditResult apply(FeatureSource var1, io.karatelabs.ext.max.jsapi.ApiTest.OperationOptions var2, String var3);
   }

   private interface TestOperation {
      Object apply(TestId var1, io.karatelabs.ext.max.jsapi.ApiTest.OperationOptions var2);
   }

   private interface LiveSessionOperation {
      Map<String, Object> apply(io.karatelabs.ext.max.jsapi.ApiTest.OperationOptions var1, LiveSessions.Use var2, Path var3);
   }

   private record OperationOptions(String f, Map<String, Object> g) {


      static io.karatelabs.ext.max.jsapi.ApiTest.OperationOptions a(String var0, Object var1) {
         if (var1 instanceof Map var2) {
            return new io.karatelabs.ext.max.jsapi.ApiTest.OperationOptions(var0, var2);
         } else {
            throw new io.karatelabs.ext.max.jsapi.ApiTest.OperationOptions.InvalidOperationOptionsException(var0 + "(path, opts) needs an opts object — ApiTest.help('" + var0.substring(8) + "')");
         }
      }

      Integer a() {
         boolean var1 = Boolean.TRUE.equals(this.g.get("background"));
         if (var1 == this.g.containsKey("scenario")) {
            throw new io.karatelabs.ext.max.jsapi.ApiTest.OperationOptions.InvalidOperationOptionsException(this.f + ": pass scenario (its index) or background: true");
         } else {
            return var1 ? null : this.a("scenario");
         }
      }

      int b() {
         return this.a("scenario");
      }

      Integer c() {
         boolean var1 = Boolean.TRUE.equals(this.g.get("feature"));
         if (var1 == this.g.containsKey("scenario")) {
            throw new io.karatelabs.ext.max.jsapi.ApiTest.OperationOptions.InvalidOperationOptionsException(this.f + ": pass scenario (its index) or feature: true");
         } else {
            return var1 ? null : this.a("scenario");
         }
      }

      FeatureEdits.StepRef d() {
         return new FeatureEdits.StepRef(this.a(), this.a("step"));
      }

      FeatureEdits.RowRef e() {
         return new FeatureEdits.RowRef(this.a("scenario"), this.a("block"), this.a("row"));
      }

      int a(String var1) {
         if (this.g.get(var1) instanceof Number var2 && var2.doubleValue() == Math.rint(var2.doubleValue()) && var2.intValue() >= 0) {
            return var2.intValue();
         } else {
            throw new io.karatelabs.ext.max.jsapi.ApiTest.OperationOptions.InvalidOperationOptionsException(this.f + ": " + var1 + " is a 0-based index, got " + this.g.get(var1));
         }
      }

      Integer b(String var1) {
         return this.g.get(var1) == null ? null : this.a(var1);
      }

      String c(String var1) {
         String var2 = this.d(var1);
         if (var2 == null) {
            throw new io.karatelabs.ext.max.jsapi.ApiTest.OperationOptions.InvalidOperationOptionsException(this.f + ": " + var1 + " is required");
         } else {
            return var2;
         }
      }

      String d(String var1) {
         Object var2 = this.g.get(var1);
         if (var2 == null) {
            return null;
         } else if (var2 instanceof String var3) {
            return var3;
         } else {
            throw new io.karatelabs.ext.max.jsapi.ApiTest.OperationOptions.InvalidOperationOptionsException(this.f + ": " + var1 + " is a string, got " + var2);
         }
      }

      List<String> e(String var1) {
         var var2 = this.f(var1);
         if (var2 != null && !var2.isEmpty()) {
            return var2;
         } else {
            throw new io.karatelabs.ext.max.jsapi.ApiTest.OperationOptions.InvalidOperationOptionsException(this.f + ": " + var1 + " is required");
         }
      }

      List<String> f(String var1) {
         Object var2 = this.g.get(var1);
         if (var2 == null) {
            return null;
         } else if (var2 instanceof String var7) {
            return List.of(var7);
         } else if (var2 instanceof List var3) {
            ArrayList var4 = new ArrayList();

            for (Object var6 : var3) {
               var4.add(var6 == null ? "" : String.valueOf(var6));
            }

            return var4;
         } else {
            throw new io.karatelabs.ext.max.jsapi.ApiTest.OperationOptions.InvalidOperationOptionsException(this.f + ": " + var1 + " is a string or a list of strings, got " + var2);
         }
      }

      public String f() {
         return this.f;
      }

      public Map<String, Object> g() {
         return this.g;
      }

      static final class InvalidOperationOptionsException extends RuntimeException {
         InvalidOperationOptionsException(String var1) {
            super(var1);
         }
      }
   }
}
