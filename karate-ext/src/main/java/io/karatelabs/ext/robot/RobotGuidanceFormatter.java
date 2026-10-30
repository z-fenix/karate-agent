package io.karatelabs.ext.robot;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class RobotGuidanceFormatter {
   private static final Pattern b = Pattern.compile("ReferenceError:\\s*([A-Za-z_$][A-Za-z0-9_$]*) is not defined");
   static final String a = "Wait on a locator from look().";

   private RobotGuidanceFormatter() {
   }

   static String a(int var0) {
      return var0 <= 0
         ? null
         : "Page didn't change AND "
            + var0
            + " new uncaught JS error"
            + (var0 == 1 ? "" : "s")
            + " just fired — likely blocking. Read with:\n  var logs = bot.capture('logs')\n  logs\n(2nd line returns the value — `var x = …` alone returns undefined.) Scan `logs.console` for an entry with `type:\"exception\"` — its `stack` shows the failing function. Patch missing function with `bot.eval(\"window.fnName = function(){}\")`. Do NOT instrument console.error via bot.eval.";
   }

   static String a(List<Map<String, Object>> var0) {
      if (var0 == null) {
         return null;
      } else {
         String var1 = null;

         for (Map var3 : var0) {
            Matcher var4 = b.matcher(String.valueOf(var3.get("text")));
            if (var4.find()) {
               var1 = var4.group(1);
            }
         }

         return var1 == null
            ? null
            : "`"
               + var1
               + "` is not defined — patch it at PAGE scope with an EMPTY stub FIRST:\n  bot.eval(\"window."
               + var1
               + " = function(){}\")\nthen redo the action and verify — the rest of the handler usually still runs once the ReferenceError is gone. The assignment MUST sit INSIDE the bot.eval(\"…\") quotes: bare JS in your block runs at bot scope, where `window` does not exist. Reimplement the body only if the retry shows the behavior still missing.";
      }
   }

   static String a(Map<String, Object> var0) {
      String var1 = d(var0);
      if (var0.get("canvas") instanceof Number var2 && var2.intValue() > 0) {
         String var5 = "This page draws content on a <canvas> — canvas pixels never appear in DOM text or elements; use bot.screenshot() to read them (the image arrives on your NEXT turn — screenshot NOW, report AFTER you have seen it).";
         var1 = var1 == null ? var5 : var1 + " " + var5;
      }

      String var4 = b(var0);
      if (var4 != null) {
         var1 = var1 == null ? var4 : var1 + " " + var4;
      }

      return var1;
   }

   static String b(Map<String, Object> var0) {
      List var1 = (List)var0.get("elements");
      if (var1 != null && !var1.isEmpty()) {
         for (Map var3 : (Iterable<Map>) (Iterable<?>) (var1)) {
            if (var3.get("occluded") != null) {
               return "'"
                  + var3.get("name")
                  + "' is occluded by `"
                  + var3.get("occluded")
                  + "` — dismiss that overlay (Escape, or its close/cancel button) before acting on it.";
            }
         }

         for (Map var10 : (Iterable<Map>) (Iterable<?>) (var1)) {
            if (var10.get("menu") instanceof Map var4 && Boolean.TRUE.equals(var4.get("closed"))) {
               return "'"
                  + var10.get("name")
                  + "' owns a collapsed menu ("
                  + var4.get("items")
                  + " items) — look({expand: '"
                  + var10.get("locator")
                  + "'}) opens it and lists the items.";
            }
         }

         for (Map var11 : (Iterable<Map>) (Iterable<?>) (var1)) {
            if (Boolean.TRUE.equals(var11.get("disabled"))) {
               return "'"
                  + var11.get("name")
                  + "' is disabled — it may enable after the page settles or a prior field commits; re-look after your next action.";
            }
         }

         for (Map var12 : (Iterable<Map>) (Iterable<?>) (var1)) {
            if (Boolean.TRUE.equals(var12.get("ambient"))) {
               return "'"
                  + var12.get("name")
                  + "' is ambient text (a label, not a target) that shares its name with an actionable element — act on the actionable one, not this label.";
            }
         }

         for (Map var13 : (Iterable<Map>) (Iterable<?>) (var1)) {
            if (d(String.valueOf(var13.get("name")))
               && var13.get("menu") == null
               && var13.get("revealedBy") == null
               && !Boolean.TRUE.equals(var13.get("disabled"))) {
               return "'"
                  + var13.get("locator")
                  + "' has no readable name (an icon-only control). Such a control often opens a menu or panel through JS that look() cannot see, so it carries no menu flag: act('"
                  + var13.get("locator")
                  + "', 'click') then look() to find out what it does, or bot.screenshot() to read the icons.";
            }
         }

         return null;
      } else {
         return null;
      }
   }

   private static boolean d(String var0) {
      return var0 != null && !var0.isBlank() && !"null".equals(var0) && !"(unnamed)".equals(var0)
         ? var0.length() <= 3 && var0.codePoints().noneMatch(Character::isLetterOrDigit)
         : true;
   }

   static String b(int var0) {
      return "Only the first "
         + var0
         + " elements are listed — this page has MORE, so an element missing from this list may still be there. Narrow rather than re-look: look({scope: '<css of the region you care about>'}) or look({role: 'input'}), or raise the cap with look({limit: 500}).";
   }

   private static String d(Map<String, Object> var0) {
      if (Boolean.TRUE.equals(var0.get("loading"))) {
         return "Page still loading. Call wait('idle') then look() again.";
      } else {
         List var1 = (List)var0.get("elements");
         if (var1 != null && !var1.isEmpty()) {
            boolean var10 = false;
            boolean var11 = false;

            for (Map var5 : (Iterable<Map>) (Iterable<?>) (var1)) {
               if (var5.get("actions") instanceof List var7) {
                  if (var7.contains("input") || var7.contains("select")) {
                     var10 = true;
                  }

                  if (var7.contains("click")) {
                     String var8 = String.valueOf(var5.getOrDefault("name", ""));
                     if (var8.toLowerCase().matches(".*(submit|save|send|login|sign.in|next|continue|search).*")) {
                        var11 = true;
                     }
                  }
               }
            }

            String var12 = "";
            List var13 = (List)var0.get("scrollContainers");
            if (var13 != null) {
               for (Map var17 : (Iterable<Map>) (Iterable<?>) (var13)) {
                  if (Boolean.FALSE.equals(var17.get("atBottom"))) {
                     String var19 = c(var17);
                     String var9 = String.valueOf(var17.getOrDefault("locator", ""));
                     var12 = " More fields below the fold in '"
                        + var19
                        + "' — look({viewport: false}) shows ALL fields at once, look({scope: '"
                        + var9
                        + "'}) focuses this panel, or act('"
                        + var9
                        + "', 'scroll', 'down') before submitting.";
                     break;
                  }
               }
            }

            if (var0.get("truncated") instanceof Number var15 && var15.intValue() > 0) {
               return b(var15.intValue()) + var12;
            } else if (var1.size() > 40) {
               return "Dense page (" + var1.size() + " elements). Use look({role: 'input'}) to filter." + var12;
            } else if (var0.containsKey("frames")) {
               return "Iframes detected. Use the full 'frame >> element' locator with act()." + var12;
            } else if (var12.isEmpty()) {
               Map<java.lang.String, Object> var16 = (Map)var0.get("scroll");
               if (var16 != null && Boolean.FALSE.equals(var16.get("atBottom"))) {
                  return var10 && var11
                     ? "Form detected. Fill visible fields, then scroll('down') to reveal more fields before submitting."
                     : "More content below. Use bot.scroll('down') to see more.";
               } else if (var10 && var11) {
                  return "Form detected. Fill fields with act(locator, 'input', value), then click submit.";
               } else {
                  return var10 ? "Input fields found. Use act(locator, 'input', value) to fill them." : null;
               }
            } else {
               return var10 ? "Fill visible fields, then scroll to reveal more." + var12 : "Scrollable panels detected." + var12;
            }
         } else {
            List var2 = (List)var0.get("text");
            if (var2 != null && !var2.isEmpty()) {
               String var3 = String.join(" ", var2).toLowerCase();
               if (var3.matches(".*(welcome|success|confirmed|complete|thank|signed in|logged in|submitted).*")) {
                  return "No interactive elements — page shows a confirmation message. If this matches your task goal, call Robot.success() with the result.";
               }
            }

            return "No interactive elements found. Try look({scope: 'body'}) or wait('idle').";
         }
      }
   }

   static String c(Map<?, ?> var0) {
      Object var1 = var0.get("label");
      String var2 = var1 == null ? "" : String.valueOf(var1).trim();
      if (!var2.isEmpty()) {
         return var2;
      } else {
         Object var3 = var0.get("locator");
         return var3 == null ? "this panel" : String.valueOf(var3);
      }
   }

   static String a(Map<String, Object> var0, String var1, String var2) {
      if (var2 != null) {
         return var2;
      } else if (var0.get("truncated") instanceof Number var3 && var3.intValue() > 0) {
         return "This page has MORE than "
            + var3.intValue()
            + " elements, so the before/after diff is INCOMPLETE — added/removed are not reported and an empty diff is not evidence that nothing happened. Judge the outcome from 'validation' (absent = no form errors) plus a narrowed look({scope: '<css of the region you acted in>'}).";
      } else if (var0.containsKey("dialog")) {
         return "Dialog detected. Use bot.page('dialog', 'accept') or bot.page('dialog', 'dismiss').";
      } else if (var0.containsKey("newTab")) {
         String var16 = var0.get("newTab") instanceof Map var17 ? String.valueOf(var17.get("title")) : "";
         StringBuilder var18 = new StringBuilder();
         var18.append("Click opened a new tab and focus was moved to it");
         if (!var16.isEmpty() && !"null".equals(var16)) {
            var18.append(" (title: '").append(var16).append("')");
         }

         var18.append(". Call look() to see the new page.");
         if (var0.containsKey("otherTabs")) {
            var18.append(" Other tabs also opened — switch with page('window', 'switchTab', titleOrUrlSubstr) if this isn't the right one.");
         }

         return var18.toString();
      } else if (var0.containsKey("navigated")) {
         return "Page changed. Call look() to see the new page.";
      } else if (var0.containsKey("validation")) {
         return !"input".equals(var1) && !"clear".equals(var1)
            ? "Form has validation errors. Fix the flagged fields and retry."
            : "Validation errors after '"
               + var1
               + "'. If the field has JS validation (enterprise SPA), try act(locator, 'click') then act(locator, 'key', 'value') instead — 'key' preserves framework event bindings.";
      } else if (Boolean.TRUE.equals(var0.get("elementGone"))) {
         return "Element disappeared (modal closed or content updated). Call look() to see current state.";
      } else {
         if (var0.containsKey("text") && var0.get("text") instanceof List var12 && !var12.isEmpty()) {
            for (Object var6 : var12) {
               String var7 = String.valueOf(var6).toLowerCase();
               if (var7.contains("error") || var7.contains("fail") || var7.contains("invalid")) {
                  return !"input".equals(var1) && !"clear".equals(var1)
                     ? "Page shows errors. Review the 'text' field and fix."
                     : "Page shows errors after '"
                        + var1
                        + "'. If caused by JS field validation, try act(locator, 'click') then act(locator, 'key', 'value') instead.";
               }
            }
         }

         if (var0.containsKey("newLocators")) {
            List var10 = (List)var0.get("newLocators");
            boolean var15 = var10.stream().anyMatch(var0x -> ((Map)var0x).get("menu") instanceof Map);
            return var15
               ? "Menu opened. An item flagged menu:{closed…} owns a FURTHER submenu — look({expand: '<its locator>'}) or the advertised hover opens it. Other items can be clicked directly."
               : "Menu opened. Click the desired item.";
         } else if (var0.containsKey("removed")) {
            return "Elements GONE after '"
               + var1
               + "' (see `removed`) — the surface you acted on closed, committed, or re-rendered. Trust this + `validation` (absent = no form errors) + a follow-up look() diff as the success oracle — never a bespoke DOM probe (a null getElementById is ambiguous).";
         } else if ("click".equals(var1)) {
            if (Boolean.TRUE.equals(var0.get("textChanged"))) {
               return "Click landed and the page ANSWERED IN TEXT (see `text`) — no interactable appeared or disappeared, so read that answer rather than repeating the click: it is the result banner/validation for what you just did. For the values behind it, bot.text('<selector>') or bot.table('<selector>').";
            } else if (var0.get("regionChanged") instanceof Map var9) {
               Object var14 = var9.get("locator");
               return "Click landed and the visible REGION changed to '"
                  + var9.get("name")
                  + "'"
                  + (var14 == null ? "" : " (" + var14 + ")")
                  + " — no interactable moved because that region holds only values, which is what this diff can see. Do NOT re-click or hover for a menu: read it with bot.text("
                  + (var14 == null ? "'<selector>'" : "'" + var14 + "'")
                  + ") or bot.table("
                  + (var14 == null ? "'<selector>'" : "'" + var14 + "'")
                  + ").";
            } else if (!Boolean.TRUE.equals(var0.get("noChange"))) {
               return "Call look() to verify the result.";
            } else {
               return var0.get("pageError") != null
                  ? "Click did NOT land — a JS exception fired during it ("
                     + var0.get("pageError")
                     + "), so the page's handler aborted partway and nothing moved. Do NOT re-click and do NOT read the region for a value that was never written: read the fault with bot.capture('logs') — its `hint` names the patch."
                  : "Click landed but NO interactable appeared or disappeared — so it either opened nothing, or it changed only VALUES/text, which this diff cannot see. Do NOT repeat the click. Read the region instead: bot.text('<selector>') or bot.table('<selector>'). If you expected a menu/panel, the trigger may be hover-driven — look({expand: '<its locator>'}) drives the reveal with the verb look() observed.";
            }
         } else {
            return !"input".equals(var1) && !"select".equals(var1) ? null : "Value set. Continue filling other fields or submit.";
         }
      }
   }

   static String a(String var0) {
      return " — expand only drives a target look() SAW owning a collapsed surface (it flags those with menu:{closed}). A JS-only reveal has no such evidence, so drive it by hand: act('"
         + var0
         + "', 'click') then look() — a reveal arrives as added elements; if the click moves nothing, act('"
         + var0
         + "', 'hover') then look(). bot.study() reports how this app builds its menus.";
   }

   static String b(String var0) {
      return "element is occluded by " + var0 + " — close the overlay first (try act(loc, 'key', '{Escape}'), click outside, or close-button)";
   }

   static String a(int var0, String[] var1) {
      StringBuilder var2 = new StringBuilder("Network idle timeout — ");
      var2.append(var0).append(" request(s) still pending");
      if (var1 != null) {
         var2.append(" (").append(var1[1]).append(" ").append(var1[0]).append(")");
      }

      return var2.toString();
   }

   static String b(List<Map<String, Object>> var0) {
      long var1 = var0.stream()
         .filter(var0x -> Boolean.TRUE.equals(var0x.get("open")) && var0x.get("ageMs") instanceof Number var1x && var1x.longValue() > 5000L)
         .count();
      return var1 > 0L
         ? var1
            + " long-open connection(s) (e.g. SSE/WebSocket) hold the browser's per-origin pool — on HTTP/1.1 ~6 of these starve further requests (page loads then stall)."
         : null;
   }

   static String a(String var0, Map<String, Object> var1, Supplier<String> var2) {
      if (var0 != null && !var0.isEmpty() && var1 != null) {
         try {
            Object var3 = var1.get("url");
            String var4 = (String)var2.get();
            if (var3 != null && var4 != null && !var3.toString().equals(var4)) {
               return null;
            }
         } catch (Exception var16) {
         }

         List var17 = (List)var1.get("elements");
         if (var17 != null && !var17.isEmpty()) {
            String var18 = var0.toLowerCase();
            ArrayList<java.lang.String> var5 = new ArrayList();

            for (String var9 : var18.split("[^a-z]+")) {
               if (var9.length() >= 3) {
                  var5.add(var9);
               }
            }

            ArrayList<java.lang.String> var19 = new ArrayList();
            ArrayList<java.lang.String> var20 = new ArrayList();

            for (Map var23 : (Iterable<Map>) (Iterable<?>) (var17)) {
               String var10 = String.valueOf(var23.getOrDefault("locator", ""));
               if (!var10.isEmpty() && !var10.equals(var0)) {
                  String var11 = var10.toLowerCase();
                  String var12 = String.valueOf(var23.getOrDefault("name", "")).toLowerCase();
                  boolean var13 = false;

                  for (String var15 : (Iterable<String>) (Iterable<?>) (var5)) {
                     if (var11.contains(var15) || var12.contains(var15)) {
                        var13 = true;
                        break;
                     }
                  }

                  if (var13) {
                     var19.add(var10);
                  } else {
                     int var28 = 0;
                     int var29 = Math.min(var18.length(), var11.length());

                     while (var28 < var29 && var18.charAt(var28) == var11.charAt(var28)) {
                        var28++;
                     }

                     if (var28 >= 3) {
                        var20.add(var10);
                     }
                  }
               }
            }

            ArrayList<java.lang.String> var22 = new ArrayList();

            for (String var26 : (Iterable<String>) (Iterable<?>) (var19)) {
               if (var22.size() < 5) {
                  var22.add(var26);
               }
            }

            for (String var27 : (Iterable<String>) (Iterable<?>) (var20)) {
               if (var22.size() < 5 && !var22.contains(var27)) {
                  var22.add(var27);
               }
            }

            return var22.isEmpty() ? null : String.join(", ", var22);
         } else {
            return null;
         }
      } else {
         return null;
      }
   }

   static String a(String var0, String var1, String var2) {
      return var0 + " never contained '" + var1 + "' — it is '" + var2 + "'";
   }

   static String c(String var0) {
      return switch (var0) {
         case "INVALID_ARGS" -> "Check method signature with help().";
         case "ELEMENT_NOT_FOUND" -> "Call look() and use the EXACT locator string from the response. Locators on the same page are NOT consistent — e.g. #vehicleYear + #make + #model, not #vehicleMake. Never extrapolate one locator from another.";
         case "ACTION_FAILED" -> "Call look() and use the exact locator string from the response, then retry.";
         case "DIALOG_BLOCKING" -> "Dismiss dialog first: bot.page('dialog', 'accept') or bot.page('dialog', 'dismiss').";
         case "TIMEOUT" -> "Page may be slow. Try wait('idle') then retry.";
         case "EVAL_FAILED" -> "Check the JS syntax (avoid backtick template literals — concatenate strings). bot.eval supports a top-level `return`, so a scan loop is fine. PREFER look()/act() to find and drive elements — only reach for eval when look() can't surface what you need (e.g. an element revealed on hover: act(rowLocator, 'hover') then look() again).";
         case "INVALID_ACTION" -> "Check available actions in look() element's 'actions' array.";
         case "INVALID_NAMESPACE" -> "Valid namespaces: nav, input, query, window, clear, dialog.";
         case "NO_DIALOG" -> "No dialog open. Continue with your next action.";
         case "INVALID_TYPE" -> "Valid capture types: 'screenshot', 'logs', 'clipboard'. The argument is a type, not a label/filename.";
         case "NOT_IMPLEMENTED" -> "Feature not yet available. Try an alternative approach.";
         default -> "Call look() to see current page state.";
      };
   }
}
