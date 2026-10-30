package io.karatelabs.driver;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Finder {
   private final Driver driver;
   private final String referenceLocator;
   private final Finder.Position position;
   private final double tolerance;
   private static final double DEFAULT_TOLERANCE = 50.0;

   public Finder(Driver driver, String referenceLocator, Finder.Position position) {
      this(driver, referenceLocator, position, 50.0);
   }

   Finder(Driver driver, String referenceLocator, Finder.Position position, double tolerance) {
      this.driver = driver;
      this.referenceLocator = referenceLocator;
      this.position = position;
      this.tolerance = tolerance;
   }

   public Finder within(double pixels) {
      return new Finder(this.driver, this.referenceLocator, this.position, pixels);
   }

   public Element find(String locator) {
      List<Element> matches = this.findAll(locator);
      if (matches.isEmpty()) {
         throw new DriverException("no element found " + this.positionDescription() + " " + this.referenceLocator + " matching: " + locator);
      } else {
         return matches.get(0);
      }
   }

   public List<Element> findAll(String locator) {
      Map<String, Object> refPos = this.driver.position(this.referenceLocator, true);
      double refX = ((Number)refPos.get("x")).doubleValue();
      double refY = ((Number)refPos.get("y")).doubleValue();
      double refWidth = ((Number)refPos.get("width")).doubleValue();
      double refHeight = ((Number)refPos.get("height")).doubleValue();
      double refCenterX = refX + refWidth / 2.0;
      double refCenterY = refY + refHeight / 2.0;
      double refRight = refX + refWidth;
      double refBottom = refY + refHeight;
      List<Element> candidates = this.driver.locateAll(locator);
      List<Finder.ElementWithDistance> matches = new ArrayList<>();

      for (Element candidate : candidates) {
         try {
            Map<String, Object> pos = candidate.position(true);
            if (pos != null) {
               double x = ((Number)pos.get("x")).doubleValue();
               double y = ((Number)pos.get("y")).doubleValue();
               double width = ((Number)pos.get("width")).doubleValue();
               double height = ((Number)pos.get("height")).doubleValue();
               if (!this.samePosition(x, y, width, height, refX, refY, refWidth, refHeight)) {
                  double centerX = x + width / 2.0;
                  double centerY = y + height / 2.0;
                  double right = x + width;
                  double bottom = y + height;
                  boolean match = false;
                  double distance = 0.0;
                  switch (this.position) {
                     case RIGHT_OF:
                        if (x >= refRight && this.verticalOverlap(refY, refBottom, y, bottom)) {
                           match = true;
                           distance = x - refRight;
                        }
                        break;
                     case LEFT_OF:
                        if (right <= refX && this.verticalOverlap(refY, refBottom, y, bottom)) {
                           match = true;
                           distance = refX - right;
                        }
                        break;
                     case ABOVE:
                        if (bottom <= refY && this.horizontalOverlap(refX, refRight, x, right)) {
                           match = true;
                           distance = refY - bottom;
                        }
                        break;
                     case BELOW:
                        if (y >= refBottom && this.horizontalOverlap(refX, refRight, x, right)) {
                           match = true;
                           distance = y - refBottom;
                        }
                        break;
                     case NEAR:
                        double dx = centerX - refCenterX;
                        double dy = centerY - refCenterY;
                        distance = Math.sqrt(dx * dx + dy * dy);
                        if (distance <= this.tolerance) {
                           match = true;
                        }
                  }

                  if (match) {
                     matches.add(new Finder.ElementWithDistance(candidate, distance));
                  }
               }
            }
         } catch (Exception var47) {
         }
      }

      matches.sort((a, b) -> Double.compare(a.distance, b.distance));
      List<Element> result = new ArrayList<>();

      for (Finder.ElementWithDistance ewd : matches) {
         result.add(ewd.element);
      }

      return result;
   }

   public Element click(String locator) {
      Element element = this.find(locator);
      element.click();
      return element;
   }

   public boolean exists(String locator) {
      return !this.findAll(locator).isEmpty();
   }

   private boolean verticalOverlap(double y1, double bottom1, double y2, double bottom2) {
      return bottom1 > y2 && bottom2 > y1;
   }

   private boolean horizontalOverlap(double x1, double right1, double x2, double right2) {
      return right1 > x2 && right2 > x1;
   }

   private boolean samePosition(double x, double y, double w, double h, double refX, double refY, double refW, double refH) {
      return Math.abs(x - refX) < 1.0 && Math.abs(y - refY) < 1.0 && Math.abs(w - refW) < 1.0 && Math.abs(h - refH) < 1.0;
   }

   private String positionDescription() {
      return switch (this.position) {
         case RIGHT_OF -> "to the right of";
         case LEFT_OF -> "to the left of";
         case ABOVE -> "above";
         case BELOW -> "below";
         case NEAR -> "near";
      };
   }

   private static class ElementWithDistance {
      final Element element;
      final double distance;

      ElementWithDistance(Element element, double distance) {
         this.element = element;
         this.distance = distance;
      }
   }

   public static enum Position {
      RIGHT_OF,
      LEFT_OF,
      ABOVE,
      BELOW,
      NEAR;
   }
}
