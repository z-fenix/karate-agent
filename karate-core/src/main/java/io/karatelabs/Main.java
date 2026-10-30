package io.karatelabs;

import io.karatelabs.cli.CleanCommand;
import io.karatelabs.cli.CliCommandProvider;
import io.karatelabs.cli.MockCommand;
import io.karatelabs.cli.RunCommand;
import io.karatelabs.core.Globals;
import io.karatelabs.output.Console;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.concurrent.Callable;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.IVersionProvider;
import picocli.CommandLine.Option;

@Command(
   name = "karate",
   mixinStandardHelpOptions = true,
   versionProvider = io.karatelabs.Main.VersionProvider.class,
   description = {"Karate API testing framework"},
   subcommands = {RunCommand.class, MockCommand.class, CleanCommand.class}
)
public class Main implements Callable<Integer> {
   @Option(
      names = {"--no-color"},
      description = {"Disable colored output"}
   )
   boolean noColor;

   public static void main(String[] args) {
      if (hasNoColor(args)) {
         Console.setColorsEnabled(false);
         args = stripNoColor(args);
      }

      CommandLine commandLine = buildCommandLine();
      args = defaultToRun(args, commandLine.getSubcommands().keySet());
      int exitCode = commandLine.setCaseInsensitiveEnumValuesAllowed(true).execute(args);
      System.exit(exitCode);
   }

   static CommandLine buildCommandLine() {
      CommandLine commandLine = new CommandLine(new io.karatelabs.Main());

      for (CliCommandProvider provider : ServiceLoader.load(CliCommandProvider.class, io.karatelabs.Main.class.getClassLoader())) {
         for (Object command : provider.commands()) {
            commandLine.addSubcommand(command);
         }
      }

      return commandLine;
   }

   static boolean hasNoColor(String[] args) {
      for (String arg : args) {
         if ("--no-color".equals(arg)) {
            return true;
         }
      }

      return false;
   }

   static String[] stripNoColor(String[] args) {
      return Arrays.stream(args).filter(a -> !"--no-color".equals(a)).toArray(String[]::new);
   }

   static String[] defaultToRun(String[] args, Set<String> subcommands) {
      if (args.length == 0) {
         return args;
      } else {
         String first = args[0];
         boolean runArgument = first.startsWith("-") || first.startsWith("classpath:")
            || first.contains("/") || first.contains("\\")
            || first.matches("(?i).*\\.feature(?::\\d+)?$") || Files.exists(Path.of(first));
         if (runArgument && !first.equals("-h") && !first.equals("--help") && !first.equals("-V") && !first.equals("--version") && !subcommands.contains(first)) {
            String[] newArgs = new String[args.length + 1];
            newArgs[0] = "run";
            System.arraycopy(args, 0, newArgs, 1, args.length);
            return newArgs;
         } else {
            return args;
         }
      }
   }

   public Integer call() {
      if (this.noColor) {
         Console.setColorsEnabled(false);
      }

      if (Files.exists(Path.of("karate-pom.json"))) {
         RunCommand runCommand = new RunCommand();
         return runCommand.call();
      } else {
         CommandLine.usage(this, System.out);
         return 0;
      }
   }

   public static io.karatelabs.Main parse(String... args) {
      io.karatelabs.Main main = new io.karatelabs.Main();
      new CommandLine(main).parseArgs(args);
      return main;
   }

   static class VersionProvider implements IVersionProvider {
      public String[] getVersion() {
         return new String[]{"Karate " + Globals.KARATE_VERSION};
      }
   }
}
