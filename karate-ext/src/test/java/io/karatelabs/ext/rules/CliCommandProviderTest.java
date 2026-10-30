package io.karatelabs.ext.rules;

import io.karatelabs.cli.CliCommandProvider;
import java.util.ServiceLoader;
import org.junit.Test;
import picocli.CommandLine;
import static org.junit.Assert.assertTrue;

public class CliCommandProviderTest {
   @Test
   public void serviceLoaderRegistersMaxCommands() {
      var names = ServiceLoader.load(CliCommandProvider.class).stream()
         .flatMap(provider -> provider.get().commands().stream())
         .map(command -> new CommandLine(command).getCommandName())
         .toList();
      assertTrue("serve command missing", names.contains("serve"));
      assertTrue("launch command missing", names.contains("launch"));
   }
}
