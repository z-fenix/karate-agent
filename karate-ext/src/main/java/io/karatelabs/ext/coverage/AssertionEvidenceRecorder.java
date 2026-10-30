package io.karatelabs.ext.coverage;

import io.karatelabs.common.Json;
import io.karatelabs.core.MatchRunEvent;
import io.karatelabs.core.RunEventType;
import io.karatelabs.core.RunListener;
import io.karatelabs.ext.robot.MatchBinding;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

public class AssertionEvidenceRecorder {
   private static final int a = 8192;

   private AssertionEvidenceRecorder() {
   }

   public static RunListener listener() {
      return var0 -> {
         if (var0 instanceof MatchRunEvent var1 && var1.type() == RunEventType.MATCH_EXIT) {
            boolean var2 = var1.result() != null && var1.result().pass;
            LinkedHashMap<java.lang.String, java.lang.Object> var3 = new LinkedHashMap();
            var3.put("kind", "assertion");
            var3.put("source", "step");
            var3.put("verb", var1.matchType() == null ? "assert" : MatchBinding.verb(var1.matchType()));
            if (var1.actualExpr() != null) {
               var3.put("actualExpr", var1.actualExpr());
            }

            if (var1.expectedExpr() != null) {
               var3.put("expectedExpr", var1.expectedExpr());
            }

            a(var3, "expected", var1.expected());
            if (!var2) {
               a(var3, "actual", var1.actual());
            }

            var3.put("pass", var2);
            var3.put("message", var1.result() == null ? null : var1.result().message);
            io.karatelabs.ext.common.EvidenceReporter.b(var3);
         }

         return true;
      };
   }

   public static Map<String, Object> a(Map<String, Object> var0) {
      boolean var1 = Boolean.TRUE.equals(var0.get("pass"));
      LinkedHashMap<java.lang.String, java.lang.Object> var2 = new LinkedHashMap();

      for (Entry var4 : var0.entrySet()) {
         String var5 = (String)var4.getKey();
         switch (var5) {
            case "expected":
               a(var2, "expected", var4.getValue());
               break;
            case "actual":
               if (!var1) {
                  a(var2, "actual", var4.getValue());
               }
               break;
            default:
               var2.put((String)var4.getKey(), var4.getValue());
         }
      }

      return var2;
   }

   private static void a(Map<String, Object> var0, String var1, Object var2) {
      int var3;
      try {
         String var4 = Json.stringifyStrict(Collections.singletonList(var2));
         var3 = var4.getBytes(StandardCharsets.UTF_8).length - 2;
      } catch (Exception var5) {
         var0.put(var1 + "Truncated", -1);
         return;
      }

      if (var3 > 8192) {
         var0.put(var1 + "Truncated", var3);
      } else {
         var0.put(var1, var2);
      }
   }
}
