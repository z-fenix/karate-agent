package io.karatelabs.ext.requirements;

import io.karatelabs.common.Json;
import io.karatelabs.core.Ext;
import io.karatelabs.core.RunEvent;
import io.karatelabs.core.RunEventType;
import io.karatelabs.core.Suite;
import io.karatelabs.ext.common.ExtMeta;
import io.karatelabs.ext.common.MaxPaths;
import io.karatelabs.ext.requirements.jsapi.Requirement;
import io.karatelabs.js.SimpleObject;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RequirementsExt implements Ext, SimpleObject {
   private static final Logger logger = LoggerFactory.getLogger(RequirementsExt.class);
   static final String VERSION = ExtMeta.version("karate-max-meta.properties");
   static final String DEFAULT_HOME = "requirements";
   private Object home;
   private List<Path> resolvedHomes = List.of();
   private Path reportDir;
   private Object provider;

   @Override
   public Object jsGet(String var1) {
      return switch (var1) {
         case "home" -> this.home;
         case "provider" -> this.provider;
         default -> null;
      };
   }

   @Override
   public void putMember(String var1, Object var2) {
      switch (var1) {
         case "home":
            this.home = var2;
            break;
         case "provider":
            this.provider = var2;
            break;
         default:
            throw new IllegalArgumentException("unknown requirements config key: req." + var1 + " (known: home, provider)");
      }
   }

   @Override
   public Collection<String> jsKeys() {
      return List.of("home", "provider");
   }

   public Object getHome() {
      return this.home;
   }

   @Override
   public void onBoot(Suite var1) {
      io.karatelabs.ext.common.keycheck.Main.requireProduct("requirements", "requirements");
      this.resolvedHomes = resolveHomes(var1.getRoot(), this.home, "requirements");
      this.reportDir = Path.of(var1.getOutputDir());
      var1.registerGlobal("Requirement", Requirement.atReportDir(this.reportDir).withRequirementsHomes(this.resolvedHomes).withProjectRoot(var1.getRoot()));
      logger.info("requirements ext booted (home={})", this.resolvedHomes);
   }

   @Override
   public boolean onEvent(RunEvent var1) {
      if (var1.getType() == RunEventType.SUITE_ENTER) {
         this.writeProviderLinks(this.reportDir);
      }

      return true;
   }

   private void writeProviderLinks(Path var1) {
      writeProviderLinks(var1, this.provider);
   }

   public Object provider() {
      return this.provider;
   }

   public static void writeProviderLinks(Path var0, Object var1) {
      var var2 = resolveProviderLinks(var1);
      if (!var2.isEmpty() && var0 != null) {
         try {
            Path var3 = var0.resolve("ext").resolve("requirements");
            Files.createDirectories(var3);
            LinkedHashMap<java.lang.String, java.util.Map<java.lang.String,java.lang.String>> var4 = new LinkedHashMap();
            var4.put("links", var2);
            Files.writeString(var3.resolve("provider.json"), Json.stringifyStrict(var4));
         } catch (Exception var5) {
            logger.warn("could not write requirements provider links: {}", var5.getMessage());
         }
      }
   }

   static Map<String, String> resolveProviderLinks(Object var0) {
      if (!(var0 instanceof Map var1)) {
         return Map.of();
      } else {
         String var2 = str(var1.get("urlTemplate"));
         if (var2 == null) {
            String var3 = str(var1.get("system"));
            if (var3 == null) {
               logger.warn("requirements.provider has no 'system' or 'urlTemplate' — no tracker links");
               return Map.of();
            }

            String var4 = str(var1.get("org"));
            String var5 = str(var1.get("project"));
            String var6 = var3.toLowerCase();
            switch (var6) {
               case "ado":
               case "azure-devops":
               case "azuredevops":
                  if (var4 == null || var5 == null) {
                     logger.warn("requirements.provider system '{}' needs org + project — no tracker links", var3);
                     return Map.of();
                  }

                  var2 = "https://dev.azure.com/" + var4 + "/" + var5 + "/_workitems/edit/{id}";
                  break;
               case "jira":
                  String var16 = str(var1.get("host"));
                  if (var16 == null) {
                     logger.warn("requirements.provider system 'jira' needs 'host' — no tracker links");
                     return Map.of();
                  }

                  var2 = var16.replaceAll("/+$", "") + "/browse/{id}";
                  break;
               case "git":
                  String var8 = str(var1.get("repoUrl"));
                  if (var8 == null) {
                     logger.warn("requirements.provider system 'git' needs 'repoUrl' — no tracker links");
                     return Map.of();
                  }

                  String var9 = str(var1.get("branch"));
                  if (var9 == null || var9.isBlank()) {
                     var9 = "main";
                  }

                  var8 = var8.replaceAll("/+$", "");
                  String var10 = "";
                  String var11 = str(var1.get("basePath"));
                  if (var11 != null && !var11.isBlank()) {
                     var10 = var11.replaceAll("^/+", "").replaceAll("/+$", "") + "/";
                  }

                  String var12 = str(var1.get("host"));
                  if (var12 == null) {
                     var12 = inferGitHost(var8);
                  }

                  String var13 = var12.toLowerCase();
                  switch (var13) {
                     case "gitlab":
                        var2 = var8 + "/-/blob/" + var9 + "/" + var10 + "{file}#{anchor}";
                        return Map.of("req", var2);
                     case "bitbucket":
                        var2 = var8 + "/src/" + var9 + "/" + var10 + "{file}#{anchor}";
                        return Map.of("req", var2);
                     case "azure":
                     case "azure-repos":
                     case "azuredevops":
                        var2 = var8 + "?path=/" + var10 + "{file}&version=GB" + var9;
                        return Map.of("req", var2);
                     default:
                        var2 = var8 + "/blob/" + var9 + "/" + var10 + "{file}#{anchor}";
                        return Map.of("req", var2);
                  }
               default:
                  logger.warn("unknown requirements.provider system '{}' — no tracker links", var3);
                  return Map.of();
            }
         }

         return Map.of("req", var2);
      }
   }

   private static String inferGitHost(String var0) {
      String var1 = var0.toLowerCase();
      if (var1.contains("gitlab")) {
         return "gitlab";
      } else if (var1.contains("bitbucket")) {
         return "bitbucket";
      } else {
         return !var1.contains("dev.azure.com") && !var1.contains(".visualstudio.com") ? "github" : "azure";
      }
   }

   private static String str(Object var0) {
      return var0 == null ? null : String.valueOf(var0);
   }

   @Override
   public Map<String, Object> getManifest() {
      LinkedHashMap<java.lang.String, java.lang.Object> var1 = new LinkedHashMap();
      var1.put("version", VERSION);
      if (!this.resolvedHomes.isEmpty()) {
         var1.put("home", this.resolvedHomes.get(0).toString());
         if (this.resolvedHomes.size() > 1) {
            var1.put("homes", this.resolvedHomes.stream().map(Path::toString).toList());
         }
      }

      return var1;
   }

   // $VF: One or more variable merging failures!
   // $VF: Could not properly define all variable types!
   // Please report this to the Vineflower issue tracker, at https://github.com/Vineflower/vineflower/issues with a copy of the class file (if you have the rights to distribute it!)
   public static List<Path> resolveHomes(Path var0, Object var1, String var2) {
      ArrayList<java.nio.file.Path> var3 = new ArrayList();

      for (Object var7 : var1 instanceof Collection ? (Collection)var1 : var1 == null ? List.of("") : List.of(var1)) {
         String var6 = var7 == null ? "" : String.valueOf(var7);
         if (!var6.isBlank()) {
            var3.add(MaxPaths.resolvePath(var0, var6));
         }
      }

      return (List<Path>)(var3.isEmpty() ? List.of(MaxPaths.resolvePath(var0, var2)) : var3);
   }
}
