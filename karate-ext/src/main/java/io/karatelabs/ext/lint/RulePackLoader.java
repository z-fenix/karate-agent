package io.karatelabs.ext.lint;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.yaml.snakeyaml.Yaml;

public final class RulePackLoader {
   private static final String BUNDLED_RULEPACK_RESOURCE = "/io/karatelabs/ext/lint/rulepack.yaml";

   private RulePackLoader() {
   }

   public static List<LintRule> bundledRules() {
      return io.karatelabs.ext.lint.RulePackLoader.BundledRuleCatalog.BUNDLED_RULES;
   }

   public static List<LintRule> parseRules(Object var0) {
      ArrayList<io.karatelabs.ext.lint.LintRule> var1 = new ArrayList();
      if (var0 == null) {
         return var1;
      } else if (var0 instanceof Map var5) {
         for (Entry var7 : (Iterable<Entry>) (Iterable<?>) (var5.entrySet())) {
            var1.add(io.karatelabs.ext.lint.LintRule.parseRule(String.valueOf(var7.getKey()), var7.getValue()));
         }

         return var1;
      } else if (!(var0 instanceof List)) {
         throw new LintException("rules must be an {id: rule} object or a list of rules, got " + var0.getClass().getSimpleName());
      } else {
         for (Object var4 : (List)var0) {
            var1.add(io.karatelabs.ext.lint.LintRule.parseRule(null, var4));
         }

         return var1;
      }
   }

   public static List<LintRule> loadRules(boolean var0, Object var1) {
      return loadRules(var0, null, var1);
   }

   public static List<LintRule> loadRules(boolean var0, Object var1, Object var2) {
      return loadRulePack(var0, var1, var2).a();
   }

   public static io.karatelabs.ext.lint.RulePackLoader.RulePack loadRulePack(boolean var0, Object var1, Object var2) {
      LinkedHashMap var3 = new LinkedHashMap();
      LinkedHashMap var4 = new LinkedHashMap();
      if (var0) {
         bundledRules().forEach(var2x -> mergeRule(var3, var4, var2x, "bundled"));
      }

      parseRules(var1).forEach(var2x -> mergeRule(var3, var4, var2x, "project"));
      parseRules(var2).forEach(var2x -> mergeRule(var3, var4, var2x, "opts"));
      List var5 = List.copyOf(var3.values());
      return new io.karatelabs.ext.lint.RulePackLoader.RulePack(var5, io.karatelabs.ext.lint.RuleSetManifest.manifest(var5, var4));
   }

   private static void mergeRule(Map<String, LintRule> var0, Map<String, String> var1, LintRule var2, String var3) {
      var0.put(var2.b(), var2);
      var1.put(var2.b(), var3);
   }

   public static boolean usesCoverageGraph(LintRule var0) {
      return var0.r().contains("Coverage");
   }

   public record RulePack(List<LintRule> a, Map<String, Object> b) {

   }

   private static final class BundledRuleCatalog {
      static final List<LintRule> BUNDLED_RULES = loadBundledRules();

      private static List<LintRule> loadBundledRules() {
         try {
            List var2;
            try (InputStream var0 = RulePackLoader.class.getResourceAsStream("/io/karatelabs/ext/lint/rulepack.yaml")) {
               if (var0 == null) {
                  throw new LintException("the bundled rulepack resource is missing: /io/karatelabs/ext/lint/rulepack.yaml");
               }

               Object var1 = new Yaml().load(new InputStreamReader(var0, StandardCharsets.UTF_8));
               var2 = List.copyOf(io.karatelabs.ext.lint.RulePackLoader.parseRules(var1));
            }

            return var2;
         } catch (LintException var5) {
            throw var5;
         } catch (Exception var6) {
            throw new LintException("could not read the bundled rulepack: " + var6.getMessage(), var6);
         }
      }
   }
}
