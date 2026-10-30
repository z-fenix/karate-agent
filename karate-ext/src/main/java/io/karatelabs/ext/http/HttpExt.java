package io.karatelabs.ext.http;

import io.karatelabs.core.Ext;
import io.karatelabs.core.Suite;
import io.karatelabs.ext.common.ExtMeta;
import io.karatelabs.ext.common.MaxPaths;
import io.karatelabs.ext.http.jsapi.Http;
import io.karatelabs.ext.mock.MockInstance;
import io.karatelabs.js.SimpleObject;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HttpExt implements Ext, SimpleObject {
   private static final Logger logger = LoggerFactory.getLogger(HttpExt.class);
   static final String VERSION = ExtMeta.version("karate-max-meta.properties");
   static final String DEFAULT_RULES = "rulebooks";
   private String rulesHome;
   private Path resolvedRules;
   private final List<MockInstance> started = Collections.synchronizedList(new ArrayList<>());
   private final List<Runnable> apps = Collections.synchronizedList(new ArrayList<>());

   @Override
   public Object jsGet(String var1) {
      return "rulesHome".equals(var1) ? this.rulesHome : null;
   }

   @Override
   public void putMember(String var1, Object var2) {
      if ("rulesHome".equals(var1)) {
         this.rulesHome = var2 == null ? null : String.valueOf(var2);
      } else {
         throw new IllegalArgumentException("unknown http config key: http." + var1 + " (known: rulesHome)");
      }
   }

   @Override
   public Collection<String> jsKeys() {
      return List.of("rulesHome");
   }

   @Override
   public void onBoot(Suite var1) {
      io.karatelabs.ext.common.keycheck.Main.requireLicensed("http");
      Path var2 = var1.getRoot();
      this.resolvedRules = resolveHome(var2, this.rulesHome, "rulebooks");
      Http var3 = new Http()
         .withRulesHome(this.resolvedRules)
         .withRoot(var2, var1.getClasspathRoot())
         .onMockStart(this.started::add)
         .onAppStart(this.apps::add);
      var1.registerGlobal("Http", var3);
      logger.info("http ext booted (rulesHome={})", this.resolvedRules);
   }

   @Override
   public void onShutdown() {
      synchronized (this.apps) {
         for (Runnable var3 : this.apps) {
            try {
               var3.run();
            } catch (RuntimeException var8) {
               logger.warn("failed to stop an app: {}", var8.toString());
            }
         }

         this.apps.clear();
      }

      synchronized (this.started) {
         for (MockInstance var13 : this.started) {
            try {
               var13.stop();
            } catch (RuntimeException var7) {
               logger.warn("failed to stop mock at {}: {}", var13.getUrl(), var7.toString());
            }
         }

         this.started.clear();
      }
   }

   @Override
   public Map<String, Object> getManifest() {
      LinkedHashMap<java.lang.String, java.lang.Object> var1 = new LinkedHashMap();
      var1.put("version", VERSION);
      if (this.resolvedRules != null) {
         var1.put("rulesHome", this.resolvedRules.toString());
      }

      return var1;
   }

   static Path resolveHome(Path var0, String var1, String var2) {
      String var3 = var1 != null && !var1.isBlank() ? var1 : var2;
      return MaxPaths.resolvePath(var0, var3);
   }
}
