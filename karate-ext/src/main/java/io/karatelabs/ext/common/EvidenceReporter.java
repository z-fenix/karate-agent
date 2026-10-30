package io.karatelabs.ext.common;

import io.karatelabs.common.Json;
import io.karatelabs.core.StepResult;
import io.karatelabs.output.LogContext;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class EvidenceReporter {
   public static final String a = "application/json";
   public static final String b = "evidence";

   private EvidenceReporter() {
   }

   public static void a(Map<String, Object> var0) {
      try {
         if (io.karatelabs.ext.common.EvidenceContext.a("assertion", var0)) {
            return;
         }

         LogContext var1 = io.karatelabs.ext.common.EvidenceContext.c();
         if (var1 == null) {
            io.karatelabs.ext.common.EvidenceContext.d();
            return;
         }

         var1.embed(Json.stringifyStrict(var0).getBytes(StandardCharsets.UTF_8), "application/json", "evidence");
      } catch (Exception var2) {
      }
   }

   public static void b(Map<String, Object> var0) {
      try {
         if (io.karatelabs.ext.common.EvidenceContext.a("assertion", var0)) {
            return;
         }

         LogContext var1 = io.karatelabs.ext.common.EvidenceContext.c();
         if (var1 == null) {
            io.karatelabs.ext.common.EvidenceContext.d();
            return;
         }

         StepResult.Embed var2 = new StepResult.Embed(Json.stringifyStrict(var0).getBytes(StandardCharsets.UTF_8), "application/json", "evidence");
         Object var3 = var0.get("label");
         boolean var4 = Boolean.TRUE.equals(var0.get("pass"));
         if (var3 != null && !String.valueOf(var3).isBlank()) {
            var1.step(String.valueOf(var3), var4, List.of(var2));
         } else {
            var1.embed(var2);
         }
      } catch (Exception var5) {
      }
   }

   public static void a(String var0, String var1, String var2) {
      try {
         String var3 = var0 != null && !var0.isBlank() ? var0 : "step";
         LinkedHashMap var4 = new LinkedHashMap();
         var4.put("name", var3);
         if (var1 != null) {
            var4.put("status", var1);
         }

         if (var2 != null) {
            var4.put("note", var2);
         }

         if (io.karatelabs.ext.common.EvidenceContext.a("step", var4)) {
            return;
         }

         LogContext var5 = io.karatelabs.ext.common.EvidenceContext.c();
         if (var5 == null) {
            io.karatelabs.ext.common.EvidenceContext.d();
            return;
         }

         StepResult var6 = var5.step(var3, !"fail".equalsIgnoreCase(var1));
         if (var2 != null && !var2.isBlank()) {
            var6.setLog(var2);
         }
      } catch (Exception var7) {
      }
   }

   public static void a(String var0, String var1) {
      try {
         String var2 = var1 != null && !var1.isBlank() ? var1 : "screenshot";
         LinkedHashMap var3 = new LinkedHashMap();
         var3.put("title", var2);
         var3.put("file", var0);
         if (io.karatelabs.ext.common.EvidenceContext.a("screenshot", var3)) {
            return;
         }

         LogContext var4 = io.karatelabs.ext.common.EvidenceContext.c();
         if (var4 == null) {
            io.karatelabs.ext.common.EvidenceContext.d();
            return;
         }

         StepResult.Embed var5 = new StepResult.Embed(var2, List.of(new StepResult.Part("primary", "image/png", var0)), null);
         var4.step(var2, true, List.of(var5));
      } catch (Exception var6) {
      }
   }
}
