package io.karatelabs.ext.robot.recording;

import io.karatelabs.driver.cdp.CdpClient;
import io.karatelabs.driver.cdp.CdpDriver;
import io.karatelabs.driver.cdp.CdpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class CdpPageApiFactory {
   private static final Logger a = LoggerFactory.getLogger(CdpPageApiFactory.class);

   private CdpPageApiFactory() {
   }

   public static CdpPageApi a(CdpDriver var0) {
      final CdpClient var1 = var0.getCdpClient();
      return new CdpPageApi() {
         @Override
         public CompletableFuture<io.karatelabs.ext.robot.recording.CdpPageApi.ScriptEvaluationResult> a(String var1x) {
            return var1.method("Network.getResponseBody")
               .param("requestId", var1x)
               .sendAsync()
               .thenApply(
                  var0x -> new io.karatelabs.ext.robot.recording.CdpPageApi.ScriptEvaluationResult(
                     io.karatelabs.ext.robot.recording.CdpPageApiFactory.a(var0x).getResultAsString("body"), Boolean.TRUE.equals(var0x.getResultAsBoolean("base64Encoded"))
                  )
               );
         }

         @Override
         public CompletableFuture<String> b(String var1x) {
            return var1.method("Network.getRequestPostData").param("requestId", var1x).sendAsync().thenApply(var0x -> io.karatelabs.ext.robot.recording.CdpPageApiFactory.a(var0x).getResultAsString("postData"));
         }
      };
   }

   private static CdpResponse a(CdpResponse var0) {
      if (var0.isError()) {
         String var1 = String.valueOf(var0.getErrorMessage());
         if (!var1.startsWith("No resource with given identifier") && !var1.startsWith("No data found for resource")) {
            throw new IllegalStateException(var1);
         } else {
            throw new io.karatelabs.ext.robot.recording.CdpPageApi.CdpPageException(var1);
         }
      } else {
         return var0;
      }
   }

   public static void a(TrafficRecorder var0, CdpDriver var1) {
      var1.addCdpEventListener(var0);
      CdpClient var2 = var1.getCdpClient();
      String var3 = null;
      String var4 = null;
      List<Map<String, Object>> var5 = List.of();
      ArrayList<java.lang.String> var6 = new ArrayList();

      try {
         Map<java.lang.String, java.lang.String> var7 = var2.method("Target.getTargetInfo").send().getResult("targetInfo");
         if (var7 != null) {
            var3 = (String)var7.get("targetId");
            var4 = (String)var7.get("browserContextId");
         }

         List var8 = var2.browserMethod("Target.getTargets").send().getResult("targetInfos");
         if (var8 != null) {
            var5 = var8;
         }
      } catch (Exception var11) {
         a.debug("traffic scope snapshot skipped: {}", var11.getMessage());
      }

      try {
         Object var13 = var1.script(
            "navigator.serviceWorker ? navigator.serviceWorker.getRegistrations().then(function(rs) { return rs.map(function(r) { return r.scope; }); }) : []"
         );
         if (var13 instanceof List) {
            for (Object var10 : (List)var13) {
               var6.add(String.valueOf(var10));
            }
         }
      } catch (Exception var12) {
         a.debug("service worker registrations not read: {}", var12.getMessage());
      }

      var0.a(var3, var4, var5, var6);
   }

   public static void b(TrafficRecorder var0, CdpDriver var1) {
      var1.removeCdpEventListener(var0);
   }
}
