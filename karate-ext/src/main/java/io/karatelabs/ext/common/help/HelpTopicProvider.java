package io.karatelabs.ext.common.help;

import io.karatelabs.common.Json;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class HelpTopicProvider {
   private final String a;
   private final String b;

   private HelpTopicProvider(String var1) {
      this.a = var1;
      this.b = "META-INF/karate/help/" + var1 + "/";
   }

   public static HelpTopicProvider a(String var0) {
      return new HelpTopicProvider(io.karatelabs.ext.common.help.ClasspathTextLoader.b(var0));
   }

   public Object a(Object... var1) {
      String var2 = var1 != null && var1.length > 0 && var1[0] != null ? String.valueOf(var1[0]) : null;
      return var2 != null && !var2.isBlank() ? this.b(var2) : this.b();
   }

   private Map<String, Object> b() {
      LinkedHashMap<java.lang.String, java.lang.Object> var1 = new LinkedHashMap();
      String var2 = io.karatelabs.ext.common.help.ClasspathTextLoader.a(this.b + "index.md");
      if (var2 == null) {
         var1.put("error", "no help index for '" + this.a + "' (" + this.b + "index.md missing)");
         return var1;
      } else {
         var1.put("markdown", var2);
         var var3 = this.a();
         if (!var3.isEmpty()) {
            var1.put("topics", var3);
            var1.put("usage", c(this.a) + ".help('topic')");
         }

         return var1;
      }
   }

   private Map<String, Object> b(String var1) {
      LinkedHashMap<java.lang.String, java.lang.Object> var2 = new LinkedHashMap();
      String var3 = io.karatelabs.ext.common.help.ClasspathTextLoader.b(var1);
      String var4 = io.karatelabs.ext.common.help.ClasspathTextLoader.a(this.b + "topics/" + var3 + ".md");
      if (var4 == null) {
         var2.put("error", "unknown topic: '" + var1 + "'. Call " + c(this.a) + ".help() to list topics.");
         var2.put("topics", this.a());
         return var2;
      } else {
         var2.put("topic", var3);
         var2.put("markdown", var4);
         return var2;
      }
   }

   public List<Map<String, Object>> a() {
      String var1 = io.karatelabs.ext.common.help.ClasspathTextLoader.a(this.b + "topics.json");
      if (var1 == null) {
         return new ArrayList<>();
      } else {
         Object var2 = Json.of(var1).value();
         if (var2 instanceof List var6) {
            return var6;
         } else {
            return (List<Map<String, Object>>)(var2 instanceof Map var3 && var3.get("topics") instanceof List var4 ? var4 : new ArrayList<>());
         }
      }
   }

   private static String c(String var0) {
      return var0.isEmpty() ? var0 : Character.toUpperCase(var0.charAt(0)) + var0.substring(1);
   }
}
