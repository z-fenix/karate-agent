package io.karatelabs.ext.max.run;

import io.karatelabs.common.Json;
import io.karatelabs.core.RunEvent;
import io.karatelabs.core.RunEventType;
import io.karatelabs.core.RunListener;
import java.util.LinkedHashMap;
import java.util.function.BiConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class RunEventSseBridge implements RunListener {
   private static final Logger logger = LoggerFactory.getLogger(RunEventSseBridge.class);
   private final BiConsumer<String, String> sink;

   public RunEventSseBridge(BiConsumer<String, String> var1) {
      this.sink = var1;
   }

   private static String sseName(RunEventType var0) {
      return switch (var0) {
         case FEATURE_ENTER -> "feature_enter";
         case FEATURE_EXIT -> "feature_exit";
         case SCENARIO_ENTER -> "scenario_enter";
         case SCENARIO_EXIT -> "scenario_exit";
         case STEP_EXIT -> "step";
         case ERROR -> "error";
         default -> null;
      };
   }

   @Override
   public boolean onEvent(RunEvent var1) {
      String var2 = sseName(var1.getType());
      if (var2 == null) {
         return true;
      } else {
         try {
            var var3 = new LinkedHashMap<>(var1.toJson());
            var3.put("type", var2);
            var3.put("ts", var1.getTimeStamp());
            this.sink.accept(var2, Json.stringifyStrict(var3));
         } catch (RuntimeException var4) {
            logger.debug("could not project run event {}: {}", var1.getType(), var4.getMessage());
         }

         return true;
      }
   }
}
