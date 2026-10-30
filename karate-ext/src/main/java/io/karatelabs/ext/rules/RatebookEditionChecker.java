package io.karatelabs.ext.rules;

import io.karatelabs.common.Json;
import io.karatelabs.ext.common.ExtMeta;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Stream;

public final class RatebookEditionChecker {
   static final String a = "evidence/changes";
   static final String b = "ratebook";
   static final String c = "karate-edition-check";
   public static final String d = "ok";
   public static final String e = "competing";
   public static final String f = "ambiguous";
   public static final String g = "not-filed";
   public static final String h = "orphan-companion";
   public static final String i = "none-selectable";
   public static final List<String> j = List.of("none-selectable", "competing", "ambiguous", "orphan-companion", "not-filed", "ok");

   private RatebookEditionChecker() {
   }

   static List<io.karatelabs.ext.rules.RatebookEditionChecker.RatebookRecord> a(Path var0, Object var1, RatebookSelectionRule var2) {
      Map<?, ?> var3;
      if (var1 instanceof Map var5) {
         var3 = var5;
      } else {
         if (!(var1 instanceof String var4)) {
            throw a("records is a name under evidence/ratebooks/ or the records object");
         }

         if (!var4.matches("[A-Za-z0-9_-]+")) {
            throw a("records name '" + var4 + "' — letters, digits, '_' or '-' only");
         }

         Path var6 = var0.resolve("evidence/ratebooks").resolve(var4 + ".json");
         if (!Files.isRegularFile(var6)) {
            throw new RuleApiException(
               404, "RATEBOOK_NOT_FOUND", "no rate-book records '" + var4 + "' at " + var0.getFileName() + "/evidence/ratebooks/" + var4 + ".json"
            );
         }

         if (!(a(var6, "records '" + var4 + "'") instanceof Map var31)) {
            throw a("'" + var4 + "' is not a records object");
         }
         var3 = var31;
      }

      if (!(var3.get("records") instanceof List var22)) {
         throw a("records.records is a list of {code, edition, line, jurisdiction, status, effective, ...}");
      } else {
         ArrayList<io.karatelabs.ext.rules.RatebookEditionChecker.RatebookRecord> var24 = new ArrayList();
         LinkedHashSet<java.lang.String> var25 = new LinkedHashSet();

         for (int var8 = 0; var8 < var22.size(); var8++) {
            if (!(var22.get(var8) instanceof Map var9)) {
               throw a("records[" + var8 + "] is not an object");
            }

            String var26 = "records[" + var8 + "].";
            String var11 = b(var9.get("code"), var26 + "code");
            String var12 = b(var9.get("edition"), var26 + "edition");
            String var13 = b(var9.get("line"), var26 + "line");
            String var14 = b(var9.get("jurisdiction"), var26 + "jurisdiction");
            String var15 = b(var9.get("status"), var26 + "status");
            String var16 = c(var9.get("effective"), var26 + "effective");
            String var17 = var9.get("renewalEffective") == null ? null : c(var9.get("renewalEffective"), var26 + "renewalEffective");
            String var18 = var9.get("expiration") == null ? null : c(var9.get("expiration"), var26 + "expiration");
            String var19 = var9.get("nameDate") == null ? null : c(var9.get("nameDate"), var26 + "nameDate");
            if (!var25.add(var11 + "\t" + var12 + "\t" + var13)) {
               throw new RuleApiException(400, "RATEBOOK_DUPLICATE", var26 + "duplicates the record " + var11 + " / " + var12 + " / " + var13);
            }

            io.karatelabs.ext.rules.RatebookSelectionRule.EditionResolution var20 = var2.a(var12);
            if (var9.get("companionOf") != null && var20.g()) {
               io.karatelabs.ext.rules.RatebookSelectionRule.EditionResolution var21 = var2.a(b(var9.get("companionOf"), var26 + "companionOf"));
               if (!var20.a() || !var21.c().equals(var20.d())) {
                  throw new RuleApiException(
                     400,
                     "COMPANION_CONFLICT",
                     var26
                        + "companionOf '"
                        + var9.get("companionOf")
                        + "' contradicts the editionId pattern, which reads '"
                        + var12
                        + "' as "
                        + (var20.a() ? "a companion of " + var20.d() : "a base edition")
                  );
               }
            }

            var24.add(new io.karatelabs.ext.rules.RatebookEditionChecker.RatebookRecord(var11, var12, var13, var14, var15, var2.b(var15), var16, var17, var18, var19, var20));
         }

         return var24;
      }
   }

   static io.karatelabs.ext.rules.RatebookEditionChecker.FiledRatebookData a(Path var0, RatebookSelectionRule var1) {
      if (!var1.f().equals("changes")) {
         return new io.karatelabs.ext.rules.RatebookEditionChecker.FiledRatebookData(var1.f(), a(var0, var1.f(), var1), List.of(), List.of(), Map.of());
      } else {
         Path var2 = var0.resolve("evidence/changes");
         ArrayList var3 = new ArrayList();
         if (Files.isDirectory(var2)) {
            try (var var4 = Files.list(var2)) {
               var4.filter(var0x -> var0x.getFileName().toString().endsWith(".json")).sorted().forEach(var3::add);
            } catch (IOException var25) {
               throw new UncheckedIOException(var25);
            }
         }

         ArrayList<io.karatelabs.ext.rules.RatebookEditionChecker.RatebookRecord> var26 = new ArrayList();
         ArrayList<java.lang.String> var5 = new ArrayList();
         ArrayList<java.lang.String> var6 = new ArrayList();
         LinkedHashMap<String, List<String>> var7 = new LinkedHashMap<>();

         for (Path var9 : (Iterable<Path>) (Iterable<?>) (var3)) {
            if (a(var9, "change set " + var9.getFileName()) instanceof Map var11
               && var11.get("id") instanceof String var12
               && !var12.isEmpty()
               && var11.get("produces") instanceof Map var13) {
               var5.add(var12);
               if (var13.get("edition") instanceof String var28 && !var28.isEmpty()) {
                  if (var13.get("jurisdictions") instanceof List var29 && var13.get("lines") instanceof List var16) {
                     String var31 = var13.get("effective") == null ? null : a(var13.get("effective"), var12, "effective");
                     String var18 = var13.get("renewalEffective") == null ? null : a(var13.get("renewalEffective"), var12, "renewalEffective");

                     for (Object var20 : var29) {
                        for (Object var22 : var16) {
                           if (var31 == null) {
                              var7.computeIfAbsent(var20 + "\t" + var22, var0x -> new ArrayList<>()).add(var12);
                           } else {
                              var26.add(
                                 new io.karatelabs.ext.rules.RatebookEditionChecker.RatebookRecord(var12, var28, String.valueOf(var22), String.valueOf(var20), null, true, var31, var18, null, null, var1.a(var28))
                              );
                           }
                        }
                     }
                     continue;
                  }

                  var6.add(var12);
                  continue;
               }

               throw new RuleApiException(400, "CHANGESET_INVALID", "change set " + var12 + ": produces.edition is a non-empty string");
            }

            throw new RuleApiException(400, "CHANGESET_INVALID", "change set " + var9.getFileName() + " carries id and produces {edition, effective, ...}");
         }

         return new io.karatelabs.ext.rules.RatebookEditionChecker.FiledRatebookData("changes", var26, var5, var6, var7);
      }
   }

   static Map<String, Object> a(io.karatelabs.ext.rules.RulebookRepository.RulebookDocument var0, Path var1, Object var2, List<io.karatelabs.ext.rules.RatebookEditionChecker.RatebookRecord> var3, RatebookSelectionRule var4) {
      a(var0.e(), var4.c());
      io.karatelabs.ext.rules.RatebookEditionChecker.FiledRatebookData var5 = a(var1, var4);
      String var6 = var4.h() == null ? null : LocalDate.parse(var4.h()).plusDays(1L).toString();
      ArrayList<java.util.Map<java.lang.String,java.lang.Object>> var7 = new ArrayList();
      ArrayList<java.util.Map<java.lang.String,java.lang.Object>> var8 = new ArrayList();

      for (String var10 : var4.i()) {
         for (String var12 : var4.j()) {
            var var13 = var3.stream().filter(var2x -> var2x.a(var10, var12)).toList();
            var var14 = var5.b().stream().filter(var2x -> var2x.a(var10, var12)).toList();

            for (String var16 : List.of("new", "renewal")) {
               boolean var17 = var16.equals("renewal") && var4.d();
               if (!var5.d().isEmpty()) {
                  var var32 = a(var10, var12, var16, var4.g(), var6);
                  var32.put("reason", "filed-scope-undeclared");
                  var32.put("changeSets", var5.d());
                  var8.add(var32);
               } else {
                  var var18 = var5.e().get(var10 + "\t" + var12);
                  if (var18 != null) {
                     var var34 = a(var10, var12, var16, var4.g(), var6);
                     var34.put("reason", "filed-edition-undated");
                     var34.put("changeSets", var18);
                     var8.add(var34);
                  } else if (var13.isEmpty() && var14.isEmpty()) {
                     var var33 = a(var10, var12, var16, var4.g(), var6);
                     var33.put("winner", null);
                     var33.put("filedWinner", null);
                     a(var33, "none-selectable", var4);
                     var7.add(var33);
                  } else {
                     TreeSet<String> var19 = new TreeSet<>();

                     for (io.karatelabs.ext.rules.RatebookEditionChecker.RatebookRecord var21 : (Iterable<io.karatelabs.ext.rules.RatebookEditionChecker.RatebookRecord>) (Iterable<?>) (var13)) {
                        a(var21, var17, var19);
                     }

                     for (io.karatelabs.ext.rules.RatebookEditionChecker.RatebookRecord var37 : (Iterable<io.karatelabs.ext.rules.RatebookEditionChecker.RatebookRecord>) (Iterable<?>) (var14)) {
                        a(var37, var17, var19);
                     }

                     ArrayList<java.lang.String> var36 = new ArrayList();
                     if (var4.g() != null) {
                        var36.add(var4.g());
                        var19.tailSet(var4.g(), false).forEach(var36::add);
                     } else {
                        var36.addAll(var19);
                     }

                     if (var4.h() != null) {
                        var36.removeIf(var1x -> var1x.compareTo(var4.h()) > 0);
                     }

                     if (var4.g() == null) {
                        var var38 = a(var10, var12, var16, null, var36.isEmpty() ? var6 : (String)var36.getFirst());
                        var38.put("reason", "pre-first");
                        var8.add(var38);
                     }

                     for (int var39 = 0; var39 < var36.size(); var39++) {
                        String var22 = (String)var36.get(var39);
                        String var23 = var39 + 1 < var36.size() ? (String)var36.get(var39 + 1) : var6;
                        a(a(var10, var12, var16, var22, var23), var22, var13, var14, var17, var4, var7, var8);
                     }
                  }
               }
            }
         }
      }

      LinkedHashMap<java.lang.String, java.lang.Integer> var24 = new LinkedHashMap();

      for (String var28 : j) {
         var24.put(var28, 0);
      }

      for (Map var29 : (Iterable<Map>) (Iterable<?>) (var7)) {
         var24.merge((String)var29.get("finding"), 1, Integer::sum);
      }

      boolean var27 = !var7.isEmpty();
      LinkedHashMap<java.lang.String, java.lang.Object> var30 = new LinkedHashMap();
      var30.put("book", var0.d());
      var30.put("records", var2 instanceof String var31 ? var31 : "inline");
      var30.put("ok", var27 && (Integer)var24.get("ok") == var7.size());
      var30.put("checked", var27);
      var30.put("complete", var8.isEmpty());
      var30.put("rule", var4.k());
      var30.put("counts", var24);
      var30.put("windows", var7);
      var30.put("notEvaluated", var8);
      var30.put("disclosures", a(var3, var5, var4));
      return var30;
   }

   static Map<String, Object> a(Map<String, Object> var0, Path var1, Object var2, RatebookSelectionRule var3) {
      io.karatelabs.ext.diff.DiffReport var4 = new io.karatelabs.ext.diff.DiffReport("rating.diff/1", "karate-edition-check", ExtMeta.version("karate-max-meta.properties"));

      for (Map var6 : (Iterable<Map>) (Iterable<?>) ((List)var0.get("windows"))) {
         String var7 = (String)var6.get("finding");
         if (!"ok".equals(var7)) {
            String var8 = var6.get("from") + ".." + (var6.get("to") == null ? "" : var6.get("to"));

            String var9 = switch (var7) {
               case "competing" -> "competing " + var6.get("competing");
               case "ambiguous" -> "tied " + var6.get("tied");
               case "orphan-companion" -> "orphans " + var6.get("orphans");
               case "not-filed" -> "winner " + a(var6.get("winner")) + ", filed " + a(var6.get("filedWinner"));
               default -> "no edition selectable";
            };
            var4.a(
               var4.a(var7, "ambiguous".equals(var7) ? io.karatelabs.ext.diff.DiffSeverity.WARNING : io.karatelabs.ext.diff.DiffSeverity.BREAKING)
                  .a(var6.get("jurisdiction") + " " + var6.get("line") + " " + var6.get("transaction") + " " + var8)
                  .b(var9)
                  .a("ratebook", a(var6))
                  .a("window", var8, null)
                  .a("rule", var6.get("rule"))
                  .a("winner", var6.get("winner"))
                  .a("filedWinner", var6.get("filedWinner"))
                  .a("competing", var6.get("competing"))
                  .a("tied", var6.get("tied"))
                  .a("orphans", var6.get("orphans"))
            );
         }
      }

      for (Map var13 : (Iterable<Map>) (Iterable<?>) ((List)var0.get("notEvaluated"))) {
         String var14 = (String)var13.get("reason");
         LinkedHashMap<java.lang.String, java.lang.Object> var15 = new LinkedHashMap();

         for (String var10 : List.of("reasons", "changeSets")) {
            if (var13.get(var10) != null) {
               var15.put(var10, var13.get(var10));
            }
         }

         String var17 = var14.equals("pre-first") ? null : (!var14.startsWith("filed-") && !var14.equals("no-filed-records") ? "current" : "baseline");
         String var18 = (var13.get("from") == null ? "" : var13.get("from")) + ".." + (var13.get("to") == null ? "" : var13.get("to"));
         var4.a(
            new io.karatelabs.ext.diff.UnevaluatedDiff(
               var14,
               "window " + var13.get("jurisdiction") + "/" + var13.get("line") + "/" + var13.get("transaction") + " " + var18,
               var17,
               null,
               var15.isEmpty() ? null : var15
            )
         );
      }

      return var4.a(b(var1, var3), a(var1, var2)).a(Boolean.TRUE.equals(var0.get("checked"))).toMap();
   }

   private static String a(Map<String, Object> var0) {
      return var0.get("jurisdiction") + "/" + var0.get("line") + "/" + var0.get("transaction") + "/" + var0.get("from");
   }

   private static Object a(Object var0) {
      return var0 instanceof Map var1 ? var1.get("edition") : null;
   }

   private static io.karatelabs.ext.diff.DiffReport.DiffArtifact b(Path var0, RatebookSelectionRule var1) {
      if (!var1.f().equals("changes")) {
         return a(var0, (Object)var1.f());
      } else {
         var var2 = new LinkedHashMap<>(io.karatelabs.ext.rules.RulebookImportEngine.a(var0, "evidence/changes"));
         var2.keySet().removeIf(var0x -> !var0x.endsWith(".json"));
         io.karatelabs.ext.rules.RulebookRepository.DigestReference var3 = RuleTableDiffEngine.b(var2);
         return io.karatelabs.ext.diff.DiffReport.DiffArtifact.a("evidence/changes", var3.c(), var3.b());
      }
   }

   private static io.karatelabs.ext.diff.DiffReport.DiffArtifact a(Path var0, Object var1) {
      if (var1 instanceof String var2) {
         String var3 = "evidence/ratebooks/" + var2 + ".json";
         return io.karatelabs.ext.diff.DiffReport.DiffArtifact.a(var3, io.karatelabs.ext.rules.RulebookImportEngine.a(var0.resolve(var3)), "file");
      } else {
         return io.karatelabs.ext.diff.DiffReport.DiffArtifact.a("inline", io.karatelabs.ext.common.Sha256.a(io.karatelabs.ext.common.CanonicalJson.a(var1)), "inline");
      }
   }

   private static void a(io.karatelabs.ext.rules.RatebookEditionChecker.RatebookRecord var0, boolean var1, Set<String> var2) {
      String var3 = var0.a(var1);
      var2.add(var3 != null ? var3 : var0.g());
      if (var0.i() != null) {
         var2.add(var0.i());
      }
   }

   private static void a(
      Map<String, Object> var0,
      String var1,
      List<io.karatelabs.ext.rules.RatebookEditionChecker.RatebookRecord> var2,
      List<io.karatelabs.ext.rules.RatebookEditionChecker.RatebookRecord> var3,
      boolean var4,
      RatebookSelectionRule var5,
      List<Map<String, Object>> var6,
      List<Map<String, Object>> var7
   ) {
      ArrayList var8 = new ArrayList();
      List var9 = a(var2, var1, var4, var8, "");
      List var10 = a(var3, var1, var4, var8, "filed-");
      if (!var8.isEmpty()) {
         var0.put("reason", ((Map)var8.getFirst()).get("reason"));
         var0.put("reasons", var8);
         var7.add(var0);
      } else {
         io.karatelabs.ext.rules.RatebookEditionChecker.EditionSelection var11 = a(var9, var2, var4, var5);
         io.karatelabs.ext.rules.RatebookEditionChecker.EditionSelection var12 = a(var10, var3, var4, var5);
         var0.put("winner", var11.b() == null ? null : var11.b().a());
         var0.put("filedWinner", var12.b() == null ? null : var12.b().a());
         if (var11.a().isEmpty()) {
            a(var0, "none-selectable", var5);
         } else if (!var11.d().isEmpty()) {
            var0.put("competing", var11.d());
            a(var0, "competing", var5);
         } else if (var11.c() != null) {
            var0.put("tied", var11.c());
            a(var0, "ambiguous", var5);
         } else if (!var11.e().isEmpty()) {
            var0.put("orphans", var11.e());
            a(var0, "orphan-companion", var5);
         } else {
            if (var12.b() == null) {
               var0.put("reason", var12.c() != null ? "filed-ambiguous" : "no-filed-records");
               var7.add(var0);
               return;
            }

            if (!a(var11.b(), var12.b())) {
               a(var0, "not-filed", var5);
            } else {
               a(var0, "ok", var5);
            }
         }

         var6.add(var0);
      }
   }

   private static List<io.karatelabs.ext.rules.RatebookEditionChecker.RatebookRecord> a(List<io.karatelabs.ext.rules.RatebookEditionChecker.RatebookRecord> var0, String var1, boolean var2, List<Map<String, Object>> var3, String var4) {
      ArrayList<io.karatelabs.ext.rules.RatebookEditionChecker.RatebookRecord> var5 = new ArrayList();

      for (io.karatelabs.ext.rules.RatebookEditionChecker.RatebookRecord var7 : var0) {
         if (var7.i() == null || var7.i().compareTo(var1) > 0) {
            String var8 = var7.a(var2);
            if (var8 == null) {
               if (var7.g().compareTo(var1) <= 0 && !Boolean.FALSE.equals(var7.f())) {
                  var3.add(a(var4 + "missing-renewal-date", var7));
               }
            } else if (var8.compareTo(var1) <= 0) {
               if (var7.f() == null) {
                  var3.add(a(var4 + "unknown-status", var7));
               } else if (var7.f()) {
                  if (!var7.k().g()) {
                     var3.add(a(var4 + "edition-id-unmatched", var7));
                  } else {
                     var5.add(var7);
                  }
               }
            }
         }
      }

      return var5;
   }

   private static Map<String, Object> a(String var0, io.karatelabs.ext.rules.RatebookEditionChecker.RatebookRecord var1) {
      LinkedHashMap<java.lang.String, java.lang.Object> var2 = new LinkedHashMap();
      var2.put("reason", var0);
      var2.put("edition", var1.b());
      if (var1.e() != null) {
         var2.put("status", var1.e());
      }

      return var2;
   }

   private static io.karatelabs.ext.rules.RatebookEditionChecker.EditionSelection a(List<io.karatelabs.ext.rules.RatebookEditionChecker.RatebookRecord> var0, List<io.karatelabs.ext.rules.RatebookEditionChecker.RatebookRecord> var1, boolean var2, RatebookSelectionRule var3) {
      LinkedHashMap<String, List<io.karatelabs.ext.rules.RatebookEditionChecker.RatebookRecord>> var4 = new LinkedHashMap<>();

      for (io.karatelabs.ext.rules.RatebookEditionChecker.RatebookRecord var6 : var0) {
         var4.computeIfAbsent(var6.k().c(), var0x -> new ArrayList<>()).add(var6);
      }

      ArrayList<java.lang.String> var13 = new ArrayList();
      ArrayList<io.karatelabs.ext.rules.RatebookEditionChecker.EditionCandidate> var14 = new ArrayList();
      ArrayList<io.karatelabs.ext.rules.RatebookEditionChecker.EditionCandidate> var7 = new ArrayList<>();

      for (List<io.karatelabs.ext.rules.RatebookEditionChecker.RatebookRecord> var9 : var4.values()) {
         if (var9.size() > 1) {
            var9.forEach(var1x -> var13.add(var1x.b()));
         }

         io.karatelabs.ext.rules.RatebookEditionChecker.RatebookRecord var10 = var9.stream().max(Comparator.<io.karatelabs.ext.rules.RatebookEditionChecker.RatebookRecord, String>comparing(var1x -> var1x.a(var2)).thenComparing(var0x -> var0x.k().e(), RatebookEditionChecker::a)).orElseThrow();
         io.karatelabs.ext.rules.RatebookEditionChecker.EditionCandidate var11 = new io.karatelabs.ext.rules.RatebookEditionChecker.EditionCandidate(var10.k(), var10.a(var2), var9, new ArrayList<>(), false);
         (var10.k().a() ? var7 : var14).add(var11);
      }

      ArrayList<java.lang.String> var15 = new ArrayList();

      for (io.karatelabs.ext.rules.RatebookEditionChecker.EditionCandidate var18 : (Iterable<io.karatelabs.ext.rules.RatebookEditionChecker.EditionCandidate>) (Iterable<?>) (var7)) {
         io.karatelabs.ext.rules.RatebookEditionChecker.EditionCandidate var20 = var14.stream().filter(var1x -> !var1x.c().a() && var1x.c().c().equals(var18.c().d())).findFirst().orElse(null);
         if (var3.e()) {
            if (var20 != null) {
               var20.f().addAll(var18.e());
            } else {
               var18.e().forEach(var1x -> var15.add(var1x.b()));
               var14.add(new io.karatelabs.ext.rules.RatebookEditionChecker.EditionCandidate(var18.c(), var18.d(), var18.e(), List.of(), true));
            }
         } else {
            if (var20 != null) {
               var20.e().forEach(var1x -> var13.add(var1x.b()));
               var18.e().forEach(var1x -> var13.add(var1x.b()));
            }

            if (var1.stream()
               .noneMatch(
                  var2x -> !var2x.k().a()
                     && var2x.k().c().equals(var18.c().d())
                     && Boolean.TRUE.equals(var2x.f())
                     && var2x.a(var2) != null
                     && var2x.a(var2).compareTo(var18.d()) <= 0
                     && (var2x.i() == null || var2x.i().compareTo(var18.d()) > 0)
               )) {
               var18.e().forEach(var1x -> var15.add(var1x.b()));
            }

            var14.add(var18);
         }
      }

      io.karatelabs.ext.rules.RatebookEditionChecker.EditionCandidate var17 = null;
      List var19 = null;
      if (!var14.isEmpty()) {
         String var21 = var14.stream().map(io.karatelabs.ext.rules.RatebookEditionChecker.EditionCandidate::d).max(Comparator.naturalOrder()).orElseThrow();
         var var12 = var14.stream().filter(var1x -> var1x.d().equals(var21)).toList();
         if (var12.size() == 1) {
            var17 = (io.karatelabs.ext.rules.RatebookEditionChecker.EditionCandidate)var12.getFirst();
         } else {
            var19 = var12.stream().map(var0x -> var0x.c().c()).toList();
         }
      }

      return new io.karatelabs.ext.rules.RatebookEditionChecker.EditionSelection(var14, var17, var19, List.copyOf(new LinkedHashSet<>(var13)), List.copyOf(new LinkedHashSet<>(var15)));
   }

   private static boolean a(io.karatelabs.ext.rules.RatebookEditionChecker.EditionCandidate var0, io.karatelabs.ext.rules.RatebookEditionChecker.EditionCandidate var1) {
      return var1.g() ? var0.b().contains(var1.c().c()) || var0.c().c().equals(var1.c().c()) : var0.c().c().equals(var1.c().c()) && var0.b().equals(var1.b());
   }

   private static int a(String var0, String var1) {
      if (var0 != null && var1 != null) {
         return var0.matches("\\d+") && var1.matches("\\d+") ? Long.compare(Long.parseLong(var0), Long.parseLong(var1)) : var0.compareTo(var1);
      } else {
         return var0 == null ? (var1 == null ? 0 : -1) : 1;
      }
   }

   private static Map<String, Object> a(String var0, String var1, String var2, String var3, String var4) {
      LinkedHashMap<java.lang.String, java.lang.Object> var5 = new LinkedHashMap();
      var5.put("jurisdiction", var0);
      var5.put("line", var1);
      var5.put("transaction", var2);
      var5.put("from", var3);
      var5.put("to", var4);
      return var5;
   }

   private static void a(Map<String, Object> var0, String var1, RatebookSelectionRule var2) {
      var0.put("finding", var1);
      if (!"ok".equals(var1)) {
         var0.put("rule", var2.a());
      }
   }

   private static Map<String, Object> a(List<io.karatelabs.ext.rules.RatebookEditionChecker.RatebookRecord> var0, io.karatelabs.ext.rules.RatebookEditionChecker.FiledRatebookData var1, RatebookSelectionRule var2) {
      LinkedHashMap<java.lang.String, java.lang.Object> var3 = new LinkedHashMap();
      ArrayList<java.util.LinkedHashMap> var4 = new ArrayList();
      LinkedHashSet<java.lang.String> var5 = new LinkedHashSet();
      ArrayList<java.lang.String> var6 = new ArrayList();

      for (io.karatelabs.ext.rules.RatebookEditionChecker.RatebookRecord var8 : var0) {
         if (var8.j() != null && !var8.j().equals(var8.g())) {
            LinkedHashMap<java.lang.String, java.lang.String> var9 = new LinkedHashMap();
            var9.put("edition", var8.b());
            var9.put("nameDate", var8.j());
            var9.put("effective", var8.g());
            var4.add(var9);
         }

         if (!var2.i().contains(var8.d()) || !var2.j().contains(var8.c())) {
            var6.add(var8.b() + " (" + var8.d() + " / " + var8.c() + ")");
         }

         if (!var8.k().h()) {
            var5.add(var8.b());
         }
      }

      for (io.karatelabs.ext.rules.RatebookEditionChecker.RatebookRecord var12 : var1.b()) {
         if (!var12.k().h()) {
            var5.add(var12.b());
         }
      }

      if (!var4.isEmpty()) {
         var3.put("nameDates", var4);
      }

      if (!var5.isEmpty()) {
         var3.put("unmapped", List.copyOf(var5));
      }

      if (!var6.isEmpty()) {
         var3.put("outOfScope", var6);
      }

      LinkedHashMap<java.lang.String, java.lang.Object> var11 = new LinkedHashMap();
      var11.put("source", var1.a());
      if (var1.a().equals("changes")) {
         var11.put("changeSets", var1.c());
      }

      var3.put("filed", var11);
      return var3;
   }

   static void a(Object var0, String var1) {
      Object var2 = var0;

      for (String var6 : var1.split("\\.")) {
         var2 = var2 instanceof Map var7 ? var7.get(var6) : null;
      }

      if (!"#date".equals(var2) && !"##date".equals(var2)) {
         throw new RuleApiException(
            400,
            "RULE_INVALID",
            "selection rule: dateKey '" + var1 + "' is not a #date field of schema.js" + (var2 == null ? " (no such field)" : " (found " + var2 + ")")
         );
      }
   }

   private static Object a(Path var0, String var1) {
      try {
         return Json.parseStrict(Files.readString(var0, StandardCharsets.UTF_8), true);
      } catch (IOException var3) {
         throw new UncheckedIOException(var3);
      } catch (RuntimeException var4) {
         throw new RuleApiException(400, "PARSE_FAILED", var1 + ": " + var4.getMessage());
      }
   }

   private static String b(Object var0, String var1) {
      if (var0 instanceof String var2 && !var2.isEmpty()) {
         return var2;
      } else {
         throw a(var1 + " is a non-empty string");
      }
   }

   private static String c(Object var0, String var1) {
      String var2 = b(var0, var1);
      if (RatebookSelectionRule.c(var2) == null) {
         throw a(var1 + " is an ISO date (YYYY-MM-DD), not '" + var2 + "'");
      } else {
         return var2;
      }
   }

   private static String a(Object var0, String var1, String var2) {
      if (var0 instanceof String var3 && RatebookSelectionRule.c(var3) != null) {
         return var3;
      } else {
         throw new RuleApiException(400, "CHANGESET_INVALID", "change set " + var1 + ": produces." + var2 + " is an ISO date (YYYY-MM-DD), not '" + var0 + "'");
      }
   }

   private static RuleApiException a(String var0) {
      return new RuleApiException(400, "RATEBOOK_INVALID", "rate-book records: " + var0);
   }

   private record EditionCandidate(io.karatelabs.ext.rules.RatebookSelectionRule.EditionResolution c, String d, List<io.karatelabs.ext.rules.RatebookEditionChecker.RatebookRecord> e, List<io.karatelabs.ext.rules.RatebookEditionChecker.RatebookRecord> f, boolean g) {

      private EditionCandidate(io.karatelabs.ext.rules.RatebookSelectionRule.EditionResolution c, String d, List<io.karatelabs.ext.rules.RatebookEditionChecker.RatebookRecord> e, List<io.karatelabs.ext.rules.RatebookEditionChecker.RatebookRecord> f, boolean g) {
         this.c = c;
         this.d = d;
         this.e = e;
         this.f = f;
         this.g = g;
      }

      Map<String, Object> a() {
         LinkedHashMap<java.lang.String, java.lang.Object> var1 = new LinkedHashMap();
         var1.put("edition", this.c.c());
         if (this.c.e() != null) {
            var1.put("version", this.c.e());
         }

         var1.put("date", this.d);
         var1.put("records", this.e.stream().map(io.karatelabs.ext.rules.RatebookEditionChecker.RatebookRecord::b).toList());
         if (!this.f.isEmpty()) {
            var1.put("companions", this.f.stream().map(io.karatelabs.ext.rules.RatebookEditionChecker.RatebookRecord::b).toList());
         }

         return var1;
      }

      Set<String> b() {
         LinkedHashSet<java.lang.String> var1 = new LinkedHashSet();

         for (io.karatelabs.ext.rules.RatebookEditionChecker.RatebookRecord var3 : this.f) {
            var1.add(var3.k().c());
         }

         return var1;
      }

      public io.karatelabs.ext.rules.RatebookSelectionRule.EditionResolution c() {
         return this.c;
      }

      public String d() {
         return this.d;
      }

      public List<io.karatelabs.ext.rules.RatebookEditionChecker.RatebookRecord> e() {
         return this.e;
      }

      public List<io.karatelabs.ext.rules.RatebookEditionChecker.RatebookRecord> f() {
         return this.f;
      }

      public boolean g() {
         return this.g;
      }
   }

   record FiledRatebookData(String a, List<io.karatelabs.ext.rules.RatebookEditionChecker.RatebookRecord> b, List<String> c, List<String> d, Map<String, List<String>> e) {

      FiledRatebookData(String a, List<io.karatelabs.ext.rules.RatebookEditionChecker.RatebookRecord> b, List<String> c, List<String> d, Map<String, List<String>> e) {
         this.a = a;
         this.b = b;
         this.c = c;
         this.d = d;
         this.e = e;
      }
   }

   record RatebookRecord(String a, String b, String c, String d, String e, Boolean f, String g, String h, String i, String j, io.karatelabs.ext.rules.RatebookSelectionRule.EditionResolution k) {

      RatebookRecord(String a, String b, String c, String d, String e, Boolean f, String g, String h, String i, String j, io.karatelabs.ext.rules.RatebookSelectionRule.EditionResolution k) {
         this.a = a;
         this.b = b;
         this.c = c;
         this.d = d;
         this.e = e;
         this.f = f;
         this.g = g;
         this.h = h;
         this.i = i;
         this.j = j;
         this.k = k;
      }

      String a(boolean var1) {
         return var1 ? this.h : this.g;
      }

      boolean a(String var1, String var2) {
         return this.d.equals(var1) && this.c.equals(var2);
      }
   }

   private record EditionSelection(List<io.karatelabs.ext.rules.RatebookEditionChecker.EditionCandidate> a, io.karatelabs.ext.rules.RatebookEditionChecker.EditionCandidate b, List<String> c, List<String> d, List<String> e) {

      private EditionSelection(List<io.karatelabs.ext.rules.RatebookEditionChecker.EditionCandidate> a, io.karatelabs.ext.rules.RatebookEditionChecker.EditionCandidate b, List<String> c, List<String> d, List<String> e) {
         this.a = a;
         this.b = b;
         this.c = c;
         this.d = d;
         this.e = e;
      }
   }
}
