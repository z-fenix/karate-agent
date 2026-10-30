package io.karatelabs.ext.common;

import java.nio.file.Path;

@FunctionalInterface
public interface MockStarter {
   Object mock(Path var1, Path var2, Path var3, Object var4, Object var5);
}
