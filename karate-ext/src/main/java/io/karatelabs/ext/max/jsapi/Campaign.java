package io.karatelabs.ext.max.jsapi;

import io.karatelabs.common.Json;
import io.karatelabs.ext.common.MaxPaths;
import io.karatelabs.ext.common.jsapi.JsApi;
import io.karatelabs.ext.common.jsapi.JsDispatch;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.Normalizer;
import java.text.Normalizer.Form;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;
import java.util.function.Supplier;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Campaign extends JsDispatch {
   private static final Logger logger = LoggerFactory.getLogger(Campaign.class);
   public static final String DIR = "campaigns";
   static final String SCHEMA = "karate-campaign/v1";
   private static final int MAX_NAME = 80;
   private static final long MAX_FILE = 1048576L;
   private static final String CREATE_USAGE = "Campaign.create({name, tests?, req?}) — name the campaign and give it tests (paths from Runner.tests()) and/or requirement ids whose tests it takes, e.g. Campaign.create({name: 'Release 1.3', req: ['ORD-001']})";
   private static final String RUN_USAGE = "Campaign.run(name[, {only?, async?, video?, env?}])";
   private static final Set<String> RUN_OPTIONS = Set.of("only", "async", "video", "env");
   private static final Map<Path, Object> CREATE_LOCKS = new ConcurrentHashMap<>();
   private final Path root;
   private final Runner runner;
   private final RunMetadataStore store;
   private Harness harness;
   private BiFunction<List<Path>, List<String>, Map<String, Object>> graphs = (var0, var1x) -> null;
   private Supplier<Object> requirements = List::of;
   private boolean noProject;

   public Campaign(Path var1, Runner var2) {
      this.root = var1;
      this.runner = var2;
      this.store = new RunMetadataStore(var1);
      this.fn("help", var0 -> JsApi.helpTopic(help(), var0));
      this.fn("create", var1x -> this.create(JsApi.arg(var1x, 0)));
      this.fn("list", var1x -> this.list());
      this.fn("get", var1x -> this.get(JsApi.str(var1x, 0)));
      this.fn("run", var1x -> this.run(JsApi.str(var1x, 0), JsApi.arg(var1x, 1)));
      this.fn("delete", var1x -> this.delete(JsApi.str(var1x, 0)));
   }

   public Campaign withHarness(Harness var1) {
      this.harness = var1;
      return this;
   }

   public Campaign withGraphs(BiFunction<List<Path>, List<String>, Map<String, Object>> var1) {
      this.graphs = var1;
      return this;
   }

   public Campaign withRequirements(Supplier<Object> var1) {
      this.requirements = var1;
      return this;
   }

   public Campaign withNoProject(boolean var1) {
      this.noProject = var1;
      return this;
   }

   Map<String, Object> create(Object var1) {
      if (this.noProject) {
         return JsApi.noProjectSelected("Campaign.create");
      } else if (var1 instanceof Map var2) {
         String var3 = var2.get("name") instanceof String var4 ? var4.trim() : "";
         if (!var3.codePoints().noneMatch(Character::isLetterOrDigit) && var3.length() <= 80) {
            var var18 = this.runner.tests(null);
            var var19 = byPath(var18);
            LinkedHashSet var6 = new LinkedHashSet();

            for (String var8 : strings(var2.get("tests"))) {
               String var9 = normalize(var8);
               if (!var19.containsKey(var9)) {
                  return JsApi.suggest(
                     JsApi.next(JsApi.missing("'" + var8 + "' is not a test of this project"), "Runner.tests()"),
                     io.karatelabs.ext.common.jsapi.SuggestionEngine.a(var9, var19.keySet())
                  );
               }

               var6.add(var9);
            }

            for (String var21 : strings(var2.get("req"))) {
               var var23 = var18.stream()
                  .filter(var1x -> Runner.declares((Map<String, Object>)var1x, var21))
                  .map(var0 -> String.valueOf(var0.get("path")))
                  .sorted()
                  .toList();
               if (var23.isEmpty()) {
                  return JsApi.next(
                     JsApi.missing("no test declares '" + var21 + "' — link a test to it first, or name the tests yourself"),
                     "Runner.tests()",
                     "Requirement.list()"
                  );
               }

               var6.addAll(var23);
            }

            if (var6.isEmpty()) {
               return JsApi.missing(
                  "Campaign.create({name, tests?, req?}) — name the campaign and give it tests (paths from Runner.tests()) and/or requirement ids whose tests it takes, e.g. Campaign.create({name: 'Release 1.3', req: ['ORD-001']})"
               );
            } else {
               synchronized (CREATE_LOCKS.computeIfAbsent(MaxPaths.dirLockKey(this.dir()), var0 -> new Object())) {
                  io.karatelabs.ext.max.jsapi.Campaign.CampaignDefinition var22 = this.find(var3);
                  if (var22 != null && !Boolean.TRUE.equals(var2.get("overwrite"))) {
                     LinkedHashMap var25 = new LinkedHashMap();
                     var25.put(
                        "error", "a campaign named '" + var22.c() + "' already exists — pass {overwrite:true} to replace its tests, or pick another name"
                     );
                     var25.put("code", "EXISTS");
                     var25.put("status", 409);
                     return var25;
                  } else {
                     Path var24 = var22 != null ? var22.a() : this.freeFile(slug(var3));
                     String var10 = var22 != null ? var22.b() : UUID.randomUUID().toString();
                     long var11 = var22 != null && var22.d() instanceof Number var13 ? var13.longValue() : System.currentTimeMillis();
                     LinkedHashMap var26 = new LinkedHashMap();
                     var26.put("schema", "karate-campaign/v1");
                     var26.put("id", var10);
                     var26.put("name", var3);
                     var26.put("createdMs", var11);
                     var26.put("tests", List.copyOf(var6));

                     try {
                        Files.createDirectories(var24.getParent());
                        io.karatelabs.ext.common.AtomicFileWriter.a(var24, Json.of(var26).toStringPretty() + "\n");
                     } catch (Exception var16) {
                        return JsApi.error(new RuntimeException("Campaign.create: could not write " + this.rel(var24) + ": " + var16.getMessage(), var16));
                     }

                     io.karatelabs.ext.max.jsapi.Campaign.CampaignDefinition var27 = new io.karatelabs.ext.max.jsapi.Campaign.CampaignDefinition(var24, var10, var3, var11, List.copyOf(var6));
                     return JsApi.next(this.state(var27, var19, true), "Campaign.run(" + io.karatelabs.ext.jsformat.JsFormatter.toJsLiteral((Object)var3) + ", {async: true})");
                  }
               }
            }
         } else {
            return JsApi.missing(
               "Campaign.create({name, tests?, req?}) — name the campaign and give it tests (paths from Runner.tests()) and/or requirement ids whose tests it takes, e.g. Campaign.create({name: 'Release 1.3', req: ['ORD-001']}) — the name needs a letter or a digit, at most 80 characters"
            );
         }
      } else {
         return JsApi.missing(
            "Campaign.create({name, tests?, req?}) — name the campaign and give it tests (paths from Runner.tests()) and/or requirement ids whose tests it takes, e.g. Campaign.create({name: 'Release 1.3', req: ['ORD-001']})"
         );
      }
   }

   public List<Map<String, Object>> list() {
      ArrayList var1 = new ArrayList();
      List var2 = this.defs(var1);
      if (!var1.isEmpty()) {
         io.karatelabs.ext.common.jsapi.EvaluationMessageScope.a(String.join("; ", var1));
      }

      if (var2.isEmpty()) {
         io.karatelabs.ext.common.jsapi.EvaluationMessageScope.a("no campaign yet — Campaign.create({name, tests}) names a set of tests to launch together");
         return new ArrayList<>();
      } else {
         var var3 = this.runs();
         ArrayList var4 = new ArrayList();

         for (io.karatelabs.ext.max.jsapi.Campaign.CampaignDefinition var6 : (Iterable<io.karatelabs.ext.max.jsapi.Campaign.CampaignDefinition>) (Iterable<?>) (var2)) {
            var var7 = this.header(var6, launches(var6, var3));
            LinkedHashMap var8 = new LinkedHashMap();
            var8.put("total", var6.e().size());
            LinkedHashMap var9 = new LinkedHashMap();
            var9.put("tests", var8);
            var7.put("totals", var9);
            var4.add(var7);
         }

         var4.sort(
            Comparator.<Map>comparingLong(
                  var0 -> Math.max(
                     var0.get("lastRunAt") instanceof Number var1x ? var1x.longValue() : 0L,
                     var0.get("createdMs") instanceof Number var3x ? var3x.longValue() : 0L
                  )
               )
               .reversed()
         );
         return var4;
      }
   }

   public int count() {
      return this.defs(null).size();
   }

   public Map<String, Object> get(String var1) {
      io.karatelabs.ext.max.jsapi.Campaign.CampaignDefinition var2 = this.find(var1);
      if (var2 == null) {
         return this.notFound(var1);
      } else {
         String var3 = io.karatelabs.ext.jsformat.JsFormatter.format(var2.c());
         return JsApi.next(
            this.state(var2, byPath(this.runner.tests(null)), true),
            "Campaign.run(" + var3 + ", {async: true})",
            "Campaign.run(" + var3 + ", {only: 'failed'})"
         );
      }
   }

   Map<String, Object> run(String var1, Object var2) {
      if (this.noProject) {
         return JsApi.noProjectSelected("Campaign.run");
      } else {
         io.karatelabs.ext.max.jsapi.Campaign.CampaignDefinition var3 = this.find(var1);
         if (var3 == null) {
            return this.notFound(var1);
         } else {
            String var4 = io.karatelabs.ext.jsformat.JsFormatter.format(var3.c());
            LinkedHashMap var5 = new LinkedHashMap();
            if (var2 instanceof Map var6) {
               for (Entry var8 : (Iterable<Entry>) (Iterable<?>) (var6.entrySet())) {
                  String var9 = String.valueOf(var8.getKey());
                  if (!RUN_OPTIONS.contains(var9)) {
                     return JsApi.missing("Campaign.run(name[, {only?, async?, video?, env?}]) — unknown option '" + var9 + "'");
                  }

                  var5.put(var9, var8.getValue());
               }
            }

            Object var13 = var5.remove("only");
            if (var13 != null && !"failed".equals(var13)) {
               return JsApi.missing("Campaign.run(name[, {only?, async?, video?, env?}]) — only takes 'failed' (the tests that failed their last launch)");
            } else {
               var var14 = byPath(this.runner.tests(null));
               Object var15 = var3.e();
               if ("failed".equals(var13)) {
                  var15 = new ArrayList();

                  for (Map var10 : maps(this.state(var3, var14, false).get("tests"))) {
                     if ("failed".equals(var10.get("status"))) {
                        ((java.util.List)var15).add(String.valueOf(var10.get("path")));
                     }
                  }

                  if (((java.util.List)var15).isEmpty()) {
                     return JsApi.next(JsApi.missing("no test of '" + var3.c() + "' failed its last launch"), "Campaign.run(" + var4 + ")");
                  }
               }

               ArrayList var17 = new ArrayList();
               ArrayList var18 = new ArrayList();

               for (String var12 : (Iterable<String>) (Iterable<?>) (var15)) {
                  (var14.containsKey(var12) ? var17 : var18).add(var12);
               }

               if (var17.isEmpty()) {
                  return JsApi.missing(
                     "none of the tests of '"
                        + var3.c()
                        + "' exists any more: "
                        + var18
                        + " — Campaign.create({name: "
                        + var4
                        + ", tests: [...], overwrite: true}) gives it new ones"
                  );
               } else {
                  var5.put("campaign", var3.c());
                  var5.put("campaignId", var3.b());
                  var var19 = new LinkedHashMap<>(this.runner.run(new ArrayList(var17), var5));
                  var19.put("campaign", var3.c());
                  if (!var18.isEmpty()) {
                     var19.put("skipped", var18);
                  }

                  return JsApi.next(var19, "Campaign.get(" + var4 + ")");
               }
            }
         }
      }
   }

   Map<String, Object> delete(String var1) {
      if (this.noProject) {
         return JsApi.noProjectSelected("Campaign.delete");
      } else {
         io.karatelabs.ext.max.jsapi.Campaign.CampaignDefinition var2 = this.find(var1);
         if (var2 == null) {
            return this.notFound(var1);
         } else {
            try {
               Files.delete(var2.a());
            } catch (Exception var4) {
               return JsApi.error(new RuntimeException("Campaign.delete: could not delete " + this.rel(var2.a()) + ": " + var4.getMessage(), var4));
            }

            LinkedHashMap var3 = new LinkedHashMap();
            var3.put("deleted", var2.c());
            var3.put("file", this.rel(var2.a()));
            return var3;
         }
      }
   }

   private static List<Map<String, Object>> launches(io.karatelabs.ext.max.jsapi.Campaign.CampaignDefinition var0, List<Map<String, Object>> var1) {
      ArrayList var2 = new ArrayList();

      for (Map var4 : var1) {
         if (var0.b().equals(var4.get("campaignId")) && !Boolean.TRUE.equals(var4.get("dryRun"))) {
            var2.add(var4);
         }
      }

      return var2;
   }

   private Map<String, Object> header(io.karatelabs.ext.max.jsapi.Campaign.CampaignDefinition var1, List<Map<String, Object>> var2) {
      LinkedHashMap var3 = new LinkedHashMap();
      var3.put("id", var1.b());
      var3.put("name", var1.c());
      var3.put("file", this.rel(var1.a()));
      var3.put("createdMs", var1.d());
      if (!var2.isEmpty()) {
         Map var4 = (Map)var2.get(0);
         var3.put("lastRunAt", var4.get("startedMs"));
         var3.put("lastRunId", var4.get("runId"));
         var3.put("lastStatus", outcome(var4));
      }

      var3.put("launches", var2.size());
      return var3;
   }

   private static String outcome(Map<String, Object> var0) {
      if (isRunning(var0)) {
         return "running";
      } else {
         String var1 = String.valueOf(var0.get("status"));

         return switch (var1) {
            case "completed" -> "passed";
            case "failed" -> "failed";
            default -> "notrun";
         };
      }
   }

   private Map<String, Object> state(io.karatelabs.ext.max.jsapi.Campaign.CampaignDefinition var1, Map<String, Map<String, Object>> var2, boolean var3) {
      var var4 = launches(var1, this.runs());
      ArrayList var5 = new ArrayList();

      for (Map var7 : (Iterable<Map>) (Iterable<?>) (var4)) {
         Path var8 = this.store.a(String.valueOf(var7.get("runId")));
         if (Files.isDirectory(var8)) {
            var5.add(var8);
         }
      }

      Map var9 = !var5.isEmpty() && !var1.e().isEmpty() ? this.graphs.apply(var5, var1.e()) : null;
      Object var10 = var9 != null ? var9.get("error") : null;
      if (var10 != null) {
         logger.warn("campaign {}: its launches could not be read: {}", var1.c(), var10);
         var9 = null;
      }

      Map var11 = this.view(var1, var4, var9, var2, var3 ? this.requirementIndex() : Map.of(), var3);
      if (var10 != null) {
         var11.put("unread", "its launches could not be read, so no test reads a verdict: " + var10);
      }

      return var11;
   }

   private Map<String, Object> view(
      io.karatelabs.ext.max.jsapi.Campaign.CampaignDefinition var1,
      List<Map<String, Object>> var2,
      Map<String, Object> var3,
      Map<String, Map<String, Object>> var4,
      Map<String, Map<String, Object>> var5,
      boolean var6
   ) {
      var var7 = var1.e();
      HashMap<String, List<Map<String, Object>>> var8 = new HashMap<>();
      HashMap var9 = new HashMap();
      if (var3 != null) {
         for (Map var11 : io.karatelabs.ext.common.jsapi.CoverageGraphAccessor.a(var3, "tests")) {
            String var12 = io.karatelabs.ext.max.jsapi.TestHistoryIndex.c(var11);
            if (var12 != null && !Boolean.TRUE.equals(var11.get("retired"))) {
               ((java.util.List)(var8.computeIfAbsent(var12, var0 -> new ArrayList<>()))).add(var11);
            }
         }

         for (Map var27 : io.karatelabs.ext.common.jsapi.CoverageGraphAccessor.a(var3, "executions")) {
            var9.put(String.valueOf(var27.get("id")), var27);
         }
      }

      var var26 = io.karatelabs.ext.max.jsapi.TestHistoryIndex.b(var3);
      var var28 = var2.stream().filter(Campaign::isRunning).findFirst().orElse(null);
      ArrayList var29 = new ArrayList();
      int var13 = 0;
      int var14 = 0;
      int var15 = 0;

      for (String var17 : (Iterable<String>) (Iterable<?>) (var7)) {
         LinkedHashMap var18 = new LinkedHashMap();
         var18.put("path", var17);
         Map var19 = (Map)var4.get(var17);
         if (var19 == null) {
            var18.put("missing", true);
         } else if (var19.get("title") != null) {
            var18.put("title", var19.get("title"));
         }

         String var20 = verdict(var8.getOrDefault(var17, List.of()), var9, var18);
         if (var28 != null && Harness.holds(var28, io.karatelabs.ext.max.jsapi.RunEvidenceInspector.a(var17))) {
            var20 = "running";
            var18.put("runId", var28.get("runId"));
         } else if (var18.get("runId") instanceof String var21) {
            io.karatelabs.ext.max.jsapi.TestHistoryIndex.TestRunAggregate var37 = (io.karatelabs.ext.max.jsapi.TestHistoryIndex.TestRunAggregate)var26.getOrDefault(var17, Map.of()).get(var21);
            if (var37 != null && var37.e) {
               var18.put("durationMs", var37.d);
            }

            if ("failed".equals(var20)) {
               String var23 = memberError(var2, var21, var17);
               if (var23 != null) {
                  var18.put("error", var23);
               }
            }
         }

         var18.put("status", var20);
         switch (var20) {
            case "passed":
               var13++;
               break;
            case "failed":
               var14++;
               break;
            case "running":
               var15++;
         }

         var29.add(var18);
      }

      int var30 = var29.size();
      var var31 = this.header(var1, var2);
      var31.put("status", var15 > 0 ? "running" : (var14 > 0 ? "failed" : (var30 > 0 && var13 == var30 ? "passed" : "notrun")));
      LinkedHashMap var32 = new LinkedHashMap();
      var32.put("passed", var13);
      var32.put("failed", var14);
      var32.put("notrun", var30 - var13 - var14 - var15);
      if (var15 > 0) {
         var32.put("running", var15);
      }

      var32.put("total", var30);
      var31.put("tests", var29);
      if (!var6) {
         return var31;
      } else {
         List<Map<String, Object>> var33 = criteria(var29, var8, var3, var4, var5);
         var31.put("criteria", var33);
         LinkedHashMap var34 = new LinkedHashMap();

         for (String var39 : List.of("passed", "failed", "notrun")) {
            var34.put(var39, var33.stream().filter(var1x -> var39.equals(var1x.get("status"))).count());
         }

         var34.put("total", var33.size());
         LinkedHashMap var36 = new LinkedHashMap();
         var36.put("tests", var32);
         var36.put("criteria", var34);
         var31.put("totals", var36);
         ArrayList var40 = new ArrayList();

         for (Map var24 : var2) {
            var40.add(launchRow(var24, var7, var26));
         }

         var31.put("runs", var40);
         return var31;
      }
   }

   private static String verdict(List<Map<String, Object>> var0, Map<String, Map<String, Object>> var1, Map<String, Object> var2) {
      if (var0.isEmpty()) {
         return "notrun";
      } else {
         boolean var3 = false;
         boolean var4 = true;
         Map var5 = null;

         for (Map var7 : var0) {
            String var8 = String.valueOf(var7.get("status"));
            var3 |= "FAILED".equals(var8);
            var4 &= "PASSED".equals(var8);
            Map var9 = (Map)var1.get(String.valueOf(var7.get("execution")));
            if (var9 != null && (var5 == null || at(var9) > at(var5))) {
               var5 = var9;
            }
         }

         if (var5 != null && var5.get("runId") != null) {
            var2.put("runId", var5.get("runId"));
         }

         return var3 ? "failed" : (var4 ? "passed" : "notrun");
      }
   }

   private static long at(Map<String, Object> var0) {
      return (var0.get("endedAt") != null ? var0.get("endedAt") : var0.get("startedAt")) instanceof Number var2 ? var2.longValue() : 0L;
   }

   // $VF: One or more variable merging failures!
   // $VF: Could not properly define all variable types!
   // Please report this to the Vineflower issue tracker, at https://github.com/Vineflower/vineflower/issues with a copy of the class file (if you have the rights to distribute it!)
   private static List<Map<String, Object>> criteria(
      List<Map<String, Object>> var0,
      Map<String, List<Map<String, Object>>> var1,
      Map<String, Object> var2,
      Map<String, Map<String, Object>> var3,
      Map<String, Map<String, Object>> var4
   ) {
      HashMap var5 = new HashMap();
      var1.forEach((var1x, var2x) -> var2x.forEach(var2xx -> var5.put(String.valueOf(var2xx.get("id")), var1x)));
      LinkedHashMap<String, Map<String, Object>> var6 = new LinkedHashMap<>();
      if (var2 != null) {
         for (Map var8 : io.karatelabs.ext.common.jsapi.CoverageGraphAccessor.a(var2, "items")) {
            if ("req".equals(var8.get("source")) && var8.get("id") instanceof String var9 && var9.startsWith("req:")) {
               String var22 = var9.substring("req:".length());
               String var11 = reqIdOf(var22);
               Map var12 = (Map)var4.get(var11);
               if (!bareOnCriteria(var22, var12)) {
                  ArrayList var13 = new ArrayList();

                  for (Object var29 : var8.get("tests") instanceof List ? (List)var8.get("tests") : List.of()) {
                     String var32 = (String)var5.get(String.valueOf(var29));
                     if (var32 != null && !var13.contains(var32)) {
                        var13.add(var32);
                     }
                  }

                  if (!var13.isEmpty()) {
                     Map var27 = criterionRow(var22, var11, var12, var4);
                     String var30 = String.valueOf(var8.get("status"));

                     var27.put("status", switch (var30) {
                        case "COVERED" -> "passed";
                        case "FAILING" -> "failed";
                        default -> "notrun";
                     });
                     var27.put("tests", var13);
                     var6.put(var22, var27);
                  }
               }
            }
         }
      }

      for (Map var19 : var0) {
         String var21 = String.valueOf(var19.get("path"));
         if (!var1.containsKey(var21) && var3.get(var21) != null) {
            for (String var24 : strings(((Map)var3.get(var21)).get("req"))) {
               String var25 = reqIdOf(var24);
               Map var26 = (Map)var4.get(var25);
               if (!bareOnCriteria(var24, var26)) {
                  Map var28 = var6.computeIfAbsent(var24, var3x -> {
                     Map var4x = criterionRow(var3x, var25, var26, var4);
                     var4x.put("status", "notrun");
                     var4x.put("tests", new ArrayList());
                     return var4x;
                  });
                  List var31 = (List)var28.get("tests");
                  if (!var31.contains(var21)) {
                     var31.add(var21);
                  }
               }
            }
         }
      }

      var var18 = new ArrayList<>(var4.keySet());
      ArrayList var20 = new ArrayList(var6.values());
      var20.sort(Comparator.<Map>comparingInt(var1x -> {
         int var2x = var18.indexOf(String.valueOf(var1x.get("req")));
         return var2x < 0 ? Integer.MAX_VALUE : var2x;
      }).thenComparingInt(var1x -> position(var4, (Map<String, Object>)var1x)));
      return var20;
   }

   private static String reqIdOf(String var0) {
      int var1 = var0.indexOf(47);
      return var1 > 0 ? var0.substring(0, var1) : var0;
   }

   private static boolean bareOnCriteria(String var0, Map<String, Object> var1) {
      return var0.indexOf(47) < 0 && var1 != null && !maps(var1.get("criteria")).isEmpty();
   }

   private static Map<String, Object> criterionRow(String var0, String var1, Map<String, Object> var2, Map<String, Map<String, Object>> var3) {
      LinkedHashMap var4 = new LinkedHashMap();
      var4.put("id", var0);
      var4.put("req", var1);
      if (var2 == null) {
         var4.put("notfound", true);
         return var4;
      } else {
         var4.put("title", var2.get("title"));
         if (var2.get("criticality") != null) {
            var4.put("criticality", var2.get("criticality"));
         }

         for (Map var6 : maps(var2.get("criteria"))) {
            if (var0.equals(var1 + "/" + var6.get("id"))) {
               var4.put("text", String.valueOf(var6.get("text")));
            }
         }

         return var4;
      }
   }

   private static Map<String, Object> launchRow(Map<String, Object> var0, List<String> var1, Map<String, Map<String, io.karatelabs.ext.max.jsapi.TestHistoryIndex.TestRunAggregate>> var2) {
      LinkedHashMap var3 = new LinkedHashMap();
      String var4 = String.valueOf(var0.get("runId"));
      var3.put("runId", var0.get("runId"));
      var3.put("startedMs", var0.get("startedMs"));
      if (var0.get("finishedMs") != null) {
         var3.put("finishedMs", var0.get("finishedMs"));
      }

      var3.put("status", var0.get("status"));
      int var5 = maps(var0.get("members")).size();
      var3.put("tests", var5 > 0 ? var5 : strings(var0.get("selector")).size());
      int var6 = 0;
      int var7 = 0;

      for (String var9 : var1) {
         io.karatelabs.ext.max.jsapi.TestHistoryIndex.TestRunAggregate var10 = (io.karatelabs.ext.max.jsapi.TestHistoryIndex.TestRunAggregate)var2.getOrDefault(var9, Map.of()).get(var4);
         if (var10 != null) {
            if (var10.b) {
               var7++;
            } else {
               var6++;
            }
         }
      }

      var3.put("passed", var6);
      var3.put("failed", var7);
      if (var0.get("error") != null) {
         var3.put("error", var0.get("error"));
      }

      return var3;
   }

   private static boolean isRunning(Map<String, Object> var0) {
      return "running".equals(var0.get("status")) || "started".equals(var0.get("status"));
   }

   private static String memberError(List<Map<String, Object>> var0, String var1, String var2) {
      String var3 = io.karatelabs.ext.max.jsapi.RunEvidenceInspector.a(var2);

      for (Map var5 : var0) {
         if (var1.equals(var5.get("runId"))) {
            for (Map var7 : maps(var5.get("memberVerdicts"))) {
               if (var3.equals(io.karatelabs.ext.max.jsapi.RunEvidenceInspector.a(String.valueOf(var7.get("path")))) && var7.get("error") != null) {
                  return String.valueOf(var7.get("error"));
               }
            }
         }
      }

      return null;
   }

   private List<Map<String, Object>> runs() {
      return this.harness != null ? maps(this.harness.runs().get("runs")) : this.store.b();
   }

   private Map<String, Map<String, Object>> requirementIndex() {
      LinkedHashMap var1 = new LinkedHashMap();
      String var3 = io.karatelabs.ext.common.jsapi.EvaluationMessageScope.a();

      Object var2;
      label46: {
         LinkedHashMap var5;
         try {
            var2 = this.requirements.get();
            break label46;
         } catch (RuntimeException var9) {
            var5 = var1;
         } finally {
            io.karatelabs.ext.common.jsapi.EvaluationMessageScope.b();
            io.karatelabs.ext.common.jsapi.EvaluationMessageScope.a(var3);
         }

         return var5;
      }

      for (Map var11 : maps(var2)) {
         var1.putIfAbsent(String.valueOf(var11.get("id")), var11);
      }

      return var1;
   }

   private static int position(Map<String, Map<String, Object>> var0, Map<String, Object> var1) {
      Map var2 = (Map)var0.get(String.valueOf(var1.get("req")));
      String var3 = String.valueOf(var1.get("id"));
      List var4 = var2 == null ? List.of() : maps(var2.get("criteria"));

      for (int var5 = 0; var5 < var4.size(); var5++) {
         if (var3.equals(var1.get("req") + "/" + ((Map)var4.get(var5)).get("id"))) {
            return var5;
         }
      }

      return -1;
   }

   private static Map<String, Map<String, Object>> byPath(List<Map<String, Object>> var0) {
      LinkedHashMap var1 = new LinkedHashMap();

      for (Map var3 : var0) {
         var1.put(String.valueOf(var3.get("path")), var3);
      }

      return var1;
   }

   private static String normalize(String var0) {
      String var1 = var0.trim().replace('\\', '/');

      while (var1.startsWith("/")) {
         var1 = var1.substring(1);
      }

      return var1.endsWith(".js") && !var1.endsWith(".karate.js") ? Check.canonicalName(var1) : var1;
   }

   private static String slug(String var0) {
      String var1 = Normalizer.normalize(var0.toLowerCase(Locale.ROOT), Form.NFD)
         .replaceAll("\\p{M}+", "")
         .replace("œ", "oe")
         .replace("æ", "ae")
         .replace("ß", "ss")
         .replaceAll("[^a-z0-9]+", "-")
         .replaceAll("^-+|-+$", "");
      return var1.length() > 80 ? var1.substring(0, 80).replaceAll("-+$", "") : var1;
   }

   private static String nameKey(String var0) {
      return var0 == null ? "" : Normalizer.normalize(var0.trim(), Form.NFC).toLowerCase(Locale.ROOT);
   }

   private Path dir() {
      return this.root.resolve("campaigns");
   }

   private Path fileOf(String var1) {
      return this.dir().resolve(var1 + ".json");
   }

   private Path freeFile(String var1) {
      String var2 = var1.isEmpty() ? "campaign" : var1;
      Path var3 = this.fileOf(var2);

      for (int var4 = 2; Files.exists(var3); var4++) {
         var3 = this.fileOf(var2 + "-" + var4);
      }

      return var3;
   }

   private String rel(Path var1) {
      return this.root.relativize(var1).toString().replace('\\', '/');
   }

   private List<Path> files() {
      if (!Files.isDirectory(this.dir())) {
         return List.of();
      } else {
         try {
            List var2;
            try (var var1 = Files.list(this.dir())) {
               var2 = var1.filter(var0 -> var0.getFileName().toString().endsWith(".json")).sorted().toList();
            }

            return var2;
         } catch (Exception var6) {
            logger.warn("could not list {}: {}", this.dir(), var6.getMessage());
            return List.of();
         }
      }
   }

   private List<io.karatelabs.ext.max.jsapi.Campaign.CampaignDefinition> defs(List<String> var1) {
      ArrayList var2 = new ArrayList();
      HashMap var3 = new HashMap();

      for (Path var5 : this.files()) {
         io.karatelabs.ext.max.jsapi.Campaign.CampaignDefinition var6 = this.read(var5);
         if (var6 != null) {
            var2.add(var6);
            var3.put(var5, modifiedMs(var5));
         }
      }

      ArrayList var13 = new ArrayList(var2);
      var13.sort(
         Comparator.<Campaign.CampaignDefinition>comparingLong(var0 -> var0.d() instanceof Number var1x ? var1x.longValue() : Long.MAX_VALUE)
            .thenComparingLong(var1x -> (Long)var3.get(var1x.a()))
            .thenComparing(io.karatelabs.ext.max.jsapi.Campaign.CampaignDefinition::a)
      );
      HashMap var14 = new HashMap();
      HashMap var15 = new HashMap();
      HashSet var7 = new HashSet();

      for (io.karatelabs.ext.max.jsapi.Campaign.CampaignDefinition var9 : (Iterable<io.karatelabs.ext.max.jsapi.Campaign.CampaignDefinition>) (Iterable<?>) (var13)) {
         io.karatelabs.ext.max.jsapi.Campaign.CampaignDefinition var10 = (io.karatelabs.ext.max.jsapi.Campaign.CampaignDefinition)var14.get(var9.b());
         String var11 = "id";
         if (var10 == null) {
            var10 = (io.karatelabs.ext.max.jsapi.Campaign.CampaignDefinition)var15.get(nameKey(var9.c()));
            var11 = "name";
         }

         if (var10 == null) {
            var14.put(var9.b(), var9);
            var15.put(nameKey(var9.c()), var9);
         } else {
            var7.add(var9.a());
            String var12 = this.rel(var9.a()) + " is left out: it repeats the " + var11 + " of " + this.rel(var10.a());
            logger.debug("campaign {}", var12);
            if (var1 != null) {
               var1.add(var12 + " — give it its own");
            }
         }
      }

      var2.removeIf(var1x -> var7.contains(((io.karatelabs.ext.max.jsapi.Campaign.CampaignDefinition)var1x).a()));
      return var2;
   }

   private static long modifiedMs(Path var0) {
      try {
         return Files.getLastModifiedTime(var0).toMillis();
      } catch (Exception var2) {
         return Long.MAX_VALUE;
      }
   }

   private io.karatelabs.ext.max.jsapi.Campaign.CampaignDefinition find(String var1) {
      String var2 = nameKey(var1);
      if (var2.isEmpty()) {
         return null;
      } else {
         for (io.karatelabs.ext.max.jsapi.Campaign.CampaignDefinition var4 : this.defs(null)) {
            if (nameKey(var4.c()).equals(var2)) {
               return var4;
            }
         }

         return null;
      }
   }

   private io.karatelabs.ext.max.jsapi.Campaign.CampaignDefinition read(Path var1) {
      String var2;
      try {
         var2 = Files.size(var1) > 1048576L ? "" : Files.readString(var1);
      } catch (Exception var9) {
         logger.debug("{} is not a campaign: {}", var1, var9.getMessage());
         return null;
      }

      if (!var2.contains("karate-campaign/v1")) {
         return null;
      } else {
         try {
            if (Json.of(var2).value() instanceof Map var3 && "karate-campaign/v1".equals(var3.get("schema"))) {
               String var11 = var1.getFileName().toString();
               var11 = var11.substring(0, var11.length() - ".json".length());
               String var5 = var3.get("name") instanceof String var6 && !var6.isBlank() ? var6.trim() : var11;
               String var13 = var3.get("id") instanceof String var14 && !var14.isBlank() ? var14.trim() : var11;
               return new io.karatelabs.ext.max.jsapi.Campaign.CampaignDefinition(var1, var13, var5, var3.get("createdMs"), strings(var3.get("tests")).stream().map(Campaign::normalize).distinct().toList());
            } else {
               return null;
            }
         } catch (Exception var10) {
            logger.warn("could not read campaign {}: {}", var1, var10.getMessage());
            return null;
         }
      }
   }

   private Map<String, Object> notFound(String var1) {
      ArrayList var2 = new ArrayList();

      for (io.karatelabs.ext.max.jsapi.Campaign.CampaignDefinition var4 : this.defs(null)) {
         var2.add(var4.c());
      }

      String var5 = var1 != null && !var1.isBlank()
         ? "no campaign named '" + var1 + "'"
         : "Campaign.get(name) / run(name) / delete(name) needs the campaign's name";
      return JsApi.suggest(JsApi.next(JsApi.notFound(var5), "Campaign.list()"), var1 == null ? List.of() : io.karatelabs.ext.common.jsapi.SuggestionEngine.a(var1, var2));
   }

   private static List<String> strings(Object var0) {
      if (var0 instanceof String var6) {
         return var6.isBlank() ? List.of() : List.of(var6.trim());
      } else {
         ArrayList var1 = new ArrayList();
         if (var0 instanceof List) {
            for (Object var4 : (List)var0) {
               if (var4 instanceof String var5 && !var5.isBlank()) {
                  var1.add(var5.trim());
               }
            }
         }

         return var1;
      }
   }

   private static List<Map<String, Object>> maps(Object var0) {
      ArrayList var1 = new ArrayList();
      if (var0 instanceof List) {
         for (Object var4 : (List)var0) {
            if (var4 instanceof Map var5) {
               var1.add(var5);
            }
         }
      }

      return var1;
   }

   private static Map<String, Object> help() {
      return JsApi.markdown(
         "# Campaign — a named set of tests, launched together as often as it takes\n\nA test's link to a requirement stays the test's own; a campaign only groups tests.\nIts state is the evidence graph over its launches: each test's latest verdict there.\n\n- `Campaign.create({name, tests?, req?, overwrite?})` — save the set in `campaigns/`\n  (`file` is its path). `tests` are paths from `Runner.tests()`; `req` (an id or a list)\n  adds every test that declares it. An existing name needs `overwrite:true`.\n- `Campaign.run(name, [{only?, async?, video?, env?}])` — launch it as one set run\n  (`async`, `video`, `env` as in `Runner.run`; `async:true` for a browser suite). `only:'failed'`\n  relaunches the tests that failed their last launch. Returns `Runner.run`'s result plus `campaign`.\n- `Campaign.get(name)` → `{name, status, tests[{path, status, runId?, error?}],\n  criteria[{id, req, title, text, status, tests}], totals, runs[]}`. A status is `passed` ·\n  `failed` · `notrun` · `running`; a skipped or cut test is `notrun`. A criterion's status is\n  the graph's over the launches; a launch keeps the links it ran under.\n- `Campaign.list()` — every campaign with its test count and `lastStatus`, its last launch's\n  outcome; `get` grades it.\n- `Campaign.delete(name)` — remove the definition; its launches stay in `runs/`."
      );
   }

   private record CampaignDefinition(Path a, String b, String c, Object d, List<String> e) {
   }
}
