package io.karatelabs.ext.kafka;

import java.util.Map;
import java.util.Properties;
import java.util.Map.Entry;
import org.apache.kafka.common.Metric;
import org.apache.kafka.common.MetricName;

final class KafkaDiagnostics {
   private KafkaDiagnostics() {
   }

   static String connectFailure(Properties props, String operation, Throwable cause) {
      StringBuilder sb = new StringBuilder();
      sb.append("kafka ").append(operation).append(" failed");
      if (cause != null && cause.getMessage() != null) {
         sb.append(": ").append(cause.getMessage());
      }

      sb.append('\n');
      sb.append("  bootstrap.servers = ").append(props.getProperty("bootstrap.servers", "<unset>")).append('\n');
      String protocol = props.getProperty("security.protocol", "PLAINTEXT");
      sb.append("  security.protocol = ").append(protocol).append('\n');
      boolean ssl = protocol.contains("SSL");
      if (ssl) {
         sb.append("  ssl.keystore.location = ").append(props.getProperty("ssl.keystore.location", "<unset>")).append('\n');
         sb.append("  ssl.truststore.location = ").append(props.getProperty("ssl.truststore.location", "<jvm default cacerts>")).append('\n');
         sb.append("  ssl.endpoint.identification.algorithm = ").append(props.getProperty("ssl.endpoint.identification.algorithm", "https")).append('\n');
         sb.append("hint: this is most often a TLS / certificate issue. check, in order:\n");
         sb.append("  1. client cert expiry: keytool -list -v -keystore <ssl.keystore.location> -storepass <pwd>\n");
         sb.append("  2. broker cert chain / expiry: openssl s_client -connect <broker:port> -showcerts </dev/null\n");
         sb.append("  3. broker cert SubjectAltName covers the host(s) in bootstrap.servers (raw IPs need IP SANs)\n");
         sb.append("  4. re-run with -Djavax.net.debug=ssl,handshake for a definitive TLS failure reason");
      } else {
         sb.append("hint: check broker reachability (network, firewall, DNS) and that brokers are listening on the configured ports");
      }

      return sb.toString();
   }

   static String connectionCount(Map<MetricName, ? extends Metric> metrics) {
      for (Entry<MetricName, ? extends Metric> e : metrics.entrySet()) {
         MetricName name = e.getKey();
         if ("connection-count".equals(name.name()) && "consumer-metrics".equals(name.group())) {
            return formatMetric(e.getValue());
         }

         if ("connection-count".equals(name.name()) && "producer-metrics".equals(name.group())) {
            return formatMetric(e.getValue());
         }
      }

      return "?";
   }

   private static String formatMetric(Metric metric) {
      Object v = metric.metricValue();
      return v instanceof Number n ? Integer.toString(n.intValue()) : String.valueOf(v);
   }
}
