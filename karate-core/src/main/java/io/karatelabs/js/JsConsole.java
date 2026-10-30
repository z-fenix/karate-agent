package io.karatelabs.js;

import java.util.LinkedHashMap;
import java.util.Map;

class JsConsole implements SimpleObject {
   final ContextRoot root;
   private final Map<String, Integer> counts = new LinkedHashMap<>();
   private final Map<String, Long> timers = new LinkedHashMap<>();

   JsConsole(ContextRoot root) {
      this.root = root;
   }

   private String argsToString(Context context, Object[] args) {
      StringBuilder sb = new StringBuilder();

      for (int i = 0; i < args.length; i++) {
         if (i > 0) {
            sb.append(' ');
         }

         sb.append(args[i] instanceof JsSymbol sym ? sym.toString() : Terms.toStringCoerce(args[i], (CoreContext)context));
      }

      return sb.toString();
   }

   private void output(String message) {
      if (this.root.onConsoleLog != null) {
         this.root.onConsoleLog.accept(message);
      } else {
         System.out.println(message);
      }
   }

   @Override
   public Object jsGet(String name) {
      return switch (name) {
         case "log", "info", "debug", "warn", "error" -> (JsCallable) (context, args) -> {
            this.output(this.argsToString(context, args));
            return null;
         };
         case "trace" -> (JsCallable) (context, args) -> {
            String message = args.length > 0 ? this.argsToString(context, args) : "Trace";
            this.output(message);
            return null;
         };
         case "dir", "dirxml" -> (JsCallable) (context, args) -> {
            if (args.length > 0) {
               this.output(Terms.toStringCoerce(args[0], (CoreContext)context));
            }

            return null;
         };
         case "table" -> (JsCallable) (context, args) -> {
            if (args.length > 0) {
               this.output(Terms.toStringCoerce(args[0], (CoreContext)context));
            }

            return null;
         };
         case "assert" -> (JsCallable) (context, args) -> {
            if (args.length == 0 || !Terms.isTruthy(args[0])) {
               String message = "Assertion failed";
               if (args.length > 1) {
                  Object[] rest = new Object[args.length - 1];
                  System.arraycopy(args, 1, rest, 0, rest.length);
                  message = message + ": " + this.argsToString(context, rest);
               }

               this.output(message);
            }

            return null;
         };
         case "count" -> (JsCallable) (context, args) -> {
            String label = args.length > 0 ? Terms.toStringCoerce(args[0], (CoreContext)context) : "default";
            int count = this.counts.merge(label, 1, Integer::sum);
            this.output(label + ": " + count);
            return null;
         };
         case "countReset" -> (JsCallable) (context, args) -> {
            String label = args.length > 0 ? Terms.toStringCoerce(args[0], (CoreContext)context) : "default";
            this.counts.remove(label);
            return null;
         };
         case "time" -> (JsCallable) (context, args) -> {
            String label = args.length > 0 ? Terms.toStringCoerce(args[0], (CoreContext)context) : "default";
            this.timers.put(label, System.currentTimeMillis());
            return null;
         };
         case "timeLog" -> (JsCallable) (context, args) -> {
            String label = args.length > 0 ? Terms.toStringCoerce(args[0], (CoreContext)context) : "default";
            Long start = this.timers.get(label);
            if (start != null) {
               long elapsed = System.currentTimeMillis() - start;
               this.output(label + ": " + elapsed + "ms");
            }

            return null;
         };
         case "timeEnd" -> (JsCallable) (context, args) -> {
            String label = args.length > 0 ? Terms.toStringCoerce(args[0], (CoreContext)context) : "default";
            Long start = this.timers.remove(label);
            if (start != null) {
               long elapsed = System.currentTimeMillis() - start;
               this.output(label + ": " + elapsed + "ms");
            }

            return null;
         };
         case "group", "groupCollapsed" -> (JsCallable) (context, args) -> {
            if (args.length > 0) {
               this.output(this.argsToString(context, args));
            }

            return null;
         };
         case "groupEnd", "clear" -> (JsCallable) (context, args) -> null;
         default -> null;
      };
   }
}
