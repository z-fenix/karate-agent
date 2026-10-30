package io.karatelabs.ext.lint;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class RuleSetManifest {
   public static final String BUNDLED_SOURCE = "bundled";
   public static final String PROJECT_SOURCE = "project";
   public static final String OPTIONS_SOURCE = "opts";

   private RuleSetManifest() {
   }

   public static Map<String, Object> manifest(List<LintRule> var0, Map<String, String> var1) {
      LinkedHashMap<java.lang.String, java.lang.Object> var2 = new LinkedHashMap();
      var2.put("digest", digest(var0));
      var2.put("total", var0.size());

      for (String var4 : List.of("bundled", "project", "opts")) {
         var2.put(var4, (int)var0.stream().filter(var2x -> var4.equals(var1.get(var2x.b()))).count());
      }

      return var2;
   }

   public static String digest(List<LintRule> var0) {
      ArrayList var1 = new ArrayList();
      var0.stream().sorted(Comparator.comparing(LintRule::b)).map(LintRule::toMap).forEach(var1::add);
      return "sha256:" + io.karatelabs.ext.common.Sha256.a(io.karatelabs.ext.common.CanonicalJson.a(var1));
   }

   public static String shortDigest(Object var0) {
      String var1 = var0 == null ? "" : String.valueOf(var0);
      int var2 = var1.indexOf(58);
      String var3 = var2 < 0 ? var1 : var1.substring(var2 + 1);
      return var3.length() <= 8 ? var3 : var3.substring(0, 8);
   }
}
