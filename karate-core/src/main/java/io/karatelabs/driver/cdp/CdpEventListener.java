package io.karatelabs.driver.cdp;

import java.util.Map;

public interface CdpEventListener {
   void onEvent(String var1, Map<String, Object> var2);
}
