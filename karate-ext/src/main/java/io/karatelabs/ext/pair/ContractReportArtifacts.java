package io.karatelabs.ext.pair;

import io.karatelabs.core.ReportAssets;
import io.karatelabs.ext.common.jsapi.JsApi;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ContractReportArtifacts {
   private static final String CONTRACT_PAGE_RESOURCE = "ext/contract/pages/contract.html";
   private static final String CONTRACT_DATA_JS = "ext/contract/contract-data.js";
   private static final String DEFAULT_REPORT_DIR = "target/karate-reports";
   private static final String REPORTS_HTTP_PREFIX = "/api/artifacts/reports/";

   public static Object readPairReport(Path var0, Object var1, Object var2, String var3) {
      Path var4 = var0 != null ? var0 : Path.of(".");
      if (var2 == null && var1 instanceof Map) {
         var2 = var1;
         var1 = null;
      }

      boolean var5 = stringOrNull(var1) == null;
      List var6 = var5 ? io.karatelabs.ext.pair.PairEvidenceRepository.artifactPaths(var4) : List.of();
      Path var7 = var5 ? (var6.isEmpty() ? null : (Path)var6.get(0)) : resolvePath(var4, stringOrNull(var1));
      if (var7 == null) {
         return JsApi.notFound(
            "no pair evidence under "
               + var4.resolve("contract/pairs")
               + " — mint one with Contract.pair({suite, spec, provider}). Nothing else writes a pair: an everyday mock run is a regression check on the mock, not half of a pair."
         );
      } else {
         Map var8;
         try {
            var8 = io.karatelabs.ext.pair.PairEvidenceRepository.readEvidence(var7);
         } catch (PairException var18) {
            return JsApi.missing(var3 + ": " + var18.getMessage());
         }

         Instant var9;
         try {
            Object var10 = var2 instanceof Map var11 ? var11.get("asOf") : null;
            var9 = var10 == null ? Instant.now() : Instant.parse(String.valueOf(var10));
         } catch (RuntimeException var17) {
            return JsApi.missing(
               var3
                  + ": `asOf` must be an ISO-8601 instant, e.g. '2026-09-01T00:00:00Z' — it is the EXPLICIT evaluation instant that makes a time-sensitive verdict deterministic instead of a hidden dependency on the clock"
            );
         }

         Map<java.lang.String, Object> var19 = io.karatelabs.ext.pair.PairEvidenceRepository.freshness(var8, io.karatelabs.ext.pair.PairEvidenceRepository.currentDigests(var4, var8), var9);
         LinkedHashMap<java.lang.String, java.lang.Object> var20 = new LinkedHashMap(var8);
         var20.put("freshness", var19);
         Map var12 = io.karatelabs.ext.pair.PairReportAssembler.applyFreshness(var8, var19);
         if (var12 != null) {
            var20.put("rung", var12);
         }

         if (!"fresh".equals(var19.get("status"))) {
            LinkedHashMap<java.lang.String, java.lang.String> var13 = new LinkedHashMap<>(io.karatelabs.ext.pair.PairEvidenceRepository.refreshHint(var8, Boolean.TRUE.equals(var8.get("draft"))));
            var13.put(
               "republish",
               "a page rendered from this pair while it was fresh still shows the rung it held then. Contract.report() re-renders it from today's reading. Re-render it wherever the page is published, or remove the page."
            );
            var20.put("refresh", var13);
            Object var14 = var20.get("claimSentence");
            if (var14 != null) {
               var20.put("observedClaimSentence", var14);
               var20.put(
                  "claimSentence",
                  "Withdrawn at this asOf ("
                     + var19.get("status")
                     + "). This pair no longer supports a claim, so there is no sentence to quote. What the run observed is in `observedClaimSentence`, unedited. It remains true of the run, and it does not describe this project as it is now. Re-mint (`refresh.call`) for a current claim."
               );
            }

            if (var20.get("claim") instanceof Map var15) {
               LinkedHashMap<java.lang.String, java.lang.String> var22 = new LinkedHashMap(var15);
               var22.put(
                  "standing",
                  "Observed, not standing. Every number in this block is what the run measured at mint time, reported unedited. At this asOf the evidence is "
                     + var19.get("status")
                     + ", so none of it supports a claim. See `rung` and `freshness`."
               );
               var20.put("claim", var22);
            }
         }

         var20.put("headline", io.karatelabs.ext.pair.PairReportSummaryBuilder.headline(var8, var19, var12));
         List var21 = io.karatelabs.ext.pair.PairReportSummaryBuilder.actions(var8, var19, var12);
         if (!var21.isEmpty()) {
            var20.put("actions", var21);
         }

         var20.put("evidenceFile", relativePath(var4, var7));
         if (Boolean.TRUE.equals(var8.get("draft"))) {
            var20.put(
               "draft",
               "a DRAFT — minted with {draft:true}, so it is exploratory rather than project evidence, whatever directory it is in. Re-mint without `draft` for the run you keep."
            );
         }

         if (var6.size() > 1) {
            var20.put(
               "selected",
               "the most recently minted of "
                  + var6.size()
                  + " pairs on file, by its own `mintedAt` — the others are still there. Name one to read it: Contract.read('"
                  + relativePath(var4, (Path)var6.get(1))
                  + "')."
            );
         }

         return var20;
      }
   }

   public static Object publishReport(Path var0, Path var1, Map<String, ReportAssets> var2, Object var3, Object var4) {
      io.karatelabs.ext.common.keycheck.Main.requireProduct("contract.report", "openapi");
      Object var5 = readPairReport(var0, var3, var4, "Contract.report");
      if (!(var5 instanceof Map var6)) {
         return var5;
      } else if (var6.get("error") != null) {
         return var6;
      } else {
         Path var7 = var0 != null ? var0 : Path.of(".");
         String var8 = (var4 == null && var3 instanceof Map ? var3 : var4) instanceof Map var9 ? stringOrNull(var9.get("out")) : null;
         Path var17 = var8 != null ? resolvePath(var7, var8) : (var1 != null ? var1 : var7.resolve("target/karate-reports"));

         try {
            Path var18 = var17.resolve("ext").resolve("contract");
            Files.createDirectories(var18.resolve("pages"));
            io.karatelabs.ext.common.ReportAssetsInstaller.b("META-INF/karate-ext/static/coverage.css", var18.resolve("coverage.css"));
            io.karatelabs.ext.common.ReportAssetsInstaller.b("META-INF/karate-ext/static/contract-report.css", var18.resolve("contract-report.css"));
            io.karatelabs.ext.common.ReportAssetsInstaller.b("META-INF/karate-ext/static/contract-report.js", var18.resolve("contract-report.js"));
            io.karatelabs.ext.common.ReportAssetsInstaller.b("META-INF/karate-ext/static/help-affordance.js", var18.resolve("help-affordance.js"));
            io.karatelabs.ext.common.ReportAssetsInstaller.b("META-INF/karate-ext/pages/contract.html", var17.resolve("ext/contract/pages/contract.html"));
            io.karatelabs.ext.common.ReportAssetsInstaller.a(var17);
            LinkedHashMap var11 = new LinkedHashMap(var6);
            if (var2 != null) {
               io.karatelabs.ext.common.ReportAssetsInstaller.a(var11, var2);
            } else {
               io.karatelabs.ext.common.ReportAssetsInstaller.a(var11, var17);
            }

            io.karatelabs.ext.common.ReportAssetsInstaller.a(var17.resolve("ext/contract/contract-data.js"), "KARATE_PAIR", var11);
         } catch (IOException var16) {
            throw new UncheckedIOException(var16);
         }

         Path var19 = var17.resolve("ext/contract/pages/contract.html");
         LinkedHashMap<java.lang.String, java.lang.Object> var20 = new LinkedHashMap();
         var20.put("file", var19.toString());
         Path var12 = var1 == null ? null : var1.toAbsolutePath().normalize();
         Path var13 = var19.toAbsolutePath().normalize();
         if (var12 != null && var13.startsWith(var12)) {
            var20.put("httpUrl", "/api/artifacts/reports/" + var12.relativize(var13).toString().replace('\\', '/'));
         }

         var20.put("url", var19.toUri().toString());
         var20.put("pairId", var6.get("pairId"));
         var20.put("evidenceFile", var6.get("evidenceFile"));
         if (var6.get("rung") instanceof Map var14) {
            var20.put("rung", var14.get("name"));
         }

         if (var6.get("freshness") instanceof Map var21) {
            var20.put("freshness", var21.get("status"));
         }

         return JsApi.next(
            var20,
            "Contract.read()",
            "the page is a PROJECTION of what Contract.read returns — it re-derives nothing, so the verb and the page can never disagree about what this pair claims. Publish the report dir and it travels beside the Coverage and Traceability pages; re-render after every mint, since freshness is derived at READ time and the page freezes the instant it was rendered at (`freshness.asOf` on the page says which)."
         );
      }
   }

   public static String relativePath(Path var0, Path var1) {
      Path var2 = var1.toAbsolutePath();
      Path var3 = var0.toAbsolutePath();
      return var2.startsWith(var3) ? var3.relativize(var2).toString() : var2.toString();
   }

   private static Path resolvePath(Path var0, String var1) {
      Path var2 = Path.of(var1);
      return (var2.isAbsolute() ? var2 : var0.resolve(var2)).normalize();
   }

   private static String stringOrNull(Object var0) {
      String var1 = var0 == null ? null : String.valueOf(var0);
      return var1 != null && !var1.isBlank() ? var1 : null;
   }

   private ContractReportArtifacts() {
   }
}
