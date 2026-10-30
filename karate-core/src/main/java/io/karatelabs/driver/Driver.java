package io.karatelabs.driver;

import io.karatelabs.core.MockHandler;
import io.karatelabs.gherkin.Feature;
import io.karatelabs.http.HttpRequest;
import io.karatelabs.http.HttpResponse;
import io.karatelabs.js.JavaCallable;
import io.karatelabs.js.JsFunction;
import io.karatelabs.js.SimpleObject;
import java.lang.reflect.Array;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Predicate;
import java.util.function.Supplier;

public interface Driver extends CoreDriver, SimpleObject {
   @Override
   default Object jsGet(String name) {
      return switch (name) {
         case "url" -> this.getUrl();
         case "title" -> this.getTitle();
         case "cookies" -> this.getCookies();
         case "dimensions" -> this.getDimensions();
         case "dialogText" -> this.getDialogText();
         case "click" -> (JavaCallable) (ctx, args) -> this.click(String.valueOf(args[0]));
         case "input" -> (JavaCallable) (ctx, args) -> {
            String loc = String.valueOf(args[0]);
            Object value = args.length > 1 ? args[1] : "";
            int delay = args.length > 2 && args[2] instanceof Number n ? n.intValue() : 0;
            if (value instanceof List<?> list) {
               String[] arr = list.stream().map(String::valueOf).toArray(String[]::new);
               return this.input(loc, arr, delay);
            } else if (value != null && value.getClass().isArray()) {
               int length = Array.getLength(value);
               String[] strs = new String[length];

               for (int i = 0; i < length; i++) {
                  strs[i] = String.valueOf(Array.get(value, i));
               }

               return this.input(loc, strs, delay);
            } else {
               String s = value == null ? "" : String.valueOf(value);
               return delay > 0 ? this.input(loc, s, delay) : this.input(loc, s);
            }
         };
         case "inputFile" -> (JavaCallable) (ctx, args) -> {
            String loc = String.valueOf(args[0]);
            List<String> files = new ArrayList<>();

            for (int i = 1; i < args.length; i++) {
               if (args[i] instanceof List<?> list) {
                  list.forEach(o -> files.add(String.valueOf(o)));
               } else if (args[i] != null && args[i].getClass().isArray()) {
                  int length = Array.getLength(args[i]);

                  for (int j = 0; j < length; j++) {
                     files.add(String.valueOf(Array.get(args[i], j)));
                  }
               } else {
                  files.add(String.valueOf(args[i]));
               }
            }

            return this.inputFile(loc, files.toArray(new String[0]));
         };
         case "clear" -> (JavaCallable) (ctx, args) -> this.clear(String.valueOf(args[0]));
         case "focus" -> (JavaCallable) (ctx, args) -> this.focus(String.valueOf(args[0]));
         case "scroll" -> (JavaCallable) (ctx, args) -> this.scroll(String.valueOf(args[0]));
         case "highlight" -> (JavaCallable) (ctx, args) -> {
            String loc = String.valueOf(args[0]);
            return args.length > 1 && args[1] instanceof Number n ? this.highlight(loc, n.intValue()) : this.highlight(loc);
         };
         case "highlightAll" -> (JavaCallable) (ctx, args) -> {
            String loc = String.valueOf(args[0]);
            if (args.length > 1 && args[1] instanceof Number n) {
               this.highlightAll(loc, n.intValue());
            } else {
               this.highlightAll(loc);
            }

            return null;
         };
         case "select" -> (JavaCallable) (ctx, args) -> {
            String locator = String.valueOf(args[0]);
            Object value = args.length > 1 ? args[1] : null;
            return value instanceof Number n ? this.select(locator, n.intValue()) : this.select(locator, value != null ? String.valueOf(value) : "");
         };
         case "submit" -> (JavaCallable) (ctx, args) -> this.submit();
         case "text" -> (JavaCallable) (ctx, args) -> this.text(String.valueOf(args[0]));
         case "html" -> (JavaCallable) (ctx, args) -> this.html(String.valueOf(args[0]));
         case "value" -> (JavaCallable) (ctx, args) -> args.length == 1 ? this.value(String.valueOf(args[0])) : this.value(String.valueOf(args[0]), String.valueOf(args[1]));
         case "attribute" -> (JavaCallable) (ctx, args) -> this.attribute(String.valueOf(args[0]), String.valueOf(args[1]));
         case "property" -> (JavaCallable) (ctx, args) -> this.property(String.valueOf(args[0]), String.valueOf(args[1]));
         case "exists" -> (JavaCallable) (ctx, args) -> this.exists(String.valueOf(args[0]));
         case "enabled" -> (JavaCallable) (ctx, args) -> this.enabled(String.valueOf(args[0]));
         case "position" -> (JavaCallable) (ctx, args) -> args.length > 1 && args[1] instanceof Boolean
            ? this.position(String.valueOf(args[0]), (Boolean)args[1])
            : this.position(String.valueOf(args[0]));
         case "locate" -> (JavaCallable) (ctx, args) -> this.locate(String.valueOf(args[0]));
         case "locateAll" -> (JavaCallable) (ctx, args) -> this.locateAll(String.valueOf(args[0]));
         case "optional" -> (JavaCallable) (ctx, args) -> this.optional(String.valueOf(args[0]));
         case "waitFor" -> (JavaCallable) (ctx, args) -> this.waitFor(String.valueOf(args[0]));
         case "waitForText" -> (JavaCallable) (ctx, args) -> this.waitForText(String.valueOf(args[0]), String.valueOf(args[1]));
         case "waitForEnabled" -> (JavaCallable) (ctx, args) -> this.waitForEnabled(String.valueOf(args[0]));
         case "waitForUrl" -> (JavaCallable) (ctx, args) -> this.waitForUrl(String.valueOf(args[0]));
         case "waitUntil" -> (JavaCallable) (ctx, args) -> {
            if (args.length == 1) {
               Supplier<Object> supplier = DriverApi.asSupplier(args[0], ctx);
               return supplier != null ? this.waitUntil(supplier) : this.waitUntil(String.valueOf(args[0]));
            } else {
               return this.waitUntil(String.valueOf(args[0]), String.valueOf(args[1]));
            }
         };
         case "waitForResultCount" -> (JavaCallable) (ctx, args) -> this.waitForResultCount(String.valueOf(args[0]), ((Number)args[1]).intValue());
         case "switchFrame" -> (JavaCallable) (ctx, args) -> {
            Object arg = args.length > 0 ? args[0] : null;
            if (arg == null) {
               this.switchFrame((String)null);
            } else if (arg instanceof Number n) {
               this.switchFrame(n.intValue());
            } else {
               this.switchFrame(String.valueOf(arg));
            }

            return null;
         };
         case "switchPage" -> (JavaCallable) (ctx, args) -> {
            Object arg = args[0];
            if (arg instanceof Number n) {
               this.switchPage(n.intValue());
            } else {
               this.switchPage(String.valueOf(arg));
            }

            return null;
         };
         case "getPages" -> (JavaCallable) (ctx, args) -> this.getPages();
         case "script" -> (JavaCallable) (ctx, args) -> args.length == 1 ? this.script(args[0]) : this.script(String.valueOf(args[0]), args[1]);
         case "scriptAll" -> (JavaCallable) (ctx, args) -> this.scriptAll(String.valueOf(args[0]), args[1]);
         case "refresh" -> (JavaCallable) (ctx, args) -> {
            this.refresh();
            return null;
         };
         case "reload" -> (JavaCallable) (ctx, args) -> {
            this.reload();
            return null;
         };
         case "back" -> (JavaCallable) (ctx, args) -> {
            this.back();
            return null;
         };
         case "forward" -> (JavaCallable) (ctx, args) -> {
            this.forward();
            return null;
         };
         case "maximize" -> (JavaCallable) (ctx, args) -> {
            this.maximize();
            return null;
         };
         case "minimize" -> (JavaCallable) (ctx, args) -> {
            this.minimize();
            return null;
         };
         case "fullscreen" -> (JavaCallable) (ctx, args) -> {
            this.fullscreen();
            return null;
         };
         case "activate" -> (JavaCallable) (ctx, args) -> {
            this.activate();
            return null;
         };
         case "close" -> (JavaCallable) (ctx, args) -> {
            this.close();
            return null;
         };
         case "quit" -> (JavaCallable) (ctx, args) -> {
            this.quit();
            return null;
         };
         case "pdf" -> (JavaCallable) (ctx, args) -> args.length > 0 && args[0] instanceof Map ? this.pdf((Map<String, Object>)args[0]) : this.pdf();
         case "rightOf" -> (JavaCallable) (ctx, args) -> this.rightOf(String.valueOf(args[0]));
         case "leftOf" -> (JavaCallable) (ctx, args) -> this.leftOf(String.valueOf(args[0]));
         case "above" -> (JavaCallable) (ctx, args) -> this.above(String.valueOf(args[0]));
         case "below" -> (JavaCallable) (ctx, args) -> this.below(String.valueOf(args[0]));
         case "near" -> (JavaCallable) (ctx, args) -> this.near(String.valueOf(args[0]));
         case "waitForAny" -> (JavaCallable) (ctx, args) -> {
            if (args.length == 1 && args[0] instanceof List<?> list) {
               String[] arr = list.stream().map(String::valueOf).toArray(String[]::new);
               return this.waitForAny(arr);
            } else if (args.length == 1 && args[0] != null && args[0].getClass().isArray()) {
               Object array = args[0];
               int length = Array.getLength(array);
               String[] strs = new String[length];

               for (int i = 0; i < length; i++) {
                  strs[i] = String.valueOf(Array.get(array, i));
               }

               return this.waitForAny(strs);
            } else {
               String[] arr = new String[args.length];

               for (int i = 0; i < args.length; i++) {
                  arr[i] = String.valueOf(args[i]);
               }

               return this.waitForAny(arr);
            }
         };
         case "timeout" -> (JavaCallable) (ctx, args) -> args.length == 0 ? this.timeout() : this.timeout(((Number)args[0]).intValue());
         case "screenshot" -> (JavaCallable) (ctx, args) -> {
            if (args.length == 0) {
               return this.screenshot(true);
            } else {
               Object first = args[0];
               if (first instanceof Boolean b) {
                  return this.screenshot(b);
               } else if (!(first instanceof String s)) {
                  return this.screenshot(true);
               } else {
                  boolean embed = args.length <= 1 || !Boolean.FALSE.equals(args[1]);
                  return this.screenshot(s, embed);
               }
            }
         };
         case "cookie" -> (JavaCallable) (ctx, args) -> {
            Object arg = args[0];
            if (arg instanceof String s) {
               return this.cookie(s);
            } else if (arg instanceof Map) {
               this.cookie((Map<String, Object>)arg);
               return null;
            } else {
               return null;
            }
         };
         case "setCookies" -> (JavaCallable) (ctx, args) -> {
            Object arg = args[0];
            if (arg instanceof List) {
               this.setCookies((List<Map<String, Object>>)arg);
            }

            return null;
         };
         case "clearCookies" -> (JavaCallable) (ctx, args) -> {
            this.clearCookies();
            return null;
         };
         case "deleteCookie" -> (JavaCallable) (ctx, args) -> {
            this.deleteCookie(String.valueOf(args[0]));
            return null;
         };
         case "intercept" -> (JavaCallable) (ctx, args) -> {
            if (args[0] instanceof Map<?, ?> configMap) {
               List<Object> patterns = (List<Object>)configMap.get("patterns");
               if (patterns == null) {
                  throw new RuntimeException("missing array argument 'patterns': " + configMap);
               }

               List<String> urlPatterns = new ArrayList<>();

               for (Object p : patterns) {
                  if (p instanceof Map) {
                     Object urlPattern = ((Map)p).get("urlPattern");
                     urlPatterns.add(urlPattern != null ? String.valueOf(urlPattern) : "*");
                  } else {
                     urlPatterns.add(String.valueOf(p));
                  }
               }

               Object handlerObj = configMap.get("handler");
               String mock = configMap.get("mock") != null ? String.valueOf(configMap.get("mock")) : null;
               if (handlerObj instanceof JavaCallable jsHandler) {
                  this.intercept(urlPatterns, request -> {
                     Object result = jsHandler.call(ctx, request);
                     if (result instanceof InterceptResponse ir) {
                        return ir;
                     } else if (result instanceof Map<?, ?> resultMap) {
                        int status = resultMap.get("status") instanceof Number n ? n.intValue() : 200;
                        String body = resultMap.get("body") != null ? String.valueOf(resultMap.get("body")) : "";
                        Map<String, Object> responseHeaders = new LinkedHashMap<>();
                        if (resultMap.get("headers") instanceof Map) {
                           responseHeaders.putAll((Map<? extends String, ? extends Object>)resultMap.get("headers"));
                        }

                        return InterceptResponse.of(status, responseHeaders, body.getBytes(StandardCharsets.UTF_8));
                     } else {
                        return null;
                     }
                  });
               } else {
                  if (mock == null) {
                     throw new RuntimeException("missing 'mock' or 'handler' in intercept config: " + configMap);
                  }

                  Feature feature = Feature.read(mock);
                  MockHandler mockHandler = new MockHandler(feature);
                  this.intercept(
                     urlPatterns,
                     request -> {
                        HttpRequest httpRequest = new HttpRequest();
                        httpRequest.setUrl(request.getUrl());
                        httpRequest.setMethod(request.getMethod());
                        if (request.getHeaders() != null) {
                           Map<String, List<String>> headers = new LinkedHashMap<>();

                           for (Entry<String, Object> entry : request.getHeaders().entrySet()) {
                              headers.put(entry.getKey(), List.of(String.valueOf(entry.getValue())));
                           }

                           httpRequest.setHeaders(headers);
                        }

                        if (request.getPostData() != null) {
                           httpRequest.setBody(request.getPostData().getBytes(StandardCharsets.UTF_8));
                        }

                        HttpResponse httpResponse = mockHandler.apply(httpRequest);
                        if (httpResponse == null) {
                           return null;
                        } else {
                           Map<String, Object> responseHeaders = new LinkedHashMap<>();
                           if (httpResponse.getHeaders() != null) {
                              for (Entry<String, List<String>> entry : httpResponse.getHeaders().entrySet()) {
                                 List<String> values = entry.getValue();
                                 if (values != null && !values.isEmpty()) {
                                    responseHeaders.put(entry.getKey(), values.getLast());
                                 }
                              }
                           }

                           return InterceptResponse.of(
                              httpResponse.getStatus(), responseHeaders, httpResponse.getBodyBytes() != null ? httpResponse.getBodyBytes() : new byte[0]
                           );
                        }
                     }
                  );
               }
            }

            return null;
         };
         case "dialog" -> (JavaCallable) (ctx, args) -> {
            boolean accept = args.length > 0 && Boolean.TRUE.equals(args[0]);
            if (args.length > 1 && args[1] != null) {
               this.dialog(accept, String.valueOf(args[1]));
            } else {
               this.dialog(accept);
            }

            return null;
         };
         case "onDialog" -> (JavaCallable) (ctx, args) -> {
            if (args.length != 0 && args[0] != null) {
               if (args[0] instanceof JavaCallable jsHandler) {
                  this.onDialog(dialog -> jsHandler.call(ctx, dialog));
               }
            } else {
               this.onDialog(null);
            }

            return this;
         };
         case "mouse" -> (JavaCallable) (ctx, args) -> {
            if (args.length == 0) {
               return this.mouse();
            } else {
               return args.length == 1 ? this.mouse(String.valueOf(args[0])) : this.mouse((Number)args[0], (Number)args[1]);
            }
         };
         case "keys" -> (JavaCallable) (ctx, args) -> this.keys();
         case "retry" -> (JavaCallable) (ctx, args) -> {
            Integer count = args.length > 0 ? ((Number)args[0]).intValue() : null;
            Integer interval = args.length > 1 ? ((Number)args[1]).intValue() : null;
            return new RetryableDriver(this, count, interval);
         };
         default -> null;
      };
   }

   @Override
   default void putMember(String name, Object value) {
      switch (name) {
         case "url":
            this.setUrl(String.valueOf(value));
            break;
         case "dimensions":
            this.setDimensions((Map<String, Object>)value);
            break;
         default:
            SimpleObject.super.putMember(name, value);
      }
   }

   @Override
   default Object eval(String js) {
      return this.script(js);
   }

   default Object eval(Object expression) {
      return this.script(expression);
   }

   @Override
   default void navigate(String url) {
      this.setUrl(url);
   }

   @Override
   default void frame(Object target) {
      if (target == null) {
         this.switchFrame((String)null);
      } else if (target instanceof Number n) {
         this.switchFrame(n.intValue());
      } else {
         this.switchFrame(target.toString());
      }
   }

   @Override
   default void window(String operation) {
      switch (operation) {
         case "maximize":
            this.maximize();
            break;
         case "minimize":
            this.minimize();
            break;
         case "fullscreen":
            this.fullscreen();
            break;
         case "normal":
            this.setDimensions(Map.of("windowState", "normal"));
            break;
         default:
            throw new DriverException("unknown window operation: " + operation);
      }
   }

   @Override
   default void tab(Object target) {
      if (target instanceof Number n) {
         this.switchPage(n.intValue());
      } else {
         this.switchPage(target.toString());
      }
   }

   void setUrl(String var1);

   String getUrl();

   String getTitle();

   void waitForPageLoad(PageLoadStrategy var1);

   void waitForPageLoad(PageLoadStrategy var1, Duration var2);

   void refresh();

   void reload();

   void back();

   void forward();

   Object script(String var1);

   default Object script(Object expression) {
      String js = Locators.toFunction(expression);
      if (expression instanceof JsFunction || Locators.isFunctionDefinition(js)) {
         js = "(" + js + ")()";
      }

      return this.script(js);
   }

   default Object script(String locator, Object expression) {
      return this.script(Locators.scriptSelector(locator, expression));
   }

   default List<Object> scriptAll(String locator, Object expression) {
      return (List<Object>)this.script(Locators.scriptAllSelector(locator, expression));
   }

   @Override
   byte[] screenshot();

   byte[] screenshot(boolean var1);

   default byte[] screenshot(String locator) {
      return this.screenshot(locator, true);
   }

   default byte[] screenshot(String locator, boolean embed) {
      return this.screenshot(embed);
   }

   default byte[] screenshotFullPage(boolean embed) {
      return this.screenshot(embed);
   }

   default byte[] failureScreenshot() {
      return this.screenshot(false);
   }

   void onDialog(DialogHandler var1);

   String getDialogText();

   void dialog(boolean var1);

   @Override
   void dialog(boolean var1, String var2);

   Dialog getDialog();

   void switchFrame(int var1);

   void switchFrame(String var1);

   Map<String, Object> getCurrentFrame();

   default Driver submit() {
      String hash = this.getUrl() + this.script("document.documentElement.id");
      this.getOptions().setPreSubmitHash(hash);
      return this;
   }

   default void waitIfSubmitRequested() {
      String before = this.getOptions().getPreSubmitHash();
      if (before != null) {
         this.getOptions().setPreSubmitHash(null);
         long deadline = System.currentTimeMillis() + this.getOptions().getTimeout();

         while (System.currentTimeMillis() < deadline) {
            String after = this.getUrl() + this.script("document.documentElement.id");
            if (!before.equals(after)) {
               return;
            }

            try {
               Thread.sleep(500L);
            } catch (InterruptedException var6) {
               Thread.currentThread().interrupt();
               return;
            }
         }
      }
   }

   default Element click(String locator) {
      this.script(Locators.clickJs(locator));
      this.waitIfSubmitRequested();
      return BaseElement.of(this, locator);
   }

   default Element focus(String locator) {
      this.script(Locators.focusJs(locator));
      return BaseElement.of(this, locator);
   }

   default Element clear(String locator) {
      this.script(Locators.clearJs(locator));
      return BaseElement.of(this, locator);
   }

   default Element input(String locator, String value) {
      this.focus(locator);
      Boolean isDateTimeInput = (Boolean)this.script(locator, "_ && ['time','date','datetime-local','month','week'].indexOf(_.type) >= 0");
      if (Boolean.TRUE.equals(isDateTimeInput)) {
         this.script(Locators.inputJs(locator, value));
      } else {
         this.clear(locator);
         this.keys().type(value);
      }

      return BaseElement.of(this, locator);
   }

   default Element input(String locator, String[] values) {
      return this.input(locator, values, 0);
   }

   default Element input(String locator, String chars, int delay) {
      String[] array = new String[chars.length()];

      for (int i = 0; i < array.length; i++) {
         array[i] = Character.toString(chars.charAt(i));
      }

      return this.input(locator, array, delay);
   }

   default Element input(String locator, String[] values, int delay) {
      this.focus(locator);
      this.clear(locator);
      Keys k = this.keys();

      for (String value : values) {
         if (delay > 0) {
            try {
               Thread.sleep(delay);
            } catch (InterruptedException var10) {
               Thread.currentThread().interrupt();
            }
         }

         if (value != null && value.length() == 1 && value.charAt(0) >= '\ue000' && value.charAt(0) <= '\ue03d') {
            k.press(value);
         } else {
            k.type(value);
         }
      }

      return BaseElement.of(this, locator);
   }

   default Element inputFile(String locator, String... files) {
      throw new DriverException("inputFile not supported by this driver");
   }

   default Element value(String locator, String value) {
      this.script(Locators.inputJs(locator, value));
      return BaseElement.of(this, locator);
   }

   default Element select(String locator, String text) {
      Object matched = this.script(Locators.optionSelector(locator, text));
      if (!Boolean.TRUE.equals(matched)) {
         throw new DriverException("select failed, no option matching: '" + text + "' for: " + locator);
      } else {
         return BaseElement.of(this, locator);
      }
   }

   default Element select(String locator, int index) {
      String js = Locators.wrapInFunctionInvoke(
         "var e = "
            + Locators.selector(locator)
            + "; e.options["
            + index
            + "].selected = true; e.dispatchEvent(new Event('input', {bubbles: true})); e.dispatchEvent(new Event('change', {bubbles: true}))"
      );
      this.script(js);
      return BaseElement.of(this, locator);
   }

   default Element scroll(String locator) {
      this.script(Locators.scrollJs(locator));
      return BaseElement.of(this, locator);
   }

   default Element highlight(String locator) {
      return this.highlight(locator, this.getOptions().getHighlightDuration());
   }

   default Element highlight(String locator, int millis) {
      this.script(Locators.highlight(locator, millis));
      return BaseElement.of(this, locator);
   }

   default void highlightAll(String locator) {
      this.highlightAll(locator, this.getOptions().getHighlightDuration());
   }

   default void highlightAll(String locator, int millis) {
      this.script(Locators.highlightAll(locator, millis));
   }

   default String text(String locator) {
      return (String)this.script(Locators.textJs(locator));
   }

   default String html(String locator) {
      return (String)this.script(Locators.outerHtmlJs(locator));
   }

   default String value(String locator) {
      return (String)this.script(Locators.valueJs(locator));
   }

   default String attribute(String locator, String name) {
      return (String)this.script(Locators.attributeJs(locator, name));
   }

   default Object property(String locator, String name) {
      return this.script(Locators.propertyJs(locator, name));
   }

   default boolean enabled(String locator) {
      return Boolean.TRUE.equals(this.script(Locators.enabledJs(locator)));
   }

   default boolean exists(String locator) {
      return Boolean.TRUE.equals(this.script(Locators.existsJs(locator)));
   }

   default Map<String, Object> position(String locator) {
      return (Map<String, Object>)this.script(Locators.getPositionJs(locator));
   }

   default Map<String, Object> position(String locator, boolean relative) {
      if (relative) {
         String js = Locators.wrapInFunctionInvoke(
            "var r = " + Locators.selector(locator) + ".getBoundingClientRect(); return { x: r.x, y: r.y, width: r.width, height: r.height }"
         );
         return (Map<String, Object>)this.script(js);
      } else {
         return this.position(locator);
      }
   }

   default Element locate(String locator) {
      return BaseElement.of(this, locator);
   }

   List<Element> locateAll(String var1);

   default List<Element> locateAll(String locator, Predicate<Element> predicate) {
      List<Element> all = this.locateAll(locator);
      List<Element> filtered = new ArrayList<>(all.size());

      for (Element e : all) {
         if (predicate.test(e)) {
            filtered.add(e);
         }
      }

      return filtered;
   }

   default List<Object> scriptAll(String locator, Object expression, Predicate<Object> predicate) {
      List<Object> all = this.scriptAll(locator, expression);
      List<Object> filtered = new ArrayList<>(all.size());

      for (Object o : all) {
         if (predicate.test(o)) {
            filtered.add(o);
         }
      }

      return filtered;
   }

   default Element optional(String locator) {
      return BaseElement.optional(this, locator);
   }

   Element waitFor(String var1);

   Element waitFor(String var1, Duration var2);

   Element waitForAny(String var1, String var2);

   Element waitForAny(String[] var1);

   Element waitForAny(String[] var1, Duration var2);

   Element waitForText(String var1, String var2);

   Element waitForText(String var1, String var2, Duration var3);

   Element waitForEnabled(String var1);

   Element waitForEnabled(String var1, Duration var2);

   String waitForUrl(String var1);

   String waitForUrl(String var1, Duration var2);

   Element waitUntil(String var1, String var2);

   Element waitUntil(String var1, String var2, Duration var3);

   boolean waitUntil(String var1);

   boolean waitUntil(String var1, Duration var2);

   Object waitUntil(Supplier<Object> var1);

   Object waitUntil(Supplier<Object> var1, Duration var2);

   List<Element> waitForResultCount(String var1, int var2);

   List<Element> waitForResultCount(String var1, int var2, Duration var3);

   default List<Object> waitForResultCount(String locator, int count, String expression) {
      return (List<Object>)this.waitUntil(() -> {
         List<Object> list = this.scriptAll(locator, expression);
         return list.size() == count ? list : null;
      });
   }

   @Override
   Map<String, Object> cookie(String var1);

   @Override
   void cookie(Map<String, Object> var1);

   void deleteCookie(String var1);

   void clearCookies();

   List<Map<String, Object>> getCookies();

   default void setCookies(List<Map<String, Object>> cookies) {
      if (cookies != null) {
         for (Map<String, Object> c : cookies) {
            this.cookie(c);
         }
      }
   }

   void maximize();

   void minimize();

   void fullscreen();

   Map<String, Object> getDimensions();

   void setDimensions(Map<String, Object> var1);

   void activate();

   @Override
   byte[] pdf(Map<String, Object> var1);

   byte[] pdf();

   @Override
   Mouse mouse();

   Mouse mouse(String var1);

   Mouse mouse(Number var1, Number var2);

   @Override
   Keys keys();

   List<String> getPages();

   void switchPage(String var1);

   void switchPage(int var1);

   default void switchPageById(String targetId) {
      this.switchPage(targetId);
   }

   default List<Map<String, Object>> drainOpenedTargets() {
      return Collections.emptyList();
   }

   default Finder rightOf(String locator) {
      return new Finder(this, locator, Finder.Position.RIGHT_OF);
   }

   default Finder leftOf(String locator) {
      return new Finder(this, locator, Finder.Position.LEFT_OF);
   }

   default Finder above(String locator) {
      return new Finder(this, locator, Finder.Position.ABOVE);
   }

   default Finder below(String locator) {
      return new Finder(this, locator, Finder.Position.BELOW);
   }

   default Finder near(String locator) {
      return new Finder(this, locator, Finder.Position.NEAR);
   }

   default Driver timeout(int millis) {
      this.getOptions().setTimeout(millis);
      return this;
   }

   default int timeout() {
      return this.getOptions().getTimeout();
   }

   default Driver delay(int millis) {
      if (millis > 0) {
         try {
            Thread.sleep(millis);
         } catch (InterruptedException var3) {
            Thread.currentThread().interrupt();
         }
      }

      return this;
   }

   @Override
   void intercept(List<String> var1, InterceptHandler var2);

   void intercept(InterceptHandler var1);

   void stopIntercept();

   @Override
   void quit();

   void close();

   @Override
   boolean isTerminated();

   default boolean isResponsive() {
      return true;
   }

   default boolean isReady() {
      return true;
   }

   default void waitUntilReady() {
   }

   @Override
   DriverOptions getOptions();
}
