package io.karatelabs.ext.common;

import io.karatelabs.common.Json;
import io.karatelabs.core.ReportAssets;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.Map;
import java.util.Map.Entry;

public final class ReportAssetsInstaller {
   public static final String a = "META-INF/karate-ext/";
   private static final String c = "io/karatelabs/output/res/alpine.min.js";
   public static final String b = "../../../";

   private ReportAssetsInstaller() {
   }

   public static void a(String var0, Path var1) throws IOException {
      try (InputStream var2 = ReportAssetsInstaller.class.getClassLoader().getResourceAsStream(var0)) {
         if (var2 == null) {
            throw new IOException("missing report resource on classpath: " + var0);
         }

         if (!var0.startsWith("META-INF/karate-ext/") || !var0.endsWith(".js") && !var0.endsWith(".css") && !var0.endsWith(".html")) {
            Files.copy(var2, var1, StandardCopyOption.REPLACE_EXISTING);
         } else {
            String var3 = new String(var2.readAllBytes(), StandardCharsets.UTF_8);
            Files.writeString(var1, io.karatelabs.ext.common.ReportHtmlBuilder.a(var0, var3), StandardCharsets.UTF_8);
         }
      }
   }

   public static void b(String var0, Path var1) throws IOException {
      byte[] var2 = a(var0);
      if (!Files.isRegularFile(var1) || !Arrays.equals(var2, Files.readAllBytes(var1))) {
         Files.createDirectories(var1.getParent());
         Files.write(var1, var2);
      }
   }

   private static byte[] a(String var0) throws IOException {
      byte[] var3;
      try (InputStream var1 = ReportAssetsInstaller.class.getClassLoader().getResourceAsStream(var0)) {
         if (var1 == null) {
            throw new IOException("missing report resource on classpath: " + var0);
         }

         byte[] var2 = var1.readAllBytes();
         if (var0.startsWith("META-INF/karate-ext/") && (var0.endsWith(".js") || var0.endsWith(".css") || var0.endsWith(".html"))) {
            return io.karatelabs.ext.common.ReportHtmlBuilder.a(var0, new String(var2, StandardCharsets.UTF_8)).getBytes(StandardCharsets.UTF_8);
         }

         var3 = var2;
      }

      return var3;
   }

   public static void a(Path var0) throws IOException {
      Path var1 = var0.resolve("res");
      Files.createDirectories(var1);
      b("io/karatelabs/output/res/alpine.min.js", var1.resolve("alpine.min.js"));
      b("io/karatelabs/output/res/karate-report.js", var1.resolve("karate-report.js"));
   }

   public static void a(Map<String, Object> var0, Path var1) {
      a(var0, "karateSummary", var1, "karate-summary.html");
      a(var0, "coverageHref", var1, "ext/coverage/pages/coverage.html");
      a(var0, "traceabilityHref", var1, "ext/traceability/pages/traceability.html");
      a(var0, "contractHref", var1, "ext/contract/pages/contract.html");
      a(var0, "governanceHref", var1, "ext/governance/pages/governance.html");
   }

   public static void a(Map<String, Object> var0, Map<String, ReportAssets> var1) {
      var0.putIfAbsent("karateSummary", "../../../karate-summary.html");
      if (var1 != null) {
         for (Entry var3 : var1.entrySet()) {
            for (ReportAssets.Page var5 : ((ReportAssets)var3.getValue()).pages()) {
               if ("nav.pages".equals(var5.slot()) && var5.href() != null) {
                  var0.putIfAbsent((String)var3.getKey() + "Href", "../../../ext/" + (String)var3.getKey() + "/" + var5.href());
               }
            }
         }
      }
   }

   private static void a(Map<String, Object> var0, String var1, Path var2, String var3) {
      if (!var0.containsKey(var1) && Files.exists(var2.resolve(var3))) {
         var0.put(var1, "../../../" + var3);
      }
   }

   public static void a(Path var0, String var1, Object var2) throws IOException {
      Files.createDirectories(var0.getParent());
      Files.writeString(var0, "window." + var1 + " = " + Json.stringifyStrict(var2) + ";\n", StandardCharsets.UTF_8);
   }
}
