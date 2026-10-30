package io.karatelabs.process;

import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public record ProcessConfig(
   List<String> args,
   Path workingDir,
   Map<String, String> env,
   boolean useShell,
   boolean redirectErrorStream,
   Duration timeout,
   Consumer<String> listener,
   Consumer<String> errorListener,
   boolean logToContext
) {
   public ProcessConfig(
      List<String> args,
      Path workingDir,
      Map<String, String> env,
      boolean useShell,
      boolean redirectErrorStream,
      Duration timeout,
      Consumer<String> listener,
      Consumer<String> errorListener,
      boolean logToContext
   ) {
      args = args != null ? List.copyOf(args) : List.of();
      env = env != null ? Map.copyOf(env) : Map.of();
      this.args = args;
      this.workingDir = workingDir;
      this.env = env;
      this.useShell = useShell;
      this.redirectErrorStream = redirectErrorStream;
      this.timeout = timeout;
      this.listener = listener;
      this.errorListener = errorListener;
      this.logToContext = logToContext;
   }

   public static ProcessConfig defaults() {
      return new ProcessConfig(List.of(), null, Map.of(), false, true, null, null, null, true);
   }
}
