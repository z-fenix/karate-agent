package io.karatelabs.ext.contract;

import java.util.List;
import java.util.Map;

public record Service(String name, List<Operation> operations, Map<String, Object> ext) {

   public Service(String var1, List<Operation> var2) {
      this(var1, var2, Map.of());
   }

}
