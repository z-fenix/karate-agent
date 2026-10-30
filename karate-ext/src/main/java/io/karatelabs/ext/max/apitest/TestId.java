package io.karatelabs.ext.max.apitest;

import io.karatelabs.ext.common.MaxPaths;
import io.karatelabs.ext.common.Sha256;
import java.nio.file.Path;

public record TestId(Path project, String path) {

   public TestId(Path project, String path) {
      this.project = project;
      this.path = path;
   }

   public static TestId of(Path var0, Path var1) {
      Path var2 = MaxPaths.dirLockKey(var0);
      Path var3 = MaxPaths.lockKey(var1);
      Path var4 = var3.startsWith(var2) ? var2.relativize(var3) : var3;
      return new TestId(var2, var4.toString().replace('\\', '/'));
   }

   public Path file() {
      return this.project.resolve(this.path);
   }

   public String projectKey() {
      return projectKey(this.project);
   }

   static String projectKey(Path var0) {
      return Sha256.a(var0.toString()).substring(0, 16);
   }

   public String testKey() {
      return Sha256.a(this.path).substring(0, 16);
   }

   public boolean under(Path var1) {
      return this.file().startsWith(MaxPaths.dirLockKey(var1));
   }
}
