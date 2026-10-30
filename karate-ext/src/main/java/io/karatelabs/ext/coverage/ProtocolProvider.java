package io.karatelabs.ext.coverage;

import io.karatelabs.ext.contract.Contract;
import io.karatelabs.ext.contract.ContractLoader;
import io.karatelabs.ext.contract.Protocol;
import io.karatelabs.ext.scaffold.ScaffoldEmitter;

public interface ProtocolProvider {
   Protocol protocol();

   AddressMatcher matcher(Contract var1);

   default ContractLoader loader() {
      return null;
   }

   default ScaffoldEmitter scaffoldEmitter() {
      return null;
   }
}
