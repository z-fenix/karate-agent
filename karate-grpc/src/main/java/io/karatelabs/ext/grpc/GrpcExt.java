package io.karatelabs.ext.grpc;

import io.karatelabs.core.Ext;
import io.karatelabs.core.Suite;
import io.karatelabs.ext.common.ExtMeta;
import io.karatelabs.ext.common.keycheck.Main;
import io.karatelabs.js.SimpleObject;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GrpcExt implements Ext, SimpleObject {
   private static final Logger logger = LoggerFactory.getLogger(io.karatelabs.ext.grpc.GrpcExt.class);
   static final String VERSION = ExtMeta.version("karate-grpc-meta.properties");
   private static final String HOST = "host";
   private static final String PORT = "port";
   private static final String PROTO = "proto";
   private static final String PROTO_ROOTS = "protoRoots";
   private static final String TLS = "tls";
   private static final String TRUST_CERT = "trustCert";
   private static final String CLIENT_CERT = "clientCert";
   private static final String CLIENT_KEY = "clientKey";
   String host;
   int port;
   String proto;
   List<String> protoRoots;
   String trustCert;
   String clientCert;
   String clientKey;

   public void onBoot(Suite suite) {
      Main.requireProduct("grpc", new String[]{"grpc"});
      suite.registerChannelFactory("grpc", new io.karatelabs.ext.grpc.GrpcChannelFactory(this));
      logger.info("grpc ext booted (channel factory registered)");
   }

   void seedDefaults(io.karatelabs.ext.grpc.GrpcConsumer s) {
      if (s.host == null) {
         s.host = this.host;
      }

      if (s.port == 0) {
         s.port = this.port;
      }

      if (s.proto == null) {
         s.proto = this.proto;
      }

      if (s.protoRoots == null) {
         s.protoRoots = this.protoRoots;
      }

      if (s.trustCert == null) {
         s.trustCert = this.trustCert;
      }

      if (s.clientCert == null) {
         s.clientCert = this.clientCert;
      }

      if (s.clientKey == null) {
         s.clientKey = this.clientKey;
      }
   }

   public Collection<String> jsKeys() {
      return List.of("host", "port", "proto", "protoRoots", "tls");
   }

   public Object jsGet(String name) {
      return switch (name) {
         case "host" -> this.host;
         case "port" -> this.port;
         case "proto" -> this.proto;
         case "protoRoots" -> this.protoRoots;
         default -> null;
      };
   }

   public void putMember(String name, Object value) {
      switch (name) {
         case "host":
            this.host = value != null ? value.toString() : null;
            break;
         case "port":
            this.port = value instanceof Number n ? n.intValue() : Integer.parseInt(value.toString());
            break;
         case "proto":
            this.proto = value != null ? value.toString() : null;
            break;
         case "protoRoots":
            if (value instanceof List) {
               this.protoRoots = (List<String>)value;
            }
            break;
         case "tls":
            if (value instanceof Map) {
               this.applyTls((Map<String, Object>)value);
            }
            break;
         default:
            logger.warn("grpc ext: unknown boot config key: {} - {}", name, value);
      }
   }

   private void applyTls(Map<String, Object> tls) {
      Object t = tls.get("trustCert");
      if (t != null) {
         this.trustCert = t.toString();
      }

      Object c = tls.get("clientCert");
      if (c != null) {
         this.clientCert = c.toString();
      }

      Object k = tls.get("clientKey");
      if (k != null) {
         this.clientKey = k.toString();
      }
   }

   public Map<String, Object> getManifest() {
      Map<String, Object> m = new LinkedHashMap<>();
      m.put("version", VERSION);
      return m;
   }
}
