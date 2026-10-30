package io.karatelabs.ext.contract;

import io.karatelabs.core.Ext;
import io.karatelabs.core.ReportAssets;
import io.karatelabs.core.RunEvent;
import io.karatelabs.core.RunEventType;
import io.karatelabs.core.Suite;
import io.karatelabs.ext.common.ReportAssetsInstaller;
import io.karatelabs.ext.pair.ContractReportArtifacts;
import io.karatelabs.js.SimpleObject;
import java.io.IOException;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ContractExt implements Ext, SimpleObject {
   private static final Logger logger = LoggerFactory.getLogger(ContractExt.class);
   private Suite suite;
   private boolean registered;
   private Path root;
   private Path reportDir;

   @Override
   public Object jsGet(String var1) {
      return null;
   }

   @Override
   public void onBoot(Suite var1) {
      io.karatelabs.ext.common.keycheck.Main.requireProduct("contract", "openapi");
      this.suite = var1;
      this.root = var1.getRoot();
      this.reportDir = Path.of(var1.getOutputDir());
      logger.info("contract ext booted (pairs read from {})", this.root);
   }

   @Override
   public boolean onEvent(RunEvent var1) {
      if (var1.getType() == RunEventType.SUITE_ENTER && !this.registered && this.suite != null) {
         this.registered = true;
         this.suite
            .registerReportAssets(
               ReportAssets.named("contract")
                  .js("static/contract-report.js")
                  .css("static/contract-report.css")
                  .asset("static/coverage.css")
                  .asset("static/help-affordance.js")
                  .page("nav.pages", "Contract", "pages/contract.html"),
               this.getClass().getClassLoader()
            );
      }

      return true;
   }

   @Override
   public void onShutdown() {
      if (this.reportDir != null) {
         this.renderFor(this.root, this.reportDir);
      }
   }

   void renderFor(Path var1, Path var2) {
      this.root = var1;
      this.reportDir = var2;

      try {
         if (io.karatelabs.ext.pair.ContractReportArtifacts.publishReport(var1, var2, this.suite == null ? null : this.suite.getReportAssets(), null, null) instanceof Map var4 && var4.get("error") != null) {
            this.absent(String.valueOf(var4.get("error")));
         }
      } catch (RuntimeException var5) {
         logger.debug("contract: the paired-run page could not be rendered: {}", var5.toString());
         this.absent("the page could not be rendered from this project's stored pairs: " + var5.getMessage());
      }
   }

   private void absent(String var1) {
      LinkedHashMap var2 = new LinkedHashMap();
      var2.put(
         "absent",
         "This project has no recorded comparison yet, so this page has no data. That is an absence of evidence. It does not show whether this project's mock behaves like its provider."
      );
      var2.put("why", var1);
      var2.put("mint", "Contract.pair({suite: 'checks/<your suite>', spec: '<your spec>', provider: 'https://<the real provider>'})");
      var2.put("karateSummary", "../../../karate-summary.html");
      io.karatelabs.ext.common.ReportAssetsInstaller.a(var2, this.suite == null ? null : this.suite.getReportAssets());

      try {
         io.karatelabs.ext.common.ReportAssetsInstaller.a(this.reportDir.resolve("ext/contract/contract-data.js"), "KARATE_PAIR", var2);
      } catch (IOException var4) {
         logger.debug("contract: could not write the empty-state page data: {}", var4.toString());
      }
   }
}
