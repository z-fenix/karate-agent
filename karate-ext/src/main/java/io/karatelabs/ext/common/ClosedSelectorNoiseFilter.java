package io.karatelabs.ext.common;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.turbo.TurboFilter;
import ch.qos.logback.core.spi.FilterReply;
import java.nio.channels.ClosedSelectorException;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;
import org.slf4j.Marker;

public class ClosedSelectorNoiseFilter extends TurboFilter {
   static final String HC5_ASYNC = "org.apache.hc.client5.http.impl.async";

   public FilterReply decide(Marker var1, Logger var2, Level var3, String var4, Object[] var5, Throwable var6) {
      if (var2 != null && "org.apache.hc.client5.http.impl.async".equals(var2.getName())) {
         Set<java.lang.Throwable> var7 = Collections.newSetFromMap(new IdentityHashMap());

         for (Throwable var8 = var6; var8 != null && var7.add(var8); var8 = var8.getCause()) {
            if (var8 instanceof ClosedSelectorException) {
               return FilterReply.DENY;
            }
         }

         return FilterReply.NEUTRAL;
      } else {
         return FilterReply.NEUTRAL;
      }
   }
}
