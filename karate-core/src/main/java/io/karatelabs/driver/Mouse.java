package io.karatelabs.driver;

public interface Mouse {
   Mouse move(double var1, double var3);

   Mouse offset(double var1, double var3);

   Mouse click();

   Mouse doubleClick();

   Mouse rightClick();

   Mouse down();

   Mouse up();

   Mouse wheel(double var1, double var3);

   Mouse scrollDown(double var1);

   Mouse scrollUp(double var1);

   Mouse scrollRight(double var1);

   Mouse scrollLeft(double var1);

   Mouse dragTo(double var1, double var3);

   double getX();

   double getY();
}
