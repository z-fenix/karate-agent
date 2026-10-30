package io.karatelabs.ext.jsonschema.validation;

public enum ValidationDirection {
   NONE("none"),
   REQUEST("request"),
   RESPONSE("response");

   private final String wire;

   private ValidationDirection(String var3) {
      this.wire = var3;
   }

   public String wire() {
      return this.wire;
   }

   public static ValidationDirection of(String var0) {
      for (ValidationDirection var4 : values()) {
         if (var4.wire.equals(var0)) {
            return var4;
         }
      }

      throw new ValidationException("unknown direction '" + var0 + "' (known: none, request, response)");
   }

   String excuses() {
      return switch (this) {
         case NONE -> null;
         case REQUEST -> "readOnly";
         case RESPONSE -> "writeOnly";
      };
   }
}
