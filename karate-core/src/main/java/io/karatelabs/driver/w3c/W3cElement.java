package io.karatelabs.driver.w3c;

import io.karatelabs.driver.BaseElement;
import io.karatelabs.driver.Driver;
import io.karatelabs.driver.Element;

public class W3cElement extends BaseElement {
   private final String elementId;
   private final W3cSession session;

   public W3cElement(Driver driver, String locator, boolean exists, String elementId, W3cSession session) {
      super(driver, locator, exists);
      this.elementId = elementId;
      this.session = session;
   }

   public String getElementId() {
      return this.elementId;
   }

   @Override
   public Element click() {
      this.assertExists();
      this.session.clickElement(this.elementId);
      return this;
   }

   @Override
   public Element clear() {
      this.assertExists();
      this.session.clearElement(this.elementId);
      return this;
   }

   @Override
   public String text() {
      this.assertExists();
      return this.session.getElementText(this.elementId);
   }

   @Override
   public String attribute(String name) {
      this.assertExists();
      return this.session.getElementAttribute(this.elementId, name);
   }

   @Override
   public boolean enabled() {
      this.assertExists();
      return this.session.isElementEnabled(this.elementId);
   }

   @Override
   public String toString() {
      return "W3cElement[" + this.getLocator() + ", id=" + this.elementId + ", exists=" + this.exists() + "]";
   }
}
