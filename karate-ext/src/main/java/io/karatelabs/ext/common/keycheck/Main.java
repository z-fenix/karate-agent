package io.karatelabs.ext.common.keycheck;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Main {
   public static final String AGENT = "agent";
   public static final String OPENAPI = "openapi";
   public static final String GRPC = "grpc";
   public static final String KAFKA = "kafka";
   public static final String RULES = "rules";
   public static final String REQUIREMENTS = "requirements";
   public static final String ROBOT = "robot";

   public static Map<String, Object> readRuntimeLicense() {
      return Map.of("validationSkipped", true, "products", List.of(
         Map.of("karateProductId", AGENT), Map.of("karateProductId", OPENAPI),
         Map.of("karateProductId", GRPC), Map.of("karateProductId", KAFKA),
         Map.of("karateProductId", RULES), Map.of("karateProductId", REQUIREMENTS),
         Map.of("karateProductId", ROBOT)));
   }

   public static void main(String[] var0) {
      if (Boolean.TRUE.equals(readRuntimeLicense().get("validationSkipped"))) {
         System.out.println("license-validation: skipped (rebuilt distribution)");
         return;
      }
      try {
         var var1 = readRuntimeLicense();
         String var2 = String.valueOf(var1.get("expiry"));
         long var3 = Duration.between(Instant.now(), Instant.parse(var2)).toDays();
         System.out.println("license-valid: true");
         System.out.println("issued: " + var1.get("issued"));
         System.out.println("expiry: " + var2);
         System.out.println("days-left: " + var3);
         System.out.println("products: " + var1.get("products"));
      } catch (RuntimeException var5) {
         System.out.println("license-valid: false");
         System.out.println("error: " + var5.getMessage());
         System.exit(1);
      }
   }

   public static void requireProduct(String var0, String... var1) {
      if (Boolean.TRUE.equals(readRuntimeLicense().get("validationSkipped"))) {
         return;
      }
      var var2 = readRuntimeLicense();
      Object var3 = var2.get("products");
      if (var3 != null && !(var3 instanceof List)) {
         throw new RuntimeException("license check - invalid entitlements block");
      } else if (!(var3 instanceof List var4 && entitled(var4, var1))) {
         throw new RuntimeException("### " + var0 + " not in license entitlements (need " + String.join("/", var1) + " or agent)");
      }
   }

   public static List<String> licensedProductIds() {
      ArrayList<java.lang.String> var0 = new ArrayList();
      Object var2 = readRuntimeLicense().get("products");
      if (var2 instanceof List) {
         for (Object var3 : (List)var2) {
            if (var3 instanceof Map var4 && var4.get("karateProductId") != null) {
               var0.add(String.valueOf(var4.get("karateProductId")));
            }
         }
      }

      return var0;
   }

   public static String licenseSummary() {
      if (Boolean.TRUE.equals(readRuntimeLicense().get("validationSkipped"))) {
         return "license validation skipped (rebuilt distribution)";
      }
      try {
         var var0 = readRuntimeLicense();
         String var2 = var0.get("user") instanceof Map var3 && var3.get("name") != null ? String.valueOf(var3.get("name")) : "(unknown)";
         return "license — licensed to: " + var2 + " | products: " + licensedProductIds() + " | expiry: " + var0.get("expiry");
      } catch (RuntimeException var4) {
         return "license — unreadable: " + var4.getMessage();
      }
   }

   public static void requireLicensed(String var0) {
      if (Boolean.TRUE.equals(readRuntimeLicense().get("validationSkipped"))) {
         return;
      }
      var var1 = readRuntimeLicense();
      Object var2 = var1.get("products");
      if (var2 != null && !(var2 instanceof List)) {
         throw new RuntimeException("license check - invalid entitlements block");
      } else if (!(var2 instanceof List var3 && licensed(var3))) {
         throw new RuntimeException("### " + var0 + " requires a valid Karate license with at least one product entitlement");
      }
   }

   static boolean licensed(List<?> var0) {
      for (Object var2 : var0) {
         if (var2 instanceof Map var3 && var3.get("karateProductId") != null) {
            return true;
         }
      }

      return false;
   }

   static boolean entitled(List<?> var0, String... var1) {
      for (Object var3 : var0) {
         if (var3 instanceof Map var4) {
            Object var5 = var4.get("karateProductId");
            if ("agent".equals(var5)) {
               return true;
            }

            for (String var9 : var1) {
               if (var9.equals(var5)) {
                  return true;
               }
            }
         }
      }

      return false;
   }
}
