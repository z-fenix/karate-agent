package io.karatelabs.ext.rules;

import io.karatelabs.core.Ext;
import io.karatelabs.core.ExtGlobalFactory;
import io.karatelabs.core.ReportAssets;
import io.karatelabs.core.ScenarioRuntime;
import io.karatelabs.core.Suite;
import io.karatelabs.ext.common.ExtMeta;
import io.karatelabs.ext.common.MaxPaths;
import io.karatelabs.ext.common.keycheck.Main;
import io.karatelabs.ext.max.jsapi.Check;
import io.karatelabs.ext.robot.MatchBinding;
import io.karatelabs.ext.rules.twin.Twin;
import io.karatelabs.js.SimpleObject;
import java.nio.file.Path;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RulesExt implements Ext, SimpleObject {
   private static final Logger logger = LoggerFactory.getLogger(RulesExt.class);
   static final String VERSION = ExtMeta.version("karate-max-meta.properties");
   static final String DEFAULT_HOME = "rulebooks";
   private String home;
   private Path resolvedHome;
   private Rule rule;
   private Twin twin;

   @Override
   public Object jsGet(String var1) {
      byte var3 = -1;
      switch (var1.hashCode()) {
         case 3208415:
            if (var1.equals("home")) {
               var3 = 0;
            }
         default:
            return switch (var3) {
               case 0 -> this.home;
               default -> null;
            };
      }
   }

   @Override
   public void putMember(String var1, Object var2) {
      byte var4 = -1;
      switch (var1.hashCode()) {
         case 3208415:
            if (var1.equals("home")) {
               var4 = 0;
            }
         default:
            switch (var4) {
               case 0:
                  this.home = str(var2);
                  return;
               default:
                  throw new IllegalArgumentException("unknown rules config key: rules." + var1 + " (known: home)");
            }
      }
   }

   @Override
   public Collection<String> jsKeys() {
      return List.of("home");
   }

   public String getHome() {
      return this.home;
   }

   private static String str(Object var0) {
      return var0 == null ? null : String.valueOf(var0);
   }

   @Override
   public void onBoot(Suite var1) {
      Main.requireProduct("rules", "rules");
      Main.requireProduct("twin", "rules");
      Path var2 = var1.getRoot();
      this.resolvedHome = resolveHome(var2, this.home, "rulebooks");
      io.karatelabs.ext.mutation.RuleMutationAdapter.install();
      this.rule = new Rule(this.resolvedHome);
      this.twin = new Twin(this.resolvedHome).withMocks(io.karatelabs.ext.http.HttpMockFactory.a);
      var1.registerGlobal("Rule", this.rule);
      var1.registerGlobal("Twin", this.twin);
      var1.registerGlobal("Schema", new Schema(this.resolvedHome));
      Path var3 = MaxPaths.checksDir(var2);
      ExtGlobalFactory var4 = var2x -> {
         ScenarioRuntime var3x = var2x.getRuntime();
         return Check.at(var3, var3x == null ? null : var3x.getEngine()).withOverlay(var1.getSystemEnv()::get);
      };
      var1.registerGlobal("Check", var4);
      var1.registerGlobal("match", new MatchBinding().onAssertion(io.karatelabs.ext.common.EvidenceReporter::b));
      var1.registerReportAssets(
         ReportAssets.named("evidence").js("static/evidence-report.js").cta("Karate Agent", "https://karatelabs.io/agent"), this.getClass().getClassLoader()
      );
      logger.info("rules ext booted (rules={})", this.resolvedHome);
   }

   @Override
   public void onShutdown() {
      if (this.rule != null) {
         this.rule.close();
      }

      if (this.twin != null) {
         this.twin.close();
      }
   }

   @Override
   public Map<String, Object> getManifest() {
      LinkedHashMap<java.lang.String, java.lang.Object> var1 = new LinkedHashMap();
      var1.put("version", VERSION);
      if (this.resolvedHome != null) {
         var1.put("home", this.resolvedHome.toString());
      }

      return var1;
   }

   static Path resolveHome(Path var0, String var1, String var2) {
      String var3 = var1 != null && !var1.isBlank() ? var1 : var2;
      return MaxPaths.resolvePath(var0, var3);
   }
}
