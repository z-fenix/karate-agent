package io.karatelabs.driver;

import io.karatelabs.common.Resource;
import io.karatelabs.core.ScenarioRuntime;
import io.karatelabs.js.Args;
import io.karatelabs.js.Context;
import io.karatelabs.js.Engine;
import io.karatelabs.js.JavaCallable;
import io.karatelabs.js.JavaType;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.function.Supplier;

public class DriverApi {
   public static final String CLICK = "click";
   public static final String INPUT = "input";
   public static final String INPUT_FILE = "inputFile";
   public static final String CLEAR = "clear";
   public static final String FOCUS = "focus";
   public static final String SCROLL = "scroll";
   public static final String HIGHLIGHT = "highlight";
   public static final String HIGHLIGHT_ALL = "highlightAll";
   public static final String SELECT = "select";
   public static final String SUBMIT = "submit";
   public static final String TEXT = "text";
   public static final String HTML = "html";
   public static final String VALUE = "value";
   public static final String ATTRIBUTE = "attribute";
   public static final String PROPERTY = "property";
   public static final String EXISTS = "exists";
   public static final String ENABLED = "enabled";
   public static final String POSITION = "position";
   public static final String LOCATE = "locate";
   public static final String LOCATE_ALL = "locateAll";
   public static final String OPTIONAL = "optional";
   public static final String WAIT_FOR = "waitFor";
   public static final String WAIT_FOR_ANY = "waitForAny";
   public static final String WAIT_FOR_TEXT = "waitForText";
   public static final String WAIT_FOR_ENABLED = "waitForEnabled";
   public static final String WAIT_FOR_URL = "waitForUrl";
   public static final String WAIT_UNTIL = "waitUntil";
   public static final String WAIT_FOR_RESULT_COUNT = "waitForResultCount";
   public static final String SWITCH_FRAME = "switchFrame";
   public static final String SWITCH_PAGE = "switchPage";
   public static final String GET_PAGES = "getPages";
   public static final String SCRIPT = "script";
   public static final String SCRIPT_ALL = "scriptAll";
   public static final String REFRESH = "refresh";
   public static final String RELOAD = "reload";
   public static final String BACK = "back";
   public static final String FORWARD = "forward";
   public static final String MAXIMIZE = "maximize";
   public static final String MINIMIZE = "minimize";
   public static final String FULLSCREEN = "fullscreen";
   public static final String ACTIVATE = "activate";
   public static final String CLOSE = "close";
   public static final String QUIT = "quit";
   public static final String SCREENSHOT = "screenshot";
   public static final String PDF = "pdf";
   public static final String COOKIE = "cookie";
   public static final String COOKIES_SET = "setCookies";
   public static final String CLEAR_COOKIES = "clearCookies";
   public static final String DELETE_COOKIE = "deleteCookie";
   public static final String INTERCEPT = "intercept";
   public static final String DIALOG = "dialog";
   public static final String ON_DIALOG = "onDialog";
   public static final String DIALOG_TEXT = "dialogText";
   public static final String MOUSE = "mouse";
   public static final String KEYS = "keys";
   public static final String RIGHT_OF = "rightOf";
   public static final String LEFT_OF = "leftOf";
   public static final String ABOVE = "above";
   public static final String BELOW = "below";
   public static final String NEAR = "near";
   public static final String RETRY = "retry";
   public static final String TIMEOUT = "timeout";
   public static final String URL = "url";
   public static final String TITLE = "title";
   public static final String COOKIES = "cookies";
   public static final String DIMENSIONS = "dimensions";
   public static final List<String> BOUND_METHODS = List.of(
      "click",
      "input",
      "inputFile",
      "clear",
      "focus",
      "scroll",
      "highlight",
      "highlightAll",
      "select",
      "submit",
      "text",
      "html",
      "value",
      "attribute",
      "property",
      "exists",
      "enabled",
      "position",
      "locate",
      "locateAll",
      "optional",
      "waitFor",
      "waitForAny",
      "waitForText",
      "waitForEnabled",
      "waitForUrl",
      "waitUntil",
      "waitForResultCount",
      "switchFrame",
      "switchPage",
      "getPages",
      "script",
      "scriptAll",
      "refresh",
      "reload",
      "back",
      "forward",
      "maximize",
      "minimize",
      "fullscreen",
      "activate",
      "close",
      "quit",
      "screenshot",
      "pdf",
      "cookie",
      "setCookies",
      "clearCookies",
      "deleteCookie",
      "intercept",
      "dialog",
      "onDialog",
      "mouse",
      "keys",
      "rightOf",
      "leftOf",
      "above",
      "below",
      "near",
      "retry",
      "timeout"
   );

   public static List<String> resolveFilePaths(String... refs) {
      List<String> files = new ArrayList<>(refs.length);

      for (String ref : refs) {
         files.add(resolveFilePath(ref));
      }

      return files;
   }

   private static String resolveFilePath(String ref) {
      if (ref.startsWith("file:")) {
         return Path.of(Resource.removePrefix(ref)).toAbsolutePath().normalize().toString();
      } else {
         ScenarioRuntime runtime = ScenarioRuntime.currentOrNull();

         Resource resource;
         try {
            resource = runtime != null && runtime.getFeatureRuntime() != null ? runtime.getFeatureRuntime().resolve(ref) : Resource.path(ref);
         } catch (Exception var4) {
            throw new DriverException(fileNotFoundMessage(ref, null), var4);
         }

         Path path = resource.isFile() ? resource.getPath() : null;
         if (path == null || path.getFileSystem() != FileSystems.getDefault()) {
            throw new DriverException(
               "inputFile: not a local file: " + ref + " — the browser needs a real file on disk; extract it first, or point at it with 'file:'"
            );
         } else if (!resource.exists()) {
            throw new DriverException(fileNotFoundMessage(ref, path));
         } else {
            return path.toAbsolutePath().normalize().toString();
         }
      }
   }

   private static String fileNotFoundMessage(String ref, Path resolved) {
      return "inputFile: file not found: "
         + ref
         + (resolved == null ? "" : " (resolved to: " + resolved.toAbsolutePath().normalize() + ")")
         + " — a bare path is relative to the feature, a leading '/' anchors the project root, 'file:' is a host machine path";
   }

   public static Supplier<Object> asSupplier(Object arg, Context ctx) {
      if (arg instanceof JavaCallable jc) {
         return () -> jc.call(ctx);
      } else if (arg instanceof Supplier<?> s) {
         return () -> s.get();
      } else {
         return arg instanceof Callable<?> c ? () -> {
            try {
               return c.call();
            } catch (Exception var2x) {
               throw new RuntimeException(var2x);
            }
         } : null;
      }
   }

   public static void bindJsHelpers(Engine engine, Driver driver) {
      for (String methodName : BOUND_METHODS) {
         Object callable = driver.jsGet(methodName);
         if (callable != null) {
            engine.putRootBinding(methodName, callable);
         }
      }

      engine.putRootBinding("Key", new JavaType(Keys.class));
      engine.putRootBinding("delay", Args.invoke(args -> {
         int millis = args.length > 0 ? ((Number)args[0]).intValue() : 0;
         if (millis > 0) {
            try {
               Thread.sleep(millis);
            } catch (InterruptedException var3x) {
               Thread.currentThread().interrupt();
            }
         }

         return null;
      }));
   }
}
