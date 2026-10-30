package io.karatelabs.ext.grpc;

import io.karatelabs.core.Channel;
import io.karatelabs.core.ChannelFactory;
import io.karatelabs.core.ScenarioRuntime;
import io.karatelabs.ext.common.keycheck.Main;
import java.util.Map;

public class GrpcChannelFactory implements ChannelFactory {
   private static volatile boolean gated;
   private final io.karatelabs.ext.grpc.GrpcExt ext;

   public GrpcChannelFactory() {
      this(null);
   }

   public GrpcChannelFactory(io.karatelabs.ext.grpc.GrpcExt ext) {
      this.ext = ext;
   }

   public Channel create(ScenarioRuntime scenarioRuntime, Map<String, Object> config) {
      if (!gated) {
         Main.requireProduct("grpc", new String[]{"grpc"});
         gated = true;
      }

      return new io.karatelabs.ext.grpc.GrpcChannel(config, this.ext);
   }
}
