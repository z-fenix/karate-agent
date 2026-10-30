package io.karatelabs.ext.contract;

import java.util.Map;

public record Address(Address.Kind kind, String primary, String qualifier, Map<String, Object> ext) {

   public Address(Address.Kind var1, String var2, String var3) {
      this(var1, var2, var3, Map.of());
   }


   public static enum Kind {
      PATH,
      RPC,
      TOPIC,
      CHANNEL;
   }
}
