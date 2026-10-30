package io.karatelabs.ext.grpc;

import com.google.protobuf.Descriptors.Descriptor;
import com.squareup.wire.schema.internal.parser.ProtoFileElement;
import com.squareup.wire.schema.internal.parser.RpcElement;
import com.squareup.wire.schema.internal.parser.ServiceElement;
import io.confluent.kafka.schemaregistry.protobuf.ProtobufSchema;
import io.grpc.MethodDescriptor;
import java.util.List;
import java.util.Map;

class ProtoCatalog implements io.karatelabs.ext.grpc.GrpcCatalog {
   private final String proto;
   private final List<String> protoRoots;
   private ProtobufSchema schema;

   ProtoCatalog(String proto, List<String> protoRoots) {
      this.proto = proto;
      this.protoRoots = protoRoots;
   }

   private ProtobufSchema schema() {
      if (this.schema == null) {
         this.schema = io.karatelabs.ext.grpc.ProtobufUtils.toSchema(this.proto, this.protoRoots);
      }

      return this.schema;
   }

   @Override
   public io.karatelabs.ext.grpc.ProtobufUtils.MethodType methodType(String service, String method) {
      return io.karatelabs.ext.grpc.ProtobufUtils.methodType(this.proto, this.protoRoots, service, method);
   }

   @Override
   public List<Map<String, Object>> methods(String service) {
      return io.karatelabs.ext.grpc.ProtobufUtils.methods(this.proto, this.protoRoots, service);
   }

   @Override
   public Map<String, Object> describe(String type) {
      return io.karatelabs.ext.grpc.ProtobufUtils.describe(this.proto, this.protoRoots, type);
   }

   @Override
   public Descriptor requestType(String service, String method) {
      return this.schema().toDescriptor(this.rpc(service, method).getRequestType());
   }

   @Override
   public Descriptor responseType(String service, String method) {
      return this.schema().toDescriptor(this.rpc(service, method).getResponseType());
   }

   @Override
   public String fullMethodName(String service, String method) {
      String packageName = this.schema().rawSchema().getPackageName();
      return MethodDescriptor.generateFullMethodName(packageName == null ? service : packageName + "." + service, method);
   }

   private RpcElement rpc(String service, String method) {
      ProtoFileElement element = this.schema().rawSchema();
      ServiceElement svc = element.getServices()
         .stream()
         .filter(x -> x.getName().equals(service))
         .findFirst()
         .orElseThrow(
            () -> new RuntimeException(
               "no such service '" + service + "' in proto — available: " + element.getServices().stream().map(ServiceElement::getName).toList()
            )
         );
      return svc.getRpcs()
         .stream()
         .filter(x -> x.getName().equals(method))
         .findFirst()
         .orElseThrow(
            () -> new RuntimeException(
               "no such method '" + method + "' on " + service + " — available: " + svc.getRpcs().stream().map(RpcElement::getName).toList()
            )
         );
   }
}
