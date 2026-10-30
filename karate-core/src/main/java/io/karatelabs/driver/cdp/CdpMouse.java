package io.karatelabs.driver.cdp;

import io.karatelabs.driver.Mouse;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CdpMouse implements Mouse {
   private static final Logger logger = LoggerFactory.getLogger(CdpMouse.class);
   private final CdpClient cdp;
   private double x;
   private double y;

   public CdpMouse(CdpClient cdp) {
      this(cdp, 0.0, 0.0);
   }

   public CdpMouse(CdpClient cdp, double x, double y) {
      this.cdp = cdp;
      this.x = x;
      this.y = y;
      this.dispatchEvent("mouseMoved");
   }

   @Override
   public Mouse move(double x, double y) {
      this.x = x;
      this.y = y;
      this.dispatchEvent("mouseMoved");
      return this;
   }

   @Override
   public Mouse offset(double dx, double dy) {
      return this.move(this.x + dx, this.y + dy);
   }

   @Override
   public Mouse click() {
      this.dispatchEvent("mouseMoved");
      sleep(5L);
      this.down();
      sleep(5L);
      this.up();
      return this;
   }

   @Override
   public Mouse doubleClick() {
      this.click();
      sleep(5L);
      this.dispatchEvent("mousePressed", Map.of("clickCount", 2));
      sleep(5L);
      this.dispatchEvent("mouseReleased", Map.of("clickCount", 2));
      return this;
   }

   @Override
   public Mouse rightClick() {
      this.dispatchEvent("mousePressed", Map.of("button", "right"));
      sleep(5L);
      this.dispatchEvent("mouseReleased", Map.of("button", "right"));
      return this;
   }

   @Override
   public Mouse down() {
      this.dispatchEvent("mousePressed");
      return this;
   }

   @Override
   public Mouse up() {
      this.dispatchEvent("mouseReleased");
      return this;
   }

   @Override
   public Mouse wheel(double deltaX, double deltaY) {
      this.cdp
         .method("Input.dispatchMouseEvent")
         .param("type", "mouseWheel")
         .param("x", this.x)
         .param("y", this.y)
         .param("deltaX", deltaX)
         .param("deltaY", deltaY)
         .sendWithoutWaiting();
      return this;
   }

   @Override
   public Mouse scrollDown(double amount) {
      return this.wheel(0.0, amount);
   }

   @Override
   public Mouse scrollUp(double amount) {
      return this.wheel(0.0, -amount);
   }

   @Override
   public Mouse scrollRight(double amount) {
      return this.wheel(amount, 0.0);
   }

   @Override
   public Mouse scrollLeft(double amount) {
      return this.wheel(-amount, 0.0);
   }

   @Override
   public Mouse dragTo(double targetX, double targetY) {
      this.down();
      sleep(5L);
      this.move(targetX, targetY);
      sleep(5L);
      this.up();
      return this;
   }

   @Override
   public double getX() {
      return this.x;
   }

   @Override
   public double getY() {
      return this.y;
   }

   private void dispatchEvent(String type) {
      this.dispatchEvent(type, Map.of());
   }

   private void dispatchEvent(String type, Map<String, Object> extra) {
      CdpMessage message = this.cdp.method("Input.dispatchMouseEvent").param("type", type).param("x", this.x).param("y", this.y);
      if (type.equals("mousePressed")) {
         if (!extra.containsKey("button")) {
            message.param("button", "left");
         }

         if (!extra.containsKey("clickCount")) {
            message.param("clickCount", 1);
         }

         if (!extra.containsKey("buttons")) {
            message.param("buttons", 1);
         }
      } else if (type.equals("mouseReleased")) {
         if (!extra.containsKey("button")) {
            message.param("button", "left");
         }

         if (!extra.containsKey("clickCount")) {
            message.param("clickCount", 1);
         }

         if (!extra.containsKey("buttons")) {
            message.param("buttons", 0);
         }
      } else {
         message.param("button", "none");
         message.param("buttons", 0);
      }

      extra.forEach(message::param);
      message.sendWithoutWaiting();
   }

   private static void sleep(long millis) {
      try {
         Thread.sleep(millis);
      } catch (InterruptedException var3) {
         Thread.currentThread().interrupt();
      }
   }
}
