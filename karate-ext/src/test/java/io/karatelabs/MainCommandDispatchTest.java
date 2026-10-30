package io.karatelabs;

import java.util.Set;
import org.junit.Test;
import static org.junit.Assert.assertArrayEquals;

public class MainCommandDispatchTest {
   private static final Set<String> COMMANDS = Set.of("run", "mock", "clean", "serve", "launch");

   @Test
   public void unknownCommandIsNotTreatedAsATestPath() {
      assertArrayEquals(new String[]{"serse"}, Main.defaultToRun(new String[]{"serse"}, COMMANDS));
   }

   @Test
   public void explicitPathsAndRunOptionsStillDefaultToRun() {
      assertArrayEquals(new String[]{"run", "missing.feature"}, Main.defaultToRun(new String[]{"missing.feature"}, COMMANDS));
      assertArrayEquals(new String[]{"run", "."}, Main.defaultToRun(new String[]{"."}, COMMANDS));
      assertArrayEquals(new String[]{"run", "classpath:examples"}, Main.defaultToRun(new String[]{"classpath:examples"}, COMMANDS));
      assertArrayEquals(new String[]{"run", "-t", "@smoke"}, Main.defaultToRun(new String[]{"-t", "@smoke"}, COMMANDS));
      assertArrayEquals(new String[]{"serve"}, Main.defaultToRun(new String[]{"serve"}, COMMANDS));
   }
}
