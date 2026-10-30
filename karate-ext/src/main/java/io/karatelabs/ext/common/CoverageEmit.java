package io.karatelabs.ext.common;

import io.karatelabs.common.Json;
import io.karatelabs.output.LogContext;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

public final class CoverageEmit {
   public static final String MIME = "application/json";
   public static final String EMIT_EMBED = "coverage-emit";

   private CoverageEmit() {
   }

   public static void emit(String var0, String var1, Map<String, Object> var2) {
      LinkedHashMap<java.lang.String, java.lang.Object> var3 = new LinkedHashMap();
      var3.put("source", var0);
      var3.put("key", var1);
      if (var2 != null) {
         var3.put("evidence", var2);
      }

      match("coverage-emit", var3);
   }

   public static void match(String var0, Map<String, Object> var1) {
      try {
         LinkedHashMap<java.lang.String, java.lang.Object> var2 = new LinkedHashMap();
         var2.put("name", var0);
         var2.put("payload", var1);
         if (io.karatelabs.ext.common.EvidenceContext.a("emit", var2)) {
            return;
         }

         LogContext var3 = io.karatelabs.ext.common.EvidenceContext.c();
         if (var3 == null) {
            io.karatelabs.ext.common.EvidenceContext.d();
            return;
         }

         var3.embed(Json.stringifyStrict(var1).getBytes(StandardCharsets.UTF_8), "application/json", var0);
      } catch (Exception var4) {
      }
   }
}
