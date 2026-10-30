package io.karatelabs.ext.max.jsapi;

import java.util.Map;

final class EvidenceProtocolMetadata {
   private EvidenceProtocolMetadata() {
   }

   static Map<String, Object> a(Map<String, Object> var0) {
      var0.put("protocol", "evidence/1");
      return var0;
   }

   static void a(RunMetadataStore var0, String var1) {
      var0.a(var1, var0x -> {
         var0x.put("protocol", "evidence/1");
         var0x.put("evidenceFinalized", true);
      });
   }
}
