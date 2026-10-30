package io.karatelabs.ext.robot;

import io.karatelabs.common.Json;
import io.karatelabs.driver.Dialog;
import io.karatelabs.driver.Driver;
import io.karatelabs.driver.DriverException;
import io.karatelabs.driver.Element;
import io.karatelabs.driver.Locators;
import io.karatelabs.driver.Mouse;
import io.karatelabs.driver.cdp.CdpDriver;
import io.karatelabs.driver.cdp.CdpEventListener;
import io.karatelabs.ext.common.jsapi.JsApi;
import io.karatelabs.js.JavaInvokable;
import io.karatelabs.js.JsFunction;
import io.karatelabs.js.SimpleObject;
import io.karatelabs.js.Terms;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BotDriver implements CdpEventListener, SimpleObject {
   private static final Logger logger = LoggerFactory.getLogger(BotDriver.class);
   private static final String AGENT_LOOK_JS = loadAgentLookJs();
   private static final String RECORDER_JS = io.karatelabs.ext.robot.RobotResourceLoader.a("js/recorder.js");
   private static final String AGENT_MODEL_JS = io.karatelabs.ext.robot.RobotResourceLoader.a("js/agent-model.js");
   private final Driver driver;
   private final io.karatelabs.ext.robot.BrowserDiagnosticsCollector diagnostics = new io.karatelabs.ext.robot.BrowserDiagnosticsCollector();
   private double[] iframeOffset;
   private Path screenshotsDir;
   private Supplier<Path> screenshotsDirSupplier;
   private Map<String, Object> lastLookResult;
   private int looksSinceAct;
   private String lastActionError;
   private final List<Map<String, Object>> pendingHeals = new ArrayList<>();
   private String lastStabilityHint;
   private final ElementRevealController reveal;
   private volatile Map<String, Object> studyDossier;
   private final String healEnv = System.getenv("KARATE_ROBOT_HEAL");
   private Process videoProcess;
   private Consumer<byte[]> screenshotConsumer;
   private BiConsumer<String, String> screenshotSavedListener;
   private volatile io.karatelabs.ext.robot.BotDriver.HealingListener healListener;
   private Function<String, String> screenshotUrlResolver;
   private UnaryOperator<String> uploadFileResolver;
   private AtomicBoolean actionFlag;
   private boolean screenshotHintEmitted;
   private volatile io.karatelabs.ext.robot.BotDriver.RecordingEventListener recordBinding;
   private volatile Runnable recordOnReset;
   private final Map<Long, String> ctxToFrame = new ConcurrentHashMap<>();
   private final Map<String, String> frameOwnerCache = new ConcurrentHashMap<>();
   private final Set<String> frameResolveTried = ConcurrentHashMap.newKeySet();
   private volatile ExecutorService frameResolver;
   static final List<String> VERBS = List.of(
      "look",
      "find",
      "findAll",
      "act",
      "go",
      "wait",
      "page",
      "eval",
      "text",
      "table",
      "capture",
      "screenshot",
      "scroll",
      "study",
      "reset",
      "on",
      "help",
      "driver"
   );
   private static final Set<String> KNOWN_KEYS = Set.of(
      "ENTER",
      "RETURN",
      "TAB",
      "ESCAPE",
      "ESC",
      "BACKSPACE",
      "DELETE",
      "SPACE",
      "UP",
      "ARROWUP",
      "DOWN",
      "ARROWDOWN",
      "LEFT",
      "ARROWLEFT",
      "RIGHT",
      "ARROWRIGHT",
      "PAGEUP",
      "PAGEDOWN",
      "HOME",
      "END"
   );
   Consumer<Runnable> barrier;
   private static final List<String> SESSION_PACK_KEYS = List.of("drawerSelector", "nameNoiseTokens", "caretSelectors", "menuSelectors");
   private static final String STUDY_RECIPE = "Take stock before driving: explore this page (look(), look({viewport:false}), eval()), identify the repeating patterns (how menus/carets open, how fields are named, where validation errors surface, date/picker widget choreographies), and author a helper or two for the fiddly bits — DECLARE each helper in a js block (`var readScore = function(){ … };`); your vars persist across turns, so that is what makes it callable later. Then submit a dossier with bot.study({...}) — its machine fields make look()/act() better for the REST of the session automatically, and a verified helper can be promoted to checks/helpers.js If a `pack` is included below, this app is already known — adopt its choreographies/helpers and only verify what is page-specific.";
   private static final Set<String> STATE_CHANGING_OPS = Set.of(
      "click", "input", "clear", "select", "check", "uncheck", "upload", "key", "doubleClick", "rightClick"
   );
   private static final List<String> KNOWN_OPS_ORDERED = List.of(
      "click", "input", "select", "check", "uncheck", "clear", "key", "upload", "hover", "focus", "scroll", "doubleClick", "rightClick"
   );
   private static final Set<String> KNOWN_OPS = Set.copyOf(KNOWN_OPS_ORDERED);
   private static final Set<String> VALUE_OPS = Set.of("input", "select", "key");
   private static final int REMOVED_EMIT_CAP = 12;
   static final String NUM_PARSE_JS = "function num(s){var v=(s||'').replace(/\\$/g,'').replace(/\\s/g,'');if(!v)return s;var neg=/^-/.test(v);if(neg)v=v.slice(1);var d=v.indexOf('.')>=0,c=v.indexOf(',')>=0,n;if(d&&c){n=v.lastIndexOf(',')>v.lastIndexOf('.')?v.replace(/\\./g,'').replace(',','.'):v.replace(/,/g,'');}else if(c){var p=v.split(',');n=(p.length===2&&p[1].length>0&&p[1].length<=2)?p[0]+'.'+p[1]:v.replace(/,/g,'');}else{n=v;}return /^\\d+(\\.\\d+)?$/.test(n)?parseFloat(neg?'-'+n:n):s;}";
   private static final String TABLE_EXTRACT_JS = "(function(){var sel=__SEL__;var t=sel?document.querySelector(sel):null;if(sel&&!t)return '__NO_MATCH__';if(!t){var c=Array.prototype.slice.call(document.querySelectorAll('table,[role=\"table\"],[role=\"grid\"]')).filter(function(e){return e.offsetParent!==null;});if(!c.length)return null;c.sort(function(a,b){return b.querySelectorAll('tr,[role=\"row\"]').length-a.querySelectorAll('tr,[role=\"row\"]').length;});t=c[0];}if(!t)return null;if(t.tagName!=='TABLE'&&t.getAttribute('role')!=='table'&&t.getAttribute('role')!=='grid'){t=t.closest('table,[role=\"table\"],[role=\"grid\"]')||t;}function cells(r){return Array.prototype.slice.call(r.querySelectorAll('th,td,[role=\"columnheader\"],[role=\"cell\"],[role=\"gridcell\"]'));}var allRows=Array.prototype.slice.call(t.querySelectorAll('tr,[role=\"row\"]'));if(!allRows.length)return null;var headerRow=t.querySelector('thead tr')||allRows[0];var headers=cells(headerRow).map(function(c){return (c.textContent||'').trim();});var body=allRows.filter(function(r){return r!==headerRow&&cells(r).length;});function num(s){var v=(s||'').replace(/\\$/g,'').replace(/\\s/g,'');if(!v)return s;var neg=/^-/.test(v);if(neg)v=v.slice(1);var d=v.indexOf('.')>=0,c=v.indexOf(',')>=0,n;if(d&&c){n=v.lastIndexOf(',')>v.lastIndexOf('.')?v.replace(/\\./g,'').replace(',','.'):v.replace(/,/g,'');}else if(c){var p=v.split(',');n=(p.length===2&&p[1].length>0&&p[1].length<=2)?p[0]+'.'+p[1]:v.replace(/,/g,'');}else{n=v;}return /^\\d+(\\.\\d+)?$/.test(n)?parseFloat(neg?'-'+n:n):s;}var max=__MAX__;var rows=[];for(var i=0;i<body.length&&rows.length<max;i++){var cs=cells(body[i]);var o={};for(var j=0;j<cs.length;j++){var k=headers[j]&&headers[j].length?headers[j]:('col'+(j+1));o[k]=num((cs[j].textContent||'').trim());}rows.push(o);}return {columns:headers,rowCount:body.length,rows:rows,truncated:body.length>rows.length,hint:'Structured rows; numeric cells parsed to numbers. Aggregate directly e.g. rows.filter(r=>r.Category===\"Travel\").length, rows.reduce((s,r)=>s+r.Amount,0). rowCount is the authoritative total — verify your row count against it before aggregating.'};})()";
   private int lastTextSize;
   private static final Duration READY_STATE_SOFT_TIMEOUT = Duration.ofSeconds(5L);

   public BotDriver(Driver var1) {
      this.driver = var1;
      this.reveal = new ElementRevealController(this, var1);
      this.injectAgentLookJs();
      if (var1 instanceof CdpDriver var2) {
         var2.addCdpEventListener(this);
      }
   }

   public void setScreenshotsDir(Path var1) {
      this.screenshotsDir = var1;
   }

   public void setScreenshotsDirSupplier(Supplier<Path> var1) {
      this.screenshotsDirSupplier = var1;
   }

   public void setScreenshotConsumer(Consumer<byte[]> var1) {
      this.screenshotConsumer = var1;
   }

   public void setScreenshotSavedListener(BiConsumer<String, String> var1) {
      this.screenshotSavedListener = var1;
   }

   public void setHealListener(io.karatelabs.ext.robot.BotDriver.HealingListener var1) {
      this.healListener = var1;
   }

   public void setScreenshotUrlResolver(Function<String, String> var1) {
      this.screenshotUrlResolver = var1;
   }

   public void setUploadFileResolver(UnaryOperator<String> var1) {
      this.uploadFileResolver = var1;
   }

   public void setActionFlag(AtomicBoolean var1) {
      this.actionFlag = var1;
   }

   @Override
   public Object jsGet(String var1) {
      return guard(this.actionFlag, this.member(var1));
   }

   static Object guard(AtomicBoolean var0, Object var1) {
      if (var0 == null || var1 == null) {
         return var1;
      } else if (var1 instanceof JavaInvokable var2) {
         return (JavaInvokable)var2x -> {
            var0.set(true);
            return var2.invoke(var2x);
         };
      } else {
         var0.set(true);
         return var1;
      }
   }

   private Object member(String var1) {
      return switch (var1) {
         case "look" -> (io.karatelabs.js.JavaInvokable) this::look;
         case "find" -> (io.karatelabs.js.JavaInvokable) this::find;
         case "findAll" -> (io.karatelabs.js.JavaInvokable) this::findAll;
         case "act" -> (io.karatelabs.js.JavaInvokable) this::act;
         case "go" -> (io.karatelabs.js.JavaInvokable) this::go;
         case "wait" -> (io.karatelabs.js.JavaInvokable) this::waitFor;
         case "page" -> (io.karatelabs.js.JavaInvokable) this::page;
         case "eval" -> (io.karatelabs.js.JavaInvokable) this::evalJs;
         case "text" -> (io.karatelabs.js.JavaInvokable) this::text;
         case "table" -> (io.karatelabs.js.JavaInvokable) this::table;
         case "capture" -> (io.karatelabs.js.JavaInvokable) this::capture;
         case "screenshot" -> (io.karatelabs.js.JavaInvokable) var1x -> {
            String var2 = null;
            Map<String, Object> var3 = Map.of();
            if (var1x.length > 0 && var1x[0] != null) {
               if (var1x[0] instanceof String var4) {
                  var2 = var4;
               } else {
                  var3 = this.argsToMap(new Object[]{var1x[0]});
               }
            }

            return this.captureScreenshot(var3, var2);
         };
         case "scroll" -> (io.karatelabs.js.JavaInvokable) var1x -> {
            String var2 = var1x.length > 0 ? String.valueOf(var1x[0]).toLowerCase() : "down";
            if (!Set.of("up", "down", "left", "right").contains(var2)) {
               throw this.fail(
                  "bot.scroll(direction) takes 'up', 'down', 'left', or 'right'. To scroll within an element, use act(locator, 'scroll', 'down').",
                  "INVALID_DIRECTION"
               );
            } else {
               return this.page("input", "scroll", var2);
            }
         };
         case "on" -> (io.karatelabs.js.JavaInvokable) this::on;
         case "study" -> (io.karatelabs.js.JavaInvokable) this::study;
         case "reset" -> (io.karatelabs.js.JavaInvokable) var1x -> this.handleReset();
         case "help" -> (io.karatelabs.js.JavaInvokable) this::help;
         case "driver" -> this.driver;
         default -> null;
      };
   }

   @Override
   public Collection<String> jsKeys() {
      return VERBS;
   }

   private Map<String, Object> handleReset() {
      var var1 = this.handleClear("all");
      this.studyDossier = null;
      this.looksSinceAct = 0;
      Runnable var2 = this.recordOnReset;
      if (var2 != null) {
         try {
            var2.run();
         } catch (Exception var4) {
         }
      }

      return var1;
   }

   private Map<String, Object> look(Object... var1) {
      Object var2 = this.argsToMap(var1);
      String var3 = null;
      if (((java.util.Map)var2).get("expand") instanceof String var4) {
         var var13 = this.expandTrigger(var4);
         if (var13 != null) {
            return var13;
         }

         var3 = var4;
         var2 = new LinkedHashMap((Map)var2);
         ((java.util.Map)var2).remove("expand");
      }

      boolean var12 = Boolean.TRUE.equals(((java.util.Map)var2).get("full"));
      var var14 = this.performStandardLook((Map<String, Object>)var2);
      Map var6;
      if (!var12 && this.lastLookResult != null && Objects.equals(var14.get("url"), this.lastLookResult.get("url"))) {
         boolean var7 = !Boolean.FALSE.equals(((java.util.Map)var2).get("viewport"));
         var6 = this.computeLookDiff(this.lastLookResult, var14, var7);
      } else {
         var6 = var14;
      }

      this.lastLookResult = var14;
      if (++this.looksSinceAct >= 3) {
         String var15 = this.looksSinceAct
            + " look()s with no act() in between. look() lists interactables only — a panel that holds just VALUES (totals, a validation/results area, table cells) never appears here however often you look. Read it: bot.text('#selector') or bot.table('#selector'), and parse in the SAME block.";
         Object var8 = var6.get("hint");
         var6.put("hint", var8 == null ? var15 : var8 + " " + var15);
      }

      if (((java.util.Map)var2).get("scope") instanceof String var16 && !var16.isBlank() && (var14.get("elements") == null || ((List)var14.get("elements")).isEmpty())) {
         String var20;
         if (this.scopeResolves(var16)) {
            var6.put("scopeEmpty", var16);
            var20 = "scope '"
               + var16
               + "' resolved but holds NO interactables — it is a values-only region (totals, a read-only summary, a validation dock). Read it: bot.text('"
               + var16
               + "') or bot.table('"
               + var16
               + "').";
         } else {
            var6.put("scopeMissing", var16);
            var20 = "scope '"
               + var16
               + "' matched NO element — look() reads the region you name, it does not fall back to the page. Take the scope from a locator look() emitted, or call look() unscoped.";
         }

         Object var9 = var6.get("hint");
         var6.put("hint", var9 == null ? var20 : var20 + " " + var9);
      }

      if (var3 != null) {
         var6.put("expanded", var3);
         String var17 = "Expanded '"
            + var3
            + "' — the revealed items carry revealedBy:{owner, via} and the surface is left OPEN (act on an item, or send '{Escape}' to close).";
         Object var21 = var6.get("hint");
         var6.put("hint", var21 == null ? var17 : var17 + " " + var21);
      }

      if (var6.get("healed") instanceof List var18 && !var18.isEmpty()) {
         StringBuilder var23 = new StringBuilder();

         for (Object var10 : var18) {
            Map var11 = (Map)var10;
            var23.append("act() healed '").append(var11.get("from")).append("' → '").append(var11.get("to")).append("' — adopt the new locator. ");
         }

         Object var25 = var6.get("hint");
         var6.put("hint", var25 == null ? var23.toString().trim() : "" + var23 + var25);
         this.pendingHeals.subList(0, var18.size()).clear();
      }

      return var6;
   }

   private Map<String, Object> expandTrigger(String var1) {
      this.injectAgentLookJs();
      String var2 = var1.replace("\\", "\\\\").replace("'", "\\'");
      if (this.driver.script("window.__kjs.model.revealInfo('" + var2 + "')") instanceof Map var4) {
         if (var4.get("error") != null) {
            throw this.fail(var4.get("error") + io.karatelabs.ext.robot.RobotGuidanceFormatter.a(var1), "EXPAND_FAILED");
         } else if (Boolean.TRUE.equals(var4.get("open"))) {
            return null;
         } else {
            String var5 = "hover".equals(var4.get("via")) ? "hover" : "click";
            var var6 = this.act(var1, var5);
            return var6 != null && var6.containsKey("error") ? var6 : null;
         }
      } else {
         throw this.fail("expand failed for '" + var1 + "'" + io.karatelabs.ext.robot.RobotGuidanceFormatter.a(var1), "EXPAND_FAILED");
      }
   }

   private Map<String, Object> performStandardLook(Map<String, Object> var1) {
      var var2 = this.checkDialogBlocking();
      if (var2 != null) {
         return var2;
      } else {
         this.waitForPageStable();
         this.injectAgentLookJs();
         String var3 = this.buildOptionsJson(var1);
         Map var4 = (Map)this.driver.script("window.__kjs.model.pageState(" + var3 + ")");
         List var5 = var4 != null ? (List)var4.get("elements") : null;
         List var6 = var4 != null ? (List)var4.get("headings") : null;
         List var7 = var4 != null ? (List)var4.get("text") : null;
         Map var8 = var4 != null ? (Map)var4.get("scroll") : null;
         LinkedHashMap var9 = new LinkedHashMap();
         var9.put("url", this.driver.getUrl());
         var9.put("title", this.driver.getTitle());
         if (this.lastActionError != null) {
            var9.put("lastError", this.lastActionError);
            this.lastActionError = null;
         }

         if (!this.pendingHeals.isEmpty()) {
            var9.put("healed", List.copyOf(this.pendingHeals));
         }

         if (var6 != null && !var6.isEmpty()) {
            var9.put("headings", var6);
         }

         if (var7 != null && !var7.isEmpty()) {
            var9.put("text", var7);
         }

         List var10 = var4 != null ? (List)var4.get("content") : null;
         if (var10 != null && !var10.isEmpty()) {
            var9.put("content", var10);
         }

         ArrayList var11 = var5 != null ? new ArrayList(var5) : new ArrayList();
         List var12 = this.discoverIframes(var11);
         var9.put("elements", var11);
         if (!var12.isEmpty()) {
            var9.put("frames", var12);
         }

         if (var8 != null) {
            var9.put("scroll", var8);
         }

         List var13 = var4 != null ? (List)var4.get("scrollContainers") : null;
         if (var13 != null && !var13.isEmpty()) {
            var9.put("scrollContainers", var13);
         }

         if ((var4 != null ? var4.get("truncated") : null) instanceof Number var15 && var15.intValue() > 0) {
            var9.put("truncated", var15.intValue());
         }

         Object var18 = var4 != null ? var4.get("canvas") : null;
         if (var18 instanceof Number var16 && var16.intValue() > 0) {
            var9.put("canvas", var18);
         }

         Boolean var19 = (Boolean)this.driver.script("document.readyState !== 'complete'");
         if (Boolean.TRUE.equals(var19)) {
            var9.put("loading", true);
         }

         String var17 = io.karatelabs.ext.robot.RobotGuidanceFormatter.a(var9);
         if (var17 != null) {
            var9.put("hint", var17);
         }

         return var9;
      }
   }

   private static Long scrollY(Map<String, Object> var0) {
      return ((var0 == null ? null : var0.get("scroll")) instanceof Map var3 ? var3.get("y") : null) instanceof Number var4 ? var4.longValue() : null;
   }

   private static Long scrollDelta(Map<String, Object> var0, Map<String, Object> var1) {
      Long var2 = scrollY(var0);
      Long var3 = scrollY(var1);
      if (var2 != null && var3 != null) {
         long var4 = Math.abs(var3 - var2);
         return var4 >= 8L ? var4 : null;
      } else {
         return null;
      }
   }

   private Map<String, Object> computeLookDiff(Map<String, Object> var1, Map<String, Object> var2, boolean var3) {
      LinkedHashMap var4 = new LinkedHashMap();
      var4.put("url", var2.get("url"));
      var4.put("title", var2.get("title"));
      var4.put("diff", true);
      if (var2.containsKey("lastError")) {
         var4.put("lastError", var2.get("lastError"));
      }

      if (var2.containsKey("healed")) {
         var4.put("healed", var2.get("healed"));
      }

      if (var2.containsKey("scroll")) {
         var4.put("scroll", var2.get("scroll"));
      }

      if (!Objects.equals(var1.get("headings"), var2.get("headings"))) {
         var4.put("headings", var2.get("headings"));
      }

      if (!Objects.equals(var1.get("text"), var2.get("text"))) {
         var4.put("text", var2.get("text"));
      }

      List var5 = (List)(var1.getOrDefault("elements", List.of()));
      List var6 = (List)(var2.getOrDefault("elements", List.of()));
      LinkedHashMap var7 = new LinkedHashMap();

      for (Map var9 : (Iterable<Map>) (Iterable<?>) (var5)) {
         String var10 = (String)var9.get("locator");
         if (var10 != null) {
            var7.put(var10, var9);
         }
      }

      LinkedHashMap var23 = new LinkedHashMap();

      for (Map var26 : (Iterable<Map>) (Iterable<?>) (var6)) {
         String var11 = (String)var26.get("locator");
         if (var11 != null) {
            var23.put(var11, var26);
         }
      }

      ArrayList var25 = new ArrayList();
      ArrayList var27 = new ArrayList();
      ArrayList var28 = new ArrayList();
      int var12 = 0;

      for (Entry var14 : (Iterable<Entry>) (Iterable<?>) (var23.entrySet())) {
         String var15 = (String)var14.getKey();
         Map var16 = (Map)var14.getValue();
         Map var17 = (Map)var7.get(var15);
         if (var17 == null) {
            var25.add(var16);
         } else {
            Map var18 = diffElement(var17, var16);
            if (var18 != null) {
               LinkedHashMap var19 = new LinkedHashMap();
               var19.put("locator", var15);
               var19.put("changes", var18);
               var28.add(var19);
            } else {
               var12++;
            }
         }
      }

      for (String var31 : (Iterable<String>) (Iterable<?>) (var7.keySet())) {
         if (!var23.containsKey(var31)) {
            var27.add(var31);
         }
      }

      ArrayList var30 = new ArrayList(var6);
      var4.put("elements", var30);
      LinkedHashMap var32 = new LinkedHashMap();
      if (!var25.isEmpty()) {
         var32.put("added", var25);
      }

      if (!var27.isEmpty()) {
         var32.put("removed", var27);
      }

      if (!var28.isEmpty()) {
         var32.put("changed", var28);
      }

      var32.put("unchanged", var12);
      var4.put("changes", var32);
      if (var2.containsKey("loading")) {
         var4.put("loading", var2.get("loading"));
      }

      if (var2.containsKey("frames") && !Objects.equals(var1.get("frames"), var2.get("frames"))) {
         var4.put("frames", var2.get("frames"));
      }

      if (var2.containsKey("scrollContainers") && !Objects.equals(var1.get("scrollContainers"), var2.get("scrollContainers"))) {
         var4.put("scrollContainers", var2.get("scrollContainers"));
      }

      if (var2.containsKey("canvas")) {
         var4.put("canvas", var2.get("canvas"));
      }

      if (var2.containsKey("truncated")) {
         var4.put("truncated", var2.get("truncated"));
      }

      StringBuilder var33 = new StringBuilder();
      boolean var34 = var25.isEmpty() && var27.isEmpty() && var28.isEmpty();
      if (var34) {
         String var35 = io.karatelabs.ext.robot.RobotGuidanceFormatter.a(this.diagnostics.e());
         if (var35 != null) {
            var33.append(var35).append(' ');
         }

         var33.append("No changes since last look()");
         var33.append(
            ". If you need DATA VALUES (totals, fields, table cells): look() lists interactables only — read values with bot.text() or bot.table(selector). If your last action should have changed the page, it may have no-opped — check its response. Use look({full: true}) for the full (un-diffed) list."
         );
      } else {
         if (!var25.isEmpty()) {
            var33.append(var25.size()).append(" added");
         }

         if (!var27.isEmpty()) {
            if (!var33.isEmpty()) {
               var33.append(", ");
            }

            var33.append(var27.size()).append(" removed");
         }

         if (!var28.isEmpty()) {
            if (!var33.isEmpty()) {
               var33.append(", ");
            }

            var33.append(var28.size()).append(" changed");
         }

         var33.append(", ").append(var12).append(" unchanged");
         var33.append(". Use look({full: true}) for the full (un-diffed) list.");
         if (var3 && scrollDelta(var1, var2) != null) {
            var33.append(" The viewport moved (y ")
               .append(scrollY(var1))
               .append("→")
               .append(scrollY(var2))
               .append("), so items scrolling in/out of view read as added/removed here —")
               .append(" look({viewport: false}) lists the page regardless of scroll.");
         }
      }

      Object var39 = var4.get("scrollContainers");
      if (var39 instanceof List) {
         for (Object var43 : (List)var39) {
            if (var43 instanceof Map var20 && Boolean.FALSE.equals(var20.get("atBottom"))) {
               String var21 = io.karatelabs.ext.robot.RobotGuidanceFormatter.c(var20);
               String var22 = var20.get("locator") != null ? String.valueOf(var20.get("locator")) : "";
               var33.append(" More below the fold in '")
                  .append(var21)
                  .append("' — look({viewport: false}) shows ALL fields at once, look({scope: '")
                  .append(var22)
                  .append("'}) focuses this panel, or act('")
                  .append(var22)
                  .append("', 'scroll', 'down').");
               break;
            }
         }
      }

      if (var4.get("truncated") instanceof Number var37 && var37.intValue() > 0) {
         var33.append(' ').append(io.karatelabs.ext.robot.RobotGuidanceFormatter.b(var37.intValue()));
      }

      String var38 = io.karatelabs.ext.robot.RobotGuidanceFormatter.b(var4);
      if (var38 != null) {
         var33.append(' ').append(var38);
      }

      if (var4.get("canvas") instanceof Number var42 && var42.intValue() > 0) {
         var33.append(
            " This page draws content on a <canvas> — canvas pixels never appear in DOM text or elements; use bot.screenshot() to read them (the image arrives on your NEXT turn — screenshot NOW, report AFTER you have seen it)."
         );
      }

      var4.put("hint", var33.toString());
      return var4;
   }

   private static Map<String, Object> diffElement(Map<String, Object> var0, Map<String, Object> var1) {
      String[] var2 = new String[]{"name", "value", "checked", "options", "placeholder", "actions", "disabled", "occluded", "menu"};
      LinkedHashMap var3 = null;

      for (String var7 : var2) {
         Object var8 = var0.get(var7);
         Object var9 = var1.get(var7);
         if (!Objects.equals(var8, var9)) {
            if (var3 == null) {
               var3 = new LinkedHashMap();
            }

            var3.put(var7, var9);
         }
      }

      return var3;
   }

   private List<Map<String, Object>> discoverIframes(List<Map<String, Object>> var1) {
      ArrayList var2 = new ArrayList();

      List var3;
      try {
         var3 = (List)this.driver.script("window.__kjs.look.getVisibleFrames()");
      } catch (Exception var23) {
         String var5 = String.valueOf(var23.getMessage());
         if (var5.contains("__kjs.model")) {
            logger.warn("discoverIframes: {}", var5);
         } else {
            logger.debug("discoverIframes: getVisibleFrames failed: {}", var5);
         }

         return var2;
      }

      if (var3 != null && !var3.isEmpty()) {
         for (Map var26 : (Iterable<Map>) (Iterable<?>) (var3)) {
            String var6 = (String)var26.get("locator");
            if (var6 != null) {
               try {
                  this.driver.switchFrame(var6);
                  this.injectAgentLookJs();
                  Map var7 = (Map)this.driver.script("window.__kjs.model.pageState({})");
                  if (var7 != null) {
                     List var8 = (List)var7.get("elements");
                     int var9 = 0;
                     if (var8 != null) {
                        var9 = var8.size();

                        for (Map var11 : (Iterable<Map>) (Iterable<?>) (var8)) {
                           String var12 = (String)var11.get("locator");
                           LinkedHashMap var13 = new LinkedHashMap(var11);
                           var13.put("locator", var6 + " >> " + var12);
                           var13.put("frame", var6);
                           var1.add(var13);
                        }
                     }

                     LinkedHashMap var27 = new LinkedHashMap();
                     var27.put("locator", var6);
                     Object var28 = var26.get("name");
                     if (var28 != null && !var28.toString().isEmpty()) {
                        var27.put("name", var28);
                     }

                     var27.put("src", var26.get("src"));
                     var27.put("elements", var9);
                     Map var29 = (Map)var7.get("scroll");
                     if (var29 != null) {
                        var27.put("scroll", var29);
                     }

                     var2.add(var27);
                  }
               } catch (Exception var24) {
                  logger.debug("discoverIframes: failed for frame {}: {}", var6, var24.getMessage());
               } finally {
                  try {
                     this.driver.switchFrame(null);
                  } catch (Exception var22) {
                     logger.warn("discoverIframes: switchFrame(null) failed: {}", var22.getMessage());
                  }
               }
            }
         }

         return var2;
      } else {
         return var2;
      }
   }

   private Object find(Object... var1) {
      if (var1.length < 1) {
         throw this.fail("find requires locator argument", "INVALID_ARGS");
      } else {
         String var2 = String.valueOf(var1[0]);
         String[] var3 = parseFrameLocator(var2);
         String var4 = var3[0];
         String var5 = var3[1];
         Map var6 = var1.length > 1 && var1[1] instanceof Map ? (Map)var1[1] : Map.of();
         boolean var7 = !Boolean.FALSE.equals(var6.get("wait"));

         try {
            if (var4 != null) {
               this.driver.switchFrame(var4);
               this.injectAgentLookJs();
            }

            Object var9;
            try {
               if (var7) {
                  this.driver.waitFor(var5);
               }

               Boolean var8 = (Boolean)this.driver.script(Locators.existsJs(var5));
               if (Boolean.TRUE.equals(var8)) {
                  return new BotElementProxy(var2, this);
               }

               var9 = null;
            } finally {
               if (var4 != null) {
                  try {
                     this.driver.switchFrame(null);
                  } catch (Exception var18) {
                  }
               }
            }

            return var9;
         } catch (DriverException var20) {
            if (!var7) {
               return null;
            } else {
               this.logAgentError("find", var20.getMessage(), var2);
               return null;
            }
         }
      }
   }

   private Object findAll(Object... var1) {
      if (var1.length < 1) {
         throw this.fail("findAll requires locator argument", "INVALID_ARGS");
      } else {
         String var2 = String.valueOf(var1[0]);
         String[] var3 = parseFrameLocator(var2);
         String var4 = var3[0];
         String var5 = var3[1];

         try {
            if (var4 != null) {
               this.driver.switchFrame(var4);
               this.injectAgentLookJs();
            }

            BotElementArrayProxy var19;
            try {
               Object var6 = (List)this.driver.script(Locators.findAllJs(var5));
               if (var6 == null) {
                  var6 = List.of();
               }

               if (var4 != null) {
                  ArrayList var7 = new ArrayList();

                  for (String var9 : (Iterable<String>) (Iterable<?>) (var6)) {
                     var7.add(var4 + " >> " + var9);
                  }

                  var6 = var7;
               }

               var19 = new BotElementArrayProxy((List<String>)var6, this);
            } finally {
               if (var4 != null) {
                  try {
                     this.driver.switchFrame(null);
                  } catch (Exception var16) {
                  }
               }
            }

            return var19;
         } catch (Exception var18) {
            this.logAgentError("findAll", var18.getMessage(), var2);
            return new BotElementArrayProxy(List.of(), this);
         }
      }
   }

   private Object page(Object... var1) {
      if (var1.length < 2) {
         throw this.fail("page requires namespace and action arguments", "INVALID_ARGS");
      } else {
         String var2 = String.valueOf(var1[0]);
         String var3 = String.valueOf(var1[1]);
         Object var4 = var1.length > 2 ? var1[2] : null;
         if (!"dialog".equals(var2)) {
            var var5 = this.checkDialogBlocking();
            if (var5 != null) {
               return var5;
            }
         }
         return switch (var2) {
            case "nav" -> this.handleNav(var3);
            case "input" -> this.handleInput(var3, var4);
            case "query" -> this.handleQuery(var3, var4);
            case "window" -> this.handleWindow(var3, var4);
            case "clear" -> this.handleClear(var3);
            case "dialog" -> this.handleDialog(var3, var4);
            case "net" -> this.handleNet(var3, var4);
            default -> throw this.fail("Unknown namespace: " + var2, "INVALID_NAMESPACE");
         };
      }
   }

   private Map<String, Object> handleNav(String var1) {
      this.looksSinceAct = 0;

      try {
         switch (var1) {
            case "back":
               this.driver.script("history.back()");
               break;
            case "forward":
               this.driver.script("history.forward()");
               break;
            case "reload":
               this.driver.script("location.reload()");
               break;
            default:
               throw this.fail("Unknown nav action: " + var1, "INVALID_ACTION");
         }

         this.softWaitForReadyState("document.readyState === 'complete'");
         LinkedHashMap var2 = new LinkedHashMap();
         var2.put("url", this.driver.getUrl());
         var2.put("title", this.driver.getTitle());
         var2.put("hint", "Call look() to see page elements.");
         return var2;
      } catch (Exception var4) {
         throw this.fail(var4.getMessage(), "NAV_FAILED");
      }
   }

   private Map<String, Object> handleInput(String var1, Object var2) {
      this.looksSinceAct = 0;

      try {
         switch (var1) {
            case "key":
               String var14 = String.valueOf(var2);
               validateKeyToken(var14);
               String var15 = mapKeyName(var14);
               String var17 = mapKeyForDom(var14);
               String var19 = mapCodeForDom(var14);
               this.driver
                  .script(
                     "(function(){ var el = document.activeElement; if (el && el !== document.body) { ['keydown','keypress','keyup'].forEach(function(t){ el.dispatchEvent(new KeyboardEvent(t, {key:'"
                        + var17
                        + "', code:'"
                        + var19
                        + "', bubbles:true, cancelable:true})); }); } else { ['keydown','keyup'].forEach(function(t){ document.dispatchEvent(new KeyboardEvent(t, {key:'"
                        + var17
                        + "', code:'"
                        + var19
                        + "', bubbles:true, cancelable:true})); }); }})()"
                  );
               this.driver.keys().press(var15);
               break;
            case "type":
               this.driver.keys().type(String.valueOf(var2));
               break;
            case "scroll":
               String var5 = var2 != null ? String.valueOf(var2).toLowerCase() : "down";
               boolean var6 = "left".equals(var5) || "right".equals(var5);
               if (var6) {
                  int var7 = "left".equals(var5) ? -1 : 1;
                  String var8 = "(function() {\n    var amount = Math.round(window.innerWidth * 0.8) * %d;\n    var scrolled = false;\n    var beforeX = window.scrollX;\n    window.scrollBy(amount, 0);\n    if (window.scrollX !== beforeX) {\n        scrolled = true;\n    }\n    return {\n        scrolled: scrolled,\n        scrollX: Math.round(window.scrollX),\n        scrollWidth: document.documentElement.scrollWidth,\n        viewportWidth: window.innerWidth,\n        atLeft: window.scrollX <= 0,\n        atRight: window.scrollX + window.innerWidth >= document.documentElement.scrollWidth - 5\n    };\n})()\n"
                     .formatted(var7);
                  if (this.driver.script(var8) instanceof Map var10) {
                     return var10;
                  }
               } else {
                  int var16 = "up".equals(var5) ? -1 : 1;
                  String var18 = "(function() {\n    var amount = Math.round(window.innerHeight * 0.8) * %d;\n    var se = document.scrollingElement || document.documentElement;\n    var beforeY = se.scrollTop;\n    window.scrollBy({top: amount, behavior: 'instant'});\n    // If window didn't scroll, find scrollable container\n    if (se.scrollTop === beforeY) {\n        var candidates = [];\n        var priority = document.querySelectorAll('[role=\"main\"], main, #content, .main-content');\n        for (var i = 0; i < priority.length; i++) {\n            var el = priority[i];\n            if (el.scrollHeight > el.clientHeight + 10) candidates.push(el);\n        }\n        if (candidates.length === 0) {\n            var all = document.querySelectorAll('*');\n            for (var i = 0; i < all.length; i++) {\n                var el = all[i];\n                if (el.scrollHeight > el.clientHeight + 10) {\n                    var style = getComputedStyle(el);\n                    if (style.overflowY === 'auto' || style.overflowY === 'scroll') candidates.push(el);\n                }\n            }\n        }\n        var best = null;\n        for (var i = 0; i < candidates.length; i++) {\n            if (!best || candidates[i].scrollHeight > best.scrollHeight) best = candidates[i];\n        }\n        if (best) best.scrollBy({top: amount, behavior: 'instant'});\n    }\n})()\n"
                     .formatted(var16);
                  this.driver.script(var18);

                  try {
                     Thread.sleep(50L);
                  } catch (InterruptedException var12) {
                  }

                  String var20 = "(function() {\n    var se = document.scrollingElement || document.documentElement;\n    var y = se.scrollTop;\n    return {\n        scrolled: true,\n        scrollY: Math.round(y),\n        scrollHeight: se.scrollHeight,\n        viewportHeight: window.innerHeight,\n        atTop: y <= 0,\n        atBottom: y + window.innerHeight >= se.scrollHeight - 5\n    };\n})()\n";
                  if (this.driver.script(var20) instanceof Map var11) {
                     return var11;
                  }
               }
               break;
            default:
               throw this.fail("Unknown input action: " + var1, "INVALID_ACTION");
         }

         return Map.of("success", true);
      } catch (Exception var13) {
         throw this.fail(var13.getMessage(), "INPUT_FAILED");
      }
   }

   private Object handleQuery(String var1, Object var2) {
      try {
         return switch (var1) {
            case "url" -> this.driver.getUrl();
            case "title" -> this.driver.getTitle();
            case "cookies" -> this.driver.script("document.cookie");
            case "tabs" -> this.listTabs();
            default -> throw this.fail("Unknown query action: " + var1, "INVALID_ACTION");
         };
      } catch (Exception var5) {
         throw this.fail(var5.getMessage(), "QUERY_FAILED");
      }
   }

   private Map<String, Object> listTabs() {
      String var1 = this.driver.getUrl();
      String var2 = this.driver.getTitle();
      ArrayList var3 = new ArrayList();
      if (this.driver instanceof CdpDriver var4) {
         var var10 = var4.getTargetInfos();

         for (int var6 = 0; var6 < var10.size(); var6++) {
            Map var7 = (Map)var10.get(var6);
            LinkedHashMap var8 = new LinkedHashMap();
            var8.put("index", var6);
            var8.put("targetId", var7.get("targetId"));
            var8.put("url", var7.get("url"));
            var8.put("title", var7.get("title"));
            var8.put("active", var7.get("active"));
            var3.add(var8);
         }
      } else {
         var var11 = this.driver.getPages();

         for (int var12 = 0; var12 < var11.size(); var12++) {
            LinkedHashMap var13 = new LinkedHashMap();
            var13.put("index", var12);
            var13.put("targetId", var11.get(var12));
            var3.add(var13);
         }
      }

      LinkedHashMap var9 = new LinkedHashMap();
      var9.put("active", Map.of("url", var1, "title", var2));
      var9.put("tabs", var3);
      var9.put("count", var3.size());
      return var9;
   }

   private Map<String, Object> handleWindow(String var1, Object var2) {
      if ("switchTab".equals(var1)) {
         return this.switchTab(var2);
      } else {
         try {
            switch (var1) {
               case "viewport":
                  if (var2 instanceof Map var5) {
                     int var6 = ((Number)var5.get("width")).intValue();
                     int var7 = ((Number)var5.get("height")).intValue();
                     this.driver.setDimensions(Map.of("width", var6, "height", var7));
                  }
                  break;
               case "maximize":
                  this.driver.maximize();
                  break;
               case "minimize":
                  this.driver.minimize();
                  break;
               case "fullscreen":
                  this.driver.fullscreen();
                  break;
               case "normal":
                  this.driver.setDimensions(Map.of("windowState", "normal"));
                  break;
               case "close":
               case "closeTab":
                  this.driver.close();
                  this.lastLookResult = null;
                  this.injectAgentLookJs();
                  break;
               default:
                  throw this.fail("Unknown window action: " + var1, "INVALID_ACTION");
            }

            return Map.of("success", true);
         } catch (Exception var8) {
            throw this.fail(var8.getMessage(), "WINDOW_FAILED");
         }
      }
   }

   private Map<String, Object> switchTab(Object var1) {
      if (var1 == null) {
         throw this.fail("switchTab requires a value: tab index, URL/title substring, or targetId", "INVALID_ARGS");
      } else {
         String var2 = this.driver.getUrl();
         String var3 = this.driver.getTitle();

         try {
            if (var1 instanceof Number var4) {
               this.driver.switchPage(var4.intValue());
            } else {
               String var5 = String.valueOf(var1);
               var var6 = this.driver.getPages();
               if (var6.contains(var5)) {
                  this.driver.switchPageById(var5);
               } else {
                  this.driver.switchPage(var5);
               }
            }
         } catch (Exception var7) {
            throw this.fail("switchTab failed: " + var7.getMessage(), "WINDOW_FAILED");
         }

         String var8 = this.driver.getUrl();
         String var9 = this.driver.getTitle();
         if (!(var1 instanceof Number) && var2.equals(var8) && var3.equals(var9)) {
            throw this.fail(
               "switchTab '"
                  + var1
                  + "' matched only the current tab ("
                  + var2
                  + "). Use a more specific substring that identifies a DIFFERENT tab, or call page('query', 'tabs') to list open tabs.",
               "WINDOW_FAILED"
            );
         } else {
            this.lastLookResult = null;
            this.injectAgentLookJs();
            LinkedHashMap var10 = new LinkedHashMap();
            var10.put("success", true);
            var10.put("url", var8);
            var10.put("title", var9);
            var10.put("hint", "Switched tabs. Call look() to see the new page.");
            return var10;
         }
      }
   }

   private Map<String, Object> handleClear(String var1) {
      try {
         switch (var1) {
            case "cookies":
               this.driver.clearCookies();
               break;
            case "storage":
               this.clearStorage();
               break;
            case "all":
               this.driver.clearCookies();
               this.clearStorage();
               this.driver.navigate("about:blank");
               break;
            default:
               throw this.fail("Unknown clear action: " + var1, "INVALID_ACTION");
         }

         LinkedHashMap var2 = new LinkedHashMap();
         var2.put("success", true);
         var2.put("hint", "all".equals(var1) ? "Browser cleared. Use go(url) to navigate." : "Cleared " + var1 + ".");
         return var2;
      } catch (Exception var4) {
         throw this.fail(var4.getMessage(), "CLEAR_FAILED");
      }
   }

   private void clearStorage() {
      this.driver.script("try { localStorage.clear(); sessionStorage.clear(); } catch (e) { /* storage disabled on this origin */ }");
   }

   private Map<String, Object> handleDialog(String var1, Object var2) {
      Dialog var3 = this.driver.getDialog();
      if (var3 == null) {
         throw this.fail("No dialog is currently open", "NO_DIALOG");
      } else {
         return switch (var1) {
            case "accept" -> {
               if (var2 != null && "prompt".equals(var3.getType())) {
                  var3.accept(String.valueOf(var2));
               } else {
                  var3.accept();
               }

               yield Map.of("success", true, "action", "accepted", "type", var3.getType());
            }
            case "dismiss" -> {
               var3.dismiss();
               yield Map.of("success", true, "action", "dismissed", "type", var3.getType());
            }
            case "info" -> this.buildDialogInfo(var3);
            default -> throw this.fail("Unknown dialog action: " + var1 + ". Use 'accept', 'dismiss', or 'info'", "INVALID_ACTION");
         };
      }
   }

   private static String unwrapKey(String var0) {
      if (var0 == null) {
         return "";
      } else {
         String var1 = var0.trim();
         return var1.length() >= 2 && var1.startsWith("{") && var1.endsWith("}") ? var1.substring(1, var1.length() - 1) : var1;
      }
   }

   static String keyId(String var0) {
      return unwrapKey(var0).toUpperCase().replace("_", "");
   }

   static void validateKeyToken(String var0) {
      if (var0 != null) {
         String var1 = var0.trim();
         if (var1.length() >= 2 && var1.startsWith("{") && var1.endsWith("}") && !KNOWN_KEYS.contains(keyId(var1))) {
            throw new DriverException(
               "Unknown key token '"
                  + var1
                  + "' [INVALID_KEY]. Recognised: {Enter}, {Tab}, {Escape}, {Backspace}, {Delete}, {Space}, {ArrowUp}, {ArrowDown}, {ArrowLeft}, {ArrowRight}, {PageUp}, {PageDown}, {Home}, {End}. For literal text, omit the curly braces."
            );
         }
      }
   }

   static String mapKeyName(String var0) {
      String var1 = keyId(var0);

      return switch (var1) {
         case "ENTER", "RETURN" -> "\ue007";
         case "TAB" -> "\ue004";
         case "ESCAPE", "ESC" -> "\ue00c";
         case "BACKSPACE" -> "\ue003";
         case "DELETE" -> "\ue017";
         case "SPACE" -> "\ue00d";
         case "UP", "ARROWUP" -> "\ue013";
         case "DOWN", "ARROWDOWN" -> "\ue015";
         case "LEFT", "ARROWLEFT" -> "\ue012";
         case "RIGHT", "ARROWRIGHT" -> "\ue014";
         case "PAGEUP" -> "\ue00e";
         case "PAGEDOWN" -> "\ue00f";
         case "HOME" -> "\ue011";
         case "END" -> "\ue010";
         default -> unwrapKey(var0);
      };
   }

   static String mapKeyForDom(String var0) {
      String var1 = keyId(var0);

      return switch (var1) {
         case "ENTER", "RETURN" -> "Enter";
         case "TAB" -> "Tab";
         case "ESCAPE", "ESC" -> "Escape";
         case "BACKSPACE" -> "Backspace";
         case "DELETE" -> "Delete";
         case "SPACE" -> " ";
         case "UP", "ARROWUP" -> "ArrowUp";
         case "DOWN", "ARROWDOWN" -> "ArrowDown";
         case "LEFT", "ARROWLEFT" -> "ArrowLeft";
         case "RIGHT", "ARROWRIGHT" -> "ArrowRight";
         case "PAGEUP" -> "PageUp";
         case "PAGEDOWN" -> "PageDown";
         case "HOME" -> "Home";
         case "END" -> "End";
         default -> var0;
      };
   }

   static String mapCodeForDom(String var0) {
      String var1 = keyId(var0);

      return switch (var1) {
         case "ENTER", "RETURN" -> "Enter";
         case "TAB" -> "Tab";
         case "ESCAPE", "ESC" -> "Escape";
         case "BACKSPACE" -> "Backspace";
         case "DELETE" -> "Delete";
         case "SPACE" -> "Space";
         case "UP", "ARROWUP" -> "ArrowUp";
         case "DOWN", "ARROWDOWN" -> "ArrowDown";
         case "LEFT", "ARROWLEFT" -> "ArrowLeft";
         case "RIGHT", "ARROWRIGHT" -> "ArrowRight";
         case "PAGEUP" -> "PageUp";
         case "PAGEDOWN" -> "PageDown";
         case "HOME" -> "Home";
         case "END" -> "End";
         default -> {
            String var3 = unwrapKey(var0);
            yield var3.isEmpty() ? "" : "Key" + var3.toUpperCase().charAt(0);
         }
      };
   }

   private Object on(Object... var1) {
      if (var1.length < 1) {
         throw this.fail("on requires event argument", "INVALID_ARGS");
      } else {
         String var2 = String.valueOf(var1[0]);
         Map var3 = var1.length > 1 && var1[1] instanceof Map ? (Map)var1[1] : Map.of();
         return new BotSubscription(var2, var3, this);
      }
   }

   private Object capture(Object... var1) {
      if (var1.length < 1) {
         throw this.fail("capture requires type argument", "INVALID_ARGS");
      } else {
         String var2 = String.valueOf(var1[0]);
         Map var3 = var1.length > 1 ? this.argsToMap(new Object[]{var1[1]}) : Map.of();

         return switch (var2) {
            case "screenshot" -> {
               Object var6 = var3.get("label") != null ? var3.get("label") : var3.get("title");
               yield this.captureScreenshot(var3, var6 != null ? String.valueOf(var6) : null);
            }
            case "logs" -> this.captureLogs();
            case "network", "net" -> this.diagnostics.a(var3);
            case "console" -> this.diagnostics.b(var3);
            case "perf" -> this.capturePerf();
            case "pdf" -> this.capturePdf(var3);
            case "clipboard" -> this.captureClipboard();
            case "video" -> this.captureVideo(var1.length > 1 ? String.valueOf(var1[1]) : "start");
            default -> throw this.fail("Unknown capture type: " + var2, "INVALID_TYPE");
         };
      }
   }

   private Map<String, Object> captureScreenshot(Map<String, Object> var1, String var2) {
      boolean var3 = Boolean.TRUE.equals(var1.get("fullPage"));
      boolean var4 = Boolean.TRUE.equals(var1.get("locators"));
      if (var3 && var4) {
         throw this.fail(
            "screenshot({fullPage: true}) cannot be combined with {locators: true} — locator coordinates are viewport-relative. Take them as two calls.",
            "INVALID_ARGS"
         );
      } else {
         try {
            this.injectAgentLookJs();

            try {
               this.driver.activate();
            } catch (Exception var15) {
               logger.debug("bringToFront before screenshot skipped: {}", var15.getMessage());
            }

            byte[] var5 = var3 ? this.driver.screenshotFullPage(false) : this.driver.screenshot();
            if (this.screenshotConsumer != null) {
               this.screenshotConsumer.accept(var5);
            }

            String var6 = var2 != null && !var2.isBlank() ? var2 : this.driver.getTitle();
            if (var6 == null || var6.isBlank()) {
               var6 = "screenshot";
            }

            Path var7 = this.screenshotsDirSupplier != null ? this.screenshotsDirSupplier.get() : this.screenshotsDir;
            String var8 = null;
            if (var7 != null) {
               try {
                  Files.createDirectories(var7);
                  String var9 = var6.replaceAll("[^a-zA-Z0-9-]", "-").replaceAll("-{2,}", "-").toLowerCase();
                  if (var9.length() > 40) {
                     var9 = var9.substring(0, 40);
                  }

                  long var10 = Files.list(var7).filter(var0 -> var0.getFileName().toString().endsWith(".png")).count();
                  var8 = String.format("%03d-%s.png", var10 + 1L, var9);
                  Path var12 = var7.resolve(var8);
                  Files.write(var12, var5);
                  String var13 = "{\"filename\":\""
                     + var8
                     + "\",\"timestamp\":"
                     + System.currentTimeMillis()
                     + ",\"title\":\""
                     + var6.replace("\"", "'")
                     + "\"}\n";
                  Files.writeString(var7.resolve("screenshots.jsonl"), var13, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                  if (this.screenshotSavedListener != null) {
                     this.screenshotSavedListener.accept(var8, var6);
                  }
               } catch (Exception var14) {
                  logger.debug("screenshot save failed: {}", var14.getMessage());
               }
            }

            LinkedHashMap var17 = new LinkedHashMap();
            var17.put("captured", true);
            var17.put("title", var6);
            if (var8 != null) {
               var17.put("filename", var8);
               String var18 = this.screenshotUrlResolver != null ? this.screenshotUrlResolver.apply(var8) : null;
               if (var18 != null) {
                  var17.put("url", var18);
                  var17.put("parts", List.of(JsApi.imagePart(var18, "image/png")));
               }
            }

            if (var4) {
               Map var19 = (Map)this.driver.script("window.__kjs.model.getLocatorBounds()");
               var var11 = this.driver.getDimensions();
               var17.put("width", var11.get("width"));
               var17.put("height", var11.get("height"));
               var17.put("locators", var19 != null ? var19 : Map.of());
               this.screenshotHintEmitted = true;
            } else if (!this.screenshotHintEmitted) {
               var17.put("hint", "Use bot.screenshot({locators: true}) to get element coordinates for vision-to-DOM mapping.");
               this.screenshotHintEmitted = true;
            }

            return var17;
         } catch (Exception var16) {
            throw this.fail(var16.getMessage(), "SCREENSHOT_FAILED");
         }
      }
   }

   private Map<String, Object> captureLogs() {
      LinkedHashMap var1 = new LinkedHashMap();

      List var2;
      try {
         var2 = (List)this.driver.script("__kjs.getConsoleLogs ? __kjs.getConsoleLogs() : []");
         if (var2 == null) {
            var2 = List.of();
         }

         var1.put("kjs", this.driver.script("__kjs.getLogs ? __kjs.getLogs() : []"));
      } catch (Exception var4) {
         var2 = List.of();
         var1.put("kjs", List.of());
      }

      var1.putAll(this.diagnostics.a(var2));
      String var3 = io.karatelabs.ext.robot.RobotGuidanceFormatter.a((List<Map<String, Object>>)var1.get("console"));
      if (var3 != null) {
         var1.put("hint", var3);
      }

      return var1;
   }

   @Override
   public void onEvent(String var1, Map<String, Object> var2) {
      if (!this.diagnostics.a(var1, var2)) {
         switch (var1) {
            case "Runtime.executionContextCreated":
               if (var2.get("context") instanceof Map var16
                  && var16.get("id") instanceof Number var17
                  && var16.get("auxData") instanceof Map var21
                  && var21.get("frameId") instanceof String var10) {
                  this.ctxToFrame.put(var17.longValue(), var10);
               }
               break;
            case "Runtime.executionContextDestroyed":
               if (var2.get("executionContextId") instanceof Number var13) {
                  this.ctxToFrame.remove(var13.longValue());
               }
               break;
            case "Runtime.executionContextsCleared":
               this.ctxToFrame.clear();
               this.frameOwnerCache.clear();
               this.frameResolveTried.clear();
               break;
            case "Runtime.bindingCalled":
               io.karatelabs.ext.robot.BotDriver.RecordingEventListener var5 = this.recordBinding;
               if (var5 != null && "__karateRecord".equals(var2.get("name"))) {
                  Object var6 = var2.get("payload");
                  if (var6 != null) {
                     String var7 = var2.get("executionContextId") instanceof Number var8 ? this.ctxToFrame.get(var8.longValue()) : null;
                     String var18 = null;
                     if (var7 != null) {
                        var18 = this.frameOwnerCache.get(var7);
                        if (var18 == null && this.frameResolveTried.add(var7)) {
                           this.frameResolverExecutor().submit(() -> {
                              String var2x = this.resolveFrameOwnerSelector(var7);
                              if (var2x != null) {
                                 this.frameOwnerCache.put(var7, var2x);
                              }
                           });
                        }
                     }

                     try {
                        var5.onRecord(var6.toString(), var7, var18);
                     } catch (Exception var12) {
                        logger.debug("recorder binding forward failed: {}", var12.getMessage());
                     }
                  }
               }
         }
      }
   }

   public void startRecording(io.karatelabs.ext.robot.BotDriver.RecordingEventListener var1, Runnable var2) {
      this.startRecording(var1, var2, false);
   }

   public void startRecording(io.karatelabs.ext.robot.BotDriver.RecordingEventListener var1, Runnable var2, boolean var3) {
      if (this.driver instanceof CdpDriver var4) {
         this.recordOnReset = var2;
         this.recordBinding = var1;
         var4.addBinding("__karateRecord");
         this.injectAgentLookJs();
         var4.addInitScript("recorder", RECORDER_JS, "agent-model");
         if (var3) {
            var4.addInitScript("recorder-delta", "window.__kjs && window.__kjs.recorder && (window.__kjs.recorder.captureDelta = true);", "recorder");
         }

         try {
            this.driver.script("window.__kjs && window.__kjs.recorder && window.__kjs.recorder.start()");
            if (var3) {
               this.driver.script("window.__kjs && window.__kjs.recorder && (window.__kjs.recorder.captureDelta = true)");
            }
         } catch (Exception var6) {
            logger.debug("recorder.js start in current doc skipped: {}", var6.getMessage());
         }

         logger.debug("recording armed{}", var3 ? " (advanced: reveal-delta capture)" : "");
      } else {
         logger.warn("recording needs a CDP driver — capture not armed");
      }
   }

   public void stopRecording() {
      this.stopRecording(true, null);
   }

   public void stopRecording(boolean var1, Runnable var2) {
      IllegalStateException var3 = null;
      if (var1 && this.recordBinding != null && this.driver instanceof CdpDriver) {
         try {
            this.driver.script("window.__kjs && window.__kjs.recorder && window.__kjs.recorder.flush()");
         } catch (Exception var7) {
            var3 = new IllegalStateException("the page's pending action was not flushed: " + var7.getMessage(), var7);
         }
      }

      Consumer<Runnable> var4 = this.barrier != null ? this.barrier : (this.driver instanceof CdpDriver var5 ? var5::awaitEventsDispatched : null);
      if (var4 != null) {
         try {
            var4.accept(var2);
            var2 = null;
         } catch (RuntimeException var8) {
            if (var3 == null) {
               var3 = new IllegalStateException("the event dispatch barrier did not run: " + var8.getMessage(), var8);
            }
         }
      }

      if (var2 != null) {
         var2.run();
      }

      this.disarmRecording();
      if (var3 != null) {
         throw var3;
      }
   }

   private void disarmRecording() {
      this.recordBinding = null;
      this.recordOnReset = null;
      if (this.driver instanceof CdpDriver var1) {
         try {
            this.driver.script("window.__kjs && window.__kjs.recorder && window.__kjs.recorder.stop()");
         } catch (Exception var5) {
         }

         try {
            var1.removeInitScript("recorder");
            var1.removeInitScript("recorder-delta");
         } catch (Exception var4) {
            logger.debug("recorder.js uninstall skipped: {}", var4.getMessage());
         }

         try {
            var1.removeBinding("__karateRecord");
         } catch (Exception var3) {
            logger.debug("__karateRecord unbind skipped: {}", var3.getMessage());
         }

         this.frameOwnerCache.clear();
         this.frameResolveTried.clear();
         ExecutorService var6 = this.frameResolver;
         if (var6 != null) {
            var6.shutdownNow();
            this.frameResolver = null;
         }

         logger.debug("recording disarmed");
      }
   }

   public boolean isRecording() {
      return this.recordBinding != null;
   }

   private synchronized ExecutorService frameResolverExecutor() {
      if (this.frameResolver == null) {
         this.frameResolver = Executors.newSingleThreadExecutor(var0 -> {
            Thread var1 = new Thread(var0, "bot-frame-resolver");
            var1.setDaemon(true);
            return var1;
         });
      }

      return this.frameResolver;
   }

   private String resolveFrameOwnerSelector(String var1) {
      if (this.driver instanceof CdpDriver var2) {
         try {
            Integer var5 = var2.getFrameOwnerBackendNodeId(var1);
            return var5 == null ? null : selectorFromNode(var2.describeNode(var5));
         } catch (Exception var4) {
            logger.debug("frame owner resolution failed for {}: {}", var1, var4.getMessage());
            return null;
         }
      } else {
         return null;
      }
   }

   private static String selectorFromNode(Map<String, Object> var0) {
      if (var0 == null) {
         return null;
      } else {
         String var1 = var0.get("nodeName") instanceof String var2 && !var2.isBlank() ? var2.toLowerCase() : "iframe";
         LinkedHashMap var9 = new LinkedHashMap();
         if (var0.get("attributes") instanceof List var10) {
            for (byte var12 = 0; var12 + 1 < var10.size(); var12 += 2) {
               var9.put(String.valueOf(var10.get(var12)), String.valueOf(var10.get(var12 + 1)));
            }
         }

         String var11 = (String)var9.get("id");
         if (var11 != null && !var11.isBlank() && !var11.matches("(?i)^(ext-|x-|ng-|ember|react-|vue-|_).*") && var11.matches("[A-Za-z][\\w-]*")) {
            return "#" + var11;
         } else {
            for (String var7 : new String[]{"data-testid", "data-test-id", "name", "title"}) {
               String var8 = (String)var9.get(var7);
               if (var8 != null && !var8.isBlank()) {
                  return var1 + "[" + (var7.equals("data-test-id") ? "data-testid" : var7) + "=\"" + var8 + "\"]";
               }
            }

            return var1;
         }
      }
   }

   private Object capturePerf() {
      String var1 = "(function () {\n  var nav = (performance.getEntriesByType('navigation') || [])[0] || {};\n  var paint = performance.getEntriesByType('paint') || [];\n  var fcp = paint.filter(function (p) { return p.name === 'first-contentful-paint'; })[0];\n  var res = performance.getEntriesByType('resource') || [];\n  var bytes = 0; res.forEach(function (r) { bytes += (r.transferSize || 0); });\n  return {\n    url: location.href,\n    ttfbMs: Math.round(nav.responseStart || 0),\n    domContentLoadedMs: Math.round(nav.domContentLoadedEventEnd || 0),\n    loadMs: Math.round(nav.loadEventEnd || 0),\n    fcpMs: fcp ? Math.round(fcp.startTime) : null,\n    resources: res.length,\n    transferredBytes: bytes\n  };\n})()\n";

      try {
         Object var2 = this.driver.script(var1);
         if (var2 instanceof Map) {
            return var2;
         } else {
            throw this.fail("perf metrics unavailable on this page", "PERF_UNAVAILABLE");
         }
      } catch (Exception var3) {
         throw this.fail(var3.getMessage(), "PERF_FAILED");
      }
   }

   private Map<String, Object> handleNet(String var1, Object var2) {
      try {
         switch (var1) {
            case "clear":
               this.diagnostics.f();
               return Map.of("success", true, "hint", "network + console capture buffers cleared");
            case "throttle":
               return this.emulateNetwork(String.valueOf(var2));
            case "offline":
               return this.emulateNetwork("offline");
            case "online":
               return this.emulateNetwork("online");
            default:
               throw this.fail("Unknown net action: " + var1 + " (clear|throttle|offline|online)", "INVALID_ACTION");
         }
      } catch (Exception var5) {
         throw this.fail(var5.getMessage(), "NET_FAILED");
      }
   }

   private Map<String, Object> emulateNetwork(String var1) {
      if (this.driver instanceof CdpDriver var2) {
         boolean var12 = "offline".equalsIgnoreCase(var1);
         double var4 = 0.0;
         double var6 = -1.0;
         double var8 = -1.0;
         String var10 = var1 == null ? "online" : var1.toLowerCase();
         switch (var10) {
            case "slow3g":
               var4 = 400.0;
               var6 = 51200.0;
               var8 = 51200.0;
               break;
            case "fast3g":
               var4 = 150.0;
               var6 = 209715.2;
               var8 = 96000.0;
         }

         var2.getCdpClient()
            .method("Network.emulateNetworkConditions")
            .param("offline", var12)
            .param("latency", var4)
            .param("downloadThroughput", var6)
            .param("uploadThroughput", var8)
            .send();
         return Map.of("success", true, "applied", var1 == null ? "online" : var1);
      } else {
         throw this.fail("network emulation needs the CDP driver", "UNSUPPORTED");
      }
   }

   private Map<String, Object> capturePdf(Map<String, Object> var1) {
      throw this.fail("PDF capture not yet implemented", "NOT_IMPLEMENTED");
   }

   private Map<String, Object> captureClipboard() {
      try {
         Object var1 = this.driver.script("navigator.clipboard.readText()");
         LinkedHashMap var2 = new LinkedHashMap();
         var2.put("text", var1 != null ? var1 : "");
         return var2;
      } catch (Exception var3) {
         throw this.fail("Failed to read clipboard: " + var3.getMessage(), "CLIPBOARD_ERROR");
      }
   }

   private Map<String, Object> captureVideo(String var1) {
      LinkedHashMap var2 = new LinkedHashMap();
      if ("stop".equals(var1)) {
         if (this.videoProcess != null && this.videoProcess.isAlive()) {
            io.karatelabs.ext.robot.VideoRecorder.a(this.videoProcess);
            this.videoProcess = null;
            var2.put("recording", false);
            var2.put("path", "video.mp4");
            return var2;
         } else {
            var2.put("recording", false);
            var2.put("message", "not recording");
            return var2;
         }
      } else if (this.videoProcess != null && this.videoProcess.isAlive()) {
         var2.put("recording", true);
         var2.put("message", "already recording");
         return var2;
      } else {
         Path var3 = this.screenshotsDirSupplier != null ? this.screenshotsDirSupplier.get() : this.screenshotsDir;
         if (var3 == null) {
            var2.put("recording", false);
            var2.put("error", "no screenshots/run dir configured for this bot");
            return var2;
         } else {
            Path var4 = var3.getParent().resolve("video.mp4");
            this.videoProcess = io.karatelabs.ext.robot.VideoRecorder.a(var4);
            if (this.videoProcess != null) {
               var2.put("recording", true);
            } else {
               var2.put("recording", false);
               var2.put("error", "failed to start ffmpeg");
            }

            return var2;
         }
      }
   }

   private Object help(Object... var1) {
      return RobotFactory.helpStatic(var1);
   }

   private Object study(Object... var1) {
      Map var2 = null;

      for (Object var6 : var1) {
         if (var6 instanceof Map var7) {
            var2 = var7;
            break;
         }
      }

      return var2 == null ? this.openStudy() : this.closeStudy(var2);
   }

   private Map<String, Object> openStudy() {
      LinkedHashMap var1 = new LinkedHashMap();
      var1.put("phase", "study");
      var1.put(
         "recipe",
         "Take stock before driving: explore this page (look(), look({viewport:false}), eval()), identify the repeating patterns (how menus/carets open, how fields are named, where validation errors surface, date/picker widget choreographies), and author a helper or two for the fiddly bits — DECLARE each helper in a js block (`var readScore = function(){ … };`); your vars persist across turns, so that is what makes it callable later. Then submit a dossier with bot.study({...}) — its machine fields make look()/act() better for the REST of the session automatically, and a verified helper can be promoted to checks/helpers.js If a `pack` is included below, this app is already known — adopt its choreographies/helpers and only verify what is page-specific."
      );
      var1.put("dossier", studyTemplate());
      this.injectAgentLookJs();
      String var2 = null;

      try {
         if (this.driver.script("(function(){var _p=window.__kjs.model.activePack();return _p?_p.id:null;})()") instanceof String var4 && !var4.isBlank()) {
            var2 = var4;
         }
      } catch (Exception var6) {
      }

      if (var2 != null) {
         LinkedHashMap var7 = new LinkedHashMap();
         var7.put("id", var2);
         String var8 = io.karatelabs.ext.robot.RobotPatternPackLoader.a(var2);
         if (var8 != null) {
            var7.put("fragments", var8);
         }

         String var5 = io.karatelabs.ext.robot.RobotPatternPackLoader.b(var2);
         if (var5 != null) {
            var7.put("helpers", var5);
         }

         var1.put("pack", var7);
         var1.put(
            "hint",
            "this page matched the '"
               + var2
               + "' pattern pack — its choreographies + helper templates are in `pack` (already applied to look()/act()). Adopt them, verify what's page-specific, then submit the dossier with bot.study({...})."
         );
      }

      if (!var1.containsKey("hint")) {
         var1.put(
            "hint",
            "explore the page, then submit a dossier with bot.study({drawerSelector, nameNoiseTokens, caretSelectors, menuSelectors, prose, helpers}) — its machine fields make look()/act() better session-wide."
         );
      }

      return var1;
   }

   private Map<String, Object> closeStudy(Map<String, Object> var1) {
      this.studyDossier = var1;
      ArrayList var2 = new ArrayList();
      this.pushSessionPack();
      boolean var3 = var1.get("caretSelectors") != null && var1.get("menuSelectors") != null;

      for (String var5 : SESSION_PACK_KEYS) {
         if (var1.get(var5) != null && (!var5.equals("caretSelectors") && !var5.equals("menuSelectors") || var3)) {
            var2.add(var5);
         }
      }

      LinkedHashMap var6 = new LinkedHashMap();
      var6.put("studied", true);
      var6.put("applied", var2);
      if (var1.get("prose") != null) {
         var6.put("prose", "pinned for the session");
      }

      if (var1.get("helpers") != null) {
         var6.put(
            "helpers",
            "recorded — call one only if you also declared it in a js block (`var name = function(){ … };`); vars persist across turns, dossier fields do not"
         );
      }

      var6.put(
         "hint",
         var2.isEmpty()
            ? "dossier recorded. Add machine fields (drawerSelector, nameNoiseTokens, caretSelectors, menuSelectors) to make look()/act() better automatically; promote a verified helper to checks/helpers.js."
            : "applied " + var2 + " — look()/act() now use them session-wide. Author your check; promote a verified helper to checks/helpers.js."
      );
      return var6;
   }

   private static Map<String, Object> studyTemplate() {
      LinkedHashMap var0 = new LinkedHashMap();
      var0.put(
         "settleSignal",
         "how you know a step advanced (a DOM/heading/active-panel change; a title token only if it varies) — advisory, for your own settle logic"
      );
      var0.put("drawerSelector", "CSS for a populated bottom drawer/panel to treat as an active surface");
      var0.put("nameNoiseTokens", "id/name segments that are framework noise, so names read meaningfully (array)");
      var0.put("caretSelectors", "CSS for disclosure carets that open a menu with no ARIA (array)");
      var0.put("menuSelectors", "CSS for the submenu container those carets own (array)");
      var0.put("prose", "1-3 sentences of page-specific gotchas to keep in context (byte-budgeted)");
      var0.put(
         "helpers",
         "the source of helpers you ALREADY declared in a js block (this field records them for promotion to checks/helpers.js — submitting source here does NOT define it)"
      );
      return var0;
   }

   private void pushSessionPack() {
      if (this.studyDossier != null) {
         LinkedHashMap var1 = new LinkedHashMap();

         for (String var3 : SESSION_PACK_KEYS) {
            Object var4 = this.studyDossier.get(var3);
            if (var4 != null) {
               var1.put(var3, var4);
            }
         }

         try {
            this.driver.script("window.__kjs && window.__kjs.model && window.__kjs.model.setSessionPack(" + Json.of(var1) + ")");
         } catch (Exception var5) {
            logger.debug("session-pack push skipped: {}", var5.getMessage());
         }
      }
   }

   public String studyProse() {
      var var1 = this.studyDossier;
      Object var2 = var1 == null ? null : var1.get("prose");
      return var2 == null ? null : String.valueOf(var2);
   }

   private static List<?> asList(Object var0) {
      if (var0 instanceof List var2) {
         return var2;
      } else if (var0 instanceof Collection var1) {
         return new ArrayList(var1);
      } else {
         return var0 != null && var0.getClass().isArray() ? Arrays.asList((Object[])var0) : List.of();
      }
   }

   private io.karatelabs.ext.robot.BotDriver.HealingMode scopeHealMode() {
      String var1 = System.getProperty("karate.robot.heal", this.healEnv);
      if (var1 != null && !var1.isBlank()) {
         return io.karatelabs.ext.robot.BotDriver.HealingMode.parse(var1, io.karatelabs.ext.robot.BotDriver.HealingMode.AUTO);
      } else {
         return io.karatelabs.ext.robot.StepDiagnosticsScope.c() ? io.karatelabs.ext.robot.BotDriver.HealingMode.STRICT : io.karatelabs.ext.robot.BotDriver.HealingMode.AUTO;
      }
   }

   private void announceHeal(String var1, String var2, String var3, Object var4) {
      LinkedHashMap var5 = new LinkedHashMap();
      var5.put("from", var1);
      var5.put("to", var2);
      var5.put("action", var3);
      if (var4 instanceof Number var6) {
         var5.put("score", var6);
      }

      this.pendingHeals.add(var5);
      io.karatelabs.ext.robot.StepDiagnosticsScope.a(var5);
      io.karatelabs.ext.robot.BotDriver.HealingListener var8 = this.healListener;
      if (var8 != null) {
         var8.onHeal(var1, var2, var3, var4 instanceof Number var7 ? var7.doubleValue() : null);
      }
   }

   private Map<String, Object> act(Object... var1) {
      if (var1.length < 2) {
         throw this.fail("act requires locator and action arguments", "INVALID_ARGS");
      } else {
         var var2 = this.checkDialogBlocking();
         if (var2 != null) {
            return var2;
         } else {
            this.lastStabilityHint = null;
            this.reveal.a();
            this.looksSinceAct = 0;
            String var3 = String.valueOf(var1[0]);
            String var4 = String.valueOf(var1[1]);
            if (!KNOWN_OPS.contains(var4)) {
               throw this.fail(
                  "unknown action '"
                     + var4
                     + "' — valid actions: "
                     + String.join(", ", KNOWN_OPS_ORDERED)
                     + ". For a keystroke use act(locator, 'key', '{Enter}').",
                  "INVALID_ACTION"
               );
            } else {
               String[] var5 = parseFrameLocator(var3);
               String var6 = var5[0];
               String var7 = var5[1];
               Object var8 = null;
               Map var9 = null;

               for (int var10 = 2; var10 < var1.length; var10++) {
                  if (var1[var10] instanceof Map) {
                     var9 = (Map)var1[var10];
                  } else if (var8 == null) {
                     var8 = var1[var10];
                  }
               }

               if (!VALUE_OPS.contains(var4) || var8 != null && var8 != Terms.UNDEFINED) {
                  boolean var55 = var9 != null && Boolean.TRUE.equals(var9.get("getNewLocators"));
                  boolean var11 = STATE_CHANGING_OPS.contains(var4);
                  io.karatelabs.ext.robot.BotDriver.HealingMode var12 = io.karatelabs.ext.robot.BotDriver.HealingMode.parse(var9 != null ? var9.get("heal") : null, this.scopeHealMode());
                  Object var13 = var9 != null ? var9.get("reveal") : null;

                  try {
                     long var14 = System.currentTimeMillis();
                     String var59 = this.driver.getUrl();
                     String var61 = this.driver.getTitle();
                     int var63 = this.diagnostics.d();
                     if (var6 != null) {
                        this.injectAgentLookJs();
                        String var64 = var6.replace("\\", "\\\\").replace("'", "\\'");
                        Map var66 = (Map)this.driver.script("window.__kjs.look.prepareFrame('" + var64 + "')");
                        if (var66 != null && !var66.containsKey("error")) {
                           this.iframeOffset = new double[]{((Number)var66.get("x")).doubleValue(), ((Number)var66.get("y")).doubleValue()};
                           Boolean var21 = (Boolean)var66.get("loaded");
                           if (!Boolean.TRUE.equals(var21)) {
                              logger.debug("act: iframe {} not fully loaded, waiting", var6);

                              try {
                                 Thread.sleep(200L);
                              } catch (InterruptedException var50) {
                              }
                           }
                        } else {
                           logger.warn("act: prepareFrame failed for {}: {}", var6, var66 != null ? var66.get("error") : "null");
                        }

                        this.driver.switchFrame(var6);
                        this.injectAgentLookJs();
                     }

                     List var65 = null;
                     List var67 = null;

                     Map var72;
                     try {
                        if (var55 || var11) {
                           this.injectAgentLookJs();

                           try {
                              if (this.driver
                                 .script(
                                    "(function(){ window.__kjs.model.snapshot(); return {text: window.__kjs.look.getPageText(), headings: window.__kjs.look.getHeadings()}; })()"
                                 ) instanceof Map var22) {
                                 var65 = (List)var22.get("text");
                                 var67 = (List)var22.get("headings");
                              }
                           } catch (Exception var49) {
                              this.driver.script("window.__kjs.model.snapshot()");
                           }
                        }

                        Map var69 = null;
                        String var71 = var7;
                        Map var23 = null;
                        int var24 = 0;

                        while (true) {
                           try {
                              var69 = this.performAction(var71, var4, var8, var13);
                              break;
                           } catch (DriverException var51) {
                              boolean var26 = var51.getMessage() != null && var51.getMessage().contains("element not found");
                              if (var26 && var24 < 2) {
                                 logger.debug("act '{}' not found (attempt {}), settling then re-locating", var71, var24 + 1);
                                 this.waitForStable();
                              } else {
                                 if (!var26 || var12 == io.karatelabs.ext.robot.BotDriver.HealingMode.OFF || var23 != null) {
                                    throw var51;
                                 }

                                 var var27 = this.reveal.a(var7);
                                 if (var27 == null || var27.get("to") == null) {
                                    if (var27 != null && Boolean.TRUE.equals(var27.get("refused"))) {
                                       throw new DriverException(this.reveal.a(var7, var27));
                                    }

                                    throw var51;
                                 }

                                 String var28 = String.valueOf(var27.get("to"));
                                 if (var12 == io.karatelabs.ext.robot.BotDriver.HealingMode.STRICT) {
                                    throw new DriverException(
                                       "locator drifted and would heal under strict mode: '"
                                          + var7
                                          + "' -> '"
                                          + var28
                                          + "' (similarity "
                                          + var27.get("score")
                                          + "). Update the check to '"
                                          + var28
                                          + "', or run with {heal:'auto'} to self-heal."
                                    );
                                 }

                                 logger.debug("act '{}' healed to '{}' (score {})", new Object[]{var7, var28, var27.get("score")});
                                 var23 = var27;
                                 this.announceHeal(var7, var28, var4, var27.get("score"));
                                 var71 = var28;
                              }

                              var24++;
                           }
                        }

                        if (var23 != null) {
                           var7 = var71;
                        }

                        if ("scroll".equals(var4) && var69.containsKey("scrolled")) {
                           var69.put("elapsed", System.currentTimeMillis() - var14);
                           return var69;
                        }

                        if (!var69.containsKey("code") || !var69.containsKey("error")) {
                           LinkedHashMap var73 = new LinkedHashMap();
                           if (var69.containsKey("dialog")) {
                              var73.put("dialog", var69.get("dialog"));
                           }

                           if (var69.containsKey("newTab")) {
                              var73.put("newTab", var69.get("newTab"));
                           }

                           if (var69.containsKey("otherTabs")) {
                              var73.put("otherTabs", var69.get("otherTabs"));
                           }

                           if (var6 != null) {
                              this.driver.switchFrame(null);
                           }

                           String var25 = this.driver.getUrl();
                           String var75 = this.driver.getTitle();
                           var73.put("url", var25);
                           var73.put("title", var75);
                           if (!var59.equals(var25)) {
                              var73.put("navigated", true);
                           }

                           var73.put("elapsed", System.currentTimeMillis() - var14);
                           if (!var73.containsKey("navigated")) {
                              this.enrichActResult(var73, var7, var4, var6);
                           }

                           if (var55 || var11) {
                              if (var6 != null) {
                                 this.driver.switchFrame(var6);
                                 this.injectAgentLookJs();
                              }

                              try {
                                 Thread.sleep(var55 ? 500L : 150L);
                              } catch (InterruptedException var48) {
                              }

                              if (this.driver.script("window.__kjs.model.diff()") instanceof Map var78) {
                                 boolean var29 = var78.get("truncated") instanceof Number var30 && var30.intValue() > 0;
                                 if (var29) {
                                    var73.put("truncated", ((Number)var78.get("truncated")).intValue());
                                 }

                                 var var81 = asList(var78.get("added"));
                                 if (var55 && !var81.isEmpty()) {
                                    var73.put("newLocators", var81);
                                 }

                                 var var83 = asList(var78.get("removed"));
                                 if (("click".equals(var4) || "doubleClick".equals(var4))
                                    && var81.isEmpty()
                                    && var83.isEmpty()
                                    && !var29
                                    && !var73.containsKey("navigated")
                                    && !var73.containsKey("newTab")
                                    && !var73.containsKey("dialog")
                                    && !var73.containsKey("validation")) {
                                    Map var32 = var67 == null ? null : this.regionSwap(var67);
                                    if (var65 != null && !normText(var65).equals(normText(var73.get("text")))) {
                                       var73.put("textChanged", true);
                                    } else if (var32 != null) {
                                       var73.put("regionChanged", var32);
                                    } else {
                                       var73.put("noChange", true);
                                       String var33 = this.diagnostics.a(var63);
                                       if (var33 != null) {
                                          var73.put("pageError", var33);
                                       }
                                    }
                                 }

                                 if (var11 && !var83.isEmpty() && !var29) {
                                    if (var83.size() > 12) {
                                       ArrayList var85 = new ArrayList(var83.subList(0, 12));
                                       var85.add("(+" + (var83.size() - 12) + " more gone — call look() for the full diff)");
                                       var73.put("removed", var85);
                                    } else {
                                       var73.put("removed", var83);
                                    }
                                 }
                              }
                           }

                           StringBuilder var77 = new StringBuilder();
                           if (var23 != null) {
                              var73.put("healed", Map.of("from", var23.get("from"), "to", var23.get("to")));
                              var77.append("locator healed: '")
                                 .append(var23.get("from"))
                                 .append("' → '")
                                 .append(var23.get("to"))
                                 .append("' — use '")
                                 .append(var23.get("to"))
                                 .append("' from now on. ");
                           }

                           String var79 = this.reveal.b();
                           if (var79 != null) {
                              var73.put("autoRevealed", var79);
                              var77.append("auto-revealed: ").append(var79).append(" — put this reveal step in your check. ");
                           }

                           Object var80 = io.karatelabs.ext.robot.RobotGuidanceFormatter.a(var73, var4, this.lastStabilityHint);
                           String var82 = var77.toString() + (var80 == null ? "" : var80);
                           var73.put("hint", var82.isBlank() ? null : var82.trim());
                           this.lastActionError = null;
                           return var73;
                        }

                        var72 = var69;
                     } finally {
                        if (var6 != null) {
                           try {
                              this.driver.switchFrame(null);
                           } catch (Exception var47) {
                           }

                           this.iframeOffset = null;
                        }
                     }

                     return var72;
                  } catch (DriverException var53) {
                     this.logAgentError("act", var53.getMessage(), var3);
                     this.lastActionError = "act('" + var3 + "', '" + var4 + "') failed: " + var53.getMessage();
                     boolean var56 = var53.getMessage() != null && var53.getMessage().contains("element not found");
                     String var58 = var56 ? "ELEMENT_NOT_FOUND" : "ACTION_FAILED";
                     boolean var60 = var53.getMessage() != null && var53.getMessage().contains("closest candidates below the similarity floor");
                     String var62 = var56 && !var60 ? io.karatelabs.ext.robot.RobotGuidanceFormatter.a(var3, this.lastLookResult, this.driver::getUrl) : null;
                     String var19 = var56 ? this.detectOcclusion(var7) : null;
                     String var20 = var53.getMessage();
                     if (var62 != null) {
                        var20 = var20 + " | similar locators seen recently: " + var62;
                     } else if (var56
                        && this.lastLookResult != null
                        && this.lastLookResult.get("url") != null
                        && !this.lastLookResult.get("url").toString().equals(this.driver.getUrl())) {
                        var20 = var20 + " | URL changed since last look() — call look() to see this page's elements";
                     }

                     if (var19 != null) {
                        var20 = var20 + " | " + var19;
                     }

                     throw this.fail(var20, var58);
                  } catch (Exception var54) {
                     String var15 = var54.getMessage();
                     if (var15 == null
                        || !var15.contains("Cannot read properties of null")
                           && !var15.contains("null is not an object")
                           && !var15.contains("undefined is not an object")) {
                        String var57 = var15 != null ? var15 : "Action failed";
                        this.logAgentError("act", var57, var3);
                        this.lastActionError = "act('" + var3 + "', '" + var4 + "') failed: " + var57;
                        throw this.fail(var57, "ACTION_FAILED");
                     } else {
                        String var16 = io.karatelabs.ext.robot.RobotGuidanceFormatter.a(var3, this.lastLookResult, this.driver::getUrl);
                        String var17 = var16 != null ? " Similar locators seen recently: " + var16 : "";
                        String var18 = "Element not found: '" + var3 + "'." + var17 + " Call bot.look() to see available elements.";
                        this.logAgentError("act", var18, var3);
                        this.lastActionError = var18;
                        throw this.fail(var18, "ELEMENT_NOT_FOUND");
                     }
                  }
               } else {
                  throw this.fail("act " + var4 + ": value is missing (null/undefined) — if it comes from __arg, the caller did not pass it", "INVALID_ARGS");
               }
            }
         }
      }
   }

   private Map<String, Object> regionSwap(List<?> var1) {
      try {
         List var2 = (List)this.driver.script("window.__kjs.look.getHeadings()");
         if (var2 != null && !var2.equals(var1)) {
            for (Object var4 : var2) {
               if (var4 instanceof Map var5 && !var1.contains(var5)) {
                  LinkedHashMap var6 = new LinkedHashMap();
                  var6.put("name", var5.get("name"));
                  if (var5.get("locator") != null) {
                     var6.put("locator", var5.get("locator"));
                  }

                  return var6;
               }
            }

            return null;
         } else {
            return null;
         }
      } catch (Exception var7) {
         return null;
      }
   }

   private static List<?> normText(Object var0) {
      return var0 instanceof List var1 ? var1 : List.of();
   }

   private void enrichActResult(Map<String, Object> var1, String var2, String var3, String var4) {
      try {
         if (var4 != null) {
            this.driver.switchFrame(var4);
            this.injectAgentLookJs();
         } else {
            this.injectAgentLookJs();
         }

         try {
            List var5 = (List)this.driver.script("window.__kjs.look.getPageText()");
            if (var5 != null && !var5.isEmpty()) {
               var1.put("text", var5);
            }

            if ("click".equals(var3)) {
               boolean var6;
               try {
                  String var7 = Locators.selector(var2);
                  var6 = Boolean.TRUE.equals(this.driver.script("(function() { try { return (" + var7 + ") != null; } catch(e) { return false; } })()"));
               } catch (Exception var17) {
                  var6 = false;
               }

               if (!var6) {
                  var1.put("elementGone", true);
               } else {
                  Object var20 = this.driver
                     .script(
                        var2,
                        "_ && _.closest && (function(el) {var form = el.closest('form');if (!form) return null;if (form.checkValidity()) return null;var msgs = [];var inputs = form.querySelectorAll('input, select, textarea');for (var i = 0; i < inputs.length; i++) {  if (!inputs[i].validity.valid && inputs[i].validationMessage) {    var label = inputs[i].name || inputs[i].id || inputs[i].type || 'field';    msgs.push(label + ': ' + inputs[i].validationMessage);  }}return msgs.length > 0 ? msgs : null;})(_)"
                     );
                  if (var20 instanceof List var8 && !var8.isEmpty()) {
                     var1.put("validation", var20);
                  }
               }
            }
         } finally {
            if (var4 != null) {
               try {
                  this.driver.switchFrame(null);
               } catch (Exception var16) {
               }
            }
         }
      } catch (Exception var19) {
         logger.debug("enrichActResult: {}", var19.getMessage());
      }
   }

   private Map<String, Object> go(Object... var1) {
      if (var1.length < 1) {
         throw this.fail("go requires url argument", "INVALID_ARGS");
      } else {
         String var2 = String.valueOf(var1[0]);
         this.driver.setUrl(var2);
         this.looksSinceAct = 0;
         this.softWaitForReadyState("document.readyState === 'complete'");
         String var3 = this.waitForStable();
         this.injectAgentLookJs();
         LinkedHashMap var4 = new LinkedHashMap();
         var4.put("url", this.driver.getUrl());
         var4.put("title", this.driver.getTitle());
         String var5 = var3 != null ? var3 : "Call look() to see page elements.";
         var4.put("hint", var5);
         return var4;
      }
   }

   private Map<String, Object> waitFor(Object... var1) {
      if (var1.length < 1) throw this.fail("wait requires condition argument", "INVALID_ARGS");
      Object condition = var1[0];
      long start = System.currentTimeMillis();
      try {
         if (condition instanceof Number number) {
            boolean idle = this.diagnostics.a();
            Thread.sleep(number.longValue());
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("elapsed", System.currentTimeMillis() - start);
            if (idle) result.put("hint", "No pending network activity — this wait may be unnecessary");
            return result;
         }
         String value = String.valueOf(condition);
         if (value.equals("idle")) {
            this.softWaitForReadyState("document.readyState === 'complete'");
            String hint = this.waitForStable();
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("elapsed", System.currentTimeMillis() - start);
            if (hint != null) result.put("hint", hint);
            return result;
         }
         if (value.equals("url") || value.equals("title")) {
            if (var1.length < 2) throw this.fail("wait('" + value + "') requires pattern argument", "INVALID_ARGS");
            return this.waitForTitleOrUrl(value, String.valueOf(var1[1]), start);
         }
         int colon = value.indexOf(':');
         if (colon > 0) {
            String kind = value.substring(0, colon);
            if (kind.equals("title") || kind.equals("url")) return this.waitForTitleOrUrl(kind, value.substring(colon + 1).trim(), start);
         }
         if (value.startsWith("{") || value.startsWith("[") || value.startsWith("#") || value.startsWith("/") || value.startsWith(".") && !value.contains("(")) this.driver.waitFor(value);
         else this.driver.waitUntil(value);
         Map<String, Object> result = new LinkedHashMap<>();
         result.put("elapsed", System.currentTimeMillis() - start);
         return result;
      } catch (InterruptedException e) {
         Thread.currentThread().interrupt();
         throw this.fail("Wait interrupted", "INTERRUPTED");
      } catch (DriverException e) {
         this.logAgentError("wait", e.getMessage(), String.valueOf(condition));
         throw this.fail(e.getMessage(), "TIMEOUT");
      }
      // $VF: Couldn't be decompiled
      // Please report this to the Vineflower issue tracker, at https://github.com/Vineflower/vineflower/issues with a copy of the class file (if you have the rights to distribute it!)
      // java.lang.StackOverflowError
      //   at java.base/java.lang.ThreadLocal$ThreadLocalMap.getEntry(ThreadLocal.java:491)
      //   at java.base/java.lang.ThreadLocal.get(ThreadLocal.java:186)
      //   at java.base/java.lang.ThreadLocal.get(ThreadLocal.java:171)
      //   at org.jetbrains.java.decompiler.main.DecompilerContext.getCurrentContext(DecompilerContext.java:67)
      //   at org.jetbrains.java.decompiler.main.DecompilerContext.getProperty(DecompilerContext.java:112)
      //   at org.jetbrains.java.decompiler.main.DecompilerContext.getOption(DecompilerContext.java:116)
      //   at org.jetbrains.java.decompiler.modules.decompiler.exps.VarExprent.getVarType(VarExprent.java:311)
      //   at org.jetbrains.java.decompiler.modules.decompiler.exps.VarExprent.getExprType(VarExprent.java:78)
      //   at org.jetbrains.java.decompiler.modules.decompiler.exps.FunctionExprent.getExprType(FunctionExprent.java:204)
      //   at org.jetbrains.java.decompiler.modules.decompiler.exps.FunctionExprent.getInferredExprType(FunctionExprent.java:312)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.getCastedExprent(ExprProcessor.java:962)
      //   at org.jetbrains.java.decompiler.modules.decompiler.exps.InvocationExprent.appendParamList(InvocationExprent.java:1153)
      //   at org.jetbrains.java.decompiler.modules.decompiler.exps.InvocationExprent.toJava(InvocationExprent.java:904)
      //   at org.jetbrains.java.decompiler.modules.decompiler.exps.InvocationExprent.toJava(InvocationExprent.java:812)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.getCastedExprent(ExprProcessor.java:1014)
      //   at org.jetbrains.java.decompiler.modules.decompiler.exps.InvocationExprent.appendParamList(InvocationExprent.java:1153)
      //   at org.jetbrains.java.decompiler.modules.decompiler.exps.InvocationExprent.toJava(InvocationExprent.java:904)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.getCastedExprent(ExprProcessor.java:1014)
      //   at org.jetbrains.java.decompiler.modules.decompiler.exps.YieldExprent.toJava(YieldExprent.java:50)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.listToJava(ExprProcessor.java:891)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.BasicBlockStatement.toJava(BasicBlockStatement.java:91)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.jmpWrapper(ExprProcessor.java:829)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.IfStatement.toJava(IfStatement.java:238)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.jmpWrapper(ExprProcessor.java:829)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.IfStatement.toJava(IfStatement.java:238)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.jmpWrapper(ExprProcessor.java:829)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.SequenceStatement.toJava(SequenceStatement.java:107)
      //   at org.jetbrains.java.decompiler.modules.decompiler.exps.SwitchExprent.toJava(SwitchExprent.java:167)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.getCastedExprent(ExprProcessor.java:1014)
      //   at org.jetbrains.java.decompiler.modules.decompiler.exps.ExitExprent.toJava(ExitExprent.java:86)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.listToJava(ExprProcessor.java:891)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.BasicBlockStatement.toJava(BasicBlockStatement.java:91)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.jmpWrapper(ExprProcessor.java:829)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.IfStatement.toJava(IfStatement.java:238)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.jmpWrapper(ExprProcessor.java:829)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.IfStatement.toJava(IfStatement.java:238)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.jmpWrapper(ExprProcessor.java:829)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.SequenceStatement.toJava(SequenceStatement.java:107)
      //   at org.jetbrains.java.decompiler.modules.decompiler.exps.SwitchExprent.toJava(SwitchExprent.java:167)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.getCastedExprent(ExprProcessor.java:1014)
      //   at org.jetbrains.java.decompiler.modules.decompiler.exps.ExitExprent.toJava(ExitExprent.java:86)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.listToJava(ExprProcessor.java:891)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.BasicBlockStatement.toJava(BasicBlockStatement.java:91)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.jmpWrapper(ExprProcessor.java:829)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.IfStatement.toJava(IfStatement.java:238)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.jmpWrapper(ExprProcessor.java:829)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.IfStatement.toJava(IfStatement.java:238)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.jmpWrapper(ExprProcessor.java:829)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.SequenceStatement.toJava(SequenceStatement.java:107)
      //   at org.jetbrains.java.decompiler.modules.decompiler.exps.SwitchExprent.toJava(SwitchExprent.java:167)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.getCastedExprent(ExprProcessor.java:1014)
      //   at org.jetbrains.java.decompiler.modules.decompiler.exps.ExitExprent.toJava(ExitExprent.java:86)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.listToJava(ExprProcessor.java:891)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.BasicBlockStatement.toJava(BasicBlockStatement.java:91)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.jmpWrapper(ExprProcessor.java:829)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.IfStatement.toJava(IfStatement.java:238)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.jmpWrapper(ExprProcessor.java:829)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.IfStatement.toJava(IfStatement.java:238)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.jmpWrapper(ExprProcessor.java:829)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.SequenceStatement.toJava(SequenceStatement.java:107)
      //   at org.jetbrains.java.decompiler.modules.decompiler.exps.SwitchExprent.toJava(SwitchExprent.java:167)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.getCastedExprent(ExprProcessor.java:1014)
      //   at org.jetbrains.java.decompiler.modules.decompiler.exps.ExitExprent.toJava(ExitExprent.java:86)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.listToJava(ExprProcessor.java:891)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.BasicBlockStatement.toJava(BasicBlockStatement.java:91)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.jmpWrapper(ExprProcessor.java:829)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.IfStatement.toJava(IfStatement.java:238)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.jmpWrapper(ExprProcessor.java:829)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.IfStatement.toJava(IfStatement.java:238)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.jmpWrapper(ExprProcessor.java:829)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.SequenceStatement.toJava(SequenceStatement.java:107)
      //   at org.jetbrains.java.decompiler.modules.decompiler.exps.SwitchExprent.toJava(SwitchExprent.java:167)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.getCastedExprent(ExprProcessor.java:1014)
      //   at org.jetbrains.java.decompiler.modules.decompiler.exps.ExitExprent.toJava(ExitExprent.java:86)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.listToJava(ExprProcessor.java:891)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.BasicBlockStatement.toJava(BasicBlockStatement.java:91)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.jmpWrapper(ExprProcessor.java:829)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.IfStatement.toJava(IfStatement.java:238)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.jmpWrapper(ExprProcessor.java:829)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.IfStatement.toJava(IfStatement.java:238)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.jmpWrapper(ExprProcessor.java:829)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.SequenceStatement.toJava(SequenceStatement.java:107)
      //   at org.jetbrains.java.decompiler.modules.decompiler.exps.SwitchExprent.toJava(SwitchExprent.java:167)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.getCastedExprent(ExprProcessor.java:1014)
      //   at org.jetbrains.java.decompiler.modules.decompiler.exps.ExitExprent.toJava(ExitExprent.java:86)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.listToJava(ExprProcessor.java:891)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.BasicBlockStatement.toJava(BasicBlockStatement.java:91)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.jmpWrapper(ExprProcessor.java:829)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.IfStatement.toJava(IfStatement.java:238)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.jmpWrapper(ExprProcessor.java:829)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.IfStatement.toJava(IfStatement.java:238)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.jmpWrapper(ExprProcessor.java:829)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.SequenceStatement.toJava(SequenceStatement.java:107)
      //   at org.jetbrains.java.decompiler.modules.decompiler.exps.SwitchExprent.toJava(SwitchExprent.java:167)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.getCastedExprent(ExprProcessor.java:1014)
      //   at org.jetbrains.java.decompiler.modules.decompiler.exps.ExitExprent.toJava(ExitExprent.java:86)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.listToJava(ExprProcessor.java:891)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.BasicBlockStatement.toJava(BasicBlockStatement.java:91)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.jmpWrapper(ExprProcessor.java:829)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.IfStatement.toJava(IfStatement.java:238)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.jmpWrapper(ExprProcessor.java:829)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.IfStatement.toJava(IfStatement.java:238)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.jmpWrapper(ExprProcessor.java:829)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.SequenceStatement.toJava(SequenceStatement.java:107)
      //   at org.jetbrains.java.decompiler.modules.decompiler.exps.SwitchExprent.toJava(SwitchExprent.java:167)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.getCastedExprent(ExprProcessor.java:1014)
      //   at org.jetbrains.java.decompiler.modules.decompiler.exps.ExitExprent.toJava(ExitExprent.java:86)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.listToJava(ExprProcessor.java:891)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.BasicBlockStatement.toJava(BasicBlockStatement.java:91)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.jmpWrapper(ExprProcessor.java:829)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.IfStatement.toJava(IfStatement.java:238)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.jmpWrapper(ExprProcessor.java:829)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.IfStatement.toJava(IfStatement.java:238)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.jmpWrapper(ExprProcessor.java:829)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.SequenceStatement.toJava(SequenceStatement.java:107)
      //   at org.jetbrains.java.decompiler.modules.decompiler.exps.SwitchExprent.toJava(SwitchExprent.java:167)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.getCastedExprent(ExprProcessor.java:1014)
      //   at org.jetbrains.java.decompiler.modules.decompiler.exps.ExitExprent.toJava(ExitExprent.java:86)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.listToJava(ExprProcessor.java:891)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.BasicBlockStatement.toJava(BasicBlockStatement.java:91)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.jmpWrapper(ExprProcessor.java:829)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.IfStatement.toJava(IfStatement.java:238)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.jmpWrapper(ExprProcessor.java:829)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.IfStatement.toJava(IfStatement.java:238)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.jmpWrapper(ExprProcessor.java:829)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.SequenceStatement.toJava(SequenceStatement.java:107)
      //   at org.jetbrains.java.decompiler.modules.decompiler.exps.SwitchExprent.toJava(SwitchExprent.java:167)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.getCastedExprent(ExprProcessor.java:1014)
      //   at org.jetbrains.java.decompiler.modules.decompiler.exps.ExitExprent.toJava(ExitExprent.java:86)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.listToJava(ExprProcessor.java:891)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.BasicBlockStatement.toJava(BasicBlockStatement.java:91)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.jmpWrapper(ExprProcessor.java:829)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.IfStatement.toJava(IfStatement.java:238)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.jmpWrapper(ExprProcessor.java:829)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.IfStatement.toJava(IfStatement.java:238)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.jmpWrapper(ExprProcessor.java:829)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.SequenceStatement.toJava(SequenceStatement.java:107)
      //   at org.jetbrains.java.decompiler.modules.decompiler.exps.SwitchExprent.toJava(SwitchExprent.java:167)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.getCastedExprent(ExprProcessor.java:1014)
      //   at org.jetbrains.java.decompiler.modules.decompiler.exps.ExitExprent.toJava(ExitExprent.java:86)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.listToJava(ExprProcessor.java:891)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.BasicBlockStatement.toJava(BasicBlockStatement.java:91)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.jmpWrapper(ExprProcessor.java:829)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.IfStatement.toJava(IfStatement.java:238)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.jmpWrapper(ExprProcessor.java:829)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.IfStatement.toJava(IfStatement.java:238)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.jmpWrapper(ExprProcessor.java:829)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.SequenceStatement.toJava(SequenceStatement.java:107)
      //   at org.jetbrains.java.decompiler.modules.decompiler.exps.SwitchExprent.toJava(SwitchExprent.java:167)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.getCastedExprent(ExprProcessor.java:1014)
      //   at org.jetbrains.java.decompiler.modules.decompiler.exps.ExitExprent.toJava(ExitExprent.java:86)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.listToJava(ExprProcessor.java:891)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.BasicBlockStatement.toJava(BasicBlockStatement.java:91)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.jmpWrapper(ExprProcessor.java:829)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.IfStatement.toJava(IfStatement.java:238)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.jmpWrapper(ExprProcessor.java:829)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.IfStatement.toJava(IfStatement.java:238)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.jmpWrapper(ExprProcessor.java:829)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.SequenceStatement.toJava(SequenceStatement.java:107)
      //   at org.jetbrains.java.decompiler.modules.decompiler.exps.SwitchExprent.toJava(SwitchExprent.java:167)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.getCastedExprent(ExprProcessor.java:1014)
      //   at org.jetbrains.java.decompiler.modules.decompiler.exps.ExitExprent.toJava(ExitExprent.java:86)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.listToJava(ExprProcessor.java:891)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.BasicBlockStatement.toJava(BasicBlockStatement.java:91)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.jmpWrapper(ExprProcessor.java:829)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.IfStatement.toJava(IfStatement.java:238)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.jmpWrapper(ExprProcessor.java:829)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.IfStatement.toJava(IfStatement.java:238)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.jmpWrapper(ExprProcessor.java:829)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.SequenceStatement.toJava(SequenceStatement.java:107)
      //   at org.jetbrains.java.decompiler.modules.decompiler.exps.SwitchExprent.toJava(SwitchExprent.java:167)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.getCastedExprent(ExprProcessor.java:1014)
      //   at org.jetbrains.java.decompiler.modules.decompiler.exps.ExitExprent.toJava(ExitExprent.java:86)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.listToJava(ExprProcessor.java:891)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.BasicBlockStatement.toJava(BasicBlockStatement.java:91)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.jmpWrapper(ExprProcessor.java:829)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.IfStatement.toJava(IfStatement.java:238)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.jmpWrapper(ExprProcessor.java:829)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.IfStatement.toJava(IfStatement.java:238)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.jmpWrapper(ExprProcessor.java:829)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.SequenceStatement.toJava(SequenceStatement.java:107)
      //   at org.jetbrains.java.decompiler.modules.decompiler.exps.SwitchExprent.toJava(SwitchExprent.java:167)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.getCastedExprent(ExprProcessor.java:1014)
      //   at org.jetbrains.java.decompiler.modules.decompiler.exps.ExitExprent.toJava(ExitExprent.java:86)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.listToJava(ExprProcessor.java:891)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.BasicBlockStatement.toJava(BasicBlockStatement.java:91)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.jmpWrapper(ExprProcessor.java:829)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.IfStatement.toJava(IfStatement.java:238)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.jmpWrapper(ExprProcessor.java:829)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.IfStatement.toJava(IfStatement.java:238)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.jmpWrapper(ExprProcessor.java:829)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.SequenceStatement.toJava(SequenceStatement.java:107)
      //   at org.jetbrains.java.decompiler.modules.decompiler.exps.SwitchExprent.toJava(SwitchExprent.java:167)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.getCastedExprent(ExprProcessor.java:1014)
      //   at org.jetbrains.java.decompiler.modules.decompiler.exps.ExitExprent.toJava(ExitExprent.java:86)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.listToJava(ExprProcessor.java:891)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.BasicBlockStatement.toJava(BasicBlockStatement.java:91)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.jmpWrapper(ExprProcessor.java:829)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.IfStatement.toJava(IfStatement.java:238)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.jmpWrapper(ExprProcessor.java:829)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.IfStatement.toJava(IfStatement.java:238)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.jmpWrapper(ExprProcessor.java:829)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.SequenceStatement.toJava(SequenceStatement.java:107)
      //   at org.jetbrains.java.decompiler.modules.decompiler.exps.SwitchExprent.toJava(SwitchExprent.java:167)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.getCastedExprent(ExprProcessor.java:1014)
      //   at org.jetbrains.java.decompiler.modules.decompiler.exps.ExitExprent.toJava(ExitExprent.java:86)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.listToJava(ExprProcessor.java:891)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.BasicBlockStatement.toJava(BasicBlockStatement.java:91)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.jmpWrapper(ExprProcessor.java:829)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.IfStatement.toJava(IfStatement.java:238)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.jmpWrapper(ExprProcessor.java:829)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.IfStatement.toJava(IfStatement.java:238)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.jmpWrapper(ExprProcessor.java:829)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.SequenceStatement.toJava(SequenceStatement.java:107)
      //   at org.jetbrains.java.decompiler.modules.decompiler.exps.SwitchExprent.toJava(SwitchExprent.java:167)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.getCastedExprent(ExprProcessor.java:1014)
      //   at org.jetbrains.java.decompiler.modules.decompiler.exps.ExitExprent.toJava(ExitExprent.java:86)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.listToJava(ExprProcessor.java:891)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.BasicBlockStatement.toJava(BasicBlockStatement.java:91)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.jmpWrapper(ExprProcessor.java:829)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.IfStatement.toJava(IfStatement.java:238)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.jmpWrapper(ExprProcessor.java:829)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.IfStatement.toJava(IfStatement.java:238)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.jmpWrapper(ExprProcessor.java:829)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.SequenceStatement.toJava(SequenceStatement.java:107)
      //   at org.jetbrains.java.decompiler.modules.decompiler.exps.SwitchExprent.toJava(SwitchExprent.java:167)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.getCastedExprent(ExprProcessor.java:1014)
      //   at org.jetbrains.java.decompiler.modules.decompiler.exps.ExitExprent.toJava(ExitExprent.java:86)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.listToJava(ExprProcessor.java:891)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.BasicBlockStatement.toJava(BasicBlockStatement.java:91)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.jmpWrapper(ExprProcessor.java:829)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.IfStatement.toJava(IfStatement.java:238)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.jmpWrapper(ExprProcessor.java:829)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.IfStatement.toJava(IfStatement.java:238)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.jmpWrapper(ExprProcessor.java:829)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.SequenceStatement.toJava(SequenceStatement.java:107)
      //   at org.jetbrains.java.decompiler.modules.decompiler.exps.SwitchExprent.toJava(SwitchExprent.java:167)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.getCastedExprent(ExprProcessor.java:1014)
      //   at org.jetbrains.java.decompiler.modules.decompiler.exps.ExitExprent.toJava(ExitExprent.java:86)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.listToJava(ExprProcessor.java:891)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.BasicBlockStatement.toJava(BasicBlockStatement.java:91)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.jmpWrapper(ExprProcessor.java:829)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.IfStatement.toJava(IfStatement.java:238)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.jmpWrapper(ExprProcessor.java:829)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.IfStatement.toJava(IfStatement.java:238)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.jmpWrapper(ExprProcessor.java:829)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.SequenceStatement.toJava(SequenceStatement.java:107)
      //   at org.jetbrains.java.decompiler.modules.decompiler.exps.SwitchExprent.toJava(SwitchExprent.java:167)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.getCastedExprent(ExprProcessor.java:1014)
      //   at org.jetbrains.java.decompiler.modules.decompiler.exps.ExitExprent.toJava(ExitExprent.java:86)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.listToJava(ExprProcessor.java:891)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.BasicBlockStatement.toJava(BasicBlockStatement.java:91)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.jmpWrapper(ExprProcessor.java:829)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.IfStatement.toJava(IfStatement.java:238)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.jmpWrapper(ExprProcessor.java:829)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.IfStatement.toJava(IfStatement.java:238)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.jmpWrapper(ExprProcessor.java:829)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.SequenceStatement.toJava(SequenceStatement.java:107)
      //   at org.jetbrains.java.decompiler.modules.decompiler.exps.SwitchExprent.toJava(SwitchExprent.java:167)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.getCastedExprent(ExprProcessor.java:1014)
      //   at org.jetbrains.java.decompiler.modules.decompiler.exps.ExitExprent.toJava(ExitExprent.java:86)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.listToJava(ExprProcessor.java:891)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.BasicBlockStatement.toJava(BasicBlockStatement.java:91)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.jmpWrapper(ExprProcessor.java:829)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.IfStatement.toJava(IfStatement.java:238)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.jmpWrapper(ExprProcessor.java:829)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.IfStatement.toJava(IfStatement.java:238)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.jmpWrapper(ExprProcessor.java:829)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.SequenceStatement.toJava(SequenceStatement.java:107)
      //   at org.jetbrains.java.decompiler.modules.decompiler.exps.SwitchExprent.toJava(SwitchExprent.java:167)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.getCastedExprent(ExprProcessor.java:1014)
      //   at org.jetbrains.java.decompiler.modules.decompiler.exps.ExitExprent.toJava(ExitExprent.java:86)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.listToJava(ExprProcessor.java:891)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.BasicBlockStatement.toJava(BasicBlockStatement.java:91)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.jmpWrapper(ExprProcessor.java:829)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.IfStatement.toJava(IfStatement.java:238)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.jmpWrapper(ExprProcessor.java:829)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.IfStatement.toJava(IfStatement.java:238)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.jmpWrapper(ExprProcessor.java:829)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.SequenceStatement.toJava(SequenceStatement.java:107)
      //   at org.jetbrains.java.decompiler.modules.decompiler.exps.SwitchExprent.toJava(SwitchExprent.java:167)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.getCastedExprent(ExprProcessor.java:1014)
      //   at org.jetbrains.java.decompiler.modules.decompiler.exps.ExitExprent.toJava(ExitExprent.java:86)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.listToJava(ExprProcessor.java:891)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.BasicBlockStatement.toJava(BasicBlockStatement.java:91)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.jmpWrapper(ExprProcessor.java:829)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.IfStatement.toJava(IfStatement.java:238)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.jmpWrapper(ExprProcessor.java:829)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.IfStatement.toJava(IfStatement.java:238)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.jmpWrapper(ExprProcessor.java:829)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.SequenceStatement.toJava(SequenceStatement.java:107)
      //   at org.jetbrains.java.decompiler.modules.decompiler.exps.SwitchExprent.toJava(SwitchExprent.java:167)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.getCastedExprent(ExprProcessor.java:1014)
      //   at org.jetbrains.java.decompiler.modules.decompiler.exps.ExitExprent.toJava(ExitExprent.java:86)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.listToJava(ExprProcessor.java:891)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.BasicBlockStatement.toJava(BasicBlockStatement.java:91)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.jmpWrapper(ExprProcessor.java:829)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.IfStatement.toJava(IfStatement.java:238)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.jmpWrapper(ExprProcessor.java:829)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.IfStatement.toJava(IfStatement.java:238)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.jmpWrapper(ExprProcessor.java:829)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.SequenceStatement.toJava(SequenceStatement.java:107)
      //   at org.jetbrains.java.decompiler.modules.decompiler.exps.SwitchExprent.toJava(SwitchExprent.java:167)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.getCastedExprent(ExprProcessor.java:1014)
      //   at org.jetbrains.java.decompiler.modules.decompiler.exps.ExitExprent.toJava(ExitExprent.java:86)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.listToJava(ExprProcessor.java:891)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.BasicBlockStatement.toJava(BasicBlockStatement.java:91)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.jmpWrapper(ExprProcessor.java:829)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.IfStatement.toJava(IfStatement.java:238)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.jmpWrapper(ExprProcessor.java:829)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.IfStatement.toJava(IfStatement.java:238)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.jmpWrapper(ExprProcessor.java:829)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.SequenceStatement.toJava(SequenceStatement.java:107)
      //   at org.jetbrains.java.decompiler.modules.decompiler.exps.SwitchExprent.toJava(SwitchExprent.java:167)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.getCastedExprent(ExprProcessor.java:1014)
      //   at org.jetbrains.java.decompiler.modules.decompiler.exps.ExitExprent.toJava(ExitExprent.java:86)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.listToJava(ExprProcessor.java:891)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.BasicBlockStatement.toJava(BasicBlockStatement.java:91)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.jmpWrapper(ExprProcessor.java:829)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.IfStatement.toJava(IfStatement.java:238)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.jmpWrapper(ExprProcessor.java:829)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.IfStatement.toJava(IfStatement.java:238)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.jmpWrapper(ExprProcessor.java:829)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.SequenceStatement.toJava(SequenceStatement.java:107)
      //   at org.jetbrains.java.decompiler.modules.decompiler.exps.SwitchExprent.toJava(SwitchExprent.java:167)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.getCastedExprent(ExprProcessor.java:1014)
      //   at org.jetbrains.java.decompiler.modules.decompiler.exps.ExitExprent.toJava(ExitExprent.java:86)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.listToJava(ExprProcessor.java:891)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.BasicBlockStatement.toJava(BasicBlockStatement.java:91)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.jmpWrapper(ExprProcessor.java:829)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.IfStatement.toJava(IfStatement.java:238)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.jmpWrapper(ExprProcessor.java:829)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.IfStatement.toJava(IfStatement.java:238)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.jmpWrapper(ExprProcessor.java:829)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.SequenceStatement.toJava(SequenceStatement.java:107)
      //   at org.jetbrains.java.decompiler.modules.decompiler.exps.SwitchExprent.toJava(SwitchExprent.java:167)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.getCastedExprent(ExprProcessor.java:1014)
      //   at org.jetbrains.java.decompiler.modules.decompiler.exps.ExitExprent.toJava(ExitExprent.java:86)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.listToJava(ExprProcessor.java:891)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.BasicBlockStatement.toJava(BasicBlockStatement.java:91)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.jmpWrapper(ExprProcessor.java:829)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.IfStatement.toJava(IfStatement.java:238)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.jmpWrapper(ExprProcessor.java:829)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.IfStatement.toJava(IfStatement.java:238)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.jmpWrapper(ExprProcessor.java:829)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.SequenceStatement.toJava(SequenceStatement.java:107)
      //   at org.jetbrains.java.decompiler.modules.decompiler.exps.SwitchExprent.toJava(SwitchExprent.java:167)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.getCastedExprent(ExprProcessor.java:1014)
      //   at org.jetbrains.java.decompiler.modules.decompiler.exps.ExitExprent.toJava(ExitExprent.java:86)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.listToJava(ExprProcessor.java:891)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.BasicBlockStatement.toJava(BasicBlockStatement.java:91)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.jmpWrapper(ExprProcessor.java:829)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.IfStatement.toJava(IfStatement.java:238)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.jmpWrapper(ExprProcessor.java:829)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.IfStatement.toJava(IfStatement.java:238)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.jmpWrapper(ExprProcessor.java:829)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.SequenceStatement.toJava(SequenceStatement.java:107)
      //   at org.jetbrains.java.decompiler.modules.decompiler.exps.SwitchExprent.toJava(SwitchExprent.java:167)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.getCastedExprent(ExprProcessor.java:1014)
      //   at org.jetbrains.java.decompiler.modules.decompiler.exps.ExitExprent.toJava(ExitExprent.java:86)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.listToJava(ExprProcessor.java:891)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.BasicBlockStatement.toJava(BasicBlockStatement.java:91)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.jmpWrapper(ExprProcessor.java:829)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.IfStatement.toJava(IfStatement.java:238)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.jmpWrapper(ExprProcessor.java:829)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.IfStatement.toJava(IfStatement.java:238)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.jmpWrapper(ExprProcessor.java:829)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.SequenceStatement.toJava(SequenceStatement.java:107)
      //   at org.jetbrains.java.decompiler.modules.decompiler.exps.SwitchExprent.toJava(SwitchExprent.java:167)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.getCastedExprent(ExprProcessor.java:1014)
      //   at org.jetbrains.java.decompiler.modules.decompiler.exps.ExitExprent.toJava(ExitExprent.java:86)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.listToJava(ExprProcessor.java:891)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.BasicBlockStatement.toJava(BasicBlockStatement.java:91)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.jmpWrapper(ExprProcessor.java:829)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.IfStatement.toJava(IfStatement.java:238)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.jmpWrapper(ExprProcessor.java:829)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.IfStatement.toJava(IfStatement.java:238)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.jmpWrapper(ExprProcessor.java:829)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.SequenceStatement.toJava(SequenceStatement.java:107)
      //   at org.jetbrains.java.decompiler.modules.decompiler.exps.SwitchExprent.toJava(SwitchExprent.java:167)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.getCastedExprent(ExprProcessor.java:1014)
      //   at org.jetbrains.java.decompiler.modules.decompiler.exps.ExitExprent.toJava(ExitExprent.java:86)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.listToJava(ExprProcessor.java:891)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.BasicBlockStatement.toJava(BasicBlockStatement.java:91)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.jmpWrapper(ExprProcessor.java:829)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.IfStatement.toJava(IfStatement.java:238)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.jmpWrapper(ExprProcessor.java:829)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.IfStatement.toJava(IfStatement.java:238)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.jmpWrapper(ExprProcessor.java:829)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.SequenceStatement.toJava(SequenceStatement.java:107)
      //   at org.jetbrains.java.decompiler.modules.decompiler.exps.SwitchExprent.toJava(SwitchExprent.java:167)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.getCastedExprent(ExprProcessor.java:1014)
      //   at org.jetbrains.java.decompiler.modules.decompiler.exps.ExitExprent.toJava(ExitExprent.java:86)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.listToJava(ExprProcessor.java:891)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.BasicBlockStatement.toJava(BasicBlockStatement.java:91)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.jmpWrapper(ExprProcessor.java:829)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.IfStatement.toJava(IfStatement.java:238)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.jmpWrapper(ExprProcessor.java:829)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.IfStatement.toJava(IfStatement.java:238)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.jmpWrapper(ExprProcessor.java:829)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.SequenceStatement.toJava(SequenceStatement.java:107)
      //   at org.jetbrains.java.decompiler.modules.decompiler.exps.SwitchExprent.toJava(SwitchExprent.java:167)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.getCastedExprent(ExprProcessor.java:1014)
      //   at org.jetbrains.java.decompiler.modules.decompiler.exps.ExitExprent.toJava(ExitExprent.java:86)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.listToJava(ExprProcessor.java:891)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.BasicBlockStatement.toJava(BasicBlockStatement.java:91)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.jmpWrapper(ExprProcessor.java:829)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.IfStatement.toJava(IfStatement.java:238)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.jmpWrapper(ExprProcessor.java:829)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.IfStatement.toJava(IfStatement.java:238)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.jmpWrapper(ExprProcessor.java:829)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.SequenceStatement.toJava(SequenceStatement.java:107)
      //   at org.jetbrains.java.decompiler.modules.decompiler.exps.SwitchExprent.toJava(SwitchExprent.java:167)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.getCastedExprent(ExprProcessor.java:1014)
      //   at org.jetbrains.java.decompiler.modules.decompiler.exps.ExitExprent.toJava(ExitExprent.java:86)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.listToJava(ExprProcessor.java:891)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.BasicBlockStatement.toJava(BasicBlockStatement.java:91)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.jmpWrapper(ExprProcessor.java:829)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.IfStatement.toJava(IfStatement.java:238)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.jmpWrapper(ExprProcessor.java:829)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.IfStatement.toJava(IfStatement.java:238)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.jmpWrapper(ExprProcessor.java:829)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.SequenceStatement.toJava(SequenceStatement.java:107)
      //   at org.jetbrains.java.decompiler.modules.decompiler.exps.SwitchExprent.toJava(SwitchExprent.java:167)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.getCastedExprent(ExprProcessor.java:1014)
      //   at org.jetbrains.java.decompiler.modules.decompiler.exps.ExitExprent.toJava(ExitExprent.java:86)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.listToJava(ExprProcessor.java:891)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.BasicBlockStatement.toJava(BasicBlockStatement.java:91)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.jmpWrapper(ExprProcessor.java:829)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.IfStatement.toJava(IfStatement.java:238)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.jmpWrapper(ExprProcessor.java:829)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.IfStatement.toJava(IfStatement.java:238)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.jmpWrapper(ExprProcessor.java:829)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.SequenceStatement.toJava(SequenceStatement.java:107)
      //   at org.jetbrains.java.decompiler.modules.decompiler.exps.SwitchExprent.toJava(SwitchExprent.java:167)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.getCastedExprent(ExprProcessor.java:1014)
      //   at org.jetbrains.java.decompiler.modules.decompiler.exps.ExitExprent.toJava(ExitExprent.java:86)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.listToJava(ExprProcessor.java:891)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.BasicBlockStatement.toJava(BasicBlockStatement.java:91)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.jmpWrapper(ExprProcessor.java:829)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.IfStatement.toJava(IfStatement.java:238)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.jmpWrapper(ExprProcessor.java:829)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.IfStatement.toJava(IfStatement.java:238)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.jmpWrapper(ExprProcessor.java:829)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.SequenceStatement.toJava(SequenceStatement.java:107)
      //   at org.jetbrains.java.decompiler.modules.decompiler.exps.SwitchExprent.toJava(SwitchExprent.java:167)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.getCastedExprent(ExprProcessor.java:1014)
      //   at org.jetbrains.java.decompiler.modules.decompiler.exps.ExitExprent.toJava(ExitExprent.java:86)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.listToJava(ExprProcessor.java:891)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.BasicBlockStatement.toJava(BasicBlockStatement.java:91)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.jmpWrapper(ExprProcessor.java:829)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.IfStatement.toJava(IfStatement.java:238)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.jmpWrapper(ExprProcessor.java:829)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.IfStatement.toJava(IfStatement.java:238)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.jmpWrapper(ExprProcessor.java:829)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.SequenceStatement.toJava(SequenceStatement.java:107)
      //   at org.jetbrains.java.decompiler.modules.decompiler.exps.SwitchExprent.toJava(SwitchExprent.java:167)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.getCastedExprent(ExprProcessor.java:1014)
      //   at org.jetbrains.java.decompiler.modules.decompiler.exps.ExitExprent.toJava(ExitExprent.java:86)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.listToJava(ExprProcessor.java:891)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.BasicBlockStatement.toJava(BasicBlockStatement.java:91)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.jmpWrapper(ExprProcessor.java:829)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.IfStatement.toJava(IfStatement.java:238)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.jmpWrapper(ExprProcessor.java:829)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.IfStatement.toJava(IfStatement.java:238)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.jmpWrapper(ExprProcessor.java:829)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.SequenceStatement.toJava(SequenceStatement.java:107)
      //   at org.jetbrains.java.decompiler.modules.decompiler.exps.SwitchExprent.toJava(SwitchExprent.java:167)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.getCastedExprent(ExprProcessor.java:1014)
      //   at org.jetbrains.java.decompiler.modules.decompiler.exps.ExitExprent.toJava(ExitExprent.java:86)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.listToJava(ExprProcessor.java:891)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.BasicBlockStatement.toJava(BasicBlockStatement.java:91)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.jmpWrapper(ExprProcessor.java:829)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.IfStatement.toJava(IfStatement.java:238)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.jmpWrapper(ExprProcessor.java:829)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.IfStatement.toJava(IfStatement.java:238)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.jmpWrapper(ExprProcessor.java:829)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.SequenceStatement.toJava(SequenceStatement.java:107)
      //   at org.jetbrains.java.decompiler.modules.decompiler.exps.SwitchExprent.toJava(SwitchExprent.java:167)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.getCastedExprent(ExprProcessor.java:1014)
      //   at org.jetbrains.java.decompiler.modules.decompiler.exps.ExitExprent.toJava(ExitExprent.java:86)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.listToJava(ExprProcessor.java:891)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.BasicBlockStatement.toJava(BasicBlockStatement.java:91)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.jmpWrapper(ExprProcessor.java:829)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.IfStatement.toJava(IfStatement.java:238)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.jmpWrapper(ExprProcessor.java:829)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.IfStatement.toJava(IfStatement.java:238)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.jmpWrapper(ExprProcessor.java:829)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.SequenceStatement.toJava(SequenceStatement.java:107)
      //   at org.jetbrains.java.decompiler.modules.decompiler.exps.SwitchExprent.toJava(SwitchExprent.java:167)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.getCastedExprent(ExprProcessor.java:1014)
      //   at org.jetbrains.java.decompiler.modules.decompiler.exps.ExitExprent.toJava(ExitExprent.java:86)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.listToJava(ExprProcessor.java:891)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.BasicBlockStatement.toJava(BasicBlockStatement.java:91)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.jmpWrapper(ExprProcessor.java:829)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.IfStatement.toJava(IfStatement.java:238)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.jmpWrapper(ExprProcessor.java:829)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.IfStatement.toJava(IfStatement.java:238)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.jmpWrapper(ExprProcessor.java:829)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.SequenceStatement.toJava(SequenceStatement.java:107)
      //   at org.jetbrains.java.decompiler.modules.decompiler.exps.SwitchExprent.toJava(SwitchExprent.java:167)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.getCastedExprent(ExprProcessor.java:1014)
      //   at org.jetbrains.java.decompiler.modules.decompiler.exps.ExitExprent.toJava(ExitExprent.java:86)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.listToJava(ExprProcessor.java:891)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.BasicBlockStatement.toJava(BasicBlockStatement.java:91)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.jmpWrapper(ExprProcessor.java:829)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.IfStatement.toJava(IfStatement.java:238)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.jmpWrapper(ExprProcessor.java:829)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.IfStatement.toJava(IfStatement.java:238)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.jmpWrapper(ExprProcessor.java:829)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.SequenceStatement.toJava(SequenceStatement.java:107)
      //   at org.jetbrains.java.decompiler.modules.decompiler.exps.SwitchExprent.toJava(SwitchExprent.java:167)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.getCastedExprent(ExprProcessor.java:1014)
      //   at org.jetbrains.java.decompiler.modules.decompiler.exps.ExitExprent.toJava(ExitExprent.java:86)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.listToJava(ExprProcessor.java:891)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.BasicBlockStatement.toJava(BasicBlockStatement.java:91)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.jmpWrapper(ExprProcessor.java:829)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.IfStatement.toJava(IfStatement.java:238)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.jmpWrapper(ExprProcessor.java:829)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.IfStatement.toJava(IfStatement.java:238)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.jmpWrapper(ExprProcessor.java:829)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.SequenceStatement.toJava(SequenceStatement.java:107)
      //   at org.jetbrains.java.decompiler.modules.decompiler.exps.SwitchExprent.toJava(SwitchExprent.java:167)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.getCastedExprent(ExprProcessor.java:1014)
      //   at org.jetbrains.java.decompiler.modules.decompiler.exps.ExitExprent.toJava(ExitExprent.java:86)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.listToJava(ExprProcessor.java:891)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.BasicBlockStatement.toJava(BasicBlockStatement.java:91)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.jmpWrapper(ExprProcessor.java:829)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.IfStatement.toJava(IfStatement.java:238)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.jmpWrapper(ExprProcessor.java:829)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.IfStatement.toJava(IfStatement.java:238)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.jmpWrapper(ExprProcessor.java:829)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.SequenceStatement.toJava(SequenceStatement.java:107)
      //   at org.jetbrains.java.decompiler.modules.decompiler.exps.SwitchExprent.toJava(SwitchExprent.java:167)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.getCastedExprent(ExprProcessor.java:1014)
      //   at org.jetbrains.java.decompiler.modules.decompiler.exps.ExitExprent.toJava(ExitExprent.java:86)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.listToJava(ExprProcessor.java:891)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.BasicBlockStatement.toJava(BasicBlockStatement.java:91)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.jmpWrapper(ExprProcessor.java:829)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.IfStatement.toJava(IfStatement.java:238)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.jmpWrapper(ExprProcessor.java:829)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.IfStatement.toJava(IfStatement.java:238)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.jmpWrapper(ExprProcessor.java:829)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.SequenceStatement.toJava(SequenceStatement.java:107)
      //   at org.jetbrains.java.decompiler.modules.decompiler.exps.SwitchExprent.toJava(SwitchExprent.java:167)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.getCastedExprent(ExprProcessor.java:1014)
      //   at org.jetbrains.java.decompiler.modules.decompiler.exps.ExitExprent.toJava(ExitExprent.java:86)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.listToJava(ExprProcessor.java:891)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.BasicBlockStatement.toJava(BasicBlockStatement.java:91)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.jmpWrapper(ExprProcessor.java:829)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.IfStatement.toJava(IfStatement.java:238)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.jmpWrapper(ExprProcessor.java:829)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.IfStatement.toJava(IfStatement.java:238)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.jmpWrapper(ExprProcessor.java:829)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.SequenceStatement.toJava(SequenceStatement.java:107)
      //   at org.jetbrains.java.decompiler.modules.decompiler.exps.SwitchExprent.toJava(SwitchExprent.java:167)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.getCastedExprent(ExprProcessor.java:1014)
      //   at org.jetbrains.java.decompiler.modules.decompiler.exps.ExitExprent.toJava(ExitExprent.java:86)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.listToJava(ExprProcessor.java:891)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.BasicBlockStatement.toJava(BasicBlockStatement.java:91)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.jmpWrapper(ExprProcessor.java:829)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.IfStatement.toJava(IfStatement.java:238)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.jmpWrapper(ExprProcessor.java:829)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.IfStatement.toJava(IfStatement.java:238)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.jmpWrapper(ExprProcessor.java:829)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.SequenceStatement.toJava(SequenceStatement.java:107)
      //   at org.jetbrains.java.decompiler.modules.decompiler.exps.SwitchExprent.toJava(SwitchExprent.java:167)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.getCastedExprent(ExprProcessor.java:1014)
      //   at org.jetbrains.java.decompiler.modules.decompiler.exps.ExitExprent.toJava(ExitExprent.java:86)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.listToJava(ExprProcessor.java:891)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.BasicBlockStatement.toJava(BasicBlockStatement.java:91)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.jmpWrapper(ExprProcessor.java:829)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.IfStatement.toJava(IfStatement.java:238)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.jmpWrapper(ExprProcessor.java:829)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.IfStatement.toJava(IfStatement.java:238)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.jmpWrapper(ExprProcessor.java:829)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.SequenceStatement.toJava(SequenceStatement.java:107)
      //   at org.jetbrains.java.decompiler.modules.decompiler.exps.SwitchExprent.toJava(SwitchExprent.java:167)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.getCastedExprent(ExprProcessor.java:1014)
      //   at org.jetbrains.java.decompiler.modules.decompiler.exps.ExitExprent.toJava(ExitExprent.java:86)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.listToJava(ExprProcessor.java:891)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.BasicBlockStatement.toJava(BasicBlockStatement.java:91)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.jmpWrapper(ExprProcessor.java:829)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.IfStatement.toJava(IfStatement.java:238)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.jmpWrapper(ExprProcessor.java:829)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.IfStatement.toJava(IfStatement.java:238)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.jmpWrapper(ExprProcessor.java:829)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.SequenceStatement.toJava(SequenceStatement.java:107)
      //   at org.jetbrains.java.decompiler.modules.decompiler.exps.SwitchExprent.toJava(SwitchExprent.java:167)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.getCastedExprent(ExprProcessor.java:1014)
      //   at org.jetbrains.java.decompiler.modules.decompiler.exps.ExitExprent.toJava(ExitExprent.java:86)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.listToJava(ExprProcessor.java:891)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.BasicBlockStatement.toJava(BasicBlockStatement.java:91)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.jmpWrapper(ExprProcessor.java:829)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.IfStatement.toJava(IfStatement.java:238)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.jmpWrapper(ExprProcessor.java:829)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.IfStatement.toJava(IfStatement.java:238)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.jmpWrapper(ExprProcessor.java:829)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.SequenceStatement.toJava(SequenceStatement.java:107)
      //   at org.jetbrains.java.decompiler.modules.decompiler.exps.SwitchExprent.toJava(SwitchExprent.java:167)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.getCastedExprent(ExprProcessor.java:1014)
      //   at org.jetbrains.java.decompiler.modules.decompiler.exps.ExitExprent.toJava(ExitExprent.java:86)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.listToJava(ExprProcessor.java:891)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.BasicBlockStatement.toJava(BasicBlockStatement.java:91)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.jmpWrapper(ExprProcessor.java:829)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.IfStatement.toJava(IfStatement.java:238)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.jmpWrapper(ExprProcessor.java:829)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.IfStatement.toJava(IfStatement.java:238)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.jmpWrapper(ExprProcessor.java:829)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.SequenceStatement.toJava(SequenceStatement.java:107)
      //   at org.jetbrains.java.decompiler.modules.decompiler.exps.SwitchExprent.toJava(SwitchExprent.java:167)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.getCastedExprent(ExprProcessor.java:1014)
      //   at org.jetbrains.java.decompiler.modules.decompiler.exps.ExitExprent.toJava(ExitExprent.java:86)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.listToJava(ExprProcessor.java:891)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.BasicBlockStatement.toJava(BasicBlockStatement.java:91)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.jmpWrapper(ExprProcessor.java:829)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.IfStatement.toJava(IfStatement.java:238)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.jmpWrapper(ExprProcessor.java:829)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.IfStatement.toJava(IfStatement.java:238)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.jmpWrapper(ExprProcessor.java:829)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.SequenceStatement.toJava(SequenceStatement.java:107)
      //   at org.jetbrains.java.decompiler.modules.decompiler.exps.SwitchExprent.toJava(SwitchExprent.java:167)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.getCastedExprent(ExprProcessor.java:1014)
      //   at org.jetbrains.java.decompiler.modules.decompiler.exps.ExitExprent.toJava(ExitExprent.java:86)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.listToJava(ExprProcessor.java:891)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.BasicBlockStatement.toJava(BasicBlockStatement.java:91)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.jmpWrapper(ExprProcessor.java:829)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.IfStatement.toJava(IfStatement.java:238)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.jmpWrapper(ExprProcessor.java:829)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.IfStatement.toJava(IfStatement.java:238)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.jmpWrapper(ExprProcessor.java:829)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.SequenceStatement.toJava(SequenceStatement.java:107)
      //   at org.jetbrains.java.decompiler.modules.decompiler.exps.SwitchExprent.toJava(SwitchExprent.java:167)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.getCastedExprent(ExprProcessor.java:1014)
      //   at org.jetbrains.java.decompiler.modules.decompiler.exps.ExitExprent.toJava(ExitExprent.java:86)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.listToJava(ExprProcessor.java:891)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.BasicBlockStatement.toJava(BasicBlockStatement.java:91)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.jmpWrapper(ExprProcessor.java:829)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.IfStatement.toJava(IfStatement.java:238)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.jmpWrapper(ExprProcessor.java:829)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.IfStatement.toJava(IfStatement.java:238)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.jmpWrapper(ExprProcessor.java:829)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.SequenceStatement.toJava(SequenceStatement.java:107)
      //   at org.jetbrains.java.decompiler.modules.decompiler.exps.SwitchExprent.toJava(SwitchExprent.java:167)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.getCastedExprent(ExprProcessor.java:1014)
      //   at org.jetbrains.java.decompiler.modules.decompiler.exps.ExitExprent.toJava(ExitExprent.java:86)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.listToJava(ExprProcessor.java:891)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.BasicBlockStatement.toJava(BasicBlockStatement.java:91)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.jmpWrapper(ExprProcessor.java:829)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.IfStatement.toJava(IfStatement.java:238)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.jmpWrapper(ExprProcessor.java:829)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.IfStatement.toJava(IfStatement.java:238)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.jmpWrapper(ExprProcessor.java:829)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.SequenceStatement.toJava(SequenceStatement.java:107)
      //   at org.jetbrains.java.decompiler.modules.decompiler.exps.SwitchExprent.toJava(SwitchExprent.java:167)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.getCastedExprent(ExprProcessor.java:1014)
      //   at org.jetbrains.java.decompiler.modules.decompiler.exps.ExitExprent.toJava(ExitExprent.java:86)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.listToJava(ExprProcessor.java:891)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.BasicBlockStatement.toJava(BasicBlockStatement.java:91)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.jmpWrapper(ExprProcessor.java:829)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.IfStatement.toJava(IfStatement.java:238)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.jmpWrapper(ExprProcessor.java:829)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.IfStatement.toJava(IfStatement.java:238)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.jmpWrapper(ExprProcessor.java:829)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.SequenceStatement.toJava(SequenceStatement.java:107)
      //   at org.jetbrains.java.decompiler.modules.decompiler.exps.SwitchExprent.toJava(SwitchExprent.java:167)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.getCastedExprent(ExprProcessor.java:1014)
      //   at org.jetbrains.java.decompiler.modules.decompiler.exps.ExitExprent.toJava(ExitExprent.java:86)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.listToJava(ExprProcessor.java:891)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.BasicBlockStatement.toJava(BasicBlockStatement.java:91)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.jmpWrapper(ExprProcessor.java:829)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.IfStatement.toJava(IfStatement.java:238)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.jmpWrapper(ExprProcessor.java:829)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.IfStatement.toJava(IfStatement.java:238)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.jmpWrapper(ExprProcessor.java:829)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.SequenceStatement.toJava(SequenceStatement.java:107)
      //   at org.jetbrains.java.decompiler.modules.decompiler.exps.SwitchExprent.toJava(SwitchExprent.java:167)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.getCastedExprent(ExprProcessor.java:1014)
      //   at org.jetbrains.java.decompiler.modules.decompiler.exps.ExitExprent.toJava(ExitExprent.java:86)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.listToJava(ExprProcessor.java:891)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.BasicBlockStatement.toJava(BasicBlockStatement.java:91)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.jmpWrapper(ExprProcessor.java:829)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.IfStatement.toJava(IfStatement.java:238)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.jmpWrapper(ExprProcessor.java:829)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.IfStatement.toJava(IfStatement.java:238)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.jmpWrapper(ExprProcessor.java:829)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.SequenceStatement.toJava(SequenceStatement.java:107)
      //   at org.jetbrains.java.decompiler.modules.decompiler.exps.SwitchExprent.toJava(SwitchExprent.java:167)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.getCastedExprent(ExprProcessor.java:1014)
      //   at org.jetbrains.java.decompiler.modules.decompiler.exps.ExitExprent.toJava(ExitExprent.java:86)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.listToJava(ExprProcessor.java:891)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.BasicBlockStatement.toJava(BasicBlockStatement.java:91)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.jmpWrapper(ExprProcessor.java:829)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.IfStatement.toJava(IfStatement.java:238)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.jmpWrapper(ExprProcessor.java:829)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.IfStatement.toJava(IfStatement.java:238)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.jmpWrapper(ExprProcessor.java:829)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.SequenceStatement.toJava(SequenceStatement.java:107)
      //   at org.jetbrains.java.decompiler.modules.decompiler.exps.SwitchExprent.toJava(SwitchExprent.java:167)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.getCastedExprent(ExprProcessor.java:1014)
      //   at org.jetbrains.java.decompiler.modules.decompiler.exps.ExitExprent.toJava(ExitExprent.java:86)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.listToJava(ExprProcessor.java:891)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.BasicBlockStatement.toJava(BasicBlockStatement.java:91)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.jmpWrapper(ExprProcessor.java:829)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.IfStatement.toJava(IfStatement.java:238)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.jmpWrapper(ExprProcessor.java:829)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.IfStatement.toJava(IfStatement.java:238)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.jmpWrapper(ExprProcessor.java:829)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.SequenceStatement.toJava(SequenceStatement.java:107)
      //   at org.jetbrains.java.decompiler.modules.decompiler.exps.SwitchExprent.toJava(SwitchExprent.java:167)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.getCastedExprent(ExprProcessor.java:1014)
      //   at org.jetbrains.java.decompiler.modules.decompiler.exps.ExitExprent.toJava(ExitExprent.java:86)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.listToJava(ExprProcessor.java:891)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.BasicBlockStatement.toJava(BasicBlockStatement.java:91)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.jmpWrapper(ExprProcessor.java:829)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.IfStatement.toJava(IfStatement.java:238)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.jmpWrapper(ExprProcessor.java:829)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.IfStatement.toJava(IfStatement.java:238)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.jmpWrapper(ExprProcessor.java:829)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.SequenceStatement.toJava(SequenceStatement.java:107)
      //   at org.jetbrains.java.decompiler.modules.decompiler.exps.SwitchExprent.toJava(SwitchExprent.java:167)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.getCastedExprent(ExprProcessor.java:1014)
      //   at org.jetbrains.java.decompiler.modules.decompiler.exps.ExitExprent.toJava(ExitExprent.java:86)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.listToJava(ExprProcessor.java:891)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.BasicBlockStatement.toJava(BasicBlockStatement.java:91)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.jmpWrapper(ExprProcessor.java:829)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.IfStatement.toJava(IfStatement.java:238)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.jmpWrapper(ExprProcessor.java:829)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.IfStatement.toJava(IfStatement.java:238)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.jmpWrapper(ExprProcessor.java:829)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.SequenceStatement.toJava(SequenceStatement.java:107)
      //   at org.jetbrains.java.decompiler.modules.decompiler.exps.SwitchExprent.toJava(SwitchExprent.java:167)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.getCastedExprent(ExprProcessor.java:1014)
      //   at org.jetbrains.java.decompiler.modules.decompiler.exps.ExitExprent.toJava(ExitExprent.java:86)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.listToJava(ExprProcessor.java:891)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.BasicBlockStatement.toJava(BasicBlockStatement.java:91)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.jmpWrapper(ExprProcessor.java:829)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.IfStatement.toJava(IfStatement.java:238)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.jmpWrapper(ExprProcessor.java:829)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.IfStatement.toJava(IfStatement.java:238)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.jmpWrapper(ExprProcessor.java:829)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.SequenceStatement.toJava(SequenceStatement.java:107)
      //   at org.jetbrains.java.decompiler.modules.decompiler.exps.SwitchExprent.toJava(SwitchExprent.java:167)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.getCastedExprent(ExprProcessor.java:1014)
      //   at org.jetbrains.java.decompiler.modules.decompiler.exps.ExitExprent.toJava(ExitExprent.java:86)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.listToJava(ExprProcessor.java:891)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.BasicBlockStatement.toJava(BasicBlockStatement.java:91)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.jmpWrapper(ExprProcessor.java:829)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.IfStatement.toJava(IfStatement.java:238)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.jmpWrapper(ExprProcessor.java:829)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.IfStatement.toJava(IfStatement.java:238)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.jmpWrapper(ExprProcessor.java:829)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.SequenceStatement.toJava(SequenceStatement.java:107)
      //   at org.jetbrains.java.decompiler.modules.decompiler.exps.SwitchExprent.toJava(SwitchExprent.java:167)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.getCastedExprent(ExprProcessor.java:1014)
      //   at org.jetbrains.java.decompiler.modules.decompiler.exps.ExitExprent.toJava(ExitExprent.java:86)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.listToJava(ExprProcessor.java:891)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.BasicBlockStatement.toJava(BasicBlockStatement.java:91)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.jmpWrapper(ExprProcessor.java:829)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.IfStatement.toJava(IfStatement.java:238)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.jmpWrapper(ExprProcessor.java:829)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.IfStatement.toJava(IfStatement.java:238)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.jmpWrapper(ExprProcessor.java:829)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.SequenceStatement.toJava(SequenceStatement.java:107)
      //   at org.jetbrains.java.decompiler.modules.decompiler.exps.SwitchExprent.toJava(SwitchExprent.java:167)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.getCastedExprent(ExprProcessor.java:1014)
      //   at org.jetbrains.java.decompiler.modules.decompiler.exps.ExitExprent.toJava(ExitExprent.java:86)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.listToJava(ExprProcessor.java:891)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.BasicBlockStatement.toJava(BasicBlockStatement.java:91)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.jmpWrapper(ExprProcessor.java:829)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.IfStatement.toJava(IfStatement.java:238)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.jmpWrapper(ExprProcessor.java:829)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.IfStatement.toJava(IfStatement.java:238)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.jmpWrapper(ExprProcessor.java:829)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.SequenceStatement.toJava(SequenceStatement.java:107)
      //   at org.jetbrains.java.decompiler.modules.decompiler.exps.SwitchExprent.toJava(SwitchExprent.java:167)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.getCastedExprent(ExprProcessor.java:1014)
      //   at org.jetbrains.java.decompiler.modules.decompiler.exps.ExitExprent.toJava(ExitExprent.java:86)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.listToJava(ExprProcessor.java:891)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.BasicBlockStatement.toJava(BasicBlockStatement.java:91)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.jmpWrapper(ExprProcessor.java:829)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.IfStatement.toJava(IfStatement.java:238)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.jmpWrapper(ExprProcessor.java:829)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.IfStatement.toJava(IfStatement.java:238)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.jmpWrapper(ExprProcessor.java:829)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.SequenceStatement.toJava(SequenceStatement.java:107)
      //   at org.jetbrains.java.decompiler.modules.decompiler.exps.SwitchExprent.toJava(SwitchExprent.java:167)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.getCastedExprent(ExprProcessor.java:1014)
      //   at org.jetbrains.java.decompiler.modules.decompiler.exps.ExitExprent.toJava(ExitExprent.java:86)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.listToJava(ExprProcessor.java:891)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.BasicBlockStatement.toJava(BasicBlockStatement.java:91)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.jmpWrapper(ExprProcessor.java:829)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.IfStatement.toJava(IfStatement.java:238)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.jmpWrapper(ExprProcessor.java:829)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.IfStatement.toJava(IfStatement.java:238)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.jmpWrapper(ExprProcessor.java:829)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.SequenceStatement.toJava(SequenceStatement.java:107)
      //   at org.jetbrains.java.decompiler.modules.decompiler.exps.SwitchExprent.toJava(SwitchExprent.java:167)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.getCastedExprent(ExprProcessor.java:1014)
      //   at org.jetbrains.java.decompiler.modules.decompiler.exps.ExitExprent.toJava(ExitExprent.java:86)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.listToJava(ExprProcessor.java:891)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.BasicBlockStatement.toJava(BasicBlockStatement.java:91)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.jmpWrapper(ExprProcessor.java:829)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.IfStatement.toJava(IfStatement.java:238)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.jmpWrapper(ExprProcessor.java:829)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.IfStatement.toJava(IfStatement.java:238)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.jmpWrapper(ExprProcessor.java:829)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.SequenceStatement.toJava(SequenceStatement.java:107)
      //   at org.jetbrains.java.decompiler.modules.decompiler.exps.SwitchExprent.toJava(SwitchExprent.java:167)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.getCastedExprent(ExprProcessor.java:1014)
      //   at org.jetbrains.java.decompiler.modules.decompiler.exps.ExitExprent.toJava(ExitExprent.java:86)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.listToJava(ExprProcessor.java:891)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.BasicBlockStatement.toJava(BasicBlockStatement.java:91)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.jmpWrapper(ExprProcessor.java:829)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.IfStatement.toJava(IfStatement.java:238)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.jmpWrapper(ExprProcessor.java:829)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.IfStatement.toJava(IfStatement.java:238)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.jmpWrapper(ExprProcessor.java:829)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.SequenceStatement.toJava(SequenceStatement.java:107)
      //   at org.jetbrains.java.decompiler.modules.decompiler.exps.SwitchExprent.toJava(SwitchExprent.java:167)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.getCastedExprent(ExprProcessor.java:1014)
      //   at org.jetbrains.java.decompiler.modules.decompiler.exps.ExitExprent.toJava(ExitExprent.java:86)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.listToJava(ExprProcessor.java:891)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.BasicBlockStatement.toJava(BasicBlockStatement.java:91)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.jmpWrapper(ExprProcessor.java:829)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.IfStatement.toJava(IfStatement.java:238)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.jmpWrapper(ExprProcessor.java:829)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.IfStatement.toJava(IfStatement.java:238)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.jmpWrapper(ExprProcessor.java:829)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.SequenceStatement.toJava(SequenceStatement.java:107)
      //   at org.jetbrains.java.decompiler.modules.decompiler.exps.SwitchExprent.toJava(SwitchExprent.java:167)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.getCastedExprent(ExprProcessor.java:1014)
      //   at org.jetbrains.java.decompiler.modules.decompiler.exps.ExitExprent.toJava(ExitExprent.java:86)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.listToJava(ExprProcessor.java:891)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.BasicBlockStatement.toJava(BasicBlockStatement.java:91)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.jmpWrapper(ExprProcessor.java:829)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.IfStatement.toJava(IfStatement.java:238)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.jmpWrapper(ExprProcessor.java:829)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.IfStatement.toJava(IfStatement.java:238)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.jmpWrapper(ExprProcessor.java:829)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.SequenceStatement.toJava(SequenceStatement.java:107)
      //   at org.jetbrains.java.decompiler.modules.decompiler.exps.SwitchExprent.toJava(SwitchExprent.java:167)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.getCastedExprent(ExprProcessor.java:1014)
      //   at org.jetbrains.java.decompiler.modules.decompiler.exps.ExitExprent.toJava(ExitExprent.java:86)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.listToJava(ExprProcessor.java:891)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.BasicBlockStatement.toJava(BasicBlockStatement.java:91)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.jmpWrapper(ExprProcessor.java:829)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.IfStatement.toJava(IfStatement.java:238)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.jmpWrapper(ExprProcessor.java:829)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.IfStatement.toJava(IfStatement.java:238)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.jmpWrapper(ExprProcessor.java:829)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.SequenceStatement.toJava(SequenceStatement.java:107)
      //   at org.jetbrains.java.decompiler.modules.decompiler.exps.SwitchExprent.toJava(SwitchExprent.java:167)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.getCastedExprent(ExprProcessor.java:1014)
      //   at org.jetbrains.java.decompiler.modules.decompiler.exps.ExitExprent.toJava(ExitExprent.java:86)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.listToJava(ExprProcessor.java:891)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.BasicBlockStatement.toJava(BasicBlockStatement.java:91)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.jmpWrapper(ExprProcessor.java:829)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.IfStatement.toJava(IfStatement.java:238)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.jmpWrapper(ExprProcessor.java:829)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.IfStatement.toJava(IfStatement.java:238)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.jmpWrapper(ExprProcessor.java:829)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.SequenceStatement.toJava(SequenceStatement.java:107)
      //   at org.jetbrains.java.decompiler.modules.decompiler.exps.SwitchExprent.toJava(SwitchExprent.java:167)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.getCastedExprent(ExprProcessor.java:1014)
      //   at org.jetbrains.java.decompiler.modules.decompiler.exps.ExitExprent.toJava(ExitExprent.java:86)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.listToJava(ExprProcessor.java:891)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.BasicBlockStatement.toJava(BasicBlockStatement.java:91)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.jmpWrapper(ExprProcessor.java:829)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.IfStatement.toJava(IfStatement.java:238)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.jmpWrapper(ExprProcessor.java:829)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.IfStatement.toJava(IfStatement.java:238)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.jmpWrapper(ExprProcessor.java:829)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.SequenceStatement.toJava(SequenceStatement.java:107)
      //   at org.jetbrains.java.decompiler.modules.decompiler.exps.SwitchExprent.toJava(SwitchExprent.java:167)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.getCastedExprent(ExprProcessor.java:1014)
      //   at org.jetbrains.java.decompiler.modules.decompiler.exps.ExitExprent.toJava(ExitExprent.java:86)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.listToJava(ExprProcessor.java:891)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.BasicBlockStatement.toJava(BasicBlockStatement.java:91)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.jmpWrapper(ExprProcessor.java:829)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.IfStatement.toJava(IfStatement.java:238)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.jmpWrapper(ExprProcessor.java:829)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.IfStatement.toJava(IfStatement.java:238)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.jmpWrapper(ExprProcessor.java:829)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.SequenceStatement.toJava(SequenceStatement.java:107)
      //   at org.jetbrains.java.decompiler.modules.decompiler.exps.SwitchExprent.toJava(SwitchExprent.java:167)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.getCastedExprent(ExprProcessor.java:1014)
      //   at org.jetbrains.java.decompiler.modules.decompiler.exps.ExitExprent.toJava(ExitExprent.java:86)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.listToJava(ExprProcessor.java:891)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.BasicBlockStatement.toJava(BasicBlockStatement.java:91)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.jmpWrapper(ExprProcessor.java:829)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.IfStatement.toJava(IfStatement.java:238)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.jmpWrapper(ExprProcessor.java:829)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.IfStatement.toJava(IfStatement.java:238)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.jmpWrapper(ExprProcessor.java:829)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.SequenceStatement.toJava(SequenceStatement.java:107)
      //   at org.jetbrains.java.decompiler.modules.decompiler.exps.SwitchExprent.toJava(SwitchExprent.java:167)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.getCastedExprent(ExprProcessor.java:1014)
      //   at org.jetbrains.java.decompiler.modules.decompiler.exps.ExitExprent.toJava(ExitExprent.java:86)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.listToJava(ExprProcessor.java:891)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.BasicBlockStatement.toJava(BasicBlockStatement.java:91)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.jmpWrapper(ExprProcessor.java:829)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.IfStatement.toJava(IfStatement.java:238)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.jmpWrapper(ExprProcessor.java:829)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.IfStatement.toJava(IfStatement.java:238)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.jmpWrapper(ExprProcessor.java:829)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.SequenceStatement.toJava(SequenceStatement.java:107)
      //   at org.jetbrains.java.decompiler.modules.decompiler.exps.SwitchExprent.toJava(SwitchExprent.java:167)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.getCastedExprent(ExprProcessor.java:1014)
      //   at org.jetbrains.java.decompiler.modules.decompiler.exps.ExitExprent.toJava(ExitExprent.java:86)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.listToJava(ExprProcessor.java:891)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.BasicBlockStatement.toJava(BasicBlockStatement.java:91)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.jmpWrapper(ExprProcessor.java:829)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.IfStatement.toJava(IfStatement.java:238)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.jmpWrapper(ExprProcessor.java:829)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.IfStatement.toJava(IfStatement.java:238)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.jmpWrapper(ExprProcessor.java:829)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.SequenceStatement.toJava(SequenceStatement.java:107)
      //   at org.jetbrains.java.decompiler.modules.decompiler.exps.SwitchExprent.toJava(SwitchExprent.java:167)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.getCastedExprent(ExprProcessor.java:1014)
      //   at org.jetbrains.java.decompiler.modules.decompiler.exps.ExitExprent.toJava(ExitExprent.java:86)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.listToJava(ExprProcessor.java:891)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.BasicBlockStatement.toJava(BasicBlockStatement.java:91)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.jmpWrapper(ExprProcessor.java:829)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.IfStatement.toJava(IfStatement.java:238)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.jmpWrapper(ExprProcessor.java:829)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.IfStatement.toJava(IfStatement.java:238)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.jmpWrapper(ExprProcessor.java:829)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.SequenceStatement.toJava(SequenceStatement.java:107)
      //   at org.jetbrains.java.decompiler.modules.decompiler.exps.SwitchExprent.toJava(SwitchExprent.java:167)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.getCastedExprent(ExprProcessor.java:1014)
      //   at org.jetbrains.java.decompiler.modules.decompiler.exps.ExitExprent.toJava(ExitExprent.java:86)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.listToJava(ExprProcessor.java:891)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.BasicBlockStatement.toJava(BasicBlockStatement.java:91)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.jmpWrapper(ExprProcessor.java:829)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.IfStatement.toJava(IfStatement.java:238)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.jmpWrapper(ExprProcessor.java:829)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.IfStatement.toJava(IfStatement.java:238)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.jmpWrapper(ExprProcessor.java:829)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.SequenceStatement.toJava(SequenceStatement.java:107)
      //   at org.jetbrains.java.decompiler.modules.decompiler.exps.SwitchExprent.toJava(SwitchExprent.java:167)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.getCastedExprent(ExprProcessor.java:1014)
      //   at org.jetbrains.java.decompiler.modules.decompiler.exps.ExitExprent.toJava(ExitExprent.java:86)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.listToJava(ExprProcessor.java:891)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.BasicBlockStatement.toJava(BasicBlockStatement.java:91)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.jmpWrapper(ExprProcessor.java:829)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.IfStatement.toJava(IfStatement.java:238)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.jmpWrapper(ExprProcessor.java:829)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.IfStatement.toJava(IfStatement.java:238)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.jmpWrapper(ExprProcessor.java:829)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.SequenceStatement.toJava(SequenceStatement.java:107)
      //   at org.jetbrains.java.decompiler.modules.decompiler.exps.SwitchExprent.toJava(SwitchExprent.java:167)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.getCastedExprent(ExprProcessor.java:1014)
      //   at org.jetbrains.java.decompiler.modules.decompiler.exps.ExitExprent.toJava(ExitExprent.java:86)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.listToJava(ExprProcessor.java:891)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.BasicBlockStatement.toJava(BasicBlockStatement.java:91)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.jmpWrapper(ExprProcessor.java:829)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.IfStatement.toJava(IfStatement.java:238)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.jmpWrapper(ExprProcessor.java:829)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.IfStatement.toJava(IfStatement.java:238)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.jmpWrapper(ExprProcessor.java:829)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.SequenceStatement.toJava(SequenceStatement.java:107)
      //   at org.jetbrains.java.decompiler.modules.decompiler.exps.SwitchExprent.toJava(SwitchExprent.java:167)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.getCastedExprent(ExprProcessor.java:1014)
      //   at org.jetbrains.java.decompiler.modules.decompiler.exps.ExitExprent.toJava(ExitExprent.java:86)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.listToJava(ExprProcessor.java:891)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.BasicBlockStatement.toJava(BasicBlockStatement.java:91)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.jmpWrapper(ExprProcessor.java:829)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.IfStatement.toJava(IfStatement.java:238)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.jmpWrapper(ExprProcessor.java:829)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.IfStatement.toJava(IfStatement.java:238)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.jmpWrapper(ExprProcessor.java:829)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.SequenceStatement.toJava(SequenceStatement.java:107)
      //   at org.jetbrains.java.decompiler.modules.decompiler.exps.SwitchExprent.toJava(SwitchExprent.java:167)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.getCastedExprent(ExprProcessor.java:1014)
      //   at org.jetbrains.java.decompiler.modules.decompiler.exps.ExitExprent.toJava(ExitExprent.java:86)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.listToJava(ExprProcessor.java:891)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.BasicBlockStatement.toJava(BasicBlockStatement.java:91)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.jmpWrapper(ExprProcessor.java:829)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.IfStatement.toJava(IfStatement.java:238)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.jmpWrapper(ExprProcessor.java:829)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.IfStatement.toJava(IfStatement.java:238)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.jmpWrapper(ExprProcessor.java:829)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.SequenceStatement.toJava(SequenceStatement.java:107)
      //   at org.jetbrains.java.decompiler.modules.decompiler.exps.SwitchExprent.toJava(SwitchExprent.java:167)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.getCastedExprent(ExprProcessor.java:1014)
      //   at org.jetbrains.java.decompiler.modules.decompiler.exps.ExitExprent.toJava(ExitExprent.java:86)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.listToJava(ExprProcessor.java:891)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.BasicBlockStatement.toJava(BasicBlockStatement.java:91)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.jmpWrapper(ExprProcessor.java:829)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.IfStatement.toJava(IfStatement.java:238)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.jmpWrapper(ExprProcessor.java:829)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.IfStatement.toJava(IfStatement.java:238)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.jmpWrapper(ExprProcessor.java:829)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.SequenceStatement.toJava(SequenceStatement.java:107)
      //   at org.jetbrains.java.decompiler.modules.decompiler.exps.SwitchExprent.toJava(SwitchExprent.java:167)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.getCastedExprent(ExprProcessor.java:1014)
      //   at org.jetbrains.java.decompiler.modules.decompiler.exps.ExitExprent.toJava(ExitExprent.java:86)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.listToJava(ExprProcessor.java:891)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.BasicBlockStatement.toJava(BasicBlockStatement.java:91)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.jmpWrapper(ExprProcessor.java:829)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.IfStatement.toJava(IfStatement.java:238)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.jmpWrapper(ExprProcessor.java:829)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.IfStatement.toJava(IfStatement.java:238)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.jmpWrapper(ExprProcessor.java:829)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.SequenceStatement.toJava(SequenceStatement.java:107)
      //   at org.jetbrains.java.decompiler.modules.decompiler.exps.SwitchExprent.toJava(SwitchExprent.java:167)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.getCastedExprent(ExprProcessor.java:1014)
      //   at org.jetbrains.java.decompiler.modules.decompiler.exps.ExitExprent.toJava(ExitExprent.java:86)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.listToJava(ExprProcessor.java:891)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.BasicBlockStatement.toJava(BasicBlockStatement.java:91)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.jmpWrapper(ExprProcessor.java:829)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.IfStatement.toJava(IfStatement.java:238)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.jmpWrapper(ExprProcessor.java:829)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.IfStatement.toJava(IfStatement.java:238)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.jmpWrapper(ExprProcessor.java:829)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.SequenceStatement.toJava(SequenceStatement.java:107)
      //   at org.jetbrains.java.decompiler.modules.decompiler.exps.SwitchExprent.toJava(SwitchExprent.java:167)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.getCastedExprent(ExprProcessor.java:1014)
      //   at org.jetbrains.java.decompiler.modules.decompiler.exps.ExitExprent.toJava(ExitExprent.java:86)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.listToJava(ExprProcessor.java:891)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.BasicBlockStatement.toJava(BasicBlockStatement.java:91)
      //   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.jmpWrapper(ExprProcessor.java:829)
      //   at org.jetbrains.java.decompiler.modules.decompiler.stats.IfStatement.toJava(IfStatement.java:238)
      //
      // Bytecode:
      // 000: aload 1
      // 001: arraylength
      // 002: bipush 1
      // 003: if_icmpge 010
      // 006: aload 0
      // 007: ldc_w "wait requires condition argument"
      // 00a: ldc "INVALID_ARGS"
      // 00c: invokevirtual io/karatelabs/ext/robot/BotDriver.fail (Ljava/lang/String;Ljava/lang/String;)Lio/karatelabs/ext/robot/c;
      // 00f: athrow
      // 010: aload 1
      // 011: bipush 0
      // 012: aaload
      // 013: astore 2
      // 014: invokestatic java/lang/System.currentTimeMillis ()J
      // 017: lstore 3
      // 018: aload 2
      // 019: instanceof java/lang/Number
      // 01c: ifeq 067
      // 01f: aload 2
      // 020: checkcast java/lang/Number
      // 023: invokevirtual java/lang/Number.longValue ()J
      // 026: lstore 5
      // 028: aload 0
      // 029: getfield io/karatelabs/ext/robot/BotDriver.diagnostics Lio/karatelabs/ext/robot/b;
      // 02c: invokevirtual io/karatelabs/ext/robot/b.a ()Z
      // 02f: istore 7
      // 031: lload 5
      // 033: invokestatic java/lang/Thread.sleep (J)V
      // 036: new java/util/LinkedHashMap
      // 039: dup
      // 03a: invokespecial java/util/LinkedHashMap.<init> ()V
      // 03d: astore 8
      // 03f: aload 8
      // 041: ldc_w "elapsed"
      // 044: invokestatic java/lang/System.currentTimeMillis ()J
      // 047: lload 3
      // 048: lsub
      // 049: invokestatic java/lang/Long.valueOf (J)Ljava/lang/Long;
      // 04c: invokeinterface java/util/Map.put (Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object; 3
      // 051: pop
      // 052: iload 7
      // 054: ifeq 064
      // 057: aload 8
      // 059: ldc_w "hint"
      // 05c: ldc "No pending network activity — this wait may be unnecessary"
      // 05e: invokeinterface java/util/Map.put (Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object; 3
      // 063: pop
      // 064: aload 8
      // 066: areturn
      // 067: aload 2
      // 068: invokestatic java/lang/String.valueOf (Ljava/lang/Object;)Ljava/lang/String;
      // 06b: astore 5
      // 06d: aload 5
      // 06f: astore 6
      // 071: bipush -1
      // 072: istore 7
      // 074: aload 6
      // 076: invokevirtual java/lang/String.hashCode ()I
      // 079: lookupswitch 83 3 116079 52 3227604 35 110371416 69
      // 09c: aload 6
      // 09e: ldc_w "idle"
      // 0a1: invokevirtual java/lang/String.equals (Ljava/lang/Object;)Z
      // 0a4: ifeq 0cc
      // 0a7: bipush 0
      // 0a8: istore 7
      // 0aa: goto 0cc
      // 0ad: aload 6
      // 0af: ldc_w "url"
      // 0b2: invokevirtual java/lang/String.equals (Ljava/lang/Object;)Z
      // 0b5: ifeq 0cc
      // 0b8: bipush 1
      // 0b9: istore 7
      // 0bb: goto 0cc
      // 0be: aload 6
      // 0c0: ldc_w "title"
      // 0c3: invokevirtual java/lang/String.equals (Ljava/lang/Object;)Z
      // 0c6: ifeq 0cc
      // 0c9: bipush 2
      // 0ca: istore 7
      // 0cc: iload 7
      // 0ce: tableswitch 156 0 2 26 90 123
      // 0e8: aload 0
      // 0e9: ldc_w "document.readyState === 'complete'"
      // 0ec: invokevirtual io/karatelabs/ext/robot/BotDriver.softWaitForReadyState (Ljava/lang/String;)V
      // 0ef: aload 0
      // 0f0: invokevirtual io/karatelabs/ext/robot/BotDriver.waitForStable ()Ljava/lang/String;
      // 0f3: astore 8
      // 0f5: new java/util/LinkedHashMap
      // 0f8: dup
      // 0f9: invokespecial java/util/LinkedHashMap.<init> ()V
      // 0fc: astore 9
      // 0fe: aload 9
      // 100: ldc_w "elapsed"
      // 103: invokestatic java/lang/System.currentTimeMillis ()J
      // 106: lload 3
      // 107: lsub
      // 108: invokestatic java/lang/Long.valueOf (J)Ljava/lang/Long;
      // 10b: invokeinterface java/util/Map.put (Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object; 3
      // 110: pop
      // 111: aload 8
      // 113: ifnull 123
      // 116: aload 9
      // 118: ldc_w "hint"
      // 11b: aload 8
      // 11d: invokeinterface java/util/Map.put (Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object; 3
      // 122: pop
      // 123: aload 9
      // 125: goto 227
      // 128: aload 1
      // 129: arraylength
      // 12a: bipush 2
      // 12b: if_icmpge 138
      // 12e: aload 0
      // 12f: ldc_w "wait('url') requires pattern argument"
      // 132: ldc "INVALID_ARGS"
      // 134: invokevirtual io/karatelabs/ext/robot/BotDriver.fail (Ljava/lang/String;Ljava/lang/String;)Lio/karatelabs/ext/robot/c;
      // 137: athrow
      // 138: aload 0
      // 139: ldc_w "url"
      // 13c: aload 1
      // 13d: bipush 1
      // 13e: aaload
      // 13f: invokestatic java/lang/String.valueOf (Ljava/lang/Object;)Ljava/lang/String;
      // 142: lload 3
      // 143: invokevirtual io/karatelabs/ext/robot/BotDriver.waitForTitleOrUrl (Ljava/lang/String;Ljava/lang/String;J)Ljava/util/Map;
      // 146: goto 227
      // 149: aload 1
      // 14a: arraylength
      // 14b: bipush 2
      // 14c: if_icmpge 159
      // 14f: aload 0
      // 150: ldc_w "wait('title') requires pattern argument"
      // 153: ldc "INVALID_ARGS"
      // 155: invokevirtual io/karatelabs/ext/robot/BotDriver.fail (Ljava/lang/String;Ljava/lang/String;)Lio/karatelabs/ext/robot/c;
      // 158: athrow
      // 159: aload 0
      // 15a: ldc_w "title"
      // 15d: aload 1
      // 15e: bipush 1
      // 15f: aaload
      // 160: invokestatic java/lang/String.valueOf (Ljava/lang/Object;)Ljava/lang/String;
      // 163: lload 3
      // 164: invokevirtual io/karatelabs/ext/robot/BotDriver.waitForTitleOrUrl (Ljava/lang/String;Ljava/lang/String;J)Ljava/util/Map;
      // 167: goto 227
      // 16a: aload 5
      // 16c: bipush 58
      // 16e: invokevirtual java/lang/String.indexOf (I)I
      // 171: istore 8
      // 173: iload 8
      // 175: ifle 1ae
      // 178: aload 5
      // 17a: bipush 0
      // 17b: iload 8
      // 17d: invokevirtual java/lang/String.substring (II)Ljava/lang/String;
      // 180: astore 9
      // 182: aload 9
      // 184: ldc_w "title"
      // 187: invokevirtual java/lang/String.equals (Ljava/lang/Object;)Z
      // 18a: ifne 198
      // 18d: aload 9
      // 18f: ldc_w "url"
      // 192: invokevirtual java/lang/String.equals (Ljava/lang/Object;)Z
      // 195: ifeq 1ae
      // 198: aload 0
      // 199: aload 9
      // 19b: aload 5
      // 19d: iload 8
      // 19f: bipush 1
      // 1a0: iadd
      // 1a1: invokevirtual java/lang/String.substring (I)Ljava/lang/String;
      // 1a4: invokevirtual java/lang/String.trim ()Ljava/lang/String;
      // 1a7: lload 3
      // 1a8: invokevirtual io/karatelabs/ext/robot/BotDriver.waitForTitleOrUrl (Ljava/lang/String;Ljava/lang/String;J)Ljava/util/Map;
      // 1ab: goto 227
      // 1ae: aload 5
      // 1b0: ldc_w "{"
      // 1b3: invokevirtual java/lang/String.startsWith (Ljava/lang/String;)Z
      // 1b6: ifne 1eb
      // 1b9: aload 5
      // 1bb: ldc "["
      // 1bd: invokevirtual java/lang/String.startsWith (Ljava/lang/String;)Z
      // 1c0: ifne 1eb
      // 1c3: aload 5
      // 1c5: ldc "#"
      // 1c7: invokevirtual java/lang/String.startsWith (Ljava/lang/String;)Z
      // 1ca: ifne 1eb
      // 1cd: aload 5
      // 1cf: ldc "/"
      // 1d1: invokevirtual java/lang/String.startsWith (Ljava/lang/String;)Z
      // 1d4: ifne 1eb
      // 1d7: aload 5
      // 1d9: ldc "."
      // 1db: invokevirtual java/lang/String.startsWith (Ljava/lang/String;)Z
      // 1de: ifeq 1fa
      // 1e1: aload 5
      // 1e3: ldc "("
      // 1e5: invokevirtual java/lang/String.contains (Ljava/lang/CharSequence;)Z
      // 1e8: ifne 1fa
      // 1eb: aload 0
      // 1ec: getfield io/karatelabs/ext/robot/BotDriver.driver Lio/karatelabs/driver/Driver;
      // 1ef: aload 5
      // 1f1: invokeinterface io/karatelabs/driver/Driver.waitFor (Ljava/lang/String;)Lio/karatelabs/driver/Element; 2
      // 1f6: pop
      // 1f7: goto 206
      // 1fa: aload 0
      // 1fb: getfield io/karatelabs/ext/robot/BotDriver.driver Lio/karatelabs/driver/Driver;
      // 1fe: aload 5
      // 200: invokeinterface io/karatelabs/driver/Driver.waitUntil (Ljava/lang/String;)Z 2
      // 205: pop
      // 206: new java/util/LinkedHashMap
      // 209: dup
      // 20a: invokespecial java/util/LinkedHashMap.<init> ()V
      // 20d: astore 9
      // 20f: aload 9
      // 211: ldc_w "elapsed"
      // 214: invokestatic java/lang/System.currentTimeMillis ()J
      // 217: lload 3
      // 218: lsub
      // 219: invokestatic java/lang/Long.valueOf (J)Ljava/lang/Long;
      // 21c: invokeinterface java/util/Map.put (Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object; 3
      // 221: pop
      // 222: aload 9
      // 224: goto 227
      // 227: areturn
      // 228: astore 5
      // 22a: invokestatic java/lang/Thread.currentThread ()Ljava/lang/Thread;
      // 22d: invokevirtual java/lang/Thread.interrupt ()V
      // 230: aload 0
      // 231: ldc "Wait interrupted"
      // 233: ldc "INTERRUPTED"
      // 235: invokevirtual io/karatelabs/ext/robot/BotDriver.fail (Ljava/lang/String;Ljava/lang/String;)Lio/karatelabs/ext/robot/c;
      // 238: athrow
      // 239: astore 5
      // 23b: aload 0
      // 23c: ldc_w "wait"
      // 23f: aload 5
      // 241: invokevirtual io/karatelabs/driver/DriverException.getMessage ()Ljava/lang/String;
      // 244: aload 2
      // 245: invokestatic java/lang/String.valueOf (Ljava/lang/Object;)Ljava/lang/String;
      // 248: invokevirtual io/karatelabs/ext/robot/BotDriver.logAgentError (Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V
      // 24b: aload 0
      // 24c: aload 5
      // 24e: invokevirtual io/karatelabs/driver/DriverException.getMessage ()Ljava/lang/String;
      // 251: ldc "TIMEOUT"
      // 253: invokevirtual io/karatelabs/ext/robot/BotDriver.fail (Ljava/lang/String;Ljava/lang/String;)Lio/karatelabs/ext/robot/c;
      // 256: athrow
   }

   private Map<String, Object> waitForTitleOrUrl(String var1, String var2, long var3) {
      String var5 = var2.replace("\\", "\\\\").replace("'", "\\'");
      String var6 = var1.equals("title") ? "(document.title || '').indexOf('" + var5 + "') !== -1" : "window.location.href.indexOf('" + var5 + "') !== -1";

      try {
         this.driver.waitUntil(var6);
      } catch (DriverException var9) {
         String var8 = var1.equals("title") ? this.driver.getTitle() : this.driver.getUrl();
         this.logAgentError("wait", var9.getMessage(), var1 + ":" + var2);
         throw new io.karatelabs.ext.robot.RobotException(io.karatelabs.ext.robot.RobotGuidanceFormatter.a(var1, var2, var8), "TIMEOUT", "Wait on a locator from look().");
      }

      LinkedHashMap var7 = new LinkedHashMap();
      var7.put("elapsed", System.currentTimeMillis() - var3);
      var7.put(var1, var1.equals("title") ? this.driver.getTitle() : this.driver.getUrl());
      return var7;
   }

   private Object evalJs(Object... var1) {
      if (var1.length < 1) {
         throw this.fail("eval requires function argument", "INVALID_ARGS");
      } else {
         var var2 = this.checkDialogBlocking();
         if (var2 != null) {
            return var2;
         } else {
            String var3 = prepareEvalExpression(var1[0]);
            logger.debug(
               "evalJs input type: {}, value: {}",
               var1[0] == null ? "null" : var1[0].getClass().getSimpleName(),
               var3.length() > 200 ? var3.substring(0, 200) + "..." : var3
            );

            try {
               Object var4 = runEvalLadder(var3, this.driver::script);
               if (var4 instanceof Map var9 && Boolean.TRUE.equals(var9.get("__kjsPending"))) {
                  Object var6 = var9.get("key");
                  String var7 = var6 == null ? null : var6.toString();
                  return var7 == null ? var4 : this.pollAsyncEvalResult(var7);
               } else {
                  return var4;
               }
            } catch (Exception var8) {
               String var5 = var8.getMessage();
               if (var5 != null && var5.contains("SyntaxError")) {
                  logger.warn(
                     "evalJs SyntaxError - JS sent to browser ({} chars): {}", var3.length(), var3.length() > 500 ? var3.substring(0, 500) + "..." : var3
                  );
               }

               this.logAgentError("eval", var5, var3.length() > 100 ? var3.substring(0, 100) + "..." : var3);
               throw this.fail(var5, "EVAL_FAILED");
            }
         }
      }
   }

   static String prepareEvalExpression(Object var0) {
      if (var0 == null) {
         return "undefined";
      } else {
         return var0 instanceof JsFunction var1 && var1.getSource() != null ? var1.getSource() : var0.toString();
      }
   }

   static Object runEvalLadder(String var0, io.karatelabs.ext.robot.BotDriver.CheckedTask var1) throws Exception {
      try {
         return var1.run(wrapForAsyncEval(var0));
      } catch (Exception var7) {
         String var3 = var7.getMessage() != null ? var7.getMessage() : "";
         if (!var3.contains("SyntaxError") && !var3.contains("Unexpected token")) {
            throw var7;
         } else {
            try {
               return var1.run(var0);
            } catch (Exception var6) {
               String var5 = var6.getMessage() != null ? var6.getMessage() : "";
               if (var5.contains("Illegal return")) {
                  return var1.run(wrapForAsyncEval("(function(){\n" + var0 + "\n})()"));
               } else {
                  throw var6;
               }
            }
         }
      }
   }

   private static String wrapForAsyncEval(String var0) {
      return "(function(){ window.__kjsAsync = window.__kjsAsync || {}; var k = '_' + Date.now() + '_' + Math.floor(Math.random()*1e9); var slot = window.__kjsAsync[k] = {done:false}; try {   var v = ("
         + var0
         + ");   if (v && typeof v.then === 'function') {     v.then(function(x){ slot.value = x; slot.done = true; })      .catch(function(e){ slot.error = (e && e.message) ? e.message : String(e); slot.done = true; });     return {__kjsPending: true, key: k};   }   delete window.__kjsAsync[k];   return v; } catch (e) { delete window.__kjsAsync[k]; throw e; }})()";
   }

   private Object pollAsyncEvalResult(String var1) {
      long var2 = System.currentTimeMillis() + 5000L;
      String var4 = "(function(){ var s = window.__kjsAsync && window.__kjsAsync['"
         + var1
         + "']; if (!s) return {missing: true}; if (!s.done) return {pending: true}; var r = {done: true, value: s.value, error: s.error}; delete window.__kjsAsync['"
         + var1
         + "']; return r; })()";

      while (System.currentTimeMillis() < var2) {
         try {
            Thread.sleep(50L);
         } catch (InterruptedException var7) {
            Thread.currentThread().interrupt();
            throw this.fail("eval interrupted", "INTERRUPTED");
         }

         if (this.driver.script(var4) instanceof Map var6) {
            if (Boolean.TRUE.equals(var6.get("done"))) {
               if (var6.get("error") != null) {
                  throw this.fail("Promise rejected: " + var6.get("error"), "EVAL_FAILED");
               }

               return var6.get("value");
            }

            if (Boolean.TRUE.equals(var6.get("missing"))) {
               throw this.fail("eval slot lost — page navigated mid-Promise", "EVAL_FAILED");
            }
         }
      }

      throw this.fail(
         "eval Promise did not resolve within 5s. For longer operations, use the window-global + poll pattern: bot.eval(\"window.__r = null; fetch(...).then(r => r.text()).then(t => window.__r = t); 'started'\") then poll with bot.eval(\"window.__r\").",
         "TIMEOUT"
      );
   }

   private Object text(Object... var1) {
      String var2 = null;
      boolean var3 = false;
      if (var1.length > 0 && var1[0] != null) {
         if (var1[0] instanceof Map var4) {
            var3 = Boolean.TRUE.equals(var4.get("full"));
            Object var5 = var4.get("scope");
            if (var5 != null) {
               var2 = String.valueOf(var5);
            }
         } else {
            var2 = String.valueOf(var1[0]);
         }
      }

      try {
         this.injectAgentLookJs();
         String var9 = var2 != null && !var2.isBlank() ? "'" + var2.replace("'", "\\'") + "'" : "null";
         String var10 = "window.__kjs.look.renderText(" + var9 + ", {full: " + var3 + "})";
         Object var6 = this.driver.script(var10);
         if (var6 == null && var2 != null && !var2.isBlank()) {
            throw this.fail(
               "no element matched '"
                  + var2
                  + "' — bot.text() reads the element you name, it does not fall back to the page. Use a locator from look(), or bot.text() with no argument for the whole page.",
               "TEXT_NO_MATCH"
            );
         } else {
            String var7 = var6 != null ? var6.toString() : "";
            this.lastTextSize = var7.length();
            return var7;
         }
      } catch (Exception var8) {
         throw this.fail(var8.getMessage(), "TEXT_FAILED");
      }
   }

   private Object table(Object... var1) {
      String var2 = null;
      int var3 = 200;
      if (var1.length > 0 && var1[0] != null) {
         if (var1[0] instanceof Map var4) {
            Object var5 = var4.get("scope");
            if (var5 == null) {
               var5 = var4.get("selector");
            }

            if (var5 != null) {
               var2 = String.valueOf(var5);
            }

            if (var4.get("limit") instanceof Number var7) {
               var3 = var7.intValue();
            }
         } else {
            var2 = String.valueOf(var1[0]);
         }
      }

      try {
         this.waitForPageStable();
         String var9 = var2 != null && !var2.isBlank() ? "'" + var2.replace("\\", "\\\\").replace("'", "\\'") + "'" : "null";
         String var10 = "(function(){var sel=__SEL__;var t=sel?document.querySelector(sel):null;if(sel&&!t)return '__NO_MATCH__';if(!t){var c=Array.prototype.slice.call(document.querySelectorAll('table,[role=\"table\"],[role=\"grid\"]')).filter(function(e){return e.offsetParent!==null;});if(!c.length)return null;c.sort(function(a,b){return b.querySelectorAll('tr,[role=\"row\"]').length-a.querySelectorAll('tr,[role=\"row\"]').length;});t=c[0];}if(!t)return null;if(t.tagName!=='TABLE'&&t.getAttribute('role')!=='table'&&t.getAttribute('role')!=='grid'){t=t.closest('table,[role=\"table\"],[role=\"grid\"]')||t;}function cells(r){return Array.prototype.slice.call(r.querySelectorAll('th,td,[role=\"columnheader\"],[role=\"cell\"],[role=\"gridcell\"]'));}var allRows=Array.prototype.slice.call(t.querySelectorAll('tr,[role=\"row\"]'));if(!allRows.length)return null;var headerRow=t.querySelector('thead tr')||allRows[0];var headers=cells(headerRow).map(function(c){return (c.textContent||'').trim();});var body=allRows.filter(function(r){return r!==headerRow&&cells(r).length;});function num(s){var v=(s||'').replace(/\\$/g,'').replace(/\\s/g,'');if(!v)return s;var neg=/^-/.test(v);if(neg)v=v.slice(1);var d=v.indexOf('.')>=0,c=v.indexOf(',')>=0,n;if(d&&c){n=v.lastIndexOf(',')>v.lastIndexOf('.')?v.replace(/\\./g,'').replace(',','.'):v.replace(/,/g,'');}else if(c){var p=v.split(',');n=(p.length===2&&p[1].length>0&&p[1].length<=2)?p[0]+'.'+p[1]:v.replace(/,/g,'');}else{n=v;}return /^\\d+(\\.\\d+)?$/.test(n)?parseFloat(neg?'-'+n:n):s;}var max=__MAX__;var rows=[];for(var i=0;i<body.length&&rows.length<max;i++){var cs=cells(body[i]);var o={};for(var j=0;j<cs.length;j++){var k=headers[j]&&headers[j].length?headers[j]:('col'+(j+1));o[k]=num((cs[j].textContent||'').trim());}rows.push(o);}return {columns:headers,rowCount:body.length,rows:rows,truncated:body.length>rows.length,hint:'Structured rows; numeric cells parsed to numbers. Aggregate directly e.g. rows.filter(r=>r.Category===\"Travel\").length, rows.reduce((s,r)=>s+r.Amount,0). rowCount is the authoritative total — verify your row count against it before aggregating.'};})()"
            .replace("__SEL__", var9)
            .replace("__MAX__", String.valueOf(var3));
         Object var11 = this.driver.script(var10);
         if ("__NO_MATCH__".equals(var11)) {
            throw this.fail(
               "no element matched '"
                  + var2
                  + "' — bot.table() reads the table you name, it does not fall back to another one. Use a locator from look(), or bot.table() with no argument to auto-pick the biggest visible table.",
               "TABLE_NO_MATCH"
            );
         } else if (!(var11 instanceof Map)) {
            throw this.fail(
               "no table found"
                  + (var2 != null ? " for selector '" + var2 + "'" : " on the page")
                  + " — pass a CSS selector (e.g. bot.table('#expense-table')), or use bot.text() for non-tabular data.",
               "NO_TABLE"
            );
         } else {
            return var11;
         }
      } catch (Exception var8) {
         throw this.fail(var8.getMessage(), "TABLE_FAILED");
      }
   }

   public int consumeLastTextSize() {
      int var1 = this.lastTextSize;
      this.lastTextSize = 0;
      return var1;
   }

   private void logAgentError(String var1, String var2, String var3) {
      this.diagnostics.a(var1, var2, var3);
   }

   private void selectFieldContents(String var1) {
      this.driver.script(var1, "_ && _.select && _.select()");
   }

   private List<String> resolveUploadPaths(Object var1) {
      if (var1 == null) {
         return List.of();
      } else {
         List var2 = var1 instanceof List var4 ? var4 : (var1 instanceof Object[] var3 ? Arrays.asList(var3) : List.of(var1));
         ArrayList var9 = new ArrayList(var2.size());

         for (Object var5 : var2) {
            if (var5 != null) {
               String var6 = String.valueOf(var5).trim();
               if (!var6.isEmpty()) {
                  String var7 = this.uploadFileResolver != null ? this.uploadFileResolver.apply(var6) : var6;
                  Path var8 = Path.of(var7).toAbsolutePath().normalize();
                  if (!Files.exists(var8)) {
                     throw new DriverException("upload file not found: '" + var6 + "'" + (var8.toString().equals(var6) ? "" : " (resolved to " + var8 + ")"));
                  }

                  var9.add(var8.toString());
               }
            }
         }

         return var9;
      }
   }

   Map<String, Object> performAction(String var1, String var2, Object var3) {
      return this.performAction(var1, var2, var3, null);
   }

   private Map<String, Object> performAction(String var1, String var2, Object var3, Object var4) {
      return (Map<String, Object>)(switch (var2) {
         case "click" -> {
            var1 = this.reveal.a(var1, var4);
            logger.debug("click: scrolling {}", var1);

            try {
               this.driver.script(Locators.scrollJs(var1));
            } catch (Exception var24) {
               logger.debug("click: scroll failed (element may not exist): {}", var24.getMessage());
            }

            Boolean var52 = (Boolean)this.driver.script(var1, "_ && _.tagName && _.tagName.toLowerCase() === 'option'");
            if (Boolean.TRUE.equals(var52)) {
               String var59 = (String)this.driver.script(var1, "_.text");
               String var68 = (String)this.driver.script(var1, "_.value");
               this.driver.script(var1, "_.parentElement.value = _.value");
               this.driver.script(var1, "_.parentElement.dispatchEvent(new Event('change', {bubbles: true}))");
               logger.debug("click: selected option '{}' via parent select", var59);
               var var41 = this.actionResult(true, null);
               yield var41;
            } else {
               String var60 = this.disabledReason(var1);
               if (var60 != null && !var60.contains("pointer-events")) {
                  for (int var69 = 0; var69 < 15 && var60 != null && !var60.contains("pointer-events"); var69++) {
                     try {
                        Thread.sleep(100L);
                     } catch (InterruptedException var23) {
                     }

                     var60 = this.disabledReason(var1);
                  }
               }

               if (var60 != null) {
                  String var71 = var1.startsWith("{")
                     ? " It matched a disabled look-alike sharing this text — target the enabled control with an index (e.g. '"
                        + nextWildcardIndex(var1)
                        + "') or call look() to find it."
                     : " Call look() to find the enabled control.";
                  throw new DriverException("element matched but is not interactable (" + var60 + "): '" + var1 + "'." + var71);
               }

               String var70 = this.driver.getUrl();
               Boolean var76 = (Boolean)this.driver
                  .script(
                     var1,
                     "_ && !!(_.closest('[aria-haspopup=\"true\"]') || _.closest('[role=\"menuitem\"]') || _.closest('[aria-expanded]') || _.closest('[role=\"button\"][role=\"menu\"] *, [role=\"menubar\"] *'))"
                  );
               if (Boolean.TRUE.equals(var76)) {
                  logger.debug("click: auto-hover pause for menu item");

                  try {
                     Thread.sleep(200L);
                  } catch (InterruptedException var22) {
                  }
               }

               Map var80 = null;
               LinkedHashMap var84 = null;

               try {
                  if (this.iframeOffset != null) {
                     logger.debug("click: using JS click for iframe element {}", var1);
                     this.driver.click(var1);
                  } else {
                     logger.debug("click: getting mouse at {}", var1);
                     Mouse var86 = this.mouseWithOffset(var1);
                     logger.debug("click: mouse at ({}, {})", var86.getX(), var86.getY());
                     var86.click();
                  }

                  logger.debug("click: done");
                  var var87 = this.waitAfterClick(var70);
                  if (var87 != null) {
                     if (var87.containsKey("stabilityHint")) {
                        this.lastStabilityHint = (String)var87.get("stabilityHint");
                     }

                     if (var87.containsKey("dialog")) {
                        Map var90 = (Map)var87.get("dialog");
                        var80 = var90;
                     }

                     if (var87.containsKey("newTab")) {
                        var84 = new LinkedHashMap();
                        var84.put("newTab", var87.get("newTab"));
                        if (var87.containsKey("otherTabs")) {
                           var84.put("otherTabs", var87.get("otherTabs"));
                        }
                     }

                     if (var87.containsKey("tabSwitchError")) {
                        this.lastStabilityHint = "Failed to focus new tab: " + var87.get("tabSwitchError");
                     }
                  }
               } catch (Exception var26) {
                  logger.debug("click: exception - checking for dialog: {}", var26.getMessage());
                  Dialog var89 = this.driver.getDialog();
                  if (var89 == null) {
                     throw var26;
                  }

                  logger.debug("click: dialog detected after timeout: type={}, message={}", var89.getType(), var89.getMessage());
                  var80 = this.buildDialogInfo(var89);
               }

               Map var42 = this.actionResult(true, var80, var84);
               yield var42;
            }
         }
         case "input" -> {
            this.requireCanHoldText(var1, "type into");
            this.driver.focus(var1);
            String var51 = String.valueOf(var3);
            Boolean var58 = (Boolean)this.driver.script(var1, "_ && ['time','date','datetime-local','month','week'].indexOf(_.type) >= 0");
            if (Boolean.TRUE.equals(var58)) {
               this.driver.script(Locators.inputJs(var1, var51));
            } else {
               this.selectFieldContents(var1);
               this.driver.keys().press("\ue003");
               this.driver.keys().type(var51);
            }

            this.lastStabilityHint = this.waitForFormAction();

            try {
               if (this.driver.script(var1, "_.value") instanceof String var75) {
                  String var79 = var51.replaceAll("[^a-zA-Z0-9]", "").toLowerCase();
                  String var83 = var75.replaceAll("[^a-zA-Z0-9]", "").toLowerCase();
                  boolean var85 = var83.isEmpty() && !var79.isEmpty();
                  boolean var88 = !var83.isEmpty() && !var79.isEmpty() && !var83.contains(var79) && !var79.contains(var83);
                  if (var85 || var88) {
                     this.driver.click(var1);

                     try {
                        Thread.sleep(200L);
                     } catch (InterruptedException var21) {
                     }

                     this.selectFieldContents(var1);
                     this.driver.keys().press("\ue003");
                     this.driver.keys().type(var51);
                     this.lastStabilityHint = this.waitForFormAction();
                     if (this.driver.script(var1, "_.value") instanceof String var17) {
                        String var18 = var17.replaceAll("[^a-zA-Z0-9]", "").toLowerCase();
                        if (var18.isEmpty() && !var79.isEmpty()) {
                           this.lastStabilityHint = "Field value lost after retry: typed '"
                              + var51
                              + "' but field is empty. The field may have a formatter — try act(locator, 'click'), wait(500), then act(locator, 'input', value).";
                        } else if (!var18.isEmpty() && !var79.isEmpty() && !var18.contains(var79) && !var79.contains(var18)) {
                           this.lastStabilityHint = "Field value mismatch after retry: typed '" + var51 + "' but field contains '" + var17 + "'.";
                        }
                     }
                  }
               }
            } catch (Exception var25) {
            }

            var var40 = this.actionResult(true, null);
            yield var40;
         }
         case "clear" -> {
            this.requireCanHoldText(var1, "clear");
            this.driver.focus(var1);
            Boolean var50 = (Boolean)this.driver.script(var1, "_ && ['time','date','datetime-local','month','week'].indexOf(_.type) >= 0");
            if (Boolean.TRUE.equals(var50)) {
               this.driver.script(Locators.inputJs(var1, ""));
            } else {
               this.selectFieldContents(var1);
               this.driver.keys().press("\ue003");
            }

            this.lastStabilityHint = this.waitForFormAction();
            var var39 = this.actionResult(true, null);
            yield var39;
         }
         case "focus" -> {
            this.driver.focus(var1);
            var var38 = this.actionResult(false, null);
            yield var38;
         }
         case "upload" -> {
            var var49 = this.resolveUploadPaths(var3);
            if (var49.isEmpty()) {
               throw new DriverException("upload requires a file path or a list of paths");
            }

            if (!(this.driver instanceof CdpDriver var57)) {
               throw new DriverException("upload is only supported on the CDP driver");
            }

            String var66 = var57.objectId(var1);
            if (var66 == null) {
               throw new DriverException(
                  "upload could not resolve a file input for locator: '" + var1 + "'. Target the <input type=file> directly (it may be hidden — that is fine)."
               );
            }

            var57.getCdpClient().method("DOM.setFileInputFiles").param("files", var49).param("objectId", var66).send();
            this.lastStabilityHint = this.waitForFormAction();
            var var37 = this.actionResult(true, null);
            yield var37;
         }
         case "key" -> {
            String var48 = String.valueOf(var3);
            validateKeyToken(var48);
            String var56 = mapKeyName(var48);
            if (var1 != null && !var1.isEmpty() && !"body".equals(var1)) {
               this.driver.focus(var1);
            }

            String var64 = mapKeyForDom(var48);
            String var74 = mapCodeForDom(var48);
            this.driver
               .script(
                  "(function(){ var el = document.activeElement; var t = (el && el !== document.body) ? el : document; ['keydown','keyup'].forEach(function(et){ t.dispatchEvent(new KeyboardEvent(et, {key:'"
                     + var64
                     + "', code:'"
                     + var74
                     + "', bubbles:true, cancelable:true})); }); })()"
               );
            this.driver.keys().press(var56);
            var var36 = this.actionResult(true, null);
            yield var36;
         }
         case "scroll" -> {
            String var47 = var3 != null ? String.valueOf(var3).toLowerCase() : "down";
            boolean var55 = "left".equals(var47) || "right".equals(var47);
            if (var1 != null && !var1.isEmpty() && !"body".equals(var1)) {
               if (var55) {
                  int var62 = "left".equals(var47) ? -1 : 1;
                  String var72 = "_ => { var target = _; while (target && target !== document.body && target !== document.documentElement) { var style = getComputedStyle(target); if ((style.overflowX === 'auto' || style.overflowX === 'scroll') && target.scrollWidth > target.clientWidth + 10) { var amount = Math.round(target.clientWidth * 0.8) * %d; var before = target.scrollLeft; target.scrollBy(amount, 0); return { scrolled: target.scrollLeft !== before, scrollLeft: Math.round(target.scrollLeft), scrollWidth: target.scrollWidth, clientWidth: target.clientWidth, atLeft: target.scrollLeft <= 0, atRight: target.scrollLeft + target.clientWidth >= target.scrollWidth - 5 }; } target = target.parentElement; } return null; }"
                     .formatted(var62);
                  if (this.driver.script(var1, var72) instanceof Map var81) {
                     yield var81;
                  } else {
                     this.driver.script(var1, "_.scrollIntoView({behavior: 'instant', inline: 'center'})");
                     var var34 = this.actionResult(true, null);
                     yield var34;
                  }
               } else {
                  int var63 = "up".equals(var47) ? -1 : 1;
                  String var73 = "_ => { var target = _; while (target && target !== document.body && target !== document.documentElement) { var style = getComputedStyle(target); if ((style.overflowY === 'auto' || style.overflowY === 'scroll') && target.scrollHeight > target.clientHeight + 10) { var amount = Math.round(target.clientHeight * 0.8) * %d; var before = target.scrollTop; target.scrollBy(0, amount); return { scrolled: target.scrollTop !== before, scrollTop: Math.round(target.scrollTop), scrollHeight: target.scrollHeight, clientHeight: target.clientHeight, atTop: target.scrollTop <= 0, atBottom: target.scrollTop + target.clientHeight >= target.scrollHeight - 5 }; } target = target.parentElement; } return null; }"
                     .formatted(var63);
                  if (this.driver.script(var1, var73) instanceof Map var82) {
                     yield var82;
                  } else {
                     this.driver.script(var1, "_.scrollIntoView({behavior: 'instant', block: 'center'})");
                     var var35 = this.actionResult(true, null);
                     yield var35;
                  }
               }
            } else {
               String var61;
               if (var55) {
                  var61 = "left".equals(var47)
                     ? "(() => { var b = window.scrollX; window.scrollBy(-Math.round(window.innerWidth * 0.8), 0); return { scrolled: window.scrollX !== b, atLeft: window.scrollX <= 0, atRight: window.scrollX + window.innerWidth >= document.documentElement.scrollWidth - 5 }; })()"
                     : "(() => { var b = window.scrollX; window.scrollBy(Math.round(window.innerWidth * 0.8), 0); return { scrolled: window.scrollX !== b, atLeft: window.scrollX <= 0, atRight: window.scrollX + window.innerWidth >= document.documentElement.scrollWidth - 5 }; })()";
               } else {
                  var61 = "up".equals(var47)
                     ? "(() => { var b = window.scrollY; window.scrollBy(0, -Math.round(window.innerHeight * 0.8)); return { scrolled: window.scrollY !== b, scrollY: Math.round(window.scrollY), viewportHeight: window.innerHeight, scrollHeight: document.documentElement.scrollHeight, atTop: window.scrollY <= 0, atBottom: window.scrollY + window.innerHeight >= document.documentElement.scrollHeight - 5 }; })()"
                     : "(() => { var b = window.scrollY; window.scrollBy(0, Math.round(window.innerHeight * 0.8)); return { scrolled: window.scrollY !== b, scrollY: Math.round(window.scrollY), viewportHeight: window.innerHeight, scrollHeight: document.documentElement.scrollHeight, atTop: window.scrollY <= 0, atBottom: window.scrollY + window.innerHeight >= document.documentElement.scrollHeight - 5 }; })()";
               }

               if (this.driver.script(var61) instanceof Map var12) {
                  LinkedHashMap var13 = new LinkedHashMap(var12);
                  if (Boolean.FALSE.equals(var13.get("scrolled"))
                     || var13.get("scrollHeight") instanceof Number var14
                        && var13.get("viewportHeight") instanceof Number var15
                        && var14.intValue() <= var15.intValue() + 5) {
                     var13.put(
                        "hint",
                        "Page body has no scrollable content. Content is likely in inner panels. Call look() to see scrollContainers, then use act(locator, 'scroll', 'down')."
                     );
                  }

                  yield var13;
               } else {
                  var var33 = this.actionResult(true, null);
                  yield var33;
               }
            }
         }
         case "rightClick" -> {
            this.driver.script(Locators.scrollJs(var1));
            if (this.iframeOffset != null) {
               this.driver.script(var1, "_.dispatchEvent(new MouseEvent('contextmenu', {bubbles: true, cancelable: true}))");
            } else {
               this.mouseWithOffset(var1).rightClick();
            }

            var var32 = this.actionResult(true, null);
            yield var32;
         }
         case "doubleClick" -> {
            this.driver.script(Locators.scrollJs(var1));
            if (this.iframeOffset != null) {
               this.driver.click(var1);
               this.driver.click(var1);
               this.driver.script(var1, "_.dispatchEvent(new MouseEvent('dblclick', {bubbles: true, cancelable: true}))");
            } else {
               Mouse var46 = this.mouseWithOffset(var1);
               var46.doubleClick();
            }

            var var31 = this.actionResult(true, null);
            yield var31;
         }
         case "hover" -> {
            this.driver.script(Locators.scrollJs(var1));
            if (this.iframeOffset != null) {
               this.driver.script(var1, "_.dispatchEvent(new PointerEvent('pointerenter', {bubbles: false, cancelable: false}))");
               this.driver.script(var1, "_.dispatchEvent(new MouseEvent('mouseenter', {bubbles: false, cancelable: false}))");
               this.driver.script(var1, "_.dispatchEvent(new PointerEvent('pointerover', {bubbles: true, cancelable: true}))");
               this.driver.script(var1, "_.dispatchEvent(new MouseEvent('mouseover', {bubbles: true, cancelable: true}))");
            } else {
               Mouse var45 = this.mouseWithOffset(var1);
               var45.move(var45.getX() - 1.0, var45.getY() - 1.0);
               var45.move(var45.getX(), var45.getY());
            }

            try {
               Thread.sleep(300L);
            } catch (InterruptedException var20) {
            }

            var var30 = this.actionResult(true, null);
            yield var30;
         }
         case "select" -> {
            String var44 = String.valueOf(var3);
            String var54 = this.findMatchingOption(var1, var44);
            if (var54 == null) {
               throw new DriverException("Option not found: '" + var44 + "'. Check the 'options' field in look() response.");
            }

            this.driver.select(var1, var54);
            this.lastStabilityHint = this.waitForFormAction();
            String var10 = this.selectedOptionText(var1);
            if (var10 != null && !optionSatisfies(var10, var44, var54)) {
               try {
                  Thread.sleep(400L);
               } catch (InterruptedException var19) {
               }

               var10 = this.selectedOptionText(var1);
            }

            if (var10 != null && !optionSatisfies(var10, var44, var54)) {
               if (!isPlaceholderOption(var10)) {
                  throw new DriverException(
                     "Select did not stick on '"
                        + var1
                        + "': requested '"
                        + var44
                        + "' but the control now shows '"
                        + var10
                        + "' — the app likely rejected it as invalid in context (e.g. out of allowed range)."
                  );
               }

               this.lastStabilityHint = "select '" + var44 + "' did not take on " + var1 + " — control shows '" + var10 + "'";
               logger.warn("{}", this.lastStabilityHint);
            }

            var var29 = this.actionResult(true, null);
            yield var29;
         }
         case "check" -> {
            Element var43 = this.driver.locate(var1);
            Boolean var53 = (Boolean)this.driver.script(var1, "_.checked");
            if (!Boolean.TRUE.equals(var53)) {
               var43.click();
               this.lastStabilityHint = this.waitForFormAction();
            }

            var var28 = this.actionResult(true, null);
            yield var28;
         }
         case "uncheck" -> {
            Element var8 = this.driver.locate(var1);
            Boolean var9 = (Boolean)this.driver.script(var1, "_.checked");
            if (Boolean.TRUE.equals(var9)) {
               var8.click();
               this.lastStabilityHint = this.waitForFormAction();
            }

            var var7 = this.actionResult(true, null);
            yield var7;
         }
         default -> throw new DriverException(
            "action '" + var2 + "' is listed as known but not implemented — KNOWN_OPS_ORDERED and performAction have drifted apart"
         );
      });
   }

   private void requireCanHoldText(String var1, String var2) {
      String var3 = (String)this.driver
         .script(
            var1,
            "_ => { if (!_ || _.nodeType !== 1) return 'nothing'; if ('value' in _ || _.isContentEditable) return null; if (_.shadowRoot) return null; if (_.tabIndex != null && _.tabIndex >= 0) return null; var tag = '<' + _.tagName.toLowerCase() + '>'; var inner = _.querySelector && _.querySelector('input, textarea, select'); return 'it resolved to a ' + tag + ', which can neither hold text nor take focus'   + (inner ? \" — the field is the <\" + inner.tagName.toLowerCase() + '> inside it' : ''); }"
         );
      if (var3 != null) {
         throw new DriverException(
            "cannot "
               + var2
               + " '"
               + var1
               + "' — "
               + var3
               + ", so the keystrokes would have gone to whatever had focus. Qualify the selector so it names the field itself (e.g. input[...] rather than [...]), or use the locator look() returned."
         );
      }
   }

   private String disabledReason(String var1) {
      return (String)this.driver
         .script(
            var1,
            "_ => { if (!_ || _.nodeType !== 1) return null; if (_.disabled === true) return 'disabled'; try { if (getComputedStyle(_).pointerEvents === 'none') return 'pointer-events:none'; } catch (e) {} var d = _.closest && _.closest('[disabled],[aria-disabled=\"true\"]'); if (d) return (d === _ ? '' : 'ancestor ') + (d.hasAttribute('disabled') ? 'disabled' : 'aria-disabled'); return null; }"
         );
   }

   private static String nextWildcardIndex(String var0) {
      Matcher var1 = Pattern.compile("^\\{([^}:]+)(?::(\\d+))?\\}(.*)$").matcher(var0);
      if (!var1.matches()) {
         return var0;
      } else {
         int var2 = var1.group(2) == null ? 2 : Integer.parseInt(var1.group(2)) + 1;
         return "{" + var1.group(1) + ":" + var2 + "}" + var1.group(3);
      }
   }

   private Map<String, Object> actionResult(boolean var1, Map<String, Object> var2) {
      return this.actionResult(var1, var2, null);
   }

   private Map<String, Object> actionResult(boolean var1, Map<String, Object> var2, Map<String, Object> var3) {
      LinkedHashMap var4 = new LinkedHashMap();
      var4.put("changed", var1);
      if (var2 != null) {
         var4.put("dialog", var2);
      }

      if (var3 != null) {
         var4.putAll(var3);
      }

      return var4;
   }

   static String[] parseFrameLocator(String var0) {
      int var1 = var0.indexOf(" >> ");
      return var1 < 0 ? new String[]{null, var0} : new String[]{var0.substring(0, var1), var0.substring(var1 + 4)};
   }

   private Mouse mouseWithOffset(String var1) {
      Mouse var2 = this.driver.mouse(var1);
      return this.iframeOffset != null ? this.driver.mouse(var2.getX() + this.iframeOffset[0], var2.getY() + this.iframeOffset[1]) : var2;
   }

   void injectAgentLookJs() {
      if (this.driver instanceof CdpDriver var1) {
         var1.addInitScript("agent-look", AGENT_LOOK_JS);
         var1.addInitScript("agent-model", AGENT_MODEL_JS, "agent-look");
         var1.addInitScript("agent-model-packs", packsInitScript(), "agent-model");
      }

      try {
         Boolean var4 = (Boolean)this.driver.script("typeof window.__kjs !== 'undefined' && typeof window.__kjs.look !== 'undefined'");
         if (!Boolean.TRUE.equals(var4)) {
            this.driver.script(AGENT_LOOK_JS);
         }

         Boolean var5 = (Boolean)this.driver.script("typeof window.__kjs.model !== 'undefined'");
         if (!Boolean.TRUE.equals(var5)) {
            this.driver.script(AGENT_MODEL_JS);
         }

         this.driver.script(packsInitScript());
         this.pushSessionPack();
      } catch (Exception var3) {
      }
   }

   private static String packsInitScript() {
      return "window.__kjs && window.__kjs.model && window.__kjs.model.usePacks(" + io.karatelabs.ext.robot.RobotPatternPackLoader.a() + ")";
   }

   private static boolean optionSatisfies(String var0, String var1, String var2) {
      if (var0 == null) {
         return true;
      } else {
         String var3 = var0.toLowerCase().trim();
         String var4 = var1 == null ? "" : var1.toLowerCase().trim();
         return var4.isEmpty() || !var3.equals(var4) && !var3.startsWith(var4) && !var3.contains(var4)
            ? var2 != null && var0.trim().equalsIgnoreCase(var2.trim())
            : true;
      }
   }

   private static boolean isPlaceholderOption(String var0) {
      if (var0 == null) {
         return true;
      } else {
         String var1 = var0.toLowerCase().trim();
         return var1.isEmpty() || var1.equals("<none>") || var1.equals("--") || var1.startsWith("select") || var1.startsWith("choose");
      }
   }

   private String selectedOptionText(String var1) {
      try {
         Object var2 = this.driver.script(var1, "_.selectedIndex >= 0 ? _.options[_.selectedIndex].text : null");
         return var2 == null ? null : String.valueOf(var2).trim();
      } catch (Exception var3) {
         return null;
      }
   }

   private String findMatchingOption(String var1, String var2) {
      try {
         List var3 = (List)this.driver.script(var1, "_ => Array.from(_.options).map(o => ({text: o.text, value: o.value}))");
         if (var3 != null && !var3.isEmpty()) {
            String var4 = var2.toLowerCase().trim();
            String var5 = null;
            String var6 = null;
            String var7 = null;

            for (Map var9 : (Iterable<Map>) (Iterable<?>) (var3)) {
               String var10 = String.valueOf(var9.get("text")).trim();
               String var11 = var10.toLowerCase();
               if (var11.equals(var4)) {
                  var5 = var10;
                  break;
               }

               if (var6 == null && var11.startsWith(var4)) {
                  var6 = var10;
               }

               if (var7 == null && var11.contains(var4)) {
                  var7 = var10;
               }
            }

            if (var5 != null) {
               return var5;
            } else {
               return var6 != null ? var6 : var7;
            }
         } else {
            return null;
         }
      } catch (Exception var12) {
         logger.debug("findMatchingOption failed: {}", var12.getMessage());
         return var2;
      }
   }

   private Map<String, Object> waitAfterClick(String var1) {
      try {
         Thread.sleep(100L);
         Dialog var2 = this.driver.getDialog();
         if (var2 != null) {
            logger.debug("Dialog detected after click: type={}, message={}", var2.getType(), var2.getMessage());
            return this.wrapDialog(this.buildDialogInfo(var2));
         }

         String var3 = this.driver.getUrl();
         if (!var1.equals(var3)) {
            logger.debug("Navigation after click: {} -> {}", var1, var3);
            this.softWaitForReadyState("document.readyState === 'complete'");
         }

         String var4 = this.waitForStable();
         var2 = this.driver.getDialog();
         if (var2 != null) {
            return this.wrapDialog(this.buildDialogInfo(var2));
         }

         var var5 = this.driver.drainOpenedTargets();
         Map var6 = null;
         if (!var5.isEmpty()) {
            var6 = this.handleOpenedTabs(var5);
         }

         if (var4 != null || var6 != null) {
            LinkedHashMap var7 = new LinkedHashMap();
            if (var4 != null) {
               var7.put("stabilityHint", var4);
            }

            if (var6 != null) {
               var7.putAll(var6);
            }

            return var7;
         }
      } catch (InterruptedException var8) {
         Thread.currentThread().interrupt();
      } catch (Exception var9) {
         logger.debug("waitAfterClick: {}", var9.getMessage());
      }

      return null;
   }

   private Map<String, Object> wrapDialog(Map<String, Object> var1) {
      LinkedHashMap var2 = new LinkedHashMap();
      var2.put("dialog", var1);
      return var2;
   }

   private Map<String, Object> handleOpenedTabs(List<Map<String, Object>> var1) {
      LinkedHashMap var2 = new LinkedHashMap();
      Map var3 = (Map)var1.get(var1.size() - 1);
      String var4 = (String)var3.get("targetId");

      try {
         this.driver.switchPageById(var4);
         this.softWaitForReadyState("document.readyState !== 'loading'");
      } catch (Exception var9) {
         logger.warn("tab switch failed for targetId={}: {}", var4, var9.getMessage());
         var2.put("tabSwitchError", var9.getMessage());
         return var2;
      }

      this.lastLookResult = null;
      this.injectAgentLookJs();
      LinkedHashMap var5 = new LinkedHashMap();
      var5.put("url", this.driver.getUrl());
      var5.put("title", this.driver.getTitle());
      var2.put("newTab", var5);
      if (var1.size() > 1) {
         ArrayList var6 = new ArrayList();

         for (int var7 = 0; var7 < var1.size() - 1; var7++) {
            LinkedHashMap var8 = new LinkedHashMap();
            var8.put("url", ((Map)var1.get(var7)).get("url"));
            var8.put("title", ((Map)var1.get(var7)).get("title"));
            var6.add(var8);
         }

         var2.put("otherTabs", var6);
      }

      return var2;
   }

   private Map<String, Object> buildDialogInfo(Dialog var1) {
      LinkedHashMap var2 = new LinkedHashMap();
      var2.put("type", var1.getType());
      var2.put("message", var1.getMessage());
      if (var1.getDefaultPrompt() != null) {
         var2.put("defaultPrompt", var1.getDefaultPrompt());
      }

      return var2;
   }

   private Map<String, Object> checkDialogBlocking() {
      Dialog var1 = this.driver.getDialog();
      if (var1 != null) {
         throw new RuntimeException(
            "Dialog is blocking: " + var1.getMessage() + " | hint: Dismiss dialog first: bot.page('dialog', 'accept') or bot.page('dialog', 'dismiss')."
         );
      } else {
         return null;
      }
   }

   private void waitForPageStable() {
      try {
         String var1 = this.driver.getUrl();
         Thread.sleep(50L);
         String var2 = this.driver.getUrl();
         if (!var1.equals(var2)) {
            logger.debug("Navigation detected: {} -> {}", var1, var2);
            this.softWaitForReadyState("document.readyState === 'complete'");
         } else {
            Boolean var3 = (Boolean)this.driver.script("document.readyState !== 'complete'");
            if (Boolean.TRUE.equals(var3)) {
               this.softWaitForReadyState("document.readyState === 'complete'");
            }
         }

         this.waitForDomStable();
      } catch (Exception var4) {
         logger.debug("waitForPageStable: {}", var4.getMessage());
      }
   }

   private String waitForFormAction() {
      return this.waitForStable();
   }

   private void softWaitForReadyState(String var1) {
      try {
         this.driver.waitUntil(var1, READY_STATE_SOFT_TIMEOUT);
      } catch (Exception var3) {
         logger.debug("readyState wait soft-fail ({}): {}", var1, var3.getMessage());
      }
   }

   private void waitForDomStable() {
      try {
         String var1 = "document.querySelectorAll('button, a, input, select, [role=\"button\"]').length";
         byte var2 = 5;
         int var3 = 0;
         Number var4 = null;

         for (int var5 = 0; var5 < var2; var5++) {
            Number var6 = (Number)this.driver.script(var1);
            if (var6 != null && var4 != null && var6.longValue() == var4.longValue()) {
               if (++var3 >= 2) {
                  logger.debug("DOM stable after {} checks, count={}", var5 + 1, var6);
                  return;
               }
            } else {
               var3 = 0;
            }

            var4 = var6;
            Thread.sleep(50L);
         }

         logger.debug("DOM stability timeout, last count={}", var4);
      } catch (Exception var7) {
         logger.debug("waitForDomStable: {}", var7.getMessage());
      }
   }

   private boolean waitForNetworkIdle(long var1) {
      long var3 = System.currentTimeMillis() + var1;
      long var5 = -1L;
      long var7 = 200L;

      try {
         for (; System.currentTimeMillis() < var3; Thread.sleep(50L)) {
            if (this.diagnostics.a()) {
               if (var5 < 0L) {
                  var5 = System.currentTimeMillis();
               } else if (System.currentTimeMillis() - var5 >= 200L) {
                  logger.debug("Network idle after {}ms silence", System.currentTimeMillis() - var5);
                  return true;
               }
            } else {
               var5 = -1L;
            }
         }
      } catch (InterruptedException var10) {
         Thread.currentThread().interrupt();
      }

      logger.debug("Network idle timeout, {} request(s) still pending", this.diagnostics.b());
      return false;
   }

   String waitForStable() {
      try {
         boolean var1;
         if (this.diagnostics.a()) {
            Thread.sleep(100L);
            if (this.diagnostics.a()) {
               var1 = true;
            } else {
               logger.debug("Network requests detected after action, waiting for idle");
               var1 = this.waitForNetworkIdle(this.driver.getOptions().getTimeout());
            }
         } else {
            logger.debug("Waiting for {} pending network request(s)", this.diagnostics.b());
            var1 = this.waitForNetworkIdle(this.driver.getOptions().getTimeout());
         }

         this.waitForDomStable();
         if (!var1) {
            return io.karatelabs.ext.robot.RobotGuidanceFormatter.a(this.diagnostics.b(), this.diagnostics.c());
         }
      } catch (InterruptedException var2) {
         Thread.currentThread().interrupt();
      }

      return null;
   }

   private boolean scopeResolves(String var1) {
      try {
         String var2 = Json.of(Map.of("s", var1)).toString();
         return Boolean.TRUE
            .equals(this.driver.script("(function(){ try { return document.querySelector(" + var2 + ".s) != null; } catch (e) { return false; } })()"));
      } catch (Exception var3) {
         return false;
      }
   }

   private String buildOptionsJson(Map<String, Object> var1) {
      return var1 != null && !var1.isEmpty() ? Json.of(var1).toString() : "{}";
   }

   private Map<String, Object> argsToMap(Object[] var1) {
      if (var1 != null && var1.length != 0) {
         Object var2 = var1[0];
         return var2 instanceof Map ? (Map)var2 : Map.of();
      } else {
         return Map.of();
      }
   }

   private io.karatelabs.ext.robot.RobotException fail(String var1, String var2) {
      return new io.karatelabs.ext.robot.RobotException(var1, var2, io.karatelabs.ext.robot.RobotGuidanceFormatter.c(var2));
   }

   private String detectOcclusion(String var1) {
      if (var1 != null && !var1.isEmpty()) {
         try {
            Object var2 = this.driver.script("window.__kjs.model.occlusionOf(" + jsString(var1) + ")");
            if (var2 == null) {
               return null;
            } else {
               String var3 = var2.toString();
               return !var3.isEmpty() && !"null".equals(var3) ? io.karatelabs.ext.robot.RobotGuidanceFormatter.b(var3) : null;
            }
         } catch (Exception var4) {
            return null;
         }
      } else {
         return null;
      }
   }

   static String jsString(String var0) {
      if (var0 == null) {
         return "null";
      } else {
         StringBuilder var1 = new StringBuilder("'");

         for (int var2 = 0; var2 < var0.length(); var2++) {
            char var3 = var0.charAt(var2);
            switch (var3) {
               case '\n':
                  var1.append("\\n");
                  break;
               case '\r':
                  var1.append("\\r");
                  break;
               case '\'':
                  var1.append("\\'");
                  break;
               case '\\':
                  var1.append("\\\\");
                  break;
               default:
                  var1.append(var3);
            }
         }

         return var1.append("'").toString();
      }
   }

   private static String loadAgentLookJs() {
      return io.karatelabs.ext.robot.RobotResourceLoader.a("js/agent-look.js");
   }

   public Driver getDriver() {
      return this.driver;
   }

   public interface HealingListener {
      void onHeal(String var1, String var2, String var3, Double var4);
   }

   static enum HealingMode {
      AUTO,
      STRICT,
      OFF;

      static io.karatelabs.ext.robot.BotDriver.HealingMode parse(Object var0, io.karatelabs.ext.robot.BotDriver.HealingMode var1) {
         if (var0 != null && !String.valueOf(var0).isBlank()) {
            String var2 = String.valueOf(var0).trim().toLowerCase();

            return switch (var2) {
               case "strict" -> STRICT;
               case "off", "false", "none" -> OFF;
               case "auto", "on", "true" -> AUTO;
               default -> var1;
            };
         } else {
            return var1;
         }
      }
   }

   public interface RecordingEventListener {
      void onRecord(String var1, String var2, String var3);
   }

   @FunctionalInterface
   interface CheckedTask {
      Object run(String var1) throws Exception;
   }
}
