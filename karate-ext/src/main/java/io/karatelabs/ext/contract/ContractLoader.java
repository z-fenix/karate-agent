package io.karatelabs.ext.contract;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public interface ContractLoader {
   Contract load(List<Path> var1, ContractLoader.LoaderOptions var2);

   Protocol protocol();

   public record LoaderOptions(String alias, String referenceDate, Map<String, Object> config) {


      public static ContractLoader.LoaderOptions empty() {
         return new ContractLoader.LoaderOptions(null, null, Map.of());
      }
   }
}
