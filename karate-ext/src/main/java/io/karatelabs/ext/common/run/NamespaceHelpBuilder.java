package io.karatelabs.ext.common.run;

import io.karatelabs.ext.common.jsapi.JsApi;
import io.karatelabs.ext.common.jsapi.JsDispatch;
import io.karatelabs.js.JavaInvokable;
import io.karatelabs.js.SimpleObject;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class NamespaceHelpBuilder {
   private static final Logger a = LoggerFactory.getLogger(NamespaceHelpBuilder.class);
   private static final Object[] b = new Object[0];

   private NamespaceHelpBuilder() {
   }

   public static JavaInvokable a(List<NamespaceBinding> var0) {
      var var1 = List.copyOf(var0);
      return var1x -> b(var1);
   }

   static Map<String, Object> b(List<NamespaceBinding> var0) {
      ArrayList<java.util.LinkedHashMap> var1 = new ArrayList();

      for (NamespaceBinding var3 : var0) {
         LinkedHashMap<java.lang.String, java.lang.Object> var4 = new LinkedHashMap();
         var4.put("name", var3.a());
         String var5 = var3.c() != null && !var3.c().isBlank() ? var3.c() : a(var3.b());
         if (var5 != null) {
            var4.put("summary", var5);
         }

         var var6 = c(var3.b());
         var var7 = new ArrayList<>(b(var3.b()));
         var7.removeAll(var6);
         if (!var7.isEmpty()) {
            var4.put("verbs", var7);
         }

         if (!var6.isEmpty()) {
            var4.put("namespaces", new ArrayList(var6));
         }

         var1.add(var4);
      }

      LinkedHashMap<java.lang.String, java.lang.Object> var8 = new LinkedHashMap();
      var8.put(
         "markdown",
         "# karate — what you can drive\n\nEach namespace below is a live JS surface. Call `<Name>.help()` for its detail and the call convention; `Skill.list()` for multi-step workflows; `Engine.vars()` to recall what you're holding. The task comes from the user — discover the pieces and compose them."
      );
      var8.put("namespaces", var1);
      var8.put(
         "usage",
         "Noun.help() for one namespace; Skill.help(name) for a workflow. A row's `verbs` are callable (`Noun.verb(…)`); a row's `namespaces` are NOT — they group further verbs, called as `Noun.sub.verb(…)`"
      );
      return JsApi.hint(
         var8,
         "pull a namespace's detail with its help() (e.g. Openapi.help(), Http.help()); match a multi-step task to a Skill.help(name) recipe when one fits"
      );
   }

   private static Collection<String> b(Object var0) {
      if (var0 instanceof SimpleObject var1) {
         var var2 = var1.jsKeys();
         if (var2 != null) {
            return var2;
         }
      }

      return List.of();
   }

   private static Collection<String> c(Object var0) {
      return (Collection<String>)(var0 instanceof JsDispatch var1 ? var1.jsNamespaces() : List.of());
   }

   static String a(Object var0) {
      if (var0 instanceof SimpleObject var1) {
         Object var2;
         try {
            var2 = var1.jsGet("help");
         } catch (RuntimeException var9) {
            return null;
         }

         if (var2 instanceof JavaInvokable var3) {
            Object var4;
            try {
               var4 = var3.invoke(b);
            } catch (RuntimeException var8) {
               a.debug("help() tagline derivation failed: {}", var8.toString());
               return null;
            }

            return var4 instanceof Map var5 && var5.get("markdown") instanceof String var6 ? a(var6) : null;
         } else {
            return null;
         }
      } else {
         return null;
      }
   }

   private static String a(String var0) {
      for (String var4 : var0.split("\n", 8)) {
         String var5 = var4.strip();
         if (!var5.isEmpty()) {
            if (!var5.startsWith("#")) {
               return null;
            }

            int var6 = var5.indexOf(8212);
            if (var6 < 0) {
               return null;
            }

            String var7 = var5.substring(var6 + 1).strip();
            return var7.isEmpty() ? null : var7;
         }
      }

      return null;
   }
}
