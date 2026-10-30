package io.karatelabs.ext.grpc;

import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.Descriptors.MethodDescriptor;
import com.google.protobuf.Descriptors.ServiceDescriptor;
import com.google.protobuf.Descriptors.FieldDescriptor.JavaType;
import com.squareup.wire.schema.Location;
import com.squareup.wire.schema.internal.parser.ProtoFileElement;
import com.squareup.wire.schema.internal.parser.ProtoParser;
import com.squareup.wire.schema.internal.parser.RpcElement;
import com.squareup.wire.schema.internal.parser.ServiceElement;
import com.squareup.wire.schema.internal.parser.TypeElement;
import io.confluent.kafka.schemaregistry.protobuf.ProtobufSchema;
import io.karatelabs.common.FileUtils;
import io.karatelabs.common.Resource;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ProtobufUtils {
   public static io.karatelabs.ext.grpc.ProtobufUtils.MethodType methodType(String proto, List<String> protoRoots, String service, String method) {
      ProtoFileElement element = toSchema(proto, protoRoots).rawSchema();
      ServiceElement svc = element.getServices()
         .stream()
         .filter(x -> x.getName().equals(service))
         .findFirst()
         .orElseThrow(
            () -> new RuntimeException(
               "no such service '" + service + "' in proto — available: " + element.getServices().stream().map(ServiceElement::getName).toList()
            )
         );
      RpcElement rpc = svc.getRpcs()
         .stream()
         .filter(x -> x.getName().equals(method))
         .findFirst()
         .orElseThrow(
            () -> new RuntimeException(
               "no such method '" + method + "' on " + service + " — available: " + svc.getRpcs().stream().map(RpcElement::getName).toList()
            )
         );
      return new io.karatelabs.ext.grpc.ProtobufUtils.MethodType(rpc.getRequestStreaming(), rpc.getResponseStreaming());
   }

   public static List<Map<String, Object>> methods(String proto, List<String> protoRoots, String service) {
      ProtoFileElement element = toSchema(proto, protoRoots).rawSchema();
      ServiceElement svc = element.getServices()
         .stream()
         .filter(x -> x.getName().equals(service))
         .findFirst()
         .orElseThrow(
            () -> new RuntimeException(
               "no such service '" + service + "' in proto — available: " + element.getServices().stream().map(ServiceElement::getName).toList()
            )
         );
      List<Map<String, Object>> out = new ArrayList<>();

      for (RpcElement rpc : svc.getRpcs()) {
         Map<String, Object> m = new LinkedHashMap<>();
         m.put("method", rpc.getName());
         m.put("kind", kindOf(rpc.getRequestStreaming(), rpc.getResponseStreaming()));
         m.put("requestType", rpc.getRequestType());
         m.put("responseType", rpc.getResponseType());
         m.put("requestStreaming", rpc.getRequestStreaming());
         m.put("responseStreaming", rpc.getResponseStreaming());
         out.add(m);
      }

      return out;
   }

   public static List<Map<String, Object>> methodsOf(ServiceDescriptor svc) {
      List<Map<String, Object>> out = new ArrayList<>();

      for (MethodDescriptor md : svc.getMethods()) {
         Map<String, Object> m = new LinkedHashMap<>();
         m.put("method", md.getName());
         m.put("kind", kindOf(md.isClientStreaming(), md.isServerStreaming()));
         m.put("requestType", md.getInputType().getName());
         m.put("responseType", md.getOutputType().getName());
         m.put("requestStreaming", md.isClientStreaming());
         m.put("responseStreaming", md.isServerStreaming());
         out.add(m);
      }

      return out;
   }

   public static String kindOf(boolean request, boolean response) {
      if (request && response) {
         return "bidi";
      } else if (request) {
         return "client-streaming";
      } else {
         return response ? "server-streaming" : "unary";
      }
   }

   public static Map<String, Object> describe(String proto, List<String> protoRoots, String type) {
      ProtobufSchema schema = toSchema(proto, protoRoots);

      Descriptor d;
      try {
         d = schema.toDescriptor(type);
      } catch (Exception var8) {
         d = null;
      }

      if (d != null) {
         return describeMessage(d);
      } else {
         List<String> available = new ArrayList<>();

         for (TypeElement t : schema.rawSchema().getTypes()) {
            available.add(t.getName());
         }

         throw new RuntimeException("no such message type '" + type + "' in proto — available: " + available);
      }
   }

   public static Map<String, Object> describeMessage(Descriptor d) {
      List<Map<String, Object>> fields = new ArrayList<>();

      for (FieldDescriptor fd : d.getFields()) {
         Map<String, Object> f = new LinkedHashMap<>();
         f.put("name", fd.getName());
         f.put("type", fieldType(fd));
         f.put("number", fd.getNumber());
         if (fd.isRepeated() && !fd.isMapField()) {
            f.put("repeated", true);
         }

         if (fd.getJavaType() == JavaType.ENUM) {
            List<String> values = new ArrayList<>();
            fd.getEnumType().getValues().forEach(v -> values.add(v.getName()));
            f.put("values", values);
         }

         fields.add(f);
      }

      Map<String, Object> out = new LinkedHashMap<>();
      out.put("message", d.getName());
      out.put("fields", fields);
      return out;
   }

   private static String fieldType(FieldDescriptor fd) {
      if (fd.isMapField()) {
         Descriptor entry = fd.getMessageType();
         return "map<" + scalarOrName(entry.findFieldByNumber(1)) + ", " + scalarOrName(entry.findFieldByNumber(2)) + ">";
      } else {
         return scalarOrName(fd);
      }
   }

   private static String scalarOrName(FieldDescriptor fd) {
      return switch (fd.getJavaType()) {
         case MESSAGE -> "message:" + fd.getMessageType().getName();
         case ENUM -> "enum:" + fd.getEnumType().getName();
         default -> fd.getType().name().toLowerCase();
      };
   }

   public static ProtobufSchema toSchema(String path, List<String> searchPaths) {
      Resource resource = Resource.path(path);
      File file = resource.getPath().toFile();
      File searchFile = null;
      if (searchPaths != null) {
         for (String searchPath : searchPaths) {
            try {
               Resource searchResource = Resource.path(searchPath);
               searchFile = searchResource.getPath().toFile();
               break;
            } catch (Exception var8) {
            }
         }
      }

      return toSchema(file, searchFile, new HashMap<>());
   }

   public static ProtobufSchema toSchema(File file, File searchFile, Map<String, ProtoFileElement> dependencies) {
      ProtoFileElement element = ProtoParser.Companion.parse(Location.get(""), FileUtils.toString(file));
      if (searchFile != null) {
         for (String dep : element.getImports()) {
            File depFile = new File(searchFile.getPath() + File.separator + dep);
            if (depFile.exists()) {
               ProtobufSchema depSchema = toSchema(depFile, searchFile, dependencies);
               dependencies.put(dep, depSchema.rawSchema());
            }
         }
      }

      return new ProtobufSchema(element, new ArrayList(), dependencies);
   }

   public record MethodType(boolean request, boolean response) {
      public boolean clientStreaming() {
         return this.request;
      }

      public boolean serverStreaming() {
         return this.response;
      }
   }
}
