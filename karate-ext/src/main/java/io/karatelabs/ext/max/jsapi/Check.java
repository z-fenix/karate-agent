package io.karatelabs.ext.max.jsapi;

import io.karatelabs.ext.common.MaxPaths;
import io.karatelabs.ext.common.Sha256;
import io.karatelabs.ext.common.jsapi.JsApi;
import io.karatelabs.ext.common.jsapi.JsDispatch;
import io.karatelabs.ext.coverage.collect.EvidenceRunMetadata;
import io.karatelabs.ext.robot.LazyBot;
import io.karatelabs.js.Engine;
import io.karatelabs.js.Terms;
import io.karatelabs.common.Json;
import io.karatelabs.ext.robot.StepDiagnosticsScope;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Check extends JsDispatch {
   private static final Logger logger = LoggerFactory.getLogger(Check.class);
   private static final Pattern ANNOTATION = Pattern.compile("^//\\s*@(\\w+):\\s*(.+)$");
   private static final Pattern AI_MARKER = Pattern.compile("^//\\s*@ai\\b.*$");
   private static final Pattern AI_BY = Pattern.compile("^(written|repaired|changed)\\b", 2);
   static final int TITLE_MAX = 80;
   static final int DESCRIPTION_MAX = 200;
   private static final Map<Path, ReentrantLock> HEADER_LOCKS = new ConcurrentHashMap<>();
   private final Path checksDir;
   private final Engine engine;
   private final Deque<String> callStack = new ArrayDeque<>();
   private Consumer<String> onRun;
   private Check.Settle onSettle;
   private Check.Verdict onVerdict;
   private boolean topArgless;
   private int asserted;
   private File files;
   private Function<String, Map<String, Object>> busy = var0 -> null;
   static final String RETURN_NOT_SERIALIZABLE = "the check's return value could not be serialized (e.g. a cyclic object)";
   private volatile ConsoleState console = new ConsoleState(null);
   private volatile Function<String, String> overlay;
   private static final int CHECKS_SCAN_DEPTH = Integer.MAX_VALUE;
   private static final Pattern PARSE_ERROR = Pattern.compile("\n(\\d+):\\d+ [^\n]*\nparser state: ");
   private static final Pattern NESTED_FRAME = Pattern.compile("^in (\\S+): ");

   public static String definitionId(String var0) {
      return io.karatelabs.ext.coverage.collect.EvidenceRunMetadata.a(var0);
   }

   public Check(Path var1, Engine var2) {
      this.checksDir = var1;
      this.engine = var2;
      this.fn("run", var1x -> this.run(JsApi.str(var1x, 0), JsApi.arg(var1x, 1), JsApi.arg(var1x, 2)));
      this.fn("list", var1x -> this.list(JsApi.str(var1x, 0)));
      this.fn("info", var1x -> this.info(JsApi.str(var1x, 0)));
      this.fn("annotate", var1x -> this.annotate(JsApi.str(var1x, 0), JsApi.arg(var1x, 1), JsApi.arg(var1x, 2)));
      this.fn("link", var1x -> this.editReq("Check.link", JsApi.str(var1x, 0), JsApi.arg(var1x, 1), true));
      this.fn("unlink", var1x -> this.editReq("Check.unlink", JsApi.str(var1x, 0), JsApi.arg(var1x, 1), false));
      this.fn("help", var0 -> JsApi.helpTopic(help(), var0));
   }

   public static Check at(Path var0, Engine var1) {
      return new Check(var0, var1);
   }

   public Check onRun(Consumer<String> var1) {
      this.onRun = var1;
      return this;
   }

   public Check onSettle(Check.Settle var1) {
      this.onSettle = var1;
      return this;
   }

   public Check onVerdict(Check.Verdict var1) {
      this.onVerdict = var1;
      return this;
   }

   public Check withConsole(ConsoleState var1) {
      this.console = var1;
      return this;
   }

   public Check withOverlay(Function<String, String> var1) {
      this.overlay = var1;
      return this;
   }

   private String stored(String var1) {
      if (this.overlay != null) {
         return this.overlay.apply(var1);
      } else {
         io.karatelabs.ext.common.secrets.SecretStore var2 = this.console.store();
         return var2 == null ? null : var2.e().get(var1);
      }
   }

   void asserted() {
      this.asserted++;
   }

   public Check withFile(File var1) {
      this.files = var1;
      return this;
   }

   public Check withBusy(Function<String, Map<String, Object>> var1) {
      this.busy = var1;
      return this;
   }

   public Object run(String var1, Object var2) {
      return this.run(var1, var2, null);
   }

    public Object run(String string, Object object, Object object2) {
        LinkedHashMap<String, Object> linkedHashMap;
        Object object3;
        Object object4;
        Path path;
        if (string == null || string.isBlank()) {
            return JsApi.missing((String)"Check.run(name, arg?, opts?) requires a check name, e.g. Check.run('login.js', {user:'admin'})");
        }
        ProjectBoot.ensure((Engine)this.engine, (Path)this.checksDir.getParent());
        ProjectConfig.ensure((Engine)this.engine, (Path)this.checksDir.getParent(), (ConsoleState)this.console);
        if (this.engine != null && !this.engine.getRootBindings().containsKey("secret")) {
            this.engine.putRootBinding("secret", (io.karatelabs.js.JavaInvokable) (args -> io.karatelabs.ext.common.secrets.SecretStore.a(JsApi.str(args, 0), this::stored)));
        }
        if ((path = this.resolveCheck(string)) == null) {
            String string2 = "no check '" + string + "' \u2014 use Check.list() to see available checks";
            if (!this.callStack.isEmpty()) {
                throw new RuntimeException(string2);
            }
            return JsApi.notFound((String)string2);
        }
        boolean bl = this.callStack.isEmpty();
        long l = System.currentTimeMillis();
        if (bl) {
            this.asserted = 0;
            boolean bl2 = this.topArgless = object == null || object == Terms.UNDEFINED || object instanceof Map && ((Map)object).isEmpty();
        }
        if (bl && this.onVerdict != null) {
            this.onVerdict.starting(string, this.topArgless);
        }
        try {
            object4 = Files.readString(path);
        }
        catch (Exception exception) {
            if (bl && this.onVerdict != null) {
                this.onVerdict.ran(string, this.topArgless, false, this.asserted);
            }
            return JsApi.notFound((String)("Check.run('" + string + "') could not read the check: " + exception.getMessage()));
        }
        if (object2 instanceof Map && Boolean.TRUE.equals(((Map)object2).get("reset")) && !this.botUnopened()) {
            try {
                this.engine.eval("(typeof bot !== 'undefined' && bot && bot.reset) ? bot.reset() : null");
            }
            catch (Exception exception) {
                logger.debug("Check.run reset skipped: {}", (Object)exception.getMessage());
            }
        }
        logger.debug("Check.run: {} ({} chars)", (Object)string, (Object)((String)object4).length());
        if (bl && this.onRun != null) {
            try {
                this.onRun.accept(string);
            }
            catch (Exception exception) {
                logger.debug("Check.onRun hook failed for {}: {}", (Object)string, (Object)exception.getMessage());
            }
        }
        this.callStack.push(string);
        object3 = object == null || object == Terms.UNDEFINED ? new LinkedHashMap() : object;
        Object v = this.engine.getRootBindings().get("__arg");
        this.engine.putRootBinding("__arg", object3);
        boolean bl3 = false;
        boolean bl4 = false;
        List list = List.of();
        try {
            Object object5;
            StepDiagnosticsScope.a();
            try {
                object5 = this.engine.eval((String)object4);
            }
            finally {
                list = StepDiagnosticsScope.b();
            }
            LinkedHashMap<String, Object> linkedHashMap2 = new LinkedHashMap<String, Object>();
            linkedHashMap2.put("ok", true);
            linkedHashMap2.put("check", string);
            if (object5 != null) {
                try {
                    linkedHashMap2.put("value", JsApi.toWire((Object)object5));
                }
                catch (Throwable throwable) {
                    throw new IllegalStateException("the check's return value could not be serialized (e.g. a cyclic object)");
                }
            }
            bl3 = true;
            if (!list.isEmpty()) {
                linkedHashMap2.put("healed", list);
            }
            Map<String, Object> map = this.settle(bl, string, true, l, null);
            bl4 = true;
            if (map != null) {
                linkedHashMap2.put("verified", map);
            }
            linkedHashMap = linkedHashMap2;
            this.callStack.pop();
        }
        catch (Throwable throwable) {
            try {
                Object object6;
                String string3 = JsApi.jsErrorMessage((Throwable)throwable);
                logger.warn("Check.run failed: {} \u2014 {}", (Object)string, (Object)string3);
                if (!bl) {
                    throw new RuntimeException("in " + string + ": " + string3 + Check.healedNote(list));
                }
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Check '").append(string).append("' failed: ").append(string3);
                String string4 = this.dismissBlockingDialog();
                if (string4 != null) {
                    stringBuilder.append("\n").append(string4);
                }
                stringBuilder.append(Check.healedNote(list));
                if (string3 != null && string3.contains("bot is not defined")) {
                    if (this.robotUnbound()) {
                        stringBuilder.append("\nThis project's karate-boot.js does not boot the robot ext, so `Robot` and `bot` are not bound in this run \u2014 add `boot.ext('robot')` to karate-boot.js.");
                    } else {
                        stringBuilder.append("\nThis check uses `bot` but no browser is bound \u2014 create one first: `var bot = Robot.create()` (or run it via a flow that provisions a bot), then re-run the check.");
                    }
                } else if (string3 != null && string3.contains("karate is not defined")) {
                    stringBuilder.append("\nA `.js` check has no `karate` global \u2014 it is plain JS, not a feature run. Use `__arg` for inputs (`Check.run('" + string + "', {..})`), the global `read(path)` for files, `File.call(path, arg?)` for `karate.call`/`callSingle` of a project .js file, and `match(...)` for assertions. Environment config (`karate.env`, base URLs, creds via `karate-config.js`) applies only when the check runs inside a `.feature` \u2014 for a standalone check, take those as `__arg` defaults instead.");
                } else if (string3 != null && string3.contains("the check's return value could not be serialized (e.g. a cyclic object)")) {
                    stringBuilder.append("\nEnd the check with a plain value (or a statement) instead.");
                } else if (Check.unparsedLine(string3) > 0) {
                    object6 = Check.innermostCheck(string3, string);
                    boolean bl5 = this.onVerdict != null && this.onVerdict.inPlace();
                    String string5 = bl5 ? Check.canonicalName((String)object6) : Check.clonePath((String)object6);
                    stringBuilder.append("\nline ").append(Check.unparsedLine(string3)).append(" doesn't parse, so nothing ran: fix that line of Check.info('").append((String)object6).append("').source, File.write('").append(string5).append("', src), then Check.run('").append(bl5 ? string : string5).append("'). Write string literals with double quotes, e.g. \"standard_user\".");
                } else {
                    object6 = Check.innermostCheck(string3, string);
                    boolean bl6 = this.onVerdict != null && this.onVerdict.inPlace();
                    String string6 = bl6 ? Check.canonicalName((String)object6) : Check.clonePath((String)object6);
                    stringBuilder.append(bl6 ? "\nDo NOT give up, and do NOT blind-rewrite the check from memory \u2014 fix only the line(s) named above IN PLACE, never in a copy" : "\nDo NOT give up, and do NOT blind-rewrite the check from memory \u2014 CLONE it and fix only the line(s) named above");
                    if (!((String)object6).equals(string)) {
                        stringBuilder.append(" (the failing line is in '").append((String)object6).append("', not '").append(string).append("')");
                    }
                    stringBuilder.append(":\n  var src = Check.info('" + (String)object6 + "').source\n  src = src.replace('<dead locator>', '<live locator, from bot.look({full: true})>')\n  File.write('" + string6 + "', src)\n  Check.run('" + (bl6 ? string : string6) + "', params)   // params = the same arg this run got\nEach Check.run re-drives the ENTIRE flow, so prove the one fixed line by hand first: bot.act('<live locator>', ...).");
                }
                try {
                    Object object7 = object6 = this.botUnopened() || Check.unparsedLine(string3) > 0 ? null : this.engine.eval("bot.look({full: true})");
                    if (object6 != null) {
                        stringBuilder.append("\nCurrent page state: ").append(Json.stringifyStrict((Object)object6));
                    }
                }
                catch (Exception exception) {
                    // empty catch block
                }
                object6 = this.settle(bl, string, false, l, string3);
                bl4 = true;
                throw new Failure(stringBuilder.toString(), (Map)object6);
            }
            catch (Throwable throwable2) {
                this.callStack.pop();
                if (!bl4) {
                    this.settle(bl, string, bl3, l, bl3 ? null : "check did not complete");
                }
                this.engine.putRootBinding("__arg", v);
                throw throwable2;
            }
        }
        if (!bl4) {
            this.settle(bl, string, bl3, l, bl3 ? null : "check did not complete");
        }
        this.engine.putRootBinding("__arg", v);
        return linkedHashMap;
    }

   private Map<String, Object> settle(boolean var1, String var2, boolean var3, long var4, String var6) {
      if (var1 && this.onVerdict != null) {
         this.onVerdict.ran(var2, this.topArgless, var3, this.asserted);
      }

      if (var1 && this.onSettle != null) {
         try {
            return this.onSettle.settle(var2, var3, var4, var6);
         } catch (Exception var8) {
            logger.debug("Check.onSettle hook failed for {}: {}", var2, var8.getMessage());
            return null;
         }
      } else {
         return null;
      }
   }

   private static String healedNote(List<Map<String, Object>> var0) {
      if (var0.isEmpty()) {
         return "";
      } else {
         StringBuilder var1 = new StringBuilder("\nA locator HEALED during this run — the check drove a locator its source does not name, so suspect it first:");

         for (Map var3 : var0) {
            var1.append("\n  '")
               .append(var3.get("from"))
               .append("' → '")
               .append(var3.get("to"))
               .append("' (")
               .append(var3.get("action"))
               .append(") — adopt the live locator, or drop the {heal:'auto'} and let the drift fail.");
         }

         return var1.toString();
      }
   }

   Object list(String var1) {
      List var2;
      try {
         var2 = headers(this.checksDir, var1);
      } catch (Exception var4) {
         return JsApi.missing("Check.list() failed: " + var4.getMessage());
      }

      if (var2.isEmpty()) {
         this.adviseOnEmpty();
      }

      return var2;
   }

   static List<Map<String, Object>> headers(Path var0, String var1) {
      ArrayList<java.util.Map<java.lang.String,java.lang.Object>> var2 = new ArrayList();

      for (String var4 : names(var0)) {
         String var5 = var4.substring(0, var4.length() - 3);
         if (var1 == null || var1.isBlank() || var5.startsWith(var1 + "/")) {
            var2.add(header(var4, parseAnnotations(var0.resolve(var4))));
         }
      }

      return var2;
   }

   static List<String> names(Path var0) {
      ArrayList<java.lang.String> var1 = new ArrayList();
      if (!Files.isDirectory(var0)) {
         return var1;
      } else {
         try {
            try (var var2 = Files.walk(var0, Integer.MAX_VALUE)) {
               var2.filter(var0x -> Files.isRegularFile(var0x) && var0x.toString().endsWith(".js"))
                  .sorted()
                  .forEach(var2x -> var1.add(var0.relativize(var2x).toString().replace('\\', '/')));
            }

            return var1;
         } catch (IOException var7) {
            throw new RuntimeException(var7.getMessage(), var7);
         }
      }
   }

   private void adviseOnEmpty() {
      if (Files.isDirectory(this.checksDir)) {
         ArrayList<java.lang.String> var1 = new ArrayList();

         try (var var2 = Files.walk(this.checksDir, Integer.MAX_VALUE)) {
            var2.filter(var0 -> Files.isRegularFile(var0) && var0.toString().endsWith(".feature"))
               .sorted()
               .forEach(var2x -> var1.add(this.checksDir.relativize(var2x).toString().replace('\\', '/')));
         } catch (Exception var7) {
            return;
         }

         if (!var1.isEmpty()) {
            String var8 = String.join(", ", (Iterable<? extends CharSequence>)(var1.size() > 4 ? var1.subList(0, 4) : var1));
            io.karatelabs.ext.common.jsapi.EvaluationMessageScope.a(
               "no `.js` checks here, but `"
                  + this.checksDir.getFileName()
                  + "/` holds "
                  + var1.size()
                  + " gherkin feature(s) — "
                  + var8
                  + (var1.size() > 4 ? ", …" : "")
                  + ". Check.* is the .js snippet lane; run a feature with Runner.run('"
                  + this.checksDir.getFileName()
                  + "/"
                  + (String)var1.get(0)
                  + "') (or the whole folder with Runner.run('"
                  + this.checksDir.getFileName()
                  + "'))."
            );
         }
      }
   }

   Object info(String var1) {
      if (var1 != null && !var1.isBlank()) {
         Path var2 = this.resolveCheck(var1);
         if (var2 == null) {
            return JsApi.notFound("no check '" + var1 + "' — use Check.list() to see available checks");
         } else {
            byte[] var3;
            String var4;
            try {
               var3 = Files.readAllBytes(var2);
               var4 = utf8(var3);
            } catch (Exception var7) {
               return JsApi.notFound("Check.info('" + var1 + "') could not read the check: " + var7.getMessage());
            }

            var var5 = header(var1, annotations(new BufferedReader(new StringReader(var4))));
            var5.put("source", var4);
            var5.put("mtime", io.karatelabs.ext.common.Sha256.a(var3));
            var var6 = this.busy.apply(canonicalName(var1));
            if (var6 != null) {
               var5.put("busy", var6);
            }

            return var5;
         }
      } else {
         return JsApi.missing("Check.info(name) requires a check name");
      }
   }

   static String aiBy(Path var0) {
      return (String)header(var0.getFileName().toString(), parseAnnotations(var0)).get("aiBy");
   }

   private static Map<String, Object> header(String var0, Map<String, String> var1) {
      LinkedHashMap<java.lang.String, java.lang.Object> var2 = new LinkedHashMap();
      var2.put("name", var0);
      var2.put("title", var1.getOrDefault("title", ""));
      var2.put("description", var1.getOrDefault("description", ""));
      var2.put("params", paramList((String)var1.get("params")));
      var2.put("req", paramList((String)var1.get("req")));
      var2.put("ai", var1.containsKey("ai"));
      Matcher var3 = AI_BY.matcher(var1.getOrDefault("ai", ""));
      if (var3.find()) {
         var2.put("aiBy", var3.group(1).toLowerCase());
      }

      return var2;
   }

   public List<String> linkTags(String var1) {
      Path var2 = var1 == null ? null : this.resolveCheck(var1);
      if (var2 == null) {
         return List.of();
      } else {
         var var3 = parseAnnotations(var2);
         ArrayList<java.lang.String> var4 = new ArrayList();
         addLinkTags(var4, "req", (String)var3.get("req"));
         addLinkTags(var4, "cov", (String)var3.get("cov"));
         return var4;
      }
   }

   private static void addLinkTags(List<String> var0, String var1, String var2) {
      if (var2 != null && !var2.isBlank()) {
         for (String var6 : var2.split(",")) {
            String var7 = var1 + "=" + var6.trim();
            if (!var6.isBlank() && !var0.contains(var7)) {
               var0.add(var7);
            }
         }
      }
   }

   Object annotate(String var1, Object var2, Object var3) {
      String var4 = "Check.annotate(name, {title?, description?, req?}[, {ifMatch}]) sets the check's header lines — a key you leave out keeps its line, '' removes it, req replaces the linked ids. Read the current values, and the mtime to pass as ifMatch, with Check.info(name)";
      if (var1 != null && !var1.isBlank() && var2 instanceof Map var5 && !var5.isEmpty()) {
         for (Object var7 : var5.keySet()) {
            if (!List.of("title", "description", "req").contains(String.valueOf(var7))) {
               return JsApi.missing(var4 + " — unknown key '" + var7 + "'");
            }
         }

         Path var13 = this.resolveCheck(var1);
         if (var13 == null) {
            return JsApi.notFound("no check '" + var1 + "' — use Check.list() to see available checks");
         } else {
            LinkedHashMap<java.lang.String, java.lang.Object> var14 = new LinkedHashMap();

            for (String var9 : List.of("title", "description")) {
               if (var5.containsKey(var9)) {
                  Object var10 = var5.get(var9);
                  String var11 = oneLine(var9, var10, "title".equals(var9) ? 80 : 200);
                  if (var11 != null) {
                     return JsApi.missing("Check.annotate: " + var11);
                  }

                  var14.put(var9, var10 == null ? "" : String.valueOf(var10).trim());
               }
            }

            List var15 = null;
            if (var5.containsKey("req")) {
               var15 = reqIds(var5.get("req"));
               if (var15 == null) {
                  return JsApi.missing("Check.annotate: req is a list of requirement ids, e.g. {req: ['ORD-001/1', 'ORD-002']} — [] removes every link");
               }

               for (String var18 : (Iterable<String>) (Iterable<?>) (var15)) {
                  String var21 = io.karatelabs.ext.coverage.graph.TraceGraph.a("req=" + var18);
                  if (var21 != null) {
                     return JsApi.missing("Check.annotate: " + var21);
                  }
               }
            }

            String var17 = var3 instanceof Map var22 && var22.get("ifMatch") instanceof String var19 && !var19.isBlank() ? var19 : null;
            List var20 = var15;
            return this.editHeader("Check.annotate", var1, var13, var17, var2x -> {
               String var3x = var2x;

               for (Entry var5x : (Iterable<Entry>) (Iterable<?>) (var14.entrySet())) {
                  var3x = withAnnotation(var3x, (String)var5x.getKey(), (String)var5x.getValue());
               }

               return var20 == null ? var3x : withReqIds(var3x, var20);
            });
         }
      } else {
         return JsApi.missing(var4);
      }
   }

   Object editReq(String var1, String var2, Object var3, boolean var4) {
      String var5 = var1
         + "(name, id) takes a requirement id or a list of them, e.g. "
         + var1
         + "('login.js', 'ORD-001/2') or "
         + var1
         + "('login.js', ['ORD-001/1', 'ORD-001/2'])";
      if (var2 != null && !var2.isBlank()) {
         Path var6 = this.resolveCheck(var2);
         if (var6 == null) {
            return JsApi.notFound("no check '" + var2 + "' — use Check.list() to see available checks");
         } else {
            var var7 = reqIds(var3);
            if (var7 != null && !var7.isEmpty()) {
               for (String var9 : (Iterable<String>) (Iterable<?>) (var7)) {
                  String var10 = io.karatelabs.ext.coverage.graph.TraceGraph.a("req=" + var9);
                  if (var10 != null) {
                     return JsApi.missing(var1 + ": " + var10);
                  }
               }

               return this.editHeader(var1, var2, var6, null, var2x -> withReqStamps(var2x, var7, var4));
            } else {
               return JsApi.missing(var5 + " — got '" + var3 + "'");
            }
         }
      } else {
         return JsApi.missing(var5);
      }
   }

   private Object editHeader(String var1, String var2, Path var3, String var4, UnaryOperator<String> var5) {
      ReentrantLock var6 = lockOf(var3);
      var6.lock();

      try {
         var var7 = this.busyRefusal(var1, var2);
         if (var7 != null) {
            return var7;
         } else {
            byte[] var8;
            String var9;
            try {
               var8 = Files.readAllBytes(var3);
               var9 = utf8(var8);
            } catch (Exception var20) {
               return JsApi.notFound(var1 + "('" + var2 + "') could not read the check: " + var20.getMessage());
            }

            String var10 = io.karatelabs.ext.common.Sha256.a(var8);
            if (var4 != null && !var4.equals(var10)) {
               LinkedHashMap<java.lang.String, java.lang.String> var26 = new LinkedHashMap();
               var26.put("path", canonicalName(var2));
               var26.put("expectedMtime", var4);
               var26.put("currentMtime", var10);
               LinkedHashMap<java.lang.String, java.lang.Object> var27 = new LinkedHashMap();
               var27.put("error", var1 + ": " + canonicalName(var2) + " changed since it was read — read it again with Check.info and re-apply the edit");
               var27.put("code", "STALE_MTIME");
               var27.put("status", 409);
               var27.put("detail", var26);
               return JsApi.next(var27, "Check.info('" + canonicalName(var2) + "')");
            } else {
               String var25 = var5.apply(var9);
               boolean var12 = !var25.equals(var9);
               if (var12) {
                  var var13 = this.rewrite(var1, var3, var25);
                  if (var13 != null) {
                     return var13;
                  }

                  try {
                     var8 = Files.readAllBytes(var3);
                     var25 = utf8(var8);
                     var10 = io.karatelabs.ext.common.Sha256.a(var8);
                  } catch (Exception var21) {
                     return JsApi.notFound(var1 + "('" + var2 + "') could not read the check back: " + var21.getMessage());
                  }
               }

               io.karatelabs.ext.max.jsapi.Check.CheckSourceIndex var28 = io.karatelabs.ext.max.jsapi.Check.CheckSourceIndex.a(var25);
               LinkedHashMap<java.lang.String, java.lang.Object> var14 = new LinkedHashMap();
               var14.put("check", canonicalName(var2));
               var14.put("title", var28.c("title"));
               var14.put("description", var28.c("description"));
               var14.put("req", var28.d());
               var14.put("mtime", var10);
               var14.put("changed", var12);
               return var14;
            }
         }
      } finally {
         var6.unlock();
      }
   }

   private static String utf8(byte[] var0) throws CharacterCodingException {
      return StandardCharsets.UTF_8.newDecoder().decode(ByteBuffer.wrap(var0)).toString();
   }

   private Map<String, Object> busyRefusal(String var1, String var2) {
      var var3 = this.busy.apply(canonicalName(var2));
      if (var3 == null) {
         return null;
      } else {
         String var4 = String.valueOf(var3.get("runId"));
         LinkedHashMap<java.lang.String, java.lang.Object> var5 = new LinkedHashMap();
         var5.put(
            "error", var1 + ": run " + var4 + " is working on " + canonicalName(var2) + " and reads its header when it settles — edit it once that run is done"
         );
         var5.put("code", "BUSY");
         var5.put("status", 409);
         var5.put("runId", var4);
         return JsApi.next(var5, "Harness.get('" + var4 + "')");
      }
   }

   static ReentrantLock lockOf(Path var0) {
      return HEADER_LOCKS.computeIfAbsent(MaxPaths.lockKey(var0), var0x -> new ReentrantLock());
   }

   private static List<String> reqIds(Object var0) {
      List var1 = var0 instanceof List var2 ? var2 : (var0 == null ? null : List.of(var0));
      if (var1 == null) {
         return null;
      } else {
         ArrayList<java.lang.String> var7 = new ArrayList();

         for (Object var4 : var1) {
            if (var4 instanceof String var5 && !var5.isBlank()) {
               String var6 = var5.trim();
               var6 = var6.startsWith("@") ? var6.substring(1) : var6;
               var6 = var6.startsWith("req=") ? var6.substring("req=".length()) : var6;
               if (!var6.isEmpty() && !var6.contains(",") && !var6.chars().anyMatch(Character::isWhitespace)) {
                  if (!var7.contains(var6)) {
                     var7.add(var6);
                  }
                  continue;
               }

               return null;
            }

            return null;
         }

         return var7;
      }
   }

   private static String oneLine(String var0, Object var1, int var2) {
      if (var1 != null && !(var1 instanceof String)) {
         return var0 + " is text — got " + var1;
      } else {
         String var3 = var1 == null ? "" : ((String)var1).trim();
         if (var3.chars().anyMatch(var0x -> var0x == 8232 || var0x == 8233 || Character.isISOControl(var0x))) {
            return "a " + var0 + " is one line of text — no line breaks or control characters";
         } else {
            return var3.length() > var2
               ? "the " + var0 + " is " + var3.length() + " characters; keep it within " + var2 + " — it is read back on every AI run"
               : null;
         }
      }
   }

   private Map<String, Object> rewrite(String var1, Path var2, String var3) {
      if (this.files != null) {
         return this.files.rewrite(var1, var2, var3);
      } else {
         try {
            Files.writeString(var2, var3);
            return null;
         } catch (Exception var5) {
            return JsApi.missing(var1 + " could not write the check: " + var5.getMessage());
         }
      }
   }

   static String withReqStamps(String var0, List<String> var1, boolean var2) {
      io.karatelabs.ext.max.jsapi.Check.CheckSourceIndex var3 = io.karatelabs.ext.max.jsapi.Check.CheckSourceIndex.a(var0);
      var var4 = new ArrayList<>(var3.d());
      boolean var5 = false;

      for (String var7 : var1) {
         if (var4.contains(var7) != var2) {
            var5 = true;
            if (var2) {
               var4.add(var7);
            } else {
               var4.remove(var7);
            }
         }
      }

      return var5 ? withLine(var3, "req", String.join(", ", var4)) : var0;
   }

   static String withReqIds(String var0, List<String> var1) {
      return withLine(io.karatelabs.ext.max.jsapi.Check.CheckSourceIndex.a(var0), "req", String.join(", ", var1));
   }

   static String withAnnotation(String var0, String var1, String var2) {
      io.karatelabs.ext.max.jsapi.Check.CheckSourceIndex var3 = io.karatelabs.ext.max.jsapi.Check.CheckSourceIndex.a(var0);
      return var3.b(var1).size() == 1 && var3.c(var1).equals(var2) ? var0 : withLine(var3, var1, var2);
   }

   private static String withLine(io.karatelabs.ext.max.jsapi.Check.CheckSourceIndex var0, String var1, String var2) {
      String[] var3 = var0.a();
      var var4 = var0.b(var1);
      var var5 = new ArrayList<>(Arrays.asList(var3));
      if (var4.isEmpty()) {
         if (!var2.isEmpty()) {
            boolean var11 = false;

            for (int var12 = 0; var12 < var3.length - 1 && !var11; var12++) {
               var11 = var3[var12].endsWith("\r");
            }

            var5.add(var0.c() + 1, "// @" + var1 + ": " + var2 + (var11 ? "\r" : ""));
         }

         return String.join("\n", var5);
      } else {
         int var6 = (Integer)var4.get(0);
         String var7 = var3[var6];
         String var8 = var7.substring(0, var7.length() - var7.stripLeading().length());
         String var9 = var7.endsWith("\r") ? "\r" : "";

         for (int var10 = var4.size() - 1; var10 >= 1; var10--) {
            var5.remove(((Integer)var4.get(var10)).intValue());
         }

         if (var2.isEmpty()) {
            var5.remove(var6);
         } else {
            var5.set(var6, var8 + "// @" + var1 + ": " + var2 + var9);
         }

         return String.join("\n", var5);
      }
   }

   static String withAiHeader(String var0, String var1) {
      for (String var5 : var0.split("\n", -1)) {
         String var6 = var5.trim();
         if (!var6.isEmpty()) {
            if (!var6.startsWith("//")) {
               break;
            }

            if (AI_MARKER.matcher(var6).matches()) {
               return var0;
            }
         }
      }

      String var7 = var1 == null ? "" : var1.replaceAll("[\\r\\n]+", " ").trim();
      return "// @ai: " + var7 + (var0.contains("\r\n") ? "\r\n" : "\n") + var0;
   }

   private String dismissBlockingDialog() {
      if (this.botUnopened()) {
         return null;
      } else {
         Object var1;
         try {
            var1 = this.engine.eval("bot.page('dialog', 'info')");
         } catch (Exception var5) {
            return null;
         }

         if (var1 instanceof Map var2) {
            try {
               this.engine.eval("bot.page('dialog', 'dismiss')");
            } catch (Exception var4) {
               logger.debug("blocking dialog could not be dismissed: {}", var4.getMessage());
               return "A "
                  + var2.get("type")
                  + " dialog ('"
                  + var2.get("message")
                  + "') is open and blocking EVERY bot call — and dismissing it FAILED ("
                  + var4.getMessage()
                  + "), so it may still be blocking. Try bot.page('dialog', 'dismiss') yourself. The check must handle it itself: bot.page('dialog', 'accept'|'dismiss') right after the act that opens it.";
            }

            return "A "
               + var2.get("type")
               + " dialog ('"
               + var2.get("message")
               + "') was open and blocking EVERY bot call — DISMISSED for you, so the page is readable again. The check must handle it itself: bot.page('dialog', 'accept'|'dismiss') right after the act that opens it.";
         } else {
            return null;
         }
      }
   }

   private boolean botUnopened() {
      try {
         return this.engine.get("bot") instanceof LazyBot var1 && !var1.isLaunched();
      } catch (Exception var3) {
         return false;
      }
   }

   private boolean robotUnbound() {
      try {
         return Boolean.TRUE.equals(this.engine.eval("typeof Robot === 'undefined'"));
      } catch (Exception var2) {
         return false;
      }
   }

   static String innermostCheck(String var0, String var1) {
      String var2 = var1;
      String var3 = var0 == null ? "" : var0;

      for (Matcher var4 = NESTED_FRAME.matcher(var3); var4.find(); var4 = NESTED_FRAME.matcher(var3)) {
         var2 = var4.group(1);
         var3 = var3.substring(var4.end());
      }

      return var2;
   }

   static int unparsedLine(String var0) {
      Matcher var1 = var0 == null ? null : PARSE_ERROR.matcher(var0);
      return var1 != null && var1.find() ? Integer.parseInt(var1.group(1)) : -1;
   }

   static String clonePath(String var0) {
      String var1 = canonicalName(var0);
      return var1.substring(0, var1.length() - ".js".length()) + "-fixed.js";
   }

   static String canonicalName(String var0) {
      return definitionId(var0).substring("check:".length());
   }

   Path resolveCheck(String var1) {
      if (var1.contains("..")) {
         return null;
      } else {
         String var2 = MaxPaths.stripLeadingSlashes(var1);
         var2 = var2.startsWith("checks/") ? var2.substring("checks/".length()) : var2;
         var2 = var2.endsWith(".js") ? var2 : var2 + ".js";

         Path var3;
         try {
            var3 = MaxPaths.within(this.checksDir, this.checksDir.resolve(var2));
         } catch (SecurityException var5) {
            return null;
         }

         return Files.isRegularFile(var3) ? var3 : null;
      }
   }

   private static List<String> paramList(String var0) {
      return var0 != null && !var0.isBlank()
         ? Arrays.stream(var0.split(",")).map(String::trim).filter(var0x -> !var0x.isEmpty()).distinct().toList()
         : List.of();
   }

   static Map<String, String> parseAnnotations(Path var0) {
      try {
         return annotations(Files.newBufferedReader(var0));
      } catch (Exception var2) {
         return new LinkedHashMap<>();
      }
   }

   private static Map<String, String> annotations(BufferedReader var0) {
      LinkedHashMap<java.lang.String, java.lang.String> var1 = new LinkedHashMap();

      String var3;
      try (BufferedReader var2 = var0) {
         while ((var3 = var2.readLine()) != null) {
            var3 = var3.trim();
            if (!var3.isEmpty()) {
               if (!var3.startsWith("//")) {
                  break;
               }

               Matcher var4 = ANNOTATION.matcher(var3);
               if (var4.matches()) {
                  String var5 = var4.group(1).toLowerCase();
                  var1.merge(
                     var5,
                     var4.group(2).trim(),
                     !"req".equals(var5) && !"cov".equals(var5) ? (var0x, var1x) -> (String)var1x : (var0x, var1x) -> var0x + ", " + var1x
                  );
               } else if (AI_MARKER.matcher(var3).matches()) {
                  var1.putIfAbsent("ai", "");
               }
            }
         }
      } catch (Exception var8) {
      }

      return var1;
   }

   private static Map<String, Object> help() {
      return JsApi.markdown(
         "# Check — run reusable checks/*.js macros\n\nA check is a plain `.js` file in `checks/` that drives the run's `bot` (and other bound\nhandles) directly. Author it once with `File.write('checks/<name>.js', ...)`, then replay it\nas one fast call instead of re-emitting the same steps. Checks are deterministic and LLM-free.\n\n- `Check.run(name, arg?)` → `{ok:true, check, value?, healed?, verified}`. The `.js` extension is optional. The\n  arg is bound as **`__arg`** (karate's call-argument magic var) for the run — the check reads\n  `__arg.foo` (nested `Check.run` is supported; each gets its own `__arg`).\n  **The check's last expression is returned as `value`** — end the check file with the value\n  you want and read it back: `var premium = Check.run('read-quote.js', row).value` (omitted\n  when the check ends on a statement). Note: if a check's last expression is itself a\n  `Check.run(...)`, its `value` is that inner result object (`{ok, check, value}`) — so you'd\n  read `.value.value`; end the check on a plain domain value (or `...value`) for a flat\n  return. On failure it THROWS a decorated error (message + current page state) — read it and\n  fix the check; do not wrap it in `if (!result.ok)`. The failure names the first dead\n  locator, any heals the run absorbed, and a clone recipe (`Check.info` → `File.write` a\n  fixed clone → `Check.run` the clone) — follow it. Acts inside a check run heal-STRICT:\n  drift fails honestly instead of self-healing; a per-act `{heal:'auto'}` opts back\n  in, and an absorbed heal is then reported as `healed:[…]` on the result — a green check\n  that healed drove a locator its source does not name, so update the source.\n  **A BROWSER check ends on `bot.destroy()` instead** — that is its last expression, so its\n  `value` is the release record, not a domain value. `destroy()` ends the run and clears its\n  vars, so a trailing `…; myTotal` after it throws \"not defined\" (only when the check OWNS\n  the bot — a borrowed one is a no-op and the var survives, which is exactly why you must\n  not rely on it). Carry what you read in `verify(ok, note)` / `Robot.step` names, where it\n  lands as evidence, rather than in the return value.\n- `verified` is the check's own record — `{assertions, failed, assertedProportion?, criteria[],\n  hits:{observed, declared}, dropped}`; a failed check's error carries the same. A nested\n  `Check.run` owns no record: its assertions count into the caller's.\n- A `Check.run` eval lasts as long as the WHOLE check — set a generous client/HTTP\n  timeout on the call (e.g. `curl --max-time 120` or more); the server never times an\n  eval out, so a short client timeout abandons a healthy run.\n- `Check.list(folder?)` → `[{name, title, description, params, req, ai}]`; `Check.info(name)`\n  adds `source` and `mtime` (read a check before editing). Checks only — `Runner.tests()` lists\n  every replayable test, the project's `.feature` suites included.\n- `Check.annotate(name, {title?, description?, req?}, {ifMatch: mtime})` sets the header in one\n  write — `''` removes a line, `req` replaces the ids; a file changed since answers 409\n  `STALE_MTIME`, a check a run is working on 409 `BUSY`. `Check.link(name, ids)` /\n  `Check.unlink(name, ids)` add or remove ids in `// @req:`. The rest of the file is unchanged.\n\nChecks self-describe via leading comment annotations:\n```js\n// @title: Log in\n// @description: log in with the given credentials\n// @params: user, pass\n// @req: ORD-001/1\nbot.go('/login')\nbot.act('#user', 'input', __arg.user)\nbot.act('#pass', 'input', __arg.pass)\nbot.act('{button}Sign In', 'click')\n```\nThen: `Check.run('login.js', {user:'admin', pass:'secret'})`.\n\n`// @req:` / `// @cov:` (comma = multi-value) link the check's runs to a requirement /\ncoverage item, whether it calls `Http.*` or only drives `bot`; its `Http.*` calls also count\nas exercised API coverage. `// @ai:` marks a check an AI wrote: the server adds it to every\n`checks/*.js` that `Harness.author` / `heal` / `revise` writes; only a human commit removes it."
      );
   }

   public static final class Failure extends RuntimeException {
      private final Map<String, Object> verified;

      Failure(String var1, Map<String, Object> var2) {
         super(var1);
         this.verified = var2;
      }

      public Map<String, Object> verified() {
         return this.verified;
      }

      public static Check.Failure in(Throwable var0) {
         Throwable var1 = var0;

         for (int var2 = 0; var1 != null && var2 < 10; var2++) {
            if (var1 instanceof Check.Failure var3) {
               return var3;
            }

            if (var1.getCause() == var1) {
               break;
            }

            var1 = var1.getCause();
         }

         return null;
      }
   }

   @FunctionalInterface
   public interface Settle {
      Map<String, Object> settle(String var1, boolean var2, long var3, String var5);
   }

   @FunctionalInterface
   public interface Verdict {
      default void starting(String var1, boolean var2) {
      }

      void ran(String var1, boolean var2, boolean var3, int var4);

      default boolean inPlace() {
         return false;
      }
   }

   private record CheckSourceIndex(String[] a, Map<String, List<Integer>> b, int c, List<String> d) {


      static io.karatelabs.ext.max.jsapi.Check.CheckSourceIndex a(String var0) {
         String[] var1 = var0.split("\n", -1);
         LinkedHashMap<String, List<Integer>> var2 = new LinkedHashMap<>();
         ArrayList<java.lang.String> var3 = new ArrayList();
         int var4 = -1;

         for (int var5 = 0; var5 < var1.length; var5++) {
            String var6 = var1[var5].trim();
            if (!var6.isEmpty()) {
               if (!var6.startsWith("//")) {
                  break;
               }

               Matcher var7 = Check.ANNOTATION.matcher(var6);
               if (var7.matches()) {
                  var4 = var5;
                  String var8 = var7.group(1).toLowerCase();
                  var2.computeIfAbsent(var8, var0x -> new ArrayList<>()).add(var5);
                  if ("req".equals(var8)) {
                     for (String var12 : var7.group(2).split(",")) {
                        String var13 = var12.trim();
                        if (!var13.isEmpty() && !var3.contains(var13)) {
                           var3.add(var13);
                        }
                     }
                  }
               }
            }
         }

         return new io.karatelabs.ext.max.jsapi.Check.CheckSourceIndex(var1, var2, var4, var3);
      }

      List<Integer> b(String var1) {
         return this.b.getOrDefault(var1, List.of());
      }

      String c(String var1) {
         var var2 = this.b(var1);
         if (var2.isEmpty()) {
            return "";
         } else {
            Matcher var3 = Check.ANNOTATION.matcher(this.a[var2.get(var2.size() - 1)].trim());
            return var3.matches() ? var3.group(2).trim() : "";
         }
      }
   }
}
