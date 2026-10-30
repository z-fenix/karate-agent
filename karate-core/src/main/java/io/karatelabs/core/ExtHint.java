package io.karatelabs.core;

import io.karatelabs.js.EngineException;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ExtHint {
   private static final Pattern NOT_DEFINED = Pattern.compile("([A-Za-z_$][\\w$]*) is not defined");

   private ExtHint() {
   }

   public static Throwable decorate(Throwable error, Suite suite) {
      String hint = forError(error, suite);
      if (hint == null) {
         return error;
      } else {
         String message = error.getMessage() == null ? error.toString() : error.getMessage();
         RuntimeException decorated = new RuntimeException(message + "\n" + hint, error);
         decorated.setStackTrace(error.getStackTrace());
         return decorated;
      }
   }

   public static String forError(Throwable error, Suite suite) {
      String missing = missingGlobal(error);
      if (missing == null) {
         return null;
      } else if (suite != null && suite.getGlobals() != null && suite.getGlobals().containsKey(missing)) {
         return null;
      } else {
         for (String candidate : extCandidates(missing)) {
            if (BootBinding.extAvailable(candidate)) {
               return "hint: an ext named '"
                  + candidate
                  + "' is on the classpath but this run never booted it — an ext binds its globals only when the project declares it. Add boot.ext('"
                  + candidate
                  + "') to karate-boot.js at the project root.";
            }
         }

         return null;
      }
   }

   private static String missingGlobal(Throwable error) {
      for (Throwable current = error; current != null; current = current.getCause()) {
         if (current instanceof EngineException ee && "ReferenceError".equals(ee.getJsErrorName())) {
            String jsMessage = ee.getJsMessage();
            if (jsMessage != null) {
               Matcher m = NOT_DEFINED.matcher(jsMessage);
               if (m.find()) {
                  return m.group(1);
               }
            }
         }
      }

      return null;
   }

   private static Set<String> extCandidates(String global) {
      String lower = global.toLowerCase();
      Set<String> candidates = new LinkedHashSet<>();
      candidates.add(lower);
      if (lower.endsWith("s")) {
         candidates.add(lower.substring(0, lower.length() - 1));
      } else {
         candidates.add(lower + "s");
      }

      return candidates;
   }
}
