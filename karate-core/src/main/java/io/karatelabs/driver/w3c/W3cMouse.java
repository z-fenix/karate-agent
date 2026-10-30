package io.karatelabs.driver.w3c;

import io.karatelabs.driver.Mouse;
import java.util.List;
import java.util.Map;

public class W3cMouse implements Mouse {
   private final W3cSession session;
   private double x;
   private double y;

   public W3cMouse(W3cSession session) {
      this.session = session;
      this.x = 0.0;
      this.y = 0.0;
   }

   public W3cMouse(W3cSession session, double x, double y) {
      this.session = session;
      this.move(x, y);
   }

   @Override
   public Mouse move(double x, double y) {
      this.x = x;
      this.y = y;
      this.dispatchPointerAction(Map.of("type", "pointerMove", "duration", 100, "x", (int)x, "y", (int)y, "origin", "viewport"));
      return this;
   }

   @Override
   public Mouse offset(double dx, double dy) {
      this.x += dx;
      this.y += dy;
      this.dispatchPointerAction(Map.of("type", "pointerMove", "duration", 100, "x", (int)dx, "y", (int)dy, "origin", "pointer"));
      return this;
   }

   @Override
   public Mouse click() {
      this.dispatchPointerActions(List.of(Map.of("type", "pointerDown", "button", 0), Map.of("type", "pointerUp", "button", 0)));
      return this;
   }

   @Override
   public Mouse doubleClick() {
      this.dispatchPointerActions(
         List.of(
            Map.of("type", "pointerDown", "button", 0),
            Map.of("type", "pointerUp", "button", 0),
            Map.of("type", "pointerDown", "button", 0),
            Map.of("type", "pointerUp", "button", 0)
         )
      );
      return this;
   }

   @Override
   public Mouse rightClick() {
      this.dispatchPointerActions(List.of(Map.of("type", "pointerDown", "button", 2), Map.of("type", "pointerUp", "button", 2)));
      return this;
   }

   @Override
   public Mouse down() {
      this.dispatchPointerAction(Map.of("type", "pointerDown", "button", 0));
      return this;
   }

   @Override
   public Mouse up() {
      this.dispatchPointerAction(Map.of("type", "pointerUp", "button", 0));
      return this;
   }

   @Override
   public Mouse wheel(double deltaX, double deltaY) {
      Map<String, Object> wheelInput = Map.of(
         "type",
         "wheel",
         "id",
         "wheel",
         "actions",
         List.of(Map.of("type", "scroll", "x", (int)this.x, "y", (int)this.y, "deltaX", (int)deltaX, "deltaY", (int)deltaY, "duration", 100))
      );
      this.session.performActions(List.of(wheelInput));
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
      this.dispatchPointerActions(
         List.of(
            Map.of("type", "pointerDown", "button", 0),
            Map.of("type", "pointerMove", "duration", 500, "x", (int)targetX, "y", (int)targetY, "origin", "viewport"),
            Map.of("type", "pointerUp", "button", 0)
         )
      );
      this.x = targetX;
      this.y = targetY;
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

   private void dispatchPointerAction(Map<String, Object> action) {
      this.dispatchPointerActions(List.of(action));
   }

   private void dispatchPointerActions(List<Map<String, Object>> actions) {
      Map<String, Object> pointerInput = Map.of("type", "pointer", "id", "mouse", "parameters", Map.of("pointerType", "mouse"), "actions", actions);
      this.session.performActions(List.of(pointerInput));
   }
}
