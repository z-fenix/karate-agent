package io.karatelabs.ext.rules;

import io.karatelabs.ext.common.keycheck.Main;
import org.junit.Test;
import static org.junit.Assert.*;

public class RebuiltLicenseTest {
   @Test
   public void rebuiltDistributionSkipsLicenseAndProductChecks() {
      Main.requireLicensed("serve");
      Main.requireProduct("grpc", "grpc");
      assertEquals(Boolean.TRUE, Main.readRuntimeLicense().get("validationSkipped"));
      assertTrue(Main.licenseSummary().contains("skipped"));
   }

   @Test
   public void serveAndRobotPromptsArePackaged() {
      for (String prompt : new String[]{"serve/core", "serve/live", "robot/core", "robot/live", "robot/loop"}) {
         assertNotNull(prompt, getClass().getClassLoader().getResource("META-INF/karate/prompt/" + prompt + ".md"));
      }
   }
}
