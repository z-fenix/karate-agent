package io.karatelabs.http;

public class WsException extends RuntimeException {
   private final WsException.Type type;

   public WsException(WsException.Type type, String message) {
      super(message);
      this.type = type;
   }

   public WsException(WsException.Type type, String message, Throwable cause) {
      super(message, cause);
      this.type = type;
   }

   public WsException.Type getType() {
      return this.type;
   }

   public boolean isTimeout() {
      return this.type == WsException.Type.TIMEOUT;
   }

   public boolean isConnectionClosed() {
      return this.type == WsException.Type.CONNECTION_CLOSED;
   }

   @Override
   public String toString() {
      return "WsException[" + this.type + "]: " + this.getMessage();
   }

   public static enum Type {
      CONNECT_FAILED,
      CONNECTION_CLOSED,
      TIMEOUT,
      PROTOCOL_ERROR,
      SEND_FAILED;
   }
}
