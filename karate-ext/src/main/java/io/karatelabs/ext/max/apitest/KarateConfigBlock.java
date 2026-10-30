package io.karatelabs.ext.max.apitest;

import io.karatelabs.common.Json;
import io.karatelabs.common.Resource;
import io.karatelabs.ext.common.Sha256;
import io.karatelabs.parser.JsParser;
import io.karatelabs.parser.Node;
import io.karatelabs.parser.NodeType;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Map.Entry;
import java.util.regex.Pattern;

public final class KarateConfigBlock {
   static final String BEGIN = "// >>> ApiTest.config: generated, change it only with ApiTest.config()";
   static final String MODEL = "// model: ";
   static final String END = "// <<< ApiTest.config sha256=";
   static final String NEW_FILE = "function fn() {\n  return {};\n}\n";
   private static final Pattern IDENT = Pattern.compile("[A-Za-z_$][A-Za-z0-9_$]*");
   private static final Pattern ENV = Pattern.compile("[A-Za-z0-9_-]+");
   private static final Pattern SECRET = Pattern.compile("[A-Za-z_][A-Za-z0-9_]*");
   static final Map<String, List<String>> SCHEMES = new LinkedHashMap<>();
   static final Map<String, List<String>> ONE_OF = Map.of("bearer", List.of("token", "login"), "apikey", List.of("header", "query"));
   public static final List<String> CREDENTIALS = List.of("token", "password", "key", "clientSecret");
   private static final Pattern LOGIN = Pattern.compile("(?!/)(?!.*(^|/)\\.\\.(/|$))[A-Za-z0-9_./-]+\\.feature");
   private static final Pattern CREDENTIAL_NAME = Pattern.compile("(?i).*(token|password|passwd|secret|key|credential).*");

   private KarateConfigBlock() {
   }

   public static KarateConfigBlock.State read(String var0) {
      if (var0 == null) {
         return new KarateConfigBlock.State(null, List.of(), false);
      } else {
         Object var1 = locate(var0);
         if (var1 instanceof Map var4) {
            return new KarateConfigBlock.State(cast(var4), null, false);
         } else if (var1 == null) {
            return new KarateConfigBlock.State(null, List.of(), false);
         } else {
            io.karatelabs.ext.max.apitest.KarateConfigBlock.GeneratedConfigBlock var2 = (io.karatelabs.ext.max.apitest.KarateConfigBlock.GeneratedConfigBlock)var1;
            var var3 = model(var2.b());
            return var3 == null
               ? new KarateConfigBlock.State(handEdited("its model line does not parse"), null, false)
               : new KarateConfigBlock.State(null, var3, true);
         }
      }
   }

   public static KarateConfigBlock.Result apply(String var0, Map<String, Object> var1) {
      KarateConfigBlock.State var2 = read(var0);
      if (var2.error() != null) {
         return new KarateConfigBlock.Result(var2.error(), null, null);
      } else {
         String var3 = var0 != null && var0.contains("\r\n") ? "\r\n" : "\n";
         String var4;
         if (var0 == null) {
            var4 = "function fn() {\n  return {};\n}\n".replace("\n", var3);
         } else if (var2.present()) {
            var4 = var0.substring(0, ((io.karatelabs.ext.max.apitest.KarateConfigBlock.GeneratedConfigBlock)locate(var0)).a());
         } else {
            var4 = !var0.isEmpty() && !var0.endsWith("\n") ? var0 + var3 : var0;
         }

         String var5 = checkHead(var4);
         if (var5 != null) {
            return new KarateConfigBlock.Result(
               refuse(
                  "NOT_EDITABLE",
                  409,
                  "karate-config.js "
                     + var5
                     + " — ApiTest.config needs the file to declare `function fn() {…}` (helper functions allowed) and nothing else outside its block",
                  null
               ),
               null,
               null
            );
         } else {
            var var6 = new ArrayList<>(var2.entries());
            var var7 = change(var6, var1);
            if (var7 != null) {
               return new KarateConfigBlock.Result(var7, null, null);
            } else {
               String var8 = render(var6, var3);
               String var9 = !var2.present() && !var4.isEmpty() && !var4.endsWith(var3 + var3) ? var3 : "";
               String var10 = var4 + var9 + var8;
               return checkParses(var10) != null
                  ? new KarateConfigBlock.Result(
                     refuse("UNINTENDED_CHANGE", 409, "ApiTest.config wrote nothing — the generated file does not parse: " + checkParses(var10), null),
                     null,
                     null
                  )
                  : new KarateConfigBlock.Result(null, var10, var6);
            }
         }
      }
   }

   private static Map<String, Object> change(List<Map<String, Object>> var0, Map<String, Object> var1) {
      Object var2 = var1.get("env");
      String var3 = var2 == null ? null : String.valueOf(var2);
      if (var3 != null && !ENV.matcher(var3).matches()) {
         return bad("env '" + var3 + "' must match " + ENV.pattern());
      } else if (var1.containsKey("remove")) {
         String var10 = String.valueOf(var1.get("remove"));
         int var13 = indexOf(var0, var10, var3);
         if (var13 < 0) {
            return refuse(
               "NOT_FOUND",
               404,
               "no config entry '" + var10 + "'" + (var3 == null ? "" : " for env '" + var3 + "'") + " — ApiTest.config() lists the entries",
               null
            );
         } else {
            var0.remove(var13);
            return null;
         }
      } else {
         LinkedHashMap var4 = new LinkedHashMap();
         if (var1.containsKey("auth")) {
            if (var1.containsKey("name") || var1.containsKey("value") || var1.containsKey("secret")) {
               return bad("auth is its own change — pass {auth, env?} alone");
            }

            if (!(var1.get("auth") instanceof Map var5)) {
               return bad("auth is {scheme, …fields} with scheme one of " + SCHEMES.keySet());
            }

            Object var14 = auth(cast(var5));
            if (var14 instanceof String var16) {
               return bad(var16);
            }

            if (var14 instanceof Map var7 && var7.containsKey("error")) {
               return cast(var7);
            }

            var4.put("name", "auth");
            putEnv(var4, var3);
            var4.put("auth", var14);
         } else {
            Object var11 = var1.get("name");
            String var15 = var11 == null ? null : String.valueOf(var11);
            if (var15 == null || !IDENT.matcher(var15).matches()) {
               return bad("name must be a JS identifier, got " + var15);
            }

            if ("auth".equals(var15)) {
               return bad("'auth' is set with {auth: {scheme, …}}");
            }

            boolean var17 = var1.containsKey("value");
            boolean var8 = var1.containsKey("secret");
            if (var17 == var8) {
               return bad("pass exactly one of value or secret");
            }

            var4.put("name", var15);
            putEnv(var4, var3);
            if (var8) {
               String var9 = String.valueOf(var1.get("secret"));
               if (!SECRET.matcher(var9).matches()) {
                  return bad("secret names an environment variable matching " + SECRET.pattern() + ", got " + var9);
               }

               var4.put("secret", var9);
            } else {
               Object var18 = scalar(var1.get("value"));
               if (var18 == null) {
                  return bad("value is a string, number or boolean on one line — a credential goes in as {secret}");
               }

               var4.put("value", var18);
            }
         }

         int var12 = indexOf(var0, (String)var4.get("name"), var3);
         if (var12 >= 0) {
            var0.set(var12, var4);
         } else {
            var0.add(var4);
         }

         return null;
      }
   }

   private static Object auth(Map<String, Object> var0) {
      Object var1 = var0.get("scheme");
      List var2 = var1 == null ? null : SCHEMES.get(String.valueOf(var1));
      if (var2 == null) {
         return "auth.scheme is one of " + SCHEMES.keySet() + ", got " + var1;
      } else {
         LinkedHashMap var3 = new LinkedHashMap();
         var3.put("scheme", var1);

         for (String var5 : var0.keySet()) {
            if (!"scheme".equals(var5) && !var2.contains(var5)) {
               return "auth " + var1 + " takes " + var2 + ", not " + var5;
            }
         }

         var var11 = ONE_OF.get(String.valueOf(var1));
         if (var11 != null && var11.stream().filter(var0::containsKey).count() != 1L) {
            return "auth " + var1 + " takes exactly one of " + var11;
         } else {
            for (String var6 : (Iterable<String>) (Iterable<?>) (var2)) {
               Object var7 = var0.get(var6);
               if (var7 == null) {
                  if (!"scope".equals(var6) && (var11 == null || !var11.contains(var6))) {
                     return "auth " + var1 + " needs " + var6;
                  }
               } else if ("login".equals(var6)) {
                  if (!(var7 instanceof String var14 && LOGIN.matcher(var14).matches())) {
                     return "auth.login is a project-relative .feature path, got " + var7;
                  }

                  var3.put(var6, var14);
               } else if (var7 instanceof Map var9) {
                  Object var13 = var9.get("secret");
                  if (var9.size() != 1 || var13 == null || !SECRET.matcher(String.valueOf(var13)).matches()) {
                     return "auth." + var6 + " is a string or {secret: '<ENV_VAR>'}";
                  }

                  var3.put(var6, Map.of("secret", String.valueOf(var13)));
               } else {
                  if (CREDENTIALS.contains(var6)) {
                     var var10 = bad("auth." + var6 + " is a credential — pass {secret: '<ENV_VAR>'}, never its value");
                     var10.put("next", List.of("ApiTest.config({auth: {scheme: '" + var1 + "', …, " + var6 + ": {secret: '<ENV_VAR>'}}, env, ifMatch})"));
                     return var10;
                  }

                  if (!(var7 instanceof String var8 && scalar(var8) != null)) {
                     return "auth." + var6 + " is a string or {secret: '<ENV_VAR>'}";
                  }

                  var3.put(var6, var8);
               }
            }

            return var3;
         }
      }
   }

   private static Object scalar(Object var0) {
      if (var0 instanceof Boolean) {
         return var0;
      } else if (var0 instanceof Number var4) {
         double var5 = var4.doubleValue();
         return Double.isFinite(var5) ? (var5 == Math.rint(var5) && Math.abs(var5) < 1.0E15 ? (long)var5 : var5) : null;
      } else if (!(var0 instanceof String var1)) {
         return null;
      } else {
         for (int var2 = 0; var2 < var1.length(); var2++) {
            char var3 = var1.charAt(var2);
            if (var3 < ' ' || var3 == 127 || var3 == 8232 || var3 == 8233) {
               return null;
            }
         }

         return var1;
      }
   }

   private static int indexOf(List<Map<String, Object>> var0, String var1, String var2) {
      for (int var3 = 0; var3 < var0.size(); var3++) {
         Map var4 = (Map)var0.get(var3);
         if (var1.equals(var4.get("name")) && Objects.equals(var2, var4.get("env"))) {
            return var3;
         }
      }

      return -1;
   }

   private static void putEnv(Map<String, Object> var0, String var1) {
      if (var1 != null) {
         var0.put("env", var1);
      }
   }

   static String render(List<Map<String, Object>> var0, String var1) {
      ArrayList var2 = new ArrayList();
      var2.add("// model: " + Json.stringifyStrict(var0));
      var2.add("var apiTestConfig = function (config, secret) {");
      var2.add("  config = config || {};");
      boolean var3 = false;

      for (boolean var7 : new boolean[]{false, true}) {
         for (boolean var11 : new boolean[]{false, true}) {
            for (Map var13 : var0) {
               Object var14 = var13.get("env");
               if (var14 != null == var11 && var13.containsKey("auth") == var7) {
                  var3 |= var7;
                  String var15 = "config." + var13.get("name") + " = " + literal(var13) + ";";
                  var2.add("  " + (var14 == null ? var15 : "if (karate.env == " + quote(String.valueOf(var14)) + ") " + var15));
               }
            }
         }
      }

      if (var3) {
         var2.add("  var a = config.auth || {};");
         var2.add("  if (a.scheme == 'bearer' && a.token) karate.configure('auth', { type: 'bearer', token: a.token });");
         var2.add(
            "  if (a.scheme == 'basic' && a.username && a.password) karate.configure('auth', { type: 'basic', username: a.username, password: a.password });"
         );
         var2.add(
            "  if (a.scheme == 'oauth2' && a.clientSecret) karate.configure('auth', a.scope ? { type: 'oauth2', accessTokenUrl: a.tokenUrl, clientId: a.clientId, clientSecret: a.clientSecret, scope: a.scope } : { type: 'oauth2', accessTokenUrl: a.tokenUrl, clientId: a.clientId, clientSecret: a.clientSecret });"
         );
      }

      var2.add("  return config;");
      var2.add("};");
      var2.add("apiTestConfig(fn(), function (name) { return karate.sysenv(name); });");
      String var16 = String.join(var1, var2) + var1;
      return "// >>> ApiTest.config: generated, change it only with ApiTest.config()" + var1 + var16 + "// <<< ApiTest.config sha256=" + io.karatelabs.ext.common.Sha256.a(var16) + var1;
   }

   private static String literal(Map<String, Object> var0) {
      if (var0.containsKey("secret")) {
         return "secret(" + quote(String.valueOf(var0.get("secret"))) + ")";
      } else if (var0.containsKey("auth")) {
         ArrayList var7 = new ArrayList();

         for (Entry var3 : cast((Map<?, ?>)var0.get("auth")).entrySet()) {
            Object var4 = var3.getValue();
            String var5 = var4 instanceof Map var6 ? "secret(" + quote(String.valueOf(var6.get("secret"))) + ")" : quote(String.valueOf(var4));
            var7.add((String)var3.getKey() + ": " + var5);
            if ("login".equals(var3.getKey())) {
               var7.add("token: karate.callSingle(" + quote("/" + var4) + ", config).token");
            }
         }

         return "{ " + String.join(", ", var7) + " }";
      } else {
         Object var1 = var0.get("value");
         return var1 instanceof String var2 ? quote(var2) : String.valueOf(var1);
      }
   }

   private static String quote(String var0) {
      return "'" + var0.replace("\\", "\\\\").replace("'", "\\'") + "'";
   }

   private static Object locate(String var0) {
      int var1 = lineStartOf(
         var0,
         "// >>> ApiTest.config: generated, change it only with ApiTest.config()"
            .substring(0, "// >>> ApiTest.config: generated, change it only with ApiTest.config()".indexOf(58))
      );
      int var2 = lineStartOf(var0, "// <<< ApiTest.config sha256=".substring(0, "// <<< ApiTest.config sha256=".indexOf(" sha256")));
      if (var1 < 0 && var2 < 0) {
         return null;
      } else if (var1 >= 0
         && var2 >= 0
         && var2 >= var1
         && lineStartOf(
               var0.substring(var1 + 1),
               "// >>> ApiTest.config: generated, change it only with ApiTest.config()"
                  .substring(0, "// >>> ApiTest.config: generated, change it only with ApiTest.config()".indexOf(58))
            )
            < 0
         && lineStartOf(var0.substring(var2 + 1), "// <<< ApiTest.config sha256=".substring(0, "// <<< ApiTest.config sha256=".indexOf(" sha256"))) < 0) {
         int var3 = var0.indexOf(10, var1) + 1;
         int var4 = var0.indexOf(10, var2);
         String var5 = (var4 < 0 ? var0.substring(var2) : var0.substring(var2, var4)).strip();
         if (!var5.startsWith("// <<< ApiTest.config sha256=")) {
            return handEdited("its end marker lost its hash");
         } else {
            String var6 = var0.substring(var3, var2);
            if (!io.karatelabs.ext.common.Sha256.a(var6).equals(var5.substring("// <<< ApiTest.config sha256=".length()))) {
               return handEdited("its body no longer matches its hash");
            } else {
               return var4 >= 0 && !var0.substring(var4 + 1).isBlank()
                  ? handEdited("code follows it — the block must stay last in the file")
                  : new io.karatelabs.ext.max.apitest.KarateConfigBlock.GeneratedConfigBlock(var1, var6);
            }
         }
      } else {
         return handEdited("its markers are missing, repeated or out of order");
      }
   }

   private static int lineStartOf(String var0, String var1) {
      int var2 = 0;

      while (true) {
         int var3 = var0.indexOf(var1, var2);
         if (var3 < 0) {
            return -1;
         }

         int var4 = var0.lastIndexOf(10, var3 - 1) + 1;
         if (var0.substring(var4, var3).isBlank()) {
            return var4;
         }

         var2 = var3 + 1;
      }
   }

   private static List<Map<String, Object>> model(String var0) {
      int var1 = var0.indexOf(10);
      String var2 = (var1 < 0 ? var0 : var0.substring(0, var1)).strip();
      if (!var2.startsWith("// model: ".strip())) {
         return null;
      } else {
         try {
            if (Json.parseStrict(var2.substring("// model: ".strip().length()).strip(), true) instanceof List var4) {
               ArrayList var5 = new ArrayList();

               for (Object var7 : var4) {
                  if (!(var7 instanceof Map var8)) {
                     return null;
                  }

                  var5.add(new LinkedHashMap<>(cast(var8)));
               }

               return var5;
            } else {
               return null;
            }
         } catch (RuntimeException var9) {
            return null;
         }
      }
   }

   public static boolean credentialNamed(String var0) {
      return var0 != null && CREDENTIAL_NAME.matcher(var0).matches();
   }

   public static List<Map<String, Object>> handWritten(String var0) {
      if (var0 != null && !var0.isBlank()) {
         String var2 = locate(var0) instanceof io.karatelabs.ext.max.apitest.KarateConfigBlock.GeneratedConfigBlock var3 ? var0.substring(0, var3.a()) : var0;

         Node var10;
         try {
            var10 = new JsParser(Resource.text(var2)).parse();
         } catch (RuntimeException var9) {
            return List.of();
         }

         LinkedHashMap var4 = new LinkedHashMap();

         for (Node var6 : var10.findAll(NodeType.LIT_OBJECT)) {
            if (takenWhole(var6)) {
               for (Node var8 : var6.findImmediateChildren(NodeType.OBJECT_ELEM)) {
                  if (var8.size() >= 3 && ":".equals(var8.get(1).getText())) {
                     put(var4, unquote(var8.get(0).getText()), var8.get(2));
                  }
               }
            }
         }

         for (Node var12 : var10.findAll(NodeType.ASSIGN_EXPR)) {
            if (var12.size() >= 3 && var12.get(0).type == NodeType.REF_DOT_EXPR && "=".equals(var12.get(1).getText())) {
               Node var13 = var12.get(0);
               put(var4, var13.get(var13.size() - 1).getText(), var12.get(2));
            }
         }

         return new ArrayList<>(var4.values());
      } else {
         return List.of();
      }
   }

   private static boolean takenWhole(Node var0) {
      Node var1 = var0.getParent();

      while (var1 != null) {
         switch (var1.type) {
            case VAR_DECL:
            case RETURN_STMT:
               return true;
            case ASSIGN_EXPR:
               return var1.get(0).type != NodeType.REF_DOT_EXPR;
            case LIT_OBJECT:
            case FN_CALL_EXPR:
            case FN_CALL_ARGS:
            case LIT_ARRAY:
               return false;
            default:
               var1 = var1.getParent();
         }
      }

      return false;
   }

   private static void put(Map<String, Map<String, Object>> var0, String var1, Node var2) {
      if (IDENT.matcher(var1).matches()) {
         LinkedHashMap var3 = new LinkedHashMap();
         var3.put("name", var1);
         Node var4 = var2;

         while (var4.type == NodeType.EXPR && var4.size() == 1) {
            var4 = var4.get(0);
         }

         boolean var5 = var4.type == NodeType.LIT_EXPR || var4.isToken() && var4.getText().matches("'.*'|\".*\"|-?[0-9.]+|true|false");
         if (var5 && !CREDENTIAL_NAME.matcher(var1).matches()) {
            var3.put("value", unquote(var4.getText()));
         }

         var0.put(var1, var3);
      }
   }

   private static String unquote(String var0) {
      String var1 = var0.strip();
      return var1.length() < 2 || (!var1.startsWith("'") || !var1.endsWith("'")) && (!var1.startsWith("\"") || !var1.endsWith("\""))
         ? var1
         : var1.substring(1, var1.length() - 1);
   }

   static String checkHead(String var0) {
      if (var0.isBlank()) {
         return "is empty";
      } else {
         Node var1;
         try {
            var1 = new JsParser(Resource.text(var0)).parse();
         } catch (RuntimeException var6) {
            return "does not parse: " + var6.getMessage();
         }

         boolean var2 = false;

         for (int var3 = 0; var3 < var1.size(); var3++) {
            Node var4 = var1.get(var3);
            if (var4.type == NodeType.STATEMENT) {
               Node var5 = var4.get(0);
               if (var5.type != NodeType.EOS) {
                  if (var5.type != NodeType.FN_EXPR || var5.size() < 2 || !var5.get(1).isToken()) {
                     return "has a top-level statement that is not a function declaration";
                  }

                  var2 |= "fn".equals(var5.get(1).getText());
               }
            }
         }

         return var2 ? null : "declares no `function fn()`";
      }
   }

   private static String checkParses(String var0) {
      try {
         new JsParser(Resource.text(var0)).parse();
         return null;
      } catch (RuntimeException var2) {
         return var2.getMessage();
      }
   }

   private static Map<String, Object> handEdited(String var0) {
      return refuse(
         "HAND_EDITED",
         409,
         "the ApiTest.config block in karate-config.js was edited by hand ("
            + var0
            + ") — restore it from version control, and keep hand-written config above the block",
         null
      );
   }

   private static Map<String, Object> bad(String var0) {
      return refuse("BAD_REQUEST", 400, "ApiTest.config: " + var0, null);
   }

   static Map<String, Object> refuse(String var0, int var1, String var2, Map<String, Object> var3) {
      LinkedHashMap var4 = new LinkedHashMap();
      var4.put("error", var2);
      var4.put("code", var0);
      var4.put("status", var1);
      if (var3 != null) {
         var4.put("detail", var3);
      }

      return var4;
   }

   private static Map<String, Object> cast(Map<?, ?> var0) {
      return (Map<String, Object>)(Map<?, ?>)var0;
   }

   static {
      SCHEMES.put("bearer", List.of("token", "login"));
      SCHEMES.put("basic", List.of("username", "password"));
      SCHEMES.put("apikey", List.of("header", "query", "key"));
      SCHEMES.put("oauth2", List.of("tokenUrl", "clientId", "clientSecret", "scope"));
   }

   public record Result(Map<String, Object> error, String text, List<Map<String, Object>> entries) {

   }

   public record State(Map<String, Object> error, List<Map<String, Object>> entries, boolean present) {

   }

   private record GeneratedConfigBlock(int a, String b) {

   }
}
