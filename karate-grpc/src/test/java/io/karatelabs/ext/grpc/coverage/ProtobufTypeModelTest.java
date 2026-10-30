package io.karatelabs.ext.grpc.coverage;

import com.google.protobuf.DescriptorProtos.DescriptorProto;
import com.google.protobuf.DescriptorProtos.EnumDescriptorProto;
import com.google.protobuf.DescriptorProtos.EnumValueDescriptorProto;
import com.google.protobuf.DescriptorProtos.FieldDescriptorProto;
import com.google.protobuf.DescriptorProtos.FileDescriptorProto;
import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.Descriptors.FileDescriptor;
import java.util.List;
import java.util.Map;
import org.junit.Test;
import static org.junit.Assert.*;

public class ProtobufTypeModelTest {
   private static FieldDescriptorProto field(String name, int number, FieldDescriptorProto.Type type) {
      return FieldDescriptorProto.newBuilder().setName(name).setNumber(number).setType(type).build();
   }

   @Test
   public void grpcDispatchExposesCallableCommands() {
      io.karatelabs.ext.grpc.GrpcConsumer consumer = new io.karatelabs.ext.grpc.GrpcConsumer(Map.of());
      assertTrue(consumer.jsGet("send") instanceof io.karatelabs.js.JavaInvokable);
      assertNull(consumer.jsGet("unknown"));
   }

   @Test
   public void samplesConstrainedAndNestedFields() throws Exception {
      EnumDescriptorProto state = EnumDescriptorProto.newBuilder().setName("State")
         .addValue(EnumValueDescriptorProto.newBuilder().setName("UNKNOWN").setNumber(0))
         .addValue(EnumValueDescriptorProto.newBuilder().setName("READY").setNumber(1)).build();
      DescriptorProto child = DescriptorProto.newBuilder().setName("Child")
         .addField(field("id", 1, FieldDescriptorProto.Type.TYPE_INT32)).build();
      DescriptorProto payload = DescriptorProto.newBuilder().setName("Payload").addEnumType(state).addNestedType(child)
         .addField(field("count", 1, FieldDescriptorProto.Type.TYPE_INT32))
         .addField(field("total", 2, FieldDescriptorProto.Type.TYPE_INT64))
         .addField(field("ratio", 3, FieldDescriptorProto.Type.TYPE_DOUBLE))
         .addField(field("label", 4, FieldDescriptorProto.Type.TYPE_STRING))
         .addField(field("enabled", 5, FieldDescriptorProto.Type.TYPE_BOOL))
         .addField(field("data", 6, FieldDescriptorProto.Type.TYPE_BYTES))
         .addField(FieldDescriptorProto.newBuilder().setName("state").setNumber(7).setType(FieldDescriptorProto.Type.TYPE_ENUM).setTypeName(".test.Payload.State"))
         .addField(FieldDescriptorProto.newBuilder().setName("child").setNumber(8).setType(FieldDescriptorProto.Type.TYPE_MESSAGE).setTypeName(".test.Payload.Child"))
         .build();
      Descriptor descriptor = FileDescriptor.buildFrom(FileDescriptorProto.newBuilder().setName("sample.proto")
         .setPackage("test").setSyntax("proto3").addMessageType(payload).build(), new FileDescriptor[0]).findMessageTypeByName("Payload");
      Map<String, Map<String, Object>> constraints = Map.of(
         "Payload.count", Map.of("numericBounds", List.of(2, 8)),
         "Payload.label", Map.of("minLen", 3));
      Map<?, ?> sample = (Map<?, ?>)new ProtobufTypeModel(descriptor, constraints).sample();
      assertEquals(5, sample.get("count"));
      assertEquals(0L, sample.get("total"));
      assertEquals(0.0, sample.get("ratio"));
      assertEquals("xxx", sample.get("label"));
      assertEquals(false, sample.get("enabled"));
      assertEquals("", sample.get("data"));
      assertEquals("READY", sample.get("state"));
      assertEquals(Map.of("id", 0), sample.get("child"));
   }
}
