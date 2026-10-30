package io.karatelabs.ext.grpc;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.Status.Code;

final class GrpcDiagnostics {
   private GrpcDiagnostics() {
   }

   static String connectFailure(io.karatelabs.ext.grpc.GrpcConsumer caller, String operation, Throwable cause) {
      StringBuilder sb = new StringBuilder();
      sb.append("grpc ").append(operation).append(" failed");
      StatusRuntimeException sre = unwrapStatus(cause);
      if (sre != null) {
         Status status = sre.getStatus();
         sb.append(": ").append(status.getCode());
         if (status.getDescription() != null) {
            sb.append(" - ").append(status.getDescription());
         }
      } else if (cause != null && cause.getMessage() != null) {
         sb.append(": ").append(cause.getMessage());
      }

      sb.append("\n  target = ").append(caller.host).append(':').append(caller.port);
      boolean tls = caller.trustCert != null || caller.clientCert != null;
      sb.append("\n  tls = ").append(tls ? (caller.clientCert != null ? "mTLS" : "TLS") : "no (plaintext)");
      if (caller.trustCert != null) {
         sb.append("\n  trustCert = ").append(caller.trustCert);
      }

      if (caller.clientCert != null) {
         sb.append("\n  clientCert = ").append(caller.clientCert);
      }

      if (sre != null) {
         Code code = sre.getStatus().getCode();
         if (code == Code.UNAVAILABLE) {
            sb.append("\nhint: UNAVAILABLE means the connection could not be established. check, in order:");
            sb.append("\n  1. server is running and listening on the configured host:port");
            sb.append(
               "\n  2. plaintext-vs-TLS mismatch — set trustCert (and clientCert/clientKey for mTLS) if the server requires TLS, or remove them if it expects plaintext"
            );
            sb.append("\n  3. firewall / network routing");
            if (tls) {
               sb.append("\n  4. TLS handshake — re-run with -Djavax.net.debug=ssl,handshake for the exact TLS failure reason");
               sb.append("\n  5. cert expiry / SAN mismatch — the trust cert chain must validate the server's cert and cover the host above");
            }
         } else if (code == Code.UNAUTHENTICATED || code == Code.PERMISSION_DENIED) {
            sb.append("\nhint: ").append(code).append(" means the server rejected the call. check the metadata/tokens being sent");
            if (caller.clientCert != null) {
               sb.append(", and (for mTLS) that the client cert is the one the server expects");
            }
         } else if (code == Code.DEADLINE_EXCEEDED) {
            sb.append("\nhint: DEADLINE_EXCEEDED — the call did not complete in time. check server-side latency or raise the deadline on the call");
         }
      }

      return sb.toString();
   }

   private static StatusRuntimeException unwrapStatus(Throwable t) {
      for (Throwable cur = t; cur != null; cur = cur.getCause()) {
         if (cur instanceof StatusRuntimeException sre) {
            return sre;
         }
      }

      return null;
   }
}
