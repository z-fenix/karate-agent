package io.karatelabs.core;

import io.karatelabs.common.StringUtils;
import io.karatelabs.common.Xml;
import io.karatelabs.js.JavaInvokable;
import io.karatelabs.js.SimpleObject;
import io.karatelabs.output.LogContext;
import io.karatelabs.output.LogLevel;
import java.util.List;
import java.util.Map;
import org.w3c.dom.Node;

class KarateJsLog implements SimpleObject {
   private static final LogContext.LogWriter log = LogContext.with(LogContext.SCENARIO_LOGGER);

   @Override
   public Object jsGet(String key) {
      return switch (key) {
         case "trace" -> this.logMethod(LogLevel.TRACE);
         case "debug" -> this.logMethod(LogLevel.DEBUG);
         case "info" -> this.logMethod(LogLevel.INFO);
         case "warn" -> this.logMethod(LogLevel.WARN);
         case "error" -> this.logMethod(LogLevel.ERROR);
         default -> null;
      };
   }

   private JavaInvokable logMethod(LogLevel level) {
      return args -> {
         if (!level.isEnabled(LogContext.getLogLevel())) {
            return null;
         } else {
            StringBuilder sb = new StringBuilder();
            if (level != LogLevel.INFO) {
               sb.append("[").append(level).append("] ");
            }

            for (int i = 0; i < args.length; i++) {
               if (i > 0) {
                  sb.append(" ");
               }

               Object val = args[i];
               if (val instanceof Node) {
                  sb.append(Xml.toString((Node)val, true));
               } else if (!(val instanceof Map) && !(val instanceof List)) {
                  sb.append(val);
               } else {
                  sb.append(StringUtils.formatJson(val));
               }
            }

            switch (level) {
               case TRACE:
                  log.trace(sb.toString());
                  break;
               case DEBUG:
                  log.debug(sb.toString());
                  break;
               case INFO:
                  log.info(sb.toString());
                  break;
               case WARN:
                  log.warn(sb.toString());
                  break;
               case ERROR:
                  log.error(sb.toString());
            }

            return null;
         }
      };
   }
}
