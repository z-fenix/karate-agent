package io.karatelabs.ext.rules.twin;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

public final class LiveConformanceRunner {
   public static final String a = "PLAY_STALE";
   public static final String b = "PLAY_NONCONFORMANT";
   public static final String c = "PLAY_UNREPRODUCED";
   private static final Set<String> d = Set.of("WIRE_UNDECLARED", "RESPONSE_INVALID", "STEP_ERROR");
   private static final List<String> e = List.of("disposition", "stage", "reason", "worldHashAfter");

   private LiveConformanceRunner() {
   }

   public static Map<String, Object> a(io.karatelabs.ext.rules.RulebookRepository var0, Path var1, String var2, String var3, Map<String, Object> var4) {
      io.karatelabs.ext.rules.twin.PlayRecordStore.PlayRecord var5 = io.karatelabs.ext.rules.twin.PlayRecordStore.a(var1, var3);
      io.karatelabs.ext.rules.twin.PlayRecordStore.PlayConfirmation var6 = io.karatelabs.ext.rules.twin.PlayRecordStore.a(var5);
      io.karatelabs.ext.rules.RulebookRepository.RulebookDocument var7 = var0.b(var2);
      TwinModel var8 = TwinModelLoader.a(var7.k());
      a(var0, var7, var5);
      io.karatelabs.ext.rules.twin.OpenApiProjector.OpenApiProjection var9 = io.karatelabs.ext.rules.twin.OpenApiProjector.a(var8, var2);
      a(var5, "projection", var5.c().get("projection"), var9.a() ? var9.c() : null);
      a(var8, var5);
      io.karatelabs.ext.rules.TwinRuleRuntime var10 = new io.karatelabs.ext.rules.TwinRuleRuntime(var0, var2);

      try (io.karatelabs.ext.common.SandboxExecutor.SandboxExecutionScope var11 = new io.karatelabs.ext.common.SandboxExecutor.SandboxExecutionScope(
            "twin-play", io.karatelabs.ext.common.SandboxExecutor.a("KARATE_RULES_TWIN_LIVE_TIMEOUT_MS", 300000L)
         )) {
         var11.a(() -> {
            a(var7, var5, var10);
            a(var8, var7, var9, var5, var10);
            return null;
         }, Twin.MAPPER);
      } catch (Throwable var17) {
         try {
            var10.close();
         } catch (Throwable var14) {
            var17.addSuppressed(var14);
         }

         throw var17;
      }

      var10.close();
      var4.put("steps", a(var5));
      var4.putIfAbsent("_label", "play: " + var5.e());
      var4.put("_expect", io.karatelabs.ext.rules.twin.CanonicalJson.d(var6.a()));
      if (!var6.b().isEmpty()) {
         var4.put("_req", List.copyOf(var6.b()));
      }

      LinkedHashMap var18 = new LinkedHashMap();
      var18.put("kind", "play");
      var18.put("record", var5.e());
      var18.put("digest", var5.h());
      var18.put("manifest", io.karatelabs.ext.rules.twin.CanonicalJson.d(var5.c()));
      var18.put("confirmation", var6.c());
      return var18;
   }

   private static void a(io.karatelabs.ext.rules.RulebookRepository var0, io.karatelabs.ext.rules.RulebookRepository.RulebookDocument var1, io.karatelabs.ext.rules.twin.PlayRecordStore.PlayRecord var2) {
      var var3 = var2.c();
      a(var2, "twin", var3.get("twin"), var1.i().get("twin"));
      a(var2, "schema", var3.get("schema"), var1.i().get("schema"));
      if (!(var3.get("rulebooks") instanceof Map var4)) {
         throw b(var2, "rulebooks", var3.get("rulebooks"), null);
      } else {
         for (Entry var6 : (Iterable<Entry>) (Iterable<?>) (var4.entrySet())) {
            String var7 = String.valueOf(var6.getKey());

            String var8;
            try {
               var8 = var0.d(var7).d().c();
            } catch (io.karatelabs.ext.rules.RuleApiException var10) {
               var8 = null;
            }

            a(var2, "rulebooks." + var7, var6.getValue(), var8);
         }
      }
   }

   private static void a(io.karatelabs.ext.rules.twin.PlayRecordStore.PlayRecord var0, String var1, Object var2, String var3) {
      if (!(var2 instanceof String var4 && var4.equals(var3))) {
         throw b(var0, var1, var2, var3);
      }
   }

   private static io.karatelabs.ext.rules.RuleApiException b(io.karatelabs.ext.rules.twin.PlayRecordStore.PlayRecord var0, String var1, Object var2, String var3) {
      LinkedHashMap var4 = new LinkedHashMap();
      var4.put("record", var0.e());
      var4.put("source", var1);
      var4.put("recorded", var2);
      var4.put("current", var3);
      return new io.karatelabs.ext.rules.RuleApiException(
            409, "PLAY_STALE", "play record " + var0.e() + " was played against another " + var1 + " — play it again under the current model"
         )
         .a(var4);
   }

   private static void a(TwinModel var0, io.karatelabs.ext.rules.twin.PlayRecordStore.PlayRecord var1) {
      String var2 = var0.k().f();
      var var3 = var1.d();

      for (int var4 = 0; var4 < var3.size(); var4++) {
         Map var5 = (Map)var3.get(var4);
         String var6 = String.valueOf(var5.get("disposition"));
         if (!"applied".equals(var6) && !"refused".equals(var6)) {
            throw a(var1, var4, var5, "the step is recorded " + var6);
         }

         if ((a(a(var5.get("exchange")).get("response")).get("body") instanceof Map var9 ? var9.get(var2) : null) instanceof String var10 && d.contains(var10)) {
            throw a(var1, var4, var5, "the response is the engine's " + var10 + ", not behaviour");
         }
      }
   }

   private static void a(io.karatelabs.ext.rules.RulebookRepository.RulebookDocument var0, io.karatelabs.ext.rules.twin.PlayRecordStore.PlayRecord var1, io.karatelabs.ext.rules.TwinRuleRuntime var2) {
      var var3 = var1.d();
      ArrayList var4 = new ArrayList(var3.size());

      for (int var5 = 0; var5 < var3.size(); var5++) {
         Map var6 = (Map)var3.get(var5);
         var var7 = a(var6.get("exchange"));
         LinkedHashMap var8 = new LinkedHashMap();
         var8.put("at", var5);
         var8.put("kind", "command");
         var8.put("command", var6.get("command"));
         var8.put("args", var6.get("args"));
         var8.put("request", var7.get("request"));
         LinkedHashMap var9 = new LinkedHashMap();
         var9.put("request", var7.get("request"));
         var9.put("response", var7.get("response"));
         var8.put("observed", var9);
         var4.add(var8);
      }

      Map var10 = Twin.graded(var0, a(var1), var4, 8, var2);
      var var11 = a(var10.get("disposition"));
      if (!"PASS".equals(var11.get("kind"))) {
         LinkedHashMap var12 = new LinkedHashMap();
         var12.put("record", var1.e());
         var12.put("disposition", var11);
         throw new io.karatelabs.ext.rules.RuleApiException(
               409,
               "PLAY_NONCONFORMANT",
               "play record " + var1.e() + ": Twin.allows reads " + var11.get("kind") + " over its seam log — " + var11.get("reason")
            )
            .a(var12);
      }
   }

   private static void a(TwinModel var0, io.karatelabs.ext.rules.RulebookRepository.RulebookDocument var1, io.karatelabs.ext.rules.twin.OpenApiProjector.OpenApiProjection var2, io.karatelabs.ext.rules.twin.PlayRecordStore.PlayRecord var3, io.karatelabs.ext.rules.TwinRuleRuntime var4) {
      TwinHttpRouter var5 = TwinHttpRouter.a(var0, var1.d(), var2, var1.i(), Map.of(var1.d(), var1.n().c()), var4, var3.b());
      var var6 = var3.d();

      for (int var7 = 0; var7 < var6.size(); var7++) {
         Map var8 = (Map)var6.get(var7);
         var var9 = var5.a(a(a(var8.get("exchange")).get("request")));

         for (String var11 : e) {
            Object var12 = var8.get(var11);
            Object var13 = var9 == null ? null : var9.get(var11);
            if (!io.karatelabs.ext.rules.twin.CanonicalJson.c(var12).equals(io.karatelabs.ext.rules.twin.CanonicalJson.c(var13))) {
               LinkedHashMap var14 = new LinkedHashMap();
               var14.put("record", var3.e());
               var14.put("at", var7);
               var14.put("command", var8.get("command"));
               var14.put("field", var11);
               var14.put("recorded", var12);
               var14.put("replayed", var13);
               throw new io.karatelabs.ext.rules.RuleApiException(
                     409,
                     "PLAY_UNREPRODUCED",
                     "play record "
                        + var3.e()
                        + ": a fresh replay of step "
                        + var7
                        + " ("
                        + var8.get("command")
                        + ") differs on "
                        + var11
                        + (var9 == null ? " — no step ran" : "")
                  )
                  .a(var14);
            }
         }
      }
   }

   private static List<Map<String, Object>> a(io.karatelabs.ext.rules.twin.PlayRecordStore.PlayRecord var0) {
      ArrayList var1 = new ArrayList(var0.d().size());

      for (Map var3 : var0.d()) {
         LinkedHashMap var4 = new LinkedHashMap();
         var4.put("command", var3.get("command"));
         var4.put("args", io.karatelabs.ext.rules.twin.CanonicalJson.d(var3.get("args")));
         var1.add(var4);
      }

      return var1;
   }

   private static io.karatelabs.ext.rules.RuleApiException a(io.karatelabs.ext.rules.twin.PlayRecordStore.PlayRecord var0, int var1, Map<String, Object> var2, String var3) {
      LinkedHashMap var4 = new LinkedHashMap();
      var4.put("record", var0.e());
      var4.put("at", var1);
      var4.put("command", var2.get("command"));
      return new io.karatelabs.ext.rules.RuleApiException(
            409, "PLAY_NONCONFORMANT", "play record " + var0.e() + ": step " + var1 + " (" + var2.get("command") + ") — " + var3
         )
         .a(var4);
   }

   private static Map<String, Object> a(Object var0) {
      return (Map<String, Object>)var0;
   }
}
