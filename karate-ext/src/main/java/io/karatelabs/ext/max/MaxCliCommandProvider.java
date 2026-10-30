package io.karatelabs.ext.max;

import io.karatelabs.cli.CliCommandProvider;
import java.util.List;

public class MaxCliCommandProvider implements CliCommandProvider {
   @Override
   public List<Object> commands() {
      return List.of(new ServeCommand(), new LaunchCommand());
   }
}
