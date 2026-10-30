package io.karatelabs.ext.max.run;

import io.karatelabs.ext.requirements.RequirementsRepository;
import io.karatelabs.ext.rules.RuleApiException;
import io.karatelabs.ext.rules.RulebookRepository;
import io.karatelabs.ext.rules.twin.TwinModelLoader;
import io.karatelabs.ext.rules.twin.TwinModel;
import io.karatelabs.ext.rules.twin.Twin;
import io.karatelabs.ext.rules.twin.PlayRecordStore;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Stream;

final class PlayRecordInspector {
   private final Twin a;
   private final Path b;
   private final Path c;
   private final Supplier<List<Path>> d;

   PlayRecordInspector(Twin var1, Path var2, Path var3, Supplier<List<Path>> var4) {
      this.a = var1;
      this.b = var2;
      this.c = var3;
      this.d = var4;
   }

   Map<String, Object> a(String var1) {
      TwinModel var2 = this.b(var1);
      ArrayList var3 = new ArrayList();

      for (String var5 : this.b()) {
         io.karatelabs.ext.rules.twin.PlayRecordStore.PlayRecord var6;
         try {
            var6 = io.karatelabs.ext.rules.twin.PlayRecordStore.a(this.c, var5);
         } catch (RuleApiException var8) {
            if ("PLAY_TAMPERED".equals(var8.b)) {
               var3.add(Map.of("id", var5, "error", Map.<String, String>of("code", var8.b, "message", var8.getMessage())));
            }
            continue;
         }

         if (var1.equals(var6.a())) {
            var3.add(this.a(var1, var2, var6));
         }
      }

      LinkedHashMap var9 = new LinkedHashMap();
      var9.put("rule", var1);
      var9.put("twin", var2 != null);
      var9.put("records", var3);
      return var9;
   }

   Map<String, Object> a(String var1, String var2, Object var3, Object var4) {
      io.karatelabs.ext.rules.twin.PlayRecordStore.PlayRecord var5 = io.karatelabs.ext.rules.twin.PlayRecordStore.a(this.c, var2);
      if (!var5.a().equals(var1)) {
         throw new RuleApiException(404, "PLAY_NOTFOUND", "play record " + var2 + " was played against rulebook '" + var5.a() + "', not '" + var1 + "'")
            .a(Map.of("record", var2));
      } else if (!(var3 instanceof Map var6)) {
         throw new RuleApiException(400, "SEQUENCE_INVALID", "_expect: must be an object");
      } else {
         ArrayList var7 = new ArrayList();
         if (var4 != null) {
            if (!(var4 instanceof List var8)) {
               throw new RuleApiException(400, "SEQUENCE_INVALID", "_req: must be an array of criterion ids");
            }

            var var9 = this.a(this.b(var1), var5).stream().map(var0 -> (String)var0.get("id")).toList();

            for (Object var11 : var8) {
               if (!(var11 instanceof String var12 && var9.contains(var12))) {
                  throw new RuleApiException(400, "SEQUENCE_INVALID", "_req: '" + var11 + "' is not a candidate for " + var2 + " — pick from " + var9 + ", or none");
               }

               var7.add(var12);
            }
         }

         return io.karatelabs.ext.rules.twin.PlayRecordStore.a(this.c, var2, var6, var7, System.currentTimeMillis());
      }
   }

   private Map<String, Object> a(String var1, TwinModel var2, io.karatelabs.ext.rules.twin.PlayRecordStore.PlayRecord var3) {
      ArrayList var4 = new ArrayList();
      ArrayList var5 = new ArrayList();
      int var6 = 0;

      for (Map var8 : var3.d()) {
         LinkedHashMap var9 = new LinkedHashMap();
         var9.put("at", var6++);
         var9.put("command", var8.get("command"));
         var9.put("args", var8.get("args"));
         var9.put("disposition", var8.get("disposition"));

         for (String var11 : List.of("stage", "reason")) {
            if (var8.get(var11) != null) {
               var9.put(var11, var8.get(var11));
            }
         }

         Object var13 = ((Map)var8.get("exchange")).get("response");
         var9.put("status", ((Map)var13).get("status"));
         var4.add(var9);
         var5.add(Map.of("command", var8.get("command"), "args", var8.get("args")));
      }

      LinkedHashMap var12 = new LinkedHashMap();
      var12.put("id", var3.e());
      var12.put("identity", var3.b());
      var12.put("identityField", var2 != null && var2.k() != null ? var2.k().c() : null);
      var12.put("steps", var4);
      var12.put("replay", var2 == null ? null : this.a.run(var1, var5, Map.of("trace", true)));
      var12.put("confirmation", a(var3));
      var12.put("candidates", this.a(var2, var3));
      return var12;
   }

   private static Map<String, Object> a(io.karatelabs.ext.rules.twin.PlayRecordStore.PlayRecord var0) {
      if (!Files.isRegularFile(var0.f().resolve("confirm.json"))) {
         return null;
      } else {
         LinkedHashMap var1 = new LinkedHashMap();

         try {
            io.karatelabs.ext.rules.twin.PlayRecordStore.PlayConfirmation var2 = io.karatelabs.ext.rules.twin.PlayRecordStore.a(var0);
            var1.put("state", "current");
            var1.put("expect", var2.a());
            var1.put("req", var2.b());
         } catch (RuleApiException var3) {
            var1.put("state", "invalid");
            var1.put("message", var3.getMessage());
         }

         return var1;
      }
   }

   private List<Map<String, Object>> a(TwinModel var1, io.karatelabs.ext.rules.twin.PlayRecordStore.PlayRecord var2) {
      ArrayList var3 = new ArrayList();
      if (var1 == null) {
         return var3;
      } else {
         var var4 = var2.d().stream().map(var0 -> String.valueOf(var0.get("command"))).distinct().toList();
         var var5 = this.a();

         for (io.karatelabs.ext.rules.twin.TwinModel.CommandModel var7 : var1.c()) {
            if (var4.contains(var7.a()) && var7.j() != null) {
               a(var3, var7.j(), a(var5, var7.j()), "command", var7.a());
            }
         }

         for (io.karatelabs.ext.rules.twin.TwinModel.CommandModel var14 : var1.c()) {
            if (var4.contains(var14.a()) && var14.q() != null) {
               String var8 = "openapi:" + var14.q().a();

               for (io.karatelabs.ext.requirements.model.RequirementNode var10 : (Iterable<io.karatelabs.ext.requirements.model.RequirementNode>) (Iterable<?>) (var5)) {
                  if (var10.k() != null && var10.k().contains(var8)) {
                     for (io.karatelabs.ext.requirements.AcceptanceCriterion var12 : (Iterable<io.karatelabs.ext.requirements.AcceptanceCriterion>) (Iterable<?>) (io.karatelabs.ext.requirements.AcceptanceCriterion.a(var10.m()))) {
                        a(var3, var10.b() + "/" + var12.a(), var12.b(), "real", var8);
                     }
                  }
               }
            }
         }

         return var3;
      }
   }

   private static void a(List<Map<String, Object>> var0, String var1, String var2, String var3, String var4) {
      for (Map var6 : var0) {
         if (var1.equals(var6.get("id"))) {
            return;
         }
      }

      LinkedHashMap var7 = new LinkedHashMap();
      var7.put("id", var1);
      if (var2 != null) {
         var7.put("text", var2);
      }

      var7.put("via", var3);
      var7.put("by", var4);
      var0.add(var7);
   }

   private static String a(List<io.karatelabs.ext.requirements.model.RequirementNode> var0, String var1) {
      int var2 = var1.indexOf(47);

      for (io.karatelabs.ext.requirements.model.RequirementNode var4 : var0) {
         if (var2 > 0 && var4.b().equals(var1.substring(0, var2))) {
            for (io.karatelabs.ext.requirements.AcceptanceCriterion var6 : (Iterable<io.karatelabs.ext.requirements.AcceptanceCriterion>) (Iterable<?>) (io.karatelabs.ext.requirements.AcceptanceCriterion.a(var4.m()))) {
               if (var6.a().equals(var1.substring(var2 + 1))) {
                  return var6.b();
               }
            }
         }
      }

      return null;
   }

   private List<io.karatelabs.ext.requirements.model.RequirementNode> a() {
      var var1 = this.d.get();
      if (var1.stream().noneMatch(var0 -> Files.exists(var0))) {
         return List.of();
      } else {
         try {
            return new RequirementsRepository(var1, this.c).a().d();
         } catch (RuntimeException var3) {
            return List.of();
         }
      }
   }

   private TwinModel b(String var1) {
      String var2 = new RulebookRepository(this.b).b(var1).k();
      return var2 == null ? null : TwinModelLoader.a(var2);
   }

   private List<String> b() {
      Path var1 = io.karatelabs.ext.rules.twin.PlayRecordStore.a(this.c);
      if (!Files.isDirectory(var1)) {
         return List.of();
      } else {
         try {
            List var3;
            try (var var2 = Files.list(var1)) {
               var3 = var2.filter(var0 -> Files.isDirectory(var0)).map(var0 -> var0.getFileName().toString()).sorted().toList();
            }

            return var3;
         } catch (IOException var7) {
            throw new UncheckedIOException(var7);
         }
      }
   }
}
