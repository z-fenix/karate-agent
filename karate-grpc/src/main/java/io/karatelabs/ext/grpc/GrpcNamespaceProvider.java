package io.karatelabs.ext.grpc;

import io.karatelabs.ext.common.run.JsNamespaceProvider;
import java.nio.file.Path;

public class GrpcNamespaceProvider implements JsNamespaceProvider {
   public String name() {
      return "Grpc";
   }

   public Object namespace() {
      return new io.karatelabs.ext.grpc.Grpc();
   }

   public Object namespace(Path root) {
      return new io.karatelabs.ext.grpc.Grpc().withRoot(root);
   }

   public String skill() {
      return "grpc";
   }
}
