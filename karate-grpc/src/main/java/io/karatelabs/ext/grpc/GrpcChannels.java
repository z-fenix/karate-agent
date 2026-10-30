package io.karatelabs.ext.grpc;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.TlsChannelCredentials;
import io.grpc.TlsChannelCredentials.Builder;
import io.karatelabs.common.Resource;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class GrpcChannels {
   private static final Logger logger = LoggerFactory.getLogger("karate.grpc");

   private GrpcChannels() {
   }

   static ManagedChannel build(io.karatelabs.ext.grpc.GrpcConsumer caller) {
      if (caller.trustCert == null && caller.clientCert == null) {
         return ManagedChannelBuilder.forAddress(caller.host, caller.port).usePlaintext().build();
      } else {
         try {
            Builder credsBuilder = TlsChannelCredentials.newBuilder();
            if (caller.trustCert != null) {
               Resource trustCertResource = Resource.path(caller.ref(caller.trustCert));
               logger.debug("using trust cert: {}", trustCertResource);
               credsBuilder.trustManager(trustCertResource.getPath().toFile());
            }

            if (caller.clientCert != null) {
               Resource clientCertResource = Resource.path(caller.ref(caller.clientCert));
               Resource clientKeyResource = Resource.path(caller.ref(caller.clientKey));
               logger.debug("using client cert: {}, key: {}", clientCertResource, clientKeyResource);
               credsBuilder.keyManager(clientCertResource.getPath().toFile(), clientKeyResource.getPath().toFile());
            }

            return io.grpc.Grpc.newChannelBuilderForAddress(caller.host, caller.port, credsBuilder.build()).build();
         } catch (IOException var4) {
            throw new RuntimeException("could not load gRPC TLS material (trustCert/clientCert/clientKey): " + var4.getMessage(), var4);
         }
      }
   }
}
