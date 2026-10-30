package io.karatelabs.ext.coverage.collect;

import io.karatelabs.common.Json;
import io.karatelabs.ext.common.MaxPaths;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public final class EvidenceRunMetadata {
   public static final String a = "evidence/1";
   public static final String b = "scenario";
   public static final String c = "check";
   public static final String d = "session";
   public static final int e = 0;
   private final String g;
   private final Map<String, Object> h;
   public static final long f = 600000L;

   private EvidenceRunMetadata(String var1, Map<String, Object> var2) {
      this.g = var1;
      this.h = var2;
   }

   public static EvidenceRunMetadata a(Path var0) {
      Path var1 = var0.toAbsolutePath().getParent();
      Path var2 = var1 == null ? null : var1.getParent();
      return var2 == null ? null : b(var2);
   }

   public static EvidenceRunMetadata b(Path var0) {
      Path var1 = var0.resolve("meta.json");
      if (!Files.isRegularFile(var1)) {
         return null;
      } else {
         try {
            return Json.of(Files.readString(var1)).value() instanceof Map var3 ? new EvidenceRunMetadata(var0.getFileName().toString(), var3) : null;
         } catch (Exception var4) {
            return null;
         }
      }
   }

   public String a() {
      return this.g;
   }

   public Map<String, Object> b() {
      return this.h;
   }

   public boolean c() {
      return "evidence/1".equals(this.h.get("protocol"));
   }

   public boolean d() {
      return Boolean.TRUE.equals(this.h.get("evidenceFinalized"));
   }

   public String e() {
      return a(this.h.get("status"));
   }

   public boolean c(Path var1) {
      if (!"running".equals(this.e())) {
         return false;
      } else {
         long var2 = -1L;

         for (Path var5 : List.of(var1.resolve("meta.json"), var1.resolve("transcript.jsonl"), MaxPaths.eventsFile(var1))) {
            try {
               if (var5 != null && Files.isRegularFile(var5)) {
                  var2 = Math.max(var2, Files.getLastModifiedTime(var5).toMillis());
               }
            } catch (IOException var7) {
            }
         }

         return var2 >= 0L && System.currentTimeMillis() - var2 < 600000L;
      }
   }

   public String f() {
      return a(this.h.get("mode"));
   }

   public String g() {
      return a(this.h.get("check"));
   }

   public static String a(String var0, int var1) {
      return var0 + "#" + var1;
   }

   public static String a(String var0) {
      String var1 = MaxPaths.stripLeadingSlashes(var0.trim()).replace('\\', '/');
      var1 = var1.startsWith("checks/") ? var1.substring("checks/".length()) : var1;
      var1 = var1.endsWith(".js") ? var1 : var1 + ".js";
      return "check:checks/" + var1;
   }

   public String d(Path var1) {
      return Files.isRegularFile(var1.resolve("karate-summary.html")) ? this.g + "/karate-summary.html" : null;
   }

   public static String b(String var0, int var1) {
      return var0 + "#s" + var1;
   }

   private static String a(Object var0) {
      return var0 == null ? null : String.valueOf(var0);
   }
}
