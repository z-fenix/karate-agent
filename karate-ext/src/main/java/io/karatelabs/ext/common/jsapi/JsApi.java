package io.karatelabs.ext.common.jsapi;

import io.karatelabs.js.EngineException;
import io.karatelabs.js.SimpleObject;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class JsApi {
   private static final Pattern NOT_DEFINED = Pattern.compile("([A-Za-z_$][\\w$]*) is not defined");
   private static final Pattern HELP_VERB = Pattern.compile("^(\\s*)- `[A-Za-z]+\\.([a-z][A-Za-z]*)");
   public static final String PARTS_KEY = "parts";

   private JsApi() {
   }

   public static Object arg(Object[] var0, int var1) {
      return var0 != null && var1 < var0.length ? var0[var1] : null;
   }

   public static String str(Object[] var0, int var1) {
      Object var2 = arg(var0, var1);
      return var2 == null ? null : String.valueOf(var2);
   }

   public static Map<String, Object> error(RuntimeException var0) {
      LinkedHashMap<java.lang.String, java.lang.Object> var1 = new LinkedHashMap();
      JsWireException var2 = wireErrorIn(var0);
      if (var2 != null) {
         var1.put("error", var2.a());
         var1.put("code", var2.b());
         var1.put("status", var2.c());
         if (var2.d() != null) {
            var1.put("detail", var2.d());
         }
      } else {
         var1.put("error", jsErrorMessage(var0));
         String var3 = jsErrorName(var0);
         var1.put("code", var3 != null ? var3 : "INTERNAL");
         var1.put("status", 500);
      }

      return var1;
   }

   private static JsWireException wireErrorIn(Throwable var0) {
      Throwable var1 = var0;

      for (int var2 = 0; var1 != null && var2 < 10; var2++) {
         if (var1 instanceof JsWireException var3) {
            return var3;
         }

         if (var1.getCause() == var1) {
            break;
         }

         var1 = var1.getCause();
      }

      return null;
   }

   public static String jsErrorMessage(Throwable var0) {
      if (var0 == null) {
         return "unknown error";
      } else if (var0 instanceof EngineException var1 && var1.getJsMessage() != null) {
         return var1.getJsMessage();
      } else {
         return var0.getMessage() == null ? var0.getClass().getSimpleName() : var0.getMessage();
      }
   }

   public static String jsErrorName(Throwable var0) {
      return var0 instanceof EngineException var1 ? var1.getJsErrorName() : null;
   }

   public static String notDefinedIdentifier(String var0) {
      if (var0 == null) {
         return null;
      } else {
         Matcher var1 = NOT_DEFINED.matcher(var0);
         return var1.find() ? var1.group(1) : null;
      }
   }

   public static Map<String, Object> missing(String var0) {
      return envelope(var0, "BAD_REQUEST", 400);
   }

   public static Map<String, Object> notFound(String var0) {
      return envelope(var0, "NOT_FOUND", 404);
   }

   public static Map<String, Object> noProjectSelected(String var0) {
      return envelope(
         var0
            + " needs a project — none is selected. Create one first: Work.seed('demo') (the worked demo), Work.create('<name>') (an empty folder), or Work.use('<name>') to pick an existing project under the /work ceiling. Nothing is written to the bare ceiling.",
         "NO_PROJECT",
         409
      );
   }

   private static Map<String, Object> envelope(String var0, String var1, int var2) {
      LinkedHashMap<java.lang.String, java.lang.Object> var3 = new LinkedHashMap();
      var3.put("error", var0);
      var3.put("code", var1);
      var3.put("status", var2);
      return var3;
   }

   public static Map<String, Object> suggest(Map<String, Object> var0, List<String> var1) {
      if (var0 != null && var1 != null && !var1.isEmpty()) {
         var0.put("suggestions", var1);
      }

      return var0;
   }

   public static Map<String, Object> hint(Map<String, Object> var0, String var1) {
      if (var0 != null && var1 != null && !var1.isBlank()) {
         var0.put("hint", var1);
      }

      return var0;
   }

   public static Object toWire(Object var0) {
      if (var0 instanceof SimpleObject var6) {
         return toWire(var6.toMap());
      } else if (var0 instanceof Map var5) {
         LinkedHashMap<java.lang.String, java.lang.Object> var7 = new LinkedHashMap();
         var5.forEach((var1x, var2x) -> var7.put(String.valueOf(var1x), toWire(var2x)));
         return var7;
      } else if (!(var0 instanceof List var1)) {
         return var0;
      } else {
         ArrayList<java.lang.Object> var2 = new ArrayList(var1.size());

         for (Object var4 : var1) {
            var2.add(toWire(var4));
         }

         return var2;
      }
   }

   public static Map<String, Object> handleRef(Object var0) {
      if (!(var0 instanceof SimpleObject)) {
         return null;
      } else {
         LinkedHashMap<java.lang.String, java.lang.Object> var1 = new LinkedHashMap();
         if (var0 instanceof JsDispatch var2) {
            if (var2.jsKind() != null) {
               var1.put("kind", var2.jsKind());
            }

            var var3 = var2.jsVerbs();
            if (var3 != null && !var3.isEmpty()) {
               var1.put("verbs", new ArrayList(var3));
            }
         }

         return var1;
      }
   }

   public static Map<String, Object> next(Map<String, Object> var0, String... var1) {
      if (var0 != null && var1 != null && var1.length > 0) {
         var0.put("next", List.of(var1));
      }

      return var0;
   }

   public static Map<String, Object> dataError(io.karatelabs.ext.common.jsapi.CoverageGraphAccessor.CoverageGraphLoadResult var0, Path var1, String var2) {
      return var0.b() == io.karatelabs.ext.common.jsapi.CoverageGraphAccessor.CoverageGraphStatus.UNREADABLE
         ? envelope("coverage data at " + var1 + " is unreadable (corrupt JSON): " + var0.d(), "UNREADABLE", 422)
         : envelope("no coverage data at " + var1 + " — " + var2, "NOT_FOUND", 404);
   }

   public static Map<String, Object> applyOutputConvention(Map<String, Object> var0, String var1, Object var2) {
      return applyOutputConvention(var0, () -> var1, var2);
   }

   public static Map<String, Object> applyOutputConvention(Map<String, Object> var0, Supplier<String> var1, Object var2) {
      String var3 = null;
      if (var2 instanceof Map var4 && var4.get("output") != null) {
         var3 = String.valueOf(var4.get("output"));
      }

      if ("markdown".equals(var3)) {
         LinkedHashMap<java.lang.String, java.lang.Object> var6 = new LinkedHashMap();
         var6.put("markdown", var1.get());
         return var6;
      } else if ("both".equals(var3)) {
         var var5 = new LinkedHashMap<>(var0);
         var5.put("markdown", var1.get());
         return var5;
      } else {
         return var0;
      }
   }

   public static Map<String, Object> markdown(String var0) {
      LinkedHashMap<java.lang.String, java.lang.Object> var1 = new LinkedHashMap();
      var1.put("markdown", var0);
      return var1;
   }

   public static Object helpTopic(Object var0, Object[] var1) {
      String var2 = str(var1, 0);
      if (var2 != null && !var2.isBlank() && var0 instanceof Map var3 && var3.get("markdown") instanceof String var4) {
         Pattern var15 = Pattern.compile("^\\s*- `[A-Za-z]+\\." + Pattern.quote(var2.trim()) + "(?![A-Za-z0-9_])");
         StringBuilder var6 = new StringBuilder();
         TreeSet<java.lang.String> var7 = new TreeSet();
         int var8 = -1;

         for (String var12 : var4.split("\n", -1)) {
            Matcher var13 = HELP_VERB.matcher(var12);
            if (var13.find()) {
               var7.add(var13.group(2));
            }

            int var14 = var12.length() - var12.stripLeading().length();
            if (var8 < 0 || !var12.isBlank() && var14 <= var8) {
               var8 = -1;
               if (var15.matcher(var12).find()) {
                  var8 = var14;
                  var6.append(var12).append('\n');
               }
            } else {
               var6.append(var12).append('\n');
            }
         }

         if (var6.isEmpty()) {
            var var17 = missing("no help topic '" + var2 + "' — help() with no argument lists everything");
            var17.put("topics", new ArrayList(var7));
            return var17;
         } else {
            LinkedHashMap<java.lang.String, java.lang.String> var16 = new LinkedHashMap();
            var16.put("topic", var2.trim());
            var16.put("markdown", var6.toString().strip() + "\n");
            return var16;
         }
      } else {
         return var0;
      }
   }

   public static Map<String, Object> textPart(String var0, String var1) {
      LinkedHashMap<java.lang.String, java.lang.Object> var2 = new LinkedHashMap();
      var2.put("type", "text");
      var2.put("text", var0);
      if (var1 != null) {
         var2.put("mime", var1);
      }

      return var2;
   }

   public static Map<String, Object> imagePart(String var0, String var1) {
      LinkedHashMap<java.lang.String, java.lang.Object> var2 = new LinkedHashMap();
      var2.put("type", "image");
      var2.put("mimeType", var1);
      var2.put("url", var0);
      return var2;
   }

   public static Map<String, Object> imagePartData(String var0, String var1) {
      LinkedHashMap<java.lang.String, java.lang.Object> var2 = new LinkedHashMap();
      var2.put("type", "image");
      var2.put("mimeType", var1);
      var2.put("data", var0);
      return var2;
   }

   public static Map<String, Object> resourcePart(String var0, String var1) {
      LinkedHashMap<java.lang.String, java.lang.Object> var2 = new LinkedHashMap();
      var2.put("type", "resource");
      var2.put("uri", var0);
      if (var1 != null) {
         var2.put("mimeType", var1);
      }

      return var2;
   }

   public static Object extractParts(Object var0) {
      if (var0 instanceof Map var1 && var1.containsKey("parts")) {
         Object var2 = ((Map)var0).remove("parts");
         return var2 instanceof List var3 && !var3.isEmpty() ? var2 : null;
      } else {
         return null;
      }
   }
}
