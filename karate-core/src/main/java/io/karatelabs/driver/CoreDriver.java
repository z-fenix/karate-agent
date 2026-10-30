package io.karatelabs.driver;

import java.util.List;
import java.util.Map;

public interface CoreDriver {
   Object eval(String var1);

   void navigate(String var1);

   Keys keys();

   byte[] screenshot();

   void frame(Object var1);

   void dialog(boolean var1, String var2);

   Mouse mouse();

   byte[] pdf(Map<String, Object> var1);

   Map<String, Object> cookie(String var1);

   void cookie(Map<String, Object> var1);

   void intercept(List<String> var1, InterceptHandler var2);

   void window(String var1);

   void tab(Object var1);

   void quit();

   boolean isTerminated();

   DriverOptions getOptions();
}
