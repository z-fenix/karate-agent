package io.karatelabs.driver;

import java.time.Duration;

public interface DriverOptions {
   int getTimeout();

   void setTimeout(int var1);

   Duration getTimeoutDuration();

   int getRetryCount();

   int getRetryInterval();

   boolean isHeadless();

   boolean isScreenshotOnFailure();

   boolean isHighlight();

   int getHighlightDuration();

   PageLoadStrategy getPageLoadStrategy();

   String getPreSubmitHash();

   void setPreSubmitHash(String var1);

   default boolean isStop() {
      return true;
   }
}
