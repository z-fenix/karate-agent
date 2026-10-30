package io.karatelabs.ext.max.jsapi;

import io.karatelabs.ext.common.ExtMeta;
import io.karatelabs.ext.common.jsapi.JsApi;
import io.karatelabs.ext.common.jsapi.JsDispatch;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

public class Settings extends JsDispatch {
   private static final List<String> SECRET_ANYWHERE = List.of(
      "PASSWORD", "PASSWD", "PWD", "PASSPHRASE", "SECRET", "TOKEN", "CREDENTIAL", "APIKEY", "PRIVATEKEY", "ACCESSKEY"
   );
   private static final Set<String> SECRET_LAST_SEGMENT = Set.of("KEY", "KEYS", "PAT", "SIGNATURE");
   static final String CONSOLE_VIEW_ENV = "KARATE_CONSOLE_VIEW";

   public Settings() {
      this.fn("help", var0 -> io.karatelabs.ext.common.help.HelpTopicProvider.a("settings").a(var0));
      this.fn("view", var1 -> this.view());
      this.fn("version", var1 -> this.version());
      this.fn("git", var1 -> this.git());
      this.fn("license", var1 -> this.license());
      this.fn("llm", var1 -> this.llm());
      this.fn("consoleView", var1 -> this.consoleView());
      this.fn("sysenv", var1 -> this.sysenv(JsApi.str(var1, 0), JsApi.str(var1, 1)));
      this.fn("sysprop", var1 -> this.sysprop(JsApi.str(var1, 0), JsApi.str(var1, 1)));
   }

   Object sysenv(String var1, String var2) {
      return this.read("sysenv", var1, var2, System::getenv);
   }

   Object sysprop(String var1, String var2) {
      return this.read("sysprop", var1, var2, System::getProperty);
   }

   private Object read(String var1, String var2, String var3, Function<String, String> var4) {
      if (var2 == null || var2.isBlank()) {
         return JsApi.missing("Settings." + var1 + "(name[, default]) needs a name, e.g. Settings." + var1 + "('PROVIDER_URL', 'http://localhost:8080')");
      } else if (secretShaped(var2)) {
         return JsApi.missing(
            "Settings."
               + var1
               + "('"
               + var2
               + "'): withheld — a secret-shaped name is never read back through this surface (Settings.llm() reports key PRESENCE, masked). A suite or karate-boot.js reads a credential directly from the process."
         );
      } else {
         String var5 = (String)var4.apply(var2);
         return var5 != null && !var5.isEmpty() ? var5 : var3;
      }
   }

   public static boolean secretShaped(String var0) {
      String var1 = var0.toUpperCase(Locale.ROOT);

      for (String var3 : SECRET_ANYWHERE) {
         if (var1.contains(var3)) {
            return true;
         }
      }

      String[] var4 = var1.split("[_.\\-]");
      return SECRET_LAST_SEGMENT.contains(var4[var4.length - 1]);
   }

   String consoleView() {
      return consoleView(System::getenv);
   }

   static String consoleView(Function<String, String> var0) {
      String var1 = (String)var0.apply("KARATE_CONSOLE_VIEW");
      return var1 != null && "manual".equals(var1.trim().toLowerCase(Locale.ROOT)) ? "manual" : "dev";
   }

   Map<String, Object> view() {
      LinkedHashMap<java.lang.String, java.lang.Object> var1 = new LinkedHashMap();
      var1.put("version", this.version());
      var1.put("git", this.git());
      var1.put("license", this.license());
      var1.put("llm", this.llm());
      var1.put("consoleView", this.consoleView());
      return var1;
   }

   String version() {
      return ExtMeta.version("karate-max-meta.properties");
   }

   String git() {
      return ExtMeta.property("karate-max-meta.properties", "git");
   }

   Map<String, Object> license() {
      LinkedHashMap<java.lang.String, java.lang.Object> var1 = new LinkedHashMap();

      Map var2;
      try {
         var2 = io.karatelabs.ext.common.keycheck.Main.readRuntimeLicense();
      } catch (RuntimeException var10) {
         var1.put("valid", false);
         var1.put("reason", var10.getMessage());
         return var1;
      }

      if (Boolean.TRUE.equals(var2.get("validationSkipped"))) {
         var1.put("valid", false);
         var1.put("validationSkipped", true);
         var1.put("reason", "License validation skipped in rebuilt distribution");
         return var1;
      }
      var1.put("valid", true);
      String var3 = licensee(var2);
      if (var3 != null) {
         var1.put("licensee", var3);
      }

      if (var2.get("user") instanceof Map var5) {
         Object var6 = var5.get("email");
         if (var6 != null && !String.valueOf(var6).isBlank()) {
            var1.put("email", String.valueOf(var6));
         }

         Object var7 = var5.get("daysLeft");
         if (var7 != null) {
            var1.put("daysLeft", var7);
         }
      }

      if (var2.get("expiry") != null) {
         var1.put("expiry", String.valueOf(var2.get("expiry")));
      }

      ArrayList<java.lang.String> var11 = new ArrayList();
      Object var13 = var2.get("products");
      if (var13 instanceof List) {
         for (Object var8 : (List)var13) {
            if (var8 instanceof Map var9 && var9.get("karateProductId") != null) {
               var11.add(String.valueOf(var9.get("karateProductId")));
            }
         }
      }

      var1.put("products", var11);
      return var1;
   }

   Map<String, Object> llm() {
      LinkedHashMap<java.lang.String, java.lang.Object> var1 = new LinkedHashMap();
      ArrayList<java.util.LinkedHashMap> var2 = new ArrayList();
      boolean var3 = false;

      for (io.karatelabs.ext.agent.llm.LlmProvider var7 : io.karatelabs.ext.agent.llm.LlmProvider.values()) {
         LinkedHashMap<java.lang.String, java.lang.Object> var8 = new LinkedHashMap();
         String var9 = var7.getApiKeyFromEnv();
         boolean var10 = var9 != null && !var9.isBlank();
         var3 = var3 || var10;
         var8.put("name", var7.name().toLowerCase());
         var8.put("envVar", var7.getEnvVar());
         var8.put("present", var10);
         String var11 = var7.resolveEndpoint(System::getenv);
         var8.put("endpoint", var11);
         var8.put("endpointOverridden", !var11.equals(var7.getDefaultEndpoint()));
         var2.add(var8);
      }

      var1.put("configured", var3);
      var1.put("providers", var2);
      return var1;
   }

   static String licensee(Map<String, Object> var0) {
      return var0 != null && var0.get("user") instanceof Map var1 ? firstNonBlank(var1, "name", "company", "organization", "org", "to", "email") : null;
   }

   private static String firstNonBlank(Map<?, ?> var0, String... var1) {
      for (String var5 : var1) {
         Object var6 = var0.get(var5);
         if (var6 != null && !String.valueOf(var6).isBlank()) {
            return String.valueOf(var6);
         }
      }

      return null;
   }
}
