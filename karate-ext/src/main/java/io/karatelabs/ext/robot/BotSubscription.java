package io.karatelabs.ext.robot;

import io.karatelabs.js.SimpleObject;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class BotSubscription implements SimpleObject {
   private final String id = UUID.randomUUID().toString().substring(0, 8);
   private final String event;
   private final Map<String, Object> options;
   private final BotDriver agentDriver;
   private final List<Map<String, Object>> events;
   private boolean active;

   public BotSubscription(String var1, Map<String, Object> var2, BotDriver var3) {
      this.event = var1;
      this.options = var2 != null ? var2 : Map.of();
      this.agentDriver = var3;
      this.events = new ArrayList<>();
      this.active = true;
      this.setupListener();
   }

   @Override
   public Object jsGet(String var1) {
      return switch (var1) {
         case "id" -> this.id;
         case "event" -> this.event;
         case "active" -> this.active;
         case "getEvents" -> (io.karatelabs.js.JavaInvokable) var1x -> this.getEvents();
         case "clear" -> (io.karatelabs.js.JavaInvokable) var1x -> this.clear();
         case "off" -> (io.karatelabs.js.JavaInvokable) var1x -> this.off();
         default -> null;
      };
   }

   @Override
   public Collection<String> jsKeys() {
      return List.of("id", "event", "active", "getEvents", "clear", "off");
   }

   private void setupListener() {
      try {
         String var1 = this.options.get("action") != null ? String.valueOf(this.options.get("action")) : "log";
         String var2 = this.event;
         switch (var2) {
            case "dialog.alert":
            case "dialog.confirm":
            case "dialog.prompt":
               this.setupDialogListener(var1);
               break;
            case "console.log":
            case "console.error":
            case "console.warn":
               this.setupConsoleListener();
         }
      } catch (Exception var4) {
         this.active = false;
      }
   }

   private void setupDialogListener(String var1) {
      String var2 = this.event.substring("dialog.".length());
      String var3 = "if (!window.__agentDialogHandlers) window.__agentDialogHandlers = {};\nwindow.__agentDialogHandlers['%s'] = {\n    action: '%s',\n    events: []\n};\n// Override native dialog functions\nif ('%s' === 'alert') {\n    window.__originalAlert = window.__originalAlert || window.alert;\n    window.alert = function(msg) {\n        window.__agentDialogHandlers['%s'].events.push({type: 'alert', message: msg, time: Date.now()});\n        if (window.__agentDialogHandlers['%s'].action === 'log') return;\n    };\n} else if ('%s' === 'confirm') {\n    window.__originalConfirm = window.__originalConfirm || window.confirm;\n    window.confirm = function(msg) {\n        window.__agentDialogHandlers['%s'].events.push({type: 'confirm', message: msg, time: Date.now()});\n        return window.__agentDialogHandlers['%s'].action === 'accept';\n    };\n} else if ('%s' === 'prompt') {\n    window.__originalPrompt = window.__originalPrompt || window.prompt;\n    window.prompt = function(msg, def) {\n        window.__agentDialogHandlers['%s'].events.push({type: 'prompt', message: msg, defaultValue: def, time: Date.now()});\n        return window.__agentDialogHandlers['%s'].action === 'accept' ? (def || '') : null;\n    };\n}\n"
         .formatted(this.id, var1, var2, this.id, this.id, var2, this.id, this.id, var2, this.id, this.id);
      this.agentDriver.getDriver().script(var3);
   }

   private void setupConsoleListener() {
      String var1 = this.event.substring("console.".length());
      String var2 = "if (!window.__agentConsoleHandlers) window.__agentConsoleHandlers = {};\nwindow.__agentConsoleHandlers['%s'] = { events: [] };\nvar origFn = console.%s;\nconsole.%s = function() {\n    var args = Array.from(arguments);\n    window.__agentConsoleHandlers['%s'].events.push({\n        level: '%s',\n        message: args.join(' '),\n        time: Date.now()\n    });\n    origFn.apply(console, arguments);\n};\n"
         .formatted(this.id, var1, var1, this.id, var1);
      this.agentDriver.getDriver().script(var2);
   }

   private List<Map<String, Object>> getEvents() {
      if (!this.active) {
         return new ArrayList<>(this.events);
      } else {
         try {
            List var1 = null;
            if (this.event.startsWith("dialog.")) {
               var1 = (List)this.agentDriver
                  .getDriver()
                  .script(
                     "window.__agentDialogHandlers && window.__agentDialogHandlers['"
                        + this.id
                        + "'] ? window.__agentDialogHandlers['"
                        + this.id
                        + "'].events : []"
                  );
            } else if (this.event.startsWith("console.")) {
               var1 = (List)this.agentDriver
                  .getDriver()
                  .script(
                     "window.__agentConsoleHandlers && window.__agentConsoleHandlers['"
                        + this.id
                        + "'] ? window.__agentConsoleHandlers['"
                        + this.id
                        + "'].events : []"
                  );
            }

            if (var1 != null) {
               this.events.addAll(var1);
            }
         } catch (Exception var2) {
         }

         return new ArrayList<>(this.events);
      }
   }

   private Map<String, Object> clear() {
      this.events.clear();

      try {
         if (this.event.startsWith("dialog.")) {
            this.agentDriver
               .getDriver()
               .script(
                  "if (window.__agentDialogHandlers && window.__agentDialogHandlers['"
                     + this.id
                     + "']) window.__agentDialogHandlers['"
                     + this.id
                     + "'].events = []"
               );
         } else if (this.event.startsWith("console.")) {
            this.agentDriver
               .getDriver()
               .script(
                  "if (window.__agentConsoleHandlers && window.__agentConsoleHandlers['"
                     + this.id
                     + "']) window.__agentConsoleHandlers['"
                     + this.id
                     + "'].events = []"
               );
         }
      } catch (Exception var2) {
      }

      LinkedHashMap<java.lang.String, java.lang.Object> var1 = new LinkedHashMap();
      var1.put("cleared", true);
      return var1;
   }

   private Map<String, Object> off() {
      this.active = false;

      try {
         if (this.event.startsWith("dialog.")) {
            String var1 = this.event.substring("dialog.".length());
            String var2 = "if (window.__agentDialogHandlers) delete window.__agentDialogHandlers['%s'];\n// Restore original if this was the only handler\nif ('%s' === 'alert' && window.__originalAlert) window.alert = window.__originalAlert;\nif ('%s' === 'confirm' && window.__originalConfirm) window.confirm = window.__originalConfirm;\nif ('%s' === 'prompt' && window.__originalPrompt) window.prompt = window.__originalPrompt;\n"
               .formatted(this.id, var1, var1, var1);
            this.agentDriver.getDriver().script(var2);
         } else if (this.event.startsWith("console.")) {
            this.agentDriver.getDriver().script("if (window.__agentConsoleHandlers) delete window.__agentConsoleHandlers['" + this.id + "']");
         }
      } catch (Exception var3) {
      }

      LinkedHashMap<java.lang.String, java.lang.Object> var4 = new LinkedHashMap();
      var4.put("unsubscribed", true);
      var4.put("id", this.id);
      return var4;
   }

   public String getId() {
      return this.id;
   }

   public String getEvent() {
      return this.event;
   }

   public boolean isActive() {
      return this.active;
   }
}
