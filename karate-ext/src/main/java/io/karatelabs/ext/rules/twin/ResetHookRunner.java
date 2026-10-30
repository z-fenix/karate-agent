package io.karatelabs.ext.rules.twin;

import java.io.IOException;
import java.io.InputStream;
import java.lang.ProcessBuilder.Redirect;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

final class ResetHookRunner {
   private static final Set<String> a = Set.of("command", "timeoutMs", "cwd");
   private static final int b = 512;
   private final List<String> c;
   private final int d;
   private final Path e;

   private ResetHookRunner(List<String> var1, int var2, Path var3) {
      this.c = var1;
      this.d = var2;
      this.e = var3;
   }

   static ResetHookRunner a(Object var0) {
      if (var0 == null || io.karatelabs.ext.rules.twin.CanonicalJson.a(var0)) {
         return null;
      } else if (!(var0 instanceof Map var1)) {
         throw Twin.usage("reset", "must be {command: [argv], timeoutMs?, cwd?}");
      } else {
         for (Object var3 : var1.keySet()) {
            if (!a.contains(String.valueOf(var3))) {
               throw Twin.usage("reset." + var3, "is not a reset option");
            }
         }

         if (var1.get("command") instanceof List var9 && !var9.isEmpty()) {
            ArrayList<java.lang.String> var11 = new ArrayList(var9.size());

            for (Object var5 : var9) {
               if (!(var5 instanceof String var6)) {
                  throw Twin.usage("reset.command", "must be a non-empty array of strings — argv, never a shell string");
               }

               var11.add(var6);
            }

            int var12 = Twin.bounded(var1.get("timeoutMs"), "reset.timeoutMs", 30000, 1, Integer.MAX_VALUE);
            Path var13 = Path.of("").toAbsolutePath().normalize();
            Path var14 = var13;
            Object var7 = var1.get("cwd");
            if (var7 != null && !io.karatelabs.ext.rules.twin.CanonicalJson.a(var7)) {
               if (!(var7 instanceof String var8) || var8.isBlank()) {
                  throw Twin.usage("reset.cwd", "must be a directory inside the project home");
               }

               var14 = var13.resolve(var8).toAbsolutePath().normalize();
               if (!var14.startsWith(var13)) {
                  throw Twin.usage("reset.cwd", "must resolve inside the project home '" + var13 + "'");
               }
            }

            return new ResetHookRunner(var11, var12, var14);
         } else {
            throw Twin.usage("reset.command", "must be a non-empty array of strings — argv, never a shell string");
         }
      }
   }

   Map<String, Object> a() {
      LinkedHashMap<java.lang.String, java.lang.Object> var1 = new LinkedHashMap();
      var1.put("command", List.copyOf(this.c));
      var1.put("perSequence", true);
      return var1;
   }

   io.karatelabs.ext.rules.twin.ResetHookRunner.ResetHookResult b() {
      ProcessBuilder var1 = new ProcessBuilder(this.c).directory(this.e.toFile()).redirectOutput(Redirect.DISCARD);

      Process var2;
      try {
         var2 = var1.start();
      } catch (RuntimeException | IOException var7) {
         return new io.karatelabs.ext.rules.twin.ResetHookRunner.ResetHookResult(this.a((Integer)null), "the reset hook could not be started: " + var7.getMessage(), "");
      }

      io.karatelabs.ext.rules.twin.ResetHookRunner.StderrRingBuffer var3 = new io.karatelabs.ext.rules.twin.ResetHookRunner.StderrRingBuffer(var2.getErrorStream());

      boolean var4;
      try {
         var4 = var2.waitFor(this.d, TimeUnit.MILLISECONDS);
      } catch (InterruptedException var6) {
         Thread.currentThread().interrupt();
         return new io.karatelabs.ext.rules.twin.ResetHookRunner.ResetHookResult(this.a(a(var2)), "the reset hook was interrupted", var3.a());
      }

      if (!var4) {
         return new io.karatelabs.ext.rules.twin.ResetHookRunner.ResetHookResult(this.a(a(var2)), "the reset hook exceeded " + this.d + "ms", var3.a());
      } else {
         int var5 = var2.exitValue();
         return var5 == 0 ? new io.karatelabs.ext.rules.twin.ResetHookRunner.ResetHookResult(this.a(var5), null, "") : new io.karatelabs.ext.rules.twin.ResetHookRunner.ResetHookResult(this.a(var5), "the reset hook exited " + var5, var3.a());
      }
   }

   private Map<String, Object> a(Integer var1) {
      LinkedHashMap<java.lang.String, java.lang.Object> var2 = new LinkedHashMap();
      var2.put("command", List.copyOf(this.c));
      var2.put("exit", var1);
      return var2;
   }

   private static Integer a(Process var0) {
      var0.destroyForcibly();

      try {
         return var0.waitFor(5L, TimeUnit.SECONDS) ? var0.exitValue() : null;
      } catch (InterruptedException var2) {
         Thread.currentThread().interrupt();
         return null;
      }
   }

   record ResetHookResult(Map<String, Object> b, String c, String d) {

      ResetHookResult(Map<String, Object> b, String c, String d) {
         this.b = b;
         this.c = c;
         this.d = d;
      }

      boolean a() {
         return this.c != null;
      }

      public Map<String, Object> b() {
         return this.b;
      }

      public String c() {
         return this.c;
      }

      public String d() {
         return this.d;
      }
   }

   private static final class StderrRingBuffer implements Runnable {
      private final InputStream a;
      private final byte[] b = new byte[512];
      private final Thread c;
      private int d;
      private int e;

      StderrRingBuffer(InputStream var1) {
         this.a = var1;
         this.c = new Thread(this, "twin-reset-hook-stderr");
         this.c.setDaemon(true);
         this.c.start();
      }

      @Override
      public void run() {
         byte[] var1 = new byte[256];

         try (InputStream var2 = this.a) {
            for (int var3 = var2.read(var1); var3 > 0; var3 = var2.read(var1)) {
               this.a(var1, var3);
            }
         } catch (IOException var7) {
         }
      }

      private synchronized void a(byte[] var1, int var2) {
         for (int var3 = 0; var3 < var2; var3++) {
            this.b[this.d] = var1[var3];
            this.d = (this.d + 1) % this.b.length;
            this.e = Math.min(this.e + 1, this.b.length);
         }
      }

      String a() {
         try {
            this.c.join(1000L);
         } catch (InterruptedException var6) {
            Thread.currentThread().interrupt();
         }

         synchronized (this) {
            byte[] var2 = new byte[this.e];
            int var3 = (this.d - this.e + this.b.length) % this.b.length;

            for (int var4 = 0; var4 < this.e; var4++) {
               var2[var4] = this.b[(var3 + var4) % this.b.length];
            }

            return new String(var2, StandardCharsets.UTF_8);
         }
      }
   }
}
