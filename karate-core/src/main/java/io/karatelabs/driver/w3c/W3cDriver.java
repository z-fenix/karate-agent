package io.karatelabs.driver.w3c;

import io.karatelabs.driver.BaseElement;
import io.karatelabs.driver.Dialog;
import io.karatelabs.driver.DialogHandler;
import io.karatelabs.driver.Driver;
import io.karatelabs.driver.DriverApi;
import io.karatelabs.driver.DriverException;
import io.karatelabs.driver.DriverOptions;
import io.karatelabs.driver.Element;
import io.karatelabs.driver.InterceptHandler;
import io.karatelabs.driver.Keys;
import io.karatelabs.driver.Locators;
import io.karatelabs.driver.Mouse;
import io.karatelabs.driver.PageLoadStrategy;
import io.karatelabs.output.LogContext;
import io.karatelabs.process.ProcessBuilder;
import io.karatelabs.process.ProcessHandle;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class W3cDriver implements Driver {
   private static final Logger logger = LoggerFactory.getLogger(W3cDriver.class);
   private static final String DRIVER_JS = loadResource("driver.js");
   private final W3cSession session;
   private final W3cDriverOptions options;
   private final ProcessHandle driverProcess;
   private volatile boolean terminated = false;

   private static String loadResource(String name) {
      try {
         String var2;
         try (InputStream is = W3cDriver.class.getResourceAsStream("/io/karatelabs/driver/" + name)) {
            if (is == null) {
               logger.warn("Resource not found: {}", name);
               return "";
            }

            var2 = new String(is.readAllBytes(), StandardCharsets.UTF_8);
         }

         return var2;
      } catch (IOException var6) {
         logger.warn("Failed to load resource: {}", name);
         return "";
      }
   }

   private W3cDriver(W3cSession session, W3cDriverOptions options, ProcessHandle driverProcess) {
      this.session = session;
      this.options = options;
      this.driverProcess = driverProcess;
   }

   public static W3cDriver start(Map<String, Object> config) {
      W3cDriverOptions opts = W3cDriverOptions.fromMap(config);
      return opts.isRemote() ? connect(opts.getWebDriverUrl(), opts) : launch(opts);
   }

   public static W3cDriver connect(String webDriverUrl, W3cDriverOptions opts) {
      logger.info("Connecting to W3C WebDriver at: {}", webDriverUrl);
      W3cSession session = W3cSession.create(webDriverUrl, opts.buildSessionPayload(), opts.getTimeoutDuration());
      return new W3cDriver(session, opts, null);
   }

   private static W3cDriver launch(W3cDriverOptions opts) {
      W3cBrowserType browserType = opts.getBrowserType();
      int port = opts.getPort();
      String executable = opts.getExecutable();
      logger.info("Launching {} on port {}", executable, port);
      List<String> command = new ArrayList<>();
      command.add(executable);
      command.add(browserType.formatPortArg(port));
      command.addAll(opts.getAddOptions());
      ProcessHandle process = ProcessHandle.start(ProcessBuilder.create().args(command).redirectErrorStream(true).logToContext(false).build());

      try {
         waitForPort("localhost", port, opts.getTimeoutDuration().toMillis());
         logger.info("{} started on port {}", executable, port);
         String baseUrl = "http://localhost:" + port;
         W3cSession session = W3cSession.create(baseUrl, opts.buildSessionPayload(), opts.getTimeoutDuration());
         return new W3cDriver(session, opts, process);
      } catch (RuntimeException var8) {
         process.close(true);
         throw new DriverException("Failed to start " + executable + ": " + var8.getMessage(), var8);
      }
   }

   public W3cSession getSession() {
      return this.session;
   }

   @Override
   public Object eval(String js) {
      if (js.contains("__kjs")) {
         this.ensureKjsRuntime();
      }

      String expression = prefixReturnIfNeeded(js);

      Object result;
      try {
         result = this.session.executeScript(expression);
      } catch (Exception var7) {
         logger.warn("javascript failed, will retry once: {}", var7.getMessage());
         sleep(this.options.getRetryInterval());

         try {
            result = this.session.executeScript(expression);
         } catch (Exception var6) {
            logger.error("javascript failed twice: {}", var6.getMessage());
            throw new DriverException("javascript failed: " + var6.getMessage(), var6);
         }
      }

      return this.wrapElementReferences(result);
   }

   private Object wrapElementReferences(Object result) {
      if (W3cSession.isElementReference(result)) {
         String elementId = W3cSession.elementIdFrom(result);
         return new W3cElement(this, "(eval result)", true, elementId, this.session);
      } else {
         if (result instanceof List<?> list) {
            boolean hasElements = list.stream().anyMatch(W3cSession::isElementReference);
            if (hasElements) {
               List<Object> wrapped = new ArrayList<>();

               for (Object item : list) {
                  if (W3cSession.isElementReference(item)) {
                     String eid = W3cSession.elementIdFrom(item);
                     wrapped.add(new W3cElement(this, "(eval result)", true, eid, this.session));
                  } else {
                     wrapped.add(item);
                  }
               }

               return wrapped;
            }
         }

         return result;
      }
   }

   @Override
   public void navigate(String url) {
      this.session.navigateTo(url);
   }

   @Override
   public Keys keys() {
      return new W3cKeys(this.session);
   }

   @Override
   public byte[] screenshot() {
      return this.session.screenshot();
   }

   @Override
   public void frame(Object target) {
      if (target == null) {
         this.session.switchFrame(null);
      } else if (target instanceof Integer) {
         int index = (Integer)target;
         if (index == -1) {
            this.session.parentFrame();
         } else {
            this.session.switchFrame(index);
         }
      } else {
         String locator = target.toString();
         String frameElementId = this.findElementIdWithRetry(locator);
         Map<String, Object> elementRef = Map.of("element-6066-11e4-a52e-4f735466cecf", frameElementId);
         this.session.switchFrame(elementRef);
      }
   }

   @Override
   public void dialog(boolean accept, String input) {
      if (input != null) {
         this.session.sendAlertText(input);
      }

      if (accept) {
         this.session.acceptAlert();
      } else {
         this.session.dismissAlert();
      }
   }

   @Override
   public Mouse mouse() {
      return new W3cMouse(this.session);
   }

   @Override
   public byte[] pdf(Map<String, Object> pdfOptions) {
      throw new UnsupportedOperationException("PDF generation not supported on WebDriver backend");
   }

   @Override
   public Map<String, Object> cookie(String name) {
      return this.session.getCookie(name);
   }

   @Override
   public void cookie(Map<String, Object> cookie) {
      this.session.addCookie(cookie);
   }

   @Override
   public void intercept(List<String> patterns, InterceptHandler handler) {
      throw new UnsupportedOperationException("Request interception not supported on WebDriver backend");
   }

   @Override
   public void window(String operation) {
      switch (operation) {
         case "maximize":
            this.session.maximizeWindow();
            break;
         case "minimize":
            this.session.minimizeWindow();
            break;
         case "fullscreen":
            this.session.fullscreenWindow();
            break;
         default:
            logger.warn("Unknown window operation: {}", operation);
      }
   }

   @Override
   public void tab(Object target) {
      if (target instanceof Integer) {
         List<String> handles = this.session.getWindowHandles();
         int index = (Integer)target;
         if (index < 0 || index >= handles.size()) {
            throw new DriverException("Tab index out of bounds: " + index + " (available: " + handles.size() + ")");
         }

         this.session.switchWindow(handles.get(index));
      } else if (target instanceof String titleOrUrl) {
         String currentHandle = this.session.getWindowHandle();

         for (String handle : this.session.getWindowHandles()) {
            this.session.switchWindow(handle);
            String title = this.session.getTitle();
            String url = this.session.getUrl();
            if (title != null && title.contains(titleOrUrl) || url != null && url.contains(titleOrUrl)) {
               return;
            }
         }

         this.session.switchWindow(currentHandle);
         throw new DriverException("No tab found matching: " + titleOrUrl);
      }
   }

   @Override
   public Element click(String locator) {
      this.eval(Locators.clickJs(locator));
      return BaseElement.existing(this, locator);
   }

   @Override
   public Element input(String locator, String value) {
      Boolean isDateTimeInput = (Boolean)this.eval(
         Locators.scriptSelector(locator, "_ && ['time','date','datetime-local','month','week'].indexOf(_.type) >= 0")
      );
      if (Boolean.TRUE.equals(isDateTimeInput)) {
         this.eval(Locators.inputJs(locator, value));
      } else {
         this.eval(Locators.clearJs(locator));
         String elementId = this.findElementIdWithRetry(locator);

         try {
            this.session.sendKeys(elementId, value);
         } catch (Exception var6) {
            logger.warn("Native sendKeys failed. Falling back to JS injection.");
            this.eval(Locators.inputJs(locator, value));
         }
      }

      return BaseElement.existing(this, locator);
   }

   @Override
   public Element inputFile(String locator, String... files) {
      List<String> resolved = DriverApi.resolveFilePaths(files);
      logger.debug("inputFile: {} <- {}", locator, resolved);
      String elementId = this.findElementIdWithRetry(locator);
      this.session.sendKeys(elementId, String.join("\n", resolved));
      return BaseElement.existing(this, locator);
   }

   @Override
   public Element clear(String locator) {
      this.eval(Locators.clearJs(locator));
      return BaseElement.existing(this, locator);
   }

   @Override
   public Element focus(String locator) {
      this.eval(Locators.focusJs(locator));
      return BaseElement.existing(this, locator);
   }

   @Override
   public Element scroll(String locator) {
      this.eval(Locators.scrollJs(locator));
      return BaseElement.existing(this, locator);
   }

   @Override
   public Element highlight(String locator) {
      this.eval(Locators.highlight(locator, this.options.getHighlightDuration()));
      return BaseElement.existing(this, locator);
   }

   @Override
   public String text(String locator) {
      return (String)this.eval(Locators.textJs(locator));
   }

   @Override
   public String html(String locator) {
      return (String)this.eval(Locators.outerHtmlJs(locator));
   }

   @Override
   public String value(String locator) {
      return (String)this.eval(Locators.valueJs(locator));
   }

   @Override
   public String attribute(String locator, String name) {
      return (String)this.eval(Locators.attributeJs(locator, name));
   }

   @Override
   public boolean enabled(String locator) {
      Object result = this.eval(Locators.enabledJs(locator));
      return Boolean.TRUE.equals(result);
   }

   @Override
   public boolean exists(String locator) {
      try {
         String js = Locators.existsJs(locator);
         if (js.contains("__kjs")) {
            this.ensureKjsRuntime();
         }

         String expression = prefixReturnIfNeeded(js);
         Object result = this.session.executeScript(expression);
         return Boolean.TRUE.equals(result);
      } catch (Exception var5) {
         return false;
      }
   }

   @Override
   public List<Element> locateAll(String locator) {
      Object result = this.eval(Locators.findAllJs(locator));
      List<Element> elements = new ArrayList<>();
      if (result instanceof List) {
         for (Object loc : (List)result) {
            elements.add(new BaseElement(this, String.valueOf(loc), true));
         }
      }

      return elements;
   }

   @Override
   public Element waitFor(String locator) {
      return this.waitFor(locator, this.options.getTimeoutDuration());
   }

   @Override
   public Element waitFor(String locator, Duration timeout) {
      long deadline = System.currentTimeMillis() + timeout.toMillis();

      while (System.currentTimeMillis() < deadline) {
         if (this.exists(locator)) {
            return BaseElement.existing(this, locator);
         }

         sleep(this.options.getRetryInterval());
      }

      throw new DriverException("waitFor timeout: " + locator);
   }

   @Override
   public Element waitForAny(String locator1, String locator2) {
      return this.waitForAny(new String[]{locator1, locator2});
   }

   @Override
   public Element waitForAny(String[] locators) {
      return this.waitForAny(locators, this.options.getTimeoutDuration());
   }

   @Override
   public Element waitForAny(String[] locators, Duration timeout) {
      long deadline = System.currentTimeMillis() + timeout.toMillis();

      while (System.currentTimeMillis() < deadline) {
         for (String locator : locators) {
            if (this.exists(locator)) {
               return BaseElement.existing(this, locator);
            }
         }

         sleep(this.options.getRetryInterval());
      }

      throw new DriverException("waitForAny timeout: " + String.join(", ", locators));
   }

   @Override
   public Element waitForText(String locator, String expected) {
      return this.waitForText(locator, expected, this.options.getTimeoutDuration());
   }

   @Override
   public Element waitForText(String locator, String expected, Duration timeout) {
      long deadline = System.currentTimeMillis() + timeout.toMillis();

      while (System.currentTimeMillis() < deadline) {
         try {
            String text = this.text(locator);
            if (text != null && text.contains(expected)) {
               return BaseElement.existing(this, locator);
            }
         } catch (Exception var7) {
         }

         sleep(this.options.getRetryInterval());
      }

      throw new DriverException("waitForText timeout: " + locator + " expected: " + expected);
   }

   @Override
   public Element waitForEnabled(String locator) {
      return this.waitForEnabled(locator, this.options.getTimeoutDuration());
   }

   @Override
   public Element waitForEnabled(String locator, Duration timeout) {
      long deadline = System.currentTimeMillis() + timeout.toMillis();

      while (System.currentTimeMillis() < deadline) {
         try {
            if (this.enabled(locator)) {
               return BaseElement.existing(this, locator);
            }
         } catch (Exception var6) {
         }

         sleep(this.options.getRetryInterval());
      }

      throw new DriverException("waitForEnabled timeout: " + locator);
   }

   @Override
   public String waitForUrl(String expected) {
      return this.waitForUrl(expected, this.options.getTimeoutDuration());
   }

   @Override
   public String waitForUrl(String expected, Duration timeout) {
      long deadline = System.currentTimeMillis() + timeout.toMillis();

      while (System.currentTimeMillis() < deadline) {
         String url = this.getUrl();
         if (url != null && url.contains(expected)) {
            return url;
         }

         sleep(this.options.getRetryInterval());
      }

      throw new DriverException("waitForUrl timeout: expected URL containing: " + expected);
   }

   @Override
   public Element waitUntil(String locator, String expression) {
      return this.waitUntil(locator, expression, this.options.getTimeoutDuration());
   }

   @Override
   public Element waitUntil(String locator, String expression, Duration timeout) {
      long deadline = System.currentTimeMillis() + timeout.toMillis();
      String js = Locators.scriptSelector(locator, expression);

      while (System.currentTimeMillis() < deadline) {
         try {
            Object result = this.eval(js);
            if (isTruthy(result)) {
               return BaseElement.existing(this, locator);
            }
         } catch (Exception var8) {
         }

         sleep(this.options.getRetryInterval());
      }

      throw new DriverException("waitUntil timeout: " + locator + " expression: " + expression);
   }

   @Override
   public boolean waitUntil(String expression) {
      return this.waitUntil(expression, this.options.getTimeoutDuration());
   }

   @Override
   public boolean waitUntil(String expression, Duration timeout) {
      long deadline = System.currentTimeMillis() + timeout.toMillis();

      while (System.currentTimeMillis() < deadline) {
         try {
            Object result = this.eval(expression);
            if (isTruthy(result)) {
               return true;
            }
         } catch (Exception var6) {
         }

         sleep(this.options.getRetryInterval());
      }

      throw new DriverException("waitUntil timeout: " + expression);
   }

   @Override
   public Object waitUntil(Supplier<Object> condition) {
      return this.waitUntil(condition, this.options.getTimeoutDuration());
   }

   @Override
   public Object waitUntil(Supplier<Object> condition, Duration timeout) {
      long deadline = System.currentTimeMillis() + timeout.toMillis();

      while (System.currentTimeMillis() < deadline) {
         Object result = condition.get();
         if (isTruthy(result)) {
            return result;
         }

         sleep(this.options.getRetryInterval());
      }

      throw new DriverException("waitUntil (supplier) timeout");
   }

   @Override
   public List<Element> waitForResultCount(String locator, int count) {
      return this.waitForResultCount(locator, count, this.options.getTimeoutDuration());
   }

   @Override
   public List<Element> waitForResultCount(String locator, int count, Duration timeout) {
      long deadline = System.currentTimeMillis() + timeout.toMillis();

      while (System.currentTimeMillis() < deadline) {
         List<Element> results = this.locateAll(locator);
         if (results.size() == count) {
            return results;
         }

         sleep(this.options.getRetryInterval());
      }

      throw new DriverException("waitForResultCount timeout: " + locator + " expected count: " + count);
   }

   @Override
   public String getUrl() {
      return this.session.getUrl();
   }

   @Override
   public String getTitle() {
      return this.session.getTitle();
   }

   @Override
   public void deleteCookie(String name) {
      this.session.deleteCookie(name);
   }

   @Override
   public void clearCookies() {
      this.session.deleteAllCookies();
   }

   @Override
   public List<Map<String, Object>> getCookies() {
      return this.session.getAllCookies();
   }

   @Override
   public Map<String, Object> getDimensions() {
      return this.session.getWindowRect();
   }

   @Override
   public void setDimensions(Map<String, Object> dimensions) {
      this.session.setWindowRect(dimensions);
   }

   @Override
   public List<String> getPages() {
      return this.session.getWindowHandles();
   }

   @Override
   public Dialog getDialog() {
      try {
         String text = this.session.getAlertText();
         if (text != null) {
            return new W3cDriver.W3cDialog(this.session, text);
         }
      } catch (Exception var2) {
      }

      return null;
   }

   @Override
   public void onDialog(DialogHandler handler) {
      throw new UnsupportedOperationException("Dialog callback handler not supported on WebDriver backend. Use dialog(true/false) after the dialog appears.");
   }

   @Override
   public void setUrl(String url) {
      this.session.navigateTo(url);
   }

   @Override
   public void waitForPageLoad(PageLoadStrategy strategy) {
   }

   @Override
   public void waitForPageLoad(PageLoadStrategy strategy, Duration timeout) {
   }

   @Override
   public void refresh() {
      this.session.refresh();
   }

   @Override
   public void reload() {
      this.session.refresh();
   }

   @Override
   public void back() {
      this.session.back();
   }

   @Override
   public void forward() {
      this.session.forward();
   }

   @Override
   public Object script(String expression) {
      return this.eval(expression);
   }

   @Override
   public byte[] screenshot(boolean embed) {
      byte[] bytes = this.screenshot();
      if (embed) {
         LogContext.get().embed(bytes, "image/png", "screenshot.png");
      }

      return bytes;
   }

   @Override
   public byte[] screenshot(String locator, boolean embed) {
      try {
         String elementId = this.findElementIdWithRetry(locator);
         byte[] bytes = this.session.elementScreenshot(elementId);
         if (embed) {
            LogContext.get().embed(bytes, "image/png", "screenshot.png");
         }

         return bytes;
      } catch (Exception var5) {
         return this.screenshot(embed);
      }
   }

   @Override
   public String getDialogText() {
      try {
         return this.session.getAlertText();
      } catch (Exception var2) {
         return null;
      }
   }

   @Override
   public void dialog(boolean accept) {
      this.dialog(accept, null);
   }

   @Override
   public void switchFrame(int index) {
      if (index == -1) {
         this.session.parentFrame();
      } else {
         this.session.switchFrame(index);
      }
   }

   @Override
   public void switchFrame(String locator) {
      if (locator == null) {
         this.session.switchFrame(null);
      } else {
         this.frame(locator);
      }
   }

   @Override
   public Map<String, Object> getCurrentFrame() {
      return null;
   }

   @Override
   public void maximize() {
      this.window("maximize");
   }

   @Override
   public void minimize() {
      this.window("minimize");
   }

   @Override
   public void fullscreen() {
      this.window("fullscreen");
   }

   @Override
   public void activate() {
      try {
         this.eval("window.focus()");
      } catch (Exception var2) {
         logger.debug("activate() not supported: {}", var2.getMessage());
      }
   }

   @Override
   public byte[] pdf() {
      return this.pdf(null);
   }

   @Override
   public Mouse mouse(String locator) {
      this.scroll(locator);
      Map<String, Object> pos = this.position(locator, true);
      double x = ((Number)pos.get("x")).doubleValue();
      double y = ((Number)pos.get("y")).doubleValue();
      double width = ((Number)pos.get("width")).doubleValue();
      double height = ((Number)pos.get("height")).doubleValue();
      return new W3cMouse(this.session, x + width / 2.0, y + height / 2.0);
   }

   @Override
   public Mouse mouse(Number x, Number y) {
      return new W3cMouse(this.session, x.doubleValue(), y.doubleValue());
   }

   @Override
   public void switchPage(String titleOrUrl) {
      this.tab(titleOrUrl);
   }

   @Override
   public void switchPage(int index) {
      this.tab(index);
   }

   @Override
   public void intercept(InterceptHandler handler) {
      throw new UnsupportedOperationException("Request interception not supported on WebDriver backend");
   }

   @Override
   public void stopIntercept() {
      throw new UnsupportedOperationException("Request interception not supported on WebDriver backend");
   }

   @Override
   public void close() {
      this.session.closeWindow();

      try {
         List<String> handles = this.session.getWindowHandles();
         if (handles != null && !handles.isEmpty()) {
            this.session.switchWindow(handles.get(0));
         }
      } catch (Exception var2) {
         logger.debug("No windows remaining after close: {}", var2.getMessage());
      }
   }

   @Override
   public void quit() {
      if (!this.terminated) {
         this.terminated = true;

         try {
            this.session.deleteSession();
         } catch (Exception var2) {
            logger.warn("Error deleting session: {}", var2.getMessage());
         }

         if (this.driverProcess != null) {
            this.driverProcess.close(true);
            logger.info("Driver process terminated");
         }
      }
   }

   @Override
   public boolean isTerminated() {
      return this.terminated;
   }

   @Override
   public DriverOptions getOptions() {
      return this.options;
   }

   private String findElementIdWithRetry(String locator) {
      String js = "return " + Locators.selector(locator);

      try {
         Object result = this.session.executeScript(js);
         if (W3cSession.isElementReference(result)) {
            return W3cSession.elementIdFrom(result);
         }
      } catch (Exception var4) {
         logger.warn("locator failed, will retry once: {}", var4.getMessage());
      }

      sleep(this.options.getRetryInterval());

      try {
         Object result = this.session.executeScript(js);
         if (W3cSession.isElementReference(result)) {
            return W3cSession.elementIdFrom(result);
         }
      } catch (Exception var5) {
         throw new DriverException("locator failed twice: " + locator, var5);
      }

      throw new DriverException("Element not found: " + locator);
   }

   private void ensureKjsRuntime() {
      try {
         Object exists = this.session.executeScript("return typeof window.__kjs !== 'undefined' && typeof window.__kjs.resolve === 'function'");
         if (!Boolean.TRUE.equals(exists)) {
            this.session.executeScript(DRIVER_JS);
            logger.debug("Injected __kjs runtime into browser");
         }
      } catch (Exception var2) {
         logger.debug("__kjs injection deferred: {}", var2.getMessage());
      }
   }

   static String prefixReturnIfNeeded(String js) {
      if (js != null && !js.isEmpty()) {
         String trimmed = js.stripLeading();
         if (trimmed.startsWith("return ") || trimmed.startsWith("return;")) {
            return js;
         } else if (!trimmed.startsWith("var ")
            && !trimmed.startsWith("let ")
            && !trimmed.startsWith("const ")
            && !trimmed.startsWith("if ")
            && !trimmed.startsWith("if(")
            && !trimmed.startsWith("for ")
            && !trimmed.startsWith("for(")
            && !trimmed.startsWith("while ")
            && !trimmed.startsWith("while(")
            && !trimmed.startsWith("try ")
            && !trimmed.startsWith("try{")
            && !trimmed.startsWith("switch ")
            && !trimmed.startsWith("switch(")
            && !trimmed.startsWith("throw ")) {
            int semi = indexOfTopLevelSemicolon(trimmed);
            return semi >= 0 && !trimmed.substring(semi + 1).isBlank() ? js : "return " + js;
         } else {
            return js;
         }
      } else {
         return js;
      }
   }

   static int indexOfTopLevelSemicolon(String s) {
      boolean inSingle = false;
      boolean inDouble = false;
      boolean inBacktick = false;
      boolean escape = false;
      int parenDepth = 0;
      int braceDepth = 0;
      int bracketDepth = 0;

      for (int i = 0; i < s.length(); i++) {
         char c = s.charAt(i);
         if (escape) {
            escape = false;
         } else if (c != '\\' || !inSingle && !inDouble && !inBacktick) {
            if (inSingle) {
               if (c == '\'') {
                  inSingle = false;
               }
            } else if (inDouble) {
               if (c == '"') {
                  inDouble = false;
               }
            } else if (inBacktick) {
               if (c == '`') {
                  inBacktick = false;
               }
            } else {
               switch (c) {
                  case '"':
                     inDouble = true;
                     break;
                  case '\'':
                     inSingle = true;
                     break;
                  case '(':
                     parenDepth++;
                     break;
                  case ')':
                     if (parenDepth > 0) {
                        parenDepth--;
                     }
                     break;
                  case ';':
                     if (parenDepth == 0 && braceDepth == 0 && bracketDepth == 0) {
                        return i;
                     }
                     break;
                  case '[':
                     bracketDepth++;
                     break;
                  case ']':
                     if (bracketDepth > 0) {
                        bracketDepth--;
                     }
                     break;
                  case '`':
                     inBacktick = true;
                     break;
                  case '{':
                     braceDepth++;
                     break;
                  case '}':
                     if (braceDepth > 0) {
                        braceDepth--;
                     }
               }
            }
         } else {
            escape = true;
         }
      }

      return -1;
   }

   private static boolean isTruthy(Object value) {
      if (value == null) {
         return false;
      } else if (value instanceof Boolean) {
         return (Boolean)value;
      } else if (value instanceof Number) {
         return ((Number)value).doubleValue() != 0.0;
      } else {
         return value instanceof String ? !((String)value).isEmpty() : true;
      }
   }

   private static void sleep(int millis) {
      try {
         Thread.sleep(millis);
      } catch (InterruptedException var2) {
         Thread.currentThread().interrupt();
      }
   }

   private static void waitForPort(String host, int port, long timeoutMillis) {
      long deadline = System.currentTimeMillis() + timeoutMillis;

      while (System.currentTimeMillis() < deadline) {
         try {
            Socket socket = new Socket(host, port);
            socket.close();
            return;
         } catch (IOException var9) {
            sleep(200);
         }
      }

      throw new DriverException("Timeout waiting for WebDriver on " + host + ":" + port);
   }

   private static class W3cDialog implements Dialog {
      private final W3cSession session;
      private final String text;
      private boolean handled = false;

      W3cDialog(W3cSession session, String text) {
         this.session = session;
         this.text = text;
      }

      @Override
      public String getMessage() {
         return this.text;
      }

      @Override
      public String getType() {
         return "alert";
      }

      @Override
      public String getDefaultPrompt() {
         return null;
      }

      @Override
      public void accept() {
         this.session.acceptAlert();
         this.handled = true;
      }

      @Override
      public void accept(String promptText) {
         if (promptText != null) {
            this.session.sendAlertText(promptText);
         }

         this.session.acceptAlert();
         this.handled = true;
      }

      @Override
      public void dismiss() {
         this.session.dismissAlert();
         this.handled = true;
      }

      @Override
      public boolean isHandled() {
         return this.handled;
      }
   }
}
