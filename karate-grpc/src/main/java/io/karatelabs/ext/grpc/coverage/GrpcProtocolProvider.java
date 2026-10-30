package io.karatelabs.ext.grpc.coverage;

import io.karatelabs.ext.contract.Contract;
import io.karatelabs.ext.contract.ContractLoader;
import io.karatelabs.ext.contract.Protocol;
import io.karatelabs.ext.coverage.AddressMatcher;
import io.karatelabs.ext.coverage.ProtocolProvider;
import io.karatelabs.ext.scaffold.ScaffoldEmitter;

public class GrpcProtocolProvider implements ProtocolProvider {
   public Protocol protocol() {
      return Protocol.GRPC;
   }

   public AddressMatcher matcher(Contract contract) {
      return new GrpcAddressMatcher();
   }

   public ContractLoader loader() {
      return new GrpcContractLoader();
   }

   public ScaffoldEmitter scaffoldEmitter() {
      return new GrpcScaffoldEmitter();
   }
}
