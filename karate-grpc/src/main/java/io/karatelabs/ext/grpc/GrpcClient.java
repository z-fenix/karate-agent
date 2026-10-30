package io.karatelabs.ext.grpc;

import com.google.protobuf.DynamicMessage;
import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.DynamicMessage.Builder;
import com.google.protobuf.util.JsonFormat;
import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ClientCall;
import io.grpc.ClientInterceptor;
import io.grpc.ClientInterceptors;
import io.grpc.ManagedChannel;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;
import io.grpc.Status;
import io.grpc.ClientCall.Listener;
import io.grpc.ForwardingClientCall.SimpleForwardingClientCall;
import io.grpc.ForwardingClientCallListener.SimpleForwardingClientCallListener;
import io.grpc.Metadata.Key;
import io.grpc.MethodDescriptor.MethodType;
import io.grpc.protobuf.ProtoUtils;
import io.grpc.stub.ClientCalls;
import io.grpc.stub.MetadataUtils;
import io.grpc.stub.StreamObserver;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GrpcClient {
   private static final Logger logger = LoggerFactory.getLogger("karate.grpc");
   private final io.karatelabs.ext.grpc.GrpcConsumer caller;
   private final ManagedChannel channel;
   private final MethodDescriptor<DynamicMessage, DynamicMessage> methodDescriptor;
   private final Descriptor requestDescriptor;
   private ClientCall<DynamicMessage, DynamicMessage> call;
   private StreamObserver<DynamicMessage> requestObserver;
   private final AtomicBoolean closed = new AtomicBoolean(false);

   public GrpcClient(io.karatelabs.ext.grpc.GrpcConsumer caller) {
      this.caller = caller;

      try {
         this.channel = io.karatelabs.ext.grpc.GrpcChannels.build(caller);
         logger.debug("connected: {}", this.channel);
         logger.debug(">> {}", caller.service + "/" + caller.method);
         io.karatelabs.ext.grpc.GrpcCatalog catalog = caller.catalog();
         this.requestDescriptor = catalog.requestType(caller.service, caller.method);
         Descriptor responseDescriptor = catalog.responseType(caller.service, caller.method);
         this.methodDescriptor = MethodDescriptor.<DynamicMessage, DynamicMessage>newBuilder()
            .setType(MethodType.UNKNOWN)
            .setFullMethodName(catalog.fullMethodName(caller.service, caller.method))
            .setRequestMarshaller(ProtoUtils.marshaller(DynamicMessage.getDefaultInstance(this.requestDescriptor)))
            .setResponseMarshaller(ProtoUtils.marshaller(DynamicMessage.getDefaultInstance(responseDescriptor)))
            .build();
      } catch (Exception var4) {
         String diag = io.karatelabs.ext.grpc.GrpcDiagnostics.connectFailure(caller, "client init", var4);
         logger.error(diag);
         throw new RuntimeException(diag, var4);
      }
   }

   private void initCall() {
      if (this.call == null) {
         Metadata headers = new Metadata();
         if (this.caller.metadataRequest != null && !this.caller.metadataRequest.isEmpty()) {
            for (Entry<String, String> entry : this.caller.metadataRequest.entrySet()) {
               Key<String> key = Key.of(entry.getKey(), Metadata.ASCII_STRING_MARSHALLER);
               headers.put(key, entry.getValue());
            }
         }

         ClientInterceptor metadataCaptureInterceptor = new ClientInterceptor() {
            public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(MethodDescriptor<ReqT, RespT> method, CallOptions callOptions, Channel next) {
               return new SimpleForwardingClientCall<ReqT, RespT>(next.newCall(method, callOptions)) {
                  public void start(Listener<RespT> responseListener, Metadata headersx) {
                     final long startedMs = System.currentTimeMillis();
                     Listener<RespT> wrappedListener = new SimpleForwardingClientCallListener<RespT>(responseListener) {
                        public void onHeaders(Metadata headers) {
                           Map<String, String> headerMap = new LinkedHashMap<>();

                           for (String key : headers.keys()) {
                              if (!key.endsWith("-bin")) {
                                 Key<String> metaKey = Key.of(key, Metadata.ASCII_STRING_MARSHALLER);
                                 String value = (String)headers.get(metaKey);
                                 if (value != null) {
                                    headerMap.put(key, value);
                                 }
                              }
                           }

                           if (!headerMap.isEmpty()) {
                              io.karatelabs.ext.grpc.GrpcClient.logger.debug("<< response headers: {}", headerMap);
                           }

                           GrpcClient.this.caller.metadataResponse = headerMap;
                           super.onHeaders(headers);
                        }

                        public void onClose(Status status, Metadata trailers) {
                           if (trailers != null) {
                              Map<String, String> trailerMap = new HashMap<>();

                              for (String key : trailers.keys()) {
                                 if (!key.endsWith("-bin")) {
                                    Key<String> metaKey = Key.of(key, Metadata.ASCII_STRING_MARSHALLER);
                                    String value = (String)trailers.get(metaKey);
                                    if (value != null) {
                                       trailerMap.put(key, value);
                                    }
                                 }
                              }

                              if (GrpcClient.this.caller.metadataResponse == null) {
                                 GrpcClient.this.caller.metadataResponse = new LinkedHashMap<>();
                              }

                              if (!trailerMap.isEmpty()) {
                                 io.karatelabs.ext.grpc.GrpcClient.logger.debug("<< response trailers: {}", trailerMap);
                              }

                              GrpcClient.this.caller.metadataResponse.putAll(trailerMap);
                           }

                           GrpcClient.this.caller.recordGrpcCompletion(status.getCode().name(), System.currentTimeMillis() - startedMs);
                           GrpcClient.this.caller.recordStatusDetails(io.karatelabs.ext.grpc.GrpcStatusDetails.decode(status, trailers));
                           super.onClose(status, trailers);
                        }
                     };
                     super.start(wrappedListener, headersx);
                  }
               };
            }
         };
         Channel interceptedChannel = ClientInterceptors.intercept(
            this.channel, new ClientInterceptor[]{MetadataUtils.newAttachHeadersInterceptor(headers), metadataCaptureInterceptor}
         );
         this.call = interceptedChannel.newCall(this.methodDescriptor, CallOptions.DEFAULT);
         this.requestObserver = ClientCalls.asyncBidiStreamingCall(this.call, new StreamObserver<DynamicMessage>() {
            public void onNext(DynamicMessage response) {
               try {
                  String json = JsonFormat.printer().print(response);
                  GrpcClient.this.caller.receive(json);
               } catch (Exception var3) {
                  if (!GrpcClient.this.closed.get()) {
                     io.karatelabs.ext.grpc.GrpcClient.logger.error("<< error: {}", var3.getMessage());
                     GrpcClient.this.caller.stop();
                  }
               }
            }

            public void onError(Throwable t) {
               if (!GrpcClient.this.closed.get()) {
                  String diag = io.karatelabs.ext.grpc.GrpcDiagnostics.connectFailure(GrpcClient.this.caller, "rpc", t);
                  io.karatelabs.ext.grpc.GrpcClient.logger.error(diag);
                  GrpcClient.this.caller.callError = diag;
                  GrpcClient.this.caller.stop();
               }
            }

            public void onCompleted() {
               GrpcClient.this.call = null;
               GrpcClient.this.caller.completeStream();
            }
         });
      }
   }

   public void close() {
      this.closed.set(true);
      this.channel.shutdownNow();
   }

   public void send(Object payload) {
      Builder builder = DynamicMessage.newBuilder(this.requestDescriptor);

      try {
         JsonFormat.parser().merge(payload.toString(), builder);
      } catch (Exception var4) {
         throw new RuntimeException(var4);
      }

      DynamicMessage request = builder.build();
      this.initCall();
      this.requestObserver.onNext(request);
      if (!this.caller.stream) {
         this.flush();
      }
   }

   public void flush() {
      this.requestObserver.onCompleted();
      this.call = null;
   }
}
