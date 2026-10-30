package io.karatelabs.ext.max.jsapi;

import io.karatelabs.common.Json;
import io.karatelabs.ext.common.ExtMeta;
import io.karatelabs.ext.common.jsapi.JsApi;
import io.karatelabs.ext.common.jsapi.JsDispatch;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Help extends JsDispatch {
   static final String ROOT = "META-INF/karate/help/pages/";
   private final Supplier<Map<String, Object>> license;
   private final boolean figures;
   private static final Pattern SVG = Pattern.compile("(?:<!--diagram:[\\w-]+-->\\n?)?<svg\\b([^>]*)>.*?</svg>(?:\\n?<!--/diagram-->)?", 32);
   private static final Pattern HEADLINE = Pattern.compile("<text[^>]*font-size=\"22\"[^>]*>([^<]*)</text>");

   public Help() {
      this(io.karatelabs.ext.common.keycheck.Main::readRuntimeLicense, false);
   }

   Help(Supplier<Map<String, Object>> var1) {
      this(var1, false);
   }

   private Help(Supplier<Map<String, Object>> var1, boolean var2) {
      this.license = var1;
      this.figures = var2;
      this.fn("pages", var1x -> this.pages());
      this.fn("topics", var1x -> this.topics());
      this.fn("page", var1x -> this.page(JsApi.str(var1x, 0)));
      this.fn("get", var1x -> this.get(JsApi.str(var1x, 0)));
      this.fn("surface", var1x -> this.surface(JsApi.str(var1x, 0)));
      this.fn("search", var1x -> this.search(JsApi.str(var1x, 0)));
      this.fn("help", var0 -> JsApi.helpTopic(help(), var0));
   }

   private static Map<String, Object> help() {
      return JsApi.markdown(
         "# Help — the product Help pages and datasheet\n\n- `Help.pages()` lists pages; `Help.page('rules' | 'datasheet')` returns one as markdown.\n- `Help.topics()` is the help-id index; `Help.get(id)` resolves an id to its page, anchor and\n  `/help#id` url; `Help.search(query)` scans ids and page text.\n- `Help.surface(id)` opens `/help#id` in the human's view.\n- A miss returns `{error}` — it never throws."
      );
   }

   public Help forConsole() {
      return new Help(this.license, true);
   }

   public List<Map<String, Object>> pages() {
      return manifestList("pages");
   }

   public List<Map<String, Object>> topics() {
      return manifestList("ids");
   }

   public Map<String, Object> page(String var1) {
      LinkedHashMap<java.lang.String, java.lang.Object> var2 = new LinkedHashMap();
      var2.put("id", var1);
      if (var1 != null && !var1.isBlank()) {
         var var3 = pageMeta(var1);
         String var4 = this.markdown(var1, var3);
         if (var4 == null) {
            var2.put("error", "help page not found: " + var1);
            JsApi.suggest(var2, io.karatelabs.ext.common.jsapi.SuggestionEngine.a(var1, pageIds()));
            return var2;
         } else {
            var2.put("title", var3 != null ? var3.get("title") : var1);
            if (!this.figures) {
               var4 = captionFigures(var4);
            }

            var2.put("markdown", io.karatelabs.ext.common.help.ClasspathTextLoader.a(var4, Map.of("VERSION", ExtMeta.version("karate-max-meta.properties"))));
            String var5 = this.licensee();
            if (var5 != null) {
               var2.put("licensedTo", var5);
            }

            return var2;
         }
      } else {
         var2.put("error", "page id required (try Help.pages())");
         return var2;
      }
   }

   static String captionFigures(String var0) {
      Matcher var1 = SVG.matcher(var0);
      StringBuilder var2 = new StringBuilder();

      while (var1.find()) {
         String var3 = "";
         if (var1.group(1).contains("k-diagram")) {
            Matcher var4 = HEADLINE.matcher(var1.group());
            var3 = "*Figure" + (var4.find() ? ": " + unescape(var4.group(1).trim()) : "") + "*";
         }

         var1.appendReplacement(var2, Matcher.quoteReplacement(var3));
      }

      var1.appendTail(var2);
      return var2.toString();
   }

   private static String unescape(String var0) {
      return var0.replace("&lt;", "<").replace("&gt;", ">").replace("&quot;", "\"").replace("&#39;", "'").replace("&amp;", "&");
   }

   private String markdown(String var1, Map<String, Object> var2) {
      String var3 = var2 != null ? String.valueOf(var2.get("file")) : sanitize(var1) + ".md";
      return io.karatelabs.ext.common.help.ClasspathTextLoader.a("META-INF/karate/help/pages/" + var3);
   }

   private String licensee() {
      try {
         return Settings.licensee(this.license.get());
      } catch (RuntimeException var2) {
         return null;
      }
   }

   public Map<String, Object> get(String var1) {
      LinkedHashMap<java.lang.String, java.lang.Object> var2 = new LinkedHashMap();
      var2.put("id", var1);
      var var3 = resolve(var1);
      if (var3 == null) {
         var2.put("found", false);
         var2.put("error", "no help for id: " + var1);
         JsApi.suggest(var2, io.karatelabs.ext.common.jsapi.SuggestionEngine.a(var1, allIds()));
         return var2;
      } else {
         String var4 = String.valueOf(var3.get("page"));
         String var5 = String.valueOf(var3.get("anchor"));
         var2.put("found", true);
         var2.put("page", var4);
         var2.put("anchor", var5);
         var2.put("title", var3.get("title"));
         var2.put("section", var3.get("section"));
         var2.put("url", "/help#" + var5);
         var var6 = this.page(var4);
         if (var6.get("markdown") != null) {
            var2.put("markdown", var6.get("markdown"));
         }

         return var2;
      }
   }

   public Map<String, Object> surface(String var1) {
      LinkedHashMap<java.lang.String, java.lang.Object> var2 = new LinkedHashMap();
      var2.put("command", "surface");
      var2.put("id", var1);
      var var3 = resolve(var1);
      if (var3 == null) {
         var2.put("found", false);
         var2.put("url", "/help");
         var2.put("error", "no help for id: " + var1);
         JsApi.suggest(var2, io.karatelabs.ext.common.jsapi.SuggestionEngine.a(var1, allIds()));
         return var2;
      } else {
         var2.put("found", true);
         var2.put("page", var3.get("page"));
         var2.put("anchor", var3.get("anchor"));
         var2.put("title", var3.get("title"));
         var2.put("url", "/help#" + var3.get("anchor"));
         return var2;
      }
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

         LinkedHashMap<java.lang.String, java.lang.Object> var12 = new LinkedHashMap();

         for (Map var15 : this.pages()) {
            String var17 = String.valueOf(var15.get("id"));
            String var8 = this.markdown(var17, pageMeta(var17));
            var12.put(var17, var8 != null ? var8 : "");
         }

         for (Map var16 : this.topics()) {
            String var18 = String.valueOf(var16.get("page"));
            String var19 = (var16.get("id") + " " + var16.get("title") + " " + var16.get("section") + " " + var12.getOrDefault(var18, ""))
               .toLowerCase(Locale.ROOT);
            ArrayList<java.lang.String> var9 = new ArrayList();

            for (String var11 : (Iterable<String>) (Iterable<?>) (var3)) {
               if (var19.contains(var11)) {
                  var9.add(var11);
               }
            }

            if (!var9.isEmpty()) {
               LinkedHashMap<java.lang.String, java.lang.Object> var20 = new LinkedHashMap();
               var20.put("id", var16.get("id"));
               var20.put("title", var16.get("title"));
               var20.put("page", var18);
               var20.put("anchor", var16.get("anchor"));
               var20.put("url", "/help#" + var16.get("anchor"));
               var20.put("score", var9.size());
               var2.add(var20);
            }
         }

         var2.sort((var0, var1x) -> {
            int var2x = Integer.compare((Integer)var1x.get("score"), (Integer)var0.get("score"));
            return var2x != 0 ? var2x : String.valueOf(var0.get("id")).compareTo(String.valueOf(var1x.get("id")));
         });
         return var2;
      } else {
         return var2;
      }
   }

   private static List<Map<String, Object>> manifestList(String var0) {
      String var1 = io.karatelabs.ext.common.help.ClasspathTextLoader.a("META-INF/karate/help/pages/manifest.json");
      if (var1 == null) {
         return new ArrayList<>();
      } else {
         return (List<Map<String, Object>>)(Json.of(var1).value() instanceof Map var3 && var3.get(var0) instanceof List var4 ? var4 : new ArrayList<>());
      }
   }

   private static Map<String, Object> pageMeta(String var0) {
      for (Map var2 : manifestList("pages")) {
         if (var0.equals(String.valueOf(var2.get("id")))) {
            return var2;
         }
      }

      return null;
   }

   private static Map<String, Object> resolve(String var0) {
      if (var0 != null && !var0.isBlank()) {
         for (Map var2 : manifestList("ids")) {
            if (var0.equals(String.valueOf(var2.get("id")))) {
               return var2;
            }
         }

         return null;
      } else {
         return null;
      }
   }

   private static List<String> pageIds() {
      ArrayList<java.lang.String> var0 = new ArrayList();

      for (Map var2 : manifestList("pages")) {
         var0.add(String.valueOf(var2.get("id")));
      }

      return var0;
   }

   private static List<String> allIds() {
      ArrayList<java.lang.String> var0 = new ArrayList();

      for (Map var2 : manifestList("ids")) {
         var0.add(String.valueOf(var2.get("id")));
      }

      return var0;
   }

   private static String sanitize(String var0) {
      return io.karatelabs.ext.common.help.ClasspathTextLoader.b(var0);
   }
}
