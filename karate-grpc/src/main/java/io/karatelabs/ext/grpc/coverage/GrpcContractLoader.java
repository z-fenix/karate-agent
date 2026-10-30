package io.karatelabs.ext.grpc.coverage;

import com.google.protobuf.Descriptors.Descriptor;
import com.squareup.wire.schema.Location;
import com.squareup.wire.schema.internal.parser.ProtoFileElement;
import com.squareup.wire.schema.internal.parser.ProtoParser;
import com.squareup.wire.schema.internal.parser.RpcElement;
import com.squareup.wire.schema.internal.parser.ServiceElement;
import io.confluent.kafka.schemaregistry.protobuf.ProtobufSchema;
import io.karatelabs.ext.contract.Address;
import io.karatelabs.ext.contract.Contract;
import io.karatelabs.ext.contract.ContractLoader;
import io.karatelabs.ext.contract.GrpcBinding;
import io.karatelabs.ext.contract.Interaction;
import io.karatelabs.ext.contract.Message;
import io.karatelabs.ext.contract.Operation;
import io.karatelabs.ext.contract.Protocol;
import io.karatelabs.ext.contract.Service;
import io.karatelabs.ext.contract.TypeRef;
import io.karatelabs.ext.contract.Address.Kind;
import io.karatelabs.ext.contract.ContractLoader.LoaderOptions;
import io.karatelabs.ext.contract.Message.Role;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class GrpcContractLoader implements ContractLoader {
   public Protocol protocol() {
      return Protocol.GRPC;
   }

   public Contract load(List<Path> artifacts, LoaderOptions opts) {
      try {
         return fromProtos(artifacts);
      } catch (IOException var4) {
         throw new RuntimeException(var4);
      }
   }

   public static Contract fromProtos(List<Path> protoFiles) throws IOException {
      List<Operation> operations = new ArrayList<>();
      LinkedHashSet<String> services = new LinkedHashSet<>();
      String firstPackage = null;
      Map<Path, ProtobufSchema> schemaCache = new ConcurrentHashMap<>();

      for (Path protoFile : protoFiles) {
         String text = Files.readString(protoFile, StandardCharsets.UTF_8);
         ProtoFileElement el = ProtoParser.Companion.parse(Location.get(protoFile.toString()), text);
         String pkg = el.getPackageName();
         if (firstPackage == null) {
            firstPackage = pkg;
         }

         for (ServiceElement svc : el.getServices()) {
            services.add(svc.getName());

            for (RpcElement rpc : svc.getRpcs()) {
               operations.add(toOperation(svc.getName(), rpc, pkg, protoFile, schemaCache));
            }
         }
      }

      Map<String, Object> ext = new LinkedHashMap<>();
      ext.put("specType", "proto");
      ext.put("services", new ArrayList<>(services));
      String title = firstPackage != null ? firstPackage : "gRPC";
      List<Service> svc = List.of(new Service(title, operations));
      return new Contract(title, Protocol.GRPC, title, null, svc, ext);
   }

   private static Operation toOperation(String service, RpcElement rpc, String pkg, Path protoFile, Map<Path, ProtobufSchema> schemaCache) {
      String id = service + "/" + rpc.getName();
      Address address = new Address(Kind.RPC, id, null);
      boolean clientStream = rpc.getRequestStreaming();
      boolean serverStream = rpc.getResponseStreaming();
      Interaction interaction = interaction(clientStream, serverStream);
      String requestType = rpc.getRequestType();
      String responseType = rpc.getResponseType();
      List<Message> inputs = List.of(new Message(requestType, typeRef(requestType, protoFile, schemaCache), null, Role.BODY));
      List<Message> outputs = List.of(new Message(responseType, typeRef(responseType, protoFile, schemaCache), null, Role.BODY));
      Map<String, Object> ext = new LinkedHashMap<>();
      ext.put("binding", new GrpcBinding(service, rpc.getName(), pkg, clientStream, serverStream));
      ext.put("package", pkg);
      ext.put("requestType", requestType);
      ext.put("responseType", responseType);
      return new Operation(id, address, interaction, inputs, outputs, List.of(), null, List.of(service), false, ext);
   }

   private static TypeRef typeRef(String typeName, Path protoFile, Map<Path, ProtobufSchema> schemaCache) {
      return new TypeRef(typeName, () -> {
         try {
            ProtobufSchema schema = schemaCache.computeIfAbsent(protoFile, GrpcContractLoader::schemaFor);
            Descriptor d = schema.toDescriptor(typeName);
            return d == null ? null : new ProtobufTypeModel(d, ProtovalidateConstraints.from(schema.rawSchema()));
         } catch (RuntimeException var5) {
            return null;
         }
      });
   }

   private static ProtobufSchema schemaFor(Path protoFile) {
      Path dir = protoFile.toAbsolutePath().getParent();
      List<String> roots = dir == null ? List.of() : List.of(dir.toString());
      return io.karatelabs.ext.grpc.ProtobufUtils.toSchema(protoFile.toString(), roots);
   }

   private static Interaction interaction(boolean clientStream, boolean serverStream) {
      if (clientStream && serverStream) {
         return Interaction.BIDI_STREAM;
      } else if (clientStream) {
         return Interaction.CLIENT_STREAM;
      } else {
         return serverStream ? Interaction.SERVER_STREAM : Interaction.REQUEST_RESPONSE;
      }
   }
}
