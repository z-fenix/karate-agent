package io.karatelabs.ext.kafka.coverage;

import io.karatelabs.ext.contract.Contract;
import io.karatelabs.ext.contract.ContractLoader;
import io.karatelabs.ext.contract.Protocol;
import io.karatelabs.ext.coverage.AddressMatcher;
import io.karatelabs.ext.coverage.ProtocolProvider;
import io.karatelabs.ext.scaffold.ScaffoldEmitter;

public class KafkaProtocolProvider implements ProtocolProvider {
   public Protocol protocol() {
      return Protocol.KAFKA;
   }

   public AddressMatcher matcher(Contract contract) {
      return new KafkaAddressMatcher();
   }

   public ContractLoader loader() {
      return new KafkaContractLoader();
   }

   public ScaffoldEmitter scaffoldEmitter() {
      return new KafkaScaffoldEmitter();
   }
}
