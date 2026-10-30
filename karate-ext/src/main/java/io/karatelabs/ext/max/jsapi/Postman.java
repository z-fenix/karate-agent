package io.karatelabs.ext.max.jsapi;

import io.karatelabs.common.Json;
import io.karatelabs.common.StringUtils;
import io.karatelabs.core.ConfigLoader;
import io.karatelabs.ext.agent.llm.SecretRedactor;
import io.karatelabs.ext.common.MaxPaths;
import io.karatelabs.ext.common.Sha256;
import io.karatelabs.ext.common.jsapi.JsApi;
import io.karatelabs.ext.common.jsapi.JsDispatch;
import io.karatelabs.ext.max.apitest.FeatureStore;
import io.karatelabs.ext.max.apitest.KarateConfigBlock;
import io.karatelabs.ext.postman.PmRuntime;
import io.karatelabs.js.JsonParser;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class Postman extends JsDispatch {
   private static final String CONFIG = "karate-config.js";
   private static final Pattern VARIABLE = Pattern.compile("\\{\\{([^{}]+)}}");
   private final Path root;
   private final Runner runner;
   private boolean noProject;
   private SecretRedactor sanitizer = new SecretRedactor(Map::of);
   private ConsoleState console = ConsoleState.HEADLESS;
   private static final Pattern SET_BY_SCRIPT = Pattern.compile("\\.set(?:\\w*Variable)?\\(\\s*[\"']([^\"']+)[\"']");
   private static final long DATA_SNIFF_BYTES = 1048576L;
   private static final Set<String> NOT_DATA_HOMES = Set.of("rulebooks", "requirements", "checks", "runs", ".karate");

   public Postman(Path var1, Runner var2) {
      this.root = var1.toAbsolutePath().normalize();
      this.runner = var2;
      this.fn("read", var1x -> this.written(this.read(JsApi.str(var1x, 0), JsApi.arg(var1x, 1))));
      this.fn("item", var1x -> io.karatelabs.ext.postman.editor.PostmanCollectionEditor.b(this.written(this.item(JsApi.str(var1x, 0), JsApi.arg(var1x, 1)))));
      this.fn("edit", var1x -> this.masked(this.edit(JsApi.str(var1x, 0), JsApi.arg(var1x, 1))));
      this.fn("run", var2x -> this.masked(var2.postman(JsApi.str(var2x, 0), JsApi.arg(var2x, 1))));
      this.fn("results", var1x -> this.masked(this.results(JsApi.str(var1x, 0), JsApi.arg(var1x, 1))));
      this.fn(
         "env",
         var1x -> JsApi.arg(var1x, 1) instanceof Map var2x && var2x.get("from") != null
            ? this.env(JsApi.str(var1x, 0), var2x)
            : this.masked(this.env(JsApi.str(var1x, 0), JsApi.arg(var1x, 1)))
      );
      this.fn("migrate", var1x -> this.written(this.migrate(JsApi.arg(var1x, 0), JsApi.arg(var1x, 1))));
      this.fn("data", var1x -> this.written(this.data(JsApi.str(var1x, 0), JsApi.arg(var1x, 1))));
      this.fn("help", var0 -> io.karatelabs.ext.common.help.HelpTopicProvider.a("postman").a(var0));
   }

   public Postman withNoProject(boolean var1) {
      this.noProject = var1;
      return this;
   }

   public Postman withSanitizer(SecretRedactor var1) {
      this.sanitizer = var1;
      return this;
   }

   public Postman withConsole(ConsoleState var1) {
      this.console = var1;
      return this;
   }

   private Object masked(Object var1) {
      return Json.parseStrict(this.sanitizer.redactKnownAndGeneric(StringUtils.formatJson(var1, false, false, false)), true);
   }

   private Object written(Object var1) {
      return Json.parseStrict(this.sanitizer.maskConfiguredSecrets(StringUtils.formatJson(var1, false, false, false)), true);
   }

   Object read(String var1, Object var2) {
      Object var3 = this.collection("Postman.read", var1);
      if (var3 instanceof Map var17) {
         return var17;
      } else {
         io.karatelabs.ext.max.jsapi.Postman.LoadedCollection var4 = (io.karatelabs.ext.max.jsapi.Postman.LoadedCollection)var3;
         Map<java.lang.String, Object> var5 = var2 instanceof Map var6 ? var6 : Map.of();
         String var18 = var5.get("item") == null ? "" : String.valueOf(var5.get("item"));
         io.karatelabs.ext.postman.PostmanCollectionNode var7 = io.karatelabs.ext.postman.PostmanCollectionTreeMapper.a(var4.a());
         io.karatelabs.ext.postman.PostmanCollectionNode var8 = io.karatelabs.ext.postman.PostmanCollectionTreeMapper.a(var7, var18);
         if (var8 == null) {
            return JsApi.next(JsApi.notFound("no item " + var18 + " in " + var1), "Postman.read('" + var1 + "')");
         } else {
            LinkedHashMap<java.lang.String, java.lang.Object> var9 = new LinkedHashMap();
            io.karatelabs.ext.postman.PostmanCollectionTreeMapper.RunStatusIndex var10 = null;
            if (var5.get("runId") != null) {
               String var11 = String.valueOf(var5.get("runId"));
               Object var12 = this.rows(var11);
               if (var12 instanceof Map var22) {
                  return var22;
               }

               LinkedHashMap<java.lang.String, java.lang.Object> var13 = new LinkedHashMap();
               var13.put("runId", var11);
               Map<java.lang.String, Object> var14 = this.runner.runStore().c(var11) ? this.runner.runStore().b(var11) : Map.of();
               if (!var4.b().equals(var14.get("collection"))) {
                  return JsApi.notFound("run " + var11 + " is not a run of " + var4.b());
               }

               if (!var4.a().c().equals(var14.get("version"))) {
                  var13.put("stale", true);
                  var13.put("reason", "the collection changed since this run; run it again for statuses");
               } else {
                  List var15 = (List)var12;
                  var10 = io.karatelabs.ext.postman.PostmanCollectionTreeMapper.RunStatusIndex.a(var15);
                  var13.put("rows", var10.a());
                  var var16 = List.of(io.karatelabs.ext.postman.PostmanCollectionTreeMapper.a(var8, var10));
                  var13.put("status", io.karatelabs.ext.postman.PostmanCollectionTreeMapper.a(var16));
                  var13.put("byRow", var16);
               }

               var9.put("run", var13);
            }

            var var19 = var7.b.asMap();
            Map<java.lang.String, Object> var20 = var19.get("info") instanceof Map var21 ? var21 : Map.of();
            var9.put("version", var4.a().c());
            if (var4.a().g() != null) {
               var9.put("editable", false);
               var9.put("reason", var4.a().g());
            }

            var9.put("name", var20.get("name"));
            var9.put("schema", var20.get("schema"));
            var9.put("auth", var19.get("auth"));
            var9.put("variables", var19.get("variable") == null ? List.of() : var19.get("variable"));
            if (!var18.isEmpty()) {
               var9.put("item", var18);
            }

            var9.put("items", io.karatelabs.ext.postman.PostmanCollectionTreeMapper.a(var8, Boolean.TRUE.equals(var5.get("full")), var10));
            return var9;
         }
      }
   }

   Object item(String var1, Object var2) {
      Object var3 = this.collection("Postman.item", var1);
      if (var3 instanceof Map var13) {
         return var13;
      } else {
         io.karatelabs.ext.max.jsapi.Postman.LoadedCollection var4 = (io.karatelabs.ext.max.jsapi.Postman.LoadedCollection)var3;
         if (var2 instanceof Map var5 && var5.get("item") instanceof String var6) {
            io.karatelabs.ext.postman.PostmanCollectionNode var14 = io.karatelabs.ext.postman.PostmanCollectionTreeMapper.a(io.karatelabs.ext.postman.PostmanCollectionTreeMapper.a(var4.a()), var6);
            if (var14 == null) {
               return JsApi.next(JsApi.notFound("no item " + var6 + " in " + var1), "Postman.read('" + var1 + "')");
            } else {
               String var8 = var4.a().g();
               LinkedHashMap<String, Object> var9 = new LinkedHashMap();
               var9.put("version", var4.a().c());
               var9.putAll(io.karatelabs.ext.postman.PostmanCollectionTreeMapper.a(var14, var1x -> var8 != null ? var8 : var1x));
               if (var9.get("auth") instanceof Map var10 && var10.get("resolved") instanceof Map var11 && "none".equals(var11.get("type"))) {
                  var var16 = this.preset(var4.a(), var6);
                  if (var16 != null) {
                     var10.put("resolved", var16);
                  }
               }

               return var9;
            }
         } else {
            return JsApi.missing("Postman.item(path, {item}) needs the item's pointer, as Postman.read lists it");
         }
      }
   }

   private Map<String, Object> preset(io.karatelabs.ext.postman.editor.PostmanCollectionDocument var1, String var2) {
      io.karatelabs.ext.postman.PostmanCollectionNode var3 = io.karatelabs.ext.postman.PostmanCollectionTreeMapper.a(var1);
      if (!io.karatelabs.ext.postman.PostmanAuthConverter.a(var3)) {
         return null;
      } else {
         ConsoleState.Kickoff var4 = this.console.at(null);

         Map var5;
         try {
            var5 = ConfigLoader.configOnly(this.root, var4.env(), null, var4.overlay());
         } catch (Exception var8) {
            return null;
         }

         if (!io.karatelabs.ext.postman.PostmanAuthConverter.a(var3, var5)) {
            return null;
         } else {
            io.karatelabs.ext.postman.PostmanCollectionNode var6 = PmRuntime.resolveAuth(io.karatelabs.ext.postman.PostmanCollectionTreeMapper.a(var3, var2));
            if (var6 != null && var6.a()) {
               LinkedHashMap<java.lang.String, java.lang.Object> var7 = new LinkedHashMap();
               var7.put("type", var3.v());
               var7.put("from", "environment");
               var7.put("item", "");
               return var7;
            } else {
               return null;
            }
         }
      }
   }

   Object edit(String var1, Object var2) {
      if (this.noProject) {
         return JsApi.noProjectSelected("Postman.edit");
      } else {
         Object var3 = this.file("Postman.edit", var1);
         if (var3 instanceof Map var11) {
            return var11;
         } else if (var2 instanceof Map var4) {
            if (var4.get("ifMatch") != null && !(var4.get("ifMatch") instanceof String)) {
               return JsApi.missing("Postman.edit: ifMatch is the version string the read answered");
            } else {
               String var5 = (String)var4.get("ifMatch");
               Object var6 = var4.get("undo");
               Object var7 = var4.get("edits");
               if (var6 == null && !(var7 instanceof List)) {
                  return JsApi.missing("Postman.edit: edits is a list of edit entries, or undo an earlier edit's token");
               } else if (var6 != null && !(var6 instanceof String)) {
                  return JsApi.missing("Postman.edit: undo is the token an earlier edit answered");
               } else {
                  boolean var8 = Boolean.TRUE.equals(var4.get("dryRun"));
                  String var9 = this.rel((Path)var3);
                  String var10 = ((Path)var3).getFileName().toString();
                  return FeatureStore.locked(
                     (Path)var3,
                     () -> {
                        io.karatelabs.ext.postman.editor.PostmanCollectionDocument var8x;
                        try {
                           var8x = io.karatelabs.ext.postman.editor.PostmanCollectionDocument.a((Path)var3);
                        } catch (IOException var12) {
                           return JsApi.notFound("Postman.edit could not read '" + var1 + "': " + var12.getMessage());
                        }

                        io.karatelabs.ext.postman.editor.PostmanCollectionEditor.CollectionEditResult var9x = var6 != null
                           ? io.karatelabs.ext.postman.editor.PostmanCollectionEditor.a(var8x, var10, (String)var6, var5)
                           : io.karatelabs.ext.postman.editor.PostmanCollectionEditor.a(var8x, var10, (List<?>)var7, var5);
                        LinkedHashMap<java.lang.String, java.lang.Object> var10x = new LinkedHashMap();
                        var10x.put("path", var9);
                        if (!var9x.a()) {
                           var10x.putAll(var9x.c());
                           return (Map<String, Object>)("STALE".equals(var10x.get("code")) ? JsApi.next(var10x, "Postman.read('" + var9 + "')") : var10x);
                        } else {
                           if (!var8 && var9x.l() != null) {
                              var var11x = FeatureStore.write((Path)var3, var9x.d(), var8x.c());
                              if (var11x != null) {
                                 var10x.putAll(var11x);
                                 return var10x;
                              }
                           }

                           var10x.putAll(var9x.b());
                           if (var8) {
                              var10x.remove("undo");
                              var10x.put("dryRun", true);
                           }

                           return var10x;
                        }
                     }
                  );
               }
            }
         } else {
            return JsApi.missing("Postman.edit(path, {ifMatch, edits, dryRun?}) — or {ifMatch, undo}");
         }
      }
   }

   Object results(String var1, Object var2) {
      Object var3 = this.rows(var1);
      if (var3 instanceof Map var12) {
         return var12;
      } else {
         Map<java.lang.String, Object> var4 = var2 instanceof Map var5 ? var5 : Map.of();
         String var13 = var4.get("item") == null ? null : String.valueOf(var4.get("item"));
         Integer var6 = var4.get("row") instanceof Number var7 ? var7.intValue() : null;
         ArrayList<java.util.Map> var14 = new ArrayList();

         for (Object var9 : (List)var3) {
            Map<java.lang.String, java.lang.Number> var10 = (Map)var9;
            String var11 = String.valueOf(var10.get("item"));
            if ((var13 == null || var13.isEmpty() || var11.equals(var13) || var11.startsWith(var13 + "/"))
               && (var6 == null || var6 == ((Number)var10.get("row")).intValue())) {
               var14.add(var10);
            }
         }

         return var14;
      }
   }

   private Object rows(String var1) {
      if (var1 != null && var1.matches("[A-Za-z0-9._-]{1,64}")) {
         Path var2 = this.root.resolve("runs").resolve(var1).resolve("postman.jsonl");
         if (!Files.isRegularFile(var2)) {
            return JsApi.notFound("no Postman results for run " + var1 + " — a run that is still going has none yet");
         } else {
            ArrayList<java.lang.Object> var3 = new ArrayList();

            try {
               for (String var5 : Files.readAllLines(var2, StandardCharsets.UTF_8)) {
                  if (!var5.isBlank()) {
                     var3.add(JsonParser.parse(var5));
                  }
               }

               return var3;
            } catch (IOException var6) {
               return JsApi.notFound("run " + var1 + "'s results could not be read: " + var6.getMessage());
            }
         }
      } else {
         return JsApi.missing("Postman.results(runId): runId is the id Postman.run answered, got " + var1);
      }
   }

   Object env(String var1, Object var2) {
      Map<java.lang.String, Object> var3 = var2 instanceof Map var4 ? var4 : Map.of();
      if (var3.get("from") != null) {
         return this.importEnv(var1, var3);
      } else {
         Object var24 = this.collection("Postman.env", var1);
         if (var24 instanceof Map var25) {
            return var25;
         } else {
            io.karatelabs.ext.max.jsapi.Postman.LoadedCollection var5 = (io.karatelabs.ext.max.jsapi.Postman.LoadedCollection)var24;
            String var6 = var3.get("env") instanceof String var7 && !var7.isBlank() ? var7.trim() : null;
            if (var6 != null && !var6.matches("[A-Za-z0-9._-]{1,64}")) {
               return JsApi.missing("Postman.env: env is a name Runner.envs() lists, got " + var6);
            } else {
               var var26 = Map.of();
               if (var3.get("data") instanceof String var27) {
                  try {
                     var var29 = io.karatelabs.ext.postman.PostmanDataFileParser.a(MaxPaths.resolveSandboxed(this.root, var27), var27);
                     var26 = (Map)var29.get(0);
                  } catch (SecurityException | io.karatelabs.ext.postman.PostmanDataFileParser.PostmanDataFileException var23) {
                     var var10 = JsApi.missing("Postman.env: " + var23.getMessage());
                     var10.put("code", "BAD_DATA");
                     return var10;
                  }
               }

               ConsoleState.Kickoff var28 = this.console.at(var6);

               Map var30;
               try {
                  var30 = ConfigLoader.configOnly(this.root, var28.env(), null, var28.overlay());
               } catch (Exception var22) {
                  var30 = Map.of();
               }

               LinkedHashSet<java.lang.String> var31 = new LinkedHashSet();

               try {
                  Path var11 = this.root.resolve("karate-config.js");
                  if (Files.isRegularFile(var11)) {
                     KarateConfigBlock.State var12 = KarateConfigBlock.read(Files.readString(var11, StandardCharsets.UTF_8));
                     if (var12.entries() != null) {
                        var12.entries().stream().filter(var0 -> var0.get("secret") != null).forEach(var1x -> var31.add(String.valueOf(var1x.get("name"))));
                     }
                  }
               } catch (IOException var21) {
               }

               io.karatelabs.ext.postman.PostmanCollectionNode var32 = io.karatelabs.ext.postman.PostmanCollectionTreeMapper.a(var5.a());
               LinkedHashMap<java.lang.String, java.lang.Object> var33 = new LinkedHashMap();

               for (Map var14 : var32.G()) {
                  if (var14.get("key") != null && !Boolean.TRUE.equals(var14.get("disabled"))) {
                     var33.put(String.valueOf(var14.get("key")), var14.get("value"));
                  }
               }

               LinkedHashSet var34 = new LinkedHashSet();
               variables(var32.b.value(), var34);
               ArrayList<java.util.LinkedHashMap> var35 = new ArrayList();

               for (String var16 : (Iterable<String>) (Iterable<?>) (var34)) {
                  LinkedHashMap<java.lang.String, java.lang.Object> var17 = new LinkedHashMap();
                  var17.put("name", var16);
                  Object var18;
                  String var19;
                  if (var26.containsKey(var16)) {
                     var19 = "data";
                     var18 = var26.get(var16);
                  } else if (var30.get(var16) instanceof String || var30.get(var16) instanceof Number || var30.get(var16) instanceof Boolean) {
                     var19 = "config";
                     var18 = var30.get(var16);
                  } else if (var33.containsKey(var16)) {
                     var19 = "collection";
                     var18 = var33.get(var16);
                  } else {
                     var19 = "unset";
                     var18 = null;
                  }

                  boolean var20 = io.karatelabs.ext.agent.llm.SecretRedactor.isSensitiveName(var16) || var31.contains(var16);
                  var17.put("from", var19);
                  var17.put("value", var20 && var18 != null && !"".equals(var18) ? "‹masked›" : var18);
                  var17.put("secret", var20);
                  var35.add(var17);
               }

               return var35;
            }
         }
      }
   }

   private static void variables(Object var0, Set<String> var1) {
      if (var0 instanceof String var2) {
         Matcher var5 = VARIABLE.matcher(var2);

         while (var5.find()) {
            String var6 = var5.group(1).trim();
            if (!var6.startsWith("$")) {
               var1.add(var6);
            }
         }
      } else if (var0 instanceof Map var3) {
         var3.values().forEach(var1x -> variables(var1x, var1));
      } else if (var0 instanceof List var4) {
         var4.forEach(var1x -> variables(var1x, var1));
      }
   }

   private Object importEnv(String var1, Map<?, ?> var2) {
      if (this.noProject) {
         return JsApi.noProjectSelected("Postman.env");
      } else {
         String var3 = String.valueOf(var2.get("from"));

         Path var4;
         try {
            var4 = MaxPaths.resolveSandboxed(this.root, var3);
         } catch (SecurityException var29) {
            return JsApi.missing("Postman.env denied: " + var29.getMessage());
         }

         Object var5;
         try {
            var5 = Files.isRegularFile(var4) ? JsonParser.parseSpans(Files.readString(var4, StandardCharsets.UTF_8)).root().toValue() : null;
         } catch (Exception var28) {
            var5 = null;
         }

         if (var5 instanceof Map var6 && var6.get("values") instanceof List var7) {
            String var30 = var2.get("env") == null ? null : String.valueOf(var2.get("env"));
            Path var9 = this.root.resolve("karate-config.js");

            String var10;
            try {
               var10 = Files.isRegularFile(var9) ? Files.readString(var9, StandardCharsets.UTF_8) : null;
            } catch (IOException var27) {
               return JsApi.notFound("Postman.env could not read karate-config.js: " + var27.getMessage());
            }

            Object var11 = var2.get("ifMatch");
            if (var10 != null && var11 == null) {
               return JsApi.missing("Postman.env: pass ifMatch, the version ApiTest.config() answered");
            } else {
               String var12 = var10 == null ? null : io.karatelabs.ext.common.Sha256.a(var10.getBytes(StandardCharsets.UTF_8));
               LinkedHashMap<java.lang.String, java.lang.String> var13 = new LinkedHashMap();
               ArrayList<java.util.LinkedHashMap> var14 = new ArrayList();
               String var15 = var10;
               List var16 = null;

               for (Object var18 : var7) {
                  if (var18 instanceof Map var19 && var19.get("key") != null && !Boolean.FALSE.equals(var19.get("enabled"))) {
                     String var20 = String.valueOf(var19.get("key"));
                     Object var21 = var19.get("value") == null ? "" : var19.get("value");
                     LinkedHashMap<java.lang.String, java.lang.Object> var22 = new LinkedHashMap();
                     var22.put("name", var20);
                     if (var30 != null) {
                        var22.put("env", var30);
                     }

                     String var23 = null;
                     if (!"secret".equals(var19.get("type")) && !io.karatelabs.ext.agent.llm.SecretRedactor.isSensitiveName(var20)) {
                        var22.put("value", var21);
                     } else {
                        var23 = secretName(var20);
                        var22.put("secret", var23);
                     }

                     KarateConfigBlock.Result var24 = KarateConfigBlock.apply(var15, var22);
                     if (var24.error() != null) {
                        if (!"BAD_REQUEST".equals(var24.error().get("code"))) {
                           return var24.error();
                        }

                        LinkedHashMap<java.lang.String, java.lang.Object> var25 = new LinkedHashMap();
                        var25.put("name", var20);
                        var25.put("reason", var24.error().get("error"));
                        var14.add(var25);
                     } else {
                        var15 = var24.text();
                        var16 = var24.entries();
                        if (var23 != null) {
                           var13.put(var23, String.valueOf(var21));
                        }
                     }
                  }
               }

               io.karatelabs.ext.common.secrets.SecretStore var31 = this.console.store();
               if (!var13.isEmpty() && var31 == null) {
                  return JsApi.missing("Postman.env: this environment holds secrets, and the secret store is the console's — import it from the console");
               } else {
                  LinkedHashMap<java.lang.String, java.lang.Object> var32 = new LinkedHashMap();
                  var32.put("path", "karate-config.js");
                  if (var16 == null) {
                     var32.put("version", var12);
                     var32.put("entries", var10 == null ? List.of() : KarateConfigBlock.read(var10).entries());
                  } else {
                     byte[] var33 = var15.getBytes(StandardCharsets.UTF_8);
                     var var34 = FeatureStore.write(var9, var33, var11 == null ? null : String.valueOf(var11));
                     if (var34 != null) {
                        var32.putAll(var34);
                        return "STALE".equals(var34.get("code")) ? JsApi.next(var32, "ApiTest.config()") : var32;
                     }

                     try {
                        var13.forEach(var31::a);
                     } catch (IllegalStateException var26) {
                        return JsApi.missing("Postman.env wrote karate-config.js but not its secrets: " + var26.getMessage());
                     }

                     var32.put("version", io.karatelabs.ext.common.Sha256.a(var33));
                     var32.put("basedOn", var12);
                     var32.put("entries", var16);
                  }

                  if (!var14.isEmpty()) {
                     var32.put("skipped", var14);
                  }

                  return var32;
               }
            }
         } else {
            return JsApi.missing("Postman.env: " + var3 + " is not a Postman environment (an object with values)");
         }
      }
   }

   static String secretName(String var0) {
      String var1 = var0.replaceAll("([a-z0-9])([A-Z])", "$1_$2").replaceAll("[^A-Za-z0-9_]", "_").toUpperCase();
      return Character.isDigit(var1.charAt(0)) ? "_" + var1 : var1;
   }

   Object migrate(Object var1, Object var2) {
      Object var3 = this.migration(var1, var2);
      return var3 instanceof Postman.Migrated var4
         ? JsApi.next(
            var4.answer(),
            "Harness.api('Migrate this folder to Karate as " + var4.to() + ": run it until it passes, then propose links', {test: '" + var4.to() + "'})"
         )
         : var3;
   }

   public Object migration(Object var1, Object var2) {
      if (!(var1 instanceof String var3)) {
         return JsApi.missing("Postman.migrate(path, {item}): path is the collection file's project-relative path, a string; got " + shape(var1));
      } else {
         Object var4 = this.collection("Postman.migrate", var3);
         if (var4 instanceof Map var19) {
            return var19;
         } else {
            io.karatelabs.ext.max.jsapi.Postman.LoadedCollection var5 = (io.karatelabs.ext.max.jsapi.Postman.LoadedCollection)var4;
            Map<java.lang.String, java.lang.String> var6 = var2 instanceof Map var7 ? var7 : Map.of();
            if (var6.get("item") != null && !(var6.get("item") instanceof String)) {
               return JsApi.missing("Postman.migrate: item is a pointer ('/item/0') or a folder's or request's name, a string; got " + shape(var6.get("item")));
            } else {
               io.karatelabs.ext.postman.PostmanCollectionNode var20 = io.karatelabs.ext.postman.PostmanCollectionTreeMapper.a(var5.a());
               Object var8 = named(var20, var6.get("item") == null ? "" : (String)var6.get("item"), var3);
               if (var8 instanceof Map var21) {
                  return var21;
               } else {
                  io.karatelabs.ext.postman.PostmanCollectionNode var9 = (io.karatelabs.ext.postman.PostmanCollectionNode)var8;
                  String var10 = io.karatelabs.ext.postman.PostmanCollectionTreeMapper.a(var9);
                  if (io.karatelabs.ext.postman.convert.PostmanFeatureConverter.a(var9).isEmpty()) {
                     return JsApi.missing("Postman.migrate: " + (var10.isEmpty() ? "the collection" : var10) + " holds no request");
                  } else {
                     String var11 = null;
                     Object var12 = List.of();
                     if (var6.get("data") != null) {
                        String var13 = String.valueOf(var6.get("data"));

                        try {
                           Path var14 = MaxPaths.resolveSandboxed(this.root, var13);
                           var var26 = io.karatelabs.ext.postman.PostmanDataFileParser.a(var14, var13);
                           var12 = new ArrayList(((Map)var26.get(0)).keySet());
                           var11 = "/" + this.rel(var14);
                        } catch (SecurityException | io.karatelabs.ext.postman.PostmanDataFileParser.PostmanDataFileException var18) {
                           var var15 = JsApi.missing("Postman.migrate: " + var18.getMessage());
                           var15.put("code", "BAD_DATA");
                           return var15;
                        }
                     }

                     String var22;
                     if (var6.get("to") != null) {
                        var22 = String.valueOf(var6.get("to")).replaceFirst("^/+", "");
                        if (!var22.endsWith(".feature")) {
                           return JsApi.missing("Postman.migrate: to is a project-relative .feature path, got " + var22);
                        }

                        try {
                           if (Files.exists(MaxPaths.resolveSandboxed(this.root, var22))) {
                              var var23 = JsApi.missing(var22 + " already exists — pass another to");
                              var23.put("code", "EXISTS");
                              var23.put("status", 409);
                              return var23;
                           }
                        } catch (SecurityException var17) {
                           return JsApi.missing("Postman.migrate denied: " + var17.getMessage());
                        }
                     } else {
                        String var24 = var9.d() == null ? "" : var9.d().toLowerCase().replaceAll("[^a-z0-9]+", "-").replaceAll("^-+|-+$", "");
                        String var27 = "checks/" + (var24.isEmpty() ? "postman" : var24);
                        var22 = var27 + ".feature";

                        for (int var16 = 2; Files.exists(this.root.resolve(var22)); var16++) {
                           var22 = var27 + "-" + var16 + ".feature";
                        }
                     }

                     String var25 = var5.b() + (var10.isEmpty() ? "" : " " + var10);
                     io.karatelabs.ext.postman.convert.PostmanConversionResult var28 = io.karatelabs.ext.postman.convert.PostmanFeatureConverter.a(
                        var9, new io.karatelabs.ext.postman.convert.PostmanFeatureConverter.MigrationOptions(var25, var11, (List<String>)var12)
                     );
                     LinkedHashMap<java.lang.String, java.lang.Object> var29 = new LinkedHashMap();
                     var29.put("path", var5.b());
                     var29.put("item", var10);
                     var29.put("to", var22);
                     var29.put("requests", var28.c());
                     var29.put("scenarios", var28.d());
                     var29.put("notConverted", var28.a());
                     var29.put("needs", var28.f());
                     var29.put("feature", var28.b());
                     return new Postman.Migrated(var22, var28.b().getBytes(StandardCharsets.UTF_8), var29);
                  }
               }
            }
         }
      }
   }

   private static Object named(io.karatelabs.ext.postman.PostmanCollectionNode var0, String var1, String var2) {
      if (!var1.isEmpty() && !var1.startsWith("/")) {
         ArrayList<io.karatelabs.ext.postman.PostmanCollectionNode> var7 = new ArrayList();
         ArrayDeque var4 = new ArrayDeque<>(var0.d);

         while (!var4.isEmpty()) {
            io.karatelabs.ext.postman.PostmanCollectionNode var5 = (io.karatelabs.ext.postman.PostmanCollectionNode)var4.poll();
            if (var5.d() != null && var5.d().strip().equalsIgnoreCase(var1.strip())) {
               var7.add(var5);
            }

            var4.addAll(var5.d);
         }

         if (var7.size() == 1) {
            return var7.get(0);
         } else if (var7.isEmpty()) {
            return JsApi.next(JsApi.notFound("no folder or request named '" + var1 + "' in " + var2), "Postman.read('" + var2 + "')");
         } else {
            var var8 = var7.stream().map(var0x -> io.karatelabs.ext.postman.PostmanCollectionTreeMapper.a(var0x) + " (" + (var0x.b() ? "request" : "folder") + ")").toList();
            var var6 = JsApi.missing(
               "Postman.migrate: " + var7.size() + " items are named '" + var1 + "': pass one's pointer as item — " + String.join(", ", var8)
            );
            var6.put("code", "AMBIGUOUS");
            var6.put("matches", var8);
            return var6;
         }
      } else {
         io.karatelabs.ext.postman.PostmanCollectionNode var3 = io.karatelabs.ext.postman.PostmanCollectionTreeMapper.a(var0, var1);
         return var3 != null ? var3 : JsApi.next(JsApi.notFound("no item " + var1 + " in " + var2), "Postman.read('" + var2 + "')");
      }
   }

   static String shape(Object var0) {
      return var0 == null
         ? "nothing"
         : (
            var0 instanceof Map
               ? "an object"
               : (
                  var0 instanceof List
                     ? "a list"
                     : (var0 instanceof Number ? "a number" : (var0 instanceof Boolean ? "a boolean" : "a " + var0.getClass().getSimpleName()))
               )
         );
   }

   Object data(String var1, Object var2) {
      Object var3 = this.collection("Postman.data", var1);
      if (var3 instanceof Map var15) {
         return var15;
      } else {
         io.karatelabs.ext.max.jsapi.Postman.LoadedCollection var4 = (io.karatelabs.ext.max.jsapi.Postman.LoadedCollection)var3;
         Map<java.lang.String, Object> var5 = var2 instanceof Map var6 ? var6 : Map.of();
         String var16 = var5.get("item") == null ? "" : String.valueOf(var5.get("item"));
         io.karatelabs.ext.postman.PostmanCollectionNode var7 = io.karatelabs.ext.postman.PostmanCollectionTreeMapper.a(var4.a());
         io.karatelabs.ext.postman.PostmanCollectionNode var8 = io.karatelabs.ext.postman.PostmanCollectionTreeMapper.a(var7, var16);
         if (var8 == null) {
            return JsApi.next(JsApi.notFound("no item " + var16 + " in " + var1), "Postman.read('" + var1 + "')");
         } else if (var5.get("text") != null) {
            return this.writeData(var4, var8, var5);
         } else {
            LinkedHashSet var9 = new LinkedHashSet();
            variables(var8.b.value(), var9);
            LinkedHashSet var10 = new LinkedHashSet();
            scriptSets(var7.b.value(), var10);

            Map var11;
            try {
               ConsoleState.Kickoff var12 = this.console.at(null);
               var11 = ConfigLoader.configOnly(this.root, var12.env(), null, var12.overlay());
            } catch (Exception var14) {
               var11 = Map.of();
            }

            Map<java.lang.Object, Object> var17 = var11;
            var9.removeIf(
               var2x -> var10.contains(var2x)
                  || var17.get(var2x) instanceof String
                  || var17.get(var2x) instanceof Number
                  || var17.get(var2x) instanceof Boolean
            );
            LinkedHashMap<java.lang.String, java.lang.Object> var13 = new LinkedHashMap();
            var13.put("item", var16);
            var13.put("columns", new ArrayList(var9));
            var13.put("to", this.dataTo(var4, var8));
            var13.put("files", this.dataFiles(var4));
            return var13;
         }
      }
   }

   private Object writeData(io.karatelabs.ext.max.jsapi.Postman.LoadedCollection var1, io.karatelabs.ext.postman.PostmanCollectionNode var2, Map<?, ?> var3) {
      if (this.noProject) {
         return JsApi.noProjectSelected("Postman.data");
      } else if (var3.get("text") instanceof String var4) {
         String var14 = var3.get("to") == null ? this.dataTo(var1, var2) : String.valueOf(var3.get("to")).replaceFirst("^/+", "");
         String var6 = var14.toLowerCase();
         if (!var6.endsWith(".csv") && !var6.endsWith(".json")) {
            return JsApi.missing("Postman.data: to is a project-relative .csv or .json path, got " + var14);
         } else {
            Path var7;
            try {
               var7 = MaxPaths.resolveSandboxed(this.root, var14);
            } catch (SecurityException var13) {
               return JsApi.missing("Postman.data denied: " + var13.getMessage());
            }

            List var8;
            try {
               var8 = io.karatelabs.ext.postman.PostmanDataFileParser.a(var4, var14, var14);
            } catch (io.karatelabs.ext.postman.PostmanDataFileParser.PostmanDataFileException var12) {
               var var10 = JsApi.missing("Postman.data: " + var12.getMessage());
               var10.put("code", "BAD_DATA");
               return var10;
            }

            byte[] var9 = var4.getBytes(StandardCharsets.UTF_8);
            LinkedHashMap<java.lang.String, java.lang.Object> var15 = new LinkedHashMap();
            var15.put("path", this.rel(var7));
            var var11 = FeatureStore.write(var7, var9, null);
            if (var11 != null) {
               var15.putAll(var11);
               return var15;
            } else {
               var15.put("version", io.karatelabs.ext.common.Sha256.a(var9));
               var15.put("rows", var8.size());
               var15.put("columns", new ArrayList(((Map)var8.get(0)).keySet()));
               return var15;
            }
         }
      } else {
         return JsApi.missing("Postman.data: text is the data file's content, a CSV with a header row or a JSON array of objects");
      }
   }

   private String dataTo(io.karatelabs.ext.max.jsapi.Postman.LoadedCollection var1, io.karatelabs.ext.postman.PostmanCollectionNode var2) {
      int var3 = var1.b().lastIndexOf(47);
      String var4 = var3 < 0 ? "" : var1.b().substring(0, var3 + 1);
      String var5 = !var2.a() && var2.d() != null ? var2.d() : "";
      String var6 = var5.toLowerCase().replaceAll("[^a-z0-9]+", "-").replaceAll("^-+|-+$", "");
      String var7 = var4 + (var6.isEmpty() ? "data" : var6);
      String var8 = var7 + ".csv";

      for (int var9 = 2; Files.exists(this.root.resolve(var8)); var9++) {
         var8 = var7 + "-" + var9 + ".csv";
      }

      return var8;
   }

   private List<String> dataFiles(io.karatelabs.ext.max.jsapi.Postman.LoadedCollection var1) {
      int var2 = var1.b().lastIndexOf(47);
      String var3 = var2 < 0 ? "" : var1.b().substring(0, var2 + 1);
      ArrayList<java.lang.String> var4 = new ArrayList();

      try {
         File.walkPruned(
            this.root,
            File.GENERATED_DIRS,
            var3x -> {
               String var4x = var3x.toLowerCase();
               int var5 = var3x.indexOf(47);
               if ((var5 < 0 || !NOT_DATA_HOMES.contains(var3x.substring(0, var5)))
                  && (var3x.startsWith(var3) || var3x.startsWith("data/") || var3x.contains("/data/"))) {
                  if (var4x.endsWith(".csv") || var4x.endsWith(".json") && this.rowsOfObjects(var3x)) {
                     var4.add(var3x);
                  }
               }
            }
         );
      } catch (IOException var6) {
         return var4;
      }

      var4.sort(null);
      return var4;
   }

   private boolean rowsOfObjects(String var1) {
      try {
         Path var2 = this.root.resolve(var1);
         if (Files.size(var2) <= 1048576L && !var1.endsWith(".postman_collection.json") && !var1.endsWith(".postman_environment.json")) {
            for (Map var4 : io.karatelabs.ext.postman.PostmanDataFileParser.a(var2, var1)) {
               if (var4.values().stream().anyMatch(var0 -> var0 instanceof Map || var0 instanceof List)) {
                  return false;
               }
            }

            return true;
         } else {
            return false;
         }
      } catch (io.karatelabs.ext.postman.PostmanDataFileParser.PostmanDataFileException | IOException var5) {
         return false;
      }
   }

   private static void scriptSets(Object var0, Set<String> var1) {
      if (var0 instanceof String var2) {
         Matcher var5 = SET_BY_SCRIPT.matcher(var2);

         while (var5.find()) {
            var1.add(var5.group(1).trim());
         }
      } else if (var0 instanceof Map var3) {
         var3.values().forEach(var1x -> scriptSets(var1x, var1));
      } else if (var0 instanceof List var4) {
         var4.forEach(var1x -> scriptSets(var1x, var1));
      }
   }

   private Object file(String var1, String var2) {
      if (var2 != null && !var2.isBlank()) {
         Path var3;
         try {
            var3 = MaxPaths.resolveSandboxed(this.root, var2);
         } catch (SecurityException var5) {
            return JsApi.missing(var1 + " denied: " + var5.getMessage());
         }

         return !Files.isRegularFile(var3) ? JsApi.notFound("no collection '" + var2 + "' — File.list() shows what is there") : var3;
      } else {
         return JsApi.missing(var1 + "(path, …) needs a project-relative collection path");
      }
   }

   private Object collection(String var1, String var2) {
      Object var3 = this.file(var1, var2);
      if (var3 instanceof Map var7) {
         return var7;
      } else {
         io.karatelabs.ext.postman.editor.PostmanCollectionDocument var4;
         try {
            var4 = io.karatelabs.ext.postman.editor.PostmanCollectionDocument.a((Path)var3);
         } catch (IOException var6) {
            return JsApi.notFound(var1 + " could not read '" + var2 + "': " + var6.getMessage());
         }

         if (var4.b(((Path)var3).getFileName().toString()) != null) {
            var var5 = JsApi.missing("'" + var2 + "' is not a Postman collection (a JSON object with info and item)");
            var5.put("code", "NOT_COLLECTION");
            var5.put("status", 422);
            return var5;
         } else {
            return new io.karatelabs.ext.max.jsapi.Postman.LoadedCollection(var4, this.rel((Path)var3));
         }
      }
   }

   private String rel(Path var1) {
      return this.root.relativize(var1).toString().replace('\\', '/');
   }

   public record Migrated(String to, byte[] bytes, Map<String, Object> answer) {

   }

   private record LoadedCollection(io.karatelabs.ext.postman.editor.PostmanCollectionDocument a, String b) {

   }
}
