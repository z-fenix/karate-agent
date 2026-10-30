package io.karatelabs.ext.max.jsapi;

import io.karatelabs.common.Json;
import io.karatelabs.ext.common.jsapi.JsApi;
import io.karatelabs.ext.common.jsapi.JsDispatch;
import io.karatelabs.ext.common.run.JsNamespaceProvider;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class Skill extends JsDispatch {
   static final String ROOT = "META-INF/karate/skills/";
   static final String RULES = "{{rules}}";

   public Skill() {
      this.fn("flows", var1 -> this.flows());
      this.fn("list", var1 -> this.list());
      this.fn("help", var1 -> this.help(JsApi.str(var1, 0), JsApi.str(var1, 1)));
      this.fn("search", var1 -> this.search(JsApi.str(var1, 0)));
   }

   public List<Map<String, Object>> flows() {
      ArrayList<java.util.Map<java.lang.String,java.lang.Object>> var1 = new ArrayList();

      for (Map var3 : this.list()) {
         Object var4 = var3.get("flow");
         if (var4 != null && !String.valueOf(var4).isBlank()) {
            LinkedHashMap<java.lang.String, java.lang.Object> var5 = new LinkedHashMap();
            var5.put("name", var3.get("name"));
            var5.put("flow", var4);
            var5.put("topics", var3.getOrDefault("topics", List.of()));
            var1.add(var5);
         }
      }

      return var1;
   }

   public List<Map<String, Object>> list() {
      String var1 = readResource("META-INF/karate/skills/index.json");
      if (var1 == null) {
         return new ArrayList<>();
      } else {
         return (List<Map<String, Object>>)(Json.of(var1).value() instanceof Map var3 && var3.get("skills") instanceof List var4
            ? filterAvailable(var4, availableExtSkills())
            : new ArrayList<>());
      }
   }

   static List<Map<String, Object>> filterAvailable(List<Map<String, Object>> var0, Set<String> var1) {
      ArrayList<java.util.Map<java.lang.String,java.lang.Object>> var2 = new ArrayList();

      for (Map var4 : var0) {
         Object var5 = var4.get("requiresExt");
         if (var5 == null || var1.contains(String.valueOf(var5))) {
            var2.add(var4);
         }
      }

      return var2;
   }

   private static Set<String> availableExtSkills() {
      HashSet<java.lang.String> var0 = new HashSet();

      for (JsNamespaceProvider var2 : JsNamespaceProvider.discover()) {
         if (var2.skill() != null) {
            var0.add(var2.skill());
         }
      }

      return var0;
   }

   public Map<String, Object> help(String var1, String var2) {
      LinkedHashMap<java.lang.String, java.lang.Object> var3 = new LinkedHashMap();
      var3.put("name", var1);
      var3.put("topic", var2);
      if (var1 != null && !var1.isBlank()) {
         String var4 = sanitize(var1);
         String var5 = var2 != null && !var2.isBlank()
            ? "META-INF/karate/skills/" + var4 + "/topics/" + sanitize(var2) + ".md"
            : "META-INF/karate/skills/" + var4 + "/skill.md";
         String var6 = readResource(var5);
         if (var6 == null) {
            var3.put("error", "skill content not found: " + var5 + " (try Skill.list())");
            ArrayList<java.lang.String> var10 = new ArrayList();

            for (Map var9 : this.list()) {
               var10.add(String.valueOf(var9.get("name")));
            }

            JsApi.suggest(var3, io.karatelabs.ext.common.jsapi.SuggestionEngine.a(var1, var10));
            return var3;
         } else {
            var3.put("path", var5);
            if (var2 == null || var2.isBlank()) {
               var var7 = this.topicsOf(var1);
               if (!var7.isEmpty()) {
                  var3.put("topics", var7);
               }
            }

            var3.put(
               "markdown",
               var6.replace(
                  "{{rules}}",
                  "- Chain, never replay: read a value a call answered (an id, a token) from its response; never paste a recorded value where a chain or `secret` exists.\n- A credential is never in a step: it reads `secret('NAME')`; `ApiTest.config({name, secret: 'NAME', ifMatch})` sets it under each name the steps read (a migration's `needs`).\n- A test is saved only by a green run of the source saved.\n- Link a criterion only once each outcome it names is asserted (a migration's `proposed`).\n"
                     .stripTrailing()
               )
            );
            return var3;
         }
      } else {
         var3.put("error", "skill name required (try Skill.list())");
         return var3;
      }
   }

   private List<Object> topicsOf(String var1) {
      for (Map var3 : this.list()) {
         if (var1.equals(String.valueOf(var3.get("name"))) && var3.get("topics") instanceof List var4) {
            return new ArrayList<>(var4);
         }
      }

      return List.of();
   }

   public List<Map<String, Object>> search(String var1) {
      ArrayList<java.util.Map<java.lang.String,java.lang.Object>> var2 = new ArrayList();
      if (var1 != null && !var1.isBlank()) {
         ArrayList<java.lang.String> var3 = new ArrayList();

         for (String var7 : var1.toLowerCase(Locale.ROOT).split("\\s+")) {
            if (!var7.isBlank()) {
               var3.add(var7);
            }
         }

         for (Map var14 : this.list()) {
            String var15 = String.valueOf(var14.get("name"));
            String var16 = String.valueOf(var14.getOrDefault("hint", ""));
            String var8 = readResource("META-INF/karate/skills/" + sanitize(var15) + "/skill.md");
            String var9 = (var15 + " " + var14.getOrDefault("title", "") + " " + var16 + " " + (var8 == null ? "" : var8)).toLowerCase(Locale.ROOT);
            ArrayList<java.lang.String> var10 = new ArrayList();

            for (String var12 : (Iterable<String>) (Iterable<?>) (var3)) {
               if (var9.contains(var12)) {
                  var10.add(var12);
               }
            }

            if (!var10.isEmpty()) {
               int var17 = var8 == null ? -1 : var8.toLowerCase(Locale.ROOT).indexOf((String)var10.get(0));
               LinkedHashMap<java.lang.String, java.lang.Object> var18 = new LinkedHashMap();
               var18.put("name", var15);
               var18.put("title", var14.get("title"));
               var18.put("score", var10.size());
               var18.put("matched", var10);
               var18.put("offset", var17);
               var18.put("snippet", var17 >= 0 ? snippetAround(var8, var17, 80) : var16);
               var2.add(var18);
            }
         }

         var2.sort((var0, var1x) -> {
            int var2x = Integer.compare((Integer)var1x.get("score"), (Integer)var0.get("score"));
            return var2x != 0 ? var2x : String.valueOf(var0.get("name")).compareTo(String.valueOf(var1x.get("name")));
         });
         return var2;
      } else {
         return var2;
      }
   }

   private static String readResource(String var0) {
      return io.karatelabs.ext.common.help.ClasspathTextLoader.a(var0);
   }

   private static String sanitize(String var0) {
      return io.karatelabs.ext.common.help.ClasspathTextLoader.b(var0);
   }

   private static String snippetAround(String var0, int var1, int var2) {
      int var3 = Math.max(0, var1 - var2);
      int var4 = Math.min(var0.length(), var1 + var2);
      String var5 = var0.substring(var3, var4).replaceAll("\\s+", " ");
      return (var3 > 0 ? "…" : "") + var5 + (var4 < var0.length() ? "…" : "");
   }
}
