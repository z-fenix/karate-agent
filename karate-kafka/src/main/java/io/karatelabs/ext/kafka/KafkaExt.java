package io.karatelabs.ext.kafka;

import io.karatelabs.common.Resource;
import io.karatelabs.core.Ext;
import io.karatelabs.core.Suite;
import io.karatelabs.ext.common.ExtMeta;
import io.karatelabs.ext.common.MaxPaths;
import io.karatelabs.ext.common.keycheck.Main;
import io.karatelabs.js.SimpleObject;
import java.nio.file.Path;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KafkaExt implements Ext, SimpleObject {
   private static final Logger logger = LoggerFactory.getLogger(io.karatelabs.ext.kafka.KafkaExt.class);
   static final String VERSION = ExtMeta.version("karate-kafka-meta.properties");
   static final String BOOTSTRAP_SERVERS = "bootstrap.servers";
   static final String SCHEMA_REGISTRY_URL = "schema.registry.url";
   static final String SECURITY_PROTOCOL = "security.protocol";
   static final String SSL_TRUSTSTORE_LOCATION = "ssl.truststore.location";
   static final String SSL_TRUSTSTORE_PASSWORD = "ssl.truststore.password";
   static final String SSL_KEYSTORE_LOCATION = "ssl.keystore.location";
   static final String SSL_KEYSTORE_PASSWORD = "ssl.keystore.password";
   static final String SSL_KEY_PASSWORD = "ssl.key.password";
   private static final String BOOTSTRAP = "bootstrap";
   private static final String SCHEMA_REGISTRY = "schemaRegistry";
   private static final String SSL = "ssl";
   private static final String PROPS = "props";
   private String bootstrap;
   private String schemaRegistry;
   private Map<String, Object> ssl;
   private Map<String, Object> props;

   public void onBoot(Suite suite) {
      Main.requireProduct("kafka", new String[]{"kafka"});
      suite.registerChannelFactory("kafka", new io.karatelabs.ext.kafka.KafkaChannelFactory(this));
      logger.info("kafka ext booted (channel factory registered)");
   }

   Map<String, Object> toClientConfig(Path root, Path classpathRoot) {
      Map<String, Object> config = new LinkedHashMap<>();
      if (this.bootstrap != null) {
         config.put("bootstrap.servers", this.bootstrap);
      }

      if (this.schemaRegistry != null) {
         config.put("schema.registry.url", this.schemaRegistry);
      }

      if (this.ssl != null) {
         this.applySsl(config, this.ssl, root, classpathRoot);
      }

      if (this.props != null) {
         config.putAll(this.props);
      }

      return config;
   }

   private void applySsl(Map<String, Object> config, Map<String, Object> ssl, Path root, Path classpathRoot) {
      Object protocol = ssl.get("protocol");
      config.put("security.protocol", protocol != null ? protocol.toString() : "SSL");
      putPath(config, "ssl.truststore.location", ssl.get("truststore"), root, classpathRoot);
      putStr(config, "ssl.truststore.password", ssl.get("truststorePassword"));
      putPath(config, "ssl.keystore.location", ssl.get("keystore"), root, classpathRoot);
      putStr(config, "ssl.keystore.password", ssl.get("keystorePassword"));
      putStr(config, "ssl.key.password", ssl.get("keyPassword"));
   }

   private static void putStr(Map<String, Object> config, String key, Object value) {
      if (value != null) {
         config.put(key, value.toString());
      }
   }

   private static void putPath(Map<String, Object> config, String key, Object value, Path root, Path classpathRoot) {
      if (value != null) {
         String ref = value.toString();
         Resource resource = root == null ? Resource.path(ref) : MaxPaths.resolveResource(root, classpathRoot, ref);
         config.put(key, resource.getPath().toAbsolutePath().toString());
      }
   }

   public Collection<String> jsKeys() {
      return List.of("bootstrap", "schemaRegistry", "ssl", "props");
   }

   public Object jsGet(String name) {
      return switch (name) {
         case "bootstrap" -> this.bootstrap;
         case "schemaRegistry" -> this.schemaRegistry;
         case "ssl" -> this.ssl;
         case "props" -> this.props;
         default -> null;
      };
   }

   public void putMember(String name, Object value) {
      switch (name) {
         case "bootstrap":
            this.bootstrap = value != null ? value.toString() : null;
            break;
         case "schemaRegistry":
            this.schemaRegistry = value != null ? value.toString() : null;
            break;
         case "ssl":
            if (value instanceof Map) {
               this.ssl = (Map<String, Object>)value;
            }
            break;
         case "props":
            if (value instanceof Map) {
               this.props = (Map<String, Object>)value;
            }
            break;
         default:
            logger.warn("kafka ext: unknown boot config key: {} - {}", name, value);
      }
   }

   public Map<String, Object> getManifest() {
      Map<String, Object> m = new LinkedHashMap<>();
      m.put("version", VERSION);
      return m;
   }
}
