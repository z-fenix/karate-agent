package io.karatelabs.ext.max.jsapi;

import io.karatelabs.common.Json;
import io.karatelabs.common.StringUtils;
import io.karatelabs.ext.common.MaxPaths;
import io.karatelabs.ext.common.jsapi.JsApi;
import io.karatelabs.ext.common.jsapi.JsDispatch;
import io.karatelabs.ext.coverage.CoverageItem;
import io.karatelabs.ext.coverage.HttpAction;
import io.karatelabs.ext.coverage.collect.OpenApiCoverageSource;
import io.karatelabs.ext.max.apitest.KarateConfigBlock;
import io.karatelabs.ext.robot.recording.RecordingEvidenceReporter;
import java.io.IOException;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public final class Traffic extends JsDispatch {
   private static final String FILE = "traffic.jsonl";
   private static final int SAMPLE = 160;
   private static final int RECORDINGS = 5;
   private static final int SHAPE_DEPTH = 3;
   private static final String CONFIG = "karate-config.js";
   private static final Pattern URL_OR_METHOD = Pattern.compile("\\s*(?:Given|When|Then|And|But|\\*)\\s+(url|method)\\b(.*)");
   private static final Pattern APP_BASED = Pattern.compile("appUrl\\s*(?:\\+\\s*(['\"])[/?#].*)?");
   private final Path root;
   private final Function<String, Path> runDir;
   private Supplier<Collection<String>> console = List::of;
   private Supplier<String> appUrl = () -> null;
   private Supplier<List<OpenApiCoverageSource>> specs;
   private static final String SIFT = "Traffic.sift(source, {app?, hosts?, moves?})";
   private static final String MIGRATE = "Traffic.migrate(source, {to?, app?, hosts?, moves?})";

   public Traffic(Path var1, Function<String, Path> var2) {
      this.root = var1.toAbsolutePath().normalize();
      this.runDir = var2;
      Report var3 = Report.atReportDir(this.root.resolve("target")).withRoot(this.root);
      this.specs = var3::openapiSources;
      this.fn("read", var1x -> this.read(JsApi.arg(var1x, 0), JsApi.arg(var1x, 1)));
      this.fn("sift", var1x -> this.sift(JsApi.arg(var1x, 0), JsApi.arg(var1x, 1)));
      this.fn("migrate", var1x -> this.migrate(JsApi.arg(var1x, 0), JsApi.arg(var1x, 1)));
      this.fn("help", var0 -> io.karatelabs.ext.common.help.HelpTopicProvider.a("traffic").a(var0));
   }

   public static Traffic at(Path var0) {
      return new Traffic(var0, var1 -> var0.resolve("runs").resolve(var1));
   }

   public Traffic withConsole(Supplier<Collection<String>> var1) {
      this.console = var1;
      return this;
   }

   public Traffic withSpecs(Supplier<List<OpenApiCoverageSource>> var1) {
      this.specs = var1;
      return this;
   }

   public Traffic withApp(Supplier<String> var1) {
      this.appUrl = var1;
      return this;
   }

   Object read(Object var1, Object var2) {
      Object var3 = this.load("Traffic.read", var1);
      if (var3 instanceof Map var11) {
         return var11;
      } else if (var2 != null && !(var2 instanceof Map)) {
         return JsApi.missing("Traffic.read(source, {app?, full?}): the options are an object");
      } else {
         var var4 = unknown("Traffic.read(source, {app?, full?})", var2, "app", "full");
         if (var4 != null) {
            return var4;
         } else {
            io.karatelabs.ext.robot.recording.TrafficLog var5 = (io.karatelabs.ext.robot.recording.TrafficLog)var3;
            Map<java.lang.String, Object> var6 = var2 == null ? Map.of() : (Map)var2;
            boolean var7 = Boolean.TRUE.equals(var6.get("full"));
            ArrayList<java.lang.Object> var8 = new ArrayList();

            for (Map var10 : var5.e()) {
               var8.add(!var7 && !var10.containsKey("lost") ? brief(var10) : whole(var10));
            }

            LinkedHashMap<java.lang.String, java.lang.Object> var12 = new LinkedHashMap();
            var12.put("app", var6.get("app") == null ? var5.a() : io.karatelabs.ext.robot.recording.RecordingEvidenceReporter.ParsedUrl.a(String.valueOf(var6.get("app"))).c());
            var12.put("actions", var5.b());
            var12.put("exchanges", var8);
            var12.put("capture", var5.c());
            return var12;
         }
      }
   }

   Object sift(Object var1, Object var2) {
      Object var3 = this.load("Traffic.sift", var1);
      if (var3 instanceof io.karatelabs.ext.robot.recording.TrafficLog var4) {
         var var5 = unknown("Traffic.sift(source, {app?, hosts?, moves?})", var2, "app", "hosts", "moves");
         return var5 != null ? var5 : this.sift("Traffic.sift(source, {app?, hosts?, moves?})", var4, var2);
      } else {
         return var3;
      }
   }

   private static Map<String, Object> unknown(String var0, Object var1, String... var2) {
      if (var1 instanceof Map var3) {
         for (Object var5 : var3.keySet()) {
            if (!List.of(var2).contains(String.valueOf(var5))) {
               return JsApi.missing(var0 + " — unknown option '" + var5 + "'");
            }
         }
      }

      return null;
   }

   private Object sift(String var1, io.karatelabs.ext.robot.recording.TrafficLog var2, Object var3) {
      if (var3 != null && !(var3 instanceof Map)) {
         return JsApi.missing(var1 + ": the options are an object");
      } else {
         Map<java.lang.String, java.lang.Object> var4 = var3 == null ? Map.of() : (Map)var3;
         String var5 = var4.get("app") == null ? var2.a() : String.valueOf(var4.get("app"));
         if (var5 != null && io.karatelabs.ext.robot.recording.RecordingEvidenceReporter.ParsedUrl.a(var5).c() != null) {
            ArrayList<java.lang.String> var6 = new ArrayList();
            if (var4.get("hosts") != null) {
               if (!(var4.get("hosts") instanceof List var7)) {
                  return JsApi.missing("hosts is a list of sites: ['https://api.acme.com']");
               }

               var7.forEach(var1x -> var6.add(String.valueOf(var1x)));
            }

            ArrayList<io.karatelabs.ext.robot.recording.RecordingEvidenceReporter.ExchangeMove> var12 = new ArrayList();
            if (var4.get("moves") != null) {
               Object var9 = var4.get("moves");
               if (!(var9 instanceof List)) {
                  return JsApi.missing("moves is a list; a move is {id, to: 'kept'|'left', by: 'tester'|'chat', reason?}");
               }

               for (Object var10 : (List)var9) {
                  if (!(var10 instanceof Map var11 && var11.get("id") != null)) {
                     return JsApi.missing("a move is {id, to: 'kept'|'left', by: 'tester'|'chat', reason?}: id is missing");
                  }

                  if (var11.get("reason") != null && !(var11.get("reason") instanceof String)) {
                     return JsApi.missing("a move's reason is a string");
                  }

                  var12.add(new io.karatelabs.ext.robot.recording.RecordingEvidenceReporter.ExchangeMove(String.valueOf(var11.get("id")), str(var11.get("to")), str(var11.get("by")), str(var11.get("reason"))));
               }
            }

            Map<java.lang.String, Object> var14 = io.karatelabs.ext.robot.recording.RecordingEvidenceReporter.a(var2, var5, var6, var12, this.console.get());
            if (var14.get("error") == null) {
               this.join(var2, var14);
            }

            return var14;
         } else {
            return JsApi.missing("no app origin: the recording opened no page — Traffic.sift(source, {app: 'https://…'})");
         }
      }
   }

   private void join(io.karatelabs.ext.robot.recording.TrafficLog var1, Map<String, Object> var2) {
      var var3 = this.specs.get();
      if (!var3.isEmpty()) {
         LinkedHashMap<java.lang.String, java.lang.String> var4 = new LinkedHashMap();

         for (Map var6 : var1.e()) {
            if (var6.get("request") instanceof Map var7 && var7.get("url") != null) {
               var4.put(String.valueOf(var6.get("id")), String.valueOf(var7.get("url")));
            }
         }

         for (Map var13 : (Iterable<Map>) (Iterable<?>) ((List)var2.get("kept"))) {
            List<CoverageItem> var14 = List.of();
            String var15 = (String)var4.get(String.valueOf(var13.get("id")));
            if (var15 != null) {
               HttpAction var9 = action(str(var13.get("method")), var15);

               for (OpenApiCoverageSource var11 : (Iterable<OpenApiCoverageSource>) (Iterable<?>) (var3)) {
                  var14 = var11.c(var9);
                  if (!var14.isEmpty()) {
                     break;
                  }
               }
            }

            LinkedHashMap<java.lang.String, java.lang.Object> var16 = new LinkedHashMap();
            if (var14.size() == 1) {
               var16.put("id", ((CoverageItem)var14.get(0)).id());
               var16.put("path", ((CoverageItem)var14.get(0)).getPath());
            } else if (var14.isEmpty()) {
               var16.put("unmatched", true);
            } else {
               var16.put("ambiguous", true);
               var16.put("candidates", var14.stream().map(CoverageItem::id).toList());
            }

            var13.put("operation", var16);
         }
      }
   }

   private static HttpAction action(String var0, String var1) {
      String var2;
      try {
         var2 = URI.create(var1).getPath();
      } catch (IllegalArgumentException var4) {
         var2 = "";
      }

      return new HttpAction(
         "traffic", "http", Set.of(), Instant.now(), null, null, 0, null, var0 == null ? "GET" : var0.toUpperCase(Locale.ROOT), var1, var2, Map.of(), 0, 0L
      );
   }

   public List<Map<String, Object>> keptExchanges(String var1) {
      if (this.load("Traffic.sift", var1) instanceof io.karatelabs.ext.robot.recording.TrafficLog var3
         && this.sift("Traffic.sift", var3, null) instanceof Map var4
         && var4.get("kept") instanceof List var5) {
         LinkedHashSet<java.lang.String> var12 = new LinkedHashSet();
         var5.forEach(var1x -> var12.add(String.valueOf(((Map)var1x).get("id"))));
         ArrayList<java.util.Map<java.lang.String,java.lang.Object>> var7 = new ArrayList();

         for (Map var9 : var3.e()) {
            if (var12.contains(String.valueOf(var9.get("id")))) {
               LinkedHashMap<java.lang.String, java.lang.Object> var10 = new LinkedHashMap();
               var10.put("request", var9.get("request"));
               if (var9.get("response") != null) {
                  var10.put("response", var9.get("response"));
               }

               var7.add(var10);
            }
         }

         return var7;
      } else {
         return List.of();
      }
   }

   Object migrate(Object var1, Object var2) {
      Object var3 = this.candidate(var1, var2);
      if (var3 instanceof Traffic.Candidate var4) {
         ArrayList<java.lang.String> var5 = new ArrayList();

         for (Entry var7 : var4.config().entrySet()) {
            var5.add(
               "ApiTest.config({name: '"
                  + (String)var7.getKey()
                  + "', value: '"
                  + (String)var7.getValue()
                  + "'"
                  + (var4.version() == null ? "" : ", ifMatch: '" + var4.version() + "'")
                  + "})"
            );
         }

         String var8 = var4.migrated().to();
         var5.add("ApiTest.land('" + var8 + "', feature)");
         return ordered(JsApi.next(new LinkedHashMap<>(var4.migrated().answer()), var5.toArray(String[]::new)));
      } else {
         return var3;
      }
   }

   static Map<String, Object> ordered(Map<String, Object> var0) {
      LinkedHashMap<java.lang.String, java.lang.Object> var1 = new LinkedHashMap();

      for (String var3 : List.of("to", "needs", "chains", "proposed", "next")) {
         if (var0.containsKey(var3)) {
            var1.put(var3, var0.get(var3));
         }
      }

      var0.forEach((var1x, var2) -> {
         if (!"feature".equals(var1x) && !"sift".equals(var1x)) {
            var1.putIfAbsent(var1x, var2);
         }
      });
      if (var0.containsKey("feature")) {
         var1.put("feature", var0.get("feature"));
      }

      if (var0.get("sift") instanceof Map var6) {
         LinkedHashMap<java.lang.String, java.lang.Object> var8 = new LinkedHashMap();
         var6.forEach((var1x, var2) -> var8.put(String.valueOf(var1x), var2));
         if (var6.get("counts") instanceof Map var4 && var4.get("byReason") != null) {
            var8.put("left", var4.get("byReason"));
         }

         var1.put("sift", var8);
      }

      return var1;
   }

   public Object candidate(Object var1, Object var2) {
      return this.candidate(var1, var2, var0 -> false);
   }

   public Object candidate(Object var1, Object var2, Predicate<Path> var3) {
      Object var4 = this.make(var1, var2, var3);
      if (var4 instanceof io.karatelabs.ext.max.jsapi.Traffic.MigrationArtifact var5) {
         LinkedHashMap<java.lang.String, java.lang.String> var6 = new LinkedHashMap();
         ArrayList<java.lang.String> var7 = new ArrayList();
         io.karatelabs.ext.robot.recording.RecordingEvidenceReporter.ParsedUrl var8 = this.appUrl.get() == null ? null : io.karatelabs.ext.robot.recording.RecordingEvidenceReporter.ParsedUrl.a(this.appUrl.get());
         List var9 = var8 != null && var8.g() != null ? segments(var8.g()) : List.of();

         for (io.karatelabs.ext.robot.recording.RobotRecording.RecordedPage var11 : var5.b().d()) {
            if (!"appUrl".equals(var11.a()) && !var5.c().contains(var11.a())) {
               String var12 = var11.b().equals(var5.b().b()) && isTheApp(var5.b(), var11.a(), var9) ? var8.c() : null;
               if (var12 != null && !var12.equals(var11.b())) {
                  String var13 = var5.d()
                     .entrySet()
                     .stream()
                     .filter(var1x -> var12.equals(String.valueOf(var1x.getValue())))
                     .map(Entry::getKey)
                     .findFirst()
                     .map(var0 -> " (" + var0 + ")")
                     .orElse("");
                  var7.add(
                     var11.a()
                        + " defaults to the recorded "
                        + var11.b()
                        + ": set it to "
                        + var12
                        + ", appUrl's origin"
                        + var13
                        + ", not appUrl: the paths carry /"
                        + String.join("/", var9)
                  );
                  var6.put(var11.a(), var12);
               } else {
                  var7.add(var11.a() + " defaults to the recorded " + var11.b() + "; set it with ApiTest.config only to point elsewhere");
               }
            }
         }

         if (!var7.isEmpty()) {
            JsApi.hint(var5.a().answer(), String.join("; ", var7));
         }

         return new Traffic.Candidate(var5.a(), var6, var5.e());
      } else {
         return var4;
      }
   }

   private static boolean isTheApp(io.karatelabs.ext.robot.recording.RobotRecording var0, String var1, List<String> var2) {
      return !var2.isEmpty()
         && var0.i()
            .stream()
            .flatMap(var0x -> var0x.b().stream())
            .filter(var1x -> var1.equals(var1x.h()))
            .allMatch(var1x -> var1x.i().size() >= var2.size() && var1x.i().subList(0, var2.size()).equals(var2));
   }

   private static List<String> segments(String var0) {
      ArrayList<java.lang.String> var1 = new ArrayList();

      for (String var5 : var0.split("/")) {
         if (!var5.isEmpty()) {
            var1.add(URLDecoder.decode(var5.replace("+", "%2B"), StandardCharsets.UTF_8));
         }
      }

      return var1;
   }

   public Object migration(Object var1, Object var2) {
      Object var3 = this.make(var1, var2, var0 -> false);
      return var3 instanceof io.karatelabs.ext.max.jsapi.Traffic.MigrationArtifact var4 ? var4.a() : var3;
   }

   private Object make(Object var1, Object var2, Predicate<Path> var3) {
      Object var4 = this.load("Traffic.migrate", var1);
      if (var4 instanceof io.karatelabs.ext.robot.recording.TrafficLog var5) {
         var var6 = unknown("Traffic.migrate(source, {to?, app?, hosts?, moves?})", var2, "to", "app", "hosts", "moves");
         if (var6 != null) {
            return var6;
         } else {
            Object var7 = this.sift("Traffic.migrate(source, {to?, app?, hosts?, moves?})", var5, var2);
            if (var7 instanceof Map var8 && var8.get("error") != null) {
               return var7;
            } else {
               Map var22 = (Map)var7;
               Map<java.lang.String, Object> var9 = var2 instanceof Map var10 ? var10 : Map.of();
               String var23 = null;
               if (var9.get("to") != null) {
                  var23 = String.valueOf(var9.get("to")).replaceFirst("^/+", "");
                  if (!var23.endsWith(".feature")) {
                     return JsApi.missing("Traffic.migrate: to is a project-relative .feature path, got " + var23);
                  }

                  try {
                     Path var11 = MaxPaths.resolveSandboxed(this.root, var23);
                     if (Files.exists(var11) && !var3.test(var11)) {
                        var var25 = JsApi.missing(var23 + " already exists — pass another to");
                        var25.put("code", "EXISTS");
                        var25.put("status", 409);
                        return var25;
                     }
                  } catch (InvalidPathException | SecurityException var21) {
                     return JsApi.missing("Traffic.migrate: to is a project-relative .feature path: " + var21.getMessage());
                  }
               }

               String var24 = this.configText();
               var var12 = configVars(var24);
               var var13 = configValues(var24);
               io.karatelabs.ext.robot.recording.RobotRecording var14 = io.karatelabs.ext.robot.recording.RobotRecording.a(var5, var22, this.appUrl.get(), this.preset(var24), var12, var13);
               if (var14.a() != null) {
                  LinkedHashMap<java.lang.String, java.lang.Object> var27 = new LinkedHashMap();
                  var27.put("error", "Traffic.migrate: " + var14.a().c());
                  var27.put("code", var14.a().a());
                  var27.put("status", var14.a().b());
                  if (!var14.k().isEmpty()) {
                     var27.put("notConverted", gaps(var14));
                  }

                  return var27;
               } else {
                  if (var23 == null) {
                     String var15 = io.karatelabs.ext.robot.recording.RecordingScriptGenerator.a(this.recordingName(var1));
                     String var16 = "checks/" + (var15 != null ? var15 : io.karatelabs.ext.robot.recording.RecordingScriptGenerator.a(var14));
                     var23 = this.owned(var16, var3);
                     if (var23 == null) {
                        var23 = var16 + ".feature";

                        for (int var17 = 2; Files.exists(this.root.resolve(var23)); var17++) {
                           var23 = var16 + "-" + var17 + ".feature";
                        }
                     }
                  }

                  io.karatelabs.ext.robot.recording.RecordingScriptGenerator.GeneratedScript var26;
                  try {
                     var26 = io.karatelabs.ext.robot.recording.RecordingScriptGenerator.a(var14, String.valueOf(var1));
                  } catch (IllegalStateException var20) {
                     return JsApi.missing("Traffic.migrate wrote no feature: " + var20.getMessage());
                  }

                  LinkedHashMap<java.lang.String, java.lang.Object> var28 = new LinkedHashMap();
                  var28.put("feature", var26.a());
                  var28.put("to", var23);
                  var28.put("chains", chains(var14));
                  ArrayList<java.lang.String> var29 = new ArrayList();
                  var14.f().stream().filter(io.karatelabs.ext.robot.recording.RobotRecording.TypedInputVariable::d).forEach(var1x -> var29.add(var1x.a()));
                  var14.e().stream().filter(var12::contains).forEach(var29::add);
                  var28.put("needs", var14.e().stream().filter(var1x -> !var12.contains(var1x)).toList());
                  var28.put("reads", var29);
                  var28.put("notConverted", gaps(var14));
                  ArrayList<java.util.Map<java.lang.String,java.lang.Object>> var18 = new ArrayList();
                  var14.l().forEach(var1x -> var18.add(row("id", var1x.a(), "what", var1x.b(), "why", var1x.c())));
                  var28.put("notChecked", var18);
                  var28.put("proposed", var26.b());
                  ArrayList<java.util.Map<java.lang.String,java.lang.Object>> var19 = new ArrayList();
                  var14.n().forEach(var1x -> var19.add(row("id", var1x.a(), "field", var1x.b(), "why", var1x.c())));
                  var28.put("notProposed", var19);
                  var28.put("sift", var22);
                  return new io.karatelabs.ext.max.jsapi.Traffic.MigrationArtifact(
                     new Postman.Migrated(var23, var26.a().getBytes(StandardCharsets.UTF_8), var28),
                     var14,
                     var12,
                     var13,
                     var24 == null ? null : this.configVersion()
                  );
               }
            }
         }
      } else {
         return var4;
      }
   }

   private static List<Map<String, Object>> chains(io.karatelabs.ext.robot.recording.RobotRecording var0) {
      ArrayList<java.util.Map<java.lang.String,java.lang.Object>> var1 = new ArrayList();

      for (io.karatelabs.ext.robot.recording.RobotRecording.RecordedEndpoint var3 : var0.j()) {
         LinkedHashMap<java.lang.String, java.lang.Object> var4 = new LinkedHashMap();
         if (var3.a() != null) {
            var4.put("name", var3.a());
         }

         if (var3.b() != null) {
            var4.put("from", ref(var3.b()));
         }

         var4.put("to", var3.c().stream().map(Traffic::ref).toList());
         if (var3.d() != null) {
            var4.put("pulled", var3.d());
         }

         if (var3.e() != null) {
            var4.put("guessed", var3.e());
         }

         var1.add(var4);
      }

      return var1;
   }

   private static Map<String, Object> ref(io.karatelabs.ext.robot.recording.RobotRecording.NamedValue var0) {
      return row("id", var0.a(), "at", var0.b());
   }

   private static List<Map<String, Object>> gaps(io.karatelabs.ext.robot.recording.RobotRecording var0) {
      ArrayList<java.util.Map<java.lang.String,java.lang.Object>> var1 = new ArrayList();
      var0.k().forEach(var1x -> var1.add(row("id", var1x.a(), "code", var1x.b(), "text", var1x.c())));
      return var1;
   }

   private static Map<String, Object> row(Object... var0) {
      LinkedHashMap<java.lang.String, java.lang.Object> var1 = new LinkedHashMap();

      for (byte var2 = 0; var2 < var0.length; var2 += 2) {
         var1.put(String.valueOf(var0[var2]), var0[var2 + 1]);
      }

      return var1;
   }

   private String configVersion() {
      try {
         return io.karatelabs.ext.common.Sha256.a(Files.readAllBytes(this.root.resolve("karate-config.js")));
      } catch (IOException var2) {
         return null;
      }
   }

   private String configText() {
      try {
         Path var1 = this.root.resolve("karate-config.js");
         return Files.isRegularFile(var1) ? Files.readString(var1) : null;
      } catch (IOException var2) {
         return null;
      }
   }

   private Map<String, Object> preset(String var1) {
      KarateConfigBlock.State var2 = KarateConfigBlock.read(var1);
      if (var2.error() != null) {
         return null;
      } else {
         for (Map var4 : var2.entries()) {
            if ("auth".equals(var4.get("name")) && var4.get("env") == null && var4.get("auth") instanceof Map var5) {
               LinkedHashMap<java.lang.String, java.lang.Object> var9 = new LinkedHashMap(var5);
               if (var9.get("login") instanceof String var7) {
                  var9.put("loginSendsToAppUrl", this.sendsToAppUrl(var7));
               }

               return var9;
            }
         }

         return null;
      }
   }

   private boolean sendsToAppUrl(String var1) {
      try {
         Path var2 = MaxPaths.resolveSandboxed(this.root, var1);
         return Files.isRegularFile(var2) && sendsToAppUrl(Files.readString(var2).split("\\R"));
      } catch (SecurityException | InvalidPathException | IOException var3) {
         return false;
      }
   }

   static boolean sendsToAppUrl(String[] var0) {
      boolean var1 = false;
      boolean var2 = false;
      boolean var3 = false;
      boolean var4 = false;
      String var5 = null;
      String var6 = null;

      for (String var10 : var0) {
         String var11 = var10.strip();
         if (var11.startsWith("\"\"\"") || var11.startsWith("```")) {
            var3 = !var3;
         } else if (!var3) {
            if (!var11.startsWith("Background:") && !var11.startsWith("Scenario")) {
               Matcher var13 = URL_OR_METHOD.matcher(var10);
               if (var13 instanceof Matcher && var13.matches()) {
                  if ("url".equals(var13.group(1))) {
                     var6 = var13.group(2).strip();
                     var5 = var2 ? var6 : var5;
                     var1 = true;
                  } else {
                     if (var6 == null || !APP_BASED.matcher(var6).matches()) {
                        return false;
                     }

                     var4 = true;
                  }
               }
            } else {
               if (!ended(var2, var4, var6)) {
                  return false;
               }

               var2 = var11.startsWith("Background:");
               var6 = var5;
               var4 = false;
            }
         }
      }

      return var1 && ended(var2, var4, var6);
   }

   private static boolean ended(boolean var0, boolean var1, String var2) {
      return var0 || var1 || var2 == null || APP_BASED.matcher(var2).matches();
   }

   private static Map<String, Object> configValues(String var0) {
      LinkedHashMap<java.lang.String, java.lang.Object> var1 = new LinkedHashMap();
      KarateConfigBlock.State var2 = KarateConfigBlock.read(var0);
      if (var2.error() == null) {
         var2.entries()
            .stream()
            .filter(var0x -> var0x.get("env") == null && var0x.containsKey("value"))
            .forEach(var1x -> var1.put(String.valueOf(var1x.get("name")), var1x.get("value")));
      }

      KarateConfigBlock.handWritten(var0)
         .stream()
         .filter(var0x -> var0x.containsKey("value"))
         .forEach(var1x -> var1.putIfAbsent(String.valueOf(var1x.get("name")), var1x.get("value")));
      return var1;
   }

   private static Set<String> configVars(String var0) {
      LinkedHashSet<java.lang.String> var1 = new LinkedHashSet();
      KarateConfigBlock.State var2 = KarateConfigBlock.read(var0);
      if (var2.error() == null) {
         var2.entries().forEach(var1x -> var1.add(String.valueOf(var1x.get("name"))));
      }

      KarateConfigBlock.handWritten(var0).forEach(var1x -> var1.add(String.valueOf(var1x.get("name"))));
      return var1;
   }

   private Object load(String var1, Object var2) {
      if (var2 instanceof String var3 && !var3.isBlank()) {
         Path var4;
         if (!var3.contains("/") && !var3.contains("\\") && !var3.endsWith(".jsonl")) {
            Path var5;
            try {
               var5 = this.runDir.apply(var3);
            } catch (InvalidPathException var7) {
               return JsApi.missing(var1 + ": not a run id: " + var7.getReason());
            }

            if (!Files.isDirectory(var5)) {
               return JsApi.notFound(var1 + ": no run '" + var3 + "' — " + this.recordings());
            }

            var4 = var5.resolve("traffic.jsonl");
         } else {
            try {
               var4 = MaxPaths.resolveSandboxed(this.root, var3);
            } catch (SecurityException var8) {
               return JsApi.missing(var1 + " denied: " + var8.getMessage());
            } catch (InvalidPathException var9) {
               return JsApi.missing(var1 + ": not a path: " + var9.getReason());
            }

            if (Files.isDirectory(var4)) {
               var4 = var4.resolve("traffic.jsonl");
            }
         }

         return io.karatelabs.ext.robot.recording.TrafficLog.a(var4, var3);
      } else {
         return JsApi.missing(var1 + "(source, …) needs a recording's run id or a project-relative traffic.jsonl — " + this.recordings());
      }
   }

   private String owned(String var1, Predicate<Path> var2) {
      Path var3 = this.root.resolve(var1).getParent();
      Pattern var4 = Pattern.compile(Pattern.quote(this.root.resolve(var1).getFileName().toString()) + "(-\\d+)?\\.feature");

      try {
         String var6;
         try (var var5 = Files.list(var3)) {
            var6 = var5.filter(var1x -> var4.matcher(var1x.getFileName().toString()).matches())
               .sorted()
               .filter(var2)
               .findFirst()
               .map(var1x -> this.root.relativize(var1x).toString().replace('\\', '/'))
               .orElse(null);
         }

         return var6;
      } catch (RuntimeException | IOException var10) {
         return null;
      }
   }

   private String recordingName(Object var1) {
      String var2 = String.valueOf(var1);

      try {
         Path var3 = !var2.contains("/") && !var2.contains("\\") && !var2.endsWith(".jsonl")
            ? this.runDir.apply(var2)
            : MaxPaths.resolveSandboxed(this.root, var2);
         Path var4 = (Files.isDirectory(var3) ? var3 : var3.getParent()).resolve("meta.json");
         return Json.parseStrict(Files.readString(var4), true) instanceof Map var6 && var6.get("name") instanceof String var5 ? var5 : null;
      } catch (RuntimeException | IOException var8) {
         return null;
      }
   }

   private String recordings() {
      Path var1 = this.runDir.apply("x").getParent();
      ArrayList<java.util.Map.Entry<java.lang.Long,java.lang.String>> var2 = new ArrayList();

      try (var var3 = Files.list(var1)) {
         var3.filter(var0 -> Files.isRegularFile(var0.resolve("traffic.jsonl"))).forEach(var1x -> {
            var var2x = Map.of();

            try {
               var2x = Json.parseStrict(Files.readString(var1x.resolve("meta.json")), true) instanceof Map var3x ? var3x : Map.of();
            } catch (RuntimeException | IOException var9x) {
            }

            long var10;
            try {
               var10 = var2x.get("startedMs") instanceof Number var5 ? var5.longValue() : Files.getLastModifiedTime(var1x.resolve("traffic.jsonl")).toMillis();
            } catch (IOException var8x) {
               var10 = 0L;
            }

            String var11 = var2x.get("address") instanceof String var12 ? io.karatelabs.ext.robot.recording.RecordingEvidenceReporter.ParsedUrl.a(var12).g() : null;
            var2.add(Map.entry(var10, var1x.getFileName() + (var11 == null ? "" : " (" + var11 + ")")));
         });
      } catch (RuntimeException | IOException var8) {
      }

      if (var2.isEmpty()) {
         return "this project has no recordings";
      } else {
         var2.sort(Entry.<Long, String>comparingByKey().reversed());
         var var9 = var2.stream().limit(5L).map(Entry::getValue).toList();
         return "recordings, newest first: " + String.join(", ", var9) + (var2.size() > 5 ? ", …" : "");
      }
   }

   private static Map<String, Object> whole(Map<String, Object> var0) {
      var var1 = new LinkedHashMap<>(var0);
      var1.remove("row");
      return var1;
   }

   private static Map<String, Object> brief(Map<String, Object> var0) {
      Map<java.lang.String, Object> var1 = var0.get("request") instanceof Map var2 ? var2 : Map.of();
      Map<java.lang.String, Object> var6 = var0.get("response") instanceof Map var7 ? var7 : null;
      LinkedHashMap<java.lang.String, java.lang.Object> var8 = new LinkedHashMap();
      var8.put("id", var0.get("id"));
      var8.put("after", var0.get("after"));
      var8.put("method", var1.get("method"));
      var8.put("url", cut(str(var1.get("url"))));
      if (var1.get("params") instanceof Map var9 && !var9.isEmpty()) {
         var8.put("params", var9);
      }

      var8.put("status", var6 == null ? null : var6.get("status"));
      var8.put("type", var0.get("type"));
      var8.put("bodyState", var0.get("bodyState"));
      body(var8, "request", var1.get("body"));
      body(var8, "response", var6 == null ? null : var6.get("body"));
      if (var0.get("hops") instanceof Number var10 && var10.intValue() > 1) {
         var8.put("hops", var10);
      }

      for (String var13 : List.of("error", "headersPartial")) {
         if (var0.get(var13) != null) {
            var8.put(var13, var0.get(var13));
         }
      }

      return var8;
   }

   private static void body(Map<String, Object> var0, String var1, Object var2) {
      if (var2 != null && !(var2 instanceof String var3 && var3.isEmpty())) {
         LinkedHashMap<java.lang.String, java.lang.Object> var5 = new LinkedHashMap();
         var5.put("shape", shape(var2, 0));
         var5.put("sample", cut(var2 instanceof String var4 ? var4 : StringUtils.formatJson(var2, false, false, false)));
         var0.put(var1, var5);
      }
   }

   private static Object shape(Object var0, int var1) {
      if (var0 instanceof Map var4) {
         if (var1 >= 3) {
            return "object";
         } else {
            LinkedHashMap<java.lang.String, java.lang.Object> var3 = new LinkedHashMap();
            var4.forEach((var2x, var3x) -> var3.put(String.valueOf(var2x), shape(var3x, var1 + 1)));
            return var3;
         }
      } else if (!(var0 instanceof List var2)) {
         return var0 == null
            ? "null"
            : (var0 instanceof String ? "string" : (var0 instanceof Number ? "number" : (var0 instanceof Boolean ? "boolean" : "string")));
      } else {
         return !var2.isEmpty() && var1 < 3 ? List.of(shape(var2.get(0), var1 + 1)) : List.of();
      }
   }

   private static String cut(String var0) {
      return var0 != null && var0.length() > 160 ? var0.substring(0, 160) + "…" : var0;
   }

   private static String str(Object var0) {
      return var0 == null ? null : String.valueOf(var0);
   }

   public record Candidate(Postman.Migrated migrated, Map<String, String> config, String version) {

   }

   private record MigrationArtifact(Postman.Migrated a, io.karatelabs.ext.robot.recording.RobotRecording b, Set<String> c, Map<String, Object> d, String e) {

   }
}
