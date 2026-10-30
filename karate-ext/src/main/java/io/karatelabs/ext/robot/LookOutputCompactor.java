package io.karatelabs.ext.robot;

import io.karatelabs.common.Json;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class LookOutputCompactor implements io.karatelabs.ext.agent.runtime.AgentHook {
   private static final List<String> a = List.of("url", "title", "hint", "validation", "navigated", "text", "scroll", "healed");

   @Override
   public Optional<String> a(String var1) {
      if (var1 == null) {
         return Optional.empty();
      } else {
         String var2 = var1;
         int var3 = var1.lastIndexOf("\n[");
         if (var3 > 0) {
            var2 = var1.substring(0, var3);
         }

         int var4 = var2.indexOf("\n⚠");
         if (var4 > 0) {
            var2 = var2.substring(0, var4);
         }

         var2 = var2.trim();
         if (var2.startsWith("{") && var2.contains("\"elements\"")) {
            Object var5;
            try {
               var5 = Json.parseStrict(var2, true);
            } catch (Exception var13) {
               return Optional.empty();
            }

            if (!(var5 instanceof Map var6)) {
               return Optional.empty();
            } else {
               LinkedHashMap<java.lang.String, java.lang.Object> var7 = new LinkedHashMap();

               for (String var9 : a) {
                  Object var10 = var6.get(var9);
                  if ((var9.equals("url") || var9.equals("title")) && var10 != null) {
                     var7.put(var9, var10);
                  }
               }

               if (var6.get("elements") instanceof List var16) {
                  var7.put("elements", var16.size() + " items (call look() to re-read locators)");
               }

               if (var6.get("scrollContainers") instanceof List var18 && !var18.isEmpty()) {
                  var7.put("scrollContainers", var18.size() + " scrollable container(s)");
               }

               for (String var11 : List.of("scroll", "hint", "validation", "navigated", "text", "healed")) {
                  Object var12 = var6.get(var11);
                  if (var12 != null) {
                     var7.put(var11, var12);
                  }
               }

               return Optional.of(Json.stringifyStrict(var7));
            }
         } else {
            return Optional.empty();
         }
      }
   }
}
