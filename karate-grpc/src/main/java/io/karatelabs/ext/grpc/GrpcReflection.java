package io.karatelabs.ext.grpc;

import com.google.protobuf.ByteString;
import com.google.protobuf.DescriptorProtos.FileDescriptorProto;
import com.google.protobuf.Descriptors.DescriptorValidationException;
import com.google.protobuf.Descriptors.FileDescriptor;
import io.grpc.Channel;
import io.grpc.StatusRuntimeException;
import io.grpc.Status.Code;
import io.grpc.reflection.v1.ServerReflectionGrpc;
import io.grpc.reflection.v1.ServerReflectionRequest;
import io.grpc.reflection.v1.ServerReflectionResponse;
import io.grpc.reflection.v1.ServiceResponse;
import io.grpc.stub.StreamObserver;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class GrpcReflection {
   private static final Logger logger = LoggerFactory.getLogger("karate.grpc");
   private static final Object COMPLETED = new Object();

   private GrpcReflection() {
   }

   static List<FileDescriptor> fetch(Channel channel, String service, long timeoutMs) {
      final BlockingQueue<Object> queue = new LinkedBlockingQueue<>();
      StreamObserver<ServerReflectionRequest> requests = ServerReflectionGrpc.newStub(channel)
         .serverReflectionInfo(new StreamObserver<ServerReflectionResponse>() {
            public void onNext(ServerReflectionResponse response) {
               queue.add(response);
            }

            public void onError(Throwable t) {
               queue.add(t);
            }

            public void onCompleted() {
               queue.add(io.karatelabs.ext.grpc.GrpcReflection.COMPLETED);
            }
         });

      try {
         requests.onNext(ServerReflectionRequest.newBuilder().setListServices("").build());
         String fqn = resolveServiceName(take(queue, timeoutMs, service), service);
         Map<String, FileDescriptorProto> protos = new LinkedHashMap<>();
         requests.onNext(ServerReflectionRequest.newBuilder().setFileContainingSymbol(fqn).build());
         addFiles(take(queue, timeoutMs, service), protos);
         resolveDependencies(requests, queue, protos, timeoutMs, service);
         requests.onCompleted();
         return link(protos);
      } catch (io.karatelabs.ext.grpc.GrpcReflection.ReflectionException var8) {
         throw var8;
      } catch (Exception var9) {
         requests.onError(var9);
         throw new RuntimeException(reflectionFailure(service, var9), var9);
      }
   }

   private static void resolveDependencies(
      StreamObserver<ServerReflectionRequest> requests, BlockingQueue<Object> queue, Map<String, FileDescriptorProto> protos, long timeoutMs, String service
   ) throws InterruptedException {
      boolean progressed = true;

      while (progressed) {
         progressed = false;

         for (FileDescriptorProto proto : new ArrayList<>(protos.values())) {
            for (String dep : proto.getDependencyList()) {
               if (!protos.containsKey(dep)) {
                  requests.onNext(ServerReflectionRequest.newBuilder().setFileByFilename(dep).build());
                  addFiles(take(queue, timeoutMs, service), protos);
                  progressed = true;
               }
            }
         }
      }
   }

   private static void addFiles(ServerReflectionResponse response, Map<String, FileDescriptorProto> protos) {
      if (response.hasErrorResponse()) {
         throw new io.karatelabs.ext.grpc.GrpcReflection.ReflectionException(
            "reflection error " + response.getErrorResponse().getErrorCode() + ": " + response.getErrorResponse().getErrorMessage()
         );
      } else {
         for (ByteString bytes : response.getFileDescriptorResponse().getFileDescriptorProtoList()) {
            try {
               FileDescriptorProto proto = FileDescriptorProto.parseFrom(bytes);
               protos.putIfAbsent(proto.getName(), proto);
            } catch (Exception var5) {
               throw new io.karatelabs.ext.grpc.GrpcReflection.ReflectionException("could not parse a reflected FileDescriptorProto: " + var5.getMessage());
            }
         }
      }
   }

   private static String resolveServiceName(ServerReflectionResponse response, String service) {
      List<String> names = new ArrayList<>();

      for (ServiceResponse sr : response.getListServicesResponse().getServiceList()) {
         names.add(sr.getName());
      }

      for (String name : names) {
         if (name.equals(service)) {
            return name;
         }
      }

      for (String namex : names) {
         if (simpleName(namex).equals(service)) {
            return namex;
         }
      }

      List<String> visible = names.stream().filter(n -> !n.startsWith("grpc.reflection.")).toList();
      throw new io.karatelabs.ext.grpc.GrpcReflection.ReflectionException(
         "no such service '" + service + "' on the server (via reflection) — available: " + (visible.isEmpty() ? names : visible)
      );
   }

   private static List<FileDescriptor> link(Map<String, FileDescriptorProto> protos) {
      Map<String, FileDescriptor> built = new HashMap<>();
      List<FileDescriptor> out = new ArrayList<>();

      for (String name : protos.keySet()) {
         out.add(buildFile(name, protos, built));
      }

      return out;
   }

   private static FileDescriptor buildFile(String name, Map<String, FileDescriptorProto> protos, Map<String, FileDescriptor> built) {
      FileDescriptor existing = built.get(name);
      if (existing != null) {
         return existing;
      } else {
         FileDescriptorProto proto = protos.get(name);
         if (proto == null) {
            throw new io.karatelabs.ext.grpc.GrpcReflection.ReflectionException("the server did not return a descriptor for the imported file '" + name + "'");
         } else {
            FileDescriptor[] deps = new FileDescriptor[proto.getDependencyCount()];

            for (int i = 0; i < proto.getDependencyCount(); i++) {
               deps[i] = buildFile(proto.getDependency(i), protos, built);
            }

            try {
               FileDescriptor fd = FileDescriptor.buildFrom(proto, deps);
               built.put(name, fd);
               return fd;
            } catch (DescriptorValidationException var7) {
               throw new io.karatelabs.ext.grpc.GrpcReflection.ReflectionException("could not link reflected descriptor '" + name + "': " + var7.getMessage());
            }
         }
      }
   }

   private static ServerReflectionResponse take(BlockingQueue<Object> queue, long timeoutMs, String service) throws InterruptedException {
      Object item = queue.poll(timeoutMs, TimeUnit.MILLISECONDS);
      if (item == null) {
         throw new io.karatelabs.ext.grpc.GrpcReflection.ReflectionException(
            "gRPC server reflection timed out after "
               + timeoutMs
               + "ms for service '"
               + service
               + "'\nhint: the server did not answer reflection in time — raise `timeout` in connect(), or pass `proto:` to skip reflection"
         );
      } else if (item == COMPLETED) {
         throw new io.karatelabs.ext.grpc.GrpcReflection.ReflectionException(
            "the server closed the reflection stream before answering for service '" + service + "'"
         );
      } else if (item instanceof Throwable t) {
         throw new io.karatelabs.ext.grpc.GrpcReflection.ReflectionException(reflectionFailure(service, t));
      } else {
         return (ServerReflectionResponse)item;
      }
   }

   private static String reflectionFailure(String service, Throwable cause) {
      StatusRuntimeException sre = unwrap(cause);
      Code code = sre != null ? sre.getStatus().getCode() : null;
      StringBuilder sb = new StringBuilder("gRPC server reflection failed for service '").append(service).append('\'');
      if (code != null) {
         sb.append(": ").append(code);
      } else if (cause != null && cause.getMessage() != null) {
         sb.append(": ").append(cause.getMessage());
      }

      if (code == Code.UNIMPLEMENTED) {
         sb.append(
            "\nhint: the server does not expose grpc.reflection.v1.ServerReflection — pass `proto:` to connect with a local .proto instead (or enable reflection on the server)"
         );
      } else if (code == Code.UNAVAILABLE) {
         sb.append("\nhint: the server is unreachable — check host/port and that it is running (or pass `proto:`)");
      } else {
         sb.append("\nhint: pass `proto:` to connect with a local .proto instead of server reflection");
      }

      return sb.toString();
   }

   private static StatusRuntimeException unwrap(Throwable t) {
      for (Throwable cur = t; cur != null; cur = cur.getCause()) {
         if (cur instanceof StatusRuntimeException sre) {
            return sre;
         }
      }

      return null;
   }

   private static String simpleName(String fqn) {
      int dot = fqn.lastIndexOf(46);
      return dot < 0 ? fqn : fqn.substring(dot + 1);
   }

   static final class ReflectionException extends RuntimeException {
      ReflectionException(String message) {
         super(message);
      }
   }
}
