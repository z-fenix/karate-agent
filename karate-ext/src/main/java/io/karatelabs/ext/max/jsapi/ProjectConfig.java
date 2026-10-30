package io.karatelabs.ext.max.jsapi;

import io.karatelabs.core.ConfigLoader;
import io.karatelabs.core.ScenarioRuntime;
import io.karatelabs.core.Suite;
import io.karatelabs.js.Engine;
import java.nio.file.Path;
import java.util.Map;
import java.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ProjectConfig {
   private static final Logger logger = LoggerFactory.getLogger(ProjectConfig.class);
   static final String SENTINEL = "__configApplied";
   static final String SWITCH = "__configSwitch";

   private ProjectConfig() {
   }

   public static String ambientEnv() {
      return io.karatelabs.ext.common.run.RunnerEnvironmentPolicy.a();
   }

   static String ambientEnv(Function<String, String> var0, Function<String, String> var1) {
      return io.karatelabs.ext.common.run.RunnerEnvironmentPolicy.a(var0, var1);
   }

   static void ensure(Engine var0, Path var1, ConsoleState var2) {
      if (var0 != null
         && var1 != null
         && (
            !Boolean.TRUE.equals(var0.getRootBindings().get("__configApplied"))
               || !(var0.getRootBindings().get("__configSwitch") instanceof Number var3 && var3.intValue() == var2.switches())
         )) {
         ConsoleState.Snapshot var6 = var2.snapshot();
         var0.putRootBinding("__configApplied", Boolean.TRUE);
         var0.putRootBinding("__configSwitch", var6.switches());

         try {
            var var7 = resolve(var1, null, var6, var2);
            var7.forEach(var0::putRootBinding);
            logger.debug("project config applied {} from {}", var7.keySet(), var1);
         } catch (Exception var5) {
            logger.warn("project config not applied ({}): {}", var1, var5.getMessage());
         }
      }
   }

   static Map<String, Object> apply(Engine var0, Path var1, String var2, ConsoleState var3) {
      return apply(var0, var1, var2, var3.snapshot(), var3);
   }

   static Map<String, Object> reload(Engine var0, Path var1, ConsoleState.Snapshot var2, ConsoleState var3) {
      return apply(var0, var1, null, var2, var3);
   }

   private static Map<String, Object> apply(Engine var0, Path var1, String var2, ConsoleState.Snapshot var3, ConsoleState var4) {
      var var5 = resolve(var1, var2, var3, var4);
      var5.forEach(var0::putRootBinding);
      var0.putRootBinding("__configApplied", Boolean.TRUE);
      var0.putRootBinding("__configSwitch", var3.switches());
      logger.debug("Runner.config applied {} from {} (env={})", new Object[]{var5.keySet(), var1, var2 != null ? var2 : var3.env()});
      return var5;
   }

   private static Map<String, Object> resolve(Path var0, String var1, ConsoleState.Snapshot var2, ConsoleState var3) {
      Suite var4 = enclosingSuite();
      return var4 != null
         ? ConfigLoader.configOnly(var0, var1 != null ? var1 : var4.env, var4.getSystemProperties(), var4.getSystemEnv())
         : ConfigLoader.configOnly(var0, var1 != null ? var1 : var2.env(), null, var3.overlay());
   }

   private static Suite enclosingSuite() {
      ScenarioRuntime var0 = ScenarioRuntime.currentOrNull();
      return var0 != null && var0.getFeatureRuntime() != null ? var0.getFeatureRuntime().getSuite() : null;
   }

   public static void reset(Engine var0) {
      if (var0 != null) {
         var0.putRootBinding("__configApplied", Boolean.FALSE);
      }
   }
}
