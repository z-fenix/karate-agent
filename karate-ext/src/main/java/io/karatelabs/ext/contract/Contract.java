package io.karatelabs.ext.contract;

import java.util.List;
import java.util.Map;

public record Contract(String id, Protocol protocol, String title, String version, List<Service> services, Map<String, Object> ext) {


   public List<Operation> operations() {
      return this.services.stream().flatMap(var0 -> var0.operations().stream()).toList();
   }
}
