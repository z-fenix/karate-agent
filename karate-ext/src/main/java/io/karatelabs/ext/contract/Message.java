package io.karatelabs.ext.contract;

import java.util.Map;

public record Message(String name, TypeRef type, String contentType, Message.Role role, Map<String, Object> ext) {

   public Message(String var1, TypeRef var2, String var3, Message.Role var4) {
      this(var1, var2, var3, var4, Map.of());
   }


   public static enum Role {
      BODY,
      PARAM,
      KEY,
      VALUE,
      HEADER,
      METADATA;
   }
}
