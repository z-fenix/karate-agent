package io.karatelabs.ext.grpc;

import com.google.protobuf.Any;
import com.google.protobuf.TypeRegistry;
import com.google.protobuf.util.JsonFormat;
import com.google.rpc.BadRequest;
import com.google.rpc.DebugInfo;
import com.google.rpc.ErrorInfo;
import com.google.rpc.Help;
import com.google.rpc.LocalizedMessage;
import com.google.rpc.PreconditionFailure;
import com.google.rpc.QuotaFailure;
import com.google.rpc.RequestInfo;
import com.google.rpc.ResourceInfo;
import com.google.rpc.RetryInfo;
import io.grpc.Metadata;
import io.grpc.Status;
import io.grpc.protobuf.StatusProto;
import io.karatelabs.common.Json;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class GrpcStatusDetails {
   private static final Logger logger = LoggerFactory.getLogger("karate.grpc");
   private static final TypeRegistry REGISTRY = TypeRegistry.newBuilder()
      .add(ErrorInfo.getDescriptor())
      .add(BadRequest.getDescriptor())
      .add(QuotaFailure.getDescriptor())
      .add(PreconditionFailure.getDescriptor())
      .add(RetryInfo.getDescriptor())
      .add(ResourceInfo.getDescriptor())
      .add(RequestInfo.getDescriptor())
      .add(Help.getDescriptor())
      .add(LocalizedMessage.getDescriptor())
      .add(DebugInfo.getDescriptor())
      .build();

   private GrpcStatusDetails() {
   }

   static Map<String, Object> decode(Status status, Metadata trailers) {
      if (trailers == null) {
         return null;
      } else {
         com.google.rpc.Status rpc;
         try {
            rpc = StatusProto.fromStatusAndTrailers(status, trailers);
         } catch (Exception var5) {
            logger.debug("no decodable grpc status details: {}", var5.getMessage());
            return null;
         }

         if (rpc != null && rpc.getDetailsCount() != 0) {
            try {
               String json = JsonFormat.printer().usingTypeRegistry(REGISTRY).omittingInsignificantWhitespace().print(rpc);
               return Json.of(json).asMap();
            } catch (Exception var4) {
               logger.warn("could not fully render grpc status details ({}); surfacing type urls", var4.getMessage());
               return fallback(rpc);
            }
         } else {
            return null;
         }
      }
   }

   private static Map<String, Object> fallback(com.google.rpc.Status rpc) {
      Map<String, Object> out = new LinkedHashMap<>();
      out.put("code", rpc.getCode());
      if (!rpc.getMessage().isEmpty()) {
         out.put("message", rpc.getMessage());
      }

      List<Object> details = new ArrayList<>();

      for (Any any : rpc.getDetailsList()) {
         details.add(Map.of("@type", any.getTypeUrl()));
      }

      out.put("details", details);
      return out;
   }
}
