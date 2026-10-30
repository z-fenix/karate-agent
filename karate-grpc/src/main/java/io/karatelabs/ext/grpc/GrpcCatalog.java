package io.karatelabs.ext.grpc;

import com.google.protobuf.Descriptors.Descriptor;
import java.util.List;
import java.util.Map;

interface GrpcCatalog {
   io.karatelabs.ext.grpc.ProtobufUtils.MethodType methodType(String var1, String var2);

   List<Map<String, Object>> methods(String var1);

   Map<String, Object> describe(String var1);

   Descriptor requestType(String var1, String var2);

   Descriptor responseType(String var1, String var2);

   String fullMethodName(String var1, String var2);
}
