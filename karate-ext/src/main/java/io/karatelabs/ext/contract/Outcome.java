package io.karatelabs.ext.contract;

import java.util.Map;

public record Outcome(String code, Outcome.Kind kind, TypeRef payload, String description, Map<String, Object> ext) {

   public Outcome(String var1, Outcome.Kind var2, TypeRef var3, String var4) {
      this(var1, var2, var3, var4, Map.of());
   }


   public static enum Kind {
      SUCCESS,
      CLIENT_ERROR,
      SERVER_ERROR,
      INFO,
      ACK,
      NACK;
   }
}
