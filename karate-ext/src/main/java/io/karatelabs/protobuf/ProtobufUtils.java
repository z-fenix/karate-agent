package io.karatelabs.protobuf;

import com.google.protobuf.DynamicMessage;
import com.google.protobuf.Message;
import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.DynamicMessage.Builder;
import com.google.protobuf.util.JsonFormat;
import com.squareup.wire.schema.Location;
import com.squareup.wire.schema.internal.parser.ProtoFileElement;
import com.squareup.wire.schema.internal.parser.ProtoParser;
import io.confluent.kafka.schemaregistry.protobuf.ProtobufSchema;
import io.karatelabs.common.FileUtils;
import io.karatelabs.common.Resource;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ProtobufUtils {
   public static ProtobufSchema toSchema(File file, File searchFile, Map<String, ProtoFileElement> dependencies) {
      ProtoFileElement element = ProtoParser.Companion.parse(Location.get(""), FileUtils.toString(file));
      if (dependencies == null) {
         dependencies = new HashMap<>();
      }

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

   public static Descriptor toDescriptor(String path, String messageName, List<String> roots) {
      try {
         Resource resource = Resource.path(path);
         File file = resource.getPath().toFile();
         File searchFile = null;
         if (roots != null) {
            for (String root : roots) {
               try {
                  Resource searchResource = Resource.path(root);
                  searchFile = searchResource.getPath().toFile();
                  break;
               } catch (Exception var9) {
               }
            }
         }

         ProtobufSchema schema = toSchema(file, searchFile, null);
         return schema.toDescriptor(messageName);
      } catch (Exception var10) {
         throw new RuntimeException(var10);
      }
   }

   public static String toJson(Descriptor descriptor, byte[] bytes) {
      try {
         Message message = DynamicMessage.parseFrom(descriptor, bytes);
         return JsonFormat.printer().includingDefaultValueFields().print(message);
      } catch (Exception var3) {
         throw new RuntimeException(var3);
      }
   }

   public static byte[] fromJson(Descriptor descriptor, String json) {
      try {
         Builder builder = DynamicMessage.newBuilder(descriptor);
         JsonFormat.parser().merge(json, builder);
         return builder.build().toByteArray();
      } catch (Exception var3) {
         throw new RuntimeException(var3);
      }
   }
}
