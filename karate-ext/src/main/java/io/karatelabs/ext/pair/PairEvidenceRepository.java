package io.karatelabs.ext.pair;

import io.karatelabs.common.Json;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.Map.Entry;
import java.util.stream.Stream;

public final class PairEvidenceRepository {
   public static final String SCHEMA = "karate-pair/v1";
   public static final String PAIRS_DIR = "contract/pairs";
   public static final int DEFAULT_MAX_AGE_WITH_BUILD_DAYS = 14;
   public static final int DEFAULT_MAX_AGE_WITHOUT_BUILD_DAYS = 7;
   private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(ZoneOffset.UTC);
   private static final Comparator<io.karatelabs.ext.pair.PairEvidenceRepository.PairEvidenceArtifact> ARTIFACT_COMPARATOR = Comparator.comparing(io.karatelabs.ext.pair.PairEvidenceRepository.PairEvidenceArtifact::b, Comparator.nullsFirst(Comparator.naturalOrder()))
      .thenComparing(var0 -> var0.a().getFileName().toString());
   private static final int g = 32768;

   private PairEvidenceRepository() {
   }

   public static Path evidencePath(Path var0, Instant var1, String var2) {
      return var0.resolve("contract/pairs").resolve(fileName(var1, var2));
   }

   public static String fileName(Instant var0, String var1) {
      return DATE_FORMATTER.format(var0) + "-" + var1 + ".json";
   }

   public static Path writeEvidence(Path var0, Map<String, Object> var1) {
      try {
         Files.createDirectories(var0.getParent());
         Path var2 = io.karatelabs.ext.common.TempFileCreator.a(var0.getParent(), ".pair-", ".tmp");
         Files.writeString(var2, Json.of(var1).toStringPretty() + "\n", StandardCharsets.UTF_8);

         try {
            Files.move(var2, var0, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
         } catch (AtomicMoveNotSupportedException var4) {
            Files.move(var2, var0, StandardCopyOption.REPLACE_EXISTING);
         }

         return var0;
      } catch (IOException var5) {
         throw new PairException("could not write the pair evidence to " + var0 + ": " + var5.getMessage(), var5);
      }
   }

   public static Map<String, Object> readEvidence(Path var0) {
      if (!Files.isRegularFile(var0)) {
         throw new PairException("no pair evidence at " + var0);
      } else {
         Map var1;
         try {
            var1 = Json.of(Files.readString(var0)).asMap();
         } catch (IOException var3) {
            throw new PairException("could not read " + var0 + ": " + var3.getMessage(), var3);
         } catch (RuntimeException var4) {
            throw new PairException(
               var0
                  + " is not readable as JSON ("
                  + var4.getMessage()
                  + ") — a stored pair is a committed file, so check it for merge-conflict markers or a hand edit. The evidence is immutable by design: repair it from git history rather than patching it, or mint a fresh pair.",
               var4
            );
         }

         if (!"karate-pair/v1".equals(var1.get("schema"))) {
            throw new PairException(var0 + " is not a karate-pair/v1 artifact (its `schema` reads '" + var1.get("schema") + "')");
         } else {
            return var1;
         }
      }
   }

   public static String specInput(Map<String, Object> var0) {
      if (var0.get("bindings") instanceof Map var1 && var1.get("inputs") instanceof Map var2) {
         Object var5 = var2.get("spec");
         return var5 == null ? null : String.valueOf(var5);
      } else {
         return null;
      }
   }

   public static boolean sameResolvedFile(Path var0, String var1, String var2) {
      if (var1 == null || var2 == null) {
         return false;
      } else if (var1.equals(var2)) {
         return true;
      } else {
         Path var3 = resolvePath(var0, var1);
         Path var4 = resolvePath(var0, var2);
         if (Files.isRegularFile(var3) && Files.isRegularFile(var4)) {
            try {
               return Files.isSameFile(var3, var4);
            } catch (IOException var6) {
               return var3.equals(var4);
            }
         } else {
            return false;
         }
      }
   }

   private static List<io.karatelabs.ext.pair.PairEvidenceRepository.PairEvidenceArtifact> listArtifacts(Path var0) {
      Path var1 = var0.resolve("contract/pairs");
      if (!Files.isDirectory(var1)) {
         return List.of();
      } else {
         ArrayList<io.karatelabs.ext.pair.PairEvidenceRepository.PairEvidenceArtifact> var2 = new ArrayList();

         try (var var3 = Files.list(var1)) {
            var var4 = var3.filter(var0x -> var0x.getFileName().toString().endsWith(".json")).toList().iterator();

            label53:
            while (true) {
               Path var5;
               Map var6;
               while (true) {
                  if (!var4.hasNext()) {
                     break label53;
                  }

                  var5 = (Path)var4.next();

                  try {
                     var6 = readEvidence(var5);
                     break;
                  } catch (PairException var11) {
                     if (!String.valueOf(var11.getMessage()).contains("is not a karate-pair/v1")) {
                        var2.add(new io.karatelabs.ext.pair.PairEvidenceRepository.PairEvidenceArtifact(var5, null, null, false));
                     }
                  }
               }

               Instant var7;
               try {
                  var7 = Instant.parse(String.valueOf(var6.get("mintedAt")));
               } catch (RuntimeException var10) {
                  var7 = null;
               }

               var2.add(new io.karatelabs.ext.pair.PairEvidenceRepository.PairEvidenceArtifact(var5, var7, specInput(var6), true));
            }
         } catch (IOException var13) {
            return List.of();
         }

         var2.sort(ARTIFACT_COMPARATOR.reversed());
         return var2;
      }
   }

   public static Path findLatestBySpec(Path var0, String var1) {
      for (io.karatelabs.ext.pair.PairEvidenceRepository.PairEvidenceArtifact var3 : listArtifacts(var0)) {
         if (var3.d() && sameResolvedFile(var0, var3.c(), var1)) {
            return var3.a();
         }
      }

      return null;
   }

   public static Path latestArtifact(Path var0) {
      var var1 = artifactPaths(var0);
      return var1.isEmpty() ? null : (Path)var1.get(0);
   }

   public static List<Path> artifactPaths(Path var0) {
      return listArtifacts(var0).stream().map(io.karatelabs.ext.pair.PairEvidenceRepository.PairEvidenceArtifact::a).toList();
   }

   public static Map<String, Object> createBindings(Map<String, Object> var0, Map<String, String> var1, Map<String, Object> var2, Instant var3, Integer var4) {
      boolean var5 = var2.get("build") != null && !String.valueOf(var2.get("build")).isBlank();
      int var6 = var4 != null ? var4 : (var5 ? 14 : 7);
      LinkedHashMap<java.lang.String, java.lang.Object> var7 = new LinkedHashMap();
      var7.put("executedAt", var3.toString());
      var7.put("maxAgeDays", var6);
      var7.put("digests", new TreeMap(var1));
      var7.put("inputs", var0);
      var7.put("provider", var2);
      var7.put(
         "note",
         var5
            ? "the provider build was DECLARED by the caller as '"
               + var2.get("build")
               + "', and this evidence keeps the longer default life (14 days) on the strength of that declaration. Nothing re-derives it: the value is an attestation, not a recomputed binding, so it can never appear in `changed` and a provider that moved under a build string you did not update will read `fresh`. It is exactly as good as your release process is at supplying it."
            : "the provider BUILD could not be identified, so a mismatch on that binding cannot be detected at all — which is why this evidence gets the SHORTER default life (7 days). Declare `providerBuild` when your environment can report one and it earns the longer life."
      );
      return var7;
   }

   public static String digestFile(Path var0) {
      if (var0 != null && Files.isRegularFile(var0)) {
         try {
            return shortSha256(Files.readAllBytes(var0));
         } catch (IOException var2) {
            return "unreadable";
         }
      } else {
         return "missing";
      }
   }

   public static String digestFiles(List<Path> var0) {
      var var1 = new ArrayList<>(var0);
      var1.sort(Comparator.comparing(Path::toString));
      StringBuilder var2 = new StringBuilder();

      for (Path var4 : (Iterable<Path>) (Iterable<?>) (var1)) {
         var2.append(var4.getFileName()).append('=').append(digestFile(var4)).append('\n');
      }

      return shortSha256(var2.toString().getBytes(StandardCharsets.UTF_8));
   }

   public static String digestWithPrefix(String var0, List<Path> var1) {
      return shortSha256((var0 + "\n" + digestFiles(var1)).getBytes(StandardCharsets.UTF_8));
   }

   public static String shortSha256(byte[] var0) {
      try {
         MessageDigest var1 = MessageDigest.getInstance("SHA-256");
         byte[] var2 = var1.digest(var0);
         StringBuilder var3 = new StringBuilder("sha256:");

         for (int var4 = 0; var4 < 8; var4++) {
            var3.append(String.format("%02x", var2[var4]));
         }

         return var3.toString();
      } catch (Exception var5) {
         throw new PairException("could not digest: " + var5.getMessage(), var5);
      }
   }

   public static Map<String, String> currentDigests(Path var0, Map<String, Object> var1) {
      LinkedHashMap<java.lang.String, java.lang.String> var2 = new LinkedHashMap();
      if (var1.get("bindings") instanceof Map var3 && var3.get("inputs") instanceof Map var4) {
         if (var4.get("spec") != null) {
            var2.put("spec", digestFile(resolvePath(var0, String.valueOf(var4.get("spec")))));
         }

         if (var4.get("suite") != null) {
            ArrayList<java.nio.file.Path> var6 = new ArrayList();

            for (String var8 : stringList(var4.get("suite"))) {
               Path var9 = resolvePath(var0, var8);
               if (Files.isDirectory(var9)) {
                  try (var var10 = Files.walk(var9)) {
                     var10.filter(var0x -> var0x.toString().endsWith(".feature")).forEach(var6::add);
                  } catch (Exception var15) {
                  }
               } else {
                  var6.add(var9);
               }
            }

            var2.put("suite", digestFiles(var6));
         }

         if (var4.get("mock") != null) {
            ArrayList<java.nio.file.Path> var17 = new ArrayList();

            for (String var21 : stringList(var4.get("mockFiles"))) {
               var17.add(resolvePath(var0, var21));
            }

            var2.put("mock", digestWithPrefix(String.valueOf(var4.get("mock")), var17));
         }

         if (var4.get("profile") != null) {
            var2.put("profile", shortSha256(String.valueOf(var4.get("profile")).getBytes(StandardCharsets.UTF_8)));
         }

         if (var4.get("fixture") != null) {
            var2.put("fixture", digestFile(resolvePath(var0, String.valueOf(var4.get("fixture")))));
         }

         ArrayList<java.nio.file.Path> var18 = new ArrayList();

         for (String var22 : stringList(var4.get("rulebooks"))) {
            var18.add(resolvePath(var0, var22));
         }

         if (!var18.isEmpty()) {
            var2.put("rulebook", digestFiles(var18));
         }

         return var2;
      } else {
         return var2;
      }
   }

   private static Path resolvePath(Path var0, String var1) {
      Path var2 = Path.of(var1);
      return (var2.isAbsolute() ? var2 : var0.resolve(var2)).normalize();
   }

   private static List<String> stringList(Object var0) {
      ArrayList<java.lang.String> var1 = new ArrayList();
      if (var0 instanceof List) {
         for (Object var4 : (List)var0) {
            if (var4 != null && !String.valueOf(var4).isBlank()) {
               var1.add(String.valueOf(var4));
            }
         }
      } else if (var0 != null && !String.valueOf(var0).isBlank()) {
         var1.add(String.valueOf(var0));
      }

      return var1;
   }

   public static Map<String, Object> freshness(Map<String, Object> var0, Map<String, String> var1, Instant var2) {
      Map<java.lang.String, Object> var3 = var0.get("bindings") instanceof Map var4 ? var4 : Map.of();
      Map var19 = var3.get("digests") instanceof Map var20 ? var20 : Map.of();
      Instant var21 = parseInstant(var3.get("executedAt"));
      int var22 = var3.get("maxAgeDays") instanceof Number var7 ? var7.intValue() : 7;
      ArrayList<java.util.LinkedHashMap> var23 = new ArrayList();
      ArrayList<java.lang.String> var24 = new ArrayList();

      for (Entry var10 : (Iterable<Entry>) (Iterable<?>) (var19.entrySet())) {
         String var11 = (String)var1.get(var10.getKey());
         if (var11 == null) {
            var24.add((String)var10.getKey());
         } else if (!var11.equals(String.valueOf(var10.getValue()))) {
            LinkedHashMap<java.lang.String, java.lang.Object> var12 = new LinkedHashMap();
            var12.put("binding", var10.getKey());
            var12.put("was", var10.getValue());
            var12.put("now", var11);
            var23.add(var12);
         }
      }

      long var25 = var21 == null ? -1L : Duration.between(var21, var2).toDays();
      boolean var26 = var25 >= 0L && var25 > var22;
      boolean var27 = var19.isEmpty();
      boolean var13 = var21 == null;
      ArrayList<java.lang.String> var14 = new ArrayList();
      if (var27) {
         var14.add("unbindable");
      }

      if (!var23.isEmpty()) {
         var14.add("stale");
      }

      if (var13) {
         var14.add("undatable");
      }

      if (var26) {
         var14.add("expired");
      }

      String var15 = var27 ? "unbindable" : (!var23.isEmpty() ? "stale" : (var13 ? "undatable" : (var26 ? "expired" : "fresh")));
      LinkedHashMap<java.lang.String, java.lang.Object> var16 = new LinkedHashMap();
      var16.put("status", var15);
      var16.put("asOf", var2.toString());
      var16.put("executedAt", var21 == null ? null : var21.toString());
      var16.put("ageDays", var25 < 0L ? null : var25);
      var16.put("maxAgeDays", var22);
      if (!var14.isEmpty()) {
         var16.put("causes", var14);
      }

      if (!var23.isEmpty()) {
         var16.put("changed", var23);
      }

      if (!var24.isEmpty()) {
         var16.put("unchecked", var24);
      }
      var16.put(
         "note",
         switch (var15) {
            case "unbindable" -> "Unbindable. This pair records no binding digests, so there is nothing to compare today's project against, and no way to establish what it is evidence about. Freshness cannot be established, so this reading withdraws the rung. A stored pair is a committed file: repair it from git history, or mint a new pair.";
            case "undatable" -> "Undatable. This pair's `executedAt` is unreadable, so its age cannot be established and it can never be shown to have expired. Freshness cannot be established, so this reading withdraws the rung. Repair the file from git history, or mint a new pair.";
            case "stale" -> "Stale. This evidence is about a different subject from the one in the project today. These changed after the mint: "
               + (String)var23.stream().map(var0x -> String.valueOf(var0x.get("binding"))).reduce((var0x, var1x) -> var0x + ", " + var1x).orElse("")
               + ". What the run observed is still true of what it observed. It is no longer true of this project."
               + (var26 ? " The evidence is also past its maximum age." : "");
            case "expired" -> "Expired. No binding we can see changed. The evidence is "
               + var25
               + " days old, against a maximum of "
               + var22
               + " days. Past that age, a provider nothing has observed is not assumed to be unchanged.";
            default -> (
                  var24.isEmpty()
                     ? "Fresh at this `asOf`. Every binding still matches"
                     : "Fresh at this `asOf` on every binding this reader could re-derive. It could not recompute "
                        + String.join(", ", var24)
                        + ", so it would not have seen a change there"
               )
               + ", and the evidence is "
               + var25
               + " of "
               + var22
               + " days old. This reader calculates freshness when it reads the file. The stored file never changes, so the same bytes can read fresh today and expired next month.";
         }
      );
      return var16;
   }

   public static Map<String, Object> refreshHint(Map<String, Object> var0) {
      return refreshHint(var0, false);
   }

   public static Map<String, Object> refreshHint(Map<String, Object> var0, boolean var1) {
      LinkedHashMap<java.lang.String, java.lang.Object> var2 = new LinkedHashMap();
      Map<java.lang.String, java.lang.Object> var3 = var0.get("scope") instanceof Map var4 ? var4 : Map.of();
      Object var11 = var3.get("provider");
      String var12 = String.valueOf(var3.get("profile"));
      if (io.karatelabs.ext.pair.DataProfile.AMBIENT.wire().equals(var12)) {
         var2.put("available", false);
         var2.put(
            "why",
            "this pair ran under the `ambient` data profile, whose ceiling sits below `proven substitutable` however fresh the evidence is — refreshing it raises the age, not the ceiling. Move the suite to `seeded` (create what it needs through the API) to lift the ceiling."
         );
         return var2;
      } else if (var11 != null && !String.valueOf(var11).isBlank()) {
         LinkedHashMap<java.lang.String, java.lang.Object> var6 = new LinkedHashMap();

         for (String var8 : List.of(
            "suite",
            "spec",
            "provider",
            "mock",
            "profile",
            "fixture",
            "tags",
            "ignore",
            "aliases",
            "targetProp",
            "env",
            "confirm",
            "providerEnv",
            "providerBuild",
            "maxAgeDays"
         )) {
            if (var3.get(var8) != null) {
               var6.put(var8, var3.get(var8));
            }
         }

         if (var1) {
            var6.put("draft", true);
         }

         var2.put("available", true);
         var2.put("call", "Contract.pair(" + Json.stringifyStrict(var6) + ")");
         String var13 = "decay can REQUEST a provider leg rather than wait for a release gate — this call is pre-bound to this pair's own scope, so running it re-mints exactly this claim.";
         if (var1) {
            var13 = var13
               + " It carries `draft:true` because THIS pair is a draft: refreshing an exploratory pair must not promote it into project evidence. Drop it when this is the run you keep.";
         }

         String var14 = providerTargetCaution(String.valueOf(var11));
         if (var14 != null) {
            var2.put("caution", var14);
         }

         if (var3.get("propKeys") instanceof List var9 && !var9.isEmpty()) {
            var2.put("supply", var9);
            var13 = var13
               + " It cannot carry the system properties this pair ran with — "
               + var9
               + " — because their VALUES are not recorded in a file the project commits, so add them back as `props` yourself. Re-minting without them re-mints a different run.";
         }

         var2.put("why", var13);
         return var2;
      } else {
         var2.put("available", false);
         var2.put(
            "why",
            "the stored pair records no provider target, so there is nothing to point a new provider leg at. Re-mint with Contract.pair({suite, spec, provider})."
         );
         return var2;
      }
   }

   static String providerTargetCaution(String var0) {
      URI var1;
      try {
         var1 = URI.create(var0.trim());
      } catch (RuntimeException var5) {
         return null;
      }

      String var2 = var1.getHost();
      int var3 = var1.getPort();
      boolean var4 = "localhost".equals(var2) || "127.0.0.1".equals(var2) || "::1".equals(var2) || "[::1]".equals(var2) || "0.0.0.0".equals(var2);
      return var4 && var3 >= 32768
         ? "the provider recorded here is "
            + var0
            + " — a loopback address on port "
            + var3
            + ", which is in the range the OS hands out. If that was a stand-in stood up in a console session, the port is gone and this call will re-mint against an unreachable target (which reads as an environment failure, not as decay). Point it at the address the provider answers on now before running it."
         : null;
   }

   private static Instant parseInstant(Object var0) {
      if (var0 == null) {
         return null;
      } else {
         try {
            return Instant.parse(String.valueOf(var0));
         } catch (RuntimeException var2) {
            return null;
         }
      }
   }

   private record PairEvidenceArtifact(Path a, Instant b, String c, boolean d) {}
}
