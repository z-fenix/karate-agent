package io.karatelabs.ext.grpc;

import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.Descriptors.FileDescriptor;
import com.google.protobuf.Descriptors.MethodDescriptor;
import com.google.protobuf.Descriptors.ServiceDescriptor;
import io.grpc.Channel;
import io.grpc.ClientInterceptor;
import io.grpc.ClientInterceptors;
import io.grpc.ManagedChannel;
import io.grpc.Metadata;
import io.grpc.Metadata.Key;
import io.grpc.stub.MetadataUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class ReflectionCatalog implements io.karatelabs.ext.grpc.GrpcCatalog {
   private static final Logger logger = LoggerFactory.getLogger("karate.grpc");
   private final List<FileDescriptor> files;

   private ReflectionCatalog(List<FileDescriptor> files) {
      this.files = files;
   }

   static io.karatelabs.ext.grpc.ReflectionCatalog fromService(io.karatelabs.ext.grpc.GrpcConsumer caller) {
      ManagedChannel channel = io.karatelabs.ext.grpc.GrpcChannels.build(caller);

      io.karatelabs.ext.grpc.ReflectionCatalog var6;
      try {
         Channel reflectChannel = channel;
         if (caller.metadataRequest != null && !caller.metadataRequest.isEmpty()) {
            Metadata headers = new Metadata();
            caller.metadataRequest.forEach((k, v) -> headers.put(Key.of(k, Metadata.ASCII_STRING_MARSHALLER), v));
            reflectChannel = ClientInterceptors.intercept(channel, new ClientInterceptor[]{MetadataUtils.newAttachHeadersInterceptor(headers)});
         }

         long timeout = caller.timeout != null ? caller.timeout : 30000L;
         logger.debug("reflecting service '{}' off {}:{}", new Object[]{caller.service, caller.host, caller.port});
         List<FileDescriptor> files = io.karatelabs.ext.grpc.GrpcReflection.fetch(reflectChannel, caller.service, timeout);
         var6 = new io.karatelabs.ext.grpc.ReflectionCatalog(files);
      } finally {
         channel.shutdownNow();

         try {
            channel.awaitTermination(5L, TimeUnit.SECONDS);
         } catch (InterruptedException var13) {
            Thread.currentThread().interrupt();
         }
      }

      return var6;
   }

   @Override
   public io.karatelabs.ext.grpc.ProtobufUtils.MethodType methodType(String service, String method) {
      MethodDescriptor md = this.method(service, method);
      return new io.karatelabs.ext.grpc.ProtobufUtils.MethodType(md.isClientStreaming(), md.isServerStreaming());
   }

   @Override
   public List<Map<String, Object>> methods(String service) {
      return io.karatelabs.ext.grpc.ProtobufUtils.methodsOf(this.service(service));
   }

   @Override
   public Map<String, Object> describe(String type) {
      Descriptor d = this.findMessage(type);
      if (d != null) {
         return io.karatelabs.ext.grpc.ProtobufUtils.describeMessage(d);
      } else {
         List<String> available = new ArrayList<>();

         for (FileDescriptor fd : this.files) {
            for (Descriptor msg : fd.getMessageTypes()) {
               available.add(msg.getName());
            }
         }

         throw new RuntimeException("no such message type '" + type + "' (via reflection) — available: " + available);
      }
   }

   @Override
   public Descriptor requestType(String service, String method) {
      return this.method(service, method).getInputType();
   }

   @Override
   public Descriptor responseType(String service, String method) {
      return this.method(service, method).getOutputType();
   }

   @Override
   public String fullMethodName(String service, String method) {
      return io.grpc.MethodDescriptor.generateFullMethodName(this.service(service).getFullName(), method);
   }

   private ServiceDescriptor service(String service) {
      for (FileDescriptor fd : this.files) {
         ServiceDescriptor svc = fd.findServiceByName(service);
         if (svc != null) {
            return svc;
         }
      }

      List<String> available = new ArrayList<>();

      for (FileDescriptor fdx : this.files) {
         for (ServiceDescriptor svc : fdx.getServices()) {
            available.add(svc.getName());
         }
      }

      throw new RuntimeException("no such service '" + service + "' (via reflection) — available: " + available);
   }

   private MethodDescriptor method(String service, String method) {
      ServiceDescriptor svc = this.service(service);
      MethodDescriptor md = svc.findMethodByName(method);
      if (md == null) {
         List<String> available = new ArrayList<>();
         svc.getMethods().forEach(m -> available.add(m.getName()));
         throw new RuntimeException("no such method '" + method + "' on " + service + " (via reflection) — available: " + available);
      } else {
         return md;
      }
   }

   private Descriptor findMessage(String type) {
      for (FileDescriptor fd : this.files) {
         for (Descriptor msg : fd.getMessageTypes()) {
            if (msg.getName().equals(type) || msg.getFullName().equals(type)) {
               return msg;
            }
         }
      }

      return null;
   }
}
