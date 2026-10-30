package io.karatelabs.ext.grpc;

import io.karatelabs.common.Json;
import io.karatelabs.ext.common.CoverageEmit;
import io.karatelabs.ext.common.MaxPaths;
import io.karatelabs.js.JavaCallable;
import io.karatelabs.js.SimpleObject;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GrpcConsumer implements SimpleObject {
   private static final Logger logger = LoggerFactory.getLogger("karate.grpc");
   private static final String PROTO = "proto";
   private static final String PROTO_ROOTS = "protoRoots";
   private static final String SERVICE = "service";
   private static final String METHOD = "method";
   private static final String FILTER = "filter";
   private static final String COUNT = "count";
   private static final String COLLECT = "collect";
   private static final String START = "start";
   private static final String SEND = "send";
   private static final String STOP = "stop";
   private static final String POP = "pop";
   private static final String CONFIG = "config";
   private static final String STREAM = "stream";
   private static final String FLUSH = "flush";
   private static final String HOST = "host";
   private static final String PORT = "port";
   private static final String CLIENT_CERT = "clientCert";
   private static final String CLIENT_KEY = "clientKey";
   private static final String TRUST_CERT = "trustCert";
   private static final String METADATA = "metadata";
   private static final String METADATA_RESPONSE = "metadataResponse";
   private static final String STATUS = "status";
   private static final String STATUS_DETAILS = "statusDetails";
   final Map<String, Object> config;
   String proto;
   List<String> protoRoots;
   String host;
   int port;
   String service;
   String method;
   boolean stream;
   int messageCount = 1;
   String clientCert;
   String clientKey;
   String trustCert;
   Map<String, String> metadataRequest;
   Map<String, String> metadataResponse;
   Long timeout;
   private boolean completeOnStreamEnd;
   private JavaCallable filterFunction;
   private boolean started;
   private io.karatelabs.ext.grpc.GrpcCatalog catalog;
   private io.karatelabs.ext.grpc.GrpcClient client;
   private final List<Object> results = new ArrayList<>();
   private CompletableFuture<List<Object>> resultsFuture = new CompletableFuture<>();
   final List<Map<String, Object>> exchanges = new ArrayList<>();
   volatile String lastStatus;
   private volatile long lastDurationMs;
   private volatile Object lastRequest;
   volatile Map<String, Object> lastStatusDetails;
   volatile String callError;
   Path root;
   Path classpathRoot;

   public GrpcConsumer(Map<String, Object> config) {
      this.config = config;
   }

   String ref(String s) {
      return s != null && this.root != null ? MaxPaths.resolveResource(this.root, this.classpathRoot, s).getPath().toString() : s;
   }

   List<String> refs(List<String> in) {
      if (in == null) {
         return null;
      } else {
         List<String> out = new ArrayList<>(in.size());

         for (String s : in) {
            out.add(this.ref(s));
         }

         return out;
      }
   }

   io.karatelabs.ext.grpc.GrpcConsumer copyConfig() {
      io.karatelabs.ext.grpc.GrpcConsumer c = new io.karatelabs.ext.grpc.GrpcConsumer(this.config);
      c.root = this.root;
      c.classpathRoot = this.classpathRoot;
      c.host = this.host;
      c.port = this.port;
      c.proto = this.proto;
      c.protoRoots = this.protoRoots;
      c.service = this.service;
      c.clientCert = this.clientCert;
      c.clientKey = this.clientKey;
      c.trustCert = this.trustCert;
      c.metadataRequest = this.metadataRequest;
      c.timeout = this.timeout;
      c.catalog = this.catalog;
      return c;
   }

   io.karatelabs.ext.grpc.GrpcCatalog catalog() {
      if (this.catalog == null) {
         this.catalog = (io.karatelabs.ext.grpc.GrpcCatalog)(this.proto != null
            ? new io.karatelabs.ext.grpc.ProtoCatalog(this.ref(this.proto), this.refs(this.protoRoots))
            : io.karatelabs.ext.grpc.ReflectionCatalog.fromService(this));
      }

      return this.catalog;
   }

   void recordGrpcCompletion(String status, long durationMs) {
      this.lastStatus = status;
      this.lastDurationMs = durationMs;
   }

   void recordStatusDetails(Map<String, Object> details) {
      this.lastStatusDetails = details;
   }

   public Void start() {
      this.started = true;
      this.client = new io.karatelabs.ext.grpc.GrpcClient(this);
      return null;
   }

   public Void send(Object o) {
      if (!this.started) {
         this.start();
      }

      Map<String, Object> map = (Map<String, Object>)o;
      this.lastRequest = map;
      String json = Json.of(map).toString();
      logger.debug(">> {}", json);
      this.client.send(json);
      return null;
   }

   public void receive(Object raw) {
      Map<String, Object> message = Json.of(raw).asMap();
      if (this.shouldConsume(message)) {
         logger.debug("<< {}", raw);
         this.results.add(message);
         if (this.results.size() >= this.messageCount) {
            this.resultsFuture.complete(this.results);
         }
      } else {
         logger.debug("<< ignoring: {}", raw);
      }
   }

   public Void stop() {
      if (this.client != null) {
         this.client.close();
      }

      if (!this.resultsFuture.isDone()) {
         this.resultsFuture.complete(this.results);
      }

      return null;
   }

   void completeStream() {
      if (this.completeOnStreamEnd && !this.resultsFuture.isDone()) {
         this.resultsFuture.complete(new ArrayList<>(this.results));
      }
   }

   protected boolean shouldConsume(Object message) {
      if (this.filterFunction == null) {
         return true;
      } else {
         try {
            Object result = this.filterFunction.call(null, new Object[]{message});
            return result instanceof Boolean b ? b : Boolean.parseBoolean(String.valueOf(result));
         } catch (Exception var4) {
            logger.error("filter expression failed: {}", var4.getMessage());
            return false;
         }
      }
   }

   public void setCount(int messageCount) {
      this.messageCount = messageCount;
   }

   public List<Object> collect() {
      try {
         List<Object> list = this.timeout != null ? this.resultsFuture.get(this.timeout, TimeUnit.MILLISECONDS) : this.resultsFuture.get();
         List<Object> clone = new ArrayList<>(list);
         this.resultsFuture = new CompletableFuture<>();
         this.results.clear();
         logger.debug("<< collect {}", clone);
         this.emitCoverageMatch();
         return clone;
      } catch (TimeoutException var3) {
         String msg = "grpc "
            + this.service
            + "/"
            + this.method
            + " timed out after "
            + this.timeout
            + "ms with "
            + this.results.size()
            + " of "
            + (this.messageCount == Integer.MAX_VALUE ? "?" : this.messageCount)
            + " message(s) received\nhint: the server did not complete the response stream in time — check it is reachable + responding, or raise `timeout` in connect()/the call opts";
         logger.warn(msg);
         throw new RuntimeException(msg, var3);
      } catch (Exception var4) {
         throw new RuntimeException(io.karatelabs.ext.grpc.GrpcDiagnostics.connectFailure(this, "collect", var4), var4);
      }
   }

   private void emitCoverageMatch() {
      if (this.service != null && this.method != null) {
         Map<String, Object> m = new LinkedHashMap<>();
         m.put("service", this.service);
         m.put("method", this.method);
         m.put("status", this.lastStatus != null ? this.lastStatus : "OK");
         m.put("durationMs", this.lastDurationMs);
         if (this.lastRequest != null) {
            m.put("request", this.lastRequest);
         }

         CoverageEmit.match("grpc-match", m);
      }
   }

   public Object pop() {
      List<Object> list = this.collect();
      return list.get(0);
   }

   public Void applyConfig(Map<String, Object> map) {
      map.forEach((k, v) -> this.putMember(k, v));
      return null;
   }

   public Void flush() {
      this.client.flush();
      return null;
   }

   Object call(String method, Object msg, Map<String, Object> opts) {
      this.method = method;
      this.completeOnStreamEnd = true;
      this.callError = null;
      if (opts != null) {
         if (opts.get("metadata") instanceof Map<?, ?> md) {
            this.metadataRequest = (Map<String, String>)md;
         }

         if (opts.get("timeout") instanceof Number t) {
            this.timeout = t.longValue();
         }
      }

      io.karatelabs.ext.grpc.ProtobufUtils.MethodType mt = this.catalog().methodType(this.service, method);
      this.setCount(mt.serverStreaming() ? Integer.MAX_VALUE : 1);
      if (mt.clientStreaming()) {
         this.stream = true;

         for (Object m : msg instanceof List ? (List)msg : List.of(msg)) {
            this.send(m);
         }

         this.flush();
      } else {
         this.stream = false;
         this.send(msg);
      }

      List<Object> responses = this.collect();
      if (responses.isEmpty() && this.callError != null) {
         String errMsg = this.callError;
         if (this.lastStatusDetails != null) {
            errMsg = errMsg + " — typed error details available on the handle: g.statusDetails (google.rpc.Status); status: g.status";
         } else if (this.lastStatus != null && !"OK".equals(this.lastStatus)) {
            errMsg = errMsg + " — gRPC status on the handle: g.status (" + this.lastStatus + ")";
         }

         throw new RuntimeException(errMsg);
      } else {
         Map<String, Object> ex = new LinkedHashMap<>();
         ex.put("method", method);
         ex.put("request", msg);
         ex.put("responses", responses);
         ex.put("status", this.lastStatus != null ? this.lastStatus : "OK");
         ex.put("durationMs", this.lastDurationMs);
         this.exchanges.add(ex);
         return mt.serverStreaming() ? responses : (responses.isEmpty() ? null : responses.get(0));
      }
   }

   public Collection<String> jsKeys() {
      return List.of(
         "proto",
         "protoRoots",
         "service",
         "method",
         "filter",
         "count",
         "collect",
         "start",
         "send",
         "stop",
         "pop",
         "config",
         "stream",
         "flush",
         "host",
         "port",
         "clientCert",
         "clientKey",
         "trustCert",
         "metadata",
         "metadataResponse",
         "status",
         "statusDetails"
      );
   }

   public Object jsGet(String name) {
      return switch (name) {
         case "send" -> (io.karatelabs.js.JavaInvokable) args -> {
            this.send(args[0]);
            return null;
         };
         case "start" -> (io.karatelabs.js.JavaInvokable) args -> {
            this.start();
            return null;
         };
         case "collect" -> (io.karatelabs.js.JavaInvokable) args -> this.collect();
         case "pop" -> (io.karatelabs.js.JavaInvokable) args -> this.pop();
         case "stop" -> (io.karatelabs.js.JavaInvokable) args -> {
            this.stop();
            return null;
         };
         case "flush" -> (io.karatelabs.js.JavaInvokable) args -> {
            this.flush();
            return null;
         };
         case "metadataResponse" -> this.metadataResponse;
         case "status" -> this.lastStatus != null ? this.lastStatus : "OK";
         case "statusDetails" -> this.lastStatusDetails;
         default -> null;
      };
   }

   public void putMember(String name, Object value) {
      switch (name) {
         case "filter":
            if (value instanceof JavaCallable jc) {
               this.filterFunction = jc;
            }
            break;
         case "count":
            this.setCount(value instanceof Number n ? n.intValue() : Integer.parseInt(value.toString()));
            break;
         case "proto":
            this.proto = value != null ? value.toString() : null;
            break;
         case "protoRoots":
            if (value instanceof List) {
               this.protoRoots = (List<String>)value;
            }
            break;
         case "service":
            this.service = value != null ? value.toString() : null;
            break;
         case "method":
            this.method = value != null ? value.toString() : null;
            break;
         case "config":
            if (value instanceof Map) {
               this.applyConfig((Map<String, Object>)value);
            }
            break;
         case "stream":
            this.stream = value instanceof Boolean b ? b : Boolean.parseBoolean(value.toString());
            break;
         case "host":
            this.host = value != null ? value.toString() : null;
            break;
         case "port":
            try {
               this.port = value instanceof Number n ? n.intValue() : Integer.parseInt(value.toString());
            } catch (Exception var6) {
               logger.error("port config failed: {}", var6.getMessage());
            }
            break;
         case "clientCert":
            this.clientCert = value != null ? value.toString() : null;
            break;
         case "clientKey":
            this.clientKey = value != null ? value.toString() : null;
            break;
         case "trustCert":
            this.trustCert = value != null ? value.toString() : null;
            break;
         case "metadata":
            if (value instanceof Map) {
               this.metadataRequest = (Map<String, String>)value;
            }
            break;
         default:
            logger.warn("put not supported on session object: {} - {}", name, value);
      }
   }
}
