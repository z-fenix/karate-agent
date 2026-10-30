package io.karatelabs.driver;

import io.karatelabs.js.JavaCallable;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class BaseElement implements Element {
   protected final Driver driver;
   protected final String locator;
   protected final boolean exists;

   public BaseElement(Driver driver, String locator, boolean exists) {
      this.driver = driver;
      this.locator = locator;
      this.exists = exists;
   }

   public static BaseElement of(Driver driver, String locator) {
      boolean exists = driver.exists(locator);
      return new BaseElement(driver, locator, exists);
   }

   public static BaseElement optional(Driver driver, String locator) {
      boolean exists = driver.exists(locator);
      return (BaseElement)(exists ? new BaseElement(driver, locator, true) : new MissingElement(driver, locator));
   }

   public static BaseElement existing(Driver driver, String locator) {
      return new BaseElement(driver, locator, true);
   }

   @Override
   public boolean exists() {
      return this.exists;
   }

   @Override
   public boolean isPresent() {
      return this.exists;
   }

   @Override
   public String getLocator() {
      return this.locator;
   }

   @Override
   public String text() {
      this.assertExists();
      return this.driver.text(this.locator);
   }

   @Override
   public String html() {
      this.assertExists();
      return this.driver.html(this.locator);
   }

   @Override
   public String innerHtml() {
      this.assertExists();
      return (String)this.driver.script(Locators.innerHtmlJs(this.locator));
   }

   @Override
   public String value() {
      this.assertExists();
      return this.driver.value(this.locator);
   }

   @Override
   public String attribute(String name) {
      this.assertExists();
      return this.driver.attribute(this.locator, name);
   }

   @Override
   public Object property(String name) {
      this.assertExists();
      return this.driver.property(this.locator, name);
   }

   @Override
   public boolean enabled() {
      this.assertExists();
      return this.driver.enabled(this.locator);
   }

   @Override
   public Map<String, Object> position() {
      this.assertExists();
      return this.driver.position(this.locator);
   }

   @Override
   public Map<String, Object> position(boolean relative) {
      this.assertExists();
      return this.driver.position(this.locator, relative);
   }

   @Override
   public Element click() {
      this.assertExists();
      this.driver.click(this.locator);
      return this;
   }

   @Override
   public Element focus() {
      this.assertExists();
      this.driver.focus(this.locator);
      return this;
   }

   @Override
   public Element clear() {
      this.assertExists();
      this.driver.clear(this.locator);
      return this;
   }

   @Override
   public Element input(String value) {
      this.assertExists();
      this.driver.input(this.locator, value);
      return this;
   }

   @Override
   public Element inputFile(String... files) {
      this.assertExists();
      this.driver.inputFile(this.locator, files);
      return this;
   }

   @Override
   public Element value(String value) {
      this.assertExists();
      this.driver.value(this.locator, value);
      return this;
   }

   @Override
   public Element select(String text) {
      this.assertExists();
      this.driver.select(this.locator, text);
      return this;
   }

   @Override
   public Element select(int index) {
      this.assertExists();
      this.driver.select(this.locator, index);
      return this;
   }

   @Override
   public Element scroll() {
      this.assertExists();
      this.driver.scroll(this.locator);
      return this;
   }

   @Override
   public Element highlight() {
      this.assertExists();
      this.driver.highlight(this.locator);
      return this;
   }

   @Override
   public Element submit() {
      this.driver.submit();
      return this;
   }

   @Override
   public Element locate(String childLocator) {
      this.assertExists();
      if (isPureJsLocator(this.locator)) {
         return of(this.driver, Locators.scopedSelectorJs(this.locator, childLocator));
      } else {
         String fullLocator = this.locator + " " + childLocator;
         return of(this.driver, fullLocator);
      }
   }

   @Override
   public List<Element> locateAll(String childLocator) {
      this.assertExists();
      if (!isPureJsLocator(this.locator)) {
         String fullLocator = this.locator + " " + childLocator;
         return this.driver.locateAll(fullLocator);
      } else {
         Object countResult = this.driver.script(Locators.scopedCountJs(this.locator, childLocator));
         int count = countResult instanceof Number ? ((Number)countResult).intValue() : 0;
         List<Element> elements = new ArrayList<>();

         for (int i = 0; i < count; i++) {
            String indexedJs = Locators.scopedIndexedSelectorJs(this.locator, childLocator, i);
            elements.add(new BaseElement(this.driver, indexedJs, true));
         }

         return elements;
      }
   }

   protected static boolean isPureJsLocator(String locator) {
      return locator.startsWith("(") && !locator.startsWith("(//");
   }

   @Override
   public Element closest(String selector) {
      this.assertExists();
      return of(this.driver, Locators.closestJs(this.locator, selector));
   }

   @Override
   public boolean matches(String selector) {
      this.assertExists();
      Object result = this.driver.script(Locators.matchesJs(this.locator, selector));
      return Boolean.TRUE.equals(result);
   }

   @Override
   public Object script(String expression) {
      this.assertExists();
      return this.driver.script(this.locator, expression);
   }

   @Override
   public Element waitFor() {
      this.driver.waitFor(this.locator);
      return this;
   }

   @Override
   public Element waitForText(String expected) {
      this.driver.waitForText(this.locator, expected);
      return this;
   }

   @Override
   public Element waitForEnabled() {
      this.driver.waitForEnabled(this.locator);
      return this;
   }

   @Override
   public Element waitUntil(String expression) {
      this.driver.waitUntil(this.locator, expression);
      return this;
   }

   @Override
   public Element retry() {
      return new BaseElement.RetryElement(this.driver, this.locator, this.exists, null, null);
   }

   @Override
   public Element retry(int count) {
      return new BaseElement.RetryElement(this.driver, this.locator, this.exists, count, null);
   }

   @Override
   public Element retry(int count, int interval) {
      return new BaseElement.RetryElement(this.driver, this.locator, this.exists, count, interval);
   }

   @Override
   public Object jsGet(String name) {
      return switch (name) {
         case "present" -> this.isPresent();
         case "exists" -> (JavaCallable) (ctx, args) -> this.exists();
         case "locator" -> (JavaCallable) (ctx, args) -> this.locator;
         case "text" -> (JavaCallable) (ctx, args) -> this.text();
         case "html" -> (JavaCallable) (ctx, args) -> this.html();
         case "value" -> (JavaCallable) (ctx, args) -> args.length > 0 ? this.value(String.valueOf(args[0])) : this.value();
         case "enabled" -> (JavaCallable) (ctx, args) -> this.enabled();
         case "position" -> (JavaCallable) (ctx, args) -> args.length > 0 && Boolean.TRUE.equals(args[0]) ? this.position(true) : this.position();
         case "click" -> (JavaCallable) (ctx, args) -> this.click();
         case "focus" -> (JavaCallable) (ctx, args) -> this.focus();
         case "clear" -> (JavaCallable) (ctx, args) -> this.clear();
         case "scroll" -> (JavaCallable) (ctx, args) -> this.scroll();
         case "highlight" -> (JavaCallable) (ctx, args) -> this.highlight();
         case "input" -> (JavaCallable) (ctx, args) -> this.input(args.length > 0 ? String.valueOf(args[0]) : "");
         case "inputFile" -> (JavaCallable) (ctx, args) -> {
            List<String> files = new ArrayList<>();

            for (Object arg : args) {
               if (arg instanceof List<?> list) {
                  list.forEach(o -> files.add(String.valueOf(o)));
               } else {
                  files.add(String.valueOf(arg));
               }
            }

            return this.inputFile(files.toArray(new String[0]));
         };
         case "attribute" -> (JavaCallable) (ctx, args) -> this.attribute(args.length > 0 ? String.valueOf(args[0]) : "");
         case "property" -> (JavaCallable) (ctx, args) -> this.property(args.length > 0 ? String.valueOf(args[0]) : "");
         case "script" -> (JavaCallable) (ctx, args) -> this.script(args.length > 0 ? String.valueOf(args[0]) : "");
         case "closest" -> (JavaCallable) (ctx, args) -> this.closest(args.length > 0 ? String.valueOf(args[0]) : "");
         case "matches" -> (JavaCallable) (ctx, args) -> this.matches(args.length > 0 ? String.valueOf(args[0]) : "");
         case "retry" -> (JavaCallable) (ctx, args) -> {
            return switch (args.length) {
               case 0 -> (Object) this.retry();
               case 1 -> (Object) this.retry(((Number)args[0]).intValue());
               default -> (Object) this.retry(((Number)args[0]).intValue(), ((Number)args[1]).intValue());
            };
         };
         default -> null;
      };
   }

   protected void assertExists() {
      if (!this.exists) {
         throw new DriverException("element not found: " + this.locator);
      }
   }

   @Override
   public String toString() {
      return "Element[" + this.locator + ", exists=" + this.exists + "]";
   }

   private static class RetryElement extends BaseElement {
      private final Duration timeout;

      RetryElement(Driver driver, String locator, boolean exists, Integer count, Integer interval) {
         super(driver, locator, exists);
         this.timeout = RetryableDriver.timeout(driver, count, interval);
      }

      @Override
      protected void assertExists() {
         this.driver.waitFor(this.locator, this.timeout);
      }

      @Override
      public boolean exists() {
         return this.driver.exists(this.locator);
      }

      @Override
      public boolean isPresent() {
         return this.exists();
      }

      @Override
      public Element waitFor() {
         this.driver.waitFor(this.locator, this.timeout);
         return this;
      }

      @Override
      public Element waitForText(String expected) {
         this.driver.waitForText(this.locator, expected, this.timeout);
         return this;
      }

      @Override
      public Element waitForEnabled() {
         this.driver.waitForEnabled(this.locator, this.timeout);
         return this;
      }

      @Override
      public Element waitUntil(String expression) {
         this.driver.waitUntil(this.locator, expression, this.timeout);
         return this;
      }
   }
}
