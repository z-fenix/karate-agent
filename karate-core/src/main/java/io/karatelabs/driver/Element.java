package io.karatelabs.driver;

import io.karatelabs.js.SimpleObject;
import java.util.List;
import java.util.Map;

public interface Element extends SimpleObject {
   boolean exists();

   boolean isPresent();

   String getLocator();

   String text();

   String html();

   String innerHtml();

   String value();

   String attribute(String var1);

   Object property(String var1);

   boolean enabled();

   Map<String, Object> position();

   Map<String, Object> position(boolean var1);

   Element click();

   Element focus();

   Element clear();

   Element input(String var1);

   Element inputFile(String... var1);

   Element value(String var1);

   Element select(String var1);

   Element select(int var1);

   Element scroll();

   Element highlight();

   Element submit();

   Element locate(String var1);

   List<Element> locateAll(String var1);

   Element closest(String var1);

   boolean matches(String var1);

   Object script(String var1);

   Element waitFor();

   Element waitForText(String var1);

   Element waitForEnabled();

   Element waitUntil(String var1);

   Element retry();

   Element retry(int var1);

   Element retry(int var1, int var2);
}
