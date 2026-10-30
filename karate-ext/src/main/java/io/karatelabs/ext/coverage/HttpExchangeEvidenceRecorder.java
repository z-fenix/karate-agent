package io.karatelabs.ext.coverage;

import io.karatelabs.common.Json;
import io.karatelabs.core.HttpRunEvent;
import io.karatelabs.core.RunEventType;
import io.karatelabs.core.RunListener;
import io.karatelabs.core.ScenarioRuntime;
import io.karatelabs.output.LogContext;
import io.karatelabs.output.LogMask;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HttpExchangeEvidenceRecorder {
   private static final Logger a = LoggerFactory.getLogger(HttpExchangeEvidenceRecorder.class);

   private HttpExchangeEvidenceRecorder() {
   }

   public static RunListener embeddingListener() {
      return var0 -> {
         if (var0 instanceof HttpRunEvent var1 && var1.type() == RunEventType.HTTP_EXIT) {
            try {
               LogContext.get().embed(Json.stringifyStrict(a(var1)).getBytes(), "application/json", "http-exchange");
            } catch (Exception var3) {
               a.debug("Failed to capture HTTP event: {}", var3.getMessage());
            }
         }

         return true;
      };
   }

   public static Map<String, Object> a(HttpRunEvent var0) {
      LogMask var1 = LogContext.get().getMask();
      if (var1 != null && var0.request() != null && !var1.enabledForUri(var0.request().getUrlAndPath())) {
         var1 = null;
      }

      LinkedHashMap var2 = new LinkedHashMap();
      if (var0.request() != null) {
         var2.put("request", a(var0.request().toMap(), var1));
      }

      if (var0.response() != null) {
         var2.put("response", a(var0.response().toMap(), var1));
      }

      return var2;
   }

   public static RunListener a(Consumer<Map<String, Object>> var0) {
      return var1 -> {
         if (var1 instanceof HttpRunEvent var2 && var2.type() == RunEventType.HTTP_EXIT) {
            LinkedHashMap var3 = new LinkedHashMap();
            if (var2.request() != null) {
               var3.put("request", var2.request().toMap());
            }

            if (var2.response() != null) {
               var3.put("response", var2.response().toMap());
            }

            try {
               var0.accept(var3);
            } catch (Exception var5) {
               a.warn("Failed to pass an HTTP exchange on: {}", var5.getMessage());
            }
         }

         return true;
      };
   }

   private static Map<String, Object> a(Map<String, Object> var0, LogMask var1) {
      if (var1 == null) {
         return var0;
      } else {
         if (var0.get("headers") instanceof Map var2) {
            LinkedHashMap var7 = new LinkedHashMap();
            var2.forEach(
               (var2x, var3) -> {
                  String var4x = String.valueOf(var2x);
                  var7.put(
                     var4x,
                     var3 instanceof List var5x
                        ? var5x.stream().map(var2xx -> var1.maskHeader(var4x, String.valueOf(var2xx))).toList()
                        : (var3 == null ? null : var1.maskHeader(var4x, String.valueOf(var3)))
                  );
               }
            );
            var0.put("headers", var7);
         }

         Object var6 = var0.get("body");
         if (var6 instanceof String var8) {
            var0.put("body", var1.maskBody(var8));
         } else if (var6 instanceof Map || var6 instanceof List) {
            String var4 = Json.stringifyStrict(var6);
            String var5 = var1.maskBody(var4);
            if (!var5.equals(var4)) {
               var0.put("body", Json.parseLenient(var5));
            }
         }

         return var0;
      }
   }

   public static RunListener nonCoverageListener() {
      RunListener var0 = embeddingListener();
      return var1 -> {
         if (var1 instanceof HttpRunEvent var2 && !a(var2.scenarioRuntime())) {
            var0.onEvent(var1);
         }

         return true;
      };
   }

   private static boolean a(ScenarioRuntime var0) {
      return var0 != null
            && var0.getFeatureRuntime() != null
            && var0.getFeatureRuntime().getSuite() != null
            && var0.getFeatureRuntime().getSuite().getBootBinding() != null
         ? var0.getFeatureRuntime().getSuite().getBootBinding().getExts().stream().anyMatch(CoverageExt.class::isInstance)
         : false;
   }
}
