package io.karatelabs.driver;

import java.util.List;
import java.util.Map;

public class MissingElement extends BaseElement {
   public MissingElement(Driver driver, String locator) {
      super(driver, locator, false);
   }

   @Override
   public String text() {
      return null;
   }

   @Override
   public String html() {
      return null;
   }

   @Override
   public String innerHtml() {
      return null;
   }

   @Override
   public String value() {
      return null;
   }

   @Override
   public String attribute(String name) {
      return null;
   }

   @Override
   public Object property(String name) {
      return null;
   }

   @Override
   public boolean enabled() {
      return false;
   }

   @Override
   public Map<String, Object> position() {
      return null;
   }

   @Override
   public Map<String, Object> position(boolean relative) {
      return null;
   }

   @Override
   public Element click() {
      return this;
   }

   @Override
   public Element focus() {
      return this;
   }

   @Override
   public Element clear() {
      return this;
   }

   @Override
   public Element input(String value) {
      return this;
   }

   @Override
   public Element inputFile(String... files) {
      return this;
   }

   @Override
   public Element value(String value) {
      return this;
   }

   @Override
   public Element select(String text) {
      return this;
   }

   @Override
   public Element select(int index) {
      return this;
   }

   @Override
   public Element scroll() {
      return this;
   }

   @Override
   public Element highlight() {
      return this;
   }

   @Override
   public Element submit() {
      return this;
   }

   @Override
   public Element locate(String childLocator) {
      String child = isPureJsLocator(this.locator) ? Locators.scopedSelectorJs(this.locator, childLocator) : this.locator + " " + childLocator;
      return new MissingElement(this.driver, child);
   }

   @Override
   public List<Element> locateAll(String childLocator) {
      return List.of();
   }

   @Override
   public Element closest(String selector) {
      return new MissingElement(this.driver, Locators.closestJs(this.locator, selector));
   }

   @Override
   public boolean matches(String selector) {
      return false;
   }

   @Override
   public Object script(String expression) {
      return null;
   }

   @Override
   public Element retry() {
      return this;
   }

   @Override
   public Element retry(int count) {
      return this;
   }

   @Override
   public Element retry(int count, int interval) {
      return this;
   }

   @Override
   public Element waitFor() {
      return this.driver.waitFor(this.locator);
   }

   @Override
   public Element waitForText(String expected) {
      return this.driver.waitForText(this.locator, expected);
   }

   @Override
   public Element waitForEnabled() {
      return this.driver.waitForEnabled(this.locator);
   }

   @Override
   public Element waitUntil(String expression) {
      return this.driver.waitUntil(this.locator, expression);
   }

   @Override
   public String toString() {
      return "MissingElement[" + this.locator + "]";
   }
}
