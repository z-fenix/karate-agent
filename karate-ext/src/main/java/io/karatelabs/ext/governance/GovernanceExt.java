package io.karatelabs.ext.governance;

import io.karatelabs.core.Ext;
import io.karatelabs.core.ReportAssets;
import io.karatelabs.core.RunEvent;
import io.karatelabs.core.RunEventType;
import io.karatelabs.core.Suite;
import io.karatelabs.ext.common.ReportAssetsInstaller;
import io.karatelabs.ext.openapi.SpecBinding;
import io.karatelabs.ext.openapi.jsapi.Openapi;
import io.karatelabs.js.SimpleObject;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GovernanceExt implements Ext, SimpleObject {
   private static final Logger logger = LoggerFactory.getLogger(GovernanceExt.class);
   private Suite suite;
   private boolean registered;
   private Path root;
   private Path reportDir;
   private Object spec;
   private Object weights;

   @Override
   public Object jsGet(String var1) {
      return switch (var1) {
         case "spec" -> this.spec;
         case "weights" -> this.weights;
         default -> null;
      };
   }

   @Override
   public void putMember(String var1, Object var2) {
      switch (var1) {
         case "spec":
            this.spec = var2;
            break;
         case "weights":
            this.weights = var2;
            break;
         default:
            throw new RuntimeException("unknown governance ext config: gov." + var1 + " (known: spec, weights)");
      }
   }

   @Override
   public Collection<String> jsKeys() {
      return List.of("spec", "weights");
   }

   @Override
   public void onBoot(Suite var1) {
      io.karatelabs.ext.common.keycheck.Main.requireProduct("governance", "openapi");
      this.suite = var1;
      this.root = var1.getRoot();
      this.reportDir = Path.of(var1.getOutputDir());
      logger.info("governance ext booted");
   }

   @Override
   public boolean onEvent(RunEvent var1) {
      if (var1.getType() == RunEventType.SUITE_ENTER && !this.registered && this.suite != null) {
         this.registered = true;
         this.suite
            .registerReportAssets(
               ReportAssets.named("governance")
                  .js("static/governance-report.js")
                  .css("static/governance-report.css")
                  .asset("static/coverage.css")
                  .asset("static/help-affordance.js")
                  .page("nav.pages", "Governance", "pages/governance.html"),
               this.getClass().getClassLoader()
            );
      }

      return true;
   }

   @Override
   public void onShutdown() {
      if (this.reportDir != null) {
         this.renderFor(this.root, this.reportDir, this.declaredSpec());
      }
   }

   private Object declaredSpec() {
      if (this.spec != null) {
         return this.spec;
      } else {
         if (this.suite != null && this.suite.getGlobal("spec") instanceof SpecBinding var1) {
            var var5 = var1.paths();
            if (!var5.isEmpty()) {
               String var3 = (String)var5.get(0);
               if (!var3.startsWith("classpath:") && !var3.startsWith("file:")) {
                  Path var4 = Path.of(var3);
                  if (this.root != null && var4.startsWith(this.root)) {
                     return this.root.relativize(var4).toString();
                  }

                  return "file:" + var4;
               }

               return var3;
            }
         }

         return null;
      }
   }

   void renderFor(Path var1, Path var2, Object var3) {
      this.root = var1;
      this.reportDir = var2;
      if (var3 == null) {
         this.absent(
            "no API document is declared for this project. Set `cov.openapi` in karate-boot.js (or `gov.spec` on this ext) so the grade knows which contract to read."
         );
      } else {
         try {
            LinkedHashMap var4 = new LinkedHashMap();
            if (this.weights != null) {
               var4.put("weights", this.weights);
            }

            if (new Openapi()
                  .withRoot(var1)
                  .withReportDir(var2)
                  .withNav(this.suite == null ? null : this.suite.getReportAssets())
                  .callReport(var3, var4.isEmpty() ? null : var4) instanceof Map var6
               && var6.get("error") != null) {
               this.absent(String.valueOf(var6.get("error")));
            }
         } catch (RuntimeException var7) {
            logger.debug("governance: the grade page could not be rendered: {}", var7.toString());
            this.absent("this project's API document could not be graded: " + var7.getMessage());
         }
      }
   }

   private void absent(String var1) {
      LinkedHashMap var2 = new LinkedHashMap();
      var2.put(
         "absent",
         "No API document was graded for this project, so this page has no data. That is an absence of evidence. It does not show that this project's contract is well governed, or that it is not."
      );
      var2.put("why", var1);
      var2.put("mint", "Openapi.report('openapi.yaml')");
      var2.put("karateSummary", "../../../karate-summary.html");
      io.karatelabs.ext.common.ReportAssetsInstaller.a(var2, this.suite == null ? null : this.suite.getReportAssets());

      try {
         io.karatelabs.ext.common.ReportAssetsInstaller.a(this.reportDir.resolve("ext/governance/governance-data.js"), "KARATE_GOVERNANCE", var2);
      } catch (IOException var4) {
         logger.debug("governance: could not write the empty-state page data: {}", var4.toString());
      }
   }
}
