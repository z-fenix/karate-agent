package io.karatelabs.ext.requirements;

import io.karatelabs.js.ObjectLike;
import java.util.Map;

public final class EvidencePolicy {
   public static final String a = "shared";
   public static final String b = "attest";
   public static final String c = "airgap";

   private EvidencePolicy() {
   }

   public static String a(String var0, Object var1) {
      return a(var0, var1, null);
   }

   public static String a(String var0, Object var1, Map<String, Object> var2) {
      String var3 = b(var0);
      io.karatelabs.ext.common.JsCallback var4 = io.karatelabs.ext.common.JsCallback.a(var1);
      if (var4 != null) {
         return a(var4.b(var3, var2), var3);
      } else if (!(var1 instanceof ObjectLike) && !(var1 instanceof Map)) {
         return var1 instanceof String var7 && !var7.isBlank() ? var7.trim().toLowerCase() : a(var3);
      } else {
         io.karatelabs.ext.common.JsCallback var5 = io.karatelabs.ext.common.JsCallback.b(var1, "resolve");
         if (var5 != null) {
            return a(var5.b(var3, var2), var3);
         } else {
            Object var6 = io.karatelabs.ext.common.JsCallback.a(var1, var3);
            if (var6 == null) {
               var6 = io.karatelabs.ext.common.JsCallback.a(var1, "default");
            }

            return var6 != null && !String.valueOf(var6).isBlank() ? String.valueOf(var6).trim().toLowerCase() : a(var3);
         }
      }
   }

   private static String a(String var0, String var1) {
      return var0 != null && !var0.isBlank() ? var0.trim().toLowerCase() : a(var1);
   }

   public static String a(String var0) {
      return "high".equals(b(var0)) ? "attest" : "shared";
   }

   public static String b(String var0) {
      if (var0 == null) {
         return "medium";
      } else {
         String var1 = var0.trim().toLowerCase();

         return switch (var1) {
            case "high", "critical" -> "high";
            case "low", "minor" -> "low";
            default -> "medium";
         };
      }
   }

   public static boolean c(String var0) {
      return "attest".equals(var0) || "airgap".equals(var0);
   }
}
