package io.karatelabs.ext.rules.twin;

import io.karatelabs.ext.common.MaxPaths;
import io.karatelabs.ext.rules.Rule;
import io.karatelabs.js.Engine;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

public final class StandInServerLauncher {
   public static final String a = "eval";
   public static final String b = "start";
   static final String c = "/openapi.yaml";

   private StandInServerLauncher() {
   }

   public static io.karatelabs.ext.rules.twin.StandInServerLauncher.StandInLaunchResult a(io.karatelabs.ext.common.MockStarter var0, Path var1, Path var2, String var3, Map<String, Object> var4) {
      return a(var0, var1, var2, var3, var4, null);
   }

   public static io.karatelabs.ext.rules.twin.StandInServerLauncher.StandInLaunchResult a(io.karatelabs.ext.common.MockStarter var0, Path var1, Path var2, String var3, Map<String, Object> var4, Consumer<Engine> var5) {
      Engine var6 = new Engine();
      var6.put("Rule", Rule.atRulesHome(var2));

      Object var7;
      try {
         if (var5 != null) {
            var5.accept(var6);
         }

         var7 = var6.eval(var3);
      } catch (RuntimeException var12) {
         return new io.karatelabs.ext.rules.twin.StandInServerLauncher.StandInLaunchFailure("eval", io.karatelabs.ext.rules.twin.ThrowableUtils.a(var12));
      }

      if (!(var7 instanceof Map)) {
         return new io.karatelabs.ext.rules.twin.StandInServerLauncher.StandInLaunchFailure("eval", "the handler source is not the operation map: " + var7);
      } else {
         LinkedHashMap<java.lang.String, java.lang.Object> var8 = new LinkedHashMap();
         var8.put("openapi", "/openapi.yaml");
         var8.put("port", 0);
         var8.put("handlers", var7);
         var8.put("arg", var4 == null ? Map.of() : var4);

         Object var9;
         try {
            var9 = var0.mock(var1, var1, var2, var8, null);
         } catch (RuntimeException var11) {
            return new io.karatelabs.ext.rules.twin.StandInServerLauncher.StandInLaunchFailure("start", io.karatelabs.ext.rules.twin.ThrowableUtils.a(var11));
         }

         return (io.karatelabs.ext.rules.twin.StandInServerLauncher.StandInLaunchResult)(var9 instanceof io.karatelabs.ext.common.MockRuntime var10 ? new io.karatelabs.ext.rules.twin.StandInServerLauncher.StandInServer(var6, var10) : new io.karatelabs.ext.rules.twin.StandInServerLauncher.StandInLaunchFailure("start", String.valueOf(var9)));
      }
   }

   public static Path a(Path var0, String var1) {
      return MaxPaths.resolveSandboxed(var0, var1);
   }

   public record StandInLaunchFailure(String a, String b) implements io.karatelabs.ext.rules.twin.StandInServerLauncher.StandInLaunchResult {

      public StandInLaunchFailure(String a, String b) {
         this.a = a;
         this.b = b;
      }
   }

   public static final class StandInServer implements io.karatelabs.ext.rules.twin.StandInServerLauncher.StandInLaunchResult, AutoCloseable {
      private final Engine a;
      private final io.karatelabs.ext.common.MockRuntime b;

      private StandInServer(Engine var1, io.karatelabs.ext.common.MockRuntime var2) {
         this.a = var1;
         this.b = var2;
      }

      public String a() {
         return this.b.getUrl();
      }

      public Engine b() {
         return this.a;
      }

      public Object a(String var1) {
         return this.b.getVariable(var1);
      }

      @Override
      public void close() {
         this.b.stop();
      }
   }

   public sealed interface StandInLaunchResult permits io.karatelabs.ext.rules.twin.StandInServerLauncher.StandInServer, io.karatelabs.ext.rules.twin.StandInServerLauncher.StandInLaunchFailure {
   }
}
