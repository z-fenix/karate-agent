package io.karatelabs.ext.max.jsapi;

import io.karatelabs.core.BootBinding;
import io.karatelabs.core.BootLoader;
import io.karatelabs.core.ExtGlobalFactory;
import io.karatelabs.js.Engine;
import java.nio.file.Path;
import java.util.Map.Entry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ProjectBoot {
   private static final Logger logger = LoggerFactory.getLogger(ProjectBoot.class);
   static final String SENTINEL = "__bootApplied";

   private ProjectBoot() {
   }

   static void ensure(Engine var0, Path var1) {
      if (var0 != null && var1 != null && !Boolean.TRUE.equals(var0.getRootBindings().get("__bootApplied"))) {
         var0.putRootBinding("__bootApplied", Boolean.TRUE);

         try {
            BootBinding var2 = BootLoader.bootOnly(var1, null);
            if (var2 == null) {
               return;
            }

            int var3 = 0;

            for (Entry var5 : var2.getGlobals().entrySet()) {
               String var6 = (String)var5.getKey();
               Object var7 = var5.getValue();
               if (!(var7 instanceof ExtGlobalFactory) && !Boolean.TRUE.equals(var0.eval("typeof " + var6 + " !== 'undefined'"))) {
                  var0.putRootBinding(var6, var7);
                  var3++;
               }
            }

            logger.debug("project boot seeded {} ext global(s) from {}", var3, var1);
         } catch (Exception var8) {
            logger.warn("project boot not applied ({}): {}", var1, var8.getMessage());
         }
      }
   }

   public static void reset(Engine var0) {
      if (var0 != null) {
         var0.putRootBinding("__bootApplied", Boolean.FALSE);
      }
   }
}
