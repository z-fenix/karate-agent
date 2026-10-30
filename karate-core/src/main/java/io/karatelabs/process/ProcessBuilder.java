package io.karatelabs.process;

import io.karatelabs.common.OsUtils;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class ProcessBuilder {
   private List<String> args = new ArrayList<>();
   private Path workingDir;
   private Map<String, String> env = new HashMap<>();
   private boolean useShell = false;
   private boolean redirectErrorStream = true;
   private Duration timeout;
   private Consumer<String> listener;
   private Consumer<String> errorListener;
   private boolean logToContext = true;

   private ProcessBuilder() {
   }

   public static ProcessBuilder create() {
      return new ProcessBuilder();
   }

   public static List<String> tokenize(String command) {
      List<String> result = new ArrayList<>();
      StringBuilder current = new StringBuilder();
      boolean inSingleQuote = false;
      boolean inDoubleQuote = false;
      boolean escaped = false;

      for (int i = 0; i < command.length(); i++) {
         char c = command.charAt(i);
         if (escaped) {
            current.append(c);
            escaped = false;
         } else if (c == '\\' && !inSingleQuote) {
            escaped = true;
         } else if (c == '\'' && !inDoubleQuote) {
            inSingleQuote = !inSingleQuote;
         } else if (c == '"' && !inSingleQuote) {
            inDoubleQuote = !inDoubleQuote;
         } else if (!Character.isWhitespace(c) || inSingleQuote || inDoubleQuote) {
            current.append(c);
         } else if (current.length() > 0) {
            result.add(current.toString());
            current.setLength(0);
         }
      }

      if (current.length() > 0) {
         result.add(current.toString());
      }

      return result;
   }

   public static List<String> wrapWithShell(List<String> args) {
      List<String> wrapped = new ArrayList<>();
      if (OsUtils.isWindows()) {
         wrapped.add("cmd");
         wrapped.add("/c");
      } else {
         wrapped.add("sh");
         wrapped.add("-c");
      }

      wrapped.add(String.join(" ", args));
      return wrapped;
   }

   public ProcessBuilder line(String command) {
      this.args = tokenize(command);
      return this;
   }

   public ProcessBuilder args(String... args) {
      this.args = new ArrayList<>(List.of(args));
      return this;
   }

   public ProcessBuilder args(List<String> args) {
      this.args = new ArrayList<>(args);
      return this;
   }

   public ProcessBuilder workingDir(Path dir) {
      this.workingDir = dir;
      return this;
   }

   public ProcessBuilder workingDir(String dir) {
      this.workingDir = dir != null ? Path.of(dir) : null;
      return this;
   }

   public ProcessBuilder env(Map<String, String> env) {
      this.env = new HashMap<>(env);
      return this;
   }

   public ProcessBuilder env(String key, String value) {
      this.env.put(key, value);
      return this;
   }

   public ProcessBuilder useShell(boolean useShell) {
      this.useShell = useShell;
      return this;
   }

   public ProcessBuilder redirectErrorStream(boolean redirect) {
      this.redirectErrorStream = redirect;
      return this;
   }

   public ProcessBuilder timeout(Duration timeout) {
      this.timeout = timeout;
      return this;
   }

   public ProcessBuilder timeoutMillis(long millis) {
      this.timeout = Duration.ofMillis(millis);
      return this;
   }

   public ProcessBuilder listener(Consumer<String> listener) {
      this.listener = listener;
      return this;
   }

   public ProcessBuilder errorListener(Consumer<String> errorListener) {
      this.errorListener = errorListener;
      return this;
   }

   public ProcessBuilder logToContext(boolean log) {
      this.logToContext = log;
      return this;
   }

   public ProcessConfig build() {
      List<String> finalArgs = this.useShell ? wrapWithShell(this.args) : this.args;
      return new ProcessConfig(
         finalArgs, this.workingDir, this.env, this.useShell, this.redirectErrorStream, this.timeout, this.listener, this.errorListener, this.logToContext
      );
   }

   public static ProcessBuilder fromMap(Map<String, Object> options) {
      ProcessBuilder builder = create();
      if (options.containsKey("args")) {
         Object argsObj = options.get("args");
         if (argsObj instanceof List) {
            builder.args((List<String>)argsObj);
         }
      } else if (options.containsKey("line")) {
         builder.line((String)options.get("line"));
      }

      if (options.containsKey("workingDir")) {
         builder.workingDir((String)options.get("workingDir"));
      }

      if (options.containsKey("env")) {
         builder.env((Map<String, String>)options.get("env"));
      }

      if (options.containsKey("useShell")) {
         Object val = options.get("useShell");
         builder.useShell(val instanceof Boolean ? (Boolean)val : Boolean.parseBoolean(val.toString()));
      }

      if (options.containsKey("redirectErrorStream")) {
         Object val = options.get("redirectErrorStream");
         builder.redirectErrorStream(val instanceof Boolean ? (Boolean)val : Boolean.parseBoolean(val.toString()));
      }

      if (options.containsKey("timeout")) {
         Object timeout = options.get("timeout");
         if (timeout instanceof Number) {
            builder.timeoutMillis(((Number)timeout).longValue());
         }
      }

      if (options.containsKey("logToContext")) {
         Object val = options.get("logToContext");
         builder.logToContext(val instanceof Boolean ? (Boolean)val : Boolean.parseBoolean(val.toString()));
      }

      return builder;
   }
}
