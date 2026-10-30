package io.karatelabs.ext.max.jsapi;

import io.karatelabs.common.Json;
import io.karatelabs.ext.common.jsapi.JsApi;
import io.karatelabs.ext.common.jsapi.JsDispatch;
import io.karatelabs.ext.max.console.ConsoleRegistry;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

public class View extends JsDispatch {
   private volatile BiConsumer<String, String> sink = (var0, var1) -> {};

   public View() {
      this.fn("help", var0 -> io.karatelabs.ext.common.help.HelpTopicProvider.a("view").a(var0));
      this.fn("map", var0 -> ConsoleRegistry.get().pages());
      this.fn("sections", var1 -> this.sections(JsApi.str(var1, 0)));
      this.fn("resolve", var1 -> this.resolve(JsApi.str(var1, 0)));
      this.fn("navigate", var1 -> this.steer("navigate", JsApi.str(var1, 0), null, null));
      this.fn("highlight", var1 -> this.steer("highlight", JsApi.str(var1, 0), null, JsApi.arg(var1, 1)));
      this.fn("callout", var1 -> this.callout(JsApi.str(var1, 0), JsApi.str(var1, 1), JsApi.arg(var1, 2)));
      this.fn("overlay", var1 -> this.overlay(JsApi.arg(var1, 0)));
      this.fn("open", var1 -> this.steer("open", JsApi.str(var1, 0), null, null));
      this.fn("clear", var1 -> this.clear());
   }

   public View withSink(BiConsumer<String, String> var1) {
      this.sink = var1;
      return this;
   }

   Object sections(String var1) {
      ConsoleRegistry var2 = ConsoleRegistry.get();
      if (var1 == null || var1.isBlank()) {
         return JsApi.missing("View.sections(id) needs a page/section id — try View.map() for the pages, then View.sections('rules') to drill in.");
      } else {
         return var2.resolve(var1) == null ? notFound(var1, var2) : var2.sections(var1);
      }
   }

   Map<String, Object> resolve(String var1) {
      ConsoleRegistry var2 = ConsoleRegistry.get();
      if (var1 != null && !var1.isBlank()) {
         var var3 = var2.resolve(var1);
         return var3 == null ? notFound(var1, var2) : var3;
      } else {
         return JsApi.missing("View.resolve(id) needs a page/section id (e.g. 'rules.reference.code') — View.map() lists the pages.");
      }
   }

   Object steer(String var1, String var2, String var3, Object var4) {
      ConsoleRegistry var5 = ConsoleRegistry.get();
      if (var2 == null || var2.isBlank()) {
         return JsApi.missing("View." + var1 + "(id) needs a page/section id — View.map() lists the pages, View.sections('<page>') drills in.");
      } else if (var2.startsWith("/")) {
         LinkedHashMap var9 = new LinkedHashMap();
         var9.put("type", var1);
         var9.put("id", var2);
         var9.put("route", var2);
         if (var3 != null) {
            var9.put("text", var3);
         }

         applyTargeting(var9, var4);
         this.sink.accept(var1, Json.stringifyStrict(var9));
         LinkedHashMap var10 = new LinkedHashMap();
         var10.put("steered", var1);
         var10.put("route", var2);
         return var10;
      } else {
         var var6 = var5.resolve(var2);
         if (var6 == null) {
            return notFound(var2, var5);
         } else {
            Object var7 = var6.get("route");
            if (("navigate".equals(var1) || "open".equals(var1)) && var7 instanceof String var8 && var8.contains("{")) {
               return danglingRoute(var2, var8);
            } else {
               LinkedHashMap var11 = new LinkedHashMap();
               var11.put("type", var1);
               var11.put("id", var2);
               var11.put("anchor", var6.get("anchor"));
               if (var7 != null) {
                  var11.put("route", var7);
               }

               if (var3 != null) {
                  var11.put("text", var3);
               }

               applyTargeting(var11, var4);
               this.sink.accept(var1, Json.stringifyStrict(var11));
               return ack(var1, var6);
            }
         }
      }
   }

   Object callout(String var1, String var2, Object var3) {
      return var2 != null && !var2.isBlank()
         ? this.steer("callout", var1, var2, var3)
         : JsApi.missing("View.callout(id, text) needs the callout text (rendered as data on the section).");
   }

   private static void applyTargeting(Map<String, Object> var0, Object var1) {
      if (var1 instanceof Map var2) {
         Object var3 = var2.get("node");
         if (var3 != null && !String.valueOf(var3).isBlank()) {
            var0.put("node", String.valueOf(var3));
         }

         Object var4 = var2.get("line");
         if (var4 instanceof Number var5) {
            var0.put("line", var5.intValue());
         } else if (var4 != null && !String.valueOf(var4).isBlank()) {
            try {
               var0.put("line", Integer.parseInt(String.valueOf(var4).trim()));
            } catch (NumberFormatException var7) {
            }
         }
      }
   }

   private static Map<String, Object> danglingRoute(String var0, String var1) {
      String var2 = var1.replaceAll("\\{[^}]*}", "<value>");
      return JsApi.missing(
         "'"
            + var0
            + "' lives on an instance route ("
            + var1
            + ") that names no specific instance, so it can't be navigated by id alone. Navigate the raw path instead — e.g. View.navigate('"
            + var2
            + "') — then View.highlight('"
            + var0
            + "') to point inside it."
      );
   }

   Object overlay(Object var1) {
      if (var1 instanceof Map var2) {
         Object var3 = var2.get("body");
         if (var3 != null && !String.valueOf(var3).isBlank()) {
            LinkedHashMap var4 = new LinkedHashMap();
            var4.put("type", "overlay");
            if (var2.get("title") != null) {
               var4.put("title", String.valueOf(var2.get("title")));
            }

            var4.put("body", String.valueOf(var3));
            Object var5 = var2.get("anchor");
            if (var5 != null) {
               var var6 = ConsoleRegistry.get().resolve(String.valueOf(var5));
               if (var6 != null) {
                  var4.put("anchor", var6.get("anchor"));
               }
            }

            if (var2.get("placement") != null) {
               var4.put("placement", String.valueOf(var2.get("placement")));
            }

            this.sink.accept("overlay", Json.stringifyStrict(var4));
            LinkedHashMap var7 = new LinkedHashMap();
            var7.put("steered", "overlay");
            if (var4.get("anchor") != null) {
               var7.put("anchor", var4.get("anchor"));
            }

            return var7;
         } else {
            return JsApi.missing("View.overlay needs a `body` (markdown text, rendered as data).");
         }
      } else {
         return JsApi.missing("View.overlay({title, body, anchor?, placement?}) needs an options object — at least a `body` (markdown, rendered as data).");
      }
   }

   Object clear() {
      this.sink.accept("view-clear", "{\"type\":\"view-clear\"}");
      LinkedHashMap var1 = new LinkedHashMap();
      var1.put("steered", "clear");
      return var1;
   }

   private static Map<String, Object> ack(String var0, Map<String, Object> var1) {
      LinkedHashMap var2 = new LinkedHashMap();
      var2.put("steered", var0);
      var2.put("id", var1.get("id"));
      if (var1.get("route") != null) {
         var2.put("route", var1.get("route"));
      }

      var2.put("anchor", var1.get("anchor"));
      return var2;
   }

   private static Map<String, Object> notFound(String var0, ConsoleRegistry var1) {
      var var2 = JsApi.missing("no such UI location: '" + var0 + "' — View.map() lists the pages; View.sections('<page>') lists a page's sections.");
      var var3 = io.karatelabs.ext.common.jsapi.SuggestionEngine.a(var0, var1.ids());
      if (!var3.isEmpty()) {
         JsApi.suggest(var2, var3);
      }

      return var2;
   }
}
