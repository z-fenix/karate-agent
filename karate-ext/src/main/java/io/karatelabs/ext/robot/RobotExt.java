package io.karatelabs.ext.robot;

import io.karatelabs.core.Channel;
import io.karatelabs.core.Ext;
import io.karatelabs.core.ScenarioRuntime;
import io.karatelabs.core.Suite;
import io.karatelabs.js.JavaInvokable;
import io.karatelabs.js.SimpleObject;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

public class RobotExt implements Ext {
   private final Map<ScenarioRuntime, LazyBot> bots = Collections.synchronizedMap(new WeakHashMap<>());
   private RobotFactory robot;

   @Override
   public void onBoot(Suite var1) {
      Path var2 = var1.outputDir.resolve("screenshots");
      this.robot = new RobotFactory();
      this.robot.setDefaultScreenshotsDir(var2);
      this.robot.onCreateGuard(() -> {
         io.karatelabs.ext.common.keycheck.Main.requireProduct("robot", "robot");
         return null;
      });
      this.robot.onStep(io.karatelabs.ext.common.EvidenceReporter::a);
      this.robot
         .onCreate(var0 -> var0.getAgentDriver().setScreenshotSavedListener((var0x, var1x) -> io.karatelabs.ext.common.EvidenceReporter.a("screenshots/" + var0x, var1x)));
      var1.registerGlobal("Robot", this.robot);
      var1.registerGlobal("bot", var2x -> this.seed(var1, var2x.getRuntime()));
   }

   private LazyBot seed(Suite var1, ScenarioRuntime var2) {
      ScenarioRuntime var3 = var2;

      while (var3.getFeatureRuntime() != null && var3.getFeatureRuntime().getCallerScenario() != null) {
         var3 = var3.getFeatureRuntime().getCallerScenario();
      }

      return this.bots.computeIfAbsent(var3, var2x -> {
         LazyBot[] var3x = new LazyBot[1];
         var3x[0] = new LazyBot(() -> {
            var2x.registerChannel(new Channel() {
               @Override
               public Object init(ScenarioRuntime var1x) {
                  return null;
               }

               @Override
               public void afterScenario() {
                  RobotExt.this.bots.remove(var2x);
                  var3x[0].release();
               }
            });
            return ((JavaInvokable)((SimpleObject)var1.getGlobal("Robot")).jsGet("create")).invoke();
         });
         return var3x[0];
      });
   }

   @Override
   public void onShutdown() {
      this.bots.clear();
      if (this.robot != null) {
         this.robot.stopOwned();
      }
   }
}
